/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.junit.Test;

import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Checks that the end-of-animation callbacks cannot make the game's animation tick loop spin forever. That loop runs
 * while the elapsed time exceeds the length of the current frame, and setting a frame does not clear the elapsed
 * time, so a callback that only sets the frame hangs on a last frame of length 0.
 */
public class AnimationHoldTest
{
	private static final int TIMEOUT_MS = 5_000;

	private static Animation animation(int... frameLengths)
	{
		Animation animation = mock(Animation.class);
		when(animation.isMayaAnim()).thenReturn(false);
		when(animation.getFrameLengths()).thenReturn(frameLengths);
		return animation;
	}

	@Test(timeout = TIMEOUT_MS)
	public void holdingALastFrameOfLengthZeroDoesNotSpin()
	{
		AnimationController controller = new AnimationController(mock(Client.class), animation(2, 0));
		controller.setOnFinished(CreatureBody::hold);

		controller.tick(5);

		assertEquals(1, controller.getFrame());
	}

	@Test(timeout = TIMEOUT_MS)
	public void holdingKeepsTheLastFrameOnLaterTicks()
	{
		AnimationController controller = new AnimationController(mock(Client.class), animation(2, 3, 0));
		controller.setOnFinished(CreatureBody::hold);

		controller.tick(20);
		controller.tick(20);

		assertEquals(2, controller.getFrame());
	}

	@Test(timeout = TIMEOUT_MS)
	public void theGhostRestartsAnAnimationWhoseLastFrameHasLengthZero()
	{
		AnimationController controller = new AnimationController(mock(Client.class), animation(2, 0));
		controller.setOnFinished(NpcGhost::restart);

		controller.tick(5);

		assertEquals(0, controller.getFrame());
	}

	@Test(timeout = TIMEOUT_MS)
	public void aLoopingBodyRestartsAnAnimationWhoseLastFrameHasLengthZero()
	{
		AnimationController controller = new AnimationController(mock(Client.class), animation(2, 0));
		controller.setOnFinished(CreatureBody::restart);

		controller.tick(5);

		assertEquals(0, controller.getFrame());
	}
}
