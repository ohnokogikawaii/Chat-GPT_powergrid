package org.patryk3211.powergrid.electricity.battery.lifepo4;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.ITerminalPlacement;
import org.patryk3211.powergrid.electricity.base.ElectricBehaviour;
import org.patryk3211.powergrid.electricity.battery.AbstractBatteryBlock;
import org.patryk3211.powergrid.electricity.battery.BatterySpec;
import org.patryk3211.powergrid.electricity.deviceconnector.IAcceptConnector;
import org.patryk3211.powergrid.electricity.info.IHaveElectricProperties;
import org.patryk3211.powergrid.electricity.info.Voltage;
import org.patryk3211.powergrid.electricity.redstoneconverter.IRedstoneConverterBehaviour;
import org.patryk3211.powergrid.electricity.wire.powercord.AutoCordEndpoint;
import org.patryk3211.powergrid.utility.Lang;
import org.patryk3211.powergrid.utility.Unit;


import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;


public class LiFePO4BatteryBlock
        extends AbstractBatteryBlock<LiFePO4BatteryBlockEntity>
        implements
        IAcceptConnector,
        IHaveElectricProperties,
        IRedstoneConverterBehaviour {


    public LiFePO4BatteryBlock(Properties properties) {
        super(properties);
    }


    @Override
    public BatterySpec getSpec() {
        return SimpleBatterySpec.LIFEPO4;
    }


    /*
     * 電圧・容量表示
     */
    @Override
    public void appendProperties(
            ItemStack stack,
            Player player,
            List<Component> tooltip
    ) {

        float maxCharge = getSpec().getMaxCharge();

        float charge;


        if(stack.hasTag()
                && stack.getTag().contains("Energy")) {

            charge =
                    (float)
                            (stack.getTag().getDouble("Energy")
                                    / maxCharge);

        } else {

            charge =
                    getSpec().getInitialCharge()
                            / maxCharge;
        }


        Voltage.max(
                getSpec().calculateVoltage(charge),
                player,
                tooltip
        );


        Lang.translate("tooltip.charge.current")
                .style(ChatFormatting.GRAY)
                .addTo(tooltip);


        Lang.builder()
                .add(Component.literal(" "))
                .add(Lang.numberConstant(charge * 100))
                .add(Component.literal("%"))
                .style(ChatFormatting.AQUA)
                .addTo(tooltip);



        Lang.translate("tooltip.capacity")
                .style(ChatFormatting.GRAY)
                .addTo(tooltip);


        Lang.builder()
                .add(Component.literal(" "))
                .add(
                        Lang.numberConstant(
                                maxCharge / 3600
                        )
                )
                .add(Component.literal(" "))
                .add(Unit.ENERGY.get())
                .style(ChatFormatting.GREEN)
                .addTo(tooltip);
    }



    /*
     * 接続判定
     */
    @Override
    public boolean canConnect(
            LevelReader world,
            BlockPos pos,
            BlockState state,
            Direction side
    ) {
        return IAcceptConnector.super.canConnect(
                world,
                pos,
                state,
                side
        );
    }



    @Override
    public boolean isPolarized() {
        return true;
    }



    @Override
    public boolean renderPlug() {
        return true;
    }



    @Override
    public @Nullable AutoCordEndpoint getEndpoint(
            UseOnContext context
    ) {
        return IAcceptConnector.super.getEndpoint(context);
    }



    @Override
    public @Nullable ITerminalPlacement cordTerminal(
            BlockState state,
            Level level,
            BlockHitResult hit
    ) {
        return IAcceptConnector.super.cordTerminal(
                state,
                level,
                hit
        );
    }



    /*
     * レッドストーンSOC出力
     */
    @Override
    public float getSignal(
            Level level,
            BlockState state,
            BlockPos pos,
            Direction face
    ){

        LiFePO4BatteryBlockEntity be =
                getBlockEntity(level,pos);


        if(be == null)
            return 0;


        return
                (float)
                        (
                                be.getEnergy()
                                        /
                                        be.getCapacity()
                        );
    }



    /*
     * BlockEntity
     */
    @Override
    public Class<LiFePO4BatteryBlockEntity> getBlockEntityClass() {
        return LiFePO4BatteryBlockEntity.class;
    }



    @Override
    public BlockEntityType<? extends LiFePO4BatteryBlockEntity>
    getBlockEntityType() {

        return ModdedBlockEntities.LIFEPO4.get();
    }



    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {

        return new LiFePO4BatteryBlockEntity(
                ModdedBlockEntities.LIFEPO4.get(),
                pos,
                state
        );
    }



    @Override
    public void withBlockEntityDo(
            BlockGetter world,
            BlockPos pos,
            Consumer<LiFePO4BatteryBlockEntity> action
    ){
        super.withBlockEntityDo(world,pos,action);
    }



    @Override
    public Optional<LiFePO4BatteryBlockEntity>
    getBlockEntityOptional(
            BlockGetter world,
            BlockPos pos
    ){

        return super.getBlockEntityOptional(
                world,pos
        );
    }



    @Override
    public @Nullable LiFePO4BatteryBlockEntity getBlockEntity(
            BlockGetter world,
            BlockPos pos
    ){

        return super.getBlockEntity(
                world,pos
        );
    }
}