/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import net.runelite.client.config.Keybind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Checks the window's hit rectangles and its mouse input: left and right clicks and the wheel are consumed inside an
 * open window, everything passes through outside it, and RuneLite's drag key (Alt, or a rebound key) always passes
 * through.
 */
public class WindowHitTest
{
	private static final Rectangle WINDOW = new Rectangle(100, 50, 400, 260);

	private final List<String> calls = new ArrayList<>();
	private final Hit tab = new Hit("tab:BAG", Hit.Kind.TAB, new Rectangle(10, 10, 100, 40), 1, true, "");
	private final Hit big = new Hit("panel", Hit.Kind.AREA, new Rectangle(0, 0, 400, 260), -1, true, "");
	private final Hit row = new Hit("row:0", Hit.Kind.ROW, new Rectangle(20, 100, 200, 60), 10, true, "");
	private WindowView view = new WindowView(true, WINDOW, Arrays.asList(big, tab, row),
		WindowKeyRules.KeyMode.WINDOW, true);
	private Rectangle toggle = new Rectangle(4, 4, 44, 44);
	private Keybind dragBind = Keybind.ALT;
	private final DragKey drag = new DragKey(() -> dragBind);
	private final WindowInput input = new WindowInput(() -> view, () -> toggle, drag, new WindowInput.Sink()
	{
		@Override
		public void moved(int x, int y)
		{
			calls.add("moved " + x + "," + y);
		}

		@Override
		public void left()
		{
			calls.add("left");
		}

		@Override
		public void pressed(int x, int y, int button, int modifiers)
		{
			calls.add("pressed " + x + "," + y + " b" + button);
		}

		@Override
		public void released(int x, int y, int button, int modifiers)
		{
			calls.add("released " + x + "," + y + " b" + button);
		}

		@Override
		public void dragged(int x, int y)
		{
			calls.add("dragged " + x + "," + y);
		}

		@Override
		public void wheel(int x, int y, int rotation)
		{
			calls.add("wheel " + x + "," + y + " " + rotation);
		}

		@Override
		public void clickedOutside()
		{
			calls.add("outside");
		}

		@Override
		public void toggleClicked()
		{
			calls.add("toggle");
		}

		@Override
		public boolean resetRequested()
		{
			calls.add("reset");
			return true;
		}
	});

	private MouseEvent press(int x, int y, int button, int modifiers)
	{
		MouseEvent event = WindowEvents.mouse(MouseEvent.MOUSE_PRESSED, x, y, button, modifiers);
		input.mousePressed(event);
		return event;
	}

	private MouseEvent release(int x, int y, int button)
	{
		MouseEvent event = WindowEvents.mouse(MouseEvent.MOUSE_RELEASED, x, y, button, 0);
		input.mouseReleased(event);
		return event;
	}

	@Test
	public void altAndARightClickInsideResetsAndIsConsumedEvenWithAltAsTheDragKey()
	{
		int mask = InputEvent.ALT_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK;
		MouseEvent down = press(150, 80, MouseEvent.BUTTON3, mask);
		MouseEvent up = release(150, 80, MouseEvent.BUTTON3);

		assertTrue(down.isConsumed());
		assertTrue(up.isConsumed());
		assertEquals(List.of("reset"), calls);
	}

	@Test
	public void altAndARightClickOutsideTheWindowPassesThrough()
	{
		int mask = InputEvent.ALT_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK;
		MouseEvent down = press(700, 400, MouseEvent.BUTTON3, mask);

		assertFalse(down.isConsumed());
		assertEquals(Collections.emptyList(), calls);
	}

	@Test
	public void hitRectanglesAreInWindowPixelsAndTheLastPaintedIsOnTop()
	{
		assertEquals("tab:BAG", view.hitAt(150, 80).id());
		assertEquals("row:0", view.hitAt(150, 170).id());
		assertEquals("panel", view.hitAt(450, 300).id());
		assertNull(view.hitAt(99, 80));
		assertNull(view.hitAt(500, 80));
		assertEquals(10, view.localX(110));
		assertEquals(10, view.localY(60));
	}

	@Test
	public void aClosedWindowHasNoHitsAndCoversNothing()
	{
		view = WindowView.CLOSED;

		assertNull(view.hitAt(150, 80));
		assertFalse(view.covers(150, 80));
	}

	@Test
	public void aLeftClickInsideIsConsumedAndReportedInWindowPixels()
	{
		MouseEvent down = press(150, 80, MouseEvent.BUTTON1, 0);
		MouseEvent up = release(150, 80, MouseEvent.BUTTON1);

		assertTrue(down.isConsumed());
		assertTrue(up.isConsumed());
		assertEquals(List.of("pressed 50,30 b1", "released 50,30 b1"), calls);
	}

