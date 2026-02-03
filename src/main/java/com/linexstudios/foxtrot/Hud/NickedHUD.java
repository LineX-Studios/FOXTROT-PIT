package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Denick.NickedManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.input.Mouse;

import java.util.Collection;

public class NickedHUD {
    private final Minecraft mc = Minecraft.getMinecraft();

    public static boolean enabled = true;
    public static boolean debugMode = false;
    public static boolean notificationsEnabled = true;

    public static int hudX = 10;
    public static int hudY = 80;
    public static boolean dragMode = false;

    private static boolean dragging = false;
    private static int dragOffsetX;
    private static int dragOffsetY;

    public void onRender(RenderGameOverlayEvent.Post event) {
        if (!enabled || event.type != RenderGameOverlayEvent.ElementType.TEXT || mc.theWorld == null) return;

        FontRenderer fr = mc.fontRendererObj;
        int xPos = hudX;
        int yPos = hudY;

        boolean foundNicked = false;

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        NetHandlerPlayClient netHandler = mc.getNetHandler();

        for (ScorePlayerTeam team : scoreboard.getTeams()) {
            Collection<String> members = team.getMembershipCollection();
            for (String member : members) {
                NetworkPlayerInfo info = netHandler.getPlayerInfo(member);
                if (info == null) continue;

                EntityOtherPlayerMP other = new EntityOtherPlayerMP(mc.theWorld, info.getGameProfile());
                if (other.getUniqueID().version() != 2) continue; // only nicked players

                if (!foundNicked) {
                    String header = EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "Nicked Players:";
                    fr.drawStringWithShadow(header, xPos, yPos, 16777215);
                    yPos += fr.FONT_HEIGHT + 1;
                    foundNicked = true;
                }

                String nickedName = other.getName();
                String realIGN = NickedManager.getResolvedIGN(nickedName);

                // Nicked name always shown in white
                String formattedName = EnumChatFormatting.WHITE + nickedName;
                // Once resolved, append revealed IGN in yellow
                if (realIGN != null && !realIGN.isEmpty()) {
                    formattedName += EnumChatFormatting.GRAY + " (" +
                            EnumChatFormatting.YELLOW + realIGN +
                            EnumChatFormatting.GRAY + ")";
                }

                String gear = getMainGear(other);
                String distanceDisplay = getDistanceOrSpawn(other);

                String fullLine = formattedName + EnumChatFormatting.WHITE + " in " +
                        gear + EnumChatFormatting.RESET + EnumChatFormatting.GRAY + " - " +
                        distanceDisplay;

                fr.drawStringWithShadow(fullLine, xPos, yPos, 16777215);
                yPos += fr.FONT_HEIGHT;
            }
        }

        // Local drag handling (still works if HUDController isn’t forwarding)
        if (dragMode) {
            if (Mouse.isButtonDown(0)) {
                if (!dragging) {
                    dragging = true;
                    dragOffsetX = Mouse.getX() - hudX;
                    dragOffsetY = mc.displayHeight - Mouse.getY() - hudY;
                }
                hudX = Mouse.getX() - dragOffsetX;
                hudY = mc.displayHeight - Mouse.getY() - dragOffsetY;
            } else {
                dragging = false;
            }
        }
    }

    private String getDistanceOrSpawn(EntityOtherPlayerMP player) {
        // Improved spawn detection: check Y and region bounds
        if (player.posY > 113.0D || isInSpawnRegion(player)) {
            return EnumChatFormatting.GRAY + "[" + EnumChatFormatting.GREEN + "SPAWN" + EnumChatFormatting.GRAY + "]";
        }
        float dist = player.getDistanceToEntity(mc.thePlayer);
        String distStr = String.format("%.2f", dist);
        if (dist < 15.0F) return EnumChatFormatting.RED + distStr;
        return EnumChatFormatting.GREEN + distStr;
    }

    private boolean isInSpawnRegion(EntityOtherPlayerMP player) {
        // Example bounds for Hypixel Pit spawn platform, adjust if needed
        return player.posX > -20 && player.posX < 20 && player.posZ > -20 && player.posZ < 20;
    }

    private String getMainGear(EntityOtherPlayerMP player) {
        ItemStack legs = player.inventory.armorInventory[1];
        if (legs != null && legs.hasTagCompound()) {
            String nbt = legs.getTagCompound().toString();
            if (nbt.contains("Regularity")) return EnumChatFormatting.DARK_RED + "Regularities";
            if (nbt.contains("Mind Assault")) return EnumChatFormatting.DARK_PURPLE + "Mind Assault";
            if (nbt.contains("Venom")) return EnumChatFormatting.DARK_PURPLE + "Venom";
            if (nbt.contains("Evil") || nbt.contains("Dark")) return EnumChatFormatting.DARK_PURPLE + "Darks";
        }
        return EnumChatFormatting.GRAY + "SHOP";
    }

    // New method so HUDController can forward drag events
    public void handleDrag(int mouseX, int mouseY) {
        if (!dragMode) return;
        if (Mouse.isButtonDown(0)) {
            if (!dragging) {
                dragging = true;
                dragOffsetX = mouseX - hudX;
                dragOffsetY = mouseY - hudY;
            }
            hudX = mouseX - dragOffsetX;
            hudY = mouseY - dragOffsetY;
        } else {
            dragging = false;
        }
    }
}
