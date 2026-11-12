/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Renderable;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.AnimationID;
import net.runelite.client.callback.RenderCallback;

/**
 * What a catch looks like: the player makes a throwing motion, the creature reacts when the jar lands, and the
 * player reacts to the result. The real monster cannot be animated, so it is hidden and a copy acts in its place
 * ({@link TargetScript}): a caught creature shrinks to nothing, stays out of sight for five seconds and fades back
 * in; one that breaks free turns to the player and attacks; one that flees runs off fading out, and the real
 * monster fades back in. All of it is drawn on the player's own screen only. The monster in the real game is
 * untouched, nothing is sent to the game, and other players see nothing. Every method must be called on the game
 * thread; RuneLite calls {@link #addEntity(Renderable, boolean)} from it too.
 */
final class CatchEffects implements RenderCallback
{
	private static final Logger log = LoggerFactory.getLogger(CatchEffects.class);

	/** Client ticks, 20 ms each, a caught creature stays out of sight after landing: five seconds. */
	static final int HIDDEN_TICKS = 250;

	/** Client ticks a caught creature takes to fade back in. */
	static final int FADE_TICKS = 30;

	/** How many steps the fade is drawn in, from nearly see-through to nearly solid. */
	static final int FADE_STEPS = 6;

	/** {@link #shadeAt(int)} for a creature that is still out of sight. */
	static final int HIDDEN = -1;

	/** {@link #shadeAt(int)} for a creature that is still in sight because the jar has not landed yet. */
	static final int VISIBLE = -3;

	/** {@link #shadeAt(int)} for a creature whose effect is over. */
	static final int DONE = -2;

	/** The game's value for a face that cannot be seen at all; 0 is solid. */
	private static final int SEE_THROUGH = 255;

	/** The animation id that means the player is not doing anything. */
	private static final int NO_ANIMATION = -1;

	/** The one reaction that is waiting for its moment. */
	private static final class Reaction
	{
		private final int animation;
		private final WorldPoint tile;
		private int age;

		private Reaction(int animation, WorldPoint tile)
		{
			this.animation = animation;
			this.tile = tile;
		}
	}

	/** One creature a jar was thrown at, from the throw until the real monster is back in sight. */
	private static final class CatchAct
	{
		private final NPC npc;
		private final CatchOutcome outcome;
		/** Client ticks since the throw. */
		private int age;
		private CreatureBody copy;
		private boolean copyTried;
		private int plane;
		private int startX;
		private int startY;
		private int facing;
		private int wantFacing;
		private boolean turning;
		private double runX;
		private double runY;
		private Model[] shades;
		private RuneLiteObject ghost;

		private CatchAct(NPC npc, CatchOutcome outcome)
		{
			this.npc = npc;
			this.outcome = outcome;
		}
	}

	private static final int TILE = 128;

	private final Client client;
	private final AnimationTable animations;
	private final BooleanSupplier enabled;
	private final Map<Integer, CatchAct> acts = new HashMap<>();
	private WorldPoint thrownFrom;
	private Reaction reaction;

	/**
	 * Creates the effects with nothing playing.
	 *
	 * @param client the game client
	 * @param animations the animation ids of each NPC, for the copy of the creature
	 * @param enabled whether the "Catch effects" setting is on; switching it off ends every effect at once
	 */
	CatchEffects(Client client, AnimationTable animations, BooleanSupplier enabled)
	{
		this.client = client;
		this.animations = animations;
		this.enabled = enabled;
	}

	/**
	 * Makes the player's character throw, and remembers the tile it threw from. The motion is left out while the
	 * character is in the middle of something else, such as fishing or fighting, so a real animation is never cut
	 * short on screen.
	 *
	 * @return whether the throw was played
	 */
	boolean throwJar()
	{
		Player player = client.getLocalPlayer();
		if (player == null || player.getAnimation() != NO_ANIMATION)
			return false;

		thrownFrom = player.getWorldLocation();
		player.setAnimation(AnimationID.HUMAN_THROW);
		player.setAnimationFrame(0);
		return true;
	}

	/**
	 * Lines up the character's reaction to a throw that was played. It starts once the throw is over, and only if
	 * the character is then idle and still on the tile it threw from. A newer result replaces a waiting one.
	 *
	 * @param outcome what the throw led to
	 */
	void react(CatchOutcome outcome)
	{
		int animation = CatchScene.reactionFor(outcome);
		reaction = animation == CatchScene.NONE ? null : new Reaction(animation, thrownFrom);
	}

