/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.GameState;
import net.runelite.api.ModelData;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;

/**
 * Shows the lead creature walking behind the player. The follower is a model this plugin adds to the player's own
 * screen: the game server never hears of it, other players cannot see it, and it cannot be clicked. Every method
 * must be called on the game thread.
 */
final class FollowerService
{
	private static final Logger log = LoggerFactory.getLogger(FollowerService.class);

	/** Scene units in one tile. */
	private static final int TILE = 128;

	/**
	 * Share of the size worked out by {@link #fit(int, double, double)} a follower is drawn at. The owner asked
	 * for three quarters on 2026-11-11, after seeing followers at full size and asking first for half.
	 */
	static final double SHOWN_SIZE = 0.75;

	/** Tiles a follower's footprint may cover. A creature that stands on more tiles in the game is shrunk to this. */
	static final double MAX_TILES = 1.15;

	/** Farthest a wing, tail or tentacle may reach from the follower's middle, in scene units: one tile. */
	static final int MAX_REACH = 128;

	/** Tallest a follower may stand, in scene units. A player is about this tall. */
	static final int MAX_HEIGHT = 200;

	/** Hue of a gilded follower, on the game's 0 to 63 scale. */
	private static final int GOLD_HUE = 7;

	/** Saturation of a gilded follower, on the game's 0 to 7 scale. */
	private static final int GOLD_SATURATION = 6;

	/** Darkest shade of a gilded follower, on the game's 0 to 127 scale. */
	private static final int GOLD_DARKEST = 24;

	/** Lightest shade of a gilded follower, on the game's 0 to 127 scale. */
	private static final int GOLD_LIGHTEST = 104;

	private final Client client;
	private final AnimationTable animations;
	private final Function<String, Species> speciesLookup;
	private final FollowerPath path = new FollowerPath();
	private CreatureBody body;
	private String shownKey;
	private boolean shownGilded;
	private int shownNpc;
	private int leadNpc;
	private int asNpc;
	private String leadKey;
	private boolean leadGilded;
	private String asKey;
	private boolean asGilded;
	private volatile boolean showingAs;
	private double fade = 1.0;
	private int height;
	private int idle = AnimationTable.NONE;
	private int walk = AnimationTable.NONE;
	private boolean wanted;
	private final SceneReload reload = new SceneReload();

	/**
	 * Creates the service. Nothing is shown until the first {@link #gameTick(String, boolean)}.
	 *
	 * @param client the game client
	 * @param animations the standing and walking animation of each NPC id
	 * @param speciesLookup finds a species by name or key, or returns null
	 */
	FollowerService(Client client, AnimationTable animations, Function<String, Species> speciesLookup)
	{
		this.client = client;
		this.animations = animations;
		this.speciesLookup = speciesLookup;
	}

	/**
	 * Tells the follower which creature to be. Called once per game tick. Where the follower stands is fed from
	 * {@link #clientTick()}, because the character is drawn gliding between tiles, and the follower trails the drawn
	 * character, not the tile the server last reported. Each client tick is watched, not each game tick, because the
	 * drawn character crosses a tile border half way through a game tick and a game tick reading would be late by a
	 * whole tick.
	 *
	 * @param speciesKey the lead creature's species, or null when no follower should show
	 * @param gilded whether the lead creature is gilded
	 */
	void gameTick(String speciesKey, boolean gilded)
	{
		gameTick(speciesKey, 0, gilded);
	}

	/**
	 * Tells the follower which creature to be, drawn as the NPC it was caught as.
	 *
	 * @param speciesKey the lead creature's species, or null when no follower should show
	 * @param npcId the NPC id the lead was caught as, or 0 for the species' first; one the species does not have
	 *     counts as 0 ({@link Species#npcIdFor(int)})
	 * @param gilded whether the lead creature is gilded
	 */
	void gameTick(String speciesKey, int npcId, boolean gilded)
	{
		Player player = client.getLocalPlayer();
		WorldView view = player == null ? null : player.getWorldView();
		if (speciesKey == null || view == null || !view.isTopLevel())
		{
			hide();
			return;
		}

		leadKey = speciesKey;
		leadGilded = gilded;
		leadNpc = npcId;
		String key = asKey != null ? asKey : speciesKey;
		boolean look = asKey != null ? asGilded : gilded;
		int npc = asKey != null ? asNpc : npcId;
		if (!key.equals(shownKey) || look != shownGilded || npc != shownNpc)
			rebuild(key, npc, look);

		if (body == null)
			return;

		wanted = true;
	}

