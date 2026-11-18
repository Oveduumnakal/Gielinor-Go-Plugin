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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the context menu: layout, the shared highlight, keys, clicks, placement and painting. */
public class ContextMenuTest
{
	private static final WindowStyle STYLE = WidgetKit.style(1.0);
	private static final FontMetrics FM = WidgetKit.metrics(STYLE, WindowFonts.Face.REGULAR);
	private static final Rectangle CANVAS = new Rectangle(0, 0, 500, 400);

	private final List<String> ran = new ArrayList<>();

	private List<ContextMenu.Item> items()
	{
		return Arrays.asList(
			ContextMenu.Item.of("Summary", () -> ran.add("Summary")),
			ContextMenu.Item.of("Make lead", () -> ran.add("Make lead")),
			ContextMenu.Item.disabled("Pin to tracker", "Coming with the tracker"),
			ContextMenu.Item.of("Rename", () -> ran.add("Rename")));
	}

	private ContextMenu open(boolean keyboard)
	{
		ContextMenu menu = new ContextMenu();
		menu.open(items(), 100, 100, CANVAS, STYLE, FM, keyboard, 0);
		return menu;
	}

	@Test
	public void aMenuOpenedByTheMouseHasNoHighlightAndOneByAKeyStartsOnTheFirstItem()
	{
		assertEquals(-1, open(false).highlight());
		assertEquals("Summary first", 0, open(true).highlight());
		assertTrue(open(true).isOpen());
	}

	@Test
	public void theKeyboardHighlightSkipsADisabledFirstItem()
	{
		ContextMenu menu = new ContextMenu();
		menu.open(Arrays.asList(ContextMenu.Item.disabled("Revive", "Not fainted"),
			ContextMenu.Item.of("Summary", () -> ran.add("Summary"))), 10, 10, CANVAS, STYLE, FM, true, 0);

		assertEquals(1, menu.highlight());
	}

	@Test
	public void anEmptyListDoesNotOpen()
	{
		ContextMenu menu = new ContextMenu();

		menu.open(new ArrayList<>(), 10, 10, CANVAS, STYLE, FM, true, 0);

		assertFalse(menu.isOpen());
	}

	@Test
	public void theItemsAreStackedInsideTheMenuBox()
	{
		ContextMenu menu = open(false);
		Rectangle bounds = menu.bounds();

		assertEquals(100, bounds.x);
		assertEquals(100, bounds.y);
		for (int i = 0; i < 4; i++)
		{
			Rectangle box = menu.itemBounds(i);
			assertTrue(bounds.contains(box));
			assertEquals(i, menu.itemAt(box.x + 2, box.y + 2));
			if (i > 0)
				assertEquals(menu.itemBounds(i - 1).y + menu.itemBounds(i - 1).height, box.y);
		}

		assertNull(menu.itemBounds(4));
		assertNull(menu.itemBounds(-1));
		assertEquals(-1, menu.itemAt(5, 5));
	}

	@Test
	public void theMenuIsWideEnoughForTheLongestLabelAndAReason()
	{
		ContextMenu menu = open(false);
		int needed = FM.stringWidth("Pin to tracker") + FM.stringWidth("Coming with the tracker");

		assertTrue(menu.bounds().width > needed);
	}

	@Test
	public void downAndUpWrapAndSkipDisabledItems()
	{
		ContextMenu menu = open(true);

		menu.onKeyPressed(KeyEvent.VK_DOWN);
		assertEquals(1, menu.highlight());
		menu.onKeyPressed(KeyEvent.VK_DOWN);
		assertEquals("skips the disabled Pin to tracker", 3, menu.highlight());
		menu.onKeyPressed(KeyEvent.VK_DOWN);
		assertEquals("wraps to the top", 0, menu.highlight());
		menu.onKeyPressed(KeyEvent.VK_UP);
		assertEquals("wraps to the bottom", 3, menu.highlight());
		menu.onKeyPressed(KeyEvent.VK_UP);
		assertEquals(1, menu.highlight());
	}

	@Test
	public void upFromNoHighlightGoesToTheLastItemAndDownToTheFirst()
	{
		ContextMenu up = open(false);
		up.onKeyPressed(KeyEvent.VK_UP);
		ContextMenu down = open(false);
		down.onKeyPressed(KeyEvent.VK_DOWN);

		assertEquals(3, up.highlight());
		assertEquals(0, down.highlight());
	}

