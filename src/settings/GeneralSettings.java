package settings;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static context.AIContext.aiDifficulty;
import static context.GameContext.*;
import static context.GameContext.backgroundImages;
import static context.GameContext.bluePaddleThemeImages;
import static context.GameContext.redPaddleThemeImages;
import static context.GameContext.selectedBackground;
import static context.GameContext.selectedPaddleTheme;
import static context.GameContext.selectedRightPaddleTheme;
import static context.LanguageContext.currentLanguageCode;
import static context.SettingsContext.*;
import static context.SettingsContext.circleMaxCombo;
import static context.SettingsContext.circleMaxScore;
import static context.SettingsContext.wasInCircleMode;


public class GeneralSettings {

    private void loadSettings() {
        try {
            File file = new File(SETTINGS_FILE);
            if (file.exists()) {
                Properties props = new Properties();
                props.load(new FileInputStream(file));

                paddleSpeedSetting = Integer.parseInt(props.getProperty("paddleSpeed", "1"));
                aiDifficultySetting = Integer.parseInt(props.getProperty("aiDifficulty", "2"));
                ballSpeedSetting = Integer.parseInt(props.getProperty("ballSpeed", "25"));
                player1UpKey = Integer.parseInt(props.getProperty("player1UpKey", String.valueOf(KeyEvent.VK_W)));
                player1DownKey = Integer.parseInt(props.getProperty("player1DownKey", String.valueOf(KeyEvent.VK_S)));
                player2UpKey = Integer.parseInt(props.getProperty("player2UpKey", String.valueOf(KeyEvent.VK_UP)));
                player2DownKey = Integer.parseInt(props.getProperty("player2DownKey", String.valueOf(KeyEvent.VK_DOWN)));
                isFirstRun = false;
            } else {
                isFirstRun = true;
            }
        } catch (Exception e) {
            // If loading fails, use defaults and treat as first run
            resetToDefaults();
            isFirstRun = true;
        }
    }

    private void resetToDefaults() {
        paddleSpeedSetting = 1;
        aiDifficultySetting = 2;
        ballSpeedSetting = 25; // Default numeric speed
        player1UpKey = KeyEvent.VK_W;
        player1DownKey = KeyEvent.VK_S;
        player2UpKey = KeyEvent.VK_UP;
        player2DownKey = KeyEvent.VK_DOWN;
    }

