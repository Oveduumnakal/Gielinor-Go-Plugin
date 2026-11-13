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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * A method reference such as {@code director::active} reads its field at once, so using it in {@code startUp}
 * before the line that sets the field throws and RuneLite refuses the plugin. Unit tests never start the plugin, so
 * this test reads {@code startUp} and checks that no field set there is referenced before it is set.
 */
public class StartUpOrderTest
{
	private static final Pattern ASSIGNED = Pattern.compile("^\\s*([a-z]\\w*) = ");

	private static final Pattern REFERENCED = Pattern.compile("\\b([a-z]\\w*)::");

	@Test
	public void noFieldIsReferencedBeforeStartUpSetsIt() throws IOException
	{
		String source = new String(Files.readAllBytes(
			Paths.get("src/main/java/com/oveduumnakal/gielinorgo/GielinorGoPlugin.java")), StandardCharsets.UTF_8);
		int start = source.indexOf("protected void startUp()");
		int end = source.indexOf("\n\t}\n", start);
		String[] lines = source.substring(start, end).split("\n");
		Map<String, Integer> setAt = new HashMap<>();
		for (int i = 0; i < lines.length; i++)
		{
			Matcher assigned = ASSIGNED.matcher(lines[i]);
			if (assigned.find())
				setAt.putIfAbsent(assigned.group(1), i);
		}

		List<String> early = new ArrayList<>();
		for (int i = 0; i < lines.length; i++)
		{
			Matcher referenced = REFERENCED.matcher(lines[i]);
			while (referenced.find())
			{
				Integer set = setAt.get(referenced.group(1));
				if (set != null && set >= i)
					early.add(lines[i].trim());
			}
		}

		assertEquals(new ArrayList<String>(), early);
	}
}
