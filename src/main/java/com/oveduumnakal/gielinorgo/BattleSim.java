/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Resolves a battle between two creatures in one go. Given the same fighters and the same random seed it always
 * produces the same result, so battles can be tested exactly.
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
		boolean myTurn = mine.level() > foe.level() || (mine.level() == foe.level() && random.nextBoolean());
		int turns = 0;
		while (!mine.fainted() && !foe.fainted() && turns < MAX_TURNS)
		{
			Combatant attacker = myTurn ? mine : foe;
			Combatant defender = myTurn ? foe : mine;
			int damage = damage(attacker, defender, random);
			defender.hit(damage);
			log.add(strike(attacker, defender, damage, myTurn));
			myTurn = !myTurn;
			turns++;
		}

		boolean won = foe.fainted() || (!mine.fainted() && mine.hpFraction() >= foe.hpFraction());
		log.add(closing(mine, foe, won));
		return new BattleResult(won, log, mine.hpFraction(), turns);
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
		double spread = VARIANCE_FLOOR + VARIANCE_RANGE * random.nextDouble();
		double raw = attacker.attack() * attacker.type().multiplierAgainst(defender.type()) * spread;
		return Math.max(1, (int) Math.round(raw - defender.defence() / 2.0));
	}

	/**
	 * The line that starts a battle, shown over the wild creature.
	 *
	 * @param mine the player's creature
	 * @param foe the wild creature
	 * @return the opening line, with a note when one style beats the other
	 */
	private static BattleLine opening(Combatant mine, Combatant foe)
	{
		String line = "Your " + mine.name() + " (Lv " + mine.level() + ") attacks!";
		double multiplier = mine.type().multiplierAgainst(foe.type());
		if (multiplier > 1.0)
			return new BattleLine(line + " " + mine.type().label() + " beats " + foe.type().label() + "!",
				Tone.GOOD, false);

		if (multiplier < 1.0)
			return new BattleLine(line + " " + foe.type().label() + " beats " + mine.type().label() + ".",
				Tone.BAD, false);

		return new BattleLine(line, Tone.INFO, false);
	}

	/**
	 * The line for one attack, shown over whoever was hit: the damage, then the health the target has left.
	 *
	 * @param attacker the creature that attacked
	 * @param defender the creature that was hit
	 * @param damage the health removed
	 * @param mine whether the player's creature made the attack
	 * @return the line, over the wild creature for the player's hits and over the player for hits taken
	 */
	private static BattleLine strike(Combatant attacker, Combatant defender, int damage, boolean mine)
	{
		String health = " (" + (int) Math.ceil(defender.hp()) + "/" + defender.maxHp() + ")";
		if (mine)
			return new BattleLine("-" + damage + health, Tone.GOOD, false);

		return new BattleLine(defender.name() + " -" + damage + health, Tone.BAD, true);
	}

	private static BattleLine closing(Combatant mine, Combatant foe, boolean won)
	{
		if (won)
			return new BattleLine("Your " + mine.name() + " won!", Tone.GOOD, false);

		return new BattleLine("Your " + mine.name() + " lost to the " + foe.name() + ".", Tone.BAD, false);
	}
}
