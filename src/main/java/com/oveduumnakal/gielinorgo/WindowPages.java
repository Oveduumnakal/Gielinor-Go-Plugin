/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * The pages of the painted window that exist so far: the Party tab with its list and its Summary. It plugs into the
 * shell as the {@link WindowShell.PageActions}, declares each page's hits, paints through {@link PartyPainter} and
 * {@link SummaryPainter}, and turns a click or a key into a change of page or one of the old panel's actions
 * ({@link PanelActions}), which the plugin carries over to the game thread. The shell and everything here run on the
 * game thread and paint from one immutable {@link WindowSnapshot}; nothing changes the game directly. Left click on a
 * row opens the Summary, a right click or Enter opens the row's menu (Summary first); on the Summary Up and Down go to
 * the neighbouring creature and Esc or Backspace go back to the list. The other tabs are not here yet, so for them
 * every method does nothing and the shell paints its placeholder. Holds no RuneLite types.
 */
final class WindowPages implements WindowShell.PageActions
{
	/** The start of the id of a Party row's hit; the place on the team follows. */
	static final String ROW_PREFIX = "row:";

	/** The id of the Open Storage button of an empty team. */
	static final String ID_STORAGE = "btn:openStorage";

	/** The id of the banner's Reset button. */
	static final String ID_RESET_PROGRESS = "btn:resetProgress";

	/** The id of the banner's Dismiss button. */
	static final String ID_DISMISS = "btn:dismissBanner";

	/** The id of the Summary's portrait and name area, which opens the creature's menu. */
	static final String ID_SUMMARY_MENU = "summary:menu";

	/** The id of the Summary's moves box. */
	static final String ID_MOVES = "summary:moves";

	/** The title of the reset confirmation. */
	static final String RESET_TITLE = "Reset progress";

	/** The button that goes ahead with a reset. */
	static final String RESET_YES = "Yes, reset";

	/** The button that keeps the save. */
	static final String RESET_NO = "No";

	private static final String STARTER_PREFIX = "starter:";
	private static final String SUMMARY_BUTTON_PREFIX = "summary:btn:";
	private static final Map<Font, Integer> HEIGHTS = new HashMap<>();

	private final WindowShell shell;
	private final PanelActions actions;
	private final PortraitCache pictures;
	private final LongSupplier clock;
	private final PageState state = new PageState();
	private final PartyMotion motion;
	private final PartyPainter party;
	private final SummaryPainter summary;
	private final BagPainter bag;
	private final TasksPage tasks;
	private final CollectionPage collection;
	private final StoragePage storage;
	private final StoragePageFlow storageFlow;
	private final BattlePage battle;
	private WindowStyle style;
	private MotionMode mode = MotionMode.FULL;
	private WindowTab tab = WindowTab.PARTY;
	private boolean enter;
	private boolean bannerDismissed;
	private int anchorX;
	private int anchorY;
	private CreatureView shown;

	/**
	 * Creates the pages.
	 *
	 * @param shell the window shell, which opens menus and dialogs for the pages and gets their hits
	 * @param actions what a click does to the game, carried over to the game thread by the plugin
	 * @param pictures where creature pictures are
	 * @param clock the time in milliseconds
	 * @param sink hears every string a page draws, for tests; {@link PageText.Sink#NONE} otherwise
	 */
	WindowPages(WindowShell shell, PanelActions actions, PortraitCache pictures, LongSupplier clock,
		PageText.Sink sink)
	{
		this.shell = shell;
		this.actions = actions;
		this.pictures = pictures;
		this.clock = clock;
		this.motion = new PartyMotion(shell.animations());
		this.party = new PartyPainter(pictures, motion, sink);
		this.summary = new SummaryPainter(pictures, sink);
		this.bag = new BagPainter(sink);
		this.tasks = new TasksPage(sink);
		this.collection = new CollectionPage(sink);
		this.storage = new StoragePage(pictures, sink);
		this.storageFlow = new StoragePageFlow(storage, actions, new StorageHost());
		this.battle = new BattlePage(shell, actions, clock, sink);
	}

	/**
	 * @param place a place on the team
	 * @return the id of that row's hit
	 */
	static String rowId(int place)
	{
		return ROW_PREFIX + place;
	}

