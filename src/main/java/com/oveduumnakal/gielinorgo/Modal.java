/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * A dialog drawn inside the window, for a confirmation such as Release or Trade, or for a text box such as Rename. It
 * dims the window behind it, shows a title, a message that wraps, an optional {@link TextBox} and a row of buttons.
 * While open it consumes every click, so nothing behind it can be pressed, and a click on a button chooses it.
 *
 * <p>Keys (the window must have focus; the caller routes them here first while the modal is open). Without a text
 * box: Left and Right move between the buttons and wrap (the default is the first, so a Release dialog starts on No),
 * Enter or Space chooses the selected button, Esc or Backspace cancels. With a text box the box owns the keys, as
 * a rename box takes every key while it is focused: Enter chooses the confirm button when the text is valid, and Esc
 * cancels. Choosing or cancelling closes the modal first and then calls back, so a callback may open another modal.
 * Hit rectangles are the final ones. Holds no RuneLite types.
 */
final class Modal
{
	private final String title;
	private final String message;
	private final List<String> buttons;
	private final IntConsumer onChoice;
	private final Runnable onCancel;
	private TextBox field;
	private int confirmIndex;
	private boolean open;
	private int selected;
	private int hovered = -1;
	private int pressed = -1;
	private long openedMs;
	private Rectangle dialog = new Rectangle();
	private Rectangle fieldBox = new Rectangle();
	private List<Rectangle> buttonBoxes = Collections.emptyList();

	/**
	 * Makes a modal; it is closed until {@link #open}.
	 *
	 * @param title the heading
	 * @param message the text under it; line breaks are kept and long lines wrap
	 * @param buttons the button labels, left to right; at least one
	 * @param defaultIndex the button selected first, such as the index of No in a Release dialog
	 * @param onChoice called with the index of the chosen button, after the modal has closed
	 * @param onCancel called when Esc cancels, after the modal has closed
	 */
	Modal(String title, String message, List<String> buttons, int defaultIndex, IntConsumer onChoice,
		Runnable onCancel)
	{
		this.title = title;
		this.message = message == null ? "" : message;
		this.buttons = Collections.unmodifiableList(new ArrayList<>(buttons));
		this.selected = Math.max(0, Math.min(this.buttons.size() - 1, defaultIndex));
		this.onChoice = onChoice;
		this.onCancel = onCancel;
	}

	/**
	 * Adds a text box to the modal, which takes the keys while the modal is open. Enter chooses a button, but only
	 * while the text is valid, and Esc cancels.
	 *
	 * @param box the text box
	 * @param confirm the index of the button Enter chooses
	 * @return this modal
	 */
	Modal withField(TextBox box, int confirm)
	{
		this.field = box;
		this.confirmIndex = confirm;
		box.onSubmit(value -> choose(confirmIndex));
		box.onCancel(this::cancel);
		return this;
	}

	/**
	 * Opens the modal. A text box gets the focus.
	 *
	 * @param nowMs the time in milliseconds
	 */
	void open(long nowMs)
	{
		open = true;
		openedMs = nowMs;
		hovered = -1;
		pressed = -1;
		if (field != null)
			field.focus(true, nowMs);
	}

	/** Closes the modal without calling back; a text box keeps its text, so a draft survives. */
	void close()
	{
		open = false;
		pressed = -1;
		if (field != null)
			field.focus(false, 0);
	}

	/** @return whether the modal is open */
	boolean isOpen()
	{
		return open;
	}

	/** @return the heading */
	String title()
	{
		return title;
	}

	/** @return the button labels */
	List<String> buttons()
	{
		return buttons;
	}

	/** @return the index of the selected button, the one Enter chooses */
	int selected()
	{
		return selected;
	}

	/**
	 * Chooses a button: closes the modal, then calls back.
	 *
	 * @param index the button
	 */
	void choose(int index)
	{
		if (!open || index < 0 || index >= buttons.size())
			return;

		close();
		onChoice.accept(index);
	}

	/** Cancels: closes the modal, then calls back. */
	void cancel()
	{
		if (!open)
			return;

		close();
		onCancel.run();
	}

