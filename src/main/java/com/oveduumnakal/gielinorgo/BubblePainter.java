/*
 * Copyright (c) 2026, Oveduumnakal
 * All rights reserved.
 */
package com.oveduumnakal.gielinorgo;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Supplier;

/**
 * Paints a {@link BubbleFrame}: the round action bubbles above a wild creature and the abbreviated card beside them.
 * It draws exactly what the frame says, in its position, size and opacity; the only state it keeps between frames is
 * the card's cached glass background ({@link GlassPanel}). The bubbles are world objects and have their own fixed
 * colours ({@link JarPalette}); the card follows the window theme of the {@link WindowStyle} it is given, on frosted
 * glass. Everything is drawn with vector shapes, bundled pictures and the kit's
 * fonts, and the graphics are left exactly as they were found. Holds no RuneLite types.
 */
final class BubblePainter
{
	/** The width of the card at scale 1 in layout units; it grows only to fit its chips. */
	static final int CARD_UNITS = 190;

	/** The widest the card grows to fit its chips, in layout units. */
	static final int CARD_MAX_UNITS = 250;

	/** The factor applied to the opacity of a greyed bubble. */
	static final double DISABLED_ALPHA = 0.8;

	/** The side, in the glyph's own units, of the square the glyphs are drawn in before they are scaled. */
	private static final int GLYPH_UNITS = 32;

	/** The side, in whole pixels at the glyph's natural size, of the grid the Battle and Info glyphs are drawn on. */
	private static final int GRID = 16;

	private static final double INNER_RATIO = 0.8;
	private static final double JAR_PX = 22.0;
	private static final double INNER_OPACITY = 0.1;
	private static final double SPRITE_SHRINK = 0.85;
	private static final double SPRITE_BOX = 1.6;
	private static final Color SWORD_GREY = new Color(0x8A9098);
	private static final double FULL_RADIUS = 13.5;
	private static final float DISABLED_SPRITE = 0.55f;

	/** The radius at which a jar picture is drawn at its natural size. */
	private static final double NATURAL_RADIUS = 15.0;

	private static final Color OUTLINE = new Color(0x141414);
	private static final Color INNER = new Color(0xF2F2F2);
	private static final Color GREY = new Color(0x808080);
	private static final Color DISABLED_GLYPH = new Color(0xD0D0D0);
	private static final Color INFO_GLYPH = new Color(0x0D2B3E);
	private static final Color INFO_HALO = new Color(0xF2F2F2);
	private static final Color KNOCKED_OUT = new Color(0xFFD23F);
	private static final Color BADGE = new Color(0, 0, 0, 199);
	private static final int BADGE_COLS = 3;
	private static final int BADGE_ROWS = 5;
	private static final double BADGE_UNIT_RADIUS = 7.5;
	private static final double BADGE_ANCHOR = 0.8;
	private static final String[][] BADGE_GLYPHS = {
		{"111", "101", "101", "101", "111"},
		{"010", "110", "010", "010", "111"},
		{"111", "001", "111", "100", "111"},
		{"111", "001", "111", "001", "111"},
		{"101", "101", "111", "001", "001"},
		{"111", "100", "111", "001", "111"},
		{"111", "100", "111", "101", "111"},
		{"111", "001", "001", "001", "001"},
		{"111", "101", "111", "101", "111"},
		{"111", "101", "111", "001", "111"},
		{"000", "010", "111", "010", "000"}
	};

	private final Supplier<BufferedImage> combatSprite;
	private final CardBackdrop backdrop;
	private final GlassPanel glass = new GlassPanel();

	/** Creates a painter that draws its own crossed swords on the Battle bubble. */
	BubblePainter()
	{
		this(() -> null);
	}

	/**
	 * Creates a painter. It holds the sprite lookup and the card's cached glass, so one can be kept for the whole
	 * session.
	 *
	 * @param combatSprite the game's combat tab picture for the Battle bubble, or a supplier of null while it is not
	 *        loaded, in which case the painter draws its own crossed swords
	 */
	BubblePainter(Supplier<BufferedImage> combatSprite)
	{
		this(combatSprite, CardBackdrop.NONE);
	}

	/**
	 * Creates a painter whose frosted-glass card can show a blurred picture of the scene behind it.
	 *
	 * @param combatSprite the game's combat tab picture for the Battle bubble, or a supplier of null while it is not
	 *        loaded, in which case the painter draws its own crossed swords
	 * @param backdrop gives the blurred scene behind the card; {@link CardBackdrop#NONE} for none
	 */
	BubblePainter(Supplier<BufferedImage> combatSprite, CardBackdrop backdrop)
	{
		this.combatSprite = combatSprite;
		this.backdrop = backdrop;
	}

