package cn.wangguan.miningassistant.mixin;

import cn.wangguan.miningassistant.MiningAssistantClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import cn.wangguan.miningassistant.RegionPick;
import cn.wangguan.miningassistant.ProtectionController;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;

@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void commandHelper$tick(CallbackInfo ci) {
        MiningAssistantClient.tick((Minecraft)(Object)this);
    }
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void commandHelper$pickFirst(CallbackInfoReturnable<Boolean> ci) {
        if (RegionPick.click((Minecraft)(Object)this, true)) ci.setReturnValue(false);
    }
    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void commandHelper$suppressHeldAttack(boolean pressed, CallbackInfo ci) {
        if (RegionPick.suppressAttack((Minecraft)(Object)this)) ci.cancel();
    }
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void commandHelper$pickSecond(CallbackInfo ci) {
        if (RegionPick.click((Minecraft)(Object)this, false)) ci.cancel();
    }
    @WrapOperation(method="handleKeybinds",at=@At(value="INVOKE",target="Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;releaseUsingItem(Lnet/minecraft/world/entity/player/Player;)V"))
    private void commandHelper$holdAutomaticFood(MultiPlayerGameMode gameMode,Player player,Operation<Void> original){
        if(!ProtectionController.holdFood((Minecraft)(Object)this,player))original.call(gameMode,player);
    }
}

