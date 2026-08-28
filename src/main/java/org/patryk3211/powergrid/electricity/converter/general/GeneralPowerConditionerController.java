package org.patryk3211.powergrid.electricity.converter.general;

import org.patryk3211.powergrid.electricity.sim.SwitchedWire;
import org.patryk3211.powergrid.electricity.sim.node.IElectricNode;
import org.patryk3211.powergrid.electricity.sim.node.RegulatedTransformerCoupling;
import org.patryk3211.powergrid.electricity.solar.SolarRegistry;

/**
 * General / Residential Power Conditioner Controller.
 *
 * 電力経路:
 *
 * PV1 ─┐
 * PV2 ─┼→ MPPT → DC LINK → 自家消費用変圧器 → LOAD
 * PV3 ─┘             │
 *                    └────→ 系統連系用変圧器 → GRID
 *
 * 基本動作:
 *
 * 1. 各PVをMPPT動作させる
 * 2. PV電力をDC LINKへ集約
 * 3. 自家消費を最優先
 * 4. 余剰電力をGRIDへ送電
 * 5. PV不足時はGRIDから電力を取り込みLOADへ供給
 * 6. GRID停電時は系統側を切断
 *
 * このクラスでは電流を直接設定しない。
 * 電圧・変圧比・スイッチ状態を制御し、
 * 実際の電流はElectricalNetworkの解によって決定される。
 */
public final class GeneralPowerConditionerController {

    public static final int CHANNEL_COUNT = 3;

    /*
     * =========================================================
     * 電圧設定
     * =========================================================
     */

    /**
     * DC LINKの目標電圧。
     *
     * 現在は400Vを基準にする。
     * 実際のDC LINK電圧は毎Tick取得する。
     */
    public static final double DC_LINK_TARGET_VOLTAGE = 400.0;

    /**
     * 自家消費側の目標電圧。
     */
    public static final double SELF_CONSUMPTION_TARGET_VOLTAGE = 200.0;

    /**
     * 系統側への送電時に使用する電圧差。
     *
     * 系統電圧そのものより少し高くすることで
     * GRID方向へ電力を流す。
     */
    private static final double EXPORT_VOLTAGE_MARGIN = 2.0;

    /**
     * 系統から取り込むときの電圧差。
     */
    private static final double IMPORT_VOLTAGE_MARGIN = 2.0;

    /*
     * =========================================================
     * 系統判定
     * =========================================================
     */

    private static final double GRID_MIN_VOLTAGE = 50.0;
    private static final double GRID_MAX_VOLTAGE = 300.0;

    private static final double MIN_VOLTAGE = 0.001;

    /*
     * =========================================================
     * 最大電流
     * =========================================================
     *
     * シミュレーション上の表示・制限用。
     */
    private static final double MAX_GRID_CURRENT = 1000.0;


    /*
     * =========================================================
     * MPPT
     * =========================================================
     */

    private final GeneralPowerConditionerMpptChannel[] mppt;


    /*
     * =========================================================
     * 変圧器
     * =========================================================
     */

    /**
     * DC LINK → 自家消費
     */
    private final RegulatedTransformerCoupling selfConsumptionTransformer;

    /**
     * DC LINK → GRID
     */
    private final RegulatedTransformerCoupling gridTransformer;


    /*
     * =========================================================
     * スイッチ
     * =========================================================
     */

    private final SwitchedWire selfConsumptionPositiveSwitch;
    private final SwitchedWire selfConsumptionNegativeSwitch;

    private final SwitchedWire gridPositiveSwitch;
    private final SwitchedWire gridNegativeSwitch;


    /*
     * =========================================================
     * DC LINK
     * =========================================================
     */

    private final IElectricNode dcLinkPlus;
    private final IElectricNode dcLinkMinus;


    /*
     * =========================================================
     * 自家消費側
     * =========================================================
     */

    private final IElectricNode selfConsumptionL;
    private final IElectricNode selfConsumptionN;


    /*
     * =========================================================
     * GRID
     * =========================================================
     */

    private final IElectricNode gridL;
    private final IElectricNode gridN;


    /*
     * =========================================================
     * 測定値
     * =========================================================
     */

    private double totalAvailablePower;

