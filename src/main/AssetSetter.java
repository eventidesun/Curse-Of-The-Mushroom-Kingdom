package main;

import object.*;
import entity.*;

public class AssetSetter {

    GamePanel gp;

    public AssetSetter(GamePanel gp) { this.gp = gp; }

    // -----------------------------------------------
    // OBJECTS — all on open grass/path tiles, never wall
    // -----------------------------------------------
    public void setObject() {

        // Healing chests — on stone path, reachable by player
        obj(0, new Chest_Object(), 58, 18);
        obj(1, new Chest_Object(), 55, 38);
        obj(2, new Chest_Object(), 57, 58);
        obj(3, new Chest_Object(), 62, 95);
        obj(4, new Chest_Object(), 59, 120);

        // Special chests — shield upgrades
        obj(5, new SpecialChest_Object(2), 63, 79);   // iron shield
        obj(6, new SpecialChest_Object(3), 60, 132);  // magic shield

        // Easter egg diamonds — hidden in grass, off the main path
        // All placed on grass (tile 0/1/2), never on wall (tile 8)
        obj(7,  new EasterEgg_Object(), 18, 12);   // castle grounds left
        obj(8,  new EasterEgg_Object(), 98, 30);   // forest right side
        obj(9,  new EasterEgg_Object(), 22, 55);   // forest left side
        obj(10, new EasterEgg_Object(), 90, 92);   // wasteland right
        obj(11, new EasterEgg_Object(), 24, 108);  // wasteland left

        // Signs — on stone path at key decision points
        obj(12, new Sign_Object(
                "Welcome to Portobello Forest.",
                "Watch your step."
        ), 59, 28);

        obj(13, new Sign_Object(
                "Bridge ahead.",
                "Locals say trolls guard it at night.",
                "...It is night."
        ), 59, 63);

        obj(14, new Sign_Object(
                "A giant was spotted nearby.",
                "Aim for the heart."
        ), 59, 82);

        obj(15, new Sign_Object(
                "Dragon's Cave.",
                "Turn back.",
                "...",
                "No, seriously."
        ), 59, 130);
    }

    private void obj(int slot, SuperObject o, int col, int row) {
        o.worldX = col * gp.tileSize;
        o.worldY = row * gp.tileSize;
        gp.object[slot] = o;
    }

    // -----------------------------------------------
    // WORLD ENEMIES — all placed on open grass/path,
    // NOT on wall tiles (tile 8)
    // Forest zone = rows 27-63, cols 20-100 (away from walls cols 0-2, 117-119)
    // Bridge zone = rows 65-78, on stone bridge cols 56-63
    // Wasteland  = rows 82-118
    // -----------------------------------------------
    public void setEnemies() {

        // --- GOBLINS (forest, rows 30-60, grass cols) ---
        gp.goblins[0] = goblin(50, 33, true);   // leader — drops rose
        gp.goblins[1] = goblin(67, 36, false);
        gp.goblins[2] = goblin(44, 48, false);
        gp.goblins[3] = goblin(72, 53, false);

        // trolls
        gp.trolls[0] = troll(57, 69, true);   // leader — drops ring
        gp.trolls[1] = troll(61, 71, false);
        gp.trolls[2] = troll(59, 74, false);

        // giant
        gp.giant        = new Giant(gp);
        gp.giant.worldX = 59 * gp.tileSize;
        gp.giant.worldY = 92 * gp.tileSize;

        // Dragon — null until cave loads
        gp.dragon = null;
    }

    private Goblin goblin(int col, int row, boolean isLeader) {
        Goblin g = new Goblin(gp);
        g.worldX   = col * gp.tileSize;
        g.worldY   = row * gp.tileSize;
        g.isLeader = isLeader;
        return g;
    }

    private Troll troll(int col, int row, boolean isLeader) {
        Troll t = new Troll(gp);
        t.worldX   = col * gp.tileSize;
        t.worldY   = row * gp.tileSize;
        t.isLeader = isLeader;
        return t;
    }

    // cave enemies
    public void setCaveEnemies() {
        gp.dragon              = new Dragon(gp);
        gp.dragon.worldX       = 19 * gp.tileSize;
        gp.dragon.worldY       = 38 * gp.tileSize;
        gp.dragon.fightStarted = false;
        System.out.println("Dragon spawned in cave.");
    }
}
