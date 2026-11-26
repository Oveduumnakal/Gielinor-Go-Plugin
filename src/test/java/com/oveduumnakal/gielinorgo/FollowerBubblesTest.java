/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Tests the model and painting of the bubbles over the follower. */
public class FollowerBubblesTest
{
	private static final double[] HEAD = {400, 300};
	private static final double[] AWAY = {20, 20};
	private static final double[] BODY = {400, 320};
	private static final Rectangle VIEW = new Rectangle(0, 0, 800, 600);

	private static final FollowerOffer.Member BOB = new FollowerOffer.Member(1, "Bob", "goblin", 0, 12, 0.5);
	private static final FollowerOffer.Member EVE = new FollowerOffer.Member(2, "Eve", "chicken", 0, 7, 1.0);

	private static FollowerOffer everything()
	{
		return new FollowerOffer.Builder()
				.battle(true)
				.switching(Arrays.asList(BOB, EVE))
				.items(Arrays.asList(
						new FollowerOffer.Item(ItemType.SMALL_SALVE, 3, Arrays.asList(BOB, EVE)),
						new FollowerOffer.Item(ItemType.ANTIDOTE, 1, Collections.emptyList())))
				.running(0.7)
				.build();
	}

	private static FollowerBubbles opened(FollowerOffer offer)
	{
		FollowerBubbles bubbles = new FollowerBubbles();
		bubbles.tick(0, offer, HEAD, true, BODY, VIEW);
		bubbles.tick(FollowerBubbles.OPEN_MS, offer, HEAD, true, BODY, VIEW);
		return bubbles;
	}

	private static FollowerBubbles.Dot dot(FollowerBubbles bubbles, FollowerBubbles.Kind kind)
	{
		return bubbles.dots()
				.stream()
				.filter(d -> d.kind() == kind)
				.findFirst()
				.orElse(null);
	}

	private static FollowerBubbles.Action press(FollowerBubbles bubbles, FollowerBubbles.Kind kind, int button)
	{
		FollowerBubbles.Dot at = dot(bubbles, kind);
		return bubbles.press(at.x(), at.y(), button);
	}

	@Test
	public void theBubblesComeOutOnlyAfterThePointerRestsOnTheFollower()
	{
		FollowerBubbles bubbles = new FollowerBubbles();
		bubbles.tick(0, everything(), HEAD, true, BODY, VIEW);
		assertFalse(bubbles.open());
		bubbles.tick(FollowerBubbles.OPEN_MS - 1, everything(), HEAD, true, BODY, VIEW);
		assertFalse(bubbles.open());
		bubbles.tick(FollowerBubbles.OPEN_MS, everything(), HEAD, true, BODY, VIEW);
		assertTrue(bubbles.open());
		assertEquals(3, bubbles.dots().size());
	}

	@Test
	public void theyGoBackAMomentAfterThePointerLeaves()
	{
		FollowerBubbles bubbles = opened(everything());
		bubbles.tick(1000, everything(), HEAD, false, AWAY, VIEW);
		assertTrue(bubbles.open());
		bubbles.tick(1000 + FollowerBubbles.CLOSE_MS, everything(), HEAD, false, AWAY, VIEW);
		assertFalse(bubbles.open());
		assertTrue(bubbles.dots().isEmpty());
	}

	@Test
	public void theyHoldStillWhileThePointerIsOnOne()
	{
		FollowerBubbles bubbles = opened(everything());
		FollowerBubbles.Dot run = dot(bubbles, FollowerBubbles.Kind.RUN);
		double[] on = {run.x(), run.y()};

		bubbles.tick(1000, everything(), new double[] {500, 300}, false, on, VIEW);

		assertEquals(run.x(), dot(bubbles, FollowerBubbles.Kind.RUN).x(), 0.0);
	}

	@Test
	public void switchComesFirstOnTheLeftAndRunLast()
	{
		FollowerBubbles bubbles = opened(everything());
		double swap = dot(bubbles, FollowerBubbles.Kind.SWITCH).x();
		double item = dot(bubbles, FollowerBubbles.Kind.ITEM).x();
		double run = dot(bubbles, FollowerBubbles.Kind.RUN).x();
		assertTrue(swap < item);
		assertTrue(item < run);
	}

	@Test
	public void aLeftClickInBattleSwitchesToTheNextCreatureAndRunRuns()
	{
		FollowerBubbles bubbles = opened(everything());

		FollowerBubbles.Action swap = press(bubbles, FollowerBubbles.Kind.SWITCH, FollowerBubbles.LEFT);
		assertEquals(FollowerBubbles.Action.Type.SWITCH, swap.type());
		assertEquals(1, swap.index());

		assertEquals(FollowerBubbles.Action.Type.RUN,
				press(bubbles, FollowerBubbles.Kind.RUN, FollowerBubbles.LEFT).type());
	}

