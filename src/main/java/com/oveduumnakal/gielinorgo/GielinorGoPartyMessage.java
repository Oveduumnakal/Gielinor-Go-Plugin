/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * The one message Gielinor Go sends through a RuneLite party, for battles and trades between friends. It reaches
 * only the people in the player's own party, and carries nothing but creatures and eggs: a team's species and
 * levels, the trainer level, the rarities of the eggs carried, and the creature or egg named in a trade. RuneLite
 * tells messages apart by class name, so this name must not be reused by another plugin. Creatures travel with
 * their rolls and temperament; nicknames never do.
 */
public class GielinorGoPartyMessage extends PartyMemberMessage
{
	/** Kind of a message that tells the party "I play too, and this is my team". */
	static final String HELLO = "HELLO";

	/** Kind of a message that asks one party member for a battle. */
	static final String CHALLENGE = "CHALLENGE";

	/** Kind of a message that takes up a challenge and sends the other team back. */
	static final String ACCEPT = "ACCEPT";

	/** Kind of a message that turns a challenge down. */
	static final String DECLINE = "DECLINE";

	/** Kind of a message that offers one party member a trade. */
	static final String OFFER = "OFFER";

	/** Kind of a message that agrees to an offer and names what is given in return. */
	static final String AGREE = "AGREE";

	/** Kind of a message that tells the other side the swap has been made, so they make it too. */
	static final String CONFIRM = "CONFIRM";

	/** Kind of a message that turns an offer down or calls a trade off. */
	static final String REFUSE = "REFUSE";

	/**
	 * The version of the battle rules and message layout this build speaks. Two players can only fight each other
	 * when it matches, since a different rule would give each of them a different winner. A message from a build
	 * that sends no version reads as 0. Version 3 added the moves of each creature and the moves of a traded one, and
	 * changed how a creature attacks (accuracy, power, no second style, a move picked at random). Version 4 added
	 * status effects (poison, venom, burn, freeze and stun), so a battle draws more random numbers and ends
	 * differently. Version 5 added the species of a boss egg in a trade. Version 6 split Melee into
	 * Stab, Slash and Crush, with a new chart of five styles.
	 */
	static final int PROTOCOL = 6;

	/** One creature or one egg named in a trade. Exactly one of the species and the egg rarity is set. */
	static final class Item
	{
		private String species;
		private int level;
		private long xp;
		private boolean gilded;
		private int[] rolls;
		private String temperament;
		private String[] moves;
		private String egg;
		private int tilesLeft;
		private String eggSpecies;

		/** Used by Gson. */
		Item()
		{
		}

		/**
		 * Names a creature.
		 *
		 * @param species the species key
		 * @param level the creature's level
		 * @param xp the creature's XP, or 0 when only the level is known
		 * @param gilded whether the creature is gilded
		 * @return the item
		 */
		static Item creature(String species, int level, long xp, boolean gilded)
		{
			Item item = new Item();
			item.species = species;
			item.level = level;
			item.xp = xp;
			item.gilded = gilded;
			return item;
		}

		/**
		 * Names a creature with its rolls and temperament.
		 *
		 * @param species the species key
		 * @param level the creature's level
		 * @param xp the creature's XP, or 0 when only the level is known
		 * @param gilded whether the creature is gilded
		 * @param rolls the creature's rolls and temperament
		 * @return the item
		 */
		static Item creature(String species, int level, long xp, boolean gilded, StatRolls rolls)
		{
			Item item = creature(species, level, xp, gilded);
			item.rolls = rolls.values();
			item.temperament = rolls.temperament() == Temperament.STEADY ? null : rolls.temperament().name();
			return item;
		}

		/**
		 * Names a creature with its rolls, temperament and moves.
		 *
		 * @param species the species key
		 * @param level the creature's level
		 * @param xp the creature's XP, or 0 when only the level is known
		 * @param gilded whether the creature is gilded
		 * @param rolls the creature's rolls and temperament
		 * @param moves the ids of the creature's moves; null or empty leaves them to the receiver
		 * @return the item
		 */
		static Item creature(String species, int level, long xp, boolean gilded, StatRolls rolls, String[] moves)
		{
			Item item = creature(species, level, xp, gilded, rolls);
			item.moves = moves == null || moves.length == 0 ? null : moves.clone();
			return item;
		}

		/**
		 * Names an egg.
		 *
		 * @param rarity the egg's rarity, as a {@link Rarity#name()}
		 * @param tilesLeft tiles still to walk before it hatches
		 * @return the item
		 */
		static Item egg(String rarity, int tilesLeft)
		{
			Item item = new Item();
			item.egg = rarity;
			item.tilesLeft = tilesLeft;
			return item;
		}

		/**
		 * Names a boss egg.
		 *
		 * @param rarity the egg's rarity, as a {@link Rarity#name()}
		 * @param tilesLeft tiles still to walk before it hatches
		 * @param species the species key the egg hatches into
		 * @return the item
		 */
		static Item egg(String rarity, int tilesLeft, String species)
		{
			Item item = egg(rarity, tilesLeft);
			item.eggSpecies = species;
			return item;
		}

		/** @return the species key a boss egg hatches into, or null for an ordinary egg */
		String eggSpecies()
		{
			return eggSpecies;
		}

		/** @return the creature's species key, or null for an egg */
		String species()
		{
			return species;
		}

		/** @return the creature's level, as the sender claims it */
		int level()
		{
			return level;
		}

		/** @return the creature's XP, as the sender claims it */
		long xp()
		{
			return xp;
		}

		/** @return whether the creature is gilded */
		boolean gilded()
		{
			return gilded;
		}

