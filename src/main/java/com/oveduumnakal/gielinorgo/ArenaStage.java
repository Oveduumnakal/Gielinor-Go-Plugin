/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;

/**
 * Acts out a team battle, a City Champion's or a friend's, on the player's own screen. Nobody is really there: the
 * opposing creatures are bodies this plugin adds to the scene, one at a time, {@link ArenaSpots#DISTANCE} tiles from
 * the player, and the lead creature walks up to the first of them. When one of the player's creatures is beaten
 * it fades out and the next one of the team takes its place on the follower; when an opposing creature is beaten
 * the next one fades in. Both sides are drawn pet-sized ({@link CreatureBody#petSized}), and the turns are acted out
 * by the same code as a battle with a wild creature ({@link DuelActing}).
 * The result was decided before the scene starts; the order and pace come from {@link TeamScript}. The scene ends
 * at once, and the lead goes back to the player, when it is cleared, when the follower leaves its errand, or when
 * the game is no longer fully logged in on the same plane. Nothing is sent to the game or the party, and other
 * players see nothing. Every method must be called on the game thread; RuneLite calls {@link #spotOf} from the
 * render thread.
 */
final class ArenaStage implements StageAnchors
{
	private static final Logger log = LoggerFactory.getLogger(ArenaStage.class);

	/** Scene units in one tile. */
	private static final int TILE = 128;

	/** How far the opacity of a fading body moves each client tick: a full fade takes {@link TeamScript#FADE_TICKS}. */
	private static final double FADE_RATE = 1.0 / (TeamScript.FADE_TICKS * TurnMotion.CLIENT_TICKS_PER_GAME_TICK);

	/** The step that makes the loser of a duel go down. */
	private static final BattleTimeline.Step DOWN = new BattleTimeline.Step(BattleTimeline.Beat.FINISH, null);

	/** How many tiles wide the opposing bodies count as when the lead's spot is chosen: pet-sized is one tile. */
	private static final double FOE_TILES = 1.0;

	private final Client client;
	private final FollowerService follower;
	private final AnimationTable animations;
	private final Function<String, Species> species;
	private final DuelActing acting;
	private volatile TeamScript.Run run;
	private CreatureBody foe;
	private boolean foeFresh;
	private OpacityFade foeFade = new OpacityFade(0.0);
	private OpacityFade leadFade = new OpacityFade(1.0);
	private boolean errandOpen;
	private boolean walking;
	private int walked;
	private double centerX;
	private double centerY;
	private int plane;
	private int facing;
	private int foeHeight;
	private volatile Spot foeSpot;

	/**
	 * Creates the stage with nothing playing.
	 *
	 * @param client the game client
	 * @param follower the lead creature
	 * @param animations the animation ids of each NPC
	 * @param species finds a species by its key, or returns null
	 */
	ArenaStage(Client client, FollowerService follower, AnimationTable animations,
		Function<String, Species> species)
	{
		this.client = client;
		this.follower = follower;
		this.animations = animations;
		this.species = species;
		this.acting = new DuelActing(client, new ProjectileModels(client), DuelActing.leadOf(follower), new FoeSide());
	}

	/** The opposing creature's side of a duel: its body, and where it stands. */
	private final class FoeSide implements DuelActing.Side
	{
		@Override
		public void act(CreatureBody.Act act)
		{
			if (foe != null)
				foe.act(act);
		}

		@Override
		public void meleeTurn(double distance, CreatureBody.Act act)
		{
			if (foe != null)
				foe.meleeTurn(distance, act);
		}

		@Override
		public int actTicks(CreatureBody.Act act)
		{
			return foe == null ? 0 : foe.actTicks(act);
		}

		@Override
		public double x()
		{
			return centerX;
		}

		@Override
		public double y()
		{
			return centerY;
		}

		@Override
		public double height()
		{
			return foeHeight;
		}
	}

	/**
	 * Starts acting a team battle out. Nothing starts when another scene is on stage, when the lead is not on
	 * screen, when no free tile is found for the opponent, when the first opposing creature cannot be drawn, or when
	 * the game is not fully logged in. The caller also checks that no wild battle is on stage.
	 *
	 * @param duels the duels of the battle, in order, seen from the player's side
	 * @param leadFirst whether the player's first creature to fight is the lead beside the player, so it needs no
	 *     fade-in and keeps its name tag
	 * @return true when the battle is on stage, so the caller should hold its closing text until it is over
	 */
	boolean start(List<Duel> duels, boolean leadFirst)
	{
		if (duels.isEmpty())
			return false;

		String firstFoe = duels.get(0).theirSpecies();
		return begin(firstFoe, TeamScript.of(duels, leadFirst).run());
	}

