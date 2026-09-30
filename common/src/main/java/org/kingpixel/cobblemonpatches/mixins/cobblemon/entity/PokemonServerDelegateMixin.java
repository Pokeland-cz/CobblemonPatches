package org.kingpixel.cobblemonpatches.mixins.cobblemon.entity;

import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonServerDelegate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optimizes server-side data synchronization for Pokemon entities by throttling static attribute
 * updates (species, aspects, ball, scale, friendship, marks) to 1 Hz (every 20 ticks) when unridden,
 * while maintaining responsive per-tick movement and pose type updates.
 */
@Mixin(value = PokemonServerDelegate.class, remap = false)
public abstract class PokemonServerDelegateMixin {

  @Shadow public PokemonEntity entity;

  @Shadow public abstract void updatePoseType();

  @Inject(method = "updateTrackedValues", at = @At("HEAD"), cancellable = true)
  private void cobblemonpatches$throttleTrackedValues(CallbackInfo ci) {
    if (this.entity == null || !this.entity.isAlive() || this.entity.isRemoved()) {
      ci.cancel();
      return;
    }

    if (this.entity.tickCount % 20 != 0 && this.entity.getPassengers().isEmpty()) {
      boolean isMoving;
      if (PoseType.Companion.getFLYING_POSES().contains(this.entity.getCurrentPoseType())) {
        isMoving = this.entity.isPokemonFlying();
      } else {
        isMoving = this.entity.isPokemonWalking();
      }

      if (this.entity.getEntityData().get(PokemonEntity.getMOVING()) != isMoving) {
        this.entity.getEntityData().set(PokemonEntity.getMOVING(), isMoving);
      }

      this.updatePoseType();
      ci.cancel();
    }
  }
}
