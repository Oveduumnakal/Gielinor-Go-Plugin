"""Numbers and colours written straight into drawing code.

Each one found becomes a field of a generated holder class that the lab's copy of the code reads, so it can be changed
like a named value. Line numbers are never changed.
"""
import re

SKIPPED_NUMBERS = {0.0, 1.0}
LITERAL = re.compile(
    r"(?P<color>new Color\(\s*(?:0[xX][0-9a-fA-F_]+|\d+)(?:\s*,\s*(?:\d+|true))*\s*\))"
    r"|(?<=px\()(?P<px>\d+)(?=\))"
    r"|(?<![\w.])(?P<number>(?:\d+\.\d+(?:[eE]-?\d+)?[fFdD]?)|(?:\d+[fF]))(?![\w.])")
METHOD = re.compile(r"^\t+(?:(?:private|public|protected|static|final|synchronized)\s+)*[\w<>\[\], .?]+\s+(\w+)\(")
MEMBER = re.compile(r"^\t+(?:(?:private|public|protected)\s+)?static final [\w.<>\[\], ?]+ \w+ =")
ARRAY = re.compile(r"^\t+(?:(?:private|public|protected)\s+)?static final [\w.]+\[\] (\w+) =")


def code_mask(line, in_comment):
    """The line with strings, characters and comments blanked out, so positions still match."""
    out = list(line)
    i = 0
    while i < len(line):
        if in_comment:
            end = line.find("*/", i)
            stop = len(line) if end < 0 else end + 2
            out[i:stop] = " " * (stop - i)
            i, in_comment = stop, end < 0
            continue
        if line.startswith("//", i):
            out[i:] = " " * (len(line) - i)
            break
        if line.startswith("/*", i):
            in_comment = True
            continue
        if line[i] in "\"'":
            j = i + 1
            while j < len(line) and line[j] != line[i]:
                j += 2 if line[j] == "\\" else 1
            out[i:j + 1] = " " * (j + 1 - i)
            i = j + 1
            continue
        i += 1
    return "".join(out), in_comment


def array_index(statement, offset):
    """Which element of a flat array initializer starts at this offset, or None."""
    flat = re.search(r"=\s*(?:new [\w.]+\[\]\s*)?\{", statement)
    start = flat.end() - 1 if flat else -1
    if start < 0 or offset < start:
        return None
    depth, index, begin = 0, 0, start + 1
    for i in range(start + 1, len(statement)):
        c = statement[i]
        if c in "({[":
            depth += 1
        elif c in ")}]" and depth:
            depth -= 1
        elif c in ",}" and not depth:
            if begin <= offset < i:
                return index if statement[begin:offset].strip() == "" else None
            index, begin = index + 1, i + 1
    return None


def extract(stem, text, excluded, tunable_declaration, color_value):
    """Rewrites one file's source.

    Returns the new source, the values found, and for each array element the statement that stores it again after a
    change. Values built once when a class loads are left alone, except arrays and the values the caller recomputes.
    """
    lines = text.split("\n")
    holder = "LabLit_" + stem
    found, stores = [], []
    in_comment, method = False, ""
    once, array, constants = False, None, False
    for number, line in enumerate(lines):
        mask, in_comment = code_mask(line, in_comment)
        stripped = mask.strip()
        if re.search(r"\benum\s+\w+", mask):
            constants = True
        named = METHOD.match(mask)
        if named and "=" not in mask.split("(")[0]:
            method = named.group(1)
        opening = ARRAY.match(mask)
        if opening:
            array = {"name": opening.group(1), "text": ""}
        elif MEMBER.match(mask) and not tunable_declaration(line):
            once = True
        offset = 0
        if array is not None:
            offset = len(array["text"])
            array["text"] += mask + " "
        written_out = tunable_declaration(line) == "plain"
        skip = once or constants or written_out or stripped.startswith(("@", "case "))
        changes = []
        for match in [] if skip else LITERAL.finditer(mask):
            written = line[match.start():match.end()]
            if match.group("color"):
                kind, value = "Color", color_value(written)
            elif match.group("px"):
                kind, value = "int", int(written)
                if value in (0, 1):
                    continue
            else:
                kind = "float" if written[-1] in "fF" else "double"
                value = float(written.rstrip("fFdD"))
                if value in SKIPPED_NUMBERS:
                    continue
                around = mask[:match.start()].rstrip()[-1:] + mask[match.end():].lstrip()[:1]
                if value in (0.5, 2.0) and ("/" in around or "*" in around):
                    continue
            if value is None:
                continue
            index = None
            if array is not None:
                index = array_index(array["text"], offset + match.start())
                if index is None:
                    continue
            key = "v%d_%d" % (number + 1, match.start())
            ident = "%s:@%d.%d" % (stem, number + 1, match.start())
            if ident in excluded:
                continue
            if index is not None:
                stores.append("%s[%d] = %s.%s;" % (array["name"], index, holder, key))
            near = line[max(0, match.start() - 28):match.end() + 8].strip()
            where = array["name"] if array is not None else method
            found.append({"key": key, "type": kind, "value": value, "line": number + 1, "file": stem,
                          "name": "%s L%d: %s" % (where, number + 1, near), "doc": line.strip(),
                          "holder": holder, "id": ident})
            changes.append((match.start(), match.end(), "%s.%s" % (holder, key)))
        for start, end, new in reversed(changes):
            line = line[:start] + new + line[end:]
        lines[number] = line
        if stripped.endswith(";"):
            once, array, constants = False, None, False
    return "\n".join(lines), found, stores


def holder_class(holder, entries):
    """The generated class that holds one file's values."""
    java = {"Color": "java.awt.Color", "int": "int", "float": "float", "double": "double"}
    out = ["package com.oveduumnakal.gielinorgo;", "", "/** Generated by ui_lab.py. */", "final class " + holder, "{"]
    for e in entries:
        if e["type"] == "Color":
            first = "new java.awt.Color(0x%08X, true)" % e["value"]
        elif e["type"] == "float":
            first = "%rf" % float(e["value"])
        else:
            first = repr(e["value"])
        out.append("\tstatic %s %s = %s;" % (java[e["type"]], e["key"], first))
    for method, argument, colors in (("labSet", "double value", False), ("labColor", "int argb", True)):
        out += ["", "\tstatic boolean %s(String name, %s)" % (method, argument), "\t{", "\t\tswitch (name)", "\t\t{"]
        for e in entries:
            if (e["type"] == "Color") != colors:
                continue
            if colors:
                new = "new java.awt.Color(argb, true)"
            elif e["type"] == "int":
                new = "(int) Math.round(value)"
            else:
                new = "(%s) value" % e["type"]
            out.append('\t\t\tcase "%s": %s = %s; return true;' % (e["key"], e["key"], new))
        out += ["\t\t\tdefault: return false;", "\t\t}", "\t}"]
    out += ["}", ""]
    return "\n".join(out)
