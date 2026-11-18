/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;

import net.runelite.client.input.KeyListener;

/**
 * The keyboard side of the painted wild creature menu, a {@link KeyListener} like {@link WindowKeys}. While the menu
 * is open it consumes the pressed, typed and released events of the keys it uses (the arrows, Enter, Space, Esc and
 * Backspace), so arrows do not turn the camera and Enter does not open chat. It ignores key repeat for Enter, Space
 * and Esc, lets anything with Ctrl or Alt through, lets RuneLite's drag key through, and does nothing while the
 * player types in chat. It runs on the UI thread and hands each key to a {@link Sink} that carries it to the game
 * thread. Holds no RuneLite types beyond the listener interface.
 */
final class WildMenuKeys implements KeyListener
{
	private final IntPredicate uses;
	private final DragKey drag;
	private final BooleanSupplier chatTyping;
	private final Sink sink;
	private final Set<Integer> down = new HashSet<>();
	private final Map<Integer, Character> swallowed = new HashMap<>();

	/** Where the keys go: the game thread side of the menu. Called on the UI thread, which queues each one. */
	interface Sink
	{
		/**
		 * A key the menu uses went down.
		 *
		 * @param keyCode the key, as {@link KeyEvent#getKeyCode}
		 */
		void pressed(int keyCode);
	}

	/**
	 * Creates the listener.
	 *
	 * @param uses whether the menu is open and uses a key code
	 * @param drag RuneLite's drag key, which always passes through
	 * @param chatTyping whether the player is typing in chat, read at each key press
	 * @param sink where the keys go
	 */
	WildMenuKeys(IntPredicate uses, DragKey drag, BooleanSupplier chatTyping, Sink sink)
	{
		this.uses = uses;
		this.drag = drag;
		this.chatTyping = chatTyping;
		this.sink = sink;
	}

	@Override
	public void keyPressed(KeyEvent event)
	{
		int code = event.getKeyCode();
		boolean repeat = !down.add(code);
		if (drag.isDragKey(event) || chatTyping.getAsBoolean() || WindowKeyRules.chorded(event.getModifiersEx())
			|| !uses.test(code))
			return;

		event.consume();
		swallowed.put(code, event.getKeyChar());
		if (repeat && WindowKeyRules.repeatIgnored(code))
			return;

		sink.pressed(code);
	}

	@Override
	public void keyTyped(KeyEvent event)
	{
		if (swallowed.containsValue(event.getKeyChar()))
			event.consume();
	}

	@Override
	public void keyReleased(KeyEvent event)
	{
		int code = event.getKeyCode();
		down.remove(code);
		if (swallowed.containsKey(code))
		{
			swallowed.remove(code);
			event.consume();
		}
	}
}
