package context;

import game.PongGame;
import settings.HistorySettings;
import settings.LanguageSettings;
import settings.MusicSettings;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;

import static context.FontContext.*;
import static context.GameContext.*;
import static context.LanguageContext.*;

public class ContextLoader {

    public static void load(MusicSettings musicSettings, LanguageSettings languageSettings, HistorySettings historySettings) {
        loadFonts();
        loadBackgrounds(); // Load background images from temi/GameBack
        loadPaddleThemes(); // Load paddle themes from temi/Padle
        musicSettings.loadMusic();
        languageSettings.loadLanguage("italiano"); // Load default language (Italian)
        historySettings.loadGameHistory(); // Load game history
    }

    private static void loadFonts() {
        try {
            // Try to load fonts as resources from JAR
            InputStream primaryStream = ContextLoader.class.getClassLoader().getResourceAsStream("font/Silkscreen/Silkscreen-Regular.ttf");
            if (primaryStream != null) {
                primaryFont = Font.createFont(Font.TRUETYPE_FONT, primaryStream).deriveFont(32f);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(primaryFont);
                primaryStream.close();
            } else {
                // Fallback to file system (development mode)
                primaryFont = Font.createFont(Font.TRUETYPE_FONT,
                        new File(getResourcePath("font/Silkscreen/Silkscreen-Regular.ttf"))).deriveFont(32f);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(primaryFont);
            }

            InputStream secondaryStream = ContextLoader.class.getClassLoader().getResourceAsStream("font/Space_Mono/SpaceMono-Regular.ttf");
            if (secondaryStream != null) {
                secondaryFont = Font.createFont(Font.TRUETYPE_FONT, secondaryStream).deriveFont(16f);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(secondaryFont);
                secondaryStream.close();
            } else {
                // Fallback to file system (development mode)
                secondaryFont = Font.createFont(Font.TRUETYPE_FONT,
                        new File(getResourcePath("font/Space_Mono/SpaceMono-Regular.ttf"))).deriveFont(16f);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(secondaryFont);
            }

            // Load rank font (Bitcount Prop Double Bold)
            InputStream rankStream = ContextLoader.class.getClassLoader().getResourceAsStream("font/Bitcount_Prop_Double/static/BitcountPropDouble-Bold.ttf");
            if (rankStream != null) {
                rankFont = Font.createFont(Font.TRUETYPE_FONT, rankStream).deriveFont(48f);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(rankFont);
                rankStream.close();
            } else {
                // Fallback to file system (development mode)
                rankFont = Font.createFont(Font.TRUETYPE_FONT,
                        new File(getResourcePath("font/Bitcount_Prop_Double/static/BitcountPropDouble-Bold.ttf"))).deriveFont(48f);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(rankFont);
            }

            System.out.println("Custom fonts loaded successfully from JAR resources");

        } catch (FontFormatException | IOException e) {
            System.out.println("Could not load custom fonts, using default fonts: " + e.getMessage());
            primaryFont = new Font("Arial", Font.BOLD, 32);
            secondaryFont = new Font("Arial", Font.PLAIN, 16);
            rankFont = new Font("Arial", Font.BOLD, 48); // Fallback for rank font
        }
    }

