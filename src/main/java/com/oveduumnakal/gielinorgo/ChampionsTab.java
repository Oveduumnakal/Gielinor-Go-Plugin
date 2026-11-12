/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.runelite.client.ui.ColorScheme;

/**
 * The Battle tab: the friends who can be battled, then one {@link ChampionCard} per City Champion. Each part is
 * rebuilt only when what it shows changes.
 */
final class ChampionsTab extends JPanel
{
	private static final String EMPTY_TEXT = "No City Champions are available.";

	private final PanelActions actions;
	private final FriendsSection friends;
	private final JPanel stack = new JPanel();
	private final JLabel header;
	private List<ChampionView> shown;

	ChampionsTab(PanelActions actions, Consumer<FriendView> trader)
	{
		super(new BorderLayout());
		this.actions = actions;
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		friends = new FriendsSection(actions, trader);
		header = PanelUi.caption("");
		header.setBorder(new EmptyBorder(10, 8, 6, 8));
		stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
		stack.setOpaque(false);
		PanelColumn column = new PanelColumn();
		for (JComponent part : new JComponent[] {friends, header, stack})
		{
			part.setAlignmentX(Component.LEFT_ALIGNMENT);
			column.stack().add(part);
		}

		add(PanelUi.scroll(column), BorderLayout.CENTER);
	}

	/**
	 * Shows the friends in the player's party, unless they equal the ones already shown.
	 *
	 * @param state the party and the friends in it
	 */
	void updateFriends(FriendsState state)
	{
		friends.update(state);
	}

	/**
	 * Shows the champions, unless they equal the ones already shown.
	 *
	 * @param champions the champions, in display order
	 */
	void update(List<ChampionView> champions)
	{
		int beaten = 0;
		for (ChampionView champion : champions)
		{
			if (champion.beaten())
				beaten++;
		}

		header.setText("CITY CHAMPIONS  " + beaten + " / " + champions.size() + " sigils");
		if (champions.equals(shown))
			return;

		shown = champions;
		stack.removeAll();
		if (champions.isEmpty())
			stack.add(PanelUi.emptyState(EMPTY_TEXT));

		for (int i = 0; i < champions.size(); i++)
		{
			if (i > 0)
				stack.add(Box.createVerticalStrut(4));

			stack.add(new ChampionCard(champions.get(i), actions));
		}

		stack.revalidate();
		stack.repaint();
	}
}
