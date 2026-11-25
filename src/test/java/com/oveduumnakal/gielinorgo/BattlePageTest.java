/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The Battle page: champions, the Elite challenge, the Rival, friends and the trade offer, on the painted window. */
public class BattlePageTest
{
	private static final long ANN = 11L;

	private static ChampionView champion(String id, String city, boolean beaten, boolean here)
	{
		return new ChampionView(id, city, "Goblin 4, Cow 5, Giant rat 7", "3 Iron jars", beaten, here);
	}

	private static String label(BattleBlock card, int item, int button)
	{
		BattleBlock.Item row = card.items().get(item);
		List<BattleBlock.Btn> buttons = row.buttons();
		return buttons.get(button).label();
	}

	private static String line(BattleBlock card, int item)
	{
		BattleBlock.Item row = card.items().get(item);
		return row.text();
	}

	private static String labelOf(List<BattleTrade.Choice> choices, int place)
	{
		return choices.get(place).label();
	}

	private static List<ChampionView> champions(int count, int here)
	{
		List<ChampionView> list = new ArrayList<>();
		for (int i = 0; i < count; i++)
			list.add(champion("c" + i, "City " + i, i == 1, i == here));

		return list;
	}

	private static EliteView elite(EliteView.Mode mode, boolean titled)
	{
		return new EliteView(mode, mode == EliteView.Mode.LOCKED ? 5 : 8, mode == EliteView.Mode.RUNNING ? 3 : 0, 5,
			"Elite of Might", "Sarachnis 81", "2 Dragon jars", titled);
	}

	private static PanelState state(List<CreatureView> team, List<ChampionView> champions, EliteView elite,
		RivalView rival)
	{
		return new PanelState(7, 0.2, 4, new EnumMap<>(JarType.class), JarType.BRONZE, 0, team,
			Collections.emptyList(), Collections.emptyList(), 0, 0, Collections.emptyList(), champions,
			TodayView.NONE, TaskView.NONE, new EnumMap<>(ItemType.class)).withGoals(MilestoneView.NONE, elite)
			.withRival(rival);
	}

	private static FriendView friend(long id, String name, boolean waiting, boolean offered, String offer)
	{
		return new FriendView(id, name, 12, "Cow 5, Imp 9", true, waiting, Arrays.asList("Cow (Lv 5)", "Imp (Lv 9)"))
			.withTrade(offer, offer.isEmpty() ? 0 : 77, offered);
	}

	private static PartyRig page(PanelState state, FriendsState friends, int width, int height)
	{
		PartyRig rig = new PartyRig();
		rig.width = width;
		rig.height = height;
		rig.shell.publish(WindowSnapshot.of(rig.now, state, true).withFriends(friends));
		rig.open();
		rig.shell.showTab(WindowTab.BATTLE, rig.now);
		rig.paint();
		return rig;
	}

	private static PartyRig simple(List<ChampionView> champions, EliteView elite)
	{
		return page(state(PartyRig.team(2), champions, elite, RivalView.NONE), FriendsState.none(), 765, 497);
	}

	private static List<String> words(PartyRig rig)
	{
		rig.drawn.clear();
		rig.paint();
		List<String> words = new ArrayList<>();
		for (PartyRig.Drawn d : rig.drawn)
			words.add(d.text);

		return words;
	}

	@Test
	public void theChampionsHeadingCountsSigilsAndGold()
	{
		List<ChampionView> list = champions(8, 0);
		assertEquals("CITY CHAMPIONS  1 / 8 sigils", BattleModel.championsHeading(list));
		list.set(1, list.get(1).withRematch("Goblin 40", true, "Reward ready"));
		assertEquals("CITY CHAMPIONS  1 / 8 sigils, 1 gold", BattleModel.championsHeading(list));
	}

