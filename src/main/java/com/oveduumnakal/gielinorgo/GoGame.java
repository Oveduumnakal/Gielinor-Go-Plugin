/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * The game itself: jars, catching, training and battles, all applied to one {@link SaveData}. It has no link to
 * the game client, so every rule can be tested with a fixed random seed and a fake clock.
 */
final class GoGame
{
	/** Tiles the player walks to find one Bronze jar. */
	static final int TILES_PER_JAR = 150;

	/** Bronze jars given for each OSRS level-up. */
	static final int JARS_PER_LEVEL_UP = 3;

	/** Chance that a battle win drops a jar. */
	static final double JAR_DROP_CHANCE = 0.25;

	/** Least health, as a share of full, the lead needs to start a battle. */
	static final double MIN_BATTLE_HP = 0.25;

	private final SpeciesTable species;
	private final Random random;
	private final LongSupplier clock;
	private final Consumer<Notice> notices;
	private SaveData data = SaveData.fresh();
	private List<LogEntry> log;
	private long xpCarry;
	private boolean dirty;

	/**
	 * Creates a game with a fresh save.
	 *
	 * @param species every catchable species
	 * @param random the source of every roll
	 * @param clock wall-clock time in milliseconds
	 * @param notices receives each line the player should be told, with whose head it belongs over
	 */
	GoGame(SpeciesTable species, Random random, LongSupplier clock, Consumer<Notice> notices)
	{
		this.species = species;
		this.random = random;
		this.clock = clock;
		this.notices = notices;
	}

	/** @return the live save */
	SaveData data()
	{
		return data;
	}

	/**
	 * Replaces the save, for a login or an account switch.
	 *
	 * @param loaded the save to play from
	 */
	void load(SaveData loaded)
	{
		data = loaded.sanitized();
		log = null;
		xpCarry = 0;
		dirty = false;
	}

	/** @return whether the save changed since the last call; calling this clears the flag */
	boolean takeDirty()
	{
		boolean was = dirty;
		dirty = false;
		return was;
	}

	/**
	 * Finds the species for a monster name.
	 *
	 * @param name the monster's name, without colour tags
	 * @return the species, or null when the monster is not catchable
	 */
	Species speciesFor(String name)
	{
		return species.find(name);
	}

	/** @return the trainer level, 1 to 99 */
	int trainerLevel()
	{
		return XpRules.trainerLevelForXp(data.trainerXp);
	}

	/**
	 * Trains the team from XP the player just earned in the real game.
	 *
	 * @param gained OSRS XP gained, in any skill
	 */
	void addOsrsXp(int gained)
	{
		if (gained <= 0 || data.team.isEmpty())
			return;

		xpCarry += gained;
		long creatureXp = xpCarry / XpRules.OSRS_XP_PER_CREATURE_XP;
		if (creatureXp == 0)
			return;

		xpCarry -= creatureXp * XpRules.OSRS_XP_PER_CREATURE_XP;
		shareXp(creatureXp);
	}

	/**
	 * Handles one XP drop from the real game: trains the team, and may turn up the skill's specialty jar.
	 *
	 * @param skill the skill's enum name, such as {@code MINING}
	 * @param gained OSRS XP gained in that skill
	 */
	void addSkillXp(String skill, int gained)
	{
		if (gained <= 0)
			return;

		addOsrsXp(gained);
		JarType jar = JarType.forSkill(skill);
		if (jar == null || random.nextDouble() >= CatchRules.specialtyDropChance(gained))
			return;

		data.addJars(jar, 1);
		dirty = true;
		tell("+1 " + jar.label() + " jar (" + skillName(skill) + ")", Tone.FIND);
	}

	/**
	 * Counts tiles walked, and hands out a Bronze jar for every {@link #TILES_PER_JAR}.
	 *
	 * @param tiles tiles walked since the last call
	 */
	void addSteps(int tiles)
	{
		if (tiles <= 0)
			return;

		data.stepProgress += tiles;
		dirty = true;
		while (data.stepProgress >= TILES_PER_JAR)
		{
			data.stepProgress -= TILES_PER_JAR;
			data.addJars(JarType.BRONZE, 1);
			tell("+1 Bronze jar (walking)", Tone.FIND);
		}
	}