	@Test
	public void enterAndSpaceRunTheHighlightedItemAndCloseTheMenu()
	{
		ContextMenu menu = open(true);
		assertTrue(menu.onKeyPressed(KeyEvent.VK_ENTER));

		assertEquals(List.of("Summary"), ran);
		assertFalse(menu.isOpen());

		menu = open(true);
		menu.onKeyPressed(KeyEvent.VK_DOWN);
		assertTrue(menu.onKeyPressed(KeyEvent.VK_SPACE));
		assertEquals(List.of("Summary", "Make lead"), ran);
	}

	@Test
	public void enterWithNothingHighlightedDoesNothingAndKeepsTheMenuOpen()
	{
		ContextMenu menu = open(false);

		assertTrue(menu.onKeyPressed(KeyEvent.VK_ENTER));

		assertTrue(ran.isEmpty());
		assertTrue(menu.isOpen());
	}

	@Test
	public void escapeAndBackspaceCloseWithoutRunningAnything()
	{
		ContextMenu esc = open(true);
		ContextMenu back = open(true);

		assertTrue(esc.onKeyPressed(KeyEvent.VK_ESCAPE));
		assertTrue(back.onKeyPressed(KeyEvent.VK_BACK_SPACE));

		assertFalse(esc.isOpen());
		assertFalse(back.isOpen());
		assertTrue(ran.isEmpty());
	}

	@Test
	public void onlyItsKeysAreUsedAndOnlyWhileOpen()
	{
		ContextMenu menu = open(true);

		assertTrue(menu.uses(KeyEvent.VK_UP));
		assertTrue(menu.uses(KeyEvent.VK_ENTER));
		assertFalse(menu.uses(KeyEvent.VK_LEFT));
		assertFalse(menu.uses(KeyEvent.VK_A));
		assertFalse(menu.onKeyPressed(KeyEvent.VK_LEFT));
		menu.close();
		assertFalse(menu.uses(KeyEvent.VK_UP));
		assertFalse(menu.onKeyPressed(KeyEvent.VK_UP));
	}

	@Test
	public void thePointerAndTheKeysShareOneHighlight()
	{
		ContextMenu menu = open(false);
		Rectangle second = menu.itemBounds(1);

		assertTrue(menu.onMouseMoved(second.x + 3, second.y + 3));
		assertEquals(1, menu.highlight());
		menu.onKeyPressed(KeyEvent.VK_DOWN);
		assertEquals("keys continue from where the pointer was", 3, menu.highlight());
		Rectangle disabled = menu.itemBounds(2);
		menu.onMouseMoved(disabled.x + 3, disabled.y + 3);
		assertEquals("a disabled item is not highlighted", 3, menu.highlight());
		assertFalse("off the menu", menu.onMouseMoved(5, 5));
		assertEquals(3, menu.highlight());
	}

	@Test
	public void aLeftClickOnAnEnabledItemRunsItAndClosesTheMenu()
	{
		ContextMenu menu = open(false);
		Rectangle box = menu.itemBounds(1);

		assertTrue(menu.onMousePressed(box.x + 4, box.y + 4, MouseEvent.BUTTON1));

		assertEquals(List.of("Make lead"), ran);
		assertFalse(menu.isOpen());
	}

	@Test
	public void aClickOnADisabledItemOrWithAnotherButtonDoesNothingAndStaysOpen()
	{
		ContextMenu menu = open(false);
		Rectangle disabled = menu.itemBounds(2);
		Rectangle enabled = menu.itemBounds(1);

		assertTrue(menu.onMousePressed(disabled.x + 4, disabled.y + 4, MouseEvent.BUTTON1));
		assertTrue(menu.onMousePressed(enabled.x + 4, enabled.y + 4, MouseEvent.BUTTON3));

		assertTrue(ran.isEmpty());
		assertTrue(menu.isOpen());
	}

	@Test
	public void aClickOutsideClosesTheMenuAndIsStillConsumed()
	{
		ContextMenu menu = open(false);

		assertTrue(menu.onMousePressed(400, 350, MouseEvent.BUTTON1));
		assertFalse(menu.isOpen());
		assertTrue(ran.isEmpty());
		assertFalse("a closed menu takes nothing", menu.onMousePressed(400, 350, MouseEvent.BUTTON1));
		assertFalse(menu.onMouseMoved(110, 110));
	}

