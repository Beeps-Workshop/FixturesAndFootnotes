#!/usr/bin/env python3
"""
Generator for the copper basin's oxidation fan-out.

Four weathering stages, each with a waxed twin that reuses the unwaxed models (vanilla does the
same for waxed copper), and four fill levels each. Run from the repo root:

    python3 tools/gen_basin.py

Writes blockstates, block + item models, loot tables, recipes with their unlock advancements, and
the NeoForge oxidation/waxing data maps; merges the en_us and mineable tag entries so hand-added
lines survive.

The bowl geometry is not written here — it is read from the authored Blockbench export, so
reshaping the basin means re-exporting and re-running this. Only the water surface is synthesised,
from the constants below; CopperBasinBlock repeats them for its collision shape.
"""
import json
import os

import authored

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src/main/resources")
ASSETS = os.path.join(RES, "assets/fixfoot")
DATA = os.path.join(RES, "data/fixfoot")
MODID = "fixfoot"
# Basin advancements live under decorations, next to the other hand-written ones — gen_slats.py
# wipes and rebuilds recipes/building_blocks, so nothing of ours can live there.
ADVANCEMENTS = "decorations"

# Water geometry, in pixels, read off the authored model: the bowl runs 2..14 across and sits on
# an inner floor topping out at y=4, with the rim at y=14. Three levels on vanilla's 3px steps,
# the full one a pixel below the rim. CopperBasinBlock repeats these for its shape — keep in step.
FLOOR = 4
WATER_PER_LEVEL = 3
MAX_LEVEL = 3

# Blockbench texture slot -> the name the per-stage models bind.
SLOTS = {"0": "body", "1": "feet"}

# prefix, display name, body texture, feet texture — freshest first, matching ModBlocks.BASIN_STAGES.
STAGES = [
    ("", "Copper", "minecraft:block/copper_block", "minecraft:block/copper_grate"),
    ("exposed_", "Exposed Copper", "minecraft:block/exposed_copper", "minecraft:block/exposed_copper_grate"),
    ("weathered_", "Weathered Copper", "minecraft:block/weathered_copper", "minecraft:block/weathered_copper_grate"),
    ("oxidized_", "Oxidized Copper", "minecraft:block/oxidized_copper", "minecraft:block/oxidized_copper_grate"),
]


def name(prefix, waxed):
    return ("waxed_" if waxed else "") + prefix + "copper_basin"


def display(disp, waxed):
    return ("Waxed " if waxed else "") + disp + " Basin"


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def base_model(level):
    """The authored bowl, plus the water surface for this level."""
    water = []
    if level > 0:
        water.append({
            "__comment": "Water surface. A flat quad like a cauldron's — no fluid is rendered; "
                         "tintindex 0 is the biome water colour, applied in FixfootClient.",
            "from": [2, FLOOR, 2],
            "to": [14, FLOOR + WATER_PER_LEVEL * level, 14],
            "faces": {"up": {"texture": "#content", "tintindex": 0}},
        })
    return authored.load("CopperBasin", SLOTS, water)


def suffix(level):
    return "" if level == 0 else f"_level{level}"


def source_block(prefix, waxed):
    """The vanilla copper block of this stage, which the basin is cut from."""
    base = "copper_block" if not prefix else prefix + "copper"
    return "minecraft:" + ("waxed_" if waxed else "") + base


def block_loot(item):
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": item}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    }


def merge_map(path, values):
    """Fold entries into a NeoForge data map, leaving the other copper families' alone."""
    existing = json.load(open(path))["values"] if os.path.exists(path) else {}
    existing.update(values)
    write(path, {"replace": False, "values": existing})


def merge(path, values, mine):
    """Rewrite a tag file, keeping every entry that isn't one of ours."""
    kept = []
    if os.path.exists(path):
        kept = [v for v in json.load(open(path)).get("values", []) if v not in mine]
    write(path, {"replace": False, "values": kept + values})


def unlock_advancement(recipe_id, ingredient):
    """Same shape gen_slats.py uses: puts the recipe in the book without showing an advancement."""
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


def recipe(name_, body, ingredient):
    write(f"{DATA}/recipe/{name_}.json", body)
    write(f"{DATA}/advancement/recipes/{ADVANCEMENTS}/{name_}.json",
          unlock_advancement(f"{MODID}:{name_}", ingredient))


