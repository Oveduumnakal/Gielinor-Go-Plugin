/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A small Markdown table for the balance report: a header, then rows of cells, numbers written the same way. */
final class BalanceTable
{
	private final List<String> head = new ArrayList<>();
	private final List<List<String>> rows = new ArrayList<>();

	/**
	 * Starts a table.
	 *
	 * @param columns the column headings
	 */
	BalanceTable(String... columns)
	{
		for (String column : columns)
			head.add(column);
	}

	/**
	 * Adds a row.
	 *
	 * @param cells the cells, each written with {@link #cell(Object)}
	 * @return this table
	 */
	BalanceTable row(Object... cells)
	{
		List<String> row = new ArrayList<>();
		for (Object cell : cells)
			row.add(cell(cell));

		rows.add(row);
		return this;
	}

	/** @return how many rows the table has */
	int size()
	{
		return rows.size();
	}

	/** @return the table as Markdown, ending in a blank line */
	@Override
	public String toString()
	{
		StringBuilder out = new StringBuilder();
		line(out, head);
		List<String> rule = new ArrayList<>();
		for (int i = 0; i < head.size(); i++)
			rule.add("---");

		line(out, rule);
		for (List<String> row : rows)
			line(out, row);

		return out.append('\n').toString();
	}

	private static void line(StringBuilder out, List<String> cells)
	{
		out.append('|');
		for (String cell : cells)
			out.append(" " + cell + " |");

		out.append('\n');
	}

	/**
	 * Writes one cell: a whole number with thousands separators, a decimal to three significant figures, anything else
	 * as its text.
	 *
	 * @param value the value
	 * @return the text
	 */
	static String cell(Object value)
	{
		if (value instanceof Double)
			return number((Double) value);

		if (value instanceof Integer || value instanceof Long)
			return String.format(Locale.ROOT, "%,d", ((Number) value).longValue());

		return String.valueOf(value);
	}

	/**
	 * Writes a decimal with the number of decimals its size needs: 3 digits kept for small values, none for large.
	 *
	 * @param value the value
	 * @return the text, {@code n/a} for a value that is not a number or infinite
	 */
	static String number(double value)
	{
		if (Double.isNaN(value) || Double.isInfinite(value))
			return "n/a";

		double size = Math.abs(value);
		if (size >= 1000)
			return String.format(Locale.ROOT, "%,.0f", value);

		if (size >= 100)
			return String.format(Locale.ROOT, "%.0f", value);

		if (size >= 10)
			return String.format(Locale.ROOT, "%.1f", value);

		if (size >= 1)
			return String.format(Locale.ROOT, "%.2f", value);

		return String.format(Locale.ROOT, "%.3f", value);
	}

	/**
	 * Writes a share as a percentage.
	 *
	 * @param share the share, 1 meaning 100%
	 * @return the text, such as {@code 37.5%}
	 */
	static String pct(double share)
	{
		return String.format(Locale.ROOT, "%.1f%%", share * 100.0);
	}

	/**
	 * Writes a length of time.
	 *
	 * @param minutes the minutes
	 * @return the text: minutes under two hours, hours above that
	 */
	static String minutes(double minutes)
	{
		if (Double.isNaN(minutes) || Double.isInfinite(minutes))
			return "n/a";

		if (minutes < 120.0)
			return number(minutes) + " min";

		return number(minutes / 60.0) + " h";
	}
}
