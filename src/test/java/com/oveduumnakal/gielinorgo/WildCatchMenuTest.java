/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks the painted wild creature menu: what it offers, the list of jars beside it, and its pointer and keys. */
public class WildCatchMenuTest
{
	private static final WindowStyle STYLE = WidgetKit.style(1.0);
	private static final FontMetrics FM = WidgetKit.metrics(STYLE, WindowFonts.Face.REGULAR);
	private static final Rectangle CANVAS = new Rectangle(0, 0, 800, 600);
	private static final int LEFT = MouseEvent.BUTTON1;

	private final List<String> ran = new ArrayList<>();
	private final WildCatchMenu menu = new WildCatchMenu();

	private static JarSuggest.JarChoice choice(JarType jar, int owned, double chance, boolean matching)
	{
		return new JarSuggest.JarChoice(jar, owned, chance, matching);
	}

	private static List<JarSuggest.JarChoice> threeJars()
	{
		return Arrays.asList(choice(JarType.BRONZE, 5, 0.38, false), choice(JarType.IRON, 2, 0.56, false),
			choice(JarType.DRAGON, 3, 0.95, false), choice(JarType.SLASH, 4, 0.94, true));
	}

	private void open(JarType standard, List<JarSuggest.JarChoice> owned, boolean withBattle, boolean catches,
		int x, int y)
	{
		menu.open(7, "Chicken (Lv 3 - New)", WildMenu.layout(standard, owned, withBattle, catches, true), owned,
			() -> ran.add("battle"), jar -> ran.add("throw " + jar), x, y, CANVAS, STYLE, FM, 0);
	}

	private void hoverOpener()
	{
		Rectangle box = menu.rootMenu().itemBounds(menu.openerIndex());
		menu.onMoved(box.x + 4, box.y + 2);
	}

	private ContextMenu.Item rootItem(int index)
	{
		List<ContextMenu.Item> items = menu.rootMenu().items();
		return items.get(index);
	}

	private ContextMenu.Item listItem(int index)
	{
		List<ContextMenu.Item> items = menu.listMenu().items();
		return items.get(index);
	}

	private List<String> labels(ContextMenu m)
	{
		List<String> list = new ArrayList<>();
		for (ContextMenu.Item item : m.items())
			list.add(item.isLine() ? "-" : item.label());

		return list;
	}

	@Test
	public void battleFirstShowsAHeadingBattleAndACatchThatOpensAList()
	{
		open(JarType.BRONZE, threeJars(), true, false, 100, 100);

		assertTrue(menu.isOpen());
		assertEquals(7, menu.wildId());
		assertEquals(Arrays.asList("Chicken (Lv 3 - New)", "Battle", "Catch"), labels(menu.rootMenu()));
		assertEquals(2, menu.openerIndex());
		assertTrue(rootItem(2).opens());
		assertFalse(menu.listOpen());
	}

	@Test
	public void catchFirstShowsAPlainCatchWithTheOddsThenTheListThenBattle()
	{
		open(JarType.IRON, threeJars(), true, true, 100, 100);

		assertEquals(Arrays.asList("Chicken (Lv 3 - New)", "Catch", "Catch", "Battle"), labels(menu.rootMenu()));
		assertEquals("Iron jar 56%", rootItem(1).reason());
		assertFalse(rootItem(1).opens());
		assertEquals(2, menu.openerIndex());
	}

	@Test
	public void oneJarHasNoListAndNoJarsDisablesCatch()
	{
		open(JarType.BRONZE, Collections.singletonList(choice(JarType.BRONZE, 5, 0.6, false)), true, false, 10, 10);

		assertEquals(-1, menu.openerIndex());
		assertEquals("Bronze jar 60%", rootItem(2).reason());

		open(null, Collections.emptyList(), true, false, 10, 10);

		ContextMenu.Item none = rootItem(2);
		assertFalse(none.enabled());
		assertEquals("No jars", none.reason());
	}

	@Test
	public void hoveringCatchOpensTheListAndHoveringBattleClosesIt()
	{
		open(JarType.BRONZE, threeJars(), true, false, 100, 100);

		hoverOpener();

		assertTrue(menu.listOpen());
		Rectangle battle = menu.rootMenu().itemBounds(1);
		menu.onMoved(battle.x + 4, battle.y + 2);
		assertFalse(menu.listOpen());
	}

