/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * What a press on the Storage page does: move a creature to the team, open its Summary, tick it, rename it, use an
 * item on it, release it after a painted confirmation, or change the sort and filters through their menus and
 * chips. A release, from the list or from a creature's Summary, only reaches {@link PanelActions} from the Yes button
 * of a confirmation whose default button is No; a gilded, strong, exceptional or nicknamed creature asks a second
 * time. Before a confirmed release is sent each creature is checked
 * against the storage list as it is now, so a list that changed while the box was open releases nothing it should
 * not. Holds no RuneLite types.
 */
final class StoragePageFlow
{
	/** The button that goes ahead with a release. */
	static final String RELEASE_YES = "Yes, release";

	/** The button that keeps the creatures; it is the default. */
	static final String RELEASE_NO = "No";

	/** The title of the first confirmation. */
	static final String RELEASE_TITLE = "Release creature";

	/** The menu item that moves a creature to the team. */
	static final String MOVE_TO_TEAM = "Move to team";

	/** The menu item that renames a creature. */
	static final String RENAME = "Rename...";

	/** The menu item that opens the healing items. */
	static final String USE_ITEM = "Use item";

	/** The menu item that starts a release. */
	static final String RELEASE = "Release...";

	/** The menu item that starts carrying a creature to another place in the Box with the keys. */
	static final String MOVE_WITHIN = "Move within box";

	/** The menu item that puts a creature last in the Box. */
	static final String MOVE_TO_END = "Move to the end of the box";

	/** What the page needs from the window around it. */
	interface Host
	{
		/**
		 * Opens a context menu.
		 *
		 * @param items the items
		 * @param x where to open it
		 * @param y where to open it
		 * @param keyboard whether a key asked
		 */
		void menu(List<ContextMenu.Item> items, int x, int y, boolean keyboard);

		/**
		 * Opens a dialog.
		 *
		 * @param dialog the dialog
		 * @param box its text box, or null
		 */
		void modal(Modal dialog, TextBox box);

		/** @return whether the keys, not the pointer, are in charge, so a menu opens with its first item lit */
		default boolean keyboardLed()
		{
			return false;
		}

		/** @return the text kept from a Rename box the window closed on, or an empty text */
		String takeDraft();

		/** @return the stored creatures as the window shows them now */
		List<CreatureView> storage();

		/** @return how many of each healing item the player owns */
		Map<ItemType, Integer> items();

		/** @return how many creatures are on the team now, which Move to team needs room in */
		int teamSize();

		/**
		 * Opens the Summary page of a stored creature.
		 *
		 * @param place the creature's place in storage
		 */
		void summary(int place);
	}

	private final StoragePage page;
	private final PanelActions actions;
	private final Host host;
	private int anchorX;
	private int anchorY;

	/**
	 * Creates the flow.
	 *
	 * @param page the page whose Select mode and ticks it drives
	 * @param actions what a click does to the game
	 * @param host the window around the page
	 */
	StoragePageFlow(StoragePage page, PanelActions actions, Host host)
	{
		this.page = page;
		this.actions = actions;
		this.host = host;
	}

	/**
	 * A hit of the page was pressed.
	 *
	 * @param id the hit's id
	 */
	void press(String id)
	{
		if (pressControl(id))
			return;

		if (StoragePage.ID_SELECT.equals(id))
		{
			page.select(!page.selecting());
			return;
		}

		if (StoragePage.ID_RELEASE.equals(id))
		{
			releaseTicked();
			return;
		}

		int place = StoragePage.place(id);
		List<CreatureView> storage = host.storage();
		if (place < 0 || place >= storage.size())
			return;

		if (page.carrying() >= 0)
			putDown(place);
		else if (page.selecting())
			page.toggle(place);
		else
			actions.moveToTeam(pick(place, storage.get(place)));
	}

	/**
	 * The button that went down on the page came up.
	 *
	 * @param x the pointer's x in window pixels
	 * @param y the pointer's y in window pixels
	 * @return whether the press became a scroll or a drag, so the release is not a click
	 */
	boolean released(int x, int y)
	{
		boolean took = page.released(x, y);
		int[] drop = page.takeMove();
		if (drop != null)
			arrange(drop[0], drop[1]);

		return took;
	}

	private void putDown(int place)
	{
		int from = page.carrying();
		page.stopCarrying();
		if (from != place)
			arrange(from, place);
	}

	private void arrange(int from, int onto)
	{
		List<CreatureView> storage = host.storage();
		if (from < 0 || from >= storage.size())
			return;

		TradePick dragged = pick(from, storage.get(from));
		TradePick target = onto < 0 || onto >= storage.size() ? null : pick(onto, storage.get(onto));
		actions.arrangeStorage(dragged, target);
	}

