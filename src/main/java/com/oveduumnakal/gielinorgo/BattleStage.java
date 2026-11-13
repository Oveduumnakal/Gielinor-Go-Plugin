/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;

/**
 * Acts out a battle between the lead creature and a wild creature's body on the player's own screen. The wild body
 * is its own thing ({@link WildBody}): it turns to face the lead creature, attacks, reacts to hits and goes down,
 * and gets up again when the battle is over. The lead walks up to it first, round walls and objects
 * ({@link TilePath#approach}), and walks back afterwards.
 * They start {@link TurnMotion#GAP_TILES} tiles apart ({@link TurnMotion#NEAR_GAP_TILES} when that spot cannot be
 * reached). A melee attacker steps up, strikes and steps back along a line that has no wall in it; a ranged or magic
 * attacker stays put and a {@link BattleProjectile} flies from one body to the other at the heights they are drawn at;
 * the blow lands at the same moment of the turn for every style ({@link TurnMotion}). When there is no way for the
 * lead to get to the wild creature, no scene starts and the battle is shown as text ({@link #takeUnreachable()}).
 * Battle text floats over the two bodies ({@link #spotOf(Notice.Anchor)}). Nothing is sent to the game, and other
 * players see nothing. The order of events comes from {@link BattleTimeline}. Every method must be called on the
 * game thread.
 */
final class BattleStage implements StageAnchors
{
	private static final Logger log = LoggerFactory.getLogger(BattleStage.class);

	private final Client client;
	private final FollowerService follower;
	private final LongSupplier clock;
	private volatile BattleTimeline timeline;
	private WildBody foe;
	private boolean errandOpen;
	private int plane;
	private boolean switched;
	private boolean unreachable;
	private boolean frozen;
	private final DuelActing acting;
	private int extraTicks;

	/**
	 * Creates the stage with nothing playing.
	 *
	 * @param client the game client
	 * @param follower the lead creature
	 * @param clock the wall clock in milliseconds, for the wild creature's life
	 */
	BattleStage(Client client, FollowerService follower, LongSupplier clock)
	{
		this.client = client;
		this.follower = follower;
		this.clock = clock;
		this.acting = new DuelActing(client, new ProjectileModels(client), DuelActing.leadOf(follower), new FoeSide());
	}

	/** The wild body's side of a duel: its model, and its place as the battle has moved it. */
	private final class FoeSide implements DuelActing.Side
	{
		@Override
		public void act(CreatureBody.Act act)
		{
			if (foe != null && foe.body() != null)
				foe.body().act(act);
		}

		@Override
		public void meleeTurn(double distance, CreatureBody.Act act)
		{
			if (foe != null && foe.body() != null)
				foe.body().meleeTurn(distance, act);
		}

		@Override
		public int actTicks(CreatureBody.Act act)
		{
			return foe == null || foe.body() == null ? 0 : foe.body().actTicks(act);
		}

		@Override
		public double x()
		{
			return foe == null ? 0 : foe.x();
		}

		@Override
		public double y()
		{
			return foe == null ? 0 : foe.y();
		}

		@Override
		public double height()
		{
			return foe == null ? 0 : foe.drawnHeight();
		}

		@Override
		public double groundZ()
		{
			return foe == null ? Double.NaN : foe.groundZ();
		}
	}

	/**
	 * Starts acting a battle out. Nothing starts when another battle is on stage, when the wild creature is busy or
	 * leaving, when the lead is not on screen or too far away, or when the lead has no way to get to it.
	 *
	 * @param wild the wild creature's body
	 * @param lines the battle's lines, in the order they are shown
	 * @param leadWon whether the player's creature won
	 * @return true when the battle is on stage, so the caller should hold its text until {@link #linesDue()}
	 */
	boolean start(WildBody wild, List<BattleLine> lines, boolean leadWon)
	{
		return start(wild, lines, leadWon, false);
	}

	/**
	 * Starts acting out a battle the player steers. It is fed turn by turn ({@link #append}) and waits between turns
	 * ({@link #waiting()}). The same things stop it from starting as for {@link #start}.
	 *
	 * @param wild the wild creature's body
	 * @param lines the lines known so far, usually the opening line
	 * @return true when the battle is on stage
	 */
	boolean startOpen(WildBody wild, List<BattleLine> lines)
	{
		return start(wild, lines, false, true);
	}

	private boolean start(WildBody wild, List<BattleLine> lines, boolean leadWon, boolean open)
	{
		WorldView view = client.getTopLevelWorldView();
		unreachable = false;
		if (timeline != null || wild == null || view == null || client.getGameState() != GameState.LOGGED_IN
			|| wild.body() == null || wild.spot() == null || wild.plane() != view.getPlane())
			return false;

		if (!wild.life().beginBattle())
			return false;

		try
		{
			wild.stand();
			if (!sendLead(wild))
			{
				wild.life().endBattle(clock.getAsLong());
				return false;
			}

			errandOpen = true;
			foe = wild;
			plane = view.getPlane();
			timeline = open ? BattleTimeline.open(BattleTimeline.stepsOf(lines))
				: new BattleTimeline(BattleTimeline.stepsOf(lines), leadWon);
			acting.release();
			frozen = false;
			return true;
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not stage a battle against NPC {}", wild.entry().npcId(), e);
			wild.life().endBattle(clock.getAsLong());
			finish();
			if (follower.onErrand())
				follower.endErrand();

			errandOpen = false;
			return false;
		}
	}

