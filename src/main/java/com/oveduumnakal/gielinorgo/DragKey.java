/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.function.Supplier;

import net.runelite.client.config.Keybind;

/**
 * RuneLite's drag key, read from {@code RuneLiteConfig.dragHotkey()} each time and never assumed to be Alt. While it
 * is held RuneLite is in its overlay edit mode, so the window's input passes every mouse event through untouched.
 * A key that is only a modifier (Alt, Ctrl, Shift) shows in the mouse event's own modifiers; any other key is
 * followed through the key events the key listener passes to {@link #observe}. A volatile flag carries the state
 * between the UI thread's two listeners.
 */
final class DragKey
{
	private final Supplier<Keybind> bind;
	private volatile boolean down;
	private volatile boolean held;

	/**
	 * Creates the tracker.
	 *
	 * @param bind the drag key, read at every event so a change in RuneLite's settings applies at once
	 */
	DragKey(Supplier<Keybind> bind)
	{
		this.bind = bind;
	}

	/**
	 * Follows a key event, so a drag key that is not a modifier is known to be held.
	 *
	 * @param event the key event
	 * @param pressed true for a press, false for a release
	 */
	void observe(KeyEvent event, boolean pressed)
	{
		Keybind key = bind.get();
		if (key == null || key.getKeyCode() == KeyEvent.VK_UNDEFINED)
			return;

		if (pressed && key.matches(event))
			down = true;
		else if (!pressed && event.getExtendedKeyCode() == key.getKeyCode())
			down = false;
	}

	/**
	 * Tells whether a key event is the drag key itself, so the key listener lets it through.
	 *
	 * @param event the key event
	 * @return true when the key is the drag key, or the modifier a modifier-only drag key stands for
	 */
	boolean isDragKey(KeyEvent event)
	{
		Keybind key = bind.get();
		if (key == null || Keybind.NOT_SET.equals(key))
			return false;

		if (key.getKeyCode() != KeyEvent.VK_UNDEFINED)
			return event.getExtendedKeyCode() == key.getKeyCode();

		Integer modifier = Keybind.getModifierForKeyCode(event.getKeyCode());
		return modifier != null && (key.getModifiers() & modifier) != 0;
	}

	/**
	 * Tells whether the drag key is held for a mouse event.
	 *
	 * @param event the mouse event
	 * @return true when RuneLite is in its edit mode for this event
	 */
	boolean heldIn(InputEvent event)
	{
		Keybind key = bind.get();
		boolean now = false;
		if (key != null && !Keybind.NOT_SET.equals(key))
		{
			int wanted = key.getModifiers();
			boolean chord = (event.getModifiersEx() & wanted) == wanted;
			now = key.getKeyCode() == KeyEvent.VK_UNDEFINED ? chord : down && chord;
		}

		held = now;
		return now;
	}

	/** @return whether the drag key was held at the last mouse event, which is fresh while the mouse moves */
	boolean held()
	{
		return held;
	}

	/** Forgets that the key is held, for when the window loses the game's focus and never sees the release. */
	void reset()
	{
		down = false;
		held = false;
	}
}
