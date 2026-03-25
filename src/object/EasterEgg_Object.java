package object;

import main.GamePanel;

public class EasterEgg_Object extends SuperObject {

    public EasterEgg_Object() {
        name      = "Easter Egg";
        collision = false;

        // Placeholder: use blueheart.png — swap to easter_egg.png when art ready
        image = loadImage("/objects/blueheart.png");
    }

    @Override
    public void interact(GamePanel gp, int index) {
        gp.player.eggsFound++;
        gp.object[index] = null;

        // Boost shield strength
        gp.player.shieldStrength = Math.min(
                gp.player.shieldStrength + 5,
                gp.player.maxShieldStrength
        );

        gp.playSE(1);
        gp.ui.showMessage("Easter egg found! (" + gp.player.eggsFound + "/5) Shield boosted!");
    }
}
