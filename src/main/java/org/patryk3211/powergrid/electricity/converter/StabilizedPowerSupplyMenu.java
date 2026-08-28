package org.patryk3211.powergrid.electricity.converter;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.patryk3211.powergrid.collections.ModdedMenus;

public class StabilizedPowerSupplyMenu
        extends AbstractContainerMenu {

    public final StabilizedPowerSupplyBlockEntity contentHolder;

    public StabilizedPowerSupplyMenu(
            MenuType<?> type,
            int id,
            Inventory inventory,
            FriendlyByteBuf extraData
    ) {

        super(
                type,
                id
        );

        var pos =
                extraData.readBlockPos();

        BlockEntity be =
                inventory.player
                        .level()
                        .getBlockEntity(pos);

        if (
                be instanceof
                        StabilizedPowerSupplyBlockEntity stabilizer
        ) {

            contentHolder =
                    stabilizer;

        } else {

            throw new IllegalStateException(
                    "Stabilized Power Supply BlockEntity not found at "
                            + pos
            );
        }



    }

    public StabilizedPowerSupplyMenu(
            MenuType<?> type,
            int id,
            Inventory inventory,
            StabilizedPowerSupplyBlockEntity blockEntity
    ) {

        super(
                type,
                id
        );

        contentHolder =
                blockEntity;


    }

    public static StabilizedPowerSupplyMenu create(
            int id,
            Inventory inventory,
            StabilizedPowerSupplyBlockEntity blockEntity
    ) {

        return new StabilizedPowerSupplyMenu(
                ModdedMenus.STABILIZED_POWER_SUPPLY.get(),
                id,
                inventory,
                blockEntity
        );
    }



    @Override
    public ItemStack quickMoveStack(
            Player player,
            int index
    ) {

        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(
            Player player
    ) {

        return
                player.distanceToSqr(
                        contentHolder.getBlockPos()
                                .getCenter()
                )
                        <=
                        64.0;
    }
}