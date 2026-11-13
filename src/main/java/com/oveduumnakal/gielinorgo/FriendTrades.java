/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import com.oveduumnakal.gielinorgo.GielinorGoPartyMessage.Item;

/**
 * Trades between friends in the same RuneLite party: one creature or egg for one creature or egg, or a gift. Jars
 * cannot be traded. A trade changes both players' saves, so unlike a battle it never happens without a click: one
 * player offers, the other accepts, and only then does each side make the swap.
 *
 * <p>The steps are: an offer; the friend agrees and names what they give, changing nothing yet; the player who
 * offered checks both halves, makes the swap and confirms; the friend makes the swap. Every step checks again that
 * the thing is still owned and that there is room for what comes back; before anyone has swapped, a creature that
 * gained XP or evolved also calls the trade off. A creature or egg promised in one trade cannot be promised in
 * another, cannot be released and does not hatch until the trade ends, and the room an agreed trade needs is held
 * back from catches, egg drops and other trades. Any party member can send any message, so nothing in a message is
 * trusted.
 *
 * <p>There is no server to referee, so a trade can complete on one side only: if a player leaves the party or logs
 * out between the two swaps, or the friend's confirmation is lost, one thing ends up with both players and the other
 * is lost. Two players who edit their clients could also both keep a creature. The game does not try to stop
 * either.
 */
final class FriendTrades implements GoGame.TradeLocks
{
	/** How long an offer waits for the friend to accept it, in milliseconds. */
	static final long OFFER_TIMEOUT_MS = 120_000L;

	/** How long an accepted trade waits for the other side to confirm it, in milliseconds. */
	static final long CONFIRM_TIMEOUT_MS = 30_000L;

	/** Most offers from different friends kept at once. */
	static final int MAX_OFFERS = 50;

	/** Shortest time between two "offers a trade" notices from the same friend, in milliseconds. */
	static final long NOTICE_GAP_MS = 5_000L;

	/** One thing the player owns: a creature or an egg, never both. */
	private static final class Mine
	{
		private final Creature creature;
		private final Egg egg;

		private Mine(Creature creature, Egg egg)
		{
			this.creature = creature;
			this.egg = egg;
		}

		private boolean is(Mine other)
		{
			return creature != null ? creature == other.creature : egg != null && egg == other.egg;
		}
	}

	/** An offer the player made that the friend has not answered yet. */
	private static final class Outgoing
	{
		private final long friendId;
		private final long id;
		private final Mine give;
		private final Item offered;
		private final Item want;
		private final long sentMs;

		private Outgoing(long friendId, long id, Mine give, Item offered, Item want, long sentMs)
		{
			this.friendId = friendId;
			this.id = id;
			this.give = give;
			this.offered = offered;
			this.want = want;
			this.sentMs = sentMs;
		}
	}

	/** An offer a friend made to the player. */
	private static final class Incoming
	{
		private final long id;
		private final Item theirs;
		private final Mine asked;
		private final long receivedMs;
		private long agreedMs;
		private Item askedAtAgreement;

		private Incoming(long id, Item theirs, Mine asked, long receivedMs)
		{
			this.id = id;
			this.theirs = theirs;
			this.asked = asked;
			this.receivedMs = receivedMs;
		}
	}

	private final SpeciesTable species;
	private final GoGame game;
	private final Random random;
	private final LongSupplier clock;
	private final Consumer<GielinorGoPartyMessage> outbox;
	private final Consumer<Notice> notices;
	private final Map<Long, Incoming> incoming = new HashMap<>();
	private final Map<Long, Long> noticedMs = new HashMap<>();
	private Outgoing outgoing;
	private boolean traded;

	/**
	 * Creates the service with no trades under way.
	 *
	 * @param species every catchable species
	 * @param game the player's game, whose save a trade changes
	 * @param random the source of trade numbers
	 * @param clock wall-clock time in milliseconds
	 * @param outbox sends a message to the party
	 * @param notices receives each line the player should be told
	 */
	FriendTrades(SpeciesTable species, GoGame game, Random random, LongSupplier clock,
		Consumer<GielinorGoPartyMessage> outbox, Consumer<Notice> notices)
	{
		this.species = species;
		this.game = game;
		this.random = random;
		this.clock = clock;
		this.outbox = outbox;
		this.notices = notices;
	}

