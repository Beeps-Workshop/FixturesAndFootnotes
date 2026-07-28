#!/usr/bin/env python3
"""
Renders the mod icon: the Wide Planter holding carrots / rose bush / carrots.

Rather than a drawing of the planter, this is a render of the actual thing — it reads
src/main/resources/assets/fixfoot/models/block/wide_planter.json and the real vanilla block
textures out of the Minecraft client jar, positions the plants exactly where WidePlanterRenderer
puts them, and projects it the way Minecraft's own GUI does. So it stays honest when the model
changes. Run from the repo root:

    python3 tools/render_icon.py

Writes src/main/resources/assets/fixfoot/icon.png, which both fabric.mod.json and
neoforge.mods.toml point at.

Needs Pillow, and a Minecraft client jar (any 1.21.1 dev checkout has one in the Gradle cache).
"""
import io
import json
import math
import os
import sys
import zipfile

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODEL = f"{ROOT}/src/main/resources/assets/fixfoot/models/block/wide_planter.json"
OUT = f"{ROOT}/src/main/resources/assets/fixfoot/icon.png"
CLIENT = os.environ.get(
    "MC_CLIENT_JAR",
    os.path.expanduser("~/.gradle/caches/fabric-loom/1.21.1/minecraft-client.jar"),
)

SIZE = 256       # icon edge, in pixels
SUPERSAMPLE = 8  # rendered at SIZE*SUPERSAMPLE, then box-filtered down for clean edges
PITCH, YAW = 30.0, 225.0  # matches vanilla's block display transform, so it reads as an MC item

# Vanilla's per-face shading. Without this every face is the same flat colour and the box reads
# as a silhouette rather than a solid.
SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "west": 0.6, "east": 0.6}


