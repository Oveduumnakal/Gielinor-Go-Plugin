/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Battles between friends in the same RuneLite party. Both players send their team and half of a random seed, and
 * each computer then plays out the same battle on its own, so the two always agree on who won. Friend battles are
 * for fun: every creature fights at full health and nothing is won or lost.
 *
 * <p>Any party member can send any message, so nothing in a message is trusted: teams are checked against the
 * species list, and an answer only counts when it matches a challenge this player really sent. It has no link to
 * the game client, so two of these can be tested against each other.
 */
final class FriendBattles
{
	/** How long a challenge waits for its answer, in milliseconds. */
	static final long CHALLENGE_TIMEOUT_MS = 30_000L;

	/** Shortest time between two battles a player takes from friends, in milliseconds, so nobody can be flooded. */
	static final long REST_MS = 10_000L;

	/** Most party members remembered as players. */
	static final int MAX_FRIENDS = 50;

	/** A party member who said they play, and the team and eggs they last showed. */
	private static final class Friend
	{
		private final int trainerLevel;
		private final String[] species;
		private final int[] levels;
		private final List<StatRolls> rolls;
		private final String[][] moves;
		private final List<Rarity> eggs;

		private Friend(int trainerLevel, String[] species, int[] levels, List<StatRolls> rolls, String[][] moves,
			List<Rarity> eggs)
		{
			this.trainerLevel = trainerLevel;
			this.species = species;
			this.levels = levels;
			this.rolls = rolls;
			this.moves = moves;
			this.eggs = eggs;
		}

		private boolean sameAs(Friend other)
		{
			return trainerLevel == other.trainerLevel && Arrays.equals(species, other.species)
				&& Arrays.equals(levels, other.levels) && rolls.equals(other.rolls)
				&& Arrays.deepEquals(moves, other.moves) && eggs.equals(other.eggs);
		}
	}

	/** A challenge this player sent that has not been answered yet. */
	private static final class Pending
	{
		private final long friendId;
		private final long battle;
		private final long seed;
		private final String[] species;
		private final int[] levels;
		private final int[][] rolls;
		private final String[] temperaments;
		private final String[][] moves;
		private final boolean steered;
		private final long sentMs;

		private Pending(long friendId, GielinorGoPartyMessage sent, long sentMs)
		{
			this.friendId = friendId;
			this.battle = sent.battle();
			this.seed = sent.seed();
			this.species = sent.species();
			this.levels = sent.levels();
			this.rolls = sent.rolls();
			this.temperaments = sent.temperaments();
			this.moves = sent.moves();
			this.steered = sent.steered();
			this.sentMs = sentMs;
		}
	}

	private final SpeciesTable species;
	private MoveTable moveTable = MoveTable.empty();
	private final Random random;
	private final LongSupplier clock;
	private final Consumer<GielinorGoPartyMessage> outbox;
	private final Consumer<Notice> notices;
	private final Map<Long, Friend> friends = new LinkedHashMap<>();
	private Pending pending;
	private Pending cancelled;
	private FriendTurnBattle turnBattle;
	private FriendTurnBattle started;
	private boolean helloWanted;
	private boolean changed;
	private long lastBattleMs = -REST_MS;

	/**
	 * Creates the service with no friends known.
	 *
	 * @param species every catchable species
	 * @param random the source of battle numbers and seed halves
	 * @param clock wall-clock time in milliseconds
	 * @param outbox sends a message to the party
	 * @param notices receives each line the player should be told
	 */
	FriendBattles(SpeciesTable species, Random random, LongSupplier clock, Consumer<GielinorGoPartyMessage> outbox,
		Consumer<Notice> notices)
	{
		this.species = species;
		this.random = random;
		this.clock = clock;
		this.outbox = outbox;
		this.notices = notices;
	}

