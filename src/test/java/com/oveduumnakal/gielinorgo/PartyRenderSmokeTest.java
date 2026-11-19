/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Paints the Party and Summary pages headless into a {@code BufferedImage}, in both themes, in every motion mode, at
 * several canvas sizes including fixed mode's 765 x 503. It fails on any exception, on a string wider than its box or
 * taller than the box it is drawn in, on a box that leaves the page, on a short string that was cut to an ellipsis, and
 * on a hit that leaves the window.
 */
public class PartyRenderSmokeTest
{
	private static final int[][] CANVASES = {{765, 503}, {1024, 768}, {1920, 1080}, {800, 520}, {640, 700}};
	private static final double[] FRACTIONS = {WindowFit.DEFAULT_FRACTION, 1.0};

	private static CreatureView variety(int i)
	{
		Move spark = new Move("spark", "Spark", CombatType.MAGIC, 1.2, 0.9);
		String[] species = {"Frenzied ice troll female", "Bloodthirsty banshee", "Man", "Cow", "Gelatinnoth Mother",
			"Imp"};
		String[] nicknames = {"WWWWWWWWWWWW", null, "Bob", null, "Mooooooooooo", "iiiiiiiiiiii"};
		double[] health = {1.0, 0.18, 0.62, 1.0, 0.05, 0.99};
		boolean gilded = i == 0 || i == 3;
		StatRolls rolls = i == 1 ? new StatRolls(5, 5, 5, 5, Temperament.STOUT) : StatRolls.AVERAGE;
		List<Move> pending = i == 2 ? Collections.singletonList(spark) : Collections.emptyList();
		String waiting = i == 3 ? "Evolves at night" : null;
		return new CreatureView(species[i], 5 + i * 17, 0.3 + i * 0.1, health[i], Rarity.values()[i % 5],
			CombatType.values()[i % 5], gilded, nicknames[i], rolls, 0, Arrays.asList(PartyRig.SLASH, PartyRig.BASH,
				spark), pending, i, waiting)
			.withSpecies(new Species(species[i], 50, CombatType.values()[i % 5], Trait.UNHOLY, Trait.SLAYER,
				Trait.FIERY))
			.withGender(i % 2 == 0 ? Gender.MALE : Gender.FEMALE)
			.withDetails("A creature with a very long description that goes on and on, much too long to fit in the"
				+ " space under the stats on a small window, and so it must be cut with an ellipsis.",
				"Grows into Lesser demon (20), Greater demon (40)")
			.withFainted(i == 4);
	}

	private static PanelState richState(int size)
	{
		List<CreatureView> team = new ArrayList<>();
		for (int i = 0; i < size; i++)
			team.add(variety(i));

		TaskView tasks = new TaskView(Arrays.asList(
			new TaskView.Row("Catch 3 Ranged creatures", 1, 3, false, "2 Iron jars", false),
			new TaskView.Row("Win 2 battles against champions", 2, 2, true, "1 Revive draught", false),
			new TaskView.Row("Walk 4,000 tiles with a creature in your team", 1200, 4000, false, "Egg", false),
			new TaskView.Row("Weekly: win 10 battles", 3, 10, false, "Rare egg", true)), 3, 4, 3_600_000L,
			86_400_000L);
		List<EggView> eggs = Arrays.asList(new EggView(Rarity.RARE, 120, 400), new EggView(Rarity.COMMON, 0, 200),
			new EggView(Rarity.LEGENDARY, 900, 1000, "Vorkath"));
		Map<ItemType, Integer> items = new EnumMap<>(ItemType.class);
		items.put(ItemType.SMALL_SALVE, 3);
		items.put(ItemType.REVIVE, 1);
		return new PanelState(7, 0.2, 4, new EnumMap<>(JarType.class), JarType.BRONZE, 0, team,
			Collections.emptyList(), Collections.emptyList(), 0, 0, eggs, Collections.emptyList(), TodayView.NONE,
			tasks, items).withResetSummary("6 creatures, 3 eggs, 4 battles won");
	}

