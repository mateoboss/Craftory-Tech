package tech.brettsaunders.craftory.tech.power.core.manager;

import dev.lone.itemsadder.api.Events.CustomBlockBreakEvent;
import dev.lone.itemsadder.api.Events.CustomBlockInteractEvent;
import dev.lone.itemsadder.api.Events.ItemsAdderFirstLoadEvent;
import dev.lone.itemsadder.api.ItemsAdder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import tech.brettsaunders.craftory.CoreHolder;
import tech.brettsaunders.craftory.CoreHolder.Blocks;
import tech.brettsaunders.craftory.CoreHolder.Items;
import tech.brettsaunders.craftory.Craftory;
import tech.brettsaunders.craftory.Utilities;
import tech.brettsaunders.craftory.tech.power.api.block.BaseCell;
import tech.brettsaunders.craftory.tech.power.api.block.BaseGenerator;
import tech.brettsaunders.craftory.tech.power.api.block.BaseMachine;
import tech.brettsaunders.craftory.tech.power.api.block.BaseProvider;
import tech.brettsaunders.craftory.tech.power.api.block.PoweredBlock;
import tech.brettsaunders.craftory.tech.power.api.interfaces.ITickable;
import tech.brettsaunders.craftory.tech.power.core.block.cell.DiamondCell;
import tech.brettsaunders.craftory.tech.power.core.block.cell.EmeraldCell;
import tech.brettsaunders.craftory.tech.power.core.block.cell.GoldCell;
import tech.brettsaunders.craftory.tech.power.core.block.cell.IronCell;
import tech.brettsaunders.craftory.tech.power.core.block.machine.electricFurnace.DiamondElectricFurnace;
import tech.brettsaunders.craftory.tech.power.core.block.machine.electricFurnace.EmeraldElectricFurnace;
import tech.brettsaunders.craftory.tech.power.core.block.machine.electricFurnace.GoldElectricFurnace;
import tech.brettsaunders.craftory.tech.power.core.block.machine.electricFurnace.IronElectricFurnace;
import tech.brettsaunders.craftory.tech.power.core.block.machine.foundry.IronFoundry;
import tech.brettsaunders.craftory.tech.power.core.block.machine.generators.SolidFuelGenerator;
import tech.brettsaunders.craftory.tech.power.core.utils.PoweredBlockType;
import tech.brettsaunders.craftory.utils.Logger;

public class PoweredBlockManager implements Listener, ITickable {

  public static final BlockFace[] faces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH,
      BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
  private static final String DATA_PATH;

  static {
    DATA_PATH = Utilities.DATA_FOLDER + File.separator + "poweredBlock.data";
  }

  private final HashMap<Location, PowerGridManager> powerConnectors;
  public HashSet<PowerGridManager> powerGridManagers;
  private HashMap<Location, PoweredBlock> poweredBlocks;
  private HashMap<World, HashSet> loadedChunkWorlds;
  private HashMap<UUID, ArrayList<Boolean>> sidesConfigCopying;

  public PoweredBlockManager() {
    poweredBlocks = new HashMap<>();
    powerGridManagers = new HashSet<>();
    powerConnectors = new HashMap<>();
    sidesConfigCopying = new HashMap<>();
    loadedChunkWorlds = new HashMap<>();
    Craftory.getInstance().getServer().getPluginManager()
        .registerEvents(this, Craftory.getInstance());
    Craftory.tickableBaseManager.addUpdate(this);
  }

  public void onEnable() {
    load();
  }

  public void onDisable() {
    save();
  }

  public void addPoweredBlock(Location location, PoweredBlock blockPowered) {
    poweredBlocks.put(location, blockPowered);
  }

  public PoweredBlock getPoweredBlock(Location location) {
    return poweredBlocks.get(location);
  }

  public boolean isPoweredBlock(Location location) {
    return poweredBlocks.containsKey(location);
  }

  public void removePoweredBlock(Location location) {
    poweredBlocks.remove(location);
  }

