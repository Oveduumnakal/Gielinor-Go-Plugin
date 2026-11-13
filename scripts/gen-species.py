#!/usr/bin/env python3
"""Generate species.json (attackable OSRS monsters) from the OSRS Wiki Bucket API.

Run from the repo root: python3 scripts/gen-species.py [dump.tar.gz]

Combat styles (issue #85): a species gets STAB, SLASH, CRUSH, RANGED or MAGIC from the first wiki attack style. The
ones the wiki does not decide (no style, Melee, Typeless, All, Dragonfire first) take STAB, SLASH or CRUSH from
scripts/species-melee-style.txt; a line there also wins over the wiki. A species with neither fails the run.

Quest monsters (issue #50): a species whose every wiki row sits on a page of Category:Quest_monsters (read through the
MediaWiki API, list=categorymembers) gets "quest": true. scripts/species-quest-add.txt lists names to flag although the
wiki does not, scripts/species-quest-keep.txt names to keep catchable although it does. The flagged names are written
to src/test/resources/quest-species.txt so they can be reviewed. A flagged species stays in species.json, so a creature
already owned keeps its species, but it never turns wild and does not count toward the Creature Log.

The wiki lists the NPC ids of only some of the NPCs that carry a monster's name (the Goblin page lists 43 of about 150;
the small Monkey Guard is missing). A wild monster is matched to its species by NAME, so any NPC with the name and a
combat level can turn wild. When the optional dump (the abextm/osrs-cache release gen-animations.py reads) is given,
the ids of every NPC in it that has the species' name (any letter case) and a combat level above 0 are added after the
wiki ids. Without it only the wiki ids are written.
"""
import json
import re
import sys
import tarfile
import time
import urllib.parse
import urllib.request

API = "https://oldschool.runescape.wiki/api.php"
OUT = "src/main/resources/com/oveduumnakal/gielinorgo/species.json"
BLOCKLIST = "scripts/species-blocklist.txt"
MELEE_STYLES = "scripts/species-melee-style.txt"
MELEE_STYLE_NAMES = ("STAB", "SLASH", "CRUSH")
QUEST_ADD = "scripts/species-quest-add.txt"
QUEST_KEEP = "scripts/species-quest-keep.txt"
QUEST_LIST = "src/test/resources/quest-species.txt"
QUEST_CATEGORY = "Category:Quest_monsters"
UA = "species-data-script/1.0"
PAGE = 500
QUERY = ("bucket('infobox_monster').select('page_name','name','combat_level','attack_style','attribute',"
         "'attack_speed','size','poisonous','slayer_level','id').limit(%d).offset(%d).run()")

# NPC ids kept per species: those of every wiki row (variant or version) with its name, the row the species' level and
# style come from first, so ids()[0] stays the id the portrait and old saves know. Every id needs animations (see
# gen-animations.py), so there is no small cap any more; this only stops a runaway name (Guard has 232).
MAX_IDS = 300

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
TRAIT_ORDER = ("FAST", "UNHOLY", "FIERY", "LARGE", "POISONOUS", "SLAYER", "AQUATIC")


