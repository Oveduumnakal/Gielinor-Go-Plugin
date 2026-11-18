/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** The painted battle controls follow the rules of the side panel's battle box and report each choice once. */
public class BattleControlsModelTest
{
	private static final Species GOBLIN = new Species("Goblin", 13, CombatType.STAB);
	private static final WindowStyle STYLE = WindowStyle.of(WindowTheme.DARK_GLASS, 1.0);

	private final List<BattleChoice> chosen = new ArrayList<>();
	private final BattleControlsModel model = new BattleControlsModel(chosen::add);

	private static List<BattleView.Option> twoOptions()
	{
		return Arrays.asList(new BattleView.Option(1, "Imp", 9, 1.0), new BattleView.Option(3, "Chicken", 4, 0.5));
	}

	private static BattleView view(boolean forced, boolean canCatch, List<BattleView.Option> options, int seconds)
	{
		Combatant mine = new Combatant(GOBLIN, "Archibald the Magnificent", 42, 0.6);
		Combatant foe = new Combatant(GOBLIN, "Lesser demon", 17, 0.25);
		return new BattleView(mine, foe, forced, canCatch, JarType.BRONZE, 12, seconds, seconds / 10.0, options);
	}

	private static BattleView viewWithMoves(Move... moves)
	{
		Combatant mine = new Combatant(GOBLIN, "Bob", 42, 0.6).moves(Arrays.asList(moves));
		Combatant foe = new Combatant(GOBLIN, "Lesser demon", 17, 0.25);
		return new BattleView(mine, foe, false, true, JarType.BRONZE, 12, 10, 1.0, twoOptions());
	}

	private static BattleView viewWithItems(List<BattleView.ItemOption> items)
	{
		Combatant mine = new Combatant(GOBLIN, "Bob", 40, 0.5);
		Combatant foe = new Combatant(GOBLIN, "Foe", 10, 1.0);
		return new BattleView(mine, foe, false, false, null, 0, 10, 1.0, Collections.emptyList(), items);
	}

	private static List<BattleView.Option> pair()
	{
		return Arrays.asList(new BattleView.Option(0, "Bob", 40, 0.5), new BattleView.Option(1, "Ann", 30, 0.2));
	}

	private Hit hit(String id)
	{
		model.layout(STYLE);
		for (Hit hit : model.hits())
			if (hit.id().equals(id))
				return hit;

		return null;
	}

	private void click(String id)
	{
		Hit hit = hit(id);
		assertNotNull("no button " + id + " on " + model.page(), hit);
		Rectangle b = hit.bounds();
		model.onMoved(b.x + b.width / 2, b.y + b.height / 2);
		model.onPressed(b.x + b.width / 2, b.y + b.height / 2, MouseEvent.BUTTON1);
		model.onReleased(b.x + b.width / 2, b.y + b.height / 2, MouseEvent.BUTTON1);
	}

	private List<String> ids()
	{
		model.layout(STYLE);
		List<String> ids = new ArrayList<>();
		for (Hit hit : model.hits())
			ids.add(hit.id());

		return ids;
	}

	@Test
	public void theControlsAreHiddenUntilABattleWaits()
	{
		assertFalse(model.visible());
		assertTrue(model.hits().isEmpty());

		model.show(view(false, true, twoOptions(), 10), BattleControlsModel.Keys.NONE);
		assertTrue(model.visible());
		assertEquals(Arrays.asList("attack", "switch", "catch", "item", "run"), ids());

		model.show(null, BattleControlsModel.Keys.NONE);
		assertFalse(model.visible());
		model.layout(STYLE);
		assertTrue(model.hits().isEmpty());
	}

	@Test
	public void theButtonsReportEachChoiceOnce()
	{
		model.show(view(false, true, twoOptions(), 10), BattleControlsModel.Keys.NONE);

		click(BattleControlsModel.ID_ATTACK);
		click(BattleControlsModel.ID_CATCH);
		click(BattleControlsModel.ID_RUN);

		assertEquals(Arrays.asList(BattleChoice.attack(), BattleChoice.catching(null), BattleChoice.run()), chosen);
	}

