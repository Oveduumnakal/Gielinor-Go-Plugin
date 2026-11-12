/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;

/**
 * Owns the bodies of every wild creature: puts one down beside the real monster it came from, wanders it, ends it
 * when its time is up, keeps it through map loads, and finds the one under the mouse. The real monster is never
 * touched. Every method must be called on the game thread, except {@link #spotOf(int)}, which the render thread may
 * call.
 */
final class WildBodies
{
	private static final Logger log = LoggerFactory.getLogger(WildBodies.class);

	/** Most steps the walk from the player to a new wild creature's tile may have: it must be on the player's side. */
	static final int SPAWN_ROUTE_STEPS = 30;

	/** Nearest a new body stands to the real monster's tile, as the game counts distance. */
	static final int SPAWN_MIN_DISTANCE = 1;

	/** Farthest a new body stands from the real monster's tile, as the game counts distance: two tiles. */
	static final int SPAWN_MAX_DISTANCE = 2;

	/** How long the place of a body that is gone is kept for floating text, in milliseconds. */
	static final long FAREWELL_MS = 10_000L;

	private final Client client;
	private final AnimationTable animations;
	private final Random random;
	private final Map<Integer, WildBody> bodies = new LinkedHashMap<>();
	private final Map<Integer, Farewell> farewells = new HashMap<>();
	private final SceneReload reload = new SceneReload();

	/** The last place of a body that is gone, kept for a few seconds so its last lines still float over it. */
	private static final class Farewell
	{
		private final StageAnchors.Spot spot;
		private final long untilMs;

		private Farewell(StageAnchors.Spot spot, long untilMs)
		{
			this.spot = spot;
			this.untilMs = untilMs;
		}
	}

	/**
	 * Creates the owner with no bodies.
	 *
	 * @param client the game client
	 * @param animations the animation ids of each NPC
	 * @param random picks where a body stands and wanders
	 */
	WildBodies(Client client, AnimationTable animations, Random random)
	{
		this.client = client;
		this.animations = animations;
		this.random = random;
	}

	/**
	 * Puts a wild creature down on the player's screen. It stands on a free tile within
	 * {@link #SPAWN_MAX_DISTANCE} tiles of the real monster that the player's side can walk to; with no such tile,
	 * or when the monster's model cannot be drawn, nothing is put down.
	 *
	 * @param entry the wild creature
	 * @param monster the real monster it came from
	 * @param nowMs the clock now, in milliseconds
	 * @return the body, or null when none could be put down
	 */
	WildBody spawn(WildEntry entry, NPC monster, long nowMs)
	{
		try
		{
			Player player = client.getLocalPlayer();
			WorldView view = client.getTopLevelWorldView();
			WorldPoint there = monster.getWorldLocation();
			WorldPoint here = player == null ? null : player.getWorldLocation();
			if (view == null || there == null || here == null || here.getPlane() != there.getPlane()
				|| client.getGameState() != GameState.LOGGED_IN)
				return null;

			List<int[]> route = new CollisionGround(view, there.getPlane()).path().pick(here.getX(), here.getY(),
				SPAWN_ROUTE_STEPS, there.getX(), there.getY(), SPAWN_MIN_DISTANCE, SPAWN_MAX_DISTANCE, random,
				(x, y) -> (x == here.getX() && y == here.getY()) || occupied(x, y, there.getPlane()));
			if (route == null || route.isEmpty())
				return null;

			int[] tile = route.get(route.size() - 1);
			int idle = monster.getIdlePoseAnimation();
			int walk = monster.getWalkAnimation();
			int facing = monster.getCurrentOrientation();
			WildBody body = new WildBody(client, animations, entry, new WildLife(nowMs, random), tile[0], tile[1],
				there.getPlane(), idle, walk, facing);
			if (!body.build())
				return null;

			bodies.put(entry.wildId(), body);
			return body;
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not put a wild creature down", e);
			return null;
		}
	}

	private boolean occupied(int x, int y, int plane)
	{
		for (WildBody other : bodies.values())
		{
			boolean sameTile = Math.round(other.x()) == x && Math.round(other.y()) == y;
			if (sameTile && other.plane() == plane)
				return true;
		}

		return false;
	}

	/**
	 * Looks up a body.
	 *
	 * @param wildId the wild creature's id
	 * @return its body, or null when there is none
	 */
	WildBody get(int wildId)
	{
		return bodies.get(wildId);
	}

	/** @return every body, oldest first */
	Collection<WildBody> all()
	{
		return new ArrayList<>(bodies.values());
	}

	/** @return whether there is no body */
	boolean isEmpty()
	{
		return bodies.isEmpty();
	}

	/**
	 * Moves every body's life on and starts the wanders that are due. Called every game tick.
	 *
	 * @param nowMs the clock now, in milliseconds
	 * @return the ids of the creatures whose bodies are over: faded out, caught or fled
	 */
	List<Integer> gameTick(long nowMs)
	{
		purge(nowMs);
		List<Integer> over = new ArrayList<>();
		Player player = client.getLocalPlayer();
		WorldPoint here = player == null ? null : player.getWorldLocation();
		Iterator<Map.Entry<Integer, WildBody>> it = bodies.entrySet().iterator();
		while (it.hasNext())
		{
			Map.Entry<Integer, WildBody> next = it.next();
			WildBody body = next.getValue();
			try
			{
				tick(body, here, nowMs);
			}
			catch (RuntimeException | AssertionError e)
			{
				log.debug("Gielinor Go: a wild body failed, removing it", e);
				body.life().remove();
			}

			if (body.life().gone())
			{
				farewell(next.getKey(), body, nowMs);
				body.remove();
				over.add(next.getKey());
				it.remove();
			}
		}

		return over;
	}

