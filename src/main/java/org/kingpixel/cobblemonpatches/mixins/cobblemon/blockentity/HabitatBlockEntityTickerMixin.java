package org.kingpixel.cobblemonpatches.mixins.cobblemon.blockentity;

import com.cobblemon.mod.common.api.habitats.spawningstyle.ActivatedHabitatSpawning;
import com.cobblemon.mod.common.block.habitat.HabitatBlockEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Optimizes HabitatBlockEntity ticking by throttling ActivatedHabitatSpawning activations
 * from every tick (20 Hz) to once every 20 ticks (1 Hz), distributed uniformly across ticks
 * based on the block's coordinate hash.
 */
@Mixin(value = HabitatBlockEntity.class, remap = false)
public abstract class HabitatBlockEntityTickerMixin {

  private HabitatBlockEntityTickerMixin() {
  }

  /**
   * Throttles the continuous TICK trigger activation of habitat blocks to 1 Hz,
   * staggered uniformly by coordinate hash to avoid tick spikes.
   */
  @WrapOperation(
      method = "TICKER$lambda$0",
      at = @At(
          value = "INVOKE",
          target = "Lcom/cobblemon/mod/common/api/habitats/spawningstyle/ActivatedHabitatSpawning;activate(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;)V",
          ordinal = 0
      )
  )
  private static void cobblemonPatches$throttleActivatedHabitatSpawning(
      ActivatedHabitatSpawning instance,
      ServerWorld world,
      BlockPos pos,
      Operation<Void> original
  ) {
    if (Math.floorMod(world.getTime() + pos.hashCode(), 20L) != 0L) {
      return;
    }
    original.call(instance, world, pos);
  }
}
