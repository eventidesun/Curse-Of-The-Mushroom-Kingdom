package entity;

import main.GamePanel;
import object.Tiara_Object;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Giant extends Entity {

    GamePanel gp;

    BufferedImage idle1, idle2;
    BufferedImage attack1, attack2, attack3;

    private Rectangle heartArea;

    private int actionTimer = 0;
    private int attackTimer = 0;
    private int attackCooldown = 120;
    private int chaseRange = 8;

    private boolean attacking = false;
    private int attackFrame = 0;
    private int attackAnimTimer = 0;

    private boolean hasDropped = false;

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

        heartArea = new Rectangle(48, 20, 32, 32);

        direction = "down";

        loadSprites();
    }

    private void loadSprites() {
        idle1 = loadImage("/player/boy_down_1.png");
        idle2 = loadImage("/player/boy_down_2.png");

        // placeholder attack (replace later)
        attack1 = loadImage("/player/boy_left_1.png");
        attack2 = loadImage("/player/boy_left_2.png");
        attack3 = loadImage("/player/boy_left_1.png");
    }

    private BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(getClass().getResourceAsStream(path));
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Giant sprite not found: " + path);
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
        } else if (dist <= 2) {
            startAttack();
        } else if (dist <= chaseRange) {
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

    private void startAttack() {
        if (attackTimer == 0) {
            attacking = true;
            attackFrame = 0;
            attackAnimTimer = 0;
        }
    }

    private void runAttackAnimation() {
        attackAnimTimer++;

        if (attackAnimTimer > 12) {
            attackFrame++;
            attackAnimTimer = 0;
        }

        // stomp hit frame
        if (attackFrame == 1) {
            stomp();
        }

        if (attackFrame > 2) {
            attacking = false;
            attackTimer = attackCooldown;
        }
    }

    private void stomp() {
        gp.playSE(2);
        gp.screenShake(20);

        int dx = Math.abs(gp.player.worldX - worldX);
        int dy = Math.abs(gp.player.worldY - worldY);

        if (dx < gp.tileSize * 3 && dy < gp.tileSize * 3) {
            gp.player.takeDamage(attackPower);
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

        if (actionTimer >= 200) {
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

    // -----------------------------------------------
    // DAMAGE — only from ABOVE
    // -----------------------------------------------
    public void takeDamage(int amount, String playerDirection) {
        if (invincible || !alive) return;

        boolean fromAbove = playerDirection.equals("down") &&
                gp.player.worldY < worldY;

        if (!fromAbove) {
            gp.ui.showMessage("Aim for the heart!");
            return;
        }

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
        gp.screenShake(30);

        if (!hasDropped) {
            hasDropped = true;

            for (int i = 0; i < gp.object.length; i++) {
                if (gp.object[i] == null) {
                    Tiara_Object tiara = new Tiara_Object();
                    tiara.worldX = worldX;
                    tiara.worldY = worldY;
                    gp.object[i] = tiara;
                    break;
                }
            }
        }
    }

    public void draw(Graphics2D g2) {
        if (!alive) return;

        int screenX = worldX - gp.tileManager.getCameraX();
        int screenY = worldY - gp.tileManager.getCameraY();

        boolean onScreen =
                worldX + gp.tileSize * 2 > gp.player.worldX - gp.player.screenX &&
                        worldX - gp.tileSize * 2 < gp.player.worldX + gp.player.screenX &&
                        worldY + gp.tileSize * 2 > gp.player.worldY - gp.player.screenY &&
                        worldY - gp.tileSize * 2 < gp.player.worldY + gp.player.screenY;

        if (!onScreen) return;

        if (invincible && invincibleTimer % 10 < 5) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
        }

        int size = gp.tileSize * 2;

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
            g2.drawImage(frame, screenX, screenY, size, size, null);
        } else {
            g2.setColor(new Color(80, 60, 40));
            g2.fillRect(screenX, screenY, size, size);
        }

        // Heart weak point
        int hx = screenX + heartArea.x;
        int hy = screenY + heartArea.y;

        g2.setColor(new Color(220, 50, 50, 200));
        g2.fillOval(hx, hy, heartArea.width, heartArea.height);

        g2.setColor(Color.RED);
        g2.drawOval(hx, hy, heartArea.width, heartArea.height);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        drawHealthBar(g2, screenX, screenY - 14, size);
    }

    private void drawHealthBar(Graphics2D g2, int x, int y, int width) {
        int fillW = (int)((double) health / maxHealth * width);

        g2.setColor(new Color(40, 40, 40, 180));
        g2.fillRoundRect(x, y, width, 8, 4, 4);

        g2.setColor(new Color(220, 60, 60));
        g2.fillRoundRect(x, y, fillW, 8, 4, 4);

        g2.setColor(new Color(255, 255, 255, 60));
        g2.drawRoundRect(x, y, width, 8, 4, 4);
    }
}
