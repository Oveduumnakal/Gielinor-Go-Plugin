/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import net.runelite.client.config.ConfigItem;

import static org.junit.Assert.assertEquals;

/**
 * RuneLite shows a setting's description as a tooltip and breaks lines only at a newline, so one long sentence
 * becomes a very wide tooltip. This test checks that no line of any description is longer than 120 characters.
 */
public class ConfigTooltipTest
{
	private static final int MAX_LINE = 120;

	@Test
	public void noTooltipLineIsWiderThan120Characters()
	{
		List<String> tooWide = new ArrayList<>();
		for (Method method : GielinorGoConfig.class.getMethods())
		{
			ConfigItem item = method.getAnnotation(ConfigItem.class);
			if (item == null)
				continue;

			for (String line : item.description().split("\n"))
				if (line.length() > MAX_LINE)
					tooWide.add(method.getName() + ": " + line.length() + " chars");
		}

		assertEquals(new ArrayList<String>(), tooWide);
	}
}
