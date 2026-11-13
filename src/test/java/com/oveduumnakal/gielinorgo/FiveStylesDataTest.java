/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Checks the bundled data and the old-save split that go with the five combat styles (issue 85). */
public class FiveStylesDataTest
{
	private final Gson gson = new Gson();

	@Test
	public void theCatchableSpeciesAreSpreadOverTheFiveStylesAsRecorded() throws IOException
	{
		Map<CombatType, Integer> counts = new EnumMap<>(CombatType.class);
		for (Species species : species().all())
		{
			if (!species.questOnly())
				counts.merge(species.style(), 1, Integer::sum);
		}

		assertEquals("Stab", 206, (int) counts.get(CombatType.STAB));
		assertEquals("Slash", 220, (int) counts.get(CombatType.SLASH));
		assertEquals("Crush", 426, (int) counts.get(CombatType.CRUSH));
		assertEquals("Ranged", 89, (int) counts.get(CombatType.RANGED));
		assertEquals("Magic", 155, (int) counts.get(CombatType.MAGIC));
	}

	@Test
	public void noCityChampionIsBeatenByOneStyleAlone() throws IOException
	{
		SpeciesTable species = species();
		ChampionTable champions;
		try (Reader reader = reader(ChampionTable.class, ChampionTable.RESOURCE))
		{
			champions = ChampionTable.load(gson, reader, species);
		}

		assertEquals(8, champions.all().size());
		for (Champion champion : champions.all())
		{
			for (CombatType attacker : CombatType.values())
			{
				boolean beatsAll = true;
				for (Champion.Member member : champion.team())
					beatsAll &= attacker.beats(species.find(member.species()).style());

				assertFalse(champion.city() + " is beaten by " + attacker + " alone", beatsAll);
			}
		}
	}

	@Test
	public void theStarterTriangleStillHolds() throws IOException
	{
		SpeciesTable table = species();
		CombatType imp = table.find("Imp").style();
		CombatType archer = table.find("Gnome Archer").style();
		CombatType wizard = table.find("Wizard").style();

		assertEquals(CombatType.SLASH, imp);
		assertTrue(imp.beats(archer));
		assertTrue(archer.beats(wizard));
		assertTrue(wizard.beats(imp));
	}

	@Test
	public void everyStyleHasItsOwnDefaultPoolOfFourMoves() throws IOException
	{
		MoveTable table = moves();

		for (CombatType style : CombatType.values())
		{
			Species plain = new Species("Plain " + style, 5, style);
			String pool = table.poolOf(plain);

			assertEquals(style.name(), "default_" + style.name().toLowerCase(), pool);
			assertEquals(style.name(), 4, table.describePool(pool).size());
			Move first = table.movesAt(plain, 1).get(0);
			assertEquals(style.name(), style, first.style());
			assertEquals(style.name(), 1.0, first.power(), 0);
		}
	}

	@Test
	public void noMoveIsStillCalledMelee() throws IOException
	{
		MoveTable table = moves();

		assertEquals(CombatType.STAB, table.find("bite").style());
		assertEquals(CombatType.SLASH, table.find("claw").style());
		assertEquals(CombatType.CRUSH, table.find("punch").style());
		assertEquals(CombatType.CRUSH, table.find("constrict").style());
		assertEquals(CombatType.STAB, table.find("sting").style());
	}

	@Test
	public void anOldWarriorJarStackIsSplitInThreeAndTheKeyGoes()
	{
		SaveData data = gson.fromJson("{\"jars\":{\"BRONZE\":4,\"WARRIOR\":7},\"selectedJar\":\"WARRIOR\"}",
			SaveData.class).sanitized();

		assertEquals(2, data.jarCount(JarType.STAB));
		assertEquals(2, data.jarCount(JarType.SLASH));
		assertEquals(3, data.jarCount(JarType.CRUSH));
		assertEquals(4, data.jarCount(JarType.BRONZE));
		assertFalse(data.jars.containsKey("WARRIOR"));
		assertEquals(JarType.BRONZE.name(), data.selectedJar);
		SaveData empty = gson.fromJson("{\"jars\":{\"WARRIOR\":0}}", SaveData.class).sanitized();
		assertEquals(0, empty.jarCount(JarType.CRUSH));
	}

	@Test
	public void theThreeNewJarsAreFoundThroughAttackStrengthAndDefence()
	{
		assertEquals(JarType.STAB, JarType.forSkill("attack"));
		assertEquals(JarType.SLASH, JarType.forSkill("STRENGTH"));
		assertEquals(JarType.CRUSH, JarType.forSkill("Defence"));
		assertEquals(Trait.STAB, JarType.STAB.trait());
		assertEquals(2.5, JarType.CRUSH.traitBonus(), 0);
	}

	@Test
	public void aSpeciesWithNoStyleIsCrushAndHasTheCrushTrait()
	{
		Species none = new Species("Nobody", 1, null);

		assertEquals(CombatType.CRUSH, none.style());
		assertTrue(none.traits().contains(Trait.CRUSH));
		assertNotNull(Trait.forStyle(CombatType.SLASH));
	}

	private SpeciesTable species() throws IOException
	{
		try (Reader reader = reader(SpeciesTable.class, SpeciesTable.RESOURCE))
		{
			return SpeciesTable.load(gson, reader);
		}
	}

	private MoveTable moves() throws IOException
	{
		try (Reader reader = reader(MoveTable.class, MoveTable.RESOURCE))
		{
			return MoveTable.load(gson, reader);
		}
	}

	private static Reader reader(Class<?> anchor, String resource)
	{
		InputStream in = anchor.getResourceAsStream(resource);
		assertNotNull(resource + " is missing", in);
		return new InputStreamReader(in, StandardCharsets.UTF_8);
	}
}