		/**
		 * Reads the creature's rolls.
		 *
		 * @return the rolls, {@link StatRolls#AVERAGE} when the sender sent none, or null when what was sent is out
		 *     of range or unknown
		 */
		StatRolls rolls()
		{
			return StatRolls.of(rolls, temperament);
		}

		/** @return whether the sender sent rolls or a temperament, rather than leaving them out */
		boolean hasRolls()
		{
			return rolls != null || temperament != null;
		}

		/** @return the ids of the creature's moves as sent, or null when it sent none */
		String[] moves()
		{
			return moves == null ? null : moves.clone();
		}

		/** @return the egg's rarity name, or null for a creature */
		String egg()
		{
			return egg;
		}

		/** @return tiles the egg still needs, as the sender claims it */
		int tilesLeft()
		{
			return tilesLeft;
		}
	}

	private String kind;
	private long to;
	private long battle;
	private long seed;
	private int trainer;
	private boolean askBack;
	private String[] species;
	private int[] levels;
	private int[][] rolls;
	private String[] temperaments;
	private String[][] moves;
	private int version;
	private String[] eggs;
	private Item give;
	private Item want;

	/** Used by Gson. */
	public GielinorGoPartyMessage()
	{
	}

	/**
	 * Makes a message.
	 *
	 * @param kind one of {@link #HELLO}, {@link #CHALLENGE}, {@link #ACCEPT} and {@link #DECLINE}
	 * @param to the party member it is for, or 0 for everyone
	 * @param battle the number the sender gave the battle or the trade, or 0
	 * @param seed the sender's half of the battle's random seed, or 0
	 * @param trainer the sender's trainer level
	 * @param species the sender's team as species keys, in fighting order
	 * @param levels the level of each creature in {@code species}
	 */
	GielinorGoPartyMessage(String kind, long to, long battle, long seed, int trainer, String[] species, int[] levels)
	{
		this.kind = kind;
		this.to = to;
		this.battle = battle;
		this.seed = seed;
		this.trainer = trainer;
		this.species = species;
		this.levels = levels;
		this.version = PROTOCOL;
	}

	/** @return what the message is for; anything a party member sent, so it may be null or unknown */
	String kind()
	{
		return kind;
	}

	/** @return the party member the message is for, or 0 for everyone */
	long to()
	{
		return to;
	}

	/**
	 * Adds the rarities of the eggs the sender carries.
	 *
	 * @param rarities one {@link Rarity#name()} per egg
	 * @return this message
	 */
	GielinorGoPartyMessage carrying(String[] rarities)
	{
		eggs = rarities;
		return this;
	}

	/**
	 * Adds the rolls and temperaments of the team in {@code species}.
	 *
	 * @param teamRolls four rolls per creature, in the order health, attack, defence, speed
	 * @param teamTemperaments a {@link Temperament#name()} per creature, null for steady
	 * @return this message
	 */
	GielinorGoPartyMessage rolled(int[][] teamRolls, String[] teamTemperaments)
	{
		rolls = teamRolls;
		temperaments = teamTemperaments;
		return this;
	}

	/**
	 * Adds what the sender gives in a trade.
	 *
	 * @param item the creature or egg, or null for nothing
	 * @return this message
	 */
	GielinorGoPartyMessage giving(Item item)
	{
		give = item;
		return this;
	}

	/**
	 * Adds what the sender asks for in a trade.
	 *
	 * @param item the creature or egg, or null for nothing
	 * @return this message
	 */
	GielinorGoPartyMessage wanting(Item item)
	{
		want = item;
		return this;
	}

	/**
	 * Adds the moves of the team in {@code species}.
	 *
	 * @param teamMoves the move ids of each creature, in the order of the team; an empty list leaves a creature's
	 *     moves to what its level unlocks
	 * @return this message
	 */
	GielinorGoPartyMessage moved(String[][] teamMoves)
	{
		moves = teamMoves;
		return this;
	}

	/** @return the move ids of each creature; as sent, so it may be null (each level decides) or nonsense */
	String[][] moves()
	{
		return moves;
	}

	/** @return the rolls of each creature; as sent, so it may be null (all average) or nonsense */
	int[][] rolls()
	{
		return rolls;
	}

	/** @return the temperament of each creature; as sent, so it may be null (all steady) or nonsense */
	String[] temperaments()
	{
		return temperaments;
	}

	/** @return the protocol version the sender speaks, or 0 for a build that sends none */
	int version()
	{
		return version;
	}

	/** @return the rarities of the eggs the sender carries; as sent, so it may be null or nonsense */
	String[] eggs()
	{
		return eggs;
	}

	/** @return what the sender gives in a trade; as sent, so it may be null or nonsense */
	Item give()
	{
		return give;
	}

	/** @return what the sender asks for in a trade; as sent, so it may be null or nonsense */
	Item want()
	{
		return want;
	}

	/** @return the number the sender gave the battle or the trade */
	long battle()
	{
		return battle;
	}

	/** @return the sender's half of the battle's random seed */
	long seed()
	{
		return seed;
	}

	/**
	 * Marks a hello that wants an answer, sent by a player who has just logged in or joined and knows nobody yet.
	 *
	 * @return this message
	 */
	GielinorGoPartyMessage askingBack()
	{
		askBack = true;
		return this;
	}

	/** @return whether the sender wants every player in the party to say hello back */
	boolean askBack()
	{
		return askBack;
	}

	/** @return the sender's trainer level, as they claim it */
	int trainer()
	{
		return trainer;
	}

	/** @return the sender's team as species keys; anything a party member sent, so it may be null or nonsense */
	String[] species()
	{
		return species;
	}

	/** @return the level of each creature; anything a party member sent, so it may be null or nonsense */
	int[] levels()
	{
		return levels;
	}
}
