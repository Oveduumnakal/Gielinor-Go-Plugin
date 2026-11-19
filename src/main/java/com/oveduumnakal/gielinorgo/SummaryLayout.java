/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;

/**
 * Where everything on the Summary page sits, in two columns. Left: a header pill with the style badge, the chips, a
 * large image box and the Description box. Right: the Stats box with the Nature box beside it and the bond pill under
 * that, and the Moves box under both. A row of action buttons runs along the foot. It is worked out from the page
 * area, the window scale and the height of one line of text, so the boxes hold the text at every size the window can
 * have. The Description box takes what the image box can spare and has no height when there is not room for its
 * heading and one line. Holds no RuneLite types.
 */
final class SummaryLayout
{
	/** The share of the page width the left column takes. */
	static final double LEFT_SHARE = 0.44;

	/** The share of the right column the Stats box takes; the Nature box has the rest. */
	static final double STATS_SHARE = 0.58;

	/** The lines of the Stats box: health, experience, attack, defence and speed. */
	static final int BAR_ROWS = 5;

	/** The labels of the action buttons along the foot, in order; the first is Back. */
	static final String[] BUTTONS = {"Back", "Rename", "Moves", "Make lead", "Storage", "Use item"};

	/** The labels of the action buttons along the foot of a stored creature's Summary, in order; the first is Back. */
	static final String[] STORED_BUTTONS = {"Back", "Rename", "Moves", "To team", "Release", "Use item"};

	/** The left column: header, chips, image and description. It is the creature's menu hit. */
	final Rectangle left;

	/** The header pill; the round style badge takes its left end, as tall as the pill. */
	final Rectangle header;

	/** The row of chips under the header. */
	final Rectangle chips;

	/** The large image box the creature portrait fills. */
	final Rectangle portrait;

	/** The Description box; its height is 0 when no heading and line fit. */
	final Rectangle description;

	/** The right column. */
	final Rectangle right;

	/** The Stats box, one line per stat. */
	final Rectangle stats;

	/** The Nature box, beside Stats. */
	final Rectangle nature;

	/** The pill of bond hearts under the Nature box. */
	final Rectangle bond;

	/** The Moves box, under Stats and Nature. */
	final Rectangle moves;

	/** The action buttons, in the order of {@link #labels()}. */
	final Rectangle[] buttons;

	private final String[] labels;

	/** The gap between boxes, in pixels. */
	final int gap;

	/** The padding inside a box, in pixels. */
	final int pad;

	/** The height of a box's heading strip, in pixels. */
	final int headingH;

	private final int lineH;
	private final int smallH;

	private SummaryLayout(Rectangle body, WindowStyle.Metrics m, int lineH, int smallH, String[] labels)
	{
		this.labels = labels.clone();
		this.lineH = lineH;
		this.smallH = smallH;
		gap = Math.max(2, m.pad / 3);
		pad = Math.max(3, m.pad / 3);
		headingH = smallH + pad;
		int buttonH = Math.max(m.chipHeight, lineH + pad);
		int mainH = Math.max(0, body.height - buttonH - gap);
		int leftW = (int) Math.round(body.width * LEFT_SHARE);
		left = new Rectangle(body.x, body.y, leftW, mainH);
		int headerH = Math.max(m.chipHeight, lineH * 3 / 2);
		header = new Rectangle(left.x, left.y, leftW, Math.min(headerH, mainH));
		int chipsY = header.y + header.height + gap;
		chips = new Rectangle(left.x, chipsY, leftW, Math.max(0, Math.min(m.chipHeight, left.y + mainH - chipsY)));
		int imageY = chips.y + chips.height + gap;
		int avail = Math.max(0, left.y + mainH - imageY);
		int imageMin = Math.max(2 * lineH, 24);
		int descWant = 2 * pad + headingH + 4 * smallH;
		int descMin = 2 * pad + headingH + smallH;
		int descH = Math.min(descWant, avail - imageMin - gap);
		if (descH < descMin)
			descH = 0;

		int imageH = Math.max(0, avail - (descH > 0 ? descH + gap : 0));
		portrait = new Rectangle(left.x, imageY, leftW, imageH);
		description = new Rectangle(left.x, imageY + imageH + (descH > 0 ? gap : 0), leftW, descH);

		right = new Rectangle(body.x + leftW + gap, body.y, Math.max(0, body.width - leftW - gap), mainH);
		int statsW = (int) Math.round(right.width * STATS_SHARE);
		int natureW = Math.max(0, right.width - statsW - gap);
		int statsWant = 2 * pad + headingH + BAR_ROWS * (smallH + 3);
		int movesMin = 2 * pad + headingH + 2 * (lineH + pad) + gap;
		int statsH = Math.min(statsWant, right.height - gap - movesMin);
		if (statsH < (right.height - gap) / 3)
			statsH = Math.max(0, (right.height - gap) / 2);

		stats = new Rectangle(right.x, right.y, statsW, statsH);
		int bondH = Math.min(Math.max(smallH + pad, 8), statsH / 3);
		nature = new Rectangle(right.x + statsW + gap, right.y, natureW, Math.max(0, statsH - bondH - gap));
		bond = new Rectangle(nature.x, nature.y + nature.height + gap, natureW, bondH);
		int movesY = right.y + statsH + gap;
		moves = new Rectangle(right.x, movesY, right.width, Math.max(0, right.y + mainH - movesY));
		buttons = new Rectangle[labels.length];
		int n = labels.length;
		int each = Math.max(1, (body.width - (n - 1) * gap) / n);
		for (int i = 0; i < n; i++)
			buttons[i] = new Rectangle(body.x + i * (each + gap), body.y + body.height - buttonH, each, buttonH);
	}

