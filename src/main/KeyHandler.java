package main;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {

    // Movement
    public boolean upPressed, downPressed, leftPressed, rightPressed;

    // Combat
    public boolean attackPressed;      // Space or Z — sword swing

    // Interaction
    public boolean interactPressed;    // Enter or E — advance dialogue / open chest
    public boolean witchHintPressed;   // H — trigger witch hint button

    // Debug (optional — remove before submission)
    public boolean debugPressed;       // F1 — skip cutscene during testing

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        // Movement
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP)    upPressed    = true;
        if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN)  downPressed  = true;
        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT)  leftPressed  = true;
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) rightPressed = true;

        // Attack — Space or Z
        if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_Z) attackPressed = true;

        // Interact / advance dialogue — Enter or E
        if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_E) interactPressed = true;

        // Witch hint — H
        if (code == KeyEvent.VK_H) witchHintPressed = true;

        // Debug skip — F1
        if (code == KeyEvent.VK_F1) debugPressed = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();

        // Movement
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP)    upPressed    = false;
        if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN)  downPressed  = false;
        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT)  leftPressed  = false;
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) rightPressed = false;

        // Attack — release allows next swing
        if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_Z) attackPressed = false;

        // Interact — release so it doesn't fire every frame
        if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_E) interactPressed = false;

        // Witch hint — release
        if (code == KeyEvent.VK_H) witchHintPressed = false;

        // Debug
        if (code == KeyEvent.VK_F1) debugPressed = false;
    }
}
