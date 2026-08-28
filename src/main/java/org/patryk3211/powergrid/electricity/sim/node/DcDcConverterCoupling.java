package org.patryk3211.powergrid.electricity.sim.node;

import org.patryk3211.powergrid.electricity.sim.solver.IAdmittanceAdder;
import org.patryk3211.powergrid.electricity.sim.solver.IResidualAdder;
import org.patryk3211.powergrid.electricity.sim.solver.IStaticResidual;

import java.util.Collection;
import java.util.List;

/**
 * 安定化電源用 DC-DC コンバータ。
 *
 * ---------------------------------------------------------
 * 特徴
 * ---------------------------------------------------------
 *
 * ・入力電圧は制限なし
 * ・出力電圧 0.1 ～ 5000 V
 * ・出力電流 最大100 A
 * ・出力電力 最大50 kW
 * ・効率 98 %
 *
 * 出力側は電流源として動作し、
 * targetOutputVoltage に向かって出力電流を制御する。
 *
 * また、入力側には実際に消費する入力電流を
 * residual として追加する。
 *
 * これにより、
 *
 *     入力電源
 *        ↓
 *     DC-DC
 *        ↓
 *     負荷
 *
 * という電力の流れを電気ネットワークに反映する。
 *
 * この内部回路には ThermalBehaviour を持たせない。
 */
