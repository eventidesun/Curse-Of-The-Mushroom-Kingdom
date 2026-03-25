package main;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) {
        JFrame window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.setTitle("The Curse of the Mushroom Kingdom");
        window.setUndecorated(true);

        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);
        window.pack();

        // Force true fullscreen using GraphicsDevice
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();

        if (gd.isFullScreenSupported()) {
            gd.setFullScreenWindow(window); // true exclusive fullscreen
        } else {
            window.setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        window.setVisible(true);
        gamePanel.setupGame();
        gamePanel.startGameThread();
    }
}
