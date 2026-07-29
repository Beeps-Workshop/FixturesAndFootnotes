package com.beepsterr.fixfoot.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The unwaxed basin, which ages copper-green over time. Same shape as {@link CopperBasinBlock};
 * the waxed variants are the plain class.
 *
 * <p>The stage-to-stage chain itself isn't declared here — it's per-loader: NeoForge reads the
 * {@code neoforge:oxidizables} / {@code neoforge:waxables} data maps out of the mod's resources,
 * and Fabric registers the pairs through {@code OxidizableBlocksRegistry} at init. Both feed the
 * same {@link WeatheringCopper#getNext} lookup used below, so scraping with an axe and waxing with
 * a honeycomb come along for free.
 */
public class WeatheringCopperBasinBlock extends CopperBasinBlock implements WeatheringCopper {
    public static final MapCodec<WeatheringCopperBasinBlock> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state")
                            .forGetter(ChangeOverTimeBlock::getAge),
                    propertiesCodec()
            ).apply(instance, WeatheringCopperBasinBlock::new));

    private final WeatheringCopper.WeatherState weatherState;

    public WeatheringCopperBasinBlock(WeatheringCopper.WeatherState weatherState, Properties properties) {
        super(properties);
        this.weatherState = weatherState;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public WeatheringCopper.WeatherState getAge() {
        return weatherState;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        changeOverTime(state, level, pos, random);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return WeatheringCopper.getNext(state.getBlock()).isPresent();
    }
}
