/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** The change log, readable inside the panel: every release, newest first, in a scrolling list with a Back button. */
final class ChangelogView extends JPanel
{
	/** Width the release text is laid out to, in HTML pixels. */
	private static final int TEXT_WIDTH = 160;

	/**
	 * Builds the view.
	 *
	 * @param changelog the releases to show
	 * @param back what happens when the player clicks Back
	 */
	ChangelogView(Changelog changelog, Runnable back)
	{
		super(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		top.setBorder(new EmptyBorder(4, 8, 4, 8));
		top.add(PanelUi.button("Back", e -> back.run()), BorderLayout.WEST);
		top.add(PanelUi.label("Change log", FontManager.getRunescapeBoldFont(), ColorScheme.BRAND_ORANGE),
			BorderLayout.CENTER);
		add(top, BorderLayout.NORTH);

		JPanel list = new JPanel();
		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setBackground(ColorScheme.DARK_GRAY_COLOR);
		list.setBorder(new EmptyBorder(4, 8, 8, 8));
		if (changelog.releases().isEmpty())
			list.add(PanelUi.emptyState("The change log could not be loaded."));

		for (Changelog.Release release : changelog.releases())
		{
			JLabel text = new JLabel(ChangelogHtml.render(release, TEXT_WIDTH));
			text.setFont(FontManager.getRunescapeSmallFont());
			text.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			text.setAlignmentX(Component.LEFT_ALIGNMENT);
			list.add(text);
			list.add(Box.createVerticalStrut(12));
		}

		add(PanelUi.scroll(list), BorderLayout.CENTER);
	}
}
