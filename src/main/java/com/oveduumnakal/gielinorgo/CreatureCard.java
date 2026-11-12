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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * One team member drawn as a slim card: portrait, name, level, combat style, and thin health and XP bars. There
 * are no buttons: clicking a card makes that creature the lead, and right-clicking opens a short menu. The card
 * is updated in place, so it never flickers and keeps its hover highlight while numbers change.
 */
final class CreatureCard extends JPanel
{
	private static final double HP_LOW = 0.25;
	private static final double HP_MID = 0.5;
	private static final int HP_BAR_HEIGHT = 5;
	private static final int XP_BAR_HEIGHT = 3;
	private static final int STRIPE = 3;
	private static final Color XP_COLOR = new Color(0x6FB7FF);

	private final PanelActions actions;
	private final PortraitSource portraits;
	private final JLabel portrait = new JLabel();
	private final JLabel name = PanelUi.label("", FontManager.getRunescapeBoldFont(), Color.WHITE);
	private final JLabel level = PanelUi.label("", FontManager.getRunescapeSmallFont(), Color.WHITE);
	private final JLabel style = PanelUi.caption("");
	private final JLabel badge = PanelUi.label("", FontManager.getRunescapeSmallFont(), ColorScheme.BRAND_ORANGE);
	private final BarComponent hp = new BarComponent(ColorScheme.PROGRESS_COMPLETE_COLOR, HP_BAR_HEIGHT);
	private final BarComponent xp = new BarComponent(XP_COLOR, XP_BAR_HEIGHT);
	private Map<ItemType, Integer> items = Collections.emptyMap();
	private int index;
	private CreatureView shown;
	private String portraitOf;

	CreatureCard(PanelActions actions, PortraitSource portraits)
	{
		super(new BorderLayout(8, 0));
		this.actions = actions;
		this.portraits = portraits;
		setBackground(ColorScheme.DARKER_GRAY_COLOR);

		portrait.setPreferredSize(new Dimension(PortraitService.SIZE, PortraitService.SIZE));
		add(portrait, BorderLayout.WEST);

		JPanel text = new JPanel();
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.setOpaque(false);
		text.add(PanelUi.sides(name, level));
		text.add(Box.createVerticalStrut(1));
		text.add(PanelUi.sides(style, badge));
		text.add(Box.createVerticalStrut(5));
		text.add(hp);
		text.add(Box.createVerticalStrut(2));
		text.add(xp);
		hp.setMaximumSize(new Dimension(Integer.MAX_VALUE, HP_BAR_HEIGHT));
		xp.setMaximumSize(new Dimension(Integer.MAX_VALUE, XP_BAR_HEIGHT));
		for (Component child : text.getComponents())
			((JComponent) child).setAlignmentX(Component.LEFT_ALIGNMENT);

		add(text, BorderLayout.CENTER);
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				pressed(e);
			}
		});
	}

	/**
	 * Shows a creature on this card.
	 *
	 * @param teamIndex the creature's position in the team; 0 is the lead
	 * @param view the creature
	 */
	void update(int teamIndex, CreatureView view)
	{
		index = teamIndex;
		shown = view;
		boolean isLead = teamIndex == 0;
		name.setText(PanelUi.displayName(view));
		name.setForeground(PanelUi.nameColor(view));
		level.setText("Lv " + view.level());
		style.setText(view.type().label() + " \u00b7 " + view.temperamentLabel());
		showBadge(view, isLead);
		hp.set(view.hpFraction(), hpColor(view.hpFraction()));
		xp.set(view.xpProgress(), XP_COLOR);
		Color stripe = isLead ? ColorScheme.BRAND_ORANGE : ColorScheme.DARKER_GRAY_COLOR;
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, STRIPE, 0, 0, view.gilded() ? PanelUi.GOLD : stripe),
			BorderFactory.createEmptyBorder(6, 6, 7, 8)));
		setCursor(Cursor.getPredefinedCursor(isLead ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
		setToolTipText("<html>" + PanelUi.tooltipName(view) + "<br>" + PanelUi.tooltipRolls(view) + "<br>Health "
			+ Math.round(view.hpFraction() * 100) + "%<br>"
			+ Math.round(view.xpProgress() * 100) + "% to level " + Math.min(XpRules.MAX_LEVEL, view.level() + 1)
			+ "<br>" + (isLead ? "Your lead: fights battles and trains twice as fast" : "Click to make lead")
			+ "<br>" + view.type().matchup() + "<br>Right-click for more</html>");
		if (view.name().equals(portraitOf))
			return;

		portraitOf = view.name();
		portrait.setIcon(null);
		String wanted = portraitOf;
		portraits.request(wanted, image -> showPortrait(wanted, image));
	}

	/**
	 * Shows the small word on the right of the second line: the rating when it is not average, otherwise "Lead" for
	 * the lead creature. A rated lead shows its rating only, because both words do not fit beside the style and
	 * temperament; the orange stripe and the tooltip still mark it as the lead.
	 *
	 * @param view the creature
	 * @param isLead whether it is the lead
	 */
	private void showBadge(CreatureView view, boolean isLead)
	{
		StatRolls.Rating rating = view.rating();
		if (rating != StatRolls.Rating.AVERAGE)
		{
			badge.setText(rating.label());
			badge.setForeground(PanelUi.ratingColor(rating));
			badge.setVisible(true);
			return;
		}

		badge.setText("Lead");
		badge.setForeground(ColorScheme.BRAND_ORANGE);
		badge.setVisible(isLead);
	}

	/**
	 * Puts a finished picture on the card, unless the card has moved on to another creature in the meantime.
	 *
	 * @param speciesName the species the picture is of
	 * @param image the picture
	 */
	private void showPortrait(String speciesName, BufferedImage image)
	{
		if (speciesName.equals(portraitOf))
			portrait.setIcon(new ImageIcon(image));
	}

	private void pressed(MouseEvent event)
	{
		if (SwingUtilities.isRightMouseButton(event))
		{
			JPopupMenu menu = new JPopupMenu();
			if (index != 0)
				menu.add(PanelUi.menuItem("Make lead", () -> actions.setLead(index)));

			menu.add(PanelUi.menuItem("Move to storage", () -> actions.moveToStorage(index)));
			menu.add(PanelUi.menuItem("Rename...", this::rename));
			ItemMenu.addTo(menu, items, shown.hpFraction(),
				type -> actions.useItem(TradePick.of(Holding.TEAM, index, shown), type));
			menu.show(this, event.getX(), event.getY());
			return;
		}

		if (SwingUtilities.isLeftMouseButton(event) && index != 0)
			actions.setLead(index);
	}

	/**
	 * Tells the card which healing items the player owns, for its {@code Use} menu.
	 *
	 * @param owned how many of each item the player owns
	 */
	void items(Map<ItemType, Integer> owned)
	{
		items = owned;
	}

	/** Asks for a new nickname for the creature on this card and hands it to the game. */
	private void rename()
	{
		String typed = PanelUi.askName(this, shown);
		if (typed != null)
			actions.rename(TradePick.of(Holding.TEAM, index, shown), typed);
	}

	private static Color hpColor(double fraction)
	{
		if (fraction > HP_MID)
			return ColorScheme.PROGRESS_COMPLETE_COLOR;

		if (fraction > HP_LOW)
			return ColorScheme.PROGRESS_INPROGRESS_COLOR;

		return ColorScheme.PROGRESS_ERROR_COLOR;
	}
}
