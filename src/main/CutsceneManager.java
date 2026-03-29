package main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

public class CutsceneManager {

    GamePanel gp;

    public enum Scene {
        INTRO_CASTLE, DREAM_WITCH,
        FLASHBACK_ROSE, FLASHBACK_RING, FLASHBACK_TIARA,
        ENDING
    }

    private Scene   currentScene;
    private int     scenePhase  = 0;
    private int     phaseTimer  = 0;
    private boolean sceneActive = false;

    private BufferedImage kingSprite, queenSprite, witchSprite;
    private BufferedImage dragon1, dragon2, dragon3;
    private BufferedImage tileWall, tileFloor, greenSmoke;

    // Dragon
    private int     dragonX       = 0;
    private int     dragonY       = 0;
    private boolean dragonVisible = false;
    private boolean queenVisible  = true;
    private boolean dragonCarrying = false;
    private int     shakeTimer    = 0;
    private int     dragonFrame   = 0;
    private int     dragonAnimTimer = 0;

    // Poof
    private boolean poofActive = false;
    private int     poofTimer  = 0;
    private int     poofX, poofY;
    private static final int POOF_DURATION = 60;

    // Screen darken — subtle ambient effect as dragon approaches
    private float   darkenAlpha = 0f;

    // Dissolve — black fade between scenes
    private enum DissolveState { NONE, FADING_OUT, FADING_IN }
    private DissolveState dissolveState  = DissolveState.NONE;
    private float         dissolveAlpha  = 0f;
    private static final float DISSOLVE_SPEED = 0.07f;
    private Runnable      dissolveOnBlack = null;

    // Dragon curse — after kidnap, dragon returns centre stage
    // and speaks the curse lines directly
    private boolean dragonReturned     = false;
    private int     dragonCentreTimer  = 0;
    private int     spellBeamX         = 0;
    private boolean spellActive        = false;
    private int     kingCurseTimer     = 0;

    // Phase 3 guard
    private boolean kingConfusedStarted = false;

    // Dream guards
    private boolean narratorDone      = false;
    private boolean witchDialogueDone = false;

    private static final String[] DRAGON_CURSE_LINES = {
            "Foolish king.",
            "You never stood a chance.",
            "And now... you will forget her.",
            "She never existed."
    };

    private static final String[] ACT1_PEACEFUL = {
            "Welcome to the Mushroom Kingdom.",
            "A peaceful land ruled by a kind king and his beloved queen.",
            "Today, like every day, the kingdom is full of joy.",
            "The king and queen stand together in their chamber.",
            "All is well... for now."
    };

    private static final String[] ACT1_CONFUSED = {
            "...",
            "What a wonderful morning!",
            "Hmm. I should get going.",
            "Time to serve the kingdom!"
    };

    public CutsceneManager(GamePanel gp) {
        this.gp = gp;
        loadImages();
    }

    private void loadImages() {
        kingSprite  = loadImg("/characters/king.png");
        queenSprite = loadImg("/characters/queen.png");
        witchSprite = loadImg("/characters/witch.png");
        dragon1     = loadImg("/enemies/dragon1.png");
        dragon2     = loadImg("/enemies/dragon2.png");
        dragon3     = loadImg("/enemies/dragon3.png");
        tileWall    = loadImg("/tiles/wall.png");
        tileFloor   = loadImg("/tiles/stone_path.png");
        greenSmoke  = loadImg("/cutscenes/green_smoke.png");
    }

    private BufferedImage loadImg(String path) {
        try { return ImageIO.read(getClass().getResourceAsStream(path)); }
        catch (Exception e) { System.out.println("Cutscene img not found: " + path); return null; }
    }

    private void dissolveToNext(Runnable onBlack) {
        if (dissolveState != DissolveState.NONE) return;
        dissolveState   = DissolveState.FADING_OUT;
        dissolveAlpha   = 0f;
        dissolveOnBlack = onBlack;
    }

