/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the text box: editing keys, selection, paste, the limit, validation, focus rules and painting. */
public class TextBoxTest
{
	private static final WindowStyle STYLE = WidgetKit.style(1.0);
	private static final Rectangle BOX = new Rectangle(20, 20, 220, 30);
	private static final int NONE = 0;
	private static final int SHIFT = InputEvent.SHIFT_DOWN_MASK;
	private static final int CTRL = InputEvent.CTRL_DOWN_MASK;
	private static final int ALT = InputEvent.ALT_DOWN_MASK;

	/** A clipboard that lives in a field, so no system clipboard is needed. */
	private static final class FakeClipboard implements TextBox.Clipboard
	{
		private String content;

		@Override
		public String paste()
		{
			return content;
		}

		@Override
		public void copy(String text)
		{
			content = text;
		}
	}

	private final FakeClipboard clipboard = new FakeClipboard();
	private final List<String> submitted = new ArrayList<>();
	private int cancelled;

	private TextBox focused()
	{
		TextBox box = new TextBox(Nickname.MAX_LENGTH, Nickname::clean, clipboard);
		box.onSubmit(submitted::add);
		box.onCancel(() -> cancelled++);
		box.focus(true, 0);
		return box;
	}

	private static void type(TextBox box, String text)
	{
		for (char c : text.toCharArray())
			box.onKeyTyped(c, NONE, 0);
	}

	private static boolean press(TextBox box, int key, int modifiers)
	{
		return box.onKeyPressed(key, modifiers, 0);
	}

	private static void layout(TextBox box)
	{
		FontMetrics fm = WidgetKit.metrics(STYLE, WindowFonts.Face.REGULAR);
		box.layout(BOX, fm, 8);
	}

	@Test
	public void typedCharactersAreInsertedAtTheCaret()
	{
		TextBox box = focused();

		type(box, "Rex");
		assertEquals("Rex", box.text());
		assertEquals(3, box.caret());
		press(box, KeyEvent.VK_LEFT, NONE);
		type(box, "-");
		assertEquals("Re-x", box.text());
		assertEquals(3, box.caret());
	}

	@Test
	public void backspaceAndDeleteRemoveOneCharacterEachWayAndStopAtTheEnds()
	{
		TextBox box = focused();
		type(box, "abcd");
		press(box, KeyEvent.VK_LEFT, NONE);

		press(box, KeyEvent.VK_BACK_SPACE, NONE);
		assertEquals("abd", box.text());
		assertEquals(2, box.caret());
		press(box, KeyEvent.VK_DELETE, NONE);
		assertEquals("ab", box.text());
		press(box, KeyEvent.VK_DELETE, NONE);
		assertEquals("Delete at the end does nothing", "ab", box.text());
		press(box, KeyEvent.VK_HOME, NONE);
		press(box, KeyEvent.VK_BACK_SPACE, NONE);
		assertEquals("Backspace at the start does nothing", "ab", box.text());
		press(box, KeyEvent.VK_DELETE, NONE);
		assertEquals("b", box.text());
		assertEquals(0, box.caret());
	}

	@Test
	public void homeEndAndTheArrowsMoveTheCaretWithinTheText()
	{
		TextBox box = focused();
		type(box, "abc");

		press(box, KeyEvent.VK_HOME, NONE);
		assertEquals(0, box.caret());
		press(box, KeyEvent.VK_LEFT, NONE);
		assertEquals("no further left", 0, box.caret());
		press(box, KeyEvent.VK_RIGHT, NONE);
		assertEquals(1, box.caret());
		press(box, KeyEvent.VK_END, NONE);
		assertEquals(3, box.caret());
		press(box, KeyEvent.VK_RIGHT, NONE);
		assertEquals("no further right", 3, box.caret());
	}

