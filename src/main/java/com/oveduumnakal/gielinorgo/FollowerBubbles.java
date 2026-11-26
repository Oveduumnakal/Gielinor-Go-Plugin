/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The bubbles over the player's own follower: Switch, Item and Run, with a cluster that opens from Switch or Item.
 * They come out when the pointer rests on the follower, stay while it is on them or on the follower, and go back a
 * moment after it leaves. While the pointer is on a bubble they hold still, so a walking follower does not carry
 * them away. Times are milliseconds and places are canvas pixels. Holds no RuneLite types.
 */
final class FollowerBubbles
{
	/** How long the pointer rests on the follower before the bubbles come out; short, so walking past opens none. */
	static final long OPEN_MS = 250;

	/** How long the pointer may stay away before the bubbles go back. */
	static final long CLOSE_MS = 400;

	/** How long the pointer rests on Switch or Item before its cluster opens. */
	static final long CLUSTER_MS = 450;

	/** The button number of a left press. */
	static final int LEFT = 1;

	/** The button number of a right press. */
	static final int RIGHT = 3;

	/** The distance between neighbouring cluster bubbles. */
	private static final int SPACING = 34;

	/** How many cluster bubbles go in a row. */
	private static final int PER_ROW = 5;

	/** What one bubble is. */
	enum Kind
	{
		/** Brings another creature out. */
		SWITCH,
		/** Uses an item. */
		ITEM,
		/** Runs from the battle. */
		RUN,
		/** One creature of a cluster. */
		MEMBER,
		/** One item of the item cluster. */
		POTION
	}

	/** One bubble where it is drawn now. */
	static final class Dot
	{
		private final Kind kind;
		private final double x;
		private final double y;
		private final boolean enabled;
		private final FollowerOffer.Member member;
		private final FollowerOffer.Item item;

		private Dot(Kind kind, double x, double y, boolean enabled, FollowerOffer.Member member,
			FollowerOffer.Item item)
		{
			this.kind = kind;
			this.x = x;
			this.y = y;
			this.enabled = enabled;
			this.member = member;
			this.item = item;
		}

		/** @return what it is */
		Kind kind()
		{
			return kind;
		}

		/** @return its centre across */
		double x()
		{
			return x;
		}

		/** @return its centre down */
		double y()
		{
			return y;
		}

		/** @return its radius */
		int radius()
		{
			return BubbleLayout.MAIN_RADIUS;
		}

		/** @return whether a left click does anything */
		boolean enabled()
		{
			return enabled;
		}

		/** @return the creature of a member bubble, or null */
		FollowerOffer.Member member()
		{
			return member;
		}

		/** @return the item of an item bubble, or null */
		FollowerOffer.Item item()
		{
			return item;
		}

		/**
		 * @param px a point across
		 * @param py a point down
		 * @return whether the point is on the bubble or a little outside it
		 */
		boolean contains(double px, double py)
		{
			return Math.hypot(px - x, py - y) <= radius() + BubbleHits.HIT_SLOP;
		}
	}

	/** What a press asks the plugin to do. */
	static final class Action
	{
		/** The kinds of request. */
		enum Type
		{
			/** Nothing; the press may still have changed the bubbles. */
			NONE,
			/** Bring a creature out. */
			SWITCH,
			/** Use an item on a creature. */
			ITEM,
			/** Run from the battle. */
			RUN
		}

		/** Asks for nothing. */
		static final Action NONE = new Action(Type.NONE, -1, null);

		private final Type type;
		private final int index;
		private final ItemType item;

		private Action(Type type, int index, ItemType item)
		{
			this.type = type;
			this.index = index;
			this.item = item;
		}

		/** @return what to do */
		Type type()
		{
			return type;
		}

		/** @return the creature's place, or -1 */
		int index()
		{
			return index;
		}

		/** @return the item, or null */
		ItemType item()
		{
			return item;
		}
	}

	/** Which cluster is open. */
	private enum Cluster
	{
		NONE,
		MEMBERS,
		ITEMS,
		TARGETS
	}

