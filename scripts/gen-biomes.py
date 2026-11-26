#!/usr/bin/env python3
"""Generate biomes.json, the primary biome of every species, from docs/SPECIES.md.

The Summary page plays that biome's background loop behind the creature. Each value is the name of the loop's folder
(see pack-backgrounds.py).

Run from the repo root:
    python3 scripts/gen-biomes.py
"""
import json

SOURCE = "docs/SPECIES.md"
OUT = "src/main/resources/com/oveduumnakal/gielinorgo/biomes.json"
FOLDERS = {
    "Cave & Dungeon": "cave-dungeon",
    "Coast & Sea": "coast-sea",
    "Crypt & Haunted": "crypt-haunted",
    "Desert": "desert",
    "Forest & Woodland": "forest-woodland",
    "Grassland & Farmland": "grassland-farmland",
    "Otherworldly": "otherworldly",
    "Snow & Mountain": "snow-mountain",
    "Swamp & Jungle": "swamp-jungle",
    "Town & Castle": "town-castle",
    "Volcano & Fire": "volcano-fire",
    "Wilderness": "wilderness",
}


def main():
    biomes = {}
    listing = False
    with open(SOURCE, encoding="utf-8") as handle:
        for line in handle:
            if line.startswith("| Species | Lv |"):
                listing = True
                continue
            if not listing or not line.startswith("| ") or line.startswith("| ---"):
                continue
            cells = [cell.strip() for cell in line.split("|")]
            biomes[cells[1]] = FOLDERS[cells[5]]

    if not biomes:
        raise SystemExit("no species found in " + SOURCE)

    with open(OUT, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(biomes, handle, ensure_ascii=False, indent=1, sort_keys=True)
        handle.write("\n")
    print("wrote %d species to %s" % (len(biomes), OUT))


if __name__ == "__main__":
    main()
