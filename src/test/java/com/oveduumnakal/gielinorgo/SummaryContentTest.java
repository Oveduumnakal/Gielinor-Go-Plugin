/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Checks the words the Summary page says: the title and the species under it, the gender, the chips, the four stats
 * with the rating, the nature box, the description, the moves, the bond and the evolution hint.
 */
public class SummaryContentTest
{
	private static final Move SLASH = new Move("slash", "Slash", CombatType.SLASH, 1.0, 0.95);
	private static final Move BASH = new Move("bash", "Bash", CombatType.CRUSH, 1.4, 0.8);
	private static final Move SPARK = new Move("spark", "Spark", CombatType.MAGIC, 1.2, 0.9);

	private static CreatureView view(String nickname, StatRolls rolls, List<Move> moves, List<Move> pending,
		int hearts, String waiting)
	{
		return new CreatureView("Imp", 14, 0.25, 0.9, Rarity.RARE, CombatType.MAGIC, false, nickname, rolls, 0, moves,
			pending, hearts, waiting)
			.withSpecies(new Species("Imp", 5, CombatType.MAGIC, Trait.UNHOLY, Trait.SLAYER, Trait.MAGIC))
			.withGender(Gender.MALE)
			.withDetails("A minor demon.", "Grows into Lesser demon (20)");
	}

	private static SummaryContent content(CreatureView view)
	{
		return SummaryContent.of(RowModel.of(view, 2));
	}

	private static CreatureView plain()
	{
		return view("Sparky", new StatRolls(3, -2, 0, 5, Temperament.BRAVE), Arrays.asList(SLASH, BASH),
			Collections.emptyList(), 3, null);
	}

	@Test
	public void theTitleIsTheNicknameWithTheSpeciesUnderItAndTheGenderAndLevelBeside()
	{
		SummaryContent c = content(plain());

		assertEquals("Sparky", c.title());
		assertEquals("Imp", c.subtitle());
		assertEquals(Gender.MALE, c.gender());
		assertEquals("Lv 14", c.level());
	}

	@Test
	public void withoutANicknameTheTitleIsTheSpeciesAndThereIsNoSecondLine()
	{
		SummaryContent c = content(view(null, StatRolls.AVERAGE, Collections.emptyList(), Collections.emptyList(), 0,
			null));

		assertEquals("Imp", c.title());
		assertEquals("", c.subtitle());
	}

	@Test
	public void theChipsAreTheStyleThenEveryTraitBesidesTheStyleOne()
	{
		List<String> labels = new ArrayList<>();
		for (SummaryContent.Chip chip : content(plain()).chips())
			labels.add(chip.label());

		assertEquals(Arrays.asList("Magic", "Unholy", "Slayer"), labels);
		assertTrue(content(plain()).chips().get(0)
			.style());
		assertTrue(!content(plain()).chips().get(1)
			.style());
	}

	@Test
	public void theFourStatsComeInOrderWithTheirRollsAndTheNetChange()
	{
		List<SummaryContent.StatLine> stats = content(plain()).stats();

		assertEquals(4, stats.size());
		assertEquals("Health", stats.get(0).label());
		assertEquals("+3", stats.get(0).rollText());
		assertEquals("Attack", stats.get(1).label());
		assertEquals("-2", stats.get(1).rollText());
		assertEquals("+7%", stats.get(1).netText());
		assertEquals("Defence", stats.get(2).label());
		assertEquals("0", stats.get(2).rollText());
		assertEquals("-10%", stats.get(2).netText());
		assertEquals("Speed", stats.get(3).label());
		assertEquals("+5", stats.get(3).rollText());
	}

	@Test
	public void aStatBarFillsFromTheLowestRollToTheHighest()
	{
		List<SummaryContent.StatLine> stats = content(view("A", new StatRolls(-5, 0, 5, 3, Temperament.STEADY),
			Collections.emptyList(), Collections.emptyList(), 0, null)).stats();

		assertEquals(0.0, stats.get(0).fraction(), 0.0);
		assertEquals(0.5, stats.get(1).fraction(), 0.0);
		assertEquals(1.0, stats.get(2).fraction(), 0.0);
		assertEquals("", stats.get(1).netText());
	}

