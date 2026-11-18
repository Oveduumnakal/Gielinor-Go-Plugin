/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Checks the states of a row: ordinary, Resting under a quarter of the health, and Fainted, which is dormant until
 * fainting exists and so is tested with a fake view.
 */
public class RowStateTest
{
	private static CreatureView view(double hp)
	{
		return PartyRig.creature("Goblin", 10, hp, null);
	}

	@Test
	public void aHealthyCreatureIsNormalAndKeepsAllItsColour()
	{
		RowModel row = RowModel.of(view(1.0), 2);

		assertEquals(RowModel.State.NORMAL, row.state());
		assertEquals(1.0, row.dim(), 0.0);
		assertFalse(row.desaturated());
		assertTrue(row.tags().isEmpty());
	}

	@Test
	public void underAQuarterOfTheHealthTheRowRestsDimmedAndWashedOut()
	{
		RowModel row = RowModel.of(view(0.24), 2);

		assertEquals(RowModel.State.RESTING, row.state());
		assertEquals(0.70, row.dim(), 0.0);
		assertTrue(row.desaturated());
		assertTrue(row.has(RowModel.Badge.RESTING));
		assertTrue(row.tags().contains("Resting"));
		assertEquals("24%", row.hpText());
	}

	@Test
	public void exactlyAQuarterIsStillNormal()
	{
		assertEquals(RowModel.State.NORMAL, RowModel.of(view(0.25), 2).state());
		assertEquals(RowModel.State.RESTING, RowModel.of(view(0.0), 2).state());
	}

	@Test
	public void aFaintedFakeViewGreysTheRowEmptiesTheBarAndSaysFainted()
	{
		RowModel row = RowModel.of(view(0.8).withFainted(true), 1);

		assertEquals(RowModel.State.FAINTED, row.state());
		assertEquals(0.0, row.hp(), 0.0);
		assertEquals("0%", row.hpText());
		assertEquals(0.55, row.dim(), 0.0);
		assertTrue(row.desaturated());
		assertTrue(row.tags().contains("Fainted"));
		assertTrue(row.tooltip(), row.tooltip().contains("Fainted"));
		assertFalse(row.has(RowModel.Badge.RESTING));
	}

	@Test
	public void faintedWinsOverRestingAndTheViewIsOtherwiseUntouched()
	{
		CreatureView down = view(0.1).withFainted(true);

		assertEquals(RowModel.State.FAINTED, RowModel.stateOf(down));
		assertFalse(view(0.1).fainted());
		assertTrue(down.fainted());
		assertEquals("Goblin", down.name());
		assertEquals(Gender.FEMALE, down.gender());
	}

	@Test
	public void theLeadKeepsItsMarkingWhileResting()
	{
		RowModel row = RowModel.of(view(0.1), 0);

		assertTrue(row.lead());
		assertTrue(row.has(RowModel.Badge.LEAD));
		assertTrue(row.has(RowModel.Badge.RESTING));
	}

	@Test
	public void healthAndLevelAndStyleComeFromTheView()
	{
		RowModel row = RowModel.of(view(0.46), 3);

		assertEquals("Lv 10", row.levelText());
		assertEquals(0.46, row.hp(), 0.0);
		assertEquals("46%", row.hpText());
		assertEquals(CombatType.SLASH, row.style());
		assertEquals(0.4, row.xp(), 0.0);
	}
}
