/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;

/**
 * The geometry of the Storage Box view: eight columns of square cells in a scrolling area, and a detail card to the
 * right of it. A cell's slot is its place in the grid, row by row, which is also the creature's place in storage
 * while the list is in box order and unfiltered. The grid never has empty gaps between creatures: the empty cells
 * are all after the last one, plus enough to fill the row and a spare one to drop on. Pure arithmetic over the style,
 * so it is tested without a canvas. Holds no RuneLite types.
 */
final class StorageGrid
{
	/** How many cells sit in a row. */
	static final int COLUMNS = 8;

	/** The side of a cell, in layout units. */
	static final double CELL_UNITS = 64;

	/** The gap between cells, in layout units. */
	static final double CELL_GAP_UNITS = 4;

	/** The fewest cells the grid shows, so an empty box still looks like a box. */
	static final int MIN_CELLS = 32;

	/** The width of the detail card, in layout units. */
	static final double DETAIL_UNITS = 190;

	/** The narrowest the detail card may get before it is left out, in layout units. */
	static final double MIN_DETAIL_UNITS = 150;

	private final int cell;
	private final int gap;
	private final int bar;
	private final int detail;
	private final int minDetail;

	/**
	 * Creates the geometry for a style.
	 *
	 * @param style the style, which has the window scale
	 */
	StorageGrid(WindowStyle style)
	{
		cell = Math.max(8, style.px(CELL_UNITS));
		gap = Math.max(1, style.px(CELL_GAP_UNITS));
		bar = Math.max(4, style.px(ScrollList.BAR_UNITS));
		detail = style.px(DETAIL_UNITS);
		minDetail = style.px(MIN_DETAIL_UNITS);
	}

	/**
	 * @param shownCount how many creatures are shown
	 * @return how many cells the grid has: at least {@link #MIN_CELLS}, a whole number of rows, with room for one
	 *         more creature than are shown, and never more than storage holds
	 */
	static int cells(int shownCount)
	{
		int wanted = Math.max(MIN_CELLS, (shownCount + 1 + COLUMNS - 1) / COLUMNS * COLUMNS);
		return Math.min(SaveData.STORAGE_SIZE, wanted);
	}

	/**
	 * @param cells the number of cells
	 * @return how many rows they fill
	 */
	static int rows(int cells)
	{
		return (cells + COLUMNS - 1) / COLUMNS;
	}

	/** @return the side of a cell in pixels */
	int cellSize()
	{
		return cell;
	}

	/** @return the distance from one cell to the next, in pixels; also a row's height in the scroll list */
	int stride()
	{
		return cell + gap;
	}

	/** @return the width of the eight columns in pixels */
	int gridWidth()
	{
		return COLUMNS * stride() - gap;
	}

	/** @return the width of the scroll area: the columns and the scroll bar */
	int areaWidth()
	{
		return gridWidth() + bar;
	}

	/**
	 * Gives the box of a cell, in the grid's own coordinates (the top-left of the first cell is 0, 0).
	 *
	 * @param slot the cell's place in the grid, from 0
	 * @return the cell's box
	 */
	Rectangle cell(int slot)
	{
		return new Rectangle(slot % COLUMNS * stride(), slot / COLUMNS * stride(), cell, cell);
	}

	/**
	 * Finds the cell under a point.
	 *
	 * @param x the point's x, from the grid's left edge
	 * @param y the point's y, from the top of the visible part of the grid
	 * @param offset how far the grid is scrolled, in pixels
	 * @param cells how many cells the grid has
	 * @return the cell's place, or -1 when the point is in a gap, off the columns or past the last cell
	 */
	int slotAt(int x, int y, int offset, int cells)
	{
		int top = y + offset;
		if (x < 0 || top < 0)
			return -1;

		int column = x / stride();
		int row = top / stride();
		if (column >= COLUMNS || x % stride() >= cell || top % stride() >= cell)
			return -1;

		int slot = row * COLUMNS + column;
		return slot < cells ? slot : -1;
	}

	/**
	 * Gives where the detail card goes: right of the scroll area, as wide as {@link #DETAIL_UNITS} allows.
	 *
	 * @param viewport the whole area below the controls
	 * @param pad the space between the area and the card
	 * @return the card's box, or an empty box when fewer than {@link #MIN_DETAIL_UNITS} units are left
	 */
	Rectangle detail(Rectangle viewport, int pad)
	{
		int left = viewport.x + areaWidth() + pad;
		int room = viewport.x + viewport.width - left;
		if (room < minDetail)
			return new Rectangle();

		return new Rectangle(left, viewport.y, Math.min(detail, room), viewport.height);
	}
}
