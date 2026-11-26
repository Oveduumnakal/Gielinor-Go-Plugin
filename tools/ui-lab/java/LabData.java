package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** A made-up save that fills every page, so each part of the window has something to draw. */
final class LabData
{
	private static final String[] NAMES = {"Goblin", "Cow", "Imp", "Giant rat", "Man", "Guard", "Chicken", "Skeleton",
		"Zombie", "Giant spider", "Scorpion", "Dark wizard", "Hill giant", "Moss giant", "Lesser demon", "Green dragon",
		"Unicorn", "Black bear", "Wolf", "Ghost", "Bruise Blamish Snail", "Ice warrior", "Hobgoblin", "Dwarf",
		"Barbarian", "Mugger", "Highwayman", "Jogre", "Cockatrice", "Basilisk"};

	private LabData()
	{
	}

	private static List<CreatureView> stored()
	{
		List<CreatureView> stored = new ArrayList<>();
		CombatType[] types = CombatType.values();
		Rarity[] rarities = Rarity.values();
		for (int i = 0; i < 42; i++)
		{
			stored.add(new CreatureView(NAMES[i % NAMES.length], 3 + i * 7 % 60, i % 10 / 10.0, 1.0 - i % 5 * 0.2,
				rarities[i % rarities.length], types[i % types.length], i % 11 == 3, i == 4 ? "Sir Clucks" : null));
		}

		return stored;
	}

	private static List<LogEntry> log()
	{
		List<LogEntry> log = new ArrayList<>();
		CombatType[] types = CombatType.values();
		Rarity[] rarities = Rarity.values();
		List<LogEntry.Stage> line = Arrays.asList(new LogEntry.Stage("Imp", true),
			new LogEntry.Stage("Lesser demon", true), new LogEntry.Stage("Greater demon", false));
		for (int i = 0; i < NAMES.length; i++)
		{
			Rarity rarity = rarities[i % rarities.length];
			CombatType type = types[i % types.length];
			if (i % 3 == 0)
			{
				log.add(new LogEntry(NAMES[i], 2 + i * 9, rarity, type, 1 + i % 6, true, i % 9 == 0, i == 3,
					"It looks like it has been in a few fights.", i == 0 ? line : Collections.emptyList()));
			}
			else if (i % 3 == 1)
			{
				log.add(new LogEntry(NAMES[i], 2 + i * 9, rarity, type, 0, true, false, false,
					"Seen once, from a safe distance.", Collections.emptyList()));
			}
			else
			{
				log.add(new LogEntry(NAMES[i], 2 + i * 9, rarity, type, 0, false, false));
			}
		}

		return log;
	}

	private static TaskView tasks()
	{
		return new TaskView(Arrays.asList(
			new TaskView.Row("Catch 3 Ranged creatures", 1, 3, false, "2 Iron jars", false),
			new TaskView.Row("Win 2 battles against champions", 2, 2, true, "1 Revive draught", false),
			new TaskView.Row("Walk 4,000 tiles with a creature in your team", 1200, 4000, false, "Egg", false),
			new TaskView.Row("Weekly: win 10 battles", 3, 10, false, "Rare egg", true)), 3, 4, 3_600_000L,
			86_400_000L);
	}

	private static MilestoneView milestones()
	{
		List<MilestoneView.Row> rows = new ArrayList<>();
		for (int i = 0; i < 6; i++)
		{
			int goal = 25 * (i + 1);
			rows.add(new MilestoneView.Row("Log " + goal + " species", Math.min(goal, 80), goal, "5 Rune jars", i < 3,
				false));
		}

		return new MilestoneView("Log 100 species", 80, 100, rows);
	}

	private static TodayView today()
	{
		return new TodayView(Arrays.asList(new TodayView.Pick("Giant rat", Rarity.COMMON),
			new TodayView.Pick("Goblin", Rarity.UNCOMMON), new TodayView.Pick("Unicorn", Rarity.RARE),
			new TodayView.Pick("Lesser demon", Rarity.EPIC), new TodayView.Pick("Green dragon", Rarity.LEGENDARY)),
			"Dragon weekend: x2 wild, x2 gilded", true, 312, true);
	}

	private static List<ChampionView> champions()
	{
		String team = "Goblin 4, Cow 5, Giant rat 7";
		return Arrays.asList(new ChampionView("a", "Lumbridge", team, "3 Iron jars", true, false),
			new ChampionView("b", "Varrock", team, "3 Steel jars", false, true),
			new ChampionView("c", "Falador", team, "3 Mithril jars", false, false),
			new ChampionView("d", "Ardougne", team, "3 Rune jars", false, false));
	}

	/** @return the save the window scenes show */
	static PanelState state()
	{
		Map<JarType, Integer> jars = new EnumMap<>(JarType.class);
		for (JarType jar : JarType.values())
			jars.put(jar, jar.ordinal() % 4 == 3 ? 0 : 3 + jar.ordinal() * 5 % 23);

		Map<ItemType, Integer> items = new EnumMap<>(ItemType.class);
		for (ItemType item : ItemType.values())
			items.put(item, 1 + item.ordinal() * 3);

		List<EggView> eggs = Arrays.asList(new EggView(Rarity.COMMON, 120, 1000), new EggView(Rarity.RARE, 2200, 4000),
			new EggView(Rarity.LEGENDARY, 9000, 10000));
		return new PanelState(7, 0.4, 25, jars, JarType.BRONZE, 40, PartyRig.team(6), stored(), log(), 10, 20, eggs,
			champions(), today(), tasks(), items).withGoals(milestones(), EliteView.NONE);
	}
}
