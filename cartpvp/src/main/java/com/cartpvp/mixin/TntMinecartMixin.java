package com.cartpvp.mixin;

import com.cartpvp.CartPvpMod;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Rút ngòi xe TNT: mặc định 80 tick (4 giây) -> 10 tick (0.5 giây).
// Chỉ có tác dụng ở nơi chạy server logic (singleplayer hoặc server có cài mod).
@Mixin(TntMinecartEntity.class)
public abstract class TntMinecartMixin {
    @Shadow private int fuseTicks;

    @Inject(method = "prime", at = @At("TAIL"))
    private void cartpvp$fastFuse(CallbackInfo ci) {
        this.fuseTicks = CartPvpMod.FUSE_TICKS;
    }
}
