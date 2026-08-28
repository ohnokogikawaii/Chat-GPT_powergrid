package org.patryk3211.powergrid.electricity.converter.general;

import org.patryk3211.powergrid.electricity.sim.node.RegulatedTransformerCoupling;
import org.patryk3211.powergrid.electricity.solar.MPPTController;
import org.patryk3211.powergrid.electricity.solar.SolarSpec;

/**
 * General Power Conditioner の1系統分のPV/MPPT制御。
 *
 * PV
 *  │
 *  │ primary
 *  ▼
 * RegulatedTransformerCoupling
 *  │
 *  │ secondary
 *  ▼
 * DC LINK
 *
 * 変圧器の二次巻数を変更することで、
 * PV側電圧がMPPT電圧(Vmpp)になるように制御する。
 */
public final class GeneralPowerConditionerMpptChannel {

    private static final double MIN_VOLTAGE = 0.001;

    /*
     * 変圧比が極端にならないように制限する。
     *
     * 410Wパネル:
     * Vmpp ≒ 数十V
     * DC LINK = 400V
     *
     * そのため通常は10倍前後の変圧比になる。
     */
    private static final double MIN_TURNS_RATIO = 0.01;
    private static final double MAX_TURNS_RATIO = 100.0;

    /*
     * MPPTの更新周期。
     *
     * 電気シミュレーションを毎Tick激しく変更すると
     * ネットワークが不安定になる可能性があるため、
     * 現状は毎Tick更新可能な構成にする。
     */
    private static final double EPSILON = 1.0e-9;

    private final SolarSpec spec;

    private final MPPTController mppt;

    private final RegulatedTransformerCoupling transformer;

    /*
     * MPPT計算結果
     */
    private double maximumPower;

    private double maximumPowerVoltage;

    private double maximumPowerCurrent;

    /*
     * 現在設定している目標
     */
    private double targetPanelVoltage;

    private double targetBusVoltage;

    /*
     * 現在設定している変圧比
     */
    private double appliedTurnsRatio;

    /*
     * 実際に計算されたPV電力。
     *
     * 現状のElectricalNetworkから取得できる
     * transformer電流を利用する。
     */
    private double actualPower;

    private double actualCurrent;

    private double actualVoltage;


    public GeneralPowerConditionerMpptChannel(
            SolarSpec spec,
            RegulatedTransformerCoupling transformer
    ) {

        if (spec == null) {
            throw new IllegalArgumentException(
                    "SolarSpec must not be null"
            );
        }

        if (transformer == null) {
            throw new IllegalArgumentException(
                    "MPPT transformer must not be null"
            );
        }

        this.spec = spec;

        this.mppt =
                new MPPTController(spec);

        this.transformer =
                transformer;
    }


    /**
     * SolarSpec / MPPTControllerから
     * 現在の最大電力点を計算する。
     */
    public void updateMaximumPower(
            double irradiance,
            double temperature
    ) {

        /*
         * 不正な入力を防止。
         */
        if (!Double.isFinite(irradiance)) {
            irradiance = 1000.0;
        }

        if (!Double.isFinite(temperature)) {
            temperature = 25.0;
        }

        irradiance =
                Math.max(
                        0.0,
                        irradiance
                );

        /*
         * MPPTモデルを更新。
         */
        mppt.update(
                irradiance,
                temperature
        );

        /*
         * MPPT結果を取得。
         */
        maximumPower =
                finitePositive(
                        mppt.getMaximumPower()
                );

        maximumPowerVoltage =
                finitePositive(
                        mppt.getVoltage()
                );

        maximumPowerCurrent =
                finitePositive(
                        mppt.getCurrent()
                );

        /*
         * PV側の目標電圧。
         */
        targetPanelVoltage =
                maximumPowerVoltage;
    }


