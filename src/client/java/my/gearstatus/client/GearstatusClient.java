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
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

public class GearstatusClient implements ClientModInitializer {

    private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(Gearstatus.MOD_ID, "hud");
    private static final Map<EquipmentSlot, Integer> lastDamageMap = new HashMap<>();
    private static final Map<EquipmentSlot, Long> flashTimeMap = new HashMap<>();

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
        int baseY = height - 22;

        boolean hasOffhand = !mc.player.getOffhandItem().isEmpty();
        int leftBase = hasOffhand ? (hotbarLeft - 32) : (hotbarLeft - 6);
        int leftStart = leftBase - 44;

        // 1. الجهة اليسرى: الخوذة والدرع
        drawSlotWidget(graphics, font, mc, mc.player.getItemBySlot(EquipmentSlot.HEAD), EquipmentSlot.HEAD, leftStart, baseY);
        drawSlotWidget(graphics, font, mc, mc.player.getItemBySlot(EquipmentSlot.CHEST), EquipmentSlot.CHEST, leftStart + 22, baseY);

        // 2. الجهة اليمنى: البنطلون والحذاء
        int rightStart = hotbarRight + 6;
        drawSlotWidget(graphics, font, mc, mc.player.getItemBySlot(EquipmentSlot.LEGS), EquipmentSlot.LEGS, rightStart, baseY);
        drawSlotWidget(graphics, font, mc, mc.player.getItemBySlot(EquipmentSlot.FEET), EquipmentSlot.FEET, rightStart + 22, baseY);

        // 3. السلاح الممسوك باليد اليمنى
        ItemStack mainHand = mc.player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            drawSlotWidget(graphics, font, mc, mainHand, EquipmentSlot.MAINHAND, rightStart + 44, baseY);
        }

        // 4. الغرض باليد اليسرى (Offhand) بجانب السلاح إذا كان موجوداً
        ItemStack offHand = mc.player.getOffhandItem();
        if (!offHand.isEmpty() && offHand.isDamageableItem()) {
            drawSlotWidget(graphics, font, mc, offHand, EquipmentSlot.OFFHAND, rightStart + 66, baseY);
        }
    }

    private static void drawSlotWidget(
        GuiGraphicsExtractor g,
        Font font,
        Minecraft mc,
        ItemStack stack,
        EquipmentSlot slot,
        int x,
        int y
    ) {
        if (stack.isEmpty()) return;

        int percent = 100;
        int glowColor = 0xFF55FF55;
        String label = "";

        // رصد الضرر لتشغيل وميض الفلاش الأبيض الخاطف
        if (stack.isDamageableItem()) {
            int currentDmg = stack.getDamageValue();
            int prevDmg = lastDamageMap.getOrDefault(slot, currentDmg);
            if (currentDmg > prevDmg) {
                flashTimeMap.put(slot, System.currentTimeMillis());
            }
            lastDamageMap.put(slot, currentDmg);

            int max = stack.getMaxDamage();
            int remaining = max - currentDmg;
            percent = Math.max(0, Math.min(100, (int) ((remaining / (double) max) * 100)));

            // تحذير خاص بالإليترا إذا قاربت على الكسر
            boolean isElytra = stack.is(Items.ELYTRA);
            if (isElytra && percent <= 15) {
                boolean flash = (System.currentTimeMillis() / 180) % 2 == 0;
                glowColor = flash ? 0xFFFF1111 : 0xFFFFFFFF;
            } else if (percent >= 75) {
                glowColor = 0xFF55FF55; // أخضر
            } else if (percent >= 40) {
                glowColor = 0xFFFFFF55; // أصفر
            } else if (percent >= 20) {
                glowColor = 0xFFFFAA00; // برتقالي
            } else {
                boolean flash = (System.currentTimeMillis() / 220) % 2 == 0;
                glowColor = flash ? 0xFFFF2222 : 0xFFFF8888; // أحمر نابض
            }

            label = percent + "%";

            // عداد الأسهم إذا ماسك قوس أو كروسبو
            if (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem) {
                int arrows = countArrows(mc);
                label = percent + "% (" + arrows + ")";
            }
        } else if (stack.getCount() > 1) {
            label = "x" + stack.getCount();
            glowColor = 0xFFE0E0E0;
        }

        // وميض الضرر الأبيض عند أكل ضربة
        long flashStart = flashTimeMap.getOrDefault(slot, 0L);
        if (System.currentTimeMillis() - flashStart < 220) {
            glowColor = 0xFFFFFFFF;
        }

        // 1. رسم إطار التوهج الكبسولي الناعم
        drawRoundedGlow(g, x, y, 20, 20, glowColor);

        // 2. رسم مجسم الأداة
        g.item(stack, x + 2, y + 2);

        // 3. شريط النيون المايكرو بدقة 1 بكسل أسفل الأيقونة
        if (stack.isDamageableItem()) {
            drawMicroBar(g, x + 2, y + 17, 16, percent, glowColor);
        }

        // 4. كتابة النسبة المئوية فوق الأيقونة
        if (!label.isEmpty()) {
            g.centeredText(font, Component.literal(label), x + 10, y - 9, glowColor);
        }
    }

    private static void drawRoundedGlow(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x + 1, y, x + w - 1, y + h, 0x770A101D);
        g.fill(x, y + 1, x + 1, y + h - 1, 0x770A101D);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, 0x770A101D);

        // حواف كبسولية ناعمة
        g.fill(x + 1, y, x + w - 1, y + 1, color);         // أعلى
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, color); // أسفل
        g.fill(x, y + 1, x + 1, y + h - 1, color);         // يسار
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color); // يمين
    }

    private static void drawMicroBar(GuiGraphicsExtractor g, int x, int y, int maxW, int percent, int color) {
        int fillW = Math.max(1, (maxW * percent) / 100);
        g.fill(x, y, x + maxW, y + 1, 0xAA222222);
        g.fill(x, y, x + fillW, y + 1, color);
    }

    private static int countArrows(Minecraft mc) {
        if (mc.player == null) return 0;
        int count = 0;
        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack s = mc.player.getInventory().getItem(i);
            if (s.is(Items.ARROW) || s.is(Items.SPECTRAL_ARROW) || s.is(Items.TIPPED_ARROW)) {
                count += s.getCount();
            }
        }
        return count;
    }
}