	@Test
	public void attackOpensTheMovesWhenTheCreatureKnowsMoreThanOne()
	{
		model.show(viewWithMoves(TestMoves.BITE, TestMoves.DRAGONFIRE), BattleControlsModel.Keys.NONE);

		assertEquals("Attack...", hit("attack") == null ? "" : label("attack"));
		click(BattleControlsModel.ID_ATTACK);
		assertTrue("Attack does not choose by itself when there is a choice", chosen.isEmpty());
		assertEquals(BattleControlsModel.Page.MOVES, model.page());
		assertEquals(Arrays.asList("move:0", "move:1", "back"), ids());
		assertEquals("Bite · Stab, power 1, accuracy 100%", label("move:0"));
		assertEquals("Dragonfire · Magic, power 1.6, accuracy 90%", label("move:1"));

		click("move:1");

		assertEquals(Collections.singletonList(BattleChoice.attack(1)), chosen);
		assertEquals(BattleControlsModel.Page.MAIN, model.page());
	}

	@Test
	public void aCreatureWithOneMoveJustAttacks()
	{
		model.show(viewWithMoves(TestMoves.BITE), BattleControlsModel.Keys.NONE);

		click(BattleControlsModel.ID_ATTACK);

		assertEquals("Attack", label("attack"));
		assertEquals(Collections.singletonList(BattleChoice.attack()), chosen);
	}

	@Test
	public void theSwitchListHoldsTheCreaturesThatCanStepIn()
	{
		model.show(view(false, true, twoOptions(), 10), BattleControlsModel.Keys.NONE);

		click(BattleControlsModel.ID_SWITCH);

		assertEquals(Arrays.asList("switch:0", "switch:1", "back"), ids());
		assertEquals("Imp Lv 9 · 100%", label("switch:0"));
		assertEquals("Chicken Lv 4 · 50%", label("switch:1"));

		click("switch:1");

		assertEquals(Collections.singletonList(BattleChoice.switchTo(3)), chosen);
	}

	@Test
	public void aForcedSwitchLeavesOnlyTheSwitchButton()
	{
		model.show(view(true, false, twoOptions(), 4), BattleControlsModel.Keys.NONE);

		assertFalse(hit("attack").enabled());
		assertFalse(hit("run").enabled());
		assertFalse(hit("catch").enabled());
		assertTrue(hit("switch").enabled());

		click(BattleControlsModel.ID_ATTACK);
		click(BattleControlsModel.ID_RUN);
		assertTrue("a disabled button does nothing", chosen.isEmpty());
		assertEquals(BattleControlsModel.Page.MAIN, model.page());
	}

	@Test
	public void switchAndCatchAreOffWhenThereIsNothingToStepInOrThrow()
	{
		model.show(view(false, false, Collections.emptyList(), 10), BattleControlsModel.Keys.NONE);

		assertFalse(hit("switch").enabled());
		assertFalse(hit("catch").enabled());
		assertTrue(hit("attack").enabled());
		assertTrue(hit("run").enabled());
		assertEquals("No other creature can fight", hit("switch").tip());
	}

