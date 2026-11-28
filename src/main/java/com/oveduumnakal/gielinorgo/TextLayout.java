/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text laid out to a width: word wrap by measured width, an ellipsis when a line limit cuts the text, and the small
 * markup the change log uses. Markup is {@code #}, {@code ##} and {@code ###} headings, {@code -} bullets (a bullet's
 * following plain lines join it), {@code **bold**}, {@code [text](url)} links and blank lines. Every line has the same
 * height, so the lines can be the rows of a {@link ScrollList}. A link is a hit area that reports its url; opening
 * it is the caller's job (through {@code LinkBrowser}), so nothing here touches RuneLite. The layout is made once for a
 * width and font and then only painted, and painting draws only the lines inside the clip.
 */
final class TextLayout
{
	private static final Pattern LINK = Pattern.compile("^\\[([^\\]]+)]\\(((?:[^()\\s]|\\([^()\\s]*\\))+)\\)");

	private final List<Line> lines;
	private final int lineHeight;
	private final int ascent;
	private final int width;
	private final boolean truncated;
	private final int bulletSize;

	private TextLayout(List<Line> lines, FontMetrics regular, FontMetrics bold, int width, boolean truncated)
	{
		this.lines = Collections.unmodifiableList(lines);
		this.lineHeight = Math.max(regular.getHeight(), bold.getHeight());
		this.ascent = Math.max(regular.getAscent(), bold.getAscent());
		this.width = width;
		this.truncated = truncated;
		this.bulletSize = Math.max(3, regular.getHeight() / 4);
	}

	/**
	 * Lays out text with markup.
	 *
	 * @param markup the text, with the change log markup
	 * @param regular the measures of the regular font
	 * @param bold the measures of the bold font
	 * @param width the room in pixels; at least one character is placed on every line, so a tiny width still ends
	 * @param maxLines the most lines to keep, or 0 for no limit; a longer text ends its last line with "..."
	 * @return the layout
	 */
	static TextLayout markup(String markup, FontMetrics regular, FontMetrics bold, int width, int maxLines)
	{
		return build(blocks(markup, true), regular, bold, width, maxLines);
	}

	/**
	 * Lays out plain text: line breaks are kept, nothing is read as markup.
	 *
	 * @param text the text
	 * @param font the measures of the font
	 * @param width the room in pixels
	 * @param maxLines the most lines to keep, or 0 for no limit; a longer text ends its last line with "..."
	 * @return the layout
	 */
	static TextLayout plain(String text, FontMetrics font, int width, int maxLines)
	{
		return build(blocks(text, false), font, font, width, maxLines);
	}

	/**
	 * Cuts a text to a width, ending with an ellipsis when it does not fit.
	 *
	 * @param s the text
	 * @param fm the measures of the font it is drawn in
	 * @param maxWidth the room in pixels
	 * @return the text if it fits, else its start and "..."
	 */
	static String ellipsis(String s, FontMetrics fm, int maxWidth)
	{
		return WindowStyle.ellipsize(s, fm, maxWidth);
	}

	/** @return how many lines the layout has */
	int lineCount()
	{
		return lines.size();
	}

	/** @return the height of every line in pixels, so the lines can be the rows of a scroll list */
	int lineHeight()
	{
		return lineHeight;
	}

	/** @return the height of the whole text in pixels */
	int height()
	{
		return lines.size() * lineHeight;
	}

	/** @return the width the text was laid out to */
	int width()
	{
		return width;
	}

	/** @return whether a line limit cut the text and the last line ends with an ellipsis */
	boolean truncated()
	{
		return truncated;
	}

	/**
	 * @param index the line, from 0
	 * @return the text of the line as plain characters, markup removed
	 */
	String lineText(int index)
	{
		StringBuilder out = new StringBuilder();
		for (Run run : lines.get(index).runs)
		{
			if (run.spaceBefore && out.length() > 0)
				out.append(' ');

			out.append(run.text);
		}

		return out.toString();
	}

	/**
	 * @param index the line, from 0
	 * @return whether any run on the line is drawn bold
	 */
	boolean hasBold(int index)
	{
		for (Run run : lines.get(index).runs)
			if (run.bold)
				return true;

		return false;
	}

	/**
	 * @param index the line, from 0
	 * @return whether the line is a heading
	 */
	boolean isHeading(int index)
	{
		return lines.get(index).heading;
	}

	/**
	 * @param index the line, from 0
	 * @return whether the line starts a bullet
	 */
	boolean isBullet(int index)
	{
		return lines.get(index).bullet;
	}