	/**
	 * Tells whether the last battle that could not start had no way for the lead to get to the wild creature, and
	 * forgets it, so the caller can say once that the battle is shown as text.
	 *
	 * @return true when the lead had no route to the wild creature
	 */
	boolean takeUnreachable()
	{
		boolean was = unreachable;
		unreachable = false;
		return was;
	}

	/** @return whether a battle is on stage */
	boolean active()
	{
		return timeline != null;
	}

	/**
	 * @return whether the battle lines may start showing: one game tick after the first beat, so each line appears
	 *     as its blow lands
	 */
	boolean linesDue()
	{
		return timeline != null && timeline.linesDue();
	}

	/**
	 * Hands over the game ticks the beats played since the last call lasted beyond {@link BattleTimeline#BEAT_TICKS},
	 * so the battle lines can wait for them ({@link NoticeBoard#stretch(int)}).
	 *
	 * @return game ticks, 0 for none
	 */
	int takeExtraTicks()
	{
		int taken = extraTicks;
		extraTicks = 0;
		return taken;
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

		if (foe.life().state() == WildLife.State.IN_CATCH)
		{
			if (!frozen)
			{
				acting.settle();
				if (foe.body() != null)
					foe.body().endTurn();
			}

			frozen = true;
			return;
		}

		if (frozen)
		{
			finish();
			return;
		}

		BattleTimeline.Step step = timeline.tick(follower.errandArrived());
		if (timeline.fightStarted() && !follower.errandArrived())
			follower.arriveNow();

		if (step != null)
		{
			acting.play(step, timeline.leadLost());
			int extra = acting.beatTicks(step) - BattleTimeline.BEAT_TICKS;
			timeline.stretch(extra);
			extraTicks += Math.max(0, extra);
		}

		if (step != null && step.beat() == BattleTimeline.Beat.LEAD_ENTERS)
			showLead(step.entered());

		if (timeline.knockedOut() && !foe.life().knockedOut())
			foe.life().knockOut();

		if (timeline.phase() == BattleTimeline.Phase.DONE)
			finish();
	}

	/** Moves the projectile and the blow of the turn on for one client tick. Called once per client tick. */
	void clientTick()
	{
		if (timeline == null || foe == null)
			return;

		try
		{
			WorldView view = client.getTopLevelWorldView();
			if (view == null || client.getGameState() != GameState.LOGGED_IN)
				return;

			acting.advanceTurn(view, plane);
			if (foe.life().state() != WildLife.State.IN_CATCH)
				foe.faceToward(follower.x(), follower.y());
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: the battle scene failed, ending it", e);
			finish();
		}
	}

	/**
	 * Ends the battle on stage when its wild creature's body is taken off the screen. Called when a body is removed.
	 *
	 * @param wildId the wild creature's id
	 */
	void forget(int wildId)
	{
		if (foe != null && foe.entry().wildId() == wildId)
			finish();
	}

	/**
	 * Adds the next turn of a battle the player steers: its blows, and the lead creature falling when another one of
	 * the team must step in.
	 *
	 * @param turn the turn
	 */
	void append(TurnResult turn)
	{
		BattleTimeline current = timeline;
		if (current == null)
			return;

		List<BattleTimeline.Step> steps = new ArrayList<>(BattleTimeline.stepsOf(turn.hits()));
		if (turn.mineFainted() && turn.status() == TurnResult.Status.NEEDS_SWITCH)
			steps.add(new BattleTimeline.Step(BattleTimeline.Beat.LEAD_FELL, null));

		current.append(steps);
	}

	/**
	 * Gives an automatic battle in which creatures step in all its beats and its end at once, as a battle the player
	 * steers is fed turn by turn. The beats play in order, a creature that falls lies down and the next one walks in.
	 *
	 * @param script what happens after the face-off
	 * @param leadWon whether the player's side won
	 * @param opener the creature that opened the battle
	 * @param leadOpened whether it is the lead, which already stands beside the player
	 */
	void playScript(WildScript script, boolean leadWon, Combatant opener, boolean leadOpened)
	{
		if (timeline == null)
			return;

		if (!leadOpened)
			showLead(opener);

		timeline.append(script.steps());
		timeline.finishWith(leadWon);
	}

	/**
	 * Gives a battle the player steers its end: what is left plays, and then the loser goes down.
	 *
	 * @param leadWon whether the player's creature won
	 */
	void finishWith(boolean leadWon)
	{
		if (timeline != null)
			timeline.finishWith(leadWon);
	}

