/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import net.runelite.client.config.Keybind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Checks that the window takes keys only while it has focus, consumes pressed, typed and released together, ignores
 * repeat for Enter, Space and Esc, and lets Ctrl, Alt, the drag key and chat typing through.
 */
public class WindowKeysTest
{
	private final List<String> calls = new ArrayList<>();
	private boolean open = true;
	private boolean focused = true;
	private boolean keyboard = true;
	private boolean chat;
	private WindowKeyRules.KeyMode mode = WindowKeyRules.KeyMode.WINDOW;
	private Keybind openKey = Keybind.NOT_SET;
	private Keybind dragBind = Keybind.ALT;
	private final DragKey drag = new DragKey(() -> dragBind);
	private final WindowKeys keys = new WindowKeys(this::view, drag, () -> openKey, () -> keyboard, () -> chat,
		new WindowKeys.Sink()
		{
			@Override
			public void pressed(int keyCode, int modifiers)
			{
				calls.add("pressed " + keyCode);
			}

			@Override
			public void typed(char c, int modifiers)
			{
				calls.add("typed " + c);
			}

			@Override
			public void openKey()
			{
				calls.add("open");
			}
		});

	private WindowView view()
	{
		return new WindowView(open, new Rectangle(0, 0, 100, 100), Collections.emptyList(), mode, focused);
	}

	private boolean pressed(int code)
	{
		return pressed(code, 0);
	}

	private boolean pressed(int code, int modifiers)
	{
		KeyEvent event = WindowEvents.press(code, modifiers);
		keys.keyPressed(event);
		return event.isConsumed();
	}

	private boolean released(int code)
	{
		KeyEvent event = WindowEvents.release(code, 0);
		keys.keyReleased(event);
		return event.isConsumed();
	}

	private boolean typed(char c)
	{
		KeyEvent event = WindowEvents.typed(c, 0);
		keys.keyTyped(event);
		return event.isConsumed();
	}

	/**
	 * Strikes a key as the toolkit does: a consumed press drops the typed event that would follow it, so the
	 * character only reaches the listener when the press was left alone. The release follows either way.
	 *
	 * @param code the key
	 * @param c the character the key types, or {@link KeyEvent#CHAR_UNDEFINED}
	 * @return whether the window took the key: its press, or the character that followed it
	 */
	private boolean strike(int code, char c)
	{
		KeyEvent press = WindowEvents.press(code, 0);
		keys.keyPressed(press);
		boolean taken = press.isConsumed();
		if (!taken && c != KeyEvent.CHAR_UNDEFINED)
		{
			KeyEvent typed = WindowEvents.typed(c, 0);
			keys.keyTyped(typed);
			taken = typed.isConsumed();
		}

		keys.keyReleased(WindowEvents.release(code, 0));
		return taken;
	}

	@Test
	public void theKeysItUsesAreConsumedPressedTypedAndReleased()
	{
		assertTrue(pressed(KeyEvent.VK_ENTER));
		assertTrue(typed('\n'));
		assertTrue(released(KeyEvent.VK_ENTER));
		assertTrue(pressed(KeyEvent.VK_LEFT));
		assertTrue(released(KeyEvent.VK_LEFT));
		assertTrue(pressed(KeyEvent.VK_SPACE));
		assertTrue(typed(' '));
		assertTrue(released(KeyEvent.VK_SPACE));
		assertTrue(pressed(KeyEvent.VK_ESCAPE));
		assertTrue(typed((char) KeyEvent.VK_ESCAPE));
		assertTrue(released(KeyEvent.VK_ESCAPE));
		assertTrue(pressed(KeyEvent.VK_BACK_SPACE));
		assertTrue(typed('\b'));
		assertTrue(released(KeyEvent.VK_BACK_SPACE));

		assertEquals(List.of("pressed 10", "pressed 37", "pressed 32", "pressed 27", "pressed 8"), calls);
	}

	@Test
	public void aKeyTheWindowDoesNotUseGoesToTheGame()
	{
		assertFalse(pressed(KeyEvent.VK_F5));
		assertFalse(pressed(KeyEvent.VK_A));
		assertFalse(typed('a'));
		assertFalse(released(KeyEvent.VK_A));
		assertTrue(calls.isEmpty());
	}

	@Test
	public void nothingIsTakenWhileTheWindowHasNoFocus()
	{
		focused = false;

		assertFalse(pressed(KeyEvent.VK_UP));
		assertFalse(typed('\n'));
		assertFalse(released(KeyEvent.VK_UP));
		assertTrue(calls.isEmpty());
	}

