#!/usr/bin/env python3
"""
Generator for the 1.21.4 item definition layer.

1.21.4 stopped resolving an item's appearance from assets/<ns>/models/item/<id>.json directly.
That file is still the model; what picks it is a new definition at assets/<ns>/items/<id>.json.
Without one an item renders as a missing model, so every item needs both.

    python3 tools/gen_item_definitions.py

Run it after the other generators — it works off whatever item models exist, so it covers the
hand-written items and the generated fan-outs alike without any of them knowing about it.

Both layouts ship together and no version branching is needed: 1.21.4 reads the definitions,
and 1.21.1 has no idea the items/ directory exists and ignores it.
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src/main/resources/assets/fixfoot")
MODID = "fixfoot"


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def main():
    models = os.path.join(ASSETS, "models/item")
    if not os.path.isdir(models):
        print("no item models to describe")
        return

    names = sorted(f[:-5] for f in os.listdir(models) if f.endswith(".json"))
    for name in names:
        write(f"{ASSETS}/items/{name}.json",
              {"model": {"type": "minecraft:model", "model": f"{MODID}:item/{name}"}})

    # Anything left behind by a renamed or removed item would keep pointing at a model that no
    # longer exists, so clear out definitions with nothing to describe.
    stale = 0
    items = os.path.join(ASSETS, "items")
    if os.path.isdir(items):
        for f in os.listdir(items):
            if f.endswith(".json") and f[:-5] not in names:
                os.remove(os.path.join(items, f))
                stale += 1

    print(f"described {len(names)} items" + (f", removed {stale} stale" if stale else ""))


if __name__ == "__main__":
    main()
