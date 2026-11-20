/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Keeps text readable on glass whatever the scene is (#324). The wash a glass surface shows is made from the scene, so
 * it can be white, and white glass under white text cannot be read. This darkens each colour of a wash, as little as
 * needed, until every text colour that sits on the surface reaches its contrast over the whole stack of layers: the
 * wash over the scene, the tint, the sheen and, for labels on raised elements, the element's own fill. Which text
 * colours sit on a surface, and how much contrast each needs, is a list of {@link Need}s. Holds no RuneLite types.
 */
final class Readability
{
	/** Light text on a pressed or plain surface: the main text and the faint text. */
	static final List<Need> BODY = Collections.unmodifiableList(Arrays.asList(
		new Need(ThemeColor.TEXT, GlassTokens.TEXT_CONTRAST, null),
		new Need(ThemeColor.SUBTEXT, GlassTokens.SUBTEXT_CONTRAST, null),
		new Need(ThemeColor.TAB_TEXT, GlassTokens.TEXT_CONTRAST, GlassTokens.white(GlassTokens.RAISED_FILL)),
		new Need(ThemeColor.CHIP_TEXT, GlassTokens.TEXT_CONTRAST, GlassTokens.white(GlassTokens.RAISED_FILL))));

	/** The tooltip's text. */
	static final List<Need> TOOLTIP = Collections.singletonList(
		new Need(ThemeColor.TOOLTIP_TEXT, GlassTokens.TEXT_CONTRAST, null));

	/** The notice's text. */
	static final List<Need> NOTICE = Collections.singletonList(
		new Need(ThemeColor.NOTICE_TEXT, GlassTokens.TEXT_CONTRAST, null));

	/**
	 * How much more contrast than the minimum a wash is darkened for, so the grain, the wash's own colour boost and the
	 * scene differing a little from its wash still leave the minimum.
	 */
	static final double MARGIN = 1.1;

	private static final int BISECTIONS = 10;
	private static final double[] LINEAR = linearTable();

	private Readability()
	{
	}

	/** How a surface stacks its layers: the colour behind the text, from the scene and the wash. */
	interface Stack
	{
		/**
		 * Gives what shows behind the text of a surface.
		 *
		 * @param scene the scene colour behind the surface, taken as opaque
		 * @param wash the wash colour drawn over it, taken as opaque
		 * @return the opaque colour the text sits on
		 */
		Color under(Color scene, Color wash);
	}

	/** One text colour that sits on a surface, with the contrast it needs and what lies between it and the glass. */
	static final class Need
	{
		private final ThemeColor text;
		private final double minimum;
		private final Color overlay;

		Need(ThemeColor text, double minimum, Color overlay)
		{
			this.text = text;
			this.minimum = minimum;
			this.overlay = overlay;
		}

		/** @return which text colour */
		ThemeColor text()
		{
			return text;
		}

		/** @return the contrast it needs */
		double minimum()
		{
			return minimum;
		}

		/** @return the translucent fill of the element the text sits on, or null when it sits on the glass itself */
		Color overlay()
		{
			return overlay;
		}
	}

	/**
	 * Gives the stack of a glass body: the wash over the scene at the wash opacity, the window colour at the tint
	 * opacity, and the strongest sheen.
	 *
	 * @param style the style the surface is drawn in
	 * @return the stack
	 */
	static Stack body(WindowStyle style)
	{
		Color tint = GlassTokens.of(style.color(ThemeColor.WINDOW), GlassTokens.TINT_OVER_WASH);
		Color sheen = GlassTokens.white(GlassTokens.SHEEN_TOP);
		return (scene, wash) -> WindowTheme.over(sheen,
			WindowTheme.over(tint, WindowTheme.mix(scene, wash, GlassTokens.WASH_ALPHA)));
	}

	/**
	 * Gives what the text of a need sits on, for one scene and wash.
	 *
	 * @param stack the stack of the surface
	 * @param need the text colour
	 * @param scene the scene colour
	 * @param wash the wash colour
	 * @return the opaque colour behind the text
	 */
	static Color behind(Stack stack, Need need, Color scene, Color wash)
	{
		Color under = stack.under(scene, wash);
		return need.overlay() == null ? under : WindowTheme.over(need.overlay(), under);
	}

	/**
	 * Tells whether every text colour reaches its contrast.
	 *
	 * @param stack the stack of the surface
	 * @param style the style the surface is drawn in
	 * @param needs the text colours on the surface
	 * @param scene the scene colour
	 * @param wash the wash colour
	 * @return true when all of them read
	 */
	static boolean reads(Stack stack, WindowStyle style, List<Need> needs, Color scene, Color wash)
	{
		for (Need need : needs)
		{
			Color under = behind(stack, need, scene, wash);
			if (WindowTheme.contrast(style.color(need.text()), under) < need.minimum())
				return false;
		}

		return true;
	}

	/**
	 * Darkens a wash colour just enough that every text colour reads over the stack, with the scene behind the surface
	 * taken to be about the wash's own colour. A colour that already reads is kept as it is.
	 *
	 * @param wash the wash colour, taken as opaque
	 * @param style the style the surface is drawn in
	 * @param stack the stack of the surface
	 * @param needs the text colours on the surface
	 * @return the colour, scaled toward black as little as needed
	 */
	static Color tame(Color wash, WindowStyle style, Stack stack, List<Need> needs)
	{
		double[] textLum = new double[needs.size()];
		for (int i = 0; i < textLum.length; i++)
			textLum[i] = luminance(style.color(needs.get(i).text()));

		return tame(wash, stack, needs, textLum);
	}

	private static Color tame(Color wash, Stack stack, List<Need> needs, double[] textLum)
	{
		if (fits(stack, needs, textLum, wash, wash))
			return wash;

		double low = 0.0;
		double high = 1.0;
		for (int i = 0; i < BISECTIONS; i++)
		{
			double mid = (low + high) / 2;
			if (fits(stack, needs, textLum, wash, scaled(wash, mid)))
				low = mid;
			else
				high = mid;
		}

		return scaled(wash, low);
	}

	private static boolean fits(Stack stack, List<Need> needs, double[] textLum, Color scene, Color wash)
	{
		Color under = stack.under(scene, wash);
		for (int i = 0; i < textLum.length; i++)
		{
			Need need = needs.get(i);
			Color behind = need.overlay() == null ? under : WindowTheme.over(need.overlay(), under);
			double bg = luminance(behind);
			double ratio = (Math.max(textLum[i], bg) + 0.05) / (Math.min(textLum[i], bg) + 0.05);
			if (ratio < need.minimum() * MARGIN)
				return false;
		}

		return true;
	}

	/**
	 * Tames every pixel of a wash.
	 *
	 * @param wash the wash, or null
	 * @param style the style the surface is drawn in
	 * @param stack the stack of the surface
	 * @param needs the text colours on the surface
	 * @return a new picture with each pixel tamed, or null for a null wash
	 */
	static BufferedImage tame(BufferedImage wash, WindowStyle style, Stack stack, List<Need> needs)
	{
		if (wash == null)
			return null;

		double[] textLum = new double[needs.size()];
		for (int i = 0; i < textLum.length; i++)
			textLum[i] = luminance(style.color(needs.get(i).text()));

		int w = wash.getWidth();
		int h = wash.getHeight();
		int[] in = wash.getRGB(0, 0, w, h, null, 0, w);
		int[] out = new int[in.length];
		int lastIn = -1;
		int lastOut = 0;
		for (int i = 0; i < in.length; i++)
		{
			int rgb = in[i] & 0xFFFFFF;
			if (rgb != lastIn)
			{
				lastIn = rgb;
				lastOut = tame(new Color(rgb), stack, needs, textLum).getRGB() & 0xFFFFFF;
			}

			out[i] = lastOut;
		}

		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		image.setRGB(0, 0, w, h, out, 0, w);
		return image;
	}

	private static Color scaled(Color color, double factor)
	{
		return new Color((int) Math.round(color.getRed() * factor), (int) Math.round(color.getGreen() * factor),
			(int) Math.round(color.getBlue() * factor));
	}

	private static double luminance(Color c)
	{
		return 0.2126 * LINEAR[c.getRed()] + 0.7152 * LINEAR[c.getGreen()] + 0.0722 * LINEAR[c.getBlue()];
	}

	private static double[] linearTable()
	{
		double[] table = new double[256];
		for (int i = 0; i < table.length; i++)
		{
			double c = i / 255.0;
			table[i] = c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
		}

		return table;
	}
}
