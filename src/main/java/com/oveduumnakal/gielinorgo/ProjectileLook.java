/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import net.runelite.api.gameval.SpotanimID;

/**
 * What a projectile of an acted-out battle looks like. Each look is one of the game's spot animations (the
 * travelling part of an attack), read from the game cache with the RuneLite cache library: its model id, animation
 * id, recolours and resize value are stored here as plain numbers, so nothing is looked up while playing. The
 * projectile itself is an object this plugin moves, never a game projectile, so other plugins do not see it.
 */
enum ProjectileLook
{
	/**
	 * A plain arrow, for ranged attackers. Spot animation {@code SpotanimID.IRON_ARROW_TRAVEL}: model 3136, no
	 * animation, two recolours (the shaft and the head).
	 */
	ARROW(SpotanimID.IRON_ARROW_TRAVEL, 3136, AnimationTable.NONE, 128, 1.0, 0, 0,
		new short[]{57, 61}, new short[]{33, 33}),

	/**
	 * A basic bolt, for magic attackers. Spot animation {@code SpotanimID.FIRESTRIKE_TRAVEL}: model 3081, animation
	 * 659, two recolours, see-through faces.
	 */
	SPELL(SpotanimID.FIRESTRIKE_TRAVEL, 3081, 659, 128, 1.0, 60, 60,
		new short[]{926, 947}, new short[]{960, 6080}),

	/**
	 * Wind Strike. Spot animation {@code SpotanimID.WINDSTRIKE_TRAVEL}: the bolt's model 3081 and animation 659 with
	 * three recolours.
	 */
	WIND_STRIKE(SpotanimID.WINDSTRIKE_TRAVEL, 3081, 659, 128, 1.0, 30, 30,
		new short[]{926, 947, 11200}, new short[]{-22417, 127, -21568}),

	/**
	 * Fire Blast. Spot animation {@code SpotanimID.FIREBLAST_TRAVEL}: model 3087, animation 663, no recolours.
	 */
	FIRE_BLAST(SpotanimID.FIREBLAST_TRAVEL, 3087, 663, 128, 1.0, 60, 60, new short[0], new short[0]),

	/**
	 * The earth wave, the nearest earth spell to Earth surge, which the constants of the game do not list. Spot
	 * animation {@code SpotanimID.EARTHWAVE_TRAVEL}: model 3115, animation 692, three recolours.
	 */
	EARTH_WAVE(SpotanimID.EARTHWAVE_TRAVEL, 3115, 692, 128, 1.0, 40, 40,
		new short[]{7104, 6080, 960}, new short[]{8092, 22464, 22430}),

	/**
	 * Dragon fire, for a dragon's breath. Spot animation {@code SpotanimID.FIREBREATH_TRAVEL}: model 3154, animation
	 * 373, no recolours. The model is two tiles wide, so it is drawn at two fifths.
	 */
	FIRE(SpotanimID.FIREBREATH_TRAVEL, 3154, 373, 128, 0.4, 0, 0, new short[0], new short[0]),

	/**
	 * A wyvern's icy breath. Spot animation {@code SpotanimID.WYVERN_SKELETON_TRAVEL_BREATH}: model 10361,
	 * animation 2992, no recolours.
	 */
	ICE(SpotanimID.WYVERN_SKELETON_TRAVEL_BREATH, 10361, 2992, 128, 1.0, 40, 0, new short[0], new short[0]);

	/** The resize value that means "as modelled". */
	static final int NORMAL_SIZE = 128;

	/** What the game adds to a model's ambient light before lighting a spot animation. */
	static final int BASE_AMBIENT = 64;

	/** What the game adds to a model's contrast before lighting a spot animation. */
	static final int BASE_CONTRAST = 850;

	private final int spotAnimId;
	private final int modelId;
	private final int animationId;
	private final int resize;
	private final double shown;
	private final int ambient;
	private final int contrast;
	private final short[] recolorFrom;
	private final short[] recolorTo;