	/** @return the glass the card is painted on; for the cost test */
	GlassPanel glass()
	{
		return glass;
	}

	/**
	 * Measures the card at scale 1: about {@value #CARD_UNITS} layout units wide, and as tall as its lines need: the
	 * name and level, the chips, the record when there is one, the four stats, the words when there are any, the odds
	 * and the knocked-out line when there is one.
	 *
	 * @param g the graphics, for the fonts' measures
	 * @param content the words, or null
	 * @param style the style, which sets the pixel sizes
	 * @return the size in pixels; a small default for null content
	 */
	Dimension cardSize(Graphics2D g, WildCardContent content, WindowStyle style)
	{
		if (content == null)
			return new Dimension(style.px(CARD_UNITS), style.px(40));

		int pad = style.px(8);
		int gap = style.px(4);
		int chips = 0;
		for (SummaryContent.Chip chip : content.chips())
			chips += style.chipWidth(g, chip.label()) + gap;

		int width = Math.max(style.px(CARD_UNITS), Math.min(style.px(CARD_MAX_UNITS), chips - gap + 2 * pad));
		int line = lineHeight(g, style);
		int lines = content.stats().size();
		int height = pad + style.px(24) + gap + style.metrics().chipHeight + gap;
		if (content.recordLabel() != null)
			height += line;

		height += lines * line;
		if (content.words() != null && !content.words().isEmpty())
			height += line;

		height += line;
		if (content.knockedOutLine() != null)
			height += line;

		return new Dimension(width, height + pad);
	}

	/**
	 * Paints the discs back to front, then the card. The transform, composite, clip, font, paint, stroke and rendering
	 * hints of the graphics are restored before it returns.
	 *
	 * @param g the graphics
	 * @param frame what to paint
	 * @param style the style the card is drawn in
	 * @param now the clock in milliseconds; reserved, since every animated number is already in the frame
	 */
	void paint(Graphics2D g, BubbleFrame frame, WindowStyle style, long now)
	{
		AffineTransform transform = g.getTransform();
		Composite composite = g.getComposite();
		Shape clip = g.getClip();
		Font font = g.getFont();
		Paint paint = g.getPaint();
		Stroke stroke = g.getStroke();
		RenderingHints hints = new RenderingHints(null);
		hints.putAll(g.getRenderingHints());
		try
		{
			WindowStyle.prepare(g);
			for (BubbleFrame.Disc disc : frame.discs())
				disc(g, disc, style);

			if (frame.card() != null)
				card(g, frame.card(), style);
		}
		finally
		{
			g.setTransform(transform);
			g.setComposite(composite);
			g.setClip(clip);
			g.setFont(font);
			g.setPaint(paint);
			g.setStroke(stroke);
			g.setRenderingHints(hints);
		}
	}

	private static double clamp(double v)
	{
		return Math.max(0.0, Math.min(1.0, v));
	}

