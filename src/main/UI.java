package main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class UI {

    GamePanel gp;

    // Fonts
    Font pixelFont_24;
    Font pixelFont_20;
    Font pixelFont_16;

    // Heart images
    BufferedImage heartFull, heartHalf, heartEmpty;

    // Shield images
    BufferedImage shieldWood, shieldIron, shieldMagic;

    // Sword HUD icon
    BufferedImage swordIcon;

    // Egg counter icon
    BufferedImage eggIcon;

    // Witch hint button
    BufferedImage witchBtn;
    boolean advanceArrowVisible = true;
    int arrowBlinkCounter = 0;

    // Dialogue box
    BufferedImage dialogueBox;
    BufferedImage witchPortrait;
    BufferedImage kingPortrait;

    // Message popup
    public boolean messageOn = false;
    public String message = "";
    int messageCounter = 0;

    public UI(GamePanel gp) {
        this.gp = gp;
        pixelFont_24 = new Font("Courier New", Font.BOLD, 24);
        pixelFont_20 = new Font("Courier New", Font.BOLD, 20);
        pixelFont_16 = new Font("Courier New", Font.BOLD, 16);
        loadImages();
    }

    private void loadImages() {
        heartFull     = loadImg("/ui/heart_full.png");
        heartHalf     = loadImg("/ui/heart_half.png");
        heartEmpty    = loadImg("/ui/heart_empty.png");
        shieldWood    = loadImg("/ui/shield_wood.png");
        shieldIron    = loadImg("/ui/shield_iron.png");
        shieldMagic   = loadImg("/ui/shield_magic.png");
        swordIcon     = loadImg("/ui/sword_icon.png");
        eggIcon       = loadImg("/ui/egg_icon.png");
        witchBtn      = loadImg("/ui/witch_button.png");
        dialogueBox   = loadImg("/ui/dialogue_box.png");
        witchPortrait = loadImg("/characters/witch_portrait.png");
        kingPortrait  = loadImg("/characters/king_portrait.png");
    }

    private BufferedImage loadImg(String path) {
        try {
            return ImageIO.read(getClass().getResourceAsStream(path));
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("UI image not found: " + path);
            return null;
        }
    }

    public void showMessage(String text) {
        message = text;
        messageOn = true;
        messageCounter = 0;
    }

    // -----------------------------------------------
    // DRAW
    // -----------------------------------------------
    public void draw(Graphics2D g2) {
        switch (gp.gameState) {
            case OVERWORLD, BATTLE -> {
                drawHearts(g2);
                drawEggCounter(g2);
                drawCombatHUD(g2);
                drawWitchButton(g2);
            }
            case DIALOGUE -> {
                drawHearts(g2);
                drawCombatHUD(g2);
                drawWitchButton(g2);
                drawDialogueBox(g2);
            }
            case CUTSCENE, DREAM -> drawDialogueBox(g2);
            case PUZZLE -> {
                drawHearts(g2);
                drawWitchButton(g2);
                drawDialogueBox(g2);
            }
        }
        if (messageOn) drawMessage(g2);
    }

    // -----------------------------------------------
    // HEARTS — small, single row, top left
    // -----------------------------------------------
    private void drawHearts(Graphics2D g2) {
        int size   = 28;  // small — was 64, now 28
        int gap    = 4;
        int startX = 16;
        int startY = 16;

        for (int i = 0; i < 10; i++) {
            int x = startX + i * (size + gap);

            // Empty heart base
            if (heartEmpty != null) {
                g2.drawImage(heartEmpty, x, startY, size, size, null);
            } else {
                g2.setColor(new Color(80, 20, 20, 180));
                g2.fillRect(x, startY, size, size);
            }

            // Full or half overlay
            int heartValue = gp.player.health - (i * 2);
            if (heartValue >= 2) {
                if (heartFull != null) {
                    g2.drawImage(heartFull, x, startY, size, size, null);
                } else {
                    g2.setColor(new Color(220, 50, 50));
                    g2.fillRect(x, startY, size, size);
                }
            } else if (heartValue == 1) {
                if (heartHalf != null) {
                    g2.drawImage(heartHalf, x, startY, size, size, null);
                } else {
                    g2.setColor(new Color(220, 50, 50));
                    g2.fillRect(x, startY, size / 2, size);
                }
            }
        }
    }

    // -----------------------------------------------
    // EGG COUNTER — just below hearts
    // -----------------------------------------------
    private void drawEggCounter(Graphics2D g2) {
        int iconSize = 20;
        int x = 16;
        int y = 52; // 16 startY + 28 heart size + 8 gap

        if (eggIcon != null) {
            g2.drawImage(eggIcon, x, y, iconSize, iconSize, null);
        } else {
            g2.setColor(new Color(255, 200, 80));
            g2.fillOval(x, y, iconSize, iconSize);
        }

        g2.setFont(pixelFont_16);
        g2.setColor(Color.white);
        g2.drawString(gp.player.eggsFound + " / 5", x + iconSize + 6, y + iconSize - 3);
    }

    // -----------------------------------------------
    // COMBAT HUD — sword + shield, bottom left
    // -----------------------------------------------
    private void drawCombatHUD(Graphics2D g2) {
        int size = 48;
        int x    = 16;
        int y    = gp.screenHeight - size - 16;

        // Sword
        if (swordIcon != null) {
            g2.drawImage(swordIcon, x, y, size, size, null);
        } else {
            g2.setColor(new Color(200, 200, 200, 180));
            g2.fillRect(x, y, size, size);
            g2.setFont(pixelFont_16);
            g2.setColor(Color.white);
            g2.drawString("SW", x + 8, y + 30);
        }

        // Shield
        int shieldX    = x + size + 10;
        BufferedImage shieldImg = switch (gp.player.shieldLevel) {
            case 2  -> shieldIron;
            case 3  -> shieldMagic;
            default -> shieldWood;
        };
        if (shieldImg != null) {
            g2.drawImage(shieldImg, shieldX, y, size, size, null);
        } else {
            g2.setColor(new Color(100, 160, 220, 180));
            g2.fillRect(shieldX, y, size, size);
            g2.setFont(pixelFont_16);
            g2.setColor(Color.white);
            g2.drawString("SH", shieldX + 8, y + 30);
        }

        // Shield strength bar
        int barX  = shieldX;
        int barY  = y + size + 4;
        int barW  = size;
        int fillW = (int)(barW * ((double) gp.player.shieldStrength / gp.player.maxShieldStrength));

        g2.setColor(new Color(40, 40, 40, 180));
        g2.fillRoundRect(barX, barY, barW, 6, 3, 3);

        Color shieldColor = switch (gp.player.shieldLevel) {
            case 2  -> new Color(180, 180, 200);
            case 3  -> new Color(160, 100, 220);
            default -> new Color(160, 100, 60);
        };
        g2.setColor(shieldColor);
        g2.fillRoundRect(barX, barY, fillW, 6, 3, 3);
        g2.setColor(new Color(255, 255, 255, 50));
        g2.drawRoundRect(barX, barY, barW, 6, 3, 3);
    }

    // -----------------------------------------------
    // WITCH BUTTON — top right, pulsing
    // -----------------------------------------------
    private void drawWitchButton(Graphics2D g2) {
        int size = 48;
        int x    = gp.screenWidth - size - 16;
        int y    = 16;

        arrowBlinkCounter++;
        if (arrowBlinkCounter > 30) {
            advanceArrowVisible = !advanceArrowVisible;
            arrowBlinkCounter   = 0;
        }

        float alpha = advanceArrowVisible ? 1.0f : 0.6f;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        if (witchBtn != null) {
            g2.drawImage(witchBtn, x, y, size, size, null);
        } else {
            g2.setColor(new Color(160, 100, 220, 200));
            g2.fillRoundRect(x, y, size, size, 10, 10);
            g2.setFont(pixelFont_16);
            g2.setColor(Color.white);
            g2.drawString("?", x + size/2 - 5, y + size/2 + 6);
        }

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    // -----------------------------------------------
    // DIALOGUE BOX — bottom of screen
    // -----------------------------------------------
    public void drawDialogueBox(Graphics2D g2) {
        if (!gp.dialogueManager.isActive()) return;

        int boxH = 160;
        int boxY = gp.screenHeight - boxH;

        if (dialogueBox != null) {
            g2.drawImage(dialogueBox, 0, boxY, gp.screenWidth, boxH, null);
        } else {
            g2.setColor(new Color(15, 10, 30, 230));
            g2.fillRect(0, boxY, gp.screenWidth, boxH);
            g2.setColor(new Color(150, 100, 200));
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(1, boxY + 1, gp.screenWidth - 2, boxH - 2);
        }

        // Portrait
        BufferedImage portrait = gp.dialogueManager.getCurrentSpeaker().equals("witch")
                ? witchPortrait : kingPortrait;
        int portraitSize = 64;
        int portraitX    = 20;
        int portraitY    = boxY + boxH / 2 - portraitSize / 2;
        if (portrait != null) {
            g2.drawImage(portrait, portraitX, portraitY, portraitSize, portraitSize, null);
        } else {
            g2.setColor(new Color(100, 80, 160, 180));
            g2.fillRect(portraitX, portraitY, portraitSize, portraitSize);
        }

        // Text
        g2.setFont(pixelFont_20);
        g2.setColor(Color.white);
        String displayText = gp.dialogueManager.getDisplayText();
        int textX    = portraitX + portraitSize + 20;
        int textY    = boxY + 44;
        int maxWidth = gp.screenWidth - textX - 50;
        drawWrappedText(g2, displayText, textX, textY, maxWidth, 30);

        // Advance arrow
        if (gp.dialogueManager.isLineComplete() && advanceArrowVisible) {
            g2.setFont(pixelFont_16);
            g2.setColor(new Color(255, 220, 100));
            g2.drawString("▼", gp.screenWidth - 40, boxY + boxH - 14);
        }
    }

    private void drawWrappedText(Graphics2D g2, String text, int x, int y, int maxWidth, int lineHeight) {
        if (text == null || text.isEmpty()) return;
        FontMetrics fm     = g2.getFontMetrics();
        String[] words     = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String testLine = line + word + " ";
            if (fm.stringWidth(testLine) > maxWidth && line.length() > 0) {
                g2.drawString(line.toString().trim(), x, y);
                y += lineHeight;
                line = new StringBuilder(word + " ");
            } else {
                line.append(word).append(" ");
            }
        }
        if (line.length() > 0) g2.drawString(line.toString().trim(), x, y);
    }

    // -----------------------------------------------
    // MESSAGE POPUP — centred pill
    // -----------------------------------------------
    private void drawMessage(Graphics2D g2) {
        g2.setFont(pixelFont_20);
        FontMetrics fm = g2.getFontMetrics();
        int textW  = fm.stringWidth(message);
        int padX   = 16;
        int padY   = 10;
        int boxX   = gp.screenWidth  / 2 - textW / 2 - padX;
        int boxY   = gp.screenHeight / 2 - 60;
        int boxW   = textW + padX * 2;
        int boxH   = fm.getHeight() + padY * 2;

        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 10, 10);
        g2.setColor(Color.yellow);
        g2.drawString(message, boxX + padX, boxY + padY + fm.getAscent());

        messageCounter++;
        if (messageCounter > 180) {
            messageCounter = 0;
            messageOn      = false;
        }
    }
}
