/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Canvas;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import net.runelite.client.config.Keybind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Dragging rows of the pinned party box. */
public class PartyBoxInputTest
{
	private static final int FIRST = 15;
	private static final int SECOND = 35;
	private static final int GAP = 25;

	private final List<String> swaps = new ArrayList<>();
	private List<Rectangle> rows = Arrays.asList(new Rectangle(10, 10, 100, 10), new Rectangle(10, 30, 100, 10));
	private boolean covered;
	private final List<String> told = new ArrayList<>();
	private final GoGame shown = twoCreatureGame();
	private final PartyBoxInput input = new PartyBoxInput(() -> rows, () -> shown.snapshot().team(),
		new DragKey(() -> Keybind.NOT_SET), point -> covered,
		(from, onto) -> swaps.add(from.index() + ">" + onto.index()));

	private GoGame twoCreatureGame()
	{
		GoGame game = new GoGame(new SpeciesTable(Arrays.asList(new Species("Chicken", 1, CombatType.STAB),
			new Species("Cow", 2, CombatType.STAB))), new Random(1), () -> 0L, notice -> told.add(notice.text()));
		game.data().team.add(new Creature("chicken", 3, 0L));
		game.data().team.add(new Creature("cow", 5, 0L));
		return game;
	}

	private static MouseEvent event(int id, int y, int button)
	{
		return new MouseEvent(new Canvas(), id, 0L, 0, 50, y, 1, false, button);
	}

	private boolean drag(int fromY, int toY, int button)
	{
		MouseEvent press = input.mousePressed(event(MouseEvent.MOUSE_PRESSED, fromY, button));
		input.mouseDragged(event(MouseEvent.MOUSE_DRAGGED, toY, button));
		input.mouseReleased(event(MouseEvent.MOUSE_RELEASED, toY, button));
		return press.isConsumed();
	}

	@Test
	public void droppingARowOnAnotherSwapsThem()
	{
		assertTrue(drag(FIRST, SECOND, MouseEvent.BUTTON1));

		assertEquals(Collections.singletonList("0>1"), swaps);
		assertEquals(-1, input.from());
	}

	@Test
	public void droppingOnItselfOrOutsideChangesNothing()
	{
		assertTrue(drag(FIRST, FIRST, MouseEvent.BUTTON1));
		assertTrue(drag(FIRST, 200, MouseEvent.BUTTON1));

		assertTrue(swaps.isEmpty());
	}

	@Test
	public void gapsRightClicksAndCoveredRowsGoThroughToTheGame()
	{
		assertFalse(drag(GAP, SECOND, MouseEvent.BUTTON1));
		assertFalse(drag(FIRST, SECOND, MouseEvent.BUTTON3));
		covered = true;
		assertFalse(drag(FIRST, SECOND, MouseEvent.BUTTON1));

		assertTrue(swaps.isEmpty());
	}

	@Test
	public void withNoDraggableRowsEveryClickGoesThrough()
	{
		rows = Collections.emptyList();

		assertFalse(drag(FIRST, SECOND, MouseEvent.BUTTON1));
		assertTrue(swaps.isEmpty());
	}

	@Test
	public void swappingTheTeamChangesTheLead()
	{
		GoGame game = new GoGame(new SpeciesTable(Arrays.asList(new Species("Chicken", 1, CombatType.STAB))),
			new Random(1), () -> 0L, notice ->
			{
			});
		Creature first = new Creature();
		Creature second = new Creature();
		game.data().team.add(first);
		game.data().team.add(second);

		game.swapTeam(0, 1);
		game.swapTeam(0, 5);

		assertTrue(game.data().team.get(0) == second);
		assertTrue(game.data().team.get(1) == first);
	}

	@Test
	public void aDragIsRefusedWhenTheTeamChangedUnderIt()
	{
		List<TradePick> picked = new ArrayList<>();
		PartyBoxInput dragging = new PartyBoxInput(() -> rows, () -> shown.snapshot().team(),
			new DragKey(() -> Keybind.NOT_SET), point -> false, (from, onto) ->
			{
				picked.add(from);
				picked.add(onto);
			});
		dragging.mousePressed(event(MouseEvent.MOUSE_PRESSED, SECOND, MouseEvent.BUTTON1));
		Creature left = shown.data().team.remove(1);
		Creature arrived = new Creature("chicken", 9, 0L);
		shown.data().team.add(arrived);
		dragging.mouseReleased(event(MouseEvent.MOUSE_RELEASED, FIRST, MouseEvent.BUTTON1));

		shown.swapTeam(picked.get(0), picked.get(1));

		assertFalse(shown.data().team.contains(left));
		assertTrue(shown.data().team.get(1) == arrived);
		assertEquals(Collections.singletonList("That creature moved. Try again."), told);
	}

	@Test
	public void aRightClickDuringADragIsSwallowedAndTheDropStillSwaps()
	{
		input.mousePressed(event(MouseEvent.MOUSE_PRESSED, FIRST, MouseEvent.BUTTON1));
		MouseEvent press = input.mousePressed(event(MouseEvent.MOUSE_PRESSED, FIRST, MouseEvent.BUTTON3));
		MouseEvent release = input.mouseReleased(event(MouseEvent.MOUSE_RELEASED, FIRST, MouseEvent.BUTTON3));
		MouseEvent left = input.mouseReleased(event(MouseEvent.MOUSE_RELEASED, SECOND, MouseEvent.BUTTON1));

		assertTrue(press.isConsumed());
		assertTrue(release.isConsumed());
		assertTrue(left.isConsumed());
		assertEquals(Collections.singletonList("0>1"), swaps);
	}
}
