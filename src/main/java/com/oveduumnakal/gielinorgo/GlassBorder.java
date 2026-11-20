/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;

/**
 * The metal of the Glass look, chosen with the Glass border setting. It sets two things: the thin border round raised
 * and pressed-in elements and pop-ups, three colours of one metal (light, middle, dark) so a 1 pixel line reads as
 * polished metal; and the accent metal that stands for "active" in place of a flat orange (the primary button, the
 * lead row, experience bars, the active tab's label and underline). Bronze keeps the orange accent, steel blue has a
 * silver one. Holds no RuneLite types beyond what the setting needs.
 */
public enum GlassBorder
{
	/** Warm brass with the orange accent: the default, close to the game's own palette. */
	BRONZE("Bronze", new int[] {0xF0CE86, 0xB8832F, 0x6A4719}, new int[] {0xF5B03A, 0xDC8A00, 0x9C5C00},
		0xFFB000, 0xFFD466, 0x1E1400),

	/**
	 * Cool steel blue, the blue of the wild Info card's edge, with a neutral silver accent. The silver is kept off blue
	 * so the jar bar, which is light blue, stays different from the experience bar.
	 */
	STEEL_BLUE("Steel blue", new int[] {0xD6F1FF, 0x7FD3F7, 0x3F7896}, new int[] {0xF1F1F0, 0xA9A9A7, 0x6E6E6D},
		0xC9C9C7, 0xE9E9E8, 0x181818);

	private final String label;
	private final Color[] border;
	private final Color[] accent;
	private final int lead;
	private final int leadEdge;
	private final int accentText;

	GlassBorder(String label, int[] border, int[] accent, int lead, int leadEdge, int accentText)
	{
		this.label = label;
		this.border = colors(border);
		this.accent = colors(accent);
		this.lead = lead;
		this.leadEdge = leadEdge;
		this.accentText = accentText;
	}

	private static Color[] colors(int[] rgb)
	{
		Color[] out = new Color[rgb.length];
		for (int i = 0; i < rgb.length; i++)
			out[i] = new Color(rgb[i]);

		return out;
	}

	/** @return the colour of the border at the top edge */
	Color high()
	{
		return border[0];
	}

	/** @return the colour of the border half way down */
	Color middle()
	{
		return border[1];
	}

	/** @return the colour of the border at the bottom edge */
	Color low()
	{
		return border[2];
	}

	/** @return the light end of the accent metal: the top of an accent fill, the active tab's label */
	Color accentHigh()
	{
		return accent[0];
	}

	/** @return the body colour of the accent metal, the flat accent of the solid look */
	Color accentMid()
	{
		return accent[1];
	}

	/** @return the dark end of the accent metal: the bottom of an accent fill */
	Color accentLow()
	{
		return accent[2];
	}

	/** @return the fill of the lead row as an RGB value */
	int leadRgb()
	{
		return lead;
	}

	/** @return the edge of the lead row as an RGB value */
	int leadEdgeRgb()
	{
		return leadEdge;
	}

	/** @return the colour of the text on the accent, as an RGB value */
	int accentTextRgb()
	{
		return accentText;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
