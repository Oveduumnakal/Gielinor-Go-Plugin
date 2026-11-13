/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.Gson;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Checks the rival's starter, when each battle opens, what it pays and the bundled teams. */
public class RivalRulesTest
{
	private static SpeciesTable species;
	private static RivalTable table;

	@BeforeClass
	public static void load() throws IOException
	{
		Gson gson = new Gson();
		try (Reader in = reader("species.json"))
		{
			species = SpeciesTable.load(gson, in);
		}

		try (Reader in = reader("rival.json"))
		{
			table = RivalTable.load(gson, in, species);
		}
	}

	private static Reader reader(String name)
	{
		InputStream in = SpeciesTable.class.getResourceAsStream(name);
		assertNotNull(name, in);
		return new InputStreamReader(in, StandardCharsets.UTF_8);
	}

	@Test
	public void theRivalTakesTheStarterThatBeatsTheOneYouPicked()
	{
		assertEquals("wizard", RivalRules.counterOf("Imp"));
		assertEquals("gnome archer", RivalRules.counterOf("Wizard"));
		assertEquals("imp", RivalRules.counterOf("Gnome Archer"));
		assertNull(RivalRules.counterOf("Goblin"));
	}

	@Test
	public void everyCounterStyleBeatsTheStarterItAnswers()
	{
		for (String name : StarterRules.STARTERS)
		{
			Species mine = species.find(name);
			Species theirs = species.find(RivalRules.counterOf(name));
			assertTrue(name, theirs.style().beats() == mine.style());
		}
	}

	@Test
	public void aSaveWithNoStarterAnswersTheStyleOfItsLead()
	{
		assertEquals("wizard", RivalRules.starterAgainst(species.find("Goblin"), species));
		assertEquals("imp", RivalRules.starterAgainst(species.find("Guard"), species));
		assertEquals("gnome archer", RivalRules.starterAgainst(species.find("Banshee"), species));
	}

	@Test
	public void noLeadGivesTheImp()
	{
		assertEquals("imp", RivalRules.starterAgainst(null, species));
	}

	@Test
	public void battlesOpenAtTwoFourSixAndEightSigilsAndTheEliteClear()
	{
		assertEquals(0, RivalRules.unlocked(0, 0));
		assertEquals(0, RivalRules.unlocked(1, 0));
		assertEquals(1, RivalRules.unlocked(2, 0));
		assertEquals(1, RivalRules.unlocked(3, 0));
		assertEquals(2, RivalRules.unlocked(4, 0));
		assertEquals(3, RivalRules.unlocked(6, 0));
		assertEquals(4, RivalRules.unlocked(8, 0));
		assertEquals(5, RivalRules.unlocked(8, 1));
		assertEquals(5, RivalRules.unlocked(8, 7));
	}

	@Test
	public void rewardsGrowWithTheBattle()
	{
		assertEquals(Rarity.RARE, RivalRules.egg(1));
		assertEquals(Rarity.RARE, RivalRules.egg(2));
		assertEquals(Rarity.EPIC, RivalRules.egg(3));
		assertEquals(Rarity.EPIC, RivalRules.egg(4));
		assertEquals(Rarity.LEGENDARY, RivalRules.egg(5));
		for (int battle = 2; battle <= RivalRules.BATTLES; battle++)
			assertTrue(RivalRules.jars(battle) > RivalRules.jars(battle - 1));
	}

	@Test
	public void theBundledTableIsSound()
	{
		assertNull(table.problem(species));
		assertTrue(table.available());
		assertEquals("Bryn", table.name());
	}

	@Test
	public void everyStarterHasFiveTeamsOfTwoToSixCatchableCreaturesInRange()
	{
		for (String starter : StarterRules.STARTERS)
		{
			String key = RivalRules.counterOf(starter);
			int lastLevel = 0;
			for (int battle = 1; battle <= RivalRules.BATTLES; battle++)
			{
				List<Champion.Member> team = table.team(key, battle);
				assertEquals(key + " " + battle, battle + 1, team.size());
				for (Champion.Member member : team)
				{
					Species found = species.find(member.species());
					assertNotNull(member.species(), found);
					assertFalse(member.species(), found.questOnly());
					assertTrue(member.level() >= 10 && member.level() <= 99);
				}

				assertTrue(key, team.get(0).level() > lastLevel);
				lastLevel = team.get(0).level();
			}

			assertTrue(table.team(key, 1).get(0).level() <= 16);
			assertTrue(table.team(key, 5).get(0).level() >= 85);
		}
	}

	@Test
	public void everyLineIsWordedAndShortEnoughForTheCard()
	{
		for (int battle = 1; battle <= RivalRules.BATTLES; battle++)
		{
			assertFalse(table.before(battle).isEmpty());
			assertFalse(table.after(battle).isEmpty());
			assertTrue(table.before(battle).length() <= 90);
			assertTrue(table.after(battle).length() <= 90);
		}
	}

	@Test
	public void aFileWithAMissingTeamOrAnUnknownCreatureLoadsAsNoRival()
	{
		Gson gson = new Gson();
		String missing = "{\"name\":\"Bryn\",\"before\":[\"a\",\"a\",\"a\",\"a\",\"a\"],"
			+ "\"after\":[\"a\",\"a\",\"a\",\"a\",\"a\"],\"teams\":{\"wizard\":[]}}";

		assertFalse(RivalTable.load(gson, new StringReader(missing), species).available());
		assertFalse(RivalTable.load(gson, new StringReader("{not json"), species).available());
		assertFalse(RivalTable.load(gson, new StringReader(""), species).available());
		assertFalse(RivalTable.empty().available());
	}
}