	@Test
	public void shiftWithTheArrowsHomeAndEndExtendsTheSelection()
	{
		TextBox box = focused();
		type(box, "abcdef");

		press(box, KeyEvent.VK_LEFT, SHIFT);
		press(box, KeyEvent.VK_LEFT, SHIFT);
		assertEquals("ef", box.selectedText());
		press(box, KeyEvent.VK_RIGHT, SHIFT);
		assertEquals("f", box.selectedText());
		press(box, KeyEvent.VK_HOME, SHIFT);
		assertEquals("the anchor stays where the selection began", "abcdef", box.selectedText());
		assertEquals(0, box.selectionStart());
		assertEquals(6, box.selectionEnd());
		assertEquals(0, box.caret());
	}

	@Test
	public void anArrowWithoutShiftDropsTheSelectionAtItsEdge()
	{
		TextBox box = focused();
		type(box, "abcdef");
		press(box, KeyEvent.VK_LEFT, SHIFT);
		press(box, KeyEvent.VK_LEFT, SHIFT);
		press(box, KeyEvent.VK_LEFT, SHIFT);

		press(box, KeyEvent.VK_RIGHT, NONE);
		assertEquals("", box.selectedText());
		assertEquals(6, box.caret());
		press(box, KeyEvent.VK_HOME, SHIFT);
		press(box, KeyEvent.VK_LEFT, NONE);
		assertEquals(0, box.caret());
	}

	@Test
	public void typingReplacesTheSelectionAndBackspaceDeletesIt()
	{
		TextBox box = focused();
		type(box, "abcdef");
		press(box, KeyEvent.VK_LEFT, SHIFT);
		press(box, KeyEvent.VK_LEFT, SHIFT);

		type(box, "X");
		assertEquals("abcdX", box.text());
		press(box, KeyEvent.VK_HOME, NONE);
		press(box, KeyEvent.VK_RIGHT, SHIFT);
		press(box, KeyEvent.VK_RIGHT, SHIFT);
		press(box, KeyEvent.VK_BACK_SPACE, NONE);
		assertEquals("cdX", box.text());
		assertEquals(0, box.caret());
		press(box, KeyEvent.VK_END, SHIFT);
		press(box, KeyEvent.VK_DELETE, NONE);
		assertEquals("", box.text());
	}

	@Test
	public void ctrlASelectsEverything()
	{
		TextBox box = focused();
		type(box, "Rex the dog");

		assertTrue(press(box, KeyEvent.VK_A, CTRL));
		assertEquals("Rex the dog", box.selectedText());
		type(box, "Bo");
		assertEquals("Bo", box.text());
	}

	@Test
	public void theTextIsLimitedToTwelveCharacters()
	{
		TextBox box = focused();

		type(box, "abcdefghijklmnop");
		assertEquals("abcdefghijkl", box.text());
		assertEquals(12, box.text().length());
		assertEquals(Nickname.MAX_LENGTH, box.text().length());
		type(box, "z");
		assertEquals("abcdefghijkl", box.text());
	}

	@Test
	public void typingOverASelectionAtTheLimitStillWorks()
	{
		TextBox box = focused();
		type(box, "abcdefghijkl");
		press(box, KeyEvent.VK_LEFT, SHIFT);
		press(box, KeyEvent.VK_LEFT, SHIFT);

		type(box, "XY");

		assertEquals("abcdefghijXY", box.text());
	}

	@Test
	public void pasteInsertsTheClipboardUpToTheLimitAndDropsLineBreaks()
	{
		TextBox box = focused();
		type(box, "Hi ");
		clipboard.content = "there\nfriend of mine, and more";

		assertTrue(press(box, KeyEvent.VK_V, CTRL));

		assertEquals(12, box.text().length());
		assertEquals("Hi therefrie", box.text());
	}

	@Test
	public void pasteReplacesTheSelectionAndIgnoresAnEmptyClipboard()
	{
		TextBox box = focused();
		type(box, "abcdef");
		press(box, KeyEvent.VK_A, CTRL);
		clipboard.content = "new";

		press(box, KeyEvent.VK_V, CTRL);
		assertEquals("new", box.text());
		clipboard.content = null;
		press(box, KeyEvent.VK_V, CTRL);
		assertEquals("new", box.text());
		clipboard.content = "\n\t";
		press(box, KeyEvent.VK_V, CTRL);
		assertEquals("new", box.text());
	}

