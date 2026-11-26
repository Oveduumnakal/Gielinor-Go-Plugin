/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

/**
 * The Summary page's portrait box with its biome behind it: the loop, scaled to cover the box, a soft shadow and the
 * creature standing on it, the two layers shifting a little with the mouse. When it cannot be shown the page paints
 * its plain portrait instead. Holds no RuneLite types.
 */
final class SummaryScene
{
	/** Where the creature's feet stand, as a share of the box's height. */
	static final double FEET = 0.90;

	/** The most of the box's width a creature takes. */
	private static final double WIDEST = 0.65;

	/** The most of the room between the chips and the feet a creature takes. */
	private static final double TALLEST = 0.88;

	/** How much larger than the box the background is drawn, so a shifted one shows no edge. */
	private static final double OVERSCAN = 1.0 + 2 * Parallax.BACKGROUND + 0.01;

	private static final Color SHADOW = new Color(0, 0, 0, 90);
	private static final Map<BufferedImage, Rectangle> TRIMMED = new WeakHashMap<>();

	private final BiomeTable biomes;
	private final BiomePlayer player;
	private final BooleanSupplier enabled;
	private final Parallax parallax = new Parallax();
	private long paintedAt;

	/**
	 * Creates the scene.
	 *
	 * @param biomes which biome each species lives in
	 * @param player plays the loops
	 * @param enabled whether the "Animated backgrounds" setting is on
	 */
	SummaryScene(BiomeTable biomes, BiomePlayer player, BooleanSupplier enabled)
	{
		this.biomes = biomes;
		this.player = player;
		this.enabled = enabled;
	}

	/**
	 * Paints the portrait box.
	 *
	 * @param g the graphics
	 * @param ctx the frame's facts
	 * @param pictures where creature pictures are
	 * @param row the creature
	 * @param box the portrait box
	 * @param top how far down the box the chips reach, which the creature stays under
	 * @param bob how far up the creature is drawn
	 * @return false when nothing was painted, so the plain portrait is wanted
	 */
	boolean paint(Graphics2D g, PageContext ctx, PortraitCache pictures, RowModel row, Rectangle2D box, double top,
		double bob, IdleStrip idle)
	{
		String biome = enabled.getAsBoolean() ? biomes.of(row.species()) : null;
		if (biome == null)
			return false;

		boolean moving = ctx.mode.loops();
		BufferedImage background = player.frame(biome, ctx.now, moving);
		if (background == null)
			return false;

		follow(ctx, moving);
		WindowStyle st = ctx.style;
		Shape clip = st.slant(box, st.metrics().slant / 2.0);
		Shape saved = g.getClip();
		g.clip(clip);
		Object hint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		Rectangle2D covered = cover(box, background.getWidth(), background.getHeight(), parallax.x(), parallax.y());
		g.drawImage(background, (int) Math.floor(covered.getX()), (int) Math.floor(covered.getY()),
			(int) Math.ceil(covered.getWidth()) + 1, (int) Math.ceil(covered.getHeight()) + 1, null);
		if (hint != null)
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);

