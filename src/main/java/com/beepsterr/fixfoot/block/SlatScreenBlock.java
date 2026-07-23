package com.beepsterr.fixfoot.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Freestanding standing slat screen — a room divider. Stands centered on the block's mid-plane,
 * runs along X or Z (picked from the placer's facing so the screen faces them), and tiles in a
 * straight line into a continuous divider. It collides (it's a divider) and is non-occluding.
 * Separate block from {@link SlatPanelBlock} (the wall-hugging multiface slats).
 */
public class SlatScreenBlock extends Block {
    public static final MapCodec<SlatScreenBlock> CODEC = simpleCodec(SlatScreenBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    // 2px panel on the centre plane, full width + height.
    private static final VoxelShape X_SHAPE = Block.box(0.0, 0.0, 7.0, 16.0, 16.0, 9.0);
    private static final VoxelShape Z_SHAPE = Block.box(7.0, 0.0, 0.0, 9.0, 16.0, 16.0);

    public SlatScreenBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Screen faces the placer: bars span the axis perpendicular to their look direction.
        Direction.Axis axis = context.getHorizontalDirection().getClockWise().getAxis();
        return defaultBlockState().setValue(AXIS, axis);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? X_SHAPE : Z_SHAPE;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        // Screen shares the wall slats' item — derive it from this block's id (…_slat_screen → …_slats).
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(this);
        ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(
                blockId.getNamespace(), blockId.getPath().replace("_slat_screen", "_slats"));
        return new ItemStack(BuiltInRegistries.ITEM.get(itemId));
    }
}
