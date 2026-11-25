/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The health the pinned party box shows while a battle is still being shown. */
public class LiveHealthTest
{
	private final Object battle = new Object();
	private final List<CreatureView> team = Arrays.asList(view("Imp"), view("Cow"));

	private static CreatureView view(String name)
	{
		return new CreatureView(name, 5, 0.0, 1.0, Rarity.COMMON, CombatType.CRUSH, false, null);
	}

	private double healthAt(LiveHealth health, int place)
	{
		CreatureView shown = health.over(team).get(place);
		return shown.hpFraction();
	}

	private boolean faintedAt(LiveHealth health, int place)
	{
		CreatureView shown = health.over(team).get(place);
		return shown.fainted();
	}

	@Test
	public void aShownLineChangesOnlyItsCreature()
	{
		LiveHealth health = new LiveHealth();
		health.follow(battle);

		assertTrue(health.show(new LiveHealth.Step(battle, 0, 0.4)));

		assertEquals(0.4, healthAt(health, 0), 1e-9);
		assertEquals(1.0, healthAt(health, 1), 1e-9);
		assertEquals(1.0, team.get(0).hpFraction(), 1e-9);
	}

	@Test
	public void noHealthLeftShowsTheCreatureFainted()
	{
		LiveHealth health = new LiveHealth();
		health.follow(battle);
		health.show(new LiveHealth.Step(battle, 1, 0.0));

		assertTrue(faintedAt(health, 1));
		assertFalse(faintedAt(health, 0));
	}

	@Test
	public void aLineOfAnotherBattleOrWithNoHealthIsIgnored()
	{
		LiveHealth health = new LiveHealth();
		health.follow(battle);

		assertFalse(health.show(null));
		assertFalse(health.show(new LiveHealth.Step(new Object(), 0, 0.1)));
		assertEquals(1.0, healthAt(health, 0), 1e-9);
	}

	@Test
	public void followingNothingGoesBackToTheSavedHealth()
	{
		LiveHealth health = new LiveHealth();
		health.follow(battle);
		health.show(new LiveHealth.Step(battle, 0, 0.4));
		health.follow(null);

		assertEquals(1.0, healthAt(health, 0), 1e-9);
	}

	@Test
	public void anUnseenNoticeKeepsItsTurnButIsNotDrawn()
	{
		NoticeBoard board = new NoticeBoard();
		board.queue(Arrays.asList(Notice.player("-3", Tone.BAD).unseen()));

		assertTrue(board.tick(0L).silent());
		assertTrue(board.visible(0L).isEmpty());
		assertTrue(board.idle());
	}
}
