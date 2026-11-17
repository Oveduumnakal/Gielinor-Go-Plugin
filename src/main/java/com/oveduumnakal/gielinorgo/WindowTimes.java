/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Every duration and size of the window's motion in one place, in milliseconds unless the name says otherwise, so
 * the feel can be tuned without hunting through the paint code. These are the {@link MotionMode#FULL} values; the
 * other modes shorten or drop them. Holds no RuneLite types.
 */
final class WindowTimes
{
	/** The window opening: scale and fade in. */
	static final long OPEN = 150;

	/** The window closing by the player: scale and fade out. */
	static final long CLOSE = 100;

	/** The window closing because of combat or a battle: a quick fade with no scale. */
	static final long COMBAT_CLOSE = 120;

	/** The scale the window opens from and closes to. */
	static final double OPEN_FROM_SCALE = 0.94;

	/** A party row sliding in. */
	static final long ROW_IN = 180;

	/** The gap between neighbouring rows sliding in. */
	static final long ROW_STAGGER = 50;

	/** The selection cursor gliding to the hovered row. */
	static final long CURSOR_GLIDE = 100;

	/** A hovered row nudging right. */
	static final long HOVER_NUDGE = 80;

	/** A health bar draining or filling. */
	static final long HP_BAR = 300;

	/** An experience bar filling. */
	static final long XP_BAR = 400;

	/** A portrait hopping once when hovered. */
	static final long PORTRAIT_HOP = 150;

	/** A portrait fading in when its picture arrives. */
	static final long PORTRAIT_FADE = 150;

	/** The highlight sliding to a new tab, and the page fading in. */
	static final long TAB = 120;

	/** How far a page moves while it fades in, in layout units. */
	static final double PAGE_MOVE_UNITS = 8;

	/** The Summary portrait growing out of its row. */
	static final long SUMMARY_GROW = 200;

	/** The gap between Summary stat bars and move chips popping in. */
	static final long SUMMARY_STAGGER = 60;

	/** The right-click menu scaling out. */
	static final long MENU = 100;

	/** A button shrinking while pressed. */
	static final long PRESS = 80;

	/** The scale a pressed button shrinks to. */
	static final double PRESS_SCALE = 0.97;

	/** Two rows sliding to each other's place, or a row sliding out. Rows ignore clicks until they land. */
	static final long SLIDE = 200;

	/** The side-to-side shake of a locked toggle or tab. */
	static final long SHAKE = 200;

	/** How far a shake moves, in layout units. */
	static final double SHAKE_UNITS = 4;

	/** The notice popping in. */
	static final long NOTICE_POP = 150;

	/** The notice staying fully visible. */
	static final long NOTICE_HOLD = 2_000;

	/** The notice fading out. */
	static final long NOTICE_FADE = 400;

	/** The scale the notice pops in from. */
	static final double NOTICE_FROM_SCALE = 0.8;

	/** The text box caret blinking, one on-and-off round. */
	static final long CARET_BLINK = 1_000;

	/** The lead arrow bobbing, one round. */
	static final long LEAD_BOB = 1_000;

	/** The lead row outline pulsing, one round. */
	static final long LEAD_PULSE = 1_600;

	/** The pulsing dot on the toggle, one round. */
	static final long DOT_PULSE = 1_600;

	/** A portrait's idle bob, one round. */
	static final long PORTRAIT_BOB = 2_400;

	/** A gilded portrait's light sweep, how often it starts. */
	static final long GILDED_EVERY = 4_000;

	/** A gilded portrait's light sweep, how long one lasts. */
	static final long GILDED_SWEEP = 600;

	private WindowTimes()
	{
	}
}
