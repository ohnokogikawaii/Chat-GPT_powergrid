/*
package org.patryk3211.powergrid.kinetics.speedcontroller;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;

import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class GridSpeedControllerRenderer
        extends SmartBlockEntityRenderer<
        GridSpeedControllerBlockEntity> {

    public GridSpeedControllerRenderer(
            BlockEntityRendererProvider.Context context) {

        super(context);
    }

    @Override
    protected void renderSafe(
            GridSpeedControllerBlockEntity blockEntity,
            float partialTicks,
            PoseStack ms,
            MultiBufferSource buffer,
            int light,
            int overlay) {

        super.renderSafe(
                blockEntity,
                partialTicks,
                ms,
                buffer,
                light,
                overlay
        );

        VertexConsumer builder =
                buffer.getBuffer(
                        Sheets.solidBlockSheet()
                );

        */
/*
         * Create Speed Controller内部の
         * 回転シャフト。
         *
         * Flywheelがvisualizationを使用していない場合。
         *//*

        if (!VisualizationManager
                .supportsVisualization(
                        blockEntity.getLevel()
                )) {

            KineticBlockEntityRenderer
                    .renderRotatingBuffer(
                            blockEntity,
                            getRotatedModel(
                                    blockEntity
                            ),
                            ms,
                            builder,
                            light
                    );
        }

        */
/*
         * 上部大歯車用ブラケット。
         *//*

        if (!blockEntity.hasBracket())
            return;

        BlockPos pos =
                blockEntity.getBlockPos();

        Level world =
                blockEntity.getLevel();

        BlockState blockState =
                blockEntity.getBlockState();

        boolean alongX =
                blockState
                        .getValue(
                                GridSpeedControllerBlock
                                        .HORIZONTAL_AXIS
                        )
                        == Axis.X;

        SuperByteBuffer bracket =
                CachedBuffers.partial(
                        AllPartialModels
                                .SPEED_CONTROLLER_BRACKET,
                        blockState
                );

        bracket.translate(
                0,
                1,
                0
        );

        bracket.rotateCentered(
                (float) (
                        alongX
                                ? Math.PI
                                : Math.PI / 2
                ),
                Direction.UP
        );

        bracket.light(
                LevelRenderer.getLightColor(
                        world,
                        pos.above()
                )
        );

        bracket.renderInto(
                ms,
                builder
        );
    }

    private SuperByteBuffer getRotatedModel(
            GridSpeedControllerBlockEntity blockEntity) {

        return CachedBuffers.block(
                KineticBlockEntityRenderer.KINETIC_BLOCK,
                KineticBlockEntityRenderer.shaft(
                        KineticBlockEntityRenderer
                                .getRotationAxisOf(
                                        blockEntity
                                )
                )
        );
    }
}*/