	@Test
	public void aChampionCardSaysWhatTheOldCardSaid()
	{
		BattleBlock here = BattleModel.champion(champion("c0", "Lumbridge", false, true));
		assertEquals("Lumbridge", here.text());
		assertEquals("Here", here.status());
		assertEquals("", here.tip());
		assertEquals("Challenge", label(here, 2, 0));
		assertEquals("First win: 3 Iron jars", line(here, 1));

		BattleBlock away = BattleModel.champion(champion("c1", "Falador", false, false));
		assertEquals("", away.status());
		assertEquals("Go to Falador to challenge its champion", away.tip());
		assertEquals(2, away.items().size());

		BattleBlock beaten = BattleModel.champion(champion("c2", "Varrock", true, true));
		assertEquals("Sigil", beaten.status());
		assertEquals("Rematch", label(beaten, 2, 0));

		ChampionView gold = champion("c3", "Draynor", true, false).withRematch("Goblin 40, Cow 41", true,
			"Reward ready");
		BattleBlock rematch = BattleModel.champion(gold);
		assertEquals("Gold sigil", rematch.status());
		assertEquals("Rematch: Goblin 40, Cow 41", line(rematch, 2));
		assertEquals("Reward ready", line(rematch, 3));
	}

	@Test
	public void onlyTheChampionInTheCityHasAButtonAndTheOthersHaveATooltip()
	{
		PartyRig rig = simple(champions(3, 2), EliteView.NONE);

		assertTrue(rig.ids().contains(BattleModel.CHAMPION_PREFIX + "c2"));
		assertFalse(rig.ids().contains(BattleModel.CHAMPION_PREFIX + "c0"));
		assertEquals("Go to City 0 to challenge its champion", rig.hit(BattleModel.CARD_PREFIX + "champion:c0")
			.tip());
		assertTrue(words(rig).contains("CITY CHAMPIONS  1 / 3 sigils"));
	}

	@Test
	public void challengingAChampionSendsItsId()
	{
		PartyRig rig = simple(champions(3, 2), EliteView.NONE);

		rig.click(BattleModel.CHAMPION_PREFIX + "c2");

		assertEquals(Collections.singletonList("challenge c2"), rig.calls);
	}

	@Test
	public void noChampionsShowsTheSameEmptyText()
	{
		PartyRig rig = simple(Collections.emptyList(), EliteView.NONE);

		assertTrue(words(rig).contains(BattleModel.EMPTY_TEXT));
		assertTrue(words(rig).contains("CITY CHAMPIONS  0 / 0 sigils"));
	}

	@Test
	public void theEliteCardNamesWhatToDoInEachMode()
	{
		assertEquals("Earn all 8 sigils · 5 / 8", BattleModel.describeElite(new EliteView(EliteView.Mode.LOCKED, 5, 0,
			5, null, null, "x", false)));
		assertEquals("Go to the Champions' Guild", BattleModel.describeElite(elite(EliteView.Mode.AWAY, false)));
		assertEquals("Elite of Might: Sarachnis 81", BattleModel.describeElite(elite(EliteView.Mode.RUNNING, false)));
		assertEquals("Elite 3 of 5", BattleModel.eliteCard(elite(EliteView.Mode.RUNNING, false)).status());
		assertEquals("Grand Champion", BattleModel.eliteCard(elite(EliteView.Mode.AWAY, true)).status());
		assertEquals("Challenge again", label(BattleModel.eliteCard(elite(EliteView.Mode.HERE, true)), 2, 0));
	}

	@Test
	public void theEliteCardShowsOnlyWhenTheChallengeExists()
	{
		PartyRig none = simple(champions(1, 0), EliteView.NONE);
		assertFalse(words(none).contains("ELITE CHALLENGE"));

		PartyRig away = simple(champions(1, 0), elite(EliteView.Mode.AWAY, false));
		assertTrue(words(away).contains("ELITE CHALLENGE"));
		assertFalse(away.ids().contains(BattleModel.ID_ELITE_CHALLENGE));
	}

	@Test
	public void theEliteButtonsFollowTheMode()
	{
		PartyRig here = simple(Collections.emptyList(), elite(EliteView.Mode.HERE, false));
		here.click(BattleModel.ID_ELITE_CHALLENGE);
		assertEquals(Collections.singletonList("eliteChallenge"), here.calls);

		PartyRig run = simple(Collections.emptyList(), elite(EliteView.Mode.RUNNING, false));
		run.click(BattleModel.ID_ELITE_CONTINUE);
		assertEquals(Collections.singletonList("eliteContinue"), run.calls);
		assertFalse(run.ids().contains(BattleModel.ID_ELITE_CHALLENGE));
	}