	private boolean pressControl(String id)
	{
		List<CreatureView> storage = host.storage();
		StorageQuery query = page.query();
		if (StoragePage.ID_SORT.equals(id))
		{
			Rectangle b = page.sortBounds();
			host.menu(StorageMenus.sort(query, next -> page.setQuery(next, host.storage())), b.x, b.y + b.height,
				host.keyboardLed());
			return true;
		}

		if (StoragePage.ID_FILTER.equals(id))
		{
			Rectangle b = page.filterBounds();
			List<ContextMenu.Item> items = StorageMenus.filter(query, next -> page.setQuery(next, host.storage()),
				children -> host.menu(children, b.x, b.y + b.height, true));
			host.menu(items, b.x, b.y + b.height, host.keyboardLed());
			return true;
		}

		if (StoragePage.ID_VIEW.equals(id))
		{
			page.setMode(page.mode().next());
			return true;
		}

		int heading = StoragePage.group(id);
		if (heading >= 0)
		{
			if (page.selecting())
				page.toggleGroup(heading);

			return true;
		}

		if (StoragePage.ID_TICK_ALL.equals(id))
		{
			page.tickAllShown();
			return true;
		}

		if (StoragePage.ID_CLEAR.equals(id))
		{
			page.setQuery(query.cleared(), storage);
			return true;
		}

		int chip = StoragePage.chip(id);
		if (chip >= 0)
		{
			page.setQuery(query.flipped(chip), storage);
			return true;
		}

		int cross = StoragePage.chipRemove(id);
		if (cross >= 0)
		{
			page.setQuery(query.without(cross), storage);
			return true;
		}

		return false;
	}

	/**
	 * A row's menu was asked for. In Select mode a click does nothing and a key ticks the row, as the menu is not
	 * used there.
	 *
	 * @param id the row's id
	 * @param x where to open it
	 * @param y where to open it
	 * @param keyboard whether a key asked
	 */
	void menu(String id, int x, int y, boolean keyboard)
	{
		int place = StoragePage.place(id);
		List<CreatureView> storage = host.storage();
		if (place < 0 || place >= storage.size())
			return;

		if (page.carrying() >= 0)
		{
			putDown(place);
			return;
		}

		if (page.selecting())
		{
			if (keyboard)
				page.toggle(place);

			return;
		}

		anchor(x, y);
		CreatureView view = storage.get(place);
		List<ContextMenu.Item> items = new ArrayList<>(ContextMenuRules.stored(facts(view), commands(place, view)));
		if (page.arrangeAllowed())
		{
			items.add(ContextMenu.Item.of(MOVE_WITHIN, () -> page.carry(place)));
			items.add(ContextMenu.Item.of(MOVE_TO_END, () -> arrange(place, -1)));
		}

		host.menu(items, x, y, keyboard);
	}

	/**
	 * The menu of a stored creature's Summary page was asked for: the creature's menu without Summary in it.
	 *
	 * @param place the creature's place in storage
	 * @param x where to open it
	 * @param y where to open it
	 * @param keyboard whether a key asked
	 */
	void summaryMenu(int place, int x, int y, boolean keyboard)
	{
		List<CreatureView> storage = host.storage();
		if (place < 0 || place >= storage.size())
			return;

		anchor(x, y);
		host.menu(summaryItems(place, storage.get(place)), x, y, keyboard);
	}

	/**
	 * Lists the items of a stored creature's Summary page, for its menu and for the state of its buttons.
	 *
	 * @param place the creature's place in storage
	 * @param view the creature
	 * @return the items without Summary
	 */
	List<ContextMenu.Item> summaryItems(int place, CreatureView view)
	{
		return ContextMenuRules.storedSummary(facts(view), commands(place, view));
	}

	/**
	 * Remembers where the next menu or box a button opens should appear.
	 *
	 * @param x the pointer's x in window pixels
	 * @param y the pointer's y in window pixels
	 */
	void anchor(int x, int y)
	{
		anchorX = x;
		anchorY = y;
	}

	/**
	 * Starts a release of one stored creature, which asks first, with No as the default.
	 *
	 * @param place the creature's place in storage
	 * @param view the creature as the player sees it
	 */
	void release(int place, CreatureView view)
	{
		releaseOne(place, view);
	}

	private ContextMenuRules.Facts facts(CreatureView view)
	{
		Map<ItemType, Integer> owned = host.items() == null ? new EnumMap<>(ItemType.class) : host.items();
		return new ContextMenuRules.Facts(RowModel.of(view, -1), false, true, owned, host.teamSize());
	}

	/**
	 * Gives what each item and each Summary button of one stored creature does.
	 *
	 * @param place the creature's place in storage
	 * @param view the creature as the player sees it
	 * @return the commands
	 */
	ContextMenuRules.StoredCommands commands(int place, CreatureView view)
	{
		return new ContextMenuRules.StoredCommands()
		{
			@Override
			public void summary()
			{
				host.summary(place);
			}

			@Override
			public void moveToTeam()
			{
				actions.moveToTeam(pick(place, view));
			}

			@Override
			public void rename()
			{
				StoragePageFlow.this.rename(place, view);
			}

			@Override
			public void moves()
			{
				TradePick pick = pick(place, view);
				Modal ask = MovePicker.modal(view, forget -> actions.resolveMove(pick, forget), () ->
					host.menu(MovePicker.forgetItems(view, index -> actions.resolveMove(pick, index)), anchorX,
						anchorY, true));
				host.modal(ask, null);
			}

			@Override
			public void useItem()
			{
				host.menu(useItems(place, view), anchorX, anchorY, true);
			}

			@Override
			public void release()
			{
				releaseOne(place, view);
			}
		};
	}

