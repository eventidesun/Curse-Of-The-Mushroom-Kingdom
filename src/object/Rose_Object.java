package object;

import main.GamePanel;
import main.CutsceneManager;

public class Rose_Object extends SuperObject {

    public Rose_Object() {
        name      = "Rose";
        collision = false; // walk over to pick up

        // Placeholder: use key.png — swap to rose.png when art is ready
        image = loadImage("/objects/key.png");
    }

    @Override
    public void interact(GamePanel gp, int index) {
        // Give item to player
        gp.player.hasRose = true;

        // Remove from world
        gp.object[index] = null;

        // Play pickup chime
        gp.playSE(1);

        // Trigger flashback cutscene
        gp.cutsceneManager.startScene(CutsceneManager.Scene.FLASHBACK_ROSE);
    }
}
