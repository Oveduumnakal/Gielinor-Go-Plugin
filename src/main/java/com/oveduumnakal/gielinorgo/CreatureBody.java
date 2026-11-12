/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.RuneLiteObject;

/**
 * A creature drawn as an object in the scene: the lead creature beside the player, or the stand-in for a wild
 * creature during a battle. It loops a base animation (standing or walking) and can act out one move of a battle
 * on top of it: attack, get hit, or go down. A move with no animation of its own is faked with a short lunge or
 * recoil, or by laying the model on the ground. Every method must be called on the game thread.
 */
final class CreatureBody extends RuneLiteObject
{
	/** One move of a battle. */
	enum Act
	{
		/** Strike at the other creature. */
		ATTACK,

		/** Strike with the second attack, such as a dragon's fire breath; the plain attack when there is none. */
		SPECIAL,

		/** React to being struck. */
		HIT,

		/** Go down and stay down until {@link CreatureBody#recover()}. */
		DEFEATED
	}

	/** What the controller does when an animation ends. */
	private enum Mode
	{
		/** Start over from the first frame. */
		LOOP,

		/** Go back to the base animation. */
		ONCE,

		/** Stay on the last frame. */
		HOLD
	}

	private final Client client;
	private final AnimationTable animations;
	private final int npcId;
	private final ModelData source;
	private final Model[] stills = new Model[OpacityFade.SOLID + 1];
	private final Model[] posables = new Model[OpacityFade.SOLID + 1];
	private final Model[] tipped = new Model[OpacityFade.SOLID + 1];
	private final int widthScale;
	private final int heightScale;
	private final AnimationController controller;
	private boolean lying;
	private int level = OpacityFade.SOLID;
	private Mode mode = Mode.LOOP;
	private int base = AnimationTable.NONE;
	private int playing = AnimationTable.NONE;
	private int lungeAge = BattleTimeline.LUNGE_TICKS;
	private double lungeTiles;

	private CreatureBody(Client client, AnimationTable animations, int npcId, ModelData source, Model still,
		Model posable, int widthScale, int heightScale)
	{
		super(client);
		this.client = client;
		this.animations = animations;
		this.npcId = npcId;
		this.source = source;
		this.stills[OpacityFade.SOLID] = still;
		this.posables[OpacityFade.SOLID] = posable;
		this.widthScale = widthScale;
		this.heightScale = heightScale;
		this.controller = new AnimationController(client, (Animation) null);
		this.controller.setOnFinished(this::finished);
	}

	/**
	 * Builds the object from an NPC's parts. The object is not in the scene until it is made active.
	 *
	 * @param client the game client
	 * @param animations the animation ids of each NPC
	 * @param parts the NPC's merged and recoloured model; this keeps it, so the caller must not change it again
	 * @param widthScale how wide to draw it, where {@link NpcModels#NORMAL_SCALE} is as modelled
	 * @param heightScale how tall to draw it, where {@link NpcModels#NORMAL_SCALE} is as modelled
	 * @return the body, or null when the model cannot be lit
	 */
	static CreatureBody create(Client client, AnimationTable animations, NpcModels.Parts parts, int widthScale,
		int heightScale)
	{
		ModelData data = parts.data();
		Model still = data.shallowCopy()
			.cloneVertices()
			.scale(widthScale, heightScale, widthScale)
			.light();
		Model posable = data.light();
		if (still == null || posable == null)
			return null;

		CreatureBody body = new CreatureBody(client, animations, parts.npcId(), data, still, posable, widthScale,
			heightScale);
		body.setModel(posable);
		body.setAnimationController(body.controller);
		return body;
	}

	/**
	 * Sets how much of the body can be seen. Opacity 1 draws the plain model; anything between draws a private
	 * see-through copy, built the first time that level is shown, and 0 draws nothing ({@link #visible()}). The
	 * shared model is never changed.
	 *
	 * @param opacity from 0 (unseen) to 1 (solid)
	 * @throws IllegalStateException when the model for that opacity cannot be built
	 */
	void setOpacity(double opacity)
	{
		int wanted = OpacityFade.levelOf(opacity);
		if (wanted == level)
			return;

		if (wanted != OpacityFade.HIDDEN)
			build(wanted);

		level = wanted;
		if (wanted != OpacityFade.HIDDEN)
			setModel(posables[wanted]);
	}

