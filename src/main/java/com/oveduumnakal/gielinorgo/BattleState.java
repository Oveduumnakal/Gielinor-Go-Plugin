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
 * decided once for each pairing of two creatures, by {@link BattleSim#movesFirst}, the first time they attack.
 *
 * <p>Every attack makes its random calls in this fixed order, and a call that has nothing to decide is not made, so
 * a creature with one move of accuracy 1 and no status makes exactly the calls battles made before moves existed:
 * (1) the stun roll, one draw, only when the attacker is stunned; a frozen or stunned creature that loses its turn
 * makes no other call; (2) the pick of the move, one draw, only when the attacker has more than one move and was not
 * told which to use; the pick is weighted by power times accuracy times the combat triangle against the opposing
 * creature, and every known move keeps a weight above zero; (3) the accuracy
 * roll, one draw, only when the move's accuracy is below 1; (4) the damage spread, one draw, only when the move
 * hits; (5) the effect roll, one draw, only when the move hits, the defender is still up and free of a status, and
 * the move has an effect that could be put on it; (6) the trait roll of a poisonous attacker's melee hit, one draw,
 * only when the hit is still unaffected and the defender can be poisoned; (7) when a freeze was just put on, one
 * {@code nextInt} draw for its length. At the end of a round, poison, venom and burn take health with no draw, the
 * creature that moved first taking its share first.
 *
 * <p>The other choices come first in their turn: a switch, a jar, an item or a run happens before the opposing
 * creature attacks, whoever is faster. A switch costs the turn: the opposing creature attacks the one that stepped
 * in. A switch after a faint is free. An item costs the turn too, and uses no random draw: a salve heals a creature
 * that is up and hurt, a Revive draught brings back one that fainted in this battle, and an Antidote cures poison,
 * venom or burn on the creature in the fight. A jar that does not catch lets the opposing creature attack. Running
 * ends the battle at once.
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
				if (choice.moveIndex() >= mine().moves().size())
					throw new IllegalArgumentException("No such move: " + choice.moveIndex());

				attackRound(turn, choice.moveIndex());
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
				useItem(choice.itemType(), choice.teamIndex(), turn);
				break;
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

	private void attackRound(Turn turn, int moveIndex)
	{
		decideOrder();
		attackIfAble(mineFirst, turn, moveIndex);
		attackIfAble(!mineFirst, turn, moveIndex);
		endRound(turn);
		settle(turn);
	}

	private void decideOrder()
	{
		if (orderKnown)
			return;

		mineFirst = BattleSim.movesFirst(mine(), foe(), random);
		orderKnown = true;
	}

	/**
	 * Makes one creature's attack, if both can still fight.
	 *
	 * @param mineAttacks whether the player's creature attacks
	 * @param turn the turn to record it in
	 * @param moveIndex the move the player chose, used only when the player's creature attacks; -1 lets it pick
	 */
	private void attackIfAble(boolean mineAttacks, Turn turn, int moveIndex)
	{
		if (mine().fainted() || foe().fainted() || attacks >= BattleSim.MAX_TURNS)
			return;

		Combatant attacker = mineAttacks ? mine() : foe();
		Combatant defender = mineAttacks ? foe() : mine();
		Status held = attacker.status();
		if (attacker.skipsTurn(random))
		{
			note(turn, BattleSim.skipped(attacker, held, wordingOf(mineAttacks)), mineAttacks, held);
			if (attacker.status() == null)
				note(turn, BattleSim.shookOff(attacker, wordingOf(mineAttacks)), mineAttacks, null);

			return;
		}

		if (held != null && attacker.status() == null)
			note(turn, BattleSim.shookOff(attacker, wordingOf(mineAttacks)), mineAttacks, null);

		Move move = pick(attacker, defender, mineAttacks ? moveIndex : -1);
		boolean wording = mineAttacks == mode.mineFirstWording;
		BattleLine line;
		Status inflicted = null;
		if (move.accuracy() < 1.0 && random.nextDouble() >= move.accuracy())
		{
			line = BattleSim.miss(attacker, wording, move);
		}
		else
		{
			int damage = BattleSim.damage(attacker, move, defender, random);
			defender.hit(damage);
			line = BattleSim.strike(attacker, defender, damage, wording, move);
			inflicted = rollStatus(attacker, defender, move);
		}

		turn.hits.add(line);
		turn.lines.add(line);
		duelHits.add(line);
		attacks++;
		totalAttacks++;
		if (inflicted != null)
			note(turn, BattleSim.afflicted(defender, inflicted, wordingOf(!mineAttacks)), !mineAttacks, inflicted);
	}

	/**
	 * Rolls what a hit does besides damage: the move's own effect first, then the trait rule that lets a poisonous
	 * creature poison with a melee hit. A roll that cannot change anything is not made.
	 *
	 * @return the status put on the defender, or null for none
	 */
	private Status rollStatus(Combatant attacker, Combatant defender, Move move)
	{
		if (defender.fainted())
			return null;

		Move.Effect effect = move.effect();
		Status wanted = effect == null ? null : Status.named(effect.status());
		if (wanted != null && effect.chance() > 0 && defender.canAfflict(wanted)
			&& random.nextDouble() < effect.chance() && defender.afflict(wanted, random))
			return wanted;

		if (move.style() == CombatType.MELEE && attacker.traits().contains(Trait.POISONOUS)
			&& defender.canAfflict(Status.POISON) && random.nextDouble() < Status.POISONOUS_HIT_CHANCE
			&& defender.afflict(Status.POISON, random))
			return Status.POISON;

		return null;
	}

	private boolean wordingOf(boolean playersCreature)
	{
		return playersCreature == mode.mineFirstWording;
	}

	/**
	 * Records a line about a status. It is acted out as a beat of its own and carries the status each side has once
	 * it has happened, so a scene can show the tags.
	 *
	 * @param turn the turn
	 * @param line the line
	 * @param aboutMine whether the creature the line is about is the player's
	 * @param shown the status that creature still shows at this line, null when it has none by now
	 */
	private void note(Turn turn, BattleLine line, boolean aboutMine, Status shown)
	{
		Status mineShows = aboutMine ? shown : mine().status();
		Status foeShows = aboutMine ? foe().status() : shown;
		BattleLine tagged = line.withTags(mineShows, foeShows);
		turn.hits.add(tagged);
		turn.lines.add(tagged);
		duelHits.add(tagged);
	}

	/**
	 * Ends a round: poison, venom and burn take their health, the creature that attacked first taking its share
	 * first, and a creature that faints ends the round for the other. Nothing happens when either is already down.
	 */
	private void endRound(Turn turn)
	{
		if (mine().fainted() || foe().fainted())
			return;

		Combatant mineNow = mine();
		Combatant foeNow = foe();
		boolean quicker = mineNow.speed() >= foeNow.speed();
		boolean mineFirstNow = orderKnown ? mineFirst : quicker;
		hurtByStatus(turn, mineFirstNow);
		if (!mine().fainted() && !foe().fainted())
			hurtByStatus(turn, !mineFirstNow);
	}

	private void hurtByStatus(Turn turn, boolean aboutMine)
	{
		Combatant victim = aboutMine ? mine() : foe();
		Status held = victim.status();
		if (held == null || !held.damaging())
			return;

		int damage = victim.endOfRound();
		note(turn, BattleSim.statusDamage(victim, held, damage, wordingOf(aboutMine)), aboutMine, held);
		if (!victim.fainted() && victim.status() == null)
			note(turn, BattleSim.shookOff(victim, wordingOf(aboutMine)), aboutMine, null);
	}

	/**
	 * Picks the move an attacker uses. A move the player chose is used as it is; a creature with one move uses it; any
	 * other picks by one weighted draw, so the stronger and the more accurate moves, and those the combat triangle
	 * favours against the defender, come up more often.
	 */
	private Move pick(Combatant attacker, Combatant defender, int chosen)
	{
		List<Move> known = attacker.moves();
		if (chosen >= 0 && chosen < known.size())
			return known.get(chosen);

		if (known.size() == 1)
			return known.get(0);

		double[] weights = new double[known.size()];
		double total = 0;
		for (int i = 0; i < weights.length; i++)
		{
			Move move = known.get(i);
			weights[i] = move.power() * move.accuracy() * move.style().multiplierAgainst(defender.type());
			total += weights[i];
		}

		double roll = random.nextDouble() * total;
		for (int i = 0; i < weights.length; i++)
		{
			roll -= weights[i];
			if (roll < 0)
				return known.get(i);
		}

		return known.get(known.size() - 1);
	}

	private void switchTo(int place, Turn turn)
	{
		if (place < 0 || place >= mine.size() || place == myIndex || mine.get(place).fainted())
			throw new IllegalArgumentException("Cannot switch to " + place);

		boolean free = needsSwitch;
		Combatant before = mine();
		before.clearStatus();
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

		attackIfAble(false, turn, -1);
		endRound(turn);
		settle(turn);
	}

	/**
	 * Finds the creatures of the player's team an item would do something for right now.
	 *
	 * @param type the item
	 * @return the places, in team order; empty when the item would do nothing, or when the player must pick the next
	 *     creature first
	 */
	List<Integer> itemTargets(ItemType type)
	{
		List<Integer> places = new ArrayList<>();
		if (type == null || over != null || needsSwitch)
			return places;

		if (type.cures())
		{
			Status held = mine().status();
			if (held != null && held.curable())
				places.add(myIndex);

			return places;
		}

		for (int i = 0; i < mine.size(); i++)
		{
			Combatant one = mine.get(i);
			boolean fits = type.revives() ? one.fainted() : !one.fainted() && one.hpFraction() < 1.0;
			if (fits)
				places.add(i);
		}

		return places;
	}

	/**
	 * Uses an item as the turn's action. The opposing creature then attacks, and the round ends as it does for a
	 * switch.
	 *
	 * @throws IllegalArgumentException when no item was picked or it would do nothing for that creature
	 */
	private void useItem(ItemType type, int place, Turn turn)
	{
		if (type == null)
			throw new IllegalArgumentException("Pick an item first");

		if (!itemTargets(type).contains(place))
			throw new IllegalArgumentException(itemRefusal(type, place));

		Combatant target = mine.get(place);
		if (type.cures())
		{
			Status cured = target.status();
			target.clearStatus();
			note(turn, new BattleLine(target.name() + " is cured of " + cured.noun(), Tone.GOOD, true,
				BattleLine.Kind.STATUS), true, null);
		}
		else if (type.revives())
		{
			target.revive(ItemRules.REVIVE_TO);
			turn.lines.add(new BattleLine(target.name() + " is back on their feet (" + (int) Math.ceil(target.hp())
				+ "/" + target.maxHp() + ")", Tone.GOOD, true));
		}
		else
		{
			int healed = target.heal(type.heal());
			turn.lines.add(new BattleLine(target.name() + " is healed +" + healed + " (" + (int) Math.ceil(target.hp())
				+ "/" + target.maxHp() + ")", Tone.GOOD, true));
		}

		attackIfAble(false, turn, -1);
		endRound(turn);
		settle(turn);
	}

	private String itemRefusal(ItemType type, int place)
	{
		if (needsSwitch)
			return "A creature must be picked first";

		if (place < 0 || place >= mine.size())
			return "No such creature: " + place;

		String name = mine.get(place).name();
		if (type.cures())
			return "Nothing to cure";

		if (type.revives())
			return name + " has not fainted";

		return mine.get(place).fainted() ? name + " has fainted" : name + " is already at full health";
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
				attackIfAble(false, turn, -1);
				endRound(turn);
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
		fighter.clearStatus();
		opponent.clearStatus();
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
