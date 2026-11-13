/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks when the change log button reads What's New. */
public class WhatsNewTest
{
	@Test
	public void aFirstInstallIsSilent()
	{
		assertFalse(WhatsNew.shouldShow("", "1.0.0", false));
		assertFalse(WhatsNew.shouldShow(null, "1.0.0", false));
		assertTrue(WhatsNew.recordSilently("", "1.0.0"));
		assertTrue(WhatsNew.recordSilently(null, "1.0.0"));
		assertFalse(WhatsNew.recordSilently("0.9.0", "1.0.0"));
		assertFalse(WhatsNew.recordSilently("", null));
	}

	@Test
	public void aNewVersionShows()
	{
		assertTrue(WhatsNew.shouldShow("0.9.0", "1.0.0", false));
	}

	@Test
	public void theSameVersionDoesNotShow()
	{
		assertFalse(WhatsNew.shouldShow("1.0.0", "1.0.0", false));
	}

	@Test
	public void aDismissedVersionDoesNotShow()
	{
		assertFalse(WhatsNew.shouldShow("0.9.0", "1.0.0", true));
	}
}
