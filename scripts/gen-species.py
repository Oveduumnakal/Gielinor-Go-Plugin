#!/usr/bin/env python3
"""Generate species.json (attackable OSRS monsters) from the OSRS Wiki Bucket API.

Run from the repo root: python3 scripts/gen-species.py
"""
import json
import re
import time
import urllib.parse
import urllib.request

API = "https://oldschool.runescape.wiki/api.php"
OUT = "src/main/resources/com/oveduumnakal/gielinorgo/species.json"
BLOCKLIST = "scripts/species-blocklist.txt"
UA = "species-data-script/1.0"
PAGE = 500
QUERY = ("bucket('infobox_monster').select('page_name','name','combat_level','attack_style','attribute',"
         "'attack_speed','size','poisonous','slayer_level','id').limit(%d).offset(%d).run()")

# NPC ids kept per species, for drawing its portrait. A few are enough: the plugin tries them in order.
MAX_IDS = 4

# Trait rules. Each trait makes one specialty jar work better (see JarType.java). CAVE is not listed here:
# it depends on where the creature is met, not on its species.
FAST_MAX_ATTACK_SPEED = 3
LARGE_MIN_SIZE = 3
UNHOLY_ATTRIBUTES = ("undead", "demon", "vampyre", "shade", "spectral")
FIERY_ATTRIBUTES = ("dragon", "draconic", "fiery")
AQUATIC_WORDS = re.compile(
    r"\b(crab|crabs|kraken|shark|fish|eel|sea|water|waterfiend|swamp|frog|toad|turtle|tortoise|jellyfish|"
    r"dagannoth|lobstrosity|leech|squid|octopus|whirlpool|mogre|newt|penguin|seagull|river|troll fisher|"
    r"wave|tempoross|hydra|crocodile|ammonite|pirate|mermaid|merfolk|ray|eels|snail|sea snake|sea troll)\b",
    re.I)
# A melee species whose wiki attack styles list a breath attack also fights with a second style now and then: magic,
# on this share of its hits. The wiki lists "Dragonfire" for the dragons and "Icy breath" for the wyverns. The primary
# style stays as it is, so this adds to the battle sim and does not replace the style.
BREATH_STYLES = ("dragonfire", "icy breath")
SECOND_STYLE = "MAGIC"
SECOND_CHANCE = 0.25
TRAIT_ORDER = ("FAST", "UNHOLY", "FIERY", "LARGE", "POISONOUS", "SLAYER", "AQUATIC")