    private double pvPower;

    private double selfConsumptionPower;

    private double loadPower;

    /**
     * 正数 = GRIDへ売電
     * 負数 = GRIDから購入
     */
    private double gridPower;

    private double gridExportPower;

    private double gridImportPower;

    private double dcLinkVoltage;

    private double selfConsumptionVoltage;

    private double gridVoltage;

    private double gridCurrent;

    private boolean gridAvailable;


    /*
     * 前Tickの値
     */
    private double previousLoadPower;


    /*
     * =========================================================
     * Constructor
     * =========================================================
     *
     * BlockEntity側の13引数構成と完全一致。
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
    public GeneralPowerConditionerController(
            RegulatedTransformerCoupling[] pvTransformers,

            RegulatedTransformerCoupling selfConsumptionTransformer,

            RegulatedTransformerCoupling gridTransformer,

            SwitchedWire selfConsumptionPositiveSwitch,
            SwitchedWire selfConsumptionNegativeSwitch,

            SwitchedWire gridPositiveSwitch,
            SwitchedWire gridNegativeSwitch,

            IElectricNode dcLinkPlus,
            IElectricNode dcLinkMinus,

            IElectricNode selfConsumptionL,
            IElectricNode selfConsumptionN,

            IElectricNode gridL,
            IElectricNode gridN
    ) {

        if (pvTransformers == null ||
                pvTransformers.length != CHANNEL_COUNT) {

            throw new IllegalArgumentException(
                    "General Power Conditioner requires 3 PV channels"
            );
        }


        /*
         * -----------------------------------------------------
         * MPPT Channel
         * -----------------------------------------------------
         */

        this.mppt =
                new GeneralPowerConditionerMpptChannel[CHANNEL_COUNT];


        for (int i = 0; i < CHANNEL_COUNT; i++) {

            this.mppt[i] =
                    new GeneralPowerConditionerMpptChannel(
                            SolarRegistry.LVYUAN_410W,
                            pvTransformers[i]
                    );
        }


        /*
         * -----------------------------------------------------
         * Transformer
         * -----------------------------------------------------
         */

        this.selfConsumptionTransformer =
                selfConsumptionTransformer;

        this.gridTransformer =
                gridTransformer;


        /*
         * -----------------------------------------------------
         * Switch
         * -----------------------------------------------------
         */

        this.selfConsumptionPositiveSwitch =
                selfConsumptionPositiveSwitch;

        this.selfConsumptionNegativeSwitch =
                selfConsumptionNegativeSwitch;

        this.gridPositiveSwitch =
                gridPositiveSwitch;

        this.gridNegativeSwitch =
                gridNegativeSwitch;


        /*
         * -----------------------------------------------------
         * Nodes
         * -----------------------------------------------------
         */

        this.dcLinkPlus =
                dcLinkPlus;

        this.dcLinkMinus =
                dcLinkMinus;

        this.selfConsumptionL =
                selfConsumptionL;

        this.selfConsumptionN =
                selfConsumptionN;

        this.gridL =
                gridL;

