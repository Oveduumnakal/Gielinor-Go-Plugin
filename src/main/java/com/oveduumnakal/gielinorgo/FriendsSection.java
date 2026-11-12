/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Component;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * The friends part of the Battle tab: one {@link FriendCard} per party member who also plays, rebuilt only when
 * something about them changes. With nobody to list, one line says what to do next.
 */
final class FriendsSection extends JPanel
{
	private static final String NO_PARTY_TEXT = "Join a RuneLite party with a friend to battle their team.";
	private static final String NOBODY_TEXT = "Nobody else in your party is playing Gielinor Go yet.";
	private static final String HINT_TEXT = "Battles are for fun: nothing is won or lost. Trades are for keeps.";

	private final PanelActions actions;
	private final Consumer<FriendView> trader;
	private FriendsState shown;

	FriendsSection(PanelActions actions, Consumer<FriendView> trader)
	{
		this.actions = actions;
		this.trader = trader;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(false);
		update(FriendsState.none());
	}

	/**
	 * Shows the friends, unless they equal the ones already shown.
	 *
	 * @param state the party and the friends in it
	 */
	void update(FriendsState state)
	{
		if (state.equals(shown))
			return;

		shown = state;
		removeAll();
		JLabel header = PanelUi.caption(state.friends().isEmpty() ? "FRIENDS" : "FRIENDS  " + state.friends().size());
		header.setBorder(new EmptyBorder(6, 8, 0, 8));
		add(header);
		if (!state.inParty())
			add(PanelUi.hint(NO_PARTY_TEXT));
		else if (state.friends().isEmpty())
			add(PanelUi.hint(NOBODY_TEXT));
		else
			add(PanelUi.hint(HINT_TEXT));

		for (FriendView friend : state.friends())
		{
			add(Box.createVerticalStrut(4));
			add(new FriendCard(friend, actions, trader));
		}

		for (Component child : getComponents())
			((JComponent) child).setAlignmentX(Component.LEFT_ALIGNMENT);

		revalidate();
		repaint();
	}
}
