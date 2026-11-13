/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPC;
import net.runelite.api.RuneLiteObject;

/**
 * A creature drawn as an object in the scene: the lead creature beside the player, or the body of a wild
 * creature. It loops a base animation (standing or walking) and can act out one move of a battle
 * on top of it: attack, get hit, or go down. A move with no animation of its own is faked with a short lunge or
 * recoil, or by laying the model on the ground. A melee attacker can also step up to its target, strike and step
 * back ({@link #meleeTurn(double, Act)}, timed by {@link TurnMotion}). Every method must be called on the game thread.
 */
final class CreatureBody extends RuneLiteObject
{
	private static final Logger log = LoggerFactory.getLogger(CreatureBody.class);

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
	private double size = 1.0;
	private int level = OpacityFade.SOLID;
	private Mode mode = Mode.LOOP;
	private int base = AnimationTable.NONE;
	private int playing = AnimationTable.NONE;
	private int lungeAge = BattleTimeline.LUNGE_TICKS;
	private double lungeTiles;
	private int turnAge = TurnMotion.TURN_TICKS;
	private double turnDistance;
	private Act turnAct = Act.ATTACK;
	private boolean turnWalking;
	private boolean turnStruck;
	private int restBase = AnimationTable.NONE;
	private int liveIdle = AnimationTable.NONE;
	private int liveWalk = AnimationTable.NONE;
	private double settleFrom;
	private int settleAge = TurnMotion.SETTLE_TICKS;
	private int drawnHeight;

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
	 * Builds a creature as it is drawn beside the player: the way a boss pet is a small copy of its boss, at
	 * {@link FollowerService#SHOWN_SIZE} of the size that fits ({@link FollowerService#fit(int, double, double)}).
	 * Each NPC id of the species is tried in turn, as the portraits do, until one has a model.
	 *
	 * @param client the game client
	 * @param animations the animation ids of each NPC
	 * @param species the species
	 * @param gilded whether to paint it gold
	 * @return the body, standing still with its idle animation and not yet in the scene, or null when no NPC id of
	 *     the species can be drawn
	 */
	static CreatureBody petSized(Client client, AnimationTable animations, Species species, boolean gilded)
	{
		return petSized(client, animations, species, 0, gilded);
	}

	/**
	 * Builds a creature as it is drawn beside the player, as the NPC variant it was caught as. That NPC id is tried
	 * first, then the others of the species in their order, so a variant that cannot be drawn still gives a body.
	 *
	 * @param client the game client
	 * @param animations the animation ids of each NPC
	 * @param species the species
	 * @param preferred the NPC id to draw first, or 0 for the species' first; see {@link Species#npcIdFor(int)}
	 * @param gilded whether to paint it gold
	 * @return the body, or null when no NPC id of the species can be drawn
	 */
	static CreatureBody petSized(Client client, AnimationTable animations, Species species, int preferred,
		boolean gilded)
	{
		for (int npcId : PortraitService.tryOrder(species, preferred))
		{
			try
			{
				CreatureBody body = petSized(client, animations, npcId, gilded);
				if (body != null)
					return body;
			}
			catch (RuntimeException | AssertionError e)
			{
				log.debug("Gielinor Go: could not build a creature from NPC {}", npcId, e);
			}
		}

		return null;
	}

	private static CreatureBody petSized(Client client, AnimationTable animations, int npcId, boolean gilded)
	{
		NpcModels.Parts parts = NpcModels.load(client, npcId);
		if (parts == null)
			return null;

		ModelData data = parts.data();
		if (gilded)
			FollowerService.gild(data);

		double factor = FollowerService.SHOWN_SIZE * FollowerService.fit(parts.size(),
			FollowerService.reachOf(data) * parts.widthScale() / NpcModels.NORMAL_SCALE,
			FollowerService.heightOf(data) * parts.heightScale() / NpcModels.NORMAL_SCALE);
		int widthScale = Math.max(1, (int) Math.round(parts.widthScale() * factor));
		int heightScale = Math.max(1, (int) Math.round(parts.heightScale() * factor));
		CreatureBody body = create(client, animations, parts, widthScale, heightScale);
		if (body == null)
			return null;

		body.drawnHeight = (int) Math.round(FollowerService.heightOf(data) * heightScale / NpcModels.NORMAL_SCALE);
		body.setBase(animations.idle(parts.npcId()));
		return body;
	}

	/**
	 * Remembers the stand and walk animations of the live NPC this body is a copy of, for an NPC id the animation
	 * table has no row for. A failed read leaves them as they were.
	 *
	 * @param live the live NPC
	 */
	void useLive(NPC live)
	{
		try
		{
			liveIdle = live.getIdlePoseAnimation();
			liveWalk = live.getWalkAnimation();
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not read the animations of NPC {}", npcId, e);
		}
	}

	/**
	 * Remembers stand and walk animations that were read from a live NPC earlier, for a body that is built again
	 * after the NPC is gone.
	 *
	 * @param idle the stand animation id, or {@link AnimationTable#NONE}
	 * @param walk the walk animation id, or {@link AnimationTable#NONE}
	 */
	void useAnimations(int idle, int walk)
	{
		liveIdle = idle;
		liveWalk = walk;
	}

	/** @return the stand animation the live NPC had, as {@link #useLive} read it, or {@link AnimationTable#NONE} */
	int liveIdle()
	{
		return liveIdle;
	}

	/** @return the walk animation the live NPC had, as {@link #useLive} read it, or {@link AnimationTable#NONE} */
	int liveWalk()
	{
		return liveWalk;
	}

	/** @return the stand animation: the table's, or the live NPC's when the table has no row for the NPC id */
	int standAnimation()
	{
		return animations.idle(npcId, liveIdle);
	}

	/** @return the walk animation: the table's, or the live NPC's when the table has no row for the NPC id */
	int walkAnimation()
	{
		return animations.walk(npcId, liveWalk, liveIdle);
	}

	/**
	 * @return how large the body is drawn compared with the real monster, as its width scale over
	 *     {@link NpcModels#NORMAL_SCALE}: the shrink it is shown at times its species' size
	 */
	double drawScale()
	{
		return (double) widthScale / NpcModels.NORMAL_SCALE;
	}

	/** @return how tall the body is drawn, in scene units, when it was made by {@link #petSized}; else 0 */
	int drawnHeight()
	{
		return drawnHeight;
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

	/**
	 * Sets how big the body is drawn compared with its own size. The scaling is done after the pose each frame, as
	 * shrinking before posing makes the model come apart. A body that stands with no animation cannot be resized,
	 * as its plain model is shared; it keeps its size until size 0 takes it out of sight ({@link #visible()}).
	 *
	 * @param factor 1 for its own size down to 0 for nothing; values outside that are clamped
	 */
	void setSize(double factor)
	{
		size = Math.max(0.0, Math.min(1.0, factor));
	}

	/** @return whether any of the body is drawn: false while it is fully faded out or has shrunk to nothing */
	boolean visible()
	{
		return level != OpacityFade.HIDDEN && size > 0.0;
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
		if (turnAge < TurnMotion.TURN_TICKS)
		{
			restBase = animationId;
			if (!turnWalking)
				applyBase(animationId);

			return;
		}

		applyBase(animationId);
	}

	private void applyBase(int animationId)
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
		cancelTurn();
		switch (act)
		{
			case ATTACK:
			case SPECIAL:
				if (!attackPose(act))
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

	/**
	 * Steps up to the target, strikes and steps back, as {@link TurnMotion} times it: it walks along the way it
	 * faces, its attack animation starts at {@link TurnMotion#strikeTick()}, and it is back on its spot when
	 * {@link TurnMotion#TURN_TICKS} client ticks are up. It keeps facing the target all the way, so on the way back
	 * it backs away. The walk is shown with the creature's walking animation, and a creature with no attack
	 * animation simply walks in and out. {@link #push()} gives the distance while the turn runs.
	 *
	 * @param distance how far it steps, in tiles
	 * @param act the attack: {@link Act#ATTACK}, or {@link Act#SPECIAL} for its second attack
	 */
	void meleeTurn(double distance, Act act)
	{
		cancelTurn();
		lying = false;
		lungeAge = BattleTimeline.LUNGE_TICKS;
		restBase = base;
		turnDistance = distance;
		turnAct = act;
		turnStruck = false;
		turnAge = 0;
	}

	/** @return whether a melee turn is running: the body is stepping up, striking or stepping back */
	boolean turning()
	{
		return turnAge < TurnMotion.TURN_TICKS;
	}

	/**
	 * Cuts a melee turn short, for a catch thrown mid-turn: the body does not freeze where it is, it eases back to
	 * its own spot over {@link TurnMotion#SETTLE_TICKS} client ticks. Does nothing when no turn is running.
	 */
	void endTurn()
	{
		if (!turning())
			return;

		double from = push();
		cancelTurn();
		settleFrom = from;
		settleAge = 0;
	}

	/** Forgets the melee turn at once, and the walk that came with it. */
	private void cancelTurn()
	{
		if (turnWalking)
			base = restBase;

		turnWalking = false;
		turnAge = TurnMotion.TURN_TICKS;
		settleAge = TurnMotion.SETTLE_TICKS;
	}

	private boolean attackPose(Act act)
	{
		if (act == Act.SPECIAL && pose(animations.secondAttack(npcId), Mode.ONCE))
			return true;

		return pose(animations.attack(npcId), Mode.ONCE);
	}

	/**
	 * Moves a melee turn on by one client tick: switches between walking and standing, and starts the strike.
	 */
	private void advanceTurn()
	{
		turnAge++;
		boolean walking = turnAge < TurnMotion.TURN_TICKS && TurnMotion.walking(turnAge);
		if (walking != turnWalking)
		{
			turnWalking = walking;
			applyBase(walking ? walkAnimation() : restBase);
		}

		if (!turnStruck && turnAge >= TurnMotion.strikeTick())
		{
			turnStruck = true;
			attackPose(turnAct);
		}
	}

	/** Gets up after being defeated, or stops whatever move is playing, and goes back to the base animation. */
	void recover()
	{
		cancelTurn();
		lying = false;
		lungeAge = BattleTimeline.LUNGE_TICKS;
		mode = Mode.LOOP;
		playing = base;
		controller.setAnimation(animation(base));
	}

	/** Moves the lunge, recoil or melee turn on by one client tick. */
	void clientTick()
	{
		if (lungeAge < BattleTimeline.LUNGE_TICKS)
			lungeAge++;

		if (settleAge < TurnMotion.SETTLE_TICKS)
			settleAge++;

		if (turning())
			advanceTurn();
	}

	/**
	 * @return how far the body is pushed along the way it faces right now, in tiles; negative is backward. While a
	 *     melee turn runs it is the step in and out, after a cut-off turn the way back to its spot, else the lunge
	 */
	double push()
	{
		if (turning())
			return TurnMotion.offset(turnAge, turnDistance);

		if (settleAge < TurnMotion.SETTLE_TICKS)
			return TurnMotion.settle(settleFrom, settleAge);

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

		int wide = Math.max(1, (int) Math.round(widthScale * size));
		int tall = Math.max(1, (int) Math.round(heightScale * size));
		if (wide != NpcModels.NORMAL_SCALE || tall != NpcModels.NORMAL_SCALE)
			posed.scale(wide, tall, wide);

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

		OpacityFade.fadeAll(faces, OpacityFade.opacityOf(wanted));
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
