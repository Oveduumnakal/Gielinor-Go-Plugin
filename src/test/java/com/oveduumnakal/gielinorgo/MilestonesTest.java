/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.swing.JPanel;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the Creature Log milestones: thresholds, families, paying once, waiting eggs and the Gilded charm. */
public class MilestonesTest
{
	private static final int SPECIES = 520;

	private final List<String> messages = new ArrayList<>();
	private SpeciesTable table;
	private Milestones milestones;
	private GoGame game;

	@Before
	public void setUp()
	{
		List<Species> all = new ArrayList<>();
		for (int i = 0; i < SPECIES; i++)
			all.add(new Species(String.format("Beast %03d", i), 1 + i, CombatType.STAB));

		table = new SpeciesTable(all);
		Family twins = new Family("twins", "Twins", JarType.EMBER,
			Arrays.asList("beast 003", "beast 400", "beast 001"));
		milestones = new Milestones(table, Arrays.asList(twins, new Family("tiny", "Tiny", JarType.CAVE,
			Arrays.asList("beast 001", "nobody", "beast 002"))));
		game = new GoGame(table, new Random(3), () -> 0L, notice -> messages.add(notice.text()));
		game.milestones(milestones);
	}

	private Rarity eggAt(int index)
	{
		Egg egg = game.data().eggs.get(index);
		return egg.rarityTier();
	}

	private static SaveData saveWithCaught(int count)
	{
		SaveData data = SaveData.fresh();
		for (int i = 0; i < count; i++)
			data.caught.put(String.format("beast %03d", i), 1);

		return data;
	}

	@Test
	public void eachThresholdIsReachedAtItsCount()
	{
		assertTrue(milestones.reached(saveWithCaught(24)).stream().noneMatch(id -> id.startsWith("species-")));
		assertEquals(Arrays.asList("species-25"), milestones.reached(saveWithCaught(25)).subList(0, 1));
		assertTrue(milestones.reached(saveWithCaught(50)).contains("species-50"));
		assertTrue(milestones.reached(saveWithCaught(100)).contains("species-100"));
		assertFalse(milestones.reached(saveWithCaught(249)).contains("species-250"));
		assertTrue(milestones.reached(saveWithCaught(250)).contains("species-250"));
		assertFalse(milestones.reached(saveWithCaught(499)).contains("species-500"));
		assertTrue(milestones.reached(saveWithCaught(500)).contains("species-500"));
	}

	@Test
	public void rewardsMatchTheTable()
	{
		assertEquals("5 Iron jars", milestones.rewardFor("species-25").text());
		assertEquals("5 Rune jars + Rare egg", milestones.rewardFor("species-50").text());
		assertEquals("5 Dragon jars + Epic egg", milestones.rewardFor("species-100").text());
		assertEquals("Gilded charm", milestones.rewardFor("species-250").text());
		assertEquals("10 Dragon jars + Legendary egg", milestones.rewardFor("species-500").text());
		assertNull(milestones.rewardFor("species-7"));
	}

	@Test
	public void aFamilyCompletesOnlyWithEveryMemberAndPaysItsTopRarity()
	{
		SaveData data = SaveData.fresh();
		data.caught.put("beast 003", 1);
		data.caught.put("beast 001", 1);
		assertFalse(milestones.reached(data).contains("family-twins"));

		data.caught.put("beast 400", 1);

		assertTrue(milestones.reached(data).contains("family-twins"));
		Milestones.Reward reward = milestones.rewardFor("family-twins");
		assertEquals(JarType.EMBER, reward.jar());
		assertEquals(3, reward.jars());
		assertEquals(Rarity.LEGENDARY, reward.egg());
	}

	@Test
	public void aFamilyIgnoresMembersTheSpeciesListDoesNotKnow()
	{
		SaveData data = SaveData.fresh();
		data.caught.put("beast 001", 1);
		data.caught.put("beast 002", 1);

		assertNull("two known members is too few for a family", milestones.byId("family-tiny"));
	}

	@Test
	public void anOldSaveWith120SpeciesIsPaid25And50And100OnLoad()
	{
		SaveData old = saveWithCaught(120);
		int ironBefore = old.jarCount(JarType.IRON);

		game.load(old);

		assertTrue(game.data().milestones.containsAll(Arrays.asList("species-25", "species-50", "species-100")));
		assertFalse(game.data().milestones.contains("species-250"));
		assertEquals(ironBefore + 5, game.data().jarCount(JarType.IRON));
		assertEquals(5, game.data().jarCount(JarType.RUNE));
		assertEquals(5, game.data().jarCount(JarType.DRAGON));
		assertEquals(2, game.data().eggs.size());
		assertEquals(Rarity.RARE, eggAt(0));
		assertEquals(Rarity.EPIC, eggAt(1));
		assertTrue(messages.contains("Milestone: 50 species! +5 Rune jars, +1 Rare egg"));
	}

