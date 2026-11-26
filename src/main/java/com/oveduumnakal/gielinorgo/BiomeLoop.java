/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import javax.imageio.ImageIO;

import com.google.gson.Gson;

/**
 * One biome's animated background as it is bundled: a reference picture and, for each frame, its difference from that
 * picture halved around 128, still compressed. {@code scripts/pack-backgrounds.py} writes the files. A frame is
 * rebuilt when it is wanted, so only the compressed bytes are held. Holds no RuneLite types.
 */
final class BiomeLoop
{
	/** The most frames a loop may claim, so a damaged manifest cannot ask for an endless read. */
	private static final int MOST_FRAMES = 512;

	/** What {@code manifest.json} says. */
	private static final class Manifest
	{
		private int frames;
		private double fps;
	}

	private final int width;
	private final int height;
	private final int[] reference;
	private final byte[][] differences;
	private final double fps;

	private BiomeLoop(BufferedImage reference, byte[][] differences, double fps)
	{
		this.width = reference.getWidth();
		this.height = reference.getHeight();
		this.reference = reference.getRGB(0, 0, width, height, null, 0, width);
		this.differences = differences;
		this.fps = fps;
	}

	/**
	 * Reads a bundled loop. This reads files, so it is not for the game thread.
	 *
	 * @param gson the Gson to read the manifest with
	 * @param biome the loop's folder name
	 * @return the loop
	 * @throws IOException when a file is missing or cannot be read
	 */
	static BiomeLoop load(Gson gson, String biome) throws IOException
	{
		String folder = "biomes/" + biome + "/";
		Manifest manifest;
		try (Reader reader = new InputStreamReader(open(folder + "manifest.json"), StandardCharsets.UTF_8))
		{
			manifest = gson.fromJson(reader, Manifest.class);
		}

		if (manifest == null || manifest.frames < 1 || manifest.frames > MOST_FRAMES || !(manifest.fps > 0))
			throw new IOException("Unusable manifest for " + biome);

		BufferedImage reference;
		try (InputStream in = open(folder + "reference.jpg"))
		{
			reference = ImageIO.read(in);
		}

		byte[][] differences = new byte[manifest.frames][];
		for (int i = 0; i < manifest.frames; i++)
		{
			try (InputStream in = open(folder + String.format(Locale.ROOT, "%03d.jpg", i)))
			{
				differences[i] = in.readAllBytes();
			}
		}

		if (reference == null)
			throw new IOException("Unusable reference picture for " + biome);

		return new BiomeLoop(reference, differences, manifest.fps);
	}

	private static InputStream open(String name) throws IOException
	{
		InputStream in = BiomeLoop.class.getResourceAsStream(name);
		if (in == null)
			throw new IOException("Missing " + name);

		return in;
	}

	/** @return how many frames there are */
	int frames()
	{
		return differences.length;
	}

	/**
	 * Picks the frame to show, by the clock, so the loop runs at its own speed at any frame rate.
	 *
	 * @param nowMs the time in milliseconds
	 * @return the frame's number
	 */
	int frameAt(long nowMs)
	{
		return frameAt(nowMs, fps, differences.length);
	}

	/**
	 * @param nowMs the time in milliseconds
	 * @param fps frames a second
	 * @param count how many frames
	 * @return the frame's number, from 0
	 */
	static int frameAt(long nowMs, double fps, int count)
	{
		return (int) Math.floorMod((long) Math.floor(nowMs * fps / 1000.0), (long) count);
	}

	/**
	 * Rebuilds a frame. This decodes a picture, so it is not for the game thread.
	 *
	 * @param index the frame's number
	 * @return the picture
	 * @throws IOException when the frame cannot be read or is the wrong size
	 */
	BufferedImage decode(int index) throws IOException
	{
		BufferedImage packed = ImageIO.read(new ByteArrayInputStream(differences[index]));
		if (packed == null || packed.getWidth() != width || packed.getHeight() != height)
			throw new IOException("Unusable frame " + index);

		int[] pixels = packed.getRGB(0, 0, width, height, null, 0, width);
		for (int i = 0; i < pixels.length; i++)
			pixels[i] = rebuild(reference[i], pixels[i]);

		BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		out.setRGB(0, 0, width, height, pixels, 0, width);
		return out;
	}

	/**
	 * Rebuilds one pixel: each channel is the reference plus twice the difference's distance from 128.
	 *
	 * @param reference the reference pixel
	 * @param difference the packed difference
	 * @return the pixel of the frame, solid
	 */
	static int rebuild(int reference, int difference)
	{
		int pixel = 0xFF000000;
		for (int shift = 0; shift <= 16; shift += 8)
		{
			int value = (reference >> shift & 255) + 2 * ((difference >> shift & 255) - 128);
			pixel |= Math.max(0, Math.min(255, value)) << shift;
		}

		return pixel;
	}
}
