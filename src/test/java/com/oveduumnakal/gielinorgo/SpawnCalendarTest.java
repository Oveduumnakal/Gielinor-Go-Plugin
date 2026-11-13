/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the featured species, night, weekend events and how they combine, with a settable date and time. */
public class SpawnCalendarTest
{
	private static final LocalDate MONDAY = LocalDate.of(2026, 11, 9);
	private static final LocalDate FRIDAY = LocalDate.of(2026, 11, 13);
	private static final LocalDate SATURDAY = LocalDate.of(2026, 11, 14);
	private static final LocalDate SUNDAY = LocalDate.of(2026, 11, 15);
	private static final LocalTime NOON = LocalTime.of(12, 0);
	private static final LocalTime MIDNIGHT = LocalTime.of(0, 0);

	private final SpeciesTable table = bigTable();
	private final SpawnCalendar calendar = new SpawnCalendar(table);

	private static SpeciesTable bigTable()
	{
		List<Species> all = new ArrayList<>();
		Random random = new Random(7);
		for (int i = 0; i < 400; i++)
			all.add(new Species("Beast " + i, 1 + random.nextInt(i < 250 ? 30 : 400), CombatType.STAB));

		all.add(new Species("Spirit", 20, CombatType.STAB, Trait.UNHOLY));
		all.add(new Species("Cave goblin", 20, CombatType.STAB));
		all.add(new Species("Colossus", 20, CombatType.STAB, Trait.LARGE));
		all.add(new Species("Wyrm", 20, CombatType.STAB, Trait.FIERY));
		return new SpeciesTable(all);
	}

	private static SpawnClock clock(LocalDate utcDate, LocalTime utcTime, LocalTime localTime)
	{
		return new SpawnClock()
		{
			@Override
			public LocalDate utcDate()
			{
				return utcDate;
			}

			@Override
			public LocalTime utcTime()
			{
				return utcTime;
			}

			@Override
			public LocalTime localTime()
			{
				return localTime;
			}
		};
	}

	private Species named(String name)
	{
		return table.find(name);
	}

	@Test
	public void theDateHashIsFixedArithmetic()
	{
		assertEquals(7531437802973452838L, DateHash.mix(0, 0));
		assertEquals(DateHash.mix(20_000, 3), DateHash.mix(20_000, 3));
		assertNotEquals(DateHash.mix(20_000, 3), DateHash.mix(20_001, 3));
		assertNotEquals(DateHash.mix(20_000, 3), DateHash.mix(20_000, 4));
		assertEquals(0, DateHash.pick(5, 1, 1));
		assertTrue(DateHash.pick(-5, 1, 7) >= 0);
	}

	@Test
	public void theSameDateAlwaysGivesTheSameFive()
	{
		List<Species> first = calendar.featured(MONDAY);
		List<Species> again = new SpawnCalendar(bigTable()).featured(MONDAY);

		assertEquals(5, first.size());
		for (int i = 0; i < 5; i++)
			{
			String key = first.get(i).key();
			assertEquals(key, again.get(i).key());
		}
	}

	@Test
	public void eachPickIsOfItsRarity()
	{
		List<Species> picks = calendar.featured(MONDAY);

		for (int i = 0; i < picks.size(); i++)
			assertEquals(Rarity.values()[i], picks.get(i).rarity());
	}

	@Test
	public void thirtyDaysGiveManyDifferentCommonPicks()
	{
		Set<String> commons = new HashSet<>();
		for (int day = 0; day < 30; day++)
			{
			List<Species> picks = calendar.featured(MONDAY.plusDays(day));
			commons.add(picks.get(0).key());
		}

		assertTrue("got " + commons.size(), commons.size() >= 20);
	}

	@Test
	public void aRarityWithNoSpeciesIsLeftOut()
	{
		SpeciesTable small = new SpeciesTable(List.of(new Species("Rat", 1, CombatType.STAB)));

		assertEquals(1, new SpawnCalendar(small).featured(MONDAY).size());
	}

	@Test
	public void nightRunsFromTwentyToJustBeforeSix()
	{
		assertTrue(SpawnCalendar.isNight(LocalTime.of(20, 0)));
		assertFalse(SpawnCalendar.isNight(LocalTime.of(19, 59)));
		assertTrue(SpawnCalendar.isNight(LocalTime.of(5, 59)));
		assertFalse(SpawnCalendar.isNight(LocalTime.of(6, 0)));
		assertTrue(SpawnCalendar.isNight(MIDNIGHT));
		assertFalse(SpawnCalendar.isNight(NOON));
	}