    private static void loadBackgrounds() {
        try {
            // Add default black theme first
            backgroundImages.add(null); // null represents black background
            backgroundNames.add("Default (Black)");

            // Try to load from app context first (for jpackage apps with --app-content)
            String backgroundDirPath = getResourcePath("temi/GameBack");
            File backgroundDir = new File(backgroundDirPath);
            if (backgroundDir.exists() && backgroundDir.isDirectory()) {
                File[] files = backgroundDir.listFiles((dir, name) -> {
                    String lowerName = name.toLowerCase();
                    return lowerName.endsWith(".png") || lowerName.endsWith(".jpg") ||
                            lowerName.endsWith(".jpeg") || lowerName.endsWith(".gif") ||
                            lowerName.endsWith(".bmp");
                });

                if (files != null && files.length > 0) {
                    // Sort files alphabetically
                    java.util.Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

                    for (File file : files) {
                        try {
                            BufferedImage img = ImageIO.read(file);
                            if (img != null) {
                                backgroundImages.add(img);
                                // Remove file extension for display name
                                String name = file.getName();
                                int lastDot = name.lastIndexOf('.');
                                if (lastDot > 0) {
                                    name = name.substring(0, lastDot);
                                }
                                backgroundNames.add(name);
                                System.out.println("✓ Background loaded from app context: " + file.getName());
                            }
                        } catch (Exception e) {
                            System.out.println("Could not load background from app context: " + file.getName() + " - " + e.getMessage());
                        }
                    }
                }
            } else {
                // Fallback: try loading from JAR resources
                System.out.println("App context not found, trying JAR resources for backgrounds");
                String[] backgroundFiles = {
                        "Furry.jpg", "Natura.png", "Notte.png"
                };

                for (String filename : backgroundFiles) {
                    try {
                        InputStream imageStream = ContextLoader.class.getClassLoader().getResourceAsStream("temi/GameBack/" + filename);
                        if (imageStream != null) {
                            BufferedImage img = ImageIO.read(imageStream);
                            imageStream.close();

                            if (img != null) {
                                backgroundImages.add(img);
                                // Remove file extension for display name
                                String name = filename;
                                int lastDot = name.lastIndexOf('.');
                                if (lastDot > 0) {
                                    name = name.substring(0, lastDot);
                                }
                                backgroundNames.add(name);
                                System.out.println("✓ Background loaded from JAR: " + filename);
                            }
                        } else {
                            System.out.println("⚠️  Background file not found in app context or JAR: " + filename);
                        }
                    } catch (Exception e) {
                        System.out.println("Could not load background from JAR: " + filename + " - " + e.getMessage());
                    }
                }
            }

            // Add default background if no images found
            if (backgroundNames.size() <= 1) { // Only default was added
                System.out.println("⚠️  No background images found");
            }

        } catch (Exception e) {
            System.out.println("Error loading backgrounds: " + e.getMessage());
        }
    }

    private static void loadPaddleThemes() {
        try {
            // Load blue paddle themes (left paddle)
            loadPaddleThemesFromDirectory(getResourcePath("temi/Padle/Blu"), bluePaddleThemeNames, bluePaddleThemeImages, "Blue");

            // Load red paddle themes (right paddle)
            loadPaddleThemesFromDirectory(getResourcePath("temi/Padle/Rosso"), redPaddleThemeNames, redPaddleThemeImages, "Red");

            // Don't shuffle paddle themes to maintain consistent indices like backgrounds
            // shufflePaddleThemes();

            // Set up legacy arrays for compatibility (use blue themes as default)
            paddleThemeNames.clear();
            paddleThemeImages.clear();
            paddleThemeNames.addAll(bluePaddleThemeNames);
            paddleThemeImages.addAll(bluePaddleThemeImages);

        } catch (Exception e) {
            System.out.println("Error loading paddle themes: " + e.getMessage());
            // Fallback to default
            addDefaultPaddleTheme(bluePaddleThemeNames, bluePaddleThemeImages);
            addDefaultPaddleTheme(redPaddleThemeNames, redPaddleThemeImages);
            addDefaultPaddleTheme(paddleThemeNames, paddleThemeImages);
        }
    }


    /**
     * Get the correct resource path based on the runtime environment
     * - For jpackage apps (all platforms): Use app/ directory structure
     * - For JAR/development: Use relative paths
     */
    public static String getResourcePath(String relativePath) {
        try {
            // Get the path to the executable JAR
            String jarPath = PongGame.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();

            // Check if we're inside a jpackage app bundle (all platforms)
            // jpackage puts resources in app/ directory next to the JAR
            if (jarPath.contains("/app/") || jarPath.contains("\\app\\")) {
                // Extract the app directory path
                String appDirPath;
                if (jarPath.contains("/app/")) {
                    appDirPath = jarPath.substring(0, jarPath.indexOf("/app/") + 5);
                } else {
                    appDirPath = jarPath.substring(0, jarPath.indexOf("\\app\\") + 5);
                }

                String resourcePath = appDirPath + relativePath;
                File resourceFile = new File(resourcePath);

                if (resourceFile.exists()) {
                    System.out.println("Using jpackage app bundle resources: " + resourcePath);
                    return resourcePath;
                }
            }
        } catch (Exception e) {
            System.out.println("Could not detect app bundle, using default path: " + e.getMessage());
        }

        // Fallback to JAR resources or relative path
        System.out.println("Using default resource path: " + relativePath);
        return relativePath;
    }

