package context;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;

import static game.PongGame.getText;


public class SettingsContext {

    // Menu selection
    public static int selectedMenuItem = 0;
    public static String[] menuItems;
    public static boolean leftPaddleSelected = false; // Track if left paddle is selected in menu
    public static boolean rightPaddleSelected = false; // Track if right paddle is selected in menu

    // Game mode selection
    public static int selectedGameMode = 0;
    public static String[] gameModes = {"NORMALE", "CIRCLE"};
    public static String[] gameModeDescriptions = {
            "Modalità normale: gioco classico standard",
            "Circle Defense: difendi il cerchio centrale dalle palle!"
    };

    // Current game mode
    public static int currentGameMode = 0; // 0=Normal, 1=Circle
    public static boolean isUsingKeyboardNavigation = false; // Track if user is navigating with keyboard
    public static int hoveredMenuItem = -1; // Track which menu item is currently hovered (-1 = none)

    // Circle Mode settings
    public static int circleModeInitialHealth = 100; // Initial health for circle mode (default 100 HP)
    public static int circleMaxCombo = 0; // Max combo reached in Circle Mode
    public static int circleMaxScore = 0; // Max balls deflected in Circle Mode
    public static boolean wasInCircleMode = false; // Was user in Circle Mode when they closed the game


    // Audio settings
    public static int musicVolume = 50; // Volume musica (0-100)
    public static int effectsVolume = 75; // Volume effetti (0-100)
    public static boolean musicEnabled = true; // Musica attiva

    // Category system for settings
    public static String[] categoryNames = {"DIFFICOLTA", "IMPOSTAZIONI PADDLE", "COMANDI", "AUDIO", "LINGUA"};
    public static int selectedCategory = 0; // Currently selected category
    public static int selectedCategorySetting = 0; // Selected setting within category
    public static boolean inCategoryColumn = true; // true = left column (categories), false = right column (settings)
    public static double[] categoryAnimationProgress = {1.0, 0.0, 0.0, 0.0, 0.0}; // Animation progress for each category (1.0 = fully visible)

    // Settings organized by category (use stable identifiers)
    public static final String[][] categorySettings = {
            {"SETTING_AI_DIFFICULTY"},
            {"SETTING_PADDLE_SPEED", "SETTING_BALL_SPEED"},
            {"SETTING_P1_UP", "SETTING_P1_DOWN", "SETTING_P2_UP", "SETTING_P2_DOWN"},
            {"SETTING_MUSIC_VOLUME", "SETTING_EFFECTS_VOLUME", "SETTING_MUSIC_ACTIVE"},
            {"SETTING_GAME_LANGUAGE"}
    };

    // Stable identifiers list for mapping logic
    public static final String[] settingNames = {"SETTING_PADDLE_SPEED", "SETTING_AI_DIFFICULTY", "SETTING_BALL_SPEED", "SETTING_P1_UP", "SETTING_P1_DOWN", "SETTING_P2_UP", "SETTING_P2_DOWN"};
    public static String[] paddleSpeedOptions = {"LENTA", "MEDIA", "VELOCE"};
    public static String[] aiDifficultyOptions = {"FACILE", "NORMALE", "DIFFICILE", "ESPERTO", "IMPOSSIBILE"};
    // Ball speed is now numeric, no longer uses options array

    // Settings background animation
    public static double checkerboardOffset = 0.0;
    public static double glowIntensity = 0.6; // Fixed intensity for stable lighting
    // Clean mouse hover system - based on Java Swing best practices
    public static boolean mouseOnBackground = false;
    public static HoverState currentHoverState = HoverState.NONE;
    public static int hoveredCategory = -1;
    public static int hoveredSetting = -1;
    public static boolean isUsingKeyboardNavigationSettings = false; // Track keyboard navigation in settings

    // Hover state enum for clean state management
    public enum HoverState {
        NONE,           // No hover
        BACKGROUND,     // Hovering over background (clickable for themes)
        CATEGORY,       // Hovering over a category
        SETTING         // Hovering over a setting
    }

    // Settings file management
    public static final String SETTINGS_FILE = getSettingsFilePath();
    public static boolean isFirstRun = true;

