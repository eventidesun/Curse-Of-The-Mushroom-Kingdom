package entity;

import main.GamePanel;
import object.Rose_Object;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Goblin extends Entity {

    GamePanel gp;

    BufferedImage idle1, idle2;
    BufferedImage attack1, attack2, attack3;

    private int actionTimer = 0;
    private int chaseRange = 5;
    private int attackRange = 1;
    private int attackTimer = 0;
    private int attackCooldown = 90;

    private boolean attacking = false;
    private int attackFrame = 0;
    private int attackAnimTimer = 0;

    private boolean hasDropped = false;
    public boolean isLeader = false;

    public Goblin(GamePanel gp) {
        this.gp = gp;

        maxHealth = 6;
        health = maxHealth;
        speed = 2;
        attackPower = 2;
        invincibleMax = 60;

        solidArea = new Rectangle(8, 16, 32, 32);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;

        direction = "down";

        loadSprites();
    }

    private void loadSprites() {
        idle1 = loadImage("/player/boy_down_1.png");
        idle2 = loadImage("/player/boy_down_2.png");

        // placeholder attack sprites for now
        attack1 = loadImage("/player/boy_left_1.png");
        attack2 = loadImage("/player/boy_left_2.png");
        attack3 = loadImage("/player/boy_left_1.png");
    }

    private BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(getClass().getResourceAsStream(path));
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Goblin sprite not found: " + path);
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
        int dist = distX + distY;

        if (attacking) {
            runAttackAnimation();
        } else if (dist <= attackRange) {
            startAttack();
        } else if (dist <= chaseRange) {
            chasePlayer();
        } else {
            patrol();
        }

        spriteCounter++;
        if (spriteCounter > 15) {
            spriteNum = (spriteNum == 1) ? 2 : 1;
            spriteCounter = 0;
        }
    }

    private void startAttack() {
        if (attackTimer == 0) {
            attacking = true;
            attackFrame = 0;
            attackAnimTimer = 0;
        }
    }

    private void runAttackAnimation() {
        attackAnimTimer++;

        if (attackAnimTimer > 8) {
            attackFrame++;
            attackAnimTimer = 0;
        }

        // hit frame
        if (attackFrame == 1) {
            gp.player.takeDamage(attackPower);
            gp.playSE(2);
        }

        if (attackFrame > 2) {
            attacking = false;
            attackTimer = attackCooldown;
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

        move();
    }

    private void patrol() {
        actionTimer++;
        if (actionTimer >= 120) {
            actionTimer = 0;
            int rand = (int)(Math.random() * 4);
            direction = switch (rand) {
                case 0 -> "up";
                case 1 -> "down";
                case 2 -> "left";
                default -> "right";
            };
        }

        move();
    }

    private void move() {
        collisionOn = false;
        gp.collisionChecker.checkTile(this);

        if (!collisionOn) {
            switch (direction) {
                case "up" -> worldY -= speed;
                case "down" -> worldY += speed;
                case "left" -> worldX -= speed;
                case "right" -> worldX += speed;
            }
        }
    }

    @Override
    public void takeDamage(int amount) {
        if (invincible || !alive) return;

        health -= amount;
        invincible = true;
        invincibleTimer = 0;

        gp.playSE(2);

        if (health <= 0) {
            health = 0;
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

        BufferedImage frame;
        if (attacking) {
            frame = switch (attackFrame) {
                case 0 -> attack1;
                case 1 -> attack2;
                default -> attack3;
            };
        } else {
            frame = (spriteNum == 1) ? idle1 : idle2;
        }

        if (frame != null) {
            g2.drawImage(frame, screenX, screenY, gp.tileSize, gp.tileSize, null);
        } else {
            g2.setColor(new Color(80, 160, 60));
            g2.fillRect(screenX, screenY, gp.tileSize, gp.tileSize);
            g2.setColor(Color.white);
            g2.setFont(new Font("Courier New", Font.BOLD, 10));
            g2.drawString("G", screenX + 20, screenY + 28);
        }

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        drawHealthBar(g2, screenX, screenY);
    }

    private void drawHealthBar(Graphics2D g2, int screenX, int screenY) {
        int barW = gp.tileSize;
        int barH = 6;
        int barY = screenY - 10;
        int fillW = (int)((double) health / maxHealth * barW);

        g2.setColor(new Color(40, 40, 40, 180));
        g2.fillRoundRect(screenX, barY, barW, barH, 3, 3);

        g2.setColor(new Color(80, 200, 80));
        g2.fillRoundRect(screenX, barY, fillW, barH, 3, 3);

        g2.setColor(new Color(255, 255, 255, 60));
        g2.drawRoundRect(screenX, barY, barW, barH, 3, 3);
    }
}
