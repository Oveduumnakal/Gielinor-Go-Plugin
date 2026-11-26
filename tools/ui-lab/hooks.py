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
    text = before(text, "content.paint(g, text);", over, name)
    text = after(text, "Rectangle2D box = new Rectangle2D.Double(x, y, w, metrics.chipHeight);",
                 '\n\t\tLabDepth.under(g, "Chips", outline(Kind.CHIP, box), this);', name)
    return before(text, "Font font = font(WindowFonts.Face.SMALL, WindowFonts.SMALL_UNITS);\n\t\ttext(g, label,",
                  'LabDepth.over(g, "Chips", outline(Kind.CHIP, box), this);\n\t\t', name)


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


def named(text, call, argument, pane, name):
    """Gives one argument of a drawing call a pane name, so what it draws takes that pane's height."""
    once(text, call, name)
    if call.count(argument) != 1:
        raise SystemExit("hooks.py: %r is not once in %r" % (argument, call))
    return text.replace(call, call.replace(argument, "LabDepth.pane(%s, %s)" % (pane, argument)))


def summary(text):
    name = "SummaryPainter"
    text = named(text, "st.card(g, pill);\n\t\tShape badge", "pill", '"Summary: name bar"', name)
    text = named(text, "st.card(g, pill);\n\t\tint hearts", "pill", '"Summary: hearts"', name)
    text = named(text, "st.panel(g, new Rectangle2D.Double(box.x, box.y, box.width, box.height));\n\t\tFont bold",
                 "new Rectangle2D.Double(box.x, box.y, box.width, box.height)", '"Summary: " + heading', name)
    text = named(text, "st.chip(g, x, box.y, chip.label(), chip.style());", "g", '"Summary: type badges"', name)
    text = named(text, "st.chipBody(g, shape);\n", "shape", '"Summary: move pills"', name)
    text = named(text, "st.chipBody(g, shape, moveOutline(", "shape", '"Summary: move pills"', name)
    text = named(text, "st.button(g, shape, labels[i],", "g", '"Summary: buttons"', name)
    portrait = "st.panelShape(box)"
    text = before(text, "if (scene == null || !scene.paint(",
                  'LabDepth.under(g, "Panels", LabDepth.pane("Summary: portrait", %s), st);\n\t\t\t' % portrait, name)
    return before(text, "header(g, ctx, layout, content);\n\t\tchips(",
                  'if (target.width > 0 && target.height > 0)\n\t\t\tLabDepth.over(g, "Panels", st.panelShape(target), st);'
                  '\n\n\t\t', name)


def bag(text):
    pane = ('box == layout.trainer ? "Bag: trainer" : box == layout.jars ? "Bag: jars" : '
            'box == layout.items ? "Bag: healing items" : "Bag: eggs"')
    return named(text, "st.panel(g, rect(box));", "rect(box)", pane, "BagPainter")


def party(text):
    name = "PartyPainter"
    for box, pane in (("eggs", "Party: eggs"), ("box", "Party: tasks")):
        shape = "new Rectangle2D.Double(%s.x, %s.y, %s.width, %s.height)" % (box, box, box, box)
        text = named(text, "st.panel(g, %s);" % shape, shape, '"%s"' % pane, name)
    return text


def collection(text):
    name = "CollectionPage"
    for box, pane in (("left", "summary"), ("right", "list"), ("detail", "details")):
        text = named(text, "st.panel(g, rect(%s));" % box, "rect(%s)" % box, '"Collection: %s"' % pane, name)
    return text


def tasks(text):
    name = "TasksPage"
    for box, pane in (("left", "tasks"), ("right", "milestones")):
        text = named(text, "st.panel(g, rect(%s));" % box, "rect(%s)" % box, '"Tasks: %s"' % pane, name)
    return text


def boxes(*calls):
    return "".join("LabBoxes.at(%s);\n\t\t" % call for call in calls)


def summary_layout(text):
    calls = boxes('"Summary: name bar", header', '"Summary: portrait", portrait, chips', '"Summary: type badges", chips',
                  '"Summary: Description", description', '"Summary: Stats", stats', '"Summary: Nature", nature',
                  '"Summary: hearts", bond', '"Summary: Moves", moves')
    anchor = "body.y + body.height - buttonH + row * (buttonH + gap), each, buttonH);\n\t\t}\n"
    each = ('for (int i = 0; i < n; i++)\n\t\t\tLabBoxes.at("Summary: " + labels[i] + " button", buttons[i]);\n')
    return after(text, anchor, "\n\t\t%s%s" % (calls, each), "SummaryLayout")


def party_layout(text):
    calls = boxes('"Party: eggs", eggs', '"Party: tasks", tasks', '"Party: Pin Party button", pinButton',
                  '"Party: More button", moreButton')
    each = 'for (int i = 0; i < SLOTS; i++)\n\t\t\tLabBoxes.at("Party: row " + (i + 1), rows[i]);\n'
    anchor = "tasks = new Rectangle(side.x, side.y + eggsH + gap, side.width, Math.max(0, free - eggsH - gap));\n"
    return after(text, anchor, "\n\t\t%s%s" % (calls, each), "PartyLayout")


def bag_layout(text):
    calls = boxes('"Bag: trainer", trainer, titleLine, winsLine, trainerBar, stepsLine, stepsBar',
                  '"Bag: jars", jars, jarsHeading, jarCells', '"Bag: healing items", items, itemsHeading, itemCells',
                  '"Bag: eggs", eggs, eggsHeading, eggRows, eggsMore')
    at = once(text, "eggsMore = rows < eggCount && capacity > 0", "BagLayout")
    end = text.index(": new Rectangle();\n", at) + len(": new Rectangle();\n")
    return text[:end] + "\n\t\t%s\n" % calls.rstrip() + text[end:]


def collection_boxes(text):
    calls = boxes('"Collection: summary", left, record, recordBar, reward, todayHeading, todayRows, event, night',
                  '"Collection: list background", right', '"Collection: filter button", filterButton, count',
                  '"Collection: search box", searchBox', '"Collection: details", detail',
                  '"Collection: species list", labList')
    anchor = "list.layout(new Rectangle(right.x + pad / 2, top, right.width - pad, Math.max(0, listBottom - top)), rowH,"
    new = ("Rectangle labList = new Rectangle(right.x + pad / 2, top, right.width - pad, "
           "Math.max(0, listBottom - top));\n\t\t%slist.layout(labList, rowH," % calls)
    once(text, anchor, "CollectionPage")
    return text.replace(anchor, new)


def tasks_boxes(text):
    calls = boxes('"Tasks: tasks", left, headings, taskRows, divider, streak',
                  '"Tasks: milestones", right, milestoneHeading, nextLine, nextBar',
                  '"Tasks: milestone list", viewport')
    return before(text, "list.layout(viewport, style.px(MILESTONE_UNITS)", calls, "TasksPage")


def chain(*hooks):
    def run(text):
        for hook in hooks:
            text = hook(text)
        return text
    return run


HOOKS = {"WindowStyle": window_style, "FollowerBubblePainter": follower_bubbles, "BubblePainter": wild_bubbles,
         "SummaryPainter": summary, "BagPainter": bag, "PartyPainter": party,
         "CollectionPage": chain(collection, collection_boxes), "TasksPage": chain(tasks, tasks_boxes),
         "SummaryLayout": summary_layout, "PartyLayout": party_layout, "BagLayout": bag_layout}


def apply(source):
    for stem, hook in HOOKS.items():
        path = source / (stem + ".java")
        path.write_text(hook(path.read_text(encoding="utf-8")), encoding="utf-8", newline="\n")