	/**
	 * @param index the line, from 0
	 * @return the right edge of the line's last run, in pixels from the text's left edge; 0 for a blank line
	 */
	int lineWidth(int index)
	{
		List<Run> runs = lines.get(index).runs;
		if (runs.isEmpty())
			return 0;

		Run last = runs.get(runs.size() - 1);
		return last.x + last.width;
	}

	/**
	 * Gives the hit areas of the links.
	 *
	 * @param originX the x of the text's left edge
	 * @param originY the y of the text's top edge
	 * @return one hit area per link on a line; a link that wraps has one per line
	 */
	List<Link> links(int originX, int originY)
	{
		List<Link> out = new ArrayList<>();
		for (int i = 0; i < lines.size(); i++)
			linksOn(i, originX, originY + i * lineHeight, out);

		return out;
	}

	/**
	 * Finds the link under a point.
	 *
	 * @param px the point's x
	 * @param py the point's y
	 * @param originX the x of the text's left edge
	 * @param originY the y of the text's top edge
	 * @return the link's url, or null when the point is not on a link
	 */
	String linkAt(int px, int py, int originX, int originY)
	{
		int index = Math.floorDiv(py - originY, lineHeight);
		if (index < 0 || index >= lines.size())
			return null;

		List<Link> out = new ArrayList<>();
		linksOn(index, originX, originY + index * lineHeight, out);
		for (Link link : out)
			if (link.box.contains(px, py))
				return link.url;

		return null;
	}

	/**
	 * Paints the lines that lie inside the graphics' clip (every line when there is no clip).
	 *
	 * @param g the graphics
	 * @param style the style, for colours and sizes
	 * @param x the left edge of the text
	 * @param y the top edge of the text
	 * @param color the colour of normal text
	 */
	void paint(Graphics2D g, WindowStyle style, int x, int y, ThemeColor color)
	{
		Rectangle clip = g.getClipBounds();
		for (int i = 0; i < lines.size(); i++)
		{
			int top = y + i * lineHeight;
			if (clip != null && (top + lineHeight < clip.y || top > clip.y + clip.height))
				continue;

			paintLine(g, style, i, x, top, color);
		}
	}

	/**
	 * Paints one line, for a scroll list that draws its rows itself.
	 *
	 * @param g the graphics
	 * @param style the style, for colours and sizes
	 * @param index the line, from 0
	 * @param x the left edge of the text
	 * @param top the top edge of the line
	 * @param color the colour of normal text
	 */
	void paintLine(Graphics2D g, WindowStyle style, int index, int x, int top, ThemeColor color)
	{
		Line line = lines.get(index);
		int baseline = top + ascent;
		if (line.bullet)
		{
			g.setColor(style.color(color));
			int dot = bulletSize;
			g.fillOval(x + line.indent / 2 - dot / 2, baseline - (ascent + dot) / 2 - 1, dot, dot);
		}

		for (Run run : line.runs)
		{
			ThemeColor colour = run.url != null ? ThemeColor.ACCENT : color;
			style.text(g, run.text, x + run.x, baseline, run.font, colour);
			if (run.url != null)
			{
				g.setColor(style.color(ThemeColor.ACCENT));
				g.fillRect(x + run.x, baseline + 1, run.width, Math.max(1, style.metrics().border / 2));
			}
		}
	}

	private void linksOn(int index, int originX, int top, List<Link> out)
	{
		Run first = null;
		Run last = null;
		for (Run run : lines.get(index).runs)
		{
			if (first != null && run.url != null && run.url.equals(first.url))
			{
				last = run;
				continue;
			}

			if (first != null)
				out.add(link(first, last, originX, top));

			first = run.url == null ? null : run;
			last = first;
		}

		if (first != null)
			out.add(link(first, last, originX, top));
	}

	private Link link(Run first, Run last, int originX, int top)
	{
		return new Link(new Rectangle(originX + first.x, top, last.x + last.width - first.x, lineHeight), first.url);
	}

	private static TextLayout build(List<Block> blocks, FontMetrics regular, FontMetrics bold, int width,
		int maxLines)
	{
		List<Line> lines = new ArrayList<>();
		int indent = regular.getHeight();
		for (Block block : blocks)
			wrap(block, regular, bold, Math.max(1, width), indent, lines);

		boolean cut = maxLines > 0 && lines.size() > maxLines;
		if (cut)
		{
			List<Line> kept = new ArrayList<>(lines.subList(0, maxLines));
			kept.set(maxLines - 1, withEllipsis(kept.get(maxLines - 1), regular, Math.max(1, width)));
			lines = kept;
		}

		return new TextLayout(lines, regular, bold, width, cut);
	}