	@Test
	public void theListHoldsSuggestedJarsALineAndTheRestWithOddsAndCounts()
	{
		open(JarType.BRONZE, threeJars(), true, false, 100, 100);

		hoverOpener();

		assertEquals(Arrays.asList("Dragon jar", "Slash jar", "-", "Iron jar", "Bronze jar"),
			labels(menu.listMenu()));
		assertEquals("95% (x3)", listItem(0).reason());
		assertEquals("94% (x4)", listItem(1).reason());
	}

	@Test
	public void theListSitsRightOfTheMenuLevelWithCatch()
	{
		open(JarType.BRONZE, threeJars(), true, false, 100, 100);

		hoverOpener();

		Rectangle first = menu.rootMenu().bounds();
		Rectangle list = menu.listMenu().bounds();
		assertEquals(first.x + first.width, list.x);
		Rectangle opener = menu.rootMenu().itemBounds(menu.openerIndex());
		assertEquals(opener.y, list.y);
	}

	@Test
	public void theListFlipsToTheLeftAtTheRightEdgeAndStaysInsideTheCanvasAtTheBottom()
	{
		open(JarType.BRONZE, threeJars(), true, false, 760, 590);

		hoverOpener();

		Rectangle first = menu.rootMenu().bounds();
		Rectangle list = menu.listMenu().bounds();
		assertTrue(list.x + list.width <= first.x);
		assertTrue(CANVAS.contains(list));
		assertTrue(CANVAS.contains(first));
	}

	@Test
	public void clickingAJarInTheListThrowsItAndClosesTheMenu()
	{
		open(JarType.BRONZE, threeJars(), true, false, 100, 100);
		hoverOpener();
		Rectangle row = menu.listMenu().itemBounds(1);

		assertTrue(menu.onPressed(row.x + 4, row.y + 2, LEFT));

		assertEquals(Collections.singletonList("throw SLASH"), ran);
		assertFalse(menu.isOpen());
		assertFalse(menu.listOpen());
	}

	@Test
	public void clickingBattleBattlesAndClosesTheMenu()
	{
		open(JarType.BRONZE, threeJars(), true, false, 100, 100);
		Rectangle battle = menu.rootMenu().itemBounds(1);

		assertTrue(menu.onPressed(battle.x + 4, battle.y + 2, LEFT));

		assertEquals(Collections.singletonList("battle"), ran);
		assertFalse(menu.isOpen());
	}

	@Test
	public void clickingAPlainCatchThrowsTheStandardJar()
	{
		open(JarType.IRON, threeJars(), true, true, 100, 100);
		Rectangle plain = menu.rootMenu().itemBounds(1);

		menu.onPressed(plain.x + 4, plain.y + 2, LEFT);

		assertEquals(Collections.singletonList("throw IRON"), ran);
	}

	@Test
	public void clickingTheCatchItemKeepsTheMenuOpenAndShowsTheList()
	{
		open(JarType.BRONZE, threeJars(), true, false, 100, 100);
		Rectangle opener = menu.rootMenu().itemBounds(menu.openerIndex());

		assertTrue(menu.onPressed(opener.x + 4, opener.y + 2, LEFT));

		assertTrue(menu.isOpen());
		assertTrue(menu.listOpen());
		assertTrue(ran.isEmpty());
	}

	@Test
	public void aClickElsewhereClosesTheMenuIsConsumedAndRunsNothing()
	{
		open(JarType.BRONZE, threeJars(), true, false, 100, 100);
		hoverOpener();

		assertTrue(menu.onPressed(700, 500, LEFT));

		assertFalse(menu.isOpen());
		assertTrue(ran.isEmpty());
	}

	@Test
	public void aClosedMenuTakesNothing()
	{
		assertFalse(menu.onPressed(5, 5, LEFT));
		assertFalse(menu.onMoved(5, 5));
		assertFalse(menu.onKeyPressed(KeyEvent.VK_ESCAPE));
		assertFalse(menu.uses(KeyEvent.VK_UP));
	}

	@Test
	public void movingOffTheMenuReportsNotOverIt()
	{
		open(JarType.BRONZE, threeJars(), true, false, 100, 100);

		assertFalse(menu.onMoved(700, 500));
		assertTrue(menu.isOpen());
	}

