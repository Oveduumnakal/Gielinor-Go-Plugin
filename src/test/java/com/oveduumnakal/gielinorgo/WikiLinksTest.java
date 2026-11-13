/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Checks the wiki addresses the Log's Open wiki choice builds. */
public class WikiLinksTest
{
	private static final String BASE = "https://oldschool.runescape.wiki/w/";

	@Test
	public void spacesBecomeUnderscores()
	{
		assertEquals(BASE + "Giant_rat", WikiLinks.urlFor("Giant rat"));
		assertEquals(BASE + "Goblin", WikiLinks.urlFor("  Goblin "));
	}

	@Test
	public void apostrophesAreEncoded()
	{
		assertEquals(BASE + "Scorpia%27s_offspring", WikiLinks.urlFor("Scorpia's offspring"));
	}

	@Test
	public void nonAsciiAndReservedCharactersAreEncoded()
	{
		assertEquals(BASE + "Tz-Kih", WikiLinks.urlFor("Tz-Kih"));
		assertEquals(BASE + "Ba%C3%B1ado", WikiLinks.urlFor("Bañado"));
		assertEquals(BASE + "A%26B%3F", WikiLinks.urlFor("A&B?"));
	}
}
