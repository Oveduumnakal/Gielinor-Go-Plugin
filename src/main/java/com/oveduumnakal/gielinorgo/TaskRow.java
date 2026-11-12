/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * One task: its words, a progress bar and the count. Built once and updated in place, so nothing flickers while
 * the progress moves. A finished task turns green.
 */
final class TaskRow extends JPanel
{
	private final JLabel name;
	private final JLabel count;
	private final BarComponent bar;

	TaskRow()
	{
		super(new BorderLayout(0, 3));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(4, 8, 4, 8));
		Font font = FontManager.getRunescapeSmallFont();
		JPanel top = new JPanel(new BorderLayout(4, 0));
		top.setOpaque(false);
		name = PanelUi.label("", font, Color.WHITE);
		count = PanelUi.label("", font, ColorScheme.LIGHT_GRAY_COLOR);
		top.add(name, BorderLayout.CENTER);
		top.add(count, BorderLayout.EAST);
		add(top, BorderLayout.NORTH);
		bar = new BarComponent(ColorScheme.BRAND_ORANGE, 4);
		add(bar, BorderLayout.CENTER);
	}

	/**
	 * Refreshes the words, count, bar and colours.
	 *
	 * @param row the task's current state
	 */
	void update(TaskView.Row row)
	{
		name.setText(row.label());
		String tip = "Reward: " + row.reward();
		setToolTipText(tip);
		name.setToolTipText(tip);
		count.setToolTipText(tip);
		bar.setToolTipText(tip);
		bar.set(row.fraction(), row.done() ? ColorScheme.PROGRESS_COMPLETE_COLOR : ColorScheme.BRAND_ORANGE);
		count.setText(row.done() ? "Done" : row.count());
		count.setForeground(row.done() ? ColorScheme.PROGRESS_COMPLETE_COLOR : Color.WHITE);
		name.setForeground(row.done() ? ColorScheme.PROGRESS_COMPLETE_COLOR : Color.WHITE);
	}
}
