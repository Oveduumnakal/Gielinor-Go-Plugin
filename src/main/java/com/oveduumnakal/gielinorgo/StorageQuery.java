/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * What the Storage page shows and in what order: a sort and a list of filter criteria. Immutable; every edit returns
 * a new query. Criteria combine as AND across facets and OR inside a style, rarity, rating or gender facet, and each
 * one is either shown (only matching creatures stay) or hidden (matching creatures go). Holds no RuneLite types.
 */
final class StorageQuery
{
	/** The query that shows everything in the stored order. */
	static final StorageQuery NONE = new StorageQuery(Sort.BOX_ORDER, false, Collections.emptyList());

	/** The ways to order the list. */
	enum Sort
	{
		BOX_ORDER("Box order"),
		LEVEL("Level"),
		SPECIES("Species"),
		RARITY("Rarity"),
		DATE_CAUGHT("Date caught"),
		NICKNAME("Nickname");

		private final String label;

		Sort(String label)
		{
			this.label = label;
		}

		/** @return the name shown to the player */
		String label()
		{
			return label;
		}

		/** @return whether the first press of this sort puts the high end first */
		boolean defaultDescending()
		{
			return this == LEVEL || this == RARITY || this == DATE_CAUGHT;
		}

		/**
		 * @param descending which way the sort runs
		 * @return the words for that direction, such as {@code high to low}, or an empty text for Box order
		 */
		String directionWords(boolean descending)
		{
			switch (this)
			{
				case LEVEL:
					return descending ? "high to low" : "low to high";
				case SPECIES:
				case NICKNAME:
					return descending ? "Z to A" : "A to Z";
				case RARITY:
					return descending ? "rarest first" : "commonest first";
				case DATE_CAUGHT:
					return descending ? "newest first" : "oldest first";
				default:
					return "";
			}
		}
	}

	/** The things a creature can be filtered by. */
	enum Facet
	{
		LEVEL_OVER,
		LEVEL_UNDER,
		STYLE,
		RARITY,
		RATING,
		GENDER,
		GILDED,
		NICKNAMED
	}

	/** What a filter leaf of the menu stands at. */
	enum Stance
	{
		OFF,
		SHOWN,
		HIDDEN
	}

	/** One filter: a facet, a value and whether matching creatures are shown or hidden. */
	static final class Criterion
	{
		private final Facet facet;
		private final int value;
		private final boolean hidden;

		/**
		 * @param facet what is filtered
		 * @param value a level for the two level facets, else the ordinal of the enum; 0 for gilded and nicknamed
		 * @param hidden whether matching creatures are hidden instead of shown
		 */
		Criterion(Facet facet, int value, boolean hidden)
		{
			this.facet = facet;
			this.value = value;
			this.hidden = hidden;
		}

		/** @return what is filtered */
		Facet facet()
		{
			return facet;
		}

		/** @return the level or enum ordinal */
		int value()
		{
			return value;
		}

		/** @return whether matching creatures are hidden */
		boolean hidden()
		{
			return hidden;
		}

		/**
		 * @param on whether matching creatures are hidden
		 * @return a copy that shows or hides
		 */
		Criterion withHidden(boolean on)
		{
			return new Criterion(facet, value, on);
		}

		/**
		 * @param view a creature
		 * @return whether it has this criterion's trait, whichever way it shows or hides
		 */
		boolean matches(CreatureView view)
		{
			switch (facet)
			{
				case LEVEL_OVER:
					return view.level() > value;
				case LEVEL_UNDER:
					return view.level() < value;
				case STYLE:
					return view.type().ordinal() == value;
				case RARITY:
					return view.rarity().ordinal() == value;
				case RATING:
					return view.rating().ordinal() == value;
				case GENDER:
					return view.gender() != null && view.gender().ordinal() == value;
				case GILDED:
					return view.gilded();
				default:
					return view.nickname() != null;
			}
		}

		/** @return the chip's words, such as {@code Level over 20} or {@code Not Crush} */
		String label()
		{
			switch (facet)
			{
				case LEVEL_OVER:
					return hidden ? "Level " + value + " or under" : "Level over " + value;
				case LEVEL_UNDER:
					return hidden ? "Level " + value + " or over" : "Level under " + value;
				case GILDED:
					return hidden ? "Not gilded" : "Gilded";
				case NICKNAMED:
					return hidden ? "Not nicknamed" : "Nicknamed";
				default:
					return (hidden ? "Not " : "") + valueName(facet, value);
			}
		}

		@Override
		public boolean equals(Object other)
		{
			if (!(other instanceof Criterion))
				return false;

			Criterion that = (Criterion) other;
			return facet == that.facet && value == that.value && hidden == that.hidden;
		}

		@Override
		public int hashCode()
		{
			return facet.hashCode() * 31 * 31 + value * 31 + (hidden ? 1 : 0);
		}
	}

	private final Sort sort;
	private final boolean descending;
	private final List<Criterion> criteria;

	private StorageQuery(Sort sort, boolean descending, List<Criterion> criteria)
	{
		this.sort = sort;
		this.descending = descending;
		this.criteria = Collections.unmodifiableList(new ArrayList<>(criteria));
	}

	/** @return how the list is ordered */
	Sort sort()
	{
		return sort;
	}

	/** @return whether the sort puts the high end first; meaningless for Box order */
	boolean descending()
	{
		return descending;
	}

	/** @return the criteria, in the order they were added */
	List<Criterion> criteria()
	{
		return criteria;
	}

	/** @return whether any criterion is set */
	boolean active()
	{
		return !criteria.isEmpty();
	}

