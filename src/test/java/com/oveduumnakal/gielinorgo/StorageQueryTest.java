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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** The Storage query: which creatures pass each combination of filters, how a leaf cycles, and the words. */
public class StorageQueryTest
{
	private static final StatRolls GOOD = new StatRolls(5, 5, 5, 5, Temperament.BRAVE);

	private static CreatureView view(int level, CombatType type, Rarity rarity, boolean gilded, String nickname,
		Gender gender, StatRolls rolls)
	{
		return new CreatureView("Goblin", level, 0.5, 1.0, rarity, type, gilded, nickname, rolls).withGender(gender);
	}

	private static CreatureView plain(int level, CombatType type)
	{
		return view(level, type, Rarity.COMMON, false, null, Gender.MALE, StatRolls.AVERAGE);
	}

	private static StorageQuery of(StorageQuery.Facet facet, int value, boolean hidden)
	{
		StorageQuery query = StorageQuery.NONE.cycled(facet, value);
		return hidden ? query.flipped(0) : query;
	}

	private static String label(StorageQuery query, int index)
	{
		List<StorageQuery.Criterion> list = query.criteria();
		return list.get(index).label();
	}

	private static boolean hidden(StorageQuery query, int index)
	{
		List<StorageQuery.Criterion> list = query.criteria();
		return list.get(index).hidden();
	}

	private static List<Integer> levels(StorageQuery query, CreatureView... all)
	{
		List<Integer> kept = new ArrayList<>();
		for (CreatureView each : all)
		{
			if (query.accepts(each))
				kept.add(each.level());
		}

		return kept;
	}

	@Test
	public void noCriteriaAcceptsEveryone()
	{
		assertTrue(StorageQuery.NONE.accepts(plain(1, CombatType.STAB)));
		assertFalse(StorageQuery.NONE.active());
		assertEquals(StorageQuery.Sort.BOX_ORDER, StorageQuery.NONE.sort());
	}

	@Test
	public void levelOverKeepsHigherLevelsOnly()
	{
		StorageQuery query = of(StorageQuery.Facet.LEVEL_OVER, 20, false);

		assertEquals(Collections.singletonList(21), levels(query, plain(20, CombatType.STAB),
			plain(21, CombatType.STAB)));
		assertFalse(query.accepts(plain(20, CombatType.STAB)));
		assertTrue(query.accepts(plain(21, CombatType.STAB)));
	}

	@Test
	public void aHiddenLevelOverKeepsTheRest()
	{
		StorageQuery query = of(StorageQuery.Facet.LEVEL_OVER, 20, true);

		assertTrue(query.accepts(plain(20, CombatType.STAB)));
		assertFalse(query.accepts(plain(21, CombatType.STAB)));
		assertEquals("Level 20 or under", label(query, 0));
	}

	@Test
	public void gildedOnlyKeepsGildedCreatures()
	{
		StorageQuery query = of(StorageQuery.Facet.GILDED, 0, false);

		assertTrue(query.accepts(view(5, CombatType.STAB, Rarity.RARE, true, null, Gender.MALE, GOOD)));
		assertFalse(query.accepts(plain(5, CombatType.STAB)));
	}

	@Test
	public void crushKeepsCrushOnly()
	{
		StorageQuery query = of(StorageQuery.Facet.STYLE, CombatType.CRUSH.ordinal(), false);

		assertTrue(query.accepts(plain(5, CombatType.CRUSH)));
		assertFalse(query.accepts(plain(5, CombatType.SLASH)));
	}

	@Test
	public void notSlashDropsSlash()
	{
		StorageQuery query = of(StorageQuery.Facet.STYLE, CombatType.SLASH.ordinal(), true);

		assertFalse(query.accepts(plain(5, CombatType.SLASH)));
		assertTrue(query.accepts(plain(5, CombatType.CRUSH)));
		assertTrue(query.accepts(plain(5, CombatType.MAGIC)));
	}

	@Test
	public void twoShownStylesAreOrNotAnd()
	{
		StorageQuery query = StorageQuery.NONE.cycled(StorageQuery.Facet.STYLE, CombatType.CRUSH.ordinal())
			.cycled(StorageQuery.Facet.STYLE, CombatType.SLASH.ordinal());

		assertTrue(query.accepts(plain(5, CombatType.CRUSH)));
		assertTrue(query.accepts(plain(5, CombatType.SLASH)));
		assertFalse(query.accepts(plain(5, CombatType.STAB)));
	}

	@Test
	public void gildedAndNotSlashAreAnded()
	{
		StorageQuery query = StorageQuery.NONE.cycled(StorageQuery.Facet.GILDED, 0)
			.cycled(StorageQuery.Facet.STYLE, CombatType.SLASH.ordinal())
			.flipped(1);

		assertTrue(query.accepts(view(5, CombatType.CRUSH, Rarity.RARE, true, null, Gender.MALE, GOOD)));
		assertFalse(query.accepts(view(5, CombatType.SLASH, Rarity.RARE, true, null, Gender.MALE, GOOD)));
		assertFalse(query.accepts(plain(5, CombatType.CRUSH)));
	}

