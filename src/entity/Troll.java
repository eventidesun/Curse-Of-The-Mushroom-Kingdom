package entity;

import main.GamePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Troll extends Entity {

    GamePanel gp;

    // Movement
    int actionLockCounter = 0;

    // Attack system
    boolean attacking = false;
    int attackFrame = 0;
    int attackAnimTimer = 0;
    int attackCooldown = 60;
    int attackTimer = 0;
    public boolean isLeader = false;

    // Sprites
    BufferedImage idle1, idle2;
    BufferedImage attack1, attack2, attack3;

    public Troll(GamePanel gp) {
        this.gp = gp;

        speed = 1;

        maxHealth = 6;
        health = maxHealth;
        attackPower = 1;

        direction = "down";

        getImage();
    }

    public void getImage() {
        try {
            idle1 = ImageIO.read(getClass().getResourceAsStream("/enemies/troll1.png"));
            idle2 = ImageIO.read(getClass().getResourceAsStream("/enemies/troll1.png"));

            attack1 = ImageIO.read(getClass().getResourceAsStream("/enemies/troll1.png"));
            attack2 = ImageIO.read(getClass().getResourceAsStream("/enemies/troll2.png"));
            attack3 = ImageIO.read(getClass().getResourceAsStream("/enemies/troll3.png"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void update() {

        if (!alive) return;

        // Invincibility frames
        if (invincible) {
            invincibleTimer++;
            if (invincibleTimer > invincibleMax) {
                invincible = false;
                invincibleTimer = 0;
            }
        }

        // Attack cooldown
        if (attackTimer > 0) attackTimer--;

        // Distance to player
        int dx = Math.abs(worldX - gp.player.worldX);
        int dy = Math.abs(worldY - gp.player.worldY);

        boolean nearPlayer = dx < 80 && dy < 80;

        if (nearPlayer) {
            attackPlayer();
        } else {
            moveRandomly();
        }

        // Idle animation
        spriteCounter++;
        if (spriteCounter > 20) {
            spriteNum = (spriteNum == 1) ? 2 : 1;
            spriteCounter = 0;
        }
    }

    private void moveRandomly() {

        actionLockCounter++;

        if (actionLockCounter > 120) {
            int i = (int)(Math.random() * 4);

            switch (i) {
                case 0 -> direction = "up";
                case 1 -> direction = "down";
                case 2 -> direction = "left";
                case 3 -> direction = "right";
            }

            actionLockCounter = 0;
        }

        switch (direction) {
            case "up"    -> worldY -= speed;
            case "down"  -> worldY += speed;
            case "left"  -> worldX -= speed;
            case "right" -> worldX += speed;
        }
    }

    private void attackPlayer() {

        if (!attacking && attackTimer == 0) {
            attacking = true;
            attackFrame = 0;
            attackAnimTimer = 0;
        }

        if (attacking) {
            attackAnimTimer++;

            if (attackAnimTimer > 8) {
                attackFrame++;
                attackAnimTimer = 0;
            }

            // DAMAGE FRAME
            if (attackFrame == 1) {
                gp.player.takeDamage(attackPower);
                gp.playSE(2);
            }

            if (attackFrame > 2) {
                attacking = false;
                attackTimer = attackCooldown;
            }
        }
    }

    public void takeDamage(int damage) {

        if (invincible) return;

        health -= damage;
        invincible = true;

        gp.playSE(1); // hit sound

        if (health <= 0) {
            alive = false;
            dropItem();
        }
    }

    private void dropItem() {
        // You will connect this to your DropItem system later

        System.out.println("Troll dropped item at: " + worldX + ", " + worldY);

        // Example:
        // gp.spawnDrop(worldX, worldY);
    }

    public void draw(Graphics2D g2) {

        if (!alive) return;

        int screenX = worldX - gp.player.worldX + gp.player.screenX;
        int screenY = worldY - gp.player.worldY + gp.player.screenY;

        BufferedImage imageToDraw;

        // Choose animation
        if (attacking) {
            imageToDraw = switch (attackFrame) {
                case 0 -> attack1;
                case 1 -> attack2;
                default -> attack3;
            };
        } else {
            imageToDraw = (spriteNum == 1) ? idle1 : idle2;
        }

        // Flash effect when hit
        if (invincible) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        }

        g2.drawImage(imageToDraw, screenX, screenY, gp.tileSize, gp.tileSize, null);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        int barWidth = gp.tileSize;
        int barHeight = 6;

        int hpBar = (int)((double)health / maxHealth * barWidth);

        g2.setColor(Color.red);
        g2.fillRect(screenX, screenY - 10, barWidth, barHeight);

        g2.setColor(Color.green);
        g2.fillRect(screenX, screenY - 10, hpBar, barHeight);
    }
}
