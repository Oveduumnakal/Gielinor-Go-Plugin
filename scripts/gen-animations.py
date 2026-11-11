#!/usr/bin/env python3
"""Generate animations.json (animation ids per NPC id) for the follower and the battle scenes.

The RuneLite API does not expose an NPC type's animations, so they are bundled. The ids come from the
NPC definitions in a public dump of the game cache (https://github.com/abextm/osrs-cache).

Each row is [standing, walking, attack, defend, death, second attack]; -1 means the NPC has none. Standing and walking are
in the NPC definition. Attack, defend (the hit reaction) and death are NOT: the cache has no link from an NPC
to them. They are found by Jagex's own sequence names (the dump's gamevals/7): an NPC whose walk is named
`goblin_walk` gets `goblin_attack`, `goblin_death` and `goblin_defend` (or `_block`, `_hit`). Humanoid NPCs
that walk with the player's skeleton (`human_*`) get the unarmed punch, block and death. Animations that
are Maya based are skipped, since they are not known to play on a model that is not an NPC.

The attack also follows the species' combat style (`style` in species.json, which the battle sim uses): a MAGIC
creature prefers a sequence of its family named for casting (`cast`, `magic`, `spell`, `mage`, `blast`, `breath`,
`fireball`), a RANGED one a shot or throw (`range`, `ranged`, `shoot`, `throw`, `bow`, `spit`, `bolt`, `arrow`),
a MELEE one the plain `_attack` or a blow (`slash`, `stab`, `crush`, `bite`, `claw`, `punch`, `kick`). The words
are matched as whole `_` separated parts of the name, so `fireplace` or `castle` never count. When nothing of the
family fits the style, the name-only pick is used. A species' ids, and the ids they transform into, share its
style. Humanoids on the player skeleton get a player animation by style: `human_caststrike` (a spell with no
staff pose), `human_bow`, or the unarmed punch. Defend and death do not depend on the style.

Ranged humanoids on the player skeleton do not all shoot a bow. HUMAN_RANGED_WEAPONS names who uses another weapon
(from the species name and the OSRS wiki: Karil's crossbow, the thrown books, chinchompas and bottles of poison):
a crossbow user gets `human_crossbow`, a thrower `human_throw`, everyone else keeps `human_bow`.

A species with a second style (`secondStyle` in species.json: the dragons and wyverns that breathe, see
gen-species.py) also gets a second attack, the last column: the breath animation. It is looked up by skeleton in
SECOND_ATTACKS, so a dragon on the dragon skeleton gets `dragon_firebreath_all_attack` while its attack stays the
melee bite. This is an animation only; the battle sim rolls the second style (see BattleSim).

Some creatures have their idle and walk on a skeleton their combat sequences do not use (a wyrm walks submerged).
SIBLING_SKELETON_FAMILIES lists the families whose attack may use the skeleton of the family's `_ready` sequence.

Besides the file, the run prints every NPC id whose attack differs from the file it replaces, with the reason.

A candidate must also use the same skeleton (framemap) as the NPC's standing or
walking animation, or its skeleton must be unknown. The file has one `sequenceId framemapId` pair per line,
-1 when the frame could not be read. It is made by reading each sequence's first frame (group = frameId >> 16,
file = frameId & 0xFFFF in the cache's animation index) and taking the first two bytes of that file, with the
RuneLite cache library. The dump does not carry frames, so this step needs a copy of the game cache.

Run from the repo root, after gen-species.py:
    python3 scripts/gen-animations.py dump.tar.gz skeletons.txt

Both files are required. The run aborts, writing nothing, when the sequence names or skeletons parse empty, or when
a species with a second style would lose a second attack the current file has.
"""
import bisect
import json
import re
import sys
import tarfile

SPECIES = "src/main/resources/com/oveduumnakal/gielinorgo/species.json"
OUT = "src/main/resources/com/oveduumnakal/gielinorgo/animations.json"
NPC_DEF = re.compile(r"(?:^|/)npc_defs/(\d+)\.json$")
SEQUENCE_DEF = re.compile(r"(?:^|/)sequences/(\d+)\.json$")
SEQUENCE_NAME = re.compile(r"(?:^|/)gamevals/7/(\d+)(?:\.json)?$")