	/** Rewards an OSRS level-up with Bronze jars. */
	void onLevelUp()
	{
		data.addJars(JarType.BRONZE, JARS_PER_LEVEL_UP);
		dirty = true;
		tell("+" + JARS_PER_LEVEL_UP + " Bronze jars (level up)", Tone.FIND);
	}

	/**
	 * Records that the player met a species in the wild.
	 *
	 * @param entry the wild creature that appeared
	 */
	void markSeen(WildEntry entry)
	{
		if (!data.seen.add(entry.species().key()))
			return;

		log = null;
		dirty = true;
	}

	/**
	 * Picks the jar the Catch option throws: the selected type when the player owns one, otherwise the weakest
	 * type they do own.
	 *
	 * @return the jar type, or null when the player has no jars at all
	 */
	JarType jarToThrow()
	{
		JarType selected = selectedJar();
		if (data.jarCount(selected) > 0)
			return selected;

		for (JarType type : JarType.values())
		{
			if (data.jarCount(type) > 0)
				return type;
		}

		return null;
	}

	/**
	 * Lists the specialty jars worth offering for one creature: owned, good at one of its traits, and not already
	 * the jar {@link #jarToThrow()} picks.
	 *
	 * @param entry the wild creature
	 * @return the jars, in enum order; empty when none apply
	 */
	List<JarType> matchingSpecialtyJars(WildEntry entry)
	{
		JarType standard = jarToThrow();
		List<JarType> matches = new ArrayList<>();
		for (JarType type : JarType.values())
		{
			boolean fits = type.specialty() && entry.traits().contains(type.trait());
			if (fits && type != standard && data.jarCount(type) > 0)
				matches.add(type);
		}

		return matches;
	}

	/**
	 * Throws the jar {@link #jarToThrow()} picks at a wild creature.
	 *
	 * @param entry the wild creature
	 * @return what happened, see {@link #tryCatch(WildEntry, JarType)}
	 */
	CatchOutcome tryCatch(WildEntry entry)
	{
		return tryCatch(entry, jarToThrow());
	}

	/**
	 * Throws a chosen jar at a wild creature.
	 *
	 * @param entry the wild creature
	 * @param jar the jar to throw, or null when the player has none
	 * @return what happened; the caller removes the creature from the wild on {@link CatchOutcome#CAUGHT} and
	 *     {@link CatchOutcome#FLED}
	 */
	CatchOutcome tryCatch(WildEntry entry, JarType jar)
	{
		String name = entry.species().name();
		if (jar == null || data.jarCount(jar) <= 0)
		{
			tell("No jars left", Tone.BAD);
			return CatchOutcome.NO_JARS;
		}

		if (data.team.size() >= SaveData.TEAM_SIZE && data.storage.size() >= SaveData.STORAGE_SIZE)
		{
			tell("Storage is full", Tone.BAD);
			return CatchOutcome.FULL;
		}

		data.addJars(jar, -1);
		dirty = true;
		if (random.nextDouble() >= CatchRules.chance(jar, entry.combatLevel(), entry.weakened(), entry.traits()))
		{
			if (random.nextDouble() < CatchRules.FLEE_CHANCE)
			{
				tell(entry, "Broke free and ran away!", Tone.BAD);
				return CatchOutcome.FLED;
			}

			tell(entry, "Broke free!", Tone.BAD);
			return CatchOutcome.BROKE_FREE;
		}

		keep(new Creature(entry.species().key(), entry.level(), clock.getAsLong()));
		data.caught.merge(entry.species().key(), 1, Integer::sum);
		data.seen.add(entry.species().key());
		log = null;
		tell(entry, "Caught " + name + " (Lv " + entry.level() + ")!", Tone.GOOD);
		addTrainerXp(CatchRules.catchXp(entry.level()));
		return CatchOutcome.CAUGHT;
	}

