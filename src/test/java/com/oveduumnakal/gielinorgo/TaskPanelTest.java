/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Draws the Tasks section offline and checks that rows are updated where they stand. */
public class TaskPanelTest
{
	private static TaskView view(int firstProgress, boolean firstDone, int streak)
	{
		return new TaskView(Arrays.asList(
			new TaskView.Row("Catch 3 Ranged creatures", firstProgress, 3, firstDone, "2 Iron jars", false),
			new TaskView.Row("Gain 5,000 XP in any skill", 4321, 5000, false, "100 trainer XP", false),
			new TaskView.Row("Catch a creature underground", 0, 1, false, "2 Iron jars", false),
			new TaskView.Row("Walk 5,000 tiles", 1200, 5000, false, "5 Iron jars + Rare egg", true)),
			streak, 3, 5L * 3_600_000L + 12L * 60_000L, 3L * 86_400_000L);
	}

	@Test
	public void theSectionIsHiddenWithoutTasksAndShownWithThem()
	{
		TasksSection section = new TasksSection();

		section.update(TaskView.NONE);
		assertFalse(section.isVisible());

		section.update(view(2, false, 4));
		assertTrue(section.isVisible());
	}

	@Test
	public void rowsKeepTheirComponentsWhenTheProgressMoves()
	{
		TasksSection section = new TasksSection();
		section.update(view(1, false, 4));
		Object[] before = section.getComponents();

		section.update(view(3, true, 5));

		assertEquals(before.length, section.getComponentCount());
		for (int i = 0; i < before.length; i++)
			assertTrue(before[i] == section.getComponent(i));
	}

	@Test
	public void theSectionFitsTheNarrowPanelAndDraws()
	{
		TasksSection section = new TasksSection();
		section.update(view(2, false, 4));
		section.setSize(new Dimension(209, 400));
		section.doLayout();
		section.setSize(new Dimension(209, section.getPreferredSize().height));
		section.doLayout();
		BufferedImage image = new BufferedImage(209, section.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		section.printAll(g);
		g.dispose();

		assertTrue(section.getPreferredSize().width <= 209);
		assertTrue(section.getPreferredSize().height > 100);
	}
}