	/**
	 * Tells the path where the player is drawn right now, as a world tile with a fraction.
	 *
	 * @param view the top level world view
	 */
	private void followDrawnCharacter(WorldView view)
	{
		Player player = client.getLocalPlayer();
		LocalPoint drawn = player == null ? null : player.getLocalLocation();
		if (drawn == null || player.getWorldView() != view)
			return;

		int plane = view.getPlane();
		double tileX = view.getBaseX() + (double) drawn.getX() / TILE - 0.5;
		double tileY = view.getBaseY() + (double) drawn.getY() / TILE - 0.5;
		path.playerAt(tileX, tileY, plane, ground(view, plane));
	}

	/**
	 * Notes that a new map area is loading. The client drops what was added to the old scene, so the follower is
	 * taken out and put back once the game is playable again.
	 */
	void sceneLoading()
	{
		reload.loading();
	}

	/**
	 * Moves the follower a little and keeps it in the scene. Called once per client tick. Nothing moves while the
	 * game is not fully logged in, so a half-loaded scene cannot place the follower.
	 */
	void clientTick()
	{
		boolean playable = client.getGameState() == GameState.LOGGED_IN;
		if (client.getGameState() == GameState.LOADING)
			reload.loading();

		if (body == null || !wanted || !playable)
			return;

		WorldView view = client.getTopLevelWorldView();
		if (view == null)
			return;

		SceneReload.Step step = reload.next(true);
		if (step == SceneReload.Step.DROP)
		{
			if (body.isActive())
				body.setActive(false);

			return;
		}

		if (step == SceneReload.Step.ADD)
		{
			renewBody();
			if (body == null)
				return;
		}

		followDrawnCharacter(view);
		if (!path.placed())
			return;

		path.advance();
		int x = sceneUnits(path.x(), view.getBaseX());
		int y = sceneUnits(path.y(), view.getBaseY());
		boolean inScene = x >= 0 && y >= 0 && x < view.getSizeX() * TILE && y < view.getSizeY() * TILE;
		if (!inScene || view.getPlane() != path.plane())
		{
			if (body.isActive())
				body.setActive(false);

			return;
		}

		body.clientTick();
		applyFade();
		if (!body.visible())
		{
			if (body.isActive())
				body.setActive(false);

			return;
		}

		double push = body.push();
		if (push != 0.0)
		{
			double turned = path.orientation() * 2 * Math.PI / FollowerPath.FULL_TURN;
			x += (int) Math.round(-Math.sin(turned) * push * TILE);
			y += (int) Math.round(-Math.cos(turned) * push * TILE);
		}

		body.setLocation(new LocalPoint(x, y, view), path.plane());
		body.setOrientation(path.orientation());
		body.setBase(path.walking() ? walk : idle);
		if (!body.isActive())
			body.setActive(true);
	}

	/**
	 * Gives the body the opacity a scene asked for. A body that cannot be built at that opacity is drawn solid from
	 * then on.
	 */
	private void applyFade()
	{
		try
		{
			body.setOpacity(fade);
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not fade the follower", e);
			fade = 1.0;
		}
	}

	/**
	 * Swaps the old body for a fresh one of the same creature, for after a map load: a new scene object with newly
	 * lit models. The old body leaves the scene first. The path is left alone, so the follower reappears where it
	 * was. When the new body cannot be built, the remembered creature is forgotten so each game tick tries again.
	 */
	private void renewBody()
	{
		String key = shownKey;
		rebuild(key, shownNpc, shownGilded);
		if (body == null)
			shownKey = null;
	}

	/** Takes the follower off the screen until the next game tick asks for it again. */
	void hide()
	{
		wanted = false;
		path.reset();
		if (body == null)
			return;

		body.recover();
		if (body.isActive())
			body.setActive(false);
	}

	/** Takes the follower off the screen and forgets its model, for when the plugin stops. */
	void remove()
	{
		hide();
		body = null;
		shownKey = null;
		asKey = null;
		showingAs = false;
		fade = 1.0;
	}

	/**
	 * Shows another creature in the lead's place, for a battle scene in which another team member steps in. It stays
	 * on screen where the lead was, and keeps showing that creature, also after the lead changes or a map loads,
	 * until {@code showAs(null, false)} gives the lead back. A creature is always shown in a fresh body, so one that
	 * was knocked down does not stay lying when another of the same species steps in. The name tag over the
	 * follower is the lead's, so it is not drawn while another creature is shown ({@link #showingAs()}).
	 *
	 * @param speciesKey the species to show, or null to show the lead again
	 * @param gilded whether the creature is painted gold; ignored for null
	 */
	void showAs(String speciesKey, boolean gilded)
	{
		showAs(speciesKey, 0, gilded);
	}

