/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.geom.Point2D;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Checks the bubbles' curve, scale, alpha, stagger, overshoot and pulse with an explicit clock. */
public class BubbleMotionTest
{
	private static final long NOW = 10_000;
	private static final Point2D.Double ORIGIN = new Point2D.Double(100, 200);
	private static final Point2D.Double REST = new Point2D.Double(60, 100);

	private static double max(Tween tween, long from, long to)
	{
		double best = Double.NEGATIVE_INFINITY;
		for (long t = from; t <= to; t++)
		{
			best = Math.max(best, tween.value(t));
		}

		return best;
	}

	@Test
	public void theBezierStartsExactlyAtTheOriginAndEndsExactlyAtTheRest()
	{
		Point2D.Double c = BubbleMotion.control(ORIGIN, REST, -27);

		Point2D.Double start = BubbleMotion.bezier(ORIGIN, c, REST, 0);
		Point2D.Double end = BubbleMotion.bezier(ORIGIN, c, REST, 1);

		assertEquals(100.0, start.x, 0.0);
		assertEquals(200.0, start.y, 0.0);
		assertEquals(60.0, end.x, 0.0);
		assertEquals(100.0, end.y, 0.0);
	}

	@Test
	public void theControlPointIsAboveTheRestAQuarterOfTheWayUpAndBent()
	{
		Point2D.Double c = BubbleMotion.control(ORIGIN, REST, -27);

		assertEquals(33.0, c.x, 1e-9);
		assertEquals(175.0, c.y, 1e-9);
	}

	@Test
	public void theBendsAreLeftForTheJarAndRightForTheOthers()
	{
		assertEquals(-27.0, BubbleMotion.bend(0, 15), 1e-9);
		assertEquals(21.0, BubbleMotion.bend(1, 15), 1e-9);
		assertEquals(27.0, BubbleMotion.bend(2, 15), 1e-9);
	}

	@Test
	public void scaleAndAlphaFollowTheirCurves()
	{
		assertEquals(0.3, BubbleMotion.scale(0, MotionMode.FULL), 1e-9);
		assertEquals(1.0, BubbleMotion.scale(1, MotionMode.FULL), 1e-9);
		assertEquals(0.0, BubbleMotion.alpha(0, MotionMode.FULL), 1e-9);
		assertEquals(0.5, BubbleMotion.alpha(0.2, MotionMode.FULL), 1e-9);
		assertEquals(1.0, BubbleMotion.alpha(0.4, MotionMode.FULL), 1e-9);
		assertEquals(1.0, BubbleMotion.alpha(1.03, MotionMode.FULL), 1e-9);
	}

	@Test
	public void hoverGrowsAndLiftsOnlyWhenThingsMayMove()
	{
		assertEquals(1.15, BubbleMotion.hoverScale(1, MotionMode.FULL), 1e-9);
		assertEquals(3.0, BubbleMotion.hoverLift(1, MotionMode.FULL), 1e-9);
		assertEquals(1.0, BubbleMotion.hoverScale(1, MotionMode.REDUCED), 0.0);
		assertEquals(0.0, BubbleMotion.hoverLift(1, MotionMode.REDUCED), 0.0);
	}

	@Test
	public void theBubblesLandLeftToRightWithAStagger()
	{
		Tween jar = BubbleMotion.appear(0, NOW, 0, MotionMode.FULL);
		Tween battle = BubbleMotion.appear(0, NOW, 1, MotionMode.FULL);
		Tween info = BubbleMotion.appear(0, NOW, 2, MotionMode.FULL);

		assertTrue(jar.value(NOW + 99) > 0);
		assertEquals(0.0, battle.value(NOW + 99), 0.0);
		assertTrue(battle.value(NOW + 100) == 0.0);
		assertTrue(battle.value(NOW + 101) > 0);
		assertEquals(0.0, info.value(NOW + 199), 0.0);
		assertTrue(info.value(NOW + 250) > 0);
		assertEquals(NOW + 500, jar.endMs());
		assertEquals(NOW + 600, battle.endMs());
		assertEquals(NOW + 700, info.endMs());
	}

	@Test
	public void theBubbleOvershootsALittleAndEndsExactlyAtOne()
	{
		Tween jar = BubbleMotion.appear(0, NOW, 0, MotionMode.FULL);

		double peak = max(jar, NOW, NOW + 600);

		assertTrue("peak " + peak, peak > 1.0);
		assertTrue("peak " + peak, peak <= 1.04);
		assertEquals(1.0, jar.value(NOW + 500), 0.0);
		assertEquals(1.0, jar.value(NOW + 5000), 0.0);
	}

