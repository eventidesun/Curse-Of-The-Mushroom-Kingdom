package entity;

import main.GamePanel;
import main.KeyHandler;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Player extends Entity {

    GamePanel  gp;
    KeyHandler keyH;

    // Screen-centre position — never changes
    public final int screenX;
    public final int screenY;

    // Player overrides Entity health with larger values
    // (no duplicate fields — we use Entity's health/maxHealth directly)

    // Quest items
    public boolean hasRose     = false;
    public boolean hasRing     = false;
    public boolean hasTiara    = false;
    public boolean hasNecklace = false;

    // Shield
    public int shieldLevel       = 1;
    public int shieldStrength    = 10;
    public int maxShieldStrength = 10;

    // Easter eggs / diamonds
    public int eggsFound = 0;

    // Potions
    public int potionsHeld    = 0;
    private boolean potionConsumed = false; // one-shot guard per keypress

    // Attack state
    public boolean attacking     = false;
    public int     attackFrame   = 0;
    public int     attackCooldown = 0;
    private boolean attackChecked = false;

    // Walk sprites
    private BufferedImage walkDown1,   walkDown2;
    private BufferedImage walkUp1,     walkUp2;
    private BufferedImage walkLeft1,   walkLeft2;
    private BufferedImage walkRight1,  walkRight2;

    // Attack sprites (use walk frames until real attack art is ready)
    private BufferedImage attackDown1,  attackDown2,  attackDown3;
    private BufferedImage attackUp1,    attackUp2,    attackUp3;
    private BufferedImage attackLeft1,  attackLeft2,  attackLeft3;
    private BufferedImage attackRight1, attackRight2, attackRight3;

    public Player(GamePanel gp, KeyHandler keyH) {
        this.gp   = gp;
        this.keyH = keyH;

        screenX = gp.screenWidth  / 2 - gp.tileSize / 2;
        screenY = gp.screenHeight / 2 - gp.tileSize / 2;

        solidArea = new Rectangle(8, 16, 32, 32);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;

        // Larger health pool than base Entity
        maxHealth = 20;
        health    = maxHealth;

        invincibleMax = 120; // 2 seconds

        speed     = 4;
        attackPower = 2;
        direction = "down";

        worldX = 59 * gp.tileSize;
        worldY = 22 * gp.tileSize;

        loadSprites();
    }

    public void heal(int amount) {
        health = Math.min(health + amount, maxHealth);
    }

    private void loadSprites() {
        walkDown1  = load("/player/boy_down_1.png");
        walkDown2  = load("/player/boy_down_2.png");
        walkUp1    = load("/player/boy_up_1.png");
        walkUp2    = load("/player/boy_up_2.png");
        walkLeft1  = load("/player/boy_left_1.png");
        walkLeft2  = load("/player/boy_left_2.png");
        walkRight1 = load("/player/boy_right_1.png");
        walkRight2 = load("/player/boy_right_2.png");

        // Reuse walk frames until dedicated attack sprites are drawn
        attackDown1  = walkDown1;  attackDown2  = walkDown2;  attackDown3  = walkDown1;
        attackUp1    = walkUp1;    attackUp2    = walkUp2;    attackUp3    = walkUp1;
        attackLeft1  = walkLeft1;  attackLeft2  = walkLeft2;  attackLeft3  = walkLeft1;
        attackRight1 = walkRight1; attackRight2 = walkRight2; attackRight3 = walkRight1;
    }

    private BufferedImage load(String path) {
        try { return ImageIO.read(getClass().getResourceAsStream(path)); }
        catch (Exception e) { System.out.println("Missing player sprite: " + path); return null; }
    }

    // -----------------------------------------------
    // UPDATE
    // -----------------------------------------------
    public void update() {

        // Start a new attack swing
        if (keyH.attackPressed && !attacking && attackCooldown == 0) {
            attacking      = true;
            attackFrame    = 0;
            attackCooldown = 30;
            attackChecked  = false;
            keyH.attackPressed = false;
        }

        if (attackCooldown > 0) attackCooldown--;

        // Potion use — P key, heals 4 hearts (2 full hearts)
        if (keyH.usePotion && !potionConsumed) {
            potionConsumed = true;
            if (potionsHeld > 0 && health < maxHealth) {
                potionsHeld--;
                heal(4);
                gp.playSE(1);
                gp.ui.showMessage("Potion used! +" + 2 + " hearts. (" + potionsHeld + " left)");
            } else if (potionsHeld == 0) {
                gp.ui.showMessage("No potions!");
            } else {
                gp.ui.showMessage("Already at full health!");
            }
        }
        if (!keyH.usePotion) potionConsumed = false; // reset when key released

        // Invincibility tick
        updateInvincibility();

        // Attacking — no movement during swing
        if (attacking) {
            attackFrame++;
            if (attackFrame == 15 && !attackChecked) {
                checkAttackHit();
                attackChecked = true;
            }
            if (attackFrame >= 30) { attacking = false; attackFrame = 0; }
            return;
        }

        // Movement
        boolean moving = keyH.upPressed || keyH.downPressed
                || keyH.leftPressed || keyH.rightPressed;

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
            if (spriteCounter > 10) { spriteNum = (spriteNum == 1) ? 2 : 1; spriteCounter = 0; }
        }
    }

    // -----------------------------------------------
    // ATTACK HIT DETECTION
    // -----------------------------------------------
    private void checkAttackHit() {
        // One-tile box in front of player
        Rectangle attackBox = new Rectangle();
        attackBox.width  = gp.tileSize;
        attackBox.height = gp.tileSize;
        switch (direction) {
            case "up"    -> { attackBox.x = worldX;               attackBox.y = worldY - gp.tileSize; }
            case "down"  -> { attackBox.x = worldX;               attackBox.y = worldY + gp.tileSize; }
            case "left"  -> { attackBox.x = worldX - gp.tileSize; attackBox.y = worldY; }
            case "right" -> { attackBox.x = worldX + gp.tileSize; attackBox.y = worldY; }
        }

        // Goblins
        for (Goblin g : gp.goblins) {
            if (g == null || !g.alive) continue;
            if (attackBox.intersects(worldRect(g))) { g.takeDamage(attackPower); gp.playSE(1); }
        }
        // Trolls
        for (Troll t : gp.trolls) {
            if (t == null || !t.alive) continue;
            if (attackBox.intersects(worldRect(t))) { t.takeDamage(attackPower); gp.playSE(1); }
        }
        // Giant — pass swing direction so it can check heart weak-point
        if (gp.giant != null && gp.giant.alive) {
            if (attackBox.intersects(worldRect(gp.giant))) {
                gp.giant.takeDamage(attackPower, direction); gp.playSE(1);
            }
        }
        // Dragon
        if (gp.dragon != null && gp.dragon.alive) {
            if (attackBox.intersects(worldRect(gp.dragon))) {
                gp.dragon.takeDamage(attackPower); gp.playSE(1);
            }
        }
    }

    private Rectangle worldRect(Entity e) {
        return new Rectangle(
                e.worldX + e.solidArea.x,
                e.worldY + e.solidArea.y,
                e.solidArea.width,
                e.solidArea.height
        );
    }

    // -----------------------------------------------
    // OBJECT INTERACTION
    // -----------------------------------------------
    private void interactObject(int index) {
        if (index == 999) return;
        if (keyH.interactPressed) {
            keyH.interactPressed = false;
            gp.object[index].interact(gp, index);
        } else if (!gp.object[index].collision) {
            gp.object[index].interact(gp, index);
        }
    }

    // -----------------------------------------------
    // TAKE DAMAGE — shield absorbs some
    // -----------------------------------------------
    @Override
    public void takeDamage(int amount) {
        if (invincible) return;

        int absorbed   = shieldLevel;
        int actualDmg  = Math.max(0, amount - absorbed);
        if (shieldStrength > 0) { shieldStrength--; actualDmg = Math.max(0, actualDmg - 1); }

        health -= actualDmg;
        if (health < 0) health = 0;

        invincible     = true;
        invincibleTimer = 0;
        gp.playSE(3);
    }

    // -----------------------------------------------
    // DRAW
    // -----------------------------------------------
    public void draw(Graphics2D g2) {
        // Flicker during invincibility
        if (invincible && invincibleTimer % 10 < 5) return;

        int drawX = worldX - gp.tileManager.getCameraX();
        int drawY = worldY - gp.tileManager.getCameraY();
        BufferedImage img = currentFrame();
        if (img != null) g2.drawImage(img, drawX, drawY, gp.tileSize, gp.tileSize, null);
    }

    private BufferedImage currentFrame() {
        if (attacking) {
            int f = attackFrame;
            return switch (direction) {
                case "down"  -> f<10 ? attackDown1  : f<20 ? attackDown2  : attackDown3;
                case "up"    -> f<10 ? attackUp1    : f<20 ? attackUp2    : attackUp3;
                case "left"  -> f<10 ? attackLeft1  : f<20 ? attackLeft2  : attackLeft3;
                case "right" -> f<10 ? attackRight1 : f<20 ? attackRight2 : attackRight3;
                default -> walkDown1;
            };
        }
        return switch (direction) {
            case "down"  -> spriteNum == 1 ? walkDown1  : walkDown2;
            case "up"    -> spriteNum == 1 ? walkUp1    : walkUp2;
            case "left"  -> spriteNum == 1 ? walkLeft1  : walkLeft2;
            case "right" -> spriteNum == 1 ? walkRight1 : walkRight2;
            default -> walkDown1;
        };
    }
}