	/**
	 * Shows another creature in the lead's place, drawn as the NPC it was caught as; see
	 * {@link #showAs(String, boolean)}.
	 *
	 * @param speciesKey the species to show, or null to show the lead again
	 * @param npcId the NPC id to draw it with, or 0 for the species' first; ignored for null
	 * @param gilded whether the creature is painted gold; ignored for null
	 */
	void showAs(String speciesKey, int npcId, boolean gilded)
	{
		asKey = speciesKey;
		asGilded = gilded;
		asNpc = npcId;
		showingAs = speciesKey != null;
		String key = speciesKey != null ? speciesKey : leadKey;
		boolean look = speciesKey != null ? gilded : leadGilded;
		int npc = speciesKey != null ? npcId : leadNpc;
		boolean different = key != null
			&& (speciesKey != null || !key.equals(shownKey) || look != shownGilded || npc != shownNpc);
		if (different && wanted && client.getGameState() == GameState.LOGGED_IN)
			swap(key, npc, look);
	}

	/** @return whether another creature than the lead is shown, so the lead's name tag must not be drawn */
	boolean showingAs()
	{
		return showingAs;
	}

	/**
	 * Sets how solid the follower is drawn. It is applied on each client tick, also to a body built after this call,
	 * and is back to solid when the plugin stops.
	 *
	 * @param opacity from 0 (unseen) to 1 (solid)
	 */
	void fadeTo(double opacity)
	{
		fade = Math.max(0.0, Math.min(1.0, opacity));
	}

	/**
	 * Sends the lead creature to a spot for a battle: it walks there on its own and then faces a point. Whatever
	 * it was doing as it trailed the player is put aside until {@link #endErrand()}.
	 *
	 * @param toX world x of the spot, in tiles
	 * @param toY world y of the spot, in tiles
	 * @param faceX world x of what it should face there, in tiles
	 * @param faceY world y of what it should face there, in tiles
	 * @return false when the lead is not on screen or the spot is too far, so nothing was started
	 */
	boolean beginErrand(double toX, double toY, double faceX, double faceY)
	{
		if (body == null || !body.isActive())
			return false;

		Player player = client.getLocalPlayer();
		WorldView view = player == null ? null : player.getWorldView();
		if (view == null)
			return false;

		List<int[]> route = new CollisionGround(view, path.plane()).path().route((int) Math.round(path.x()),
			(int) Math.round(path.y()), (int) Math.round(toX), (int) Math.round(toY), FollowerPath.MAX_ERRAND);
		return route != null && path.goVia(route, faceX, faceY);
	}

	/**
	 * Sends the lead creature along a route that was worked out round walls and objects, then has it face a point.
	 * Whatever it was doing as it trailed the player is put aside until {@link #endErrand()}.
	 *
	 * @param route the tiles to step onto, in order, from the tile the lead stands on
	 * @param faceX world x of what it should face there, in tiles
	 * @param faceY world y of what it should face there, in tiles
	 * @return false when the lead is not on screen or the route is too long, so nothing was started
	 */
	boolean beginErrand(List<int[]> route, double faceX, double faceY)
	{
		return body != null && body.isActive() && path.goVia(route, faceX, faceY);
	}

	/**
	 * Finds the tile the lead stands on, for working out a route from it.
	 *
	 * @return the tile as {x, y}; meaningful only while {@link #shown()}
	 */
	int[] tile()
	{
		return new int[]{(int) Math.round(path.x()), (int) Math.round(path.y())};
	}

	/**
	 * Gives the plane and the walls the lead walks among, for working out a route.
	 *
	 * @return a finder over the lead's plane, or null when the lead is not in a scene
	 */
	TilePath tilePath()
	{
		Player player = client.getLocalPlayer();
		WorldView view = player == null ? null : player.getWorldView();
		return view == null ? null : new CollisionGround(view, path.plane()).path();
	}

	/**
	 * Gives the height of the ground the lead is drawn on, so a projectile can leave from its body.
	 *
	 * @return the scene's z of the lead's feet, or NaN when the lead is not on screen
	 */
	double groundZ()
	{
		LocalPoint ground = groundPoint();
		return ground == null ? Double.NaN : Perspective.getTileHeight(client, ground, path.plane());
	}