	/**
	 * Marks a creature to react when the jar lands: it is replaced by a copy that acts out the result, and the
	 * real monster comes back when that is over. A monster in any fight is left alone, so nothing that can hurt
	 * anyone is ever hidden.
	 *
	 * @param outcome what the throw led to
	 * @param npc the monster that was thrown at, or null when it already left view
	 */
	void play(CatchOutcome outcome, NPC npc)
	{
		if (npc == null || fighting(npc) || TargetScript.end(outcome) == 0)
			return;

		finish(acts.put(npc.getIndex(), new CatchAct(npc, outcome)));
	}

	/** Moves every effect on by one client tick. */
	void clientTick()
	{
		if (!enabled.getAsBoolean())
		{
			clear();
			return;
		}

		tickReaction();
		Iterator<CatchAct> effects = acts.values().iterator();
		while (effects.hasNext())
		{
			CatchAct effect = effects.next();
			effect.age++;
			if (!advance(effect))
			{
				finish(effect);
				effects.remove();
			}
		}
	}

	/**
	 * Moves one effect on and draws it. Any failure in client code ends the effect, which shows the real monster.
	 *
	 * @param effect the effect, already one tick older
	 * @return false when the effect is over or must end now
	 */
	private boolean advance(CatchAct effect)
	{
		try
		{
			if (!stillValid(effect))
				return false;

			if (effect.age >= TargetScript.LAND && !effect.copyTried)
				startCopy(effect);

			if (effect.copyTried && effect.copy == null && effect.outcome != CatchOutcome.CAUGHT)
				return false;

			if (effect.copy != null)
				stepCopy(effect);

			int shade = shadeAt(effect.outcome, effect.age);
			if (shade >= 0)
				show(effect, shade);

			return shade != DONE;
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: catch effect for NPC {} failed", effect.npc.getId(), e);
			return false;
		}
	}

	private boolean stillValid(CatchAct effect)
	{
		WorldView view = client.getTopLevelWorldView();
		return view != null
			&& client.getGameState() == GameState.LOGGED_IN
			&& view.npcs().byIndex(effect.npc.getIndex()) == effect.npc
			&& !fighting(effect.npc)
			&& (effect.copy == null || view.getPlane() == effect.plane);
	}

	/**
	 * Builds the copy of the monster on the spot it stands on at the landing. A monster that cannot be copied
	 * leaves a caught creature with the plain vanish and the other results with nothing.
	 *
	 * @param effect the effect that reached the landing
	 */
	private void startCopy(CatchAct effect)
	{
		effect.copyTried = true;
		WorldView view = client.getTopLevelWorldView();
		LocalPoint at = effect.npc.getLocalLocation();
		if (at == null || view == null || !TargetScript.hidden(effect.outcome, effect.age))
			return;

		NpcModels.Parts parts = NpcModels.load(client, effect.npc.getId());
		if (parts == null)
			return;

		CreatureBody body = CreatureBody.create(client, animations, parts, parts.widthScale(), parts.heightScale());
		if (body == null)
			return;

		body.setBase(animations.idle(parts.npcId()));
		effect.copy = body;
		effect.plane = view.getPlane();
		effect.startX = at.getX();
		effect.startY = at.getY();
		effect.facing = effect.npc.getCurrentOrientation();
		effect.wantFacing = effect.facing;
	}

	/**
	 * Plays the script on the copy for this tick: the act that starts, the turn, the size, the opacity and the run.
	 *
	 * @param effect the effect with a copy
	 */
	private void stepCopy(CatchAct effect)
	{
		CreatureBody body = effect.copy;
		if (TargetScript.done(effect.outcome, effect.age))
		{
			effect.copy = null;
			release(body);
			return;
		}

		TargetScript.Step step = TargetScript.stepAt(effect.outcome, effect.age);
		if (step == TargetScript.Step.HIT)
			body.act(CreatureBody.Act.HIT);
		else if (step == TargetScript.Step.ATTACK)
			body.act(CreatureBody.Act.ATTACK);

		if (effect.age == TargetScript.ACT_TICK)
			startTurn(effect);

		if (effect.turning)
			effect.facing = FollowerPath.turnToward(effect.facing, effect.wantFacing);

		body.clientTick();
		body.setSize(TargetScript.size(effect.outcome, effect.age));
		body.setOpacity(TargetScript.opacity(effect.outcome, effect.age));
		place(effect);
	}

	/**
	 * Works out which way the copy turns as its second act starts, and starts it running when it flees.
	 *
	 * @param effect the effect at the second act
	 */
	private void startTurn(CatchAct effect)
	{
		TargetScript.Turn turn = TargetScript.turnOf(effect.outcome);
		if (turn == TargetScript.Turn.NONE)
			return;

		Player player = client.getLocalPlayer();
		LocalPoint from = player == null ? null : player.getLocalLocation();
		double dx = from == null ? 0 : effect.startX - from.getX();
		double dy = from == null ? 0 : effect.startY - from.getY();
		double length = Math.sqrt(dx * dx + dy * dy);
		if (length < 1)
		{
			double turned = effect.facing * 2 * Math.PI / FollowerPath.FULL_TURN;
			dx = -Math.sin(turned);
			dy = -Math.cos(turned);
			length = 1;
		}

		effect.runX = dx / length;
		effect.runY = dy / length;
		effect.turning = true;
		if (turn == TargetScript.Turn.TO_PLAYER)
		{
			effect.wantFacing = FollowerPath.angle(-effect.runX, -effect.runY);
			return;
		}

		effect.wantFacing = FollowerPath.angle(effect.runX, effect.runY);
		effect.copy.setBase(animations.walk(effect.copy.npcId()));
	}

