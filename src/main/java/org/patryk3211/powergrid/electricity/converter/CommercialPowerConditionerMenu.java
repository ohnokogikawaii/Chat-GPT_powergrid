package org.patryk3211.powergrid.electricity.converter;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import org.patryk3211.powergrid.collections.ModdedMenus;

public class CommercialPowerConditionerMenu
        extends AbstractContainerMenu {

    /*
     * =========================================================
     * 設定範囲
     * =========================================================
     */

    public static final double MIN_OUTPUT_VOLTAGE = 100.0;

    public static final double MAX_OUTPUT_VOLTAGE = 6600.0;

    public static final double OUTPUT_VOLTAGE_STEP = 10.0;


    /*
     * =========================================================
     * BlockEntity
     * =========================================================
     */

    private final CommercialPowerConditionerBlockEntity blockEntity;


    /*
     * =========================================================
     * サーバー側コンストラクタ
     * =========================================================
     */

    public CommercialPowerConditionerMenu(
            MenuType<?> type,
            int containerId,
            Inventory playerInventory,
            CommercialPowerConditionerBlockEntity blockEntity
    ) {

        super(
                type,
                containerId
        );

        this.blockEntity =
                blockEntity;
    }


    /*
     * =========================================================
     * クライアント側コンストラクタ
     * =========================================================
     *
     * BlockEntityの位置を受け取る。
     *
     * Screen側から必要になった場合に使用できる。
     */

    public CommercialPowerConditionerMenu(
            MenuType<?> type,
            int containerId,
            Inventory playerInventory,
            FriendlyByteBuf buffer
    ) {

        this(
                type,
                containerId,
                playerInventory,
                getBlockEntityFromBuffer(
                        playerInventory,
                        buffer
                )
        );
    }


    /*
     * =========================================================
     * BufferからBlockEntity取得
     * =========================================================
     */

    private static CommercialPowerConditionerBlockEntity
    getBlockEntityFromBuffer(
            Inventory inventory,
            FriendlyByteBuf buffer
    ) {

        return (CommercialPowerConditionerBlockEntity)
                inventory.player.level().getBlockEntity(
                        buffer.readBlockPos()
                );
    }


    /*
     * =========================================================
     * BlockEntity
     * =========================================================
     */

    public CommercialPowerConditionerBlockEntity
    getBlockEntity() {

        return blockEntity;
    }


    /*
     * =========================================================
     * 系統連系ON/OFF
     * =========================================================
     */

    public boolean isGridTieEnabled() {

        if (blockEntity == null)
            return false;

        return blockEntity.isGridTieEnabled();
    }


    public void setGridTieEnabled(
            boolean enabled
    ) {

        if (blockEntity == null)
            return;

        blockEntity.setGridTieEnabled(
                enabled
        );
    }


    /*
     * =========================================================
     * 手動出力電圧
     * =========================================================
     */

    public double getManualOutputVoltage() {

        if (blockEntity == null)
            return MIN_OUTPUT_VOLTAGE;

        return clampOutputVoltage(
                blockEntity.getManualOutputVoltage()
        );
    }


    public void setManualOutputVoltage(
            double voltage
    ) {

        if (blockEntity == null)
            return;

        voltage =
                clampOutputVoltage(
                        voltage
                );

        blockEntity.setManualOutputVoltage(
                voltage
        );
    }


    /*
     * =========================================================
     * 電圧増加
     * =========================================================
     */

    public void increaseOutputVoltage() {

        double voltage =
                getManualOutputVoltage();

        voltage +=
                OUTPUT_VOLTAGE_STEP;

        setManualOutputVoltage(
                voltage
        );
    }


    /*
     * =========================================================
     * 電圧減少
     * =========================================================
     */

    public void decreaseOutputVoltage() {

        double voltage =
                getManualOutputVoltage();

        voltage -=
                OUTPUT_VOLTAGE_STEP;

        setManualOutputVoltage(
                voltage
        );
    }


    /*
     * =========================================================
     * 最大電圧
     * =========================================================
     */

    public void setMaximumOutputVoltage() {

        setManualOutputVoltage(
                MAX_OUTPUT_VOLTAGE
        );
    }


    /*
     * =========================================================
     * 最小電圧
     * =========================================================
     */

    public void setMinimumOutputVoltage() {

        setManualOutputVoltage(
                MIN_OUTPUT_VOLTAGE
        );
    }


    /*
     * =========================================================
     * 電圧を100～6600Vに制限し、10V単位に丸める
     * =========================================================
     */

    public static double clampOutputVoltage(
            double voltage
    ) {

        if (!Double.isFinite(voltage))
            return MIN_OUTPUT_VOLTAGE;

        voltage =
                Math.max(
                        MIN_OUTPUT_VOLTAGE,
                        Math.min(
                                MAX_OUTPUT_VOLTAGE,
                                voltage
                        )
                );

        /*
         * 10V単位に丸める。
         */

        voltage =
                Math.round(
                        voltage
                                /
                                OUTPUT_VOLTAGE_STEP
                )
                        *
                        OUTPUT_VOLTAGE_STEP;

        return Math.max(
                MIN_OUTPUT_VOLTAGE,
                Math.min(
                        MAX_OUTPUT_VOLTAGE,
                        voltage
                )
        );
    }


    /*
     * =========================================================
     * 実際の目標出力電圧
     * =========================================================
     */

    public double getTargetOutputVoltage() {

        if (blockEntity == null)
            return 0.0;

        return blockEntity.getTargetOutputVoltage();
    }


    /*
     * =========================================================
     * 系統電圧
     * =========================================================
     */

    public double getGridVoltage() {

        if (blockEntity == null)
            return 0.0;

        return blockEntity.getGridVoltage();
    }


    /*
     * =========================================================
     * 系統電流
     * =========================================================
     */

    public double getGridCurrent() {

        if (blockEntity == null)
            return 0.0;

        return blockEntity.getGridCurrent();
    }


    /*
     * =========================================================
     * 系統出力
     * =========================================================
     */

    public double getGridPower() {

        if (blockEntity == null)
            return 0.0;

        return blockEntity.getOutputPower();
    }


    /*
     * =========================================================
     * DC LINK
     * =========================================================
     */

    public double getDcLinkVoltage() {

        if (blockEntity == null)
            return 0.0;

        return blockEntity.getDcLinkVoltage();
    }


    /*
     * =========================================================
     * 系統接続状態
     * =========================================================
     */

    public boolean isGridConnected() {

        if (blockEntity == null)
            return false;

        return blockEntity.isGridConnected();
    }


    /*
     * =========================================================
     * PV総電力
     * =========================================================
     */

    public double getTotalAvailablePower() {

        if (blockEntity == null)
            return 0.0;

        return blockEntity.getTotalAvailablePower();
    }


    /*
     * =========================================================
     * MPPT
     * =========================================================
     */

    public double getMpptPower(
            int index
    ) {

        if (blockEntity == null)
            return 0.0;

        return blockEntity.getMpptPower(
                index
        );
    }


    public double getMpptVoltage(
            int index
    ) {

        if (blockEntity == null)
            return 0.0;

        return blockEntity.getMpptVoltage(
                index
        );
    }


    public double getMpptCurrent(
            int index
    ) {

        if (blockEntity == null)
            return 0.0;

        return blockEntity.getMpptCurrent(
                index
        );
    }


    public double getMpptMaximumPower(
            int index
    ) {

        if (blockEntity == null)
            return 0.0;

        return blockEntity.getMpptMaximumPower(
                index
        );
    }


    public double getMpptMaximumVoltage(
            int index
    ) {

        if (blockEntity == null)
            return 0.0;

        return blockEntity.getMpptMaximumVoltage(
                index
        );
    }


    public double getMpptMaximumCurrent(
            int index
    ) {

        if (blockEntity == null)
            return 0.0;

        return blockEntity.getMpptMaximumCurrent(
                index
        );
    }


    /*
     * =========================================================
     * プレイヤーインベントリ
     * =========================================================
     *
     * このMenuではアイテムスロットを使用しない。
     */

    @Override
    public ItemStack quickMoveStack(
            Player player,
            int index
    ) {

        return ItemStack.EMPTY;
    }


    /*
     * =========================================================
     * Menu有効判定
     * =========================================================
     */

    @Override
    public boolean stillValid(
            Player player
    ) {

        if (blockEntity == null)
            return false;

        return player.distanceToSqr(
                blockEntity.getBlockPos().getX() + 0.5,
                blockEntity.getBlockPos().getY() + 0.5,
                blockEntity.getBlockPos().getZ() + 0.5
        ) <= 64.0;
    }


    /*
     * =========================================================
     * Menu登録用
     * =========================================================
     *
     * BlockEntity側では
     *
     * ModdedMenus.COMMERCIAL_POWER_CONDITIONER.get()
     *
     * を使用する。
     */
}

