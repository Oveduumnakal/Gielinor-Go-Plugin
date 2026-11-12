/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the four bundled item pictures: one each, in the agreed size, with a clear margin, all different. */
public class ItemIconsTest
{
	private static final int SIDE = 32;

	@Test
	public void everyItemHasA32PixelPictureWithAnAlphaChannelAndAClearMargin() throws IOException
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
	public void theFourPicturesDiffer() throws IOException
	{
		Set<String> seen = new HashSet<>();
		for (ItemType type : ItemType.values())
			assertTrue(type.name() + " matches another item", seen.add(pixels(read(type))));

		assertEquals(4, seen.size());
	}

	@Test
	public void theShownPictureIs16PixelsAndAMissingItemGivesNull()
	{
		for (ItemType type : ItemType.values())
		{
			assertNotNull(type.name(), ItemIcons.image(type));
			assertEquals(ItemIcons.ICON_SIZE, ItemIcons.image(type).getWidth());
			assertEquals(ItemIcons.ICON_SIZE, ItemIcons.image(type).getHeight());
			assertNotNull(type.name(), ItemIcons.swing(type));
			assertNotNull(type.name(), ItemIcons.swingGrey(type));
		}

		assertNull(ItemIcons.image(null));
		assertNull(ItemIcons.swing(null));
		assertNull(ItemIcons.swingGrey(null));
	}

	private static BufferedImage read(ItemType type) throws IOException
	{
		try (InputStream in = ItemIcons.class.getResourceAsStream(ItemIcons.resourceName(type)))
		{
			assertNotNull(type.name() + " has no file", in);
			return ImageIO.read(in);
		}
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
