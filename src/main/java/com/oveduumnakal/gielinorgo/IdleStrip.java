/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A creature's standing motion as a short strip of pictures, each shown for its own time, played round and round by
 * the clock. All pictures share one framing. Holds no RuneLite types.
 */
final class IdleStrip
{
	/** The most pictures in a strip; a longer motion is sampled evenly. */
	static final int MOST_FRAMES = 24;

	/** Where strips come from. */
	interface Source
	{
		/**
		 * @param species the species name
		 * @param npcId the NPC id the creature looks like, or 0
		 * @return its idle loop, or null while there is none, in which case it is asked for
		 */
		IdleStrip strip(String species, int npcId);
	}

	private final List<BufferedImage> pictures;
	private final long[] ends;
	private final Rectangle shape;

	/**
	 * Makes a strip.
	 *
	 * @param pictures the pictures, at least one
	 * @param millis how long each is shown
	 */
	IdleStrip(List<BufferedImage> pictures, long[] millis)
	{
		this.pictures = Collections.unmodifiableList(new ArrayList<>(pictures));
		this.ends = new long[pictures.size()];
		long total = 0;
		Rectangle all = null;
		for (int i = 0; i < ends.length; i++)
		{
			total += Math.max(1, millis[i]);
			ends[i] = total;
			Rectangle seen = SummaryScene.trimmed(pictures.get(i));
			all = all == null ? new Rectangle(seen) : all.union(seen);
		}

		this.shape = all;
	}

	/**
	 * @param nowMs the time in milliseconds
	 * @return the picture of this moment
	 */
	BufferedImage at(long nowMs)
	{
		long into = Math.floorMod(nowMs, ends[ends.length - 1]);
		for (int i = 0; i < ends.length; i++)
		{
			if (into < ends[i])
				return pictures.get(i);
		}

		return pictures.get(ends.length - 1);
	}

	/** @return the part of the square the creature covers in any of the pictures */
	Rectangle shape()
	{
		return new Rectangle(shape);
	}

	/** @return how many pictures there are */
	int size()
	{
		return pictures.size();
	}

	/**
	 * Picks which frames of an animation a strip takes.
	 *
	 * @param count how many frames the animation has
	 * @return the frame numbers in order: all of them, or {@link #MOST_FRAMES} spread evenly
	 */
	static int[] sample(int count)
	{
		int taken = Math.max(0, Math.min(count, MOST_FRAMES));
		int[] frames = new int[taken];
		for (int i = 0; i < taken; i++)
			frames[i] = i * count / taken;

		return frames;
	}

	/**
	 * Gives how long each sampled frame is shown: its own time and that of the frames skipped after it.
	 *
	 * @param sampled the frame numbers, from {@link #sample(int)}
	 * @param lengths how long each frame of the animation lasts, in client ticks of 20 milliseconds
	 * @return milliseconds for each sampled frame
	 */
	static long[] millis(int[] sampled, int[] lengths)
	{
		long[] millis = new long[sampled.length];
		for (int i = 0; i < sampled.length; i++)
		{
			int until = i + 1 < sampled.length ? sampled[i + 1] : lengths.length;
			for (int frame = sampled[i]; frame < until; frame++)
				millis[i] += 20L * Math.max(0, lengths[frame]);
		}

		return millis;
	}
}
