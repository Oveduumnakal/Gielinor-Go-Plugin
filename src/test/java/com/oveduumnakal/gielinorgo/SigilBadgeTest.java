/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.EnumMap;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** Checks the sigil badges: which block shows which, where they sit, and that the two badges differ in shape. */
public class SigilBadgeTest
{
	private static final int SIDE = 40;

	private static ChampionView champion(boolean beaten, boolean gold)
	{
		ChampionView view = new ChampionView("c", "Lumbridge", "Goblin 4", "3 jars", beaten, false);
		return gold ? view.withRematch("Goblin 90", true, "") : view;
	}

	private static BufferedImage draw(boolean gold)
	{
		BufferedImage image = new BufferedImage(SIDE, SIDE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		WindowStyle.prepare(g);
		g.setColor(Color.WHITE);
		Icons.sigil(g, gold, 4, 4, SIDE - 8);
		g.dispose();
		return image;
	}

	private static int covered(BufferedImage image)
	{
		int count = 0;
		for (int y = 0; y < SIDE; y++)
		{
			for (int x = 0; x < SIDE; x++)
				count += (image.getRGB(x, y) >>> 24) > 127 ? 1 : 0;
		}

		return count;
	}

	@Test
	public void aChampionCardShowsTheBadgeOfItsSigilAndKeepsItsWords()
	{
		BattleBlock none = BattleModel.champion(champion(false, false));
		BattleBlock plain = BattleModel.champion(champion(true, false));
		BattleBlock gold = BattleModel.champion(champion(true, true));

		assertEquals(BattleBlock.Sigil.NONE, none.sigil());
		assertEquals(BattleBlock.Sigil.NORMAL, plain.sigil());
		assertEquals(BattleBlock.Sigil.GOLD, gold.sigil());
		assertEquals("Sigil", plain.status());
		assertEquals("Gold sigil", gold.status());
	}

	@Test
	public void theChampionsHeadingShowsASmallBadgeAndKeepsTheCountWords()
	{
		PanelState panel = new PanelState(7, 0.2, 4, new EnumMap<>(JarType.class), JarType.BRONZE, 0,
			PartyRig.team(1), Collections.emptyList(), Collections.emptyList(), 0, 0, Collections.emptyList(),
			Collections.singletonList(champion(true, true)), TodayView.NONE, TaskView.NONE,
			new EnumMap<>(ItemType.class));
		BattleBlock heading = null;
		for (BattleBlock block : BattleModel.build(panel, new FriendsState(false, Collections.emptyList())))
		{
			if (block.kind() == BattleBlock.Kind.HEADING && block.text().startsWith("CITY CHAMPIONS"))
				heading = block;
		}


		assertEquals(BattleBlock.Sigil.NORMAL, heading.sigil());
		assertTrue(heading.text().contains("1 / 1 sigils, 1 gold"));
	}

	@Test
	public void theTwoBadgesDifferInShapeAndStayInsideTheirSquare()
	{
		BufferedImage plain = draw(false);
		BufferedImage gold = draw(true);

		assertNotEquals(covered(plain), covered(gold));
		assertTrue("the gold badge is mostly filled", covered(gold) > covered(plain));
		for (int i = 0; i < SIDE; i++)
		{
			assertEquals(0, plain.getRGB(i, 0) >>> 24);
			assertEquals(0, gold.getRGB(0, i) >>> 24);
		}
	}
}
