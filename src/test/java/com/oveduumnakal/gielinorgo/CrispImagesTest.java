/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Checks that pictures are scaled to the real pixel size and not stretched on the canvas (issue 266). */
public class CrispImagesTest
{
	private static BufferedImage halves(int side)
	{
		BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setColor(Color.RED);
		g.fillRect(0, 0, side / 2, side);
		g.setColor(Color.BLUE);
		g.fillRect(side / 2, 0, side / 2, side);
		g.dispose();
		return image;
	}

	@Test
	public void aPictureIsScaledToTheExactSideAndKept()
	{
		BufferedImage source = halves(128);

		BufferedImage small = CrispImages.fitted(source, 20);

		assertEquals(20, small.getWidth());
		assertEquals(20, small.getHeight());
		assertSame(small, CrispImages.fitted(source, 20));
		assertSame(source, CrispImages.fitted(source, 128));
	}

	@Test
	public void aShrunkEdgeStaysAsSharpAsTheSourceEdge()
	{
		BufferedImage small = CrispImages.fitted(halves(128), 16);

		assertEquals(Color.RED.getRGB(), small.getRGB(6, 8));
		assertEquals(Color.BLUE.getRGB(), small.getRGB(9, 8));
	}

	@Test
	public void theDeviceScaleIsTheCanvasScaleAndNeverBelowOne()
	{
		BufferedImage canvas = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = canvas.createGraphics();
		assertEquals(1.0, CrispImages.deviceScale(g), 1e-9);
		g.scale(2.0, 2.0);
		assertEquals(2.0, CrispImages.deviceScale(g), 1e-9);
		g.dispose();
		Graphics2D small = canvas.createGraphics();
		small.scale(0.5, 0.5);
		assertEquals(1.0, CrispImages.deviceScale(small), 1e-9);
		small.dispose();
	}

	@Test
	public void aDoubledCanvasDrawsTheEdgeOnAWholeDevicePixel()
	{
		BufferedImage canvas = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = canvas.createGraphics();
		g.scale(2.0, 2.0);
		CrispImages.draw(g, halves(128), 4, 4, 16);
		g.dispose();

		assertEquals(Color.RED.getRGB(), canvas.getRGB(15, 20));
		assertEquals(Color.BLUE.getRGB(), canvas.getRGB(24, 20));
		assertEquals(0, canvas.getRGB(7, 20));
		assertEquals(0, canvas.getRGB(41, 20));
	}

	@Test
	public void everyJarAndItemDrawsSomething()
	{
		for (JarType type : JarType.values())
		{
			BufferedImage canvas = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = canvas.createGraphics();
			assertTrue(type.name(), JarIcons.draw(g, type, 4, 4, 16));
			g.dispose();
			assertTrue(type.name(), painted(canvas));
		}

		for (ItemType type : ItemType.values())
		{
			BufferedImage canvas = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = canvas.createGraphics();
			assertTrue(type.name(), ItemIcons.draw(g, type, 4, 4, 16));
			g.dispose();
			assertTrue(type.name(), painted(canvas));
		}

		BufferedImage canvas = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = canvas.createGraphics();
		assertEquals(false, JarIcons.draw(g, null, 0, 0, 16));
		g.dispose();
		assertNotNull(canvas);
	}

	private static boolean painted(BufferedImage image)
	{
		for (int x = 0; x < image.getWidth(); x++)
		{
			for (int y = 0; y < image.getHeight(); y++)
			{
				if ((image.getRGB(x, y) >>> 24) != 0)
					return true;
			}
		}

		return false;
	}
}
