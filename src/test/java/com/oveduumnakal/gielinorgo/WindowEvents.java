/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.JLabel;

/** Builds the AWT events the window's input tests feed to the listeners. */
final class WindowEvents
{
	private WindowEvents()
	{
	}

	/**
	 * @param code the key
	 * @param modifiers the modifiers
	 * @return a key press whose extended key code is the key's own, as a real keyboard gives
	 */
	static KeyEvent press(int code, int modifiers)
	{
		return key(KeyEvent.KEY_PRESSED, code, modifiers, KeyEvent.CHAR_UNDEFINED);
	}

	/**
	 * @param code the key
	 * @param modifiers the modifiers
	 * @return a key release
	 */
	static KeyEvent release(int code, int modifiers)
	{
		return key(KeyEvent.KEY_RELEASED, code, modifiers, KeyEvent.CHAR_UNDEFINED);
	}

	/**
	 * @param c the character
	 * @param modifiers the modifiers
	 * @return a typed character
	 */
	static KeyEvent typed(char c, int modifiers)
	{
		return key(KeyEvent.KEY_TYPED, KeyEvent.VK_UNDEFINED, modifiers, c);
	}

	private static KeyEvent key(int id, int code, int modifiers, char c)
	{
		return new KeyEvent(new JLabel(), id, 0L, modifiers, code, c)
		{
			@Override
			public int getExtendedKeyCode()
			{
				return code;
			}
		};
	}

	/**
	 * @param id a {@code MouseEvent.MOUSE_*} id
	 * @param x the canvas x
	 * @param y the canvas y
	 * @param button the button, 0 for none
	 * @param modifiers the modifiers
	 * @return a mouse event
	 */
	static MouseEvent mouse(int id, int x, int y, int button, int modifiers)
	{
		return new MouseEvent(new JLabel(), id, 0L, modifiers, x, y, 1, false, button);
	}

	/**
	 * @param x the canvas x
	 * @param y the canvas y
	 * @param rotation the wheel's clicks
	 * @param modifiers the modifiers
	 * @return a wheel event
	 */
	static MouseWheelEvent wheel(int x, int y, int rotation, int modifiers)
	{
		return new MouseWheelEvent(new JLabel(), MouseEvent.MOUSE_WHEEL, 0L, modifiers, x, y, 0, false,
			MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, rotation);
	}
}
