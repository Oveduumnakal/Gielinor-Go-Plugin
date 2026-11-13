/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
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
	/** The title for clearing the Elite challenge. */
	static final String TITLE_GRAND_CHAMPION = "GRAND_CHAMPION";

	/** The title for a complete Creature Log. */
	static final String TITLE_MASTER_COLLECTOR = "MASTER_COLLECTOR";

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

		/** @return whether any trade is open, offered or waiting to be confirmed */
		default boolean open()
		{
			return false;
		}

		/**
		 * @return whether a trade the player is part of is under way, which blocks a reset: one they offered, or one
		 *     they agreed to; an offer they have not answered does not count
		 */
		default boolean blocksReset()
		{
			return open();
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
	private Conditions conditions = Conditions.NONE;
	private ExamineTable examine = ExamineTable.empty();
	private final ChampionTable champions;
	private RematchTable rematches = RematchTable.empty();
	private final Random random;
	private final LongSupplier clock;
	private final Consumer<Notice> notices;
	private Consumer<Cue> cues = cue ->
	{
	};
	private SaveData data = SaveData.fresh();
	private List<LogEntry> log;
	private long xpCarry;
	private boolean dirty;
	private String cityId;
	private TradeLocks tradeLocks = NO_LOCKS;
	private Supplier<TodayView> today = () -> TodayView.NONE;
	private static final long DAY_MILLIS = 86_400_000L;
	private static final long HOUR_MILLIS = 3_600_000L;

	/** Dragon jars the first rematch win in a city pays, besides the gold sigil. */
	static final int REMATCH_FIRST_JARS = 5;

	/** Dragon jars a champion pays once per UTC day after its first rematch win. */
	static final int REMATCH_DAILY_JARS = 1;

	private final TaskCalendar taskCalendar = new TaskCalendar();
	private TaskBoard board;
	private Milestones milestoneTable = Milestones.NONE;
	private boolean milestonesDue;
	private boolean walking;
	private Creature bondLead;
	private int bondTiles;
	private BossTable bossTable = BossTable.empty();
	private final Set<String> bossEggsWaiting = new LinkedHashSet<>();
	private EliteTable elite = EliteTable.empty();
	private RivalTable rivals = RivalTable.empty();
	private EliteRun run;
	private boolean atGuild;
	private boolean bankNear;
	private MoveTable moves = MoveTable.empty();
	private Random starterRandom = new Random();
	private List<Creature> starters;
	private Long starterSeed;

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
	 * Sets the examine lines the Creature Log shows.
	 *
	 * @param table the wiki's examine line for each species
	 */
	void examine(ExamineTable table)
	{
		this.examine = table;
		this.log = null;
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
		settleMoves();
		log = null;
		starters = null;
		bossEggsWaiting.clear();
		xpCarry = 0;
		dirty = false;
		run = null;
		milestonesDue = true;
		settleMilestones();
	}

	/**
	 * Sets the random source of the starter rolls. It is separate from the game's own, so showing the offer never
	 * moves the rolls of a catch.
	 *
	 * @param random the source
	 */
	void starterRandom(Random random)
	{
		starterRandom = random;
	}

	/**
	 * Sets a stable seed for the starter rolls, usually one made from the account. With a seed, every roll of the
	 * offer starts from it, so the same account is offered the same three starters after a relog, a profile switch or
	 * a reset, and a pick always means the starter that was shown.
	 *
	 * @param seed the seed, or null to roll from the random source of {@link #starterRandom(Random)}
	 */
	void starterSeed(Long seed)
	{
		starterSeed = seed;
	}

	/**
	 * Gives the starters on offer. They are rolled when first asked for and kept until picked or the offer ends,
	 * so closing and opening the panel never rerolls them. With a seed from {@link #starterSeed(Long)} a roll after a
	 * load gives the same three again.
	 *
	 * @return the three starters with what they grow into; empty when no starter is offered
	 */
	List<StarterView> starterOffer()
	{
		if (!StarterRules.offered(data))
		{
			starters = null;
			return Collections.emptyList();
		}

		for (String name : StarterRules.STARTERS)
		{
			if (species.find(name) == null)
				return Collections.emptyList();
		}

		if (starters == null)
			starters = StarterRules.roll(starterSeed == null ? starterRandom : new Random(starterSeed),
				clock.getAsLong());

		List<CreatureView> shown = views(starters, clock.getAsLong());
		List<StarterView> offer = new ArrayList<>();
		for (int i = 0; i < shown.size(); i++)
			offer.add(new StarterView(shown.get(i), growsInto(starters.get(i).species)));

		return offer;
	}

	/**
	 * Takes one of the starters on offer: it joins as the lead. It is marked caught and seen in the Log, but pays
	 * no task, no streak and no trainer XP, because it is not a catch.
	 *
	 * @param index the place in {@link #starterOffer()}
	 * @return true when the starter joined; false when none is on offer or the place does not exist
	 */
	boolean pickStarter(int index)
	{
		List<StarterView> offer = starterOffer();
		if (offer.isEmpty() || index < 0 || index >= offer.size())
			return false;

		Creature picked = starters.get(index);
		starters = null;
		settleMoves(picked);
		keep(picked);
		recordOwned(picked);
		if (data.rival == null && RivalRules.counterOf(picked.species) != null)
			data.rival = new RivalProgress(RivalRules.counterOf(picked.species));

		tell(nameOf(picked) + " joined your team", Tone.GOOD);
		String rating = ratingNote(picked);
		if (!rating.isEmpty())
			tell(rating, Tone.FIND);

		settleMilestones();
		return true;
	}

	private String growsInto(String speciesKey)
	{
		List<String> steps = new ArrayList<>();
		EvolutionTable.Step step = evolutions.next(speciesKey);
		for (int i = 0; i < MAX_EVOLUTIONS_AT_ONCE * 2 && step != null; i++)
		{
			Species grown = species.find(step.to());
			if (grown == null)
				break;

			steps.add(grown.name() + " (" + step.level() + ")");
			step = evolutions.next(grown.key());
		}

		return steps.isEmpty() ? "Does not evolve" : "Grows into " + String.join(", ", steps);
	}

	/**
	 * Tells why the save cannot be reset right now.
	 *
	 * @return the reason, worded for the player, or null when a reset is allowed. A battle on screen is checked by the
	 *     plugin, which owns it
	 */
	String resetBlocker()
	{
		if (eliteOpen())
			return "Finish or forfeit the Elite run first";

		if (tradeLocks.blocksReset())
			return "Finish or cancel the trade first";

		return null;
	}

	/**
	 * Puts the save back to a fresh one: the starting jars and nothing else. Settings are not part of the save. Call
	 * {@link #resetBlocker()} first.
	 */
	void reset()
	{
		load(SaveData.fresh());
		dirty = true;
		tell("Progress reset", Tone.INFO);
	}

	/**
	 * Words what a reset would lose, for the confirm dialog.
	 *
	 * @return for example {@code 6 creatures in your team, 41 in Storage, 212 species in the Log, 5 sigils}
	 */
	String resetSummary()
	{
		return data.team.size() + plural(data.team.size(), " creature", " creatures") + " in your team, "
			+ data.storage.size() + " in Storage, "
			+ data.caught.size() + " species in the Log, "
			+ data.sigils.size() + plural(data.sigils.size(), " sigil", " sigils") + ", "
			+ data.eggs.size() + plural(data.eggs.size(), " egg", " eggs");
	}

	private static String plural(int count, String one, String many)
	{
		return count == 1 ? one : many;
	}

	/**
	 * Sets the moves creatures can learn. Without this call no creature has any, and every creature fights with its
	 * one basic attack. Call it before {@link #load(SaveData)}, which gives old creatures their moves.
	 *
	 * @param table the moves
	 */
	void moves(MoveTable table)
	{
		moves = table == null ? MoveTable.empty() : table;
		if (data != null)
			settleMoves();
	}

	/** @return the moves creatures can learn */
	MoveTable moveTable()
	{
		return moves;
	}

	/** Gives every owned creature the moves it should have and drops the ones the moves data no longer allows. */
	private void settleMoves()
	{
		for (Creature creature : data.team)
			settleMoves(creature);

		for (Creature creature : data.storage)
			settleMoves(creature);
	}

	private void settleMoves(Creature creature)
	{
		Species found = species.find(creature.species);
		if (found == null || moves.size() == 0)
			return;

		if (creature.moves == null || creature.moves.isEmpty())
			MoveLearning.start(creature, found, moves);

		MoveLearning.settle(creature, found, moves);
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
		walkWithLead(tiles);
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
		settleBossEggs();
	}

	/**
	 * Gives the lead a bond point for every {@link BondRules#TILES_PER_POINT} tiles walked. The part of a point
	 * walked so far is kept in memory only, and starts again when another creature takes the lead.
	 *
	 * @param tiles tiles walked since the last call
	 */
	private void walkWithLead(int tiles)
	{
		Creature lead = data.lead();
		if (lead != bondLead)
		{
			bondLead = lead;
			bondTiles = 0;
		}

		if (lead == null)
			return;

		bondTiles += tiles;
		int points = bondTiles / BondRules.TILES_PER_POINT;
		bondTiles -= points * BondRules.TILES_PER_POINT;
		addBond(lead, points);
	}

	/**
	 * Adds bond points to a creature, and tells the player when that earns a heart.
	 *
	 * @param creature the creature
	 * @param points the points to add; nothing happens for 0 or less
	 */
	private void addBond(Creature creature, int points)
	{
		if (creature == null || points <= 0)
			return;

		int before = BondRules.hearts(creature.bond);
		creature.bond = (int) Math.min(Integer.MAX_VALUE, (long) creature.bond + points);
		dirty = true;
		if (BondRules.hearts(creature.bond) > before)
			tell(nameOf(creature) + "'s bond grew", Tone.GOOD);
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
	 * Tells the game what the world is like, so evolutions with a condition can fire. Call it every tick; nothing
	 * happens unless something changed. A team creature that has its level and now meets the condition evolves on
	 * the spot. Creatures in Storage do not evolve.
	 *
	 * @param now whether it is night and whether the player is underground
	 * @return true when the conditions changed, so the panel needs a refresh
	 */
	boolean setConditions(Conditions now)
	{
		if (now.equals(conditions))
			return false;

		conditions = now;
		boolean evolved = false;
		for (Creature creature : new ArrayList<>(data.team))
			evolved |= evolveIfReady(creature);

		if (evolved)
		{
			dirty = true;
			cues.accept(Cue.EVOLVE);
		}

		return true;
	}

	/**
	 * Sets where the game's sound cues go. Without this call they are dropped.
	 *
	 * @param sink receives each cue as it happens; the caller decides whether and how to play it
	 */
	void setCues(Consumer<Cue> sink)
	{
		this.cues = sink;
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

		Champion champion = foe(championId);
		if (champion == null)
			return null;

		if (!champion.rival() && !champion.id().equals(cityId))
		{
			tell("Go to " + champion.city() + " to challenge its champion", Tone.BAD);
			return null;
		}

		boolean again = rematchOpen(champion);
		Champion foe = again ? rematches.rematchOf(champion) : champion;
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
				.npc(creature.npcId)
				.moves(MoveLearning.known(creature, found, moves)));
		}

		if (mine.isEmpty())
		{
			tell(data.team.isEmpty() ? "Catch a creature first" : "Your team is too tired", Tone.BAD);
			return null;
		}

		List<Combatant> theirs = new ArrayList<>();
		for (Champion.Member member : foe.team())
			theirs.add(foeOf(species.find(member.species()), member.level()));

		TeamBattleResult fought = BattleSim.teamFight(mine, theirs, random);
		for (int i = 0; i < fighters.size(); i++)
		{
			Creature fighter = fighters.get(i);
			fighter.hpFraction = mine.get(i).hpFraction();
			fighter.hpStampMs = now;
		}

		dirty = true;
		List<BattleLine> lines = new ArrayList<>();
		lines.add(openingLine(champion, again));
		lines.addAll(fought.lines());
		lines.addAll(verdictLines(champion, fought.won()));
		if (fought.won())
			payChampionWin(foe, again);

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
				cues.accept(Cue.BROKE_FREE);
				return CatchOutcome.FLED;
			}

			tell(entry, "Broke free!", Tone.BAD);
			cues.accept(Cue.BROKE_FREE);
			return CatchOutcome.BROKE_FREE;
		}

		Creature caught = new Creature(entry.species().key(), entry.level(), clock.getAsLong());
		caught.gilded = entry.gilded();
		caught.npcId = entry.npcId();
		caught.setRolls(StatRolls.roll(random));
		MoveLearning.start(caught, entry.species(), moves);
		keep(caught);
		recordOwned(caught);
		if (caught.gilded)
			tell(entry, "Caught gilded " + name + " (Lv " + entry.level() + ")!", Tone.FIND);
		else
			tell(entry, "Caught " + name + " (Lv " + entry.level() + ")!", Tone.GOOD);

		cues.accept(Cue.CATCH);
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

		Combatant foe = foeOf(entry.species(), entry.level());
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
		Combatant foe = foeOf(entry.species(), entry.level());
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

		Champion champion = foe(championId);
		if (champion == null)
			return null;

		if (!champion.rival() && !champion.id().equals(cityId))
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

		boolean again = rematchOpen(champion);
		Champion foe = again ? rematches.rematchOf(champion) : champion;
		List<Combatant> theirs = new ArrayList<>();
		for (Champion.Member member : foe.team())
			theirs.add(foeOf(species.find(member.species()), member.level()));

		BattleState state = new BattleState(mine, theirs, random, BattleState.Mode.CHAMPION);
		BattleSession session = new BattleSession(state, fighters, null, foe, openingLine(champion, again));
		return again ? session.rematch() : session;
	}

	/**
	 * Starts the player's side of a battle against a friend, played turn by turn. The teams are the ones both players
	 * sent, at full health, so nothing is read from the save but the creatures behind the fighters and how many of
	 * each item the player owns, which is copied into a budget for this battle. Nothing is paid when it ends, no
	 * health is written back, and no item is ever taken from the save.
	 *
	 * @param battle the battle shared with the friend
	 * @return the battle
	 */
	BattleSession startFriendBattle(FriendTurnBattle battle)
	{
		BattleState state = battle.state();
		List<Creature> fighters = new ArrayList<>();
		for (Creature creature : data.team)
		{
			if (species.find(creature.species) != null && fighters.size() < state.team().size())
				fighters.add(creature);
		}

		BattleLine opening = new BattleLine("Battle with " + battle.friendName() + "!", Tone.INFO, true);
		return new BattleSession(state, fighters, null, null, opening).friend(battle).friendItems(itemCounts());
	}

	/**
	 * Takes the player's choice for a turn of a battle against a friend and sends it. The turn is played when the
	 * friend's choice is in as well, and then comes out of {@link #friendTurn}. An item must be left in the budget
	 * the battle began with.
	 *
	 * @param session the battle
	 * @param choice what the player does
	 * @return false when the choice cannot be made now, which changes nothing
	 */
	boolean actFriend(BattleSession session, BattleChoice choice)
	{
		FriendTurnBattle battle = session.friendTurn();
		ItemType item = choice.kind() == BattleChoice.Kind.ITEM ? choice.itemType() : null;
		if (item != null && session.friendItemsLeft(item) <= 0)
		{
			tell("No " + item.label() + " left", Tone.BAD);
			return false;
		}

		String why = battle.refusal(choice);
		if (why != null)
		{
			if (item != null)
				tell(why, Tone.BAD);

			return false;
		}

		return battle.local(choice, clock.getAsLong());
	}

	/**
	 * Takes the next turn of a battle against a friend that has been played since the last call. An item the player
	 * chose in it is counted down in the battle's budget; the save is not touched.
	 *
	 * @param session the battle
	 * @return the turn, from the player's side, or null when none has been played
	 */
	TurnResult friendTurn(BattleSession session)
	{
		FriendTurnBattle.Played played = session.friendTurn().poll();
		if (played == null)
			return null;

		BattleChoice mine = played.mine();
		ItemType item = mine != null && mine.kind() == BattleChoice.Kind.ITEM ? mine.itemType() : null;
		if (item != null)
			session.useFriendItem(item);

		return played.result();
	}

	/**
	 * Opens the clock of the turn of a battle against a friend that waits, now that the player can see it.
	 *
	 * @param session the battle
	 */
	void friendOpened(BattleSession session)
	{
		session.friendTurn().opened(clock.getAsLong());
	}

	/**
	 * Moves the clock of a battle against a friend on, and finds out whether it ended with no result. When it did,
	 * the player is told why.
	 *
	 * @param session the battle
	 * @return what to tell the player when the friend left, stopped answering or went out of step; null while the
	 *     battle goes on, was played to the end, or was left by the player
	 */
	String friendEnded(BattleSession session)
	{
		FriendTurnBattle battle = session.friendTurn();
		battle.timeout(clock.getAsLong());
		FriendTurnBattle.End end = battle.ended();
		if (end == null || end == FriendTurnBattle.End.MINE)
			return null;

		String words = end.words(battle.friendName());
		tell(words, Tone.INFO);
		return words;
	}

	/**
	 * Tells whether a Battle against a wild creature would be accepted now: the lead exists and is rested to
	 * {@link #MIN_BATTLE_HP}. It is the same test {@link #startBattle} and {@link #battle} refuse on, with no
	 * message and no side effect, so a menu can leave the Battle entry out when it would only be refused.
	 *
	 * @param now the time, for the health each creature has recovered
	 * @return true when a battle could start
	 */
	boolean canBattle(long now)
	{
		Creature lead = data.lead();
		return lead != null && species.find(lead.species) != null && lead.hpFractionAt(now) >= MIN_BATTLE_HP;
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
				.npc(creature.npcId)
				.moves(MoveLearning.known(creature, found, moves)));
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

		ItemType item = choice.kind() == BattleChoice.Kind.ITEM ? choice.itemType() : null;
		if (item != null && data.itemCount(item) <= 0)
		{
			tell("No " + item.label() + " left", Tone.BAD);
			return null;
		}

		TurnResult turn;
		try
		{
			turn = session.state().step(choice);
		}
		catch (IllegalArgumentException e)
		{
			if (item != null)
				tell(e.getMessage(), Tone.BAD);

			return null;
		}

		if (item != null)
		{
			data.addItems(item, -1);
			dirty = true;
		}

		writeHealth(session);
		if (turn.status().over())
			settle(session, turn.status());

		return turn;
	}

	/**
	 * Ends a battle the player steers because the player left it: logging out, hopping, walking away or the creature
	 * going away. It counts as running, and in an Elite battle that ends the run: it counts as a loss.
	 *
	 * @param session the battle
	 * @return the turn that ended it, or null when the battle was over already
	 */
	TurnResult forfeit(BattleSession session)
	{
		if (session.friendTurn() != null)
		{
			session.friendTurn().leave();
			tell("You left the battle", Tone.INFO);
			return null;
		}

		TurnResult turn = act(session, BattleChoice.run());
		if (turn != null && session.isElite())
		{
			for (BattleLine line : session.verdict())
				tell(line.text(), line.tone());
		}

		return turn;
	}

	/**
	 * The lines that close a battle the player steers, to show after the turn that ended it.
	 *
	 * @param session the battle
	 * @param turn the turn that ended it
	 * @return the closing lines; none when the turn did not end the battle, or the player ran or caught (an Elite
	 * battle always closes, a run included)
	 */
	List<BattleLine> closingLines(BattleSession session, TurnResult turn)
	{
		TurnResult.Status status = turn.status();
		if (session.isElite() && status.over())
			return session.verdict();

		if (status != TurnResult.Status.WON && status != TurnResult.Status.LOST)
			return Collections.emptyList();

		boolean won = status == TurnResult.Status.WON;
		if (session.friendTurn() != null)
		{
			String name = session.friendTurn().friendName();
			return Collections.singletonList(won ? new BattleLine("You beat " + name + "!", Tone.GOOD, true)
				: new BattleLine(name + " won this one", Tone.BAD, true));
		}

		if (session.wild())
		{
			BattleState state = session.state();
			return Collections.singletonList(BattleSim.closing(state.mine(), state.foe(), won));
		}

		return verdictLines(session.champion(), won);
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
		BattleView view = new BattleView(state.mine(), state.foe(), state.needsSwitch(), canCatch, jar,
			jar == null ? 0 : data.jarCount(jar), secondsLeft, timeLeft, options, itemOptions(session));
		FriendTurnBattle friend = session.friendTurn();
		return friend == null ? view : view.againstFriend(friend.friendName(), !friend.mustChoose());
	}

	/**
	 * Lists the items the player owns that would do something in a battle right now, with their targets.
	 *
	 * @param session the battle; against a friend the count is what is left of the battle's own budget
	 * @return one option per such item, in item order
	 */
	private List<BattleView.ItemOption> itemOptions(BattleSession session)
	{
		BattleState state = session.state();
		List<BattleView.ItemOption> items = new ArrayList<>();
		for (ItemType type : ItemType.values())
		{
			int count = itemsFor(session, type);
			List<BattleView.Option> targets = new ArrayList<>();
			for (int place : state.itemTargets(type))
			{
				Combatant one = state.team().get(place);
				targets.add(new BattleView.Option(place, one.name(), one.level(), one.hpFraction()));
			}

			if (count > 0 && !targets.isEmpty())
				items.add(new BattleView.ItemOption(type, count, targets));
		}

		return items;
	}

	/**
	 * Picks the item a hotkey uses: the first owned item, in item order, that would do something, on the creature in
	 * the fight when it is a target and otherwise on the first creature it would help.
	 *
	 * @param session the battle
	 * @return the choice, or null when no owned item would do anything now
	 */
	BattleChoice quickItem(BattleSession session)
	{
		BattleState state = session.state();
		for (ItemType type : ItemType.values())
		{
			List<Integer> targets = state.itemTargets(type);
			if (itemsFor(session, type) <= 0 || targets.isEmpty())
				continue;

			return BattleChoice.item(type, targets.contains(state.myIndex()) ? state.myIndex() : targets.get(0));
		}

		return null;
	}

	/**
	 * @param session a battle
	 * @param type an item
	 * @return how many of it the player may use in the battle: the budget it began with when the battle is against a
	 *     friend, otherwise what the save holds
	 */
	private int itemsFor(BattleSession session, ItemType type)
	{
		return session.friendTurn() != null ? session.friendItemsLeft(type) : data.itemCount(type);
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
		if (session.isElite())
		{
			settleElite(session, status == TurnResult.Status.WON);
			dirty = true;
			return;
		}

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
			payChampionWin(session.champion(), session.isRematch());

		dirty = true;
	}

	/**
	 * Settles a steered Elite battle that is over. A win pays the battle and moves the run on, clearing it after the
	 * Grand Champion. A loss, or running, ends the run. The closing lines are kept on the session.
	 *
	 * @param session the battle
	 * @param won whether the player won
	 */
	private void settleElite(BattleSession session, boolean won)
	{
		List<BattleLine> lines = new ArrayList<>();
		String opponent = session.champion().city();
		if (run == null || !run.open())
		{
			session.verdict(lines);
			return;
		}

		run.record(won);
		if (won)
		{
			rewardEliteWin(session.champion());
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
			lines.add(new BattleLine(session.state().ended() == TurnResult.Status.RAN
				? "You left the battle. The Elite challenge is over. Rest and try again"
				: opponent + " won. The Elite challenge is over. Rest and try again", Tone.BAD, true));
			run = null;
		}

		session.verdict(lines);
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
	 * Checks that a creature a panel picked is still where the panel listed it. When it is not, says so and the
	 * caller does nothing.
	 *
	 * @param pick the creature as the panel showed it
	 * @return true when the pick still points at the same creature
	 */
	private boolean stillThere(TradePick pick)
	{
		if (pick.matches(snapshot()))
			return true;

		tell("That creature moved. Try again.", Tone.BAD);
		return false;
	}

	/**
	 * Makes the creature a panel picked the lead, if it is still where the panel listed it.
	 *
	 * @param pick the team member as the panel showed it
	 */
	void setLead(TradePick pick)
	{
		if (stillThere(pick))
			setLead(pick.index());
	}

	/**
	 * Moves the team member a panel picked into storage, if it is still where the panel listed it.
	 *
	 * @param pick the team member as the panel showed it
	 */
	void moveToStorage(TradePick pick)
	{
		if (stillThere(pick))
			moveToStorage(pick.index());
	}

	/**
	 * Moves the stored creature a panel picked onto the team, if it is still where the panel listed it.
	 *
	 * @param pick the stored creature as the panel showed it
	 */
	void moveToTeam(TradePick pick)
	{
		if (stillThere(pick))
			moveToTeam(pick.index());
	}

	/**
	 * Releases the stored creature a panel picked, if it is still where the panel listed it.
	 *
	 * @param pick the stored creature as the panel showed it
	 */
	void release(TradePick pick)
	{
		if (stillThere(pick))
			release(pick.index());
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
	 * Releases a stored creature for good. The lead gains creature XP for it.
	 *
	 * @param storageIndex position in storage
	 */
	void release(int storageIndex)
	{
		release(Collections.singletonList(storageIndex));
	}

	/**
	 * Releases several stored creatures for good, in any order. A creature promised in a trade and a place that does
	 * not exist are skipped. The lead gains the creature XP of every creature that went, with one line to say so.
	 *
	 * @param storageIndexes positions in storage
	 */
	void release(List<Integer> storageIndexes)
	{
		List<Integer> places = new ArrayList<>(new TreeSet<>(storageIndexes));
		Collections.reverse(places);
		long xp = 0;
		int count = 0;
		String lastName = null;
		for (int index : places)
		{
			if (index < 0 || index >= data.storage.size())
				continue;

			Creature kept = data.storage.get(index);
			if (tradeLocks.locked(kept))
			{
				tell(nameOf(kept) + " is promised in a trade", Tone.BAD);
				continue;
			}

			Species found = species.find(kept.species);
			xp += ReleaseRules.xp(kept.level(), found == null ? Rarity.COMMON : found.rarity());
			lastName = nameOf(kept);
			data.storage.remove(index);
			count++;
		}

		if (count == 0)
			return;

		dirty = true;
		String what = count == 1 ? lastName : count + " creatures";
		Creature lead = data.lead();
		if (lead == null)
		{
			tell("Released " + what, Tone.INFO);
			return;
		}

		tell(nameOf(lead) + " +" + xp + " XP (released " + what + ")", Tone.GOOD);
		trainLead(lead, xp);
	}

	/**
	 * Gives the lead creature XP the way a battle win does: it can level, learn moves and evolve.
	 *
	 * @param lead the lead creature
	 * @param xp the creature XP to add
	 */
	private void trainLead(Creature lead, long xp)
	{
		int before = lead.level();
		lead.xp = XpRules.capped(lead.xp + xp);
		if (lead.level() > before)
			tell(nameOf(lead) + " grew to Lv " + lead.level() + "!", Tone.GOOD);

		if (evolveIfReady(lead))
			cues.accept(Cue.EVOLVE);
		else if (lead.level() > before)
			cues.accept(Cue.LEVEL_UP);

		learnMoves(lead, before);
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
		int teamRoom = eliteOpen() ? 0 : SaveData.TEAM_SIZE - data.team.size();
		int free = teamRoom + SaveData.STORAGE_SIZE - data.storage.size();
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

	/** @return the name of the species a boss egg holds, or null for an ordinary egg */
	private String eggName(Egg egg)
	{
		Species fixed = egg.species == null ? null : species.find(egg.species);
		return fixed == null ? null : fixed.name();
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
			eggs.add(new EggView(egg.rarityTier(), egg.tilesLeft, egg.tilesTotal, eggName(egg)));

		freezeTeam(now);
		TaskView tasks = startTasks().view(now, bestUnlockedJar());
		return new PanelState(trainerLevel(), XpRules.trainerProgress(data.trainerXp), data.battlesWon, jars,
			selectedJar(), data.stepProgress, views(data.team, now), views(data.storage, now), log(),
			knownCaughtSpecies(), data.seen.size(), eggs, championViews(), today.get(), tasks, itemCounts())
			.withGoals(milestoneTable.progress(data), eliteView())
			.withResetSummary(resetSummary())
			.withRival(rivalView())
			.withStarters(starterOffer())
			.withTitle(shownTitle(), titles());
	}

	/** @return how many caught species count toward the Log: known ones that are not quest-only */
	private int knownCaughtSpecies()
	{
		int known = 0;
		for (String key : data.caught.keySet())
		{
			Species found = species.find(key);
			if (found != null && !found.questOnly())
				known++;
		}

		return known;
	}

	private List<ChampionView> championViews()
	{
		List<ChampionView> views = new ArrayList<>();
		for (Champion champion : champions.all())
		{
			String reward = champion.rewardJars() + " " + champion.rewardJar().label() + " jars";
			ChampionView view = new ChampionView(champion.id(), champion.city(), teamText(champion), reward,
				data.sigils.contains(champion.id()), champion.id().equals(cityId), champion.rewardJar());
			if (rematchOpen(champion))
			{
				view = view.withRematch(teamText(rematches.rematchOf(champion)),
					data.goldSigils.contains(champion.id()), rematchNote(champion));
			}

			views.add(view);
		}

		return views;
	}

	private String teamText(Champion champion)
	{
		List<String> team = new ArrayList<>();
		for (Champion.Member member : champion.team())
			team.add(species.find(member.species()).name() + " " + member.level());

		return String.join(", ", team);
	}

	/**
	 * Words when a champion's daily rematch reward can be won.
	 *
	 * @param champion the champion
	 * @return {@code Reward ready}, or {@code Reward again in 5 h} counted to the next UTC midnight
	 */
	private String rematchNote(Champion champion)
	{
		long now = clock.getAsLong();
		long today = taskCalendar.dayOf(now);
		Long paid = data.rematchDay.get(champion.id());
		if (paid == null || paid.longValue() < today)
			return "Reward ready";

		long left = (paid.longValue() + 1) * DAY_MILLIS - now;
		return "Reward again in " + Math.max(1L, (left + HOUR_MILLIS - 1) / HOUR_MILLIS) + " h";
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
	 * Hatches an egg into a level 1 creature: of the egg's own species for a boss egg, otherwise of a random species
	 * of the egg's rarity.
	 *
	 * @param egg the egg whose walking is done
	 * @return true when the egg is used up; false when there is no room, so it waits
	 */
	private boolean hatch(Egg egg)
	{
		if (!hasRoomForCreature())
			return false;

		Species born = egg.species == null ? null : species.find(egg.species);
		if (born == null)
		{
			List<Species> pool = new ArrayList<>();
			for (Species candidate : species.catchable())
			{
				if (candidate.rarity() == egg.rarityTier())
					pool.add(candidate);
			}

			if (pool.isEmpty())
				return true;

			born = pool.get(random.nextInt(pool.size()));
		}

		Creature creature = new Creature(born.key(), 1, clock.getAsLong());
		creature.gilded = random.nextInt(gildedOneIn()) == 0;
		creature.setRolls(StatRolls.roll(random));
		MoveLearning.start(creature, born, moves);
		keep(creature);
		recordOwned(creature);
		tell("Egg hatched: " + (creature.gilded ? "gilded " : "") + born.name() + "!", Tone.FIND);
		cues.accept(Cue.HATCH);
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
	 * @return whether it evolved at least once
	 */
	private boolean evolveIfReady(Creature creature)
	{
		boolean evolved = false;
		EvolutionTable.Step step = evolutions.next(creature.species, conditions);
		for (int i = 0; i < MAX_EVOLUTIONS_AT_ONCE && step != null && creature.level() >= step.level(); i++)
		{
			Species grown = species.find(step.to());
			String before = nameOf(creature);
			creature.species = grown.key();
			creature.npcId = 0;
			MoveLearning.evolved(creature, grown, moves);
			recordOwned(creature);
			tell(before + " evolved into " + grown.name() + "!", Tone.GOOD);
			evolved = true;
			step = evolutions.next(creature.species, conditions);
		}

		settleMilestones();
		return evolved;
	}

	/**
	 * Pays a won champion battle: the champion's or the rival's reward and the tasks it finishes.
	 *
	 * @param champion the champion or rival that was beaten, as fought: with the rematch team for a rematch
	 * @param rematch whether the battle was a City Champion rematch
	 */
	private void payChampionWin(Champion champion, boolean rematch)
	{
		if (champion.rival())
		{
			rewardRivalWin(champion);
			payTasks(startTasks().battleWon());
			return;
		}

		if (rematch)
			rewardRematchWin(champion);
		else
			rewardChampionWin(champion);

		payTasks(startTasks().battleWon());
		payTasks(startTasks().championBeaten());
	}

	/**
	 * Finds who a challenge is against.
	 *
	 * @param id a City Champion's id, or {@link RivalRules#ID} for the rival
	 * @return the champion; the rival with the team for the next battle; or null when there is none, in which case
	 *     the player is told why for the rival
	 */
	private Champion foe(String id)
	{
		if (!RivalRules.ID.equals(id))
			return champions.byId(id);

		if (!rivals.available())
			return null;

		int won = data.rival == null ? 0 : data.rival.won;
		if (won >= RivalRules.BATTLES)
		{
			tell(rivals.name() + " has nothing left to prove", Tone.INFO);
			return null;
		}

		if (RivalRules.unlocked(data.sigils.size(), data.eliteClears) <= won)
		{
			tell(RivalRules.requirement(won + 1) + " to meet " + rivals.name() + " again", Tone.BAD);
			return null;
		}

		List<Champion.Member> team = rivals.team(rivalStarter(), won + 1);
		if (team.isEmpty())
			return null;

		return Champion.rival(rivals.name(), won + 1, team, bestUnlockedJar(), RivalRules.jars(won + 1));
	}

	/**
	 * Fixes the rival's starter when it is first needed and gives it.
	 *
	 * @return the species key of the rival's starter
	 */
	private String rivalStarter()
	{
		if (data.rival == null)
		{
			Creature lead = data.lead();
			Species leading = lead == null ? null : species.find(lead.species);
			data.rival = new RivalProgress(RivalRules.starterAgainst(leading, species));
			dirty = true;
		}

		return data.rival.starter;
	}

	private BattleLine openingLine(Champion champion, boolean rematch)
	{
		if (champion.rival())
			return new BattleLine(champion.title() + ": \"" + rivals.before(champion.rivalBattle()) + "\"",
				Tone.INFO, true);

		return new BattleLine((rematch ? "Rematch with the " : "Challenging the ") + champion.title() + "!",
			Tone.INFO, true);
	}

	private List<BattleLine> verdictLines(Champion champion, boolean won)
	{
		String title = champion.title();
		if (champion.rival())
		{
			if (!won)
				return Collections.singletonList(new BattleLine(title + " won. Rest and try again", Tone.BAD, true));

			return Arrays.asList(new BattleLine("You beat " + title + "!", Tone.GOOD, true),
				new BattleLine(title + ": \"" + rivals.after(champion.rivalBattle()) + "\"", Tone.INFO, true));
		}

		if (won)
			return Collections.singletonList(new BattleLine("You beat the " + title + "!", Tone.GOOD, true));

		return Collections.singletonList(new BattleLine("The " + title + " won. Rest and try again", Tone.BAD, true));
	}

	/**
	 * Pays the first win of one of the rival's battles: team XP, jars of the best tier, an egg and trainer XP. A
	 * battle already won pays nothing, and none can be won out of order.
	 *
	 * @param champion the rival's team that was beaten
	 */
	private void rewardRivalWin(Champion champion)
	{
		data.battlesWon++;
		int battle = champion.rivalBattle();
		if (data.rival == null || data.rival.won >= battle)
			return;

		data.rival.won = battle;
		long creatureXp = 0;
		for (Champion.Member member : champion.team())
			creatureXp += 20L + 5L * member.level();

		shareXp(creatureXp);
		tell(champion.title() + " is beaten (" + battle + " of " + RivalRules.BATTLES + ")", Tone.FIND);
		data.addJars(champion.rewardJar(), champion.rewardJars());
		tell(foundJars(champion.rewardJar(), champion.rewardJars(), "rival"), Tone.FIND, champion.rewardJar());
		tell(eggOrJars(RivalRules.egg(battle)), Tone.FIND);
		addTrainerXp(50L + 10L * champion.topLevel());
	}

	/** @return what the rival card on the Battle tab shows */
	private RivalView rivalView()
	{
		if (!rivals.available())
			return RivalView.NONE;

		String name = rivals.name();
		int won = data.rival == null ? 0 : data.rival.won;
		if (won >= RivalRules.BATTLES)
			return new RivalView(RivalView.Mode.BEATEN, name, RivalRules.BATTLES, rivals.after(RivalRules.BATTLES),
				null, null, "Beaten. " + name + " has nothing left to prove", null);

		int battle = won + 1;
		String line = won == 0 ? null : rivals.after(won);
		if (RivalRules.unlocked(data.sigils.size(), data.eliteClears) <= won)
			return new RivalView(RivalView.Mode.LOCKED, name, battle, line, null, null,
				RivalRules.requirement(battle) + " to meet " + name + (won == 0 ? "" : " again"), null);

		List<Champion.Member> members = rivals.team(rivalStarter(), battle);
		if (members.isEmpty())
			return RivalView.NONE;

		List<String> team = new ArrayList<>();
		for (Champion.Member member : members)
			team.add(species.find(member.species()).name() + " " + member.level());

		JarType jar = bestUnlockedJar();
		String reward = RivalRules.jars(battle) + " " + jar.label() + " jars, a " + RivalRules.egg(battle).label()
			+ " egg";
		return new RivalView(RivalView.Mode.READY, name, battle, rivals.before(battle), String.join(", ", team),
			reward, "Ready to fight anywhere", jar);
	}

	/**
	 * Sets the rival. Without this call there is none.
	 *
	 * @param table the rival's name, lines and teams
	 */
	void rival(RivalTable table)
	{
		rivals = table == null ? RivalTable.empty() : table;
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
		addBond(data.lead(), BondRules.CHAMPION_POINTS);
		if (!first)
			return;

		tell("Sigil of " + champion.city() + " earned!", Tone.FIND);
		cues.accept(Cue.SIGIL);
		data.addJars(champion.rewardJar(), champion.rewardJars());
		tell(foundJars(champion.rewardJar(), champion.rewardJars(), "champion"), Tone.FIND,
			champion.rewardJar());
		addTrainerXp(50L + 10L * champion.topLevel());
		giveItems(ItemType.LARGE_SALVE, ItemRules.CHAMPION_LARGE_SALVES, "champion");
		giveItems(ItemType.REVIVE, ItemRules.CHAMPION_REVIVES, "champion");
	}

	/**
	 * Sets the champions' second teams. Without this call a champion is always fought with the first team.
	 *
	 * @param table the rematch teams
	 */
	void rematches(RematchTable table)
	{
		rematches = table == null ? RematchTable.empty() : table;
	}

	/**
	 * Tells whether a champion is fought with the second team: the Elite challenge has been cleared once and the
	 * champion has one.
	 *
	 * @param champion the champion
	 * @return true when the next fight with the champion is a rematch
	 */
	private boolean rematchOpen(Champion champion)
	{
		return data.eliteClears >= 1 && !rematches.teamOf(champion.id()).isEmpty();
	}

	/**
	 * Pays out a won rematch: creature XP every time, the gold sigil and Dragon jars for the first win in a city, and
	 * a Dragon jar once per UTC day per champion after that. A later win the same day pays half the XP only.
	 *
	 * @param foe the champion as fought, carrying the rematch team
	 */
	private void rewardRematchWin(Champion foe)
	{
		data.battlesWon++;
		long creatureXp = 0;
		for (Champion.Member member : foe.team())
			creatureXp += 20L + 5L * member.level();

		long today = taskCalendar.dayOf(clock.getAsLong());
		boolean first = data.goldSigils.add(foe.id());
		Long paid = data.rematchDay.get(foe.id());
		boolean daily = paid == null || paid.longValue() < today;
		dirty = true;
		shareXp(daily ? creatureXp : creatureXp / 2);
		if (!daily)
		{
			tell("Reward for " + foe.city() + " paid today: creature XP only", Tone.INFO);
			return;
		}

		data.rematchDay.put(foe.id(), today);
		if (first)
		{
			tell("Gold sigil of " + foe.city() + " earned!", Tone.FIND);
			cues.accept(Cue.SIGIL);
			data.addJars(JarType.DRAGON, REMATCH_FIRST_JARS);
			tell(foundJars(JarType.DRAGON, REMATCH_FIRST_JARS, "champion"), Tone.FIND, JarType.DRAGON);
			milestonesDue = true;
			settleMilestones();
			return;
		}

		data.addJars(JarType.DRAGON, REMATCH_DAILY_JARS);
		tell(foundJars(JarType.DRAGON, REMATCH_DAILY_JARS, "champion"), Tone.FIND, JarType.DRAGON);
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

	/**
	 * Sets the bosses whose kills give an egg. Without this call there are none.
	 *
	 * @param table the boss table
	 */
	void bosses(BossTable table)
	{
		bossTable = table == null ? BossTable.empty() : table;
	}

	/** @return the bosses whose kills give an egg */
	BossTable bossTable()
	{
		return bossTable;
	}

	/**
	 * Handles a boss kill the game reported in the chat. The first kill of a boss always gives its egg, later kills
	 * give it {@link BossRules#LATER_CHANCE} of the time. With a full egg bag the egg waits, one place per boss,
	 * until a slot is free; a first-kill egg is only counted as paid once it is in the bag.
	 *
	 * @param chatName the boss or raid name from the kill-count line
	 */
	void bossKilled(String chatName)
	{
		Species boss = bossTable.speciesFor(chatName);
		if (boss == null)
			return;

		boolean first = !data.bossKills.contains(boss.key());
		if (!BossRules.eggDrops(first, random))
			return;

		bossEggsWaiting.add(boss.key());
		settleBossEggs();
	}

	/** Gives the boss eggs that wait for a slot, as far as there are slots, unless the egg list is being walked. */
	private void settleBossEggs()
	{
		if (walking)
			return;

		Iterator<String> waiting = bossEggsWaiting.iterator();
		while (waiting.hasNext() && hasRoomForEgg())
		{
			Species boss = species.find(waiting.next());
			waiting.remove();
			if (boss == null)
				continue;

			data.eggs.add(new Egg(boss));
			data.bossKills.add(boss.key());
			dirty = true;
			tell("You found a " + boss.name() + " egg!", Tone.FIND);
		}
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
		boolean paid = false;
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
			paid = true;
			List<String> parts = new ArrayList<>();
			if (reward.jars() > 0)
			{
				data.addJars(reward.jar(), reward.jars());
				parts.add("+" + reward.jars() + " " + reward.jar().label() + " jars");
			}

			if (reward.specialtyJars() > 0)
			{
				for (JarType type : JarType.values())
				{
					if (type.specialty())
						data.addJars(type, reward.specialtyJars());
				}

				parts.add("+" + reward.specialtyJars() + " of every specialty jar");
			}

			if (reward.egg() != null)
			{
				data.eggs.add(new Egg(reward.egg()));
				parts.add("+1 " + reward.egg().label() + " egg");
			}

			if (reward.charm())
				parts.add("Gilded charm: gilded odds doubled");

			if (reward.title())
				parts.add("Title: Master Collector");

			String line = "Milestone: " + milestone.label() + "! " + String.join(", ", parts);
			if (reward.jar() == null)
				tell(line, Tone.FIND);
			else
				tell(line, Tone.FIND, reward.jar());
		}

		if (paid)
			cues.accept(Cue.MILESTONE);
	}

	/** @return the titles the player has earned, Grand Champion first and Master Collector second */
	List<String> titles()
	{
		List<String> earned = new ArrayList<>();
		if (data.eliteClears > 0)
			earned.add(TITLE_GRAND_CHAMPION);

		if (data.milestones.contains(Milestones.COMPLETE_ID))
			earned.add(TITLE_MASTER_COLLECTOR);

		return earned;
	}

	/** @return the title the header shows: the one picked if it is earned, else the newest earned, or null for none */
	String shownTitle()
	{
		List<String> earned = titles();
		if (earned.isEmpty())
			return null;

		if (data.title != null && earned.contains(data.title))
			return data.title;

		return earned.get(earned.size() - 1);
	}

	/**
	 * Picks which earned title the header shows. A title that is not earned is ignored.
	 *
	 * @param title {@link #TITLE_GRAND_CHAMPION} or {@link #TITLE_MASTER_COLLECTOR}
	 */
	void pickTitle(String title)
	{
		if (!titles().contains(title))
			return;

		data.title = title;
		dirty = true;
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
		if (!eliteOpen() && !eliteBegin())
			return null;

		return eliteBattle();
	}

	/**
	 * Starts an Elite run, or finds the next battle of the one that is open, as a battle the player steers turn by
	 * turn. The team keeps the health it carries and the battle is paid when it ends ({@link #act}): winning moves
	 * the run on, losing or running ends it. There is no catching, and the creatures too tired to fight sit out.
	 *
	 * @return the battle, or null when the challenge cannot start; the reason is told to the player
	 */
	BattleSession eliteSteered()
	{
		if (!eliteOpen() && !eliteBegin())
			return null;

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
		BattleState state = new BattleState(mine, run.opponents(), random, BattleState.Mode.CHAMPION);
		BattleLine opening = new BattleLine("Elite challenge " + number + " of " + EliteTable.STAGES + ": "
			+ run.opponent() + "!", Tone.INFO, true);
		return new BattleSession(state, fighters, null, run.foe(), opening).elite(number);
	}

	/**
	 * Starts a run after checking that it may start.
	 *
	 * @return false when the challenge cannot start; the reason is told to the player
	 */
	private boolean eliteBegin()
	{
		long now = clock.getAsLong();
		boolean fit = !eliteFighters(new ArrayList<>(), now, MIN_BATTLE_HP).isEmpty();
		String refusal = EliteRun.refusal(elite, data.sigils.size(), atGuild, hasRoomForEgg(), fit);
		if (refusal != null)
		{
			tell(refusal, Tone.BAD);
			return false;
		}

		for (Creature creature : data.team)
		{
			creature.hpFraction = creature.hpFractionAt(now);
			creature.hpStampMs = now;
		}

		run = new EliteRun(elite, species).withMoves(moves);
		tell("The Elite challenge begins. Health carries over and nothing rests until it ends", Tone.INFO);
		return true;
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
				.npc(creature.npcId)
				.moves(MoveLearning.known(creature, found, moves)));
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
		addBond(data.lead(), BondRules.CHAMPION_POINTS);
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

			tell("Day " + streak.days() + ": " + bonus, Tone.FIND);
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
		if (eliteOpen())
		{
			data.storage.add(creature);
			tell(nameOf(creature) + " went to storage: the Elite challenge is open, so your team stays as it is",
				Tone.INFO);
			return;
		}

		if (data.team.size() < SaveData.TEAM_SIZE)
			data.team.add(creature);
		else
			data.storage.add(creature);
	}

	private void rewardWin(Creature lead, Species leadSpecies, WildEntry entry)
	{
		data.battlesWon++;
		int before = lead.level();
		long won = BondRules.withBonus(20L + 5L * entry.level(), BondRules.hearts(lead.bond));
		lead.xp = XpRules.capped(lead.xp + won);
		if (lead == data.lead())
			addBond(lead, BondRules.WIN_POINTS);

		if (lead.level() > before)
			tell(nameOf(lead) + " grew to Lv " + lead.level() + "!", Tone.GOOD);

		if (evolveIfReady(lead))
			cues.accept(Cue.EVOLVE);
		else if (lead.level() > before)
			cues.accept(Cue.LEVEL_UP);

		learnMoves(lead, before);
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

		long[] xpBefore = new long[before.length];
		for (int i = 0; i < before.length; i++)
			xpBefore[i] = data.team.get(i).xp;

		XpRules.share(data.team, creatureXp);
		for (int i = 0; i < before.length; i++)
		{
			Creature member = data.team.get(i);
			long gained = member.xp - xpBefore[i];
			member.xp = XpRules.capped(xpBefore[i] + BondRules.withBonus(gained, BondRules.hearts(member.bond)));
		}

		dirty = true;
		Cue loudest = null;
		for (int i = 0; i < before.length; i++)
		{
			Creature creature = data.team.get(i);
			if (creature.level() > before[i])
				tell(nameOf(creature) + " grew to Lv " + creature.level() + "!", Tone.GOOD);

			if (evolveIfReady(creature))
				loudest = Cue.stronger(loudest, Cue.EVOLVE);
			else if (creature.level() > before[i])
				loudest = Cue.stronger(loudest, Cue.LEVEL_UP);

			learnMoves(creature, before[i]);
		}

		if (loudest != null)
			cues.accept(loudest);
	}

	/**
	 * Teaches a creature the moves its new levels unlock and tells the player: a move is learned while there is
	 * room, and waits for a choice once the creature knows four.
	 *
	 * @param creature the creature that gained levels
	 * @param fromLevel the level it had before
	 */
	private void learnMoves(Creature creature, int fromLevel)
	{
		Species found = species.find(creature.species);
		if (found == null || creature.level() <= fromLevel || moves.size() == 0)
			return;

		if (creature.moves == null || creature.moves.isEmpty())
		{
			MoveLearning.start(creature, found, moves);
			dirty = true;
			return;
		}

		MoveLearning.Result result = MoveLearning.levelUp(creature, found, moves, fromLevel, creature.level());
		for (Move move : result.learned())
			tell(nameOf(creature) + " learned " + move.name() + "!", Tone.GOOD);

		for (Move move : result.queued())
			tell(nameOf(creature) + " wants to learn " + move.name(), Tone.FIND);

		if (!result.learned().isEmpty() || !result.queued().isEmpty())
			dirty = true;
	}

	private Combatant foeOf(Species foe, int level)
	{
		return new Combatant(foe, level, 1.0).moves(moves.movesAt(foe, level));
	}

	/**
	 * Settles the move a creature is waiting to learn: it replaces one of the four it knows, or is skipped.
	 *
	 * @param creature a creature of the team or storage
	 * @param forgetIndex the place in its known moves of the one to forget, or -1 to skip the new move
	 * @return true when a pending move was settled
	 */
	boolean resolveMove(Creature creature, int forgetIndex)
	{
		if (creature == null || !(data.team.contains(creature) || data.storage.contains(creature)))
			return false;

		MoveLearning.Resolution done = MoveLearning.resolve(creature, moves, forgetIndex);
		if (done == null)
			return false;

		dirty = true;
		if (done.wasLearned())
			tell(nameOf(creature) + " forgot " + done.forgotten().name() + " and learned " + done.move().name() + "!",
				Tone.GOOD);
		else
			tell(nameOf(creature) + " did not learn " + done.move().name(), Tone.INFO);

		return true;
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
		notices.accept(Notice.creature(entry.wildId(), text, tone));
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

	private String waitingText(Creature creature)
	{
		EvolutionCondition waiting = evolutions.waitingFor(creature.species, creature.level(), conditions);
		return waiting == null ? null : waiting.waitingText();
	}

	private List<CreatureView> views(List<Creature> creatures, long now)
	{
		List<CreatureView> views = new ArrayList<>(creatures.size());
		for (Creature creature : creatures)
		{
			Species found = species.find(creature.species);
			Rarity rarity = found == null ? Rarity.COMMON : found.rarity();
			CombatType type = found == null ? CombatType.CRUSH : found.style();
			views.add(new CreatureView(speciesNameOf(creature), creature.level(), XpRules.progress(creature.xp),
				creature.hpFractionAt(now), rarity, type, creature.gilded, creature.nickname,
				creature.rolls(), found == null ? 0 : found.npcIdFor(creature.npcId),
				found == null ? Collections.emptyList() : MoveLearning.known(creature, found, moves),
				MoveLearning.pending(creature, moves), BondRules.hearts(creature.bond), waitingText(creature))
				.withSpecies(found));
		}

		return views;
	}

	private List<LogEntry.Stage> evolutionLine(Species entry)
	{
		LinkedList<Species> stages = new LinkedList<>();
		stages.add(entry);
		Set<String> visited = new HashSet<>();
		visited.add(entry.key());
		EvolutionTable.Step step = evolutions.previous(entry.key());
		while (step != null)
		{
			Species before = species.find(step.from());
			if (before == null || !visited.add(before.key()))
				break;

			stages.addFirst(before);
			step = evolutions.previous(before.key());
		}

		step = evolutions.next(entry.key());
		while (step != null)
		{
			Species after = species.find(step.to());
			if (after == null || !visited.add(after.key()))
				break;

			stages.addLast(after);
			step = evolutions.next(after.key());
		}

		if (stages.size() < 2)
			return Collections.emptyList();

		List<LogEntry.Stage> line = new ArrayList<>(stages.size());
		Set<String> inLine = new HashSet<>();
		for (Species stage : stages)
			inLine.add(stage.key());

		for (Species stage : stages)
			line.add(stageOf(stage, inLine));

		return line;
	}

	private boolean knownSpecies(Species stage)
	{
		Integer count = data.caught.get(stage.key());
		return data.seen.contains(stage.key()) || count != null && count > 0;
	}

	private LogEntry.Stage stageOf(Species stage, Set<String> inLine)
	{
		EvolutionTable.Step into = evolutions.previous(stage.key());
		if (into == null)
			return new LogEntry.Stage(stage.name(), knownSpecies(stage));

		List<LogEntry.Stage> others = new ArrayList<>();
		for (EvolutionTable.Step sibling : evolutions.steps(into.from()))
		{
			Species other = species.find(sibling.to());
			if (other != null && !inLine.contains(other.key()))
				others.add(new LogEntry.Stage(other.name(), knownSpecies(other), conditionOf(sibling),
					Collections.emptyList()));
		}

		return new LogEntry.Stage(stage.name(), knownSpecies(stage), conditionOf(into), others);
	}

	private static String conditionOf(EvolutionTable.Step step)
	{
		return step.when() == null ? null : step.when().phrase();
	}

	private List<LogEntry> log()
	{
		if (log != null)
			return log;

		List<LogEntry> entries = new ArrayList<>(species.size());
		for (Species entry : species.all())
		{
			Integer count = data.caught.get(entry.key());
			if (entry.questOnly() && (count == null || count < 1))
				continue;

			entries.add(new LogEntry(entry.name(), entry.level(), entry.rarity(), entry.style(),
				count == null ? 0 : count, data.seen.contains(entry.key()), data.gilded.contains(entry.key()),
				entry.questOnly(), examine.textFor(entry.key()), evolutionLine(entry)));
		}

		log = Collections.unmodifiableList(entries);
		return log;
	}
}