	/**
	 * Sets the moves creatures can learn, which the moves a friend sends are checked against. Without this call no
	 * move is known, so a creature fights with its basic attack and a message that names moves is turned away.
	 *
	 * @param table the moves
	 * @return this service
	 */
	FriendBattles withMoves(MoveTable table)
	{
		this.moveTable = table == null ? MoveTable.empty() : table;
		return this;
	}

	/**
	 * Tells the party that this player plays, and shows them the team.
	 *
	 * @param data the player's save
	 * @param askBack whether everyone who plays should say hello back, for a player who knows nobody yet
	 */
	void hello(SaveData data, boolean askBack)
	{
		GielinorGoPartyMessage hello = message(GielinorGoPartyMessage.HELLO, 0, 0, 0, data);
		outbox.accept(askBack ? hello.askingBack() : hello);
		helloWanted = false;
	}

	/**
	 * Reports whether someone in the party is waiting to hear that this player plays. The caller sends the hello,
	 * so a burst of people joining is answered once instead of once each.
	 *
	 * @return true when a hello is owed; calling this clears the flag
	 */
	boolean takeHelloWanted()
	{
		boolean wanted = helloWanted;
		helloWanted = false;
		return wanted;
	}

	/**
	 * Reports whether messages received since the last call changed anything the panel shows: a friend's team, a
	 * battle, or the state of a challenge. A message that told nothing new does not count, so a spammer cannot make
	 * the panel redraw.
	 *
	 * @return true when something changed; calling this clears the flag
	 */
	boolean takeChanged()
	{
		boolean was = changed;
		changed = false;
		return was;
	}

	/**
	 * Gives up on a challenge nobody answered. Called once per game tick.
	 *
	 * @return true when a challenge just ran out of time, so the panel needs a refresh
	 */
	boolean expire()
	{
		if (pending == null || waiting(clock.getAsLong()))
			return false;

		pending = null;
		tell("Your challenge got no answer", Tone.BAD);
		return true;
	}

	/**
	 * Cancels the challenge this player sent, because another battle is starting. A battle already on is left
	 * alone. If the friend accepts the cancelled challenge anyway, they are sent a
	 * {@link GielinorGoPartyMessage#LEAVE} instead of a battle.
	 */
	void cancelChallenge()
	{
		if (pending == null)
			return;

		cancelled = pending;
		pending = null;
		changed = true;
	}

	/**
	 * Tells whether an answer belongs to a challenge that was cancelled and is still within its time.
	 *
	 * @param friendId the party member id of the sender
	 * @param battle the battle id the message names
	 * @return true when it does
	 */
	private boolean cancelledBy(long friendId, long battle)
	{
		return cancelled != null && cancelled.friendId == friendId && cancelled.battle == battle
			&& clock.getAsLong() - cancelled.sentMs <= CHALLENGE_TIMEOUT_MS;
	}

	/**
	 * Challenges a friend with the whole team. The battle happens when their answer arrives.
	 *
	 * @param friendId the friend's party member id
	 * @param friendName the friend's name
	 * @param data the player's save
	 */
	void challenge(long friendId, String friendName, SaveData data)
	{
		challenge(friendId, friendName, data, false);
	}

	/**
	 * Challenges a friend with the whole team, saying whether this player wants to choose the turns. The battle is
	 * played turn by turn only when the friend wants that too.
	 *
	 * @param friendId the friend's party member id
	 * @param friendName the friend's name
	 * @param data the player's save
	 * @param steering whether this player has Choose battle actions on
	 */
	void challenge(long friendId, String friendName, SaveData data, boolean steering)
	{
		if (busy())
		{
			tell("Finish your battle first", Tone.BAD);
			return;
		}

		Friend friend = friends.get(friendId);
		if (friend == null || friend.species.length == 0)
		{
			tell(friendName + " has no team to battle", Tone.BAD);
			return;
		}

		if (data.team.isEmpty())
		{
			tell("Catch a creature first", Tone.BAD);
			return;
		}

		long now = clock.getAsLong();
		if (waiting(now))
		{
			tell("Still waiting for an answer", Tone.BAD);
			return;
		}

		GielinorGoPartyMessage sent = message(GielinorGoPartyMessage.CHALLENGE, friendId, random.nextLong(),
			random.nextLong(), data).steering(steering);
		pending = new Pending(friendId, sent, now);
		outbox.accept(sent);
		tell("Challenged " + friendName + "!", Tone.INFO);
	}

