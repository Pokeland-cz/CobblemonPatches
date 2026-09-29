package org.kingpixel.cobblemonpatches.mixins.cobblemon.spawning;

import com.cobblemon.mod.common.api.spawning.CobblemonSpawningZoneGenerator;
import com.cobblemon.mod.common.api.spawning.influence.detector.HabitatBlockDetector;
import com.cobblemon.mod.common.api.spawning.influence.detector.SpawningInfluenceDetector;
import com.cobblemon.mod.common.api.spawning.prospecting.IncenseSweetDetector;
import com.cobblemon.mod.common.api.spawning.prospecting.SaccharineLogSlatheredDetector;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Optimizes CobblemonSpawningZoneGenerator by filtering out no-op block-level detectors
 * from the voxel generation loop, eliminating tens of thousands of redundant method calls
 * and empty list allocations per spawn zone.
 */
@Mixin(value = CobblemonSpawningZoneGenerator.class, remap = false)
public abstract class CobblemonSpawningZoneGeneratorMixin {

  @Unique
  private static volatile Set<SpawningInfluenceDetector> cobblemonPatches$cachedActiveDetectors = null;

  @Unique
  private static volatile Set<SpawningInfluenceDetector> cobblemonPatches$lastDetectorsRef = null;

  /**
   * Filters the detectors evaluated during the per-voxel block loop. If all registered detectors
   * return empty lists for detectFromBlock, an empty set is returned to completely bypass the loop.
   */
  @WrapOperation(
      method = "generate",
      at = @At(
          value = "INVOKE",
          target = "Lcom/cobblemon/mod/common/api/spawning/influence/detector/SpawningInfluenceDetector$Companion;getDetectors()Ljava/util/Set;",
          ordinal = 0
      )
  )
  private Set<SpawningInfluenceDetector> cobblemonPatches$filterActiveBlockDetectors(
      SpawningInfluenceDetector.Companion instance,
      Operation<Set<SpawningInfluenceDetector>> original
  ) {
    Set<SpawningInfluenceDetector> allDetectors = original.call(instance);
    return cobblemonPatches$resolveActiveBlockDetectors(allDetectors);
  }

  @Unique
  private static Set<SpawningInfluenceDetector> cobblemonPatches$resolveActiveBlockDetectors(
      Set<SpawningInfluenceDetector> allDetectors
  ) {
    if (allDetectors == cobblemonPatches$lastDetectorsRef && cobblemonPatches$cachedActiveDetectors != null) {
      return cobblemonPatches$cachedActiveDetectors;
    }

    Set<SpawningInfluenceDetector> active = new HashSet<>();
    for (SpawningInfluenceDetector detector : allDetectors) {
      if (!cobblemonPatches$isKnownNoOpBlockDetector(detector)) {
        active.add(detector);
      }
    }

    Set<SpawningInfluenceDetector> result = active.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(active);
    cobblemonPatches$cachedActiveDetectors = result;
    cobblemonPatches$lastDetectorsRef = allDetectors;
    return result;
  }

  @Unique
  private static boolean cobblemonPatches$isKnownNoOpBlockDetector(SpawningInfluenceDetector detector) {
    return detector instanceof HabitatBlockDetector
        || detector instanceof SaccharineLogSlatheredDetector
        || detector instanceof IncenseSweetDetector;
  }
}
