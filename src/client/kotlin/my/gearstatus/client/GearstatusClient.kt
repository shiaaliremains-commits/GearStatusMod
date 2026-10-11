package my.gearstatus.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack

object GearstatusClient : ClientModInitializer {

    override fun onInitializeClient() {
        HudRenderCallback.EVENT.register { graphics, _ ->
            renderGearHud(graphics)
        }
    }

    private fun renderGearHud(graphics: GuiGraphicsExtractor) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        if (player.isSpectator) return

        val width = mc.window.guiScaledWidth
        val height = mc.window.guiScaledHeight
        val font = mc.font

        val hotbarLeft = width / 2 - 91
        val hotbarRight = width / 2 + 91
        val baseY = height - 20

        // 1. الدروع العلوية على يسار الـ Hotbar (خوذة + درع)
        val helmet = player.getItemBySlot(EquipmentSlot.HEAD)
        val chest = player.getItemBySlot(EquipmentSlot.CHEST)

        drawSlotWidget(graphics, font, helmet, hotbarLeft - 56, baseY)
        drawSlotWidget(graphics, font, chest, hotbarLeft - 26, baseY)

        // 2. الدروع السفلية على يمين الـ Hotbar (بنطلون + بوت)
        val legs = player.getItemBySlot(EquipmentSlot.LEGS)
        val feet = player.getItemBySlot(EquipmentSlot.FEET)

        drawSlotWidget(graphics, font, legs, hotbarRight + 6, baseY)
        drawSlotWidget(graphics, font, feet, hotbarRight + 36, baseY)

        // 3. السلاح أو الأداة الممسوكة باليد اليمنى
        val mainHand = player.mainHandItem
        if (!mainHand.isEmpty) {
            drawSlotWidget(graphics, font, mainHand, hotbarRight + 66, baseY)
        }
    }

    private fun drawSlotWidget(
        graphics: GuiGraphicsExtractor,
        font: net.minecraft.client.gui.Font,
        stack: ItemStack,
        x: Int,
        y: Int
    ) {
        if (stack.isEmpty) return

        // 1. رسم مجسم الأداة ثلاثي الأبعاد مباشرة بدون أي مربعات رمادية خشنة
        graphics.item(stack, x, y)

        // 2. حساب نسبة المتانة ولونها الديناميكي
        val (text, color) = getStatusText(stack)
        if (text.isNotEmpty()) {
            graphics.centeredText(font, Component.literal(text), x + 8, y + 17, color)
        }
    }

    private fun getStatusText(stack: ItemStack): Pair<String, Int> {
        if (stack.isEmpty) return Pair("", 0)

        if (stack.isDamageableItem) {
            val max = stack.maxDamage
            val damage = stack.damageValue
            val remaining = max - damage
            val percent = ((remaining.toDouble() / max) * 100).toInt().coerceIn(0, 100)

            val color = when {
                percent >= 75 -> 0xFF55FF55.toInt() // أخضر نيون ناصع
                percent >= 40 -> 0xFFFFFF55.toInt() // أصفر
                percent >= 20 -> 0xFFFFAA00.toInt() // برتقالي
                else -> {
                    // نبض تحذيري باللون الأحمر إذا الدرع قرب ينكسر
                    val flash = (System.currentTimeMillis() / 250) % 2 == 0L
                    if (flash) 0xFFFF2222.toInt() else 0xFFFF8888.toInt()
                }
            }
            return Pair("$percent%", color)
        } else if (stack.count > 1) {
            // إذا كانت بلوكة أو أكل باليد يعرض عدد الحبات
            return Pair("x${stack.count}", 0xFFE0E0E0.toInt())
        }

        return Pair("", 0)
    }
}