	/**
	 * Sends the lead creature into a battle against a wild creature.
	 *
	 * @param entry the wild creature
	 * @return the battle, or null when no battle could start
	 */
	BattleResult battle(WildEntry entry)
	{
		Creature lead = data.lead();
		Species leadSpecies = lead == null ? null : species.find(lead.species);
		if (leadSpecies == null)
		{
			tell("Catch a creature first", Tone.BAD);
			return null;
		}

		if (entry.battled())
		{
			tell(entry, "Already battled", Tone.BAD);
			return null;
		}

		long now = clock.getAsLong();
		double health = lead.hpFractionAt(now);
		if (health < MIN_BATTLE_HP)
		{
			tell(leadSpecies.name() + " is too tired", Tone.BAD);
			return null;
		}

		Combatant mine = new Combatant(leadSpecies, lead.level(), health);
		Combatant foe = new Combatant(entry.species(), entry.level(), 1.0);
		BattleResult result = BattleSim.fight(mine, foe, random);
		lead.hpFraction = result.hpFractionLeft();
		lead.hpStampMs = now;
		entry.markBattled(result.won());
		dirty = true;
		if (result.won())
			rewardWin(lead, leadSpecies, entry);

		return result;
	}

	/**
	 * Makes a team member the lead.
	 *
	 * @param teamIndex position in the team
	 */
	void setLead(int teamIndex)
	{
		if (teamIndex <= 0 || teamIndex >= data.team.size())
			return;

		data.team.add(0, data.team.remove(teamIndex));
		dirty = true;
	}

	/**
	 * Moves a team member into storage.
	 *
	 * @param teamIndex position in the team
	 */
	void moveToStorage(int teamIndex)
	{
		if (teamIndex < 0 || teamIndex >= data.team.size())
			return;

		if (data.storage.size() >= SaveData.STORAGE_SIZE)
		{
			tell("Storage is full", Tone.BAD);
			return;
		}

		data.storage.add(data.team.remove(teamIndex));
		dirty = true;
	}

	/**
	 * Moves a stored creature onto the team.
	 *
	 * @param storageIndex position in storage
	 */
	void moveToTeam(int storageIndex)
	{
		if (storageIndex < 0 || storageIndex >= data.storage.size())
			return;

		if (data.team.size() >= SaveData.TEAM_SIZE)
		{
			tell("Team is full", Tone.BAD);
			return;
		}

		data.team.add(data.storage.remove(storageIndex));
		dirty = true;
	}

	/**
	 * Releases a stored creature for good.
	 *
	 * @param storageIndex position in storage
	 */
	void release(int storageIndex)
	{
		if (storageIndex < 0 || storageIndex >= data.storage.size())
			return;

		Creature released = data.storage.remove(storageIndex);
		dirty = true;
		tell("Released " + nameOf(released), Tone.INFO);
	}

	/**
	 * Chooses the jar thrown by the Catch option.
	 *
	 * @param type the jar type
	 */
	void selectJar(JarType type)
	{
		if (type == null || type.name().equals(data.selectedJar))
			return;

		data.selectedJar = type.name();
		dirty = true;
	}

	/** @return a copy of everything the side panel shows, safe to hand to the UI thread */
	PanelState snapshot()
	{
		long now = clock.getAsLong();
		Map<JarType, Integer> jars = new EnumMap<>(JarType.class);
		for (JarType type : JarType.values())
			jars.put(type, data.jarCount(type));

		return new PanelState(trainerLevel(), XpRules.trainerProgress(data.trainerXp), data.battlesWon, jars,
			selectedJar(), data.stepProgress, views(data.team, now), views(data.storage, now), log(),
			data.caughtSpecies(), data.seen.size());
	}

	private JarType selectedJar()
	{
		for (JarType type : JarType.values())
		{
			if (type.name().equals(data.selectedJar))
				return type;
		}

		return JarType.BRONZE;
	}

	private void keep(Creature creature)
	{
		if (data.team.size() < SaveData.TEAM_SIZE)
			data.team.add(creature);
		else
			data.storage.add(creature);
	}

