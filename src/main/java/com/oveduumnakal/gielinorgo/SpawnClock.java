/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

/** Where the spawn rules get the date and the time, so a test can set both. */
interface SpawnClock
{
	/** @return today's date in UTC, which decides the featured species and the weekend event */
	LocalDate utcDate();

	/** @return the time of day in UTC, which decides when the featured species change */
	LocalTime utcTime();

	/** @return the time of day on this computer's clock, which decides night */
	LocalTime localTime();

	/** @return the real clock */
	static SpawnClock system()
	{
		return new SpawnClock()
		{
			@Override
			public LocalDate utcDate()
			{
				return LocalDate.now(ZoneOffset.UTC);
			}

			@Override
			public LocalTime utcTime()
			{
				return LocalTime.now(ZoneOffset.UTC);
			}

			@Override
			public LocalTime localTime()
			{
				return LocalTime.now();
			}
		};
	}
}
