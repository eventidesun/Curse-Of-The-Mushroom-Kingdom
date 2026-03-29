package object;

import main.GamePanel;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Chest_Object extends SuperObject {

    public boolean opened   = false;
    private BufferedImage openedImage;
    private BufferedImage potionImage;

    // Potion pickup — tracks potions held in this chest (1 per chest)
    private boolean potionCollected = false;

    public Chest_Object() {
        name      = "Chest";
        collision = false;  // player can walk up to and interact — no collision block

        image       = loadImage("/objects/chest.png");
        openedImage = loadImage("/objects/chest_opened.png");
        potionImage = loadImage("/objects/potion_red.png");
    }

    @Override
    public void interact(GamePanel gp, int index) {
        if (opened) {
            // Already open — player can collect the potion if still there
            if (!potionCollected) {
                potionCollected = true;
                gp.player.potionsHeld = Math.min(gp.player.potionsHeld + 1, 9);
                gp.playSE(1);
                gp.ui.showMessage("Potion collected! Press P to use. (" + gp.player.potionsHeld + " held)");
            }
            return;
        }

        // Open the chest
        opened = true;
        image  = openedImage;
        gp.playSE(1);
        gp.ui.showMessage("Chest opened! Walk over it again to collect the potion.");
    }

    @Override
    public void draw(Graphics2D g2, GamePanel gp) {
        int sx = worldX - gp.tileManager.getCameraX();
        int sy = worldY - gp.tileManager.getCameraY();

        if (sx + gp.tileSize < 0 || sx > gp.screenWidth ||
                sy + gp.tileSize < 0 || sy > gp.screenHeight) return;

        // Draw chest (closed or open)
        if (image != null)
            g2.drawImage(image, sx, sy, gp.tileSize, gp.tileSize, null);

        // Draw potion sitting in the open chest if not yet collected
        if (opened && !potionCollected && potionImage != null) {
            int ps = gp.tileSize / 2;
            g2.drawImage(potionImage, sx + ps / 2, sy - ps / 2, ps, ps, null);
        }
    }
}
