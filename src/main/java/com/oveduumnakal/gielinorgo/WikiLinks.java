/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/** Builds the Old School RuneScape Wiki address of a monster, for the Log's Open wiki choice. */
final class WikiLinks
{
	private static final String BASE = "https://oldschool.runescape.wiki/w/";

	private WikiLinks()
	{
	}

	/**
	 * Builds the wiki page address of a monster.
	 *
	 * @param name the species name
	 * @return the address, with spaces as underscores and every other special character percent-encoded
	 */
	static String urlFor(String name)
	{
		String page = name.trim().replaceAll("\\s+", "_");
		try
		{
			return BASE + URLEncoder.encode(page, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			throw new IllegalStateException(e);
		}
	}
}
