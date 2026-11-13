/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Polygon;
import java.awt.Shape;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;

/**
 * One wild creature on the player's screen: its own copy of the monster's model, drawn at {@link #SIZE} of the real
 * size on a tile near the monster it came from. It is not the monster: it stands, wanders a step or two, takes part
 * in battles and catches, and leaves on its own timing ({@link WildLife}), whatever the real monster does. It lives
 * on the player's own screen only; nothing is sent to the game. Every method must be called on the game thread.
 */
final class WildBody
{
	private static final Logger log = LoggerFactory.getLogger(WildBody.class);

	/** Share of the real monster's size the body is drawn at. The owner asked for three quarters on 2026-11-11. */
	static final double SIZE = 0.75;

	/** Scene units in one tile. */
	private static final int TILE = 128;

	/** How far a body walks in one client tick, in tiles: one tile per game tick, as a monster walks. */
	private static final double WALK_PER_CLIENT_TICK = 1.0 / FollowerPath.CLIENT_TICKS;

	/** Closer than this to a tile, in tiles, counts as standing on it. */
	private static final double ARRIVED = 0.001;

	private final Client client;
	private final AnimationTable animations;
	private final WildEntry entry;
	private final WildLife life;
	private final int homeX;
	private final int homeY;
	private final int plane;
	private final int idleAnimation;
	private final int walkAnimation;
	private final Deque<int[]> route = new ArrayDeque<>();
	private CreatureBody body;
	private double x;
	private double y;
	private int facing;
	private int wantFacing;
	private int drawnHeight;
	private int tiles = 1;
	private boolean facingHeld;
	private boolean fadeBroken;
	private boolean unreachableNoted;
	private volatile StageAnchors.Spot spot;
	private volatile StageAnchors.Spot lastSpot;
	private BodyCatch scene;
	private boolean turningAway;
	private double runFromX;
	private double runFromY;
	private double runDirX;
	private double runDirY;

	/**
	 * Creates a body on a tile. It is not in the scene until the first {@link #clientTick}.
	 *
	 * @param client the game client
	 * @param animations the animation ids of each NPC
	 * @param entry the wild creature this is the body of
	 * @param life the life that decides when it appears, wanders and leaves
	 * @param tileX the world x of the tile it stands on
	 * @param tileY the world y of the tile it stands on
	 * @param plane the plane
	 * @param idle the stand animation of the monster it copies, from the live NPC
	 * @param walk the walk animation of the monster it copies, from the live NPC
	 * @param facing the way it faces to begin with, on the game's angle scale
	 */
	WildBody(Client client, AnimationTable animations, WildEntry entry, WildLife life, int tileX, int tileY,
		int plane, int idle, int walk, int facing)
	{
		this.client = client;
		this.animations = animations;
		this.entry = entry;
		this.life = life;
		this.homeX = tileX;
		this.homeY = tileY;
		this.x = tileX;
		this.y = tileY;
		this.plane = plane;
		this.idleAnimation = idle;
		this.walkAnimation = walk;
		this.facing = facing;
		this.wantFacing = facing;
	}

	/**
	 * Builds the model of the body, or builds it again after a map load.
	 *
	 * @return false when the monster's model cannot be drawn, so there is no body
	 */
	boolean build()
	{
		try
		{
			removeFromScene();
			NpcModels.Parts parts = NpcModels.load(client, entry.npcId());
			if (parts == null)
				return false;

			if (entry.gilded())
				FollowerService.gild(parts.data());

			int wide = shrunk(parts.widthScale());
			int tall = shrunk(parts.heightScale());
			drawnHeight = (int) Math.round(FollowerService.heightOf(parts.data()) * tall / NpcModels.NORMAL_SCALE);
			tiles = Math.max(1, (int) Math.round(parts.size() * SIZE));
			CreatureBody made = CreatureBody.create(client, animations, parts, wide, tall);
			if (made == null)
				return false;

			made.useAnimations(idleAnimation, walkAnimation);
			made.setBase(made.standAnimation());
			made.setOpacity(0.0);
			body = made;
			fadeBroken = false;
			return true;
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not build a wild body for NPC {}", entry.npcId(), e);
			body = null;
			return false;
		}
	}

