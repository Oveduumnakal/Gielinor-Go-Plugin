/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Where the follower stands and how it moves. It is told where the player is drawn, a little at a time, and walks
 * the way the player came, so it trails one tile behind the drawn character and takes every corner it took.
 * Positions are world tiles; a whole number is the middle of a tile. It has no link to the game client, so it can
 * be tested tick by tick.
 */
final class FollowerPath
{
	/** Client ticks, 20 ms each, in one game tick. */
	static final int CLIENT_TICKS = 30;

	/** Most tiles the follower may be too far behind before it gives up and reappears next to the player. */
	static final int MAX_TRAIL = 8;

	/** Tiles the follower keeps between itself and the player, along the way the player came. */
	static final double FOLLOW_GAP = 1.0;

	/** Fastest the follower moves, in tiles per game tick. A running player moves 2. */
	static final int MAX_SPEED = 4;

	/** Units in a full turn on the game's angle scale: 0 faces south, 512 west, 1024 north, 1536 east. */
	static final int FULL_TURN = 2048;

	/** Most the follower turns in one client tick, on the game's angle scale. */
	static final int TURN_PER_TICK = 64;

	/** Fastest the follower moves on an errand, in tiles per game tick: faster than a run, so a battle starts soon. */
	static final int ERRAND_SPEED = 3;

	/** Farthest, in tiles, an errand may take the follower from where it stands now. */
	static final int MAX_ERRAND = 12;

	/** Closer than this to a tile, in tiles, counts as standing on it. */
	private static final double ARRIVED = 0.001;

	/** Tiles tried, in order, when the follower is put down next to the player: south first, corners last. */
	private static final int[][] NEIGHBOURS = {
		{0, -1}, {-1, 0}, {1, 0}, {0, 1}, {-1, -1}, {1, -1}, {-1, 1}, {1, 1}
	};

	/** Tells the path which tiles the follower can be put down on. */
	interface Ground
	{
		/**
		 * Checks one tile on the player's plane.
		 *
		 * @param x world x
		 * @param y world y
		 * @return true when nothing blocks the tile
		 */
		boolean free(int x, int y);

		/**
		 * Checks whether the follower can walk from the player's tile onto a neighbouring one in one step. The
		 * default only asks {@link #free(int, int)}; a ground that knows about walls overrides it.
		 *
		 * @param fromX world x of the player's tile
		 * @param fromY world y of the player's tile
		 * @param toX world x of the neighbouring tile
		 * @param toY world y of the neighbouring tile
		 * @return true when the step is open
		 */
		default boolean reachable(int fromX, int fromY, int toX, int toY)
		{
			return free(toX, toY);
		}
	}

	/** Finds a way between two tiles that does not cross a wall, for the walk back from an errand. */
	interface Router
	{
		/**
		 * Finds a route.
		 *
		 * @param fromX world x of the start tile
		 * @param fromY world y of the start tile
		 * @param toX world x of the end tile
		 * @param toY world y of the end tile
		 * @return the tiles to step onto, ending on the end tile, or null when there is no route
		 */
		List<int[]> route(int fromX, int fromY, int toX, int toY);
	}

	private final Deque<double[]> trail = new ArrayDeque<>();
	private final Deque<double[]> waypoints = new ArrayDeque<>();
	private boolean placed;
	private boolean walking;
	private int playerX;
	private int playerY;
	private double drawnX;
	private double drawnY;
	private int plane;
	private double x;
	private double y;
	private int facing;
	private int wantFacing;
	private boolean errand;
	private double errandX;
	private double errandY;
	private double lookX;
	private double lookY;

	/**
	 * Records where the player is drawn, and keeps the points the player passed through. It is meant to be called
	 * every client tick with the drawn position, which moves a little at a time; a first reading, a jump of more
	 * than {@link StepCounter#MAX_STEP} tiles (a teleport) or a change of plane puts the follower down next to the
	 * player.
	 *
	 * @param x the player's world x, in tiles; a whole number is the middle of a tile
	 * @param y the player's world y, in tiles; a whole number is the middle of a tile
	 * @param level the player's plane
	 * @param ground which tiles are free, for putting the follower down
	 */
	void playerAt(double x, double y, int level, Ground ground)
	{
		double jump = Math.max(Math.abs(x - drawnX), Math.abs(y - drawnY));
		if (!placed || level != plane || jump > StepCounter.MAX_STEP)
		{
			place(x, y, level, ground);
			return;
		}

		playerX = (int) Math.round(x);
		playerY = (int) Math.round(y);
		if (errand)
		{
			drawnX = x;
			drawnY = y;
			return;
		}

		if (jump > ARRIVED)
		{
			trail.add(new double[]{drawnX, drawnY});
			drawnX = x;
			drawnY = y;
		}

		double behind = behind();
		if (behind > MAX_TRAIL)
		{
			place(x, y, level, ground);
			return;
		}

		walking = behind > ARRIVED;
	}

