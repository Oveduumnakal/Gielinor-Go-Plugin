/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Fails the build when the top change log entry is not the version in {@code runelite-plugin.properties}. */
public class ChangelogGuardTest
{
	@Test
	public void theTopEntryIsThePluginVersion() throws IOException
	{
		Properties properties = new Properties();
		try (InputStream in = new FileInputStream("runelite-plugin.properties"))
		{
			properties.load(in);
		}

		assertEquals("Bump the version in runelite-plugin.properties and add a matching entry at the top of"
			+ " changelog.md together", properties.getProperty("version"), Changelog.load().currentVersion());
	}
}
