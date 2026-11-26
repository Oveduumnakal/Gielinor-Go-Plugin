# UI Lab

A page in your browser that draws the plugin's interface with the plugin's own painters, and lets you change its
numbers and colours while you look.

## Run it

From the repo root:

```
python tools/ui-lab/ui_lab.py
```

It builds (about half a minute), then opens `http://127.0.0.1:8917/`. Stop it with Ctrl+C. Run it again after you
change the plugin's code. It needs Python and a JDK 11 or newer (`JAVA_HOME` is used when set).

## Use it

- **Scene** picks what to look at; **Theme** and **Scale** how it is drawn. **Zoom** only enlarges the picture.
- The left side lists the controls by source file, plus the theme's colours. Search narrows them: try `glass`,
  `radius`, `open` or `hp`. Hover a name for its description and where it lives.
- **Pause**, **Speed** and the slider under them control time. **Restart** starts the scene over.
- The picture takes hover and clicks: tabs, rows and bubbles answer.
- **Hold: original** shows the scene as the code has it, for comparison.
- **Export preset** saves what you changed as `ui-preset.json`: each value with its file, line, old and new value.
  Hand that file over to have it applied to the code. **Load preset** brings it back later.

## What it does to the code

Nothing under `src/` is written to. The sources are copied to `build/ui-lab`, the UI classes' named values stop being
`final` in the copy, and that copy is compiled with the server in `java/`. The folder is not part of the plugin build.

The server listens on this computer only and refuses requests from other sites.

## What it cannot show

- Creature portraits and in-world animation need the game, so a blue square stands in for a portrait.
- Values written directly inside drawing code have no control until they are given a name.
- It is a close view, not the game: check what you keep in the client.
