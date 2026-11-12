/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Polygon;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Renderable;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.RenderCallback;

/**
 * Acts out a battle between the lead creature and a wild one on the player's own screen. The real monster cannot be
 * moved or animated by a plugin, so it is kept from being drawn and a copy of it stands on its tile instead; the
 * copy turns to face the lead creature, attacks, reacts to hits and goes down. The lead walks up to it first, and
 * walks back afterwards. The real monster is hidden, and an {@link NpcGhost} (built from a private copy of its
 * model) mirrors its place, facing and animation at full size. Everything fades, as {@link BattleFades} plans: at the
 * start the ghost fades from solid while the stand-in fades in, over {@link BattleFades#START_FADE_CLIENT_TICKS}
 * client ticks (1 game tick); during the fight the ghost rests at 0 on the tile the battle started on and at 10%
 * (90% see-through) off it, fading between the two over {@link BattleFades#FADE_CLIENT_TICKS} client ticks (3 game
 * ticks); at a normal end the stand-in fades out while the ghost fades up to solid over the same 3 game ticks, and
 * only then is the real monster shown. An abort shows it at once. The stand-in is drawn at {@link #WILD_SIZE} of
 * the real size. The real monster is untouched, nothing is sent to the game, and other players see nothing.
 * Battle text floats over the stand-in and the lead ({@link #spotOf(Notice.Anchor)}).
 * The order of events comes from {@link BattleTimeline}. Every method must be called on the game thread;
 * RuneLite calls {@link #addEntity(Renderable, boolean)} from it too.
 */
final class BattleStage implements RenderCallback, StageAnchors
{
	private static final Logger log = LoggerFactory.getLogger(BattleStage.class);

	/** Scene units in one tile. */
	private static final int TILE = 128;

	/** How far from the wild creature's edge the lead stands, in tiles. */
	private static final double GAP = 0.75;

	/**
	 * Share of the real monster's size the fighting stand-in is drawn at. The owner asked for three quarters on
	 * 2026-11-11. The see-through ghost stays at full size because it mirrors the real monster.
	 */
	static final double WILD_SIZE = 0.75;

	private final Client client;
	private final FollowerService follower;
	private final AnimationTable animations;
	private volatile BattleTimeline timeline;
	private NPC npc;
	private CreatureBody standIn;
	private NpcGhost ghost;
	private BattleFades fades;
	private boolean errandOpen;
	private WorldPoint home;
	private double centerX;
	private double centerY;
	private int plane;
	private int facing;
	private int foeHeight;
	private int foeTiles = 1;
	private volatile Spot foeSpot;

	/**
	 * Creates the stage with nothing playing.
	 *
	 * @param client the game client
	 * @param follower the lead creature
	 * @param animations the animation ids of each NPC
	 */
	BattleStage(Client client, FollowerService follower, AnimationTable animations)
	{
		this.client = client;
		this.follower = follower;
		this.animations = animations;
	}

	/**
	 * Starts acting a battle out. Nothing starts, and the monster stays as it is, when another battle is on
	 * stage, when the monster is in a fight, when the lead is not on screen or too far away, or when
	 * the monster's model cannot be drawn.
	 *
	 * @param wild the wild monster
	 * @param lines the battle's lines, in the order they are shown
	 * @param leadWon whether the player's creature won
	 * @return true when the battle is on stage, so the caller should hold its text until {@link #fighting()}
	 */
	boolean start(NPC wild, List<BattleLine> lines, boolean leadWon)
	{
		WorldView view = client.getTopLevelWorldView();
		if (timeline != null || wild == null || view == null || client.getGameState() != GameState.LOGGED_IN)
			return false;

		if (CatchEffects.fighting(wild) || wild.isDead()
			|| view.npcs().byIndex(wild.getIndex()) != wild)
			return false;

		LocalPoint at = wild.getLocalLocation();
		if (at == null)
			return false;

		try
		{
			NpcModels.Parts parts = NpcModels.load(client, wild.getId());
			if (parts == null)
				return false;

			CreatureBody body = CreatureBody.create(client, animations, parts, shrunk(parts.widthScale()),
				shrunk(parts.heightScale()));
			if (body == null)
				return false;

			double x = at.getX() / (double) TILE - 0.5 + view.getBaseX();
			double y = at.getY() / (double) TILE - 0.5 + view.getBaseY();
			if (!sendLead(x, y, parts.size() * WILD_SIZE))
				return false;

			errandOpen = true;
			body.setBase(animations.idle(parts.npcId()));
			body.setOpacity(0.0);
			standIn = body;
			npc = wild;
			home = wild.getWorldLocation();
			centerX = x;
			centerY = y;
			plane = view.getPlane();
			facing = wild.getCurrentOrientation();
			foeTiles = Math.max(1, (int) Math.round(parts.size() * WILD_SIZE));
			foeHeight = (int) Math.round(FollowerService.heightOf(parts.data()) * shrunk(parts.heightScale())
				/ NpcModels.NORMAL_SCALE);
			timeline = new BattleTimeline(BattleTimeline.beatsOf(lines), leadWon);
			fades = new BattleFades(true);
			place(view);
			startGhost(wild);
			return true;
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not stage a battle against NPC {}", wild.getId(), e);
			finish();
			if (follower.onErrand())
				follower.endErrand();

			errandOpen = false;
			return false;
		}
	}