	/**
	 * Offers a friend one of the player's creatures.
	 *
	 * @param friendId the friend's party member id
	 * @param friendName the friend's name
	 * @param creature the creature to give
	 * @param want what to ask for in return, or null to give it away
	 */
	void offer(long friendId, String friendName, Creature creature, Item want)
	{
		offer(friendId, friendName, new Mine(creature, null), want);
	}

	/**
	 * Offers a friend one of the player's eggs.
	 *
	 * @param friendId the friend's party member id
	 * @param friendName the friend's name
	 * @param egg the egg to give
	 * @param want what to ask for in return, or null to give it away
	 */
	void offer(long friendId, String friendName, Egg egg, Item want)
	{
		offer(friendId, friendName, new Mine(null, egg), want);
	}

	/**
	 * Answers the offer a friend made, from the buttons on their card.
	 *
	 * @param friendId the friend's party member id
	 * @param friendName the friend's name
	 * @param accept true to take the trade, false to turn it down
	 */
	void answer(long friendId, String friendName, boolean accept)
	{
		Incoming offer = incoming.get(friendId);
		long now = clock.getAsLong();
		if (offer == null || offer.agreedMs != 0 || now - offer.receivedMs > OFFER_TIMEOUT_MS)
			return;

		if (!accept)
		{
			incoming.remove(friendId);
			send(GielinorGoPartyMessage.REFUSE, friendId, offer.id);
			return;
		}

		if (offer.asked != null && !owns(offer.asked))
		{
			incoming.remove(friendId);
			send(GielinorGoPartyMessage.REFUSE, friendId, offer.id);
			tell("You no longer have what " + friendName + " asked for", Tone.BAD);
			return;
		}

		if (offer.asked != null && promised(offer.asked, offer))
		{
			tell("That is already promised in another trade", Tone.BAD);
			return;
		}

		if (!roomFor(offer.theirs, offer.asked))
		{
			tell(noRoom(offer.theirs), Tone.BAD);
			return;
		}

		offer.agreedMs = now;
		offer.askedAtAgreement = offer.asked == null ? null : describe(offer.asked);
		GielinorGoPartyMessage agree = bare(GielinorGoPartyMessage.AGREE, friendId, offer.id);
		outbox.accept(agree.giving(offer.askedAtAgreement));
	}

	/**
	 * Handles a trade message from another party member.
	 *
	 * @param message the message; nothing in it is trusted
	 * @param myId this player's party member id
	 * @param senderName the sender's name, as the party service knows it
	 * @return true when the message changed where a trade stands, so the panel needs a refresh
	 */
	boolean receive(GielinorGoPartyMessage message, long myId, String senderName)
	{
		String kind = message.kind();
		if (kind == null || message.getMemberId() == myId || message.to() != myId)
			return false;

		if (kind.equals(GielinorGoPartyMessage.OFFER) && message.version() != GielinorGoPartyMessage.PROTOCOL)
		{
			tell(senderName + " runs a different version of Gielinor Go", Tone.BAD);
			return false;
		}

		if (kind.equals(GielinorGoPartyMessage.OFFER))
			return offered(message, senderName);

		if (kind.equals(GielinorGoPartyMessage.AGREE))
			return agreed(message, senderName);

		if (kind.equals(GielinorGoPartyMessage.CONFIRM))
			return confirmed(message, senderName);

		return kind.equals(GielinorGoPartyMessage.REFUSE) && refused(message, senderName);
	}

	/**
	 * Gives up on trades that waited too long. Called once per game tick.
	 *
	 * @return true when an offer just ran out of time, so the panel needs a refresh
	 */
	boolean expire()
	{
		long now = clock.getAsLong();
		boolean changed = false;
		if (outgoing != null && now - outgoing.sentMs > OFFER_TIMEOUT_MS)
		{
			outgoing = null;
			changed = true;
			tell("Your trade offer got no answer", Tone.BAD);
		}

		Iterator<Incoming> offers = incoming.values().iterator();
		while (offers.hasNext())
		{
			Incoming offer = offers.next();
			if (stale(offer, now))
			{
				offers.remove();
				changed = true;
			}
		}

		return changed;
	}

