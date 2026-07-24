#!/usr/bin/env python3
"""
Generator for Fixtures & Footnotes slat variants.

For each wood it emits three material variants (log/stem/block, stripped, planks), each with a
wall-slats block + a standing-screen block sharing one item. Run from the repo root:

    python3 tools/gen_slats.py

It writes blockstates, models, item models, loot tables and stonecutter recipes into
src/main/resources, rebuilds the mineable/axe tag + en_us slat entries, and regenerates
src/main/java/com/beepsterr/fixfoot/content/SlatVariants.java (the key list the mod loops over).
Adding a wood = one entry in WOODS below.
"""
import json
import os
import shutil

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src/main/resources")
ASSETS = os.path.join(RES, "assets/fixfoot")
DATA = os.path.join(RES, "data/fixfoot")
MODID = "fixfoot"

# wood -> the noun used for its "log" material (log / stem / block)
WOODS = {
    "oak": "log", "spruce": "log", "birch": "log", "jungle": "log",
    "acacia": "log", "dark_oak": "log", "mangrove": "log", "cherry": "log",
    "crimson": "stem", "warped": "stem",
    "bamboo": "block",
}


def title(s):
    return " ".join(w.capitalize() for w in s.split("_"))


def wood_materials(wood, noun):
    """The three (key, base_item, texture, material_display) tuples for a wood: log / stripped / planks."""
    return [
        (f"{wood}_{noun}", f"minecraft:{wood}_{noun}",
         f"minecraft:block/{wood}_{noun}", f"{title(wood)} {title(noun)}"),
        (f"stripped_{wood}_{noun}", f"minecraft:stripped_{wood}_{noun}",
         f"minecraft:block/stripped_{wood}_{noun}", f"Stripped {title(wood)} {title(noun)}"),
        (f"{wood}_planks", f"minecraft:{wood}_planks",
         f"minecraft:block/{wood}_planks", f"{title(wood)} Planks"),
    ]


def variants():
    """(key, base_item, texture, material_display) for every variant."""
    out = []
    for wood, noun in WOODS.items():
        out.extend(wood_materials(wood, noun))
    return out


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def bars(z0, z1):
    """The 4 slat bars (x-positions from the authored model), spanning z0..z1, texture #0."""
    xs = [(1, 3), (5, 7), (9, 11), (13, 15)]
    els = []
    for (x0, x1) in xs:
        els.append({
            "from": [x0, 0, z0], "to": [x1, 16, z1],
            "faces": {
                "north": {"uv": [x0, 0, x1, 16], "texture": "#0"},
                "south": {"uv": [x0, 0, x1, 16], "texture": "#0"},
                "east": {"uv": [0, 0, 2, 16], "texture": "#0"},
                "west": {"uv": [0, 0, 2, 16], "texture": "#0"},
                "up": {"uv": [x0, 0, x1, 2], "texture": "#0"},
                "down": {"uv": [x0, 0, x1, 2], "texture": "#0"},
            },
        })
    return {"textures": {"particle": "#0"}, "elements": els}


def wall_blockstate(key):
    m = f"{MODID}:block/{key}_slats"
    vine = f"{MODID}:block/slats_vine_overlay"
    mp = []
    for face, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        a = {"model": m} if y == 0 else {"model": m, "y": y}
        mp.append({"when": {face: "true"}, "apply": a})
    for face, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        a = {"model": vine} if y == 0 else {"model": vine, "y": y}
        mp.append({"when": {face: "true", "vined": "true"}, "apply": a})
    return {"multipart": mp}


def screen_blockstate(key):
    m = f"{MODID}:block/{key}_slat_screen"
    return {"variants": {"axis=x": {"model": m}, "axis=z": {"model": m, "y": 90}}}


def block_loot(item):
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": item}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    }


def stonecut_recipe(base, result_item):
    return {
        "type": "minecraft:stonecutting",
        "ingredient": {"item": base},
        "result": {"count": 8, "id": result_item},
    }


