/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Plays the healing items through the game with a scripted or seeded roll and a fake clock. */
public class ItemGameTest
{
	private static final Species CHICKEN = new Species("Chicken", 1, CombatType.MELEE);
	private static final Species DRAGON = new Species("King Black Dragon", 276, CombatType.MELEE);
	private static final double EPSILON = 1e-9;

	/** Answers {@code nextDouble} from a script, then from a seeded stream. */
	private static final class Script extends Random
	{
		private final Deque<Double> rolls = new ArrayDeque<>();

		private Script(Double... values)
		{
			super(3);
			rolls.addAll(Arrays.asList(values));
		}

		@Override
		public double nextDouble()
		{
			return rolls.isEmpty() ? super.nextDouble() : rolls.poll();
		}
	}

	private final List<Notice> notices = new ArrayList<>();
	private long now = 1_000_000L;
	private SpeciesTable table;
	private GoGame game;

	@Before
	public void setUp()
	{
		table = new SpeciesTable(Arrays.asList(CHICKEN, DRAGON));
		game = newGame(new Random(11));
	}

	@Test
	public void aScriptedHerbloreDropGivesASmallSalveAndNoAntidote()
	{
		game = newGame(new Script(0.99, 0.0, 0.99));

		game.addSkillXp("HERBLORE", 30_000);

		assertEquals(1, game.data().itemCount(ItemType.SMALL_SALVE));
		assertEquals(0, game.data().itemCount(ItemType.ANTIDOTE));
		Notice find = noticeFor("+1 Small salve (Herblore)");
		assertEquals(Tone.FIND, find.tone());
		assertEquals(ItemType.SMALL_SALVE, find.item());
		assertEquals(null, find.jar());
		assertTrue(game.takeDirty());
	}

	@Test
	public void theItemRollIsSeparateFromTheJarRoll()
	{
		game = newGame(new Script(0.0, 0.0, 0.0));

		game.addSkillXp("HERBLORE", 30_000);

		assertEquals(1, game.data().jarCount(JarType.VENOM));
		assertEquals(1, game.data().itemCount(ItemType.SMALL_SALVE));
		assertEquals(1, game.data().itemCount(ItemType.ANTIDOTE));
	}

	@Test
	public void cookingFindsSalvesButNoAntidotes()
	{
		game = newGame(new Script(0.99, 0.0));

		game.addSkillXp("COOKING", 30_000);

		assertEquals(1, game.data().itemCount(ItemType.SMALL_SALVE));
		assertEquals(0, game.data().itemCount(ItemType.ANTIDOTE));
	}

	@Test
	public void unrelatedSkillsFindNoItems()
	{
		for (int i = 0; i < 300; i++)
		{
			game.addSkillXp("HITPOINTS", 30_000);
			game.addSkillXp("MINING", 30_000);
		}

		for (ItemType type : ItemType.values())
			assertEquals(type.name(), 0, game.data().itemCount(type));
	}

	@Test
	public void battleWinsDropSalvesAndRevivesOverTimeButNeverAntidotes()
	{
		game.data().trainerXp = XpRules.trainerXpForLevel(XpRules.MAX_LEVEL);
		for (int i = 0; i < 400; i++)
		{
			game.data().team.clear();
			game.data().team.add(new Creature("king black dragon", 40, now));
			game.battle(new WildEntry(1, CHICKEN, 2, 1));
		}

		int small = game.data().itemCount(ItemType.SMALL_SALVE);
		assertTrue("expected about 40 Small salves, got " + small, small >= 15 && small <= 80);
		assertTrue(game.data().itemCount(ItemType.LARGE_SALVE) > 0);
		assertTrue(game.data().itemCount(ItemType.REVIVE) > 0);
		assertEquals(0, game.data().itemCount(ItemType.ANTIDOTE));
		assertEquals(ItemType.SMALL_SALVE, noticeFor("+1 Small salve (battle win)").item());
	}

