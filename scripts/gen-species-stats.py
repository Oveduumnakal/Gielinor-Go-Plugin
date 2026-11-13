#!/usr/bin/env python3
"""Write the per-species stat factors ("stats": [health, attack, defence, speed]) into species.json (issue #86).

Run from the repo root, after gen-species.py: python3 scripts/gen-species-stats.py

The species list stays as it is; only the "stats" field of each species is added or replaced, in the same one-line
format. The wiki rows are picked with the same rule as the species' level (gen-species.py usable_rows, highest combat
level), so a species' stats and its level come from one row.

Each factor is 0.75 to 1.25 (the owner's limit) and shapes a stat around the formula in Combatant.java:
  health   hitpoints against a curve fitted over all species by combat level
  attack   max hit (the strength level when there is none), same curve idea
  defence  defence level, same curve idea
  speed    attack speed in ticks: 3 or faster 1.25, 4 1.0, 5 0.9, 6 or slower 0.8
Missing wiki numbers give 1.0. The three curve-based ratios and the speed value are scaled so their product is 1.0
(a species that is tough pays for it elsewhere), then the three ratios are squeezed smoothly into the range.

Never worse: along every evolution link of evolutions.json, a stat of the later stage is lifted until, at every
creature level from the evolution level to 99, it is at least what the earlier stage had. The lifts are printed.
"""
import importlib.util
import json
import math
import os
import re
import sys
import time
import urllib.parse
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("gen_species", os.path.join(HERE, "gen-species.py"))
gen_species = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gen_species)

OUT = gen_species.OUT
EVOLUTIONS = "src/main/resources/com/oveduumnakal/gielinorgo/evolutions.json"
QUERY = ("bucket('infobox_monster').select('page_name','name','combat_level','hitpoints','max_hit','strength_level',"
         "'defence_level','attack_speed').limit(%d).offset(%d).run()")
MIN_FACTOR = 0.75
MAX_FACTOR = 1.25
FAST_MAX_ATTACK_SPEED = gen_species.FAST_MAX_ATTACK_SPEED
SPEED_BY_TICKS = {4: 1.0, 5: 0.9}
MAX_LEVEL = 99


def fetch(offset):
    params = urllib.parse.urlencode({"action": "bucket", "format": "json",
                                     "query": QUERY % (gen_species.PAGE, offset)})
    req = urllib.request.Request(gen_species.API + "?" + params, headers={"User-Agent": gen_species.UA})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.load(resp).get("bucket", [])


def first_number(value):
    """The first whole number in a wiki value (a list or a string like '28 (Melee)'), 0 when there is none."""
    for item in gen_species.as_list(value):
        found = re.search(r"\d+", str(item))
        if found:
            return int(found.group())
    return 0


def speed_value(ticks):
    """The speed factor for an attack speed in ticks, 1.0 when the wiki has none."""
    if ticks <= 0:
        return 1.0
    if ticks <= FAST_MAX_ATTACK_SPEED:
        return MAX_FACTOR
    return SPEED_BY_TICKS.get(ticks, 0.8)


def fit(points):
    """Least squares line of ln(value) against ln(level); returns (intercept, slope)."""
    xs = [math.log(level) for level, _ in points]
    ys = [math.log(value) for _, value in points]
    mx, my = sum(xs) / len(xs), sum(ys) / len(ys)
    var = sum((x - mx) ** 2 for x in xs)
    slope = sum((x - mx) * (y - my) for x, y in zip(xs, ys)) / var
    return my - slope * mx, slope


def squeeze(log_ratio):
    """Maps a log ratio smoothly into 0.75..1.25: a ratio of 2 gives about 1.19, 4 about 1.24, 0.5 about 0.81."""
    return 1.0 + 0.25 * math.tanh(log_ratio / math.log(2))


def raw_numbers(row):
    return (first_number(row.get("hitpoints")),
            first_number(row.get("max_hit")) or first_number(row.get("strength_level")),
            first_number(row.get("defence_level")),
            first_number(row.get("attack_speed")))


