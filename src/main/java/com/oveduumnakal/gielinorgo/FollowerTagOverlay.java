/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Draws the lead creature's name above the follower: its nickname, or its species name when it has none. */
final class FollowerTagOverlay extends Overlay
{
	private static final Logger log = LoggerFactory.getLogger(FollowerTagOverlay.class);
	private static final int HEAD_GAP = 30;
	/** Opacity of the tag text and its shadow, from 0 to 255: half. */
	static final int TAG_ALPHA = 128;
	private static final Color TAG_COLOR = new Color(255, 255, 255, TAG_ALPHA);
	private static final Color SHADOW_COLOR = new Color(0, 0, 0, TAG_ALPHA);

	private final Client client;
	private final FollowerService follower;
	private final Supplier<String> tag;

	/**
	 * Creates the overlay.
	 *
	 * @param client the game client
	 * @param follower the follower to draw over
	 * @param tag gives the text to draw, or null for none; read on the render thread, so it must be a cached value
	 */
	FollowerTagOverlay(Client client, FollowerService follower, Supplier<String> tag)
	{
		this.client = client;
		this.follower = follower;
		this.tag = tag;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		try
		{
			draw(graphics);
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not draw the follower's name", e);
		}

		return null;
	}

	/**
	 * Draws the name over the follower, when there is a name and the follower is on screen.
	 *
	 * @param graphics the canvas
	 */
	private void draw(Graphics2D graphics)
	{
		String text = tag.get();
		if (text == null || !follower.shown())
			return;

		LocalPoint ground = follower.groundPoint();
		if (ground == null)
			return;

		graphics.setFont(FontManager.getRunescapeBoldFont());
		Point at = Perspective.getCanvasTextLocation(client, graphics, ground, text,
			follower.tagHeight() + HEAD_GAP);
		if (at == null)
			return;

		graphics.setColor(SHADOW_COLOR);
		graphics.drawString(text, at.getX() + 1, at.getY() + 1);
		graphics.setColor(TAG_COLOR);
		graphics.drawString(text, at.getX(), at.getY());
	}

	/**
	 * Picks the text the tag shows for a creature.
	 *
	 * @param nickname the creature's nickname, or null for none
	 * @param species the creature's species name
	 * @return the nickname when there is one, otherwise the species name
	 */
	static String nameFor(String nickname, String species)
	{
		return nickname == null || nickname.isEmpty() ? species : nickname;
	}
}
