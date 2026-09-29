package org.kingpixel.cobblemonpatches.mixins.async;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixin into {@link Entity} guaranteeing that entity removal and discard operations
 * always execute on the main server thread, preventing asynchronous chunk/entity manager
 * race conditions and errors when third-party mods dispatch entity removal from Netty threads.
 *
 * @author Carlos Varas Alonso
 */
@Mixin(Entity.class)
public abstract class EntityRemoveAsyncMixin {

  /**
   * Marshals asynchronous entity removal invocations to the main server thread.
   *
   * @param reason   the reason for removal
   * @param original the wrapped original method operation
   */
  @WrapMethod(method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V")
  private void guardAsyncRemove(Entity.RemovalReason reason, Operation<Void> original) {
    Entity self = (Entity) (Object) this;
    if (!self.level().isClientSide()) {
      MinecraftServer server = self.getServer();
      if (server != null && !server.isSameThread()) {
        server.execute(() -> original.call(reason));
        return;
      }
    }
    original.call(reason);
  }
}
