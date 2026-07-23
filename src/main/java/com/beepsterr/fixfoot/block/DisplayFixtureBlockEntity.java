package com.beepsterr.fixfoot.block;

import com.beepsterr.fixfoot.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DisplayFixtureBlockEntity extends BlockEntity {
    private ItemStack displayedItem = ItemStack.EMPTY;

    public DisplayFixtureBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISPLAY_FIXTURE.get(), pos, state);
    }

    public ItemStack getDisplayedItem() {
        return displayedItem;
    }

    public void setDisplayedItem(ItemStack stack) {
        this.displayedItem = stack;
        setChanged();
        if (level != null) {
            // Trigger a block update so the renderer re-reads the synced item.
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // parseOptional yields EMPTY for an absent/empty compound.
        this.displayedItem = ItemStack.parseOptional(registries, tag.getCompound("DisplayedItem"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // Always write the key (saveOptional encodes EMPTY as an empty compound) so a removal
        // syncs "empty" to clients instead of sending an empty update packet that leaves the
        // rendered item stale.
        tag.put("DisplayedItem", displayedItem.saveOptional(registries));
    }

    // --- Client sync: send the stored stack to clients so the BER can draw it. ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
