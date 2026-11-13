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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
	private final JButton select;
	private final JButton releaseSelected;
	private final TreeSet<Integer> ticked = new TreeSet<>();
	private boolean selecting;
	private List<CreatureView> shown;
	private Map<ItemType, Integer> items = Collections.emptyMap();

	StorageTab(PanelActions actions)
	{
		super(new BorderLayout());
		this.actions = actions;
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		header = PanelUi.caption("");
		header.setBorder(new EmptyBorder(6, 8, 6, 8));
		select = PanelUi.button("Select", e -> toggleSelecting());
		releaseSelected = PanelUi.button("Release selected (0)", e -> confirmReleaseSelected());
		releaseSelected.setVisible(false);
		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setOpaque(false);
		JPanel headerRow = PanelUi.sides(header, select);
		headerRow.setBorder(new EmptyBorder(0, 0, 0, 8));
		top.add(headerRow);
		JPanel releaseRow = new JPanel(new BorderLayout());
		releaseRow.setOpaque(false);
		releaseRow.setBorder(new EmptyBorder(0, 8, 6, 8));
		releaseRow.add(releaseSelected, BorderLayout.CENTER);
		top.add(releaseRow);
		add(top, BorderLayout.NORTH);
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
		ticked.clear();
		refreshSelection();
		stack.removeAll();
		if (storage.isEmpty())
			stack.add(PanelUi.emptyState(EMPTY_TEXT));

		if (!storage.isEmpty())
			stack.add(PanelUi.hint(selecting ? "Tick the creatures to release, then press Release selected."
				: "Click a creature to move it to your team. Right-click to rename or release."));

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
		if (selecting)
		{
			JCheckBox box = new JCheckBox();
			box.setOpaque(false);
			box.setFocusable(false);
			box.setSelected(ticked.contains(index));
			box.addActionListener(e -> tick(index, box.isSelected()));
			row.add(box, BorderLayout.WEST);
		}

		row.setOpaque(true);
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(5, 8, 5, 8));
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		row.setToolTipText("<html>" + PanelUi.tooltipName(view) + "<br>" + view.type().label()
			+ " - " + view.rarity().label() + "<br>" + PanelUi.tooltipRolls(view) + "<br>Click to move to your team"
			+ "<br>Releasing pays " + ReleaseRules.xp(view) + " XP to your lead"
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
		if (selecting)
		{
			if (SwingUtilities.isLeftMouseButton(event))
				tick(index, !ticked.contains(index));

			return;
		}

		if (SwingUtilities.isRightMouseButton(event))
		{
			JPopupMenu menu = new JPopupMenu();
			TradePick pick = TradePick.of(Holding.STORAGE, index, view);
			menu.add(PanelUi.menuItem("Move to team", () -> actions.moveToTeam(pick)));
			menu.add(PanelUi.menuItem("Rename...", () -> rename(index, view)));
			ItemMenu.addTo(menu, items, view.hpFraction(),
				type -> actions.useItem(TradePick.of(Holding.STORAGE, index, view), type));
			menu.add(PanelUi.menuItem("Release...", () -> confirmRelease(index, view)));
			menu.show(row, event.getX(), event.getY());
			return;
		}

		if (SwingUtilities.isLeftMouseButton(event))
			actions.moveToTeam(TradePick.of(Holding.STORAGE, index, view));
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

	private void confirmRelease(int index, CreatureView view)
	{
		int answer = JOptionPane.showConfirmDialog(this, "Release " + PanelUi.displayName(view) + " for good? Your lead"
			+ " gains " + ReleaseRules.xp(view) + " XP. This cannot be undone.", "Release creature",
			JOptionPane.YES_NO_OPTION);
		if (answer == JOptionPane.YES_OPTION && askAgain(Collections.singletonList(view)))
			actions.release(TradePick.of(Holding.STORAGE, index, view));
	}

	private void toggleSelecting()
	{
		selecting = !selecting;
		ticked.clear();
		refreshSelection();
		List<CreatureView> now = shown;
		shown = null;
		if (now != null)
			update(now);
	}

	private void tick(int index, boolean on)
	{
		if (on)
			ticked.add(index);
		else
			ticked.remove(index);

		refreshSelection();
		if (shown != null)
			rebuildKeepingTicks();
	}

	private void rebuildKeepingTicks()
	{
		TreeSet<Integer> keep = new TreeSet<>(ticked);
		List<CreatureView> now = shown;
		shown = null;
		update(now);
		ticked.addAll(keep);
		refreshSelection();
	}

	private void refreshSelection()
	{
		select.setText(selecting ? "Done" : "Select");
		releaseSelected.setVisible(selecting);
		releaseSelected.setEnabled(!ticked.isEmpty());
		releaseSelected.setText("Release selected (" + ticked.size() + ")");
	}

	private void confirmReleaseSelected()
	{
		List<TradePick> picks = new ArrayList<>();
		List<CreatureView> views = new ArrayList<>();
		long total = 0;
		StringBuilder names = new StringBuilder();
		for (int index : ticked)
		{
			if (shown == null || index >= shown.size())
				continue;

			CreatureView view = shown.get(index);
			picks.add(TradePick.of(Holding.STORAGE, index, view));
			views.add(view);
			total += ReleaseRules.xp(view);
			names.append("\n" + PanelUi.displayName(view) + " (Lv " + view.level() + ")");
		}

		if (picks.isEmpty())
			return;

		int answer = JOptionPane.showConfirmDialog(this, "Release for good?" + names + "\n\nYour lead gains "
			+ total + " XP. This cannot be undone.", "Release creatures", JOptionPane.YES_NO_OPTION);
		if (answer != JOptionPane.YES_OPTION || !askAgain(views))
			return;

		actions.releaseMany(picks);
		selecting = false;
		ticked.clear();
		refreshSelection();
	}

	/**
	 * Asks a second time when any of the creatures is gilded, rated Strong or Exceptional, or has a nickname.
	 *
	 * @param views the creatures about to go
	 * @return true when none needs it or the player agreed again
	 */
	private boolean askAgain(List<CreatureView> views)
	{
		StringBuilder special = new StringBuilder();
		for (CreatureView view : views)
		{
			if (ReleaseRules.needsSecondConfirm(view))
				special.append("\n").append(PanelUi.displayName(view));
		}

		if (special.length() == 0)
			return true;

		return JOptionPane.showConfirmDialog(this, "These are special (gilded, rated Strong or Exceptional, or"
			+ " named). Really release them?" + special, "Release creature", JOptionPane.YES_NO_OPTION)
			== JOptionPane.YES_OPTION;
	}
}
