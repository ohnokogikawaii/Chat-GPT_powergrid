package org.patryk3211.powergrid.electricity.converter;

import com.simibubi.create.foundation.block.IBE;
import dev.architectury.registry.menu.MenuRegistry;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;
import org.patryk3211.powergrid.electricity.base.terminals.BlockStateTerminalCollection;

public class CommercialPowerConditionerBlock
        extends ElectricBlock
        implements IBE<CommercialPowerConditionerBlockEntity> {

    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    /*
     * 基準方向:
     *
     * SOUTH = Blockbenchモデルそのまま
     *
     * プレイヤー → 正面 → 背面 → 壁
     *
     * となるようにする。
     */

    private static final TerminalBoundingBox[] TERMINALS = {

            // 0 = PV1+
            new TerminalBoundingBox(
                    IDecoratedTerminal.POSITIVE,
                    9, 0, 13,
                    10, 1, 14
            ).withColor(IDecoratedTerminal.RED),

            // 1 = PV1-
            new TerminalBoundingBox(
                    IDecoratedTerminal.NEGATIVE,
                    7, 0, 13,
                    8, 1, 14
            ).withColor(IDecoratedTerminal.BLUE),

            // 2 = PV2+
            new TerminalBoundingBox(
                    IDecoratedTerminal.POSITIVE,
                    13, 0, 13,
                    14, 1, 14
            ).withColor(IDecoratedTerminal.RED),

            // 3 = PV2-
            new TerminalBoundingBox(
                    IDecoratedTerminal.NEGATIVE,
                    11, 0, 13,
                    12, 1, 14
            ).withColor(IDecoratedTerminal.BLUE),

            // 4 = PV3+
            new TerminalBoundingBox(
                    IDecoratedTerminal.POSITIVE,
                    15, 9, 13,
                    16, 10, 14
            ).withColor(IDecoratedTerminal.RED),

            // 5 = PV3-
            new TerminalBoundingBox(
                    IDecoratedTerminal.NEGATIVE,
                    15, 6, 13,
                    16, 7, 14
            ).withColor(IDecoratedTerminal.BLUE),

            // 6 = PV4+
            new TerminalBoundingBox(
                    IDecoratedTerminal.POSITIVE,
                    0, 9, 13,
                    1, 10, 14
            ).withColor(IDecoratedTerminal.RED),

            // 7 = PV4-
            new TerminalBoundingBox(
                    IDecoratedTerminal.NEGATIVE,
                    0, 6, 13,
                    1, 7, 14
            ).withColor(IDecoratedTerminal.BLUE),

            // 8 = PV5+
            new TerminalBoundingBox(
                    IDecoratedTerminal.POSITIVE,
                    9, 15, 13,
                    10, 16, 14
            ).withColor(IDecoratedTerminal.RED),

            // 9 = PV5-
            new TerminalBoundingBox(
                    IDecoratedTerminal.NEGATIVE,
                    6, 15, 13,
                    7, 16, 14
            ).withColor(IDecoratedTerminal.BLUE),

            // 10 = GRID+
            new TerminalBoundingBox(
                    IDecoratedTerminal.POSITIVE,
                    4, 0, 13,
                    5, 1, 14
            ).withColor(IDecoratedTerminal.RED),

            // 11 = GRID-
            new TerminalBoundingBox(
                    IDecoratedTerminal.NEGATIVE,
                    2, 0, 13,
                    3, 1, 14
            ).withColor(IDecoratedTerminal.BLUE)
    };

    /*
     * SOUTH方向を基準とした本体形状。
     */
    private static final VoxelShape BASE_SHAPE = box(
            1,
            1,
            12,
            15,
            15,
            16
    );

    public CommercialPowerConditionerBlock(Properties properties) {
        super(properties);

        var shaper = VoxelShaper.forHorizontal(
                BASE_SHAPE,
                Direction.SOUTH
        );

        setTerminalCollection(
                BlockStateTerminalCollection.builder(this)

                        /*
                         * FACINGごとに端子を回転
                         */
                        .forAllStates(state -> {

                            Direction facing =
                                    state.getValue(FACING);

                            return BlockStateTerminalCollection.each(
                                    TERMINALS,
                                    terminal -> rotateTerminal(
                                            terminal,
                                            facing
                                    )
                            );
                        })

                        /*
                         * FACINGごとに当たり判定を回転
                         */
                        .withShapeMapper(
                                state -> shaper.get(
                                        state.getValue(FACING)
                                )
                        )

                        .build()
        );
    }

    private static TerminalBoundingBox rotateTerminal(
            TerminalBoundingBox terminal,
            Direction facing
    ) {
        return switch (facing) {

            /*
             * Blockbenchで作った基準方向
             */
            case SOUTH -> terminal;

            /*
             * South → West
             */
            case WEST -> terminal.rotateAroundY(90);

            /*
             * South → North
             */
            case NORTH -> terminal.rotateAroundY(180);

            /*
             * South → East
             */
            case EAST -> terminal.rotateAroundY(270);

            default -> terminal;
        };
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {

        /*
         * プレイヤーの視線方向をそのまま正面にする。
         *
         * プレイヤー → 正面 → 背面 → 壁
         */
        return defaultBlockState()
                .setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    public Class<CommercialPowerConditionerBlockEntity>
    getBlockEntityClass() {
        return CommercialPowerConditionerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CommercialPowerConditionerBlockEntity>
    getBlockEntityType() {
        return ModdedBlockEntities
                .COMMERCIAL_POWER_CONDITIONER
                .get();
    }
    @Override
    public InteractionResult use(
            BlockState state,
            Level world,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {

        if (world.isClientSide)
            return InteractionResult.SUCCESS;

        if (!(player instanceof ServerPlayer serverPlayer))
            return InteractionResult.SUCCESS;

        withBlockEntityDo(
                world,
                pos,
                be -> MenuRegistry.openExtendedMenu(
                        serverPlayer,
                        be,
                        be::sendToMenu
                )
        );

        return InteractionResult.SUCCESS;
    }

}