	@Test
	public void theFirstChampionWinPaysTwoLargeSalvesAndARevive()
	{
		Champion lumbridge = new Champion("lumbridge", "Lumbridge", 3201, 3200, 3260, 3250, Arrays.asList(
			new Champion.Member("Chicken", 2)), JarType.IRON, 3);
		game = new GoGame(table, EvolutionTable.empty(), new ChampionTable(Arrays.asList(lumbridge), table),
			new Random(5), () -> now, notices::add);
		Creature dragon = new Creature("king black dragon", 60, now);
		game.data().team.add(dragon);
		game.moveTo(3222, 3218, 0);

		TeamBattleResult first = game.challenge("lumbridge");
		int largeAfterFirst = game.data().itemCount(ItemType.LARGE_SALVE);
		int reviveAfterFirst = game.data().itemCount(ItemType.REVIVE);
		dragon.hpFraction = 1.0;
		TeamBattleResult rematch = game.challenge("lumbridge");

		assertTrue(first.won());
		assertTrue(rematch.won());
		assertEquals(ItemRules.CHAMPION_LARGE_SALVES, largeAfterFirst);
		assertEquals(ItemRules.CHAMPION_REVIVES, reviveAfterFirst);
		assertEquals(largeAfterFirst, game.data().itemCount(ItemType.LARGE_SALVE));
		assertEquals(ItemType.LARGE_SALVE, noticeFor("+2 Large salves (champion)").item());
		assertEquals(ItemType.REVIVE, noticeFor("+1 Revive draught (champion)").item());
	}

	@Test
	public void aSalveHealsAndIsSpent()
	{
		Creature hurt = hurtCreature(0.4);
		game.data().addItems(ItemType.SMALL_SALVE, 2);

		boolean used = game.useItem(hurt, ItemType.SMALL_SALVE);

		assertTrue(used);
		assertEquals(0.65, hurt.hpFractionAt(now), EPSILON);
		assertEquals(1, game.data().itemCount(ItemType.SMALL_SALVE));
		assertEquals(now, hurt.hpStampMs);
		assertEquals("King Black Dragon is at 65% health", notices.get(notices.size() - 1).text());
		assertTrue(game.takeDirty());
	}

	@Test
	public void aLargeSalveCapsAtFullHealth()
	{
		Creature hurt = hurtCreature(0.7);
		game.data().addItems(ItemType.LARGE_SALVE, 1);

		assertTrue(game.useItem(hurt, ItemType.LARGE_SALVE));

		assertEquals(1.0, hurt.hpFractionAt(now), EPSILON);
	}

	@Test
	public void aSalveCountsTheRestAlreadyTaken()
	{
		Creature hurt = hurtCreature(0.0);
		now += Creature.FULL_REST_MS / 2;
		game.data().addItems(ItemType.SMALL_SALVE, 1);

		game.useItem(hurt, ItemType.SMALL_SALVE);

		assertEquals(0.75, hurt.hpFractionAt(now), EPSILON);
	}

	@Test
	public void aSalveAtFullHealthIsRefusedAndNotSpent()
	{
		Creature fit = hurtCreature(1.0);
		game.data().addItems(ItemType.SMALL_SALVE, 1);

		assertFalse(game.useItem(fit, ItemType.SMALL_SALVE));

		assertEquals(1, game.data().itemCount(ItemType.SMALL_SALVE));
		assertEquals("King Black Dragon is already at full health", notices.get(notices.size() - 1).text());
		assertEquals(Tone.BAD, notices.get(notices.size() - 1).tone());
	}

	@Test
	public void aRevivePutsATiredCreatureBackAtHalf()
	{
		Creature tired = hurtCreature(0.1);
		game.data().addItems(ItemType.REVIVE, 1);

		assertTrue(game.useItem(tired, ItemType.REVIVE));

		assertEquals(0.5, tired.hpFractionAt(now), EPSILON);
		assertEquals(0, game.data().itemCount(ItemType.REVIVE));
	}