	@Test
	public void nothingIsTakenWhileTheWindowIsClosedOrItsKeyboardIsOff()
	{
		open = false;
		assertFalse(pressed(KeyEvent.VK_RIGHT));

		open = true;
		keyboard = false;
		assertFalse(pressed(KeyEvent.VK_RIGHT));
		assertTrue(calls.isEmpty());
	}

	@Test
	public void holdingEnterSpaceOrEscActsOnceButUpAndDownRepeat()
	{
		pressed(KeyEvent.VK_ENTER);
		assertTrue(pressed(KeyEvent.VK_ENTER));
		assertTrue(pressed(KeyEvent.VK_ENTER));
		pressed(KeyEvent.VK_SPACE);
		pressed(KeyEvent.VK_SPACE);
		pressed(KeyEvent.VK_ESCAPE);
		pressed(KeyEvent.VK_ESCAPE);
		pressed(KeyEvent.VK_DOWN);
		pressed(KeyEvent.VK_DOWN);
		pressed(KeyEvent.VK_DOWN);

		assertEquals(List.of("pressed 10", "pressed 32", "pressed 27", "pressed 40", "pressed 40", "pressed 40"),
			calls);

		released(KeyEvent.VK_ENTER);
		pressed(KeyEvent.VK_ENTER);
		assertEquals("pressed 10", calls.get(calls.size() - 1));
		assertEquals(7, calls.size());
	}

	@Test
	public void ctrlAndAltCombinationsGoToTheGame()
	{
		assertFalse(pressed(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK));
		assertFalse(pressed(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK));
		assertFalse(pressed(KeyEvent.VK_UP, InputEvent.META_DOWN_MASK));
		assertTrue(calls.isEmpty());

		KeyEvent chorded = WindowEvents.typed('\n', InputEvent.CTRL_DOWN_MASK);
		keys.keyTyped(chorded);
		assertFalse(chorded.isConsumed());
	}

	@Test
	public void theDragKeyGoesToRuneLiteAsAltAndAsAReboundKey()
	{
		assertFalse(pressed(KeyEvent.VK_ALT, InputEvent.ALT_DOWN_MASK));
		assertFalse(released(KeyEvent.VK_ALT));

		dragBind = new Keybind(KeyEvent.VK_SPACE, 0);
		assertFalse(pressed(KeyEvent.VK_SPACE));
		assertFalse(released(KeyEvent.VK_SPACE));
		assertTrue(pressed(KeyEvent.VK_ENTER));
		assertEquals(List.of("pressed 10"), calls);
	}

	@Test
	public void typingInChatMakesTheWindowStandAside()
	{
		chat = true;
		openKey = new Keybind(KeyEvent.VK_F7, 0);

		assertFalse(pressed(KeyEvent.VK_ENTER));
		assertFalse(typed('\n'));
		assertFalse(pressed(KeyEvent.VK_LEFT));
		assertFalse(pressed(KeyEvent.VK_F7));
		assertTrue(calls.isEmpty());
	}

	@Test
	public void theOpenKeyWorksWhileClosedAndActsOncePerPress()
	{
		open = false;
		openKey = new Keybind(KeyEvent.VK_F7, 0);

		assertTrue(pressed(KeyEvent.VK_F7));
		assertTrue(pressed(KeyEvent.VK_F7));
		assertTrue(released(KeyEvent.VK_F7));
		assertTrue(pressed(KeyEvent.VK_F7));

		assertEquals(List.of("open", "open"), calls);
	}

	@Test
	public void anOpenKeyThatTypesACharacterSwallowsThatCharacterToo()
	{
		open = false;
		openKey = new Keybind(KeyEvent.VK_J, 0);
		KeyEvent press = new KeyEvent(new javax.swing.JLabel(), KeyEvent.KEY_PRESSED, 0L, 0, KeyEvent.VK_J, 'j')
		{
			@Override
			public int getExtendedKeyCode()
			{
				return KeyEvent.VK_J;
			}
		};
		keys.keyPressed(press);

		assertTrue(press.isConsumed());
		assertTrue(typed('j'));
		assertTrue(released(KeyEvent.VK_J));
		assertFalse(typed('j'));
	}

	@Test
	public void aNotSetOpenKeyTakesNothing()
	{
		open = false;

		assertFalse(pressed(KeyEvent.VK_F7));
		assertFalse(pressed(0));
		assertTrue(calls.isEmpty());
	}