	@Test
	public void theKeysWalkTheMenuOpenTheListAndChooseAJar()
	{
		open(JarType.BRONZE, threeJars(), true, false, 100, 100);

		menu.onKeyPressed(KeyEvent.VK_DOWN);
		assertEquals(1, menu.rootMenu().highlight());
		menu.onKeyPressed(KeyEvent.VK_DOWN);
		assertEquals(2, menu.rootMenu().highlight());
		menu.onKeyPressed(KeyEvent.VK_RIGHT);

		assertTrue(menu.listOpen());
		assertEquals(0, menu.listMenu().highlight());
		menu.onKeyPressed(KeyEvent.VK_DOWN);
		assertEquals(1, menu.listMenu().highlight());
		menu.onKeyPressed(KeyEvent.VK_DOWN);
		assertEquals("the line is skipped", 3, menu.listMenu().highlight());
		menu.onKeyPressed(KeyEvent.VK_ENTER);

		assertEquals(Collections.singletonList("throw IRON"), ran);
		assertFalse(menu.isOpen());
	}

	@Test
	public void leftClosesTheListAndEscClosesEverything()
	{
		open(JarType.BRONZE, threeJars(), true, false, 100, 100);
		menu.onKeyPressed(KeyEvent.VK_DOWN);
		menu.onKeyPressed(KeyEvent.VK_DOWN);
		menu.onKeyPressed(KeyEvent.VK_RIGHT);

		menu.onKeyPressed(KeyEvent.VK_LEFT);

		assertFalse(menu.listOpen());
		assertTrue(menu.isOpen());
		menu.onKeyPressed(KeyEvent.VK_RIGHT);
		assertTrue(menu.listOpen());
		assertTrue(menu.onKeyPressed(KeyEvent.VK_ESCAPE));
		assertFalse(menu.isOpen());
		assertFalse(menu.listOpen());
	}

	@Test
	public void enterOnTheCatchItemOpensTheListWithAHighlight()
	{
		open(JarType.BRONZE, threeJars(), true, false, 100, 100);
		menu.onKeyPressed(KeyEvent.VK_DOWN);
		menu.onKeyPressed(KeyEvent.VK_DOWN);

		menu.onKeyPressed(KeyEvent.VK_ENTER);

		assertTrue(menu.isOpen());
		assertTrue(menu.listOpen());
		assertEquals(0, menu.listMenu().highlight());
	}

	@Test
	public void enterOnBattleBattles()
	{
		open(JarType.BRONZE, threeJars(), true, false, 100, 100);
		menu.onKeyPressed(KeyEvent.VK_DOWN);

		menu.onKeyPressed(KeyEvent.VK_ENTER);

		assertEquals(Collections.singletonList("battle"), ran);
		assertFalse(menu.isOpen());
	}

	@Test
	public void theMenuUsesOnlyItsKeys()
	{
		open(JarType.BRONZE, threeJars(), true, false, 100, 100);

		for (int key : new int[] {KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
			KeyEvent.VK_ENTER, KeyEvent.VK_SPACE, KeyEvent.VK_ESCAPE, KeyEvent.VK_BACK_SPACE})
			assertTrue(menu.uses(key));

		assertFalse(menu.uses(KeyEvent.VK_A));
		assertFalse(menu.onKeyPressed(KeyEvent.VK_A));
	}

	@Test
	public void openingAgainReplacesTheOldMenu()
	{
		open(JarType.BRONZE, threeJars(), true, false, 100, 100);
		hoverOpener();

		open(JarType.BRONZE, Collections.singletonList(choice(JarType.BRONZE, 5, 0.6, false)), false, true, 50, 50);

		assertFalse(menu.listOpen());
		assertEquals(Arrays.asList("Chicken (Lv 3 - New)", "Catch"), labels(menu.rootMenu()));
	}

	@Test
	public void paintingDrawsTheMenuAndTheListInsideTheCanvas()
	{
		open(JarType.BRONZE, threeJars(), true, false, 760, 300);
		hoverOpener();
		BufferedImage image = WidgetKit.canvas(CANVAS.width, CANVAS.height);
		Graphics2D g = WidgetKit.graphics(image);

		menu.paint(g, STYLE, CANVAS, MotionMode.OFF, 1_000);
		g.dispose();

		Rectangle first = menu.rootMenu().bounds();
		Rectangle list = menu.listMenu().bounds();
		assertTrue(WidgetKit.painted(image, first.x, first.y, first.width, first.height) > 0);
		assertTrue(WidgetKit.painted(image, list.x, list.y, list.width, list.height) > 0);
		assertTrue(CANVAS.contains(list));
	}

	@Test
	public void aClosedMenuPaintsNothing()
	{
		BufferedImage image = WidgetKit.canvas(CANVAS.width, CANVAS.height);
		Graphics2D g = WidgetKit.graphics(image);

		menu.paint(g, STYLE, CANVAS, MotionMode.OFF, 0);
		g.dispose();

		assertEquals(0, WidgetKit.painted(image, 0, 0, CANVAS.width, CANVAS.height));
	}
}