	@Test
	public void duringTheOvershootTheBubbleIsAboveItsRestingPlace()
	{
		Tween jar = BubbleMotion.appear(0, NOW, 0, MotionMode.FULL);
		long at = NOW;
		double best = 0;
		for (long t = NOW; t <= NOW + 500; t++)
		{
			if (jar.value(t) > best)
			{
				best = jar.value(t);
				at = t;
			}
		}

		Point2D.Double p = BubbleMotion.position(ORIGIN, REST, -27, jar.value(at), MotionMode.FULL);

		assertTrue("y " + p.y, p.y < REST.y);
	}

	@Test
	public void theBubblesGoBackRightToLeftAndLandAtZero()
	{
		Tween jar = BubbleMotion.retract(1, NOW, 0, MotionMode.FULL);
		Tween info = BubbleMotion.retract(1, NOW, 2, MotionMode.FULL);

		assertTrue(info.value(NOW + 1) < 1.0);
		assertEquals(1.0, jar.value(NOW + 100), 0.0);
		assertTrue(jar.value(NOW + 101) < 1.0);
		assertEquals(0.0, info.value(NOW + 300), 0.0);
		assertEquals(0.0, jar.value(NOW + 400), 0.0);
	}

	@Test
	public void aRetractFromPartWayTakesPartOfTheTime()
	{
		Tween half = BubbleMotion.retract(0.5, NOW, 2, MotionMode.FULL);

		assertEquals(NOW + 150, half.endMs());
	}

	@Test
	public void aReverseFromPartWayStartsFromWhereItIs()
	{
		Tween back = BubbleMotion.appear(0.4, NOW, 0, MotionMode.FULL);

		assertEquals(0.4, back.value(NOW), 1e-9);
		assertEquals(NOW + 300, back.endMs());
	}

	@Test
	public void reducedMotionKeepsTheBubbleAtRestAndFadesLinearly()
	{
		Tween jar = BubbleMotion.appear(0, NOW, 0, MotionMode.REDUCED);
		Tween info = BubbleMotion.appear(0, NOW, 2, MotionMode.REDUCED);

		for (long t = NOW; t <= NOW + 100; t += 10)
		{
			Point2D.Double p = BubbleMotion.position(ORIGIN, REST, -27, jar.value(t), MotionMode.REDUCED);
			assertEquals(60.0, p.x, 0.0);
			assertEquals(100.0, p.y, 0.0);
			assertEquals(1.0, BubbleMotion.scale(jar.value(t), MotionMode.REDUCED), 0.0);
			assertEquals(jar.value(t), BubbleMotion.alpha(jar.value(t), MotionMode.REDUCED), 1e-9);
		}

		assertEquals(NOW + MotionMode.REDUCED_FADE_MS, jar.endMs());
		assertEquals(NOW + MotionMode.REDUCED_FADE_MS, info.endMs());
		assertEquals(0.5, jar.value(NOW + MotionMode.REDUCED_FADE_MS / 2), 1e-9);
	}

	@Test
	public void withMotionOffEveryBubbleIsThereAtOnce()
	{
		for (int i = 0; i < 3; i++)
		{
			Tween appear = BubbleMotion.appear(0, NOW, i, MotionMode.OFF);
			Tween retract = BubbleMotion.retract(1, NOW, i, MotionMode.OFF);

			assertEquals(1.0, appear.value(NOW), 0.0);
			assertEquals(0.0, retract.value(NOW), 0.0);
		}
	}

	@Test
	public void theClusterAndCardTweensFollowTheirTimings()
	{
		assertEquals(NOW + 450 + 120, BubbleMotion.clusterAppear(0, NOW, 2, MotionMode.FULL).endMs());
		assertEquals(NOW + 250 + 60, BubbleMotion.clusterRetract(1, NOW, 2, MotionMode.FULL).endMs());
		assertEquals(NOW + 180, BubbleMotion.cardShow(0, NOW, MotionMode.FULL).endMs());
		assertEquals(NOW + 120, BubbleMotion.cardHide(1, NOW, MotionMode.FULL).endMs());
		assertEquals(0.85, BubbleMotion.cardScale(0, MotionMode.FULL), 1e-9);
		assertEquals(1.0, BubbleMotion.cardScale(1, MotionMode.REDUCED), 0.0);
	}

	@Test
	public void thePulseIsZeroWhenNothingMayLoop()
	{
		assertEquals(0.0, BubbleMotion.pulse(NOW + 300, MotionMode.REDUCED), 0.0);
		assertEquals(0.0, BubbleMotion.pulse(NOW + 300, MotionMode.OFF), 0.0);
	}

	@Test
	public void thePulseRunsFromZeroToOneAndBackEveryPeriod()
	{
		assertEquals(0.0, BubbleMotion.pulse(2400, MotionMode.FULL), 1e-9);
		assertEquals(1.0, BubbleMotion.pulse(2400 + 600, MotionMode.FULL), 1e-9);
		assertEquals(0.0, BubbleMotion.pulse(2400 + 1200, MotionMode.FULL), 1e-9);
	}
}
