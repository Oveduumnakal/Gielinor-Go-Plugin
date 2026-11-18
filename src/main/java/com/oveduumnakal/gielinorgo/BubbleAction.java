/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** What a press on a bubble asks the plugin to do. Immutable. Holds no RuneLite types. */
final class BubbleAction
{
	/** The things a press can ask for. */
	enum Kind
	{
		/** Nothing for the plugin to do; the press may still have changed the bubbles. */
		NONE,

		/** Throw the jar the creature would be caught with; a null jar lets the game say there is none. */
		THROW,

		/** Start a battle; the game says why when it refuses. */
		BATTLE,

		/** Make a jar the one to throw, without throwing it. */
		SELECT_JAR
	}

	/** The action that asks for nothing. */
	static final BubbleAction NONE = new BubbleAction(Kind.NONE, -1, null);

	private final Kind kind;
	private final int wildId;
	private final JarType jar;

	private BubbleAction(Kind kind, int wildId, JarType jar)
	{
		this.kind = kind;
		this.wildId = wildId;
		this.jar = jar;
	}

	/**
	 * @param wildId the creature
	 * @param jar the jar that would be thrown, or null for none
	 * @return a throw
	 */
	static BubbleAction throwJar(int wildId, JarType jar)
	{
		return new BubbleAction(Kind.THROW, wildId, jar);
	}

	/**
	 * @param wildId the creature
	 * @return a battle
	 */
	static BubbleAction battle(int wildId)
	{
		return new BubbleAction(Kind.BATTLE, wildId, null);
	}

	/**
	 * @param wildId the creature
	 * @param jar the jar picked
	 * @return a jar selection
	 */
	static BubbleAction select(int wildId, JarType jar)
	{
		return new BubbleAction(Kind.SELECT_JAR, wildId, jar);
	}

	/** @return what to do */
	Kind kind()
	{
		return kind;
	}

	/** @return the creature, or -1 for {@link Kind#NONE} */
	int wildId()
	{
		return wildId;
	}

	/** @return the jar picked for {@link Kind#SELECT_JAR}, the one that would go for {@link Kind#THROW}, else null */
	JarType jar()
	{
		return jar;
	}
}