	/** @return whether a battle is on stage */
	boolean active()
	{
		return timeline != null;
	}

	/** @return whether the lead has arrived and the battle lines are being played */
	boolean fighting()
	{
		return timeline != null && timeline.fightStarted();
	}

	/**
	 * Moves the battle on by one game tick. Called once per game tick, before the battle lines are shown, so the
	 * first line and the first blow come on the same tick.
	 */
	void gameTick()
	{
		if (timeline == null)
			return;

		try
		{
			step();
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: the battle scene failed, ending it", e);
			finish();
		}
	}

	private void step()
	{
		if (!stillValid())
		{
			finish();
			return;
		}

		if (fades.ending())
			return;

		BattleTimeline.Beat beat = timeline.tick(follower.errandArrived());
		if (timeline.fightStarted() && !follower.errandArrived())
			follower.arriveNow();

		if (beat != null)
			play(beat);

		if (timeline.phase() == BattleTimeline.Phase.DONE)
			beginEnd();
	}

	/** Turns the stand-in and moves it for one client tick. Called once per client tick. */
	void clientTick()
	{
		if (timeline == null || standIn == null)
			return;

		try
		{
			move();
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: the battle scene failed, ending it", e);
			finish();
		}
	}

	private void move()
	{
		WorldView view = client.getTopLevelWorldView();
		if (view == null || client.getGameState() != GameState.LOGGED_IN)
			return;

		standIn.clientTick();
		fades.tick(home != null && home.equals(npc.getWorldLocation()));
		standIn.setOpacity(fades.standInOpacity());
		mirrorGhost();
		int wanted = FollowerPath.angle(follower.x() - centerX, follower.y() - centerY);
		facing = FollowerPath.turnToward(facing, wanted);
		place(view);
		if (fades.finished())
			finish();
	}

	/**
	 * Ends the battle on stage for a monster that left view. Called for every monster that despawns.
	 *
	 * @param gone the monster
	 */
	void forget(NPC gone)
	{
		if (gone == npc)
			finish();
	}

	/**
	 * @param other a monster
	 * @return whether it lies knocked out: the lead beat it in the battle on stage and the stand-in is down, until it
	 *     starts to fade back
	 */
	boolean knockedOut(NPC other)
	{
		return other != null && other == npc && timeline != null && timeline.knockedOut()
			&& fades != null && !fades.ending();
	}

	/** @return the game's index of the monster whose stand-in is on stage, or -1 when no battle is on stage */
	int stagedIndex()
	{
		NPC staged = npc;
		return timeline == null || staged == null ? -1 : staged.getIndex();
	}

	/**
	 * Tells whether the mouse is over the stand-in, so its right-click menu can offer a catch. The stand-in is not
	 * known to the game, so this is worked out here: the mouse counts as over it when it is inside the outline of
	 * the model as drawn (see {@link #hitShapes(LocalPoint)}) or inside the ground tiles it stands on. Any failure
	 * counts as "not over it".
	 *
	 * @return true when a battle is on stage, its stand-in is drawn, and the mouse is over it
	 */
	boolean mouseOverStandIn()
	{
		try
		{
			Spot spot = foeSpot;
			Point mouse = client.getMouseCanvasPosition();
			if (timeline == null || standIn == null || spot == null || mouse == null)
				return false;

			return ShapeHits.anyContains(hitShapes(spot.point()), mouse.getX(), mouse.getY());
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not test the mouse against the stand-in", e);
			return false;
		}
	}

