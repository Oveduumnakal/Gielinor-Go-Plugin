/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Where everything on the Party page sits, worked out from the page area and the window scale alone: six slanted rows
 * on the left, a column for eggs, tasks and the reset banner on the right, and the room the empty and first-run states
 * use. Rows are laid out at their final places (animations only move what is drawn, never what is hit). All lengths
 * are layout units times the window scale, so both themes share the same boxes. Holds no RuneLite types.
 */
final class PartyLayout
{
	/** How many rows the page has. */
	static final int SLOTS = SaveData.TEAM_SIZE;

	/** The gap between rows, and between the page's parts, in layout units. */
	static final double GAP_UNITS = 6;

	/** The share of the page width the right column takes. */
	static final double SIDE_SHARE = 0.30;

	/** The width of the strip for the lead's arrow, in layout units. */
	static final double ARROW_UNITS = 12;

	/** The most starter cards the first-run page shows. */
	static final int STARTERS = 3;

	/** The area of the rows, and of the starter cards and the empty message. */
	final Rectangle main;

	/** The six rows at their final places; every row is as wide as {@link #main} less the arrow room and the nudge. */
	final Rectangle[] rows;

	/** The strip left of the rows where the lead's arrow is drawn. */
	final Rectangle arrow;

	/** The right column. */
	final Rectangle side;

	/** The eggs box in the right column. */
	final Rectangle eggs;

	/** The top tasks box in the right column. */
	final Rectangle tasks;

	/** The Pin Party to UI button, at the foot of the right column, left of the More button. */
	final Rectangle pinButton;

	/** The More button at the foot of the right column, right of Pin Party to UI; it opens a menu. */
	final Rectangle moreButton;

	private final WindowStyle.Metrics metrics;
	private final int gap;

	private PartyLayout(Rectangle body, WindowStyle.Metrics metrics, double scale)
	{
		this.metrics = metrics;
		gap = Math.max(2, (int) Math.round(GAP_UNITS * scale));
		int sideW = (int) Math.round(body.width * SIDE_SHARE);
		main = new Rectangle(body.x, body.y, Math.max(0, body.width - sideW - gap), body.height);
		side = new Rectangle(main.x + main.width + gap, body.y, sideW, body.height);
		int rowH = Math.max(1, (main.height - (SLOTS - 1) * gap) / SLOTS);
		int room = Math.max(6, (int) Math.round(ARROW_UNITS * scale));
		arrow = new Rectangle(main.x, main.y, room, main.height);
		int rowW = Math.max(0, main.width - room - metrics.nudge);
		rows = new Rectangle[SLOTS];
		for (int i = 0; i < SLOTS; i++)
			rows[i] = new Rectangle(main.x + room, main.y + i * (rowH + gap), rowW, rowH);

		int pad = metrics.pad;
		int buttonH = Math.min(metrics.chipHeight + pad / 2, Math.max(0, side.height / 8));
		int rowTop = side.y + side.height - buttonH;
		int moreW = Math.max(0, (side.width - gap) * 2 / 5);
		int pinW = Math.max(0, side.width - gap - moreW);
		pinButton = new Rectangle(side.x, rowTop, pinW, buttonH);
		moreButton = new Rectangle(side.x + pinW + gap, rowTop, moreW, buttonH);
		int free = Math.max(0, rowTop - side.y - gap);
		int eggsH = Math.max(0, (free - gap) * 2 / 5);
		eggs = new Rectangle(side.x, side.y, side.width, eggsH);
		tasks = new Rectangle(side.x, side.y + eggsH + gap, side.width, Math.max(0, free - eggsH - gap));
	}

	/**
	 * Works the layout out.
	 *
	 * @param body the page area in window pixels
	 * @param metrics the sizes at the window scale
	 * @param scale the window scale
	 * @return the layout
	 */
	static PartyLayout of(Rectangle body, WindowStyle.Metrics metrics, double scale)
	{
		return new PartyLayout(body, metrics, scale);
	}

