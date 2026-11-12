/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** Checks what a nickname may look like. */
public class NicknameTest
{
	@Test
	public void spacesAreTrimmedAndCollapsed()
	{
		assertEquals("Big Bob", Nickname.clean("  Big    Bob  "));
	}

	@Test
	public void lengthOneAndTwelvePassButThirteenFails()
	{
		assertEquals("A", Nickname.clean("A"));
		assertEquals("123456789012", Nickname.clean("123456789012"));
		assertNull(Nickname.clean("1234567890123"));
	}

	@Test
	public void lettersNumbersSpacesAndHyphensAreAllowed()
	{
		assertEquals("Mr-Bob 2", Nickname.clean("Mr-Bob 2"));
	}

	@Test
	public void markupAndSymbolsAreRefused()
	{
		assertNull(Nickname.clean("<b>Bob"));
		assertNull(Nickname.clean("Bob&Al"));
		assertNull(Nickname.clean("Bob!"));
		assertNull(Nickname.clean("Böb"));
	}

	@Test
	public void emptyOrMissingGivesNull()
	{
		assertNull(Nickname.clean(""));
		assertNull(Nickname.clean("    "));
		assertNull(Nickname.clean(null));
	}
}
