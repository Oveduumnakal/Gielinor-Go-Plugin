/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The battle box at the top of the side panel, shown only while a battle the player steers waits for a choice: both
 * creatures with their health, the time left, and the buttons for Attack, Switch, Catch, Item and Run. Attack opens the
 * creature's moves when it knows more than one. It is built once and updated in place, and it reports each choice to a
 * consumer.
 */
final class BattleBox extends JPanel
{
	private static final int BAR_HEIGHT = 5;
	private static final int TIME_HEIGHT = 3;
	private static final Color MINE = new Color(0x5FBF60);
	private static final Color FOE = new Color(0xD9534F);
	private static final Color TIME = new Color(0xFFD23F);

	private final Consumer<BattleChoice> choose;
	private final JLabel title = PanelUi.label("", FontManager.getRunescapeBoldFont(), ColorScheme.BRAND_ORANGE);
	private final JLabel mineName = PanelUi.label("", FontManager.getRunescapeSmallFont(), Color.WHITE);
	private final JLabel foeName = PanelUi.label("", FontManager.getRunescapeSmallFont(), Color.WHITE);
	private final BarComponent mineBar = new BarComponent(MINE, BAR_HEIGHT);
	private final BarComponent foeBar = new BarComponent(FOE, BAR_HEIGHT);
	private final BarComponent timeBar = new BarComponent(TIME, TIME_HEIGHT);
	private final JButton attack;
	private final JButton swap;
	private final JButton jar;
	private final JButton run;
	private final JButton item;
	private List<BattleView.ItemOption> items = Collections.emptyList();
	private List<BattleView.Option> options = Collections.emptyList();
	private List<Move> moves = Collections.emptyList();

