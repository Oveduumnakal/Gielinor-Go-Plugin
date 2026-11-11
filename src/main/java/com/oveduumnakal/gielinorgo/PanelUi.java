/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** Small factory methods shared by the side panel's tabs, so every label, button and scroll pane looks alike. */
final class PanelUi
{
	private PanelUi()
	{
	}

	/**
	 * Makes a plain label.
	 *
	 * @param text the text
	 * @param font the font
	 * @param color the text colour
	 * @return the label
	 */
	static JLabel label(String text, Font font, Color color)
	{
		JLabel label = new JLabel(text);
		label.setFont(font);
		label.setForeground(color);
		return label;
	}

	/**
	 * Makes a compact button.
	 *
	 * @param text the button text
	 * @param listener what happens on a click
	 * @return the button
	 */
	static JButton button(String text, ActionListener listener)
	{
		JButton button = new JButton(text);
		button.setFont(FontManager.getRunescapeSmallFont());
		button.setFocusable(false);
		button.setMargin(new Insets(2, 4, 2, 4));
		button.addActionListener(listener);
		return button;
	}

	/**
	 * Makes a centred, wrapping message for a tab with nothing to list.
	 *
	 * @param text the message
	 * @return the label
	 */
	static JLabel emptyState(String text)
	{
		JLabel label = new JLabel("<html><div style='text-align:center;width:170px'>" + text + "</div></html>");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setAlignmentX(Component.CENTER_ALIGNMENT);
		label.setBorder(BorderFactory.createEmptyBorder(16, 8, 16, 8));
		return label;
	}

	/**
	 * Puts a short caption in front of a bar.
	 *
	 * @param caption the caption, such as {@code HP}
	 * @param bar the bar
	 * @return the combined row
	 */
	static JPanel barRow(String caption, BarComponent bar)
	{
		JPanel row = new JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		JLabel label = label(caption, FontManager.getRunescapeSmallFont(), ColorScheme.LIGHT_GRAY_COLOR);
		label.setPreferredSize(new Dimension(22, 10));
		row.add(label, BorderLayout.WEST);
		row.add(bar, BorderLayout.CENTER);
		return row;
	}

	/**
	 * Wraps a component in a vertical-only scroll pane that matches the panel's colours.
	 *
	 * @param view the scrolled component
	 * @return the scroll pane
	 */
	static JScrollPane scroll(JComponent view)
	{
		JScrollPane pane = new JScrollPane(view, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		pane.setBorder(null);
		pane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		JScrollBar bar = pane.getVerticalScrollBar();
		bar.setUnitIncrement(16);
		return pane;
	}
}
