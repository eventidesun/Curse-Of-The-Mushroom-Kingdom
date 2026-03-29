package main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

public class EndingScene {

    GamePanel gp;

    private BufferedImage kingSprite, queenSprite, heartImg;

    // PHASES
    // 0 — fade in, queen falls, king runs to catch
    // 1 — dialogue plays over the reunion scene
    // 2 — heart image grows from centre
    // 3 — fade to dark red
    // 4 — THE END
    private int   phase = 0;
    private int   timer = 0;

    // Queen fall
    private float queenY;
    private float queenTargetY;
    private float queenX;
    private boolean queenCaught = false;

    // King run
    private float kingX;
    private float kingTargetX;

    // Heart image
    private float heartSize    = 0f;
    private float heartMaxSize;

    // Fade to dark
    private float fadeAlpha = 0f;

    // Fade in from black at start
    private float dissolveAlpha = 1f;
    private boolean dissolved   = false;

    // Dialogue guard — only start dialogue once
    private boolean dialogueStarted  = false;
    private boolean dialogueFinished = false;
    private int     dialogueDelay    = 0; // wait a few frames before checking isActive

    public EndingScene(GamePanel gp) {
        this.gp      = gp;
        heartMaxSize = Math.min(gp.screenWidth, gp.screenHeight) * 0.75f;
        queenX       = gp.screenWidth / 2f - gp.tileSize / 2f;
        queenY       = -gp.tileSize * 2f;
        queenTargetY = gp.screenHeight / 2f - gp.tileSize / 2f;
        kingX        = gp.screenWidth / 2f + gp.tileSize * 3f;
        kingTargetX  = gp.screenWidth / 2f - gp.tileSize * 0.5f;
        loadSprites();
    }

    private void loadSprites() {
        try {
            kingSprite  = ImageIO.read(getClass().getResourceAsStream("/characters/king.png"));
            queenSprite = ImageIO.read(getClass().getResourceAsStream("/characters/queen.png"));
            heartImg    = ImageIO.read(getClass().getResourceAsStream("/cutscenes/heart.png"));
        } catch (Exception e) {
            System.out.println("EndingScene sprites not found: " + e.getMessage());
        }
    }

    public void update() {
        timer++;

        // Fade in from black
        if (!dissolved) {
            dissolveAlpha -= 0.04f;
            if (dissolveAlpha <= 0f) { dissolveAlpha = 0f; dissolved = true; }
            return;
        }

        switch (phase) {
            case 0 -> {
                // Queen falls from above
                if (!queenCaught) {
                    queenY = Math.min(queenY + 6f, queenTargetY);
                    if (queenY >= queenTargetY) queenCaught = true;
                }
                // King runs toward queen
                if (kingX > kingTargetX) kingX -= 4f;

                // Once both in place, wait then start dialogue
                if (queenCaught && kingX <= kingTargetX + 4f && timer > 80) {
                    phase = 1;
                    timer = 0;
                    dialogueStarted = false;
                }
            }
            case 1 -> {
                // Start dialogue exactly once
                if (!dialogueStarted) {
                    dialogueStarted = true;
                    dialogueDelay   = 0;
                    gp.dialogueManager.startDialogue("king", new String[]{
                            "You came for me.",
                            "I never forgot you.",
                            "Not really.",
                            "Not where it mattered."
                    });
                }
                // Wait at least 10 frames before checking isActive — dialoguew
                // needs a frame to register as active before we check completion
                dialogueDelay++;
                if (dialogueStarted && dialogueDelay > 10 && !gp.dialogueManager.isActive()) {
                    phase = 2;
                    timer = 0;
                    gp.stopMusic();
                    gp.playMusic(0);
                }
            }
            case 2 -> {
                heartSize = Math.min(heartSize + 8f, heartMaxSize);
                if (heartSize >= heartMaxSize) { phase = 3; timer = 0; }
            }
            case 3 -> {
                fadeAlpha = Math.min(fadeAlpha + 0.012f, 1f);
                if (fadeAlpha >= 1f) { phase = 4; timer = 0; }
            }
            case 4 -> {
                if (timer > 240) gp.gameThread = null;
            }
        }
    }