# The state part of a sequence name: everything before it is the creature's family, everything after is a variant.
# The state word is a whole `_` separated part (digits may follow it) and the last such part wins, so
# `cave_crawler_walk` is the family `cave_crawler`, not `cave`.
STATE = re.compile(
    r"^(.+)_(?:walk|ready|idle|stand|run|move|fly|swim|crawl|hover|float|flying|walking|standing|moving)"
    r"(\d*)(_.*)?$")


def family_of(prefix):
    """Strips state words left at the end of a family: `dream_untouchable_ready` (of `..._ready_walk`) gives
    `dream_untouchable`."""
    match = STATE.match(prefix)
    while match:
        prefix = match.group(1)
        match = STATE.match(prefix)
    return prefix


# What each combat animation is called, best first.
KINDS = {
    "attack": ["attack"],
    "defend": ["defend", "block", "hit", "hurt", "flinch"],
    "death": ["death", "die", "dead"],
}
# What a humanoid that walks with the player's skeleton does. The attack depends on the style, see HUMAN_ATTACK.
HUMAN = {"attack": "human_unarmedpunch", "defend": "human_unarmedblock", "death": "human_death"}
HUMAN_ATTACK = {"MELEE": "human_unarmedpunch", "RANGED": "human_bow", "MAGIC": "human_caststrike"}
# Whole parts of a sequence name (split at `_`, digits dropped) that say how an attack is made, per style.
STYLE_WORDS = {
    "MAGIC": {"cast", "casting", "spellcast", "magic", "spell", "spells", "mage", "mager", "blast", "breath",
              "firebreath", "fireball", "caststrike", "castwave", "castblast", "magicattack"},
    "RANGED": {"range", "ranged", "rangeattack", "shoot", "shot", "shooting", "throw", "throwing", "thrown", "bow",
               "shortbow", "longbow", "crossbow", "xbow", "xbows", "spit", "bolt", "arrow", "dart", "darts",
               "launch", "fling", "hurl", "sling", "snipe", "ranger", "slime", "iceball"},
    "MELEE": {"slash", "stab", "crush", "bite", "claw", "claws", "punch", "kick", "swipe", "smash", "lunge", "whip",
              "sting", "melee", "meleeattack", "scimitar", "doublescimitar"},
}
# Who among the ranged humanoids does not use a bow: (species name pattern, player sequence, why). First match wins.
HUMAN_RANGED_WEAPONS = [
    (re.compile(r"crossbow|xbow", re.I), "human_crossbow", "name says crossbow"),
    (re.compile(r"^Karil the Tainted$"), "human_crossbow", "Karil fights with his crossbow"),
    (re.compile(r"thrower|chinchompa|knife|knives|dart|javelin", re.I), "human_throw", "name says a thrown weapon"),
    (re.compile(r"^(Crazy|Deranged) archaeologist$"), "human_throw", "wiki: throws explosive books"),
    (re.compile(r"^Ranging Ro$"), "human_throw", "wiki: attacks by throwing black chinchompas"),
    (re.compile(r"^The Forsaken Assassin$"), "human_throw", "wiki: throws bottles of poison"),
]
# The second attack of a species with a second style, by name. The one on the NPC's own skeleton is used.
SECOND_ATTACKS = ["dragon_firebreath_all_attack", "fossil_npc_wyvern_flight_breath", "ds2_vorkath_ranged"]
# Families that walk on another skeleton than the one of their ready and combat sequences.
SIBLING_SKELETON_FAMILIES = {"wyrm": "wyrm_ready"}
# Parts of a name that mean the sequence is not a plain attack of the family.
NOT_ATTACK = {"walk", "run", "ready", "idle", "stand", "death", "die", "dead", "defend", "def", "block", "hit", "hurt",
              "flinch", "special", "spec", "spawn", "emerge", "appear", "transform", "pvn", "walkmerge", "teleport",
              "tele", "heal", "summon", "enchant", "open", "close", "up", "down", "end", "start", "getup"}