	/**
	 * Reports whether a trade went through since the last call, so the party can be shown the new team.
	 *
	 * @return true when a trade was made; calling this clears the flag
	 */
	boolean takeTraded()
	{
		boolean was = traded;
		traded = false;
		return was;
	}

	/**
	 * Drops every trade with a friend who left the party.
	 *
	 * @param memberId the friend's party member id
	 */
	void forget(long memberId)
	{
		incoming.remove(memberId);
		noticedMs.remove(memberId);
		if (outgoing != null && outgoing.friendId == memberId)
			outgoing = null;
	}

	@Override
	public boolean locked(Creature creature)
	{
		return promised(new Mine(creature, null), null);
	}

	@Override
	public boolean locked(Egg egg)
	{
		return promised(new Mine(null, egg), null);
	}

	@Override
	public boolean open()
	{
		return waitingOnAnyone() || !incoming.isEmpty();
	}

	private boolean waitingOnAnyone()
	{
		return outgoing != null && clock.getAsLong() - outgoing.sentMs <= OFFER_TIMEOUT_MS;
	}

	@Override
	public int reservedCreatureSlots()
	{
		return reserved(false);
	}

	@Override
	public int reservedEggSlots()
	{
		return reserved(true);
	}

	/** Drops every trade, for when the player joins or leaves a party. */
	void clear()
	{
		incoming.clear();
		noticedMs.clear();
		outgoing = null;
	}

	/**
	 * Describes the offer a friend has made, for their card.
	 *
	 * @param friendId the friend's party member id
	 * @return the offer in words, such as {@code Imp (Lv 20) for your Cow (Lv 5)}; empty when there is none to
	 *     answer
	 */
	String offerFrom(long friendId)
	{
		Incoming offer = incoming.get(friendId);
		if (offer == null || offer.agreedMs != 0 || clock.getAsLong() - offer.receivedMs > OFFER_TIMEOUT_MS)
			return "";

		String theirs = label(offer.theirs, species);
		if (offer.asked == null)
			return theirs + " as a gift";

		return theirs + " for your " + label(describe(offer.asked), species);
	}

	/**
	 * Reports whether the player is waiting for a friend to answer an offer.
	 *
	 * @param friendId the friend's party member id
	 * @return true while the player's offer to them is open
	 */
	boolean waitingOn(long friendId)
	{
		return outgoing != null && outgoing.friendId == friendId
			&& clock.getAsLong() - outgoing.sentMs <= OFFER_TIMEOUT_MS;
	}

	/**
	 * Puts a creature or egg into words.
	 *
	 * @param item the creature or egg
	 * @param species every catchable species, for the creature's proper name
	 * @return a label such as {@code Imp (Lv 20)}, {@code gilded Imp (Lv 20)} or {@code Rare egg}
	 */
	static String label(Item item, SpeciesTable species)
	{
		if (item.egg() != null)
		{
			Species boss = item.eggSpecies() == null ? null : species.find(item.eggSpecies());
			Rarity rarity = rarityNamed(item.egg());
			if (boss != null)
				return boss.name() + " egg";

			return (rarity == null ? "Unknown" : rarity.label()) + " egg";
		}

		Species found = species.find(item.species());
		String name = found == null ? String.valueOf(item.species()) : found.name();
		StatRolls rolls = item.rolls();
		String rating = rolls == null || rolls.rating() == StatRolls.Rating.AVERAGE ? "" : " " + rolls.rating().label();
		return (item.gilded() ? "gilded " : "") + name + " (Lv " + item.level() + ")" + rating;
	}

	/**
	 * Finds a rarity by the name a message used for it.
	 *
	 * @param name a {@link Rarity#name()}, as sent
	 * @return the rarity, or null when the name is not one
	 */
	static Rarity rarityNamed(String name)
	{
		for (Rarity rarity : Rarity.values())
		{
			if (rarity.name().equals(name))
				return rarity;
		}

		return null;
	}

