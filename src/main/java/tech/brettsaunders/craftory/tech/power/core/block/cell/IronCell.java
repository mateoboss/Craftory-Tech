package tech.brettsaunders.craftory.tech.power.core.block.cell;

import org.bukkit.Location;
import tech.brettsaunders.craftory.tech.power.api.block.BaseCell;

/**
 * Energy Cell
 *
 * Capacity: 400,000
 * Max Input: 200
 * Max Output: 200
 * Level: 0 (IRON)
 */
public class IronCell extends BaseCell {
  private static final long serialVersionUID = 10015L;
  private static final byte CLEVEL = 0;
  private static final int C_OUTPUT_AMOUNT = 200;

  public IronCell(Location location) {
    super(location, CLEVEL, C_OUTPUT_AMOUNT);
  }

  public IronCell() {
    super();
  }

}
