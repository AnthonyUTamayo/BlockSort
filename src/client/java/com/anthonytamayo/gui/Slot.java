package com.anthonytamayo.gui;

import io.github.cottonmc.cotton.gui.widget.TooltipBuilder;
import io.github.cottonmc.cotton.gui.widget.WWidget;
import io.github.cottonmc.cotton.gui.widget.data.InputResult;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

public class Slot extends WWidget {
    final ClientPlayerEntity player = net.minecraft.client.MinecraftClient.getInstance().player;
    final ItemStack stack;

    public Slot(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public void paint(DrawContext context, int x, int y, int mouseX, int mouseY) {
        context.drawItem(stack, x, y);
    }

    @Override
    public InputResult onClick(int x, int y, int button) {
        assert player != null;
        if (!player.hasPermissionLevel(2) || !player.isCreative()) return InputResult.PROCESSED;
        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        String nbt = stack.getOrCreateNbt().toString();
        String command = "give @s " + itemId + nbt;
        if (button == 2) {
            command += " " + stack.getMaxCount();
        }
        player.networkHandler.sendCommand(command);
        return InputResult.PROCESSED;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void addTooltip(TooltipBuilder tooltip) {
        tooltip.add(Text.translatable(stack.getTranslationKey()));

    }
}

