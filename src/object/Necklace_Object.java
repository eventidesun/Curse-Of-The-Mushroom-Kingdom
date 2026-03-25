package object;

import main.GamePanel;

public class Necklace_Object extends SuperObject {

    public Necklace_Object() {
        name      = "Necklace";
        collision = false;

        // Placeholder: use lantern.png — swap to necklace.png when art ready
        image = loadImage("/objects/lantern.png");
    }

    @Override
    public void interact(GamePanel gp, int index) {
        gp.player.hasNecklace = true;
        gp.object[index]      = null;
        gp.playSE(1);

        // Full memory restored — trigger special dialogue
        gp.startDialogue("king", new String[]{
                "This necklace...",
                "I gave this to her on our wedding day.",
                "The Mushroom Queen.",
                "My queen.",
                "I remember everything now.",
                "And I'm going to bring her home."
        });
    }
}
