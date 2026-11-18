/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import net.runelite.client.config.Keybind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Checks the UI-thread side of the painted wild creature menu: which right clicks it takes, what it consumes while
 * the menu is open, and which keys it swallows.
 */
public class WildMenuInputTest
{
	private final List<String> calls = new ArrayList<>();
	private WildMenuInput.Spot spot = new WildMenuInput.Spot(9, 200, 150);
	private boolean open;
	private long now = 1_000;
	private boolean typing;
	private final DragKey drag = new DragKey(() -> Keybind.ALT);
	private final WildMenuInput input = new WildMenuInput(() -> spot, () -> open, drag, () -> now,
		new WildMenuInput.Sink()
		{
			@Override
			public void open(int wildId, int x, int y)
			{
				calls.add("open " + wildId + " " + x + "," + y);
			}

			@Override
			public void moved(int x, int y)
			{
				calls.add("moved " + x + "," + y);
			}

			@Override
			public void pressed(int x, int y, int button)
			{
				calls.add("pressed " + x + "," + y + " b" + button);
			}
		});
	private final WildMenuKeys keys = new WildMenuKeys(code -> open && code == KeyEvent.VK_DOWN, drag, () -> typing,
		code -> calls.add("key " + code));

	private MouseEvent press(int x, int y, int button, int modifiers)
	{
		MouseEvent event = WindowEvents.mouse(MouseEvent.MOUSE_PRESSED, x, y, button, modifiers);
		input.mousePressed(event);
		return event;
	}

	@Test
	public void aRightClickOnTheSpotIsTakenAndOpensTheMenu()
	{
		MouseEvent event = press(202, 151, MouseEvent.BUTTON3, 0);

		assertTrue(event.isConsumed());
		assertEquals(1, calls.size());
		assertEquals("open 9 202,151", calls.get(0));
	}

	@Test
	public void aLeftClickOnTheSpotPassesThroughSoTheGameBattles()
	{
		MouseEvent event = press(200, 150, MouseEvent.BUTTON1, 0);

		assertFalse(event.isConsumed());
		assertTrue(calls.isEmpty());
	}

	@Test
	public void aRightClickAwayFromTheSpotOrWithNoSpotPassesThrough()
	{
		assertFalse(press(200 + WildMenuInput.TOLERANCE + 1, 150, MouseEvent.BUTTON3, 0).isConsumed());
		spot = null;
		assertFalse(press(200, 150, MouseEvent.BUTTON3, 0).isConsumed());
		assertTrue(calls.isEmpty());
	}

	@Test
	public void shiftOrTheDragKeyLetsARightClickReachTheGameMenu()
	{
		assertFalse(press(200, 150, MouseEvent.BUTTON3, InputEvent.SHIFT_DOWN_MASK).isConsumed());
		assertFalse(press(200, 150, MouseEvent.BUTTON3, InputEvent.ALT_DOWN_MASK).isConsumed());
		assertTrue(calls.isEmpty());
	}

	@Test
	public void theReleaseAndTheClickOfATakenPressAreTakenToo()
	{
		press(200, 150, MouseEvent.BUTTON3, 0);
		MouseEvent release = WindowEvents.mouse(MouseEvent.MOUSE_RELEASED, 200, 150, MouseEvent.BUTTON3, 0);
		MouseEvent click = WindowEvents.mouse(MouseEvent.MOUSE_CLICKED, 200, 150, MouseEvent.BUTTON3, 0);

		input.mouseReleased(release);
		input.mouseClicked(click);

		assertTrue(release.isConsumed());
		assertTrue(click.isConsumed());
		MouseEvent later = WindowEvents.mouse(MouseEvent.MOUSE_RELEASED, 5, 5, MouseEvent.BUTTON1, 0);
		input.mouseReleased(later);
		assertFalse(later.isConsumed());
	}

	@Test
	public void aReleaseOfAnUntakenPressPassesThrough()
	{
		press(200, 150, MouseEvent.BUTTON1, 0);
		MouseEvent release = WindowEvents.mouse(MouseEvent.MOUSE_RELEASED, 200, 150, MouseEvent.BUTTON1, 0);

		input.mouseReleased(release);

		assertFalse(release.isConsumed());
	}

