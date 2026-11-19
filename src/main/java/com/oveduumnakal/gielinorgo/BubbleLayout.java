/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Where the action bubbles go, as pure geometry: the resting arc above a creature's head, the jar cluster, the card,
 * the edge clamp and the fallback shape for how near the mouse must be. Everything is a function of its inputs. Holds
 * no RuneLite types.
 */
final class BubbleLayout
{
	/** The radius of a main bubble. */
	static final int MAIN_RADIUS = 15;

	/** The radius of the arc the three main bubbles sit on. */
	static final int ARC_RADIUS = 80;

	/** The angle between neighbouring main bubbles on the arc, in degrees. */
	static final int ARC_STEP_DEG = 28;

	/** The gap between the tag line and the bottom of the resting bubbles. */
	static final int TAG_CLEARANCE = 18;

	/** How far inside the viewport everything is kept. */
	static final int EDGE_INSET = 4;

	/** The radius of a jar in the cluster: the same as a main bubble. */
	static final int CLUSTER_RADIUS = MAIN_RADIUS;

	/** The distance between neighbouring jar centres in the cluster: the same as between the main bubbles. */
	static final double CLUSTER_SPACING = Math.hypot(ARC_RADIUS * Math.sin(Math.toRadians(ARC_STEP_DEG)),
		ARC_RADIUS * (1.0 - Math.cos(Math.toRadians(ARC_STEP_DEG))));

	/** How many slots the cluster pattern has; there are 17 jar types. */
	static final int CLUSTER_SLOTS = 19;

	/** The least clear space between a cluster jar and a main bubble. */
	static final int CLUSTER_CLEARANCE = 2;

	/** The gap between the card and the bubbles. */
	static final int CARD_GAP = 8;

	/** How much bigger than its radius the group bounds count a bubble, for hover growth. */
	static final double GROUP_GROWTH = 1.15;

	/** The padding the group bounds add around each bubble. */
	static final double GROUP_PAD = 3.0;

	/** How much of the way up the fallback ellipse is, against its width. */
	static final double REACH_SQUASH = 0.6;

	/** How many candidate slots the honeycomb offers, so a blocked slot can be skipped and the next one used. */
	private static final int CLUSTER_CANDIDATES = 48;

	/** The three main bubbles at rest: index 0 Catch (left), 1 Battle (middle), 2 Info (right). Immutable. */
	static final class Arc
	{
		private final double[] x;
		private final double[] y;
		private final double radius;
		private final Rectangle2D bounds;

		private Arc(double[] x, double[] y, double radius, Rectangle2D bounds)
		{
			this.x = x;
			this.y = y;
			this.radius = radius;
			this.bounds = bounds;
		}

		/**
		 * @param index 0, 1 or 2
		 * @return the resting centre x
		 */
		double x(int index)
		{
			return x[index];
		}

		/**
		 * @param index 0, 1 or 2
		 * @return the resting centre y
		 */
		double y(int index)
		{
			return y[index];
		}

		/**
		 * @param index 0, 1 or 2
		 * @return the resting centre as a new point
		 */
		Point2D.Double rest(int index)
		{
			return new Point2D.Double(x[index], y[index]);
		}

		/** @return the radius of a main bubble */
		double radius()
		{
			return radius;
		}

		/** @return a new rectangle around the three bubbles, each counted at its hover size plus the padding */
		Rectangle2D.Double bounds()
		{
			return new Rectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
		}
	}

	/** The jar cluster placed: its slots and where it went. Immutable. */
	static final class ClusterPlan
	{
		private final double centreX;
		private final double centreY;
		private final List<Point2D.Double> slots;
		private final Rectangle2D bounds;

		private ClusterPlan(double centreX, double centreY, List<Point2D.Double> slots, Rectangle2D bounds)
		{
			this.centreX = centreX;
			this.centreY = centreY;
			this.slots = Collections.unmodifiableList(slots);
			this.bounds = bounds;
		}

		/** @return the x of the middle of the pattern, which is the jar bubble's own x */
		double centreX()
		{
			return centreX;
		}

		/** @return the y of the middle of the pattern, which is the jar bubble's own y */
		double centreY()
		{
			return centreY;
		}

		/** @return the centres of the slots, in fill order: the one nearest the jar bubble first, then outward */
		List<Point2D.Double> slots()
		{
			return slots;
		}

		/** @return a new rectangle around every slot's disc */
		Rectangle2D.Double bounds()
		{
			return new Rectangle2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
		}
	}

