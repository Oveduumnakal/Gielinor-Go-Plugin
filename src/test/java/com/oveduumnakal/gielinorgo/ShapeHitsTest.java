/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Polygon;
import java.awt.Shape;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks the point-in-shapes test used for the click on a stand-in. */
public class ShapeHitsTest
{
	private static final Polygon TALL = new Polygon(new int[]{0, 10, 10, 0}, new int[]{0, 0, 40, 40}, 4);
	private static final Polygon FLAT = new Polygon(new int[]{20, 60, 60, 20}, new int[]{30, 30, 40, 40}, 4);

	@Test
	public void aPointInEitherShapeHits()
	{
		assertTrue(ShapeHits.anyContains(Arrays.asList(TALL, FLAT), 5, 20));
		assertTrue(ShapeHits.anyContains(Arrays.asList(TALL, FLAT), 50, 35));
	}

	@Test
	public void aPointOutsideAllShapesMisses()
	{
		assertFalse(ShapeHits.anyContains(Arrays.asList(TALL, FLAT), 50, 10));
		assertFalse(ShapeHits.anyContains(Collections.<Shape>emptyList(), 5, 20));
	}

	@Test
	public void aMissingShapeIsSkipped()
	{
		assertTrue(ShapeHits.anyContains(Arrays.asList(null, FLAT), 30, 35));
		assertFalse(ShapeHits.anyContains(Collections.singletonList(null), 30, 35));
	}
}
