/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Paints the toggle and the notice overlays headless, in each state they have. */
public class WindowOverlaysTest
{
	private long now = 5_000L;
	private MotionMode motion = MotionMode.FULL;

	private WindowToggle toggle()
	{
		return toggle(WindowTheme.RUNELITE_DARK);
	}

	private WindowToggle toggle(WindowTheme look)
	{
		BufferedImage jar = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = jar.createGraphics();
		g.fillRect(2, 2, 14, 14);
		g.dispose();
		return new WindowToggle(() -> look, () -> motion, () -> now, jar);
	}

	private static int opaque(BufferedImage image)
	{
		int count = 0;
		for (int y = 0; y < image.getHeight(); y++)
			for (int x = 0; x < image.getWidth(); x++)
				count += image.getRGB(x, y) >>> 24 > 0 ? 1 : 0;

		return count;
	}

	private Dimension render(WindowToggle toggle, BufferedImage image)
	{
		Graphics2D g = image.createGraphics();
		try
		{
			return toggle.render(g);
		}
		finally
		{
			g.dispose();
		}
	}

	@Test
	public void theToggleIsADraggableSquare()
	{
		WindowToggle toggle = toggle();
		BufferedImage image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);

		assertEquals(new Dimension(WindowToggle.SIZE, WindowToggle.SIZE), render(toggle, image));
		assertTrue(opaque(image) > WindowToggle.SIZE * WindowToggle.SIZE / 2);
	}

	private static int leftEdge(BufferedImage image, int y)
	{
		for (int x = 0; x < image.getWidth(); x++)
			if (image.getRGB(x, y) >>> 24 >= 120)
				return x;

		return -1;
	}

	@Test
	public void theToggleIsNotSlantedInEitherLookAndTheGlassOneHasABorder()
	{
		for (WindowTheme look : new WindowTheme[] {WindowTheme.RUNELITE_DARK, WindowTheme.RUNELITE_GLASS})
		{
			BufferedImage image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
			render(toggle(look), image);

			int top = leftEdge(image, 8);
			int bottom = leftEdge(image, WindowToggle.SIZE - 9);
			assertTrue(top >= 0);
			assertEquals(top, bottom);
		}

		BufferedImage glass = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
		render(toggle(WindowTheme.RUNELITE_GLASS), glass);
		int edge = leftEdge(glass, WindowToggle.SIZE / 2);
		assertTrue(glass.getRGB(edge, WindowToggle.SIZE / 2) != glass.getRGB(edge + 3, WindowToggle.SIZE / 2));
	}

	@Test
	public void aLockedToggleIsDimmerAndAWaitingOneHasADot()
	{
		WindowToggle plain = toggle();
		BufferedImage normal = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
		render(plain, normal);

		WindowToggle locked = toggle();
		locked.locked(true);
		BufferedImage grey = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
		render(locked, grey);
		assertTrue(locked.isLocked());
		assertTrue(normal.getRGB(22, 22) != grey.getRGB(22, 22));

		WindowToggle waiting = toggle();
		waiting.waiting(true);
		BufferedImage dotted = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
		render(waiting, dotted);
		assertTrue(waiting.isWaiting());
		assertTrue(normal.getRGB(WindowToggle.SIZE - 6, 6) != dotted.getRGB(WindowToggle.SIZE - 6, 6));
	}

	@Test
	public void aLockedToggleWaitingShowsNoDotAndAShakeMovesTheIconForAMoment()
	{
		WindowToggle both = toggle();
		both.locked(true);
		both.waiting(true);
		BufferedImage image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
		render(both, image);
		WindowToggle locked = toggle();
		locked.locked(true);
		BufferedImage same = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
		render(locked, same);
		assertEquals(same.getRGB(WindowToggle.SIZE - 6, 6), image.getRGB(WindowToggle.SIZE - 6, 6));

		both.shake();
		now += 30;
		BufferedImage shaken = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
		render(both, shaken);
		now += 500;
		BufferedImage rested = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
		render(both, rested);
		assertEquals(image.getRGB(10, 22), rested.getRGB(10, 22));
	}

	@Test
	public void aToggleWithNoPictureStillPaints()
	{
		WindowToggle plain = new WindowToggle(() -> WindowTheme.RUNELITE_DARK, () -> MotionMode.OFF,
			() -> now, null);
		BufferedImage image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);

		assertNotNull(render(plain, image));
		assertTrue(opaque(image) > 0);
	}

	@Test
	public void theNoticePaintsWhileItLastsAndThenGoesAway()
	{
		WindowNotice notice = new WindowNotice(() -> motion, () -> WindowTheme.RUNELITE_DARK, () -> now, () -> 765,
			() -> 503);
		BufferedImage empty = new BufferedImage(765, 503, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = empty.createGraphics();
		assertNull(notice.render(g));
		g.dispose();
		assertEquals(0, opaque(empty));

		notice.show(NoticeMessage.CLOSED_COMBAT, 380, 250);
		now += 1_000;
		BufferedImage shown1 = new BufferedImage(765, 503, BufferedImage.TYPE_INT_ARGB);
		g = shown1.createGraphics();
		assertNull(notice.render(g));
		g.dispose();
		assertTrue(opaque(shown1) > 1_000);
		assertEquals(NoticeMessage.CLOSED_COMBAT, notice.current().message());

		now += NoticeClock.totalMs(motion);
		BufferedImage gone = new BufferedImage(765, 503, BufferedImage.TYPE_INT_ARGB);
		g = gone.createGraphics();
		notice.render(g);
		g.dispose();
		assertEquals(0, opaque(gone));
		assertNull(notice.current());
	}

	@Test
	public void aNewerNoticeReplacesTheOlderOne()
	{
		WindowNotice notice = new WindowNotice(() -> motion, () -> WindowTheme.RUNELITE_DARK, () -> now, () -> 765,
			() -> 503);
		notice.show(NoticeMessage.LOADING, 10, 10);
		notice.show(NoticeMessage.BLOCKED_BATTLE, 20, 30);

		assertEquals(NoticeMessage.BLOCKED_BATTLE, notice.current().message());
		assertEquals(20, notice.current().centerX());
		notice.clear();
		assertFalse(notice.current() != null);
	}
}
