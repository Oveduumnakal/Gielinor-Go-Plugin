/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import net.runelite.api.Client;
import net.runelite.client.ui.DrawManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests the one service every glass surface gets its wash from. */
public class BackdropsTest
{
	private final Client client = mock(Client.class);
	private final DrawManager drawManager = mock(DrawManager.class);
	private final AtomicBoolean glass = new AtomicBoolean(true);
	private final AtomicLong now = new AtomicLong(1000);
	private final Backdrops backdrops = new Backdrops(client, drawManager, glass::get, now::get);

	private static BufferedImage frame(int rgb)
	{
		BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setColor(new Color(rgb));
		g.fillRect(0, 0, 400, 300);
		g.dispose();
		return image;
	}

	@SuppressWarnings("unchecked")
	private Consumer<java.awt.Image> listener(int times)
	{
		ArgumentCaptor<Consumer<java.awt.Image>> captor = ArgumentCaptor.forClass(Consumer.class);
		verify(drawManager, times(times)).requestNextFrameListener(captor.capture());
		return captor.getValue();
	}

	@Test
	public void theFirstAskGetsNothingAndRequestsOneFrame()
	{
		assertNull(backdrops.wash("window", new Rectangle(100, 100, 120, 80)));
		assertNull(backdrops.wash("card", new Rectangle(10, 10, 60, 40)));
		listener(1);
	}

	@Test
	public void aFrameBuildsOneWashPerLiveSurface()
	{
		backdrops.wash("window", new Rectangle(100, 100, 120, 80));
		backdrops.wash("card", new Rectangle(250, 100, 60, 40));
		listener(1).accept(frame(0x2040C0));
		BufferedImage window = backdrops.wash("window", new Rectangle(100, 100, 120, 80));
		BufferedImage card = backdrops.wash("card", new Rectangle(250, 100, 60, 40));
		assertNotNull(window);
		assertNotNull(card);
		assertTrue((window.getRGB(1, 1) & 0xFF) > (window.getRGB(1, 1) >> 16 & 0xFF));
		assertEquals(2, backdrops.surfaces());
	}

	@Test
	public void itAsksAtMostEveryHundredMilliseconds()
	{
		backdrops.wash("window", new Rectangle(100, 100, 120, 80));
		listener(1).accept(frame(0x808080));
		now.addAndGet(50);
		backdrops.wash("window", new Rectangle(100, 100, 120, 80));
		verify(drawManager, times(1)).requestNextFrameListener(any());
		now.addAndGet(60);
		backdrops.wash("window", new Rectangle(100, 100, 120, 80));
		verify(drawManager, times(2)).requestNextFrameListener(any());
	}

	@Test
	public void aSurfaceThatStopsAskingIsForgotten()
	{
		backdrops.wash("window", new Rectangle(100, 100, 120, 80));
		listener(1).accept(frame(0x808080));
		assertNotNull(backdrops.wash("window", new Rectangle(100, 100, 120, 80)));
		now.addAndGet(GlassTokens.WASH_FORGET_MS + 1);
		assertNull(backdrops.wash("window", new Rectangle(100, 100, 120, 80)));
	}

	@Test
	public void aNewWashIsBlendedIntoTheLastOne()
	{
		backdrops.wash("window", new Rectangle(100, 100, 120, 80));
		listener(1).accept(frame(0x000000));
		now.addAndGet(110);
		backdrops.wash("window", new Rectangle(100, 100, 120, 80));
		listener(2).accept(frame(0xFFFFFF));
		int red = backdrops.wash("window", new Rectangle(100, 100, 120, 80)).getRGB(1, 1) >> 16 & 0xFF;
		assertTrue("red " + red, red > 100 && red < 160);
	}

	@Test
	public void withTheLookOffItNeverRequestsAFrame()
	{
		glass.set(false);
		assertNull(backdrops.wash("window", new Rectangle(100, 100, 120, 80)));
		verify(drawManager, never()).requestNextFrameListener(any());
		assertEquals(0, backdrops.surfaces());
	}

	@Test
	public void aBiggerFrameThanTheCanvasScalesTheBoxes()
	{
		when(client.getCanvasWidth()).thenReturn(200);
		when(client.getCanvasHeight()).thenReturn(150);
		backdrops.wash("window", new Rectangle(50, 40, 60, 40));
		listener(1).accept(frame(0xC04020));
		assertNotNull(backdrops.wash("window", new Rectangle(50, 40, 60, 40)));
	}
}
