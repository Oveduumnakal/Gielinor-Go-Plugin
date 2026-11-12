/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import net.runelite.api.coords.LocalPoint;

/**
 * Tells the floating text where the bodies of a battle that is acted out on screen stand. Every stage implements
 * it for its own stand-ins, so text can float over them instead of over the hidden real monsters.
 */
interface StageAnchors
{
	/** Where one body stands: the ground under it, the plane, and how tall it is drawn. */
	final class Spot
	{
		private final LocalPoint point;
		private final int plane;
		private final int height;

		/**
		 * Describes a place.
		 *
		 * @param point the ground under the body, in the scene
		 * @param plane the plane it stands on
		 * @param height how tall the body is drawn, in local units
		 */
		Spot(LocalPoint point, int plane, int height)
		{
			this.point = point;
			this.plane = plane;
			this.height = height;
		}

		/** @return the ground under the body */
		LocalPoint point()
		{
			return point;
		}

		/** @return the plane it stands on */
		int plane()
		{
			return plane;
		}

		/** @return how tall the body is drawn, in local units */
		int height()
		{
			return height;
		}
	}

	/**
	 * Finds where an anchor is drawn right now. May be called from the render thread.
	 *
	 * @param anchor which body; {@link Notice.Anchor#HEAD} never has a spot
	 * @return its place, or null when that anchor is not on stage
	 */
	Spot spotOf(Notice.Anchor anchor);
}