	private void offer(long friendId, String friendName, Mine give, Item want)
	{
		long now = clock.getAsLong();
		if (!owns(give))
		{
			tell("You no longer have that", Tone.BAD);
			return;
		}

		if (outgoing != null && now - outgoing.sentMs <= OFFER_TIMEOUT_MS)
		{
			tell("Still waiting for an answer to your last offer", Tone.BAD);
			return;
		}

		if (promised(give, null))
		{
			tell("That is already promised in a trade you accepted", Tone.BAD);
			return;
		}

		if (!roomFor(want, give))
		{
			tell(noRoom(want), Tone.BAD);
			return;
		}

		outgoing = new Outgoing(friendId, random.nextLong(), give, describe(give), want, now);
		GielinorGoPartyMessage sent = bare(GielinorGoPartyMessage.OFFER, friendId, outgoing.id);
		outbox.accept(sent.giving(describe(give)).wanting(want));
		tell("Offered " + label(describe(give), species) + " to " + friendName, Tone.INFO);
	}

	/**
	 * Notes an offer from a friend. What they ask for is looked up among the player's own things straight away;
	 * an offer asking for something the player does not have is turned down without bothering the player.
	 *
	 * @param message the offer
	 * @param senderName the friend's name
	 * @return true when the offer was kept
	 */
	private boolean offered(GielinorGoPartyMessage message, String senderName)
	{
		long sender = message.getMemberId();
		long now = clock.getAsLong();
		Incoming open = incoming.get(sender);
		if (!valid(message.give()) || (open == null && incoming.size() >= MAX_OFFERS))
			return false;

		if (open != null && !stale(open, now))
			return false;

		Mine asked = null;
		if (message.want() != null)
		{
			asked = find(message.want());
			if (asked == null)
			{
				send(GielinorGoPartyMessage.REFUSE, sender, message.battle());
				return false;
			}
		}

		incoming.put(sender, new Incoming(message.battle(), message.give(), asked, now));
		Long last = noticedMs.get(sender);
		if (last == null || now - last >= NOTICE_GAP_MS)
		{
			if (noticedMs.size() >= MAX_OFFERS)
				noticedMs.clear();

			noticedMs.put(sender, now);
			tell(senderName + " offers a trade. See the Battle tab", Tone.INFO);
		}

		return true;
	}

	/**
	 * Finishes the player's own offer once the friend has agreed: checks both halves, makes the swap and tells
	 * the friend to make theirs.
	 *
	 * @param message the friend's agreement, naming what they give
	 * @param senderName the friend's name
	 * @return true when the message matched the player's open offer
	 */
	private boolean agreed(GielinorGoPartyMessage message, String senderName)
	{
		long sender = message.getMemberId();
		if (!waitingOn(sender) || outgoing.id != message.battle())
			return false;

		Outgoing mine = outgoing;
		outgoing = null;
		Item theirs = message.give();
		boolean asExpected = mine.want == null ? theirs == null : valid(theirs) && sameThing(mine.want, theirs);
		if (!asExpected || !unchanged(mine.give, mine.offered) || !roomFor(theirs, mine.give))
		{
			send(GielinorGoPartyMessage.REFUSE, sender, mine.id);
			tell("The trade with " + senderName + " fell through", Tone.BAD);
			return true;
		}

		String gave = label(describe(mine.give), species);
		giveAway(mine.give);
		if (theirs != null && !take(theirs))
			tell("There was no room for " + label(theirs, species), Tone.BAD);

		send(GielinorGoPartyMessage.CONFIRM, sender, mine.id);
		traded = true;
		tell(theirs == null ? "Gave " + gave + " to " + senderName
			: "Traded " + gave + " for " + senderName + "'s " + label(theirs, species), Tone.GOOD);
		return true;
	}