  public void load() {
    try {
      BukkitObjectInputStream in = new BukkitObjectInputStream(
          new GZIPInputStream(new FileInputStream(DATA_PATH)));
      PowerBlockManagerData data = (PowerBlockManagerData) in.readObject();
      poweredBlocks = data.poweredBlocks;
      powerGridManagers = data.powerGridManagers;
      in.close();
      Logger.info("Powered Block Data Loaded");
    } catch (FileNotFoundException e) {
      Logger.debug("First Run - Generating Powered Block Data");
    } catch (IOException e) {
      Logger.error("Powered Block Data IO Loading Issue");
      Logger.captureError(e);
    } catch (ClassNotFoundException e) {
      Logger.captureError(e);
    }
  }

  public void save() {
    try {
      PowerBlockManagerData data = new PowerBlockManagerData(poweredBlocks, powerGridManagers);
      BukkitObjectOutputStream out = new BukkitObjectOutputStream(
          new GZIPOutputStream(new FileOutputStream(DATA_PATH)));
      out.writeObject(data);
      out.close();
      Logger.debug("Powered Block Data Saved");
    } catch (IOException e) {
      Logger.warn("Couldn't save Powered Block Data");
      Logger.captureError(e);
    }
  }

  @EventHandler
  public void onWorldSave(WorldSaveEvent event) {
    save();
  }

  @EventHandler
  public void onItemsAdderLoaded(ItemsAdderFirstLoadEvent e) {
    poweredBlocks.forEach(((location, poweredBlock) -> poweredBlock.setupGUI()));
  }

  @EventHandler
  public void onGUIBlockClick(CustomBlockInteractEvent e) {
    if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }
    if (e.getPlayer().isSneaking() || ItemsAdder.matchCustomItemName(e.getItem(), CoreHolder.Items.CONFIGURATOR)) {
      return;
    }

