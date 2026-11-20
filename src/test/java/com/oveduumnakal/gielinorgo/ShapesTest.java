/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Tests the soft-tipped slanted outlines of the glass look. */
public class ShapesTest
{
	private static double area(Shape shape)
	{
		Area area = new Area(shape);
		PathIterator it = area.getPathIterator(null, 0.05);
		double[] c = new double[6];
		double sum = 0;
		double fx = 0;
		double fy = 0;
		double px = 0;
		double py = 0;
		while (!it.isDone())
		{
			int type = it.currentSegment(c);
			if (type == PathIterator.SEG_MOVETO)
			{
				fx = c[0];
				fy = c[1];
				px = c[0];
				py = c[1];
			}
			else if (type == PathIterator.SEG_LINETO)
			{
				sum += px * c[1] - c[0] * py;
				px = c[0];
				py = c[1];
			}
			else if (type == PathIterator.SEG_CLOSE)
			{
				sum += px * fy - fx * py;
			}

			it.next();
		}

		return Math.abs(sum) / 2;
	}

	@Test
	public void aRoundedSlantStaysInsideThePlainSlant()
	{
		double[][] boxes = {{0, 0, 84, 32, 9}, {5, 7, 100, 44, 7}, {0, 0, 300, 120, 14}, {0, 0, 180, 12, 6},
			{3, 3, 60, 26, 9}};
		for (double[] b : boxes)
		{
			Rectangle2D box = new Rectangle2D.Double(b[0], b[1], b[2], b[3]);
			Shape plain = Slant.shape(box, b[4]);
			Shape rounded = Shapes.roundedSlant(box, b[4], b[3] * GlassTokens.TIP_ROUND);
			Area outside = new Area(rounded);
			outside.subtract(new Area(plain));
			assertTrue("outside " + area(outside), area(outside) < 0.5);
			assertTrue(area(rounded) < area(plain));
		}
	}

	@Test
	public void theRoundedAreaIsWithinThreePercentOfThePlainAreaAtFifteenPercent()
	{
		double[][] boxes = {{0, 0, 84, 32, 9}, {0, 0, 100, 44, 7}, {0, 0, 300, 120, 14}, {0, 0, 60, 26, 9}};
		for (double[] b : boxes)
		{
			Rectangle2D box = new Rectangle2D.Double(b[0], b[1], b[2], b[3]);
			double plain = area(Slant.shape(box, b[4]));
			double rounded = area(Shapes.roundedSlant(box, b[4], b[3] * GlassTokens.TIP_ROUND));
			assertTrue("ratio " + rounded / plain, rounded / plain >= 0.97);
		}
	}

	@Test
	public void hitTestsStillUseThePlainSlant()
	{
		Rectangle2D box = new Rectangle2D.Double(10, 10, 84, 32);
		assertTrue(Slant.contains(50, 26, box, 9));
		assertFalse(Slant.contains(11, 10, box, 9));
		Path2D rounded = Shapes.roundedSlant(box, 9, 4.8);
		assertTrue(rounded.contains(50, 26));
		assertFalse(rounded.contains(10.5, 10.5));
	}

	@Test
	public void aRoundedFilledBarStaysInsideTheSharpOne()
	{
		Rectangle2D box = new Rectangle2D.Double(0, 0, 180, 12);
		for (double f : new double[] {0.1, 0.5, 1.0})
		{
			Area outside = new Area(Shapes.roundedFilled(box, 6, f, 1.8));
			outside.subtract(new Area(Slant.filled(box, 6, f)));
			assertTrue("fraction " + f + " " + area(outside), area(outside) < 0.5);
		}
	}

	@Test
	public void theFormKeyIgnoresWhereTheShapeIsAndSeesWhatItLooksLike()
	{
		Shape a = Shapes.roundedSlant(new Rectangle2D.Double(3, 4, 84, 32), 9, 4.8);
		Shape b = Shapes.roundedSlant(new Rectangle2D.Double(50.25, 90, 84, 32), 9, 4.8);
		Shape c = Shapes.roundedSlant(new Rectangle2D.Double(3, 4, 85, 32), 9, 4.8);
		assertArrayEquals(Shapes.formKey(a), Shapes.formKey(b));
		assertFalse(Arrays.equals(Shapes.formKey(a), Shapes.formKey(c)));
	}
}