	/**
	 * Handles a message from another party member.
	 *
	 * @param message the message; nothing in it is trusted
	 * @param myId this player's party member id
	 * @param senderName the sender's name, as the party service knows it
	 * @param data the player's save
	 * @param accepting whether the player takes battles from friends
	 * @return the battle the message led to, seen from this player's side, or null when it led to none
	 */
	TeamBattleResult receive(GielinorGoPartyMessage message, long myId, String senderName, SaveData data,
		boolean accepting)
	{
		return receive(message, myId, senderName, data, accepting, false);
	}

	/**
	 * Handles a message from another party member, for a player who may want to choose the turns of a battle. A
	 * battle that both players want to choose is not played here: it is handed over by {@link #takeStarted()}, and
	 * this returns null for it.
	 *
	 * @param message the message; nothing in it is trusted
	 * @param myId this player's party member id
	 * @param senderName the sender's name, as the party service knows it
	 * @param data the player's save
	 * @param accepting whether the player takes battles from friends
	 * @param steering whether the player has Choose battle actions on
	 * @return the battle the message led to, seen from this player's side, or null when it led to none or to a battle
	 *     that is played turn by turn
	 */
	TeamBattleResult receive(GielinorGoPartyMessage message, long myId, String senderName, SaveData data,
		boolean accepting, boolean steering)
	{
		return receive(message, myId, senderName, data, accepting, steering, true);
	}

	/**
	 * Handles a message from another party member, for a player who may be busy with a battle of their own. A friend
	 * who accepts a challenge to a battle both players choose the turns of, while the player is busy, starts no
	 * battle: the player is told and the friend is sent a {@link GielinorGoPartyMessage#LEAVE} so their side ends
	 * too.
	 *
	 * @param message the message; nothing in it is trusted
	 * @param myId this player's party member id
	 * @param senderName the sender's name, as the party service knows it
	 * @param data the player's save
	 * @param accepting whether the player takes battles from friends
	 * @param steering whether the player has Choose battle actions on
	 * @param free whether the player can start a battle now; false while another battle is on
	 * @return the battle the message led to, seen from this player's side, or null when it led to none or to a battle
	 *     that is played turn by turn
	 */
	TeamBattleResult receive(GielinorGoPartyMessage message, long myId, String senderName, SaveData data,
		boolean accepting, boolean steering, boolean free)
	{
		long sender = message.getMemberId();
		String kind = message.kind();
		if (kind == null || sender == myId)
			return null;

		if (kind.equals(GielinorGoPartyMessage.HELLO))
		{
			meet(message);
			return null;
		}

		if (message.to() != myId)
			return null;

		if (kind.equals(GielinorGoPartyMessage.TURN) || kind.equals(GielinorGoPartyMessage.LEAVE)
			|| kind.equals(GielinorGoPartyMessage.READY))
		{
			steer(message);
			return null;
		}

		boolean fights = kind.equals(GielinorGoPartyMessage.CHALLENGE) || kind.equals(GielinorGoPartyMessage.ACCEPT);
		if (fights && message.version() != GielinorGoPartyMessage.PROTOCOL)
		{
			tell(senderName + " runs a different version of Gielinor Go", Tone.BAD);
			return null;
		}

		if (kind.equals(GielinorGoPartyMessage.CHALLENGE))
			return challenged(message, senderName, data, accepting, steering);

		if (kind.equals(GielinorGoPartyMessage.ACCEPT) && cancelledBy(sender, message.battle()))
		{
			cancelled = null;
			outbox.accept(GielinorGoPartyMessage.leave(sender, message.battle()));
			return null;
		}

		if (pending == null || pending.friendId != sender || pending.battle != message.battle()
			|| !waiting(clock.getAsLong()))
			return null;

		Pending answered = pending;
		pending = null;
		changed = true;
		if (kind.equals(GielinorGoPartyMessage.DECLINE))
		{
			tell(senderName + " is not battling right now", Tone.BAD);
			return null;
		}

		List<Combatant> mine = fighters(answered.species, answered.levels, answered.rolls, answered.temperaments,
			answered.moves);
		List<Combatant> theirs = fighters(message);
		if (!kind.equals(GielinorGoPartyMessage.ACCEPT) || mine == null || theirs == null || theirs.isEmpty())
			return null;

		remember(message);
		if (!free && answered.steered && message.steered())
		{
			tell(senderName + " accepted, but you are in a battle", Tone.BAD);
			outbox.accept(GielinorGoPartyMessage.leave(sender, answered.battle));
			return null;
		}

		lastBattleMs = clock.getAsLong();
		long seed = answered.seed ^ message.seed();
		if (answered.steered && message.steered())
		{
			begin(new FriendTurnBattle(answered.battle, sender, senderName, true, mine, theirs, seed, outbox));
			return null;
		}

		boolean leadFirst = leadIsFirst(mine, data);
		return fight(mine, theirs, seed, true, senderName).withLeadFirst(leadFirst);
	}