	private List<ContextMenu.Item> useItems(int place, CreatureView view)
	{
		return ContextMenuRules.useItems(facts(view), type -> actions.useItem(pick(place, view), type));
	}

	private void rename(int place, CreatureView view)
	{
		TextBox box = new TextBox(Nickname.MAX_LENGTH, WindowPages::renameClean, TextBox.system());
		String draft = host.takeDraft();
		box.setText(draft.isEmpty() ? view.nickname() == null ? "" : view.nickname() : draft);
		box.placeholder(view.name());
		openRename(box, place, view);
	}

	private void openRename(TextBox box, int place, CreatureView view)
	{
		TradePick pick = pick(place, view);
		Modal ask = new Modal("Rename", "Name for your " + view.name() + " (1 to " + Nickname.MAX_LENGTH
			+ " letters, numbers, spaces or -). Leave it empty to go back to its species name.",
			Arrays.asList("Cancel", "Save"), 1, choice ->
			{
				if (choice != 1)
					return;

				String value = box.value();
				if (value == null)
					openRename(box, place, view);
				else
					actions.rename(pick, value);
			}, () ->
			{
			}).withField(box, 1);
		host.modal(ask, box);
	}

	private void releaseOne(int place, CreatureView view)
	{
		String message = "Release " + name(view) + " for good? Your lead gains " + ReleaseRules.xp(view)
			+ " XP. This cannot be undone.";
		confirm(RELEASE_TITLE, message, Collections.singletonList(place), Collections.singletonList(view));
	}

	private void releaseTicked()
	{
		List<CreatureView> storage = host.storage();
		List<Integer> places = new ArrayList<>();
		List<CreatureView> views = new ArrayList<>();
		long total = 0;
		StringBuilder names = new StringBuilder();
		TreeSet<Integer> visible = new TreeSet<>();
		for (int place : page.shownPlaces())
			visible.add(place);

		for (int place : page.ticked())
		{
			if (place >= storage.size() || !visible.contains(place))
				continue;

			places.add(place);
			views.add(storage.get(place));
			total += ReleaseRules.xp(storage.get(place));
			names.append('\n').append(name(storage.get(place))).append(" (Lv ").append(storage.get(place).level())
				.append(')');
		}

		if (places.isEmpty())
			return;

		confirm("Release creatures", "Release for good?" + names + "\n\nYour lead gains " + total
			+ " XP. This cannot be undone.", places, views);
	}

	private void confirm(String title, String message, List<Integer> places, List<CreatureView> views)
	{
		Modal ask = new Modal(title, message, Arrays.asList(RELEASE_NO, RELEASE_YES), 0, choice ->
		{
			if (choice == 1)
				askAgain(places, views);
		}, () ->
		{
		});
		host.modal(ask, null);
	}

	private void askAgain(List<Integer> places, List<CreatureView> views)
	{
		StringBuilder special = new StringBuilder();
		for (CreatureView view : views)
		{
			if (ReleaseRules.needsSecondConfirm(view))
				special.append('\n').append(name(view));
		}

		if (special.length() == 0)
		{
			send(places, views);
			return;
		}

		Modal ask = new Modal("Really release?", "These are special (gilded, rated Strong or Exceptional, or named)."
			+ " Really release them?" + special, Arrays.asList(RELEASE_NO, RELEASE_YES), 0, choice ->
			{
				if (choice == 1)
					send(places, views);
			}, () ->
			{
			});
		host.modal(ask, null);
	}

	/**
	 * Sends a confirmed release, for the creatures that are still where the player saw them.
	 *
	 * @param places where each was in storage
	 * @param views the creature each was
	 */
	private void send(List<Integer> places, List<CreatureView> views)
	{
		List<CreatureView> now = host.storage();
		List<TradePick> picks = new ArrayList<>();
		for (int i = 0; i < places.size(); i++)
		{
			int place = places.get(i);
			if (place < now.size() && now.get(place).sameCreature(views.get(i)))
				picks.add(pick(place, now.get(place)));
		}

		if (picks.isEmpty())
			return;

		if (picks.size() == 1 && !page.selecting())
			actions.release(picks.get(0));
		else
			actions.releaseMany(picks);

		page.select(false);
	}

	private static TradePick pick(int place, CreatureView view)
	{
		return TradePick.of(Holding.STORAGE, place, view);
	}

	private static String name(CreatureView view)
	{
		String shown = view.nickname() == null ? view.name() : view.nickname();
		return view.gilded() ? "Gilded " + shown : shown;
	}
}