	/**
	 * Starts acting out a team battle the player steers, which is fed turn by turn ({@link #append}) and waits in
	 * between ({@link #waiting()}). The same things stop it from starting as for {@link #start}.
	 *
	 * @param foeSpecies the species key of the first opposing creature
	 * @param mineSpecies the species key of the player's first creature to fight
	 * @param mineGilded whether that creature is gilded
	 * @param leadFirst whether the player's first creature to fight is the lead beside the player, so it needs no
	 *     fade-in and keeps its name tag
	 * @return true when the battle is on stage
	 */
	boolean startOpen(String foeSpecies, String mineSpecies, boolean mineGilded, boolean leadFirst)
	{
		return startOpen(foeSpecies, mineSpecies, mineGilded, 0, leadFirst);
	}

	/**
	 * Starts the scene for a battle the player steers, with the player's first creature drawn as the NPC it was
	 * caught as.
	 *
	 * @param foeSpecies the species key of the first opposing creature
	 * @param mineSpecies the species key of the player's first creature to fight
	 * @param mineGilded whether that creature is gilded
	 * @param mineNpcId the NPC id to draw that creature with, 0 for the species' first
	 * @param leadFirst whether the player's first creature to fight is the lead beside the player
	 * @return true when the battle is on stage
	 */
	boolean startOpen(String foeSpecies, String mineSpecies, boolean mineGilded, int mineNpcId, boolean leadFirst)
	{
		TeamScript.Run open = TeamScript.openRun();
		List<TeamScript.Entry> first = new ArrayList<>();
		first.add(TeamScript.enterFoe(foeSpecies));
		if (!leadFirst)
			first.add(TeamScript.enterMine(mineSpecies, mineGilded, mineNpcId));

		open.append(first);
		return begin(foeSpecies, open);
	}

	/**
	 * Adds the next turn of a battle the player steers to the scene.
	 *
	 * @param turn the turn
	 */
	void append(TurnResult turn)
	{
		TeamScript.Run current = run;
		if (current != null && current.open())
			current.append(TeamScript.entriesOf(turn));
	}

	/**
	 * Gives a battle the player steers its end: what is left plays, the opposing creature that won fades out, and
	 * the scene ends.
	 *
	 * @param won whether the player's team won
	 */
	void finishWith(boolean won)
	{
		TeamScript.Run current = run;
		if (current == null || !current.open())
			return;

		current.append(TeamScript.endEntries(won));
		current.close();
	}

	/** @return whether a battle the player steers has played everything it was given and waits for a choice */
	boolean waiting()
	{
		TeamScript.Run current = run;
		return current != null && !walking && current.waiting();
	}

	private boolean begin(String firstFoe, TeamScript.Run script)
	{
		WorldView view = client.getTopLevelWorldView();
		Player player = client.getLocalPlayer();
		if (run != null || view == null || player == null
			|| client.getGameState() != GameState.LOGGED_IN || !follower.shown())
			return false;

		CreatureBody body = null;
		try
		{
			LocalPoint at = player.getLocalLocation();
			if (at == null)
				return false;

			body = buildFoe(firstFoe);
			if (body == null)
				return false;

			double playerX = at.getX() / (double) TILE - 0.5 + view.getBaseX();
			double playerY = at.getY() / (double) TILE - 0.5 + view.getBaseY();
			if (!place(playerX, playerY, player.getCurrentOrientation()))
			{
				release(body);
				return false;
			}

			errandOpen = true;
			foe = body;
			foeFresh = true;
			foeHeight = body.drawnHeight();
			foeFade = new OpacityFade(0.0);
			leadFade = new OpacityFade(1.0);
			plane = view.getPlane();
			facing = FollowerPath.angle(follower.x() - centerX, follower.y() - centerY);
			walking = true;
			walked = 0;
			acting.release();
			run = script;
			return true;
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not stage a team battle", e);
			if (body != null && body != foe)
				release(body);

			finish();
			return false;
		}
	}

