/*
 * Copyright 2025 patryk3211
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.patryk3211.powergrid.electricity.converter;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.patryk3211.powergrid.base.IMultiScreenHandlerFactory;
import org.patryk3211.powergrid.collections.ModdedMenus;
import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.IElectricEntity.CircuitBuilder;
import org.patryk3211.powergrid.electricity.sim.ElectricalNetwork;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.RegulatedTransformerCoupling;
import org.patryk3211.powergrid.electricity.sim.node.VoltageSourceCoupling;

import java.util.List;


/**
 * 安定化電源
 *
 * =========================================================
 * 概要
 * =========================================================
 *
 * INPUT+
 * INPUT-
 *     |
 *     v
 * 内部可変変圧器
 *     |
 *     v
 * 電流制限用直列抵抗
 *     |
 *     v
 * OUTPUT+
 * OUTPUT-
 *
 *
 * 出力電圧は内部変圧器の巻数比によって制御する。
 *
 * 電流制限は出力側の直列抵抗を動的に変更して行う。
 *
 * これにより、
 *
 *   ・通常の抵抗負荷
 *   ・低抵抗負荷
 *   ・バッテリー充電
 *
 * を同じ回路モデルで扱う。
 *
 *
 * =========================================================
 * 逆流保護
 * =========================================================
 *
 * 通常：
 *
 *   内部 -> OUTPUT
 *
 * 逆流：
 *
 *   OUTPUT -> 内部
 *
 * を検出すると出力スイッチをOFFにする。
 *
 * 遮断後は一定時間待機し、
 * 内部電圧が外部電圧より十分高くなった場合のみ
 * 再接続する。
 */
