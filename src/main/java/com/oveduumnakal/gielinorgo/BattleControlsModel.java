/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * What the painted battle controls know and do, without any drawing: which page of buttons shows, where each button
 * sits, which are enabled, what a click on one chooses, and what the pointer is over. It holds the same rules the side
 * panel's battle box has: Attack opens the creature's moves only when it knows more than one, Switch and Item open
 * lists, an item that helps one creature is used on it at once, and a forced switch leaves only Switch. Every choice
 * goes to one consumer, and a choice closes the lists. All coordinates are pixels from the box's top-left corner. The
 * model is used on the game thread only and holds no RuneLite types.
 */
final class BattleControlsModel
{
	/** The id of the Attack button. */
	static final String ID_ATTACK = "attack";

	/** The id of the Switch button. */
	static final String ID_SWITCH = "switch";

	/** The id of the Catch button. */
	static final String ID_CATCH = "catch";

	/** The id of the Item button. */
	static final String ID_ITEM = "item";

	/** The id of the Run button. */
	static final String ID_RUN = "run";

	/** The id of the Back button of a list. */
	static final String ID_BACK = "back";

	/** The start of the id of a move in the move list, followed by its place. */
	static final String MOVE_PREFIX = "move:";

	/** The start of the id of a creature in the switch list, followed by its place. */
	static final String SWITCH_PREFIX = "switch:";

	/** The start of the id of an item in the item list, followed by its place. */
	static final String ITEM_PREFIX = "item:";

	/** The start of the id of a creature in the list an item is used on, followed by its place. */
	static final String TARGET_PREFIX = "target:";

	private static final double WIDTH_UNITS = 360;
	private static final double LINE_UNITS = 32;
	private static final double NAME_UNITS = 28;
	private static final double BUTTON_UNITS = 40;
	private static final double GAP_UNITS = 8;
	private static final double HINT_UNITS = 24;
	private static final int MIN_BAR = 3;

	/** The pages of buttons. */
	enum Page
	{
		/** The five choices. */
		MAIN,

		/** The creature's moves. */
		MOVES,

		/** The creatures that could step in. */
		SWITCH,

		/** The items that would do something. */
		ITEMS,

		/** The creatures one item could be used on. */
		TARGETS
	}

	/** The hotkeys the player set for each choice; a null key is not set and shows nothing. */
	static final class Keys
	{
		/** No hotkey is set. */
		static final Keys NONE = new Keys(null, null, null, null, null);

		private final String attack;
		private final String swap;
		private final String jar;
		private final String item;
		private final String run;

		/**
		 * Makes the set of hotkeys.
		 *
		 * @param attack the Attack key, or null
		 * @param swap the Switch key, or null
		 * @param jar the Catch key, or null
		 * @param item the Item key, or null
		 * @param run the Run key, or null
		 */
		Keys(String attack, String swap, String jar, String item, String run)
		{
			this.attack = attack;
			this.swap = swap;
			this.jar = jar;
			this.item = item;
			this.run = run;
		}

		private String of(String id)
		{
			switch (id)
			{
				case ID_ATTACK:
					return attack;
				case ID_SWITCH:
					return swap;
				case ID_CATCH:
					return jar;
				case ID_ITEM:
					return item;
				case ID_RUN:
					return run;
				default:
					return null;
			}
		}
	}

	/** One button: what it says, where it is and whether it can be pressed. */
	static final class Button
	{
		private final String id;
		private final String label;
		private final String tip;
		private final boolean enabled;
		private final Rectangle bounds;

		Button(String id, String label, String tip, boolean enabled, Rectangle bounds)
		{
			this.id = id;
			this.label = label;
			this.tip = tip == null ? "" : tip;
			this.enabled = enabled;
			this.bounds = new Rectangle(bounds);
		}

		/** @return the stable id */
		String id()
		{
			return id;
		}

		/** @return the text, with the hotkey when one is set */
		String label()
		{
			return label;
		}

		/** @return why it is off, or what it does; empty for nothing to say */
		String tip()
		{
			return tip;
		}