    private static void loadPaddleThemesFromDirectory(String dirPath, ArrayList<String> themeNames,
                                               ArrayList<BufferedImage> themeImages, String colorName) {
        // Add default gradient theme first
        themeNames.add("Default (" + colorName + " Gradient)");
        themeImages.add(null); // null represents default gradient

        try {
            // Try to load from app context first (for jpackage apps with --app-content)
            File paddleDir = new File(dirPath);
            if (paddleDir.exists() && paddleDir.isDirectory()) {
                File[] files = paddleDir.listFiles((dir, name) -> {
                    String lowerName = name.toLowerCase();
                    return lowerName.endsWith(".png") || lowerName.endsWith(".jpg") ||
                            lowerName.endsWith(".jpeg") || lowerName.endsWith(".gif") ||
                            lowerName.endsWith(".bmp");
                });

                if (files != null && files.length > 0) {
                    // Sort files alphabetically
                    java.util.Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

                    for (File file : files) {
                        try {
                            BufferedImage img = ImageIO.read(file);
                            if (img != null) {
                                themeImages.add(img);
                                // Clean up the display name
                                String name = file.getName();
                                int lastDot = name.lastIndexOf('.');
                                if (lastDot > 0) {
                                    name = name.substring(0, lastDot);
                                }

                                // Remove "pixellab-" prefix if present
                                if (name.startsWith("pixellab-")) {
                                    name = name.substring("pixellab-".length());
                                }

                                // Remove everything after "--" (including color codes)
                                int doubleHyphenIndex = name.indexOf("--");
                                if (doubleHyphenIndex > 0) {
                                    name = name.substring(0, doubleHyphenIndex);
                                }

                                // Remove numeric suffixes (timestamps)
                                name = name.replaceAll("-\\d+$", "");

                                // Clean up remaining hyphens and make it more readable
                                name = name.replaceAll("-+", " ").trim();

                                // Capitalize first letter of each word
                                String[] words = name.split("\\s+");
                                StringBuilder cleanName = new StringBuilder();
                                for (String word : words) {
                                    if (word.length() > 0) {
                                        if (cleanName.length() > 0) cleanName.append(" ");
                                        cleanName.append(word.substring(0, 1).toUpperCase())
                                                .append(word.substring(1).toLowerCase());
                                    }
                                }

                                themeNames.add(cleanName.toString());
                                System.out.println("✓ " + colorName + " paddle theme loaded from app context: " + file.getName() + " -> " + cleanName.toString());
                            }
                        } catch (Exception e) {
                            System.out.println("Could not load " + colorName.toLowerCase() + " paddle theme from app context: " + file.getName() + " - " + e.getMessage());
                        }
                    }
                }
            } else {
                // Fallback: try loading from JAR resources
                System.out.println("App context not found, trying JAR resources for " + colorName.toLowerCase() + " paddle themes");

                // Create relative path for JAR resources
                String jarResourcePath = dirPath.replace("\\", "/");
                if (jarResourcePath.startsWith("temi/")) {
                    jarResourcePath = jarResourcePath.substring(5); // Remove "temi/" prefix if present
                }

                // List of known paddle files for this color (fallback list)
                String[] paddleFiles = {}; // Empty - will be populated based on actual JAR contents

                // Try to enumerate JAR contents
                try {
                    String fullJarPath = "temi/Padle/" + (colorName.equals("Blue") ? "Blu" : "Rosso");
                    URL resourceUrl = ContextLoader.class.getResource("/" + fullJarPath);
                    if (resourceUrl != null && resourceUrl.getProtocol().equals("jar")) {
                        String jarPath = resourceUrl.getPath().substring(5, resourceUrl.getPath().indexOf("!"));
                        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarPath)) {
                            java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                            java.util.List<String> foundFiles = new java.util.ArrayList<>();

                            while (entries.hasMoreElements()) {
                                java.util.jar.JarEntry entry = entries.nextElement();
                                String entryName = entry.getName();
                                if (entryName.startsWith(fullJarPath + "/") && !entry.isDirectory()) {
                                    String fileName = entryName.substring(entryName.lastIndexOf("/") + 1);
                                    String lowerName = fileName.toLowerCase();
                                    if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg") ||
                                            lowerName.endsWith(".jpeg") || lowerName.endsWith(".gif") ||
                                            lowerName.endsWith(".bmp")) {
                                        foundFiles.add(fileName);
                                    }
                                }
                            }
                            paddleFiles = foundFiles.toArray(new String[0]);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Could not enumerate JAR contents for " + colorName.toLowerCase() + " paddles: " + e.getMessage());
                }

                // Load each found file from JAR
                for (String filename : paddleFiles) {
                    try {
                        String resourcePath = "temi/Padle/" + (colorName.equals("Blue") ? "Blu" : "Rosso") + "/" + filename;
                        InputStream imageStream = ContextLoader.class.getClassLoader().getResourceAsStream(resourcePath);
                        if (imageStream != null) {
                            BufferedImage img = ImageIO.read(imageStream);
                            imageStream.close();

                            if (img != null) {
                                themeImages.add(img);
                                // Clean up the display name (same logic as above)
                                String name = filename;
                                int lastDot = name.lastIndexOf('.');
                                if (lastDot > 0) {
                                    name = name.substring(0, lastDot);
                                }

                                // Remove "pixellab-" prefix if present
                                if (name.startsWith("pixellab-")) {
                                    name = name.substring("pixellab-".length());
                                }

                                // Remove everything after "--" (including color codes)
                                int doubleHyphenIndex = name.indexOf("--");
                                if (doubleHyphenIndex > 0) {
                                    name = name.substring(0, doubleHyphenIndex);
                                }

                                // Remove numeric suffixes (timestamps)
                                name = name.replaceAll("-\\d+$", "");

                                // Clean up remaining hyphens and make it more readable
                                name = name.replaceAll("-+", " ").trim();

                                // Capitalize first letter of each word
                                String[] words = name.split("\\s+");
                                StringBuilder cleanName = new StringBuilder();
                                for (String word : words) {
                                    if (word.length() > 0) {
                                        if (cleanName.length() > 0) cleanName.append(" ");
                                        cleanName.append(word.substring(0, 1).toUpperCase())
                                                .append(word.substring(1).toLowerCase());
                                    }
                                }

                                themeNames.add(cleanName.toString());
                                System.out.println("✓ " + colorName + " paddle theme loaded from JAR: " + filename + " -> " + cleanName.toString());
                            }
                        } else {
                            System.out.println("⚠️  " + colorName + " paddle theme file not found in app context or JAR: " + filename);
                        }
                    } catch (Exception e) {
                        System.out.println("Could not load " + colorName.toLowerCase() + " paddle theme from JAR: " + filename + " - " + e.getMessage());
                    }
                }
            }

            // Add default theme if no images found
            if (themeNames.size() <= 1) { // Only default was added
                System.out.println("⚠️  No " + colorName.toLowerCase() + " paddle theme images found");
            }

        } catch (Exception e) {
            System.out.println("Error loading " + colorName.toLowerCase() + " paddle themes from " + dirPath + ": " + e.getMessage());
        }
    }

    private static void addDefaultPaddleTheme(ArrayList<String> themeNames, ArrayList<BufferedImage> themeImages) {
        themeNames.add("Default (Gradient)");
        themeImages.add(null);
    }

}
