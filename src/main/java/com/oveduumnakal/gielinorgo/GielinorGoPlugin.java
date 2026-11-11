/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;

import com.google.gson.Gson;
import com.google.inject.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;

/**
 * Entry point: watches monsters, XP drops and steps in the real game and feeds them to {@link GoGame}. Everything
 * the plugin does stays on this computer. Its menu options are client-side only and send nothing to the game.
 */
@PluginDescriptor(
	name = "Gielinor Go",
	description = "A creature-raising side game: catch wild monsters, train them with your own XP gains, and win"
		+ " auto-battles while you play",
	tags = {"creature", "catch", "collect", "battle", "train", "team", "minigame", "fun", "monster"}
)
public class GielinorGoPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(GielinorGoPlugin.class);

	/** Game ticks between extra chances for a monster already in view to turn wild. */
	private static final int REROLL_TICKS = 25;

	/** Game ticks between side panel refreshes while nothing changes, so resting health still creeps up. */
	private static final int PANEL_TICKS = 5;

	/** Game ticks between saves of changed progress. */
	private static final int SAVE_TICKS = 50;

	/** Most battle log lines played back on screen. */
	private static final int BATTLE_LINES = 6;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private GielinorGoConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ModelOutlineRenderer outlineRenderer;

	@Inject
	private Gson gson;

	private final Random random = new Random();
	private final Map<Integer, NPC> candidates = new HashMap<>();
	private final Map<Skill, Integer> lastXp = new EnumMap<>(Skill.class);
	private final Map<Skill, Integer> lastLevel = new EnumMap<>(Skill.class);
	private final StepCounter stepCounter = new StepCounter();
	private WildTracker tracker = new WildTracker(random);
	private GoGame game;
	private GoPersistence persistence;
	private WildOverlay wildOverlay;
	private final NoticeBoard noticeBoard = new NoticeBoard();
	private FloatingTextOverlay floatingText;
	private GoPanel panel;
	private NavigationButton navButton;
	private boolean loaded;
	private boolean unsaved;
	private int ticks;

	/**
	 * Supplies the plugin's settings.
	 *
	 * @param manager the RuneLite config manager
	 * @return the settings proxy
	 */
	@Provides
	GielinorGoConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(GielinorGoConfig.class);
	}

	@Override
	protected void startUp()
	{
		tracker = new WildTracker(random);
		game = new GoGame(loadSpecies(), random, System::currentTimeMillis, this::announce);
		persistence = new GoPersistence(configManager, gson);
		wildOverlay = new WildOverlay(this, config, outlineRenderer);
		floatingText = new FloatingTextOverlay(client, this, noticeBoard);
		overlayManager.add(wildOverlay);
		overlayManager.add(floatingText);
		panel = new GoPanel(panelActions());
		BufferedImage icon = ImageUtil.loadImageResource(GielinorGoPlugin.class, "panel_icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Gielinor Go")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		clientThread.invoke(this::startOnClientThread);
	}

	@Override
	protected void shutDown()
	{
		clientThread.invoke(this::saveNow);
		overlayManager.remove(wildOverlay);
		overlayManager.remove(floatingText);
		noticeBoard.clear();
		clientToolbar.removeNavigation(navButton);
		tracker.clear();
		candidates.clear();
		lastXp.clear();
		lastLevel.clear();
		stepCounter.reset();
	}

	/** @return the monsters that are wild right now */
	Collection<WildEntry> wildCreatures()
	{
		return tracker.all();
	}

	/**
	 * Finds a tracked monster.
	 *
	 * @param npcIndex the game's index for the monster
	 * @return the monster, or null when it is no longer in view
	 */
	NPC npcFor(int npcIndex)
	{
		return candidates.get(npcIndex);
	}

	/**
	 * Loads the right save whenever the logged-in account changes.
	 *
	 * @param event the profile change
	 */
	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		clientThread.invoke(this::loadSave);
	}

	/**
	 * Saves on logout and forgets everything tied to the scene that was just left.
	 *
	 * @param event the game state change
	 */
	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		if (state == GameState.LOGGED_IN && !loaded)
			loadSave();

		if (state != GameState.LOGIN_SCREEN && state != GameState.HOPPING)
			return;

		saveNow();
		tracker.clear();
		candidates.clear();
		stepCounter.reset();
		noticeBoard.clear();
		if (state == GameState.LOGIN_SCREEN)
		{
			lastXp.clear();
			lastLevel.clear();
		}
	}

	/**
	 * Tracks a monster that came into view and gives it a chance to turn wild.
	 *
	 * @param event the spawn
	 */
	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		if (speciesOf(npc) == null)
			return;

		candidates.put(npc.getIndex(), npc);
		offer(npc);
	}

	/**
	 * Forgets a monster that left view.
	 *
	 * @param event the despawn
	 */
	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		int index = event.getNpc().getIndex();
		candidates.remove(index);
		tracker.remove(index);
	}

	/**
	 * Trains the team from an XP drop, and rewards level-ups.
	 *
	 * @param event the stat change
	 */
	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		Skill skill = event.getSkill();
		Integer previousXp = lastXp.put(skill, event.getXp());
		Integer previousLevel = lastLevel.put(skill, event.getLevel());
		if (previousXp == null || previousLevel == null || !loaded)
			return;

		game.addSkillXp(skill.name(), event.getXp() - previousXp);
		if (event.getLevel() > previousLevel)
			game.onLevelUp();
	}

	/**
	 * Counts steps and plays back battle lines. The panel is redrawn on the same tick anything changes, so the
	 * step count and XP bars move live; the save is written on its own slower beat.
	 *
	 * @param event the tick
	 */
	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!loaded)
			return;

		ticks++;
		noticeBoard.tick(System.currentTimeMillis());
		countSteps();
		if (!playable())
			tracker.clear();
		else if (ticks % REROLL_TICKS == 0)
			offerRandomCandidate();

		boolean changed = game.takeDirty();
		unsaved |= changed;
		if (changed || ticks % PANEL_TICKS == 0)
			refreshPanel();

		if (unsaved && ticks % SAVE_TICKS == 0)
			saveNow();
	}

	/**
	 * Adds the Catch and Battle options to the right-click menu of a wild monster. Both are client-side entries.
	 *
	 * @param event the menu that just opened
	 */
	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		if (!loaded || tracker.all().isEmpty())
			return;

		Set<Integer> wildInMenu = new LinkedHashSet<>();
		for (MenuEntry entry : event.getMenuEntries())
		{
			NPC npc = entry.getNpc();
			if (npc != null && tracker.get(npc.getIndex()) != null)
				wildInMenu.add(npc.getIndex());
		}

		for (int index : wildInMenu)
			addWildOptions(tracker.get(index));
	}

	private void startOnClientThread()
	{
		loaded = false;
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

		loadSave();
		WorldView view = client.getTopLevelWorldView();
		if (view == null)
			return;

		for (NPC npc : view.npcs())
		{
			if (speciesOf(npc) != null)
				candidates.put(npc.getIndex(), npc);
		}
	}

	private SpeciesTable loadSpecies()
	{
		try (InputStream in = GielinorGoPlugin.class.getResourceAsStream(SpeciesTable.RESOURCE))
		{
			if (in == null)
			{
				log.warn("Gielinor Go: the species list is missing, so no creatures can appear");
				return new SpeciesTable(Collections.emptyList());
			}

			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				return SpeciesTable.load(gson, reader);
			}
		}
		catch (IOException e)
		{
			log.warn("Gielinor Go: could not read the species list", e);
			return new SpeciesTable(Collections.emptyList());
		}
	}

	private void loadSave()
	{
		if (configManager.getRSProfileKey() == null)
			return;

		game.load(persistence.load());
		loaded = true;
		unsaved = false;
		tracker.clear();
		refreshPanel();
	}

	private void saveNow()
	{
		if (!loaded || game == null)
			return;

		unsaved |= game.takeDirty();
		if (!unsaved)
			return;

		persistence.save(game.data());
		unsaved = false;
	}

	private void refreshPanel()
	{
		PanelState state = game.snapshot();
		GoPanel target = panel;
		SwingUtilities.invokeLater(() -> target.update(state));
	}

	/**
	 * Shows a line from the game as floating text, and in the chatbox when that setting is on.
	 *
	 * @param notice the line and whose head it belongs over
	 */
	private void announce(Notice notice)
	{
		if (config.showResults())
			noticeBoard.show(notice, System.currentTimeMillis());

		if (config.chatMessages())
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", notice.text(), null);
	}

	/**
	 * Works out which species a monster is.
	 *
	 * @param npc the monster
	 * @return the species, or null when the monster cannot be fought or is not in the species list
	 */
	private Species speciesOf(NPC npc)
	{
		if (npc == null || npc.getName() == null || npc.getCombatLevel() < 1 || game == null)
			return null;

		return game.speciesFor(Text.removeTags(npc.getName()));
	}

	/** @return whether wild creatures may appear where the player is right now */
	private boolean playable()
	{
		WorldView view = client.getTopLevelWorldView();
		if (view == null || view.isInstance())
			return false;

		if (config.enableInPvp())
			return true;

		Set<WorldType> world = client.getWorldType();
		boolean pvpWorld = world.contains(WorldType.PVP) || world.contains(WorldType.DEADMAN);
		return !pvpWorld && client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) == 0;
	}

	private void offer(NPC npc)
	{
		Species species = speciesOf(npc);
		if (!loaded || species == null || !playable())
			return;

		WorldPoint point = npc.getWorldLocation();
		boolean underground = point != null && Terrain.underground(point.getX(), point.getY());
		WildEntry entry = tracker.offer(npc.getIndex(), species, npc.getCombatLevel(), underground,
			System.currentTimeMillis(), config.wildChance());
		if (entry == null)
			return;

		game.markSeen(entry);
		announce(Notice.creature(entry.npcIndex(), "A wild " + species.name() + " appeared!", Tone.INFO));
	}

	private void offerRandomCandidate()
	{
		if (candidates.isEmpty())
			return;

		List<NPC> pool = new ArrayList<>(candidates.values());
		offer(pool.get(random.nextInt(pool.size())));
	}

	private void countSteps()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
			return;

		WorldPoint point = player.getWorldLocation();
		game.addSteps(stepCounter.advance(point.getX(), point.getY(), point.getPlane()));
	}

	private void addWildOptions(WildEntry entry)
	{
		Species species = entry.species();
		Rarity rarity = species.rarity();
		String name = ColorUtil.wrapWithColorTag(species.name(), rarity.color());
		String target = name + " (Lv " + entry.level() + ")";
		int index = entry.npcIndex();
		JarType standard = game.jarToThrow();
		for (JarType specialty : game.matchingSpecialtyJars(entry))
			addCatchOption(entry, target, specialty);

		addCatchOption(entry, target, standard);
		if (entry.battled())
			return;

		client.getMenu()
			.createMenuEntry(1)
			.setOption("Battle")
			.setTarget(target)
			.setType(MenuAction.RUNELITE)
			.onClick(clicked -> battle(index));
	}

	/**
	 * Adds one Catch option for one jar, showing the odds with that jar.
	 *
	 * @param entry the wild creature
	 * @param target the coloured creature name and level
	 * @param jar the jar this option throws, or null when the player has none
	 */
	private void addCatchOption(WildEntry entry, String target, JarType jar)
	{
		int index = entry.npcIndex();
		String odds = " - no jars";
		if (jar != null)
		{
			double chance = CatchRules.chance(jar, entry.combatLevel(), entry.weakened(), entry.traits());
			odds = " - " + jar.label() + " jar " + percent(chance);
		}

		client.getMenu()
			.createMenuEntry(1)
			.setOption("Catch")
			.setTarget(target + odds)
			.setType(MenuAction.RUNELITE)
			.onClick(clicked -> throwJar(index, jar));
	}

	private static String percent(double chance)
	{
		return Math.round(chance * 100) + "%";
	}

	private void throwJar(int npcIndex, JarType jar)
	{
		WildEntry entry = tracker.get(npcIndex);
		if (entry == null)
			return;

		CatchOutcome outcome = game.tryCatch(entry, jar);
		if (outcome == CatchOutcome.CAUGHT || outcome == CatchOutcome.FLED)
			tracker.remove(npcIndex);

		refreshPanel();
	}

	private void battle(int npcIndex)
	{
		WildEntry entry = tracker.get(npcIndex);
		if (entry == null)
			return;

		BattleResult result = game.battle(entry);
		if (result != null && config.showResults())
			noticeBoard.queue(battleNotices(result, npcIndex));

		refreshPanel();
	}

	/**
	 * Turns a battle into floating lines: hits on the wild creature go over its head, hits taken go over the
	 * player's.
	 *
	 * @param result the battle
	 * @param npcIndex the game's index for the wild creature
	 * @return the lines to play back, one per game tick
	 */
	private static List<Notice> battleNotices(BattleResult result, int npcIndex)
	{
		List<Notice> notices = new ArrayList<>();
		for (BattleLine line : result.summary(BATTLE_LINES))
		{
			if (line.overPlayer())
				notices.add(Notice.player(line.text(), line.tone()));
			else
				notices.add(Notice.creature(npcIndex, line.text(), line.tone()));
		}

		return notices;
	}

	/** @return the panel's actions, each carried over to the game thread and followed by a panel refresh */
	private PanelActions panelActions()
	{
		return new PanelActions()
		{
			@Override
			public void setLead(int teamIndex)
			{
				onGameThread(() -> game.setLead(teamIndex));
			}

			@Override
			public void moveToStorage(int teamIndex)
			{
				onGameThread(() -> game.moveToStorage(teamIndex));
			}

			@Override
			public void moveToTeam(int storageIndex)
			{
				onGameThread(() -> game.moveToTeam(storageIndex));
			}

			@Override
			public void release(int storageIndex)
			{
				onGameThread(() -> game.release(storageIndex));
			}

			@Override
			public void selectJar(JarType type)
			{
				onGameThread(() -> game.selectJar(type));
			}
		};
	}

	private void onGameThread(Runnable action)
	{
		clientThread.invoke(() ->
		{
			if (!loaded)
				return;

			action.run();
			refreshPanel();
		});
	}
}