		/** @return whether it can be pressed */
		boolean enabled()
		{
			return enabled;
		}

		/** @return a copy of the button's box */
		Rectangle bounds()
		{
			return new Rectangle(bounds);
		}
	}

	/** Where every part of the box sits for one page at one scale. */
	static final class Layout
	{
		/** The size of the whole box. */
		final Dimension size;

		/** The title line. */
		final Rectangle title;

		/** The line with the player's creature, empty on a list page. */
		final Rectangle mineName;

		/** The player's health bar, empty on a list page. */
		final Rectangle mineBar;

		/** The line with the opposing creature, empty on a list page. */
		final Rectangle foeName;

		/** The opposing health bar, empty on a list page. */
		final Rectangle foeBar;

		/** The time bar. */
		final Rectangle timeBar;

		/** The line of words about the button under the pointer, empty on a list page. */
		final Rectangle hint;

		/** The buttons in paint order. */
		final List<Button> buttons;

		Layout(Dimension size, Rectangle[] parts, List<Button> buttons)
		{
			this.size = size;
			this.title = parts[0];
			this.mineName = parts[1];
			this.mineBar = parts[2];
			this.foeName = parts[3];
			this.foeBar = parts[4];
			this.timeBar = parts[5];
			this.hint = parts[6];
			this.buttons = Collections.unmodifiableList(buttons);
		}
	}

	private final Consumer<BattleChoice> choose;
	private BattleView view;
	private Keys keys = Keys.NONE;
	private Page page = Page.MAIN;
	private BattleView.ItemOption pending;
	private List<Hit> hits = Collections.emptyList();
	private String hover;
	private String pressed;

	/**
	 * Makes the model, showing nothing.
	 *
	 * @param choose called with each choice the player makes, once per click
	 */
	BattleControlsModel(Consumer<BattleChoice> choose)
	{
		this.choose = choose;
	}

	/**
	 * Shows the battle that waits, or hides the controls. A list that no longer makes sense for the new state, such as
	 * the move list of a creature that was switched out, closes.
	 *
	 * @param next the battle, or null when none waits
	 * @param hotkeys the hotkeys the player set
	 */
	void show(BattleView next, Keys hotkeys)
	{
		view = next;
		keys = hotkeys == null ? Keys.NONE : hotkeys;
		if (next == null)
		{
			reset();
			return;
		}

		if (!valid(page))
			page = Page.MAIN;

		if (page == Page.TARGETS)
		{
			pending = find(pending);
			if (pending == null)
				page = Page.ITEMS;
		}
	}

	/** @return whether the controls are shown */
	boolean visible()
	{
		return view != null;
	}

	/** @return the page of buttons that shows */
	Page page()
	{
		return page;
	}

	/** @return the id of the button under the pointer, or null */
	String hover()
	{
		return hover;
	}

	/** @return the id of the button that is held down, or null */
	String pressed()
	{
		return pressed;
	}

	/** @return the hits of the last layout, for the input listener; empty while nothing is shown */
	List<Hit> hits()
	{
		return hits;
	}

	/** @return the title, such as {@code Your turn · 7s} */
	String title()
	{
		if (view == null)
			return "";

		if (page == Page.MAIN || view.waitingForFriend() || view.forcedSwitch())
			return BattlePrompt.title(view);

		String time = " · " + view.secondsLeft() + "s";
		switch (page)
		{
			case MOVES:
				return "Choose a move" + time;
			case SWITCH:
				return "Choose a creature" + time;
			case ITEMS:
				return "Choose an item" + time;
			default:
				return "Use it on which creature?" + time;
		}
	}

	/** @return the line about the player's creature */
	String mineLine()
	{
		return view == null ? "" : BattlePrompt.mineLine(view);
	}

	/** @return the line about the opposing creature */
	String foeLine()
	{
		return view == null ? "" : BattlePrompt.foeLine(view);
	}

	/** @return the player's creature's health from 0 to 1 */
	double mineHp()
	{
		return view == null ? 0 : view.mineHp();
	}

	/** @return the opposing creature's health from 0 to 1 */
	double foeHp()
	{
		return view == null ? 0 : view.foeHp();
	}