def main():
    # Shared geometry, one model per fill level.
    for level in range(MAX_LEVEL + 1):
        write(f"{ASSETS}/models/block/basin_base{suffix(level)}.json", base_model(level))

    lang = {}
    ids = []
    for prefix, disp, body, feet in STAGES:
        stage_id = name(prefix, False)
        # Block models: only the unwaxed stage has them; the waxed twin points at these.
        for level in range(MAX_LEVEL + 1):
            textures = {"body": body, "feet": feet}
            if level > 0:
                textures["content"] = "minecraft:block/water_still"
            write(f"{ASSETS}/models/block/{stage_id}{suffix(level)}.json",
                  {"parent": f"{MODID}:block/basin_base{suffix(level)}", "textures": textures})

        for waxed in (False, True):
            block_id = name(prefix, waxed)
            ids.append(block_id)
            write(f"{ASSETS}/blockstates/{block_id}.json", {"variants": {
                f"level={level}": {"model": f"{MODID}:block/{stage_id}{suffix(level)}"}
                for level in range(MAX_LEVEL + 1)
            }})
            write(f"{ASSETS}/models/item/{block_id}.json", {"parent": f"{MODID}:block/{stage_id}"})
            write(f"{DATA}/loot_table/blocks/{block_id}.json", block_loot(f"{MODID}:{block_id}"))
            lang[f"block.fixfoot.{block_id}"] = display(disp, waxed)

    # Cut from copper like the drainage blocks, one basin to the block. The crafting recipe stays
    # alongside it: the grid is the cheap route at five ingots, the stonecutter the convenient one.
    for prefix, *_ in STAGES:
        for waxed in (False, True):
            block_id = name(prefix, waxed)
            base = source_block(prefix, waxed)
            recipe(f"{block_id}_from_{base.split(':')[1]}_stonecutting", {
                "type": "minecraft:stonecutting",
                "ingredient": {"item": base},
                "result": {"count": 1, "id": f"{MODID}:{block_id}"},
            }, base)

    # A basin is a cauldron two rows tall instead of three — five ingots rather than seven.
    recipe("copper_basin", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["C C", "CCC"],
        "key": {"C": {"item": "minecraft:copper_ingot"}},
        "result": {"count": 1, "id": f"{MODID}:copper_basin"},
    }, "minecraft:copper_ingot")

    # Waxing is normally a honeycomb interaction; vanilla also ships the crafting shortcut, so match it.
    for prefix, *_ in STAGES:
        unwaxed, waxed_id = name(prefix, False), name(prefix, True)
        recipe(f"{waxed_id}_from_honeycomb", {
            "type": "minecraft:crafting_shapeless",
            "category": "misc",
            "group": "waxed_copper_basin",
            "ingredients": [{"item": f"{MODID}:{unwaxed}"}, {"item": "minecraft:honeycomb"}],
            "result": {"count": 1, "id": f"{MODID}:{waxed_id}"},
        }, f"{MODID}:{unwaxed}")

    # NeoForge drives weathering and waxing from data maps; Fabric registers the same pairs in
    # code (see FixfootFabric). Harmless dead data on Fabric, which never reads this namespace.
    maps = os.path.join(RES, "data/neoforge/data_maps/block")
    merge_map(f"{maps}/oxidizables.json", {
        f"{MODID}:{name(STAGES[i][0], False)}": {
            "next_oxidation_stage": f"{MODID}:{name(STAGES[i + 1][0], False)}"}
        for i in range(len(STAGES) - 1)
    })
    merge_map(f"{maps}/waxables.json", {
        f"{MODID}:{name(prefix, False)}": {"waxed": f"{MODID}:{name(prefix, True)}"}
        for prefix, *_ in STAGES
    })

    lang_path = f"{ASSETS}/lang/en_us.json"
    if os.path.exists(lang_path):
        merged = json.load(open(lang_path))
        merged.update(lang)
        lang = merged
    write(lang_path, lang)

    # Copper needs a stone pickaxe, same as the blocks it's made of.
    mine = [f"{MODID}:{i}" for i in ids]
    for tag in ("mineable/pickaxe", "needs_stone_tool"):
        merge(f"{RES}/data/minecraft/tags/block/{tag}.json", mine, mine)

    print(f"generated {len(ids)} basins ({len(STAGES)} stages x waxed/unwaxed)")


if __name__ == "__main__":
    main()
