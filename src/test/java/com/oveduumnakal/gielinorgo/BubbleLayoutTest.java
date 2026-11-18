/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** Checks the resting arc, the edge clamp, the jar cluster, the card and the fallback reach shape. */
public class BubbleLayoutTest
{
	private static final Rectangle VIEW = new Rectangle(0, 0, 800, 600);
	private static final List<Rectangle> NO_BLOCKERS = Collections.emptyList();

	private static BubbleAnchors at(double headX, double headY)
	{
		return new BubbleAnchors(headX, headY + 60, headX, headY);
	}

	private static BubbleLayout.ClusterPlan cluster(BubbleLayout.Arc arc, int n, int remembered, Rectangle view,
		List<Rectangle> blockers)
	{
		List<Point2D.Double> mains = new ArrayList<>();
		for (int i = 0; i < 3; i++)
			mains.add(arc.rest(i));

		return BubbleLayout.cluster(arc.x(0), arc.y(0), arc.radius(), n, remembered, mains, arc.radius() * 1.15,
			view, blockers);
	}

	private static Rectangle around(Point2D.Double point)
	{
		return new Rectangle((int) point.x - 2, (int) point.y - 2, 4, 4);
	}

	private static void assertInside(Rectangle2D box, Rectangle view)
	{
		Rectangle2D inset = BubbleLayout.inset(view);
		assertTrue(box + " in " + inset, inset.contains(box.getX() + 1e-6, box.getY() + 1e-6)
			&& inset.contains(box.getMaxX() - 1e-6, box.getMaxY() - 1e-6));
	}

	@Test
	public void theMiddleBubbleIsHighestAndTheGroupIsSymmetricAboutTheHead()
	{
		BubbleLayout.Arc arc = BubbleLayout.main(at(400, 300), VIEW);

		assertTrue(arc.y(1) < arc.y(0));
		assertEquals(arc.y(0), arc.y(2), 1e-9);
		assertEquals(400.0, arc.x(1), 1e-9);
		assertEquals(400.0 - arc.x(0), arc.x(2) - 400.0, 1e-9);
		assertEquals(80 * Math.sin(Math.toRadians(28)), 400.0 - arc.x(0), 1e-9);
		assertEquals(80 * (1 - Math.cos(Math.toRadians(28))), arc.y(0) - arc.y(1), 1e-9);
	}

	@Test
	public void theBubblesSitAboveTheTagLineWithClearance()
	{
		BubbleLayout.Arc arc = BubbleLayout.main(at(400, 300), VIEW);

		assertEquals(300 - 18 - 15 - 80 * (1 - Math.cos(Math.toRadians(28))), arc.y(1), 1e-9);
	}

	@Test
	public void neighbouringBubblesAreAtLeastTwoRadiiAndSixApart()
	{
		BubbleLayout.Arc arc = BubbleLayout.main(at(400, 300), VIEW);

		for (int i = 0; i < 3; i++)
		{
			for (int j = i + 1; j < 3; j++)
			{
				double d = Math.hypot(arc.x(i) - arc.x(j), arc.y(i) - arc.y(j));
				assertTrue("distance " + d, d >= 2 * 15 + 6);
			}
		}
	}

	@Test
	public void aHeadNearAnEdgeOrCornerKeepsTheGroupInsideWithItsShapeUnchanged()
	{
		BubbleLayout.Arc free = BubbleLayout.main(at(400, 300), VIEW);
		double[][] heads = {{2, 300}, {798, 300}, {400, 5}, {400, 598}, {1, 1}, {799, 1}, {1, 599}, {799, 599}};
		for (double[] head : heads)
		{
			BubbleLayout.Arc arc = BubbleLayout.main(at(head[0], head[1]), VIEW);

			assertInside(arc.bounds(), VIEW);
			for (int i = 1; i < 3; i++)
			{
				assertEquals(free.x(i) - free.x(0), arc.x(i) - arc.x(0), 1e-9);
				assertEquals(free.y(i) - free.y(0), arc.y(i) - arc.y(0), 1e-9);
			}
		}
	}

	@Test
	public void theArcIsNotMovedWhenItAlreadyFits()
	{
		BubbleAnchors anchors = at(400, 300);

		BubbleLayout.Arc arc = BubbleLayout.main(anchors, VIEW);

		assertEquals(400.0, arc.x(1), 1e-9);
	}

	@Test
	public void theCorridorRunsFromTheTopOfTheArcDownToTheCreature()
	{
		BubbleAnchors anchors = at(400, 300);
		BubbleLayout.Arc arc = BubbleLayout.main(anchors, VIEW);

		Rectangle2D corridor = BubbleLayout.corridor(arc, anchors.originY());

		assertEquals(arc.bounds().getY(), corridor.getY(), 1e-9);
		assertEquals(anchors.originY(), corridor.getMaxY(), 1e-9);
		assertTrue(corridor.contains(400, 330));
	}

