/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A battle played one turn at a time. Both teams, the creature of each in the fight and the seeded random source
 * live here, and {@link #step(BattleChoice)} plays one turn for a choice of the player. An automatic battle is a
 * loop that always attacks (see {@link BattleSim}); a battle the player steers is the same loop with a choice
 * from the player. It holds no game client types.
 *
 * <p>A turn is a round: the faster creature attacks, then the other, and the round stops early when one of them
 * faints or when {@link BattleSim#MAX_TURNS} attacks have been made against the same opponent. Who is faster is
 * decided once for each pairing of two creatures, by {@link BattleSim#movesFirst}, the first time they attack. Every
 * attack makes the same random calls in the same order as the battles that were resolved in one go before this
 * class existed: the style roll of a creature with a second style, then the damage spread.
 *
 * <p>The other choices come first in their turn: a switch, a jar or a run happens before the opposing creature
 * attacks, whoever is faster. A switch costs the turn: the opposing creature attacks the one that stepped in. A
 * switch after a faint is free. A jar that does not catch lets the opposing creature attack. Running ends the battle
 * at once.
 */
final class BattleState
{
	/** Decides what a thrown jar does, so the rules of catching stay outside the battle. */
	interface CatchJudge
	{
		/**
		 * Throws a jar at the opposing creature.
		 *
		 * @param jar the jar chosen, or null to let the game pick one
		 * @param foeHealthLeft the share of full health the opposing creature has left, from 0 to 1
		 * @return what the throw did; {@link CatchOutcome#NO_JARS} and {@link CatchOutcome#FULL} use no turn
		 */
		CatchOutcome throwJar(JarType jar, double foeHealthLeft);
	}

	/** How a battle words its lines and what it does to the loser of a pairing. */
	static final class Mode
	{
		/** A single fight between two creatures, as {@link BattleSim#fight} plays it. */
		static final Mode SINGLE = new Mode(true, false, false, false);

		/** A team battle played in one go, as {@link BattleSim#teamFight} plays it. */
		static final Mode TEAM = new Mode(true, true, true, false);

		/** A wild battle the player steers: one opposing creature, a line for every switch. */
		static final Mode WILD = new Mode(true, false, true, true);

		/** A City Champion battle the player steers. */
		static final Mode CHAMPION = new Mode(true, true, true, true);

		private final boolean mineFirstWording;
		private final boolean knockOutLines;
		private final boolean knockOutLoser;
		private final boolean switchLines;

		private Mode(boolean mineFirstWording, boolean knockOutLines, boolean knockOutLoser, boolean switchLines)
		{
			this.mineFirstWording = mineFirstWording;
			this.knockOutLines = knockOutLines;
			this.knockOutLoser = knockOutLoser;
			this.switchLines = switchLines;
		}

		/**
		 * The mode of one duel between two creatures, which keeps no knock-out lines and leaves the loser as it is.
		 *
		 * @param firstIsMine whether the player's side is the first creature, which words the lines
		 * @return the mode
		 */
		static Mode duel(boolean firstIsMine)
		{
			return new Mode(firstIsMine, false, false, false);
		}
	}

	private static final int NONE = -1;

	private final List<Combatant> mine;
	private final List<Combatant> theirs;
	private final Random random;
	private final Mode mode;
	private final CatchJudge judge;
	private int myIndex;
	private int foeIndex;
	private boolean orderKnown;
	private boolean mineFirst;
	private int attacks;
	private int totalAttacks;
	private boolean needsSwitch;
	private TurnResult.Status over;
	private List<BattleLine> duelHits = new ArrayList<>();

	/**
	 * Starts a battle with the first creature of each side in the fight.
	 *
	 * @param mine the player's creatures, in the order they fight; their health is changed by the battle
	 * @param theirs the opposing creatures, in the order they fight
	 * @param random the source of damage spread and of the first move on a level tie
	 * @param mode how the lines are worded and what happens to a loser
	 */
	BattleState(List<Combatant> mine, List<Combatant> theirs, Random random, Mode mode)
	{
		this(mine, theirs, random, mode, null);
	}

	/**
	 * Starts a battle in which jars can be thrown.
	 *
	 * @param mine the player's creatures, in the order they fight; their health is changed by the battle
	 * @param theirs the opposing creatures, in the order they fight
	 * @param random the source of damage spread and of the first move on a level tie
	 * @param mode how the lines are worded and what happens to a loser
	 * @param judge decides what a thrown jar does, or null when jars cannot be thrown
	 */
	BattleState(List<Combatant> mine, List<Combatant> theirs, Random random, Mode mode, CatchJudge judge)
	{
		if (mine.isEmpty() || theirs.isEmpty())
			throw new IllegalArgumentException("Both sides need a creature");

		this.mine = new ArrayList<>(mine);
		this.theirs = new ArrayList<>(theirs);
		this.random = random;
		this.mode = mode;
		this.judge = judge;
	}

	/** @return the player's creature in the fight */
	Combatant mine()
	{
		return mine.get(myIndex);
	}

	/** @return the opposing creature in the fight */
	Combatant foe()
	{
		return theirs.get(foeIndex);
	}

	/** @return the place of the player's creature in the fight, in the order the team was given */
	int myIndex()
	{
		return myIndex;
	}

	/** @return the place of the opposing creature in the fight */
	int foeIndex()
	{
		return foeIndex;
	}

	/** @return the player's creatures, in the order they were given */
	List<Combatant> team()
	{
		return Collections.unmodifiableList(mine);
	}

	/** @return the opposing creatures, in the order they were given */
	List<Combatant> opponents()
	{
		return Collections.unmodifiableList(theirs);
	}

	/** @return whether the player must pick the next creature, because the one in the fight fainted */
	boolean needsSwitch()
	{
		return needsSwitch;
	}

	/** @return where the battle ended, or null while it goes on */
	TurnResult.Status ended()
	{
		return over;
	}

	/** @return how many attacks have been made against the opponent now in the fight */
	int attacks()
	{
		return attacks;
	}

	/** @return how many attacks have been made in the whole battle */
	int totalAttacks()
	{
		return totalAttacks;
	}

	/** @return the share of its health the opposing creature has lost, from 0 to 1 */
	double foeHealthLost()
	{
		return 1.0 - foe().hpFraction();
	}

	/**
	 * Finds the places of the player's creatures that could step in.
	 *
	 * @return the places, in team order, of every creature that has not fainted and is not in the fight
	 */
	List<Integer> switchTargets()
	{
		List<Integer> places = new ArrayList<>();
		for (int i = 0; i < mine.size(); i++)
		{
			if (i != myIndex && !mine.get(i).fainted())
				places.add(i);
		}

		return places;
	}

	/** @return the place of the first creature of the player's that has not fainted, or -1 when none is left */
	int firstAlive()
	{
		for (int i = 0; i < mine.size(); i++)
		{
			if (!mine.get(i).fainted())
				return i;
		}

		return NONE;
	}

	/**
	 * Plays one turn.
	 *
	 * @param choice what the player does; after a faint only a switch is allowed
	 * @return what happened and where the battle stands
	 * @throws IllegalStateException when the battle is over
	 * @throws IllegalArgumentException when the choice cannot be made now
	 */
	TurnResult step(BattleChoice choice)
	{
		if (over != null)
			throw new IllegalStateException("The battle is over: " + over);

		if (needsSwitch && choice.kind() != BattleChoice.Kind.SWITCH)
			throw new IllegalArgumentException("A creature must be picked first");

		Turn turn = new Turn();
		switch (choice.kind())
		{
			case ATTACK:
				attackRound(turn);
				break;

			case SWITCH:
				switchTo(choice.teamIndex(), turn);
				break;

			case CATCH:
				throwJar(choice.jar(), turn);
				break;

			case RUN:
				over = TurnResult.Status.RAN;
				turn.lines.add(new BattleLine("You ran away", Tone.INFO, true));
				break;

			default:
				throw new IllegalArgumentException("Items cannot be used yet");
		}

		return turn.result();
	}

	/** What one turn has done so far. */
	private final class Turn
	{
		private final List<BattleLine> lines = new ArrayList<>();
		private final List<BattleLine> hits = new ArrayList<>();
		private Duel duel;
		private boolean used = true;
		private Combatant enteredMine;
		private Combatant enteredFoe;
		private boolean mineFainted;
		private boolean foeFainted;
		private boolean voluntary;
		private CatchOutcome thrown;

		private TurnResult result()
		{
			TurnResult.Status status = over != null ? over
				: needsSwitch ? TurnResult.Status.NEEDS_SWITCH : TurnResult.Status.ONGOING;
			return new TurnResult(status, lines, hits, duel, used, enteredMine, enteredFoe, mineFainted, foeFainted,
				voluntary, thrown);
		}
	}

	private void attackRound(Turn turn)
	{
		decideOrder();
		attackIfAble(mineFirst, turn);
		attackIfAble(!mineFirst, turn);
		settle(turn);
	}

	private void decideOrder()
	{
		if (orderKnown)
			return;

		mineFirst = BattleSim.movesFirst(mine(), foe(), random);
		orderKnown = true;
	}

	private void attackIfAble(boolean mineAttacks, Turn turn)
	{
		if (mine().fainted() || foe().fainted() || attacks >= BattleSim.MAX_TURNS)
			return;

		Combatant attacker = mineAttacks ? mine() : foe();
		Combatant defender = mineAttacks ? foe() : mine();
		CombatType style = attacker.rollStyle(random);
		int damage = BattleSim.damage(attacker, style, defender, random);
		defender.hit(damage);
		BattleLine line = BattleSim.strike(attacker, defender, damage, mineAttacks == mode.mineFirstWording, style);
		turn.hits.add(line);
		turn.lines.add(line);
		duelHits.add(line);
		attacks++;
		totalAttacks++;
	}

	private void switchTo(int place, Turn turn)
	{
		if (place < 0 || place >= mine.size() || place == myIndex || mine.get(place).fainted())
			throw new IllegalArgumentException("Cannot switch to " + place);

		boolean free = needsSwitch;
		Combatant before = mine();
		myIndex = place;
		needsSwitch = false;
		orderKnown = false;
		attacks = 0;
		duelHits = new ArrayList<>();
		turn.enteredMine = mine();
		turn.voluntary = !free;
		if (mode.switchLines)
		{
			if (!free)
				turn.lines.add(new BattleLine(before.name() + " comes back", Tone.INFO, true));

			turn.lines.add(new BattleLine("Go, " + mine().name() + "!", Tone.INFO, true));
		}

		if (free)
			return;

		attackIfAble(false, turn);
		settle(turn);
	}

	private void throwJar(JarType jar, Turn turn)
	{
		if (judge == null)
			throw new IllegalArgumentException("Jars cannot be thrown in this battle");

		CatchOutcome outcome = judge.throwJar(jar, foe().hpFraction());
		turn.thrown = outcome;
		switch (outcome)
		{
			case CAUGHT:
				over = TurnResult.Status.CAUGHT;
				break;

			case FLED:
				over = TurnResult.Status.FOE_FLED;
				break;

			case BROKE_FREE:
				attackIfAble(false, turn);
				settle(turn);
				break;

			default:
				turn.used = false;
				break;
		}
	}

	/** Ends the pairing in the fight when one of the two fainted or the attack limit was reached. */
	private void settle(Turn turn)
	{
		boolean capped = !mine().fainted() && !foe().fainted() && attacks >= BattleSim.MAX_TURNS;
		if (!mine().fainted() && !foe().fainted() && !capped)
			return;

		boolean won = BattleSim.ahead(mine(), foe());
		Combatant fighter = mine();
		Combatant opponent = foe();
		if (mode.knockOutLoser)
		{
			if (won)
				opponent.knockOut();
			else
				fighter.knockOut();
		}

		turn.mineFainted = !won;
		turn.foeFainted = won;
		BattleLine out = null;
		if (mode.knockOutLines)
		{
			out = BattleSim.knockOutLine(fighter, opponent, won);
			turn.lines.add(out);
			turn.duel = new Duel(fighter, myIndex, opponent, foeIndex, duelHits, out, won);
		}

		duelHits = new ArrayList<>();
		attacks = 0;
		orderKnown = false;
		if (won && foeIndex + 1 < theirs.size())
		{
			foeIndex++;
			turn.enteredFoe = foe();
			return;
		}

		if (won)
		{
			over = TurnResult.Status.WON;
			return;
		}

		if (firstOther() == NONE)
		{
			over = TurnResult.Status.LOST;
			return;
		}

		needsSwitch = true;
		if (mode.switchLines)
			turn.lines.add(new BattleLine("Choose your next creature", Tone.INFO, true));
	}

	private int firstOther()
	{
		for (int i = 0; i < mine.size(); i++)
		{
			if (i != myIndex && !mine.get(i).fainted())
				return i;
		}

		return NONE;
	}
}
