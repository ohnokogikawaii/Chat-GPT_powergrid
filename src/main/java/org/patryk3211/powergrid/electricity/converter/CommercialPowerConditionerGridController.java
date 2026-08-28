package org.patryk3211.powergrid.electricity.converter;

import org.patryk3211.powergrid.electricity.sim.node.DcDcConverterCoupling;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;

/** Export-only grid interface.  It never draws power from GRID. */
public final class CommercialPowerConditionerGridController {
    public static final double MIN_OUTPUT_VOLTAGE = 100.0;
    public static final double MAX_OUTPUT_VOLTAGE = 6600.0;
    private static final double START_POWER = 1.0;
    private static final double EFFICIENCY = 0.98;
    private final DcDcConverterCoupling converter;
    private final IElectricNode gridPositiveNode, gridNegativeNode;
    private double gridVoltage, gridCurrent, gridPower, targetOutputVoltage;
    private boolean gridConnected = false, gridTieEnabled = true;
    private double manualOutputVoltage = MIN_OUTPUT_VOLTAGE;

    public CommercialPowerConditionerGridController(DcDcConverterCoupling converter,
            IElectricNode gridPositiveNode, IElectricNode gridNegativeNode) {
        this.converter = converter;
        this.gridPositiveNode = gridPositiveNode;
        this.gridNegativeNode = gridNegativeNode;
    }
    public void update(double availablePower, double dcLinkVoltage) {
        gridVoltage = readGridVoltage();
        boolean hasPv = Double.isFinite(availablePower) && availablePower >= START_POWER;
        boolean gridNormal = gridVoltage >= MIN_OUTPUT_VOLTAGE && gridVoltage <= MAX_OUTPUT_VOLTAGE;
        if (gridTieEnabled && (!hasPv || !gridNormal || dcLinkVoltage <= 0.001)) {
            stop(); return;
        }
        if (gridTieEnabled) targetOutputVoltage = gridVoltage;
        else targetOutputVoltage = clampOutputVoltage(manualOutputVoltage);
        if (!gridTieEnabled && (!hasPv || dcLinkVoltage <= 0.001)) { stop(); return; }
        converter.setEfficiency(EFFICIENCY);
        converter.setOutputVoltage(targetOutputVoltage);
        converter.setAvailableInputPower(Math.max(0.0, availablePower));
        converter.setOutputCurrent(targetOutputVoltage > 0.001 ? availablePower * EFFICIENCY / targetOutputVoltage : 0.0);
        gridConnected = true;
        gridCurrent = Math.max(0.0, converter.getOutputCurrent());
        gridPower = targetOutputVoltage * gridCurrent;
    }
    private void stop() {
        converter.setAvailableInputPower(0.0); converter.setOutputCurrent(0.0);
        gridConnected = false; gridCurrent = 0.0; gridPower = 0.0; targetOutputVoltage = 0.0;
    }
    private double readGridVoltage() {
        if (gridPositiveNode == null || gridNegativeNode == null) return 0.0;
        double voltage = gridPositiveNode.getVoltage() - gridNegativeNode.getVoltage();
        return Double.isFinite(voltage) ? Math.abs(voltage) : 0.0;
    }
    private static double clampOutputVoltage(double voltage) {
        if (!Double.isFinite(voltage)) return MIN_OUTPUT_VOLTAGE;
        voltage = Math.max(MIN_OUTPUT_VOLTAGE, Math.min(MAX_OUTPUT_VOLTAGE, voltage));
        return Math.round(voltage / 10.0) * 10.0;
    }
    public boolean isGridTieEnabled() { return gridTieEnabled; }
    public void setGridTieEnabled(boolean enabled) { gridTieEnabled = enabled; if (!enabled) stop(); }
    public void setGridTieEnabledFromGui(boolean enabled) { setGridTieEnabled(enabled); }
    public double getManualOutputVoltage() { return manualOutputVoltage; }
    public void setManualOutputVoltage(double voltage) { manualOutputVoltage = clampOutputVoltage(voltage); }
    public void setManualOutputVoltageFromGui(double voltage) { setManualOutputVoltage(voltage); }
    public double getTargetOutputVoltage() { return targetOutputVoltage; }
    public double getDcLinkVoltage() { return 0.0; }
    public double getGridVoltage() { return gridVoltage; }
    public double getGridCurrent() { return gridCurrent; }
    public double getGridPower() { return gridPower; }
    public double getOutputLimit() { return gridConnected ? 1.0 : 0.0; }
    public boolean isGridConnected() { return gridConnected; }
    public double calculateOutputLimit() { return getOutputLimit(); }
}
