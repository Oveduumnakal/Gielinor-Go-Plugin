/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

/**
 * The paint kit of the window: the slanted row, the health and experience bars, a tab with a disabled state, a chip,
 * hover and press shading, the panel, the frame and the notice. It draws at one scale and in one {@link WindowTheme}.
 * Every colour comes from the theme and every size from {@link Metrics}, which depends on the scale only, so both
 * styles have the same shapes, sizes and hit areas. Text is drawn at whole multiples of the RuneScape fonts' native
 * size (see {@link WindowFonts}). Everything draws through a {@link Graphics2D}, so a test paints into a
 * {@code BufferedImage}. A style is cheap to make each frame.
 */
final class WindowStyle
{
	/** The share of a health bar under which it turns to the warning colour. */
	static final double HP_WARNING_BELOW = 0.50;

	/** The share of a health bar under which it turns to the danger colour. */
	static final double HP_DANGER_BELOW = 0.20;

	private final WindowTheme theme;
	private final double scale;
	private final Metrics metrics;

	private WindowStyle(WindowTheme theme, double scale)
	{
		this.theme = theme;
		this.scale = scale;
		this.metrics = new Metrics(scale);
	}

	/**
	 * @param theme the colours
	 * @param scale the window scale, from {@link WindowFit#scale}
	 * @return a style that paints in the theme at the scale
	 */
	static WindowStyle of(WindowTheme theme, double scale)
	{
		return new WindowStyle(theme, scale);
	}

	/** @return the theme this style paints in */
	WindowTheme theme()
	{
		return theme;
	}

	/** @return the window scale this style paints at */
	double scale()
	{
		return scale;
	}

	/** @return the sizes in pixels; the same in every theme */
	Metrics metrics()
	{
		return metrics;
	}

	/**
	 * @param name which colour
	 * @return the theme's colour of that name
	 */
	Color color(ThemeColor name)
	{
		return theme.color(name);
	}

	/**
	 * @param units a length in layout units
	 * @return the length in pixels, rounded
	 */
	int px(double units)
	{
		return (int) Math.round(units * scale);
	}

	/**
	 * @param face which RuneScape font
	 * @param units the wanted size in layout units; the font is drawn at a sharp whole multiple of its native size
	 * @return the font
	 */
	Font font(WindowFonts.Face face, double units)
	{
		return WindowFonts.font(face, units, scale);
	}

	/**
	 * Gives the largest sharp font that lets a text fit a width: the wanted size, or the next whole multiple down
	 * until the text fits, and the native size at the least. A label never spills out of its tab or button.
	 *
	 * @param g the graphics, for the font's measures
	 * @param face which RuneScape font
	 * @param units the wanted size in layout units
	 * @param text the text to draw
	 * @param maxWidth the room in pixels
	 * @return the font
	 */
	Font fitted(Graphics2D g, WindowFonts.Face face, double units, String text, int maxWidth)
	{
		for (int step = WindowFonts.step(units, scale); step > 1; step--)
		{
			Font candidate = WindowFonts.ofStep(face, step);
			if (g.getFontMetrics(candidate).stringWidth(text) <= maxWidth)
				return candidate;
		}

		return WindowFonts.ofStep(face, 1);
	}

	/**
	 * Sets the rendering hints the kit expects: smooth shapes, antialiased text with whole-pixel advances.
	 *
	 * @param g the graphics to set up
	 */
	static void prepare(Graphics2D g)
	{
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	}

	/**
	 * Gives the colour name a health bar uses at a fill level.
	 *
	 * @param fraction the share of health left, from 0 to 1
	 * @return the fill colour name: plain, warning under half, danger under a fifth
	 */
	static ThemeColor hpColor(double fraction)
	{
		if (fraction < HP_DANGER_BELOW)
			return ThemeColor.HP_DANGER;

		if (fraction < HP_WARNING_BELOW)
			return ThemeColor.HP_WARNING;

		return ThemeColor.HP_FILL;
	}