    private void updateDissolve() {
        if (dissolveState == DissolveState.NONE) return;
        if (dissolveState == DissolveState.FADING_OUT) {
            dissolveAlpha += DISSOLVE_SPEED;
            if (dissolveAlpha >= 1f) {
                dissolveAlpha = 1f;
                dissolveState = DissolveState.FADING_IN;
                if (dissolveOnBlack != null) { dissolveOnBlack.run(); dissolveOnBlack = null; }
            }
        } else {
            dissolveAlpha -= DISSOLVE_SPEED;
            if (dissolveAlpha <= 0f) { dissolveAlpha = 0f; dissolveState = DissolveState.NONE; }
        }
    }

    public void startScene(Scene scene) {
        currentScene        = scene;
        scenePhase          = 0;
        phaseTimer          = 0;
        sceneActive         = true;
        dragonVisible       = false;
        dragonCarrying      = false;
        queenVisible        = true;
        poofActive          = false;
        poofTimer           = 0;
        darkenAlpha         = 0f;
        shakeTimer          = 0;
        dragonFrame         = 0;
        dragonAnimTimer     = 0;
        dragonReturned      = false;
        dragonCentreTimer   = 0;
        spellActive         = false;
        kingCurseTimer      = 0;
        kingConfusedStarted = false;
        narratorDone        = false;
        witchDialogueDone   = false;
        dissolveState       = DissolveState.NONE;
        dissolveAlpha       = 0f;
        dissolveOnBlack     = null;
        dragonX             = gp.screenWidth + 100;
        dragonY             = gp.screenHeight / 2 - gp.tileSize;

        gp.gameState = (scene == Scene.DREAM_WITCH)
                ? GamePanel.GameState.DREAM
                : GamePanel.GameState.CUTSCENE;

        switch (scene) {
            case INTRO_CASTLE -> gp.dialogueManager.startDialogue("king", ACT1_PEACEFUL);
            case DREAM_WITCH  -> gp.dialogueManager.startDialogue("narrator", new String[]{
                    "Later that night, the king fell into a deep sleep.",
                    "And in that sleep... something stirred.",
                    "A dream.",
                    "Or perhaps a warning."
            });
            case FLASHBACK_ROSE  -> gp.dialogueManager.startDialogue("king", new String[]{
                    "This rose...", "Someone used to love these.", "Why does my chest ache?"
            });
            case FLASHBACK_RING  -> gp.dialogueManager.startDialogue("king", new String[]{
                    "A ring...", "I remember... hands. Small, gentle hands.", "Who did this belong to?"
            });
            case FLASHBACK_TIARA -> gp.dialogueManager.startDialogue("king", new String[]{
                    "A tiara...", "A crown fit for a queen.", "My queen.",
                    "I remember now. I remember everything."
            });
            case ENDING -> gp.dialogueManager.startDialogue("king", new String[]{
                    "The orb shatters.", "She falls... and I catch her.", "Her eyes open.",
                    "You came for me.", "I never forgot you.", "Not really.",
                    "Not where it mattered."
            });
        }
    }

    public void update() {
        if (!sceneActive) return;
        phaseTimer++;
        if (shakeTimer > 0) shakeTimer--;
        updateDissolve();

        // Dragon wing animation — always ticking when dragon is visible
        if (dragonVisible) {
            dragonAnimTimer++;
            if (dragonAnimTimer > 10) { dragonFrame = (dragonFrame + 1) % 3; dragonAnimTimer = 0; }
        }

        if (poofActive) {
            poofTimer++;
            if (poofTimer >= POOF_DURATION) {
                poofActive = false; queenVisible = false; poofTimer = 0; dragonCarrying = true;
            }
        }

        switch (currentScene) {
            case INTRO_CASTLE                    -> updateIntro();
            case DREAM_WITCH                     -> updateDream();
            case FLASHBACK_ROSE, FLASHBACK_RING,
                 FLASHBACK_TIARA                 -> updateFlashback();
            case ENDING                          -> updateEnding();
        }
    }

