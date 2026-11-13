/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/** One carried egg: its rarity, how many tiles remain, and a progress bar. Updated in place as the player walks. */
final class EggRow extends JPanel
{
	private final Rarity rarity;
	private final String bossName;
	private final JLabel status;
	private final BarComponent bar;

	EggRow(EggView egg)
	{
		super(new BorderLayout(0, 3));
		rarity = egg.rarity();
		bossName = egg.bossName();
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(4, 8, 4, 8));

		JPanel top = new JPanel(new BorderLayout(4, 0));
		top.setOpaque(false);
		top.add(PanelUi.label(egg.label(), FontManager.getRunescapeSmallFont(), rarity.color()),
			BorderLayout.WEST);
		status = PanelUi.label("", FontManager.getRunescapeSmallFont(), ColorScheme.LIGHT_GRAY_COLOR);
		top.add(status, BorderLayout.EAST);
		add(top, BorderLayout.NORTH);

		bar = new BarComponent(ColorScheme.BRAND_ORANGE, 4);
		add(bar, BorderLayout.CENTER);
		update(egg);
	}

	/** @return the rarity this row was built for */
	Rarity rarity()
	{
		return rarity;
	}

	/** @return the boss species name this row was built for, or null for an ordinary egg */
	String bossName()
	{
		return bossName;
	}

	/**
	 * Refreshes the progress text and bar.
	 *
	 * @param egg the egg's current state
	 */
	void update(EggView egg)
	{
		bar.set(egg.progress(), ColorScheme.BRAND_ORANGE);
		if (egg.tilesLeft() == 0)
		{
			status.setText("Ready - storage is full");
			status.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
			return;
		}

		status.setText(egg.tilesLeft() + " tiles left");
		status.setForeground(Color.WHITE);
	}
}
