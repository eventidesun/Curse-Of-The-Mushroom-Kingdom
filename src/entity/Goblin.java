package entity;

import main.GamePanel;
import object.Rose_Object;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Goblin extends Entity {

    GamePanel gp;

    // Sprites — placeholder uses boy sprites mirrored
    // Swap to goblin_idle_1.png etc when art is ready
    BufferedImage idle1, idle2;
    BufferedImage attack1, attack2;

    // AI
    private int actionTimer    = 0;   // counts up, triggers direction change
    private int chaseRange     = 5;   // tiles — how close before goblin chases
    private int attackRange    = 1;   // tiles — how close before goblin attacks
    private int attackTimer    = 0;   // cooldown between attacks
    private int attackCooldown = 90;  // 1.5 seconds between hits

    // Drop
    private boolean hasDropped = false;
    public boolean isLeader    = false; // only the leader goblin drops the rose

    public Goblin(GamePanel gp) {
        this.gp = gp;

        maxHealth    = 6;
        health       = 6;
        speed        = 2;  // slower than king (4)
        attackPower  = 2;  // 1 heart damage
        invincibleMax = 60;

        solidArea = new Rectangle(8, 16, 32, 32);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;

        direction = "down";

        loadSprites();
    }

    private void loadSprites() {
        // Using existing boy sprites as placeholder
        // Replace with goblin sprites when Aseprite art is done
        idle1   = loadImage("/player/boy_down_1.png");
        idle2   = loadImage("/player/boy_down_2.png");
        attack1 = loadImage("/player/boy_left_1.png");
        attack2 = loadImage("/player/boy_left_2.png");
    }

    private BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(getClass().getResourceAsStream(path));
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Goblin sprite not found: " + path);
            return null;
        }
    }

    // -----------------------------------------------
    // UPDATE
    // -----------------------------------------------
    public void update() {
        if (!alive) return;

        // Tick invincibility
        if (invincible) {
            invincibleTimer++;
            if (invincibleTimer >= invincibleMax) {
                invincible      = false;
                invincibleTimer = 0;
            }
        }

        // Tick attack cooldown
        if (attackTimer > 0) attackTimer--;

        // Distance to player (in tiles)
        int distX = Math.abs(gp.player.worldX - worldX) / gp.tileSize;
        int distY = Math.abs(gp.player.worldY - worldY) / gp.tileSize;
        int dist  = distX + distY;

        if (dist <= attackRange) {
            // ATTACK — close enough to hit player
            if (attackTimer == 0) {
                gp.player.takeDamage(attackPower);
                attackTimer = attackCooldown;
            }
        } else if (dist <= chaseRange) {
            // CHASE — move toward player
            chasePlayer();
        } else {
            // PATROL — wander randomly
            patrol();
        }

        // Animate sprite
        spriteCounter++;
        if (spriteCounter > 15) {
            spriteNum     = (spriteNum == 1) ? 2 : 1;
            spriteCounter = 0;
        }
    }

    private void chasePlayer() {
        // Face and move toward king
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
        if (actionTimer >= 120) { // change direction every 2 seconds
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

    // -----------------------------------------------
    // TAKE DAMAGE — called from Player attack check
    // -----------------------------------------------
    public void takeDamage(int amount) {
        if (invincible) return;

        health -= amount;
        invincible      = true;
        invincibleTimer = 0;

        gp.playSE(2); // hitmonster.wav — already in your sounds!

        if (health <= 0) {
            die();
        }
    }

    private void die() {
        alive = false;
        gp.playSE(2);

        if (!hasDropped && isLeader) {
            hasDropped = true;
            dropRose();
        }
    }

    private void dropRose() {
        // Find an empty object slot and place the rose at goblin's position
        for (int i = 0; i < gp.object.length; i++) {
            if (gp.object[i] == null) {
                Rose_Object rose = new Rose_Object();
                rose.worldX = worldX;
                rose.worldY = worldY;
                gp.object[i] = rose;
                break;
            }
        }
    }

    // -----------------------------------------------
    // DRAW
    // -----------------------------------------------
    public void draw(Graphics2D g2) {
        if (!alive) return;

        int screenX = worldX - gp.tileManager.getCameraX();
        int screenY = worldY - gp.tileManager.getCameraY();

        // Only draw if on screen
        if (worldX + gp.tileSize > gp.player.worldX - gp.player.screenX &&
                worldX - gp.tileSize < gp.player.worldX + gp.player.screenX &&
                worldY + gp.tileSize > gp.player.worldY - gp.player.screenY &&
                worldY - gp.tileSize < gp.player.worldY + gp.player.screenY) {

            // Flash white when invincible
            if (invincible && invincibleTimer % 10 < 5) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            }

            BufferedImage frame = spriteNum == 1 ? idle1 : idle2;

            if (frame != null) {
                g2.drawImage(frame, screenX, screenY, gp.tileSize, gp.tileSize, null);
            } else {
                // Placeholder — green square for goblin
                g2.setColor(new Color(80, 160, 60));
                g2.fillRect(screenX, screenY, gp.tileSize, gp.tileSize);
                g2.setColor(Color.white);
                g2.setFont(new Font("Courier New", Font.BOLD, 10));
                g2.drawString("G", screenX + 20, screenY + 28);
            }

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

            // Health bar above goblin
            drawHealthBar(g2, screenX, screenY);
        }
    }

    private void drawHealthBar(Graphics2D g2, int screenX, int screenY) {
        int barW  = gp.tileSize;
        int barH  = 6;
        int barY  = screenY - 10;
        int fillW = (int)((double) health / maxHealth * barW);

        g2.setColor(new Color(40, 40, 40, 180));
        g2.fillRoundRect(screenX, barY, barW, barH, 3, 3);

        g2.setColor(new Color(80, 200, 80));
        g2.fillRoundRect(screenX, barY, fillW, barH, 3, 3);

        g2.setColor(new Color(255, 255, 255, 60));
        g2.drawRoundRect(screenX, barY, barW, barH, 3, 3);
    }
}
