/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The menu behind the painted window's Feedback button: Report a bug and Request a feature. Each opens GitHub's issue
 * form through {@link IssueLinks#build}; the player types the text there, and the plugin sends nothing itself. Holds
 * no RuneLite types.
 */
final class FeedbackLinks
{
	/** The label of the bug report item. */
	static final String BUG = "Report a bug";

	/** The label of the feature request item. */
	static final String FEATURE = "Request a feature";

	private FeedbackLinks()
	{
	}

	/**
	 * Builds the address of a bug report, with the plugin version filled in when it is known.
	 *
	 * @param version the plugin version, or null
	 * @return the address
	 */
	static String bugUrl(String version)
	{
		Map<String, String> fields = new LinkedHashMap<>();
		if (version != null)
			fields.put(IssueLinks.VERSION_KEY, version);

		return IssueLinks.build(IssueLinks.BUG_TEMPLATE, IssueLinks.BUG_PREFIX, "", fields);
	}

	/** @return the address of a feature request */
	static String featureUrl()
	{
		return IssueLinks.build(IssueLinks.FEATURE_TEMPLATE, IssueLinks.FEATURE_PREFIX, "", Collections.emptyMap());
	}

	/**
	 * Builds the menu's items.
	 *
	 * @param opener what opens an address in the browser
	 * @param version the plugin version, or null
	 * @return Report a bug, then Request a feature
	 */
	static List<ContextMenu.Item> menu(Consumer<String> opener, String version)
	{
		return Arrays.asList(
			ContextMenu.Item.of(BUG, () -> opener.accept(bugUrl(version))),
			ContextMenu.Item.of(FEATURE, () -> opener.accept(featureUrl())));
	}
}
