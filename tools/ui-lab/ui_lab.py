#!/usr/bin/env python3
"""UI Lab: draws the plugin's UI in a browser with its own painters, and lets you change values while you look.

Run from anywhere:
    python tools/ui-lab/ui_lab.py            build, start and open the page
    python tools/ui-lab/ui_lab.py --no-open  do not open a browser

How it works. The plugin's sources are copied to build/ui-lab. In the copy only, each named number and colour of the UI
classes stops being final, and each class gets methods that set them by name, so no reflection is needed. The theme's
colour lookup asks the lab for an override first. The copy is compiled with a small web server (java/) that renders
scenes with the real painting code. Nothing under src/ is written to.

Rebuild (restart the script) after you change the plugin's code.
"""
import json
import os
import pathlib
import re
import shutil
import subprocess
import sys

LAB = pathlib.Path(__file__).resolve().parent
ROOT = LAB.parents[1]
BUILD = ROOT / "build" / "ui-lab"
PACKAGE = "com/oveduumnakal/gielinorgo"
MAIN = ROOT / "src" / "main" / "java" / PACKAGE
RESOURCES = ROOT / "src" / "main" / "resources"
TEST_SOURCES = ROOT / "src" / "test" / "java"

# The classes whose named values become controls: what draws, lays out, times or moves the UI.
UI_CLASSES = re.compile(
    r"^(.*(Painter|Overlay|Layout|Style|Motion|Tokens|Times|Theme|Page|Fonts|Fit|Icons|Frame|Scene|Bubbles|Tip|Tooltip)"
    r"|Shapes|Parallax|Easing|Loop|CrispImages|JarFlight|JarRenderer|JarShapes|ItemShapes|JarLiveView|JarFixedView"
    r"|Window[A-Za-z]*|Glass[A-Za-z]*|Bubble[A-Za-z]*|FloatingText[A-Za-z]*)$")

DECLARATION = re.compile(
    r"^(?P<head>\t+(?:private |public |protected )?)static final (?P<type>int|long|double|float|Color) "
    r"(?P<name>[A-Z][A-Z0-9_]*) = (?P<value>[^;]+);\s*$")
NUMBER = re.compile(r"^-?(0[xX][0-9a-fA-F_]+[Ll]?|(\d[\d_]*)?\.?\d[\d_]*([eE]-?\d+)?[LlFfDd]?)$")
CACHE_FIELD = re.compile(r"static (?:final )?[\w.]*(?:Map|Set)<.*> (CACHE|TRIMMED|GREY)\b")
TYPE_OPEN = re.compile(r"\b(class|interface|enum)\s+([A-Za-z_]\w*)")


def without_noise(line, in_comment):
    """Blanks strings, characters and comments, so braces can be counted."""
    out = []
    i = 0
    while i < len(line):
        if in_comment:
            end = line.find("*/", i)
            if end < 0:
                return "".join(out), True
            i, in_comment = end + 2, False
            continue
        c = line[i]
        if line.startswith("//", i):
            break
        if line.startswith("/*", i):
            in_comment, i = True, i + 2
            continue
        if c in "\"'":
            j = i + 1
            while j < len(line) and line[j] != c:
                j += 2 if line[j] == "\\" else 1
            i = j + 1
            continue
        out.append(c)
        i += 1
    return "".join(out), in_comment


def color_value(text):
    """The ARGB of a colour written out in a declaration, or None when it is computed."""
    match = re.match(r"^new Color\(([^()]*?)(?:,\s*true)?\)$", text)
    if not match:
        named = {"Color.WHITE": 0xFFFFFFFF, "Color.BLACK": 0xFF000000}
        return named.get(text)
    parts = [p.strip().replace("_", "") for p in match.group(1).split(",")]
    try:
        numbers = [int(p, 0) for p in parts]
    except ValueError:
        return None
    if len(numbers) == 1:
        packed = numbers[0]
        return packed & 0xFFFFFFFF if text.endswith("true)") else 0xFF000000 | packed
    if len(numbers) in (3, 4):
        alpha = numbers[3] if len(numbers) == 4 else 255
        return alpha << 24 | numbers[0] << 16 | numbers[1] << 8 | numbers[2]
    return None


def number_value(text):
    if not NUMBER.match(text):
        return None
    clean = text.replace("_", "")
    if clean.lower().startswith(("0x", "-0x")):
        return int(clean.rstrip("Ll"), 16)
    clean = clean.rstrip("LlFfDd")
    return float(clean) if any(c in clean for c in ".eE") else int(clean)


