package entity;

import main.GamePanel;
import object.Necklace_Object;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Dragon extends Entity {

    GamePanel gp;

    BufferedImage idle1, idle2, attack1, attack2;

    // Attack pattern phases
    public enum Phase { IDLE, FIRE_BREATH, TAIL_SWIPE, CHARGE }
    private Phase currentPhase  = Phase.IDLE;
    private int phaseTimer       = 0;
    private int phaseLength      = 180; // 3 seconds per phase
    private int attackCooldown   = 0;

    // Fire breath projectile
    private int fireX, fireY;
    private boolean fireActive   = false;
    private int fireDirection    = 1; // 1 = right, -1 = left

    // Charge
    private boolean charging     = false;
    private int chargeTimer      = 0;

    private boolean hasDropped   = false;
    public boolean fightStarted  = false;

    public Dragon(GamePanel gp) {
        this.gp = gp;

        maxHealth    = 30;
        health       = 30;
        speed        = 2;
        attackPower  = 4;
        invincibleMax = 90;

        // Dragon is 3x3 tiles = 192x192 on screen
        solidArea = new Rectangle(24, 24, 144, 144);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;

        direction = "down";
        loadSprites();
    }

    private void loadSprites() {
        // Placeholder: scaled up boy sprites — swap to dragon sprites when art ready
        idle1   = loadImage("/player/boy_down_1.png");
        idle2   = loadImage("/player/boy_down_2.png");
        attack1 = loadImage("/player/boy_left_1.png");
        attack2 = loadImage("/player/boy_left_2.png");
    }

    private BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(getClass().getResourceAsStream(path));
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }

    public void update() {
        if (!alive || !fightStarted) return;

        if (invincible) {
            invincibleTimer++;
            if (invincibleTimer >= invincibleMax) {
                invincible = false;
                invincibleTimer = 0;
            }
        }

        if (attackCooldown > 0) attackCooldown--;

        phaseTimer++;

        // Switch phases every phaseLength frames
        if (phaseTimer >= phaseLength) {
            phaseTimer = 0;
            pickNextPhase();
        }

        switch (currentPhase) {
            case IDLE        -> facePlayer();
            case FIRE_BREATH -> updateFireBreath();
            case TAIL_SWIPE  -> updateTailSwipe();
            case CHARGE      -> updateCharge();
        }

        // Update fire projectile
        if (fireActive) updateFireProjectile();

        // Animate
        spriteCounter++;
        if (spriteCounter > 20) {
            spriteNum = (spriteNum == 1) ? 2 : 1;
            spriteCounter = 0;
        }
    }

    private void pickNextPhase() {
        // Cycle through phases — gets more aggressive at low health
        double healthPercent = (double) health / maxHealth;
        int roll = (int)(Math.random() * 3);

        if (healthPercent < 0.3) {
            // Under 30% health — mostly fire and charge
            currentPhase = roll == 0 ? Phase.FIRE_BREATH : Phase.CHARGE;
            phaseLength  = 120; // faster
        } else {
            currentPhase = switch (roll) {
                case 0  -> Phase.FIRE_BREATH;
                case 1  -> Phase.TAIL_SWIPE;
                default -> Phase.CHARGE;
            };
            phaseLength = 180;
        }
    }

    private void facePlayer() {
        int px = gp.player.worldX;
        int py = gp.player.worldY;
        direction = Math.abs(py - worldY) > Math.abs(px - worldX)
                ? (py < worldY ? "up" : "down")
                : (px < worldX ? "left" : "right");
    }

    private void updateFireBreath() {
        if (attackCooldown == 0) {
            // Launch fire projectile toward player
            fireX         = worldX + gp.tileSize;
            fireY         = worldY + gp.tileSize;
            fireActive    = true;
            fireDirection = gp.player.worldX > worldX ? 1 : -1;
            attackCooldown = 60;
            gp.playSE(2); // fire SFX placeholder
        }
    }

    private void updateTailSwipe() {
        // AoE around dragon — damages player if close
        if (attackCooldown == 0) {
            int distX = Math.abs(gp.player.worldX - worldX);
            int distY = Math.abs(gp.player.worldY - worldY);
            if (distX < gp.tileSize * 3 && distY < gp.tileSize * 3) {
                gp.player.takeDamage(attackPower - 1);
                gp.screenShake(15);
            }
            attackCooldown = 90;
        }
    }

    private void updateCharge() {
        if (!charging && attackCooldown == 0) {
            charging    = true;
            chargeTimer = 60; // charge lasts 1 second
            facePlayer();
        }

        if (charging) {
            chargeTimer--;
            // Move fast toward player
            int chargeSpeed = 5;
            collisionOn = false;
            gp.collisionChecker.checkTile(this);
            if (!collisionOn) {
                switch (direction) {
                    case "up"    -> worldY -= chargeSpeed;
                    case "down"  -> worldY += chargeSpeed;
                    case "left"  -> worldX -= chargeSpeed;
                    case "right" -> worldX += chargeSpeed;
                }
            }

            // Check if hit player during charge
            int distX = Math.abs(gp.player.worldX - worldX);
            int distY = Math.abs(gp.player.worldY - worldY);
            if (distX < gp.tileSize && distY < gp.tileSize) {
                gp.player.takeDamage(attackPower);
                gp.screenShake(20);
            }

            if (chargeTimer <= 0) {
                charging = false;
                attackCooldown = 90;
            }
        }
    }

    private void updateFireProjectile() {
        fireX += fireDirection * 6; // fire moves fast

        // Check if fire hit player
        int distX = Math.abs(gp.player.worldX - fireX);
        int distY = Math.abs(gp.player.worldY - fireY);
        if (distX < gp.tileSize && distY < gp.tileSize) {
            gp.player.takeDamage(3);
            fireActive = false;
        }

        // Fire goes off screen — deactivate
        if (fireX < 0 || fireX > gp.maxWorldCol * gp.tileSize) {
            fireActive = false;
        }
    }

    public void takeDamage(int amount) {
        if (invincible || !fightStarted) return;
        health -= amount;
        invincible = true;
        invincibleTimer = 0;
        gp.playSE(2);
        if (health <= 0) die();
    }

    private void die() {
        alive = false;
        gp.screenShake(40);
        gp.stopMusic();
        gp.playSE(2);

        if (!hasDropped) {
            hasDropped = true;
            for (int i = 0; i < gp.object.length; i++) {
                if (gp.object[i] == null) {
                    Necklace_Object necklace = new Necklace_Object();
                    necklace.worldX = worldX + gp.tileSize;
                    necklace.worldY = worldY + gp.tileSize;
                    gp.object[i] = necklace;
                    break;
                }
            }
        }

        // After short delay, transition to orb scene
        // This is handled by GamePanel checking dragon.alive
    }

    public void draw(Graphics2D g2) {
        if (!alive) return;

        int screenX = worldX - gp.tileManager.getCameraX();
        int screenY = worldY - gp.tileManager.getCameraY();

        boolean onScreen =
                worldX + gp.tileSize * 3 > gp.player.worldX - gp.player.screenX &&
                        worldX - gp.tileSize * 3 < gp.player.worldX + gp.player.screenX &&
                        worldY + gp.tileSize * 3 > gp.player.worldY - gp.player.screenY &&
                        worldY - gp.tileSize * 3 < gp.player.worldY + gp.player.screenY;

        if (!onScreen) return;

        if (invincible && invincibleTimer % 10 < 5) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
        }

        // Dragon is 3x3 tiles = 192x192
        int drawSize = gp.tileSize * 3;

        BufferedImage frame = (currentPhase == Phase.FIRE_BREATH || currentPhase == Phase.CHARGE)
                ? (spriteNum == 1 ? attack1 : attack2)
                : (spriteNum == 1 ? idle1   : idle2);

        if (frame != null) {
            g2.drawImage(frame, screenX, screenY, drawSize, drawSize, null);
        } else {
            // Placeholder — dark red rectangle
            g2.setColor(new Color(160, 30, 30));
            g2.fillRect(screenX, screenY, drawSize, drawSize);
            g2.setColor(new Color(220, 80, 80));
            g2.setStroke(new BasicStroke(3));
            g2.drawRect(screenX, screenY, drawSize, drawSize);
            g2.setColor(Color.white);
            g2.setFont(new Font("Courier New", Font.BOLD, 18));
            g2.drawString("DRAGON", screenX + 30, screenY + drawSize / 2);
        }

        // Draw necklace on dragon's neck area
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2.setColor(new Color(255, 215, 0, 200)); // gold
        g2.fillOval(screenX + drawSize / 2 - 8, screenY + 30, 16, 16);

        // Fire projectile
        if (fireActive) {
            int fx = fireX - gp.player.worldX + gp.player.screenX;
            int fy = fireY - gp.player.worldY + gp.player.screenY;
            g2.setColor(new Color(255, 120, 20, 220));
            g2.fillOval(fx, fy, 24, 24);
            g2.setColor(new Color(255, 200, 60, 180));
            g2.fillOval(fx + 4, fy + 4, 16, 16);
        }

        // Health bar — big and prominent for boss
        drawHealthBar(g2, screenX, screenY - 18, drawSize);

        // Phase indicator (debug — remove before submission)
        g2.setColor(Color.white);
        g2.setFont(new Font("Courier New", Font.PLAIN, 10));
        g2.drawString(currentPhase.toString(), screenX, screenY - 22);
    }

    private void drawHealthBar(Graphics2D g2, int x, int y, int width) {
        int fillW = (int)((double) health / maxHealth * width);
        g2.setColor(new Color(40, 40, 40, 200));
        g2.fillRoundRect(x, y, width, 10, 5, 5);
        // Health bar color shifts red as dragon gets lower
        float healthPct = (float) health / maxHealth;
        g2.setColor(new Color(1f - healthPct, healthPct * 0.3f, 0f));
        g2.fillRoundRect(x, y, fillW, 10, 5, 5);
        g2.setColor(new Color(255, 255, 255, 80));
        g2.drawRoundRect(x, y, width, 10, 5, 5);
    }
}
