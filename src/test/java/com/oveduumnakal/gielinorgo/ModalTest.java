/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the modal: button keys, clicks, closing before calling back, the text box mode, layout and painting. */
public class ModalTest
{
	private static final WindowStyle STYLE = WidgetKit.style(1.0);
	private static final Rectangle AREA = new Rectangle(0, 0, 800, 520);
	private static final int NONE = 0;
	private static final int ALT = InputEvent.ALT_DOWN_MASK;

	private final List<Integer> chosen = new ArrayList<>();
	private int cancelled;
	private boolean openDuringChoice = true;

	private Modal release()
	{
		Modal[] holder = new Modal[1];
		holder[0] = new Modal("Release Rex?", "Rex will leave your storage for good.\nThis cannot be undone.",
			List.of("No", "Yes"), 0, index ->
			{
				openDuringChoice = holder[0].isOpen();
				chosen.add(index);
			}, () -> cancelled++);
		holder[0].open(0);
		layout(holder[0]);
		return holder[0];
	}

	private static void layout(Modal modal)
	{
		modal.layout(AREA, STYLE, WidgetKit.metrics(STYLE, WindowFonts.Face.REGULAR),
			WidgetKit.metrics(STYLE, WindowFonts.Face.BOLD));
	}

	private Modal rename(TextBox box)
	{
		Modal modal = new Modal("Rename", "Give Rex a nickname.", List.of("Cancel", "Save"), 1,
			chosen::add, () -> cancelled++).withField(box, 1);
		modal.open(0);
		layout(modal);
		return modal;
	}

	@Test
	public void aReleaseDialogStartsOnNoAndLeftAndRightWrapBetweenTheButtons()
	{
		Modal modal = release();

		assertEquals(0, modal.selected());
		assertTrue(modal.onKeyPressed(KeyEvent.VK_RIGHT, NONE, 0));
		assertEquals(1, modal.selected());
		modal.onKeyPressed(KeyEvent.VK_RIGHT, NONE, 0);
		assertEquals("wraps", 0, modal.selected());
		modal.onKeyPressed(KeyEvent.VK_LEFT, NONE, 0);
		assertEquals("wraps back", 1, modal.selected());
	}

	@Test
	public void enterChoosesTheSelectedButtonAfterTheModalClosed()
	{
		Modal modal = release();

		assertTrue(modal.onKeyPressed(KeyEvent.VK_ENTER, NONE, 0));

		assertEquals(List.of(0), chosen);
		assertFalse(modal.isOpen());
		assertFalse("closed before the callback ran", openDuringChoice);
		assertEquals(0, cancelled);
	}

	@Test
	public void spaceChoosesYesAfterMovingRight()
	{
		Modal modal = release();
		modal.onKeyPressed(KeyEvent.VK_RIGHT, NONE, 0);

		modal.onKeyPressed(KeyEvent.VK_SPACE, NONE, 0);

		assertEquals(List.of(1), chosen);
	}

	@Test
	public void escapeAndBackspaceCancelWithoutChoosing()
	{
		Modal esc = release();
		Modal back = release();

		assertTrue(esc.onKeyPressed(KeyEvent.VK_ESCAPE, NONE, 0));
		assertTrue(back.onKeyPressed(KeyEvent.VK_BACK_SPACE, NONE, 0));

		assertTrue(chosen.isEmpty());
		assertEquals(2, cancelled);
		assertFalse(esc.isOpen());
	}

	@Test
	public void usedKeysAreReportedForConsumingTheirThreeEventsAndOtherKeysPass()
	{
		Modal modal = release();

		assertTrue(modal.uses(KeyEvent.VK_LEFT, NONE));
		assertTrue(modal.onKeyReleased(KeyEvent.VK_ENTER, NONE));
		assertFalse(modal.uses(KeyEvent.VK_UP, NONE));
		assertFalse(modal.onKeyPressed(KeyEvent.VK_Q, NONE, 0));
		assertFalse("no text box, so typing is not taken", modal.onKeyTyped('q', NONE, 0));
		modal.close();
		assertFalse(modal.uses(KeyEvent.VK_LEFT, NONE));
		assertFalse(modal.onKeyPressed(KeyEvent.VK_ENTER, NONE, 0));
	}

