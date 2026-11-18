/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;

/**
 * Where everything on the Bag page sits, worked out from the page area and the window scale alone: the trainer box
 * across the top, the jars on the left, and the items and the eggs on the right. All lengths are layout units times
 * the window scale, so both themes share the same boxes. Holds no RuneLite types.
 */
final class BagLayout
{
	/** How many jar cells sit side by side. */
	static final int JAR_COLUMNS = 4;

	/** The share of the page width the jars box takes. */
	static final double JARS_SHARE = 0.55;

	private static final double GAP_UNITS = 8;
	private static final double TRAINER_UNITS = 112;
	private static final double HEADING_UNITS = 30;
	private static final double LINE_UNITS = 32;
	private static final double JAR_CELL_UNITS = 46;
	private static final double ITEM_CELL_UNITS = 46;
	private static final double EGG_ROW_UNITS = 40;

	/** The trainer box: title, level bar, wins and the next-jar strip. */
	final Rectangle trainer;

	/** The title line in the trainer box; a click on it changes the title. */
	final Rectangle titleLine;

	/** The line with the number of battles won, at the right of the title line. */
	final Rectangle winsLine;

	/** The trainer level bar. */
	final Rectangle trainerBar;

	/** The line of words over the next-jar strip. */
	final Rectangle stepsLine;

	/** The next-jar strip. */
	final Rectangle stepsBar;

	/** The jars box. */
	final Rectangle jars;

	/** The heading of the jars box. */
	final Rectangle jarsHeading;

	/** One cell for each jar shown, in order. */
	final Rectangle[] jarCells;

	/** The items box. */
	final Rectangle items;

	/** The heading of the items box. */
	final Rectangle itemsHeading;

	/** One cell for each item, in order. */
	final Rectangle[] itemCells;

	/** The eggs box. */
	final Rectangle eggs;

	/** The heading of the eggs box. */
	final Rectangle eggsHeading;

	/** One row for each egg that fits, in order. */
	final Rectangle[] eggRows;

	/** The line under the last egg row that says how many more eggs there are, or an empty box. */
	final Rectangle eggsMore;

	private BagLayout(Rectangle body, WindowStyle.Metrics m, double scale, int jarCount, int eggCount)
	{
		int gap = px(GAP_UNITS, scale);
		int pad = m.pad;
		int heading = px(HEADING_UNITS, scale);
		trainer = new Rectangle(body.x, body.y, body.width, Math.min(body.height, px(TRAINER_UNITS, scale)));
		int line = px(LINE_UNITS, scale);
		int inner = Math.max(0, trainer.width - 2 * pad);
		int winsW = inner / 3;
		titleLine = new Rectangle(trainer.x + pad, trainer.y + pad / 2, inner - winsW, line);
		winsLine = new Rectangle(titleLine.x + titleLine.width, titleLine.y, winsW, line);
		trainerBar = new Rectangle(trainer.x + pad, titleLine.y + line + pad / 2, inner, Math.max(3, m.bar));
		stepsLine = new Rectangle(trainer.x + pad, trainerBar.y + trainerBar.height + pad / 2, inner, line);
		stepsBar = new Rectangle(trainer.x + pad, stepsLine.y + line, inner, Math.max(3, m.bar / 2));
		int top = trainer.y + trainer.height + gap;
		int rest = Math.max(0, body.y + body.height - top);
		int leftW = (int) Math.round((body.width - gap) * JARS_SHARE);
		jars = new Rectangle(body.x, top, leftW, rest);
		jarsHeading = new Rectangle(jars.x + pad, jars.y + pad / 2, jars.width - 2 * pad, heading);
		int columns = jarCount > JAR_COLUMNS * 2 ? JAR_COLUMNS + 1 : JAR_COLUMNS;
		int jarRows = Math.max(1, (jarCount + columns - 1) / columns);
		int jarRoom = jars.y + jars.height - (jarsHeading.y + jarsHeading.height + pad / 2) - pad;
		int jarH = Math.max(1, Math.min(px(JAR_CELL_UNITS, scale), (jarRoom - (jarRows - 1) * (gap / 2)) / jarRows));
		jarCells = grid(jars, jarsHeading, pad, gap / 2, jarH, columns, jarCount);
		int rightX = jars.x + jars.width + gap;
		int rightW = body.x + body.width - rightX;
		int itemsH = Math.min(rest, heading + pad + px(ITEM_CELL_UNITS, scale) + pad);
		items = new Rectangle(rightX, top, rightW, itemsH);
		itemsHeading = new Rectangle(items.x + pad, items.y + pad / 2, items.width - 2 * pad, heading);
		itemCells = grid(items, itemsHeading, pad, gap / 2, px(ITEM_CELL_UNITS, scale), ItemType.values().length,
			ItemType.values().length);
		int eggsTop = items.y + items.height + gap;
		eggs = new Rectangle(rightX, eggsTop, rightW, Math.max(0, body.y + body.height - eggsTop));
		eggsHeading = new Rectangle(eggs.x + pad, eggs.y + pad / 2, eggs.width - 2 * pad, heading);
		int rowH = px(EGG_ROW_UNITS, scale);
		int room = Math.max(0, eggs.y + eggs.height - (eggsHeading.y + heading + pad / 2) - pad);
		int capacity = room / Math.max(1, rowH);
		int rows = eggCount <= capacity ? eggCount : Math.max(0, capacity - 1);
		eggRows = new Rectangle[rows];
		for (int i = 0; i < rows; i++)
			eggRows[i] = new Rectangle(eggs.x + pad, eggsHeading.y + heading + pad / 2 + i * rowH,
				eggs.width - 2 * pad, rowH - gap / 2);

		eggsMore = rows < eggCount && capacity > 0
			? new Rectangle(eggs.x + pad, eggsHeading.y + heading + pad / 2 + rows * rowH, eggs.width - 2 * pad, rowH)
			: new Rectangle();
	}

	/**
	 * Works the layout out.
	 *
	 * @param body the page area
	 * @param metrics the style's measures
	 * @param scale the window scale
	 * @param jarCount how many jar cells to place
	 * @param eggCount how many eggs the player has
	 * @return the layout
	 */
	static BagLayout of(Rectangle body, WindowStyle.Metrics metrics, double scale, int jarCount, int eggCount)
	{
		return new BagLayout(body, metrics, scale, jarCount, eggCount);
	}

	private static Rectangle[] grid(Rectangle box, Rectangle heading, int pad, int gap, int cellH, int columns,
		int count)
	{
		Rectangle[] cells = new Rectangle[count];
		int width = Math.max(1, (box.width - 2 * pad - (columns - 1) * gap) / columns);
		int top = heading.y + heading.height + pad / 2;
		for (int i = 0; i < count; i++)
			cells[i] = new Rectangle(box.x + pad + (i % columns) * (width + gap), top + (i / columns) * (cellH + gap),
				width, cellH);

		return cells;
	}

	private static int px(double units, double scale)
	{
		return (int) Math.round(units * scale);
	}
}