	private static void alpha(Graphics2D g, double a)
	{
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) clamp(a)));
	}

	private static Color withAlpha(Color c, double a)
	{
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) Math.round(255 * clamp(a)));
	}

	private static Ellipse2D circle(double cx, double cy, double r)
	{
		return new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
	}

	private void disc(Graphics2D g, BubbleFrame.Disc d, WindowStyle style)
	{
		double r = d.painted();
		if (r < 0.5 || d.alpha() <= 0)
			return;

		double cx = Math.round(d.x());
		double cy = Math.round(d.y());
		boolean jar = d.kind() == BubbleKind.JAR || d.kind() == BubbleKind.CLUSTER_JAR;
		Color base = d.kind() == BubbleKind.BATTLE ? JarPalette.BATTLE_BASE
			: d.kind() == BubbleKind.INFO ? JarPalette.INFO_BASE : JarPalette.of(d.jar());
		Color fill = d.disabled() ? WindowTheme.mix(base, GREY, 0.75) : base;
		double opacity = d.alpha() * (d.disabled() ? DISABLED_ALPHA : 1.0);
		alpha(g, opacity);
		Area shadow = new Area(circle(cx, cy + 2, r));
		shadow.subtract(new Area(circle(cx, cy, r)));
		g.setColor(new Color(0, 0, 0, 77));
		g.fill(shadow);
		if (d.pulse() > 0)
		{
			double p = clamp(d.pulse());
			g.setColor(withAlpha(KNOCKED_OUT, 0.5 * (1 - p)));
			g.setStroke(new BasicStroke(2f));
			g.draw(circle(cx, cy, r + 2 + 3 * p));
		}

		Area band = new Area(circle(cx, cy, r));
		band.subtract(new Area(circle(cx, cy, r * INNER_RATIO)));
		g.setColor(fill);
		g.fill(band);
		g.setColor(withAlpha(WindowTheme.mix(fill, Color.WHITE, 0.35), 0.6));
		g.setStroke(new BasicStroke(1f));
		g.draw(circle(cx, cy, Math.max(0.5, r - 1.25)));
		g.setColor(withAlpha(OUTLINE, 0.85));
		g.setStroke(new BasicStroke(2f));
		g.draw(circle(cx, cy, r));
		g.setColor(withAlpha(d.disabled() ? WindowTheme.mix(INNER, GREY, 0.3) : INNER, INNER_OPACITY));
		g.fill(circle(cx, cy, r * INNER_RATIO));
		if (jar)
			jarFace(g, d, fill, opacity, style);
		else if (d.kind() == BubbleKind.BATTLE)
			battleFace(g, cx, cy, r, d.disabled());
		else
			infoFace(g, cx, cy, r, d.disabled());

		g.setStroke(new BasicStroke(2f));
		double ring = r + 2.5;
		if (d.selected())
		{
			g.setColor(WindowTheme.mix(fill, Color.WHITE, 0.5));
			g.draw(circle(cx, cy, ring));
			ring += 2.5;
		}

		if (d.hover() > 0)
		{
			g.setColor(withAlpha(Color.WHITE, 0.7 * clamp(d.hover())));
			g.draw(circle(cx, cy, ring));
		}

		if (jar && d.count() > 0)
			badge(g, d);
	}

	private void jarFace(Graphics2D g, BubbleFrame.Disc d, Color fill, double opacity, WindowStyle style)
	{
		double r = d.painted();
		double cx = Math.round(d.x());
		double cy = Math.round(d.y());
		BufferedImage image = JarIcons.source(d.jar());
		if (image != null)
		{
			double k = r / NATURAL_RADIUS;
			if (d.disabled())
				alpha(g, opacity * 0.5);

			double side = JAR_PX * k;
			CrispImages.draw(g, image, cx - side / 2.0, cy - side / 2.0, side);
			alpha(g, opacity);
			return;
		}

		AffineTransform saved = g.getTransform();
		double size = r * 1.0;
		g.translate(cx - size / 2.0, cy - size / 2.0);
		g.scale(size / GLYPH_UNITS, size / GLYPH_UNITS);
		g.setColor(d.disabled() ? GREY : fill);
		Icons.jar(g, 0, 0, GLYPH_UNITS);
		g.setTransform(saved);
	}

	private static double glyphUnit(double size)
	{
		double unit = size / GRID;
		return unit >= 0.75 ? Math.max(1, Math.round(unit)) : unit;
	}

	private static void square(Graphics2D g, int x, int y, int grow)
	{
		g.fillRect(x - grow, y - grow, 2 + 2 * grow, 2 + 2 * grow);
	}

	private static void blades(Graphics2D g, int grow)
	{
		for (int i = 1; i <= 12; i++)
		{
			square(g, i, i, grow);
			square(g, 14 - i, i, grow);
		}

		for (int i = 9; i <= 11; i++)
		{
			square(g, i, 20 - i, grow);
			square(g, 14 - i, 20 - i, grow);
		}
	}

	private void spriteFace(Graphics2D g, BufferedImage sprite, double cx, double cy, double r, boolean disabled)
	{
		double ratio = SPRITE_BOX * r / Math.max(sprite.getWidth(), sprite.getHeight());
		double scale = (r >= FULL_RADIUS ? Math.max(1, Math.floor(ratio)) : ratio) * SPRITE_SHRINK;
		int w = (int) Math.round(sprite.getWidth() * scale);
		int h = (int) Math.round(sprite.getHeight() * scale);
		Object hint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		Composite saved = g.getComposite();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, scale == Math.rint(scale)
			? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		if (disabled && saved instanceof AlphaComposite)
			g.setComposite(AlphaComposite.SrcOver.derive(((AlphaComposite) saved).getAlpha() * DISABLED_SPRITE));

		g.drawImage(sprite, (int) Math.round(cx - w / 2.0), (int) Math.round(cy - h / 2.0), w, h, null);
		g.setComposite(saved);
		if (hint != null)
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
	}

	private void battleFace(Graphics2D g, double cx, double cy, double r, boolean disabled)
	{
		BufferedImage sprite = combatSprite.get();
		if (sprite != null)
		{
			spriteFace(g, sprite, cx, cy, r, disabled);
			return;
		}

		double unit = glyphUnit(1.2 * r);
		AffineTransform saved = g.getTransform();
		g.translate(Math.round(cx - GRID * unit / 2.0), Math.round(cy - GRID * unit / 2.0));
		g.scale(unit, unit);
		g.setColor(withAlpha(OUTLINE, 0.85));
		blades(g, 1);
		g.setColor(disabled ? DISABLED_GLYPH : SWORD_GREY);
		blades(g, 0);
		g.setTransform(saved);
	}

	private void infoFace(Graphics2D g, double cx, double cy, double r, boolean disabled)
	{
		double unit = glyphUnit(1.1 * r);
		AffineTransform saved = g.getTransform();
		g.translate(Math.round(cx - GRID * unit / 2.0), Math.round(cy - GRID * unit / 2.0));
		g.scale(unit, unit);
		g.setColor(withAlpha(INFO_HALO, 0.9));
		infoShape(g, 1);
		g.setColor(disabled ? new Color(0x707070) : INFO_GLYPH);
		infoShape(g, 0);
		g.setTransform(saved);
	}

	private static void infoShape(Graphics2D g, int grow)
	{
		g.fillRect(6 - grow, 1 - grow, 4 + 2 * grow, 4 + 2 * grow);
		g.fillRect(4 - grow, 6 - grow, 6 + 2 * grow, 2 + 2 * grow);
		g.fillRect(6 - grow, 6 - grow, 4 + 2 * grow, 8 + 2 * grow);
		g.fillRect(4 - grow, 12 - grow, 8 + 2 * grow, 2 + 2 * grow);
	}

	private void badge(Graphics2D g, BubbleFrame.Disc d)
	{
		String text = d.count() > 99 ? "99+" : String.valueOf(d.count());
		int unit = Math.max(1, (int) Math.round(d.painted() / BADGE_UNIT_RADIUS));
		int height = (BADGE_ROWS + 2) * unit;
		int digitsWidth = (text.length() * (BADGE_COLS + 1) - 1) * unit;
		int width = Math.max(height, digitsWidth + 2 * unit + (digitsWidth % 2 == 0 ? 0 : 1));
		long right = Math.round(Math.round(d.x()) + BADGE_ANCHOR * d.painted());
		long bottom = Math.round(Math.round(d.y()) + BADGE_ANCHOR * d.painted());
		int left = (int) (right - width);
		int top = (int) (bottom - height);
		g.setColor(BADGE);
		g.fill(new RoundRectangle2D.Double(left, top, width, height, height, height));
		g.setColor(Color.WHITE);
		int x = left + (width - digitsWidth) / 2;
		for (char c : text.toCharArray())
		{
			String[] rows = BADGE_GLYPHS[c == '+' ? 10 : c - '0'];
			for (int row = 0; row < BADGE_ROWS; row++)
			{
				for (int col = 0; col < BADGE_COLS; col++)
				{
					if (rows[row].charAt(col) == '1')
						g.fillRect(x + col * unit, top + (row + 1) * unit, unit, unit);
				}
			}

			x += (BADGE_COLS + 1) * unit;
		}
	}

	private int lineHeight(Graphics2D g, WindowStyle style)
	{
		return g.getFontMetrics(style.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS)).getHeight()
			+ style.px(2);
	}

	private void card(Graphics2D g, BubbleFrame.Card card, WindowStyle style)
	{
		WildCardContent content = card.content();
		if (content == null || card.alpha() <= 0)
			return;

		Rectangle box = card.bounds();
		double s = card.scale();
		double ax = card.side() == BubbleFrame.Side.LEFT ? box.x + box.width
			: card.side() == BubbleFrame.Side.ABOVE ? box.x + box.width / 2.0 : box.x;
		double ay = card.side() == BubbleFrame.Side.ABOVE ? box.y + box.height : box.y + box.height / 2.0;
		g.translate(ax, ay);
		g.scale(s, s);
		g.translate(-ax, -ay);
		alpha(g, card.alpha());
		Rectangle footprint = new Rectangle(box.x, box.y, box.width,
			box.height + Math.max(0, style.metrics().shadow));
		Shape onScreen = g.getTransform().createTransformedShape(footprint);
		Rectangle screen = onScreen.getBounds();
		glass.paint(g, box, style, backdrop.behind(screen));
		if (card.pinned())
			style.focusRing(g, box);

		PageText text = new PageText(style, PageText.Sink.NONE);
		int pad = style.px(8);
		int gap = style.px(4);
		int left = box.x + pad;
		int inner = box.width - 2 * pad;
		Font small = style.font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);
		FontMetrics sm = g.getFontMetrics(small);
		int line = lineHeight(g, style);
		int y = box.y + pad;
		int nameH = style.px(24);
		int levelW = sm.stringWidth(content.level());
		Rectangle level = new Rectangle(left + inner - levelW, y, levelW, nameH);
		text.draw(g, content.level(), level, small, ThemeColor.SUBTEXT, PageText.Align.RIGHT);
		Rectangle name = new Rectangle(left, y, Math.max(0, inner - levelW - gap), nameH);
		Color nameColor = new Color(content.nameRgb());
		text.draw(g, content.name(), name, text.nameFont(g, content.name(), name.width, WindowFonts.BODY_UNITS),
			nameColor, PageText.Align.LEFT);
		y += nameH + gap;
		int cx = left;
		for (SummaryContent.Chip chip : content.chips())
		{
			int w = style.chipWidth(g, chip.label());
			if (cx + w <= left + inner)
			{
				if (!glass.chip(g, cx, y, chip.label(), chip.style(), style))
					style.chip(g, cx, y, chip.label(), chip.style());

				cx += w + gap;
			}
		}

		y += style.metrics().chipHeight + gap;
		if (content.recordLabel() != null)
		{
			int icon = Math.max(8, line - style.px(6));
			g.setColor(new Color(content.recordRgb()));
			Icons.mark(g, recordKind(content.recordLabel()), left, y + (line - icon) / 2.0, icon);
			Rectangle label = new Rectangle(left + icon + gap, y, Math.max(0, inner - icon - gap), line);
			text.draw(g, content.recordLabel(), label, small, new Color(content.recordRgb()), PageText.Align.LEFT);
			y += line;
		}

		y = stats(g, content, style, text, small, new Rectangle(left, y, inner, line));
		if (content.words() != null && !content.words().isEmpty())
		{
			text.draw(g, content.words(), new Rectangle(left, y, inner, line), small, ThemeColor.SUBTEXT,
				PageText.Align.LEFT);
			y += line;
		}

		text.draw(g, content.odds(), new Rectangle(left, y, inner, line), small, ThemeColor.TEXT,
			PageText.Align.LEFT);
		y += line;
		if (content.knockedOutLine() != null)
			text.draw(g, content.knockedOutLine(), new Rectangle(left, y, inner, line), small, KNOCKED_OUT,
				PageText.Align.LEFT);
	}

	private static WildRecord.Kind recordKind(String label)
	{
		for (WildRecord.Kind kind : WildRecord.Kind.values())
			if (kind.name().equalsIgnoreCase(label))
				return kind;

		return WildRecord.Kind.NEW;
	}

	private int stats(Graphics2D g, WildCardContent content, WindowStyle style, PageText text, Font small,
		Rectangle first)
	{
		FontMetrics sm = g.getFontMetrics(small);
		int labelW = sm.stringWidth("Defence");
		int valueW = sm.stringWidth("+99%");
		int gap = style.px(4);
		int y = first.y;
		List<WildCardContent.StatRow> rows = content.stats();
		for (WildCardContent.StatRow row : rows)
		{
			Rectangle label = new Rectangle(first.x, y, labelW, first.height);
			text.draw(g, row.label(), label, small, ThemeColor.TEXT, PageText.Align.LEFT);
			Rectangle value = new Rectangle(first.x + first.width - valueW, y, valueW, first.height);
			text.draw(g, row.percentText(), value, small, ThemeColor.SUBTEXT, PageText.Align.RIGHT);
			int barX = label.x + labelW + gap;
			int barW = Math.max(0, value.x - gap - barX);
			int barH = Math.max(3, style.metrics().bar * 3 / 4);
			if (barW > 0)
			{
				Rectangle2D bar = new Rectangle2D.Double(barX, y + (first.height - barH) / 2.0, barW, barH);
				if (!glass.bar(g, bar, row.fraction(), style))
					style.xpBar(g, bar, row.fraction());
			}

			y += first.height;
		}

		return y;
	}
}
