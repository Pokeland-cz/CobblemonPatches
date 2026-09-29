package org.kingpixel.cobblemonpatches.util;

import com.cobblemon.mod.common.api.spawning.SpawningZone;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A lazy list implementation for SpawningZone nearby blocks.
 * Eliminates tens of thousands of eager block lookups and memory allocations during spawn zone resolution.
 * Blocks are only scanned if a spawning rule actually inspects neededNearbyBlocks.
 */
public class LazyNearbyBlockList extends AbstractList<BlockState> {
  private final SpawningZone zone;
  private final int centerX;
  private final int centerY;
  private final int centerZ;
  private final int maxHorizontalRadius;
  private final int maxVerticalRadius;
  private List<BlockState> cached = null;

  public LazyNearbyBlockList(
      SpawningZone zone,
      int centerX,
      int centerY,
      int centerZ,
      int maxHorizontalRadius,
      int maxVerticalRadius
  ) {
    this.zone = zone;
    this.centerX = centerX;
    this.centerY = centerY;
    this.centerZ = centerZ;
    this.maxHorizontalRadius = maxHorizontalRadius;
    this.maxVerticalRadius = maxVerticalRadius;
  }

  private List<BlockState> resolve() {
    if (this.cached == null) {
      int baseX = this.zone.getBaseX();
      int baseY = this.zone.getBaseY();
      int baseZ = this.zone.getBaseZ();
      int length = this.zone.getLength();
      int height = this.zone.getHeight();
      int width = this.zone.getWidth();

      int minX = Math.max(this.centerX - this.maxHorizontalRadius, baseX);
      int minY = Math.max(this.centerY - this.maxVerticalRadius, baseY);
      int minZ = Math.max(this.centerZ - this.maxHorizontalRadius, baseZ);
      int maxX = Math.min(this.centerX + this.maxHorizontalRadius, baseX + length);
      int maxY = Math.min(this.centerY + this.maxVerticalRadius, baseY + height);
      int maxZ = Math.min(this.centerZ + this.maxHorizontalRadius, baseZ + width);

      int countX = Math.max(0, maxX - minX + 1);
      int countY = Math.max(0, maxY - minY + 1);
      int countZ = Math.max(0, maxZ - minZ + 1);
      List<BlockState> blocks = new ArrayList<>(countX * countY * countZ);

      BlockState stoneState = SpawningZone.Companion.getStoneState();
      for (int x = minX; x <= maxX; x++) {
        for (int y = minY; y <= maxY; y++) {
          for (int z = minZ; z <= maxZ; z++) {
            blocks.add(this.zone.getBlockState(x, y, z, stoneState));
          }
        }
      }
      this.cached = blocks;
    }
    return this.cached;
  }

  @Override
  public BlockState get(int index) {
    return resolve().get(index);
  }

  @Override
  public int size() {
    return resolve().size();
  }

  @Override
  public Iterator<BlockState> iterator() {
    return resolve().iterator();
  }
}
