/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.LongSupplier;

/**
 * The painted trade offer, in steps that need no new widget: a short dialog explains, a menu picks what to give, a
 * menu picks what to ask for, and a last dialog repeats the whole offer before it is sent. It mirrors the old Swing
 * {@code TradeDialog}: jars are never listed because jars cannot be traded, the choices are the team, then storage,
 * then eggs, the friend's side is their team's creatures then their eggs (or nothing, a gift), and the friend has to
 * accept before anything changes hands. A long list (storage can hold many creatures) is shown a page at a time. The
 * words are built here apart from any painting. Holds no RuneLite types.
 */
final class BattleTrade
{
	/** How many choices a menu shows before it offers the next page. */
	static final int PAGE = 10;

	/** The words for asking nothing in return. */
	static final String GIFT = "Nothing (a gift)";

	/** The title of the dialog that says there is nothing to give. */
	static final String TITLE = "Trade";

	/** What the dialog says when the player owns no creature or egg. */
	static final String NOTHING_TO_GIVE = "You have no creatures or eggs to trade.";

	/** The button that closes a dialog. */
	static final String CLOSE = "Close";

	/** The button that goes on to pick what to give. */
	static final String PICK = "Pick what to give";

	/** The button that sends the offer. */
	static final String OFFER = "Offer";

	/** The button that cancels. */
	static final String CANCEL = "Cancel";

	/** One thing the player can give: the pick the plugin finds it by, and the words for it. */
	static final class Choice
	{
		private final TradePick pick;
		private final String label;

		private Choice(TradePick pick, String label)
		{
			this.pick = pick;
			this.label = label;
		}

		/** @return what the plugin matches again before it sends */
		TradePick pick()
		{
			return pick;
		}

		/** @return the words for it */
		String label()
		{
			return label;
		}
	}

	private final WindowShell shell;
	private final PanelActions actions;
	private final LongSupplier clock;
	private final PanelState state;
	private final FriendView friend;
	private final int x;
	private final int y;
	private final List<Choice> mine;
	private Choice given;
	private int wantIndex = -1;

	/**
	 * Starts a trade offer.
	 *
	 * @param shell opens the menus and dialogs
	 * @param actions receives the offer
	 * @param clock the time in milliseconds
	 * @param state what the player owns right now
	 * @param friend the friend to trade with
	 * @param x where the menus open, in window pixels
	 * @param y where the menus open, in window pixels
	 */
	BattleTrade(WindowShell shell, PanelActions actions, LongSupplier clock, PanelState state, FriendView friend,
		int x, int y)
	{
		this.shell = shell;
		this.actions = actions;
		this.clock = clock;
		this.state = state;
		this.friend = friend;
		this.x = x;
		this.y = y;
		this.mine = choices(state);
	}

	/**
	 * Lists everything the player can give: the team, then storage, then eggs.
	 *
	 * @param state what the player owns right now
	 * @return one choice per creature and egg
	 */
	static List<Choice> choices(PanelState state)
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
			choices.add(new Choice(TradePick.of(i, egg), egg.label() + "  (" + egg.tilesLeft() + " tiles left)"));
		}

		return choices;
	}

	/**
	 * @param view a creature
	 * @return its name, its species when it has a nickname, its level and a rating when it is not average
	 */
	static String creatureLabel(CreatureView view)
	{
		String shown = view.nickname() == null ? view.name() : view.nickname();
		String name = view.gilded() ? "Gilded " + shown : shown;
		String species = view.nickname() == null ? "" : " [" + view.name() + "]";
		String rating = view.rating() == StatRolls.Rating.AVERAGE ? "" : " " + view.rating().label();
		return name + species + " (Lv " + view.level() + ")" + rating;
	}

	/**
	 * Cuts a long list into menu pages.
	 *
	 * @param labels every label
	 * @param page the page to show, from 0
	 * @param pick called with the place in {@code labels} of the label chosen
	 * @param turn called with the page to show instead, from the Back and More items
	 * @return the items of that page, with Back first when there is a page before and More last when there is one after
	 */
	static List<ContextMenu.Item> paged(List<String> labels, int page, IntConsumer pick, IntConsumer turn)
	{
		List<ContextMenu.Item> items = new ArrayList<>();
		int from = page * PAGE;
		int to = Math.min(labels.size(), from + PAGE);
		if (page > 0)
			items.add(ContextMenu.Item.of("Back to " + (from - PAGE + 1) + " - " + from, () -> turn.accept(page - 1)));

		for (int i = from; i < to; i++)
		{
			int place = i;
			items.add(ContextMenu.Item.of(labels.get(i), () -> pick.accept(place)));
		}

		if (to < labels.size())
			items.add(ContextMenu.Item.of("More (" + (labels.size() - to) + " left)...", () -> turn.accept(page + 1)));

		return items;
	}

	/** Opens the first step, or says there is nothing to trade. */
	void start()
	{
		if (mine.isEmpty())
		{
			shell.openModal(new Modal(TITLE, NOTHING_TO_GIVE, Collections.singletonList(CLOSE), 0, choice ->
			{
			}, () ->
			{
			}), null, clock.getAsLong());
			return;
		}

		String message = "Offer " + friend.name() + " a creature or egg of yours, for one of theirs or for nothing."
			+ "\nThey have to accept before anything changes hands.";
		shell.openModal(new Modal("Trade with " + friend.name(), message, Arrays.asList(CANCEL, PICK), 1, choice ->
		{
			if (choice == 1)
				pickGive(0);
		}, () ->
		{
		}), null, clock.getAsLong());
	}

	private void pickGive(int page)
	{
		List<String> labels = new ArrayList<>();
		for (Choice choice : mine)
			labels.add(choice.label());

		shell.openMenu(paged(labels, page, this::gave, this::pickGive), x, y, true, clock.getAsLong());
	}

	private void gave(int place)
	{
		given = mine.get(place);
		pickWant(0);
	}

	/** @return the words for what to ask: a gift first, then the friend's creatures and eggs */
	List<String> wants()
	{
		List<String> wants = new ArrayList<>();
		wants.add(GIFT);
		wants.addAll(friend.tradeable());
		return wants;
	}

	private void pickWant(int page)
	{
		shell.openMenu(paged(wants(), page, this::wanted, this::pickWant), x, y, true, clock.getAsLong());
	}

	private void wanted(int place)
	{
		wantIndex = place - 1;
		confirm();
	}

	/**
	 * @param give the words for what is given
	 * @param want the words for what is asked, or {@link #GIFT}
	 * @param name the friend's name
	 * @return the text of the last dialog
	 */
	static String summary(String give, String want, String name)
	{
		return "Give " + name + ": " + give + "\nFor their: " + want + "\nThey have to accept before anything changes "
			+ "hands.";
	}

	private void confirm()
	{
		String want = wantIndex < 0 ? GIFT : friend.tradeable().get(wantIndex);
		shell.openModal(new Modal("Trade with " + friend.name(), summary(given.label(), want, friend.name()),
			Arrays.asList(CANCEL, OFFER), 1, choice ->
			{
				if (choice == 1)
					send();
			}, () ->
			{
			}), null, clock.getAsLong());
	}

	private void send()
	{
		String wantLabel = wantIndex < 0 ? null : friend.tradeable().get(wantIndex);
		actions.offerTrade(friend.memberId(), given.pick(), wantIndex, wantLabel);
	}

	/** @return what the player owns, as it was when the offer started */
	PanelState state()
	{
		return state;
	}
}
