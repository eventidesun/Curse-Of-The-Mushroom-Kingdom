package entity;

import main.GamePanel;
import object.Necklace_Object;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Dragon extends Entity {

    GamePanel gp;

    // -----------------------------------------------
    // SPRITES — same dragon as cutscene, fire form in battle
    // -----------------------------------------------
    BufferedImage idle1, idle2, idle3;   // dragon1/2/3 — circling idle
    BufferedImage fire1, fire2, fire3;   // dragon_fire1/2/3 — when breathing fire

    // -----------------------------------------------
    // PHASES
    // IDLE    — drifts toward player slowly
    // FIRE    — spits a fireball
    // CHARGE  — quick dash toward player
    // -----------------------------------------------
    public enum Phase { IDLE, FIRE, CHARGE }
    private Phase currentPhase = Phase.IDLE;
    private int   phaseTimer   = 0;
    private int   attackCooldown = 0;

    // Fire projectile
    private boolean fireActive = false;
    private float   fireX, fireY;
    private int     fireDir   = 1;    // +1 = right, -1 = left
    private float   fireSpeed = 8f;

    // Charge
    private boolean charging    = false;
    private int     chargeTimer = 0;

    // Queen visual — she's held above the dragon until it dies
    private BufferedImage queenSprite;

    // Defeat flag
    private boolean hasDropped  = false;
    public  boolean fightStarted = false;

    // Direction flag for sprite flip
    private int facing = -1; // -1 = left (facing king), +1 = right

    public Dragon(GamePanel gp) {
        this.gp = gp;

        // Balanced — not too hard for a jam game
        maxHealth   = 20;
        health      = maxHealth;
        speed       = 1;
        attackPower = 3;
        invincibleMax = 60;

        solidArea = new Rectangle(16, 16, 160, 160);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;

        loadSprites();
    }

    private void loadSprites() {
        idle1 = load("/enemies/dragon1.png");
        idle2 = load("/enemies/dragon2.png");
        idle3 = load("/enemies/dragon3.png");
        fire1 = load("/enemies/dragon_fire1.png");
        fire2 = load("/enemies/dragon_fire2.png");
        fire3 = load("/enemies/dragon_fire3.png");
        queenSprite = load("/player/boy_down_2.png"); // placeholder — swap to queen sprite when ready
    }

    private BufferedImage load(String path) {
        try {
            var is = getClass().getResourceAsStream(path);
            if (is != null) return ImageIO.read(is);
        } catch (Exception ignored) {}
        System.out.println("Missing dragon sprite: " + path);
        return null;
    }

    // -----------------------------------------------
    // UPDATE
    // -----------------------------------------------
    public void update() {
        if (!alive || !fightStarted) return;

        // Invincibility tick
        if (invincible) {
            invincibleTimer++;
            if (invincibleTimer >= invincibleMax) { invincible = false; invincibleTimer = 0; }
        }
        if (attackCooldown > 0) attackCooldown--;

        // Face the player
        facing = gp.player.worldX > worldX ? 1 : -1;

        // Phase timer — rotate through phases every ~3 seconds
        phaseTimer++;
        if (phaseTimer >= 180) {
            phaseTimer = 0;
            pickPhase();
        }

        switch (currentPhase) {
            case IDLE   -> drift();
            case FIRE   -> doFire();
            case CHARGE -> doCharge();
        }

        if (fireActive) moveFireball();

        // Sprite animation
        spriteCounter++;
        if (spriteCounter > 10) { spriteNum = (spriteNum + 1) % 3; spriteCounter = 0; }
    }

    private void pickPhase() {
        double hpFrac = (double) health / maxHealth;
        int roll = (int)(Math.random() * 3);
        // Below half health: no idle, faster rotation
        if (hpFrac < 0.5) {
            currentPhase = roll == 0 ? Phase.FIRE : Phase.CHARGE;
        } else {
            currentPhase = switch (roll) {
                case 0 -> Phase.FIRE;
                case 1 -> Phase.CHARGE;
                default -> Phase.IDLE;
            };
        }
    }

    // Slow drift toward player — gentle threat presence
    private void drift() {
        int px = gp.player.worldX, py = gp.player.worldY;
        int dx = px - worldX, dy = py - worldY;
        double dist = Math.sqrt(dx*dx + dy*dy);
        if (dist > gp.tileSize * 4) {
            worldX += (int)(dx / dist * speed);
            worldY += (int)(dy / dist * speed);
        }
    }

    // Spit one fireball toward the player
    private void doFire() {
        if (attackCooldown == 0 && !fireActive) {
            fireX = worldX + gp.tileSize * 1.5f;
            fireY = worldY + gp.tileSize * 1.5f;
            fireDir = facing;
            fireActive = true;
            attackCooldown = 90; // ~1.5s before another fireball
            gp.playSE(2);
        }
    }

    private void moveFireball() {
        fireX += fireDir * fireSpeed;

        // Hit player?
        float dx = Math.abs(gp.player.worldX + gp.tileSize/2f - fireX);
        float dy = Math.abs(gp.player.worldY + gp.tileSize/2f - fireY);
        if (dx < gp.tileSize * 0.8f && dy < gp.tileSize * 0.8f) {
            gp.player.takeDamage(attackPower);
            fireActive = false;
        }

        // Off screen
        if (fireX < -gp.tileSize * 2 || fireX > (gp.maxWorldColCurrent + 2) * gp.tileSize)
            fireActive = false;
    }

    // Quick dash at player — telegraphed, stoppable
    private void doCharge() {
        if (!charging && attackCooldown == 0) {
            charging    = true;
            chargeTimer = 45; // charge lasts 0.75s
        }
        if (charging) {
            chargeTimer--;
            worldX += facing * 5;

            // Hit player during charge?
            int dx = Math.abs(gp.player.worldX - worldX);
            int dy = Math.abs(gp.player.worldY - worldY);
            if (dx < gp.tileSize * 1.5f && dy < gp.tileSize * 1.5f) {
                gp.player.takeDamage(attackPower);
                gp.screenShake(15);
            }
            if (chargeTimer <= 0) { charging = false; attackCooldown = 120; }
        }
    }

    // -----------------------------------------------
    // TAKE DAMAGE
    // -----------------------------------------------
    @Override
    public void takeDamage(int amount) {
        if (invincible || !alive || !fightStarted) return;
        health -= amount;
        invincible      = true;
        invincibleTimer = 0;
        gp.playSE(2);
        if (health <= 0) { health = 0; die(); }
    }

    private void die() {
        alive = false;
        gp.screenShake(40);

        // Drop the necklace — restores king's last memory
        if (!hasDropped) {
            hasDropped = true;
            for (int i = 0; i < gp.object.length; i++) {
                if (gp.object[i] == null) {
                    Necklace_Object n = new Necklace_Object();
                    n.worldX = worldX + gp.tileSize;
                    n.worldY = worldY + gp.tileSize * 2;
                    gp.object[i] = n;
                    break;
                }
            }
        }

        // Trigger the queen-falls cutscene then ending
        gp.cutsceneManager.startScene(main.CutsceneManager.Scene.ENDING);
    }

    // -----------------------------------------------
    // DRAW
    // -----------------------------------------------
    public void draw(Graphics2D g2) {
        if (!alive) return;

        int sx   = worldX - gp.tileManager.getCameraX();
        int sy   = worldY - gp.tileManager.getCameraY();
        int size = gp.tileSize * 4; // 4×4 tile dragon — same as cutscene

        // Frustum cull
        if (sx + size < 0 || sx > gp.screenWidth ||
                sy + size < 0 || sy > gp.screenHeight) return;

        // Flash when hit
        if (invincible && invincibleTimer % 8 < 4)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));

        // Pick frame — fire sprites during FIRE phase, idle sprites otherwise
        BufferedImage[] frames = (currentPhase == Phase.FIRE && fireActive)
                ? new BufferedImage[]{ fire1, fire2, fire3 }
                : new BufferedImage[]{ idle1, idle2, idle3 };
        BufferedImage frame = frames[spriteNum % 3];

        // Draw queen held above dragon (until death)
        if (queenSprite != null) {
            int qx = sx + size / 2 - gp.tileSize / 2;
            int qy = sy - gp.tileSize - 8;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            g2.drawImage(queenSprite, qx, qy, gp.tileSize, gp.tileSize, null);
        }

        // Draw dragon — flip horizontally based on facing direction
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        if (frame != null) {
            if (facing == 1) {
                // Facing right — flip (dragon naturally faces left in the sprite)
                g2.drawImage(frame, sx + size, sy, -size, size, null);
            } else {
                // Facing left — natural orientation
                g2.drawImage(frame, sx, sy, size, size, null);
            }
        } else {
            g2.setColor(Color.RED);
            g2.fillRect(sx, sy, size, size);
        }

        // Fire projectile
        if (fireActive) {
            int fx = (int)(fireX - gp.tileManager.getCameraX());
            int fy = (int)(fireY - gp.tileManager.getCameraY());
            // Outer glow
            g2.setColor(new Color(255, 80, 0, 140));
            g2.fillOval(fx - 12, fy - 12, 48, 48);
            // Core
            g2.setColor(new Color(255, 200, 40));
            g2.fillOval(fx - 6, fy - 6, 28, 28);
        }

        // Health bar — red, above dragon
        drawHealthBar(g2, sx, sy - 20, size);
    }

    private void drawHealthBar(Graphics2D g2, int x, int y, int w) {
        int fill = (int)((double) health / maxHealth * w);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        g2.setColor(new Color(40, 40, 40, 200)); g2.fillRoundRect(x, y, w, 12, 6, 6);
        g2.setColor(new Color(200, 50, 50));     g2.fillRoundRect(x, y, fill, 12, 6, 6);
        g2.setColor(new Color(255,255,255, 50)); g2.drawRoundRect(x, y, w, 12, 6, 6);
    }
}
