#!/usr/bin/env python3
"""
Generator for Fixtures & Footnotes slat variants.

For each wood it emits three material variants (log/stem/block, stripped, planks), each with a
wall-slats block + a standing-screen block sharing one item. Run from the repo root:

    python3 tools/gen_slats.py

It writes blockstates, models, item models, loot tables and stonecutter recipes into
src/main/resources, rebuilds the mineable/axe tag + en_us slat entries, and regenerates
src/main/java/com/beepsterr/fixfoot/content/SlatVariants.java (the key list the mod loops over).
Adding a wood = one entry in WOODS below; adding a non-wood material = one entry in EXTRAS.
"""
import json
import os
import shutil

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src/main/resources")
ASSETS = os.path.join(RES, "assets/fixfoot")
DATA = os.path.join(RES, "data/fixfoot")
MODID = "fixfoot"
# Generated slat advancements go here; hand-written ones live under recipes/decorations so the
# wipe below can't eat them.
SLAT_ADVANCEMENTS = "building_blocks"

# wood -> the noun used for its "log" material (log / stem / block)
WOODS = {
    "oak": "log", "spruce": "log", "birch": "log", "jungle": "log",
    "acacia": "log", "dark_oak": "log", "mangrove": "log", "cherry": "log",
    "crimson": "stem", "warped": "stem",
    "bamboo": "block",
}

# Non-wood materials. Single variant each, no stripped/planks fan-out. These copy their vanilla
# block's properties in code (light level, sound, hardness), so glowstone slats glow etc.
# tool = which mineable/<tool> tag to list them in (None = no tool tag, like their vanilla block).
EXTRAS = [
    # key, base item, texture, display, Blocks.<field>, tool
    ("glowstone", "minecraft:glowstone", "minecraft:block/glowstone",
     "Glowstone", "GLOWSTONE", None),
    ("sea_lantern", "minecraft:sea_lantern", "minecraft:block/sea_lantern",
     "Sea Lantern", "SEA_LANTERN", None),
    ("ochre_froglight", "minecraft:ochre_froglight", "minecraft:block/ochre_froglight_side",
     "Ochre Froglight", "OCHRE_FROGLIGHT", None),
    ("verdant_froglight", "minecraft:verdant_froglight", "minecraft:block/verdant_froglight_side",
     "Verdant Froglight", "VERDANT_FROGLIGHT", None),
    ("pearlescent_froglight", "minecraft:pearlescent_froglight",
     "minecraft:block/pearlescent_froglight_side",
     "Pearlescent Froglight", "PEARLESCENT_FROGLIGHT", None),
    ("magma", "minecraft:magma_block", "minecraft:block/magma",
     "Magma", "MAGMA_BLOCK", "pickaxe"),
]


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
    for key, base, tex, disp, _field, _tool in EXTRAS:
        out.append((key, base, tex, disp))
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


FACES = (("north", 0), ("east", 90), ("south", 180), ("west", 270))

# SlatPanelBlock.Overlay value -> the model drawn a pixel in front of the bars. One enum property
# rather than a boolean each, so the overlays stay mutually exclusive without inflating the states.
OVERLAYS = {
    "vine": f"{MODID}:block/slats_vine_overlay",
    "pink_petals": f"{MODID}:block/slats_petals_overlay",
}


def wall_blockstate(key):
    def apply(model, y):
        return {"model": model} if y == 0 else {"model": model, "y": y}

    m = f"{MODID}:block/{key}_slats"
    mp = [{"when": {face: "true"}, "apply": apply(m, y)} for face, y in FACES]
    for overlay, model in OVERLAYS.items():
        for face, y in FACES:
            mp.append({"when": {face: "true", "overlay": overlay}, "apply": apply(model, y)})
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


def merge_tag(path, values):
    """Write a mineable tag, keeping any hand-added non-slat entries already in the file."""
    kept = []
    if os.path.exists(path):
        for v in json.load(open(path)).get("values", []):
            if not (isinstance(v, str) and v.startswith(f"{MODID}:")
                    and (v.endswith("_slats") or v.endswith("_slat_screen"))):
                kept.append(v)
    write(path, {"replace": False, "values": kept + values})


def stonecut_recipe(base, result_item):
    return {
        "type": "minecraft:stonecutting",
        "ingredient": {"item": base},
        "result": {"count": 8, "id": result_item},
    }


def unlock_advancement(recipe_id, ingredient):
    """
    Puts a recipe in the recipe book. Without one a recipe is craftable but never shown, so a
    player with no JEI has no way to discover it. Vanilla's shape: fires either when you first
    pick up the ingredient or if you were granted the recipe some other way. Parented to the
    hidden minecraft:recipes/root, so it never appears on the advancements screen.
    """
    return {
        "parent": "minecraft:recipes/root",
        "criteria": {
            "has_ingredient": {
                "trigger": "minecraft:inventory_changed",
                "conditions": {"items": [{"items": ingredient}]},
            },
            "has_the_recipe": {
                "trigger": "minecraft:recipe_unlocked",
                "conditions": {"recipe": recipe_id},
            },
        },
        "requirements": [["has_the_recipe", "has_ingredient"]],
        "rewards": {"recipes": [recipe_id]},
    }


