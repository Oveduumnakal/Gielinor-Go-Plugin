/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Draws the battle box offline: it fits the panel, updates in place and reports the player's choices. */
public class BattleBoxTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.MELEE);
	private static final int PANEL_WIDTH = 225;

	private static BattleView view(boolean forced, boolean canCatch, List<BattleView.Option> options, int seconds)
	{
		Combatant mine = new Combatant(GOBLIN, "Archibald the Magnificent", 42, 0.6);
		Combatant foe = new Combatant(GOBLIN, "Lesser demon", 17, 0.25);
		return new BattleView(mine, foe, forced, canCatch, JarType.BRONZE, 12, seconds, seconds / 10.0, options);
	}

	private static List<BattleView.Option> twoOptions()
	{
		return Arrays.asList(new BattleView.Option(1, "Imp", 9, 1.0), new BattleView.Option(3, "Chicken", 4, 0.5));
	}

	@Test
	public void theBoxIsHiddenUntilABattleWaits()
	{
		BattleBox box = new BattleBox(choice ->
		{
		});

		assertFalse(box.isVisible());

		box.show(view(false, true, twoOptions(), 10));
		assertTrue(box.isVisible());

		box.show(null);
		assertFalse(box.isVisible());
	}

	@Test
	public void theButtonsReportEachChoice()
	{
		List<BattleChoice> chosen = new ArrayList<>();
		BattleBox box = new BattleBox(chosen::add);
		box.show(view(false, true, twoOptions(), 10));

		box.attackButton().doClick();
		box.catchButton().doClick();
		box.runButton().doClick();

		assertEquals(Arrays.asList(BattleChoice.attack(), BattleChoice.catching(null), BattleChoice.run()), chosen);
	}

	private static BattleView viewWithMoves(Move... moves)
	{
		Combatant mine = new Combatant(GOBLIN, "Bob", 42, 0.6).moves(Arrays.asList(moves));
		Combatant foe = new Combatant(GOBLIN, "Lesser demon", 17, 0.25);
		return new BattleView(mine, foe, false, true, JarType.BRONZE, 12, 10, 1.0, twoOptions());
	}

	@Test
	public void attackOpensTheMovesWhenTheCreatureKnowsMoreThanOne()
	{
		List<BattleChoice> chosen = new ArrayList<>();
		BattleBox box = new BattleBox(chosen::add);
		box.show(viewWithMoves(TestMoves.BITE, TestMoves.DRAGONFIRE));

		box.attackButton().doClick();
		assertTrue("Attack does not choose by itself when there is a choice", chosen.isEmpty());
		assertEquals("Attack...", box.attackButton().getText());

		JPopupMenu menu = box.moveMenu();
		assertEquals(2, menu.getComponentCount());
		assertEquals("Bite \u00b7 Melee, power 1, accuracy 100%", ((JMenuItem) menu.getComponent(0)).getText());
		assertEquals("Dragonfire \u00b7 Magic, power 1.6, accuracy 90%",
			((JMenuItem) menu.getComponent(1)).getText());

		((JMenuItem) menu.getComponent(1)).doClick();

		assertEquals(Collections.singletonList(BattleChoice.attack(1)), chosen);
	}

	@Test
	public void aCreatureWithOneMoveJustAttacks()
	{
		List<BattleChoice> chosen = new ArrayList<>();
		BattleBox box = new BattleBox(chosen::add);
		box.show(viewWithMoves(TestMoves.BITE));

		box.attackButton().doClick();

		assertEquals("Attack", box.attackButton().getText());
		assertEquals(Collections.singletonList(BattleChoice.attack()), chosen);
	}

	@Test
	public void theSwitchListHoldsTheCreaturesThatCanStepIn()
	{
		List<BattleChoice> chosen = new ArrayList<>();
		BattleBox box = new BattleBox(chosen::add);
		box.show(view(false, true, twoOptions(), 10));

		JPopupMenu menu = box.switchMenu();

		assertEquals(2, menu.getComponentCount());
		assertEquals("Imp Lv 9 · 100%", ((JMenuItem) menu.getComponent(0)).getText());
		assertEquals("Chicken Lv 4 · 50%", ((JMenuItem) menu.getComponent(1)).getText());

		((JMenuItem) menu.getComponent(1)).doClick();

		assertEquals(Collections.singletonList(BattleChoice.switchTo(3)), chosen);
	}

	@Test
	public void aForcedSwitchLeavesOnlyTheSwitchButton()
	{
		BattleBox box = new BattleBox(choice ->
		{
		});

		box.show(view(true, false, twoOptions(), 4));

		assertFalse(box.attackButton().isEnabled());
		assertFalse(box.runButton().isEnabled());
		assertFalse(box.catchButton().isEnabled());
		assertTrue(box.switchButton().isEnabled());
	}

	@Test
	public void switchAndCatchAreOffWhenThereIsNothingToStepInOrThrow()
	{
		BattleBox box = new BattleBox(choice ->
		{
		});

		box.show(view(false, false, Collections.emptyList(), 10));

		assertFalse(box.switchButton().isEnabled());
		assertFalse(box.catchButton().isEnabled());
		assertTrue(box.attackButton().isEnabled());
		assertTrue(box.runButton().isEnabled());
		assertEquals(0, box.switchMenu().getComponentCount());
	}

	@Test
	public void theBoxUpdatesInPlace()
	{
		BattleBox box = new BattleBox(choice ->
		{
		});
		box.show(view(false, true, twoOptions(), 10));
		Object[] before = box.getComponents();

		box.show(view(false, true, twoOptions(), 3));

		assertEquals(before.length, box.getComponentCount());
		for (int i = 0; i < before.length; i++)
			assertTrue(before[i] == box.getComponent(i));
	}

	@Test
	public void theBoxFitsThePanelWidthAndDraws()
	{
		BattleBox box = new BattleBox(choice ->
		{
		});
		box.show(view(false, true, twoOptions(), 10));
		box.setSize(new Dimension(PANEL_WIDTH - 16, 400));
		box.doLayout();
		box.setSize(new Dimension(PANEL_WIDTH - 16, box.getPreferredSize().height));
		box.doLayout();
		BufferedImage image = new BufferedImage(PANEL_WIDTH - 16, box.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		box.printAll(g);
		g.dispose();

		assertTrue("fits " + box.getPreferredSize(), box.getPreferredSize().width <= PANEL_WIDTH - 16);
		assertTrue(box.getPreferredSize().height > 100);
	}

	@Test
	public void theTitleCountsTheSecondsOrAsksForTheNextCreature()
	{
		BattleBox box = new BattleBox(choice ->
		{
		});
		box.show(view(false, true, twoOptions(), 7));
		String first = firstLabel(box);

		box.show(view(true, false, twoOptions(), 7));
		String second = firstLabel(box);

		assertEquals("YOUR TURN · 7s", first);
		assertEquals("CHOOSE YOUR NEXT CREATURE", second);
	}

	private static String firstLabel(BattleBox box)
	{
		return ((javax.swing.JLabel) box.getComponent(0)).getText();
	}
}