def main():
    if not os.path.exists(CLIENT):
        sys.exit(f"No client jar at {CLIENT}. Set MC_CLIENT_JAR to one.")
    jar = zipfile.ZipFile(CLIENT)

    def texture(name):
        with jar.open(f"assets/minecraft/textures/block/{name}.png") as fh:
            im = Image.open(io.BytesIO(fh.read())).convert("RGBA")
        return im.crop((0, 0, im.width, im.width)) if im.height > im.width else im

    quads = []

    def box(lo, hi, faces):
        """One axis-aligned cuboid; `faces` maps a face name to (texture, uv)."""
        x0, y0, z0 = lo
        x1, y1, z1 = hi
        corners = {
            "up": [(x0, y1, z0), (x0, y1, z1), (x1, y1, z1), (x1, y1, z0)],
            "down": [(x0, y0, z1), (x0, y0, z0), (x1, y0, z0), (x1, y0, z1)],
            "north": [(x1, y1, z0), (x1, y0, z0), (x0, y0, z0), (x0, y1, z0)],
            "south": [(x0, y1, z1), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1)],
            "west": [(x0, y1, z0), (x0, y0, z0), (x0, y0, z1), (x0, y1, z1)],
            "east": [(x1, y1, z1), (x1, y0, z1), (x1, y0, z0), (x1, y1, z0)],
        }
        for name, (tex, uv) in faces.items():
            quads.append((corners[name], tex, uv, SHADE[name]))

    def cross(cx, cz, y0, y1, size, tex):
        """Vanilla `cross`: two quads on the block diagonals, unshaded, drawn from both sides."""
        h = size / 2.0
        for ax, az, bx, bz in ((-h, -h, h, h), (h, -h, -h, h)):
            pts = [(cx + ax, y1, cz + az), (cx + ax, y0, cz + az),
                   (cx + bx, y0, cz + bz), (cx + bx, y1, cz + bz)]
            quads.append((pts, tex, (0, 0, 16, 16), 1.0))
            quads.append((pts[::-1], tex, (16, 0, 0, 16), 1.0))

    model = json.load(open(MODEL))
    pot = {"#pot": texture("terracotta"), "#soil": texture("dirt")}
    for element in model["elements"]:
        faces = {n: (pot[f["texture"]], tuple(f.get("uv", [0, 0, 16, 16])))
                 for n, f in element["faces"].items()}
        box(element["from"], element["to"], faces)

    # Plant placement mirrors WidePlanterRenderer: slotOffsetX across the trough, resting on the
    # soil, half scale. Keep these in step with that class if it ever moves.
    carrots = texture("carrots_stage3")
    rose_lower, rose_upper = texture("rose_bush_bottom"), texture("rose_bush_top")
    scale, plant_z, soil_top = 0.5, 0.25, 8
    for slot in range(3):
        cx = 8 + ((slot + 0.5) / 3 - 0.5) * 16
        cz = 8 + plant_z * 16
        size = 16 * scale
        if slot == 1:
            cross(cx, cz, soil_top, soil_top + size, size, rose_lower)
            cross(cx, cz, soil_top + size, soil_top + 2 * size, size, rose_upper)
        else:
            cross(cx, cz, soil_top, soil_top + size, size, carrots)

    # Negative pitch: with a positive one the depth term makes higher points farther away, which
    # is a view from underneath — no top face and no visible trough interior.
    rx, ry = math.radians(-PITCH), math.radians(YAW)

    def project(p):
        x, y, z = p[0] - 8, p[1] - 8, p[2] - 8
        x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
        y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
        return x, -y, z  # screen y grows downward; z is depth, smaller is nearer

    projected = [([project(p) for p in pts], tex, uv, sh) for pts, tex, uv, sh in quads]
    xs = [p[0] for q in projected for p in q[0]]
    ys = [p[1] for q in projected for p in q[0]]
    pad = 0.06 * max(max(xs) - min(xs), max(ys) - min(ys))
    minx, maxx = min(xs) - pad, max(xs) + pad
    miny, maxy = min(ys) - pad, max(ys) + pad
    span = max(maxx - minx, maxy - miny)
    w = SIZE * SUPERSAMPLE
    offx, offy = (span - (maxx - minx)) / 2, (span - (maxy - miny)) / 2

    def to_px(p):
        return ((p[0] - minx + offx) / span * w, (p[1] - miny + offy) / span * w)

    img = Image.new("RGBA", (w, w), (0, 0, 0, 0))
    px = img.load()
    zbuf = [[1e9] * w for _ in range(w)]

    def raster(tri, tex, uvs, depth, shade):
        (x0, y0), (x1, y1), (x2, y2) = tri
        area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
        if abs(area) < 1e-9:
            return
        tw, th = tex.size
        tp = tex.load()
        for yy in range(max(0, int(min(y0, y1, y2))), min(w, int(max(y0, y1, y2)) + 2)):
            for xx in range(max(0, int(min(x0, x1, x2))), min(w, int(max(x0, x1, x2)) + 2)):
                cx, cy = xx + 0.5, yy + 0.5
                w0 = ((x1 - cx) * (y2 - cy) - (x2 - cx) * (y1 - cy)) / area
                w1 = ((x2 - cx) * (y0 - cy) - (x0 - cx) * (y2 - cy)) / area
                w2 = 1 - w0 - w1
                if w0 < 0 or w1 < 0 or w2 < 0:
                    continue
                d = w0 * depth[0] + w1 * depth[1] + w2 * depth[2]
                if d >= zbuf[yy][xx]:
                    continue
                u = w0 * uvs[0][0] + w1 * uvs[1][0] + w2 * uvs[2][0]
                v = w0 * uvs[0][1] + w1 * uvs[1][1] + w2 * uvs[2][1]
                # Nearest-neighbour, so texels stay square instead of turning to mush.
                r, g, b, a = tp[min(tw - 1, max(0, int(u / 16 * tw))),
                                min(th - 1, max(0, int(v / 16 * th)))]
                if a < 128:  # cutout, matching how these models render in game
                    continue
                zbuf[yy][xx] = d
                px[xx, yy] = (int(r * shade), int(g * shade), int(b * shade), 255)

    for pts, tex, uv, shade in projected:
        screen = [to_px(p) for p in pts]
        depth = [p[2] for p in pts]
        u0, v0, u1, v1 = uv
        uvc = [(u0, v0), (u0, v1), (u1, v1), (u1, v0)]
        for idx in ((0, 1, 2), (0, 2, 3)):
            raster([screen[i] for i in idx], tex, [uvc[i] for i in idx],
                   [depth[i] for i in idx], shade)

    img.resize((SIZE, SIZE), Image.LANCZOS).save(OUT)
    print(f"wrote {OUT} ({SIZE}x{SIZE}, {len(quads)} quads)")


if __name__ == "__main__":
    main()
