/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.client.ui.DrawManager;

/**
 * The one service every glass surface gets its wash from (#324). A surface asks by name with its box on the screen; it
 * is handed the last wash made for it, or null. While any surface asked in the last {@value
 * GlassTokens#WASH_FORGET_MS} ms and the Glass look is on, it asks {@link DrawManager} for one finished frame, at most
 * every {@value GlassTokens#WASH_REFRESH_MS} ms; when the frame arrives it builds one {@link RingWash} per live
 * surface (the wash reads a thin band just outside the box, so it works with the GPU plugin on) and blends it half and
 * half into the last wash of the same size, so colours drift. A surface that stops asking is forgotten. With the
 * Glass look off it never asks for a frame. Every five seconds it logs at debug how long the washes took.
 */
final class Backdrops implements WashSource
{
	/** How much of each new wash is mixed into the one shown. */
	static final double BLEND = 0.5;

	private static final Logger log = LoggerFactory.getLogger(Backdrops.class);
	private static final long REPORT_NANOS = 5_000_000_000L;

	private final Client client;
	private final DrawManager drawManager;
	private final BooleanSupplier glassOn;
	private final LongSupplier clock;
	private final Map<String, Surface> surfaces = new ConcurrentHashMap<>();
	private volatile boolean waiting;
	private volatile long requestedAt;
	private long nanos;
	private int frames;
	private long reportedAt;

	/**
	 * Creates the service.
	 *
	 * @param client the client, for the canvas size
	 * @param drawManager the draw manager that hands out finished frames
	 * @param glassOn tells whether the Glass look is on; with it off no frame is ever requested
	 */
	Backdrops(Client client, DrawManager drawManager, BooleanSupplier glassOn)
	{
		this(client, drawManager, glassOn, System::currentTimeMillis);
	}

	/**
	 * Creates the service with a clock of its own, so a test can move time.
	 *
	 * @param client the client, for the canvas size
	 * @param drawManager the draw manager that hands out finished frames
	 * @param glassOn tells whether the Glass look is on
	 * @param clock the time in milliseconds
	 */
	Backdrops(Client client, DrawManager drawManager, BooleanSupplier glassOn, LongSupplier clock)
	{
		this.client = client;
		this.drawManager = drawManager;
		this.glassOn = glassOn;
		this.clock = clock;
	}

	@Override
	public BufferedImage wash(String surface, Rectangle screenBox)
	{
		if (!glassOn.getAsBoolean())
		{
			surfaces.clear();
			return null;
		}

		long now = clock.getAsLong();
		forget(now);
		Surface s = surfaces.computeIfAbsent(surface, name -> new Surface());
		s.box = new Rectangle(screenBox);
		s.askedAt = now;
		if (!waiting && now - requestedAt >= GlassTokens.WASH_REFRESH_MS)
		{
			waiting = true;
			requestedAt = now;
			drawManager.requestNextFrameListener(this::arrived);
		}

		return s.wash;
	}

	/** @return how many surfaces are being kept; for tests */
	int surfaces()
	{
		return surfaces.size();
	}

	private void forget(long now)
	{
		Iterator<Surface> it = surfaces.values().iterator();
		while (it.hasNext())
		{
			if (now - it.next().askedAt > GlassTokens.WASH_FORGET_MS)
				it.remove();
		}
	}

	/**
	 * Builds a wash for each live surface from a finished frame.
	 *
	 * @param image the frame
	 */
	void arrived(Image image)
	{
		long start = System.nanoTime();
		try
		{
			BufferedImage frame = asBuffered(image);
			if (frame == null)
				return;

			long now = clock.getAsLong();
			forget(now);
			for (Surface s : surfaces.values())
			{
				Rectangle box = s.box;
				if (box == null || box.isEmpty())
					continue;

				BufferedImage next = RingWash.wash(frame, toFrame(box, frame));
				if (next != null)
					s.wash = RingWash.blend(s.wash, next, BLEND);
			}
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
			log.debug("glass: surfaces={} mean={}us frames={}", surfaces.size(), nanos / 1000 / Math.max(1, frames),
				frames);
			nanos = 0;
			frames = 0;
			reportedAt = now;
		}
	}

	/** One surface that asked: where it is, when it last asked and the wash made for it. */
	private static final class Surface
	{
		private volatile Rectangle box;
		private volatile long askedAt;
		private volatile BufferedImage wash;
	}
}