	/**
	 * Puts the copy where the script wants it, or takes it out of the scene while it cannot be seen.
	 *
	 * @param effect the effect with a copy
	 */
	private void place(CatchAct effect)
	{
		WorldView view = client.getTopLevelWorldView();
		CreatureBody body = effect.copy;
		double run = TargetScript.runTiles(effect.outcome, effect.age) * TILE;
		double turned = effect.facing * 2 * Math.PI / FollowerPath.FULL_TURN;
		double push = body.push() * TILE;
		int x = (int) Math.round(effect.startX + effect.runX * run - Math.sin(turned) * push);
		int y = (int) Math.round(effect.startY + effect.runY * run - Math.cos(turned) * push);
		boolean inScene = view != null && x >= 0 && y >= 0 && x < view.getSizeX() * TILE && y < view.getSizeY() * TILE;
		if (!inScene || !body.visible())
		{
			if (body.isActive())
				body.setActive(false);

			return;
		}

		body.setLocation(new LocalPoint(x, y, view), effect.plane);
		body.setOrientation(effect.facing);
		if (!body.isActive())
			body.setActive(true);
	}

	/** Ages the waiting reaction and plays it when its moment comes. */
	private void tickReaction()
	{
		if (reaction == null)
			return;

		reaction.age++;
		if (reaction.age < CatchScene.REACT_TICK)
			return;

		Reaction due = reaction;
		reaction = null;
		Player player = client.getLocalPlayer();
		if (player == null || player.getAnimation() != NO_ANIMATION || due.tile == null
			|| !due.tile.equals(player.getWorldLocation()))
			return;

		player.setAnimation(due.animation);
		player.setAnimationFrame(0);
	}

	/**
	 * Tells whether a monster is in a fight: it is targeting someone, or its health bar shows, which it does for a
	 * while after anyone hits it.
	 *
	 * @param npc the monster
	 * @return whether it must stay in sight
	 */
	static boolean fighting(NPC npc)
	{
		return npc.getInteracting() != null || npc.getHealthRatio() >= 0;
	}

	/**
	 * Ends the effect on a monster that left view.
	 *
	 * @param npc the monster
	 */
	void forget(NPC npc)
	{
		CatchAct effect = acts.get(npc.getIndex());
		if (effect == null || effect.npc != npc)
			return;

		finish(effect);
		acts.remove(npc.getIndex());
	}

	/** Ends every effect at once, for a logout, a world hop or the plugin stopping. */
	void clear()
	{
		acts.values().forEach(this::finish);
		acts.clear();
		reaction = null;
	}

	/**
	 * Ends every effect at once because a map is loading. Objects the plugin put into the scene stop being drawn
	 * after a map load even though they still report as active, so nothing may be left playing across one.
	 */
	void sceneLoading()
	{
		clear();
	}

	/**
	 * Keeps a monster that is out of sight or fading in from being drawn. RuneLite asks this for every creature,
	 * several times a frame.
	 *
	 * @param renderable the creature RuneLite is about to draw
	 * @param ui whether the question is about its health bar and other overhead parts
	 * @return false for a monster whose catch effect is playing, true for everything else
	 */
	@Override
	public boolean addEntity(Renderable renderable, boolean ui)
	{
		if (acts.isEmpty() || !(renderable instanceof NPC))
			return true;

		CatchAct effect = acts.get(((NPC) renderable).getIndex());
		return effect == null || effect.npc != renderable || shadeAt(effect.outcome, effect.age) == VISIBLE;
	}

	/**
	 * Works out what a caught creature looks like at one moment of its effect.
	 *
	 * @param age client ticks since the throw
	 * @return {@link #VISIBLE} until the jar lands, {@link #HIDDEN} while it is out of sight,
	 *     then the fade step from 0 (faintest) to {@link #FADE_STEPS} - 1 (nearly solid), then {@link #DONE}
	 */
	static int shadeAt(int age)
	{
		if (!CatchScene.landed(age))
			return VISIBLE;

		int hiddenFor = age - CatchScene.LAND_TICK;
		if (hiddenFor <= HIDDEN_TICKS)
			return HIDDEN;

		int fading = hiddenFor - HIDDEN_TICKS - 1;
		return fading >= FADE_TICKS ? DONE : fading * FADE_STEPS / FADE_TICKS;
	}

