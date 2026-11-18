/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** The kinds of bubble that can be painted and clicked above a wild creature. Holds no RuneLite types. */
enum BubbleKind
{
	/** The Catch bubble: throws a jar. */
	JAR,

	/** The Battle bubble. */
	BATTLE,

	/** The Info bubble: shows the abbreviated card. */
	INFO,

	/** One jar in the cluster that opens from the Catch bubble; picks the jar to throw. */
	CLUSTER_JAR,

	/** The abbreviated info card. */
	CARD
}
