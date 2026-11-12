/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

	private static final int TODAY_ROWS = 5;
	private static final Color EVENT_COLOR = new Color(0x4AA3FF);
	private static final String STAR = "\u2605";

	/** Whether the Today section is open, remembered while the plugin runs. */
	private static boolean todayOpen = true;

	private final JLabel todayCaption;
	private final JPanel todayBody;
	private final JLabel[] todayNames = new JLabel[TODAY_ROWS];
	private final JLabel[] todayRarities = new JLabel[TODAY_ROWS];
	private final JPanel[] todayRows = new JPanel[TODAY_ROWS];
	private final JLabel todayEvent;
	private final JLabel todayNight;
	private final JLabel header;
	private final IconTextField search;
	private final JComboBox<LogFilter> filter;
	private final DefaultListModel<LogEntry> model = new DefaultListModel<>();
	private final JList<LogEntry> list;
	private final JScrollPane scroll;
	private List<LogEntry> shown;
	private Set<String> featured = Collections.emptySet();
	private TodayView today = TodayView.NONE;

	LogTab()
	{
		super(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		todayCaption = PanelUi.caption("TODAY");
		todayCaption.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		todayCaption.setToolTipText("Click to open or close");
		todayCaption.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				todayOpen = !todayOpen;
				showToday();
			}
		});
		todayBody = new JPanel();
		todayBody.setLayout(new BoxLayout(todayBody, BoxLayout.Y_AXIS));
		todayBody.setOpaque(false);
		todayBody.setAlignmentX(Component.LEFT_ALIGNMENT);
		for (int i = 0; i < TODAY_ROWS; i++)
		{
			todayNames[i] = PanelUi.caption("");
			todayRarities[i] = PanelUi.caption("");
			todayRows[i] = PanelUi.sides(todayNames[i], todayRarities[i]);
			todayRows[i].setAlignmentX(Component.LEFT_ALIGNMENT);
			todayBody.add(todayRows[i]);
		}

		todayEvent = PanelUi.wrapped("", EVENT_COLOR);
		todayNight = PanelUi.wrapped("", EVENT_COLOR);
		todayEvent.setAlignmentX(Component.LEFT_ALIGNMENT);
		todayNight.setAlignmentX(Component.LEFT_ALIGNMENT);
		todayBody.add(todayEvent);
		todayBody.add(todayNight);

		header = PanelUi.caption("");
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
		todayCaption.setAlignmentX(Component.LEFT_ALIGNMENT);
		top.add(todayCaption);
		top.add(todayBody);
		top.add(Box.createVerticalStrut(6));
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
		updateToday(state.today());
		header.setText("CAUGHT  " + state.caughtSpecies() + " / " + state.totalSpecies() + "   SEEN  "
			+ state.seenSpecies());
		if (state.log() == shown)
			return;

		shown = state.log();
		refill(true);
	}

	private void updateToday(TodayView view)
	{
		boolean changed = !view.featuredKeys().equals(featured);
		today = view;
		featured = view.featuredKeys();
		List<TodayView.Pick> picks = view.featured();
		for (int i = 0; i < TODAY_ROWS; i++)
		{
			boolean has = i < picks.size();
			todayRows[i].setVisible(has && todayOpen);
			if (!has)
				continue;

			TodayView.Pick pick = picks.get(i);
			todayNames[i].setText(pick.name());
			todayNames[i].setForeground(pick.rarity().color());
			todayRarities[i].setText(pick.rarity().label());
			todayRarities[i].setForeground(pick.rarity().color());
		}

		todayEvent.setText(PanelUi.wrappedHtml(view.event() == null ? "" : view.event()));
		todayNight.setText(PanelUi.wrappedHtml(view.night() ? view.nightLine() : ""));
		showToday();
		if (changed && filter.getSelectedItem() == LogFilter.FEATURED)
			refill(false);
	}

	private void showToday()
	{
		todayCaption.setVisible(today.active());
		todayCaption.setText("TODAY \u00B7 resets in " + today.resetText());
		todayBody.setVisible(today.active() && todayOpen);
		for (int i = 0; i < TODAY_ROWS; i++)
			todayRows[i].setVisible(todayOpen && i < today.featured().size());

		todayEvent.setVisible(todayOpen && today.event() != null);
		todayNight.setVisible(todayOpen && today.night());
		todayCaption.revalidate();
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
			if (selected.matches(entry, featured) && matchesQuery(entry, query))
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
		NOT_SEEN("Not seen"),
		FEATURED("Featured");

		private final String label;

		LogFilter(String label)
		{
			this.label = label;
		}

		/**
		 * Tests one species against this filter.
		 *
		 * @param entry the species
		 * @param featured the keys of today's featured species
		 * @return whether the list should show it
		 */
		boolean matches(LogEntry entry, Set<String> featured)
		{
			if (this == FEATURED)
				return featured.contains(Species.keyFor(entry.name()));

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

			String tip = "Lv " + entry.speciesLevel() + " · " + entry.type().label() + " · "
				+ entry.rarity().label();
			if (entry.gilded())
				return tip + " · gilded owned";

			return tip;
		}
	}

	private static String starFor(Font font)
	{
		return font.canDisplay(STAR.charAt(0)) ? STAR + " " : "* ";
	}

	/** Draws one species row. A single instance is reused for every row. */
	private final class LogRenderer extends JPanel implements ListCellRenderer<LogEntry>
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
			String star = featured.contains(Species.keyFor(entry.name())) ? starFor(name.getFont()) : "";
			if (entry.caught())
			{
				name.setText(star + entry.name());
				name.setForeground(entry.rarity().color());
				right.setText("×" + entry.caughtCount());
				right.setForeground(entry.gilded() ? PanelUi.GOLD : ColorScheme.LIGHT_GRAY_COLOR);
			}
			else if (entry.seen())
			{
				name.setText(star + entry.name());
				name.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				right.setText("seen");
				right.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			}
			else
			{
				name.setText(star + "???");
				name.setForeground(UNSEEN_COLOR);
				right.setText(entry.rarity().label());
				right.setForeground(UNSEEN_COLOR);
			}

			return this;
		}
	}
}
