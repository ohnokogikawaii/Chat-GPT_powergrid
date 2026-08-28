package org.patryk3211.powergrid.network.packets;

import dev.architectury.networking.NetworkManager;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import org.patryk3211.powergrid.electricity.converter.StabilizedPowerSupplyBlockEntity;
import org.patryk3211.powergrid.network.SimplePacket;

import java.util.function.Supplier;

public class StabilizedPowerSupplyC2SPacket
        implements SimplePacket {

    private final BlockPos blockPos;

    private final boolean mpptEnabled;

    private final double outputVoltage;

    private final double currentLimit;

    public StabilizedPowerSupplyC2SPacket(
            BlockPos blockPos,
            boolean mpptEnabled,
            double outputVoltage,
            double currentLimit
    ) {

        this.blockPos =
                blockPos;

        this.mpptEnabled =
                mpptEnabled;

        this.outputVoltage =
                outputVoltage;

        this.currentLimit =
                currentLimit;
    }

    public StabilizedPowerSupplyC2SPacket(
            FriendlyByteBuf buffer
    ) {

        blockPos =
                buffer.readBlockPos();

        mpptEnabled =
                buffer.readBoolean();

        outputVoltage =
                buffer.readDouble();

        currentLimit =
                buffer.readDouble();
    }

    @Override
    public void encode(
            FriendlyByteBuf buffer
    ) {

        buffer.writeBlockPos(
                blockPos
        );

        buffer.writeBoolean(
                mpptEnabled
        );

        buffer.writeDouble(
                outputVoltage
        );

        buffer.writeDouble(
                currentLimit
        );
    }

    @Override
    public void handle(
            Supplier<NetworkManager.PacketContext> context
    ) {

        var ctx =
                context.get();

        ctx.queue(() -> {

            if (
                    !(ctx.getPlayer()
                            instanceof ServerPlayer player)
            )
                return;

            if (
                    player.distanceToSqr(
                            blockPos.getCenter()
                    )
                            >
                            64.0
            )
                return;

            var be =
                    player.level()
                            .getBlockEntity(
                                    blockPos
                            );

            if (
                    !(be instanceof
                            StabilizedPowerSupplyBlockEntity supply)
            )
                return;

            /*
             * サーバー側で値を検証。
             */
            supply.setMpptEnabled(
                    mpptEnabled
            );

            supply.setTargetOutputVoltage(
                    outputVoltage
            );

            supply.setCurrentLimit(
                    currentLimit
            );

            supply.setChanged();

            supply.sendData();
        });
    }
}