/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * Which keys the painted window takes from the game, as pure rules over a key code and its modifiers. The key
 * listener asks them on the UI thread, with a {@link KeyMode} the window published, so it never touches window state.
 * The arrows, Enter, Space, Esc and Backspace are fixed so a controller layout always works. Ctrl, Alt and the drag
 * key are never taken by the navigation modes: they belong to the game and to RuneLite's edit mode. Holds no RuneLite
 * types.
 */
final class WindowKeyRules
{
	private WindowKeyRules()
	{
	}

	/** Which part of the window gets the keys right now. */
	enum KeyMode
	{
		/** The window is closed, unfocused or has its keyboard turned off: every key belongs to the game. */
		NONE,

		/** The tabs and the page: Left, Right, Up, Down, Enter, Space, Esc and Backspace. */
		WINDOW,

		/** A context menu is open: Up, Down, Enter, Space, Esc and Backspace. */
		MENU,

		/** A dialog without a text box is open: Left, Right, Enter, Space, Esc and Backspace. */
		MODAL,

		/** A text box has focus: it takes every key but Alt and Ctrl combinations it does not use. */
		FIELD
	}

	/**
	 * Tells whether the window takes a key being pressed or released.
	 *
	 * @param mode which part has the keys
	 * @param keyCode the key, as {@link KeyEvent#getKeyCode}
	 * @param modifiers the modifiers, as {@link InputEvent#getModifiersEx}
	 * @return true when the key must be consumed so the game never sees it
	 */
	static boolean uses(KeyMode mode, int keyCode, int modifiers)
	{
		switch (mode)
		{
			case FIELD:
				return TextBox.takes(keyCode, modifiers);
			case WINDOW:
				return !chorded(modifiers) && (horizontal(keyCode) || vertical(keyCode) || confirm(keyCode)
					|| back(keyCode));
			case MENU:
				return !chorded(modifiers) && (vertical(keyCode) || confirm(keyCode) || back(keyCode));
			case MODAL:
				return !chorded(modifiers) && (horizontal(keyCode) || confirm(keyCode) || back(keyCode));
			default:
				return false;
		}
	}

	/**
	 * Tells whether a key in a text box is a character key, which the box takes from the typed character that follows
	 * and not from the key press. Consuming the press of such a key would stop the typed character from ever
	 * arriving, since the toolkit drops the typed event of a consumed press, so the box could not be typed into.
	 * Enter, Esc, Backspace, Delete, Tab, the arrows and the other action keys, modifier keys and Ctrl chords are not
	 * character keys: the box acts on their press.
	 *
	 * @param keyCode the key, as {@link KeyEvent#getKeyCode}
	 * @param modifiers the modifiers, as {@link InputEvent#getModifiersEx}
	 * @param actionKey whether the key is an action key, as {@link KeyEvent#isActionKey}
	 * @return true when the press must be left alone so the typed character follows
	 */
	static boolean typesCharacter(int keyCode, int modifiers, boolean actionKey)
	{
		if (actionKey || chorded(modifiers))
			return false;

		switch (keyCode)
		{
			case KeyEvent.VK_ENTER:
			case KeyEvent.VK_ESCAPE:
			case KeyEvent.VK_BACK_SPACE:
			case KeyEvent.VK_DELETE:
			case KeyEvent.VK_TAB:
			case KeyEvent.VK_SHIFT:
			case KeyEvent.VK_CONTROL:
			case KeyEvent.VK_ALT:
			case KeyEvent.VK_META:
			case KeyEvent.VK_ALT_GRAPH:
			case KeyEvent.VK_CAPS_LOCK:
			case KeyEvent.VK_UNDEFINED:
				return false;
			default:
				return true;
		}
	}

	/**
	 * Tells whether the window takes a typed character. Enter, Space, Esc and Backspace type characters of their own,
	 * and the game would act on them (Enter opens chat), so the window takes those along with the key.
	 *
	 * @param mode which part has the keys
	 * @param c the character, as {@link KeyEvent#getKeyChar}
	 * @param modifiers the modifiers, as {@link InputEvent#getModifiersEx}
	 * @return true when the event must be consumed
	 */
	static boolean typedUsed(KeyMode mode, char c, int modifiers)
	{
		switch (mode)
		{
			case FIELD:
				return TextBox.takesTyped(c, modifiers);
			case NONE:
				return false;
			default:
				return !chorded(modifiers) && (c == '\n' || c == '\r' || c == ' ' || c == KeyEvent.VK_ESCAPE
					|| c == '\b');
		}
	}

	/**
	 * @param keyCode the key
	 * @return whether holding it should not repeat its action: Enter, Space and Esc act once per press
	 */
	static boolean repeatIgnored(int keyCode)
	{
		return keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_ESCAPE;
	}

	/**
	 * @param modifiers the modifiers, as {@link InputEvent#getModifiersEx}
	 * @return whether Ctrl or Alt is held, which sends a key to the game
	 */
	static boolean chorded(int modifiers)
	{
		return (modifiers & (InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.ALT_GRAPH_DOWN_MASK
			| InputEvent.META_DOWN_MASK)) != 0;
	}

	private static boolean horizontal(int keyCode)
	{
		return keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT;
	}

	private static boolean vertical(int keyCode)
	{
		return keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN;
	}

	private static boolean confirm(int keyCode)
	{
		return keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_SPACE;
	}

	private static boolean back(int keyCode)
	{
		return keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_BACK_SPACE;
	}
}
