/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Creature Log milestones: a reward at 25, 50, 100, 250 and 500 species caught, and one for completing a
 * family such as all goblins. Pure rules with no RuneLite types: the game asks which milestones are reached and what
 * each one pays, and keeps the paid ones in {@link SaveData#milestones}.
 */
final class Milestones
{
	/** The species counts that pay a milestone, in order. */
	static final int[] SPECIES_STEPS = {25, 50, 100, 250, 500};

	/** The species count whose reward is the Gilded charm. */
	static final int CHARM_STEP = 250;

	/** The id of the milestone that is the Gilded charm. */
	static final String CHARM_ID = "species-" + CHARM_STEP;

	/** Jars paid for completing a family. */
	static final int FAMILY_JARS = 3;

	/** Milestones with nothing in them, the default before the game is given its tables. */
	static final Milestones NONE = new Milestones(new SpeciesTable(Collections.emptyList()), Collections.emptyList());

	/** What one milestone pays. */
	static final class Reward
	{
		private final JarType jar;
		private final int jars;
		private final Rarity egg;
		private final boolean charm;

		Reward(JarType jar, int jars, Rarity egg, boolean charm)
		{
			this.jar = jar;
			this.jars = jars;
			this.egg = egg;
			this.charm = charm;
		}

		/** @return the jar type paid, or null when no jars are */
		JarType jar()
		{
			return jar;
		}

		/** @return how many jars are paid, 0 for none */
		int jars()
		{
			return jars;
		}

		/** @return the rarity of the egg paid, or null when no egg is */
		Rarity egg()
		{
			return egg;
		}

		/** @return whether the reward is the Gilded charm */
		boolean charm()
		{
			return charm;
		}

		/** @return the reward in a few words, such as {@code 5 Rune jars + Rare egg} */
		String text()
		{
			List<String> parts = new ArrayList<>();
			if (jars > 0)
				parts.add(jars + " " + jar.label() + (jars == 1 ? " jar" : " jars"));

			if (egg != null)
				parts.add(egg.label() + " egg");

			if (charm)
				parts.add("Gilded charm");

			return String.join(" + ", parts);
		}
	}

	/** One milestone: a goal and its reward. */
	static final class Milestone
	{
		private final String id;
		private final String label;
		private final int target;
		private final Family family;
		private final Reward reward;

		Milestone(String id, String label, int target, Family family, Reward reward)
		{
			this.id = id;
			this.label = label;
			this.target = target;
			this.family = family;
			this.reward = reward;
		}

		/** @return the id saved in {@link SaveData#milestones} once paid */
		String id()
		{
			return id;
		}

		/** @return the goal in words, such as {@code 100 species} or {@code All Goblins} */
		String label()
		{
			return label;
		}

		/** @return the species count that completes a count milestone, or the family's size */
		int target()
		{
			return target;
		}

		/** @return the family for a family milestone, or null for a species count */
		Family family()
		{
			return family;
		}

		/** @return what it pays */
		Reward reward()
		{
			return reward;
		}
	}

	private final SpeciesTable species;
	private final List<Milestone> all;

	/**
	 * Builds the milestones from the species and family tables. Family members the species table does not know are
	 * left out, and a family with fewer than three known members is dropped.
	 *
	 * @param species every catchable species
	 * @param families the families
	 */
	Milestones(SpeciesTable species, List<Family> families)
	{
		this.species = species;
		List<Milestone> built = new ArrayList<>();
		for (int step : SPECIES_STEPS)
			built.add(new Milestone("species-" + step, step + " species", step, null, countReward(step)));

		for (Family family : families)
		{
			if (family == null || family.id() == null)
				continue;

			List<String> known = knownMembers(family);
			if (known.size() < 3)
				continue;

			Family trimmed = new Family(family.id(), family.label(), family.jar(), known);
			Rarity top = Rarity.COMMON;
			for (String key : known)
			{
				Rarity rarity = species.find(key).rarity();
				if (rarity.ordinal() > top.ordinal())
					top = rarity;
			}

			built.add(new Milestone("family-" + family.id(), "All " + family.label(), known.size(), trimmed,
				new Reward(family.jar(), FAMILY_JARS, top, false)));
		}

		all = Collections.unmodifiableList(built);
	}

	private static Reward countReward(int step)
	{
		switch (step)
		{
			case 25:
				return new Reward(JarType.IRON, 5, null, false);
			case 50:
				return new Reward(JarType.RUNE, 5, Rarity.RARE, false);
			case 100:
				return new Reward(JarType.DRAGON, 5, Rarity.EPIC, false);
			case CHARM_STEP:
				return new Reward(null, 0, null, true);
			default:
				return new Reward(JarType.DRAGON, 10, Rarity.LEGENDARY, false);
		}
	}

	private List<String> knownMembers(Family family)
	{
		List<String> known = new ArrayList<>();
		for (String key : family.species())
		{
			if (key != null && species.find(key) != null && !known.contains(key))
				known.add(key);
		}

		return known;
	}

	/** @return every milestone: the species counts first, then the families */
	List<Milestone> all()
	{
		return all;
	}

	/**
	 * Finds a milestone by id.
	 *
	 * @param id the milestone's id
	 * @return the milestone, or null when there is none
	 */
	Milestone byId(String id)
	{
		for (Milestone milestone : all)
		{
			if (milestone.id().equals(id))
				return milestone;
		}

		return null;
	}

	/**
	 * Finds what a milestone pays.
	 *
	 * @param id the milestone's id
	 * @return the reward, or null when there is no such milestone
	 */
	Reward rewardFor(String id)
	{
		Milestone milestone = byId(id);
		return milestone == null ? null : milestone.reward();
	}

	/**
	 * Counts the species a save has caught that the species table still knows.
	 *
	 * @param data the save
	 * @return the count
	 */
	int caughtCount(SaveData data)
	{
		int known = 0;
		for (String key : data.caught.keySet())
		{
			if (species.find(key) != null)
				known++;
		}

		return known;
	}

	/**
	 * Counts how far a save is towards a milestone.
	 *
	 * @param milestone the milestone
	 * @param data the save
	 * @return the species caught, or for a family the members caught, never above the target
	 */
	int progressOf(Milestone milestone, SaveData data)
	{
		if (milestone.family() == null)
			return Math.min(milestone.target(), caughtCount(data));

		int have = 0;
		for (String key : milestone.family().species())
		{
			Integer times = data.caught.get(key);
			if (times != null && times > 0)
				have++;
		}

		return have;
	}

	/**
	 * Lists the milestones whose goal a save has met, paid or not.
	 *
	 * @param data the save
	 * @return the ids, in the order of {@link #all()}
	 */
	List<String> reached(SaveData data)
	{
		List<String> ids = new ArrayList<>();
		for (Milestone milestone : all)
		{
			if (progressOf(milestone, data) >= milestone.target())
				ids.add(milestone.id());
		}

		return ids;
	}

	/**
	 * Builds what the Log tab shows.
	 *
	 * @param data the save
	 * @return the next species goal and every milestone with its progress
	 */
	MilestoneView progress(SaveData data)
	{
		int caught = caughtCount(data);
		String nextLabel = null;
		int nextTarget = 0;
		for (int step : SPECIES_STEPS)
		{
			if (caught < step)
			{
				nextLabel = "Next: " + step + " species";
				nextTarget = step;
				break;
			}
		}

		List<MilestoneView.Row> rows = new ArrayList<>();
		for (Milestone milestone : all)
		{
			rows.add(new MilestoneView.Row(milestone.label(), progressOf(milestone, data), milestone.target(),
				milestone.reward().text(), data.milestones.contains(milestone.id()), milestone.family() != null));
		}

		return new MilestoneView(nextLabel, caught, nextTarget, rows);
	}
}