	/**
	 * Builds what a level still lacks, and nothing that is already built. The model that stands still is not built
	 * here; {@link #stillNow()} makes it the first time it is drawn.
	 *
	 * @param wanted the level, below solid or solid
	 */
	private void build(int wanted)
	{
		boolean needTip = lying && tipped[wanted] == null;
		if (posables[wanted] != null && !needTip)
			return;

		ModelData data = faded(wanted);
		if (posables[wanted] == null)
			posables[wanted] = data.light();

		if (posables[wanted] == null)
			throw new IllegalStateException("no model for level " + wanted);

		if (needTip)
			tipped[wanted] = lieDown(data);
	}

	/** @return whether any of the body is drawn: false while it is fully faded out */
	boolean visible()
	{
		return level != OpacityFade.HIDDEN;
	}

	/** @return the id of the NPC this body shows */
	int npcId()
	{
		return npcId;
	}

	/**
	 * Sets the animation that plays over and over when nothing else does. One that already plays is left alone, so
	 * it does not restart every tick.
	 *
	 * @param animationId the animation, or {@link AnimationTable#NONE} to stand still
	 */
	void setBase(int animationId)
	{
		base = animationId;
		if (mode != Mode.LOOP)
			return;

		boolean running = animationId == AnimationTable.NONE || controller.getAnimation() != null;
		if (animationId == playing && running)
			return;

		playing = animationId;
		controller.setAnimation(animation(animationId));
	}

	/**
	 * Acts out one move of a battle.
	 *
	 * @param act the move
	 */
	void act(Act act)
	{
		lying = false;
		lungeAge = BattleTimeline.LUNGE_TICKS;
		switch (act)
		{
			case ATTACK:
				if (!pose(animations.attack(npcId), Mode.ONCE))
					lunge(BattleTimeline.LUNGE_TILES);

				break;

			case SPECIAL:
				if (!pose(animations.secondAttack(npcId), Mode.ONCE) && !pose(animations.attack(npcId), Mode.ONCE))
					lunge(BattleTimeline.LUNGE_TILES);

				break;

			case HIT:
				if (!pose(animations.defend(npcId), Mode.ONCE))
					lunge(-BattleTimeline.RECOIL_TILES);

				break;

			default:
				if (!pose(animations.death(npcId), Mode.HOLD))
					layDown();

				break;
		}
	}

	/** Gets up after being defeated, or stops whatever move is playing, and goes back to the base animation. */
	void recover()
	{
		lying = false;
		lungeAge = BattleTimeline.LUNGE_TICKS;
		mode = Mode.LOOP;
		playing = base;
		controller.setAnimation(animation(base));
	}

	/** Moves the lunge or recoil on by one client tick. */
	void clientTick()
	{
		if (lungeAge < BattleTimeline.LUNGE_TICKS)
			lungeAge++;
	}

	/** @return how far the body is pushed along the way it faces right now, in tiles; negative is backward */
	double push()
	{
		return BattleTimeline.lunge(lungeAge, lungeTiles);
	}

	/** @return whether the body is laid on the ground because its creature has no death animation */
	boolean lyingDown()
	{
		return lying;
	}

	/**
	 * Gives the model to hit-test a click against. It is a model that already exists and is never changed here: the
	 * body tipped onto the ground while it lies down, else the body standing still. An animation that is playing is
	 * not followed, so a death animation that lays the model down is covered by the tile it stands on instead.
	 *
	 * @return the model, or null when none is built
	 */
	Model hitModel()
	{
		if (lying && tipped[level] != null)
			return tipped[level];

		return stillNow();
	}

	@Override
	public Model getModel()
	{
		if (lying)
			return tipped[level] != null ? tipped[level] : stillNow();

		if (controller.getAnimation() == null)
			return stillNow();

		Model posed = super.getModel();
		if (posed == null || posed == getBaseModel())
			return stillNow();

		if (widthScale != NpcModels.NORMAL_SCALE || heightScale != NpcModels.NORMAL_SCALE)
			posed.scale(widthScale, heightScale, widthScale);

		return posed;
	}

