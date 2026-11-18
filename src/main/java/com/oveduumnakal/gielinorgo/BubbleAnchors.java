/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Where a wild creature is on the canvas this tick: the middle of its model and the line its name tag sits on. The
 * bubbles grow from the origin and rest above the head. Immutable. Holds no RuneLite types.
 */
final class BubbleAnchors
{
	private final double originX;
	private final double originY;
	private final double headX;
	private final double headY;

	/**
	 * @param originX canvas x of the middle of the model
	 * @param originY canvas y of the middle of the model
	 * @param headX canvas x of the tag line
	 * @param headY canvas y of the tag line
	 */
	BubbleAnchors(double originX, double originY, double headX, double headY)
	{
		this.originX = originX;
		this.originY = originY;
		this.headX = headX;
		this.headY = headY;
	}

	/** @return canvas x of the middle of the model, where the bubbles start */
	double originX()
	{
		return originX;
	}

	/** @return canvas y of the middle of the model, where the bubbles start */
	double originY()
	{
		return originY;
	}

	/** @return canvas x of the tag line, the middle of the resting bubbles */
	double headX()
	{
		return headX;
	}

	/** @return canvas y of the tag line, which the resting bubbles sit above */
	double headY()
	{
		return headY;
	}
}