	/**
	 * Works out what the real monster looks like at one moment of the effect for a result.
	 *
	 * @param outcome what the throw led to
	 * @param age client ticks since the throw
	 * @return {@link #VISIBLE} until the jar lands, {@link #HIDDEN} while a copy acts in its place or, for a caught
	 *     creature, while it is out of sight, then the fade step, then {@link #DONE}. A creature that broke free
	 *     is never faded: it is simply back at {@link #DONE}.
	 */
	static int shadeAt(CatchOutcome outcome, int age)
	{
		if (TargetScript.end(outcome) == 0 || age < TargetScript.LAND)
			return VISIBLE;

		switch (outcome)
		{
			case CAUGHT:
				return shadeAt(age);
			case FLED:
				return fadeAfter(age - TargetScript.FLED_END);
			default:
				return TargetScript.hidden(outcome, age) ? HIDDEN : DONE;
		}
	}

	private static int fadeAfter(int fading)
	{
		if (fading < 0)
			return HIDDEN;

		return fading >= FADE_TICKS ? DONE : fading * FADE_STEPS / FADE_TICKS;
	}

	/**
	 * Works out how see-through one step of the fade is.
	 *
	 * @param step the fade step, from 0 to {@link #FADE_STEPS} - 1
	 * @return the game's transparency value, from just under 255 (cannot be seen) down toward 0 (solid)
	 */
	static int transparencyAt(int step)
	{
		return SEE_THROUGH - (step + 1) * SEE_THROUGH / (FADE_STEPS + 1);
	}

	/**
	 * Draws one step of the fade: a see-through copy of the monster, standing where the monster stands. The copies
	 * are made the first time they are needed. A monster that cannot be copied simply stays out of sight until
	 * the effect ends.
	 *
	 * @param effect the creature fading in
	 * @param shade the fade step to draw
	 */
	private void show(CatchAct effect, int shade)
	{
		if (effect.shades == null)
			effect.shades = shadesOf(effect.npc);

		LocalPoint at = effect.npc.getLocalLocation();
		if (effect.shades.length == 0 || at == null || effect.npc.getWorldView() == null)
			return;

		if (effect.ghost == null)
			effect.ghost = client.createRuneLiteObject();

		effect.ghost.setModel(effect.shades[shade]);
		effect.ghost.setLocation(at, effect.npc.getWorldView().getPlane());
		effect.ghost.setOrientation(effect.npc.getCurrentOrientation());
		if (!effect.ghost.isActive())
			effect.ghost.setActive(true);
	}

	/**
	 * Makes the see-through copies of a monster, one per fade step.
	 *
	 * @param npc the monster
	 * @return the copies, faintest first; empty when the monster's model cannot be loaded
	 */
	private Model[] shadesOf(NPC npc)
	{
		try
		{
			NpcModels.Parts parts = NpcModels.load(client, npc.getId());
			if (parts == null)
				return new Model[0];

			ModelData whole = parts.scaled();
			Model[] shades = new Model[FADE_STEPS];
			for (int step = 0; step < FADE_STEPS; step++)
			{
				ModelData faint = whole.shallowCopy();
				faint.cloneTransparencies(true);
				fade(faint.getFaceTransparencies(), transparencyAt(step));
				shades[step] = faint.light();
				if (shades[step] == null)
					return new Model[0];
			}

			return shades;
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not draw the fade for NPC {}", npc.getId(), e);
			return new Model[0];
		}
	}

	/**
	 * Makes every face at least as see-through as a fade step asks. A face that was already more see-through,
	 * such as a ghost's, stays as it was.
	 *
	 * @param transparencies one value per face, 0 for solid up to 255 for unseen, changed in place
	 * @param transparency the least see-through any face may be
	 */
	static void fade(byte[] transparencies, int transparency)
	{
		if (transparencies == null)
			return;

		for (int i = 0; i < transparencies.length; i++)
			transparencies[i] = (byte) Math.max(transparencies[i] & 0xFF, transparency);
	}

	/**
	 * Takes the copy and the fade away. Never throws: each client call is guarded on its own.
	 *
	 * @param effect the effect, or null for none
	 */
	private void finish(CatchAct effect)
	{
		if (effect == null)
			return;

		CreatureBody body = effect.copy;
		effect.copy = null;
		if (body != null)
			release(body);

		try
		{
			if (effect.ghost != null && effect.ghost.isActive())
				effect.ghost.setActive(false);
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not remove the fade", e);
		}
	}

	private void release(CreatureBody body)
	{
		try
		{
			if (body.isActive())
				body.setActive(false);
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not remove the copy", e);
		}
	}
}
