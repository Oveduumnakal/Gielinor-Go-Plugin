/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks the motion kit: easing, tweens, stagger, smoothed values, modes, the animation table and the shell. */
public class MotionTest
{
	@Test
	public void everyEasingEndsAtExactlyZeroAndOne()
	{
		for (Easing easing : Easing.values())
		{
			assertEquals(easing + " at 0", 0.0, easing.apply(0.0), 0.0);
			assertEquals(easing + " at 1", 1.0, easing.apply(1.0), 0.0);
			assertEquals(easing + " below 0", 0.0, easing.apply(-4.0), 0.0);
			assertEquals(easing + " above 1", 1.0, easing.apply(7.0), 0.0);
		}
	}

	@Test
	public void easingsHaveTheirShape()
	{
		assertEquals(0.5, Easing.LINEAR.apply(0.5), 1e-12);
		assertTrue(Easing.OUT_CUBIC.apply(0.5) > 0.5);
		assertTrue(Easing.IN_CUBIC.apply(0.5) < 0.5);
		assertEquals(0.5, Easing.IN_OUT_SINE.apply(0.5), 1e-12);
		assertTrue("out-back overshoots", Easing.OUT_BACK.apply(0.7) > 1.0);
	}

	@Test
	public void outCubicNeverGoesPastOne()
	{
		double last = 0.0;
		for (int i = 0; i <= 1000; i++)
		{
			double v = Easing.OUT_CUBIC.apply(i / 1000.0);
			assertTrue(v <= 1.0 && v >= last);
			last = v;
		}
	}

	@Test
	public void aTweenIsAtItsStartMiddleAndEnd()
	{
		Tween tween = Tween.of(10, 30, 1_000, 200, Easing.LINEAR);
		assertEquals(10.0, tween.value(900), 0.0);
		assertEquals(10.0, tween.value(1_000), 0.0);
		assertEquals(20.0, tween.value(1_100), 1e-9);
		assertEquals(30.0, tween.value(1_200), 0.0);
		assertEquals(30.0, tween.value(99_000), 0.0);
		assertFalse(tween.done(1_199));
		assertTrue(tween.done(1_200));
		assertEquals(1_200, tween.endMs());
		assertEquals(10.0, tween.from(), 0.0);
		assertEquals(30.0, tween.to(), 0.0);
	}

	@Test
	public void aDelayHoldsTheStartValue()
	{
		Tween tween = new Tween(0, 100, 0, 50, 100, Easing.LINEAR);
		assertEquals(0.0, tween.value(49), 0.0);
		assertEquals(0.0, tween.value(50), 0.0);
		assertEquals(50.0, tween.value(100), 1e-9);
		assertEquals(100.0, tween.value(150), 0.0);
		assertFalse(tween.done(10));
		assertEquals(150, tween.endMs());
	}

	@Test
	public void aZeroLengthTweenJumpsAfterItsDelay()
	{
		Tween tween = new Tween(1, 9, 100, 20, 0, Easing.OUT_CUBIC);
		assertEquals(1.0, tween.value(119), 0.0);
		assertEquals(9.0, tween.value(120), 0.0);
		assertTrue(tween.done(120));
	}

	@Test
	public void staggerSpacesNeighboursByTheStep()
	{
		assertEquals(0, Tween.stagger(0, 50));
		assertEquals(150, Tween.stagger(3, 50));
		assertEquals(0, Tween.stagger(-2, 50));

		long start = 1_000;
		long step = WindowTimes.ROW_STAGGER;
		Tween first = new Tween(-40, 0, start, Tween.stagger(0, step), WindowTimes.ROW_IN, Easing.OUT_CUBIC);
		Tween third = new Tween(-40, 0, start, Tween.stagger(2, step), WindowTimes.ROW_IN, Easing.OUT_CUBIC);
		assertTrue(first.value(start + 90) > third.value(start + 90));
		assertEquals(-40.0, third.value(start + 100), 0.0);
		assertEquals(0.0, third.value(start + 100 + WindowTimes.ROW_IN), 0.0);
	}