	/** The kinds of element in the window. Each kind has one slant, so every element of a kind has the same shape. */
	enum Kind
	{
		/** A window, box, card, modal, menu, tooltip or notice: the large slanted shapes. */
		BODY,
		/** A boxed section inside a window. */
		PANEL,
		/** A party row, a portrait frame or a pin button. */
		ROW,
		/** A tab. */
		TAB,
		/** A button. */
		BUTTON,
		/** A small label such as a style or a trait. */
		CHIP,
		/** A health, experience or time bar. */
		BAR,
		/** A search box or the thumb of a scroll bar. */
		FIELD
	}

	/**
	 * Gives how far the sides of an element lean.
	 *
	 * @param kind the kind of element
	 * @return the lean in pixels
	 */
	int skew(Kind kind)
	{
		switch (kind)
		{
			case BODY:
				return metrics.slant;
			case PANEL:
				return metrics.slant / 2;
			case ROW:
				return metrics.slant;
			case TAB:
				return metrics.tabSlant;
			case BUTTON:
			case CHIP:
				return metrics.chipSlant;
			default:
				return metrics.barSlant;
		}
	}

	/**
	 * Gives the box a body is drawn over: a body is drawn wider than its content box by its lean (half on each side),
	 * so the content stays inside the slanted sides. Other kinds are drawn over their box.
	 *
	 * @param kind the kind of element
	 * @param box the element's content box
	 * @return the box to draw the outline over
	 */
	Rectangle2D drawn(Kind kind, Rectangle2D box)
	{
		if (kind != Kind.BODY)
			return box;

		double half = skew(kind) / 2.0;
		return new Rectangle2D.Double(box.getX() - half, box.getY(), box.getWidth() + 2 * half, box.getHeight());
	}

	/**
	 * Gives the outline of an element. Every fill, border, shadow, clip and highlight of the element uses this one
	 * outline, so nothing is painted as a plain rectangle. The solid look has sharp slants; the glass look rounds the
	 * tips by {@link GlassTokens#TIP_ROUND} of the element's height (at most {@link GlassTokens#TIP_MAX} pixels).
	 *
	 * @param kind the kind of element
	 * @param box the element's box
	 * @return the outline
	 */
	Shape outline(Kind kind, Rectangle2D box)
	{
		return slant(drawn(kind, box), skew(kind));
	}

	/**
	 * Gives the slanted outline of a box at a lean of the caller's choice: sharp in the solid look, with soft tips in
	 * the glass look.
	 *
	 * @param box the box the outline fits in
	 * @param skew how far the sides lean, in pixels
	 * @return the outline
	 */
	Shape slant(Rectangle2D box, double skew)
	{
		if (!theme.glass())
			return Slant.shape(box, skew);

		double radius = Math.min(box.getHeight() * GlassTokens.TIP_ROUND, GlassTokens.TIP_MAX * scale);
		return Shapes.roundedSlant(box, skew, radius);
	}

	/**
	 * Gives the filled part of a slanted bar, still slanted the same way: sharp in the solid look, with soft tips in
	 * the glass look.
	 *
	 * @param box the box of the whole bar
	 * @param skew how far the sides lean, in pixels
	 * @param fraction the filled share, from 0 to 1
	 * @return the outline of the filled part
	 */
	Shape filled(Rectangle2D box, double skew, double fraction)
	{
		if (!theme.glass())
			return Slant.filled(box, skew, fraction);

		double radius = Math.min(box.getHeight() * GlassTokens.TIP_ROUND, GlassTokens.TIP_MAX * scale);
		return Shapes.roundedFilled(box, skew, fraction, radius);
	}

	/**
	 * Gives how far a body's paint reaches beyond its content box on every side: its lean, and its shadows.
	 *
	 * @return the room in pixels
	 */
	int bodyPad()
	{
		return theme.glass() ? GlassPanel.pad(this) : metrics.slant / 2 + metrics.shadow + 1;
	}

