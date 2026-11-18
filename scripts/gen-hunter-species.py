#!/usr/bin/env python3
"""Merges the Hunter creatures (issue #207) into species.json and animations.json. Offline and deterministic.

Run from the repository root:

    python3 scripts/gen-hunter-species.py

The game gives a Hunter creature (pyre fox, kebbits, salamanders, moths, antelope, implings) no combat level, so
gen-species.py, which keeps only rows with a combat level, never lists them. scripts/species-hunter.json is the hand-kept
source: a list of {name, level, style, traits, ids, anim}, where level is the creature's Hunter level requirement and
anim is [standing, walking] read from the game cache (the other four animation columns stay -1).

The run removes every species that carries "hunter": true from species.json, adds the entries of the file (with
"hunter": true, sorted by lower-case name like the rest of the file) and gives each id that has no row in
animations.json the row [standing, walking, -1, -1, -1, -1], in numeric id order. It writes nothing and fails when
a name is already a species that is not a Hunter creature, an id already belongs to another species, or an entry is
malformed. Running it twice changes nothing.
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources", "com", "oveduumnakal", "gielinorgo")
SOURCE = os.path.join(HERE, "species-hunter.json")
STYLES = ("STAB", "SLASH", "CRUSH", "RANGED", "MAGIC")


def fail(message):
    raise SystemExit("hunter species: " + message + ", nothing written")


def line(value):
    return json.dumps(value, separators=(",", ":"), ensure_ascii=False)


def check(entries):
    """Fails on a malformed entry, a repeated name or a repeated id."""
    names = set()
    ids = {}
    for entry in entries:
        name = entry.get("name")
        if not name or name.lower() in names:
            fail("a name is missing or repeated: %r" % name)
        names.add(name.lower())
        if not isinstance(entry.get("level"), int) or entry["level"] < 1:
            fail("%s needs a Hunter level of at least 1" % name)
        if entry.get("style") not in STYLES:
            fail("%s has an unknown style %r" % (name, entry.get("style")))
        if not entry.get("ids"):
            fail("%s has no NPC ids" % name)
        anim = entry.get("anim")
        if not isinstance(anim, list) or len(anim) != 2:
            fail("%s needs anim [standing, walking]" % name)
        for npc_id in entry["ids"]:
            if npc_id in ids:
                fail("id %d is on both %s and %s" % (npc_id, ids[npc_id], name))
            ids[npc_id] = name


def main():
    with open(SOURCE, encoding="utf-8") as handle:
        entries = json.load(handle)
    check(entries)
    with open(os.path.join(RES, "species.json"), encoding="utf-8") as handle:
        species = json.load(handle)
    kept = [s for s in species if not s.get("hunter")]
    taken = {s["name"].lower() for s in kept}
    owners = {npc_id: s["name"] for s in kept for npc_id in s.get("ids", [])}
    for entry in entries:
        if entry["name"].lower() in taken:
            fail("%s is already a species that is not a Hunter creature" % entry["name"])
        for npc_id in entry["ids"]:
            if npc_id in owners:
                fail("id %d of %s already belongs to %s" % (npc_id, entry["name"], owners[npc_id]))
    for entry in entries:
        row = {"name": entry["name"], "level": entry["level"], "style": entry["style"]}
        if entry.get("traits"):
            row["traits"] = entry["traits"]
        row["ids"] = entry["ids"]
        row["hunter"] = True
        kept.append(row)
    kept.sort(key=lambda s: s["name"].lower())

    with open(os.path.join(RES, "animations.json"), encoding="utf-8") as handle:
        animations = {int(k): v for k, v in json.load(handle).items()}
    added = 0
    for entry in entries:
        for npc_id in entry["ids"]:
            if npc_id not in animations:
                animations[npc_id] = [entry["anim"][0], entry["anim"][1], -1, -1, -1, -1]
                added += 1

    with open(os.path.join(RES, "species.json"), "w", encoding="utf-8", newline="\n") as handle:
        handle.write("[\n" + ",\n".join(line(s) for s in kept) + "\n]\n")
    with open(os.path.join(RES, "animations.json"), "w", encoding="utf-8", newline="\n") as handle:
        handle.write("{\n" + ",\n".join(json.dumps(str(k)) + ":" + line(animations[k]) for k in sorted(animations))
                     + "\n}\n")
    print("hunter species:", len(entries), "species in file:", len(kept), "animation rows added:", added)


if __name__ == "__main__":
    main()