	/**
	 * Works out the scale value for the body.
	 *
	 * @param scale the game's scale value for the real monster, where {@link NpcModels#NORMAL_SCALE} is as modelled
	 * @return the value for the body, {@link #SIZE} of it, never below 1
	 */
	static int shrunk(int scale)
	{
		return Math.max(1, (int) Math.round(scale * SIZE));
	}

	/** @return the wild creature this is the body of */
	WildEntry entry()
	{
		return entry;
	}

	/** @return the life of the body */
	WildLife life()
	{
		return life;
	}

	/** @return the model, for acting out a battle or a catch; null while it has none */
	CreatureBody body()
	{
		return body;
	}

	/** @return the world x of the body, in tiles; a whole number is the middle of a tile */
	double x()
	{
		return x;
	}

	/** @return the world y of the body, in tiles; a whole number is the middle of a tile */
	double y()
	{
		return y;
	}

	/** @return the tile it appeared on, x */
	int homeX()
	{
		return homeX;
	}

	/** @return the tile it appeared on, y */
	int homeY()
	{
		return homeY;
	}

	/** @return the plane it stands on */
	int plane()
	{
		return plane;
	}

	/** @return how many tiles wide the body is drawn, after shrinking */
	int tiles()
	{
		return tiles;
	}

	/** @return how tall the body is drawn, in scene units */
	int drawnHeight()
	{
		return drawnHeight;
	}

	/** @return the way the body faces, on the game's angle scale */
	int facing()
	{
		return facing;
	}

	/**
	 * Gives the height of the ground the body is drawn on, so a projectile can leave from or land on its body.
	 *
	 * @return the scene's z of the body's feet, or NaN while it is not drawn
	 */
	double groundZ()
	{
		StageAnchors.Spot at = spot;
		return at == null ? Double.NaN : Perspective.getTileHeight(client, at.point(), plane);
	}

	/**
	 * Notes that the player was told a battle with this creature is shown as text because it cannot be reached.
	 *
	 * @return true the first time, false after that, so the player is told once
	 */
	boolean noteUnreachable()
	{
		boolean first = !unreachableNoted;
		unreachableNoted = true;
		return first;
	}

	/** @return where the body is drawn now, or null while it is not in the scene */
	StageAnchors.Spot spot()
	{
		return spot;
	}

	/** @return where the body was last drawn, kept while it is out of sight, for text floating over it */
	StageAnchors.Spot lastSpot()
	{
		return lastSpot;
	}

	/** @return whether the body is in the scene right now */
	boolean drawn()
	{
		return body != null && body.isActive();
	}

	/** @return whether the body is walking to a spot */
	boolean walking()
	{
		return !route.isEmpty();
	}

	/**
	 * Sends the body walking along a route, one tile after another.
	 *
	 * @param steps the tiles to step onto, in order
	 */
	void walkAlong(List<int[]> steps)
	{
		route.clear();
		route.addAll(steps);
	}

	/** Stops the body where it is, between tiles if need be, and has it stand. */
	void stand()
	{
		route.clear();
	}

	/**
	 * Has the body face a place and keep facing it, such as the lead creature in a battle. Until
	 * {@link #freeFacing()}, the body turns toward it every client tick.
	 *
	 * @param tileX the place's world x, in tiles
	 * @param tileY the place's world y, in tiles
	 */
	void faceToward(double tileX, double tileY)
	{
		double dx = tileX - x;
		double dy = tileY - y;
		if (Math.abs(dx) > ARRIVED || Math.abs(dy) > ARRIVED)
			wantFacing = FollowerPath.angle(dx, dy);

		facingHeld = true;
	}

