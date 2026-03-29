package entity;

import main.GamePanel;
import object.Necklace_Object;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Dragon extends Entity {

    GamePanel gp;

    // ANIMATION
    BufferedImage idle1, idle2, idle3;
    BufferedImage fire1, fire2, fire3;

    // PHASE SYSTEM
    public enum Phase { IDLE, FIRE_BREATH, TAIL_SWIPE, CHARGE }
    private Phase currentPhase = Phase.IDLE;

    private int phaseTimer = 0;
    private int phaseLength = 180;

    private int attackCooldown = 0;

    // FIRE
    private int fireX, fireY;
    private boolean fireActive = false;
    private int fireDirection = 1;

    // CHARGE
    private boolean charging = false;
    private int chargeTimer = 0;

    private boolean hasDropped = false;
    public boolean fightStarted = false;

    public Dragon(GamePanel gp) {
        this.gp = gp;

        maxHealth = 30;
        health = maxHealth;

        speed = 2;
        attackPower = 4;
        invincibleMax = 90;

        solidArea = new Rectangle(24, 24, 144, 144);

        loadSprites();
    }

    private void loadSprites() {
        idle1 = load("/enemies/dragon1.png");
        idle2 = load("/enemies/dragon2.png");
        idle3 = load("/enemies/dragon3.png");

        fire1 = load("/enemies/dragon_fire1.png");
        fire2 = load("/enemies/dragon_fire2.png");
        fire3 = load("/enemies/dragon_fire3.png");
    }

    private BufferedImage load(String path) {
        try {
            return ImageIO.read(getClass().getResourceAsStream(path));
        } catch (Exception e) {
            System.out.println("Missing dragon sprite: " + path);
            return null;
        }
    }

    public void update() {
        if (!alive || !fightStarted) return;

        handleInvincibility();
        if (attackCooldown > 0) attackCooldown--;

        phaseTimer++;
        if (phaseTimer >= phaseLength) {
            phaseTimer = 0;
            pickNextPhase();
        }

        facePlayer();

        switch (currentPhase) {
            case FIRE_BREATH -> updateFire();
            case TAIL_SWIPE  -> updateTail();
            case CHARGE      -> updateCharge();
        }

        if (fireActive) updateFireProjectile();

        animate();
    }

    private void handleInvincibility() {
        if (invincible) {
            invincibleTimer++;
            if (invincibleTimer >= invincibleMax) {
                invincible = false;
                invincibleTimer = 0;
            }
        }
    }

    private void animate() {
        spriteCounter++;
        if (spriteCounter > 10) {
            spriteNum = (spriteNum + 1) % 3;
            spriteCounter = 0;
        }
    }

    private void pickNextPhase() {
        double hp = (double) health / maxHealth;
        int roll = (int)(Math.random() * 3);

        if (hp < 0.3) {
            currentPhase = roll == 0 ? Phase.FIRE_BREATH : Phase.CHARGE;
            phaseLength = 120;
        } else {
            currentPhase = switch (roll) {
                case 0 -> Phase.FIRE_BREATH;
                case 1 -> Phase.TAIL_SWIPE;
                default -> Phase.CHARGE;
            };
            phaseLength = 180;
        }
    }

    private void facePlayer() {
        fireDirection = gp.player.worldX > worldX ? 1 : -1;
    }

    private void updateFire() {
        if (attackCooldown == 0) {
            fireX = worldX + gp.tileSize;
            fireY = worldY + gp.tileSize;
            fireActive = true;
            attackCooldown = 40;
            gp.playSE(2);
        }
    }

    private void updateFireProjectile() {
        fireX += fireDirection * 10;

        int dx = Math.abs(gp.player.worldX - fireX);
        int dy = Math.abs(gp.player.worldY - fireY);

        if (dx < gp.tileSize && dy < gp.tileSize) {
            gp.player.takeDamage(3);
            fireActive = false;
        }

        if (fireX < 0 || fireX > gp.maxWorldCol * gp.tileSize) {
            fireActive = false;
        }
    }

    private void updateTail() {
        if (attackCooldown == 0) {
            int dx = Math.abs(gp.player.worldX - worldX);
            int dy = Math.abs(gp.player.worldY - worldY);

            if (dx < gp.tileSize * 3 && dy < gp.tileSize * 3) {
                gp.player.takeDamage(attackPower);
                gp.screenShake(15);
            }

            attackCooldown = 90;
        }
    }

    private void updateCharge() {
        if (!charging && attackCooldown == 0) {
            charging = true;
            chargeTimer = 60;
        }

        if (charging) {
            chargeTimer--;

            int speed = 6;

            if (fireDirection == 1) worldX += speed;
            else worldX -= speed;

            int dx = Math.abs(gp.player.worldX - worldX);
            int dy = Math.abs(gp.player.worldY - worldY);

            if (dx < gp.tileSize && dy < gp.tileSize) {
                gp.player.takeDamage(attackPower);
                gp.screenShake(25);
            }

            if (chargeTimer <= 0) {
                charging = false;
                attackCooldown = 100;
            }
        }
    }

    public void takeDamage(int amount) {
        if (invincible || !fightStarted) return;

        health -= amount;
        invincible = true;
        invincibleTimer = 0;

        gp.playSE(2);

        if (health <= 0) die();
    }

    private void die() {
        alive = false;
        gp.screenShake(40);

        if (!hasDropped) {
            hasDropped = true;

            for (int i = 0; i < gp.object.length; i++) {
                if (gp.object[i] == null) {
                    Necklace_Object n = new Necklace_Object();
                    n.worldX = worldX;
                    n.worldY = worldY;
                    gp.object[i] = n;
                    break;
                }
            }
        }
    }

    public void draw(Graphics2D g2) {
        if (!alive) return;

        int screenX = worldX - gp.tileManager.getCameraX();
        int screenY = worldY - gp.tileManager.getCameraY();

        int size = gp.tileSize * 3;

        BufferedImage frame;

        if (currentPhase == Phase.FIRE_BREATH) {
            frame = switch (spriteNum) {
                case 0 -> fire1;
                case 1 -> fire2;
                default -> fire3;
            };
        } else {
            frame = switch (spriteNum) {
                case 0 -> idle1;
                case 1 -> idle2;
                default -> idle3;
            };
        }

        if (frame != null) {

            if (fireDirection == 1) {
                // RIGHT
                g2.drawImage(frame, screenX, screenY, size, size, null);
            } else {
                // LEFT (flip)
                g2.drawImage(frame,
                        screenX + size, screenY,
                        -size, size,
                        null);
            }

        } else {
            g2.setColor(Color.RED);
            g2.fillRect(screenX, screenY, size, size);
        }

        // FIRE VISUAL
        if (fireActive) {
            int fx = fireX - gp.player.worldX + gp.player.screenX;
            int fy = fireY - gp.player.worldY + gp.player.screenY;

            g2.setColor(new Color(255,120,20));
            g2.fillOval(fx, fy, 30, 30);
        }

        drawHealthBar(g2, screenX, screenY - 20, size);
    }

    private void drawHealthBar(Graphics2D g2, int x, int y, int width) {
        int fill = (int)((double) health / maxHealth * width);

        g2.setColor(new Color(40,40,40));
        g2.fillRoundRect(x, y, width, 12, 6, 6);

        g2.setColor(new Color(200,50,50));
        g2.fillRoundRect(x, y, fill, 12, 6, 6);
    }
}
