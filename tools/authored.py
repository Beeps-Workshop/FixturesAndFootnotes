"""
Shared helper for reading Blockbench exports out of assets/models/json/.

The generators keep geometry in the authored file rather than restating it, so reshaping a block
is a re-export plus a re-run. Elements are copied wholesale — only the Blockbench texture slots
are renamed — so per-element keys the exporter emits (rotation, shade, …) survive untouched.
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
AUTHORED = os.path.join(ROOT, "assets/models/json")

# Every block model needs item transforms, and these have no parent of their own. Vanilla's
# block/block carries nothing but gui_light + display, so it supplies them without touching
# geometry — without it a block item renders untransformed in the GUI and in hand.
DISPLAY_PARENT = "minecraft:block/block"


def load(name, slots, extra_elements=()):
    """
    The authored model at assets/models/json/<name>.json, with its texture slots renamed.

    slots maps the Blockbench slot ("0", "1", …) to the name the per-variant models bind, so a
    generator can rebind them per oxidation stage. Its own display block wins if it has one.
    """
    src = json.load(open(os.path.join(AUTHORED, name + ".json")))
    elements = []
    for element in src["elements"]:
        element = json.loads(json.dumps(element))  # deep copy; keep rotation/shade/uv as authored
        # Blockbench writes a rotation block on every element even when nothing is rotated. A
        # zero-angle rotation does nothing, so drop it rather than carry the noise into every
        # generated file — anything with a real angle is kept verbatim.
        if element.get("rotation", {}).get("angle") == 0:
            del element["rotation"]
        for face in element.get("faces", {}).values():
            face["texture"] = "#" + slots[face["texture"].lstrip("#")]
        elements.append(element)
    elements.extend(extra_elements)

    model = {
        "parent": DISPLAY_PARENT,
        "ambientocclusion": False,
        "textures": {"particle": "#" + next(iter(slots.values()))},
        "elements": elements,
    }
    if "display" in src:
        model["display"] = src["display"]
    return model