	/** Lets the body face the way it walks again. */
	void freeFacing()
	{
		facingHeld = false;
	}

	/**
	 * Moves the body on by one client tick: it walks, turns, takes the opacity of its life, plays its animation and
	 * is put in the scene.
	 *
	 * @param view the top level world view
	 * @param nowMs the clock now, in milliseconds
	 */
	void clientTick(WorldView view, long nowMs)
	{
		if (body == null)
			return;

		if (scene != null)
			stepCatch(nowMs);

		advanceWalk();
		facing = FollowerPath.turnToward(facing, wantFacing);
		body.clientTick();
		body.setBase(walking() || turningAway ? body.walkAnimation() : body.standAnimation());
		fade(nowMs);
		place(view);
	}

	/**
	 * Starts the catch scene on the body: a jar has been thrown at it. It flinches when the jar lands, and then a
	 * caught creature shrinks away, one that breaks free turns to the player and attacks, and one that flees turns
	 * and runs off fading out ({@link BodyCatch}). A body that lies knocked out stays down until its second act.
	 *
	 * @param outcome what the throw led to
	 * @return true when the scene plays; false when the throw touches nothing or the creature is already leaving, so
	 *     the caller ends the creature itself if it was caught or fled
	 */
	boolean beginCatch(CatchOutcome outcome)
	{
		if (body == null || !BodyCatch.plays(outcome) || life.leaving())
			return false;

		boolean lying = life.knockedOut() || body.lyingDown();
		if (!life.beginCatch())
			return false;

		body.endTurn();
		route.clear();
		turningAway = false;
		scene = new BodyCatch(outcome, lying);
		return true;
	}

	/** @return whether a catch scene is playing on the body */
	boolean catching()
	{
		return scene != null;
	}

	private void stepCatch(long nowMs)
	{
		BodyCatch current = scene;
		current.tick();
		if (current.getsUp())
			body.recover();

		TargetScript.Step step = current.step();
		if (step == TargetScript.Step.HIT)
			body.act(CreatureBody.Act.HIT);
		else if (step == TargetScript.Step.ATTACK)
			body.act(CreatureBody.Act.ATTACK);

		if (current.turn() != TargetScript.Turn.NONE)
			startCatchTurn(current.turn());

		body.setSize(current.size());
		if (turningAway)
		{
			double run = current.runTiles();
			x = runFromX + runDirX * run;
			y = runFromY + runDirY * run;
		}

		if (current.removesBody())
		{
			scene = null;
			life.endCatch(false, nowMs);
		}
		else if (current.standsAgain())
		{
			scene = null;
			life.endCatch(true, nowMs);
			turningAway = false;
			facingHeld = false;
			body.setSize(1.0);
			body.recover();
		}
	}

	/**
	 * Works out which way the body turns for its second act: to the player, or away from the player and running that
	 * way.
	 *
	 * @param turn the turn, not none
	 */
	private void startCatchTurn(TargetScript.Turn turn)
	{
		Player player = client.getLocalPlayer();
		WorldView view = client.getTopLevelWorldView();
		LocalPoint from = player == null || view == null ? null : player.getLocalLocation();
		double dx = from == null ? 0 : x - (from.getX() / (double) TILE - 0.5 + view.getBaseX());
		double dy = from == null ? 0 : y - (from.getY() / (double) TILE - 0.5 + view.getBaseY());
		double length = Math.hypot(dx, dy);
		if (length < 0.01)
		{
			double turned = facing * 2 * Math.PI / FollowerPath.FULL_TURN;
			dx = -Math.sin(turned);
			dy = -Math.cos(turned);
			length = 1;
		}

		runDirX = dx / length;
		runDirY = dy / length;
		runFromX = x;
		runFromY = y;
		facingHeld = true;
		if (turn == TargetScript.Turn.TO_PLAYER)
		{
			wantFacing = FollowerPath.angle(-runDirX, -runDirY);
			return;
		}

		wantFacing = FollowerPath.angle(runDirX, runDirY);
		turningAway = true;
	}

