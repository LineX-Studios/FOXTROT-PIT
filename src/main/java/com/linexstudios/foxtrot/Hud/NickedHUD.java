package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Denick.NickedManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.lwjgl.input.Mouse;

import java.util.Collection;

public class NickedHUD {
    private final Minecraft mc = Minecraft.getMinecraft();

    public static boolean enabled = true;
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

        NetHandlerPlayClient netHandler = mc.getNetHandler();
        if (netHandler == null) return;

        Collection<NetworkPlayerInfo> playerList = netHandler.getPlayerInfoMap();
        boolean foundNicked = false;

        // --- STEP 1: RENDER ACTUAL NICKED PLAYERS ---
        for (NetworkPlayerInfo info : playerList) {
            boolean isVersion2Nick = info.getGameProfile().getId().version() == 2;
            String nickedName = info.getGameProfile().getName();
            String realIGN = NickedManager.getResolvedIGN(nickedName);

            if (isVersion2Nick || (realIGN != null && !realIGN.isEmpty())) {
                if (!foundNicked) {
                    renderHeader(fr, xPos, yPos);
                    yPos += fr.FONT_HEIGHT + 2;
                    foundNicked = true;
                }

                EntityOtherPlayerMP other = new EntityOtherPlayerMP(mc.theWorld, info.getGameProfile());
                renderPlayerLine(fr, other, nickedName, realIGN, xPos, yPos);
                yPos += fr.FONT_HEIGHT;
            }
        }

        // --- STEP 2: RENDER PLACEHOLDER (Only if list is empty and Drag Mode is ON) ---
        if (!foundNicked && dragMode) {
            renderHeader(fr, xPos, yPos);
            yPos += fr.FONT_HEIGHT + 2;
            String placeholder = EnumChatFormatting.DARK_AQUA + "Nicked Players";
            fr.drawStringWithShadow(placeholder, xPos, yPos, 16777215);
        }
    }

    // Helper to draw the "Nicked Players:" title
    private void renderHeader(FontRenderer fr, int x, int y) {
        String header = EnumChatFormatting.DARK_AQUA.toString() + EnumChatFormatting.BOLD.toString() + "Nicked Players:";
        fr.drawStringWithShadow(header, x, y, 16777215);
    }

    // Helper to draw the actual player info line
    private void renderPlayerLine(FontRenderer fr, EntityOtherPlayerMP other, String nick, String real, int x, int y) {
        String formattedName = EnumChatFormatting.WHITE + nick;
        if (real != null && !real.isEmpty()) {
            formattedName += EnumChatFormatting.GRAY + " (" + EnumChatFormatting.YELLOW + real + EnumChatFormatting.GRAY + ")";
        }

        String gear = getMainGear(other);
        String distance = getDistanceOrSpawn(other);
        String fullLine = formattedName + EnumChatFormatting.WHITE + " - " + gear + " " + distance;

        fr.drawStringWithShadow(fullLine, x, y, 16777215);
    }

    private String getDistanceOrSpawn(EntityOtherPlayerMP player) {
        if (player.posY > 113.0D || isInSpawnRegion(player)) {
            return EnumChatFormatting.GRAY + "[" + EnumChatFormatting.GREEN + "SPAWN" + EnumChatFormatting.GRAY + "]";
        }
        float dist = player.getDistanceToEntity(mc.thePlayer);
        String distStr = String.format("%.1f", dist) + "m";
        return (dist < 15.0F ? EnumChatFormatting.RED.toString() : EnumChatFormatting.GREEN.toString()) + distStr;
    }

    private boolean isInSpawnRegion(EntityOtherPlayerMP player) {
        return player.posX > -20 && player.posX < 20 && player.posZ > -20 && player.posZ < 20;
    }

    private String getMainGear(EntityOtherPlayerMP player) {
        ItemStack legs = player.inventory.armorInventory[1];
        if (legs != null && legs.hasTagCompound()) {
            String nbt = legs.getTagCompound().toString();
            if (nbt.contains("Regularity")) return EnumChatFormatting.DARK_RED + "Regularity";
            if (nbt.contains("Mind Assault")) return EnumChatFormatting.DARK_PURPLE + "Mind Assault";
            if (nbt.contains("Venom")) return EnumChatFormatting.DARK_PURPLE + "Venom";
            if (nbt.contains("Evil") || nbt.contains("Dark")) return EnumChatFormatting.DARK_PURPLE + "Darks";
        }
        return EnumChatFormatting.GRAY + "Shop";
    }

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