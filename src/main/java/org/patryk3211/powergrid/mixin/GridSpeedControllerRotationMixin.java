/*
package org.patryk3211.powergrid.mixin;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;

import org.patryk3211.powergrid.collections.ModdedBlocks;
import org.patryk3211.powergrid.kinetics.speedcontroller.GridSpeedControllerBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RotationPropagator.class)
public class GridSpeedControllerRotationMixin {

    */
/*
     * Create 6.0.7:
     *
     * private static boolean
     * isLargeCogToSpeedController(
     *     BlockState from,
     *     BlockState to,
     *     BlockPos diff)
     *
     * にGrid版を追加する。
     *//*

    @Inject(
            method = "isLargeCogToSpeedController",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void powergrid$gridSpeedController(
            BlockState from,
            BlockState to,
            BlockPos diff,
            CallbackInfoReturnable<Boolean> cir) {

        */
/*
         * Grid Speed Controller以外は
         * Create本来の処理へ。
         *//*

        if (!ModdedBlocks
                .GRID_SPEED_CONTROLLER
                .has(to)) {

            return;
        }

        */
/*
         * 大歯車でなければ接続不可。
         *//*

        if (!ICogWheel.isLargeCog(from)) {

            cir.setReturnValue(false);
            return;
        }

        */
/*
         * Createと同じ。
         *
         * 大歯車はコントローラーの真上。
         *
         * from = 大歯車
         * to   = Grid Controller
         *
         * なので差分はDOWN。
         *//*

        if (!diff.equals(
                BlockPos.ZERO.below()
        )) {

            cir.setReturnValue(false);
            return;
        }

        Axis cogAxis =
                from.getValue(
                        CogWheelBlock.AXIS
                );

        */
/*
         * 垂直大歯車は不可。
         *//*

        if (cogAxis.isVertical()) {

            cir.setReturnValue(false);
            return;
        }

        */
/*
         * Grid Controller側も
         * HorizontalAxisを持っていることを確認。
         *//*

        if (!to.hasProperty(
                HorizontalAxisKineticBlock
                        .HORIZONTAL_AXIS
        )) {

            cir.setReturnValue(false);
            return;
        }

        Axis controllerAxis =
                to.getValue(
                        GridSpeedControllerBlock
                                .HORIZONTAL_AXIS
                );

        */
/*
         * 大歯車とコントローラーは
         * 90度でなければならない。
         *//*

        if (controllerAxis == cogAxis) {

            cir.setReturnValue(false);
            return;
        }

        */
/*
         * Grid Speed Controller ↔
         * Large Cog Wheel 接続成立。
         *//*

        cir.setReturnValue(true);
    }
}*/