	private FollowerOffer offer;
	private boolean open;
	private double anchorX;
	private double anchorY;
	private Cluster cluster = Cluster.NONE;
	private ItemType picked;
	private long overSince = -1;
	private long awaySince = -1;
	private long restSince = -1;
	private Kind resting;
	private Rectangle viewport = new Rectangle();

	/** @return whether the bubbles are out */
	boolean open()
	{
		return open;
	}

	/** Puts the bubbles away at once. */
	void hide()
	{
		open = false;
		cluster = Cluster.NONE;
		picked = null;
		overSince = -1;
		awaySince = -1;
		restSince = -1;
		resting = null;
		offer = null;
	}

	/**
	 * Moves the model on.
	 *
	 * @param now the time
	 * @param available what may be offered, or null to put the bubbles away
	 * @param head where the follower's head is on the canvas, as x and y
	 * @param onBody whether the pointer is on or very near the follower
	 * @param pointer the pointer as x and y
	 * @param view the game's viewport
	 */
	void tick(long now, FollowerOffer available, double[] head, boolean onBody, double[] pointer, Rectangle view)
	{
		if (available == null || !available.any() || head == null)
		{
			hide();
			return;
		}

		offer = available;
		viewport = view;
		Dot under = open ? at(pointer[0], pointer[1]) : null;
		if (!open)
		{
			anchorX = head[0];
			anchorY = head[1];
			overSince = onBody ? overSince < 0 ? now : overSince : -1;
			open = onBody && now - overSince >= OPEN_MS;
			awaySince = -1;
			return;
		}

		if (under == null)
		{
			anchorX = head[0];
			anchorY = head[1];
		}

		if (under != null || onBody || nearGroup(pointer[0], pointer[1]))
		{
			awaySince = -1;
		}
		else
		{
			awaySince = awaySince < 0 ? now : awaySince;
			if (now - awaySince >= CLOSE_MS)
			{
				hide();
				return;
			}
		}

		rest(now, under == null ? null : under.kind());
	}

	/** Opens a cluster once the pointer has rested on its bubble, and closes it when another main bubble is met. */
	private void rest(long now, Kind kind)
	{
		if (kind != resting)
		{
			resting = kind;
			restSince = now;
			if (kind == Kind.RUN || kind == Kind.SWITCH && cluster == Cluster.ITEMS
				|| kind == Kind.ITEM && cluster == Cluster.MEMBERS)
				cluster = Cluster.NONE;

			return;
		}

		if (now - restSince < CLUSTER_MS)
			return;

		if (kind == Kind.SWITCH && offer.switchEnabled())
			cluster = Cluster.MEMBERS;

		if (kind == Kind.ITEM && offer.live() && !offer.items().isEmpty() && cluster != Cluster.TARGETS)
			cluster = Cluster.ITEMS;
	}

	/**
	 * Gives every bubble where it is drawn now, the cluster's last so they are painted over the others.
	 *
	 * @return the bubbles; none while they are away
	 */
	List<Dot> dots()
	{
		if (!open || offer == null)
			return Collections.emptyList();

		List<Dot> dots = mains();
		dots.addAll(clusterDots());
		return shiftIntoView(dots);
	}

	private List<Dot> mains()
	{
		List<Kind> kinds = new ArrayList<>();
		if (offer.switchShown())
			kinds.add(Kind.SWITCH);

		if (offer.itemShown())
			kinds.add(Kind.ITEM);

		if (offer.runShown())
			kinds.add(Kind.RUN);

		List<Dot> dots = new ArrayList<>();
		double drop = BubbleLayout.ARC_RADIUS * (1 - Math.cos(Math.toRadians(BubbleLayout.ARC_STEP_DEG)));
		double top = anchorY - BubbleLayout.TAG_CLEARANCE - BubbleLayout.MAIN_RADIUS - drop;
		for (int i = 0; i < kinds.size(); i++)
		{
			double angle = Math.toRadians((i - (kinds.size() - 1) / 2.0) * BubbleLayout.ARC_STEP_DEG);
			double x = anchorX + BubbleLayout.ARC_RADIUS * Math.sin(angle);
			double y = top + BubbleLayout.ARC_RADIUS * (1 - Math.cos(angle));
			dots.add(new Dot(kinds.get(i), x, y, enabled(kinds.get(i)), null, null));
		}

		return dots;
	}

