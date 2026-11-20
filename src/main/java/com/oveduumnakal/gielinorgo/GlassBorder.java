/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;

/**
 * The thin metallic border the Glass look draws round buttons and pop-ups, chosen with the Glass border setting. Each
 * is three colours of one metal, light at the top edge, middle, dark at the bottom edge, so a 1 pixel line reads as
 * polished metal. Holds no RuneLite types beyond what the setting needs.
 */
public enum GlassBorder
{
	/** Warm brass, the default: it sits well with the orange accent and the game's own palette. */
	BRONZE("Bronze", 0xF0CE86, 0xB8832F, 0x6A4719),

	/** Cool steel blue, the blue of the wild Info card's edge. */
	STEEL_BLUE("Steel blue", 0xD6F1FF, 0x7FD3F7, 0x3F7896);

	private final String label;
	private final Color high;
	private final Color middle;
	private final Color low;

	GlassBorder(String label, int high, int middle, int low)
	{
		this.label = label;
		this.high = new Color(high);
		this.middle = new Color(middle);
		this.low = new Color(low);
	}

	/** @return the colour at the top edge */
	Color high()
	{
		return high;
	}

	/** @return the colour half way down */
	Color middle()
	{
		return middle;
	}

	/** @return the colour at the bottom edge */
	Color low()
	{
		return low;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
