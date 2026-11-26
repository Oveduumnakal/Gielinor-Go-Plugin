/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.image.BufferedImage;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Paints the whole window on the Summary page with biome backgrounds on, as the plugin wires them. */
public class SummarySceneWindowTest
{
	private static boolean[] on = {true};

	private static PartyRig onSummary(BiomePlayer player)
	{
		PartyRig rig = new PartyRig();
		BiomeTable biomes = BiomeTable.bundled(new Gson());
		rig.pages.scenery(new SummaryScene(biomes, player, () -> on[0]));
		rig.publish(PartyRig.state(PartyRig.team(4)), true);
		rig.open();
		rig.click("row:0");
		return rig;
	}

	private static long differing(BufferedImage a, BufferedImage b)
	{
		long count = 0;
		for (int y = 0; y < a.getHeight(); y++)
		{
			for (int x = 0; x < a.getWidth(); x++)
				count += a.getRGB(x, y) == b.getRGB(x, y) ? 0 : 1;
		}

		return count;
	}

	@Test
	public void theSummaryShowsTheSceneAndTurningItOffBringsTheOldPageBack()
	{
		on[0] = true;
		BiomePlayer player = BiomePlayer.bundled(Runnable::run, new Gson());
		PartyRig rig = onSummary(player);
		for (int i = 0; i < 40; i++)
		{
			rig.now += 50;
			rig.paint();
		}

		BufferedImage with = rig.paint();
		assertTrue(player.holding());

		on[0] = false;
		BufferedImage without = rig.paint();

		assertTrue("the scene changes a large part of the page", differing(with, without) > 2_000);
	}

	@Test
	public void nothingIsReadWhileTheSettingIsOff()
	{
		on[0] = false;
		BiomePlayer player = BiomePlayer.bundled(Runnable::run, new Gson());
		PartyRig rig = onSummary(player);
		rig.paint();

		assertFalse(player.holding());
	}
}
