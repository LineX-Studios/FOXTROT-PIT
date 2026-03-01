package com.linexstudios.foxtrot.mixin;

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

        // Chat Type 2 is the Action Bar
        if (packetIn.getChatType() == 2) {
            String cleanMessage = EnumChatFormatting.getTextWithoutFormattingCodes(packetIn.getChatComponent().getUnformattedText()).toLowerCase();

            // If the server tries to push a Telebow action bar...
            if (cleanMessage.contains("telebow") && cleanMessage.contains("cooldown")) {
                
                // 1. DELETE THE PACKET. The vanilla action bar will never see this.
                ci.cancel();

                // 2. STEAL THE TIME AND FEED IT TO OUR HUD!
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
        // Chat Type 0/1 are standard chat messages (Failsafes)
        else {
            if (!TelebowHUD.enabled) return;
            String cleanMessage = EnumChatFormatting.getTextWithoutFormattingCodes(packetIn.getChatComponent().getUnformattedText());
            
            if (cleanMessage.contains("NOPE! Can't teleport there") || 
                cleanMessage.contains("You died!") || 
                cleanMessage.contains("RESPAWNED!")) {
                TelebowHUD.instance.clearCooldown();
            }
        }
    }
}