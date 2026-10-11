package my.gearstatus.client.mixin;

import my.gearstatus.client.GearstatusClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    // الحقن في دالة الرندر الحقيقية لإصدار 26.3
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onRenderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        GearstatusClient.renderGearHud(graphics);
    }
}