	/** @return the share of the turn that is left, from 0 to 1 */
	double timeLeft()
	{
		return view == null ? 0 : view.timeLeft();
	}

	/** @return the words about the button under the pointer: why it is off, or what it does */
	String hint()
	{
		if (hover == null)
			return "";

		for (Hit hit : hits)
			if (hit.id().equals(hover))
				return hit.tip();

		return "";
	}

	/**
	 * Works out where everything sits and publishes the hits. Painting calls it each frame.
	 *
	 * @param style the style, which has the scale
	 * @return the layout of the page that shows
	 */
	Layout layout(WindowStyle style)
	{
		if (view == null)
		{
			hits = Collections.emptyList();
			return new Layout(new Dimension(), empty(7), new ArrayList<>());
		}

		Layout made = page == Page.MAIN ? mainLayout(style) : listLayout(style);
		List<Hit> list = new ArrayList<>();
		for (Button button : made.buttons)
			list.add(new Hit(button.id, Hit.Kind.BUTTON, button.bounds, -1, button.enabled, button.tip));

		hits = Collections.unmodifiableList(list);
		return made;
	}

	private Layout mainLayout(WindowStyle style)
	{
		int pad = style.metrics().pad;
		int width = style.px(WIDTH_UNITS);
		int inner = width - 2 * pad;
		int bar = Math.max(MIN_BAR, style.metrics().bar);
		int line = style.px(LINE_UNITS);
		int name = style.px(NAME_UNITS);
		int gap = style.px(GAP_UNITS);
		int y = pad;
		Rectangle[] parts = empty(7);
		parts[0] = new Rectangle(pad, y, inner, line);
		y += line;
		parts[1] = new Rectangle(pad, y, inner, name);
		y += name;
		parts[2] = new Rectangle(pad, y, inner, bar);
		y += bar + gap;
		parts[3] = new Rectangle(pad, y, inner, name);
		y += name;
		parts[4] = new Rectangle(pad, y, inner, bar);
		y += bar + gap;
		parts[5] = new Rectangle(pad, y, inner, Math.max(MIN_BAR, bar / 2));
		y += parts[5].height + gap + gap / 2;
		int buttonW = (inner - gap) / 2;
		int buttonH = style.px(BUTTON_UNITS);
		List<Button> buttons = new ArrayList<>();
		String[] ids = {ID_ATTACK, ID_SWITCH, ID_CATCH, ID_ITEM, ID_RUN};
		for (int i = 0; i < ids.length; i++)
		{
			Rectangle box = new Rectangle(pad + (i % 2) * (buttonW + gap), y + (i / 2) * (buttonH + gap), buttonW,
				buttonH);
			buttons.add(mainButton(ids[i], box));
		}

		y += 3 * buttonH + 2 * gap + gap;
		parts[6] = new Rectangle(pad, y, inner, style.px(HINT_UNITS));
		y += parts[6].height + pad;
		return new Layout(new Dimension(width, y), parts, buttons);
	}

	private Layout listLayout(WindowStyle style)
	{
		int pad = style.metrics().pad;
		int width = style.px(WIDTH_UNITS);
		int inner = width - 2 * pad;
		int line = style.px(LINE_UNITS);
		int gap = style.px(GAP_UNITS);
		int buttonH = style.px(BUTTON_UNITS);
		int y = pad;
		Rectangle[] parts = empty(7);
		parts[0] = new Rectangle(pad, y, inner, line);
		y += line;
		parts[5] = new Rectangle(pad, y, inner, Math.max(MIN_BAR, style.metrics().bar / 2));
		y += parts[5].height + gap;
		List<Button> buttons = new ArrayList<>();
		List<String[]> rows = rows();
		for (String[] row : rows)
		{
			buttons.add(new Button(row[0], row[1], row[2], true, new Rectangle(pad, y, inner, buttonH)));
			y += buttonH + gap;
		}

		buttons.add(new Button(ID_BACK, "Back", "", true, new Rectangle(pad, y, inner, buttonH)));
		y += buttonH + pad;
		return new Layout(new Dimension(width, y), parts, buttons);
	}