	@Test
	public void aMenuTakesUpAndDownButNotLeftAndRight()
	{
		mode = WindowKeyRules.KeyMode.MENU;

		assertTrue(pressed(KeyEvent.VK_UP));
		assertTrue(pressed(KeyEvent.VK_ENTER));
		assertFalse(pressed(KeyEvent.VK_LEFT));
		assertFalse(pressed(KeyEvent.VK_RIGHT));
	}

	@Test
	public void aDialogWithoutATextBoxTakesLeftAndRightButNotUpAndDown()
	{
		mode = WindowKeyRules.KeyMode.MODAL;

		assertTrue(pressed(KeyEvent.VK_LEFT));
		assertTrue(pressed(KeyEvent.VK_RIGHT));
		assertTrue(pressed(KeyEvent.VK_ESCAPE));
		assertFalse(pressed(KeyEvent.VK_UP));
		assertFalse(pressed(KeyEvent.VK_DOWN));
	}

	@Test
	public void aTextBoxTakesEveryKeyAndTypedCharacterEvenWithoutWindowFocusOrKeyboard()
	{
		mode = WindowKeyRules.KeyMode.FIELD;
		focused = false;
		keyboard = false;

		assertTrue(strike(KeyEvent.VK_A, 'a'));
		assertTrue(pressed(KeyEvent.VK_ENTER));
		assertTrue(pressed(KeyEvent.VK_F5));
		assertEquals(List.of("typed a", "pressed 10", "pressed 116"), calls);
	}

	@Test
	public void aCharacterTypedIntoATextBoxReachesTheBoxAndNeverTheGame()
	{
		mode = WindowKeyRules.KeyMode.FIELD;

		assertTrue(strike(KeyEvent.VK_N, 'n'));
		assertTrue(strike(KeyEvent.VK_SPACE, ' '));
		assertTrue(strike(KeyEvent.VK_1, '1'));
		assertTrue(strike(KeyEvent.VK_MINUS, '-'));
		assertEquals(List.of("typed n", "typed  ", "typed 1", "typed -"), calls);
	}

	@Test
	public void theKeysATextBoxActsOnArePressedAndConsumedNotTyped()
	{
		mode = WindowKeyRules.KeyMode.FIELD;

		assertTrue(strike(KeyEvent.VK_BACK_SPACE, '\b'));
		assertTrue(strike(KeyEvent.VK_DELETE, (char) 127));
		assertTrue(strike(KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED));
		assertTrue(strike(KeyEvent.VK_HOME, KeyEvent.CHAR_UNDEFINED));
		assertEquals(List.of("pressed 8", "pressed 127", "pressed 37", "pressed 36"), calls);
	}

	@Test
	public void aCharacterKeyPressInATextBoxIsLeftAloneSoItsTypedCharacterFollows()
	{
		mode = WindowKeyRules.KeyMode.FIELD;

		assertFalse(pressed(KeyEvent.VK_Q));
		assertFalse(pressed(KeyEvent.VK_SPACE));
		assertTrue(typed('q'));
		assertFalse(released(KeyEvent.VK_Q));
	}

	@Test
	public void aTextBoxLetsAltAndMostCtrlCombinationsThrough()
	{
		mode = WindowKeyRules.KeyMode.FIELD;

		assertFalse(pressed(KeyEvent.VK_X, InputEvent.ALT_DOWN_MASK));
		assertFalse(pressed(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK));
		assertTrue(pressed(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
		assertFalse(pressed(KeyEvent.VK_CONTROL, InputEvent.CTRL_DOWN_MASK));

		KeyEvent typedAlt = WindowEvents.typed('x', InputEvent.ALT_DOWN_MASK);
		keys.keyTyped(typedAlt);
		assertFalse(typedAlt.isConsumed());
	}

	@Test
	public void theOpenKeyIsLeftToTheTextBoxWhileOneHasTheKeys()
	{
		mode = WindowKeyRules.KeyMode.FIELD;
		openKey = new Keybind(KeyEvent.VK_F, 0);

		assertTrue(strike(KeyEvent.VK_F, 'f'));
		assertEquals(List.of("typed f"), calls);
	}

	@Test
	public void theReleaseOfAKeyTakenIsTakenEvenIfFocusMovedAway()
	{
		pressed(KeyEvent.VK_LEFT);
		focused = false;

		assertTrue(released(KeyEvent.VK_LEFT));
	}

	@Test
	public void aReleaseOfAKeyTheGameSawGoesToTheGame()
	{
		focused = false;
		pressed(KeyEvent.VK_LEFT);
		focused = true;

		assertFalse(released(KeyEvent.VK_LEFT));
	}
}
