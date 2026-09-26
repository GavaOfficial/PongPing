import game.PongGame;
import javax.swing.*;
import java.awt.*;

/**
 * Main class for the CheerpJ browser deployment.
 * Runs exactly the same game as the desktop version; only the window setup differs
 * (the browser page is the window, so the frame is undecorated and fills it).
 */
public class Main {

    public static void main(String[] args) {
        // Enable web mode
        System.setProperty("pongping.webmode", "true");
        System.out.println("[WEB] PongPing web mode starting...");

        // Save settings, progress and history in CheerpJ's persistent storage
        // (/files/ is kept in the browser's IndexedDB between sessions)
        System.setProperty("user.home", "/files");

        // Create frame without decorations (must be done before setVisible)
        JFrame frame = new JFrame("Pong Ping");
        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

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
