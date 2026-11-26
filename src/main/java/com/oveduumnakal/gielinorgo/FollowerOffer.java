/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What the bubbles over the follower may offer at one moment: only what the game already allows. A bubble can be left
 * out altogether, or shown greyed while it cannot be used. Holds no RuneLite types.
 */
final class FollowerOffer
{
	/** A creature of the team, as a bubble shows it. */
	static final class Member
	{
		private final int index;
		private final String name;
		private final String species;
		private final int npcId;
		private final int level;
		private final double health;

		/**
		 * Describes a creature.
		 *
		 * @param index its place, in the team out of battle and in the battle's own team during one
		 * @param name what it is called
		 * @param species its species, for the portrait
		 * @param npcId the NPC id it looks like, or 0
		 * @param level its level
		 * @param health its share of full health, from 0 to 1
		 */
		Member(int index, String name, String species, int npcId, int level, double health)
		{
			this.index = index;
			this.name = name;
			this.species = species;
			this.npcId = npcId;
			this.level = level;
			this.health = health;
		}

		/** @return its place */
		int index()
		{
			return index;
		}

		/** @return its name */
		String name()
		{
			return name;
		}

		/** @return its species */
		String species()
		{
			return species;
		}

		/** @return the NPC id it looks like, or 0 */
		int npcId()
		{
			return npcId;
		}

		/** @return its level */
		int level()
		{
			return level;
		}

		/** @return its share of full health */
		double health()
		{
			return health;
		}
	}

	/** An owned item and the creatures it would help now. */
	static final class Item
	{
		private final ItemType type;
		private final int count;
		private final List<Member> targets;

		/**
		 * Describes an item.
		 *
		 * @param type the item
		 * @param count how many are owned
		 * @param targets the creatures it would help; empty greys it
		 */
		Item(ItemType type, int count, List<Member> targets)
		{
			this.type = type;
			this.count = count;
			this.targets = Collections.unmodifiableList(new ArrayList<>(targets));
		}

		/** @return the item */
		ItemType type()
		{
			return type;
		}

		/** @return how many are owned */
		int count()
		{
			return count;
		}

		/** @return the creatures it would help */
		List<Member> targets()
		{
			return targets;
		}
	}

	private final boolean inBattle;
	private final boolean switchShown;
	private final boolean itemShown;
	private final boolean runShown;
	private final boolean live;
	private final double runChance;
	private final List<Member> others;
	private final List<Item> items;
	private final int out;

	private FollowerOffer(Builder b)
	{
		this.inBattle = b.inBattle;
		this.switchShown = b.switchShown;
		this.itemShown = b.itemShown;
		this.runShown = b.runShown;
		this.live = b.live;
		this.runChance = b.runChance;
		this.others = Collections.unmodifiableList(new ArrayList<>(b.others));
		this.items = Collections.unmodifiableList(new ArrayList<>(b.items));
		this.out = b.out;
	}

	/** Collects the parts of an offer. */
	static final class Builder
	{
		private boolean inBattle;
		private boolean switchShown;
		private boolean itemShown;
		private boolean runShown;
		private boolean live = true;
		private double runChance = 1.0;
		private final List<Member> others = new ArrayList<>();
		private final List<Item> items = new ArrayList<>();
		private int out;

		/**
		 * Marks the offer as made during a battle.
		 *
		 * @param choiceOpen whether a choice can be made now
		 * @return this builder
		 */
		Builder battle(boolean choiceOpen)
		{
			inBattle = true;
			live = choiceOpen;
			return this;
		}

		/**
		 * Shows the Switch bubble.
		 *
		 * @param members the creatures that can be brought out
		 * @return this builder
		 */
		Builder switching(List<Member> members)
		{
			switchShown = true;
			others.addAll(members);
			return this;
		}

		/**
		 * Shows the Item bubble.
		 *
		 * @param owned the owned items, in item order
		 * @param creatureOut the place of the creature that is out, which a quick use prefers
		 * @return this builder
		 */
		Builder items(List<Item> owned, int creatureOut)
		{
			itemShown = true;
			items.addAll(owned);
			out = creatureOut;
			return this;
		}

		/**
		 * Shows the Run bubble.
		 *
		 * @param chance the chance of getting away
		 * @return this builder
		 */
		Builder running(double chance)
		{
			runShown = true;
			runChance = chance;
			return this;
		}

		/** @return the offer */
		FollowerOffer build()
		{
			return new FollowerOffer(this);
		}
	}

	/** @return whether a battle is on */
	boolean inBattle()
	{
		return inBattle;
	}

	/** @return whether the Switch bubble is shown */
	boolean switchShown()
	{
		return switchShown;
	}

	/** @return whether the Item bubble is shown */
	boolean itemShown()
	{
		return itemShown;
	}

	/** @return whether the Run bubble is shown */
	boolean runShown()
	{
		return runShown;
	}

	/** @return whether anything is shown at all */
	boolean any()
	{
		return switchShown || itemShown || runShown;
	}

	/** @return whether Switch would do anything now */
	boolean switchEnabled()
	{
		return live && !others.isEmpty();
	}

	/** @return whether some item would help someone now */
	boolean itemEnabled()
	{
		return live && quickItem() != null;
	}

	/** @return whether Run can be pressed now */
	boolean runEnabled()
	{
		return live;
	}

	/** @return whether a choice can be made now */
	boolean live()
	{
		return live;
	}

	/** @return the chance of getting away */
	double runChance()
	{
		return runChance;
	}

	/** @return the creatures that can be brought out */
	List<Member> others()
	{
		return others;
	}

	/** @return the owned items */
	List<Item> items()
	{
		return items;
	}

	/** @return the item a quick use takes, the first that would help someone, or null */
	Item quickItem()
	{
		return items.stream()
				.filter(item -> !item.targets().isEmpty())
				.findFirst()
				.orElse(null);
	}

	/**
	 * Picks whom a quick use helps: the creature that is out when it is one of them, otherwise the first.
	 *
	 * @param item an item with targets
	 * @return the creature
	 */
	Member quickTarget(Item item)
	{
		return item.targets()
				.stream()
				.filter(member -> member.index() == out)
				.findFirst()
				.orElse(item.targets().get(0));
	}
}
