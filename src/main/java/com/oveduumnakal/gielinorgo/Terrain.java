/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** Facts about places in the game world that the rules need. Pure maths on world coordinates. */
final class Terrain
{
	/**
	 * World y from which a tile is treated as underground. The surface map ends below this line, and the game
	 * keeps most caves and dungeons above it.
	 */
	static final int UNDERGROUND_MIN_Y = 4160;

	private static final int PRIFDDINAS_MIN_X = 3136;
	private static final int PRIFDDINAS_MAX_X = 3391;
	private static final int PRIFDDINAS_MIN_Y = 5952;
	private static final int PRIFDDINAS_MAX_Y = 6207;

	private Terrain()
	{
	}

	/**
	 * Guesses whether a tile is underground. The game does not say so directly, so this goes by where the tile
	 * sits on the world map. Prifddinas is the one large surface area stored among the dungeons, and is excluded.
	 *
	 * @param x world x
	 * @param y world y
	 * @return true when the tile is most likely in a cave or dungeon
	 */
	static boolean underground(int x, int y)
	{
		if (y < UNDERGROUND_MIN_Y)
			return false;

		boolean prifddinas = x >= PRIFDDINAS_MIN_X && x <= PRIFDDINAS_MAX_X
			&& y >= PRIFDDINAS_MIN_Y && y <= PRIFDDINAS_MAX_Y;
		return !prifddinas;
	}
}