	@Test
	public void aReviveOnACreatureThatCanStillBattleIsRefused()
	{
		Creature fine = hurtCreature(GoGame.MIN_BATTLE_HP);
		game.data().addItems(ItemType.REVIVE, 1);

		assertFalse(game.useItem(fine, ItemType.REVIVE));

		assertEquals(1, game.data().itemCount(ItemType.REVIVE));
		assertEquals("King Black Dragon does not need reviving", notices.get(notices.size() - 1).text());
	}

	@Test
	public void ownedNoneDoesNothing()
	{
		Creature hurt = hurtCreature(0.3);

		assertFalse(game.useItem(hurt, ItemType.LARGE_SALVE));

		assertEquals(0.3, hurt.hpFractionAt(now), EPSILON);
		assertEquals(0, game.data().itemCount(ItemType.LARGE_SALVE));
		assertEquals("No Large salve left", notices.get(notices.size() - 1).text());
	}

	@Test
	public void theAntidoteHasNothingToCureOutsideBattleAndIsNotSpent()
	{
		Creature hurt = hurtCreature(0.3);
		game.data().addItems(ItemType.ANTIDOTE, 1);

		assertFalse(game.useItem(hurt, ItemType.ANTIDOTE));

		assertEquals(1, game.data().itemCount(ItemType.ANTIDOTE));
		assertEquals("Nothing to cure", notices.get(notices.size() - 1).text());
	}

	@Test
	public void aCreatureThatIsNotTheirsIsLeftAlone()
	{
		Creature stranger = new Creature("king black dragon", 40, now);
		stranger.hpFraction = 0.2;
		game.data().addItems(ItemType.SMALL_SALVE, 1);

		assertFalse(game.useItem(stranger, ItemType.SMALL_SALVE));
		assertFalse(game.useItem(null, ItemType.SMALL_SALVE));

		assertEquals(1, game.data().itemCount(ItemType.SMALL_SALVE));
	}

	@Test
	public void aStoredCreatureCanBeHealedToo()
	{
		Creature stored = new Creature("chicken", 3, now);
		stored.hpFraction = 0.2;
		game.data().storage.add(stored);
		game.data().addItems(ItemType.SMALL_SALVE, 1);

		assertTrue(game.useItem(stored, ItemType.SMALL_SALVE));

		assertEquals(0.45, stored.hpFractionAt(now), EPSILON);
	}

	@Test
	public void aPromisedCreatureCanStillBeHealed()
	{
		Creature promised = hurtCreature(0.3);
		game.tradeLocks(new GoGame.TradeLocks()
		{
			@Override
			public boolean locked(Creature creature)
			{
				return creature == promised;
			}
		});
		game.data().addItems(ItemType.SMALL_SALVE, 1);

		assertTrue(game.useItem(promised, ItemType.SMALL_SALVE));
	}

	@Test
	public void theSnapshotCarriesTheCountsForEveryItem()
	{
		game.data().addItems(ItemType.REVIVE, 3);

		Map<ItemType, Integer> items = game.snapshot().items();

		assertEquals(ItemType.values().length, items.size());
		assertEquals(3, (int) items.get(ItemType.REVIVE));
		assertEquals(0, (int) items.get(ItemType.ANTIDOTE));
	}

	private GoGame newGame(Random random)
	{
		return new GoGame(table, random, () -> now, notices::add);
	}

	private Creature hurtCreature(double health)
	{
		Creature creature = new Creature("king black dragon", 40, now);
		creature.hpFraction = health;
		game.data().team.add(creature);
		return creature;
	}

	private Notice noticeFor(String text)
	{
		for (Notice notice : notices)
		{
			if (notice.text().equals(text))
				return notice;
		}

		throw new AssertionError("no notice said: " + text);
	}
}
