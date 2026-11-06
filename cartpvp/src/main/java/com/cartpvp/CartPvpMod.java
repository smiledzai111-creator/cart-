package com.cartpvp;

import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class CartPvpMod implements ModInitializer {
    public static final int FUSE_TICKS = 10; // 0.5 giây
    public static final Item CART_CROSSBOW = new CartCrossbowItem(new Item.Settings().maxCount(1).maxDamage(465));

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, Identifier.of("cartpvp", "cart_crossbow"), CART_CROSSBOW);
    }
}
