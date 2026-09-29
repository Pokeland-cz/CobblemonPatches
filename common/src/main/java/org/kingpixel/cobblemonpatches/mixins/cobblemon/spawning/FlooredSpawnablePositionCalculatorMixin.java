package org.kingpixel.cobblemonpatches.mixins.cobblemon.spawning;

import com.cobblemon.mod.common.api.spawning.SpawningZone;
import com.cobblemon.mod.common.api.spawning.position.calculators.AreaSpawningInput;
import com.cobblemon.mod.common.api.spawning.position.calculators.FlooredSpawnablePositionCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optimizes FlooredSpawnablePositionCalculator.fits() by:
 * 1. Short-circuiting air blocks immediately without checking surrounding conditions.
 * 2. Directly querying (x, y + 1, z) on SpawningZone to eliminate BlockPos.above() heap allocations.
 */
@Mixin(value = FlooredSpawnablePositionCalculator.class, remap = false)
public interface FlooredSpawnablePositionCalculatorMixin {

  @Inject(method = "fits", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$fastFlooredFits(
      AreaSpawningInput input,
      CallbackInfoReturnable<Boolean> cir
  ) {
    BlockPos pos = input.getPosition();
    int x = pos.getX();
    int y = pos.getY();
    int z = pos.getZ();
    SpawningZone zone = input.getZone();

    BlockState floorState = zone.getBlockState(x, y, z, SpawningZone.Companion.getStoneState());
    if (floorState.isAir()) {
      cir.setReturnValue(false);
      return;
    }

    FlooredSpawnablePositionCalculator<?> self = (FlooredSpawnablePositionCalculator<?>) this;
    if (!self.getBaseCondition().invoke(floorState)) {
      cir.setReturnValue(false);
      return;
    }

    BlockState aboveState = zone.getBlockState(x, y + 1, z, SpawningZone.Companion.getStoneState());
    cir.setReturnValue(self.getSurroundingCondition().invoke(aboveState));
  }
}
