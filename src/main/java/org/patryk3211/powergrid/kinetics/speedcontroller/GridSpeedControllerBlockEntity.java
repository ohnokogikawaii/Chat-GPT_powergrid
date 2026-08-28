/*
package org.patryk3211.powergrid.kinetics.speedcontroller;

import java.util.List;

import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.base.IElectricEntity;
import org.patryk3211.powergrid.electricity.sim.node.OwnedFloatingNode;

public class GridSpeedControllerBlockEntity
        extends SpeedControllerBlockEntity
        implements IElectricEntity {

    */
/*
     * ============================================================
     * 電圧制御設定
     * ============================================================
     *//*


    public static final double DEFAULT_TARGET_VOLTAGE =
            6600.0;

    public static final double DEFAULT_CONTROL_GAIN =
            0.01;

    public static final int DEFAULT_MAX_CONTROL_STEP =
            4;

    public static final double DEFAULT_DEADBAND =
            1.0;

    private double targetVoltage =
            DEFAULT_TARGET_VOLTAGE;

    private double controlGain =
            DEFAULT_CONTROL_GAIN;

    private int maxControlStep =
            DEFAULT_MAX_CONTROL_STEP;

    private double voltageDeadband =
            DEFAULT_DEADBAND;

    private double measuredVoltage =
            0.0;

    private boolean voltageControlEnabled =
            true;

    */
/*
     * ============================================================
     * 電気Behaviour
     * ============================================================
     *//*


    private ElectricBehaviour electricBehaviour;

    public GridSpeedControllerBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state) {

        super(type, pos, state);
    }

    */
/*
     * ============================================================
     * Behaviour
     * ============================================================
     *//*


    @Override
    public void addBehaviours(
            List<BlockEntityBehaviour> behaviours) {

        */
/*
         * 最重要：
         *
         * Create Speed Controllerの
         * targetSpeed / callback / computer behaviour等を
         * そのまま生成させる。
         *//*

        super.addBehaviours(behaviours);

        */
/*
         * Power Gridの電気回路。
         *//*

        electricBehaviour =
                new ElectricBehaviour(this);

        behaviours.add(electricBehaviour);
    }

    */
/*
     * ============================================================
     * 電気回路
     * ============================================================
     *
     * Terminal 0 = +
     * Terminal 1 = -
     *//*


    @Override
    public void buildCircuit(
            CircuitBuilder builder) {

        builder.setTerminalCount(2);
    }

    */
/*
     * ============================================================
     * Tick
     * ============================================================
     *//*


    @Override
    public void tick() {

        */
/*
         * CreateのSpeed Controller処理を先に実行。
         *
         * これを削除しない。
         *//*

        super.tick();

        if (level == null)
            return;

        if (level.isClientSide)
            return;

        if (electricBehaviour == null)
            return;

        */
/*
         * 電圧測定。
         *//*

        updateMeasuredVoltage();

        */
/*
         * 自動電圧制御。
         *//*

        if (voltageControlEnabled)
            updateVoltageControl();
    }

    */
/*
     * ============================================================
     * 電圧測定
     * ============================================================
     *//*


    private void updateMeasuredVoltage() {

        if (!electricBehaviour.hasTerminal(0)
                || !electricBehaviour.hasTerminal(1)) {

            measuredVoltage = 0.0;
            return;
        }

        OwnedFloatingNode positive =
                electricBehaviour.getTerminal(0);

        OwnedFloatingNode negative =
                electricBehaviour.getTerminal(1);

        if (positive == null
                || negative == null) {

            measuredVoltage = 0.0;
            return;
        }

        measuredVoltage =
                Math.abs(
                        positive.getVoltage()
                                - negative.getVoltage()
                );
    }

    */
/*
     * ============================================================
     * 電圧 → 回転数制御
     * ============================================================
     *//*


    private void updateVoltageControl() {

        if (targetSpeed == null)
            return;

        int currentSpeed =
                targetSpeed.getValue();

        */
/*
         * 停止中は自動的に方向を決めない。
         *//*

        if (currentSpeed == 0)
            return;

        double voltageError =
                targetVoltage
                        - measuredVoltage;

        */
/*
         * デッドバンド。
         *//*

        if (Math.abs(voltageError)
                <= voltageDeadband) {

            return;
        }

        */
/*
         * 電圧誤差をRPM補正量へ変換。
         *//*

        double correction =
                voltageError
                        * controlGain;

        */
/*
         * 1tickの最大変化量。
         *//*

        correction =
                Math.max(
                        -maxControlStep,
                        Math.min(
                                maxControlStep,
                                correction
                        )
                );

        int currentMagnitude =
                Math.abs(currentSpeed);

        int newMagnitude =
                (int) Math.round(
                        currentMagnitude
                                + correction
                );

        */
