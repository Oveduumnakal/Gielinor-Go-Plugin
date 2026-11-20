/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;

/**
 * Paints the pieces of the Storage Box view: a creature's cell, an empty cell, the detail card beside the grid and
 * the picture that follows the pointer while a creature is dragged. The page decides where everything goes; this
 * only draws. Holds no RuneLite types.
 */
final class StorageBoxPainter
{
	/** The side of the style icon in a cell's corner, in layout units. */
	static final double ICON_UNITS = 14;

	/** The height of the health strip along a cell's bottom edge, in layout units. */
	static final double STRIP_UNITS = 4;

	/** How big the picture that follows the pointer is, against a cell. */
	static final double GHOST_SCALE = 0.9;

	/** How see-through the cell is that a creature is being dragged from. */
	static final double SOURCE_OPACITY = 0.35;

	/** The text on the card while no creature is highlighted. */
	static final String CARD_HINT = "Point at a creature to see it here.";

	private StorageBoxPainter()
	{
	}

	/** How one cell is dressed beyond its creature. */
	static final class Look
	{
		private final boolean selecting;
		private final boolean ticked;
		private final boolean source;
		private final boolean target;
		private final double hover;
		private final double press;

		/**
		 * @param selecting whether Select mode is on, which shows a tick box
		 * @param ticked whether the creature is ticked
		 * @param source whether the creature is being dragged or carried, so its cell fades
		 * @param target whether the cell is under a dragged creature, so it is ringed
		 * @param hover how much the pointer or the keys are on the cell, from 0 to 1
		 * @param press how much it is pressed, from 0 to 1
		 */
		Look(boolean selecting, boolean ticked, boolean source, boolean target, double hover, double press)
		{
			this.selecting = selecting;
			this.ticked = ticked;
			this.source = source;
			this.target = target;
			this.hover = hover;
			this.press = press;
		}
	}

	/**
	 * Paints a creature's cell: its portrait in a square frame, the level in the bottom-left over a dark strip, the
	 * style icon in the top-right, a thin health strip along the bottom and, in Select mode, a tick box top-left.
	 *
	 * @param g the graphics
	 * @param ctx what the frame needs
	 * @param pictures where the pictures are
	 * @param text draws strings and tells the tests about them
	 * @param view the creature
	 * @param box the cell's box
	 * @param clip the area the cell may draw text in; text that would leave it is left out
	 * @param look how the cell is dressed
	 */
	static void cell(Graphics2D g, PageContext ctx, PortraitCache pictures, PageText text, CreatureView view,
		Rectangle box, Rectangle clip, Look look)
	{
		WindowStyle st = ctx.style;
		RowModel model = RowModel.of(view, -1);
		double wash = look.source ? SOURCE_OPACITY : 1.0;
		Composite saved = PageContext.fade(g, model.dim() * wash);
		Rectangle2D cellBox = rect(box);
		Shape cell = st.outline(WindowStyle.Kind.PANEL, cellBox);
		PortraitFrame.paint(g, st, pictures, model, cellBox, st.skew(WindowStyle.Kind.PANEL), 0, ctx.now,
			ctx.mode);
		Shape savedClip = g.getClip();
		g.clip(cell);
		int strip = Math.max(2, st.px(STRIP_UNITS));
		g.setColor(st.color(ThemeColor.HP_TRACK));
		g.fillRect(box.x, box.y + box.height - strip, box.width, strip);
		int filled = (int) Math.round(box.width * model.hp());
		g.setColor(st.color(WindowStyle.hpColor(model.hp())));
		g.fillRect(box.x, box.y + box.height - strip, filled, strip);
		Font small = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		int lineH = g.getFontMetrics(small).getHeight();
		String level = model.levelText();
		int levelW = Math.min(box.width, g.getFontMetrics(small).stringWidth(level) + st.metrics().pad / 2);
		Rectangle levelBox = new Rectangle(box.x, box.y + box.height - strip - lineH, levelW, lineH);
		g.setColor(st.color(ThemeColor.SCRIM));
		g.fillRect(levelBox.x, levelBox.y, levelBox.width, levelBox.height);
		if (clip.contains(levelBox))
			text.draw(g, level, levelBox, small, ThemeColor.TEXT, PageText.Align.CENTER);

		int icon = Math.max(6, st.px(ICON_UNITS));
		int inset = Math.max(1, st.metrics().pad / 4);
		g.setClip(savedClip);
		Icons.style(g, model.style(), box.x + box.width - icon - inset, box.y + inset, icon);
		if (look.selecting)
		{
			int side = Math.max(8, box.width / 4);
			StoragePage.tickBox(g, st, new Rectangle(box.x + inset, box.y + inset, side, side), look.ticked);
		}

		g.setComposite(saved);
		st.shade(g, cell, look.hover, look.press);
		if (look.hover > 0 || look.target)
			st.focusRing(g, cell);
	}