    // Settings screen variables
    public static int selectedSetting = 0; // Current setting being modified
    public static int paddleSpeedSetting = 1; // 0 = Lenta, 1 = Media, 2 = Veloce
    public static int aiDifficultySetting = 2; // 0-4 (0 = Facile, 4 = Impossibile)
    public static int ballSpeedSetting = 25; // Velocità massima numerica (range 5-100)
    public static int player1UpKey = KeyEvent.VK_W; // Tasto su per player 1
    public static int player1DownKey = KeyEvent.VK_S; // Tasto giù per player 1
    public static int player2UpKey = KeyEvent.VK_UP; // Tasto su per player 2
    public static int player2DownKey = KeyEvent.VK_DOWN; // Tasto giù per player 2

    private static String getSettingsFilePath() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        String appDataPath;

        if (os.contains("win")) {
            // Windows: %APPDATA%\GavaTech\Pong-Ping\
            appDataPath = System.getenv("APPDATA");
            if (appDataPath == null) {
                appDataPath = userHome + File.separator + "AppData" + File.separator + "Roaming";
            }
        } else if (os.contains("mac")) {
            // macOS: ~/Library/Application Support/GavaTech/Pong-Ping/
            appDataPath = userHome + File.separator + "Library" + File.separator + "Application Support";
        } else {
            // Linux/Unix: ~/.config/GavaTech/Pong-Ping/
            appDataPath = userHome + File.separator + ".config";
        }

        // Create directory structure: GavaTech/Pong-Ping
        File directory = new File(appDataPath + File.separator + "GavaTech" + File.separator + "Pong-Ping");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        return directory.getAbsolutePath() + File.separator + "settings.properties";
    }

    // Right paddle combo system (player 2 - multiplayer only)
    public static int rightComboCount = 0;
    public static int rightMaxCombo = 0;

    // Advanced combo visual effects (left paddle)
    public static float comboScale = 1.0f;
    public static float comboPulse = 0.0f;
    public static float comboGlow = 0.0f;
    public static Color comboColor = Color.YELLOW;
    public static boolean comboMilestoneHit = false;
    public static int comboMilestoneTimer = 0;
    public static long lastComboTime = 0;

    // Combo visibility control (left paddle)
    public static boolean showCombo = false;
    public static int comboShowTimer = 0;
    public static final int COMBO_SHOW_DURATION = 90; // 1.5 seconds at 60 FPS

    // Right paddle combo visual effects
    public static float rightComboScale = 1.0f;
    public static float rightComboPulse = 0.0f;
    public static float rightComboGlow = 0.0f;
    public static Color rightComboColor = Color.YELLOW;
    public static boolean rightComboMilestoneHit = false;
    public static int rightComboMilestoneTimer = 0;
    public static long lastRightComboTime = 0;

    // Right combo visibility control
    public static boolean showRightCombo = false;
    public static int rightComboShowTimer = 0;

    // Cached glow colors to avoid concurrent access issues
    public static Color cachedLeftGlowColor = new Color(100, 150, 255, 100);
    public static Color cachedRightGlowColor = new Color(255, 100, 100, 100);

    public static void updateLocalizedArrays() {
        // Initialize and update menu items
        if (menuItems == null) {
            menuItems = new String[5];
        }
        menuItems[0] = getText("MENU_SINGLE_PLAYER");
        menuItems[1] = getText("MENU_TWO_PLAYERS");
        menuItems[2] = getText("MENU_HISTORY");
        menuItems[3] = getText("MENU_SETTINGS");
        menuItems[4] = getText("MENU_EXIT");

        // Update category names
        categoryNames[0] = getText("SETTINGS_DIFFICULTY");
        categoryNames[1] = getText("SETTINGS_PADDLE");
        categoryNames[2] = getText("SETTINGS_CONTROLS");
        categoryNames[3] = getText("SETTINGS_AUDIO");
        categoryNames[4] = getText("SETTINGS_LANGUAGE");

        // Update paddle speed options
        paddleSpeedOptions[0] = getText("PADDLE_SPEED_SLOW");
        paddleSpeedOptions[1] = getText("PADDLE_SPEED_MEDIUM");
        paddleSpeedOptions[2] = getText("PADDLE_SPEED_FAST");

        // Update AI difficulty options
        aiDifficultyOptions[0] = getText("AI_DIFFICULTY_EASY");
        aiDifficultyOptions[1] = getText("AI_DIFFICULTY_NORMAL");
        aiDifficultyOptions[2] = getText("AI_DIFFICULTY_HARD");
        aiDifficultyOptions[3] = getText("AI_DIFFICULTY_EXPERT");
        aiDifficultyOptions[4] = getText("AI_DIFFICULTY_IMPOSSIBLE");
    }

}