	/**
	 * Makes the player's half of a swap the friend has just confirmed. The asked creature may have gained XP or
	 * evolved since the player agreed: the friend already holds the agreed description, so the swap goes ahead as
	 * long as the player still owns it. A promised item cannot be released and its room is reserved, so the checks
	 * here are a last guard.
	 *
	 * @param message the confirmation
	 * @param senderName the friend's name
	 * @return true when the message matched a trade the player agreed to
	 */
	private boolean confirmed(GielinorGoPartyMessage message, String senderName)
	{
		long sender = message.getMemberId();
		Incoming offer = incoming.get(sender);
		if (offer == null || offer.id != message.battle() || offer.agreedMs == 0)
			return false;

		incoming.remove(sender);
		boolean kept = offer.asked == null || owns(offer.asked);
		if (!kept || !roomFor(offer.theirs, offer.asked))
		{
			send(GielinorGoPartyMessage.REFUSE, sender, offer.id);
			tell("The trade with " + senderName + " fell through", Tone.BAD);
			return true;
		}

		String gave = offer.asked == null ? null : label(describe(offer.asked), species);
		if (offer.asked != null)
			giveAway(offer.asked);

		if (!take(offer.theirs))
			tell("There was no room for " + label(offer.theirs, species), Tone.BAD);

		traded = true;
		String got = label(offer.theirs, species);
		tell(gave == null ? senderName + " gave you " + got
			: "Traded " + gave + " for " + senderName + "'s " + got, Tone.GOOD);
		return true;
	}

	private boolean refused(GielinorGoPartyMessage message, String senderName)
	{
		long sender = message.getMemberId();
		if (outgoing != null && outgoing.friendId == sender && outgoing.id == message.battle())
		{
			outgoing = null;
			tell(senderName + " turned the trade down", Tone.BAD);
			return true;
		}

		Incoming offer = incoming.get(sender);
		if (offer == null || offer.id != message.battle())
			return false;

		incoming.remove(sender);
		if (offer.agreedMs != 0)
			tell("The trade with " + senderName + " fell through", Tone.BAD);

		return true;
	}

	/**
	 * Checks that a creature or egg named in a message could exist.
	 *
	 * @param item the item, as sent
	 * @return true for a known species with XP inside the level cap and rolls in range, or a real egg rarity with
	 *     a sensible number of tiles left
	 */
	private boolean valid(Item item)
	{
		if (item == null)
			return false;

		if (item.egg() != null)
		{
			Rarity rarity = rarityNamed(item.egg());
			return item.species() == null && rarity != null && item.tilesLeft() >= 0
				&& item.tilesLeft() <= rarity.eggTiles() && validBossEgg(item, rarity);
		}

		return item.species() != null && species.find(item.species()) != null && item.rolls() != null
			&& item.xp() >= 0 && item.xp() <= XpRules.xpForLevel(XpRules.MAX_LEVEL)
			&& XpRules.levelForXp(item.xp()) == item.level() && validMoves(item);
	}

	/**
	 * Checks the fixed species of an egg in a message: none, or a species that boss kills give an egg of, whose
	 * rarity is the egg's.
	 *
	 * @param item an egg whose rarity is already checked
	 * @param rarity the egg's rarity
	 * @return true when the egg is ordinary or a real boss egg
	 */
	private boolean validBossEgg(Item item, Rarity rarity)
	{
		if (item.eggSpecies() == null)
			return true;

		Species boss = species.find(item.eggSpecies());
		return boss != null && game.bossTable().isBoss(boss) && boss.rarity() == rarity;
	}

	/**
	 * Checks the moves a creature in a message carries: none, or at most four that are all different and all in the
	 * species' pool at the creature's level.
	 *
	 * @param item a creature whose species and level are already checked
	 * @return true when the moves are allowed
	 */
	private boolean validMoves(Item item)
	{
		String[] ids = item.moves();
		if (ids == null)
			return true;

		if (ids.length > MoveTable.MAX_KNOWN)
			return false;

		Species found = species.find(item.species());
		Set<String> seen = new HashSet<>();
		for (String id : ids)
		{
			if (id == null || !seen.add(id) || !game.moveTable().knows(found, item.level(), id))
				return false;
		}

		return true;
	}

	/**
	 * Checks that what a friend sent is what the player asked for.
	 *
	 * @param want what the player asked for: a species and level, or an egg rarity
	 * @param got what the friend gives; already checked by {@link #valid(Item)}
	 * @return true when they match
	 */
	private static boolean sameThing(Item want, Item got)
	{
		if (want.egg() != null)
			return want.egg().equals(got.egg()) && Objects.equals(want.eggSpecies(), got.eggSpecies());

		return got.egg() == null && Species.keyFor(want.species()).equals(Species.keyFor(got.species()))
			&& XpRules.levelForXp(got.xp()) == want.level() && sameRolls(want, got) && sameMoves(want, got);
	}

