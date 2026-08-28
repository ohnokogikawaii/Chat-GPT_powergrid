/*
package org.patryk3211.powergrid.kinetics.speedcontroller;

import java.util.function.Predicate;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;

import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;
import org.patryk3211.powergrid.kinetics.base.ElectricKineticBlock;

public class GridSpeedControllerBlock
        extends ElectricKineticBlock
        implements IBE<GridSpeedControllerBlockEntity> {

    */
/*
     * CreateのHorizontalAxisKineticBlockと
     * 完全に同じPropertyを使用する。
     *
     * 重要：
     * 独自のAXISを作ってはいけない。
     *//*

    public static final Property<Axis> HORIZONTAL_AXIS =
            BlockStateProperties.HORIZONTAL_AXIS;

    */
/*
     * 上部大歯車設置用
     *//*

    private static final int PLACEMENT_HELPER_ID =
            PlacementHelpers.register(
                    new GridSpeedControllerPlacementHelper()
            );

    */
/*
     * ============================================================
     * 電気端子
     * ============================================================
     *
     *       +       -
     *      上面
     *//*


    private static final TerminalBoundingBox[] TOP_TERMINALS = {

            new TerminalBoundingBox(
                    net.minecraft.network.chat.Component.literal("+"),
                    3,
                    14,
                    5,
                    6,
                    16,
                    11
            ),

            new TerminalBoundingBox(
                    net.minecraft.network.chat.Component.literal("-"),
                    10,
                    14,
                    5,
                    13,
                    16,
                    11
            )
    };

    public GridSpeedControllerBlock(
            Properties properties) {

        super(properties);

        */
/*
         * 電気端子
         *//*

        setTerminalCollection(
                BlockStateTerminalCollection
                        .builder(this)
                        .forAllStates(
                                state -> TOP_TERMINALS
                        )
                        .withShapeMapper(
                                state -> AllShapes.SPEED_CONTROLLER
                        )
                        .build()
        );
    }

    */
/*
     * ============================================================
     * BlockState
     * ============================================================
     *//*


    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {

        */
/*
         * Createと同じHORIZONTAL_AXISを登録。
         *//*

        builder.add(HORIZONTAL_AXIS);

        super.createBlockStateDefinition(builder);
    }

    */
/*
     * ============================================================
     * 設置
     * ============================================================
     *//*


    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context) {

        */
/*
         * 真上に既に大歯車がある場合。
         *
         * 大歯車とコントローラーの軸を90度にする。
         *//*

        BlockState above =
                context.getLevel()
                        .getBlockState(
                                context.getClickedPos().above()
                        );

        if (ICogWheel.isLargeCog(above)
                && above.hasProperty(CogWheelBlock.AXIS)
                && above.getValue(CogWheelBlock.AXIS)
                .isHorizontal()) {

            Axis cogAxis =
                    above.getValue(
                            CogWheelBlock.AXIS
                    );

            Axis controllerAxis =
                    cogAxis == Axis.X
                            ? Axis.Z
                            : Axis.X;

            return defaultBlockState()
                    .setValue(
                            HORIZONTAL_AXIS,
                            controllerAxis
                    );
        }

        */
/*
         * CreateのHorizontalAxisKineticBlockと同じ
         * 周囲のshaftを優先する。
         *//*

        Axis preferredAxis =
                HorizontalAxisKineticBlock
                        .getPreferredHorizontalAxis(
                                context
                        );

        if (preferredAxis != null) {

            return defaultBlockState()
                    .setValue(
                            HORIZONTAL_AXIS,
                            preferredAxis
                    );
        }

        */
/*
         * Createと同じデフォルト。
         *//*

        return defaultBlockState()
                .setValue(
                        HORIZONTAL_AXIS,
                        context
                                .getHorizontalDirection()
                                .getClockWise()
                                .getAxis()
                );
    }

    */
/*
     * ============================================================
     * 回転
     * ============================================================
     *//*


    @Override
    public BlockState rotate(
            BlockState state,
            Rotation rotation) {

        Axis axis =
                state.getValue(HORIZONTAL_AXIS);

        return state.setValue(
                HORIZONTAL_AXIS,
                rotation
                        .rotate(
                                Direction.get(
                                        Direction.AxisDirection.POSITIVE,
                                        axis
                                )
                        )
                        .getAxis()
        );
    }

    @Override
    public BlockState mirror(
            BlockState state,
            Mirror mirror) {

        return state;
    }

    */