	@Test
	public void forfeitAsksFirstAndKeepsGoingByDefault()
	{
		PartyRig rig = simple(Collections.emptyList(), elite(EliteView.Mode.RUNNING, false));

		rig.click(BattleModel.ID_ELITE_FORFEIT);
		assertTrue(rig.calls.isEmpty());
		rig.key(KeyEvent.VK_ENTER);
		assertTrue("the default is to keep going", rig.calls.isEmpty());

		rig.click(BattleModel.ID_ELITE_FORFEIT);
		rig.key(KeyEvent.VK_RIGHT);
		rig.key(KeyEvent.VK_ENTER);
		assertEquals(Collections.singletonList("eliteForfeit"), rig.calls);
	}

	@Test
	public void theRivalIsHiddenLockedReadyOrBeaten()
	{
		PartyRig hidden = simple(champions(1, 0), EliteView.NONE);
		assertFalse(words(hidden).contains("RIVAL"));

		RivalView locked = new RivalView(RivalView.Mode.LOCKED, "Bryn", 2, null, null, "2 Iron jars",
			"Earn 4 sigils to meet Bryn again", null);
		PartyRig lockedRig = page(state(PartyRig.team(1), champions(1, 0), EliteView.NONE, locked),
			FriendsState.none(), 765, 497);
		assertTrue(words(lockedRig).contains("RIVAL"));
		assertTrue(words(lockedRig).contains("Battle 2 / 5"));
		assertTrue(words(lockedRig).contains("Earn 4 sigils to meet Bryn again"));
		assertFalse(lockedRig.ids().contains(BattleModel.ID_RIVAL));

		RivalView ready = new RivalView(RivalView.Mode.READY, "Bryn", 2, "Round two", "Goblin 9", "2 Iron jars",
			"Ready when you are", null);
		PartyRig readyRig = page(state(PartyRig.team(1), champions(1, 0), EliteView.NONE, ready),
			FriendsState.none(), 765, 497);
		assertTrue(words(readyRig).contains("\"Round two\""));
		assertTrue(words(readyRig).contains("Win: 2 Iron jars"));
		readyRig.click(BattleModel.ID_RIVAL);
		assertEquals(Collections.singletonList("challenge rival"), readyRig.calls);
	}

	@Test
	public void theFriendsSectionSaysWhatToDoWithNoParty()
	{
		PartyRig rig = simple(champions(1, 0), EliteView.NONE);
		assertTrue(words(rig).contains("FRIENDS"));
		assertTrue(words(rig).contains(BattleModel.NO_PARTY_TEXT));

		PartyRig party = page(state(PartyRig.team(1), champions(1, 0), EliteView.NONE, RivalView.NONE),
			new FriendsState(true, Collections.emptyList()), 765, 497);
		assertTrue(words(party).contains(BattleModel.NOBODY_TEXT));
	}

	@Test
	public void aFriendCardChallengesAndOffersATrade()
	{
		FriendsState friends = new FriendsState(true, Arrays.asList(friend(ANN, "Ann", false, false, ""),
			new FriendView(22L, "Bob", 3, "", false, false, Collections.emptyList())));
		PartyRig rig = page(state(PartyRig.team(1), champions(1, 0), EliteView.NONE, RivalView.NONE), friends, 765,
			497);

		assertTrue(words(rig).contains("FRIENDS  2"));
		assertTrue(words(rig).contains(BattleModel.HINT_TEXT));
		assertTrue(words(rig).contains("Trainer Lv 12"));
		assertTrue(words(rig).contains("No creatures yet"));
		rig.click(BattleModel.FRIEND_CHALLENGE_PREFIX + ANN);
		assertEquals(Collections.singletonList("challengeFriend " + ANN), rig.calls);
		assertFalse("a friend with no team cannot be challenged",
			rig.hit(BattleModel.FRIEND_CHALLENGE_PREFIX + 22).enabled());
	}

