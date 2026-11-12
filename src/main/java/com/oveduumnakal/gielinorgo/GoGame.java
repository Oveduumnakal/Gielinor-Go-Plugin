/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

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

	/** Chance that a battle win drops an egg, while the player has room for one. */
	static final double EGG_DROP_CHANCE = 0.10;

	/** Chance that a battle win over a roaming creature drops an egg, while the player has room for one. */
	static final double ROAMING_EGG_CHANCE = 0.30;

	/** Jars a task or streak pays when its egg has no free slot to go in. */
	static final int EGG_FALLBACK_JARS = 5;

	/** Bronze jars the first catch of a UTC day pays. */
	static final int STREAK_JARS = 2;

	/** The least share of full health a creature needs to take part in a battle of an Elite run. */
	static final double ELITE_MIN_HEALTH = 0.01;

	/** Most evolutions one creature can go through in a single burst of XP. */
	private static final int MAX_EVOLUTIONS_AT_ONCE = 5;

	/**
	 * What open trades hold back. A creature or egg promised in a trade cannot be released or hatched, and the
	 * room a trade will need is not given to anything else.
	 */
	interface TradeLocks
	{
		/**
		 * Reports whether a creature is promised in a trade.
		 *
		 * @param creature the creature
		 * @return true while it cannot be released
		 */
		default boolean locked(Creature creature)
		{
			return false;
		}

		/**
		 * Reports whether an egg is promised in a trade.
		 *
		 * @param egg the egg
		 * @return true while it cannot hatch
		 */
		default boolean locked(Egg egg)
		{
			return false;
		}

		/** @return creature slots, in the team and storage together, that open trades will need */
		default int reservedCreatureSlots()
		{
			return 0;
		}

		/** @return egg slots that open trades will need */
		default int reservedEggSlots()
		{
			return 0;
		}
	}

	private static final TradeLocks NO_LOCKS = new TradeLocks()
	{
	};

	private final SpeciesTable species;
	private final EvolutionTable evolutions;
	private final ChampionTable champions;
	private final Random random;
	private final LongSupplier clock;
	private final Consumer<Notice> notices;
	private SaveData data = SaveData.fresh();
	private List<LogEntry> log;
	private long xpCarry;
	private boolean dirty;
	private String cityId;
	private TradeLocks tradeLocks = NO_LOCKS;
	private Supplier<TodayView> today = () -> TodayView.NONE;
	private final TaskCalendar taskCalendar = new TaskCalendar();
	private TaskBoard board;
	private Milestones milestoneTable = Milestones.NONE;
	private boolean milestonesDue;
	private boolean walking;
	private EliteTable elite = EliteTable.empty();
	private EliteRun run;
	private boolean atGuild;
	private boolean bankNear;

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
		this(species, EvolutionTable.empty(), ChampionTable.empty(), random, clock, notices);
	}

	/**
	 * Creates a game with a fresh save, evolution lines and City Champions.
	 *
	 * @param species every catchable species
	 * @param evolutions which species grow into which
	 * @param champions the City Champions
	 * @param random the source of every roll
	 * @param clock wall-clock time in milliseconds
	 * @param notices receives each line the player should be told, with whose head it belongs over
	 */
	GoGame(SpeciesTable species, EvolutionTable evolutions, ChampionTable champions, Random random,
		LongSupplier clock, Consumer<Notice> notices)
	{
		this.species = species;
		this.evolutions = evolutions;
		this.champions = champions;
		this.random = random;
		this.clock = clock;
		this.notices = notices;
	}

	/**
	 * Sets where the panel's Today section comes from.
	 *
	 * @param source gives what the Today section shows right now
	 */
	void today(Supplier<TodayView> source)
	{
		this.today = source;
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
		run = null;
		milestonesDue = true;
		settleMilestones();
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
		if (gained > 0)
			payTasks(startTasks().gainedXp(gained));

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
		if (jar != null && random.nextDouble() < CatchRules.specialtyDropChance(gained))
		{
			data.addJars(jar, 1);
			dirty = true;
			tell(foundJars(jar, 1, skillName(skill)), Tone.FIND, jar);
		}

		rollSkillItems(skill, gained);
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

		payTasks(startTasks().walked(tiles));
		data.stepProgress += tiles;
		dirty = true;
		while (data.stepProgress >= TILES_PER_JAR)
		{
			data.stepProgress -= TILES_PER_JAR;
			data.addJars(JarType.BRONZE, 1);
			tell(foundJars(JarType.BRONZE, 1, null), Tone.FIND, JarType.BRONZE);
		}

		walking = true;
		Iterator<Egg> carried = data.eggs.iterator();
		while (carried.hasNext())
		{
			Egg egg = carried.next();
			boolean wasReady = egg.tilesLeft <= 0;
			egg.tilesLeft = Math.max(0, egg.tilesLeft - tiles);
			if (egg.tilesLeft > 0)
				continue;

			if (tradeLocks.locked(egg))
				continue;

			if (hatch(egg))
				carried.remove();
			else if (!wasReady)
				tell("An egg is ready, but storage is full", Tone.BAD);
		}

		walking = false;
		settleMilestones();
	}

	/**
	 * Records where the player is standing, so the game knows which City Champion can be challenged.
	 *
	 * @param x world x
	 * @param y world y
	 * @param plane world plane
	 * @return true when the player walked into or out of a champion's city, so the panel needs a refresh
	 */
	boolean moveTo(int x, int y, int plane)
	{
		boolean guild = elite.contains(x, y, plane);
		boolean guildChanged = guild != atGuild;
		atGuild = guild;
		Champion here = champions.at(x, y, plane);
		String id = here == null ? null : here.id();
		if (id == null ? cityId == null : id.equals(cityId))
			return guildChanged;

		cityId = id;
		return true;
	}

	/**
	 * Tells the game whether a bank is near the player, so the team may be changed there.
	 *
	 * @param near true when a banker, bank booth or bank chest is within {@link BankSpots#NEAR_TILES} tiles
	 */
	void setBankNear(boolean near)
	{
		bankNear = near;
	}

	/** @return whether the team may be changed here; otherwise the player is told where to go */
	private boolean mayChangeTeam()
	{
		if (TeamSwapRule.allowed(cityId != null, bankNear))
			return true;

		tell(TeamSwapRule.REFUSAL, Tone.BAD);
		return false;
	}

	/**
	 * Challenges a City Champion with the whole team. Creatures too tired to battle sit out.
	 *
	 * @param championId the champion's id
	 * @return the battle, or null when no battle could start
	 */
	TeamBattleResult challenge(String championId)
	{
		if (eliteBusy())
			return null;

		Champion champion = champions.byId(championId);
		if (champion == null)
			return null;

		if (!champion.id().equals(cityId))
		{
			tell("Go to " + champion.city() + " to challenge its champion", Tone.BAD);
			return null;
		}

		long now = clock.getAsLong();
		List<Creature> fighters = new ArrayList<>();
		List<Combatant> mine = new ArrayList<>();
		for (Creature creature : data.team)
		{
			Species found = species.find(creature.species);
			double health = creature.hpFractionAt(now);
			if (found == null || health < MIN_BATTLE_HP)
				continue;

			fighters.add(creature);
			mine.add(new Combatant(found, nameOf(creature), creature.level(), health, creature.rolls())
				.gilded(creature.gilded)
				.npc(creature.npcId));
		}

		if (mine.isEmpty())
		{
			tell(data.team.isEmpty() ? "Catch a creature first" : "Your team is too tired", Tone.BAD);
			return null;
		}

		List<Combatant> theirs = new ArrayList<>();
		for (Champion.Member member : champion.team())
			theirs.add(new Combatant(species.find(member.species()), member.level(), 1.0));

		TeamBattleResult fought = BattleSim.teamFight(mine, theirs, random);
		for (int i = 0; i < fighters.size(); i++)
		{
			Creature fighter = fighters.get(i);
			fighter.hpFraction = mine.get(i).hpFraction();
			fighter.hpStampMs = now;
		}

		dirty = true;
		List<BattleLine> lines = new ArrayList<>();
		lines.add(new BattleLine("Challenging the " + champion.title() + "!", Tone.INFO, true));
		lines.addAll(fought.lines());
		if (fought.won())
		{
			lines.add(new BattleLine("You beat the " + champion.title() + "!", Tone.GOOD, true));
			rewardChampionWin(champion);
			payTasks(startTasks().battleWon());
			payTasks(startTasks().championBeaten());
		}
		else
		{
			lines.add(new BattleLine("The " + champion.title() + " won. Rest and try again", Tone.BAD, true));
		}

		return new TeamBattleResult(fought.won(), lines, fought.duels())
			.withLeadFirst(fighters.get(0) == data.lead());
	}

	/** Rewards an OSRS level-up with Bronze jars. */
	void onLevelUp()
	{
		data.addJars(JarType.BRONZE, JARS_PER_LEVEL_UP);
		dirty = true;
		tell(foundJars(JarType.BRONZE, JARS_PER_LEVEL_UP, "level up"), Tone.FIND, JarType.BRONZE);
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
		return tryCatch(entry, jar, false);
	}

	/**
	 * Throws a chosen jar at a wild creature that may lie knocked out.
	 *
	 * @param entry the wild creature
	 * @param jar the jar to throw, or null when the player has none
	 * @param knockedOut whether the creature lies knocked out, which makes it easier to catch
	 * @return what happened, see {@link #tryCatch(WildEntry, JarType)}
	 */
	CatchOutcome tryCatch(WildEntry entry, JarType jar, boolean knockedOut)
	{
		return throwJar(entry, jar, () -> CatchRules.chance(jar, entry.combatLevel(), entry.weakened(),
			entry.traits(), knockedOut));
	}

	/**
	 * Throws a jar in the middle of a battle, at a creature that is still standing: the chance grows with the health
	 * it has lost ({@link CatchRules#inBattle}). The jar, the room and the trade locks are checked as for any throw.
	 *
	 * @param entry the wild creature
	 * @param jar the jar to throw, or null for the one {@link #jarToThrow()} picks
	 * @param healthLeft the share of full health the creature has left
	 * @return what happened, see {@link #tryCatch(WildEntry, JarType)}
	 */
	private CatchOutcome tryCatchInBattle(WildEntry entry, JarType jar, double healthLeft)
	{
		JarType use = jar == null ? jarToThrow() : jar;
		return throwJar(entry, use, () -> CatchRules.inBattle(use, entry.combatLevel(), entry.traits(), healthLeft));
	}

	private CatchOutcome throwJar(WildEntry entry, JarType jar, DoubleSupplier chance)
	{
		String name = entry.species().name();
		if (jar == null || data.jarCount(jar) <= 0)
		{
			tell("No jars left", Tone.BAD);
			return CatchOutcome.NO_JARS;
		}

		if (!hasRoomForCreature())
		{
			tell("Storage is full", Tone.BAD);
			return CatchOutcome.FULL;
		}

		data.addJars(jar, -1);
		dirty = true;
		if (random.nextDouble() >= chance.getAsDouble())
		{
			if (random.nextDouble() < CatchRules.FLEE_CHANCE)
			{
				tell(entry, "Broke free and ran away!", Tone.BAD);
				return CatchOutcome.FLED;
			}

			tell(entry, "Broke free!", Tone.BAD);
			return CatchOutcome.BROKE_FREE;
		}

		Creature caught = new Creature(entry.species().key(), entry.level(), clock.getAsLong());
		caught.gilded = entry.gilded();
		caught.npcId = entry.npcId();
		caught.setRolls(StatRolls.roll(random));
		keep(caught);
		recordOwned(caught);
		if (caught.gilded)
			tell(entry, "Caught gilded " + name + " (Lv " + entry.level() + ")!", Tone.FIND);
		else
			tell(entry, "Caught " + name + " (Lv " + entry.level() + ")!", Tone.GOOD);

		String rating = ratingNote(caught);
		if (!rating.isEmpty())
			tell(entry, rating, Tone.FIND);

		addTrainerXp(CatchRules.catchXp(entry.level()));
		payCatch(entry);
		settleMilestones();
		return CatchOutcome.CAUGHT;
	}

	/**
	 * Sends the team into a battle against a wild creature, played to the end without the player's choices. It is
	 * the engine of a steered battle ({@link #startBattle}) answered by rule: every turn attacks, and when a
	 * creature faints the next one that can fight steps in for free. The battle is lost only when the whole team is
	 * down. The team fights in order and the creatures too tired to battle sit out, so when the lead is too tired
	 * the first rested creature opens. Winning, health, the daily tasks and the {@code battled} mark are settled by
	 * {@link #act} exactly as for a steered battle.
	 *
	 * @param entry the wild creature
	 * @return the battle, or null when no battle could start
	 */
	BattleResult battle(WildEntry entry)
	{
		BattleSession session = startAutoBattle(entry);
		if (session == null)
			return null;

		List<BattleLine> log = new ArrayList<>();
		List<TurnResult> turns = new ArrayList<>();
		log.add(session.opening());
		while (!session.over())
		{
			BattleState state = session.state();
			BattleChoice next = state.needsSwitch() ? BattleChoice.switchTo(state.firstAlive())
				: BattleChoice.attack();
			TurnResult turn = act(session, next);
			if (turn == null)
				break;

			turns.add(turn);
			log.addAll(turn.lines());
		}

		BattleState state = session.state();
		boolean won = state.ended() == TurnResult.Status.WON;
		log.add(BattleSim.closing(state.mine(), state.foe(), won));
		return new BattleResult(won, log, state.mine().hpFraction(), state.totalAttacks(), turns,
			session.fighter(0), session.fighters().get(0) == data.lead());
	}

	private BattleSession startAutoBattle(WildEntry entry)
	{
		if (eliteBusy())
			return null;

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

		List<Creature> fighters = new ArrayList<>();
		List<Combatant> mine = restedTeam(clock.getAsLong(), fighters);
		if (mine.isEmpty())
		{
			tell(data.team.size() > 1 ? "Your team is too tired" : nameOf(lead) + " is too tired", Tone.BAD);
			return null;
		}

		Combatant foe = new Combatant(entry.species(), entry.level(), 1.0);
		BattleState state = new BattleState(mine, Collections.singletonList(foe), random, BattleState.Mode.WILD);
		return new BattleSession(state, fighters, entry, null, BattleSim.opening(mine.get(0), foe));
	}

	/**
	 * Starts a battle against a wild creature that the player steers turn by turn. The lead fights first and the
	 * rest of the team, the ones rested enough to battle, can be switched in. Nothing is paid until the battle is
	 * over ({@link #act}). The battle does not change the creature until then; {@link #forfeit} ends one that
	 * cannot go on.
	 *
	 * @param entry the wild creature
	 * @return the battle, or null when no battle could start
	 */
	BattleSession startBattle(WildEntry entry)
	{
		if (eliteBusy())
			return null;

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
		if (lead.hpFractionAt(now) < MIN_BATTLE_HP)
		{
			tell(nameOf(lead) + " is too tired", Tone.BAD);
			return null;
		}

		List<Creature> fighters = new ArrayList<>();
		List<Combatant> mine = restedTeam(now, fighters);
		Combatant foe = new Combatant(entry.species(), entry.level(), 1.0);
		BattleState state = new BattleState(mine, Collections.singletonList(foe), random, BattleState.Mode.WILD,
			(jar, healthLeft) -> tryCatchInBattle(entry, jar, healthLeft));
		return new BattleSession(state, fighters, entry, null, BattleSim.opening(mine.get(0), foe));
	}

	/**
	 * Starts a battle against a City Champion that the player steers turn by turn. The team fights in order, the
	 * creatures too tired to battle sit out, and a fainted creature must be replaced.
	 *
	 * @param championId the champion's id
	 * @return the battle, or null when no battle could start
	 */
	BattleSession startChallenge(String championId)
	{
		if (eliteBusy())
			return null;

		Champion champion = champions.byId(championId);
		if (champion == null)
			return null;

		if (!champion.id().equals(cityId))
		{
			tell("Go to " + champion.city() + " to challenge its champion", Tone.BAD);
			return null;
		}

		List<Creature> fighters = new ArrayList<>();
		List<Combatant> mine = restedTeam(clock.getAsLong(), fighters);
		if (mine.isEmpty())
		{
			tell(data.team.isEmpty() ? "Catch a creature first" : "Your team is too tired", Tone.BAD);
			return null;
		}

		List<Combatant> theirs = new ArrayList<>();
		for (Champion.Member member : champion.team())
			theirs.add(new Combatant(species.find(member.species()), member.level(), 1.0));

		BattleState state = new BattleState(mine, theirs, random, BattleState.Mode.CHAMPION);
		BattleLine opening = new BattleLine("Challenging the " + champion.title() + "!", Tone.INFO, true);
		return new BattleSession(state, fighters, null, champion, opening);
	}

	/**
	 * Builds the fighters of a steered battle: the team in order, the creatures too tired to battle left out.
	 *
	 * @param now the time, for the health each creature has recovered
	 * @param fighters receives the saved creature behind each fighter, in the same order
	 * @return the fighters
	 */
	private List<Combatant> restedTeam(long now, List<Creature> fighters)
	{
		List<Combatant> mine = new ArrayList<>();
		for (Creature creature : data.team)
		{
			Species found = species.find(creature.species);
			double health = creature.hpFractionAt(now);
			if (found == null || health < MIN_BATTLE_HP)
				continue;

			fighters.add(creature);
			mine.add(new Combatant(found, nameOf(creature), creature.level(), health, creature.rolls())
				.gilded(creature.gilded)
				.npc(creature.npcId));
		}

		return mine;
	}

	/**
	 * Plays one turn of a battle the player steers. The health of every creature that lost some is written to the
	 * saved creature after each turn. When the turn ends the battle the rewards are paid as for an automatic
	 * battle: a win over a wild creature through {@code rewardWin}, a win over a champion through
	 * {@code rewardChampionWin}, and the task hooks for both; a catch goes through the usual jar, storage and trade
	 * lock checks. A choice the battle does not accept now changes nothing.
	 *
	 * @param session the battle
	 * @param choice what the player does
	 * @return what the turn did, or null when the battle is over already or the choice was not accepted
	 */
	TurnResult act(BattleSession session, BattleChoice choice)
	{
		if (session.over())
			return null;

		TurnResult turn;
		try
		{
			turn = session.state().step(choice);
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}

		writeHealth(session);
		if (turn.status().over())
			settle(session, turn.status());

		return turn;
	}

	/**
	 * Ends a battle the player steers because the player left it: logging out, hopping, walking away or the creature
	 * going away. It counts as running.
	 *
	 * @param session the battle
	 * @return the turn that ended it, or null when the battle was over already
	 */
	TurnResult forfeit(BattleSession session)
	{
		return act(session, BattleChoice.run());
	}

	/**
	 * The lines that close a battle the player steers, to show after the turn that ended it.
	 *
	 * @param session the battle
	 * @param turn the turn that ended it
	 * @return the closing lines; none when the turn did not end the battle, or the player ran or caught
	 */
	List<BattleLine> closingLines(BattleSession session, TurnResult turn)
	{
		TurnResult.Status status = turn.status();
		if (status != TurnResult.Status.WON && status != TurnResult.Status.LOST)
			return Collections.emptyList();

		boolean won = status == TurnResult.Status.WON;
		if (session.wild())
		{
			BattleState state = session.state();
			return Collections.singletonList(BattleSim.closing(state.mine(), state.foe(), won));
		}

		String title = session.champion().title();
		if (won)
			return Collections.singletonList(new BattleLine("You beat the " + title + "!", Tone.GOOD, true));

		return Collections.singletonList(new BattleLine("The " + title + " won. Rest and try again", Tone.BAD,
			true));
	}

	/**
	 * Describes a steered battle that waits for a choice, for the panel and the on-screen prompt.
	 *
	 * @param session the battle
	 * @param secondsLeft the whole seconds left to choose
	 * @param timeLeft the share of the time left, from 0 to 1
	 * @return the view
	 */
	BattleView battleView(BattleSession session, int secondsLeft, double timeLeft)
	{
		BattleState state = session.state();
		List<BattleView.Option> options = new ArrayList<>();
		for (int place : state.switchTargets())
		{
			Combatant one = state.team().get(place);
			options.add(new BattleView.Option(place, one.name(), one.level(), one.hpFraction()));
		}

		JarType jar = jarToThrow();
		boolean canCatch = session.wild() && jar != null && !state.needsSwitch();
		return new BattleView(state.mine(), state.foe(), state.needsSwitch(), canCatch, jar,
			jar == null ? 0 : data.jarCount(jar), secondsLeft, timeLeft, options);
	}

	private void writeHealth(BattleSession session)
	{
		long now = clock.getAsLong();
		List<Creature> fighters = session.fighters();
		for (int i = 0; i < fighters.size(); i++)
		{
			if (!session.changed(i))
				continue;

			Creature fighter = fighters.get(i);
			fighter.hpFraction = session.fighter(i).hpFraction();
			fighter.hpStampMs = now;
			session.written(i);
			dirty = true;
		}
	}

	private void settle(BattleSession session, TurnResult.Status status)
	{
		BattleState state = session.state();
		if (session.wild())
		{
			WildEntry entry = session.entry();
			if (status == TurnResult.Status.WON || status == TurnResult.Status.LOST
				|| status == TurnResult.Status.RAN)
				entry.markBattled(status == TurnResult.Status.WON);

			if (status == TurnResult.Status.WON)
			{
				Creature fighter = session.fighters().get(state.myIndex());
				rewardWin(fighter, species.find(fighter.species), entry);
				payTasks(startTasks().battleWon());
			}

			dirty = true;
			return;
		}

		if (status == TurnResult.Status.WON)
		{
			rewardChampionWin(session.champion());
			payTasks(startTasks().battleWon());
			payTasks(startTasks().championBeaten());
		}

		dirty = true;
	}

	/**
	 * Makes a team member the lead.
	 *
	 * @param teamIndex position in the team
	 */
	void setLead(int teamIndex)
	{
		if (eliteBusy())
			return;

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
		if (eliteBusy())
			return;

		if (!mayChangeTeam())
			return;

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
		if (eliteBusy())
			return;

		if (!mayChangeTeam())
			return;

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

		if (tradeLocks.locked(data.storage.get(storageIndex)))
		{
			tell(nameOf(data.storage.get(storageIndex)) + " is promised in a trade", Tone.BAD);
			return;
		}

		Creature released = data.storage.remove(storageIndex);
		dirty = true;
		tell("Released " + nameOf(released), Tone.INFO);
	}

	/**
	 * Gives a creature a nickname, or takes it away. Nicknames stay on this computer.
	 *
	 * @param creature the creature to rename
	 * @param raw what the player typed; an empty name goes back to the species name
	 */
	void rename(Creature creature, String raw)
	{
		if (creature == null || !owns(creature))
			return;

		String typed = raw == null ? "" : raw.trim();
		String cleaned = Nickname.clean(typed);
		if (!typed.isEmpty() && cleaned == null)
		{
			tell("Names are 1 to " + Nickname.MAX_LENGTH + " letters, numbers, spaces or -", Tone.BAD);
			return;
		}

		String species = speciesNameOf(creature);
		String before = nameOf(creature);
		creature.nickname = cleaned;
		dirty = true;
		if (cleaned == null)
			tell(before + " is called " + species + " again", Tone.INFO);
		else
			tell("Renamed " + species + " to " + cleaned, Tone.INFO);
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

	/**
	 * Checks that a creature is still the player's, for a trade that was offered a while ago.
	 *
	 * @param creature the creature
	 * @return true when it is on the team or in storage
	 */
	boolean owns(Creature creature)
	{
		return data.team.contains(creature) || data.storage.contains(creature);
	}

	/**
	 * Checks that an egg is still the player's.
	 *
	 * @param egg the egg
	 * @return true when the player still carries it
	 */
	boolean owns(Egg egg)
	{
		return data.eggs.contains(egg);
	}

	/**
	 * Connects the game to the trades that hold things back.
	 *
	 * @param locks the open trades, or null for none
	 */
	void tradeLocks(TradeLocks locks)
	{
		tradeLocks = locks == null ? NO_LOCKS : locks;
	}

	/** @return whether the team or the storage can take one more creature, past the room trades will need */
	boolean hasRoomForCreature()
	{
		int free = SaveData.TEAM_SIZE - data.team.size() + SaveData.STORAGE_SIZE - data.storage.size();
		return free > tradeLocks.reservedCreatureSlots();
	}

	/** @return whether the player can carry one more egg, past the room trades will need */
	boolean hasRoomForEgg()
	{
		return data.eggs.size() + tradeLocks.reservedEggSlots() < SaveData.MAX_EGGS;
	}

	/**
	 * Takes a creature out of the save, for a trade.
	 *
	 * @param creature the creature to give away
	 */
	void giveAway(Creature creature)
	{
		if (data.team.remove(creature) || data.storage.remove(creature))
			dirty = true;
	}

	/**
	 * Takes an egg out of the save, for a trade.
	 *
	 * @param egg the egg to give away
	 */
	void giveAway(Egg egg)
	{
		if (data.eggs.remove(egg))
		{
			dirty = true;
			milestonesDue = true;
		}
	}

	/**
	 * Adds a creature that came from a trade, to the team when it has room and otherwise to storage. It counts in
	 * the Creature Log like a catch.
	 *
	 * @param creature the creature received
	 * @return false, changing nothing, when the team and the storage are both full
	 */
	boolean receive(Creature creature)
	{
		if (!hasRoomForCreature())
			return false;

		keep(creature);
		recordOwned(creature);
		settleMilestones();
		return true;
	}

	/**
	 * Adds an egg that came from a trade.
	 *
	 * @param egg the egg received
	 * @return false, changing nothing, when the player already carries {@link SaveData#MAX_EGGS} eggs
	 */
	boolean receive(Egg egg)
	{
		if (!hasRoomForEgg())
			return false;

		data.eggs.add(egg);
		dirty = true;
		return true;
	}

	/** @return a copy of everything the side panel shows, safe to hand to the UI thread */
	PanelState snapshot()
	{
		long now = clock.getAsLong();
		Map<JarType, Integer> jars = new EnumMap<>(JarType.class);
		for (JarType type : JarType.values())
			jars.put(type, data.jarCount(type));

		List<EggView> eggs = new ArrayList<>();
		for (Egg egg : data.eggs)
			eggs.add(new EggView(egg.rarityTier(), egg.tilesLeft, egg.tilesTotal));

		freezeTeam(now);
		TaskView tasks = startTasks().view(now, bestUnlockedJar());
		return new PanelState(trainerLevel(), XpRules.trainerProgress(data.trainerXp), data.battlesWon, jars,
			selectedJar(), data.stepProgress, views(data.team, now), views(data.storage, now), log(),
			knownCaughtSpecies(), data.seen.size(), eggs, championViews(), today.get(), tasks, itemCounts())
			.withGoals(milestoneTable.progress(data), eliteView());
	}

	/** @return how many caught species the species list still knows, so removed species do not inflate the total */
	private int knownCaughtSpecies()
	{
		int known = 0;
		for (String key : data.caught.keySet())
		{
			if (species.find(key) != null)
				known++;
		}

		return known;
	}

	private List<ChampionView> championViews()
	{
		List<ChampionView> views = new ArrayList<>();
		for (Champion champion : champions.all())
		{
			List<String> team = new ArrayList<>();
			for (Champion.Member member : champion.team())
				team.add(species.find(member.species()).name() + " " + member.level());

			String reward = champion.rewardJars() + " " + champion.rewardJar().label() + " jars";
			views.add(new ChampionView(champion.id(), champion.city(), String.join(", ", team), reward,
				data.sigils.contains(champion.id()), champion.id().equals(cityId), champion.rewardJar()));
		}

		return views;
	}

	/**
	 * Records that the player owns a creature of a species, for the Creature Log.
	 *
	 * @param creature the creature just caught, hatched or evolved
	 */
	private void recordOwned(Creature creature)
	{
		data.caught.merge(creature.species, 1, Integer::sum);
		data.seen.add(creature.species);
		if (creature.gilded)
			data.gilded.add(creature.species);

		log = null;
		dirty = true;
		milestonesDue = true;
	}

	/**
	 * Hatches an egg into a level 1 creature of a random species of the egg's rarity.
	 *
	 * @param egg the egg whose walking is done
	 * @return true when the egg is used up; false when there is no room, so it waits
	 */
	private boolean hatch(Egg egg)
	{
		if (!hasRoomForCreature())
			return false;

		List<Species> pool = new ArrayList<>();
		for (Species candidate : species.all())
		{
			if (candidate.rarity() == egg.rarityTier())
				pool.add(candidate);
		}

		if (pool.isEmpty())
			return true;

		Species born = pool.get(random.nextInt(pool.size()));
		Creature creature = new Creature(born.key(), 1, clock.getAsLong());
		creature.gilded = random.nextInt(gildedOneIn()) == 0;
		creature.setRolls(StatRolls.roll(random));
		keep(creature);
		recordOwned(creature);
		tell("Egg hatched: " + (creature.gilded ? "gilded " : "") + born.name() + "!", Tone.FIND);
		String rating = ratingNote(creature);
		if (!rating.isEmpty())
			tell(rating, Tone.FIND);

		payTasks(startTasks().hatched());
		return true;
	}

	/**
	 * Turns a creature into the next species of its line for every evolution level it has reached.
	 *
	 * @param creature the creature that just gained XP
	 */
	private void evolveIfReady(Creature creature)
	{
		EvolutionTable.Step step = evolutions.next(creature.species);
		for (int i = 0; i < MAX_EVOLUTIONS_AT_ONCE && step != null && creature.level() >= step.level(); i++)
		{
			Species grown = species.find(step.to());
			String before = nameOf(creature);
			creature.species = grown.key();
			creature.npcId = 0;
			recordOwned(creature);
			tell(before + " evolved into " + grown.name() + "!", Tone.GOOD);
			step = evolutions.next(creature.species);
		}

		settleMilestones();
	}

	/**
	 * Pays out a win against a City Champion: XP for the team every time, and the sigil, jars and trainer XP the
	 * first time.
	 *
	 * @param champion the champion that was beaten
	 */
	private void rewardChampionWin(Champion champion)
	{
		data.battlesWon++;
		boolean first = data.sigils.add(champion.id());
		long creatureXp = 0;
		for (Champion.Member member : champion.team())
			creatureXp += 20L + 5L * member.level();

		shareXp(first ? creatureXp : creatureXp / 2);
		if (!first)
			return;

		tell("Sigil of " + champion.city() + " earned!", Tone.FIND);
		data.addJars(champion.rewardJar(), champion.rewardJars());
		tell(foundJars(champion.rewardJar(), champion.rewardJars(), "champion"), Tone.FIND,
			champion.rewardJar());
		addTrainerXp(50L + 10L * champion.topLevel());
		giveItems(ItemType.LARGE_SALVE, ItemRules.CHAMPION_LARGE_SALVES, "champion");
		giveItems(ItemType.REVIVE, ItemRules.CHAMPION_REVIVES, "champion");
	}

	/**
	 * Sets the Creature Log milestones the game pays. Without this call there are none.
	 *
	 * @param table the milestones
	 */
	void milestones(Milestones table)
	{
		milestoneTable = table == null ? Milestones.NONE : table;
		milestonesDue = true;
	}

	/** @return whether the Gilded charm is owned, which is milestone {@link Milestones#CHARM_ID} being paid */
	boolean hasGildedCharm()
	{
		return data.milestones.contains(Milestones.CHARM_ID);
	}

	/** @return how rare a gilded creature is: one in this many, 500 or half of it with the Gilded charm */
	int gildedOneIn()
	{
		return gildedOneIn(WildTracker.GILDED_ONE_IN);
	}

	/**
	 * Applies the Gilded charm to odds that may already carry an event boost.
	 *
	 * @param base the odds without the charm: one in this many
	 * @return {@code base}, or half of it with the charm
	 */
	int gildedOneIn(int base)
	{
		return hasGildedCharm() ? Math.max(1, base / 2) : base;
	}

	/**
	 * Pays the milestones that are reached and not yet paid, unless the egg list is being walked over. A reward with
	 * an egg waits for a free egg slot and is tried again at the next chance.
	 */
	private void settleMilestones()
	{
		if (milestonesDue && !walking)
			payMilestones();
	}

	/** Pays every reached, unpaid milestone whose reward fits and tells the player. */
	private void payMilestones()
	{
		milestonesDue = false;
		for (String id : milestoneTable.reached(data))
		{
			if (data.milestones.contains(id))
				continue;

			Milestones.Milestone milestone = milestoneTable.byId(id);
			Milestones.Reward reward = milestone.reward();
			if (reward.egg() != null && !hasRoomForEgg())
			{
				milestonesDue = true;
				continue;
			}

			data.milestones.add(id);
			dirty = true;
			List<String> parts = new ArrayList<>();
			if (reward.jars() > 0)
			{
				data.addJars(reward.jar(), reward.jars());
				parts.add("+" + reward.jars() + " " + reward.jar().label() + " jars");
			}

			if (reward.egg() != null)
			{
				data.eggs.add(new Egg(reward.egg()));
				parts.add("+1 " + reward.egg().label() + " egg");
			}

			if (reward.charm())
				parts.add("Gilded charm: gilded odds doubled");

			String line = "Milestone: " + milestone.label() + "! " + String.join(", ", parts);
			if (reward.jar() == null)
				tell(line, Tone.FIND);
			else
				tell(line, Tone.FIND, reward.jar());
		}
	}

	/**
	 * Sets the Elite challenge. Without this call there is none.
	 *
	 * @param table the Elite teams and rewards
	 */
	void elite(EliteTable table)
	{
		elite = table == null ? EliteTable.empty() : table;
		run = null;
	}

	/** @return whether an Elite run is open, so the team's health is carried and nothing rests */
	boolean eliteOpen()
	{
		return run != null && run.open();
	}

	/**
	 * Starts an Elite run and plays its first battle, or plays the next battle of a run that is open.
	 *
	 * @return the battle, or null when the challenge cannot start; the reason is told to the player
	 */
	TeamBattleResult eliteChallenge()
	{
		if (eliteOpen())
			return eliteBattle();

		long now = clock.getAsLong();
		boolean fit = !eliteFighters(new ArrayList<>(), now, MIN_BATTLE_HP).isEmpty();
		String refusal = EliteRun.refusal(elite, data.sigils.size(), atGuild, hasRoomForEgg(), fit);
		if (refusal != null)
		{
			tell(refusal, Tone.BAD);
			return null;
		}

		for (Creature creature : data.team)
		{
			creature.hpFraction = creature.hpFractionAt(now);
			creature.hpStampMs = now;
		}

		run = new EliteRun(elite, species);
		tell("The Elite challenge begins. Health carries over and nothing rests until it ends", Tone.INFO);
		return eliteBattle();
	}

	/**
	 * Plays the next battle of the open Elite run.
	 *
	 * @return the battle, or null when no run is open
	 */
	TeamBattleResult eliteContinue()
	{
		return eliteOpen() ? eliteBattle() : null;
	}

	/** Gives up the open Elite run. The team starts resting again and the next attempt begins at trainer 1. */
	void eliteForfeit()
	{
		if (!eliteOpen())
			return;

		run.forfeit();
		run = null;
		dirty = true;
		tell("You gave up the Elite challenge. Your team rests again", Tone.INFO);
	}

	/**
	 * Tells the player that something they asked for cannot be done during an Elite run.
	 *
	 * @return true while a run is open
	 */
	private boolean eliteBusy()
	{
		if (!eliteOpen())
			return false;

		tell("Finish or give up the Elite challenge first", Tone.BAD);
		return true;
	}

	/**
	 * Lists the creatures that can fight, with the health they carry.
	 *
	 * @param fighters receives the creatures, in team order
	 * @param now the time, for the health of a creature that is resting
	 * @param minHealth the least share of full health a creature may have and still fight
	 * @return the creatures as combatants, in the same order
	 */
	private List<Combatant> eliteFighters(List<Creature> fighters, long now, double minHealth)
	{
		List<Combatant> mine = new ArrayList<>();
		for (Creature creature : data.team)
		{
			Species found = species.find(creature.species);
			double health = creature.hpFractionAt(now);
			if (found == null || health < minHealth || health <= 0.0)
				continue;

			fighters.add(creature);
			mine.add(new Combatant(found, nameOf(creature), creature.level(), health, creature.rolls())
				.gilded(creature.gilded)
				.npc(creature.npcId));
		}

		return mine;
	}

	/** Keeps the team from resting: every creature's health is stamped as of now while a run is open. */
	private void freezeTeam(long now)
	{
		if (!eliteOpen())
			return;

		for (Creature creature : data.team)
			creature.hpStampMs = now;
	}

	/**
	 * Fights the next battle of the open run and pays what it earns.
	 *
	 * @return the battle
	 */
	private TeamBattleResult eliteBattle()
	{
		long now = clock.getAsLong();
		freezeTeam(now);
		List<Creature> fighters = new ArrayList<>();
		List<Combatant> mine = eliteFighters(fighters, now, ELITE_MIN_HEALTH);
		if (mine.isEmpty())
		{
			run.forfeit();
			run = null;
			tell("No creature is left to fight. The Elite run is over", Tone.BAD);
			return null;
		}

		int number = run.stage() + 1;
		String opponent = run.opponent();
		Champion foe = elite.stages().get(run.stage());
		TeamBattleResult fought = run.fight(mine, random);
		for (int i = 0; i < fighters.size(); i++)
		{
			Creature fighter = fighters.get(i);
			fighter.hpFraction = mine.get(i).hpFraction();
			fighter.hpStampMs = now;
		}

		dirty = true;
		List<BattleLine> lines = new ArrayList<>();
		lines.add(new BattleLine("Elite challenge " + number + " of " + EliteTable.STAGES + ": " + opponent + "!",
			Tone.INFO, true));
		lines.addAll(fought.lines());
		if (fought.won())
		{
			rewardEliteWin(foe);
			boolean cleared = run.state() == EliteRun.State.CLEARED;
			lines.add(new BattleLine(cleared ? "You beat the Grand Champion!" : opponent + " beaten. Next: "
				+ run.opponent(), Tone.GOOD, true));
			if (cleared)
			{
				payEliteClear();
				run = null;
			}
		}
		else
		{
			lines.add(new BattleLine(opponent + " won. The Elite challenge is over. Rest and try again", Tone.BAD,
				true));
			run = null;
		}

		return new TeamBattleResult(fought.won(), lines, fought.duels())
			.withLeadFirst(fighters.get(0) == data.lead());
	}

	/**
	 * Pays the creature XP of one won Elite battle: full for the first clear of the challenge, half after it.
	 *
	 * @param foe the team that was beaten
	 */
	private void rewardEliteWin(Champion foe)
	{
		data.battlesWon++;
		long creatureXp = 0;
		for (Champion.Member member : foe.team())
			creatureXp += 20L + 5L * member.level();

		shareXp(data.eliteClears == 0 ? creatureXp : creatureXp / 2);
		payTasks(startTasks().battleWon());
	}

	/** Pays the clear of the whole challenge: the big reward the first time, the small one after. */
	private void payEliteClear()
	{
		boolean first = data.eliteClears == 0;
		data.eliteClears++;
		dirty = true;
		if (!first)
		{
			data.addJars(JarType.DRAGON, elite.laterDragonJars());
			tell("Elite challenge cleared again: +" + elite.laterDragonJars() + " Dragon jars", Tone.GOOD,
				JarType.DRAGON);
			return;
		}

		data.addJars(JarType.DRAGON, elite.firstDragonJars());
		for (JarType type : JarType.values())
		{
			if (type.specialty())
				data.addJars(type, elite.firstSpecialtyJars());
		}

		tell("You are the Grand Champion!", Tone.FIND);
		tell("+" + elite.firstDragonJars() + " Dragon jars, +" + elite.firstSpecialtyJars()
			+ " of every specialty jar", Tone.FIND, JarType.DRAGON);
		tell(eggOrJars(elite.firstEgg()), Tone.FIND);
	}

	/** @return what the Elite card on the Battle tab shows */
	private EliteView eliteView()
	{
		if (!elite.available())
			return EliteView.NONE;

		int sigils = data.sigils.size();
		boolean titled = data.eliteClears > 0;
		String reward = titled ? elite.laterDragonJars() + " Dragon jars"
			: elite.firstDragonJars() + " Dragon jars, " + elite.firstSpecialtyJars()
				+ " of every specialty jar, a " + elite.firstEgg().label() + " egg";
		if (eliteOpen())
			return new EliteView(EliteView.Mode.RUNNING, sigils, run.stage() + 1, EliteTable.STAGES, run.opponent(),
				run.opponentTeam(), reward, titled);

		EliteView.Mode mode = sigils < EliteRun.SIGILS_NEEDED ? EliteView.Mode.LOCKED
			: atGuild ? EliteView.Mode.HERE : EliteView.Mode.AWAY;
		return new EliteView(mode, sigils, 0, EliteTable.STAGES, null, null, reward, titled);
	}

	/**
	 * Finds the task board for the live save, making a new one when the save was replaced.
	 *
	 * @return the board
	 */
	private TaskBoard tasks()
	{
		if (board == null || !board.tracks(data.tasks))
			board = new TaskBoard(taskCalendar, data.tasks);

		return board;
	}

	/**
	 * Gets the task board ready for an event: the board for the live save, moved on to today and this week.
	 *
	 * @return the board
	 */
	private TaskBoard startTasks()
	{
		TaskBoard current = tasks();
		if (current.rollover(clock.getAsLong()))
			dirty = true;

		return current;
	}

	/**
	 * Handles a catch for the streak and the tasks: the first catch of a UTC day pays jars and may pay an egg.
	 *
	 * @param entry the creature just caught
	 */
	private void payCatch(WildEntry entry)
	{
		TaskBoard current = startTasks();
		TaskBoard.Streak streak = current.recordCatch();
		dirty = true;
		if (streak != null)
		{
			data.addJars(JarType.BRONZE, STREAK_JARS);
			String bonus = "+" + STREAK_JARS + " Bronze jars";
			if (streak.eggDue())
				bonus += ", " + eggOrJars(Rarity.UNCOMMON);

			tell("Day " + streak.days() + " streak: " + bonus, Tone.FIND);
		}

		boolean underground = entry.traits().contains(Trait.CAVE);
		payTasks(current.caught(entry.species().style(), underground));
	}

	/**
	 * Gives an egg, or jars when the player cannot carry another egg.
	 *
	 * @param rarity the egg's rarity
	 * @return the reward in words, for the line the player is told
	 */
	private String eggOrJars(Rarity rarity)
	{
		if (hasRoomForEgg())
		{
			data.eggs.add(new Egg(rarity));
			return "+1 " + rarity.label() + " egg";
		}

		JarType best = bestUnlockedJar();
		data.addJars(best, EGG_FALLBACK_JARS);
		return "+" + EGG_FALLBACK_JARS + " " + best.label() + " jars instead of an egg";
	}

	/**
	 * Pays the tasks an event just finished and tells the player. Egg rewards are only ever paid for the weekly
	 * task, which no egg hatching can finish, so the egg list is never changed while a hatch walks over it.
	 *
	 * @param done the finished tasks
	 */
	private void payTasks(List<Task> done)
	{
		for (Task task : done)
		{
			JarType best = bestUnlockedJar();
			List<String> parts = new ArrayList<>();
			if (task.jars() > 0)
			{
				data.addJars(best, task.jars());
				parts.add("+" + task.jars() + " " + best.label() + (task.jars() == 1 ? " jar" : " jars"));
			}

			if (task.egg() != null)
				parts.add(eggOrJars(task.egg()));

			if (task.trainerXp() > 0)
				parts.add("+" + task.trainerXp() + " trainer XP");

			dirty = true;
			tell((task.weekly() ? "Weekly" : "Daily") + " task done: " + String.join(", ", parts), Tone.GOOD);
			if (task.trainerXp() > 0)
				addTrainerXp(task.trainerXp());
		}
	}

	/** @return the tier jar with the highest trainer level the player has reached */
	private JarType bestUnlockedJar()
	{
		JarType best = JarType.BRONZE;
		int level = trainerLevel();
		for (JarType type : JarType.values())
		{
			if (!type.specialty() && level >= type.trainerLevel() && type.trainerLevel() >= best.trainerLevel())
				best = type;
		}

		return best;
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
			tell(nameOf(lead) + " grew to Lv " + lead.level() + "!", Tone.GOOD);

		evolveIfReady(lead);
		addTrainerXp(5L + entry.level());
		if (random.nextDouble() < JAR_DROP_CHANCE)
		{
			JarType drop = randomUnlockedJar();
			data.addJars(drop, 1);
			tell(foundJars(drop, 1, "battle win"), Tone.FIND, drop);
		}

		double eggChance = entry.roaming() ? ROAMING_EGG_CHANCE : EGG_DROP_CHANCE;
		if (!hasRoomForEgg() || random.nextDouble() >= eggChance)
		{
			rollWinItems();
			return;
		}

		Rarity rarity = entry.species().rarity();
		if (entry.roaming())
			rarity = rarity.next();

		data.eggs.add(new Egg(rarity));
		tell("+1 " + rarity.label() + " egg (battle win)", Tone.FIND);
		rollWinItems();
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

			evolveIfReady(creature);
		}
	}

	/** @return how many of each healing item the player owns, with an entry for every item */
	private Map<ItemType, Integer> itemCounts()
	{
		Map<ItemType, Integer> counts = new EnumMap<>(ItemType.class);
		for (ItemType type : ItemType.values())
			counts.put(type, data.itemCount(type));

		return counts;
	}

	/**
	 * Rolls, after an XP drop, for each healing item the skill can turn up. Every item has its own roll, which is
	 * separate from the specialty jar's.
	 *
	 * @param skill the skill's enum name
	 * @param gained OSRS XP in the drop
	 */
	private void rollSkillItems(String skill, int gained)
	{
		for (ItemType type : ItemType.forSkill(skill))
		{
			if (random.nextDouble() < ItemRules.xpChance(gained))
				giveItems(type, 1, skillName(skill));
		}
	}

	/** Rolls, after a battle win, for each healing item a win can drop. */
	private void rollWinItems()
	{
		for (ItemType type : ItemType.values())
		{
			double chance = ItemRules.winChance(type);
			if (chance > 0 && random.nextDouble() < chance)
				giveItems(type, 1, "battle win");
		}
	}

	/**
	 * Adds healing items to the save and tells the player.
	 *
	 * @param type the item
	 * @param count how many
	 * @param source what the line names in brackets, such as {@code Herblore}
	 */
	private void giveItems(ItemType type, int count, String source)
	{
		data.addItems(type, count);
		dirty = true;
		String name = count == 1 ? type.label() : type.label() + "s";
		notices.accept(Notice.playerItem("+" + count + " " + name + " (" + source + ")", Tone.FIND, type));
	}

	/**
	 * Uses a healing item on a creature outside battle. Nothing is spent when the player has none, when the creature
	 * is not the player's, or when the item would do nothing.
	 *
	 * @param creature a team member or stored creature
	 * @param type the item
	 * @return true when an item was used up
	 */
	boolean useItem(Creature creature, ItemType type)
	{
		if (creature == null || type == null || !data.team.contains(creature) && !data.storage.contains(creature))
			return false;

		if (data.itemCount(type) <= 0)
		{
			tell("No " + type.label() + " left", Tone.BAD);
			return false;
		}

		if (type.cures())
		{
			tell("Nothing to cure", Tone.BAD);
			return false;
		}

		long now = clock.getAsLong();
		double health = creature.hpFractionAt(now);
		if (!ItemRules.appliesOutsideBattle(type, health))
		{
			tell(refusal(creature, type), Tone.BAD);
			return false;
		}

		creature.hpFraction = ItemRules.healthAfter(type, health);
		creature.hpStampMs = now;
		data.addItems(type, -1);
		dirty = true;
		tell(nameOf(creature) + " is at " + Math.round(creature.hpFraction * 100) + "% health", Tone.GOOD);
		return true;
	}

	/**
	 * Words why an item cannot be used on a creature.
	 *
	 * @param creature the creature
	 * @param type the item
	 * @return the line to show
	 */
	private String refusal(Creature creature, ItemType type)
	{
		if (type.revives())
			return nameOf(creature) + " does not need reviving";

		return nameOf(creature) + " is already at full health";
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
	 * Words a jar find: {@code Found a Bronze jar} or {@code Found 3 Bronze jars}, with an optional short source.
	 *
	 * @param jar the jar found
	 * @param count how many
	 * @param source what gave them, shown in brackets when it tells the player something, or null
	 * @return the line
	 */
	static String foundJars(JarType jar, int count, String source)
	{
		String what = count == 1 ? "a " + jar.label() + " jar" : count + " " + jar.label() + " jars";
		return "Found " + what + (source == null ? "" : " (" + source + ")");
	}

	/**
	 * Tells the player about a jar they found, with the jar's picture beside the line.
	 *
	 * @param text the line
	 * @param tone its mood
	 * @param jar the jar the line is about
	 */
	private void tell(String text, Tone tone, JarType jar)
	{
		notices.accept(Notice.player(text, tone, jar));
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

	/**
	 * Names a creature for the player's lines: its nickname when it has one, otherwise its species name.
	 *
	 * @param creature the creature
	 * @return the name to show
	 */
	private String nameOf(Creature creature)
	{
		return creature.nickname != null ? creature.nickname : speciesNameOf(creature);
	}

	/**
	 * Words the rating of a creature for the line that follows a catch or hatch.
	 *
	 * @param creature the creature
	 * @return the rating and temperament, such as {@code Strong (Brave)}, or an empty string for an average creature
	 */
	private static String ratingNote(Creature creature)
	{
		StatRolls rolls = creature.rolls();
		if (rolls.rating() == StatRolls.Rating.AVERAGE)
			return "";

		return rolls.rating().label() + " (" + rolls.temperament().label() + ")";
	}

	/**
	 * @param creature a creature the player owns
	 * @return its species name as written for the player, such as {@code Monkey Guard}; the saved key when the
	 *     species is no longer in the list
	 */
	String speciesNameOf(Creature creature)
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
			views.add(new CreatureView(speciesNameOf(creature), creature.level(), XpRules.progress(creature.xp),
				creature.hpFractionAt(now), rarity, type, creature.gilded, creature.nickname,
				creature.rolls(), found == null ? 0 : found.npcIdFor(creature.npcId)));
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
				count == null ? 0 : count, data.seen.contains(entry.key()), data.gilded.contains(entry.key())));
		}

		log = Collections.unmodifiableList(entries);
		return log;
	}
}
