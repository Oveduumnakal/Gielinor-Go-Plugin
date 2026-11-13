/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

/**
 * Turns the small subset of markdown that {@code changelog.md} uses into the HTML a Swing label shows: two levels of
 * heading, bullets with wrapped lines, paragraphs and bold text. Pure, so it is easy to test.
 */
final class ChangelogHtml
{
	private ChangelogHtml()
	{
	}

	/**
	 * Renders one release.
	 *
	 * @param release the release
	 * @param width the width the text is laid out to, in HTML pixels
	 * @return a complete HTML text for a label
	 */
	static String render(Changelog.Release release, int width)
	{
		String date = release.date().isEmpty() ? "" : " - " + PanelUi.escape(release.date());
		StringBuilder html = new StringBuilder("<html><div style='width:" + width + "px'>");
		html.append("<b>" + PanelUi.escape(release.version()) + "</b>" + date + "<br>");
		String bullet = null;
		for (String raw : release.body().split("\n", -1))
		{
			String line = raw.trim();
			if (bullet != null && !line.isEmpty() && !line.startsWith("-") && !line.startsWith("#"))
			{
				bullet = bullet + " " + line;
				continue;
			}

			if (bullet != null)
				html.append("&bull; " + inline(bullet) + "<br>");

			bullet = null;
			if (line.startsWith("###"))
				html.append("<br><b>" + inline(line.substring(3).trim()) + "</b><br>");
			else if (line.startsWith("##"))
				html.append("<br><u><b>" + inline(line.substring(2).trim()) + "</b></u><br>");
			else if (line.startsWith("-"))
				bullet = line.substring(1).trim();
			else if (!line.isEmpty())
				html.append(inline(line) + "<br>");
		}

		if (bullet != null)
			html.append("&bull; " + inline(bullet) + "<br>");

		html.append("</div></html>");
		return html.toString();
	}

	/**
	 * Escapes a line and turns each pair of double asterisks into bold.
	 *
	 * @param text the markdown line
	 * @return HTML for the line
	 */
	static String inline(String text)
	{
		String safe = PanelUi.escape(text);
		StringBuilder out = new StringBuilder();
		boolean bold = false;
		int at = 0;
		int next = safe.indexOf("**", at);
		while (next >= 0)
		{
			out.append(safe, at, next).append(bold ? "</b>" : "<b>");
			bold = !bold;
			at = next + 2;
			next = safe.indexOf("**", at);
		}

		out.append(safe.substring(at));
		if (bold)
			out.append("</b>");

		return out.toString();
	}
}