def factors(level, numbers, curves):
    """Returns [health, attack, defence, speed] for a species with the given combat level and wiki numbers."""
    logs = []
    for number, (intercept, slope) in zip(numbers[:3], curves):
        if number > 0:
            logs.append(math.log(number) - (intercept + slope * math.log(level)))
        else:
            logs.append(0.0)
    speed = speed_value(numbers[3])
    mean = (sum(logs) + math.log(speed)) / 4
    health, attack, defence = (squeeze(x - mean) for x in logs)
    speed = min(MAX_FACTOR, max(MIN_FACTOR, speed / math.exp(mean)))
    return [round(health, 2), round(attack, 2), round(defence, 2), round(speed, 2)]


def stat_value(index, species_level, level, factor):
    """The same formulas as Combatant.java with an average roll, rounded like Math.round."""
    tier = math.sqrt(max(1, species_level))
    if index == 0:
        base = 20 + 2.0 * level + tier * level / 2.0
    elif index == 1:
        base = 5 + level / 2.0 + tier * level / 8.0
    elif index == 2:
        base = 5 + level / 2.0 + tier * level / 10.0
    else:
        base = 5 + level // 2
    return math.floor(base * factor + 0.5)


def links(species):
    """The evolution links the game uses (see EvolutionTable.java): first link per species, no quest-only target."""
    chosen = {}
    with open(EVOLUTIONS, encoding="utf-8") as f:
        for step in json.load(f):
            src, dst = step["from"].lower(), step["to"].lower()
            if src not in species or dst not in species or species[dst].get("quest"):
                continue
            chosen.setdefault(src, (src, dst, step["level"]))
    return list(chosen.values())


def never_worse(species):
    """Lifts the later stage of every link; returns the list of (species, stat index, old, new) and the unfixable."""
    lifted, stuck = {}, []
    for _ in range(20):
        changed = False
        for src, dst, evo_level in links(species):
            a, b = species[src], species[dst]
            for i in range(4):
                factor = b["stats"][i]
                while factor <= MAX_FACTOR and not all(
                        stat_value(i, b["level"], lvl, factor) >= stat_value(i, a["level"], lvl, a["stats"][i])
                        for lvl in range(evo_level, MAX_LEVEL + 1)):
                    factor = round(factor + 0.01, 2)
                if factor > MAX_FACTOR:
                    stuck.append((src, dst, i))
                elif factor > b["stats"][i]:
                    lifted.setdefault((dst, i), b["stats"][i])
                    b["stats"][i] = factor
                    changed = True
        if not changed:
            break
    return lifted, sorted(set(stuck))


def main():
    with open(OUT, encoding="utf-8") as f:
        entries = json.load(f)
    species = {e["name"].lower(): e for e in entries}
    rows = []
    offset = 0
    while True:
        page = fetch(offset)
        if not page:
            break
        rows.extend(page)
        offset += gen_species.PAGE
        time.sleep(1)
    chosen = {}
    for key, name, page, level, row in gen_species.usable_rows(rows, gen_species.load_blocklist()):
        if key not in chosen or level > chosen[key][0]:
            chosen[key] = (level, raw_numbers(row))
    curves = []
    for index in range(3):
        points = [(level, nums[index]) for level, nums in chosen.values() if nums[index] > 0]
        curves.append(fit(points))
        print("curve %d: ln(stat) = %.3f + %.3f ln(level), %d species" % (index, *curves[-1], len(points)))
    missing = []
    for key, e in species.items():
        level, numbers = chosen.get(key, (e["level"], (0, 0, 0, 0)))
        if key not in chosen:
            missing.append(e["name"])
        e["stats"] = factors(e["level"], numbers, curves)
    print("no wiki row (all 1.0):", len(missing), missing[:20])
    lifted, stuck = never_worse(species)
    names = ("health", "attack", "defence", "speed")
    print("lifted by the never-worse pass:", len(lifted))
    for (dst, i), old in sorted(lifted.items())[:40]:
        print("  %s %s %.2f -> %.2f" % (species[dst]["name"], names[i], old, species[dst]["stats"][i]))
    print("could not be lifted within 1.25:", len(stuck), stuck[:20])
    with open(OUT, "w", encoding="utf-8") as f:
        f.write("[\n")
        f.write(",\n".join(json.dumps(e, separators=(",", ":"), ensure_ascii=False) for e in entries))
        f.write("\n]\n")
    for i, name in enumerate(names):
        values = sorted(e["stats"][i] for e in entries)
        print(name, "min %.2f median %.2f max %.2f" % (values[0], values[len(values) // 2], values[-1]))


if __name__ == "__main__":
    main()
