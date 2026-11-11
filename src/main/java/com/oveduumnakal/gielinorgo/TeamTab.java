/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BorderLayout;
import java.util.List;
import javax.swing.Box;
import javax.swing.JPanel;

import net.runelite.client.ui.ColorScheme;

/** The Team tab: one {@link CreatureCard} per team member, rebuilt only when the team actually changes. */
final class TeamTab extends JPanel
{
	private static final String EMPTY_TEXT = "No creatures yet. Right-click an outlined monster and choose Catch.";

	private final PanelActions actions;
	private final JPanel stack;
	private List<CreatureView> shown;

	TeamTab(PanelActions actions)
	{
		super(new BorderLayout());
		this.actions = actions;
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		PanelColumn column = new PanelColumn();
		stack = column.stack();
		add(PanelUi.scroll(column), BorderLayout.CENTER);
	}

	/**
	 * Shows a team, unless it equals the one already shown.
	 *
	 * @param team the team, lead first
	 */
	void update(List<CreatureView> team)
	{
		if (team.equals(shown))
			return;

		shown = team;
		stack.removeAll();
		if (team.isEmpty())
			stack.add(PanelUi.emptyState(EMPTY_TEXT));

		for (int i = 0; i < team.size(); i++)
		{
			if (i > 0)
				stack.add(Box.createVerticalStrut(6));

			stack.add(new CreatureCard(i, team.get(i), actions));
		}

		stack.revalidate();
		stack.repaint();
	}
}
