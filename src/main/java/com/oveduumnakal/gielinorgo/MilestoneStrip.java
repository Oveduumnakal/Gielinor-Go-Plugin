/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * The milestone strip under the Log tab's header: the next species goal with a bar and a count, and a button that
 * opens the full list. Updated in place.
 */
final class MilestoneStrip extends JPanel
{
	private static final int BAR_HEIGHT = 5;
	private static final int ROW_BAR_HEIGHT = 4;
	private static final int DIALOG_WIDTH = PluginPanel.PANEL_WIDTH - 20;

	private final JLabel next = PanelUi.caption("");
	private final JLabel count = PanelUi.caption("");
	private final BarComponent bar = new BarComponent(ColorScheme.PROGRESS_COMPLETE_COLOR, BAR_HEIGHT);
	private final JButton open;
	private MilestoneView view = MilestoneView.NONE;

	MilestoneStrip()
	{
		setOpaque(false);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		bar.setTrack(ColorScheme.DARKER_GRAY_COLOR);
		open = PanelUi.button("Milestones...", e -> MilestoneStrip.this.openList());
		JPanel line = PanelUi.sides(next, count);
		line.setAlignmentX(Component.LEFT_ALIGNMENT);
		bar.setAlignmentX(Component.LEFT_ALIGNMENT);
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		buttons.setOpaque(false);
		buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
		buttons.add(open);
		add(line);
		add(Box.createVerticalStrut(2));
		add(bar);
		add(Box.createVerticalStrut(4));
		add(buttons);
	}

	/**
	 * Shows the current milestones.
	 *
	 * @param milestones what to show
	 */
	void update(MilestoneView milestones)
	{
		view = milestones;
		boolean any = milestones.nextLabel() != null;
		next.setText(any ? milestones.nextLabel() : "All species milestones reached");
		count.setText(any ? milestones.nextCurrent() + " / " + milestones.nextTarget() : "");
		bar.set(any ? (double) milestones.nextCurrent() / milestones.nextTarget() : 1.0,
			ColorScheme.PROGRESS_COMPLETE_COLOR);
		setVisible(!milestones.rows().isEmpty());
	}

	private void openList()
	{
		MilestoneStrip.show(this, view);
	}

	/**
	 * Builds the list of every milestone with its bar, reward and a tick when paid.
	 *
	 * @param milestones what to list
	 * @return the list, ready to put in a window
	 */
	static JPanel list(MilestoneView milestones)
	{
		JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(ColorScheme.DARK_GRAY_COLOR);
		body.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		boolean family = false;
		List<MilestoneView.Row> rows = milestones.rows();
		for (MilestoneView.Row row : rows)
		{
			if (row.family() && !family)
			{
				family = true;
				body.add(Box.createVerticalStrut(6));
				body.add(PanelUi.caption("FAMILIES"));
			}

			body.add(Box.createVerticalStrut(4));
			body.add(rowOf(row));
		}

		return body;
	}

	private static JPanel rowOf(MilestoneView.Row row)
	{
		Color color = row.paid() ? ColorScheme.PROGRESS_COMPLETE_COLOR : ColorScheme.LIGHT_GRAY_COLOR;
		String tick = row.paid() ? "✓ " : "";
		JPanel box = new JPanel();
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
		box.setOpaque(false);
		box.setAlignmentX(Component.LEFT_ALIGNMENT);
		JLabel title = PanelUi.label(tick + row.label(), FontManager.getRunescapeSmallFont(),
			color);
		JLabel counts = PanelUi.caption(row.current() + " / " + row.target());
		BarComponent progress = new BarComponent(color, ROW_BAR_HEIGHT);
		progress.setTrack(ColorScheme.DARKER_GRAY_COLOR);
		progress.set((double) row.current() / Math.max(1, row.target()), color);
		String waiting = row.reached() && !row.paid() ? " (waiting for an egg slot)" : "";
		JLabel reward = PanelUi.wrapped(row.reward() + waiting, ColorScheme.MEDIUM_GRAY_COLOR);
		JPanel top = PanelUi.sides(title, counts);
		top.setAlignmentX(Component.LEFT_ALIGNMENT);
		progress.setAlignmentX(Component.LEFT_ALIGNMENT);
		reward.setAlignmentX(Component.LEFT_ALIGNMENT);
		box.add(top);
		box.add(progress);
		box.add(reward);
		box.setMaximumSize(new Dimension(DIALOG_WIDTH, Integer.MAX_VALUE));
		return box;
	}

	/**
	 * Opens the list of milestones in a window.
	 *
	 * @param parent the component the window is centred on
	 * @param milestones what to list
	 */
	static void show(Component parent, MilestoneView milestones)
	{
		JScrollPane pane = PanelUi.scroll(list(milestones));
		pane.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH + 40, 360));
		JOptionPane.showMessageDialog(parent, pane, "Milestones",
			JOptionPane.PLAIN_MESSAGE);
	}
}
