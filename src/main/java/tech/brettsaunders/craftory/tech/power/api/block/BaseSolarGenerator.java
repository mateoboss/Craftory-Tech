package tech.brettsaunders.craftory.tech.power.api.block;


import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class BaseSolarGenerator extends BaseGenerator{

  private static final int BASE_CAPACITY = 50000;
  private static final int BASE_OUTPUT = 20;
  private static final int[] MULTIPLIERS = {1,2,4,8};
  public BaseSolarGenerator(Location location, String blockName, byte level) {
    super(location,blockName,level,BASE_OUTPUT*MULTIPLIERS[level],BASE_CAPACITY*MULTIPLIERS[level]);
  }
  public BaseSolarGenerator() {super();}

  @Override
  protected boolean canStart() {
    return location.getBlock().getRelative(BlockFace.UP).getLightFromSky()==15;
  }

  @Override
  protected boolean canFinish() {
    return !canStart();
  }

  @Override
  protected void processTick() {
    energyProduced = BASE_OUTPUT;
    energyStorage.modifyEnergyStored(energyProduced);
  }

}