def read_dump(source):
    """Reads the NPC definitions, the sequence names and the sequences that are Maya based."""
    defs = {}
    names = {}
    maya = set()
    with tarfile.open(fileobj=source, mode="r:gz") as tar:
        for member in tar:
            if not member.isfile():
                continue
            match = NPC_DEF.search(member.name)
            if match:
                defs[int(match.group(1))] = json.load(tar.extractfile(member))
                continue
            match = SEQUENCE_NAME.search(member.name)
            if match:
                entry = json.load(tar.extractfile(member))
                names[entry["id"]] = entry["name"]
                continue
            match = SEQUENCE_DEF.search(member.name)
            if match:
                entry = json.load(tar.extractfile(member))
                if entry.get("animMayaID", -1) >= 0:
                    maya.add(entry["id"])
    return defs, names, maya


def read_skeletons(path):
    skeletons = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            parts = line.split()
            if len(parts) == 2:
                skeletons[int(parts[0])] = int(parts[1])
    return skeletons


class Sequences:
    """Finds the combat animations of a creature from the sequence names."""

    def __init__(self, names, maya, skeletons):
        self.names = names
        self.maya = maya
        self.skeletons = skeletons
        self.by_name = {}
        for seq_id, name in sorted(names.items(), key=lambda item: (len(item[1]), item[0])):
            self.by_name.setdefault(name, seq_id)
        self.sorted_names = sorted((name, seq_id) for seq_id, name in names.items())
        self.keys = [name for name, _ in self.sorted_names]
        self.memo = {}
        self.family_names = set()
        for name in names.values():
            match = STATE.match(name)
            if match:
                self.family_names.add(family_of(match.group(1)))

    def starting_with(self, prefix):
        """Sequence ids whose name starts with prefix, the shortest name first."""
        if prefix not in self.memo:
            at = bisect.bisect_left(self.keys, prefix)
            found = []
            while at < len(self.keys) and self.keys[at].startswith(prefix):
                found.append(self.sorted_names[at])
                at += 1
            found.sort(key=lambda pair: (len(pair[0]), pair[1]))
            self.memo[prefix] = [seq_id for _, seq_id in found]
        return self.memo[prefix]

    def families(self, definition):
        """The (family, variant) of the walking and standing animation: goblin_walk gives ('goblin', '')."""
        found = []
        for key in ("walkingAnimation", "standingAnimation"):
            name = self.names.get(definition.get(key, -1))
            if not name:
                continue
            match = STATE.match(name)
            family = (family_of(match.group(1)), (match.group(2) or "") + (match.group(3) or "")) if match else (
                re.sub(r"_?\d+$", "", name), "")
            if family[0] and family not in found:
                found.append(family)
        return found

    def humanoid(self, definition):
        for key in ("walkingAnimation", "standingAnimation"):
            if (self.names.get(definition.get(key, -1)) or "").startswith("human_"):
                return True
        return False

    def own_skeletons(self, definition):
        wanted = set()
        for key in ("walkingAnimation", "standingAnimation"):
            skeleton = self.skeletons.get(definition.get(key, -1), -1)
            if skeleton >= 0:
                wanted.add(skeleton)
        return wanted

    def fits(self, seq_id, wanted):
        if seq_id in self.maya:
            return False
        skeleton = self.skeletons.get(seq_id, -1)
        return not wanted or skeleton < 0 or skeleton in wanted

    def styled(self, prefix, style, wanted):
        """The sequence of the family whose name fits the style best, or -1.

        Names are `prefix_` plus parts. A name qualifies when it has a part of the style's words and no part that
        marks another state (walk, death, special). Names that also say `attack` come first (`goblin_attack_magic`
        before `goblin_cast`), then the shorter name, then the lower id. For MELEE the plain `_attack` is the best.
        A name that starts with another family (`goblin_champion_attack` for the prefix `goblin`) only counts when
        nothing of the exact family fits.
        """
        words = STYLE_WORDS[style]
        found = []
        for seq_id in self.starting_with(prefix + "_"):
            parts = [re.sub(r"\d+$", "", part) for part in self.names[seq_id][len(prefix) + 1:].split("_")]
            parts = [part for part in parts if part]
            if not self.fits(seq_id, wanted) or any(part in NOT_ATTACK for part in parts):
                continue
            plain = parts == ["attack"]
            if style == "MELEE" and plain:
                rank = 0
            elif any(part in words for part in parts):
                rank = 1 if "attack" in parts else 2
            else:
                continue
            raw = self.names[seq_id][len(prefix) + 1:].split("_")
            other = any(prefix + "_" + "_".join(raw[:k]) in self.family_names for k in range(1, len(raw)))
            found.append((other, rank, len(self.names[seq_id]), seq_id))
        return min(found)[3] if found else -1

    def pick_attack(self, definition, style, species_name=""):
        """The attack animation and why it was chosen: (sequence id, reason).

        The reason is "weapon" (a ranged humanoid's own weapon), "style" (it fits the style), "fallback" (the
        name-only pick, which may be of another style) or "none".
        """
        wanted = self.own_skeletons(definition)
        if self.humanoid(definition) and style in HUMAN_ATTACK:
            name = HUMAN_ATTACK[style]
            reason = "style"
            if style == "RANGED":
                for pattern, sequence, why in HUMAN_RANGED_WEAPONS:
                    if pattern.search(species_name):
                        name, reason = sequence, "weapon: " + why
                        break
            seq_id = self.by_name.get(name, -1)
            if seq_id >= 0 and self.fits(seq_id, wanted):
                return seq_id, reason
        if style in STYLE_WORDS:
            for prefix, _ in self.families(definition):
                seq_id = self.styled(prefix, style, self.family_skeletons(prefix, wanted))
                if seq_id >= 0:
                    return seq_id, "style"
        seq_id = self.find(definition, "attack")
        return seq_id, "fallback" if seq_id >= 0 else "none"

    def family_skeletons(self, prefix, wanted):
        """The skeletons a family's attack may use: its own, plus its ready sequence's for a listed family."""
        ready = self.by_name.get(SIBLING_SKELETON_FAMILIES.get(prefix, ""), -1)
        skeleton = self.skeletons.get(ready, -1)
        return wanted | {skeleton} if skeleton >= 0 else wanted

    def second_attack(self, definition):
        """The breath animation of a species with a second style, from the skeleton the NPC walks on, or -1."""
        wanted = self.own_skeletons(definition)
        if not self.skeletons:
            raise SystemExit("the skeletons file is empty, nothing to match a second attack against")
        for name in SECOND_ATTACKS:
            seq_id = self.by_name.get(name, -1)
            if wanted and seq_id >= 0 and seq_id not in self.maya and self.skeletons.get(seq_id, -1) in wanted:
                return seq_id
        return -1

    def find(self, definition, kind):
        wanted = self.own_skeletons(definition)
        if self.humanoid(definition):
            seq_id = self.by_name.get(HUMAN[kind], -1)
            if seq_id >= 0 and self.fits(seq_id, wanted):
                return seq_id
        for prefix, rest in self.families(definition):
            for word in KINDS[kind]:
                base = prefix + "_" + word
                exact = [base + rest, base, base + "01", base + "_01", base + "1"]
                candidates = [self.by_name[n] for n in exact if n in self.by_name]
                candidates += self.starting_with(base)
                for seq_id in candidates:
                    if self.fits(seq_id, wanted):
                        return seq_id
        return -1


