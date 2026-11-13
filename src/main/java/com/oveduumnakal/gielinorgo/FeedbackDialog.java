/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.runelite.client.util.LinkBrowser;

/**
 * The short forms behind the panel's Feedback button, one for a bug report and one for a feature request. Submit opens
 * GitHub's new-issue page in the browser with the fields filled in; the player reviews and posts it there.
 */
final class FeedbackDialog
{
	private static final String NO_AREA = "(not sure)";
	private static final int FORM_WIDTH = 320;
	private static final int BOX_ROWS = 4;

	/** One text box of a form: what to call it and which template field it fills. */
	private static final class Box
	{
		private final String label;
		private final String key;
		private final JTextArea area = new JTextArea(BOX_ROWS, 28);

		private Box(String label, String key)
		{
			this.label = label;
			this.key = key;
			area.setLineWrap(true);
			area.setWrapStyleWord(true);
		}
	}

	private FeedbackDialog()
	{
	}

	/**
	 * Asks for a bug report and opens GitHub with it.
	 *
	 * @param parent the component the window is centred on
	 */
	static void reportBug(Component parent)
	{
		List<Box> boxes = new ArrayList<>();
		boxes.add(new Box("What happened", "description"));
		boxes.add(new Box("Steps to reproduce", "repro"));
		boxes.add(new Box("What you expected", "expected"));
		Map<String, String> extra = new LinkedHashMap<>();
		String version = Changelog.load().currentVersion();
		if (version != null)
			extra.put(IssueLinks.VERSION_KEY, version);

		ask(parent, "Report a bug", IssueLinks.BUG_TEMPLATE, IssueLinks.BUG_PREFIX, boxes, IssueLinks.BUG_AREAS,
			extra);
	}

	/**
	 * Asks for a feature request and opens GitHub with it.
	 *
	 * @param parent the component the window is centred on
	 */
	static void requestFeature(Component parent)
	{
		List<Box> boxes = new ArrayList<>();
		boxes.add(new Box("The problem or motivation", "problem"));
		boxes.add(new Box("Your idea", "solution"));
		ask(parent, "Request a feature", IssueLinks.FEATURE_TEMPLATE, IssueLinks.FEATURE_PREFIX, boxes,
			IssueLinks.FEATURE_AREAS, new LinkedHashMap<>());
	}

	private static void ask(Component parent, String heading, String template, String prefix, List<Box> boxes,
		List<String> areas, Map<String, String> extra)
	{
		JTextField title = new JTextField();
		JComboBox<String> area = new JComboBox<>();
		area.addItem(NO_AREA);
		for (String option : areas)
			area.addItem(option);

		JPanel form = new JPanel();
		form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
		form.add(row("Title", title));
		for (Box box : boxes)
		{
			JScrollPane scroll = new JScrollPane(box.area);
			scroll.setPreferredSize(new Dimension(FORM_WIDTH, box.area.getPreferredSize().height + 4));
			form.add(row(box.label, scroll));
		}

		form.add(row("Area", area));
		form.add(new JLabel("<html>Each box is cut at " + IssueLinks.MAX_FIELD + " characters.<br>"
			+ "Submit opens GitHub in your browser. Nothing is sent until you post it there.</html>"));

		Object[] buttons = {"Submit", "Cancel"};
		int answer = JOptionPane.showOptionDialog(parent, form, heading, JOptionPane.DEFAULT_OPTION,
			JOptionPane.PLAIN_MESSAGE, null, buttons, buttons[0]);
		if (answer != 0)
			return;

		Map<String, String> fields = new LinkedHashMap<>();
		for (Box box : boxes)
			fields.put(box.key, box.area.getText());

		if (area.getSelectedIndex() > 0)
			fields.put("area", (String) area.getSelectedItem());

		fields.putAll(extra);
		LinkBrowser.browse(IssueLinks.build(template, prefix, title.getText(), fields));
	}

	private static JPanel row(String label, Component field)
	{
		JPanel row = new JPanel(new BorderLayout(0, 2));
		row.add(new JLabel(label), BorderLayout.NORTH);
		row.add(field, BorderLayout.CENTER);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		return row;
	}
}
