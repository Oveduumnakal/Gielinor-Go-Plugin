#!/usr/bin/env python3
"""Writes families.json, the creature families behind the Creature Log milestones of Gielinor Go.

Run from the repository root, after gen-species.py:

    python3 scripts/gen-families.py

A species flagged "quest" (met only during a quest, issue #50) is never a member: a family must be completable.

Each family is picked from species.json by a name pattern (with a pattern for names to leave out) and carries an id,
a label, the specialty jar paid for completing it, and the species keys. It reads only the local species.json, so it
needs no network, and its output is the same on every run. It aborts without writing when a family has fewer than
3 species, because a family that small would be no goal.
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, "..", "src", "main", "resources", "com", "oveduumnakal", "gielinorgo")
MIN_MEMBERS = 3

FAMILIES = [
    ("goblins", "Goblins", "WARRIOR", r"\bgoblin\b", None),
    ("dragons", "Dragons", "EMBER", r"\bdragon\b", None),
    ("giants", "Giants", "GIANT", r"\b(hill|moss|fire|ice) giant\b|\bgiant champion\b", None),
    ("demons", "Demons", "SLAYER", r"\bdemon\b", r"^demon of "),
    ("skeletons", "Skeletons", "BLESSED", r"\bskeleton\b", None),
    ("spiders", "Spiders", "VENOM", r"\bspider\b", None),
    ("wizards", "Wizards", "MYSTIC", r"\bwizard\b", None),
    ("dagannoths", "Dagannoths", "TIDE", r"\bdagannoth\b", None),
    ("trolls", "Trolls", "CAVE", r"\btroll\b", None),
    ("wyverns", "Wyverns", "SWIFT", r"\bwyvern\b", None),
]


def key_for(name):
    """The lookup key of a species name, as Species.keyFor does in the plugin."""
    return name.replace(" ", " ").strip().lower()


def main():
    with open(os.path.join(DATA, "species.json"), encoding="utf-8") as handle:
        species = json.load(handle)

    out = []
    for fid, label, jar, include, exclude in FAMILIES:
        members = []
        for row in species:
            name = row["name"]
            if row.get("quest"):
                continue
            if not re.search(include, name, re.I):
                continue
            if exclude and re.search(exclude, name, re.I):
                continue
            members.append(key_for(name))
        members = sorted(set(members))
        if len(members) < MIN_MEMBERS:
            sys.exit("family %s has only %d species, aborting" % (fid, len(members)))
        out.append({"id": fid, "label": label, "jar": jar, "species": members})

    path = os.path.join(DATA, "families.json")
    with open(path, "w", encoding="utf-8") as handle:
        handle.write("[\n")
        handle.write(",\n".join(json.dumps(f, ensure_ascii=False, separators=(",", ":")) for f in out))
        handle.write("\n]\n")
    print("wrote %d families to %s" % (len(out), os.path.normpath(path)))


if __name__ == "__main__":
    main()
