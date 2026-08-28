package org.patryk3211.powergrid.electricity.converter;

import com.simibubi.create.foundation.block.IBE;
import dev.architectury.registry.menu.MenuRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;

import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.HorizontalAxisElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

public class StabilizedPowerSupplyBlock
        extends HorizontalAxisElectricBlock
        implements IBE<StabilizedPowerSupplyBlockEntity> {

    private static final TerminalBoundingBox[] TERMINALS = {

            /*
             * INPUT +
             */
            new TerminalBoundingBox(
                    IDecoratedTerminal.POSITIVE,
                    3, 4, 0,
                    7, 10, 2
            ).withColor(
                    IDecoratedTerminal.RED
            ),

            /*
             * INPUT -
             */
            new TerminalBoundingBox(
                    IDecoratedTerminal.NEGATIVE,
                    9, 4, 0,
                    13, 10, 2
            ).withColor(
                    IDecoratedTerminal.BLUE
            ),

            /*
             * OUTPUT +
             */
            new TerminalBoundingBox(
                    IDecoratedTerminal.POSITIVE,
                    3, 4, 14,
                    7, 10, 16
            ).withColor(
                    IDecoratedTerminal.RED
            ),

            /*
             * OUTPUT -
             */
            new TerminalBoundingBox(
                    IDecoratedTerminal.NEGATIVE,
                    9, 4, 14,
                    13, 10, 16
            ).withColor(
                    IDecoratedTerminal.BLUE
            )
    };

    public StabilizedPowerSupplyBlock(Properties properties) {
        super(properties);

        setTerminalCollection(
                horizontalZTerminals(
                        this,
                        TERMINALS,
                        box(0, 0, 0, 16, 12, 16)
                )
        );
    }

    @Override
    public Class<StabilizedPowerSupplyBlockEntity> getBlockEntityClass() {
        return StabilizedPowerSupplyBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends StabilizedPowerSupplyBlockEntity>
    getBlockEntityType() {
        return ModdedBlockEntities.STABILIZED_POWER_SUPPLY.get();
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

        withBlockEntityDo(
                world,
                pos,
                be -> MenuRegistry.openExtendedMenu(
                        (ServerPlayer) player,
                        be,
                        be::sendToMenu
                )
        );

        return InteractionResult.SUCCESS;
    }
}