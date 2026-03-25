package main;

import entity.*;
import object.SuperObject;
import tile.TileManager;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel implements Runnable {

    final int originalTileSize    = 16;
    final int scale               = 4;
    public final int tileSize     = originalTileSize * scale;

    Dimension screenSize          = Toolkit.getDefaultToolkit().getScreenSize();
    public final int screenWidth  = (int) screenSize.getWidth();
    public final int screenHeight = (int) screenSize.getHeight();
    public final int maxScreenCol = screenWidth  / tileSize;
    public final int maxScreenRow = screenHeight / tileSize;

    public final int maxWorldCol = 120;
    public final int maxWorldRow = 140;
    public int maxWorldColCurrent = 120;
    public int maxWorldRowCurrent = 140;

    int FPS = 60;

    public enum GameState { CUTSCENE, DREAM, OVERWORLD, BATTLE, DIALOGUE, PUZZLE, ENDING }
    public GameState gameState = GameState.CUTSCENE;

    public boolean puzzleAfterDialogue = false;

    private int shakeTimer     = 0;
    private int shakeMagnitude = 0;

    public TileManager tileManager           = new TileManager(this);
    public KeyHandler keyH                   = new KeyHandler();
    public Sound music                       = new Sound();
    public Sound se                          = new Sound();
    public CollisionChecker collisionChecker = new CollisionChecker(this);
    public AssetSetter assetSetter           = new AssetSetter(this);
    public UI ui                             = new UI(this);
    public DialogueManager dialogueManager   = new DialogueManager(this);
    public CutsceneManager cutsceneManager   = new CutsceneManager(this);
    public PuzzleScene puzzleScene           = new PuzzleScene(this);
    public EndingScene endingScene           = new EndingScene(this);

    Thread gameThread;

    public Player player        = new Player(this, keyH);
    public SuperObject[] object = new SuperObject[40];

    public Goblin[] goblins = new Goblin[10];
    public Troll[]  trolls  = new Troll[6];
    public Giant    giant   = null;
    public Dragon   dragon  = null;

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);
    }

    public void setupGame() {
        assetSetter.setObject();
        assetSetter.setEnemies();

        // Player position for when overworld starts after cutscene
        player.worldX = 59 * tileSize;
        player.worldY = 12 * tileSize;

        // START CUTSCENE — press F1 to skip during testing
        // To skip to overworld directly, comment line below and uncomment the next:
        cutsceneManager.startScene(CutsceneManager.Scene.INTRO_CASTLE);
        // gameState = GameState.OVERWORLD;

        playMusic(0);
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000.0 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        while (gameThread != null) {
            long currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;
            if (delta >= 1) { update(); repaint(); delta--; }
        }
    }

    public void update() {
        if (shakeTimer > 0) shakeTimer--;

        // F1 — skip to overworld
        if (keyH.debugPressed) {
            keyH.debugPressed = false;
            gameState = GameState.OVERWORLD;
            playMusic(1);
        }

        tileManager.updateTransition();
        checkCaveEntrance();

        switch (gameState) {
            case CUTSCENE, DREAM -> {
                cutsceneManager.update();
                dialogueManager.update(); // typewriter ticks during cutscenes
            }
            case OVERWORLD, BATTLE -> {
                if (!tileManager.isTransitioning()) {
                    player.update();
                    updateEnemies();
                    checkPlayerAttack();
                    checkDragonDefeated();
                    checkCaveExit();
                }
            }
            case DIALOGUE -> dialogueManager.update();
            case PUZZLE   -> puzzleScene.update();
            case ENDING   -> endingScene.update();
        }
    }

    private void checkCaveEntrance() {
        if (tileManager.currentMap != TileManager.MapName.WORLD) return;
        if (tileManager.isTransitioning()) return;
        int col = player.worldX / tileSize;
        int row = player.worldY / tileSize;
        if (row >= 135 && col >= 52 && col <= 67) {
            tileManager.transitionToCave();
            stopMusic();
        }
    }

    public void checkCaveExit() {
        if (tileManager.currentMap != TileManager.MapName.CAVE) return;
        if (tileManager.isTransitioning()) return;
        if (player.worldY / tileSize <= 1) {
            tileManager.transitionToWorld();
            playMusic(1);
        }
    }

    private void updateEnemies() {
        for (Goblin g : goblins) if (g != null && g.alive) g.update();
        for (Troll  t : trolls)  if (t != null && t.alive) t.update();
        if (giant  != null && giant.alive)  giant.update();
        if (dragon != null && dragon.alive) dragon.update();
    }

    private void checkPlayerAttack() {
        if (!player.attacking) return;
        if (player.attackFrame < 10 || player.attackFrame >= 20) return;
        int range = tileSize + 10;
        int px = player.worldX + tileSize / 2;
        int py = player.worldY + tileSize / 2;
        for (Goblin g : goblins) {
            if (g == null || !g.alive) continue;
            if (hitCheck(px, py, g.worldX + tileSize/2, g.worldY + tileSize/2, range)) g.takeDamage(player.attackPower);
        }
        for (Troll t : trolls) {
            if (t == null || !t.alive) continue;
            if (hitCheck(px, py, t.worldX + tileSize/2, t.worldY + tileSize/2, range)) t.takeDamage(player.attackPower);
        }
        if (giant != null && giant.alive)
            if (hitCheck(px, py, giant.worldX + tileSize, giant.worldY + tileSize, range * 2))
                giant.takeDamage(player.attackPower, player.direction);
        if (dragon != null && dragon.alive)
            if (hitCheck(px, py, dragon.worldX + tileSize, dragon.worldY + tileSize, range * 3))
                dragon.takeDamage(player.attackPower);
    }

    private boolean hitCheck(int px, int py, int ex, int ey, int range) {
        int distX = Math.abs(px - ex);
        int distY = Math.abs(py - ey);
        boolean inRange = distX < range && distY < range;
        boolean inFront = switch (player.direction) {
            case "up"    -> ey < py && distX < tileSize;
            case "down"  -> ey > py && distX < tileSize;
            case "left"  -> ex < px && distY < tileSize;
            case "right" -> ex > px && distY < tileSize;
            default -> false;
        };
        return inRange && inFront;
    }

    private void checkDragonDefeated() {
        if (dragon != null && !dragon.alive && player.hasNecklace) {
            if (object[20] == null) {
                object[20]        = new object.Orb_Object();
                object[20].worldX = dragon.worldX;
                object[20].worldY = dragon.worldY - tileSize * 2;
            }
        }
    }

    @Override
    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_OFF);

        if (shakeTimer > 0) {
            g2.translate((int)((Math.random()-0.5)*shakeMagnitude*2),
                    (int)((Math.random()-0.5)*shakeMagnitude*2));
        }

        switch (gameState) {
            case CUTSCENE, DREAM -> { cutsceneManager.draw(g2); ui.draw(g2); }
            case OVERWORLD, BATTLE, DIALOGUE -> {
                tileManager.draw(g2); drawObjects(g2); drawEnemies(g2); player.draw(g2); ui.draw(g2);
            }
            case PUZZLE -> { tileManager.draw(g2); player.draw(g2); puzzleScene.draw(g2); ui.draw(g2); }
            case ENDING -> endingScene.draw(g2);
        }

        tileManager.drawTransitionOverlay(g2);
        g2.dispose();
    }

    private void drawObjects(Graphics2D g2) { for (SuperObject o : object) if (o != null) o.draw(g2, this); }

    private void drawEnemies(Graphics2D g2) {
        for (Goblin g : goblins) if (g != null && g.alive) g.draw(g2);
        for (Troll  t : trolls)  if (t != null && t.alive) t.draw(g2);
        if (giant  != null && giant.alive)  giant.draw(g2);
        if (dragon != null && dragon.alive) dragon.draw(g2);
    }

    public void screenShake(int magnitude) { shakeTimer = 20; shakeMagnitude = magnitude; }

    public void startDialogue(String speaker, String[] lines) {
        dialogueManager.startDialogue(speaker, lines);
        gameState = GameState.DIALOGUE;
    }

    public void endDialogue() {
        if (puzzleAfterDialogue) { puzzleAfterDialogue = false; puzzleScene.resume(); }
        else gameState = GameState.OVERWORLD;
    }

    public void playMusic(int i) { music.setFile(i); music.play(); music.loop(); }
    public void stopMusic()      { music.stop(); }
    public void playSE(int i)    { se.setFile(i); se.play(); }
}
