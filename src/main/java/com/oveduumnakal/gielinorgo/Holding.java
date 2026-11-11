/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/** Where something the player owns is kept, so the side panel can point at it for a trade. */
enum Holding
{
	/** A creature on the team, by its place in {@link PanelState#team()}. */
	TEAM,

	/** A creature in storage, by its place in {@link PanelState#storage()}. */
	STORAGE,

	/** An egg being carried, by its place in {@link PanelState#eggs()}. */
	EGG
}