	/**
	 * Paints the window: a soft shadow, the body and the frame.
	 *
	 * @param g the graphics
	 * @param box the window bounds
	 */
	void window(Graphics2D g, Rectangle box)
	{
		Shape body = outline(Kind.BODY, box);
		g.setColor(color(ThemeColor.SHADOW));
		g.fill(outline(Kind.BODY, new Rectangle2D.Double(box.x, box.y + metrics.shadow, box.width, box.height)));
		g.setColor(color(ThemeColor.WINDOW));
		g.fill(body);
		g.setColor(color(ThemeColor.FRAME));
		g.setStroke(new BasicStroke(metrics.border));
		g.draw(outline(Kind.BODY, inset(box, metrics.border / 2.0)));
	}

	/**
	 * Paints the header bar that holds the tabs.
	 *
	 * @param g the graphics
	 * @param box the header bounds
	 */
	void header(Graphics2D g, Rectangle2D box)
	{
		g.setColor(color(ThemeColor.HEADER));
		g.fill(box);
	}

	/**
	 * Gives the outline a {@link #panel} of this box has, to cut something to the panel's rounded corners.
	 *
	 * @param box the section bounds
	 * @return the rounded outline
	 */
	Shape panelShape(Rectangle2D box)
	{
		return outline(Kind.PANEL, box);
	}

	/**
	 * Paints a boxed section of a page.
	 *
	 * @param g the graphics
	 * @param box the section bounds
	 */
	void panel(Graphics2D g, Rectangle2D box)
	{
		box(g, box, Kind.PANEL, ThemeColor.PANEL_BOX, ThemeColor.PANEL_BORDER);
	}

	/**
	 * Paints a slanted box: a fill and a border on the outline of its kind. The panel, the menu and the modal's dialog
	 * are painted by this.
	 *
	 * @param g the graphics
	 * @param box the box
	 * @param kind the kind of element, which sets the lean
	 * @param fill the colour name of the fill
	 * @param border the colour name of the border
	 */
	void box(Graphics2D g, Rectangle2D box, Kind kind, ThemeColor fill, ThemeColor border)
	{
		g.setColor(color(fill));
		g.fill(outline(kind, box));
		g.setColor(color(border));
		g.setStroke(new BasicStroke(metrics.border));
		g.draw(outline(kind, inset(box, metrics.border / 2.0)));
	}

	/**
	 * Paints a slanted row, with its outline and the hover and press shading. The returned shape is the row's
	 * slanted outline, to clip a portrait to or to hit-test with {@link Slant#contains}.
	 *
	 * @param g the graphics
	 * @param box the row's box
	 * @param lead whether this is the lead row, which has its own colours
	 * @param hover how much the pointer is on the row, from 0 to 1
	 * @param press how much the row is pressed, from 0 to 1
	 * @return the slanted shape of the row
	 */
	Shape row(Graphics2D g, Rectangle2D box, boolean lead, double hover, double press)
	{
		Shape shape = outline(Kind.ROW, box);
		g.setColor(color(lead ? ThemeColor.LEAD_ROW : ThemeColor.ROW));
		g.fill(shape);
		shade(g, shape, hover, press);
		g.setColor(color(lead ? ThemeColor.LEAD_BORDER : ThemeColor.ROW_BORDER));
		g.setStroke(new BasicStroke(metrics.border));
		g.draw(outline(Kind.ROW, inset(box, metrics.border / 2.0)));
		return shape;
	}

	/**
	 * @param lead whether text sits on the lead row
	 * @return the colour name for text on a row
	 */
	static ThemeColor rowText(boolean lead)
	{
		return lead ? ThemeColor.LEAD_TEXT : ThemeColor.ROW_TEXT;
	}

