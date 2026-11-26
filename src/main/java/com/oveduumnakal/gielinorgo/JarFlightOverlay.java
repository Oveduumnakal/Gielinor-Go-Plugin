/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
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
 * Draws the thrown jar flying in an arc from the player to the creature's body, on the player's own screen only. The
 * jar is a 3D model ({@link JarShapes}) that spins and tumbles end over end, seen by the game's camera; if it cannot be
 * drawn, its flat picture flies instead. The flight starts where the player stood at the release, follows the creature,
 * and ends by itself when the jar lands. Everything is measured in the top-level world view, so a throw from a boat
 * aims true. Every client call is guarded, so a failure here can never break the drawing of the rest of the screen.
 */
final class JarFlightOverlay extends Overlay
{
	private static final Logger log = LoggerFactory.getLogger(JarFlightOverlay.class);

	/** The height of the player's hand above the ground, in local units. */
	private static final int HAND_HEIGHT = 100;

	/** Client ticks the jar takes to turn once. */
	private static final double TURN_TICKS = 20;

	/** The alpha of the ring round a jar in flight, a little fainter than on a still picture. */
	private static final int EDGE_ALPHA = 0xB4;

	/** The widest or tallest picture drawn for the model, in pixels; a camera right on the jar shows the flat one. */
	private static final int LARGEST = 400;

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
	private final Map<JarType, JarModel> models = new EnumMap<>(JarType.class);
	private boolean flat;
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
		flat = false;
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
		Supplier<StageAnchors.Spot> place = target;
		StageAnchors.Spot body = place == null ? null : place.get();
		End from = release;
		if (body == null || body.point() == null || from == null)
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
		if (!flat && drawModel(graphics, flying, at, height, from.point, to))
			return;

		BufferedImage image = JarIcons.source(flying, JarIcons.pixels(graphics, JarIcons.ICON_SIZE));
		int plane = world.getPlane();
		Point screen = Perspective.localToCanvas(client, at, plane, (int) Math.round(height - ground(at)));
		if (image == null || screen == null)
			return;

		double half = JarIcons.ICON_SIZE / 2.0;
		CrispImages.draw(graphics, image, screen.getX() - half, screen.getY() - half, JarIcons.ICON_SIZE);
	}

	/**
	 * Draws the 3D jar. A failure is remembered, so the rest of this flight shows the flat picture.
	 *
	 * @param graphics the canvas
	 * @param flying the jar being thrown
	 * @param at where the jar is over the ground
	 * @param height its height, larger being higher
	 * @param from where it was thrown from
	 * @param to where it is going
	 * @return whether nothing else is to be drawn
	 */
	private boolean drawModel(Graphics2D graphics, JarType flying, LocalPoint at, double height, LocalPoint from,
		LocalPoint to)
	{
		try
		{
			JarModel model = models.computeIfAbsent(flying, type -> JarShapes.build(type, true));
			JarLiveView.Camera camera = camera();
			double turned = (age - JarFlight.RELEASE_TICK) / TURN_TICKS * 2 * Math.PI;
			JarLiveView view = new JarLiveView(camera, new double[] {at.getX(), at.getY(), -height}, turned,
				to.getX() - from.getX(), to.getY() - from.getY());
			if (!view.inFront())
				return true;

			Rectangle area = JarRenderer.bounds(model, view);
			if (area.width > LARGEST || area.height > LARGEST)
				return false;

			int samples = Math.max(area.width, area.height) <= 64 ? 4 : 2;
			double flicker = 0.5 + 0.5 * Math.sin(turned * 2);
			graphics.drawImage(JarRenderer.render(model, view, area, samples, flicker, EDGE_ALPHA), area.x, area.y,
				null);
			return true;
		}
		catch (RuntimeException e)
		{
			log.debug("Gielinor Go: could not draw the 3D jar", e);
			flat = true;
			return false;
		}
	}

	/**
	 * Reads the game's camera the way the game itself projects with it: the GPU plugin draws with a finer camera than
	 * the plain renderer does.
	 *
	 * @return the camera as it is now
	 */
	private JarLiveView.Camera camera()
	{
		if (client.isGpu())
		{
			double[] position = {client.getCameraFpX(), client.getCameraFpY(), client.getCameraFpZ()};
			return new JarLiveView.Camera(position, client.getCameraFpYaw(), client.getCameraFpPitch(),
				client.getScale(), client.getViewportXOffset() + client.getViewportWidth() / 2.0,
				client.getViewportYOffset() + client.getViewportHeight() / 2.0);
		}

		double[] position = {client.getCameraX(), client.getCameraY(), client.getCameraZ()};
		return new JarLiveView.Camera(position, JarLiveView.radians(client.getCameraYaw()),
			JarLiveView.radians(client.getCameraPitch()), client.getScale(),
			client.getViewportXOffset() + client.getViewportWidth() / 2,
			client.getViewportYOffset() + client.getViewportHeight() / 2);
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
