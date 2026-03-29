package main;

public class DialogueManager {

    GamePanel gp;

    private String[] lines;
    private int currentLine    = 0;
    private String displayText = "";
    private int charIndex      = 0;
    private int typeTimer      = 0;
    private int typeSpeed      = 2;
    private boolean lineComplete  = false;
    private boolean active        = false;
    private String currentSpeaker = "king";

    private static final String[][] WITCH_HINTS = {
            { "The goblins guard something precious...", "Defeat them and see what they carry." },
            { "The trolls roam the misty path ahead.", "What they stole will jog your memory." },
            { "A giant blocks the way to the lair.", "Overcome him and you'll feel something familiar." },
            { "The dragon's fire is fierce...", "But your heart is fiercer. Use what you've found." },
            { "The orb holds a love spell.", "Place the four items together to break it." }
    };

    public DialogueManager(GamePanel gp) {
        this.gp = gp;
    }

    public void startDialogue(String speaker, String[] newLines) {
        this.lines          = newLines;
        this.currentSpeaker = speaker;
        this.currentLine    = 0;
        this.charIndex      = 0;
        this.displayText    = "";
        this.typeTimer      = 0;
        this.lineComplete   = false;
        this.active         = true;
    }

    public void showWitchHint() {
        int hintIndex = gp.player.hasRose  ? 1 : 0;
        hintIndex     = gp.player.hasRing  ? 2 : hintIndex;
        hintIndex     = gp.player.hasTiara ? 3 : hintIndex;
        if (hintIndex >= WITCH_HINTS.length) hintIndex = WITCH_HINTS.length - 1;
        startDialogue("witch", WITCH_HINTS[hintIndex]);
        gp.gameState = GamePanel.GameState.DIALOGUE;
    }

    public void update() {
        if (!active) return;

        if (!lineComplete) {
            typeTimer++;
            if (typeTimer >= typeSpeed) {
                typeTimer = 0;
                if (charIndex < lines[currentLine].length()) {
                    displayText = lines[currentLine].substring(0, charIndex + 1);
                    charIndex++;
//                    gp.playSE(5); // typing tick sound - not really working
                } else {
                    lineComplete = true;
                }
            }
        } else {
            if (gp.keyH.interactPressed) {
                gp.keyH.interactPressed = false;
                advanceLine();
            }
        }
    }

    private void advanceLine() {
        currentLine++;
        if (currentLine >= lines.length) {
            // All lines finished
            active      = false;
            displayText = "";

            if (gp.gameState == GamePanel.GameState.DIALOGUE) {
                gp.endDialogue();
            }
        } else {
            // Move to next line
            charIndex    = 0;
            displayText  = "";
            lineComplete = false;
        }
    }

    public void skipToEnd() {
        if (!lineComplete) {
            displayText  = lines[currentLine];
            charIndex    = lines[currentLine].length();
            lineComplete = true;
        }
    }

    public boolean isActive()         { return active; }
    public boolean isLineComplete()   { return lineComplete; }
    public String  getDisplayText()   { return displayText; }
    public String  getCurrentSpeaker(){ return currentSpeaker; }
}