	@Test
	public void theItemListUsesOneAtOnceWhenItHelpsOneCreatureAndAsksOtherwise()
	{
		List<BattleView.Option> both = pair();
		model.show(viewWithItems(Arrays.asList(new BattleView.ItemOption(ItemType.SMALL_SALVE, 3, both),
			new BattleView.ItemOption(ItemType.ANTIDOTE, 1, Collections.singletonList(both.get(0))))),
			BattleControlsModel.Keys.NONE);

		assertTrue(hit("item").enabled());
		click(BattleControlsModel.ID_ITEM);
		assertEquals(Arrays.asList("item:0", "item:1", "back"), ids());

		click("item:1");
		assertEquals(Collections.singletonList(BattleChoice.item(ItemType.ANTIDOTE, 0)), chosen);

		chosen.clear();
		click(BattleControlsModel.ID_ITEM);
		click("item:0");
		assertTrue("an item that could help several asks which one", chosen.isEmpty());
		assertEquals(BattleControlsModel.Page.TARGETS, model.page());
		assertEquals(Arrays.asList("target:0", "target:1", "back"), ids());
		assertEquals("Ann Lv 30 · 20%", label("target:1"));

		click("target:1");
		assertEquals(Collections.singletonList(BattleChoice.item(ItemType.SMALL_SALVE, 1)), chosen);
		assertEquals(BattleControlsModel.Page.MAIN, model.page());
	}

	@Test
	public void theItemButtonIsGreyWhenNoItemWouldHelp()
	{
		model.show(viewWithItems(Collections.emptyList()), BattleControlsModel.Keys.NONE);

		assertFalse(hit("item").enabled());
	}

	@Test
	public void backAndARightClickGoBackOneStep()
	{
		List<BattleView.Option> both = pair();
		model.show(viewWithItems(Collections.singletonList(new BattleView.ItemOption(ItemType.SMALL_SALVE, 3, both))),
			BattleControlsModel.Keys.NONE);
		click(BattleControlsModel.ID_ITEM);
		click("item:0");
		assertEquals(BattleControlsModel.Page.TARGETS, model.page());

		click(BattleControlsModel.ID_BACK);
		assertEquals(BattleControlsModel.Page.ITEMS, model.page());

		model.onPressed(5, 5, MouseEvent.BUTTON3);
		model.onReleased(5, 5, MouseEvent.BUTTON3);
		assertEquals(BattleControlsModel.Page.MAIN, model.page());

		model.onPressed(5, 5, MouseEvent.BUTTON3);
		model.onReleased(5, 5, MouseEvent.BUTTON3);
		assertEquals(BattleControlsModel.Page.MAIN, model.page());
		assertTrue(chosen.isEmpty());
	}

	@Test
	public void aClickThatStartsOnOneButtonAndEndsOnAnotherDoesNothing()
	{
		model.show(view(false, true, twoOptions(), 10), BattleControlsModel.Keys.NONE);
		Rectangle attack = hit("attack").bounds();
		Rectangle run = hit("run").bounds();

		model.onPressed(attack.x + 2, attack.y + 2, MouseEvent.BUTTON1);
		assertEquals("attack", model.pressed());
		model.onReleased(run.x + 2, run.y + 2, MouseEvent.BUTTON1);

		assertTrue(chosen.isEmpty());
		assertEquals(null, model.pressed());
	}

	@Test
	public void aRightClickOnAButtonChoosesNothing()
	{
		model.show(view(false, true, twoOptions(), 10), BattleControlsModel.Keys.NONE);
		Rectangle run = hit("run").bounds();

		model.onPressed(run.x + 2, run.y + 2, MouseEvent.BUTTON3);
		model.onReleased(run.x + 2, run.y + 2, MouseEvent.BUTTON3);

		assertTrue(chosen.isEmpty());
	}

	@Test
	public void aListClosesWhenTheNewStateHasNothingToShowInIt()
	{
		model.show(viewWithMoves(TestMoves.BITE, TestMoves.DRAGONFIRE), BattleControlsModel.Keys.NONE);
		click(BattleControlsModel.ID_ATTACK);
		assertEquals(BattleControlsModel.Page.MOVES, model.page());

		model.show(viewWithMoves(TestMoves.BITE, TestMoves.DRAGONFIRE), BattleControlsModel.Keys.NONE);
		assertEquals("the list stays while the state still has it", BattleControlsModel.Page.MOVES, model.page());

		model.show(viewWithMoves(TestMoves.BITE), BattleControlsModel.Keys.NONE);
		assertEquals(BattleControlsModel.Page.MAIN, model.page());

		model.show(null, BattleControlsModel.Keys.NONE);
		model.show(viewWithMoves(TestMoves.BITE, TestMoves.DRAGONFIRE), BattleControlsModel.Keys.NONE);
		assertEquals("a new battle starts on the main page", BattleControlsModel.Page.MAIN, model.page());
	}

