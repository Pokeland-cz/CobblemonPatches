package org.kingpixel.cobblemonpatches.mixins.cobblemon.entity;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor interface for {@link Entity} providing zero-overhead access
 * to private entity state such as onGround.
 */
@Mixin(Entity.class)
public interface EntityAccessor {

  /**
   * Retrieves the raw onGround collision status of the entity.
   *
   * @return true if on ground
   */
  @Accessor("onGround")
  boolean cobblemonpatches$isOnGround();
}
