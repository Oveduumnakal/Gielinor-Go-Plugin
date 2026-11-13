/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.event.KeyEvent;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.runelite.client.config.Keybind;
import net.runelite.client.input.KeyListener;

/**
 * Turns the hotkeys the player set into battle choices. A key is taken only while a battle waits for a choice, so
 * nothing is stolen from chat or dialogue the rest of the time, and keys that are not set do nothing. The choice is
 * handed on as a kind; the plugin carries it to the game thread.
 */
final class BattleHotkeys implements KeyListener
{
	private final Supplier<Keybind> attack;
	private final Supplier<Keybind> swap;
	private final Supplier<Keybind> jar;
	private final Supplier<Keybind> run;
	private final Supplier<Keybind> item;
	private final BooleanSupplier open;
	private final Consumer<BattleChoice.Kind> pressed;

	/**
	 * Creates the listener.
	 *
	 * @param attack the Attack hotkey, read at each key press
	 * @param swap the Switch hotkey
	 * @param jar the Catch hotkey
	 * @param run the Run hotkey
	 * @param open whether a choice is open; read on the key thread, so it must be a cached value
	 * @param pressed called with the kind of choice a key stands for
	 */
	BattleHotkeys(Supplier<Keybind> attack, Supplier<Keybind> swap, Supplier<Keybind> jar, Supplier<Keybind> run,
		BooleanSupplier open, Consumer<BattleChoice.Kind> pressed)
	{
		this(attack, swap, jar, run, () -> Keybind.NOT_SET, open, pressed);
	}

	/**
	 * Creates the listener with an Item hotkey as well.
	 *
	 * @param attack the Attack hotkey, read at each key press
	 * @param swap the Switch hotkey
	 * @param jar the Catch hotkey
	 * @param run the Run hotkey
	 * @param item the Item hotkey
	 * @param open whether a choice is open; read on the key thread, so it must be a cached value
	 * @param pressed called with the kind of choice a key stands for
	 */
	BattleHotkeys(Supplier<Keybind> attack, Supplier<Keybind> swap, Supplier<Keybind> jar, Supplier<Keybind> run,
		Supplier<Keybind> item, BooleanSupplier open, Consumer<BattleChoice.Kind> pressed)
	{
		this.item = item;
		this.attack = attack;
		this.swap = swap;
		this.jar = jar;
		this.run = run;
		this.open = open;
		this.pressed = pressed;
	}

	/**
	 * Finds the choice a key press stands for.
	 *
	 * @param event the key press
	 * @return the kind of choice, or null when no hotkey matches
	 */
	BattleChoice.Kind kindOf(KeyEvent event)
	{
		if (matches(attack, event))
			return BattleChoice.Kind.ATTACK;

		if (matches(swap, event))
			return BattleChoice.Kind.SWITCH;

		if (matches(jar, event))
			return BattleChoice.Kind.CATCH;

		if (matches(item, event))
			return BattleChoice.Kind.ITEM;

		return matches(run, event) ? BattleChoice.Kind.RUN : null;
	}

	private static boolean matches(Supplier<Keybind> key, KeyEvent event)
	{
		Keybind bind = key.get();
		return bind != null && bind.matches(event);
	}

	@Override
	public void keyTyped(KeyEvent event)
	{
	}

	@Override
	public void keyPressed(KeyEvent event)
	{
		if (!open.getAsBoolean())
			return;

		BattleChoice.Kind kind = kindOf(event);
		if (kind == null)
			return;

		event.consume();
		pressed.accept(kind);
	}

	@Override
	public void keyReleased(KeyEvent event)
	{
	}
}