def main():
    vs = variants()

    # shared base models (geometry once, texture swapped per variant via #0)
    write(f"{ASSETS}/models/block/slats_base.json", bars(0, 2))
    write(f"{ASSETS}/models/block/slat_screen_base.json", bars(7, 9))

    lang = {
        "block.fixfoot.display_fixture": "Display Fixture",
        "itemGroup.fixfoot.main": "Fixtures & Footnotes",
    }
    axe = []

    for key, base, tex, disp in vs:
        item = f"{MODID}:{key}_slats"
        # models (thin — parent the shared base, set the texture)
        write(f"{ASSETS}/models/block/{key}_slats.json",
              {"parent": f"{MODID}:block/slats_base", "textures": {"0": tex}})
        write(f"{ASSETS}/models/block/{key}_slat_screen.json",
              {"parent": f"{MODID}:block/slat_screen_base", "textures": {"0": tex}})
        write(f"{ASSETS}/models/item/{key}_slats.json", {"parent": f"{MODID}:block/{key}_slats"})
        # blockstates
        write(f"{ASSETS}/blockstates/{key}_slats.json", wall_blockstate(key))
        write(f"{ASSETS}/blockstates/{key}_slat_screen.json", screen_blockstate(key))
        # loot (wall drops handled in code, but keep a table; screen uses this table)
        write(f"{DATA}/loot_table/blocks/{key}_slats.json", block_loot(item))
        write(f"{DATA}/loot_table/blocks/{key}_slat_screen.json", block_loot(item))
        # lang + tag
        lang[f"block.fixfoot.{key}_slats"] = f"{disp} Slats"
        lang[f"block.fixfoot.{key}_slat_screen"] = f"{disp} Slat Screen"
        axe.append(f"{MODID}:{key}_slats")
        axe.append(f"{MODID}:{key}_slat_screen")

    # Stonecutter recipes: the log/stem/block base cuts into ALL three of its slat variants; the
    # stripped + planks materials each also cut into their own.
    shutil.rmtree(f"{DATA}/recipe", ignore_errors=True)
    for wood, noun in WOODS.items():
        mats = wood_materials(wood, noun)  # [log, stripped, planks]
        log_key, log_base = mats[0][0], mats[0][1]
        for (key, base, tex, disp) in mats:
            write(f"{DATA}/recipe/{key}_slats_from_{log_key}_stonecutting.json",
                  stonecut_recipe(log_base, f"{MODID}:{key}_slats"))
        for (key, base, tex, disp) in mats[1:]:
            write(f"{DATA}/recipe/{key}_slats_from_{key}_stonecutting.json",
                  stonecut_recipe(base, f"{MODID}:{key}_slats"))

    # merge into en_us so hand-added (non-slat) entries survive a regen
    lang_path = f"{ASSETS}/lang/en_us.json"
    if os.path.exists(lang_path):
        merged = json.load(open(lang_path))
        merged.update(lang)
        lang = merged
    write(lang_path, lang)
    write(f"{RES}/data/minecraft/tags/block/mineable/axe.json", {"replace": False, "values": axe})

    # SlatVariants.java — the key list the registration loops over
    keys = ",\n".join(f'            "{k}"' for k, *_ in vs)
    java = (
        "package com.beepsterr.fixfoot.content;\n\n"
        "import java.util.List;\n\n"
        "/** Generated by tools/gen_slats.py — the slat material variant keys. Do not edit by hand. */\n"
        "public final class SlatVariants {\n"
        "    private SlatVariants() {\n    }\n\n"
        "    public static final List<String> KEYS = List.of(\n"
        f"{keys}\n"
        "    );\n"
        "}\n"
    )
    jpath = os.path.join(ROOT, "src/main/java/com/beepsterr/fixfoot/content/SlatVariants.java")
    os.makedirs(os.path.dirname(jpath), exist_ok=True)
    with open(jpath, "w") as f:
        f.write(java)

    print(f"generated {len(vs)} variants ({len(WOODS)} woods x 3 materials)")


if __name__ == "__main__":
    main()
