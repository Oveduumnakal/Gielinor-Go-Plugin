#!/usr/bin/env python3
"""Cuts scripts/style-icons-source.png (five combat style icons in a grid) into one square PNG per style.

Grid: top row Ranged, Magic, Stab; bottom row Slash, Crush. Each icon is cropped to what is painted in its cell,
centred in a transparent square with a small margin and scaled to 128 x 128. Offline and repeatable:
    python3 scripts/cut-style-icons.py
Needs Pillow and numpy.
"""
import os

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SOURCE = os.path.join(HERE, "style-icons-source.png")
OUT = os.path.join(HERE, "..", "src", "main", "resources", "com", "oveduumnakal", "gielinorgo", "styles")
SIZE = 128
MARGIN = 0.06
ALPHA_FLOOR = 16

# name: (left, top, right, bottom) of the cell in the 1254 x 1254 source
CELLS = {
    "ranged": (0, 150, 460, 650),
    "magic": (460, 150, 850, 650),
    "stab": (850, 150, 1254, 650),
    "slash": (0, 650, 460, 1150),
    "crush": (460, 650, 900, 1150),
}


def cut(image, box):
    cell = image.crop(box)
    alpha = np.array(cell.getchannel("A"))
    ys, xs = np.where(alpha > ALPHA_FLOOR)
    cell = cell.crop((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))
    side = max(cell.size)
    inner = int(round(SIZE * (1.0 - 2 * MARGIN)))
    scale = inner / side
    cell = cell.resize((max(1, round(cell.width * scale)), max(1, round(cell.height * scale))), Image.LANCZOS)
    out = Image.new("RGBA", (SIZE, SIZE), (255, 255, 255, 0))
    out.paste(cell, ((SIZE - cell.width) // 2, (SIZE - cell.height) // 2), cell)
    return out


def main():
    image = Image.open(SOURCE).convert("RGBA")
    os.makedirs(OUT, exist_ok=True)
    for name, box in CELLS.items():
        icon = cut(image, box)
        icon.save(os.path.join(OUT, name + ".png"), optimize=True)
        print(name, icon.size)


if __name__ == "__main__":
    main()
