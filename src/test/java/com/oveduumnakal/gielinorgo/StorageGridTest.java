/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The Box geometry: cell counts, slots under the pointer, and that grid, bar and card fit the page at every size. */
public class StorageGridTest
{
	private static final double[] SCALES = {0.67, 1.0, 1.75};

	@Test
	public void theGridAlwaysHasASpareCellAndAtLeastThirtyTwo()
	{
		assertEquals(32, StorageGrid.cells(0));
		assertEquals(32, StorageGrid.cells(31));
		assertEquals(40, StorageGrid.cells(32));
		assertEquals(40, StorageGrid.cells(39));
		assertEquals(48, StorageGrid.cells(40));
		assertEquals(300, StorageGrid.cells(299));
		assertEquals(300, StorageGrid.cells(300));
		assertEquals(300, StorageGrid.cells(900));
	}

	@Test
	public void rowsRoundUp()
	{
		assertEquals(4, StorageGrid.rows(32));
		assertEquals(5, StorageGrid.rows(33));
		assertEquals(38, StorageGrid.rows(300));
	}

	@Test
	public void slotAtAgreesWithCell()
	{
		for (double scale : SCALES)
		{
			StorageGrid grid = new StorageGrid(WidgetKit.style(scale));
			for (int slot = 0; slot < 300; slot += 7)
			{
				Rectangle box = grid.cell(slot);
				assertEquals(slot, grid.slotAt(box.x, box.y, 0, 300));
				assertEquals(slot, grid.slotAt(box.x + box.width - 1, box.y + box.height - 1, 0, 300));
				assertEquals(slot, grid.slotAt(box.x + box.width / 2, box.y + box.height / 2 - 50, 50, 300));
			}
		}
	}

	@Test
	public void aPointInAGapOrPastTheEndIsNoCell()
	{
		StorageGrid grid = new StorageGrid(WidgetKit.style(1.0));
		Rectangle first = grid.cell(0);

		assertEquals(-1, grid.slotAt(first.x + first.width, first.y + 2, 0, 32));
		assertEquals(-1, grid.slotAt(first.x + 2, first.y + first.height, 0, 32));
		assertEquals(-1, grid.slotAt(-1, 5, 0, 32));
		assertEquals(-1, grid.slotAt(5, -1, 0, 32));
		assertEquals(-1, grid.slotAt(grid.gridWidth() + 3, 5, 0, 32));
		Rectangle past = grid.cell(32);
		assertEquals(-1, grid.slotAt(past.x + 2, past.y + 2, 0, 32));
		assertEquals(32, grid.slotAt(past.x + 2, past.y + 2, 0, 40));
	}

	@Test
	public void thereAreEightColumns()
	{
		StorageGrid grid = new StorageGrid(WidgetKit.style(1.0));

		assertEquals(8, StorageGrid.COLUMNS);
		Rectangle first = grid.cell(0);
		assertEquals(first.y, grid.cell(7).y);
		assertTrue(grid.cell(8).y > first.y);
		assertEquals(grid.cell(8).x, first.x);
		assertEquals(8 * grid.stride() - (grid.stride() - grid.cellSize()), grid.gridWidth());
	}

	@Test
	public void gridBarAndCardFitThePageAtEveryScale()
	{
		for (double scale : SCALES)
		{
			int width = (int) Math.round(WindowFit.DESIGN_W * scale);
			int height = (int) Math.round(WindowFit.DESIGN_H * scale);
			WindowLayout layout = WindowLayout.of(width, height);
			WindowStyle style = WidgetKit.style(layout.scale);
			StorageGrid grid = new StorageGrid(style);
			int pad = style.metrics().pad;
			Rectangle viewport = new Rectangle(layout.body.x + pad / 2, layout.body.y + 100, layout.body.width - pad,
				layout.body.height - 120);
			Rectangle card = grid.detail(viewport, pad);

			assertFalse("the card shows at scale " + scale, card.isEmpty());
			assertTrue("scale " + scale, grid.areaWidth() < card.x - viewport.x);
			assertTrue("card inside the body at scale " + scale,
				card.x + card.width <= viewport.x + viewport.width);
			assertTrue("card at least 150 units at scale " + scale, card.width >= style.px(150));
		}
	}

	@Test
	public void aNarrowAreaLeavesTheCardOut()
	{
		StorageGrid grid = new StorageGrid(WidgetKit.style(1.0));

		Rectangle narrow = new Rectangle(0, 0, grid.areaWidth() + 100, 200);

		assertTrue(grid.detail(narrow, 12).isEmpty());
	}

	@Test
	public void fourFullRowsShowInTheDesignSizeViewport()
	{
		WindowStyle style = WidgetKit.style(1.0);
		StorageGrid grid = new StorageGrid(style);
		int viewportHeight = 276;

		assertEquals(4, viewportHeight / grid.stride());
		assertEquals(68, grid.stride());
	}
}