public class DcDcConverterCoupling
        extends CouplingNode
        implements IStaticResidual {

    private final IElectricNode inputPositive;
    private final IElectricNode inputNegative;

    private final IElectricNode outputPositive;
    private final IElectricNode outputNegative;

    private double efficiency = 0.98;

    private double targetOutputVoltage = 24.0;

    private double maxOutputCurrent = 100.0;

    private static final double MAX_OUTPUT_POWER = 50000.0;

    /** Commercial inverter output limit (6.6 kV). */
    private static final double MAX_OUTPUT_VOLTAGE = 6600.0;

    private static final double MAX_OUTPUT_CURRENT = 100.0;

    private static final double MIN_INPUT_VOLTAGE = 0.001;

    /**
     * 出力電圧を目標値へ近づけるための仮想コンダクタンス。
     */
    private static final double OUTPUT_REGULATION_CONDUCTANCE = 100.0;

    private double inputVoltage;
    private double inputCurrent;
    private double inputPower;

    private double outputVoltage;
    private double outputCurrent;
    private double outputPower;

    /**
     * MPPT等から与えられる最大入力電力。
     *
     * Infinityの場合は電力制限なし。
     */
    private double availableInputPower = Double.POSITIVE_INFINITY;

    /**
     * 現在要求されている出力電流。
     */
    private double requestedOutputCurrent = 1.0;

    /**
     * 実際に使用可能な出力電力。
     */
    private double allowedOutputPower;


    public DcDcConverterCoupling(
            IElectricNode inputPositive,
            IElectricNode inputNegative,
            IElectricNode outputPositive,
            IElectricNode outputNegative
    ) {
        this.inputPositive = inputPositive;
        this.inputNegative = inputNegative;

        this.outputPositive = outputPositive;
        this.outputNegative = outputNegative;
    }


    /*
     * =========================================================
     * 設定
     * =========================================================
     */

    public void setEfficiency(double efficiency) {

        if (!Double.isFinite(efficiency))
            return;

        this.efficiency =
                Math.max(
                        0.000001,
                        Math.min(
                                1.0,
                                efficiency
                        )
                );
    }


    public double getEfficiency() {
        return efficiency;
    }


    public void setOutputVoltage(double voltage) {

        if (!Double.isFinite(voltage))
            return;

        targetOutputVoltage =
                Math.max(
                        0.1,
                        Math.min(
                                MAX_OUTPUT_VOLTAGE,
                                voltage
                        )
                );
    }


    public double getTargetOutputVoltage() {
        return targetOutputVoltage;
    }


    public double getOutputVoltage() {
        return outputVoltage;
    }


    public void setMaxOutputCurrent(double current) {

        if (!Double.isFinite(current))
            return;

        maxOutputCurrent =
                Math.max(
                        0.0,
                        Math.min(
                                MAX_OUTPUT_CURRENT,
                                current
                        )
                );

        requestedOutputCurrent =
                Math.min(
                        requestedOutputCurrent,
                        maxOutputCurrent
                );
    }


    public double getMaxOutputCurrent() {
        return maxOutputCurrent;
    }


    public void setOutputCurrent(double current) {

        if (!Double.isFinite(current))
            current = 0.0;

        requestedOutputCurrent =
                Math.max(
                        0.0,
                        Math.min(
                                maxOutputCurrent,
                                current
                        )
                );
    }


    public double getOutputCurrent() {
        return outputCurrent;
    }


    public void setAvailableInputPower(double power) {

        if (!Double.isFinite(power)) {
            availableInputPower =
                    Double.POSITIVE_INFINITY;
            return;
        }

        availableInputPower =
                Math.max(
                        0.0,
                        power
                );
    }


    public double getAvailableInputPower() {
        return availableInputPower;
    }


    public double getInputVoltage() {
        return inputVoltage;
    }


    public double getInputCurrent() {
        return inputCurrent;
    }


    public double getInputPower() {
        return inputPower;
    }


    public double getOutputPower() {
        return outputPower;
    }


    public double getAllowedOutputPower() {
        return allowedOutputPower;
    }


    /*
     * =========================================================
     * 回路行列
     * =========================================================
     */

    @Override
    public void couple(
            IAdmittanceAdder admittance
    ) {

        /*
         * 出力側に非常に小さい導通を持たせる。
         *
         * これにより出力ノードが完全に浮くことを防ぐ。
         */

        final double outputConductance =
                OUTPUT_REGULATION_CONDUCTANCE;

        admittance.add(
                outputPositive.getIndex(),
                outputPositive.getIndex(),
                outputConductance
        );

        admittance.add(
                outputPositive.getIndex(),
                outputNegative.getIndex(),
                -outputConductance
        );

        admittance.add(
                outputNegative.getIndex(),
                outputPositive.getIndex(),
                -outputConductance
        );

        admittance.add(
                outputNegative.getIndex(),
                outputNegative.getIndex(),
                outputConductance
        );


        /*
         * 入力側には極小の安定化コンダクタンスを追加。
         */

        final double inputConductance =
                1.0e-9;

        admittance.add(
                inputPositive.getIndex(),
                inputPositive.getIndex(),
                inputConductance
        );

        admittance.add(
                inputNegative.getIndex(),
                inputNegative.getIndex(),
                inputConductance
        );
    }


    /*
     * =========================================================
     * 電流・電力制御
     * =========================================================
     */

    @Override
    public void addStaticResidual(
            IResidualAdder residual
    ) {

        double vin =
                inputPositive.getVoltage()
                        -
                        inputNegative.getVoltage();

        double vout =
                outputPositive.getVoltage()
                        -
                        outputNegative.getVoltage();


        if (!Double.isFinite(vin))
            vin = 0.0;

        if (!Double.isFinite(vout))
            vout = 0.0;


        inputVoltage =
                Math.max(
                        0.0,
                        vin
                );

        outputVoltage =
                Math.max(
                        0.0,
                        vout
                );


        /*
         * =====================================================
         * 最大出力電力
         * =====================================================
         */

        double maximumPower =
                MAX_OUTPUT_POWER;


        /*
         * MPPT等による入力電力制限。
         */

        if (Double.isFinite(availableInputPower)) {

            maximumPower =
                    Math.min(
                            maximumPower,
                            Math.max(
                                    0.0,
                                    availableInputPower
                                            * efficiency
                            )
                    );
        }


        allowedOutputPower =
                Math.max(
                        0.0,
                        maximumPower
                );


        /*
         * =====================================================
         * 最大出力電流
         * =====================================================
         */

        double allowedCurrent =
                Math.min(
                        maxOutputCurrent,
                        MAX_OUTPUT_CURRENT
                );


        /*
         * 電力制限を電流制限へ変換。
         */

        if (targetOutputVoltage > 0.0) {

            allowedCurrent =
                    Math.min(
                            allowedCurrent,
                            allowedOutputPower
                                    / targetOutputVoltage
                    );
        }


        allowedCurrent =
                Math.max(
                        0.0,
                        allowedCurrent
                );


        /*
         * 実際の要求電流。
         */

        double current =
                Math.max(
                        0.0,
                        Math.min(
                                requestedOutputCurrent,
                                allowedCurrent
                        )
                );


        /*
         * =====================================================
         * 出力電圧制御
         * =====================================================
         *
         * 目標電圧との差に応じて電流を減少させる。
         *
         * 例えば、
         *
         * target = 100V
         * actual = 90V
         *
         * なら電流を増やす。
         *
         * actual > target
         *
         * なら電流を減らす。
         */

        if (current > 0.0 &&
                targetOutputVoltage > 0.0) {

            double voltageError =
                    targetOutputVoltage
                            -
                            outputVoltage;

            double correction =
                    OUTPUT_REGULATION_CONDUCTANCE
                            *
                            voltageError;

            /*
             * 現在電流 + 電圧補正。
             */

            current =
                    current
                            +
                            correction;

            /*
             * 絶対値制限。
             */

            current =
                    Math.max(
                            0.0,
                            Math.min(
                                    allowedCurrent,
                                    current
                            )
                    );
        }


        outputCurrent =
                current;


        /*
         * =====================================================
         * 出力電力
         * =====================================================
         */

        outputPower =
                outputVoltage
                        *
                        outputCurrent;


        if (!Double.isFinite(outputPower) ||
                outputPower < 0.0) {

            outputPower = 0.0;
        }


        /*
         * 最大電力を超えないようにする。
         */

        if (outputPower >
                MAX_OUTPUT_POWER) {

            outputPower =
                    MAX_OUTPUT_POWER;

            if (outputVoltage >
                    MIN_INPUT_VOLTAGE) {

                outputCurrent =
                        Math.min(
                                outputCurrent,
                                MAX_OUTPUT_POWER
                                        / outputVoltage
                        );
            }
        }


        /*
         * =====================================================
         * 入力電力
         * =====================================================
         *
         * Pinput = Poutput / efficiency
         */

        inputPower =
                outputPower
                        /
                        Math.max(
                                0.000001,
                                efficiency
                        );


        /*
         * 入力電力制限。
         */

        if (Double.isFinite(
                availableInputPower)) {

            inputPower =
                    Math.min(
                            inputPower,
                            Math.max(
                                    0.0,
                                    availableInputPower
                            )
                    );
        }


        /*
         * =====================================================
         * 入力電流
         * =====================================================
         */

        if (inputVoltage >
                MIN_INPUT_VOLTAGE) {

            inputCurrent =
                    inputPower
                            /
                            inputVoltage;

        } else {

            inputCurrent = 0.0;
        }


        if (!Double.isFinite(inputCurrent) ||
                inputCurrent < 0.0) {

            inputCurrent = 0.0;
        }


        /*
         * =====================================================
         * 入力側へ実際の消費電流を追加
         * =====================================================
         *
         * 電流の向き：
         *
         * inputPositive
         *       ↓
         * inputNegative
         *
         * したがって、
         *
         * +I
         * -I
         *
         * を入力端子へ追加する。
         *
         * これが以前のコードには無かったため、
         * DC-DCが入力電源から実際に電力を消費していなかった。
         */

        residual.add(
                inputPositive.getIndex(),
                inputCurrent
        );

        residual.add(
                inputNegative.getIndex(),
                -inputCurrent
        );


        /*
         * =====================================================
         * 出力側へ電流を供給
         * =====================================================
         */

        residual.add(
                outputPositive.getIndex(),
                -outputCurrent
        );

        residual.add(
                outputNegative.getIndex(),
                outputCurrent
        );
    }


    /*
     * =========================================================
     * Node情報
     * =========================================================
     */

    @Override
    public Collection<IElectricNode> coupledNodes() {

        return List.of(
                inputPositive,
                inputNegative,
                outputPositive,
                outputNegative
        );
    }


    @Override
    public boolean isSource() {
        return true;
    }


    @Override
    public List<INode> affectedNodes() {

        return List.of(
                inputPositive,
                inputNegative,
                outputPositive,
                outputNegative
        );
    }


    public IElectricNode getInputPositive() {
        return inputPositive;
    }


    public IElectricNode getInputNegative() {
        return inputNegative;
    }


    public IElectricNode getOutputPositive() {
        return outputPositive;
    }


    public IElectricNode getOutputNegative() {
        return outputNegative;
    }


    @Override
    public String toString() {

        return String.format(
                "StabilizedDCConverter(" +
                        "Vin=%gV " +
                        "Iin=%gA " +
                        "Pin=%gW " +
                        "Vout=%gV " +
                        "Iout=%gA " +
                        "Pout=%gW " +
                        "Target=%gV)",
                inputVoltage,
                inputCurrent,
                inputPower,
                outputVoltage,
                outputCurrent,
                outputPower,
                targetOutputVoltage
        );
    }
}
