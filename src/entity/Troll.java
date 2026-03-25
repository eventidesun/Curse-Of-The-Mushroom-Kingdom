package entity;

import main.GamePanel;
import object.Ring_Object;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Troll extends Entity {

    GamePanel gp;

    BufferedImage idle1, idle2;

    private int actionTimer   = 0;
    private int chaseRange    = 6;
    private int attackRange   = 1;
    private int attackTimer   = 0;
    private int attackCooldown = 100; // slightly slower than goblin

    public boolean isLeader   = false;
    private boolean hasDropped = false;

    public Troll(GamePanel gp) {
        this.gp = gp;

        maxHealth    = 10;
        health       = 10;
        speed        = 2;
        attackPower  = 3; // 1.5 hearts — harder than goblin
        invincibleMax = 60;

        // Troll is taller — 16x24 sprite shown as 64x96 on screen
        // solidArea matches just the feet so collision feels right
        solidArea = new Rectangle(8, 32, 48, 32);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;

        direction = "down";
        loadSprites();
    }

    private void loadSprites() {
        // Placeholder: using existing boy sprites
        // Swap to troll_idle_1.png when art is ready
        idle1 = loadImage("/player/boy_down_1.png");
        idle2 = loadImage("/player/boy_down_2.png");
    }

    private BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(getClass().getResourceAsStream(path));
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Troll sprite not found: " + path);
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

        int distX = Math.abs(gp.player.worldX - worldX) / gp.tileSize;
        int distY = Math.abs(gp.player.worldY - worldY) / gp.tileSize;
        int dist  = distX + distY;

        if (dist <= attackRange) {
            if (attackTimer == 0) {
                gp.player.takeDamage(attackPower);
                attackTimer = attackCooldown;
                // Screen shake on troll hit
                gp.screenShake(12);
            }
        } else if (dist <= chaseRange) {
            chasePlayer();
        } else {
            patrol();
        }

        spriteCounter++;
        if (spriteCounter > 18) {
            spriteNum     = (spriteNum == 1) ? 2 : 1;
            spriteCounter = 0;
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
        if (actionTimer >= 150) {
            actionTimer = 0;
            int rand = (int)(Math.random() * 4);
            direction = switch (rand) {
                case 0 -> "up";
                case 1 -> "down";
                case 2 -> "left";
                default -> "right";
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

    public void takeDamage(int amount) {
        if (invincible) return;
        health -= amount;
        invincible = true;
        invincibleTimer = 0;
        gp.playSE(2);
        if (health <= 0) die();
    }

    private void die() {
        alive = false;
        if (!hasDropped && isLeader) {
            hasDropped = true;
            for (int i = 0; i < gp.object.length; i++) {
                if (gp.object[i] == null) {
                    Ring_Object ring = new Ring_Object();
                    ring.worldX = worldX;
                    ring.worldY = worldY;
                    gp.object[i] = ring;
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
                worldX + gp.tileSize > gp.player.worldX - gp.player.screenX &&
                        worldX - gp.tileSize < gp.player.worldX + gp.player.screenX &&
                        worldY + gp.tileSize > gp.player.worldY - gp.player.screenY &&
                        worldY - gp.tileSize < gp.player.worldY + gp.player.screenY;

        if (!onScreen) return;

        if (invincible && invincibleTimer % 10 < 5) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
        }

        // Troll drawn taller: 64x96 (1 tile wide, 1.5 tiles tall)
        int drawW = gp.tileSize;
        int drawH = gp.tileSize + gp.tileSize / 2;

        if (idle1 != null) {
            g2.drawImage(spriteNum == 1 ? idle1 : idle2,
                    screenX, screenY - gp.tileSize / 2, drawW, drawH, null);
        } else {
            g2.setColor(new Color(100, 70, 140));
            g2.fillRect(screenX, screenY - gp.tileSize / 2, drawW, drawH);
            g2.setColor(Color.white);
            g2.setFont(new Font("Courier New", Font.BOLD, 10));
            g2.drawString("T", screenX + 24, screenY + 28);
        }

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        drawHealthBar(g2, screenX, screenY - gp.tileSize / 2 - 10);
    }

    private void drawHealthBar(Graphics2D g2, int x, int y) {
        int barW  = gp.tileSize;
        int fillW = (int)((double) health / maxHealth * barW);
        g2.setColor(new Color(40, 40, 40, 180));
        g2.fillRoundRect(x, y, barW, 6, 3, 3);
        g2.setColor(new Color(180, 100, 220));
        g2.fillRoundRect(x, y, fillW, 6, 3, 3);
        g2.setColor(new Color(255, 255, 255, 60));
        g2.drawRoundRect(x, y, barW, 6, 3, 3);
    }
}