	private void layDown()
	{
		lying = true;
		if (level != OpacityFade.HIDDEN && tipped[level] == null)
			tipped[level] = lieDown(faded(level));
	}

	private boolean pose(int animationId, Mode how)
	{
		Animation animation = animationId == AnimationTable.NONE ? null : client.loadAnimation(animationId);
		if (animation == null)
			return false;

		mode = how;
		playing = animationId;
		controller.setAnimation(animation);
		return true;
	}

	private void lunge(double tiles)
	{
		lungeTiles = tiles;
		lungeAge = 0;
	}

	/**
	 * Called by the controller when an animation reaches its end. Frames are numbered from 0, so the last is one
	 * less than their count.
	 *
	 * @param finished the controller
	 */
	private void finished(AnimationController finished)
	{
		if (mode == Mode.HOLD)
		{
			hold(finished);
			return;
		}

		if (mode == Mode.ONCE)
		{
			mode = Mode.LOOP;
			playing = base;
			finished.setAnimation(animation(base));
			return;
		}

		restart(finished);
	}

	/**
	 * Starts a controller's animation over. Reset zeroes both the frame and the elapsed time, so the game's tick loop
	 * cannot spin on a leftover.
	 *
	 * @param controller the controller whose animation just ended
	 */
	static void restart(AnimationController controller)
	{
		controller.reset();
	}

	/**
	 * Keeps a controller on the last frame of its animation. Used as the controller's end-of-animation callback,
	 * it is called again each time the animation would end, so the creature stays down. The controller is reset
	 * first, which zeroes its elapsed time: the game's tick loop runs while the elapsed time exceeds the length of
	 * the current frame, so a last frame of length 0 would otherwise spin forever.
	 *
	 * @param controller the controller whose animation just ended
	 */
	static void hold(AnimationController controller)
	{
		Animation animation = controller.getAnimation();
		if (animation == null)
			return;

		controller.reset();
		controller.setFrame(Math.max(0, lastFrame(animation)));
	}

	private static int lastFrame(Animation animation)
	{
		return animation.isMayaAnim() ? animation.getDuration() - 1 : animation.getFrameLengths().length - 1;
	}

	private Animation animation(int animationId)
	{
		return animationId == AnimationTable.NONE ? null : client.loadAnimation(animationId);
	}

	private Model stillNow()
	{
		if (stills[level] == null && level != OpacityFade.HIDDEN)
			stills[level] = stillAt(level);

		return stills[level] != null ? stills[level] : stills[OpacityFade.SOLID];
	}

	/**
	 * @param wanted the level
	 * @return the model that stands still at that level, scaled here; null if it cannot be built
	 */
	private Model stillAt(int wanted)
	{
		try
		{
			return faded(wanted).shallowCopy()
				.cloneVertices()
				.scale(widthScale, heightScale, widthScale)
				.light();
		}
		catch (RuntimeException | AssertionError e)
		{
			return null;
		}
	}

	/**
	 * @param wanted the level
	 * @return the source model, or for a see-through level a private copy of it with its faces faded
	 */
	private ModelData faded(int wanted)
	{
		if (wanted == OpacityFade.SOLID)
			return source;

		ModelData copy = source.shallowCopy();
		copy.cloneTransparencies(true);
		byte[] faces = copy.getFaceTransparencies();
		if (faces == null)
			throw new IllegalStateException("no transparencies for level " + wanted);

		GhostMirror.fadeAll(faces, OpacityFade.opacityOf(wanted));
		return copy;
	}

	/**
	 * @param data the model at the level being built
	 * @return the creature tipped onto the ground, made on a private copy of the model; null if it cannot be lit
	 */
	private Model lieDown(ModelData data)
	{
		ModelData copy = data.shallowCopy()
			.cloneVertices()
			.scale(widthScale, heightScale, widthScale);
		ModelTilt.lieDown(copy.getVerticesY(), copy.getVerticesZ(), copy.getVerticesCount());
		return copy.light();
	}
}
