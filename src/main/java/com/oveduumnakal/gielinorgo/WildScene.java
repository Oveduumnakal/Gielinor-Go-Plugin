/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** Lets {@link BattleDirector} drive a battle against a wild creature that is acted out by {@link BattleStage}. */
final class WildScene implements BattleDirector.Scene
{
	private final BattleStage stage;

	/**
	 * Wraps the stage.
	 *
	 * @param stage the stage, with a battle already started open
	 */
	WildScene(BattleStage stage)
	{
		this.stage = stage;
	}

	@Override
	public boolean active()
	{
		return stage.active();
	}

	@Override
	public boolean waiting()
	{
		return stage.waiting();
	}

	@Override
	public void append(TurnResult turn)
	{
		stage.append(turn);
	}

	@Override
	public void finishWith(boolean won)
	{
		stage.finishWith(won);
	}

	@Override
	public void showLead(Combatant entered)
	{
		stage.showLead(entered);
	}

	@Override
	public void runAway()
	{
		stage.runAway();
	}

	@Override
	public void end()
	{
		stage.clear();
	}
}
