/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;

import com.google.gson.Gson;
import com.google.inject.Provides;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.RuneLiteConfig;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.events.UserJoin;
import net.runelite.client.party.events.UserPart;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
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

	/** Where the pointer counts as being when it is outside the canvas: far from every bubble and creature. */
	private static final int OFF_CANVAS = -100_000;

	/** Game ticks between extra chances for a monster already in view to turn wild. */
	private static final int REROLL_TICKS = 25;

	/** Farthest from the player, in tiles, a monster may be and still turn wild: its creature would leave beyond. */
	private static final int SPAWN_RANGE = WildLife.LEAVE_TILES - 5;

	/** Rarity used to read a species' event boost back out of the calendar's chance: the chance times this. */
	private static final int BOOST_ONE_IN = 100;

	/** Game ticks between window refreshes while nothing changes, so resting health still creeps up. */
	private static final int PANEL_TICKS = 5;

	/** Game ticks between saves of changed progress. */
	private static final int SAVE_TICKS = 50;

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
	private SpriteManager spriteManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private GielinorGoConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ModelOutlineRenderer outlineRenderer;

	@Inject
	private TooltipManager tooltipManager;

	@Inject
	private Gson gson;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private RenderCallbackManager renderCallbackManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private RuneLiteConfig runeLiteConfig;

	@Inject
	private PartyService partyService;

	@Inject
	private WSClient wsClient;

	private final Random random = new Random();
	private final CombatWatch combatWatch = new CombatWatch();
	private final Map<Integer, NPC> candidates = new HashMap<>();
	private final Map<Skill, Integer> lastXp = new EnumMap<>(Skill.class);
	private final XpLedger ledger = new XpLedger();
	private final Map<Skill, Integer> lastLevel = new EnumMap<>(Skill.class);
	private final StepCounter stepCounter = new StepCounter();
	private final BankSpots bankSpots = new BankSpots();
	private BankWatch bankWatch;
	private WildTracker tracker = new WildTracker(random);
	private final SpawnClock spawnClock = SpawnClock.system();
	private SpawnCalendar calendar;
	private SkillingTable skilling = SkillingTable.empty();
	private long lastSkillingMs = -SkillingRules.COOLDOWN_MS;
	private GoGame game;
	private GoPersistence persistence;
	private WildOverlay wildOverlay;
	private WildHover wildHover;
	private WildBubbles bubbles;
	private WildBubbleOverlay bubbleOverlay;
	private BubbleInput bubbleInput;
	private FollowerBubbles followerModel;
	private BiomePlayer biomePlayer;
	private IdleService idleStrips;
	private FollowerBubbleView followerView;

	/** Whether the follower bubbles failed this session, which leaves them off. */
	private boolean followerBubblesFailed;
	private volatile boolean bubblesFailed;
	private volatile BufferedImage combatSprite;
	private WildBodies bodies;
	private final NoticeBoard noticeBoard = new NoticeBoard();

	private final CueGate cueGate = new CueGate();
	private List<Notice> heldNotices;

	private FloatingTextOverlay floatingText;
	private FollowerTagOverlay followerTag;
	/** The lead creature's nickname for the overlay, kept up to date on the game thread. Null for none. */
	private volatile String leadTag;
	private PortraitService portraits;
	private FollowerService follower;
	private CatchEffects effects;
	private JarFlightOverlay jarFlight;
	private BattleStage stage;
	private ArenaStage arena;
	private BattleDirector director;
	private BattleControls battleControls;
	private WindowInput controlsInput;
	private TrackerOverlay trackerOverlay;
	private PartyBoxOverlay partyBoxOverlay;
	private PartyBoxInput partyBoxInput;
	private TrackerModel trackerModel;
	private TrackerView lastTracker = TrackerView.NONE;
	private BattleHotkeys hotkeys;
	private volatile boolean choiceOpen;

	/** Whether a battle that plays itself is on, which the Run key can stop. */
	private volatile boolean autoBattleOpen;
	private int steerWild = Notice.PLAYER;
	private WorldPoint steerHome;
	private List<Notice> pendingLines = new ArrayList<>();
	private int pendingTicks;
	private FriendBattles friends;
	private FriendTrades trades;
	private final PartyRateGate partyRate = new PartyRateGate();
	private boolean helloDue;
	private boolean helloAsksBack;
	private int lastHelloTick;
	private GoWindow window;
	private WindowToggle windowToggle;
	private PortraitCache windowPortraits;
	private WindowNotice windowNotice;
	private WindowInput windowInput;
	private WindowKeys windowKeys;
	private DragKey dragKey;
	private volatile boolean chatTyping;
	private long windowVersion;
	private boolean firstRunNoticed;
	private boolean loaded;
	private boolean unsaved;
	private String saveProfileKey;
	private int ticks;
	private final ActionSteps actionSteps = new ActionSteps();

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
		game.bosses(loadBosses(species));
		game.setCues(this::playCue);
		game.autoPinEggs(config::autoPinEggs);
		game.teamTowns(loadTeamTowns());
		bankWatch = new BankWatch(client, bankSpots);
		game.elite(loadElite(species));
		game.rival(loadRival(species));
		game.rematches(loadRematches(species));
		game.moves(loadMoves());
		game.examine(loadExamine());
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
		skilling = loadSkilling(species, animations);
		lastSkillingMs = -SkillingRules.COOLDOWN_MS;
		bodies = new WildBodies(client, animations, random);
		wildHover = new WildHover();
		wildOverlay = new WildOverlay(client, bodies, config, outlineRenderer, wildHover, tooltipManager);
		floatingText = new FloatingTextOverlay(client, noticeBoard, this::spotFor, this::tagFor, this::healthFor);
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
		trades.battleOn(director::active);
		hotkeys = new BattleHotkeys(config::attackKey, config::switchKey, config::catchKey, config::runKey,
			config::itemKey, () -> choiceOpen, this::hotkeyPressed)
				.runAlso(() -> autoBattleOpen);
		keyManager.registerKeyListener(hotkeys);
		detectVersionChange();
		startWindow();
		startControls();
		startTracker();
		startPartyBox();
		startBubbles();
		startFollowerBubbles();
		clientThread.invoke(this::startOnClientThread);
	}

	@Override
	protected void shutDown()
	{
		stopPartyBox();
		stopTracker();
		stopBubbles();
		stopFollowerBubbles();
		stopControls();
		WindowStyle.share(null);
		stopWindow();
		biomePlayer.clear();
		idleStrips.clear();
		keyManager.unregisterKeyListener(hotkeys);
		choiceOpen = false;
		autoBattleOpen = false;
		clientThread.invoke(director::end);
		GoGame endingGame = loaded ? game : null;
		GoPersistence endingPersistence = persistence;
		String endingProfile = saveProfileKey;
		boolean endingUnsaved = unsaved;
		Map<String, Integer> endingXp = ledger.stamp();
		loaded = false;
		clientThread.invoke(() ->
		{
			if (endingGame != null)
				trades.settleHeld();

			if (endingGame != null && endingXp != null)
				endingGame.stampXp(endingXp);

			SaveFlush.write(endingGame, endingPersistence, endingProfile, endingUnsaved);
		});
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
		portraits.clear();
		tracker.clear();
		candidates.clear();
		lastXp.clear();
		lastLevel.clear();
		ledger.reset();
		stepCounter.reset();
		actionSteps.reset();
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
	 * Finds the health bar a body of the battle on stage shows. Called from the render thread.
	 *
	 * @param anchor which body
	 * @return the share of full health, or null for no bar
	 */
	private Double healthFor(Notice.Anchor anchor)
	{
		BattleStage wild = stage;
		Double share = wild == null ? null : wild.healthOf(anchor);
		ArenaStage team = arena;
		return share != null || team == null ? share : team.healthOf(anchor);
	}

	/**
	 * Loads the right save whenever the logged-in account changes.
	 *
	 * @param event the profile change
	 */
	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		clientThread.invoke(() ->
		{
			director.end();
			trades.settleHeld();
			saveNow();
			loadSave();
		});
	}

	/**
	 * Saves when the client is closed, because RuneLite does not stop plugins then. The event does not arrive on the
	 * client thread, which may still be changing the game, so the save is handed to that thread and the shutdown waits
	 * for it. The config is flushed to disk afterwards: the config manager's own handler may have run first, and
	 * without the flush the save would stay in memory.
	 *
	 * @param event the shutdown
	 */
	@Subscribe
	public void onClientShutdown(ClientShutdown event)
	{
		event.waitFor(SaveFlush.handOver(clientThread::invoke, this::saveForShutdown));
	}

	private void saveForShutdown()
	{
		try
		{
			director.settle();
			trades.settleHeld();
			saveNow();
			configManager.sendConfig();
		}
		catch (RuntimeException e)
		{
			log.warn("Gielinor Go: could not save on shutdown", e);
		}
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
		closeWindow();
		choiceOpen = false;
		battleControls.model().show(null, BattleControlsModel.Keys.NONE);
		clearTracker();
		pendingLines.clear();
		if (loaded)
			trades.settleHeld();

		saveNow();
		trades.clear();
		stage.clear();
		arena.clear();
		follower.hide();
		effects.clear();
		jarFlight.clear();
		bodies.clear();
		hideBubbles();
		tracker.clear();
		bankSpots.clear();
		game.setBankNear(false);
		game.setBankOpen(false);
		candidates.clear();
		stepCounter.reset();
		actionSteps.reset();
		noticeBoard.clear();
		if (state == GameState.LOGIN_SCREEN)
		{
			lastXp.clear();
			lastLevel.clear();
			ledger.reset();
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
		bankWatch.noteNpc(npc);
		if (speciesOf(npc) == null)
			return;

		candidates.put(npc.getIndex(), npc);
		offer(npc);
	}

	/**
	 * Remembers a bank booth or chest that came into view, so the team can be changed near it. What an object id
	 * offers is looked up once and kept.
	 *
	 * @param event the spawn
	 */
	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		bankWatch.noteObject(event.getGameObject());
	}

	/**
	 * Forgets a bank booth or chest that left view.
	 *
	 * @param event the despawn
	 */
	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		bankSpots.remove(event.getGameObject());
	}

	/**
	 * Forgets a monster that left view.
	 *
	 * @param event the despawn
	 */
	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		bankSpots.remove(event.getNpc());
		int index = event.getNpc().getIndex();
		candidates.remove(index);
		tracker.leftView(index, System.currentTimeMillis());
	}

	/**
	 * Reads the game's kill-count lines. A boss or raid the table knows can give its egg. Only game messages are
	 * read, and nothing about the loot is looked at.
	 *
	 * @param event the chat message
	 */
	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!loaded || game == null || event.getType() != ChatMessageType.GAMEMESSAGE)
			return;

		try
		{
			String name = BossRules.killedName(Text.removeTags(event.getMessage()));
			if (name != null)
				game.bossKilled(name);
		}
		catch (RuntimeException e)
		{
			log.warn("Gielinor Go: could not read a kill-count line", e);
		}
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
		ledger.read(skill.name(), event.getXp());
		Integer previousXp = lastXp.put(skill, event.getXp());
		Integer previousLevel = lastLevel.put(skill, event.getLevel());
		if (previousXp == null || previousLevel == null || !loaded)
			return;

		game.addSkillXp(skill.name(), event.getXp() - previousXp);
		actionStep(skill.name(), event.getXp() - previousXp);
		skillingEncounter(skill.name(), event.getXp() - previousXp, event.getLevel());
		if (event.getLevel() > previousLevel)
			game.onLevelUp();
	}

	/**
	 * Counts an XP drop in a skill that is not a combat skill as one step toward the eggs carried, within the limits
	 * of {@link ActionSteps}. Nothing happens with the setting off, or with no egg carried, so the hour is not spent.
	 *
	 * @param skill the skill's name in capitals
	 * @param xpGained OSRS XP in the drop
	 */
	private void actionStep(String skill, int xpGained)
	{
		if (!config.skillingHatchesEggs() || !ActionSteps.counts(skill, xpGained) || !game.carriesEgg())
			return;

		game.addActionSteps(actionSteps.take(ticks, System.currentTimeMillis()));
	}

	/**
	 * Gives an XP drop in a gathering skill its chance to bring a wild creature beside the player. Nothing happens
	 * with the setting off, where creatures cannot be wild, inside the cooldown or with three creatures out already.
	 *
	 * @param skill the skill's name in capitals
	 * @param xpGained OSRS XP in the drop
	 * @param skillLevel the player's level in the skill
	 */
	private void skillingEncounter(String skill, int xpGained, int skillLevel)
	{
		try
		{
			long now = System.currentTimeMillis();
			if (!WildHiding.mayAppear(combatWatch.inCombat(now)) || xpGained <= 0
				|| !SkillingRules.gathering(skill) || !config.skillingEncounters() || !playable()
				|| !SkillingRules.ready(lastSkillingMs, now) || tracker.all().size() >= WildTracker.MAX_WILD)
				return;

			Species species = skilling.pick(skill, skillLevel, random);
			if (species == null)
				return;

			boolean events = config.spawnEvents();
			double boost = calendar.chance(species, BOOST_ONE_IN, events, spawnClock) * BOOST_ONE_IN;
			if (random.nextDouble() >= SkillingRules.chance(xpGained, config.wildChance()) * boost)
				return;

			Player player = client.getLocalPlayer();
			WorldPoint point = player == null ? null : player.getWorldLocation();
			boolean underground = point != null && Terrain.underground(point.getX(), point.getY());
			WildEntry entry = tracker.offerLoose(species, underground,
				game.gildedOneIn(calendar.gildedOneIn(species, events, spawnClock)), events);
			if (entry == null)
				return;

			if (bodies.spawnBesidePlayer(entry, now) == null)
			{
				tracker.withdrawLoose(entry);
				return;
			}

			lastSkillingMs = now;
			entry.withFirstMeeting(game.markSeen(entry)).record(game.recordOf(entry));
			String kind = entry.gilded() ? "A gilded " : entry.roaming() ? "A roaming " : "A wild ";
			announce(Notice.creature(entry.wildId(), kind + species.name() + " " + skilling.message(skill) + "!",
				entry.gilded() || entry.roaming() ? Tone.FIND : Tone.INFO));
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: a skilling encounter failed", e);
		}
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
		reconcileXp();
		watchCombat();
		steering();
		battleScene();
		noticeBoard.tick(System.currentTimeMillis());

		portraits.tick();
		idleStrips.tick();
		biomePlayer.sweep(System.currentTimeMillis());
		countSteps();
		updateFollower();
		greetParty();
		wildTick();
		if (!playable())
		{
			bodies.clear();
			hideBubbles();
			tracker.clearWild(System.currentTimeMillis());
		}
		else if (ticks % REROLL_TICKS == 0)
		{
			offerRandomCandidate();
		}

		trackerTick();
		if (!battleBusy())
		{
			game.reviveHere();
			game.rescue();
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
		watchWindow();
		if (!loaded)
		{
			hideBubbles();

			return;
		}

		long now = System.currentTimeMillis();
		bodies.clientTick(now, combatWatch.inCombat(now), this::inBattleWithWild);
		wildPointer();
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
		refreshRecords();
		for (int wildId : bodies.gameTick(now))
		{
			tracker.remove(wildId);
			stage.forget(wildId);
		}
	}

	/** Brings every wild creature's seen-or-caught record up to date, so a catch changes the marks within a tick. */
	private void refreshRecords()
	{
		try
		{
			for (WildEntry entry : tracker.all())
				entry.record(game.recordOf(entry));
		}
		catch (RuntimeException e)
		{
			log.debug("Gielinor Go: could not refresh the wild creatures' records", e);
		}
	}

	/**
	 * Moves the wild creature bubbles on with the pointer, and adds the Catch and Battle entries to the menu while
	 * the mouse is over a wild creature's body, with the one the player most likely wants first. The entries are
	 * client-side only and nothing is sent to the game. They are added only while the mouse is over the game view
	 * (the menu then has a "Walk here"), never over an interface, and never while the menu is open. The entries
	 * always sit below the game's own, so a plain left click on the body is not ours. Over a painted bubble the
	 * game's entries are cleared instead, as under the painted window, so the game offers nothing there.
	 */
	private void wildPointer()
	{
		try
		{
			net.runelite.api.Point mouse = client.getMouseCanvasPosition();
			boolean menuOpen = client.isMenuOpen();
			boolean overScene = false;
			boolean hasOurs = false;
			if (mouse != null && !menuOpen)
			{
				for (MenuEntry entry : client.getMenu().getMenuEntries())
				{
					hasOurs |= entry.getType() == MenuAction.RUNELITE && WildMenu.isOurs(entry.getOption());
					overScene |= entry.getType() == MenuAction.WALK;
				}
			}

			bubbleTick(mouse, overScene, menuOpen);
			followerBubbleTick(mouse, menuOpen);
			if (mouse != null && !menuOpen && followerView.covers(mouse.getX(), mouse.getY()) && !dragKey.held())
			{
				client.getMenu().setMenuEntries(new MenuEntry[0]);
				wildHover.clear();
				return;
			}

			if (bodies.isEmpty() || menuOpen || mouse == null)
			{
				wildHover.clear();
				return;
			}

			if (bubbles.covers(mouse.getX(), mouse.getY()) && !dragKey.held())
			{
				client.getMenu().setMenuEntries(new MenuEntry[0]);
				wildHover.clear();
				return;
			}

			WildBody hit = overScene || hasOurs ? bodies.under(mouse) : null;
			WildEntry tracked = hit == null ? null : tracker.get(hit.entry().wildId());
			if (tracked != null && !hasOurs)
				addWildOptions(tracked);

			if (tracked != null && !bubbles.showing(tracked.wildId())
				&& WildHover.wanted(config.wildTooltip(), false, true, director.active()))
				wildHover.update(tracked.wildId(), WildTip.text(tracked), System.currentTimeMillis());
			else
				wildHover.clear();
		}
		catch (RuntimeException | AssertionError e)
		{
			wildHover.clear();
			log.debug("Gielinor Go: could not add the wild creature's menu entries", e);
		}
	}

	/** @return whether the bubbles are wanted: the setting is on and they have not failed this session */
	private boolean bubblesOn()
	{
		return config.wildBubbles() && !bubblesFailed;
	}

	/**
	 * Moves the bubbles' model on by one client tick: works out which creature the pointer is near, what it
	 * offers, and what the bubbles must keep clear of. Paused while the game's menu is open, dismissed during a
	 * battle or where wild creatures may not appear, dropped at once when the setting is off or they failed. A
	 * failure turns the bubbles off for the session, which leaves the game's menu entries.
	 *
	 * @param mouse the pointer on the canvas, or null when it is outside it
	 * @param overScene whether the game's menu holds a "Walk here", which means the pointer is over the game view
	 * @param menuOpen whether the game's menu is open
	 */
	private void bubbleTick(net.runelite.api.Point mouse, boolean overScene, boolean menuOpen)
	{
		try
		{
			if (!bubblesOn())
			{
				hideBubbles();
				return;
			}

			loadCombatSprite();
			bubbles.pause(menuOpen);
			if (menuOpen)
				return;

			long now = System.currentTimeMillis();
			if (battleBusy() || !playable())
			{
				bubbles.dismiss(now);
				return;
			}

			int px = mouse == null ? OFF_CANVAS : mouse.getX();
			int py = mouse == null ? OFF_CANVAS : mouse.getY();
			Rectangle viewport = new Rectangle(client.getViewportXOffset(), client.getViewportYOffset(),
				client.getViewportWidth(), client.getViewportHeight());
			List<Rectangle> blockers = new ArrayList<>();
			if (window.shell().isOpen())
				blockers.add(window.screenBounds());

			int currentId = bubbles.wildId();
			if (currentId != WildBubbles.NO_TARGET && bodies.anchorsOf(currentId) == null)
			{
				hideBubbles();
				currentId = WildBubbles.NO_TARGET;
			}

			boolean currentEligible = false;
			boolean currentNear = false;
			List<WildBubbles.Candidate> candidates = new ArrayList<>();
			for (WildBody body : bodies.all())
			{
				int id = body.entry().wildId();
				BubbleAnchors anchors = bubbleEligible(body) ? bodies.anchorsOf(id) : null;
				if (anchors == null)
					continue;

				boolean near = overScene && mouse != null && nearBody(body, px, py);
				double dx = px - anchors.originX();
				double dy = py - anchors.originY();
				candidates.add(new WildBubbles.Candidate(id, near, dx * dx + dy * dy));
				if (id == currentId)
				{
					currentEligible = true;
					currentNear = near;
				}
			}

			boolean inside = currentNear || bubbles.holds(px, py);
			WildBubbles.Pick pick = WildBubbles.pick(currentId, currentEligible, inside, candidates);
			WildBody picked = pick.wildId() == WildBubbles.NO_TARGET ? null : bodies.get(pick.wildId());
			WildEntry entry = picked == null ? null : tracker.get(pick.wildId());
			if (entry == null)
			{
				bubbles.tick(null, null, viewport, false, blockers, px, py, now, config.windowAnimations());
				return;
			}

			boolean near = false;
			for (WildBubbles.Candidate candidate : candidates)
			{
				if (candidate.wildId() == pick.wildId())
					near = candidate.near();
			}

			bubbles.tick(bubbleOffer(entry, picked, now), bodies.anchorsOf(pick.wildId()), viewport, near,
				blockers, px, py, now, config.windowAnimations());
		}
		catch (RuntimeException | AssertionError e)
		{
			bubblesBroke(e);
		}
	}

	/**
	 * @param body a wild creature's body
	 * @return whether it may have bubbles: tracked, drawn, not hidden for the player's combat, not leaving and not in
	 *     a catch or battle
	 */
	private boolean bubbleEligible(WildBody body)
	{
		WildLife.State state = body.life().state();
		return tracker.get(body.entry().wildId()) != null && body.drawn() && !body.hidden()
			&& !body.life().leaving() && state != WildLife.State.IN_CATCH && state != WildLife.State.IN_BATTLE;
	}

	private boolean nearBody(WildBody body, int px, int py)
	{
		Shape reach = bodies.reachOf(body.entry().wildId(), WildBubbles.NEAR_TILES);
		return reach != null && reach.contains(px, py) || body.contains(px, py);
	}

	private BubbleOffer bubbleOffer(WildEntry entry, WildBody body, long now)
	{
		int wildId = entry.wildId();
		List<JarSuggest.JarChoice> owned = jarChoices(entry);
		JarType jar = game.jarToThrow();
		JarSuggest.JarChoice shown = null;
		for (JarSuggest.JarChoice choice : owned)
		{
			if (choice.jar() == jar)
				shown = choice;
		}

		boolean knockedOut = knockedOut(wildId);
		boolean ready = !entry.battled() && game.canBattle(now, config.chooseBattleActions())
			&& !body.life().leaving();
		return new BubbleOffer(wildId, jar, owned, ready, knockedOut, WildCardContent.of(entry, shown, knockedOut));
	}

	/**
	 * Fetches the game's combat tab picture for the Battle bubble, if it is not stored yet. It runs on the client
	 * thread each time the bubbles tick until the game has the picture ready, which after a slow start can take a
	 * while.
	 */
	private void loadCombatSprite()
	{
		if (combatSprite != null)
			return;

		BufferedImage sprite = spriteManager.getSprite(SpriteID.SideIcons._0, 0);
		if (sprite == null)
			return;

		combatSprite = sprite;
		log.debug("Gielinor Go: combat tab picture is {}x{}", sprite.getWidth(), sprite.getHeight());
	}

	/** Creates the wild creature bubbles, their overlay and their mouse input, and registers them. */
	private void startBubbles()
	{
		bubblesFailed = false;
		bubbles = new WildBubbles();
		bubbleOverlay = new WildBubbleOverlay(client, bodies, bubbles, new BubblePainter(() -> combatSprite,
			WashSource.NONE),
			this::windowStyle, config::windowAnimations, config::windowFraction, () -> loaded && bubblesOn(),
			System::currentTimeMillis, this::bubblesBroke);
		bubbleInput = new BubbleInput(bubbleOverlay::hits, dragKey, bubbleSink());
		overlayManager.add(bubbleOverlay);
		mouseManager.registerMouseListener(bubbleInput);
	}

	/** Registers the bubbles over the follower. */
	private void startFollowerBubbles()
	{
		followerBubblesFailed = false;
		followerModel = new FollowerBubbles();
		followerView = new FollowerBubbleView(client, followerModel, new FollowerBubblePainter(windowPortraits),
			bubbleOverlay::style, () -> dragKey.held(),
			press -> clientThread.invoke(() -> followerPressed(press)), () -> followerBubblesFailed = true);
		overlayManager.add(followerView);
		mouseManager.registerMouseListener(followerView);
	}

	/** Unregisters the bubbles over the follower. */
	private void stopFollowerBubbles()
	{
		mouseManager.unregisterMouseListener(followerView);
		overlayManager.remove(followerView);
		followerView.clear();
		clientThread.invoke(followerModel::hide);
	}

	/**
	 * Moves the follower bubbles on by one client tick. A wild creature's bubbles win over these out of battle, and
	 * they stay away while the pointer is over the painted window. A failure turns them off for the session; the
	 * window, the battle box and the hotkeys do everything they do.
	 *
	 * @param mouse the pointer on the canvas, or null when it is outside it
	 * @param menuOpen whether the game's menu is open, which leaves the bubbles as they are
	 */
	private void followerBubbleTick(net.runelite.api.Point mouse, boolean menuOpen)
	{
		if (menuOpen)
			return;

		try
		{
			StageAnchors.Spot spot = followerBubblesWanted(mouse) ? followerSpot() : null;
			net.runelite.api.Point head = spot == null ? null
				: Perspective.localToCanvas(client, spot.point(), spot.plane(),
					spot.height() + WildOverlay.TAG_HEIGHT_OFFSET);
			net.runelite.api.Point foot = spot == null ? null
				: Perspective.localToCanvas(client, spot.point(), spot.plane(), 0);
			if (mouse == null || head == null || foot == null)
			{
				followerModel.hide();
				return;
			}

			double half = Math.max(14, (foot.getY() - head.getY()) / 2.0);
			boolean onBody = Math.abs(mouse.getX() - head.getX()) <= half && mouse.getY() >= head.getY()
				&& mouse.getY() <= foot.getY() + 8;
			long now = System.currentTimeMillis();
			BattleSession battle = director.session();
			FollowerOffer offer = battle == null
				? FollowerOffers.outOfBattle(game, now)
				: FollowerOffers.inBattle(battle, director.view(), director.automatic());
			followerModel.tick(now, offer, new double[] {head.getX(), head.getY()}, onBody,
				new double[] {mouse.getX(), mouse.getY()}, new Rectangle(client.getViewportXOffset(),
					client.getViewportYOffset(), client.getViewportWidth(), client.getViewportHeight()));
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: the follower bubbles failed", e);
			followerBubblesFailed = true;
			followerModel.hide();
			followerView.clear();
		}
	}

	private boolean followerBubblesWanted(net.runelite.api.Point mouse)
	{
		if (!config.followerBubbles() || followerBubblesFailed || !loaded || !saveLive() || mouse == null)
			return false;

		if (window.shell().isOpen() && window.screenBounds().contains(mouse.getX(), mouse.getY()))
			return false;

		if (director.active())
			return true;

		return playable() && bubbles.wildId() == WildBubbles.NO_TARGET;
	}

	/** @return where the lead creature is drawn: on the stage in a battle that is acted out, else following */
	private StageAnchors.Spot followerSpot()
	{
		StageAnchors.Spot staged = stage.active() ? stage.spotOf(Notice.Anchor.STAGE_LEAD)
			: arena.active() ? arena.spotOf(Notice.Anchor.STAGE_LEAD) : null;
		if (staged != null)
			return staged;

		LocalPoint ground = follower.groundPoint();
		WorldView view = client.getTopLevelWorldView();
		return ground == null || view == null ? null
			: new StageAnchors.Spot(ground, view.getPlane(), follower.tagHeight());
	}

	/**
	 * Acts on a press on a follower bubble, through the same paths as the window, the battle box and the hotkeys.
	 *
	 * @param press the press as x, y and button
	 */
	private void followerPressed(int[] press)
	{
		if (!loaded || !saveLive() || !config.followerBubbles())
			return;

		FollowerBubbles.Action action = followerModel.press(press[0], press[1], press[2]);
		boolean battling = director.active();
		switch (action.type())
		{
			case SWITCH:
				if (battling)
					chooseBattle(BattleChoice.switchTo(action.index()));
				else
					game.setLead(action.index());

				break;

			case ITEM:
				if (battling)
					chooseBattle(BattleChoice.item(action.item(), action.index()));
				else if (action.index() < game.data().team.size())
					game.useItem(game.data().team.get(action.index()), action.item());

				break;

			case RUN:
				if (director.automatic())
					askToRun();
				else
					chooseBattle(BattleChoice.run());

				break;

			default:
				return;
		}

		followerModel.hide();
		refreshPanel();
	}

	/** Unregisters the wild creature bubbles. */
	private void stopBubbles()
	{
		mouseManager.unregisterMouseListener(bubbleInput);
		overlayManager.remove(bubbleOverlay);
		bubbleOverlay.publish(BubbleHits.EMPTY);
		clientThread.invoke(this::hideBubbles);
	}

	/**
	 * Called when the bubbles threw while painting or while acting on a click: they are off for the rest of the
	 * session and the game's own menu entries are used. Runs on the game thread.
	 *
	 * @param cause what was thrown
	 */
	private void bubblesBroke(Throwable cause)
	{
		log.debug("Gielinor Go: the wild creature bubbles failed, using the game's menu entries", cause);
		bubblesFailed = true;
		hideBubbles();
	}

	/** Drops the bubbles and their click targets at once. Runs on the game thread. */
	private void hideBubbles()
	{
		if (bubbles != null)
			bubbles.hideNow();

		if (bubbleOverlay != null)
			bubbleOverlay.publish(BubbleHits.EMPTY);
	}

	/** @return where the mouse events of the bubbles go, each carried over to the game thread */
	private BubbleInput.Sink bubbleSink()
	{
		return new BubbleInput.Sink()
		{
			@Override
			public void pressed(int generation, BubbleKind kind, JarType jar, int button)
			{
				clientThread.invoke(() -> bubblePressed(generation, kind, jar, button));
			}

			@Override
			public void pressedOutside(int generation)
			{
				clientThread.invoke(() -> bubblePressedOutside(generation));
			}
		};
	}

	private void bubblePressedOutside(int generation)
	{
		try
		{
			if (bubblesOn() && loaded)
				bubbles.pressedOutside(generation, System.currentTimeMillis());
		}
		catch (RuntimeException | AssertionError e)
		{
			bubblesBroke(e);
		}
	}

	/**
	 * Acts on a press that was taken on a bubble: throws the jar the bubble shows, starts the battle, or picks a
	 * jar from the cluster. Runs on the game thread; a failure turns the bubbles off.
	 *
	 * @param generation the generation of the click targets the press was made against
	 * @param kind the bubble that was pressed
	 * @param jar the jar of the pressed bubble, or null
	 * @param button the button, 1 for left and 3 for right
	 */
	private void bubblePressed(int generation, BubbleKind kind, JarType jar, int button)
	{
		try
		{
			if (!bubblesOn() || !saveLive())
				return;

			BubbleAction action = bubbles.press(generation, kind, jar, button, System.currentTimeMillis());
			if (action.kind() == BubbleAction.Kind.THROW)
			{
				throwJar(action.wildId(), game.jarToThrow());
			}
			else if (action.kind() == BubbleAction.Kind.BATTLE)
			{
				battle(action.wildId());
			}
			else if (action.kind() == BubbleAction.Kind.SELECT_JAR)
			{
				game.selectJar(action.jar());
				refreshPanel();
			}
		}
		catch (RuntimeException | AssertionError e)
		{
			bubblesBroke(e);
		}
	}

	private void startOnClientThread()
	{
		loaded = false;
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

		loadSave();
		StatBaseline.seed(Arrays.asList(Skill.values()), lastXp, lastLevel, client::getSkillExperience,
			client::getRealSkillLevel);
		Map<String, Integer> now = new HashMap<>();
		for (Map.Entry<Skill, Integer> reading : lastXp.entrySet())
			now.put(reading.getKey().name(), reading.getValue());

		ledger.seed(now);
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

	private ExamineTable loadExamine()
	{
		try (InputStream in = GielinorGoPlugin.class.getResourceAsStream(ExamineTable.RESOURCE))
		{
			if (in == null)
				return ExamineTable.empty();

			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				return ExamineTable.load(gson, reader);
			}
		}
		catch (IOException e)
		{
			log.warn("Gielinor Go: could not read the examine lines", e);
			return ExamineTable.empty();
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

	private BossTable loadBosses(SpeciesTable species)
	{
		try (InputStream in = GielinorGoPlugin.class.getResourceAsStream(BossTable.RESOURCE))
		{
			if (in == null)
				return BossTable.empty();

			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				return BossTable.load(gson, reader, species);
			}
		}
		catch (IOException e)
		{
			log.warn("Gielinor Go: could not read the boss list", e);
			return BossTable.empty();
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

	private RivalTable loadRival(SpeciesTable species)
	{
		try (InputStream in = GielinorGoPlugin.class.getResourceAsStream(RivalTable.RESOURCE))
		{
			if (in == null)
				return RivalTable.empty();

			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				return RivalTable.load(gson, reader, species);
			}
		}
		catch (IOException e)
		{
			log.warn("Gielinor Go: could not read the rival", e);
			return RivalTable.empty();
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

	private TeamTowns loadTeamTowns()
	{
		try (InputStream in = GielinorGoPlugin.class.getResourceAsStream(TeamTowns.RESOURCE))
		{
			if (in == null)
				return TeamTowns.empty();

			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				return TeamTowns.load(gson, reader);
			}
		}
		catch (IOException e)
		{
			log.warn("Gielinor Go: could not read the team-change towns", e);
			return TeamTowns.empty();
		}
	}

	private RematchTable loadRematches(SpeciesTable species)
	{
		try (InputStream in = GielinorGoPlugin.class.getResourceAsStream(RematchTable.RESOURCE))
		{
			if (in == null)
				return RematchTable.empty();

			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				return RematchTable.load(gson, reader, species);
			}
		}
		catch (IOException e)
		{
			log.warn("Gielinor Go: could not read the champion rematch teams", e);
			return RematchTable.empty();
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

	private SkillingTable loadSkilling(SpeciesTable species, AnimationTable animations)
	{
		try (InputStream in = GielinorGoPlugin.class.getResourceAsStream(SkillingTable.RESOURCE))
		{
			if (in == null)
				return SkillingTable.empty();

			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				return SkillingTable.load(gson, reader, species, animations);
			}
		}
		catch (IOException e)
		{
			log.warn("Gielinor Go: could not read the skilling list", e);
			return SkillingTable.empty();
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
		long account = client.getAccountHash();
		game.starterSeed(account == -1L ? null : Long.valueOf(account));
		saveProfileKey = configManager.getRSProfileKey();
		loaded = true;
		ledger.markUnreconciled();
		helloDue = true;
		helloAsksBack = true;
		unsaved = false;
		tracker.clear();
		clearTracker();
		refreshPanel();
	}

	private void saveNow()
	{
		if (!loaded || game == null)
			return;

		stampXp();
		SaveFlush.write(game, persistence, saveProfileKey, unsaved);
		unsaved = false;
	}

	/**
	 * Writes the XP the client reported into the save's snapshot, when the ledger allows it: only after this login was
	 * compared with the save, and never on a world that keeps its own XP.
	 */
	private void stampXp()
	{
		Map<String, Integer> xp = ledger.stamp();
		if (xp != null && game.stampXp(xp))
			unsaved = true;
	}

	/**
	 * Credits the XP gained since the save was last written, once per login, on the first game tick when every skill's
	 * login reading is in. It waits for the save of the logged-in account to be the one that is loaded. A failure is
	 * logged and never retried, so it cannot credit twice.
	 */
	private void reconcileXp()
	{
		boolean worldOk = OfflineXp.countsWorld(worldTypeNames());
		ledger.worldOk(worldOk);
		if (!ledger.reconcileDue(game.xpSnapshot().keySet()) || !worldOk || saveProfileKey == null
			|| !saveProfileKey.equals(configManager.getRSProfileKey()))
			return;

		try
		{
			if (config.offlineProgress())
				game.addOfflineXp(OfflineXp.compute(game.xpSnapshot(), ledger.login(), random));
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not count the XP gained while away", e);
		}
		finally
		{
			ledger.markReconciled();
			stampXp();
		}
	}

	private List<String> worldTypeNames()
	{
		List<String> names = new ArrayList<>();
		for (WorldType type : client.getWorldType())
			names.add(type.name());

		return names;
	}

	private void refreshPanel()
	{
		PanelState state = game.snapshot().withBattle(director.view());
		publishWindow(state);
		lastTracker = state.tracker();
		trackerOverlay.publish(trackerModel.frame(lastTracker, System.currentTimeMillis()));
		partyBoxOverlay.publish(PartyBoxModel.rows(state.team()));
	}

	/**
	 * Moves the tracker on one game tick: hands it the pinned eggs that hatched, unpins the tasks whose "Done!" has
	 * shown long enough, and draws the lines again so a finished line leaves on time rather than at the next refresh.
	 */
	private void trackerTick()
	{
		long now = System.currentTimeMillis();
		for (String label : game.takePinNews())
			trackerModel.hatched(label, now);

		for (String id : trackerModel.expired(now))
			game.unpinTask(TrackerView.taskSlot(id));

		trackerOverlay.publish(trackerModel.frame(lastTracker, now));
	}

	/** Forgets what the tracker shows, for when there is no save to show it from. */
	private void clearTracker()
	{
		trackerModel.clear();
		lastTracker = TrackerView.NONE;
		trackerOverlay.publish(Collections.emptyList());
		partyBoxOverlay.publish(Collections.emptyList());
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
			views.add(view.withTrade(trades.offerFrom(view.memberId()), trades.offerId(view.memberId()),
				trades.waitingOn(view.memberId())));

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

		boolean turnMessage = GielinorGoPartyMessage.TURN.equals(message.kind())
			|| GielinorGoPartyMessage.LEAVE.equals(message.kind())
			|| GielinorGoPartyMessage.READY.equals(message.kind());
		if (!partyRate.allow(message, me.getMemberId(), trades.awaitsReply(message, me.getMemberId()),
			turnMessage && friends.inTurnBattleWith(message.getMemberId()), System.currentTimeMillis()))
			return;

		String sender = friendName(partyService.getMemberById(message.getMemberId()));
		heldNotices = new ArrayList<>();
		try
		{
			TeamBattleResult result = friends.receive(message, me.getMemberId(), sender, game.data(),
				config.friendBattles() && !battleBusy(), config.chooseBattleActions(), !battleBusy());
			FriendTurnBattle started = friends.takeStarted();
			if (started != null)
				beginFriendBattle(started);

			boolean changed = friends.takeChanged() | result != null | started != null;
			changed |= trades.receive(message, me.getMemberId(), sender);
			saveNow();
			if (started == null)
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

	/**
	 * Resets the account's progress after the player confirmed in the panel. Refused while a battle, a trade or an
	 * Elite run is on. The old save is copied to a safety key first, then everything drawn for it is cleared.
	 */
	private void resetProgress()
	{
		if (battleOpen())
			return;

		String blocker = game.resetBlocker();
		if (blocker != null)
		{
			announce(Notice.player(blocker, Tone.BAD));
			return;
		}

		director.end();
		persistence.save(game.data());
		persistence.backUp();
		game.reset();
		persistence.save(game.data());
		game.takeDirty();
		unsaved = false;
		stage.clear();
		arena.clear();
		effects.clear();
		jarFlight.clear();
		helloDue = true;
	}

	private void challengeFriend(long memberId)
	{
		if (battleOpen())
			return;

		friends.challenge(memberId, friendName(partyService.getMemberById(memberId)), game.data(),
			config.chooseBattleActions());
	}

	/**
	 * Opens a battle against a friend in which both players choose the turns: the scene, the director and the
	 * opening line. The battle pays nothing and writes nothing back to the save but the items used.
	 *
	 * @param friend the battle shared with the friend
	 */
	private void beginFriendBattle(FriendTurnBattle friend)
	{
		heldNotices = HeldNotices.open(heldNotices);
		closeWindowForBattle();
		BattleSession battle = game.startFriendBattle(friend);
		BattleState state = battle.state();
		Combatant first = state.mine();
		boolean leadFirst = !battle.fighters().isEmpty() && battle.fighters().get(0) == game.followerCreature();
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
	 * Releases the creatures a panel picked, once it is checked that every one is still where it was listed.
	 *
	 * @param picks the creatures as the Storage tab showed them
	 */
	private void releaseMany(List<TradePick> picks)
	{
		PanelState state = game.snapshot();
		List<Integer> places = new ArrayList<>();
		for (TradePick pick : picks)
		{
			if (pick.from() != Holding.STORAGE || !pick.matches(state))
			{
				announce(Notice.player("What you picked has changed. Pick again", Tone.BAD));
				return;
			}

			places.add(pick.index());
		}

		game.release(places);
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

	private void answerTrade(long memberId, long offerId, boolean accept)
	{
		trades.answer(memberId, friendName(partyService.getMemberById(memberId)), offerId, accept);
		saveNow();
		if (loaded)
			refreshPanel();
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
	 * Plays the game sound for a cue, on the client thread, when sounds are on, the plugin is not hidden for PvP and
	 * no other sound played a moment ago. The game's own sound effect volume applies.
	 *
	 * @param cue the moment that happened
	 */
	private void playCue(Cue cue)
	{
		clientThread.invoke(() ->
		{
			try
			{
				if (config.sounds() && !hiddenForPvp() && cueGate.allow(cue, System.currentTimeMillis()))
					client.playSoundEffect(cue.soundId());
			}
			catch (RuntimeException e)
			{
				log.debug("Could not play a sound", e);
			}
		});
	}

	/**
	 * Works out which species a monster is, for turning it wild.
	 *
	 * @param npc the monster
	 * @return the species, or null when the monster is not in the species list, has no combat level (unless it is a
	 *     listed Hunter creature) or is a quest-only species (those never turn wild)
	 */
	private Species speciesOf(NPC npc)
	{
		if (npc == null || npc.getName() == null || game == null)
			return null;

		Species found = game.speciesFor(Text.removeTags(npc.getName()));
		return WildEligibility.eligible(found, npc.getId(), npc.getCombatLevel()) && !skilling.exclusive(found)
			? found : null;
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
		Creature lead = config.showFollower() && !hiddenForPvp() ? game.followerCreature() : null;
		leadTag = lead != null && config.showLeadName()
			? FollowerTagOverlay.nameFor(lead.nickname, game.speciesNameOf(lead), lead.level()) : null;
		follower.setWander(config.followerWander() && !followerModel.open());
		follower.gameTick(lead == null ? null : lead.species, lead == null ? 0 : lead.npcId,
			lead != null && lead.gilded);
	}

	private void offer(NPC npc)
	{
		Species species = speciesOf(npc);
		long now = System.currentTimeMillis();
		if (!loaded || species == null || !playable() || !nearPlayer(npc)
			|| !WildHiding.mayAppear(combatWatch.inCombat(now)))
			return;

		WorldPoint point = npc.getWorldLocation();
		boolean underground = point != null && Terrain.underground(point.getX(), point.getY());
		boolean events = config.spawnEvents();
		WildEntry entry = tracker.offer(npc.getIndex(), npc.getId(), species,
			WildEligibility.combatLevel(species, npc.getCombatLevel()), underground,
			now, calendar.monsterChance(species, config.wildChance(), events, spawnClock),
			game.gildedOneIn(calendar.gildedOneIn(species, events, spawnClock)), events);
		if (entry == null)
			return;

		if (bodies.spawn(entry, npc, now) == null)
		{
			tracker.withdraw(entry);
			return;
		}

		entry.withFirstMeeting(game.markSeen(entry)).record(game.recordOf(entry));
		if (entry.gilded())
			playCue(Cue.GILDED);

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

		Conditions world = Conditions.of(SpawnCalendar.isNight(spawnClock.localTime()),
			Terrain.underground(point.getX(), point.getY()));
		if (game.setConditions(world))
			refreshPanel();

		bankSpots.forgetFar(point.getX(), point.getY());
		game.setBankNear(bankSpots.near(point.getX(), point.getY(), point.getPlane()));
	}

	/**
	 * Notes whether the player is fighting a real monster this tick: they are interacting with an NPC, or an NPC is
	 * interacting with them.
	 */
	private void watchCombat()
	{
		try
		{
			Player player = client.getLocalPlayer();
			WorldView view = client.getTopLevelWorldView();
			if (player == null || view == null)
			{
				combatWatch.reset();
				return;
			}

			boolean fighting = player.getInteracting() instanceof NPC
				&& !RandomEvents.isRandomEvent(player.getInteracting().getName());
			for (NPC npc : view.npcs())
			{
				if (fighting)
					break;

				fighting = npc.getInteracting() == player && !RandomEvents.isRandomEvent(npc.getName());
			}

			if (fighting)
				combatWatch.noteFighting(System.currentTimeMillis());
		}
		catch (RuntimeException e)
		{
			log.debug("Gielinor Go: could not tell whether the player is in combat", e);
		}
	}

	private void addWildOptions(WildEntry entry)
	{
		Species species = entry.species();
		Rarity rarity = species.rarity();
		String target = WildMenu.target(entry.prefix(), species.name(), rarity.color().getRGB(), entry.level(),
			entry.record());
		int wildId = entry.wildId();
		List<WildMenu.Top> tops = wildTops(entry, jarChoices(entry));
		for (int i = tops.size() - 1; i >= 0; i--)
		{
			WildMenu.Top top = tops.get(i);
			if (top.kind() == WildMenu.TopKind.CATCH_PLAIN)
				addCatchOption(entry, target, top.jar());
			else if (top.kind() == WildMenu.TopKind.CATCH_SUBMENU)
				addCatchSubmenu(wildId, target, top.rows());
			else
				addBattleOption(wildId, target);
		}
	}

	/**
	 * Lays out the entries of a wild creature's menu, the first one on top.
	 *
	 * @param entry the wild creature
	 * @param choices every owned jar with its odds, from {@link #jarChoices}
	 * @return the entries, top first
	 */
	private List<WildMenu.Top> wildTops(WildEntry entry, List<JarSuggest.JarChoice> choices)
	{
		int wildId = entry.wildId();
		WildBody body = bodies.get(wildId);
		boolean canBattle = game.canBattle(System.currentTimeMillis(), config.chooseBattleActions());
		boolean withBattle = !entry.battled() && !battleBusy() && body != null && !body.life().leaving()
			&& canBattle;
		return WildMenu.layout(game.jarToThrow(), choices, withBattle,
			knockedOut(wildId) || !canBattle, WildMenu.SUBMENU);
	}

	/**
	 * Lists every jar the player owns with its odds against one wild creature right now.
	 *
	 * @param entry the wild creature
	 * @return one choice per owned jar, in enum order
	 */
	private List<JarSuggest.JarChoice> jarChoices(WildEntry entry)
	{
		boolean knockedOut = knockedOut(entry.wildId());
		double healthLeft = inBattleWith(entry) ? foeHealthLeft() : CatchOdds.NOT_IN_BATTLE;
		List<JarSuggest.JarChoice> choices = new ArrayList<>();
		for (Map.Entry<JarType, Integer> owned : game.ownedJars().entrySet())
		{
			JarType jar = owned.getKey();
			boolean matching = jar.specialty() && entry.traits().contains(jar.trait());
			choices.add(new JarSuggest.JarChoice(jar, owned.getValue(),
				CatchOdds.of(jar, entry, knockedOut, healthLeft), matching));
		}

		return choices;
	}

	/**
	 * Adds the Catch entry that opens the list of jars: the suggested ones, a grey line, then the rest. The entry
	 * itself does nothing when clicked; a row throws its jar.
	 *
	 * @param wildId the wild creature's id
	 * @param target the coloured creature name and level
	 * @param rows the rows of the list, top first
	 */
	private void addCatchSubmenu(int wildId, String target, List<WildMenu.Row> rows)
	{
		MenuEntry parent = client.getMenu()
			.createMenuEntry(-1)
			.setOption(WildMenu.CATCH)
			.setTarget(target)
			.setType(MenuAction.RUNELITE)
			.setDeprioritized(true);
		Menu sub = parent.createSubMenu();
		for (int i = rows.size() - 1; i >= 0; i--)
		{
			WildMenu.Row row = rows.get(i);
			if (row.kind() == WildMenu.RowKind.SEPARATOR)
			{
				sub.createMenuEntry(-1)
					.setOption("<col=808080>" + WildMenu.SEPARATOR + "</col>")
					.setType(MenuAction.RUNELITE);
				continue;
			}

			JarSuggest.JarChoice choice = row.choice();
			JarType jar = choice.jar();
			sub.createMenuEntry(-1)
				.setOption(WildMenu.jarOption(choice))
				.setTarget(WildMenu.jarTarget(choice))
				.setType(MenuAction.RUNELITE)
				.onClick(clicked -> throwJar(wildId, jar));
		}
	}

	private void addBattleOption(int wildId, String target)
	{
		client.getMenu()
			.createMenuEntry(-1)
			.setOption(WildMenu.BATTLE)
			.setTarget(target)
			.setType(MenuAction.RUNELITE)
			.setDeprioritized(true)
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
			double chance = CatchOdds.of(jar, entry, knockedOut(wildId),
				inBattleWith(entry) ? foeHealthLeft() : CatchOdds.NOT_IN_BATTLE);
			odds = " - " + jar.label() + " jar " + percent(chance);
		}

		client.getMenu()
			.createMenuEntry(-1)
			.setOption(WildMenu.CATCH)
			.setTarget(target + odds)
			.setType(MenuAction.RUNELITE)
			.setDeprioritized(true)
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

		if (inBattleWith(entry) && !director.automatic())
		{
			chooseBattle(BattleChoice.catching(jar));
			return;
		}

		CatchOutcome outcome = game.tryCatch(entry, jar, knockedOut(wildId));
		if (outcome == CatchOutcome.CAUGHT || outcome == CatchOutcome.FLED)
		{
			if (inBattleWith(entry))
				director.finish();

			tracker.remove(wildId);
		}

		showCatch(outcome, wildId, jar);
		refreshPanel();
	}

	/**
	 * Acts a throw out on screen, when that setting is on: the player throws whenever a jar was really used and
	 * then reacts to the result, and the creature's body reacts when the jar lands: a caught one shrinks into the
	 * jar, one that breaks free attacks and stands again, one that flees runs off fading out. With the setting off a
	 * caught or fled creature simply disappears. A caught or fled creature also ends the battle the stage acts out
	 * with it, so no queued attack plays after the catch.
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
		boolean throwing = config.catchEffects() && effects.throwJar();
		int delay = throwing ? jarFlight.start(jar, () -> bodies.spotOf(wildId)) : 0;
		boolean played = config.catchEffects() && body != null && body.beginCatch(outcome, delay);
		if (throwing)
			effects.react(outcome, delay);

		boolean gone = outcome == CatchOutcome.CAUGHT || outcome == CatchOutcome.FLED;
		if (gone && !played)
			bodies.remove(wildId, System.currentTimeMillis());

		if (gone)
			stage.forget(wildId);
	}

	private void battle(int wildId)
	{
		WildEntry entry = tracker.get(wildId);
		WildBody body = bodies.get(wildId);
		if (entry == null || body == null || body.life().leaving() || battleBusy())
			return;

		friends.cancelChallenge();
		closeWindowForBattle();

		if (config.chooseBattleActions())
		{
			startSteeredBattle(wildId, entry);
			return;
		}

		startAutomaticBattle(wildId, entry);
	}

	/**
	 * Starts a wild battle that plays itself. It is played turn by turn like a steered one, so Run can stop it. With
	 * "Battle animations" on the lead walks up and the turns are acted out; otherwise the battle is played in text.
	 *
	 * @param wildId the wild creature's id
	 * @param entry the wild creature
	 */
	private void startAutomaticBattle(int wildId, WildEntry entry)
	{
		beginWild(wildId, game.startAutoBattle(entry), true);
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
		noticeBoard.stretch(stage.takeExtraTicks());
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

	/** @return whether a battle is on that a new one must wait for: one the player steers, or one still playing */
	private boolean battleBusy()
	{
		return director.active();
	}

	private void challenge(String championId)
	{
		if (battleBusy())
			return;

		friends.cancelChallenge();

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

		friends.cancelChallenge();

		if (config.chooseBattleActions())
		{
			startSteeredElite(true);
			return;
		}

		heldNotices = new ArrayList<>();
		playTeamBattle(game.eliteChallenge(), true);
	}

	private void eliteContinue()
	{
		if (battleOpen())
			return;

		friends.cancelChallenge();

		if (config.chooseBattleActions())
		{
			startSteeredElite(false);
			return;
		}

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
		if (!battleBusy())
			return false;

		announce(Notice.player("Finish the battle first", Tone.BAD));
		return true;
	}

	private void eliteForfeit()
	{
		heldNotices = new ArrayList<>();
		BattleSession battle = director.session();
		if (battle != null && battle.isElite())
		{
			director.giveUp();
			flushHeld();
			refreshPanel();
			return;
		}

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
		if (result != null)
			closeWindowForBattle();

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
		List<Notice> after = HeldNotices.close(heldNotices);
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

			Notice notice;
			if (staged && wildId != Notice.PLAYER && line.overPlayer())
				notice = Notice.stageLead(line.text(), line.tone());
			else if (staged && wildId != Notice.PLAYER)
				notice = Notice.stageFoe(wildId, line.text(), line.tone());
			else if (line.overPlayer() || wildId == Notice.PLAYER)
				notice = Notice.player(line.text(), line.tone());
			else
				notice = Notice.creature(wildId, line.text(), line.tone());

			queue.add(notice);
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

	/**
	 * @param wildId a wild creature's id
	 * @return whether it is the foe of the battle in progress, which real combat never hides
	 */
	private boolean inBattleWithWild(int wildId)
	{
		BattleSession battle = director.session();
		return battle != null && battle.wild() && battle.entry().wildId() == wildId;
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
		beginWild(wildId, game.startBattle(entry), false);
	}

	/**
	 * Puts a wild battle that has just started in the director's hands, on the stage when it can be acted out.
	 *
	 * @param wildId the wild creature's id
	 * @param battle the battle, or null when none could start
	 * @param automatic whether it plays itself
	 */
	private void beginWild(int wildId, BattleSession battle, boolean automatic)
	{
		if (heldNotices == null)
			heldNotices = new ArrayList<>();

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

		WildScene scene = staged ? new WildScene(stage) : null;
		if (automatic)
			director.beginAutomatic(battle, scene);
		else
			director.begin(battle, scene);

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
		beginSteeredTeam(game.startChallenge(championId));
	}

	/**
	 * Starts the next battle of the Elite challenge, which waits for the player's choice each turn.
	 *
	 * @param start whether this starts a run, rather than carrying on with the one that is open
	 */
	private void startSteeredElite(boolean start)
	{
		heldNotices = new ArrayList<>();
		BattleSession battle = start || game.eliteOpen() ? game.eliteSteered() : null;
		beginSteeredTeam(battle);
	}

	/**
	 * Opens a team battle the player steers, a City Champion's or an Elite team's: the scene, the director and the
	 * opening line.
	 *
	 * @param battle the battle, or null when none could start
	 */
	private void beginSteeredTeam(BattleSession battle)
	{
		if (battle == null)
		{
			playBack(null, Notice.PLAYER, false);
			refreshPanel();
			return;
		}

		closeWindowForBattle();
		BattleState state = battle.state();
		Combatant first = state.mine();
		List<Creature> fighters = battle.fighters();
		boolean leadFirst = fighters.get(0) == game.followerCreature();
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
			if (kind == BattleChoice.Kind.RUN && director.automatic())
			{
				askToRun();
				return;
			}

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

	/** Asks to run from the battle that plays itself, and says so once. */
	private void askToRun()
	{
		boolean already = director.fleeing();
		if (director.flee() && !already)
			announce(Notice.player("Trying to run", Tone.INFO));
	}

	/**
	 * Moves a battle the player steers on by one game tick: opens the choice when the scene is ready, counts the
	 * time, plays the creature's own attack when it is up, and ends the battle as running when the player left it.
	 * The panel is redrawn every tick while a choice is open so the countdown moves.
	 */
	private void steering()
	{
		autoBattleOpen = director.automatic();
		if (!director.active())
		{
			if (choiceOpen)
			{
				choiceOpen = false;
				battleControls.model().show(null, BattleControlsModel.Keys.NONE);
				refreshPanel();
			}

			return;
		}

		heldNotices = new ArrayList<>();
		director.gameTick(steerForfeits());
		flushHeld();
		boolean open = director.promptOpen();
		choiceOpen = open;
		battleControls.model().show(open ? director.view() : null, controlKeys());
		if (open || !director.active())
			refreshPanel();
	}

	private BattleControlsModel.Keys controlKeys()
	{
		return new BattleControlsModel.Keys(keyText(config.attackKey()), keyText(config.switchKey()),
			keyText(config.catchKey()), keyText(config.itemKey()), keyText(config.runKey()));
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
	 * goes to the usual catch effects, and the director ends the scene right after. A jar that does not hold plays
	 * the throw and the battle goes on.
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

		effects.react(outcome, jarFlight.start(jar, () -> bodies.spotOf(wildId)));
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

	/**
	 * Compares the version in the change log with the one stored the last time the change log was opened. A first
	 * install records the current version without telling the player what changed.
	 *
	 * @return true when the panel should show What's New
	 */
	private boolean detectVersionChange()
	{
		String current = Changelog.load().currentVersion();
		String lastSeen = config.lastSeenVersion();
		if (WhatsNew.recordSilently(lastSeen, current))
		{
			configManager.setConfiguration(GielinorGoConfig.GROUP, GielinorGoConfig.LAST_SEEN_VERSION, current);
			return false;
		}

		boolean dismissed = config.whatsNewDismissed();
		if (dismissed && current != null && !current.equals(lastSeen))
		{
			configManager.setConfiguration(GielinorGoConfig.GROUP, GielinorGoConfig.WHATS_NEW_DISMISSED, false);
			dismissed = false;
		}

		return WhatsNew.shouldShow(lastSeen, current, dismissed);
	}

	/** @return the panel's actions, each carried over to the game thread and followed by a panel refresh */
	private PanelActions panelActions()
	{
		return new PanelActions()
		{
			@Override
			public void openWiki(String species)
			{
				LinkBrowser.browse(WikiLinks.urlFor(species));
			}

			@Override
			public void whatsNewSeen()
			{
				String current = Changelog.load().currentVersion();
				if (current == null)
					return;

				configManager.setConfiguration(GielinorGoConfig.GROUP, GielinorGoConfig.LAST_SEEN_VERSION, current);
				configManager.setConfiguration(GielinorGoConfig.GROUP, GielinorGoConfig.WHATS_NEW_DISMISSED, true);
			}

			@Override
			public void pickStarter(int index)
			{
				onGameThread(() -> game.pickStarter(index));
			}

			@Override
			public void resetProgress()
			{
				onGameThread(GielinorGoPlugin.this::resetProgress);
			}

			@Override
			public void setLead(TradePick pick)
			{
				onGameThread(() -> game.setLead(pick));
			}

			@Override
			public void reviveAtBank(TradePick pick)
			{
				onGameThread(() ->
				{
					if (battleOpen())
						return;

					lookForBank();
					game.reviveAtBank(pick);
				});
			}

			@Override
			public void moveToStorage(TradePick pick)
			{
				onGameThread(() ->
				{
					lookForBank();
					game.moveToStorage(pick);
				});
			}

			@Override
			public void moveToTeam(TradePick pick)
			{
				onGameThread(() ->
				{
					lookForBank();
					game.moveToTeam(pick);
				});
			}

			@Override
			public void arrangeStorage(TradePick from, TradePick onto)
			{
				onGameThread(() -> game.arrangeStorage(from, onto));
			}

			@Override
			public void release(TradePick pick)
			{
				onGameThread(() -> game.release(pick));
			}

			@Override
			public void releaseMany(List<TradePick> picks)
			{
				onGameThread(() -> GielinorGoPlugin.this.releaseMany(picks));
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
			public void pinTask(int slot, boolean pinned)
			{
				onGameThread(() ->
				{
					if (pinned)
						game.pinTask(slot);
					else
						game.unpinTask(slot);
				});
			}

			@Override
			public void pinEgg(TradePick pick, boolean pinned)
			{
				onGameThread(() -> game.pinEgg(pick, pinned));
			}

			@Override
			public void pickTitle(String title)
			{
				onGameThread(() -> game.pickTitle(title));
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
			public void answerTrade(long memberId, long offerId, boolean accept)
			{
				onGameThread(() -> GielinorGoPlugin.this.answerTrade(memberId, offerId, accept));
			}
		};
	}

	/**
	 * Looks around the player for a bank and checks the bank interface, so a swap is judged on what is there now,
	 * including a bank that was already in view before any spawn event could be seen.
	 */
	private void lookForBank()
	{
		bankWatch.scanAroundPlayer();
		Player player = client.getLocalPlayer();
		if (player != null)
		{
			WorldPoint point = player.getWorldLocation();
			game.setBankNear(bankSpots.near(point.getX(), point.getY(), point.getPlane()));
		}

		game.setBankOpen(bankWatch.bankOpen());
	}

	/**
	 * Applies the painted window's settings that act at once: a new size preset resizes and centres the window.
	 *
	 * @param event the setting change
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (window == null || !GielinorGoConfig.GROUP.equals(event.getGroup()))
			return;

		if (GielinorGoConfig.WINDOW_SIZE.equals(event.getKey()))
			window.applyPreset(config.windowSize());
	}

	/** @return the style the painted window and its overlays use: glass with its border, or solid when it is off */
	private WindowTheme windowStyle()
	{
		return WindowTheme.of(config.glassLook(), config.glassBorder());
	}

	/** Creates the painted window, its toggle, its notice and their input, and registers them. */
	private void startWindow()
	{
		windowNotice = new WindowNotice(config::windowAnimations, this::windowStyle, System::currentTimeMillis,
			client::getCanvasWidth, client::getCanvasHeight);
		window = new GoWindow(config, configManager, overlayManager, System::currentTimeMillis,
			client::getCanvasWidth, client::getCanvasHeight, this::showNotice);
		windowPortraits = new PortraitCache(portraits, System::currentTimeMillis);
		WindowPages pages = new WindowPages(window.shell(), panelActions(), windowPortraits,
			System::currentTimeMillis, PageText.Sink.NONE);
		biomePlayer = BiomePlayer.bundled(executor, gson);
		pages.scenery(new SummaryScene(BiomeTable.bundled(gson), biomePlayer, config::animatedBackgrounds));
		idleStrips = new IdleService(client, clientThread, executor, game::speciesFor, loadAnimations(),
			config::idleAnimation);
		pages.idle(idleStrips);
		window.shell().pages(pages);
		BufferedImage jar = ImageUtil.loadImageResource(GielinorGoPlugin.class, "panel_icon_large.png");
		windowToggle = new WindowToggle(this::windowStyle, config::windowAnimations,
			System::currentTimeMillis, jar);
		dragKey = new DragKey(runeLiteConfig::dragHotkey);
		windowInput = new WindowInput(window::view, windowToggle::bounds, dragKey, windowSink());
		windowKeys = new WindowKeys(window::view, dragKey, config::windowKey, config::windowKeyboard,
			() -> chatTyping, windowKeySink());
		overlayManager.add(window);
		overlayManager.add(windowToggle);
		overlayManager.add(windowNotice);
		mouseManager.registerMouseListener(windowInput);
		mouseManager.registerMouseWheelListener(windowInput);
		keyManager.registerKeyListener(windowKeys);
	}

	/** Unregisters the painted window and everything that belongs to it. */
	private void stopWindow()
	{
		keyManager.unregisterKeyListener(windowKeys);
		mouseManager.unregisterMouseListener(windowInput);
		mouseManager.unregisterMouseWheelListener(windowInput);
		overlayManager.remove(window);
		overlayManager.remove(windowToggle);
		overlayManager.remove(windowNotice);
		windowNotice.clear();
		windowPortraits.clear();
		chatTyping = false;
		firstRunNoticed = false;
	}

	/** Creates the painted battle controls and their mouse input, and registers them. */
	private void startControls()
	{
		BattleControlsModel model = new BattleControlsModel(this::chooseBattle);
		battleControls = new BattleControls(model, this::windowStyle, config::windowFraction,
			client::getCanvasWidth, client::getCanvasHeight);
		controlsInput = new WindowInput(battleControls::view, () -> null, dragKey, controlsSink(model));
		overlayManager.add(battleControls);
		mouseManager.registerMouseListener(controlsInput);
		mouseManager.registerMouseWheelListener(controlsInput);
	}

	/** Creates the pinned tracker and registers it. It takes no mouse input, so every click goes to the game. */
	private void startTracker()
	{
		trackerModel = new TrackerModel();
		trackerOverlay = new TrackerOverlay(this::windowStyle, config::windowFraction, client::getCanvasWidth,
			client::getCanvasHeight, config::showTracker, director::active, () -> loaded,
			() -> onGameThread(game::unpinAll));
		overlayManager.add(trackerOverlay);
	}

	/** Creates the pinned party box and the mouse input that reorders the party from it, and registers both. */
	private void startPartyBox()
	{
		partyBoxOverlay = new PartyBoxOverlay(windowPortraits, this::windowStyle, config::windowFraction,
			client::getCanvasWidth, client::getCanvasHeight, config::pinParty, director::active, () -> loaded,
			System::currentTimeMillis);
		partyBoxInput = new PartyBoxInput(partyBoxOverlay::draggableRows, dragKey,
			point -> bubbleOverlay != null && bubbleOverlay.hits().at(point.x, point.y) != null,
			(from, onto) -> onGameThread(() -> swapTeam(from, onto)));
		partyBoxOverlay.reorder(this::mayReorderParty, partyBoxInput);
		overlayManager.add(partyBoxOverlay);
		mouseManager.registerMouseListener(partyBoxInput);
	}

	/**
	 * @return whether the pinned box may reorder the party now: not in a Go battle or real combat, where a click is
	 *     meant for something else, and not while the game's menu is open
	 */
	private boolean mayReorderParty()
	{
		return loaded && saveLive() && !battleOn() && !client.isMenuOpen()
			&& !combatWatch.inCombat(System.currentTimeMillis());
	}

	private void swapTeam(int from, int onto)
	{
		if (!mayReorderParty())
			return;

		game.swapTeam(from, onto);
		refreshPanel();
	}

	/** Unregisters the pinned party box. */
	private void stopPartyBox()
	{
		mouseManager.unregisterMouseListener(partyBoxInput);
		overlayManager.remove(partyBoxOverlay);
		partyBoxOverlay.publish(Collections.emptyList());
	}

	/** Unregisters the pinned tracker. */
	private void stopTracker()
	{
		overlayManager.remove(trackerOverlay);
		trackerOverlay.publish(Collections.emptyList());
	}

	/** Unregisters the painted battle controls. */
	private void stopControls()
	{
		mouseManager.unregisterMouseListener(controlsInput);
		mouseManager.unregisterMouseWheelListener(controlsInput);
		overlayManager.remove(battleControls);
		battleControls.model().show(null, BattleControlsModel.Keys.NONE);
	}

	/**
	 * @param model the controls' model
	 * @return where the mouse events of the battle controls go, each carried over to the game thread
	 */
	private WindowInput.Sink controlsSink(BattleControlsModel model)
	{
		return new WindowInput.Sink()
		{
			@Override
			public void moved(int x, int y)
			{
				clientThread.invoke(() -> model.onMoved(x, y));
			}

			@Override
			public void left()
			{
				clientThread.invoke(model::onLeft);
			}

			@Override
			public void pressed(int x, int y, int button, int modifiers)
			{
				clientThread.invoke(() -> model.onPressed(x, y, button));
			}

			@Override
			public void released(int x, int y, int button, int modifiers)
			{
				clientThread.invoke(() -> model.onReleased(x, y, button));
			}

			@Override
			public void dragged(int x, int y)
			{
				return;
			}

			@Override
			public void wheel(int x, int y, int rotation)
			{
				return;
			}

			@Override
			public void clickedOutside()
			{
				return;
			}

			@Override
			public void toggleClicked()
			{
				return;
			}
		};
	}

	/** @return where the mouse events of the window go, each carried over to the game thread */
	private WindowInput.Sink windowSink()
	{
		return new WindowInput.Sink()
		{
			@Override
			public void moved(int x, int y)
			{
				clientThread.invoke(() -> window.shell().onMoved(x, y, System.currentTimeMillis()));
			}

			@Override
			public void left()
			{
				clientThread.invoke(() -> window.shell().onLeft());
			}

			@Override
			public void pressed(int x, int y, int button, int modifiers)
			{
				clientThread.invoke(() -> window.shell().onPressed(x, y, button, modifiers,
					System.currentTimeMillis()));
			}

			@Override
			public void released(int x, int y, int button, int modifiers)
			{
				clientThread.invoke(() -> window.shell().onReleased(x, y, button, System.currentTimeMillis()));
			}

			@Override
			public void dragged(int x, int y)
			{
				clientThread.invoke(() -> window.shell().onDragged(x, y));
			}

			@Override
			public void wheel(int x, int y, int rotation)
			{
				clientThread.invoke(() -> window.shell().onWheel(x, y, rotation));
			}

			@Override
			public void clickedOutside()
			{
				clientThread.invoke(() -> window.shell().focusLost(System.currentTimeMillis()));
			}

			@Override
			public void toggleClicked()
			{
				clientThread.invoke(GielinorGoPlugin.this::toggleWindow);
			}

			@Override
			public boolean resetRequested()
			{
				clientThread.invoke(() -> window.shell().onResetRequested());
				return true;
			}
		};
	}

	/** @return where the key events of the window go, each carried over to the game thread */
	private WindowKeys.Sink windowKeySink()
	{
		return new WindowKeys.Sink()
		{
			@Override
			public void pressed(int keyCode, int modifiers)
			{
				clientThread.invoke(() -> window.shell().onKey(keyCode, modifiers, System.currentTimeMillis()));
			}

			@Override
			public void typed(char c, int modifiers)
			{
				clientThread.invoke(() -> window.shell().onTyped(c, modifiers, System.currentTimeMillis()));
			}

			@Override
			public void openKey()
			{
				clientThread.invoke(GielinorGoPlugin.this::toggleWindow);
			}
		};
	}

	/**
	 * Opens the painted window, or closes it when it is open. It does not open during combat, a battle or before the
	 * save is read: the toggle shakes and a notice says why.
	 */
	private void toggleWindow()
	{
		long now = System.currentTimeMillis();
		WindowShell shell = window.shell();
		if (shell.isOpen())
		{
			shell.close(now, ShellMotion.Phase.CLOSE);
			return;
		}

		NoticeMessage blocked = WindowRules.blockedNotice(combatWatch.inCombat(now), battleOn(),
			loaded && saveLive());
		if (blocked == null)
		{
			shell.open(now);
			return;
		}

		windowToggle.shake();
		noticeUnderToggle(blocked);
	}

	/**
	 * Watches what closes the window on every client tick: real combat, a Gielinor Go battle, logout and the login
	 * screen. It greys the toggle, shows the pulsing dot, notes whether the player is typing in chat and clears the
	 * game's menu entries under the window.
	 */
	private void watchWindow()
	{
		if (window == null)
			return;

		long now = System.currentTimeMillis();
		boolean fighting = combatWatch.inCombat(now);
		boolean battle = director != null && battleOn();
		boolean locked = !WindowRules.mayOpen(fighting, battle, loaded && saveLive());
		windowToggle.locked(locked);
		WindowShell shell = window.shell();
		if (shell.isOpen() && locked)
		{
			NoticeMessage why = WindowRules.closedNotice(fighting, battle);
			boolean quick = why != null;
			shell.close(now, quick ? ShellMotion.Phase.COMBAT_CLOSE : ShellMotion.Phase.CLOSE);
			if (quick)
			{
				Rectangle b = window.screenBounds();
				showNotice(why, b.x + b.width / 2, b.y + b.height / 2);
			}
		}

		windowToggle.waiting(shell.waiting());
		boolean typing = chatboxFocused();
		if (typing && !chatTyping && shell.isOpen())
			shell.focusLost(now);

		chatTyping = typing;
		clearMenuUnderWindow();
	}

	/** @return whether a Gielinor Go battle is on, one the player steers or one acted out on screen */
	private boolean battleOn()
	{
		return WindowRules.battleOn(battleBusy(), stage.active(), arena.active());
	}

	/**
	 * Closes the painted window as a battle starts, with the notice that says why. Every path that starts a battle
	 * calls it, so the window is gone whether the battle was started from the window, a bubble, the menu or a friend.
	 */
	private void closeWindowForBattle()
	{
		if (window == null || !window.shell().isOpen())
			return;

		long now = System.currentTimeMillis();
		window.shell().close(now, ShellMotion.Phase.COMBAT_CLOSE);
		Rectangle b = window.screenBounds();
		showNotice(NoticeMessage.CLOSED_BATTLE, b.x + b.width / 2, b.y + b.height / 2);
	}

	/** Closes the painted window at once, for logout and world hop. */
	private void closeWindow()
	{
		if (window != null)
			window.shell().close(System.currentTimeMillis(), ShellMotion.Phase.CLOSE);
	}

	/**
	 * Tells whether the chat box is taking keys, so the window's keys stand aside. It is the check the Key Remapping
	 * plugin makes.
	 *
	 * @return true while the chat box has a key listener
	 */
	private boolean chatboxFocused()
	{
		try
		{
			Widget chatbox = client.getWidget(InterfaceID.Chatbox.UNIVERSE);
			return chatbox != null && chatbox.getOnKeyListener() != null;
		}
		catch (RuntimeException e)
		{
			log.debug("Gielinor Go: could not tell whether the player is typing in chat", e);
			return false;
		}
	}

	/**
	 * While the mouse is over the open window or the toggle, and RuneLite's drag key is not held, clears the game's
	 * menu entries so the game shows no "Walk here" and offers nothing under the window.
	 */
	private void clearMenuUnderWindow()
	{
		try
		{
			net.runelite.api.Point mouse = client.getMouseCanvasPosition();
			if (mouse == null || client.isMenuOpen() || dragKey.held())
				return;

			Rectangle toggle = windowToggle.bounds();
			boolean over = window.view().covers(mouse.getX(), mouse.getY())
				|| battleControls.view().covers(mouse.getX(), mouse.getY())
				|| toggle != null && toggle.contains(mouse.getX(), mouse.getY());
			if (over)
				client.getMenu().setMenuEntries(new MenuEntry[0]);
		}
		catch (RuntimeException e)
		{
			log.debug("Gielinor Go: could not clear the menu under the window", e);
		}
	}

	/**
	 * Shows a pop-up notice, and writes it to the chat box too when that setting is on.
	 *
	 * @param message what to say
	 * @param centerX the centre x on the canvas
	 * @param centerY the centre y on the canvas
	 */
	private void showNotice(NoticeMessage message, int centerX, int centerY)
	{
		windowNotice.show(message, centerX, centerY);
		if (config.chatMessages())
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message.text(), null);
	}

	private void noticeUnderToggle(NoticeMessage message)
	{
		Rectangle b = windowToggle.bounds();
		if (b == null)
			b = new Rectangle(0, 0, WindowToggle.SIZE, WindowToggle.SIZE);

		showNotice(message, b.x + b.width / 2, b.y + b.height + WindowToggle.SIZE / 2);
	}

	/**
	 * Hands the painted window a fresh snapshot, built here on the game thread, and tells a new player where to click.
	 *
	 * @param state what the side panel shows
	 */
	private void publishWindow(PanelState state)
	{
		if (window == null)
			return;

		windowVersion++;
		boolean storage = WindowRules.storageEnabled(game.inTeamArea(), game.nearBank());
		WindowSnapshot snapshot = WindowSnapshot.of(windowVersion, state, storage).withFriends(friendsState());
		window.shell().publish(snapshot);
		if (!snapshot.firstRun())
		{
			firstRunNoticed = false;
		}
		else if (!firstRunNoticed && !window.shell().isOpen())
		{
			firstRunNoticed = true;
			noticeUnderToggle(NoticeMessage.FIRST_RUN);
		}
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
