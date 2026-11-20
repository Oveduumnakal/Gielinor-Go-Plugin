/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

/**
 * Paints the kit's primitives, the wild Info card and the wild bubbles on a fixed scene, so a test can compare them
 * with recorded pictures ({@link SolidLookTest}, {@link GlassLookTest}) and so a preview can show them (see
 * {@link GlassPreviewTest}). Everything here is deterministic.
 */
final class LookSheet
{
	/** The width of the primitives sheet in layout units. */
	static final int SHEET_UNITS = 600;

	/** The height of the primitives sheet in layout units. */
	static final int SHEET_HEIGHT_UNITS = 560;

	private LookSheet()
	{
	}

	/**
	 * Paints a fixed scene: a blue-green sky above a sandy ground with a few coloured blocks, so a translucent
	 * surface over it picks up several colours.
	 *
	 * @param width the width
	 * @param height the height
	 * @return the scene
	 */
	static BufferedImage scene(int width, int height)
	{
		BufferedImage image = WidgetKit.canvas(width, height);
		Graphics2D g = image.createGraphics();
		g.setPaint(new GradientPaint(0, 0, new Color(0x4A7FB0), 0, height * 0.55f, new Color(0x9CC3A0)));
		g.fillRect(0, 0, width, height);
		g.setColor(new Color(0xB8A070));
		g.fillRect(0, (int) (height * 0.55), width, height);
		g.setColor(new Color(0x7A3B2A));
		g.fillRect(width / 8, height / 6, width / 6, height / 4);
		g.setColor(new Color(0xE8DCC0));
		g.fillRect(width * 3 / 4, height / 5, width / 5, height / 5);
		g.setColor(new Color(0x2E5A2A));
		g.fillRect(width / 3, height * 3 / 5, width / 4, height / 5);
		g.dispose();
		return image;
	}

	/**
	 * Paints every primitive of the kit at fixed sizes and states.
	 *
	 * @param theme the theme to paint in
	 * @param scale the window scale
	 * @return the sheet
	 */
	static BufferedImage primitives(WindowTheme theme, double scale)
	{
		WindowStyle st = WindowStyle.of(theme, scale);
		int w = st.px(SHEET_UNITS);
		int h = st.px(SHEET_HEIGHT_UNITS);
		BufferedImage image = scene(w, h);
		Graphics2D g = WidgetKit.graphics(image);
		int gap = st.px(12);
		int x0 = gap;
		int y = gap;
		st.window(g, new Rectangle(x0, y, st.px(250), st.px(120)));
		st.header(g, new Rectangle2D.Double(x0 + st.px(270), y, st.px(300), st.px(44)));
		st.panel(g, new Rectangle2D.Double(x0 + st.px(270), y + st.px(56), st.px(300), st.px(64)));
		y += st.px(136);

		int rowW = st.px(270);
		int rowH = st.metrics().row;
		boolean[] lead = {true, false, false, false};
		double[] hover = {0, 0, 0.7, 0};
		double[] press = {0, 0, 0, 0.7};
		for (int i = 0; i < 4; i++)
		{
			int x = x0 + (i % 2) * (rowW + gap);
			int top = y + (i / 2) * (rowH + gap);
			st.row(g, new Rectangle2D.Double(x, top, rowW, rowH), lead[i], hover[i], press[i]);
		}

		y += 2 * (rowH + gap);
		String[] tabs = {"Party", "Bag", "Soon", "Storage"};
		boolean[] active = {true, false, false, false};
		boolean[] disabled = {false, false, true, false};
		double[] tabHover = {0, 0, 0, 0.7};
		int tabW = st.px(100);
		for (int i = 0; i < tabs.length; i++)
		{
			st.tab(g, new Rectangle2D.Double(x0 + i * (tabW + gap), y, tabW, st.metrics().tab), tabs[i], active[i],
				disabled[i], tabHover[i]);
		}

		y += st.metrics().tab + gap;
		String[] buttons = {"Battle", "Catch", "Locked", "Hover", "Press"};
		boolean[] primary = {true, false, false, false, false};
		boolean[] off = {false, false, true, false, false};
		double[] buttonHover = {0, 0, 0, 0.7, 0};
		double[] buttonPress = {0, 0, 0, 0, 0.7};
		int buttonW = st.px(84);
		int buttonH = st.px(32);
		for (int i = 0; i < buttons.length; i++)
		{
			st.button(g, new Rectangle2D.Double(x0 + i * (buttonW + gap), y, buttonW, buttonH), buttons[i],
				primary[i], off[i], buttonHover[i], buttonPress[i]);
		}

		y += buttonH + gap;
		int chipX = x0;
		chipX += st.chip(g, chipX, y, "Magic", false) + gap;
		chipX += st.chip(g, chipX, y, "Unholy", true) + gap;
		st.chip(g, chipX, y, "Slayer", false);
		Rectangle2D focus = new Rectangle2D.Double(x0 + st.px(280), y, buttonW, buttonH);
		st.button(g, focus, "Focus", false, false, 0, 0);
		st.focusRing(g, Slant.shape(focus, st.metrics().chipSlant));
		y += st.metrics().chipHeight + gap;

		int barW = st.px(180);
		double[] hp = {1.0, 0.4, 0.1};
		for (int i = 0; i < hp.length; i++)
			st.hpBar(g, new Rectangle2D.Double(x0, y + i * (st.metrics().bar + gap), barW, st.metrics().bar), hp[i]);

		st.xpBar(g, new Rectangle2D.Double(x0 + barW + gap, y, barW, st.metrics().bar), 0.5);
		y += 3 * (st.metrics().bar + gap);
		st.notice(g, "Caught an Imp!", x0 + st.px(120), y + st.px(20), 1.0, 1.0);
		st.grip(g, new Rectangle2D.Double(x0 + st.px(300), y, st.px(24), st.px(24)), 0.0);
		g.dispose();
		return image;
	}

