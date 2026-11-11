/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * One friend drawn as a card: name, trainer level, team, buttons to challenge them and to offer a trade, and the
 * trade they are offering, when there is one, with buttons to accept or decline it.
 */
final class FriendCard extends JPanel
{
	FriendCard(FriendView view, PanelActions actions, Consumer<FriendView> trader)
	{
		super(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createEmptyBorder(6, 8, 7, 8));

		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setOpaque(false);
		JLabel name = PanelUi.label(view.name(), FontManager.getRunescapeBoldFont(), Color.WHITE);
		body.add(PanelUi.sides(name, PanelUi.caption("Trainer Lv " + view.trainerLevel())));
		body.add(Box.createVerticalStrut(2));
		String team = view.challengeable() ? view.team() : "No creatures yet";
		body.add(PanelUi.wrapped(team, ColorScheme.LIGHT_GRAY_COLOR));
		JButton challenge = PanelUi.button(view.waiting() ? "Waiting..." : "Challenge",
			e -> actions.challengeFriend(view.memberId()));
		challenge.setEnabled(view.challengeable() && !view.waiting());
		JButton trade = PanelUi.button(view.offered() ? "Offered..." : "Trade", e -> trader.accept(view));
		trade.setEnabled(!view.offered());
		body.add(row(challenge, trade));
		if (!view.offer().isEmpty())
		{
			body.add(Box.createVerticalStrut(6));
			body.add(PanelUi.wrapped("Offers " + view.offer(), PanelUi.GOLD));
			body.add(row(PanelUi.button("Accept", e -> actions.answerTrade(view.memberId(), true)),
				PanelUi.button("Decline", e -> actions.answerTrade(view.memberId(), false))));
		}

		for (Component child : body.getComponents())
			((JComponent) child).setAlignmentX(Component.LEFT_ALIGNMENT);

		add(body, BorderLayout.CENTER);
	}

	private static JPanel row(JButton left, JButton right)
	{
		JPanel row = new JPanel(new GridLayout(1, 2, 4, 0));
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(6, 0, 0, 0));
		row.add(left);
		row.add(right);
		return row;
	}
}
