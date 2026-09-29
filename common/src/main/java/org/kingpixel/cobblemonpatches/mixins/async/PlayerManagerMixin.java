package org.kingpixel.cobblemonpatches.mixins.async;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.kingpixel.cobblemonpatches.PatchesUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link PlayerList} to ensure player removal events are performed on the main thread.
 */
@Mixin(PlayerList.class)
public abstract class PlayerManagerMixin {

  /**
   * Asserts that player removal is executed on the main server thread.
   *
   * @param player the player entity being removed
   * @param ci     callback information
   */
  @Inject(
    method = "remove",
    at = @At("HEAD")
  )
  private void remove(ServerPlayer player, CallbackInfo ci) {
    PatchesUtil.catchOp("Remove Player");
  }
}
