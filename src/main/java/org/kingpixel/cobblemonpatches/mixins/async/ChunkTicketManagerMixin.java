package org.kingpixel.cobblemonpatches.mixins.async;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link DistanceManager} to prevent {@link NullPointerException}s
 * when handling players leaving chunk sections whose player sets have already been cleared or removed.
 */
@Mixin(DistanceManager.class)
public abstract class ChunkTicketManagerMixin {

  @Shadow
  private Long2ObjectMap<ObjectSet<ServerPlayer>> playersPerChunk;

  /**
   * Prevents NullPointerException by canceling chunk leave processing if no player set
   * exists for the target chunk coordinate.
   *
   * @param pos    the chunk section position being left
   * @param player the player entity leaving the section
   * @param ci     callback information to cancel method execution if unmapped
   */
  @Inject(
    method = "removePlayer",
    at = @At("HEAD"),
    cancellable = true
  )
  private void guardHandleChunkLeave(
    SectionPos pos,
    ServerPlayer player,
    CallbackInfo ci
  ) {
    long l = pos.chunk().toLong();
    ObjectSet<ServerPlayer> objectSet = this.playersPerChunk.get(l);
    if (objectSet == null) {
      ci.cancel();
    }
  }
}
