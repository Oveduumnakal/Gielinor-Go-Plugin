/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;

/**
 * The Log tab: the Creature Log as one scrolling list with a search box and a filter. The list uses a single
 * shared cell renderer, so ~1,260 species cost almost nothing to draw.
 */
final class LogTab extends JPanel
{
	private static final int ROW_HEIGHT = 22;
	private static final Color UNSEEN_COLOR = new Color(0x6A6A6A);

	private final JLabel header;
	private final IconTextField search;
	private final JComboBox<LogFilter> filter;
	private final DefaultListModel<LogEntry> model = new DefaultListModel<>();
	private final JList<LogEntry> list;
	private final JScrollPane scroll;
	private List<LogEntry> shown;

	LogTab()
	{
		super(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		header = PanelUi.label("", FontManager.getRunescapeBoldFont(), ColorScheme.LIGHT_GRAY_COLOR);
		search = new IconTextField();
		search.setIcon(IconTextField.Icon.SEARCH);
		search.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 28));
		search.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		search.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		search.getDocument().addDocumentListener(new RefilterListener());
		filter = new JComboBox<>(LogFilter.values());
		filter.addActionListener(e -> refill(false));

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setOpaque(false);
		top.setBorder(new EmptyBorder(6, 8, 6, 8));
		header.setAlignmentX(Component.LEFT_ALIGNMENT);
		search.setAlignmentX(Component.LEFT_ALIGNMENT);
		filter.setAlignmentX(Component.LEFT_ALIGNMENT);
		top.add(header);
		top.add(Box.createVerticalStrut(4));
		top.add(search);
		top.add(Box.createVerticalStrut(4));
		top.add(filter);
		add(top, BorderLayout.NORTH);

		list = new LogList(model);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setFixedCellHeight(ROW_HEIGHT);
		list.setCellRenderer(new LogRenderer());
		list.setBackground(ColorScheme.DARK_GRAY_COLOR);
		ToolTipManager.sharedInstance().registerComponent(list);
		scroll = PanelUi.scroll(list);
		add(scroll, BorderLayout.CENTER);
	}

	/**
	 * Refreshes the header and, when the log instance changed, the list.
	 *
	 * @param state the new panel state
	 */
	void update(PanelState state)
	{
		header.setText(state.caughtSpecies() + " / " + state.totalSpecies() + " caught · "
			+ state.seenSpecies() + " seen");
		if (state.log() == shown)
			return;

		shown = state.log();
		refill(true);
	}

	private void refill(boolean keepScroll)
	{
		if (shown == null)
			return;

		JScrollBar bar = scroll.getVerticalScrollBar();
		int position = bar.getValue();
		String typed = search.getText().trim();
		String query = typed.toLowerCase(Locale.ROOT);
		LogFilter selected = (LogFilter) filter.getSelectedItem();
		List<LogEntry> matches = new ArrayList<>();
		for (LogEntry entry : shown)
		{
			if (selected.matches(entry) && matchesQuery(entry, query))
				matches.add(entry);
		}

		model.clear();
		model.addAll(matches);
		if (keepScroll)
			SwingUtilities.invokeLater(() -> bar.setValue(position));
	}

	private static boolean matchesQuery(LogEntry entry, String query)
	{
		if (query.isEmpty())
			return true;

		return isKnown(entry) && entry.name().toLowerCase(Locale.ROOT).contains(query);
	}

	private static boolean isKnown(LogEntry entry)
	{
		return entry.seen() || entry.caught();
	}

	/** Which species the list shows. */
	enum LogFilter
	{
		ALL("All"),
		CAUGHT("Caught"),
		SEEN_ONLY("Seen only"),
		NOT_SEEN("Not seen");

		private final String label;

		LogFilter(String label)
		{
			this.label = label;
		}

		/**
		 * Tests one species against this filter.
		 *
		 * @param entry the species
		 * @return whether the list should show it
		 */
		boolean matches(LogEntry entry)
		{
			if (this == CAUGHT)
				return entry.caught();

			if (this == SEEN_ONLY)
				return entry.seen() && !entry.caught();

			if (this == NOT_SEEN)
				return !isKnown(entry);

			return true;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	/** Re-runs the filter whenever the search text changes. */
	private final class RefilterListener implements DocumentListener
	{
		@Override
		public void insertUpdate(DocumentEvent e)
		{
			refill(false);
		}

		@Override
		public void removeUpdate(DocumentEvent e)
		{
			refill(false);
		}

		@Override
		public void changedUpdate(DocumentEvent e)
		{
			refill(false);
		}
	}

	/** The log list: always as wide as the viewport, and with a tooltip for every species the player knows. */
	private static final class LogList extends JList<LogEntry>
	{
		LogList(DefaultListModel<LogEntry> model)
		{
			super(model);
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public String getToolTipText(MouseEvent event)
		{
			int index = locationToIndex(event.getPoint());
			if (index < 0 || !getCellBounds(index, index).contains(event.getPoint()))
				return null;

			LogEntry entry = getModel().getElementAt(index);
			if (!isKnown(entry))
				return null;

			return "Lv " + entry.speciesLevel() + " · " + entry.type().label() + " · "
				+ entry.rarity().label();
		}
	}

	/** Draws one species row. A single instance is reused for every row. */
	private static final class LogRenderer extends JPanel implements ListCellRenderer<LogEntry>
	{
		private final JLabel name = new JLabel();
		private final JLabel right = new JLabel();

		LogRenderer()
		{
			super(new BorderLayout(4, 0));
			setBorder(new EmptyBorder(0, 8, 0, 8));
			name.setFont(FontManager.getRunescapeSmallFont());
			right.setFont(FontManager.getRunescapeSmallFont());
			add(name, BorderLayout.CENTER);
			add(right, BorderLayout.EAST);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends LogEntry> list, LogEntry entry, int index,
			boolean selected, boolean focused)
		{
			setBackground(index % 2 == 0 ? ColorScheme.DARK_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR);
			if (entry.caught())
			{
				name.setText(entry.name());
				name.setForeground(entry.rarity().color());
				right.setText("×" + entry.caughtCount());
				right.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			}
			else if (entry.seen())
			{
				name.setText(entry.name());
				name.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				right.setText("seen");
				right.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			}
			else
			{
				name.setText("???");
				name.setForeground(UNSEEN_COLOR);
				right.setText(entry.rarity().label());
				right.setForeground(UNSEEN_COLOR);
			}

			return this;
		}
	}
}
