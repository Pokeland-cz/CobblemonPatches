package org.kingpixel.cobblemonpatches.mixins.cobblemon.spawning;

import com.cobblemon.mod.common.api.spawning.influence.SpawningZoneInfluence;
import com.cobblemon.mod.common.api.spawning.influence.detector.HabitatBlockDetector;
import com.cobblemon.mod.common.api.spawning.spawner.Spawner;
import com.cobblemon.mod.common.api.spawning.spawner.SpawningZoneInput;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Optimizes HabitatBlockDetector by clamping the excessive 128-block POI scan radius
 * to a realistic 48-block radius and caching resolved habitat zone influences per chunk for 10 seconds.
 */
@Mixin(value = HabitatBlockDetector.class, remap = false)
public abstract class HabitatBlockDetectorMixin {

  @Unique
  private static final int MAX_SEARCH_RADIUS = 48;

  @Unique
  private static final Cache<DetectorCacheKey, List<SpawningZoneInfluence>> CACHE = Caffeine.newBuilder()
      .expireAfterWrite(10, TimeUnit.SECONDS)
      .maximumSize(512)
      .build();

  /**
   * Retrieves cached habitat zone influences if available for the current chunk.
   */
  @Inject(method = "detectFromInput", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$getCachedHabitatInfluences(
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
   * Clamps the POI storage search radius from Cobblemon's hardcoded 128 blocks down to 48 blocks,
   * reducing scanned chunk sections by over 95%.
   */
  @WrapOperation(
      method = "detectFromInput",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/world/poi/PointOfInterestStorage;getPositions(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/world/poi/PointOfInterestStorage$OccupationStatus;)Ljava/util/stream/Stream;",
          remap = false
      )
  )
  private Stream<BlockPos> cobblemonPatches$clampHabitatSearchRadius(
      PointOfInterestStorage instance,
      Predicate<RegistryEntry<PointOfInterestType>> typePredicate,
      Predicate<BlockPos> posPredicate,
      BlockPos pos,
      int radius,
      PointOfInterestStorage.OccupationStatus occupationStatus,
      Operation<Stream<BlockPos>> original
  ) {
    int clampedRadius = Math.min(radius, MAX_SEARCH_RADIUS);
    return original.call(instance, typePredicate, posPredicate, pos, clampedRadius, occupationStatus);
  }

  /**
   * Stores computed habitat influences in the Caffeine cache upon completion.
   */
  @Inject(method = "detectFromInput", at = @At("RETURN"))
  private void cobblemonPatches$cacheHabitatInfluences(
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
