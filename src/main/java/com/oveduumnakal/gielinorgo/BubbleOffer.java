/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What one wild creature offers the player this tick: the jar a Catch would throw, the jars owned, whether Battle is
 * ready, whether the creature is knocked out and the words of its card. Immutable. Holds no RuneLite types.
 */
final class BubbleOffer
{
	private final int wildId;
	private final JarType jar;
	private final List<JarSuggest.JarChoice> owned;
	private final boolean battleReady;
	private final boolean knockedOut;
	private final WildCardContent card;

	/**
	 * @param wildId the creature's own id
	 * @param jar the jar a Catch throws, or null when the player has none
	 * @param owned every owned jar with its odds against this creature; copied
	 * @param battleReady whether Battle would start; false greys the bubble, which still can be clicked
	 * @param knockedOut whether the creature lost its one battle and is easier to catch
	 * @param card the words of the card
	 */
	BubbleOffer(int wildId, JarType jar, List<JarSuggest.JarChoice> owned, boolean battleReady, boolean knockedOut,
		WildCardContent card)
	{
		this.wildId = wildId;
		this.jar = jar;
		this.owned = Collections.unmodifiableList(new ArrayList<>(owned));
		this.battleReady = battleReady;
		this.knockedOut = knockedOut;
		this.card = card;
	}

	/** @return the creature's own id */
	int wildId()
	{
		return wildId;
	}

	/** @return the jar a Catch throws, or null when the player has none */
	JarType jar()
	{
		return jar;
	}

	/** @return every owned jar with its odds, in no particular order; never changes */
	List<JarSuggest.JarChoice> owned()
	{
		return owned;
	}

	/** @return whether Battle would start */
	boolean battleReady()
	{
		return battleReady;
	}

	/** @return whether the creature is knocked out */
	boolean knockedOut()
	{
		return knockedOut;
	}

	/** @return the words of the card */
	WildCardContent card()
	{
		return card;
	}
}
