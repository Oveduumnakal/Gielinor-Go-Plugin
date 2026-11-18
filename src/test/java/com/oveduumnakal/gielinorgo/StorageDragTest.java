/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The Box drag state machine: the threshold, click against drag, drops and cancels. */
public class StorageDragTest
{
	@Test
	public void aPressThatBarelyMovesIsAClick()
	{
		StorageDrag drag = new StorageDrag(6);

		drag.press(3, 100, 100);
		assertFalse(drag.moved(103, 102));
		StorageDrag.Result result = drag.release(3);

		assertEquals(StorageDrag.Kind.CLICK, result.kind());
		assertEquals(3, result.from());
	}

	@Test
	public void movingTheThresholdStartsADrag()
	{
		StorageDrag drag = new StorageDrag(6);

		drag.press(3, 100, 100);
		assertFalse(drag.moved(105, 100));
		assertTrue(drag.moved(106, 100));
		assertTrue(drag.dragging());
		assertEquals(3, drag.from());
		assertEquals(106, drag.pointerX());
		assertEquals(100, drag.pointerY());
	}

	@Test
	public void onceDraggingAMoveBackIsStillADrag()
	{
		StorageDrag drag = new StorageDrag(6);
		drag.press(1, 0, 0);
		drag.moved(20, 0);

		assertTrue(drag.moved(1, 0));
		assertEquals(StorageDrag.Kind.CANCEL, drag.release(1).kind());
	}

	@Test
	public void droppingOnAnotherCellIsADrop()
	{
		StorageDrag drag = new StorageDrag(6);
		drag.press(1, 0, 0);
		drag.moved(0, 50);

		StorageDrag.Result result = drag.release(4);

		assertEquals(StorageDrag.Kind.DROP, result.kind());
		assertEquals(1, result.from());
		assertEquals(4, result.to());
		assertFalse(drag.dragging());
		assertFalse(drag.pressed());
	}

	@Test
	public void droppingOnAnEmptyCellIsADropToo()
	{
		StorageDrag drag = new StorageDrag(6);
		drag.press(1, 0, 0);
		drag.moved(0, 50);

		assertEquals(StorageDrag.Kind.DROP, drag.release(31).kind());
	}

	@Test
	public void droppingOutsideTheGridCancels()
	{
		StorageDrag drag = new StorageDrag(6);
		drag.press(1, 0, 0);
		drag.moved(0, 50);

		StorageDrag.Result result = drag.release(-1);

		assertEquals(StorageDrag.Kind.CANCEL, result.kind());
		assertEquals(-1, result.to());
	}

	@Test
	public void droppingOnTheSourceCancels()
	{
		StorageDrag drag = new StorageDrag(6);
		drag.press(2, 0, 0);
		drag.moved(40, 0);

		assertEquals(StorageDrag.Kind.CANCEL, drag.release(2).kind());
	}

	@Test
	public void cancelForgetsThePress()
	{
		StorageDrag drag = new StorageDrag(6);
		drag.press(2, 0, 0);
		drag.moved(40, 0);

		drag.cancel();

		assertFalse(drag.dragging());
		assertEquals(StorageDrag.Kind.NONE, drag.release(5).kind());
	}

	@Test
	public void aPressOnNoCreatureNeverDrags()
	{
		StorageDrag drag = new StorageDrag(6);
		drag.press(-1, 0, 0);

		assertFalse(drag.moved(90, 90));
		assertEquals(StorageDrag.Kind.NONE, drag.release(3).kind());
	}

	@Test
	public void theThresholdGrowsWithTheScaleButNeverBelowFour()
	{
		assertEquals(4, StorageDrag.threshold(WidgetKit.style(0.5)));
		assertEquals(6, StorageDrag.threshold(WidgetKit.style(1.0)));
		assertEquals(11, StorageDrag.threshold(WidgetKit.style(1.75)));
	}
}
