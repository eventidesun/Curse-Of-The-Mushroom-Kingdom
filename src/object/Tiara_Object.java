package object;

import main.GamePanel;
import main.CutsceneManager;

public class Tiara_Object extends SuperObject {

    public Tiara_Object() {
        name      = "Tiara";
        collision = false;

        image = loadImage("/objects/tiara.png");
    }

    @Override
    public void interact(GamePanel gp, int index) {
        gp.player.hasTiara = true;
        gp.object[index]   = null;
        gp.playSE(1);
        gp.cutsceneManager.startScene(CutsceneManager.Scene.FLASHBACK_TIARA);
    }
}
