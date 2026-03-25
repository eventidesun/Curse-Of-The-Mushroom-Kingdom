package main;

import java.awt.*;

public class PuzzleScene {

    GamePanel gp;

    // Puzzle state
    private boolean active       = false;
    private boolean roseSlotFilled    = false;
    private boolean ringSlotFilled    = false;
    private boolean tiaraSlotFilled   = false;
    private boolean necklaceSlotFilled = false;
    private boolean solved       = false;

    // Selected slot (player navigates with arrow keys)
    private int selectedSlot = 0; // 0=rose, 1=ring, 2=tiara, 3=necklace

    // Shatter animation
    private boolean shattering   = false;
    private int shatterTimer     = 0;
    private int shatterMax       = 120;

    // Orb position on screen (centre)
    private int orbX, orbY, orbSize;

    // Slot positions around the orb (N/E/S/W)
    private int[][] slotPositions; // [slot][x,y]

    public PuzzleScene(GamePanel gp) {
        this.gp = gp;
        orbSize = gp.tileSize * 3; // 192px
        orbX    = gp.screenWidth  / 2 - orbSize / 2;
        orbY    = gp.screenHeight / 2 - orbSize / 2;

        // Slot positions: above, right, below, left
        slotPositions = new int[][] {
                { orbX + orbSize / 2 - 32, orbY - 80 },        // rose — top
                { orbX + orbSize + 16,     orbY + orbSize / 2 - 32 }, // ring — right
                { orbX + orbSize / 2 - 32, orbY + orbSize + 16 }, // tiara — bottom
                { orbX - 80,               orbY + orbSize / 2 - 32 }  // necklace — left
        };
    }

    public void startPuzzle() {
        active   = false; // don't start immediately
        solved   = false;
        shattering = false;

        // Show intro dialogue first
        gp.startDialogue("witch", new String[] {
                "She's trapped inside that orb...",
                "A love spell. Only love can break it.",
                "Place the tokens of your love around the orb.",
                "The flower, the ring, the crown...",
                "And the necklace you reclaimed.",
                "Surround her with your memories."
        });

        // After dialogue ends, endDialogue() will call resumePuzzle()
        gp.puzzleAfterDialogue = true;
    }

    public void resume() {
        active = true;
        gp.gameState = GamePanel.GameState.PUZZLE;
    }

    public void update() {
        if (!active) return;
        if (shattering) {
            shatterTimer++;
            if (shatterTimer >= shatterMax) {
                // Puzzle complete — start ending
                gp.cutsceneManager.startScene(CutsceneManager.Scene.ENDING);
            }
            return;
        }

        // Navigate slots with arrow keys
        if (gp.keyH.rightPressed) {
            gp.keyH.rightPressed = false;
            selectedSlot = (selectedSlot + 1) % 4;
        }
        if (gp.keyH.leftPressed) {
            gp.keyH.leftPressed = false;
            selectedSlot = (selectedSlot + 3) % 4;
        }

        // Place item with E/Enter
        if (gp.keyH.interactPressed) {
            gp.keyH.interactPressed = false;
            placeItem();
        }
    }

    private void placeItem() {
        switch (selectedSlot) {
            case 0 -> {
                if (gp.player.hasRose && !roseSlotFilled) {
                    roseSlotFilled = true;
                    gp.playSE(1);
                    gp.ui.showMessage("You placed the flower.");
                } else if (!gp.player.hasRose) {
                    gp.ui.showMessage("You don't have the flower yet.");
                }
            }
            case 1 -> {
                if (gp.player.hasRing && !ringSlotFilled) {
                    ringSlotFilled = true;
                    gp.playSE(1);
                    gp.ui.showMessage("You placed the ring.");
                } else if (!gp.player.hasRing) {
                    gp.ui.showMessage("You don't have the ring yet.");
                }
            }
            case 2 -> {
                if (gp.player.hasTiara && !tiaraSlotFilled) {
                    tiaraSlotFilled = true;
                    gp.playSE(1);
                    gp.ui.showMessage("You placed the tiara.");
                } else if (!gp.player.hasTiara) {
                    gp.ui.showMessage("You don't have the tiara yet.");
                }
            }
            case 3 -> {
                if (gp.player.hasNecklace && !necklaceSlotFilled) {
                    necklaceSlotFilled = true;
                    gp.playSE(1);
                    gp.ui.showMessage("You placed the necklace.");
                } else if (!gp.player.hasNecklace) {
                    gp.ui.showMessage("You don't have the necklace yet.");
                }
            }
        }

        // Check if all 4 placed
        if (roseSlotFilled && ringSlotFilled && tiaraSlotFilled && necklaceSlotFilled) {
            solve();
        }
    }