	/**
	 * Builds the box, hidden until a battle waits.
	 *
	 * @param choose called with each choice the player makes
	 */
	BattleBox(Consumer<BattleChoice> choose)
	{
		this.choose = choose;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(0, 8, 6, 8),
			BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE),
				new EmptyBorder(6, 6, 6, 6))));
		attack = PanelUi.button("Attack", e -> openMoveMenu());
		swap = PanelUi.button("Switch", e -> openSwitchMenu());
		jar = PanelUi.button("Catch", e -> choose.accept(BattleChoice.catching(null)));
		item = PanelUi.button("Item", e -> openItemMenu());
		run = PanelUi.button("Run", e -> choose.accept(BattleChoice.run()));
		mineBar.setTrack(ColorScheme.DARK_GRAY_COLOR);
		foeBar.setTrack(ColorScheme.DARK_GRAY_COLOR);
		timeBar.setTrack(ColorScheme.DARK_GRAY_COLOR);
		add(left(title));
		add(Box.createVerticalStrut(4));
		add(left(mineName));
		add(left(mineBar));
		add(Box.createVerticalStrut(4));
		add(left(foeName));
		add(left(foeBar));
		add(Box.createVerticalStrut(4));
		add(left(timeBar));
		add(Box.createVerticalStrut(6));
		add(left(buttons()));
		setVisible(false);
	}

	private JPanel buttons()
	{
		JPanel grid = new JPanel(new GridLayout(0, 2, 4, 4));
		grid.setOpaque(false);
		grid.add(attack);
		grid.add(swap);
		grid.add(jar);
		grid.add(item);
		grid.add(run);
		JPanel wrap = new JPanel(new BorderLayout());
		wrap.setOpaque(false);
		wrap.add(grid, BorderLayout.CENTER);
		return wrap;
	}

	/**
	 * Shows the battle that waits, or hides the box.
	 *
	 * @param view the battle, or null when none waits
	 */
	void show(BattleView view)
	{
		setVisible(view != null);
		if (view == null)
			return;

		options = view.options();
		moves = view.moves();
		items = view.items();
		title.setText(titleOf(view));
		mineName.setText(view.mineName() + " Lv " + view.mineLevel() + BattleView.tagText(view.mineStatus()));
		foeName.setText((view.friendName() == null ? "" : view.friendName() + "'s ") + view.foeName() + " Lv "
			+ view.foeLevel() + BattleView.tagText(view.foeStatus()));
		mineBar.set(view.mineHp(), MINE);
		foeBar.set(view.foeHp(), FOE);
		mineBar.setToolTipText(Math.round(view.mineHp() * 100) + "% health");
		foeBar.setToolTipText(Math.round(view.foeHp() * 100) + "% health");
		timeBar.set(view.timeLeft(), TIME);
		boolean waiting = view.waitingForFriend();
		attack.setEnabled(!view.forcedSwitch() && !waiting);
		attack.setText(view.canChooseMove() ? "Attack..." : "Attack");
		attack.setToolTipText(view.canChooseMove() ? "Choose one of your creature's moves" : null);
		run.setEnabled(!view.forcedSwitch() || view.friendName() != null);
		run.setToolTipText(view.friendName() == null ? null : "Leave the battle. Nothing is won or lost.");
		swap.setEnabled(view.canSwitch() && !waiting);
		swap.setToolTipText(view.canSwitch() ? "Choose a creature to step in. It costs your turn."
			: "No other creature can fight");
		item.setEnabled(view.canUseItem() && !waiting);
		item.setToolTipText(view.canUseItem() ? "Use a healing item. It costs your turn."
			: "No item would help right now");
		jar.setEnabled(view.canCatch());
		jar.setText(view.jarLabel() == null ? "Catch" : "Catch x" + view.jarCount());
		jar.setToolTipText(catchTip(view));
	}

	private static String titleOf(BattleView view)
	{
		if (view.waitingForFriend())
			return "WAITING FOR " + view.friendName().toUpperCase() + "...";

		if (view.forcedSwitch())
			return "CHOOSE YOUR NEXT CREATURE";

		return "YOUR TURN · " + view.secondsLeft() + "s";
	}

	private static String catchTip(BattleView view)
	{
		if (view.jarLabel() == null)
			return "No jars left";

		return "<html>Throw a " + view.jarLabel() + " jar. The more health the creature has lost,<br>the better "
			+ "the chance. It costs your turn.</html>";
	}

	/**
	 * Attacks at once when the creature knows one move, and otherwise opens the list of its moves under the Attack
	 * button.
	 */
	private void openMoveMenu()
	{
		if (moves.size() <= 1)
		{
			choose.accept(BattleChoice.attack());
			return;
		}

		JPopupMenu menu = moveMenu();
		if (attack.isShowing())
			menu.show(attack, 0, attack.getHeight());
	}

	/**
	 * Builds the list of the creature's moves.
	 *
	 * @return a menu with one item per move, each attacking with that move
	 */
	JPopupMenu moveMenu()
	{
		JPopupMenu menu = new JPopupMenu();
		for (int i = 0; i < moves.size(); i++)
		{
			Move move = moves.get(i);
			String text = move.plain() ? "Attack" : move.name() + " \u00b7 " + move.describe();
			int place = i;
			menu.add(PanelUi.menuItem(text, () -> choose.accept(BattleChoice.attack(place))));
		}

		return menu;
	}

	/** Opens the list of items that would do something, under the Item button. */
	private void openItemMenu()
	{
		JPopupMenu menu = itemMenu();
		if (menu.getComponentCount() > 0 && item.isShowing())
			menu.show(item, 0, item.getHeight());
	}

	/**
	 * Builds the list of items that would do something now. An item that helps one creature is used on it at once;
	 * one that could help several opens a list of them.
	 *
	 * @return a menu with one entry per item, each choosing that item
	 */
	JPopupMenu itemMenu()
	{
		JPopupMenu menu = new JPopupMenu();
		for (BattleView.ItemOption option : items)
		{
			ItemType type = option.type();
			String text = type.label() + "  x" + option.count();
			if (option.targets().size() == 1)
			{
				BattleView.Option only = option.targets().get(0);
				int place = only.teamIndex();
				JMenuItem entry = PanelUi.menuItem(text, () -> choose.accept(BattleChoice.item(type, place)));
				entry.setIcon(ItemIcons.swing(type));
				menu.add(entry);
				continue;
			}

			JMenu sub = new JMenu(text);
			sub.setIcon(ItemIcons.swing(type));
			for (BattleView.Option target : option.targets())
			{
				String name = target.name() + " Lv " + target.level() + " \u00b7 "
					+ Math.round(target.hpFraction() * 100) + "%";
				int place = target.teamIndex();
				sub.add(PanelUi.menuItem(name, () -> choose.accept(BattleChoice.item(type, place))));
			}

			menu.add(sub);
		}

		return menu;
	}

	/** Opens the list of creatures that could step in, under the Switch button. */
	private void openSwitchMenu()
	{
		JPopupMenu menu = switchMenu();
		if (menu.getComponentCount() > 0 && swap.isShowing())
			menu.show(swap, 0, swap.getHeight());
	}

	/**
	 * Builds the list of creatures that could step in.
	 *
	 * @return a menu with one item per creature, each choosing that switch
	 */
	JPopupMenu switchMenu()
	{
		JPopupMenu menu = new JPopupMenu();
		for (BattleView.Option option : options)
		{
			String text = option.name() + " Lv " + option.level() + " · " + Math.round(option.hpFraction() * 100)
				+ "%";
			int place = option.teamIndex();
			menu.add(PanelUi.menuItem(text, () -> choose.accept(BattleChoice.switchTo(place))));
		}

		return menu;
	}

	/** @return the Attack button */
	JButton attackButton()
	{
		return attack;
	}

	/** @return the Switch button */
	JButton switchButton()
	{
		return swap;
	}

	/** @return the Item button */
	JButton itemButton()
	{
		return item;
	}

	/** @return the Catch button */
	JButton catchButton()
	{
		return jar;
	}

	/** @return the Run button */
	JButton runButton()
	{
		return run;
	}

	private static <T extends JComponent> T left(T component)
	{
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		return component;
	}
}
