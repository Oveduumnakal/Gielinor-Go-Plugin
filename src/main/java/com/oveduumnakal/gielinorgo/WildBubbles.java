/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * The state machine of the action bubbles that pop out above one wild creature: which creature has them, how far each
 * has flown, when the jar cluster and the card open and close, and what a press means. It is a pure function of the
 * times passed in, so a test drives it with a fake clock. It holds no RuneLite types, takes no locks and is confined
 * to the client thread; the mouse listener on the event thread only ever sees the immutable {@link BubbleHits} the
 * overlay publishes.
 *
 * <p>One group is {@code current}; a group that was replaced or dismissed keeps painting while it flies back, with no
 * click targets. A group runs {@code OPENING}, {@code OPEN}, {@code CLOSING}, then is gone. The pointer position is
 * passed in with every tick, so hover, the cluster and the card all follow from {@link #tick}.
 */
final class WildBubbles
{
	/** How many tiles beyond the creature's body still count as near; the glue reads it. */
	static final int NEAR_TILES = 2;

	/**
	 * How long the pointer may stay outside before the group goes back, in milliseconds. An open jar cluster goes
	 * back first and the main bubbles follow once it has landed on the Catch bubble.
	 */
	static final long GRACE_MS = 350;

	/**
	 * How long the pointer must be off every bubble before the bubbles, held still while it was on one, glide back to
	 * the creature, in milliseconds.
	 */
	static final long HOLD_MS = 1000;

	/** How long the pointer rests on the Catch bubble before the cluster opens. */
	static final long CLUSTER_HOVER_MS = 450;

	/** How long the pointer may stay outside the cluster before it closes. */
	static final long CLUSTER_GRACE_MS = 300;

	/** How long the pointer rests on the Info bubble before the card peeks out. */
	static final long CARD_HOVER_MS = 150;

	/** How long the pointer may stay outside the Info bubble and the card before an unpinned card hides. */
	static final long CARD_HIDE_MS = 200;

	/** How many pixels a hit or a hot zone is grown by. */
	static final int HIT_SLOP = BubbleHits.HIT_SLOP;

	/** How far a bubble must have flown, 0 to 1, before a click on it counts. */
	static final double CLICKABLE_AT = 0.5;

	/** The id {@link #wildId()} gives when no creature has bubbles. */
	static final int NO_TARGET = -1;

	/** The button number of a left press. */
	static final int LEFT = 1;

	/** The button number of a right press. */
	static final int RIGHT = 3;

	/** The size the card is assumed to be until the painter has measured it. */
	static final Dimension DEFAULT_CARD_SIZE = new Dimension(190, 130);

	private static final BubbleKind[] MAIN_KINDS = {BubbleKind.JAR, BubbleKind.BATTLE, BubbleKind.INFO};

	/** A creature that could have the bubbles: its id, whether the pointer is near it and how far it is. */
	static final class Candidate
	{
		private final int wildId;
		private final boolean near;
		private final double distSq;

		/**
		 * @param wildId the creature's own id
		 * @param near whether the pointer is near it
		 * @param distSq the squared canvas distance from the pointer to the creature's middle
		 */
		Candidate(int wildId, boolean near, double distSq)
		{
			this.wildId = wildId;
			this.near = near;
			this.distSq = distSq;
		}

		/** @return the creature's own id */
		int wildId()
		{
			return wildId;
		}

		/** @return whether the pointer is near it */
		boolean near()
		{
			return near;
		}

		/** @return the squared distance to its middle */
		double distSq()
		{
			return distSq;
		}
	}

	/** The creature {@link #pick} chose and whether the pointer counts as being at it. */
	static final class Pick
	{
		private final int wildId;
		private final boolean inside;

		private Pick(int wildId, boolean inside)
		{
			this.wildId = wildId;
			this.inside = inside;
		}

		/** @return the creature's id, or {@link #NO_TARGET} when none is chosen */
		int wildId()
		{
			return wildId;
		}

		/** @return whether the pointer counts as being at it; false lets the grace time run */
		boolean inside()
		{
			return inside;
		}
	}

	/** Where a group is in its life. */
	private enum Phase
	{
		/** At least one bubble is still flying out. */
		OPENING,

		/** All three bubbles have landed. */
		OPEN,

		/** The bubbles are flying back. */
		CLOSING
	}

	/** Where the jar cluster is in its life. */
	private enum ClusterState
	{
		/** Not shown. */
		CLOSED,

		/** Open or opening. */
		OPEN,

		/** The jars are flying back. */
		CLOSING
	}

	/** Whether the card is hidden, peeking while hovered, or pinned. */
	private enum CardState
	{
		/** Not shown, or fading out. */
		NONE,

		/** Shown while the pointer is on the Info bubble or the card. */
		PEEK,

		/** Shown until the player clicks elsewhere. */
		PINNED
	}

	/** Everything one creature's bubbles keep between ticks. */
	private static final class Group
	{
		private final int wildId;
		private final int generation;
		private final Tween[] path = new Tween[3];
		private final Smoothed[] hover = new Smoothed[3];
		private final Map<JarType, Smoothed> clusterHover = new EnumMap<>(JarType.class);
		private Phase phase = Phase.OPENING;
		private BubbleOffer offer;
		private BubbleAnchors anchors;
		private long outsideSince = -1;
		private BubbleAnchors held;
		private long holdOffSince = -1;
		private Tween glide;
		private ClusterState cluster = ClusterState.CLOSED;
		private Tween[] slots = new Tween[0];
		private int[] ranks = new int[0];
		private long jarHoverSince = -1;
		private boolean armed = true;
		private long clusterOutsideSince = -1;
		private CardState card = CardState.NONE;
		private Tween cardTween = new Tween(0, 0, 0, 0, 0, Easing.LINEAR);
		private BubbleFrame.Side cardSide;
		private long infoHoverSince = -1;
		private long cardOutsideSince = -1;
		private List<Rectangle2D> zones = Collections.emptyList();
		private BubbleHits hits = BubbleHits.EMPTY;

		private Group(int wildId, int generation)
		{
			this.wildId = wildId;
			this.generation = generation;
			for (int i = 0; i < 3; i++)
			{
				hover[i] = new Smoothed(0, BubbleMotion.HOVER_MS);
			}
		}

		private Smoothed hoverOf(JarType jar)
		{
			return clusterHover.computeIfAbsent(jar, j -> new Smoothed(0, BubbleMotion.HOVER_MS));
		}
	}

	/** The places of everything one group shows this moment. */
	private static final class Geom
	{
		private final BubbleLayout.Arc arc;
		private final BubbleLayout.ClusterPlan cluster;
		private final BubbleLayout.CardPlan card;

		private Geom(BubbleLayout.Arc arc, BubbleLayout.ClusterPlan cluster, BubbleLayout.CardPlan card)
		{
			this.arc = arc;
			this.cluster = cluster;
			this.card = card;
		}
	}

	private Group current;
	private final List<Group> retiring = new ArrayList<>();
	private int generation;
	private boolean paused;
	private MotionMode mode = MotionMode.FULL;
	private Rectangle viewport = new Rectangle(0, 0, 765, 503);
	private List<Rectangle> blockers = Collections.emptyList();
	private Dimension cardSize = new Dimension(DEFAULT_CARD_SIZE);

	/**
	 * Chooses which creature has the bubbles. A creature that has them and still has the pointer is kept even when
	 * another is nearer; else the nearest creature the pointer is near wins, the lower id on a tie; else a creature
	 * that has them stays, with the pointer outside, so the grace time runs; else none.
	 *
	 * @param currentId the creature that has the bubbles now, or {@link #NO_TARGET}
	 * @param currentEligible whether that creature may still have them
	 * @param currentInside whether the pointer is near it or in its hot zones; see {@link #holds}
	 * @param candidates the eligible creatures
	 * @return the choice
	 */
	static Pick pick(int currentId, boolean currentEligible, boolean currentInside, List<Candidate> candidates)
	{
		if (currentId != NO_TARGET && currentEligible && currentInside)
			return new Pick(currentId, true);

		Candidate best = null;
		for (Candidate candidate : candidates)
		{
			if (!candidate.near())
				continue;

			if (best == null || candidate.distSq() < best.distSq()
				|| (candidate.distSq() == best.distSq() && candidate.wildId() < best.wildId()))
				best = candidate;
		}

		if (best != null)
			return new Pick(best.wildId(), true);

		if (currentId != NO_TARGET && currentEligible)
			return new Pick(currentId, false);

		return new Pick(NO_TARGET, false);
	}

	/**
	 * Moves the bubbles on one client tick. Call it with the creature the pointer is at, found with {@link #pick}.
	 * A null offer or null anchors means no creature may have them, which dismisses the current group at once.
	 *
	 * @param offer what the creature offers, or null for none
	 * @param anchors where the creature is on the canvas, or null when it is not visible
	 * @param viewport the game viewport on the canvas
	 * @param near whether the pointer is near the creature's body
	 * @param blockers rectangles the bubbles must not cover, such as the open painted window; copied
	 * @param px the pointer's canvas x
	 * @param py the pointer's canvas y
	 * @param now the time in milliseconds
	 * @param mode the motion mode
	 */
	void tick(BubbleOffer offer, BubbleAnchors anchors, Rectangle viewport, boolean near, List<Rectangle> blockers,
		int px, int py, long now, MotionMode mode)
	{
		this.mode = mode;
		this.viewport = new Rectangle(viewport);
		this.blockers = Collections.unmodifiableList(new ArrayList<>(blockers));
		prune(now);
		if (paused)
			return;

		if (offer == null || anchors == null)
		{
			retireCurrent(now);
			return;
		}

		if (current != null && current.wildId != offer.wildId())
			retireCurrent(now);

		Group g = current;
		if (g == null)
		{
			if (!near || arcBlocked(anchors))
				return;

			generation++;
			g = new Group(offer.wildId(), generation);
			current = g;
			for (int i = 0; i < 3; i++)
				g.path[i] = BubbleMotion.appear(0, now, i, mode);
		}

		g.offer = offer;
		g.anchors = anchors;
		settle(g, now);
		if (g.phase == Phase.CLOSING)
		{
			if (!arcBlocked(anchors) && (near || onMainDisc(g, anchors, px, py, now)))
				reverse(g, now);
			else
				return;
		}

		BubbleAnchors at = placed(g, anchors, now);
		Geom geom = geom(g, at, now);
		if (blocked(groupBounds(g, geom)))
		{
			retireCurrent(now);
			return;
		}

		List<BubbleFrame.Disc> discs = discs(g, geom, at, now, true);
		updateHover(g, discs, px, py, now);
		if (holdLogic(g, discs, anchors, near, px, py, now))
		{
			refresh(g, anchors, now);
			return;
		}

		boolean inside = near || inZones(zones(g, geom, discs, now), px, py);
		if (inside)
			g.outsideSince = -1;
		else if (g.outsideSince < 0)
			g.outsideSince = now;

		if (!inside && now - g.outsideSince >= GRACE_MS)
		{
			if (g.cluster != ClusterState.CLOSED)
			{
				closeCluster(g, now, false);
				refresh(g, anchors, now);
				return;
			}

			close(g, now);
			refresh(g, anchors, now);
			return;
		}

		clusterLogic(g, discs, geom, px, py, now);
		cardLogic(g, discs, geom, px, py, now);
		refresh(g, anchors, now);
	}

	/**
	 * Tells whether the pointer is in a hot zone of the current group: a bubble, the corridor down to the creature, the
	 * jar cluster and the gap to it, or the card and the gap to it. It judges by the zones of the last tick and does
	 * not need the pointer to be over the scene.
	 *
	 * @param x the pointer's canvas x
	 * @param y the pointer's canvas y
	 * @return whether the current group, while opening or open, is held by the pointer being there
	 */
	boolean holds(double x, double y)
	{
		Group g = current;
		return g != null && !paused && g.phase != Phase.CLOSING && inZones(g.zones, x, y);
	}

	/**
	 * Tells whether the pointer is over something clickable, as of the last tick.
	 *
	 * @param x the pointer's canvas x
	 * @param y the pointer's canvas y
	 * @return whether a bubble or the card is under it; the corridor does not count
	 */
	boolean covers(double x, double y)
	{
		Group g = current;
		return g != null && !paused && g.phase != Phase.CLOSING && g.hits.covers(x, y);
	}

	/**
	 * Works out what a press on a bubble does.
	 *
	 * @param generation the generation the press was made against, from {@link BubbleHits#generation()}
	 * @param kind what was hit
	 * @param jar the jar of the bubble, for {@link BubbleKind#CLUSTER_JAR}
	 * @param button 1 for left, 3 for right; other buttons do nothing
	 * @param now the time in milliseconds
	 * @return what the plugin must do; {@link BubbleAction#NONE} for a stale or closing group or a press that only
	 *         changes the bubbles
	 */
	BubbleAction press(int generation, BubbleKind kind, JarType jar, int button, long now)
	{
		Group g = current;
		if (paused || g == null || g.generation != generation || g.phase == Phase.CLOSING)
			return BubbleAction.NONE;

		if (button != LEFT && button != RIGHT)
			return BubbleAction.NONE;

		boolean left = button == LEFT;
		BubbleOffer offer = g.offer;
		if (kind == BubbleKind.JAR)
		{
			if (left)
			{
				closeCluster(g, now, true);
				return BubbleAction.throwJar(g.wildId, offer.jar());
			}

			if (g.cluster == ClusterState.OPEN)
				closeCluster(g, now, true);
			else if (!offer.owned().isEmpty())
				openCluster(g, now);

			return BubbleAction.NONE;
		}

		if (kind == BubbleKind.BATTLE)
			return left ? BubbleAction.battle(g.wildId) : BubbleAction.NONE;

		if (kind == BubbleKind.INFO)
		{
			if (left)
				togglePin(g, now);

			return BubbleAction.NONE;
		}

		if (kind == BubbleKind.CLUSTER_JAR && g.cluster == ClusterState.OPEN && ownsJar(offer, jar))
		{
			closeCluster(g, now, true);
			return BubbleAction.select(g.wildId, jar);
		}

		return BubbleAction.NONE;
	}

	/**
	 * Handles a press that hit nothing while the hits were modal: closes the cluster and unpins and hides the card.
	 *
	 * @param generation the generation of the hits the press was made against
	 * @param now the time in milliseconds
	 */
	void pressedOutside(int generation, long now)
	{
		Group g = current;
		if (paused || g == null || g.generation != generation)
			return;

		closeCluster(g, now, false);
		if (g.card != CardState.NONE)
			hideCard(g, now);
	}

	/**
	 * Closes the current group at once with no grace time, as when the creature is gone, caught, leaving, in a battle
	 * or under a window. The bubbles still fly back, with no click targets.
	 *
	 * @param now the time in milliseconds
	 */
	void dismiss(long now)
	{
		retireCurrent(now);
	}

	/**
	 * Pauses or resumes the bubbles, as while the game's own menu is open. Paused, nothing is painted or clickable and
	 * the state is kept; on resume the group shows again without playing its opening, and the grace times count from
	 * then.
	 *
	 * @param paused true to pause
	 */
	void pause(boolean paused)
	{
		if (this.paused && !paused)
		{
			resetTimers(current);
			for (Group g : retiring)
				resetTimers(g);
		}

		this.paused = paused;
	}

	/** Drops everything at once with no animation, as on logout. */
	void hideNow()
	{
		current = null;
		retiring.clear();
	}

	/** @return the id of the creature that has the bubbles, even while they fly back, or {@link #NO_TARGET} */
	int wildId()
	{
		return current == null ? NO_TARGET : current.wildId;
	}

	/**
	 * @param wildId a creature's id
	 * @return whether that creature's bubbles are opening or open, which hides its tooltip
	 */
	boolean showing(int wildId)
	{
		Group g = current;
		return g != null && g.wildId == wildId && g.phase != Phase.CLOSING;
	}

	/** @return whether the jar cluster is open or opening on the current group */
	boolean clusterOpen()
	{
		return current != null && current.cluster == ClusterState.OPEN;
	}

	/** @return whether the current group's card is pinned */
	boolean cardPinned()
	{
		return current != null && current.card == CardState.PINNED;
	}

	/** @return whether the current group's card is shown, peeking or pinned */
	boolean cardShown()
	{
		return current != null && current.card != CardState.NONE;
	}

	/**
	 * Gives the words of the tooltip for a bubble under the pointer: the jar bubbles name the jar and its catch chance
	 * on this creature, Battle says it starts combat and Info says it shows information. The Info bubble has none
	 * while its card shows, so the two never fight over the same spot.
	 *
	 * @param hit what is under the pointer, from the published {@link BubbleHits}; may be null
	 * @return the tooltip words, or null when there is nothing to say
	 */
	String tip(BubbleHits.Hit hit)
	{
		Group g = current;
		if (hit == null || g == null || g.offer == null || paused || g.phase == Phase.CLOSING)
			return null;

		if (hit.kind() == BubbleKind.BATTLE)
			return "Start combat";

		if (hit.kind() == BubbleKind.INFO)
			return g.card == CardState.NONE ? "Information" : null;

		JarType jar = hit.kind() == BubbleKind.JAR ? g.offer.jar() : hit.jar();
		if (hit.kind() != BubbleKind.JAR && hit.kind() != BubbleKind.CLUSTER_JAR)
			return null;

		if (jar == null)
			return "No jar to throw";

		for (JarSuggest.JarChoice choice : g.offer.owned())
		{
			if (choice.jar() == jar)
				return jar.label() + " jar\n" + choice.pct() + "% catch chance";
		}

		return jar.label() + " jar";
	}

	/** @return the words of the current creature's card for the painter to measure, or null when there is none */
	WildCardContent cardContent()
	{
		return current == null || current.offer == null ? null : current.offer.card();
	}

	/**
	 * Works out what to paint now. Groups that fly back are drawn first and are not clickable; the current group is
	 * drawn over them. Nothing is painted while paused.
	 *
	 * @param anchorsOf gives a creature's anchors for this frame, or null when it cannot be placed, which stops that
	 *        group painting at once
	 * @param viewport the game viewport
	 * @param cardSize the size of the card at scale 1, as the painter measured it; null keeps the last size
	 * @param now the time in milliseconds
	 * @param mode the motion mode
	 * @return the frame; {@link BubbleFrame#NONE} when there is nothing to paint
	 */
	BubbleFrame frame(IntFunction<BubbleAnchors> anchorsOf, Rectangle viewport, Dimension cardSize, long now,
		MotionMode mode)
	{
		this.mode = mode;
		if (cardSize != null)
			this.cardSize = new Dimension(cardSize);

		if (paused)
			return BubbleFrame.NONE;

		prune(now);
		List<BubbleFrame.Disc> discs = new ArrayList<>();
		BubbleFrame.Card card = null;
		for (Group g : retiring)
		{
			BubbleAnchors a = placed(g, anchorsOf.apply(g.wildId), now);
			if (a == null)
				continue;

			Geom geom = geom(g, a, viewport, now);
			discs.addAll(discs(g, geom, a, now, false));
			if (card == null)
				card = cardOf(g, geom, now, false);
		}

		int gen = 0;
		boolean modal = false;
		Group c = current;
		BubbleAnchors anchors = c == null ? null : placed(c, anchorsOf.apply(c.wildId), now);
		if (anchors != null)
		{
			boolean live = c.phase != Phase.CLOSING;
			Geom geom = geom(c, anchors, viewport, now);
			discs.addAll(discs(c, geom, anchors, now, live));
			BubbleFrame.Card own = cardOf(c, geom, now, live);
			if (own != null)
				card = own;

			gen = live ? c.generation : 0;
			modal = live && (c.cluster == ClusterState.OPEN || c.card == CardState.PINNED);
		}

		if (discs.isEmpty() && card == null)
			return BubbleFrame.NONE;

		return new BubbleFrame(discs, card, gen, modal);
	}

	private boolean onMainDisc(Group g, BubbleAnchors anchors, int px, int py, long now)
	{
		BubbleAnchors at = placed(g, anchors, now);
		List<BubbleFrame.Disc> discs = discs(g, geom(g, at, now), at, now, true);
		for (int i = 0; i < 3; i++)
		{
			BubbleFrame.Disc disc = discs.get(i);
			if (disc.painted() > 0 && Math.hypot(px - disc.x(), py - disc.y()) <= disc.painted() + HIT_SLOP)
				return true;
		}

		return false;
	}

	private void retireCurrent(long now)
	{
		Group g = current;
		if (g == null)
			return;

		if (g.phase != Phase.CLOSING)
			close(g, now);

		g.hits = BubbleHits.EMPTY;
		g.zones = Collections.emptyList();
		retiring.add(g);
		current = null;
	}

	private void close(Group g, long now)
	{
		g.phase = Phase.CLOSING;
		for (int i = 0; i < 3; i++)
			g.path[i] = BubbleMotion.retract(BubbleMotion.clamp(g.path[i].value(now)), now, i, mode);

		closeCluster(g, now, false);
		if (g.card != CardState.NONE)
			hideCard(g, now);

		resetTimers(g);
	}

	private void reverse(Group g, long now)
	{
		g.phase = Phase.OPENING;
		for (int i = 0; i < 3; i++)
			g.path[i] = BubbleMotion.appear(BubbleMotion.clamp(g.path[i].value(now)), now, i, mode);

		resetTimers(g);
	}

	private static void resetTimers(Group g)
	{
		if (g == null)
			return;

		g.outsideSince = -1;
		g.jarHoverSince = -1;
		g.clusterOutsideSince = -1;
		g.infoHoverSince = -1;
		g.cardOutsideSince = -1;
	}

	private void settle(Group g, long now)
	{
		if (g.phase == Phase.OPENING && allDone(g.path, now))
			g.phase = Phase.OPEN;

		if (g.cluster == ClusterState.CLOSING && allDone(g.slots, now))
		{
			g.cluster = ClusterState.CLOSED;
			g.slots = new Tween[0];
		}
	}

	private static boolean allDone(Tween[] tweens, long now)
	{
		for (Tween tween : tweens)
		{
			if (!tween.done(now))
				return false;
		}

		return true;
	}

	private boolean gone(Group g, long now)
	{
		boolean clusterGone = g.cluster == ClusterState.CLOSED
			|| g.cluster == ClusterState.CLOSING && allDone(g.slots, now);
		return g.phase == Phase.CLOSING && allDone(g.path, now) && clusterGone && g.card == CardState.NONE
			&& g.cardTween.value(now) <= 0;
	}

	private void prune(long now)
	{
		retiring.removeIf(g -> gone(g, now));
		if (current != null && gone(current, now))
			current = null;
	}

	private boolean arcBlocked(BubbleAnchors anchors)
	{
		return blocked(BubbleLayout.main(anchors, viewport).bounds());
	}

	private boolean blocked(Rectangle2D box)
	{
		for (Rectangle blocker : blockers)
		{
			if (box.intersects(blocker))
				return true;
		}

		return false;
	}

	private Rectangle2D groupBounds(Group g, Geom geom)
	{
		Rectangle2D bounds = geom.arc.bounds();
		if (g.cluster == ClusterState.OPEN && geom.cluster != null)
			bounds = bounds.createUnion(geom.cluster.bounds());

		if (g.card != CardState.NONE && geom.card != null)
			bounds = bounds.createUnion(geom.card.bounds());

		return bounds;
	}

	private Geom geom(Group g, BubbleAnchors anchors, long now)
	{
		return geom(g, anchors, viewport, now);
	}

	private Geom geom(Group g, BubbleAnchors anchors, Rectangle area, long now)
	{
		BubbleLayout.Arc arc = BubbleLayout.main(anchors, area);
		BubbleLayout.ClusterPlan cluster = null;
		List<JarSuggest.JarChoice> choices = g.offer == null ? Collections.emptyList() : clusterChoices(g.offer);
		if (g.cluster != ClusterState.CLOSED && !choices.isEmpty())
		{
			List<Point2D.Double> mains = new ArrayList<>();
			for (int i = 0; i < 3; i++)
				mains.add(arc.rest(i));

			cluster = BubbleLayout.cluster(arc.x(0), arc.y(0), choices.size(), mains,
				arc.radius() * BubbleLayout.GROUP_GROWTH, area, blockers);
		}

		BubbleLayout.CardPlan card = null;
		if (g.card != CardState.NONE || g.cardTween.value(now) > 0)
		{
			card = BubbleLayout.card(arc.bounds(), cardSize.width, cardSize.height, g.cardSide, area, blockers);
			g.cardSide = card.side();
		}

		return new Geom(arc, cluster, card);
	}

	private static List<JarSuggest.JarChoice> clusterChoices(BubbleOffer offer)
	{
		List<JarSuggest.JarChoice> out = new ArrayList<>();
		JarSuggest.JarChoice shown = null;
		for (JarSuggest.JarChoice choice : offer.owned())
		{
			if (choice.jar() == offer.jar())
				shown = choice;
		}

		if (shown != null)
			out.add(shown);

		JarSuggest.Split split = JarSuggest.split(offer.owned());
		List<JarSuggest.JarChoice> rest = new ArrayList<>(split.suggested());
		rest.addAll(split.others());
		for (JarSuggest.JarChoice choice : rest)
		{
			if (choice != shown && out.size() < BubbleLayout.CLUSTER_SLOTS)
				out.add(choice);
		}

		return out;
	}

	private static boolean ownsJar(BubbleOffer offer, JarType jar)
	{
		for (JarSuggest.JarChoice choice : offer.owned())
		{
			if (choice.jar() == jar)
				return true;
		}

		return false;
	}

	private static int countOf(BubbleOffer offer, JarType jar)
	{
		for (JarSuggest.JarChoice choice : offer.owned())
		{
			if (choice.jar() == jar)
				return choice.owned();
		}

		return 0;
	}

	private List<BubbleFrame.Disc> discs(Group g, Geom geom, BubbleAnchors a, long now, boolean live)
	{
		List<BubbleFrame.Disc> discs = new ArrayList<>();
		BubbleOffer offer = g.offer;
		Point2D.Double origin = new Point2D.Double(a.originX(), a.originY());
		double r = geom.arc.radius();
		for (int i = 0; i < 3; i++)
		{
			double t = g.path[i].value(now);
			Point2D.Double p = BubbleMotion.position(origin, geom.arc.rest(i), BubbleMotion.bend(i, r), t, mode);
			double h = g.hover[i].value(now);
			double scale = BubbleMotion.scale(t, mode) * BubbleMotion.hoverScale(h, mode);
			boolean jarBubble = i == 0;
			JarType jar = jarBubble ? offer.jar() : null;
			int count = jarBubble ? countOf(offer, jar) : 0;
			boolean disabled = jarBubble ? jar == null : i == 1 && !offer.battleReady();
			double pulse = jarBubble && offer.knockedOut() ? BubbleMotion.pulse(now, mode) : 0.0;
			discs.add(new BubbleFrame.Disc(MAIN_KINDS[i], p.x, p.y - BubbleMotion.hoverLift(h, mode), r, scale,
				BubbleMotion.alpha(t, mode), h, jar, count, disabled, false, pulse, live && t >= CLICKABLE_AT));
		}

		if (geom.cluster == null)
			return discs;

		List<JarSuggest.JarChoice> choices = clusterChoices(offer);
		Point2D.Double from = geom.arc.rest(0);
		double rs = BubbleLayout.CLUSTER_RADIUS;
		for (int j = 0; j < geom.cluster.slots().size() && j < g.slots.length && j < choices.size(); j++)
		{
			JarSuggest.JarChoice choice = choices.get(j);
			double t = g.slots[j].value(now);
			Point2D.Double p = BubbleMotion.position(from, geom.cluster.slots().get(j), -1.4 * rs, t, mode);
			double h = g.hoverOf(choice.jar()).value(now);
			double scale = BubbleMotion.scale(t, mode) * BubbleMotion.hoverScale(h, mode);
			discs.add(new BubbleFrame.Disc(BubbleKind.CLUSTER_JAR, p.x, p.y - BubbleMotion.hoverLift(h, mode), rs,
				scale, BubbleMotion.alpha(t, mode), h, choice.jar(), choice.owned(), false,
				choice.jar() == offer.jar(), 0.0, live && g.cluster == ClusterState.OPEN && t >= CLICKABLE_AT));
		}

		return discs;
	}

	private BubbleFrame.Card cardOf(Group g, Geom geom, long now, boolean live)
	{
		if (geom.card == null)
			return null;

		double t = g.cardTween.value(now);
		if (t <= 0)
			return null;

		boolean clickable = live && g.card != CardState.NONE && t >= CLICKABLE_AT;
		return new BubbleFrame.Card(geom.card.bounds(), g.offer.card(), g.card == CardState.PINNED,
			BubbleMotion.cardScale(t, mode), BubbleMotion.clamp(t), geom.card.side(), clickable);
	}

	private static boolean over(BubbleFrame.Disc disc, double px, double py)
	{
		return disc.clickable() && Math.hypot(px - disc.x(), py - disc.y()) <= disc.painted() + HIT_SLOP;
	}

	private void updateHover(Group g, List<BubbleFrame.Disc> discs, double px, double py, long now)
	{
		for (int i = 0; i < 3; i++)
			g.hover[i].set(over(discs.get(i), px, py) ? 1 : 0, now, mode);

		for (int i = 3; i < discs.size(); i++)
		{
			BubbleFrame.Disc disc = discs.get(i);
			g.hoverOf(disc.jar()).set(over(disc, px, py) ? 1 : 0, now, mode);
		}
	}

	private static Rectangle2D.Double around(BubbleFrame.Disc disc)
	{
		return BubbleLayout.grow(new Rectangle2D.Double(disc.x(), disc.y(), 0, 0), disc.painted() + HIT_SLOP);
	}

	private List<Rectangle2D> zones(Group g, Geom geom, List<BubbleFrame.Disc> discs, long now)
	{
		List<Rectangle2D> zones = new ArrayList<>();
		for (int i = 0; i < 3; i++)
			zones.add(around(discs.get(i)));

		zones.add(BubbleLayout.corridor(geom.arc, placed(g, g.anchors, now).originY()));
		if (g.cluster == ClusterState.OPEN && geom.cluster != null)
			zones.add(clusterZone(discs, geom));

		if (g.card != CardState.NONE && geom.card != null)
			zones.add(cardZone(discs, geom));

		return zones;
	}

	private static Rectangle2D.Double clusterZone(List<BubbleFrame.Disc> discs, Geom geom)
	{
		return BubbleLayout.union(around(discs.get(0)), BubbleLayout.grow(geom.cluster.bounds(), HIT_SLOP));
	}

	private static Rectangle2D.Double cardZone(List<BubbleFrame.Disc> discs, Geom geom)
	{
		return BubbleLayout.union(around(discs.get(2)), BubbleLayout.grow(geom.card.bounds(), HIT_SLOP));
	}

	private static boolean inZones(List<Rectangle2D> zones, double x, double y)
	{
		for (Rectangle2D zone : zones)
		{
			if (zone.contains(x, y))
				return true;
		}

		return false;
	}

	private void refresh(Group g, BubbleAnchors anchors, long now)
	{
		settle(g, now);
		BubbleAnchors at = placed(g, anchors, now);
		Geom geom = geom(g, at, now);
		boolean live = g.phase != Phase.CLOSING;
		List<BubbleFrame.Disc> discs = discs(g, geom, at, now, live);
		g.zones = live ? zones(g, geom, discs, now) : Collections.emptyList();
		boolean modal = live && (g.cluster == ClusterState.OPEN || g.card == CardState.PINNED);
		g.hits = new BubbleFrame(discs, cardOf(g, geom, now, live), g.generation, modal).hits();
	}

	/**
	 * Gives where the creature counts as being for the bubbles. While the pointer is on a bubble they stay where they
	 * are even if the creature walks; once it has been off every bubble for {@link #HOLD_MS} they glide to the
	 * creature.
	 */
	private static BubbleAnchors placed(Group g, BubbleAnchors live, long now)
	{
		if (live == null || g.held == null)
			return live;

		if (g.glide == null)
			return g.held;

		return BubbleAnchors.between(g.held, live, g.glide.value(now));
	}

	/**
	 * Holds the bubbles still while the pointer is on any of them and lets them glide back after it has been away for
	 * {@link #HOLD_MS}. If the pointer is no longer near the creature by then, the bubbles go back instead.
	 *
	 * @return whether the group was closed
	 */
	private boolean holdLogic(Group g, List<BubbleFrame.Disc> discs, BubbleAnchors live, boolean near, double px,
		double py, long now)
	{
		boolean onBubble = false;
		for (BubbleFrame.Disc disc : discs)
		{
			if (over(disc, px, py))
				onBubble = true;
		}

		if (onBubble)
		{
			g.held = placed(g, live, now);
			g.glide = null;
			g.holdOffSince = -1;
			return false;
		}

		if (g.held == null)
			return false;

		if (g.glide != null)
		{
			if (g.glide.done(now))
			{
				g.held = null;
				g.glide = null;
				g.holdOffSince = -1;
			}

			return false;
		}

		if (g.holdOffSince < 0)
			g.holdOffSince = now;

		if (now - g.holdOffSince < HOLD_MS)
			return false;

		if (!near)
		{
			close(g, now);
			return true;
		}

		g.glide = Tween.of(0, 1, now, mode.moves() ? BubbleMotion.HOLD_GLIDE_MS : 0, Easing.IN_OUT_SINE);
		return false;
	}

	private void clusterLogic(Group g, List<BubbleFrame.Disc> discs, Geom geom, double px, double py, long now)
	{
		boolean overJar = over(discs.get(0), px, py);
		if (!overJar)
		{
			g.jarHoverSince = -1;
			g.armed = true;
		}

		boolean owns = !g.offer.owned().isEmpty();
		if (g.cluster == ClusterState.OPEN)
		{
			if (!owns || g.slots.length != clusterChoices(g.offer).size())
			{
				closeCluster(g, now, false);
				return;
			}

			if (geom.cluster == null || clusterZone(discs, geom).contains(px, py))
				g.clusterOutsideSince = -1;
			else if (g.clusterOutsideSince < 0)
				g.clusterOutsideSince = now;

			if (g.clusterOutsideSince >= 0 && now - g.clusterOutsideSince >= CLUSTER_GRACE_MS)
				closeCluster(g, now, false);

			return;
		}

		if (!overJar || !g.armed || !owns)
		{
			g.jarHoverSince = -1;
			return;
		}

		if (g.jarHoverSince < 0)
			g.jarHoverSince = now;

		if (now - g.jarHoverSince >= CLUSTER_HOVER_MS)
			openCluster(g, now);
	}

	private void cardLogic(Group g, List<BubbleFrame.Disc> discs, Geom geom, double px, double py, long now)
	{
		boolean overInfo = over(discs.get(2), px, py);
		if (g.card == CardState.NONE)
		{
			if (!overInfo || g.cluster == ClusterState.OPEN)
			{
				g.infoHoverSince = -1;
				return;
			}

			if (g.infoHoverSince < 0)
				g.infoHoverSince = now;

			if (now - g.infoHoverSince >= CARD_HOVER_MS)
				showCard(g, CardState.PEEK, now);

			return;
		}

		if (g.card != CardState.PEEK)
			return;

		if (geom.card == null || cardZone(discs, geom).contains(px, py))
			g.cardOutsideSince = -1;
		else if (g.cardOutsideSince < 0)
			g.cardOutsideSince = now;

		if (g.cardOutsideSince >= 0 && now - g.cardOutsideSince >= CARD_HIDE_MS)
			hideCard(g, now);
	}

	private void openCluster(Group g, long now)
	{
		List<JarSuggest.JarChoice> choices = clusterChoices(g.offer);
		if (choices.isEmpty() || g.anchors == null)
			return;

		if (g.card != CardState.NONE)
			hideCard(g, now);

		Tween[] before = g.slots;
		g.cluster = ClusterState.OPEN;
		g.slots = new Tween[choices.size()];
		g.clusterOutsideSince = -1;
		g.jarHoverSince = -1;
		BubbleLayout.ClusterPlan plan = geom(g, placed(g, g.anchors, now), now).cluster;
		g.ranks = new int[plan.slots().size()];
		for (int j = 0; j < g.ranks.length; j++)
			g.ranks[j] = j;

		for (int j = 0; j < g.slots.length; j++)
		{
			double from = before.length == g.slots.length ? BubbleMotion.clamp(before[j].value(now)) : 0.0;
			g.slots[j] = BubbleMotion.clusterAppear(from, now, g.ranks[j], mode);
		}
	}

	private void closeCluster(Group g, long now, boolean disarm)
	{
		if (disarm)
			g.armed = false;

		if (g.cluster != ClusterState.OPEN)
			return;

		g.cluster = ClusterState.CLOSING;
		for (int j = 0; j < g.slots.length; j++)
		{
			double from = BubbleMotion.clamp(g.slots[j].value(now));
			g.slots[j] = BubbleMotion.clusterRetract(from, now, g.ranks[j], mode);
		}

		g.clusterOutsideSince = -1;
	}

	private void showCard(Group g, CardState state, long now)
	{
		if (state == CardState.PINNED)
			closeCluster(g, now, false);

		boolean wasHidden = g.card == CardState.NONE;
		g.card = state;
		g.cardOutsideSince = -1;
		if (wasHidden)
			g.cardTween = BubbleMotion.cardShow(BubbleMotion.clamp(g.cardTween.value(now)), now, mode);
	}

	private void hideCard(Group g, long now)
	{
		g.card = CardState.NONE;
		g.cardOutsideSince = -1;
		g.cardTween = BubbleMotion.cardHide(BubbleMotion.clamp(g.cardTween.value(now)), now, mode);
	}

	private void togglePin(Group g, long now)
	{
		if (g.card == CardState.PINNED)
		{
			g.card = CardState.PEEK;
			g.cardOutsideSince = -1;
			return;
		}

		showCard(g, CardState.PINNED, now);
	}
}
