/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the scrolling list: only visible rows paint, the offset clamps, and the wheel, bar and drag work. */
public class ScrollListTest
{
	private static final WindowStyle STYLE = WidgetKit.style(1.0);
	private static final Rectangle VIEW = new Rectangle(10, 20, 200, 100);

	private static ScrollList list(int rows)
	{
		ScrollList list = new ScrollList();
		list.layout(VIEW, 20, rows, STYLE);
		return list;
	}

	@Test
	public void onlyTheVisibleRowsArePaintedEvenInAHugeList()
	{
		ScrollList list = list(1254);
		BufferedImage image = WidgetKit.canvas(260, 200);
		Graphics2D g = WidgetKit.graphics(image);
		List<Integer> painted = new ArrayList<>();
		list.paint(g, STYLE, (graphics, index, box) -> painted.add(index));
		g.dispose();

		assertEquals(5, painted.size());
		assertEquals(0, (int) painted.get(0));
		assertEquals(4, (int) painted.get(4));

		list.setOffset(30);
		painted.clear();
		g = WidgetKit.graphics(image);
		list.paint(g, STYLE, (graphics, index, box) -> painted.add(index));
		g.dispose();

		assertEquals("a partly shown row at each end", 6, painted.size());
		assertEquals(1, (int) painted.get(0));
		assertEquals(6, (int) painted.get(5));
	}

	@Test
	public void rowsArePaintedInsideTheViewportClipAndAtTheirScrolledPlace()
	{
		ScrollList list = list(50);
		list.setOffset(10);
		BufferedImage image = WidgetKit.canvas(260, 200);
		Graphics2D g = WidgetKit.graphics(image);
		List<Rectangle> boxes = new ArrayList<>();
		list.paint(g, STYLE, (graphics, index, box) ->
		{
			boxes.add(box);
			graphics.fillRect(box.x - 5, box.y - 5, box.width + 10, box.height + 10);
		});
		g.dispose();

		assertEquals(new Rectangle(10, 10, list.contentWidth(), 20), boxes.get(0));
		assertEquals("nothing paints above the viewport", 0, WidgetKit.painted(image, 0, 0, 260, 20));
		assertEquals("nothing paints below the viewport", 0, WidgetKit.painted(image, 0, 120, 260, 80));
		assertTrue(WidgetKit.painted(image, 10, 20, 100, 100) > 0);
	}

	@Test
	public void theOffsetAlwaysClampsToTheContent()
	{
		ScrollList list = list(10);

		assertEquals(200, list.contentHeight());
		assertEquals(100, list.maxOffset());
		list.setOffset(-50);
		assertEquals(0, list.offset());
		list.setOffset(9_999);
		assertEquals(100, list.offset());
		list.scrollBy(-30);
		assertEquals(70, list.offset());
		list.scrollBy(500);
		assertEquals(100, list.offset());
	}

	@Test
	public void aShorterListOrABiggerViewportClampsTheOffsetAgain()
	{
		ScrollList list = list(10);
		list.setOffset(100);

		list.layout(VIEW, 20, 7, STYLE);
		assertEquals(40, list.offset());
		list.layout(VIEW, 20, 3, STYLE);
		assertEquals(0, list.offset());
		assertFalse(list.scrollable());
	}

	@Test
	public void aListThatFitsHasNoBarAndFullWidth()
	{
		ScrollList list = list(4);

		assertFalse(list.scrollable());
		assertEquals(0, list.maxOffset());
		assertEquals(200, list.contentWidth());
		assertTrue(list.trackBounds().isEmpty());
		assertTrue(list.thumbBounds().isEmpty());
		BufferedImage image = WidgetKit.canvas(260, 200);
		Graphics2D g = WidgetKit.graphics(image);
		list.paintBar(g, STYLE);
		g.dispose();
		assertEquals(0, WidgetKit.painted(image, 0, 0, 260, 200));
	}

	@Test
	public void anEmptyListHasNoVisibleRows()
	{
		ScrollList list = list(0);

		assertEquals(-1, list.lastVisible());
		assertEquals(0, list.firstVisible());
		assertEquals(-1, list.rowAt(20, 30));
		assertNull(list.rowBounds(0));
	}

	@Test
	public void theBarTakesRoomFromTheRowsAndItsHandleIsInTheGroove()
	{
		ScrollList list = list(50);

		assertTrue(list.scrollable());
		assertTrue(list.contentWidth() < VIEW.width);
		Rectangle track = list.trackBounds();
		Rectangle thumb = list.thumbBounds();
		assertEquals(VIEW.x + VIEW.width, track.x + track.width);
		assertEquals(VIEW.height, track.height);
		assertTrue(track.contains(thumb));
		assertEquals("at the top", track.y, thumb.y);
		list.setOffset(list.maxOffset());
		assertEquals("at the bottom", track.y + track.height, list.thumbBounds().y + list.thumbBounds().height);
	}

	@Test
	public void theHandleHasAMinimumLengthInAHugeList()
	{
		ScrollList list = list(100_000);

		assertTrue(list.thumbBounds().height >= STYLE.px(ScrollList.MIN_THUMB_UNITS));
	}

	@Test
	public void rowAtAndRowBoundsAgreeAndSkipTheBar()
	{
		ScrollList list = list(50);
		list.setOffset(30);

		assertEquals(1, list.rowAt(20, 20));
		assertEquals(1, list.rowAt(20, 29));
		assertEquals(2, list.rowAt(20, 30));
		assertEquals(6, list.rowAt(20, 119));
		assertEquals(-1, list.rowAt(20, 19));
		assertEquals(-1, list.rowAt(20, 120));
		assertEquals(-1, list.rowAt(VIEW.x + list.contentWidth() + 1, 50));
		assertEquals(-1, list.rowAt(5, 50));
		Rectangle first = list.rowBounds(1);
		assertEquals("cut to the viewport", new Rectangle(10, 20, list.contentWidth(), 10), first);
		assertEquals(new Rectangle(10, 30, list.contentWidth(), 20), list.rowBounds(2));
		assertNull("scrolled out of view", list.rowBounds(0));
		assertNull("not yet in view", list.rowBounds(7));
		assertNull(list.rowBounds(50));
	}