	private static Line withEllipsis(Line line, FontMetrics regular, int width)
	{
		int avail = width;
		int dots = regular.stringWidth("...");
		List<Run> runs = new ArrayList<>(line.runs);
		while (!runs.isEmpty())
		{
			Run last = runs.get(runs.size() - 1);
			if (last.x + last.width + dots <= avail)
				break;

			String text = last.text;
			String shorter = text.length() > 1 ? text.substring(0, text.length() - 1) : "";
			runs.remove(runs.size() - 1);
			if (!shorter.isEmpty())
				runs.add(last.withText(shorter, last.fm.stringWidth(shorter)));
		}

		if (runs.isEmpty())
		{
			Run only = line.runs.isEmpty() ? null : line.runs.get(0);
			FontMetrics fm = only == null ? regular : only.fm;
			runs.add(new Run("...", false, null, false, 0, fm.stringWidth("..."), fm));
			return new Line(runs, line.heading, line.bullet, line.indent);
		}

		Run last = runs.get(runs.size() - 1);
		runs.set(runs.size() - 1, last.withText(last.text + "...", last.fm.stringWidth(last.text + "...")));
		return new Line(runs, line.heading, line.bullet, line.indent);
	}

	private static void wrap(Block block, FontMetrics regular, FontMetrics bold, int width, int indentUnit,
		List<Line> out)
	{
		if (block.words.isEmpty())
		{
			out.add(new Line(new ArrayList<>(), false, false, 0));
			return;
		}

		int indent = block.bullet ? indentUnit : 0;
		int room = Math.max(1, width - indent);
		List<Run> runs = new ArrayList<>();
		boolean startsBullet = block.bullet;
		int x = indent;
		for (Word word : block.words)
		{
			FontMetrics fm = word.bold || block.heading ? bold : regular;
			String rest = word.text;
			boolean firstPiece = true;
			while (!rest.isEmpty())
			{
				int space = word.spaceBefore && firstPiece && x > indent ? regular.charWidth(' ') : 0;
				if (x > indent && x + space + fm.stringWidth(rest) > width)
				{
					out.add(new Line(runs, block.heading, startsBullet, indent));
					startsBullet = false;
					runs = new ArrayList<>();
					x = indent;
					space = 0;
				}

				String piece = fm.stringWidth(rest) > room ? fit(rest, fm, room) : rest;
				int pieceWidth = fm.stringWidth(piece);
				runs.add(new Run(piece, word.bold || block.heading, word.url, space > 0, x + space, pieceWidth, fm));
				x += space + pieceWidth;
				rest = rest.substring(piece.length());
				firstPiece = false;
				if (!rest.isEmpty())
				{
					out.add(new Line(runs, block.heading, startsBullet, indent));
					startsBullet = false;
					runs = new ArrayList<>();
					x = indent;
				}
			}
		}

		if (!runs.isEmpty())
			out.add(new Line(runs, block.heading, startsBullet, indent));
	}

	private static String fit(String word, FontMetrics fm, int avail)
	{
		int end = 1;
		while (end < word.length() && fm.stringWidth(word.substring(0, end + 1)) <= avail)
			end++;

		return word.substring(0, end);
	}

	private static List<Block> blocks(String text, boolean markup)
	{
		List<Block> out = new ArrayList<>();
		Block bullet = null;
		boolean blank = true;
		for (String raw : text.split("\n", -1))
		{
			String line = markup ? raw.trim() : raw.replaceAll("\\s+$", "");
			boolean plainText = !line.isEmpty() && !line.startsWith("-") && !line.startsWith("#");
			if (markup && bullet != null && plainText)
			{
				List<Word> more = words(line, true, false, null);
				if (!more.isEmpty())
					more.set(0, more.get(0).withSpace(true));

				bullet.words.addAll(more);
				continue;
			}

			bullet = null;
			if (line.isEmpty())
			{
				if (!markup || !blank)
					out.add(new Block(false, false));

				blank = true;
				continue;
			}

			blank = false;
			if (!markup)
			{
				Block block = new Block(false, false);
				block.words.addAll(words(line, false, false, null));
				out.add(block);
			}
			else if (line.startsWith("#"))
			{
				Block block = new Block(true, false);
				block.words.addAll(words(line.replaceFirst("^#+", "").trim(), true, true, null));
				out.add(block);
			}
			else if (line.startsWith("-"))
			{
				bullet = new Block(false, true);
				bullet.words.addAll(words(line.substring(1).trim(), true, false, null));
				out.add(bullet);
			}
			else
			{
				Block block = new Block(false, false);
				block.words.addAll(words(line, true, false, null));
				out.add(block);
			}
		}

		while (!out.isEmpty() && out.get(out.size() - 1).words.isEmpty())
			out.remove(out.size() - 1);

		return out;
	}

