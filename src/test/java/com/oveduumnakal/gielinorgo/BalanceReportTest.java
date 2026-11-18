/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The harness behind the balance review of issue 220. {@link #writeTheReport} does nothing in a normal build; with the
 * {@code BALANCE_REPORT} environment variable set it measures the real game data and writes
 * {@code build/balance-report.md}. The other tests run every build and cover the harness itself on a tiny table.
 */
public class BalanceReportTest
{
	private static final int FIGHTS = 2_000;

	/** Writes the report when asked: run {@code BALANCE_REPORT=1 ./gradlew test --no-daemon -q} on this class. */
	@Test
	public void writeTheReport() throws IOException
	{
		Assume.assumeTrue(System.getenv("BALANCE_REPORT") != null);
		String report = BalanceReport.build(BalanceData.load(FIGHTS));
		Path path = Paths.get("build", "balance-report.md");
		Files.createDirectories(path.getParent());
		Files.write(path, report.getBytes(StandardCharsets.UTF_8));
		System.out.println("Balance report written to " + path.toAbsolutePath());
		assertTrue(report.length() > 1000);
	}

	private static BalanceData tiny()
	{
		SpeciesTable table = new SpeciesTable(Arrays.asList(
			new Species("Rat", 2, CombatType.CRUSH),
			new Species("Wolf", 40, CombatType.SLASH),
			new Species("Troll", 100, CombatType.CRUSH),
			new Species("Ogre", 200, CombatType.MAGIC),
			new Species("Dragon", 320, CombatType.STAB, Trait.FIERY)));
		return new BalanceData(table, EvolutionTable.empty(), MoveTable.empty(), SkillingTable.empty(), 5);
	}

	@Test
	public void theReportBuilderWritesEverySectionForATinyTable()
	{
		String report = BalanceReport.build(tiny());
		assertTrue(report.startsWith("# Gielinor Go balance report"));
		for (int section = 1; section <= 8; section++)
			assertTrue("section " + section, report.contains("\n## " + section + ". "));
	}

	@Test
	public void theReportIsTheSameTwice()
	{
		assertEquals(BalanceReport.build(tiny()), BalanceReport.build(tiny()));
	}

	@Test
	public void theReportReadsTheRealConstants()
	{
		String report = BalanceReport.build(tiny());
		assertTrue(report.contains("`GoGame.TILES_PER_JAR`"));
		assertTrue(report.contains(BalanceTable.cell(Rarity.LEGENDARY.eggTiles())));
		assertTrue(report.contains("`CatchRules.WEAKENED_BONUS` " + CatchRules.WEAKENED_BONUS));
	}

	@Test
	public void theWaitFormulaAgreesWithTheRealTracker()
	{
		double chance = 0.2;
		for (int monsters : new int[] {1, 3})
		{
			double formula = BalanceSpawn.minutesToWild(chance, monsters);
			double played = BalanceSpawn.simulatedMinutes(chance, monsters, 600);
			assertEquals("monsters " + monsters, formula, played, formula * 0.2 + 0.2);
		}
	}

	@Test
	public void aTableWritesItsRows()
	{
		BalanceTable table = new BalanceTable("A", "B").row("x", 1_234).row("y", 0.5);
		assertEquals(2, table.size());
		assertTrue(table.toString().contains("| x | 1,234 |"));
		assertTrue(table.toString().contains("| y | 0.500 |"));
		assertEquals("n/a", BalanceTable.number(Double.POSITIVE_INFINITY));
	}
}
