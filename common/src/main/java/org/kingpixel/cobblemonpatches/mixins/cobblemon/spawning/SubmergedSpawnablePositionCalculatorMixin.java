package org.kingpixel.cobblemonpatches.mixins.cobblemon.spawning;

import com.cobblemon.mod.common.api.spawning.SpawningZone;
import com.cobblemon.mod.common.api.spawning.position.calculators.AreaSpawningInput;
import com.cobblemon.mod.common.api.spawning.position.calculators.SubmergedSpawnablePositionCalculator;
import kotlin.jvm.functions.Function1;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optimizes SubmergedSpawnablePositionCalculator.fits() by:
 * 1. Fast-failing if center state is solid or air.
 * 2. Directly querying (x, y - 1, z) and (x, y + 1, z) to eliminate BlockPos.below() / above() allocations.
 */
@Mixin(value = SubmergedSpawnablePositionCalculator.class, remap = false)
public abstract class SubmergedSpawnablePositionCalculatorMixin {

  @Inject(method = "fits", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$fastSubmergedFits(
      AreaSpawningInput input,
      CallbackInfoReturnable<Boolean> cir
  ) {
    BlockPos pos = input.getPosition();
    int x = pos.getX();
    int y = pos.getY();
    int z = pos.getZ();
    SpawningZone zone = input.getZone();

    BlockState centerState = zone.getBlockState(x, y, z, SpawningZone.Companion.getStoneState());
    if (centerState.isAir() || centerState.isSolid()) {
      cir.setReturnValue(false);
      return;
    }

    SubmergedSpawnablePositionCalculator self = (SubmergedSpawnablePositionCalculator) (Object) this;
    Function1<BlockState, Boolean> condition = null;
    for (Function1<BlockState, Boolean> fc : self.getFluidConditions()) {
      if (fc.invoke(centerState)) {
        condition = fc;
        break;
      }
    }

    if (condition == null) {
      cir.setReturnValue(false);
      return;
    }

    BlockState belowState = zone.getBlockState(x, y - 1, z, SpawningZone.Companion.getStoneState());
    if (!condition.invoke(belowState)) {
      cir.setReturnValue(false);
      return;
    }

    BlockState aboveState = zone.getBlockState(x, y + 1, z, SpawningZone.Companion.getStoneState());
    cir.setReturnValue(condition.invoke(aboveState));
  }
}
