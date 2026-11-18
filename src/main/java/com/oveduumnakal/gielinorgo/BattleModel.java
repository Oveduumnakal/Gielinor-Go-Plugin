/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns what the old Battle tab showed (the City Champions, the Elite challenge, the Rival and the friends in the
 * party) into the {@link BattleBlock}s of the painted Battle page, with the same words, the same gating and the same
 * order as the old cards. Pure: no painting, no game. Holds no RuneLite types.
 */
final class BattleModel
{
	/** The start of the id of a champion's Challenge or Rematch button; the champion's id follows. */
	static final String CHAMPION_PREFIX = "battle:champion:";

	/** The id of the Elite Challenge button. */
	static final String ID_ELITE_CHALLENGE = "battle:elite:challenge";

	/** The id of the Elite Continue button. */
	static final String ID_ELITE_CONTINUE = "battle:elite:continue";

	/** The id of the Elite Forfeit button. */
	static final String ID_ELITE_FORFEIT = "battle:elite:forfeit";

	/** The id of the Rival's Challenge button. */
	static final String ID_RIVAL = "battle:rival:challenge";

	/** The start of the id of a friend's Challenge button; the member id follows. */
	static final String FRIEND_CHALLENGE_PREFIX = "battle:friend:challenge:";

	/** The start of the id of a friend's Trade button; the member id follows. */
	static final String FRIEND_TRADE_PREFIX = "battle:friend:trade:";

	/** The start of the id of an Accept button; the member id, a colon and the offer id follow. */
	static final String ACCEPT_PREFIX = "battle:friend:accept:";

	/** The start of the id of a Decline button; the member id, a colon and the offer id follow. */
	static final String DECLINE_PREFIX = "battle:friend:decline:";

	/** The start of the id of a card's own hit, which carries its tooltip. */
	static final String CARD_PREFIX = "battle:card:";

	/** What shows when there are no champions. */
	static final String EMPTY_TEXT = "No City Champions are available.";

	/** What shows in the friends section outside a party. */
	static final String NO_PARTY_TEXT = "Join a RuneLite party with a friend to battle their team.";

	/** What shows in a party with nobody else playing. */
	static final String NOBODY_TEXT = "Nobody else in your party is playing Gielinor Go yet.";

	/** What shows in a party with friends. */
	static final String HINT_TEXT = "Battles are for fun: nothing is won or lost. Trades are for keeps.";

	private BattleModel()
	{
	}

	/**
	 * Builds the whole column: friends, the rival, the champions, then the Elite challenge.
	 *
	 * @param panel what the game shows
	 * @param friends the friends in the party
	 * @return the blocks, top to bottom
	 */
	static List<BattleBlock> build(PanelState panel, FriendsState friends)
	{
		List<BattleBlock> blocks = new ArrayList<>();
		friends(blocks, friends);
		rival(blocks, panel.rival());
		champions(blocks, panel.champions());
		elite(blocks, panel.elite());
		return blocks;
	}

	/**
	 * @param champions every champion
	 * @return the heading of the champions' section, such as {@code CITY CHAMPIONS  2 / 8 sigils, 1 gold}
	 */
	static String championsHeading(List<ChampionView> champions)
	{
		int beaten = 0;
		int gold = 0;
		for (ChampionView champion : champions)
		{
			beaten += champion.beaten() ? 1 : 0;
			gold += champion.gold() ? 1 : 0;
		}

		return "CITY CHAMPIONS  " + beaten + " / " + champions.size() + " sigils" + (gold > 0 ? ", " + gold + " gold"
			: "");
	}

	private static void champions(List<BattleBlock> blocks, List<ChampionView> champions)
	{
		blocks.add(BattleBlock.heading(championsHeading(champions)));
		if (champions.isEmpty())
			blocks.add(BattleBlock.note(EMPTY_TEXT));

		for (ChampionView champion : champions)
			blocks.add(champion(champion));
	}

