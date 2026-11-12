/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JLabel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Draws the Items line of the panel header offline and checks that it fits the 225 pixel panel. */
public class ItemsRowTest
{
	@Test
	public void theItemsLineFitsTheNarrowPanelAndRendersForALook() throws IOException
	{
		Map<ItemType, Integer> owned = new EnumMap<>(ItemType.class);
		owned.put(ItemType.SMALL_SALVE, 12);
		owned.put(ItemType.REVIVE, 1);
		ItemsRow row = new ItemsRow();
		row.update(owned);
		row.setSize(new Dimension(209, 20));
		row.doLayout();

		assertTrue(row.getPreferredSize().width <= 209);
		for (Component cell : row.getComponents())
		{
			assertTrue(cell.getPreferredSize().width <= cell.getWidth());
			assertNotNull(((JLabel) cell).getToolTipText());
		}

		BufferedImage image = new BufferedImage(209, 20, BufferedImage.TYPE_INT_ARGB);
		row.paint(image.getGraphics());
		File out = new File("build/items-row.png");
		out.getParentFile().mkdirs();
		ImageIO.write(image, "png", out);
	}

	@Test
	public void everyItemHasATooltipThatNamesItsEffect()
	{
		assertTrue(ItemsRow.tooltip(ItemType.SMALL_SALVE, 2).contains("Restores 25% health"));
		assertTrue(ItemsRow.tooltip(ItemType.LARGE_SALVE, 0).contains("Restores 60% health"));
		assertTrue(ItemsRow.tooltip(ItemType.REVIVE, 1).contains("50% health"));
		assertTrue(ItemsRow.tooltip(ItemType.ANTIDOTE, 1).contains("Cures poison, venom and burn"));
		assertEquals(4, ItemType.values().length);
	}
}
