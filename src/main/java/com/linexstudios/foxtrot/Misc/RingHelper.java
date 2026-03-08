package com.linexstudios.foxtrot.Misc;

import com.linexstudios.foxtrot.Render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.util.AxisAlignedBB;

import java.util.ArrayList;
import java.util.List;

public class RingHelper {

    public static final RingHelper instance = new RingHelper();
    private final Minecraft mc = Minecraft.getMinecraft();

    public static boolean enabled = false;
    public static boolean preventMisplace = true;
    public static boolean renderRing = true;

    private static final List<BlockPos> CLASSIC_RING = new ArrayList<>();
    private static final List<BlockPos> ELEMENTS_RING = new ArrayList<>();
    private static final List<BlockPos> GENESIS_RING = new ArrayList<>();

    public List<BlockPos> activeRingCache = CLASSIC_RING;
    private long lastMapCheck = 0;

    static {
        // Classic / Corals (Abyss) / Seasons (Y=82 shifted to 83)
        int[][] classicCoords = {
            {-1,82,10},{0,82,10},{1,82,10},{2,82,10},{5,82,9},{3,82,10},{4,82,9},{7,82,7},{6,82,8},{8,82,6},{9,82,5},{9,82,4},{10,82,0},{10,82,1},{10,82,3},{10,82,2},{10,82,-1},{10,82,-2},{9,82,-3},{9,82,-4},{8,82,-5},{7,82,-6},{6,82,-7},{5,82,-8},{4,82,-8},{3,82,-9},{2,82,-9},{1,82,-9},{-1,82,-9},{-2,82,-9},{0,82,-9},{-3,82,-8},{-5,82,-7},{-6,82,-6},{-7,82,-5},{-8,82,-3},{-9,82,3},{-8,82,4},{-8,82,5},{-7,82,6},{-6,82,7},{-5,82,8},{-4,82,9},{-3,82,9},{-2,82,10},{-9,82,2},{-9,82,1},{-9,82,0},{-9,82,-1},{-9,82,-2},{-8,82,-4},{-4,82,-8}
        };
        for (int[] c : classicCoords) CLASSIC_RING.add(new BlockPos(c[0], c[1] + 1, c[2]));

        // Elements (Y=71 / Y=72 shifted up by 1)
        int[][] elementsCoords = {
            {6,71,-7},{5,71,-8},{4,71,-8},{3,71,-9},{2,71,-9},{1,71,-9},{0,71,-9},{-1,71,-9},{-3,71,-8},{-2,71,-9},{-5,71,-7},{-4,71,-8},{-6,71,-6},{-7,71,-5},{-8,71,-4},{-8,71,-3},{-9,71,-1},{-9,71,0},{-9,71,1},{-9,71,3},{-9,71,2},{-9,71,-2},{-8,71,4},{-8,71,5},{-7,71,6},{-6,71,7},{-5,71,8},{-4,71,9},{-3,71,9},{-2,72,10},{-1,72,10},{0,72,10},{1,72,10},{2,72,10},{3,72,10},{5,71,9},{4,71,9},{7,71,7},{6,71,8},{8,71,6},{9,71,5},{9,71,4},{10,72,3},{10,72,2},{10,72,1},{10,72,0},{10,72,-1},{10,72,-2},{9,71,-3},{9,71,-4},{8,71,-5},{7,71,-6}
        };
        for (int[] c : elementsCoords) ELEMENTS_RING.add(new BlockPos(c[0], c[1] + 1, c[2]));

        // Genesis (Y=43 shifted to 44)
        int[][] genesisCoords = {
            {11,43,-1},{11,43,-2},{11,43,0},{11,43,1},{11,43,2},{11,43,3},{10,43,4},{10,43,5},{9,43,6},{9,43,7},{8,43,8},{7,43,9},{6,43,9},{5,43,10},{4,43,10},{3,43,11},{2,43,11},{1,43,11},{0,43,11},{-1,43,11},{-2,43,11},{-3,43,10},{-4,43,10},{-5,43,9},{-6,43,9},{-7,43,8},{-8,43,7},{-8,43,6},{-9,43,5},{-9,43,4},{-10,43,3},{-10,43,2},{-10,43,1},{-10,43,0},{-10,43,-1},{-10,43,-2},{-9,43,-3},{-9,43,-4},{-8,43,-5},{-8,43,-6},{-7,43,-7},{-6,43,-8},{-4,43,-9},{-3,43,-9},{-5,43,-8},{-2,43,-10},{-1,43,-10},{0,43,-10},{1,43,-10},{2,43,-10},{3,43,-10},{5,43,-9},{4,43,-9},{7,43,-8},{6,43,-8},{8,43,-7},{10,43,-4},{9,43,-5},{9,43,-6},{10,43,-3}
        };
        for (int[] c : genesisCoords) GENESIS_RING.add(new BlockPos(c[0], c[1] + 1, c[2]));
    }

