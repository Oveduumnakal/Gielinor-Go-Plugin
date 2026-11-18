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
import static org.junit.Assert.assertTrue;

/** Pins eggs to the tracker through the game service. */
public class EggPinGameTest
{
	private final List<String> messages = new ArrayList<>();
	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(new Species("Chicken", 1, CombatType.STAB)));
		game = new GoGame(table, new Random(5), () -> 20_000L * 86_400_000L, notice -> messages.add(notice.text()));
		for (int i = 0; i < 3; i++)
			game.data().eggs.add(new Egg(Rarity.COMMON));

		egg(0).tilesLeft = 5;
	}

	private Egg egg(int place)
	{
		List<Egg> eggs = game.data().eggs;
		return eggs.get(place);
	}

	private List<TrackerView.Line> trackerLines()
	{
		TrackerView view = game.snapshot().tracker();
		return view.lines();
	}

	private EggView eggView(int place)
	{
		List<EggView> views = game.snapshot().eggs();
		return views.get(place);
	}

	private TrackerView.Line eggLine(int place)
	{
		for (TrackerView.Line line : trackerLines())
		{
			if (line.id().equals("egg:" + place))
				return line;
		}

		return null;
	}

	@Test
	public void aPinnedEggShowsItsStepsLeft()
	{
		egg(0).tilesLeft = 1000;
		Egg egg = egg(1);

		assertTrue(game.pinEgg(egg));

		TrackerView.Line line = eggLine(1);
		assertEquals("Common egg", line.label());
		assertEquals("1000 steps", line.valueText());
		assertEquals(Rarity.COMMON, line.rarity());
		game.addSteps(100);
		assertEquals("900 steps", eggLine(1).valueText());
		assertEquals(0.1, eggLine(1).fraction(), 1e-9);
	}

	@Test
	public void aReadyEggSaysReady()
	{
		egg(0).tilesLeft = 0;
		game.pinEgg(egg(0));

		assertEquals("Ready", eggLine(0).valueText());
	}

	@Test
	public void theFlagStaysOnTheSameEggWhenAnotherOneHatches()
	{
		Egg pinned = egg(1);
		game.pinEgg(pinned);

		game.addSteps(5);

		assertEquals(2, game.data().eggs.size());
		assertTrue(egg(0) == pinned);
		assertTrue(pinned.pinned);
		assertFalse(egg(1).pinned);
		assertEquals(1, trackerLines().size());
		TrackerView.Line only = trackerLines().get(0);
		assertEquals("egg:0", only.id());
		assertTrue("the pinned egg did not hatch, so there is no news", game.takePinNews().isEmpty());
	}

	@Test
	public void aPinnedEggThatHatchesLeavesOneNewsLabelAndNoPin()
	{
		game.pinEgg(egg(0));

		game.addSteps(5);

		assertEquals(0, game.pinCount());
		assertEquals(Arrays.asList("Common egg"), game.takePinNews());
		assertTrue(game.takePinNews().isEmpty());
		assertTrue(trackerLines().isEmpty());
	}

	@Test
	public void anEggGivenAwayLeavesNoNews()
	{
		Egg egg = egg(0);
		game.pinEgg(egg);

		game.giveAway(egg);

		assertEquals(0, game.pinCount());
		assertTrue(game.takePinNews().isEmpty());
	}

	@Test
	public void unpinAndAnEggThatIsNotCarriedChangeNothing()
	{
		Egg egg = egg(2);
		game.pinEgg(egg);
		game.unpinEgg(egg);
		assertEquals(0, game.pinCount());

		assertFalse(game.pinEgg(new Egg(Rarity.RARE)));
		assertEquals(0, game.pinCount());
	}

	@Test
	public void theEggViewCarriesThePin()
	{
		game.pinEgg(egg(2));

		assertTrue(eggView(2).pinned());
		assertFalse(eggView(1).pinned());
		EggView pinned = eggView(2);
		assertFalse(pinned.equals(pinned.withPinned(false)));
	}

	@Test
	public void aPanelPickOfAnEggPinsThatEgg()
	{
		TradePick pick = TradePick.of(2, eggView(2));

		game.pinEgg(pick, true);

		assertTrue(egg(2).pinned);
		game.pinEgg(TradePick.of(2, eggView(2)), false);
		assertFalse(egg(2).pinned);
	}
}
