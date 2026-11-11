/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

/** Settings for Gielinor Go: how often wild creatures appear and how the game reports what happens. */
@ConfigGroup(GielinorGoConfig.GROUP)
public interface GielinorGoConfig extends Config
{
	/** Config group key, shared with the plugin so both agree on where settings and progress are stored. */
	String GROUP = "gielinorgo";

	/**
	 * How rare wild creatures are.
	 *
	 * @return N, where a monster turns wild one time in N
	 */
	@Range(min = 2, max = 50)
	@ConfigItem(
		keyName = "wildChance",
		name = "Wild creature rarity",
		description = "A nearby monster turns wild one time in this many. Lower numbers mean more wild creatures.",
		position = 0
	)
	default int wildChance()
	{
		return 8;
	}

	/**
	 * Colour of the outline drawn around wild creatures.
	 *
	 * @return the outline colour, with alpha
	 */
	@Alpha
	@ConfigItem(
		keyName = "outlineColor",
		name = "Wild outline colour",
		description = "Colour of the outline around monsters you can catch or battle.",
		position = 1
	)
	default Color outlineColor()
	{
		return new Color(0, 255, 255, 220);
	}

	/**
	 * Whether catches, battles and finds are shown as text floating over heads.
	 *
	 * @return true to show the floating text
	 */
	@ConfigItem(
		keyName = "showResults",
		name = "Show floating text",
		description = "Shows catches and battles as text over the monster's head, and things you find or earn over"
			+ " your own head.",
		position = 2
	)
	default boolean showResults()
	{
		return true;
	}

	/**
	 * Whether results are also written to the chatbox.
	 *
	 * @return true to print results as game messages
	 */
	@ConfigItem(
		keyName = "chatMessages",
		name = "Also print to chat",
		description = "Also writes catches, battles and rewards to your chatbox. Only you can see these messages.",
		position = 3
	)
	default boolean chatMessages()
	{
		return false;
	}

	/**
	 * Whether the game keeps running in the Wilderness and on PvP worlds.
	 *
	 * @return true to allow wild creatures in PvP areas
	 */
	@ConfigItem(
		keyName = "enableInPvp",
		name = "Play in PvP areas",
		description = "Lets wild creatures appear in the Wilderness and on PvP worlds. Off by default so nothing"
			+ " extra is on your screen or in your menus where other players can attack you.",
		position = 4
	)
	default boolean enableInPvp()
	{
		return false;
	}
}
