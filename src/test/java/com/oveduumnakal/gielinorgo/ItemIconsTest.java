/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the four rendered item pictures: one each, at the size asked, with a clear margin, all different. */
public class ItemIconsTest
{
	private static final int SIDE = 128;

	@Test
	public void everyItemHasAPictureWithAnAlphaChannelAndAClearMargin()
	{
		for (ItemType type : ItemType.values())
		{
			BufferedImage image = read(type);

			assertEquals(type.name(), SIDE, image.getWidth());
			assertEquals(type.name(), SIDE, image.getHeight());
			assertTrue(type.name(), image.getColorModel().hasAlpha());
			for (int i = 0; i < SIDE; i++)
			{
				assertEquals(type.name(), 0, image.getRGB(i, 0) >>> 24);
				assertEquals(type.name(), 0, image.getRGB(i, SIDE - 1) >>> 24);
				assertEquals(type.name(), 0, image.getRGB(0, i) >>> 24);
				assertEquals(type.name(), 0, image.getRGB(SIDE - 1, i) >>> 24);
			}
		}
	}

	@Test
	public void theFourPicturesDiffer()
	{
		Set<String> seen = new HashSet<>();
		for (ItemType type : ItemType.values())
			assertTrue(type.name() + " matches another item", seen.add(pixels(read(type))));

		assertEquals(4, seen.size());
	}

	@Test
	public void aPictureIsRenderedAtTheSizeAskedForAndKept()
	{
		for (ItemType type : ItemType.values())
		{
			for (int side : new int[] {16, 24, 33, 50})
			{
				BufferedImage image = ItemIcons.source(type, side);
				assertNotNull(type.name(), image);
				assertEquals(side, image.getWidth());
				assertTrue(image == ItemIcons.source(type, side));
			}
		}

		assertNull(ItemIcons.source(null, 16));
	}

	@Test
	public void theFourDifferAtTheSmallestSizeDrawn()
	{
		Set<String> seen = new HashSet<>();
		for (ItemType type : ItemType.values())
			assertTrue(type.name() + " matches another item", seen.add(pixels(ItemIcons.source(type, 16))));
	}

	@Test
	public void theSmallSalveIsShorterThanTheLargeOneAndStandsOnTheSameLine()
	{
		int[] small = rows(ItemIcons.source(ItemType.SMALL_SALVE, SIDE));
		int[] large = rows(ItemIcons.source(ItemType.LARGE_SALVE, SIDE));
		assertTrue(small[1] - small[0] < large[1] - large[0]);
		assertEquals(large[1], small[1], 2);
	}

	/** @return the first and last row with anything solid in it */
	private static int[] rows(BufferedImage image)
	{
		int[] rows = {image.getHeight(), -1};
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				if (image.getRGB(x, y) >>> 24 < 128)
					continue;

				rows[0] = Math.min(rows[0], y);
				rows[1] = Math.max(rows[1], y);
			}
		}

		return rows;
	}

	private static BufferedImage read(ItemType type)
	{
		BufferedImage image = ItemIcons.source(type, SIDE);
		assertNotNull(type.name() + " has no picture", image);
		return image;
	}

	private static String pixels(BufferedImage image)
	{
		StringBuilder out = new StringBuilder();
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
				out.append(Integer.toHexString(image.getRGB(x, y))).append(',');
		}

		return out.toString();
	}
}
