package object;

import main.GamePanel;
import main.CutsceneManager;

public class Tiara_Object extends SuperObject {

    public Tiara_Object() {
        name      = "Tiara";
        collision = false;

        // Placeholder: use boots.png — swap to tiara.png when art ready
        image = loadImage("/objects/boots.png");
    }

    @Override
    public void interact(GamePanel gp, int index) {
        gp.player.hasTiara = true;
        gp.object[index]   = null;
        gp.playSE(1);
        gp.cutsceneManager.startScene(CutsceneManager.Scene.FLASHBACK_TIARA);
    }
}