	@Test
	public void aClickOnAButtonChoosesItWhenTheReleaseIsStillOnIt()
	{
		Modal modal = release();
		Rectangle yes = modal.buttonBounds(1);

		assertTrue(modal.onMousePressed(yes.x + 3, yes.y + 3, MouseEvent.BUTTON1, NONE, 0));
		assertTrue(chosen.isEmpty());
		assertTrue(modal.onMouseReleased(yes.x + 4, yes.y + 4));

		assertEquals(List.of(1), chosen);
		assertFalse(modal.isOpen());
	}

	@Test
	public void releasingOffTheButtonChoosesNothing()
	{
		Modal modal = release();
		Rectangle yes = modal.buttonBounds(1);

		modal.onMousePressed(yes.x + 3, yes.y + 3, MouseEvent.BUTTON1, NONE, 0);
		modal.onMouseReleased(5, 5);

		assertTrue(chosen.isEmpty());
		assertTrue(modal.isOpen());
	}

	@Test
	public void anyClickWhileOpenIsConsumedAndOneOnTheDimChoosesNothing()
	{
		Modal modal = release();

		assertTrue(modal.onMousePressed(5, 5, MouseEvent.BUTTON1, NONE, 0));
		assertTrue(modal.onMouseReleased(5, 5));
		assertTrue(modal.onMousePressed(5, 5, MouseEvent.BUTTON3, NONE, 0));
		assertTrue(modal.onMouseMoved(5, 5));

		assertTrue(chosen.isEmpty());
		assertTrue(modal.isOpen());
		modal.close();
		assertFalse(modal.onMousePressed(5, 5, MouseEvent.BUTTON1, NONE, 0));
		assertFalse(modal.onMouseReleased(5, 5));
		assertFalse(modal.onMouseMoved(5, 5));
	}

	@Test
	public void hoveringAButtonSelectsItSoPointerAndKeysShareOneSelection()
	{
		Modal modal = release();
		Rectangle yes = modal.buttonBounds(1);

		modal.onMouseMoved(yes.x + 3, yes.y + 3);

		assertEquals(1, modal.selected());
		modal.onKeyPressed(KeyEvent.VK_LEFT, NONE, 0);
		assertEquals(0, modal.selected());
	}

	@Test
	public void theDialogIsCentredInsideTheAreaWithAllButtonsInsideIt()
	{
		Modal modal = release();
		Rectangle dialog = modal.dialogBounds();

		assertEquals(AREA.x + AREA.width / 2, dialog.x + dialog.width / 2, 1);
		assertEquals(AREA.y + AREA.height / 2, dialog.y + dialog.height / 2, 1);
		assertTrue(AREA.contains(dialog));
		for (int i = 0; i < 2; i++)
			assertTrue(dialog.contains(modal.buttonBounds(i)));

		Rectangle no = modal.buttonBounds(0);
		assertTrue(no.x < modal.buttonBounds(1).x);
		assertNull(modal.buttonBounds(2));
		assertEquals(-1, modal.buttonAt(0, 0));
	}

	@Test
	public void aSmallAreaKeepsTheDialogInsideIt()
	{
		Modal modal = release();
		Rectangle small = new Rectangle(0, 0, 300, 200);

		modal.layout(small, STYLE, WidgetKit.metrics(STYLE, WindowFonts.Face.REGULAR),
			WidgetKit.metrics(STYLE, WindowFonts.Face.BOLD));

		assertTrue(small.contains(modal.dialogBounds()));
	}

	@Test
	public void aLongMessageMakesTheDialogTallerNotWider()
	{
		Modal shortMessage = new Modal("T", "Short", List.of("OK"), 0, chosen::add, () -> cancelled++);
		Modal longMessage = new Modal("T", "A long long message that goes on and on and on and on and on and on "
			+ "and on and on and on and on and on and on and on and on and on", List.of("OK"), 0, chosen::add,
			() -> cancelled++);
		layout(shortMessage);
		layout(longMessage);

		Rectangle tall = longMessage.dialogBounds();
		Rectangle small = shortMessage.dialogBounds();
		assertTrue(tall.height > small.height);
		assertEquals(tall.width, small.width);
	}