	private static boolean fits(PartyRig rig, Rectangle body)
	{
		for (PartyRig.Drawn d : rig.drawn)
		{
			if (d.width > d.box.width || d.height > d.box.height + 1 || !body.contains(d.box))
				return false;
		}

		return true;
	}

	private String problems(PartyRig rig, String where)
	{
		Dimension size = new Dimension(rig.width, rig.height);
		Rectangle body = WindowLayout.of(size.width, size.height).body;
		StringBuilder text = new StringBuilder();
		for (PartyRig.Drawn d : rig.drawn)
		{
			if (d.width > d.box.width)
				text.append(where).append(" too wide ")
					.append(d).append('\n');

			if (d.height > d.box.height + 1)
				text.append(where).append(" too tall ")
					.append(d).append('\n');

			if (!body.contains(d.box))
				text.append(where).append(" leaves the page ")
					.append(d).append(" body ").append(body)
					.append('\n');
		}

		for (Hit hit : rig.shell.hits())
		{
			if (!new Rectangle(0, 0, rig.width, rig.height).contains(hit.bounds()))
				text.append(where).append(" hit outside ")
					.append(hit.id()).append('\n');
		}

		return text.toString();
	}

	private static PartyRig rig(WindowTheme theme, MotionMode mode, int[] canvas, double fraction)
	{
		PartyRig rig = new PartyRig();
		rig.theme = theme;
		rig.motion = mode;
		Dimension size = WindowFit.sizeFor(fraction, canvas[0], canvas[1]);
		rig.width = size.width;
		rig.height = size.height;
		return rig;
	}

	@Test
	public void thePartyPageFitsItsTextAtEveryCanvasSizeInBothThemesAndEveryMotionMode()
	{
		StringBuilder problems = new StringBuilder();
		for (WindowTheme theme : WindowTheme.values())
			for (MotionMode mode : MotionMode.values())
				for (int[] canvas : CANVASES)
					for (double fraction : FRACTIONS)
						for (int team : new int[] {6, 3, 1})
						{
							PartyRig rig = rig(theme, mode, canvas, fraction);
							rig.publish(richState(team), true);
							rig.open();
							rig.now += 1_000;
							rig.drawn.clear();
							rig.paint();
							String where = theme + " " + mode + " " + rig.width + "x" + rig.height + " team " + team;
							assertFalse(where, rig.drawn.isEmpty());
							problems.append(problems(rig, where));
						}

		assertEquals(problems.toString(), "", problems.toString());
	}

	@Test
	public void theSummaryPageFitsItsTextAtEveryCanvasSizeInBothThemesForEveryKindOfCreature()
	{
		StringBuilder problems = new StringBuilder();
		for (WindowTheme theme : WindowTheme.values())
			for (MotionMode mode : new MotionMode[] {MotionMode.OFF, MotionMode.FULL})
				for (int[] canvas : CANVASES)
					for (double fraction : FRACTIONS)
						for (int creature = 0; creature < 6; creature++)
						{
							PartyRig rig = rig(theme, mode, canvas, fraction);
							rig.publish(richState(6), true);
							rig.open();
							rig.click(WindowPages.rowId(creature));
							rig.now += 2_000;
							rig.drawn.clear();
							rig.paint();
							String where = theme + " " + mode + " " + rig.width + "x" + rig.height + " creature "
								+ creature;
							assertEquals(where, PageState.Page.SUMMARY, rig.pages.pageState().page());
							assertFalse(where, rig.drawn.isEmpty());
							problems.append(problems(rig, where));
						}

		assertEquals(problems.toString(), "", problems.toString());
	}

