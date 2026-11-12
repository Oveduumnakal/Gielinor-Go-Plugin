/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
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
	private Map<ItemType, Integer> items = Collections.emptyMap();

	StorageTab(PanelActions actions)
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
	 * Shows the stored creatures, unless they equal the ones already shown.
	 *
	 * @param storage the stored creatures
	 */
	void update(List<CreatureView> storage)
	{
		header.setText("STORED  " + storage.size() + " / " + SaveData.STORAGE_SIZE);
		if (storage.equals(shown))
			return;

		shown = storage;
		stack.removeAll();
		if (storage.isEmpty())
			stack.add(PanelUi.emptyState(EMPTY_TEXT));

		if (!storage.isEmpty())
			stack.add(PanelUi.hint("Click a creature to move it to your team. Right-click to rename or release."));

		for (int i = 0; i < storage.size(); i++)
		{
			if (i > 0)
				stack.add(Box.createVerticalStrut(1));

			stack.add(row(i, storage.get(i)));
		}

		for (Component child : stack.getComponents())
			((JComponent) child).setAlignmentX(Component.LEFT_ALIGNMENT);

		stack.revalidate();
		stack.repaint();
	}

	/**
	 * Builds one stored creature's line. Clicking it moves the creature onto the team; right-clicking opens a
	 * menu that can also release it.
	 *
	 * @param index the creature's position in storage
	 * @param view the creature
	 * @return the line
	 */
	private JPanel row(int index, CreatureView view)
	{
		JLabel name = PanelUi.label(PanelUi.displayName(view), FontManager.getRunescapeSmallFont(),
			PanelUi.nameColor(view));
		JPanel row = PanelUi.sides(name, PanelUi.caption("Lv " + view.level()));
		row.setOpaque(true);
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(5, 8, 5, 8));
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		row.setToolTipText("<html>" + PanelUi.tooltipName(view) + "<br>" + view.type().label()
			+ " - " + view.rarity().label() + "<br>" + PanelUi.tooltipRolls(view) + "<br>Click to move to your team"
			+ "<br>Right-click to rename or release</html>");
		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				row.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				pressed(e, row, index, view);
			}
		});
		return row;
	}

	private void pressed(MouseEvent event, JPanel row, int index, CreatureView view)
	{
		if (SwingUtilities.isRightMouseButton(event))
		{
			JPopupMenu menu = new JPopupMenu();
			menu.add(PanelUi.menuItem("Move to team", () -> actions.moveToTeam(index)));
			menu.add(PanelUi.menuItem("Rename...", () -> rename(index, view)));
			ItemMenu.addTo(menu, items, view.hpFraction(),
				type -> actions.useItem(TradePick.of(Holding.STORAGE, index, view), type));
			menu.add(PanelUi.menuItem("Release...", () -> confirmRelease(index, PanelUi.displayName(view))));
			menu.show(row, event.getX(), event.getY());
			return;
		}

		if (SwingUtilities.isLeftMouseButton(event))
			actions.moveToTeam(index);
	}

	/**
	 * Tells the tab which healing items the player owns, for the {@code Use} menu of each row.
	 *
	 * @param owned how many of each item the player owns
	 */
	void items(Map<ItemType, Integer> owned)
	{
		items = owned;
	}

	private void rename(int index, CreatureView view)
	{
		String typed = PanelUi.askName(this, view);
		if (typed != null)
			actions.rename(TradePick.of(Holding.STORAGE, index, view), typed);
	}

	private void confirmRelease(int index, String name)
	{
		int answer = JOptionPane.showConfirmDialog(this, "Release " + name + " for good? This cannot be undone.",
			"Release creature", JOptionPane.YES_NO_OPTION);
		if (answer == JOptionPane.YES_OPTION)
			actions.release(index);
	}
}
