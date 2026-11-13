/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the kill-count chat patterns and the egg chances. */
public class BossRulesTest
{
	@Test
	public void aKillCountLineNamesTheBoss()
	{
		assertEquals("Vorkath", BossRules.killedName("Your Vorkath kill count is: 12."));
		assertEquals("Giant Mole", BossRules.killedName("Your Giant Mole kill count is: 1."));
		assertEquals("Kree'arra", BossRules.killedName("Your Kree'arra kill count is: 1,234."));
	}

	@Test
	public void colourTagsAndThousandsSeparatorsAreAllowed()
	{
		assertEquals("Zulrah",
			BossRules.killedName("Your <col=ef1020>Zulrah</col> kill count is: <col=ff0000>1,234</col>."));
		assertEquals("Zulrah", BossRules.killedName("<col=ef1020>Your Zulrah kill count is: 12,345,678.</col>"));
	}

	@Test
	public void aCompletedRaidNamesTheRaid()
	{
		assertEquals("Chambers of Xeric", BossRules.killedName("Your completed Chambers of Xeric count is: 5."));
		assertEquals("Chambers of Xeric Challenge Mode",
			BossRules.killedName("Your completed Chambers of Xeric Challenge Mode count is: 1,005."));
		assertEquals("Theatre of Blood", BossRules.killedName("Your completed Theatre of Blood count is: 2."));
	}

	@Test
	public void otherLinesAreIgnored()
	{
		assertNull(BossRules.killedName(null));
		assertNull(BossRules.killedName(""));
		assertNull(BossRules.killedName("You found a Vorkath egg!"));
		assertNull(BossRules.killedName("Your Vorkath kill count is: many."));
		assertNull(BossRules.killedName("Your Vorkath kill count is: 12. Nice one"));
		assertNull(BossRules.killedName("Oh, your Vorkath kill count is: 12."));
		assertNull(BossRules.killedName("Your completed raids count is high."));
	}

	@Test
	public void theFirstKillAlwaysDrops()
	{
		Random random = new Random(1);
		for (int i = 0; i < 200; i++)
			assertTrue(BossRules.eggDrops(true, random));
	}

	@Test
	public void laterKillsDropAboutOneInTwenty()
	{
		assertEquals(0.05, BossRules.LATER_CHANCE, 0.0);
		Random random = new Random(42);
		int drops = 0;
		for (int i = 0; i < 20_000; i++)
		{
			if (BossRules.eggDrops(false, random))
				drops++;
		}

		assertTrue("drops " + drops, drops > 800 && drops < 1200);
	}
}
