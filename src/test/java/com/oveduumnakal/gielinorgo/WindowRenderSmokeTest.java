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
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Paints the whole window headless into a {@code BufferedImage}, in both themes, at several canvas sizes including
 * fixed mode's 765 x 503, and in every state the shell has (a menu, a dialog, a tip, a greyed Storage). It fails on
 * any exception, on a hit outside the window, and on a label wider than its box.
 */
public class WindowRenderSmokeTest
{
	private static final int[][] CANVASES = {{765, 503}, {1024, 768}, {1920, 1080}, {800, 520}, {640, 700}};

	private WindowTheme theme = WindowTheme.DARK_GLASS;
	private final WindowShell shell = new WindowShell(new WindowShell.Host()
	{
		@Override
		public WindowTheme theme()
		{
			return theme;
		}

		@Override
		public MotionMode motion()
		{
			return MotionMode.OFF;
		}

		@Override
		public boolean keyHints()
		{
			return true;
		}

		@Override
		public boolean keyboard()
		{
			return true;
		}

		@Override
		public String sizeLabel()
		{
			return "Size: Fit canvas";
		}

		@Override
		public void resetWindow()
		{
			return;
		}

		@Override
		public void flipTheme()
		{
			return;
		}

		@Override
		public void nextSize()
		{
			return;
		}

		@Override
		public void notice(NoticeMessage message)
		{
			return;
		}
	});

	private static PanelState state(int team)
	{
		List<CreatureView> creatures = new ArrayList<>();
		for (int i = 0; i < team; i++)
			creatures.add(new CreatureView("Goblin", 5, 0.5, 1.0, Rarity.COMMON, CombatType.STAB, false, ""));

		return new PanelState(1, 0.0, 0, new EnumMap<>(JarType.class), JarType.BRONZE, 0, creatures,
			Collections.emptyList(), Collections.emptyList(), 0, 0, Collections.emptyList(),
			Collections.emptyList(), TodayView.NONE, TaskView.NONE);
	}

	private BufferedImage paintAt(Dimension size, long nowMs)
	{
		BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			assertTrue(shell.paint(g, size.width, size.height, nowMs));
		}
		finally
		{
			g.dispose();
		}

