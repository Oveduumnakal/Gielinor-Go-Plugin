/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
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
 * The Elite challenge as a card under the City Champions: locked until all eight sigils are earned, then a pointer
 * to the Champions' Guild, a Challenge button at the guild, and during a run the battle that is next with Continue
 * and Forfeit.
 */
final class EliteCard extends JPanel
{
	private static final int STRIPE = 3;

	EliteCard(EliteView view, PanelActions actions)
	{
		super(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		boolean live = view.mode() == EliteView.Mode.HERE || view.mode() == EliteView.Mode.RUNNING;
		Color stripe = view.titled() ? PanelUi.GOLD : ColorScheme.DARKER_GRAY_COLOR;
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, STRIPE, 0, 0, live ? ColorScheme.PROGRESS_COMPLETE_COLOR : stripe),
			BorderFactory.createEmptyBorder(6, 6, 7, 8)));
		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setOpaque(false);
		body.add(PanelUi.sides(PanelUi.label("Elite challenge", FontManager.getRunescapeBoldFont(), Color.WHITE),
			status(view)));
		body.add(Box.createVerticalStrut(2));
		body.add(PanelUi.wrapped(describe(view), ColorScheme.LIGHT_GRAY_COLOR));
		body.add(PanelUi.wrapped("Reward: " + view.reward(), ColorScheme.MEDIUM_GRAY_COLOR));
		if (view.mode() == EliteView.Mode.HERE)
			body.add(buttons(PanelUi.button(view.titled() ? "Challenge again" : "Challenge",
				e -> actions.eliteChallenge())));

		if (view.mode() == EliteView.Mode.RUNNING)
			body.add(buttons(PanelUi.button("Continue", e -> actions.eliteContinue()),
				PanelUi.button("Forfeit", e -> actions.eliteForfeit())));

		for (Component child : body.getComponents())
			((JComponent) child).setAlignmentX(Component.LEFT_ALIGNMENT);

		add(body, BorderLayout.CENTER);
	}

	private static JLabel status(EliteView view)
	{
		if (view.mode() == EliteView.Mode.RUNNING)
			return PanelUi.label("Elite " + view.stage() + " of " + view.stages(), FontManager.getRunescapeSmallFont(),
				ColorScheme.PROGRESS_COMPLETE_COLOR);

		if (view.titled())
			return PanelUi.label("Grand Champion", FontManager.getRunescapeSmallFont(), PanelUi.GOLD);

		return PanelUi.caption("");
	}

	/**
	 * Words what the player can do about the challenge right now.
	 *
	 * @param view the card
	 * @return a short line, or two for a run that is open
	 */
	static String describe(EliteView view)
	{
		switch (view.mode())
		{
			case LOCKED:
				return "Earn all " + EliteRun.SIGILS_NEEDED + " sigils · " + view.sigils() + " / "
					+ EliteRun.SIGILS_NEEDED;
			case AWAY:
				return "Go to the Champions' Guild";
			case HERE:
				return "Four Elite trainers and the Grand Champion, back to back. Health carries over and nothing "
					+ "rests until the run ends.";
			default:
				return view.opponent() + ": " + view.team();
		}
	}

	private static JPanel buttons(JButton... buttons)
	{
		JPanel row = new JPanel(new GridLayout(1, buttons.length, 4, 0));
		row.setOpaque(false);
		row.setBorder(new EmptyBorder(6, 0, 0, 0));
		for (JButton button : buttons)
			row.add(button);

		return row;
	}
}