	@Test
	public void theTextBoxModeTypesIntoTheBoxAndConsumesTheKeysIt()
	{
		TextBox box = new TextBox();
		Modal modal = rename(box);

		assertTrue(box.focused());
		assertTrue(modal.onKeyTyped('R', NONE, 0));
		assertTrue(modal.onKeyTyped('e', NONE, 0));
		assertTrue(modal.onKeyPressed(KeyEvent.VK_X, NONE, 0));
		assertTrue(modal.uses(KeyEvent.VK_LEFT, NONE));
		assertTrue(modal.onKeyReleased(KeyEvent.VK_X, NONE));
		assertFalse("Alt belongs to RuneLite", modal.onKeyPressed(KeyEvent.VK_X, ALT, 0));
		assertEquals("Re", box.text());
		assertTrue("Left moves the caret, not the buttons", modal.onKeyPressed(KeyEvent.VK_LEFT, NONE, 0));
		assertEquals(1, box.caret());
		assertEquals(1, modal.selected());
	}

	@Test
	public void enterInTheTextBoxChoosesTheConfirmButtonOnlyWhenTheTextIsValid()
	{
		TextBox box = new TextBox();
		Modal modal = rename(box);
		modal.onKeyPressed(KeyEvent.VK_ENTER, NONE, 0);
		assertTrue("empty text is refused", chosen.isEmpty());
		assertTrue(modal.isOpen());

		for (char c : "Rex!".toCharArray())
			modal.onKeyTyped(c, NONE, 0);

		modal.onKeyPressed(KeyEvent.VK_ENTER, NONE, 0);
		assertTrue("a refused name is not saved", chosen.isEmpty());
		modal.onKeyPressed(KeyEvent.VK_BACK_SPACE, NONE, 0);
		modal.onKeyPressed(KeyEvent.VK_ENTER, NONE, 0);

		assertEquals(List.of(1), chosen);
		assertEquals("Rex", box.value());
		assertFalse(modal.isOpen());
	}

	@Test
	public void escapeInTheTextBoxCancelsAndTheDraftIsKept()
	{
		TextBox box = new TextBox();
		Modal modal = rename(box);
		for (char c : "Draft".toCharArray())
			modal.onKeyTyped(c, NONE, 0);

		modal.onKeyPressed(KeyEvent.VK_ESCAPE, NONE, 0);

		assertEquals(1, cancelled);
		assertFalse(modal.isOpen());
		assertFalse("the box lost its focus", box.focused());
		assertEquals("the typed text is kept as a draft", "Draft", box.text());
		assertFalse("so a key listener passes keys on", box.onKeyTyped('x', NONE, 0));
	}

	@Test
	public void closingWhileTypingKeepsTheDraftAndRunsNothing()
	{
		TextBox box = new TextBox();
		Modal modal = rename(box);
		for (char c : "Half".toCharArray())
			modal.onKeyTyped(c, NONE, 0);

		modal.close();

		assertEquals("Half", box.text());
		assertTrue(chosen.isEmpty());
		assertEquals(0, cancelled);
		modal.open(10);
		assertTrue("reopening gives the box focus again", box.focused());
		assertEquals("Half", box.text());
	}

	@Test
	public void clickingTheTextBoxFocusesItAndTheButtonsStillWork()
	{
		TextBox box = new TextBox();
		Modal modal = rename(box);
		Rectangle field = modal.fieldBounds();
		Rectangle save = modal.buttonBounds(1);

		assertTrue(modal.onMousePressed(field.x + 5, field.y + 5, MouseEvent.BUTTON1, NONE, 0));
		assertTrue(modal.onMouseDragged(field.x + 8));
		modal.onMouseReleased(field.x + 5, field.y + 5);
		assertTrue(box.focused());
		assertTrue(modal.isOpen());
		modal.onMousePressed(save.x + 3, save.y + 3, MouseEvent.BUTTON1, NONE, 0);
		modal.onMouseReleased(save.x + 3, save.y + 3);
		assertEquals(List.of(1), chosen);
	}