	@Test
	public void onlySaturdayAndSundayAreWeekend()
	{
		assertNull(SpawnCalendar.weekendTheme(FRIDAY));
		assertNull(SpawnCalendar.weekendTheme(MONDAY));
		assertTrue(SpawnCalendar.weekendTheme(SATURDAY) != null);
		assertEquals(SpawnCalendar.weekendTheme(SATURDAY), SpawnCalendar.weekendTheme(SUNDAY));
	}

	@Test
	public void themesTurnByWeekAndCycleThroughAllSix()
	{
		Set<SpawnCalendar.Theme> seen = new HashSet<>();
		SpawnCalendar.Theme previous = null;
		for (int week = 0; week < 6; week++)
		{
			SpawnCalendar.Theme theme = SpawnCalendar.weekendTheme(SATURDAY.plusWeeks(week));
			assertNotEquals(previous, theme);
			previous = theme;
			seen.add(theme);
		}

		assertEquals(6, seen.size());
		assertEquals(SpawnCalendar.weekendTheme(SATURDAY), SpawnCalendar.weekendTheme(SATURDAY.plusWeeks(6)));
	}

	@Test
	public void anOrdinarySpeciesOnAnOrdinaryDayIsNotBoosted()
	{
		Species plain = pickNotFeatured(MONDAY);

		assertEquals(1.0, calendar.wildMultiplier(plain, MONDAY, NOON), 0.0);
		assertEquals(1.0, calendar.gildedMultiplier(plain, MONDAY), 0.0);
	}

	@Test
	public void aFeaturedSpeciesIsWildThreeTimesAsOften()
	{
		Species featured = calendar.featured(MONDAY).get(0);

		assertEquals(3.0, calendar.wildMultiplier(featured, MONDAY, NOON), 0.0);
		assertTrue(calendar.isFeatured(featured, MONDAY));
	}

	@Test
	public void nightDoublesUndeadOnly()
	{
		Species spirit = named("Spirit");
		Species plain = pickNotFeatured(MONDAY);
		boolean spiritFeatured = calendar.isFeatured(spirit, MONDAY);

		assertEquals(spiritFeatured ? 6.0 : 2.0, calendar.wildMultiplier(spirit, MONDAY, MIDNIGHT), 0.0);
		assertEquals(1.0, calendar.wildMultiplier(plain, MONDAY, MIDNIGHT), 0.0);
	}

	@Test
	public void theWeekendThemeDoublesWildAndGilded()
	{
		SpawnCalendar.Theme theme = SpawnCalendar.weekendTheme(SATURDAY);
		Species member = memberOf(theme);
		Species outsider = outsiderOf(theme);

		assertEquals(2.0, calendar.gildedMultiplier(member, SATURDAY), 0.0);
		assertEquals(1.0, calendar.gildedMultiplier(outsider, SATURDAY), 0.0);
		double expected = calendar.isFeatured(member, SATURDAY) ? 6.0 : 2.0;
		assertEquals(expected, calendar.wildMultiplier(member, SATURDAY, NOON), 0.0);
		assertEquals(1.0, calendar.wildMultiplier(outsiderNotFeatured(), SATURDAY, NOON), 0.0);
	}

	@Test
	public void multipliersMultiplyAndStopAtSix()
	{
		SpeciesTable table = new SpeciesTable(List.of(new Species("Ghost goblin", 5, CombatType.STAB, Trait.UNHOLY)));
		SpawnCalendar only = new SpawnCalendar(table);
		Species ghost = table.find("Ghost goblin");
		LocalDate goblinWeekend = goblinWeekend();

		assertTrue(only.isFeatured(ghost, goblinWeekend));
		assertEquals(6.0, only.wildMultiplier(ghost, goblinWeekend, MIDNIGHT), 0.0);
		assertEquals(6.0, only.wildMultiplier(ghost, goblinWeekend, NOON), 0.0);
		assertEquals(3.0, only.wildMultiplier(ghost, goblinWeekend.minusDays(2), NOON), 0.0);
		assertEquals(6.0, only.wildMultiplier(ghost, goblinWeekend.minusDays(2), MIDNIGHT), 0.0);
	}

