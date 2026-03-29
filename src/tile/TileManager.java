package tile;

import main.GamePanel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TileManager {

    GamePanel gp;
    public Tile[] tile;
    public int[][] mapTileNum;

    public enum MapName { WORLD, CAVE }
    public MapName currentMap = MapName.WORLD;

    private boolean fading      = false;
    private boolean fadingIn    = false;
    private float   fadeAlpha   = 0f;
    private String  nextMapPath = null;
    private int[]   spawnPos    = null;

    public TileManager(GamePanel gp) {
        this.gp = gp;
        tile = new Tile[10];
        getTileImage();
        loadMap("/maps/world.txt");
    }

    public void getTileImage() {
        // -----------------------------------------------
        // TILE INDEX
        //  0 = grass          (passable)
        //  1 = grass_patch    (passable — lighter grass variation)
        //  2 = grass_dark     (passable — darker grass variation)
        //  3 = bush           (COLLISION)
        //  4 = bush_light     (COLLISION — lighter bush)
        //  5 = stone_path     (passable)
        //  6 = water          (COLLISION)
        //  7 = sand           (passable)
        //  8 = wall           (COLLISION)
        //  9 = castle_floor   (passable)
        // -----------------------------------------------
        try {
            tile[0] = new Tile();
            tile[0].image = ImageIO.read(getClass().getResourceAsStream("/tiles/grass_patch.png"));

            tile[1] = new Tile();
            tile[1].image = loadTileWithFallback("/tiles/grass_dark.png", "/tiles/grass_patch.png");

            tile[2] = new Tile();
            tile[2].image = loadTileWithFallback("/tiles/grass_dark.png", "/tiles/grass_patch.png");

            tile[3] = new Tile();
            tile[3].image = loadTileWithFallback("/tiles/bush.png", "/tiles/grass_patch.png");
            tile[3].collision = true;

            tile[4] = new Tile();
            tile[4].image = loadTileWithFallback("/tiles/bush_light.png", "/tiles/bush.png");
            tile[4].collision = true;

            tile[5] = new Tile();
            tile[5].image = loadTileWithFallback("/tiles/stone_path.png", "/tiles/grass_patch.png");

            tile[6] = new Tile();
            tile[6].image = ImageIO.read(getClass().getResourceAsStream("/tiles/water.png"));
            tile[6].collision = true;

            tile[7] = new Tile();
            tile[7].image = loadTileWithFallback("/tiles/sand.png", "/tiles/grass_patch.png");

            tile[8] = new Tile();
            tile[8].image = ImageIO.read(getClass().getResourceAsStream("/tiles/wall.png"));
            tile[8].collision = true;

            tile[9] = new Tile();
            tile[9].image = loadTileWithFallback("/tiles/castle_floor.png", "/tiles/stone_path.png");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load primary tile; fall back to secondary if not found
    private java.awt.image.BufferedImage loadTileWithFallback(String primary, String fallback) {
        try {
            InputStream is = getClass().getResourceAsStream(primary);
            if (is != null) return ImageIO.read(is);
        } catch (Exception ignored) {}
        try {
            System.out.println("Tile not found: " + primary + " — using fallback: " + fallback);
            return ImageIO.read(getClass().getResourceAsStream(fallback));
        } catch (Exception e) {
            System.out.println("Fallback also missing: " + fallback);
            return null;
        }
    }

    // -----------------------------------------------
    // MAP LOADING
    // -----------------------------------------------
    public void loadMap(String filePath) {
        int cols, rows;
        if (filePath.contains("cave")) {
            cols = 40; rows = 60;
            currentMap = MapName.CAVE;
            gp.maxWorldColCurrent = 40;
            gp.maxWorldRowCurrent = 60;
        } else {
            cols = 120; rows = 140;
            currentMap = MapName.WORLD;
            gp.maxWorldColCurrent = 120;
            gp.maxWorldRowCurrent = 140;
        }

        mapTileNum = new int[cols][rows];

        try {
            InputStream is = getClass().getResourceAsStream(filePath);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            int row = 0;
            while (row < rows) {
                String line = br.readLine();
                if (line == null) break;
                String[] numbers = line.trim().split(" ");
                for (int col = 0; col < cols && col < numbers.length; col++) {
                    try {
                        mapTileNum[col][row] = Integer.parseInt(numbers[col].trim());
                    } catch (NumberFormatException e) {
                        mapTileNum[col][row] = 0;
                    }
                }
                row++;
            }
            br.close();
            System.out.println("Map loaded: " + filePath + " (" + cols + "x" + rows + ")");

            if (filePath.contains("cave")) {
                gp.assetSetter.setCaveEnemies();
            }

        } catch (Exception e) {
            System.out.println("Map load error: " + e.getMessage());
        }
    }

    // -----------------------------------------------
    // SAFE TILE SOLID CHECK
    // -----------------------------------------------
    public boolean isSolid(int col, int row) {
        col = Math.max(0, Math.min(col, gp.maxWorldColCurrent - 1));
        row = Math.max(0, Math.min(row, gp.maxWorldRowCurrent - 1));
        int tileNum = mapTileNum[col][row];
        if (tileNum < 0 || tileNum >= tile.length || tile[tileNum] == null) return false;
        return tile[tileNum].collision;
    }

    // -----------------------------------------------
    // TRANSITIONS
    // -----------------------------------------------
    public void transitionToCave() {
        if (fading) return;
        fading = true; fadingIn = false; fadeAlpha = 0f;
        nextMapPath = "/maps/cave.txt";
        spawnPos = new int[]{ 19 * gp.tileSize, 2 * gp.tileSize };
    }

    public void transitionToWorld() {
        if (fading) return;
        fading = true; fadingIn = false; fadeAlpha = 0f;
        nextMapPath = "/maps/world.txt";
        spawnPos = new int[]{ 59 * gp.tileSize, 134 * gp.tileSize };
    }

    public void updateTransition() {
        if (!fading) return;
        if (!fadingIn) {
            fadeAlpha += 0.05f;
            if (fadeAlpha >= 1f) {
                fadeAlpha = 1f;
                loadMap(nextMapPath);
                gp.player.worldX = spawnPos[0];
                gp.player.worldY = spawnPos[1];
                fadingIn = true;
            }
        } else {
            fadeAlpha -= 0.05f;
            if (fadeAlpha <= 0f) {
                fadeAlpha = 0f;
                fading = false;
                fadingIn = false;
            }
        }
    }

    public void drawTransitionOverlay(Graphics2D g2) {
        if (fadeAlpha <= 0f) return;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha));
        g2.setColor(Color.black);
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    public boolean isTransitioning() { return fading; }

    // -----------------------------------------------
    // DRAW
    // -----------------------------------------------
    public void draw(Graphics2D g2) {
        int camX = getCameraX();
        int camY = getCameraY();

        for (int worldRow = 0; worldRow < gp.maxWorldRowCurrent; worldRow++) {
            for (int worldCol = 0; worldCol < gp.maxWorldColCurrent; worldCol++) {
                int tileNum = mapTileNum[worldCol][worldRow];
                int screenX = worldCol * gp.tileSize - camX;
                int screenY = worldRow * gp.tileSize - camY;

                if (screenX + gp.tileSize > 0 && screenX < gp.screenWidth &&
                        screenY + gp.tileSize > 0 && screenY < gp.screenHeight) {
                    if (tileNum >= 0 && tileNum < tile.length && tile[tileNum] != null) {
                        g2.drawImage(tile[tileNum].image, screenX, screenY, gp.tileSize, gp.tileSize, null);
                    }
                }
            }
        }
    }

    // -----------------------------------------------
    // CAMERA
    // -----------------------------------------------
    public int getCameraX() {
        int mapW = gp.maxWorldColCurrent * gp.tileSize;
        int camX = gp.player.worldX - gp.screenWidth / 2;
        return Math.max(0, Math.min(camX, Math.max(0, mapW - gp.screenWidth)));
    }

    public int getCameraY() {
        int mapH = gp.maxWorldRowCurrent * gp.tileSize;
        int camY = gp.player.worldY - gp.screenHeight / 2;
        return Math.max(0, Math.min(camY, Math.max(0, mapH - gp.screenHeight)));
    }
}
