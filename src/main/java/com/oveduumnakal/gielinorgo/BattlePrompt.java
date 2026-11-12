/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.List;

/** The words of the small on-screen prompt shown while a battle waits for a choice. It draws nothing itself. */
final class BattlePrompt
{
	private BattlePrompt()
	{
	}

	/**
	 * Words the prompt.
	 *
	 * @param view the battle that waits
	 * @param hotkeys the hotkeys the player set, such as {@code Attack F1 · Run F4}, or empty when none is set
	 * @return the lines, top to bottom: the title with the time left, both creatures, and the hotkeys when set
	 */
	static List<String> lines(BattleView view, String hotkeys)
	{
		List<String> lines = new ArrayList<>();
		String time = " · " + view.secondsLeft() + "s";
		lines.add(view.forcedSwitch() ? "Pick your next creature" + time : "Your turn" + time);
		lines.add(view.mineName() + " Lv " + view.mineLevel() + " " + percent(view.mineHp()));
		lines.add(view.foeName() + " Lv " + view.foeLevel() + " " + percent(view.foeHp()));
		if (!hotkeys.isEmpty())
			lines.add(hotkeys);

		return lines;
	}

	/**
	 * Lists the hotkeys that are set.
	 *
	 * @param attack the Attack hotkey text, or null when unset
	 * @param swap the Switch hotkey text, or null when unset
	 * @param jar the Catch hotkey text, or null when unset
	 * @param run the Run hotkey text, or null when unset
	 * @param catching whether a jar can be thrown, so the Catch hotkey is worth showing
	 * @param forced whether the next creature must be picked, so only Switch counts
	 * @return the hotkeys joined with a dot, or an empty text when none applies
	 */
	static String hotkeys(String attack, String swap, String jar, String run, boolean catching, boolean forced)
	{
		List<String> parts = new ArrayList<>();
		if (swap != null)
			parts.add("Switch " + swap);

		if (!forced && attack != null)
			parts.add(0, "Attack " + attack);

		if (!forced && catching && jar != null)
			parts.add("Catch " + jar);

		if (!forced && run != null)
			parts.add("Run " + run);

		return String.join(" · ", parts);
	}

	private static String percent(double fraction)
	{
		return Math.round(fraction * 100) + "%";
	}
}
