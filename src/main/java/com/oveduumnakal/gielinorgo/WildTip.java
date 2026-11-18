/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;

/**
 * Builds the text of the tooltip shown while the mouse rests on a wild creature, in RuneLite's tooltip markup:
 * {@code <col=rrggbb>} colour tags and {@code </br>} line breaks. Free of RuneLite types.
 */
final class WildTip
{
	/** The tooltip markup for a line break. */
	static final String LINE_BREAK = "</br>";

	/** The colour of a gilded creature's name and outline. */
	static final Color GILDED_COLOR = new Color(0xFFD23F);

	private WildTip()
	{
	}

	/**
	 * Builds the tooltip text: the name coloured by rarity (gold when gilded), then the level, style and rarity,
	 * then, when the species record is known, one line saying whether it is new, seen or caught.
	 *
	 * @param entry the wild creature
	 * @return the text, with no trailing line break
	 */
	static String text(WildEntry entry)
	{
		Species species = entry.species();
		Rarity rarity = species.rarity();
		Color nameColor = entry.gilded() ? GILDED_COLOR : rarity.color();
		String name = clean(entry.prefix() + species.name());
		StringBuilder text = new StringBuilder();
		text.append(colored(nameColor.getRGB() & 0xFFFFFF, name)).append(LINE_BREAK);
		text.append("Lv ").append(entry.level()).append(" - ").append(species.style().label()).append(" - ")
			.append(rarity.label());
		WildRecord record = entry.record();
		if (record != null)
			text.append(LINE_BREAK).append(colored(record.rgb(), record.tipText()));

		return text.toString();
	}

	private static String colored(int rgb, String words)
	{
		return String.format("<col=%06x>%s</col>", rgb, words);
	}

	private static String clean(String name)
	{
		return name.replace("<", "").replace(">", "");
	}
}
