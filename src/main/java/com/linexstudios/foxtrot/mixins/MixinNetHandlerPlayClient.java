package com.linexstudios.foxtrot.mixins;

import com.linexstudios.foxtrot.Hud.TelebowHUD;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.EnumChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    @Inject(method = "handleChat", at = @At("HEAD"), cancellable = true)
    public void onHandleChat(S02PacketChat packetIn, CallbackInfo ci) {
        if (packetIn == null || packetIn.getChatComponent() == null) return;

        if (packetIn.getType() == 2) {
            String cleanMessage = EnumChatFormatting.getTextWithoutFormattingCodes(packetIn.getChatComponent().getUnformattedText()).toLowerCase();

            if (cleanMessage.contains("telebow") && cleanMessage.contains("cooldown")) {
                ci.cancel();

                if (TelebowHUD.enabled) {
                    Matcher m = Pattern.compile("(\\d+)s").matcher(cleanMessage);
                    if (!m.find()) m = Pattern.compile("(\\d+) seconds").matcher(cleanMessage);
                    
                    if (m.find()) {
                        long seconds = Long.parseLong(m.group(1));
                        TelebowHUD.instance.setCooldown(seconds);
                    }
                }
            }
        } 
        else {
            if (!TelebowHUD.enabled) return;
            String cleanMessage = EnumChatFormatting.getTextWithoutFormattingCodes(packetIn.getChatComponent().getUnformattedText());
            
            // ADDED: "DEATH!" to immediately clear the timer when you get killed or /oof
            if (cleanMessage.contains("NOPE! Can't teleport there") || 
                cleanMessage.contains("You died!") || 
                cleanMessage.contains("RESPAWNED!") ||
                cleanMessage.contains("DEATH!")) {
                
                TelebowHUD.instance.clearCooldown();
            }
        }
    }
}