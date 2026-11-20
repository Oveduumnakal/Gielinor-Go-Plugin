/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

/**
 * The look of the painted window: one value per {@link ThemeColor}, and nothing else. Shapes, sizes, fonts and hit
 * areas do not depend on the theme, so any style lays out the same. This is the only class that writes colour
 * literals; the paint code reads {@link #color}. Adding a style is one more constant that fills every name,
 * and {@code WindowThemeTest} checks that it did and that its text is readable. It also holds the colour maths the
 * paint code needs (mixing, alpha and contrast). Holds no RuneLite types.
 */
public enum WindowTheme
{
	/** RuneLite's own grey and orange, with an amber lead row, painted as solid fills. */
	RUNELITE_DARK("RuneLite dark", runeLitePalette(null), false, GlassBorder.BRONZE),

	/**
	 * The same colours as {@link #RUNELITE_DARK}, painted as tinted glass the game shows through
	 * (#324). Switched on by the Glass look setting.
	 */
	RUNELITE_GLASS("RuneLite glass", runeLitePalette(GlassBorder.BRONZE), true, GlassBorder.BRONZE),

	/** {@link #RUNELITE_GLASS} with the steel blue border, for the Glass border setting. */
	RUNELITE_GLASS_STEEL("RuneLite glass, steel border", runeLitePalette(GlassBorder.STEEL_BLUE), true,
		GlassBorder.STEEL_BLUE);

	private final String label;
	private final Map<ThemeColor, Color> colors;
	private final boolean glass;
	private final GlassBorder border;

	WindowTheme(String label, Palette palette, boolean glass, GlassBorder border)
	{
		this.label = label;
		this.colors = palette.colors;
		this.glass = glass;
		this.border = border;
	}

	/**
	 * The one place the Glass look and Glass border settings pick a style, so the window and its overlays always
	 * agree.
	 *
	 * @param glass the Glass look setting
	 * @param border the Glass border setting
	 * @return the glass style with that border when on, {@link #RUNELITE_DARK} when off
	 */
	static WindowTheme of(boolean glass, GlassBorder border)
	{
		if (!glass)
			return RUNELITE_DARK;

		return border == GlassBorder.STEEL_BLUE ? RUNELITE_GLASS_STEEL : RUNELITE_GLASS;
	}

	/** @return the metal border glass buttons and pop-ups wear; unused by the solid look */
	GlassBorder border()
	{
		return border;
	}

	/** @return true when the window is painted as tinted glass, false for the solid fills */
	public boolean glass()
	{
		return glass;
	}

	/**
	 * @param name which colour
	 * @return the theme's colour of that name; every name is filled in every theme
	 */
	Color color(ThemeColor name)
	{
		return colors.get(name);
	}

	@Override
	public String toString()
	{
		return label;
	}

	/**
	 * Mixes two colours, channel by channel, alpha included.
	 *
	 * @param from the colour at 0
	 * @param to the colour at 1
	 * @param amount how far toward {@code to}, kept between 0 and 1
	 * @return the mix
	 */
	static Color mix(Color from, Color to, double amount)
	{
		double t = Math.max(0.0, Math.min(1.0, amount));
		return new Color(
			channel(from.getRed(), to.getRed(), t),
			channel(from.getGreen(), to.getGreen(), t),
			channel(from.getBlue(), to.getBlue(), t),
			channel(from.getAlpha(), to.getAlpha(), t));
	}

