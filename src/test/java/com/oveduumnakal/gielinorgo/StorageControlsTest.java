/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Where the Storage controls sit at every scale: inside the body, apart, with chips that never spill. */
public class StorageControlsTest
{
	private static final double[] SCALES = {0.67, 1.0, 1.75};

	private static Rectangle body(WindowStyle style)
	{
		return new Rectangle(20, 30, style.px(776), style.px(360));
	}

	private static FontMetrics font(WindowStyle style)
	{
		return WindowPages.metrics(style.font(WindowFonts.Face.BOLD, WindowFonts.BODY_UNITS));
	}

	private static StorageQuery chips(int count)
	{
		StorageQuery query = StorageQuery.NONE;
		StorageQuery.Facet[] facets = {StorageQuery.Facet.STYLE, StorageQuery.Facet.RARITY,
			StorageQuery.Facet.RATING, StorageQuery.Facet.GILDED, StorageQuery.Facet.NICKNAMED,
			StorageQuery.Facet.LEVEL_OVER, StorageQuery.Facet.LEVEL_UNDER};
		int[] values = {2, 2, 2, 0, 0, 20, 60};
		for (int i = 0; i < count; i++)
			query = query.cycled(facets[i % facets.length], values[i % facets.length] + i / facets.length);

		return query;
	}

	private static List<Rectangle> all(StorageControls.Layout layout, boolean selecting)
	{
		List<Rectangle> boxes = new ArrayList<>();
		boxes.add(layout.select());
		boxes.add(layout.filter());
		boxes.add(layout.sort());
		boxes.add(layout.heading());
		boxes.add(layout.strip());
		if (selecting)
		{
			boxes.add(layout.tickAll());
			boxes.add(layout.release());
		}

		return boxes;
	}

	@Test
	public void everyBoxLiesInsideTheBodyAtEveryScale()
	{
		for (double scale : SCALES)
		{
			WindowStyle style = WidgetKit.style(scale);
			for (boolean selecting : new boolean[] {false, true})
			{
				StorageControls.Layout layout = StorageControls.layout(body(style), style, font(style), selecting,
					false, chips(4));

				for (Rectangle box : all(layout, selecting))
					assertTrue(scale + " " + box, body(style).contains(box));

				for (StorageControls.Chip chip : layout.chips())
				{
					assertTrue(body(style).contains(chip.label()));
					assertTrue(body(style).contains(chip.remove()));
				}

				assertTrue(layout.listTop() < body(style).y + body(style).height);
			}
		}
	}

	@Test
	public void theButtonsDoNotOverlapEachOther()
	{
		for (double scale : SCALES)
		{
			WindowStyle style = WidgetKit.style(scale);
			StorageControls.Layout layout = StorageControls.layout(body(style), style, font(style), true, false,
				chips(2));
			List<Rectangle> line1 = new ArrayList<>();
			line1.add(layout.select());
			line1.add(layout.filter());
			line1.add(layout.sort());
			line1.add(layout.heading());
			List<Rectangle> line2 = new ArrayList<>();
			line2.add(layout.strip());
			line2.add(layout.tickAll());
			line2.add(layout.release());

			assertNoOverlap(scale + " line 1", line1);
			assertNoOverlap(scale + " line 2", line2);
			assertTrue("line 2 sits under line 1", layout.strip().y >= layout.select().y + layout.select().height);
		}
	}

	@Test
	public void theHeadingKeepsAtLeastAHundredUnitsWithAViewButtonToo()
	{
		for (double scale : SCALES)
		{
			WindowStyle style = WidgetKit.style(scale);
			for (boolean view : new boolean[] {false, true})
			{
				StorageControls.Layout layout = StorageControls.layout(body(style), style, font(style), false, view,
					StorageQuery.NONE);

				assertTrue(scale + " heading " + layout.heading().width,
					layout.heading().width >= style.px(100));
				assertEquals(view, !layout.view().isEmpty());
			}
		}
	}