	/** @return whether the lead is on an errand: false once it was put back next to the player, or hidden */
	boolean onErrand()
	{
		return body != null && path.onErrand();
	}

	/** @return whether the lead has reached the spot of its errand */
	boolean errandArrived()
	{
		return body != null && path.arrived();
	}

	/** Puts the lead on the spot of its errand at once, for when it took too long to walk there. */
	void arriveNow()
	{
		path.arriveNow();
	}

	/** @return the lead's world x, in tiles; a whole number is the middle of a tile */
	double x()
	{
		return path.x();
	}

	/** @return the lead's world y, in tiles; a whole number is the middle of a tile */
	double y()
	{
		return path.y();
	}

	/** @return whether the follower is on screen right now, so a tag over it has something to float above */
	boolean shown()
	{
		return body != null && body.isActive();
	}

	/**
	 * Finds the spot on the ground under the follower, for drawing something over it.
	 *
	 * @return the follower's place in the scene, or null when it is not on screen
	 */
	LocalPoint groundPoint()
	{
		WorldView view = client.getTopLevelWorldView();
		if (!shown() || view == null || !path.placed())
			return null;

		return new LocalPoint(sceneUnits(path.x(), view.getBaseX()), sceneUnits(path.y(), view.getBaseY()), view);
	}

	/** @return how tall the follower is drawn, in local units, for placing a tag above its head */
	int tagHeight()
	{
		return height;
	}

	/**
	 * Acts out a move of a battle with the lead creature.
	 *
	 * @param act the move
	 */
	void act(CreatureBody.Act act)
	{
		if (body != null)
			body.act(act);
	}

	/**
	 * Has the lead creature step up to its target, strike and step back ({@link CreatureBody#meleeTurn}).
	 *
	 * @param distance how far it steps, in tiles
	 * @param act the attack
	 */
	void meleeTurn(double distance, CreatureBody.Act act)
	{
		if (body != null)
			body.meleeTurn(distance, act);
	}

	/**
	 * Tells whether a spot on the ground is free to stand on, by the same check that picks the tile the lead walks
	 * back to ({@link #endErrand()}).
	 *
	 * @param tileX world x of the spot, in tiles
	 * @param tileY world y of the spot, in tiles
	 * @return true when the tile the spot is on is not blocked
	 */
	boolean spotFree(double tileX, double tileY)
	{
		Player player = client.getLocalPlayer();
		WorldView view = player == null ? null : player.getWorldView();
		return view != null && free(view, path.plane(), (int) Math.round(tileX), (int) Math.round(tileY));
	}

	/** Ends an errand: the lead gets up if it was down, and walks back to the player. */
	void endErrand()
	{
		if (body == null)
			return;

		body.recover();
		Player player = client.getLocalPlayer();
		WorldView view = player == null ? null : player.getWorldView();
		if (view == null)
			path.reset();
		else
			path.endErrand(ground(view, path.plane()), routerOver(view));
	}

	private static FollowerPath.Ground ground(WorldView view, int plane)
	{
		CollisionGround walls = new CollisionGround(view, plane);
		return new FollowerPath.Ground()
		{
			@Override
			public boolean free(int x, int y)
			{
				return FollowerService.free(view, plane, x, y);
			}

			@Override
			public boolean reachable(int fromX, int fromY, int toX, int toY)
			{
				return walls.canStep(fromX, fromY, toX, toY);
			}
		};
	}

	private FollowerPath.Router routerOver(WorldView view)
	{
		TilePath tiles = new CollisionGround(view, path.plane()).path();
		return (fromX, fromY, toX, toY) -> tiles.route(fromX, fromY, toX, toY, FollowerPath.MAX_ERRAND);
	}

	/**
	 * Works out how much smaller than the game draws it a creature must be to walk beside the player, the way a
	 * boss pet is a small copy of its boss.
	 *
	 * @param size how many tiles wide the creature stands in the game
	 * @param reach farthest any part of the creature is from its middle, in scene units, at the game's own size
	 * @param height how tall the creature stands, in scene units, at the game's own size
	 * @return the factor to shrink by, from just above 0 to 1; never bigger than 1, so small creatures stay small
	 */
	static double fit(int size, double reach, double height)
	{
		double factor = Math.min(1.0, MAX_TILES / Math.max(1, size));
		if (reach * factor > MAX_REACH)
			factor = MAX_REACH / reach;

		if (height * factor > MAX_HEIGHT)
			factor = MAX_HEIGHT / height;

		return factor;
	}