	/**
	 * @param id a hit's id
	 * @return the place on the team it names, or -1 when it is not a row's id
	 */
	static int rowIndex(String id)
	{
		if (id == null || !id.startsWith(ROW_PREFIX))
			return -1;

		try
		{
			return Integer.parseInt(id.substring(ROW_PREFIX.length()));
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}

	/**
	 * @param place a starter's place in the offer
	 * @return the id of that starter's hit
	 */
	static String starterId(int place)
	{
		return STARTER_PREFIX + place;
	}

	/**
	 * @param place a button's place in {@link SummaryLayout#BUTTONS}
	 * @return the id of that button's hit
	 */
	static String summaryButtonId(int place)
	{
		return SUMMARY_BUTTON_PREFIX + place;
	}

	/** @return which page of the Party tab is shown and which creature the Summary is on */
	PageState pageState()
	{
		return state;
	}

	/** @return the Tasks page, whose milestone list the tests drive */
	TasksPage tasksPage()
	{
		return tasks;
	}

	/** @return the Collection page, whose species list the tests drive */
	CollectionPage collectionPage()
	{
		return collection;
	}

	/** @return the Storage page, whose list and ticks the tests drive */
	StoragePage storagePage()
	{
		return storage;
	}

	/** @return the Battle page, whose scrolling column the tests drive */
	BattlePage battlePage()
	{
		return battle;
	}

	/** @return the Party page's motion, for tests */
	PartyMotion motion()
	{
		return motion;
	}

	@Override
	public void frame(WindowStyle current, MotionMode motionMode, long nowMs)
	{
		style = current;
		mode = motionMode;
	}

	@Override
	public void opened(long nowMs)
	{
		enter = true;
	}

	@Override
	public void closed()
	{
		motion.reset();
		storage.reset();
		state.back();
		enter = false;
		tab = WindowTab.PARTY;
	}

	@Override
	public List<Hit> hits(WindowTab current, Rectangle body)
	{
		if (current != tab && current == WindowTab.PARTY)
			enter = true;

		tab = current;
		PanelState panel = loaded();
		if (current == WindowTab.BAG && panel != null && style != null)
		{
			state.back();
			return bag.hits(bagLayout(body, panel), style, panel);
		}

		if (current == WindowTab.TASKS && panel != null && style != null)
		{
			state.back();
			tasks.layout(body, style, panel);
			return tasks.hits(style, panel);
		}

		if (current == WindowTab.COLLECTION && panel != null && style != null)
		{
			state.back();
			collection.layout(body, style, panel);
			return collection.hits();
		}

		if (current == WindowTab.STORAGE && panel != null && style != null)
		{
			state.back();
			storage.layout(body, style, panel.storage());
			return storage.hits(WindowShell.PAGE_ORDER);
		}

		if (current == WindowTab.BATTLE && panel != null && style != null)
		{
			state.back();
			battle.layout(body, style, shell.snapshot());
			return battle.hits();
		}

		if (current != WindowTab.PARTY || panel == null || style == null)
		{
			state.back();
			return Collections.emptyList();
		}

		long now = clock.getAsLong();
		List<CreatureView> team = panel.team();
		follow(team);
		PartyLayout layout = PartyLayout.of(body, style.metrics(), style.scale(), showBanner(panel));
		if (enter)
		{
			motion.enter(layout.rows[0].width, mode, now);
			enter = false;
		}

		motion.sync(team, layout, mode, now);
		List<Hit> hits = new ArrayList<>();
		if (state.page() == PageState.Page.SUMMARY)
			summaryHits(hits, body, panel);
		else
			partyHits(hits, layout, panel, now);

		return hits;
	}

	private void partyHits(List<Hit> hits, PartyLayout layout, PanelState panel, long now)
	{
		List<CreatureView> team = panel.team();
		int order = WindowShell.PAGE_ORDER;
		for (int i = 0; i < team.size(); i++)
		{
			hits.add(new Hit(rowId(i), Hit.Kind.ROW, layout.rows[i], order++, !motion.sliding(i, now),
				RowModel.of(team.get(i), i).tooltip()));
		}

		if (team.isEmpty())
		{
			emptyHits(hits, layout, panel, order);
			order += PartyLayout.STARTERS + 1;
		}

		order = Math.max(order, WindowShell.PAGE_ORDER + PartyLayout.SLOTS);
		if (showBanner(panel))
		{
			hits.add(new Hit(ID_RESET_PROGRESS, Hit.Kind.BUTTON, layout.resetButton, order++, true,
				"Start over with a fresh save. You are asked again first."));
			hits.add(new Hit(ID_DISMISS, Hit.Kind.BUTTON, layout.dismissButton, order, true,
				"Hide this until the window is opened again"));
		}

		hits.add(new Hit("area:eggs", Hit.Kind.AREA, layout.eggs, -1, true, eggsTip(panel.eggs())));
		hits.add(new Hit("area:tasks", Hit.Kind.AREA, layout.tasks, -1, true, tasksTip(panel.tasks())));
	}

	private void emptyHits(List<Hit> hits, PartyLayout layout, PanelState panel, int first)
	{
		int order = first;
		if (!panel.starters().isEmpty())
		{
			FontMetrics bm = metrics(style.font(WindowFonts.Face.BOLD, WindowFonts.BODY_UNITS));
			List<Rectangle> cards = layout.starterCards(panel.starters().size(),
				PartyPainter.captionHeight(style, bm));
			for (int i = 0; i < cards.size(); i++)
			{
				StarterView starter = panel.starters().get(i);
				hits.add(new Hit(starterId(i), Hit.Kind.BUTTON, cards.get(i), order++, true,
					"Choose " + starter.creature().name() + ". " + starter.growsInto()));
			}

			return;
		}

		if (shell.snapshot().storageEnabled())
		{
			FontMetrics bm = metrics(style.font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS));
			hits.add(new Hit(ID_STORAGE, Hit.Kind.BUTTON, PartyPainter.emptyButton(layout, style, bm), order, true,
				"Go to Storage to take a creature out"));
		}
	}