    private void updateIntro() {

        // Phase 0 — wait for peaceful dialogue
        if (scenePhase == 0 && !gp.dialogueManager.isActive()
                && dissolveState == DissolveState.NONE) {
            scenePhase = 1;
            gp.playSE(6);
        }

        if (scenePhase == 1) {
            dragonVisible = true;

            // Step A: dragon flies in from right, grabs queen
            if (!dragonCarrying && !poofActive && !dragonReturned) {
                dragonX -= 10;
                if (darkenAlpha < 0.3f) darkenAlpha += 0.006f;
                int queenX = gp.screenWidth / 2 + 40;
                if (dragonX <= queenX + 60 && queenVisible) {
                    shakeTimer = 40;
                    poofActive = true; poofTimer = 0;
                    poofX = queenX; poofY = gp.screenHeight / 2 - gp.tileSize / 2;
                    gp.playSE(7); gp.stopMusic();
                }
            }

            // Step B: dragon carries queen off screen right
            if (dragonCarrying && !dragonReturned) {
                dragonX += 12;
                // When dragon is fully off screen, dissolve then return centre
                if (dragonX > gp.screenWidth + 200 && dissolveState == DissolveState.NONE) {
                    dissolveToNext(() -> {
                        // Dragon returns to centre stage — face the king
                        dragonReturned    = true;
                        dragonCarrying    = false;
                        queenVisible      = false;
                        darkenAlpha       = 0f;
                        dragonCentreTimer = 0;
                        dragonX = gp.screenWidth / 2 + 40;
                        dragonY = gp.screenHeight / 2 - gp.tileSize * 2;
                    });
                }
            }

            // Step C: dragon back centre, speaks curse lines
            if (dragonReturned && !spellActive && kingCurseTimer == 0) {
                dragonCentreTimer++;
                // Wait a beat then start dragon's dialogue
                if (dragonCentreTimer == 60 && !gp.dialogueManager.isActive()) {
                    gp.dialogueManager.startDialogue("dragon", DRAGON_CURSE_LINES);
                }
                // Once curse dialogue done, fire the spell
                if (dragonCentreTimer > 60 && !gp.dialogueManager.isActive()) {
                    spellActive = true;
                    spellBeamX  = dragonX;
                }
            }

            // Step D: spell travels to king
            if (spellActive) {
                spellBeamX -= 15;
                int kingX = gp.screenWidth / 2 - 120;
                if (spellBeamX <= kingX + gp.tileSize) {
                    spellActive    = false;
                    kingCurseTimer = 60;
                    shakeTimer     = 20;
                    gp.playSE(7);
                }
            }

            // Step E: after curse hits, dragon exits and dissolves to phase 2
            if (kingCurseTimer > 0) {
                kingCurseTimer--;
                dragonX += 8; // dragon drifts right while king is cursed
                if (kingCurseTimer == 0 && dissolveState == DissolveState.NONE) {
                    dissolveToNext(() -> {
                        dragonVisible = false;
                        scenePhase    = 2;
                    });
                }
            }
        }

        // Phase 2: king alone, confused
        if (scenePhase == 2 && dissolveState == DissolveState.NONE) {
            if (!kingConfusedStarted) {
                kingConfusedStarted = true;
                gp.dialogueManager.startDialogue("king", ACT1_CONFUSED);
            }
            if (kingConfusedStarted && !gp.dialogueManager.isActive()) {
                dissolveToNext(() -> {
                    sceneActive = false;
                    startScene(Scene.DREAM_WITCH);
                });
            }
        }
    }