	@Test
	public void waitingAndOfferedDisableTheirButtonsAndSayWhy()
	{
		FriendsState friends = new FriendsState(true, Collections.singletonList(friend(ANN, "Ann", true, true, "")));
		PartyRig rig = page(state(PartyRig.team(1), champions(0, 0), EliteView.NONE, RivalView.NONE), friends, 765,
			497);

		Hit challenge = rig.hit(BattleModel.FRIEND_CHALLENGE_PREFIX + ANN);
		Hit trade = rig.hit(BattleModel.FRIEND_TRADE_PREFIX + ANN);
		assertFalse(challenge.enabled());
		assertFalse(trade.enabled());
		BattleBlock card = BattleModel.friendCard(friends.friends().get(0));
		assertEquals("Waiting...", label(card, 1, 0));
		assertEquals("Offered...", label(card, 1, 1));
	}

	@Test
	public void anIncomingOfferCanBeAcceptedOrDeclinedByItsId()
	{
		FriendsState friends = new FriendsState(true, Collections.singletonList(friend(ANN, "Ann", false, false,
			"gilded Imp (Lv 20) for your Cow (Lv 5)")));
		PartyRig rig = page(state(PartyRig.team(1), champions(0, 0), EliteView.NONE, RivalView.NONE), friends, 765,
			497);

		assertTrue(words(rig).contains("Offers gilded Imp (Lv 20) for your Cow (Lv 5)"));
		rig.click(BattleModel.ACCEPT_PREFIX + ANN + ":77");
		rig.click(BattleModel.DECLINE_PREFIX + ANN + ":77");

		assertEquals(Arrays.asList("answerTrade " + ANN + " 77 true", "answerTrade " + ANN + " 77 false"),
			rig.calls);
	}

	@Test
	public void aSwappedOfferIsAnsweredWithTheIdThatWasShownSoTheGameCanRefuseIt()
	{
		FriendsState first = new FriendsState(true, Collections.singletonList(friend(ANN, "Ann", false, false,
			"gilded Imp (Lv 20) for your Cow (Lv 5)")));
		PartyRig rig = page(state(PartyRig.team(1), champions(0, 0), EliteView.NONE, RivalView.NONE), first, 765, 497);
		Hit accept = rig.hit(BattleModel.ACCEPT_PREFIX + ANN + ":77");
		FriendView swapped = new FriendView(ANN, "Ann", 12, "Cow 5, Imp 9", true, false, Collections.emptyList())
			.withTrade("Imp (Lv 1) for nothing", 78, false);
		rig.shell.publish(WindowSnapshot.of(rig.now + 1, state(PartyRig.team(1), champions(0, 0), EliteView.NONE,
			RivalView.NONE), true).withFriends(new FriendsState(true, Collections.singletonList(swapped))));
		rig.paint();

		rig.pages.press(accept);

		assertEquals("the old id goes to the game, which says the offer changed and does nothing",
			Collections.singletonList("answerTrade " + ANN + " 77 true"), rig.calls);
		assertEquals(78, BattleModel.offerOf(rig.hit(BattleModel.ACCEPT_PREFIX + ANN + ":78").id()));
	}

	@Test
	public void theTradeOfferGoesThroughFourStepsAndIsMatchedByTheGame()
	{
		FriendsState friends = new FriendsState(true, Collections.singletonList(friend(ANN, "Ann", false, false, "")));
		PartyRig rig = page(state(PartyRig.team(2), champions(0, 0), EliteView.NONE, RivalView.NONE), friends, 765,
			497);

		rig.click(BattleModel.FRIEND_TRADE_PREFIX + ANN);
		rig.key(KeyEvent.VK_ENTER);
		rig.key(KeyEvent.VK_DOWN);
		rig.key(KeyEvent.VK_ENTER);
		assertTrue("nothing is sent while the offer is being built", rig.calls.isEmpty());
		rig.key(KeyEvent.VK_DOWN);
		rig.key(KeyEvent.VK_DOWN);
		rig.key(KeyEvent.VK_ENTER);
		assertTrue(rig.calls.isEmpty());
		rig.key(KeyEvent.VK_ENTER);

		assertEquals(Collections.singletonList("offerTrade " + ANN + " TEAM#1 1 Imp (Lv 9)"), rig.calls);
	}

