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

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        Font font = mc.font;

        // موقع عمودي آمن ومثالي: يسار الـ Hotbar مع مسافة كافية لعدم لمس اليد اليسرى
        int hotbarLeft = width / 2 - 91;
        int x = Math.max(10, hotbarLeft - 68);
        int baseY = height - 20;

        EquipmentSlot[] armorSlots = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
        };

        // نبدأ من الأعلى وننزل عمودياً لكل قطعة
        int currentY = baseY - (4 * 19);

        // 1. رسم الدروع الأربعة عمودياً
        for (EquipmentSlot slot : armorSlots) {
            ItemStack armor = mc.player.getItemBySlot(slot);
            if (!armor.isEmpty()) {
                drawSlotWidget(graphics, font, armor, x, currentY);
            }
            currentY += 19;
        }

        // 2. رسم السلاح أو الأداة باليد في السطر الأخير بمحاذاة الـ Hotbar
        ItemStack mainHand = mc.player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            drawSlotWidget(graphics, font, mainHand, x, baseY);
        }
    }

    private static void drawSlotWidget(
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

        // 1. رسم إطار نيون ناعم وأنيق حول الأيقونة
        drawGlowBorder(g, x, y, glowColor);

        // 2. رسم مجسم الأداة
        g.item(stack, x, y);

        // 3. كتابة النسبة على يمين الأيقونة بوضوح تام وبارتفاع متناسق
        if (!label.isEmpty()) {
            g.centeredText(font, Component.literal(label), x + 28, y + 5, glowColor);
        }
    }

    // إطار نيون محيط بالقطعة بدقة 1 بكسل بدون أي تشويه
    private static void drawGlowBorder(GuiGraphicsExtractor g, int x, int y, int color) {
        g.fill(x - 1, y - 1, x + 17, y, color);         // أعلى
        g.fill(x - 1, y + 16, x + 17, y + 17, color);   // أسفل
        g.fill(x - 1, y - 1, x, y + 17, color);         // يسار
        g.fill(x + 16, y - 1, x + 17, y + 17, color);   // يمين
    }
}