def fetch(offset):
    params = urllib.parse.urlencode({"action": "bucket", "format": "json", "query": QUERY % (PAGE, offset)})
    req = urllib.request.Request(API + "?" + params, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.load(resp).get("bucket", [])


def quest_pages():
    """Returns the lowercase titles of the pages in the wiki's quest monster category."""
    titles = set()
    params = {"action": "query", "list": "categorymembers", "cmtitle": QUEST_CATEGORY, "cmlimit": 500,
              "cmnamespace": 0, "format": "json"}
    while True:
        req = urllib.request.Request(API + "?" + urllib.parse.urlencode(params), headers={"User-Agent": UA})
        with urllib.request.urlopen(req, timeout=60) as resp:
            data = json.load(resp)
        for member in data["query"]["categorymembers"]:
            titles.add(page_key(member["title"]))
        if "continue" not in data:
            return titles
        params.update(data["continue"])
        time.sleep(1)


def page_key(title):
    return re.sub(r"\s+", " ", (title or "").replace("_", " ")).strip().lower()


def style_of(styles):
    """Returns STAB, SLASH, CRUSH, RANGED or MAGIC when the first wiki style names one, else None (undecided).

    As before, only the first style listed counts, so a monster listed as Melee, Magic stays a melee monster and gets
    its style from scripts/species-melee-style.txt.
    """
    styles = as_list(styles)
    if not styles:
        return None
    first = str(styles[0]).lower()
    if "magic" in first or "spell" in first:
        return "MAGIC"
    if "range" in first or "ranging" in first:
        return "RANGED"
    for word in ("stab", "slash", "crush"):
        if word in first:
            return word.upper()
    return None


def load_melee_styles(path=MELEE_STYLES):
    """Returns {lowercase name: STYLE} from scripts/species-melee-style.txt (name = STYLE, # starts a comment)."""
    found = {}
    with open(path, encoding="utf-8") as f:
        for number, line in enumerate(f, 1):
            line = line.split("#")[0].strip()
            if not line:
                continue
            name, _, style = line.rpartition("=")
            name, style = name.strip().lower(), style.strip().upper()
            if style not in MELEE_STYLE_NAMES:
                raise SystemExit("%s:%d: %r is not STAB, SLASH or CRUSH" % (path, number, style))
            found[name] = style
    return found


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
    return found


def merge_ids(first, more):
    """The ids of the best row, then the ids of the other rows with the same name that it does not have."""
    merged = list(first)
    for npc_id in more:
        if npc_id not in merged:
            merged.append(npc_id)
    return merged


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
    name, level, style, traits, ids = entry[:5]
    data = {"name": name, "level": level, "style": style}
    if traits:
        data["traits"] = traits
    if ids:
        data["ids"] = ids
    if len(entry) > 5 and entry[5]:
        data["quest"] = True
    return data


NPC_DEF = re.compile(r"(?:^|/)npc_defs/(\d+)\.json$")


def dump_ids(path):
    """Maps a lowercase NPC name to the sorted ids of the NPCs in the dump that have it and a combat level."""
    found = {}
    with tarfile.open(path, "r:*") as tar:
        for member in tar:
            match = NPC_DEF.search(member.name)
            if not match:
                continue
            definition = json.load(tar.extractfile(member))
            name = str(definition.get("name") or "").strip().lower()
            if name and (definition.get("combatLevel") or 0) > 0:
                found.setdefault(name, []).append(int(match.group(1)))
    return {name: sorted(ids) for name, ids in found.items()}


def load_blocklist(path=BLOCKLIST):
    """Returns the lowercase names listed in the file, skipping blank lines and # comments."""
    with open(path, encoding="utf-8") as f:
        lines = [line.split("#")[0].strip().lower() for line in f]
    return {line for line in lines if line}


def resolve_styles(entries):
    """Gives every species its style from the wiki, or from the melee style file where the wiki has none.

    A line in the file also wins over the wiki (the generator prints each such override). A species with no wiki style
    and no line fails the run.
    """
    chosen = load_melee_styles()
    missing = [e[0] for e in entries if e[2] is None and e[0].lower() not in chosen]
    if missing:
        print("species-melee-style.txt has no line for %d species:" % len(missing), file=sys.stderr)
        print("\n".join(missing), file=sys.stderr)
        raise SystemExit("nothing written")
    known = {e[0].lower() for e in entries}
    unknown = sorted(set(chosen) - known)
    if unknown:
        print("species-melee-style.txt names no known species (ignored):", unknown)
    for e in entries:
        if e[2] is not None and e[0].lower() in chosen and chosen[e[0].lower()] != e[2]:
            print("override: %s is %s on the wiki, %s here" % (e[0], e[2], chosen[e[0].lower()]))
    return [(e[0], e[1], chosen.get(e[0].lower(), e[2])) + e[3:] for e in entries]


def is_quest(key, row_pages, quest_titles, quest_add, quest_keep):
    """True when the wiki puts every row of the species in the quest category, or the add file names it."""
    if key in quest_keep:
        return False
    if key in quest_add:
        return True
    pages = row_pages.get(key, set())
    return bool(pages) and pages <= quest_titles


def main():
    blocked_names = load_blocklist()
    blocked = set()
    quest_titles = quest_pages()
    quest_add = load_blocklist(QUEST_ADD)
    quest_keep = load_blocklist(QUEST_KEEP)
    row_pages = {}
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
        row_pages.setdefault(key, set()).add(page_key(row.get("page_name")))
        row_ids = ids_of(row)
        if key not in best or level > best[key][1]:
            kept = best[key][4] if key in best else []
            best[key] = (name, level, style_of(row.get("attack_style")), traits_of(name, row),
                           merge_ids(row_ids, kept))
        else:
            best[key] = best[key][:4] + (merge_ids(best[key][4], row_ids),)
    entries = sorted(best.values(), key=lambda e: e[0].lower())
    entries = resolve_styles(entries)
    entries = [e + (is_quest(e[0].lower(), row_pages, quest_titles, quest_add, quest_keep),) for e in entries]
    if len(sys.argv) > 1:
        extra = dump_ids(sys.argv[1])
        entries = [e[:4] + (merge_ids(e[4], extra.get(e[0].lower(), [])),) + e[5:] for e in entries]
    entries = [e[:4] + (e[4][:MAX_IDS],) + e[5:] for e in entries]
    with open(OUT, "w", encoding="utf-8") as f:
        f.write("[\n")
        f.write(",\n".join(json.dumps(record(e), separators=(",", ":"), ensure_ascii=False) for e in entries))
        f.write("\n]\n")
    quest = [e[0] for e in entries if e[5]]
    with open(QUEST_LIST, "w", encoding="utf-8") as f:
        f.write("# Species flagged quest-only by gen-species.py (see scripts/species-quest-*.txt). Generated.\n")
        f.write("".join(name + "\n" for name in quest))
    print("quest species:", len(quest))
    print("species:", len(entries))
    for s in ("STAB", "SLASH", "CRUSH", "RANGED", "MAGIC"):
        print(s, sum(1 for e in entries if e[2] == s), "(catchable:", sum(1 for e in entries if e[2] == s and not e[5]) , ")")
    print("from the melee style file:", sum(1 for e in entries if e[0].lower() in load_melee_styles()))
    for t in TRAIT_ORDER:
        print(t, sum(1 for e in entries if t in e[3]))
    print("with ids", sum(1 for e in entries if e[4]))
    print("blocked:", len(blocked), sorted(blocked))
    print("review:", [e[0] for e in entries if any(c.isdigit() for c in e[0]) or not e[4]])


if __name__ == "__main__":
    main()
