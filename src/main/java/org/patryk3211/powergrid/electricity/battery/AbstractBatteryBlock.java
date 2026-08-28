/*
 * Copyright 2025 patryk3211
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.patryk3211.powergrid.electricity.battery;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;
import org.patryk3211.powergrid.electricity.base.ElectricBlock;

import java.util.List;

public abstract class AbstractBatteryBlock<T extends BatteryBlockEntity> extends ElectricBlock implements IBE<T> {
    public AbstractBatteryBlock(Properties settings) {
        super(settings);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(world, pos, state, placer, itemStack);
        if(itemStack.hasTag()) {
            withBlockEntityDo(world, pos, be -> {
                var tag = itemStack.getTag();
                if(tag.contains("Energy")) {
                    be.setEnergy(tag.getDouble("Energy"));
                }
            });
        }
    }

    public abstract BatterySpec getSpec();



    @Override
    public List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder params
    ) {
        List<ItemStack> drops = super.getDrops(state, params);

        if (drops.isEmpty())
            return drops;

        BlockEntity blockEntity =
                params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        if (blockEntity instanceof BatteryBlockEntity battery) {
            ItemStack stack = drops.get(0);

            CompoundTag tag = stack.getOrCreateTag();
            tag.putDouble("Energy", battery.getEnergy());
        }

        return drops;
    }
}