    /**
     * DC LINK電圧を指定して、
     * PV側をVmppへ持っていくための
     * 変圧比を設定する。
     *
     * 例:
     *
     * PV Vmpp = 35V
     * DC LINK = 400V
     *
     * ratio = 400 / 35
     *
     * とする。
     */
    public void applyTarget(
            double busVoltage
    ) {

        targetBusVoltage =
                finitePositive(
                        busVoltage
                );

        /*
         * 有効な電圧がない場合は制御しない。
         */
        if (
                transformer == null
                        ||
                        targetPanelVoltage <= MIN_VOLTAGE
                        ||
                        targetBusVoltage <= MIN_VOLTAGE
        ) {

            return;
        }

        /*
         * 理想変圧器:
         *
         * Vsecondary / Vprimary
         * =
         * secondaryTurns / primaryTurns
         *
         * なので、
         *
         * secondaryTurns =
         * primaryTurns
         * ×
         * Vbus / Vmpp
         */
        double ratio =
                targetBusVoltage
                        /
                        targetPanelVoltage;

        if (!Double.isFinite(ratio)) {
            return;
        }

        /*
         * 極端な変圧比を制限。
         */
        ratio =
                Math.max(
                        MIN_TURNS_RATIO,
                        Math.min(
                                MAX_TURNS_RATIO,
                                ratio
                        )
                );

        /*
         * 現在の変圧比とほぼ同じなら
         * ElectricalNetworkの行列を書き換えない。
         */
        if (
                Math.abs(
                        ratio
                                -
                                appliedTurnsRatio
                )
                        >
                        EPSILON
        ) {

            double secondaryTurns =
                    transformer.getPrimaryTurns()
                            *
                            ratio;

            transformer.setSecondaryTurns(
                    secondaryTurns
            );

            appliedTurnsRatio =
                    ratio;
        }
    }


    /**
     * ElectricalNetworkの解から
     * 実際のPV電力を取得する。
     *
     * 注意:
     * MPPT最大電力そのものではなく、
     * 実際に回路から取得できた電力を返す。
     */
    public void updateActualPower() {

        /*
         * RegulatedTransformerCouplingの
         * primary currentを取得。
         */
        actualCurrent =
                finitePositive(
                        transformer.getPrimaryCurrent()
                );

        /*
         * 現在のMPPT目標電圧を
         * PV側電圧の近似値として使用。
         *
         * 現在のTransformerCouplingには
         * primary node voltage getterがないため、
         * MPPT目標値を使用する。
         */
        actualVoltage =
                targetPanelVoltage;

        actualPower =
                finitePositive(
                        actualVoltage
                                *
                                actualCurrent
                );

        /*
         * 理論最大電力を超えないようにする。
         */
        if (maximumPower > 0.0) {

            actualPower =
                    Math.min(
                            actualPower,
                            maximumPower
                    );
        }
    }


    /**
     * MPPT制御を1回実行。
     */
    public void update(
            double irradiance,
            double temperature,
            double busVoltage
    ) {

        /*
         * ① MPPTの最大電力点を計算
         */
        updateMaximumPower(
                irradiance,
                temperature
        );

        /*
         * ② DC LINKを基準に
         *    PV側をVmppへ設定
         */
        applyTarget(
                busVoltage
        );

        /*
         * ③ 実際の電力を取得
         */
        updateActualPower();
    }


    private static double finitePositive(
            double value
    ) {

        return Double.isFinite(value)
                &&
                value > 0.0
                ? value
                : 0.0;
    }


    public SolarSpec getSpec() {

        return spec;
    }


    public MPPTController getMppt() {

        return mppt;
    }


    public RegulatedTransformerCoupling getTransformer() {

        return transformer;
    }


    /**
     * MPPTが計算した最大電力。
     */
    public double getMaximumPower() {

        return maximumPower;
    }


    /**
     * MPPT電圧(Vmpp)。
     */
    public double getMaximumPowerVoltage() {

        return maximumPowerVoltage;
    }


    /**
     * MPPT電流(Impp)。
     */
    public double getMaximumPowerCurrent() {

        return maximumPowerCurrent;
    }


    /**
     * PV側の目標電圧。
     */
    public double getTargetPanelVoltage() {

        return targetPanelVoltage;
    }


    /**
     * DC LINK側の目標電圧。
     */
    public double getTargetBusVoltage() {

        return targetBusVoltage;
    }


    /**
     * 現在適用している変圧比。
     */
    public double getAppliedTurnsRatio() {

        return appliedTurnsRatio;
    }


    /**
     * 実際に取得したと計算されるPV電力。
     */
    public double getActualPower() {

        return actualPower;
    }


    /**
     * 実際のPV電流。
     */
    public double getActualCurrent() {

        return actualCurrent;
    }


    /**
     * PV側電圧。
     */
    public double getActualVoltage() {

        return actualVoltage;
    }


    /**
     * GUIなどで使用する電力。
     *
     * 以前はmaximumPowerを返していたが、
     * これでは「発電可能電力」と「実際の発電電力」が
     * 区別できないためactualPowerを返す。
     */
    public double getPower() {

        return actualPower;
    }
}