	/**
	 * Paints an empty cell: a faint dashed outline, as an empty team slot has.
	 *
	 * @param g the graphics
	 * @param st the style
	 * @param box the cell's box
	 * @param target whether a dragged creature is over it, so it is ringed
	 */
	static void empty(Graphics2D g, WindowStyle st, Rectangle box, boolean target)
	{
		Composite saved = PageContext.fade(g, 0.55);
		Shape shape = st.outline(WindowStyle.Kind.PANEL, rect(box));
		if (st.theme().glass())
		{
			st.card(g, shape);
		}
		else
		{
			g.setColor(st.color(ThemeColor.PANEL_BOX));
			g.fill(shape);
			Stroke stroke = g.getStroke();
			g.setColor(st.color(ThemeColor.PANEL_BORDER));
			g.setStroke(new BasicStroke(st.metrics().border, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
				new float[] {6f, 5f}, 0f));
			g.draw(shape);
			g.setStroke(stroke);
		}

		g.setComposite(saved);
		if (target)
			st.focusRing(g, shape);
	}

	/**
	 * Paints the picture that follows the pointer while a creature is dragged: the portrait at 90% of a cell, with no
	 * text, centred on the pointer.
	 *
	 * @param g the graphics
	 * @param ctx what the frame needs
	 * @param pictures where the pictures are
	 * @param view the creature
	 * @param cell the side of a cell in pixels
	 * @param x the pointer's x
	 * @param y the pointer's y
	 */
	static void ghost(Graphics2D g, PageContext ctx, PortraitCache pictures, CreatureView view, int cell, int x,
		int y)
	{
		int side = (int) Math.round(cell * GHOST_SCALE);
		Composite saved = PageContext.fade(g, 0.9);
		PortraitFrame.paint(g, ctx.style, pictures, RowModel.of(view, -1),
			rect(new Rectangle(x - side / 2, y - side / 2, side, side)), 0, 0, ctx.now, ctx.mode);
		g.setComposite(saved);
	}

	/**
	 * Paints the detail card: the highlighted creature's name, level, style, rarity, rating and temperament, its
	 * health bar and what a click, a drag and a right click do. With nothing highlighted it shows a hint.
	 *
	 * @param g the graphics
	 * @param ctx what the frame needs
	 * @param text draws strings and tells the tests about them
	 * @param card the card's box
	 * @param view the highlighted creature, or null
	 * @param how the middle hint line: what a drag does now, or why it does nothing
	 */
	static void card(Graphics2D g, PageContext ctx, PageText text, Rectangle card, CreatureView view, String how)
	{
		WindowStyle st = ctx.style;
		st.panel(g, rect(card));
		WindowStyle.Metrics m = st.metrics();
		Font small = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		int pad = m.pad;
		Rectangle inner = new Rectangle(card.x + pad, card.y + pad, Math.max(0, card.width - 2 * pad),
			Math.max(0, card.height - 2 * pad));
		if (view == null)
		{
			text.drawWrapped(g, CARD_HINT, inner, small, ThemeColor.SUBTEXT);
			return;
		}

		RowModel model = RowModel.of(view, -1);
		Font regular = st.font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS);
		int nameH = g.getFontMetrics(regular).getHeight();
		int smallH = g.getFontMetrics(small).getHeight();
		int y = inner.y;
		text.drawName(g, (view.gilded() ? "Gilded " : "") + model.name(), new Rectangle(inner.x, y, inner.width, nameH),
			ThemeColor.TEXT);
		y += nameH;
		String[] lines = {model.levelText() + " · " + view.type().label() + " · " + view.rarity().label(),
			view.rating().label() + " · " + view.temperamentLabel()};
		for (String line : lines)
		{
			text.draw(g, line, new Rectangle(inner.x, y, inner.width, smallH), small, ThemeColor.SUBTEXT,
				PageText.Align.LEFT);
			y += smallH;
		}

		y += m.pad / 2;
		int barH = Math.max(3, m.bar / 2);
		st.hpBar(g, new Rectangle2D.Double(inner.x, y, inner.width, barH), model.hp());
		y += barH + m.pad;
		int room = inner.y + inner.height - y;
		text.drawWrapped(g, "Click to move to your team.\n" + how + "\nRight-click for more.",
			new Rectangle(inner.x, y, inner.width, Math.max(0, room)), small, ThemeColor.SUBTEXT);
	}

	private static Rectangle2D rect(Rectangle r)
	{
		return new Rectangle2D.Double(r.x, r.y, r.width, r.height);
	}
}
