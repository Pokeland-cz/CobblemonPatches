package org.kingpixel.cobblemonpatches.mixins.cobblemon.spawning;

import com.cobblemon.mod.common.api.spawning.influence.SpawningZoneInfluence;
import com.cobblemon.mod.common.api.spawning.prospecting.SaccharineLogSlatheredDetector;
import com.cobblemon.mod.common.api.spawning.spawner.Spawner;
import com.cobblemon.mod.common.api.spawning.spawner.SpawningZoneInput;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Optimizes SaccharineLogSlatheredDetector by caching resolved honey-slathered log influences
 * per chunk for 10 seconds, eliminating expensive repeated POI stream queries.
 */
@Mixin(value = SaccharineLogSlatheredDetector.class, remap = false)
public abstract class SaccharineLogSlatheredDetectorMixin {

  @Unique
  private static final Cache<DetectorCacheKey, List<SpawningZoneInfluence>> CACHE = Caffeine.newBuilder()
      .expireAfterWrite(10, TimeUnit.SECONDS)
      .maximumSize(512)
      .build();

  /**
   * Returns cached saccharine log influences if present for the current chunk.
   */
  @Inject(method = "detectFromInput", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$getCachedLogInfluences(
      Spawner spawner,
      SpawningZoneInput input,
      CallbackInfoReturnable<List<SpawningZoneInfluence>> cir
  ) {
    DetectorCacheKey key = DetectorCacheKey.of(spawner, input);
    List<SpawningZoneInfluence> cached = CACHE.getIfPresent(key);
    if (cached != null) {
      cir.setReturnValue(cached);
    }
  }

  /**
   * Stores newly computed saccharine log influences in the Caffeine cache upon return.
   */
  @Inject(method = "detectFromInput", at = @At("RETURN"))
  private void cobblemonPatches$cacheLogInfluences(
      Spawner spawner,
      SpawningZoneInput input,
      CallbackInfoReturnable<List<SpawningZoneInfluence>> cir
  ) {
    List<SpawningZoneInfluence> result = cir.getReturnValue();
    if (result != null) {
      DetectorCacheKey key = DetectorCacheKey.of(spawner, input);
      CACHE.put(key, Collections.unmodifiableList(result));
    }
  }
}
