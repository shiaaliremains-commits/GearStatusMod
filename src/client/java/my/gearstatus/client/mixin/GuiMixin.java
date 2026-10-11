package my.gearstatus.client.mixin;

import my.gearstatus.client.GearstatusClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractRenderState(DeltaTracker deltaTracker, boolean bl, boolean bl2, CallbackInfo ci) {
        GearstatusClient.onRenderTick();
    }
}