	@Test
	public void theWheelScrollsAndIsConsumedWhenOverTheListEvenAtTheEnds()
	{
		ScrollList list = list(50);

		assertTrue(list.onWheel(50, 50, 1));
		assertEquals(30, list.offset());
		assertTrue(list.onWheel(50, 50, -1));
		assertEquals(0, list.offset());
		assertTrue("at the top and scrolling up, still consumed", list.onWheel(50, 50, -3));
		assertEquals(0, list.offset());
		list.setOffset(list.maxOffset());
		assertTrue("at the end, still consumed", list.onWheel(50, 50, 5));
		assertEquals(list.maxOffset(), list.offset());
		assertFalse("outside the list the wheel passes", list.onWheel(300, 50, 1));
		assertFalse(list.onWheel(50, 10, 1));
	}

	@Test
	public void aTrackpadStyleFractionOfANotchMovesPartOfARow()
	{
		ScrollList list = list(50);

		list.onWheel(50, 50, 0.2);

		assertEquals(6, list.offset());
	}

	@Test
	public void aWheelOverAListThatFitsIsStillConsumedSoTheCameraDoesNotZoom()
	{
		ScrollList list = list(2);

		assertTrue(list.onWheel(50, 50, 1));
		assertEquals(0, list.offset());
	}

	@Test
	public void draggingTheHandleScrollsAndTheReleaseIsNotAClick()
	{
		ScrollList list = list(50);
		Rectangle thumb = list.thumbBounds();
		int x = thumb.x + 2;

		assertTrue(list.onPress(x, thumb.y + 3));
		assertTrue(list.dragging());
		assertTrue(list.onDrag(x, thumb.y + 3 + 20));
		assertTrue(list.offset() > 0);
		list.onDrag(x, 5_000);
		assertEquals(list.maxOffset(), list.offset());
		assertEquals(list.trackBounds().y + list.trackBounds().height,
			list.thumbBounds().y + list.thumbBounds().height);
		list.onDrag(x, -5_000);
		assertEquals(0, list.offset());
		assertTrue(list.onRelease());
		assertFalse(list.dragging());
	}

	@Test
	public void clickingTheGrooveOffTheHandlePagesUpOrDown()
	{
		ScrollList list = list(50);
		Rectangle track = list.trackBounds();

		assertTrue(list.onPress(track.x + 2, track.y + track.height - 2));
		assertEquals(VIEW.height, list.offset());
		list.onRelease();
		assertTrue(list.onPress(track.x + 2, track.y + track.height - 2));
		assertEquals(2 * VIEW.height, list.offset());
		list.onRelease();
		list.onPress(track.x + 2, track.y);
		list.onRelease();
		assertTrue(list.offset() <= VIEW.height);
	}

	@Test
	public void dragRowsOnlyScrollsAfterTheThresholdAndThenTheReleaseIsNotAClick()
	{
		ScrollList list = list(50);

		assertTrue(list.onPress(50, 60));
		assertFalse("under the threshold", list.onDrag(50, 60 + ScrollList.DRAG_THRESHOLD - 1));
		assertEquals(0, list.offset());
		assertFalse("so the release is a click", list.onRelease());

		list.onPress(50, 60);
		assertTrue(list.onDrag(50, 30));
		assertEquals("rows follow the pointer", 30, list.offset());
		assertTrue(list.dragging());
		list.onDrag(50, 200);
		assertEquals(0, list.offset());
		assertTrue("a drag is not a click", list.onRelease());
		assertFalse(list.dragging());
	}

	@Test
	public void aPressOutsideTheListIsNotTaken()
	{
		ScrollList list = list(50);

		assertFalse(list.onPress(400, 400));
		assertFalse(list.onDrag(400, 300));
		assertFalse(list.onRelease());
	}

	@Test
	public void aListThatFitsDoesNotDragScroll()
	{
		ScrollList list = list(3);

		list.onPress(50, 60);
		assertFalse(list.onDrag(50, 10));
		assertFalse(list.onRelease());
		assertEquals(0, list.offset());
	}

	@Test
	public void ensureVisibleScrollsJustFarEnoughForKeyboardFollow()
	{
		ScrollList list = list(50);

		list.ensureVisible(3);
		assertEquals("already in view", 0, list.offset());
		list.ensureVisible(5);
		assertEquals("one row below the view", 20, list.offset());
		list.ensureVisible(30);
		assertEquals(30 * 20 + 20 - 100, list.offset());
		list.ensureVisible(2);
		assertEquals(40, list.offset());
		list.ensureVisible(-1);
		list.ensureVisible(99);
		assertEquals(40, list.offset());
	}

	@Test
	public void theBarPaintsInThemeColours()
	{
		ScrollList list = list(50);
		BufferedImage image = WidgetKit.canvas(260, 200);
		Graphics2D g = WidgetKit.graphics(image);
		list.paintBar(g, STYLE);
		g.dispose();

		Rectangle thumb = list.thumbBounds();
		int pixel = image.getRGB(thumb.x + thumb.width / 2, thumb.y + thumb.height / 2);
		assertEquals(STYLE.color(ThemeColor.SCROLL_THUMB).getRGB(), pixel);
		Rectangle track = list.trackBounds();
		assertTrue(WidgetKit.painted(image, track.x, track.y, track.width, track.height) > 0);
	}
}