    if (poweredBlocks.containsKey(e.getBlockClicked().getLocation())) {
      //Open GUI of Powered Block
      poweredBlocks.get(e.getBlockClicked().getLocation()).openGUI(e.getPlayer());
      e.setCancelled(true);
    }
  }

  public boolean isProvider(Location location) {
    if (isPoweredBlock(location)) {
      return poweredBlocks.get(location).isProvider();
    }
    return false;

  }

  public boolean isReceiver(Location location) {
    if (isPoweredBlock(location)) {
      return poweredBlocks.get(location).isReceiver();
    }
    return false;
  }

  /* Block Type Getters */
  public boolean isCell(Location location) {
    if (isPoweredBlock(location)) {
      return poweredBlocks.get(location).isProvider() && poweredBlocks.get(location).isReceiver();
    }
    return false;
  }

  public boolean isGenerator(Location location) {
    if (isPoweredBlock(location)) {
      return poweredBlocks.get(location).isProvider() && !poweredBlocks.get(location).isReceiver();
    }
    return false;
  }

  public boolean isMachine(Location location) {
    if (isPoweredBlock(location)) {
      return !poweredBlocks.get(location).isProvider() && !poweredBlocks.get(location).isReceiver();
    }
    return false;
  }

  /* Events */
  @EventHandler
  public void onPoweredBlockPlace(BlockPlaceEvent event) {
    Location location = event.getBlockPlaced().getLocation();
    Craftory.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(
        Craftory.getInstance(), () -> {
          PoweredBlock poweredBlock = null;
          PoweredBlockType type = PoweredBlockType.MACHINE;
          if (!ItemsAdder.isCustomBlock(event.getBlockPlaced())) {
            return;
          }

          ItemStack blockPlacedItemStack = ItemsAdder.getCustomBlock(event.getBlockPlaced());
          String blockPlacedName = ItemsAdder.getCustomItemName(blockPlacedItemStack);

          switch (blockPlacedName) {
            case CoreHolder.Blocks.IRON_CELL:
              poweredBlock = new IronCell(location);
              type = PoweredBlockType.CELL;
              break;
            case CoreHolder.Blocks.GOLD_CELL:
              poweredBlock = new GoldCell(location);
              type = PoweredBlockType.CELL;
              break;
            case CoreHolder.Blocks.DIAMOND_CELL:
              poweredBlock = new DiamondCell(location);
              type = PoweredBlockType.CELL;
              break;
            case CoreHolder.Blocks.EMERALD_CELL:
              poweredBlock = new EmeraldCell(location);
              type = PoweredBlockType.CELL;
              break;

            case CoreHolder.Blocks.SOLID_FUEL_GENERATOR:
              poweredBlock = new SolidFuelGenerator(location);
              type = PoweredBlockType.GENERATOR;
              break;
            case CoreHolder.Blocks.POWER_CONNECTOR:
              PowerGridManager manager = new PowerGridManager(location);
              getAdjacentPowerBlocks(location, manager);
              addPowerGridManager(location, manager);
              type = PoweredBlockType.CELL;
              break;
            case CoreHolder.Blocks.IRON_ELECTRIC_FURNACE:
              poweredBlock = new IronElectricFurnace(location);
              type = PoweredBlockType.MACHINE;
              break;
            case CoreHolder.Blocks.GOLD_ELECTRIC_FURNACE:
              poweredBlock = new GoldElectricFurnace(location);
              type = PoweredBlockType.MACHINE;
              break;
            case CoreHolder.Blocks.EMERALD_ELECTRIC_FURNACE:
              poweredBlock = new EmeraldElectricFurnace(location);
              type = PoweredBlockType.MACHINE;
              break;
            case CoreHolder.Blocks.DIAMOND_ELECTRIC_FURNACE:
              poweredBlock = new DiamondElectricFurnace(location);
              type = PoweredBlockType.MACHINE;
              break;
            case CoreHolder.Blocks.IRON_FOUNDRY:
              poweredBlock = new IronFoundry(location);
              type = PoweredBlockType.MACHINE;
              break;
            default:
              return;
          }

          //Carry out PoweredBlock Base Setup
          if (poweredBlock != null) {
            addPoweredBlock(location, poweredBlock);
            if (poweredBlock.isReceiver()) {
              updateAdjacentProviders(location, true, type);
            }
          }
        }, 1L);
  }

  @EventHandler
  public void onPoweredBlockBreak(CustomBlockBreakEvent event) {
    Location location = event.getBlock().getLocation();
    if (!poweredBlocks.containsKey(location)) {
      return;
    }
    // Drop items
    PoweredBlock b = poweredBlocks.get(location);
    World world = location.getWorld();
    Inventory inventory = b.getInventory();
    ItemStack item;
    for (Integer i : b.getInteractableSlots()) {
      item = inventory.getItem(i);
      if (item != null) {
        world.dropItemNaturally(location, item);
      }
    }

    if (isReceiver(location)) {
      updateAdjacentProviders(location, false, PoweredBlockType.MACHINE);
    }
    Craftory.tickableBaseManager.removeUpdate(getPoweredBlock(location));
    removePoweredBlock(location);
  }

  @EventHandler
  public void onWrenchLeftClick(PlayerInteractEvent e) {
    if (e.getAction() != Action.LEFT_CLICK_BLOCK) {
      return;
    }
    if (!ItemsAdder.matchCustomItemName(e.getItem(), CoreHolder.Items.WRENCH)) {
      return;
    }
    e.setCancelled(true);

    //Show power levels
    if (isPoweredBlock(e.getClickedBlock().getLocation())) {
      PoweredBlock block = getPoweredBlock(e.getClickedBlock().getLocation());
      e.getPlayer().sendMessage(
          "Stored: " + block.getInfoEnergyStored() + " RE / " + block.getInfoEnergyCapacity()
              + " RE");
    }
  }

  @EventHandler
  public void onConfigurator(final PlayerInteractEvent e) {
    if (!ItemsAdder.matchCustomItemName(e.getItem(), CoreHolder.Items.CONFIGURATOR)) {
      return;
    }
    e.setCancelled(true);

    final Player player = e.getPlayer();
    if (e.getAction() == Action.RIGHT_CLICK_AIR && player.isSneaking()) {
      sidesConfigCopying.remove(player.getUniqueId());
      player.sendMessage("Removed Sides Config Copy Data");
    }

    if (isProvider(e.getClickedBlock().getLocation())) {
      BaseProvider provider = (BaseProvider) getPoweredBlock(e.getClickedBlock().getLocation());
      if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
        sidesConfigCopying.put(player.getUniqueId(), provider.getSideConfig());
        player.sendMessage("Copied Sides Config");
      } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
        if (sidesConfigCopying.containsKey(player.getUniqueId())) {
          provider.setSidesConfig(sidesConfigCopying.get(player.getUniqueId()));
          player.sendMessage("Pasted Sides Config");
        } else {
          player.sendMessage("No Sides Config Data Found, Please Copy First");
        }
      }
    }
  }

  //TODO CLEAN UP
  private void updateAdjacentProviders(Location location, Boolean setTo, PoweredBlockType type) {
    Block block;
    Location connectorLocation = location;
    for (BlockFace face : faces) {
      block = location.getBlock().getRelative(face);
      if (ItemsAdder.isCustomBlock(block)) {
        if (poweredBlocks.containsKey(block.getLocation()) && isProvider(block.getLocation())) {
          ((BaseProvider) getPoweredBlock(block.getLocation()))
              .updateOutputCache(face.getOppositeFace(), setTo);
        } else if (setTo && ItemsAdder.getCustomItemName(ItemsAdder.getCustomBlock(block))
            == CoreHolder.Blocks.POWER_CONNECTOR) { //TODO fix type part - seperate
          switch (type) {
            case MACHINE:
              powerConnectors.get(location).addMachine(connectorLocation, location);
              break;
            case GENERATOR:
              powerConnectors.get(location).addGenerator(connectorLocation, location);
              break;
            case CELL:
              powerConnectors.get(location).addPowerCell(connectorLocation, location);
              break;
          }

        }
      }
    }
  }

  private void getAdjacentPowerBlocks(Location location, PowerGridManager powerGridManager) {
    Block block;
    Location connectorLocation = location;
    for (BlockFace face : faces) {
      block = location.getBlock().getRelative(face);
      if (ItemsAdder.isCustomBlock(block) && poweredBlocks.containsKey(block.getLocation())) {
        if (isCell(location)) {
          powerGridManager.addPowerCell(connectorLocation, location);
        } else if (isGenerator(location)) {
          powerGridManager.addGenerator(connectorLocation, location);
        } else if (isMachine(location)) {
          powerGridManager.addMachine(connectorLocation, location);
        }
      }
    }
  }

  public void print(Player player) {
    player.sendMessage(poweredBlocks.toString());
  }

  private void addPowerGridManager(Location location, PowerGridManager manger) {
    powerGridManagers.add(manger);
    powerConnectors.put(location, manger);
    //TODO for every merge or place of a power connector
    //TODO when merge change this
  }

  public PowerGridManager getPowerGridManager(Location location) {
    return powerConnectors.get(location);
  }

  @Override
  public void update(long worldTime) {
    if (worldTime % CoreHolder.FOUR_TICKS == 0) {
      //Generate HashMap of loaded chunks in worlds
      HashSet<Chunk> loadedChunks;
      for (World world : Bukkit.getWorlds()) {
        loadedChunks = new HashSet<>(Arrays.asList(world.getLoadedChunks()));
        loadedChunkWorlds.put(world, loadedChunks);
      }
    }

    //If in loaded chunk, call update
    try {
      poweredBlocks.forEach(((location, poweredBlock) -> {
        if (loadedChunkWorlds != null && loadedChunkWorlds.get(location.getWorld())
            .contains(location.getChunk())) {
          poweredBlock.update(worldTime);
        }
      }));
    } catch (NullPointerException e) {
      Logger.debug(e.toString());
    }
  }

  private static class PowerBlockManagerData implements Serializable {

    private static final long serialVersionUID = 9999L;
    protected final HashMap<Location, PoweredBlock> poweredBlocks;
    protected final HashSet<PowerGridManager> powerGridManagers;

    public PowerBlockManagerData(HashMap<Location, PoweredBlock> poweredBlocks,
        HashSet<PowerGridManager> powerGridManagers) {
      this.poweredBlocks = poweredBlocks;
      this.powerGridManagers = powerGridManagers;
    }
  }

}
