package com.linexstudios.foxtrot.mixins;

import com.linexstudios.foxtrot.Util.Ranks;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScorePlayerTeam.class)
public class MixinScorePlayerTeam {

    @Inject(method = "formatPlayerName", at = @At("RETURN"), cancellable = true)
    private static void onFormatPlayerName(Team team, String name, CallbackInfoReturnable<String> cir) {
        if (team == null) return;
        
        String formatted = cir.getReturnValue();
        if (formatted == null) return;
        
        String unformatted = StringUtils.stripControlCodes(formatted);
        
        // SCOREBOARD FIX: If the line contains your Level, hijack it!
        if (unformatted.startsWith("Level:")) {
            if (Ranks.isEnabled && Ranks.instance.isInPit()) {
                // Get your custom [120] bracket
                String newBracket = Ranks.instance.getCustomTabPitBracket();
                
                // Completely overwrite what Minecraft draws to the screen. Zero flickering.
                cir.setReturnValue(EnumChatFormatting.WHITE + "Level: " + newBracket);
            }
        }
    }
}