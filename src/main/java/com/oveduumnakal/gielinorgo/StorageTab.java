/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** The Storage tab: a compact row for every stored creature, rebuilt only when the stored list changes. */
final class StorageTab extends JPanel
{
	private static final String EMPTY_TEXT = "Storage is empty. Creatures go here when your team is full.";

	private final PanelActions actions;
	private final JPanel stack;
	private final JLabel header;
	private List<CreatureView> shown;

	StorageTab(PanelActions actions)
	{
		super(new BorderLayout());
		this.actions = actions;
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		header = PanelUi.label("", FontManager.getRunescapeBoldFont(), ColorScheme.LIGHT_GRAY_COLOR);
		header.setBorder(new EmptyBorder(6, 8, 6, 8));
		add(header, BorderLayout.NORTH);
		PanelColumn column = new PanelColumn();
		stack = column.stack();
		add(PanelUi.scroll(column), BorderLayout.CENTER);
	}

	/**
	 * Shows the stored creatures, unless they equal the ones already shown.
	 *
	 * @param storage the stored creatures
	 */
	void update(List<CreatureView> storage)
	{
		header.setText(storage.size() + " / " + SaveData.STORAGE_SIZE + " stored");
		if (storage.equals(shown))
			return;

		shown = storage;
		stack.removeAll();
		if (storage.isEmpty())
			stack.add(PanelUi.emptyState(EMPTY_TEXT));

		for (int i = 0; i < storage.size(); i++)
		{
			if (i > 0)
				stack.add(Box.createVerticalStrut(3));

			stack.add(row(i, storage.get(i)));
		}

		stack.revalidate();
		stack.repaint();
	}

	private JPanel row(int index, CreatureView view)
	{
		JPanel row = new JPanel(new BorderLayout(0, 3));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(4, 8, 4, 8));

		JPanel top = new JPanel(new BorderLayout(4, 0));
		top.setOpaque(false);
		top.add(PanelUi.label(view.name(), FontManager.getRunescapeSmallFont(), view.rarity().color()),
			BorderLayout.CENTER);
		top.add(PanelUi.label("Lv " + view.level(), FontManager.getRunescapeSmallFont(),
			ColorScheme.LIGHT_GRAY_COLOR), BorderLayout.EAST);
		row.add(top, BorderLayout.NORTH);

		JPanel buttons = new JPanel(new GridLayout(1, 2, 4, 0));
		buttons.setOpaque(false);
		buttons.add(PanelUi.button("To team", e -> actions.moveToTeam(index)));
		buttons.add(PanelUi.button("Release", e -> confirmRelease(index, view.name())));
		row.add(buttons, BorderLayout.CENTER);
		return row;
	}

	private void confirmRelease(int index, String name)
	{
		int answer = JOptionPane.showConfirmDialog(this, "Release " + name + " for good? This cannot be undone.",
			"Release creature", JOptionPane.YES_NO_OPTION);
		if (answer == JOptionPane.YES_OPTION)
			actions.release(index);
	}
}
