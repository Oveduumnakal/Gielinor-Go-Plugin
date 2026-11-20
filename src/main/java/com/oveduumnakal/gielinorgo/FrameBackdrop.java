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
 * Gives the frosted-glass Info card (#294) its backdrop from the game's pixels. The mode comes from the JVM property
 * {@value #PROPERTY}; there is no setting. {@code ring} (the default) reads the finished frame a few times a second and
 * turns a thin band just outside the card into a soft colour wash ({@link RingWash}); it works with the GPU plugin on,
 * because it never needs the pixels under the card. {@code buffer} blurs the client's own pixel buffer under the card
 * (the 3D scene is there only with the GPU plugin off). {@code frame} blurs the next finished frame under the card,
 * which already holds the card, so it blurs itself. {@code none} turns the backdrop off. Every five seconds it logs at
 * debug how long the reads took.
 */
final class FrameBackdrop implements CardBackdrop
{
	/**
	 * The JVM property that picks the mode: {@code ring} (the default), {@code buffer}, {@code frame} or
	 * {@code none}.
	 */
	static final String PROPERTY = "gielinorgo.glassBackdrop";

	private static final Logger log = LoggerFactory.getLogger(FrameBackdrop.class);
	private static final long REPORT_NANOS = 5_000_000_000L;

	/** How often the ring mode asks for a new frame while the card is shown, in milliseconds. */
	static final long RING_REFRESH_MS = 100;

	/** After this long without the card being painted, the ring mode forgets its wash, in milliseconds. */
	static final long RING_FORGET_MS = 250;

	/** How much of each new wash the ring mode mixes into the one shown. */
	static final double RING_BLEND = 0.5;

	/** Where the backdrop comes from. */
	enum Mode
	{
		/** No backdrop. */
		NONE,
		/** A colour wash from a band of the finished frame just outside the card. */
		RING,
		/** The client's pixel buffer, read when the card is painted. */
		BUFFER,
		/** The next finished frame, read when it arrives. */
		FRAME;

		/**
		 * Reads a mode from the property's text.
		 *
		 * @param text the property value, or null
		 * @return the mode it names, {@link #RING} when there is no text, or {@link #NONE} for anything else
		 */
		static Mode parse(String text)
		{
			if (text == null)
				return RING;

			String t = text.trim().toLowerCase(Locale.ROOT);
			if ("ring".equals(t))
				return RING;

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
	private volatile BufferedImage wash;
	private volatile long askedAt;
	private long requestedAt;
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
	 * @return the backdrop, or {@link CardBackdrop#NONE} when the property turns it off
	 */
	static CardBackdrop create(Client client, DrawManager drawManager)
	{
		Mode mode = Mode.parse(System.getProperty(PROPERTY));
		if (mode == Mode.NONE)
			return CardBackdrop.NONE;

		log.debug("Gielinor Go: glass backdrop on, mode={}", mode);
		return new FrameBackdrop(client, drawManager, mode);
	}

	@Override
	public BufferedImage behind(Rectangle screenBox)
	{
		if (mode == Mode.RING)
			return fromRing(screenBox);

		long start = System.nanoTime();
		BufferedImage out = mode == Mode.BUFFER ? fromBuffer(screenBox) : fromFrame(screenBox);
		count(System.nanoTime() - start);
		return out;
	}

	private BufferedImage fromRing(Rectangle box)
	{
		long now = System.currentTimeMillis();
		if (now - askedAt > RING_FORGET_MS)
		{
			wash = null;
			requestedAt = 0;
		}

		askedAt = now;
		wanted = new Rectangle(box);
		if (!waiting && now - requestedAt >= RING_REFRESH_MS)
		{
			waiting = true;
			requestedAt = now;
			drawManager.requestNextFrameListener(this::ringArrived);
		}

		return wash;
	}

	private void ringArrived(Image image)
	{
		long start = System.nanoTime();
		try
		{
			Rectangle box = wanted;
			BufferedImage frame = asBuffered(image);
			if (box == null || box.isEmpty() || frame == null)
				return;

			BufferedImage next = RingWash.wash(frame, toFrame(box, frame));
			if (next != null)
				wash = RingWash.blend(wash, next, RING_BLEND);
		}
		finally
		{
			waiting = false;
			count(System.nanoTime() - start);
		}
	}

	/**
	 * Turns a box in the overlay's pixels into the frame's pixels. The frame can be bigger than the canvas the
	 * overlays draw on (stretched mode, a scaled display), so the box is scaled by the ratio of the two.
	 */
	private Rectangle toFrame(Rectangle box, BufferedImage frame)
	{
		int canvasW = client.getCanvasWidth();
		int canvasH = client.getCanvasHeight();
		if (canvasW <= 0 || canvasH <= 0 || (canvasW == frame.getWidth() && canvasH == frame.getHeight()))
			return box;

		double sx = frame.getWidth() / (double) canvasW;
		double sy = frame.getHeight() / (double) canvasH;
		int x = (int) Math.floor(box.x * sx);
		int y = (int) Math.floor(box.y * sy);
		return new Rectangle(x, y, (int) Math.ceil((box.x + box.width) * sx) - x,
			(int) Math.ceil((box.y + box.height) * sy) - y);
	}

	private static BufferedImage asBuffered(Image image)
	{
		if (image instanceof BufferedImage)
			return (BufferedImage) image;

		int w = image.getWidth(null);
		int h = image.getHeight(null);
		if (w <= 0 || h <= 0)
			return null;

		BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = copy.createGraphics();
		try
		{
			g.drawImage(image, 0, 0, null);
		}
		finally
		{
			g.dispose();
		}

		return copy;
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
