package org.patryk3211.powergrid.electricity.sim.node;

import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.sim.solver.IAdmittanceAdder;
import org.patryk3211.powergrid.electricity.sim.solver.IResidualAdder;
import org.patryk3211.powergrid.electricity.sim.solver.IStaticResidual;

import java.util.Collection;
import java.util.List;

public class VoltageSourceCoupling
        extends CouplingNode
        implements IStaticResidual {

    protected final IElectricNode positive;

    @Nullable
    protected final IElectricNode negative;
    private double mpptMaximumPower = 0.0;
    private double voltage;
    private float resistance;
    /*
     * MPPT対応電源かどうか。
     *
     * SolarBlockEntityだけがtrueにする。
     */
    private boolean mpptSource = false;
    private static final float BLOCKED_RESISTANCE = 1_000_000f;

    /*
     * BMSによる方向別遮断
     */
    private boolean chargeBlocked;
    private boolean dischargeBlocked;


    public VoltageSourceCoupling(
            IElectricNode positive,
            @Nullable IElectricNode negative,
            float resistance
    ) {
        this.positive = positive;
        this.negative = negative;
        this.resistance = Math.max(0.000001f, resistance);
    }

    public void setMpptSource(
            boolean mpptSource
    ) {
        this.mpptSource =
                mpptSource;
    }

    public boolean isMpptSource() {
        return mpptSource;
    }

    public void setMpptMaximumPower(
            double power
    ) {
        this.mpptMaximumPower =
                Math.max(
                        0.0,
                        power
                );
    }

    public double getMpptMaximumPower() {
        return mpptMaximumPower;
    }

    public VoltageSourceCoupling(
            IElectricNode positive,
            @Nullable IElectricNode negative,
            Number resistance
    ) {
        this.positive = positive;
        this.negative = negative;
        this.resistance =
                Math.max(0.000001f, resistance.floatValue());
    }


    public VoltageSourceCoupling(
            IElectricNode positive,
            @Nullable IElectricNode negative,
            float resistance,
            float voltage
    ) {
        this(positive, negative, resistance);
        setVoltage(voltage);
    }


    @Override
    public boolean isSource() {
        return true;
    }


    public void setVoltage(double voltage) {
        this.voltage = voltage;
    }


    public void setResistance(float resistance) {

        float newResistance =
                Math.max(0.000001f, resistance);

        float oldEffectiveResistance =
                getEffectiveResistance();

        this.resistance = newResistance;

        float newEffectiveResistance =
                getEffectiveResistance();

        if (network == null)
            return;

        float delta =
                newEffectiveResistance
                        - oldEffectiveResistance;

        if (Math.abs(delta) < 0.000001f)
            return;

        network.alterConductanceMatrix(
                index,
                index,
                -delta
        );
    }


    public void setCurrentDirectionBlocked(
            boolean chargeBlocked,
            boolean dischargeBlocked
    ) {
        this.chargeBlocked = chargeBlocked;
        this.dischargeBlocked = dischargeBlocked;

        updateNetworkResistance();
    }


    public void setChargeBlocked(boolean blocked) {

        this.chargeBlocked = blocked;

        updateNetworkResistance();
    }


    public void setDischargeBlocked(boolean blocked) {

        this.dischargeBlocked = blocked;

        updateNetworkResistance();
    }


    public boolean isChargeBlocked() {
        return chargeBlocked;
    }


    public boolean isDischargeBlocked() {
        return dischargeBlocked;
    }


    /*
     * 実際に流れる電流方向を、
     * 現在の端子電圧から判定する。
     *
     * I = (Vexternal - Vbattery) / R
     *
     * I > 0 : 充電
     * I < 0 : 放電
     */
    private float getEffectiveResistance() {

        double positiveVoltage =
                positive.getVoltage();

        double negativeVoltage =
                negative != null
                        ? negative.getVoltage()
                        : 0.0;

        double externalVoltage =
                positiveVoltage - negativeVoltage;

        /*
         * 充電方向
         */
        if (
                chargeBlocked
                        && externalVoltage > voltage
        ) {
            return BLOCKED_RESISTANCE;
        }

        /*
         * 放電方向
         */
        if (
                dischargeBlocked
                        && externalVoltage < voltage
        ) {
            return BLOCKED_RESISTANCE;
        }

        return resistance;
    }


    private void updateNetworkResistance() {

        if (network == null)
            return;

        /*
         * 重要：
         *
         * 現在の抵抗と有効抵抗の差だけ変更する。
         */
        float effectiveResistance =
                getEffectiveResistance();

        float delta =
                effectiveResistance - resistance;

        if (Math.abs(delta) < 0.000001f)
            return;

        network.alterConductanceMatrix(
                index,
                index,
                -delta
        );
    }


    @Override
    public void couple(IAdmittanceAdder admittance) {

        admittance.add(
                index,
                positive.getIndex(),
                1
        );

        admittance.add(
                positive.getIndex(),
                index,
                1
        );

        float effectiveResistance =
                getEffectiveResistance();

        admittance.add(
                index,
                index,
                -effectiveResistance
        );

        if (negative != null) {

            admittance.add(
                    index,
                    negative.getIndex(),
                    -1
            );

            admittance.add(
                    negative.getIndex(),
                    index,
                    -1
            );
        }
    }


    @Override
    public Collection<IElectricNode> coupledNodes() {

        if (negative == null)
            return List.of(positive);

        return List.of(
                positive,
                negative
        );
    }


    /*
     * ソルバー上の実際の電流。
     *
     * ここでは0に書き換えない。
     * BMSの遮断は回路抵抗側で処理する。
     */
    public double getCurrent() {
        return getStateValue();
    }


    public double getVoltage() {
        return voltage;
    }


    public float getResistance() {
        return resistance;
    }


    public float getEffectiveResistanceValue() {
        return getEffectiveResistance();
    }


    public IElectricNode getPositive() {
        return positive;
    }


    @Nullable
    public IElectricNode getNegative() {
        return negative;
    }


    @Override
    public void addStaticResidual(
            IResidualAdder residual
    ) {
        residual.add(index, voltage);
    }


    @Override
    public List<INode> affectedNodes() {

        if (negative != null) {
            return List.of(
                    positive,
                    negative
            );
        }

        return List.of(
                positive
        );
    }


    @Override
    public String toString() {

        if (negative != null) {

            return String.format(
                    "VoltageSource(%s %s V=%g)",
                    positive,
                    negative,
                    voltage
            );
        }

        return String.format(
                "VoltageSource(%s V=%g)",
                positive,
                voltage
        );
    }
}