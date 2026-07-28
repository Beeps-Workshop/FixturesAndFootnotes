#!/usr/bin/env python3
"""
Generates the recipe-book unlock advancements for the hand-written crafting recipes.

A recipe with no unlock advancement is craftable but never appears in the recipe book, so a player
without JEI has no way to discover it. Vanilla ships one advancement per recipe; this writes the
same thing for ours.

The trigger item is read out of each recipe rather than hardcoded here, so retuning a recipe's
ingredients can't silently leave its unlock pointing at something you no longer craft it from.
Run from the repo root after changing any recipe:

    python3 tools/gen_recipe_unlocks.py

Slat stonecutting recipes are not handled here — tools/gen_slats.py emits those advancements
alongside the recipes it generates, under advancement/recipes/building_blocks.
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "src/main/resources/data/fixfoot")
RECIPES = os.path.join(DATA, "recipe")
OUT = os.path.join(DATA, "advancement/recipes/decorations")
MODID = "fixfoot"


def unlock_advancement(recipe_id, ingredient):
    """Vanilla's shape: granted on first picking up the ingredient, or if the recipe arrived some
    other way. Parented to the hidden minecraft:recipes/root so it stays off the advancement screen."""
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


def first_ingredient(recipe):
    """The item in the recipe's top-left occupied slot. Item predicates take a bare id or a #tag."""
    symbol = next(c for c in "".join(recipe["pattern"]) if c != " ")
    entry = recipe["key"][symbol]
    return entry["item"] if "item" in entry else "#" + entry["tag"]


def main():
    os.makedirs(OUT, exist_ok=True)
    for existing in os.listdir(OUT):
        os.remove(os.path.join(OUT, existing))

    count = 0
    for filename in sorted(os.listdir(RECIPES)):
        if "stonecutting" in filename:
            continue
        recipe = json.load(open(os.path.join(RECIPES, filename)))
        if recipe.get("type") != "minecraft:crafting_shaped":
            print(f"skipping {filename}: only shaped recipes are handled")
            continue
        name = filename[:-len(".json")]
        body = unlock_advancement(f"{MODID}:{name}", first_ingredient(recipe))
        with open(os.path.join(OUT, filename), "w") as fh:
            json.dump(body, fh, indent=2)
            fh.write("\n")
        count += 1
        print(f"  {name:22} unlocks on {first_ingredient(recipe)}")
    print(f"wrote {count} advancements to {os.path.relpath(OUT, ROOT)}")


if __name__ == "__main__":
    main()
