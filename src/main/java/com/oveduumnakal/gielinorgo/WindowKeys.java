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
import java.util.function.Supplier;

import net.runelite.client.config.Keybind;
import net.runelite.client.input.KeyListener;

/**
 * The keyboard side of the painted window, a {@link KeyListener} like {@link BattleHotkeys}. Keys belong to the window
 * only while it has focus: then it consumes pressed, typed and released for the keys it uses, so arrows do not turn
 * the camera and Enter does not open chat. A text box that has focus takes every key it uses regardless of the window
 * keyboard setting; it leaves the press of a character key alone, so the typed character that follows reaches it. It
 * ignores key repeat for Enter, Space and Esc (holding Up or Down still repeats), lets anything
 * with Ctrl or Alt through, lets RuneLite's drag key through, and does nothing while the player types in chat. The
 * rebindable "open window" key works while the window is closed or unfocused. It runs on the UI thread and decides
 * from the published {@link WindowView}; the work goes to a {@link Sink} that carries it to the game thread. Holds no
 * RuneLite types beyond the listener and the key binding.
 */
final class WindowKeys implements KeyListener
{
	private final Supplier<WindowView> view;
	private final DragKey drag;
	private final Supplier<Keybind> openKey;
	private final BooleanSupplier keyboard;
	private final BooleanSupplier chatTyping;
	private final Sink sink;
	private final Set<Integer> down = new HashSet<>();
	private final Map<Integer, Character> swallowed = new HashMap<>();

	/** Where the keys go: the game thread side of the window. Called on the UI thread, which queues each one. */
	interface Sink
	{
		/**
		 * A key the window uses went down. Repeats of Enter, Space and Esc are not passed on.
		 *
		 * @param keyCode the key, as {@link KeyEvent#getKeyCode}
		 * @param modifiers the modifiers, as {@link KeyEvent#getModifiersEx}
		 */
		void pressed(int keyCode, int modifiers);

		/**
		 * A character was typed into a text box.
		 *
		 * @param c the character
		 * @param modifiers the modifiers
		 */
		void typed(char c, int modifiers);

		/** The "open window" key was pressed. */
		void openKey();
	}

	/**
	 * Creates the listener.
	 *
	 * @param view the published window view
	 * @param drag RuneLite's drag key, which always passes through
	 * @param openKey the "open window" key, read at each key press; not set by default
	 * @param keyboard whether the "Window keyboard" setting is on, read at each key press; a cached value
	 * @param chatTyping whether the player is typing in chat, read at each key press; a cached value
	 * @param sink where the keys go
	 */
	WindowKeys(Supplier<WindowView> view, DragKey drag, Supplier<Keybind> openKey, BooleanSupplier keyboard,
		BooleanSupplier chatTyping, Sink sink)
	{
		this.view = view;
		this.drag = drag;
		this.openKey = openKey;
		this.keyboard = keyboard;
		this.chatTyping = chatTyping;
		this.sink = sink;
	}

	/**
	 * Works out which part of the window gets the keys now.
	 *
	 * @return {@link WindowKeyRules.KeyMode#NONE} unless the window is open and either a text box has focus or the
	 *         window has focus with its keyboard on; else the mode the window published
	 */
	WindowKeyRules.KeyMode mode()
	{
		WindowView v = view.get();
		if (!v.open())
			return WindowKeyRules.KeyMode.NONE;

		if (v.keys() == WindowKeyRules.KeyMode.FIELD)
			return v.keys();

		return v.focused() && keyboard.getAsBoolean() ? v.keys() : WindowKeyRules.KeyMode.NONE;
	}

	@Override
	public void keyPressed(KeyEvent event)
	{
		int code = event.getKeyCode();
		drag.observe(event, true);
		boolean repeat = !down.add(code);
		if (drag.isDragKey(event) || chatTyping.getAsBoolean())
			return;

		Keybind open = openKey.get();
		if (open != null && open.matches(event) && mode() != WindowKeyRules.KeyMode.FIELD)
		{
			take(event);
			if (!repeat)
				sink.openKey();

			return;
		}

		WindowKeyRules.KeyMode mode = mode();
		if (mode == WindowKeyRules.KeyMode.FIELD
			&& WindowKeyRules.typesCharacter(code, event.getModifiersEx(), event.isActionKey()))
			return;

		if (!WindowKeyRules.uses(mode, code, event.getModifiersEx()))
			return;

		take(event);
		if (repeat && WindowKeyRules.repeatIgnored(code))
			return;

		sink.pressed(code, event.getModifiersEx());
	}

	@Override
	public void keyTyped(KeyEvent event)
	{
		if (chatTyping.getAsBoolean())
			return;

		char c = event.getKeyChar();
		WindowKeyRules.KeyMode mode = mode();
		if (mode != WindowKeyRules.KeyMode.FIELD && swallowed.containsValue(c))
		{
			event.consume();
			return;
		}

		if (!WindowKeyRules.typedUsed(mode, c, event.getModifiersEx()))
			return;

		event.consume();
		if (mode == WindowKeyRules.KeyMode.FIELD)
			sink.typed(c, event.getModifiersEx());
	}

	@Override
	public void keyReleased(KeyEvent event)
	{
		int code = event.getKeyCode();
		drag.observe(event, false);
		down.remove(code);
		if (swallowed.containsKey(code))
		{
			swallowed.remove(code);
			event.consume();
		}
	}

	private void take(KeyEvent event)
	{
		event.consume();
		swallowed.put(event.getKeyCode(), event.getKeyChar());
	}
}
