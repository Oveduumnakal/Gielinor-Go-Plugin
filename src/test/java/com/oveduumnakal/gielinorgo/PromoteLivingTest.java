/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Random;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** After a fight the first living creature takes the place of a lead that fainted; an Elite run freezes the order. */
public class PromoteLivingTest
{
	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.STAB);
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.STAB);
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);

	private long now = 1_000_000L;
	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(CHICKEN, GOBLIN, DRAGON));
		game = new GoGame(table, new Random(5), () -> now, notice ->
		{
		});
	}

	@Test
	public void aLeadThatFaintsInABattleIsReplacedByTheOneThatWon()
	{
		Creature weak = new Creature("chicken", 1, now);
		Creature strong = new Creature("goblin", 70, now);
		game.data().team.add(weak);
		game.data().team.add(strong);

		BattleResult result = game.battle(new WildEntry(1, GOBLIN, 12, 5));

		assertTrue(result.won());
		assertTrue(weak.fainted);
		assertSame("the survivor leads now", strong, game.data().lead());
		assertSame(weak, game.data().team.get(1));
	}

	@Test
	public void aLeadThatStandsStaysTheLead()
	{
		Creature strong = new Creature("goblin", 70, now);
		Creature other = new Creature("goblin", 10, now);
		game.data().team.add(strong);
		game.data().team.add(other);

		game.battle(new WildEntry(1, CHICKEN, 1, 1));

		assertSame(strong, game.data().lead());
	}

	@Test
	public void aTeamThatIsAllDownKeepsItsOrder()
	{
		Creature one = new Creature("chicken", 1, now);
		Creature two = new Creature("chicken", 1, now);
		game.data().team.add(one);
		game.data().team.add(two);

		game.battle(new WildEntry(1, DRAGON, 60, 276));

		assertTrue(one.fainted);
		assertTrue(two.fainted);
		assertSame(one, game.data().lead());
	}

	@Test
	public void aSteeredBattleFollowsTheSameRule()
	{
		Creature weak = new Creature("chicken", 1, now);
		Creature strong = new Creature("goblin", 70, now);
		game.data().team.add(weak);
		game.data().team.add(strong);
		BattleSession session = game.startBattle(new WildEntry(1, GOBLIN, 12, 5));
		assertNotNull(session);

		while (!session.over())
		{
			BattleState state = session.state();
			game.act(session, state.needsSwitch() ? BattleChoice.switchTo(state.firstAlive())
				: BattleChoice.attack());
		}

		assertSame(strong, game.data().lead());
	}

	@Test
	public void theOrderIsFrozenDuringAnEliteRunAndAppliedWhenItEnds()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(new Species("Mite", 1, CombatType.STAB),
			new Species("Brute", 200, CombatType.STAB)));
		StringBuilder stages = new StringBuilder();
		for (int i = 1; i <= 5; i++)
		{
			stages.append(i > 1 ? "," : "").append("{\"id\":\"e").append(i).append("\",\"city\":\"Elite ").append(i)
				.append("\",\"team\":[{\"species\":\"Mite\",\"level\":70}]}");
		}

		String json = "{\"minX\":3180,\"minY\":3346,\"maxX\":3202,\"maxY\":3372,\"firstDragonJars\":10,"
			+ "\"firstSpecialtyJars\":3,\"firstEgg\":\"LEGENDARY\",\"laterDragonJars\":2,\"stages\":[" + stages + "]}";
		GoGame elite = new GoGame(table, new Random(7), () -> now, notice ->
		{
		});
		elite.elite(EliteTable.load(new Gson(), new StringReader(json), table));
		for (int i = 0; i < 8; i++)
			elite.data().sigils.add("city-" + i);

		elite.moveTo(3191, 3367, 0);
		Creature lead = new Creature("brute", 60, now);
		Creature other = new Creature("brute", 60, now);
		elite.data().team.add(lead);
		elite.data().team.add(other);
		BattleSession battle = elite.eliteSteered();
		while (!battle.over())
			elite.act(battle, BattleChoice.attack());

		assertTrue(elite.eliteOpen());
		lead.faint(now);
		elite.data().team.get(0);
		BattleSession next = elite.eliteSteered();
		assertNotNull(next);
		assertSame("frozen while the run is open", lead, elite.data().lead());

		elite.eliteForfeit();

		assertFalse(elite.eliteOpen());
		assertSame("applied at the end of the run", other, elite.data().lead());
	}
}