	/**
	 * Paints the resize grip: three short diagonal lines in the bottom-right corner of the window.
	 *
	 * @param g the graphics
	 * @param box the grip's square, in the window's corner
	 * @param active how much the grip is being dragged, from 0 to 1
	 */
	void grip(Graphics2D g, Rectangle2D box, double active)
	{
		Color base = color(ThemeColor.SUBTEXT);
		g.setColor(WindowTheme.mix(base, color(ThemeColor.ACCENT), active));
		g.setStroke(new BasicStroke(Math.max(1f, metrics.border)));
		double step = box.getWidth() / 4.0;
		double right = box.getMaxX() - metrics.border;
		double bottom = box.getMaxY() - metrics.border;
		for (int i = 1; i <= 3; i++)
			g.draw(new Line2D.Double(right - i * step, bottom, right, bottom - i * step));
	}

	/**
	 * Lays the hover light and the press dark over a shape.
	 *
	 * @param g the graphics
	 * @param shape what to shade
	 * @param hover how much the pointer is on it, from 0 to 1
	 * @param press how much it is pressed, from 0 to 1
	 */
	void shade(Graphics2D g, Shape shape, double hover, double press)
	{
		if (hover > 0)
		{
			g.setColor(WindowTheme.fade(color(ThemeColor.HOVER_SHADE), hover));
			g.fill(shape);
		}

		if (press > 0)
		{
			g.setColor(WindowTheme.fade(color(ThemeColor.PRESS_SHADE), press));
			g.fill(shape);
		}
	}

	/**
	 * Draws the ring around what has keyboard focus.
	 *
	 * @param g the graphics
	 * @param shape the outline to ring
	 */
	void focusRing(Graphics2D g, Shape shape)
	{
		focusRing(g, shape, ThemeColor.FOCUS_RING);
	}

	/**
	 * Draws a ring around what has keyboard focus, in a colour of the caller's choice.
	 *
	 * @param g the graphics
	 * @param shape the outline to ring
	 * @param ring the colour name of the ring
	 */
	void focusRing(Graphics2D g, Shape shape, ThemeColor ring)
	{
		g.setColor(color(ring));
		g.setStroke(new BasicStroke(Math.max(2f, metrics.border)));
		g.draw(shape);
	}

	/**
	 * Paints a slanted bar: an empty track and a filled part.
	 *
	 * @param g the graphics
	 * @param box the bar's box
	 * @param fraction the filled share, from 0 to 1
	 * @param fill the colour name of the filled part
	 * @param track the colour name of the empty part
	 */
	void bar(Graphics2D g, Rectangle2D box, double fraction, ThemeColor fill, ThemeColor track)
	{
		g.setColor(color(track));
		g.fill(outline(Kind.BAR, box));
		if (fraction > 0)
		{
			g.setColor(color(fill));
			g.fill(filled(box, metrics.barSlant, fraction));
		}
	}

	/**
	 * Paints a health bar, in the plain, warning or danger colour for its level.
	 *
	 * @param g the graphics
	 * @param box the bar's box
	 * @param fraction the share of health left, from 0 to 1
	 */
	void hpBar(Graphics2D g, Rectangle2D box, double fraction)
	{
		bar(g, box, fraction, hpColor(fraction), ThemeColor.HP_TRACK);
	}

	/**
	 * Paints an experience bar.
	 *
	 * @param g the graphics
	 * @param box the bar's box
	 * @param fraction the share of the level done, from 0 to 1
	 */
	void xpBar(Graphics2D g, Rectangle2D box, double fraction)
	{
		bar(g, box, fraction, ThemeColor.XP_FILL, ThemeColor.XP_TRACK);
	}