	/** The card placed: the side and the rectangle. Immutable. */
	static final class CardPlan
	{
		private final BubbleFrame.Side side;
		private final Rectangle bounds;

		private CardPlan(BubbleFrame.Side side, Rectangle bounds)
		{
			this.side = side;
			this.bounds = bounds;
		}

		/** @return the side of the bubbles the card is on */
		BubbleFrame.Side side()
		{
			return side;
		}

		/** @return a new rectangle with the card's place */
		Rectangle bounds()
		{
			return new Rectangle(bounds);
		}
	}

	private BubbleLayout()
	{
	}

	/**
	 * @param viewport the viewport
	 * @return the viewport shrunk by {@link #EDGE_INSET} on every side
	 */
	static Rectangle2D.Double inset(Rectangle viewport)
	{
		return new Rectangle2D.Double(viewport.x + EDGE_INSET, viewport.y + EDGE_INSET,
			viewport.width - 2.0 * EDGE_INSET, viewport.height - 2.0 * EDGE_INSET);
	}

	/**
	 * Lays out the three main bubbles on their arc above a creature's head. The middle one is highest; the whole
	 * group slides as one into the viewport when it would leave it. The anchors are not moved.
	 *
	 * @param anchors where the creature is
	 * @param viewport the viewport; the group is kept {@link #EDGE_INSET} inside it
	 * @return the arc
	 */
	static Arc main(BubbleAnchors anchors, Rectangle viewport)
	{
		double r = MAIN_RADIUS;
		double step = Math.toRadians(ARC_STEP_DEG);
		double dx = ARC_RADIUS * Math.sin(step);
		double dy = ARC_RADIUS * (1.0 - Math.cos(step));
		double middleY = anchors.headY() - TAG_CLEARANCE - r - dy;
		double[] xs = {anchors.headX() - dx, anchors.headX(), anchors.headX() + dx};
		double[] ys = {middleY + dy, middleY, middleY + dy};
		double grown = r * GROUP_GROWTH + GROUP_PAD;
		Rectangle2D bounds = null;
		for (int i = 0; i < 3; i++)
		{
			Rectangle2D disc = new Rectangle2D.Double(xs[i] - grown, ys[i] - grown, 2 * grown, 2 * grown);
			bounds = bounds == null ? disc : bounds.createUnion(disc);
		}

		double[] shift = shiftInto(bounds, inset(viewport));
		for (int i = 0; i < 3; i++)
		{
			xs[i] += shift[0];
			ys[i] += shift[1];
		}

		bounds = new Rectangle2D.Double(bounds.getX() + shift[0], bounds.getY() + shift[1], bounds.getWidth(),
			bounds.getHeight());
		return new Arc(xs, ys, r, bounds);
	}

	private static double[] shiftInto(Rectangle2D box, Rectangle2D area)
	{
		double tx = 0;
		double ty = 0;
		if (box.getX() < area.getX())
			tx = area.getX() - box.getX();
		else if (box.getMaxX() > area.getMaxX())
			tx = area.getMaxX() - box.getMaxX();

		if (box.getY() < area.getY())
			ty = area.getY() - box.getY();
		else if (box.getMaxY() > area.getMaxY())
			ty = area.getMaxY() - box.getMaxY();

		return new double[] {tx, ty};
	}

	/**
	 * Gives the corridor between the arc and the creature: the arc's width, from the top of the arc down to the
	 * creature's middle.
	 *
	 * @param arc the arc
	 * @param originY the canvas y of the creature's middle
	 * @return the rectangle; it is at least as tall as the arc
	 */
	static Rectangle2D.Double corridor(Arc arc, double originY)
	{
		Rectangle2D.Double box = arc.bounds();
		double bottom = Math.max(box.getMaxY(), originY);
		return new Rectangle2D.Double(box.x, box.y, box.width, bottom - box.y);
	}

	/**
	 * @param box a rectangle
	 * @param by how much to grow it on every side
	 * @return a new, grown rectangle
	 */
	static Rectangle2D.Double grow(Rectangle2D box, double by)
	{
		return new Rectangle2D.Double(box.getX() - by, box.getY() - by, box.getWidth() + 2 * by,
			box.getHeight() + 2 * by);
	}

	/**
	 * @param a a rectangle
	 * @param b another
	 * @return a new rectangle around both
	 */
	static Rectangle2D.Double union(Rectangle2D a, Rectangle2D b)
	{
		Rectangle2D both = a.createUnion(b);
		return new Rectangle2D.Double(both.getX(), both.getY(), both.getWidth(), both.getHeight());
	}