	@Test
	public void theMenuFlipsAndStaysInsideTheCanvasAtTheEdges()
	{
		ContextMenu menu = new ContextMenu();

		menu.open(items(), 495, 395, CANVAS, STYLE, FM, false, 0);
		Rectangle corner = menu.bounds();
		menu.open(items(), 5, 5, CANVAS, STYLE, FM, false, 0);
		Rectangle origin = menu.bounds();

		assertTrue(CANVAS.contains(corner));
		assertEquals("flipped to the left of the click", 495, corner.x + corner.width);
		assertEquals("flipped above the click", 395, corner.y + corner.height);
		assertEquals(new Rectangle(5, 5, origin.width, origin.height), origin);
		for (int i = 0; i < 4; i++)
			assertTrue(origin.contains(menu.itemBounds(i)));
	}

	@Test
	public void relayoutFollowsANewScaleAndCanvas()
	{
		ContextMenu menu = open(false);
		Rectangle before = menu.bounds();
		WindowStyle big = WidgetKit.style(2.0);
		FontMetrics bigFm = WidgetKit.metrics(big, WindowFonts.Face.REGULAR);

		Rectangle wide = new Rectangle(0, 0, 2000, 1000);
		menu.layout(wide, big, bigFm);

		assertTrue(menu.bounds().height > before.height);
		assertTrue(menu.bounds().width > before.width);
		assertTrue(wide.contains(menu.bounds()));
	}

	@Test
	public void hitRectanglesAreFinalWhileTheMenuAnimatesOpen()
	{
		ContextMenu menu = open(false);
		Rectangle before = menu.itemBounds(1);
		BufferedImage image = WidgetKit.canvas(500, 400);
		Graphics2D g = WidgetKit.graphics(image);
		menu.paint(g, STYLE, CANVAS, MotionMode.FULL, 10);
		g.dispose();

		assertEquals(before, menu.itemBounds(1));
		assertTrue(menu.onMousePressed(before.x + 3, before.y + 3, MouseEvent.BUTTON1));
		assertEquals("a click at the first frame already lands", List.of("Make lead"), ran);
	}

	@Test
	public void paintShowsThePanelTheHighlightAndDimmedItems()
	{
		ContextMenu menu = open(true);
		BufferedImage image = WidgetKit.canvas(500, 400);
		Graphics2D g = WidgetKit.graphics(image);
		menu.paint(g, STYLE, CANVAS, MotionMode.OFF, 1_000);
		g.dispose();

		Rectangle bounds = menu.bounds();
		assertEquals("nothing outside the menu", 0, WidgetKit.painted(image, 0, 0, 500, bounds.y - 1));
		assertEquals(STYLE.color(ThemeColor.PANEL_BOX).getRGB(), image.getRGB(bounds.x + bounds.width - 4,
			bounds.y + bounds.height / 2));
		Rectangle first = menu.itemBounds(0);
		int highlighted = image.getRGB(first.x + first.width - 3, first.y + 2);
		assertTrue("highlight differs from the panel", highlighted != STYLE.color(ThemeColor.PANEL_BOX).getRGB());
		Rectangle second = menu.itemBounds(1);
		assertEquals(STYLE.color(ThemeColor.PANEL_BOX).getRGB(), image.getRGB(second.x + second.width - 3,
			second.y + 2));
	}

	@Test
	public void aMenuFadesInDuringItsOpenTimeInFullMotionAndNotInOff()
	{
		ContextMenu menu = open(false);
		BufferedImage start = WidgetKit.canvas(500, 400);
		BufferedImage later = WidgetKit.canvas(500, 400);
		BufferedImage off = WidgetKit.canvas(500, 400);
		Graphics2D g = WidgetKit.graphics(start);
		menu.paint(g, STYLE, CANVAS, MotionMode.FULL, 0);
		g.dispose();
		g = WidgetKit.graphics(later);
		menu.paint(g, STYLE, CANVAS, MotionMode.FULL, WindowTimes.MENU);
		g.dispose();
		g = WidgetKit.graphics(off);
		menu.paint(g, STYLE, CANVAS, MotionMode.OFF, 0);
		g.dispose();

		Rectangle bounds = menu.bounds();
		int x = bounds.x + bounds.width - 4;
		int y = bounds.y + bounds.height / 2;
		assertTrue("starts transparent", (start.getRGB(x, y) >>> 24) < (later.getRGB(x, y) >>> 24));
		assertEquals("the same as off once it is open", later.getRGB(x, y), off.getRGB(x, y));
	}

