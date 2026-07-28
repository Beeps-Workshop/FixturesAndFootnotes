# Launch checklist

First public release of Fixtures & Footnotes. **1.21.1 only** — see [Known gaps](#known-gaps).

Ships 44 creative-tab entries: 39 slat variants, 2 sheetmetal blocks, and 3 attached fixtures
(Wide Planter, Small Storage Net, Storage Net).

---

## 1. Blockers

Nothing below ships correctly until these are done.

- [ ] **Mod icon.** Both loaders reference `assets/fixfoot/icon.png` and the file does not exist.
      Put a 128×128 PNG at `src/main/resources/assets/fixfoot/icon.png`.
      Fabric logs a warning without it; NeoForge shows no logo in the mod list.
- [ ] **Mod description.** Both files still contain the literal string
      `<replace with your mod description>`:
      - `fabric/src/main/resources/fabric.mod.json`
      - `neoforge/src/main/resources/META-INF/neoforge.mods.toml`
- [ ] **Version number.** `mod.version` in `gradle.properties` is `0.1.0`. Decide whether a public
      first release should be `1.0.0`. The jar name derives from it.
- [ ] **Licence.** Both files say `ARR` (all rights reserved). Fine if deliberate — worth being a
      deliberate choice rather than a template leftover, since it governs packs and forks.

## 2. Build

- [ ] Build **per-version**, not `chiseledBuild`:
      ```
      ./gradlew :fabric:1.21.1:build :neoforge:1.21.1:build
      ```
      `chiseledBuild` also builds 1.21.4, which does not compile, so it fails the whole run.
- [ ] Collect jars from `fabric/build/libs/` and `neoforge/build/libs/`
- [ ] Confirm the filenames carry the version you settled on above
- [ ] Install both jars into a clean profile and confirm the mod loads with icon and description

## 3. Verify in a survival world

Creative has hidden real problems before — the fixtures were creative-only for a long time
without anyone noticing there were no recipes.

- [ ] Every item is obtainable: 5 crafting recipes + slats via stonecutter
- [ ] Recipe costs feel fair (all 5 are first-draft guesses — see `data/fixfoot/recipe/`)
- [ ] Breaking each block drops it
- [ ] Breaking a fixture drops both the fixture **and** its contents
- [ ] A stocked net and a planted planter survive a **world reload** — attachment, facing, contents
- [ ] Fixtures still hang correctly on partial supports: slats, fences, trapdoors
- [ ] Nothing crashes when placed on odd faces (floors, ceilings, snow layers)

## 4. Known gaps

Decide ship-or-fix for each. None are bugs; all are deliberate current limits.

- [ ] **1.21.4 does not compile.** `SlatPanelBlock` and `SlatScreenBlock` use APIs removed in
      1.21.2, and there are 4 further seams in the fixture code, each marked `VERSION SEAM`.
      Ship 1.21.1 only and port afterwards.
- [ ] **Recipes are not in the recipe book.** No unlock advancements, so recipes are craftable but
      undiscoverable. ~6 formulaic files if you want them.
- [ ] **Storage Nets cannot be placed side by side.** At 26px wide their hitboxes overlap, so
      `survives()` forces every-other-block spacing.
- [ ] **Wide Planter uses vanilla placeholder textures** (terracotta + dirt). Its geometry is
      final; only the texture references are placeholder.

## 5. Publish

- [ ] Tag the release and merge `wide-planter` into `main`
- [ ] Push (nothing has been pushed yet)
- [ ] Write release notes — the commit messages carry the reasoning and are a good source
- [ ] Upload to Modrinth / CurseForge as intended, with the 1.21.1-only support range set

---

## After launch

Roughly in dependency order:

1. **1.21.4 port** — the breaks are all 1.21.2-era and persist into 26.x, so the work carries
   forward even though the version isn't strictly a prerequisite.
2. **Trellis + Trellis Shelf** — the shelf reuses `AttachedFixtureEntity` nearly unchanged.
3. **Remaining board items** — Curtain, Rod, Pipe, Beams, Half-Doors, Basin, Watering Can,
   Copper Grate Panel, Trims.
