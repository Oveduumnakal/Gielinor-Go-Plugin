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
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** One City Champion drawn as a card: city, team, first-win reward, status and a challenge button. */
final class ChampionCard extends JPanel
{
	private static final int STRIPE = 3;
	private static final Color HERE_COLOR = ColorScheme.PROGRESS_COMPLETE_COLOR;

	ChampionCard(ChampionView view, PanelActions actions)
	{
		super(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		Color stripe = view.beaten() ? PanelUi.GOLD : ColorScheme.DARKER_GRAY_COLOR;
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, STRIPE, 0, 0, view.here() && !view.beaten() ? HERE_COLOR : stripe),
			BorderFactory.createEmptyBorder(6, 6, 7, 8)));

		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setOpaque(false);
		JLabel city = PanelUi.label(view.city(), FontManager.getRunescapeBoldFont(), Color.WHITE);
		body.add(PanelUi.sides(city, statusLabel(view)));
		body.add(Box.createVerticalStrut(2));
		body.add(PanelUi.wrapped(view.team(), ColorScheme.LIGHT_GRAY_COLOR));
		body.add(PanelUi.wrapped("First win: " + view.reward(), ColorScheme.MEDIUM_GRAY_COLOR));
		if (view.here())
		{
			JButton button = PanelUi.button(view.beaten() ? "Rematch" : "Challenge",
				e -> actions.challenge(view.id()));
			JPanel buttonRow = new JPanel(new BorderLayout());
			buttonRow.setOpaque(false);
			buttonRow.setBorder(new EmptyBorder(6, 0, 0, 0));
			buttonRow.add(button, BorderLayout.CENTER);
			body.add(buttonRow);
		}
		else
		{
			setToolTipText("Go to " + view.city() + " to challenge its champion");
		}

		for (Component child : body.getComponents())
			((JComponent) child).setAlignmentX(Component.LEFT_ALIGNMENT);

		add(body, BorderLayout.CENTER);
	}

	/**
	 * Makes the short status shown beside the city name.
	 *
	 * @param view the champion
	 * @return "Sigil" in gold once beaten, "Here" in green while the player is in the city, otherwise nothing
	 */
	private static JLabel statusLabel(ChampionView view)
	{
		if (view.beaten())
			return PanelUi.label("Sigil", FontManager.getRunescapeSmallFont(), PanelUi.GOLD);

		if (view.here())
			return PanelUi.label("Here", FontManager.getRunescapeSmallFont(), HERE_COLOR);

		return PanelUi.caption("");
	}
}
