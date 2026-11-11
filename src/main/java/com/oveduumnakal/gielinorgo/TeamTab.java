/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.runelite.client.ui.ColorScheme;

/**
 * The Team tab: one {@link CreatureCard} per team member, then the carried eggs. Cards are rebuilt only when the
 * team changes; egg rows are rebuilt only when the egg count or rarities change, and otherwise updated in place.
 */
final class TeamTab extends JPanel
{
	private static final String EMPTY_TEXT = "No creatures yet. Right-click an outlined monster and choose Catch.";

	private final PanelActions actions;
	private final PortraitSource portraits;
	private final JPanel cards = new JPanel();
	private final JPanel eggBox = new JPanel();
	private final JLabel eggHeader;
	private final JPanel eggRows = new JPanel();
	private final List<EggRow> rows = new ArrayList<>();
	private final List<CreatureCard> slots = new ArrayList<>();
	private final JLabel hint = PanelUi.hint("Click a creature to make it your lead. Right-click for more.");

	TeamTab(PanelActions actions, PortraitSource portraits)
	{
		super(new BorderLayout());
		this.actions = actions;
		this.portraits = portraits;
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		PanelColumn column = new PanelColumn();
		JPanel stack = column.stack();
		cards.setLayout(new BoxLayout(cards, BoxLayout.Y_AXIS));
		cards.setOpaque(false);
		eggBox.setLayout(new BoxLayout(eggBox, BoxLayout.Y_AXIS));
		eggBox.setOpaque(false);
		eggRows.setLayout(new BoxLayout(eggRows, BoxLayout.Y_AXIS));
		eggRows.setOpaque(false);
		eggHeader = PanelUi.caption("");
		eggHeader.setBorder(new EmptyBorder(10, 8, 4, 8));
		cards.setAlignmentX(Component.LEFT_ALIGNMENT);
		eggBox.setAlignmentX(Component.LEFT_ALIGNMENT);
		eggRows.setAlignmentX(Component.LEFT_ALIGNMENT);
		eggBox.add(eggHeader);
		eggBox.add(eggRows);
		eggBox.setVisible(false);
		hint.setAlignmentX(Component.LEFT_ALIGNMENT);
		stack.add(cards);
		stack.add(hint);
		stack.add(eggBox);
		add(PanelUi.scroll(column), BorderLayout.CENTER);
	}

	/**
	 * Shows the team and the eggs, rebuilding only what changed.
	 *
	 * @param team the team, lead first
	 * @param eggs the carried eggs
	 */
	void update(List<CreatureView> team, List<EggView> eggs)
	{
		updateCards(team);
		updateEggs(eggs);
	}

	/**
	 * Shows the team. The cards are only rebuilt when the team grows or shrinks; otherwise each card is updated
	 * where it stands, so nothing flickers while health and XP change.
	 *
	 * @param team the team, lead first
	 */
	private void updateCards(List<CreatureView> team)
	{
		hint.setVisible(team.size() > 1);
		if (team.size() != slots.size() || (team.isEmpty() && cards.getComponentCount() == 0))
		{
			slots.clear();
			cards.removeAll();
			if (team.isEmpty())
				cards.add(PanelUi.emptyState(EMPTY_TEXT));

			for (int i = 0; i < team.size(); i++)
			{
				if (i > 0)
					cards.add(Box.createVerticalStrut(4));

				CreatureCard card = new CreatureCard(actions, portraits);
				slots.add(card);
				cards.add(card);
			}

			cards.revalidate();
			cards.repaint();
		}

		for (int i = 0; i < team.size(); i++)
			slots.get(i).update(i, team.get(i));
	}

	private void updateEggs(List<EggView> eggs)
	{
		eggBox.setVisible(!eggs.isEmpty());
		eggHeader.setText("EGGS  " + eggs.size() + " / " + SaveData.MAX_EGGS);
		if (sameShape(eggs))
		{
			for (int i = 0; i < eggs.size(); i++)
				rows.get(i).update(eggs.get(i));

			return;
		}

		rows.clear();
		eggRows.removeAll();
		for (int i = 0; i < eggs.size(); i++)
		{
			if (i > 0)
				eggRows.add(Box.createVerticalStrut(3));

			EggRow row = new EggRow(eggs.get(i));
			rows.add(row);
			eggRows.add(row);
		}

		eggBox.revalidate();
		eggBox.repaint();
	}

	private boolean sameShape(List<EggView> eggs)
	{
		if (eggs.size() != rows.size())
			return false;

		for (int i = 0; i < eggs.size(); i++)
		{
			if (eggs.get(i).rarity() != rows.get(i).rarity())
				return false;
		}

		return true;
	}
}
