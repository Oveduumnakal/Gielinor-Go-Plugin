/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Component;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * The small window that asks what to offer a friend: one of the player's creatures or eggs, for one of the
 * friend's or for nothing. Jars are never listed, because jars cannot be traded.
 */
final class TradeDialog
{
	private static final String GIFT = "Nothing (a gift)";

	/** One thing the player can give, as the drop-down shows it and as the plugin finds it. */
	private static final class Choice
	{
		private final TradePick pick;
		private final String label;

		private Choice(TradePick pick, String label)
		{
			this.pick = pick;
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	private TradeDialog()
	{
	}

	/**
	 * Asks the player what to offer, and sends the offer when they confirm.
	 *
	 * @param parent the component the window is centred on
	 * @param state what the player owns right now
	 * @param friend the friend to trade with
	 * @param actions receives the offer
	 */
	static void ask(Component parent, PanelState state, FriendView friend, PanelActions actions)
	{
		List<Choice> mine = choices(state);
		if (mine.isEmpty())
		{
			JOptionPane.showMessageDialog(parent, "You have no creatures or eggs to trade.", "Trade",
				JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		JComboBox<Choice> give = new JComboBox<>(mine.toArray(new Choice[0]));
		JComboBox<String> want = new JComboBox<>();
		want.addItem(GIFT);
		for (String theirs : friend.tradeable())
			want.addItem(theirs);

		JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
		form.add(new JLabel("Give " + friend.name() + ":"));
		form.add(give);
		form.add(new JLabel("For their:"));
		form.add(want);
		form.add(new JLabel("They have to accept before anything changes hands."));
		int answer = JOptionPane.showConfirmDialog(parent, form, "Trade with " + friend.name(),
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		Choice chosen = (Choice) give.getSelectedItem();
		if (answer != JOptionPane.OK_OPTION || chosen == null)
			return;

		int wantIndex = want.getSelectedIndex() - 1;
		String wantLabel = wantIndex < 0 ? null : friend.tradeable().get(wantIndex);
		actions.offerTrade(friend.memberId(), chosen.pick, wantIndex, wantLabel);
	}

	/**
	 * Lists everything the player can give: the team, then storage, then eggs.
	 *
	 * @param state what the player owns right now
	 * @return one choice per creature and egg
	 */
	private static List<Choice> choices(PanelState state)
	{
		List<Choice> choices = new ArrayList<>();
		for (int i = 0; i < state.team().size(); i++)
		{
			CreatureView view = state.team().get(i);
			choices.add(new Choice(TradePick.of(Holding.TEAM, i, view), creatureLabel(view) + "  (team)"));
		}

		for (int i = 0; i < state.storage().size(); i++)
		{
			CreatureView view = state.storage().get(i);
			choices.add(new Choice(TradePick.of(Holding.STORAGE, i, view), creatureLabel(view) + "  (storage)"));
		}

		for (int i = 0; i < state.eggs().size(); i++)
		{
			EggView egg = state.eggs().get(i);
			String label = egg.label() + "  (" + egg.tilesLeft() + " tiles left)";
			choices.add(new Choice(TradePick.of(i, egg), label));
		}

		return choices;
	}

	private static String creatureLabel(CreatureView view)
	{
		String species = view.nickname() == null ? "" : " [" + view.name() + "]";
		String rating = view.rating() == StatRolls.Rating.AVERAGE ? "" : " " + view.rating().label();
		return PanelUi.displayName(view) + species + " (Lv " + view.level() + ")" + rating;
	}
}
