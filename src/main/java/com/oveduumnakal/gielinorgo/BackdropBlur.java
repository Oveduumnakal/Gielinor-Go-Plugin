/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/**
 * Blurs a part of a frame for the frosted-glass card (#294): crops it, shrinks it four times with bilinear sampling,
 * then runs a 3x3 box blur over it twice. The result is small; the glass stretches it back over the card, which
 * softens it further. Uses only the JDK. Holds no RuneLite types.
 */
final class BackdropBlur
{
	/** How many times smaller than the region the blurred picture is. */
	static final int SHRINK = 4;

	/** How many times the box blur runs. */
	static final int PASSES = 2;

	private static final float NINTH = 1f / 9f;
	private static final ConvolveOp BOX = new ConvolveOp(new Kernel(3, 3, new float[] {
		NINTH, NINTH, NINTH,
		NINTH, NINTH, NINTH,
		NINTH, NINTH, NINTH
	}), ConvolveOp.EDGE_NO_OP, null);

	private BackdropBlur()
	{
	}

	/**
	 * Blurs a region of a frame given as pixels.
	 *
	 * @param argb the frame's pixels, row by row; the alpha byte is ignored
	 * @param width the frame width
	 * @param height the frame height
	 * @param region the part to blur, in frame pixels; kept inside the frame
	 * @return the blurred region, {@link #SHRINK} times smaller, or null when the region misses the frame
	 */
	static BufferedImage blur(int[] argb, int width, int height, Rectangle region)
	{
		Rectangle r = region.intersection(new Rectangle(0, 0, width, height));
		if (r.isEmpty() || argb.length < width * height)
			return null;

		BufferedImage crop = new BufferedImage(r.width, r.height, BufferedImage.TYPE_INT_RGB);
		crop.setRGB(0, 0, r.width, r.height, argb, r.y * width + r.x, width);
		return blur(crop);
	}

	/**
	 * Blurs a whole picture.
	 *
	 * @param crop the picture, already cut to the region
	 * @return the blurred picture, {@link #SHRINK} times smaller (at least one pixel each way)
	 */
	static BufferedImage blur(BufferedImage crop)
	{
		int w = Math.max(1, crop.getWidth() / SHRINK);
		int h = Math.max(1, crop.getHeight() / SHRINK);
		BufferedImage small = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = small.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawImage(crop, 0, 0, w, h, null);
		}
		finally
		{
			g.dispose();
		}

		BufferedImage out = small;
		for (int i = 0; i < PASSES; i++)
			out = BOX.filter(out, null);

		return out;
	}
}
