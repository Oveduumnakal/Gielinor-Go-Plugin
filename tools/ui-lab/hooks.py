"""Where the lab's copy of the painters calls LabDepth. Each anchor must match exactly once, or the build stops."""


def once(text, anchor, name):
    if text.count(anchor) != 1:
        raise SystemExit("%s has changed (%r matched %d times); update hooks.py" % (name, anchor, text.count(anchor)))
    return text.index(anchor)


def before(text, anchor, new, name):
    at = once(text, anchor, name)
    return text[:at] + new + text[at:]


def after(text, anchor, new, name):
    at = once(text, anchor, name) + len(anchor)
    return text[:at] + new + text[at:]


def wrap(text, signature, inner, element, shape, name, condition=None):
    """Renames a method and puts one in its place that draws depth under and over it."""
    at = once(text, signature, name)
    returns = signature.split("(")[0].split()[-2]
    method = signature.split("(")[0].split()[-1]
    arguments = [p.split()[-1] for p in signature[signature.index("(") + 1:signature.rindex(")")].split(",")]
    call = "%s(%s)" % (inner, ", ".join(arguments))
    guard = "if (%s)\n\t\t\t" % condition if condition else ""
    body = [signature, "\t{", "\t\tShape labShape = %s;" % shape,
            '\t\t%sLabDepth.under(g, "%s", labShape, this);' % (guard, element)]
    body.append("\t\t%s%s;" % ("" if returns == "void" else "%s labResult = " % returns, call))
    body.append('\t\t%sLabDepth.over(g, "%s", labShape, this);' % (guard, element))
    if returns != "void":
        body.append("\t\treturn labResult;")
    body += ["\t}", "", "\t"]
    renamed = signature.replace(" %s(" % method, " %s(" % inner)
    return text[:at] + "\n".join(body).lstrip("\n") + renamed + text[at + len(signature):]


def window_style(text):
    name = "WindowStyle"
    text = wrap(text, "void popup(Graphics2D g, Rectangle2D box, String surface, ThemeColor fill, ThemeColor border, "
                      "boolean shadow,\n\t\tList<Readability.Need> needs)", "labPopup", "Panels",
                "outline(Kind.BODY, box)", name)
    text = wrap(text, "void panel(Graphics2D g, Rectangle2D box)", "labPanel", "Panels",
                "outline(Kind.PANEL, box)", name)
    text = wrap(text, "void card(Graphics2D g, Shape shape)", "labCard", "Panels", "shape", name)
    text = wrap(text, "Shape row(Graphics2D g, Rectangle2D box, boolean lead, double hover, double press)", "labRow",
                "Rows", "outline(Kind.ROW, box)", name)
    text = wrap(text, "private void chipBody(Graphics2D g, Shape shape, boolean accent)", "labChip", "Chips", "shape",
                name)
    text = wrap(text, "void chipBody(Graphics2D g, Shape shape, Color[] outline)", "labOutlinedChip", "Chips", "shape",
                name, "theme.glass()")
    under = '\n\t\tLabDepth.under(g, "Buttons and tabs", shape, this);'
    over = 'LabDepth.over(g, "Buttons and tabs", shape, this);\n\t\t'
    text = after(text, "Shape shape = outline(Kind.TAB, box);", under, name)
    text = before(text, "Font font = fitted(g, WindowFonts.Face.BOLD", over, name)
    text = after(text, "Shape shape = outline(Kind.BUTTON, box);", under, name)
    return before(text, "content.paint(g, text);", over, name)


def follower_bubbles(text):
    name = "FollowerBubblePainter"
    text = before(text, "if (style.theme().glass())\n\t\t{\n\t\t\tGlassStyle.disc(",
                  'LabDepth.under(g, "Bubbles", circle(cx, cy, r), style);\n\t\t', name)
    return before(text, "face(g, dot, cx, cy, r);\n", 'LabDepth.over(g, "Bubbles", circle(cx, cy, r), style);\n\t\t',
                  name)


def wild_bubbles(text):
    name = "BubblePainter"
    text = before(text, "if (glass)\n\t\t{\n\t\t\tGlassStyle.disc(",
                  'LabDepth.under(g, "Bubbles", circle(cx, cy, r), style);\n\t\t', name)
    return before(text, "if (jar)\n\t\t\tjarFace(", 'LabDepth.over(g, "Bubbles", circle(cx, cy, r), style);\n\t\t', name)


HOOKS = {"WindowStyle": window_style, "FollowerBubblePainter": follower_bubbles, "BubblePainter": wild_bubbles}


def apply(source):
    for stem, hook in HOOKS.items():
        path = source / (stem + ".java")
        path.write_text(hook(path.read_text(encoding="utf-8")), encoding="utf-8", newline="\n")
