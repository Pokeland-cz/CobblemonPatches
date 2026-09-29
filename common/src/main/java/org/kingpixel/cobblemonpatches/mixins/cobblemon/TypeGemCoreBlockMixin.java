package org.kingpixel.cobblemonpatches.mixins.cobblemon;

import com.cobblemon.mod.common.api.tags.CobblemonBlockTags;
import com.cobblemon.mod.common.block.TypeGemClusterBlock;
import com.cobblemon.mod.common.block.TypeGemCoreBlock;
import kotlin.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optimizes {@link TypeGemCoreBlock} growth during world generation and random ticking.
 * Eliminates massive allocations from BFS (LinkedList/HashSet/Pair), removes redundant
 * collections shuffling, avoids map/registry key conversions, and skips wasteful block updates.
 */
@Mixin(value = TypeGemCoreBlock.class, remap = false)
public abstract class TypeGemCoreBlockMixin {

  @Unique
  private static final Direction[] DIRECTIONS = Direction.values();

  /**
   * Replaces forced growth loop with an allocation-free growth routine.
   *
   * @param level      the structure world access instance
   * @param pos        the core block position
   * @param random     random generator
   * @param percentage target growth fill percentage
   * @param ci         callback info
   */
  @Inject(method = "forceGrow", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatchesForceGrow(WorldGenLevel level, BlockPos pos, RandomSource random, float percentage,
                                         CallbackInfo ci) {
    float desiredLength = TypeGemCoreBlock.MAX_CONNECTED_GEMS * percentage;
    while (true) {
      Pair<Boolean, Integer> growInfo = performOptimizedGrow(level, pos, random, true);
      boolean grown = growInfo.getFirst();
      int clusterSize = growInfo.getSecond();

      if (!grown || clusterSize >= desiredLength) {
        break;
      }
    }
    ci.cancel();
  }

  /**
   * Replaces standard growth tick with an allocation-free growth routine.
   *
   * @param level  the structure world access instance
   * @param pos    the core block position
   * @param random random generator
   * @param forced whether growth is forced
   * @param cir    callback returnable with growth status and cluster size
   */
  @Inject(method = "grow", at = @At("HEAD"), cancellable = true)
  private void cobblemonPatchesGrow(WorldGenLevel level, BlockPos pos, RandomSource random, boolean forced,
                                    CallbackInfoReturnable<Pair<Boolean, Integer>> cir) {
    cir.setReturnValue(performOptimizedGrow(level, pos, random, forced));
    cir.cancel();
  }

  /**
   * Performs an optimized cluster growth check and placement using fixed-size arrays.
   *
   * @param level  the world level
   * @param pos    core position
   * @param random random generator
   * @param forced whether forced growth is enabled
   * @return pair containing whether growth occurred and total gem count
   */
  @Unique
  private Pair<Boolean, Integer> performOptimizedGrow(WorldGenLevel level, BlockPos pos,
                                                      RandomSource random, boolean forced) {
    int maxCapacity = Math.max(16, TypeGemCoreBlock.MAX_CONNECTED_GEMS);
    BlockPos[] gemPositions = new BlockPos[maxCapacity];
    BlockState[] gemStates = new BlockState[maxCapacity];
    int gemCount = collectConnectedGems(level, pos, gemPositions, gemStates);

    if (gemCount >= TypeGemCoreBlock.MAX_CONNECTED_GEMS) {
      if (!hasBreathingRoom(level, gemPositions, gemCount)) {
        return new Pair<>(false, gemCount);
      }
      updateStuntState(level, gemPositions, gemCount, true);
    } else {
      updateStuntState(level, gemPositions, gemCount, false);
    }

    int[] gemIndices = new int[gemCount];
    for (int i = 0; i < gemCount; i++) {
      gemIndices[i] = i;
    }
    shuffleIndices(gemIndices, gemCount, random);

    for (int g = 0; g < gemCount; g++) {
      int idx = gemIndices[g];
      BlockPos gemPos = gemPositions[idx];
      BlockState gemState = gemStates[idx];

      TypeGemClusterBlock clusterBlock = TypeGemClusterBlock.Companion.clusterFromGemBlock(gemState.getBlock());
      if (clusterBlock == null) {
        continue;
      }

      int newGemCount = tryGrowFromGem(level, gemPos, clusterBlock, random, forced, gemCount);
      if (newGemCount > 0) {
        return new Pair<>(true, newGemCount);
      }
    }

    return new Pair<>(false, gemCount);
  }

  /**
   * Attempts to grow a new cluster block from an existing gem position into adjacent air blocks.
   *
   * @param level           world access
   * @param gemPos          position of the source gem block
   * @param clusterBlock    the target cluster block type
   * @param random          random generator
   * @param forced          whether growth is forced to advance
   * @param currentGemCount current count of connected gems
   * @return updated gem count if growth succeeded, 0 otherwise
   */
  @Unique
  private static int tryGrowFromGem(WorldGenLevel level, BlockPos gemPos, TypeGemClusterBlock clusterBlock,
                                    RandomSource random, boolean forced, int currentGemCount) {
    int[] dirIndices = {0, 1, 2, 3, 4, 5};
    shuffleIndices(dirIndices, 6, random);

    for (int d = 0; d < 6; d++) {
      Direction dir = DIRECTIONS[dirIndices[d]];
      BlockPos targetPos = gemPos.relative(dir);

      if (!level.getBlockState(targetPos).isAir()) {
        continue;
      }

      placeClusterBlock(level, targetPos, clusterBlock, dir);

      int finalGemCount = currentGemCount;
      if (forced) {
        advanceCluster(level, targetPos, random);
        if (isGemBlock(level.getBlockState(targetPos))) {
          finalGemCount++;
        }
      }
      return finalGemCount;
    }
    return 0;
  }

  /**
   * Places a fresh cluster block with the specified facing direction.
   *
   * @param level        world access
   * @param targetPos    position to place the cluster
   * @param clusterBlock cluster block instance
   * @param dir          facing direction
   */
  @Unique
  private static void placeClusterBlock(WorldGenLevel level, BlockPos targetPos,
                                        TypeGemClusterBlock clusterBlock, Direction dir) {
    BlockState placeState = clusterBlock.defaultBlockState()
      .setValue(TypeGemClusterBlock.Companion.getFACING(), dir)
      .setValue(TypeGemClusterBlock.Companion.getSTAGE(), 0)
      .setValue(TypeGemClusterBlock.Companion.getSHOULD_GROW(), true)
      .setValue(TypeGemClusterBlock.Companion.getSTUNTED(), false);
    level.setBlock(targetPos, placeState, getUpdateFlags(level));
  }

  /**
   * Computes appropriate block update flags depending on world type.
   *
   * @param level world access
   * @return block update flag bitmask
   */
  @Unique
  private static int getUpdateFlags(LevelAccessor level) {
    return level instanceof ServerLevel ? Block.UPDATE_ALL : Block.UPDATE_CLIENTS;
  }

  /**
   * Collects connected gem blocks using an array-based BFS traversal to avoid heap allocations.
   *
   * @param level        block view
   * @param origin       origin block pos
   * @param outPositions output array for discovered positions
   * @param outStates    output array for corresponding block states
   * @return total number of connected gem blocks discovered
   */
  @Unique
  private static int collectConnectedGems(BlockGetter level, BlockPos origin,
                                          BlockPos[] outPositions, BlockState[] outStates) {
    int count = 0;
    outPositions[count] = origin;
    outStates[count] = level.getBlockState(origin);
    count++;

    int head = 0;
    while (head < count) {
      BlockPos current = outPositions[head++];
      for (Direction dir : DIRECTIONS) {
        BlockPos neighbor = current.relative(dir);
        if (isAlreadyVisited(outPositions, count, neighbor)) {
          continue;
        }

        BlockState neighborState = level.getBlockState(neighbor);
        if (isGemBlock(neighborState) && count < outPositions.length) {
          outPositions[count] = neighbor;
          outStates[count] = neighborState;
          count++;
        }
      }
    }
    return count;
  }

  /**
   * Linear search to determine whether a block position is already recorded in the working array.
   *
   * @param positions array of positions
   * @param count     number of valid entries in array
   * @param target    position being searched
   * @return true if position is present
   */
  @Unique
  private static boolean isAlreadyVisited(BlockPos[] positions, int count, BlockPos target) {
    for (int i = 0; i < count; i++) {
      if (positions[i].equals(target)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Updates stunted status across all adjacent cluster blocks.
   *
   * @param level        world access
   * @param gemPositions array of gem block positions
   * @param gemCount     number of gem blocks
   * @param stunted      target stunted boolean state
   */
  @Unique
  private static void updateStuntState(LevelAccessor level, BlockPos[] gemPositions, int gemCount, boolean stunted) {
    for (int i = 0; i < gemCount; i++) {
      BlockPos gemPos = gemPositions[i];
      for (Direction dir : DIRECTIONS) {
        applyStuntToNeighbor(level, gemPos.relative(dir), stunted);
      }
    }
  }

  /**
   * Updates cluster properties on a neighbor block if it matches {@link TypeGemClusterBlock}.
   *
   * @param level       world access
   * @param neighborPos neighbor position
   * @param stunted     stunted state
   */
  @Unique
  private static void applyStuntToNeighbor(LevelAccessor level, BlockPos neighborPos, boolean stunted) {
    BlockState state = level.getBlockState(neighborPos);
    if (!(state.getBlock() instanceof TypeGemClusterBlock)) {
      return;
    }

    BooleanProperty stuntedProp = TypeGemClusterBlock.Companion.getSTUNTED();
    if (state.hasProperty(stuntedProp) && state.getValue(stuntedProp) != stunted) {
      BlockState updated = state.setValue(stuntedProp, stunted);
      BooleanProperty growProp = TypeGemClusterBlock.Companion.getSHOULD_GROW();
      if (updated.hasProperty(growProp)) {
        updated = updated.setValue(growProp, !stunted);
      }
      level.setBlock(neighborPos, updated, getUpdateFlags(level));
    }
  }

  /**
   * Checks whether at least one neighbor position is air, allowing room for growth.
   *
   * @param level        structure world access
   * @param gemPositions array of gem positions
   * @param gemCount     number of gem positions
   * @return true if any neighboring position is air
   */
  @Unique
  private static boolean hasBreathingRoom(WorldGenLevel level, BlockPos[] gemPositions, int gemCount) {
    for (int i = 0; i < gemCount; i++) {
      BlockPos gemPos = gemPositions[i];
      for (Direction dir : DIRECTIONS) {
        if (level.getBlockState(gemPos.relative(dir)).isAir()) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Fast-forwards cluster growth stages up to 5 iterations.
   *
   * @param level      structure world access
   * @param clusterPos position of the cluster block
   * @param random     random generator
   */
  @Unique
  private static void advanceCluster(WorldGenLevel level, BlockPos clusterPos, RandomSource random) {
    for (int i = 0; i < 5; i++) {
      BlockState clusterState = level.getBlockState(clusterPos);
      if (clusterState.getBlock() instanceof TypeGemClusterBlock clusterBlock) {
        clusterBlock.advanceGrowth(clusterState, level, clusterPos, random);
      } else {
        break;
      }
    }
  }

  /**
   * In-place Fisher-Yates array shuffling using Minecraft's Random instance.
   *
   * @param array  array to shuffle
   * @param length number of elements to shuffle
   * @param random random generator
   */
  @Unique
  private static void shuffleIndices(int[] array, int length, RandomSource random) {
    for (int i = length - 1; i > 0; i--) {
      int j = random.nextInt(i + 1);
      int tmp = array[i];
      array[i] = array[j];
      array[j] = tmp;
    }
  }

  /**
   * Checks if the given block state belongs to Cobblemon type gem blocks.
   *
   * @param state block state to check
   * @return true if tagged as a type gem block
   */
  @Unique
  private static boolean isGemBlock(BlockState state) {
    return state.is(CobblemonBlockTags.TYPE_GEM_BLOCKS);
  }
}
