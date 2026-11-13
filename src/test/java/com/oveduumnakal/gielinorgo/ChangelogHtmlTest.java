/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks how change log markdown becomes HTML. */
public class ChangelogHtmlTest
{
	@Test
	public void boldPairsBecomeBoldText()
	{
		assertEquals("a <b>b</b> c", ChangelogHtml.inline("a **b** c"));
	}

	@Test
	public void markupInTheTextIsEscaped()
	{
		assertEquals("&lt;i&gt; &amp; x", ChangelogHtml.inline("<i> & x"));
	}

	@Test
	public void bulletsHeadingsAndWrappedLinesRender()
	{
		Changelog log = Changelog.parse("# 1.0.0 - Today\n## Area\n### Sub\n- **One.** First\n  line two.\n- Two.\n");
		String html = ChangelogHtml.render(log.releases().get(0), 160);

		assertTrue(html.contains("<u><b>Area</b></u>"));
		assertTrue(html.contains("<br><b>Sub</b>"));
		assertTrue(html.contains("&bull; <b>One.</b> First line two.<br>"));
		assertTrue(html.contains("&bull; Two.<br>"));
		assertFalse(html.contains("**"));
	}
}
