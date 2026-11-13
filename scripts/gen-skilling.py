#!/usr/bin/env python3
"""Checks scripts/skilling-species.json and copies it to the resources as skilling.json (issue 90).

Run from the repository root:

    python3 scripts/gen-skilling.py

The file is hand-written: per gathering skill a message and a list of {species, minLevel}, plus the exclusive
species (met only while skilling) with their odds. The run aborts, writing nothing, when a name is not a species in
species.json, the species is quest-only, or its first NPC id has no row in animations.json (it could not be drawn).
"""
import json
import os
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources", "com", "oveduumnakal", "gielinorgo")
SKILLS = ("FISHING", "WOODCUTTING", "MINING", "FARMING", "HUNTER")


def main():
    source = os.path.join(HERE, "skilling-species.json")
    with open(source, encoding="utf-8") as f:
        table = json.load(f)
    with open(os.path.join(RES, "species.json"), encoding="utf-8") as f:
        species = {s["name"].strip().lower(): s for s in json.load(f)}
    with open(os.path.join(RES, "animations.json"), encoding="utf-8") as f:
        animations = json.load(f)

    problems = []

    def check(name):
        found = species.get(name.strip().lower())
        if found is None or found.get("quest"):
            problems.append("not a catchable species: " + name)
        elif not found.get("ids") or str(found["ids"][0]) not in animations:
            problems.append("no animations for the first id of: " + name)

    if sorted(table["skills"]) != sorted(SKILLS):
        problems.append("skills must be exactly " + ", ".join(SKILLS))
    for skill, block in table["skills"].items():
        if not block.get("species"):
            problems.append("no species for " + skill)
        for row in block.get("species", []):
            check(row["species"])
    for row in table.get("exclusive", []):
        check(row["species"])
        if row["skill"] not in SKILLS or row["oneIn"] < 1:
            problems.append("bad exclusive row: " + row["species"])

    if problems:
        print("\n".join(problems))
        sys.exit(1)

    shutil.copyfile(source, os.path.join(RES, "skilling.json"))
    print("wrote skilling.json")


if __name__ == "__main__":
    main()
