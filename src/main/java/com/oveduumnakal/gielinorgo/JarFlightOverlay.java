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
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws the thrown jar's picture flying in an arc from the player to the creature's body, on the player's own screen
 * only. The flight starts where the player stood at the release, follows the creature, and ends by itself when the jar
 * lands. Everything is measured in the top-level world view, so a throw from a boat aims true. Every client call is
 * guarded, so a failure here can never break the drawing of the rest of the screen.
 */
final class JarFlightOverlay extends Overlay
{
	private static final Logger log = LoggerFactory.getLogger(JarFlightOverlay.class);

	/** The height of the player's hand above the ground, in local units. */
	private static final int HAND_HEIGHT = 100;

	/** One end of a flight: a place on the top-level ground and an absolute height, larger being higher. */
	private static final class End
	{
		private final LocalPoint point;
		private final double height;

		private End(LocalPoint point, double height)
		{
			this.point = point;
			this.height = height;
		}
	}

	private final Client client;
	private JarType jar;
	private Supplier<StageAnchors.Spot> target;
	private End release;
	private int age;
	private int flightTicks = JarFlight.MIN_FLIGHT;
	private int arc = JarFlight.MIN_ARC;

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
	 * @return client ticks this throw lands later than a short one, for everything timed from the landing
	 */
	int start(JarType thrown, Supplier<StageAnchors.Spot> place)
	{
		jar = place == null ? null : thrown;
		target = place;
		release = null;
		age = 0;
		double tiles = jar == null ? 0 : tilesTo(place);
		flightTicks = JarFlight.flightTicks(tiles);
		arc = JarFlight.arc(tiles);
		return JarFlight.delay(tiles);
	}

	/** Moves the flight on by one client tick, and ends it once the jar has landed. */
	void clientTick()
	{
		if (jar == null)
			return;

		age++;
		if (age >= JarFlight.RELEASE_TICK + flightTicks)
		{
			clear();
			return;
		}

		if (age == JarFlight.RELEASE_TICK)
			release = hand();
	}

	/** Ends any flight at once, for a logout, a world hop or the plugin stopping. */
	void clear()
	{
		jar = null;
		target = null;
		release = null;
		age = 0;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		JarType flying = jar;
		if (flying == null || !JarFlight.flying(age, flightTicks))
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
		BufferedImage image = JarIcons.source(flying);
		Supplier<StageAnchors.Spot> place = target;
		StageAnchors.Spot body = place == null ? null : place.get();
		End from = release;
		if (image == null || body == null || body.point() == null || from == null)
			return;

		WorldView world = client.getTopLevelWorldView();
		LocalPoint to = body.point();
		double progress = JarFlight.progress(age, flightTicks);
		LocalPoint at = new LocalPoint(
			(int) Math.round(JarFlight.between(from.point.getX(), to.getX(), progress)),
			(int) Math.round(JarFlight.between(from.point.getY(), to.getY(), progress)),
			world);
		double end = ground(to) + body.height() / 2.0;
		double height = JarFlight.height(from.height, end, progress, arc);
		int plane = world.getPlane();
		Point screen = Perspective.localToCanvas(client, at, plane, (int) Math.round(height - ground(at)));
		if (screen == null)
			return;

		double half = JarIcons.ICON_SIZE / 2.0;
		CrispImages.draw(graphics, image, screen.getX() - half, screen.getY() - half, JarIcons.ICON_SIZE);
	}

	/**
	 * Measures a throw.
	 *
	 * @param place finds the creature's body
	 * @return the distance from the player to it in tiles, or 0 when either end is unknown
	 */
	private double tilesTo(Supplier<StageAnchors.Spot> place)
	{
		LocalPoint from = player();
		StageAnchors.Spot body = place.get();
		if (from == null || body == null || body.point() == null)
			return 0;

		return Math.hypot(body.point().getX() - from.getX(), body.point().getY() - from.getY())
			/ Perspective.LOCAL_TILE_SIZE;
	}

	/** @return where the jar leaves the hand now, or null when the player cannot be placed */
	private End hand()
	{
		LocalPoint from = player();
		return from == null ? null : new End(from, ground(from) + HAND_HEIGHT);
	}

	/**
	 * Places the player in the top-level world view, which is where creatures are placed. On a boat the player's own
	 * coordinates are the boat's, so they are carried over to the main world.
	 *
	 * @return the player's place, or null when it is unknown
	 */
	private LocalPoint player()
	{
		Player player = client.getLocalPlayer();
		if (player == null || player.getWorldView() == null)
			return null;

		LocalPoint local = player.getLocalLocation();
		WorldView view = player.getWorldView();
		if (local == null || view.isTopLevel())
			return local;

		WorldView top = client.getTopLevelWorldView();
		WorldEntity boat = top.worldEntities().byIndex(view.getId());
		return boat == null ? null : boat.transformToMainWorld(local);
	}

	/**
	 * Gives the absolute height of the ground; the game counts upwards as negative, so it is turned over here.
	 *
	 * @param point a place in the top-level world view
	 * @return the height, larger being higher
	 */
	private double ground(LocalPoint point)
	{
		return -Perspective.getTileHeight(client, point, client.getTopLevelWorldView().getPlane());
	}
}