	/**
	 * Paints the wild Info card on the fixed scene.
	 *
	 * @param theme the theme to paint in
	 * @param scale the window scale
	 * @return the picture
	 */
	static BufferedImage card(WindowTheme theme, double scale)
	{
		WindowStyle st = WindowStyle.of(theme, scale);
		BubblePainter painter = new BubblePainter();
		BubbleFrame frame = GlassCardRenderTest.frame(painter, st, st.px(24), st.px(24));
		Rectangle box = frame.card().bounds();
		BufferedImage image = scene(box.width + st.px(48), box.height + st.px(60));
		Graphics2D g = WidgetKit.graphics(image);
		painter.paint(g, frame, st, 1000L);
		g.dispose();
		return image;
	}

	/**
	 * Paints three wild bubbles: a jar, a Battle and an Info.
	 *
	 * @param theme the theme to paint in
	 * @param scale the window scale
	 * @return the picture
	 */
	static BufferedImage bubbles(WindowTheme theme, double scale)
	{
		WindowStyle st = WindowStyle.of(theme, scale);
		BubblePainter painter = new BubblePainter();
		List<BubbleFrame.Disc> discs = Arrays.asList(disc(BubbleKind.JAR, 40, 50, JarType.RUNE, false),
			disc(BubbleKind.BATTLE, 80, 28, null, false), disc(BubbleKind.INFO, 120, 50, null, false),
			disc(BubbleKind.JAR, 160, 50, JarType.DRAGON, true));
		BubbleFrame frame = new BubbleFrame(discs, null, 1, true);
		BufferedImage image = scene(200, 90);
		Graphics2D g = WidgetKit.graphics(image);
		painter.paint(g, frame, st, 1000L);
		g.dispose();
		return image;
	}

	private static BubbleFrame.Disc disc(BubbleKind kind, double x, double y, JarType jar, boolean disabled)
	{
		return new BubbleFrame.Disc(kind, x, y, 15, 1.0, 1.0, 0.0, jar, 0, disabled, false, 0.0, true);
	}
}
