/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Checks that every item's tooltip names its effect. */
public class BagTextTest
{
	@Test
	public void everyItemHasATooltipThatNamesItsEffect()
	{
		assertTrue(BagText.itemTip(ItemType.SMALL_SALVE, 2).contains("Restores 25% health"));
		assertTrue(BagText.itemTip(ItemType.LARGE_SALVE, 0).contains("Restores 60% health"));
		assertTrue(BagText.itemTip(ItemType.REVIVE, 1).contains("50% health"));
		assertTrue(BagText.itemTip(ItemType.ANTIDOTE, 1).contains("Cures poison, venom and burn"));
		assertEquals(4, ItemType.values().length);
	}
}
