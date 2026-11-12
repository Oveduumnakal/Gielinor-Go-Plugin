/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * The Tasks section of the Team tab: the day's three tasks with the time to the reset, the weekly task, and the
 * streak. The rows are built once and updated in place.
 */
final class TasksSection extends JPanel
{
	private final JLabel dailyHeader = PanelUi.caption("");
	private final JLabel weeklyHeader = PanelUi.caption("");
	private final JLabel streakLine = PanelUi.caption("");
	private final List<TaskRow> dailyRows = new ArrayList<>();
	private final TaskRow weeklyRow = new TaskRow();

	TasksSection()
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(false);
		dailyHeader.setBorder(new EmptyBorder(10, 8, 4, 8));
		weeklyHeader.setBorder(new EmptyBorder(8, 8, 4, 8));
		streakLine.setBorder(new EmptyBorder(6, 8, 4, 8));
		add(left(dailyHeader));
		for (int i = 0; i < TaskCalendar.DAILY_COUNT; i++)
		{
			if (i > 0)
				add(Box.createVerticalStrut(3));

			TaskRow row = new TaskRow();
			dailyRows.add(row);
			add(left(row));
		}

		add(left(weeklyHeader));
		add(left(weeklyRow));
		add(left(streakLine));
		setVisible(false);
	}

	/**
	 * Refreshes every row, the countdowns and the streak line.
	 *
	 * @param view what to show; a view without rows hides the section
	 */
	void update(TaskView view)
	{
		List<TaskView.Row> rows = view.rows();
		setVisible(rows.size() == dailyRows.size() + 1);
		if (!isVisible())
			return;

		dailyHeader.setText("TASKS · resets in " + TaskView.duration(view.millisToReset()));
		weeklyHeader.setText("WEEKLY · resets in " + TaskView.duration(view.millisToWeeklyReset()));
		for (int i = 0; i < dailyRows.size(); i++)
			dailyRows.get(i).update(rows.get(i));

		weeklyRow.update(rows.get(rows.size() - 1));
		streakLine.setText("Streak " + view.streak() + (view.streak() == 1 ? " day" : " days") + " · egg in "
			+ view.eggIn());
		streakLine.setToolTipText("Catch a creature every day. The first catch of each day pays " + GoGame.STREAK_JARS
			+ " Bronze jars, and every " + TaskBoard.STREAK_EGG_EVERY + "th day in a row pays an Uncommon egg.");
	}

	private static <T extends JComponent> T left(T component)
	{
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		return component;
	}
}
