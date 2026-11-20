/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;

/**
 * Every number of the glass look in one place (#324): how strong the wash, the tint and the sheen are, how
 * big a surface's shadows and bevel are, and how pressed, lifted and accented elements are filled. Lengths are in
 * pixels at scale 1; paint code multiplies them by the style's scale. Opacities run from 0 to 1. Holds no RuneLite
 * types.
 */
final class GlassTokens
{
	/** The opacity the scene wash is drawn at, so the real scene still shows faintly through it. */
	static final double WASH_ALPHA = 0.68;

	/** The opacity of the window colour laid over the wash. */
	static final double TINT_OVER_WASH = 0.29;

	/** The opacity of the window colour when there is no wash yet; high enough that text reads over white. */
	static final double TINT_SOLID = 0.78;

	/**
	 * The opacity of the window colour on tooltips and popup menus when there is no wash: nearly solid, so their text
	 * reads over any scene, with a hint of glass left (#331).
	 */
	static final double POPUP_TINT = 0.94;

	/** The width of the metal border round buttons and pop-ups, in pixels at scale 1. */
	static final double BORDER_WIDTH = 1.1;

	/** How much of the border's strength a disabled button keeps. */
	static final double BORDER_DISABLED = 0.45;

	/** Where the middle colour of the border sits, from the top (0) to the bottom (1). */
	static final double BORDER_MIDDLE = 0.45;

	/** How opaque the accent metal is; a metal reads as solid, so it is far less see-through than plain glass. */
	static final double METAL_ALPHA = 0.96;

	/** Where the body colour of the accent metal sits, from the top (0) to the bottom (1). */
	static final double METAL_MIDDLE = 0.5;

	/** The opacity of the bright sheen over the top half of the accent metal. */
	static final double METAL_SHEEN = 0.18;

	/** How much a pressed accent element darkens. */
	static final double METAL_PRESSED_SHADE = 0.16;

	/** The metal of an earned bond heart: light, body and dark red. */
	static final Color[] HEART_RED = {new Color(0xFF8F86), new Color(0xD32F2F), new Color(0x7C1717)};

	/** The metal of an earned bond heart when the bond is complete: light, body and dark gold. */
	static final Color[] HEART_GOLD = {new Color(0xFFE59A), new Color(0xFFCC33), new Color(0xA67C00)};

	/** The dull metal red border of an empty bond heart: light, body and dark. */
	static final Color[] HEART_DULL = {new Color(0x9C6464), new Color(0x6F3B3B), new Color(0x452424)};

	/** The grey fill of an empty bond heart. */
	static final Color EMPTY_HEART = new Color(0x4A, 0x4A, 0x4A, 0xE6);

	/** The metal outlines of a move's pill by the kind of attack: light, body and dark. */
	static final Color[] OUTLINE_MELEE = {new Color(0xFF9A92), new Color(0xD84040), new Color(0x7E1C1C)};

	/** The metal outline of a ranged move's pill. */
	static final Color[] OUTLINE_RANGED = {new Color(0xA8E6A5), new Color(0x43A047), new Color(0x1E5B22)};

	/** The metal outline of a magic move's pill. */
	static final Color[] OUTLINE_MAGIC = {new Color(0xA9D3FF), new Color(0x2F7DE0), new Color(0x163E7C)};

	/** The opacity of the white sheen at the top edge. */
	static final double SHEEN_TOP = 0.08;

	/** The opacity of the white sheen at the bottom edge. */
	static final double SHEEN_BOTTOM = 0.04;

	/** The radius of a soft tip, as a share of the element's height. */
	static final double TIP_ROUND = 0.15;

	/** The largest tip radius, in pixels at scale 1, so a big body does not get big round corners. */
	static final double TIP_MAX = 10;

	/** The lowest contrast allowed between body text and what is behind it. */
	static final double TEXT_CONTRAST = 4.5;

