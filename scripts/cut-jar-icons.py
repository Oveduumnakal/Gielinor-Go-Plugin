#!/usr/bin/env python3
"""Cuts scripts/jar-icons-source.png (the 17 jar pictures in a grid, transparent background) into jars/<jar>.png.

Grid, row by row: Bronze, Iron, Rune, Dragon / Cave, Swift, Blessed, Slayer / Venom (Poison), Ember, Giant, Tide /
Stab, Slash, Crush, Archer (Ranged) / Mystic (Magic). Every jar is scaled by the SAME factor, so the jars keep their
relative sizes, and stands on the same line in a transparent 128 x 128 square with an eight pixel margin all round (the
game scales it to the exact pixel size when it draws it, so it stays sharp at any window size). Offline and repeatable:
    python3 scripts/cut-jar-icons.py
Needs Pillow, numpy and scipy.
"""
import os

import numpy as np
from PIL import Image
from scipy import ndimage

HERE = os.path.dirname(os.path.abspath(__file__))
SOURCE = os.path.join(HERE, "jar-icons-source.png")
OUT = os.path.join(HERE, "..", "src", "main", "resources", "com", "oveduumnakal", "gielinorgo", "jars")
SIZE = 128
MARGIN = 8
ALPHA_FLOOR = 16
ORDER = ["bronze", "iron", "rune", "dragon", "cave", "swift", "blessed", "slayer", "venom", "ember", "giant", "tide",
         "stab", "slash", "crush", "archer", "mystic"]


def find_jars(image):
    """Finds the 17 jars: each is one blob of painted pixels, taken row by row and left to right."""
    painted = np.array(image.getchannel("A")) > ALPHA_FLOOR
    labels, count = ndimage.label(ndimage.binary_dilation(painted, iterations=6))
    boxes = []
    for index in range(1, count + 1):
        ys, xs = np.where((labels == index) & painted)
        if len(ys) > 2000:
            boxes.append((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))

    boxes.sort(key=lambda box: (round(box[1] / 150), box[0]))
    if len(boxes) != len(ORDER):
        raise SystemExit("expected %d jars, found %d" % (len(ORDER), len(boxes)))

    return boxes


def main():
    image = Image.open(SOURCE).convert("RGBA")
    boxes = find_jars(image)
    tallest = max(box[3] - box[1] for box in boxes)
    scale = (SIZE - 2 * MARGIN) / tallest
    os.makedirs(OUT, exist_ok=True)
    for name, box in zip(ORDER, boxes):
        jar = image.crop(box)
        jar = jar.resize((max(1, round(jar.width * scale)), max(1, round(jar.height * scale))), Image.LANCZOS)
        icon = Image.new("RGBA", (SIZE, SIZE), (255, 255, 255, 0))
        icon.paste(jar, ((SIZE - jar.width) // 2, SIZE - MARGIN - jar.height), jar)
        icon.save(os.path.join(OUT, name + ".png"), optimize=True)
        print(name, jar.size)


if __name__ == "__main__":
    main()
