/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** The gender of a creature. Saved by its constant name, so a constant is never renamed. */
enum Gender
{
	/** A male creature. */
	MALE,

	/** A female creature. */
	FEMALE;

	/**
	 * Reads a gender from the name it is saved or sent under.
	 *
	 * @param name a {@link #name()}, or null
	 * @return the gender, or null when the name is null or unknown
	 */
	static Gender named(String name)
	{
		if (name == null)
			return null;

		for (Gender gender : values())
		{
			if (gender.name().equals(name))
				return gender;
		}

		return null;
	}
}
