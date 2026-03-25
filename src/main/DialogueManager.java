package main;

public class DialogueManager {

    GamePanel gp;

    // Current dialogue state
    private String[] lines;          // all lines in current conversation
    private int currentLine = 0;     // which line we're on
    private String displayText = ""; // what's actually shown so far (typewriter)
    private int charIndex = 0;       // how many chars revealed
    private int typeTimer = 0;       // counts up to typeSpeed
    private int typeSpeed = 2;       // lower = faster typing (frames per character)
    private boolean lineComplete = false;
    private boolean active = false;
    private String currentSpeaker = "king"; // "king" or "witch"

    // Witch hint lines — shown when player clicks hint button
    private static final String[][] WITCH_HINTS = {
            { "The goblins guard something precious...", "Defeat them and see what they carry." },
            { "The trolls roam the misty path ahead.", "What they stole will jog your memory." },
            { "A giant blocks the way to the lair.", "Overcome him and you'll feel something familiar." },
            { "The dragon's fire is fierce...", "But your heart is fiercer. Use what you've found." },
            { "The orb holds a love spell.", "Place the three items together to break it." }
    };

    public DialogueManager(GamePanel gp) {
        this.gp = gp;
    }

    // -----------------------------------------------
    // START a new dialogue — pass speaker and lines
    // -----------------------------------------------
    public void startDialogue(String speaker, String[] newLines) {
        this.lines        = newLines;
        this.currentSpeaker = speaker;
        this.currentLine  = 0;
        this.charIndex    = 0;
        this.displayText  = "";
        this.typeTimer    = 0;
        this.lineComplete = false;
        this.active       = true;
    }

    // Show witch hint based on how many items collected
    public void showWitchHint() {
        int hintIndex = gp.player.hasRose  ? 1 : 0;
        hintIndex     = gp.player.hasRing  ? 2 : hintIndex;
        hintIndex     = gp.player.hasTiara ? 3 : hintIndex;
        // hint 4 = puzzle hint, shown once in lair
        if (hintIndex >= WITCH_HINTS.length) hintIndex = WITCH_HINTS.length - 1;

        startDialogue("witch", WITCH_HINTS[hintIndex]);
        gp.gameState = GamePanel.GameState.DIALOGUE;
    }

    // -----------------------------------------------
    // UPDATE — called every frame when in DIALOGUE state
    // -----------------------------------------------
    public void update() {
        if (!active) return;

        if (!lineComplete) {
            // Typewriter: reveal one character every typeSpeed frames
            typeTimer++;
            if (typeTimer >= typeSpeed) {
                typeTimer = 0;
                if (charIndex < lines[currentLine].length()) {
                    displayText = lines[currentLine].substring(0, charIndex + 1);
                    charIndex++;
                    gp.playSE(5); // index 5 = your typing tick sound
                } else {
                    lineComplete = true;
                }
            }
        } else {
            // Wait for player to press ENTER/SPACE to advance
            if (gp.keyH.interactPressed) {
                gp.keyH.interactPressed = false; // consume the keypress
                advanceLine();
            }
        }
    }

    private void advanceLine() {
        currentLine++;
        if (currentLine >= lines.length) {
            // Dialogue finished
            active = false;
            displayText = "";
            gp.endDialogue();
        } else {
            // Next line
            charIndex    = 0;
            displayText  = "";
            lineComplete = false;
        }
    }

    // If player presses interact while typing, skip to end of line
    public void skipToEnd() {
        if (!lineComplete) {
            displayText  = lines[currentLine];
            charIndex    = lines[currentLine].length();
            lineComplete = true;
        }
    }

    // -----------------------------------------------
    // GETTERS for UI.java to read
    // -----------------------------------------------
    public boolean isActive()      { return active; }
    public boolean isLineComplete(){ return lineComplete; }
    public String  getDisplayText(){ return displayText; }
    public String  getCurrentSpeaker(){ return currentSpeaker; }
}
