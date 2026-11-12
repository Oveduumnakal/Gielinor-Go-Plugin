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

	/** What a line says when a creature uses its second style; only the breath users have one. */
	private static final String BREATHES = " breathes fire for ";

	/** The same for wyverns, whose breath is icy. */
	private static final String BREATHES_ICE = " breathes ice for ";

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
	 * Damage of one attack made in a given style, which for a creature with a second style may not be its main one.
	 *
	 * @param attacker the creature attacking
	 * @param style the style of this attack, which decides the combat triangle
	 * @param defender the creature being hit
	 * @param random the source of the damage spread
	 * @return the damage, at least 1
	 */
	static int damage(Combatant attacker, CombatType style, Combatant defender, Random random)
	{
		double spread = VARIANCE_FLOOR + VARIANCE_RANGE * random.nextDouble();
		double raw = attacker.attack() * style.multiplierAgainst(defender.type()) * spread;
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
	 * The line for one attack, shown over whoever was hit: the damage, then the health the target has left.
	 *
	 * @param attacker the creature that attacked
	 * @param defender the creature that was hit
	 * @param damage the health removed
	 * @param mine whether the player's creature made the attack
	 * @param style the style the attack was made in, already rolled; a style other than the attacker's own reads as
	 *     a fire breath
	 * @return the line, over the wild creature for the player's hits and over the player for hits taken
	 */
	static BattleLine strike(Combatant attacker, Combatant defender, int damage, boolean mine,
		CombatType style)
	{
		boolean special = style != attacker.type();
		boolean icy = special && icy(attacker);
		String health = " (" + (int) Math.ceil(defender.hp()) + "/" + defender.maxHp() + ")";
		if (mine)
		{
			String text = special ? breath(attacker) + damage + health : "-" + damage + health;
			return new BattleLine(text, Tone.GOOD, false, BattleLine.Kind.LEAD_HIT, special, style, icy);
		}

		String text = special ? breath(attacker) + damage + health : defender.name() + " -" + damage + health;
		return new BattleLine(text, Tone.BAD, true, BattleLine.Kind.FOE_HIT, special, style, icy);
	}

	private static String breath(Combatant attacker)
	{
		return attacker.name() + (icy(attacker) ? BREATHES_ICE : BREATHES);
	}

	/**
	 * @param attacker the creature that breathes
	 * @return whether its breath is icy: wyverns breathe ice, everything else fire
	 */
	private static boolean icy(Combatant attacker)
	{
		String name = attacker.name().toLowerCase();
		return name.contains("wyvern");
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
