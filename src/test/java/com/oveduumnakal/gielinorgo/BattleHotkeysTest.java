/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JLabel;

import org.junit.Test;

import net.runelite.client.config.Keybind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks that hotkeys act only while a choice is open, only when set, and take the key they use. */
public class BattleHotkeysTest
{
	private final List<BattleChoice.Kind> pressed = new ArrayList<>();
	private boolean open = true;

	private BattleHotkeys keys(Keybind attack, Keybind swap, Keybind jar, Keybind run)
	{
		return new BattleHotkeys(() -> attack, () -> swap, () -> jar, () -> run, () -> open, pressed::add);
	}

	private static KeyEvent press(int code)
	{
		return press(code, 0);
	}

	private static KeyEvent press(int code, int modifiers)
	{
		return new KeyEvent(new JLabel(), KeyEvent.KEY_PRESSED, 0L, modifiers, code, KeyEvent.CHAR_UNDEFINED)
		{
			@Override
			public int getExtendedKeyCode()
			{
				return code;
			}
		};
	}

	private BattleHotkeys allSet()
	{
		return keys(new Keybind(KeyEvent.VK_F1, 0), new Keybind(KeyEvent.VK_F2, 0), new Keybind(KeyEvent.VK_F3, 0),
			new Keybind(KeyEvent.VK_F4, InputEvent.SHIFT_DOWN_MASK));
	}

	@Test
	public void eachKeyIsItsChoice()
	{
		BattleHotkeys keys = allSet();

		assertEquals(BattleChoice.Kind.ATTACK, keys.kindOf(press(KeyEvent.VK_F1)));
		assertEquals(BattleChoice.Kind.SWITCH, keys.kindOf(press(KeyEvent.VK_F2)));
		assertEquals(BattleChoice.Kind.CATCH, keys.kindOf(press(KeyEvent.VK_F3)));
		assertNull(keys.kindOf(press(KeyEvent.VK_F4)));
		assertNull(keys.kindOf(press(KeyEvent.VK_F5)));
	}

	@Test
	public void aKeyWithModifiersNeedsThem()
	{
		BattleHotkeys keys = allSet();
		KeyEvent shifted = press(KeyEvent.VK_F4, InputEvent.SHIFT_DOWN_MASK);

		assertEquals(BattleChoice.Kind.RUN, keys.kindOf(shifted));
	}

	@Test
	public void aPressWhileAChoiceIsOpenIsHandedOnAndTaken()
	{
		BattleHotkeys keys = allSet();
		KeyEvent event = press(KeyEvent.VK_F1);

		keys.keyPressed(event);

		assertEquals(Collections.singletonList(BattleChoice.Kind.ATTACK), pressed);
		assertTrue(event.isConsumed());
	}

	@Test
	public void nothingIsTakenWhileNoChoiceIsOpen()
	{
		BattleHotkeys keys = allSet();
		open = false;
		KeyEvent event = press(KeyEvent.VK_F1);

		keys.keyPressed(event);

		assertTrue(pressed.isEmpty());
		assertFalse(event.isConsumed());
	}

	@Test
	public void keysThatAreNotSetDoNothing()
	{
		BattleHotkeys keys = keys(Keybind.NOT_SET, Keybind.NOT_SET, Keybind.NOT_SET, Keybind.NOT_SET);
		KeyEvent event = press(KeyEvent.VK_UNDEFINED);

		keys.keyPressed(event);
		keys.keyPressed(press(KeyEvent.VK_A));

		assertTrue(pressed.isEmpty());
		assertFalse(event.isConsumed());
	}
}
