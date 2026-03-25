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

    // Screen position — king is always drawn at centre of screen
    public final int screenX;
    public final int screenY;

    // -----------------------------------------------
    // HEALTH
    // 10 hearts × 2 units each = 20 max
    // -----------------------------------------------
    public int health    = 20;
    public int maxHealth = 20;

    // -----------------------------------------------
    // QUEST ITEMS
    // -----------------------------------------------
    public boolean hasRose     = false;
    public boolean hasRing     = false;
    public boolean hasTiara    = false;
    public boolean hasNecklace = false;

    // -----------------------------------------------
    // SHIELD
    // Level 1 = wood, 2 = iron, 3 = magic
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
    public int attackCooldown  = 0; // prevent spamming
    public int attackPower     = 2; // damage dealt per hit

    // Invincibility frames after taking damage (so you don't lose all hearts at once)
    public boolean invincible    = false;
    public int invincibleTimer   = 0;
    public int invincibleMax     = 120; // 2 seconds at 60fps

    // -----------------------------------------------
    // SPRITES
    // Walk cycle — 2 frames per direction
    // -----------------------------------------------
    BufferedImage walkDown1,  walkDown2;
    BufferedImage walkUp1,    walkUp2;
    BufferedImage walkLeft1,  walkLeft2;
    BufferedImage walkRight1, walkRight2;

    // Attack frames — 3 frames (wind-up, swing, recover)
    BufferedImage attackDown1,  attackDown2,  attackDown3;
    BufferedImage attackUp1,    attackUp2,    attackUp3;
    BufferedImage attackLeft1,  attackLeft2,  attackLeft3;
    BufferedImage attackRight1, attackRight2, attackRight3;

    // Hurt flash
    BufferedImage hurtFrame;

    public Player(GamePanel gp, KeyHandler keyH) {
        this.gp   = gp;
        this.keyH = keyH;

        // King is always drawn at the centre of the screen
        screenX = gp.screenWidth  / 2 - gp.tileSize / 2;
        screenY = gp.screenHeight / 2 - gp.tileSize / 2;

        // Solid collision box — smaller than the full tile so movement feels tight
        solidArea = new Rectangle(8, 16, 32, 32);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;

        setDefaultValues();
        loadSprites();
    }

    // where the player starts at the very beginning
    private void setDefaultValues() {
        // Starting position on the world map (in tiles)
        worldX = 23 * gp.tileSize;
        worldY = 23 * gp.tileSize;
        speed = 4;
        direction = "down";
    }

    private void loadSprites() {
        try {
            // --- Walk cycle ---
            // Using boy sprites as placeholder until king sprites are ready
            // When art is done, swap paths to /characters/king_down_1.png etc.
            walkDown1  = loadImage("/player/boy_down_1.png");
            walkDown2  = loadImage("/player/boy_down_2.png");
            walkUp1    = loadImage("/player/boy_up_1.png");
            walkUp2    = loadImage("/player/boy_up_2.png");
            walkLeft1  = loadImage("/player/boy_left_1.png");
            walkLeft2  = loadImage("/player/boy_left_2.png");
            walkRight1 = loadImage("/player/boy_right_1.png");
            walkRight2 = loadImage("/player/boy_right_2.png");

            // --- Attack frames ---
            // Placeholder: reuse walk frames until attack art is ready
            // Swap to /characters/king_attack_down_1.png etc. when done
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

    // -----------------------------------------------
    // UPDATE — called every frame
    // -----------------------------------------------
    public void update() {

        // Handle attack input
        if (keyH.attackPressed && !attacking && attackCooldown == 0) {
            attacking     = true;
            attackFrame   = 0;
            attackCooldown = 30; // half second cooldown between swings
        }

        // Handle witch hint button
        if (keyH.witchHintPressed) {
            keyH.witchHintPressed = false; // consume
            gp.dialogueManager.showWitchHint();
        }

        // Tick cooldowns
        if (attackCooldown > 0) attackCooldown--;
        if (invincible) {
            invincibleTimer++;
            if (invincibleTimer >= invincibleMax) {
                invincible      = false;
                invincibleTimer = 0;
            }
        }

        // -----------------------------------------------
        // ATTACKING — run attack animation
        // -----------------------------------------------
        if (attacking) {
            attackFrame++;

            // Frame 1–10: wind-up
            // Frame 11–20: swing (this is when hit registers — handled by enemy classes)
            // Frame 21–30: recover
            if (attackFrame >= 30) {
                attacking   = false;
                attackFrame = 0;
            }
            // Don't move while attacking
            return;
        }

        // -----------------------------------------------
        // MOVEMENT
        // -----------------------------------------------
        boolean moving = keyH.upPressed || keyH.downPressed ||
                keyH.leftPressed || keyH.rightPressed;

        if (moving) {
            if (keyH.upPressed)    direction = "up";
            if (keyH.downPressed)  direction = "down";
            if (keyH.leftPressed)  direction = "left";
            if (keyH.rightPressed) direction = "right";

            // Check tile collision
            collisionOn = false;
            gp.collisionChecker.checkTile(this);

            // Check object collision
            int objIndex = gp.collisionChecker.checkObject(this, true);
            interactObject(objIndex);

            // Only move if no collision
            if (!collisionOn) {
                switch (direction) {
                    case "up"    -> worldY -= speed;
                    case "down"  -> worldY += speed;
                    case "left"  -> worldX -= speed;
                    case "right" -> worldX += speed;
                }
            }

            // Animate walk cycle — swap frame every 10 game frames
            spriteCounter++;
            if (spriteCounter > 10) {
                spriteNum     = (spriteNum == 1) ? 2 : 1;
                spriteCounter = 0;
            }
        }

        // Check cave exit — player walks back to top of cave
        gp.checkCaveExit();

        // Handle interact key — advance dialogue or interact with object
        if (keyH.interactPressed) {
            keyH.interactPressed = false; // consume so it doesn't fire every frame

            if (gp.dialogueManager.isActive()) {
                // If line is still typing, skip to end of line
                if (!gp.dialogueManager.isLineComplete()) {
                    gp.dialogueManager.skipToEnd();
                }
                // Otherwise DialogueManager.update() handles advancing on interact
            }
        }
    }

    // -----------------------------------------------
    // OBJECT INTERACTION
    // -----------------------------------------------
    private void interactObject(int index) {
        if (index != 999) {
            // If player presses E near an object, interact with it
            if (gp.keyH.interactPressed) {
                gp.keyH.interactPressed = false;
                gp.object[index].interact(gp, index);
            } else if (!gp.object[index].collision) {
                // Non-collision objects (rose, ring etc.) auto-interact on touch
                gp.object[index].interact(gp, index);
            }
        }
    }

    // -----------------------------------------------
    // TAKE DAMAGE
    // Call this from enemy classes: gp.player.takeDamage(2)
    // -----------------------------------------------
    public void takeDamage(int amount) {
        if (invincible) return; // already flashing — ignore hit

        // Shield absorbs some damage
        int shieldAbsorb = shieldLevel; // wood=1, iron=2, magic=3
        int actualDamage = Math.max(0, amount - shieldAbsorb);

        // Damage shield first if it has strength
        if (shieldStrength > 0) {
            shieldStrength -= 1;
            actualDamage = Math.max(0, actualDamage - 1);
        }

        health -= actualDamage;
        if (health < 0) health = 0;

        // Start invincibility frames
        invincible      = true;
        invincibleTimer = 0;

        gp.playSE(3); // index 3 = receivedamage.wav — already in your sound folder!

        // Check death
        if (health == 0) {
            // TODO: game over screen
            System.out.println("King has fallen.");
        }
    }

    // -----------------------------------------------
    // HEAL
    // Call this from chest: gp.player.heal(4)
    // -----------------------------------------------
    public void heal(int amount) {
        health = Math.min(health + amount, maxHealth);
    }

    // -----------------------------------------------
    // DRAW
    // -----------------------------------------------
    public void draw(Graphics2D g2) {
        BufferedImage image = getCurrentFrame();

        // Invincibility flash — alternate visibility every 5 frames
        if (invincible && invincibleTimer % 10 < 5) {
            // Skip drawing = flash effect
            return;
        }

        if (image != null) {
            int drawX = worldX - gp.tileManager.getCameraX();
            int drawY = worldY - gp.tileManager.getCameraY();
            g2.drawImage(image, drawX, drawY, gp.tileSize, gp.tileSize, null);
        } else {
            // Placeholder rectangle when sprites aren't loaded yet
            g2.setColor(new Color(100, 180, 100));
            g2.fillRect(screenX, screenY, gp.tileSize, gp.tileSize);
            g2.setColor(Color.white);
            g2.drawRect(screenX, screenY, gp.tileSize, gp.tileSize);
        }
    }

    private BufferedImage getCurrentFrame() {
        if (attacking) {
            // Attack animation frames
            int f = attackFrame;
            return switch (direction) {
                case "down"  -> f < 10 ? attackDown1  : f < 20 ? attackDown2  : attackDown3;
                case "up"    -> f < 10 ? attackUp1    : f < 20 ? attackUp2    : attackUp3;
                case "left"  -> f < 10 ? attackLeft1  : f < 20 ? attackLeft2  : attackLeft3;
                case "right" -> f < 10 ? attackRight1 : f < 20 ? attackRight2 : attackRight3;
                default      -> walkDown1;
            };
        }

        // Walk animation
        return switch (direction) {
            case "down"  -> spriteNum == 1 ? walkDown1  : walkDown2;
            case "up"    -> spriteNum == 1 ? walkUp1    : walkUp2;
            case "left"  -> spriteNum == 1 ? walkLeft1  : walkLeft2;
            case "right" -> spriteNum == 1 ? walkRight1 : walkRight2;
            default      -> walkDown1;
        };
    }
}
