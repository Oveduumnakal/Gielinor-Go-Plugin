/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The rival as a card on the Battle tab: name, what the rival says, the team for the open battle, the reward and a
 * Challenge button. When no battle is open it says what opens the next one.
 */
final class RivalCard extends JPanel
{
	private static final int STRIPE = 3;

	RivalCard(RivalView view, PanelActions actions)
	{
		super(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		boolean ready = view.mode() == RivalView.Mode.READY;
		Color stripe = view.mode() == RivalView.Mode.BEATEN ? PanelUi.GOLD : ColorScheme.DARKER_GRAY_COLOR;
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, STRIPE, 0, 0, ready ? ColorScheme.PROGRESS_COMPLETE_COLOR : stripe),
			BorderFactory.createEmptyBorder(6, 6, 7, 8)));

		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setOpaque(false);
		JLabel name = PanelUi.label(view.name(), FontManager.getRunescapeBoldFont(), Color.WHITE);
		body.add(PanelUi.sides(name, PanelUi.caption("Battle " + view.battle() + " / " + RivalRules.BATTLES)));
		body.add(Box.createVerticalStrut(2));
		if (view.line() != null)
			body.add(PanelUi.wrapped("\"" + view.line() + "\"", ColorScheme.LIGHT_GRAY_COLOR));

		if (view.team() != null)
			body.add(PanelUi.wrapped(view.team(), ColorScheme.LIGHT_GRAY_COLOR));

		body.add(PanelUi.wrapped(view.status(), ready ? ColorScheme.PROGRESS_COMPLETE_COLOR : PanelUi.GOLD));
		if (view.reward() != null)
		{
			JLabel reward = PanelUi.label("Win: " + view.reward(), FontManager.getRunescapeSmallFont(),
				ColorScheme.MEDIUM_GRAY_COLOR);
			if (view.rewardJar() != null)
			{
				reward.setIcon(JarIcons.swing(view.rewardJar()));
				reward.setIconTextGap(4);
				reward.setHorizontalTextPosition(JLabel.RIGHT);
				reward.setHorizontalAlignment(JLabel.LEFT);
			}

			body.add(reward);
		}

		if (ready)
		{
			JPanel buttonRow = new JPanel(new BorderLayout());
			buttonRow.setOpaque(false);
			buttonRow.setBorder(new EmptyBorder(6, 0, 0, 0));
			buttonRow.add(PanelUi.button("Challenge", e -> actions.challenge(RivalRules.ID)), BorderLayout.CENTER);
			body.add(buttonRow);
		}

		for (Component child : body.getComponents())
			((JComponent) child).setAlignmentX(Component.LEFT_ALIGNMENT);

		add(body, BorderLayout.CENTER);
	}
}
