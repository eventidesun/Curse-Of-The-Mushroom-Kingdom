package main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

public class EndingScene {

    GamePanel gp;

    private BufferedImage kingSprite, queenSprite;

    // -----------------------------------------------
    // PHASES
    // 0 — queen falls from above, king runs to catch
    // 1 — dialogue (from CutsceneManager.ENDING)
    // 2 — heart grows from centre
    // 3 — fade to warm red
    // 4 — THE END
    // -----------------------------------------------
    private int   phase     = 0;
    private int   timer     = 0;

    // Queen fall animation
    private float queenY    = 0f;
    private float queenTargetY;
    private float queenX;
    private boolean queenCaught = false;

    // King run animation
    private float  kingX;
    private float  kingTargetX;
    private int    kingFrame    = 0;
    private int    kingFrameTimer = 0;

    // Heart
    private float heartSize    = 0f;
    private float heartMaxSize;

    // Fade
    private float fadeAlpha    = 0f;

    // Dissolve into ending
    private float dissolveAlpha = 1f; // starts black, fades in
    private boolean dissolved   = false;

    public EndingScene(GamePanel gp) {
        this.gp  = gp;
        heartMaxSize  = Math.min(gp.screenWidth, gp.screenHeight) * 0.75f;
        queenX        = gp.screenWidth / 2f - gp.tileSize / 2f;
        queenY        = -gp.tileSize * 2f;                         // starts above screen
        queenTargetY  = gp.screenHeight / 2f - gp.tileSize / 2f;  // lands centre
        kingX         = gp.screenWidth / 2f + gp.tileSize * 3f;   // starts right
        kingTargetX   = gp.screenWidth / 2f - gp.tileSize * 0.5f; // runs to centre-left
        loadSprites();
    }

    private void loadSprites() {
        try {
            kingSprite  = ImageIO.read(getClass().getResourceAsStream("/player/boy_down_1.png"));
            queenSprite = ImageIO.read(getClass().getResourceAsStream("/player/boy_down_2.png"));
        } catch (Exception e) {
            System.out.println("EndingScene sprites not found: " + e.getMessage());
        }
    }

    public void update() {
        timer++;

        // Fade in from black at start
        if (!dissolved) {
            dissolveAlpha -= 0.04f;
            if (dissolveAlpha <= 0f) { dissolveAlpha = 0f; dissolved = true; }
            return;
        }

        // King walk animation
        kingFrameTimer++;
        if (kingFrameTimer > 10) { kingFrame = kingFrame == 0 ? 1 : 0; kingFrameTimer = 0; }

        switch (phase) {
            case 0 -> {
                // Queen falls
                if (!queenCaught) {
                    queenY = Math.min(queenY + 6f, queenTargetY);
                    if (queenY >= queenTargetY) queenCaught = true;
                }
                // King runs toward queen
                if (kingX > kingTargetX) kingX -= 4f;

                // Once both in place, short pause then start dialogue
                if (queenCaught && kingX <= kingTargetX + 4f && timer > 80) {
                    phase = 1;
                    timer = 0;
                    // Start the reunion dialogue via cutscene manager
                    gp.cutsceneManager.startScene(
                            main.CutsceneManager.Scene.ENDING
                    );
                }
            }
            case 1 -> {
                // Wait for ENDING cutscene dialogue to finish
                // CutsceneManager.ENDING calls gp.gameState = ENDING when done
                // but we've already done that — wait for sceneActive = false
                if (!gp.cutsceneManager.isActive()) {
                    phase = 2; timer = 0;
                    gp.playMusic(0); // castle music — peaceful
                }
            }
            case 2 -> {
                heartSize = Math.min(heartSize + 10f, heartMaxSize);
                if (heartSize >= heartMaxSize) { phase = 3; timer = 0; }
            }
            case 3 -> {
                fadeAlpha = Math.min(fadeAlpha + 0.012f, 1f);
                if (fadeAlpha >= 1f) { phase = 4; timer = 0; }
            }
            case 4 -> {
                if (timer > 240) gp.gameThread = null; // stop the game
            }
        }
    }

    public void draw(Graphics2D g2) {
        int cx = gp.screenWidth  / 2;
        int cy = gp.screenHeight / 2;
        int ts = gp.tileSize;

        // ---- Phase 0 & 1: reunion scene ----
        if (phase <= 1) {
            // Warm sunset background
            g2.setColor(new Color(255, 210, 170));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

            // Ground line
            g2.setColor(new Color(140, 190, 100));
            g2.fillRect(0, cy + ts, gp.screenWidth, gp.screenHeight - cy - ts);

            // Stone path strip
            g2.setColor(new Color(170, 160, 150));
            g2.fillRect(cx - ts * 3, cy, ts * 6, ts);

            // Queen falling / standing
            int qx = (int) queenX, qy = (int) queenY;
            if (queenSprite != null)
                g2.drawImage(queenSprite, qx, qy, ts, ts, null);
            else {
                g2.setColor(new Color(220, 120, 160));
                g2.fillRect(qx, qy, ts, ts);
            }

            // King running / standing
            int kx = (int) kingX, ky = (int) queenTargetY;
            if (kingSprite != null)
                g2.drawImage(kingSprite, kx, ky, ts, ts, null);
            else {
                g2.setColor(new Color(80, 140, 80));
                g2.fillRect(kx, ky, ts, ts);
            }

            // Small floating hearts once caught
            if (queenCaught) {
                for (int i = 0; i < 3; i++) {
                    float pulse = (float)(Math.sin(timer * 0.08 + i * 1.2) * 4);
                    int hx = cx - 20 + i * 20;
                    int hy = (int)(qy - 40 - i * 8 + pulse);
                    g2.setColor(new Color(220, 50, 80, 180));
                    drawHeart(g2, hx, hy, 18);
                }
            }
        }

        // ---- Phase 2+: growing heart ----
        if (phase >= 2) {
            g2.setColor(new Color(200, 40, 60));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
            g2.setColor(new Color(230, 70, 90));
            drawHeart(g2, cx - (int)(heartSize/2), cy - (int)(heartSize/2), (int) heartSize);
        }

        // ---- Phase 3+: fade overlay ----
        if (phase >= 3) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, fadeAlpha)));
            g2.setColor(new Color(160, 20, 40));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        // ---- Phase 4: THE END ----
        if (phase >= 4) {
            g2.setFont(new Font("Courier New", Font.BOLD, 72));
            g2.setColor(new Color(255, 230, 220));
            String end  = "THE END";
            int endW    = g2.getFontMetrics().stringWidth(end);
            g2.drawString(end, cx - endW / 2, cy - 20);

            g2.setFont(new Font("Courier New", Font.BOLD, 22));
            g2.setColor(new Color(255, 210, 200, 200));
            String sub  = "The Curse of the Mushroom Kingdom";
            int subW    = g2.getFontMetrics().stringWidth(sub);
            g2.drawString(sub, cx - subW / 2, cy + 36);
        }

        // Fade-in dissolve at start
        if (!dissolved || dissolveAlpha > 0f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, dissolveAlpha)));
            g2.setColor(Color.black);
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }
    }

    // Pixel heart using two ovals + triangle
    private void drawHeart(Graphics2D g2, int x, int y, int size) {
        int half = size / 2;
        g2.fillOval(x,        y, half, half);
        g2.fillOval(x + half, y, half, half);
        int[] hx = { x, x + size, x + size / 2 };
        int[] hy = { y + half / 2, y + half / 2, y + size };
        g2.fillPolygon(hx, hy, 3);
    }
}