	@Test
	public void aClosedMenuPaintsNothing()
	{
		ContextMenu menu = open(false);
		menu.close();
		BufferedImage image = WidgetKit.canvas(500, 400);
		Graphics2D g = WidgetKit.graphics(image);
		menu.paint(g, STYLE, CANVAS, MotionMode.FULL, 0);
		g.dispose();

		assertEquals(0, WidgetKit.painted(image, 0, 0, 500, 400));
	}

	@Test
	public void theItemsReportTheirLabelStateAndReason()
	{
		List<ContextMenu.Item> list = items();

		assertEquals("Summary", list.get(0).label());
		assertTrue(list.get(0).enabled());
		assertEquals("", list.get(0).reason());
		assertFalse(list.get(2).enabled());
		assertEquals("Coming with the tracker", list.get(2).reason());
		ContextMenu menu = open(false);
		assertEquals(4, menu.items().size());
		assertEquals(100, menu.anchor().x);
	}

	private static ContextMenu.Item itemOf(ContextMenu menu, int index)
	{
		return menu.items().get(index);
	}

	private ContextMenu openNested()
	{
		ContextMenu menu = new ContextMenu();
		menu.open(Arrays.asList(
			ContextMenu.Item.header("Chicken"),
			ContextMenu.Item.of("Battle", () -> ran.add("Battle")),
			ContextMenu.Item.line(),
			ContextMenu.Item.opener("Catch", () -> ran.add("Catch")),
			ContextMenu.Item.detailed("Bronze jar", "60% (x3)", () -> ran.add("Bronze"))), 100, 100, CANVAS, STYLE,
			FM, false, 0);
		return menu;
	}

	@Test
	public void anOpenerRunsItsActionAndKeepsTheMenuOpen()
	{
		ContextMenu menu = openNested();
		Rectangle box = menu.itemBounds(3);

		boolean consumed = menu.onMousePressed(box.x + 5, box.y + 2, MouseEvent.BUTTON1);

		assertTrue(consumed);
		assertEquals(Collections.singletonList("Catch"), ran);
		assertTrue(menu.isOpen());
	}

	@Test
	public void anOpenerChosenByAKeyKeepsTheMenuOpenToo()
	{
		ContextMenu menu = openNested();
		menu.move(1);
		menu.move(1);

		assertEquals(3, menu.highlight());
		assertTrue(menu.activate());
		assertTrue(menu.isOpen());
		assertEquals(Collections.singletonList("Catch"), ran);
	}

	@Test
	public void anItemWithADetailClosesTheMenuAndRuns()
	{
		ContextMenu menu = openNested();
		Rectangle box = menu.itemBounds(4);

		menu.onMousePressed(box.x + 5, box.y + 2, MouseEvent.BUTTON1);

		assertEquals(Collections.singletonList("Bronze"), ran);
		assertFalse(menu.isOpen());
	}

	@Test
	public void theHeaderAndTheLineCannotBeChosenAndTheKeysSkipThem()
	{
		ContextMenu menu = openNested();
		Rectangle header = menu.itemBounds(0);
		Rectangle line = menu.itemBounds(2);

		menu.onMousePressed(header.x + 5, header.y + 2, MouseEvent.BUTTON1);
		menu.onMousePressed(line.x + 5, line.y + 2, MouseEvent.BUTTON1);

		assertTrue(menu.isOpen());
		assertTrue(ran.isEmpty());
		assertTrue(itemOf(menu, 2).isLine());
		menu.move(1);
		assertEquals(1, menu.highlight());
		menu.move(1);
		assertEquals(3, menu.highlight());
	}

	@Test
	public void theMenuIsWideEnoughForTheDetailAndTheArrow()
	{
		ContextMenu menu = openNested();
		int needed = FM.stringWidth("Bronze jar") + FM.stringWidth("60% (x3)");

		assertTrue(menu.bounds().width > needed);
	}

	@Test
	public void aMenuWithALineAnArrowAndADetailPaints()
	{
		ContextMenu menu = openNested();
		BufferedImage image = WidgetKit.canvas(500, 400);
		Graphics2D g = WidgetKit.graphics(image);

		menu.paint(g, STYLE, CANVAS, MotionMode.OFF, 1000);
		g.dispose();

		Rectangle b = menu.bounds();
		assertTrue(WidgetKit.painted(image, b.x, b.y, b.width, b.height) > 0);
		assertTrue(itemOf(menu, 3).opens());
		assertFalse(itemOf(menu, 1).opens());
	}
}
