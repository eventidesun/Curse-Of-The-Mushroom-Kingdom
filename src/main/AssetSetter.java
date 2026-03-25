package main;

import object.*;
import entity.*;

public class AssetSetter {

    GamePanel gp;

    public AssetSetter(GamePanel gp) { this.gp = gp; }

    // -----------------------------------------------
    // OBJECTS — world map positions (120x140 grid)
    // -----------------------------------------------
    public void setObject() {

        // Healing chests
        gp.object[0] = new Chest_Object();
        gp.object[0].worldX = 58 * gp.tileSize; gp.object[0].worldY = 18 * gp.tileSize;

        gp.object[1] = new Chest_Object();
        gp.object[1].worldX = 60 * gp.tileSize; gp.object[1].worldY = 38 * gp.tileSize;

        gp.object[2] = new Chest_Object();
        gp.object[2].worldX = 57 * gp.tileSize; gp.object[2].worldY = 60 * gp.tileSize;

        gp.object[3] = new Chest_Object();
        gp.object[3].worldX = 62 * gp.tileSize; gp.object[3].worldY = 95 * gp.tileSize;

        gp.object[4] = new Chest_Object();
        gp.object[4].worldX = 59 * gp.tileSize; gp.object[4].worldY = 120 * gp.tileSize;

        // Special chests — shield upgrades
        gp.object[5] = new SpecialChest_Object(2); // iron shield — after trolls
        gp.object[5].worldX = 63 * gp.tileSize; gp.object[5].worldY = 79 * gp.tileSize;

        gp.object[6] = new SpecialChest_Object(3); // magic shield — near cave entrance
        gp.object[6].worldX = 60 * gp.tileSize; gp.object[6].worldY = 132 * gp.tileSize;

        // Easter eggs — hidden
        gp.object[7] = new EasterEgg_Object();
        gp.object[7].worldX = 15 * gp.tileSize; gp.object[7].worldY = 10 * gp.tileSize;

        gp.object[8] = new EasterEgg_Object();
        gp.object[8].worldX = 30 * gp.tileSize; gp.object[8].worldY = 45 * gp.tileSize;

        gp.object[9] = new EasterEgg_Object();
        gp.object[9].worldX = 47 * gp.tileSize; gp.object[9].worldY = 67 * gp.tileSize;

        gp.object[10] = new EasterEgg_Object();
        gp.object[10].worldX = 88 * gp.tileSize; gp.object[10].worldY = 92 * gp.tileSize;

        gp.object[11] = new EasterEgg_Object();
        gp.object[11].worldX = 100 * gp.tileSize; gp.object[11].worldY = 125 * gp.tileSize;

        // Signs
        gp.object[12] = new Sign_Object(
                "Welcome to Portobello Forest.",
                "Watch your step."
        );
        gp.object[12].worldX = 59 * gp.tileSize; gp.object[12].worldY = 28 * gp.tileSize;

        gp.object[13] = new Sign_Object(
                "Bridge ahead.",
                "Locals say trolls guard it at night.",
                "...It is night."
        );
        gp.object[13].worldX = 59 * gp.tileSize; gp.object[13].worldY = 63 * gp.tileSize;

        gp.object[14] = new Sign_Object(
                "River crossing.",
                "A giant was spotted nearby.",
                "Aim for the heart."
        );
        gp.object[14].worldX = 59 * gp.tileSize; gp.object[14].worldY = 82 * gp.tileSize;

        gp.object[15] = new Sign_Object(
                "Dragon's Cave.",
                "Turn back.",
                "...",
                "No, seriously."
        );
        gp.object[15].worldX = 59 * gp.tileSize; gp.object[15].worldY = 130 * gp.tileSize;
    }

    // -----------------------------------------------
    // WORLD ENEMIES
    // Dragon is NOT placed here — spawns when cave loads
    // -----------------------------------------------
    public void setEnemies() {

        // Goblins — forest zone rows 30-60
        gp.goblins[0] = new Goblin(gp);
        gp.goblins[0].worldX = 55 * gp.tileSize;
        gp.goblins[0].worldY = 35 * gp.tileSize;
        gp.goblins[0].isLeader = true; // drops rose

        gp.goblins[1] = new Goblin(gp);
        gp.goblins[1].worldX = 62 * gp.tileSize;
        gp.goblins[1].worldY = 40 * gp.tileSize;

        gp.goblins[2] = new Goblin(gp);
        gp.goblins[2].worldX = 58 * gp.tileSize;
        gp.goblins[2].worldY = 50 * gp.tileSize;

        gp.goblins[3] = new Goblin(gp);
        gp.goblins[3].worldX = 65 * gp.tileSize;
        gp.goblins[3].worldY = 55 * gp.tileSize;

        // Trolls — bridge zone rows 67-75
        gp.trolls[0] = new Troll(gp);
        gp.trolls[0].worldX = 54 * gp.tileSize;
        gp.trolls[0].worldY = 70 * gp.tileSize;
        gp.trolls[0].isLeader = true; // drops ring

        gp.trolls[1] = new Troll(gp);
        gp.trolls[1].worldX = 59 * gp.tileSize;
        gp.trolls[1].worldY = 72 * gp.tileSize;

        gp.trolls[2] = new Troll(gp);
        gp.trolls[2].worldX = 64 * gp.tileSize;
        gp.trolls[2].worldY = 70 * gp.tileSize;

        // Giant — river zone rows 85-100
        gp.giant        = new Giant(gp);
        gp.giant.worldX = 59 * gp.tileSize;
        gp.giant.worldY = 90 * gp.tileSize;

        // Dragon — null until cave loads
        gp.dragon = null;
    }

    // -----------------------------------------------
    // CAVE ENEMIES — called by TileManager.loadMap()
    // when cave.txt is loaded
    // -----------------------------------------------
    public void setCaveEnemies() {
        gp.dragon              = new Dragon(gp);
        gp.dragon.worldX       = 19 * gp.tileSize;
        gp.dragon.worldY       = 38 * gp.tileSize;
        gp.dragon.fightStarted = false;
        System.out.println("Dragon spawned in cave.");
    }
}
