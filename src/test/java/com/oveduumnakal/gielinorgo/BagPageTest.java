/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The Bag page: what it lists, what a click does, and that everything sits inside the page at every size. */
public class BagPageTest
{
	private static final int[][] CANVASES = {{535, 348}, {765, 497}, {1344, 874}};

	private static PanelState state(int eggs, boolean everyJar)
	{
		Map<JarType, Integer> jars = new EnumMap<>(JarType.class);
		jars.put(JarType.BRONZE, 12);
		jars.put(JarType.SLASH, 2);
		if (everyJar)
			for (JarType type : JarType.values())
				jars.put(type, 1);

		Map<ItemType, Integer> items = new EnumMap<>(ItemType.class);
		items.put(ItemType.SMALL_SALVE, 3);
		List<EggView> list = new ArrayList<>();
		for (int i = 0; i < eggs; i++)
			list.add(new EggView(Rarity.values()[i % 5], 100 * i, 500 + 100 * i));

		return new PanelState(7, 0.4, 5, jars, JarType.SLASH, 40, Collections.emptyList(), Collections.emptyList(),
			Collections.emptyList(), 0, 0, list, Collections.emptyList(), TodayView.NONE, null, items);
	}

	private static PartyRig bag(PanelState state)
	{
		PartyRig rig = new PartyRig();
		rig.publish(state, true);
		rig.open();
		rig.shell.showTab(WindowTab.BAG, rig.now);
		rig.paint();
		return rig;
	}

	@Test
	public void theJarsShownAreTheTiersAndTheSpecialtyJarsOwnedOrSelected()
	{
		PanelState state = state(0, false);

		assertEquals(Arrays.asList(JarType.BRONZE, JarType.IRON, JarType.RUNE, JarType.DRAGON, JarType.SLASH),
			BagText.jarsShown(state));
	}

	@Test
	public void noCountIsCutToAnEllipsisAtAnySize()
	{
		for (int[] canvas : CANVASES)
			for (int count : new int[] {9, 99, 999, 9999})
			{
				Map<JarType, Integer> jars = new EnumMap<>(JarType.class);
				for (JarType type : JarType.values())
					jars.put(type, count);

				Map<ItemType, Integer> items = new EnumMap<>(ItemType.class);
				for (ItemType type : ItemType.values())
					items.put(type, count);

				PartyRig rig = new PartyRig();
				rig.width = canvas[0];
				rig.height = canvas[1];
				rig.publish(new PanelState(7, 0.4, 5, jars, JarType.SLASH, 40, Collections.emptyList(),
					Collections.emptyList(), Collections.emptyList(), 0, 0, Collections.emptyList(),
					Collections.emptyList(), TodayView.NONE, null, items), true);
				rig.open();
				rig.shell.showTab(WindowTab.BAG, rig.now);
				rig.drawn.clear();
				rig.paint();

				long whole = rig.drawn.stream()
					.filter(d -> d.text.equals(String.valueOf(count)))
					.count();
				assertTrue(canvas[0] + " count " + count, whole >= JarType.values().length + ItemType.values().length);
			}
	}

	@Test
	public void clickingAJarSelectsIt()
	{
		PartyRig rig = bag(state(0, false));

		rig.click(BagPainter.jarId(1));

		assertEquals(Collections.singletonList("selectJar IRON"), rig.calls);
	}

	@Test
	public void theTitleChangesOnlyWhenMoreThanOneIsEarned()
	{
		PartyRig rig = bag(state(0, false));

		assertFalse(rig.hit(BagPainter.ID_TITLE).enabled());
		rig.click(BagPainter.ID_TITLE);

		assertTrue(rig.calls.isEmpty());
	}

	@Test
	public void theTooltipsSayWhatEachThingDoes()
	{
		PartyRig rig = new PartyRig();
		rig.width = 765;
		rig.height = 497;
		rig.publish(state(2, false), true);
		rig.open();
		rig.shell.showTab(WindowTab.BAG, rig.now);
		rig.paint();

		assertTrue(tip(rig, BagPainter.jarId(0)).contains("x1.0 catch chance"));
		assertTrue(tip(rig, BagPainter.jarId(4)).contains("catch chance on"));
		assertTrue(tip(rig, "bag:item:0").contains("Restores 25% health"));
		assertTrue(tip(rig, "bag:egg:1").contains("steps to go"));
		assertTrue(tip(rig, "bag:steps").contains((GoGame.TILES_PER_JAR - 40) + " more tiles"));
	}

