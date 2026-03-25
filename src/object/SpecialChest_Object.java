package object;

import main.GamePanel;

public class SpecialChest_Object extends SuperObject {

    public boolean opened = false;
    public int shieldTierGiven; // 2 = iron, 3 = magic

    public SpecialChest_Object(int tier) {
        name             = "Special Chest";
        collision        = true;
        shieldTierGiven  = tier;

        // Placeholder: reuse chest image with tint — swap to special_chest.png when ready
        image = loadImage("/objects/chest.png");
    }

    @Override
    public void interact(GamePanel gp, int index) {
        if (opened) return;

        opened = true;
        image  = loadImage("/objects/chest_opened.png");

        // Only upgrade if better than current
        if (shieldTierGiven > gp.player.shieldLevel) {
            gp.player.shieldLevel       = shieldTierGiven;
            gp.player.shieldStrength    = 10 * shieldTierGiven;
            gp.player.maxShieldStrength = 10 * shieldTierGiven;

            String tierName = shieldTierGiven == 2 ? "Iron Shield" : "Magic Shield";
            gp.ui.showMessage("You found a " + tierName + "!");
        } else {
            gp.ui.showMessage("Your shield is already stronger.");
        }

        gp.playSE(1);
    }
}