	/**
	 * Moves the follower along the points the player passed through for one client tick, just far enough to be
	 * {@link #FOLLOW_GAP} tile behind the player along that way, and turns it toward the way it is going.
	 */
	void advance()
	{
		if (!placed)
			return;

		if (errand)
		{
			advanceErrand();
			return;
		}

		double budget = Math.min(behind(), (double) MAX_SPEED / CLIENT_TICKS);
		boolean moved = budget > ARRIVED;
		while (budget > 0)
		{
			double targetX = trail.isEmpty() ? drawnX : trail.peek()[0];
			double targetY = trail.isEmpty() ? drawnY : trail.peek()[1];
			double dx = targetX - x;
			double dy = targetY - y;
			double distance = Math.max(Math.abs(dx), Math.abs(dy));
			if (distance > ARRIVED)
				wantFacing = angle(dx, dy);

			if (distance <= budget)
			{
				x = targetX;
				y = targetY;
				budget -= distance;
				if (trail.isEmpty())
					break;

				trail.poll();
			}
			else
			{
				x += dx / distance * budget;
				y += dy / distance * budget;
				budget = 0;
			}
		}

		walking = moved;
		facing = turnToward(facing, wantFacing);
	}

	/** Forgets the follower's position, so the next reading puts it down next to the player again. */
	void reset()
	{
		placed = false;
		walking = false;
		errand = false;
		trail.clear();
		waypoints.clear();
	}

	/**
	 * Sends the follower to a spot on its own errand, such as a battle, leaving the player's trail alone. On
	 * arrival it faces a point. The errand ends with {@link #endErrand(Ground)}, or when the follower is put down
	 * next to the player again.
	 *
	 * @param toX world x of the spot, in tiles
	 * @param toY world y of the spot, in tiles
	 * @param faceX world x of what to face once there, in tiles
	 * @param faceY world y of what to face once there, in tiles
	 * @return false when there is no follower yet or the spot is more than {@link #MAX_ERRAND} tiles away
	 */
	boolean goTo(double toX, double toY, double faceX, double faceY)
	{
		if (!placed || Math.max(Math.abs(toX - x), Math.abs(toY - y)) > MAX_ERRAND)
			return false;

		errand = true;
		errandX = toX;
		errandY = toY;
		lookX = faceX;
		lookY = faceY;
		trail.clear();
		waypoints.clear();
		walking = true;
		return true;
	}

	/**
	 * Sends the follower to a spot along a route of tiles it can walk, so it goes round walls and objects instead of
	 * through them. Otherwise it is {@link #goTo}.
	 *
	 * @param route the tiles to step onto, in order; empty to stay on the tile the follower is on
	 * @param faceX world x of what to face once there, in tiles
	 * @param faceY world y of what to face once there, in tiles
	 * @return false when there is no follower yet or the route has more than {@link #MAX_ERRAND} steps
	 */
	boolean goVia(List<int[]> route, double faceX, double faceY)
	{
		if (!placed || route.size() > MAX_ERRAND)
			return false;

		errand = true;
		trail.clear();
		waypoints.clear();
		for (int[] tile : route)
			waypoints.add(new double[]{tile[0], tile[1]});

		int[] last = route.isEmpty() ? new int[]{(int) Math.round(x), (int) Math.round(y)}
			: route.get(route.size() - 1);
		errandX = last[0];
		errandY = last[1];
		lookX = faceX;
		lookY = faceY;
		walking = true;
		return true;
	}

	/**
	 * Puts the follower on its spot at once, for an errand that took too long.
	 */
	void arriveNow()
	{
		if (!errand)
			return;

		waypoints.clear();
		x = errandX;
		y = errandY;
		walking = false;
		wantFacing = angle(lookX - x, lookY - y);
		facing = wantFacing;
	}

	/**
	 * Ends an errand: the follower walks back to the player, to the free tile next to them that is nearest to it.
	 *
	 * @param ground which tiles are free
	 */
	void endErrand(Ground ground)
	{
		endErrand(ground, null);
	}

	/**
	 * Ends an errand: the follower walks back to the player, to the free tile next to them that is nearest to it,
	 * by a route that does not cross a wall. With no route it is put down next to the player at once.
	 *
	 * @param ground which tiles are free
	 * @param router finds the route back, or null to walk straight
	 */
	void endErrand(Ground ground, Router router)
	{
		if (!errand)
			return;

		errand = false;
		waypoints.clear();
		trail.clear();
		int[] best = {playerX, playerY};
		double bestDistance = Double.MAX_VALUE;
		for (int[] step : NEIGHBOURS)
		{
			int tileX = playerX + step[0];
			int tileY = playerY + step[1];
			double distance = Math.max(Math.abs(tileX - x), Math.abs(tileY - y));
			if (distance < bestDistance && ground.reachable(playerX, playerY, tileX, tileY))
			{
				best = new int[]{tileX, tileY};
				bestDistance = distance;
			}
		}

		List<int[]> way = router == null ? null : router.route((int) Math.round(x), (int) Math.round(y), best[0],
			best[1]);
		if (router != null && way == null)
		{
			place(drawnX, drawnY, plane, ground);
			return;
		}

		if (way == null)
		{
			trail.add(new double[]{best[0], best[1]});
		}
		else
		{
			for (int[] tile : way)
				trail.add(new double[]{tile[0], tile[1]});
		}

		walking = true;
	}

