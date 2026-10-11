package my.gearstatus.client;

import my.gearstatus.Gearstatus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class GearstatusClient implements ClientModInitializer {

    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(Gearstatus.MOD_ID, "hud");

    @Override
    public void onInitializeClient() {
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.HOTBAR,
            HUD_ID,
            GearstatusClient::renderGearHud
        );
    }

    public static void renderGearHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isSpectator()) return;

        int height = mc.getWindow().getGuiScaledHeight();
        Font font = mc.font;

        // عمودي 100% في منتصف يسار الشاشة (مستحيل يلمس حافة الشاشة أو الـ Hotbar)
        int startX = 6;
        int startY = (height / 2) - 50;

        EquipmentSlot[] armorSlots = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
        };

        int currentY = startY;

        // 1. رسم الدروع الأربعة عمودياً
        for (EquipmentSlot slot : armorSlots) {
            ItemStack armor = mc.player.getItemBySlot(slot);
            if (!armor.isEmpty()) {
                drawVerticalWidget(graphics, font, armor, startX, currentY);
                currentY += 21; // مسافة عمودية كافية لكل سطر
            }
        }

        // 2. رسم السلاح أو الأداة باليد اليمنى في السطر الخامس عمودياً
        ItemStack mainHand = mc.player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            drawVerticalWidget(graphics, font, mainHand, startX, currentY);
        }
    }

    private static void drawVerticalWidget(
        GuiGraphicsExtractor g,
        Font font,
        ItemStack stack,
        int x,
        int y
    ) {
        if (stack.isEmpty()) return;

        int percent = 100;
        int glowColor = 0xFF55FF55;
        String label = "";

        if (stack.isDamageableItem()) {
            int max = stack.getMaxDamage();
            int damage = stack.getDamageValue();
            int remaining = max - damage;
            percent = Math.max(0, Math.min(100, (int) ((remaining / (double) max) * 100)));

            if (percent >= 75) {
                glowColor = 0xFF55FF55; // أخضر نيون
            } else if (percent >= 40) {
                glowColor = 0xFFFFFF55; // أصفر
            } else if (percent >= 20) {
                glowColor = 0xFFFFAA00; // برتقالي
            } else {
                boolean flash = (System.currentTimeMillis() / 220) % 2 == 0;
                glowColor = flash ? 0xFFFF2222 : 0xFFFF8888; // أحمر نابض
            }
            label = percent + "%";
        } else if (stack.getCount() > 1) {
            label = "x" + stack.getCount();
            glowColor = 0xFFE0E0E0;
        }

        // 1. رسم Glow Frame ناعم حول مجسم الأداة
        drawGlowFrame(g, x - 1, y - 1, 18, 18, glowColor);

        // 2. رسم مجسم الأداة
        g.item(stack, x, y);

        // 3. كتابة النسبة بجانب الأيقونة على اليمين بوضوح
        if (!label.isEmpty()) {
            g.centeredText(font, Component.literal(label), x + 34, y + 5, glowColor);
        }
    }

    private static void drawGlowFrame(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        int alpha = (color >> 24) & 0xFF;
        int rgb = color & 0xFFFFFF;

        int fillAlpha = (alpha * 35) / 255;
        g.fill(x, y, x + w, y + h, (fillAlpha << 24) | 0x0A0F1D);

        int borderAlpha = (alpha * 180) / 255;
        int borderColor = (borderAlpha << 24) | rgb;

        g.fill(x, y, x + w, y + 1, borderColor);
        g.fill(x, y + h - 1, x + w, y + h, borderColor);
        g.fill(x, y, x + 1, y + h, borderColor);
        g.fill(x + w - 1, y, x + w, y + h, borderColor);
    }
}
