package org.kingpixel.cobblemonpatches;

import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryOps;

/**
 * Utility class providing cached {@link DynamicOps} backed by the current server's {@link DynamicRegistryManager}.
 * Avoids rebuilding registry ops repeatedly during NBT serialization operations.
 */
public class OpsUtil {

  private static DynamicOps<NbtElement> ops;

  private OpsUtil() {
  }

  /**
   * Retrieves or initializes the cached RegistryOps instance for NbtElement operations.
   *
   * @return the cached DynamicOps instance, or NbtOps.INSTANCE if unavailable
   */
  public static DynamicOps<NbtElement> getOps() {
    if (ops == null && CobblemonPatches.server != null) {
      try {
        DynamicRegistryManager registryManager = CobblemonPatches.server.getRegistryManager();
        if (registryManager != null) {
          ops = RegistryOps.of(NbtOps.INSTANCE, registryManager);
        }
      } catch (Exception e) {
        CobblemonPatches.LOGGER.error("Failed to create RegistryOps", e);
      }
    }
    return ops != null ? ops : NbtOps.INSTANCE;
  }

  /**
   * Clears the cached DynamicOps instance when the server stops or lifecycle resets.
   */
  public static void clear() {
    ops = null;
  }
}
