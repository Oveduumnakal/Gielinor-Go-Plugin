/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import net.runelite.api.Animation;
import net.runelite.api.AnimationController;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.Perspective;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;

/**
 * The arrow, bolt or breath that crosses the gap in a battle acted out on screen. It is one object that this
 * plugin moves itself every client tick, along a {@link ProjectilePath}; it is never a game projectile, so other
 * plugins that react to projectiles do not see it, and it is drawn on the player's own screen only. Its looks come
 * from {@link ProjectileLook}. Every method must be called on the game thread.
 */
final class BattleProjectile extends RuneLiteObject
{
	/** Scene units in one tile. */
	private static final int TILE = 128;

	private final Client client;
	private final ProjectileModels models;
	private final AnimationController controller;
	private ProjectilePath path;
	private ProjectileLook look;
	private Model shown;
	private int size = ProjectileLook.NORMAL_SIZE;

	/**
	 * Creates the object, which is not in the scene until a look is launched.
	 *
	 * @param client the game client
	 * @param models where the models of the looks are built
	 */
	BattleProjectile(Client client, ProjectileModels models)
	{
		super(client);
		this.client = client;
		this.models = models;
		this.controller = new AnimationController(client, (Animation) null);
		this.controller.setOnFinished(CreatureBody::restart);
		setAnimationController(controller);
	}

	/**
	 * Gets a flight ready. Nothing is drawn until {@link #place}.
	 *
	 * @param flight which look
	 * @param route the way it flies
	 * @param casterScale how large the caster is drawn compared with the real monster, where 1.0 is as modelled
	 * @return false when the look has no model, so this attack shows no projectile
	 */
	boolean launch(ProjectileLook flight, ProjectilePath route, double casterScale)
	{
		int drawn = flight.sizeFor(casterScale);
		Model model = models.modelOf(flight, drawn);
		if (model == null)
			return false;

		hide();
		look = flight;
		path = route;
		shown = model;
		size = drawn;
		Animation animation = flight.animated() ? client.loadAnimation(flight.animationId()) : null;
		controller.setAnimation(animation);
		setModel(model);
		return true;
	}

	/**
	 * Puts the projectile where it is at a moment of the turn, or takes it out of the scene when it is not in the
	 * air then.
	 *
	 * @param age client ticks since the turn began
	 * @param view the top level world view
	 * @param plane the plane of the battle
	 */
	void place(int age, WorldView view, int plane)
	{
		if (path == null || !ProjectilePath.visible(age))
		{
			hide();
			return;
		}

		double tileX = path.x(age);
		double tileY = path.y(age);
		int x = (int) Math.round((tileX - view.getBaseX() + 0.5) * TILE);
		int y = (int) Math.round((tileY - view.getBaseY() + 0.5) * TILE);
		if (x < 0 || y < 0 || x >= view.getSizeX() * TILE || y >= view.getSizeY() * TILE)
		{
			hide();
			return;
		}

		LocalPoint at = new LocalPoint(x, y, view);
		double[] way = path.direction();
		setLocation(at, plane);
		if (path.hasGround())
			setZ((int) Math.round(path.z(age)));
		else
			setZ(Perspective.getTileHeight(client, at, plane) - (int) Math.round(path.height(age)));

		setOrientation(FollowerPath.angle(way[0], way[1]));
		if (!isActive())
			setActive(true);
	}

	/** Takes the projectile out of the scene. Safe to call at any time. */
	void hide()
	{
		if (isActive())
			setActive(false);
	}

	/** Takes the projectile out of the scene for good and forgets its flight. */
	void remove()
	{
		hide();
		path = null;
		look = null;
		shown = null;
	}

	/** @return the look that is flying, or null when nothing is */
	ProjectileLook look()
	{
		return look;
	}

	@Override
	public Model getModel()
	{
		if (shown == null || controller.getAnimation() == null)
			return shown;

		Model posed = super.getModel();
		if (posed == null || posed == getBaseModel())
			return shown;

		if (size != ProjectileLook.NORMAL_SIZE)
			posed.scale(size, size, size);

		return posed;
	}
}