	@Test
	public void aGiftAsksForNothingAndCancelSendsNothing()
	{
		FriendsState friends = new FriendsState(true, Collections.singletonList(friend(ANN, "Ann", false, false, "")));
		PartyRig rig = page(state(PartyRig.team(2), champions(0, 0), EliteView.NONE, RivalView.NONE), friends, 765,
			497);

		rig.click(BattleModel.FRIEND_TRADE_PREFIX + ANN);
		rig.key(KeyEvent.VK_ENTER);
		rig.key(KeyEvent.VK_ENTER);
		rig.key(KeyEvent.VK_ENTER);
		rig.key(KeyEvent.VK_ENTER);
		assertEquals(Collections.singletonList("offerTrade " + ANN + " TEAM#0 -1 null"), rig.calls);

		rig.calls.clear();
		rig.click(BattleModel.FRIEND_TRADE_PREFIX + ANN);
		rig.key(KeyEvent.VK_ESCAPE);
		rig.key(KeyEvent.VK_ENTER);
		assertTrue(rig.calls.isEmpty());
	}

	@Test
	public void tradingWithNothingToGiveSaysSo()
	{
		FriendsState friends = new FriendsState(true, Collections.singletonList(friend(ANN, "Ann", false, false, "")));
		PartyRig rig = page(state(Collections.emptyList(), champions(0, 0), EliteView.NONE, RivalView.NONE), friends,
			765, 497);

		rig.click(BattleModel.FRIEND_TRADE_PREFIX + ANN);
		rig.key(KeyEvent.VK_ENTER);

		assertTrue(rig.calls.isEmpty());
		assertTrue(BattleTrade.choices(state(Collections.emptyList(), champions(0, 0), EliteView.NONE,
			RivalView.NONE)).isEmpty());
	}

	@Test
	public void theGiveListIsTeamThenStorageThenEggsAndNeverJars()
	{
		PanelState base = state(PartyRig.team(2), champions(0, 0), EliteView.NONE, RivalView.NONE);
		PanelState owned = new PanelState(7, 0.2, 4, new EnumMap<>(JarType.class), JarType.BRONZE, 0, base.team(),
			Collections.singletonList(PartyRig.creature("Imp", 9, 1.0, null)), Collections.emptyList(), 0, 0,
			Collections.singletonList(new EggView(Rarity.RARE, 120, 500)), Collections.emptyList(), TodayView.NONE,
			TaskView.NONE);

		List<BattleTrade.Choice> choices = BattleTrade.choices(owned);

		assertEquals(4, choices.size());
		assertTrue(labelOf(choices, 0).endsWith("(team)"));
		assertEquals("Imp (Lv 9)  (storage)", labelOf(choices, 2));
		assertTrue(labelOf(choices, 3).endsWith("(120 tiles left)"));
	}

	@Test
	public void longMenusComePagedWithMoreAndBack()
	{
		List<String> labels = new ArrayList<>();
		for (int i = 0; i < 25; i++)
			labels.add("c" + i);

		List<Integer> picked = new ArrayList<>();
		List<Integer> turned = new ArrayList<>();
		List<ContextMenu.Item> first = BattleTrade.paged(labels, 0, picked::add, turned::add);
		assertEquals(BattleTrade.PAGE + 1, first.size());
		assertEquals("More (15 left)...", first.get(first.size() - 1).label());
		List<ContextMenu.Item> last = BattleTrade.paged(labels, 2, picked::add, turned::add);
		assertEquals(6, last.size());
		assertEquals("Back to 11 - 20", last.get(0).label());
	}

	@Test
	public void theColumnScrollsWithTheWheelTheBarAndPageKeys()
	{
		PartyRig rig = page(state(PartyRig.team(1), champions(30, 0), elite(EliteView.Mode.HERE, false),
			RivalView.NONE), FriendsState.none(), 535, 348);
		ScrollList list = rig.pages.battlePage().list();
		assertTrue(list.scrollable());
		assertEquals(0, list.offset());

		Rectangle view = list.viewport();
		rig.shell.onWheel(view.x + 5, view.y + 5, 2);
		assertTrue("the wheel scrolls", list.offset() > 0);

		rig.key(KeyEvent.VK_END);
		assertEquals(list.maxOffset(), list.offset());
		assertTrue("the Elite button is reachable at the end", rig.ids().contains(BattleModel.ID_ELITE_CHALLENGE));
		rig.key(KeyEvent.VK_HOME);
		assertEquals(0, list.offset());
		rig.key(KeyEvent.VK_PAGE_DOWN);
		assertTrue(list.offset() > 0);

		Rectangle thumb = list.thumbBounds();
		int before = list.offset();
		rig.shell.onPressed(thumb.x + 1, thumb.y + 1, MouseEvent.BUTTON1, 0, rig.now);
		rig.shell.onDragged(thumb.x + 1, thumb.y + 21);
		rig.shell.onReleased(thumb.x + 1, thumb.y + 21, MouseEvent.BUTTON1, rig.now);
		assertTrue("dragging the handle scrolls", list.offset() > before);
		assertTrue(rig.calls.isEmpty());
	}

