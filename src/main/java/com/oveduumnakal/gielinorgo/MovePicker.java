/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * The painted move picker: a {@link Modal} that lists a creature's moves and, when a new one waits, asks whether to
 * skip it or to forget one of the known moves, and the menu that picks the move to forget. It replaces the Swing
 * {@code MoveDialog} in the painted window. A creature may know up to four moves and a dialog cannot lay out five
 * buttons, so the choice is two steps: the dialog offers Skip or Forget one, and Forget one opens a menu with a line
 * for each known move. The words and the answers are built here, apart from any painting, so they are tested without
 * a window. Holds no RuneLite types.
 */
final class MovePicker
{
	/** The button that skips the waiting move; it is the one the dialog starts on. */
	static final String SKIP = "Skip";

	/** The button that goes on to choose a move to forget. */
	static final String FORGET_ONE = "Forget one";

	/** The only button of the dialog when no move waits. */
	static final String CLOSE = "Close";

	/** The answer that skips the waiting move, as {@link PanelActions#resolveMove} reads it. */
	static final int SKIP_ANSWER = -1;

	private MovePicker()
	{
	}

	/**
	 * Words the heading of the dialog.
	 *
	 * @param view the creature
	 * @return {@code New move: Fireball} while one waits, otherwise {@code Moves}
	 */
	static String title(CreatureView view)
	{
		return view.pending().isEmpty() ? "Moves" : "New move: " + view.pending().get(0).name();
	}

	/**
	 * Words the message of the dialog.
	 *
	 * @param view the creature
	 * @return the creature's name, its known moves one to a line, and when a move waits its description and the
	 *         question
	 */
	static String message(CreatureView view)
	{
		StringBuilder text = new StringBuilder(view.nickname() == null ? view.name()
			: view.nickname() + " (" + view.name() + ")");
		for (Move move : view.moves())
			text.append('\n')
				.append(move.name())
				.append(": ")
				.append(move.describe());

		if (view.moves().isEmpty())
			text.append("\nNo moves yet.");

		if (!view.pending().isEmpty())
		{
			Move waiting = view.pending().get(0);
			text.append("\n\nNew: ")
				.append(waiting.name())
				.append(": ")
				.append(waiting.describe());
			text.append("\nSkip it, or forget one of the moves above to learn it.");
		}

		return text.toString();
	}

	/**
	 * Lists the buttons of the dialog.
	 *
	 * @param view the creature
	 * @return {@link #SKIP} and {@link #FORGET_ONE} when a move waits and the creature knows at least one move,
	 *         {@link #SKIP} alone when it knows none, otherwise {@link #CLOSE}
	 */
	static List<String> buttons(CreatureView view)
	{
		if (view.pending().isEmpty())
			return Collections.singletonList(CLOSE);

		return view.moves().isEmpty() ? Collections.singletonList(SKIP) : Arrays.asList(SKIP, FORGET_ONE);
	}

	/**
	 * Builds the dialog.
	 *
	 * @param view the creature
	 * @param resolve called with {@link #SKIP_ANSWER} when the player skips the waiting move
	 * @param forget called when the player chooses to forget one; the caller then opens {@link #forgetItems}
	 * @return the dialog, not yet open; Esc cancels it and nothing is called
	 */
	static Modal modal(CreatureView view, IntConsumer resolve, Runnable forget)
	{
		List<String> buttons = buttons(view);
		return new Modal(title(view), message(view), buttons, 0, index ->
		{
			String chosen = buttons.get(index);
			if (SKIP.equals(chosen))
				resolve.accept(SKIP_ANSWER);
			else if (FORGET_ONE.equals(chosen))
				forget.run();
		}, () ->
		{
		});
	}

	/**
	 * Builds the menu that picks the move to forget.
	 *
	 * @param view the creature
	 * @param resolve called with the place of the known move to forget
	 * @return one item per known move, in order
	 */
	static List<ContextMenu.Item> forgetItems(CreatureView view, IntConsumer resolve)
	{
		List<ContextMenu.Item> items = new ArrayList<>();
		List<Move> moves = view.moves();
		for (int i = 0; i < moves.size(); i++)
		{
			int place = i;
			items.add(ContextMenu.Item.of("Forget " + moves.get(i).name(), () -> resolve.accept(place)));
		}

		return items;
	}
}