	private void rewardWin(Creature lead, Species leadSpecies, WildEntry entry)
	{
		data.battlesWon++;
		int before = lead.level();
		lead.xp = XpRules.capped(lead.xp + 20L + 5L * entry.level());
		if (lead.level() > before)
			tell(leadSpecies.name() + " grew to Lv " + lead.level() + "!", Tone.GOOD);

		addTrainerXp(5L + entry.level());
		if (random.nextDouble() >= JAR_DROP_CHANCE)
			return;

		JarType drop = randomUnlockedJar();
		data.addJars(drop, 1);
		tell("+1 " + drop.label() + " jar (battle win)", Tone.FIND);
	}

	private JarType randomUnlockedJar()
	{
		List<JarType> unlocked = new ArrayList<>();
		int level = trainerLevel();
		for (JarType type : JarType.values())
		{
			if (!type.specialty() && level >= type.trainerLevel())
				unlocked.add(type);
		}

		int first = random.nextInt(unlocked.size());
		int second = random.nextInt(unlocked.size());
		return unlocked.get(Math.min(first, second));
	}

	private void addTrainerXp(long amount)
	{
		int before = trainerLevel();
		data.trainerXp += amount;
		dirty = true;
		int after = trainerLevel();
		if (after == before)
			return;

		tell("Trainer level " + after + "!", Tone.GOOD);
		for (JarType type : JarType.values())
		{
			if (type.trainerLevel() > before && type.trainerLevel() <= after)
				tell("Battles now drop " + type.label() + " jars", Tone.FIND);
		}
	}

	private void shareXp(long creatureXp)
	{
		int[] before = new int[data.team.size()];
		for (int i = 0; i < before.length; i++)
			before[i] = data.team.get(i).level();

		XpRules.share(data.team, creatureXp);
		dirty = true;
		for (int i = 0; i < before.length; i++)
		{
			Creature creature = data.team.get(i);
			if (creature.level() > before[i])
				tell(nameOf(creature) + " grew to Lv " + creature.level() + "!", Tone.GOOD);
		}
	}

	/**
	 * Tells the player something about themselves: a find, a reward, or something they cannot do.
	 *
	 * @param text the line
	 * @param tone its mood
	 */
	private void tell(String text, Tone tone)
	{
		notices.accept(Notice.player(text, tone));
	}

	/**
	 * Tells the player something about a wild creature.
	 *
	 * @param entry the creature the line is about
	 * @param text the line
	 * @param tone its mood
	 */
	private void tell(WildEntry entry, String text, Tone tone)
	{
		notices.accept(Notice.creature(entry.npcIndex(), text, tone));
	}

	private static String skillName(String skill)
	{
		String lower = skill.toLowerCase(Locale.ROOT);
		return lower.substring(0, 1).toUpperCase(Locale.ROOT) + lower.substring(1);
	}

	private String nameOf(Creature creature)
	{
		Species found = species.find(creature.species);
		return found == null ? creature.species : found.name();
	}

	private List<CreatureView> views(List<Creature> creatures, long now)
	{
		List<CreatureView> views = new ArrayList<>(creatures.size());
		for (Creature creature : creatures)
		{
			Species found = species.find(creature.species);
			Rarity rarity = found == null ? Rarity.COMMON : found.rarity();
			CombatType type = found == null ? CombatType.MELEE : found.style();
			views.add(new CreatureView(nameOf(creature), creature.level(), XpRules.progress(creature.xp),
				creature.hpFractionAt(now), rarity, type));
		}

		return views;
	}

	private List<LogEntry> log()
	{
		if (log != null)
			return log;

		List<LogEntry> entries = new ArrayList<>(species.size());
		for (Species entry : species.all())
		{
			Integer count = data.caught.get(entry.key());
			entries.add(new LogEntry(entry.name(), entry.level(), entry.rarity(), entry.style(),
				count == null ? 0 : count, data.seen.contains(entry.key())));
		}

		log = Collections.unmodifiableList(entries);
		return log;
	}
}
