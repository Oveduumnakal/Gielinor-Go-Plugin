/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The pinned party box: what its rows hold, when it shows, and that it paints inside its box. */
public class PartyBoxTest
{
	private static List<CreatureView> team(int count)
	{
		List<CreatureView> team = new ArrayList<>();
		for (int i = 0; i < count; i++)
			team.add(new CreatureView("Goblin", 10 + i, 0.5, 1.0 - i * 0.15, Rarity.COMMON, CombatType.CRUSH, false,
				i == 1 ? "Gobbo" : null));

		return team;
	}

	@Test
	public void rowsFollowTheTeamAndCarryNameLevelAndHealth()
	{
		List<RowModel> rows = PartyBoxModel.rows(team(3));

		assertEquals(3, rows.size());
		assertTrue(rows.get(0).lead());
		assertEquals("Goblin", rows.get(0).name());
		assertEquals("Gobbo", rows.get(1).name());
		assertEquals("Lv 12", rows.get(2).levelText());
		assertEquals("70%", rows.get(2).hpText());
	}

	@Test
	public void noTeamAndTooManyCreaturesAreHandled()
	{
		assertTrue(PartyBoxModel.rows(null).isEmpty());
		assertTrue(PartyBoxModel.rows(Collections.emptyList()).isEmpty());
		assertEquals(PartyBoxModel.MAX_ROWS, PartyBoxModel.rows(team(PartyBoxModel.MAX_ROWS + 3)).size());
	}

	@Test
	public void theBoxShowsOnlyWhenEnabledLoadedOutOfBattleAndNotEmpty()
	{
		assertTrue(PartyBoxModel.visible(true, false, true, 1));
		assertFalse(PartyBoxModel.visible(false, false, true, 1));
		assertFalse(PartyBoxModel.visible(true, true, true, 1));
		assertFalse(PartyBoxModel.visible(true, false, false, 1));
		assertFalse(PartyBoxModel.visible(true, false, true, 0));
	}

	@Test
	public void theBoxGrowsWithTheRowsAndStaysAsWide()
	{
		WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_DARK, 1.0);

		Dimension none = PartyBoxPainter.size(style, 0);
		Dimension one = PartyBoxPainter.size(style, 1);
		Dimension two = PartyBoxPainter.size(style, 2);
		Dimension six = PartyBoxPainter.size(style, 6);
		assertTrue(two.height > one.height);
		assertEquals(one.width, six.width);
		assertEquals(1, none.height);
	}

	@Test
	public void everyCountDrawsInsideItsBox()
	{
		PortraitCache pictures = new PortraitCache((name, onReady) ->
		{
		}, () -> 0L);
		StringBuilder problems = new StringBuilder();
		for (WindowTheme theme : WindowTheme.values())
			for (double scale : new double[] {WindowFit.MIN_SCALE, 1.0, 2.0})
				for (int shown = 1; shown <= PartyBoxModel.MAX_ROWS; shown++)
				{
					WindowStyle style = WindowStyle.of(theme, scale);
					Dimension size = PartyBoxPainter.size(style, shown);
					BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = image.createGraphics();
					WindowStyle.prepare(g);
					int[] drawn = {0};
					int count = shown;
					new PartyBoxPainter(pictures, (text, width, height, box) ->
					{
						drawn[0]++;
						if (width > box.width)
							problems.append(theme).append(' ').append(scale).append(' ').append(count).append(": ")
								.append(text).append('\n');
					}).paint(g, style, PartyBoxModel.rows(team(count)), 0L);
					g.dispose();
					assertTrue("something was painted", image.getRGB(size.width / 2, size.height / 2) != 0);
					assertTrue("text was drawn", drawn[0] >= count * 3);
				}

		assertEquals(problems.toString(), "", problems.toString());
	}
}
