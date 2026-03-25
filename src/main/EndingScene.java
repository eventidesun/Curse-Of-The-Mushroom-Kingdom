package main;

import java.awt.*;

public class EndingScene {

    GamePanel gp;

    private int timer        = 0;
    private int phase        = 0;

    // Heart animation
    private float heartSize  = 0f;
    private float heartMaxSize;

    // Screen fade
    private float fadeAlpha  = 0f;

    // Villager clap animation
    private int clapFrame    = 0;
    private int clapTimer    = 0;

    // Villager positions (row of 6 at bottom)
    private int[] villagerX;
    private int   villagerY;

    public EndingScene(GamePanel gp) {
        this.gp = gp;
        heartMaxSize = Math.min(gp.screenWidth, gp.screenHeight) * 0.8f;
        villagerY    = gp.screenHeight - gp.tileSize - 20;
        villagerX    = new int[] {
                gp.screenWidth / 2 - 320,
                gp.screenWidth / 2 - 192,
                gp.screenWidth / 2 - 64,
                gp.screenWidth / 2 + 64,
                gp.screenWidth / 2 + 192,
                gp.screenWidth / 2 + 320
        };
    }

    public void update() {
        timer++;
        clapTimer++;
        if (clapTimer > 20) {
            clapFrame = clapFrame == 0 ? 1 : 0;
            clapTimer = 0;
        }

        switch (phase) {
            case 0 -> {
                // Show reunion scene for 3 seconds
                if (timer > 180) { phase = 1; timer = 0; gp.playSE(1); }
            }
            case 1 -> {
                // Villagers appear + cheer — play cheer SFX once
                if (timer == 1) gp.playSE(1); // swap to crowd_cheer.wav
                if (timer > 240) { phase = 2; timer = 0; }
            }
            case 2 -> {
                // Heart grows from centre
                heartSize = Math.min(heartSize + 12f, heartMaxSize);
                if (heartSize >= heartMaxSize) { phase = 3; timer = 0; }
            }
            case 3 -> {
                // Fade to red
                fadeAlpha = Math.min(fadeAlpha + 0.015f, 1f);
                if (fadeAlpha >= 1f) { phase = 4; timer = 0; }
            }
            case 4 -> {
                // "THE END" stays for 3 seconds then stop
                if (timer > 180) gp.gameThread = null;
            }
        }
    }

    public void draw(Graphics2D g2) {
        int cx = gp.screenWidth  / 2;
        int cy = gp.screenHeight / 2;

        // Phase 0 + 1: show the reunion moment
        if (phase <= 1) {
            // Sky background
            g2.setColor(new Color(255, 200, 180));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

            // King and queen together (placeholder rectangles)
            // King
            g2.setColor(new Color(80, 140, 80));
            g2.fillRect(cx - 80, cy - 60, gp.tileSize, gp.tileSize);
            // Queen
            g2.setColor(new Color(220, 120, 160));
            g2.fillRect(cx + 16, cy - 60, gp.tileSize, gp.tileSize);

            // Heart between them
            g2.setColor(new Color(220, 50, 80, 200));
            drawHeart(g2, cx - 16, cy - 100, 48);

            if (phase == 1) {
                drawVillagers(g2);
            }
        }

        // Phase 2+: growing heart
        if (phase >= 2) {
            // Red background behind heart
            g2.setColor(new Color(200, 40, 60));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

            g2.setColor(new Color(220, 60, 80));
            drawHeart(g2, cx - (int)(heartSize / 2), cy - (int)(heartSize / 2), (int) heartSize);
        }

        // Phase 3+: fade overlay
        if (phase >= 3) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
            g2.setColor(new Color(180, 20, 40));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        // Phase 4: "THE END"
        if (phase >= 4) {
            g2.setFont(new Font("Courier New", Font.BOLD, 72));
            g2.setColor(new Color(255, 220, 220));
            String text = "THE END";
            int textW   = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, cx - textW / 2, cy - 20);

            g2.setFont(new Font("Courier New", Font.BOLD, 24));
            g2.setColor(new Color(255, 200, 200, 200));
            String sub = "The Curse of the Mushroom Kingdom";
            int subW   = g2.getFontMetrics().stringWidth(sub);
            g2.drawString(sub, cx - subW / 2, cy + 40);
        }
    }

    private void drawVillagers(Graphics2D g2) {
        for (int vx : villagerX) {
            // Simple villager: colored square with clapping arms
            g2.setColor(new Color(200, 160, 100));
            g2.fillRect(vx, villagerY, gp.tileSize, gp.tileSize);

            // Clap arms alternate left/right
            if (clapFrame == 0) {
                g2.setColor(new Color(180, 140, 80));
                g2.fillRect(vx - 8, villagerY + 16, 10, 6);
                g2.fillRect(vx + gp.tileSize - 2, villagerY + 16, 10, 6);
            } else {
                g2.setColor(new Color(180, 140, 80));
                g2.fillRect(vx - 4, villagerY + 22, 10, 6);
                g2.fillRect(vx + gp.tileSize - 6, villagerY + 22, 10, 6);
            }
        }
    }

    // Simple pixel heart shape
    private void drawHeart(Graphics2D g2, int x, int y, int size) {
        int half = size / 2;
        // Heart as two overlapping ovals + a triangle
        g2.fillOval(x,        y, half, half);
        g2.fillOval(x + half, y, half, half);
        int[] hx = { x, x + size, x + size / 2 };
        int[] hy = { y + half / 2, y + half / 2, y + size };
        g2.fillPolygon(hx, hy, 3);
    }
}