def cut(name, base, result_item):
    """One stonecutter recipe plus the advancement that makes it discoverable."""
    write(f"{DATA}/recipe/{name}.json", stonecut_recipe(base, result_item))
    write(f"{DATA}/advancement/recipes/{SLAT_ADVANCEMENTS}/{name}.json",
          unlock_advancement(f"{MODID}:{name}", base))


def main():
    vs = variants()

    # shared base models (geometry once, texture swapped per variant via #0)
    write(f"{ASSETS}/models/block/slats_base.json", bars(0, 2))
    write(f"{ASSETS}/models/block/slat_screen_base.json", bars(7, 9))

    lang = {
        "block.fixfoot.display_fixture": "Display Fixture",
        "itemGroup.fixfoot.main": "Fixtures & Footnotes",
    }
    tools = {"axe": []}  # tool tag name -> block ids; woods are axe, extras pick their own
    extra_tool = {key: tool for key, _b, _t, _d, _f, tool in EXTRAS}

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
        tool = extra_tool.get(key, "axe")
        if tool is not None:
            tools.setdefault(tool, []).append(f"{MODID}:{key}_slats")
            tools[tool].append(f"{MODID}:{key}_slat_screen")

    # Stonecutter recipes: the log/stem/block base cuts into ALL three of its slat variants; the
    # stripped + planks materials each also cut into their own.
    # Only the stonecutting recipes are generated, so the hand-written crafting ones are moved
    # aside and restored rather than wiped.
    handwritten = {f: open(f"{DATA}/recipe/{f}").read()
                   for f in os.listdir(f"{DATA}/recipe") if "stonecutting" not in f} \
        if os.path.isdir(f"{DATA}/recipe") else {}
    shutil.rmtree(f"{DATA}/recipe", ignore_errors=True)
    shutil.rmtree(f"{DATA}/advancement/recipes/{SLAT_ADVANCEMENTS}", ignore_errors=True)
    os.makedirs(f"{DATA}/recipe", exist_ok=True)
    for name, body in handwritten.items():
        open(f"{DATA}/recipe/{name}", "w").write(body)

    for wood, noun in WOODS.items():
        mats = wood_materials(wood, noun)  # [log, stripped, planks]
        log_key, log_base = mats[0][0], mats[0][1]
        for (key, base, tex, disp) in mats:
            cut(f"{key}_slats_from_{log_key}_stonecutting", log_base, f"{MODID}:{key}_slats")
        for (key, base, tex, disp) in mats[1:]:
            cut(f"{key}_slats_from_{key}_stonecutting", base, f"{MODID}:{key}_slats")
    for key, base, _tex, _disp, _field, _tool in EXTRAS:
        cut(f"{key}_slats_from_{key}_stonecutting", base, f"{MODID}:{key}_slats")

    # merge into en_us so hand-added (non-slat) entries survive a regen
    lang_path = f"{ASSETS}/lang/en_us.json"
    if os.path.exists(lang_path):
        merged = json.load(open(lang_path))
        merged.update(lang)
        lang = merged
    write(lang_path, lang)
    for tool, values in tools.items():
        merge_tag(f"{RES}/data/minecraft/tags/block/mineable/{tool}.json", values)

    # SlatVariants.java — the key list the registration loops over
    keys = ",\n".join(f'            "{k}"' for k, *_ in vs)
    bases = ",\n".join(
        f'            Map.entry("{key}", (Supplier<Block>) () -> Blocks.{field})'
        for key, _b, _t, _d, field, _tool in EXTRAS)
    java = (
        "package com.beepsterr.fixfoot.content;\n\n"
        "import java.util.List;\n"
        "import java.util.Map;\n"
        "import java.util.function.Supplier;\n"
        "import net.minecraft.world.level.block.Block;\n"
        "import net.minecraft.world.level.block.Blocks;\n\n"
        "/** Generated by tools/gen_slats.py — the slat material variant keys. Do not edit by hand. */\n"
        "public final class SlatVariants {\n"
        "    private SlatVariants() {\n    }\n\n"
        "    public static final List<String> KEYS = List.of(\n"
        f"{keys}\n"
        "    );\n\n"
        "    /**\n"
        "     * Non-wood variants and the vanilla block they copy their properties from, so e.g.\n"
        "     * glowstone slats glow and magma slats need a pickaxe. Keys absent here are woods.\n"
        "     */\n"
        "    public static final Map<String, Supplier<Block>> SPECIAL_BASE = Map.ofEntries(\n"
        f"{bases}\n"
        "    );\n"
        "}\n"
    )
    jpath = os.path.join(ROOT, "src/main/java/com/beepsterr/fixfoot/content/SlatVariants.java")
    os.makedirs(os.path.dirname(jpath), exist_ok=True)
    with open(jpath, "w") as f:
        f.write(java)

    print(f"generated {len(vs)} variants ({len(WOODS)} woods x 3 materials + {len(EXTRAS)} extras)")


if __name__ == "__main__":
    main()