def wanted_ids(defs):
    """The NPC ids of every species plus the ids those turn into (varbit and varp transforms).

    A species' own ids are claimed before any transform child, so a child never takes an id another species lists.
    Returns (styles, species, owners): styles maps each NPC id to its species' style (the first species wins for an
    id two species share), species is the list of (name, style, ids) of the species that have ids, and owners maps
    each NPC id to (species name, whether the species has a second style).
    """
    with open(SPECIES, encoding="utf-8") as f:
        entries = json.load(f)
    styles = {}
    owners = {}
    species = []
    expanded = []
    for entry in entries:
        style = entry.get("style", "MELEE")
        own = set(entry.get("ids", []))
        ids = set(own)
        for npc_id in list(own):
            for child in defs.get(npc_id, {}).get("configs") or []:
                if child is not None and child > 0:
                    ids.add(child)
        expanded.append((entry, style, own, ids))
    for entry, style, own, _ in expanded:
        for npc_id in own:
            styles.setdefault(npc_id, style)
            owners.setdefault(npc_id, (entry["name"], "secondStyle" in entry))
    for entry, style, _, ids in expanded:
        for npc_id in ids:
            styles.setdefault(npc_id, style)
            owners.setdefault(npc_id, (entry["name"], "secondStyle" in entry))
        if ids:
            species.append((entry["name"], style, ids))
    return styles, species, owners


