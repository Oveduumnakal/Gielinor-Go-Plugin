/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks the new-issue address the Feedback button opens. */
public class IssueLinksTest
{
	private static final String BASE = IssueLinks.NEW_ISSUE + "?template=bug_report.yml";

	@Test
	public void specialCharactersAreEncoded()
	{
		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("repro", "a & b #1\nline two");

		assertEquals(BASE + "&repro=a%20%26%20b%20%231%0Aline%20two",
			IssueLinks.build("bug_report.yml", "[Bug]: ", "", fields));
	}

	@Test
	public void nonAsciiTextIsEncodedAsUtf8()
	{
		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("description", "café");

		assertEquals(BASE + "&description=caf%C3%A9", IssueLinks.build("bug_report.yml", "[Bug]: ", null, fields));
	}

	@Test
	public void blankFieldsAndABlankTitleAreLeftOut()
	{
		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("description", "   ");
		fields.put("repro", null);
		fields.put("expected", "");

		assertEquals(BASE, IssueLinks.build("bug_report.yml", "[Bug]: ", "  ", fields));
	}

	@Test
	public void theTitleGetsThePrefix()
	{
		String url = IssueLinks.build("feature_request.yml", "[Feature]: ", " More jars ", new LinkedHashMap<>());

		assertEquals(IssueLinks.NEW_ISSUE + "?template=feature_request.yml&title=%5BFeature%5D%3A%20More%20jars", url);
	}

	@Test
	public void fieldsKeepTheirOrder()
	{
		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("problem", "p");
		fields.put("solution", "s");
		fields.put("area", "a");

		assertEquals(IssueLinks.NEW_ISSUE + "?template=feature_request.yml&problem=p&solution=s&area=a",
			IssueLinks.build("feature_request.yml", "[Feature]: ", "", fields));
	}

	@Test
	public void aLongFieldIsCutAtTheLimit()
	{
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < IssueLinks.MAX_FIELD + 500; i++)
			text.append('x');

		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("repro", text.toString());
		String url = IssueLinks.build("bug_report.yml", "[Bug]: ", "", fields);

		assertEquals(BASE.length() + "&repro=".length() + IssueLinks.MAX_FIELD, url.length());
	}

	@Test
	public void theCutNeverSplitsAPairOfChars()
	{
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < IssueLinks.MAX_FIELD - 1; i++)
			text.append('x');

		text.append("😀");
		String cut = IssueLinks.cut(text.toString());

		assertEquals(IssueLinks.MAX_FIELD - 1, cut.length());
		assertFalse(Character.isHighSurrogate(cut.charAt(cut.length() - 1)));
		assertTrue(IssueLinks.cut("short").equals("short"));
	}
}