    private void solve() {
        solved     = true;
        shattering = true;
        shatterTimer = 0;
        gp.stopMusic();
        gp.playSE(1); // orb shatter SFX — swap to orb_shatter.wav
        gp.screenShake(25);
    }

    public void draw(Graphics2D g2) {
        if (!active) return;

        // Dark overlay
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        if (shattering) {
            drawShatter(g2);
            return;
        }

        // Draw orb in centre
        drawOrb(g2);

        // Draw 4 item slots around orb
        String[] slotNames   = { "Flower", "Ring", "Tiara", "Necklace" };
        boolean[] slotFilled = { roseSlotFilled, ringSlotFilled, tiaraSlotFilled, necklaceSlotFilled };
        boolean[] playerHas  = {
                gp.player.hasRose, gp.player.hasRing,
                gp.player.hasTiara, gp.player.hasNecklace
        };

        for (int i = 0; i < 4; i++) {
            int sx = slotPositions[i][0];
            int sy = slotPositions[i][1];
            boolean isSelected = (i == selectedSlot);

            // Slot background
            if (slotFilled[i]) {
                g2.setColor(new Color(100, 220, 100, 180));
            } else if (isSelected) {
                g2.setColor(new Color(220, 200, 80, 180));
            } else if (playerHas[i]) {
                g2.setColor(new Color(80, 120, 200, 140));
            } else {
                g2.setColor(new Color(60, 60, 60, 140));
            }
            g2.fillRoundRect(sx, sy, 64, 64, 10, 10);

            // Border
            g2.setStroke(new BasicStroke(isSelected ? 3f : 1.5f));
            g2.setColor(isSelected ? new Color(255, 230, 80) : new Color(180, 180, 180, 120));
            g2.drawRoundRect(sx, sy, 64, 64, 10, 10);

            // Label
            g2.setFont(new Font("Courier New", Font.BOLD, 11));
            g2.setColor(Color.white);
            int labelX = sx + 32 - g2.getFontMetrics().stringWidth(slotNames[i]) / 2;
            g2.drawString(slotNames[i], labelX, sy + 80);

            // Checkmark if filled
            if (slotFilled[i]) {
                g2.setFont(new Font("Courier New", Font.BOLD, 28));
                g2.setColor(new Color(80, 220, 80));
                g2.drawString("✓", sx + 18, sy + 42);
            }
        }

        // Instructions
        g2.setFont(new Font("Courier New", Font.BOLD, 16));
        g2.setColor(new Color(220, 220, 220));
        g2.drawString("← → select slot    E place item    H for hint",
                gp.screenWidth / 2 - 230, gp.screenHeight - 40);
    }

    private void drawOrb(Graphics2D g2) {
        // Pulsing glow
        float pulse = 0.4f + 0.2f * (float) Math.sin(System.currentTimeMillis() / 500.0);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse * 0.5f));
        g2.setColor(new Color(160, 100, 255));
        g2.fillOval(orbX - 16, orbY - 16, orbSize + 32, orbSize + 32);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
        g2.setColor(new Color(100, 50, 200));
        g2.fillOval(orbX, orbY, orbSize, orbSize);

        // Queen silhouette
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g2.setColor(new Color(255, 200, 230));
        g2.fillRect(orbX + orbSize / 2 - 24, orbY + orbSize / 2 - 40, 48, 60);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
        g2.setColor(new Color(180, 140, 255));
        g2.setStroke(new BasicStroke(3));
        g2.drawOval(orbX, orbY, orbSize, orbSize);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    private void drawShatter(Graphics2D g2) {
        float progress = (float) shatterTimer / shatterMax;

        // Orb cracks and fades
        float alpha = 1f - progress;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.8f));
        g2.setColor(new Color(180, 100, 255));
        g2.fillOval(orbX, orbY, orbSize, orbSize);

        // Expanding shatter lines
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(new Color(255, 255, 255));
        g2.setStroke(new BasicStroke(2));
        int cx = orbX + orbSize / 2;
        int cy = orbY + orbSize / 2;
        int reach = (int)(progress * 300);
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            g2.drawLine(cx, cy,
                    cx + (int)(reach * Math.cos(angle)),
                    cy + (int)(reach * Math.sin(angle)));
        }

        // Flash white at end
        if (progress > 0.8f) {
            float flashAlpha = (progress - 0.8f) * 5f;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, flashAlpha));
            g2.setColor(Color.white);
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        }

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    public boolean isActive() { return active; }
    public boolean isSolved() { return solved; }
}