	@Test
	public void aRightClickInsideIsConsumedToo()
	{
		MouseEvent down = press(150, 170, MouseEvent.BUTTON3, InputEvent.BUTTON3_DOWN_MASK);
		MouseEvent up = release(150, 170, MouseEvent.BUTTON3);

		assertTrue(down.isConsumed());
		assertTrue(up.isConsumed());
		assertEquals(List.of("pressed 50,120 b3", "released 50,120 b3"), calls);
	}

	@Test
	public void theClickEventAfterAReleaseIsConsumedInsideAndNotOutside()
	{
		MouseEvent inside = WindowEvents.mouse(MouseEvent.MOUSE_CLICKED, 150, 80, MouseEvent.BUTTON1, 0);
		MouseEvent outside = WindowEvents.mouse(MouseEvent.MOUSE_CLICKED, 20, 300, MouseEvent.BUTTON1, 0);
		input.mouseClicked(inside);
		input.mouseClicked(outside);

		assertTrue(inside.isConsumed());
		assertFalse(outside.isConsumed());
	}

	@Test
	public void aReleaseOutsideAfterAPressInsideIsStillConsumed()
	{
		press(150, 80, MouseEvent.BUTTON1, 0);
		MouseEvent moved = WindowEvents.mouse(MouseEvent.MOUSE_DRAGGED, 700, 700, 0, 0);
		input.mouseDragged(moved);
		MouseEvent up = release(700, 700, MouseEvent.BUTTON1);

		assertTrue(moved.isConsumed());
		assertTrue(up.isConsumed());
		assertEquals(List.of("pressed 50,30 b1", "dragged 600,650", "released 600,650 b1"), calls);
	}

	@Test
	public void aClickOutsideAnOpenWindowPassesThroughAndDropsFocus()
	{
		MouseEvent down = press(20, 300, MouseEvent.BUTTON1, 0);
		MouseEvent up = release(20, 300, MouseEvent.BUTTON1);

		assertFalse(down.isConsumed());
		assertFalse(up.isConsumed());
		assertEquals(List.of("outside"), calls);
	}

	@Test
	public void aClickWhileTheWindowIsClosedIsNotTheWindowsBusiness()
	{
		view = WindowView.CLOSED;
		MouseEvent down = press(150, 80, MouseEvent.BUTTON1, 0);

		assertFalse(down.isConsumed());
		assertTrue(calls.isEmpty());
	}

	@Test
	public void theWheelInsideIsConsumedAndReportedOutsideItPassesThrough()
	{
		MouseWheelEvent inside = WindowEvents.wheel(150, 80, -2, 0);
		MouseWheelEvent outside = WindowEvents.wheel(20, 300, 1, 0);
		input.mouseWheelMoved(inside);
		input.mouseWheelMoved(outside);

		assertTrue(inside.isConsumed());
		assertFalse(outside.isConsumed());
		assertEquals(List.of("wheel 50,30 -2"), calls);
	}

	@Test
	public void theWheelOverAClosedWindowPassesThrough()
	{
		view = WindowView.CLOSED;
		MouseWheelEvent event = WindowEvents.wheel(150, 80, 1, 0);
		input.mouseWheelMoved(event);

		assertFalse(event.isConsumed());
		assertTrue(calls.isEmpty());
	}

	@Test
	public void theDragKeyAsAltPassesClicksDragsAndTheWheelThrough()
	{
		MouseEvent down = press(150, 80, MouseEvent.BUTTON1, InputEvent.ALT_DOWN_MASK);
		MouseEvent up = release(150, 80, MouseEvent.BUTTON1);
		MouseEvent clicked = WindowEvents.mouse(MouseEvent.MOUSE_CLICKED, 150, 80, MouseEvent.BUTTON1,
			InputEvent.ALT_DOWN_MASK);
		input.mouseClicked(clicked);
		MouseWheelEvent wheel = WindowEvents.wheel(150, 80, 1, InputEvent.ALT_DOWN_MASK);
		input.mouseWheelMoved(wheel);

		assertFalse(down.isConsumed());
		assertFalse(up.isConsumed());
		assertFalse(clicked.isConsumed());
		assertFalse(wheel.isConsumed());
		assertTrue(calls.isEmpty());
		assertTrue(drag.held());
	}

