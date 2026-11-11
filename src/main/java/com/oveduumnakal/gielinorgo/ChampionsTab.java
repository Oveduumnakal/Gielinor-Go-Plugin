/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BorderLayout;
import java.util.List;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.runelite.client.ui.ColorScheme;

/** The Cities tab: one {@link ChampionCard} per City Champion, rebuilt only when the champions change. */
final class ChampionsTab extends JPanel
{
	private static final String EMPTY_TEXT = "No City Champions are available.";

	private final PanelActions actions;
	private final JPanel stack;
	private final JLabel header;
	private List<ChampionView> shown;

	ChampionsTab(PanelActions actions)
	{
		super(new BorderLayout());
		this.actions = actions;
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		header = PanelUi.caption("");
		header.setBorder(new EmptyBorder(6, 8, 6, 8));
		add(header, BorderLayout.NORTH);
		PanelColumn column = new PanelColumn();
		stack = column.stack();
		add(PanelUi.scroll(column), BorderLayout.CENTER);
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

		header.setText("SIGILS  " + beaten + " / " + champions.size());
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
