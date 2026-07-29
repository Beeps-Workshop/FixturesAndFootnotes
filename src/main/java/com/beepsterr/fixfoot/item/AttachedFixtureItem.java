package com.beepsterr.fixfoot.item;

import com.beepsterr.fixfoot.entity.AttachedFixtureEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.beepsterr.fixfoot.compat.Interactions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Places an {@link AttachedFixtureEntity} onto the clicked block face, the way an item frame is
 * placed. Vanilla's HangingEntityItem hardcodes its handful of entity types, so fixtures get their
 * own placer that takes a factory instead.
 */
public class AttachedFixtureItem extends Item {

    /** Builds the fixture for a placement attempt. {@code facing} points away from the support block. */
    @FunctionalInterface
    public interface FixtureFactory {
        AttachedFixtureEntity create(Level level, BlockPos pos, Direction facing);
    }

    private final FixtureFactory factory;

    public AttachedFixtureItem(FixtureFactory factory, Properties properties) {
        super(properties);
        this.factory = factory;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Direction facing = context.getClickedFace();
        BlockPos attachPos = context.getClickedPos().relative(facing);
        ItemStack stack = context.getItemInHand();

        if (player == null || !player.mayUseItemAt(attachPos, facing, stack)) {
            return InteractionResult.FAIL;
        }

        Level level = context.getLevel();
        AttachedFixtureEntity fixture = factory.create(level, attachPos, facing);
        if (!fixture.canAttachTo(facing) || !fixture.survives()) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide) {
            fixture.playPlacementSound();
            level.addFreshEntity(fixture);
            stack.shrink(1);
        }
        return Interactions.sided(level);
    }
}
