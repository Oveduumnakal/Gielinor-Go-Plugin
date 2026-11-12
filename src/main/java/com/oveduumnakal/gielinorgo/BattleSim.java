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
 * Resolves a battle between creatures in one go, as a loop over {@link BattleState} that always attacks, and holds the
 * rules of a single attack. Given the same fighters and the same random seed it always produces the same result, so
 * battles can be tested exactly.
 */
final class BattleSim
{
	/** Most attacks a battle can run before it is decided on health left. */
	static final int MAX_TURNS = 50;

	private static final double VARIANCE_FLOOR = 0.85;
	private static final double VARIANCE_RANGE = 0.3;

	private BattleSim()
	{
	}

	/**
	 * Fights a battle to the end.
	 *
	 * @param mine the player's creature
	 * @param foe the opposing creature
	 * @param random the source of damage spread and of the first move on a level tie
	 * @return the outcome
	 */
	static BattleResult fight(Combatant mine, Combatant foe, Random random)
	{
		List<BattleLine> log = new ArrayList<>();
		log.add(opening(mine, foe));
		BattleState state = new BattleState(Collections.singletonList(mine), Collections.singletonList(foe), random,
			BattleState.Mode.SINGLE);
		while (state.ended() == null)
			log.addAll(state.step(BattleChoice.attack()).lines());

		boolean won = state.ended() == TurnResult.Status.WON;
		log.add(closing(mine, foe, won));
		return new BattleResult(won, log, mine.hpFraction(), state.totalAttacks());
	}

	/**
	 * Fights a whole team against another. The first creature on each side fights; the loser is replaced by the
	 * next on its side, and the winner carries on with the health it has left.
	 *
	 * @param mine the player's creatures, in the order they fight; their health is changed by the battle
	 * @param theirs the opposing creatures, in the order they fight
	 * @param random the source of damage spread and of the first move on a level tie
	 * @return the outcome, with one line per creature knocked out and the duels it was made of
	 */
	static TeamBattleResult teamFight(List<Combatant> mine, List<Combatant> theirs, Random random)
	{
		List<BattleLine> log = new ArrayList<>();
		List<Duel> duels = new ArrayList<>();
		BattleState state = new BattleState(mine, theirs, random, BattleState.Mode.TEAM);
		while (state.ended() == null)
		{
			TurnResult turn = state.step(state.needsSwitch() ? BattleChoice.switchTo(state.firstAlive())
				: BattleChoice.attack());
			if (turn.duel() == null)
				continue;

			duels.add(turn.duel());
			log.add(turn.duel().knockOut());
		}

		return new TeamBattleResult(state.ended() == TurnResult.Status.WON, log, duels);
	}

	/**
	 * Fights one duel without keeping its lines. Both creatures are left with the health the duel cost them.
	 *
	 * @param first the creature whose side is asking; it wins a duel that ends level
	 * @param second the creature it fights
	 * @param random the source of damage spread and of the first move on a level tie
	 * @return true when the first creature won
	 */
	static boolean duelWon(Combatant first, Combatant second, Random random)
	{
		return duelWon(first, second, random, null, true);
	}

	/**
	 * Fights one duel and keeps the line of every hit. It makes exactly the same random calls as
	 * {@link #duelWon(Combatant, Combatant, Random)}, so keeping the lines changes no result.
	 *
	 * @param first the creature whose side is asking; it wins a duel that ends level
	 * @param second the creature it fights
	 * @param random the source of damage spread and of the first move on a level tie
	 * @param hits receives one line per hit, or null to keep none
	 * @param firstIsMine whether the first creature is the player's, so its hits are worded as the player's and the
	 *     second one's as the opponent's
	 * @return true when the first creature won
	 */
	static boolean duelWon(Combatant first, Combatant second, Random random, List<BattleLine> hits,
		boolean firstIsMine)
	{
		BattleState state = new BattleState(Collections.singletonList(first), Collections.singletonList(second),
			random, BattleState.Mode.duel(firstIsMine));
		while (state.ended() == null)
		{
			TurnResult turn = state.step(BattleChoice.attack());
			if (hits != null)
				hits.addAll(turn.hits());
		}

		return state.ended() == TurnResult.Status.WON;
	}

	/**
	 * Decides who attacks first: the faster creature, then the higher level, then a coin toss. Creatures with no
	 * rolls are exactly as fast as their level says, so for them this is the old rule and the coin is tossed only
	 * when the levels are equal.
	 *
	 * @param mine the player's creature
	 * @param foe the opposing creature
	 * @param random the source of the coin toss
	 * @return true when the player's creature attacks first
	 */
	static boolean movesFirst(Combatant mine, Combatant foe, Random random)
	{
		if (mine.speed() != foe.speed())
			return mine.speed() > foe.speed();

		return mine.level() > foe.level() || (mine.level() == foe.level() && random.nextBoolean());
	}

	/**
	 * Decides who is winning a duel that has stopped.
	 *
	 * @param mine the player's creature
	 * @param foe the opposing creature
	 * @return true when the foe is out of health, or when neither is and the player's creature has kept at least
	 *     as large a share of its health
	 */
	static boolean ahead(Combatant mine, Combatant foe)
	{
		return foe.fainted() || (!mine.fainted() && mine.hpFraction() >= foe.hpFraction());
	}

	/**
	 * Damage of one attack.
	 *
	 * @param attacker the creature attacking
	 * @param defender the creature being hit
	 * @param random the source of the damage spread
	 * @return the damage, at least 1
	 */
	static int damage(Combatant attacker, Combatant defender, Random random)
	{
		return damage(attacker, attacker.type(), defender, random);
	}

