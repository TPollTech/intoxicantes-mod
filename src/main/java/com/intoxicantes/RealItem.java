package com.intoxicantes;

import net.minecraft.world.item.Item;

/**
 * Real (R$): moeda do Mercado Esquinão do Gago.
 * Empilhavel em 64,serve pra trocar com o Gago e entre players.
 */
public class RealItem extends Item {
    public RealItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, Item.TooltipContext context,
            net.minecraft.world.item.component.TooltipDisplay display,
            java.util.function.Consumer<net.minecraft.network.chat.Component> output,
            net.minecraft.world.item.TooltipFlag flag) {
        output.accept(net.minecraft.network.chat.Component.translatable("commerce.intoxicantes.real.hint"));
        super.appendHoverText(stack, context, display, output, flag);
    }
}
