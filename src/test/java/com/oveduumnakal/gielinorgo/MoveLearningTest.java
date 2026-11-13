/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Plays the learning of moves through the game: at a level, a full set, a choice, a jump and an evolution. */
public class MoveLearningTest
{
	private static final String MOVES = "{\"moves\":["
		+ "{\"id\":\"a\",\"name\":\"Aa\",\"style\":\"STAB\",\"power\":1.0,\"accuracy\":1.0},"
		+ "{\"id\":\"b\",\"name\":\"Bb\",\"style\":\"STAB\",\"power\":1.2,\"accuracy\":0.9},"
		+ "{\"id\":\"c\",\"name\":\"Cc\",\"style\":\"STAB\",\"power\":1.4,\"accuracy\":0.9},"
		+ "{\"id\":\"d\",\"name\":\"Dd\",\"style\":\"STAB\",\"power\":1.6,\"accuracy\":0.8},"
		+ "{\"id\":\"e\",\"name\":\"Ee\",\"style\":\"STAB\",\"power\":1.8,\"accuracy\":0.8},"
		+ "{\"id\":\"f\",\"name\":\"Ff\",\"style\":\"STAB\",\"power\":2.0,\"accuracy\":0.7},"
		+ "{\"id\":\"g\",\"name\":\"Gg\",\"style\":\"MAGIC\",\"power\":1.0,\"accuracy\":1.0}],"
		+ "\"defaults\":{\"STAB\":\"long\",\"MAGIC\":\"magic\"},"
		+ "\"pools\":{\"long\":{\"styles\":[\"STAB\"],"
		+ "\"learns\":[[\"a\",1],[\"b\",5],[\"c\",10],[\"d\",20],[\"e\",30],[\"f\",40]]},"
		+ "\"magic\":{\"styles\":[\"MAGIC\"],\"learns\":[[\"g\",1]]},"
		+ "\"short\":{\"styles\":[\"STAB\"],\"learns\":[[\"a\",1],[\"b\",5]]}},"
		+ "\"species\":{\"pup\":\"long\",\"cub\":\"long\",\"lad\":\"long\",\"odd\":\"short\"}}";

	private static final Species CUB = new Species("Cub", 5, CombatType.STAB);
	private static final Species PUP = new Species("Pup", 5, CombatType.STAB);
	private static final Species LAD = new Species("Lad", 20, CombatType.STAB);
	private static final Species ODD = new Species("Odd", 20, CombatType.STAB);

	private final List<String> messages = new ArrayList<>();
	private GoGame game;

	@Before
	public void setUp()
	{
		SpeciesTable species = new SpeciesTable(Arrays.asList(PUP, CUB, LAD, ODD));
		EvolutionTable evolutions = EvolutionTable.load(new Gson(),
			new StringReader("[{\"from\":\"Cub\",\"to\":\"Lad\",\"level\":20},"
				+ "{\"from\":\"Lad\",\"to\":\"Odd\",\"level\":60}]"), species);
		game = new GoGame(species, evolutions, ChampionTable.empty(), new Random(3), () -> 1_000_000L,
			notice -> messages.add(notice.text()));
		game.moves(MoveTable.load(new Gson(), new StringReader(MOVES)));
	}

	private Creature own(String species, int level, String... moves)
	{
		Creature creature = new Creature(species, level, 1L);
		creature.moves = new ArrayList<>(Arrays.asList(moves));
		SaveData save = SaveData.fresh();
		save.team.add(creature);
		game.load(save);
		return game.data().team.get(0);
	}

	private void levelTo(Creature creature, int level)
	{
		long missing = XpRules.xpForLevel(level) - creature.xp;
		game.addOsrsXp((int) (missing * XpRules.OSRS_XP_PER_CREATURE_XP));
		assertEquals(level, creature.level());
	}

	@Test
	public void aMoveIsLearnedAtItsLevelAndTheCreatureIsTold()
	{
		Creature pup = own("pup", 1, "a");

		levelTo(pup, 5);

		assertEquals(Arrays.asList("a", "b"), pup.moves);
		assertTrue(messages.toString(), messages.contains("Pup learned Bb!"));
		assertTrue(game.takeDirty());
	}

	@Test
	public void aFifthMoveWaitsAndReplacingOneLearnsIt()
	{
		Creature pup = own("pup", 20, "a", "b", "c", "d");

		levelTo(pup, 30);

		assertEquals(Arrays.asList("a", "b", "c", "d"), pup.moves);
		assertEquals(Arrays.asList("e"), pup.pendingMoves);
		assertTrue(messages.toString(), messages.contains("Pup wants to learn Ee"));

		assertTrue(game.resolveMove(pup, 1));

		assertEquals(Arrays.asList("a", "e", "c", "d"), pup.moves);
		assertTrue(pup.pendingMoves.isEmpty());
		assertTrue(messages.toString(), messages.contains("Pup forgot Bb and learned Ee!"));
	}

	@Test
	public void skippingAPendingMoveDropsItAndKeepsTheFour()
	{
		Creature pup = own("pup", 20, "a", "b", "c", "d");
		levelTo(pup, 30);

		assertTrue(game.resolveMove(pup, -1));

		assertEquals(Arrays.asList("a", "b", "c", "d"), pup.moves);
		assertTrue(pup.pendingMoves.isEmpty());
		assertTrue(messages.toString(), messages.contains("Pup did not learn Ee"));
		assertFalse("nothing is left to settle", game.resolveMove(pup, -1));
	}