	@Test
	public void smoothedReachesItsTargetAndNeverOvershoots()
	{
		Smoothed hp = new Smoothed(1.0, 300);
		hp.set(0.4, 1_000, MotionMode.FULL);
		double last = 1.0;
		for (long t = 1_000; t <= 1_400; t += 7)
		{
			double v = hp.value(t);
			assertTrue("monotone down " + v, v <= last);
			assertTrue("never under the target " + v, v >= 0.4);
			last = v;
		}

		assertEquals(0.4, hp.value(1_300), 0.0);
		assertTrue(hp.settled(1_300));
		assertFalse(hp.settled(1_100));
		assertEquals(1.0, hp.value(1_000), 0.0);
	}

	@Test
	public void smoothedRetargetsFromWhereItIsAndStaysBetweenOldAndNewTargets()
	{
		Smoothed bar = new Smoothed(0.0, 400);
		bar.set(1.0, 0, MotionMode.FULL);
		double midway = bar.value(100);
		assertTrue(midway > 0.0 && midway < 1.0);

		bar.set(0.5, 100, MotionMode.FULL);
		assertEquals(midway, bar.value(100), 1e-12);
		for (long t = 100; t <= 600; t += 11)
		{
			double v = bar.value(t);
			assertTrue("stays within the two ends " + v, v >= 0.5 && v <= Math.max(midway, 0.5));
		}

		assertEquals(0.5, bar.value(500), 0.0);
		bar.set(0.5, 700, MotionMode.FULL);
		assertTrue(bar.settled(700));
	}

	@Test
	public void smoothedJumpsInOffAndGlidesInReduced()
	{
		Smoothed off = new Smoothed(1.0, 300);
		off.set(0.2, 50, MotionMode.OFF);
		assertEquals(0.2, off.value(50), 0.0);
		assertTrue(off.settled(50));

		Smoothed reduced = new Smoothed(1.0, 300);
		reduced.set(0.2, 50, MotionMode.REDUCED);
		assertTrue(reduced.value(150) > 0.2);
		assertEquals(0.2, reduced.value(350), 0.0);

		Smoothed snap = new Smoothed(0.0, 300);
		snap.snap(0.7);
		assertEquals(0.7, snap.value(0), 0.0);
		assertEquals(0.7, snap.target(), 0.0);
	}

	@Test
	public void modesDecideWhatMoves()
	{
		assertTrue(MotionMode.FULL.moves() && MotionMode.FULL.loops() && MotionMode.FULL.fades());
		assertFalse(MotionMode.REDUCED.moves() || MotionMode.REDUCED.loops());
		assertTrue(MotionMode.REDUCED.fades() && MotionMode.REDUCED.smoothBars());
		assertFalse(MotionMode.OFF.moves() || MotionMode.OFF.loops() || MotionMode.OFF.fades());
		assertFalse(MotionMode.OFF.smoothBars());

		assertEquals(200, MotionMode.FULL.moveMs(200));
		assertEquals(0, MotionMode.REDUCED.moveMs(200));
		assertEquals(0, MotionMode.OFF.moveMs(200));
		assertEquals(150, MotionMode.FULL.fadeMs(150));
		assertEquals(80, MotionMode.REDUCED.fadeMs(150));
		assertEquals(60, MotionMode.REDUCED.fadeMs(60));
		assertEquals(0, MotionMode.OFF.fadeMs(150));
		assertEquals(300, MotionMode.REDUCED.barMs(300));
		assertEquals(0, MotionMode.OFF.barMs(300));
		assertEquals("Full", MotionMode.FULL.toString());
		assertEquals("Reduced", MotionMode.REDUCED.toString());
		assertEquals("Off", MotionMode.OFF.toString());
	}