	/**
	 * @param facet the name of a facet value
	 * @param value its value
	 * @return the name of the value as the player knows it, such as {@code Crush} or {@code Strong}
	 */
	static String valueName(Facet facet, int value)
	{
		switch (facet)
		{
			case STYLE:
				return CombatType.values()[value].label();
			case RARITY:
				return Rarity.values()[value].label();
			case RATING:
				return StatRolls.Rating.values()[value].label();
			case GENDER:
				return value == Gender.MALE.ordinal() ? "Male" : "Female";
			case GILDED:
				return "Gilded";
			case NICKNAMED:
				return "Nicknamed";
			default:
				return String.valueOf(value);
		}
	}

	/**
	 * Picks the sort. Picking the sort that is on flips its direction; another sort starts at its default direction.
	 *
	 * @param next the sort to use
	 * @return the new query
	 */
	StorageQuery withSort(Sort next)
	{
		if (next == sort)
			return next == Sort.BOX_ORDER ? this : new StorageQuery(sort, !descending, criteria);

		return new StorageQuery(next, next.defaultDescending(), criteria);
	}

	/**
	 * Steps one filter leaf: absent becomes shown, shown becomes hidden, hidden goes away. A level facet holds one
	 * value, so a new value replaces the old one and starts shown.
	 *
	 * @param facet the facet
	 * @param value the level or enum ordinal; 0 for gilded and nicknamed
	 * @return the new query
	 */
	StorageQuery cycled(Facet facet, int value)
	{
		List<Criterion> next = new ArrayList<>(criteria);
		boolean level = facet == Facet.LEVEL_OVER || facet == Facet.LEVEL_UNDER;
		for (int i = 0; i < next.size(); i++)
		{
			Criterion each = next.get(i);
			if (each.facet() != facet || !level && each.value() != value)
				continue;

			if (level && each.value() != value)
				next.set(i, new Criterion(facet, value, false));
			else if (each.hidden())
				next.remove(i);
			else
				next.set(i, each.withHidden(true));

			return new StorageQuery(sort, descending, next);
		}

		next.add(new Criterion(facet, value, false));
		return new StorageQuery(sort, descending, next);
	}

	/**
	 * @param facet the facet
	 * @param value the level or enum ordinal
	 * @return where that leaf stands now
	 */
	Stance stance(Facet facet, int value)
	{
		for (Criterion each : criteria)
		{
			if (each.facet() == facet && each.value() == value)
				return each.hidden() ? Stance.HIDDEN : Stance.SHOWN;
		}

		return Stance.OFF;
	}

	/**
	 * Words a filter leaf with its own state, so the menu shows what a press does next.
	 *
	 * @param facet the facet
	 * @param value the level or enum ordinal
	 * @param base the leaf's plain label
	 * @return the label, with {@code (only)} when shown and {@code (hidden)} when hidden
	 */
	String stateWords(Facet facet, int value, String base)
	{
		switch (stance(facet, value))
		{
			case SHOWN:
				return base + " (only)";
			case HIDDEN:
				return base + " (hidden)";
			default:
				return base;
		}
	}

	/**
	 * @param index a criterion's place in {@link #criteria()}
	 * @return the query without it; this query when the place does not exist
	 */
	StorageQuery without(int index)
	{
		if (index < 0 || index >= criteria.size())
			return this;

		List<Criterion> next = new ArrayList<>(criteria);
		next.remove(index);
		return new StorageQuery(sort, descending, next);
	}

	/**
	 * @param index a criterion's place in {@link #criteria()}
	 * @return the query with that criterion switched between shown and hidden; this query when it does not exist
	 */
	StorageQuery flipped(int index)
	{
		if (index < 0 || index >= criteria.size())
			return this;

		List<Criterion> next = new ArrayList<>(criteria);
		Criterion each = next.get(index);
		next.set(index, each.withHidden(!each.hidden()));
		return new StorageQuery(sort, descending, next);
	}

	/** @return the query without any criterion, keeping the sort */
	StorageQuery cleared()
	{
		return criteria.isEmpty() ? this : new StorageQuery(sort, descending, Collections.emptyList());
	}

	/**
	 * Tells whether a creature passes every criterion.
	 *
	 * @param view the creature
	 * @return whether it is shown
	 */
	boolean accepts(CreatureView view)
	{
		Map<Facet, Boolean> anyShown = new EnumMap<>(Facet.class);
		Map<Facet, Boolean> shownMatch = new EnumMap<>(Facet.class);
		for (Criterion each : criteria)
		{
			boolean match = each.matches(view);
			if (each.hidden())
			{
				if (match)
					return false;

				continue;
			}

			if (isAnd(each.facet()))
			{
				if (!match)
					return false;

				continue;
			}

			anyShown.put(each.facet(), Boolean.TRUE);
			if (match)
				shownMatch.put(each.facet(), Boolean.TRUE);
		}

		for (Facet facet : anyShown.keySet())
		{
			if (!shownMatch.containsKey(facet))
				return false;
		}

		return true;
	}

	private static boolean isAnd(Facet facet)
	{
		return facet == Facet.LEVEL_OVER || facet == Facet.LEVEL_UNDER || facet == Facet.GILDED
			|| facet == Facet.NICKNAMED;
	}

	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof StorageQuery))
			return false;

		StorageQuery that = (StorageQuery) other;
		return sort == that.sort && (sort == Sort.BOX_ORDER || descending == that.descending)
			&& criteria.equals(that.criteria);
	}

	@Override
	public int hashCode()
	{
		return sort.hashCode() * 31 + criteria.hashCode();
	}
}