def scan(path):
    """Finds the tunable declarations of one source file, with the class that holds each."""
    lines = path.read_text(encoding="utf-8").replace("\r\n", "\n").split("\n")
    stack, pending, in_comment, doc = [], None, False, []
    found = []
    for number, line in enumerate(lines):
        stripped = line.strip()
        if stripped.startswith("/**"):
            doc = []
        if stripped.startswith(("/**", "*")):
            doc.append(re.sub(r"^/\*\*|\*/$|^\*", "", stripped).strip())
        match = DECLARATION.match(line)
        if match and not in_comment:
            kinds = [k for k in stack if k]
            if kinds and kinds[-1][0] != "interface":
                nested = ".".join(name for _, name in kinds[1:])
                found.append({"line": number, "file": path.stem, "nested": nested, "name": match.group("name"),
                              "type": match.group("type"), "expr": match.group("value").strip(),
                              "doc": " ".join(d for d in doc if d and not d.startswith("@"))})
        if match or (stripped and not stripped.startswith(("/*", "*", "@"))):
            doc = []
        code, in_comment = without_noise(line, in_comment)
        opened = TYPE_OPEN.search(code)
        if opened:
            pending = (opened.group(1), opened.group(2))
        for c in code:
            if c == "{":
                stack.append(pending)
                pending = None
            elif c == "}" and stack:
                stack.pop()
    return lines, found


def collect():
    declarations, caches = [], {}
    for path in sorted(MAIN.glob("*.java")):
        text = path.read_text(encoding="utf-8")
        names = [m.group(1) for m in CACHE_FIELD.finditer(text)]
        if names:
            caches[path.stem] = sorted(set(names))
        if not UI_CLASSES.match(path.stem):
            continue
        _, found = scan(path)
        for entry in found:
            entry["id"] = "%s:%s%s" % (entry["file"], entry["nested"] + "." if entry["nested"] else "", entry["name"])
            if entry["type"] == "Color":
                entry["value"] = color_value(entry["expr"])
            else:
                entry["value"] = number_value(entry["expr"])
            entry["derived"] = entry["value"] is None
            declarations.append(entry)
    return declarations, caches


def field(entry):
    return (entry["nested"] + "." if entry["nested"] else "") + entry["name"]


def injected(stem, entries, caches):
    """The methods added to one copied class."""
    out = ["", "\tstatic boolean labSet(String name, double value)", "\t{", "\t\tswitch (name)", "\t\t{"]
    for e in entries:
        if not e["derived"] and e["type"] != "Color":
            cast = "" if e["type"] == "double" else "(%s) " % e["type"]
            rounded = "Math.round(value)" if e["type"] in ("int", "long") else "value"
            out.append('\t\t\tcase "%s": %s = %s%s; return true; /*lab:%s*/'
                       % (field(e), field(e), cast, rounded, e["id"]))
    out += ["\t\t\tdefault: return false;", "\t\t}", "\t}", ""]
    out += ["\tstatic boolean labColor(String name, int argb)", "\t{", "\t\tswitch (name)", "\t\t{"]
    for e in entries:
        if not e["derived"] and e["type"] == "Color":
            out.append('\t\t\tcase "%s": %s = new java.awt.Color(argb, true); return true; /*lab:%s*/'
                       % (field(e), field(e), e["id"]))
    out += ["\t\t\tdefault: return false;", "\t\t}", "\t}", "", "\tstatic void labRecompute()", "\t{"]
    for e in entries:
        if e["derived"]:
            out.append("\t\t%s = %s; /*lab:%s*/" % (field(e), e["expr"], e["id"]))
    out += ["\t}", "", "\tstatic void labClear()", "\t{"]
    out += ["\t\t%s.clear();" % name for name in caches]
    out += ["\t}"]
    return out


def write_copy(declarations, caches, excluded, no_recompute):
    source = BUILD / "src" / PACKAGE
    if source.exists():
        shutil.rmtree(source)
    shutil.copytree(MAIN, source)
    by_file = {}
    for entry in declarations:
        if entry["id"] not in excluded:
            by_file.setdefault(entry["file"], []).append(entry)
    classes = sorted(set(by_file) | set(caches))
    for stem in classes:
        path = source / (stem + ".java")
        lines = path.read_text(encoding="utf-8").replace("\r\n", "\n").split("\n")
        for e in by_file.get(stem, []):
            lines[e["line"]] = lines[e["line"]].replace("static final ", "static ", 1)
        last = max(i for i, line in enumerate(lines) if line.startswith("}"))
        entries = [dict(e, derived=False, type="skip") if e["id"] in no_recompute else e
                   for e in by_file.get(stem, [])]
        entries = [e for e in entries if e["type"] != "skip"]
        lines[last:last] = injected(stem, entries, caches.get(stem, []))
        path.write_text("\n".join(lines), encoding="utf-8", newline="\n")

    theme = source / "WindowTheme.java"
    text = theme.read_text(encoding="utf-8")
    old = "\t\treturn colors.get(name);\n"
    if old not in text:
        raise SystemExit("WindowTheme.color has changed; update ui_lab.py")
    text = text.replace(old, "\t\tColor lab = LabTheme.override(this, name);\n"
                             "\t\treturn lab != null ? lab : colors.get(name);\n", 1)
    theme.write_text(text, encoding="utf-8", newline="\n")

    registry = ["package com.oveduumnakal.gielinorgo;", "", "/** Generated by ui_lab.py. */", "final class LabRegistry",
                "{", "\tstatic boolean set(String file, String name, double value)", "\t{", "\t\tswitch (file)",
                "\t\t{"]
    registry += ['\t\t\tcase "%s": return %s.labSet(name, value);' % (s, s) for s in classes]
    registry += ["\t\t\tdefault: return false;", "\t\t}", "\t}", "",
                 "\tstatic boolean color(String file, String name, int argb)", "\t{", "\t\tswitch (file)", "\t\t{"]
    registry += ['\t\t\tcase "%s": return %s.labColor(name, argb);' % (s, s) for s in classes]
    registry += ["\t\t\tdefault: return false;", "\t\t}", "\t}", "", "\tstatic void refresh()", "\t{",
                 "\t\tfor (int pass = 0; pass < 2; pass++)", "\t\t{"]
    registry += ["\t\t\t%s.labRecompute();" % s for s in classes]
    registry += ["\t\t}", ""] + ["\t\t%s.labClear();" % s for s in classes] + ["\t}", "}", ""]
    (source / "LabRegistry.java").write_text("\n".join(registry), encoding="utf-8", newline="\n")

    for path in (LAB / "java").glob("*.java"):
        shutil.copy(path, source / path.name)
    return source


