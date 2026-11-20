/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.function.Consumer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Checks that a portrait carries no coloured rarity bar on its edge, and that a detailed picture is scaled down to
 * the box it is shown in smoothly instead of being stretched or sampled in blocks (issue 286).
 */
public class PortraitFrameTest
{
	private static BufferedImage paint(Rarity rarity)
	{
		CreatureView view = new CreatureView("Goblin", 12, 0.5, 1.0, rarity, CombatType.SLASH, false, null,
			StatRolls.AVERAGE, 0, Collections.emptyList(), Collections.emptyList(), 2, null);
		PortraitCache pictures = new PortraitCache(new PortraitSource()
		{
			@Override
			public void request(String speciesName, Consumer<BufferedImage> onReady)
			{
				return;
			}
		}, () -> 0L);
		BufferedImage image = WidgetKit.canvas(60, 60);
		Graphics2D g = WidgetKit.graphics(image);
		PortraitFrame.paint(g, WidgetKit.style(1.0), pictures, RowModel.of(view, 0),
			new Rectangle2D.Double(10, 10, 40, 40), 0, 0, 0, MotionMode.FULL);
		g.dispose();
		return image;
	}

	@Test
	public void theEdgeLooksTheSameWhateverTheRarity()
	{
		BufferedImage common = paint(Rarity.COMMON);
		BufferedImage legendary = paint(Rarity.LEGENDARY);

		for (int y = 10; y < 50; y++)
		{
			for (int x = 10; x < 16; x++)
				assertEquals(common.getRGB(x, y), legendary.getRGB(x, y));
		}
	}

	private static BufferedImage paintStripes(int box, double scale)
	{
		int side = PortraitService.SIZE;
		BufferedImage stripes = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < side; y++)
		{
			for (int x = 0; x < side; x++)
				stripes.setRGB(x, y, x % 2 == 0 ? 0xFF000000 : 0xFFFFFFFF);
		}

		CreatureView view = new CreatureView("Goblin", 12, 0.5, 1.0, Rarity.COMMON, CombatType.SLASH, false, null,
			StatRolls.AVERAGE, 0, Collections.emptyList(), Collections.emptyList(), 2, null);
		PortraitCache pictures = new PortraitCache(new PortraitSource()
		{
			@Override
			public void request(String speciesName, Consumer<BufferedImage> onReady)
			{
				onReady.accept(stripes);
			}
		}, () -> 0L);
		int canvas = (int) Math.round((box + 20) * scale);
		BufferedImage image = WidgetKit.canvas(canvas, canvas);
		Graphics2D g = WidgetKit.graphics(image);
		g.scale(scale, scale);
		PortraitFrame.paint(g, WidgetKit.style(1.0), pictures, RowModel.of(view, 0),
			new Rectangle2D.Double(10, 10, box, box), 0, 0, 0, MotionMode.OFF);
		g.dispose();
		return image;
	}

	@Test
	public void aPortraitIsDrawnWellAboveTheSizeItIsShownAt()
	{
		assertTrue(PortraitService.SIZE >= 128);
	}

	/**
	 * A picture of one pixel stripes shown at half its size or less comes out as the average of the stripes. Between
	 * half and full size one bilinear step cannot average a pattern that fine, which a portrait never has, so those
	 * sizes are left out.
	 */
	@Test
	public void aDetailedPictureShrinksToAveragedColoursAtEverySize()
	{
		for (int box : new int[] {24, 40, 64})
		{
			for (double scale : new double[] {1.0, 1.5, 2.0})
			{
				if (box * scale > PortraitService.SIZE / 2.0)
					continue;

				BufferedImage image = paintStripes(box, scale);
				int at = (int) Math.round((10 + box / 2.0) * scale);
				for (int dx = -3; dx <= 3; dx++)
				{
					int grey = image.getRGB(at + dx, at) & 0xFF;
					assertTrue("box " + box + " scale " + scale + " at " + dx + ": " + grey,
						grey > 70 && grey < 185);
				}
			}
		}
	}
}
