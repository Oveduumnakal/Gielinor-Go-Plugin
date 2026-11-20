/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Makes the "ambient ring" backdrop for the frosted-glass Info card (#294). A finished frame already holds the card,
 * so the pixels under it cannot be used; instead this reads a thin band of the frame just outside the card on all
 * four sides, averages each side into a row of colour cells, and fills the inside by blending the four sides, nearer
 * sides weighing more. The result is a soft wash in the scene's own colours, with no shapes, small enough to be
 * stretched to the card. Holds no RuneLite types.
 */
final class RingWash
{
	/** Frame pixels left between the card's footprint and the band, so its edge and shadow are not read. */
	static final int GAP = 3;

	/** The thickness of the band read on each side, in frame pixels. */
	static final int BAND = 6;

	/** How many frame pixels one cell of the wash covers along each side. */
	static final int CELL = 8;

	/**
	 * How much stronger the wash's colours are made than the scene's, as frosted glass does; 1 keeps them as they are.
	 * Averaging a busy scene greys it, so this brings the colour back.
	 */
	static final float SATURATION = 1.6f;

	/** How many times the wash is softened with a three-cell average. */
	static final int PASSES = 2;

	private static final int RED = 0;
	private static final int GREEN = 1;
	private static final int BLUE = 2;

	private RingWash()
	{
	}

	/**
	 * Builds the wash for a card.
	 *
	 * @param frame the finished frame
	 * @param card the card's footprint (body and shadow), in frame pixels
	 * @return the wash, {@link #CELL} times smaller than the card (at least two cells each way), or null when no
	 *         side of the band is on the frame
	 */
	static BufferedImage wash(BufferedImage frame, Rectangle card)
	{
		if (card.width <= 0 || card.height <= 0)
			return null;

		int cols = Math.max(2, card.width / CELL);
		int rows = Math.max(2, card.height / CELL);
		int out = GAP + BAND;
		Rectangle frameBox = new Rectangle(0, 0, frame.getWidth(), frame.getHeight());
		double[][] top = side(frame, frameBox, new Rectangle(card.x, card.y - out, card.width, BAND), cols, true);
		double[][] bottom = side(frame, frameBox, new Rectangle(card.x, card.y + card.height + GAP, card.width, BAND),
			cols, true);
		double[][] left = side(frame, frameBox, new Rectangle(card.x - out, card.y, BAND, card.height), rows, false);
		double[][] right = side(frame, frameBox, new Rectangle(card.x + card.width + GAP, card.y, BAND, card.height),
			rows, false);
		if (top == null && bottom == null && left == null && right == null)
			return null;

		double[][][] cells = new double[rows][cols][];
		double reach = GAP + BAND / 2.0;
		for (int j = 0; j < rows; j++)
		{
			double v = (j + 0.5) / rows;
			for (int i = 0; i < cols; i++)
			{
				double u = (i + 0.5) / cols;
				double[] sum = new double[3];
				double weight = 0;
				weight += add(sum, top == null ? null : top[i], v * card.height + reach);
				weight += add(sum, bottom == null ? null : bottom[i], (1 - v) * card.height + reach);
				weight += add(sum, left == null ? null : left[j], u * card.width + reach);
				weight += add(sum, right == null ? null : right[j], (1 - u) * card.width + reach);
				cells[j][i] = new double[] {sum[RED] / weight, sum[GREEN] / weight, sum[BLUE] / weight};
			}
		}

		for (int p = 0; p < PASSES; p++)
			cells = soften(cells);

		BufferedImage image = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_RGB);
		for (int j = 0; j < rows; j++)
		{
			for (int i = 0; i < cols; i++)
				image.setRGB(i, j, vivid(rgb(cells[j][i])));
		}

		return image;
	}

	/**
	 * Mixes a new wash into the one before, so the colours drift instead of jumping when the scene changes.
	 *
	 * @param before the wash before, or null
	 * @param next the new wash
	 * @param amount how much of the new wash to take, from 0 to 1
	 * @return the mix, or {@code next} itself when the two differ in size or there is nothing before
	 */
	static BufferedImage blend(BufferedImage before, BufferedImage next, double amount)
	{
		if (before == null || before.getWidth() != next.getWidth() || before.getHeight() != next.getHeight())
			return next;

		double t = Math.max(0.0, Math.min(1.0, amount));
		BufferedImage image = new BufferedImage(next.getWidth(), next.getHeight(), BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < next.getHeight(); y++)
		{
			for (int x = 0; x < next.getWidth(); x++)
			{
				int a = before.getRGB(x, y);
				int b = next.getRGB(x, y);
				int r = mix(a >> 16 & 0xFF, b >> 16 & 0xFF, t);
				int g = mix(a >> 8 & 0xFF, b >> 8 & 0xFF, t);
				int bl = mix(a & 0xFF, b & 0xFF, t);
				image.setRGB(x, y, r << 16 | g << 8 | bl);
			}
		}

		return image;
	}

	private static double add(double[] sum, double[] color, double distance)
	{
		if (color == null)
			return 0;

		double w = 1.0 / (distance * distance);
		sum[RED] += color[RED] * w;
		sum[GREEN] += color[GREEN] * w;
		sum[BLUE] += color[BLUE] * w;
		return w;
	}

	/**
	 * Averages one side of the band into cells. Cells that fall off the frame copy their nearest neighbour on the
	 * frame.
	 *
	 * @return one colour per cell, or null when the whole side is off the frame
	 */
	private static double[][] side(BufferedImage frame, Rectangle frameBox, Rectangle band, int count,
		boolean across)
	{
		Rectangle on = band.intersection(frameBox);
		if (on.isEmpty())
			return null;

		int[] pixels = frame.getRGB(on.x, on.y, on.width, on.height, null, 0, on.width);
		double[][] cells = new double[count][];
		int length = across ? band.width : band.height;
		for (int c = 0; c < count; c++)
		{
			int from = length * c / count;
			int to = Math.max(from + 1, length * (c + 1) / count);
			double[] sum = new double[3];
			int n = 0;
			for (int a = from; a < to; a++)
			{
				int along = (across ? band.x : band.y) + a;
				int start = across ? on.x : on.y;
				int end = start + (across ? on.width : on.height);
				if (along < start || along >= end)
					continue;

				int depth = across ? on.height : on.width;
				for (int d = 0; d < depth; d++)
				{
					int index = across ? d * on.width + (along - on.x) : (along - on.y) * on.width + d;
					int p = pixels[index];
					sum[RED] += p >> 16 & 0xFF;
					sum[GREEN] += p >> 8 & 0xFF;
					sum[BLUE] += p & 0xFF;
					n++;
				}
			}

			if (n > 0)
				cells[c] = new double[] {sum[RED] / n, sum[GREEN] / n, sum[BLUE] / n};
		}

		fillGaps(cells);
		return cells;
	}

	private static void fillGaps(double[][] cells)
	{
		double[] last = null;
		for (int c = 0; c < cells.length; c++)
		{
			if (cells[c] != null)
				last = cells[c];
			else if (last != null)
				cells[c] = last;
		}

		last = null;
		for (int c = cells.length - 1; c >= 0; c--)
		{
			if (cells[c] != null)
				last = cells[c];
			else
				cells[c] = last;
		}
	}

	private static double[][][] soften(double[][][] cells)
	{
		int rows = cells.length;
		int cols = cells[0].length;
		double[][][] out = new double[rows][cols][];
		for (int j = 0; j < rows; j++)
		{
			for (int i = 0; i < cols; i++)
			{
				double[] sum = new double[3];
				int n = 0;
				for (int dj = -1; dj <= 1; dj++)
				{
					for (int di = -1; di <= 1; di++)
					{
						int y = Math.max(0, Math.min(rows - 1, j + dj));
						int x = Math.max(0, Math.min(cols - 1, i + di));
						sum[RED] += cells[y][x][RED];
						sum[GREEN] += cells[y][x][GREEN];
						sum[BLUE] += cells[y][x][BLUE];
						n++;
					}
				}

				out[j][i] = new double[] {sum[RED] / n, sum[GREEN] / n, sum[BLUE] / n};
			}
		}

		return out;
	}

	private static int vivid(int rgb)
	{
		float[] hsb = Color.RGBtoHSB(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, null);
		return Color.HSBtoRGB(hsb[0], Math.min(1f, hsb[1] * SATURATION), hsb[2]) & 0xFFFFFF;
	}

	private static int rgb(double[] color)
	{
		return channel(color[RED]) << 16 | channel(color[GREEN]) << 8 | channel(color[BLUE]);
	}

	private static int channel(double value)
	{
		return (int) Math.max(0, Math.min(255, Math.round(value)));
	}

	private static int mix(int from, int to, double t)
	{
		return (int) Math.round(from + (to - from) * t);
	}
}