def java_tool(name):
    home = os.environ.get("JAVA_HOME")
    if home:
        return str(pathlib.Path(home) / "bin" / name)
    return name


def classpath():
    gradle = str(ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew"))
    out = subprocess.run([gradle, "-q", "-I", str(LAB / "classpath.gradle"), "uiLabClasspath"], cwd=ROOT,
                         capture_output=True, text=True)
    for line in out.stdout.splitlines():
        if line.startswith("UILAB_CP="):
            return line[len("UILAB_CP="):]
    raise SystemExit("could not read the classpath:\n" + out.stdout + out.stderr)


def blame(output, source, declarations):
    """Finds which declarations a failed compile is about: the ones to leave final, and recomputations to drop."""
    excluded, dropped = set(), set()
    by_name = {}
    for e in declarations:
        by_name.setdefault(e["name"], []).append(e)
    for match in re.finditer(r"^(.*?\.java):(\d+): error: (.*)$", output, re.M):
        path, number, message = pathlib.Path(match.group(1)), int(match.group(2)), match.group(3)
        if not path.exists():
            continue
        line = path.read_text(encoding="utf-8").split("\n")[number - 1]
        marker = re.search(r"/\*lab:([^*]+)\*/", line)
        if marker:
            entry = next(e for e in declarations if e["id"] == marker.group(1))
            (dropped if entry["derived"] else excluded).add(entry["id"])
            continue
        for name in re.findall(r"\b[A-Z][A-Z0-9_]*\b", line):
            candidates = by_name.get(name, [])
            local = [e for e in candidates if e["file"] == path.stem]
            excluded.update(e["id"] for e in (local or candidates))
    return excluded, dropped


def compile_lab():
    declarations, caches = collect()
    cp = classpath()
    excluded, dropped = set(), set()
    out = BUILD / "classes"
    for attempt in range(8):
        source = write_copy(declarations, caches, excluded, dropped)
        if out.exists():
            shutil.rmtree(out)
        out.mkdir(parents=True)
        args = BUILD / "javac.args"
        files = "\n".join('"%s"' % p.as_posix() for p in source.glob("*.java"))
        args.write_text('-encoding UTF-8\n--release 11\n-nowarn\n-Xmaxerrs 500\n-d "%s"\n-cp "%s"\n-sourcepath "%s"\n%s\n'
                        % (out.as_posix(), cp.replace("\\", "/"), TEST_SOURCES.as_posix(), files), encoding="utf-8")
        result = subprocess.run([java_tool("javac"), "@" + str(args)], capture_output=True, text=True)
        if result.returncode == 0:
            break
        more_excluded, more_dropped = blame(result.stderr, source, declarations)
        if not (more_excluded - excluded) and not (more_dropped - dropped):
            raise SystemExit("compile failed:\n" + result.stderr[:6000])
        excluded |= more_excluded
        dropped |= more_dropped
    else:
        raise SystemExit("compile still failing after several tries")

    controls = [{"id": e["id"], "file": e["file"], "name": field(e), "type": e["type"], "value": e["value"],
                 "line": e["line"] + 1, "doc": e["doc"]}
                for e in declarations if not e["derived"] and e["id"] not in excluded]
    (BUILD / "controls.json").write_text(json.dumps(controls), encoding="utf-8")
    print("UI Lab: %d controls; %d values stay fixed because the code needs them final"
          % (len(controls), len(excluded)))
    return cp, out


def main():
    BUILD.mkdir(parents=True, exist_ok=True)
    cp, classes = compile_lab()
    run = os.pathsep.join([str(classes), str(RESOURCES), cp])
    args = BUILD / "java.args"
    args.write_text('-Djava.awt.headless=true\n-cp "%s"\ncom.oveduumnakal.gielinorgo.LabServer\n"%s"\n"%s"\n%s\n'
                    % (run.replace("\\", "/"), (LAB / "web").as_posix(), (BUILD / "controls.json").as_posix(),
                       "no-open" if "--no-open" in sys.argv else "open"), encoding="utf-8")
    try:
        subprocess.run([java_tool("java"), "@" + str(args)], cwd=ROOT)
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
