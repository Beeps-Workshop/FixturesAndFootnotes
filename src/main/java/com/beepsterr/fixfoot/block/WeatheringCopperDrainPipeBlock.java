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
 * The unwaxed downpipe, which ages copper-green over time; the waxed twins are {@link CopperDrainPipeBlock}
 * itself. The stage-to-stage chain is registered per loader — see {@link WeatheringCopperBasinBlock}.
 */
public class WeatheringCopperDrainPipeBlock extends CopperDrainPipeBlock implements WeatheringCopper {
    public static final MapCodec<WeatheringCopperDrainPipeBlock> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state")
                            .forGetter(ChangeOverTimeBlock::getAge),
                    propertiesCodec()
            ).apply(instance, WeatheringCopperDrainPipeBlock::new));

    private final WeatheringCopper.WeatherState weatherState;

    public WeatheringCopperDrainPipeBlock(WeatheringCopper.WeatherState weatherState, Properties properties) {
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