/*
     * ============================================================
     * 回転軸
     * ============================================================
     *//*


    @Override
    public Axis getRotationAxis(
            BlockState state) {

        return state.getValue(
                HORIZONTAL_AXIS
        );
    }

    */
/*
     * ============================================================
     * Shaft
     * ============================================================
     *//*


    @Override
    public boolean hasShaftTowards(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            Direction direction) {

        return direction.getAxis()
                == getRotationAxis(state);
    }

    */
/*
     * ============================================================
     * 上部大歯車の変更検知
     * ============================================================
     *//*


    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            BlockPos neighborPos,
            boolean movedByPiston) {

        */
/*
         * Create Speed Controllerと同じ。
         *
         * 上のブロックだけを監視する。
         *//*

        if (neighborPos.equals(pos.above())) {

            withBlockEntityDo(
                    level,
                    pos,
                    GridSpeedControllerBlockEntity
                            ::updateBracket
            );
        }
    }

    */
/*
     * ============================================================
     * 大歯車のPlacementHelper
     * ============================================================
     *//*


    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult ray) {

        ItemStack heldItem =
                player.getItemInHand(hand);

        IPlacementHelper helper =
                PlacementHelpers.get(
                        PLACEMENT_HELPER_ID
                );

        if (helper.matchesItem(heldItem)) {

            return helper
                    .getOffset(
                            player,
                            level,
                            state,
                            pos,
                            ray
                    )
                    .placeInWorld(
                            level,
                            (BlockItem)
                                    heldItem.getItem(),
                            player,
                            hand,
                            ray
                    );
        }

        return InteractionResult.PASS;
    }

    */
/*
     * ============================================================
     * Shape
     * ============================================================
     *//*


    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {

        return AllShapes.SPEED_CONTROLLER;
    }

    */
/*
     * ============================================================
     * Block Entity
     * ============================================================
     *//*


    @Override
    public Class<GridSpeedControllerBlockEntity>
    getBlockEntityClass() {

        return GridSpeedControllerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GridSpeedControllerBlockEntity>
    getBlockEntityType() {

        return ModdedBlockEntities
                .GRID_SPEED_CONTROLLER
                .get();
    }

    */
/*
     * ============================================================
     * PlacementHelper
     * ============================================================
     *//*


    private static class GridSpeedControllerPlacementHelper
            implements IPlacementHelper {

        @Override
        public Predicate<ItemStack>
        getItemPredicate() {

            return ((Predicate<ItemStack>)
                    ICogWheel::isLargeCogItem)
                    .and(
                            ICogWheel::isDedicatedCogItem
                    );
        }

        @Override
        public Predicate<BlockState>
        getStatePredicate() {

            */
/*
             * CreateのSpeed Controllerではなく、
             * Grid版自身を対象にする。
             *//*

            return state ->
                    state.getBlock()
                            instanceof GridSpeedControllerBlock;
        }

        @Override
        public PlacementOffset getOffset(
                Player player,
                Level level,
                BlockState state,
                BlockPos pos,
                BlockHitResult ray) {

            */
/*
             * 大歯車は必ず真上。
             *//*

            BlockPos newPos =
                    pos.above();

            */
/*
             * 既に何かあれば失敗。
             *//*

            if (!level
                    .getBlockState(newPos)
                    .canBeReplaced()) {

                return PlacementOffset.fail();
            }

            */
/*
             * コントローラーと大歯車は90度。
             *//*

            Axis controllerAxis =
                    state.getValue(
                            HORIZONTAL_AXIS
                    );

            Axis cogAxis =
                    controllerAxis == Axis.X
                            ? Axis.Z
                            : Axis.X;

            */
/*
             * Createと同じ干渉チェック。
             *//*

            if (!CogWheelBlock
                    .isValidCogwheelPosition(
                            true,
                            level,
                            newPos,
                            cogAxis
                    )) {

                return PlacementOffset.fail();
            }

            */
/*
             * 大歯車のAXISを直接設定。
             *//*

            return PlacementOffset.success(
                    newPos,
                    s -> s.setValue(
                            CogWheelBlock.AXIS,
                            cogAxis
                    )
            );
        }
    }
}*/
