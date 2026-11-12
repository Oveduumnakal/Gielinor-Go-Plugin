/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.materialtabs.MaterialTab;
import net.runelite.client.ui.components.materialtabs.MaterialTabGroup;

/**
 * The Gielinor Go side panel: a short header with the trainer's progress and the jar to throw, the battle box while
 * a battle waits for a choice, and four tabs for the team, the storage, the Creature Log and battles against friends
 * and City Champions. It only draws {@link PanelState} and {@link FriendsState} snapshots and reports clicks to
 * {@link PanelActions}.
 */
final class GoPanel extends PluginPanel
{
	private static final int TRAINER_BAR_HEIGHT = 4;

	/** One line of the jar drop-down: a jar type and how many the player owns. */
	private static final class JarChoice
	{
		private final JarType type;
		private final int count;

		private JarChoice(JarType type, int count)
		{
			this.type = type;
			this.count = count;
		}

		@Override
		public boolean equals(Object other)
		{
			return other instanceof JarChoice && ((JarChoice) other).type == type && ((JarChoice) other).count == count;
		}

		@Override
		public int hashCode()
		{
			return type.hashCode() * 31 + count;
		}

		@Override
		public String toString()
		{
			return type.label() + " jar  x" + count;
		}
	}

	/** Draws a jar drop-down line in the panel's small font, dimmed when the player has none of that jar. */
	private static final class JarRenderer extends DefaultListCellRenderer
	{
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
			boolean focused)
		{
			super.getListCellRendererComponent(list, value, index, selected, focused);
			setFont(FontManager.getRunescapeSmallFont());
			setBorder(new EmptyBorder(3, 6, 3, 6));
			if (value instanceof JarChoice)
			{
				JarChoice choice = (JarChoice) value;
				setForeground(choice.count > 0 ? Color.WHITE : ColorScheme.MEDIUM_GRAY_COLOR);
				setToolTipText(jarTooltip(choice.type));
				setIcon(JarIcons.swing(choice.type));
				setIconTextGap(6);
			}

			return this;
		}
	}

	private final PanelActions actions;
	private final JLabel trainerLabel;
	private final BarComponent trainerBar;
	private final JLabel winsLabel;
	private final JLabel stepsLabel;
	private final JComboBox<JarChoice> jarBox = new JComboBox<>();
	private final TeamTab teamTab;
	private final StorageTab storageTab;
	private final LogTab logTab;
	private final ChampionsTab championsTab;
	private final BattleBox battleBox;
	private List<JarChoice> shownJars = new ArrayList<>();
	private boolean fillingJars;
	private PanelState latest;

	GoPanel(PanelActions actions, PortraitSource portraits)
	{
		super(false);
		this.actions = actions;
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		trainerLabel = PanelUi.label("", FontManager.getRunescapeSmallFont(), Color.WHITE);
		trainerBar = new BarComponent(ColorScheme.BRAND_ORANGE, TRAINER_BAR_HEIGHT);
		winsLabel = PanelUi.caption("");
		stepsLabel = PanelUi.caption("");
		teamTab = new TeamTab(actions, portraits);
		storageTab = new StorageTab(actions);
		logTab = new LogTab();
		championsTab = new ChampionsTab(actions, this::openTrade);
		battleBox = new BattleBox(actions::battleChoice);

		JPanel top = new JPanel(new BorderLayout());
		top.setBackground(ColorScheme.DARK_GRAY_COLOR);
		top.add(buildHeader(), BorderLayout.NORTH);
		top.add(battleBox, BorderLayout.CENTER);
		add(top, BorderLayout.NORTH);
		add(buildTabs(), BorderLayout.CENTER);
	}

	/**
	 * Redraws the panel from a fresh snapshot. Only the parts that changed are touched.
	 *
	 * @param state the new snapshot
	 */
	void update(PanelState state)
	{
		latest = state;
		trainerLabel.setText((state.elite().titled() ? "Grand Champion Lv " : "Trainer Lv ") + state.trainerLevel());
		trainerLabel.setForeground(state.elite().titled() ? PanelUi.GOLD : Color.WHITE);
		trainerBar.set(state.trainerProgress(), ColorScheme.BRAND_ORANGE);
		trainerBar.setToolTipText(Math.round(state.trainerProgress() * 100) + "% to the next trainer level");
		winsLabel.setText(state.battlesWon() + (state.battlesWon() == 1 ? " battle won" : " battles won"));
		stepsLabel.setText("Next jar " + state.stepProgress() + " / " + GoGame.TILES_PER_JAR);
		stepsLabel.setToolTipText("Walk " + (GoGame.TILES_PER_JAR - state.stepProgress())
			+ " more tiles to find a Bronze jar");
		showJars(state);
		battleBox.show(state.battle());
		teamTab.update(state.team(), state.eggs(), state.tasks());
		storageTab.update(state.storage());
		logTab.update(state);
		championsTab.update(state.champions());
		championsTab.updateElite(state.elite());
	}

	/**
	 * Redraws the friends on the Battle tab, unless nothing about the party changed.
	 *
	 * @param state the party and the friends in it
	 */
	void updateFriends(FriendsState state)
	{
		championsTab.updateFriends(state);
	}

	/**
	 * Opens the window that asks what to offer a friend.
	 *
	 * @param friend the friend whose Trade button was clicked
	 */
	private void openTrade(FriendView friend)
	{
		if (latest != null)
			TradeDialog.ask(this, latest, friend, actions);
	}

	private JPanel buildHeader()
	{
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);
		header.setBorder(new EmptyBorder(8, 8, 6, 8));

		JLabel title = PanelUi.label("Gielinor Go", FontManager.getRunescapeBoldFont(), ColorScheme.BRAND_ORANGE);
		header.add(PanelUi.sides(title, trainerLabel));
		header.add(Box.createVerticalStrut(4));
		trainerBar.setTrack(ColorScheme.DARKER_GRAY_COLOR);
		trainerBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, TRAINER_BAR_HEIGHT));
		header.add(trainerBar);
		header.add(Box.createVerticalStrut(4));
		header.add(PanelUi.sides(winsLabel, stepsLabel));
		header.add(Box.createVerticalStrut(8));

		jarBox.setRenderer(new JarRenderer());
		jarBox.setFocusable(false);
		jarBox.setMaximumRowCount(JarType.values().length);
		jarBox.addActionListener(e -> jarChosen());
		JPanel jarRow = new JPanel(new BorderLayout(8, 0));
		jarRow.setOpaque(false);
		jarRow.add(PanelUi.caption("Throw"), BorderLayout.WEST);
		jarRow.add(jarBox, BorderLayout.CENTER);
		header.add(jarRow);
		for (Component child : header.getComponents())
			((JComponent) child).setAlignmentX(Component.LEFT_ALIGNMENT);

		return header;
	}

	/**
	 * Fills the jar drop-down with the tier jars and every specialty jar the player owns. It is left alone while
	 * it is open, so the list never jumps under the player's cursor.
	 *
	 * @param state the new snapshot
	 */
	private void showJars(PanelState state)
	{
		List<JarChoice> choices = new ArrayList<>();
		JarChoice selected = null;
		for (JarType type : JarType.values())
		{
			Integer owned = state.jars().get(type);
			int count = owned == null ? 0 : owned;
			if (type.specialty() && count == 0 && type != state.selectedJar())
				continue;

			JarChoice choice = new JarChoice(type, count);
			choices.add(choice);
			if (type == state.selectedJar())
				selected = choice;
		}

		if (jarBox.isPopupVisible() || (choices.equals(shownJars) && selected != null
			&& selected.equals(jarBox.getSelectedItem())))
			return;

		shownJars = choices;
		fillingJars = true;
		jarBox.setModel(new DefaultComboBoxModel<>(choices.toArray(new JarChoice[0])));
		jarBox.setSelectedItem(selected);
		jarBox.setToolTipText(jarTooltip(state.selectedJar()));
		fillingJars = false;
	}

	private void jarChosen()
	{
		Object chosen = jarBox.getSelectedItem();
		if (!fillingJars && chosen instanceof JarChoice)
			actions.selectJar(((JarChoice) chosen).type);
	}

	private static String jarTooltip(JarType type)
	{
		if (!type.specialty())
		{
			return "<html>x" + String.format("%.1f", type.bonus()) + " catch chance<br>"
				+ "Drops from battles at trainer level " + type.trainerLevel() + "</html>";
		}

		List<String> skills = new ArrayList<>();
		for (String skill : type.skills())
			skills.add(skill.substring(0, 1) + skill.substring(1).toLowerCase(Locale.ROOT));

		return "<html>x" + String.format("%.2f", type.traitBonus()) + " catch chance on " + type.trait().label()
			+ "<br>x" + String.format("%.1f", type.bonus()) + " on anything else"
			+ "<br>Found while training " + String.join(", ", skills) + "</html>";
	}

	private JPanel buildTabs()
	{
		JPanel display = new JPanel(new BorderLayout());
		display.setBackground(ColorScheme.DARK_GRAY_COLOR);
		MaterialTabGroup group = new MaterialTabGroup(display);
		MaterialTab team = new MaterialTab("Team", group, teamTab);
		MaterialTab storage = new MaterialTab("Storage", group, storageTab);
		MaterialTab log = new MaterialTab("Log", group, logTab);
		MaterialTab battle = new MaterialTab("Battle", group, championsTab);
		group.setBorder(new EmptyBorder(2, 0, 4, 0));
		for (MaterialTab tab : new MaterialTab[] {team, storage, log, battle})
		{
			tab.setFont(FontManager.getRunescapeSmallFont());
			group.addTab(tab);
		}

		group.select(team);

		JPanel center = new JPanel(new BorderLayout());
		center.setBackground(ColorScheme.DARK_GRAY_COLOR);
		center.add(group, BorderLayout.NORTH);
		center.add(display, BorderLayout.CENTER);
		return center;
	}
}
