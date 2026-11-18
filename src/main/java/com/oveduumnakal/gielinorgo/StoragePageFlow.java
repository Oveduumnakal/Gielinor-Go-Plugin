/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * What a press on the Storage page does: move a creature to the team, tick it, rename it, use an item on it, or
 * release it after a painted confirmation. A release only reaches {@link PanelActions} from the Yes button of a
 * confirmation whose default button is No; a gilded, strong, exceptional or nicknamed creature asks a second time.
 * Before a confirmed release is sent each creature is checked against the storage list as it is now, so a list that
 * changed while the box was open releases nothing it should not. Holds no RuneLite types.
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

	/** The menu item that revives a fainted creature for free; the Storage tab only opens where that is allowed. */
	static final String REVIVE = ContextMenuRules.REVIVE;

	/** The menu item that renames a creature. */
	static final String RENAME = "Rename...";

	/** The menu item that opens the healing items. */
	static final String USE_ITEM = "Use item";

	/** The menu item that starts a release. */
	static final String RELEASE = "Release...";

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

		/** @return the text kept from a Rename box the window closed on, or an empty text */
		String takeDraft();

		/** @return the stored creatures as the window shows them now */
		List<CreatureView> storage();

		/** @return how many of each healing item the player owns */
		Map<ItemType, Integer> items();
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

		if (page.selecting())
			page.toggle(place);
		else
			actions.moveToTeam(pick(place, storage.get(place)));
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

		if (page.selecting())
		{
			if (keyboard)
				page.toggle(place);

			return;
		}

		anchorX = x;
		anchorY = y;
		CreatureView view = storage.get(place);
		List<ContextMenu.Item> items = new ArrayList<>();
		items.add(ContextMenu.Item.of(MOVE_TO_TEAM, () -> actions.moveToTeam(pick(place, view))));
		if (view.fainted())
			items.add(ContextMenu.Item.of(REVIVE, () -> actions.reviveAtBank(pick(place, view))));

		items.add(ContextMenu.Item.of(RENAME, () -> rename(place, view)));
		items.add(useItem(place, view));
		items.add(ContextMenu.Item.of(RELEASE, () -> releaseOne(place, view)));
		host.menu(items, x, y, keyboard);
	}

	private ContextMenu.Item useItem(int place, CreatureView view)
	{
		List<ContextMenu.Item> uses = useItems(place, view);
		if (uses.isEmpty())
			return ContextMenu.Item.disabled(USE_ITEM, ContextMenuRules.NO_ITEM_REASON);

		return ContextMenu.Item.of(USE_ITEM, () -> host.menu(useItems(place, view), anchorX, anchorY, true));
	}

	private List<ContextMenu.Item> useItems(int place, CreatureView view)
	{
		Map<ItemType, Integer> owned = host.items() == null ? new EnumMap<>(ItemType.class) : host.items();
		RowModel row = RowModel.of(view, -1);
		ContextMenuRules.Facts facts = new ContextMenuRules.Facts(row, false, true, owned);
		return ContextMenuRules.useItems(facts, type -> actions.useItem(pick(place, view), type));
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
		for (int place : page.ticked())
		{
			if (place >= storage.size())
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