	private static PanelState storedState(int size)
	{
		List<CreatureView> storage = new ArrayList<>();
		for (int i = 0; i < size; i++)
			storage.add(variety(i).withStats(1234 + i, 99 + i, 88, 777).withCaught(1_794_000_000_000L + i));

		Map<ItemType, Integer> items = new EnumMap<>(ItemType.class);
		items.put(ItemType.SMALL_SALVE, 3);
		return new PanelState(7, 0.2, 4, new EnumMap<>(JarType.class), JarType.BRONZE, 0, PartyRig.team(2), storage,
			Collections.emptyList(), 0, 0, Collections.emptyList(), Collections.emptyList(), TodayView.NONE,
			TaskView.NONE, items);
	}

	@Test
	public void theStoredSummaryFitsItsTextAtEveryCanvasSizeInBothThemesForEveryKindOfCreature()
	{
		StringBuilder problems = new StringBuilder();
		for (WindowTheme theme : WindowTheme.values())
			for (MotionMode mode : new MotionMode[] {MotionMode.OFF, MotionMode.FULL})
				for (int[] canvas : CANVASES)
					for (double fraction : FRACTIONS)
						for (int creature = 0; creature < 6; creature++)
						{
							PartyRig rig = rig(theme, mode, canvas, fraction);
							rig.publish(storedState(6), true);
							rig.open();
							rig.shell.showTab(WindowTab.STORAGE, rig.now);
							rig.paint();
							if (!rig.ids().contains(StoragePage.rowId(creature)))
								continue;

							rig.click(StoragePage.rowId(creature), MouseEvent.BUTTON3);
							rig.key(KeyEvent.VK_DOWN);
							rig.key(KeyEvent.VK_ENTER);
							rig.now += 2_000;
							rig.drawn.clear();
							rig.paint();
							String where = theme + " " + mode + " " + rig.width + "x" + rig.height + " stored "
								+ creature;
							assertEquals(where, PageState.Page.SUMMARY, rig.pages.storedPageState().page());
							assertFalse(where, rig.drawn.isEmpty());
							List<String> texts = new ArrayList<>();
							for (PartyRig.Drawn d : rig.drawn)
								texts.add(d.text);

							if (!texts.contains(String.valueOf(1234 + creature)))
								problems.append(where).append(" missing the health number in ").append(texts)
									.append('\n');

							problems.append(problems(rig, where));
						}

		assertEquals(problems.toString(), "", problems.toString());
	}

	@Test
	public void theCaughtLineIsDrawnWhereTheIdentityBoxHasRoomForIt()
	{
		int seen = 0;
		for (int creature = 0; creature < 4; creature++)
		{
			PartyRig rig = rig(WindowTheme.RUNELITE_DARK, MotionMode.OFF, CANVASES[CANVASES.length - 1], 1.0);
			rig.publish(storedState(6), true);
			rig.open();
			rig.shell.showTab(WindowTab.STORAGE, rig.now);
			rig.paint();
			rig.click(StoragePage.rowId(creature), MouseEvent.BUTTON3);
			rig.key(KeyEvent.VK_DOWN);
			rig.key(KeyEvent.VK_ENTER);
			rig.drawn.clear();
			rig.paint();
			for (PartyRig.Drawn d : rig.drawn)
			{
				if (d.text.startsWith("Caught "))
					seen++;
			}
		}

		assertTrue("the caught line never fit at the largest canvas", seen > 0);
	}

	@Test
	public void everyStoredButtonLabelFitsItsBox()
	{
		StringBuilder problems = new StringBuilder();
		for (int[] canvas : CANVASES)
			for (double fraction : FRACTIONS)
			{
				Dimension size = WindowFit.sizeFor(fraction, canvas[0], canvas[1]);
				WindowLayout window = WindowLayout.of(size.width, size.height);
				WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_DARK, window.scale);
				int line = WidgetKit.metrics(style, WindowFonts.Face.REGULAR).getHeight();
				int small = WidgetKit.metrics(style, WindowFonts.Face.SMALL).getHeight();
				SummaryLayout layout = SummaryLayout.of(window.body, style.metrics(), line, small,
					SummaryLayout.STORED_BUTTONS);
				int chip = 2 * style.metrics().chipSlant;
				for (int i = 0; i < SummaryLayout.STORED_BUTTONS.length; i++)
				{
					if (!labelFits(style, SummaryLayout.STORED_BUTTONS[i], layout.buttons[i].width - chip))
						problems.append(window.scale).append(" button ")
							.append(SummaryLayout.STORED_BUTTONS[i]).append('\n');
				}
			}

