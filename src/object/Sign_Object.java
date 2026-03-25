package object;

import main.GamePanel;

public class Sign_Object extends SuperObject {

    private String[] lines;

    public Sign_Object(String... lines) {
        name         = "Sign";
        collision    = true;
        this.lines   = lines;

        // Placeholder: use door.png — swap to sign.png when art ready
        image = loadImage("/objects/door.png");
    }

    @Override
    public void interact(GamePanel gp, int index) {
        gp.startDialogue("king", lines);
    }
}