	/** The lowest contrast allowed between faint text and what is behind it. */
	static final double SUBTEXT_CONTRAST = 3.0;

	/** The opacity of the dark outer shadow of a large raised surface. */
	static final double LARGE_DARK = 0.51;

	/** The horizontal offset of the dark shadow of a large raised surface. */
	static final int LARGE_DARK_DX = 7;

	/** The vertical offset of the dark shadow of a large raised surface. */
	static final int LARGE_DARK_DY = 9;

	/** The blur radius of the dark shadow of a large raised surface. */
	static final int LARGE_DARK_BLUR = 10;

	/** The opacity of the light outer shadow of a large raised surface. */
	static final double LARGE_LIGHT = 0.13;

	/** The offset of the light shadow of a large raised surface, on both axes. */
	static final int LARGE_LIGHT_OFFSET = -5;

	/** The blur radius of the light shadow of a large raised surface. */
	static final int LARGE_LIGHT_BLUR = 8;

	/** The width of the bevel of a large raised surface. */
	static final float LARGE_BEVEL = 2.5f;

	/** The opacity of the bevel's light end on a large raised surface. */
	static final double LARGE_BEVEL_LIGHT = 0.27;

	/** The opacity of the bevel's dark end on a large raised surface. */
	static final double LARGE_BEVEL_DARK = 0.47;

	/** The opacity of the dark outer shadow of a small raised element. */
	static final double SMALL_DARK = 0.55;

	/** The horizontal offset of the dark shadow of a small raised element. */
	static final int SMALL_DARK_DX = 2;

	/** The vertical offset of the dark shadow of a small raised element. */
	static final int SMALL_DARK_DY = 3;

	/** The blur radius of the dark shadow of a small raised element. */
	static final int SMALL_DARK_BLUR = 2;

	/** The opacity of the light outer shadow of a small raised element. */
	static final double SMALL_LIGHT = 0.10;

	/** The offset of the light shadow of a small raised element, on both axes. */
	static final int SMALL_LIGHT_OFFSET = -1;

	/** The blur radius of the light shadow of a small raised element. */
	static final int SMALL_LIGHT_BLUR = 2;

	/** The width of the bevel of a small raised element. */
	static final float SMALL_BEVEL = 1.6f;

	/** The opacity of the bevel's light end on a small raised element. */
	static final double SMALL_BEVEL_LIGHT = 0.30;

	/** The opacity of the bevel's dark end on a small raised element. */
	static final double SMALL_BEVEL_DARK = 0.35;

	/** How much a hovered element's offsets and blur grow, so it lifts. */
	static final double LIFT = 1.8;

	/** The opacity of the white fill of a plain raised element. */
	static final double RAISED_FILL = 0.07;

	/** The opacity of the white fill of a hovered raised element. */
	static final double LIFTED_FILL = 0.14;

	/** The opacity of the white fill of a disabled, flat element. */
	static final double FLAT_FILL = 0.03;

	/** The opacity of the black fill of a pressed tab (the active one). */
	static final double PRESSED_TAB_FILL = 0.30;

	/** The opacity of the white fill of a pressed button. */
	static final double PRESSED_BUTTON_FILL = 0.04;

	/** The opacity of the black fill of an inset panel. */
	static final double INSET_FILL = 0.20;

	/** The opacity of the dark inner shadow of a pressed element. */
	static final double PRESSED_DARK = 0.60;

	/** The offset of the dark inner shadow of a pressed element, on both axes. */
	static final int PRESSED_DARK_OFFSET = 2;

	/** The blur radius of the dark inner shadow of a pressed element. */
	static final int PRESSED_DARK_BLUR = 2;

	/** The opacity of the light inner shadow of a pressed element. */
	static final double PRESSED_LIGHT = 0.12;

	/** The offset of the light inner shadow of a pressed element, on both axes. */
	static final int PRESSED_LIGHT_OFFSET = -1;

	/** The blur radius of the light inner shadow of a pressed element. */
	static final int PRESSED_LIGHT_BLUR = 1;