	@Test
	public void aViewButtonSitsLeftOfSortWithoutOverlap()
	{
		WindowStyle style = WidgetKit.style(1.0);

		StorageControls.Layout layout = StorageControls.layout(body(style), style, font(style), false, true,
			StorageQuery.NONE);

		assertTrue(layout.view().x + layout.view().width <= layout.sort().x);
		assertTrue(layout.heading().x + layout.heading().width <= layout.view().x);
	}

	@Test
	public void chipsNeverReachTheSelectModeButtons()
	{
		for (double scale : SCALES)
		{
			WindowStyle style = WidgetKit.style(scale);
			for (int count : new int[] {1, 3, 7, 12})
			{
				StorageControls.Layout layout = StorageControls.layout(body(style), style, font(style), true, false,
					chips(count));
				Rectangle strip = layout.strip();

				assertTrue(scale + " strip " + strip, strip.x + strip.width <= layout.tickAll().x);
				for (StorageControls.Chip chip : layout.chips())
				{
					assertTrue(strip.contains(chip.label()));
					assertTrue(strip.contains(chip.remove()));
				}

				if (!layout.clear().isEmpty())
					assertTrue(strip.contains(layout.clear()));

				if (!layout.more().isEmpty())
					assertTrue(strip.contains(layout.more()));
			}
		}
	}

	@Test
	public void chipsDoNotOverlapEachOther()
	{
		WindowStyle style = WidgetKit.style(1.0);
		StorageControls.Layout layout = StorageControls.layout(body(style), style, font(style), false, false,
			chips(6));
		List<Rectangle> boxes = new ArrayList<>();
		for (StorageControls.Chip chip : layout.chips())
		{
			boxes.add(chip.label());
			boxes.add(chip.remove());
		}

		if (!layout.more().isEmpty())
			boxes.add(layout.more());

		if (!layout.clear().isEmpty())
			boxes.add(layout.clear());

		assertNoOverlap("chips", boxes);
	}

	@Test
	public void aCrowdOfChipsShowsPlusNMoreInsteadOfOverflowing()
	{
		WindowStyle style = WidgetKit.style(0.67);

		StorageControls.Layout layout = StorageControls.layout(body(style), style, font(style), true, false,
			chips(12));

		assertTrue("some chips did not fit", layout.moreCount() > 0);
		assertEquals(12, layout.chips().size() + layout.moreCount());
		assertFalse("the note has a box", layout.more().isEmpty());
		assertEquals("+" + layout.moreCount() + " more", StorageControls.moreText(layout.moreCount()));
	}

	@Test
	public void fewChipsAllFitAndLeaveRoomForClear()
	{
		WindowStyle style = WidgetKit.style(1.0);

		StorageControls.Layout layout = StorageControls.layout(body(style), style, font(style), false, false,
			chips(2));

		assertEquals(2, layout.chips().size());
		assertEquals(0, layout.moreCount());
		assertTrue(layout.more().isEmpty());
		assertFalse("Clear shows when there is room", layout.clear().isEmpty());
	}

	@Test
	public void noCriteriaMeansNoChipsAndNoClear()
	{
		WindowStyle style = WidgetKit.style(1.0);

		StorageControls.Layout layout = StorageControls.layout(body(style), style, font(style), false, false,
			StorageQuery.NONE);

		assertTrue(layout.chips().isEmpty());
		assertTrue(layout.clear().isEmpty());
	}

	@Test
	public void theListStartsWhereItDidBeforeTheControlsExisted()
	{
		for (double scale : SCALES)
		{
			WindowStyle style = WidgetKit.style(scale);
			WindowStyle.Metrics m = style.metrics();
			Rectangle body = body(style);
			int old = body.y + m.pad + m.chipHeight + m.pad / 2 + style.px(28) + m.pad / 2;

			StorageControls.Layout layout = StorageControls.layout(body, style, font(style), false, false,
				StorageQuery.NONE);

			assertEquals("scale " + scale, old, layout.listTop());
		}
	}

	private static void assertNoOverlap(String label, List<Rectangle> boxes)
	{
		for (int i = 0; i < boxes.size(); i++)
		{
			for (int j = i + 1; j < boxes.size(); j++)
				assertFalse(label + " " + boxes.get(i) + " and " + boxes.get(j),
					boxes.get(i).intersects(boxes.get(j)));
		}
	}
}