	@Test
	public void whileTheMenuIsOpenEveryPressIsConsumedAndSentOn()
	{
		open = true;

		MouseEvent left = press(10, 20, MouseEvent.BUTTON1, 0);
		MouseEvent right = press(30, 40, MouseEvent.BUTTON3, 0);

		assertTrue(left.isConsumed());
		assertTrue(right.isConsumed());
		assertEquals(2, calls.size());
		assertEquals("pressed 10,20 b1", calls.get(0));
		assertEquals("pressed 30,40 b3", calls.get(1));
	}

	@Test
	public void theDragKeyStillPassesThroughWhileTheMenuIsOpen()
	{
		open = true;

		assertFalse(press(10, 20, MouseEvent.BUTTON1, InputEvent.ALT_DOWN_MASK).isConsumed());
		assertTrue(calls.isEmpty());
	}

	@Test
	public void clicksAreSwallowedWhileTheMenuIsBeingOpened()
	{
		press(200, 150, MouseEvent.BUTTON3, 0);
		calls.clear();
		now += 100;

		MouseEvent second = press(200, 150, MouseEvent.BUTTON3, 0);

		assertTrue(second.isConsumed());
		assertTrue(calls.isEmpty());
		now += WildMenuInput.OPENING_MS;
		assertTrue(press(200, 150, MouseEvent.BUTTON3, 0).isConsumed());
		assertEquals(1, calls.size());
	}

	@Test
	public void movesAreSentOnlyWhileOpenAndNeverConsumed()
	{
		MouseEvent closed = WindowEvents.mouse(MouseEvent.MOUSE_MOVED, 5, 6, 0, 0);
		input.mouseMoved(closed);
		open = true;
		MouseEvent moved = WindowEvents.mouse(MouseEvent.MOUSE_MOVED, 7, 8, 0, 0);
		input.mouseMoved(moved);
		MouseEvent dragged = WindowEvents.mouse(MouseEvent.MOUSE_DRAGGED, 9, 10, 0, 0);
		input.mouseDragged(dragged);

		assertEquals(2, calls.size());
		assertEquals("moved 7,8", calls.get(0));
		assertEquals("moved 9,10", calls.get(1));
		assertFalse(moved.isConsumed());
	}

	@Test
	public void theMenuKeysAreSwallowedOnlyWhileOpen()
	{
		KeyEvent closed = WindowEvents.press(KeyEvent.VK_DOWN, 0);
		keys.keyPressed(closed);
		assertFalse(closed.isConsumed());
		keys.keyReleased(WindowEvents.release(KeyEvent.VK_DOWN, 0));

		open = true;
		KeyEvent down = WindowEvents.press(KeyEvent.VK_DOWN, 0);
		keys.keyPressed(down);
		KeyEvent up = WindowEvents.release(KeyEvent.VK_DOWN, 0);
		keys.keyReleased(up);

		assertTrue(down.isConsumed());
		assertTrue(up.isConsumed());
		assertEquals(1, calls.size());
		assertEquals("key " + KeyEvent.VK_DOWN, calls.get(0));
	}

	@Test
	public void otherKeysChordsAndChatTypingPassThrough()
	{
		open = true;

		KeyEvent other = WindowEvents.press(KeyEvent.VK_A, 0);
		keys.keyPressed(other);
		KeyEvent chord = WindowEvents.press(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK);
		keys.keyPressed(chord);
		typing = true;
		KeyEvent chat = WindowEvents.press(KeyEvent.VK_DOWN, 0);
		keys.keyPressed(chat);

		assertFalse(other.isConsumed());
		assertFalse(chord.isConsumed());
		assertFalse(chat.isConsumed());
		assertTrue(calls.isEmpty());
	}

	@Test
	public void aHeldArrowRepeatsToTheMenu()
	{
		open = true;

		keys.keyPressed(WindowEvents.press(KeyEvent.VK_DOWN, 0));
		keys.keyPressed(WindowEvents.press(KeyEvent.VK_DOWN, 0));

		assertEquals(2, calls.size());
	}
}