	@Test
	public void theTextBoxSitsInsideTheDialogAboveTheButtons()
	{
		Modal modal = rename(new TextBox());
		Rectangle field = modal.fieldBounds();

		assertTrue(modal.dialogBounds().contains(field));
		assertTrue(field.y + field.height <= modal.buttonBounds(0).y);
		Modal plain = release();
		assertTrue(plain.fieldBounds().isEmpty());
	}

	@Test
	public void paintDimsTheAreaAndDrawsTheDialogAndTheButtons()
	{
		Modal modal = release();
		BufferedImage image = WidgetKit.canvas(800, 520);
		Graphics2D g = WidgetKit.graphics(image);
		modal.paint(g, STYLE, AREA, MotionMode.OFF, 1_000);
		g.dispose();

		int dimmed = image.getRGB(30, 5);
		assertEquals(STYLE.color(ThemeColor.SCRIM).getRGB(), dimmed);
		Rectangle dialog = modal.dialogBounds();
		assertEquals(STYLE.color(ThemeColor.PANEL_BOX).getRGB(), image.getRGB(dialog.x + dialog.width - 6,
			dialog.y + dialog.height / 2));
		Rectangle yes = modal.buttonBounds(1);
		assertEquals("unselected button", STYLE.color(ThemeColor.CHIP).getRGB(), image.getRGB(yes.x + yes.width / 2,
			yes.y + 4));
		Rectangle no = modal.buttonBounds(0);
		assertEquals("selected button stands out", STYLE.color(ThemeColor.CHIP_ACCENT).getRGB(),
			image.getRGB(no.x + no.width / 2, no.y + 4));
	}

	@Test
	public void aClosedModalPaintsNothing()
	{
		Modal modal = release();
		modal.close();
		BufferedImage image = WidgetKit.canvas(800, 520);
		Graphics2D g = WidgetKit.graphics(image);
		modal.paint(g, STYLE, AREA, MotionMode.FULL, 0);
		g.dispose();

		assertEquals(0, WidgetKit.painted(image, 0, 0, 800, 520));
	}

	@Test
	public void theModalFadesInInFullMotion()
	{
		Modal modal = release();
		BufferedImage start = WidgetKit.canvas(800, 520);
		BufferedImage done = WidgetKit.canvas(800, 520);
		Graphics2D g = WidgetKit.graphics(start);
		modal.paint(g, STYLE, AREA, MotionMode.FULL, 0);
		g.dispose();
		g = WidgetKit.graphics(done);
		modal.paint(g, STYLE, AREA, MotionMode.FULL, 500);
		g.dispose();

		assertTrue((start.getRGB(30, 5) >>> 24) < (done.getRGB(30, 5) >>> 24));
	}

	@Test
	public void theButtonPaintHasItsOwnColoursForDisabledAndShrinksWhilePressed()
	{
		Rectangle2D box = new Rectangle2D.Double(20, 20, 120, 40);
		BufferedImage plain = WidgetKit.canvas(200, 100);
		BufferedImage pressed = WidgetKit.canvas(200, 100);
		BufferedImage disabled = WidgetKit.canvas(200, 100);
		Graphics2D g = WidgetKit.graphics(plain);
		STYLE.button(g, box, "OK", false, false, 0, 0);
		g.dispose();
		g = WidgetKit.graphics(pressed);
		STYLE.button(g, box, "OK", false, false, 0, 1);
		g.dispose();
		g = WidgetKit.graphics(disabled);
		STYLE.button(g, box, "OK", false, true, 1, 1);
		g.dispose();

		assertEquals(STYLE.color(ThemeColor.CHIP).getRGB(), plain.getRGB(80, 24));
		assertEquals(STYLE.color(ThemeColor.TAB_DISABLED).getRGB(), disabled.getRGB(80, 24));
		assertTrue("pressed covers fewer pixels", WidgetKit.painted(pressed, 0, 0, 200, 100)
			< WidgetKit.painted(plain, 0, 0, 200, 100));
	}

	@Test
	public void theTitleAndButtonsAreReported()
	{
		Modal modal = release();

		assertEquals("Release Rex?", modal.title());
		assertEquals(List.of("No", "Yes"), modal.buttons());
	}
}