	/**
	 * @param style the style
	 * @return the font of the message and the text box
	 */
	static Font font(WindowStyle style)
	{
		return style.font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS);
	}

	/**
	 * @param style the style
	 * @return the font of the title and the buttons
	 */
	static Font boldFont(WindowStyle style)
	{
		return style.font(WindowFonts.Face.BOLD, WindowFonts.BODY_UNITS);
	}

	/**
	 * Works the boxes out: the dialog centred in the area, then the title, message, text box and buttons.
	 *
	 * @param area the area the modal dims and centres in, normally the window
	 * @param style the style
	 * @param regular the measures of {@link #font}
	 * @param bold the measures of {@link #boldFont}
	 */
	void layout(Rectangle area, WindowStyle style, FontMetrics regular, FontMetrics bold)
	{
		int pad = style.metrics().pad;
		int widest = 0;
		for (String label : buttons)
			widest = Math.max(widest, bold.stringWidth(label));

		int buttonWidth = widest + 4 * pad;
		int buttonsWidth = buttons.size() * buttonWidth + (buttons.size() - 1) * pad;
		int width = Math.min(area.width - 2 * pad, Math.max(style.px(420), buttonsWidth + 2 * pad));
		TextLayout text = TextLayout.plain(message, regular, width - 2 * pad, 0);
		int buttonHeight = bold.getHeight() + pad;
		int fieldHeight = field == null ? 0 : regular.getHeight() + pad;
		int height = pad + bold.getHeight() + pad + text.height() + pad + (field == null ? 0 : fieldHeight + pad)
			+ buttonHeight + pad;
		dialog = new Rectangle(area.x + (area.width - width) / 2, area.y + (area.height - height) / 2, width,
			Math.min(height, area.height));
		int y = dialog.y + pad + bold.getHeight() + pad + text.height() + pad;
		fieldBox = field == null ? new Rectangle() : new Rectangle(dialog.x + pad, y, width - 2 * pad, fieldHeight);
		if (field != null)
		{
			y += fieldHeight + pad;
			field.layout(fieldBox, regular, pad);
		}

		List<Rectangle> list = new ArrayList<>();
		int x = dialog.x + dialog.width - pad - buttonsWidth;
		for (int i = 0; i < buttons.size(); i++)
			list.add(new Rectangle(x + i * (buttonWidth + pad), y, buttonWidth, buttonHeight));

		buttonBoxes = list;
	}

	/** @return the dialog's box, by its final place */
	Rectangle dialogBounds()
	{
		return new Rectangle(dialog);
	}

	/**
	 * @param index the button
	 * @return the button's box, or null when there is no such button
	 */
	Rectangle buttonBounds(int index)
	{
		return index < 0 || index >= buttonBoxes.size() ? null : new Rectangle(buttonBoxes.get(index));
	}

	/** @return the text box's area, or an empty box when the modal has none */
	Rectangle fieldBounds()
	{
		return new Rectangle(fieldBox);
	}

	/**
	 * Finds the button under a point.
	 *
	 * @param x the point's x
	 * @param y the point's y
	 * @return the button's index, or -1
	 */
	int buttonAt(int x, int y)
	{
		for (int i = 0; i < buttonBoxes.size(); i++)
			if (buttonBoxes.get(i).contains(x, y))
				return i;

		return -1;
	}

	/**
	 * Handles the pointer moving.
	 *
	 * @param x the pointer's x
	 * @param y the pointer's y
	 * @return whether the modal is open, so nothing behind it is hovered
	 */
	boolean onMouseMoved(int x, int y)
	{
		if (!open)
			return false;

		hovered = buttonAt(x, y);
		if (hovered >= 0)
			selected = hovered;

		return true;
	}

	/**
	 * Handles a mouse button going down.
	 *
	 * @param x the pointer's x
	 * @param y the pointer's y
	 * @param button the button, as {@link MouseEvent#getButton}
	 * @param modifiers the modifiers, as {@link java.awt.event.InputEvent#getModifiersEx}
	 * @param nowMs the time in milliseconds
	 * @return whether the click was consumed: always while the modal is open
	 */
	boolean onMousePressed(int x, int y, int button, int modifiers, long nowMs)
	{
		if (!open)
			return false;

		if (field != null && fieldBox.contains(x, y))
		{
			field.onMousePressed(x, y, button, modifiers, nowMs);
			return true;
		}

		if (button == MouseEvent.BUTTON1)
		{
			pressed = buttonAt(x, y);
			if (pressed >= 0)
				selected = pressed;
		}

		return true;
	}

	/**
	 * Handles a mouse button going up: a button is chosen when the pointer is still on the one that went down.
	 *
	 * @param x the pointer's x
	 * @param y the pointer's y
	 * @return whether the event was consumed: always while the modal is open
	 */
	boolean onMouseReleased(int x, int y)
	{
		if (!open)
			return false;

		if (field != null)
			field.onMouseReleased();

		int down = pressed;
		pressed = -1;
		if (down >= 0 && buttonAt(x, y) == down)
			choose(down);

		return true;
	}

	/**
	 * Handles the pointer moving with the button down, for a selection in the text box.
	 *
	 * @param x the pointer's x
	 * @return whether the modal is open
	 */
	boolean onMouseDragged(int x)
	{
		if (!open)
			return false;

		if (field != null)
			field.onMouseDragged(x);

		return true;
	}

	/**
	 * Tells whether the modal uses a key, so a key listener can consume its pressed, typed and released events.
	 *
	 * @param keyCode the key, as {@link KeyEvent#getKeyCode}
	 * @param modifiers the modifiers, as {@link java.awt.event.InputEvent#getModifiersEx}
	 * @return whether the modal is open and takes the key
	 */
	boolean uses(int keyCode, int modifiers)
	{
		if (!open)
			return false;

		if (field != null)
			return field.uses(keyCode, modifiers);

		return keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_ENTER
			|| keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_BACK_SPACE;
	}

	/**
	 * Handles a key going down.
	 *
	 * @param keyCode the key, as {@link KeyEvent#getKeyCode}
	 * @param modifiers the modifiers, as {@link java.awt.event.InputEvent#getModifiersEx}
	 * @param nowMs the time in milliseconds
	 * @return whether the key was used and must be consumed
	 */
	boolean onKeyPressed(int keyCode, int modifiers, long nowMs)
	{
		if (!open)
			return false;

		if (field != null)
			return field.onKeyPressed(keyCode, modifiers, nowMs);

		if (!uses(keyCode, modifiers))
			return false;

		int n = buttons.size();
		switch (keyCode)
		{
			case KeyEvent.VK_LEFT:
				selected = Math.floorMod(selected - 1, n);
				break;
			case KeyEvent.VK_RIGHT:
				selected = Math.floorMod(selected + 1, n);
				break;
			case KeyEvent.VK_ENTER:
			case KeyEvent.VK_SPACE:
				choose(selected);
				break;
			default:
				cancel();
				break;
		}

		return true;
	}

	/**
	 * Handles a character typed.
	 *
	 * @param c the character
	 * @param modifiers the modifiers, as {@link java.awt.event.InputEvent#getModifiersEx}
	 * @param nowMs the time in milliseconds
	 * @return whether the event was consumed
	 */
	boolean onKeyTyped(char c, int modifiers, long nowMs)
	{
		if (!open)
			return false;

		return field != null && field.onKeyTyped(c, modifiers, nowMs);
	}

	/**
	 * Handles a key going up.
	 *
	 * @param keyCode the key, as {@link KeyEvent#getKeyCode}
	 * @param modifiers the modifiers, as {@link java.awt.event.InputEvent#getModifiersEx}
	 * @return whether the event was consumed, which pairs with {@link #uses}
	 */
	boolean onKeyReleased(int keyCode, int modifiers)
	{
		return uses(keyCode, modifiers);
	}

	/**
	 * Paints the dim, the dialog, the text box and the buttons, fading in as the modal opens.
	 *
	 * @param g the graphics
	 * @param style the style
	 * @param area the area the modal dims and centres in
	 * @param mode the motion mode
	 * @param nowMs the time in milliseconds
	 */
	void paint(Graphics2D g, WindowStyle style, Rectangle area, MotionMode mode, long nowMs)
	{
		if (!open)
			return;

		Font font = font(style);
		Font boldFont = boldFont(style);
		FontMetrics regular = g.getFontMetrics(font);
		FontMetrics bold = g.getFontMetrics(boldFont);
		layout(area, style, regular, bold);
		Composite saved = g.getComposite();
		long fadeMs = mode.fadeMs(WindowTimes.TAB);
		if (fadeMs > 0)
		{
			float alpha = (float) Easing.OUT_CUBIC.apply((double) (nowMs - openedMs) / fadeMs);
			g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
		}

		Shape savedClip = g.getClip();
		g.clip(style.outline(WindowStyle.Kind.BODY, new Rectangle2D.Double(area.x, area.y, area.width, area.height)));
		g.setColor(style.color(ThemeColor.SCRIM));
		g.fillRect(area.x, area.y, area.width, area.height);
		g.setClip(savedClip);
		style.popup(g, new Rectangle2D.Double(dialog.x, dialog.y, dialog.width, dialog.height), "modal",
			ThemeColor.PANEL_BOX, ThemeColor.PANEL_BORDER, false, Readability.BODY);
		int pad = style.metrics().pad;
		style.text(g, title, dialog.x + pad, dialog.y + pad + bold.getAscent(), boldFont, ThemeColor.TEXT);
		int textTop = dialog.y + pad + bold.getHeight() + pad;
		TextLayout.plain(message, regular, dialog.width - 2 * pad, 0).paint(g, style, dialog.x + pad, textTop,
			ThemeColor.TEXT);
		if (field != null)
			field.paint(g, style, fieldBox, mode, nowMs);

		for (int i = 0; i < buttonBoxes.size(); i++)
		{
			Rectangle box = buttonBoxes.get(i);
			Rectangle2D shape = new Rectangle2D.Double(box.x, box.y, box.width, box.height);
			style.button(g, shape, buttons.get(i), i == confirmIndex && field != null || i == selected,
				false, i == hovered ? 1.0 : 0.0, i == pressed ? 1.0 : 0.0);
			if (i == selected && field == null)
				style.focusRing(g, style.outline(WindowStyle.Kind.BUTTON, shape));
		}

		g.setComposite(saved);
	}
}
