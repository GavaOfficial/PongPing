import game.PongGame;
import javax.swing.*;
import java.awt.*;

import static context.DimensionalContext.MIN_HEIGHT;
import static context.DimensionalContext.MIN_WIDTH;

/**
 * Entry point of the web version.
 * This file is translated to JavaScript together with the rest of src/ (see tools/build-web.sh)
 * and runs exactly the same game as the desktop version; only the window setup differs
 * (the browser page is the window, so the frame is undecorated and fills it).
 */
public class Main {

    public static void main(String[] args) {
        // Enable web mode
        System.setProperty("pongping.webmode", "true");
        System.out.println("[WEB] PongPing web mode starting...");

        // Settings, progress and history are saved under this folder, which the web
        // runtime keeps in the browser's storage between sessions
        System.setProperty("user.home", "/files");

        // Create frame without decorations (must be done before setVisible)
        JFrame frame = new JFrame("Pong Ping");
        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT)); // Same minimum size as the desktop window

        // Create and add game
        PongGame game = new PongGame();
        frame.add(game);

        // Fullscreen setup - simplified for browser
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);

        // Give focus to game
        game.requestFocus();

        System.out.println("[WEB] Ready!");
    }
}
