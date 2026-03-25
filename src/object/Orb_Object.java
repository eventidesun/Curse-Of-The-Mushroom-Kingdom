package object;

import main.GamePanel;

import java.awt.*;

public class Orb_Object extends SuperObject {

    // Animation
    private int glowFrame  = 0;
    private float glowAlpha = 0.3f;
    private boolean glowUp  = true;

    public Orb_Object() {
        name      = "Orb";
        collision = true;

        // No image loaded — drawn entirely in code for the glow effect
        // Swap to orb.png when art is ready
    }

    @Override
    public void interact(GamePanel gp, int index) {
        // Touching the orb starts the puzzle
        gp.gameState = GamePanel.GameState.PUZZLE;
        gp.puzzleScene.startPuzzle();
    }

    public void draw(Graphics2D g2, GamePanel gp) {
        int screenX = worldX - gp.player.worldX + gp.player.screenX;
        int screenY = worldY - gp.player.worldY + gp.player.screenY;

        boolean onScreen =
                worldX + gp.tileSize * 2 > gp.player.worldX - gp.player.screenX &&
                        worldX - gp.tileSize * 2 < gp.player.worldX + gp.player.screenX &&
                        worldY + gp.tileSize * 2 > gp.player.worldY - gp.player.screenY &&
                        worldY - gp.tileSize * 2 < gp.player.worldY + gp.player.screenY;

        if (!onScreen) return;

        // Animate glow pulse
        glowFrame++;
        if (glowUp) {
            glowAlpha += 0.01f;
            if (glowAlpha >= 0.7f) glowUp = false;
        } else {
            glowAlpha -= 0.01f;
            if (glowAlpha <= 0.2f) glowUp = true;
        }

        int orbSize = gp.tileSize * 2; // 128x128 on screen

        // Outer glow ring
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, glowAlpha * 0.5f));
        g2.setColor(new Color(160, 100, 255));
        g2.fillOval(screenX - 8, screenY - 8, orbSize + 16, orbSize + 16);

        // Orb body
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        g2.setColor(new Color(120, 60, 220));
        g2.fillOval(screenX, screenY, orbSize, orbSize);

        // Inner shine
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
        g2.setColor(Color.white);
        g2.fillOval(screenX + 12, screenY + 10, orbSize / 3, orbSize / 4);

        // Queen silhouette inside (placeholder — replace with queen sprite)
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g2.setColor(new Color(255, 200, 220));
        int queenX = screenX + orbSize / 2 - 16;
        int queenY = screenY + orbSize / 2 - 20;
        g2.fillRect(queenX, queenY, 32, 40); // body placeholder

        // Orb border
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        g2.setColor(new Color(200, 160, 255));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawOval(screenX, screenY, orbSize, orbSize);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }
}