	/**
	 * Paints a tab. A disabled tab has its own colours and no hover shading.
	 *
	 * @param g the graphics
	 * @param box the tab's box
	 * @param label the tab's name
	 * @param active whether this is the selected tab
	 * @param disabled whether the tab cannot be chosen
	 * @param hover how much the pointer is on the tab, from 0 to 1
	 */
	void tab(Graphics2D g, Rectangle2D box, String label, boolean active, boolean disabled, double hover)
	{
		Shape shape = outline(Kind.TAB, box);
		ThemeColor body = disabled ? ThemeColor.TAB_DISABLED : active ? ThemeColor.TAB_ACTIVE : ThemeColor.TAB;
		ThemeColor text = disabled ? ThemeColor.TAB_DISABLED_TEXT
			: active ? ThemeColor.TAB_ACTIVE_TEXT : ThemeColor.TAB_TEXT;
		g.setColor(color(body));
		g.fill(shape);
		if (!disabled)
			shade(g, shape, active ? 0 : hover, 0);

		Font font = fitted(g, WindowFonts.Face.BOLD, WindowFonts.BODY_UNITS, label,
			(int) box.getWidth() - 2 * metrics.tabSlant);
		FontMetrics fm = g.getFontMetrics(font);
		text(g, label, (int) Math.round(box.getCenterX() - fm.stringWidth(label) / 2.0),
			baseline(box, fm), font, text);
	}

	/**
	 * Paints a button: a slanted label that shrinks to 97% while pressed. A disabled button has its own colours and
	 * no hover or press shading.
	 *
	 * @param g the graphics
	 * @param box the button's box
	 * @param label the button's text
	 * @param primary whether it is the main choice, which stands out
	 * @param disabled whether it cannot be pressed
	 * @param hover how much the pointer is on it, from 0 to 1
	 * @param press how much it is pressed, from 0 to 1
	 */
	void button(Graphics2D g, Rectangle2D box, String label, boolean primary, boolean disabled, double hover,
		double press)
	{
		AffineTransform saved = g.getTransform();
		double shrink = 1.0 - (1.0 - WindowTimes.PRESS_SCALE) * Math.max(0.0, Math.min(1.0, press));
		g.translate(box.getCenterX(), box.getCenterY());
		g.scale(shrink, shrink);
		g.translate(-box.getCenterX(), -box.getCenterY());
		Shape shape = outline(Kind.BUTTON, box);
		ThemeColor body = disabled ? ThemeColor.TAB_DISABLED : primary ? ThemeColor.CHIP_ACCENT : ThemeColor.CHIP;
		ThemeColor text = disabled ? ThemeColor.TAB_DISABLED_TEXT
			: primary ? ThemeColor.CHIP_ACCENT_TEXT : ThemeColor.CHIP_TEXT;
		g.setColor(color(body));
		g.fill(shape);
		if (!disabled)
			shade(g, shape, hover, press);

		Font font = fitted(g, WindowFonts.Face.BOLD, WindowFonts.BODY_UNITS, label,
			(int) box.getWidth() - 2 * metrics.chipSlant);
		FontMetrics fm = g.getFontMetrics(font);
		text(g, label, (int) Math.round(box.getCenterX() - fm.stringWidth(label) / 2.0), baseline(box, fm), font,
			text);
		g.setTransform(saved);
	}