		assertEquals(problems.toString(), "", problems.toString());
	}

	@Test
	public void theEmptyAndFirstRunPagesFitToo()
	{
		StringBuilder problems = new StringBuilder();
		List<StarterView> offer = Arrays.asList(
			new StarterView(variety(0), "Grows into Lesser demon (20), Greater demon (40)"),
			new StarterView(variety(1), "Does not evolve"), new StarterView(variety(2), "Grows into Hobgoblin (20)"));
		for (WindowTheme theme : WindowTheme.values())
			for (int[] canvas : CANVASES)
				for (double fraction : FRACTIONS)
					for (boolean starters : new boolean[] {true, false})
					{
						PartyRig rig = rig(theme, MotionMode.OFF, canvas, fraction);
						PanelState empty = PartyRig.state(Collections.emptyList());
						rig.publish(starters ? empty.withStarters(offer) : empty, true);
						rig.open();
						rig.drawn.clear();
						rig.paint();
						String where = theme + " " + rig.width + "x" + rig.height + " starters " + starters;
						assertFalse(where, rig.drawn.isEmpty());
						problems.append(problems(rig, where));
					}

		assertEquals(problems.toString(), "", problems.toString());
	}

	@Test
	public void shortStringsAreNeverCutToAnEllipsisAtAnySize()
	{
		StringBuilder problems = new StringBuilder();
		for (WindowTheme theme : WindowTheme.values())
			for (int[] canvas : CANVASES)
				for (double fraction : FRACTIONS)
				{
					PartyRig rig = rig(theme, MotionMode.OFF, canvas, fraction);
					rig.publish(richState(6), true);
					rig.open();
					rig.drawn.clear();
					rig.paint();
					String where = theme + " " + rig.width + "x" + rig.height;
					List<String> texts = new ArrayList<>();
					for (PartyRig.Drawn d : rig.drawn)
						texts.add(d.text);

					for (String must : new String[] {"Lv 5", "Lv 22", "Lv 39", "Lv 56", "Lv 73", "Lv 90", "100%",
						"Eggs  3 / 3", "Tasks", "Lead", "Resting", "Fainted", "Waiting"})
					{
						if (!texts.contains(must))
							problems.append(where).append(" missing '").append(must).append("' in ").append(texts)
								.append('\n');
					}
				}

		assertEquals(problems.toString(), "", problems.toString());
	}

	@Test
	public void theSummaryNeverCutsItsShortWords()
	{
		StringBuilder problems = new StringBuilder();
		for (WindowTheme theme : WindowTheme.values())
			for (int[] canvas : CANVASES)
				for (double fraction : FRACTIONS)
				{
					PartyRig rig = rig(theme, MotionMode.OFF, canvas, fraction);
					rig.publish(richState(6), true);
					rig.open();
					rig.click("row:2");
					rig.drawn.clear();
					rig.paint();
					String where = theme + " " + rig.width + "x" + rig.height;
					List<String> texts = new ArrayList<>();
					for (PartyRig.Drawn d : rig.drawn)
						texts.add(d.text);

					for (String must : new String[] {"Bob", "Man", "LV. 39", "Stats", "HP", "XP", "Attack",
						"Def", "Speed", "Nature", "Steady", "Description", "Moves", "Slash", "Bash", "Spark",
						"New move: Spark"})
					{
						if (!texts.contains(must))
							problems.append(where).append(" missing '").append(must).append("' in ").append(texts)
								.append('\n');
					}
				}

		assertEquals(problems.toString(), "", problems.toString());
	}

	@Test
	public void everyButtonLabelOfThePagesFitsItsBox()
	{
		StringBuilder problems = new StringBuilder();
		for (int[] canvas : CANVASES)
			for (double fraction : FRACTIONS)
			{
				Dimension size = WindowFit.sizeFor(fraction, canvas[0], canvas[1]);
				WindowLayout window = WindowLayout.of(size.width, size.height);
				WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_DARK, window.scale);
				int line = WidgetKit.metrics(style, WindowFonts.Face.REGULAR).getHeight();
				int small = WidgetKit.metrics(style, WindowFonts.Face.SMALL).getHeight();
				SummaryLayout layout = SummaryLayout.of(window.body, style.metrics(), line, small);
				int chip = 2 * style.metrics().chipSlant;
				for (int i = 0; i < SummaryLayout.BUTTONS.length; i++)
				{
					if (!labelFits(style, SummaryLayout.BUTTONS[i], layout.buttons[i].width - chip))
						problems.append(window.scale).append(" button ")
							.append(SummaryLayout.BUTTONS[i]).append('\n');
				}

				PartyLayout party = PartyLayout.of(window.body, style.metrics(), window.scale);
				if (!labelFits(style, PartyPainter.MORE, party.moreButton.width - chip))
					problems.append(window.scale).append(" More\n");

				if (!labelFits(style, PartyPainter.PIN_PARTY, party.pinButton.width - chip))
					problems.append(window.scale).append(" Pin Party\n");
			}

		assertEquals(problems.toString(), "", problems.toString());
	}

	private static boolean labelFits(WindowStyle style, String text, int room)
	{
		Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
		try
		{
			FontMetrics fm = g.getFontMetrics(style.fitted(g, WindowFonts.Face.BOLD, WindowFonts.BODY_UNITS, text,
				room));
			return fm.stringWidth(text) <= room;
		}
		finally
		{
			g.dispose();
		}
	}

	@Test
	public void theSummaryLayoutBoxesStayInsideThePageAndDoNotOverlap()
	{
		for (int[] canvas : CANVASES)
			for (double fraction : FRACTIONS)
			{
				Dimension size = WindowFit.sizeFor(fraction, canvas[0], canvas[1]);
				WindowLayout window = WindowLayout.of(size.width, size.height);
				WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_DARK, window.scale);
				int line = WidgetKit.metrics(style, WindowFonts.Face.REGULAR).getHeight();
				int small = WidgetKit.metrics(style, WindowFonts.Face.SMALL).getHeight();
				SummaryLayout layout = SummaryLayout.of(window.body, style.metrics(), line, small);
				String where = size.width + "x" + size.height;
				List<Rectangle> parts = new ArrayList<>(Arrays.asList(layout.header, layout.chips,
					layout.portrait, layout.stats, layout.nature, layout.moves, layout.bond));
				if (layout.description.height > 0)
					parts.add(layout.description);

				parts.addAll(Arrays.asList(layout.buttons));
				for (Rectangle part : parts)
					assertTrue(where + " " + part, window.body.contains(part));

				for (int i = 0; i < parts.size(); i++)
					for (int j = i + 1; j < parts.size(); j++)
						assertFalse(where + " " + parts.get(i) + " overlaps " + parts.get(j),
							parts.get(i).intersects(parts.get(j)));

				assertTrue(where, layout.left.contains(layout.portrait));
				assertTrue(where, layout.left.contains(layout.header));
				assertTrue(where, layout.left.contains(layout.chips));
				assertTrue(where, layout.right.contains(layout.moves));
			}
	}

	@Test
	public void theThreeStagesOfTheMotionAndThePagesPaintInFullMode()
	{
		PartyRig rig = rig(WindowTheme.RUNELITE_DARK, MotionMode.FULL, new int[] {765, 503},
			WindowFit.DEFAULT_FRACTION);
		rig.publish(richState(6), true);
		rig.open();
		rig.now = 1_000;
		BufferedImage start = rig.paint();
		rig.now = 1_000 + WindowTimes.ROW_IN + 6 * WindowTimes.ROW_STAGGER + 300;
		BufferedImage settled = rig.paint();

		assertNotEquals(pixels(start), pixels(settled));
		rig.click("row:1");
		rig.now += 50;
		BufferedImage growing = rig.paint();
		rig.now += WindowTimes.SUMMARY_GROW + 6 * WindowTimes.SUMMARY_STAGGER + 400;
		BufferedImage done = rig.paint();

		assertNotEquals(pixels(growing), pixels(done));
		assertTrue(problems(rig, "full").isEmpty() || !rig.drawn.isEmpty());
	}

	@Test
	public void aMenuAndTheModalsPaintOverThePagesInBothThemes()
	{
		for (WindowTheme theme : WindowTheme.values())
		{
			PartyRig rig = rig(theme, MotionMode.OFF, new int[] {765, 503}, WindowFit.DEFAULT_FRACTION);
			rig.publish(richState(6), true);
			rig.open();
			BufferedImage plain = rig.paint();

			rig.click("row:1", java.awt.event.MouseEvent.BUTTON3);
			BufferedImage menu = rig.paint();
			assertNotEquals(theme.toString(), pixels(plain), pixels(menu));
			rig.key(KeyEvent.VK_ESCAPE);

			rig.click(WindowPages.ID_MORE);
			BufferedImage more = rig.paint();
			assertNotEquals(theme.toString(), pixels(plain), pixels(more));
			rig.key(KeyEvent.VK_ESCAPE);

			rig.click("row:2");
			rig.click(WindowPages.summaryButtonId(2));
			BufferedImage moves = rig.paint();
			assertEquals(WindowKeyRules.KeyMode.MODAL, rig.shell.keyMode());
			assertNotEquals(theme.toString(), pixels(menu), pixels(moves));
			rig.key(KeyEvent.VK_ESCAPE);

			rig.click(WindowPages.summaryButtonId(1));
			assertEquals(WindowKeyRules.KeyMode.FIELD, rig.shell.keyMode());
			assertFalse(theme.toString(), rig.shell.hits().isEmpty());
		}
	}

	@Test
	public void theRightColumnDrawsTheEggsAndTheFirstThreeTasks()
	{
		PartyRig rig = rig(WindowTheme.RUNELITE_DARK, MotionMode.OFF, new int[] {1024, 768},
			WindowSizePreset.MEDIUM.fraction());
		rig.publish(richState(6), true);
		rig.open();
		rig.drawn.clear();
		rig.paint();
		List<String> texts = new ArrayList<>();
		for (PartyRig.Drawn d : rig.drawn)
			texts.add(d.text);

		assertTrue(texts.toString(), texts.contains("Eggs  3 / 3"));
		assertTrue(texts.toString(), texts.contains("Rare egg  120 steps"));
		assertTrue(texts.toString(), texts.contains("Common egg  Ready"));
		assertTrue(texts.toString(), texts.contains("Catch 3 Ranged creatures"));
		assertTrue(texts.toString(), texts.contains("Done"));
		assertFalse(texts.toString(), texts.contains("Weekly: win 10 battles"));
		assertTrue(fits(rig, WindowLayout.of(rig.width, rig.height).body));
	}

	private static List<Integer> pixels(BufferedImage image)
	{
		List<Integer> list = new ArrayList<>();
		for (int y = 0; y < image.getHeight(); y += 2)
			for (int x = 0; x < image.getWidth(); x += 2)
				list.add(image.getRGB(x, y));

		return list;
	}
}
