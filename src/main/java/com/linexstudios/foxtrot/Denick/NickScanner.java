package com.linexstudios.foxtrot.Denick;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashSet;
import java.util.Set;

public class NickScanner {
    public static final NickScanner instance = new NickScanner();
    public static boolean enabled = true; // Standalone scanner is always on
    private final Minecraft mc = Minecraft.getMinecraft();
    private long lastScan = 0;
    
    // Keeps track of who we've already pushed to the HUD so we don't spam it
    public static final Set<String> detectedNicks = new HashSet<>();

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (!enabled || mc.theWorld == null || mc.thePlayer == null || event.phase != TickEvent.Phase.END) return;

        // Scan the Tab List every 1 second
        if (System.currentTimeMillis() - lastScan < 1000) return;
        lastScan = System.currentTimeMillis();

        if (mc.getNetHandler() == null) return;

        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info == null || info.getGameProfile() == null) continue;

            // YOUR ORIGINAL MAGIC CHECK: Hypixel Nicks use Version 2 UUIDs!
            if (info.getGameProfile().getId().version() == 2) {
                String nickName = info.getGameProfile().getName();

                // Instantly detect them and push to NickedHUD before we even have their nonce
                if (!detectedNicks.contains(nickName)) {
                    detectedNicks.add(nickName);
                    NickedManager.addNicked(nickName, EnumChatFormatting.GRAY + "Scraping...");
                }
            }
        }
    }
}