package object;

import main.GamePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Tiara_Object extends SuperObject {

    private BufferedImage poofImage;
    private int    poofTimer    = 0;
    private static final int POOF_DURATION = 90;
    private boolean poofDone   = false;

    public Tiara_Object() {
        name      = "Tiara";
        collision = false;

        try {
            image     = ImageIO.read(getClass().getResourceAsStream("/objects/tiara.png"));
            poofImage = ImageIO.read(getClass().getResourceAsStream("/objects/tiara_poof.png"));
        } catch (Exception e) {
            System.out.println("Tiara_Object sprite not found: " + e.getMessage());
        }
    }

    @Override
    public void interact(GamePanel gp, int index) {
        gp.player.hasTiara = true;
        gp.object[index]   = null;
        gp.cutsceneManager.startScene(
                main.CutsceneManager.Scene.FLASHBACK_TIARA
        );
    }

    @Override
    public void draw(Graphics2D g2, GamePanel gp) {
        int sx = worldX - gp.tileManager.getCameraX();
        int sy = worldY - gp.tileManager.getCameraY();

        if (sx + gp.tileSize < 0 || sx > gp.screenWidth ||
                sy + gp.tileSize < 0 || sy > gp.screenHeight) return;

        if (image != null)
            g2.drawImage(image, sx, sy, gp.tileSize, gp.tileSize, null);

        if (!poofDone && poofImage != null) {
            poofTimer++;
            float progress = (float) poofTimer / POOF_DURATION;
            float alpha    = Math.max(0f, 1f - progress);
            int   size     = (int)(gp.tileSize + progress * gp.tileSize * 0.4f);
            int   ox       = sx + gp.tileSize / 2 - size / 2;
            int   oy       = sy + gp.tileSize / 2 - size / 2;

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.drawImage(poofImage, ox, oy, size, size, null);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

            if (poofTimer >= POOF_DURATION) poofDone = true;
        }
    }
}