	@Test
	public void theClusterOffsetsAreUniqueAndSpacedAtLeastTwoRadii()
	{
		for (int n = 1; n <= 17; n++)
		{
			List<Point2D.Double> slots = BubbleLayout.clusterOffsets(n);

			assertEquals(n, slots.size());
			for (int i = 0; i < n; i++)
			{
				for (int j = i + 1; j < n; j++)
					assertTrue("n=" + n + " " + i + "," + j, slots.get(i).distance(slots.get(j)) >= 2 * 12 - 1e-9);
			}
		}
	}

	@Test
	public void theFirstRingStartsAtTheTopAndGoesClockwise()
	{
		List<Point2D.Double> slots = BubbleLayout.clusterOffsets(7);

		assertEquals(0.0, slots.get(0).x, 0.0);
		assertEquals(0.0, slots.get(1).x, 1e-9);
		assertEquals(-28.0, slots.get(1).y, 1e-9);
		assertTrue(slots.get(2).x > 0);
	}

	@Test
	public void theClusterExtentGrowsWithTheRings()
	{
		assertEquals(12.0, BubbleLayout.clusterExtent(1), 0.0);
		assertEquals(40.0, BubbleLayout.clusterExtent(7), 0.0);
		assertEquals(68.0, BubbleLayout.clusterExtent(8), 0.0);
		assertEquals(68.0, BubbleLayout.clusterExtent(17), 0.0);
	}

	@Test
	public void theClusterFitsInsideTheViewportAndClearsTheMainBubbles()
	{
		BubbleLayout.Arc arc = BubbleLayout.main(at(400, 300), VIEW);
		for (int n = 1; n <= 17; n++)
		{
			BubbleLayout.ClusterPlan plan = cluster(arc, n, -1, VIEW, NO_BLOCKERS);

			assertEquals(n, plan.slots().size());
			assertEquals(0, plan.direction());
			assertInside(plan.bounds(), VIEW);
			for (Point2D.Double slot : plan.slots())
			{
				for (int i = 0; i < 3; i++)
					assertTrue(Math.hypot(slot.x - arc.x(i), slot.y - arc.y(i)) >= 12 + 15 + 2);
			}
		}
	}

	@Test
	public void theClusterSlotsNeverOverlapEachOther()
	{
		BubbleLayout.Arc arc = BubbleLayout.main(at(400, 300), VIEW);

		BubbleLayout.ClusterPlan plan = cluster(arc, 17, -1, VIEW, NO_BLOCKERS);

		for (int i = 0; i < 17; i++)
		{
			for (int j = i + 1; j < 17; j++)
			{
				Point2D.Double a = plan.slots().get(i);
				Point2D.Double b = plan.slots().get(j);
				assertTrue(a.distance(b) >= 24 - 1e-9);
			}
		}
	}

	@Test
	public void theClusterStaysInsideTheViewportAtEveryEdgeAndCorner()
	{
		double[][] heads = {{20, 300}, {780, 300}, {400, 100}, {400, 580}, {20, 40}, {780, 40}, {20, 580}, {780, 580}};
		for (double[] head : heads)
		{
			BubbleLayout.Arc arc = BubbleLayout.main(at(head[0], head[1]), VIEW);

			BubbleLayout.ClusterPlan plan = cluster(arc, 17, -1, VIEW, NO_BLOCKERS);

			assertInside(plan.bounds(), VIEW);
		}
	}

	@Test
	public void whereADirectionFitsNoJarOverlapsAMainBubble()
	{
		double[][] heads = {{780, 300}, {400, 580}, {780, 580}};
		for (double[] head : heads)
		{
			BubbleLayout.Arc arc = BubbleLayout.main(at(head[0], head[1]), VIEW);

			BubbleLayout.ClusterPlan plan = cluster(arc, 17, -1, VIEW, NO_BLOCKERS);

			if (plan.direction() < 0)
				continue;

			for (Point2D.Double slot : plan.slots())
			{
				for (int i = 0; i < 3; i++)
					assertTrue(Math.hypot(slot.x - arc.x(i), slot.y - arc.y(i)) >= 12 + 15 + 2);
			}
		}
	}

	@Test
	public void theClusterTakesTheNextDirectionWhenTheFirstIsBlocked()
	{
		BubbleLayout.Arc arc = BubbleLayout.main(at(400, 300), VIEW);
		BubbleLayout.ClusterPlan first = cluster(arc, 5, -1, VIEW, NO_BLOCKERS);
		List<Rectangle> blockers = Collections.singletonList(around(first.slots().get(0)));

		BubbleLayout.ClusterPlan second = cluster(arc, 5, -1, VIEW, blockers);

		assertEquals(0, first.direction());
		assertEquals(1, second.direction());
	}

	@Test
	public void theRememberedDirectionIsKeptWhileItStillFits()
	{
		BubbleLayout.Arc arc = BubbleLayout.main(at(400, 300), VIEW);

		BubbleLayout.ClusterPlan kept = cluster(arc, 5, 3, VIEW, NO_BLOCKERS);

		assertEquals(3, kept.direction());
	}