	@Test
	public void levelOverAndUnderMakeARange()
	{
		StorageQuery query = StorageQuery.NONE.cycled(StorageQuery.Facet.LEVEL_OVER, 20)
			.cycled(StorageQuery.Facet.LEVEL_UNDER, 50);

		assertEquals(Collections.singletonList(30), levels(query, plain(20, CombatType.STAB),
			plain(30, CombatType.STAB), plain(50, CombatType.STAB)));
	}

	@Test
	public void aShownAndAHiddenStyleOfOneFacetBothApply()
	{
		StorageQuery query = StorageQuery.NONE.cycled(StorageQuery.Facet.STYLE, CombatType.CRUSH.ordinal())
			.cycled(StorageQuery.Facet.STYLE, CombatType.SLASH.ordinal())
			.flipped(1);

		assertTrue(query.accepts(plain(5, CombatType.CRUSH)));
		assertFalse(query.accepts(plain(5, CombatType.SLASH)));
		assertFalse(query.accepts(plain(5, CombatType.STAB)));
	}

	@Test
	public void ratingRarityGenderAndNicknameFilter()
	{
		CreatureView strong = view(5, CombatType.STAB, Rarity.EPIC, false, "Bob", Gender.FEMALE, GOOD);
		CreatureView plain = plain(5, CombatType.STAB);

		StorageQuery rating = of(StorageQuery.Facet.RATING, strong.rating().ordinal(), false);

		assertTrue(rating.accepts(strong));
		assertFalse(rating.accepts(plain));
		assertTrue(of(StorageQuery.Facet.RARITY, Rarity.EPIC.ordinal(), false).accepts(strong));
		assertFalse(of(StorageQuery.Facet.RARITY, Rarity.EPIC.ordinal(), true).accepts(strong));
		assertTrue(of(StorageQuery.Facet.GENDER, Gender.FEMALE.ordinal(), false).accepts(strong));
		assertFalse(of(StorageQuery.Facet.GENDER, Gender.FEMALE.ordinal(), false).accepts(plain));
		assertTrue(of(StorageQuery.Facet.NICKNAMED, 0, false).accepts(strong));
		assertTrue(of(StorageQuery.Facet.NICKNAMED, 0, true).accepts(plain));
		assertFalse(of(StorageQuery.Facet.NICKNAMED, 0, true).accepts(strong));
	}

	@Test
	public void aLeafCyclesAbsentShownHiddenAbsent()
	{
		StorageQuery shown = StorageQuery.NONE.cycled(StorageQuery.Facet.STYLE, 2);
		StorageQuery hidden = shown.cycled(StorageQuery.Facet.STYLE, 2);
		StorageQuery gone = hidden.cycled(StorageQuery.Facet.STYLE, 2);

		assertEquals(StorageQuery.Stance.OFF, StorageQuery.NONE.stance(StorageQuery.Facet.STYLE, 2));
		assertEquals(StorageQuery.Stance.SHOWN, shown.stance(StorageQuery.Facet.STYLE, 2));
		assertEquals(StorageQuery.Stance.HIDDEN, hidden.stance(StorageQuery.Facet.STYLE, 2));
		assertEquals(StorageQuery.Stance.OFF, gone.stance(StorageQuery.Facet.STYLE, 2));
		assertFalse(gone.active());
	}

	@Test
	public void aNewLevelReplacesTheOldOneOfTheSameFacet()
	{
		StorageQuery query = StorageQuery.NONE.cycled(StorageQuery.Facet.LEVEL_OVER, 20)
			.cycled(StorageQuery.Facet.LEVEL_OVER, 40);

		assertEquals(1, query.criteria().size());
		assertEquals("Level over 40", label(query, 0));
		assertEquals(StorageQuery.Stance.OFF, query.stance(StorageQuery.Facet.LEVEL_OVER, 20));
	}

	@Test
	public void theSameLevelCyclesLikeAnyLeaf()
	{
		StorageQuery query = StorageQuery.NONE.cycled(StorageQuery.Facet.LEVEL_UNDER, 40)
			.cycled(StorageQuery.Facet.LEVEL_UNDER, 40);

		assertEquals("Level 40 or over", label(query, 0));
		assertFalse(query.cycled(StorageQuery.Facet.LEVEL_UNDER, 40).active());
	}

