/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the change log parser and the bundled file. */
public class ChangelogTest
{
	@Test
	public void twoReleasesAreParsedNewestFirst()
	{
		Changelog log = Changelog.parse("# 1.1.0 - March 2 2027\n\n## Quick Overview\nNew.\n\n"
			+ "# 1.0.0 - January 5 2027\n\n- **Old.** Text.\n");

		Changelog.Release first = log.releases().get(0);
		Changelog.Release second = log.releases().get(1);

		assertEquals(2, log.releases().size());
		assertEquals("1.1.0", log.currentVersion());
		assertEquals("March 2 2027", first.date());
		assertEquals("## Quick Overview\nNew.", first.body());
		assertEquals("- **Old.** Text.", second.body());
		assertTrue(log.hasVersion("1.0.0"));
		assertFalse(log.hasVersion("0.9.0"));
	}

	@Test
	public void subHeadingsAreNotReleaseBoundaries()
	{
		Changelog log = Changelog.parse("# 1.0.0 - Today\n## Area\n### Sub\ntext\n");

		Changelog.Release first = log.releases().get(0);

		assertEquals(1, log.releases().size());
		assertEquals("## Area\n### Sub\ntext", first.body());
	}

	@Test
	public void aHeadingWithoutADateHasAnEmptyDate()
	{
		Changelog log = Changelog.parse("# 2.0.0\nbody\n");

		Changelog.Release first = log.releases().get(0);

		assertEquals("2.0.0", log.currentVersion());
		assertEquals("", first.date());
	}

	@Test
	public void anEmptyFileHasNoReleases()
	{
		Changelog log = Changelog.parse("");

		assertTrue(log.releases().isEmpty());
		assertNull(log.currentVersion());
		assertFalse(log.hasVersion("1.0.0"));
	}

	@Test
	public void htmlCommentLinesAreDropped()
	{
		Changelog log = Changelog.parse("# 1.0.0 - Today\n- one\n<!-- add lines above -->\n");
		Changelog.Release first = log.releases().get(0);

		assertEquals("- one", first.body());
	}

	@Test
	public void theBundledFileIsWellFormed()
	{
		Changelog log = Changelog.load();
		Set<String> seen = new HashSet<>();

		assertFalse("changelog.md did not load", log.releases().isEmpty());
		for (Changelog.Release release : log.releases())
		{
			assertTrue("duplicate version " + release.version(), seen.add(release.version()));
			assertFalse("no date for " + release.version(), release.date().isEmpty());
			assertTrue("no Quick Overview for " + release.version(), release.body().contains("## Quick Overview"));
			assertTrue("no Detailed Breakdown for " + release.version(),
				release.body().contains("## Detailed Breakdown"));
		}
	}
}
