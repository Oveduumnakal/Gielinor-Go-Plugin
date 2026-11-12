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
		int turns = duel(mine, foe, random, log);
		boolean won = ahead(mine, foe);
		log.add(closing(mine, foe, won));
		return new BattleResult(won, log, mine.hpFraction(), turns);
	}

	/**
	 * Fights a whole team against another. The first creature on each side fights; the loser is replaced by the
	 * next on its side, and the winner carries on with the health it has left.
	 *
	 * @param mine the player's creatures, in the order they fight; their health is changed by the battle
	 * @param theirs the opposing creatures, in the order they fight
	 * @param random the source of damage spread and of the first move on a level tie
	 * @return the outcome, with one line per creature knocked out
	 */
	static TeamBattleResult teamFight(List<Combatant> mine, List<Combatant> theirs, Random random)
	{
		List<BattleLine> log = new ArrayList<>();
		int myIndex = 0;
		int theirIndex = 0;
		while (myIndex < mine.size() && theirIndex < theirs.size())
		{
			Combatant fighter = mine.get(myIndex);
			Combatant foe = theirs.get(theirIndex);
			if (duelWon(fighter, foe, random))
			{
				foe.knockOut();
				log.add(new BattleLine(fighter.name() + " beat " + foe.name() + " (Lv " + foe.level() + ")",
					Tone.GOOD, true));
				theirIndex++;
			}
			else
			{
				fighter.knockOut();
				log.add(new BattleLine(fighter.name() + " lost to " + foe.name() + " (Lv " + foe.level() + ")",
					Tone.BAD, true));
				myIndex++;
			}
		}

		return new TeamBattleResult(theirIndex >= theirs.size(), log);
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
		duel(first, second, random, null);
		return ahead(first, second);
	}

	/**
	 * Trades blows until one side is out of health or the turn limit is reached.
	 *
	 * @param mine the player's creature
	 * @param foe the opposing creature
	 * @param random the source of damage spread and of the first move on a level tie
	 * @param log receives one line per attack, or null to keep no lines
	 * @return how many attacks were made
	 */
	private static int duel(Combatant mine, Combatant foe, Random random, List<BattleLine> log)
	{
		boolean myTurn = mine.level() > foe.level() || (mine.level() == foe.level() && random.nextBoolean());
		int turns = 0;
		while (!mine.fainted() && !foe.fainted() && turns < MAX_TURNS)
		{
			Combatant attacker = myTurn ? mine : foe;
			Combatant defender = myTurn ? foe : mine;
			CombatType style = attacker.rollStyle(random);
			int damage = damage(attacker, style, defender, random);
			defender.hit(damage);
			if (log != null)
				log.add(strike(attacker, defender, damage, myTurn, style != attacker.type()));

			myTurn = !myTurn;
			turns++;
		}

		return turns;
	}

	/**
	 * Decides who is winning a duel that has stopped.
	 *
	 * @param mine the player's creature
	 * @param foe the opposing creature
	 * @return true when the foe is out of health, or when neither is and the player's creature has kept at least
	 *     as large a share of its health
	 */
	private static boolean ahead(Combatant mine, Combatant foe)
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
	private static BattleLine opening(Combatant mine, Combatant foe)
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
	 * @param special whether the attack was of the attacker's second style, which reads as a fire breath
	 * @return the line, over the wild creature for the player's hits and over the player for hits taken
	 */
	private static BattleLine strike(Combatant attacker, Combatant defender, int damage, boolean mine,
		boolean special)
	{
		String health = " (" + (int) Math.ceil(defender.hp()) + "/" + defender.maxHp() + ")";
		if (mine)
		{
			String text = special ? breath(attacker) + damage + health : "-" + damage + health;
			return new BattleLine(text, Tone.GOOD, false, BattleLine.Kind.LEAD_HIT, special);
		}

		String text = special ? breath(attacker) + damage + health : defender.name() + " -" + damage + health;
		return new BattleLine(text, Tone.BAD, true, BattleLine.Kind.FOE_HIT, special);
	}

	private static String breath(Combatant attacker)
	{
		String name = attacker.name();
		return name + (name.toLowerCase().contains("wyvern") ? BREATHES_ICE : BREATHES);
	}

	private static BattleLine closing(Combatant mine, Combatant foe, boolean won)
	{
		if (won)
			return new BattleLine("Your " + mine.name() + " won!", Tone.GOOD, false, BattleLine.Kind.CLOSING);

		return new BattleLine("Your " + mine.name() + " lost to the " + foe.name() + ".", Tone.BAD, false,
			BattleLine.Kind.CLOSING);
	}
}
