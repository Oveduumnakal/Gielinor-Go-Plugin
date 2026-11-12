/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Draws the Battle tab with the Elite card offline, at the panel's width. */
public class ElitePanelTest
{
	private static EliteView view(EliteView.Mode mode)
	{
		return new EliteView(mode, 8, mode == EliteView.Mode.RUNNING ? 3 : 0, 5, "Elite of Might", "Sarachnis 81",
			"2 Dragon jars", true);
	}

	@Test
	public void theCardTextNamesWhatToDoInEachMode()
	{
		assertEquals("Earn all 8 sigils · 5 / 8", EliteCard.describe(new EliteView(EliteView.Mode.LOCKED, 5, 0,
			5, null, null, "x", false)));
		assertEquals("Go to the Champions' Guild", EliteCard.describe(view(EliteView.Mode.AWAY)));
		assertEquals("Elite of Might: Sarachnis 81", EliteCard.describe(view(EliteView.Mode.RUNNING)));
	}

	@Test
	public void theTabShowsTheCardOnlyWhenTheChallengeExists()
	{
		ChampionsTab tab = new ChampionsTab(null, trade ->
		{
		});
		tab.setSize(new Dimension(225, 700));
		tab.updateElite(EliteView.NONE);
		tab.update(Collections.emptyList());
		tab.doLayout();
		assertFalse(hasCard(tab));

		tab.updateElite(view(EliteView.Mode.RUNNING));
		tab.doLayout();
		BufferedImage image = new BufferedImage(225, 700, BufferedImage.TYPE_INT_ARGB);
		tab.printAll(image.getGraphics());

		assertTrue(hasCard(tab));
	}

	private static boolean hasCard(Container parent)
	{
		for (Component child : parent.getComponents())
		{
			if (child instanceof EliteCard || child instanceof Container && hasCard((Container) child))
				return true;
		}

		return false;
	}
}
