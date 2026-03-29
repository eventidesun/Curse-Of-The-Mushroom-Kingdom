package entity;

import main.GamePanel;
import main.KeyHandler;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Player extends Entity {

    GamePanel  gp;
    KeyHandler keyH;

    public final int screenX;
    public final int screenY;

    // Quest items — only 3, no necklace
    public boolean hasRose  = false;
    public boolean hasRing  = false;
    public boolean hasTiara = false;

    // Shield
    public int shieldLevel       = 1;
    public int shieldStrength    = 10;
    public int maxShieldStrength = 10;

    // Diamonds / easter eggs
    public int eggsFound = 0;

    // Potions
    public int      potionsHeld    = 0;
    private boolean potionConsumed = false;

    // Attack state
    public boolean  attacking      = false;
    public int      attackFrame    = 0;
    public int      attackCooldown = 0;
    private boolean attackChecked  = false;

    // Walk sprites
    private BufferedImage walkDown1,  walkDown2;
    private BufferedImage walkUp1,    walkUp2;
    private BufferedImage walkLeft1,  walkLeft2;
    private BufferedImage walkRight1, walkRight2;

    // Attack sprites (reuse walk frames until real art is ready)
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

        maxHealth     = 20;
        health        = maxHealth;
        invincibleMax = 120;
        speed         = 4;
        attackPower   = 2;
        direction     = "down";

        worldX = 59 * gp.tileSize;
        worldY = 22 * gp.tileSize;

        loadSprites();
    }

    public void heal(int amount) {
        health = Math.min(health + amount, maxHealth);
    }

    // -----------------------------------------------
    // RESPAWN — back to castle gate, full health,
    // no cutscene, quest items kept
    // -----------------------------------------------
    public void respawn() {
        health         = maxHealth;
        shieldStrength = maxShieldStrength;
        invincible     = true;   // brief grace period on spawn
        invincibleTimer = 0;
        attacking      = false;
        attackFrame    = 0;
        attackCooldown = 0;
        direction      = "down";

        // Back to castle gate — same as overworld start
        worldX = 59 * gp.tileSize;
        worldY = 22 * gp.tileSize;

        // If player died in the cave, kick back to world map
        if (gp.tileManager.currentMap == tile.TileManager.MapName.CAVE) {
            gp.tileManager.transitionToWorld();
            gp.playMusic(1);
        }
    }

    private void loadSprites() {
        // King uses his own sprite — single idle frame since no walk animation yet
        walkDown1  = load("/characters/king.png");
        walkDown2  = load("/characters/king.png");
        walkUp1    = load("/characters/king.png");
        walkUp2    = load("/characters/king.png");
        walkLeft1  = load("/characters/king.png");
        walkLeft2  = load("/characters/king.png");
        walkRight1 = load("/characters/king.png");
        walkRight2 = load("/characters/king.png");

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

        // Start attack
        if (keyH.attackPressed && !attacking && attackCooldown == 0) {
            attacking       = true;
            attackFrame     = 0;
            attackCooldown  = 30;
            attackChecked   = false;
            keyH.attackPressed = false;
        }
        if (attackCooldown > 0) attackCooldown--;

        // Potion — P key
        if (keyH.usePotion && !potionConsumed) {
            potionConsumed = true;
            if (potionsHeld > 0 && health < maxHealth) {
                potionsHeld--;
                heal(6);
                gp.playSE(1);
                gp.ui.showMessage("Potion used! +3 hearts. (" + potionsHeld + " left)");
            } else if (potionsHeld == 0) {
                gp.ui.showMessage("No potions!");
            } else {
                gp.ui.showMessage("Already at full health!");
            }
        }
        if (!keyH.usePotion) potionConsumed = false;

        // Invincibility tick — paused during attack swing so no flicker
        if (!attacking) updateInvincibility();

        // Attack animation
        if (attacking) {
            attackFrame++;
            if (attackFrame == 15 && !attackChecked) {
                checkAttackHit();
                attackChecked = true;
            }
            if (attackFrame >= 30) { attacking = false; attackFrame = 0; }
            return; // no movement during swing
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
        Rectangle box = new Rectangle();
        box.width = box.height = gp.tileSize;
        switch (direction) {
            case "up"    -> { box.x = worldX;               box.y = worldY - gp.tileSize; }
            case "down"  -> { box.x = worldX;               box.y = worldY + gp.tileSize; }
            case "left"  -> { box.x = worldX - gp.tileSize; box.y = worldY; }
            case "right" -> { box.x = worldX + gp.tileSize; box.y = worldY; }
        }

        for (Goblin g : gp.goblins) {
            if (g == null || !g.alive) continue;
            if (box.intersects(worldRect(g))) { g.takeDamage(attackPower); gp.playSE(1); }
        }
        for (Troll t : gp.trolls) {
            if (t == null || !t.alive) continue;
            if (box.intersects(worldRect(t))) { t.takeDamage(attackPower); gp.playSE(1); }
        }
        if (gp.giant != null && gp.giant.alive)
            if (box.intersects(worldRect(gp.giant))) {
                gp.giant.takeDamage(attackPower, direction); gp.playSE(1);
            }
        if (gp.dragon != null && gp.dragon.alive)
            if (box.intersects(worldRect(gp.dragon))) {
                gp.dragon.takeDamage(attackPower); gp.playSE(1);
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
    // TAKE DAMAGE
    // -----------------------------------------------
    @Override
    public void takeDamage(int amount) {
        if (invincible) return;

        int absorbed  = shieldLevel;
        int actualDmg = Math.max(0, amount - absorbed);
        if (shieldStrength > 0) { shieldStrength--; actualDmg = Math.max(0, actualDmg - 1); }

        health -= actualDmg;
        if (health < 0) health = 0;

        invincible      = true;
        invincibleTimer = 0;
        gp.playSE(3);

        // Death — respawn at castle, no cutscene
        if (health == 0) {
            gp.ui.showMessage("The king has fallen... but his quest is not over.");
            respawn();
        }
    }

    // -----------------------------------------------
    // DRAW — flicker only when damaged, never during attack
    // -----------------------------------------------
    public void draw(Graphics2D g2) {
        if (invincible && !attacking && invincibleTimer % 10 < 5) return;

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
