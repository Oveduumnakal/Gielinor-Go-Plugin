/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Consumer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Checks the paint kit: the slant geometry, the painted shapes and colours, fonts, the layer cache and notices. */
public class WindowStyleTest
{
	private static final Rectangle2D BOX = new Rectangle2D.Double(20, 20, 200, 40);

	private static BufferedImage canvas()
	{
		return new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
	}

	private static Graphics2D graphics(BufferedImage image)
	{
		Graphics2D g = image.createGraphics();
		WindowStyle.prepare(g);
		return g;
	}

	private static Color at(BufferedImage image, int x, int y)
	{
		return new Color(image.getRGB(x, y), true);
	}

	private static void assertClose(String what, Color expected, Color actual)
	{
		assertClose(what, expected, actual, 2);
	}

	private static void assertClose(String what, Color expected, Color actual, int tolerance)
	{
		assertTrue(what + " expected " + expected + " but was " + actual,
			Math.abs(expected.getRed() - actual.getRed()) <= tolerance
				&& Math.abs(expected.getGreen() - actual.getGreen()) <= tolerance
				&& Math.abs(expected.getBlue() - actual.getBlue()) <= tolerance
				&& Math.abs(expected.getAlpha() - actual.getAlpha()) <= tolerance);
	}

	@Test
	public void thePanelShapeSlantsTheCornersSoACardStripeCanFollowThem()
	{
		WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_DARK, 1.0);
		Shape shape = style.panelShape(BOX);

