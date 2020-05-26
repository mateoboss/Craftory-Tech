package tech.brettsaunders.craftory.tech.power.api.block;

import dev.lone.itemsadder.api.FontImages.FontImageWrapper;
import dev.lone.itemsadder.api.FontImages.TexturedInventoryWrapper;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.bukkit.entity.Player;

public abstract class BlockGUI implements Externalizable {

  /* Static Constants */
  private static final long serialVersionUID = 100000000L;

  /* Per Object Variables */
  private TexturedInventoryWrapper inventoryInterface;
  private String title;
  private String backgroundImage;

  /* Saving, Setup and Loading */
  public BlockGUI() {
    updateGUI();
    title = "";

  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeUTF(title);
    out.writeUTF(backgroundImage);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    title = in.readUTF();
    backgroundImage = in.readUTF();
  }

  /*GUI Methods */
  protected void setGUITitle(String title) {
    this.title = title;
    updateGUI();
  }

  protected void setGUIBackgroundImage(String imageName) {
    this.backgroundImage = imageName;
    updateGUI();
  }

  protected void setGUIDetails(String title, String backgroundImageName) {
    this.backgroundImage = backgroundImageName;
    this.title = title;
    updateGUI();
  }

  private void updateGUI() {
    if (title != null && !backgroundImage.isEmpty()) {
      inventoryInterface = new TexturedInventoryWrapper(null, 54, title, new FontImageWrapper(backgroundImage));
    } else {
      inventoryInterface = new TexturedInventoryWrapper(null, 54, title, new FontImageWrapper("mcguis:blank_menu"));
    }
  }

  public void openGUI(Player player) {
    if (inventoryInterface == null) {
      updateGUI();
    }
    inventoryInterface.showInventory(player);
  }
}