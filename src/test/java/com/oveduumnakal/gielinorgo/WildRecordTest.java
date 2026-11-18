/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Checks what a wild creature's species record says, and how a wild entry carries it. */
public class WildRecordTest
{
	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.STAB);

	@Test
	public void aSpeciesNeverCaughtAndFirstMetIsNew()
	{
		WildRecord record = WildRecord.of(0, true);

		assertEquals(WildRecord.Kind.NEW, record.kind());
		assertEquals(0, record.caughtCount());
	}

	@Test
	public void aSpeciesNeverCaughtButSeenBeforeIsSeen()
	{
		assertEquals(WildRecord.Kind.SEEN, WildRecord.of(0, false).kind());
	}

	@Test
	public void aCaughtSpeciesIsCaughtWhateverTheFirstMeetingWas()
	{
		WildRecord first = WildRecord.of(3, true);
		WildRecord later = WildRecord.of(1, false);

		assertEquals(WildRecord.Kind.CAUGHT, first.kind());
		assertEquals(3, first.caughtCount());
		assertEquals(WildRecord.Kind.CAUGHT, later.kind());
		assertEquals(1, later.caughtCount());
	}

	@Test
	public void aNegativeCountReadsAsNotCaught()
	{
		assertEquals(WildRecord.Kind.SEEN, WildRecord.of(-2, false).kind());
		assertEquals(0, WildRecord.of(-2, false).caughtCount());
	}

	@Test
	public void theTipTextsAreExact()
	{
		assertEquals("New to you: first time you meet this species", WildRecord.of(0, true).tipText());
		assertEquals("Seen before, not caught yet", WildRecord.of(0, false).tipText());
		assertEquals("Caught before (x3)", WildRecord.of(3, false).tipText());
		assertEquals("Caught before (x1)", WildRecord.of(1, true).tipText());
	}

	@Test
	public void theLabelsAreTheMenuWords()
	{
		assertEquals("New", WildRecord.of(0, true).label());
		assertEquals("Seen", WildRecord.of(0, false).label());
		assertEquals("Caught", WildRecord.of(2, false).label());
	}

	@Test
	public void theThreeColoursDiffer()
	{
		int caught = WildRecord.of(1, false).rgb();
		int fresh = WildRecord.of(0, true).rgb();
		int seen = WildRecord.of(0, false).rgb();

		assertEquals(0x8BE28B, caught);
		assertEquals(0xFFE066, fresh);
		assertEquals(0xB0C4DE, seen);
		assertNotEquals(caught, fresh);
		assertNotEquals(caught, seen);
		assertNotEquals(fresh, seen);
	}

	@Test
	public void recordsAreEqualWhenKindAndCountMatch()
	{
		assertEquals(WildRecord.of(2, true), WildRecord.of(2, false));
		assertEquals(WildRecord.of(2, true).hashCode(), WildRecord.of(2, false).hashCode());
		assertNotEquals(WildRecord.of(2, true), WildRecord.of(3, true));
		assertNotEquals(WildRecord.of(0, true), WildRecord.of(0, false));
		assertFalse(WildRecord.of(0, true).equals(null));
		assertFalse(WildRecord.of(0, true).equals("New"));
	}

	@Test
	public void aNewEntryHasNoRecordYet()
	{
		WildEntry entry = new WildEntry(1, CHICKEN, 3, 1);

		assertNull(entry.record());
		assertFalse(entry.firstMeeting());
	}

	@Test
	public void anEntryKeepsTheRecordItIsGiven()
	{
		WildEntry entry = new WildEntry(1, CHICKEN, 3, 1);
		WildRecord record = WildRecord.of(0, true);

		entry.record(record);

		assertSame(record, entry.record());
	}

	@Test
	public void withFirstMeetingChainsAndSticks()
	{
		WildEntry entry = new WildEntry(1, CHICKEN, 3, 1);

		assertSame(entry, entry.withFirstMeeting(true));
		assertTrue(entry.firstMeeting());
		assertFalse(entry.withFirstMeeting(false).firstMeeting());
	}
}
