/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Paints the battle controls from a {@link BattleControlsModel} with the painted window's kit: the frame, the title,
 * both creatures with their health bars, the time bar, the buttons and the line of words about the button under the
 * pointer. It sets up and restores nothing itself; the overlay that calls it does. Holds no RuneLite types.
 */
final class BattleControlsPainter
{
	private static final double PERCENT_UNITS = 56;

	private final PageText.Sink sink;

	/**
	 * Creates the painter.
	 *
	 * @param sink hears what text is drawn, for tests
	 */
	BattleControlsPainter(PageText.Sink sink)
	{
		this.sink = sink;
	}

	/**
	 * Paints the controls at (0, 0).
	 *
	 * @param g the graphics
	 * @param style the style, with the scale the layout was made at
	 * @param model what to paint
	 * @param layout the layout of the model's current page at that scale
	 */
	void paint(Graphics2D g, WindowStyle style, BattleControlsModel model, BattleControlsModel.Layout layout)
	{
		PageText text = new PageText(style, sink);
		style.windowOn(g, new Rectangle(layout.size), "battle");
		Font bold = style.font(WindowFonts.Face.BOLD, WindowFonts.BODY_UNITS);
		Font small = style.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		text.draw(g, model.title(), layout.title, bold, ThemeColor.GOLD, PageText.Align.LEFT);
		if (model.page() == BattleControlsModel.Page.MAIN)
		{
			creature(g, style, text, layout.mineName, layout.mineBar, model.mineLine(), model.mineHp());
			creature(g, style, text, layout.foeName, layout.foeBar, model.foeLine(), model.foeHp());
		}

		style.bar(g, rect(layout.timeBar), model.timeLeft(), ThemeColor.GOLD, ThemeColor.HP_TRACK);
		for (BattleControlsModel.Button button : layout.buttons)
		{
			String id = button.id();
			double hover = id.equals(model.hover()) ? 1.0 : 0.0;
			double press = id.equals(model.pressed()) ? 1.0 : 0.0;
			style.button(g, rect(button.bounds()), button.label(), false, !button.enabled(), hover, press);
		}

		text.draw(g, model.hint(), layout.hint, small, ThemeColor.SUBTEXT, PageText.Align.LEFT);
	}

	private void creature(Graphics2D g, WindowStyle style, PageText text, Rectangle name, Rectangle bar, String line,
		double hp)
	{
		int room = style.px(PERCENT_UNITS);
		Rectangle label = new Rectangle(name.x, name.y, Math.max(0, name.width - room), name.height);
		Rectangle percent = new Rectangle(name.x + label.width, name.y, name.width - label.width, name.height);
		Font small = style.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		text.drawName(g, line, label, ThemeColor.TEXT);
		text.draw(g, Math.round(hp * 100) + "%", percent, small, ThemeColor.SUBTEXT, PageText.Align.RIGHT);
		style.hpBar(g, rect(bar), hp);
	}

	private static Rectangle2D rect(Rectangle r)
	{
		return new Rectangle2D.Double(r.x, r.y, r.width, r.height);
	}
}