public class StabilizedPowerSupplyBlockEntity
        extends ElectricBlockEntity
        implements IHaveGoggleInformation,
        IMultiScreenHandlerFactory {


    /*
     * =========================================================
     * 内部変圧器
     * =========================================================
     */

    private RegulatedTransformerCoupling converter;


    /*
     * =========================================================
     * 外部端子
     *
     * 0 = INPUT+
     * 1 = INPUT-
     * 2 = OUTPUT+
     * 3 = OUTPUT-
     * =========================================================
     */

    private IElectricNode inputPlusNode;
    private IElectricNode inputMinusNode;

    private IElectricNode outputPlusNode;
    private IElectricNode outputMinusNode;


    /*
     * =========================================================
     * 内部変圧器二次側
     * =========================================================
     */

    private IElectricNode converterOutputPlusNode;
    private IElectricNode converterOutputMinusNode;


    /*
     * =========================================================
     * 出力スイッチ
     * =========================================================
     */

    private SwitchedWire outputSwitchPlus;
    private SwitchedWire outputSwitchMinus;


    /*
     * =========================================================
     * ユーザー設定
     * =========================================================
     */

    private boolean mpptEnabled = true;

    private double targetOutputVoltage = 24.0;

    private double currentLimit = 100.0;


    /*
     * =========================================================
     * 電気的上限
     * =========================================================
     */

    private static final double EFFICIENCY = 0.98;

    private static final double MAX_OUTPUT_VOLTAGE = 5000.0;

    private static final double MAX_OUTPUT_CURRENT = 100.0;

    private static final double MAX_OUTPUT_POWER = 50000.0;


    /*
     * =========================================================
     * 電圧・電流判定
     * =========================================================
     */

    private static final double MIN_VOLTAGE = 0.001;

    private static final double MIN_CURRENT = 0.000001;


    /*
     * =========================================================
     * 出力配線抵抗
     *
     * Plus側とMinus側にそれぞれ半分ずつ入れる。
     *
     * 合計：
     *
     * OUTPUT_WIRE_RESISTANCE
     *
     * =========================================================
     */

    private static final double OUTPUT_WIRE_RESISTANCE = 0.0001;


    /*
     * =========================================================
     * 逆流保護
     * =========================================================
     */

    /**
     * この値を超えて外部から内部へ流れた場合、
     * 逆流と判定する。
     */
    private static final double REVERSE_CURRENT_THRESHOLD = 0.01;


    /**
     * 再接続に必要な電圧差。
     */
    private static final double RECONNECT_VOLTAGE_MARGIN = 0.5;


    /**
     * 逆流検出後の待機時間。
     */
    private static final int RECONNECT_DELAY_TICKS = 20;


    /**
     * 一時的な数値振動で遮断しないための連続検出回数。
     */
    private static final int REVERSE_DETECTION_TICKS = 2;


    private int reconnectTimer = 0;

    private int reverseDetectionCounter = 0;


    /*
     * =========================================================
     * 保護状態
     * =========================================================
     */

    private enum OutputProtectionState {

        CONNECTED,

        BLOCKED
    }


    private OutputProtectionState protectionState =
            OutputProtectionState.CONNECTED;


    /*
     * =========================================================
     * 表示値
     * =========================================================
     */

    private double inputVoltage;

    private double inputCurrent;

    private double inputPower;

    private double outputVoltage;

    private double outputCurrent;

    private double outputPower;

    private double internalOutputVoltage;

    private double reverseCurrent;


    /*
     * =========================================================
     * MPPT
     * =========================================================
     */

    private double mpptMaximumPower;

    private double mpptMaximumOutputPower;


    /*
     * =========================================================
     * コンストラクタ
     * =========================================================
     */

    public StabilizedPowerSupplyBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {

        super(
                type,
                pos,
                state
        );
    }


    /*
     * =========================================================
     * 回路構築
     * =========================================================
     */

    @Override
    public void buildCircuit(
            CircuitBuilder builder
    ) {

        /*
         * 4端子
         */

        builder.setTerminalCount(4);


        /*
         * INPUT
         */

        inputPlusNode =
                builder.terminalNode(0);

        inputMinusNode =
                builder.terminalNode(1);


        /*
         * OUTPUT
         */

        outputPlusNode =
                builder.terminalNode(2);

        outputMinusNode =
                builder.terminalNode(3);


        /*
         * =====================================================
         * 内部二次側ノード
         * =====================================================
         */

        converterOutputPlusNode =
                builder.addInternalNode();

        converterOutputMinusNode =
                builder.addInternalNode();


        /*
         * =====================================================
         * 内部可変変圧器
         * =====================================================
         *
         * 一次側：
         *
         *     100
         *
         * 二次側：
         *
         *     初期24
         *
         * Vout / Vin = N2 / N1
         */

        converter =
                builder.addInternalNode(
                        RegulatedTransformerCoupling.class,
                        inputPlusNode,
                        inputMinusNode,
                        converterOutputPlusNode,
                        converterOutputMinusNode,
                        100.0,
                        Math.max(
                                0.0001,
                                targetOutputVoltage
                        )
                );


        /*
         * =====================================================
         * 出力スイッチ
         * =====================================================
         *
         * 初期状態はON。
         */

        outputSwitchPlus =
                builder.connectSwitch(
                        (float) (OUTPUT_WIRE_RESISTANCE / 2.0),
                        converterOutputPlusNode,
                        outputPlusNode,
                        protectionState
                                ==
                                OutputProtectionState.CONNECTED
                );


        outputSwitchMinus =
                builder.connectSwitch(
                        (float) (OUTPUT_WIRE_RESISTANCE / 2.0),
                        converterOutputMinusNode,
                        outputMinusNode,
                        protectionState
                                ==
                                OutputProtectionState.CONNECTED
                );
    }


    /*
     * =========================================================
     * 電気Tick
     * =========================================================
     */

    @Override
    public void electricalTick() {

        if (
                converter == null
                        ||
                        inputPlusNode == null
                        ||
                        inputMinusNode == null
                        ||
                        outputPlusNode == null
                        ||
                        outputMinusNode == null
                        ||
                        converterOutputPlusNode == null
                        ||
                        converterOutputMinusNode == null
        ) {

            return;
        }


        /*
         * =====================================================
         * 1. 入力電圧
         * =====================================================
         */

        inputVoltage =
                Math.max(
                        0.0,
                        inputPlusNode.getVoltage()
                                -
                                inputMinusNode.getVoltage()
                );


        /*
         * =====================================================
         * 2. 出力電圧
         * =====================================================
         */

        outputVoltage =
                Math.max(
                        0.0,
                        outputPlusNode.getVoltage()
                                -
                                outputMinusNode.getVoltage()
                );


        /*
         * =====================================================
         * 3. 内部出力電圧
         * =====================================================
         */

        internalOutputVoltage =
                Math.max(
                        0.0,
                        converterOutputPlusNode.getVoltage()
                                -
                                converterOutputMinusNode.getVoltage()
                );


        /*
         * =====================================================
         * 4. MPPT
         * =====================================================
         */

        if (mpptEnabled) {

            mpptMaximumPower =
                    findMpptMaximumPower();

            mpptMaximumOutputPower =
                    mpptMaximumPower
                            * EFFICIENCY;

        } else {

            mpptMaximumPower = 0.0;

            mpptMaximumOutputPower = 0.0;
        }


        /*
         * =====================================================
         * 5. 逆流検出
         * =====================================================
         *
         * outputSwitchPlus:
         *
         *   internal -> external
         *
         * が正方向。
         *
         * よって負値なら逆流。
         */

        if (
                protectionState
                        ==
                        OutputProtectionState.CONNECTED
        ) {

            detectReverseCurrent();
        }


        /*
         * =====================================================
         * 6. 遮断状態
         * =====================================================
         */

        if (
                protectionState
                        ==
                        OutputProtectionState.BLOCKED
        ) {

            handleBlockedState();

            updateNetworkData();

            return;
        }


        /*
         * =====================================================
         * 7. 最大許容電流
         * =====================================================
         */

        double maximumAllowedCurrent =
                calculateMaximumAllowedCurrent();


        /*
         * =====================================================
         * 8. 内部変圧器電圧を設定
         * =====================================================
         *
         * ここでは電流を生成しない。
         *
         * 単純に、
         *
         *   Vinternal = targetOutputVoltage
         *
         * を作る。
         */

        setTransformerOutputVoltage();


        /*
         * =====================================================
         * 9. 電流制限用直列抵抗
         * =====================================================
         *
         * 重要。
         *
         * 低抵抗バッテリーに対しても、
         *
         *   I = (Vinternal - Vbattery) / R
         *
         * となるように抵抗を自動調整する。
         */

        updateOutputCurrentLimitingResistance(
                maximumAllowedCurrent
        );


        /*
         * =====================================================
         * 10. 実際の電流を取得
         * =====================================================
         */

        double rawOutputCurrent =
                outputSwitchPlus != null
                        ? outputSwitchPlus.current()
                        : 0.0;


        /*
         * =====================================================
         * 11. 再接続直後の逆流確認
         * =====================================================
         */

        if (
                rawOutputCurrent
                        <
                        -REVERSE_CURRENT_THRESHOLD
        ) {

            reverseCurrent =
                    -rawOutputCurrent;

            blockOutput();

            outputCurrent = 0.0;

            outputPower = 0.0;

            updateNetworkData();

            return;
        }


        /*
         * =====================================================
         * 12. 正常電流
         * =====================================================
         *
         * 正方向なら、
         *
         *   抵抗負荷
         *   バッテリー充電
         *
         * のどちらでも正常。
         */

        outputCurrent =
                Math.max(
                        0.0,
                        rawOutputCurrent
                );


        /*
         * =====================================================
         * 13. 入力電流
         * =====================================================
         */

        inputCurrent =
                Math.abs(
                        converter.getPrimaryCurrent()
                );


        /*
         * =====================================================
         * 14. 電力
         * =====================================================
         */

        outputPower =
                outputVoltage
                        *
                        outputCurrent;


        inputPower =
                inputVoltage
                        *
                        inputCurrent;


        /*
         * =====================================================
         * 15. 安全処理
         * =====================================================
         */

        if (!Double.isFinite(outputPower))
            outputPower = 0.0;

        if (!Double.isFinite(inputPower))
            inputPower = 0.0;


        outputPower =
                Math.max(
                        0.0,
                        Math.min(
                                MAX_OUTPUT_POWER,
                                outputPower
                        )
                );


        inputPower =
                Math.max(
                        0.0,
                        inputPower
                );


        /*
         * =====================================================
         * 16. 更新
         * =====================================================
         */

        updateNetworkData();
    }


    /*
     * =========================================================
     * 最大許容電流
     * =========================================================
     */

    private double calculateMaximumAllowedCurrent() {

        double maximumAllowedCurrent =
                Math.min(
                        currentLimit,
                        MAX_OUTPUT_CURRENT
                );


        /*
         * 50kW制限
         */

        if (
                targetOutputVoltage
                        >
                        MIN_VOLTAGE
        ) {

            maximumAllowedCurrent =
                    Math.min(
                            maximumAllowedCurrent,
                            MAX_OUTPUT_POWER
                                    /
                                    targetOutputVoltage
                    );
        }


        /*
         * MPPT制限
         */

        if (
                mpptEnabled
                        &&
                        mpptMaximumOutputPower
                                >
                                0.0
                        &&
                        targetOutputVoltage
                                >
                                MIN_VOLTAGE
        ) {

            maximumAllowedCurrent =
                    Math.min(
                            maximumAllowedCurrent,
                            mpptMaximumOutputPower
                                    /
                                    targetOutputVoltage
                    );
        }


        return Math.max(
                0.0,
                Math.min(
                        MAX_OUTPUT_CURRENT,
                        maximumAllowedCurrent
                )
        );
    }


    /*
     * =========================================================
     * 変圧器電圧設定
     * =========================================================
     */

    private void setTransformerOutputVoltage() {

        if (
                inputVoltage
                        <=
                        MIN_VOLTAGE
        ) {

            return;
        }


        double ratio =
                targetOutputVoltage
                        /
                        inputVoltage;


        if (
                !Double.isFinite(ratio)
                        ||
                        ratio <= 0.0
        ) {

            return;
        }


        double secondaryTurns =
                converter.getPrimaryTurns()
                        *
                        ratio;


        converter.setSecondaryTurns(
                secondaryTurns
        );
    }


    /*
     * =========================================================
     * 出力電流制限抵抗
     * =========================================================
     *
     * この方式の重要部分。
     *
     * 例えば、
     *
     * 内部 = 58.4V
     * バッテリー = 52V
     * 最大電流 = 10A
     *
     * なら、
     *
     * R = (58.4 - 52) / 10
     *   = 0.64Ω
     *
     * を出力側に入れる。
     *
     * Plus / Minusに0.32Ωずつ入れる。
     *
     * 通常の抵抗負荷では
     * 内部電圧と出力電圧がほぼ同じなので、
     * ほぼ最低抵抗になる。
     */

    private void updateOutputCurrentLimitingResistance(
            double maximumAllowedCurrent
    ) {

        if (
                outputSwitchPlus == null
                        ||
                        outputSwitchMinus == null
        ) {

            return;
        }


        /*
         * 電流制限OFF相当。
         */

        if (
                maximumAllowedCurrent
                        <=
                        MIN_CURRENT
        ) {

            outputSwitchPlus.setResistance(
                    (float) (OUTPUT_WIRE_RESISTANCE / 2.0)
            );

            outputSwitchMinus.setResistance(
                    (float) (OUTPUT_WIRE_RESISTANCE / 2.0)
            );

            return;
        }


        /*
         * 内部側と外部側の電圧差。
         */

        double voltageDifference =
                internalOutputVoltage
                        -
                        outputVoltage;


        /*
         * 外部電圧が内部より高い場合は、
         * 電流制限抵抗を追加しない。
         *
         * この状態は逆流検出側で処理する。
         */

        if (
                voltageDifference
                        <=
                        0.0
        ) {

            outputSwitchPlus.setResistance(
                    (float) (OUTPUT_WIRE_RESISTANCE / 2.0)
            );

            outputSwitchMinus.setResistance(
                    (float) (OUTPUT_WIRE_RESISTANCE / 2.0)
            );

            return;
        }


        /*
         * I = V / R
         *
         * R = V / I
         */

        double requiredResistance =
                voltageDifference
                        /
                        maximumAllowedCurrent;


        if (
                !Double.isFinite(requiredResistance)
                        ||
                        requiredResistance < 0.0
        ) {

            requiredResistance = 0.0;
        }


        /*
         * 最低配線抵抗を含める。
         */

        double totalResistance =
                Math.max(
                        OUTPUT_WIRE_RESISTANCE,
                        requiredResistance
                );


        /*
         * Plus / Minusに半分ずつ。
         */

        double halfResistance =
                totalResistance / 2.0;


        /*
         * float範囲安全処理。
         */

        halfResistance =
                Math.max(
                        0.000001,
                        Math.min(
                                Float.MAX_VALUE,
                                halfResistance
                        )
                );


        outputSwitchPlus.setResistance(
                (float) halfResistance
        );

        outputSwitchMinus.setResistance(
                (float) halfResistance
        );
    }


    /*
     * =========================================================
     * 逆流検出
     * =========================================================
     */

    private void detectReverseCurrent() {

        if (outputSwitchPlus == null)
            return;


        double plusCurrent =
                outputSwitchPlus.current();


        double minusCurrent =
                outputSwitchMinus != null
                        ? outputSwitchMinus.current()
                        : 0.0;


        boolean reverseDetected =
                plusCurrent
                        <
                        -REVERSE_CURRENT_THRESHOLD
                        ||
                        minusCurrent
                                >
                                REVERSE_CURRENT_THRESHOLD;


        if (reverseDetected) {

            if (plusCurrent < 0.0) {

                reverseCurrent =
                        -plusCurrent;

            } else {

                reverseCurrent =
                        Math.max(
                                0.0,
                                minusCurrent
                        );
            }


            reverseDetectionCounter++;


            /*
             * 数値振動1回では遮断しない。
             */

            if (
                    reverseDetectionCounter
                            >=
                            REVERSE_DETECTION_TICKS
            ) {

                blockOutput();

                reverseDetectionCounter = 0;
            }

        } else {

            reverseDetectionCounter = 0;

            /*
             * 正常状態では逆流表示を消す。
             */

            if (reverseCurrent < 0.01)
                reverseCurrent = 0.0;
        }
    }


    /*
     * =========================================================
     * 遮断処理
     * =========================================================
     */

    private void blockOutput() {

        protectionState =
                OutputProtectionState.BLOCKED;


        reconnectTimer =
                RECONNECT_DELAY_TICKS;


        reverseDetectionCounter = 0;


        if (outputSwitchPlus != null)
            outputSwitchPlus.setState(false);


        if (outputSwitchMinus != null)
            outputSwitchMinus.setState(false);


        setChanged();
    }


    /*
     * =========================================================
     * 遮断中処理
     * =========================================================
     */

    private void handleBlockedState() {

        /*
         * 念のためOFFを維持。
         */

        if (outputSwitchPlus != null)
            outputSwitchPlus.setState(false);


        if (outputSwitchMinus != null)
            outputSwitchMinus.setState(false);


        /*
         * 出力電流は0表示。
         */

        outputCurrent = 0.0;

        outputPower = 0.0;


        /*
         * 入力側表示。
         */

        inputCurrent =
                Math.abs(
                        converter.getPrimaryCurrent()
                );


        inputPower =
                Math.max(
                        0.0,
                        inputVoltage
                                *
                                inputCurrent
                );


        /*
         * タイマー。
         */

        if (reconnectTimer > 0) {

            reconnectTimer--;

            return;
        }


        /*
         * =====================================================
         * 再接続安全確認
         * =====================================================
         */

        boolean inputAvailable =
                inputVoltage
                        >
                        MIN_VOLTAGE;


        boolean internalHigher =
                internalOutputVoltage
                        >
                        outputVoltage
                                +
                                RECONNECT_VOLTAGE_MARGIN;


        boolean targetHigher =
                targetOutputVoltage
                        >
                        outputVoltage
                                +
                                RECONNECT_VOLTAGE_MARGIN;


        if (
                inputAvailable
                        &&
                        internalHigher
                        &&
                        targetHigher
        ) {

            reconnectOutput();
        }
    }


    /*
     * =========================================================
     * 再接続
     * =========================================================
     */

    private void reconnectOutput() {

        if (
                outputSwitchPlus == null
                        ||
                        outputSwitchMinus == null
        ) {

            return;
        }


        /*
         * 最終安全確認。
         */

        if (
                inputVoltage
                        <=
                        MIN_VOLTAGE
        ) {

            return;
        }


        if (
                internalOutputVoltage
                        <=
                        outputVoltage
                                +
                                RECONNECT_VOLTAGE_MARGIN
        ) {

            return;
        }


        if (
                targetOutputVoltage
                        <=
                        outputVoltage
                                +
                                RECONNECT_VOLTAGE_MARGIN
        ) {

            return;
        }


        /*
         * 先に最低抵抗へ戻す。
         *
         * 次のtickで電流制限抵抗を計算する。
         */

        outputSwitchPlus.setResistance(
                (float) (OUTPUT_WIRE_RESISTANCE / 2.0)
        );


        outputSwitchMinus.setResistance(
                (float) (OUTPUT_WIRE_RESISTANCE / 2.0)
        );


        /*
         * ON。
         */

        outputSwitchPlus.setState(true);

        outputSwitchMinus.setState(true);


        protectionState =
                OutputProtectionState.CONNECTED;


        reconnectTimer = 0;

        reverseCurrent = 0.0;

        reverseDetectionCounter = 0;


        setChanged();
    }


    /*
     * =========================================================
     * MPPT最大電力
     * =========================================================
     */

    private double findMpptMaximumPower() {

        if (inputPlusNode == null)
            return 0.0;


        ElectricalNetwork network =
                inputPlusNode.getNetwork();


        if (network == null)
            return 0.0;


        double totalPower = 0.0;


        for (
                var coupling :
                network.getCouplings()
        ) {

            if (
                    !(coupling
                            instanceof VoltageSourceCoupling source)
            ) {

                continue;
            }


            /*
             * MPPT対応電源のみ。
             */

            if (!source.isMpptSource())
                continue;


            double power =
                    source.getMpptMaximumPower();


            if (!Double.isFinite(power))
                continue;


            totalPower +=
                    Math.max(
                            0.0,
                            power
                    );
        }


        if (!Double.isFinite(totalPower))
            return 0.0;


        return Math.max(
                0.0,
                totalPower
        );
    }


    /*
     * =========================================================
     * ネットワークデータ更新
     * =========================================================
     */

    private void updateNetworkData() {

        setChanged();


        if (
                level != null
                        &&
                        !level.isClientSide
                        &&
                        level.getGameTime() % 5 == 0
        ) {

            sendData();
        }
    }


    /*
     * =========================================================
     * MPPT設定
     * =========================================================
     */

    public boolean isMpptEnabled() {

        return mpptEnabled;
    }


    public void setMpptEnabled(
            boolean enabled
    ) {

        mpptEnabled =
                enabled;

        setChanged();


        if (
                level != null
                        &&
                        !level.isClientSide
        ) {

            sendData();
        }
    }


    /*
     * =========================================================
     * 出力電圧
     * =========================================================
     */

    public double getTargetOutputVoltage() {

        return targetOutputVoltage;
    }


    public void setTargetOutputVoltage(
            double voltage
    ) {

        targetOutputVoltage =
                Math.max(
                        0.1,
                        Math.min(
                                MAX_OUTPUT_VOLTAGE,
                                voltage
                        )
                );


        setChanged();


        if (
                level != null
                        &&
                        !level.isClientSide
        ) {

            sendData();
        }
    }


    /*
     * =========================================================
     * 電流制限
     * =========================================================
     */

    public double getCurrentLimit() {

        return currentLimit;
    }


    public void setCurrentLimit(
            double current
    ) {

        currentLimit =
                Math.max(
                        0.0,
                        Math.min(
                                MAX_OUTPUT_CURRENT,
                                current
                        )
                );


        setChanged();


        if (
                level != null
                        &&
                        !level.isClientSide
        ) {

            sendData();
        }
    }


    /*
     * =========================================================
     * GUI
     * =========================================================
     */

    @Override
    public AbstractContainerMenu createMenu(
            int syncId,
            Inventory playerInventory,
            Player player,
            int menuIndex
    ) {

        return new StabilizedPowerSupplyMenu(
                ModdedMenus.STABILIZED_POWER_SUPPLY.get(),
                syncId,
                playerInventory,
                this
        );
    }


    @Override
    public Component getDisplayName() {

        return Component.literal(
                "Stabilized Power Supply"
        );
    }


    /*
     * =========================================================
     * NBT WRITE
     * =========================================================
     */

    @Override
    protected void write(
            CompoundTag tag,
            boolean clientPacket
    ) {

        super.write(
                tag,
                clientPacket
        );


        tag.putBoolean(
                "MPPT",
                mpptEnabled
        );


        tag.putDouble(
                "TargetOutputVoltage",
                targetOutputVoltage
        );


        tag.putDouble(
                "CurrentLimit",
                currentLimit
        );


        tag.putDouble(
                "InputVoltage",
                inputVoltage
        );


        tag.putDouble(
                "InputCurrent",
                inputCurrent
        );


        tag.putDouble(
                "InputPower",
                inputPower
        );


        tag.putDouble(
                "OutputVoltage",
                outputVoltage
        );


        tag.putDouble(
                "OutputCurrent",
                outputCurrent
        );


        tag.putDouble(
                "OutputPower",
                outputPower
        );


        tag.putDouble(
                "InternalOutputVoltage",
                internalOutputVoltage
        );


        tag.putDouble(
                "ReverseCurrent",
                reverseCurrent
        );


        tag.putDouble(
                "MPPTMaximumPower",
                mpptMaximumPower
        );


        tag.putDouble(
                "MPPTMaximumOutputPower",
                mpptMaximumOutputPower
        );


        tag.putInt(
                "ReconnectTimer",
                reconnectTimer
        );


        tag.putInt(
                "ReverseDetectionCounter",
                reverseDetectionCounter
        );


        tag.putBoolean(
                "OutputBlocked",
                protectionState
                        ==
                        OutputProtectionState.BLOCKED
        );
    }


    /*
     * =========================================================
     * NBT READ
     * =========================================================
     */

    @Override
    protected void read(
            CompoundTag tag,
            boolean clientPacket
    ) {

        super.read(
                tag,
                clientPacket
        );


        mpptEnabled =
                tag.getBoolean(
                        "MPPT"
                );


        targetOutputVoltage =
                tag.getDouble(
                        "TargetOutputVoltage"
                );


        targetOutputVoltage =
                Math.max(
                        0.1,
                        Math.min(
                                MAX_OUTPUT_VOLTAGE,
                                targetOutputVoltage
                        )
                );


        currentLimit =
                tag.getDouble(
                        "CurrentLimit"
                );


        currentLimit =
                Math.max(
                        0.0,
                        Math.min(
                                MAX_OUTPUT_CURRENT,
                                currentLimit
                        )
                );


        inputVoltage =
                tag.getDouble(
                        "InputVoltage"
                );


        inputCurrent =
                tag.getDouble(
                        "InputCurrent"
                );


        inputPower =
                tag.getDouble(
                        "InputPower"
                );


        outputVoltage =
                tag.getDouble(
                        "OutputVoltage"
                );


        outputCurrent =
                tag.getDouble(
                        "OutputCurrent"
                );


        outputPower =
                tag.getDouble(
                        "OutputPower"
                );


        internalOutputVoltage =
                tag.getDouble(
                        "InternalOutputVoltage"
                );


        reverseCurrent =
                tag.getDouble(
                        "ReverseCurrent"
                );


        mpptMaximumPower =
                tag.getDouble(
                        "MPPTMaximumPower"
                );


        mpptMaximumOutputPower =
                tag.getDouble(
                        "MPPTMaximumOutputPower"
                );


        reconnectTimer =
                Math.max(
                        0,
                        tag.getInt(
                                "ReconnectTimer"
                        )
                );


        reverseDetectionCounter =
                Math.max(
                        0,
                        tag.getInt(
                                "ReverseDetectionCounter"
                        )
                );


        boolean blocked =
                tag.getBoolean(
                        "OutputBlocked"
                );


        protectionState =
                blocked
                        ?
                        OutputProtectionState.BLOCKED
                        :
                        OutputProtectionState.CONNECTED;
    }


    /*
     * =========================================================
     * ゴーグル
     * =========================================================
     */

    @Override
    public boolean addToGoggleTooltip(
            List<Component> tooltip,
            boolean sneaking
    ) {

        tooltip.add(
                Component.literal(
                        "Stabilized Power Supply"
                )
        );


        tooltip.add(
                Component.literal(
                        String.format(
                                "MPPT: %s",
                                mpptEnabled
                                        ? "ON"
                                        : "OFF"
                        )
                )
        );


        tooltip.add(
                Component.literal(
                        String.format(
                                "Output: %s",
                                protectionState
                                        ==
                                        OutputProtectionState.CONNECTED
                                        ?
                                        "CONNECTED"
                                        :
                                        "BLOCKED"
                        )
                )
        );


        /*
         * INPUT
         */

        tooltip.add(
                Component.literal(
                        String.format(
                                "Input: %.2f V / %.2f A / %.1f W",
                                inputVoltage,
                                inputCurrent,
                                inputPower
                        )
                )
        );


        /*
         * OUTPUT
         */

        tooltip.add(
                Component.literal(
                        String.format(
                                "Output: %.2f V / %.2f A / %.1f W",
                                outputVoltage,
                                outputCurrent,
                                outputPower
                        )
                )
        );


        /*
         * 設定電圧
         */

        tooltip.add(
                Component.literal(
                        String.format(
                                "Set Voltage: %.2f V",
                                targetOutputVoltage
                        )
                )
        );


        /*
         * 電流制限
         */

        tooltip.add(
                Component.literal(
                        String.format(
                                "Current Limit: %.2f A",
                                currentLimit
                        )
                )
        );


        /*
         * 内部電圧
         */

        tooltip.add(
                Component.literal(
                        String.format(
                                "Internal Voltage: %.2f V",
                                internalOutputVoltage
                        )
                )
        );


        /*
         * 変圧比
         */

        if (converter != null) {

            tooltip.add(
                    Component.literal(
                            String.format(
                                    "Turns Ratio: %.6f",
                                    converter.getTurnsRatio()
                            )
                    )
            );


            tooltip.add(
                    Component.literal(
                            String.format(
                                    "Secondary Turns: %.4f",
                                    converter.getSecondaryTurns()
                            )
                    )
            );
        }


        /*
         * 逆流
         */

        if (reverseCurrent > 0.0) {

            tooltip.add(
                    Component.literal(
                            String.format(
                                    "Reverse Current: %.3f A",
                                    reverseCurrent
                            )
                    )
            );
        }


        /*
         * 再接続タイマー
         */

        if (
                protectionState
                        ==
                        OutputProtectionState.BLOCKED
        ) {

            tooltip.add(
                    Component.literal(
                            String.format(
                                    "Reconnect: %d ticks",
                                    reconnectTimer
                            )
                    )
            );
        }


        /*
         * MPPT
         */

        if (mpptEnabled) {

            tooltip.add(
                    Component.literal(
                            String.format(
                                    "MPPT Input Maximum: %.1f W",
                                    mpptMaximumPower
                            )
                    )
            );


            tooltip.add(
                    Component.literal(
                            String.format(
                                    "MPPT Output Maximum: %.1f W",
                                    mpptMaximumOutputPower
                            )
                    )
            );
        }


        /*
         * 効率
         */

        tooltip.add(
                Component.literal(
                        String.format(
                                "Efficiency: %.1f%%",
                                EFFICIENCY * 100.0
                        )
                )
        );


        /*
         * 最大出力
         */

        tooltip.add(
                Component.literal(
                        String.format(
                                "Output Limit: %.1f kW",
                                MAX_OUTPUT_POWER / 1000.0
                        )
                )
        );


        return true;
    }


    /*
     * =========================================================
     * GUIデータ
     * =========================================================
     */

    public void sendToMenu(
            FriendlyByteBuf buffer
    ) {

        buffer.writeBlockPos(
                getBlockPos()
        );
    }


    /*
     * =========================================================
     * Getter
     * =========================================================
     */

    public double getInputVoltage() {

        return inputVoltage;
    }


    public double getInputCurrent() {

        return inputCurrent;
    }


    public double getInputPower() {

        return inputPower;
    }


    public double getOutputVoltage() {

        return outputVoltage;
    }


    public double getOutputCurrent() {

        return outputCurrent;
    }


    public double getOutputPower() {

        return outputPower;
    }


    public double getInternalOutputVoltage() {

        return internalOutputVoltage;
    }


    public double getReverseCurrent() {

        return reverseCurrent;
    }


    public boolean isOutputBlocked() {

        return protectionState
                ==
                OutputProtectionState.BLOCKED;
    }


    public double getMpptMaximumPower() {

        return mpptMaximumPower;
    }


    public double getMpptMaximumOutputPower() {

        return mpptMaximumOutputPower;
    }


    public double getMaximumOutputVoltage() {

        return MAX_OUTPUT_VOLTAGE;
    }


    public double getMaximumOutputCurrent() {

        return MAX_OUTPUT_CURRENT;
    }


    public double getMaximumOutputPower() {

        return MAX_OUTPUT_POWER;
    }


    public double getEfficiency() {

        return EFFICIENCY;
    }
}