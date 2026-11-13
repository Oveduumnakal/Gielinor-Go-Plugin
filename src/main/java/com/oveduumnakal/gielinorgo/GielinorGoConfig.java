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
import net.runelite.client.config.Keybind;
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

	/** Key of the hidden item that stores the version the change log was last opened for. */
	String LAST_SEEN_VERSION = "lastSeenVersion";

	/** Key of the hidden item that stores whether What's New was cleared. */
	String WHATS_NEW_DISMISSED = "whatsNewDismissed";

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
		description = "Colour of the outline around wild creatures you can catch or battle.",
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
		description = "Shows catches and battles as text over the wild creature, and things you find or earn over"
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
		description = "Your character makes a throwing motion when you throw a jar, then cheers for a catch,"
			+ " stamps when the creature breaks free or cries when it runs away.\nThe creature reacts too: a catch"
			+ " shrinks it into the jar, one that breaks free attacks you and one that"
			+ " flees runs off.\nOnly you can see this, and no real monster is changed.",
		position = 4
	)
	default boolean catchEffects()
	{
		return true;
	}

	/**
	 * Whether a battle is acted out: against a wild creature, the lead walks up to it and they fight; against a
	 * City Champion or a friend, the two teams fight beside the player.
	 *
	 * @return true to play the battle animations
	 */
	@ConfigItem(
		keyName = "battleAnimations",
		name = "Battle animations",
		description = "Your lead creature walks up to the wild creature in a battle. They attack and react to each"
			+ " hit,\nand the loser lies down for a few seconds.\nCity Champion and friend battles are acted out too."
			+ "\nOnly you can see this, and no real monster is changed.",
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
	 * Whether the lead creature's name floats over the follower.
	 *
	 * @return true to show the name over the follower
	 */
	@ConfigItem(
		keyName = "showLeadName",
		name = "Show lead's name",
		description = "Shows your lead creature's name above it while it follows you, at half opacity. Only you can"
			+ " see it.\nIt shows the nickname you gave it, or the species name when it has none.",
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

	/**
	 * Whether a battle with a wild creature or a City Champion waits for the player to choose an action each turn.
	 *
	 * @return true to choose attack, switch, catch or run every turn; false for battles that play themselves
	 */
	@ConfigItem(
		keyName = "chooseBattleActions",
		name = "Choose battle actions",
		description = "A battle with a wild creature or a City Champion waits for you each turn: Attack, Switch,"
			+ " Catch or Run.\nChoose in the side panel, or with the hotkeys below. With no choice in time, your"
			+ " creature attacks by itself.\nOff means a battle is one click and plays itself, with your lead"
			+ " alone. Friend battles always play themselves.",
		position = 11
	)
	default boolean chooseBattleActions()
	{
		return false;
	}

	/**
	 * How long the player has to choose each turn of a battle they steer.
	 *
	 * @return the seconds
	 */
	@Range(min = 5, max = 30)
	@ConfigItem(
		keyName = "turnSeconds",
		name = "Seconds per turn",
		description = "How long you have to choose before your creature attacks by itself.\nOnly used when Choose"
			+ " battle actions is on.",
		position = 12
	)
	default int turnSeconds()
	{
		return 10;
	}

	/**
	 * Hotkey for Attack while a battle waits for a choice.
	 *
	 * @return the key, unset by default
	 */
	@ConfigItem(
		keyName = "attackKey",
		name = "Attack hotkey",
		description = "Attacks while a battle waits for your choice. Unset by default, so no key is taken from"
			+ " chat or dialogue.",
		position = 13
	)
	default Keybind attackKey()
	{
		return Keybind.NOT_SET;
	}

	/**
	 * Hotkey for Switch while a battle waits for a choice.
	 *
	 * @return the key, unset by default
	 */
	@ConfigItem(
		keyName = "switchKey",
		name = "Switch hotkey",
		description = "Steps in the next creature that can fight while a battle waits for your choice. Unset by"
			+ " default.\nTo pick a particular creature, use the Switch button in the side panel.",
		position = 14
	)
	default Keybind switchKey()
	{
		return Keybind.NOT_SET;
	}

	/**
	 * Hotkey for Catch while a battle waits for a choice.
	 *
	 * @return the key, unset by default
	 */
	@ConfigItem(
		keyName = "catchKey",
		name = "Catch hotkey",
		description = "Throws the selected jar while a battle with a wild creature waits for your choice. Unset by"
			+ " default.",
		position = 15
	)
	default Keybind catchKey()
	{
		return Keybind.NOT_SET;
	}

	/**
	 * Hotkey for Run while a battle waits for a choice.
	 *
	 * @return the key, unset by default
	 */
	@ConfigItem(
		keyName = "runKey",
		name = "Run hotkey",
		description = "Runs away while a battle waits for your choice. Nothing is won and the creature cannot be"
			+ " battled again. Unset by default.",
		position = 16
	)
	default Keybind runKey()
	{
		return Keybind.NOT_SET;
	}

	/**
	 * Hotkey for Item while a battle waits for a choice.
	 *
	 * @return the key, unset by default
	 */
	@ConfigItem(
		keyName = "itemKey",
		name = "Item hotkey",
		description = "Uses the first healing item that would help while a battle waits for your choice: Small"
			+ " salve, Large salve, Revive draught, then Antidote.\nIt costs your turn. To pick the item and the"
			+ " creature, use the Item button in the side panel. Unset by default.",
		position = 17
	)
	default Keybind itemKey()
	{
		return Keybind.NOT_SET;
	}

	/**
	 * When a plain left click on a wild creature is ours.
	 *
	 * @return the choice, not while in combat by default
	 */
	@ConfigItem(
		keyName = "leftClickWild",
		name = "Left-click wild creatures",
		description = "Always: a left click on a wild creature battles or catches it.\nNot while in combat: while you"
			+ " fight a real monster the left click goes to the game, and Battle and Catch stay in the right-click"
			+ " menu.\nNever: Battle and Catch are in the right-click menu only.",
		position = 18
	)
	default LeftClickMode leftClickWild()
	{
		return LeftClickMode.OUT_OF_COMBAT;
	}

	/**
	 * The version the change log was last opened for. Empty on a first install. Not shown in the settings.
	 *
	 * @return the version, or an empty text
	 */
	@ConfigItem(
		keyName = GielinorGoConfig.LAST_SEEN_VERSION,
		name = "Last seen version",
		description = "The plugin version the change log was last opened for.",
		hidden = true
	)
	default String lastSeenVersion()
	{
		return "";
	}

	/**
	 * Whether the player already opened the change log for the current version. Not shown in the settings.
	 *
	 * @return true once the change log was opened
	 */
	@ConfigItem(
		keyName = GielinorGoConfig.WHATS_NEW_DISMISSED,
		name = "What's New dismissed",
		description = "Whether the What's New badge was cleared for the current version.",
		hidden = true
	)
	default boolean whatsNewDismissed()
	{
		return false;
	}

	/**
	 * Whether gathering skills bring wild creatures.
	 *
	 * @return true by default
	 */
	@ConfigItem(
		keyName = "skillingEncounters",
		name = "Skilling encounters",
		description = "Fishing, Woodcutting, Mining, Farming and Hunter XP drops sometimes bring a wild creature"
			+ " beside you. The Wild creature rarity setting scales how often.",
		position = 19
	)
	default boolean skillingEncounters()
	{
		return true;
	}

	/**
	 * Whether short game sounds play for catches, level-ups and other big moments.
	 *
	 * @return true to play them
	 */
	@ConfigItem(
		keyName = "sounds",
		name = "Sounds",
		description = "Plays a short game sound for a catch, a creature breaking free, a level-up, an evolution, a"
			+ " hatched egg,\na gilded creature appearing, a sigil or a milestone. They use the game's sound effect"
			+ " volume.",
		position = 20
	)
	default boolean sounds()
	{
		return true;
	}
}