	@Test
	public void loopsAreStillOutsideFullMotion()
	{
		assertEquals(0.0, Loop.wave(0, 1_000, 0), 1e-12);
		assertEquals(1.0, Loop.wave(250, 1_000, 0), 1e-12);
		assertEquals(-1.0, Loop.wave(750, 1_000, 0), 1e-12);
		assertEquals(0.0, Loop.pulse(0, 1_000, 0), 1e-12);
		assertEquals(1.0, Loop.pulse(500, 1_000, 0), 1e-12);
		assertEquals(Loop.wave(0, 1_000, 250), Loop.wave(250, 1_000, 0), 1e-12);
		assertEquals(0.25, Loop.phase(-750, 1_000, 0), 1e-12);
		assertEquals(0.0, Loop.phase(10, 0, 0), 0.0);
		assertTrue(Loop.blink(100, 1_000));
		assertFalse(Loop.blink(600, 1_000));

		assertEquals(1.0, Loop.wave(MotionMode.FULL, 250, 1_000, 0), 1e-12);
		assertEquals(0.0, Loop.wave(MotionMode.REDUCED, 250, 1_000, 0), 0.0);
		assertEquals(0.0, Loop.pulse(MotionMode.OFF, 500, 1_000, 0), 0.0);
		assertEquals(1.0, Loop.pulse(MotionMode.FULL, 500, 1_000, 0), 1e-12);
	}

	@Test
	public void slidingRowsRefuseClicksUntilTheyLand()
	{
		Animations table = new Animations();
		assertTrue(table.slide("row:1", MotionMode.FULL, 0, 64, 1_000));
		assertTrue(table.blocksClicks("row:1", 1_000));
		assertTrue(table.blocksClicks("row:1", 1_199));
		assertFalse("lands in at most 200 ms", table.blocksClicks("row:1", 1_200));
		assertFalse(table.blocksClicks("row:2", 1_000));
		assertTrue(table.active("row:1", 1_100));
		assertFalse(table.active("row:1", 1_200));
	}

	@Test
	public void entrancesDoNotBlockClicksAndHitTestsUseTheFinalLayout()
	{
		Animations table = new Animations();
		assertTrue(table.enter("row:3", MotionMode.FULL, -50, 0, 1_000, 3));
		assertFalse(table.blocksClicks("row:3", 1_000));
		assertTrue(table.active("row:3", 1_000));
		assertEquals("still waiting out its stagger", -50.0, table.value("row:3", 1_100, 0), 0.0);
		assertEquals("hit rectangles use the final place", 0.0, table.finalValue("row:3", 7), 0.0);
		assertEquals(7.0, table.finalValue("row:9", 7), 0.0);

		table.slide("row:4", MotionMode.FULL, 0, 64, 1_000);
		assertEquals("a sliding row is hit where it lands", 64.0, table.finalValue("row:4", 0), 0.0);
		assertTrue("but it is painted partway", table.value("row:4", 1_050, 64) < 64.0);
	}

	@Test
	public void reducedAndOffHaveNoSlidingAndNoWaiting()
	{
		for (MotionMode mode : new MotionMode[] {MotionMode.REDUCED, MotionMode.OFF})
		{
			Animations table = new Animations();
			assertFalse(table.slide("row:1", mode, 0, 64, 0));
			assertFalse(table.enter("row:2", mode, -50, 0, 0, 2));
			assertFalse(table.blocksClicks("row:1", 0));
			assertFalse(table.anyActive(0));
			assertEquals(64.0, table.value("row:1", 0, 64), 0.0);
			assertEquals(0, table.size());
		}
	}

	@Test
	public void theTablePrunesLandedAnimationsAndClears()
	{
		Animations table = new Animations();
		table.start("a", Tween.of(0, 1, 0, 100, Easing.LINEAR), false);
		table.start("b", Tween.of(0, 1, 0, 300, Easing.LINEAR), false);
		assertTrue(table.anyActive(50));
		assertEquals(2, table.size());

		table.prune(150);
		assertEquals(1, table.size());
		assertEquals(0.5, table.value("b", 150, 0), 1e-12);
		assertEquals("a landed tween paints the resting value", 9.0, table.value("a", 150, 9), 0.0);

		assertFalse(table.anyActive(300));
		table.clear();
		assertEquals(0, table.size());
	}