    public void loadSettingsFromFile(LanguageSettings languageSettings) {
        System.out.println("DEBUG (Caricamento): Inizio caricamento impostazioni...");
        try {
            // Get user's app data directory
            String userHome = System.getProperty("user.home");
            String appDataPath;

            // Determine app data path based on OS
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                appDataPath = System.getenv("APPDATA");
                if (appDataPath == null) {
                    appDataPath = userHome + "\\AppData\\Roaming";
                }
            } else if (os.contains("mac")) {
                appDataPath = userHome + "/Library/Application Support";
            } else {
                appDataPath = userHome + "/.local/share";
            }

            // Find settings.properties file
            java.io.File gavaTechDir = new java.io.File(appDataPath, "GavaTech");
            java.io.File pongPingDir = new java.io.File(gavaTechDir, "Pong-Ping");
            java.io.File settingsFile = new java.io.File(pongPingDir, "settings.properties");

            System.out.println("DEBUG (Caricamento): Percorsi di ricerca:");
            System.out.println("  appDataPath: " + appDataPath);
            System.out.println("  gavaTechDir: " + gavaTechDir.getAbsolutePath() + " (exists: " + gavaTechDir.exists() + ")");
            System.out.println("  pongPingDir: " + pongPingDir.getAbsolutePath() + " (exists: " + pongPingDir.exists() + ")");
            System.out.println("  settingsFile: " + settingsFile.getAbsolutePath() + " (exists: " + settingsFile.exists() + ")");

            if (settingsFile.exists()) {
                System.out.println("DEBUG (Caricamento): File trovato, caricamento in corso...");
                java.util.Properties properties = new java.util.Properties();
                java.io.FileInputStream fis = new java.io.FileInputStream(settingsFile);
                properties.load(fis);
                fis.close();

                System.out.println("DEBUG (Caricamento): Properties caricate dal file:");
                for (String key : properties.stringPropertyNames()) {
                    System.out.println("  " + key + "=" + properties.getProperty(key));
                }

                // Load all settings
                paddleSpeedSetting = Integer.parseInt(properties.getProperty("paddle.speed", "1"));
                aiDifficultySetting = Integer.parseInt(properties.getProperty("ai.difficulty", "2"));
                ballSpeedSetting = Integer.parseInt(properties.getProperty("ball.speed", "25"));
                selectedPaddleTheme = Integer.parseInt(properties.getProperty("paddle.theme", "0"));
                selectedRightPaddleTheme = Integer.parseInt(properties.getProperty("right.paddle.theme", "0"));
                player1UpKey = Integer.parseInt(properties.getProperty("player1.up.key", String.valueOf(java.awt.event.KeyEvent.VK_W)));
                player1DownKey = Integer.parseInt(properties.getProperty("player1.down.key", String.valueOf(java.awt.event.KeyEvent.VK_S)));
                player2UpKey = Integer.parseInt(properties.getProperty("player2.up.key", String.valueOf(java.awt.event.KeyEvent.VK_UP)));
                player2DownKey = Integer.parseInt(properties.getProperty("player2.down.key", String.valueOf(java.awt.event.KeyEvent.VK_DOWN)));
                musicVolume = Integer.parseInt(properties.getProperty("music.volume", "50"));
                effectsVolume = Integer.parseInt(properties.getProperty("effects.volume", "75"));
                musicEnabled = Boolean.parseBoolean(properties.getProperty("music.enabled", "true"));
                selectedBackground = Integer.parseInt(properties.getProperty("selected.background", "0"));

                // Load language setting with validation
                String savedLanguage = properties.getProperty("language.code", "italiano");
                // Validate that the saved language is supported
                if (savedLanguage.equals("italiano") || savedLanguage.equals("inglese") || savedLanguage.equals("spagnolo")) {
                    currentLanguageCode = savedLanguage;
                } else {
                    System.out.println("⚠️  Invalid language code found: " + savedLanguage + ". Resetting to italiano.");
                    currentLanguageCode = "italiano";
                }
                // Actually load the language content and update localized arrays
                languageSettings.loadLanguage(currentLanguageCode);
                updateLocalizedArrays();

                // Load Circle Mode statistics
                circleMaxCombo = Integer.parseInt(properties.getProperty("circle.max.combo", "0"));
                circleMaxScore = Integer.parseInt(properties.getProperty("circle.max.score", "0"));

                // Load if user was in Circle Mode when they closed the game
                wasInCircleMode = Boolean.parseBoolean(properties.getProperty("was.in.circle.mode", "false"));

                // Validate selectedBackground index
                if (selectedBackground < 0 || selectedBackground >= backgroundImages.size()) {
                    selectedBackground = 0; // Reset to default if invalid
                }

                // Validate paddle theme indices
                if (selectedPaddleTheme < 0 || selectedPaddleTheme >= bluePaddleThemeImages.size()) {
                    selectedPaddleTheme = 0; // Reset to default if invalid
                }
                if (selectedRightPaddleTheme < 0 || selectedRightPaddleTheme >= redPaddleThemeImages.size()) {
                    selectedRightPaddleTheme = 0; // Reset to default if invalid
                }

                // Update cached glow colors after loading themes
                updateCachedGlowColors();

                System.out.println("DEBUG (Caricamento): Temi paddle caricati:");
                System.out.println("  selectedPaddleTheme=" + selectedPaddleTheme);
                System.out.println("  selectedRightPaddleTheme=" + selectedRightPaddleTheme);

                // Apply AI difficulty immediately after loading
                aiDifficulty = aiDifficultySetting + 1; // Convert to 1-5 range
                System.out.println("DEBUG (Caricamento): Valori caricati:");
                System.out.println("  paddleSpeedSetting=" + paddleSpeedSetting);
                System.out.println("  aiDifficultySetting=" + aiDifficultySetting + " (aiDifficulty=" + aiDifficulty + ")");
                System.out.println("  ballSpeedSetting=" + ballSpeedSetting);
                System.out.println("  language.code=" + currentLanguageCode);
                System.out.println("  musicVolume=" + musicVolume + ", effectsVolume=" + effectsVolume);

            } else {
                System.out.println("DEBUG (Caricamento): File settings.properties NON TROVATO. Usando valori di default.");
            }
        } catch (Exception e) {
            System.out.println("ERRORE nel caricare le impostazioni: " + e.getMessage());
            e.printStackTrace();
            // Set default AI difficulty if loading fails
            aiDifficulty = aiDifficultySetting + 1; // Convert to 1-5 range
            System.out.println("DEBUG (Errore): Difficoltà IA impostata a default: aiDifficultySetting=" + aiDifficultySetting + " (aiDifficulty=" + aiDifficulty + ")");
        }
    }

    public void updateCachedGlowColors() {
        // Update cached glow colors safely to avoid concurrent access during rendering
        try {
            cachedLeftGlowColor = getPaddleGlowColor(true);
            cachedRightGlowColor = getPaddleGlowColor(false);
        } catch (Exception e) {
            // Fallback to default colors if there's any issue
            cachedLeftGlowColor = new Color(100, 150, 255, 100);
            cachedRightGlowColor = new Color(255, 100, 100, 100);
        }
    }

    public Color getPaddleGlowColor(boolean isLeftPaddle) {
        if (isLeftPaddle) {
            // Left paddle glow color based on selected theme
            if (selectedPaddleTheme >= 0 && selectedPaddleTheme < bluePaddleThemeImages.size()) {
                BufferedImage paddleImg = bluePaddleThemeImages.get(selectedPaddleTheme);
                Color dominantColor = extractDominantColor(paddleImg);
                if (dominantColor != null) {
                    // Make the glow color slightly more vibrant and with some transparency
                    int r = Math.min(255, (int)(dominantColor.getRed() * 1.2));
                    int g = Math.min(255, (int)(dominantColor.getGreen() * 1.2));
                    int b = Math.min(255, (int)(dominantColor.getBlue() * 1.2));
                    return new Color(r, g, b, 100);
                }
            }
            // Fallback to default blue glow
            return new Color(100, 150, 255, 100);
        } else {
            // Right paddle glow color based on selected theme
            if (selectedRightPaddleTheme >= 0 && selectedRightPaddleTheme < redPaddleThemeImages.size()) {
                BufferedImage paddleImg = redPaddleThemeImages.get(selectedRightPaddleTheme);
                Color dominantColor = extractDominantColor(paddleImg);
                if (dominantColor != null) {
                    // Make the glow color slightly more vibrant and with some transparency
                    int r = Math.min(255, (int)(dominantColor.getRed() * 1.2));
                    int g = Math.min(255, (int)(dominantColor.getGreen() * 1.2));
                    int b = Math.min(255, (int)(dominantColor.getBlue() * 1.2));
                    return new Color(r, g, b, 100);
                }
            }
            // Fallback to default red glow
            return new Color(255, 100, 100, 100);
        }
    }

    private Color extractDominantColor(BufferedImage image) {
        if (image == null) return null;

        // Sample pixels from the image to find dominant color
        Map<Integer, Integer> colorCount = new HashMap<>();
        int sampleStep = Math.max(1, Math.max(image.getWidth(), image.getHeight()) / 20); // Sample every few pixels

        for (int y = 0; y < image.getHeight(); y += sampleStep) {
            for (int x = 0; x < image.getWidth(); x += sampleStep) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb, true);

                // Skip transparent or very dark pixels
                if (color.getAlpha() < 100 || (color.getRed() + color.getGreen() + color.getBlue()) < 100) {
                    continue;
                }

                // Group similar colors together (reduce precision to avoid too many variations)
                int groupedRgb = ((color.getRed() / 32) * 32) << 16 |
                        ((color.getGreen() / 32) * 32) << 8 |
                        (color.getBlue() / 32) * 32;

                colorCount.put(groupedRgb, colorCount.getOrDefault(groupedRgb, 0) + 1);
            }
        }

        if (colorCount.isEmpty()) return null;

        // Find the most common color
        int dominantRgb = colorCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);

        return new Color(dominantRgb);
    }

}
