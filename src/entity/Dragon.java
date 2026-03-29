package entity;

import main.GamePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Dragon extends Entity {

    GamePanel gp;

    // Sprites — same dragon as cutscene
    BufferedImage idle1, idle2, idle3;   // dragon1/2/3
    BufferedImage fire1, fire2, fire3;   // dragon_fire1/2/3

    public enum Phase { IDLE, FIRE, CHARGE }
    private Phase currentPhase = Phase.IDLE;
    private int   phaseTimer   = 0;
    private int   attackCooldown = 0;

    // Fire projectile
    private boolean fireActive = false;
    private float   fireX, fireY;
    private int     fireDir    = -1;  // -1 = left (facing king by default)

    // Charge
    private boolean charging    = false;
    private int     chargeTimer = 0;

    // Queen sprite drawn above dragon
    private BufferedImage queenSprite;

    private boolean hasDropped   = false;
    public  boolean fightStarted = false;

    // Facing: -1 = left, +1 = right
    private int facing = -1;

    public Dragon(GamePanel gp) {
        this.gp = gp;

        maxHealth     = 20;
        health        = maxHealth;
        speed         = 1;
        attackPower   = 3;
        invincibleMax = 60;

        solidArea         = new Rectangle(16, 16, 160, 160);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;

        loadSprites();
    }

    private void loadSprites() {
        idle1       = load("/enemies/dragon1.png");
        idle2       = load("/enemies/dragon2.png");
        idle3       = load("/enemies/dragon3.png");
        fire1       = load("/enemies/dragon_fire1.png");
        fire2       = load("/enemies/dragon_fire2.png");
        fire3       = load("/enemies/dragon_fire3.png");
        queenSprite = load("/player/boy_down_2.png"); // swap to queen sprite when ready
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
    // UPDATE — only runs when fightStarted = true
    // fightStarted is set by GamePanel.checkDragonFightStart()
    // which requires hasRose + hasRing + hasTiara
    // -----------------------------------------------
    public void update() {
        if (!alive || !fightStarted) return;

        if (invincible) {
            invincibleTimer++;
            if (invincibleTimer >= invincibleMax) { invincible = false; invincibleTimer = 0; }
        }
        if (attackCooldown > 0) attackCooldown--;

        // Face the player
        facing = gp.player.worldX > worldX ? 1 : -1;
        fireDir = facing;

        // Rotate phases every ~3 seconds
        phaseTimer++;
        if (phaseTimer >= 180) { phaseTimer = 0; pickPhase(); }

        switch (currentPhase) {
            case IDLE   -> drift();
            case FIRE   -> doFire();
            case CHARGE -> doCharge();
        }

        if (fireActive) moveFireball();

        // Wing animation
        spriteCounter++;
        if (spriteCounter > 10) { spriteNum = (spriteNum + 1) % 3; spriteCounter = 0; }
    }

    private void pickPhase() {
        double hpFrac = (double) health / maxHealth;
        int roll = (int)(Math.random() * 3);
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

    private void drift() {
        int dx = gp.player.worldX - worldX;
        int dy = gp.player.worldY - worldY;
        double dist = Math.sqrt(dx*dx + dy*dy);
        if (dist > gp.tileSize * 4) {
            worldX += (int)(dx / dist * speed);
            worldY += (int)(dy / dist * speed);
        }
    }

    private void doFire() {
        if (attackCooldown == 0 && !fireActive) {
            fireX = worldX + gp.tileSize * 1.5f;
            fireY = worldY + gp.tileSize * 1.5f;
            fireActive    = true;
            attackCooldown = 90;
            gp.playSE(2);
        }
    }

    private void moveFireball() {
        fireX += fireDir * 8f;
        float dx = Math.abs(gp.player.worldX + gp.tileSize/2f - fireX);
        float dy = Math.abs(gp.player.worldY + gp.tileSize/2f - fireY);
        if (dx < gp.tileSize * 0.8f && dy < gp.tileSize * 0.8f) {
            gp.player.takeDamage(attackPower);
            fireActive = false;
        }
        if (fireX < -gp.tileSize * 2 || fireX > (gp.maxWorldColCurrent + 2) * gp.tileSize)
            fireActive = false;
    }

    private void doCharge() {
        if (!charging && attackCooldown == 0) {
            charging = true; chargeTimer = 45;
        }
        if (charging) {
            chargeTimer--;
            worldX += facing * 5;
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
        invincible = true; invincibleTimer = 0;
        gp.playSE(2);
        if (health <= 0) { health = 0; die(); }
    }

    private void die() {
        alive = false;
        gp.screenShake(40);
        // Trigger queen-falls and the ending cutscene
        gp.cutsceneManager.startScene(main.CutsceneManager.Scene.ENDING);
    }

    public void draw(Graphics2D g2) {
        if (!alive) return;

        int sx   = worldX - gp.tileManager.getCameraX();
        int sy   = worldY - gp.tileManager.getCameraY();
        int size = gp.tileSize * 4;

        if (sx + size < 0 || sx > gp.screenWidth || sy + size < 0 || sy > gp.screenHeight) return;

        // Flash when hit
        if (invincible && invincibleTimer % 8 < 4)
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));

        // Use fire sprites when actively breathing, idle otherwise
        BufferedImage[] frames = (currentPhase == Phase.FIRE && fireActive)
                ? new BufferedImage[]{ fire1, fire2, fire3 }
                : new BufferedImage[]{ idle1, idle2, idle3 };
        BufferedImage frame = frames[spriteNum % 3];

        // Queen held above dragon
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        if (queenSprite != null) {
            int qx = sx + size/2 - gp.tileSize/2;
            int qy = sy - gp.tileSize - 8;
            g2.drawImage(queenSprite, qx, qy, gp.tileSize, gp.tileSize, null);
        }

        // Dragon — flip based on facing direction
        if (frame != null) {
            if (facing == 1)
                g2.drawImage(frame, sx + size, sy, -size, size, null); // facing right: flip
            else
                g2.drawImage(frame, sx, sy, size, size, null);          // facing left: natural
        } else {
            g2.setColor(Color.RED); g2.fillRect(sx, sy, size, size);
        }

        // Fireball
        if (fireActive) {
            int fx = (int)(fireX - gp.tileManager.getCameraX());
            int fy = (int)(fireY - gp.tileManager.getCameraY());
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            g2.setColor(new Color(255, 80, 0, 140)); g2.fillOval(fx-12, fy-12, 48, 48);
            g2.setColor(new Color(255, 200, 40));    g2.fillOval(fx-6,  fy-6,  28, 28);
        }

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        drawHealthBar(g2, sx, sy - 20, size);
    }

    private void drawHealthBar(Graphics2D g2, int x, int y, int w) {
        int fill = (int)((double) health / maxHealth * w);
        g2.setColor(new Color(40,40,40,200));  g2.fillRoundRect(x, y, w, 12, 6, 6);
        g2.setColor(new Color(200,50,50));     g2.fillRoundRect(x, y, fill, 12, 6, 6);
        g2.setColor(new Color(255,255,255,50));g2.drawRoundRect(x, y, w, 12, 6, 6);
    }
}
