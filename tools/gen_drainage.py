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
SLOTS = {"0": "body", "2": "inside"}

# The dark mouth of the drain. Not per stage — the inside of a pipe is a hole, and a hole does not
# oxidise — so every stage binds the same texture.
INTERIOR = "minecraft:block/black_concrete"

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
DRAIN_PARTS = {
    "": "Drainage/CopperDrainageBottomPiece",
}
PIPE_PARTS = {
    "": "Drainage/CopperDrainagePipe",
    "_connected": "Drainage/CopperDrainagePipeGutterConnection",
}

# Blocks cut from one copper block, by family. Copper is priced at 9 per block: the thin pieces you
# need a lot of are near enough an ingot each, the drain is chunkier and costs about two.
CUTS = {"gutter": 8, "drain_pipe": 8, "drain": 4}

# The authored models face north; a facing is that many clockwise quarter turns from it.
TURNS = {"north": 0, "east": 90, "south": 180, "west": 270}


def source_block(prefix, waxed):
    """The vanilla copper block of this stage, which is what the drainage pieces are cut from."""
    base = "copper_block" if not prefix else prefix + "copper"
    return "minecraft:" + ("waxed_" if waxed else "") + base


def stonecut(base, result_item, count):
    return {
        "type": "minecraft:stonecutting",
        "ingredient": {"item": base},
        "result": {"count": count, "id": result_item},
    }


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


# CopperGutterBlock.GutterShape -> the authored model it wears and how far that model is turned.
# The turn is the shape's own, independent of the block's facing: five models cover nine shapes
# because a corner turned is a different corner, and that independence is what lets the facing
# stay exactly as the player placed it instead of being derived along with the shape.
GUTTER_SHAPES = {
    "straight":         ("", 0),
    "left_front_near":  ("_inner", 0),
    "left_front_far":   ("_tight", 0),
    "left_back_near":   ("_tight", 270),
    "left_back_far":    ("_wide", 0),
    "right_front_near": ("_inner", 90),
    "right_front_far":  ("_tight_mirrored", 0),
    "right_back_near":  ("_tight_mirrored", 90),
    "right_back_far":   ("_wide", 270),
}


def gutter_blockstate(stage_id):
    """
    Thirty-six variants over five models: nine shapes at four facings. The model is turned by the
    shape's own rotation plus the facing's, which is the whole reason the facing can be left alone.
    """
    variants = {}
    for facing, facing_turn in TURNS.items():
        for shape, (part, shape_turn) in GUTTER_SHAPES.items():
            variants[f"facing={facing},shape={shape}"] = apply(
                f"{MODID}:block/{stage_id}{part}", (facing_turn + shape_turn) % 360)
    return {"variants": variants}


def pipe_blockstate(stage_id):
    """Plain column until a gutter sits above; then the collar, turned to face that gutter."""
    variants = {}
    for facing, base in TURNS.items():
        variants[f"connected=false,facing={facing}"] = apply(f"{MODID}:block/{stage_id}", 0)
        variants[f"connected=true,facing={facing}"] = apply(f"{MODID}:block/{stage_id}_connected", base)
    return {"variants": variants}


def drain_blockstate(stage_id):
    """
    One model, turned to whichever way the mouth was pointed. Every flow state shares it for now —
    the water leaving the mouth is particles rather than geometry, so only a stream for `flowing`
    is still missing.
    """
    return {"variants": {f"facing={facing},flow={flow}": apply(f"{MODID}:block/{stage_id}", turn)
                         for facing, turn in TURNS.items()
                         for flow in ("empty", "flowing", "dripping")}}


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
        ("drain", "Drain", DRAIN_PARTS, drain_blockstate),
    ]

    lang = {}
    ids = []
    for family, noun, parts, blockstate in families:
        # Shared geometry, one base per authored piece. Which slots a piece actually uses varies
        # (only the drain has an interior), so each records its own for the per-stage models.
        used = {}
        for part_suffix, source in parts.items():
            base = authored.load(source, SLOTS)
            used[part_suffix] = {face["texture"].lstrip("#")
                                 for element in base["elements"] for face in element["faces"].values()}
            write(f"{ASSETS}/models/block/{family}_base{part_suffix}.json", base)

        for prefix, disp, texture in STAGES:
            stage_id = name(prefix, False, family)
            for part_suffix in parts:
                textures = {"body": texture}
                if "inside" in used[part_suffix]:
                    textures["inside"] = INTERIOR
                write(f"{ASSETS}/models/block/{stage_id}{part_suffix}.json",
                      {"parent": f"{MODID}:block/{family}_base{part_suffix}", "textures": textures})

            for waxed in (False, True):
                block_id = name(prefix, waxed, family)
                ids.append(block_id)
                write(f"{ASSETS}/blockstates/{block_id}.json", blockstate(stage_id))
                write(f"{ASSETS}/models/item/{block_id}.json", {"parent": f"{MODID}:block/{stage_id}"})
                write(f"{DATA}/loot_table/blocks/{block_id}.json", block_loot(f"{MODID}:{block_id}"))
                lang[f"block.fixfoot.{block_id}"] = display(disp, waxed, noun)

    # Cut from copper rather than crafted: it is how vanilla shapes every other copper block, and
    # it keeps each ageing stage cutting into its own without a recipe book full of near-duplicates.
    for family, *_ in families:
        for prefix, *_rest in STAGES:
            for waxed in (False, True):
                block_id = name(prefix, waxed, family)
                base = source_block(prefix, waxed)
                recipe(f"{block_id}_from_{base.split(':')[1]}_stonecutting",
                       stonecut(base, f"{MODID}:{block_id}", CUTS[family]), base)

    # Waxing stays a crafting recipe, mirroring vanilla's shortcut alongside the honeycomb interaction.
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