	@Test
	public void theRatingIsTheOneWordOfTheRolls()
	{
		assertEquals("Average", content(plain()).rating());
		assertEquals("Exceptional", content(view("A", new StatRolls(5, 5, 5, 5, Temperament.STEADY),
			Collections.emptyList(), Collections.emptyList(), 0, null)).rating());
	}

	@Test
	public void theNatureBoxHasTheTemperamentAndItsEffect()
	{
		SummaryContent c = content(plain());

		assertEquals("Nature: Brave", c.natureTitle());
		assertEquals("+10% Attack, -10% Defence", c.natureEffect());
		assertEquals("no effect", content(view("A", StatRolls.AVERAGE, Collections.emptyList(),
			Collections.emptyList(), 0, null)).natureEffect());
	}

	@Test
	public void theDescriptionIsTheExamineLineOrAnHonestSentence()
	{
		assertEquals("A minor demon.", content(plain()).description());
		CreatureView none = new CreatureView("Imp", 1, 0, 1.0, Rarity.COMMON, CombatType.MAGIC, false, null);

		assertEquals(SummaryContent.NO_DESCRIPTION, content(none).description());
	}

	@Test
	public void theMovesAreAtMostFourAndAWaitingMoveIsAPendingBadge()
	{
		SummaryContent c = content(plain());

		assertEquals(2, c.moves().size());
		assertEquals("Slash", c.moves().get(0)
			.name());
		assertEquals("Slash, power 1, accuracy 95%", c.moves().get(0)
			.detail());
		assertEquals(CombatType.CRUSH, c.moves().get(1)
			.style());
		assertNull(c.pendingMove());

		List<Move> five = Arrays.asList(SLASH, BASH, SPARK, SLASH, BASH);
		SummaryContent crowded = content(view("A", StatRolls.AVERAGE, five, Collections.singletonList(SPARK), 0,
			null));

		assertEquals(SummaryContent.MAX_MOVES, crowded.moves().size());
		assertEquals("New move: Spark", crowded.pendingMove());
	}

	@Test
	public void theBondHeartsAndTheirWordsFollowTheBond()
	{
		assertEquals(3, content(plain()).hearts());
		assertTrue(content(plain()).bondText()
			.startsWith("Bond 3 of 5"));
		assertTrue(content(view("A", StatRolls.AVERAGE, Collections.emptyList(), Collections.emptyList(), 0, null))
			.bondText().startsWith("No bond yet"));
	}

	@Test
	public void theEvolutionHintIsWhatItWaitsForThenWhatItGrowsInto()
	{
		assertEquals("Grows into Lesser demon (20)", content(plain()).evolution());
		assertEquals("Evolves at night", content(view("A", StatRolls.AVERAGE, Collections.emptyList(),
			Collections.emptyList(), 0, "Evolves at night")).evolution());
		assertEquals(SummaryContent.DOES_NOT_EVOLVE, content(new CreatureView("Imp", 1, 0, 1.0, Rarity.COMMON,
			CombatType.MAGIC, false, null)).evolution());
	}

	@Test
	public void upAndDownGoToTheNeighbouringCreatureAndWrapAtTheEnds()
	{
		assertEquals(3, SummaryContent.neighbour(2, 1, 6));
		assertEquals(1, SummaryContent.neighbour(2, -1, 6));
		assertEquals(0, SummaryContent.neighbour(5, 1, 6));
		assertEquals(5, SummaryContent.neighbour(0, -1, 6));
		assertEquals(0, SummaryContent.neighbour(0, 1, 1));
		assertEquals(0, SummaryContent.neighbour(3, 1, 0));
	}

	@Test
	public void aCreatureOfAnUnknownSpeciesStillHasASummary()
	{
		CreatureView unknown = new CreatureView("Mystery", 3, 0, 1.0, Rarity.COMMON, CombatType.CRUSH, false, null);
		SummaryContent c = content(unknown);

		assertEquals("Mystery", c.title());
		assertEquals(1, c.chips().size());
		assertEquals("Crush", c.chips().get(0)
			.label());
		assertNull(c.gender());
		assertTrue(c.moves().isEmpty());
	}
}