    private void updateDream() {
        if (scenePhase == 0 && !narratorDone && !gp.dialogueManager.isActive()
                && dissolveState == DissolveState.NONE) {
            narratorDone = true;
            scenePhase   = 1;
            gp.dialogueManager.startDialogue("witch", new String[]{
                    "King... can you hear me?",
                    "The curse has stolen your memory.",
                    "You had someone precious to you.",
                    "Go into the forest and fight the evil.",
                    "The answers lie with those who took from her."
            });
        }
        if (scenePhase == 1 && narratorDone && !witchDialogueDone
                && !gp.dialogueManager.isActive()
                && dissolveState == DissolveState.NONE) {
            witchDialogueDone = true;
            dissolveToNext(() -> {
                sceneActive  = false;
                gp.gameState = GamePanel.GameState.OVERWORLD;
                gp.playMusic(2);
            });
        }
    }

    private void updateFlashback() {
        if (!gp.dialogueManager.isActive() && dissolveState == DissolveState.NONE) {
            dissolveToNext(() -> { sceneActive = false; gp.gameState = GamePanel.GameState.OVERWORLD; });
        }
    }

    private void updateEnding() {
        if (!gp.dialogueManager.isActive() && dissolveState == DissolveState.NONE) {
            dissolveToNext(() -> { sceneActive = false; gp.gameState = GamePanel.GameState.ENDING; });
        }
    }