	/**
	 * Checks that two creatures know the same moves, when the one asked for names them. A request from a build that
	 * sends no moves matches any creature.
	 *
	 * @param want what was asked for
	 * @param got what was found or offered
	 * @return true when the moves match or the request names none
	 */
	private static boolean sameMoves(Item want, Item got)
	{
		String[] asked = want.moves();
		return asked == null || Arrays.equals(asked, got.moves());
	}

	/**
	 * Checks that two creatures have the same rolls and temperament, when the one asked for names them. A request
	 * from a build that sends no rolls matches any creature.
	 *
	 * @param want what was asked for
	 * @param got what was found or offered
	 * @return true when the rolls match or the request names none
	 */
	private static boolean sameRolls(Item want, Item got)
	{
		StatRolls asked = want.rolls();
		return !want.hasRolls() || (asked != null && asked.equals(got.rolls()));
	}

	/**
	 * Finds, among the player's own things, what a friend asked for.
	 *
	 * @param want a species and level on the player's team, or an egg rarity
	 * @return the creature or egg, or null when the player has nothing like it
	 */
	private Mine find(Item want)
	{
		if (want.egg() != null)
		{
			for (Egg egg : game.data().eggs)
			{
				if (egg.rarityTier().name().equals(want.egg()) && Objects.equals(egg.species, want.eggSpecies()))
					return new Mine(null, egg);
			}

			return null;
		}

		for (Creature creature : game.data().team)
		{
			boolean same = Species.keyFor(creature.species).equals(Species.keyFor(want.species()));
			if (same && creature.level() == want.level() && sameRolls(want, describe(new Mine(creature, null)))
				&& sameMoves(want, describe(new Mine(creature, null))))
				return new Mine(creature, null);
		}

		return null;
	}

	/**
	 * Checks there is room for what a trade brings in. A creature for a creature and an egg for an egg always fit.
	 *
	 * @param coming what the player would receive, or null for nothing
	 * @param going what the player would give, or null for nothing
	 * @return true when it fits
	 */
	private boolean roomFor(Item coming, Mine going)
	{
		if (coming == null)
			return true;

		if (coming.egg() != null)
			return (going != null && going.egg != null) || game.hasRoomForEgg();

		return (going != null && going.creature != null) || game.hasRoomForCreature();
	}

	/**
	 * Counts the slots open trades will need: one for each agreed incoming offer, and for the player's own open
	 * offer, whose item is not matched by one of the same kind going out.
	 *
	 * @param eggs true to count egg slots, false for creature slots
	 * @return the slots held back
	 */
	private int reserved(boolean eggs)
	{
		long now = clock.getAsLong();
		int slots = 0;
		if (outgoing != null && now - outgoing.sentMs <= OFFER_TIMEOUT_MS)
			slots += needs(outgoing.want, outgoing.give, eggs);

		for (Incoming offer : incoming.values())
		{
			if (offer.agreedMs != 0 && !stale(offer, now))
				slots += needs(offer.theirs, offer.asked, eggs);
		}

		return slots;
	}

	private static int needs(Item coming, Mine going, boolean eggs)
	{
		if (coming == null || (coming.egg() != null) != eggs)
			return 0;

		boolean swapped = going != null && (going.egg != null) == eggs;
		return swapped ? 0 : 1;
	}

	private static boolean stale(Incoming offer, long now)
	{
		return offer.agreedMs == 0
			? now - offer.receivedMs > OFFER_TIMEOUT_MS
			: now - offer.agreedMs > CONFIRM_TIMEOUT_MS;
	}

	/**
	 * Reports whether a creature or egg is already promised in a trade that has not finished: the player's own open
	 * offer, or another offer the player accepted and has not yet seen confirmed.
	 *
	 * @param mine the creature or egg
	 * @param except the incoming offer being answered, which does not count, or null
	 * @return true when giving it here as well could hand it out twice
	 */
	private boolean promised(Mine mine, Incoming except)
	{
		long now = clock.getAsLong();
		if (outgoing != null && now - outgoing.sentMs <= OFFER_TIMEOUT_MS && outgoing.give.is(mine))
			return true;

		for (Incoming other : incoming.values())
		{
			if (other != except && other.agreedMs != 0 && other.asked != null && !stale(other, now)
				&& other.asked.is(mine))
				return true;
		}

		return false;
	}

