/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/** Checks the text the wild creature hover tooltip shows. */
public class WildTipTest
{
	private static final Species IMP = new Species("Imp", 7, CombatType.STAB);

	private static WildEntry plain(int level)
	{
		return new WildEntry(1, IMP, level, level);
	}

	@Test
	public void aPlainCommonCreatureHasTwoLinesAndNoTrailingBreak()
	{
		String text = WildTip.text(plain(5));

		assertEquals("<col=c8c8c8>Imp</col></br>Lv 5 - Stab - Common", text);
	}

	@Test
	public void aGildedCreatureIsGoldAndSaysGilded()
	{
		WildEntry entry = new WildEntry(1, IMP, 5, 5, false, true);

		assertEquals("<col=ffd23f>Gilded Imp</col></br>Lv 5 - Stab - Common", WildTip.text(entry));
	}

	@Test
	public void aRoamingCreatureSaysRoamingInItsRarityColour()
	{
		WildEntry entry = new WildEntry(1, IMP, 5, 5, false, false, true);

		assertEquals("<col=c8c8c8>Roaming Imp</col></br>Lv 5 - Stab - Common", WildTip.text(entry));
	}

	@Test
	public void aGildedRoamingCreatureSaysBothInGold()
	{
		WildEntry entry = new WildEntry(1, IMP, 5, 5, false, true, true);

		assertEquals("<col=ffd23f>Gilded Roaming Imp</col></br>Lv 5 - Stab - Common", WildTip.text(entry));
	}

	@Test
	public void theLineUsesTheCreatureLevelNotTheSpeciesLevel()
	{
		Species rare = new Species("Dragon", 120, CombatType.MAGIC);
		WildEntry entry = new WildEntry(2, rare, 90, 120);

		assertEquals("<col=4aa3ff>Dragon</col></br>Lv 90 - Magic - Rare", WildTip.text(entry));
	}

	@Test
	public void aNewSpeciesAddsItsRecordLine()
	{
		WildEntry entry = plain(5);
		entry.record(WildRecord.of(0, true));

		assertEquals("<col=c8c8c8>Imp</col></br>Lv 5 - Stab - Common</br>"
			+ "<col=ffe066>New to you: first time you meet this species</col>", WildTip.text(entry));
	}

	@Test
	public void aSeenSpeciesAddsItsRecordLine()
	{
		WildEntry entry = plain(5);
		entry.record(WildRecord.of(0, false));

		assertEquals("<col=c8c8c8>Imp</col></br>Lv 5 - Stab - Common</br>"
			+ "<col=b0c4de>Seen before, not caught yet</col>", WildTip.text(entry));
	}

	@Test
	public void aCaughtSpeciesAddsItsRecordLineWithTheCount()
	{
		WildEntry entry = plain(5);
		entry.record(WildRecord.of(3, false));

		assertEquals("<col=c8c8c8>Imp</col></br>Lv 5 - Stab - Common</br>"
			+ "<col=8be28b>Caught before (x3)</col>", WildTip.text(entry));
	}

	@Test
	public void angleBracketsInANameCannotBreakTheMarkup()
	{
		Species odd = new Species("<col=ff0000>Imp</col>", 7, CombatType.STAB);
		String text = WildTip.text(new WildEntry(3, odd, 7, 7));

		assertEquals("<col=c8c8c8>col=ff0000Imp/col</col></br>Lv 7 - Stab - Common", text);
		assertFalse(text.contains("<col=ff0000>"));
	}
}
