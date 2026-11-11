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
 * tells messages apart by class name, so this name must not be reused by another plugin.
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

	/** One creature or one egg named in a trade. Exactly one of the species and the egg rarity is set. */
	static final class Item
	{
		private String species;
		private int level;
		private long xp;
		private boolean gilded;
		private String egg;
		private int tilesLeft;

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