	@Test
	public void outOfBattleALeftClickOnSwitchOpensTheList()
	{
		FollowerOffer offer = new FollowerOffer.Builder()
				.switching(Arrays.asList(BOB, EVE))
				.build();
		FollowerBubbles bubbles = opened(offer);

		assertEquals(FollowerBubbles.Action.Type.NONE,
				press(bubbles, FollowerBubbles.Kind.SWITCH, FollowerBubbles.LEFT).type());
		assertEquals(3, bubbles.dots().size());
		FollowerBubbles.Action chosen = press(bubbles, FollowerBubbles.Kind.MEMBER, FollowerBubbles.LEFT);
		assertEquals(FollowerBubbles.Action.Type.SWITCH, chosen.type());
	}

	@Test
	public void anItemThatHelpsSeveralAsksWhich()
	{
		FollowerBubbles bubbles = opened(everything());
		assertEquals("a click only opens the bag", FollowerBubbles.Action.Type.NONE,
				press(bubbles, FollowerBubbles.Kind.ITEM, FollowerBubbles.LEFT).type());
		assertEquals(5, bubbles.dots().size());

		assertEquals(FollowerBubbles.Action.Type.NONE,
				press(bubbles, FollowerBubbles.Kind.POTION, FollowerBubbles.LEFT).type());
		FollowerBubbles.Action used = press(bubbles, FollowerBubbles.Kind.MEMBER, FollowerBubbles.LEFT);

		assertEquals(FollowerBubbles.Action.Type.ITEM, used.type());
		assertEquals(ItemType.SMALL_SALVE, used.item());
		assertEquals(1, used.index());
	}

	@Test
	public void restingOnABubbleOpensItsCluster()
	{
		FollowerBubbles bubbles = opened(everything());
		FollowerBubbles.Dot swap = dot(bubbles, FollowerBubbles.Kind.SWITCH);
		double[] on = {swap.x(), swap.y()};

		bubbles.tick(1000, everything(), HEAD, false, on, VIEW);
		bubbles.tick(1000 + FollowerBubbles.CLUSTER_MS, everything(), HEAD, false, on, VIEW);

		assertEquals(5, bubbles.dots().size());
	}

	@Test
	public void nothingIsAskedWhileNoChoiceIsOpen()
	{
		FollowerOffer waiting = new FollowerOffer.Builder()
				.battle(false)
				.switching(Arrays.asList(BOB))
				.running(0.5)
				.build();
		FollowerBubbles bubbles = opened(waiting);

		for (FollowerBubbles.Dot dot : bubbles.dots())
		{
			assertFalse(dot.enabled());
			assertEquals(FollowerBubbles.Action.Type.NONE,
					bubbles.press(dot.x(), dot.y(), FollowerBubbles.LEFT).type());
			assertTrue(bubbles.tip(dot).contains("wait for your turn"));
		}
	}

	@Test
	public void theWholeGroupStaysInsideTheViewport()
	{
		FollowerBubbles bubbles = new FollowerBubbles();
		double[] corner = {5, 20};
		bubbles.tick(0, everything(), corner, true, corner, VIEW);
		bubbles.tick(FollowerBubbles.OPEN_MS, everything(), corner, true, corner, VIEW);

		for (FollowerBubbles.Dot dot : bubbles.dots())
		{
			assertTrue(dot.x() - dot.radius() >= 0);
			assertTrue(dot.y() - dot.radius() >= 0);
		}
	}

	@Test
	public void theBubblesPaintInBothLooks()
	{
		FollowerBubbles bubbles = opened(everything());
		press(bubbles, FollowerBubbles.Kind.ITEM, FollowerBubbles.RIGHT);
		List<FollowerBubbles.Dot> dots = bubbles.dots();
		for (WindowTheme theme : new WindowTheme[] {WindowTheme.RUNELITE_DARK, WindowTheme.RUNELITE_GLASS})
		{
			BufferedImage canvas = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = canvas.createGraphics();
			new FollowerBubblePainter(null).paint(g, dots, dots.get(0), WindowStyle.of(theme, 1.0));
			g.dispose();
			for (FollowerBubbles.Dot dot : dots)
				assertTrue(canvas.getRGB((int) dot.x(), (int) dot.y()) >>> 24 != 0);
		}
	}
}