	/** @return whether a battle the player steers has played everything it was given and waits for a choice */
	boolean waiting()
	{
		return timeline != null && timeline.waiting() && foe != null
			&& foe.life().state() != WildLife.State.IN_CATCH;
	}

	/**
	 * Shows another creature of the team as the lead, for a switch. The lead's own name tag is hidden while another
	 * is shown, and {@code finish} gives the lead back.
	 *
	 * @param entered the creature that steps in
	 */
	void showLead(Combatant entered)
	{
		if (timeline == null)
			return;

		try
		{
			acting.settle();
			follower.showAs(entered.speciesKey(), entered.npcId(), entered.isGilded());
			switched = true;
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not show the creature that stepped in", e);
		}
	}

	/** Ends a battle the player steers because the player ran: the wild creature stands, the lead walks back. */
	void runAway()
	{
		if (timeline != null)
			finish();
	}

	/** @return the id of the wild creature whose battle is on stage, or -1 when no battle is on stage */
	int stagedId()
	{
		WildBody staged = foe;
		return timeline == null || staged == null ? -1 : staged.entry().wildId();
	}

	/**
	 * Finds where battle text floats while a battle is on stage: over the wild body, pushed with its lunge and
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
			return foe == null ? null : foe.spot();

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
		if (timeline == null || (anchor != Notice.Anchor.STAGE_FOE && anchor != Notice.Anchor.STAGE_LEAD))
			return null;

		return acting.tagOf(anchor == Notice.Anchor.STAGE_LEAD);
	}

	/** Ends the battle on stage, if any; the wild creature stands again and the lead walks back to the player. */
	void clear()
	{
		finish();
	}

	/**
	 * Works out where the lead should stand to face the wild creature, and sends it there by a route that goes round
	 * walls and objects: {@link TurnMotion#GAP_TILES} tiles from the creature's edge, or
	 * {@link TurnMotion#NEAR_GAP_TILES} when that spot cannot be reached. The spot has a clear line to the creature,
	 * so the steps of a melee attacker cross no wall. The gap that worked is kept for those steps.
	 *
	 * @param wild the wild creature's body
	 * @return whether the lead set off; false when no spot can be reached
	 */
	private boolean sendLead(WildBody wild)
	{
		TilePath tiles = follower.tilePath();
		if (tiles == null || !follower.shown())
			return false;

		int[] lead = follower.tile();
		double dx = follower.x() - wild.x();
		double dy = follower.y() - wild.y();
		double length = Math.hypot(dx, dy);
		int foeX = (int) Math.round(wild.x());
		int foeY = (int) Math.round(wild.y());
		for (double tryGap : new double[]{TurnMotion.GAP_TILES, TurnMotion.NEAR_GAP_TILES})
		{
			double reach = wild.tiles() / 2.0 + tryGap;
			double aimX = length < 0.01 ? wild.x() : wild.x() + dx / length * reach;
			double aimY = length < 0.01 ? wild.y() - reach : wild.y() + dy / length * reach;
			List<int[]> route = tiles.approach(foeX, foeY, (int) Math.round(reach), aimX, aimY, lead[0], lead[1],
				FollowerPath.MAX_ERRAND);
			if (route == null)
				continue;

			int[] spot = route.isEmpty() ? lead : route.get(route.size() - 1);
			if (follower.beginErrand(route, wild.x(), wild.y()))
			{
				acting.setGap(Math.max(TurnMotion.CONTACT_TILES,
					Math.hypot(spot[0] - wild.x(), spot[1] - wild.y()) - wild.tiles() / 2.0));
				return true;
			}
		}

		unreachable = true;
		return false;
	}

	private boolean stillValid()
	{
		WorldView view = client.getTopLevelWorldView();
		WildLife.State state = foe == null ? WildLife.State.GONE : foe.life().state();
		boolean foeHere = state != WildLife.State.LEAVING && state != WildLife.State.GONE;
		return view != null
			&& client.getGameState() == GameState.LOGGED_IN
			&& view.getPlane() == plane
			&& foeHere
			&& follower.onErrand();
	}

	/**
	 * Ends the scene. Safe to call at any time, any number of times, and it never throws: the state is cleared
	 * first, and each client call is guarded on its own.
	 */
	private void finish()
	{
		WildBody wild = foe;
		boolean sendBack = timeline != null && errandOpen;
		timeline = null;
		frozen = false;
		acting.release();
		boolean giveBack = switched;
		switched = false;
		foe = null;
		errandOpen = false;
		if (wild != null)
			release(wild);

		if (giveBack)
		{
			try
			{
				follower.showAs(null, false);
			}
			catch (RuntimeException | AssertionError e)
			{
				log.debug("Gielinor Go: could not give the lead back", e);
			}
		}

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

	private void release(WildBody wild)
	{
		try
		{
			wild.freeFacing();
			if (wild.life().state() == WildLife.State.IN_BATTLE || wild.life().knockedOut())
			{
				if (wild.body() != null)
					wild.body().recover();

				wild.life().endBattle(clock.getAsLong());
			}
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not stand the wild creature up", e);
		}
	}
}
