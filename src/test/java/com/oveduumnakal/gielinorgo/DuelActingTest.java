/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Checks when a duel is settled, so the next beat of a battle may start. */
public class DuelActingTest
{
	/** A side whose melee turn is on or off. */
	private static final class FakeSide implements DuelActing.Side
	{
		private boolean turning;

		@Override
		public void act(CreatureBody.Act act)
		{
		}

		@Override
		public void meleeTurn(double distance, CreatureBody.Act act)
		{
		}

		@Override
		public boolean turning()
		{
			return turning;
		}

		@Override
		public double x()
		{
			return 0;
		}

		@Override
		public double y()
		{
			return 0;
		}

		@Override
		public double height()
		{
			return 0;
		}
	}

	@Test
	public void aDuelIsSettledOnlyWhenNeitherSideIsInAMeleeTurn()
	{
		FakeSide lead = new FakeSide();
		FakeSide foe = new FakeSide();
		DuelActing acting = new DuelActing(null, null, lead, foe);

		assertTrue(acting.settled());

		lead.turning = true;
		assertFalse(acting.settled());

		lead.turning = false;
		foe.turning = true;
		assertFalse(acting.settled());

		foe.turning = false;
		assertTrue(acting.settled());
	}
}
