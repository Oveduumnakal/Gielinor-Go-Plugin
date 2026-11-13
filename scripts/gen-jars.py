#!/usr/bin/env python3
"""Draws the 17 capture-jar pictures that Gielinor Go shows in the panel and beside jar finds.

Run from the repository root (needs Pillow):

    python3 scripts/gen-jars.py [contact-sheet.png]

It writes one 32x32 PNG per jar to src/main/resources/com/oveduumnakal/gielinorgo/jars/<name>.png, where <name> is
the lower-case JarType name (bronze.png ... mystic.png). It also writes the four healing-item pictures (small_salve, large_salve, revive, antidote) to
src/main/resources/com/oveduumnakal/gielinorgo/items/. The optional argument is a path for a contact sheet that
shows every jar at 32 px and at 16 px on the panel colour; keep it outside the repository.

Everything is drawn here from polygons and arcs, at 4x size and averaged down (box filter, so nothing leaks into the margin). The output is the same
every run (no dates or random values), so the committed files only change when this script does. The PNGs may be
replaced by hand-made art of the same size (32x32, RGBA, a fully transparent 1 px margin) with no code change.
"""
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFont

SIZE = 32
SCALE = 4
WHITE = (255, 255, 255, 255)
DARK_LID = (88, 90, 102, 255)
CLEAR_GLASS = (205, 228, 238, 190)
PANEL = (40, 40, 40, 255)

OUT_DIR = os.path.join(
    os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "com", "oveduumnakal",
    "gielinorgo", "jars")


def px(*values):
    """Turns 32-unit drawing coordinates into 4x pixel coordinates."""
    return [v * SCALE for v in values]


def polygon(draw, points, fill):
    draw.polygon([(x * SCALE, y * SCALE) for x, y in points], fill=fill)


def line(draw, points, width, fill):
    draw.line([(x * SCALE, y * SCALE) for x, y in points], fill=fill, width=int(width * SCALE), joint="curve")


def disc(draw, cx, cy, r, fill):
    draw.ellipse(px(cx - r, cy - r, cx + r, cy + r), fill=fill)


def shade(color, factor):
    """Makes a colour darker (below 1) or lighter (above 1)."""
    return tuple(max(0, min(255, int(c * factor))) for c in color[:3]) + (255,)