	/**
	 * Forgets a friend who left the party.
	 *
	 * @param memberId the friend's party member id
	 */
	void forget(long memberId)
	{
		friends.remove(memberId);
		if (pending != null && pending.friendId == memberId)
			pending = null;

		if (turnBattle != null && turnBattle.friendId() == memberId)
			turnBattle.end(FriendTurnBattle.End.LEFT);
	}

	/** Forgets every friend, for when the player joins or leaves a party. */
	void clear()
	{
		friends.clear();
		pending = null;
		if (turnBattle != null)
			turnBattle.end(FriendTurnBattle.End.LEFT);
	}

	/**
	 * Hands over the battle that both players want to choose the turns of, once, as soon as it starts.
	 *
	 * @return the battle, or null when none started since the last call
	 */
	FriendTurnBattle takeStarted()
	{
		FriendTurnBattle taken = started;
		started = null;
		return taken;
	}

	/**
	 * Moves the clocks of the battle played turn by turn on. Called once per game tick.
	 *
	 * @return the battle that is on, or null when none is
	 */
	FriendTurnBattle tickTurnBattle()
	{
		if (turnBattle == null || turnBattle.over())
			return null;

		turnBattle.timeout(clock.getAsLong());
		return turnBattle;
	}

	/**
	 * Tells whether a battle played turn by turn is on against the given player, so its messages are not held to the
	 * rate limit of ordinary ones. The battle itself refuses a turn that is not due.
	 *
	 * @param memberId the other player's party member id
	 * @return true while such a battle is on
	 */
	boolean inTurnBattleWith(long memberId)
	{
		return busy() && turnBattle.friendId() == memberId;
	}

	/**
	 * Tells whether a battle played turn by turn is still on, so no other battle starts beside it.
	 *
	 * @return true while it is
	 */
	private boolean busy()
	{
		return turnBattle != null && !turnBattle.over();
	}

	private void begin(FriendTurnBattle battle)
	{
		turnBattle = battle;
		started = battle;
		changed = true;
	}

	/**
	 * Passes a message about the battle played turn by turn to that battle. A message from a different version, or
	 * one for no battle that is on, is dropped, except that a different version ends the battle.
	 *
	 * @param message a message of kind TURN, LEAVE or READY addressed to this player
	 */
	private void steer(GielinorGoPartyMessage message)
	{
		if (turnBattle == null || turnBattle.over() || turnBattle.friendId() != message.getMemberId())
			return;

		if (message.version() != GielinorGoPartyMessage.PROTOCOL)
		{
			turnBattle.end(FriendTurnBattle.End.OUT_OF_STEP);
			return;
		}

		turnBattle.remote(message, clock.getAsLong());
	}

