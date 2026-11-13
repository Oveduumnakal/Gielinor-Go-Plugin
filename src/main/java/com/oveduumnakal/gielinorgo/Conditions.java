/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** What the world is like for the player right now, as far as evolutions care. Immutable and free of clock access. */
final class Conditions
{
	/** Daytime, on the surface: the state before the client has reported anything. */
	static final Conditions NONE = new Conditions(false, false);

	private final boolean night;
	private final boolean underground;

	private Conditions(boolean night, boolean underground)
	{
		this.night = night;
		this.underground = underground;
	}

	/**
	 * Describes the world.
	 *
	 * @param night whether it is night on the computer's clock ({@link SpawnCalendar#isNight})
	 * @param underground whether the player is underground ({@link Terrain#underground})
	 * @return the conditions
	 */
	static Conditions of(boolean night, boolean underground)
	{
		return new Conditions(night, underground);
	}

	/** @return whether it is night */
	boolean night()
	{
		return night;
	}

	/** @return whether the player is underground */
	boolean underground()
	{
		return underground;
	}

	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof Conditions))
			return false;

		Conditions that = (Conditions) other;
		return night == that.night && underground == that.underground;
	}

	@Override
	public int hashCode()
	{
		return (night ? 2 : 0) + (underground ? 1 : 0);
	}
}
