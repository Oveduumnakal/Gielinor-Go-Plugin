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

/**
 * Settings for Gielinor Go: how often wild creatures appear, how the game reports what happens, and whether the lead
 * creature follows the player.
 */
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
	 * Whether a catch is acted out: the player throws, and a caught creature vanishes and fades back in.
	 *
	 * @return true to play the catch effects
	 */
	@ConfigItem(
		keyName = "catchEffects",
		name = "Catch effects",
		description = "Your character makes a throwing motion when you throw a jar.\nA creature you catch vanishes"
			+ " for five seconds and fades back in.\nOnly you can see this, and the real monster is untouched.",
		position = 4
	)
	default boolean catchEffects()
	{
		return true;
	}

	/**
	 * Whether a battle against a wild creature is acted out: the lead walks up to it and they fight.
	 *
	 * @return true to play the battle animations
	 */
	@ConfigItem(
		keyName = "battleAnimations",
		name = "Battle animations",
		description = "Your lead creature walks up to the wild creature in a battle. They attack and react to each"
			+ " hit,\nand the loser lies down for a few seconds.\nOnly you can see this, and the real monster is"
			+ " untouched.",
		position = 5
	)
	default boolean battleAnimations()
	{
		return true;
	}

	/**
	 * Whether the lead creature is shown walking behind the player.
	 *
	 * @return true to show the follower
	 */
	@ConfigItem(
		keyName = "showFollower",
		name = "Lead creature follows you",
		description = "Shows your lead creature walking behind you, shrunk to fit beside you. Only you can see it,"
			+ " and it cannot be clicked.",
		position = 6
	)
	default boolean showFollower()
	{
		return true;
	}

	/**
	 * Whether friends in the player's RuneLite party may battle the player's team.
	 *
	 * @return true to take up challenges from friends
	 */
	@ConfigItem(
		keyName = "friendBattles",
		name = "Accept friend battles",
		description = "Lets friends in your RuneLite party battle your team. The battle plays itself and nothing is"
			+ " won or lost.\nTurn this off to turn every challenge down.",
		position = 7
	)
	default boolean friendBattles()
	{
		return true;
	}

	/**
	 * Whether the game keeps running in the Wilderness and on PvP worlds.
	 *
	 * @return true to allow wild creatures and the follower in PvP areas
	 */
	@ConfigItem(
		keyName = "enableInPvp",
		name = "Play in PvP areas",
		description = "Lets wild creatures and your follower appear in the Wilderness and on PvP worlds.\nOff by"
			+ " default so nothing extra is on your screen or in your menus where other players can attack you.",
		position = 8
	)
	default boolean enableInPvp()
	{
		return false;
	}

	/**
	 * Whether the lead creature's nickname floats over the follower.
	 *
	 * @return true to show the nickname over the follower
	 */
	@ConfigItem(
		keyName = "showLeadName",
		name = "Show lead's nickname",
		description = "Shows the nickname you gave your lead creature above it while it follows you. Only you can"
			+ " see it.\nA lead without a nickname shows nothing.",
		position = 9
	)
	default boolean showLeadName()
	{
		return true;
	}

	/**
	 * Whether featured species, night and weekend events change what turns wild.
	 *
	 * @return true to use spawn events
	 */
	@ConfigItem(
		keyName = "spawnEvents",
		name = "Spawn events",
		description = "Turns on today's featured species, night and weekend events, and stronger roaming"
			+ " creatures.\nOff means every monster has the same odds and nothing roams.",
		position = 10
	)
	default boolean spawnEvents()
	{
		return true;
	}
}