	@Test
	public void copyAndCutUseTheSelection()
	{
		TextBox box = focused();
		type(box, "abcdef");
		press(box, KeyEvent.VK_LEFT, SHIFT);
		press(box, KeyEvent.VK_LEFT, SHIFT);

		press(box, KeyEvent.VK_C, CTRL);
		assertEquals("ef", clipboard.content);
		assertEquals("abcdef", box.text());
		press(box, KeyEvent.VK_X, CTRL);
		assertEquals("abcd", box.text());
		assertEquals("ef", clipboard.content);
		clipboard.content = "kept";
		press(box, KeyEvent.VK_C, CTRL);
		assertEquals("nothing selected copies nothing", "kept", clipboard.content);
	}

	@Test
	public void theValueIsTheCleanedNicknameAndInvalidTextHasNone()
	{
		TextBox box = focused();

		assertNull("empty is not valid", box.value());
		type(box, "  Rex   Jr ");
		assertEquals("Rex Jr", box.value());
		assertTrue(box.valid());
		press(box, KeyEvent.VK_A, CTRL);
		type(box, "Bad!");
		assertNull(box.value());
		assertFalse(box.valid());
	}

	@Test
	public void enterSubmitsTheCleanedTextOnlyWhenItIsValid()
	{
		TextBox box = focused();
		type(box, "Rex");

		assertTrue(press(box, KeyEvent.VK_ENTER, NONE));
		assertEquals(List.of("Rex"), submitted);
		type(box, "!");
		assertTrue("consumed even when refused", press(box, KeyEvent.VK_ENTER, NONE));
		assertEquals("nothing more was submitted", 1, submitted.size());
	}

	@Test
	public void escapeCallsCancelAndIsConsumed()
	{
		TextBox box = focused();

		assertTrue(press(box, KeyEvent.VK_ESCAPE, NONE));

		assertEquals(1, cancelled);
	}

	@Test
	public void anUnfocusedBoxConsumesNothingAndChangesNothing()
	{
		TextBox box = focused();
		type(box, "Rex");
		box.focus(false, 0);

		assertFalse(box.onKeyTyped('x', NONE, 0));
		assertFalse(press(box, KeyEvent.VK_BACK_SPACE, NONE));
		assertFalse(press(box, KeyEvent.VK_ENTER, NONE));
		assertFalse(press(box, KeyEvent.VK_ESCAPE, NONE));
		assertFalse(press(box, KeyEvent.VK_LEFT, NONE));
		assertFalse(box.onKeyReleased(KeyEvent.VK_LEFT, NONE));
		assertFalse(box.uses(KeyEvent.VK_A, NONE));
		assertEquals("Rex", box.text());
		assertTrue(submitted.isEmpty());
		assertEquals(0, cancelled);
	}

	@Test
	public void aFocusedBoxTakesEveryKeyAsTypingSoNoHotkeyFires()
	{
		TextBox box = focused();

		assertTrue(press(box, KeyEvent.VK_Q, NONE));
		assertTrue(press(box, KeyEvent.VK_1, NONE));
		assertTrue(press(box, KeyEvent.VK_UP, NONE));
		assertTrue(press(box, KeyEvent.VK_F5, NONE));
		assertTrue(box.onKeyReleased(KeyEvent.VK_Q, NONE));
		assertTrue(box.onKeyTyped('q', NONE, 0));
		assertTrue(box.onKeyTyped('\n', NONE, 0));
		assertEquals("only the typed event inserts", "q", box.text());
	}

