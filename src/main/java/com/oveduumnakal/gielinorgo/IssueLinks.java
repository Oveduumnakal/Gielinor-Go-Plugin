/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Builds the address of GitHub's new-issue page with a template and its fields already filled in. The player reviews
 * and posts the issue in the browser, so the plugin sends nothing itself. Pure, so it is easy to test.
 */
final class IssueLinks
{
	/** GitHub's new-issue page for this plugin. */
	static final String NEW_ISSUE = "https://github.com/Oveduumnakal/Gielinor-Go-Plugin/issues/new";

	/** File name of the bug report template. */
	static final String BUG_TEMPLATE = "bug_report.yml";

	/** File name of the feature request template. */
	static final String FEATURE_TEMPLATE = "feature_request.yml";

	/** Start of every bug report title, as in the template. */
	static final String BUG_PREFIX = "[Bug]: ";

	/** Start of every feature request title, as in the template. */
	static final String FEATURE_PREFIX = "[Feature]: ";

	/** The template field that takes the plugin version of a bug report. */
	static final String VERSION_KEY = "plugin-version";

	/** The most characters one field or title may carry into the address; the rest is cut. */
	static final int MAX_FIELD = 2000;

	/** The options of the bug report's area drop-down, exactly as the template writes them. */
	static final List<String> BUG_AREAS = Collections.unmodifiableList(Arrays.asList(
		"Wild creatures / catching",
		"Battles / training",
		"Side panel (team, storage, creature log)",
		"Saved progress",
		"Configuration / settings",
		"Other"));

	/** The options of the feature request's area drop-down, exactly as the template writes them. */
	static final List<String> FEATURE_AREAS = Collections.unmodifiableList(Arrays.asList(
		"Wild creatures / catching",
		"Battles / training",
		"Side panel (team, storage, creature log)",
		"Saved progress",
		"Configuration / settings",
		"New / other"));

	private IssueLinks()
	{
	}

	/**
	 * Builds the address.
	 *
	 * @param template the template file name, such as {@link #BUG_TEMPLATE}
	 * @param titlePrefix the text every title starts with
	 * @param title what the player typed as the title
	 * @param fields the template field ids and what the player typed, in the order they should appear
	 * @return the address; a blank title and blank fields are left out and long text is cut
	 */
	static String build(String template, String titlePrefix, String title, Map<String, String> fields)
	{
		StringBuilder url = new StringBuilder(NEW_ISSUE + "?template=" + encode(template));
		if (title != null && !title.trim().isEmpty())
			url.append("&title=" + encode(titlePrefix + cut(title.trim())));

		for (Map.Entry<String, String> field : fields.entrySet())
		{
			String value = field.getValue();
			if (value != null && !value.trim().isEmpty())
				url.append("&" + encode(field.getKey()) + "=" + encode(cut(value.trim())));
		}

		return url.toString();
	}

	/**
	 * Cuts a text to {@link #MAX_FIELD} characters without splitting a character that takes two.
	 *
	 * @param text the text
	 * @return the text, or its first {@link #MAX_FIELD} characters
	 */
	static String cut(String text)
	{
		if (text.length() <= MAX_FIELD)
			return text;

		int end = MAX_FIELD;
		if (Character.isHighSurrogate(text.charAt(end - 1)))
			end--;

		return text.substring(0, end);
	}

	private static String encode(String text)
	{
		try
		{
			return URLEncoder.encode(text, "UTF-8").replace("+", "%20");
		}
		catch (UnsupportedEncodingException e)
		{
			throw new IllegalStateException(e);
		}
	}
}
