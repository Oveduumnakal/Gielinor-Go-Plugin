#!/usr/bin/env python3
"""Fails when the UI Lab no longer matches the plugin. Run it by hand, or as ./gradlew checkUiLab. It is not part of
./gradlew build, so a change to the painters never waits on the lab.

It checks that:
  1. the lab still builds against the plugin's code, with every hook finding its place;
  2. every preset in tools/ui-lab/presets still points at something, and was made from the value the code still has.

    python tools/ui-lab/check.py
"""
import json
import os
import pathlib
import re
import subprocess
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

import ui_lab  # noqa: E402

LAB = ui_lab.LAB
PRESETS = LAB / "presets"


def normalized(line):
    """A source line the way the lab names the element it draws."""
    return re.sub(r"LabLit_\w+\.v\d+_\d+", "#", line.strip())


def current():
    """Builds the lab away from any running one, and reads back everything a preset can point at."""
    ui_lab.BUILD = ui_lab.ROOT / "build" / "ui-lab-check"
    ui_lab.BUILD.mkdir(parents=True, exist_ok=True)
    cp, classes = ui_lab.compile_lab(os.environ.get("UILAB_CP"))
    run = os.pathsep.join([str(classes), str(ui_lab.RESOURCES), cp])
    dump = ui_lab.BUILD / "dump.json"
    args = ui_lab.BUILD / "java.args"
    args.write_text('-Djava.awt.headless=true\n-cp "%s"\ncom.oveduumnakal.gielinorgo.LabServer\n"%s"\n"%s"\ndump\n"%s"\n'
                    % (run.replace("\\", "/"), (LAB / "web").as_posix(), (ui_lab.BUILD / "controls.json").as_posix(),
                       dump.as_posix()), encoding="utf-8")
    subprocess.run([ui_lab.java_tool("java"), "@" + str(args)], cwd=ui_lab.ROOT, check=True)
    return json.loads(dump.read_text(encoding="utf-8"))


def element_exists(key):
    """Whether the line that draws an element ("el|Class.method|kind|line") is still there."""
    _, owner, _, statement = key.split("|", 3)
    source = ui_lab.BUILD / "src" / ui_lab.PACKAGE / (owner.split(".")[0] + ".java")
    if not source.exists():
        return False
    return statement in (normalized(line) for line in source.read_text(encoding="utf-8").splitlines())


def preset_problem(entry, known, boxes):
    ident = entry.get("id", "")
    if ident.startswith("layout:"):
        name = ident[len("layout:"):].rsplit(".", 1)[0]
        if name.startswith("el|"):
            return None if element_exists(name) else "the line that drew this element has changed"
        return None if name in boxes else "there is no pane or button of this name"
    if ident not in known:
        return "there is no such value any more"
    was, now = entry.get("was"), known[ident]
    if was is not None and abs(float(was) - float(now)) > 1e-6:
        return "made when the value was %s; the code now has %s" % (was, now)
    return None


def check_presets(dump):
    known = {c["id"]: c["value"] for key in ("controls", "themeColors") for c in dump[key]}
    boxes = set(dump["boxes"])
    problems = []
    for path in sorted(PRESETS.glob("*.json")) if PRESETS.exists() else []:
        try:
            entries = json.loads(path.read_text(encoding="utf-8"))
            found = [(e.get("id"), preset_problem(e, known, boxes)) for e in entries]
        except (ValueError, AttributeError) as error:
            problems.append("preset %s is not readable: %s" % (path.name, error))
            continue
        problems += ["preset %s: %s: %s" % (path.name, ident, what) for ident, what in found if what]
    return problems


def main():
    problems = check_presets(current())
    if problems:
        print("UI Lab check failed:\n  " + "\n  ".join(problems))
        sys.exit(1)
    print("UI Lab check passed.")


if __name__ == "__main__":
    main()
