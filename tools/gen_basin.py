#!/usr/bin/env python3
"""
Generator for the copper basin's oxidation fan-out.

Four weathering stages, each with a waxed twin that reuses the unwaxed models (vanilla does the
same for waxed copper), and four fill levels each. Run from the repo root:

    python3 tools/gen_basin.py

Writes blockstates, block + item models, loot tables, recipes with their unlock advancements, and
the NeoForge oxidation/waxing data maps; merges the en_us and mineable tag entries so hand-added
lines survive. The geometry constants below are the same numbers CopperBasinBlock declares in
code (its shape and water height are derived from them) — change the two together.
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src/main/resources")
ASSETS = os.path.join(RES, "assets/fixfoot")
DATA = os.path.join(RES, "data/fixfoot")
MODID = "fixfoot"
# Basin advancements live under decorations, next to the other hand-written ones — gen_slats.py
# wipes and rebuilds recipes/building_blocks, so nothing of ours can live there.
ADVANCEMENTS = "decorations"

# Geometry, in pixels. Mirrors CopperBasinBlock: 2px feet, walls to 12, inner floor topping out
# at 4, water surface at 7/9/11 for levels 1..3 — a pixel below the rim when full.
FOOT_HEIGHT = 2
HEIGHT = 12
FLOOR = 4
WATER_BASE = 5
WATER_PER_LEVEL = 2
MAX_LEVEL = 3

# prefix, display name, texture — freshest first, matching ModBlocks.BASIN_STAGES.
STAGES = [
    ("", "Copper", "minecraft:block/copper_block"),
    ("exposed_", "Exposed Copper", "minecraft:block/exposed_copper"),
    ("weathered_", "Weathered Copper", "minecraft:block/weathered_copper"),
    ("oxidized_", "Oxidized Copper", "minecraft:block/oxidized_copper"),
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


def faces(spec):
    """{'north': '#side'} or {'north': ('#side', 'north')} -> model face entries, the 2nd being a cullface."""
    out = {}
    for face, value in spec.items():
        texture, cull = value if isinstance(value, tuple) else (value, None)
        out[face] = {"texture": texture} if cull is None else {"texture": texture, "cullface": cull}
    return out


def walls():
    """The four 2px sides. No 'up' cullface — the rim stops at y=12, so a block above can't hide it."""
    return [
        {"from": [0, FOOT_HEIGHT, 0], "to": [2, HEIGHT, 16], "faces": faces({
            "north": ("#side", "north"), "east": "#side", "south": ("#side", "south"),
            "west": ("#side", "west"), "up": "#top", "down": "#inside"})},
        {"from": [14, FOOT_HEIGHT, 0], "to": [16, HEIGHT, 16], "faces": faces({
            "north": ("#side", "north"), "east": ("#side", "east"), "south": ("#side", "south"),
            "west": "#side", "up": "#top", "down": "#inside"})},
        {"from": [2, FOOT_HEIGHT, 0], "to": [14, HEIGHT, 2], "faces": faces({
            "north": ("#side", "north"), "south": "#side", "up": "#top", "down": "#inside"})},
        {"from": [2, FOOT_HEIGHT, 14], "to": [14, HEIGHT, 16], "faces": faces({
            "north": "#side", "south": ("#side", "south"), "up": "#top", "down": "#inside"})},
        # inner floor
        {"from": [2, FOOT_HEIGHT, 2], "to": [14, FLOOR, 14],
         "faces": faces({"up": "#inside", "down": "#inside"})},
    ]


def feet():
    """Four L-shaped corner legs, vanilla's cauldron layout squashed to FOOT_HEIGHT."""
    legs = [
        ([0, 0, 0], [4, FOOT_HEIGHT, 2], {"north": ("#side", "north"), "east": "#side",
                                          "south": "#side", "west": ("#side", "west")}),
        ([0, 0, 2], [2, FOOT_HEIGHT, 4], {"east": "#side", "south": "#side",
                                          "west": ("#side", "west")}),
        ([12, 0, 0], [16, FOOT_HEIGHT, 2], {"north": ("#side", "north"), "east": ("#side", "east"),
                                            "south": "#side", "west": "#side"}),
        ([14, 0, 2], [16, FOOT_HEIGHT, 4], {"east": ("#side", "east"), "south": "#side",
                                            "west": "#side"}),
        ([0, 0, 14], [4, FOOT_HEIGHT, 16], {"north": "#side", "east": "#side",
                                            "south": ("#side", "south"), "west": ("#side", "west")}),
        ([0, 0, 12], [2, FOOT_HEIGHT, 14], {"north": "#side", "east": "#side",
                                            "west": ("#side", "west")}),
        ([12, 0, 14], [16, FOOT_HEIGHT, 16], {"north": "#side", "east": ("#side", "east"),
                                              "south": ("#side", "south"), "west": "#side"}),
        ([14, 0, 12], [16, FOOT_HEIGHT, 14], {"north": "#side", "east": ("#side", "east"),
                                              "west": "#side"}),
    ]
    out = []
    for start, end, spec in legs:
        spec = dict(spec)
        spec["down"] = ("#bottom", "down")
        out.append({"from": start, "to": end, "faces": faces(spec)})
    return out


def base_model(level):
    """Shared geometry for one fill level; the per-stage models just bind the textures."""
    elements = walls() + feet()
    if level > 0:
        surface = WATER_BASE + WATER_PER_LEVEL * level
        elements.append({
            "__comment": "Water surface. Flat quad like a cauldron's — no fluid is rendered; "
                         "tintindex 0 is the biome water colour, applied in FixfootClient.",
            "from": [2, FLOOR, 2], "to": [14, surface, 14],
            "faces": {"up": {"texture": "#content", "tintindex": 0}},
        })
    return {"ambientocclusion": False, "textures": {"particle": "#side"}, "elements": elements}


def suffix(level):
    return "" if level == 0 else f"_level{level}"


def block_loot(item):
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": item}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    }


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
    for prefix, disp, texture in STAGES:
        stage_id = name(prefix, False)
        # Block models: only the unwaxed stage has them; the waxed twin points at these.
        for level in range(MAX_LEVEL + 1):
            textures = {slot: texture for slot in ("side", "top", "bottom", "inside")}
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

    # A basin is a cauldron two rows tall instead of three — five ingots rather than seven.
    recipe("copper_basin", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["C C", "CCC"],
        "key": {"C": {"item": "minecraft:copper_ingot"}},
        "result": {"count": 1, "id": f"{MODID}:copper_basin"},
    }, "minecraft:copper_ingot")

    # Waxing is normally a honeycomb interaction; vanilla also ships the crafting shortcut, so match it.
    for prefix, _disp, _tex in STAGES:
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
    write(f"{maps}/oxidizables.json", {"replace": False, "values": {
        f"{MODID}:{name(STAGES[i][0], False)}": {
            "next_oxidation_stage": f"{MODID}:{name(STAGES[i + 1][0], False)}"}
        for i in range(len(STAGES) - 1)
    }})
    write(f"{maps}/waxables.json", {"replace": False, "values": {
        f"{MODID}:{name(prefix, False)}": {"waxed": f"{MODID}:{name(prefix, True)}"}
        for prefix, _disp, _tex in STAGES
    }})

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
