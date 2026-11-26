package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

/** Theme colours changed in the lab. The lab's copy of {@code WindowTheme.color} asks here first. */
final class LabTheme
{
	private static final Map<String, Color> OVERRIDES = new LinkedHashMap<>();

	private LabTheme()
	{
	}

	static Map<String, WindowTheme> themes()
	{
		Map<String, WindowTheme> themes = new LinkedHashMap<>();
		themes.put("Solid", WindowTheme.RUNELITE_DARK);
		themes.put("Glass bronze", WindowTheme.RUNELITE_GLASS);
		themes.put("Glass steel", WindowTheme.RUNELITE_GLASS_STEEL);
		return themes;
	}

	private static String key(WindowTheme theme, ThemeColor name)
	{
		for (Map.Entry<String, WindowTheme> entry : themes().entrySet())
		{
			if (entry.getValue() == theme)
				return entry.getKey() + "/" + name.name();
		}

		return "?";
	}

	static Color override(WindowTheme theme, ThemeColor name)
	{
		return OVERRIDES.isEmpty() ? null : OVERRIDES.get(key(theme, name));
	}

	static void set(String key, Color color)
	{
		if (color == null)
			OVERRIDES.remove(key);
		else
			OVERRIDES.put(key, color);
	}

	static Map<String, Color> all()
	{
		return OVERRIDES;
	}
}
