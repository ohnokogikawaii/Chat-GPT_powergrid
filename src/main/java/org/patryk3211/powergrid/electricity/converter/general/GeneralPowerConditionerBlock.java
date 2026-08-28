package org.patryk3211.powergrid.electricity.converter.general;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.patryk3211.powergrid.electricity.base.ElectricBlockEntity;
import org.patryk3211.powergrid.electricity.base.IElectricEntity.CircuitBuilder;
import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.RegulatedTransformerCoupling;

import java.util.List;


/**
 * General / Residential Power Conditioner.
 *
 * 電力経路
 *
 * PV1 ─┐
 * PV2 ─┼─ MPPT ─→ DC LINK ─┬─ 自家消費用変圧器 ─→ LOAD
 * PV3 ─┘                    │
 *                           └─ 系統連系用変圧器 ─→ GRID
 *
 *
 * 外部端子
 *
 * 0-1 : PV1
 * 2-3 : PV2
 * 4-5 : PV3
 * 6-7 : GRID L/N
 * 8-9 : LOAD L/N
 *
 *
 * 変圧器
 *
 * PV:
 *   100 → 400 V
 *
 * 自家消費:
 *   400 → 200 V
 *
 * 系統:
 *   400 → 200 V
 *
 *
 * 電力優先順位
 *
 * 1. PV
 * 2. 自家消費
 * 3. 余剰電力をGRIDへ
 * 4. PV不足時はGRIDから補う
 */