	/** @return whether the follower is on an errand */
	boolean onErrand()
	{
		return errand;
	}

	/** @return whether the follower is on an errand and has reached its spot */
	boolean arrived()
	{
		return errand && waypoints.isEmpty() && Math.max(Math.abs(errandX - x), Math.abs(errandY - y)) <= ARRIVED;
	}

	/** @return whether the follower has a position yet */
	boolean placed()
	{
		return placed;
	}

	/** @return the follower's world x, in tiles; a whole number is the middle of a tile */
	double x()
	{
		return x;
	}

	/** @return the follower's world y, in tiles; a whole number is the middle of a tile */
	double y()
	{
		return y;
	}

	/** @return the plane the follower is on */
	int plane()
	{
		return plane;
	}

	/** @return the way the follower faces, on the game's angle scale */
	int orientation()
	{
		return facing;
	}

	/** @return whether the follower had tiles to walk at the last game tick, so it should look like it is walking */
	boolean walking()
	{
		return walking;
	}

	/** Moves the follower toward its errand spot for one client tick, then turns it. */
	private void advanceErrand()
	{
		double goalX = waypoints.isEmpty() ? errandX : waypoints.peek()[0];
		double goalY = waypoints.isEmpty() ? errandY : waypoints.peek()[1];
		double dx = goalX - x;
		double dy = goalY - y;
		double distance = Math.max(Math.abs(dx), Math.abs(dy));
		double budget = (double) ERRAND_SPEED / CLIENT_TICKS;
		if (distance > ARRIVED)
		{
			if (distance <= budget)
			{
				x = goalX;
				y = goalY;
				waypoints.poll();
			}
			else
			{
				x += dx / distance * budget;
				y += dy / distance * budget;
			}

			wantFacing = angle(dx, dy);
		}
		else
		{
			waypoints.poll();
		}

		walking = !arrived();
		if (!walking)
			wantFacing = angle(lookX - x, lookY - y);

		facing = turnToward(facing, wantFacing);
	}

	/**
	 * Works out the facing for a direction of travel.
	 *
	 * @param dx movement east, any unit
	 * @param dy movement north, same unit
	 * @return the angle on the game's scale, 0 to {@link #FULL_TURN} - 1
	 */
	static int angle(double dx, double dy)
	{
		long turned = Math.round(Math.atan2(-dx, -dy) * FULL_TURN / (2 * Math.PI));
		return (int) turned & (FULL_TURN - 1);
	}

	/**
	 * Turns one client tick's worth toward a facing, the short way round.
	 *
	 * @param from the current facing
	 * @param to the facing wanted
	 * @return the new facing
	 */
	static int turnToward(int from, int to)
	{
		int half = FULL_TURN / 2;
		int difference = ((to - from + half) & (FULL_TURN - 1)) - half;
		int step = Math.max(-TURN_PER_TICK, Math.min(TURN_PER_TICK, difference));
		return (from + step) & (FULL_TURN - 1);
	}

	/**
	 * Puts the follower down on a free tile next to the player, facing them, or on the player's own tile when every
	 * tile around them is blocked.
	 *
	 * @param drawn the player's drawn world x, in tiles
	 * @param drawnAt the player's drawn world y, in tiles
	 * @param level the player's plane
	 * @param ground which tiles are free
	 */
	private void place(double drawn, double drawnAt, int level, Ground ground)
	{
		int px = (int) Math.round(drawn);
		int py = (int) Math.round(drawnAt);
		trail.clear();
		placed = true;
		walking = false;
		errand = false;
		playerX = px;
		playerY = py;
		drawnX = drawn;
		drawnY = drawnAt;
		plane = level;
		x = px;
		y = py;
		for (int[] step : NEIGHBOURS)
		{
			if (!ground.reachable(px, py, px + step[0], py + step[1]))
				continue;

			x = px + step[0];
			y = py + step[1];
			facing = angle(-step[0], -step[1]);
			wantFacing = facing;
			return;
		}
	}

	/**
	 * Works out how far the follower is from being {@link #FOLLOW_GAP} tile behind the player, along the points
	 * the player passed through, counting a diagonal step as one tile like the game does.
	 *
	 * @return the tiles the follower may still walk, or 0 when it is that close or closer
	 */
	private double behind()
	{
		double length = 0;
		double fromX = x;
		double fromY = y;
		for (double[] point : trail)
		{
			length += Math.max(Math.abs(point[0] - fromX), Math.abs(point[1] - fromY));
			fromX = point[0];
			fromY = point[1];
		}

		length += Math.max(Math.abs(drawnX - fromX), Math.abs(drawnY - fromY));
		return Math.max(0, length - FOLLOW_GAP);
	}
}
