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

	/** Key of the setting that shows the painted window's jar button. */

	/** Key of the painted window's size preset. */
	String WINDOW_SIZE = "windowSize";

	/** Key of the hidden item that stores the painted window's width as a share of the canvas width. */
	String WINDOW_FRACTION = "windowFraction";

	/** The key of {@link #pinParty()}. */
	String PIN_PARTY = "pinParty";

	/**
	 * How rare wild creatures are.
	 *
	 * @return N, where a monster turns wild one time in N
	 */
	@Range(min = 2, max = 50)
	@ConfigItem(
		keyName = "wildChance",
		name = "Wild creature rarity",
		description = "A nearby Common monster turns wild one time in this many.\n"
			+ "Stronger monsters are rarer: Legendary ones 20 times less often.\n"
			+ "Lower numbers mean more wild creatures.",
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
		description = "Your character makes a throwing motion when you throw a jar,\nthen cheers for a catch,"
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
			+ " and clicks pass through it.",
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
			+ " Catch or Run.\nChoose with the buttons on the game screen, or with the hotkeys below.\nWith no choice"
			+ " in time, your creature attacks by itself.\nOff means a battle is one click and plays itself, with your"
			+ " lead alone.\nA friend battle is played turn by turn only when both players have this on.",
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
			+ " default.\nTo pick a particular creature, use the Switch button in the battle controls box.",
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
		description = "Runs away while a battle waits for your choice.\nNothing is won and the creature cannot be"
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
		description = "Uses the first healing item that would help while a battle waits for your choice: \nSmall"
			+ " salve, Large salve, Revive draught, then Antidote.\nIt costs your turn. To pick the item and the"
			+ " creature, use the Item button in the battle controls box. Unset by default.",
		position = 17
	)
	default Keybind itemKey()
	{
		return Keybind.NOT_SET;
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
			+ " beside you.\nThe Wild creature rarity setting scales how often.",
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

	/**
	 * How much the painted window moves.
	 *
	 * @return the motion mode, full by default
	 */
	@ConfigItem(
		keyName = "windowAnimations",
		name = "Window animations",
		description = "Full: the window eases open, tabs slide and icons pulse.\nReduced: only fades.\nOff: nothing"
			+ " moves.",
		position = 22
	)
	default MotionMode windowAnimations()
	{
		return MotionMode.FULL;
	}

	/**
	 * Whether the Summary page plays the creature's biome behind it.
	 *
	 * @return true to show the backgrounds
	 */
	@ConfigItem(
		keyName = "animatedBackgrounds",
		name = "Animated backgrounds",
		description = "Plays the creature's home biome behind it on the Summary page.\n"
			+ "Window animations set to Reduced or Off holds it still.",
		position = 40
	)
	default boolean animatedBackgrounds()
	{
		return true;
	}

	/**
	 * The size of the painted window, as a share of the game screen's width.
	 *
	 * @return the size preset, small by default
	 */
	@ConfigItem(
		keyName = GielinorGoConfig.WINDOW_SIZE,
		name = "Window size",
		description = "Sets the painted window to a size and centres it.\nYou can also drag the window's edge or"
			+ " its bottom-right corner,\nor Alt + right-click the window to reset it.",
		position = 23
	)
	default WindowSizePreset windowSize()
	{
		return WindowSizePreset.SMALL;
	}

	/**
	 * Whether the painted window takes the arrow keys, Enter, Space, Esc and Backspace while it has focus.
	 *
	 * @return true to use keys in the window
	 */
	@ConfigItem(
		keyName = "windowKeyboard",
		name = "Window keyboard",
		description = "Lets you use the painted window from the keyboard\n(or a controller mapped to keys)."
			+ "\nLeft and Right change tab, Up and Down move the highlight, Enter or Space select, Esc goes back."
			+ "\nKeys go to the window only while it has focus; click the window to give it focus."
			+ "\nChat typing is never touched.",
		position = 25
	)
	default boolean windowKeyboard()
	{
		return true;
	}

	/**
	 * The key that opens and closes the painted window.
	 *
	 * @return the key, unset by default
	 */
	@ConfigItem(
		keyName = "windowKey",
		name = "Open window key",
		description = "Opens and closes the painted window. Unset by default, so no key is taken from the game.",
		position = 26
	)
	default Keybind windowKey()
	{
		return Keybind.NOT_SET;
	}

	/**
	 * Whether hovering a wild creature shows a small tooltip about it.
	 *
	 * @return true to show the tooltip
	 */
	@ConfigItem(
		keyName = "wildTooltip",
		name = "Wild creature tooltip",
		description = "Shows a small tooltip with the name, level, style and Creature Log record when you hover a wild"
			+ " creature.\nIt hides while a menu is open and during a battle, and while its bubbles show.",
		position = 29
	)
	default boolean wildTooltip()
	{
		return true;
	}

	/**
	 * Whether the pop-out Catch, Battle and Info bubbles show above a wild creature.
	 *
	 * @return true to show the bubbles
	 */
	@ConfigItem(
		keyName = "wildBubbles",
		name = "Wild creature bubbles",
		description = "Shows Catch, Battle and Info bubbles above a wild creature when your mouse comes near it.\n"
			+ "Off: use Battle and Catch in the creature's right-click menu instead.",
		position = 30
	)
	default boolean wildBubbles()
	{
		return true;
	}

	/**
	 * Whether bubbles pop out over the lead creature that follows the player.
	 *
	 * @return true to show them
	 */
	@ConfigItem(
		keyName = "followerBubbles",
		name = "Follower bubbles",
		description = "Shows Switch, Item and Run bubbles above your lead creature when your mouse rests on it.\n"
			+ "Run shows only in a wild battle.",
		position = 39
	)
	default boolean followerBubbles()
	{
		return true;
	}

	/**
	 * Whether XP gained while the plugin was not watching trains the team at login.
	 *
	 * @return true to count XP gained while away
	 */
	@ConfigItem(
		keyName = "offlineProgress",
		name = "Count XP gained while away",
		description = "Counts the XP you gained since the last time you played with this plugin,\n"
			+ "for example on mobile, and trains your team with it when you log in.\n"
			+ "No web request is made. The XP is read from the game when you log in.",
		position = 32
	)
	default boolean offlineProgress()
	{
		return true;
	}

	/**
	 * Whether the pinned tracker box shows on the game screen.
	 *
	 * @return true to show the tracker
	 */
	@ConfigItem(
		keyName = "showTracker",
		name = "Pinned tracker",
		description = "Shows the tasks and eggs you pinned in a small box on the game screen.\n"
			+ "Pin them with the pin button on the Tasks and Bag tabs.\n"
			+ "Hold RuneLite's drag key to move it.",
		position = 33
	)
	default boolean showTracker()
	{
		return true;
	}

	/**
	 * Whether a newly acquired egg is pinned to the on-screen tracker.
	 *
	 * @return true to pin new eggs while the tracker has room
	 */
	@ConfigItem(
		keyName = "autoPinEggs",
		name = "Auto Pin Eggs",
		description = "Pins each new egg to the Pinned tracker, while it has room.\n"
			+ "Eggs you already carry are left as they are.",
		position = 38
	)
	default boolean autoPinEggs()
	{
		return false;
	}

	/**
	 * Whether the party box shows on the game screen.
	 *
	 * @return true to pin the party to the game screen
	 */
	@ConfigItem(
		keyName = GielinorGoConfig.PIN_PARTY,
		name = "Pin Party to UI",
		description = "Shows your team, with portraits, names, levels and health, in a box on the game screen.\n"
			+ "The Party page has a Pin Party to UI button too.\n"
			+ "Drag one creature onto another in the box to swap them.\n"
			+ "Hold RuneLite's drag key to move it.",
		position = 34
	)
	default boolean pinParty()
	{
		return false;
	}

	/**
	 * Whether the lead creature wanders near the player when the player stands still.
	 *
	 * @return true to let the lead wander
	 */
	@ConfigItem(
		keyName = "followerWander",
		name = "Lead wanders when you stop",
		description = "When you stand still, your lead creature wanders around within 3 tiles of you\n"
			+ "instead of waiting one tile behind you. Only you can see it.",
		position = 31
	)
	default boolean followerWander()
	{
		return true;
	}

	/**
	 * Whether the plugin's windows, boxes and bubbles are painted as frosted glass.
	 *
	 * @return true for the glass look, false for the solid look
	 */
	@ConfigItem(
		keyName = "glassLook",
		name = "Glass look",
		description = "Shows the windows, boxes and bubbles as tinted glass the game shows through."
			+ "\nOff: the solid look.",
		position = 35
	)
	default boolean glassLook()
	{
		return true;
	}

	/**
	 * The metal of the thin border the Glass look draws round buttons and pop-ups.
	 *
	 * @return the border, bronze by default
	 */
	@ConfigItem(
		keyName = "glassBorder",
		name = "Glass border",
		description = "The thin metal edge round buttons, tooltips and menus in the Glass look.\nBronze: warm brass."
			+ "\nSteel blue: the blue of the Info card's edge.",
		position = 37
	)
	default GlassBorder glassBorder()
	{
		return GlassBorder.BRONZE;
	}

	/**
	 * Whether skilling counts toward hatching eggs.
	 *
	 * @return true when an XP drop in a skill that is not a combat skill counts as one step for the eggs
	 */
	@ConfigItem(
		keyName = "skillingHatchesEggs",
		name = "Skilling hatches eggs",
		description = "An XP drop in a skill that is not a combat skill counts as one step for the eggs you carry."
			+ "\nAt most one a game tick and 1,200 an hour; walking is much faster and still counts for jars, tasks"
			+ " and bond.\nOff: only walking hatches eggs.",
		position = 36
	)
	default boolean skillingHatchesEggs()
	{
		return true;
	}

	/**
	 * The share of the game screen's width the painted window takes. It follows the game window. Set by dragging the
	 * window's edge and by the size presets; not shown in the settings.
	 *
	 * @return the fraction, 70% by default
	 */
	@ConfigItem(
		keyName = GielinorGoConfig.WINDOW_FRACTION,
		name = "Window width share",
		description = "Share of the game screen's width the painted window takes.",
		hidden = true
	)
	default double windowFraction()
	{
		return WindowFit.DEFAULT_FRACTION;
	}
}