def read_previous():
    """The row of each NPC id in the file about to be replaced, or {} when there is none."""
    try:
        with open(OUT, encoding="utf-8") as f:
            old = json.load(f)
    except (OSError, ValueError):
        return {}
    return {int(npc_id): row + [-1] * (6 - len(row)) for npc_id, row in old.items()}


def main():
    if len(sys.argv) < 3:
        raise SystemExit("the skeletons file is required: gen-animations.py dump.tar.gz skeletons.txt "
                         "(without it every second attack and skeleton check silently disappears)")
    with open(sys.argv[1], "rb") as f:
        defs, names, maya = read_dump(f)
    if not names:
        raise SystemExit("no sequence names found in the dump (gamevals/7); nothing written")
    skeletons = read_skeletons(sys.argv[2])
    if not skeletons:
        raise SystemExit("no skeletons found in " + sys.argv[2] + "; nothing written")
    sequences = Sequences(names, maya, skeletons)
    previous = read_previous()
    rows = []
    missing = 0
    found = {"attack": 0, "defend": 0, "death": 0, "second attack": 0}
    styles, species, owners = wanted_ids(defs)
    matched = {}
    changes = []
    second_now = {}
    for npc_id in sorted(styles):
        definition = defs.get(npc_id)
        if definition is None:
            missing += 1
            continue
        stand = definition.get("standingAnimation", -1)
        walk = definition.get("walkingAnimation", -1)
        if stand < 0 and walk < 0:
            continue
        species_name, has_second = owners[npc_id]
        attack, reason = sequences.pick_attack(definition, styles[npc_id], species_name)
        matched[npc_id] = (attack, reason == "style" or reason.startswith("weapon"))
        second = sequences.second_attack(definition) if has_second else -1
        combat = [attack, sequences.find(definition, "defend"), sequences.find(definition, "death"), second]
        for kind, seq_id in zip(("attack", "defend", "death", "second attack"), combat):
            if seq_id >= 0:
                found[kind] += 1
        if npc_id in previous and previous[npc_id][2] != attack:
            changes.append((npc_id, species_name, styles[npc_id], names.get(previous[npc_id][2], "none"),
                            names.get(attack, "none"), reason))
        rows.append('"%d":[%d,%d,%d,%d,%d,%d]' % ((npc_id, stand, walk) + tuple(combat)))
        if has_second:
            second_now.setdefault(species_name, []).append(second)
    lost = sorted(name for name, seconds in second_now.items() if max(seconds) < 0 and any(
        previous.get(i, [0] * 6)[5] >= 0 for i, (owner, _) in owners.items() if owner == name))
    if lost:
        raise SystemExit("species with a second style would lose their second attack, nothing written: "
                         + ", ".join(lost))
    with open(OUT, "w", encoding="utf-8") as f:
        f.write("{\n" + ",\n".join(rows) + "\n}\n")
    print("npc ids with animations:", len(rows))
    print("npc ids missing from the dump:", missing)
    print("npc ids with attack / defend / death / second attack:", found["attack"], found["defend"], found["death"],
          found["second attack"])
    for style in ("MELEE", "RANGED", "MAGIC"):
        counts = {"style-matched": 0, "fallback": 0, "none": 0}
        for _, species_style, ids in species:
            if species_style != style:
                continue
            picks = [matched[i] for i in ids if i in matched]
            if any(fits_style for _, fits_style in picks):
                counts["style-matched"] += 1
            elif any(attack >= 0 for attack, _ in picks):
                counts["fallback"] += 1
            else:
                counts["none"] += 1
        print("species with ids,", style + ":", counts)
    print("attack changed against the previous file:", len(changes))
    for npc_id, species_name, style, was, now, reason in changes:
        print("  %d %s (%s): %s -> %s [%s]" % (npc_id, species_name, style, was, now, reason))


if __name__ == "__main__":
    main()
