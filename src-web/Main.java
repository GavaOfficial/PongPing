import game.PongGame;
import javax.swing.*;
import java.awt.*;

/**
 * WEB-OPTIMIZED Main class for CheerpJ browser deployment
 * Stripped down to bare essentials for maximum performance
 */
public class Main {

    public static void main(String[] args) {
        // Enable web mode
        System.setProperty("pongping.webmode", "true");
        System.out.println("[WEB] PongPing web mode starting...");

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
