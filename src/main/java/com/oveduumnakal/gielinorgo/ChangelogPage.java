/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * The change log inside the painted window: every release, newest first, as wrapped text in a {@link ScrollList}
 * with a Back button above it. It replaces the page area while it is open, so it is a shell-level page, not a tab.
 * An issue number such as {@code #17} is a link to that issue on GitHub; pressing a link and releasing on the same
 * link hands its address to the opener once, and a press that turned into a scroll opens nothing. Holds no RuneLite
 * types.
 */
final class ChangelogPage
{
	/** The id of the Back button's hit. */
	static final String ID_BACK = "btn:changelog-back";

	/** The text shown when the change log has no releases. */
	static final String EMPTY = "The change log could not be loaded.";

	private static final Pattern ISSUE = Pattern.compile("(?<![\\w&\\[#])#(\\d+)");
	private static final String ISSUES = IssueLinks.NEW_ISSUE.replace("/issues/new", "/issues/");
	private static final double BACK_UNITS = 90;
	private static final double TOP_UNITS = 36;

	private final String markup;
	private final ScrollList list = new ScrollList();
	private TextLayout text;
	private Font laidFont;
	private int laidWidth = -1;
	private int textX;
	private String pendingLink;

	/**
	 * Creates the page.
	 *
	 * @param changelog the releases to show
	 */
	ChangelogPage(Changelog changelog)
	{
		this.markup = markup(changelog);
	}

	/**
	 * Writes the releases as the markup {@link TextLayout} reads, with each issue number made a link.
	 *
	 * @param changelog the releases
	 * @return the markup
	 */
	static String markup(Changelog changelog)
	{
		if (changelog.releases().isEmpty())
			return EMPTY;

		StringBuilder out = new StringBuilder();
		for (Changelog.Release release : changelog.releases())
		{
			String date = release.date().isEmpty() ? "" : " - " + release.date();
			out.append("# ").append(release.version());
			out.append(date).append("\n\n");
			for (String line : release.body().split("\n", -1))
				out.append(linkIssues(line)).append('\n');

			out.append('\n');
		}

		return out.toString().trim();
	}

	/**
	 * Turns each issue number in a line into a markdown link to the issue.
	 *
	 * @param line one markdown line
	 * @return the line, with {@code #17} as a link to issue 17; a heading line is left as it is
	 */
	static String linkIssues(String line)
	{
		if (line.trim().startsWith("#"))
			return line;

		return ISSUE.matcher(line).replaceAll("[#$1](" + ISSUES + "$1)");
	}

	/**
	 * Lists the page's hits: the Back button.
	 *
	 * @param style the style
	 * @param body the page area
	 * @return the hits, with a focus order from {@link WindowShell#PAGE_ORDER}
	 */
	List<Hit> hits(WindowStyle style, Rectangle body)
	{
		return Collections.singletonList(new Hit(ID_BACK, Hit.Kind.BUTTON, backBox(style, body),
			WindowShell.PAGE_ORDER, true, "Back to the window (Esc)"));
	}

	private static Rectangle backBox(WindowStyle style, Rectangle body)
	{
		int pad = style.metrics().pad;
		return new Rectangle(body.x + pad, body.y + pad, style.px(BACK_UNITS), style.px(TOP_UNITS));
	}

	/**
	 * Paints the page and lays the text out for this frame's size.
	 *
	 * @param g the graphics
	 * @param ctx what the frame needs
	 */
	void paint(Graphics2D g, PageContext ctx)
	{
		WindowStyle st = ctx.style;
		Rectangle body = ctx.body;
		int pad = st.metrics().pad;
		st.panel(g, new Rectangle2D.Double(body.x, body.y, body.width, body.height));
		Rectangle back = backBox(st, body);
		Rectangle2D shape = new Rectangle2D.Double(back.x, back.y, back.width, back.height);
		st.button(g, shape, "Back", false, false, ctx.hoverOf(ID_BACK), ctx.pressOf(ID_BACK));
		if (ctx.focused && ID_BACK.equals(ctx.highlight))
			st.focusRing(g, st.outline(WindowStyle.Kind.BUTTON, shape));

		Font title = st.font(WindowFonts.Face.BOLD, WindowFonts.TITLE_UNITS);
		FontMetrics tm = g.getFontMetrics(title);
		int baseline = back.y + (back.height + tm.getAscent() - tm.getDescent()) / 2;
		st.text(g, "Change log", back.x + back.width + pad, baseline, title, ThemeColor.TEXT);
		int top = back.y + back.height + pad;
		int bottom = body.y + body.height - pad;
		Rectangle view = new Rectangle(body.x + pad, top, body.width - 2 * pad, Math.max(0, bottom - top));
		Font regular = st.font(WindowFonts.Face.REGULAR, WindowFonts.BODY_UNITS);
		Font bold = st.font(WindowFonts.Face.BOLD, WindowFonts.BODY_UNITS);
		int bar = st.px(ScrollList.BAR_UNITS) + pad / 2;
		fit(g, regular, bold, Math.max(1, view.width - bar - pad));
		list.layout(view, text.lineHeight(), text.lineCount(), st);
		textX = view.x + pad;
		TextLayout lines = text;
		int x = textX;
		list.paint(g, st, (gr, index, box) -> lines.paintLine(gr, st, index, x, box.y, ThemeColor.TEXT));
	}

	private void fit(Graphics2D g, Font regular, Font bold, int width)
	{
		if (text != null && regular.equals(laidFont) && width == laidWidth)
			return;

		text = TextLayout.markup(markup, g.getFontMetrics(regular), g.getFontMetrics(bold), width, 0);
		laidFont = regular;
		laidWidth = width;
	}

	/**
	 * The wheel turned over the page.
	 *
	 * @param x the pointer's x in window pixels
	 * @param y the pointer's y in window pixels
	 * @param rotation the wheel's clicks
	 */
	void wheel(int x, int y, int rotation)
	{
		list.onWheel(x, y, rotation);
	}

	/**
	 * A button went down on the page.
	 *
	 * @param x the pointer's x in window pixels
	 * @param y the pointer's y in window pixels
	 * @return whether the press landed on the scroll bar, so it is not a click on what lies under it
	 */
	boolean pressed(int x, int y)
	{
		pendingLink = linkAt(x, y);
		boolean onBar = list.scrollable() && list.trackBounds().contains(x, y);
		boolean took = list.onPress(x, y);
		return onBar && took;
	}

	/**
	 * The pointer moved with the button down.
	 *
	 * @param x the pointer's x in window pixels
	 * @param y the pointer's y in window pixels
	 */
	void dragged(int x, int y)
	{
		list.onDrag(x, y);
	}

	/**
	 * The button came up. A press and release on the same link opens it, once.
	 *
	 * @param x the pointer's x in window pixels
	 * @param y the pointer's y in window pixels
	 * @param opener what opens an address
	 * @return whether the release was a scroll or a link, so it is not a click on a hit under it
	 */
	boolean released(int x, int y, Consumer<String> opener)
	{
		boolean scrolled = list.onRelease();
		String url = pendingLink;
		pendingLink = null;
		if (scrolled)
			return true;

		if (url != null && url.equals(linkAt(x, y)))
		{
			opener.accept(url);
			return true;
		}

		return false;
	}

	/**
	 * @param x the pointer's x in window pixels
	 * @param y the pointer's y in window pixels
	 * @return the address of the link under the point, or null
	 */
	String linkAt(int x, int y)
	{
		if (text == null || !list.viewport().contains(x, y))
			return null;

		return text.linkAt(x, y, textX, list.viewport().y - list.offset());
	}

	/** @return the scrolling list, which the tests drive */
	ScrollList list()
	{
		return list;
	}

	/** @return the laid out text, or null before the first paint */
	TextLayout text()
	{
		return text;
	}

	/** @return the x of the text's left edge as of the last paint */
	int textX()
	{
		return textX;
	}
}
