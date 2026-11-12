/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws the thrown jar's picture flying in an arc from the player to the creature's body, on the player's own screen
 * only. The flight follows both of them while it lasts, and ends by itself when the jar lands. Every client call
 * is guarded, so a failure here can never break the drawing of the rest of the screen.
 */
final class JarFlightOverlay extends Overlay
{
	private static final Logger log = LoggerFactory.getLogger(JarFlightOverlay.class);

	/** The height of the player's hand above the ground, in local units. */
	private static final int HAND_HEIGHT = 100;

	private final Client client;
	private JarType jar;
	private Supplier<StageAnchors.Spot> target;
	private int age;

	/**
	 * Creates the overlay with no jar in the air.
	 *
	 * @param client the game client
	 */
	JarFlightOverlay(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	/**
	 * Starts a flight to a creature's body.
	 *
	 * @param thrown the jar that was thrown
	 * @param place finds where the creature's body is drawn each time it is asked, or null for nothing to draw
	 */
	void start(JarType thrown, Supplier<StageAnchors.Spot> place)
	{
		jar = place == null ? null : thrown;
		target = place;
		age = 0;
	}

	/** Moves the flight on by one client tick, and ends it once the jar has landed. */
	void clientTick()
	{
		if (jar == null)
			return;

		age++;
		if (age >= CatchScene.LAND_TICK)
			clear();
	}

	/** Ends any flight at once, for a logout, a world hop or the plugin stopping. */
	void clear()
	{
		jar = null;
		target = null;
		age = 0;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		JarType flying = jar;
		if (flying == null || !JarFlight.flying(age))
			return null;

		try
		{
			draw(graphics, flying);
		}
		catch (RuntimeException e)
		{
			log.debug("Gielinor Go: could not draw the jar in flight", e);
			clear();
		}

		return null;
	}

	/**
	 * Draws the jar at its place for the current moment of the flight.
	 *
	 * @param graphics the canvas
	 * @param flying the jar being thrown
	 */
	private void draw(Graphics2D graphics, JarType flying)
	{
		BufferedImage image = JarIcons.image(flying);
		Player player = client.getLocalPlayer();
		Supplier<StageAnchors.Spot> place = target;
		StageAnchors.Spot body = place == null ? null : place.get();
		if (image == null || player == null || body == null || player.getWorldView() == null)
			return;

		LocalPoint from = player.getLocalLocation();
		LocalPoint to = body.point();
		if (from == null || to == null)
			return;

		double progress = JarFlight.progress(age);
		LocalPoint at = new LocalPoint(
			(int) Math.round(JarFlight.between(from.getX(), to.getX(), progress)),
			(int) Math.round(JarFlight.between(from.getY(), to.getY(), progress)),
			player.getWorldView());
		int height = (int) Math.round(JarFlight.height(HAND_HEIGHT, body.height() / 2.0, progress));
		Point screen = Perspective.localToCanvas(client, at, player.getWorldView().getPlane(), height);
		if (screen == null)
			return;

		graphics.drawImage(image, screen.getX() - image.getWidth() / 2, screen.getY() - image.getHeight() / 2, null);
	}
}
