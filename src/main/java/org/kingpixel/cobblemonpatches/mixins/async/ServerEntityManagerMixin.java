package org.kingpixel.cobblemonpatches.mixins.async;

import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.kingpixel.cobblemonpatches.PatchesUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link PersistentEntitySectionManager} to detect asynchronous chunk unloading.
 */
@Mixin(PersistentEntitySectionManager.class)
public abstract class ServerEntityManagerMixin {

  /**
   * Asserts that chunk unloading runs on the main server thread.
   *
   * @param ci callback information
   */
  @Inject(method = "processUnloads", at = @At("HEAD"))
  private void beforeUnloadChunks(CallbackInfo ci) {
    PatchesUtil.catchOp("unloadChunks is executing");
  }
}
