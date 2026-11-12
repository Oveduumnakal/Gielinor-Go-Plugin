/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Renderable;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.AnimationID;
import net.runelite.client.callback.RenderCallback;

/**
 * What a catch looks like: the player makes a throwing motion, and a creature that was caught drops out of sight
 * for five seconds and then fades back in. All of it is drawn on the player's own screen only. The monster in the real
 * game is untouched, nothing is sent to the game, and other players see nothing. Every method must be called on
 * the game thread; RuneLite calls {@link #addEntity(Renderable, boolean)} from it too.
 */
final class CatchEffects implements RenderCallback
{
	private static final Logger log = LoggerFactory.getLogger(CatchEffects.class);

	/** Client ticks, 20 ms each, a caught creature stays out of sight: five seconds. */
	static final int HIDDEN_TICKS = 250;

	/** Client ticks a caught creature takes to fade back in. */
	static final int FADE_TICKS = 30;

	/** How many steps the fade is drawn in, from nearly see-through to nearly solid. */
	static final int FADE_STEPS = 6;

	/** {@link #shadeAt(int)} for a creature that is still out of sight. */
	static final int HIDDEN = -1;

	/** {@link #shadeAt(int)} for a creature whose effect is over. */
	static final int DONE = -2;

	/** The game's value for a face that cannot be seen at all; 0 is solid. */
	private static final int SEE_THROUGH = 255;

	/** The animation id that means the player is not doing anything. */
	private static final int NO_ANIMATION = -1;

	/** One caught creature that is out of sight or fading back in. */
	private static final class Vanish
	{
		private final NPC npc;
		private int age;
		private Model[] shades;
		private RuneLiteObject ghost;

		private Vanish(NPC npc)
		{
			this.npc = npc;
		}
	}

	private final Client client;
	private final Map<Integer, Vanish> vanishing = new HashMap<>();

	/**
	 * Creates the effects with nothing playing.
	 *
	 * @param client the game client
	 */
	CatchEffects(Client client)
	{
		this.client = client;
	}

	/**
	 * Makes the player's character throw. The motion is left out while the character is in the middle of
	 * something else, such as fishing or fighting, so a real animation is never cut short on screen.
	 */
	void throwJar()
	{
		Player player = client.getLocalPlayer();
		if (player == null || player.getAnimation() != NO_ANIMATION)
			return;

		player.setAnimation(AnimationID.HUMAN_THROW);
		player.setAnimationFrame(0);
	}

	/**
	 * Takes a caught creature out of sight; it fades back in by itself. A monster in any fight is left alone, so
	 * nothing that can hurt anyone is ever hidden.
	 *
	 * @param npc the monster that was caught, or null when it already left view
	 */
	void vanish(NPC npc)
	{
		if (npc == null || fighting(npc))
			return;

		finish(vanishing.put(npc.getIndex(), new Vanish(npc)));
	}

	/** Moves every effect on by one client tick. */
	void clientTick()
	{
		Iterator<Vanish> effects = vanishing.values().iterator();
		while (effects.hasNext())
		{
			Vanish effect = effects.next();
			if (fighting(effect.npc))
			{
				finish(effect);
				effects.remove();
				continue;
			}

			effect.age++;
			int shade = shadeAt(effect.age);
			if (shade >= 0)
				show(effect, shade);

			if (shade == DONE)
			{
				finish(effect);
				effects.remove();
			}
		}
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
		Vanish effect = vanishing.get(npc.getIndex());
		if (effect == null || effect.npc != npc)
			return;

		finish(effect);
		vanishing.remove(npc.getIndex());
	}

	/** Ends every effect at once, for a logout, a world hop or the plugin stopping. */
	void clear()
	{
		vanishing.values().forEach(this::finish);
		vanishing.clear();
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
		if (vanishing.isEmpty() || !(renderable instanceof NPC))
			return true;

		Vanish effect = vanishing.get(((NPC) renderable).getIndex());
		return effect == null || effect.npc != renderable;
	}

	/**
	 * Works out what a caught creature looks like at one moment of its effect.
	 *
	 * @param age client ticks since the catch
	 * @return {@link #HIDDEN} while it is out of sight, then the fade step from 0 (faintest) to
	 *     {@link #FADE_STEPS} - 1 (nearly solid), then {@link #DONE}
	 */
	static int shadeAt(int age)
	{
		if (age <= HIDDEN_TICKS)
			return HIDDEN;

		int fading = age - HIDDEN_TICKS - 1;
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
	private void show(Vanish effect, int shade)
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

	private void finish(Vanish effect)
	{
		if (effect != null && effect.ghost != null && effect.ghost.isActive())
			effect.ghost.setActive(false);
	}
}
