/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Tests the 3D models of the jars. */
public class JarShapesTest
{
	private static int faces(JarType type, boolean thrown)
	{
		List<JarModel.Face> faces = JarShapes.build(type, thrown).faces();
		return faces.size();
	}

	@Test
	public void theEmberJarHasTheApprovedNumberOfFaces()
	{
		assertEquals(360, faces(JarType.EMBER, true));
		assertEquals(216, faces(JarType.EMBER, false));
	}

	@Test
	public void everyJarBuildsWithinTheApprovedSizes()
	{
		for (JarType type : JarType.values())
		{
			int thrown = faces(type, true);
			int icon = faces(type, false);
			assertTrue(type + " thrown " + thrown, thrown >= 280 && thrown <= 444);
			assertTrue(type + " icon " + icon, icon >= 213 && icon <= 248);
		}
	}

	@Test
	public void onlyAThrownJarHasContents()
	{
		for (JarType type : JarType.values())
		{
			JarModel icon = JarShapes.build(type, false);
			assertTrue(type.toString(), icon.faces()
					.stream()
					.noneMatch(f -> f.layer() == JarModel.Layer.CONTENT || f.layer() == JarModel.Layer.LIQUID));
			assertTrue(type.toString(), JarShapes.build(type, true)
					.faces()
					.stream()
					.anyMatch(f -> f.layer() == JarModel.Layer.CONTENT));
		}
	}

	@Test
	public void tierJarsHaveABandAndSpecialtyJarsAnEmblem()
	{
		for (JarType type : JarType.values())
		{
			JarModel jar = JarShapes.build(type, true);
			boolean band = jar.faces()
					.stream()
					.anyMatch(f -> f.layer() == JarModel.Layer.BAND);
			boolean emblem = jar.faces()
					.stream()
					.anyMatch(f -> f.layer() == JarModel.Layer.EMBLEM);
			assertEquals(type.toString(), !type.specialty(), band);
			assertEquals(type.toString(), type.specialty(), emblem);
		}
	}

	@Test
	public void theContentsAreInTheSamePlacesEveryTime()
	{
		List<JarModel.Face> first = JarShapes.build(JarType.CAVE, true).faces();
		List<JarModel.Face> second = JarShapes.build(JarType.CAVE, true).faces();
		for (int i = 0; i < first.size(); i++)
		{
			double[][] a = first.get(i).points();
			double[][] b = second.get(i).points();
			for (int corner = 0; corner < a.length; corner++)
			{
				for (int k = 0; k < 3; k++)
					assertEquals(a[corner][k], b[corner][k], 0.0);
			}
		}
	}

	@Test
	public void onlyVenomAndTideHoldLiquid()
	{
		Set<JarType> liquid = new HashSet<>();
		for (JarType type : JarType.values())
		{
			if (JarShapes.build(type, true).liquid())
				liquid.add(type);
		}

		assertEquals(2, liquid.size());
		assertTrue(liquid.contains(JarType.VENOM));
		assertTrue(liquid.contains(JarType.TIDE));
	}

	@Test
	public void theJarIsAsTallAsItsStatedHeight()
	{
		double low = Double.MAX_VALUE;
		double high = -Double.MAX_VALUE;
		for (JarModel.Face face : JarShapes.build(JarType.IRON, true).faces())
		{
			for (double[] point : face.points())
			{
				low = Math.min(low, point[1]);
				high = Math.max(high, point[1]);
			}
		}

		assertEquals(JarShapes.HEIGHT, high - low, 0.001);
	}
}
