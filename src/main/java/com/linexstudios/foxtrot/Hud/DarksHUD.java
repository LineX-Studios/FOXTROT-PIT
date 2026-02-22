package com.linexstudios.foxtrot.Hud;

import com.linexstudios.foxtrot.Util.SpawnRegions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DarksHUD extends DraggableHUD {
    public static final DarksHUD instance = new DarksHUD();
    private final Minecraft mc = Minecraft.getMinecraft();

    public static boolean enabled = true;

    // Set the default position slightly below RegHUD
    public DarksHUD() { super("Darks List", 10, 240); }

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT) return;
        if (mc.currentScreen instanceof EditHUDGui) return;
        if (mc.currentScreen instanceof HUDSettingsGui) return;
        render(false, 0, 0); 
    }

    @Override
    public void draw(boolean isEditing) {
        if (!enabled || mc.theWorld == null) return;

        FontRenderer fr = mc.fontRendererObj;
        int currentY = 0; 
        int maxWidth = fr.getStringWidth("Dark Pants Players");
        boolean foundDark = false;

        Set<String> renderedPlayers = new HashSet<>();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!(player instanceof EntityOtherPlayerMP)) continue;
            EntityOtherPlayerMP other = (EntityOtherPlayerMP) player;

            String enchantsDisplay = getDarkEnchants(other);
            if (enchantsDisplay == null) continue;

            String name = other.getName();
            if (renderedPlayers.contains(name.toLowerCase())) continue;
            renderedPlayers.add(name.toLowerCase());

            if (!foundDark) {
                fr.drawStringWithShadow(EnumChatFormatting.DARK_PURPLE + "" + EnumChatFormatting.BOLD + "Dark Pants Players:", 0, currentY, 0xFFFFFF);
                currentY += fr.FONT_HEIGHT + 2;
                foundDark = true;
            }

            String displayName = other.getDisplayName().getFormattedText();
            displayName = EnumChatFormatting.DARK_PURPLE + "[" + EnumChatFormatting.LIGHT_PURPLE + "D" + EnumChatFormatting.DARK_PURPLE + "] " + EnumChatFormatting.RESET + displayName;

            String dist = SpawnRegions.getLocationFormat(mc.thePlayer, other);

            String fullLine = displayName + EnumChatFormatting.GRAY + " - " + enchantsDisplay + EnumChatFormatting.GRAY + " - " + dist;
            fr.drawStringWithShadow(fullLine, 0, currentY, 0xFFFFFF);

            int lineWidth = fr.getStringWidth(fullLine);
            if (lineWidth > maxWidth) maxWidth = lineWidth;
            currentY += fr.FONT_HEIGHT;
        }

        if (!foundDark) {
            if (isEditing) {
                fr.drawStringWithShadow(EnumChatFormatting.DARK_PURPLE + "" + EnumChatFormatting.BOLD + "Dark Pants Players:", 0, currentY, 0xFFFFFF);
                currentY += fr.FONT_HEIGHT + 2;
                String placeholder = EnumChatFormatting.DARK_PURPLE + "[" + EnumChatFormatting.LIGHT_PURPLE + "D" + EnumChatFormatting.DARK_PURPLE + "] " + EnumChatFormatting.GRAY + "[120] Player" + EnumChatFormatting.GRAY + " - " + EnumChatFormatting.WHITE + "VENOM" + EnumChatFormatting.GRAY + " - " + EnumChatFormatting.GREEN + "" + EnumChatFormatting.BOLD + "SPAWN";
                fr.drawStringWithShadow(placeholder, 0, currentY, 0xFFFFFF);
                currentY += fr.FONT_HEIGHT;
                maxWidth = Math.max(maxWidth, fr.getStringWidth(placeholder));
            } else {
                this.width = 0; this.height = 0;
                return;
            }
        }

        this.width = maxWidth;
        this.height = currentY;
    }

    private String getDarkEnchants(EntityOtherPlayerMP player) {
        ItemStack pants = player.inventory.armorInventory[1];
        if (pants == null || !pants.hasTagCompound()) return null;

        NBTTagCompound extra = pants.getTagCompound().getCompoundTag("ExtraAttributes");
        if (extra == null || !extra.hasKey("CustomEnchants")) return null;

        NBTTagList enchants = extra.getTagList("CustomEnchants", 10);
        boolean isDarkPants = false;
        List<String> darkEnchantList = new ArrayList<>();
        List<String> sideEnchants = new ArrayList<>();

        for (int i = 0; i < enchants.tagCount(); i++) {
            String key = enchants.getCompoundTagAt(i).getString("Key").trim();
            String formatted = formatEnchant(key);

            if (formatted != null) {
                if (isPrimaryDarkEnchant(key)) {
                    isDarkPants = true;
                    darkEnchantList.add(formatted);
                } else {
                    sideEnchants.add(formatted);
                }
            }
        }

        if (!isDarkPants) return null;

        List<String> finalEnchants = new ArrayList<>();
        finalEnchants.addAll(darkEnchantList); // Put dark enchants first
        finalEnchants.addAll(sideEnchants);

        return String.join(EnumChatFormatting.WHITE + "/", finalEnchants);
    }

    /**
     * Checks if the enchant is one of the exclusive dark pants enchants.
     */
    private boolean isPrimaryDarkEnchant(String rawKey) {
        String key = rawKey.trim().toLowerCase();
        return key.equals("venom") || key.equals("sanguisuge") || 
               key.equals("grim_reaper") || key.equals("misery") || key.equals("spite") || 
               key.equals("nostalgia") || key.equals("golden_handcuffs") || key.equals("hedge_fund") || 
               key.equals("heartripper") || key.equals("needless_suffering") || 
               key.equals("mind_assault") || key.equals("lycanthropy");
    }

    public static String formatEnchant(String rawKey) {
        if (rawKey == null) return null;

        String key = rawKey.trim().toLowerCase();

        switch (key) {
            // --- Dark Enchants ---
            case "venom": return EnumChatFormatting.DARK_PURPLE+ "VENOM";
            case "sanguisuge": return EnumChatFormatting.RED + "SANGUISUGE";
            case "grim_reaper": return EnumChatFormatting.DARK_GRAY + "GRIM REAPER";
            case "misery": return EnumChatFormatting.DARK_PURPLE + "MISERY";
            case "spite": return EnumChatFormatting.DARK_PURPLE + "SPITE";
            case "nostalgia": return EnumChatFormatting.BLUE + "NOSTALGIA";
            case "golden_handcuffs": return EnumChatFormatting.GOLD + "GOLDEN CUFFS";
            case "hedge_fund": return EnumChatFormatting.YELLOW + "HEDGE FUND";
            case "heartripper": return EnumChatFormatting.RED + "HEART RIPPER";
            case "needless_suffering": return EnumChatFormatting.YELLOW + "NEEDLESS SUFFERING";
            case "mind_assault": return EnumChatFormatting.DARK_PURPLE + "MIND ASSAULTS";
            case "lycanthropy": return EnumChatFormatting.DARK_RED + "LYCANTHROPY";
            default: return null;
        }
    }
}