/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the words of the abbreviated card: name colour, chips, stat bars, record, words, odds and knocked out. */
public class WildCardContentTest
{
	private static Species imp()
	{
		return new Species("Imp", 5, CombatType.MAGIC, Trait.UNHOLY, Trait.SLAYER, Trait.MAGIC)
			.withFactors(1.25, 0.75, 1.0, 1.12);
	}

	private static WildEntry entry(Species species, boolean gilded)
	{
		return new WildEntry(3, species, 14, species.level(), false, gilded);
	}

	private static List<String> labels(List<SummaryContent.Chip> chips)
	{
		List<String> labels = new ArrayList<>();
		for (SummaryContent.Chip chip : chips)
			labels.add(chip.label());

		return labels;
	}

	@Test
	public void theChipsAreTheSummaryChipsThenTheRarity()
	{
		WildEntry entry = entry(imp(), false);

		WildCardContent card = WildCardContent.of(entry, null, false);

		List<String> expected = labels(SummaryContent.chipsFor(CombatType.MAGIC, entry.traits()));
		expected.add(Rarity.COMMON.label());
		assertEquals(expected, labels(card.chips()));
		List<SummaryContent.Chip> chips = card.chips();
		assertTrue(chips.get(0).style());
		assertEquals(CombatType.MAGIC.label(), chips.get(0).label());
		List<String> words = labels(chips);
		assertEquals(1, Collections.frequency(words, CombatType.MAGIC.label()));
		assertFalse(chips.get(chips.size() - 1).style());
	}

	@Test
	public void theStyleTraitIsSkippedLikeOnTheSummary()
	{
		WildCardContent card = WildCardContent.of(entry(imp(), false), null, false);

		assertEquals(1, Collections.frequency(labels(card.chips()), "Magic"));
		assertTrue(labels(card.chips()).contains("Unholy"));
		assertTrue(labels(card.chips()).contains("Slayer"));
	}

	@Test
	public void theNameAndLevelAreWorded()
	{
		WildCardContent card = WildCardContent.of(entry(imp(), false), null, false);

		assertEquals("Imp", card.name());
		assertEquals("Lv 14", card.level());
		assertEquals(Rarity.COMMON.color().getRGB() & 0xFFFFFF, card.nameRgb());
	}

	@Test
	public void aGildedCreatureUsesTheGildedColourAndPrefix()
	{
		WildCardContent card = WildCardContent.of(entry(imp(), true), null, false);

		assertEquals("Gilded Imp", card.name());
		assertEquals(WildTip.GILDED_COLOR.getRGB() & 0xFFFFFF, card.nameRgb());
	}

	@Test
	public void theFourStatRowsFillFromTheFactor()
	{
		WildCardContent card = WildCardContent.of(entry(imp(), false), null, false);

		List<WildCardContent.StatRow> rows = card.stats();
		assertEquals(4, rows.size());
		assertEquals("Health", rows.get(0).label());
		assertEquals(1.0, rows.get(0).fraction(), 1e-9);
		assertEquals("+25%", rows.get(0).percentText());
		assertEquals(0.0, rows.get(1).fraction(), 1e-9);
		assertEquals("-25%", rows.get(1).percentText());
		assertEquals(0.5, rows.get(2).fraction(), 1e-9);
		assertEquals("0%", rows.get(2).percentText());
		assertEquals(0.74, rows.get(3).fraction(), 1e-9);
		assertEquals("+12%", rows.get(3).percentText());
	}

	@Test
	public void aFactorOfOneQuarterBelowFillsNothingAndOneQuarterAboveFillsAll()
	{
		Species species = new Species("Cow", 2, CombatType.CRUSH).withFactors(0.75, 1.25, 1.0, 1.0);

		WildCardContent card = WildCardContent.of(entry(species, false), null, false);

		List<WildCardContent.StatRow> rows = card.stats();
		assertEquals(0.0, rows.get(0).fraction(), 0.0);
		assertEquals(1.0, rows.get(1).fraction(), 0.0);
	}

	@Test
	public void theWordsAreTheSpeciesTwoStrongestJoinedByADot()
	{
		WildCardContent card = WildCardContent.of(entry(imp(), false), null, false);

		assertEquals(String.join(" · ", imp().words(2)), card.words());
		assertTrue(card.words().contains(" · "));
	}

	@Test
	public void aPlainSpeciesHasNoWords()
	{
		Species plain = new Species("Cow", 2, CombatType.CRUSH);

		assertEquals("", WildCardContent.of(entry(plain, false), null, false).words());
	}

	@Test
	public void aNullRecordLeavesOutTheRecordLine()
	{
		WildCardContent card = WildCardContent.of(entry(imp(), false), null, false);

		assertNull(card.recordLabel());
	}

	@Test
	public void aRecordGivesItsWordAndColour()
	{
		WildEntry entry = entry(imp(), false);
		entry.record(WildRecord.of(0, true));

		WildCardContent card = WildCardContent.of(entry, null, false);

		assertEquals("New", card.recordLabel());
		assertEquals(0xFFE066, card.recordRgb());
	}

	@Test
	public void theOddsLineNamesTheJarAndThePercent()
	{
		JarSuggest.JarChoice shown = new JarSuggest.JarChoice(JarType.RUNE, 2, 0.456, false);

		WildCardContent card = WildCardContent.of(entry(imp(), false), shown, false);

		assertEquals("Rune jar 46%", card.odds());
	}

	@Test
	public void withNoJarTheOddsLineSaysSo()
	{
		WildCardContent card = WildCardContent.of(entry(imp(), false), null, false);

		assertEquals("No jars", card.odds());
	}

	@Test
	public void aKnockedOutCreatureAddsItsLine()
	{
		WildCardContent hurt = WildCardContent.of(entry(imp(), false), null, true);
		WildCardContent well = WildCardContent.of(entry(imp(), false), null, false);

		assertEquals("Knocked out: twice as easy to catch", hurt.knockedOutLine());
		assertNull(well.knockedOutLine());
	}
}