	/**
	 * Gives a colour a share of its own opacity, for fades.
	 *
	 * @param color the colour
	 * @param factor the share of its alpha to keep, kept between 0 and 1
	 * @return the colour with its alpha multiplied by the factor
	 */
	static Color fade(Color color, double factor)
	{
		double f = Math.max(0.0, Math.min(1.0, factor));
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) Math.round(color.getAlpha() * f));
	}

	/**
	 * Lays a colour over an opaque one, as the screen would show it.
	 *
	 * @param top the colour on top, with its alpha
	 * @param under the colour below, taken as opaque
	 * @return the opaque result
	 */
	static Color over(Color top, Color under)
	{
		double a = top.getAlpha() / 255.0;
		return new Color(
			channel(under.getRed(), top.getRed(), a),
			channel(under.getGreen(), top.getGreen(), a),
			channel(under.getBlue(), top.getBlue(), a));
	}

	/**
	 * Gives the WCAG contrast ratio of two opaque colours.
	 *
	 * @param a one colour
	 * @param b the other colour
	 * @return the ratio, from 1 (no contrast) to 21 (black on white)
	 */
	static double contrast(Color a, Color b)
	{
		double la = luminance(a);
		double lb = luminance(b);
		return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
	}

	private static double luminance(Color c)
	{
		return 0.2126 * linear(c.getRed()) + 0.7152 * linear(c.getGreen()) + 0.0722 * linear(c.getBlue());
	}

	private static double linear(int channel)
	{
		double c = channel / 255.0;
		return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
	}

	private static int channel(int from, int to, double t)
	{
		return (int) Math.round(from + (to - from) * t);
	}

	/**
	 * Builds the RuneLite grey palette. The solid look has RuneLite's own flat orange for its accent; a glass style
	 * takes the accent from its metal, so the Glass border setting can change it without touching the solid look.
	 *
	 * @param metal the glass metal, or null for the solid look
	 * @return the palette
	 */
	private static Palette runeLitePalette(GlassBorder metal)
	{
		int accent = metal == null ? 0xDC8A00 : metal.accentMid().getRGB();
		int lead = metal == null ? 0xFFB000 : metal.leadRgb();
		int leadEdge = metal == null ? 0xFFD466 : metal.leadEdgeRgb();
		int accentText = metal == null ? 0x1E1400 : metal.accentTextRgb();
		return new Palette()
		.put(ThemeColor.WINDOW, 0x1E1E1E, 240)
		.put(ThemeColor.FRAME, 0x464646)
		.put(ThemeColor.HEADER, 0x151515)
		.put(ThemeColor.TAB, 0x282828)
		.put(ThemeColor.TAB_ACTIVE, 0x3E3E3E)
		.put(ThemeColor.TAB_DISABLED, 0x1A1A1A)
		.put(ThemeColor.TAB_TEXT, 0xDCDCDC)
		.put(ThemeColor.TAB_ACTIVE_TEXT, 0xFFFFFF)
		.put(ThemeColor.TAB_DISABLED_TEXT, 0x6E6E6E)
		.put(ThemeColor.ROW, 0x282828)
		.put(ThemeColor.ROW_BORDER, 0x404040)
		.put(ThemeColor.ROW_TEXT, 0xFFFFFF)
		.put(ThemeColor.LEAD_ROW, lead)
		.put(ThemeColor.LEAD_BORDER, leadEdge)
		.put(ThemeColor.LEAD_TEXT, accentText)
		.put(ThemeColor.PORTRAIT_BACKDROP, 0x181818)
		.put(ThemeColor.TEXT, 0xEBEBEB)
		.put(ThemeColor.SUBTEXT, 0xB4B4B4)
		.put(ThemeColor.HP_FILL, 0x50C878)
		.put(ThemeColor.HP_WARNING, 0xE6BE3C)
		.put(ThemeColor.HP_DANGER, 0xE64646)
		.put(ThemeColor.HP_TRACK, 0x101010)
		.put(ThemeColor.XP_FILL, accent)
		.put(ThemeColor.JAR_FILL, 0x7FD3F0)
		.put(ThemeColor.XP_TRACK, 0x101010)
		.put(ThemeColor.ACCENT, accent)
		.put(ThemeColor.PANEL_BOX, 0x262626)
		.put(ThemeColor.PANEL_BORDER, 0x3C3C3C)
		.put(ThemeColor.TOOLTIP, 0x141414, 245)
		.put(ThemeColor.TOOLTIP_TEXT, 0xF0F0F0)
		.put(ThemeColor.NOTICE, 0x141414, 235)
		.put(ThemeColor.NOTICE_TEXT, 0xFFFFFF)
		.put(ThemeColor.CHIP, 0x3A3A3A)
		.put(ThemeColor.CHIP_TEXT, 0xF0F0F0)
		.put(ThemeColor.CHIP_ACCENT, accent)
		.put(ThemeColor.CHIP_ACCENT_TEXT, accentText)
		.put(ThemeColor.HOVER_SHADE, 0xFFFFFF, 26)
		.put(ThemeColor.PRESS_SHADE, 0x000000, 60)
		.put(ThemeColor.SHADOW, 0x000000, 90)
		.put(ThemeColor.FOCUS_RING, 0xFFB000)
		.put(ThemeColor.PARTY_HIGHLIGHT, 0x4FC3F7)
		.put(ThemeColor.INPUT, 0x141414)
		.put(ThemeColor.SELECTION, accent, 110)
		.put(ThemeColor.SCRIM, 0x000000, 150)
		.put(ThemeColor.SCROLL_TRACK, 0x101010, 160)
		.put(ThemeColor.SCROLL_THUMB, 0x6E6E6E)
		.put(ThemeColor.GOLD, 0xFFCC33);
	}

	/** Collects the colours of one theme while its constant is built. */
	private static final class Palette
	{
		private final Map<ThemeColor, Color> colors = new EnumMap<>(ThemeColor.class);

		Palette put(ThemeColor name, int rgb)
		{
			return put(name, rgb, 255);
		}

		Palette put(ThemeColor name, int rgb, int alpha)
		{
			colors.put(name, new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, alpha));
			return this;
		}
	}
}