	@Test
	public void theLabelsShowTheHotkeysThatAreSet()
	{
		BattleControlsModel.Keys keys = new BattleControlsModel.Keys("F1", null, "F3", null, "F4");
		model.show(view(false, true, twoOptions(), 10), keys);

		assertEquals("Attack (F1)", label("attack"));
		assertEquals("Switch", label("switch"));
		assertEquals("Catch x12 (F3)", label("catch"));
		assertEquals("Run (F4)", label("run"));
	}

	@Test
	public void theTitleCountsTheSecondsOrAsksForTheNextCreature()
	{
		model.show(view(false, true, twoOptions(), 7), BattleControlsModel.Keys.NONE);
		assertEquals("Your turn · 7s", model.title());

		model.show(view(true, true, twoOptions(), 7), BattleControlsModel.Keys.NONE);
		assertEquals("Pick your next creature · 7s", model.title());
	}

	@Test
	public void aFriendBattleWaitsWithEverythingButRunOff()
	{
		BattleView waiting = view(false, false, twoOptions(), 10).againstFriend("Ann", true);
		model.show(waiting, BattleControlsModel.Keys.NONE);

		assertFalse(hit("attack").enabled());
		assertFalse(hit("switch").enabled());
		assertFalse(hit("item").enabled());
		assertTrue(hit("run").enabled());
		assertEquals("Waiting for Ann...", model.title());
	}

	@Test
	public void theHintSaysWhyAButtonIsOffWhileThePointerIsOverIt()
	{
		model.show(view(false, false, Collections.emptyList(), 10), BattleControlsModel.Keys.NONE);
		Rectangle swap = hit("switch").bounds();

		model.onMoved(swap.x + 2, swap.y + 2);
		assertEquals("No other creature can fight", model.hint());

		model.onLeft();
		assertEquals("", model.hint());
	}

	@Test
	public void everyButtonSitsInsideTheBoxAndNoTwoOverlapAtAnyScale()
	{
		BattleView many = viewWithMoves(TestMoves.BITE, TestMoves.DRAGONFIRE);
		for (double scale : new double[] {WindowFit.MIN_SCALE, 1.0, 1.5, 2.0})
		{
			WindowStyle style = WindowStyle.of(WindowTheme.DARK_GLASS, scale);
			for (String open : new String[] {"", BattleControlsModel.ID_ATTACK, BattleControlsModel.ID_SWITCH})
			{
				BattleControlsModel m = new BattleControlsModel(chosen::add);
				m.show(many, BattleControlsModel.Keys.NONE);
				BattleControlsModel.Layout layout = m.layout(style);
				if (!open.isEmpty())
				{
					Rectangle b = layout.buttons.stream().filter(x -> x.id().equals(open)).findFirst().get().bounds();
					m.onPressed(b.x + 2, b.y + 2, MouseEvent.BUTTON1);
					m.onReleased(b.x + 2, b.y + 2, MouseEvent.BUTTON1);
					layout = m.layout(style);
				}

				Rectangle box = new Rectangle(layout.size);
				for (BattleControlsModel.Button a : layout.buttons)
				{
					assertTrue(scale + " " + open + " " + a.id(), box.contains(a.bounds()));
					for (BattleControlsModel.Button other : layout.buttons)
						assertTrue(a == other || !a.bounds().intersects(other.bounds()));
				}
			}
		}
	}

	private String label(String id)
	{
		model.layout(STYLE);
		for (BattleControlsModel.Button button : model.layout(STYLE).buttons)
			if (button.id().equals(id))
				return button.label();

		return null;
	}
}
