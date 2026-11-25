/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

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
	static final double FOLLOW_GAP = 2.0;

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

	/** A player this close, in tiles, to where they walked before has doubled back. */
	private static final double LOOP_REACH = 0.25;

	/** The shortest walk, in tiles, that counts as a loop to cut out of the trail. */
	private static final double LOOP_LENGTH = 1.0;

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
	private double steppedX;
	private double steppedY;
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
	private final FollowerStroll stroll;
	private final FollowerPace pace;
	private final double gap;
	private boolean playerMoving;
	private boolean strolling;
	private boolean rejoinDue;

	/** Makes a path whose idle strolls are picked at random. */
	FollowerPath()
	{
		this(new Random());
	}

	/**
	 * Makes a path with a given source of chance, so a test can repeat a stroll.
	 *
	 * @param random picks how long the follower rests and where it strolls to
	 */
	FollowerPath(Random random)
	{
		this(random, FOLLOW_GAP, true);
	}

	/**
	 * Makes a path with its own gap and pace, so a test can check the trailing alone.
	 *
	 * @param random picks how long the follower rests, where it strolls to and how far it lags
	 * @param gap tiles to keep behind the player, along the way the player came
	 * @param lagging false to set off at once with every step, as a tight shadow
	 */
	FollowerPath(Random random, double gap, boolean lagging)
	{
		this.gap = gap;
		stroll = new FollowerStroll(random);
		pace = new FollowerPace(random, lagging);
	}

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

		playerMoving = jump > ARRIVED;
		if (jump > ARRIVED)
		{
			if (stroll.phase() == FollowerStroll.Phase.OFF)
			{
				stroll.forgetRest();
			}
			else
			{
				stroll.stop();
				rejoinDue = true;
			}

			trail.add(new double[]{drawnX, drawnY});
			drawnX = x;
			drawnY = y;
			cutLoop();
		}

		double behind = behind();
		if (behind > MAX_TRAIL)
		{
			place(x, y, level, ground);
			return;
		}

		if (stroll.phase() == FollowerStroll.Phase.OFF)
			walking = pace.moving(behind, playerMoving);
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

		if (stroll.phase() == FollowerStroll.Phase.WALKING)
		{
			advanceStroll();
			return;
		}

		if (stroll.phase() == FollowerStroll.Phase.STANDING)
		{
			walking = false;
			facing = turnToward(facing, wantFacing);
			return;
		}

		double excess = behind();
		double playerStep = Math.max(Math.abs(drawnX - steppedX), Math.abs(drawnY - steppedY));
		steppedX = drawnX;
		steppedY = drawnY;
		double budget = pace.step(excess, playerStep);
		boolean moved = budget > ARRIVED;
		if (moved && !playerMoving && excess - budget <= ARRIVED)
			pace.caughtUp();

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
		rejoinDue = false;
		stroll.stop();
		pace.reset();
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
		rejoinDue = false;
		stroll.stop();
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
		rejoinDue = false;
		stroll.stop();
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
		int[] best = nearestNeighbour(ground);

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

	/**
	 * Turns the idle stroll on or off. Off is how the path starts. Turning it off while the follower is out on a
	 * stroll, or standing where one ended, brings it home: {@link #rejoinDue()} becomes true.
	 *
	 * @param on whether the follower may stroll when the player stands still
	 */
	void setStrolling(boolean on)
	{
		strolling = on;
		if (!on && stroll.phase() != FollowerStroll.Phase.OFF)
		{
			stroll.stop();
			rejoinDue = placed && !errand;
		}
	}

	/**
	 * Counts one client tick of rest and tells whether a stroll should start now. It never allocates, so it may be
	 * asked every client tick. The rest only counts while the follower stands still with nowhere left to walk: caught
	 * up with a stopped player, or standing where its last stroll ended.
	 *
	 * @return true when {@link #startStroll(TilePath)} should be called
	 */
	boolean strollDue()
	{
		if (!strolling || !placed || errand || rejoinDue)
			return false;

		FollowerStroll.Phase phase = stroll.phase();
		boolean settled = phase == FollowerStroll.Phase.STANDING
			|| phase == FollowerStroll.Phase.OFF && !walking && behind() <= ARRIVED;
		return stroll.restOver(settled);
	}

	/**
	 * Picks a tile within {@link FollowerStroll#RADIUS} tiles of the player, never the player's own, and starts
	 * walking to it. With nowhere to go nothing starts and a new rest is rolled.
	 *
	 * @param tiles knows which steps are open
	 * @return true when the follower is walking to a new tile
	 */
	boolean startStroll(TilePath tiles)
	{
		return stroll.begin(tiles, (int) Math.round(x), (int) Math.round(y), playerX, playerY);
	}

	/** @return whether the follower is out on a stroll, or standing where one ended */
	boolean wandering()
	{
		return stroll.phase() != FollowerStroll.Phase.OFF;
	}

	/**
	 * @return whether the follower was left away from the player's trail by a stroll that has ended, so
	 *     {@link #rejoin(Ground, Router)} should be called before the next move
	 */
	boolean rejoinDue()
	{
		return rejoinDue;
	}

	/**
	 * Brings a follower that was away on a stroll back to the player's trail: it walks a route that does not cross a
	 * wall to the tile the player stood on when it set off (or to the player, when it never moved), and from there
	 * trails as usual, one tile behind. With no route it is put down next to the player at once.
	 *
	 * @param ground which tiles are free, for the fallback
	 * @param router finds the route; null walks straight
	 */
	void rejoin(Ground ground, Router router)
	{
		rejoinDue = false;
		if (!placed || errand)
			return;

		double[] first = trail.peek();
		int[] to = first == null ? nearestNeighbour(ground) : new int[]{(int) Math.round(first[0]),
			(int) Math.round(first[1])};
		int[] from = {(int) Math.round(x), (int) Math.round(y)};
		boolean onPlayer = from[0] == playerX && from[1] == playerY;
		if (onPlayer)
			from = nearestNeighbour(ground);

		List<int[]> way = router == null ? Collections.singletonList(to) : router.route(from[0], from[1], to[0], to[1]);
		if (way == null)
		{
			place(drawnX, drawnY, plane, ground);
			return;
		}

		List<int[]> walk = new ArrayList<>();
		if (onPlayer)
			walk.add(from);

		walk.addAll(way);
		if (walk.isEmpty() && first == null)
			walk.add(to);

		for (int i = walk.size() - 1; i >= 0; i--)
			trail.addFirst(new double[]{walk.get(i)[0], walk.get(i)[1]});

		walking = behind() > ARRIVED;
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

	/** Moves the follower along its stroll for one client tick, then turns it. On the last tile it faces the player. */
	private void advanceStroll()
	{
		Deque<double[]> way = stroll.route();
		double budget = (double) FollowerStroll.SPEED / CLIENT_TICKS;
		while (budget > 0 && !way.isEmpty())
		{
			double[] goal = way.peek();
			double dx = goal[0] - x;
			double dy = goal[1] - y;
			double distance = Math.max(Math.abs(dx), Math.abs(dy));
			if (distance > ARRIVED)
				wantFacing = angle(dx, dy);

			if (distance <= budget)
			{
				x = goal[0];
				y = goal[1];
				budget -= distance;
				way.poll();
			}
			else
			{
				x += dx / distance * budget;
				y += dy / distance * budget;
				budget = 0;
			}
		}

		walking = !way.isEmpty();
		if (!walking)
		{
			stroll.arrived();
			wantFacing = angle(playerX - x, playerY - y);
		}

		facing = turnToward(facing, wantFacing);
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
	 * Finds the free tile next to the player that is nearest to the follower, for the follower to walk to.
	 *
	 * @param ground which tiles are free
	 * @return the tile as {x, y}; the player's own tile when every neighbour is blocked
	 */
	private int[] nearestNeighbour(Ground ground)
	{
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

		return best;
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
		rejoinDue = false;
		stroll.stop();
		pace.reset();
		playerX = px;
		playerY = py;
		drawnX = drawn;
		drawnY = drawnAt;
		steppedX = drawn;
		steppedY = drawnAt;
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
	/**
	 * Cuts out of the trail the walk the player undid by coming back to where they were, so the follower does not walk
	 * out to a turning point and back again.
	 */
	private void cutLoop()
	{
		double length = 0;
		double fromX = drawnX;
		double fromY = drawnY;
		int walkedSince = 0;
		int cut = 0;
		for (Iterator<double[]> back = trail.descendingIterator(); back.hasNext();)
		{
			double[] point = back.next();
			length += Math.max(Math.abs(point[0] - fromX), Math.abs(point[1] - fromY));
			fromX = point[0];
			fromY = point[1];
			if (length > LOOP_LENGTH && near(point[0], point[1]))
				cut = walkedSince;

			walkedSince++;
		}

		length += Math.max(Math.abs(x - fromX), Math.abs(y - fromY));
		if (length > LOOP_LENGTH && near(x, y))
			cut = trail.size();

		for (int i = 0; i < cut; i++)
			trail.removeLast();
	}

	private boolean near(double atX, double atY)
	{
		return Math.max(Math.abs(atX - drawnX), Math.abs(atY - drawnY)) <= LOOP_REACH;
	}

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
		return Math.max(0, length - gap);
	}
}