	ProjectileLook(int spotAnimId, int modelId, int animationId, int resize, double shown, int ambient, int contrast,
		short[] recolorFrom, short[] recolorTo)
	{
		this.spotAnimId = spotAnimId;
		this.modelId = modelId;
		this.animationId = animationId;
		this.resize = resize;
		this.shown = shown;
		this.ambient = ambient;
		this.contrast = contrast;
		this.recolorFrom = recolorFrom;
		this.recolorTo = recolorTo;
	}

	/**
	 * Picks the look of an attack.
	 *
	 * @param style the style of the attack
	 * @param special whether it was the attacker's second kind, which reads as a breath
	 * @param icy whether a breath is icy, as a wyvern's is
	 * @return the look, or null for a melee attack or an unknown style, which has no projectile
	 */
	static ProjectileLook of(CombatType style, boolean special, boolean icy)
	{
		return of(style, special, icy, null);
	}

	/**
	 * Picks the look of an attack, a spell showing the projectile of its own move.
	 *
	 * @param style the style of the attack
	 * @param special whether it was the attacker's second kind, which reads as a breath
	 * @param icy whether a breath is icy, as a wyvern's is
	 * @param moveId the id of the move, or null when it is not known
	 * @return the look, or null for a melee attack or an unknown style, which has no projectile
	 */
	static ProjectileLook of(CombatType style, boolean special, boolean icy, String moveId)
	{
		if (style == null || style.melee())
			return null;

		if (special)
			return icy ? ICE : FIRE;

		if (style == CombatType.RANGED)
			return ARROW;

		return spellOf(moveId);
	}

	/**
	 * Gives the projectile of a spell.
	 *
	 * @param moveId the id of the move, or null
	 * @return the spell's own look, or the generic bolt {@link #SPELL} when the move has no match
	 */
	private static ProjectileLook spellOf(String moveId)
	{
		if (moveId == null)
			return SPELL;

		switch (moveId)
		{
			case "wind_strike":
				return WIND_STRIKE;

			case "fire_blast":
				return FIRE_BLAST;

			case "earth_surge":
				return EARTH_WAVE;

			default:
				return SPELL;
		}
	}

	/** @return the id of the game's spot animation this look was read from */
	int spotAnimId()
	{
		return spotAnimId;
	}

	/** @return the id of the model */
	int modelId()
	{
		return modelId;
	}

	/** @return the id of the animation the model plays, or {@link AnimationTable#NONE} */
	int animationId()
	{
		return animationId;
	}

	/** @return whether the model plays an animation */
	boolean animated()
	{
		return animationId != AnimationTable.NONE;
	}

	/** @return the colours to replace, in the game's packed form; same length as {@link #recolorTo()} */
	short[] recolorFrom()
	{
		return recolorFrom.clone();
	}

	/** @return the colours that replace them */
	short[] recolorTo()
	{
		return recolorTo.clone();
	}

	/** @return the ambient light value to light the model with */
	int ambient()
	{
		return BASE_AMBIENT + ambient;
	}

	/** @return the contrast value to light the model with */
	int contrast()
	{
		return BASE_CONTRAST + contrast;
	}

	/** @return the size to draw it at, where {@link #NORMAL_SIZE} is as modelled: the game's resize and our shrink */
	int size()
	{
		return sizeFor(1.0);
	}

	/**
	 * Gives the size to draw it at for a caster that is itself drawn smaller or larger than modelled.
	 *
	 * @param casterScale the caster's draw scale, where 1.0 is as modelled (a body's scale value divided by
	 *     {@link #NORMAL_SIZE})
	 * @return the size, where {@link #NORMAL_SIZE} is as modelled, never below 1
	 */
	int sizeFor(double casterScale)
	{
		return Math.max(1, (int) Math.round(resize * shown * casterScale));
	}
}
