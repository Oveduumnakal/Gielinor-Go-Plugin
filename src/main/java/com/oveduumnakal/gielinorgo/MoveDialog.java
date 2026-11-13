/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.JOptionPane;

/**
 * The small dialog of a creature's moves: it lists the moves the creature knows and, when a new one waits, asks which
 * known move to forget or whether to skip the new one. The words are built apart from the dialog so they can be
 * tested.
 */
final class MoveDialog
{
	/** The text of the button that skips a waiting move. */
	static final String SKIP = "Skip";

	private MoveDialog()
	{
	}

	/**
	 * Words the message of the dialog.
	 *
	 * @param view the creature
	 * @return HTML: the known moves with their numbers and, when a move waits, the new move and the question
	 */
	static String message(CreatureView view)
	{
		StringBuilder text = new StringBuilder("<html><b>" + PanelUi.tooltipName(view) + "</b><br>");
		for (Move move : view.moves())
			text.append(PanelUi.escape(move.name()) + ": " + move.describe() + "<br>");

		if (view.pending().isEmpty())
			return text.append("</html>").toString();

		Move waiting = view.pending().get(0);
		text.append("<br><b>New move: " + PanelUi.escape(waiting.name()) + "</b><br>" + waiting.describe()
			+ "<br><br>Pick a move to forget, or skip the new one.</html>");
		return text.toString();
	}

	/**
	 * Lists the buttons of the dialog when a move waits: one per known move, then {@link #SKIP}.
	 *
	 * @param view the creature
	 * @return the button texts, empty when no move waits
	 */
	static List<String> options(CreatureView view)
	{
		List<String> options = new ArrayList<>();
		if (view.pending().isEmpty())
			return options;

		for (Move move : view.moves())
			options.add("Forget " + move.name());

		options.add(SKIP);
		return options;
	}

	/**
	 * Shows the dialog and reports the player's choice.
	 *
	 * @param parent the component to centre the dialog on
	 * @param view the creature
	 * @param resolve called with the place of the known move to forget, or -1 to skip the new move; never called
	 *     when no move waits or the dialog is closed
	 */
	static void show(Component parent, CreatureView view, IntConsumer resolve)
	{
		List<String> options = options(view);
		if (options.isEmpty())
		{
			JOptionPane.showMessageDialog(parent, message(view), "Moves", JOptionPane.PLAIN_MESSAGE);
			return;
		}

		int picked = JOptionPane.showOptionDialog(parent, message(view), "A new move", JOptionPane.DEFAULT_OPTION,
			JOptionPane.PLAIN_MESSAGE, null, options.toArray(), SKIP);
		if (picked < 0)
			return;

		resolve.accept(picked >= view.moves().size() ? -1 : picked);
	}
}
