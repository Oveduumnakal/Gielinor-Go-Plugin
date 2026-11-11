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
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

/**
 * The Gielinor Go side panel: a fixed header with trainer, jar and walking progress, and three tabs for the team,
 * the storage and the Creature Log. It only draws {@link PanelState} snapshots and reports clicks to
 * {@link PanelActions}.
 */
final class GoPanel extends PluginPanel
{
	private static final int TRAINER_BAR_HEIGHT = 6;

	private final Map<JarType, JLabel> jarLabels = new EnumMap<>(JarType.class);
	private final JPanel jarGrid = new JPanel(new GridLayout(0, 2, 4, 4));
	private List<JarType> shownJars = new ArrayList<>();
	private final JLabel trainerLabel;
	private final BarComponent trainerBar;
	private final JLabel battlesLabel;
	private final JLabel stepsLabel;
	private final TeamTab teamTab;
	private final StorageTab storageTab;
	private final LogTab logTab;

	GoPanel(PanelActions actions)
	{
		super(false);
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		trainerLabel = PanelUi.label("", FontManager.getRunescapeSmallFont(), Color.WHITE);
		trainerBar = new BarComponent(ColorScheme.BRAND_ORANGE, TRAINER_BAR_HEIGHT);
		battlesLabel = PanelUi.label("", FontManager.getRunescapeSmallFont(), ColorScheme.LIGHT_GRAY_COLOR);
		stepsLabel = PanelUi.label("", FontManager.getRunescapeSmallFont(), ColorScheme.LIGHT_GRAY_COLOR);
		teamTab = new TeamTab(actions);
		storageTab = new StorageTab(actions);
		logTab = new LogTab();

		add(buildHeader(actions), BorderLayout.NORTH);
		add(buildTabs(), BorderLayout.CENTER);
	}

	/**
	 * Redraws the panel from a fresh snapshot. Only the parts that changed are rebuilt.
	 *
	 * @param state the new snapshot
	 */
	void update(PanelState state)
	{
		trainerLabel.setText("Trainer Lv " + state.trainerLevel());
		trainerBar.set(state.trainerProgress(), ColorScheme.BRAND_ORANGE);
		battlesLabel.setText("Battles won: " + state.battlesWon());
		stepsLabel.setText(state.stepProgress() + " / " + GoGame.TILES_PER_JAR + " tiles to next jar");
		List<JarType> visible = new ArrayList<>();
		for (JarType type : JarType.values())
		{
			JLabel label = jarLabels.get(type);
			Integer owned = state.jars().get(type);
			int count = owned == null ? 0 : owned;
			label.setText(type.label() + " " + count);
			styleJar(label, type == state.selectedJar());
			if (!type.specialty() || count > 0 || type == state.selectedJar())
				visible.add(type);
		}

		showJars(visible);
		teamTab.update(state.team());
		storageTab.update(state.storage());
		logTab.update(state);
	}

	private JPanel buildHeader(PanelActions actions)
	{
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);
		header.setBorder(new EmptyBorder(10, 10, 6, 10));

		JLabel title = PanelUi.label("Gielinor Go", FontManager.getRunescapeBoldFont(), ColorScheme.BRAND_ORANGE);
		header.add(title);
		header.add(Box.createVerticalStrut(6));
		header.add(trainerLabel);
		header.add(Box.createVerticalStrut(2));
		header.add(trainerBar);
		header.add(Box.createVerticalStrut(4));
		header.add(battlesLabel);
		header.add(Box.createVerticalStrut(6));

		jarGrid.setOpaque(false);
		List<JarType> visible = new ArrayList<>();
		for (JarType type : JarType.values())
		{
			jarLabels.put(type, jarLabel(type, actions));
			if (!type.specialty())
				visible.add(type);
		}

		showJars(visible);
		header.add(jarGrid);
		header.add(Box.createVerticalStrut(6));
		header.add(stepsLabel);
		trainerBar.setTrack(ColorScheme.DARKER_GRAY_COLOR);
		trainerBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, TRAINER_BAR_HEIGHT));
		for (Component child : header.getComponents())
			((JComponent) child).setAlignmentX(Component.LEFT_ALIGNMENT);

		return header;
	}

	private JLabel jarLabel(JarType type, PanelActions actions)
	{
		JLabel label = PanelUi.label(type.label(), FontManager.getRunescapeSmallFont(), Color.WHITE);
		label.setOpaque(true);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		label.setToolTipText(jarTooltip(type));
		styleJar(label, false);
		label.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				actions.selectJar(type);
			}
		});
		return label;
	}

	/**
	 * Puts the given jars in the grid, when they are not the ones already shown. Tier jars are always listed;
	 * a specialty jar only takes up room while the player owns one.
	 *
	 * @param visible the jars to show, in display order
	 */
	private void showJars(List<JarType> visible)
	{
		if (visible.equals(shownJars))
			return;

		shownJars = visible;
		jarGrid.removeAll();
		for (JarType type : visible)
			jarGrid.add(jarLabels.get(type));

		jarGrid.revalidate();
		jarGrid.repaint();
	}

	private static String jarTooltip(JarType type)
	{
		if (!type.specialty())
		{
			return "<html>×" + String.format("%.1f", type.bonus()) + " catch chance<br>"
				+ "Drops from battles at trainer level " + type.trainerLevel() + "</html>";
		}

		List<String> skills = new ArrayList<>();
		for (String skill : type.skills())
			skills.add(skill.substring(0, 1) + skill.substring(1).toLowerCase(Locale.ROOT));

		return "<html>×" + String.format("%.2f", type.traitBonus()) + " catch chance on " + type.trait().label()
			+ "<br>×" + String.format("%.1f", type.bonus()) + " on anything else"
			+ "<br>Found while training " + String.join(", ", skills) + "</html>";
	}

	private static void styleJar(JLabel label, boolean selected)
	{
		if (selected)
		{
			label.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
			label.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE, 2),
				BorderFactory.createEmptyBorder(3, 2, 3, 2)));
			return;
		}

		label.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		label.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
			BorderFactory.createEmptyBorder(4, 3, 4, 3)));
	}

	private JPanel buildTabs()
	{
		JPanel display = new JPanel(new BorderLayout());
		display.setBackground(ColorScheme.DARK_GRAY_COLOR);
		MaterialTabGroup group = new MaterialTabGroup(display);
		MaterialTab team = new MaterialTab("Team", group, teamTab);
		MaterialTab storage = new MaterialTab("Storage", group, storageTab);
		MaterialTab log = new MaterialTab("Log", group, logTab);
		group.setBorder(new EmptyBorder(5, 0, 5, 0));
		group.addTab(team);
		group.addTab(storage);
		group.addTab(log);
		group.select(team);

		JPanel center = new JPanel(new BorderLayout());
		center.setBackground(ColorScheme.DARK_GRAY_COLOR);
		center.add(group, BorderLayout.NORTH);
		center.add(display, BorderLayout.CENTER);
		return center;
	}
}
