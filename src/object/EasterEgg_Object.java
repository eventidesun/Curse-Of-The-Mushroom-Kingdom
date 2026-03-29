package object;

import main.GamePanel;

public class EasterEgg_Object extends SuperObject {

    public EasterEgg_Object() {
        name      = "Easter Egg";
        collision = false;
        image     = loadImage("/objects/egg_icon.png"); // diamond placeholder — swap when art ready
    }

    @Override
    public void interact(GamePanel gp, int index) {
        gp.player.eggsFound++;
        gp.object[index] = null;

        // Each egg permanently increases attack power
        gp.player.attackPower++;

        gp.playSE(1);
        gp.ui.showMessage("Diamond found! Attack power up! (" + gp.player.eggsFound + "/5)");
    }
}
