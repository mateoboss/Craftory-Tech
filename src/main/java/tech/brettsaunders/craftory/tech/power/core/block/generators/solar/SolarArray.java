/*******************************************************************************
 * Copyright (c) 2020. BrettSaunders & Craftory Team - All Rights Reserved
 *
 * This file is part of Craftory.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential
 *
 * File Author: Brett Saunders
 ******************************************************************************/

package tech.brettsaunders.craftory.tech.power.core.block.generators.solar;

import org.bukkit.Location;
import tech.brettsaunders.craftory.CoreHolder.Blocks;
import tech.brettsaunders.craftory.tech.power.api.block.BaseSolarGenerator;

public class SolarArray extends BaseSolarGenerator {
  /* Static Constants Private */
  private static final byte C_LEVEL = 3;

  /* Construction */
  public SolarArray(Location location) {
    super(location, Blocks.SOLAR_ARRAY, C_LEVEL);
  }

  /* Saving, Setup and Loading */
  public SolarArray() {
    super();
  }
}