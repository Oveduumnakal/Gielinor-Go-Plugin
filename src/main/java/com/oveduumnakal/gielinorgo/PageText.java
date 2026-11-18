/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Draws the text of a page into a box: it picks a font that fits, cuts what still does not fit to an ellipsis, centres
 * the line up and down in the box, and tells a {@link Sink} what it drew and where, so a test can check that every
 * string stayed inside its box. A long name first drops from the bold font to the small font of the same whole-multiple
 * size (the closest thing to "one step smaller" that keeps the text sharp, since the fonts are only crisp at whole
 * multiples of their native size) and only then ends in an ellipsis. Holds no RuneLite types.
 */
final class PageText
{
	/** Where the text is aligned in its box. */
	enum Align
	{
		/** Against the left edge. */
		LEFT,

		/** Centred. */
		CENTER,

		/** Against the right edge. */
		RIGHT
	}

	/** Hears what is drawn, for tests. */
	interface Sink
	{
		/** A sink that hears nothing. */
		Sink NONE = (text, width, height, box) ->
		{
		};

		/**
		 * One string was drawn.
		 *
		 * @param text what was drawn, after any ellipsis
		 * @param width how wide it is, in pixels
		 * @param height the height of its font's line, in pixels
		 * @param box the box it was drawn into
		 */
		void drawn(String text, int width, int height, Rectangle box);
	}

	private final WindowStyle style;
	private final Sink sink;

	/**
	 * Creates a text drawer.
	 *
	 * @param style the style to draw in
	 * @param sink hears what is drawn
	 */
	PageText(WindowStyle style, Sink sink)
	{
		this.style = style;
		this.sink = sink;
	}

	/**
	 * Draws a line of text in a box.
	 *
	 * @param g the graphics
	 * @param text the text
	 * @param box the box; the text is cut to its width
	 * @param font the font
	 * @param colour the colour name
	 * @param align where in the box the text sits
	 * @return the text that was drawn, after any ellipsis
	 */
	String draw(Graphics2D g, String text, Rectangle box, Font font, ThemeColor colour, Align align)
	{
		FontMetrics fm = g.getFontMetrics(font);
		String shown = WindowStyle.ellipsize(text, fm, box.width);
		int w = fm.stringWidth(shown);
		int x = align == Align.LEFT ? box.x : align == Align.RIGHT ? box.x + box.width - w
			: box.x + (box.width - w) / 2;
		int baseline = box.y + (box.height + fm.getAscent() - fm.getDescent()) / 2;
		if (!shown.isEmpty())
		{
			style.text(g, shown, x, baseline, font, colour);
			sink.drawn(shown, w, fm.getHeight(), box);
		}

		return shown;
	}

	/**
	 * Gives the font a name is drawn in: the bold font if the name fits, otherwise the small font of the same size.
	 *
	 * @param g the graphics
	 * @param name the name
	 * @param room the room in pixels
	 * @param units the wanted size in layout units
	 * @return the font
	 */
	Font nameFont(Graphics2D g, String name, int room, double units)
	{
		Font bold = style.font(WindowFonts.Face.BOLD, units);
		if (g.getFontMetrics(bold).stringWidth(name) <= room)
			return bold;

		return style.font(WindowFonts.Face.SMALL, units);
	}

	/**
	 * Draws a name that shrinks one step and then ends in an ellipsis.
	 *
	 * @param g the graphics
	 * @param name the name
	 * @param box the box
	 * @param colour the colour name
	 * @return the text that was drawn
	 */
	String drawName(Graphics2D g, String name, Rectangle box, ThemeColor colour)
	{
		return draw(g, name, box, nameFont(g, name, box.width, WindowFonts.BODY_UNITS), colour, Align.LEFT);
	}

	/**
	 * Draws wrapped text in a box, as many lines as fit its height, the last ending in an ellipsis when the text is
	 * longer.
	 *
	 * @param g the graphics
	 * @param text the text
	 * @param box the box
	 * @param font the font
	 * @param colour the colour name
	 * @return how many lines were drawn
	 */
	int drawWrapped(Graphics2D g, String text, Rectangle box, Font font, ThemeColor colour)
	{
		FontMetrics fm = g.getFontMetrics(font);
		int lines = Math.max(0, box.height / Math.max(1, fm.getHeight()));
		if (lines == 0 || box.width <= 0)
			return 0;

		TextLayout layout = TextLayout.plain(text, fm, box.width, lines);
		g.setFont(font);
		layout.paint(g, style, box.x, box.y, colour);
		for (int i = 0; i < layout.lineCount(); i++)
			sink.drawn(layout.lineText(i), layout.lineWidth(i), fm.getHeight(),
				new Rectangle(box.x, box.y + i * fm.getHeight(), box.width, fm.getHeight()));

		return layout.lineCount();
	}
}
