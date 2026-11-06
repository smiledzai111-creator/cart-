package com.cartpvp;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class CartCrossbowItem extends Item {
    private static final int CHARGE_TICKS = 25;   // thời gian lên dây
    private static final double SPEED = 2.2;      // tốc độ bay của xe
    private static final int COOLDOWN = 30;

    public CartCrossbowItem(Settings settings) { super(settings); }

    @Override
    public UseAction getUseAction(ItemStack stack) { return UseAction.CROSSBOW; }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) { return 72000; }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!player.isCreative() && findCart(player) < 0) return TypedActionResult.fail(stack);
        player.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player) || world.isClient) return;
        if (getMaxUseTime(stack, user) - remainingUseTicks < CHARGE_TICKS) return;

        if (!player.isCreative()) {
            int slot = findCart(player);
            if (slot < 0) return;
            player.getInventory().getStack(slot).decrement(1);
            stack.damage(1, player, EquipmentSlot.MAINHAND);
        }

        Vec3d look = player.getRotationVec(1.0F);
        Vec3d pos = player.getEyePos().add(look.multiply(1.2)).subtract(0, 0.3, 0);
        TntMinecartEntity cart = new TntMinecartEntity(world, pos.x, pos.y, pos.z);
        cart.setVelocity(look.multiply(SPEED));
        world.spawnEntity(cart);
        cart.prime(); // bắt đầu đếm ngòi; cũng nổ khi va chạm tường ở tốc độ cao

        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 1.0F, 0.8F);
        player.getItemCooldownManager().set(this, COOLDOWN);
    }

    private static int findCart(PlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getStack(i).isOf(Items.TNT_MINECART)) return i;
        }
        return -1;
    }
}
