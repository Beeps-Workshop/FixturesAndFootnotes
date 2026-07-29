#!/usr/bin/env python3
"""
Generator for the copper gutter and drain pipe.

Both weather through the same four stages as the basin, each with a waxed twin that reuses the
unwaxed models. Run from the repo root:

    python3 tools/gen_drainage.py

Geometry is not written here — it is read from the authored Blockbench exports under
assets/models/json/Drainage/, so reshaping a piece means re-exporting and re-running this. What
this owns is the fan-out: which authored model each blockstate points at, and how far it is turned.

The gutter is deliberately not on vanilla's stairs scheme: a stair is symmetric front-to-back and
a channel is not, so a gutter tells apart three corners where stairs see two. Four models cover all
sixteen facing/shape combinations as plain y-rotations, with no mirrored pieces to author, because
a corner's handedness lives in the facing CopperGutterBlock derives for it.

No water yet: this is placement only, so the gutter has no filled state and the pipe has no flow.
"""
import json
import os

import authored

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src/main/resources")
ASSETS = os.path.join(RES, "assets/fixfoot")
DATA = os.path.join(RES, "data/fixfoot")
MODID = "fixfoot"
ADVANCEMENTS = "decorations"

# Blockbench texture slot -> the name the per-stage models bind. Both pieces use a single slot.
SLOTS = {"0": "body"}

# prefix, display name, texture — freshest first, matching ModBlocks.COPPER_STAGES.
STAGES = [
    ("", "Copper", "minecraft:block/copper_block"),
    ("exposed_", "Exposed Copper", "minecraft:block/exposed_copper"),
    ("weathered_", "Weathered Copper", "minecraft:block/weathered_copper"),
    ("oxidized_", "Oxidized Copper", "minecraft:block/oxidized_copper"),
]

# The authored pieces, keyed by the model suffix they get, matching CopperGutterBlock.GutterShape.
# Every corner takes water in from the left and turns it out the front or back edge; they differ by
# where along that edge, which is set by the facing of the gutter they hand off to.
GUTTER_PARTS = {
    "": "Drainage/CopperGutter",
    "_inner": "Drainage/CopperGutterCornerInner",
    "_tight": "Drainage/CopperGutterCornerTight",
    "_wide": "Drainage/CopperGutterCornerWide",
    "_tight_mirrored": "Drainage/CopperGutterCornerTightM",
}
PIPE_PARTS = {
    "": "Drainage/CopperDrainagePipe",
    "_connected": "Drainage/CopperDrainagePipeGutterConnection",
}

# The authored models face north; a facing is that many clockwise quarter turns from it.
TURNS = {"north": 0, "east": 90, "south": 180, "west": 270}


def name(prefix, waxed, family):
    return ("waxed_" if waxed else "") + prefix + "copper_" + family


def display(disp, waxed, noun):
    return ("Waxed " if waxed else "") + disp + " " + noun


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def apply(model, y):
    """A blockstate variant, with uvlock so a turned model keeps its texture upright."""
    return {"model": model} if y == 0 else {"model": model, "y": y, "uvlock": True}


def gutter_blockstate(stage_id):
    """
    Sixteen variants over four models, one per shape and facing. There are no mirrored corners to
    author: a corner's handedness lives in the facing the block derives for itself, so the mirror
    of any corner is the same model under a different facing.
    """
    variants = {}
    for facing, turn in TURNS.items():
        for shape, part in (("straight", ""), ("inner", "_inner"), ("tight", "_tight"),
                            ("wide", "_wide"), ("tight_mirrored", "_tight_mirrored")):
            variants[f"facing={facing},shape={shape}"] = apply(f"{MODID}:block/{stage_id}{part}", turn)
    return {"variants": variants}


def pipe_blockstate(stage_id):
    """Plain column until a gutter sits above; then the collar, turned to face that gutter."""
    variants = {}
    for facing in TURNS:
        variants[f"connected=false,facing={facing}"] = apply(f"{MODID}:block/{stage_id}", 0)
    for facing, base in TURNS.items():
        variants[f"connected=true,facing={facing}"] = apply(f"{MODID}:block/{stage_id}_connected", base)
    return {"variants": variants}


def block_loot(item):
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": item}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    }


