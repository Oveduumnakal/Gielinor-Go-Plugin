/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Fails the build when the Feedback form drifts from the GitHub issue templates it fills in. */
public class IssueTemplatesTest
{
	private static List<String> lines(String file) throws IOException
	{
		return Files.readAllLines(Paths.get(".github", "ISSUE_TEMPLATE", file), StandardCharsets.UTF_8);
	}

	private static void assertHasId(List<String> lines, String file, String id)
	{
		assertTrue(file + " has no field with id " + id, lines.contains("    id: " + id));
	}

	private static List<String> areaOptions(List<String> lines)
	{
		List<String> options = new ArrayList<>();
		int at = lines.indexOf("    id: area");
		assertTrue("no area field", at >= 0);
		boolean inOptions = false;
		for (int i = at + 1; i < lines.size(); i++)
		{
			String line = lines.get(i);
			if (line.startsWith("  - type:"))
				break;

			if (line.trim().equals("options:"))
				inOptions = true;
			else if (inOptions && line.trim().startsWith("- "))
				options.add(line.trim().substring(2));
			else if (inOptions && !line.trim().isEmpty())
				break;
		}

		return options;
	}

	@Test
	public void everyBugKeyIsATemplateField() throws IOException
	{
		List<String> lines = lines(IssueLinks.BUG_TEMPLATE);
		for (String id : Arrays.asList("description", "repro", "expected", "area", IssueLinks.VERSION_KEY))
			assertHasId(lines, IssueLinks.BUG_TEMPLATE, id);
	}

	@Test
	public void everyFeatureKeyIsATemplateField() throws IOException
	{
		List<String> lines = lines(IssueLinks.FEATURE_TEMPLATE);
		for (String id : Arrays.asList("problem", "solution", "area"))
			assertHasId(lines, IssueLinks.FEATURE_TEMPLATE, id);
	}

	@Test
	public void theAreaOptionsMatchTheTemplates() throws IOException
	{
		assertEquals(IssueLinks.BUG_AREAS, areaOptions(lines(IssueLinks.BUG_TEMPLATE)));
		assertEquals(IssueLinks.FEATURE_AREAS, areaOptions(lines(IssueLinks.FEATURE_TEMPLATE)));
	}

	@Test
	public void theTitlePrefixesMatchTheTemplates() throws IOException
	{
		assertTrue(lines(IssueLinks.BUG_TEMPLATE).contains("title: \"" + IssueLinks.BUG_PREFIX + "\""));
		assertTrue(lines(IssueLinks.FEATURE_TEMPLATE).contains("title: \"" + IssueLinks.FEATURE_PREFIX + "\""));
	}
}
