/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * The "Window size" presets of the settings, each a fraction of the canvas width. The player can also drag the
 * window's edge or its corner grip, or hold RuneLite's drag key and drag the edge. Holds no RuneLite types.
 */
public enum WindowSizePreset
{
	/** The default, half of the canvas width. */
	SMALL("Small", WindowFit.DEFAULT_FRACTION),

	/** About 70% of the canvas width. */
	MEDIUM("Medium", 0.70),

	/** About 85% of the canvas width. */
	LARGE("Large", 0.85),

	/** As large as the canvas allows, in either direction. */
	FIT_CANVAS("Fit canvas", 1.0);

	private final String label;
	private final double fraction;

	WindowSizePreset(String label, double fraction)
	{
		this.label = label;
		this.fraction = fraction;
	}

	/** @return the share of the canvas width this preset asks for, before the canvas height limits it */
	public double fraction()
	{
		return fraction;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