	/**
	 * Builds the canvas shapes the stand-in covers: the outline of its model at its drawn place and facing
	 * ({@link Perspective#getClickbox}), and the polygon of the ground tiles under it, which also covers a model
	 * that lies flat or whose death animation is not followed.
	 *
	 * @param at where the stand-in is drawn
	 * @return the shapes; an entry that cannot be built is left out
	 */
	private List<Shape> hitShapes(LocalPoint at)
	{
		List<Shape> shapes = new ArrayList<>();
		WorldView view = client.getTopLevelWorldView();
		Model model = standIn.hitModel();
		if (view != null && model != null)
		{
			int z = Perspective.getTileHeight(client, at, plane);
			shapes.add(Perspective.getClickbox(client, view, model, facing, at.getX(), at.getY(), z));
		}

		Polygon ground = Perspective.getCanvasTileAreaPoly(client, at, foeTiles);
		if (ground != null)
			shapes.add(ground);

		return shapes;
	}

	/**
	 * @param other a monster
	 * @return whether it is the one on stage, so it is hidden and a stand-in is drawn in its place
	 */
	boolean isStaged(NPC other)
	{
		return other != null && other == npc;
	}

	/**
	 * Finds where battle text floats while a battle is on stage: over the stand-in, pushed with its lunge and
	 * recoil, or over the lead's body.
	 *
	 * @param anchor which body
	 * @return its place, or null when no battle is on stage or that body is not drawn
	 */
	@Override
	public Spot spotOf(Notice.Anchor anchor)
	{
		if (timeline == null)
			return null;

		if (anchor == Notice.Anchor.STAGE_FOE)
			return foeSpot;

		if (anchor != Notice.Anchor.STAGE_LEAD)
			return null;

		LocalPoint ground = follower.groundPoint();
		WorldView view = client.getTopLevelWorldView();
		return ground == null || view == null ? null : new Spot(ground, view.getPlane(), follower.tagHeight());
	}

	/** Ends the battle on stage, if any, and shows the real monster again; the lead walks back to the player. */
	void clear()
	{
		finish();
	}

	/**
	 * Keeps the real monster from being drawn while its stand-in is on stage. RuneLite asks this for every creature,
	 * several times a frame.
	 *
	 * @param renderable the creature RuneLite is about to draw
	 * @param ui whether the question is about its health bar and other overhead parts
	 * @return false for the monster that is in the battle, true for everything else
	 */
	@Override
	public boolean addEntity(Renderable renderable, boolean ui)
	{
		return npc == null || renderable != npc;
	}

	/**
	 * Works out where the lead should stand to face the monster: on the line from the monster to where the lead is
	 * now, just outside the monster's edge.
	 *
	 * @param x the monster's middle, world x in tiles
	 * @param y the monster's middle, world y in tiles
	 * @param size how many tiles wide the stand-in is, after shrinking
	 * @return whether the lead set off
	 */
	private boolean sendLead(double x, double y, double size)
	{
		double dx = follower.x() - x;
		double dy = follower.y() - y;
		double length = Math.hypot(dx, dy);
		double reach = size / 2.0 + GAP;
		double toX = length < 0.01 ? x : x + dx / length * reach;
		double toY = length < 0.01 ? y - reach : y + dy / length * reach;
		return follower.beginErrand(toX, toY, x, y);
	}

	/**
	 * Works out a scale value for the stand-in.
	 *
	 * @param scale the game's scale value for the real monster, where {@link NpcModels#NORMAL_SCALE} is as modelled
	 * @return the value for the stand-in, {@link #WILD_SIZE} of it, never below 1
	 */
	static int shrunk(int scale)
	{
		return Math.max(1, (int) Math.round(scale * WILD_SIZE));
	}

	/**
	 * Builds the see-through copy of the real monster and puts it in the scene. A failure only costs the copy: the
	 * battle plays with the stand-in alone and the real monster stays hidden.
	 *
	 * @param wild the real monster
	 */
	private void startGhost(NPC wild)
	{
		try
		{
			NpcModels.Parts parts = NpcModels.load(client, wild.getId());
			ghost = parts == null ? null : NpcGhost.create(client, parts);
			if (ghost != null)
				ghost.mirror(wild, fades.ghostOpacity());
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not build the see-through copy of NPC {}", wild.getId(), e);
			dropGhost();
		}
	}

