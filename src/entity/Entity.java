package entity;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Entity {

    // World position (pixels, not tiles)
    public int worldX, worldY;
    public int speed;

    // Sprite
    public BufferedImage image;
    public String direction = "down";
    public int spriteCounter = 0;
    public int spriteNum     = 1;

    // Collision
    public Rectangle solidArea         = new Rectangle(0, 0, 48, 48);
    public int solidAreaDefaultX       = 0;
    public int solidAreaDefaultY       = 0;
    public boolean collisionOn         = false;

    // Combat — used by Player and all enemies
    public int maxHealth  = 6;
    public int health     = 6;
    public int attackPower = 1;
    public boolean alive  = true;

    // Invincibility frames after being hit
    public boolean invincible    = false;
    public int invincibleTimer   = 0;
    public int invincibleMax     = 60; // 1 second
}
