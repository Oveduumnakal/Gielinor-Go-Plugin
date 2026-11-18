/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * What the player's Creature Log knows about a wild creature's species when the creature is looked at: new to them,
 * seen before, or already caught. Caught beats new, and new beats seen. Immutable and free of RuneLite types, so
 * the overlay can read it from the render thread.
 */
final class WildRecord
{
	/** The three states, in the order a player passes through them. */
	enum Kind
	{
		/** The species was first met when this creature appeared, and none was ever caught. */
		NEW("New", "New to you: first time you meet this species", 0xFFE066),

		/** The species was seen before this creature appeared, and none was ever caught. */
		SEEN("Seen", "Seen before, not caught yet", 0xB0C4DE),

		/** At least one of the species was caught, hatched or evolved into. */
		CAUGHT("Caught", "Caught before", 0x8BE28B);

		private final String label;
		private final String tip;
		private final int rgb;

		Kind(String label, String tip, int rgb)
		{
			this.label = label;
			this.tip = tip;
			this.rgb = rgb;
		}
	}

	private final Kind kind;
	private final int caughtCount;

	private WildRecord(Kind kind, int caughtCount)
	{
		this.kind = kind;
		this.caughtCount = caughtCount;
	}

	/**
	 * Works out the record of a species.
	 *
	 * @param caughtCount how many times the player has caught the species (zero or less counts as never)
	 * @param firstMeeting whether this creature's appearance was the first time the species was ever seen
	 * @return the record: caught when the count is above zero, else new for a first meeting, else seen
	 */
	static WildRecord of(int caughtCount, boolean firstMeeting)
	{
		if (caughtCount > 0)
			return new WildRecord(Kind.CAUGHT, caughtCount);

		return new WildRecord(firstMeeting ? Kind.NEW : Kind.SEEN, 0);
	}

	/** @return which of the three states this is */
	Kind kind()
	{
		return kind;
	}

	/** @return how many times the species was caught, or zero when it never was */
	int caughtCount()
	{
		return caughtCount;
	}

	/** @return the word the menu uses: New, Seen or Caught */
	String label()
	{
		return kind.label;
	}

	/** @return the sentence a tooltip uses; for a caught species it ends with the count, as in "Caught before (x3)" */
	String tipText()
	{
		if (kind == Kind.CAUGHT)
			return kind.tip + " (x" + caughtCount + ")";

		return kind.tip;
	}

	/** @return the colour of the mark as 0xRRGGBB */
	int rgb()
	{
		return kind.rgb;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
			return true;

		if (!(other instanceof WildRecord))
			return false;

		WildRecord that = (WildRecord) other;
		return kind == that.kind && caughtCount == that.caughtCount;
	}

	@Override
	public int hashCode()
	{
		return 31 * kind.hashCode() + caughtCount;
	}
}
