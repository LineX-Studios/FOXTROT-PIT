package com.linexstudios.foxtrot.mixins;

import com.linexstudios.foxtrot.Hud.TelebowHUD;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP {

    // Injects into the method that handles you sending a chat message or command
    @Inject(method = "sendChatMessage", at = @At("HEAD"))
    public void onSendChatMessage(String message, CallbackInfo ci) {
        // Send the exact command string over to our Telebow tracker
        TelebowHUD.instance.onPlayerCommand(message);
    }
}