/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.OverlayManager;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Checks the real window follows the Glass look setting, the same way its overlays do (#324). */
public class GoWindowThemeTest
{
	private final GielinorGoConfig config = mock(GielinorGoConfig.class);

	/** @return a window built the way the plugin builds it, on the mocked settings */
	private GoWindow window()
	{
		return new GoWindow(config, mock(ConfigManager.class), mock(OverlayManager.class), () -> 0L,
			() -> 800, () -> 600, (message, x, y) ->
			{
			});
	}

	/** Glass look on: the window paints as glass. */
	@Test
	public void glassLookOnPaintsTheWindowAsGlass()
	{
		when(config.glassLook()).thenReturn(true);
		GoWindow window = window();
		assertSame(WindowTheme.RUNELITE_GLASS, window.shell().theme());
	}

	/** Glass look off: the window paints solid. */
	@Test
	public void glassLookOffPaintsTheWindowSolid()
	{
		when(config.glassLook()).thenReturn(false);
		GoWindow window = window();
		assertSame(WindowTheme.RUNELITE_DARK, window.shell().theme());
	}

	/** Turning the setting over while the window exists changes the look on the next frame. */
	@Test
	public void theWindowFollowsTheSettingLive()
	{
		when(config.glassLook()).thenReturn(false);
		GoWindow window = window();
		assertSame(WindowTheme.RUNELITE_DARK, window.shell().theme());
		when(config.glassLook()).thenReturn(true);
		assertSame(WindowTheme.RUNELITE_GLASS, window.shell().theme());
	}
}