	/**
	 * Gives the shape that counts as near a creature when the polygon of tiles around it is not available.
	 *
	 * @param originX the canvas x of the creature's middle
	 * @param originY the canvas y of the creature's middle
	 * @param footprintTiles how many tiles wide the creature is
	 * @param nearTiles how many tiles beyond its edge still count as near
	 * @param pixelsPerTile how many canvas pixels one tile is wide here
	 * @return an ellipse centred on the creature, wider than tall
	 */
	static Ellipse2D.Double reachFallback(double originX, double originY, double footprintTiles, double nearTiles,
		double pixelsPerTile)
	{
		double rx = (nearTiles + footprintTiles / 2.0) * pixelsPerTile;
		double ry = rx * REACH_SQUASH;
		return new Ellipse2D.Double(originX - rx, originY - ry, 2 * rx, 2 * ry);
	}

	/**
	 * Gives the offsets of the cluster's slots from the jar bubble, in fill order. The slots are the points of a
	 * honeycomb with one spacing between neighbouring centres, which is the gap the main bubbles have, kept to the left
	 * of the bubble. They are ordered by distance from the bubble, then by nearness to the bubble's own row, then upper
	 * before lower: 9 o'clock first, then the two beside it at 11 and 7, and so on outward. Each slot touches an
	 * earlier one or the bubble.
	 *
	 * @param count how many slots, at most {@link #CLUSTER_CANDIDATES}; fewer than 0 counts as 0
	 * @return the offsets, as new points
	 */
	static List<Point2D.Double> clusterOffsets(int count)
	{
		double s = CLUSTER_SPACING;
		List<Point2D.Double> all = new ArrayList<>();
		for (int row = -8; row <= 8; row++)
		{
			for (int col = -10; col <= 2; col++)
			{
				double x = col * s + row * s / 2.0;
				double y = row * s * Math.sqrt(3.0) / 2.0;
				if (x <= -s / 2.0 + 1e-6 && Math.hypot(x, y) >= s - 1e-6)
					all.add(new Point2D.Double(x, y));
			}
		}

		all.sort((a, b) ->
		{
			int far = Long.compare(Math.round(Math.hypot(a.x, a.y) * 1000), Math.round(Math.hypot(b.x, b.y) * 1000));
			int level = Double.compare(Math.round(Math.abs(a.y) * 1000), Math.round(Math.abs(b.y) * 1000));
			return far != 0 ? far : level != 0 ? level : Double.compare(a.y, b.y);
		});
		return new ArrayList<>(all.subList(0, Math.max(0, Math.min(count, Math.min(CLUSTER_CANDIDATES, all.size())))));
	}

	/**
	 * Places the cluster to the left of the Catch bubble. It walks the honeycomb slots in fill order and takes each
	 * one whose jar is inside the inset viewport, clears every main bubble and misses every blocker, until it has
	 * enough. A slot that does not fit is skipped, so the next one takes its place. When too few slots fit, the skipped
	 * ones are used in their fill order, slid into the viewport, and may overlap.
	 *
	 * @param jarX the Catch bubble's resting x
	 * @param jarY the Catch bubble's resting y
	 * @param count how many jars the cluster holds, 1 to {@link #CLUSTER_SLOTS}
	 * @param mains the centres of the three main bubbles
	 * @param mainReach how far a main bubble reaches, hover growth included
	 * @param viewport the viewport
	 * @param blockers rectangles the cluster must not touch
	 * @return the plan
	 */
	static ClusterPlan cluster(double jarX, double jarY, int count, List<Point2D.Double> mains, double mainReach,
		Rectangle viewport, List<Rectangle> blockers)
	{
		int wanted = Math.max(0, Math.min(count, CLUSTER_SLOTS));
		Rectangle2D.Double area = inset(viewport);
		List<Point2D.Double> fitting = new ArrayList<>();
		List<Point2D.Double> skipped = new ArrayList<>();
		for (Point2D.Double offset : clusterOffsets(CLUSTER_CANDIDATES))
		{
			Point2D.Double slot = new Point2D.Double(jarX + offset.x, jarY + offset.y);
			if (fitting.size() < wanted && slotFits(slot, mains, mainReach, area, blockers))
				fitting.add(slot);
			else
				skipped.add(slot);
		}

		List<Point2D.Double> slots = new ArrayList<>(fitting);
		for (Point2D.Double slot : skipped)
		{
			if (slots.size() >= wanted)
				break;

			double rs = CLUSTER_RADIUS;
			slots.add(new Point2D.Double(Math.max(area.getX() + rs, Math.min(slot.x, area.getMaxX() - rs)),
				Math.max(area.getY() + rs, Math.min(slot.y, area.getMaxY() - rs))));
		}

		return new ClusterPlan(jarX, jarY, slots, discBounds(slots));
	}

