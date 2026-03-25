package object;

import main.GamePanel;
import main.CutsceneManager;

public class Ring_Object extends SuperObject {

    public Ring_Object() {
        name      = "Ring";
        collision = false;

        // Placeholder: use coin_bronze.png — swap to ring.png when art ready
        image = loadImage("/objects/coin_bronze.png");
    }

    @Override
    public void interact(GamePanel gp, int index) {
        gp.player.hasRing = true;
        gp.object[index]  = null;
        gp.playSE(1);
        gp.cutsceneManager.startScene(CutsceneManager.Scene.FLASHBACK_RING);
    }
}
