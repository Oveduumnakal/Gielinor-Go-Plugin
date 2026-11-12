/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Lets {@link BattleDirector} drive a battle against a City Champion that is acted out by {@link ArenaStage}. The
 * arena brings a creature in on its own when a turn says so, so showing the lead needs nothing here.
 */
final class ArenaScene implements BattleDirector.Scene
{
	private final ArenaStage arena;

	/**
	 * Wraps the arena.
	 *
	 * @param arena the arena, with a battle already started open
	 */
	ArenaScene(ArenaStage arena)
	{
		this.arena = arena;
	}

	@Override
	public boolean active()
	{
		return arena.active();
	}

	@Override
	public boolean waiting()
	{
		return arena.waiting();
	}

	@Override
	public void append(TurnResult turn)
	{
		arena.append(turn);
	}

	@Override
	public void finishWith(boolean won)
	{
		arena.finishWith(won);
	}

	@Override
	public void showLead(Combatant entered)
	{
	}

	@Override
	public void runAway()
	{
		arena.clear();
	}

	@Override
	public void end()
	{
		arena.clear();
	}
}