	@Test
	public void everyHitStaysInsideThePageWhereverTheColumnIsScrolled()
	{
		StringBuilder problems = new StringBuilder();
		for (int[] canvas : new int[][] {{535, 348}, {765, 497}, {1344, 874}})
		{
			PartyRig rig = page(everything(), friendsWithOffer(), canvas[0], canvas[1]);
			Rectangle body = WindowLayout.of(rig.width, rig.height).body;
			ScrollList list = rig.pages.battlePage().list();
			for (int step = 0; step <= 4; step++)
			{
				list.setOffset(list.maxOffset() * step / 4.0);
				rig.paint();
				for (Hit hit : rig.shell.hits())
				{
					boolean page = hit.id().startsWith("battle:");
					if (page && !body.contains(hit.bounds()))
						problems.append(canvas[0] + " " + hit.id() + " " + hit.bounds() + "\n");
				}
			}
		}

		assertEquals(problems.toString(), "", problems.toString());
	}

	@Test
	public void everyStringStaysInsideItsBoxAtEverySize()
	{
		StringBuilder problems = new StringBuilder();
		for (int[] canvas : new int[][] {{535, 348}, {765, 497}, {1344, 874}})
		{
			PartyRig rig = page(everything(), friendsWithOffer(), canvas[0], canvas[1]);
			Rectangle body = WindowLayout.of(rig.width, rig.height).body;
			ScrollList list = rig.pages.battlePage().list();
			for (int step = 0; step <= 4; step++)
			{
				list.setOffset(list.maxOffset() * step / 4.0);
				rig.drawn.clear();
				rig.paint();
				for (PartyRig.Drawn d : rig.drawn)
				{
					boolean sideways = d.box.x < body.x || d.box.x + d.box.width > body.x + body.width;
					if (d.width > d.box.width || d.height > d.box.height + 1 || sideways
						|| !d.box.intersects(body))
						problems.append(canvas[0] + " " + d + "\n");
				}
			}
		}

		assertEquals(problems.toString(), "", problems.toString());
	}

	@Test
	public void aTallWindowShowsTheWholeColumnInsideThePage()
	{
		PartyRig rig = page(state(PartyRig.team(1), champions(1, 0), EliteView.NONE,
			RivalView.NONE), FriendsState.none(), 1344, 874);
		Rectangle body = WindowLayout.of(rig.width, rig.height).body;

		ScrollList list = rig.pages.battlePage().list();
		assertFalse(list.scrollable());
		rig.drawn.clear();
		rig.paint();
		for (PartyRig.Drawn d : rig.drawn)
			assertTrue(d.toString(), body.contains(d.box));
	}

	private static PanelState everything()
	{
		RivalView rival = new RivalView(RivalView.Mode.READY, "Bryn", 3,
			"You again? I have been training with a very long list of creatures since we last met", "Goblin 9, Cow 11, "
			+ "Giant rat 12, Imp 14, Bruise Blamish Snail 20, Man 3", "2 Iron jars", "Ready when you are", null);
		return state(PartyRig.team(3), champions(8, 3), elite(EliteView.Mode.RUNNING, true), rival);
	}

	private static FriendsState friendsWithOffer()
	{
		return new FriendsState(true, Arrays.asList(
			friend(ANN, "Ann With A Really Long Display Name", false, false,
				"gilded Imp (Lv 20) for your Bruise Blamish Snail (Lv 20) and a long tail of words"),
			friend(33L, "Cat", true, true, "")));
	}
}
