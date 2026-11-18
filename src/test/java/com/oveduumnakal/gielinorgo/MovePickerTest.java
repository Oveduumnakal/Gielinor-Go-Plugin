/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Checks the painted move picker: the words, the buttons for each case, the answers it gives and the menu that picks
 * the move to forget.
 */
public class MovePickerTest
{
	private static final Move SLASH = new Move("slash", "Slash", CombatType.SLASH, 1.0, 0.95);
	private static final Move BASH = new Move("bash", "Bash", CombatType.CRUSH, 1.4, 0.8);
	private static final Move SPARK = new Move("spark", "Spark", CombatType.MAGIC, 1.2, 0.9);

	private final List<Integer> answers = new ArrayList<>();
	private final List<String> events = new ArrayList<>();

	private static CreatureView view(List<Move> known, List<Move> pending)
	{
		return new CreatureView("Goblin", 12, 0.5, 1.0, Rarity.COMMON, CombatType.SLASH, false, "Gobbo",
			StatRolls.AVERAGE, 0, known, pending);
	}

	@Test
	public void withNoWaitingMoveTheDialogJustListsTheMovesAndCloses()
	{
		CreatureView view = view(Arrays.asList(SLASH, BASH), Collections.emptyList());

		assertEquals("Moves", MovePicker.title(view));
		assertEquals(Collections.singletonList("Close"), MovePicker.buttons(view));
		String text = MovePicker.message(view);
		assertTrue(text, text.startsWith("Gobbo (Goblin)"));
		assertTrue(text, text.contains("Slash: Slash, power 1, accuracy 95%"));
		assertTrue(text, text.contains("Bash: Crush, power 1.4, accuracy 80%"));
		assertFalse(text, text.contains("New:"));
	}

	@Test
	public void aWaitingMoveGetsItsOwnTitleDescriptionAndTheQuestion()
	{
		CreatureView view = view(Arrays.asList(SLASH, BASH), Collections.singletonList(SPARK));

		assertEquals("New move: Spark", MovePicker.title(view));
		assertEquals(Arrays.asList("Skip", "Forget one"), MovePicker.buttons(view));
		String text = MovePicker.message(view);
		assertTrue(text, text.contains("New: Spark: Magic, power 1.2, accuracy 90%"));
		assertTrue(text, text.contains("forget one of the moves"));
	}

	@Test
	public void aCreatureWithNoKnownMoveCanOnlySkipTheWaitingOne()
	{
		CreatureView view = view(Collections.emptyList(), Collections.singletonList(SPARK));

		assertEquals(Collections.singletonList("Skip"), MovePicker.buttons(view));
		assertTrue(MovePicker.message(view).contains("No moves yet."));
	}

	@Test
	public void skipAnswersMinusOneAndStartsOnSkip()
	{
		CreatureView view = view(Arrays.asList(SLASH, BASH), Collections.singletonList(SPARK));
		Modal modal = MovePicker.modal(view, answers::add, () -> events.add("forget"));
		modal.open(0L);

		assertEquals(0, modal.selected());
		assertTrue(modal.onKeyPressed(KeyEvent.VK_ENTER, 0, 0L));
		assertEquals(Collections.singletonList(-1), answers);
		assertTrue(events.isEmpty());
		assertFalse(modal.isOpen());
	}

	@Test
	public void forgetOneHandsOverToTheMenuAndAnswersNothingYet()
	{
		CreatureView view = view(Arrays.asList(SLASH, BASH), Collections.singletonList(SPARK));
		Modal modal = MovePicker.modal(view, answers::add, () -> events.add("forget"));
		modal.open(0L);

		modal.onKeyPressed(KeyEvent.VK_RIGHT, 0, 0L);
		modal.onKeyPressed(KeyEvent.VK_ENTER, 0, 0L);

		assertEquals(Collections.singletonList("forget"), events);
		assertTrue(answers.isEmpty());
	}

	@Test
	public void escapeCancelsWithoutAnAnswer()
	{
		CreatureView view = view(Arrays.asList(SLASH, BASH), Collections.singletonList(SPARK));
		Modal modal = MovePicker.modal(view, answers::add, () -> events.add("forget"));
		modal.open(0L);

		modal.onKeyPressed(KeyEvent.VK_ESCAPE, 0, 0L);

		assertTrue(answers.isEmpty());
		assertTrue(events.isEmpty());
		assertFalse(modal.isOpen());
	}

	@Test
	public void closeJustClosesWhenNothingWaits()
	{
		CreatureView view = view(Arrays.asList(SLASH), Collections.emptyList());
		Modal modal = MovePicker.modal(view, answers::add, () -> events.add("forget"));
		modal.open(0L);

		modal.onKeyPressed(KeyEvent.VK_ENTER, 0, 0L);

		assertFalse(modal.isOpen());
		assertTrue(answers.isEmpty());
		assertTrue(events.isEmpty());
	}

	@Test
	public void theForgetMenuHasOneItemPerKnownMoveAndAnswersItsPlace()
	{
		CreatureView view = view(Arrays.asList(SLASH, BASH, SPARK), Collections.singletonList(SLASH));
		List<ContextMenu.Item> items = MovePicker.forgetItems(view, answers::add);

		assertEquals(3, items.size());
		assertEquals("Forget Slash", items.get(0).label());
		assertEquals("Forget Bash", items.get(1).label());
		assertEquals("Forget Spark", items.get(2).label());

		WindowStyle style = WidgetKit.style(1.0);
		ContextMenu menu = new ContextMenu();
		menu.open(items, 5, 5, new Rectangle(0, 0, 640, 480), style,
			WidgetKit.metrics(style, WindowFonts.Face.REGULAR), true, 0L);
		menu.move(1);
		menu.activate();

		assertEquals(Collections.singletonList(1), answers);
	}
}
