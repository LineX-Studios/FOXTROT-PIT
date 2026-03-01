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
            
            // 1. SCOREBOARD FIX: Hijack the Level line
            if (unformatted.startsWith("Level:")) {
                if (Ranks.changeLevel) {
                    String newBracket = Ranks.instance.getCustomTabPitBracket();
                    cir.setReturnValue(EnumChatFormatting.WHITE + "Level: " + newBracket);
                }
            }
            
            // 2. SCOREBOARD FIX: Hijack the natively drawn Prestige line
            else if (unformatted.startsWith("Prestige:")) {
                if (Ranks.changePrestige) {
                    if (Ranks.targetPrestige > 0) {
                        String romanNum = Ranks.instance.toRoman(Ranks.targetPrestige);
                        // Force the Roman Numeral to be Yellow
                        cir.setReturnValue(EnumChatFormatting.WHITE + "Prestige: " + EnumChatFormatting.YELLOW + romanNum);
                    } else {
                        // If they natively have prestige but spoof to 0, completely hide the line
                        cir.setReturnValue(EnumChatFormatting.RESET.toString());
                    }
                }
            }
            
            // 3. SCOREBOARD FIX: Hijack the Needed XP / XP line
            else if (unformatted.startsWith("Needed XP:") || unformatted.startsWith("XP:")) {
                if (Ranks.changeLevel || Ranks.changePrestige) {
                    if (Ranks.targetLevel >= 120) {
                        // At level 120, Hypixel drops "Needed"
                        cir.setReturnValue(EnumChatFormatting.WHITE + "XP: " + EnumChatFormatting.AQUA + "MAXED!");
                    } else {
                        String spoofedXP = Ranks.instance.getSpoofedNeededXP();
                        cir.setReturnValue(EnumChatFormatting.WHITE + "Needed XP: " + EnumChatFormatting.AQUA + spoofedXP);
                    }
                }
            }
            
        }
    }
}