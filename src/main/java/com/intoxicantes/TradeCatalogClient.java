package com.intoxicantes;

import java.util.List;

import net.minecraft.world.item.ItemStack;

/**
 * v1.2.44 — Espelho CLIENT do catalogo de colheitas: a tela do ponto do
 * traficante so precisa dos ICONES (ItemStack de demonstracao) na MESMA
 * ordem do TradeCatalog.harvests() do servidor. Nenhuma regra de negocio
 * aqui — preco/estoque/entrega sao todos server-side.
 */
final class TradeCatalogClient {
    private TradeCatalogClient() {}

    static List<ItemStack> colheitas() {
        return List.of(
                new ItemStack(IntoxicantesMod.LOUPULO_FRESCO, 8),
                new ItemStack(IntoxicantesMod.UVA, 8),
                new ItemStack(net.minecraft.world.item.Items.WHEAT, 8),
                new ItemStack(IntoxicantesMod.CANA_DE_ACUCAR, 8),
                new ItemStack(net.minecraft.world.item.Items.HONEY_BOTTLE, 4),
                new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE, 8));
    }
}
