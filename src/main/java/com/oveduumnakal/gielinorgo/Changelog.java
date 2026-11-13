/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The bundled {@code changelog.md} as a list of releases, newest first. Each release starts with a top-level
 * {@code # <version> - <date>} heading, and everything up to the next one is that release's markdown body: a Quick
 * Overview, a Detailed Breakdown by area, and Bug Fixes. Lines that are HTML comments are left out. The parser is
 * pure and has no RuneLite types. The newest release's version is the plugin's current version, and
 * {@code ChangelogGuardTest} checks that it matches {@code runelite-plugin.properties}.
 */
final class Changelog
{
	/**
	 * Resource path of the bundled change log, relative to this class's package like {@code panel_icon.png}, so
	 * another plugin's change log can never be found in its place.
	 */
	static final String RESOURCE = "changelog.md";

	/** A release heading such as {@code # 1.0.0 - July 25 2026}; the date is optional and a single hash only. */
	private static final Pattern HEADING = Pattern.compile("^#(?!#)\\s+(\\S+)(?:\\s*-\\s*(.*))?$");

	private final List<Release> releases;

	private Changelog(List<Release> releases)
	{
		this.releases = releases;
	}

	/** One release: its version, its written-out date and the markdown under its heading. */
	static final class Release
	{
		private final String version;
		private final String date;
		private final String body;

		private Release(String version, String date, String body)
		{
			this.version = version;
			this.date = date;
			this.body = body;
		}

		/** @return the version, such as {@code 1.0.0} */
		String version()
		{
			return version;
		}

		/** @return the date as written in the heading, or an empty text when the heading has none */
		String date()
		{
			return date;
		}

		/** @return the markdown beneath the heading, trimmed */
		String body()
		{
			return body;
		}
	}

	/**
	 * Loads and parses the bundled change log.
	 *
	 * @return the releases, or an empty change log when the resource is missing or unreadable
	 */
	static Changelog load()
	{
		try (InputStream in = Changelog.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
				return new Changelog(Collections.emptyList());

			return parse(read(in));
		}
		catch (IOException e)
		{
			return new Changelog(Collections.emptyList());
		}
	}

	/**
	 * Parses change log markdown.
	 *
	 * @param markdown the text
	 * @return the releases in document order, which is newest first in the bundled file
	 */
	static Changelog parse(String markdown)
	{
		List<Release> releases = new ArrayList<>();
		String version = null;
		String date = "";
		StringBuilder body = new StringBuilder();
		for (String line : markdown.split("\n", -1))
		{
			Matcher heading = HEADING.matcher(line.replace("\r", ""));
			if (heading.matches())
			{
				if (version != null)
					releases.add(new Release(version, date, body.toString().trim()));

				version = heading.group(1);
				date = heading.group(2) == null ? "" : heading.group(2).trim();
				body = new StringBuilder();
			}
			else if (version != null && !line.trim().startsWith("<!--"))
				body.append(line).append('\n');
		}

		if (version != null)
			releases.add(new Release(version, date, body.toString().trim()));

		return new Changelog(releases);
	}

	/** @return the releases, newest first */
	List<Release> releases()
	{
		return Collections.unmodifiableList(releases);
	}

	/** @return the newest release's version, or null when the change log is empty */
	String currentVersion()
	{
		return releases.isEmpty() ? null : releases.get(0).version();
	}

	/**
	 * Tells whether a release exists.
	 *
	 * @param version the version to look for
	 * @return true when some release has exactly this version
	 */
	boolean hasVersion(String version)
	{
		for (Release release : releases)
		{
			if (release.version().equals(version))
				return true;
		}

		return false;
	}

	private static String read(InputStream in) throws IOException
	{
		StringBuilder text = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			String line = reader.readLine();
			while (line != null)
			{
				text.append(line).append('\n');
				line = reader.readLine();
			}
		}

		return text.toString();
	}
}
