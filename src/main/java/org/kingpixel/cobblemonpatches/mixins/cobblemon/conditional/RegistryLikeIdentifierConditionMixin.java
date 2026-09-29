package org.kingpixel.cobblemonpatches.mixins.cobblemon.conditional;

import com.cobblemon.mod.common.api.conditional.RegistryLikeIdentifierCondition;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Caches identifier matching checks in {@link RegistryLikeIdentifierCondition}
 * to prevent repeated Identifier comparisons during spawning passes.
 */
@Mixin(value = RegistryLikeIdentifierCondition.class, remap = false)
public abstract class RegistryLikeIdentifierConditionMixin<T> {

  @Shadow
  public abstract ResourceLocation getIdentifier();

  @Unique
  private final ConcurrentHashMap<Holder<T>, Boolean> cobblemonPatches$cache = new ConcurrentHashMap<>(16);

  /**
   * Evaluates if the given registry entry matches the configured identifier, utilizing a thread-safe memoization cache.
   *
   * @param t   the registry entry to test
   * @param cir callback returnable with matching result
   */
  @Inject(method = "fits(Lnet/minecraft/core/Holder;)Z", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$cachedFits(Holder<T> t, CallbackInfoReturnable<Boolean> cir) {
    if (t == null) {
      cir.setReturnValue(false);
      return;
    }
    Boolean cached = this.cobblemonPatches$cache.get(t);
    if (cached != null) {
      cir.setReturnValue(cached);
      return;
    }
    boolean result = t.is(this.getIdentifier());
    this.cobblemonPatches$cache.put(t, result);
    cir.setReturnValue(result);
  }
}
