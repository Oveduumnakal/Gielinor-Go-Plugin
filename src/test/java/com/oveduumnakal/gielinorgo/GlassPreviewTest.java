/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import org.junit.Assume;
import org.junit.Test;

/**
 * Lets the owner review the glass look without a client (#324): paints every glass surface on a game screenshot and
 * writes pictures. Skipped unless the environment variable {@code GLASS_SCENE} names a screenshot; the pictures go to
 * the folder {@code BUBBLE_PNG_DIR}, as {@code preview-<name>.png}. The wash of each surface is made the way the game
 * makes it, from a band of the screenshot just outside the surface ({@link RingWash}).
 */
public class GlassPreviewTest
{
	private static BufferedImage scene()
	{
		String path = System.getenv("GLASS_SCENE");
		Assume.assumeNotNull(path);
		try
		{
			BufferedImage read = ImageIO.read(new File(path));
			BufferedImage copy = new BufferedImage(read.getWidth(), read.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = copy.createGraphics();
			g.drawImage(read, 0, 0, null);
			g.dispose();
			return copy;
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}

	private static File folder()
	{
		String dir = System.getenv("BUBBLE_PNG_DIR");
		Assume.assumeNotNull(dir);
		File folder = new File(dir);
		folder.mkdirs();
		return folder;
	}

	private static BufferedImage copy(BufferedImage src)
	{
		BufferedImage c = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = c.createGraphics();
		g.drawImage(src, 0, 0, null);
		g.dispose();
		return c;
	}

	private static void write(BufferedImage image, Rectangle crop, String name)
	{
		Rectangle on = crop.intersection(new Rectangle(0, 0, image.getWidth(), image.getHeight()));
		try
		{
			ImageIO.write(image.getSubimage(on.x, on.y, on.width, on.height), "png",
				new File(folder(), "preview-" + name + ".png"));
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Paints the Info card on the screenshot at a few spots.
	 */
	@Test
	public void theInfoCardOnTheScene()
	{
		BufferedImage scene = scene();
		BufferedImage clean = copy(scene);
		String[] names = {"grass", "stone-wall", "beige-wall"};
		int[][] spots = {{820, 30}, {1150, 170}, {1480, 420}};
		WindowStyle style = WindowStyle.of(WindowTheme.RUNELITE_GLASS, 1.0);
		for (int i = 0; i < spots.length; i++)
		{
			BubblePainter painter = new BubblePainter(() -> null, (name, foot) -> RingWash.wash(clean, foot));
			BubbleFrame frame = GlassCardRenderTest.frame(painter, style, spots[i][0], spots[i][1]);
			Rectangle box = frame.card().bounds();
			BufferedImage canvas = copy(scene);
			Graphics2D g = WidgetKit.graphics(canvas);
			painter.paint(g, frame, style, 1000L);
			g.dispose();
			int m = 60;
			write(canvas, new Rectangle(box.x - m, box.y - m, box.width + 2 * m, box.height + 2 * m),
				"card-" + names[i]);
		}
	}

	/**
	 * Paints the main window on every page over the screenshot, in glass.
	 */
	@Test
	public void theMainWindowOnEveryPage()
	{
		BufferedImage scene = scene();
		BufferedImage clean = copy(scene);
		WindowStyle.share((name, box) -> RingWash.wash(clean, box));
		try
		{
			for (WindowTab tab : WindowTab.values())
			{
				PartyRig rig = new PartyRig();
				rig.theme = WindowTheme.RUNELITE_GLASS;
				rig.width = 760;
				rig.height = 500;
				rig.publish(PartyRig.state(PartyRig.team(6)), true);
				rig.open();
				rig.shell.showTab(tab, rig.now);
				rig.now += 1_000;
				BufferedImage canvas = copy(scene);
				Graphics2D g = WidgetKit.graphics(canvas);
				g.translate(560, 140);
				rig.shell.paint(g, rig.width, rig.height, rig.now);
				g.dispose();
				write(canvas, new Rectangle(490, 70, 900, 640), "window-" + tab.name().toLowerCase());
			}
		}
		finally
		{
			WindowStyle.share(null);
		}
	}

	/**
	 * Paints the sheet of every primitive in glass at scales 1.0, 1.5 and 2.0.
	 */
	@Test
	public void everyPrimitiveAtThreeScales()
	{
		folder();
		for (double scale : new double[] {1.0, 1.5, 2.0})
		{
			BufferedImage sheet = LookSheet.primitives(WindowTheme.RUNELITE_GLASS, scale);
			write(sheet, new Rectangle(0, 0, sheet.getWidth(), sheet.getHeight()), "primitives-" + scale);
			BufferedImage card = LookSheet.card(WindowTheme.RUNELITE_GLASS, scale);
			write(card, new Rectangle(0, 0, card.getWidth(), card.getHeight()), "card-sheet-" + scale);
		}
	}
}
