package com.cartpvp.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.function.Predicate;

/**
 * Thứ tự:
 *  1. Đặt ray vào block đang nhìn.
 *  2. Lùi 1 block (xa ray).
 *  3. Quẹt bật lửa: đặt lửa vào ô nằm giữa người bắn và ray.
 *  4. Quay mặt lại phía ray, đặt xe TNT lên ray.
 *  5. Bắn nỏ qua lửa vào xe (mũi tên xuyên lửa bị bốc cháy -> xe nổ ngay).
 *
 * Hotbar cần có: ray, xe TNT, bật lửa, nỏ ĐÃ NẠP. Hãy đứng thẳng hàng với ray.
 */
public class CartMacro {
    private enum S { IDLE, BACK, FIRE, PLACE_CART, SHOOT }

    private static S state = S.IDLE;
    private static BlockPos railPos, fireCell;
    private static Vec3d startPos;
    private static int timer;

    public static void start(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.interactionManager == null || state != S.IDLE) return;
        if (!(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            msg(mc, "Hãy nhìn vào block để đặt ray."); return;
        }
        if (hotbar(p, s -> s.isIn(ItemTags.RAILS)) < 0) { msg(mc, "Thiếu ray trên hotbar."); return; }
        if (hotbar(p, s -> s.isOf(Items.TNT_MINECART)) < 0) { msg(mc, "Thiếu xe TNT trên hotbar."); return; }
        if (hotbar(p, s -> s.isOf(Items.FLINT_AND_STEEL)) < 0) { msg(mc, "Thiếu bật lửa trên hotbar."); return; }
        if (hotbar(p, s -> s.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(s)) < 0) { msg(mc, "Thiếu nỏ đã nạp trên hotbar."); return; }

        // 1. Đặt ray
        select(p, s -> s.isIn(ItemTags.RAILS));
        railPos = hit.getBlockPos().offset(hit.getSide());
        mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, hit);

        // Ô đặt lửa = ô kề ray, phía người chơi đang đứng
        Vec3d rel = p.getPos().subtract(Vec3d.ofBottomCenter(railPos));
        Direction back = Math.abs(rel.x) > Math.abs(rel.z)
                ? (rel.x > 0 ? Direction.EAST : Direction.WEST)
                : (rel.z > 0 ? Direction.SOUTH : Direction.NORTH);
        fireCell = railPos.offset(back);

        aim(p, Vec3d.ofCenter(railPos));   // quay mặt về ray để phím lùi đi thẳng ra xa
        startPos = p.getPos();
        timer = 0;
        state = S.BACK;
    }

    public static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (state == S.IDLE) return;
        if (p == null || mc.world == null || mc.interactionManager == null) { reset(mc); return; }
        timer++;

        switch (state) {
            case BACK -> {                       // 2. Lùi 1 block
                mc.options.backKey.setPressed(true);
                double dx = p.getX() - startPos.x, dz = p.getZ() - startPos.z;
                if (dx * dx + dz * dz >= 1.2 * 1.2 || timer > 40) {
                    mc.options.backKey.setPressed(false);
                    state = S.FIRE; timer = 0;
                }
            }
            case FIRE -> {                       // 3. Quẹt bật lửa
                select(p, s -> s.isOf(Items.FLINT_AND_STEEL));
                Vec3d top = new Vec3d(fireCell.getX() + 0.5, fireCell.getY(), fireCell.getZ() + 0.5);
                mc.interactionManager.interactBlock(p, Hand.MAIN_HAND,
                        new BlockHitResult(top, Direction.UP, fireCell.down(), false));
                state = S.PLACE_CART; timer = 0;
            }
            case PLACE_CART -> {                 // 4. Quay lại phía ray, đặt xe TNT
                if (timer < 2) return;           // chờ lửa xuất hiện
                aim(p, Vec3d.ofCenter(railPos));
                select(p, s -> s.isOf(Items.TNT_MINECART));
                Vec3d at = new Vec3d(railPos.getX() + 0.5, railPos.getY() + 0.1, railPos.getZ() + 0.5);
                mc.interactionManager.interactBlock(p, Hand.MAIN_HAND,
                        new BlockHitResult(at, Direction.UP, railPos, false));
                state = S.SHOOT; timer = 0;
            }
            case SHOOT -> {                      // 5. Bắn nỏ qua lửa vào xe
                TntMinecartEntity cart = findCart(mc);
                if (cart == null) { if (timer > 10) reset(mc); return; }
                aim(p, cart.getPos().add(0, 0.3, 0));
                select(p, s -> s.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(s));
                mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
                reset(mc);
            }
            default -> {}
        }
    }

    private static void aim(ClientPlayerEntity p, Vec3d target) {
        Vec3d t = target.subtract(p.getEyePos());
        p.setYaw((float) (MathHelper.atan2(t.z, t.x) * 57.29578) - 90f);
        p.setPitch((float) -(MathHelper.atan2(t.y, Math.sqrt(t.x * t.x + t.z * t.z)) * 57.29578));
    }

    private static TntMinecartEntity findCart(MinecraftClient mc) {
        List<TntMinecartEntity> l = mc.world.getEntitiesByClass(
                TntMinecartEntity.class, new Box(railPos).expand(1.5), e -> true);
        return l.isEmpty() ? null : l.get(0);
    }

    private static int hotbar(ClientPlayerEntity p, Predicate<ItemStack> test) {
        for (int i = 0; i < 9; i++) if (test.test(p.getInventory().getStack(i))) return i;
        return -1;
    }

    private static void select(ClientPlayerEntity p, Predicate<ItemStack> test) {
        int i = hotbar(p, test);
        if (i >= 0) p.getInventory().selectedSlot = i;
    }

    private static void reset(MinecraftClient mc) {
        mc.options.backKey.setPressed(false);
        state = S.IDLE;
    }

    private static void msg(MinecraftClient mc, String s) {
        if (mc.player != null) mc.player.sendMessage(Text.literal("[CartPvP] " + s), true);
    }
}