	@Test
	public void chipLabelsReadAsThePlanSays()
	{
		assertEquals("Level over 20", label(of(StorageQuery.Facet.LEVEL_OVER, 20, false), 0));
		assertEquals("Level under 40", label(of(StorageQuery.Facet.LEVEL_UNDER, 40, false), 0));
		assertEquals("Crush", label(of(StorageQuery.Facet.STYLE, CombatType.CRUSH.ordinal(), false), 0));
		assertEquals("Not Crush", label(of(StorageQuery.Facet.STYLE, CombatType.CRUSH.ordinal(), true), 0));
		assertEquals("Rare", label(of(StorageQuery.Facet.RARITY, Rarity.RARE.ordinal(), false), 0));
		assertEquals("Strong", label(of(StorageQuery.Facet.RATING, StatRolls.Rating.STRONG.ordinal(), false), 0));
		assertEquals("Male", label(of(StorageQuery.Facet.GENDER, Gender.MALE.ordinal(), false), 0));
		assertEquals("Gilded", label(of(StorageQuery.Facet.GILDED, 0, false), 0));
		assertEquals("Not gilded", label(of(StorageQuery.Facet.GILDED, 0, true), 0));
		assertEquals("Nicknamed", label(of(StorageQuery.Facet.NICKNAMED, 0, false), 0));
		assertEquals("Not nicknamed", label(of(StorageQuery.Facet.NICKNAMED, 0, true), 0));
	}

	@Test
	public void aLeafSaysItsOwnState()
	{
		StorageQuery shown = StorageQuery.NONE.cycled(StorageQuery.Facet.STYLE, 2);

		assertEquals("Crush", StorageQuery.NONE.stateWords(StorageQuery.Facet.STYLE, 2, "Crush"));
		assertEquals("Crush (only)", shown.stateWords(StorageQuery.Facet.STYLE, 2, "Crush"));
		assertEquals("Crush (hidden)", shown.flipped(0).stateWords(StorageQuery.Facet.STYLE, 2, "Crush"));
	}

	@Test
	public void flippedWithoutAndClearedEditTheCriteria()
	{
		StorageQuery query = StorageQuery.NONE.cycled(StorageQuery.Facet.GILDED, 0)
			.cycled(StorageQuery.Facet.NICKNAMED, 0);

		assertTrue(hidden(query.flipped(0), 0));
		assertFalse("flipping does not touch its neighbour", hidden(query.flipped(0), 1));
		List<StorageQuery.Criterion> rest = query.without(0).criteria();

		assertEquals(1, rest.size());
		assertEquals("Nicknamed", label(query.without(0), 0));
		assertSame(query, query.without(5));
		assertSame(query, query.flipped(-1));
		assertFalse(query.cleared().active());
	}

	@Test
	public void clearingKeepsTheSort()
	{
		StorageQuery query = StorageQuery.NONE.withSort(StorageQuery.Sort.LEVEL)
			.cycled(StorageQuery.Facet.GILDED, 0);

		assertEquals(StorageQuery.Sort.LEVEL, query.cleared().sort());
	}

	@Test
	public void pickingASortStartsAtItsDefaultAndPickingItAgainFlips()
	{
		StorageQuery level = StorageQuery.NONE.withSort(StorageQuery.Sort.LEVEL);
		StorageQuery species = StorageQuery.NONE.withSort(StorageQuery.Sort.SPECIES);

		assertTrue("level starts high to low", level.descending());
		assertFalse("species starts A to Z", species.descending());
		assertFalse(level.withSort(StorageQuery.Sort.LEVEL).descending());
		StorageQuery flipped = level.withSort(StorageQuery.Sort.LEVEL);

		assertTrue(flipped.withSort(StorageQuery.Sort.LEVEL).descending());
		assertTrue(StorageQuery.NONE.withSort(StorageQuery.Sort.RARITY).descending());
		assertTrue(StorageQuery.NONE.withSort(StorageQuery.Sort.DATE_CAUGHT).descending());
		assertFalse(StorageQuery.NONE.withSort(StorageQuery.Sort.NICKNAME).descending());
		assertTrue("a different sort starts at its own default", level.withSort(StorageQuery.Sort.SPECIES)
			.equals(species));
		assertSame(StorageQuery.NONE, StorageQuery.NONE.withSort(StorageQuery.Sort.BOX_ORDER));
	}

	@Test
	public void directionWordsMatchTheMenu()
	{
		assertEquals("high to low", StorageQuery.Sort.LEVEL.directionWords(true));
		assertEquals("low to high", StorageQuery.Sort.LEVEL.directionWords(false));
		assertEquals("A to Z", StorageQuery.Sort.SPECIES.directionWords(false));
		assertEquals("Z to A", StorageQuery.Sort.NICKNAME.directionWords(true));
		assertEquals("rarest first", StorageQuery.Sort.RARITY.directionWords(true));
		assertEquals("commonest first", StorageQuery.Sort.RARITY.directionWords(false));
		assertEquals("newest first", StorageQuery.Sort.DATE_CAUGHT.directionWords(true));
		assertEquals("oldest first", StorageQuery.Sort.DATE_CAUGHT.directionWords(false));
		assertEquals("", StorageQuery.Sort.BOX_ORDER.directionWords(true));
	}
}
