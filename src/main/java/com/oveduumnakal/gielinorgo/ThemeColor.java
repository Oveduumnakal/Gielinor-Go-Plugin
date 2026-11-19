/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Every colour the painted window uses, by name. Paint code asks a {@link WindowTheme} for one of these and never
 * writes a colour of its own, so a new style is one more theme that fills each name. Holds no RuneLite types.
 */
enum ThemeColor
{
	/** The window body behind everything. */
	WINDOW,

	/** The outline of the window. */
	FRAME,

	/** The header bar behind the tabs. */
	HEADER,

	/** A tab that is not selected. */
	TAB,

	/** The selected tab. */
	TAB_ACTIVE,

	/** A tab that cannot be chosen, such as Storage away from a bank. */
	TAB_DISABLED,

	/** Text on a tab that is not selected. */
	TAB_TEXT,

	/** Text on the selected tab. */
	TAB_ACTIVE_TEXT,

	/** Text on a disabled tab; it is exempt from the contrast rule, since the control is off. */
	TAB_DISABLED_TEXT,

	/** A row, such as a party member. */
	ROW,

	/** The outline of a row. */
	ROW_BORDER,

	/** Text on a row. */
	ROW_TEXT,

	/** The lead row. */
	LEAD_ROW,

	/** The outline of the lead row. */
	LEAD_BORDER,

	/** Text on the lead row. */
	LEAD_TEXT,

	/** What sits behind a creature's picture. */
	PORTRAIT_BACKDROP,

	/** Main text on the window and on panel boxes. */
	TEXT,

	/** Quieter text on the window and on panel boxes. */
	SUBTEXT,

	/** A health bar with plenty left. */
	HP_FILL,

	/** A health bar getting low. */
	HP_WARNING,

	/** A health bar nearly empty. */
	HP_DANGER,

	/** The empty part of a health bar. */
	HP_TRACK,

	/** An experience bar. */
	XP_FILL,

	/** The Bag's next-jar bar, a different hue and lightness from the experience bar beside it. */
	JAR_FILL,

	/** The empty part of an experience bar. */
	XP_TRACK,

	/** The highlight colour: selection, links, the pulsing dot. */
	ACCENT,

	/** A boxed section on a page. */
	PANEL_BOX,

	/** The outline of a boxed section. */
	PANEL_BORDER,

	/** A tooltip. */
	TOOLTIP,

	/** Text in a tooltip. */
	TOOLTIP_TEXT,

	/** The pop-up notice. */
	NOTICE,

	/** Text in the pop-up notice. */
	NOTICE_TEXT,

	/** A chip, such as a style or a trait. */
	CHIP,

	/** Text on a chip. */
	CHIP_TEXT,

	/** A chip that stands out. */
	CHIP_ACCENT,

	/** Text on a chip that stands out. */
	CHIP_ACCENT_TEXT,

	/** The light laid over something the pointer is on. */
	HOVER_SHADE,

	/** The dark laid over something being pressed. */
	PRESS_SHADE,

	/** The drop shadow under raised parts. */
	SHADOW,

	/** The ring around what has keyboard focus. */
	FOCUS_RING,

	/** The field of a text box. */
	INPUT,

	/** The light laid over selected text and the highlighted item of a menu. */
	SELECTION,

	/** The dim laid over the window behind a modal. */
	SCRIM,

	/** The groove of a scroll bar. */
	SCROLL_TRACK,

	/** The handle of a scroll bar. */
	SCROLL_THUMB,

	/** Gold, for a gilded creature's frame and a new move's star. */
	GOLD
}
