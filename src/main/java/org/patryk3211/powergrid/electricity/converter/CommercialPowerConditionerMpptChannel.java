package org.patryk3211.powergrid.electricity.converter;

import org.patryk3211.powergrid.electricity.sim.node.DcDcConverterCoupling;
import org.patryk3211.powergrid.electricity.solar.MPPTController;
import org.patryk3211.powergrid.electricity.solar.SolarSpec;

/** One PV string MPPT stage which transfers real limited power to the DC link. */
public final class CommercialPowerConditionerMpptChannel {
    private static final double EFFICIENCY = 0.98;
    private final MPPTController mppt;
    private final DcDcConverterCoupling converter;
    private double maximumPower, maximumPowerVoltage, maximumPowerCurrent;
    private double voltage, current, power, targetDcLinkVoltage, curtailment;

    public CommercialPowerConditionerMpptChannel(SolarSpec spec, DcDcConverterCoupling converter) {
        this.mppt = new MPPTController(spec);
        this.converter = converter;
    }
    public void update(double targetDcLinkVoltage, double irradiance, double temperature, double availableRatio) {
        mppt.update(irradiance, temperature);
        maximumPower = positive(mppt.getMaximumPower());
        maximumPowerVoltage = positive(mppt.getVoltage());
        maximumPowerCurrent = positive(mppt.getCurrent());
        this.targetDcLinkVoltage = Math.max(0.0, targetDcLinkVoltage);
        curtailment = Math.max(0.0, Math.min(1.0, availableRatio));
        double availablePower = maximumPower * curtailment;
        converter.setEfficiency(EFFICIENCY);
        converter.setOutputVoltage(this.targetDcLinkVoltage);
        converter.setAvailableInputPower(availablePower);
        converter.setOutputCurrent(this.targetDcLinkVoltage > 0.001 ? availablePower * EFFICIENCY / this.targetDcLinkVoltage : 0.0);
        voltage = maximumPowerVoltage;
        current = positive(converter.getInputCurrent());
        power = Math.min(availablePower, voltage * current);
    }
    private static double positive(double value) { return Double.isFinite(value) ? Math.max(0.0, value) : 0.0; }
    public MPPTController getMppt() { return mppt; }
    public DcDcConverterCoupling getConverter() { return converter; }
    public double getVoltage() { return voltage; }
    public double getCurrent() { return current; }
    public double getPower() { return power; }
    public double getMaximumPower() { return maximumPower; }
    public double getMaximumPowerVoltage() { return maximumPowerVoltage; }
    public double getMaximumPowerCurrent() { return maximumPowerCurrent; }
    public double getMaximumVoltage() { return maximumPowerVoltage; }
    public double getMaximumCurrent() { return maximumPowerCurrent; }
    public double getTargetPanelVoltage() { return maximumPowerVoltage; }
    public double getTargetDcLinkVoltage() { return targetDcLinkVoltage; }
    public double getCurtailment() { return curtailment; }
}