	/** @return the gap between parts, in pixels */
	int gap()
	{
		return gap;
	}

	/**
	 * Lays out the starter cards a new player chooses from, stacked in the main area under a caption line.
	 *
	 * @param count how many cards, at most {@link #STARTERS}
	 * @param captionH the height the caption above them takes
	 * @return one box per card
	 */
	List<Rectangle> starterCards(int count, int captionH)
	{
		int n = Math.max(1, Math.min(STARTERS, count));
		int top = main.y + captionH;
		int each = Math.max(1, (main.height - captionH - (n - 1) * gap) / n);
		List<Rectangle> cards = new ArrayList<>();
		for (int i = 0; i < Math.min(count, STARTERS); i++)
			cards.add(new Rectangle(main.x, top + i * (each + gap), main.width - metrics.nudge, each));

		return cards;
	}

	/**
	 * Splits a row into the places its parts are drawn in.
	 *
	 * @param row the row's box
	 * @return the parts
	 */
	Parts parts(Rectangle row)
	{
		return new Parts(row, metrics);
	}

	/** Where the parts of one row sit: portrait, name, icons, level, health and experience bars side by side. */
	static final class Parts
	{
		/** The padding inside the row, in pixels. */
		final int pad;

		/** The portrait, a square at the row's left. */
		final Rectangle portrait;

		/** The room for the name and the tags. */
		final Rectangle name;

		/** The room for the tags, at the right end of {@link #name}. */
		final Rectangle tags;

		/** The gender icon. */
		final Rectangle gender;

		/** The style icon. */
		final Rectangle style;

		/** The level text. */
		final Rectangle level;

		/** The health bar. */
		final Rectangle hp;

		/** The health percentage text, at the right of the health bar. */
		final Rectangle hpText;

		/** The experience bar, beside the health bar in the right half of the row. */
		final Rectangle xp;

		Parts(Rectangle row, WindowStyle.Metrics m)
		{
			pad = Math.max(2, m.pad / 3);
			int side = Math.max(1, row.height - 2 * pad);
			portrait = new Rectangle(row.x + m.slant, row.y + pad, side, side);
			int left = portrait.x + portrait.width + 2 * pad;
			int right = row.x + row.width - m.slant - pad;
			int hpH = Math.max(3, m.bar * 3 / 4);
			int xpH = Math.max(2, m.bar * 2 / 5);
			int barsTop = row.y + row.height - pad - hpH;
			int topH = Math.max(1, barsTop - row.y - pad / 2);
			int icon = Math.max(8, Math.min(topH, m.text));
			int iconY = row.y + pad / 2 + Math.max(0, (topH - icon) / 2);
			int levelW = Math.max(icon * 2, m.slant * 5);
			level = new Rectangle(right - levelW, row.y + pad / 2, levelW, topH);
			style = new Rectangle(level.x - pad - icon, iconY, icon, icon);
			gender = new Rectangle(style.x - pad - icon, iconY, icon, icon);
			name = new Rectangle(left, row.y + pad / 2, Math.max(0, gender.x - pad - left), topH);
			int tagW = Math.max(0, Math.min(name.width * 2 / 5, m.slant * 6));
			tags = new Rectangle(name.x + name.width - tagW, name.y, tagW, topH);
			int pctW = Math.max(m.slant * 4, 1);
			int half = Math.max(1, (right - left - pad) / 2);
			hp = new Rectangle(left, barsTop, Math.max(1, half - pctW - pad), hpH);
			int textH = Math.max(hpH + pad, m.text);
			int textTop = Math.max(row.y, Math.min(barsTop + hpH / 2 - textH / 2, row.y + row.height - textH));
			hpText = new Rectangle(hp.x + hp.width + pad, textTop, pctW, textH);
			int xpX = left + half + pad;
			xp = new Rectangle(xpX, barsTop + (hpH - xpH) / 2, Math.max(1, right - xpX), xpH);
		}
	}
}