	@Test
	public void theShellOpensWithScaleAndFadeInFullMotion()
	{
		ShellMotion.Frame start = ShellMotion.frame(MotionMode.FULL, ShellMotion.Phase.OPEN, 0);
		assertEquals(0.94, start.scale, 1e-12);
		assertEquals(0.0, start.alpha, 0.0);
		assertFalse(start.done);

		ShellMotion.Frame mid = ShellMotion.frame(MotionMode.FULL, ShellMotion.Phase.OPEN, 75);
		assertTrue(mid.scale > 0.94 && mid.scale < 1.0);
		assertTrue(mid.alpha > 0.0 && mid.alpha < 1.0);

		ShellMotion.Frame end = ShellMotion.frame(MotionMode.FULL, ShellMotion.Phase.OPEN, 150);
		assertEquals(1.0, end.scale, 0.0);
		assertEquals(1.0, end.alpha, 0.0);
		assertTrue(end.done);
		assertEquals(150, ShellMotion.durationMs(MotionMode.FULL, ShellMotion.Phase.OPEN));
	}

	@Test
	public void theShellClosesFasterAndACombatCloseOnlyFades()
	{
		assertEquals(100, ShellMotion.durationMs(MotionMode.FULL, ShellMotion.Phase.CLOSE));
		assertEquals(120, ShellMotion.durationMs(MotionMode.FULL, ShellMotion.Phase.COMBAT_CLOSE));

		ShellMotion.Frame closing = ShellMotion.frame(MotionMode.FULL, ShellMotion.Phase.CLOSE, 50);
		assertTrue(closing.scale < 1.0 && closing.scale > 0.94);
		assertTrue(closing.alpha < 1.0 && closing.alpha > 0.0);

		ShellMotion.Frame combat = ShellMotion.frame(MotionMode.FULL, ShellMotion.Phase.COMBAT_CLOSE, 60);
		assertEquals("no scale for a combat close", 1.0, combat.scale, 0.0);
		assertEquals(0.5, combat.alpha, 1e-12);

		ShellMotion.Frame gone = ShellMotion.frame(MotionMode.FULL, ShellMotion.Phase.CLOSE, 100);
		assertEquals(0.0, gone.alpha, 0.0);
		assertTrue(gone.done);
	}

	@Test
	public void theShellReducedOnlyFadesAndOffIsInstant()
	{
		for (ShellMotion.Phase phase : ShellMotion.Phase.values())
		{
			assertEquals(80, ShellMotion.durationMs(MotionMode.REDUCED, phase));
			ShellMotion.Frame half = ShellMotion.frame(MotionMode.REDUCED, phase, 40);
			assertEquals("reduced never scales", 1.0, half.scale, 0.0);
			assertEquals(0, ShellMotion.durationMs(MotionMode.OFF, phase));
		}

		assertEquals(0.5, ShellMotion.frame(MotionMode.REDUCED, ShellMotion.Phase.CLOSE, 40).alpha, 1e-12);

		ShellMotion.Frame open = ShellMotion.frame(MotionMode.OFF, ShellMotion.Phase.OPEN, 0);
		assertTrue(open.done);
		assertEquals(1.0, open.alpha, 0.0);
		ShellMotion.Frame close = ShellMotion.frame(MotionMode.OFF, ShellMotion.Phase.CLOSE, 0);
		assertTrue(close.done);
		assertEquals(0.0, close.alpha, 0.0);
	}

	@Test
	public void theLockedShakeIsShortSidewaysAndOnlyInFullMotion()
	{
		assertEquals(0.0, ShellMotion.shake(MotionMode.FULL, 0, 4), 1e-12);
		boolean moved = false;
		for (long t = 1; t < 200; t += 5)
		{
			double offset = ShellMotion.shake(MotionMode.FULL, t, 4);
			assertTrue(Math.abs(offset) <= 4.0);
			moved |= Math.abs(offset) > 0.5;
		}

		assertTrue(moved);
		assertEquals(0.0, ShellMotion.shake(MotionMode.FULL, 200, 4), 0.0);
		assertEquals(0.0, ShellMotion.shake(MotionMode.FULL, -5, 4), 0.0);
		assertEquals(0.0, ShellMotion.shake(MotionMode.REDUCED, 50, 4), 0.0);
		assertEquals(0.0, ShellMotion.shake(MotionMode.OFF, 50, 4), 0.0);
	}
}