def draw_jar(lid, glass, symbol, band=None):
    """Draws one jar.

    lid: lid colour. glass: glass colour (may be translucent). symbol: a function that draws a white symbol on the
    glass, or None. band: a colour for a wide band across the body (the metal jars), or None.
    """
    big = Image.new("RGBA", (SIZE * SCALE, SIZE * SCALE), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    outline = shade(glass if band is None else (120, 150, 165, 255), 0.55)
    d.rounded_rectangle(px(9.5, 6, 22.5, 11), radius=1 * SCALE, fill=outline)
    d.rounded_rectangle(px(3.5, 8.5, 28.5, 30.5), radius=6 * SCALE, fill=outline)
    d.rounded_rectangle(px(10.5, 6.5, 21.5, 11), radius=1 * SCALE, fill=glass)
    d.rounded_rectangle(px(4.5, 9.5, 27.5, 29.5), radius=5 * SCALE, fill=glass)
    if band is not None:
        d.rectangle(px(4.5, 17, 27.5, 24), fill=band)
        d.rectangle(px(4.5, 17, 27.5, 18), fill=shade(band, 1.25))
        d.rectangle(px(4.5, 23, 27.5, 24), fill=shade(band, 0.7))
    d.rounded_rectangle(px(8, 2, 24, 8), radius=1.5 * SCALE, fill=shade(lid, 0.6))
    d.rounded_rectangle(px(8.5, 2.5, 23.5, 7.5), radius=1.2 * SCALE, fill=lid)
    d.rectangle(px(9, 3, 23, 4), fill=shade(lid, 1.3))
    d.rectangle(px(9, 6.5, 23, 7.5), fill=shade(lid, 0.75))
    shine = Image.new("RGBA", big.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(shine)
    sd.rounded_rectangle(px(6, 12, 8, 27), radius=1 * SCALE, fill=(255, 255, 255, 90))
    big = Image.alpha_composite(big, shine)
    if symbol is not None:
        top = Image.new("RGBA", big.size, (0, 0, 0, 0))
        symbol(ImageDraw.Draw(top))
        big = Image.alpha_composite(big, top)

    return big.resize((SIZE, SIZE), Image.BOX)


def pickaxe(d):
    line(d, [(16, 14), (16, 27)], 2.2, WHITE)
    d.arc(px(7, 11, 25, 29), 198, 342, fill=WHITE, width=int(3 * SCALE))


def wing(d):
    polygon(d, [(8.5, 26), (9.5, 19), (13, 14.5), (18, 12), (24, 11.5), (22, 15), (24.5, 16), (21, 19.5),
                (23, 21), (18.5, 24), (14, 26)], WHITE)


def star(d):
    points = []
    for i in range(10):
        radius = 8.2 if i % 2 == 0 else 3.4
        angle = -math.pi / 2 + i * math.pi / 5
        points.append((16 + radius * math.cos(angle), 20 + radius * math.sin(angle)))

    polygon(d, points, WHITE)


def skull(d):
    disc(d, 16, 18.5, 6.3, WHITE)
    d.rounded_rectangle(px(12.2, 21, 19.8, 27.5), radius=1.2 * SCALE, fill=WHITE)
    eye = (45, 45, 50, 255)
    disc(d, 13.4, 18.5, 1.9, eye)
    disc(d, 18.6, 18.5, 1.9, eye)
    d.rectangle(px(14.6, 24.2, 15.4, 27.5), fill=eye)
    d.rectangle(px(16.6, 24.2, 17.4, 27.5), fill=eye)


def drop(d):
    polygon(d, [(16, 10.5), (21.2, 19), (11.2, 19)], WHITE)
    disc(d, 16, 21.3, 5.6, WHITE)


def flame(d):
    polygon(d, [(16.5, 10), (19.5, 14), (20.5, 12.5), (23, 18), (22.5, 24), (19.5, 27.5), (12.5, 27.5),
                (9.5, 24), (9.5, 19), (12, 15.5), (13.5, 17), (14.5, 13.5)], WHITE)
    inner = (255, 190, 90, 255)
    polygon(d, [(16, 19), (18.5, 22.5), (18.5, 25), (16, 26.8), (13.5, 25), (13.5, 22.5)], inner)


def footprint(d):
    d.ellipse(px(11.6, 19.5, 20.4, 28), fill=WHITE)
    for cx, cy, r in ((10.6, 16.2, 1.9), (14.4, 13, 2.1), (18.6, 13, 2.1), (22.2, 16.2, 1.9)):
        disc(d, cx, cy, r, WHITE)


def wave(d):
    for base in (16.5, 23):
        pts = [(9 + i * 0.7, base + 2.2 * math.sin(i * 0.7 * 0.95)) for i in range(0, 21)]
        line(d, pts, 2.6, WHITE)


def dagger(d):
    polygon(d, [(16, 9), (17.8, 13), (17.8, 21), (14.2, 21), (14.2, 13)], WHITE)
    d.rounded_rectangle(px(11.5, 21, 20.5, 22.8), radius=0.8 * SCALE, fill=WHITE)
    d.rectangle(px(15, 22.8, 17, 27), fill=WHITE)
    disc(d, 16, 27.6, 1.3, WHITE)


def scimitar(d):
    polygon(d, [(21.5, 9.5), (22.5, 15), (20, 20), (16.5, 22.5), (14, 22), (17.5, 19), (19.5, 14.5)], WHITE)
    d.rounded_rectangle(px(10, 20.5, 17, 22.3), radius=0.8 * SCALE, fill=WHITE)
    line(d, [(12.8, 22), (10.5, 27.5)], 2.0, WHITE)


def mace(d):
    line(d, [(16, 17), (16, 28)], 2.2, WHITE)
    disc(d, 16, 14, 4.6, WHITE)
    for dx, dy in ((0, -6.6), (6.2, -2.4), (-6.2, -2.4), (4.6, 4.2), (-4.6, 4.2)):
        disc(d, 16 + dx * 0.9, 14 + dy * 0.9, 1.4, WHITE)


def arrow(d):
    line(d, [(9.5, 27), (21, 15.5)], 1.8, WHITE)
    polygon(d, [(25, 11.5), (19, 13.4), (23.1, 17.5)], WHITE)
    line(d, [(9.5, 27), (9.2, 22.5)], 1.6, WHITE)
    line(d, [(9.5, 27), (14, 26.8)], 1.6, WHITE)
    line(d, [(12.5, 24), (12.5, 20.5)], 1.6, WHITE)
    line(d, [(12.5, 24), (16, 24)], 1.6, WHITE)


def rune(d):
    polygon(d, [(16, 10.5), (23.5, 19.5), (16, 28.5), (8.5, 19.5)], WHITE)
    cut = (100, 50, 150, 255)
    line(d, [(16, 13.5), (16, 25.5)], 1.8, cut)
    line(d, [(16, 17.5), (19.8, 20.5)], 1.8, cut)


JARS = [
    ("bronze", (176, 105, 50, 255), CLEAR_GLASS, None, (190, 112, 52, 255)),
    ("iron", (140, 142, 150, 255), CLEAR_GLASS, None, (150, 152, 160, 255)),
    ("rune", (48, 150, 195, 255), CLEAR_GLASS, None, (48, 150, 195, 255)),
    ("dragon", (190, 38, 38, 255), CLEAR_GLASS, None, (200, 40, 40, 255)),
    ("cave", DARK_LID, (125, 95, 62, 235), pickaxe, None),
    ("swift", DARK_LID, (70, 170, 225, 235), wing, None),
    ("blessed", DARK_LID, (205, 150, 25, 235), star, None),
    ("slayer", DARK_LID, (95, 105, 118, 235), skull, None),
    ("venom", DARK_LID, (62, 165, 55, 235), drop, None),
    ("ember", DARK_LID, (225, 95, 25, 235), flame, None),
    ("giant", DARK_LID, (150, 150, 40, 235), footprint, None),
    ("tide", DARK_LID, (40, 105, 210, 235), wave, None),
    ("stab", DARK_LID, (200, 70, 120, 235), dagger, None),
    ("slash", DARK_LID, (190, 55, 55, 235), scimitar, None),
    ("crush", DARK_LID, (215, 125, 45, 235), mace, None),
    ("archer", DARK_LID, (30, 140, 118, 235), arrow, None),
    ("mystic", DARK_LID, (140, 80, 200, 235), rune, None),
]


def make(entry):
    name, lid, glass, symbol, band = entry
    return draw_jar(lid, glass, symbol, band)


def contact_sheet(images, path):
    """Writes every jar at 32 px, at 16 px, and the 16 px version enlarged 4x so it can be inspected."""
    cell_w, cell_h = 100, 120
    sheet = Image.new("RGBA", (cell_w * 5, cell_h * 4), PANEL)
    font = ImageFont.load_default()
    d = ImageDraw.Draw(sheet)
    for i, (name, image) in enumerate(images):
        x, y = (i % 5) * cell_w, (i // 5) * cell_h
        small = image.resize((16, 16), Image.LANCZOS)
        d.text((x + 6, y + 4), name, fill=WHITE, font=font)
        sheet.alpha_composite(image, (x + 6, y + 20))
        sheet.alpha_composite(small, (x + 50, y + 28))
        sheet.alpha_composite(small.resize((64, 64), Image.NEAREST), (x + 6, y + 54))
    sheet.save(path)


ITEMS_DIR = os.path.join(
    os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "com", "oveduumnakal",
    "gielinorgo", "items")


def draw_pot(body, liquid, cork):
    """Draws a round-bellied pot with a cork; a bigger body makes the Large salve."""
    big = Image.new("RGBA", (SIZE * SCALE, SIZE * SCALE), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    outline = shade(body, 0.45)
    d.rounded_rectangle(px(11.5, 3.5, 20.5, 9), radius=1 * SCALE, fill=outline)
    d.rounded_rectangle(px(5.5, 9.5, 26.5, 29.5), radius=8 * SCALE, fill=outline)
    d.rounded_rectangle(px(12.5, 4.5, 19.5, 9), radius=1 * SCALE, fill=cork)
    d.rounded_rectangle(px(6.5, 10.5, 25.5, 28.5), radius=7 * SCALE, fill=body)
    d.rounded_rectangle(px(8.5, 15, 23.5, 27), radius=5 * SCALE, fill=liquid)
    d.rectangle(px(8.5, 15, 23.5, 16), fill=shade(liquid, 1.3))
    shine = Image.new("RGBA", big.size, (0, 0, 0, 0))
    ImageDraw.Draw(shine).rounded_rectangle(px(8, 17, 10, 25), radius=1 * SCALE, fill=(255, 255, 255, 90))
    return Image.alpha_composite(big, shine)


def draw_vial(liquid, symbol):
    """Draws a tall thin vial with a cork and a coloured liquid; symbol draws on top, or is None."""
    big = Image.new("RGBA", (SIZE * SCALE, SIZE * SCALE), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    outline = shade(liquid, 0.45)
    d.rounded_rectangle(px(13, 2.5, 19, 9), radius=1 * SCALE, fill=outline)
    d.rounded_rectangle(px(8.5, 8.5, 23.5, 30), radius=6 * SCALE, fill=outline)
    d.rounded_rectangle(px(13.5, 3, 18.5, 8.5), radius=1 * SCALE, fill=(150, 110, 70, 255))
    d.rounded_rectangle(px(9.5, 9.5, 22.5, 29), radius=5 * SCALE, fill=CLEAR_GLASS)
    d.rounded_rectangle(px(9.5, 15, 22.5, 29), radius=5 * SCALE, fill=liquid)
    d.rectangle(px(9.5, 15, 22.5, 16), fill=shade(liquid, 1.3))
    shine = Image.new("RGBA", big.size, (0, 0, 0, 0))
    ImageDraw.Draw(shine).rounded_rectangle(px(11, 12, 12.5, 26), radius=0.7 * SCALE, fill=(255, 255, 255, 110))
    big = Image.alpha_composite(big, shine)
    if symbol is not None:
        top = Image.new("RGBA", big.size, (0, 0, 0, 0))
        symbol(ImageDraw.Draw(top))
        big = Image.alpha_composite(big, top)

    return big


def vial_wing(d):
    polygon(d, [(22, 17), (29, 10), (30, 16), (27, 15), (29, 20), (25, 19), (26, 24), (22, 22)], WHITE)


def vial_cross(d):
    d.rectangle(px(14.5, 17.5, 17.5, 26.5), fill=WHITE)
    d.rectangle(px(11.5, 20.5, 20.5, 23.5), fill=WHITE)


ITEMS = [
    ("small_salve", lambda: draw_pot((176, 138, 96, 255), (96, 190, 90, 255), (150, 110, 70, 255))),
    ("large_salve", lambda: draw_pot((120, 96, 140, 255), (70, 140, 220, 255), (150, 110, 70, 255))),
    ("revive", lambda: draw_vial((226, 70, 96, 255), vial_wing)),
    ("antidote", lambda: draw_vial((70, 190, 170, 255), vial_cross)),
]


def write_items():
    """Writes the four healing-item pictures to the items folder."""
    os.makedirs(ITEMS_DIR, exist_ok=True)
    for name, make_item in ITEMS:
        make_item().resize((SIZE, SIZE), Image.BOX).save(os.path.join(ITEMS_DIR, name + ".png"), optimize=False)


def main():
    write_items()
    os.makedirs(OUT_DIR, exist_ok=True)
    images = []
    for entry in JARS:
        image = make(entry)
        image.save(os.path.join(OUT_DIR, entry[0] + ".png"), optimize=False)
        images.append((entry[0], image))

    if len(sys.argv) > 1:
        contact_sheet(images, sys.argv[1])


if __name__ == "__main__":
    main()
