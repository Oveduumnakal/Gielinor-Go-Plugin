/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * The plain arithmetic behind the see-through copy of a monster that is hidden for a battle: how see-through each
 * face becomes, and which of the monster's two animations the copy should show. Kept apart from the game objects
 * so it can be tested without a client.
 */
final class GhostMirror
{
	/** How much of the monster can still be seen, from 0 (nothing) to 1 (all of it): 10%, so 90% see-through. */
	static final double OPACITY = 0.10;

	/** The value the game uses for "no animation". */
	static final int NO_ANIMATION = -1;

	/** The most see-through value a face can have; 255 cannot be seen at all. */
	private static final int MAX_TRANSPARENCY = 255;

	/** The animation and frame to show. */
	static final class Pick
	{
		private final int animation;
		private final int frame;

		private Pick(int animation, int frame)
		{
			this.animation = animation;
			this.frame = frame;
		}

		/** @return the animation id, or {@link GhostMirror#NO_ANIMATION} to stand still */
		int animation()
		{
			return animation;
		}

		/** @return the frame the monster is on in that animation */
		int frame()
		{
			return frame;
		}
	}

	private GhostMirror()
	{
	}

	/**
	 * Makes one face see-through. The result is never less see-through than the face was.
	 *
	 * @param transparency the face's value, 0 for solid up to 255 for unseen
	 * @param opacity the share of the face that stays visible, from 0 to 1
	 * @return the new value
	 */
	static int fade(int transparency, double opacity)
	{
		int t = Math.max(0, Math.min(MAX_TRANSPARENCY, transparency));
		return MAX_TRANSPARENCY - (int) Math.round((MAX_TRANSPARENCY - t) * opacity);
	}

	/**
	 * Makes every face {@link #OPACITY} visible, in place. A missing array means every face was solid, so there is
	 * nothing to change here; the caller makes the array first.
	 *
	 * @param transparencies one value per face, or null
	 */
	static void fadeAll(byte[] transparencies)
	{
		fadeAll(transparencies, OPACITY);
	}

	/**
	 * Makes every face the given share visible, in place. A missing array means every face was solid, so there is
	 * nothing to change here; the caller makes the array first.
	 *
	 * @param transparencies one value per face, or null
	 * @param opacity the share of each face that stays visible, from 0 to 1
	 */
	static void fadeAll(byte[] transparencies, double opacity)
	{
		if (transparencies == null)
			return;

		for (int i = 0; i < transparencies.length; i++)
			transparencies[i] = (byte) fade(transparencies[i] & 0xFF, opacity);
	}

	/**
	 * Chooses what the copy shows: the monster's action animation when it has one, else its pose (standing or
	 * walking).
	 *
	 * @param action the action animation id, or -1
	 * @param actionFrame the frame of the action animation
	 * @param pose the pose animation id, or -1
	 * @param poseFrame the frame of the pose animation
	 * @return the animation and frame to mirror
	 */
	static Pick pick(int action, int actionFrame, int pose, int poseFrame)
	{
		if (action != NO_ANIMATION)
			return new Pick(action, actionFrame);

		if (pose != NO_ANIMATION)
			return new Pick(pose, poseFrame);

		return new Pick(NO_ANIMATION, 0);
	}

	/**
	 * Whether the copy's animation has to be changed. Only a different id changes it, so an animation that plays
	 * is never restarted; one that has run out on the copy (no animation left) is started again.
	 *
	 * @param shown the id the copy shows now, or -1
	 * @param running whether the copy's controller still holds an animation
	 * @param wanted the id the monster shows
	 * @return true when the animation must be set
	 */
	static boolean animationChanged(int shown, boolean running, int wanted)
	{
		return shown != wanted || (wanted != NO_ANIMATION && !running);
	}

	/**
	 * Whether the copy's frame has fallen out of step with the monster's.
	 *
	 * @param shown the frame the copy is on
	 * @param wanted the frame the monster is on
	 * @return true when the frame must be set
	 */
	static boolean drifted(int shown, int wanted)
	{
		return shown != wanted;
	}
}
