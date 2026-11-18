/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Reviving a fainted creature: the free revive where the team may be changed, and the draught anywhere. */
public class ReviveGameTest
{
	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.STAB);
	private static final double EPSILON = 1e-9;

	private final List<String> messages = new ArrayList<>();
	private long now = 1_000_000L;
	private GoGame game;
	private Creature team;
	private Creature stored;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(CHICKEN));
		game = new GoGame(table, new Random(3), () -> now, notice -> messages.add(notice.text()));
		team = new Creature("chicken", 5, now);
		stored = new Creature("chicken", 6, now);
		game.data().team.add(team);
		game.data().storage.add(stored);
		team.faint(now);
		stored.faint(now);
	}

	@Test
	public void reviveAtABankStandsATeamMemberUpAtHalfHealthAndTellsTheLine()
	{
		game.setBankNear(true);

		assertTrue(game.reviveAtBank(team));

		assertFalse(team.fainted);
		assertEquals(0.5, team.hpFractionAt(now), EPSILON);
		assertEquals(0.5, ReviveRules.BANK_TO, EPSILON);
		assertTrue(messages.toString(), messages.contains("Chicken was revived"));
		assertTrue(game.takeDirty());
	}

	@Test
	public void theRevivedCreatureRestsFromThere()
	{
		game.setBankNear(true);
		game.reviveAtBank(team);

		assertEquals(1.0, team.hpFractionAt(now + Creature.FULL_REST_MS), EPSILON);
	}

	@Test
	public void aStoredCreatureCanBeRevivedToo()
	{
		game.setBankNear(true);

		assertTrue(game.reviveAtBank(stored));

		assertFalse(stored.fainted);
		assertEquals(0.5, stored.hpFractionAt(now), EPSILON);
	}

	@Test
	public void awayFromABankAndATownItIsRefused()
	{
		game.setBankNear(false);

		assertFalse(game.reviveAtBank(team));

		assertTrue(team.fainted);
		assertTrue(messages.toString(), messages.contains("Go to a bank or a city to revive Chicken"));
	}

	@Test
	public void anOpenBankAllowsItWithNoBankerNearby()
	{
		game.setBankNear(false);
		game.setBankOpen(true);

		assertTrue(game.reviveAtBank(team));
	}

	@Test
	public void aTeamTownAllowsItLikeASwap()
	{
		game.setBankNear(false);
		game.teamTowns(new TeamTowns(Arrays.asList(new TeamTowns.Town("Fortis", 1630, 3070, 1750, 3170))));

		game.moveTo(1700, 3100, 0);

		assertTrue(game.inTeamArea());
		assertTrue(game.reviveAtBank(team));
	}

	@Test
	public void aCreatureThatIsUpHasNothingToRevive()
	{
		game.setBankNear(true);
		game.reviveAtBank(team);
		now += 1;

		assertFalse(game.reviveAtBank(team));
		assertTrue(messages.toString(), messages.contains("Chicken has not fainted"));
	}

	@Test
	public void aCreatureThePlayerDoesNotOwnIsIgnored()
	{
		game.setBankNear(true);

		assertFalse(game.reviveAtBank(new Creature("chicken", 3, now)));
		assertFalse(game.reviveAtBank((Creature) null));
	}

	@Test
	public void aSalveIsRefusedOnAFaintedCreatureAndSpendsNothing()
	{
		game.data().addItems(ItemType.LARGE_SALVE, 2);

		assertFalse(game.useItem(team, ItemType.LARGE_SALVE));

		assertTrue(team.fainted);
		assertEquals(2, game.data().itemCount(ItemType.LARGE_SALVE));
		assertTrue(messages.toString(), messages.contains("Chicken has fainted. Revive it first"));
	}

	@Test
	public void aReviveDraughtWorksAnywhereSpendsOneAndRevivesToHalf()
	{
		game.setBankNear(false);
		game.data().addItems(ItemType.REVIVE, 2);

		assertTrue(game.useItem(team, ItemType.REVIVE));

		assertFalse(team.fainted);
		assertEquals(0.5, team.hpFractionAt(now), EPSILON);
		assertEquals(1, game.data().itemCount(ItemType.REVIVE));
		assertTrue(messages.toString(), messages.contains("Chicken was revived to 50% health"));
	}

	@Test
	public void aReviveDraughtOnAStoredCreatureWorksToo()
	{
		game.data().addItems(ItemType.REVIVE, 1);

		assertTrue(game.useItem(stored, ItemType.REVIVE));

		assertFalse(stored.fainted);
	}

	@Test
	public void withNoDraughtNothingHappens()
	{
		assertFalse(game.useItem(team, ItemType.REVIVE));

		assertTrue(team.fainted);
		assertTrue(messages.toString(), messages.contains("No Revive draught left"));
	}

	@Test
	public void aDraughtOnACreatureThatIsUpAndRestedIsStillRefused()
	{
		game.data().addItems(ItemType.REVIVE, 1);
		team.revive(1.0, now);

		assertFalse(game.useItem(team, ItemType.REVIVE));
		assertEquals(1, game.data().itemCount(ItemType.REVIVE));
	}

	@Test
	public void aPickRevivesTheCreatureItPointsAt()
	{
		game.setBankNear(true);
		List<CreatureView> shown = game.snapshot().storage();
		TradePick pick = TradePick.of(Holding.STORAGE, 0, shown.get(0));

		assertTrue(game.reviveAtBank(pick));

		assertFalse(stored.fainted);
		assertTrue(team.fainted);
	}
}