	private void tick(WildBody body, WorldPoint here, long nowMs)
	{
		boolean samePlane = here != null && here.getPlane() == body.plane();
		int distance = here == null ? 0 : Math.max(Math.abs(here.getX() - (int) Math.round(body.x())),
			Math.abs(here.getY() - (int) Math.round(body.y())));
		WildLife life = body.life();
		life.update(nowMs, distance, samePlane);
		if (life.state() == WildLife.State.ROAMING && !body.walking())
			life.endWander(nowMs);

		if (life.wanderDue(nowMs))
			wander(body, nowMs);
	}

	private void wander(WildBody body, long nowMs)
	{
		WorldView view = client.getTopLevelWorldView();
		if (view == null)
			return;

		int fromX = (int) Math.round(body.x());
		int fromY = (int) Math.round(body.y());
		List<int[]> route = new CollisionGround(view, body.plane()).path().pick(fromX, fromY,
			WildLife.WANDER_STEPS, body.homeX(), body.homeY(), 0, WildLife.WANDER_RADIUS, random,
			(x, y) -> occupied(x, y, body.plane()) || playerOn(x, y));
		if (route == null || route.isEmpty())
		{
			body.life().endWander(nowMs);
			return;
		}

		body.life().startWander();
		body.walkAlong(route);
	}

	private boolean playerOn(int x, int y)
	{
		Player player = client.getLocalPlayer();
		WorldPoint here = player == null ? null : player.getWorldLocation();
		return here != null && here.getX() == x && here.getY() == y;
	}

	/**
	 * Moves every body on by one client tick, and puts them back in the scene after a map load: a load drops every
	 * object the plugin added, so each body is taken out on one tick and built again on the next.
	 *
	 * @param nowMs the clock now, in milliseconds
	 */
	void clientTick(long nowMs)
	{
		if (client.getGameState() == GameState.LOADING)
			reload.loading();

		WorldView view = client.getTopLevelWorldView();
		if (bodies.isEmpty() || view == null || client.getGameState() != GameState.LOGGED_IN)
			return;

		SceneReload.Step step = reload.next(true);
		for (WildBody body : all())
		{
			try
			{
				if (step == SceneReload.Step.DROP)
					body.removeFromScene();
				else if (step == SceneReload.Step.ADD && !body.build())
					body.life().remove();
				else if (step == SceneReload.Step.NONE)
					body.clientTick(view, nowMs);
			}
			catch (RuntimeException | AssertionError e)
			{
				log.debug("Gielinor Go: could not move a wild body", e);
				body.life().remove();
			}
		}
	}

	/**
	 * Notes that a map area is loading, so the bodies are taken out and put back once the game is playable again.
	 */
	void sceneLoading()
	{
		reload.loading();
	}

	/**
	 * Finds the body under the mouse. Where several overlap the one nearest the player wins.
	 *
	 * @param mouse the mouse on the canvas
	 * @return the body, or null when the mouse is over none
	 */
	WildBody under(Point mouse)
	{
		Player player = client.getLocalPlayer();
		WorldPoint here = player == null ? null : player.getWorldLocation();
		WildBody best = null;
		double bestDistance = Double.MAX_VALUE;
		for (WildBody body : bodies.values())
		{
			if (body.life().leaving() || !body.contains(mouse.getX(), mouse.getY()))
				continue;

			double distance = here == null ? 0 : Math.hypot(body.x() - here.getX(), body.y() - here.getY());
			if (distance < bestDistance)
			{
				best = body;
				bestDistance = distance;
			}
		}

		return best;
	}

	/**
	 * Finds where a wild creature is drawn, for floating text. A body that is gone keeps its last place for
	 * {@link #FAREWELL_MS}, so its last lines still float over it. May be called from the render thread.
	 *
	 * @param wildId the wild creature's id
	 * @return its place, or null when it has none
	 */
	StageAnchors.Spot spotOf(int wildId)
	{
		WildBody body = bodies.get(wildId);
		if (body != null && body.spot() != null)
			return body.spot();

		Farewell gone = farewells.get(wildId);
		return gone == null ? null : gone.spot;
	}

	/**
	 * Ends a creature's body at once, for a creature that was caught or fled with no scene to play, or that the
	 * player no longer sees.
	 *
	 * @param wildId the wild creature's id
	 * @param nowMs the clock now, in milliseconds
	 */
	void remove(int wildId, long nowMs)
	{
		WildBody body = bodies.remove(wildId);
		if (body == null)
			return;

		farewell(wildId, body, nowMs);
		body.life().remove();
		body.remove();
	}

	private void farewell(int wildId, WildBody body, long nowMs)
	{
		StageAnchors.Spot spot = body.spot();
		if (spot != null)
			farewells.put(wildId, new Farewell(spot, nowMs + FAREWELL_MS));
	}

	private void purge(long nowMs)
	{
		farewells.values().removeIf(gone -> gone.untilMs <= nowMs);
	}

	/** Takes every body out at once, for a logout, a world hop, the wilderness or the plugin stopping. */
	void clear()
	{
		for (WildBody body : bodies.values())
			body.remove();

		bodies.clear();
		farewells.clear();
		reload.reset();
	}
}
