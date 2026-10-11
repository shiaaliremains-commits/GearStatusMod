package my.gearstatus.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class GearstatusClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> renderGearHud(graphics));
    }

    private static void renderGearHud(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isSpectator()) return;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        Font font = mc.font;

        int hotbarLeft = width / 2 - 91;
        int hotbarRight = width / 2 + 91;
        int baseY = height - 20;

        // 1. الدروع العلوية يسار الـ Hotbar (خوذة + درع)
        drawSlot(graphics, font, mc.player.getItemBySlot(EquipmentSlot.HEAD), hotbarLeft - 56, baseY);
        drawSlot(graphics, font, mc.player.getItemBySlot(EquipmentSlot.CHEST), hotbarLeft - 26, baseY);

        // 2. الدروع السفلية يمين الـ Hotbar (بنطلون + بوت)
        drawSlot(graphics, font, mc.player.getItemBySlot(EquipmentSlot.LEGS), hotbarRight + 6, baseY);
        drawSlot(graphics, font, mc.player.getItemBySlot(EquipmentSlot.FEET), hotbarRight + 36, baseY);

        // 3. السلاح أو الأداة باليد اليمنى
        ItemStack mainHand = mc.player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            drawSlot(graphics, font, mainHand, hotbarRight + 66, baseY);
        }
    }

    private static void drawSlot(GuiGraphicsExtractor g, Font font, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;

        g.item(stack, x, y);

        if (stack.isDamageableItem()) {
            int max = stack.getMaxDamage();
            int damage = stack.getDamageValue();
            int remaining = max - damage;
            int percent = Math.max(0, Math.min(100, (int) ((remaining / (double) max) * 100)));

            int color;
            if (percent >= 75) {
                color = 0xFF55FF55; // أخضر نيون
            } else if (percent >= 40) {
                color = 0xFFFFFF55; // أصفر
            } else if (percent >= 20) {
                color = 0xFFFFAA00; // برتقالي
            } else {
                boolean flash = (System.currentTimeMillis() / 250) % 2 == 0;
                color = flash ? 0xFFFF2222 : 0xFFFF8888; // نبض أحمر تحذيري
            }

            g.centeredText(font, Component.literal(percent + "%"), x + 8, y + 17, color);
        } else if (stack.getCount() > 1) {
            g.centeredText(font, Component.literal("x" + stack.getCount()), x + 8, y + 17, 0xFFE0E0E0);
        }
    }
}