	private void summaryHits(List<Hit> hits, Rectangle body, PanelState panel)
	{
		SummaryLayout layout = summaryLayout(body);
		int order = WindowShell.PAGE_ORDER;
		RowModel row = RowModel.of(panel.team().get(state.index()), state.index());
		hits.add(new Hit(ID_SUMMARY_MENU, Hit.Kind.ROW, layout.left, order++, true, SummaryPainter.HEADER_TIP));
		hits.add(new Hit(ID_MOVES, Hit.Kind.BUTTON, layout.moves, order++, true, movesTip(row.view())));
		boolean[] enabled = buttonStates(row);
		String[] tips = buttonTips(row);
		for (int i = 0; i < SummaryLayout.BUTTONS.length; i++)
			hits.add(new Hit(summaryButtonId(i), Hit.Kind.BUTTON, layout.buttons[i], order++, enabled[i], tips[i]));
	}

	private static String movesTip(CreatureView view)
	{
		StringBuilder text = new StringBuilder("Moves");
		for (Move move : view.moves())
			text.append('\n').append(move.name()).append(": ").append(move.describe());

		return text.append("\nClick to open").toString();
	}

	private static String eggsTip(List<EggView> eggs)
	{
		if (eggs.isEmpty())
			return "No eggs. Winning battles and catching on separate days can give you one.";

		StringBuilder text = new StringBuilder();
		for (EggView egg : eggs)
		{
			if (text.length() > 0)
				text.append('\n');

			text.append(egg.label()).append(egg.tilesLeft() <= 0 ? ": ready, waiting for room"
				: ": " + egg.tilesLeft() + " steps to go");
		}

		return text.toString();
	}

	private static String tasksTip(TaskView tasks)
	{
		if (tasks == null || tasks.rows().isEmpty())
			return "";

		StringBuilder text = new StringBuilder();
		for (TaskView.Row row : tasks.rows())
		{
			if (text.length() > 0)
				text.append('\n');

			text.append(row.label()).append(": ").append(row.done() ? "done" : row.count());
		}

		return text.toString();
	}

