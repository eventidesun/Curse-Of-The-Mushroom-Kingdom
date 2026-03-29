package entity;

import main.GamePanel;
import object.Tiara_Object;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Giant extends Entity {

    GamePanel gp;

    private BufferedImage idle1, idle2;
    private BufferedImage attack1, attack2, attack3;

    private final Rectangle heartArea = new Rectangle(48, 20, 32, 32);

    private int actionTimer = 0;
    private int attackTimer = 0;
    private static final int ATTACK_COOLDOWN = 120;
    private static final int CHASE_RANGE = 8;

    // Attack animation
    private boolean attacking = false;
    private int attackFrame = 0;
    private int attackAnimTimer = 0;

    // Drop
    private boolean hasDropped = false;

    private int pulseCounter = 0;

    private static final AlphaComposite HALF_ALPHA =
            AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f);

    private static final AlphaComposite FULL_ALPHA =
            AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f);

    public Giant(GamePanel gp) {
        this.gp = gp;

        maxHealth = 20;
        health = maxHealth;
        speed = 1;
        attackPower = 5;
        invincibleMax = 90;

        solidArea = new Rectangle(16, 16, 96, 96);
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;

        direction = "down";
        loadSprites();
    }

    private void loadSprites() {
        idle1 = scale(load("/enemies/giant.png", "/enemies/giant.png"));
        idle2 = idle1;
        attack1 = idle1;
        attack2 = idle1;
        attack3 = idle1;
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
                gp.tileSize * 2,
                gp.tileSize * 2,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2 = scaled.createGraphics();
        g2.drawImage(img, 0, 0, gp.tileSize * 2, gp.tileSize * 2, null);
        g2.dispose();

        return scaled;
    }

    public void update() {
        if (!alive) return;

        updateInvincibility();

        pulseCounter++;

        if (attackTimer > 0) attackTimer--;

        int dist = tileDist();

        if (attacking) {
            runAttackAnim();
        } else if (dist <= 2) {
            beginAttack();
        } else if (dist <= CHASE_RANGE) {
            chasePlayer();
        } else {
            patrol();
        }

        spriteCounter++;
        if (spriteCounter > 25) {
            spriteNum = (spriteNum == 1) ? 2 : 1;
            spriteCounter = 0;
        }
    }

    private int tileDist() {
        int dx = Math.abs(gp.player.worldX - worldX) / gp.tileSize;
        int dy = Math.abs(gp.player.worldY - worldY) / gp.tileSize;
        return dx + dy;
    }

    private void beginAttack() {
        if (attackTimer == 0) {
            attacking = true;
            attackFrame = 0;
            attackAnimTimer = 0;
        }
    }

    private void runAttackAnim() {
        attackAnimTimer++;
        if (attackAnimTimer > 12) {
            attackFrame++;
            attackAnimTimer = 0;
        }
        if (attackFrame == 1) stomp();
        if (attackFrame > 2) {
            attacking = false;
            attackTimer = ATTACK_COOLDOWN;
        }
    }

    private void stomp() {
        gp.playSE(2);
        gp.screenShake(20);

        int dx = Math.abs(gp.player.worldX - worldX);
        int dy = Math.abs(gp.player.worldY - worldY);

        if (dx < gp.tileSize * 3 && dy < gp.tileSize * 3)
            gp.player.takeDamage(attackPower);
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
        if (actionTimer >= 200) {
            actionTimer = 0;
            direction = switch ((int)(Math.random() * 4)) {
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

    public void takeDamage(int amount, String playerDirection) {
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

    @Override
    public void takeDamage(int amount) {
        takeDamage(amount, "any");
    }

    private void die() {
        alive = false;
        gp.screenShake(30);

        if (!hasDropped) {
            hasDropped = true;
            spawnDrop();
        }
    }

    private void spawnDrop() {
        for (int i = 0; i < gp.object.length; i++) {
            if (gp.object[i] == null) {
                Tiara_Object tiara = new Tiara_Object();
                tiara.worldX = worldX;
                tiara.worldY = worldY;
                gp.object[i] = tiara;
                return;
            }
        }
    }

    public void draw(Graphics2D g2) {
        if (!alive) return;

        int sx = worldX - gp.tileManager.getCameraX();
        int sy = worldY - gp.tileManager.getCameraY();
        int size = gp.tileSize * 2;

        if (sx + size < 0 || sx > gp.screenWidth ||
                sy + size < 0 || sy > gp.screenHeight) return;

        if (invincible && invincibleTimer % 10 < 5)
            g2.setComposite(HALF_ALPHA);

        BufferedImage frame = attacking
                ? switch (attackFrame) {
            case 0 -> attack1;
            case 1 -> attack2;
            default -> attack3;
        }
                : (spriteNum == 1 ? idle1 : idle2);

        if (frame != null)
            g2.drawImage(frame, sx, sy, null);
        else {
            g2.setColor(new Color(80, 60, 40));
            g2.fillRect(sx, sy, size, size);
        }

        float pulse = (float)(Math.sin(pulseCounter * 0.1) * 0.3 + 0.7);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse));

        int hx = sx + heartArea.x;
        int hy = sy + heartArea.y;

        g2.setColor(new Color(220, 50, 50));
        g2.fillOval(hx, hy, heartArea.width, heartArea.height);

        g2.setColor(Color.RED);
        g2.drawOval(hx, hy, heartArea.width, heartArea.height);

        g2.setComposite(FULL_ALPHA);

        drawHealthBar(g2, sx, sy - 14, size);
    }

    private void drawHealthBar(Graphics2D g2, int x, int y, int w) {
        int fw = (int)((double) health / maxHealth * w);

        g2.setColor(new Color(40, 40, 40, 180));
        g2.fillRoundRect(x, y, w, 8, 4, 4);

        g2.setColor(new Color(220, 60, 60));
        g2.fillRoundRect(x, y, fw, 8, 4, 4);

        g2.setColor(new Color(255, 255, 255, 60));
        g2.drawRoundRect(x, y, w, 8, 4, 4);
    }
}
