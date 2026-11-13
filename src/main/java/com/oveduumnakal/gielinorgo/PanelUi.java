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
import javax.swing.JOptionPane;
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

	/**
	 * Width the wrapped text of a card is laid out to, in HTML pixels, so it fills the card without running past
	 * its edge.
	 */
	private static final int CARD_TEXT_WIDTH = 140;

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
	 * Makes a small label for a card whose text wraps instead of running past the card's edge.
	 *
	 * @param text the text, shown as written
	 * @param color the text colour
	 * @return the label
	 */
	static JLabel wrapped(String text, Color color)
	{
		JLabel label = new JLabel(wrappedHtml(text));
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(color);
		return label;
	}

	/**
	 * Builds the HTML a {@link #wrapped} label shows, for changing its text later.
	 *
	 * @param text the text, shown as written
	 * @return the text escaped and laid out to the card width
	 */
	static String wrappedHtml(String text)
	{
		return "<html><div style='width:" + CARD_TEXT_WIDTH + "px'>" + escape(text) + "</div></html>";
	}

	/**
	 * Makes text safe to put inside an HTML label, so a name can never be read as markup.
	 *
	 * @param text the text
	 * @return the text with its HTML special characters replaced
	 */
	static String escape(String text)
	{
		String result = text.replace("&", "&amp;");
		result = result.replace("<", "&lt;");
		return result.replace(">", "&gt;");
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
	 * @return its nickname when it has one, otherwise its species name, with a "Gilded " prefix for a gilded one
	 */
	static String displayName(CreatureView view)
	{
		String shown = view.nickname() == null ? view.name() : view.nickname();
		if (view.gilded())
			return "Gilded " + shown;

		return shown;
	}

	/**
	 * Asks the player for a creature's nickname.
	 *
	 * @param parent the component the window is centred on
	 * @param view the creature
	 * @return what the player typed, an empty string to reset the name, or null when they cancelled
	 */
	static String askName(Component parent, CreatureView view)
	{
		Object typed = JOptionPane.showInputDialog(parent,
			"Name for your " + view.name() + " (1 to " + Nickname.MAX_LENGTH
				+ " letters, numbers, spaces or -). Leave it empty to reset.",
			"Rename creature", JOptionPane.PLAIN_MESSAGE, null, null,
			view.nickname() == null ? "" : view.nickname());
		return typed == null ? null : typed.toString();
	}

	/**
	 * Builds the first line of a creature's tooltip: its nickname and species, safe to put in an HTML label.
	 *
	 * @param view the creature
	 * @return the nickname and species for a nicknamed creature, otherwise the species name
	 */
	static String tooltipName(CreatureView view)
	{
		if (view.nickname() == null)
			return escape(view.name());

		return escape(view.nickname()) + " (" + escape(view.name()) + ")";
	}

	/**
	 * Words the bond line of a creature's tooltip.
	 *
	 * @param view the creature
	 * @return a line break and the bond line, or an empty string while it has no hearts
	 */
	static String tooltipBond(CreatureView view)
	{
		return view.hearts() > 0 ? "<br>" + BondRules.describe(view.hearts()) : "";
	}

	/**
	 * Picks the colour of a rating word.
	 *
	 * @param rating the rating
	 * @return gold for exceptional, green for strong, otherwise grey
	 */
	static Color ratingColor(StatRolls.Rating rating)
	{
		if (rating == StatRolls.Rating.EXCEPTIONAL)
			return GOLD;

		return rating == StatRolls.Rating.STRONG ? ColorScheme.PROGRESS_COMPLETE_COLOR : ColorScheme.LIGHT_GRAY_COLOR;
	}

	/**
	 * Writes the hidden numbers of a creature for a tooltip.
	 *
	 * @param view the creature
	 * @return three lines of HTML: the rating and temperament with its effect, then the four rolls with the
	 *     percentage each stat really gets, two to a line
	 */
	static String tooltipRolls(CreatureView view)
	{
		return view.rating().label() + " \u00b7 " + view.temperamentLabel() + ": " + view.temperamentEffect()
			+ "<br>" + view.rollsNetLines();
	}

	/**
	 * Says what a creature's species is good or bad at, for its tooltip.
	 *
	 * @param view the creature
	 * @return HTML lines starting with a line break, or an empty text when the species is not known
	 */
	static String tooltipSpecies(CreatureView view)
	{
		if (view.speciesFactors().isEmpty())
			return "";

		String words = view.speciesWords();
		return "<br>Species: " + (words.isEmpty() ? "all-round" : words) + " (" + view.speciesFactors() + ")";
	}

	/**
	 * Lists a creature's moves for its tooltip, one line each.
	 *
	 * @param view the creature
	 * @return HTML lines starting with a line break, or an empty text when the creature has no moves
	 */
	static String tooltipMoves(CreatureView view)
	{
		StringBuilder text = new StringBuilder();
		for (Move move : view.moves())
			text.append("<br>" + escape(move.name()) + ": " + move.describe());

		if (!view.pending().isEmpty())
			text.append("<br>Wants to learn " + escape(view.pending().get(0).name()));

		return text.toString();
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
