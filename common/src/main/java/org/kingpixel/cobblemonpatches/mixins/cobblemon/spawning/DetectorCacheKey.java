package org.kingpixel.cobblemonpatches.mixins.cobblemon.spawning;

import com.cobblemon.mod.common.api.spawning.spawner.Spawner;
import com.cobblemon.mod.common.api.spawning.spawner.SpawningZoneInput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Cache key identifying a spawner and chunk within a dimension for spawning influence detector caching.
 *
 * @param spawnerName the spawner identifier/name
 * @param dimension   the world identifier
 * @param chunkX      chunk X coordinate
 * @param chunkZ      chunk Z coordinate
 */
public record DetectorCacheKey(String spawnerName, ResourceLocation dimension, int chunkX, int chunkZ) {

  /**
   * Constructs a cache key from the given spawner and spawning zone input.
   *
   * @param spawner the spawner executing the zone check
   * @param input   the spawning zone input
   * @return the cache key for the center chunk and spawner type
   */
  public static DetectorCacheKey of(Spawner spawner, SpawningZoneInput input) {
    String spawnerName = spawner != null ? spawner.getName() : "";
    ResourceLocation dimension = input.getWorld().dimension().location();
    Vec3 center = input.getCenter();
    int chunkX = ((int) Math.floor(center.x())) >> 4;
    int chunkZ = ((int) Math.floor(center.z())) >> 4;
    return new DetectorCacheKey(spawnerName, dimension, chunkX, chunkZ);
  }
}
