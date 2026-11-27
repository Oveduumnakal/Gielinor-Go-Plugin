"""Stand-in portraits for the lab: each creature's picture from the OSRS Wiki.

The plugin draws portraits from the game's models, which only a running client has. These pictures are downloaded once
into .portraits/, which is never committed. Only creature names are sent. To use a picture of your own, save it there as
the creature's name, lower case with _ for spaces (lesser_demon.png).
"""
import json
import pathlib
import re
import urllib.parse
import urllib.request

API = "https://oldschool.runescape.wiki/api.php"
AGENT = {"User-Agent": "ui-lab portrait fetch"}
EXTRA = ["Goblin", "Cow", "Imp", "Bruise Blamish Snail", "Man", "Rat", "Chicken", "Lesser demon"]


def slug(name):
    return re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_")


def wanted(lab):
    """Every creature the lab's sample data mentions."""
    data = (lab / "java" / "LabData.java").read_text(encoding="utf-8")
    block = re.search(r"NAMES = \{(.*?)\};", data, re.S)
    names = re.findall(r'"([^"]+)"', block.group(1)) if block else []
    return sorted(set(names + EXTRA))


def read(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=AGENT), timeout=20) as reply:
        return reply.read()


def fetch(lab):
    folder = lab / ".portraits"
    folder.mkdir(exist_ok=True)
    missing = [n for n in wanted(lab) if not (folder / (slug(n) + ".png")).exists()]
    if not missing:
        return
    print("UI Lab: downloading %d creature pictures from the OSRS Wiki (once)..." % len(missing))
    try:
        query = urllib.parse.urlencode({"action": "query", "format": "json", "prop": "pageimages",
                                        "piprop": "thumbnail", "pithumbsize": 384, "pilimit": 50, "redirects": 1,
                                        "titles": "|".join(missing[:50])})
        answer = json.loads(read(API + "?" + query))["query"]
        renamed = {r["from"]: r["to"] for key in ("normalized", "redirects") for r in answer.get(key, [])}
        pictures = {p["title"]: p["thumbnail"]["source"] for p in answer["pages"].values() if "thumbnail" in p}
        got = 0
        for name in missing:
            title = name
            for _ in range(3):
                title = renamed.get(title, title)
            if title in pictures:
                (folder / (slug(name) + ".png")).write_bytes(read(pictures[title]))
                got += 1
        print("UI Lab: got %d of %d pictures" % (got, len(missing)))
    except (OSError, ValueError, KeyError) as error:
        print("UI Lab: no pictures this time (%s); blue squares stand in" % error)
