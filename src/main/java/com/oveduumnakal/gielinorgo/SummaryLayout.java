/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;

/**
 * Where everything on the Summary page sits: the portrait and who the creature is on the left, the stat, nature,
 * description, moves and bond boxes stacked on the right, and a row of action buttons along the foot. It is worked out
 * from the page area, the window scale and the height of one line of text, so the boxes hold the text at every size
 * the window can have. The description takes whatever height the other boxes leave, and has none when there is not
 * room for a line. Holds no RuneLite types.
 */
final class SummaryLayout
{
	/** The share of the page width the left column takes. */
	static final double LEFT_SHARE = 0.34;

	/** The labels of the action buttons along the foot, in order; the first is Back. */
	static final String[] BUTTONS = {"Back", "Rename", "Moves", "Make lead", "Storage", "Use item"};

	/** The labels of the action buttons along the foot of a stored creature's Summary, in order; the first is Back. */
	static final String[] STORED_BUTTONS = {"Back", "Rename", "Moves", "To team", "Release", "Use item"};

	/** The left column: portrait and identity. It is the creature's menu hit. */
	final Rectangle left;

	/** The portrait, a square at the top of the left column. */
	final Rectangle portrait;

	/** The room under the portrait for the title, species, gender, level and chips. */
	final Rectangle identity;

	/** The right column. */
	final Rectangle right;

	/** The Stats box, two stats to a line. */
	final Rectangle stats;

	/** The Nature box, one line. */
	final Rectangle nature;

	/** The Description box; its height is 0 when no line fits. */
	final Rectangle description;

	/** The Moves box, two moves to a line. */
	final Rectangle moves;

	/** The bond hearts and the evolution hint, one line. */
	final Rectangle bond;

	/** The action buttons, in the order of {@link #labels()}. */
	final Rectangle[] buttons;

	private final String[] labels;

	/** The gap between boxes, in pixels. */
	final int gap;

	/** The padding inside a box, in pixels. */
	final int pad;

	private SummaryLayout(Rectangle body, WindowStyle.Metrics m, int lineH, int smallH, String[] labels)
	{
		this.labels = labels.clone();
		gap = Math.max(2, m.pad / 3);
		pad = Math.max(3, m.pad / 3);
		int buttonH = Math.max(m.chipHeight, lineH + pad);
		int top = body.y;
		int mainH = Math.max(0, body.height - buttonH - gap);
		int leftW = (int) Math.round(body.width * LEFT_SHARE);
		left = new Rectangle(body.x, top, leftW, mainH);
		int side = Math.max(1, Math.min(leftW, mainH * 52 / 100));
		portrait = new Rectangle(left.x, top, side, side);
		identity = new Rectangle(left.x, top + side + gap, leftW, Math.max(0, mainH - side - gap));
		right = new Rectangle(body.x + leftW + gap, top, Math.max(0, body.width - leftW - gap), mainH);
		int rowH = lineH + 2;
		int statsH = 2 * rowH + 2 * pad;
		int natureH = lineH + 2 * pad;
		int movesH = 2 * (lineH + smallH) + 2 * pad;
		int bondH = lineH + 2 * pad;
		int used = statsH + natureH + movesH + bondH + 4 * gap;
		int descH = right.height - used;
		int minDesc = lineH + 2 * pad;
		if (descH < minDesc)
			descH = 0;

		int y = top;
		stats = new Rectangle(right.x, y, right.width, statsH);
		y += statsH + gap;
		nature = new Rectangle(right.x, y, right.width, natureH);
		y += natureH + gap;
		description = new Rectangle(right.x, y, right.width, descH);
		y += descH + (descH > 0 ? gap : 0);
		moves = new Rectangle(right.x, y, right.width, movesH);
		y += movesH + gap;
		bond = new Rectangle(right.x, y, right.width, bondH);
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
	 * Splits a two-column box into its cells, row by row.
	 *
	 * @param box the box
	 * @param index the cell, 0 to 3: top left, top right, bottom left, bottom right
	 * @return the cell inside the box's padding
	 */
	Rectangle cell(Rectangle box, int index)
	{
		int w = (box.width - 2 * pad) / 2;
		int h = (box.height - 2 * pad) / 2;
		return new Rectangle(box.x + pad + (index % 2) * w, box.y + pad + (index / 2) * h, w, h);
	}
}
