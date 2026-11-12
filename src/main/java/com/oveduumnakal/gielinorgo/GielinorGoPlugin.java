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
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.StatChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
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
	private PartyService partyService;

	@Inject
	private WSClient wsClient;

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
	private List<Notice> heldNotices;
	private FloatingTextOverlay floatingText;
	private FollowerTagOverlay followerTag;
	/** The lead creature's nickname for the overlay, kept up to date on the game thread. Null for none. */
	private volatile String leadTag;
	private GoPanel panel;
	private PortraitService portraits;
	private FollowerService follower;
	private CatchEffects effects;
	private BattleStage stage;
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
		friends = new FriendBattles(species, random, System::currentTimeMillis, this::sendToParty, this::announce);
		trades = new FriendTrades(species, game, random, System::currentTimeMillis, this::sendToParty,
			this::announce);
		game.tradeLocks(trades);
		wsClient.registerMessage(GielinorGoPartyMessage.class);
		lastHelloTick = -HELLO_TICKS;
		persistence = new GoPersistence(configManager, gson);
		wildOverlay = new WildOverlay(this, config, outlineRenderer);
		floatingText = new FloatingTextOverlay(client, this, noticeBoard,
			anchor -> stage == null ? null : stage.spotOf(anchor));
		overlayManager.add(wildOverlay);
		overlayManager.add(floatingText);
		portraits = new PortraitService(client, clientThread, game::speciesFor);
		AnimationTable animations = loadAnimations();
		follower = new FollowerService(client, animations, game::speciesFor);
		followerTag = new FollowerTagOverlay(client, follower, () -> leadTag);
		overlayManager.add(followerTag);
		effects = new CatchEffects(client);
		renderCallbackManager.register(effects);
		stage = new BattleStage(client, follower, animations);
		renderCallbackManager.register(stage);
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
		clientThread.invoke(this::saveNow);
		clientThread.invoke(stage::clear);
		clientThread.invoke(follower::remove);
		renderCallbackManager.unregister(effects);
		renderCallbackManager.unregister(stage);
		clientThread.invoke(effects::clear);
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
	 * @param npc a monster
	 * @return whether a battle is acted out in its place, so it is hidden and must not be outlined
	 */
	boolean isStaged(NPC npc)
	{
		return stage.isStaged(npc);
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
			stage.clear();
			follower.sceneLoading();
		}

		if (state != GameState.LOGIN_SCREEN && state != GameState.HOPPING)
			return;

		saveNow();
		trades.clear();
		stage.clear();
		follower.hide();
		effects.clear();
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
		effects.forget(event.getNpc());
		stage.forget(event.getNpc());
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
		battleScene();
		noticeBoard.tick(System.currentTimeMillis());
		portraits.tick();
		countSteps();
		updateFollower();
		greetParty();
		if (!playable())
			tracker.clearWild(System.currentTimeMillis());
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
	 * Moves the follower and the catch effects on a little. Client ticks come 50 times a second, so both are
	 * smooth.
	 *
	 * @param event the client tick
	 */
	@Subscribe
	public void onClientTick(ClientTick event)
	{
		effects.clientTick();
		if (!loaded)
			return;

		follower.clientTick();
		stage.clientTick();
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
	 * Adds the Catch and Battle options to the right-click menu of a wild monster, and the Catch options to the menu
	 * of a battle's stand-in while the mouse is over it. All are client-side entries.
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

		int standIn = stage.stagedIndex();
		boolean overStandIn = standIn >= 0 && tracker.get(standIn) != null && stage.mouseOverStandIn();
		for (int index : wildInMenu)
			addWildOptions(tracker.get(index), true);

		if (overStandIn && !wildInMenu.contains(standIn))
			addWildOptions(tracker.get(standIn), false);
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
		PanelState state = game.snapshot();
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
			playBack(result == null ? null : result.lines(), Notice.PLAYER);
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
		leadTag = lead != null && config.showLeadName() ? lead.nickname : null;
		follower.gameTick(lead == null ? null : lead.species, lead != null && lead.gilded);
	}

	private void offer(NPC npc)
	{
		Species species = speciesOf(npc);
		if (!loaded || species == null || !playable())
			return;

		long now = System.currentTimeMillis();
		if (tracker.restore(npc.getIndex(), npc.getId(), now) != null)
			return;

		WorldPoint point = npc.getWorldLocation();
		boolean underground = point != null && Terrain.underground(point.getX(), point.getY());
		WildEntry entry = tracker.offer(npc.getIndex(), npc.getId(), species, npc.getCombatLevel(), underground,
			now, config.wildChance());
		if (entry == null)
			return;

		game.markSeen(entry);
		String kind = entry.gilded() ? "A gilded " : "A wild ";
		announce(Notice.creature(entry.npcIndex(), kind + species.name() + " appeared!",
			entry.gilded() ? Tone.FIND : Tone.INFO));
	}

	private void offerRandomCandidate()
	{
		long now = System.currentTimeMillis();
		List<NPC> pool = new ArrayList<>();
		for (NPC npc : candidates.values())
		{
			if (tracker.canRestore(npc.getIndex(), npc.getId(), now))
				tracker.restore(npc.getIndex(), npc.getId(), now);
			else if (tracker.mayRoll(npc.getIndex(), npc.getId(), now))
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
	}

	private void addWildOptions(WildEntry entry, boolean withBattle)
	{
		Species species = entry.species();
		Rarity rarity = species.rarity();
		String name = ColorUtil.wrapWithColorTag(species.name(), rarity.color());
		String target = (entry.gilded() ? "Gilded " : "") + name + " (Lv " + entry.level() + ")";
		int index = entry.npcIndex();
		JarType standard = game.jarToThrow();
		for (JarType specialty : game.matchingSpecialtyJars(entry))
			addCatchOption(entry, target, specialty);

		addCatchOption(entry, target, standard);
		if (!withBattle || entry.battled())
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
			double chance = CatchRules.chance(jar, entry.combatLevel(), entry.weakened(), entry.traits(),
				stage.knockedOut(candidates.get(index)));
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

		CatchOutcome outcome = game.tryCatch(entry, jar, stage.knockedOut(candidates.get(npcIndex)));
		if (outcome == CatchOutcome.CAUGHT || outcome == CatchOutcome.FLED)
			tracker.remove(npcIndex, System.currentTimeMillis());

		showCatch(outcome, npcIndex);
		refreshPanel();
	}

	/**
	 * Acts a throw out on screen, when that setting is on: the player throws whenever a jar was really used, and
	 * a caught creature vanishes and fades back in.
	 *
	 * @param outcome what the throw led to
	 * @param npcIndex the game's index for the monster thrown at
	 */
	private void showCatch(CatchOutcome outcome, int npcIndex)
	{
		boolean thrown = outcome != CatchOutcome.NO_JARS && outcome != CatchOutcome.FULL;
		if (!thrown)
			return;

		stage.forget(candidates.get(npcIndex));
		if (!config.catchEffects())
			return;

		effects.throwJar();
		if (outcome == CatchOutcome.CAUGHT)
			effects.vanish(candidates.get(npcIndex));
	}

	private void battle(int npcIndex)
	{
		WildEntry entry = tracker.get(npcIndex);
		if (entry == null)
			return;

		heldNotices = new ArrayList<>();
		BattleResult result = game.battle(entry);
		List<BattleLine> lines = result == null ? null : result.summary(BATTLE_LINES);
		boolean staged = lines != null && config.battleAnimations()
			&& stage.start(candidates.get(npcIndex), lines, result.won());
		playBack(lines, npcIndex, staged);
		refreshPanel();
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

		stage.gameTick();
		if (noticeBoard.holding() && (stage.fighting() || !stage.active()))
			noticeBoard.release();
	}

	private void challenge(String championId)
	{
		heldNotices = new ArrayList<>();
		TeamBattleResult result = game.challenge(championId);
		playBack(result == null ? null : result.lines(), Notice.PLAYER);
	}

	/**
	 * Shows a battle one line per game tick, followed by whatever the game announced while resolving it, so
	 * rewards never float up before the hits that earned them.
	 *
	 * @param lines the battle's lines, or null when no battle started
	 * @param npcIndex the game's index for the wild creature, or {@link Notice#PLAYER} when there is none
	 */
	private void playBack(List<BattleLine> lines, int npcIndex)
	{
		playBack(lines, npcIndex, false);
	}

	/**
	 * Shows a battle one line per game tick, followed by whatever the game announced while resolving it.
	 *
	 * @param lines the battle's lines, or null when no battle started
	 * @param npcIndex the game's index for the wild creature, or {@link Notice#PLAYER} when there is none
	 * @param staged whether the battle is acted out on screen, so its lines wait for the lead creature to arrive
	 */
	private void playBack(List<BattleLine> lines, int npcIndex, boolean staged)
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
			if (staged && npcIndex != Notice.PLAYER && line.overPlayer())
				queue.add(Notice.stageLead(line.text(), line.tone()));
			else if (staged && npcIndex != Notice.PLAYER)
				queue.add(Notice.stageFoe(npcIndex, line.text(), line.tone()));
			else if (line.overPlayer() || npcIndex == Notice.PLAYER)
				queue.add(Notice.player(line.text(), line.tone()));
			else
				queue.add(Notice.creature(npcIndex, line.text(), line.tone()));
		}

		queue.addAll(after);
		noticeBoard.enqueue(queue, staged);

		if (!config.chatMessages())
			return;

		for (Notice notice : after)
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", notice.text(), null);
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
			public void selectJar(JarType type)
			{
				onGameThread(() -> game.selectJar(type));
			}

			@Override
			public void challenge(String championId)
			{
				onGameThread(() -> GielinorGoPlugin.this.challenge(championId));
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