	@Test
	public void aReboundDragKeyPassesThroughAndAltActsOnTheWindowAgain()
	{
		dragBind = new Keybind(KeyEvent.VK_F, 0);
		MouseEvent altClick = press(150, 80, MouseEvent.BUTTON1, InputEvent.ALT_DOWN_MASK);
		assertTrue(altClick.isConsumed());
		release(150, 80, MouseEvent.BUTTON1);
		calls.clear();

		drag.observe(WindowEvents.press(KeyEvent.VK_F, 0), true);
		MouseEvent held = press(150, 80, MouseEvent.BUTTON1, 0);
		MouseWheelEvent wheel = WindowEvents.wheel(150, 80, 1, 0);
		input.mouseWheelMoved(wheel);
		assertFalse(held.isConsumed());
		assertFalse(wheel.isConsumed());
		assertTrue(calls.isEmpty());

		drag.observe(WindowEvents.release(KeyEvent.VK_F, 0), false);
		MouseEvent after = press(150, 80, MouseEvent.BUTTON1, 0);
		assertTrue(after.isConsumed());
		assertEquals(List.of("pressed 50,30 b1"), calls);
	}

	@Test
	public void theDragKeyNeverMeansAnythingWhenItIsNotSet()
	{
		dragBind = Keybind.NOT_SET;
		MouseEvent down = press(150, 80, MouseEvent.BUTTON1, InputEvent.ALT_DOWN_MASK);

		assertTrue(down.isConsumed());
	}

	@Test
	public void aClickOnTheToggleIsConsumedAndReportedOnReleaseOnly()
	{
		MouseEvent down = press(10, 10, MouseEvent.BUTTON1, 0);
		assertTrue(down.isConsumed());
		assertTrue(calls.isEmpty());

		MouseEvent up = release(10, 10, MouseEvent.BUTTON1);
		assertTrue(up.isConsumed());
		assertEquals(List.of("toggle"), calls);
	}

	@Test
	public void aToggleClickThatEndsElsewhereIsNotAClick()
	{
		press(10, 10, MouseEvent.BUTTON1, 0);
		MouseEvent up = release(300, 300, MouseEvent.BUTTON1);

		assertTrue(up.isConsumed());
		assertTrue(calls.isEmpty());
	}

	@Test
	public void aRightClickOnTheToggleDoesNotToggleAndAltPassesThrough()
	{
		press(10, 10, MouseEvent.BUTTON3, 0);
		release(10, 10, MouseEvent.BUTTON3);
		assertTrue(calls.isEmpty());

		MouseEvent dragged = press(10, 10, MouseEvent.BUTTON1, InputEvent.ALT_DOWN_MASK);
		assertFalse(dragged.isConsumed());
	}

	@Test
	public void noToggleMeansNothingToClick()
	{
		toggle = null;
		view = WindowView.CLOSED;
		MouseEvent down = press(10, 10, MouseEvent.BUTTON1, 0);

		assertFalse(down.isConsumed());
	}

	@Test
	public void movingInsideTheWindowReportsItsPlaceAndLeavingReportsOnce()
	{
		MouseEvent in = WindowEvents.mouse(MouseEvent.MOUSE_MOVED, 150, 80, 0, 0);
		MouseEvent out = WindowEvents.mouse(MouseEvent.MOUSE_MOVED, 20, 300, 0, 0);
		input.mouseMoved(in);
		input.mouseMoved(out);
		input.mouseMoved(WindowEvents.mouse(MouseEvent.MOUSE_MOVED, 30, 300, 0, 0));

		assertFalse(in.isConsumed());
		assertFalse(out.isConsumed());
		assertEquals(List.of("moved 50,30", "left"), calls);
	}

	@Test
	public void movingUnderTheDragKeyCountsAsLeavingTheWindow()
	{
		input.mouseMoved(WindowEvents.mouse(MouseEvent.MOUSE_MOVED, 150, 80, 0, 0));
		input.mouseMoved(WindowEvents.mouse(MouseEvent.MOUSE_MOVED, 151, 80, 0, InputEvent.ALT_DOWN_MASK));

		assertEquals(List.of("moved 50,30", "left"), calls);
	}

	@Test
	public void theMouseLeavingTheCanvasClearsTheHover()
	{
		input.mouseMoved(WindowEvents.mouse(MouseEvent.MOUSE_MOVED, 150, 80, 0, 0));
		input.mouseExited(WindowEvents.mouse(MouseEvent.MOUSE_EXITED, 0, 0, 0, 0));
		input.mouseExited(WindowEvents.mouse(MouseEvent.MOUSE_EXITED, 0, 0, 0, 0));

		assertEquals(List.of("moved 50,30", "left"), calls);
	}

	@Test
	public void theViewCopiesWhatItIsGivenSoItCannotChangeUnderTheUiThread()
	{
		List<Hit> mutable = new ArrayList<>(Collections.singletonList(tab));
		Rectangle bounds = new Rectangle(WINDOW);
		WindowView copy = new WindowView(true, bounds, mutable, WindowKeyRules.KeyMode.WINDOW, false);
		mutable.clear();
		bounds.x = 0;

		assertEquals(1, copy.hits().size());
		assertEquals(100, copy.bounds().x);
		assertFalse(copy.focused());
	}
}