		creature(g, ctx, pictures, row, box, new double[] {top, bob}, idle);
		g.setClip(saved);
		PortraitFrame.border(g, st, row, box, clip, ctx.now, ctx.mode);
		return true;
	}

	/** Glides the layers toward the mouse, from the middle again after the page was away. */
	private void follow(PageContext ctx, boolean moving)
	{
		if (ctx.now - paintedAt > BiomePlayer.IDLE_MS)
			parallax.rest();

		paintedAt = ctx.now;
		Point mouse = moving ? ctx.pointer() : null;
		Rectangle body = ctx.body;
		if (mouse == null || body.width <= 0 || body.height <= 0)
		{
			if (moving)
				parallax.toward(0, 0, ctx.now);
			else
				parallax.rest();

			return;
		}

		parallax.toward(2.0 * (mouse.x - body.x) / body.width - 1, 2.0 * (mouse.y - body.y) / body.height - 1, ctx.now);
	}

	private void creature(Graphics2D g, PageContext ctx, PortraitCache pictures, RowModel row, Rectangle2D box,
		double[] topAndBob, IdleStrip idle)
	{
		BufferedImage image = idle == null ? pictures.image(row.species(), row.view().npcId()) : idle.at(ctx.now);
		if (image == null)
			return;

		Rectangle shape = idle == null ? trimmed(image) : idle.shape();
		double dx = parallax.x() * Parallax.BACKGROUND * Parallax.MODEL * box.getWidth();
		double dy = parallax.y() * Parallax.BACKGROUND * Parallax.MODEL * box.getHeight();
		Rectangle2D stood = place(box, topAndBob[0], shape);
		g.setColor(SHADOW);
		g.fill(new Ellipse2D.Double(stood.getCenterX() + dx - stood.getWidth() * 0.4,
			stood.getMaxY() + dy - stood.getHeight() * 0.04, stood.getWidth() * 0.8,
			Math.max(2, stood.getHeight() * 0.08)));

		double scale = stood.getHeight() / shape.height;
		double side = image.getWidth() * scale;
		double left = stood.getX() + dx - shape.x * scale;
		double up = stood.getY() + dy - shape.y * scale - topAndBob[1];
		BufferedImage shown = row.desaturated() ? PortraitFrame.grey(image) : image;
		if (idle != null)
		{
			PortraitFrame.scaled(g, shown, left, up, side);
			return;
		}

		Composite saved = PageContext.fade(g, pictures.alpha(row.species(), row.view().npcId(), ctx.now, ctx.mode));
		CrispImages.draw(g, shown, left, up, side);
		g.setComposite(saved);
	}

	/**
	 * Gives where the background goes: scaled to cover the box with a little to spare, shifted against the mouse and
	 * never far enough to show an edge.
	 *
	 * @param box the portrait box
	 * @param width the background's width
	 * @param height the background's height
	 * @param mouseX the eased mouse position across, from -1 to 1
	 * @param mouseY the same, downward
	 * @return the rectangle to draw it in
	 */
	static Rectangle2D cover(Rectangle2D box, int width, int height, double mouseX, double mouseY)
	{
		double scale = Math.max(box.getWidth() / width, box.getHeight() / height) * OVERSCAN;
		double w = width * scale;
		double h = height * scale;
		return new Rectangle2D.Double(box.getCenterX() - w / 2 - mouseX * Parallax.BACKGROUND * box.getWidth(),
			box.getCenterY() - h / 2 - mouseY * Parallax.BACKGROUND * box.getHeight(), w, h);
	}

	/**
	 * Gives where the creature stands: its feet at {@link #FEET} of the box, centred, as large as fits under the chips.
	 *
	 * @param box the portrait box
	 * @param top how far down the box the chips reach
	 * @param shape the part of its picture the creature covers
	 * @return where that part goes
	 */
	static Rectangle2D place(Rectangle2D box, double top, Rectangle shape)
	{
		double feet = box.getY() + box.getHeight() * FEET;
		double room = Math.max(1, (feet - Math.max(box.getY(), top)) * TALLEST);
		double scale = Math.min(room / shape.height, box.getWidth() * WIDEST / shape.width);
		double w = shape.width * scale;
		double h = shape.height * scale;
		return new Rectangle2D.Double(box.getCenterX() - w / 2, feet - h, w, h);
	}

	/**
	 * Finds the part of a picture that is not empty.
	 *
	 * @param image the picture
	 * @return the smallest rectangle round everything seen, or the whole picture when nothing is
	 */
	static synchronized Rectangle trimmed(BufferedImage image)
	{
		Rectangle have = TRIMMED.get(image);
		if (have != null)
			return have;

		int left = image.getWidth();
		int top = image.getHeight();
		int right = -1;
		int bottom = -1;
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				if (image.getRGB(x, y) >>> 24 < 32)
					continue;

				left = Math.min(left, x);
				right = Math.max(right, x);
				top = Math.min(top, y);
				bottom = Math.max(bottom, y);
			}
		}

		Rectangle found = right < 0 ? new Rectangle(0, 0, image.getWidth(), image.getHeight())
			: new Rectangle(left, top, right - left + 1, bottom - top + 1);
		TRIMMED.put(image, found);
		return found;
	}
}
