/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Model;
import net.runelite.client.callback.ClientThread;

/**
 * Makes idle loops. A model can only be read and posed on the game thread while logged in, so a few poses are taken
 * each game tick and their numbers copied out; the pictures are then painted off that thread. The last few loops are
 * kept. A creature with no standing animation that can be sampled keeps its still picture.
 */
final class IdleService implements IdleStrip.Source
{
	private static final Logger log = LoggerFactory.getLogger(IdleService.class);

	/** Poses taken in one game tick, which keeps the cost of posing small on any one frame. */
	private static final int POSES_PER_TICK = 2;

	/** How many creatures' loops are kept. */
	private static final int KEPT = 4;

	/** A loop being made. Only the game thread touches it until it is handed to the painter. */
	private static final class Making
	{
		private final String key;
		private final Model base;
		private final Animation animation;
		private final int[] frames;
		private final List<PortraitPainter.Mesh> poses = new ArrayList<>();

		private Making(String key, Model base, Animation animation)
		{
			this.key = key;
			this.base = base;
			this.animation = animation;
			this.frames = IdleStrip.sample(animation.getFrameLengths().length);
		}
	}

	private final Client client;
	private final ClientThread clientThread;
	private final Executor painting;
	private final Function<String, Species> species;
	private final AnimationTable animations;
	private final BooleanSupplier enabled;
	private final Map<String, IdleStrip> strips = new LinkedHashMap<String, IdleStrip>(16, 0.75f, true)
	{
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, IdleStrip> eldest)
		{
			return size() > KEPT;
		}
	};
	private String wanted;
	private String wantedSpecies;
	private int wantedNpc;
	private String failed;
	private Making making;
	private boolean busy;

	/**
	 * Creates the service.
	 *
	 * @param client the game client
	 * @param clientThread runs work on the game thread
	 * @param painting paints the pictures, off the game thread
	 * @param species finds a species by name or key
	 * @param animations the standing animation of each NPC id
	 * @param enabled whether the "Idle animation" setting is on
	 */
	IdleService(Client client, ClientThread clientThread, Executor painting, Function<String, Species> species,
		AnimationTable animations, BooleanSupplier enabled)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.painting = painting;
		this.species = species;
		this.animations = animations;
		this.enabled = enabled;
	}

	@Override
	public synchronized IdleStrip strip(String name, int npcId)
	{
		if (!enabled.getAsBoolean())
			return null;

		String key = PortraitSource.cacheKey(name, npcId);
		IdleStrip ready = strips.get(key);
		if (ready == null)
		{
			wanted = key;
			wantedSpecies = name;
			wantedNpc = npcId;
		}

		return ready;
	}

	/** Takes a few poses of the loop being made, or starts the one asked for last. Called on the game thread. */
	void tick()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
			return;

		try
		{
			Making current = next();
			if (current != null)
				pose(current);
		}
		catch (RuntimeException | AssertionError e)
		{
			log.debug("Gielinor Go: could not pose a creature", e);
			giveUp();
		}
	}

	/** Forgets every loop, for when the plugin stops. */
	void clear()
	{
		synchronized (this)
		{
			strips.clear();
			wanted = null;
			failed = null;
		}

		clientThread.invokeLater(() -> making = null);
	}

	/** @return the loop to go on with, starting the one wanted when it is a different one, or null for nothing */
	private Making next()
	{
		String key;
		String name;
		int npcId;
		synchronized (this)
		{
			if (busy || wanted == null || wanted.equals(failed) || strips.containsKey(wanted))
				return making;

			key = wanted;
			name = wantedSpecies;
			npcId = wantedNpc;
		}

		if (making == null || !making.key.equals(key))
			making = start(key, species.apply(Species.keyFor(name)), npcId);

		return making;
	}

	private Making start(String key, Species found, int preferred)
	{
		if (found != null)
		{
			for (int npcId : PortraitService.tryOrder(found, preferred))
			{
				int id = animations.idle(npcId);
				Animation animation = id == AnimationTable.NONE ? null : client.loadAnimation(id);
				if (animation == null || animation.isMayaAnim() || animation.getFrameLengths() == null
					|| animation.getFrameLengths().length < 2)
					continue;

				NpcModels.Parts parts = NpcModels.load(client, npcId);
				if (parts != null)
					return new Making(key, parts.scaled().light(), animation);
			}
		}

		synchronized (this)
		{
			failed = key;
		}

		return null;
	}

	private void pose(Making current)
	{
		AnimationController controller = new AnimationController(client, current.animation);
		for (int i = 0; i < POSES_PER_TICK && current.poses.size() < current.frames.length; i++)
		{
			controller.setFrame(current.frames[current.poses.size()]);
			current.poses.add(copy(controller.animate(current.base)));
		}

		if (current.poses.size() < current.frames.length)
			return;

		making = null;
		long[] millis = IdleStrip.millis(current.frames, current.animation.getFrameLengths());
		synchronized (this)
		{
			busy = true;
		}

		painting.execute(() -> paint(current.key, current.poses, millis));
	}

	/** Copies a posed model's numbers at once, since the client may reuse its arrays for the next pose. */
	private static PortraitPainter.Mesh copy(Model model)
	{
		int[][] corners = {model.getFaceIndices1().clone(), model.getFaceIndices2().clone(),
			model.getFaceIndices3().clone()};
		int[][] colors = {model.getFaceColors1().clone(), model.getFaceColors2().clone(),
			model.getFaceColors3().clone()};
		byte[] transparencies = model.getFaceTransparencies() == null ? null : model.getFaceTransparencies().clone();
		return new PortraitPainter.Mesh(model.getVerticesX().clone(), model.getVerticesY().clone(),
			model.getVerticesZ().clone(), corners, colors, transparencies);
	}

	private void paint(String key, List<PortraitPainter.Mesh> poses, long[] millis)
	{
		try
		{
			List<BufferedImage> pictures = PortraitPainter.paintAll(poses, PortraitService.SIZE);
			synchronized (this)
			{
				if (pictures == null)
					failed = key;
				else
					strips.put(key, new IdleStrip(pictures, millis));
			}
		}
		catch (RuntimeException e)
		{
			log.debug("Gielinor Go: could not paint an idle loop", e);
			synchronized (this)
			{
				failed = key;
			}
		}
		finally
		{
			synchronized (this)
			{
				busy = false;
			}
		}
	}

	private void giveUp()
	{
		making = null;
		synchronized (this)
		{
			failed = wanted;
		}
	}
}