	@Test
	public void altCtrlKeysWithoutAUseAndLoneModifierKeysPassThrough()
	{
		TextBox box = focused();

		assertFalse("Alt pressed", press(box, KeyEvent.VK_A, ALT));
		assertFalse("Alt typed", box.onKeyTyped('a', ALT, 0));
		assertFalse("Alt released", box.onKeyReleased(KeyEvent.VK_A, ALT));
		assertFalse("Ctrl+Z has no use", press(box, KeyEvent.VK_Z, CTRL));
		assertFalse("Ctrl+Z typed char", box.onKeyTyped('\u001a', CTRL, 0));
		assertFalse(press(box, KeyEvent.VK_SHIFT, SHIFT));
		assertFalse(press(box, KeyEvent.VK_CONTROL, CTRL));
		assertFalse(press(box, KeyEvent.VK_ALT, ALT));
		assertTrue("the Ctrl+V character is consumed", box.onKeyTyped('\u0016', CTRL, 0));
		assertEquals("", box.text());
	}

	@Test
	public void controlCharactersAreNeverInserted()
	{
		TextBox box = focused();

		box.onKeyTyped('\b', NONE, 0);
		box.onKeyTyped('\u007f', NONE, 0);
		box.onKeyTyped('\t', NONE, 0);
		box.onKeyTyped(KeyEvent.CHAR_UNDEFINED, NONE, 0);

		assertEquals("", box.text());
	}

	@Test
	public void aDraftSurvivesLosingFocusAndASetTextIsCutToTheLimit()
	{
		TextBox box = focused();
		type(box, "Draft");
		box.focus(false, 0);

		assertEquals("Draft", box.text());
		box.setText("abcdefghijklmnopq\n");
		assertEquals("abcdefghijkl", box.text());
		assertEquals(12, box.caret());
		box.setText(null);
		assertEquals("", box.text());
	}

	@Test
	public void aPressInsideFocusesAndPlacesTheCaretUnderThePointer()
	{
		TextBox box = new TextBox(Nickname.MAX_LENGTH, Nickname::clean, clipboard);
		box.setText("abcdef");
		layout(box);
		FontMetrics fm = WidgetKit.metrics(STYLE, WindowFonts.Face.REGULAR);
		int afterThree = BOX.x + 8 + fm.stringWidth("abc");

		assertTrue(box.onMousePressed(afterThree + 1, 30, MouseEvent.BUTTON1, NONE, 0));

		assertTrue(box.focused());
		assertEquals(3, box.caret());
		assertEquals(0, box.selectionEnd() - box.selectionStart());
		box.onMousePressed(BOX.x, 30, MouseEvent.BUTTON1, NONE, 0);
		assertEquals(0, box.caret());
		box.onMousePressed(BOX.x + BOX.width - 1, 30, MouseEvent.BUTTON1, NONE, 0);
		assertEquals("past the text goes to the end", 6, box.caret());
	}

	@Test
	public void dragSelectsAndShiftClickExtends()
	{
		TextBox box = new TextBox(Nickname.MAX_LENGTH, Nickname::clean, clipboard);
		box.setText("abcdef");
		layout(box);
		FontMetrics fm = WidgetKit.metrics(STYLE, WindowFonts.Face.REGULAR);
		int at1 = BOX.x + 8 + fm.stringWidth("a");
		int at4 = BOX.x + 8 + fm.stringWidth("abcd");

		box.onMousePressed(at1, 30, MouseEvent.BUTTON1, NONE, 0);
		assertTrue(box.onMouseDragged(at4));
		box.onMouseReleased();
		assertEquals("bcd", box.selectedText());
		assertFalse("no drag after the release", box.onMouseDragged(at1));
		box.onMousePressed(BOX.x + 8 + fm.stringWidth("abcdef"), 30, MouseEvent.BUTTON1, SHIFT, 0);
		assertEquals("shift extends from the old anchor", "bcdef", box.selectedText());
	}

	@Test
	public void aPressOutsideDropsFocusButKeepsTheTextAndIsNotConsumed()
	{
		TextBox box = focused();
		type(box, "Rex");
		layout(box);

		assertFalse(box.onMousePressed(5, 5, MouseEvent.BUTTON1, NONE, 0));

		assertFalse(box.focused());
		assertEquals("Rex", box.text());
		assertFalse(box.onKeyTyped('x', NONE, 0));
	}