		return image;
	}

	private static int opaquePixels(BufferedImage image)
	{
		int count = 0;
		for (int y = 0; y < image.getHeight(); y += 3)
			for (int x = 0; x < image.getWidth(); x += 3)
				count += image.getRGB(x, y) >>> 24 > 0 ? 1 : 0;

		return count;
	}

	@Test
	public void bothThemesPaintTheWholeWindowAtEveryCanvasSize()
	{
		for (WindowTheme each : WindowTheme.values())
			for (int[] canvas : CANVASES)
				for (WindowSizePreset preset : WindowSizePreset.values())
				{
					theme = each;
					Dimension size = WindowFit.preset(preset, canvas[0], canvas[1]).getSize();
					shell.publish(WindowSnapshot.of(1, state(3), true));
					shell.open(1_000L);
					BufferedImage image = paintAt(size, 1_000L);
					shell.close(1_000L, ShellMotion.Phase.CLOSE);

					String where = each + " " + canvas[0] + "x" + canvas[1] + " " + preset + " " + size;
					assertTrue(where, size.width <= canvas[0] && size.height <= canvas[1]);
					assertTrue(where, opaquePixels(image) > size.width * size.height / 9 / 2);
				}
	}

	@Test
	public void fixedModeGivesAWindowAboutFiveHundredAndThirtyFivePixelsWide()
	{
		Dimension size = WindowFit.sizeFor(WindowFit.DEFAULT_FRACTION, 765, 503);

		assertEquals(536, size.width, 1);
		assertEquals(348, size.height, 2);
		assertTrue(WindowFit.scale(size.width, size.height) >= WindowFit.MIN_SCALE);
	}

	@Test
	public void everyHitStaysInsideTheWindowAndHitsDoNotMoveWithTheTheme()
	{
		for (int[] canvas : CANVASES)
		{
			Dimension size = WindowFit.sizeFor(WindowFit.DEFAULT_FRACTION, canvas[0], canvas[1]);
			List<List<String>> perTheme = new ArrayList<>();
			for (WindowTheme each : WindowTheme.values())
			{
				theme = each;
				WindowShell fresh = new WindowShell(shellHost());
				fresh.publish(WindowSnapshot.of(1, state(2), false));
				fresh.open(5L);
				Graphics2D g = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB)
					.createGraphics();
				fresh.paint(g, size.width, size.height, 5L);
				g.dispose();
				List<String> boxes = new ArrayList<>();
				for (Hit hit : fresh.hits())
				{
					assertTrue(hit.id(), new Rectangle(0, 0, size.width, size.height).contains(hit.bounds()));
					boxes.add(hit.id() + hit.bounds());
				}

				perTheme.add(boxes);
			}

			assertEquals(perTheme.get(0), perTheme.get(1));
		}
	}

	private WindowShell.Host shellHost()
	{
		return new WindowShell.Host()
		{
			@Override
			public WindowTheme theme()
			{
				return theme;
			}

			@Override
			public MotionMode motion()
			{
				return MotionMode.OFF;
			}

			@Override
			public boolean keyHints()
			{
				return true;
			}

			@Override
			public boolean keyboard()
			{
				return true;
			}

			@Override
			public String sizeLabel()
			{
				return "Size: Medium";
			}

			@Override
			public void resetWindow()
			{
				return;
			}

			@Override
			public void flipTheme()
			{
				return;
			}

			@Override
			public void nextSize()
			{
				return;
			}

			@Override
			public void notice(NoticeMessage message)
			{
				return;
			}
		};
	}

	@Test
	public void everyLabelFitsItsBoxAtTheSmallestAndAtLargeScales()
	{
		for (int[] canvas : CANVASES)
			for (double fraction : new double[] {WindowFit.DEFAULT_FRACTION, 1.0})
			{
				Dimension size = WindowFit.sizeFor(fraction, canvas[0], canvas[1]);
				WindowLayout layout = WindowLayout.of(size.width, size.height);
				WindowStyle style = WindowStyle.of(WindowTheme.DARK_GLASS, layout.scale);
				FontMetrics small = WidgetKit.metrics(style, WindowFonts.Face.SMALL);
				String where = canvas[0] + "x" + canvas[1] + " scale " + layout.scale;

				WindowTab[] tabs = WindowTab.values();
				for (int i = 0; i < tabs.length; i++)
					assertTrue(where + " tab " + tabs[i], textFits(style, tabs[i].label(),
						layout.tabs[i].width - 2 * style.metrics().tabSlant));

				int chip = 2 * style.metrics().chipSlant;
				assertTrue(where + " close", textFits(style, "Close", layout.close.width - chip));
				assertTrue(where + " reset", textFits(style, "Reset window", layout.reset.width - chip));
				for (WindowTheme each : WindowTheme.values())
					assertTrue(where + " theme " + each, textFits(style, each.toString(), layout.theme.width - chip));

				for (WindowSizePreset preset : WindowSizePreset.values())
					assertTrue(where + " size " + preset, textFits(style, "Size: " + preset,
						layout.size.width - chip));

				assertTrue(where + " change log", textFits(style, "What's New", layout.changelog.width - chip));
				assertTrue(where + " feedback", textFits(style, "Feedback", layout.feedback.width - chip));
				for (String hint : Arrays.asList(WindowShell.HINT_WINDOW, WindowShell.HINT_MENU,
					WindowShell.HINT_MODAL, WindowShell.HINT_FIELD, WindowShell.HINT_UNFOCUSED))
					assertTrue(where + " hint " + hint, small.stringWidth(hint) <= layout.hints.width);
			}
	}

	private static boolean textFits(WindowStyle style, String text, int room)
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
	public void theLayoutPartsStayInsideTheWindowAndDoNotOverlap()
	{
		for (int[] canvas : CANVASES)
		{
			Dimension size = WindowFit.sizeFor(WindowFit.DEFAULT_FRACTION, canvas[0], canvas[1]);
			WindowLayout layout = WindowLayout.of(size.width, size.height);
			List<Rectangle> parts = new ArrayList<>(Arrays.asList(layout.tabs));
			parts.addAll(Arrays.asList(layout.close, layout.body, layout.hints, layout.reset, layout.theme,
				layout.size));

			for (Rectangle part : parts)
				assertTrue(canvas[0] + " " + part, layout.window.contains(part));

			for (int i = 0; i < parts.size(); i++)
				for (int j = i + 1; j < parts.size(); j++)
					assertFalse(i + " overlaps " + j, parts.get(i).intersects(parts.get(j)));
		}
	}

	@Test
	public void aMenuADialogAndATipPaintOverTheWindow()
	{
		Dimension size = WindowFit.sizeFor(WindowFit.DEFAULT_FRACTION, 765, 503);
		for (WindowTheme each : WindowTheme.values())
		{
			theme = each;
			shell.publish(WindowSnapshot.of(1, state(3), false));
			shell.open(0L);
			BufferedImage plain = paintAt(size, 0L);

			Hit storageHit = shell.hits().get(WindowTab.STORAGE.ordinal());
			Rectangle storage = storageHit.bounds();
			shell.onMoved(storage.x + 4, storage.y + 4, 0L);
			BufferedImage tipped = paintAt(size, 1_000L);
			assertNotEquals(each.toString(), pixels(plain), pixels(tipped));

			shell.onLeft();
			shell.openMenu(List.of(ContextMenu.Item.of("Summary", () -> { }), ContextMenu.Item.disabled("Pin",
				"Coming with the tracker")), 60, 80, true, 1_000L);
			BufferedImage menu = paintAt(size, 1_000L);
			assertNotEquals(each.toString(), pixels(plain), pixels(menu));
			shell.onKey(KeyEvent.VK_ESCAPE, 0, 1_000L);

			TextBox box = new TextBox();
			shell.openModal(new Modal("Rename", "Pick a new name.", Arrays.asList("Cancel", "Save"), 1, i -> { },
				() -> { }).withField(box, 1), box, 1_000L);
			shell.onTyped('A', 0, 1_000L);
			BufferedImage modal = paintAt(size, 1_000L);
			assertNotEquals(each.toString(), pixels(plain), pixels(modal));

			shell.close(1_000L, ShellMotion.Phase.CLOSE);
		}
	}

	private static List<Integer> pixels(BufferedImage image)
	{
		List<Integer> list = new ArrayList<>();
		for (int y = 0; y < image.getHeight(); y += 2)
			for (int x = 0; x < image.getWidth(); x += 2)
				list.add(image.getRGB(x, y));

		return list;
	}

	@Test
	public void theTwoThemesLookDifferent()
	{
		Dimension size = WindowFit.sizeFor(WindowFit.DEFAULT_FRACTION, 765, 503);
		List<List<Integer>> looks = new ArrayList<>();
		for (WindowTheme each : WindowTheme.values())
		{
			theme = each;
			shell.publish(WindowSnapshot.of(1, state(1), true));
			shell.open(0L);
			looks.add(pixels(paintAt(size, 0L)));
			shell.close(0L, ShellMotion.Phase.CLOSE);
		}

		assertNotEquals(looks.get(0), looks.get(1));
	}

	@Test
	public void theNoticePaintsInBothThemesAtTheEdgesOfTheCanvas()
	{
		for (WindowTheme each : WindowTheme.values())
			for (NoticeMessage message : NoticeMessage.values())
			{
				BufferedImage image = new BufferedImage(765, 503, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = image.createGraphics();
				WindowStyle style = WindowStyle.of(each, 1.0);
				WindowStyle.prepare(g);
				FontMetrics fm = g.getFontMetrics(style.font(WindowFonts.Face.BOLD, WindowFonts.BODY_UNITS));
				int half = (fm.stringWidth(message.text()) + 2 * style.metrics().pad) / 2;
				int cx = NoticeSlot.clampCenter(0, half, 765);
				style.notice(g, message.text(), cx, 40, 1.0, 1.0);
				g.dispose();

				assertTrue(each + " " + message, opaquePixels(image) > 0);
				assertTrue(each + " " + message, cx - half >= 0 && cx + half <= 765);
			}
	}
}
