/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Paints the Party page: six slanted rows (portrait, name, gender, style, level, health and experience bars, the
 * lead's arrow and outline, the badges), the empty slots, the first-run starter cards, the "No creatures yet" message
 * and the right column with the eggs, the top three tasks and the reset banner. The layout and the content come from
 * {@link PartyLayout} and {@link RowModel}, the motion from {@link PartyMotion}; this class only draws. All colours
 * come from the theme and all text is drawn through {@link PageText}. Holds no RuneLite types.
 */
final class PartyPainter
{
	/** The most tasks the right column lists. */
	static final int TASKS_SHOWN = 3;

	/** The words of the first-run caption. */
	static final String CHOOSE = "Choose your first creature";

	/** The words of an empty team. */
	static final String EMPTY = "No creatures yet";

	/** The hint under {@link #EMPTY}. */
	static final String EMPTY_HINT = "Right-click an outlined monster and choose Catch.";

	/** The label of the button that goes to Storage from an empty team. */
	static final String OPEN_STORAGE = "Open Storage";

	/** The label of the button that pins the party to the game screen. */
	static final String PIN_PARTY = "Pin Party to UI";

	/** The title of the reset banner. */
	static final String BANNER = "Start over?";

	private final PortraitCache pictures;
	private final PartyMotion motion;
	private final PageText.Sink sink;
	private final BooleanSupplier pinned;

	/**
	 * Creates the painter.
	 *
	 * @param pictures where creature pictures are
	 * @param motion the rows' motion
	 * @param sink hears every string drawn, for tests
	 * @param pinned whether the party is pinned to the game screen, which lights the Pin Party to UI button
	 */
	PartyPainter(PortraitCache pictures, PartyMotion motion, PageText.Sink sink, BooleanSupplier pinned)
	{
		this.pinned = pinned;
		this.pictures = pictures;
		this.motion = motion;
		this.sink = sink;
	}

	/**
	 * Paints the page.
	 *
	 * @param g the graphics
	 * @param ctx the frame's facts
	 * @param layout where everything is
	 * @param banner whether the reset banner shows
	 */
	void paint(Graphics2D g, PageContext ctx, PartyLayout layout, boolean banner)
	{
		Shape savedClip = g.getClip();
		g.clipRect(ctx.body.x, ctx.body.y, ctx.body.width, ctx.body.height);
		PanelState state = ctx.snapshot.state();
		List<CreatureView> team = state == null ? Collections.emptyList() : state.team();
		if (!team.isEmpty())
			rows(g, ctx, layout, team);
		else if (state != null && !state.starters().isEmpty())
			starters(g, ctx, layout, state.starters());
		else
			empty(g, ctx, layout);

		side(g, ctx, layout, state, banner);
		g.setClip(savedClip);
	}

	private void rows(Graphics2D g, PageContext ctx, PartyLayout layout, List<CreatureView> team)
	{
		WindowStyle st = ctx.style;
		for (int i = 0; i < PartyLayout.SLOTS; i++)
		{
			if (i >= team.size())
			{
				emptySlot(g, ctx, layout.rows[i], i);
				continue;
			}

			RowModel model = RowModel.of(team.get(i), i);
			String id = WindowPages.rowId(i);
			double nudge = motion.nudge(i, ctx.now) * st.metrics().nudge;
			double x = layout.rows[i].x + motion.rowX(i, ctx.now) + nudge;
			double y = motion.rowY(i, layout.rows[i].y, ctx.now);
			double from = layout.rows[i].width * PartyMotion.ENTER_FROM;
			double enter = Math.max(0.0, 1.0 + motion.rowX(i, ctx.now) / from);
			row(g, ctx, layout, model, new Rectangle2D.Double(x, y, layout.rows[i].width, layout.rows[i].height),
				ctx.hoverOf(id), Math.min(1.0, enter));
		}

		for (PartyMotion.Ghost ghost : motion.ghosts(ctx.now))
			ghost(g, ctx, layout, ghost);

		if (ctx.focused && ctx.highlight != null && ctx.highlight.startsWith(WindowPages.ROW_PREFIX))
		{
			double at = motion.cursor(ctx.now);
			int whole = (int) Math.floor(at);
			int step = layout.rows[1].y - layout.rows[0].y;
			double y = layout.rows[0].y + at * step;
			Rectangle row = layout.rows[Math.max(0, Math.min(PartyLayout.SLOTS - 1, whole))];
			int near = Math.max(0, Math.min(PartyLayout.SLOTS - 1, (int) Math.round(at)));
			double x = row.x + motion.nudge(near, ctx.now) * st.metrics().nudge;
			st.focusRing(g, Slant.shape(new Rectangle2D.Double(x, y, row.width, row.height), st.metrics().slant),
				ThemeColor.PARTY_HIGHLIGHT);
		}

		lead(g, ctx, layout);
	}

	private void ghost(Graphics2D g, PageContext ctx, PartyLayout layout, PartyMotion.Ghost ghost)
	{
		double t = Easing.OUT_CUBIC.apply(ghost.progress(ctx.now));
		Rectangle base = layout.rows[ghost.index()];
		double x = base.x - base.width * t;
		Composite saved = PageContext.fade(g, 1.0 - t);
		row(g, ctx, layout, RowModel.of(ghost.view(), ghost.index()),
			new Rectangle2D.Double(x, base.y, base.width, base.height), 0.0, 1.0);
		g.setComposite(saved);
	}

	private void lead(Graphics2D g, PageContext ctx, PartyLayout layout)
	{
		WindowStyle st = ctx.style;
		Rectangle row = layout.rows[0];
		double bob = Loop.wave(ctx.mode, ctx.now, WindowTimes.LEAD_BOB, 0) * 2.0 * st.scale();
		double size = Math.max(6, layout.arrow.width - 2);
		g.setColor(st.color(ThemeColor.FOCUS_RING));
		g.fill(Icons.arrow(layout.arrow.x + 1 + bob + 1, row.y + row.height / 2.0, size));
	}

	private void emptySlot(Graphics2D g, PageContext ctx, Rectangle row, int index)
	{
		WindowStyle st = ctx.style;
		Composite saved = PageContext.fade(g, 0.55);
		Shape shape = Slant.shape(new Rectangle2D.Double(row.x, row.y, row.width, row.height), st.metrics().slant);
		g.setColor(st.color(ThemeColor.PANEL_BOX));
		g.fill(shape);
		Stroke stroke = g.getStroke();
		g.setColor(st.color(ThemeColor.PANEL_BORDER));
		g.setStroke(new BasicStroke(st.metrics().border, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
			new float[] {6f, 5f}, 0f));
		g.draw(shape);
		g.setStroke(stroke);
		g.setComposite(saved);
		PageText text = new PageText(st, sink);
		int slant = st.metrics().slant;
		Rectangle box = new Rectangle(row.x + slant * 2, row.y, Math.max(0, row.width - slant * 4), row.height);
		text.draw(g, "Empty slot " + (index + 1), box, st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS),
			ThemeColor.SUBTEXT, PageText.Align.LEFT);
	}

	private void row(Graphics2D g, PageContext ctx, PartyLayout layout, RowModel model, Rectangle2D box,
		double hover, double enter)
	{
		WindowStyle st = ctx.style;
		Composite saved = PageContext.fade(g, model.dim() * enter);
		boolean lead = model.lead();
		st.row(g, box, lead, hover, ctx.pressOf(WindowPages.rowId(model.index())));
		if (lead)
		{
			double pulse = Loop.pulse(ctx.mode, ctx.now, WindowTimes.LEAD_PULSE, 0);
			Stroke stroke = g.getStroke();
			double glow = ctx.mode.loops() ? 0.35 + 0.65 * pulse : 1.0;
			g.setColor(WindowTheme.fade(st.color(ThemeColor.LEAD_BORDER), glow));
			g.setStroke(new BasicStroke(st.metrics().border + 1f));
			g.draw(Slant.shape(new Rectangle2D.Double(box.getX() + 1, box.getY() + 1, box.getWidth() - 2,
				box.getHeight() - 2), st.metrics().slant));
			g.setStroke(stroke);
		}

		Rectangle at = new Rectangle((int) Math.round(box.getX()), (int) Math.round(box.getY()),
			(int) Math.round(box.getWidth()), (int) Math.round(box.getHeight()));
		PartyLayout.Parts parts = layout.parts(at);
		ThemeColor ink = WindowStyle.rowText(lead);
		double idle = Loop.wave(ctx.mode, ctx.now, WindowTimes.PORTRAIT_BOB, model.index() * 437L);
		double hop = motion.hop(model.index(), ctx.now) * PartyMotion.HOP_UNITS;
		double bob = (idle * (lead ? 1.0 : 0.6) + hop) * st.scale();
		Rectangle p = parts.portrait;
		PortraitFrame.paint(g, st, pictures, model,
			new Rectangle2D.Double(p.x, p.y, p.width, p.height), st.metrics().slant / 2.0, bob, ctx.now, ctx.mode);
		if (model.newMove())
			star(g, ctx, parts);

		text(g, ctx, parts, model, ink, lead);
		bars(g, ctx, parts, model, ink);
		g.setComposite(saved);
	}

	private void star(Graphics2D g, PageContext ctx, PartyLayout.Parts parts)
	{
		WindowStyle st = ctx.style;
		double pulse = Loop.pulse(ctx.mode, ctx.now, WindowTimes.LEAD_BOB, 0);
		double radius = Math.max(4.0, parts.portrait.width / 5.0) * (1.0 + 0.25 * pulse);
		double cx = parts.portrait.x + parts.portrait.width - radius * 0.9;
		double cy = parts.portrait.y + radius * 1.0;
		Shape star = Icons.star(cx, cy, radius);
		g.setColor(st.color(ThemeColor.HP_TRACK));
		Stroke stroke = g.getStroke();
		g.setStroke(new BasicStroke(Math.max(2f, st.metrics().border)));
		g.draw(star);
		g.setStroke(stroke);
		g.setColor(st.color(ThemeColor.GOLD));
		g.fill(star);
	}

	private void text(Graphics2D g, PageContext ctx, PartyLayout.Parts parts, RowModel model, ThemeColor ink,
		boolean lead)
	{
		WindowStyle st = ctx.style;
		PageText text = new PageText(st, sink);
		Font small = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		FontMetrics sm = g.getFontMetrics(small);
		String tag = tag(model);
		Rectangle nameBox = new Rectangle(parts.name);
		if (!tag.isEmpty())
		{
			int w = Math.min(parts.tags.width, sm.stringWidth(tag));
			Rectangle tagBox = new Rectangle(parts.name.x + parts.name.width - w, parts.name.y, w, parts.name.height);
			text.draw(g, tag, tagBox, small, ink, PageText.Align.RIGHT);
			nameBox.width = Math.max(0, parts.name.width - w - parts.pad);
		}

		text.drawName(g, model.name(), nameBox, ink);
		if (model.gender() != null)
		{
			g.setColor(GenderColors.of(model.gender(), lead));
			Icons.gender(g, model.gender(), parts.gender.x, parts.gender.y, parts.gender.width);
		}

		g.setColor(st.color(ink));
		Icons.style(g, model.style(), parts.style.x, parts.style.y, parts.style.width);
		text.draw(g, model.levelText(), parts.level, small, ink, PageText.Align.RIGHT);
		text.draw(g, model.hpText(), parts.hpText, small, ink, PageText.Align.LEFT);
	}

	/**
	 * Picks the one tag a row has room for: a state word first, then what it waits for, then the rating, then Lead.
	 *
	 * @param model the row
	 * @return the tag, or an empty text for none
	 */
	static String tag(RowModel model)
	{
		if (model.has(RowModel.Badge.FAINTED))
			return RowModel.Badge.FAINTED.label();

		if (model.has(RowModel.Badge.RESTING))
			return RowModel.Badge.RESTING.label();

		if (model.has(RowModel.Badge.WAITING))
			return RowModel.Badge.WAITING.label();

		if (model.has(RowModel.Badge.RATING))
			return model.rating();

		return model.lead() ? RowModel.Badge.LEAD.label() : "";
	}

	private void bars(Graphics2D g, PageContext ctx, PartyLayout.Parts parts, RowModel model, ThemeColor ink)
	{
		WindowStyle st = ctx.style;
		int i = model.index();
		double hp = model.state() == RowModel.State.FAINTED ? 0.0 : motion.hp(i, ctx.now);
		Rectangle2D hpBox = new Rectangle2D.Double(parts.hp.x, parts.hp.y, parts.hp.width, parts.hp.height);
		st.hpBar(g, hpBox, hp);
		if (hp > 0 && hp < WindowStyle.HP_DANGER_BELOW && ctx.mode.loops())
		{
			double pulse = Loop.pulse(ctx.mode, ctx.now, WindowTimes.LEAD_PULSE, 0);
			g.setColor(WindowTheme.fade(st.color(ThemeColor.HP_DANGER), 0.2 + 0.5 * pulse));
			g.fill(Slant.filled(hpBox, st.metrics().barSlant, hp));
		}

		st.xpBar(g, new Rectangle2D.Double(parts.xp.x, parts.xp.y, parts.xp.width, parts.xp.height),
			motion.xp(i, ctx.now));
	}

	private void empty(Graphics2D g, PageContext ctx, PartyLayout layout)
	{
		WindowStyle st = ctx.style;
		PageText text = new PageText(st, sink);
		int pad = st.metrics().pad;
		Rectangle main = layout.main;
		st.panel(g, new Rectangle2D.Double(main.x, main.y, main.width, main.height));
		Font title = st.font(WindowFonts.Face.BOLD, WindowFonts.TITLE_UNITS);
		FontMetrics tm = g.getFontMetrics(title);
		Rectangle titleBox = new Rectangle(main.x + pad, main.y + pad, main.width - 2 * pad, tm.getHeight());
		text.draw(g, EMPTY, titleBox, st.font(WindowFonts.Face.BOLD, WindowFonts.BODY_UNITS), ThemeColor.TEXT,
			PageText.Align.LEFT);
		Font body = st.font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS);
		FontMetrics bm = g.getFontMetrics(body);
		Rectangle hint = new Rectangle(main.x + pad, titleBox.y + bm.getHeight() + pad / 2, main.width - 2 * pad,
			bm.getHeight() * 2);
		text.drawWrapped(g, EMPTY_HINT, hint, body, ThemeColor.SUBTEXT);
		if (ctx.snapshot.storageEnabled())
		{
			Rectangle button = emptyButton(layout, st, bm);
			Rectangle2D shape = new Rectangle2D.Double(button.x, button.y, button.width, button.height);
			st.button(g, shape, OPEN_STORAGE, true, false, ctx.hoverOf(WindowPages.ID_STORAGE),
				ctx.pressOf(WindowPages.ID_STORAGE));
			if (ctx.focused && WindowPages.ID_STORAGE.equals(ctx.highlight))
				st.focusRing(g, Slant.shape(shape, st.metrics().chipSlant));
		}
	}

	/**
	 * Gives the place of the Open Storage button of an empty team.
	 *
	 * @param layout the layout
	 * @param st the style
	 * @param bm the measures of the body font
	 * @return the button's box
	 */
	static Rectangle emptyButton(PartyLayout layout, WindowStyle st, FontMetrics bm)
	{
		int pad = st.metrics().pad;
		int w = Math.min(layout.main.width - 2 * pad, bm.stringWidth(OPEN_STORAGE) + 6 * pad);
		return new Rectangle(layout.main.x + pad, layout.main.y + layout.main.height - pad - bm.getHeight() - pad,
			w, bm.getHeight() + pad);
	}

	/**
	 * Gives the height of the caption above the starter cards.
	 *
	 * @param st the style
	 * @param bm the measures of the body font
	 * @return the height in pixels
	 */
	static int captionHeight(WindowStyle st, FontMetrics bm)
	{
		return bm.getHeight() + st.metrics().pad / 2;
	}

	private void starters(Graphics2D g, PageContext ctx, PartyLayout layout, List<StarterView> starters)
	{
		WindowStyle st = ctx.style;
		PageText text = new PageText(st, sink);
		Font bold = st.font(WindowFonts.Face.BOLD, WindowFonts.BODY_UNITS);
		FontMetrics bm = g.getFontMetrics(bold);
		int captionH = captionHeight(st, bm);
		text.draw(g, CHOOSE, new Rectangle(layout.main.x, layout.main.y, layout.main.width, bm.getHeight()), bold,
			ThemeColor.TEXT, PageText.Align.LEFT);
		List<Rectangle> cards = layout.starterCards(starters.size(), captionH);
		for (int i = 0; i < cards.size(); i++)
		{
			Rectangle card = cards.get(i);
			String id = WindowPages.starterId(i);
			StarterView starter = starters.get(i);
			RowModel model = RowModel.of(starter.creature(), 1);
			Rectangle2D box = new Rectangle2D.Double(card.x, card.y, card.width, card.height);
			Shape shape = st.row(g, box, false, ctx.hoverOf(id), ctx.pressOf(id));
			PartyLayout.Parts parts = layout.parts(card);
			Rectangle p = parts.portrait;
			PortraitFrame.paint(g, st, pictures, model, new Rectangle2D.Double(p.x, p.y, p.width, p.height),
				st.metrics().slant / 2.0, 0, ctx.now, ctx.mode);
			Rectangle nameBox = new Rectangle(parts.name.x, parts.name.y, parts.level.x - parts.name.x - parts.pad,
				parts.name.height);
			text.drawName(g, model.name(), nameBox, ThemeColor.ROW_TEXT);
			Font small = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
			text.draw(g, model.levelText(), parts.level, small, ThemeColor.ROW_TEXT, PageText.Align.RIGHT);
			Rectangle info = new Rectangle(parts.name.x, parts.hp.y - parts.pad, card.x + card.width - parts.name.x
				- st.metrics().slant - parts.pad, Math.max(0, card.y + card.height - parts.hp.y));
			text.drawWrapped(g, starter.growsInto(), info, small, ThemeColor.SUBTEXT);
			if (ctx.focused && id.equals(ctx.highlight))
				st.focusRing(g, shape);
		}
	}

	private void side(Graphics2D g, PageContext ctx, PartyLayout layout, PanelState state, boolean banner)
	{
		WindowStyle st = ctx.style;
		PageText text = new PageText(st, sink);
		int pad = st.metrics().pad;
		Font bold = st.font(WindowFonts.Face.BOLD, WindowFonts.BODY_UNITS);
		Font small = st.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		FontMetrics bm = g.getFontMetrics(bold);
		FontMetrics sm = g.getFontMetrics(small);
		Rectangle eggs = layout.eggs;
		st.panel(g, new Rectangle2D.Double(eggs.x, eggs.y, eggs.width, eggs.height));
		List<EggView> carried = state == null ? Collections.emptyList() : state.eggs();
		Rectangle title = inner(eggs, pad, bm.getHeight());
		text.draw(g, "Eggs  " + carried.size() + " / " + SaveData.MAX_EGGS, title, bold, ThemeColor.TEXT,
			PageText.Align.LEFT);
		int y = title.y + bm.getHeight() + pad / 3;
		int lines = Math.max(0, (eggs.y + eggs.height - pad / 2 - y) / (sm.getHeight() + 2));
		if (carried.isEmpty())
			text.draw(g, "None yet", new Rectangle(title.x, y, title.width, sm.getHeight()), small,
				ThemeColor.SUBTEXT, PageText.Align.LEFT);

		for (int i = 0; i < Math.min(lines, carried.size()); i++)
		{
			EggView egg = carried.get(i);
			Rectangle line = new Rectangle(title.x, y + i * (sm.getHeight() + 2), title.width, sm.getHeight());
			g.setColor(st.color(ThemeColor.ACCENT));
			g.fill(Icons.egg(line.x, line.y + 1, sm.getHeight() - 2));
			String words = egg.tilesLeft() <= 0 ? "Ready" : egg.tilesLeft() + " steps";
			Rectangle label = new Rectangle(line.x + sm.getHeight(), line.y, line.width - sm.getHeight(), line.height);
			text.draw(g, egg.label() + "  " + words, label, small, ThemeColor.TEXT, PageText.Align.LEFT);
		}

		tasks(g, ctx, layout, state, text, bold, small);
		pin(g, ctx, layout);
		if (banner)
			banner(g, ctx, layout, text, bold, small);
	}

	private void tasks(Graphics2D g, PageContext ctx, PartyLayout layout, PanelState state, PageText text, Font bold,
		Font small)
	{
		WindowStyle st = ctx.style;
		int pad = st.metrics().pad;
		FontMetrics bm = g.getFontMetrics(bold);
		FontMetrics sm = g.getFontMetrics(small);
		Rectangle box = layout.tasks;
		st.panel(g, new Rectangle2D.Double(box.x, box.y, box.width, box.height));
		Rectangle title = inner(box, pad, bm.getHeight());
		text.draw(g, "Tasks", title, bold, ThemeColor.TEXT, PageText.Align.LEFT);
		List<TaskView.Row> rows = state == null || state.tasks() == null ? Collections.emptyList()
			: state.tasks().rows();
		int y = title.y + bm.getHeight() + pad / 3;
		int each = sm.getHeight() + st.metrics().bar / 2 + 3;
		int fit = Math.max(0, (box.y + box.height - pad / 2 - y) / each);
		for (int i = 0; i < Math.min(Math.min(TASKS_SHOWN, fit), rows.size()); i++)
		{
			TaskView.Row row = rows.get(i);
			int top = y + i * each;
			String count = row.done() ? "Done" : row.count();
			int countW = sm.stringWidth(count);
			Rectangle label = new Rectangle(title.x, top, Math.max(0, title.width - countW - pad / 2), sm.getHeight());
			text.draw(g, row.label(), label, small, row.done() ? ThemeColor.SUBTEXT : ThemeColor.TEXT,
				PageText.Align.LEFT);
			text.draw(g, count, new Rectangle(title.x + title.width - countW, top, countW, sm.getHeight()), small,
				ThemeColor.SUBTEXT, PageText.Align.RIGHT);
			int barH = Math.max(2, st.metrics().bar / 3);
			st.xpBar(g, new Rectangle2D.Double(title.x, top + sm.getHeight() + 1, title.width, barH),
				row.done() ? 1.0 : row.fraction());
		}
	}

	private void pin(Graphics2D g, PageContext ctx, PartyLayout layout)
	{
		WindowStyle st = ctx.style;
		Rectangle box = layout.pinButton;
		Rectangle2D shape = new Rectangle2D.Double(box.x, box.y, box.width, box.height);
		st.button(g, shape, PIN_PARTY, pinned.getAsBoolean(), false, ctx.hoverOf(WindowPages.ID_PIN_PARTY),
			ctx.pressOf(WindowPages.ID_PIN_PARTY));
		if (ctx.focused && WindowPages.ID_PIN_PARTY.equals(ctx.highlight))
			st.focusRing(g, Slant.shape(shape, st.metrics().chipSlant));
	}

	private void banner(Graphics2D g, PageContext ctx, PartyLayout layout, PageText text, Font bold, Font small)
	{
		WindowStyle st = ctx.style;
		int pad = st.metrics().pad;
		Rectangle box = layout.banner;
		st.panel(g, new Rectangle2D.Double(box.x, box.y, box.width, box.height));
		FontMetrics bm = g.getFontMetrics(bold);
		Rectangle title = inner(box, pad, bm.getHeight());
		text.draw(g, BANNER, title, bold, ThemeColor.TEXT, PageText.Align.LEFT);
		int top = title.y + bm.getHeight() + pad / 4;
		Rectangle note = new Rectangle(title.x, top, title.width, Math.max(0, layout.resetButton.y - pad / 4 - top));
		text.drawWrapped(g, "Begin a fresh save.", note, small, ThemeColor.SUBTEXT);
		button(g, ctx, layout.resetButton, "Reset", WindowPages.ID_RESET_PROGRESS);
		button(g, ctx, layout.dismissButton, "Dismiss", WindowPages.ID_DISMISS);
	}

	private void button(Graphics2D g, PageContext ctx, Rectangle box, String label, String id)
	{
		WindowStyle st = ctx.style;
		Rectangle2D shape = new Rectangle2D.Double(box.x, box.y, box.width, box.height);
		st.button(g, shape, label, false, false, ctx.hoverOf(id), ctx.pressOf(id));
		if (ctx.focused && id.equals(ctx.highlight))
			st.focusRing(g, Slant.shape(shape, st.metrics().chipSlant));
	}

	private static Rectangle inner(Rectangle box, int pad, int height)
	{
		return new Rectangle(box.x + pad, box.y + pad / 2, Math.max(0, box.width - 2 * pad), height);
	}
}
