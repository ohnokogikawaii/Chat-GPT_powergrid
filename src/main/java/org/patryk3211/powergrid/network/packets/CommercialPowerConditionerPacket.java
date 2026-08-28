package org.patryk3211.powergrid.network.packets;

import dev.architectury.networking.NetworkManager;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.patryk3211.powergrid.electricity.converter.CommercialPowerConditionerBlockEntity;
import org.patryk3211.powergrid.electricity.converter.CommercialPowerConditionerMenu;
import org.patryk3211.powergrid.network.SimplePacket;

import java.util.function.Supplier;

public class CommercialPowerConditionerPacket
        implements SimplePacket {

    private final BlockPos blockPos;

    private final boolean gridTieEnabled;

    private final double outputVoltage;


    /*
     * =========================================================
     * コンストラクタ
     * =========================================================
     */

    public CommercialPowerConditionerPacket(
            BlockPos blockPos,
            boolean gridTieEnabled,
            double outputVoltage
    ) {

        this.blockPos =
                blockPos;

        this.gridTieEnabled =
                gridTieEnabled;

        this.outputVoltage =
                outputVoltage;
    }


    /*
     * =========================================================
     * Client → Server デコード
     * =========================================================
     */

    public CommercialPowerConditionerPacket(
            FriendlyByteBuf buffer
    ) {

        this.blockPos =
                buffer.readBlockPos();

        this.gridTieEnabled =
                buffer.readBoolean();

        this.outputVoltage =
                buffer.readDouble();
    }


    /*
     * =========================================================
     * エンコード
     * =========================================================
     */

    @Override
    public void encode(
            FriendlyByteBuf buffer
    ) {

        buffer.writeBlockPos(
                blockPos
        );

        buffer.writeBoolean(
                gridTieEnabled
        );

        buffer.writeDouble(
                outputVoltage
        );
    }


    /*
     * =========================================================
     * パケット処理
     * =========================================================
     */

    @Override
    public void handle(
            Supplier<NetworkManager.PacketContext> contextSupplier
    ) {

        NetworkManager.PacketContext context =
                contextSupplier.get();

        context.queue(
                () -> {

                    /*
                     * =================================================
                     * 送信元プレイヤー取得
                     * =================================================
                     */

                    if (
                            !(context.getPlayer()
                                    instanceof ServerPlayer player)
                    ) {

                        return;
                    }


                    /*
                     * =================================================
                     * 距離チェック
                     * =================================================
                     *
                     * GUIを開いている装置から
                     * 8ブロック以内のみ操作可能。
                     */

                    if (
                            player.distanceToSqr(
                                    blockPos.getCenter()
                            )
                                    >
                                    64.0
                    ) {

                        return;
                    }


                    /*
                     * =================================================
                     * BlockEntity取得
                     * =================================================
                     */

                    BlockEntity blockEntity =
                            player.level()
                                    .getBlockEntity(
                                            blockPos
                                    );

                    if (
                            !(blockEntity
                                    instanceof
                                    CommercialPowerConditionerBlockEntity conditioner)
                    ) {

                        return;
                    }


                    /*
                     * =================================================
                     * 電圧をサーバー側でも検証
                     * =================================================
                     */

                    double voltage =
                            CommercialPowerConditionerMenu
                                    .clampOutputVoltage(
                                            outputVoltage
                                    );


                    /*
                     * =================================================
                     * 系統連系設定
                     * =================================================
                     */

                    conditioner.setGridTieEnabled(
                            gridTieEnabled
                    );


                    /*
                     * =================================================
                     * 手動出力電圧設定
                     * =================================================
                     */

                    conditioner.setManualOutputVoltage(
                            voltage
                    );


                    /*
                     * =================================================
                     * 保存
                     * =================================================
                     */

                    conditioner.setChanged();

                    conditioner.sendData();
                }
        );
    }
}