    public void draw(Graphics2D g2) {
        int cx = gp.screenWidth  / 2;
        int cy = gp.screenHeight / 2;
        int ts = gp.tileSize;

        // Phase 0 & 1: reunion scene
        if (phase <= 1) {
            // Warm sunset background
            g2.setColor(new Color(255, 210, 170));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

            // Ground
            g2.setColor(new Color(140, 190, 100));
            g2.fillRect(0, cy + ts, gp.screenWidth, gp.screenHeight - cy - ts);

            // Stone path
            g2.setColor(new Color(170, 160, 150));
            g2.fillRect(cx - ts * 3, cy, ts * 6, ts);

            // Queen — 2x size
            int qx = (int) queenX, qy = (int) queenY;
            if (queenSprite != null)
                g2.drawImage(queenSprite, qx, qy, ts*2, ts*2, null);
            else {
                g2.setColor(new Color(220, 120, 160));
                g2.fillRect(qx, qy, ts, ts);
            }

            // King — 2x size
            int kx = (int) kingX, ky = (int)(queenTargetY);
            if (kingSprite != null)
                g2.drawImage(kingSprite, kx, ky, ts*2, ts*2, null);
            else {
                g2.setColor(new Color(80, 140, 80));
                g2.fillRect(kx, ky, ts, ts);
            }

            // Floating hearts above queen once caught
            if (queenCaught) {
                for (int i = 0; i < 3; i++) {
                    float pulse = (float)(Math.sin(timer * 0.08 + i * 1.2) * 4);
                    int hx = cx - 20 + i * 24;
                    int hy = (int)(qy - 50 - i * 10 + pulse);
                    if (heartImg != null) {
                        g2.drawImage(heartImg, hx - 12, hy - 12, 28, 28, null);
                    } else {
                        g2.setColor(new Color(220, 50, 80, 200));
                        drawHeart(g2, hx, hy, 18);
                    }
                }
            }

            // Dialogue box drawn on top during phase 1
            if (phase == 1) gp.ui.drawDialogueBox(g2);
        }

        // Phase 2+: growing heart image
        if (phase >= 2) {
            g2.setColor(new Color(200, 40, 60));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

            int hs = (int) heartSize;
            if (heartImg != null) {
                g2.drawImage(heartImg, cx - hs/2, cy - hs/2, hs, hs, null);
            } else {
                g2.setColor(new Color(230, 70, 90));
                drawHeart(g2, cx - hs/2, cy - hs/2, hs);
            }
        }

        // Phase 3+: fade overlay
        if (phase >= 3) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, fadeAlpha)));
            g2.setColor(new Color(160, 20, 40));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        // Phase 4: THE END
        if (phase >= 4) {
            g2.setFont(new Font("Courier New", Font.BOLD, 72));
            g2.setColor(new Color(255, 230, 220));
            String end = "THE END";
            g2.drawString(end, cx - g2.getFontMetrics().stringWidth(end)/2, cy - 20);

            g2.setFont(new Font("Courier New", Font.BOLD, 22));
            g2.setColor(new Color(255, 210, 200, 200));
            String sub = "The Curse of the Mushroom Kingdom";
            g2.drawString(sub, cx - g2.getFontMetrics().stringWidth(sub)/2, cy + 36);
        }

        if (!dissolved || dissolveAlpha > 0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, dissolveAlpha)));
            g2.setColor(Color.black);
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }
    }

    private void drawHeart(Graphics2D g2, int x, int y, int size) {
        int half = size / 2;
        g2.fillOval(x, y, half, half);
        g2.fillOval(x + half, y, half, half);
        int[] hx = { x, x + size, x + size/2 };
        int[] hy = { y + half/2, y + half/2, y + size };
        g2.fillPolygon(hx, hy, 3);
    }
}
