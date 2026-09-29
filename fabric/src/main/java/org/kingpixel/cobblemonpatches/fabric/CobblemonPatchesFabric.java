package org.kingpixel.cobblemonpatches.fabric;

import net.fabricmc.api.ModInitializer;
import org.kingpixel.cobblemonpatches.CobblemonPatches;

public class CobblemonPatchesFabric implements ModInitializer {

  @Override
  public void onInitialize() {
    CobblemonPatches.init();
  }
}
