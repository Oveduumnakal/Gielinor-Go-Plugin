/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Runs steered battles through the director with a fake scene: prompts, timeouts, forfeits and hand-overs. */
public class BattleDirectorTest
{
	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.STAB);
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.STAB);

	/** A scene that records what it is told. */
	private static final class FakeScene implements BattleDirector.Scene
	{
		private boolean alive = true;
		private boolean ready;
		private final List<String> told = new ArrayList<>();

		@Override
		public boolean active()
		{
			return alive;
		}

		@Override
		public boolean waiting()
		{
			return ready;
		}

		@Override
		public void append(TurnResult turn)
		{
			ready = false;
			told.add("append " + turn.hits().size());
		}

		@Override
		public void finishWith(boolean won)
		{
			told.add("finish " + won);
		}

		@Override
		public void showLead(Combatant entered)
		{
			told.add("lead " + entered.name());
		}

		@Override
		public void runAway()
		{
			told.add("run");
		}

		@Override
		public void end()
		{
			told.add("end");
		}
	}

	private final List<String> shown = new ArrayList<>();
	private final List<String> thrown = new ArrayList<>();
	private final List<List<BattleLine>> closings = new ArrayList<>();
	private long now = 1_000_000L;
	private int seconds = 10;
	private GoGame game;
	private BattleDirector director;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(CHICKEN, GOBLIN, DRAGON));
		Champion lumbridge = new Champion("lumbridge", "Lumbridge", 3201, 3200, 3260, 3250, Arrays.asList(
			new Champion.Member("Chicken", 2)), JarType.IRON, 3);
		game = new GoGame(table, new EvolutionTable(Arrays.asList(), table), new ChampionTable(
			Arrays.asList(lumbridge), table), new Random(5), () -> now, notice ->
			{
			});
		director = new BattleDirector(game, () -> seconds, new BattleDirector.Listener()
		{
			@Override
			public void turn(BattleSession session, TurnResult turn, List<BattleLine> closing, boolean hasScene)
			{
				shown.add(turn.status() + (hasScene ? " scene" : " text"));
				closings.add(closing);
			}

			@Override
			public void jarThrown(BattleSession session, JarType jar, CatchOutcome outcome)
			{
				thrown.add(jar + " " + outcome);
			}
		});
	}

	private BattleSession start(Species foe, int level, Creature... team)
	{
		game.data().team.addAll(Arrays.asList(team));
		return game.startBattle(new WildEntry(1, foe, level, 1));
	}

	private Creature creature(String key, int level)
	{
		return new Creature(key, level, now);
	}

	@Test
	public void aBattleWithNoSceneWaitsForAChoiceAtOnceAndAgainAfterEachTurn()
	{
		BattleSession session = start(DRAGON, 60, creature("king black dragon", 60));
		director.begin(session, null);

		assertTrue(director.active());
		assertTrue(director.promptOpen());
		assertNotNull(director.view());

		assertTrue(director.choose(BattleChoice.attack()));

		assertEquals(1, shown.size());
		assertTrue(shown.get(0).endsWith("text"));
		if (!session.over())
			assertTrue(director.promptOpen());
	}

	@Test
	public void aSceneKeepsThePromptShutUntilItWaitsAndThenOpensIt()
	{
		FakeScene scene = new FakeScene();
		director.begin(start(DRAGON, 60, creature("king black dragon", 60)), scene);

		director.gameTick(false);

		assertFalse(director.promptOpen());
		assertNull(director.view());
		assertFalse("no choice before the prompt opens", director.choose(BattleChoice.attack()));

		scene.ready = true;
		director.gameTick(false);

		assertTrue(director.promptOpen());
		assertEquals(10, director.view().secondsLeft());
		assertEquals(1.0, director.view().timeLeft(), 0.0);
	}

	@Test
	public void aChoiceGoesToTheSceneAndTheBattleWaitsForItToFinishTheTurn()
	{
		FakeScene scene = new FakeScene();
		director.begin(start(GOBLIN, 30, creature("goblin", 30)), scene);
		scene.ready = true;
		director.gameTick(false);

		assertTrue(director.choose(BattleChoice.attack()));

		assertFalse(director.promptOpen());
		assertEquals(1, scene.told.size());
		assertTrue(scene.told.get(0).startsWith("append"));
		assertTrue(shown.get(0).endsWith("scene"));
	}

	@Test
	public void aWonBattleEndsTheSceneWithAWinAndTakesNoMoreChoices()
	{
		FakeScene scene = new FakeScene();
		director.begin(start(CHICKEN, 2, creature("king black dragon", 60)), scene);
		scene.ready = true;
		director.gameTick(false);

		director.choose(BattleChoice.attack());

		assertEquals(Arrays.asList("append 1", "finish true"), scene.told);
		assertFalse(director.active());
		assertEquals(1, closings.get(0).size());
		assertFalse(director.choose(BattleChoice.attack()));
	}

	@Test
	public void aLostBattleEndsTheSceneWithALoss()
	{
		FakeScene scene = new FakeScene();
		director.begin(start(DRAGON, 80, creature("chicken", 1)), scene);
		scene.ready = true;
		director.gameTick(false);

		director.choose(BattleChoice.attack());

		assertEquals("finish false", scene.told.get(scene.told.size() - 1));
		assertFalse(director.active());
	}

	@Test
	public void noChoiceInTimeMeansTheCreatureAttacks()
	{
		BattleSession session = start(DRAGON, 60, creature("king black dragon", 60));
		director.begin(session, null);
		int limit = TurnClock.ticksFor(10);

		for (int i = 1; i < limit; i++)
		{
			director.gameTick(false);
			assertEquals("tick " + i, 0, shown.size());
		}

		director.gameTick(false);

		assertEquals(1, shown.size());
		assertTrue(session.state().totalAttacks() > 0);
	}

	@Test
	public void theTimePerTurnComesFromTheSetting()
	{
		seconds = 5;
		director.begin(start(DRAGON, 60, creature("king black dragon", 60)), null);

		assertEquals(5, director.view().secondsLeft());

		for (int i = 0; i < TurnClock.ticksFor(5); i++)
			director.gameTick(false);

		assertEquals(1, shown.size());
	}

	@Test
	public void noChoiceInTimeAfterAFaintStepsInTheNextCreature()
	{
		BattleSession session = start(DRAGON, 80, creature("chicken", 1), creature("goblin", 30));
		director.begin(session, null);
		director.choose(BattleChoice.attack());
		assertTrue(session.state().needsSwitch());

		for (int i = 0; i < TurnClock.ticksFor(10); i++)
			director.gameTick(false);

		assertFalse(session.state().needsSwitch());
		assertEquals(1, session.state().myIndex());
	}

	@Test
	public void aSwitchShowsTheNewLeadOnTheScene()
	{
		FakeScene scene = new FakeScene();
		director.begin(start(GOBLIN, 30, creature("goblin", 30), creature("chicken", 3)), scene);
		scene.ready = true;
		director.gameTick(false);

		director.choose(BattleChoice.switchTo(1));

		assertEquals(Arrays.asList("lead Chicken", "append 1"), scene.told);
	}

	@Test
	public void runningEndsTheSceneWithNoRewards()
	{
		FakeScene scene = new FakeScene();
		WildEntry foe = new WildEntry(1, GOBLIN, 30, 1);
		game.data().team.add(creature("goblin", 30));
		director.begin(game.startBattle(foe), scene);
		scene.ready = true;
		director.gameTick(false);

		director.choose(BattleChoice.run());

		assertEquals(Arrays.asList("run"), scene.told);
		assertFalse(director.active());
		assertTrue(foe.battled());
		assertEquals(0, game.data().battlesWon);
	}

	@Test
	public void leavingTheBattleEndsItAsRunning()
	{
		FakeScene scene = new FakeScene();
		WildEntry foe = new WildEntry(1, GOBLIN, 30, 1);
		game.data().team.add(creature("goblin", 30));
		director.begin(game.startBattle(foe), scene);

		director.gameTick(true);

		assertFalse(director.active());
		assertEquals(Arrays.asList("end"), scene.told);
		assertTrue(foe.battled());
		assertFalse(foe.weakened());
	}

	@Test
	public void aSceneThatEndedByItselfEndsTheBattleToo()
	{
		FakeScene scene = new FakeScene();
		WildEntry foe = new WildEntry(1, GOBLIN, 30, 1);
		game.data().team.add(creature("goblin", 30));
		director.begin(game.startBattle(foe), scene);
		scene.alive = false;

		director.gameTick(false);

		assertFalse(director.active());
		assertTrue(foe.battled());
	}

	@Test
	public void startingAnotherBattleEndsTheFirstAsRunning()
	{
		game.data().team.add(creature("goblin", 30));
		WildEntry first = new WildEntry(1, GOBLIN, 30, 1);
		WildEntry second = new WildEntry(2, GOBLIN, 30, 1);
		director.begin(game.startBattle(first), null);

		director.begin(game.startBattle(second), null);

		assertTrue(first.battled());
		assertFalse(second.battled());
		assertTrue(director.active());
	}

	@Test
	public void aJarThatCatchesIsHandedOverAndTheSceneIsLeftToTheCatchEffects()
	{
		FakeScene scene = new FakeScene();
		game.data().team.add(creature("king black dragon", 80));
		game.data().addJars(JarType.BRONZE, 300);
		director.begin(game.startBattle(new WildEntry(1, CHICKEN, 3, 1)), scene);
		int guard = 0;

		while (director.active() && guard++ < 200)
		{
			scene.ready = true;
			director.gameTick(false);
			director.choose(BattleChoice.catching(null));
		}

		assertFalse(director.active());
		assertEquals("BRONZE CAUGHT", thrown.get(thrown.size() - 1));
		assertTrue(thrown.stream().allMatch(line -> line.startsWith("BRONZE ")));
		assertFalse(scene.told.stream().anyMatch(line -> line.startsWith("finish")));
		assertEquals(thrown.size() - 1, scene.told.size());
	}

	@Test
	public void aJarThatCannotBeThrownKeepsThePromptOpenAndTheClockGoing()
	{
		game.data().team.add(creature("goblin", 30));
		SaveData save = game.data();
		save.addJars(JarType.BRONZE, -save.jarCount(JarType.BRONZE));
		director.begin(game.startBattle(new WildEntry(1, GOBLIN, 30, 1)), null);
		director.gameTick(false);
		director.gameTick(false);
		int left = director.view().secondsLeft();

		assertTrue(director.choose(BattleChoice.catching(null)));

		assertTrue(director.promptOpen());
		assertEquals(left, director.view().secondsLeft());
		assertTrue(shown.get(0).startsWith("ONGOING"));
		assertTrue("nothing was thrown", thrown.isEmpty());
	}

	@Test
	public void aChoiceTheBattleDoesNotAcceptChangesNothing()
	{
		game.data().team.add(creature("goblin", 30));
		director.begin(game.startBattle(new WildEntry(1, GOBLIN, 30, 1)), null);

		assertFalse(director.choose(BattleChoice.switchTo(3)));
		assertFalse(director.choose(BattleChoice.item()));

		assertTrue(director.promptOpen());
		assertEquals(0, shown.size());
	}

	@Test
	public void aChampionBattleHasNoJarsButTheSameChoices()
	{
		game.data().team.add(creature("king black dragon", 60));
		game.moveTo(3222, 3218, 0);
		FakeScene scene = new FakeScene();
		director.begin(game.startChallenge("lumbridge"), scene);
		scene.ready = true;
		director.gameTick(false);

		assertFalse(director.view().canCatch());

		director.choose(BattleChoice.attack());

		assertEquals(Arrays.asList("append 1", "finish true"), scene.told);
		assertEquals(1, game.data().battlesWon);
	}

	private FriendTurnBattle friendBattle(List<Combatant> mine, List<Combatant> theirs, boolean first,
		List<GielinorGoPartyMessage> sent)
	{
		return new FriendTurnBattle(5L, first ? 2L : 1L, first ? "Bob" : "Ann", first, mine, theirs, 99L, sent::add);
	}

	@Test
	public void aBattleWithAFriendWaitsForTheFriendThenShowsTheTurn()
	{
		game.data().team.add(creature("king black dragon", 60));
		List<GielinorGoPartyMessage> fromMe = new ArrayList<>();
		List<GielinorGoPartyMessage> fromFriend = new ArrayList<>();
		List<Combatant> mine = new ArrayList<>(Arrays.asList(new Combatant(DRAGON, 60, 1.0)));
		List<Combatant> theirs = new ArrayList<>(Arrays.asList(new Combatant(CHICKEN, 2, 1.0)));
		FriendTurnBattle mineBattle = friendBattle(mine, theirs, true, fromMe);
		FriendTurnBattle friendSide = friendBattle(
			new ArrayList<>(Arrays.asList(new Combatant(CHICKEN, 2, 1.0))),
			new ArrayList<>(Arrays.asList(new Combatant(DRAGON, 60, 1.0))), false, fromFriend);
		director.begin(game.startFriendBattle(mineBattle), null);

		director.gameTick(false);
		assertTrue(director.promptOpen());
		assertFalse(director.view().waitingForFriend());
		assertEquals("Bob", director.view().friendName());

		assertTrue(director.choose(BattleChoice.attack()));
		assertEquals(1, fromMe.size());
		assertTrue(shown.isEmpty());
		director.gameTick(false);
		assertTrue(director.view().waitingForFriend());
		assertFalse(director.choose(BattleChoice.attack()));

		friendSide.opened(now);
		friendSide.local(BattleChoice.attack(), now);
		GielinorGoPartyMessage reply = fromFriend.get(0);
		reply.setMemberId(2L);
		mineBattle.remote(reply, now);
		director.gameTick(false);

		assertEquals(1, shown.size());
		assertFalse(director.active());
	}

	private void playHealingTurn(FriendTurnBattle mineBattle, FriendTurnBattle friendSide,
		List<GielinorGoPartyMessage> fromFriend, BattleChoice choice)
	{
		director.gameTick(false);
		assertTrue(director.choose(choice));
		friendSide.opened(now);
		friendSide.local(BattleChoice.attack(), now);
		GielinorGoPartyMessage reply = fromFriend.remove(0);
		reply.setMemberId(2L);
		mineBattle.remote(reply, now);
		director.gameTick(false);
	}

	@Test
	public void anItemUsedInABattleWithAFriendIsNeverTakenFromTheSave()
	{
		game.data().team.add(creature("king black dragon", 60));
		game.data().addItems(ItemType.SMALL_SALVE, 2);
		List<GielinorGoPartyMessage> fromMe = new ArrayList<>();
		List<GielinorGoPartyMessage> fromFriend = new ArrayList<>();
		FriendTurnBattle mineBattle = friendBattle(
			new ArrayList<>(Arrays.asList(new Combatant(DRAGON, 60, 0.5))),
			new ArrayList<>(Arrays.asList(new Combatant(CHICKEN, 2, 1.0))), true, fromMe);
		FriendTurnBattle friendSide = friendBattle(
			new ArrayList<>(Arrays.asList(new Combatant(CHICKEN, 2, 1.0))),
			new ArrayList<>(Arrays.asList(new Combatant(DRAGON, 60, 0.5))), false, fromFriend);
		director.begin(game.startFriendBattle(mineBattle), null);

		playHealingTurn(mineBattle, friendSide, fromFriend, BattleChoice.item(ItemType.SMALL_SALVE, 0));

		assertEquals(2, game.data().itemCount(ItemType.SMALL_SALVE));
		assertEquals(1, director.session().friendItemsLeft(ItemType.SMALL_SALVE));
		List<BattleView.ItemOption> options = director.view().items();
		assertEquals(1, options.get(0).count());
	}

	@Test
	public void anItemUsedFromThePanelDuringAFriendBattleDoesNotChangeTheBattlesItems()
	{
		game.data().team.add(creature("king black dragon", 60));
		game.data().addItems(ItemType.LARGE_SALVE, 1);
		List<GielinorGoPartyMessage> fromMe = new ArrayList<>();
		FriendTurnBattle mineBattle = friendBattle(
			new ArrayList<>(Arrays.asList(new Combatant(DRAGON, 60, 0.5))),
			new ArrayList<>(Arrays.asList(new Combatant(CHICKEN, 2, 1.0))), true, fromMe);
		director.begin(game.startFriendBattle(mineBattle), null);
		game.data().addItems(ItemType.LARGE_SALVE, -1);

		assertEquals(1, director.session().friendItemsLeft(ItemType.LARGE_SALVE));
		director.gameTick(false);
		assertTrue(director.choose(BattleChoice.item(ItemType.LARGE_SALVE, 0)));
		assertEquals(0, game.data().itemCount(ItemType.LARGE_SALVE));
	}

	@Test
	public void anItemTheBattleBeganWithoutCannotBeUsedInIt()
	{
		game.data().team.add(creature("king black dragon", 60));
		FriendTurnBattle mineBattle = friendBattle(
			new ArrayList<>(Arrays.asList(new Combatant(DRAGON, 60, 0.5))),
			new ArrayList<>(Arrays.asList(new Combatant(CHICKEN, 2, 1.0))), true, new ArrayList<>());
		director.begin(game.startFriendBattle(mineBattle), null);
		game.data().addItems(ItemType.LARGE_SALVE, 3);

		director.gameTick(false);

		assertFalse(director.choose(BattleChoice.item(ItemType.LARGE_SALVE, 0)));
	}

	@Test
	public void leavingABattleWithAFriendTellsTheFriendAndEndsItWithNoResult()
	{
		game.data().team.add(creature("king black dragon", 60));
		List<GielinorGoPartyMessage> sent = new ArrayList<>();
		FriendTurnBattle battle = friendBattle(new ArrayList<>(Arrays.asList(new Combatant(DRAGON, 60, 1.0))),
			new ArrayList<>(Arrays.asList(new Combatant(CHICKEN, 2, 1.0))), true, sent);
		director.begin(game.startFriendBattle(battle), null);

		assertTrue(director.choose(BattleChoice.run()));

		assertFalse(director.active());
		assertEquals(GielinorGoPartyMessage.LEAVE, sent.get(0).kind());
		assertEquals(0, game.data().battlesWon);
		assertTrue(shown.isEmpty());
	}

	@Test
	public void aFriendWhoLeavesEndsTheBattleOnTheNextTick()
	{
		game.data().team.add(creature("king black dragon", 60));
		FriendTurnBattle battle = friendBattle(new ArrayList<>(Arrays.asList(new Combatant(DRAGON, 60, 1.0))),
			new ArrayList<>(Arrays.asList(new Combatant(CHICKEN, 2, 1.0))), true, new ArrayList<>());
		director.begin(game.startFriendBattle(battle), null);
		battle.end(FriendTurnBattle.End.LEFT);

		director.gameTick(false);

		assertFalse(director.active());
	}
}
