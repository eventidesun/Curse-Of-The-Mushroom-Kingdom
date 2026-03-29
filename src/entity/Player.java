package entity;

import main.GamePanel;
import main.KeyHandler;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Player extends Entity {

    GamePanel gp;
    KeyHandler keyH;

    public final int screenX;
    public final int screenY;

    // -----------------------------------------------
    // HEALTH
    // -----------------------------------------------
    public int health    = 20;
    public int maxHealth = 20;

    // heal
    public void heal(int amount) {
        health = Math.min(health + amount, maxHealth);
    }

    // -----------------------------------------------
    // QUEST ITEMS
    // -----------------------------------------------
    public boolean hasRose     = false;
    public boolean hasRing     = false;
    public boolean hasTiara    = false;
    public boolean hasNecklace = false;

    // -----------------------------------------------
    // SHIELD
    // -----------------------------------------------
    public int shieldLevel        = 1;
    public int shieldStrength     = 10;
    public int maxShieldStrength  = 10;

    // -----------------------------------------------
    // EASTER EGGS
    // -----------------------------------------------
    public int eggsFound = 0;

    // -----------------------------------------------
    // COMBAT
    // -----------------------------------------------
    public boolean attacking   = false;
    public int attackFrame     = 0;
    public int attackCooldown  = 0;
    public int attackPower     = 2;

    public boolean invincible    = false;
    public int invincibleTimer   = 0;
    public int invincibleMax     = 120;

    // ✅ NEW: attack hitbox
    Rectangle attackArea = new Rectangle(0, 0, 36, 36);
    boolean attackChecked = false;

    // -----------------------------------------------
    // SPRITES
    // -----------------------------------------------
    BufferedImage walkDown1,  walkDown2;
    BufferedImage walkUp1,    walkUp2;
    BufferedImage walkLeft1,  walkLeft2;
    BufferedImage walkRight1, walkRight2;

    BufferedImage attackDown1,  attackDown2,  attackDown3;
    BufferedImage attackUp1,    attackUp2,    attackUp3;
    BufferedImage attackLeft1,  attackLeft2,  attackLeft3;
    BufferedImage attackRight1, attackRight2, attackRight3;

    public Player(GamePanel gp, KeyHandler keyH) {
        this.gp   = gp;
        this.keyH = keyH;

        screenX = gp.screenWidth  / 2 - gp.tileSize / 2;
        screenY = gp.screenHeight / 2 - gp.tileSize / 2;

        solidArea = new Rectangle(8, 16, 32, 32);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;

        setDefaultValues();
        loadSprites();
    }

    private void setDefaultValues() {
        worldX = 23 * gp.tileSize;
        worldY = 23 * gp.tileSize;
        speed = 4;
        direction = "down";
    }

    private void loadSprites() {
        try {
            walkDown1  = loadImage("/player/boy_down_1.png");
            walkDown2  = loadImage("/player/boy_down_2.png");
            walkUp1    = loadImage("/player/boy_up_1.png");
            walkUp2    = loadImage("/player/boy_up_2.png");
            walkLeft1  = loadImage("/player/boy_left_1.png");
            walkLeft2  = loadImage("/player/boy_left_2.png");
            walkRight1 = loadImage("/player/boy_right_1.png");
            walkRight2 = loadImage("/player/boy_right_2.png");

            attackDown1  = walkDown1;  attackDown2  = walkDown2;  attackDown3  = walkDown1;
            attackUp1    = walkUp1;    attackUp2    = walkUp2;    attackUp3    = walkUp1;
            attackLeft1  = walkLeft1;  attackLeft2  = walkLeft2;  attackLeft3  = walkLeft1;
            attackRight1 = walkRight1; attackRight2 = walkRight2; attackRight3 = walkRight1;

        } catch (Exception e) {
            System.out.println("Player sprite not found: " + e.getMessage());
        }
    }

    private BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(getClass().getResourceAsStream(path));
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Missing sprite: " + path);
            return null;
        }
    }

    public void update() {

        if (keyH.attackPressed && !attacking && attackCooldown == 0) {
            attacking     = true;
            attackFrame   = 0;
            attackCooldown = 30;
        }

        if (attackCooldown > 0) attackCooldown--;

        if (invincible) {
            invincibleTimer++;
            if (invincibleTimer >= invincibleMax) {
                invincible = false;
                invincibleTimer = 0;
            }
        }

        if (attacking) {

            attackFrame++;

            if (attackFrame == 15 && !attackChecked) {
                checkAttackHit();
                attackChecked = true;
            }

            if (attackFrame >= 30) {
                attacking = false;
                attackFrame = 0;
                attackChecked = false;
            }

            return;
        }

        boolean moving = keyH.upPressed || keyH.downPressed ||
                keyH.leftPressed || keyH.rightPressed;

        if (moving) {
            if (keyH.upPressed)    direction = "up";
            if (keyH.downPressed)  direction = "down";
            if (keyH.leftPressed)  direction = "left";
            if (keyH.rightPressed) direction = "right";

            collisionOn = false;
            gp.collisionChecker.checkTile(this);

            int objIndex = gp.collisionChecker.checkObject(this, true);
            interactObject(objIndex);

            if (!collisionOn) {
                switch (direction) {
                    case "up"    -> worldY -= speed;
                    case "down"  -> worldY += speed;
                    case "left"  -> worldX -= speed;
                    case "right" -> worldX += speed;
                }
            }

            spriteCounter++;
            if (spriteCounter > 10) {
                spriteNum = (spriteNum == 1) ? 2 : 1;
                spriteCounter = 0;
            }
        }
    }

    private void checkAttackHit() {

        Rectangle attackBox = new Rectangle();

        switch (direction) {
            case "up" -> {
                attackBox.x = worldX;
                attackBox.y = worldY - gp.tileSize;
            }
            case "down" -> {
                attackBox.x = worldX;
                attackBox.y = worldY + gp.tileSize;
            }
            case "left" -> {
                attackBox.x = worldX - gp.tileSize;
                attackBox.y = worldY;
            }
            case "right" -> {
                attackBox.x = worldX + gp.tileSize;
                attackBox.y = worldY;
            }
        }

        attackBox.width  = gp.tileSize;
        attackBox.height = gp.tileSize;

        for (Troll t : gp.trolls) {
            if (t == null || !t.alive) continue;

            Rectangle enemyBox = new Rectangle(
                    t.worldX + t.solidArea.x,
                    t.worldY + t.solidArea.y,
                    t.solidArea.width,
                    t.solidArea.height
            );

            if (attackBox.intersects(enemyBox)) {
                t.takeDamage(attackPower);
                gp.playSE(1);
            }
        }

        for (Goblin g : gp.goblins) {
            if (g == null || !g.alive) continue;

            Rectangle enemyBox = new Rectangle(
                    g.worldX + g.solidArea.x,
                    g.worldY + g.solidArea.y,
                    g.solidArea.width,
                    g.solidArea.height
            );

            if (attackBox.intersects(enemyBox)) {
                g.takeDamage(attackPower);
                gp.playSE(1);
            }
        }

        if (gp.giant != null && gp.giant.alive) {

            Rectangle enemyBox = new Rectangle(
                    gp.giant.worldX + gp.giant.solidArea.x,
                    gp.giant.worldY + gp.giant.solidArea.y,
                    gp.giant.solidArea.width,
                    gp.giant.solidArea.height
            );

            if (attackBox.intersects(enemyBox)) {
                gp.giant.takeDamage(attackPower, direction);
                gp.playSE(1);
            }
        }

        if (gp.dragon != null && gp.dragon.alive) {

            Rectangle enemyBox = new Rectangle(
                    gp.dragon.worldX + gp.dragon.solidArea.x,
                    gp.dragon.worldY + gp.dragon.solidArea.y,
                    gp.dragon.solidArea.width,
                    gp.dragon.solidArea.height
            );

            if (attackBox.intersects(enemyBox)) {
                gp.dragon.takeDamage(attackPower);
                gp.playSE(1);
            }
        }
    }

    private void interactObject(int index) {
        if (index != 999) {
            if (gp.keyH.interactPressed) {
                gp.keyH.interactPressed = false;
                gp.object[index].interact(gp, index);
            } else if (!gp.object[index].collision) {
                gp.object[index].interact(gp, index);
            }
        }
    }

    public void takeDamage(int amount) {
        if (invincible) return;

        int shieldAbsorb = shieldLevel;
        int actualDamage = Math.max(0, amount - shieldAbsorb);

        if (shieldStrength > 0) {
            shieldStrength -= 1;
            actualDamage = Math.max(0, actualDamage - 1);
        }

        health -= actualDamage;
        if (health < 0) health = 0;

        invincible = true;
        invincibleTimer = 0;

        gp.playSE(3);

        if (health == 0) {
            System.out.println("King has fallen.");
        }
    }

    public void draw(Graphics2D g2) {
        BufferedImage image = getCurrentFrame();

        if (invincible && invincibleTimer % 10 < 5) return;

        int drawX = worldX - gp.tileManager.getCameraX();
        int drawY = worldY - gp.tileManager.getCameraY();

        if (image != null) {
            g2.drawImage(image, drawX, drawY, gp.tileSize, gp.tileSize, null);
        }
    }

    private BufferedImage getCurrentFrame() {
        if (attacking) {
            int f = attackFrame;
            return switch (direction) {
                case "down"  -> f < 10 ? attackDown1  : f < 20 ? attackDown2  : attackDown3;
                case "up"    -> f < 10 ? attackUp1    : f < 20 ? attackUp2    : attackUp3;
                case "left"  -> f < 10 ? attackLeft1  : f < 20 ? attackLeft2  : attackLeft3;
                case "right" -> f < 10 ? attackRight1 : f < 20 ? attackRight2 : attackRight3;
                default      -> walkDown1;
            };
        }

        return switch (direction) {
            case "down"  -> spriteNum == 1 ? walkDown1  : walkDown2;
            case "up"    -> spriteNum == 1 ? walkUp1    : walkUp2;
            case "left"  -> spriteNum == 1 ? walkLeft1  : walkLeft2;
            case "right" -> spriteNum == 1 ? walkRight1 : walkRight2;
            default      -> walkDown1;
        };
    }
}
