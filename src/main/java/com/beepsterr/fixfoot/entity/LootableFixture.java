package com.beepsterr.fixfoot.entity;

import org.jetbrains.annotations.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * A fixture that can be handed a loot table and fill itself from it — the mechanism structures use
 * to generate a shed whose racks and planters aren't empty.
 *
 * <p>Deliberately not vanilla's {@link net.minecraft.world.entity.vehicle.ContainerEntity}, for two
 * reasons:
 *
 * <ul>
 *   <li>That interface extends {@code Container} and {@code MenuProvider}, so every implementor
 *       must offer a screen. {@link WidePlanterEntity} is three synched slots with a plantable
 *       filter and no menu at all, and shouldn't be made to pretend otherwise.</li>
 *   <li>Its four loot accessors were renamed between 1.21.1 and 1.21.4 ({@code getLootTable} became
 *       {@code getContainerLootTable}, and so on), so implementing it in shared code would need a
 *       Stonecutter seam per accessor. Owning the interface means the rename can't reach us.</li>
 * </ul>
 *
 * <p>The rolling itself is version-clean: {@code LootTable.getRandomItems(LootParams, long)},
 * {@code LootContextParams.ORIGIN} and {@code MinecraftServer.reloadableRegistries()} are identical
 * on both nodes. Only the <em>type</em> names around them moved ({@code LootContextParamSet} became
 * {@code ContextKeySet}), which never appears in {@link AttachedFixtureEntity#rollLoot} because it
 * only touches the static fields.
 *
 * <p>Unlike vanilla containers, which unpack on first open, fixtures roll on load — see
 * {@link AttachedFixtureEntity#tick()}. Their contents are visible from outside, so deferring would
 * show an empty planter that sprouts the moment someone clicks it.
 */
public interface LootableFixture {
    /** The table to roll on next load, or null once it has been rolled. */
    @Nullable
    ResourceKey<LootTable> getFixtureLootTable();

    void setFixtureLootTable(@Nullable ResourceKey<LootTable> table);

    /** Roll seed. Zero means "roll fresh every time", matching vanilla's convention for containers. */
    long getFixtureLootSeed();

    void setFixtureLootSeed(long seed);

    /**
     * Fill this fixture from the resolved table, however it stores things. Called server-side only,
     * exactly once.
     *
     * <p>Handed the table rather than a rolled list on purpose. A {@code Container} wants
     * {@link LootTable#fill}, which scatters stacks across random empty slots and sometimes splits
     * one stack into two — the reason vanilla chests don't read as a solid block of items from slot
     * zero. That behaviour lives inside {@code fill} and can't be recovered from its output.
     *
     * <p>Fixtures that aren't containers can't call {@code fill} at all, and roll with
     * {@link LootTable#getRandomItems(LootParams, long)} instead, then take what suits them.
     */
    void acceptLoot(LootTable table, LootParams params, long seed);
}