	private static List<Word> words(String text, boolean markup, boolean allBold, String url)
	{
		Words words = new Words(allBold, url);
		int i = 0;
		while (i < text.length())
		{
			char c = text.charAt(i);
			Matcher link = markup && c == '[' ? LINK.matcher(text.substring(i)) : null;
			if (markup && text.startsWith("**", i))
			{
				words.flush();
				words.starBold = !words.starBold;
				i += 2;
			}
			else if (link != null && link.find())
			{
				words.flush();
				List<Word> inner = words(link.group(1), false, allBold || words.starBold, link.group(2));
				if (!inner.isEmpty())
					inner.set(0, inner.get(0).withSpace(words.space));

				words.out.addAll(inner);
				words.space = false;
				i += link.end();
			}
			else
			{
				if (Character.isWhitespace(c))
				{
					words.flush();
					words.space = true;
				}
				else
				{
					words.word.append(c);
				}

				i++;
			}
		}

		words.flush();
		return words.out;
	}

	/** Collects the words of a line while it is scanned, with the bold state and a space waiting before the next. */
	private static final class Words
	{
		private final List<Word> out = new ArrayList<>();
		private final StringBuilder word = new StringBuilder();
		private final boolean allBold;
		private final String url;
		private boolean starBold;
		private boolean space;

		Words(boolean allBold, String url)
		{
			this.allBold = allBold;
			this.url = url;
		}

		void flush()
		{
			if (word.length() == 0)
				return;

			out.add(new Word(word.toString(), allBold || starBold, url, space));
			word.setLength(0);
			space = false;
		}
	}

	/** A hit area of a link on the painted text. */
	static final class Link
	{
		/** The area, in the same coordinates as the origin given to {@link TextLayout#links}. */
		final Rectangle box;

		/** Where the link goes. */
		final String url;

		Link(Rectangle box, String url)
		{
			this.box = box;
			this.url = url;
		}
	}

	/** One word or word piece with its style, before it is placed. */
	private static final class Word
	{
		private final String text;
		private final boolean bold;
		private final String url;
		private final boolean spaceBefore;

		Word(String text, boolean bold, String url, boolean spaceBefore)
		{
			this.text = text;
			this.bold = bold;
			this.url = url;
			this.spaceBefore = spaceBefore;
		}

		Word withSpace(boolean space)
		{
			return new Word(text, bold, url, space);
		}
	}

	/** A paragraph, heading or bullet, as words. */
	private static final class Block
	{
		private final List<Word> words = new ArrayList<>();
		private final boolean heading;
		private final boolean bullet;

		Block(boolean heading, boolean bullet)
		{
			this.heading = heading;
			this.bullet = bullet;
		}
	}

	/** A placed piece of text on a line. */
	private static final class Run
	{
		private final String text;
		private final boolean bold;
		private final String url;
		private final boolean spaceBefore;
		private final int x;
		private final int width;
		private final FontMetrics fm;
		private final Font font;

		Run(String text, boolean bold, String url, boolean spaceBefore, int x, int width, FontMetrics fm)
		{
			this.text = text;
			this.bold = bold;
			this.url = url;
			this.spaceBefore = spaceBefore;
			this.x = x;
			this.width = width;
			this.fm = fm;
			this.font = fm.getFont();
		}

		Run withText(String newText, int newWidth)
		{
			return new Run(newText, bold, url, spaceBefore, x, newWidth, fm);
		}
	}

	/** One wrapped line: its runs, whether it is a heading, and a bullet's hanging indent. */
	private static final class Line
	{
		private final List<Run> runs;
		private final boolean heading;
		private final boolean bullet;
		private final int indent;

		Line(List<Run> runs, boolean heading, boolean bullet, int indent)
		{
			this.runs = runs;
			this.heading = heading;
			this.bullet = bullet;
			this.indent = indent;
		}
	}
}