	private static boolean slotFits(Point2D.Double slot, List<Point2D.Double> mains, double mainReach,
		Rectangle2D area, List<Rectangle> blockers)
	{
		double rs = CLUSTER_RADIUS;
		if (slot.x - rs < area.getX() || slot.x + rs > area.getMaxX() || slot.y - rs < area.getY()
			|| slot.y + rs > area.getMaxY())
			return false;

		double apart = rs + mainReach + CLUSTER_CLEARANCE;
		for (Point2D.Double main : mains)
		{
			if (Math.hypot(slot.x - main.x, slot.y - main.y) < apart)
				return false;
		}

		Rectangle2D disc = new Rectangle2D.Double(slot.x - rs, slot.y - rs, 2 * rs, 2 * rs);
		for (Rectangle blocker : blockers)
		{
			if (disc.intersects(blocker))
				return false;
		}

		return true;
	}

	private static Rectangle2D discBounds(List<Point2D.Double> slots)
	{
		double rs = CLUSTER_RADIUS;
		Rectangle2D bounds = null;
		for (Point2D.Double slot : slots)
		{
			Rectangle2D disc = new Rectangle2D.Double(slot.x - rs, slot.y - rs, 2 * rs, 2 * rs);
			bounds = bounds == null ? disc : bounds.createUnion(disc);
		}

		return bounds == null ? new Rectangle2D.Double() : bounds;
	}

	/**
	 * Places the card beside the bubbles. It tries right, then left, then above, the remembered side first, and takes
	 * the first that is inside the inset viewport and misses every blocker; when none fits it takes the right side
	 * pushed into the viewport.
	 *
	 * @param group the bounds of the main bubbles at rest
	 * @param width the card's width
	 * @param height the card's height
	 * @param remembered the side used last, or null
	 * @param viewport the viewport
	 * @param blockers rectangles the card must not touch
	 * @return the plan
	 */
	static CardPlan card(Rectangle2D group, int width, int height, BubbleFrame.Side remembered, Rectangle viewport,
		List<Rectangle> blockers)
	{
		Rectangle2D.Double area = inset(viewport);
		List<BubbleFrame.Side> order = new ArrayList<>();
		if (remembered != null)
			order.add(remembered);

		for (BubbleFrame.Side side : BubbleFrame.Side.values())
		{
			if (side != remembered)
				order.add(side);
		}

		for (BubbleFrame.Side side : order)
		{
			Rectangle box = place(side, group, width, height, area);
			if (fits(box, area, blockers))
				return new CardPlan(side, box);
		}

		Rectangle right = place(BubbleFrame.Side.RIGHT, group, width, height, area);
		double[] shift = shiftInto(right, area);
		right.translate((int) Math.round(shift[0]), (int) Math.round(shift[1]));
		return new CardPlan(BubbleFrame.Side.RIGHT, right);
	}

	private static Rectangle place(BubbleFrame.Side side, Rectangle2D group, int width, int height,
		Rectangle2D area)
	{
		double x;
		double y;
		if (side == BubbleFrame.Side.RIGHT)
		{
			x = group.getMaxX() + CARD_GAP;
			y = group.getY();
		}
		else if (side == BubbleFrame.Side.LEFT)
		{
			x = group.getX() - CARD_GAP - width;
			y = group.getY();
		}
		else
		{
			x = group.getCenterX() - width / 2.0;
			y = group.getY() - CARD_GAP - height;
		}

		if (side != BubbleFrame.Side.ABOVE)
			y = Math.max(area.getY(), Math.min(y, area.getMaxY() - height));

		return new Rectangle((int) Math.round(x), (int) Math.round(y), width, height);
	}

	private static boolean fits(Rectangle box, Rectangle2D area, List<Rectangle> blockers)
	{
		if (box.x < area.getX() || box.y < area.getY() || box.getMaxX() > area.getMaxX()
			|| box.getMaxY() > area.getMaxY())
			return false;

		for (Rectangle blocker : blockers)
		{
			if (box.intersects(blocker))
				return false;
		}

		return true;
	}
}
