/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** One team member drawn as a card: name, level, combat style, health and XP bars, and the action buttons. */
final class CreatureCard extends JPanel
{
	private static final double HP_LOW = 0.25;
	private static final double HP_MID = 0.5;

	CreatureCard(int index, CreatureView view, PanelActions actions)
	{
		super(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(6, 8, 6, 8));

		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setOpaque(false);

		JPanel nameRow = new JPanel(new BorderLayout(4, 0));
		nameRow.setOpaque(false);
		nameRow.add(PanelUi.label(view.name(), FontManager.getRunescapeBoldFont(), view.rarity().color()),
			BorderLayout.CENTER);
		nameRow.add(PanelUi.label("Lv " + view.level(), FontManager.getRunescapeSmallFont(), Color.WHITE),
			BorderLayout.EAST);
		body.add(nameRow);

		JPanel typeRow = new JPanel(new BorderLayout(4, 0));
		typeRow.setOpaque(false);
		typeRow.add(PanelUi.label(view.type().label(), FontManager.getRunescapeSmallFont(),
			ColorScheme.LIGHT_GRAY_COLOR), BorderLayout.CENTER);
		if (index == 0)
			typeRow.add(PanelUi.label("LEAD", FontManager.getRunescapeSmallFont(), ColorScheme.BRAND_ORANGE),
				BorderLayout.EAST);

		body.add(typeRow);
		body.add(Box.createVerticalStrut(4));

		BarComponent hp = new BarComponent(hpColor(view.hpFraction()), 8);
		hp.set(view.hpFraction(), hpColor(view.hpFraction()));
		body.add(PanelUi.barRow("HP", hp));
		body.add(Box.createVerticalStrut(3));

		BarComponent xp = new BarComponent(ColorScheme.BRAND_ORANGE, 8);
		xp.set(view.xpProgress(), ColorScheme.BRAND_ORANGE);
		body.add(PanelUi.barRow("XP", xp));
		body.add(Box.createVerticalStrut(6));

		JPanel buttons = new JPanel(new GridLayout(1, 0, 4, 0));
		buttons.setOpaque(false);
		if (index != 0)
			buttons.add(PanelUi.button("Make lead", e -> actions.setLead(index)));

		buttons.add(PanelUi.button("Store", e -> actions.moveToStorage(index)));
		body.add(buttons);

		add(body, BorderLayout.CENTER);
	}

	private static Color hpColor(double fraction)
	{
		if (fraction > HP_MID)
			return ColorScheme.PROGRESS_COMPLETE_COLOR;

		if (fraction > HP_LOW)
			return ColorScheme.PROGRESS_INPROGRESS_COLOR;

		return ColorScheme.PROGRESS_ERROR_COLOR;
	}
}
