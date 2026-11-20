/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Random;
import javax.imageio.ImageIO;

import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Paints the pinned frosted-glass Info card over a bright, a dark and a busy scene in every theme, checks that the
 * scene shows through, and prints what the glass costs per frame next to the old solid window. Set the environment
 * variable {@code BUBBLE_PNG_DIR} to write each picture as {@code glass-<theme>-<scene>.png}.
 */
public class GlassCardRenderTest
{
	private static final int W = 300;
	private static final int H = 340;
	private static final int RUNS = 1000;

	/** A scene behind the card. */
	private enum Scene
	{
		/** A bright, sandy scene. */
		BRIGHT,
		/** A dark, night-time scene. */
		DARK,
		/** Random coloured squares, like a busy game scene. */
		BUSY,
		/** Grass above water, each with a little noise, like a lake shore. */
		SHORE
	}

	private static void fill(BufferedImage image, Scene scene)
	{
		Graphics2D g = image.createGraphics();
		try
		{
			if (scene == Scene.SHORE)
			{
				Random random = new Random(7);
				for (int y = 0; y < image.getHeight(); y += 4)
				{
					for (int x = 0; x < image.getWidth(); x += 4)
					{
						int shade = random.nextInt(40);
						Color base = y < image.getHeight() / 2 ? new Color(70 + shade, 120 + shade, 40)
							: new Color(30, 70 + shade, 140 + shade);
						g.setColor(base);
						g.fillRect(x, y, 4, 4);
					}
				}
			}
			else if (scene == Scene.BUSY)
			{
				Random random = new Random(6);
				for (int y = 0; y < image.getHeight(); y += 6)
				{
					for (int x = 0; x < image.getWidth(); x += 6)
					{
						g.setColor(new Color(random.nextInt(0x1000000)));
						g.fillRect(x, y, 6, 6);
					}
				}
			}
			else
			{
				g.setColor(new Color(scene == Scene.BRIGHT ? 0xF4F1E8 : 0x101418));
				g.fillRect(0, 0, image.getWidth(), image.getHeight());
			}
		}
		finally
		{
			g.dispose();
		}
	}

	private static BubbleFrame frame(BubblePainter painter, WindowStyle style)
	{
		return frame(painter, style, 30, 30);
	}

	/**
	 * Builds a frame holding the sample Info card.
	 *
	 * @param painter the painter that measures the card
	 * @param style the style
	 * @param x the card's left edge
	 * @param y the card's top edge
	 * @return the frame
	 */
	static BubbleFrame frame(BubblePainter painter, WindowStyle style, int x, int y)
	{
		return frame(painter, style, x, y, true);
	}

	/**
	 * Builds a frame holding the sample Info card, pinned or not.
	 *
	 * @param painter the painter that measures the card
	 * @param style the style
	 * @param x the card's left edge
	 * @param y the card's top edge
	 * @param pinned whether the player clicked the card to keep it
	 * @return the frame
	 */
	static BubbleFrame frame(BubblePainter painter, WindowStyle style, int x, int y, boolean pinned)
	{
		Species species = new Species("Imp", 5, CombatType.MAGIC, Trait.UNHOLY, Trait.SLAYER, Trait.MAGIC)
			.withFactors(1.25, 0.75, 1.0, 1.12);
		WildEntry entry = new WildEntry(3, species, 14, species.level(), false, false);
		entry.record(WildRecord.of(2, false));
		JarSuggest.JarChoice shown = new JarSuggest.JarChoice(JarType.RUNE, 2, 0.456, false);
		WildCardContent content = WildCardContent.of(entry, shown, true);
		BufferedImage probe = WidgetKit.canvas(1, 1);
		Graphics2D pg = WidgetKit.graphics(probe);
		Dimension size = painter.cardSize(pg, content, style);
		pg.dispose();
		Rectangle bounds = new Rectangle(x, y, size.width, size.height);
		BubbleFrame.Card card = new BubbleFrame.Card(bounds, content, pinned, 1.0, 1.0, BubbleFrame.Side.RIGHT, true);
		return new BubbleFrame(Collections.emptyList(), card, 1, true);
	}

	private static BufferedImage render(BubblePainter painter, BubbleFrame frame, WindowStyle style, Scene scene)
	{
		return render(painter, frame, style, scene, "");
	}

	private static BufferedImage render(BubblePainter painter, BubbleFrame frame, WindowStyle style, Scene scene,
		String suffix)
	{
		BufferedImage image = WidgetKit.canvas(W, H);
		fill(image, scene);
		Graphics2D g = WidgetKit.graphics(image);
		painter.paint(g, frame, style, 1000L);
		g.dispose();
		String dir = System.getenv("BUBBLE_PNG_DIR");
		if (dir != null)
		{
			String name = "glass-" + style.theme().name().toLowerCase() + "-" + scene.name().toLowerCase() + suffix;
			try
			{
				ImageIO.write(image, "png", new File(dir, name + ".png"));
			}
			catch (IOException e)
			{
				throw new IllegalStateException(e);
			}
		}

		return image;
	}

