/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

/**
 * One colour for each jar, used as the base of its action bubble above a wild creature. The jar pictures are mostly
 * grey, so their own colours cannot tell the bubbles apart at a glance; these are chosen to be different hues. Every
 * colour differs from the Battle and Info bubble bases. Holds no RuneLite types.
 */
final class JarPalette
{
	/** The base colour of the Battle bubble. */
	static final Color BATTLE_BASE = new Color(0xD32F2F);

	/** The base colour of the Info bubble. */
	static final Color INFO_BASE = new Color(0x7FD3F7);

	private static final Color NEUTRAL = new Color(0x9E9E9E);
	private static final Map<JarType, Color> COLORS = new EnumMap<>(JarType.class);

	static
	{
		COLORS.put(JarType.BRONZE, new Color(0xC07A3A));
		COLORS.put(JarType.IRON, new Color(0xA7ADB4));
		COLORS.put(JarType.RUNE, new Color(0x3F8FD0));
		COLORS.put(JarType.DRAGON, new Color(0xB3261E));
		COLORS.put(JarType.CAVE, new Color(0x7D6B5D));
		COLORS.put(JarType.SWIFT, new Color(0xF2C230));
		COLORS.put(JarType.BLESSED, new Color(0xF3E9C6));
		COLORS.put(JarType.SLAYER, new Color(0x5C5C5C));
		COLORS.put(JarType.VENOM, new Color(0x3FA34D));
		COLORS.put(JarType.EMBER, new Color(0xEF6C00));
		COLORS.put(JarType.GIANT, new Color(0x8A9A3B));
		COLORS.put(JarType.TIDE, new Color(0x1FA3A3));
		COLORS.put(JarType.STAB, new Color(0xE06C75));
		COLORS.put(JarType.SLASH, new Color(0xC2185B));
		COLORS.put(JarType.CRUSH, new Color(0x8E5A3C));
		COLORS.put(JarType.ARCHER, new Color(0x2E7D32));
		COLORS.put(JarType.MYSTIC, new Color(0x5E35B1));
	}

	private JarPalette()
	{
	}

	/**
	 * Gives the base colour of a jar's bubble.
	 *
	 * @param jar the jar, or null
	 * @return its colour, or a neutral grey for null
	 */
	static Color of(JarType jar)
	{
		Color found = jar == null ? null : COLORS.get(jar);
		return found == null ? NEUTRAL : found;
	}
}
