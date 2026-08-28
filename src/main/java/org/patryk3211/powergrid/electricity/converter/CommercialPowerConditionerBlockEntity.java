package org.patryk3211.powergrid.electricity.converter;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.DcDcConverterCoupling;

import java.util.List;

public class CommercialPowerConditionerBlockEntity
        extends ElectricBlockEntity
        implements IHaveGoggleInformation,
        IMultiScreenHandlerFactory {

    /*
     * =========================================================
     * 外部端子
     * =========================================================
     *
     * 0  PV1+
     * 1  PV1-
     * 2  PV2+
     * 3  PV2-
     * 4  PV3+
     * 5  PV3-
     * 6  PV4+
     * 7  PV4-
     * 8  PV5+
     * 9  PV5-
     * 10 GRID+
     * 11 GRID-
     */

    private IElectricNode[] pvPlus;

    private IElectricNode[] pvMinus;

    private IElectricNode gridPlus;

    private IElectricNode gridMinus;


    /*
     * =========================================================
     * 共通DC幹線
     * =========================================================
     */

    private IElectricNode dcLinkPlus;

    private IElectricNode dcLinkMinus;


    /*
     * =========================================================
     * 系統側内部
     * =========================================================
     */

    private DcDcConverterCoupling[] mpptConverters;

    /** DC LINKからGRIDへ実電力を注入するインバータ。 */
    private DcDcConverterCoupling gridConverter;


    /*
     * =========================================================
     * Controller
     * =========================================================
     */

    private CommercialPowerConditionerController controller;


    private double irradiance = 1000.0;

    private double temperature = 25.0;


    public CommercialPowerConditionerBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {

        super(
                type,
                pos,
                state
        );


        setLazyTickRate(5);
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

        builder.setTerminalCount(12);

        /*
         * The new converter coupling is temporarily isolated.  A coupling
         * spanning all PV ports and the grid port must first be validated in
         * its own test network; otherwise a non-convergent residual can pull
         * every externally connected source to 0 V.
         */
        controller = null;



        /*
         * =====================================================
         * PV端子
         * =====================================================
         */

        pvPlus =
                new IElectricNode[5];

        pvMinus =
                new IElectricNode[5];


        for (int i = 0; i < 5; i++) {

            int plusIndex =
                    i * 2;

            int minusIndex =
                    plusIndex + 1;


            pvPlus[i] =
                    builder.terminalNode(
                            plusIndex
                    );


            pvMinus[i] =
                    builder.terminalNode(
                            minusIndex
                    );
        }


        /*
         * =====================================================
         * GRID端子
         * =====================================================
         */

        gridPlus =
                builder.terminalNode(10);

        gridMinus =
                builder.terminalNode(11);


        /*
         * =====================================================
         * DC LINK
         * =====================================================
         */

        dcLinkPlus =
                builder.addInternalNode();

        dcLinkMinus =
                builder.addInternalNode();


        /* Each MPPT stage consumes PV input and injects real DC-link power. */
        mpptConverters = new DcDcConverterCoupling[5];


        for (int i = 0; i < 5; i++) {

            mpptConverters[i] =
                    builder.addInternalNode(
                            DcDcConverterCoupling.class,

                            pvPlus[i],
                            pvMinus[i],

                            dcLinkPlus,
                            dcLinkMinus

                    );
        }


        /*
         * =====================================================
         * 幹線変圧器
         * =====================================================
         *
         * DC LINK
         *      ↓
         * 400V共通幹線
         *      ↓
         * 幹線変圧器
         *      ↓
         * GRID
         */

        gridConverter =
                builder.addInternalNode(
                        DcDcConverterCoupling.class,

                        dcLinkPlus,
                        dcLinkMinus,

                        gridPlus,
                        gridMinus
                );


        /*
         * =====================================================
         * Controller
         * =====================================================
         */

        controller =
                new CommercialPowerConditionerController(

                        mpptConverters,

                        gridConverter,

                        dcLinkPlus,
                        dcLinkMinus,

                        gridPlus,
                        gridMinus
                );
    }


    /*
     * =========================================================
     * 電気Tick
     * =========================================================
     */

    @Override
    public void electricalTick() {

        if (controller == null)
            return;


        irradiance =
                1000.0;

        temperature =
                25.0;


        controller.update(
                irradiance,
                temperature
        );


        setChanged();
    }


    /*
     * =========================================================
     * Controller
     * =========================================================
     */

    public CommercialPowerConditionerController
    getController() {

        return controller;
    }


    public double getTotalAvailablePower() {

        if (controller == null)
            return 0.0;


        return controller
                .getTotalAvailablePower();
    }


    public double getOutputPower() {

        if (controller == null)
            return 0.0;


        return controller
                .getTotalOutputPower();
    }


    public double getGridVoltage() {

        if (controller == null)
            return 0.0;


        return controller
                .getGridController()
                .getGridVoltage();
    }


    public double getGridCurrent() {

        if (controller == null)
            return 0.0;


        return controller
                .getGridController()
                .getGridCurrent();
    }


    public double getDcLinkVoltage() {

        if (controller == null)
            return 0.0;


        return controller
                .getDcLinkVoltage();
    }


    public double getTargetDcLinkVoltage() {

        if (controller == null)
            return 400.0;


        return controller
                .getTargetDcLinkVoltage();
    }


    public boolean isGridConnected() {

        return controller != null
                &&
                controller
                        .getGridController()
                        .isGridConnected();
    }


    public boolean isGridTieEnabled() {

        return controller != null
                &&
                controller
                        .getGridController()
                        .isGridTieEnabled();
    }


    public void setGridTieEnabled(
            boolean enabled
    ) {

        if (controller == null)
            return;


        controller
                .getGridController()
                .setGridTieEnabledFromGui(
                        enabled
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


    public double getManualOutputVoltage() {

        if (controller == null)
            return 100.0;


        return controller
                .getGridController()
                .getManualOutputVoltage();
    }


    public void setManualOutputVoltage(
            double voltage
    ) {

        if (controller == null)
            return;


        controller
                .getGridController()
                .setManualOutputVoltageFromGui(
                        voltage
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


    public double getTargetOutputVoltage() {

        if (controller == null)
            return 0.0;


        return controller
                .getGridController()
                .getTargetOutputVoltage();
    }


    /*
     * =========================================================
     * MPPT
     * =========================================================
     */

    public double getMpptMaximumPower(
            int index
    ) {

        if (
                controller == null
                        ||
                        index < 0
                        ||
                        index >= 5
        ) {

            return 0.0;
        }


        return controller
                .getChannel(index)
                .getMaximumPower();
    }


    public double getMpptMaximumVoltage(
            int index
    ) {

        if (
                controller == null
                        ||
                        index < 0
                        ||
                        index >= 5
        ) {

            return 0.0;
        }


        return controller
                .getChannel(index)
                .getMaximumPowerVoltage();
    }


    public double getMpptMaximumCurrent(
            int index
    ) {

        if (
                controller == null
                        ||
                        index < 0
                        ||
                        index >= 5
        ) {

            return 0.0;
        }


        return controller
                .getChannel(index)
                .getMaximumPowerCurrent();
    }


    public double getMpptPower(
            int index
    ) {

        if (
                controller == null
                        ||
                        index < 0
                        ||
                        index >= 5
        ) {

            return 0.0;
        }


        return controller
                .getChannel(index)
                .getPower();
    }


    public double getMpptVoltage(
            int index
    ) {

        if (
                controller == null
                        ||
                        index < 0
                        ||
                        index >= 5
        ) {

            return 0.0;
        }


        return controller
                .getChannel(index)
                .getVoltage();
    }


    public double getMpptCurrent(
            int index
    ) {

        if (
                controller == null
                        ||
                        index < 0
                        ||
                        index >= 5
        ) {

            return 0.0;
        }


        return controller
                .getChannel(index)
                .getCurrent();
    }


    public double getMpptTargetDcLinkVoltage(
            int index
    ) {

        if (
                controller == null
                        ||
                        index < 0
                        ||
                        index >= 5
        ) {

            return 0.0;
        }


        return controller
                .getChannel(index)
                .getTargetDcLinkVoltage();
    }


    /*
     * =========================================================
     * NBT
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
                "GridTieEnabled",
                isGridTieEnabled()
        );


        tag.putDouble(
                "ManualOutputVoltage",
                getManualOutputVoltage()
        );
    }


    @Override
    protected void read(
            CompoundTag tag,
            boolean clientPacket
    ) {

        super.read(
                tag,
                clientPacket
        );


        boolean gridTieEnabled =
                tag.contains(
                        "GridTieEnabled"
                )
                        &&
                        tag.getBoolean(
                                "GridTieEnabled"
                        );


        double manualVoltage =
                tag.contains(
                        "ManualOutputVoltage"
                )
                        ?
                        tag.getDouble(
                                "ManualOutputVoltage"
                        )
                        :
                        100.0;


        if (controller != null) {

            controller
                    .getGridController()
                    .setGridTieEnabled(
                            gridTieEnabled
                    );


            controller
                    .getGridController()
                    .setManualOutputVoltage(
                            manualVoltage
                    );
        }
    }


    /*
     * =========================================================
     * Goggles
     * =========================================================
     */

    @Override
    public boolean addToGoggleTooltip(
            List<Component> tooltip,
            boolean sneaking
    ) {

        tooltip.add(
                Component.literal(
                        "Commercial Power Conditioner"
                )
        );


        tooltip.add(
                Component.literal(
                        String.format(
                                "PV Available: %.2f W",
                                getTotalAvailablePower()
                        )
                )
        );


        tooltip.add(
                Component.literal(
                        String.format(
                                "DC Link: %.2f / %.2f V",
                                getDcLinkVoltage(),
                                getTargetDcLinkVoltage()
                        )
                )
        );


        tooltip.add(
                Component.literal(
                        String.format(
                                "Grid Voltage: %.2f V",
                                getGridVoltage()
                        )
                )
        );


        tooltip.add(
                Component.literal(
                        String.format(
                                "Grid Current: %.2f A",
                                getGridCurrent()
                        )
                )
        );


        tooltip.add(
                Component.literal(
                        String.format(
                                "Grid Output: %.2f W",
                                getOutputPower()
                        )
                )
        );


        tooltip.add(
                Component.literal(
                        "Grid Tie: "
                                +
                                (
                                        isGridTieEnabled()
                                                ? "ON"
                                                : "OFF"
                                )
                )
        );


        tooltip.add(
                Component.literal(
                        "Grid: "
                                +
                                (
                                        isGridConnected()
                                                ? "CONNECTED"
                                                : "OFF"
                                )
                )
        );


        tooltip.add(
                Component.literal(
                        String.format(
                                "Target Voltage: %.2f V",
                                getTargetOutputVoltage()
                        )
                )
        );


        if (controller != null) {

            for (int i = 0; i < 5; i++) {

                CommercialPowerConditionerMpptChannel channel =
                        controller.getChannel(i);


                tooltip.add(
                        Component.literal(
                                String.format(
                                        "MPPT%d: %.2f W / %.2f W",
                                        i + 1,
                                        channel.getPower(),
                                        channel.getMaximumPower()
                                )
                        )
                );


                tooltip.add(
                        Component.literal(
                                String.format(
                                        "MPPT%d: %.2f V %.2f A → %.2f V",
                                        i + 1,
                                        channel.getVoltage(),
                                        channel.getCurrent(),
                                        channel.getTargetDcLinkVoltage()
                                )
                        )
                );
            }
        }


        return true;
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

        return new CommercialPowerConditionerMenu(
                ModdedMenus.COMMERCIAL_POWER_CONDITIONER.get(),
                syncId,
                playerInventory,
                this
        );
    }


    @Override
    public Component getDisplayName() {

        return Component.literal(
                "Commercial Power Conditioner"
        );
    }
}