	private Button mainButton(String id, Rectangle box)
	{
		boolean waiting = view.waitingForFriend();
		boolean friend = view.friendName() != null;
		boolean enabled;
		String text;
		String tip;
		switch (id)
		{
			case ID_ATTACK:
				enabled = !view.forcedSwitch() && !waiting;
				text = view.canChooseMove() ? "Attack..." : "Attack";
				tip = view.canChooseMove() ? "Choose one of your creature's moves" : "";
				break;
			case ID_SWITCH:
				enabled = view.canSwitch() && !waiting;
				text = "Switch";
				tip = view.canSwitch() ? "Choose a creature to step in. It costs your turn."
					: "No other creature can fight";
				break;
			case ID_CATCH:
				enabled = view.canCatch();
				text = view.jarLabel() == null ? "Catch" : "Catch x" + view.jarCount();
				tip = view.jarLabel() == null ? "No jars left" : "Throw a " + view.jarLabel()
					+ " jar. The more health it has lost, the better the chance. It costs your turn.";
				break;
			case ID_ITEM:
				enabled = view.canUseItem() && !waiting;
				text = "Item";
				tip = view.canUseItem() ? "Use a healing item. It costs your turn." : "No item would help right now";
				break;
			default:
				enabled = !view.forcedSwitch() || friend;
				text = "Run";
				tip = friend ? "Leave the battle. Nothing is won or lost." : runTip(view.fleeChance());
				break;
		}

		String key = keys.of(id);
		String label = key == null ? text : text + " (" + key + ")";
		return new Button(id, label, tip, enabled, box);
	}

	private List<String[]> rows()
	{
		List<String[]> rows = new ArrayList<>();
		if (page == Page.MOVES)
		{
			for (int i = 0; i < view.moves().size(); i++)
			{
				Move move = view.moves().get(i);
				String text = move.plain() ? "Attack" : move.name() + " · " + move.describe();
				rows.add(new String[] {MOVE_PREFIX + i, text, ""});
			}
		}
		else if (page == Page.SWITCH)
		{
			for (int i = 0; i < view.options().size(); i++)
				rows.add(new String[] {SWITCH_PREFIX + i, optionText(view.options().get(i)), ""});
		}
		else if (page == Page.ITEMS)
		{
			for (int i = 0; i < view.items().size(); i++)
			{
				BattleView.ItemOption option = view.items().get(i);
				rows.add(new String[] {ITEM_PREFIX + i, option.type().label() + "  x" + option.count(), ""});
			}
		}
		else if (pending != null)
		{
			for (int i = 0; i < pending.targets().size(); i++)
				rows.add(new String[] {TARGET_PREFIX + i, optionText(pending.targets().get(i)), ""});
		}

		return rows;
	}

	private static String optionText(BattleView.Option option)
	{
		return option.name() + " Lv " + option.level() + " · " + Math.round(option.hpFraction() * 100) + "%";
	}

	/**
	 * The pointer moved over the box.
	 *
	 * @param x the pointer's x in box pixels
	 * @param y the pointer's y in box pixels
	 */
	void onMoved(int x, int y)
	{
		Hit hit = hitAt(x, y);
		hover = hit == null ? null : hit.id();
	}

	/** The pointer left the box. */
	void onLeft()
	{
		hover = null;
	}

	/**
	 * A mouse button went down inside the box.
	 *
	 * @param x the pointer's x in box pixels
	 * @param y the pointer's y in box pixels
	 * @param button the button, as {@link MouseEvent#getButton}
	 */
	void onPressed(int x, int y, int button)
	{
		Hit hit = hitAt(x, y);
		pressed = hit != null && hit.enabled() && button == MouseEvent.BUTTON1 ? hit.id() : null;
	}

