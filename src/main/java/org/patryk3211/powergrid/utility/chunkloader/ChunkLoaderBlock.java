package org.patryk3211.powergrid.utility.chunkloader;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ChunkLoaderBlock extends Block {

    public ChunkLoaderBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            forceChunk(serverLevel, pos, true);
        }
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!level.isClientSide
                && level instanceof ServerLevel serverLevel
                && !state.is(newState.getBlock())) {

            forceChunk(serverLevel, pos, false);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static void forceChunk(
            ServerLevel level,
            BlockPos pos,
            boolean forced
    ) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        level.setChunkForced(chunkX, chunkZ, forced);
    }
}