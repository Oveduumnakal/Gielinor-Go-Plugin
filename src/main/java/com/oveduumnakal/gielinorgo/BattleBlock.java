/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * One piece of the Battle page's scrolling column: a section heading, a note under it, or a card. A card has a title
 * with a short status beside it, a stripe down its left edge, lines of text and rows of buttons. The blocks hold only
 * words, colours and hit ids; {@link BattleLayout} places them and {@link BattlePage} paints them. Immutable. Holds no
 * RuneLite types.
 */
final class BattleBlock
{
	/** What a block is. */
	enum Kind
	{
		/** A section heading in small capitals. */
		HEADING,

		/** A line or two of quiet text. */
		NOTE,

		/** A card with a title, lines and buttons. */
		CARD
	}

	/** Which sigil badge a block shows. */
	enum Sigil
	{
		/** No badge. */
		NONE,

		/** The plain sigil of a beaten champion. */
		NORMAL,

		/** The gold sigil of a rematch win. */
		GOLD
	}

	/** One button on a card. */
	static final class Btn
	{
		private final String id;
		private final String label;
		private final boolean enabled;
		private final String tip;

		/**
		 * Makes a button.
		 *
		 * @param id the hit id
		 * @param label the words on it
		 * @param enabled whether it can be pressed; a disabled one still takes the pointer
		 * @param tip the tooltip, or empty
		 */
		Btn(String id, String label, boolean enabled, String tip)
		{
			this.id = id;
			this.label = label;
			this.enabled = enabled;
			this.tip = tip;
		}

		/** @return the hit id */
		String id()
		{
			return id;
		}

		/** @return the words on the button */
		String label()
		{
			return label;
		}

		/** @return whether it can be pressed */
		boolean enabled()
		{
			return enabled;
		}

		/** @return the tooltip, or an empty text */
		String tip()
		{
			return tip;
		}
	}

	/** One line (or wrapped paragraph) of a card, or one row of buttons. */
	static final class Item
	{
		private final String text;
		private final ThemeColor color;
		private final boolean small;
		private final List<Btn> buttons;

		private Item(String text, ThemeColor color, boolean small, List<Btn> buttons)
		{
			this.text = text;
			this.color = color;
			this.small = small;
			this.buttons = buttons;
		}

		/**
		 * @param text the words; they wrap
		 * @param color the colour
		 * @param small whether to use the small font
		 * @return a text item
		 */
		static Item text(String text, ThemeColor color, boolean small)
		{
			return new Item(text, color, small, Collections.emptyList());
		}

		/**
		 * @param buttons the buttons, side by side
		 * @return a row of buttons
		 */
		static Item buttons(Btn... buttons)
		{
			return new Item("", ThemeColor.TEXT, false, Collections.unmodifiableList(Arrays.asList(buttons)));
		}

		/** @return the words, or an empty text for a row of buttons */
		String text()
		{
			return text;
		}

		/** @return the colour of the words */
		ThemeColor color()
		{
			return color;
		}

		/** @return whether the words use the small font */
		boolean small()
		{
			return small;
		}

		/** @return the buttons of a button row, or an empty list for a text item */
		List<Btn> buttons()
		{
			return buttons;
		}

		/** @return whether this is a row of buttons */
		boolean isButtons()
		{
			return !buttons.isEmpty();
		}
	}

	private final Kind kind;
	private final String text;
	private final String status;
	private final ThemeColor statusColor;
	private final ThemeColor stripe;
	private final String tip;
	private final String id;
	private final List<Item> items;
	private final Sigil sigil;

	private BattleBlock(Kind kind, String text, String status, ThemeColor statusColor, ThemeColor stripe, String tip,
		String id, List<Item> items)
	{
		this(kind, text, status, statusColor, stripe, tip, id, items, Sigil.NONE);
	}

	private BattleBlock(Kind kind, String text, String status, ThemeColor statusColor, ThemeColor stripe, String tip,
		String id, List<Item> items, Sigil sigil)
	{
		this.kind = kind;
		this.text = text;
		this.status = status;
		this.statusColor = statusColor;
		this.stripe = stripe;
		this.tip = tip;
		this.id = id;
		this.items = Collections.unmodifiableList(new ArrayList<>(items));
		this.sigil = sigil;
	}

	/**
	 * @param badge the sigil badge to show beside the block's words
	 * @return a copy of this block that shows the badge
	 */
	BattleBlock withSigil(Sigil badge)
	{
		return new BattleBlock(kind, text, status, statusColor, stripe, tip, id, items, badge);
	}

	/** @return the sigil badge the block shows, or {@link Sigil#NONE} */
	Sigil sigil()
	{
		return sigil;
	}

	/**
	 * @param text the heading
	 * @return a heading block
	 */
	static BattleBlock heading(String text)
	{
		return new BattleBlock(Kind.HEADING, text, "", ThemeColor.SUBTEXT, null, "", "", Collections.emptyList());
	}

	/**
	 * @param text the note
	 * @return a note block
	 */
	static BattleBlock note(String text)
	{
		return new BattleBlock(Kind.NOTE, text, "", ThemeColor.SUBTEXT, null, "", "", Collections.emptyList());
	}

	/**
	 * Makes a card.
	 *
	 * @param id the id of the card's own hit, which carries its tooltip
	 * @param title the bold title
	 * @param status the short word beside the title, or empty
	 * @param statusColor the colour of the status
	 * @param stripe the colour of the stripe on the left edge
	 * @param tip the tooltip of the card, or empty
	 * @param items the lines and button rows
	 * @return a card block
	 */
	static BattleBlock card(String id, String title, String status, ThemeColor statusColor, ThemeColor stripe,
		String tip, List<Item> items)
	{
		return new BattleBlock(Kind.CARD, title, status, statusColor, stripe, tip, id, items);
	}

	/** @return what kind of block this is */
	Kind kind()
	{
		return kind;
	}

	/** @return the heading, the note or the card's title */
	String text()
	{
		return text;
	}

	/** @return the card's status word, or an empty text */
	String status()
	{
		return status;
	}

	/** @return the colour of the status word */
	ThemeColor statusColor()
	{
		return statusColor;
	}

	/** @return the colour of the card's stripe */
	ThemeColor stripe()
	{
		return stripe;
	}

	/** @return the card's tooltip, or an empty text */
	String tip()
	{
		return tip;
	}

	/** @return the id of the card's own hit */
	String id()
	{
		return id;
	}

	/** @return the card's lines and button rows */
	List<Item> items()
	{
		return items;
	}
}