	/**
	 * Picks the opponent's tile and sends the lead to its own spot in front of it.
	 *
	 * @param playerX the player's world x, in tiles
	 * @param playerY the player's world y, in tiles
	 * @param orientation the way the player faces
	 * @return whether a free tile was found and the lead set off
	 */
	private boolean place(double playerX, double playerY, int orientation)
	{
		long tileX = Math.round(playerX);
		long tileY = Math.round(playerY);
		for (int[] offset : ArenaSpots.candidates(orientation))
		{
			double x = tileX + offset[0];
			double y = tileY + offset[1];
			if (follower.spotFree(x, y) && sendLead(x, y))
			{
				centerX = x;
				centerY = y;
				return true;
			}
		}

		return false;
	}

	/**
	 * Works out where the lead should stand to face the opponent and sends it there by a route that goes round walls
	 * and objects, as {@link ArenaSpots#lead} says. The gap that worked is kept for the steps of melee attackers.
	 *
	 * @param x the opponent's middle, world x in tiles
	 * @param y the opponent's middle, world y in tiles
	 * @return whether the lead set off; false when no spot can be reached
	 */
	private boolean sendLead(double x, double y)
	{
		TilePath tiles = follower.tilePath();
		if (tiles == null || !follower.shown())
			return false;

		int[] lead = follower.tile();
		ArenaSpots.Lead spot = ArenaSpots.lead(tiles, x, y, FOE_TILES, lead[0], lead[1], FollowerPath.MAX_ERRAND);
		if (spot == null || !follower.beginErrand(spot.route, x, y))
			return false;

		acting.setGap(spot.gap);
		return true;
	}

	/** @return whether a team battle is on stage */
	boolean active()
	{
		return run != null;
	}

	/**
	 * Moves the scene on by one game tick. Called once per game tick.
	 *
	 * @return the lines that are due on this tick, as notices over the creature that was hit or over the player;
	 *     usually none
	 */
	List<Notice> gameTick()
	{
		List<Notice> due = new ArrayList<>();
		TeamScript.Run current = run;
		if (current == null)
			return due;

		try
		{
			if (!stillValid())
			{
				finish();
				return due;
			}

			if (walking && !arrived())
				return due;

			walking = false;
			TeamScript.Tick tick = current.tick();
			if (tick.done())
			{
				finish();
				return due;
			}

			if (tick.started() != null)
				begin(tick.started());

			if (tick.line() != null)
				due.add(TeamScript.noticeOf(tick.line()));
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: the team battle scene failed, ending it", e);
			finish();
		}

		return due;
	}

	/**
	 * Waits for the lead to reach its spot, for at most {@link BattleTimeline#MAX_WALK_TICKS} game ticks, and then
	 * puts it there.
	 *
	 * @return whether the lead is in place and the scene may begin
	 */
	private boolean arrived()
	{
		walked++;
		if (follower.errandArrived())
			return true;

		if (walked < BattleTimeline.MAX_WALK_TICKS)
			return false;

		follower.arriveNow();
		return true;
	}

	/**
	 * Starts one entry of the script.
	 *
	 * @param entry the entry
	 */
	private void begin(TeamScript.Entry entry)
	{
		switch (entry.kind())
		{
			case ENTER_FOE:
				enterFoe(entry.species());
				break;

			case ENTER_MINE:
				follower.fadeTo(0.0);
				follower.showAs(entry.species(), entry.npcId(), entry.gilded());
				leadFade = new OpacityFade(0.0);
				leadFade.moveAt(1.0, FADE_RATE);
				break;

			case HIT:
				acting.play(entry.step(), false);
				if (run != null)
					run.stretch(acting.beatTicks(entry.step()) - BattleTimeline.BEAT_TICKS);

				break;

			case DOWN:
				acting.play(DOWN, entry.side() == TeamScript.Side.MINE);
				break;

			default:
				OpacityFade out = entry.side() == TeamScript.Side.MINE ? leadFade : foeFade;
				out.moveAt(0.0, FADE_RATE);
				break;
		}
	}

	/**
	 * Brings an opposing creature in: the one built when the scene started the first time, a new body each time
	 * after.
	 *
	 * @param key the species key
	 */
	private void enterFoe(String key)
	{
		if (!foeFresh)
		{
			CreatureBody next = buildFoe(key);
			if (next == null)
			{
				finish();
				return;
			}

			CreatureBody old = foe;
			foe = next;
			foeHeight = next.drawnHeight();
			if (old != null)
				release(old);
		}

		foeFresh = false;
		foeFade = new OpacityFade(0.0);
		foeFade.moveAt(1.0, FADE_RATE);
	}

	/**
	 * Builds the body of an opposing creature.
	 *
	 * @param key the species key
	 * @return the body, or null when the species is unknown or cannot be drawn
	 */
	private CreatureBody buildFoe(String key)
	{
		Species found = key == null ? null : species.apply(key);
		return found == null ? null : CreatureBody.petSized(client, animations, found, false);
	}