	/**
	 * Works the layout out.
	 *
	 * @param body the page area in window pixels
	 * @param metrics the sizes at the window scale
	 * @param lineH the height of one line of body text, in pixels
	 * @param smallH the height of one line of small text, in pixels
	 * @return the layout
	 */
	static SummaryLayout of(Rectangle body, WindowStyle.Metrics metrics, int lineH, int smallH)
	{
		return of(body, metrics, lineH, smallH, BUTTONS);
	}

	/**
	 * Works the layout out for a given set of buttons.
	 *
	 * @param body the page area in window pixels
	 * @param metrics the sizes at the window scale
	 * @param lineH the height of one line of body text, in pixels
	 * @param smallH the height of one line of small text, in pixels
	 * @param labels the labels of the action buttons, such as {@link #BUTTONS} or {@link #STORED_BUTTONS}
	 * @return the layout
	 */
	static SummaryLayout of(Rectangle body, WindowStyle.Metrics metrics, int lineH, int smallH, String[] labels)
	{
		return new SummaryLayout(body, metrics, lineH, smallH, labels);
	}

	/** @return the labels of the action buttons, in order, as a copy */
	String[] labels()
	{
		return labels.clone();
	}

	/**
	 * Gives the strip a box's heading takes along its top.
	 *
	 * @param box a box of the page: Stats, Nature, Moves or Description
	 * @return the heading's rectangle, which the painter underlines
	 */
	Rectangle heading(Rectangle box)
	{
		return new Rectangle(box.x, box.y, box.width, Math.min(box.height, headingH));
	}

	/**
	 * Gives the room of one line of the Stats box.
	 *
	 * @param index the line, 0 for health to {@link #BAR_ROWS} minus 1
	 * @return the line inside the box's padding, under its heading
	 */
	Rectangle statRow(int index)
	{
		int rowH = Math.max(1, (stats.height - headingH - 2 * pad) / BAR_ROWS);
		return new Rectangle(stats.x + pad, stats.y + headingH + pad + index * rowH, stats.width - 2 * pad, rowH);
	}

	/**
	 * Gives the pill of one move, two to a line.
	 *
	 * @param index the move's place, 0 to {@link SummaryContent#MAX_MOVES} minus 1
	 * @return the pill's rectangle inside the Moves box, under its heading
	 */
	Rectangle movePill(int index)
	{
		int w = Math.max(1, (moves.width - 2 * pad - gap) / 2);
		int inner = Math.max(1, moves.height - headingH - 2 * pad);
		int h = Math.max(1, Math.min(lineH * 3 / 2, (inner - gap) / 2));
		int x = moves.x + pad + (index % 2) * (w + gap);
		int y = moves.y + headingH + pad + (index / 2) * (h + gap);
		return new Rectangle(x, y, w, h);
	}

	/**
	 * Gives the room of the Description box's text, between its heading and its "Grows into..." line.
	 *
	 * @return the text area; its height is 0 or less when there is no room for a line
	 */
	Rectangle descriptionText()
	{
		int top = description.y + headingH + pad;
		return new Rectangle(description.x + pad, top, description.width - 2 * pad,
			description.y + description.height - pad - smallH - top);
	}

	/**
	 * Gives the room of the "Grows into..." line, along the foot of the Description box.
	 *
	 * @return the line's rectangle
	 */
	Rectangle growsLine()
	{
		return new Rectangle(description.x + pad, description.y + description.height - pad - smallH,
			description.width - 2 * pad, smallH);
	}
}