	@Test
	public void theChanceIsTheMultiplierOverTheSettingAndNeverAboveOne()
	{
		Species featured = calendar.featured(MONDAY).get(0);
		SpawnClock noon = clock(MONDAY, NOON, NOON);

		assertEquals(3.0 / 8, calendar.chance(featured, 8, true, noon), 1e-9);
		assertEquals(1.0, calendar.chance(featured, 2, true, noon), 0.0);
		assertEquals(1.0, calendar.chance(featured, 1, true, noon), 0.0);
	}

	@Test
	public void withSpawnEventsOffEveryMultiplierIsOne()
	{
		SpawnClock saturdayNight = clock(SATURDAY, NOON, MIDNIGHT);
		Species featured = calendar.featured(SATURDAY).get(0);
		Species spirit = named("Spirit");
		Species member = memberOf(SpawnCalendar.weekendTheme(SATURDAY));

		for (Species species : List.of(featured, spirit, member))
		{
			assertEquals(1.0 / 8, calendar.chance(species, 8, false, saturdayNight), 1e-9);
			assertEquals(WildTracker.GILDED_ONE_IN, calendar.gildedOneIn(species, false, saturdayNight));
		}

		assertFalse(calendar.today(false, saturdayNight).active());
	}

	@Test
	public void theWeekendHalvesTheGildedOdds()
	{
		SpawnClock saturday = clock(SATURDAY, NOON, NOON);
		Species member = memberOf(SpawnCalendar.weekendTheme(SATURDAY));
		Species outsider = outsiderOf(SpawnCalendar.weekendTheme(SATURDAY));

		assertEquals(WildTracker.GILDED_ONE_IN / 2, calendar.gildedOneIn(member, true, saturday));
		assertEquals(WildTracker.GILDED_ONE_IN, calendar.gildedOneIn(outsider, true, saturday));
	}

	@Test
	public void todayNamesTheFeaturedTheEventAndTheTimeToReset()
	{
		TodayView view = calendar.today(true, clock(SATURDAY, LocalTime.of(18, 48), LocalTime.of(21, 0)));

		assertTrue(view.active());
		assertEquals(5, view.featured().size());
		assertTrue(view.night());
		assertEquals("5h 12m", view.resetText());
		assertTrue(view.event().endsWith(" weekend: x2 wild, x2 gilded"));
		TodayView.Pick middle = view.featured().get(2);
		assertTrue(view.isFeatured(middle.name()));
		assertNull(calendar.today(true, clock(MONDAY, NOON, NOON)).event());
	}

	@Test
	public void theResetTextRoundsUpAndNeverShowsZero()
	{
		assertEquals(1, TodayView.minutesToMidnight(86_399));
		assertEquals(1, TodayView.minutesToMidnight(86_340));
		assertEquals(2, TodayView.minutesToMidnight(86_339));
		assertEquals(1440, TodayView.minutesToMidnight(0));
	}

	private Species pickNotFeatured(LocalDate date)
	{
		for (Species species : table.all())
		{
			if (!calendar.isFeatured(species, date) && !species.traits().contains(Trait.UNHOLY))
				return species;
		}

		throw new AssertionError("no ordinary species");
	}

	private Species outsiderNotFeatured()
	{
		SpawnCalendar.Theme theme = SpawnCalendar.weekendTheme(SATURDAY);
		for (Species species : table.all())
		{
			if (!theme.covers(species) && !calendar.isFeatured(species, SATURDAY))
				return species;
		}

		throw new AssertionError("none");
	}

	private Species memberOf(SpawnCalendar.Theme theme)
	{
		for (Species species : table.all())
		{
			if (theme.covers(species))
				return species;
		}

		throw new AssertionError("no member of " + theme);
	}

	private Species outsiderOf(SpawnCalendar.Theme theme)
	{
		for (Species species : table.all())
		{
			if (!theme.covers(species))
				return species;
		}

		throw new AssertionError("everyone is a member of " + theme);
	}

	private static LocalDate goblinWeekend()
	{
		LocalDate day = SATURDAY;
		while (SpawnCalendar.weekendTheme(day) != SpawnCalendar.Theme.GOBLIN)
			day = day.plusWeeks(1);

		return day;
	}
}
