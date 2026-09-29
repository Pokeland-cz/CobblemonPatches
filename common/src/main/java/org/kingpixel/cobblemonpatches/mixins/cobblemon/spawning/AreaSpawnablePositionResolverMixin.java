package org.kingpixel.cobblemonpatches.mixins.cobblemon.spawning;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.spawning.SpawningZone;
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.api.spawning.position.AreaSpawnablePosition;
import com.cobblemon.mod.common.api.spawning.position.AreaSpawnablePositionResolver;
import com.cobblemon.mod.common.api.spawning.position.calculators.AreaSpawnablePositionCalculator;
import com.cobblemon.mod.common.api.spawning.position.calculators.AreaSpawningInput;
import com.cobblemon.mod.common.api.spawning.spawner.Spawner;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optimizes AreaSpawnablePositionResolver.resolve() by:
 * 1. Eliminating tens of thousands of Vec3 allocations per spawn pass via primitive distance-squared checks.
 * 2. Eliminating Kotlin lambda and iterator allocations in inner loops.
 */
@Mixin(value = AreaSpawnablePositionResolver.class, remap = false)
public interface AreaSpawnablePositionResolverMixin {

  @Inject(method = "resolve", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatches$fastResolve(
      Spawner spawner,
      List<AreaSpawnablePositionCalculator<?>> spawnablePositionCalculators,
      SpawningZone zone,
      CallbackInfoReturnable<List<AreaSpawnablePosition>> cir
  ) {
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(1, 2, 3);
    AreaSpawningInput input = new AreaSpawningInput(spawner, pos, zone);
    List<AreaSpawnablePosition> spawnablePositions = new ArrayList<>();

    int baseX = zone.getBaseX();
    int baseY = zone.getBaseY();
    int baseZ = zone.getBaseZ();

    List<Vec3> nearbyEntities = zone.getNearbyEntityPositions();
    boolean hasNearbyEntities = nearbyEntities != null && !nearbyEntities.isEmpty();
    double minDist = Cobblemon.INSTANCE.getConfig().getMinimumDistanceBetweenEntities();
    double minDistSq = minDist * minDist;
    Entity causeEntity = zone.getCause().getEntity();

    List<SpawningInfluence> spawnerInfluences = spawner.getInfluences();
    boolean hasInfluences = spawnerInfluences != null && !spawnerInfluences.isEmpty();
    ServerLevel world = input.getWorld();

    int maxX = baseX + zone.getLength();
    int maxY = baseY + zone.getHeight();
    int maxZ = baseZ + zone.getWidth();

    for (int x = baseX; x < maxX; x++) {
      for (int y = baseY; y < maxY; y++) {
        for (int z = baseZ; z < maxZ; z++) {
          if (hasNearbyEntities && cobblemonPatches$isTooCloseToEntity(nearbyEntities, x, y, z, minDistSq, causeEntity)) {
            continue;
          }

          pos.set(x, y, z);
          AreaSpawnablePosition spawnablePosition = cobblemonPatches$processVoxelPosition(
              input, spawnablePositionCalculators, spawnerInfluences, hasInfluences, world, zone
          );

          if (spawnablePosition != null) {
            spawnablePositions.add(spawnablePosition);
            pos = new BlockPos.MutableBlockPos(1, 2, 3);
            input.setPosition(pos);
          }
        }
      }
    }

    cir.setReturnValue(spawnablePositions);
  }

  @Unique
  private static AreaSpawnablePosition cobblemonPatches$processVoxelPosition(
      AreaSpawningInput input,
      List<AreaSpawnablePositionCalculator<?>> calculators,
      List<SpawningInfluence> spawnerInfluences,
      boolean hasInfluences,
      ServerLevel world,
      SpawningZone zone
  ) {
    AreaSpawnablePositionCalculator<?> fittedCalculator = cobblemonPatches$findFittedCalculator(
        calculators, input, spawnerInfluences, hasInfluences, world
    );
    if (fittedCalculator == null) {
      return null;
    }

    AreaSpawnablePosition spawnablePosition = fittedCalculator.calculate(input);
    if (spawnablePosition != null) {
      cobblemonPatches$applyInfluences(spawnablePosition, zone, spawnerInfluences);
    }
    return spawnablePosition;
  }

  @Unique
  private static boolean cobblemonPatches$isTooCloseToEntity(
      List<Vec3> nearbyEntities,
      int x,
      int y,
      int z,
      double minDistSq,
      Entity causeEntity
  ) {
    for (int i = 0; i < nearbyEntities.size(); i++) {
      Vec3 entPos = nearbyEntities.get(i);
      double dx = entPos.x - x;
      double dy = entPos.y - y;
      double dz = entPos.z - z;
      double distSq = dx * dx + dy * dy + dz * dz;
      if (distSq < minDistSq && (causeEntity == null || !causeEntity.position().closerThan(entPos, 0.001))) {
        return true;
      }
    }
    return false;
  }

  @Unique
  private static AreaSpawnablePositionCalculator<?> cobblemonPatches$findFittedCalculator(
      List<AreaSpawnablePositionCalculator<?>> spawnablePositionCalculators,
      AreaSpawningInput input,
      List<SpawningInfluence> spawnerInfluences,
      boolean hasInfluences,
      ServerLevel world
  ) {
    for (int c = 0; c < spawnablePositionCalculators.size(); c++) {
      AreaSpawnablePositionCalculator<?> calc = spawnablePositionCalculators.get(c);
      if (!calc.fits(input)) {
        continue;
      }

      if (!hasInfluences || cobblemonPatches$isAllowedByInfluences(calc, input.getPosition(), spawnerInfluences, world)) {
        return calc;
      }
    }
    return null;
  }

  @Unique
  private static boolean cobblemonPatches$isAllowedByInfluences(
      AreaSpawnablePositionCalculator<?> calc,
      BlockPos pos,
      List<SpawningInfluence> spawnerInfluences,
      ServerLevel world
  ) {
    for (int k = 0; k < spawnerInfluences.size(); k++) {
      if (!spawnerInfluences.get(k).isAllowedPosition(world, pos, calc)) {
        return false;
      }
    }
    return true;
  }

  @Unique
  private static void cobblemonPatches$applyInfluences(
      AreaSpawnablePosition spawnablePosition,
      SpawningZone zone,
      List<SpawningInfluence> spawnerInfluences
  ) {
    List<SpawningInfluence> zoneInfluences = zone.getInfluences(spawnablePosition);
    if (zoneInfluences != null) {
      for (int i = 0; i < zoneInfluences.size(); i++) {
        SpawningInfluence inf = zoneInfluences.get(i);
        spawnablePosition.getInfluences().add(inf);
        inf.affectSpawnablePosition(spawnablePosition);
      }
    }
    if (spawnerInfluences != null) {
      for (int i = 0; i < spawnerInfluences.size(); i++) {
        SpawningInfluence inf = spawnerInfluences.get(i);
        inf.affectSpawnablePosition(spawnablePosition);
      }
    }
  }
}
