/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import net.runelite.client.config.Keybind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Checks the UI-thread side of the wild creature bubbles: which presses it takes, what it takes with them, and what
 * it tells the game thread.
 */
public class BubbleInputTest
{
	private final List<String> calls = new ArrayList<>();
	private BubbleHits hits = hits(false);
	private final DragKey drag = new DragKey(() -> Keybind.ALT);
	private final BubbleInput input = new BubbleInput(() -> hits, drag, new BubbleInput.Sink()
	{
		@Override
		public void pressed(int generation, BubbleKind kind, JarType jar, int button)
		{
			calls.add("pressed " + generation + " " + kind + " " + jar + " b" + button);
		}

		@Override
		public void pressedOutside(int generation)
		{
			calls.add("outside " + generation);
		}
	});

	private static BubbleHits hits(boolean modal)
	{
		return new BubbleHits(4, modal, Collections.singletonList(
			BubbleHits.Hit.disc(BubbleKind.BATTLE, null, 200, 150, 15)));
	}

	private MouseEvent press(int x, int y, int button, int modifiers)
	{
		MouseEvent event = WindowEvents.mouse(MouseEvent.MOUSE_PRESSED, x, y, button, modifiers);
		input.mousePressed(event);
		return event;
	}

	@Test
	public void aLeftPressOnABubbleIsTakenAndSentOn()
	{
		MouseEvent event = press(202, 151, MouseEvent.BUTTON1, 0);

		assertTrue(event.isConsumed());
		assertEquals(1, calls.size());
		assertEquals("pressed 4 BATTLE null b1", calls.get(0));
	}

	@Test
	public void aRightPressOnABubbleIsTakenAndSentOn()
	{
		MouseEvent event = press(200, 150, MouseEvent.BUTTON3, 0);

		assertTrue(event.isConsumed());
		assertEquals("pressed 4 BATTLE null b3", calls.get(0));
	}

	@Test
	public void theReleaseAndTheClickOfATakenPressAreTakenToo()
	{
		press(200, 150, MouseEvent.BUTTON1, 0);
		MouseEvent release = WindowEvents.mouse(MouseEvent.MOUSE_RELEASED, 200, 150, MouseEvent.BUTTON1, 0);
		MouseEvent click = WindowEvents.mouse(MouseEvent.MOUSE_CLICKED, 200, 150, MouseEvent.BUTTON1, 0);

		input.mouseReleased(release);
		input.mouseClicked(click);

		assertTrue(release.isConsumed());
		assertTrue(click.isConsumed());
		MouseEvent later = WindowEvents.mouse(MouseEvent.MOUSE_RELEASED, 5, 5, MouseEvent.BUTTON1, 0);
		input.mouseReleased(later);
		assertFalse(later.isConsumed());
	}

	@Test
	public void aPressElsewherePassesThroughAndSaysNothing()
	{
		MouseEvent event = press(300, 300, MouseEvent.BUTTON1, 0);
		MouseEvent release = WindowEvents.mouse(MouseEvent.MOUSE_RELEASED, 300, 300, MouseEvent.BUTTON1, 0);
		input.mouseReleased(release);

		assertFalse(event.isConsumed());
		assertFalse(release.isConsumed());
		assertTrue(calls.isEmpty());
	}

	@Test
	public void aPressElsewhereWhileModalPassesThroughAndTellsTheSink()
	{
		hits = hits(true);

		MouseEvent event = press(300, 300, MouseEvent.BUTTON1, 0);

		assertFalse(event.isConsumed());
		assertEquals(1, calls.size());
		assertEquals("outside 4", calls.get(0));
	}

	@Test
	public void theDragKeyLetsEveryPressThrough()
	{
		hits = hits(true);

		assertFalse(press(200, 150, MouseEvent.BUTTON1, InputEvent.ALT_DOWN_MASK).isConsumed());
		assertFalse(press(300, 300, MouseEvent.BUTTON3, InputEvent.ALT_DOWN_MASK).isConsumed());
		assertTrue(calls.isEmpty());
	}

	@Test
	public void theMiddleButtonPassesThroughEvenOverABubble()
	{
		hits = hits(true);

		assertFalse(press(200, 150, MouseEvent.BUTTON2, 0).isConsumed());
		assertFalse(press(300, 300, MouseEvent.BUTTON2, 0).isConsumed());
		assertTrue(calls.isEmpty());
	}

	@Test
	public void movesEntersAndExitsAreNeverTaken()
	{
		MouseEvent moved = WindowEvents.mouse(MouseEvent.MOUSE_MOVED, 200, 150, 0, 0);
		MouseEvent entered = WindowEvents.mouse(MouseEvent.MOUSE_ENTERED, 200, 150, 0, 0);
		MouseEvent exited = WindowEvents.mouse(MouseEvent.MOUSE_EXITED, 200, 150, 0, 0);

		input.mouseMoved(moved);
		input.mouseEntered(entered);
		input.mouseExited(exited);

		assertFalse(moved.isConsumed());
		assertFalse(entered.isConsumed());
		assertFalse(exited.isConsumed());
	}

	@Test
	public void aDragIsTakenOnlyAfterATakenPress()
	{
		MouseEvent before = WindowEvents.mouse(MouseEvent.MOUSE_DRAGGED, 200, 150, MouseEvent.BUTTON1, 0);
		input.mouseDragged(before);
		assertFalse(before.isConsumed());

		press(200, 150, MouseEvent.BUTTON1, 0);
		MouseEvent during = WindowEvents.mouse(MouseEvent.MOUSE_DRAGGED, 210, 160, MouseEvent.BUTTON1, 0);
		input.mouseDragged(during);
		assertTrue(during.isConsumed());

		input.mouseReleased(WindowEvents.mouse(MouseEvent.MOUSE_RELEASED, 210, 160, MouseEvent.BUTTON1, 0));
		MouseEvent after = WindowEvents.mouse(MouseEvent.MOUSE_DRAGGED, 220, 170, MouseEvent.BUTTON1, 0);
		input.mouseDragged(after);
		assertFalse(after.isConsumed());
	}

	@Test
	public void aDragAfterAPassedPressIsNotTaken()
	{
		press(300, 300, MouseEvent.BUTTON1, 0);
		MouseEvent dragged = WindowEvents.mouse(MouseEvent.MOUSE_DRAGGED, 310, 310, MouseEvent.BUTTON1, 0);

		input.mouseDragged(dragged);

		assertFalse(dragged.isConsumed());
	}

	@Test
	public void aDraggedPressDoesNotSwallowTheNextClick()
	{
		press(200, 150, MouseEvent.BUTTON1, 0);
		input.mouseReleased(WindowEvents.mouse(MouseEvent.MOUSE_RELEASED, 205, 152, MouseEvent.BUTTON1, 0));
		press(10, 10, MouseEvent.BUTTON1, 0);
		input.mouseReleased(WindowEvents.mouse(MouseEvent.MOUSE_RELEASED, 10, 10, MouseEvent.BUTTON1, 0));
		MouseEvent click = WindowEvents.mouse(MouseEvent.MOUSE_CLICKED, 10, 10, MouseEvent.BUTTON1, 0);

		input.mouseClicked(click);

		assertFalse(click.isConsumed());
	}
}