    public void draw(Graphics2D g2) {
        if (!sceneActive) return;

        if (shakeTimer > 0) {
            int i = Math.min(shakeTimer, 10);
            g2.translate((int)((Math.random()-0.5)*i*2), (int)((Math.random()-0.5)*i*2));
        }

        switch (currentScene) {
            case INTRO_CASTLE    -> drawBedroom(g2);
            case DREAM_WITCH     -> drawDream(g2);
            case FLASHBACK_ROSE  -> drawFlashback(g2, "rose");
            case FLASHBACK_RING  -> drawFlashback(g2, "ring");
            case FLASHBACK_TIARA -> drawFlashback(g2, "tiara");
            case ENDING          -> drawEnding(g2);
        }

        // Ambient darken
        if (darkenAlpha > 0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, darkenAlpha)));
            g2.setColor(Color.black);
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        // Letterbox
        g2.setColor(Color.black);
        g2.fillRect(0, 0, gp.screenWidth, 70);

        // Dialogue on top
        gp.ui.drawDialogueBox(g2);

        // Dissolve overlay — always the very top layer
        if (dissolveState != DissolveState.NONE && dissolveAlpha > 0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, dissolveAlpha)));
            g2.setColor(Color.black);
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }
    }

    private void drawBedroom(Graphics2D g2) {
        int ts = gp.tileSize;

        // Floor
        for (int col=0; col<=gp.screenWidth/ts; col++)
            for (int row=0; row<=gp.screenHeight/ts; row++) {
                if (tileFloor!=null) g2.drawImage(tileFloor,col*ts,row*ts,ts,ts,null);
                else { g2.setColor(new Color(160,130,90)); g2.fillRect(col*ts,row*ts,ts,ts); }
            }
        // Wall top rows
        for (int col=0; col<=gp.screenWidth/ts; col++)
            for (int row=0; row<3; row++) {
                if (tileWall!=null) g2.drawImage(tileWall,col*ts,row*ts,ts,ts,null);
                else { g2.setColor(new Color(100,100,120)); g2.fillRect(col*ts,row*ts,ts,ts); }
            }

        int kingX=gp.screenWidth/2-120, kingY=gp.screenHeight/2-ts;
        if (kingSprite!=null) g2.drawImage(kingSprite,kingX,kingY,ts*2,ts*2,null);
        else { g2.setColor(new Color(80,140,80)); g2.fillRect(kingX,kingY,ts,ts); }

        // Queen
        if (queenVisible) {
            int qx=gp.screenWidth/2+40, qy=gp.screenHeight/2-ts;
            if (queenSprite!=null) g2.drawImage(queenSprite,qx,qy,ts*2,ts*2,null);
            else { g2.setColor(new Color(200,100,160)); g2.fillRect(qx,qy,ts,ts); }
        }

        // Dragon
        if (dragonVisible) {
            int dw = ts * 4, dh = ts * 4;
            int bob = (int)(Math.sin(phaseTimer * 0.2) * 6);
            int drawY = dragonY + bob;
            BufferedImage frame = switch (dragonFrame) {
                case 0 -> dragon1; case 1 -> dragon2; default -> dragon3;
            };

            if (dragonCarrying && queenSprite != null)
                g2.drawImage(queenSprite, dragonX, drawY - ts, ts*2, ts*2, null);

            if (frame != null)
                g2.drawImage(frame, dragonX + dw, drawY, -dw, dh, null);
            else {
                g2.setColor(new Color(160,30,30));
                g2.fillRect(dragonX, drawY, dw, dh);
            }
        }

        // Poof
        if (poofActive) drawPoof(g2, poofX+ts/2, poofY+ts/2, poofTimer, POOF_DURATION);
    }

    private void drawDream(Graphics2D g2) {
        g2.setColor(new Color(10, 5, 30));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        g2.setColor(new Color(255, 255, 255, 100));
        long seed = 42;
        for (int i = 0; i < 40; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int sx = (int)(Math.abs(seed % gp.screenWidth));
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int sy = (int)(Math.abs(seed % (gp.screenHeight / 2)));
            g2.fillOval(sx, sy, (i % 3 == 0) ? 3 : 2, (i % 3 == 0) ? 3 : 2);
        }

        int kx = gp.screenWidth/2 - gp.tileSize, ky = gp.screenHeight - 280;
        if (kingSprite!=null) g2.drawImage(kingSprite, kx, ky, gp.tileSize*2, gp.tileSize*2, null);
        else { g2.setColor(new Color(80,140,80)); g2.fillRect(kx, ky, gp.tileSize, gp.tileSize); }

        float witchAlpha = (scenePhase == 0) ? 0.6f : 1.0f;
        float bob = (float)(Math.sin(phaseTimer * 0.05) * 8);
        int wx = gp.screenWidth/2 - gp.tileSize;
        int wy = (int)(gp.screenHeight/2 - 120 + bob);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, witchAlpha));
        if (witchSprite!=null) g2.drawImage(witchSprite, wx, wy, gp.tileSize*2, gp.tileSize*2, null);
        else { g2.setColor(new Color(180,120,255)); g2.fillRect(wx, wy, gp.tileSize*2, gp.tileSize*2); }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    private void drawPoof(Graphics2D g2, int cx, int cy, int timer, int duration) {
        float p = (float) timer / duration;
        float alpha = p < 0.5f ? p / 0.5f : (1f - p) / 0.5f;
        alpha = Math.max(0f, Math.min(1f, alpha));
        int size = (int)(80 + p * 120);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        if (greenSmoke != null)
            g2.drawImage(greenSmoke, cx-size/2, cy-size/2, size, size, null);
        else { g2.setColor(new Color(40,160,40)); g2.fillOval(cx-size/2, cy-size/2, size, size); }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    private void drawFlashback(Graphics2D g2, String item) {
        // White base
        g2.setColor(Color.white);
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        // Subtle warm wash
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
        g2.setColor(new Color(255, 220, 160));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        int ts = gp.tileSize;
        int qx = gp.screenWidth/2 - ts, qy = gp.screenHeight/2 - ts;
        if (queenSprite!=null) g2.drawImage(queenSprite, qx, qy, ts*2, ts*2, null);
        else { g2.setColor(new Color(255,200,220)); g2.fillRect(qx, qy, ts, ts); }

        g2.setFont(new Font("Courier New", Font.BOLD, 18));
        g2.setColor(new Color(80, 55, 30));
        String lbl = switch(item){ case "rose"->"A rose..."; case "ring"->"A ring..."; default->"A crown..."; };
        g2.drawString(lbl, gp.screenWidth/2 - g2.getFontMetrics().stringWidth(lbl)/2, qy + ts*2 + 30);
    }

    private void drawEnding(Graphics2D g2) {
        g2.setColor(new Color(255,200,180));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
    }

    public boolean isActive() { return sceneActive; }
}
