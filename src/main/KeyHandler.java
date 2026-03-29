package main;

import java.awt.event.*;

public class KeyHandler implements KeyListener {

    public boolean upPressed, downPressed, leftPressed, rightPressed;
    public boolean attackPressed;
    public boolean interactPressed;
    public boolean usePotion;      // P key — consume one potion
    public boolean debugPressed;   // F1 — skip cutscene

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP)    upPressed    = true;
        if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN)  downPressed  = true;
        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT)  leftPressed  = true;
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) rightPressed = true;
        if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_Z) attackPressed   = true;
        if (code == KeyEvent.VK_E || code == KeyEvent.VK_ENTER) interactPressed = true;
        if (code == KeyEvent.VK_P)  usePotion    = true;
        if (code == KeyEvent.VK_F1) debugPressed = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP)    upPressed    = false;
        if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN)  downPressed  = false;
        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT)  leftPressed  = false;
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) rightPressed = false;
        if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_Z) attackPressed   = false;
        if (code == KeyEvent.VK_E || code == KeyEvent.VK_ENTER) interactPressed = false;
        if (code == KeyEvent.VK_P)  usePotion    = false;
        if (code == KeyEvent.VK_F1) debugPressed = false;
    }

    @Override public void keyTyped(KeyEvent e) {}
}