	@Test
	public void theRememberedDirectionIsDroppedWhenItNoLongerFits()
	{
		BubbleLayout.Arc arc = BubbleLayout.main(at(400, 300), VIEW);
		BubbleLayout.ClusterPlan up = cluster(arc, 5, 3, VIEW, NO_BLOCKERS);
		List<Rectangle> blockers = Collections.singletonList(around(up.slots().get(0)));

		BubbleLayout.ClusterPlan plan = cluster(arc, 5, 3, VIEW, blockers);

		assertNotEquals(3, plan.direction());
	}

	@Test
	public void theCardIsOnTheRightNormally()
	{
		BubbleLayout.Arc arc = BubbleLayout.main(at(400, 300), VIEW);

		BubbleLayout.CardPlan plan = BubbleLayout.card(arc.bounds(), 190, 140, null, VIEW, NO_BLOCKERS);

		assertEquals(BubbleFrame.Side.RIGHT, plan.side());
		assertEquals(arc.bounds().getMaxX() + 8, plan.bounds().x, 1.0);
		assertInside(plan.bounds(), VIEW);
	}

	@Test
	public void theCardIsOnTheLeftNearTheRightEdge()
	{
		BubbleLayout.Arc arc = BubbleLayout.main(at(700, 300), VIEW);

		BubbleLayout.CardPlan plan = BubbleLayout.card(arc.bounds(), 190, 140, null, VIEW, NO_BLOCKERS);

		assertEquals(BubbleFrame.Side.LEFT, plan.side());
		assertInside(plan.bounds(), VIEW);
	}

	@Test
	public void theCardIsAboveWhenBothSidesAreBlocked()
	{
		BubbleLayout.Arc arc = BubbleLayout.main(at(400, 300), VIEW);
		Rectangle2D g = arc.bounds();
		List<Rectangle> blockers = new ArrayList<>();
		blockers.add(new Rectangle((int) g.getMaxX() + 5, (int) g.getY(), 300, (int) g.getHeight()));
		blockers.add(new Rectangle(0, (int) g.getY(), (int) g.getX() - 5, (int) g.getHeight()));

		BubbleLayout.CardPlan plan = BubbleLayout.card(g, 190, 140, null, VIEW, blockers);

		assertEquals(BubbleFrame.Side.ABOVE, plan.side());
		assertInside(plan.bounds(), VIEW);
	}

	@Test
	public void theCardIsAlwaysInsideTheViewportEvenWhenNothingFits()
	{
		BubbleLayout.Arc arc = BubbleLayout.main(at(400, 300), VIEW);
		List<Rectangle> blockers = Collections.singletonList(new Rectangle(0, 0, 800, 600));

		BubbleLayout.CardPlan plan = BubbleLayout.card(arc.bounds(), 190, 140, null, VIEW, blockers);

		assertEquals(BubbleFrame.Side.RIGHT, plan.side());
		assertInside(plan.bounds(), VIEW);
	}

	@Test
	public void theCardKeepsItsSideWhileItStillFits()
	{
		BubbleLayout.Arc arc = BubbleLayout.main(at(400, 300), VIEW);

		BubbleLayout.CardPlan plan = BubbleLayout.card(arc.bounds(), 190, 140, BubbleFrame.Side.LEFT, VIEW,
			NO_BLOCKERS);

		assertEquals(BubbleFrame.Side.LEFT, plan.side());
	}

	@Test
	public void theCardIsPushedIntoTheViewportVertically()
	{
		BubbleLayout.Arc arc = BubbleLayout.main(at(400, 40), VIEW);

		BubbleLayout.CardPlan plan = BubbleLayout.card(arc.bounds(), 190, 140, null, VIEW, NO_BLOCKERS);

		assertEquals(BubbleFrame.Side.RIGHT, plan.side());
		assertInside(plan.bounds(), VIEW);
	}

	@Test
	public void theFallbackReachContainsTheCreatureAndNothingBeyondItsWidth()
	{
		Ellipse2D.Double reach = BubbleLayout.reachFallback(300, 200, 1, 2, 40);

		double rx = (2 + 0.5) * 40;
		assertTrue(reach.contains(300, 200));
		assertTrue(reach.contains(300 + rx - 1, 200));
		assertFalse(reach.contains(300 + rx + 1, 200));
		assertFalse(reach.contains(300 - rx - 1, 200));
		assertFalse(reach.contains(300, 200 + rx * 0.6 + 1));
	}

	@Test
	public void theRanksCountSlotsFurtherLeft()
	{
		List<Point2D.Double> slots = new ArrayList<>();
		slots.add(new Point2D.Double(50, 0));
		slots.add(new Point2D.Double(10, 0));
		slots.add(new Point2D.Double(90, 0));

		int[] ranks = BubbleLayout.xRanks(slots);

		assertEquals(1, ranks[0]);
		assertEquals(0, ranks[1]);
		assertEquals(2, ranks[2]);
	}
}
