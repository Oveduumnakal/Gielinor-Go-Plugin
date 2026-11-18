/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the rules that keep the XP snapshot from being written at the wrong time. */
public class XpLedgerTest
{
	@Test
	public void firstReadingIsKeptSeparateFromTheLatest()
	{
		XpLedger ledger = new XpLedger();

		ledger.read("MINING", 100);
		ledger.read("MINING", 250);

		assertEquals(Integer.valueOf(100), ledger.login().get("MINING"));
		assertTrue(ledger.reconcileDue());
	}

	@Test
	public void overallIsNotRead()
	{
		XpLedger ledger = new XpLedger();

		ledger.read("OVERALL", 5_000);

		assertFalse(ledger.reconcileDue());
		assertTrue(ledger.login().isEmpty());
	}

	@Test
	public void nothingIsDueBeforeAnyReading()
	{
		assertFalse(new XpLedger().reconcileDue());
	}

	@Test
	public void nothingIsStampedBeforeTheLoginIsReconciled()
	{
		XpLedger ledger = new XpLedger();
		ledger.worldOk(true);
		ledger.read("MINING", 100);

		assertNull(ledger.stamp());

		ledger.markReconciled();

		assertFalse(ledger.reconcileDue());
		assertEquals(Integer.valueOf(100), ledger.stamp().get("MINING"));
	}

	@Test
	public void theStampIsTheLatestReading()
	{
		XpLedger ledger = new XpLedger();
		ledger.worldOk(true);
		ledger.read("MINING", 100);
		ledger.markReconciled();
		ledger.read("MINING", 340);

		assertEquals(Integer.valueOf(340), ledger.stamp().get("MINING"));
		assertEquals(Integer.valueOf(100), ledger.login().get("MINING"));
	}

	@Test
	public void nothingIsStampedOnASeasonalWorld()
	{
		XpLedger ledger = new XpLedger();
		ledger.read("MINING", 100);
		ledger.markReconciled();
		ledger.worldOk(false);

		assertNull(ledger.stamp());
		assertFalse(ledger.worldOk());
	}

	@Test
	public void theStampIsACopy()
	{
		XpLedger ledger = new XpLedger();
		ledger.worldOk(true);
		ledger.read("MINING", 100);
		ledger.markReconciled();

		ledger.stamp().put("MINING", 1);

		assertEquals(Integer.valueOf(100), ledger.stamp().get("MINING"));
	}

	@Test
	public void seedFillsBothReadings()
	{
		XpLedger ledger = new XpLedger();
		Map<String, Integer> now = new HashMap<>();
		now.put("MINING", 900);
		now.put("OVERALL", 5_000);

		ledger.seed(now);

		assertEquals(Integer.valueOf(900), ledger.login().get("MINING"));
		assertFalse(ledger.login().containsKey("OVERALL"));
		assertTrue(ledger.reconcileDue());
	}

	@Test
	public void resetForgetsEverything()
	{
		XpLedger ledger = new XpLedger();
		ledger.worldOk(true);
		ledger.read("MINING", 100);
		ledger.markReconciled();

		ledger.reset();

		assertTrue(ledger.login().isEmpty());
		assertNull(ledger.stamp());
		assertFalse(ledger.reconcileDue());
		assertFalse(ledger.worldOk());
	}

	@Test
	public void aNewSaveNeedsReconcilingAgain()
	{
		XpLedger ledger = new XpLedger();
		ledger.worldOk(true);
		ledger.read("MINING", 100);
		ledger.markReconciled();

		ledger.markUnreconciled();

		assertTrue(ledger.reconcileDue());
		assertNull(ledger.stamp());
	}

	@Test
	public void anAccountSwitchCannotWriteTheNewAccountsXpIntoTheOldSave()
	{
		XpLedger ledger = new XpLedger();
		ledger.worldOk(true);
		ledger.read("MINING", 100);
		ledger.markReconciled();
		ledger.reset();

		ledger.read("MINING", 9_000_000);
		ledger.worldOk(true);

		assertNull("the old save is written before the new one is reconciled", ledger.stamp());
	}
}
