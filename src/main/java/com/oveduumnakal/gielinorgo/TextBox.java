/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A one-line text box drawn in the window, for renaming a creature: a caret that blinks, a selection, Left, Right, Home
 * and End (Shift extends the selection), Backspace, Delete, Ctrl+A, Ctrl+C, Ctrl+X and Ctrl+V (paste), a limit of
 * {@link Nickname#MAX_LENGTH} characters, and validation through {@link Nickname#clean}, so the rule lives in one
 * place. Typing takes any printable character; a text the rule refuses shows a red outline and Enter then does nothing.
 *
 * <p>The box consumes keys only while it is focused. Focused, it takes every key as typing: the keys it uses, and also
 * the others (so a hotkey cannot fire while the player types), except keys held with Alt, Ctrl keys it has no use for
 * and the lone modifier keys. Enter hands the cleaned text to {@link #onSubmit}, Esc calls {@link #onCancel}. The
 * input methods take plain values, so a key listener passes {@link KeyEvent} details straight in. The box keeps its
 * text and caret when it loses focus, so a draft survives. Holds no RuneLite types.
 */
final class TextBox
{
	private static final char CTRL_A = 1;
	private static final char CTRL_C = 3;
	private static final char CTRL_V = 22;
	private static final char CTRL_X = 24;

	private final StringBuilder text = new StringBuilder();
	private final int maxLength;
	private final Function<String, String> cleaner;
	private final Clipboard clipboard;
	private int caret;
	private int anchor;
	private boolean focused;
	private long editedMs;
	private Rectangle bounds = new Rectangle();
	private FontMetrics metrics;
	private int pad;
	private int scrollX;
	private boolean selecting;
	private String placeholder = "";
	private Consumer<String> onSubmit = s ->
	{
	};
	private Runnable onCancel = () ->
	{
	};

	/** Where Ctrl+C, Ctrl+X and Ctrl+V go to and come from. */
	interface Clipboard
	{
		/** @return the text on the clipboard, or null when there is none or it cannot be read */
		String paste();

		/**
		 * Puts a text on the clipboard.
		 *
		 * @param text the text
		 */
		void copy(String text);
	}

	/**
	 * Makes a text box for a nickname: {@link Nickname#MAX_LENGTH} characters, checked by {@link Nickname#clean}, with
	 * the system clipboard.
	 */
	TextBox()
	{
		this(Nickname.MAX_LENGTH, Nickname::clean, system());
	}

	/**
	 * Makes a text box.
	 *
	 * @param maxLength the most characters it holds
	 * @param cleaner turns the text into the value to save, or null when the text is not valid
	 * @param clipboard where copy and paste go; a test passes its own
	 */
	TextBox(int maxLength, Function<String, String> cleaner, Clipboard clipboard)
	{
		this.maxLength = maxLength;
		this.cleaner = cleaner;
		this.clipboard = clipboard;
	}

	/** @return the system clipboard, which does nothing when it cannot be reached, as in a headless run */
	static Clipboard system()
	{
		return new Clipboard()
		{
			@Override
			public String paste()
			{
				try
				{
					return (String) Toolkit.getDefaultToolkit()
						.getSystemClipboard()
						.getData(DataFlavor.stringFlavor);
				}
				catch (Exception | LinkageError e)
				{
					return null;
				}
			}

			@Override
			public void copy(String text)
			{
				try
				{
					Toolkit.getDefaultToolkit()
						.getSystemClipboard()
						.setContents(new StringSelection(text), null);
				}
				catch (Exception | LinkageError e)
				{
					return;
				}
			}
		};
	}

	/**
	 * Sets what Enter does.
	 *
	 * @param handler gets the cleaned text; not called while the text is not valid
	 */
	void onSubmit(Consumer<String> handler)
	{
		onSubmit = handler;
	}

	/**
	 * Sets what Esc does.
	 *
	 * @param handler runs when Esc goes down in the focused box
	 */
	void onCancel(Runnable handler)
	{
		onCancel = handler;
	}

	/**
	 * @param hint what to show, dim, while the box is empty
	 */
	void placeholder(String hint)
	{
		placeholder = hint == null ? "" : hint;
	}

	/** @return the text as typed */
	String text()
	{
		return text.toString();
	}

	/**
	 * Replaces the text, for a draft kept from an earlier open. The text is cut to the limit and the caret goes to
	 * the end with nothing selected.
	 *
	 * @param value the text
	 */
	void setText(String value)
	{
		text.setLength(0);
		String clean = value == null ? "" : strip(value);
		text.append(clean, 0, Math.min(clean.length(), maxLength));
		caret = text.length();
		anchor = caret;
	}

	/** @return the value to save, or null when the text is not valid */
	String value()
	{
		return cleaner.apply(text.toString());
	}

	/** @return whether the text is valid, so Enter would save it */
	boolean valid()
	{
		return value() != null;
	}

	/** @return the caret's place, from 0 to the length of the text */
	int caret()
	{
		return caret;
	}

	/** @return the start of the selection, or the caret's place when nothing is selected */
	int selectionStart()
	{
		return Math.min(caret, anchor);
	}

	/** @return the end of the selection, or the caret's place when nothing is selected */
	int selectionEnd()
	{
		return Math.max(caret, anchor);
	}

	/** @return the selected text, empty when nothing is selected */
	String selectedText()
	{
		return text.substring(selectionStart(), selectionEnd());
	}

	/** @return whether the box has keyboard focus */
	boolean focused()
	{
		return focused;
	}

	/**
	 * Gives or takes the focus. Losing it keeps the text.
	 *
	 * @param on whether the box is focused
	 * @param nowMs the time in milliseconds, so the caret starts solid
	 */
	void focus(boolean on, long nowMs)
	{
		focused = on;
		editedMs = nowMs;
		selecting = false;
	}

	/** @return the box's area, by its final place; set by {@link #layout} or a paint */
	Rectangle bounds()
	{
		return new Rectangle(bounds);
	}

	/**
	 * Sets where the box is and the font its text is measured in, so the pointer can place the caret.
	 *
	 * @param box the box's area
	 * @param fm the measures of {@link #font}
	 * @param padding the space inside the box at the left and right
	 */
	void layout(Rectangle box, FontMetrics fm, int padding)
	{
		bounds = new Rectangle(box);
		metrics = fm;
		pad = padding;
		scrollToCaret();
	}

	/**
	 * @param style the style
	 * @return the font the box uses, a whole multiple of the regular font's native size
	 */
	static Font font(WindowStyle style)
	{
		return style.font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS);
	}

	/**
	 * Tells whether the box would consume a key, so a key listener can consume its pressed, typed and released events
	 * together.
	 *
	 * @param keyCode the key, as {@link KeyEvent#getKeyCode}
	 * @param modifiers the modifiers, as {@link InputEvent#getModifiersEx}
	 * @return whether the box is focused and takes the key
	 */
	boolean uses(int keyCode, int modifiers)
	{
		return focused && takes(keyCode, modifiers);
	}

	/**
	 * Tells whether a focused box takes a key, whatever box it is. The key listener decides on the UI thread from
	 * this and a published flag that a box is focused, without touching the box.
	 *
	 * @param keyCode the key, as {@link KeyEvent#getKeyCode}
	 * @param modifiers the modifiers, as {@link InputEvent#getModifiersEx}
	 * @return false for Alt combinations and for the modifier keys themselves, and for Ctrl combinations other than
	 *         select all, copy, cut, paste, the arrows, Home, End, Backspace and Delete; true for everything else
	 */
	static boolean takes(int keyCode, int modifiers)
	{
		if (alt(modifiers) || isModifierKey(keyCode))
			return false;

		return !ctrl(modifiers) || keyCode == KeyEvent.VK_A || keyCode == KeyEvent.VK_C || keyCode == KeyEvent.VK_X
			|| keyCode == KeyEvent.VK_V || keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT
			|| keyCode == KeyEvent.VK_HOME || keyCode == KeyEvent.VK_END || keyCode == KeyEvent.VK_BACK_SPACE
			|| keyCode == KeyEvent.VK_DELETE;
	}

	/**
	 * Handles a key going down.
	 *
	 * @param keyCode the key, as {@link KeyEvent#getKeyCode}
	 * @param modifiers the modifiers, as {@link InputEvent#getModifiersEx}
	 * @param nowMs the time in milliseconds, so the caret restarts solid
	 * @return whether the key was consumed; always false while the box is not focused
	 */
	boolean onKeyPressed(int keyCode, int modifiers, long nowMs)
	{
		if (!uses(keyCode, modifiers))
			return false;

		editedMs = nowMs;
		boolean shift = (modifiers & InputEvent.SHIFT_DOWN_MASK) != 0;
		boolean control = ctrl(modifiers);
		switch (keyCode)
		{
			case KeyEvent.VK_LEFT:
				moveCaret(hasSelection() && !shift ? selectionStart() : caret - 1, shift);
				break;
			case KeyEvent.VK_RIGHT:
				moveCaret(hasSelection() && !shift ? selectionEnd() : caret + 1, shift);
				break;
			case KeyEvent.VK_HOME:
				moveCaret(0, shift);
				break;
			case KeyEvent.VK_END:
				moveCaret(text.length(), shift);
				break;
			case KeyEvent.VK_BACK_SPACE:
				backspace();
				break;
			case KeyEvent.VK_DELETE:
				delete();
				break;
			case KeyEvent.VK_ENTER:
				submit();
				break;
			case KeyEvent.VK_ESCAPE:
				onCancel.run();
				break;
			case KeyEvent.VK_A:
				if (control)
				{
					anchor = 0;
					caret = text.length();
				}

				break;
			case KeyEvent.VK_C:
				if (control)
					copy(false);

				break;
			case KeyEvent.VK_X:
				if (control)
					copy(true);

				break;
			case KeyEvent.VK_V:
				if (control)
					insert(clipboard.paste());

				break;
			default:
				break;
		}

		scrollToCaret();
		return true;
	}

	/**
	 * Handles a character typed.
	 *
	 * @param c the character, as {@link KeyEvent#getKeyChar}
	 * @param modifiers the modifiers, as {@link InputEvent#getModifiersEx}
	 * @param nowMs the time in milliseconds, so the caret restarts solid
	 * @return whether the event was consumed: true for any character while focused, except with Alt held and with
	 *         Ctrl held for anything but the characters of Ctrl+A, C, V and X; only a printable character with no
	 *         Ctrl changes the text
	 */
	boolean onKeyTyped(char c, int modifiers, long nowMs)
	{
		if (!focused || !takesTyped(c, modifiers))
			return false;

		if (ctrl(modifiers))
			return true;

		if (c >= 32 && c != 127 && c != KeyEvent.CHAR_UNDEFINED)
		{
			editedMs = nowMs;
			insert(String.valueOf(c));
			scrollToCaret();
		}

		return true;
	}

	/**
	 * Tells whether a focused box takes a typed character, whatever box it is, for the key listener on the UI thread.
	 *
	 * @param c the character, as {@link KeyEvent#getKeyChar}
	 * @param modifiers the modifiers, as {@link InputEvent#getModifiersEx}
	 * @return false with Alt held, and with Ctrl held for anything but the characters of Ctrl+A, C, V and X
	 */
	static boolean takesTyped(char c, int modifiers)
	{
		if (alt(modifiers))
			return false;

		return !ctrl(modifiers) || c == CTRL_A || c == CTRL_C || c == CTRL_V || c == CTRL_X;
	}

	/**
	 * Handles a key going up.
	 *
	 * @param keyCode the key, as {@link KeyEvent#getKeyCode}
	 * @param modifiers the modifiers, as {@link InputEvent#getModifiersEx}
	 * @return whether the event was consumed, which pairs with {@link #uses}
	 */
	boolean onKeyReleased(int keyCode, int modifiers)
	{
		return uses(keyCode, modifiers);
	}

	/**
	 * Handles a mouse button going down. A press inside the box focuses it and puts the caret under the pointer
	 * (Shift extends the selection); a press outside takes the focus away and is not consumed here.
	 *
	 * @param x the pointer's x
	 * @param y the pointer's y
	 * @param button the button, as {@link MouseEvent#getButton}
	 * @param modifiers the modifiers, as {@link InputEvent#getModifiersEx}
	 * @param nowMs the time in milliseconds
	 * @return whether the press was inside the box and is consumed
	 */
	boolean onMousePressed(int x, int y, int button, int modifiers, long nowMs)
	{
		if (!bounds.contains(x, y))
		{
			focus(false, nowMs);
			return false;
		}

		boolean wasFocused = focused;
		focus(true, nowMs);
		if (button == MouseEvent.BUTTON1)
		{
			int at = indexAt(x);
			caret = at;
			if ((modifiers & InputEvent.SHIFT_DOWN_MASK) == 0 || !wasFocused)
				anchor = at;

			selecting = true;
		}

		return true;
	}

	/**
	 * Handles the pointer moving with the button down: it drags out a selection.
	 *
	 * @param x the pointer's x
	 * @return whether a selection is being dragged
	 */
	boolean onMouseDragged(int x)
	{
		if (!selecting)
			return false;

		caret = indexAt(x);
		scrollToCaret();
		return true;
	}

	/** Handles the button going up. */
	void onMouseReleased()
	{
		selecting = false;
	}

	/**
	 * Finds the caret place nearest to an x.
	 *
	 * @param x the x on screen
	 * @return the place from 0 to the length of the text
	 */
	int indexAt(int x)
	{
		if (metrics == null)
			return text.length();

		int target = x - bounds.x - pad + scrollX;
		int best = 0;
		int bestDistance = Math.abs(target);
		for (int i = 1; i <= text.length(); i++)
		{
			int distance = Math.abs(target - metrics.stringWidth(text.substring(0, i)));
			if (distance < bestDistance)
			{
				best = i;
				bestDistance = distance;
			}
		}

		return best;
	}

	/**
	 * Paints the box: the field, its outline (accent when focused, red when the text is refused), the selection,
	 * the text, the caret and a character count.
	 *
	 * @param g the graphics
	 * @param style the style
	 * @param box the box's area
	 * @param mode the motion mode; the caret blinks only in full motion
	 * @param nowMs the time in milliseconds
	 */
	void paint(Graphics2D g, WindowStyle style, Rectangle box, MotionMode mode, long nowMs)
	{
		Font font = font(style);
		FontMetrics fm = g.getFontMetrics(font);
		layout(box, fm, style.metrics().pad);
		Rectangle2D field = new Rectangle2D.Double(box.x, box.y, box.width, box.height);
		g.setColor(style.color(ThemeColor.INPUT));
		g.fill(style.outline(WindowStyle.Kind.FIELD, field));
		boolean refused = !text.toString().isEmpty() && !valid();
		ThemeColor outline = refused ? ThemeColor.HP_DANGER : focused ? ThemeColor.ACCENT : ThemeColor.PANEL_BORDER;
		g.setColor(style.color(outline));
		g.setStroke(new BasicStroke(style.metrics().border));
		g.draw(style.outline(WindowStyle.Kind.FIELD, new Rectangle2D.Double(box.x + style.metrics().border / 2.0,
			box.y + style.metrics().border / 2.0, box.width - style.metrics().border,
			box.height - style.metrics().border)));
		Font small = style.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		FontMetrics smallFm = g.getFontMetrics(small);
		String count = text.length() + "/" + maxLength;
		int countWidth = smallFm.stringWidth(count) + pad;
		int baseline = box.y + (box.height + fm.getAscent() - fm.getDescent()) / 2;
		style.text(g, count, box.x + box.width - countWidth, box.y + (box.height + smallFm.getAscent()
			- smallFm.getDescent()) / 2, small, ThemeColor.SUBTEXT);
		Shape saved = g.getClip();
		int textRight = box.x + box.width - countWidth - pad / 2;
		g.clipRect(box.x + pad, box.y, Math.max(1, textRight - box.x - pad), box.height);
		int originX = box.x + pad - scrollX;
		if (hasSelection())
		{
			int from = fm.stringWidth(text.substring(0, selectionStart()));
			int to = fm.stringWidth(text.substring(0, selectionEnd()));
			g.setColor(style.color(ThemeColor.SELECTION));
			g.fillRect(originX + from, box.y + style.metrics().border, to - from,
				box.height - 2 * style.metrics().border);
		}

		if (text.length() == 0 && !focused)
			style.text(g, placeholder, originX, baseline, font, ThemeColor.SUBTEXT);
		else
			style.text(g, text.toString(), originX, baseline, font, ThemeColor.TEXT);

		if (focused && (!mode.loops() || Loop.blink(nowMs - editedMs, WindowTimes.CARET_BLINK)))
		{
			int caretX = originX + fm.stringWidth(text.substring(0, caret));
			g.setColor(style.color(ThemeColor.TEXT));
			g.fillRect(caretX, box.y + style.metrics().border * 2, Math.max(1, style.metrics().border),
				box.height - 4 * style.metrics().border);
		}

		g.setClip(saved);
	}

	private boolean hasSelection()
	{
		return caret != anchor;
	}

	private void moveCaret(int to, boolean extend)
	{
		caret = Math.max(0, Math.min(text.length(), to));
		if (!extend)
			anchor = caret;
	}

	private void backspace()
	{
		if (hasSelection())
		{
			deleteSelection();
			return;
		}

		if (caret > 0)
		{
			text.deleteCharAt(caret - 1);
			caret--;
			anchor = caret;
		}
	}

	private void delete()
	{
		if (hasSelection())
		{
			deleteSelection();
			return;
		}

		if (caret < text.length())
			text.deleteCharAt(caret);
	}

	private void deleteSelection()
	{
		int from = selectionStart();
		text.delete(from, selectionEnd());
		caret = from;
		anchor = from;
	}

	private void insert(String raw)
	{
		if (raw == null)
			return;

		String clean = strip(raw);
		int room = maxLength - (text.length() - (selectionEnd() - selectionStart()));
		if (clean.isEmpty() || room <= 0)
			return;

		String part = clean.substring(0, Math.min(room, clean.length()));
		int from = selectionStart();
		text.replace(from, selectionEnd(), part);
		caret = from + part.length();
		anchor = caret;
	}

	private void copy(boolean cut)
	{
		if (!hasSelection())
			return;

		clipboard.copy(selectedText());
		if (cut)
			deleteSelection();
	}

	private void submit()
	{
		String value = value();
		if (value != null)
			onSubmit.accept(value);
	}

	private void scrollToCaret()
	{
		if (metrics == null)
			return;

		int room = Math.max(1, bounds.width - 2 * pad - metrics.stringWidth(maxLength + "/" + maxLength) - pad);
		int at = metrics.stringWidth(text.substring(0, caret));
		if (at - scrollX > room)
			scrollX = at - room;

		if (at - scrollX < 0)
			scrollX = at;

		int whole = metrics.stringWidth(text.toString());
		if (whole - scrollX < room)
			scrollX = Math.max(0, whole - room);
	}

	private static String strip(String raw)
	{
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < raw.length(); i++)
		{
			char c = raw.charAt(i);
			if (c >= 32 && c != 127)
				out.append(c);
		}

		return out.toString();
	}

	private static boolean ctrl(int modifiers)
	{
		return (modifiers & (InputEvent.CTRL_DOWN_MASK | InputEvent.META_DOWN_MASK)) != 0;
	}

	private static boolean alt(int modifiers)
	{
		return (modifiers & (InputEvent.ALT_DOWN_MASK | InputEvent.ALT_GRAPH_DOWN_MASK)) != 0;
	}

	private static boolean isModifierKey(int keyCode)
	{
		return keyCode == KeyEvent.VK_SHIFT || keyCode == KeyEvent.VK_CONTROL || keyCode == KeyEvent.VK_ALT
			|| keyCode == KeyEvent.VK_META || keyCode == KeyEvent.VK_ALT_GRAPH || keyCode == KeyEvent.VK_CAPS_LOCK;
	}
}
