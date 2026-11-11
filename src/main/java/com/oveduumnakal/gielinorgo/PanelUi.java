/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** Small factory methods shared by the side panel's tabs, so every label, button and scroll pane looks alike. */
final class PanelUi
{
	/** The colour of gilded creatures and earned sigils. */
	static final Color GOLD = new Color(0xFFD23F);

	/**
	 * Width a wrapped hint is laid out to, in HTML pixels. Swing draws an HTML pixel about 1.3 screen pixels wide.
	 */
	private static final int HINT_WIDTH = 150;

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
	 * Makes a small grey label for secondary information.
	 *
	 * @param text the text
	 * @return the label
	 */
	static JLabel caption(String text)
	{
		return label(text, FontManager.getRunescapeSmallFont(), ColorScheme.LIGHT_GRAY_COLOR);
	}

	/**
	 * Makes a dim, wrapping line that tells the player how a list works.
	 *
	 * @param text the hint
	 * @return the label
	 */
	static JLabel hint(String text)
	{
		JLabel label = new JLabel("<html><div style='width:" + HINT_WIDTH + "px'>" + text + "</div></html>");
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		label.setBorder(BorderFactory.createEmptyBorder(6, 8, 2, 8));
		return label;
	}

	/**
	 * Puts one component at the left edge of a row and another at the right.
	 *
	 * @param left the component that takes the spare width
	 * @param right the component kept at its own width on the right
	 * @return the row
	 */
	static JPanel sides(JComponent left, JComponent right)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		row.add(left, BorderLayout.CENTER);
		row.add(right, BorderLayout.EAST);
		return row;
	}

	/**
	 * Makes a line for a right-click menu.
	 *
	 * @param text the menu text
	 * @param action what happens when it is chosen
	 * @return the menu item
	 */
	static JMenuItem menuItem(String text, Runnable action)
	{
		JMenuItem item = new JMenuItem(text);
		item.addActionListener(e -> action.run());
		return item;
	}

	/**
	 * Names a creature for display.
	 *
	 * @param view the creature
	 * @return its name, with a "Gilded " prefix for a gilded one
	 */
	static String displayName(CreatureView view)
	{
		if (view.gilded())
			return "Gilded " + view.name();

		return view.name();
	}

	/**
	 * Picks the colour of a creature's name.
	 *
	 * @param view the creature
	 * @return gold for a gilded creature, otherwise its rarity colour
	 */
	static Color nameColor(CreatureView view)
	{
		if (view.gilded())
			return GOLD;

		return view.rarity().color();
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