	/**
	 * Makes the controller that plays a follower's standing or walking animation over and over. RuneLite's own
	 * way of looping steps back by the animation's loop offset, and drops the animation when it has none, as the
	 * plain human stand and walk do; this starts again from the first frame instead, which always works.
	 *
	 * @param client the game client
	 * @param animation the animation to start with, or null to stand still
	 * @return the controller
	 */
	static AnimationController looping(Client client, Animation animation)
	{
		AnimationController controller = new AnimationController(client, animation);
		controller.setOnFinished(AnimationController::reset);
		return controller;
	}

	/**
	 * Turns one of the game's packed colours gold, keeping how light or dark it was. A colour that is already one
	 * of these golds comes back unchanged, so recolouring a model one colour at a time never recolours a face
	 * twice.
	 *
	 * @param color a colour in the game's packed hue, saturation and lightness form
	 * @return the gold of matching lightness, in the same form
	 */
	static short gilded(short color)
	{
		int shade = Math.max(GOLD_DARKEST, Math.min(GOLD_LIGHTEST, color & 127));
		return (short) (GOLD_HUE << 10 | GOLD_SATURATION << 7 | shade);
	}

	/**
	 * Swaps the follower for a different creature. The old one disappears at once; the new one appears where it
	 * stood. A species that cannot be drawn leaves no follower, and is not tried again until the lead changes.
	 *
	 * @param speciesKey the species to show
	 * @param npcId the NPC id to draw it with, 0 for the species' first
	 * @param gilded whether to paint it gold
	 */
	private void rebuild(String speciesKey, int npcId, boolean gilded)
	{
		if (body != null && body.isActive())
			body.setActive(false);

		if (path.onErrand())
			path.reset();

		swap(speciesKey, npcId, gilded);
	}

	/**
	 * Swaps the follower's body for one of another creature, leaving the path, and an errand on it, alone.
	 *
	 * @param speciesKey the species to show
	 * @param npcId the NPC id to draw it with, 0 for the species' first
	 * @param gilded whether to paint it gold
	 */
	private void swap(String speciesKey, int npcId, boolean gilded)
	{
		if (body != null && body.isActive())
			body.setActive(false);

		body = null;
		shownKey = speciesKey;
		shownGilded = gilded;
		shownNpc = npcId;
		Species species = speciesLookup.apply(speciesKey);
		if (species == null)
			return;

		body = CreatureBody.petSized(client, animations, species, npcId, gilded);
		if (body == null)
			return;

		height = body.drawnHeight();
		idle = animations.idle(body.npcId());
		walk = animations.walk(body.npcId());
		body.setBase(idle);
	}

	/**
	 * Paints a model gold.
	 *
	 * @param data the model, changed in place
	 */
	static void gild(ModelData data)
	{
		short[] colors = data.getFaceColors();
		if (colors == null)
			return;

		boolean[] done = new boolean[1 << 16];
		for (short color : colors.clone())
		{
			int index = color & 0xFFFF;
			if (!done[index])
				data.recolor(color, gilded(color));

			done[index] = true;
		}
	}

	/**
	 * @param data a model
	 * @return how far any vertex is from the model's middle, sideways, in scene units
	 */
	static double reachOf(ModelData data)
	{
		float[] x = data.getVerticesX();
		float[] z = data.getVerticesZ();
		double farthest = 0;
		for (int i = 0; i < Math.min(data.getVerticesCount(), Math.min(x.length, z.length)); i++)
			farthest = Math.max(farthest, Math.hypot(x[i], z[i]));

		return farthest;
	}

	static double heightOf(ModelData data)
	{
		float[] y = data.getVerticesY();
		double tallest = 0;
		for (int i = 0; i < Math.min(data.getVerticesCount(), y.length); i++)
			tallest = Math.max(tallest, -y[i]);

		return tallest;
	}

	private static int sceneUnits(double tile, int base)
	{
		return (int) Math.round((tile - base + 0.5) * TILE);
	}

	private static boolean free(WorldView view, int plane, int x, int y)
	{
		CollisionData[] maps = view.getCollisionMaps();
		if (maps == null || plane < 0 || plane >= maps.length || maps[plane] == null)
			return false;

		int[][] flags = maps[plane].getFlags();
		int sceneX = x - view.getBaseX();
		int sceneY = y - view.getBaseY();
		if (sceneX < 0 || sceneY < 0 || sceneX >= flags.length || sceneY >= flags[sceneX].length)
			return false;

		return (flags[sceneX][sceneY] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0;
	}
}
