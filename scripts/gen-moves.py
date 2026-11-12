#!/usr/bin/env python3
"""Writes moves.json, the moves creatures learn in Gielinor Go (issue 21).

Run from the repository root, after gen-species.py:

    python3 scripts/gen-moves.py

It reads two local files, so it needs no network and its output is the same on every run:

  scripts/move-pools.json  the table the owner reads and tunes: every move, every pool (which move is learned at
                           which level) and the ordered rules that give a species a pool from its name
  species.json             the species and their combat styles

Each species gets the pool of the first rule that matches its name and fits its style (a pool lists the styles it
suits; a species of another style falls back to the default pool for its style). Only species with a non-default pool
are written to the species map; every other species uses the default pool of its style.

Moves.json holds {"moves": [...], "defaults": {style: pool}, "pools": {pool: {"styles": [...], "learns": [[move,
level], ...]}}, "species": {species key: pool}}.

The run aborts, writing nothing, when the table is empty, a rule or a pool names something that does not exist, a
pool does not start with a power 1.0, accuracy 1.0 move at level 1, a pool's levels do not rise, a move is out of
range, a pool is never used, or no species at all matched a rule.
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, "..", "src", "main", "resources", "com", "oveduumnakal", "gielinorgo")
POOLS = os.path.join(HERE, "move-pools.json")
STYLES = ("MELEE", "RANGED", "MAGIC")
SLOTS = ("ATTACK", "SECOND")
MAX_POWER = 3.0
MIN_ACCURACY = 0.5
MAX_MOVES_PER_POOL = 12


def key_for(name):
    """The lookup key of a species name, as Species.keyFor does in the plugin."""
    return name.replace(" ", " ").strip().lower()


def fail(message):
    raise SystemExit("moves: " + message + ", nothing written")


def check_moves(moves):
    """Returns the moves by id after checking every one is in range."""
    if not moves:
        fail("the table has no moves")
    by_id = {}
    for move in moves:
        mid = move.get("id")
        if not mid or mid in by_id:
            fail("a move has no id or a repeated id: %r" % mid)
        if move.get("style") not in STYLES:
            fail("move %s has an unknown style %r" % (mid, move.get("style")))
        if not move.get("name"):
            fail("move %s has no name" % mid)
        if not 0.0 < move.get("power", 0) <= MAX_POWER:
            fail("move %s has a power out of range" % mid)
        if not MIN_ACCURACY <= move.get("accuracy", 0) <= 1.0:
            fail("move %s has an accuracy out of range" % mid)
        if move.get("slot", "ATTACK") not in SLOTS:
            fail("move %s has an unknown slot" % mid)
        effect = move.get("effect")
        if effect is not None and (not effect.get("status") or not 0.0 < effect.get("chance", 0) <= 1.0):
            fail("move %s has a broken effect" % mid)
        by_id[mid] = move
    return by_id


def check_pools(table, by_id):
    """Checks every pool and the defaults, returning the pools."""
    pools = table.get("pools") or {}
    if not pools:
        fail("the table has no pools")
    for name, pool in pools.items():
        styles = pool.get("styles") or []
        learns = pool.get("learns") or []
        if not styles or any(style not in STYLES for style in styles):
            fail("pool %s has no valid styles" % name)
        if not learns or len(learns) > MAX_MOVES_PER_POOL:
            fail("pool %s has no moves or too many" % name)
        first = learns[0]
        if first[1] != 1 or by_id.get(first[0], {}).get("power") != 1.0 \
                or by_id.get(first[0], {}).get("accuracy") != 1.0:
            fail("pool %s does not start with a power 1.0, accuracy 1.0 move at level 1" % name)
        last = 0
        seen = set()
        for move_id, level in learns:
            if move_id not in by_id:
                fail("pool %s names the unknown move %s" % (name, move_id))
            if move_id in seen:
                fail("pool %s repeats the move %s" % (name, move_id))
            if level <= last and not (level == 1 and last == 0):
                fail("pool %s does not rise at %s" % (name, move_id))
            if not 1 <= level <= 99:
                fail("pool %s has a level out of range at %s" % (name, move_id))
            seen.add(move_id)
            last = level
    defaults = table.get("defaults") or {}
    for style in STYLES:
        if defaults.get(style) not in pools or style not in pools[defaults[style]]["styles"]:
            fail("there is no valid default pool for %s" % style)
    return pools


def compile_rules(table, pools):
    """Turns the rules into (pool, matcher) pairs, matched against a species name."""
    rules = []
    for rule in table.get("rules") or []:
        pool = rule.get("pool")
        if pool not in pools:
            fail("a rule names the unknown pool %r" % pool)
        if "names" in rule:
            names = {key_for(n) for n in rule["names"]}
            rules.append((pool, lambda name, names=names: key_for(name) in names))
        elif rule.get("match"):
            pattern = re.compile(rule["match"], re.I)
            rules.append((pool, lambda name, pattern=pattern: bool(pattern.search(name))))
        else:
            fail("a rule for %s has neither names nor match" % pool)
    return rules


def assign(species, rules, pools, defaults):
    """Returns {species key: pool} for every species whose pool is not the default of its style."""
    out = {}
    for row in species:
        name = row["name"]
        style = row.get("style", "MELEE")
        for pool, matches in rules:
            if style in pools[pool]["styles"] and matches(name):
                if pool != defaults[style]:
                    out[key_for(name)] = pool
                break
    return out


def render(table, by_id, pools, assigned):
    """Writes the file as text: one move and one pool per line, so a diff reads well."""
    lines = ['{', '"moves": [']
    ordered = sorted(by_id.values(), key=lambda m: m["id"])
    for i, move in enumerate(ordered):
        lines.append(json.dumps(move, separators=(",", ":"), ensure_ascii=False) + ("," if i < len(ordered) - 1 else ""))
    lines.append('],')
    lines.append('"defaults": ' + json.dumps(table["defaults"], separators=(",", ":")) + ',')
    lines.append('"pools": {')
    names = sorted(pools)
    for i, name in enumerate(names):
        body = json.dumps(pools[name], separators=(",", ":"))
        lines.append(json.dumps(name) + ": " + body + ("," if i < len(names) - 1 else ""))
    lines.append('},')
    lines.append('"species": {')
    keys = sorted(assigned)
    for i, key in enumerate(keys):
        lines.append(json.dumps(key, ensure_ascii=False) + ": " + json.dumps(assigned[key])
                     + ("," if i < len(keys) - 1 else ""))
    lines.append('}')
    lines.append('}')
    return "\n".join(lines) + "\n"


def main():
    with open(POOLS, encoding="utf-8") as handle:
        table = json.load(handle)
    with open(os.path.join(DATA, "species.json"), encoding="utf-8") as handle:
        species = json.load(handle)
    if not species:
        fail("species.json is empty")

    by_id = check_moves(table.get("moves") or [])
    pools = check_pools(table, by_id)
    rules = compile_rules(table, pools)
    assigned = assign(species, rules, pools, table["defaults"])
    if not assigned:
        fail("no species matched a rule")
    unused = sorted(set(pools) - set(assigned.values()) - set(table["defaults"].values()))
    if unused:
        fail("pools no species uses: " + ", ".join(unused))
    unusedMoves = sorted(set(by_id) - {m for pool in pools.values() for m, _ in pool["learns"]})
    if unusedMoves:
        fail("moves no pool teaches: " + ", ".join(unusedMoves))

    with open(os.path.join(DATA, "moves.json"), "w", encoding="utf-8", newline="\n") as handle:
        handle.write(render(table, by_id, pools, assigned))

    counts = {}
    for pool in assigned.values():
        counts[pool] = counts.get(pool, 0) + 1
    print("moves:", len(by_id), "pools:", len(pools), "species with their own pool:", len(assigned), "of", len(species))
    for pool in sorted(counts, key=counts.get, reverse=True):
        print("  %-14s %d" % (pool, counts[pool]))


if __name__ == "__main__":
    sys.exit(main())