        this.gridN =
                gridN;
    }


    /*
     * =========================================================
     * Update
     * =========================================================
     */

    public void update(
            double irradiance,
            double temperature
    ) {

        /*
         * -----------------------------------------------------
         * 1. 現在の電圧を読む
         * -----------------------------------------------------
         */

        readVoltages();


        /*
         * -----------------------------------------------------
         * 2. MPPT計算
         * -----------------------------------------------------
         */

        totalAvailablePower = 0.0;


        for (GeneralPowerConditionerMpptChannel channel : mppt) {

            channel.updateMaximumPower(
                    irradiance,
                    temperature
            );

            totalAvailablePower +=
                    channel.getMaximumPower();
        }


        /*
         * -----------------------------------------------------
         * 3. DC LINK電圧を確認
         * -----------------------------------------------------
         *
         * 初期状態では電圧が存在しない可能性がある。
         *
         * その場合でもMPPTの目標電圧を400Vに設定する。
         */

        double targetDcVoltage =
                DC_LINK_TARGET_VOLTAGE;


        /*
         * -----------------------------------------------------
         * 4. 各MPPTをDC LINKへ合わせる
         * -----------------------------------------------------
         */

        for (GeneralPowerConditionerMpptChannel channel : mppt) {

            channel.applyTarget(
                    targetDcVoltage
            );
        }


        /*
         * -----------------------------------------------------
         * 5. PV電力を計算
         * -----------------------------------------------------
         */

        pvPower =
                calculatePvPower();


        /*
         * -----------------------------------------------------
         * 6. GRID状態確認
         * -----------------------------------------------------
         */

        updateGridStatus();


        /*
         * -----------------------------------------------------
         * 7. 自家消費側を常時有効
         * -----------------------------------------------------
         */

        connectSelfConsumption();


        /*
         * 自家消費用変圧器の出力電圧を設定。
         */

        updateSelfConsumptionVoltage();


        /*
         * -----------------------------------------------------
         * 8. GRID制御
         * -----------------------------------------------------
         */

        if (!gridAvailable) {

            /*
             * 系統停電
             */

            disconnectGrid();

            gridPower = 0.0;

            gridExportPower = 0.0;

            gridImportPower = 0.0;

            gridCurrent = 0.0;

        } else {

            /*
             * 系統正常
             */

            controlGrid();
        }


        /*
         * -----------------------------------------------------
         * 9. 電圧再測定
         * -----------------------------------------------------
         */

        readVoltages();


        /*
         * -----------------------------------------------------
         * 10. 電力計算
         * -----------------------------------------------------
         */

        calculatePowerFlow();


        previousLoadPower =
                loadPower;
    }


    /*
     * =========================================================
     * PV電力
     * =========================================================
     */

    private double calculatePvPower() {

        double power = 0.0;


        for (GeneralPowerConditionerMpptChannel channel : mppt) {

            double current =
                    Math.abs(
                            channel
                                    .getTransformer()
                                    .getPrimaryCurrent()
                    );


            double voltage =
                    channel.getMaximumPowerVoltage();


            double actualPower =
                    finitePositive(
                            current * voltage
                    );


            /*
             * MPPTが計算した最大電力を超えない。
             */

            actualPower =
                    Math.min(
                            actualPower,
                            channel.getMaximumPower()
                    );


            power += actualPower;
        }


        return Math.min(
                power,
                totalAvailablePower
        );
    }


    /*
     * =========================================================
     * 自家消費
     * =========================================================
     */

    private void connectSelfConsumption() {

        if (selfConsumptionPositiveSwitch != null) {

            selfConsumptionPositiveSwitch.setState(true);
        }


        if (selfConsumptionNegativeSwitch != null) {

            selfConsumptionNegativeSwitch.setState(true);
        }
    }


    private void updateSelfConsumptionVoltage() {

        if (selfConsumptionTransformer == null)
            return;


        /*
         * DC LINK実測値を使用する。
         *
         * DC LINKが400Vなら200V。
         * DC LINKが350Vなら175V。
         *
         * したがって、
         *
         * Vsecondary / Vprimary
         * =
         * 200 / 実測DC LINK電圧
         */

        double primaryVoltage =
                dcLinkVoltage;


        if (primaryVoltage <= MIN_VOLTAGE) {

            /*
             * 起動直後などでDC LINKがまだ0Vの場合。
             *
             * 初期比率400→200を使用。
             */

            primaryVoltage =
                    DC_LINK_TARGET_VOLTAGE;
        }


        double ratio =
                SELF_CONSUMPTION_TARGET_VOLTAGE
                        /
                        primaryVoltage;


        ratio =
                clamp(
                        ratio,
                        1.0e-6,
                        1.0e6
                );


        selfConsumptionTransformer.setSecondaryTurns(
                selfConsumptionTransformer.getPrimaryTurns()
                        *
                        ratio
        );
    }


    /*
     * =========================================================
     * GRID制御
     * =========================================================
     */

    private void controlGrid() {

        /*
         * 現在の負荷電力を取得。
         *
         * 前Tickの値も利用する。
         */

        double estimatedLoad =
                Math.max(
                        0.0,
                        previousLoadPower
                );


        /*
         * PV > LOAD
         *
         * → 余剰電力をGRIDへ送る
         */

        if (pvPower > estimatedLoad) {

            double surplus =
                    pvPower
                            -
                            estimatedLoad;


            setGridExport(
                    surplus
            );

            return;
        }


        /*
         * PV < LOAD
         *
         * → GRIDから不足分を取り込む
         */

        double deficit =
                estimatedLoad
                        -
                        pvPower;


        setGridImport(
                deficit
        );
    }


    /*
     * =========================================================
     * GRID Export
     * =========================================================
     */

    private void setGridExport(
            double requestedPower
    ) {

        connectGrid();


        if (gridTransformer == null)
            return;


        if (gridVoltage <= MIN_VOLTAGE) {

            gridPower = 0.0;

            gridExportPower = 0.0;

            gridImportPower = 0.0;

            gridCurrent = 0.0;

            return;
        }


        /*
         * DC LINK → GRID
         *
         * 系統電圧より少し高い電圧を
         * GRID側に作る。
         */

        double targetGridVoltage =
                gridVoltage
                        +
                        EXPORT_VOLTAGE_MARGIN;


        /*
         * DC LINK実測値から変圧比を計算。
         */

        double primaryVoltage =
                dcLinkVoltage;


        if (primaryVoltage <= MIN_VOLTAGE) {

            primaryVoltage =
                    DC_LINK_TARGET_VOLTAGE;
        }


        double ratio =
                targetGridVoltage
                        /
                        primaryVoltage;


        ratio =
                clamp(
                        ratio,
                        1.0e-6,
                        1.0e6
                );


        gridTransformer.setSecondaryTurns(
                gridTransformer.getPrimaryTurns()
                        *
                        ratio
        );


        /*
         * 電力値。
         */

        double maximumPower =
                MAX_GRID_CURRENT
                        *
                        Math.max(
                                gridVoltage,
                                0.0
                        );


        gridExportPower =
                Math.min(
                        Math.max(
                                requestedPower,
                                0.0
                        ),
                        maximumPower
                );


        gridImportPower =
                0.0;


        gridPower =
                gridExportPower;


        gridCurrent =
                gridVoltage > MIN_VOLTAGE
                        ? gridExportPower / gridVoltage
                        : 0.0;
    }


    /*
     * =========================================================
     * GRID Import
     * =========================================================
     */

    private void setGridImport(
            double requestedPower
    ) {

        connectGrid();


        if (gridTransformer == null)
            return;


        if (gridVoltage <= MIN_VOLTAGE) {

            gridPower = 0.0;

            gridExportPower = 0.0;

            gridImportPower = 0.0;

            gridCurrent = 0.0;

            return;
        }


        /*
         * GRID → DC LINK
         *
         * 系統側からAC側へ電力を取り込む。
         *
         * 系統側の電圧をAC側より少し高くする。
         */

        double targetGridVoltage =
                Math.max(
                        MIN_VOLTAGE,
                        gridVoltage
                                +
                                IMPORT_VOLTAGE_MARGIN
                );


        double primaryVoltage =
                dcLinkVoltage;


        if (primaryVoltage <= MIN_VOLTAGE) {

            primaryVoltage =
                    DC_LINK_TARGET_VOLTAGE;
        }


        double ratio =
                targetGridVoltage
                        /
                        primaryVoltage;


        ratio =
                clamp(
                        ratio,
                        1.0e-6,
                        1.0e6
                );


        gridTransformer.setSecondaryTurns(
                gridTransformer.getPrimaryTurns()
                        *
                        ratio
        );


        double maximumPower =
                MAX_GRID_CURRENT
                        *
                        Math.max(
                                gridVoltage,
                                0.0
                        );


        gridImportPower =
                Math.min(
                        Math.max(
                                requestedPower,
                                0.0
                        ),
                        maximumPower
                );


        gridExportPower =
                0.0;


        /*
         * Importなので負。
         */

        gridPower =
                -gridImportPower;


        gridCurrent =
                gridVoltage > MIN_VOLTAGE
                        ? gridImportPower / gridVoltage
                        : 0.0;
    }


    /*
     * =========================================================
     * GRID接続
     * =========================================================
     */

    private void connectGrid() {

        if (gridPositiveSwitch != null) {

            gridPositiveSwitch.setState(true);
        }


        if (gridNegativeSwitch != null) {

            gridNegativeSwitch.setState(true);
        }
    }


    private void disconnectGrid() {

        if (gridPositiveSwitch != null) {

            gridPositiveSwitch.setState(false);
        }


        if (gridNegativeSwitch != null) {

            gridNegativeSwitch.setState(false);
        }
    }


    /*
     * =========================================================
     * GRID状態
     * =========================================================
     */

    private void updateGridStatus() {

        gridVoltage =
                voltage(
                        gridL,
                        gridN
                );


        gridAvailable =
                gridVoltage >= GRID_MIN_VOLTAGE
                        &&
                        gridVoltage <= GRID_MAX_VOLTAGE;
    }


    /*
     * =========================================================
     * 電圧取得
     * =========================================================
     */

    private void readVoltages() {

        dcLinkVoltage =
                voltage(
                        dcLinkPlus,
                        dcLinkMinus
                );


        selfConsumptionVoltage =
                voltage(
                        selfConsumptionL,
                        selfConsumptionN
                );


        gridVoltage =
                voltage(
                        gridL,
                        gridN
                );
    }


    private static double voltage(
            IElectricNode a,
            IElectricNode b
    ) {

        if (a == null || b == null)
            return 0.0;


        double value =
                Math.abs(
                        a.getVoltage()
                                -
                                b.getVoltage()
                );


        return Double.isFinite(value)
                ? value
                : 0.0;
    }


    /*
     * =========================================================
     * 電力計算
     * =========================================================
     */

    private void calculatePowerFlow() {

        /*
         * 自家消費電力。
         *
         * PV電力からGRID輸出分を差し引いたものを
         * 自家消費側へ割り当てる。
         */

        if (gridExportPower > 0.0) {

            selfConsumptionPower =
                    Math.max(
                            0.0,
                            pvPower
                                    -
                                    gridExportPower
                    );

        } else {

            /*
             * GRIDから購入している場合、
             * PVはすべて自家消費へ回る。
             */

            selfConsumptionPower =
                    Math.max(
                            0.0,
                            pvPower
                    );
        }


        /*
         * LOAD電力。
         *
         * 現在のシミュレーションでは
         * 自家消費側に接続された負荷の実測電流を
         * 直接取得できないため、
         * 電力収支から推定する。
         */

        if (gridImportPower > 0.0) {

            loadPower =
                    pvPower
                            +
                            gridImportPower;

        } else {

            loadPower =
                    Math.max(
                            0.0,
                            pvPower
                                    -
                                    gridExportPower
                    );
        }
    }


    /*
     * =========================================================
     * Utility
     * =========================================================
     */

    private static double finitePositive(
            double value
    ) {

        return Double.isFinite(value)
                &&
                value > 0.0
                ? value
                : 0.0;
    }


    private static double clamp(
            double value,
            double min,
            double max
    ) {

        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }


    /*
     * =========================================================
     * Getter
     * =========================================================
     */

    public GeneralPowerConditionerMpptChannel getChannel(
            int index
    ) {

        if (index < 0 ||
                index >= CHANNEL_COUNT) {

            throw new IndexOutOfBoundsException(
                    "MPPT channel index: " + index
            );
        }


        return mppt[index];
    }


    public double getTotalAvailablePower() {

        return totalAvailablePower;
    }


    public double getPvPower() {

        return pvPower;
    }


    public double getSelfConsumptionPower() {

        return selfConsumptionPower;
    }


    public double getLoadPower() {

        return loadPower;
    }


    public double getGridPower() {

        return gridPower;
    }


    public double getGridExportPower() {

        return gridExportPower;
    }


    public double getGridImportPower() {

        return gridImportPower;
    }


    public double getDcLinkVoltage() {

        return dcLinkVoltage;
    }


    public double getAcVoltage() {

        return selfConsumptionVoltage;
    }


    public double getSelfConsumptionVoltage() {

        return selfConsumptionVoltage;
    }


    public double getGridVoltage() {

        return gridVoltage;
    }


    public double getGridCurrent() {

        return gridCurrent;
    }


    public boolean isGridAvailable() {

        return gridAvailable;
    }
}