	@Test
	public void aMilestoneIsPaidOnce()
	{
		game.load(saveWithCaught(30));
		int jars = game.data().jarCount(JarType.IRON);
		long said = messages.stream().filter(line -> line.startsWith("Milestone:")).count();

		game.load(game.data());
		game.addSteps(10);

		assertEquals(jars, game.data().jarCount(JarType.IRON));
		assertEquals(said, messages.stream().filter(line -> line.startsWith("Milestone:")).count());
	}

	@Test
	public void aCatchThatReachesAMilestonePaysIt()
	{
		game.load(saveWithCaught(24));

		assertTrue(game.receive(new Creature("beast 300", 5, 0L)));

		assertTrue(game.data().milestones.contains("species-25"));
		assertEquals(5, game.data().jarCount(JarType.IRON));
	}

	@Test
	public void anEggRewardWaitsForAFreeEggSlot()
	{
		SaveData old = saveWithCaught(60);
		for (int i = 0; i < SaveData.MAX_EGGS; i++)
			old.eggs.add(new Egg(Rarity.COMMON));

		game.load(old);

		assertTrue(game.data().milestones.contains("species-25"));
		assertFalse(game.data().milestones.contains("species-50"));
		assertEquals(0, game.data().jarCount(JarType.RUNE));
		assertEquals(SaveData.MAX_EGGS, game.data().eggs.size());

		game.giveAway(game.data().eggs.get(0));
		game.addSteps(1);

		assertTrue(game.data().milestones.contains("species-50"));
		assertEquals(5, game.data().jarCount(JarType.RUNE));
		assertEquals(SaveData.MAX_EGGS, game.data().eggs.size());
	}

	@Test
	public void aHatchingEggNeverBreaksTheEggList()
	{
		SaveData data = saveWithCaught(49);
		Egg ready = new Egg(Rarity.LEGENDARY);
		ready.tilesLeft = 1;
		data.eggs.add(ready);
		game.load(data);

		game.addSteps(5);

		assertTrue(game.data().milestones.contains("species-50"));
		assertEquals(1, game.data().eggs.size());
		assertEquals(Rarity.RARE, eggAt(0));
	}

	@Test
	public void theCharmHalvesTheGildedOdds()
	{
		game.load(saveWithCaught(100));
		assertEquals(WildTracker.GILDED_ONE_IN, game.gildedOneIn());
		assertFalse(game.hasGildedCharm());

		game.load(saveWithCaught(250));

		assertTrue(game.hasGildedCharm());
		assertEquals(250, game.gildedOneIn());
		assertEquals(125, game.gildedOneIn(250));
		assertEquals(1, game.gildedOneIn(1));
	}

	@Test
	public void theViewShowsTheNextGoalAndEveryMilestone()
	{
		game.load(saveWithCaught(87));

		MilestoneView view = game.snapshot().milestones();

		assertEquals("Next: 100 species", view.nextLabel());
		assertEquals(87, view.nextCurrent());
		assertEquals(100, view.nextTarget());
		assertEquals(6, view.rows().size());
		MilestoneView.Row first = view.rows().get(0);
		MilestoneView.Row third = view.rows().get(2);
		assertTrue(first.paid());
		assertFalse(third.paid());
		assertEquals(87, third.current());
	}

	@Test
	public void theStripAndTheListFitTheNarrowPanel()
	{
		game.load(saveWithCaught(87));
		MilestoneView view = game.snapshot().milestones();
		MilestoneStrip strip = new MilestoneStrip();
		strip.update(view);
		strip.setSize(new Dimension(209, 120));
		strip.doLayout();
		JPanel list = MilestoneStrip.list(view);
		list.setSize(new Dimension(225, 400));
		list.doLayout();
		BufferedImage image = new BufferedImage(225, 400, BufferedImage.TYPE_INT_ARGB);
		list.printAll(image.getGraphics());

		assertTrue(strip.isVisible());
		assertTrue(strip.getPreferredSize().width <= 209);
		assertTrue(list.getPreferredSize().width <= 225);
	}
}
