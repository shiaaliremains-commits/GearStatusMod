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

        int hotbarLeft = width / 2 - 91;
        int hotbarRight = width / 2 + 91;
        int baseY = height - 22; // يطابق ارتفاع الـ Hotbar بالملي

        // مسافة أمان لعدم التداخل مع خانة اليد اليسرى إذا كانت ممتلئة
        boolean hasOffhand = !mc.player.getOffhandItem().isEmpty();
        int leftBase = hasOffhand ? (hotbarLeft - 32) : (hotbarLeft - 6);
        int leftStart = leftBase - 44;

        // 1. الجهة اليسرى: الخوذة والدرع
        drawSlotWidget(graphics, font, mc.player.getItemBySlot(EquipmentSlot.HEAD), leftStart, baseY);
        drawSlotWidget(graphics, font, mc.player.getItemBySlot(EquipmentSlot.CHEST), leftStart + 22, baseY);

        // 2. الجهة اليمنى: البنطلون والحذاء والسلاح
        int rightStart = hotbarRight + 6;
        drawSlotWidget(graphics, font, mc.player.getItemBySlot(EquipmentSlot.LEGS), rightStart, baseY);
        drawSlotWidget(graphics, font, mc.player.getItemBySlot(EquipmentSlot.FEET), rightStart + 22, baseY);

        ItemStack mainHand = mc.player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            drawSlotWidget(graphics, font, mainHand, rightStart + 44, baseY);
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

        // 1. رسم إطار التوهج Glow Frame
        drawGlowBorder(g, x, y, 20, 20, glowColor);

        // 2. رسم مجسم الأداة بداخل الإطار
        g.item(stack, x + 2, y + 2);

        // 3. كتابة النسبة المئوية فوق الأيقونة (مستحيل تنزل تحت الشاشة نهائياً)
        if (!label.isEmpty()) {
            g.centeredText(font, Component.literal(label), x + 10, y - 9, glowColor);
        }
    }

    private static void drawGlowBorder(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        // خلفية خفيفة ناعمة
        g.fill(x, y, x + w, y + h, 0x770A101D);

        // حدود النيون المتوهجة
        g.fill(x, y, x + w, y + 1, color);         // أعلى
        g.fill(x, y + h - 1, x + w, y + h, color); // أسفل
        g.fill(x, y, x + 1, y + h, color);         // يسار
        g.fill(x + w - 1, y, x + w, y + h, color); // يمين
    }
}
