package org.kingpixel.cobblemonpatches.mixins.cobblemon.spawning;

import com.cobblemon.mod.common.api.spawning.SpawningZone;
import org.kingpixel.cobblemonpatches.util.LazyNearbyBlockList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Optimizes SpawningZone nearbyBlocks by replacing eager cube scanning with LazyNearbyBlockList.
 */
@Mixin(value = SpawningZone.class, remap = false)
public abstract class SpawningZoneMixin {

  @Inject(method = "nearbyBlocks(IIIII)Ljava/util/List;", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$lazyNearbyBlocks(
      int centerX,
      int centerY,
      int centerZ,
      int maxHorizontalRadius,
      int maxVerticalRadius,
      CallbackInfoReturnable<List<BlockState>> cir
  ) {
    cir.setReturnValue(new LazyNearbyBlockList(
        (SpawningZone) (Object) this,
        centerX,
        centerY,
        centerZ,
        maxHorizontalRadius,
        maxVerticalRadius
    ));
  }
}
