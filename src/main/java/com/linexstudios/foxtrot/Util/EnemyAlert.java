package com.linexstudios.foxtrot.Util;

import com.linexstudios.foxtrot.Hud.EnemyHUD;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashSet;
import java.util.Set;

public class EnemyAlert {

    private final Set<String> alertedThisLobby = new HashSet<>();

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        // Runs perfectly in sync with EnemyHUD's detection
        if (event.phase != TickEvent.Phase.END || !EnemyHUD.notificationsEnabled || Minecraft.getMinecraft().thePlayer == null || Minecraft.getMinecraft().theWorld == null) return;

        // Scans the exact same list EnemyHUD uses to draw on your screen
        for (EntityPlayer player : Minecraft.getMinecraft().theWorld.playerEntities) {
            if (player == Minecraft.getMinecraft().thePlayer) continue;

            String name = player.getName();

            if (EnemyHUD.isTarget(name) && !alertedThisLobby.contains(name)) {
                alertedThisLobby.add(name);

                String alert = EnumChatFormatting.GRAY + "[" + EnumChatFormatting.RED + "Foxtrot" + EnumChatFormatting.GRAY + "] "
                        + EnumChatFormatting.DARK_RED + EnumChatFormatting.BOLD + "ENEMY: "
                        + EnumChatFormatting.RED + name + EnumChatFormatting.YELLOW + " has entered your lobby!";

                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(alert));
                // NO SOUND PLAYED - Purely visual chat alert
            }
        }
    }

    @SubscribeEvent
    public void onWorldChange(WorldEvent.Load event) {
        // Resets the tracker when you switch lobbies or drop into the Pit
        alertedThisLobby.clear();
    }
}