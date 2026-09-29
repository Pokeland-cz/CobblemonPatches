package org.kingpixel.cobblemonpatches.mixins.scoreboard;

import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for {@link Scoreboard} to guard against crashes when removing score holders.
 * <p>
 * Prevents client and server crashes caused by {@link IllegalStateException} when
 * removing a score holder that is not registered to the target team.
 * </p>
 */
@Mixin(Scoreboard.class)
public abstract class ScoreboardMixin {

  @Shadow
  @Nullable
  public abstract PlayerTeam getPlayersTeam(String scoreHolderName);

  /**
   * Intercepts removal of a score holder from a team, cancelling the operation if the holder is not assigned to that team.
   *
   * @param scoreHolderName The score holder identifier or entity name.
   * @param team            The scoreboard team from which removal is requested.
   * @param ci              The injection callback information.
   */
  @Inject(
      method = "removePlayerFromTeam(Ljava/lang/String;Lnet/minecraft/world/scores/PlayerTeam;)V",
      at = @At("HEAD"),
      cancellable = true
  )
  private void cobblemonPatchesGuardRemoveScoreHolderFromTeam(
      String scoreHolderName,
      PlayerTeam team,
      CallbackInfo ci
  ) {
    if (this.getPlayersTeam(scoreHolderName) != team) {
      ci.cancel();
    }
  }
}