	@Test
	public void aTextTooLongForTheBoxScrollsSoTheCaretStaysInside()
	{
		TextBox box = focused();
		type(box, "WWWWWWWWWWWW");
		BufferedImage image = WidgetKit.canvas(260, 80);
		Graphics2D g = WidgetKit.graphics(image);
		box.paint(g, STYLE, new Rectangle(20, 20, 140, 30), MotionMode.OFF, 0);
		g.dispose();

		assertTrue(WidgetKit.painted(image, 20, 20, 140, 30) > 0);
		assertEquals("nothing paints outside the box", 0, WidgetKit.painted(image, 161, 0, 99, 80));
		assertEquals(0, WidgetKit.painted(image, 0, 0, 260, 19));
	}

	@Test
	public void paintShowsTheFieldAndOutlineByStateAndHidesTheCaretBetweenBlinks()
	{
		TextBox box = new TextBox(Nickname.MAX_LENGTH, Nickname::clean, clipboard);
		BufferedImage idle = WidgetKit.canvas(260, 80);
		Graphics2D g = WidgetKit.graphics(idle);
		box.paint(g, STYLE, BOX, MotionMode.FULL, 0);
		g.dispose();
		assertEquals("unfocused outline", STYLE.color(ThemeColor.PANEL_BORDER).getRGB(),
			idle.getRGB(BOX.x + BOX.width / 2, BOX.y));

		box.focus(true, 0);
		BufferedImage active = WidgetKit.canvas(260, 80);
		g = WidgetKit.graphics(active);
		box.paint(g, STYLE, BOX, MotionMode.FULL, 0);
		g.dispose();
		assertEquals("focused outline", STYLE.color(ThemeColor.ACCENT).getRGB(),
			active.getRGB(BOX.x + BOX.width / 2, BOX.y));

		type(box, "Bad!");
		BufferedImage refused = WidgetKit.canvas(260, 80);
		g = WidgetKit.graphics(refused);
		box.paint(g, STYLE, BOX, MotionMode.FULL, 0);
		g.dispose();
		assertEquals("refused outline", STYLE.color(ThemeColor.HP_DANGER).getRGB(),
			refused.getRGB(BOX.x + BOX.width / 2, BOX.y));
	}

	@Test
	public void theCaretBlinksInFullMotionAndStaysSolidInOffAndReduced()
	{
		TextBox box = focused();
		box.focus(true, 0);
		int on = caretPixels(box, MotionMode.FULL, 100);
		int off = caretPixels(box, MotionMode.FULL, 600);
		int offMode = caretPixels(box, MotionMode.OFF, 600);
		int reduced = caretPixels(box, MotionMode.REDUCED, 600);

		assertTrue("caret on", on > 0);
		assertEquals("caret off half a round later", 0, off);
		assertEquals(on, offMode);
		assertEquals(on, reduced);
		assertEquals("an edit restarts the blink", on, caretPixelsAfterEdit(box));
	}

	private int caretPixelsAfterEdit(TextBox box)
	{
		box.onKeyPressed(KeyEvent.VK_END, NONE, 550);
		return caretPixels(box, MotionMode.FULL, 600);
	}

	private int caretPixels(TextBox box, MotionMode mode, long now)
	{
		BufferedImage image = WidgetKit.canvas(260, 80);
		Graphics2D g = WidgetKit.graphics(image);
		box.paint(g, STYLE, BOX, mode, now);
		g.dispose();
		int from = BOX.x + STYLE.metrics().pad;
		return countColour(image, STYLE.color(ThemeColor.TEXT).getRGB(), from, from + 3);
	}

	private static int countColour(BufferedImage image, int rgb, int fromX, int toX)
	{
		int count = 0;
		for (int x = fromX; x < toX; x++)
			for (int y = BOX.y; y < BOX.y + BOX.height; y++)
				if (image.getRGB(x, y) == rgb)
					count++;

		return count;
	}

	@Test
	public void theBoxHasItsAreaAfterLayout()
	{
		TextBox box = new TextBox();
		layout(box);

		assertEquals(BOX, box.bounds());
		assertEquals(12, Nickname.MAX_LENGTH);
	}
}
