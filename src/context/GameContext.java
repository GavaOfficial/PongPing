package context;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class GameContext {
        // Current dimensions (updated on resize)
    public static int BOARD_WIDTH = DimensionalContext.BASE_WIDTH;
    public static int BOARD_HEIGHT = DimensionalContext.BASE_HEIGHT;
    public static int PADDLE_WIDTH = 20;
    public static int PADDLE_HEIGHT = 80;
    public static int BALL_SIZE = 20;
    public static int MENU_PADDLE_WIDTH = 40;

    // Background selection variables
    public static int selectedBackground = 0; // Default background
    public static ArrayList<String> backgroundNames = new ArrayList<>(); // Dynamic background names
    public static ArrayList<Image> backgroundImages = new ArrayList<>(); // Loaded background images
    public static ArrayList<String> backgroundFilenames = new ArrayList<>(); // Original filenames for lazy loading
    public static int selectedBackgroundOption = 0; // Currently selected in background menu

    // Lazy loading flags (web mode optimization)
    public static boolean allBackgroundsLoaded = false; // Track if all backgrounds are loaded
    public static boolean allPaddleThemesLoaded = false; // Track if all paddle themes are loaded

    // Paddle selection variables
    // Separate paddle themes for blue (left) and red (right) paddles
    public static ArrayList<String> bluePaddleThemeNames = new ArrayList<>(); // Blue paddle themes
    public static ArrayList<BufferedImage> bluePaddleThemeImages = new ArrayList<>(); // Blue paddle images
    public static ArrayList<String> bluePaddleThemeFilenames = new ArrayList<>(); // Original filenames for lazy loading
    public static ArrayList<String> redPaddleThemeNames = new ArrayList<>(); // Red paddle themes
    public static ArrayList<BufferedImage> redPaddleThemeImages = new ArrayList<>(); // Red paddle images
    public static ArrayList<String> redPaddleThemeFilenames = new ArrayList<>(); // Original filenames for lazy loading

    // Legacy arrays for compatibility (will use blue themes for now)
    public static ArrayList<String> paddleThemeNames = new ArrayList<>(); // Available paddle themes
    public static ArrayList<BufferedImage> paddleThemeImages = new ArrayList<>(); // Loaded paddle images
    public static int selectedPaddleTheme = 0; // Currently selected left paddle theme
    public static int selectedRightPaddleTheme = 0; // Currently selected right paddle theme
    public static int previewPaddleY = 300; // Y position of preview paddle in selection screen

    // Game loop constants
    public static final int LOGIC_FPS = 60;
    public static final long LOGIC_TIME_STEP = 1000000000L / LOGIC_FPS; // nanoseconds
    public static volatile boolean gameRunning = false;
    public static Thread gameLoopThread;


    // Scale factors
    public static double scaleX = 1.0;
    public static double scaleY = 1.0;

}