	private boolean enabled(Kind kind)
	{
		if (kind == Kind.SWITCH)
			return offer.switchEnabled();

		return kind == Kind.ITEM ? offer.itemEnabled() : offer.runEnabled();
	}

	private List<Dot> clusterDots()
	{
		List<Dot> dots = new ArrayList<>();
		if (cluster == Cluster.MEMBERS)
		{
			for (FollowerOffer.Member member : offer.others())
				add(dots, Kind.MEMBER, offer.switchEnabled(), member, null, offer.others().size());
		}
		else if (cluster == Cluster.ITEMS)
		{
			for (FollowerOffer.Item item : offer.items())
				add(dots, Kind.POTION, offer.live() && !item.targets().isEmpty(), null, item, offer.items().size());
		}
		else if (cluster == Cluster.TARGETS)
		{
			FollowerOffer.Item item = pickedItem();
			if (item == null)
				return dots;

			for (FollowerOffer.Member member : item.targets())
				add(dots, Kind.MEMBER, offer.live(), member, item, item.targets().size());
		}

		return dots;
	}

	/** Puts the next cluster bubble in rows above the main ones, each row centred. */
	private void add(List<Dot> dots, Kind kind, boolean enabled, FollowerOffer.Member member, FollowerOffer.Item item,
		int total)
	{
		int at = dots.size();
		int row = at / PER_ROW;
		int inRow = Math.min(PER_ROW, total - row * PER_ROW);
		double x = anchorX + (at % PER_ROW - (inRow - 1) / 2.0) * SPACING;
		double drop = BubbleLayout.ARC_RADIUS * (1 - Math.cos(Math.toRadians(BubbleLayout.ARC_STEP_DEG)));
		double y = anchorY - BubbleLayout.TAG_CLEARANCE - BubbleLayout.MAIN_RADIUS - drop - SPACING * (row + 1.1);
		dots.add(new Dot(kind, x, y, enabled, member, item));
	}

	/** Moves the whole group so that none of it hangs outside the viewport. */
	private List<Dot> shiftIntoView(List<Dot> dots)
	{
		if (dots.isEmpty() || viewport.width <= 0)
			return dots;

		double reach = BubbleLayout.MAIN_RADIUS + BubbleLayout.EDGE_INSET;
		double left = dots.stream()
				.mapToDouble(Dot::x)
				.min()
				.getAsDouble() - reach;
		double right = dots.stream()
				.mapToDouble(Dot::x)
				.max()
				.getAsDouble() + reach;
		double top = dots.stream()
				.mapToDouble(Dot::y)
				.min()
				.getAsDouble() - reach;
		double dx = Math.max(0, viewport.x - left) - Math.max(0, right - viewport.x - viewport.width);
		double dy = Math.max(0, viewport.y - top);
		if (dx == 0 && dy == 0)
			return dots;

		List<Dot> moved = new ArrayList<>();
		for (Dot dot : dots)
			moved.add(new Dot(dot.kind, dot.x + dx, dot.y + dy, dot.enabled, dot.member, dot.item));

		return moved;
	}

	/** @return whether a point is within the rectangle round all the bubbles, so crossing the gaps keeps them out */
	private boolean nearGroup(double px, double py)
	{
		List<Dot> dots = dots();
		double reach = BubbleLayout.MAIN_RADIUS * 2.0;
		return !dots.isEmpty()
			&& px >= dots.stream().mapToDouble(Dot::x).min().getAsDouble() - reach
			&& px <= dots.stream().mapToDouble(Dot::x).max().getAsDouble() + reach
			&& py >= dots.stream().mapToDouble(Dot::y).min().getAsDouble() - reach
			&& py <= dots.stream().mapToDouble(Dot::y).max().getAsDouble() + reach;
	}

	/**
	 * Finds the bubble under a point.
	 *
	 * @param x across
	 * @param y down
	 * @return the topmost bubble there, or null
	 */
	Dot at(double x, double y)
	{
		List<Dot> dots = dots();
		for (int i = dots.size() - 1; i >= 0; i--)
		{
			if (dots.get(i).contains(x, y))
				return dots.get(i);
		}

		return null;
	}

