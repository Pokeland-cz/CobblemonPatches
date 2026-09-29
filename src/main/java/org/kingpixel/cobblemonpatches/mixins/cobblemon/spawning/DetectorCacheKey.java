package org.kingpixel.cobblemonpatches.mixins.cobblemon.spawning;

import com.cobblemon.mod.common.api.spawning.spawner.Spawner;
import com.cobblemon.mod.common.api.spawning.spawner.SpawningZoneInput;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * Cache key identifying a spawner and chunk within a dimension for spawning influence detector caching.
 *
 * @param spawnerName the spawner identifier/name
 * @param dimension   the world identifier
 * @param chunkX      chunk X coordinate
 * @param chunkZ      chunk Z coordinate
 */
public record DetectorCacheKey(String spawnerName, Identifier dimension, int chunkX, int chunkZ) {

  /**
   * Constructs a cache key from the given spawner and spawning zone input.
   *
   * @param spawner the spawner executing the zone check
   * @param input   the spawning zone input
   * @return the cache key for the center chunk and spawner type
   */
  public static DetectorCacheKey of(Spawner spawner, SpawningZoneInput input) {
    String spawnerName = spawner != null ? spawner.getName() : "";
    Identifier dimension = input.getWorld().getRegistryKey().getValue();
    Vec3d center = input.getCenter();
    int chunkX = ((int) Math.floor(center.getX())) >> 4;
    int chunkZ = ((int) Math.floor(center.getZ())) >> 4;
    return new DetectorCacheKey(spawnerName, dimension, chunkX, chunkZ);
  }
}