/*
         * Createの最大回転数。
         *//*

        int maxSpeed =
                com.simibubi.create.infrastructure.config
                        .AllConfigs
                        .server()
                        .kinetics
                        .maxRotationSpeed
                        .get();

        newMagnitude =
                Math.max(
                        1,
                        Math.min(
                                maxSpeed,
                                newMagnitude
                        )
                );

        */
/*
         * 回転方向維持。
         *//*

        int newSpeed =
                currentSpeed < 0
                        ? -newMagnitude
                        : newMagnitude;

        if (newSpeed == currentSpeed)
            return;

        */
/*
         * Create側のcallbackを通す。
         *
         * これによって
         * RotationPropagatorが再計算される。
         *//*

        targetSpeed.setValue(newSpeed);
    }

    */
/*
     * ============================================================
     * 上部大歯車
     * ============================================================
     *
     * Create SpeedControllerBlockEntityの
     * hasBracketはpackage-privateなので、
     * renderer用に公開する。
     *//*


    public boolean hasBracket() {
        return false;
    }

    */
/*
     * Create側のupdateBracketをそのまま使用。
     *
     * 上の大歯車を追加・削除したときに
     * Createの内部判定を実行する。
     *//*

    @Override
    public void updateBracket() {
        super.updateBracket();
    }

    */
/*
     * ============================================================
     * Getter / Setter
     * ============================================================
     *//*


    public double getTargetVoltage() {
        return targetVoltage;
    }

    public void setTargetVoltage(double value) {

        targetVoltage =
                Math.max(0.0, value);

        setChanged();
        sendData();
    }

    public double getControlGain() {
        return controlGain;
    }

    public void setControlGain(double value) {

        controlGain =
                Math.max(0.0, value);

        setChanged();
        sendData();
    }

    public int getMaxControlStep() {
        return maxControlStep;
    }

    public void setMaxControlStep(int value) {

        maxControlStep =
                Math.max(1, value);

        setChanged();
        sendData();
    }

    public double getVoltageDeadband() {
        return voltageDeadband;
    }

    public void setVoltageDeadband(double value) {

        voltageDeadband =
                Math.max(0.0, value);

        setChanged();
        sendData();
    }

    public double getMeasuredVoltage() {
        return measuredVoltage;
    }

    public boolean isVoltageControlEnabled() {
        return voltageControlEnabled;
    }

    public void setVoltageControlEnabled(
            boolean enabled) {

        voltageControlEnabled =
                enabled;

        setChanged();
        sendData();
    }

    */
/*
     * ============================================================
     * NBT
     * ============================================================
     *//*


    @Override
    protected void write(
            CompoundTag tag,
            boolean clientPacket) {

        super.write(
                tag,
                clientPacket
        );

        tag.putDouble(
                "GridTargetVoltage",
                targetVoltage
        );

        tag.putDouble(
                "GridControlGain",
                controlGain
        );

        tag.putInt(
                "GridMaxControlStep",
                maxControlStep
        );

        tag.putDouble(
                "GridVoltageDeadband",
                voltageDeadband
        );

        tag.putDouble(
                "GridMeasuredVoltage",
                measuredVoltage
        );

        tag.putBoolean(
                "GridVoltageControlEnabled",
                voltageControlEnabled
        );
    }

    @Override
    protected void read(
            CompoundTag tag,
            boolean clientPacket) {

        super.read(
                tag,
                clientPacket
        );

        if (tag.contains(
                "GridTargetVoltage"
        )) {

            targetVoltage =
                    tag.getDouble(
                            "GridTargetVoltage"
                    );
        }

        if (tag.contains(
                "GridControlGain"
        )) {

            controlGain =
                    tag.getDouble(
                            "GridControlGain"
                    );
        }

        if (tag.contains(
                "GridMaxControlStep"
        )) {

            maxControlStep =
                    tag.getInt(
                            "GridMaxControlStep"
                    );
        }

        if (tag.contains(
                "GridVoltageDeadband"
        )) {

            voltageDeadband =
                    tag.getDouble(
                            "GridVoltageDeadband"
                    );
        }

        if (tag.contains(
                "GridMeasuredVoltage"
        )) {

            measuredVoltage =
                    tag.getDouble(
                            "GridMeasuredVoltage"
                    );
        }

        if (tag.contains(
                "GridVoltageControlEnabled"
        )) {

            voltageControlEnabled =
                    tag.getBoolean(
                            "GridVoltageControlEnabled"
                    );
        }
    }

    */
/*
     * ============================================================
     * Remove
     * ============================================================
     *//*


    @Override
    public void remove() {

        if (electricBehaviour != null) {
            electricBehaviour.remove();
            electricBehaviour = null;
        }

        super.remove();
    }
}*/