	/** Turns the opposing body, moves it and fades both sides for one client tick. Called once per client tick. */
	void clientTick()
	{
		if (run == null || foe == null)
			return;

		try
		{
			move();
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: the team battle scene failed, ending it", e);
			finish();
		}
	}

	private void move()
	{
		WorldView view = client.getTopLevelWorldView();
		if (view == null || client.getGameState() != GameState.LOGGED_IN)
			return;

		foe.clientTick();
		acting.advanceTurn(view, plane);
		foeFade.tick();
		leadFade.tick();
		foe.setOpacity(foeFade.opacity());
		follower.fadeTo(leadFade.opacity());
		facing = FollowerPath.turnToward(facing, FollowerPath.angle(follower.x() - centerX, follower.y() - centerY));
		show(view);
	}

	/**
	 * Puts the opposing body where it stands, or takes it out of the scene while it is unseen or off the map.
	 *
	 * @param view the top level world view
	 */
	private void show(WorldView view)
	{
		double push = foe.push();
		double turned = facing * 2 * Math.PI / FollowerPath.FULL_TURN;
		int x = (int) Math.round((centerX - view.getBaseX() + 0.5 - Math.sin(turned) * push) * TILE);
		int y = (int) Math.round((centerY - view.getBaseY() + 0.5 - Math.cos(turned) * push) * TILE);
		boolean inScene = x >= 0 && y >= 0 && x < view.getSizeX() * TILE && y < view.getSizeY() * TILE;
		if (!inScene || !foe.visible())
		{
			if (foe.isActive())
				foe.setActive(false);

			foeSpot = null;
			return;
		}

		LocalPoint at = new LocalPoint(x, y, view);
		foeSpot = new Spot(at, plane, foeHeight);
		foe.setLocation(at, plane);
		foe.setOrientation(facing);
		if (!foe.isActive())
			foe.setActive(true);
	}

	private boolean stillValid()
	{
		WorldView view = client.getTopLevelWorldView();
		return view != null
			&& client.getGameState() == GameState.LOGGED_IN
			&& view.getPlane() == plane
			&& follower.onErrand();
	}

	/**
	 * Finds where battle text floats while a team battle is on stage: over the opposing creature, pushed with its
	 * lunge and recoil, or over the lead's body.
	 *
	 * @param anchor which body
	 * @return its place, or null when no scene is on stage or that body is not drawn
	 */
	@Override
	public Spot spotOf(Notice.Anchor anchor)
	{
		if (run == null)
			return null;

		if (anchor == Notice.Anchor.STAGE_FOE)
			return foeSpot;

		if (anchor != Notice.Anchor.STAGE_LEAD)
			return null;

		LocalPoint ground = follower.groundPoint();
		WorldView view = client.getTopLevelWorldView();
		return ground == null || view == null ? null : new Spot(ground, view.getPlane(), follower.tagHeight());
	}

	/**
	 * Finds the status shown as a tag over a body, while a scene is on stage.
	 *
	 * @param anchor which body
	 * @return its status, or null for none or when no scene is on stage
	 */
	@Override
	public Status statusOf(Notice.Anchor anchor)
	{
		if (run == null || (anchor != Notice.Anchor.STAGE_FOE && anchor != Notice.Anchor.STAGE_LEAD))
			return null;

		return acting.tagOf(anchor == Notice.Anchor.STAGE_LEAD);
	}

	/** Ends the scene, if any: the opposing body goes, the lead is itself again and walks back to the player. */
	void clear()
	{
		finish();
	}

	/**
	 * Ends the scene. Safe to call at any time, any number of times, and it never throws: the state is cleared first,
	 * and each client call is guarded on its own.
	 */
	private void finish()
	{
		boolean touched = run != null || errandOpen;
		boolean sendBack = errandOpen;
		CreatureBody body = foe;
		run = null;
		foe = null;
		foeFresh = false;
		foeSpot = null;
		errandOpen = false;
		walking = false;
		acting.release();
		if (body != null)
			release(body);

		if (!touched)
			return;

		try
		{
			follower.fadeTo(1.0);
			follower.showAs(null, false);
			if (sendBack)
				follower.endErrand();
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not give the lead back", e);
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
			log.debug("Gielinor Go: could not reset an opposing body", e);
		}

		try
		{
			if (body.isActive())
				body.setActive(false);
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not remove an opposing body", e);
		}
	}
}