	/**
	 * @param view one champion
	 * @return the champion's card, worded as the old card was
	 */
	static BattleBlock champion(ChampionView view)
	{
		ThemeColor stripe = view.here() && !view.beaten() ? ThemeColor.HP_FILL
			: view.beaten() ? ThemeColor.ACCENT : ThemeColor.ROW_BORDER;
		String status = view.gold() ? "Gold sigil" : view.beaten() ? "Sigil" : view.here() ? "Here" : "";
		ThemeColor statusColor = view.here() && !view.beaten() ? ThemeColor.HP_FILL : ThemeColor.ACCENT;
		List<BattleBlock.Item> items = new ArrayList<>();
		items.add(BattleBlock.Item.text(view.team(), ThemeColor.TEXT, false));
		items.add(BattleBlock.Item.text("First win: " + view.reward(), ThemeColor.SUBTEXT, true));
		if (view.rematchOpen())
		{
			items.add(BattleBlock.Item.text("Rematch: " + view.rematchTeam(), ThemeColor.TEXT, false));
			items.add(BattleBlock.Item.text(view.rematchNote(), ThemeColor.SUBTEXT, true));
		}

		String tip = "";
		if (view.here())
		{
			items.add(BattleBlock.Item.buttons(new BattleBlock.Btn(CHAMPION_PREFIX + view.id(),
				view.beaten() ? "Rematch" : "Challenge", true, "Fight " + view.city() + "'s champion")));
		}
		else
		{
			tip = "Go to " + view.city() + " to challenge its champion";
		}

		return BattleBlock.card(CARD_PREFIX + "champion:" + view.id(), view.city(), status, statusColor, stripe, tip,
			items);
	}

	private static void elite(List<BattleBlock> blocks, EliteView view)
	{
		if (view.mode() == EliteView.Mode.HIDDEN)
			return;

		blocks.add(BattleBlock.heading("ELITE CHALLENGE"));
		blocks.add(eliteCard(view));
	}

	/**
	 * @param view the Elite challenge
	 * @return its card, worded as the old card was
	 */
	static BattleBlock eliteCard(EliteView view)
	{
		boolean live = view.mode() == EliteView.Mode.HERE || view.mode() == EliteView.Mode.RUNNING;
		ThemeColor stripe = live ? ThemeColor.HP_FILL : view.titled() ? ThemeColor.ACCENT : ThemeColor.ROW_BORDER;
		String status = "";
		ThemeColor statusColor = ThemeColor.ACCENT;
		if (view.mode() == EliteView.Mode.RUNNING)
		{
			status = "Elite " + view.stage() + " of " + view.stages();
			statusColor = ThemeColor.HP_FILL;
		}
		else if (view.titled())
		{
			status = "Grand Champion";
		}

		List<BattleBlock.Item> items = new ArrayList<>();
		items.add(BattleBlock.Item.text(describeElite(view), ThemeColor.TEXT, false));
		items.add(BattleBlock.Item.text("Reward: " + view.reward(), ThemeColor.SUBTEXT, false));
		if (view.mode() == EliteView.Mode.HERE)
		{
			items.add(BattleBlock.Item.buttons(new BattleBlock.Btn(ID_ELITE_CHALLENGE,
				view.titled() ? "Challenge again" : "Challenge", true, "Start the Elite run")));
		}

		if (view.mode() == EliteView.Mode.RUNNING)
		{
			items.add(BattleBlock.Item.buttons(
				new BattleBlock.Btn(ID_ELITE_CONTINUE, "Continue", true, "Fight the next Elite battle"),
				new BattleBlock.Btn(ID_ELITE_FORFEIT, "Forfeit", true, "Give up the run. You are asked first.")));
		}

		return BattleBlock.card(CARD_PREFIX + "elite", "Elite challenge", status, statusColor, stripe, "", items);
	}

	/**
	 * Words what the player can do about the Elite challenge right now.
	 *
	 * @param view the challenge
	 * @return a short line, or two for a run that is open
	 */
	static String describeElite(EliteView view)
	{
		switch (view.mode())
		{
			case LOCKED:
				return "Earn all " + EliteRun.SIGILS_NEEDED + " sigils · " + view.sigils() + " / "
					+ EliteRun.SIGILS_NEEDED;
			case AWAY:
				return "Go to the Champions' Guild";
			case HERE:
				return "Four Elite trainers and the Grand Champion, back to back. Health carries over and nothing "
					+ "rests until the run ends.";
			default:
				return view.opponent() + ": " + view.team();
		}
	}

	private static void rival(List<BattleBlock> blocks, RivalView view)
	{
		if (view.mode() == RivalView.Mode.HIDDEN)
			return;

		blocks.add(BattleBlock.heading("RIVAL"));
		blocks.add(rivalCard(view));
	}

