package entity;

import main.GamePanel;
import object.Rose_Object;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Goblin extends Entity {

    GamePanel gp;

    private BufferedImage idle1, idle2;
    private BufferedImage attack1, attack2, attack3;

    private int actionTimer = 0;
    private int attackTimer = 0;
    private static final int ATTACK_COOLDOWN = 90;
    private static final int CHASE_RANGE = 5;
    private static final int ATTACK_RANGE = 1;

    private boolean attacking = false;
    private int attackFrame = 0;
    private int attackAnimTimer = 0;

    private boolean hasDropped = false;
    public boolean isLeader = false;

    private static final AlphaComposite HALF_ALPHA =
            AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f);

    private static final AlphaComposite FULL_ALPHA =
            AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f);

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
        idle1 = scale(load("/enemies/goblin1.png", "/enemies/goblin2.png"));
        idle2 = idle1;
        attack1 = idle1;
        attack2 = scale(load("/enemies/goblin2.png", "/enemies/goblin2.png"));
        attack3 = scale(load("/enemies/goblin3.png", "/enemies/goblin2.png"));
    }

    private BufferedImage load(String primary, String fallback) {
        try {
            var is = getClass().getResourceAsStream(primary);
            if (is != null) return ImageIO.read(is);
        } catch (Exception ignored) {}
        try { return ImageIO.read(getClass().getResourceAsStream(fallback)); }
        catch (Exception e) { return null; }
    }

    private BufferedImage scale(BufferedImage img) {
        if (img == null) return null;

        BufferedImage scaled = new BufferedImage(
                gp.tileSize,
                gp.tileSize,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2 = scaled.createGraphics();
        g2.drawImage(img, 0, 0, gp.tileSize, gp.tileSize, null);
        g2.dispose();

        return scaled;
    }

    public void update() {
        if (!alive) return;

        updateInvincibility();

        if (attackTimer > 0) attackTimer--;

        int dist = tileDist();

        if (attacking) {
            runAttackAnim();
        } else if (dist <= ATTACK_RANGE) {
            beginAttack();
        } else if (dist <= CHASE_RANGE) {
            chasePlayer();
        } else {
            patrol();
        }

        spriteCounter++;
        if (spriteCounter > 15) { spriteNum = (spriteNum == 1) ? 2 : 1; spriteCounter = 0; }
    }

    private int tileDist() {
        int dx = Math.abs(gp.player.worldX - worldX) / gp.tileSize;
        int dy = Math.abs(gp.player.worldY - worldY) / gp.tileSize;
        return dx + dy;
    }

    private void beginAttack() {
        if (attackTimer == 0) { attacking = true; attackFrame = 0; attackAnimTimer = 0; }
    }

    private void runAttackAnim() {
        attackAnimTimer++;
        if (attackAnimTimer > 8) { attackFrame++; attackAnimTimer = 0; }
        if (attackFrame == 1) { gp.player.takeDamage(attackPower); gp.playSE(2); }
        if (attackFrame > 2) { attacking = false; attackTimer = ATTACK_COOLDOWN; }
    }

    private void chasePlayer() {
        int px = gp.player.worldX, py = gp.player.worldY;
        if (Math.abs(py - worldY) > Math.abs(px - worldX))
            direction = py < worldY ? "up" : "down";
        else
            direction = px < worldX ? "left" : "right";
        move();
    }

    private void patrol() {
        actionTimer++;
        if (actionTimer >= 120) {
            actionTimer = 0;
            direction = switch ((int)(Math.random() * 4)) {
                case 0 -> "up"; case 1 -> "down"; case 2 -> "left"; default -> "right";
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
        if (health <= 0) { health = 0; die(); }
    }

    private void die() {
        alive = false;
        if (isLeader && !hasDropped) {
            hasDropped = true;
            spawnDrop();
        }
    }

    private void spawnDrop() {
        for (int i = 0; i < gp.object.length; i++) {
            if (gp.object[i] == null) {
                Rose_Object rose = new Rose_Object();
                rose.worldX = worldX;
                rose.worldY = worldY;
                gp.object[i] = rose;
                return;
            }
        }
    }

    public void draw(Graphics2D g2) {
        if (!alive) return;

        int sx = worldX - gp.tileManager.getCameraX();
        int sy = worldY - gp.tileManager.getCameraY();

        if (sx + gp.tileSize < 0 || sx > gp.screenWidth ||
                sy + gp.tileSize < 0 || sy > gp.screenHeight) return;

        if (invincible && invincibleTimer % 10 < 5)
            g2.setComposite(HALF_ALPHA);

        BufferedImage frame = attacking
                ? switch (attackFrame) { case 0 -> attack1; case 1 -> attack2; default -> attack3; }
                : (spriteNum == 1 ? idle1 : idle2);

        if (frame != null)
            g2.drawImage(frame, sx, sy, null);
        else {
            g2.setColor(new Color(80, 160, 60));
            g2.fillRect(sx, sy, gp.tileSize, gp.tileSize);
        }

        g2.setComposite(FULL_ALPHA);
        drawHealthBar(g2, sx, sy);
    }

    private void drawHealthBar(Graphics2D g2, int sx, int sy) {
        int w = gp.tileSize, h = 6, y = sy - 10;
        int fw = (int)((double) health / maxHealth * w);
        g2.setColor(new Color(40, 40, 40, 180)); g2.fillRoundRect(sx, y, w, h, 3, 3);
        g2.setColor(new Color(80, 200, 80)); g2.fillRoundRect(sx, y, fw, h, 3, 3);
        g2.setColor(new Color(255, 255, 255, 60)); g2.drawRoundRect(sx, y, w, h, 3, 3);
    }
}
