package entity;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Entity {

    public int worldX, worldY;
    public int speed;

    public BufferedImage image;
    public String direction  = "down";
    public int spriteCounter = 0;
    public int spriteNum     = 1;

    public Rectangle solidArea         = new Rectangle(0, 0, 48, 48);
    public int       solidAreaDefaultX = 0;
    public int       solidAreaDefaultY = 0;
    public boolean   collisionOn       = false;

    public int     maxHealth   = 6;
    public int     health      = 6;
    public int     attackPower = 1;
    public boolean alive       = true;

    public boolean invincible      = false;
    public int     invincibleTimer = 0;
    public int     invincibleMax   = 60;

    // Base takeDamage — subclasses override when needed
    public void takeDamage(int damage) {
        if (invincible || !alive) return;
        health -= damage;
        invincible = true;
        invincibleTimer = 0;
        if (health <= 0) { health = 0; alive = false; }
    }

    // Call this every frame for any entity that uses invincibility
    public void updateInvincibility() {
        if (!invincible) return;
        invincibleTimer++;
        if (invincibleTimer > invincibleMax) { invincible = false; invincibleTimer = 0; }
    }
}
