/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Decides when an object this plugin added to the scene must be taken out and put back in. The client rebuilds the
 * scene when a new map area loads, and an object added before that is no longer drawn, even though the client may
 * still list it as added. So once a load has been seen and the game is playable again, the object is removed on one
 * client tick and added again on the next. It has no link to the game client, so it can be tested.
 */
final class SceneReload
{
	/** What to do with the object on this client tick. */
	enum Step
	{
		/** Nothing special: draw as usual. */
		NONE,

		/** Take the object out of the scene and do nothing else this tick. */
		DROP,

		/** Put the object back in the scene this tick, whatever the client says about it. */
		ADD
	}

	private boolean loadSeen;
	private boolean dropped;

	/** Notes that the scene is loading, or is about to. */
	void loading()
	{
		loadSeen = true;
		dropped = false;
	}

	/**
	 * Asks what to do on this client tick.
	 *
	 * @param playable whether the game is fully logged in, with its scene ready
	 * @return {@link Step#DROP} once after a load, {@link Step#ADD} on the tick after that, else {@link Step#NONE}
	 */
	Step next(boolean playable)
	{
		if (!playable)
			return Step.NONE;

		if (dropped)
		{
			dropped = false;
			return Step.ADD;
		}

		if (loadSeen)
		{
			loadSeen = false;
			dropped = true;
			return Step.DROP;
		}

		return Step.NONE;
	}

	/** Forgets any load seen, for when the object is gone for good. */
	void reset()
	{
		loadSeen = false;
		dropped = false;
	}
}
