package com.oveduumnakal.gielinorgo;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;

/** Stand-in portraits from the pictures in the lab's .portraits folder. */
final class LabPortraits
{
	private static final Map<String, BufferedImage> LOADED = new HashMap<>();

	/** The folder of pictures, or null to always use the stand-in square. */
	static Path folder;

	private LabPortraits()
	{
	}

	/** @return the species' picture fitted into a portrait's square, or the fallback when there is none */
	static BufferedImage of(String species, BufferedImage fallback)
	{
		if (folder == null || species == null)
			return fallback;

		String slug = species.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
		if (!LOADED.containsKey(slug))
			LOADED.put(slug, load(folder.resolve(slug + ".png")));

		BufferedImage picture = LOADED.get(slug);
		return picture == null ? fallback : picture;
	}

	private static BufferedImage load(Path file)
	{
		if (!Files.exists(file))
			return null;

		try
		{
			BufferedImage source = ImageIO.read(file.toFile());
			if (source == null)
				return null;

			int side = PortraitService.SIZE;
			double fit = (side - 8.0) / Math.max(source.getWidth(), source.getHeight());
			int w = Math.max(1, (int) Math.round(source.getWidth() * fit));
			int h = Math.max(1, (int) Math.round(source.getHeight() * fit));
			BufferedImage square = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = square.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.drawImage(source, (side - w) / 2, (side - h) / 2, w, h, null);
			g.dispose();
			return square;
		}
		catch (IOException e)
		{
			return null;
		}
	}
}