	@Test
	public void everyThingSitsInsideThePageAndNoCellsOverlapAtAnySize()
	{
		StringBuilder problems = new StringBuilder();
		for (int[] canvas : CANVASES)
			for (int eggs : new int[] {0, 3, 12})
			{
				PartyRig rig = new PartyRig();
				rig.width = canvas[0];
				rig.height = canvas[1];
				rig.publish(state(eggs, true), true);
				rig.open();
				rig.shell.showTab(WindowTab.BAG, rig.now);
				rig.drawn.clear();
				rig.paint();
				Rectangle body = WindowLayout.of(rig.width, rig.height).body;
				for (PartyRig.Drawn d : rig.drawn)
				{
					if (d.width > d.box.width)
						note(problems, canvas[0], " too wide ", d);

					if (d.height > d.box.height + 1)
						note(problems, canvas[0], " too tall ", d);

					if (!body.contains(d.box))
						note(problems, canvas[0], " outside ", d);
				}

				List<Hit> hits = new ArrayList<>(rig.shell.hits());
				hits.removeIf(h -> !h.id().startsWith("bag:"));
				for (Hit a : hits)
				{
					if (!body.contains(a.bounds()))
						note(problems, canvas[0], " hit outside ", a.id());

					for (Hit b : hits)
						if (a != b && a.bounds().intersects(b.bounds()) && !overlapsByDesign(a, b))
							note(problems, canvas[0], a.id() + " overlaps ", b.id());
				}
			}

		assertEquals(problems.toString(), "", problems.toString());
	}

	private static String tip(PartyRig rig, String id)
	{
		return rig.hit(id).tip();
	}

	private static void note(StringBuilder problems, int canvas, String what, Object subject)
	{
		problems.append(canvas + " " + what + subject + "\n");
	}

	private static boolean overlapsByDesign(Hit a, Hit b)
	{
		Hit one = a.id().equals("bag:steps") || a.id().equals("bag:level") ? a : b;
		return one.id().equals("bag:steps") || one.id().equals("bag:level") || one.id().equals(BagPainter.ID_TITLE);
	}

	private static PartyRig bagWith(PanelState state)
	{
		PartyRig rig = new PartyRig();
		rig.width = 1344;
		rig.height = 874;
		rig.publish(state, true);
		rig.open();
		rig.shell.showTab(WindowTab.BAG, rig.now);
		rig.paint();
		return rig;
	}

	private static PanelState pinnedEgg(PanelState base, int place, int trackerLines)
	{
		List<EggView> eggs = new ArrayList<>(base.eggs());
		eggs.set(place, eggs.get(place).withPinned(true));
		List<TrackerView.Line> list = new ArrayList<>();
		for (int i = 0; i < trackerLines; i++)
			list.add(new TrackerView.Line(TrackerView.Kind.EGG, "egg:" + i, "Rare egg", "5 steps", 0.5, Rarity.RARE,
				false));

		return new PanelState(7, 0.4, 5, new EnumMap<>(JarType.class), JarType.SLASH, 40, Collections.emptyList(),
			Collections.emptyList(), Collections.emptyList(), 0, 0, eggs, Collections.emptyList(), TodayView.NONE,
			null, new EnumMap<>(ItemType.class)).withTracker(new TrackerView(list));
	}

	@Test
	public void everyEggRowHasAFocusablePinButton()
	{
		PartyRig rig = bagWith(state(3, false));

		for (int i = 0; i < 3; i++)
		{
			Hit hit = rig.hit("bag:pin:" + i);
			assertEquals(Hit.Kind.BUTTON, hit.kind());
			assertTrue(hit.order() >= WindowShell.PAGE_ORDER);
			assertTrue(hit.enabled());
			assertEquals("Pin to the tracker", hit.tip());
		}
	}

	@Test
	public void pressingAnEggsPinAsksTheGameToPinIt()
	{
		PartyRig rig = bagWith(state(3, false));

		rig.click("bag:pin:1");

		assertEquals(Collections.singletonList("pinEgg EGG#1 true"), rig.calls);
	}

	@Test
	public void aPinnedEggOffersUnpin()
	{
		PartyRig rig = bagWith(pinnedEgg(state(3, false), 2, 5));

		assertEquals("Unpin from the tracker", rig.hit("bag:pin:2").tip());
		assertTrue(rig.hit("bag:pin:2").enabled());
		rig.click("bag:pin:2");

		assertEquals(Collections.singletonList("pinEgg EGG#2 false"), rig.calls);
	}

	@Test
	public void aFullTrackerDisablesTheOtherEggPins()
	{
		PartyRig rig = bagWith(pinnedEgg(state(3, false), 2, 5));

		assertFalse(rig.hit("bag:pin:0").enabled());
		assertEquals("Tracker is full (5)", rig.hit("bag:pin:0").tip());
		rig.click("bag:pin:0");
		assertTrue(rig.calls.isEmpty());
	}
}