	@Override
	public boolean paint(Graphics2D g, WindowTab current, PageContext ctx)
	{
		PanelState panel = ctx.snapshot.state();
		if (current == WindowTab.BAG && panel != null)
		{
			bag.paint(g, ctx, bagLayout(ctx.body, ctx.style, panel), panel);
			return true;
		}

		if (current == WindowTab.TASKS && panel != null)
		{
			tasks.layout(ctx.body, ctx.style, panel);
			tasks.paint(g, ctx, panel);
			return true;
		}

		if (current == WindowTab.COLLECTION && panel != null)
		{
			collection.layout(ctx.body, ctx.style, panel);
			collection.paint(g, ctx, panel);
			return true;
		}

		if (current == WindowTab.STORAGE && panel != null)
		{
			storage.layout(ctx.body, ctx.style, panel.storage());
			storage.paint(g, ctx, panel.storage());
			return true;
		}

		if (current == WindowTab.BATTLE && panel != null)
		{
			battle.layout(ctx.body, ctx.style, ctx.snapshot);
			battle.paint(g, ctx);
			return true;
		}

		if (current != WindowTab.PARTY || panel == null)
			return false;

		if (state.page() == PageState.Page.SUMMARY && state.index() < panel.team().size())
		{
			paintSummary(g, ctx, panel);
			return true;
		}

		PartyLayout layout = PartyLayout.of(ctx.body, ctx.style.metrics(), ctx.style.scale(), showBanner(panel));
		motion.highlight(WindowPages.rowIndex(ctx.highlight), ctx.mode, ctx.now);
		party.paint(g, ctx, layout, showBanner(panel));
		return true;
	}

	private void paintSummary(Graphics2D g, PageContext ctx, PanelState panel)
	{
		SummaryLayout layout = summaryLayout(ctx.body);
		RowModel row = RowModel.of(panel.team().get(state.index()), state.index());
		Rectangle origin = null;
		if (state.cameFrom() >= 0)
		{
			PartyLayout rows = PartyLayout.of(ctx.body, ctx.style.metrics(), ctx.style.scale(), showBanner(panel));
			origin = rows.parts(rows.rows[state.cameFrom()]).portrait;
		}

		summary.paint(g, ctx, layout, SummaryContent.of(row), buttonStates(row), state.openedMs(), origin);
	}

