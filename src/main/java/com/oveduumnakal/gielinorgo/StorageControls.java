/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Where the Storage page's controls sit, and their words. The first line holds Select, Filter, Sort (and later a View
 * button) and the heading; the second line, the old hint line, holds the filter chips or the hint on the left and
 * the Select-mode buttons on the right. Pure layout: it takes pixel metrics and returns rectangles, so it is tested
 * without a canvas. Holds no RuneLite types.
 */
final class StorageControls
{
	/** The width of the Select button, in layout units. */
	static final double SELECT_UNITS = 90;

	/** The width of the Filter button, in layout units. */
	static final double FILTER_UNITS = 104;

	/** The width of the Sort button, in layout units. */
	static final double SORT_UNITS = 168;

	/** The width of the View button the Storage views add, in layout units. */
	static final double VIEW_UNITS = 104;

	/** The width of the Tick all shown button, in layout units. */
	static final double TICK_ALL_UNITS = 150;

	/** The width of the Release selected button, in layout units. */
	static final double RELEASE_UNITS = 190;

	/** The width of the Clear chip, in layout units. */
	static final double CLEAR_UNITS = 60;

	/** The width of a chip's remove cross, in layout units. */
	static final double REMOVE_UNITS = 22;

	/** The height of the second line, in layout units. */
	static final double LINE_UNITS = 28;

	/** The text of the chip that stands in for the chips that do not fit; {@code %d} is how many. */
	static final String MORE_FORMAT = "+%d more";

	private StorageControls()
	{
	}

	/** One filter chip: the label that flips it and the cross that removes it. */
	static final class Chip
	{
		private final int index;
		private final Rectangle label;
		private final Rectangle remove;

		/**
		 * @param index the criterion's place in the query
		 * @param label the label's box
		 * @param remove the cross's box
		 */
		Chip(int index, Rectangle label, Rectangle remove)
		{
			this.index = index;
			this.label = label;
			this.remove = remove;
		}

		/** @return the criterion's place in the query */
		int index()
		{
			return index;
		}

		/** @return the label's box; a press flips the filter between shown and hidden */
		Rectangle label()
		{
			return new Rectangle(label);
		}

		/** @return the cross's box; a press removes the filter */
		Rectangle remove()
		{
			return new Rectangle(remove);
		}
	}

	/** Where everything sits. Boxes that do not show are empty. */
	static final class Layout
	{
		private final Rectangle select;
		private final Rectangle filter;
		private final Rectangle sort;
		private final Rectangle view;
		private final Rectangle heading;
		private final Rectangle strip;
		private final Rectangle tickAll;
		private final Rectangle release;
		private final List<Chip> chips;
		private final Rectangle more;
		private final int moreCount;
		private final Rectangle clear;
		private final int bottom;

		Layout(Rectangle[] boxes, List<Chip> chips, int moreCount, int bottom)
		{
			this.select = boxes[0];
			this.filter = boxes[1];
			this.sort = boxes[2];
			this.view = boxes[3];
			this.heading = boxes[4];
			this.strip = boxes[5];
			this.tickAll = boxes[6];
			this.release = boxes[7];
			this.more = boxes[8];
			this.clear = boxes[9];
			this.chips = Collections.unmodifiableList(chips);
			this.moreCount = moreCount;
			this.bottom = bottom;
		}

		/** @return the Select (or Done) button */
		Rectangle select()
		{
			return new Rectangle(select);
		}

		/** @return the Filter button */
		Rectangle filter()
		{
			return new Rectangle(filter);
		}

		/** @return the Sort button */
		Rectangle sort()
		{
			return new Rectangle(sort);
		}

		/** @return the View button, or an empty box when the page has none */
		Rectangle view()
		{
			return new Rectangle(view);
		}

		/** @return the heading's box */
		Rectangle heading()
		{
			return new Rectangle(heading);
		}

		/** @return the left part of the second line, where the chips or the hint go */
		Rectangle strip()
		{
			return new Rectangle(strip);
		}

		/** @return the Tick all shown button, or an empty box outside Select mode */
		Rectangle tickAll()
		{
			return new Rectangle(tickAll);
		}

		/** @return the Release selected button, or an empty box outside Select mode */
		Rectangle release()
		{
			return new Rectangle(release);
		}

		/** @return the chips that fit, in the query's order */
		List<Chip> chips()
		{
			return chips;
		}

		/** @return the box of the {@code +N more} note, or an empty box when every chip fits */
		Rectangle more()
		{
			return new Rectangle(more);
		}

		/** @return how many chips did not fit */
		int moreCount()
		{
			return moreCount;
		}

		/** @return the Clear chip, or an empty box when there is none or no room */
		Rectangle clear()
		{
			return new Rectangle(clear);
		}

		/** @return the y where the list may start */
		int listTop()
		{
			return bottom;
		}
	}

	/**
	 * @param count how many chips did not fit
	 * @return the words of the {@code +N more} note
	 */
	static String moreText(int count)
	{
		return String.format(MORE_FORMAT, count);
	}