		assertFalse("the very corner is cut away", shape.contains(BOX.getX() + 0.5, BOX.getY() + 0.5));
		assertTrue("the middle of the left edge is kept", shape.contains(BOX.getX() + 5, BOX.getCenterY()));
		assertTrue("the long end of the left edge is kept", shape.contains(BOX.getX() + 1, BOX.getMaxY() - 1));
	}

	@Test
	public void slantShapeFitsItsBoxAndLeansLikeASlash()
	{
		Path2D.Double shape = Slant.shape(BOX, 10);
		assertEquals(BOX, shape.getBounds2D());
		assertTrue(shape.contains(120, 40));
		assertTrue(shape.contains(219, 21));
		assertTrue(shape.contains(21, 59));
		assertFalse("top-left corner is cut", shape.contains(21, 21));
		assertFalse("bottom-right corner is cut", shape.contains(219, 59));
		assertEquals(30, Slant.leftAt(20, BOX, 10), 1e-9);
		assertEquals(20, Slant.leftAt(60, BOX, 10), 1e-9);
		assertEquals(220, Slant.rightAt(20, BOX, 10), 1e-9);
		assertEquals(210, Slant.rightAt(60, BOX, 10), 1e-9);
	}

	@Test
	public void slantHitTestMatchesTheDrawnShape()
	{
		Path2D.Double shape = Slant.shape(BOX, 14);
		for (int x = 15; x < 230; x += 5)
		{
			for (int y = 15; y < 66; y += 3)
			{
				boolean inside = Slant.contains(x + 0.5, y + 0.5, BOX, 14);
				assertEquals(x + "," + y, shape.contains(x + 0.5, y + 0.5), inside);
			}
		}

		assertFalse(Slant.contains(120, 19, BOX, 14));
		assertFalse(Slant.contains(120, 61, BOX, 14));
	}

	@Test
	public void slantIsLimitedToHalfTheWidthAndNeverNegative()
	{
		Rectangle2D narrow = new Rectangle2D.Double(0, 0, 10, 10);
		assertEquals(narrow, Slant.shape(narrow, 500).getBounds2D());
		assertEquals(5, Slant.leftAt(0, narrow, 500), 1e-9);
		assertEquals(0, Slant.leftAt(0, narrow, -9), 1e-9);
	}

	@Test
	public void filledPartOfABarKeepsTheSlant()
	{
		assertEquals(BOX, Slant.filled(BOX, 10, 1.0).getBounds2D());
		Rectangle2D half = Slant.filled(BOX, 10, 0.5).getBounds2D();
		assertEquals(20, half.getMinX(), 1e-9);
		assertEquals(125, half.getMaxX(), 1e-9);
		Rectangle2D empty = Slant.filled(BOX, 10, 0.0).getBounds2D();
		assertEquals(20, empty.getMinX(), 1e-9);
		assertEquals(BOX, Slant.filled(BOX, 10, 7.0).getBounds2D());
	}

	@Test
	public void aRowIsPaintedInThemeColoursAndClippedToTheSlant()
	{
		for (WindowTheme theme : WindowTheme.values())
		{
			WindowStyle style = WindowStyle.of(theme, 1.0);
			BufferedImage image = canvas();
			Graphics2D g = graphics(image);
			Shape shape = style.row(g, BOX, false, 0, 0);
			style.row(g, new Rectangle2D.Double(20, 100, 200, 40), true, 0, 0);
			g.dispose();

			assertTrue(shape.contains(120, 40));
			assertClose(theme + " row", theme.color(ThemeColor.ROW), at(image, 120, 40));
			assertClose(theme + " lead row", theme.color(ThemeColor.LEAD_ROW), at(image, 120, 120));
			assertEquals("outside the slant", 0, at(image, 22, 22).getAlpha());
			assertEquals("outside the box", 0, at(image, 10, 40).getAlpha());
			assertClose(theme + " row border", theme.color(ThemeColor.ROW_BORDER), at(image, 120, 21));
			assertClose(theme + " lead border", theme.color(ThemeColor.LEAD_BORDER), at(image, 120, 101));
		}
	}

	@Test
	public void hoverLightensAndPressDarkens()
	{
		WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_DARK, 1.0);
		BufferedImage image = canvas();
		Graphics2D g = graphics(image);
		style.row(g, BOX, false, 0, 0);
		style.row(g, new Rectangle2D.Double(20, 80, 200, 40), false, 1, 0);
		style.row(g, new Rectangle2D.Double(20, 140, 200, 40), false, 0, 1);
		g.dispose();

		Color plain = at(image, 120, 40);
		assertTrue(at(image, 120, 100).getBlue() > plain.getBlue());
		assertTrue(at(image, 120, 160).getBlue() < plain.getBlue());
	}

	@Test
	public void healthColourFollowsTheLevel()
	{
		assertEquals(ThemeColor.HP_FILL, WindowStyle.hpColor(1.0));
		assertEquals(ThemeColor.HP_FILL, WindowStyle.hpColor(0.5));
		assertEquals(ThemeColor.HP_WARNING, WindowStyle.hpColor(0.49));
		assertEquals(ThemeColor.HP_WARNING, WindowStyle.hpColor(0.2));
		assertEquals(ThemeColor.HP_DANGER, WindowStyle.hpColor(0.19));
		assertEquals(ThemeColor.HP_DANGER, WindowStyle.hpColor(0.0));
	}

	@Test
	public void healthAndExperienceBarsFillTheRightShare()
	{
		for (WindowTheme theme : WindowTheme.values())
		{
			WindowStyle style = WindowStyle.of(theme, 1.0);
			BufferedImage image = canvas();
			Graphics2D g = graphics(image);
			Rectangle2D hp = new Rectangle2D.Double(20, 20, 200, 12);
			style.hpBar(g, hp, 0.6);
			style.hpBar(g, new Rectangle2D.Double(20, 50, 200, 12), 0.3);
			style.hpBar(g, new Rectangle2D.Double(20, 80, 200, 12), 0.1);
			style.xpBar(g, new Rectangle2D.Double(20, 110, 200, 12), 0.5);
			g.dispose();

			assertClose(theme + " hp fill", theme.color(ThemeColor.HP_FILL), at(image, 60, 26));
			assertClose(theme + " hp track", theme.color(ThemeColor.HP_TRACK), at(image, 200, 26));
			assertClose(theme + " hp warning", theme.color(ThemeColor.HP_WARNING), at(image, 40, 56));
			assertClose(theme + " hp danger", theme.color(ThemeColor.HP_DANGER), at(image, 28, 86));
			assertClose(theme + " xp fill", theme.color(ThemeColor.XP_FILL), at(image, 60, 116));
			assertClose(theme + " xp track", theme.color(ThemeColor.XP_TRACK), at(image, 200, 116));
		}
	}

	@Test
	public void aDisabledTabHasItsOwnColoursAndNoHover()
	{
		WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_DARK, 1.0);
		BufferedImage image = canvas();
		Graphics2D g = graphics(image);
		style.tab(g, new Rectangle2D.Double(10, 10, 160, 44), "Party", false, false, 0);
		style.tab(g, new Rectangle2D.Double(10, 70, 160, 44), "Party", true, false, 0);
		style.tab(g, new Rectangle2D.Double(10, 130, 160, 44), "Storage", false, true, 1);
		g.dispose();

		WindowTheme theme = WindowTheme.RUNELITE_DARK;
		assertClose("tab", theme.color(ThemeColor.TAB), at(image, 20, 50));
		assertClose("active tab", theme.color(ThemeColor.TAB_ACTIVE), at(image, 20, 110));
		assertClose("disabled tab, hover ignored", theme.color(ThemeColor.TAB_DISABLED), at(image, 20, 170));
		assertTrue("the label is drawn", hasPixelOtherThan(image, 40, 20, 100, 24, theme.color(ThemeColor.TAB)));
	}

	@Test
	public void aChipReportsItsWidthAndPaintsInsideIt()
	{
		WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_DARK, 1.0);
		BufferedImage image = canvas();
		Graphics2D g = graphics(image);
		int measured = style.chipWidth(g, "Slayer");
		int painted = style.chip(g, 20, 20, "Slayer", false);
		int accent = style.chip(g, 20, 60, "Slayer", true);
		g.dispose();

		assertEquals(measured, painted);
		assertEquals(measured, accent);
		assertTrue(style.metrics().chipPad * 2 < measured);
		int low = style.metrics().chipHeight - 2;
		assertClose("chip", WindowTheme.RUNELITE_DARK.color(ThemeColor.CHIP), at(image, 21, 20 + low));
		assertClose("accent chip", WindowTheme.RUNELITE_DARK.color(ThemeColor.CHIP_ACCENT), at(image, 21, 60 + low));
		assertEquals(0, at(image, 20 + measured + 4, 30).getAlpha());
	}

	@Test
	public void theWindowAndPanelPaintBodyAndFrame()
	{
		WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_DARK, 1.0);
		BufferedImage image = canvas();
		Graphics2D g = graphics(image);
		style.window(g, new Rectangle(10, 10, 300, 200));
		style.panel(g, new Rectangle2D.Double(30, 30, 100, 60));
		g.dispose();

		WindowTheme theme = WindowTheme.RUNELITE_DARK;
		assertClose("window, over its own shadow", theme.color(ThemeColor.WINDOW), at(image, 250, 150), 8);
		assertClose("frame", theme.color(ThemeColor.FRAME), at(image, 150, 10));
		assertClose("panel", theme.color(ThemeColor.PANEL_BOX), at(image, 80, 60));
		assertEquals("nothing outside", 0, at(image, 5, 5).getAlpha());
	}

	@Test
	public void layoutNumbersScaleWithTheWindowAndNothingElse()
	{
		WindowStyle one = WindowStyle.of(WindowTheme.RUNELITE_DARK, 1.0);
		WindowStyle two = WindowStyle.of(WindowTheme.RUNELITE_DARK, 2.0);
		assertEquals(14, one.metrics().slant);
		assertEquals(28, two.metrics().slant);
		assertEquals(64, one.metrics().row);
		assertEquals(128, two.metrics().row);
		WindowStyle tiny = WindowStyle.of(WindowTheme.RUNELITE_DARK, 0.1);
		WindowStyle lite = WindowStyle.of(WindowTheme.RUNELITE_DARK, 1.0);
		assertEquals(1, tiny.metrics().border);
		assertEquals(one.metrics(), lite.metrics());
		assertNotEquals(one.metrics(), two.metrics());
	}

	@Test
	public void textIsOnlyEverDrawnAtWholeMultiplesOfTheNativeSize()
	{
		assertEquals(1, WindowFonts.step(WindowFonts.BODY_UNITS, WindowFit.MIN_SCALE));
		assertEquals(1, WindowFonts.step(WindowFonts.BODY_UNITS, 1.0));
		assertEquals(2, WindowFonts.step(WindowFonts.TITLE_UNITS, 1.0));
		assertEquals(1, WindowFonts.step(1, 0.01));
		assertEquals(WindowFonts.MAX_STEP, WindowFonts.step(500, 3.0));

		for (double scale = 0.5; scale <= 3.0; scale += 0.07)
		{
			for (WindowFonts.Face face : WindowFonts.Face.values())
			{
				double[] sizes = {WindowFonts.SMALL_UNITS, WindowFonts.BODY_UNITS, WindowFonts.TITLE_UNITS};
				for (double units : sizes)
				{
					Font font = WindowFonts.font(face, units, scale);
					assertEquals(0, font.getSize() % WindowFonts.NATIVE_PX);
					assertTrue(font.getSize() >= WindowFonts.NATIVE_PX);
					assertEquals(font.getSize(), WindowFonts.pixelSize(units, scale));
				}
			}
		}

		assertSame(WindowFonts.font(WindowFonts.Face.BOLD, 24, 1.0), WindowFonts.font(WindowFonts.Face.BOLD, 24, 1.0));
		Font regular = WindowFonts.font(WindowFonts.Face.REGULAR, 24, 1.0);
		assertTrue(regular.getFamily().startsWith("RuneScape"));
	}

	@Test
	public void nativeSizeTextPaintsOnlyCrispPixels()
	{
		WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_DARK, 1.0);
		BufferedImage image = canvas();
		Graphics2D g = graphics(image);
		style.text(g, "Lv 42 HP", 10, 40, style.font(WindowFonts.Face.REGULAR, 16), ThemeColor.TEXT);
		g.dispose();

		int solid = 0;
		int partial = 0;
		for (int y = 0; y < 60; y++)
		{
			for (int x = 0; x < 100; x++)
			{
				int a = at(image, x, y).getAlpha();
				if (a == 255)
					solid++;
				else if (a > 0)
					partial++;
			}
		}

		assertTrue("solid " + solid, solid > 20);
		assertTrue("a pixel font at its native size has almost no soft edges: " + partial + " of " + solid,
			partial * 4 <= solid);
	}

	@Test
	public void ellipsizeCutsToTheWidth()
	{
		BufferedImage image = canvas();
		Graphics2D g = graphics(image);
		Font font = WindowFonts.font(WindowFonts.Face.REGULAR, 24, 1.0);
		FontMetrics fm = g.getFontMetrics(font);
		g.dispose();

		assertEquals("Man", WindowStyle.ellipsize("Man", fm, 200));
		String cut = WindowStyle.ellipsize("Frenzied ice troll female", fm, 100);
		assertTrue(cut, cut.endsWith("..."));
		assertTrue(cut, fm.stringWidth(cut) <= 100);
		assertTrue(cut, cut.startsWith("Fren"));
		assertEquals("", WindowStyle.ellipsize("Frenzied", fm, 3));
	}

	@Test
	public void stillLayersPaintOnlyWhenTheKeyChanges()
	{
		StillLayers layers = new StillLayers();
		int[] calls = {0};
		Consumer<Graphics2D> painter = g ->
		{
			calls[0]++;
			g.setColor(Color.RED);
			g.fillRect(0, 0, 10, 10);
		};

		BufferedImage first = layers.layer("party", 100, 80, 1.0, WindowTheme.RUNELITE_DARK, 1, painter);
		assertSame(first, layers.layer("party", 100, 80, 1.0, WindowTheme.RUNELITE_DARK, 1, painter));
		assertEquals(1, layers.paints());
		assertEquals(1, calls[0]);
		assertEquals(Color.RED.getRGB(), first.getRGB(5, 5));
		assertEquals(0, first.getRGB(50, 50));

		layers.layer("party", 100, 80, 1.0, WindowTheme.RUNELITE_DARK, 2, painter);
		assertEquals(2, layers.paints());
		layers.layer("party", 100, 80, 1.5, WindowTheme.RUNELITE_DARK, 2, painter);
		assertEquals(3, layers.paints());
		layers.layer("party", 120, 80, 1.5, WindowTheme.RUNELITE_DARK, 2, painter);
		assertEquals(4, layers.paints());
		layers.layer("bag", 120, 80, 1.5, WindowTheme.RUNELITE_DARK, 2, painter);
		assertEquals(5, layers.paints());
		assertEquals(2, layers.size());

		layers.layer("bag", 120, 80, 1.5, WindowTheme.RUNELITE_DARK, 2, painter);
		assertEquals(5, layers.paints());
		layers.clear();
		assertEquals(0, layers.size());
	}

	@Test
	public void stillLayersDrawOntoTheCanvasAtAPosition()
	{
		StillLayers layers = new StillLayers();
		BufferedImage target = canvas();
		Graphics2D g = graphics(target);
		layers.draw(g, 30, 40, "party", 50, 50, 1.0, WindowTheme.RUNELITE_DARK, 1, p ->
		{
			p.setColor(Color.GREEN);
			p.fillRect(0, 0, 50, 50);
		});
		g.dispose();

		assertEquals(Color.GREEN.getRGB(), target.getRGB(30, 40));
		assertEquals(Color.GREEN.getRGB(), target.getRGB(79, 89));
		assertEquals(0, target.getRGB(29, 40));
		assertEquals(0, target.getRGB(80, 90));
	}

	@Test
	public void stillLayersAreSharperOnADoubledCanvasAndPaintAgainWhenTheDensityChanges()
	{
		StillLayers layers = new StillLayers();
		Consumer<Graphics2D> painter = p ->
		{
			p.setColor(Color.GREEN);
			p.fillRect(0, 0, 50, 50);
		};

		BufferedImage plain = layers.layer("party", 50, 50, 1.0, WindowTheme.RUNELITE_DARK, 1, painter, 1.0);
		BufferedImage doubled = layers.layer("party", 50, 50, 1.0, WindowTheme.RUNELITE_DARK, 1, painter, 2.0);

		assertEquals(50, plain.getWidth());
		assertEquals(100, doubled.getWidth());
		assertEquals(100, doubled.getHeight());
		assertEquals(Color.GREEN.getRGB(), doubled.getRGB(99, 99));
		assertEquals(2, layers.paints());
		assertSame(doubled, layers.layer("party", 50, 50, 1.0, WindowTheme.RUNELITE_DARK, 1, painter, 2.0));
		assertEquals(2, layers.paints());
	}

	@Test
	public void theNoticeDrawsAroundItsCentreAndFadesToNothing()
	{
		WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_DARK, 1.0);
		BufferedImage shown = canvas();
		Graphics2D g = graphics(shown);
		style.notice(g, "Still loading your save.", 200, 150, 1.0, 1.0);
		g.dispose();
		assertTrue(at(shown, 200, 150).getAlpha() > 200);
		assertEquals(0, at(shown, 5, 5).getAlpha());
		assertTrue(hasPixelOtherThan(shown, 100, 130, 200, 40, Color.MAGENTA));

		BufferedImage gone = canvas();
		g = graphics(gone);
		style.notice(g, "Still loading your save.", 200, 150, 1.0, 0.0);
		g.dispose();
		assertFalse(hasPixelOtherThan(gone, 0, 0, 400, 300, Color.MAGENTA));
	}

	@Test
	public void windowClassesWriteNoColourLiterals() throws IOException
	{
		File dir = new File("src/main/java/com/oveduumnakal/gielinorgo");
		String[] names = {"WindowStyle", "Slant", "StillLayers", "WindowFonts", "WindowFit", "Easing", "Tween",
			"Smoothed", "Loop", "Animations", "ShellMotion", "NoticeClock", "MotionMode", "WindowTimes"};
		for (String name : names)
		{
			byte[] bytes = Files.readAllBytes(new File(dir, name + ".java").toPath());
			String source = new String(bytes, StandardCharsets.UTF_8);
			assertFalse(name + " writes a colour literal; read the theme instead", source.contains("new Color("));
		}
	}

	private static boolean hasPixelOtherThan(BufferedImage image, int x, int y, int w, int h, Color not)
	{
		for (int j = y; j < y + h; j++)
		{
			for (int i = x; i < x + w; i++)
			{
				Color c = at(image, i, j);
				if (c.getAlpha() != 0 && !c.equals(not))
					return true;
			}
		}

		return false;
	}
}