	/**
	 * Acts on a press.
	 *
	 * @param x across
	 * @param y down
	 * @param button {@link #LEFT} or {@link #RIGHT}
	 * @return what the plugin is to do
	 */
	Action press(double x, double y, int button)
	{
		Dot dot = at(x, y);
		if (dot == null)
			return Action.NONE;

		switch (dot.kind())
		{
			case SWITCH:
				return pressSwitch(button);
			case ITEM:
				return pressItem(button);
			case RUN:
				return button == LEFT && dot.enabled() ? new Action(Action.Type.RUN, -1, null) : Action.NONE;
			case POTION:
				return button == LEFT && dot.enabled() ? pick(dot.item()) : Action.NONE;
			default:
				return button == LEFT && dot.enabled() ? chose(dot) : Action.NONE;
		}
	}

	/** In a battle a left click brings in the next creature; otherwise, and on a right click, the list opens. */
	private Action pressSwitch(int button)
	{
		if (!offer.switchEnabled())
			return Action.NONE;

		if (button == LEFT && offer.inBattle())
			return new Action(Action.Type.SWITCH, offer.others().get(0).index(), null);

		cluster = Cluster.MEMBERS;
		return Action.NONE;
	}

	/** A left click uses the first item that would help; a right click opens the list. */
	private Action pressItem(int button)
	{
		if (button == RIGHT)
		{
			if (offer.live() && !offer.items().isEmpty())
				cluster = Cluster.ITEMS;

			return Action.NONE;
		}

		FollowerOffer.Item item = offer.live() ? offer.quickItem() : null;
		return item == null ? Action.NONE
			: new Action(Action.Type.ITEM, offer.quickTarget(item).index(), item.type());
	}

	/** An item picked from the list is used at once on the one creature it helps, or asks which of several. */
	private Action pick(FollowerOffer.Item item)
	{
		if (item.targets().size() == 1)
		{
			cluster = Cluster.NONE;
			return new Action(Action.Type.ITEM, item.targets().get(0).index(), item.type());
		}

		picked = item.type();
		cluster = Cluster.TARGETS;
		return Action.NONE;
	}

	private Action chose(Dot dot)
	{
		cluster = Cluster.NONE;
		if (dot.item() == null)
			return new Action(Action.Type.SWITCH, dot.member().index(), null);

		return new Action(Action.Type.ITEM, dot.member().index(), dot.item().type());
	}

	private FollowerOffer.Item pickedItem()
	{
		return offer.items()
				.stream()
				.filter(item -> item.type() == picked)
				.findFirst()
				.orElse(null);
	}

	/**
	 * Says what a bubble would do, for its tooltip.
	 *
	 * @param dot the bubble
	 * @return the words
	 */
	String tip(Dot dot)
	{
		boolean waiting = offer != null && !offer.live();
		switch (dot.kind())
		{
			case SWITCH:
				if (waiting)
					return "Switch: wait for your turn";

				if (!dot.enabled())
					return "No other creature to switch to";

				return offer.inBattle() ? "Switch to " + offer.others().get(0).name() : "Switch lead";

			case ITEM:
				if (waiting)
					return "Item: wait for your turn";

				FollowerOffer.Item item = offer.quickItem();
				return item == null ? "No item would help right now"
					: "Use " + item.type().label() + " on " + offer.quickTarget(item).name();

			case RUN:
				return runTip();

			case POTION:
				return dot.item().type().label() + " x" + dot.item().count()
					+ (dot.item().targets().isEmpty() ? ": would do nothing" : "");

			default:
				FollowerOffer.Member member = dot.member();
				return member.name() + " LV " + member.level() + ", " + Math.round(member.health() * 100)
					+ "% health";
		}
	}

	private String runTip()
	{
		if (!offer.live())
			return "Run: wait for your turn";

		return offer.runChance() >= 1.0 ? "Run away"
			: "Run: " + Math.round(offer.runChance() * 100) + "% chance to get away";
	}
}
