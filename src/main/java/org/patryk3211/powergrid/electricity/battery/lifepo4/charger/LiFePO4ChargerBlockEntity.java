/*package org.patryk3211.powergrid.electricity.battery.lifepo4.charger;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.sim.node.DcDcConverterCoupling;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

import java.util.List;

public class LiFePO4ChargerBlockEntity
        extends ElectricBlockEntity
        implements IHaveGoggleInformation {

    /*
     * =========================================
     * DCDCコンバーター
     * =========================================
     */
    /*private DcDcConverterCoupling converter;

    /*
     * =========================================
     * 端子
     *
     * 0 INPUT+
     * 1 INPUT-
     * 2 BATTERY+
     * 3 BATTERY-
     * 4 SENSE+
     * 5 SENSE-
     * =========================================
     */
    /*private IElectricNode inputPlusNode;
    private IElectricNode inputMinusNode;

    private IElectricNode batteryPlusNode;
    private IElectricNode batteryMinusNode;

    private IElectricNode sensePlusNode;
    private IElectricNode senseMinusNode;

    /*
     * =========================================
     * 表示用電気値
     * =========================================
     */
    /*private double inputVoltage;
    private double inputCurrent;
    private double inputPower;

    private double batteryVoltage;

    private double outputVoltage;
    private double outputCurrent;
    private double outputPower;

    /*
     * =========================================
     * LiFePO4 16S
     *
     * 16セル × 3.65V
     * = 58.4V
     * =========================================
     */
    /*private static final double MAX_VOLTAGE = 58.4;

    /*
     * =========================================
     * バッテリー電圧との差
     *
     * 0.03V / A
     *
     * 例：
     *
     * 0.03V → 1A
     * 0.30V → 10A
     * 3.00V → 100A
     * =========================================
     */
    /*private static final double CHARGE_VOLTAGE_DROP_PER_AMP = 0.03;

    /*
     * =========================================
     * DCDC効率
     * =========================================
     */
    /*private static final double EFFICIENCY = 0.98;
    /*
     * 充電器側の最大出力電流
     *
     * ケーブルの最大電流は1280Aだが、
     * 充電器自体は1000Aまでに制限する。
     */
    /*private static final double MAX_OUTPUT_CURRENT = 1000.0;
    /*
     * =========================================
     * 最小入力電圧
     *
     * 0V以下は扱わない。
     * =========================================
     */
    /*private static final double MIN_VOLTAGE = 0.0;

    public LiFePO4ChargerBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }

    /*
     * =========================================
     * 回路構築
     * =========================================
     */
    /*@Override
    public void buildCircuit(
            CircuitBuilder builder
    ) {

        builder.setTerminalCount(6);

        /*
         * INPUT
         */
        /*inputPlusNode =
                builder.terminalNode(0);

        inputMinusNode =
                builder.terminalNode(1);

        /*
         * BATTERY
         */
        /*batteryPlusNode =
                builder.terminalNode(2);

        batteryMinusNode =
                builder.terminalNode(3);

        /*
         * VOLTAGE SENSE
         */
        /*sensePlusNode =
                builder.terminalNode(4);

        senseMinusNode =
                builder.terminalNode(5);

        /*
         * =========================================
         * DCDCコンバーター
         * =========================================
         */
        /*converter =
                builder.addInternalNode(
                        DcDcConverterCoupling.class,
                        inputPlusNode,
                        inputMinusNode,
                        batteryPlusNode,
                        batteryMinusNode
                );

        /*
         * 効率98%
         */
        /*converter.setEfficiency(
                EFFICIENCY
        );

        converter.setMaxOutputCurrent(
                MAX_OUTPUT_CURRENT
        );

        converter.setOutputVoltage(
                MAX_VOLTAGE
        );

        converter.setOutputCurrent(
                0.0
        );

        /*
         * =========================================
         * 電圧センサー
         * =========================================
         *
         * センス端子はバッテリー端子へ
         * 高抵抗で接続する。
         */
        /*builder.connect(
                1_000_000f,
                sensePlusNode,
                batteryPlusNode
        );

        builder.connect(
                1_000_000f,
                senseMinusNode,
                batteryMinusNode
        );
    }

    /*
     * =========================================
     * 電気シミュレーション
     * =========================================
     */
    /*@Override
    public void electricalTick() {

        if (converter == null)
            return;

        /*
         * =========================================
         * 入力電圧
         * =========================================
         */
        /*if (
                inputPlusNode != null
                        &&
                        inputMinusNode != null
        ) {

            inputVoltage =
                    inputPlusNode.getVoltage()
                            -
                            inputMinusNode.getVoltage();

        } else {

            inputVoltage = 0.0;
        }

        inputVoltage =
                Math.max(
                        MIN_VOLTAGE,
                        inputVoltage
                );

        /*
         * =========================================
         * バッテリー電圧
         * =========================================
         */
        /*if (
                batteryPlusNode != null
                        &&
                        batteryMinusNode != null
        ) {

            batteryVoltage =
                    batteryPlusNode.getVoltage()
                            -
                            batteryMinusNode.getVoltage();

        } else {

            batteryVoltage = 0.0;
        }

        batteryVoltage =
                Math.max(
                        MIN_VOLTAGE,
                        batteryVoltage
                );

        /*
         * =========================================
         * 充電電圧決定
         * =========================================
         *
         * ケース1
         *
         * 入力電圧 > バッテリー電圧
         *
         * → 入力電圧をそのまま印加
         *
         * ここには最大電圧制限を設けない。
         *
         * 100V → 100V
         * 500V → 500V
         * 5000V → 5000V
         *
         * =========================================
         *
         * ケース2
         *
         * 入力電圧 <= バッテリー電圧
         *
         * → バッテリー電圧より0.03V高くする。
         *
         * これにより1A相当の充電電流になる。
         * =========================================
         */
        /*double targetVoltage;

        if (
                inputVoltage >
                        batteryVoltage
        ) {

            /*
             * 入力電圧を直接印加
             */
            /*targetVoltage =
                    inputVoltage;

        } else {

            /*
             * バッテリー電圧より少し高くする
             */
            /*targetVoltage =
                    batteryVoltage
                            + CHARGE_VOLTAGE_DROP_PER_AMP;
        }

        /*
         * =========================================
         * DCDC設定
         * =========================================
         */
        /*converter.setEfficiency(
                EFFICIENCY
        );

        converter.setMaxOutputCurrent(
                MAX_OUTPUT_CURRENT
        );

        converter.setOutputVoltage(
                targetVoltage
        );

        /*
         * =========================================
         * バッテリー電圧から
         * 充電電流を計算
         * =========================================
         */
        /*converter.updateFromBattery(
                batteryVoltage,
                CHARGE_VOLTAGE_DROP_PER_AMP
        );

        /*
         * =========================================
         * 表示値更新
         * =========================================
         */
        /*inputCurrent =
                converter.getInputCurrent();

        inputPower =
                converter.getInputPower();

        outputVoltage =
                converter.getOutputVoltage();

        outputCurrent =
                converter.getOutputCurrent();

        outputPower =
                converter.getOutputPower();
    }

    /*
     * =========================================
     * Goggles表示
     * =========================================
     */
    /*@Override
    public boolean addToGoggleTooltip(
            List<Component> tooltip,
            boolean sneaking
    ) {

        tooltip.add(
                Component.literal(
                        "LiFePO4 Charger"
                ).withStyle(
                        ChatFormatting.GOLD
                )
        );

        tooltip.add(
                Component.literal(
                        String.format(
                                "Input Voltage: %.2fV",
                                inputVoltage
                        )
                )
        );

        tooltip.add(
                Component.literal(
                        String.format(
                                "Input Current: %.2fA",
                                inputCurrent
                        )
                )
        );

        tooltip.add(
                Component.literal(
                        String.format(
                                "Input Power: %.1fW",
                                inputPower
                        )
                )
        );

        tooltip.add(
                Component.literal(
                        String.format(
                                "Battery Voltage: %.2fV",
                                batteryVoltage
                        )
                )
        );

        tooltip.add(
                Component.literal(
                        String.format(
                                "Output Voltage: %.2fV",
                                outputVoltage
                        )
                )
        );

        tooltip.add(
                Component.literal(
                        String.format(
                                "Output Current: %.2fA",
                                outputCurrent
                        )
                )
        );

        tooltip.add(
                Component.literal(
                        String.format(
                                "Output Power: %.1fW",
                                outputPower
                        )
                )
        );

        return true;
    }
}*/