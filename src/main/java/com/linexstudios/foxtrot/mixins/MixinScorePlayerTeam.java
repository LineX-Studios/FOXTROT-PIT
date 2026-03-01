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
        
        if (Ranks.isEnabled && Ranks.instance.isInPit()) {
            
            // 1. SCOREBOARD FIX: Hijack the Level line!
            if (unformatted.startsWith("Level:")) {
                if (Ranks.changeLevel) {
                    String newBracket = Ranks.instance.getCustomTabPitBracket();
                    cir.setReturnValue(EnumChatFormatting.WHITE + "Level: " + newBracket);
                }
            }
            
            // 2. SCOREBOARD FIX: Hijack the Prestige line!
            else if (unformatted.startsWith("Prestige:")) {
                if (Ranks.changePrestige && Ranks.targetPrestige > 0) {
                    String romanNum = Ranks.instance.toRoman(Ranks.targetPrestige);
                    EnumChatFormatting color = Ranks.instance.getPrestigeColor(Ranks.targetPrestige);
                    
                    cir.setReturnValue(EnumChatFormatting.WHITE + "Prestige: " + color + romanNum);
                }
            }
            
            // 3. SCOREBOARD FIX: Hijack the Needed XP line!
            else if (unformatted.startsWith("Needed XP:")) {
                if (Ranks.changeLevel || Ranks.changePrestige) {
                    // Pulls the dynamically calculated, comma-formatted XP from Ranks.java!
                    String spoofedXP = Ranks.instance.getSpoofedNeededXP();
                    cir.setReturnValue(EnumChatFormatting.WHITE + "Needed XP: " + spoofedXP);
                }
            }
            
        }
    }
}