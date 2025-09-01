import game.PongGame;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static context.DimensionalContext.MIN_HEIGHT;
import static context.DimensionalContext.MIN_WIDTH;

public class Main {

    public static void main(String[] args) {
        // Set application name for dock and taskbar
        System.setProperty("apple.awt.application.name", "Pong Ping");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Pong Ping");

        // Set system look and feel to match OS theme
        try {
            // Enable system theme (including dark mode on macOS)
            System.setProperty("apple.awt.application.appearance", "system");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Could not set system look and feel: " + e.getMessage());
        }

        JFrame frame = new JFrame("Pong Ping by Gava");
        PongGame game = new PongGame();

        // Set application icon (cross-platform compatible)
        setApplicationIcon(frame);

        frame.add(game);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                game.stopGameLoop();
                System.exit(0);
            }
        });
        frame.setResizable(true); // Allow resizing
        frame.setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT)); // Set minimum size
        frame.pack();
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);

        game.requestFocus();
    }

    /**
     * Sets the application icon in a cross-platform compatible way
     * Supports Windows, Mac, and Linux systems
     */
    private static void setApplicationIcon(JFrame frame) {
        List<Image> iconImages = new ArrayList<>();

        // Try to load icon from different sources and sizes
        String[] iconPaths = {
                "icon.png",        // Current directory
                "/icon.png",       // From JAR root
                "icons/icon.png",  // Icons subdirectory
                "/icons/icon.png"  // Icons subdirectory in JAR
        };

        // Common icon sizes for different platforms
        int[] iconSizes = {16, 20, 24, 32, 40, 48, 64, 128, 256};

        boolean iconLoaded = false;

        // Try to load the main icon
        for (String iconPath : iconPaths) {
            try {
                Image image = loadImageFromPath(iconPath);
                if (image != null) {
                    iconImages.add(image);

                    // Create scaled versions for different sizes
                    for (int size : iconSizes) {
                        Image scaledImage = image.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                        iconImages.add(scaledImage);
                    }

                    iconLoaded = true;
                    System.out.println("Icona caricata con successo da: " + iconPath);
                    break;
                }
            } catch (Exception e) {
                // Continue trying other paths
            }
        }

        if (!iconLoaded) {
            System.out.println("Impossibile caricare l'icona da tutti i percorsi tentati.");
            System.out.println("Percorsi tentati: " + String.join(", ", iconPaths));
            return;
        }

        // Set icon for the JFrame (works on Windows and Linux)
        if (!iconImages.isEmpty()) {
            frame.setIconImages(iconImages);
        }

        // Set icon for Mac dock (JDK 9+)
        try {
            // Use reflection to avoid compilation errors on older JDK versions
            Class<?> taskbarClass = Class.forName("java.awt.Taskbar");
            Object taskbar = taskbarClass.getMethod("getTaskbar").invoke(null);
            taskbarClass.getMethod("setIconImage", Image.class).invoke(taskbar, iconImages.get(0));
            System.out.println("Icona impostata per Mac dock");
        } catch (Exception e) {
            // Taskbar API not available or not supported on this platform
            System.out.println("Taskbar API non disponibile su questa piattaforma");
        }
    }

    /**
     * Loads an image from various sources (file system or JAR resources)
     */
    private static Image loadImageFromPath(String path) {
        try {
            // First try as a resource (from JAR) using ClassLoader
            InputStream resourceStream = PongGame.class.getClassLoader().getResourceAsStream(path.startsWith("/") ? path.substring(1) : path);
            if (resourceStream != null) {
                BufferedImage img = ImageIO.read(resourceStream);
                resourceStream.close();
                return img;
            }

            // Try with getResource as backup
            URL resourceUrl = PongGame.class.getResource(path);
            if (resourceUrl != null) {
                return ImageIO.read(resourceUrl);
            }

            // Then try as a file
            File iconFile = new File(path);
            if (iconFile.exists()) {
                return ImageIO.read(iconFile);
            }

            // Try using Toolkit as fallback
            return Toolkit.getDefaultToolkit().getImage(path);

        } catch (Exception e) {
            return null;
        }
    }

}
