package main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class UI {

    GamePanel gp;

    Font pixelFont_24;
    Font pixelFont_20;
    Font pixelFont_16;

    BufferedImage heartFull, heartHalf, heartEmpty;
    BufferedImage slotEmpty, slotFilled;
    BufferedImage roseImg, ringImg, tiaraImg, necklaceImg;
    BufferedImage shieldWood, shieldIron, shieldMagic;
    BufferedImage swordIcon;
    BufferedImage eggIcon;
    BufferedImage witchBtn;
    BufferedImage dialogueBox;
    BufferedImage witchPortrait;
    BufferedImage kingPortrait;
    BufferedImage advanceArrow;

    // Separate continuous timer for bounce — never resets
    private int bounceTimer = 0;

    // Witch button blink (separate from bounce)
    boolean advanceArrowVisible = true;
    int arrowBlinkCounter = 0;

    public boolean messageOn = false;
    public String message    = "";
    int messageCounter       = 0;

    public UI(GamePanel gp) {
        this.gp = gp;
        pixelFont_24 = new Font("Courier New", Font.BOLD, 24);
        pixelFont_20 = new Font("Courier New", Font.BOLD, 20);
        pixelFont_16 = new Font("Courier New", Font.BOLD, 16);
        loadImages();
    }

    private void loadImages() {
        heartFull   = loadImg("/ui/heart_full.png");
        heartHalf   = loadImg("/ui/heart_half.png");
        heartEmpty  = loadImg("/ui/heart_empty.png");
        slotEmpty   = loadImg("/ui/slot_empty.png");
        slotFilled  = loadImg("/ui/slot_filled.png");
        roseImg     = loadImg("/objects/rose.png");
        ringImg     = loadImg("/objects/ring.png");
        tiaraImg    = loadImg("/objects/tiara.png");
        necklaceImg = loadImg("/objects/necklace.png");
        shieldWood  = loadImg("/ui/shield_wood.png");
        shieldIron  = loadImg("/ui/shield_iron.png");
        shieldMagic = loadImg("/ui/shield_magic.png");
        swordIcon   = loadImg("/ui/sword_icon.png");
        eggIcon     = loadImg("/ui/egg_icon.png");
        witchBtn    = loadImg("/ui/witch_button.png");
        dialogueBox   = loadImg("/ui/dialogue_box.png");
        witchPortrait = loadImg("/characters/witch_portrait.png");
        kingPortrait  = loadImg("/characters/king_portrait.png");
        advanceArrow  = loadImg("/ui/advance_arrow.png");
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
        message = text; messageOn = true; messageCounter = 0;
    }

    public void draw(Graphics2D g2) {
        bounceTimer++; // always ticking — smooth sine wave
        switch (gp.gameState) {
            case OVERWORLD, BATTLE -> {
                drawHearts(g2); drawItemSlots(g2);
                drawEggCounter(g2); drawCombatHUD(g2); drawWitchButton(g2);
            }
            case DIALOGUE -> {
                drawHearts(g2); drawItemSlots(g2);
                drawCombatHUD(g2); drawWitchButton(g2); drawDialogueBox(g2);
            }
            case CUTSCENE, DREAM -> drawDialogueBox(g2);
            case PUZZLE -> {
                drawHearts(g2); drawItemSlots(g2);
                drawWitchButton(g2); drawDialogueBox(g2);
            }
        }
        if (messageOn) drawMessage(g2);
    }

    private void drawHearts(Graphics2D g2) {
        int size = 28, gap = 4, startX = 16, startY = 16;
        for (int i = 0; i < 10; i++) {
            int x = startX + i * (size + gap);
            if (heartEmpty != null) g2.drawImage(heartEmpty, x, startY, size, size, null);
            else { g2.setColor(new Color(80,20,20,180)); g2.fillRect(x,startY,size,size); }
            int hv = gp.player.health - (i * 2);
            if (hv >= 2) {
                if (heartFull != null) g2.drawImage(heartFull, x, startY, size, size, null);
                else { g2.setColor(new Color(220,50,50)); g2.fillRect(x,startY,size,size); }
            } else if (hv == 1) {
                if (heartHalf != null) g2.drawImage(heartHalf, x, startY, size, size, null);
                else { g2.setColor(new Color(220,50,50)); g2.fillRect(x,startY,size/2,size); }
            }
        }
    }

    private void drawItemSlots(Graphics2D g2) {
        int size = 28, gap = 6, startX = 16, startY = 52;
        BufferedImage[] items = { roseImg, ringImg, tiaraImg, necklaceImg };
        boolean[] collected   = {
                gp.player.hasRose, gp.player.hasRing,
                gp.player.hasTiara, gp.player.hasNecklace
        };
        for (int i = 0; i < 4; i++) {
            int x = startX + i * (size + gap);
            if (collected[i]) {
                if (slotFilled != null) g2.drawImage(slotFilled, x, startY, size, size, null);
                else {
                    g2.setColor(new Color(200,160,50,200)); g2.fillRoundRect(x,startY,size,size,4,4);
                    g2.setColor(new Color(255,200,80)); g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(x,startY,size,size,4,4); g2.setStroke(new BasicStroke(1));
                }
                if (items[i] != null) g2.drawImage(items[i], x+2, startY+2, size-4, size-4, null);
                else {
                    Color[] fb = { new Color(220,50,80), new Color(220,200,50),
                            new Color(180,100,220), new Color(100,200,220) };
                    g2.setColor(fb[i]); g2.fillOval(x+6, startY+6, size-12, size-12);
                }
            } else {
                if (slotEmpty != null) g2.drawImage(slotEmpty, x, startY, size, size, null);
                else {
                    g2.setColor(new Color(40,30,60,160)); g2.fillRoundRect(x,startY,size,size,4,4);
                    g2.setColor(new Color(100,80,140,180)); g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(x,startY,size,size,4,4); g2.setStroke(new BasicStroke(1));
                    g2.setFont(pixelFont_16); g2.setColor(new Color(255,255,255,60));
                    g2.drawString("?", x+size/2-5, startY+size/2+6);
                }
            }
        }
    }

    private void drawEggCounter(Graphics2D g2) {
        int iconSize = 20, x = 16, y = 88;
        if (eggIcon != null) g2.drawImage(eggIcon, x, y, iconSize, iconSize, null);
        else { g2.setColor(new Color(255,200,80)); g2.fillOval(x,y,iconSize,iconSize); }
        g2.setFont(pixelFont_16); g2.setColor(Color.white);
        g2.drawString(gp.player.eggsFound + " / 5", x+iconSize+6, y+iconSize-3);
    }

    private void drawCombatHUD(Graphics2D g2) {
        int size = 48, x = 16, y = gp.screenHeight - size - 16;
        if (swordIcon != null) g2.drawImage(swordIcon, x, y, size, size, null);
        else {
            g2.setColor(new Color(200,200,200,180)); g2.fillRect(x,y,size,size);
            g2.setFont(pixelFont_16); g2.setColor(Color.white); g2.drawString("SW",x+8,y+30);
        }
        int shieldX = x + size + 10;
        BufferedImage si = switch (gp.player.shieldLevel) {
            case 2 -> shieldIron; case 3 -> shieldMagic; default -> shieldWood;
        };
        if (si != null) g2.drawImage(si, shieldX, y, size, size, null);
        else {
            g2.setColor(new Color(100,160,220,180)); g2.fillRect(shieldX,y,size,size);
            g2.setFont(pixelFont_16); g2.setColor(Color.white); g2.drawString("SH",shieldX+8,y+30);
        }
        int barX=shieldX, barY=y+size+4, barW=size;
        int fillW=(int)(barW*((double)gp.player.shieldStrength/gp.player.maxShieldStrength));
        g2.setColor(new Color(40,40,40,180)); g2.fillRoundRect(barX,barY,barW,6,3,3);
        Color sc = switch(gp.player.shieldLevel){
            case 2->new Color(180,180,200); case 3->new Color(160,100,220); default->new Color(160,100,60);
        };
        g2.setColor(sc); g2.fillRoundRect(barX,barY,fillW,6,3,3);
        g2.setColor(new Color(255,255,255,50)); g2.drawRoundRect(barX,barY,barW,6,3,3);
    }

    private void drawWitchButton(Graphics2D g2) {
        int size = 48, x = gp.screenWidth-size-16, y = 16;
        arrowBlinkCounter++;
        if (arrowBlinkCounter > 30) { advanceArrowVisible=!advanceArrowVisible; arrowBlinkCounter=0; }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, advanceArrowVisible?1.0f:0.6f));
        if (witchBtn != null) g2.drawImage(witchBtn, x, y, size, size, null);
        else {
            g2.setColor(new Color(160,100,220,200)); g2.fillRoundRect(x,y,size,size,10,10);
            g2.setFont(pixelFont_16); g2.setColor(Color.white); g2.drawString("?",x+size/2-5,y+size/2+6);
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    // dialogue box
    public void drawDialogueBox(Graphics2D g2) {
        if (!gp.dialogueManager.isActive()) return;
        int boxH = 160, boxY = gp.screenHeight - boxH;

        if (dialogueBox != null) {
            g2.drawImage(dialogueBox, 0, boxY, gp.screenWidth, boxH, null);
        } else {
            g2.setColor(new Color(15,10,30,230)); g2.fillRect(0,boxY,gp.screenWidth,boxH);
            g2.setColor(new Color(150,100,200)); g2.setStroke(new BasicStroke(2));
            g2.drawRect(1,boxY+1,gp.screenWidth-2,boxH-2); g2.setStroke(new BasicStroke(1));
        }

        String speaker = gp.dialogueManager.getCurrentSpeaker();
        BufferedImage portrait = speaker.equals("witch") ? witchPortrait : kingPortrait;
        int ps=64, px=20, py=boxY+boxH/2-ps/2;
        if (portrait != null) g2.drawImage(portrait,px,py,ps,ps,null);
        else { g2.setColor(new Color(100,80,160,180)); g2.fillRect(px,py,ps,ps); }

        g2.setFont(pixelFont_20); g2.setColor(Color.white);
        drawWrappedText(g2, gp.dialogueManager.getDisplayText(),
                px+ps+20, boxY+44, gp.screenWidth-px-ps-90, 30);

        if (gp.dialogueManager.isLineComplete()) {
            float bounceSine = (float)(Math.sin(bounceTimer * 0.12));
            int bounceX = (int)(bounceSine * 5);

            int arrowW = 32;
            int arrowH = 32;
            int baseX = gp.screenWidth - arrowW - 20;
            int baseY = boxY + boxH/2 - arrowH/2;

            if (advanceArrow != null) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                g2.drawImage(advanceArrow, baseX + bounceX + 4, baseY + 4, arrowW, arrowH, null);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                g2.drawImage(advanceArrow, baseX + bounceX, baseY, arrowW, arrowH, null);
            } else {
                int ax = baseX + bounceX;
                int ay = baseY;
                int[] arrowXpts = { ax,      ax,      ax + arrowW };
                int[] arrowYpts = { ay,       ay + arrowH, ay + arrowH/2 };

                // Shadow
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                g2.setColor(Color.black);
                int[] sxpts = { ax+4,      ax+4,      ax + arrowW + 4 };
                int[] sypts = { ay+4,       ay + arrowH + 4, ay + arrowH/2 + 4 };
                g2.fillPolygon(sxpts, sypts, 3);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

                g2.setColor(new Color(255, 220, 80));
                g2.fillPolygon(arrowXpts, arrowYpts, 3);

                g2.setColor(new Color(255, 255, 180));
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(ax, ay, ax, ay + arrowH);
                g2.drawLine(ax, ay, ax + arrowW, ay + arrowH/2);
                g2.setStroke(new BasicStroke(1));

                g2.setColor(new Color(120, 80, 0));
                g2.drawPolygon(arrowXpts, arrowYpts, 3);
            }
        }
    }

    private void drawWrappedText(Graphics2D g2, String text, int x, int y, int maxWidth, int lineHeight) {
        if (text==null||text.isEmpty()) return;
        FontMetrics fm=g2.getFontMetrics();
        String[] words=text.split(" ");
        StringBuilder line=new StringBuilder();
        for (String word : words) {
            String test=line+word+" ";
            if (fm.stringWidth(test)>maxWidth&&line.length()>0) {
                g2.drawString(line.toString().trim(),x,y);
                y+=lineHeight; line=new StringBuilder(word+" ");
            } else line.append(word).append(" ");
        }
        if (line.length()>0) g2.drawString(line.toString().trim(),x,y);
    }

    private void drawMessage(Graphics2D g2) {
        g2.setFont(pixelFont_20);
        FontMetrics fm=g2.getFontMetrics();
        int tw=fm.stringWidth(message),px=16,py=10;
        int bx=gp.screenWidth/2-tw/2-px,by=gp.screenHeight/2-60;
        g2.setColor(new Color(0,0,0,180));
        g2.fillRoundRect(bx,by,tw+px*2,fm.getHeight()+py*2,10,10);
        g2.setColor(Color.yellow);
        g2.drawString(message,bx+px,by+py+fm.getAscent());
        messageCounter++;
        if (messageCounter>180){messageCounter=0;messageOn=false;}
    }
}
