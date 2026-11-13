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
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.events.UserJoin;
import net.runelite.client.party.events.UserPart;
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
 * Entry point: watches monsters, XP drops and steps in the real game and feeds them to {@link GoGame}. Its menu
 * options and its follower are client-side only and send nothing to the game. The only things that leave this
 * computer are creatures and eggs: the team, the eggs carried and whatever is put in a trade, sent through
 * RuneLite's party service to the people in the player's own party so friends can battle and trade.
 */
@PluginDescriptor(
	name = "Gielinor Go",
	description = "A creature-raising side game: catch wild monsters, train them with your own XP gains, and win"
		+ " auto-battles while you play",
	tags = {"creature", "catch", "collect", "battle", "train", "team", "minigame", "fun", "monster", "follower",
		"pet", "party"}
)
public class GielinorGoPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(GielinorGoPlugin.class);

	/** Game ticks between extra chances for a monster already in view to turn wild. */
	private static final int REROLL_TICKS = 25;

	/** Farthest from the player, in tiles, a monster may be and still turn wild: its creature would leave beyond. */
	private static final int SPAWN_RANGE = WildLife.LEAVE_TILES - 5;

	/** Game ticks between side panel refreshes while nothing changes, so resting health still creeps up. */
	private static final int PANEL_TICKS = 5;

	/** Game ticks between saves of changed progress. */
	private static final int SAVE_TICKS = 50;

	/** Most battle log lines played back on screen. */
	private static final int BATTLE_LINES = 6;

	/** Game ticks between two hellos to the party, so a burst of people joining is answered once. */
	private static final int HELLO_TICKS = 5;

	/** Game ticks between hellos sent just to keep friends' view of the team fresh: five minutes. */
	private static final int HELLO_REFRESH_TICKS = 500;

	/** Longest name a player can have in the game. */
	private static final int MAX_NAME_LENGTH = 12;

	/** What a party member is called until the party service knows their name. */
	private static final String UNNAMED_FRIEND = "your friend";

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

	@Inject
	private RenderCallbackManager renderCallbackManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private PartyService partyService;

	@Inject
	private WSClient wsClient;

	private final Random random = new Random();
	private final Map<Integer, NPC> candidates = new HashMap<>();
	private final Map<Skill, Integer> lastXp = new EnumMap<>(Skill.class);
	private final Map<Skill, Integer> lastLevel = new EnumMap<>(Skill.class);
	private final StepCounter stepCounter = new StepCounter();
	private final BankSpots bankSpots = new BankSpots();
	private final Map<Integer, Boolean> bankObjects = new HashMap<>();
	private WildTracker tracker = new WildTracker(random);
	private final SpawnClock spawnClock = SpawnClock.system();
	private SpawnCalendar calendar;
	private GoGame game;
	private GoPersistence persistence;
	private WildOverlay wildOverlay;
	private WildBodies bodies;
	private final NoticeBoard noticeBoard = new NoticeBoard();
	private List<Notice> heldNotices;
	private FloatingTextOverlay floatingText;
	private FollowerTagOverlay followerTag;
	/** The lead creature's nickname for the overlay, kept up to date on the game thread. Null for none. */
	private volatile String leadTag;
	private GoPanel panel;
	private PortraitService portraits;
	private FollowerService follower;
	private CatchEffects effects;
	private JarFlightOverlay jarFlight;
	private BattleStage stage;
	private ArenaStage arena;
	private BattleDirector director;
	private BattlePromptOverlay promptOverlay;
	private BattleHotkeys hotkeys;
	private volatile boolean choiceOpen;
	private int steerWild = Notice.PLAYER;
	private WorldPoint steerHome;
	private List<Notice> pendingLines = new ArrayList<>();
	private int pendingTicks;
	private FriendBattles friends;
	private FriendTrades trades;
	private final MessageRate partyRate = new MessageRate();
	private boolean helloDue;
	private boolean helloAsksBack;
	private int lastHelloTick;
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
		SpeciesTable species = loadSpecies();
		game = new GoGame(species, loadEvolutions(species), loadChampions(species), random,
			System::currentTimeMillis, this::announce);
		calendar = new SpawnCalendar(species);
		game.milestones(loadMilestones(species));
		game.elite(loadElite(species));
		game.moves(loadMoves());
		game.today(() -> calendar.today(config.spawnEvents(), spawnClock));
		friends = new FriendBattles(species, random, System::currentTimeMillis, this::sendToParty, this::announce)
			.withMoves(game.moveTable());
		trades = new FriendTrades(species, game, random, System::currentTimeMillis, this::sendToParty,
			this::announce);
		game.tradeLocks(trades);
		wsClient.registerMessage(GielinorGoPartyMessage.class);
		lastHelloTick = -HELLO_TICKS;
		persistence = new GoPersistence(configManager, gson);
		AnimationTable animations = loadAnimations();
		bodies = new WildBodies(client, animations, random);
		wildOverlay = new WildOverlay(client, bodies, config, outlineRenderer);
		floatingText = new FloatingTextOverlay(client, noticeBoard, this::spotFor, this::tagFor);
		overlayManager.add(wildOverlay);
		overlayManager.add(floatingText);
		portraits = new PortraitService(client, clientThread, game::speciesFor);
		follower = new FollowerService(client, animations, game::speciesFor);
		followerTag = new FollowerTagOverlay(client, follower, () -> follower.showingAs() ? null : leadTag);
		overlayManager.add(followerTag);
		effects = new CatchEffects(client, config::catchEffects);
		jarFlight = new JarFlightOverlay(client);
		overlayManager.add(jarFlight);
		stage = new BattleStage(client, follower, System::currentTimeMillis);
		arena = new ArenaStage(client, follower, animations, game::speciesFor);
		director = new BattleDirector(game, config::turnSeconds, battleListener());
		promptOverlay = new BattlePromptOverlay();
		overlayManager.add(promptOverlay);
		hotkeys = new BattleHotkeys(config::attackKey, config::switchKey, config::catchKey, config::runKey,
			config::itemKey, () -> choiceOpen, this::hotkeyPressed);
		keyManager.registerKeyListener(hotkeys);
		panel = new GoPanel(panelActions(), portraits);
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
		keyManager.unregisterKeyListener(hotkeys);
		overlayManager.remove(promptOverlay);
		choiceOpen = false;
		clientThread.invoke(director::end);
		clientThread.invoke(this::saveNow);
		clientThread.invoke(stage::clear);
		clientThread.invoke(arena::clear);
		clientThread.invoke(bodies::clear);
		clientThread.invoke(follower::remove);
		clientThread.invoke(effects::clear);
		clientThread.invoke(jarFlight::clear);
		overlayManager.remove(jarFlight);
		wsClient.unregisterMessage(GielinorGoPartyMessage.class);
		overlayManager.remove(wildOverlay);
		overlayManager.remove(floatingText);
		overlayManager.remove(followerTag);
		leadTag = null;
		noticeBoard.clear();
		clientToolbar.removeNavigation(navButton);
		portraits.clear();
		tracker.clear();
		candidates.clear();
		lastXp.clear();
		lastLevel.clear();
		stepCounter.reset();
	}

	/**
	 * Finds where a notice floats when it is not over the player's head: over a wild creature's body, or over a body
	 * of a battle that is acted out on screen, whichever stage is playing. Called from the render thread.
	 *
	 * @param notice the notice
	 * @return the place of its body, or null when none is drawn and it floats over the player
	 */
	private StageAnchors.Spot spotFor(Notice notice)
	{
		Notice.Anchor anchor = notice.anchor();
		if (anchor == Notice.Anchor.HEAD)
			return null;

		if (anchor == Notice.Anchor.WILD || (anchor == Notice.Anchor.STAGE_FOE && !notice.overPlayer()))
			return bodies.spotOf(notice.wildId());

		BattleStage wild = stage;
		StageAnchors.Spot spot = wild == null ? null : wild.spotOf(anchor);
		ArenaStage team = arena;
		return spot != null || team == null ? spot : team.spotOf(anchor);
	}

	/**
	 * Finds the status tag a body of the battle on stage shows. Called from the render thread.
	 *
	 * @param anchor which body
	 * @return its status, or null for none
	 */
	private Status tagFor(Notice.Anchor anchor)
	{
		BattleStage wild = stage;
		Status status = wild == null ? null : wild.statusOf(anchor);
		ArenaStage team = arena;
		return status != null || team == null ? status : team.statusOf(anchor);
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

		if (state == GameState.LOADING)
		{
			bodies.sceneLoading();
			stage.clear();
			arena.clear();
			follower.sceneLoading();
			bankSpots.clear();
		}

		if (state != GameState.LOGIN_SCREEN && state != GameState.HOPPING)
			return;

		director.end();
		choiceOpen = false;
		promptOverlay.show(Collections.emptyList());
		pendingLines.clear();
		saveNow();
		trades.clear();
		stage.clear();
		arena.clear();
		follower.hide();
		effects.clear();
		jarFlight.clear();
		bodies.clear();
		tracker.clear();
		bankSpots.clear();
		game.setBankNear(false);
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
	 * Remembers a banker that came into view, so the team can be changed near it.
	 *
	 * @param event the spawn
	 */
	@Subscribe
	public void onBankerSpawned(NpcSpawned event)
	{
		try
		{
			NPC npc = event.getNpc();
			NPCComposition composition = npc.getTransformedComposition();
			WorldPoint point = npc.getWorldLocation();
			if (composition != null && point != null && offersBank(composition.getActions()))
				bankSpots.add(npc, point.getX(), point.getY(), point.getPlane());
		}
		catch (RuntimeException e)
		{
			log.debug("Gielinor Go: could not check a monster for a bank", e);
		}
	}

	/**
	 * Forgets a banker that left view.
	 *
	 * @param event the despawn
	 */
	@Subscribe
	public void onBankerDespawned(NpcDespawned event)
	{
		bankSpots.remove(event.getNpc());
	}

	/**
	 * Remembers a bank booth or chest that came into view, so the team can be changed near it. What an object id
	 * offers is looked up once and kept.
	 *
	 * @param event the spawn
	 */
	@Subscribe
	public void onBankObjectSpawned(GameObjectSpawned event)
	{
		try
		{
			GameObject object = event.getGameObject();
			if (!bankObjects.computeIfAbsent(object.getId(), this::objectOffersBank))
				return;

			WorldPoint point = object.getWorldLocation();
			if (point != null)
				bankSpots.add(object, point.getX(), point.getY(), point.getPlane());
		}
		catch (RuntimeException e)
		{
			log.debug("Gielinor Go: could not check an object for a bank", e);
		}
	}

	/**
	 * Forgets a bank booth or chest that left view.
	 *
	 * @param event the despawn
	 */
	@Subscribe
	public void onBankObjectDespawned(GameObjectDespawned event)
	{
		bankSpots.remove(event.getGameObject());
	}

	private boolean objectOffersBank(int id)
	{
		ObjectComposition composition = client.getObjectDefinition(id);
		if (composition != null && composition.getImpostorIds() != null)
			composition = composition.getImpostor();

		return composition != null && offersBank(composition.getActions());
	}

	private static boolean offersBank(String[] actions)
	{
		if (actions == null)
			return false;

		for (String action : actions)
		{
			if ("Bank".equals(action))
				return true;
		}

		return false;
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
		tracker.leftView(index, System.currentTimeMillis());
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
		steering();
		battleScene();
		noticeBoard.tick(System.currentTimeMillis());
		portraits.tick();
		countSteps();
		updateFollower();
		greetParty();
		wildTick();
		if (!playable())
		{
			bodies.clear();
			tracker.clearWild(System.currentTimeMillis());
		}
		else if (ticks % REROLL_TICKS == 0)
		{
			offerRandomCandidate();
		}

		boolean changed = game.takeDirty();
		unsaved |= changed;
		if (changed || ticks % PANEL_TICKS == 0)
			refreshPanel();

		if (unsaved && ticks % SAVE_TICKS == 0)
			saveNow();
	}

	/**
	 * Moves the follower and the catch effects on a little. Client ticks come 50 times a second, so both are
	 * smooth.
	 *
	 * @param event the client tick
	 */
	@Subscribe
	public void onClientTick(ClientTick event)
	{
		effects.clientTick();
		jarFlight.clientTick();
		if (!loaded)
			return;

		bodies.clientTick(System.currentTimeMillis());
		wildMenu();
		follower.clientTick();
		stage.clientTick();
		arena.clientTick();
	}

	/**
	 * Handles a message another Gielinor Go player sent through the party. Party messages arrive on a network
	 * thread, so the work is handed to the game thread.
	 *
	 * @param message the message
	 */
	@Subscribe
	public void onGielinorGoPartyMessage(GielinorGoPartyMessage message)
	{
		clientThread.invoke(() -> receiveFromParty(message));
	}

	/**
	 * Says hello when someone joins the party, so they learn that this player plays too.
	 *
	 * @param event the member who joined
	 */
	@Subscribe
	public void onUserJoin(UserJoin event)
	{
		clientThread.invoke(this::queueHello);
	}

	/**
	 * Forgets a friend who left the party.
	 *
	 * @param event the member who left
	 */
	@Subscribe
	public void onUserPart(UserPart event)
	{
		clientThread.invoke(() -> forgetFriend(event.getMemberId()));
	}

	/**
	 * Starts over with nobody known when the player joins or leaves a party.
	 *
	 * @param event the party change
	 */
	@Subscribe
	public void onPartyChanged(PartyChanged event)
	{
		clientThread.invoke(this::forgetParty);
	}

	/**
	 * Moves the wild creatures' lives on by one game tick, and lets go of the ones whose bodies are over.
	 */
	private void wildTick()
	{
		long now = System.currentTimeMillis();
		for (int wildId : bodies.gameTick(now))
		{
			tracker.remove(wildId);
			stage.forget(wildId);
		}
	}

	/**
	 * Adds the Catch and Battle entries to the menu while the mouse is over a wild creature's body, with the one
	 * the player most likely wants on top, so a plain click does that and the right-click menu offers the rest. The
	 * entries are client-side only and nothing is sent to the game. They are added only while the mouse is over the
	 * game view (the menu then has a "Walk here"), never over an interface, and never while the menu is open. A click
	 * on the body therefore never walks the player to the tile behind it.
	 */
	private void wildMenu()
	{
		try
		{
			if (bodies.isEmpty() || client.isMenuOpen() || client.getMouseCanvasPosition() == null)
				return;

			MenuEntry[] present = client.getMenu().getMenuEntries();
			boolean overScene = false;
			for (MenuEntry entry : present)
			{
				if (entry.getType() == MenuAction.RUNELITE && WildMenu.isOurs(entry.getOption()))
					return;

				overScene |= entry.getType() == MenuAction.WALK;
			}

			WildBody hit = overScene ? bodies.under(client.getMouseCanvasPosition()) : null;
			if (hit != null && tracker.get(hit.entry().wildId()) != null)
				addWildOptions(hit.entry());
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not add the wild creature's menu entries", e);
		}
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

	private EvolutionTable loadEvolutions(SpeciesTable species)
	{
		try (InputStream in = GielinorGoPlugin.class.getResourceAsStream(EvolutionTable.RESOURCE))
		{
			if (in == null)
				return EvolutionTable.empty();

			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				return EvolutionTable.load(gson, reader, species);
			}
		}
		catch (IOException e)
		{
			log.warn("Gielinor Go: could not read the evolution list", e);
			return EvolutionTable.empty();
		}
	}

	private Milestones loadMilestones(SpeciesTable species)
	{
		try (InputStream in = GielinorGoPlugin.class.getResourceAsStream(Family.RESOURCE))
		{
			if (in == null)
				return new Milestones(species, Collections.emptyList());

			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				return new Milestones(species, Family.load(gson, reader));
			}
		}
		catch (IOException e)
		{
			log.warn("Gielinor Go: could not read the family list", e);
			return new Milestones(species, Collections.emptyList());
		}
	}

	private EliteTable loadElite(SpeciesTable species)
	{
		try (InputStream in = GielinorGoPlugin.class.getResourceAsStream(EliteTable.RESOURCE))
		{
			if (in == null)
				return EliteTable.empty();

			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				return EliteTable.load(gson, reader, species);
			}
		}
		catch (IOException e)
		{
			log.warn("Gielinor Go: could not read the Elite challenge", e);
			return EliteTable.empty();
		}
	}

	private ChampionTable loadChampions(SpeciesTable species)
	{
		try (InputStream in = GielinorGoPlugin.class.getResourceAsStream(ChampionTable.RESOURCE))
		{
			if (in == null)
				return ChampionTable.empty();

			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				return ChampionTable.load(gson, reader, species);
			}
		}
		catch (IOException e)
		{
			log.warn("Gielinor Go: could not read the champion list", e);
			return ChampionTable.empty();
		}
	}

	private MoveTable loadMoves()
	{
		try (InputStream in = GielinorGoPlugin.class.getResourceAsStream(MoveTable.RESOURCE))
		{
			if (in == null)
				return MoveTable.empty();

			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				return MoveTable.load(gson, reader);
			}
		}
		catch (IOException e)
		{
			log.warn("Gielinor Go: could not read the move list", e);
			return MoveTable.empty();
		}
	}

	private AnimationTable loadAnimations()
	{
		try (InputStream in = GielinorGoPlugin.class.getResourceAsStream(AnimationTable.RESOURCE))
		{
			if (in == null)
				return AnimationTable.empty();

			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				return AnimationTable.load(gson, reader);
			}
		}
		catch (IOException e)
		{
			log.warn("Gielinor Go: could not read the animation list", e);
			return AnimationTable.empty();
		}
	}

	private void loadSave()
	{
		if (configManager.getRSProfileKey() == null)
			return;

		director.end();
		game.load(persistence.load());
		loaded = true;
		helloDue = true;
		helloAsksBack = true;
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
		PanelState state = game.snapshot().withBattle(director.view());
		FriendsState party = friendsState();
		GoPanel target = panel;
		SwingUtilities.invokeLater(() -> target.update(state));
		SwingUtilities.invokeLater(() -> target.updateFriends(party));
	}

	/** @return the party members who also play, named as the party service names them */
	private FriendsState friendsState()
	{
		if (!partyService.isInParty())
			return FriendsState.none();

		Map<Long, String> names = new HashMap<>();
		for (PartyMember member : partyService.getMembers())
			names.put(member.getMemberId(), friendName(member));

		List<FriendView> views = new ArrayList<>();
		for (FriendView view : friends.views(names))
			views.add(view.withTrade(trades.offerFrom(view.memberId()), trades.waitingOn(view.memberId())));

		return new FriendsState(true, views);
	}

	/**
	 * Works out what to call a party member in floating text and on their card.
	 *
	 * @param member the party member, or null when they already left
	 * @return their name in the game without tags, or a stand-in while the party service does not know it
	 */
	private static String friendName(PartyMember member)
	{
		String raw = member == null ? null : member.getDisplayName();
		String name = raw == null ? "" : Text.removeTags(raw).trim();
		if (name.isEmpty() || name.equals("<unknown>"))
			return UNNAMED_FRIEND;

		return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
	}

	private void sendToParty(GielinorGoPartyMessage message)
	{
		if (partyService.isInParty())
			partyService.send(message);
	}

	private void queueHello()
	{
		helloDue = true;
	}

	/**
	 * Tells the party this player plays: a few ticks after logging in or after someone joined, and every few
	 * minutes after that. Also gives up on a challenge nobody answered.
	 */
	private void greetParty()
	{
		boolean battleExpired = friends.expire();
		boolean tradeExpired = trades.expire();
		if (battleExpired || tradeExpired)
			refreshPanel();

		boolean wanted = friends.takeHelloWanted();
		boolean traded = trades.takeTraded();
		if (ticks % HELLO_REFRESH_TICKS == 0 || wanted || traded)
			helloDue = true;

		if (!helloDue || ticks - lastHelloTick < HELLO_TICKS || !partyService.isInParty())
			return;

		friends.hello(game.data(), helloAsksBack);
		helloDue = false;
		helloAsksBack = false;
		lastHelloTick = ticks;
	}

	private void forgetFriend(long memberId)
	{
		friends.forget(memberId);
		trades.forget(memberId);
		if (loaded)
			refreshPanel();
	}

	private void forgetParty()
	{
		friends.clear();
		trades.clear();
		partyRate.clear();
		helloDue = true;
		helloAsksBack = true;
		if (loaded)
			refreshPanel();
	}

	/**
	 * Acts on a message from another player in the party: notes who plays, and plays out a battle when the
	 * message starts one.
	 *
	 * @param message the message; nothing in it is trusted
	 */
	private void receiveFromParty(GielinorGoPartyMessage message)
	{
		PartyMember me = partyService.getLocalMember();
		if (!saveLive() || me == null || message.getMemberId() == me.getMemberId())
			return;

		if (!partyRate.allow(message.getMemberId(), System.currentTimeMillis()))
			return;

		String sender = friendName(partyService.getMemberById(message.getMemberId()));
		heldNotices = new ArrayList<>();
		try
		{
			TeamBattleResult result = friends.receive(message, me.getMemberId(), sender, game.data(),
				config.friendBattles());
			boolean changed = friends.takeChanged() | result != null;
			changed |= trades.receive(message, me.getMemberId(), sender);
			saveNow();
			playTeamBattle(result, !GielinorGoPartyMessage.CHALLENGE.equals(message.kind()) || !interacting());
			if (changed)
				refreshPanel();
		}
		finally
		{
			heldNotices = null;
		}
	}

	/**
	 * Tells whether the player is in the game with a save loaded, so a change to the save will be written. At the
	 * login screen or while hopping worlds nothing is saved, so no trade may run then.
	 *
	 * @return true while logged in (or between two scenes) with the save loaded
	 */
	private boolean saveLive()
	{
		GameState state = client.getGameState();
		return loaded && (state == GameState.LOGGED_IN || state == GameState.LOADING);
	}

	private void challengeFriend(long memberId)
	{
		friends.challenge(memberId, friendName(partyService.getMemberById(memberId)), game.data());
	}

	/**
	 * Offers a friend a trade the player set up in the panel. What the player picked is matched again against the
	 * save, because the save may have changed since the panel was drawn; if it no longer matches, nothing is sent.
	 *
	 * @param memberId the friend's party member id
	 * @param give what the player picked to give, as the panel showed it
	 * @param wantIndex the place of what to ask for in what the friend has shown, or -1 for nothing
	 * @param wantLabel the label the panel showed at that place
	 */
	private void offerTrade(long memberId, TradePick give, int wantIndex, String wantLabel)
	{
		SaveData data = game.data();
		String name = friendName(partyService.getMemberById(memberId));
		GielinorGoPartyMessage.Item want = null;
		if (wantIndex >= 0)
		{
			want = trades.friendItem(friends.tradeable(memberId), wantIndex, wantLabel);
			if (want == null)
			{
				announce(Notice.player(name + " no longer shows that. Pick again", Tone.BAD));
				return;
			}
		}

		if (!give.matches(game.snapshot()))
		{
			announce(Notice.player("What you picked has changed. Pick again", Tone.BAD));
			return;
		}

		if (give.from() == Holding.EGG)
		{
			trades.offer(memberId, name, data.eggs.get(give.index()), want);
			return;
		}

		List<Creature> kept = give.from() == Holding.TEAM ? data.team : data.storage;
		trades.offer(memberId, name, kept.get(give.index()), want);
	}

	/**
	 * Renames the creature a panel pick points at, once it is checked that the same creature is still there.
	 *
	 * @param pick the creature as the panel showed it
	 * @param name the typed name
	 */
	private void rename(TradePick pick, String name)
	{
		if (!pick.matches(game.snapshot()))
		{
			announce(Notice.player("What you picked has changed. Pick again", Tone.BAD));
			return;
		}

		SaveData data = game.data();
		List<Creature> kept = pick.from() == Holding.TEAM ? data.team : data.storage;
		game.rename(kept.get(pick.index()), name);
	}

	/**
	 * Settles the move the creature a panel pick points at waits to learn, once it is checked that the same creature
	 * is still there. A battle the player steers keeps the moves it started with.
	 *
	 * @param pick the creature as the panel showed it
	 * @param forgetIndex the place of the known move to forget, or -1 to skip the new move
	 */
	private void resolveMove(TradePick pick, int forgetIndex)
	{
		if (!pick.matches(game.snapshot()))
		{
			announce(Notice.player("What you picked has changed. Pick again", Tone.BAD));
			return;
		}

		SaveData data = game.data();
		List<Creature> kept = pick.from() == Holding.TEAM ? data.team : data.storage;
		game.resolveMove(kept.get(pick.index()), forgetIndex);
	}

	/**
	 * Uses a healing item on the creature a panel pick points at, once it is checked that the same creature is
	 * still there.
	 *
	 * @param pick the creature as the panel showed it
	 * @param type the item to use
	 */
	private void useItem(TradePick pick, ItemType type)
	{
		if (battleOpen())
			return;

		if (!pick.matches(game.snapshot()))
		{
			announce(Notice.player("What you picked has changed. Pick again", Tone.BAD));
			return;
		}

		SaveData data = game.data();
		List<Creature> kept = pick.from() == Holding.TEAM ? data.team : data.storage;
		game.useItem(kept.get(pick.index()), type);
	}

	private void answerTrade(long memberId, boolean accept)
	{
		trades.answer(memberId, friendName(partyService.getMemberById(memberId)), accept);
		saveNow();
	}

	/**
	 * Shows a line from the game as floating text, and in the chatbox when that setting is on.
	 *
	 * @param notice the line and whose head it belongs over
	 */
	private void announce(Notice notice)
	{
		if (heldNotices != null)
		{
			heldNotices.add(notice);
			return;
		}

		if (config.showResults())
			noticeBoard.show(notice, System.currentTimeMillis());

		if (config.chatMessages())
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", notice.text(), null);
	}

	/**
	 * Works out which species a monster is, for turning it wild.
	 *
	 * @param npc the monster
	 * @return the species, or null when the monster cannot be fought, is not in the species list or is a quest-only
	 *     species (those never turn wild)
	 */
	private Species speciesOf(NPC npc)
	{
		if (npc == null || npc.getName() == null || npc.getCombatLevel() < 1 || game == null)
			return null;

		Species found = game.speciesFor(Text.removeTags(npc.getName()));
		return found == null || found.questOnly() ? null : found;
	}

	/** @return whether wild creatures may appear where the player is right now */
	private boolean playable()
	{
		WorldView view = client.getTopLevelWorldView();
		return view != null && !view.isInstance() && !hiddenForPvp();
	}

	/** @return whether the player is somewhere other players can attack them, with the setting to play there off */
	private boolean hiddenForPvp()
	{
		if (config.enableInPvp())
			return false;

		Set<WorldType> world = client.getWorldType();
		boolean pvpWorld = world.contains(WorldType.PVP) || world.contains(WorldType.DEADMAN);
		return pvpWorld || client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) != 0;
	}

	/**
	 * Tells the follower which creature leads the team and where the player stands. Unlike wild creatures, the
	 * follower also shows in instances such as the player's house.
	 */
	private void updateFollower()
	{
		Creature lead = config.showFollower() && !hiddenForPvp() ? game.data().lead() : null;
		leadTag = lead != null && config.showLeadName()
			? FollowerTagOverlay.nameFor(lead.nickname, game.speciesNameOf(lead)) : null;
		follower.gameTick(lead == null ? null : lead.species, lead == null ? 0 : lead.npcId,
			lead != null && lead.gilded);
	}

	private void offer(NPC npc)
	{
		Species species = speciesOf(npc);
		if (!loaded || species == null || !playable() || !nearPlayer(npc))
			return;

		long now = System.currentTimeMillis();
		WorldPoint point = npc.getWorldLocation();
		boolean underground = point != null && Terrain.underground(point.getX(), point.getY());
		boolean events = config.spawnEvents();
		WildEntry entry = tracker.offer(npc.getIndex(), npc.getId(), species, npc.getCombatLevel(), underground,
			now, calendar.chance(species, config.wildChance(), events, spawnClock),
			game.gildedOneIn(calendar.gildedOneIn(species, events, spawnClock)), events);
		if (entry == null)
			return;

		if (bodies.spawn(entry, npc, now) == null)
		{
			tracker.withdraw(entry);
			return;
		}

		game.markSeen(entry);
		String kind = entry.gilded() ? "A gilded " : entry.roaming() ? "A roaming " : "A wild ";
		announce(Notice.creature(entry.wildId(), kind + species.name() + " appeared!",
			entry.gilded() || entry.roaming() ? Tone.FIND : Tone.INFO));
	}

	/**
	 * Tells whether a monster is close enough to the player for a wild creature to appear beside it: a creature that
	 * would leave at once for being too far away never appears.
	 *
	 * @param npc the monster
	 * @return whether it is on the player's plane and within {@link #SPAWN_RANGE} tiles
	 */
	private boolean nearPlayer(NPC npc)
	{
		Player player = client.getLocalPlayer();
		WorldPoint here = player == null ? null : player.getWorldLocation();
		WorldPoint there = npc.getWorldLocation();
		return here != null && there != null && here.getPlane() == there.getPlane()
			&& here.distanceTo2D(there) <= SPAWN_RANGE;
	}

	private void offerRandomCandidate()
	{
		long now = System.currentTimeMillis();
		List<NPC> pool = new ArrayList<>();
		for (NPC npc : candidates.values())
		{
			if (tracker.mayRoll(npc.getIndex(), npc.getId(), now) && nearPlayer(npc))
				pool.add(npc);
		}

		if (!pool.isEmpty())
			offer(pool.get(random.nextInt(pool.size())));
	}

	private void countSteps()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
			return;

		WorldPoint point = player.getWorldLocation();
		game.addSteps(stepCounter.advance(point.getX(), point.getY(), point.getPlane()));
		if (game.moveTo(point.getX(), point.getY(), point.getPlane()))
			refreshPanel();

		bankSpots.forgetFar(point.getX(), point.getY());
		game.setBankNear(bankSpots.near(point.getX(), point.getY(), point.getPlane()));
	}

	private void addWildOptions(WildEntry entry)
	{
		Species species = entry.species();
		Rarity rarity = species.rarity();
		String name = ColorUtil.wrapWithColorTag(species.name(), rarity.color());
		String target = entry.prefix() + name + " (Lv " + entry.level() + ")";
		int wildId = entry.wildId();
		WildBody body = bodies.get(wildId);
		boolean withBattle = !entry.battled() && !director.active() && body != null && !body.life().leaving();
		List<WildMenu.Item> items = WildMenu.topFirst(game.jarToThrow(), game.matchingSpecialtyJars(entry),
			withBattle, WildMenu.leftClickCatches(knockedOut(wildId)));
		for (int i = items.size() - 1; i >= 0; i--)
		{
			WildMenu.Item item = items.get(i);
			if (item.isCatch())
				addCatchOption(entry, target, item.jar());
			else
				addBattleOption(wildId, target);
		}
	}

	private void addBattleOption(int wildId, String target)
	{
		client.getMenu()
			.createMenuEntry(-1)
			.setOption(WildMenu.BATTLE)
			.setTarget(target)
			.setType(MenuAction.RUNELITE)
			.onClick(clicked -> battle(wildId));
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
		int wildId = entry.wildId();
		String odds = " - no jars";
		if (jar != null)
		{
			double chance = inBattleWith(entry)
				? CatchRules.inBattle(jar, entry.combatLevel(), entry.traits(), foeHealthLeft())
				: CatchRules.chance(jar, entry.combatLevel(), entry.weakened(), entry.traits(), knockedOut(wildId));
			odds = " - " + jar.label() + " jar " + percent(chance);
		}

		client.getMenu()
			.createMenuEntry(-1)
			.setOption(WildMenu.CATCH)
			.setTarget(target + odds)
			.setType(MenuAction.RUNELITE)
			.onClick(clicked -> throwJar(wildId, jar));
	}

	/**
	 * @param wildId the wild creature's id
	 * @return whether it lies knocked out after losing a battle, which makes it easier to catch
	 */
	private boolean knockedOut(int wildId)
	{
		WildBody body = bodies.get(wildId);
		return body != null && body.life().knockedOut();
	}

	private static String percent(double chance)
	{
		return Math.round(chance * 100) + "%";
	}

	private void throwJar(int wildId, JarType jar)
	{
		WildEntry entry = tracker.get(wildId);
		if (entry == null)
			return;

		if (inBattleWith(entry))
		{
			chooseBattle(BattleChoice.catching(jar));
			return;
		}

		CatchOutcome outcome = game.tryCatch(entry, jar, knockedOut(wildId));
		if (outcome == CatchOutcome.CAUGHT || outcome == CatchOutcome.FLED)
			tracker.remove(wildId);

		showCatch(outcome, wildId, jar);
		refreshPanel();
	}

	/**
	 * Acts a throw out on screen, when that setting is on: the player throws whenever a jar was really used and
	 * then reacts to the result, and the creature's body reacts when the jar lands: a caught one shrinks into the
	 * jar, one that breaks free attacks and stands again, one that flees runs off fading out. With the setting off a
	 * caught or fled creature simply disappears.
	 *
	 * @param outcome what the throw led to
	 * @param wildId the wild creature's id
	 * @param jar the jar that was thrown
	 */
	private void showCatch(CatchOutcome outcome, int wildId, JarType jar)
	{
		boolean thrown = outcome != CatchOutcome.NO_JARS && outcome != CatchOutcome.FULL;
		if (!thrown)
			return;

		WildBody body = bodies.get(wildId);
		boolean played = config.catchEffects() && body != null && body.beginCatch(outcome);
		if (config.catchEffects() && effects.throwJar())
		{
			effects.react(outcome);
			jarFlight.start(jar, () -> bodies.spotOf(wildId));
		}

		boolean gone = outcome == CatchOutcome.CAUGHT || outcome == CatchOutcome.FLED;
		if (gone && !played)
		{
			bodies.remove(wildId, System.currentTimeMillis());
			stage.forget(wildId);
		}
	}

	private void battle(int wildId)
	{
		WildEntry entry = tracker.get(wildId);
		WildBody body = bodies.get(wildId);
		if (entry == null || body == null || body.life().leaving() || director.active())
			return;

		if (config.chooseBattleActions())
		{
			startSteeredBattle(wildId, entry);
			return;
		}

		heldNotices = new ArrayList<>();
		BattleResult result = game.battle(entry);
		List<BattleLine> lines = result == null ? null : result.playbackSummary(BATTLE_LINES);
		boolean staged = false;
		if (result != null && config.battleAnimations() && !arena.active() && !stage.active())
		{
			staged = result.switches() > 0 ? stageTeamBattle(body, result, lines)
				: stage.start(body, lines, result.won());
			noteUnreachable(wildId, body);
		}

		playBack(lines, wildId, staged);
		refreshPanel();
	}

	/**
	 * Says once for a wild creature that a battle with it is shown as text because the lead has no way to get to it.
	 *
	 * @param wildId the wild creature's id
	 * @param body its body
	 */
	private void noteUnreachable(int wildId, WildBody body)
	{
		if (stage.takeUnreachable() && body.noteUnreachable())
			announce(Notice.creature(wildId, "Can't reach it, so the battle is shown as text", Tone.INFO));
	}

	/**
	 * Acts out an automatic battle in which creatures stepped in, through the same open timeline a battle the player
	 * steers uses: the face-off, then every beat at once ({@link WildScript}), then the end. The real lead is shown
	 * again when the scene ends.
	 *
	 * @param body the wild creature's body
	 * @param result the battle
	 * @param lines the lines of the battle, as {@link WildScript} cut them
	 * @return whether the battle is on stage, so its text waits for the scene
	 */
	private boolean stageTeamBattle(WildBody body, BattleResult result, List<BattleLine> lines)
	{
		if (!stage.startOpen(body, lines.subList(0, 1)))
			return false;

		stage.playScript(WildScript.of(result), result.won(), result.opener(), result.leadOpened());
		return true;
	}

	/**
	 * Moves a battle that is acted out on screen on by one game tick, and lets its text start once the lead creature
	 * has walked up to the wild one. Called before the floating text moves on, so the first line and the first blow
	 * come on the same tick. A battle that ends early, or is switched off, lets its text out at once.
	 */
	private void battleScene()
	{
		if (stage.active() && !config.battleAnimations())
			stage.clear();

		if (arena.active() && !config.battleAnimations())
			arena.clear();

		stage.gameTick();
		if (pendingTicks > 0 && --pendingTicks == 0)
		{
			noticeBoard.queue(pendingLines);
			pendingLines = new ArrayList<>();
		}

		long now = System.currentTimeMillis();
		for (Notice line : arena.gameTick())
		{
			if (config.showResults())
				noticeBoard.show(line, now);
		}

		if (noticeBoard.holding() && (stage.linesDue() || (!stage.active() && !arena.active())))
			noticeBoard.release();
	}

	private void challenge(String championId)
	{
		if (director.active())
			return;

		if (config.chooseBattleActions())
		{
			startSteeredChallenge(championId);
			return;
		}

		heldNotices = new ArrayList<>();
		playTeamBattle(game.challenge(championId), true);
	}

	private void eliteChallenge()
	{
		if (battleOpen())
			return;

		heldNotices = new ArrayList<>();
		playTeamBattle(game.eliteChallenge(), true);
	}

	private void eliteContinue()
	{
		if (battleOpen())
			return;

		heldNotices = new ArrayList<>();
		playTeamBattle(game.eliteContinue(), true);
	}

	/**
	 * Tells the player that a battle they are choosing the turns of comes first. An Elite battle or a healing item in
	 * the middle of it would change the team behind the battle's back.
	 *
	 * @return whether such a battle is on
	 */
	private boolean battleOpen()
	{
		if (!director.active())
			return false;

		announce(Notice.player("Finish the battle first", Tone.BAD));
		return true;
	}

	private void eliteForfeit()
	{
		heldNotices = new ArrayList<>();
		game.eliteForfeit();
		playBack(null, Notice.PLAYER);
	}

	/**
	 * @return whether the player is busy with something in the game, such as fighting a monster or talking to
	 *     someone, so a scene they did not ask for must not start over it
	 */
	private boolean interacting()
	{
		Player player = client.getLocalPlayer();
		return player == null || player.getInteracting() != null;
	}

	/**
	 * Shows a team battle: acted out beside the player when it can be, otherwise as text only. Acted out, each hit
	 * floats over the creature that was hit and each knock-out over the player as the scene reaches it; the lines
	 * before the first knock-out show at once, and the verdict and whatever the game announced while resolving the
	 * battle wait until the scene is over. {@link #heldNotices} must be open, and is closed here.
	 *
	 * @param result the battle, or null when none started
	 * @param allowScene whether a scene may start; false when the player is busy with something else
	 */
	private void playTeamBattle(TeamBattleResult result, boolean allowScene)
	{
		boolean staged = result != null && allowScene && config.battleAnimations() && !stage.active()
			&& arena.start(result.duels(), result.leadFirst());
		if (!staged)
		{
			playBack(result == null ? null : result.lines(), Notice.PLAYER);
			return;
		}

		long now = System.currentTimeMillis();
		for (BattleLine line : result.leadIn())
		{
			if (config.showResults())
				noticeBoard.show(Notice.player(line.text(), line.tone()), now);
		}

		playBack(result.leadOut(), Notice.PLAYER, true);
	}

	/**
	 * Shows a battle one line per game tick, followed by whatever the game announced while resolving it, so
	 * rewards never float up before the hits that earned them.
	 *
	 * @param lines the battle's lines, or null when no battle started
	 * @param wildId the wild creature's id, or {@link Notice#PLAYER} when there is none
	 */
	private void playBack(List<BattleLine> lines, int wildId)
	{
		playBack(lines, wildId, false);
	}

	/**
	 * Shows a battle one line per game tick, followed by whatever the game announced while resolving it.
	 *
	 * @param lines the battle's lines, or null when no battle started
	 * @param wildId the wild creature's id, or {@link Notice#PLAYER} when there is none
	 * @param staged whether the battle is acted out on screen, so its lines wait for the lead creature to arrive
	 */
	private void playBack(List<BattleLine> lines, int wildId, boolean staged)
	{
		List<Notice> after = heldNotices;
		heldNotices = null;
		if (lines == null || !config.showResults())
		{
			after.forEach(this::announce);
			return;
		}

		List<Notice> queue = new ArrayList<>();
		for (BattleLine line : lines)
		{
			if (line.kind() == BattleLine.Kind.OPENING)
				continue;

			if (staged && wildId != Notice.PLAYER && line.overPlayer())
				queue.add(Notice.stageLead(line.text(), line.tone()));
			else if (staged && wildId != Notice.PLAYER)
				queue.add(Notice.stageFoe(wildId, line.text(), line.tone()));
			else if (line.overPlayer() || wildId == Notice.PLAYER)
				queue.add(Notice.player(line.text(), line.tone()));
			else
				queue.add(Notice.creature(wildId, line.text(), line.tone()));
		}

		queue.addAll(after);
		noticeBoard.enqueue(queue, staged);

		if (!config.chatMessages())
			return;

		for (Notice notice : after)
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", notice.text(), null);
	}

	/**
	 * @param entry a wild creature
	 * @return whether it is the one the player is fighting in a battle they steer
	 */
	private boolean inBattleWith(WildEntry entry)
	{
		BattleSession battle = director.session();
		return battle != null && battle.wild() && battle.entry() == entry;
	}

	/** @return the share of full health the creature in the battle the player steers has left */
	private double foeHealthLeft()
	{
		BattleState state = director.session().state();
		Combatant foe = state.foe();
		return foe.hpFraction();
	}

	/**
	 * Starts a battle with a wild creature that waits for the player's choice each turn. With "Battle animations" on
	 * the lead walks up to it and the turns are acted out; otherwise the battle is played in text.
	 *
	 * @param wildId the wild creature's id
	 * @param entry the wild creature
	 */
	private void startSteeredBattle(int wildId, WildEntry entry)
	{
		heldNotices = new ArrayList<>();
		BattleSession battle = game.startBattle(entry);
		if (battle == null)
		{
			playBack(null, wildId, false);
			refreshPanel();
			return;
		}

		List<BattleLine> opening = Collections.singletonList(battle.opening());
		steerWild = wildId;
		steerHome = null;
		WildBody body = bodies.get(wildId);
		boolean staged = body != null && config.battleAnimations() && !arena.active() && !stage.active()
			&& stage.startOpen(body, opening);
		if (body != null)
			noteUnreachable(wildId, body);

		director.begin(battle, staged ? new WildScene(stage) : null);
		playBack(opening, wildId, staged);
		refreshPanel();
	}

	/**
	 * Starts a battle with a City Champion that waits for the player's choice each turn.
	 *
	 * @param championId the champion's id
	 */
	private void startSteeredChallenge(String championId)
	{
		heldNotices = new ArrayList<>();
		BattleSession battle = game.startChallenge(championId);
		if (battle == null)
		{
			playBack(null, Notice.PLAYER, false);
			refreshPanel();
			return;
		}

		BattleState state = battle.state();
		Combatant first = state.mine();
		List<Creature> fighters = battle.fighters();
		boolean leadFirst = fighters.get(0) == game.data().lead();
		boolean staged = config.battleAnimations() && !stage.active()
			&& arena.startOpen(state.foe().speciesKey(), first.speciesKey(), first.isGilded(), first.npcId(),
				leadFirst);
		Player player = client.getLocalPlayer();
		steerWild = Notice.PLAYER;
		steerHome = player == null ? null : player.getWorldLocation();
		director.begin(battle, staged ? new ArenaScene(arena) : null);
		playBack(Collections.singletonList(battle.opening()), Notice.PLAYER, false);
		refreshPanel();
	}

	/**
	 * Plays the player's choice in the battle that waits, if one does. Called on the game thread.
	 *
	 * @param choice what the player chose
	 */
	private void chooseBattle(BattleChoice choice)
	{
		if (!director.promptOpen())
			return;

		heldNotices = new ArrayList<>();
		director.choose(choice);
		flushHeld();
		refreshPanel();
	}

	/** Uses the first item that would help, as the Item hotkey does; says so when none would. */
	private void quickItem(BattleSession battle)
	{
		BattleChoice choice = game.quickItem(battle);
		if (choice == null)
			announce(Notice.player("No item would help right now", Tone.BAD));
		else
			chooseBattle(choice);
	}

	/** Shows whatever the game announced while a turn was played and nobody took, so no line is lost. */
	private void flushHeld()
	{
		List<Notice> left = heldNotices;
		heldNotices = null;
		if (left != null)
			left.forEach(this::announce);
	}

	/**
	 * Carries a hotkey to the game thread as the choice it stands for. The Switch key steps in the first creature
	 * that can fight.
	 *
	 * @param kind which key was pressed
	 */
	private void hotkeyPressed(BattleChoice.Kind kind)
	{
		clientThread.invoke(() ->
		{
			BattleSession battle = director.session();
			if (battle == null || !director.promptOpen())
				return;

			BattleState state = battle.state();
			if (kind == BattleChoice.Kind.SWITCH)
			{
				List<Integer> targets = state.switchTargets();
				if (!targets.isEmpty())
					chooseBattle(BattleChoice.switchTo(targets.get(0)));

				return;
			}

			if (state.needsSwitch())
				return;

			if (kind == BattleChoice.Kind.ATTACK)
				chooseBattle(BattleChoice.attack());
			else if (kind == BattleChoice.Kind.ITEM)
				quickItem(battle);
			else if (kind == BattleChoice.Kind.CATCH && battle.wild())
				chooseBattle(BattleChoice.catching(null));
			else if (kind == BattleChoice.Kind.RUN)
				chooseBattle(BattleChoice.run());
		});
	}

	/**
	 * Moves a battle the player steers on by one game tick: opens the choice when the scene is ready, counts the
	 * time, plays the creature's own attack when it is up, and ends the battle as running when the player left it.
	 * The panel is redrawn every tick while a choice is open so the countdown moves.
	 */
	private void steering()
	{
		if (!director.active())
		{
			if (choiceOpen)
			{
				choiceOpen = false;
				promptOverlay.show(Collections.emptyList());
				refreshPanel();
			}

			return;
		}

		heldNotices = new ArrayList<>();
		director.gameTick(steerForfeits());
		flushHeld();
		boolean open = director.promptOpen();
		choiceOpen = open;
		promptOverlay.show(open ? promptLines() : Collections.emptyList());
		if (open || !director.active())
			refreshPanel();
	}

	private List<String> promptLines()
	{
		BattleView view = director.view();
		if (view == null)
			return Collections.emptyList();

		String hotkeyText = BattlePrompt.hotkeys(keyText(config.attackKey()), keyText(config.switchKey()),
			keyText(config.catchKey()), keyText(config.itemKey()), keyText(config.runKey()), view.canCatch(),
			view.forcedSwitch());
		return BattlePrompt.lines(view, hotkeyText);
	}

	private static String keyText(Keybind key)
	{
		return key == null || Keybind.NOT_SET.equals(key) ? null : key.toString();
	}

	/** @return whether the player left the battle they steer: logged out, walked away, or the creature is gone */
	private boolean steerForfeits()
	{
		Player player = client.getLocalPlayer();
		WorldPoint here = player == null ? null : player.getWorldLocation();
		boolean loggedIn = client.getGameState() == GameState.LOGGED_IN && here != null;
		boolean inView = true;
		WorldPoint there = steerHome;
		if (steerWild != Notice.PLAYER)
		{
			WildBody body = bodies.get(steerWild);
			inView = body != null && !body.life().leaving() && tracker.get(steerWild) != null;
			there = body == null ? null
				: new WorldPoint((int) Math.round(body.x()), (int) Math.round(body.y()), body.plane());
		}

		if (here == null || there == null)
			return !loggedIn || !inView;

		return TurnClock.forfeits(loggedIn, inView, here.getX() - there.getX(), here.getY() - there.getY());
	}

	/** @return what hears a steered battle and shows it */
	private BattleDirector.Listener battleListener()
	{
		return new BattleDirector.Listener()
		{
			@Override
			public void turn(BattleSession session, TurnResult turn, List<BattleLine> closing, boolean hasScene)
			{
				showTurn(session, turn, closing, hasScene);
			}

			@Override
			public void jarThrown(BattleSession session, JarType jar, CatchOutcome outcome)
			{
				showBattleThrow(session, jar, outcome);
			}
		};
	}

	/**
	 * Acts out a jar thrown in the middle of a battle. A catch, or a creature that runs off, ends the battle, so it
	 * goes to the usual catch effects, which also end the scene. A jar that does not hold plays the throw and the
	 * battle goes on.
	 *
	 * @param session the battle
	 * @param jar the jar thrown
	 * @param outcome what it did
	 */
	private void showBattleThrow(BattleSession session, JarType jar, CatchOutcome outcome)
	{
		int wildId = session.entry().wildId();
		if (outcome == CatchOutcome.CAUGHT || outcome == CatchOutcome.FLED)
		{
			tracker.remove(wildId);
			showCatch(outcome, wildId, jar);
			return;
		}

		if (outcome != CatchOutcome.BROKE_FREE || !config.catchEffects() || !effects.throwJar())
			return;

		effects.react(outcome);
		jarFlight.start(jar, () -> bodies.spotOf(wildId));
	}

	/**
	 * Shows the lines of one turn of a battle the player steers, followed by whatever the game announced while
	 * playing it. When the scene acts the battle out, blows and the result float as they land, and anything about
	 * who steps in floats at once; against a champion the scene shows the blows itself and the verdict waits for it
	 * to end.
	 *
	 * @param session the battle
	 * @param turn the turn
	 * @param closing the lines that close the battle, when this turn ended it
	 * @param hasScene whether a scene acts the battle out
	 */
	private void showTurn(BattleSession session, TurnResult turn, List<BattleLine> closing, boolean hasScene)
	{
		List<Notice> after = heldNotices == null ? new ArrayList<>() : heldNotices;
		heldNotices = null;
		if (!config.showResults() || !turn.turnUsed())
		{
			after.forEach(this::announce);
			return;
		}

		int wildId = steerWild;
		boolean wild = session.wild();
		List<Notice> now = new ArrayList<>();
		List<Notice> timed = new ArrayList<>();
		for (BattleLine line : turn.lines())
		{
			boolean blow = line.blow();
			Duel duel = turn.duel();
			boolean knockOut = duel != null && duel.knockOut() == line;
			if (hasScene && !wild && (blow || knockOut))
				continue;

			Notice notice = noticeOf(line, wildId, hasScene);
			if (hasScene && wild && blow)
				timed.add(notice);
			else if (hasScene)
				now.add(notice);
			else
				timed.add(notice);
		}

		long stamp = System.currentTimeMillis();
		now.forEach(notice -> noticeBoard.show(notice, stamp));
		List<Notice> result = new ArrayList<>();
		for (BattleLine line : closing)
			result.add(noticeOf(line, wildId, hasScene));

		result.addAll(after);
		if (!hasScene)
		{
			timed.addAll(result);
			noticeBoard.enqueue(timed, false);
		}
		else if (wild)
		{
			timed.addAll(result);
			pendingLines.addAll(timed);
			pendingTicks = Math.max(pendingTicks, 2);
		}
		else
		{
			noticeBoard.enqueue(result, true);
		}

		if (config.chatMessages())
		{
			for (Notice notice : after)
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", notice.text(), null);
		}
	}

	private static Notice noticeOf(BattleLine line, int wildId, boolean staged)
	{
		if (wildId == Notice.PLAYER)
			return staged ? TeamScript.noticeOf(line) : Notice.player(line.text(), line.tone());

		if (staged && line.overPlayer())
			return Notice.stageLead(line.text(), line.tone());

		if (staged)
			return Notice.stageFoe(wildId, line.text(), line.tone());

		return line.overPlayer() ? Notice.player(line.text(), line.tone())
			: Notice.creature(wildId, line.text(), line.tone());
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
			public void rename(TradePick pick, String name)
			{
				onGameThread(() -> GielinorGoPlugin.this.rename(pick, name));
			}

			@Override
			public void useItem(TradePick pick, ItemType type)
			{
				onGameThread(() -> GielinorGoPlugin.this.useItem(pick, type));
			}

			@Override
			public void resolveMove(TradePick pick, int forgetIndex)
			{
				onGameThread(() -> GielinorGoPlugin.this.resolveMove(pick, forgetIndex));
			}

			@Override
			public void selectJar(JarType type)
			{
				onGameThread(() -> game.selectJar(type));
			}

			@Override
			public void battleChoice(BattleChoice choice)
			{
				onGameThread(() -> chooseBattle(choice));
			}

			@Override
			public void challenge(String championId)
			{
				onGameThread(() -> GielinorGoPlugin.this.challenge(championId));
			}

			@Override
			public void eliteChallenge()
			{
				onGameThread(GielinorGoPlugin.this::eliteChallenge);
			}

			@Override
			public void eliteContinue()
			{
				onGameThread(GielinorGoPlugin.this::eliteContinue);
			}

			@Override
			public void eliteForfeit()
			{
				onGameThread(GielinorGoPlugin.this::eliteForfeit);
			}

			@Override
			public void challengeFriend(long memberId)
			{
				onGameThread(() -> GielinorGoPlugin.this.challengeFriend(memberId));
			}

			@Override
			public void offerTrade(long memberId, TradePick give, int wantIndex, String wantLabel)
			{
				onGameThread(() -> GielinorGoPlugin.this.offerTrade(memberId, give, wantIndex, wantLabel));
			}

			@Override
			public void answerTrade(long memberId, boolean accept)
			{
				onGameThread(() -> GielinorGoPlugin.this.answerTrade(memberId, accept));
			}
		};
	}

	private void onGameThread(Runnable action)
	{
		clientThread.invoke(() ->
		{
			if (!saveLive())
				return;

			action.run();
			refreshPanel();
		});
	}
}