	/**
	 * Lists the friends the panel shows.
	 *
	 * @param names the name of every member now in the party, by member id; a friend not in it is left out
	 * @return one view per friend, in the order they were met
	 */
	List<FriendView> views(Map<Long, String> names)
	{
		boolean waiting = waiting(clock.getAsLong());
		List<FriendView> views = new ArrayList<>();
		for (Map.Entry<Long, Friend> entry : friends.entrySet())
		{
			String name = names.get(entry.getKey());
			if (name == null)
				continue;

			Friend friend = entry.getValue();
			List<String> team = new ArrayList<>();
			for (int i = 0; i < friend.species.length; i++)
			{
				String rating = ratingNote(friend.rolls.get(i));
				team.add(species.find(friend.species[i]).name() + " " + friend.levels[i] + rating);
			}

			List<String> tradeable = new ArrayList<>();
			for (GielinorGoPartyMessage.Item item : tradeable(entry.getKey()))
				tradeable.add(FriendTrades.label(item, species));

			views.add(new FriendView(entry.getKey(), name, friend.trainerLevel, String.join(", ", team),
				friend.species.length > 0, waiting && pending.friendId == entry.getKey(), tradeable));
		}

		return views;
	}

	/**
	 * Lists what a friend has shown that can be asked for in a trade: the creatures on their team, then their
	 * eggs. The panel shows the same list in the same order, and passes back a place in it.
	 *
	 * @param friendId the friend's party member id
	 * @return the creatures and eggs, empty for someone unknown
	 */
	List<GielinorGoPartyMessage.Item> tradeable(long friendId)
	{
		List<GielinorGoPartyMessage.Item> items = new ArrayList<>();
		Friend friend = friends.get(friendId);
		if (friend == null)
			return items;

		for (int i = 0; i < friend.species.length; i++)
			items.add(GielinorGoPartyMessage.Item.creature(friend.species[i], friend.levels[i], 0, false,
				friend.rolls.get(i), friend.moves[i]));

		for (Rarity egg : friend.eggs)
			items.add(GielinorGoPartyMessage.Item.egg(egg.name(), 0));

		return items;
	}

	/**
	 * Takes up or turns down a challenge. A challenge taken up is fought here at once; the challenger fights the
	 * same battle when the answer reaches them.
	 *
	 * @param message the challenge
	 * @param senderName the challenger's name
	 * @param data the player's save
	 * @param accepting whether the player takes battles from friends
	 * @return the battle seen from this player's side, or null when the challenge was turned down or made no sense
	 */
	private TeamBattleResult challenged(GielinorGoPartyMessage message, String senderName, SaveData data,
		boolean accepting, boolean steering)
	{
		List<Combatant> theirs = fighters(message);
		if (theirs == null || theirs.isEmpty())
			return null;

		long now = clock.getAsLong();
		GielinorGoPartyMessage answer = message(GielinorGoPartyMessage.ACCEPT, message.getMemberId(),
			message.battle(), random.nextLong(), data).steering(steering);
		List<Combatant> mine = fighters(answer);
		if (!accepting || busy() || mine == null || mine.isEmpty() || now - lastBattleMs < REST_MS)
		{
			outbox.accept(new GielinorGoPartyMessage(GielinorGoPartyMessage.DECLINE, message.getMemberId(),
				message.battle(), 0, 0, new String[0], new int[0]));
			return null;
		}

		remember(message);
		lastBattleMs = now;
		outbox.accept(answer);
		long seed = message.seed() ^ answer.seed();
		if (steering && message.steered())
		{
			begin(new FriendTurnBattle(message.battle(), message.getMemberId(), senderName, false, mine, theirs, seed,
				outbox));
			return null;
		}

		boolean leadFirst = leadIsFirst(mine, data);
		return fight(theirs, mine, seed, false, senderName).withLeadFirst(leadFirst);
	}

