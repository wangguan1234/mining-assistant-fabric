package cn.wangguan.miningassistant.mixin;

import cn.wangguan.miningassistant.MiningAssistantClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(Options.class)
abstract class OptionsMixin {
    @Shadow @Final @Mutable public KeyMapping[] keyMappings;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;load()V"))
    private void commandHelper$registerKey(CallbackInfo ci) {
        int originalLength = keyMappings.length;
        keyMappings = Arrays.copyOf(keyMappings, originalLength + 1);
        keyMappings[originalLength] = MiningAssistantClient.OPEN;
    }
}

