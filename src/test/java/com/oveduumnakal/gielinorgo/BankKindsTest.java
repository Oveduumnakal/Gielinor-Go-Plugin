/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks which bankers, booths and chests count as a bank. */
public class BankKindsTest
{
	@Test
	public void aBankerAndABoothHaveABankAction()
	{
		assertTrue(BankKinds.npcOffersBank(new String[] {"Talk-to", "Bank", "Collect", null, null}));
		assertTrue(BankKinds.objectOffersBank("Bank booth", new String[] {null, "Bank", "Collect", null, null}));
	}

	@Test
	public void theActionMatchesWhateverTheCaseOrSpacing()
	{
		assertTrue(BankKinds.npcOffersBank(new String[] {" bank "}));
		assertTrue(BankKinds.objectOffersBank("Booth", new String[] {"BANK"}));
	}

	@Test
	public void aChestThatOnlyHasUseCountsWhenItsNameSaysBank()
	{
		assertTrue(BankKinds.objectOffersBank("Bank chest", new String[] {"Use", null, null, null, null}));
		assertTrue(BankKinds.objectOffersBank("Bank chest", new String[] {"Open", "Collect"}));
		assertTrue(BankKinds.objectOffersBank("BANK CHEST", new String[] {"Use"}));
		assertFalse(BankKinds.objectOffersBank("Chest", new String[] {"Use"}));
	}

	@Test
	public void depositBoxesAndLookalikesDoNotCount()
	{
		assertFalse(BankKinds.objectOffersBank("Bank deposit box", new String[] {"Deposit", "Use"}));
		assertFalse(BankKinds.objectOffersBank("Riverbank", new String[] {"Use"}));
		assertFalse(BankKinds.objectOffersBank("Bank sign", new String[] {null, null}));
		assertFalse(BankKinds.objectOffersBank("Bank sign", null));
	}

	@Test
	public void missingDataIsNotABank()
	{
		assertFalse(BankKinds.npcOffersBank(null));
		assertFalse(BankKinds.npcOffersBank(new String[] {"Talk-to", null}));
		assertFalse(BankKinds.objectOffersBank(null, null));
		assertFalse(BankKinds.objectOffersBank(null, new String[] {"Use"}));
	}
}
