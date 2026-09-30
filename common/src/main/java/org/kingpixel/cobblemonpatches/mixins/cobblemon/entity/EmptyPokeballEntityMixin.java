package org.kingpixel.cobblemonpatches.mixins.cobblemon.entity;

import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link EmptyPokeBallEntity} to prevent {@link NullPointerException} crashes
 * during Pokeball capture collisions if the throwing player disconnected in the interim.
 */
@Mixin(value = EmptyPokeBallEntity.class, remap = false)
public abstract class EmptyPokeballEntityMixin extends ThrowableItemProjectile {

  /**
   * Constructs an instance of EmptyPokeballEntityMixin.
   *
   * @param entityType entity type definition
   * @param d          x position
   * @param e          y position
   * @param f          z position
   * @param world      world instance
   */
  protected EmptyPokeballEntityMixin(EntityType<? extends ThrowableItemProjectile> entityType, double d, double e, double f, Level world) {
    super(entityType, d, e, f, world);
  }

  /**
   * Guards beginCapture execution to ensure the throwing owner entity is still present and valid.
   *
   * @param ci callback info
   */
  @Inject(method = "beginCapture", at = @At("HEAD"), cancellable = true, remap = false)
  private void guardBeginCapture(CallbackInfo ci) {
    if (this.getOwner() == null) {
      ci.cancel();
    }
  }
}
