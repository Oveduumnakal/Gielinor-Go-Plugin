/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Before;
import org.junit.Test;

/**
 * Holds the glass look to recorded pictures, as {@link SolidLookTest} does for the solid look: every primitive and the
 * Info card painted with {@link WindowTheme#RUNELITE_GLASS} at scales 1.0 and 1.5 over a fixed scene, tinted and
 * see-through with no scene wash, as the game paints it. Same tolerance. Record with {@code SOLID_LOOK_RECORD=1};
 * the pictures live in {@code src/test/resources/.../glass-look/}.
 */
public class GlassLookTest
{
	private static final String FOLDER = "glass-look";
	private static final double[] SCALES = {1.0, 1.5};

	/** Soft gradients and shadows round one or two levels apart on another machine; that is not a change of look. */
	private static final int NOISE = 2;

	/** Starts every test from empty caches, so the pictures do not depend on what other tests painted first. */
	@Before
	public void emptyTheCaches()
	{
		GlassPanel.clear();
		GlassStyle.clearSprites();
		SoftShadows.clear();
	}

	@Test
	public void everyPrimitiveLooksAsRecorded()
	{
		for (double scale : SCALES)
			SolidLookTest.check(FOLDER, "primitives-" + scale, LookSheet.primitives(WindowTheme.RUNELITE_GLASS, scale),
				NOISE);
	}

	@Test
	public void theWildBubblesLookAsRecorded()
	{
		SolidLookTest.check(FOLDER, "bubbles-1.0", LookSheet.bubbles(WindowTheme.RUNELITE_GLASS, 1.0), NOISE);
	}

	@Test
	public void theInfoCardLooksAsRecorded()
	{
		for (double scale : SCALES)
			SolidLookTest.check(FOLDER, "card-" + scale, LookSheet.card(WindowTheme.RUNELITE_GLASS, scale), NOISE);
	}
}
