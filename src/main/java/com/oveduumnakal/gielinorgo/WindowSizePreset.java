/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * The "Window size" presets, each a fraction of the canvas width. They are a way to size the window without the
 * drag key, which a desktop (KDE on a Steam Deck) may take for itself. Holds no RuneLite types.
 */
public enum WindowSizePreset
{
	/** About half the canvas width. */
	SMALL("Small", 0.50),

	/** The default, 70% of the canvas width. */
	MEDIUM("Medium", WindowFit.DEFAULT_FRACTION),

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
