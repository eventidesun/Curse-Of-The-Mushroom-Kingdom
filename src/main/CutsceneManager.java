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

    private BufferedImage kingSprite, queenSprite, dragonSprite, witchSprite;
    private BufferedImage tileWall, tileFloor, greenSmoke;
    private BufferedImage dragon1, dragon2, dragon3;

    // Dragon
    private int     dragonX        = 0;
    private int     dragonY        = 0;
    private boolean dragonVisible  = false;
    private boolean queenVisible   = true;
    private boolean dragonCarrying = false;
    private int     shakeTimer     = 0;
    private boolean dragonReturning = false;

    // Poof
    private boolean poofActive = false;
    private int     poofTimer  = 0;
    private int     poofX, poofY;
    private static final int POOF_DURATION = 60;

    // Screen darken — used for within-scene fades
    private float   darkenAlpha  = 0f;
    private boolean darkenActive = false;

    private enum DissolveState { NONE, FADING_OUT, FADING_IN }
    private DissolveState dissolveState = DissolveState.NONE;
    private float         dissolveAlpha = 0f;
    private static final float DISSOLVE_SPEED = 0.07f;
    private Runnable      dissolveOnBlack = null; // fires once screen is fully black

    // Transform sequence
    private int     transformPhase    = 0;
    private int     transformTimer    = 0;
    private boolean transformVisible  = false;
    private boolean isWitchForm       = false;
    private int     spellBeamX        = 0;
    private boolean spellActive       = false;
    private int     kingCurseTimer    = 0;
    private boolean witchPoofOut      = false;
    private int     witchPoofTimer    = 0;
    private boolean didTransformFlash = false;

    // dragon animation
    private int dragonFrame = 0;
    private int dragonAnimTimer = 0;

    // Phase 3 king dialogue guard
    private boolean kingConfusedStarted = false;

    // Dream scene guards
    private boolean narratorDone      = false;
    private boolean witchDialogueDone = false;

    private static final String[] TRANSFORM_DIALOGUE_WITCH = {
            "Pathetic king.",
            "You could not even protect her.",
            "Now you will not even remember her.",
            "*cackles*"
    };

    private static final String[] TRANSFORM_DIALOGUE_CURSE = {
            "By the dark magic of forgotten love...",
            "Let his heart be empty.",
            "Let her face fade from his mind.",
            "Let her name dissolve like smoke.",
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
            "Hmm. I should get going",
            "Time to serve the kingdom!"
    };

    public CutsceneManager(GamePanel gp) {
        this.gp = gp;
        loadImages();
    }

    private void loadImages() {
        kingSprite   = loadImg("/player/boy_down_1.png");
        queenSprite  = loadImg("/player/boy_down_2.png");
        dragon1 = loadImg("/enemies/dragon1.png");
        dragon2 = loadImg("/enemies/dragon2.png");
        dragon3 = loadImg("/enemies/dragon3.png");
        dragonSprite = loadImg("/enemies/dragon1.png");
        witchSprite  = loadImg("/enemies/dragon_fire1.png");
        tileWall     = loadImg("/tiles/wall.png");
        tileFloor    = loadImg("/tiles/stone_path.png");
        greenSmoke   = loadImg("/cutscenes/green_smoke.png");
    }

    private BufferedImage loadImg(String path) {
        try { return ImageIO.read(getClass().getResourceAsStream(path)); }
        catch (Exception e) { System.out.println("Cutscene img not found: " + path); return null; }
    }

    private void dissolveToNext(Runnable onBlack) {
        if (dissolveState != DissolveState.NONE) return; // already dissolving
        dissolveState  = DissolveState.FADING_OUT;
        dissolveAlpha  = 0f;
        dissolveOnBlack = onBlack;
    }

    private void updateDissolve() {
        if (dissolveState == DissolveState.NONE) return;
        if (dissolveState == DissolveState.FADING_OUT) {
            dissolveAlpha += DISSOLVE_SPEED;
            if (dissolveAlpha >= 1f) {
                dissolveAlpha = 1f;
                dissolveState = DissolveState.FADING_IN;
                if (dissolveOnBlack != null) {
                    dissolveOnBlack.run();
                    dissolveOnBlack = null;
                }
            }
        } else if (dissolveState == DissolveState.FADING_IN) {
            dissolveAlpha -= DISSOLVE_SPEED;
            if (dissolveAlpha <= 0f) {
                dissolveAlpha = 0f;
                dissolveState = DissolveState.NONE;
            }
        }
    }

    // start scene
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
        darkenActive        = false;
        shakeTimer          = 0;
        transformVisible    = false;
        isWitchForm         = false;
        transformPhase      = 0;
        transformTimer      = 0;
        spellActive         = false;
        kingCurseTimer      = 0;
        witchPoofOut        = false;
        witchPoofTimer      = 0;
        didTransformFlash   = false;
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

        if (poofActive) {
            poofTimer++;
            if (poofTimer >= POOF_DURATION) {
                poofActive     = false;
                queenVisible   = false;
                poofTimer      = 0;
                dragonCarrying = true;
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

        // wait for dialogue
        if (scenePhase == 0 && !gp.dialogueManager.isActive()
                && dissolveState == DissolveState.NONE) {
            scenePhase = 1;
        }

        // dragon attack
        if (scenePhase == 1) {

            dragonAnimTimer++;
            if (dragonAnimTimer > 10) { // speed of flapping
                dragonFrame = (dragonFrame + 1) % 3;
                dragonAnimTimer = 0;
            }

            dragonVisible = true;

            if (!dragonCarrying && !poofActive) {
                dragonX -= 10;

                if (darkenAlpha < 0.3f) darkenAlpha += 0.006f;

                int queenX = gp.screenWidth / 2 + 40;

                if (dragonX <= queenX + 60 && queenVisible) {
                    shakeTimer = 40;
                    poofActive = true;
                    poofTimer = 0;

                    poofX = queenX;
                    poofY = gp.screenHeight / 2 - gp.tileSize / 2;

                    gp.playSE(7);
                    gp.stopMusic();
                }
            }

            if (dragonCarrying) {
                dragonX += 12;

                if (dragonX > gp.screenWidth + 200
                        && dissolveState == DissolveState.NONE) {

                    dissolveToNext(() -> {
                        dragonVisible    = false;
                        dragonX          = gp.screenWidth / 2 - gp.tileSize;
                        dragonY          = gp.screenHeight / 2 - gp.tileSize;
                        transformPhase   = 0;
                        transformTimer   = 0;
                        transformVisible = false;
                        didTransformFlash = false;
                        darkenAlpha      = 0f;
                        scenePhase       = 2;
                    });
                }
            }
        }

        // transform sequence, phase 2
        if (scenePhase == 2 && dissolveState != DissolveState.FADING_OUT) {
            transformTimer++;

            switch (transformPhase) {
                case 0 -> {
                    transformVisible = true; isWitchForm = false;
                    if (transformTimer > 60) {
                        transformPhase = 1; transformTimer = 0;
                        gp.dialogueManager.startDialogue("witch", TRANSFORM_DIALOGUE_WITCH);
                    }
                }
                case 1 -> {
                    if (!gp.dialogueManager.isActive()) {
                        transformPhase = 2; transformTimer = 0;
                    }
                }
                case 2 -> {
                    if (transformTimer == 1) { shakeTimer = 50; gp.playSE(7); }
                    if (transformTimer == 30 && !didTransformFlash) {
                        isWitchForm = true;
                        didTransformFlash = true;
                    }
                    if (transformTimer > 80) {
                        transformPhase = 3; transformTimer = 0;
                        gp.dialogueManager.startDialogue("witch", TRANSFORM_DIALOGUE_CURSE);
                    }
                }
                case 3 -> {
                    if (!gp.dialogueManager.isActive()) {
                        transformPhase = 4;
                        transformTimer = 0;
                        witchPoofTimer = 0;
                        witchPoofOut   = true;
                    }
                }
                case 4 -> {
                    witchPoofTimer++;
                    if (transformTimer > 60) witchPoofOut = true;
                    // Once witch is gone, dissolve to king alone scene
                    if (witchPoofTimer > 60 && dissolveState == DissolveState.NONE) {
                        dissolveToNext(() -> {
                            transformVisible = false;
                            witchPoofOut     = true;
                            scenePhase       = 3;
                            transformTimer   = 0;
                        });
                    }
                }
            }
        }

        // PHASE 3: king alone, confused — dialogue then dissolve to dream
        if (scenePhase == 3 && dissolveState == DissolveState.NONE) {
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

    // dream
    private void updateDream() {
        // Phase 0: narrator — wait for finish
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
        // Phase 1: witch done — dissolve to overworld
        if (scenePhase == 1 && narratorDone && !witchDialogueDone
                && !gp.dialogueManager.isActive()
                && dissolveState == DissolveState.NONE) {
            witchDialogueDone = true;
            dissolveToNext(() -> {
                sceneActive      = false;
                gp.gameState     = GamePanel.GameState.OVERWORLD;
                gp.playMusic(2);
            });
        }
    }

    // for flashbacks and ending
    private void updateFlashback() {
        if (!gp.dialogueManager.isActive() && dissolveState == DissolveState.NONE) {
            dissolveToNext(() -> {
                sceneActive  = false;
                gp.gameState = GamePanel.GameState.OVERWORLD;
            });
        }
    }

    private void updateEnding() {
        if (!gp.dialogueManager.isActive() && dissolveState == DissolveState.NONE) {
            dissolveToNext(() -> {
                sceneActive  = false;
                gp.gameState = GamePanel.GameState.ENDING;
            });
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

        if (darkenAlpha > 0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, darkenAlpha)));
            g2.setColor(Color.black);
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        g2.setColor(Color.black);
        g2.fillRect(0, 0, gp.screenWidth, 70);

        gp.ui.drawDialogueBox(g2);

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

        // Walls
        for (int col=0; col<=gp.screenWidth/ts; col++)
            for (int row=0; row<3; row++) {
                if (tileWall!=null) g2.drawImage(tileWall,col*ts,row*ts,ts,ts,null);
                else { g2.setColor(new Color(100,100,120)); g2.fillRect(col*ts,row*ts,ts,ts); }
            }

        // King
        int kingX=gp.screenWidth/2-120, kingY=gp.screenHeight/2-ts/2;
        if (kingSprite!=null) g2.drawImage(kingSprite,kingX,kingY,ts,ts,null);
        else { g2.setColor(new Color(80,140,80)); g2.fillRect(kingX,kingY,ts,ts); }

        // Queen
        if (queenVisible) {
            int qx=gp.screenWidth/2+40, qy=gp.screenHeight/2-ts/2;
            if (queenSprite!=null) g2.drawImage(queenSprite,qx,qy,ts,ts,null);
            else { g2.setColor(new Color(200,100,160)); g2.fillRect(qx,qy,ts,ts); }
        }

        if (dragonVisible) {

            BufferedImage currentDragon = switch (dragonFrame) {
                case 0 -> dragon1;
                case 1 -> dragon2;
                default -> dragon3;
            };

            int dragonWidth  = ts * 4;   // bigger dragon
            int dragonHeight = ts * 4;

            int bob = (int)(Math.sin(phaseTimer * 0.2) * 6);
            int drawY = dragonY + bob;

            // Queen being carried
            if (dragonCarrying && queenSprite != null) {
                g2.drawImage(queenSprite, dragonX, drawY - ts, ts, ts, null);
            }

            if (currentDragon != null) {
                g2.drawImage(
                        currentDragon,
                        dragonX + dragonWidth, drawY,
                        -dragonWidth, dragonHeight, // flip horizontally
                        null
                );
            } else {
                g2.setColor(new Color(160,30,30));
                g2.fillRect(dragonX, drawY, dragonWidth, dragonHeight);
            }
        }

        // Poof
        if (poofActive) drawPoof(g2, poofX+ts/2, poofY+ts/2, poofTimer, POOF_DURATION);

        // Transform villain
        if (transformVisible) {
            int tx=gp.screenWidth/2-ts, ty=gp.screenHeight/2-ts;

            if (scenePhase==2 && transformPhase==2 && transformTimer==30 && didTransformFlash) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
                g2.setColor(Color.white);
                g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            }

            if (!isWitchForm) {

                BufferedImage currentDragon = switch (dragonFrame) {
                    case 0 -> dragon1;
                    case 1 -> dragon2;
                    default -> dragon3;
                };

                int dragonWidth  = ts * 4;
                int dragonHeight = ts * 4;

                if (currentDragon != null) {
                    g2.drawImage(currentDragon,
                            tx + dragonWidth, ty,
                            -dragonWidth, dragonHeight,
                            null);

                } else {
                    g2.setColor(Color.RED);
                    g2.fillRect(tx, ty, dragonWidth, dragonHeight);
                }
            } else {
                if (witchPoofOut) {
                    float wo = Math.min(1f, witchPoofTimer / 60f);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f-wo));
                }
                if (witchSprite!=null) g2.drawImage(witchSprite,tx,ty,ts*2,ts*2,null);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                if (witchPoofOut) drawPoof(g2, tx+ts, ty+ts, witchPoofTimer, 60);
            }
        }

        // Spell beam
        if (spellActive) drawSpellBeam(g2, spellBeamX, gp.screenHeight/2-10);
    }

    // dream scene
    private void drawDream(Graphics2D g2) {
        g2.setColor(new Color(10, 5, 30));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        int kx=gp.screenWidth/2-32, ky=gp.screenHeight-250;
        if (kingSprite!=null) g2.drawImage(kingSprite,kx,ky,gp.tileSize,gp.tileSize,null);
        else { g2.setColor(new Color(80,140,80)); g2.fillRect(kx,ky,gp.tileSize,gp.tileSize); }

        if (scenePhase == 1) {
            float bob = (float)(Math.sin(phaseTimer * 0.05) * 8);
            int wx = gp.screenWidth/2-32, wy = (int)(gp.screenHeight/2-80+bob);
            if (witchSprite!=null) g2.drawImage(witchSprite,wx,wy,gp.tileSize,gp.tileSize,null);
            else { g2.setColor(new Color(180,120,255)); g2.fillRect(wx,wy,gp.tileSize,gp.tileSize); }
        }
    }

    private void drawPoof(Graphics2D g2, int cx, int cy, int timer, int duration) {
        float p = (float) timer / duration;
        float alpha = p < 0.5f ? p / 0.5f : (1f - p) / 0.5f;
        alpha = Math.max(0f, Math.min(1f, alpha));
        int size = (int)(80 + p * 120);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        if (greenSmoke != null)
            g2.drawImage(greenSmoke, cx-size/2, cy-size/2, size, size, null);
        else {
            g2.setColor(new Color(40,160,40));
            g2.fillOval(cx-size/2, cy-size/2, size, size);
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    private void drawSpellBeam(Graphics2D g2, int x, int y) {
        for (int i=0; i<8; i++) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (8-i)/8f*0.8f));
            g2.setColor(new Color(180,0,255));
            g2.fillRoundRect(x+i*20, y-6, 30-i*3, 12, 6, 6);
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2.setColor(new Color(240,180,255)); g2.fillOval(x-12,y-12,24,24);
        g2.setColor(new Color(200,100,255)); g2.fillOval(x-8,y-8,16,16);
    }

    // -----------------------------------------------
    // FLASHBACK
    // -----------------------------------------------
    private void drawFlashback(Graphics2D g2, String item) {
        g2.setColor(new Color(180,140,80)); g2.fillRect(0,0,gp.screenWidth,gp.screenHeight);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
        g2.setColor(new Color(140,90,30)); g2.fillRect(0,0,gp.screenWidth,gp.screenHeight);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
        int qx=gp.screenWidth/2-32, qy=gp.screenHeight/2-80;
        if (queenSprite!=null) g2.drawImage(queenSprite,qx,qy,gp.tileSize,gp.tileSize,null);
        else { g2.setColor(new Color(255,200,220)); g2.fillRect(qx,qy,gp.tileSize,gp.tileSize); }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2.setFont(new Font("Courier New",Font.BOLD,18)); g2.setColor(new Color(255,240,180));
        String lbl = switch(item){case "rose"->"A rose..."; case "ring"->"A ring..."; default->"A crown...";};
        g2.drawString(lbl, gp.screenWidth/2-g2.getFontMetrics().stringWidth(lbl)/2, qy+gp.tileSize+50);
    }

    private void drawEnding(Graphics2D g2) {
        g2.setColor(new Color(255,200,180));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
    }

    public boolean isActive() { return sceneActive; }
}