public class GeneralPowerConditionerBlock
        extends ElectricBlockEntity
        implements IHaveGoggleInformation {


    /*
     * =========================================================
     * 外部端子
     * =========================================================
     */

    private IElectricNode[] pvPlus;
    private IElectricNode[] pvMinus;

    private IElectricNode gridL;
    private IElectricNode gridN;

    private IElectricNode loadL;
    private IElectricNode loadN;


    /*
     * =========================================================
     * DC LINK
     * =========================================================
     */

    private IElectricNode dcLinkPlus;
    private IElectricNode dcLinkMinus;


    /*
     * =========================================================
     * 自家消費側
     * =========================================================
     */

    private IElectricNode selfConsumptionL;
    private IElectricNode selfConsumptionN;


    /*
     * =========================================================
     * 系統連系側
     * =========================================================
     */

    private IElectricNode gridTransformerL;
    private IElectricNode gridTransformerN;


    /*
     * =========================================================
     * 変圧器
     * =========================================================
     */

    private RegulatedTransformerCoupling[] pvTransformers;

    private RegulatedTransformerCoupling selfConsumptionTransformer;

    private RegulatedTransformerCoupling gridTransformer;


    /*
     * =========================================================
     * スイッチ
     * =========================================================
     *
     * 自家消費スイッチ:
     *
     * Transformer secondary → LOAD
     *
     * 系統スイッチ:
     *
     * Transformer secondary → GRID
     */

    private SwitchedWire selfConsumptionPositiveSwitch;
    private SwitchedWire selfConsumptionNegativeSwitch;

    private SwitchedWire gridPositiveSwitch;
    private SwitchedWire gridNegativeSwitch;


    /*
     * =========================================================
     * Controller
     * =========================================================
     */

    private GeneralPowerConditionerController controller;


    /*
     * =========================================================
     * Solar conditions
     * =========================================================
     */

    private double irradiance = 1000.0;

    private double temperature = 25.0;


    /*
     * =========================================================
     * Constructor
     * =========================================================
     */

    public GeneralPowerConditionerBlock(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state
    ) {
        super(type, pos, state);
    }


    /*
     * =========================================================
     * Circuit
     * =========================================================
     */

    @Override
    public void buildCircuit(CircuitBuilder builder) {


        /*
         * =====================================================
         * 外部端子
         * =====================================================
         */

        builder.setTerminalCount(10);


        /*
         * -----------------------------------------------------
         * PV1～PV3
         * -----------------------------------------------------
         */

        pvPlus = new IElectricNode[3];

        pvMinus = new IElectricNode[3];


        for (int i = 0; i < 3; i++) {

            pvPlus[i] =
                    builder.terminalNode(i * 2);

            pvMinus[i] =
                    builder.terminalNode(i * 2 + 1);
        }


        /*
         * -----------------------------------------------------
         * GRID
         * -----------------------------------------------------
         */

        gridL =
                builder.terminalNode(6);

        gridN =
                builder.terminalNode(7);


        /*
         * -----------------------------------------------------
         * LOAD
         * -----------------------------------------------------
         */

        loadL =
                builder.terminalNode(8);

        loadN =
                builder.terminalNode(9);


        /*
         * =====================================================
         * DC LINK
         * =====================================================
         */

        dcLinkPlus =
                builder.addInternalNode();

        dcLinkMinus =
                builder.addInternalNode();


        /*
         * =====================================================
         * 自家消費変圧器二次側
         * =====================================================
         */

        selfConsumptionL =
                builder.addInternalNode();

        selfConsumptionN =
                builder.addInternalNode();


        /*
         * =====================================================
         * 系統変圧器二次側
         * =====================================================
         */

        gridTransformerL =
                builder.addInternalNode();

        gridTransformerN =
                builder.addInternalNode();


        /*
         * =====================================================
         * PV MPPT変圧器
         * =====================================================
         *
         * PV側
         *
         * 100 V
         *
         * ↓
         *
         * DC LINK
         *
         * 400 V
         */

        pvTransformers =
                new RegulatedTransformerCoupling[3];


        for (int i = 0; i < 3; i++) {

            pvTransformers[i] =
                    builder.addInternalNode(
                            RegulatedTransformerCoupling.class,

                            pvPlus[i],
                            pvMinus[i],

                            dcLinkPlus,
                            dcLinkMinus,

                            100.0,
                            400.0
                    );
        }


        /*
         * =====================================================
         * 自家消費用変圧器
         * =====================================================
         *
         * DC LINK
         *
         * 400 V
         *
         * ↓
         *
         * 200 V
         *
         * LOAD
         */

        selfConsumptionTransformer =
                builder.addInternalNode(
                        RegulatedTransformerCoupling.class,

                        dcLinkPlus,
                        dcLinkMinus,

                        selfConsumptionL,
                        selfConsumptionN,

                        400.0,
                        200.0
                );


        /*
         * =====================================================
         * 系統連系用変圧器
         * =====================================================
         *
         * DC LINK
         *
         * 400 V
         *
         * ↓
         *
         * GRID
         *
         * 系統電圧に応じて
         * ControllerがsecondaryTurnsを変更する。
         */

        gridTransformer =
                builder.addInternalNode(
                        RegulatedTransformerCoupling.class,

                        dcLinkPlus,
                        dcLinkMinus,

                        gridTransformerL,
                        gridTransformerN,

                        400.0,
                        200.0
                );


        /*
         * =====================================================
         * 自家消費スイッチ
         * =====================================================
         *
         * 重要:
         *
         * 以前:
         *
         * DC LINK → selfConsumptionL
         *
         * となっていた。
         *
         * これは変圧器をバイパスしてしまう。
         *
         * 正しくは:
         *
         * selfConsumptionL → LOAD L
         * selfConsumptionN → LOAD N
         */

        selfConsumptionPositiveSwitch =
                builder.connectSwitch(
                        0.0001f,
                        selfConsumptionL,
                        loadL,
                        true
                );


        selfConsumptionNegativeSwitch =
                builder.connectSwitch(
                        0.0001f,
                        selfConsumptionN,
                        loadN,
                        true
                );


        /*
         * =====================================================
         * 系統スイッチ
         * =====================================================
         *
         * 系統変圧器二次側と
         * GRID端子を接続する。
         */

        gridPositiveSwitch =
                builder.connectSwitch(
                        0.0001f,
                        gridTransformerL,
                        gridL,
                        false
                );


        gridNegativeSwitch =
                builder.connectSwitch(
                        0.0001f,
                        gridTransformerN,
                        gridN,
                        false
                );


        /*
         * =====================================================
         * Controller
         * =====================================================
         *
         * 13引数構成
         *
         * 1  pvTransformers
         * 2  selfConsumptionTransformer
         * 3  gridTransformer
         * 4  selfConsumptionPositiveSwitch
         * 5  selfConsumptionNegativeSwitch
         * 6  gridPositiveSwitch
         * 7  gridNegativeSwitch
         * 8  dcLinkPlus
         * 9  dcLinkMinus
         * 10 selfConsumptionL
         * 11 selfConsumptionN
         * 12 gridL
         * 13 gridN
         */

        controller =
                new GeneralPowerConditionerController(

                        pvTransformers,

                        selfConsumptionTransformer,

                        gridTransformer,

                        selfConsumptionPositiveSwitch,
                        selfConsumptionNegativeSwitch,

                        gridPositiveSwitch,
                        gridNegativeSwitch,

                        dcLinkPlus,
                        dcLinkMinus,

                        selfConsumptionL,
                        selfConsumptionN,

                        gridL,
                        gridN
                );
    }


    /*
     * =========================================================
     * Electrical Tick
     * =========================================================
     */

    @Override
    public void electricalTick() {

        if (controller == null)
            return;


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

    public GeneralPowerConditionerController getController() {

        return controller;
    }


    /*
     * =========================================================
     * Power
     * =========================================================
     */

    public double getTotalAvailablePower() {

        return controller == null
                ? 0.0
                : controller.getTotalAvailablePower();
    }


    public double getPvPower() {

        return controller == null
                ? 0.0
                : controller.getPvPower();
    }


    public double getLoadPower() {

        return controller == null
                ? 0.0
                : controller.getLoadPower();
    }


    public double getGridPower() {

        return controller == null
                ? 0.0
                : controller.getGridPower();
    }


    public double getSelfConsumptionPower() {

        return controller == null
                ? 0.0
                : controller.getSelfConsumptionPower();
    }


    public double getGridExportPower() {

        return controller == null
                ? 0.0
                : controller.getGridExportPower();
    }


    public double getGridImportPower() {

        return controller == null
                ? 0.0
                : controller.getGridImportPower();
    }


    /*
     * =========================================================
     * Voltage / Current
     * =========================================================
     */

    public double getGridVoltage() {

        return controller == null
                ? 0.0
                : controller.getGridVoltage();
    }


    public double getGridCurrent() {

        return controller == null
                ? 0.0
                : controller.getGridCurrent();
    }


    public double getAcVoltage() {

        return controller == null
                ? 0.0
                : controller.getAcVoltage();
    }


    public double getDcLinkVoltage() {

        return controller == null
                ? 0.0
                : controller.getDcLinkVoltage();
    }


    public boolean isGridAvailable() {

        return controller != null
                &&
                controller.isGridAvailable();
    }


    /*
     * =========================================================
     * MPPT
     * =========================================================
     */

    public double getMpptMaximumPower(int index) {

        return controller == null
                ? 0.0
                : controller
                .getChannel(index)
                .getMaximumPower();
    }


    public double getMpptPower(int index) {

        return controller == null
                ? 0.0
                : controller
                .getChannel(index)
                .getPower();
    }


    /*
     * =========================================================
     * Goggle Information
     * =========================================================
     */

    @Override
    public boolean addToGoggleTooltip(
            List<Component> tooltip,
            boolean sneaking
    ) {

        tooltip.add(
                Component.literal(
                        "General Power Conditioner"
                )
        );


        tooltip.add(
                Component.literal(
                        String.format(
                                "PV: %.2f W",
                                getPvPower()
                        )
                )
        );


        tooltip.add(
                Component.literal(
                        String.format(
                                "LOAD: %.2f W",
                                getLoadPower()
                        )
                )
        );


        tooltip.add(
                Component.literal(
                        String.format(
                                "SELF: %.2f W",
                                getSelfConsumptionPower()
                        )
                )
        );


        tooltip.add(
                Component.literal(
                        String.format(
                                "GRID EXPORT: %.2f W",
                                getGridExportPower()
                        )
                )
        );


        tooltip.add(
                Component.literal(
                        String.format(
                                "GRID IMPORT: %.2f W",
                                getGridImportPower()
                        )
                )
        );


        tooltip.add(
                Component.literal(
                        String.format(
                                "DC LINK: %.2f V",
                                getDcLinkVoltage()
                        )
                )
        );


        tooltip.add(
                Component.literal(
                        String.format(
                                "AC: %.2f V",
                                getAcVoltage()
                        )
                )
        );


        tooltip.add(
                Component.literal(
                        String.format(
                                "GRID: %.2f V / %.2f A",
                                getGridVoltage(),
                                getGridCurrent()
                        )
                )
        );


        tooltip.add(
                Component.literal(
                        "Grid: "
                                +
                                (
                                        isGridAvailable()
                                                ? "AVAILABLE"
                                                : "DISCONNECTED"
                                )
                )
        );


        return true;
    }
}