	/**
	 * Works out where every control sits in the page body.
	 *
	 * @param body the page body
	 * @param style the style
	 * @param chipFont the measures of the font chip labels are drawn in
	 * @param selecting whether Select mode is on
	 * @param viewButton whether the page has a View button, left of Sort
	 * @param query the sort and filters, whose criteria become chips
	 * @return the layout
	 */
	static Layout layout(Rectangle body, WindowStyle style, FontMetrics chipFont, boolean selecting,
		boolean viewButton, StorageQuery query)
	{
		WindowStyle.Metrics m = style.metrics();
		int gap = Math.max(1, m.pad / 2);
		int y = body.y + m.pad;
		int left = body.x + m.pad;
		int right = body.x + body.width - m.pad;
		Rectangle[] boxes = new Rectangle[10];
		for (int i = 0; i < boxes.length; i++)
			boxes[i] = new Rectangle();

		int edge = right;
		edge -= style.px(SELECT_UNITS);
		boxes[0] = new Rectangle(edge, y, style.px(SELECT_UNITS), m.chipHeight);
		edge -= gap + style.px(FILTER_UNITS);
		boxes[1] = new Rectangle(edge, y, style.px(FILTER_UNITS), m.chipHeight);
		edge -= gap + style.px(SORT_UNITS);
		boxes[2] = new Rectangle(edge, y, style.px(SORT_UNITS), m.chipHeight);
		if (viewButton)
		{
			edge -= gap + style.px(VIEW_UNITS);
			boxes[3] = new Rectangle(edge, y, style.px(VIEW_UNITS), m.chipHeight);
		}

		boxes[4] = new Rectangle(left, y, Math.max(0, edge - gap - left), m.chipHeight);
		int lineY = y + m.chipHeight + gap;
		int line = style.px(LINE_UNITS);
		int stripRight = right;
		if (selecting)
		{
			int releaseW = style.px(RELEASE_UNITS);
			int tickW = style.px(TICK_ALL_UNITS);
			boxes[7] = new Rectangle(right - releaseW, lineY, releaseW, line);
			boxes[6] = new Rectangle(boxes[7].x - gap - tickW, lineY, tickW, line);
			stripRight = boxes[6].x - gap;
		}

		boxes[5] = new Rectangle(left, lineY, Math.max(0, stripRight - left), line);
		List<Chip> chips = new ArrayList<>();
		int moreCount = placeChips(boxes, chips, style, chipFont, query, gap);
		return new Layout(boxes, chips, moreCount, lineY + line + gap);
	}

	private static int placeChips(Rectangle[] boxes, List<Chip> chips, WindowStyle style, FontMetrics font,
		StorageQuery query, int gap)
	{
		List<StorageQuery.Criterion> criteria = query.criteria();
		if (criteria.isEmpty())
			return 0;

		WindowStyle.Metrics m = style.metrics();
		Rectangle strip = boxes[5];
		int clearW = style.px(CLEAR_UNITS);
		int removeW = style.px(REMOVE_UNITS);
		int chipY = strip.y + (strip.height - m.chipHeight) / 2;
		int budget = strip.width - clearW - gap;
		int x = strip.x;
		int used = 0;
		for (int i = 0; i < criteria.size(); i++)
		{
			int labelW = font.stringWidth(criteria.get(i).label()) + 2 * m.chipPad;
			int need = labelW + removeW + (i == 0 ? 0 : gap);
			if (used + need > budget)
				break;

			chips.add(new Chip(i, new Rectangle(x + used + (i == 0 ? 0 : gap), chipY, labelW, m.chipHeight),
				new Rectangle(x + used + (i == 0 ? 0 : gap) + labelW, chipY, removeW, m.chipHeight)));
			used += need;
		}

		int hidden = criteria.size() - chips.size();
		if (hidden > 0)
		{
			while (!chips.isEmpty() && !moreFits(font, m, hidden, used, budget, gap))
			{
				Chip last = chips.remove(chips.size() - 1);
				used = last.label.x - strip.x - (chips.isEmpty() ? 0 : gap);
				hidden++;
			}

			int moreW = font.stringWidth(moreText(hidden)) + 2 * m.chipPad;
			int moreX = strip.x + used + (chips.isEmpty() ? 0 : gap);
			if (moreX + moreW <= strip.x + strip.width)
			{
				boxes[8] = new Rectangle(moreX, chipY, moreW, m.chipHeight);
				used = moreX + moreW - strip.x;
			}
		}

		int clearX = strip.x + used + gap;
		if (clearX + clearW <= strip.x + strip.width)
			boxes[9] = new Rectangle(clearX, chipY, clearW, m.chipHeight);

		return hidden;
	}

	private static boolean moreFits(FontMetrics font, WindowStyle.Metrics m, int hidden, int used, int budget,
		int gap)
	{
		return used + gap + font.stringWidth(moreText(hidden)) + 2 * m.chipPad <= budget;
	}
}
