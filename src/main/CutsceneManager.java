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
    private BufferedImage tileWall, tileFloor;

    private int     dragonX       = 0;
    private boolean dragonVisible = false;
    private boolean queenVisible  = true;
    private float   poofAlpha     = 0f;
    private boolean poofActive    = false;
    private int     poofTimer     = 0;
    private int     shakeTimer    = 0;
    private int     dreamFrameIndex = 0;
    private int     dreamFrameTimer = 0;

    private static final String[][] ACT1_DIALOGUE = {
            {
                    "Welcome to the Mushroom Kingdom.",
                    "A peaceful land ruled by a kind king and his beloved queen.",
                    "Today, like every day, the kingdom is full of joy.",
                    "The king and queen stand together in their chamber.",
                    "All is well... for now."
            },
            {
                    "...",
                    "What... what just happened?",
                    "Someone was here.",
                    "Someone important.",
                    "Why can not I remember...?"
            }
    };

    public CutsceneManager(GamePanel gp) {
        this.gp = gp;
        loadImages();
    }

    private void loadImages() {
        kingSprite   = loadImg("/player/boy_down_1.png");
        queenSprite  = loadImg("/player/boy_down_2.png");
        dragonSprite = loadImg("/objects/chest.png");
        witchSprite  = loadImg("/player/boy_up_1.png");
        tileWall     = loadImg("/tiles/wall.png");
        tileFloor    = loadImg("/tiles/earth.png");
    }

    private BufferedImage loadImg(String path) {
        try { return ImageIO.read(getClass().getResourceAsStream(path)); }
        catch (Exception e) { System.out.println("Cutscene img not found: " + path); return null; }
    }

    public void startScene(Scene scene) {
        currentScene  = scene;
        scenePhase    = 0;
        phaseTimer    = 0;
        sceneActive   = true;
        dragonVisible = false;
        queenVisible  = true;
        poofActive    = false;
        poofAlpha     = 0f;
        dragonX       = gp.screenWidth + 200;

        gp.gameState = switch (scene) {
            case DREAM_WITCH -> GamePanel.GameState.DREAM;
            default          -> GamePanel.GameState.CUTSCENE;
        };

        switch (scene) {
            case INTRO_CASTLE -> gp.dialogueManager.startDialogue("king", ACT1_DIALOGUE[0]);
            case DREAM_WITCH  -> gp.dialogueManager.startDialogue("witch", new String[]{
                    "King... can you hear me?",
                    "The curse has stolen your memory.",
                    "You had someone precious to you.",
                    "Follow the forest path.",
                    "The answers lie with those who took from her."
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

        if (currentScene == Scene.DREAM_WITCH) {
            dreamFrameTimer++;
            if (dreamFrameTimer > 20) { dreamFrameIndex = (dreamFrameIndex + 1) % 3; dreamFrameTimer = 0; }
        }

        if (poofActive) {
            poofTimer++;
            poofAlpha = Math.min(1f, poofTimer / 10f);
            if (poofTimer > 30) {
                poofActive = false; queenVisible = false; poofAlpha = 0f;
                gp.dialogueManager.startDialogue("king", ACT1_DIALOGUE[1]);
                scenePhase = 2;
            }
        }

        if (currentScene == Scene.INTRO_CASTLE && scenePhase == 1) {
            dragonVisible = true;
            dragonX -= 8;
            if (dragonX < gp.screenWidth / 2 - 50 && !poofActive && queenVisible) {
                poofActive = true; poofTimer = 0; shakeTimer = 30;
                gp.playSE(7); gp.stopMusic();
            }
        }

        if (!gp.dialogueManager.isActive() && sceneActive) {
            switch (currentScene) {
                case INTRO_CASTLE -> {
                    if (scenePhase == 0) { scenePhase = 1; gp.playSE(6); }
                    else if (scenePhase == 2) { endScene(); startScene(Scene.DREAM_WITCH); }
                }
                case DREAM_WITCH -> { endScene(); gp.gameState = GamePanel.GameState.OVERWORLD; gp.playMusic(1); }
                case FLASHBACK_ROSE, FLASHBACK_RING, FLASHBACK_TIARA -> { endScene(); gp.gameState = GamePanel.GameState.OVERWORLD; }
                case ENDING -> { endScene(); gp.gameState = GamePanel.GameState.ENDING; }
            }
        }

        if (gp.keyH.debugPressed) {
            gp.keyH.debugPressed = false;
            endScene(); gp.gameState = GamePanel.GameState.OVERWORLD; gp.playMusic(1);
        }
    }

    private void endScene() { sceneActive = false; }

    public void draw(Graphics2D g2) {
        if (!sceneActive) return;

        if (shakeTimer > 0) {
            g2.translate((int)((Math.random()-0.5)*12), (int)((Math.random()-0.5)*12));
        }

        switch (currentScene) {
            case INTRO_CASTLE    -> drawBedroomScene(g2);
            case DREAM_WITCH     -> drawDreamScene(g2);
            case FLASHBACK_ROSE  -> drawFlashback(g2, "rose");
            case FLASHBACK_RING  -> drawFlashback(g2, "ring");
            case FLASHBACK_TIARA -> drawFlashback(g2, "tiara");
            case ENDING          -> drawEndingScene(g2);
        }

        // Top letterbox bar only — leave bottom clear for dialogue
        g2.setColor(Color.black);
        g2.fillRect(0, 0, gp.screenWidth, 70);

        // Dialogue box drawn last so it's always on top
        gp.ui.drawDialogueBox(g2);
    }

    private void drawBedroomScene(Graphics2D g2) {
        int ts = gp.tileSize;

        for (int col = 0; col <= gp.screenWidth/ts; col++)
            for (int row = 0; row <= gp.screenHeight/ts; row++) {
                if (tileFloor != null) g2.drawImage(tileFloor, col*ts, row*ts, ts, ts, null);
                else { g2.setColor(new Color(160,130,90)); g2.fillRect(col*ts,row*ts,ts,ts); }
            }

        for (int col = 0; col <= gp.screenWidth/ts; col++)
            for (int row = 0; row < 3; row++) {
                if (tileWall != null) g2.drawImage(tileWall, col*ts, row*ts, ts, ts, null);
                else { g2.setColor(new Color(100,100,120)); g2.fillRect(col*ts,row*ts,ts,ts); }
            }

        int winX = gp.screenWidth/2-60, winY = ts, winW = 120, winH = 80;
        g2.setColor(new Color(135,200,240)); g2.fillRect(winX,winY,winW,winH);
        g2.setColor(new Color(80,60,40)); g2.setStroke(new BasicStroke(4));
        g2.drawRect(winX,winY,winW,winH);
        g2.drawLine(winX+winW/2,winY,winX+winW/2,winY+winH);
        g2.drawLine(winX,winY+winH/2,winX+winW,winY+winH/2);
        g2.setStroke(new BasicStroke(1));

        int bedX = gp.screenWidth-300, bedY = gp.screenHeight/2-60;
        g2.setColor(new Color(120,80,60)); g2.fillRect(bedX,bedY,220,140);
        g2.setColor(new Color(200,180,220)); g2.fillRect(bedX+10,bedY+10,200,100);
        g2.setColor(new Color(240,220,240));
        g2.fillRoundRect(bedX+20,bedY+15,80,40,10,10);
        g2.fillRoundRect(bedX+120,bedY+15,80,40,10,10);

        int kingX = gp.screenWidth/2-120, kingY = gp.screenHeight/2-ts/2;
        if (kingSprite != null) g2.drawImage(kingSprite,kingX,kingY,ts,ts,null);
        else { g2.setColor(new Color(80,140,80)); g2.fillRect(kingX,kingY,ts,ts); }

        if (queenVisible) {
            int qx = gp.screenWidth/2+40, qy = gp.screenHeight/2-ts/2;
            if (queenSprite != null) g2.drawImage(queenSprite,qx,qy,ts,ts,null);
            else { g2.setColor(new Color(200,100,160)); g2.fillRect(qx,qy,ts,ts); }
            if (poofActive) {
                float r = poofTimer * 6f;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f,0.8f-poofAlpha)));
                g2.setColor(new Color(40,160,40));
                g2.fillOval((int)(qx+ts/2-r),(int)(qy+ts/2-r),(int)(r*2),(int)(r*2));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1f));
            }
        }

        if (dragonVisible) {
            int dy = gp.screenHeight/2-ts;
            if (dragonSprite != null) g2.drawImage(dragonSprite,dragonX,dy,ts*2,ts*2,null);
            else { g2.setColor(new Color(160,30,30)); g2.fillRect(dragonX,dy,ts*2,ts*2); }
        }

        drawTorch(g2, winX-50, winY+10);
        drawTorch(g2, winX+winW+20, winY+10);
    }

    private void drawTorch(Graphics2D g2, int x, int y) {
        g2.setColor(new Color(100,70,40)); g2.fillRect(x,y+20,12,30);
        float f = (float)(Math.sin(phaseTimer*0.15)*0.3+0.7);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,f));
        g2.setColor(new Color(255,140,20));
        g2.fillPolygon(new int[]{x-4,x+6,x+16,x+12,x+6}, new int[]{y+20,y-8,y+20,y+4,y+20}, 5);
        g2.setColor(new Color(255,220,60));
        g2.fillPolygon(new int[]{x+2,x+6,x+10}, new int[]{y+18,y,y+18}, 3);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1f));
    }

    private void drawDreamScene(Graphics2D g2) {
        g2.setColor(new Color(10,5,30)); g2.fillRect(0,0,gp.screenWidth,gp.screenHeight);
        for (int i = 0; i < 80; i++) {
            int sx=(i*137+50)%gp.screenWidth, sy=(i*97+30)%(gp.screenHeight-100);
            float tw=(float)(Math.sin(phaseTimer*0.05+i)*0.3+0.7);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,tw));
            g2.setColor(new Color(220,210,255)); g2.fillOval(sx,sy,3,3);
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1f));
        for (int i=0; i<5; i++) {
            float ma=0.08f+(float)(Math.sin(phaseTimer*0.03+i*1.2)*0.04);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,ma));
            g2.setColor(new Color(180,160,255));
            g2.fillOval(-100+i*200,gp.screenHeight-200,400,200);
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1f));
        int kx=gp.screenWidth/2-32, ky=gp.screenHeight-250;
        if (kingSprite!=null) g2.drawImage(kingSprite,kx,ky,gp.tileSize,gp.tileSize,null);
        else { g2.setColor(new Color(80,140,80)); g2.fillRect(kx,ky,gp.tileSize,gp.tileSize); }
        float bob=(float)(Math.sin(phaseTimer*0.05)*8);
        int wx=gp.screenWidth/2-32, wy=(int)(gp.screenHeight/2-80+bob);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.9f));
        if (witchSprite!=null) g2.drawImage(witchSprite,wx,wy,gp.tileSize,gp.tileSize,null);
        else { g2.setColor(new Color(180,120,255)); g2.fillRect(wx,wy,gp.tileSize,gp.tileSize); }
        for (int i=0; i<6; i++) {
            double a=phaseTimer*0.04+i*Math.PI/3;
            int spx=wx+32+(int)(Math.cos(a)*50), spy=wy+32+(int)(Math.sin(a)*30);
            float sa=(float)(Math.sin(phaseTimer*0.1+i)*0.3+0.7);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,sa));
            g2.setColor(new Color(220,180,255)); g2.fillOval(spx-4,spy-4,8,8);
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1f));
    }

    private void drawFlashback(Graphics2D g2, String item) {
        g2.setColor(new Color(180,140,80)); g2.fillRect(0,0,gp.screenWidth,gp.screenHeight);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.4f));
        g2.setColor(new Color(140,90,30)); g2.fillRect(0,0,gp.screenWidth,gp.screenHeight);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.4f));
        int qx=gp.screenWidth/2-32, qy=gp.screenHeight/2-80;
        if (queenSprite!=null) g2.drawImage(queenSprite,qx,qy,gp.tileSize,gp.tileSize,null);
        else { g2.setColor(new Color(255,200,220)); g2.fillRect(qx,qy,gp.tileSize,gp.tileSize); }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1f));
        g2.setFont(new Font("Courier New",Font.BOLD,18));
        g2.setColor(new Color(255,240,180));
        String lbl = switch(item){ case "rose"->"A rose..."; case "ring"->"A ring..."; default->"A crown..."; };
        g2.drawString(lbl, gp.screenWidth/2-g2.getFontMetrics().stringWidth(lbl)/2, qy+gp.tileSize+50);
    }

    private void drawEndingScene(Graphics2D g2) {
        g2.setColor(new Color(255,200,180)); g2.fillRect(0,0,gp.screenWidth,gp.screenHeight);
    }

    public boolean isActive() { return sceneActive; }
}
