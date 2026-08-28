package org.patryk3211.powergrid.electricity.converter;

import org.patryk3211.powergrid.electricity.sim.node.DcDcConverterCoupling;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.solar.SolarRegistry;

/** Five-string, export-only commercial PV inverter controller. */
public final class CommercialPowerConditionerController {
    public static final int CHANNEL_COUNT = 5;
    private static final double DC_LINK_TARGET_VOLTAGE = 400.0;
    private final CommercialPowerConditionerMpptChannel[] channels;
    private final CommercialPowerConditionerGridController gridController;
    private final IElectricNode dcLinkPlus;
    private final IElectricNode dcLinkMinus;
    private double totalAvailablePower;
    private double totalOutputPower;
    private double dcLinkVoltage;

    public CommercialPowerConditionerController(DcDcConverterCoupling[] mpptConverters,
            DcDcConverterCoupling gridConverter, IElectricNode dcLinkPlus, IElectricNode dcLinkMinus,
            IElectricNode gridPositiveNode, IElectricNode gridNegativeNode) {
        if (mpptConverters == null || mpptConverters.length != CHANNEL_COUNT)
            throw new IllegalArgumentException("Commercial Power Conditioner requires 5 MPPT converters");
        this.dcLinkPlus = dcLinkPlus;
        this.dcLinkMinus = dcLinkMinus;
        channels = new CommercialPowerConditionerMpptChannel[CHANNEL_COUNT];
        for (int i = 0; i < CHANNEL_COUNT; i++)
            channels[i] = new CommercialPowerConditionerMpptChannel(SolarRegistry.LVYUAN_410W, mpptConverters[i]);
        gridController = new CommercialPowerConditionerGridController(gridConverter, gridPositiveNode, gridNegativeNode);
    }

    public void update(double irradiance, double temperature) {
        totalAvailablePower = 0.0;
        for (var channel : channels) {
            channel.update(DC_LINK_TARGET_VOLTAGE, irradiance, temperature, 1.0);
            totalAvailablePower += channel.getPower();
        }
        dcLinkVoltage = readVoltage(dcLinkPlus, dcLinkMinus);
        gridController.update(totalAvailablePower, dcLinkVoltage);
        totalOutputPower = gridController.getGridPower();
    }

    private static double readVoltage(IElectricNode positive, IElectricNode negative) {
        if (positive == null || negative == null) return 0.0;
        double voltage = positive.getVoltage() - negative.getVoltage();
        return Double.isFinite(voltage) ? Math.abs(voltage) : 0.0;
    }
    public CommercialPowerConditionerMpptChannel getChannel(int index) { return channels[index]; }
    public double getTotalAvailablePower() { return totalAvailablePower; }
    public double getTotalOutputPower() { return totalOutputPower; }
    public double getDcLinkVoltage() { return dcLinkVoltage; }
    public double getTargetDcLinkVoltage() { return DC_LINK_TARGET_VOLTAGE; }
    public CommercialPowerConditionerGridController getGridController() { return gridController; }
}