	@Test
	public void aChoiceThatMakesNoSenseChangesNothing()
	{
		Creature pup = own("pup", 20, "a", "b", "c", "d");
		levelTo(pup, 30);

		assertFalse(game.resolveMove(pup, 4));
		assertFalse(game.resolveMove(pup, -2));
		assertFalse(game.resolveMove(new Creature("pup", 1, 1L), 0));
		assertEquals(Arrays.asList("e"), pup.pendingMoves);
	}

	@Test
	public void aJumpOverSeveralLevelsLearnsWhatFitsAndQueuesTheRest()
	{
		Creature pup = own("pup", 1, "a");

		levelTo(pup, 99);

		assertEquals(Arrays.asList("a", "b", "c", "d"), pup.moves);
		assertEquals(Arrays.asList("e", "f"), pup.pendingMoves);
	}

	@Test
	public void theOldestWaitingMoveIsSettledFirst()
	{
		Creature pup = own("pup", 1, "a");
		levelTo(pup, 99);

		assertTrue(game.resolveMove(pup, 0));
		assertEquals(Arrays.asList("e", "b", "c", "d"), pup.moves);
		assertEquals(Arrays.asList("f"), pup.pendingMoves);
	}

	@Test
	public void anEvolutionKeepsTheMovesTheNewPoolStillTeaches()
	{
		Creature cub = own("cub", 19, "a", "b", "c");

		levelTo(cub, 20);

		assertEquals("lad", cub.species);
		assertEquals("a, b and c stay, d is learned at 20", Arrays.asList("a", "b", "c", "d"), cub.moves);
	}

	@Test
	public void anEvolutionDropsMovesTheNewPoolDoesNotTeach()
	{
		Creature lad = own("lad", 59, "b", "c", "d", "e");

		levelTo(lad, 60);

		assertEquals("odd", lad.species);
		assertEquals("only a and b are in the new pool", Arrays.asList("b", "a"), lad.moves);
	}

	@Test
	public void anOldCreatureGetsTheNewestFourMovesItsLevelHasUnlockedOnLoad()
	{
		Creature old = new Creature("cub", 30, 1L);
		SaveData save = SaveData.fresh();
		save.team.add(old);
		save.storage.add(new Creature("cub", 5, 1L));

		game.load(save);
		Creature loadedOld = game.data().team.get(0);
		Creature loadedYoung = game.data().storage.get(0);

		assertEquals(Arrays.asList("b", "c", "d", "e"), loadedOld.moves);
		assertEquals(Arrays.asList("a", "b"), loadedYoung.moves);
		assertTrue(loadedOld.pendingMoves.isEmpty());
	}

	@Test
	public void loadingDropsMovesTheSpeciesCannotKnow()
	{
		Creature creature = new Creature("cub", 12, 1L);
		creature.moves = new ArrayList<>(Arrays.asList("a", "nonsense", "f", "g", "a"));
		SaveData save = SaveData.fresh();
		save.team.add(creature);

		game.load(save);
		Creature loaded = game.data().team.get(0);

		assertEquals("f is too late, g is another pool, nonsense is unknown, a is not repeated",
			Arrays.asList("a", "b", "c"), loaded.moves);
	}

	@Test
	public void aCatchAndAHatchStartWithTheMovesTheirLevelHas()
	{
		Creature caught = new Creature("cub", 25, 1L);

		MoveLearning.start(caught, CUB, game.moveTable());

		assertEquals(Arrays.asList("a", "b", "c", "d"), caught.moves);
		assertTrue(caught.pendingMoves.isEmpty());
	}

	@Test
	public void withoutAMoveTableNothingIsLearnedOrChanged()
	{
		GoGame bare = new GoGame(new SpeciesTable(Arrays.asList(CUB)), new Random(3), () -> 1L, notice ->
		{
		});
		Creature cub = new Creature("cub", 1, 1L);
		SaveData save = SaveData.fresh();
		save.team.add(cub);
		bare.load(save);

		bare.addOsrsXp(1_000_000);
		Creature after = bare.data().team.get(0);

		assertTrue(after.level() > 1);
		assertNull(after.moves);
	}

	@Test
	public void aBattleUsesTheCreaturesMovesAndAWildCreatureGetsItsLevelsMoves()
	{
		Creature pup = own("pup", 12, "a", "b", "c");
		pup.hpFraction = 1.0;

		BattleResult result = game.battle(new WildEntry(1, PUP, 12, 12));

		boolean mineNamed = false;
		boolean foeNamed = false;
		for (BattleLine line : result.lines())
		{
			mineNamed |= line.kind() == BattleLine.Kind.LEAD_HIT && line.text().contains(" uses ");
			foeNamed |= line.kind() == BattleLine.Kind.FOE_HIT && line.text().contains(" uses ");
		}

		assertTrue("the creature's own moves are named in its lines", mineNamed);
		assertTrue("the wild creature fights with the moves its level unlocked", foeNamed);
	}
}
