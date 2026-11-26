#!/usr/bin/env python3
"""Pack the 12 animated biome backgrounds into the plugin's resources, small enough to ship in the jar.

The loops are made outside this repo: one folder per biome holding numbered JPEG frames and a manifest.json
(frames, fps, width, height, loop). Each is shrunk to 480 x 264 and stored as one reference picture (the per-pixel
median of the loop) plus, for each frame, its difference from that picture, halved around 128. Still areas then cost
almost nothing. The median, not the first frame, keeps moving things from leaving a ghost; halving, not clipping,
keeps large changes from smearing. The plugin rebuilds a frame as reference + 2 * (difference - 128).

Needs Pillow and numpy. Run from the repo root:
    python3 scripts/pack-backgrounds.py <folder holding the 12 loops>
"""
import io
import json
import os
import sys

import numpy
from PIL import Image

OUT = "src/main/resources/com/oveduumnakal/gielinorgo/biomes"
SIZE = (480, 264)
REFERENCE_QUALITY = 85
FRAME_QUALITY = 75
STILL = 3
BIOMES = [
    "cave-dungeon", "coast-sea", "crypt-haunted", "desert", "forest-woodland", "grassland-farmland",
    "otherworldly", "snow-mountain", "swamp-jungle", "town-castle", "volcano-fire", "wilderness",
]


def jpeg(pixels, path, quality):
    Image.fromarray(pixels.astype(numpy.uint8), "RGB").save(path, "JPEG", quality=quality, optimize=True)


def pack(source, biome):
    folder = os.path.join(source, biome)
    with open(os.path.join(folder, "manifest.json"), encoding="utf-8") as handle:
        manifest = json.load(handle)
    count = int(manifest["frames"])
    frames = numpy.stack([
        numpy.asarray(Image.open(os.path.join(folder, "%03d.jpg" % i)).convert("RGB").resize(SIZE, Image.LANCZOS))
        for i in range(count)
    ]).astype(numpy.int16)

    out = os.path.join(OUT, biome)
    os.makedirs(out, exist_ok=True)
    reference_path = os.path.join(out, "reference.jpg")
    jpeg(numpy.clip(numpy.median(frames, axis=0).round(), 0, 255), reference_path, REFERENCE_QUALITY)
    reference = numpy.asarray(Image.open(reference_path).convert("RGB")).astype(numpy.int16)

    errors = []
    for i in range(count):
        difference = frames[i] - reference
        difference[numpy.abs(difference) <= STILL] = 0
        buffer = io.BytesIO()
        halved = numpy.clip(difference // 2 + 128, 0, 255).astype(numpy.uint8)
        Image.fromarray(halved, "RGB").save(buffer, "JPEG", quality=FRAME_QUALITY, optimize=True)
        with open(os.path.join(out, "%03d.jpg" % i), "wb") as handle:
            handle.write(buffer.getvalue())
        played = numpy.asarray(Image.open(buffer).convert("RGB")).astype(numpy.int16)
        rebuilt = numpy.clip(reference + 2 * (played - 128), 0, 255)
        errors.append(numpy.abs(rebuilt - frames[i]))

    with open(os.path.join(out, "manifest.json"), "w", encoding="utf-8", newline="\n") as handle:
        json.dump({"frames": count, "fps": float(manifest["fps"])}, handle)
        handle.write("\n")

    size = sum(os.path.getsize(os.path.join(out, name)) for name in os.listdir(out))
    error = numpy.stack(errors)
    print("%-20s %3d frames  %5.2f MiB  mean error %.1f  pixels off by more than 48: %.3f%%" % (
        biome, count, size / 1048576.0, error.mean(), 100.0 * (error.max(axis=-1) > 48).mean()))
    return size


def main():
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)
    total = sum(pack(sys.argv[1], biome) for biome in BIOMES)
    print("total %.2f MiB" % (total / 1048576.0))


if __name__ == "__main__":
    main()
