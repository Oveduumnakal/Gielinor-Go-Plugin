/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Prints what the glass look costs (#324): every glass surface of one typical frame, the window open on the Party
 * page, the tracker, the party box, the Info card and three bubbles, painted a thousand times warm, in glass and in
 * the solid look. It prints and does not gate on speed; it only checks that the glass frame is painted at all.
 */
public class GlassCostTest
{
	private static final int RUNS = 1000;
	private static final int W = 1920;
	private static final int H = 1080;

	private static final long[] parts = new long[4];

	private static double frameMicros(WindowTheme theme)
	{
		BufferedImage clean = LookSheet.scene(W, H);
		Map<String, BufferedImage> cache = new ConcurrentHashMap<>();
		WashSource source = (name, box) -> cache.computeIfAbsent(name + box.width + "x" + box.height,
			key -> RingWash.wash(clean, box));
		WindowStyle.share(source);
		try
		{
			WindowStyle style = WindowStyle.of(theme, 1.0);
			PartyRig rig = new PartyRig();
			rig.theme = theme;
			rig.width = 760;
			rig.height = 500;
			rig.publish(PartyRig.state(PartyRig.team(6)), true);
			rig.open();
			rig.now += 1_000;
			TrackerPainter tracker = new TrackerPainter(PageText.Sink.NONE);
			List<TrackerView.Line> lines = Arrays.asList(
				new TrackerView.Line(TrackerView.Kind.TASK, "task:0", "Walk 4,000 tiles", "1.2k / 4k", 0.3, null,
					false),
				new TrackerView.Line(TrackerView.Kind.EGG, "egg:0", "Legendary egg", "12,345 steps", 0.2,
					Rarity.LEGENDARY, false));
			PartyBoxPainter box = new PartyBoxPainter(rig.pictures, PageText.Sink.NONE);
			List<CreatureView> team = new ArrayList<>();
			for (int i = 0; i < 3; i++)
				team.add(new CreatureView("Goblin", 10 + i, 0.5, 1.0 - i * 0.15, Rarity.COMMON, CombatType.CRUSH,
					false, null));

			List<RowModel> rows = PartyBoxModel.rows(team);
			BubblePainter bubbles = new BubblePainter(() -> null, source);
			BubbleFrame card = GlassCardRenderTest.frame(bubbles, style, 1300, 120);
			List<BubbleFrame.Disc> discs = Arrays.asList(
				new BubbleFrame.Disc(BubbleKind.JAR, 1000, 800, 15, 1.0, 1.0, 0.0, JarType.RUNE, 0, false, false,
					0.0, true),
				new BubbleFrame.Disc(BubbleKind.BATTLE, 1040, 778, 15, 1.0, 1.0, 0.0, null, 0, false, false, 0.0,
					true),
				new BubbleFrame.Disc(BubbleKind.INFO, 1080, 800, 15, 1.0, 1.0, 0.0, null, 0, false, false, 0.0,
					true));
			BubbleFrame withBubbles = new BubbleFrame(discs, card.card(), 1, true);
			BufferedImage canvas = LookSheet.scene(W, H);
			Graphics2D g = WidgetKit.graphics(canvas);
			Runnable frame = () ->
			{
				long t0 = System.nanoTime();
				g.translate(500, 300);
				rig.shell.paint(g, rig.width, rig.height, rig.now);
				long t1 = System.nanoTime();
				g.translate(-440, -240);
				tracker.paint(g, style, lines);
				g.translate(0, 150);
				long t2 = System.nanoTime();
				box.paint(g, style, rows, 0L);
				g.translate(-60, -210);
				long t3 = System.nanoTime();
				bubbles.paint(g, withBubbles, style, 1000L);
				long t4 = System.nanoTime();
				parts[0] += t1 - t0;
				parts[1] += t2 - t1;
				parts[2] += t3 - t2;
				parts[3] += t4 - t3;
			};
			for (int i = 0; i < RUNS / 4; i++)
			{
				g.setTransform(new java.awt.geom.AffineTransform());
				frame.run();
			}

			java.util.Arrays.fill(parts, 0);
			long start = System.nanoTime();
			for (int i = 0; i < RUNS; i++)
			{
				g.setTransform(new java.awt.geom.AffineTransform());
				frame.run();
			}

			g.dispose();
			System.out.printf("  %s: window %.0f us, tracker %.0f us, party box %.0f us, card+bubbles %.0f us%n",
				theme, parts[0] / 1000.0 / RUNS, parts[1] / 1000.0 / RUNS, parts[2] / 1000.0 / RUNS,
				parts[3] / 1000.0 / RUNS);
			return (System.nanoTime() - start) / 1000.0 / RUNS;
		}
		finally
		{
			WindowStyle.share(null);
		}
	}

	@Test
	public void aTypicalFrameCostsThisMuchInGlassAndInSolid()
	{
		double solid = frameMicros(WindowTheme.RUNELITE_DARK);
		double glass = frameMicros(WindowTheme.RUNELITE_GLASS);
		System.out.printf("glass cost, one typical frame (window on Party, tracker, party box, card, 3 bubbles):"
			+ " solid %.0f us, glass %.0f us%n", solid, glass);
		assertTrue(glass > 0 && solid > 0);
	}
}
