/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;

/**
 * The one place the gender sign's colours live: blue for male and pink for female. Each has a bright shade that reads
 * on the dark panels and rows, and a deeper shade for the light lead row, so the sign stays clear on both. Shape
 * still tells the two signs apart, so the colours never carry the meaning alone. Holds no RuneLite types.
 */
final class GenderColors
{
	/** The blue of the male sign on a dark background. */
	static final Color MALE = new Color(0x4DA3FF);

	/** The pink of the female sign on a dark background. */
	static final Color FEMALE = new Color(0xFF7EB6);

	/** The blue of the male sign on a light background. */
	static final Color MALE_ON_LIGHT = new Color(0x1F66C2);

	/** The pink of the female sign on a light background. */
	static final Color FEMALE_ON_LIGHT = new Color(0xC2306F);

	private GenderColors()
	{
	}

	/**
	 * Gives the colour of a gender's sign.
	 *
	 * @param gender the gender; anything but female counts as male, as {@link Icons#gender} draws it
	 * @param onLight whether the sign sits on a light background, such as the lead row
	 * @return the colour
	 */
	static Color of(Gender gender, boolean onLight)
	{
		if (gender == Gender.FEMALE)
			return onLight ? FEMALE_ON_LIGHT : FEMALE;

		return onLight ? MALE_ON_LIGHT : MALE;
	}
}
