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
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * One starter creature on offer to a new player: portrait, name, level, combat style, temperament and rating, and
 * what it grows into. The whole card is a button; a click asks once and then takes the creature.
 */
final class StarterCard extends JPanel
{
	private static final int STRIPE = 3;

	private final PanelActions actions;
	private final int index;
	private final String speciesName;

	StarterCard(PanelActions actions, PortraitSource portraits, int index, StarterView starter)
	{
		super(new BorderLayout(8, 0));
		this.actions = actions;
		this.index = index;
		CreatureView view = starter.creature();
		speciesName = view.name();
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, STRIPE, 0, 0, ColorScheme.BRAND_ORANGE),
			BorderFactory.createEmptyBorder(6, 6, 7, 8)));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setToolTipText("<html>" + PanelUi.tooltipName(view) + "<br>" + PanelUi.tooltipRolls(view) + "<br>"
			+ view.type().matchup() + "<br>Click to choose</html>");
		JLabel portrait = new JLabel();
		portrait.setPreferredSize(new Dimension(PortraitService.SIZE, PortraitService.SIZE));
		add(portrait, BorderLayout.WEST);
		JLabel name = PanelUi.label(view.name(), FontManager.getRunescapeBoldFont(), Color.WHITE);
		JLabel level = PanelUi.label("Lv " + view.level(), FontManager.getRunescapeSmallFont(), Color.WHITE);
		JLabel style = PanelUi.caption(view.type().label() + " · " + view.temperamentLabel());
		JLabel rating = PanelUi.label(view.rating().label(), FontManager.getRunescapeSmallFont(),
			PanelUi.ratingColor(view.rating()));
		JLabel grows = PanelUi.wrapped(starter.growsInto(), ColorScheme.MEDIUM_GRAY_COLOR);
		JPanel text = new JPanel();
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.setOpaque(false);
		text.add(PanelUi.sides(name, level));
		text.add(Box.createVerticalStrut(1));
		text.add(PanelUi.sides(style, rating));
		text.add(Box.createVerticalStrut(3));
		text.add(grows);
		for (Component child : text.getComponents())
			((JComponent) child).setAlignmentX(Component.LEFT_ALIGNMENT);

		add(text, BorderLayout.CENTER);
		portraits.request(view.name(), view.npcId(), image -> showPortrait(portrait, image));
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
				if (SwingUtilities.isLeftMouseButton(e))
					confirm();
			}
		});
	}

	private static void showPortrait(JLabel target, BufferedImage image)
	{
		target.setIcon(new ImageIcon(image));
	}

	/** Asks once, with No as the default, and takes the creature only on Yes. */
	private void confirm()
	{
		Object[] buttons = {"Yes, choose " + speciesName, "No"};
		int answer = JOptionPane.showOptionDialog(this, "Choose " + speciesName + " as your first creature?",
			"Your first creature", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, buttons,
			buttons[1]);
		if (answer == 0)
			actions.pickStarter(index);
	}
}