	private void advanceWalk()
	{
		if (route.isEmpty())
			return;

		int[] next = route.peek();
		double dx = next[0] - x;
		double dy = next[1] - y;
		double distance = Math.max(Math.abs(dx), Math.abs(dy));
		if (distance <= WALK_PER_CLIENT_TICK)
		{
			x = next[0];
			y = next[1];
			route.poll();
		}
		else
		{
			x += dx / distance * WALK_PER_CLIENT_TICK;
			y += dy / distance * WALK_PER_CLIENT_TICK;
		}

		if (!facingHeld && distance > ARRIVED)
			wantFacing = FollowerPath.angle(dx, dy);
	}

	private void fade(long nowMs)
	{
		try
		{
			double shown = scene == null ? 1.0 : scene.opacity();
			body.setOpacity(fadeBroken ? 1.0 : Math.min(life.opacity(nowMs), shown));
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not fade a wild body", e);
			fadeBroken = true;
		}
	}

	private void place(WorldView view)
	{
		double push = body.push();
		double turned = facing * 2 * Math.PI / FollowerPath.FULL_TURN;
		int sceneX = (int) Math.round((x - view.getBaseX() + 0.5 - Math.sin(turned) * push) * TILE);
		int sceneY = (int) Math.round((y - view.getBaseY() + 0.5 - Math.cos(turned) * push) * TILE);
		boolean inScene = sceneX >= 0 && sceneY >= 0 && sceneX < view.getSizeX() * TILE
			&& sceneY < view.getSizeY() * TILE && view.getPlane() == plane;
		if (!inScene || !body.visible())
		{
			if (body.isActive())
				body.setActive(false);

			spot = null;
			if (inScene && lastSpot != null)
				lastSpot = new StageAnchors.Spot(new LocalPoint(sceneX, sceneY, view), plane, drawnHeight);

			return;
		}

		LocalPoint at = new LocalPoint(sceneX, sceneY, view);
		spot = new StageAnchors.Spot(at, plane, drawnHeight);
		lastSpot = spot;
		body.setLocation(at, plane);
		body.setOrientation(facing);
		if (!body.isActive())
			body.setActive(true);
	}

	/**
	 * Tells whether a point on the canvas is over the body: inside the outline of its model as drawn, or inside the
	 * ground tiles it stands on. Any failure counts as "not over it".
	 *
	 * @param canvasX the point's canvas x
	 * @param canvasY the point's canvas y
	 * @return true when the body is drawn and the point is over it
	 */
	boolean contains(int canvasX, int canvasY)
	{
		try
		{
			StageAnchors.Spot at = spot;
			if (body == null || at == null || !body.isActive())
				return false;

			return ShapeHits.anyContains(hitShapes(at.point()), canvasX, canvasY);
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not test the mouse against a wild body", e);
			return false;
		}
	}

	private List<Shape> hitShapes(LocalPoint at)
	{
		List<Shape> shapes = new ArrayList<>();
		WorldView view = client.getTopLevelWorldView();
		Model model = body.hitModel();
		if (view != null && model != null)
		{
			int z = Perspective.getTileHeight(client, at, plane);
			shapes.add(Perspective.getClickbox(client, view, model, facing, at.getX(), at.getY(), z));
		}

		Polygon ground = Perspective.getCanvasTileAreaPoly(client, at, tiles);
		if (ground != null)
			shapes.add(ground);

		return shapes;
	}

	/** Takes the body out of the scene and lets it go. Never throws. */
	void remove()
	{
		route.clear();
		removeFromScene();
		body = null;
		spot = null;
	}

	/** Takes the body out of the scene, so that it is not drawn while it is built again or after a map load. */
	void removeFromScene()
	{
		try
		{
			if (body != null && body.isActive())
				body.setActive(false);
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not remove a wild body", e);
		}
	}
}