    private boolean isInPit() {
        if (mc.theWorld == null || mc.thePlayer == null) return false;
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) return false;

        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) return false;

        String title = StringUtils.stripControlCodes(objective.getDisplayName());
        return title.contains("THE HYPIXEL PIT") || title.contains("PIT");
    }

    public void updateMapDetection() {
        if (!enabled || mc.theWorld == null || !isInPit()) return;

        long now = System.currentTimeMillis();
        if (now - lastMapCheck < 2000) return; 
        lastMapCheck = now;

        if (!mc.theWorld.isAirBlock(new BlockPos(11, 43, 0))) {
            activeRingCache = GENESIS_RING;
        } else if (!mc.theWorld.isAirBlock(new BlockPos(10, 72, 0))) {
            activeRingCache = ELEMENTS_RING;
        } else {
            activeRingCache = CLASSIC_RING;
        }
    }

    public boolean shouldBlockPlacement(BlockPos clickedPos, EnumFacing face) {
        if (!enabled || !preventMisplace || mc.thePlayer == null || mc.theWorld == null || !isInPit()) return false;
        if (Math.abs(clickedPos.getX()) > 30 || Math.abs(clickedPos.getZ()) > 30) return false;

        BlockPos targetPos = clickedPos.offset(face);
        if (Math.abs(targetPos.getX()) > 14 || Math.abs(targetPos.getZ()) > 14) {
            return false; 
        }

        boolean isValidPlacement = false;
        for (BlockPos ringBlock : activeRingCache) {
            int dx = Math.abs(ringBlock.getX() - targetPos.getX());
            int dz = Math.abs(ringBlock.getZ() - targetPos.getZ());
            
            if (dx <= 1 && dz <= 1) {
                isValidPlacement = true;
                break;
            }
        }
        return !isValidPlacement;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!enabled || !renderRing || mc.thePlayer == null || mc.theWorld == null || !isInPit()) return;

        for (BlockPos pos : activeRingCache) {
            double x = pos.getX() - mc.getRenderManager().viewerPosX;
            double y = pos.getY() - mc.getRenderManager().viewerPosY;
            double z = pos.getZ() - mc.getRenderManager().viewerPosZ;

            AxisAlignedBB bb = new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1);
            
            // Render using RenderUtils engine
            RenderUtils.setup3D();
            RenderUtils.drawFilledBox(bb, 1.0f, 0.33f, 1.0f, 0.20f);
            RenderUtils.drawOutlinedBox(bb, 1.0f, 0.33f, 1.0f, 1.0f, 2.0f);
            RenderUtils.end3D();
        }
    }
    
    public static void toggle() {
        enabled = !enabled;
        if (Minecraft.getMinecraft().thePlayer != null) {
            String prefix = EnumChatFormatting.GRAY + "[" + EnumChatFormatting.RED + "Foxtrot" + EnumChatFormatting.GRAY + "] ";
            String status = enabled ? EnumChatFormatting.GREEN + "ON" : EnumChatFormatting.RED + "OFF";
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(prefix + EnumChatFormatting.YELLOW + "Ring Helper: " + status));
        }
    }
}