	/**
	 * Tells whether the first creature of a team is the player's lead, the one that walks beside the player.
	 *
	 * @param mine the player's fighters, in order
	 * @param data the player's save
	 * @return true when the first fighter is of the lead's species
	 */
	private static boolean leadIsFirst(List<Combatant> mine, SaveData data)
	{
		Creature lead = data.lead();
		return lead != null && !mine.isEmpty() && mine.get(0).speciesKey().equals(Species.keyFor(lead.species));
	}

	/**
	 * Notes a party member who says they play. Someone met for the first time, or who asks, is owed a hello in
	 * return, so both sides end up knowing each other whoever joined or logged in first.
	 *
	 * @param message their hello
	 */
	private void meet(GielinorGoPartyMessage message)
	{
		boolean known = friends.containsKey(message.getMemberId());
		if (remember(message) && (!known || message.askBack()))
			helloWanted = true;
	}

	/**
	 * Keeps the team a party member showed.
	 *
	 * @param message any message that carries the sender's team
	 * @return true when the team made sense and was kept
	 */
	private boolean remember(GielinorGoPartyMessage message)
	{
		long sender = message.getMemberId();
		if (fighters(message) == null)
			return false;

		if (!friends.containsKey(sender) && friends.size() >= MAX_FRIENDS)
			return false;

		int trainer = Math.max(1, Math.min(XpRules.MAX_LEVEL, message.trainer()));
		Friend kept = new Friend(trainer, message.species().clone(), message.levels().clone(),
			rollsFor(message.species().length, message.rolls(), message.temperaments()), movesKept(message),
			eggsOf(message.eggs()));
		Friend before = friends.put(sender, kept);
		changed |= before == null || !before.sameAs(kept);
		return true;
	}

	/**
	 * Reads the eggs a party member says they carry.
	 *
	 * @param rarities rarity names, as sent
	 * @return the rarities that are real, at most as many as a player can carry
	 */
	private static List<Rarity> eggsOf(String[] rarities)
	{
		List<Rarity> eggs = new ArrayList<>();
		for (int i = 0; rarities != null && i < rarities.length && eggs.size() < SaveData.MAX_EGGS; i++)
		{
			Rarity rarity = FriendTrades.rarityNamed(rarities[i]);
			if (rarity != null)
				eggs.add(rarity);
		}

		return eggs;
	}

	/**
	 * Plays out a battle between two teams. Both players call this with the same teams in the same order and the
	 * same seed, so both get the same battle; only the wording differs by side.
	 *
	 * @param first the challenger's team, at full health
	 * @param second the team of the player who was challenged, at full health
	 * @param seed both seed halves combined
	 * @param iAmFirst whether this player is the challenger
	 * @param friendName the other player's name
	 * @return the battle seen from this player's side
	 */
	private static TeamBattleResult fight(List<Combatant> first, List<Combatant> second, long seed, boolean iAmFirst,
		String friendName)
	{
		Random dice = new Random(seed);
		List<BattleLine> lines = new ArrayList<>();
		List<Duel> duels = new ArrayList<>();
		lines.add(new BattleLine("Battle with " + friendName + "!", Tone.INFO, true));
		int firstIndex = 0;
		int secondIndex = 0;
		while (firstIndex < first.size() && secondIndex < second.size())
		{
			Combatant one = first.get(firstIndex);
			Combatant two = second.get(secondIndex);
			List<BattleLine> hits = new ArrayList<>();
			boolean firstWon = BattleSim.duelWon(one, two, dice, hits, iAmFirst);
			int oneIndex = firstIndex;
			int twoIndex = secondIndex;
			if (firstWon)
			{
				two.knockOut();
				secondIndex++;
			}
			else
			{
				one.knockOut();
				firstIndex++;
			}

			Combatant mine = iAmFirst ? one : two;
			Combatant theirs = iAmFirst ? two : one;
			boolean iWon = firstWon == iAmFirst;
			BattleLine out = new BattleLine("Your " + mine.name() + (iWon ? " beat " : " lost to ") + friendName
				+ "'s " + theirs.name() + " (Lv " + theirs.level() + ")", iWon ? Tone.GOOD : Tone.BAD, true);
			lines.add(out);
			duels.add(new Duel(mine, iAmFirst ? oneIndex : twoIndex, theirs, iAmFirst ? twoIndex : oneIndex, hits, out,
				iWon));
		}

		boolean won = (secondIndex >= second.size()) == iAmFirst;
		if (won)
			lines.add(new BattleLine("You beat " + friendName + "!", Tone.GOOD, true));
		else
			lines.add(new BattleLine(friendName + " won this one", Tone.BAD, true));

		return new TeamBattleResult(won, lines, duels);
	}