def merge_list(path, values):
    """Rewrite a tag file, keeping every entry that isn't one of ours."""
    kept = []
    if os.path.exists(path):
        kept = [v for v in json.load(open(path)).get("values", []) if v not in values]
    write(path, {"replace": False, "values": kept + values})


def merge_map(path, values):
    """Fold entries into a NeoForge data map, leaving the other copper families' alone."""
    existing = json.load(open(path))["values"] if os.path.exists(path) else {}
    existing.update(values)
    write(path, {"replace": False, "values": existing})


def unlock_advancement(recipe_id, ingredient):
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
    families = [
        ("gutter", "Gutter", GUTTER_PARTS, gutter_blockstate),
        ("drain_pipe", "Drain Pipe", PIPE_PARTS, pipe_blockstate),
    ]

    lang = {}
    ids = []
    for family, noun, parts, blockstate in families:
        # Shared geometry, one base per authored piece.
        for part_suffix, source in parts.items():
            write(f"{ASSETS}/models/block/{family}_base{part_suffix}.json", authored.load(source, SLOTS))

        for prefix, disp, texture in STAGES:
            stage_id = name(prefix, False, family)
            for part_suffix in parts:
                write(f"{ASSETS}/models/block/{stage_id}{part_suffix}.json",
                      {"parent": f"{MODID}:block/{family}_base{part_suffix}", "textures": {"body": texture}})

            for waxed in (False, True):
                block_id = name(prefix, waxed, family)
                ids.append(block_id)
                write(f"{ASSETS}/blockstates/{block_id}.json", blockstate(stage_id))
                write(f"{ASSETS}/models/item/{block_id}.json", {"parent": f"{MODID}:block/{stage_id}"})
                write(f"{DATA}/loot_table/blocks/{block_id}.json", block_loot(f"{MODID}:{block_id}"))
                lang[f"block.fixfoot.{block_id}"] = display(disp, waxed, noun)

    # Gutters come off the stonecutter like the rest of copper's shapes do; pipes are a short run.
    recipe("copper_gutter", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["C C", "CCC"],
        "key": {"C": {"item": "minecraft:copper_ingot"}},
        "result": {"count": 6, "id": f"{MODID}:copper_gutter"},
    }, "minecraft:copper_ingot")
    recipe("copper_drain_pipe", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["C", "C", "C"],
        "key": {"C": {"item": "minecraft:copper_ingot"}},
        "result": {"count": 4, "id": f"{MODID}:copper_drain_pipe"},
    }, "minecraft:copper_ingot")

    for family, *_ in families:
        for prefix, *_rest in STAGES:
            unwaxed, waxed_id = name(prefix, False, family), name(prefix, True, family)
            recipe(f"{waxed_id}_from_honeycomb", {
                "type": "minecraft:crafting_shapeless",
                "category": "misc",
                "group": f"waxed_copper_{family}",
                "ingredients": [{"item": f"{MODID}:{unwaxed}"}, {"item": "minecraft:honeycomb"}],
                "result": {"count": 1, "id": f"{MODID}:{waxed_id}"},
            }, f"{MODID}:{unwaxed}")

    maps = os.path.join(RES, "data/neoforge/data_maps/block")
    oxidizables, waxables = {}, {}
    for family, *_ in families:
        for i, (prefix, *_rest) in enumerate(STAGES):
            waxables[f"{MODID}:{name(prefix, False, family)}"] = {"waxed": f"{MODID}:{name(prefix, True, family)}"}
            if i + 1 < len(STAGES):
                oxidizables[f"{MODID}:{name(prefix, False, family)}"] = {
                    "next_oxidation_stage": f"{MODID}:{name(STAGES[i + 1][0], False, family)}"}
    merge_map(f"{maps}/oxidizables.json", oxidizables)
    merge_map(f"{maps}/waxables.json", waxables)

    lang_path = f"{ASSETS}/lang/en_us.json"
    if os.path.exists(lang_path):
        merged = json.load(open(lang_path))
        merged.update(lang)
        lang = merged
    write(lang_path, lang)

    mine = [f"{MODID}:{i}" for i in ids]
    for tag in ("mineable/pickaxe", "needs_stone_tool"):
        merge_list(f"{RES}/data/minecraft/tags/block/{tag}.json", mine)

    print(f"generated {len(ids)} drainage blocks ({len(families)} families x {len(STAGES)} stages x waxed/unwaxed)")


if __name__ == "__main__":
    main()