	/**
	 * Damage of one plain attack made in a given style, a move of power 1.
	 *
	 * @param attacker the creature attacking
	 * @param style the style of this attack, which decides the combat triangle
	 * @param defender the creature being hit
	 * @param random the source of the damage spread
	 * @return the damage, at least 1
	 */
	static int damage(Combatant attacker, CombatType style, Combatant defender, Random random)
	{
		return damage(attacker, Move.basic(style), defender, random);
	}

	/**
	 * Damage of one attack with a move: the attacker's attack stat, times the combat triangle for the move's style
	 * against the defender's style, times the move's power, times a spread of 0.85 to 1.15, less half the defender's
	 * defence. A power of 1 leaves the formula exactly as it was before moves existed.
	 *
	 * @param attacker the creature attacking
	 * @param move the move used
	 * @param defender the creature being hit
	 * @param random the source of the damage spread; one call
	 * @return the damage, at least 1
	 */
	static int damage(Combatant attacker, Move move, Combatant defender, Random random)
	{
		double spread = VARIANCE_FLOOR + VARIANCE_RANGE * random.nextDouble();
		double raw = attacker.attack() * move.style().multiplierAgainst(defender.type()) * move.power() * spread;
		return Math.max(1, (int) Math.round(raw - defender.defence() / 2.0));
	}

	/**
	 * The line that starts a battle, shown over the wild creature.
	 *
	 * @param mine the player's creature
	 * @param foe the wild creature
	 * @return the opening line, with a note when one style beats the other
	 */
	static BattleLine opening(Combatant mine, Combatant foe)
	{
		String line = "Your " + mine.name() + " (Lv " + mine.level() + ") attacks!";
		double multiplier = mine.type().multiplierAgainst(foe.type());
		if (multiplier > 1.0)
			return new BattleLine(line + " " + mine.type().label() + " beats " + foe.type().label() + "!",
				Tone.GOOD, false, BattleLine.Kind.OPENING);

		if (multiplier < 1.0)
			return new BattleLine(line + " " + foe.type().label() + " beats " + mine.type().label() + ".",
				Tone.BAD, false, BattleLine.Kind.OPENING);

		return new BattleLine(line, Tone.INFO, false, BattleLine.Kind.OPENING);
	}

	/**
	 * The line for one attack that hit, shown over whoever was hit. A creature's basic attack reads as it always did
	 * (the damage, then the health the target has left); a named move reads {@code Bob uses Dragonfire: -14 (20/50)}.
	 *
	 * @param attacker the creature that attacked
	 * @param defender the creature that was hit
	 * @param damage the health removed
	 * @param mine whether the player's creature made the attack
	 * @param move the move used; one of the second slot plays the attacker's second animation
	 * @return the line, over the wild creature for the player's hits and over the player for hits taken
	 */
	static BattleLine strike(Combatant attacker, Combatant defender, int damage, boolean mine, Move move)
	{
		String health = " (" + (int) Math.ceil(defender.hp()) + "/" + defender.maxHp() + ")";
		String named = attacker.name() + " uses " + move.name() + ": -" + damage + health;
		if (mine)
		{
			String text = move.plain() ? "-" + damage + health : named;
			return new BattleLine(text, Tone.GOOD, false, BattleLine.Kind.LEAD_HIT, move.second(), move.style(),
				move.icy());
		}

		String text = move.plain() ? defender.name() + " -" + damage + health : named;
		return new BattleLine(text, Tone.BAD, true, BattleLine.Kind.FOE_HIT, move.second(), move.style(),
			move.icy());
	}

	/**
	 * The line for an attack that missed, shown over the creature that was not hit.
	 *
	 * @param attacker the creature that attacked
	 * @param mine whether the player's creature made the attack
	 * @param move the move used
	 * @return the line, over the wild creature for the player's misses and over the player for the foe's
	 */
	static BattleLine miss(Combatant attacker, boolean mine, Move move)
	{
		String text = attacker.name() + "'s " + move.name() + " missed";
		BattleLine.Kind kind = mine ? BattleLine.Kind.LEAD_HIT : BattleLine.Kind.FOE_HIT;
		return new BattleLine(text, Tone.INFO, !mine, kind, move.second(), move.style(), move.icy(), true);
	}

	/**
	 * The line that tells how a pairing of a team battle ended.
	 *
	 * @param fighter the player's creature of the pairing
	 * @param opponent the opposing creature of the pairing
	 * @param won whether the player's creature won
	 * @return the line, shown over the player
	 */
	static BattleLine knockOutLine(Combatant fighter, Combatant opponent, boolean won)
	{
		if (won)
			return new BattleLine(fighter.name() + " beat " + opponent.name() + " (Lv " + opponent.level() + ")",
				Tone.GOOD, true);

		return new BattleLine(fighter.name() + " lost to " + opponent.name() + " (Lv " + opponent.level() + ")",
			Tone.BAD, true);
	}

	/**
	 * The line that ends a fight against a wild creature.
	 *
	 * @param mine the player's creature
	 * @param foe the wild creature
	 * @param won whether the player's creature won
	 * @return the closing line
	 */
	static BattleLine closing(Combatant mine, Combatant foe, boolean won)
	{
		if (won)
			return new BattleLine("Your " + mine.name() + " won!", Tone.GOOD, false, BattleLine.Kind.CLOSING);

		return new BattleLine("Your " + mine.name() + " lost to the " + foe.name() + ".", Tone.BAD, false,
			BattleLine.Kind.CLOSING);
	}
}
