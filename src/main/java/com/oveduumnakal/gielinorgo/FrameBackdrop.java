/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.BufferProvider;
import net.runelite.api.Client;
import net.runelite.client.ui.DrawManager;

/**
 * The blur probe for the frosted-glass Info card (#294): reads the game pixels behind the card and blurs them. Off
 * unless the client is started with the JVM property {@value #PROPERTY} set to {@code buffer} or {@code frame}; there
 * is no setting. {@code buffer} reads the client's own pixel buffer at the moment the card is painted (expected to
 * hold the 3D scene only without the GPU plugin). {@code frame} asks for the next finished frame (GPU included), so
 * it is one frame late and may show the card itself. Every five seconds it logs at debug how long the reads took.
 */
final class FrameBackdrop implements CardBackdrop
{
	/** The JVM property that picks the mode: {@code none} (the default), {@code buffer} or {@code frame}. */
	static final String PROPERTY = "gielinorgo.glassBackdrop";

	private static final Logger log = LoggerFactory.getLogger(FrameBackdrop.class);
	private static final long REPORT_NANOS = 5_000_000_000L;

	/** Where the backdrop comes from. */
	enum Mode
	{
		/** No backdrop. */
		NONE,
		/** The client's pixel buffer, read when the card is painted. */
		BUFFER,
		/** The next finished frame, read when it arrives. */
		FRAME;

		/**
		 * Reads a mode from the property's text.
		 *
		 * @param text the property value, or null
		 * @return the mode it names, or {@link #NONE} for anything else
		 */
		static Mode parse(String text)
		{
			if (text == null)
				return NONE;

			String t = text.trim().toLowerCase(Locale.ROOT);
			if ("buffer".equals(t))
				return BUFFER;

			if ("frame".equals(t))
				return FRAME;

			return NONE;
		}
	}

	private final Client client;
	private final DrawManager drawManager;
	private final Mode mode;
	private volatile BufferedImage lastFrame;
	private volatile boolean waiting;
	private volatile Rectangle wanted;
	private long nanos;
	private int frames;
	private long reportedAt;

	private FrameBackdrop(Client client, DrawManager drawManager, Mode mode)
	{
		this.client = client;
		this.drawManager = drawManager;
		this.mode = mode;
	}

	/**
	 * Gives the backdrop the JVM property asks for.
	 *
	 * @param client the client
	 * @param drawManager the draw manager, for the {@code frame} mode
	 * @return the probe, or {@link CardBackdrop#NONE} when the property is unset or names no mode
	 */
	static CardBackdrop create(Client client, DrawManager drawManager)
	{
		Mode mode = Mode.parse(System.getProperty(PROPERTY));
		if (mode == Mode.NONE)
			return CardBackdrop.NONE;

		log.debug("Gielinor Go: glass backdrop probe on, mode={}", mode);
		return new FrameBackdrop(client, drawManager, mode);
	}

	@Override
	public BufferedImage behind(Rectangle screenBox)
	{
		long start = System.nanoTime();
		BufferedImage out = mode == Mode.BUFFER ? fromBuffer(screenBox) : fromFrame(screenBox);
		count(System.nanoTime() - start);
		return out;
	}

	private BufferedImage fromBuffer(Rectangle box)
	{
		BufferProvider buffer = client.getBufferProvider();
		if (buffer == null)
			return null;

		return BackdropBlur.blur(buffer.getPixels(), buffer.getWidth(), buffer.getHeight(), box);
	}

	private BufferedImage fromFrame(Rectangle box)
	{
		wanted = new Rectangle(box);
		if (!waiting)
		{
			waiting = true;
			drawManager.requestNextFrameListener(this::arrived);
		}

		return lastFrame;
	}

	private void arrived(Image image)
	{
		long start = System.nanoTime();
		try
		{
			Rectangle box = wanted;
			if (box == null || box.isEmpty())
				return;

			BufferedImage crop = new BufferedImage(box.width, box.height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = crop.createGraphics();
			try
			{
				g.drawImage(image, -box.x, -box.y, null);
			}
			finally
			{
				g.dispose();
			}

			lastFrame = BackdropBlur.blur(crop);
		}
		finally
		{
			waiting = false;
			count(System.nanoTime() - start);
		}
	}

	private synchronized void count(long took)
	{
		nanos += took;
		frames++;
		long now = System.nanoTime();
		if (reportedAt == 0)
		{
			reportedAt = now;
		}
		else if (now - reportedAt >= REPORT_NANOS)
		{
			log.debug("glass backdrop: mode={} gpu={} mean={}us frames={}", mode.name().toLowerCase(Locale.ROOT),
				client.isGpu(), nanos / 1000 / Math.max(1, frames), frames);
			nanos = 0;
			frames = 0;
			reportedAt = now;
		}
	}
}