	private SummaryLayout summaryLayout(Rectangle body)
	{
		int line = fontHeight(style.font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS));
		int small = fontHeight(style.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS));
		return SummaryLayout.of(body, style.metrics(), line, small);
	}

	private boolean showBanner(PanelState panel)
	{
		return !bannerDismissed && !panel.resetSummary().isEmpty();
	}

	private PanelState loaded()
	{
		WindowSnapshot snap = shell.snapshot();
		return snap.loaded() ? snap.state() : null;
	}

	/**
	 * Keeps the Summary on the creature it was opened on when the team changed: Make lead moves it to the top, and a
	 * creature that left the team (moved to storage) sends the page back to the list.
	 */
	private void follow(List<CreatureView> team)
	{
		if (state.page() != PageState.Page.SUMMARY)
		{
			shown = null;
			return;
		}

		state.clamp(team.size());
		if (state.page() != PageState.Page.SUMMARY)
		{
			shown = null;
			return;
		}

		CreatureView now = team.get(state.index());
		if (shown == null || shown.sameCreature(now))
		{
			shown = now;
			return;
		}

		for (int i = 0; i < team.size(); i++)
		{
			if (shown.sameCreature(team.get(i)))
			{
				state.step(i - state.index(), team.size(), state.openedMs());
				shown = team.get(i);
				return;
			}
		}

		state.back();
		shown = null;
	}

	@Override
	public void press(Hit hit)
	{
		long now = clock.getAsLong();
		PanelState panel = loaded();
		if (panel == null)
			return;

		String id = hit.id();
		if (id.startsWith(CollectionPage.PREFIX))
		{
			collection.press(id);
			return;
		}

		if (tab == WindowTab.BATTLE)
		{
			battle.press(hit, panel, shell.snapshot().friends());
			return;
		}

		if (pressBag(id, panel) || pressPin(id, panel))
			return;

		if (tab == WindowTab.STORAGE)
		{
			storageFlow.press(id);
			return;
		}

		int place = rowIndex(id);
		if (place >= 0)
		{
			openSummary(place, panel, now);
			return;
		}

		if (id.startsWith(STARTER_PREFIX))
		{
			actions.pickStarter(Integer.parseInt(id.substring(STARTER_PREFIX.length())));
			return;
		}

		switch (id)
		{
			case ID_STORAGE:
				shell.showTab(WindowTab.STORAGE, now);
				break;
			case ID_RESET_PROGRESS:
				confirmReset(panel, now);
				break;
			case ID_DISMISS:
				bannerDismissed = true;
				break;
			case ID_SUMMARY_MENU:
				menu(hit, hit.bounds().x + hit.bounds().width / 2, hit.bounds().y + hit.bounds().height / 2, false);
				break;
			case ID_MOVES:
				pressSummaryButton(2, panel);
				break;
			default:
				if (id.startsWith(SUMMARY_BUTTON_PREFIX))
					pressSummaryButton(Integer.parseInt(id.substring(SUMMARY_BUTTON_PREFIX.length())), panel);

				break;
		}
	}

	private BagLayout bagLayout(Rectangle body, PanelState panel)
	{
		return bagLayout(body, style, panel);
	}

	private static BagLayout bagLayout(Rectangle body, WindowStyle st, PanelState panel)
	{
		int jars = BagText.jarsShown(panel).size();
		return BagLayout.of(body, st.metrics(), st.scale(), jars, panel.eggs().size());
	}

	private boolean pressPin(String id, PanelState panel)
	{
		if (id.startsWith(TasksPage.PIN_PREFIX))
		{
			int slot = Integer.parseInt(id.substring(TasksPage.PIN_PREFIX.length()));
			List<TaskView.Row> rows = panel.tasks() == null ? Collections.emptyList() : panel.tasks().rows();
			if (slot >= 0 && slot < rows.size())
				actions.pinTask(slot, !rows.get(slot).pinned());

			return true;
		}

		int place = BagPainter.pinPlace(id);
		List<EggView> eggs = panel.eggs();
		if (place >= 0 && place < eggs.size())
			actions.pinEgg(TradePick.of(place, eggs.get(place)), !eggs.get(place).pinned());

		return place >= 0;
	}

	private boolean pressBag(String id, PanelState panel)
	{
		if (BagPainter.ID_TITLE.equals(id))
		{
			List<String> titles = panel.titles();
			if (titles.size() > 1)
				actions.pickTitle(titles.get((titles.indexOf(panel.shownTitle()) + 1) % titles.size()));

			return true;
		}

		int place = BagPainter.jarPlace(id);
		List<JarType> jars = BagText.jarsShown(panel);
		if (place >= 0 && place < jars.size())
			actions.selectJar(jars.get(place));

		return place >= 0;
	}

	private void openSummary(int place, PanelState panel, long now)
	{
		if (state.open(place, panel.team().size(), now))
			shown = panel.team().get(place);
	}

	private void pressSummaryButton(int button, PanelState panel)
	{
		if (state.page() != PageState.Page.SUMMARY || state.index() >= panel.team().size())
			return;

		Commands commands = commands(panel, state.index());
		switch (button)
		{
			case 0:
				back();
				break;
			case 1:
				commands.rename();
				break;
			case 2:
				commands.moves();
				break;
			case 3:
				commands.makeLead();
				break;
			case 4:
				commands.moveToStorage();
				break;
			default:
				commands.useItem();
				break;
		}
	}

	private void confirmReset(PanelState panel, long now)
	{
		String message = "Reset all progress? You will lose everything in this account's save:\n"
			+ panel.resetSummary() + ".\nYou start again with 10 Bronze jars. Settings are kept.";
		Modal ask = new Modal(RESET_TITLE, message, Arrays.asList(RESET_NO, RESET_YES), 0, choice ->
		{
			if (choice == 1)
				actions.resetProgress();
		}, () ->
		{
		});
		shell.openModal(ask, null, now);
	}

	@Override
	public void menu(Hit hit, int x, int y, boolean keyboard)
	{
		PanelState panel = loaded();
		if (panel == null)
			return;

		if (hit.id().startsWith(CollectionPage.PREFIX))
		{
			collectionMenu(hit, x, y, keyboard);
			return;
		}

		if (tab == WindowTab.STORAGE)
		{
			storageFlow.menu(hit.id(), x, y, keyboard);
			return;
		}

		int place = ID_SUMMARY_MENU.equals(hit.id()) ? state.index() : rowIndex(hit.id());
		if (place < 0 || place >= panel.team().size())
			return;

		anchorX = x;
		anchorY = y;
		Commands commands = commands(panel, place);
		ContextMenuRules.Facts facts = facts(panel, place);
		List<ContextMenu.Item> items = ID_SUMMARY_MENU.equals(hit.id()) ? ContextMenuRules.summary(facts, commands)
			: ContextMenuRules.party(facts, commands);
		shell.openMenu(items, x, y, keyboard, clock.getAsLong());
	}

	private void collectionMenu(Hit hit, int x, int y, boolean keyboard)
	{
		LogEntry entry = collection.entryOf(hit.id());
		if (entry == null)
			return;

		List<ContextMenu.Item> items = CollectionPage.menuItems(entry, actions::openWiki);
		if (!items.isEmpty())
			shell.openMenu(items, x, y, keyboard, clock.getAsLong());
	}

	@Override
	public void wheel(int x, int y, int rotation)
	{
		if (tab == WindowTab.TASKS)
			tasks.wheel(x, y, rotation);
		else if (tab == WindowTab.COLLECTION)
			collection.wheel(x, y, rotation);
		else if (tab == WindowTab.STORAGE)
			storage.wheel(x, y, rotation);

		if (tab == WindowTab.BATTLE)
			battle.wheel(x, y, rotation);
	}

	@Override
	public boolean pointerPressed(int x, int y)
	{
		return tab == WindowTab.TASKS && tasks.pressed(x, y)
			|| tab == WindowTab.COLLECTION && collection.pressed(x, y)
			|| tab == WindowTab.STORAGE && storage.pressed(x, y)
			|| tab == WindowTab.BATTLE && battle.pressed(x, y);
	}

	@Override
	public void pointerDragged(int x, int y)
	{
		if (tab == WindowTab.TASKS)
			tasks.dragged(x, y);
		else if (tab == WindowTab.COLLECTION)
			collection.dragged(x, y);
		else if (tab == WindowTab.STORAGE)
			storage.dragged(x, y);

		if (tab == WindowTab.BATTLE)
			battle.dragged(x, y);
	}

	@Override
	public boolean pointerReleased(int x, int y)
	{
		return tab == WindowTab.TASKS && tasks.released()
			|| tab == WindowTab.COLLECTION && collection.released()
			|| tab == WindowTab.STORAGE && storageFlow.released(x, y)
			|| tab == WindowTab.BATTLE && battle.released();
	}

	@Override
	public int depth()
	{
		if (tab == WindowTab.STORAGE)
			return storage.carrying() >= 0 ? 1 : 0;

		return tab == WindowTab.PARTY ? state.depth() : 0;
	}

	@Override
	public void back()
	{
		if (tab == WindowTab.STORAGE && storage.carrying() >= 0)
		{
			storage.stopCarrying();
			return;
		}

		state.back();
		shown = null;
	}

	@Override
	public boolean key(int keyCode, int modifiers, long nowMs)
	{
		PanelState panel = loaded();
		if (panel != null && tab == WindowTab.COLLECTION)
			return collectionKey(keyCode);

		if (panel != null && tab == WindowTab.STORAGE)
			return storageKey(keyCode);

		if (panel != null && tab == WindowTab.BATTLE)
			return battle.key(keyCode);

		if (panel == null || tab != WindowTab.PARTY || state.page() != PageState.Page.SUMMARY)
			return false;

		int size = panel.team().size();
		switch (keyCode)
		{
			case KeyEvent.VK_UP:
				state.step(-1, size, nowMs);
				shown = state.index() < size ? panel.team().get(state.index()) : null;
				return true;
			case KeyEvent.VK_DOWN:
				state.step(1, size, nowMs);
				shown = state.index() < size ? panel.team().get(state.index()) : null;
				return true;
			case KeyEvent.VK_ENTER:
			case KeyEvent.VK_SPACE:
				return openSummaryMenu();
			default:
				return false;
		}
	}

	private boolean storageKey(int keyCode)
	{
		int place = storage.keyTarget(keyCode, shell.focus().highlight());
		if (place < 0)
			return false;

		shell.focus().highlightKey(StoragePage.rowId(place));
		return true;
	}

	private boolean collectionKey(int keyCode)
	{
		boolean activate = keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_SPACE;
		if (!activate)
			return collection.key(keyCode);

		Hit highlighted = shell.focus().highlighted();
		if (highlighted != null && highlighted.kind() == Hit.Kind.BUTTON)
			return false;

		Rectangle row = collection.chosenRowBounds();
		LogEntry entry = collection.selectedEntry();
		if (row == null || entry == null || !CollectionFilter.known(entry))
			return false;

		List<ContextMenu.Item> items = CollectionPage.menuItems(entry, actions::openWiki);
		shell.openMenu(items, row.x + row.width / 2, row.y + row.height / 2, true, clock.getAsLong());
		return true;
	}

	private boolean openSummaryMenu()
	{
		Hit highlighted = shell.focus().highlighted();
		boolean onButton = highlighted != null && highlighted.kind() != Hit.Kind.TAB
			&& !ID_SUMMARY_MENU.equals(highlighted.id());
		if (onButton)
			return false;

		for (Hit hit : shell.hits())
		{
			if (ID_SUMMARY_MENU.equals(hit.id()))
			{
				Rectangle b = hit.bounds();
				menu(hit, b.x + b.width / 2, b.y + b.height / 2, true);
				return true;
			}
		}

		return false;
	}

	private ContextMenuRules.Facts facts(PanelState panel, int place)
	{
		boolean elite = panel.elite().mode() == EliteView.Mode.RUNNING;
		return new ContextMenuRules.Facts(RowModel.of(panel.team().get(place), place), elite,
			shell.snapshot().storageEnabled(), panel.items());
	}

	private boolean[] buttonStates(RowModel row)
	{
		PanelState panel = loaded();
		boolean[] enabled = new boolean[SummaryLayout.BUTTONS.length];
		Arrays.fill(enabled, true);
		if (panel == null)
			return enabled;

		List<ContextMenu.Item> items = menuItems(panel, row);
		enabled[3] = enabledOf(items, ContextMenuRules.MAKE_LEAD);
		enabled[4] = enabledOf(items, ContextMenuRules.STORAGE);
		enabled[5] = enabledOf(items, ContextMenuRules.USE_ITEM);
		return enabled;
	}

	private String[] buttonTips(RowModel row)
	{
		PanelState panel = loaded();
		String[] tips = {"Back to the party (Esc)", "Give it a nickname", "See its moves", "Make it your lead",
			"Move it to storage", "Use a healing item on it"};
		if (panel == null)
			return tips;

		List<ContextMenu.Item> items = menuItems(panel, row);
		tips[3] = reasonOr(items, ContextMenuRules.MAKE_LEAD, tips[3]);
		tips[4] = reasonOr(items, ContextMenuRules.STORAGE, tips[4]);
		tips[5] = reasonOr(items, ContextMenuRules.USE_ITEM, tips[5]);
		return tips;
	}

	private List<ContextMenu.Item> menuItems(PanelState panel, RowModel row)
	{
		return ContextMenuRules.summary(facts(panel, row.index()), commands(panel, row.index()));
	}

	private static boolean enabledOf(List<ContextMenu.Item> items, String label)
	{
		for (ContextMenu.Item item : items)
		{
			if (item.label().equals(label))
				return item.enabled();
		}

		return false;
	}

	private static String reasonOr(List<ContextMenu.Item> items, String label, String otherwise)
	{
		for (ContextMenu.Item item : items)
		{
			if (item.label().equals(label) && !item.enabled())
				return label + ": " + item.reason();
		}

		return otherwise;
	}

	private Commands commands(PanelState panel, int place)
	{
		return new Commands(panel, place);
	}

	/**
	 * Cleans what is typed into the Rename box: empty is valid and means going back to the species name.
	 *
	 * @param typed the text
	 * @return an empty text for none, the cleaned nickname, or null when it is not allowed
	 */
	static String renameClean(String typed)
	{
		return typed.trim().isEmpty() ? "" : Nickname.clean(typed);
	}

	private static int fontHeight(Font font)
	{
		synchronized (HEIGHTS)
		{
			Integer have = HEIGHTS.get(font);
			if (have != null)
				return have;

			Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
			try
			{
				int height = g.getFontMetrics(font).getHeight();
				HEIGHTS.put(font, height);
				return height;
			}
			finally
			{
				g.dispose();
			}
		}
	}

	static FontMetrics metrics(Font font)
	{
		Graphics2D g = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
		try
		{
			return g.getFontMetrics(font);
		}
		finally
		{
			g.dispose();
		}
	}

	/** What the Storage page asks of the window around it. */
	private final class StorageHost implements StoragePageFlow.Host
	{
		@Override
		public void menu(List<ContextMenu.Item> items, int x, int y, boolean keyboard)
		{
			shell.openMenu(items, x, y, keyboard, clock.getAsLong());
		}

		@Override
		public void modal(Modal dialog, TextBox box)
		{
			shell.openModal(dialog, box, clock.getAsLong());
		}

		@Override
		public boolean keyboardLed()
		{
			return shell.focus().keyboardLed();
		}

		@Override
		public String takeDraft()
		{
			return shell.takeDraft();
		}

		@Override
		public List<CreatureView> storage()
		{
			PanelState panel = loaded();
			return panel == null ? Collections.emptyList() : panel.storage();
		}

		@Override
		public Map<ItemType, Integer> items()
		{
			PanelState panel = loaded();
			return panel == null ? Collections.emptyMap() : panel.items();
		}
	}

	/** What each menu item and each Summary button does for one creature. */
	private final class Commands implements ContextMenuRules.Commands
	{
		private final PanelState panel;
		private final int place;
		private final CreatureView view;

		Commands(PanelState panel, int place)
		{
			this.panel = panel;
			this.place = place;
			this.view = panel.team().get(place);
		}

		private TradePick pick()
		{
			return TradePick.of(Holding.TEAM, place, view);
		}

		@Override
		public void summary()
		{
			openSummary(place, panel, clock.getAsLong());
		}

		@Override
		public void makeLead()
		{
			actions.setLead(pick());
		}

		@Override
		public void moveToStorage()
		{
			actions.moveToStorage(pick());
		}

		@Override
		public void rename()
		{
			TextBox box = new TextBox(Nickname.MAX_LENGTH, WindowPages::renameClean, TextBox.system());
			String draft = shell.takeDraft();
			box.setText(draft.isEmpty() ? view.nickname() == null ? "" : view.nickname() : draft);
			box.placeholder(view.name());
			openRename(box);
		}

		private void openRename(TextBox box)
		{
			TradePick pick = pick();
			Modal ask = new Modal("Rename", "Name for your " + view.name() + " (1 to " + Nickname.MAX_LENGTH
				+ " letters, numbers, spaces or -). Leave it empty to go back to its species name.",
				Arrays.asList("Cancel", "Save"), 1, choice ->
				{
					if (choice != 1)
						return;

					String value = box.value();
					if (value == null)
						openRename(box);
					else
						actions.rename(pick, value);
				}, () ->
				{
				}).withField(box, 1);
			shell.openModal(ask, box, clock.getAsLong());
		}

		@Override
		public void moves()
		{
			TradePick pick = pick();
			Modal ask = MovePicker.modal(view, forget -> actions.resolveMove(pick, forget), () ->
				shell.openMenu(MovePicker.forgetItems(view, index -> actions.resolveMove(pick, index)), anchorX,
					anchorY, true, clock.getAsLong()));
			shell.openModal(ask, null, clock.getAsLong());
		}

		@Override
		public void useItem()
		{
			TradePick pick = pick();
			List<ContextMenu.Item> items = ContextMenuRules.useItems(facts(panel, place),
				type -> actions.useItem(pick, type));
			shell.openMenu(items, anchorX, anchorY, true, clock.getAsLong());
		}

		@Override
		public void revive()
		{
			actions.useItem(pick(), ItemType.REVIVE);
		}
	}
}