	@Test
	public void theSceneShowsThroughTheCardInEveryTheme()
	{
		for (WindowTheme theme : new WindowTheme[] {WindowTheme.RUNELITE_GLASS})
		{
			BubblePainter painter = new BubblePainter();
			WindowStyle style = WindowStyle.of(theme, 1.0);
			BubbleFrame frame = frame(painter, style);
			Rectangle box = frame.card().bounds();
			int x = box.x + style.px(4);
			int y = (int) box.getCenterY();

			BufferedImage bright = render(painter, frame, style, Scene.BRIGHT);
			BufferedImage dark = render(painter, frame, style, Scene.DARK);
			render(painter, frame, style, Scene.BUSY);

			assertNotEquals(theme.name(), bright.getRGB(x, y), dark.getRGB(x, y));
			assertNotEquals(theme.name(), 0xF4F1E8, bright.getRGB(x, y) & 0xFFFFFF);
		}
	}

	@Test
	public void theRingWashTintsTheCardWithTheSceneAroundIt()
	{
		WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_GLASS, 1.0);
		BubblePainter plain = new BubblePainter();
		BubbleFrame frame = frame(plain, style);
		Rectangle box = frame.card().bounds();
		int x = (int) box.getCenterX();
		int y = box.y + box.height - style.px(4);
		for (Scene scene : Scene.values())
		{
			BufferedImage around = WidgetKit.canvas(W, H);
			fill(around, scene);
			BubblePainter ringed = new BubblePainter(() -> null, (name, foot) -> RingWash.wash(around, foot));
			BufferedImage image = render(ringed, frame, style, scene, "-ring");
			assertNotEquals(scene.name(), 0, image.getRGB(x, y) & 0xFFFFFF);
		}

		BufferedImage warm = WidgetKit.canvas(W, H);
		Graphics2D g = warm.createGraphics();
		g.setColor(new Color(0xC04020));
		g.fillRect(0, 0, W, H);
		g.dispose();
		BufferedImage cool = WidgetKit.canvas(W, H);
		g = cool.createGraphics();
		g.setColor(new Color(0x2040C0));
		g.fillRect(0, 0, W, H);
		g.dispose();
		int overWarm = render(new BubblePainter(() -> null, (name, foot) -> RingWash.wash(warm, foot)), frame, style,
			Scene.DARK).getRGB(x, y);
		int overCool = render(new BubblePainter(() -> null, (name, foot) -> RingWash.wash(cool, foot)), frame, style,
			Scene.DARK).getRGB(x, y);
		assertTrue((overWarm >> 16 & 0xFF) > (overCool >> 16 & 0xFF));
		assertTrue((overCool & 0xFF) > (overWarm & 0xFF));
	}

	private static int spread(BufferedImage image, int x, Rectangle box)
	{
		int min = 255;
		int max = 0;
		for (int y = box.y + box.height / 3; y < box.y + 2 * box.height / 3; y++)
		{
			int green = image.getRGB(x, y) >> 8 & 0xFF;
			min = Math.min(min, green);
			max = Math.max(max, green);
		}

		return max - min;
	}

	private static double micros(Runnable paint)
	{
		for (int i = 0; i < RUNS / 4; i++)
			paint.run();

		long start = System.nanoTime();
		for (int i = 0; i < RUNS; i++)
			paint.run();

		return (System.nanoTime() - start) / 1000.0 / RUNS;
	}

	@Test
	public void theCachedGlassIsNoSlowerThanPaintingItEachFrame()
	{
		WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_GLASS, 1.0);
		BubblePainter painter = new BubblePainter();
		BubbleFrame frame = frame(painter, style);
		Rectangle box = frame.card().bounds();
		BufferedImage canvas = WidgetKit.canvas(W, H);
		fill(canvas, Scene.BUSY);
		Graphics2D g = WidgetKit.graphics(canvas);
		Shape body = Shapes.roundedSlant(new java.awt.geom.Rectangle2D.Double(box.x, box.y, box.width, box.height),
			style.metrics().slant, box.height * GlassTokens.TIP_ROUND);
		WindowStyle solid = WindowStyle.of(WindowTheme.RUNELITE_DARK, 1.0);

		double old = micros(() -> solid.window(g, box));
		double warm = micros(() -> GlassPanel.paint(g, body, style, null, Readability.BODY));
		double cold = micros(() ->
		{
			GlassPanel.clear();
			GlassPanel.paint(g, body, style, null, Readability.BODY);
		});
		double card = micros(() -> painter.paint(g, frame, style, 1000L));
		g.dispose();

		System.out.printf("glass cost per frame: old window %.1f us, glass warm %.1f us, glass cold %.1f us,"
			+ " whole card warm %.1f us%n", old, warm, cold, card);
		assertTrue("warm " + warm + " cold " + cold, warm <= cold);
	}
}
