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

/** Checks that a portrait carries no coloured rarity bar on its edge. */
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
			{
				assertEquals(common.getRGB(x, y), legendary.getRGB(x, y));
			}
		}
	}
}