	/**
	 * A mouse button that went down inside the box came up. A left click presses the enabled button it went down and
	 * came up on; a right click on a list goes back.
	 *
	 * @param x the pointer's x in box pixels
	 * @param y the pointer's y in box pixels
	 * @param button the button, as {@link MouseEvent#getButton}
	 */
	void onReleased(int x, int y, int button)
	{
		String down = pressed;
		pressed = null;
		if (view == null)
			return;

		if (button == MouseEvent.BUTTON3 && page != Page.MAIN)
		{
			back();
			return;
		}

		Hit hit = hitAt(x, y);
		if (button == MouseEvent.BUTTON1 && hit != null && hit.enabled() && hit.id().equals(down))
			perform(hit.id());
	}

	private void perform(String id)
	{
		switch (id)
		{
			case ID_ATTACK:
				if (view.moves().size() <= 1)
					send(BattleChoice.attack());
				else
					page = Page.MOVES;

				return;
			case ID_SWITCH:
				page = Page.SWITCH;
				return;
			case ID_ITEM:
				page = Page.ITEMS;
				return;
			case ID_CATCH:
				send(BattleChoice.catching(null));
				return;
			case ID_RUN:
				send(BattleChoice.run());
				return;
			case ID_BACK:
				back();
				return;
			default:
				performListed(id);
		}
	}

	private void performListed(String id)
	{
		if (id.startsWith(MOVE_PREFIX))
			send(BattleChoice.attack(place(id, MOVE_PREFIX)));
		else if (id.startsWith(SWITCH_PREFIX))
			send(BattleChoice.switchTo(teamIndex(view.options(), place(id, SWITCH_PREFIX))));
		else if (id.startsWith(ITEM_PREFIX))
			chooseItem(view.items().get(place(id, ITEM_PREFIX)));
		else if (id.startsWith(TARGET_PREFIX) && pending != null)
			send(BattleChoice.item(pending.type(), teamIndex(pending.targets(), place(id, TARGET_PREFIX))));
	}

	private void chooseItem(BattleView.ItemOption option)
	{
		if (option.targets().size() == 1)
		{
			send(BattleChoice.item(option.type(), teamIndex(option.targets(), 0)));
			return;
		}

		pending = option;
		page = Page.TARGETS;
	}

	private void back()
	{
		page = page == Page.TARGETS ? Page.ITEMS : Page.MAIN;
		pending = null;
	}

	private void send(BattleChoice choice)
	{
		page = Page.MAIN;
		pending = null;
		hover = null;
		choose.accept(choice);
	}

	private void reset()
	{
		page = Page.MAIN;
		pending = null;
		hover = null;
		pressed = null;
		hits = Collections.emptyList();
	}

	private boolean valid(Page shown)
	{
		switch (shown)
		{
			case MOVES:
				return view.moves().size() > 1 && !view.forcedSwitch() && !view.waitingForFriend();
			case SWITCH:
				return view.canSwitch() && !view.options().isEmpty() && !view.waitingForFriend();
			case ITEMS:
			case TARGETS:
				return view.canUseItem() && !view.items().isEmpty() && !view.waitingForFriend();
			default:
				return true;
		}
	}

	private BattleView.ItemOption find(BattleView.ItemOption old)
	{
		if (old == null)
			return null;

		for (BattleView.ItemOption option : view.items())
			if (option.type() == old.type() && !option.targets().isEmpty())
				return option;

		return null;
	}

	private Hit hitAt(int x, int y)
	{
		for (int i = hits.size() - 1; i >= 0; i--)
			if (hits.get(i).contains(x, y))
				return hits.get(i);

		return null;
	}

	private static int teamIndex(List<BattleView.Option> list, int place)
	{
		return list.get(place).teamIndex();
	}

	private static int place(String id, String prefix)
	{
		return Integer.parseInt(id.substring(prefix.length()));
	}

	private static Rectangle[] empty(int count)
	{
		Rectangle[] parts = new Rectangle[count];
		for (int i = 0; i < count; i++)
			parts[i] = new Rectangle();

		return parts;
	}

	/** @return what the Run button says when pointed at: the odds, or nothing when it cannot fail */
	static String runTip(double chance)
	{
		return chance >= 1.0 ? ""
			: Math.round(chance * 100) + "% chance to get away. A failed try costs your turn.";
	}
}