	/**
	 * Turns the team a message carries into fighters at full health.
	 *
	 * @param message the message
	 * @return the fighters, or null when the team makes no sense, see
	 *     {@link #fighters(String[], int[], int[][], String[], String[][])}
	 */
	private List<Combatant> fighters(GielinorGoPartyMessage message)
	{
		return fighters(message.species(), message.levels(), message.rolls(), message.temperaments(),
			message.moves());
	}

	/**
	 * Turns a team from a message into fighters at full health.
	 *
	 * @param keys species keys, as sent
	 * @param levels creature levels, as sent
	 * @param rolls four rolls per creature, as sent; null means every creature is average
	 * @param temperaments a temperament name per creature, as sent; null means every creature is steady
	 * @param moves the move ids of each creature, as sent; null, or an empty list for a creature, means the moves its
	 *     level unlocked
	 * @return the fighters, empty for an empty team, or null when the team makes no sense: too many creatures, an
	 *     unknown species, a level outside 1 to 99, rolls out of range or of the wrong shape, an unknown
	 *     temperament, or moves that are too many, repeated, unknown to the species or not yet unlocked at its level
	 */
	private List<Combatant> fighters(String[] keys, int[] levels, int[][] rolls, String[] temperaments,
		String[][] moves)
	{
		if (keys == null || levels == null || keys.length != levels.length || keys.length > SaveData.TEAM_SIZE
			|| (moves != null && moves.length != keys.length))
			return null;

		List<StatRolls> made = rollsFor(keys.length, rolls, temperaments);
		if (made == null)
			return null;

		List<Combatant> fighters = new ArrayList<>();
		for (int i = 0; i < keys.length; i++)
		{
			Species found = keys[i] == null ? null : species.find(keys[i]);
			if (found == null || levels[i] < 1 || levels[i] > XpRules.MAX_LEVEL)
				return null;

			List<Move> known = movesFor(found, levels[i], moves == null ? null : moves[i]);
			if (known == null)
				return null;

			fighters.add(new Combatant(found, levels[i], 1.0, made.get(i)).moves(known));
		}

		return fighters;
	}

	/**
	 * Checks the moves a message names for one creature.
	 *
	 * @param found the creature's species
	 * @param level the creature's level
	 * @param ids the move ids as sent, or null
	 * @return the moves; what the level unlocks when none are named; null when more than four are named, one is
	 *     missing or repeated, or one is not in the species' pool at this level
	 */
	private List<Move> movesFor(Species found, int level, String[] ids)
	{
		if (ids == null || ids.length == 0)
			return moveTable.movesAt(found, level);

		if (ids.length > MoveTable.MAX_KNOWN)
			return null;

		List<Move> known = new ArrayList<>();
		for (String id : ids)
		{
			if (id == null || !moveTable.knows(found, level, id) || known.contains(moveTable.find(id)))
				return null;

			known.add(moveTable.find(id));
		}

		return known;
	}