	/**
	 * Gives the width a chip needs, so a row of chips can be laid out before it is painted.
	 *
	 * @param g the graphics, for the font's measures
	 * @param label the chip's text
	 * @return the width in pixels
	 */
	int chipWidth(Graphics2D g, String label)
	{
		FontMetrics fm = g.getFontMetrics(font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS));
		return fm.stringWidth(label) + 2 * metrics.chipPad;
	}

	/**
	 * Paints a chip, a small slanted label such as a style or a trait.
	 *
	 * @param g the graphics
	 * @param x the left edge
	 * @param y the top edge
	 * @param label the chip's text
	 * @param accent whether it stands out
	 * @return the chip's width in pixels
	 */
	int chip(Graphics2D g, int x, int y, String label, boolean accent)
	{
		int w = chipWidth(g, label);
		Rectangle2D box = new Rectangle2D.Double(x, y, w, metrics.chipHeight);
		g.setColor(color(accent ? ThemeColor.CHIP_ACCENT : ThemeColor.CHIP));
		g.fill(outline(Kind.CHIP, box));
		Font font = font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		text(g, label, x + metrics.chipPad, baseline(box, g.getFontMetrics(font)), font,
			accent ? ThemeColor.CHIP_ACCENT_TEXT : ThemeColor.CHIP_TEXT);
		return w;
	}

	/**
	 * Paints the pop-up notice: a dark pill with the text in it, drawn around a centre point, so a pop that scales
	 * the notice keeps it centred.
	 *
	 * @param g the graphics
	 * @param message the notice text
	 * @param cx the centre x
	 * @param cy the centre y
	 * @param grow the size factor, 1 for plain; a pop in goes from 0.8 to 1 with an overshoot
	 * @param opacity the share of opacity, from 0 to 1
	 */
	void notice(Graphics2D g, String message, double cx, double cy, double grow, double opacity)
	{
		if (opacity <= 0)
			return;

		Font font = font(WindowFonts.Face.BOLD, WindowFonts.BODY_UNITS);
		FontMetrics fm = g.getFontMetrics(font);
		double w = fm.stringWidth(message) + 2.0 * metrics.pad;
		double h = fm.getHeight() + metrics.pad;
		AffineTransform saved = g.getTransform();
		Composite savedComposite = g.getComposite();
		g.translate(cx, cy);
		g.scale(grow, grow);
		g.setComposite(AlphaComposite.SrcOver.derive((float) Math.max(0.0, Math.min(1.0, opacity))));
		Rectangle2D box = new Rectangle2D.Double(-w / 2.0, -h / 2.0, w, h);
		this.box(g, box, Kind.BODY, ThemeColor.NOTICE, ThemeColor.FRAME);
		text(g, message, (int) Math.round(-fm.stringWidth(message) / 2.0), baseline(box, fm), font,
			ThemeColor.NOTICE_TEXT);
		g.setTransform(saved);
		g.setComposite(savedComposite);
	}

	/**
	 * Draws text in a theme colour at a baseline.
	 *
	 * @param g the graphics
	 * @param s the text
	 * @param x the left edge
	 * @param baselineY the baseline
	 * @param font the font, from {@link #font}
	 * @param colour the colour name
	 */
	void text(Graphics2D g, String s, int x, int baselineY, Font font, ThemeColor colour)
	{
		text(g, s, x, baselineY, font, color(colour));
	}

	/**
	 * Draws text in a plain colour at a baseline, for a colour that is not part of the theme, such as a rarity's.
	 *
	 * @param g the graphics
	 * @param s the text
	 * @param x the left edge
	 * @param baselineY the baseline
	 * @param font the font, from {@link #font}
	 * @param colour the colour
	 */
	void text(Graphics2D g, String s, int x, int baselineY, Font font, Color colour)
	{
		g.setFont(font);
		g.setColor(colour);
		g.drawString(s, x, baselineY);
	}

	/**
	 * Cuts a text to a width, ending with an ellipsis when it does not fit.
	 *
	 * @param s the text
	 * @param fm the measures of the font it is drawn in
	 * @param maxWidth the room in pixels
	 * @return the text if it fits, else its start and "..."; empty if not even the ellipsis fits
	 */
	static String ellipsize(String s, FontMetrics fm, int maxWidth)
	{
		if (fm.stringWidth(s) <= maxWidth)
			return s;

		String dots = "...";
		if (fm.stringWidth(dots) > maxWidth)
			return "";

		int end = s.length();
		while (end > 0 && fm.stringWidth(s.substring(0, end).trim() + dots) > maxWidth)
			end--;

		return s.substring(0, end).trim() + dots;
	}

	private static int baseline(Rectangle2D box, FontMetrics fm)
	{
		return (int) Math.round(box.getCenterY() + (fm.getAscent() - fm.getDescent()) / 2.0);
	}

	private static Rectangle2D inset(Rectangle2D box, double by)
	{
		return new Rectangle2D.Double(box.getX() + by, box.getY() + by, box.getWidth() - 2 * by,
			box.getHeight() - 2 * by);
	}

	/**
	 * Every size the kit draws with, in pixels. It depends on the scale and on nothing else, so it is the same in
	 * every theme; {@code WindowThemeTest} holds it to that.
	 */
	static final class Metrics
	{
		/** The lean of a row's sides, in layout units. */
		static final double SLANT_UNITS = 14;

		/** The height of a party row, in layout units. */
		static final double ROW_UNITS = 64;

		/** The height of a tab, in layout units. */
		static final double TAB_UNITS = 44;

		/** The height of a health or experience bar, in layout units. */
		static final double BAR_UNITS = 12;

		/** The height of a chip, in layout units. */
		static final double CHIP_UNITS = 26;

		/** The padding inside boxes, in layout units. */
		static final double PAD_UNITS = 12;

		/** The corner radius, in layout units. */
		static final double CORNER_UNITS = 6;

		/** How far a hovered row nudges right, in layout units. */
		static final double NUDGE_UNITS = 4;

		/** The lean of a row's sides, in pixels. */
		final int slant;

		/** The height of a party row, in pixels. */
		final int row;

		/** The height of a tab, in pixels. */
		final int tab;

		/** The height of a bar, in pixels. */
		final int bar;

		/** The height of a chip, in pixels. */
		final int chipHeight;

		/** The padding inside boxes, in pixels. */
		final int pad;

		/** The corner radius, in pixels. */
		final int corner;

		/** The outline width, in pixels; at least 1. */
		final int border;

		/** The drop shadow offset, in pixels. */
		final int shadow;

		/** The hover nudge, in pixels. */
		final int nudge;

		/** The lean of a bar's sides, in pixels. */
		final int barSlant;

		/** The lean of a tab's sides, in pixels. */
		final int tabSlant;

		/** The lean of a chip's sides, in pixels. */
		final int chipSlant;

		/** The padding at each end of a chip's text, in pixels. */
		final int chipPad;

		/** The pixel size of small print, a sharp whole multiple of the native font size; icons match it. */
		final int text;

		Metrics(double scale)
		{
			slant = round(SLANT_UNITS, scale);
			row = round(ROW_UNITS, scale);
			tab = round(TAB_UNITS, scale);
			bar = round(BAR_UNITS, scale);
			chipHeight = round(CHIP_UNITS, scale);
			pad = round(PAD_UNITS, scale);
			corner = round(CORNER_UNITS, scale);
			border = Math.max(1, round(2, scale));
			shadow = Math.max(1, round(3, scale));
			nudge = round(NUDGE_UNITS, scale);
			barSlant = round(BAR_UNITS / 2, scale);
			tabSlant = round(SLANT_UNITS / 2, scale);
			chipSlant = round(CHIP_UNITS / 3, scale);
			chipPad = round(10, scale);
			text = WindowFonts.pixelSize(WindowFonts.SMALL_UNITS, scale);
		}

		private static int round(double units, double scale)
		{
			return (int) Math.round(units * scale);
		}

		@Override
		public boolean equals(Object other)
		{
			if (!(other instanceof Metrics))
				return false;

			Metrics m = (Metrics) other;
			return slant == m.slant && row == m.row && tab == m.tab && bar == m.bar && chipHeight == m.chipHeight
				&& pad == m.pad && corner == m.corner && border == m.border && shadow == m.shadow
				&& nudge == m.nudge && barSlant == m.barSlant && tabSlant == m.tabSlant
				&& chipSlant == m.chipSlant && chipPad == m.chipPad && text == m.text;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(slant, row, tab, bar, chipHeight, pad, corner, border, shadow, nudge, barSlant,
				tabSlant, chipSlant, chipPad, text);
		}
	}
}
