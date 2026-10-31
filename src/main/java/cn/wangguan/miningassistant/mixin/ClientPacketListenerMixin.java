package cn.wangguan.miningassistant.mixin;

import cn.wangguan.miningassistant.ProtectionController;
import cn.wangguan.miningassistant.EmergencyController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    @Inject(method="handleDamageEvent",at=@At("TAIL"))
    private void commandHelper$damage(ClientboundDamageEventPacket packet,CallbackInfo ci){
        ProtectionController.onDamage(Minecraft.getInstance(),packet);
    }
    @Inject(method="handleMovePlayer",at=@At("TAIL"))
    private void commandHelper$position(ClientboundPlayerPositionPacket packet,CallbackInfo ci){
        EmergencyController.onPosition(Minecraft.getInstance());
    }
    @Inject(method="handleRespawn",at=@At("TAIL"))
    private void commandHelper$respawn(ClientboundRespawnPacket packet,CallbackInfo ci){
        EmergencyController.onRespawn(Minecraft.getInstance(),packet);
    }
    @Inject(method="handlePlayerCombatKill",at=@At("TAIL"))
    private void commandHelper$death(ClientboundPlayerCombatKillPacket packet,CallbackInfo ci){
        EmergencyController.onDeath(Minecraft.getInstance(),packet.playerId());
    }
}

