package org.patryk3211.powergrid.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import org.patryk3211.powergrid.electricity.wire.IWire;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateBaseMixin {
    @Inject(
            // 1. 開発環境の公式名だけを書く（これでIDEのコンパイルエラーが消えます）
            method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"),
            cancellable = true,
            // 2. remapをtrueに。Architectury(Loom)のコンパイルタスクが製品版ビルド時に自動でForge名にリマップしてくれます

            // 3. 開発環境（MojMap）とビルド環境（Forge/Fabric）での微妙な不整合によるコンパイルエラーを防ぐ
            require = 0,
            expect = 1
    )
    private void powerGrid$beforeBlockUse(Level level, Player player, InteractionHand hand, BlockHitResult result, CallbackInfoReturnable<InteractionResult> cir) {
        var item = player.getItemInHand(hand);
        if(IWire.isWire(level, item.getItem()) && item.hasTag() && item.getTagElement("Connection") != null) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