	/** The opacity of the black fill of a groove (a bar's track). */
	static final double GROOVE_FILL = 0.35;

	/** The opacity of the dark inner shadow of a groove. */
	static final double GROOVE_DARK = 0.55;

	/** The opacity of the light inner shadow of a groove. */
	static final double GROOVE_LIGHT = 0.18;

	/** The offset of a groove's dark inner shadow, on both axes. */
	static final int GROOVE_OFFSET = 1;

	/** The blur radius of a groove's inner shadows. */
	static final int GROOVE_BLUR = 1;

	/** How much white is mixed into the top of a health bar's fill. */
	static final double HP_TOP_LIGHT = 0.25;

	/** The text colour on an accented element. */
	static final Color ACCENT_TEXT = new Color(0x1E1400);

	/** The opacity of the bubble colour laid over the bubble's tint. */
	static final double BUBBLE_TINT = 0.38;

	/** The opacity of the white highlight on a bubble. */
	static final double BUBBLE_HIGHLIGHT = 0.30;

	/** The highlight's centre, from the bubble's centre in radii, to the left. */
	static final double BUBBLE_HIGHLIGHT_X = -0.35;

	/** The highlight's centre, from the bubble's centre in radii, upward. */
	static final double BUBBLE_HIGHLIGHT_Y = -0.45;

	/** The highlight's radius, in bubble radii. */
	static final double BUBBLE_HIGHLIGHT_RADIUS = 0.9;

	/** The width of a bubble's bevel. */
	static final float BUBBLE_BEVEL = 2f;

	/** The opacity of the bevel's light end on a bubble. */
	static final double BUBBLE_BEVEL_LIGHT = 0.45;

	/** The opacity of the bevel's dark end on a bubble. */
	static final double BUBBLE_BEVEL_DARK = 0.55;

	/** The width of a bubble's rim. */
	static final float BUBBLE_RIM = 1.5f;

	/** How much white the rim colour has mixed in. */
	static final double BUBBLE_RIM_WHITE = 0.25;

	/** The opacity of a bubble's rim. */
	static final double BUBBLE_RIM_ALPHA = 0.95;

	/** The stroke width of the focus glow's sharp ring. */
	static final float RING_WIDTH = 2f;

	/** The opacity of the focus glow's sharp ring. */
	static final double RING_ALPHA = 0.9;

	/** The stroke width of the focus glow's blurred copy. */
	static final float GLOW_WIDTH = 4f;

	/** The opacity of the focus glow's blurred copy. */
	static final double GLOW_ALPHA = 0.35;

	/** How long a pop-up's wash takes to fade in once it first arrives, in milliseconds. */
	static final long WASH_FADE_MS = 100;

	/** How often the backdrops ask for a new frame while a surface is shown, in milliseconds. */
	static final long WASH_REFRESH_MS = 100;

	/** After this long without a surface asking, its wash is forgotten, in milliseconds. */
	static final long WASH_FORGET_MS = 250;

	private GlassTokens()
	{
	}

	/**
	 * Gives white at an opacity.
	 *
	 * @param opacity from 0 to 1
	 * @return white with that alpha
	 */
	static Color white(double opacity)
	{
		return new Color(255, 255, 255, alpha(opacity));
	}

	/**
	 * Gives black at an opacity.
	 *
	 * @param opacity from 0 to 1
	 * @return black with that alpha
	 */
	static Color black(double opacity)
	{
		return new Color(0, 0, 0, alpha(opacity));
	}

	/**
	 * Gives a colour at an opacity.
	 *
	 * @param color the colour
	 * @param opacity from 0 to 1
	 * @return the colour with that alpha
	 */
	static Color of(Color color, double opacity)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha(opacity));
	}

	private static int alpha(double opacity)
	{
		return (int) Math.round(Math.max(0.0, Math.min(1.0, opacity)) * 255);
	}
}