	/**
	 * @param view the rival
	 * @return the rival's card, worded as the old card was
	 */
	static BattleBlock rivalCard(RivalView view)
	{
		boolean ready = view.mode() == RivalView.Mode.READY;
		ThemeColor stripe = ready ? ThemeColor.HP_FILL
			: view.mode() == RivalView.Mode.BEATEN ? ThemeColor.ACCENT : ThemeColor.ROW_BORDER;
		List<BattleBlock.Item> items = new ArrayList<>();
		if (view.line() != null)
			items.add(BattleBlock.Item.text("\"" + view.line() + "\"", ThemeColor.TEXT, false));

		if (view.team() != null)
			items.add(BattleBlock.Item.text(view.team(), ThemeColor.TEXT, false));

		items.add(BattleBlock.Item.text(view.status(), ready ? ThemeColor.HP_FILL : ThemeColor.ACCENT, false));
		if (view.reward() != null)
			items.add(BattleBlock.Item.text("Win: " + view.reward(), ThemeColor.SUBTEXT, true));

		if (ready)
		{
			items.add(BattleBlock.Item.buttons(new BattleBlock.Btn(ID_RIVAL, "Challenge", true,
				"Fight " + view.name() + " anywhere")));
		}

		return BattleBlock.card(CARD_PREFIX + "rival", view.name(), "Battle " + view.battle() + " / "
			+ RivalRules.BATTLES, ThemeColor.SUBTEXT, stripe, "", items);
	}

	private static void friends(List<BattleBlock> blocks, FriendsState state)
	{
		blocks.add(BattleBlock.heading(state.friends().isEmpty() ? "FRIENDS" : "FRIENDS  " + state.friends().size()));
		blocks.add(BattleBlock.note(!state.inParty() ? NO_PARTY_TEXT : state.friends().isEmpty() ? NOBODY_TEXT
			: HINT_TEXT));
		for (FriendView friend : state.friends())
			blocks.add(friendCard(friend));
	}

	/**
	 * @param view one friend
	 * @return the friend's card, worded as the old card was
	 */
	static BattleBlock friendCard(FriendView view)
	{
		List<BattleBlock.Item> items = new ArrayList<>();
		items.add(BattleBlock.Item.text(view.challengeable() ? view.team() : "No creatures yet", ThemeColor.TEXT,
			false));
		String who = String.valueOf(view.memberId());
		items.add(BattleBlock.Item.buttons(
			new BattleBlock.Btn(FRIEND_CHALLENGE_PREFIX + who, view.waiting() ? "Waiting..." : "Challenge",
				view.challengeable() && !view.waiting(), "Battle " + view.name() + "'s team for fun"),
			new BattleBlock.Btn(FRIEND_TRADE_PREFIX + who, view.offered() ? "Offered..." : "Trade", !view.offered(),
				"Offer " + view.name() + " a trade")));
		if (!view.offer().isEmpty())
		{
			items.add(BattleBlock.Item.text("Offers " + view.offer(), ThemeColor.ACCENT, false));
			items.add(BattleBlock.Item.buttons(
				new BattleBlock.Btn(ACCEPT_PREFIX + who + ":" + view.offerId(), "Accept", true,
					"Take this trade. Nothing changes hands until " + view.name() + " confirms."),
				new BattleBlock.Btn(DECLINE_PREFIX + who + ":" + view.offerId(), "Decline", true,
					"Turn this trade down")));
		}

		return BattleBlock.card(CARD_PREFIX + "friend:" + who, view.name(), "Trainer Lv " + view.trainerLevel(),
			ThemeColor.SUBTEXT, ThemeColor.ROW_BORDER, "", items);
	}

	/**
	 * @param id a button id made with one of the friend prefixes
	 * @param prefix the prefix it starts with
	 * @return the member id after the prefix, or -1 when the id is not of that kind
	 */
	static long memberOf(String id, String prefix)
	{
		if (id == null || !id.startsWith(prefix))
			return -1;

		String rest = id.substring(prefix.length());
		int colon = rest.indexOf(':');
		try
		{
			return Long.parseLong(colon < 0 ? rest : rest.substring(0, colon));
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}

	/**
	 * @param id an Accept or Decline button id
	 * @return the offer id the button was drawn for, or 0 when the id has none
	 */
	static long offerOf(String id)
	{
		int colon = id == null ? -1 : id.lastIndexOf(':');
		if (colon < 0)
			return 0;

		try
		{
			return Long.parseLong(id.substring(colon + 1));
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}
}
