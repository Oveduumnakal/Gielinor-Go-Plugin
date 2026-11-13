/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;

/** Moves for the fixtures of the battle tests, so a test dragon bites and breathes like the real pool says. */
final class TestMoves
{
	/** A plain bite. */
	static final Move BITE = new Move("bite", "Bite", CombatType.STAB, 1.0, 1.0);

	/** A fire breath, which plays the second animation. */
	static final Move DRAGONFIRE = new Move("dragonfire", "Dragonfire", CombatType.MAGIC, 1.6, 0.9)
		.inSlot(Move.SECOND, false);

	/** An icy breath. */
	static final Move ICY_BREATH = new Move("icy_breath", "Icy breath", CombatType.MAGIC, 1.6, 0.9)
		.inSlot(Move.SECOND, true);

	private TestMoves()
	{
	}

	/** @return a dragon's moves: a bite and a fire breath */
	static List<Move> dragon()
	{
		return Arrays.asList(BITE, DRAGONFIRE);
	}

	/** @return a wyvern's moves: a bite and an icy breath */
	static List<Move> wyvern()
	{
		return Arrays.asList(BITE, ICY_BREATH);
	}

	/** @return a creature whose only move is a magic attack of power 1, which never misses */
	static List<Move> alwaysMagic()
	{
		return Collections.singletonList(Move.basic(CombatType.MAGIC));
	}

	/**
	 * Makes a plain melee move of power 1 and accuracy 1 that can cause a status.
	 *
	 * @param id the move id
	 * @param status the status it can cause
	 * @param chance the chance, from 0 to 1, that a hit causes it
	 * @return the move
	 */
	static Move causing(String id, Status status, double chance)
	{
		String json = "{\"id\":\"" + id + "\",\"name\":\"" + id + "\",\"style\":\"STAB\",\"power\":1.0,"
			+ "\"accuracy\":1.0,\"effect\":{\"status\":\"" + status.name() + "\",\"chance\":" + chance + "}}";
		return new Gson().fromJson(json, Move.class);
	}
}