	/**
	 * Moves the see-through copy to where the real monster is, at the opacity the fades ask for. The copy is dropped
	 * for good if that fails.
	 */
	private void mirrorGhost()
	{
		if (ghost == null)
			return;

		try
		{
			ghost.mirror(npc, fades.ghostOpacity());
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not mirror the see-through copy of NPC {}", npc.getId(), e);
			dropGhost();
		}
	}

	private void dropGhost()
	{
		if (ghost != null)
		{
			try
			{
				ghost.remove();
			}
			catch (RuntimeException | AssertionError e)
			{
				log.debug("Gielinor Go: could not remove the see-through copy", e);
			}
		}

		ghost = null;
	}

	private void play(BattleTimeline.Beat beat)
	{
		switch (beat)
		{
			case FACE_OFF:
				break;

			case LEAD_ATTACKS:
				follower.act(CreatureBody.Act.ATTACK);
				standIn.act(CreatureBody.Act.HIT);
				break;

			case FOE_ATTACKS:
				standIn.act(CreatureBody.Act.ATTACK);
				follower.act(CreatureBody.Act.HIT);
				break;

			case LEAD_SPECIAL:
				follower.act(CreatureBody.Act.SPECIAL);
				standIn.act(CreatureBody.Act.HIT);
				break;

			case FOE_SPECIAL:
				standIn.act(CreatureBody.Act.SPECIAL);
				follower.act(CreatureBody.Act.HIT);
				break;

			default:
				if (timeline.leadLost())
					follower.act(CreatureBody.Act.DEFEATED);
				else
					standIn.act(CreatureBody.Act.DEFEATED);

				break;
		}
	}

	/**
	 * Starts the fade at a normal end: the lead may walk back at once, the stand-in fades out and the ghost fades up
	 * to solid; the real monster is shown when that is over.
	 */
	private void beginEnd()
	{
		fades.beginEnd();
		if (errandOpen)
			follower.endErrand();

		errandOpen = false;
	}

	private boolean stillValid()
	{
		WorldView view = client.getTopLevelWorldView();
		return view != null
			&& client.getGameState() == GameState.LOGGED_IN
			&& view.getPlane() == plane
			&& view.npcs().byIndex(npc.getIndex()) == npc
			&& !npc.isDead()
			&& !CatchEffects.fighting(npc)
			&& (fades.ending() || follower.onErrand());
	}

	private void place(WorldView view)
	{
		double push = standIn.push();
		double turned = facing * 2 * Math.PI / FollowerPath.FULL_TURN;
		int x = (int) Math.round((centerX - view.getBaseX() + 0.5 - Math.sin(turned) * push) * TILE);
		int y = (int) Math.round((centerY - view.getBaseY() + 0.5 - Math.cos(turned) * push) * TILE);
		boolean inScene = x >= 0 && y >= 0 && x < view.getSizeX() * TILE && y < view.getSizeY() * TILE;
		if (!inScene || !standIn.visible())
		{
			if (standIn.isActive())
				standIn.setActive(false);

			foeSpot = null;
			return;
		}

		LocalPoint at = new LocalPoint(x, y, view);
		foeSpot = new Spot(at, plane, foeHeight);
		standIn.setLocation(at, plane);
		standIn.setOrientation(facing);
		if (!standIn.isActive())
			standIn.setActive(true);
	}

	/**
	 * Takes the stand-in away and gives the real monster back. Safe to call at any time, any number of times, and
	 * it never throws: the state is cleared first, and each client call is guarded on its own.
	 */
	private void finish()
	{
		dropGhost();
		CreatureBody body = standIn;
		boolean sendBack = timeline != null && errandOpen;
		timeline = null;
		foeSpot = null;
		npc = null;
		standIn = null;
		fades = null;
		errandOpen = false;
		if (body != null)
			release(body);

		if (!sendBack)
			return;

		try
		{
			follower.endErrand();
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not send the lead back", e);
		}
	}

	private void release(CreatureBody body)
	{
		try
		{
			body.recover();
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not reset the stand-in", e);
		}

		try
		{
			if (body.isActive())
				body.setActive(false);
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not remove the stand-in", e);
		}
	}
}