def fetch(offset):
    params = urllib.parse.urlencode({"action": "bucket", "format": "json", "query": QUERY % (PAGE, offset)})
    req = urllib.request.Request(API + "?" + params, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.load(resp).get("bucket", [])


def style_of(styles):
    if isinstance(styles, str):
        styles = [styles]
    if not styles:
        return "MELEE"
    first = str(styles[0]).lower()
    if "magic" in first or "spell" in first:
        return "MAGIC"
    if "range" in first or "ranging" in first:
        return "RANGED"
    return "MELEE"


def second_style_of(style, row):
    """Returns (second style, chance of using it per hit), or None when the species has no second style."""
    listed = " ".join(str(a).lower() for a in as_list(row.get("attack_style")))
    if style == "MELEE" and any(word in listed for word in BREATH_STYLES):
        return SECOND_STYLE, SECOND_CHANCE
    return None


def as_list(value):
    if value is None:
        return []
    return value if isinstance(value, list) else [value]


def as_int(value):
    try:
        return int(as_list(value)[0])
    except (IndexError, TypeError, ValueError):
        return 0


def ids_of(row):
    found = []
    for value in as_list(row.get("id")):
        try:
            npc_id = int(value)
        except (TypeError, ValueError):
            continue
        if npc_id > 0 and npc_id not in found:
            found.append(npc_id)
    return found[:MAX_IDS]


def traits_of(name, row):
    attributes = " ".join(str(a).lower() for a in as_list(row.get("attribute")))
    poison = " ".join(str(p).lower() for p in as_list(row.get("poisonous")))
    speed = as_int(row.get("attack_speed"))
    found = set()
    if 0 < speed <= FAST_MAX_ATTACK_SPEED:
        found.add("FAST")
    if any(word in attributes for word in UNHOLY_ATTRIBUTES):
        found.add("UNHOLY")
    if any(word in attributes for word in FIERY_ATTRIBUTES):
        found.add("FIERY")
    if as_int(row.get("size")) >= LARGE_MIN_SIZE:
        found.add("LARGE")
    if poison and not poison.startswith("no") and "immune" not in poison:
        found.add("POISONOUS")
    if as_int(row.get("slayer_level")) > 1:
        found.add("SLAYER")
    if AQUATIC_WORDS.search(name):
        found.add("AQUATIC")
    return [t for t in TRAIT_ORDER if t in found]


def clean_name(name):
    name = re.sub(r"<[^>]*>", "", name or "")
    name = re.sub(r"\[\[(?:[^|\]]*\|)?([^\]]*)\]\]", r"\1", name)
    name = name.split("#")[0]
    name = re.sub(r"\s+", " ", name).strip()
    return name


def record(entry):
    name, level, style, traits, ids, second = entry
    data = {"name": name, "level": level, "style": style}
    if second:
        data["secondStyle"], data["secondChance"] = second
    if traits:
        data["traits"] = traits
    if ids:
        data["ids"] = ids
    return data


def load_blocklist():
    """Returns the lowercase names listed in the blocklist file, skipping blank lines and # comments."""
    with open(BLOCKLIST, encoding="utf-8") as f:
        lines = [line.split("#")[0].strip().lower() for line in f]
    return {line for line in lines if line}


def main():
    blocked_names = load_blocklist()
    blocked = set()
    rows = []
    offset = 0
    while True:
        page = fetch(offset)
        if not page:
            break
        rows.extend(page)
        offset += PAGE
        time.sleep(1)
    # Variants from special pages (minigames, unused, historical) inflate levels, so when a
    # name has rows on its own main page, only those rows are used.
    main_names = {clean_name(r.get("name")).lower() for r in rows
                  if clean_name(r.get("name")).lower() == clean_name(r.get("page_name")).lower()}
    best = {}
    for row in rows:
        name = clean_name(row.get("name") or row.get("page_name"))
        page = clean_name(row.get("page_name")).lower()
        if name.lower() in blocked_names or page in blocked_names:
            blocked.add(name.lower() if name.lower() in blocked_names else page)
            continue
        if name.lower() in main_names and page != name.lower():
            continue
        if re.search(r"\((unused|historical)", row.get("page_name") or "", re.I):
            continue
        try:
            level = int(row.get("combat_level"))
        except (TypeError, ValueError):
            continue
        if not name or level < 1 or any(c in name for c in "[]{}<>|()"):
            continue
        key = name.lower()
        if key not in best or level > best[key][1]:
            best[key] = (name, level, style_of(row.get("attack_style")), traits_of(name, row), ids_of(row),
                           second_style_of(style_of(row.get("attack_style")), row))
    entries = sorted(best.values(), key=lambda e: e[0].lower())
    with open(OUT, "w", encoding="utf-8") as f:
        f.write("[\n")
        f.write(",\n".join(json.dumps(record(e), separators=(",", ":"), ensure_ascii=False) for e in entries))
        f.write("\n]\n")
    print("species:", len(entries))
    for s in ("MELEE", "RANGED", "MAGIC"):
        print(s, sum(1 for e in entries if e[2] == s))
    for t in TRAIT_ORDER:
        print(t, sum(1 for e in entries if t in e[3]))
    print("with ids", sum(1 for e in entries if e[4]))
    print("blocked:", len(blocked), sorted(blocked))
    print("review:", [e[0] for e in entries if any(c.isdigit() for c in e[0]) or not e[4]])


if __name__ == "__main__":
    main()