	/**
	 * Checks that something the player gives is still owned and still what was described when the trade was set
	 * up. A creature that gained XP, grew or evolved is not. An egg keeps being the same egg while it is walked.
	 *
	 * @param mine the creature or egg
	 * @param before what it was described as
	 * @return true when it can still be given as described
	 */
	private boolean unchanged(Mine mine, Item before)
	{
		if (!owns(mine))
			return false;

		Item now = describe(mine);
		if (before.egg() != null)
			return before.egg().equals(now.egg()) && Objects.equals(before.eggSpecies(), now.eggSpecies());

		return now.egg() == null && Species.keyFor(before.species()).equals(Species.keyFor(now.species()))
			&& before.xp() == now.xp() && before.gilded() == now.gilded() && before.rolls().equals(now.rolls())
			&& Arrays.equals(before.moves(), now.moves());
	}

	/**
	 * Finds, in the list of what a friend has shown, the item the player picked. The list can change between
	 * the dialog opening and the offer being sent, so the label the player saw is checked too.
	 *
	 * @param theirs the friend's tradeable items, from {@code FriendBattles.tradeable}
	 * @param index the place the player picked, or -1 for nothing
	 * @param label the label shown for that place
	 * @return the item, or null when there is nothing at that place or it is not the item shown
	 */
	Item friendItem(List<Item> theirs, int index, String label)
	{
		if (index < 0 || index >= theirs.size() || !label(theirs.get(index), species).equals(label))
			return null;

		return theirs.get(index);
	}

	private static String noRoom(Item coming)
	{
		return coming.egg() != null ? "You cannot carry another egg" : "Storage is full";
	}

	private boolean owns(Mine mine)
	{
		return mine.creature != null ? game.owns(mine.creature) : mine.egg != null && game.owns(mine.egg);
	}

	private void giveAway(Mine mine)
	{
		if (mine.creature != null)
			game.giveAway(mine.creature);
		else
			game.giveAway(mine.egg);
	}

	/**
	 * Adds a creature or egg a friend gave to the player's save. A creature arrives rested, with the XP and the
	 * gilded flag it had; an egg keeps the tiles it still needs.
	 *
	 * @param item the item; already checked by {@link #valid(Item)}
	 * @return false when there was no room, which the checks before the swap should make impossible
	 */
	private boolean take(Item item)
	{
		if (item.egg() != null)
		{
			Egg egg = new Egg(rarityNamed(item.egg()));
			egg.species = item.eggSpecies() == null ? null : species.find(item.eggSpecies()).key();
			egg.tilesLeft = item.tilesLeft();
			return game.receive(egg);
		}

		Creature creature = new Creature(species.find(item.species()).key(), 1, clock.getAsLong());
		creature.xp = XpRules.capped(item.xp());
		creature.gilded = item.gilded();
		creature.setRolls(item.rolls());
		Species found = species.find(item.species());
		MoveLearning.start(creature, found, game.moveTable());
		if (item.moves() != null)
			creature.moves = new ArrayList<>(Arrays.asList(item.moves()));

		return game.receive(creature);
	}

	private Item describe(Mine mine)
	{
		if (mine.creature != null)
		{
			Creature creature = mine.creature;
			String[] moves = MoveLearning.sendable(creature, species.find(creature.species), game.moveTable());
			return Item.creature(creature.species, creature.level(), creature.xp, creature.gilded, creature.rolls(),
				moves);
		}

		return Item.egg(mine.egg.rarityTier().name(), mine.egg.tilesLeft, mine.egg.species);
	}

	private static GielinorGoPartyMessage bare(String kind, long to, long id)
	{
		return new GielinorGoPartyMessage(kind, to, id, 0, 0, new String[0], new int[0]);
	}

	private void send(String kind, long to, long id)
	{
		outbox.accept(bare(kind, to, id));
	}

	private void tell(String text, Tone tone)
	{
		notices.accept(Notice.player(text, tone));
	}
}
