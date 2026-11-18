/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks what the creature view carries of moves: the known ones and the ones that wait to be learned. */
public class CreatureViewMovesTest
{
	private static final Move CLAW = new Move("claw", "Claw", CombatType.STAB, 1.25, 0.95);

	private static CreatureView view(List<Move> moves, List<Move> pending)
	{
		return new CreatureView("Green dragon", 30, 0.5, 1.0, Rarity.RARE, CombatType.STAB, false, "Smaug",
			StatRolls.AVERAGE, 0, moves, pending);
	}

	private static CreatureView waiting()
	{
		return view(Arrays.asList(TestMoves.BITE, CLAW), Collections.singletonList(TestMoves.DRAGONFIRE));
	}

	@Test
	public void aViewCarriesItsMovesAndTheOnesThatWait()
	{
		CreatureView plain = view(Arrays.asList(TestMoves.BITE, CLAW), Collections.emptyList());
		CreatureView waiting = waiting();

		assertEquals(2, plain.moves().size());
		assertTrue(plain.pending().isEmpty());
		assertEquals(1, waiting.pending().size());
		assertFalse("a move that waits changes the view, so the card redraws", plain.equals(waiting));
		assertEquals(plain, view(Arrays.asList(TestMoves.BITE, CLAW), Collections.emptyList()));
	}

	@Test
	public void theGameShowsMovesAndPendingMovesInTheTeamView()
	{
		GoGame game = new GoGame(new SpeciesTable(Arrays.asList(new Species("Cub", 5, CombatType.STAB))),
			new Random(1), () -> 1L, notice ->
			{
			});
		game.moves(MoveTable.load(new Gson(), new StringReader(
			"{\"moves\":[{\"id\":\"a\",\"name\":\"Aa\",\"style\":\"STAB\",\"power\":1.0,\"accuracy\":1.0},"
			+ "{\"id\":\"b\",\"name\":\"Bb\",\"style\":\"STAB\",\"power\":1.2,\"accuracy\":0.9}],"
			+ "\"defaults\":{\"STAB\":\"p\"},\"pools\":{\"p\":{\"styles\":[\"STAB\"],"
			+ "\"learns\":[[\"a\",1],[\"b\",5]]}},\"species\":{}}")));
		Creature cub = new Creature("cub", 6, 1L);
		cub.moves = new ArrayList<>(Collections.singletonList("a"));
		cub.pendingMoves = new ArrayList<>(Collections.singletonList("b"));
		SaveData save = SaveData.fresh();
		save.team.add(cub);
		game.load(save);

		List<CreatureView> team = game.snapshot().team();
		CreatureView shown = team.get(0);

		assertEquals(Collections.singletonList("a"), moveIds(shown.moves()));
		assertEquals(Collections.singletonList("b"), moveIds(shown.pending()));
	}

	private static List<String> moveIds(List<Move> moves)
	{
		List<String> ids = new ArrayList<>();
		for (Move move : moves)
			ids.add(move.id());

		return ids;
	}
}
