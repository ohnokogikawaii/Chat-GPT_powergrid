//package org.patryk3211.powergrid.electricity.battery.lifepo4.charger;


/*import com.simibubi.create.foundation.block.IBE;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.patryk3211.powergrid.collections.ModdedBlockEntities;
import org.patryk3211.powergrid.electricity.base.HorizontalAxisElectricBlock;
import org.patryk3211.powergrid.electricity.base.IDecoratedTerminal;
import org.patryk3211.powergrid.electricity.base.TerminalBoundingBox;

import javax.annotation.ParametersAreNonnullByDefault;


@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LiFePO4ChargerBlock
        extends HorizontalAxisElectricBlock
        implements IBE<LiFePO4ChargerBlockEntity> {


    public static final Property<Direction.Axis> HORIZONTAL_AXIS =
            BlockStateProperties.HORIZONTAL_AXIS;



    private static final VoxelShape SHAPE =
            Shapes.or(
                    box(0,0,0,16,12,16)
            );




    private static final TerminalBoundingBox[] TERMINALS =
            new TerminalBoundingBox[]{


                    /*
                     * 電源入力側（前面）
                     */

                    /*new TerminalBoundingBox(
                            IDecoratedTerminal.POSITIVE,
                            3,4,0,
                            7,10,2
                    )
                            .withColor(IDecoratedTerminal.RED),


                    new TerminalBoundingBox(
                            IDecoratedTerminal.NEGATIVE,
                            9,4,0,
                            13,10,2
                    )
                            .withColor(IDecoratedTerminal.BLUE),



                    /*
                     * バッテリー接続側（背面）
                     */

                    /*new TerminalBoundingBox(
                            IDecoratedTerminal.POSITIVE,
                            3,8,14,
                            7,12,16
                    )
                            .withColor(IDecoratedTerminal.RED),


                    new TerminalBoundingBox(
                            IDecoratedTerminal.NEGATIVE,
                            9,8,14,
                            13,12,16
                    )
                            .withColor(IDecoratedTerminal.BLUE),



                    /*
                     * 電圧測定端子（背面下側）
                     */

                    /*new TerminalBoundingBox(
                            IDecoratedTerminal.POSITIVE,
                            3,2,14,
                            7,6,16
                    )
                            .withColor(IDecoratedTerminal.RED),


                    new TerminalBoundingBox(
                            IDecoratedTerminal.NEGATIVE,
                            9,2,14,
                            13,6,16
                    )
                            .withColor(IDecoratedTerminal.BLUE)

            };



    public LiFePO4ChargerBlock(Properties properties) {

        super(properties);


        setTerminalCollection(
                horizontalZTerminals(
                        this,
                        TERMINALS,
                        SHAPE
                )
        );

    }



    @Override
    public Class<LiFePO4ChargerBlockEntity> getBlockEntityClass(){

        return LiFePO4ChargerBlockEntity.class;

    }



    @Override
    public BlockEntityType<? extends LiFePO4ChargerBlockEntity>
    getBlockEntityType(){

        return ModdedBlockEntities.LIFEPO4_CHARGER.get();

    }

}*/