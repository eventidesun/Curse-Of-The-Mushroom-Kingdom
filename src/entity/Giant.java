package entity;

import main.GamePanel;
import object.Tiara_Object;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Giant extends Entity {

    GamePanel gp;

    BufferedImage idle1, idle2;

    // Heart weak point — only takes damage when attacked from above
    private Rectangle heartArea; // relative to giant's world position
    private boolean heartVisible = true;

    private int actionTimer    = 0;
    private int attackTimer    = 0;
    private int attackCooldown = 120; // 2 seconds between stomps
    private int chaseRange     = 8;

    private boolean hasDropped = false;

    // Stomp knockback
    private int knockbackTimer = 0;

    public Giant(GamePanel gp) {
        this.gp = gp;

        maxHealth    = 20;
        health       = 20;
        speed        = 1; // giant is slow
        attackPower  = 5; // 2.5 hearts per stomp — very dangerous
        invincibleMax = 90;

        // Giant is 32x32 sprite = 128x128 on screen
        solidArea = new Rectangle(16, 16, 96, 96);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;

        // Heart weak point — center of giant's chest area
        heartArea = new Rectangle(48, 20, 32, 32);

        direction = "down";
        loadSprites();
    }

    private void loadSprites() {
        // Placeholder: boy sprite scaled up — swap to giant_idle_1.png
        idle1 = loadImage("/player/boy_down_1.png");
        idle2 = loadImage("/player/boy_down_2.png");
    }

    private BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(getClass().getResourceAsStream(path));
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Giant sprite not found: " + path);
            return null;
        }
    }

    public void update() {
        if (!alive) return;

        if (invincible) {
            invincibleTimer++;
            if (invincibleTimer >= invincibleMax) {
                invincible = false;
                invincibleTimer = 0;
            }
        }

        if (attackTimer > 0) attackTimer--;
        if (knockbackTimer > 0) knockbackTimer--;

        int distX = Math.abs(gp.player.worldX - worldX) / gp.tileSize;
        int distY = Math.abs(gp.player.worldY - worldY) / gp.tileSize;
        int dist  = distX + distY;

        if (dist <= 2) {
            // STOMP — big AoE attack
            if (attackTimer == 0) {
                stomp();
            }
        } else if (dist <= chaseRange) {
            chasePlayer();
        } else {
            patrol();
        }

        spriteCounter++;
        if (spriteCounter > 25) {
            spriteNum = (spriteNum == 1) ? 2 : 1;
            spriteCounter = 0;
        }
    }

    private void stomp() {
        attackTimer = attackCooldown;
        gp.playSE(2); // stomp SFX
        gp.screenShake(20); // big shake

        // Check if player is within stomp radius (3 tiles)
        int distX = Math.abs(gp.player.worldX - worldX);
        int distY = Math.abs(gp.player.worldY - worldY);
        if (distX < gp.tileSize * 3 && distY < gp.tileSize * 3) {
            gp.player.takeDamage(attackPower);
        }
    }

    private void chasePlayer() {
        int px = gp.player.worldX;
        int py = gp.player.worldY;
        if (Math.abs(py - worldY) > Math.abs(px - worldX)) {
            direction = py < worldY ? "up" : "down";
        } else {
            direction = px < worldX ? "left" : "right";
        }
        collisionOn = false;
        gp.collisionChecker.checkTile(this);
        if (!collisionOn) {
            switch (direction) {
                case "up"    -> worldY -= speed;
                case "down"  -> worldY += speed;
                case "left"  -> worldX -= speed;
                case "right" -> worldX += speed;
            }
        }
    }

    private void patrol() {
        actionTimer++;
        if (actionTimer >= 200) {
            actionTimer = 0;
            int rand = (int)(Math.random() * 4);
            direction = switch (rand) {
                case 0 -> "up"; case 1 -> "down";
                case 2 -> "left"; default -> "right";
            };
        }
        collisionOn = false;
        gp.collisionChecker.checkTile(this);
        if (!collisionOn) {
            switch (direction) {
                case "up"    -> worldY -= speed;
                case "down"  -> worldY += speed;
                case "left"  -> worldX -= speed;
                case "right" -> worldX += speed;
            }
        }
    }

    // -----------------------------------------------
    // TAKE DAMAGE — only when hit from above (heart weak point)
    // -----------------------------------------------
    public void takeDamage(int amount, String playerDirection) {
        if (invincible) return;

        // Must be attacking downward AND player must be above the giant
        boolean fromAbove = playerDirection.equals("down") &&
                gp.player.worldY < worldY;

        if (!fromAbove) {
            // Show hint message — player needs to attack heart from above
            gp.ui.showMessage("Aim for the heart! Attack from above!");
            return;
        }

        health -= amount;
        invincible = true;
        invincibleTimer = 0;
        gp.playSE(2);

        if (health <= 0) die();
    }

    private void die() {
        alive = false;
        gp.screenShake(30);
        if (!hasDropped) {
            hasDropped = true;
            for (int i = 0; i < gp.object.length; i++) {
                if (gp.object[i] == null) {
                    Tiara_Object tiara = new Tiara_Object();
                    tiara.worldX = worldX + gp.tileSize / 2;
                    tiara.worldY = worldY + gp.tileSize / 2;
                    gp.object[i] = tiara;
                    break;
                }
            }
        }
    }

    public void draw(Graphics2D g2) {
        if (!alive) return;

        int screenX = worldX - gp.tileManager.getCameraX();
        int screenY = worldY - gp.tileManager.getCameraY();

        boolean onScreen =
                worldX + gp.tileSize * 2 > gp.player.worldX - gp.player.screenX &&
                        worldX - gp.tileSize * 2 < gp.player.worldX + gp.player.screenX &&
                        worldY + gp.tileSize * 2 > gp.player.worldY - gp.player.screenY &&
                        worldY - gp.tileSize * 2 < gp.player.worldY + gp.player.screenY;

        if (!onScreen) return;

        if (invincible && invincibleTimer % 10 < 5) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
        }

        // Giant is 2x2 tiles = 128x128 on screen
        int drawSize = gp.tileSize * 2;

        if (idle1 != null) {
            g2.drawImage(spriteNum == 1 ? idle1 : idle2,
                    screenX, screenY, drawSize, drawSize, null);
        } else {
            g2.setColor(new Color(80, 60, 40));
            g2.fillRect(screenX, screenY, drawSize, drawSize);
            g2.setColor(Color.white);
            g2.setFont(new Font("Courier New", Font.BOLD, 14));
            g2.drawString("GIANT", screenX + 20, screenY + 64);
        }

        // Draw heart weak point on giant's chest — always visible
        int heartScreenX = screenX + heartArea.x;
        int heartScreenY = screenY + heartArea.y;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2.setColor(new Color(220, 50, 50, 200));
        g2.fillOval(heartScreenX, heartScreenY, heartArea.width, heartArea.height);
        g2.setColor(new Color(255, 100, 100));
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(heartScreenX, heartScreenY, heartArea.width, heartArea.height);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        drawHealthBar(g2, screenX, screenY - 14, drawSize);
    }

    private void drawHealthBar(Graphics2D g2, int x, int y, int width) {
        int fillW = (int)((double) health / maxHealth * width);
        g2.setColor(new Color(40, 40, 40, 180));
        g2.fillRoundRect(x, y, width, 8, 4, 4);
        g2.setColor(new Color(220, 60, 60));
        g2.fillRoundRect(x, y, fillW, 8, 4, 4);
        g2.setColor(new Color(255, 255, 255, 60));
        g2.drawRoundRect(x, y, width, 8, 4, 4);
    }
}