	/**
	 * Keeps the moves a message carries, as one array of ids for each creature.
	 *
	 * @param message a message whose team already made sense
	 * @return the ids per creature, empty for a creature that sent none
	 */
	private static String[][] movesKept(GielinorGoPartyMessage message)
	{
		String[][] kept = new String[message.species().length][];
		String[][] sent = message.moves();
		for (int i = 0; i < kept.length; i++)
			kept[i] = sent == null || sent[i] == null ? new String[0] : sent[i].clone();

		return kept;
	}

	/**
	 * Reads the rolls a message carries for a team.
	 *
	 * @param size how many creatures the team has
	 * @param rolls four rolls per creature, as sent, or null for all average
	 * @param temperaments a temperament name per creature, as sent, or null for all steady
	 * @return one set of rolls per creature, or null when an array is the wrong length, a roll is out of range or
	 *     a temperament is unknown
	 */
	private static List<StatRolls> rollsFor(int size, int[][] rolls, String[] temperaments)
	{
		if ((rolls != null && rolls.length != size) || (temperaments != null && temperaments.length != size))
			return null;

		List<StatRolls> made = new ArrayList<>(size);
		for (int i = 0; i < size; i++)
		{
			if (rolls != null && rolls[i] == null)
				return null;

			int[] values = rolls == null ? null : rolls[i];
			StatRolls one = StatRolls.of(values, temperaments == null ? null : temperaments[i]);
			if (one == null)
				return null;

			made.add(one);
		}

		return made;
	}

	/**
	 * Words a creature's rating for the end of a team line.
	 *
	 * @param rolls the creature's rolls
	 * @return a space and the rating in brackets, or an empty string for an average creature
	 */
	static String ratingNote(StatRolls rolls)
	{
		StatRolls.Rating rating = rolls.rating();
		return rating == StatRolls.Rating.AVERAGE ? "" : " (" + rating.label() + ")";
	}

	/**
	 * Makes a message that carries this player's team. Creatures of a species this version does not know are
	 * left out, so the other side never turns the whole team away.
	 *
	 * @param kind what the message is for
	 * @param to the party member it is for, or 0 for everyone
	 * @param battle the battle's number, or 0
	 * @param seed this player's half of the seed, or 0
	 * @param data the player's save
	 * @return the message
	 */
	private GielinorGoPartyMessage message(String kind, long to, long battle, long seed, SaveData data)
	{
		List<Creature> known = new ArrayList<>();
		for (Creature creature : data.team)
		{
			if (species.find(creature.species) != null)
				known.add(creature);
		}

		String[] keys = new String[known.size()];
		int[] levels = new int[known.size()];
		int[][] rolls = new int[known.size()][];
		String[] temperaments = new String[known.size()];
		String[][] moves = new String[known.size()][];
		for (int i = 0; i < keys.length; i++)
		{
			keys[i] = known.get(i).species;
			levels[i] = known.get(i).level();
			moves[i] = MoveLearning.sendable(known.get(i), species.find(known.get(i).species), moveTable);
			StatRolls own = known.get(i).rolls();
			rolls[i] = own.values();
			temperaments[i] = own.temperament() == Temperament.STEADY ? null : own.temperament().name();
		}

		String[] eggs = new String[data.eggs.size()];
		for (int i = 0; i < eggs.length; i++)
		{
			Rarity rarity = data.eggs.get(i).rarityTier();
			eggs[i] = rarity.name();
		}

		GielinorGoPartyMessage message = new GielinorGoPartyMessage(kind, to, battle, seed,
			XpRules.trainerLevelForXp(data.trainerXp), keys, levels);
		return message.rolled(rolls, temperaments)
			.moved(moves)
			.carrying(eggs);
	}

	private boolean waiting(long now)
	{
		return pending != null && now - pending.sentMs <= CHALLENGE_TIMEOUT_MS;
	}

	private void tell(String text, Tone tone)
	{
		notices.accept(Notice.player(text, tone));
	}
}
