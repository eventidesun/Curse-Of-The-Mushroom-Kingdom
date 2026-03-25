package object;

import main.GamePanel;

public class Chest_Object extends SuperObject {

    public boolean opened = false;

    public Chest_Object() {
        name      = "Chest";
        collision = true;

        // Placeholder: uses existing chest.png
        image = loadImage("/objects/chest.png");
    }

    @Override
    public void interact(GamePanel gp, int index) {
        if (opened) return;

        opened = true;

        // Swap to open image
        image = loadImage("/objects/chest_opened.png");

        // Restore 4 health (2 hearts)
        gp.player.heal(4);

        // Show message
        gp.ui.showMessage("You found a healing egg! +2 hearts");

        // Play sound — coin.wav as placeholder, swap to chime later
        gp.playSE(1);
    }
}
