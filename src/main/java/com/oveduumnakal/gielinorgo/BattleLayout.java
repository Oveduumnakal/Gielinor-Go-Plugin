/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Places the {@link BattleBlock}s of the Battle page in one tall column, measuring every wrapped line so a card is
 * exactly as tall as its words. Boxes are in content coordinates: (0, 0) is the column's top left corner, before any
 * scrolling. Pure geometry; the fonts come in as measures. Holds no RuneLite types.
 */
final class BattleLayout
{
	private static final double GAP_UNITS = 6;
	private static final double STRIPE_UNITS = 3;
	private static final double BUTTON_UNITS = 34;

	/** One block, placed. */
	static final class Placed
	{
		/** The block. */
		final BattleBlock block;

		/** The block's whole box. */
		final Rectangle box;

		/** The box of a card's title, or of a heading's or note's words. */
		final Rectangle title;

		/** The box of a card's status word. */
		final Rectangle status;

		/** The box of each item of a card, in order; a button row's box covers the row. */
		final List<Rectangle> items = new ArrayList<>();

		/** The boxes of the buttons of each button row, in order, empty lists for text items. */
		final List<List<Rectangle>> buttons = new ArrayList<>();

		/** The card's stripe. */
		final Rectangle stripe;

		/** The square of the sigil badge, or an empty box when the block shows none. */
		Rectangle badge = new Rectangle();

		Placed(BattleBlock block, Rectangle box, Rectangle title, Rectangle status, Rectangle stripe)
		{
			this.block = block;
			this.box = box;
			this.title = title;
			this.status = status;
			this.stripe = stripe;
		}
	}

	/** The measures of the three fonts the page uses. */
	static final class Fonts
	{
		final FontMetrics bold;
		final FontMetrics regular;
		final FontMetrics small;

		/**
		 * @param bold the bold font's measures
		 * @param regular the regular font's measures
		 * @param small the small font's measures
		 */
		Fonts(FontMetrics bold, FontMetrics regular, FontMetrics small)
		{
			this.bold = bold;
			this.regular = regular;
			this.small = small;
		}
	}

	private final List<Placed> placed;
	private final int height;

	private BattleLayout(List<Placed> placed, int height)
	{
		this.placed = placed;
		this.height = height;
	}

	/** @return every block, placed, top to bottom */
	List<Placed> placed()
	{
		return placed;
	}

	/** @return the height of the whole column */
	int height()
	{
		return height;
	}

	/**
	 * Places the blocks.
	 *
	 * @param blocks the blocks, top to bottom
	 * @param width the width of the column
	 * @param style the style
	 * @param fonts the measures of the fonts
	 * @return the layout
	 */
	static BattleLayout of(List<BattleBlock> blocks, int width, WindowStyle style, Fonts fonts)
	{
		int pad = style.metrics().pad;
		int gap = style.px(GAP_UNITS);
		int y = 0;
		List<Placed> out = new ArrayList<>();
		for (BattleBlock block : blocks)
		{
			Placed one;
			if (block.kind() == BattleBlock.Kind.HEADING)
			{
				int h = fonts.small.getHeight() + gap;
				one = words(block, new Rectangle(0, y + gap / 2, width, fonts.small.getHeight()), y, h, width);
				if (block.sigil() != BattleBlock.Sigil.NONE)
				{
					int side = fonts.small.getHeight();
					one.badge = new Rectangle(0, one.title.y, side, side);
					one.title.setBounds(side + pad / 2, one.title.y, width - side - pad / 2, side);
				}
			}
			else if (block.kind() == BattleBlock.Kind.NOTE)
			{
				int lines = TextLayout.plain(block.text(), fonts.small, width - 2 * pad, 0).lineCount();
				int h = lines * fonts.small.getHeight() + gap;
				one = words(block, new Rectangle(pad / 2, y, width - pad, lines * fonts.small.getHeight()), y, h,
					width);
			}
			else
			{
				one = card(block, y, width, style, fonts);
			}

			out.add(one);
			y += one.box.height + (block.kind() == BattleBlock.Kind.CARD ? gap : 0);
		}

		return new BattleLayout(out, y + gap);
	}

	private static Placed words(BattleBlock block, Rectangle title, int y, int height, int width)
	{
		return new Placed(block, new Rectangle(0, y, width, height), title, new Rectangle(), new Rectangle());
	}

	private static Placed card(BattleBlock block, int y, int width, WindowStyle style, Fonts fonts)
	{
		int pad = style.metrics().pad;
		int gap = style.px(GAP_UNITS);
		int stripeW = Math.max(2, style.px(STRIPE_UNITS));
		int x = stripeW + pad;
		int inner = Math.max(0, width - x - pad);
		int top = y + pad;
		int titleH = fonts.bold.getHeight();
		boolean badged = block.sigil() != BattleBlock.Sigil.NONE;
		int badgeW = badged ? titleH : 0;
		int statusW = block.status().isEmpty() ? 0 : fonts.small.stringWidth(block.status()) + pad;
		statusW = Math.min(statusW, inner / 2);
		Rectangle title = new Rectangle(x, top, Math.max(0, inner - statusW - badgeW), titleH);
		Rectangle status = new Rectangle(x + inner - statusW - badgeW, top, statusW, titleH);
		int cursor = top + titleH + gap / 2;
		List<Rectangle> items = new ArrayList<>();
		List<List<Rectangle>> buttons = new ArrayList<>();
		int buttonH = Math.max(style.px(BUTTON_UNITS), fonts.bold.getHeight() + pad);
		for (BattleBlock.Item item : block.items())
		{
			if (item.isButtons())
			{
				cursor += gap / 2;
				items.add(new Rectangle(x, cursor, inner, buttonH));
				buttons.add(row(item.buttons().size(), x, cursor, inner, buttonH, gap));
				cursor += buttonH;
			}
			else
			{
				FontMetrics fm = item.small() ? fonts.small : fonts.regular;
				int lines = Math.max(1, TextLayout.plain(item.text(), fm, inner, 0).lineCount());
				items.add(new Rectangle(x, cursor, inner, lines * fm.getHeight()));
				buttons.add(new ArrayList<>());
				cursor += lines * fm.getHeight();
			}
		}

		int bottom = cursor + pad;
		Rectangle box = new Rectangle(0, y, width, bottom - y);
		Placed placed = new Placed(block, box, title, status, new Rectangle(0, y, stripeW, box.height));
		if (badged)
			placed.badge = new Rectangle(x + inner - badgeW, top, badgeW, titleH);

		placed.items.addAll(items);
		placed.buttons.addAll(buttons);
		return placed;
	}

	private static List<Rectangle> row(int count, int x, int y, int width, int height, int gap)
	{
		List<Rectangle> boxes = new ArrayList<>();
		int each = (width - (count - 1) * gap) / Math.max(1, count);
		for (int i = 0; i < count; i++)
			boxes.add(new Rectangle(x + i * (each + gap), y, each, height));

		return boxes;
	}
}
