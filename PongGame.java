import javax.swing.*;
import javax.sound.sampled.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.AlphaComposite;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Iterator;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.FileReader;


public class PongGame extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {
    
    // Game loop constants
    private static final int LOGIC_FPS = 60;
    private static final long LOGIC_TIME_STEP = 1000000000L / LOGIC_FPS; // nanoseconds
    protected volatile boolean gameRunning = false;
    private Thread gameLoopThread;
    // Base dimensions (reference for scaling)
    private static final int BASE_WIDTH = 800;
    private static final int BASE_HEIGHT = 600;
    private static final int MIN_WIDTH = 600;
    private static final int MIN_HEIGHT = 450;
    
    // Current dimensions (updated on resize)
    protected int BOARD_WIDTH = BASE_WIDTH;
    protected int BOARD_HEIGHT = BASE_HEIGHT;
    protected int PADDLE_WIDTH = 20;
    protected int PADDLE_HEIGHT = 80;
    protected int BALL_SIZE = 20;
    private int MENU_PADDLE_WIDTH = 40;
    
    // Scale factors
    protected double scaleX = 1.0;
    protected double scaleY = 1.0;
    
    protected static final int WINNING_SCORE = 10;
    
    // Game states
    protected enum GameState { SETTINGS, MENU, PLAYING, PAUSED, GAME_OVER, SINGLE_PLAYER, TRANSITIONING, BACKGROUND_SELECTION, PADDLE_SELECTION, RIGHT_PADDLE_SELECTION, FIRST_ACCESS, DEBUG, GAME_MODE_SELECTION, RANK, HISTORY }
    protected GameState currentState; // Will be set based on first run check
    
    // Helper method to change state with debug logging
    protected void setState(GameState newState) {
        if (currentState != newState) {
            System.out.println("DEBUG (StateChange): " + currentState + " → " + newState);
            currentState = newState;
            
            // Inizializza particelle di sfondo quando entriamo nel menu
            if (newState == GameState.MENU || newState == GameState.FIRST_ACCESS) {
                createBackgroundParticles();
            }
        }
    }
    
    // Game objects
    protected int paddle1Y = 250;
    protected int paddle2Y = 250;
    private int prevPaddle1Y = 250;
    private int prevPaddle2Y = 250;
    protected double ballX = 400;
    protected double ballY = 300;
    protected double ballVX = 4;
    protected double ballVY = 4;
    protected double maxBallSpeed = 20;
    
    // Base speeds (will be scaled)
    private final double BASE_BALL_SPEED = 3.0;  // Velocità base ottimizzata
    private final double BASE_MAX_BALL_SPEED = 12.0;  // Velocità massima ottimizzata  
    private final double BASE_MIN_BALL_SPEED = 2.0;  // Velocità minima ottimizzata
    
    // Ball trail system
    private List<Point2D> ballTrailPoints = new ArrayList<>();
    private static final int MAX_TRAIL_LENGTH = 12;
    
    // Screen shake system
    private double shakeX = 0;
    private double shakeY = 0;
    private double shakeIntensity = 0;
    private int shakeFrames = 0;
    private Random shakeRandom = new Random();
    
    // Dynamic paddle glow
    protected float leftPaddleGlow = 0.0f;
    protected float rightPaddleGlow = 0.0f;
    private static final float MAX_PADDLE_GLOW = 1.0f;
    private static final double GLOW_DISTANCE_THRESHOLD = 150.0;
    
    // Scaled ball speeds (calculated from base speeds)
    protected double minBallSpeed = 3.0;
    protected final double PADDLE_SPEED_TRANSFER = 0.3;
    private final double ANGLE_FACTOR = 15.0;
    
    // Scores and game stats
    protected int score1 = 0;
    protected int score2 = 0;
    private int rallies = 0;
    private int currentRallyHits = 0; // Count hits in current rally for speed progression
    private long gameStartTime;
    protected long gameEndTime = 0;
    protected String winner = "";
    
    // Debug mode variables
    private int debugScore1 = 0;
    private int debugScore2 = 0;
    private int debugCombos = 0;
    private int debugSelection = 0; // 0=score1, 1=score2, 2=combos
    private String[] debugLabels = {"Player 1 Score", "Player 2 Score", "Combos"};
    private String currentRank = "NOVICE";
    private Color rankColor = Color.WHITE;
    
    // Ranking system for single player
    protected String finalRank = "";
    protected boolean showRankScreen = false;
    protected int rankAnimationFrame = 0;
    
    // Rank screen animation phases
    private boolean rankPaddleTransitionComplete = false;
    private boolean rankTextTransitionStarted = false;
    private double rankPaddleProgress = 0.0; // 0.0 = posizione gioco, 1.0 = posizione rank
    private double rankTextProgress = 0.0; // 0.0 = fuori schermo destra, 1.0 = posizione finale
    
    // Scrolling text animation phases
    private boolean scrollingTextStarted = false;
    private boolean scrollingTextEntryComplete = false;
    private boolean showingDifficultyPhase = true;
    private boolean gameInfoTransitionStarted = false;
    private boolean difficultyHasBeenCovered = false; // Track if difficulty has been covered by scrolling text
    private double scrollingTextDropProgress = 0.0; // 0.0 = fuori schermo alto, 1.0 = posizione finale
    private double gameInfoSlideProgress = 0.0; // 0.0 = fuori schermo sinistra, 1.0 = completamente passato
    private int difficultyDisplayFrames = 0;
    
    // Fire ball system
    private int consecutivePaddleBounces = 0; // Count consecutive paddle bounces without wall hits
    private boolean isFireBallActive = false; // Fire effect active
    private boolean doublePointsActive = false; // Double points active (15+ bounces)
    private boolean unlimitedSpeedActive = false; // Unlimited speed active (20+ bounces)
    
    // Smooth fire transition system
    private float fireIntensity = 0.0f; // Current fire intensity (0.0 to 1.0)
    private float targetFireIntensity = 0.0f; // Target fire intensity
    private long lastFireUpdate = System.currentTimeMillis();
    
    // Combo system for single player (left paddle - player vs AI)
    private int comboCount = 0;
    private int maxCombo = 0;
    
    // Combo system for two players mode
    private int player1ComboCount = 0;
    private int player1MaxCombo = 0;
    private int player2ComboCount = 0; 
    private int player2MaxCombo = 0;
    
    // Adaptive AI tracking variables
    private int playerWinStreak = 0;  // Consecutive points won by player
    private int aiWinStreak = 0;      // Consecutive points won by AI
    private int lastPointWinner = 0;  // 1=player, 2=AI, 0=none yet
    
    // Advanced tracking for comprehensive AI adaptation
    private java.util.List<Long> rallyDurations = new java.util.ArrayList<>();  // Track rally lengths
    private java.util.List<Integer> rallyHitCounts = new java.util.ArrayList<>(); // Track hits per rally
    private java.util.List<Double> ballSpeedHistory = new java.util.ArrayList<>(); // Track ball speed progression
    private long currentRallyStartTime = 0;   // When current rally started
    private int consecutiveMissedShots = 0;   // Track player consistency
    private double averageRallyLength = 0.0;  // Moving average of rally performance // Best combo achieved this session
    
    // Right paddle combo system (player 2 - multiplayer only)
    private int rightComboCount = 0;
    private int rightMaxCombo = 0;
    
    // Advanced combo visual effects (left paddle)
    private float comboScale = 1.0f;
    private float comboPulse = 0.0f;
    private float comboGlow = 0.0f;
    private Color comboColor = Color.YELLOW;
    private boolean comboMilestoneHit = false;
    private int comboMilestoneTimer = 0;
    private long lastComboTime = 0;
    
    // Combo visibility control (left paddle)
    private boolean showCombo = false;
    private int comboShowTimer = 0;
    private static final int COMBO_SHOW_DURATION = 90; // 1.5 seconds at 60 FPS
    
    // Right paddle combo visual effects
    private float rightComboScale = 1.0f;
    private float rightComboPulse = 0.0f;
    private float rightComboGlow = 0.0f;
    private Color rightComboColor = Color.YELLOW;
    private boolean rightComboMilestoneHit = false;
    private int rightComboMilestoneTimer = 0;
    private long lastRightComboTime = 0;
    
    // Right combo visibility control
    private boolean showRightCombo = false;
    private int rightComboShowTimer = 0;
    
    // Cached glow colors to avoid concurrent access issues
    protected Color cachedLeftGlowColor = new Color(100, 150, 255, 100);
    protected Color cachedRightGlowColor = new Color(255, 100, 100, 100);
    
    // Object Pool for Particles (performance optimization)
    private java.util.Queue<Particle> particlePool = new java.util.ArrayDeque<>();
    private static final int MAX_POOL_SIZE = 100; // Maximum particles to keep in pool (reduced from 200)
    private static final int MAX_ACTIVE_PARTICLES = 50; // Limit active particles to prevent lag (reduced from 100)
    private int frameCounter = 0; // Counter for frame-based optimizations
    
    // Input tracking
    private boolean wPressed = false;
    private boolean sPressed = false;
    private boolean upPressed = false;
    private boolean downPressed = false;
    
    // Menu selection
    private int selectedMenuItem = 0;
    private String[] menuItems;
    private boolean leftPaddleSelected = false; // Track if left paddle is selected in menu
    private boolean rightPaddleSelected = false; // Track if right paddle is selected in menu
    
    // Game mode selection
    private int selectedGameMode = 0;
    private String[] gameModes = {"NORMALE"};
    private String[] gameModeDescriptions = {
        "Modalità normale: gioco classico standard"
    };
    
    // Current game mode
    private int currentGameMode = 0; // 0=Normal
    private boolean isUsingKeyboardNavigation = false; // Track if user is navigating with keyboard
    private int hoveredMenuItem = -1; // Track which menu item is currently hovered (-1 = none)
    
    // Pause system - simple approach
    private boolean isPaused = false;
    private boolean wasSinglePlayer = false; // Remember if we were in single player mode
    
    // Animated pause line
    private double pauseLineOffset = 0;
    private static final double PAUSE_LINE_SPEED = -2.0; // Negative = reverse direction
    
    // Pause transition animation
    private boolean isTransitioningToPause = false;
    private boolean isTransitioningFromPause = false;
    private double pauseTransitionProgress = 0.0; // 0.0 = game state, 1.0 = pause state
    private static final double PAUSE_TRANSITION_SPEED = 0.08; // Animation speed
    private double centerLineRotation = 0.0; // 0 = vertical, π/4 = diagonal
    private double scoreTranslationProgress = 0.0; // For score position animation
    
    // Saved ball velocity during pause transition
    private double pausedBallVX = 0.0;
    private double pausedBallVY = 0.0;
    
    // Rank to home transition animation
    private boolean isRankToHomeTransition = false;
    private double rankToHomeProgress = 0.0; // 0.0 = rank state, 1.0 = home state
    private static final double RANK_TO_HOME_SPEED = 0.035; // Animation speed (slower)
    
    // Paddle selection to home transition animation
    private boolean isPaddleToHomeTransition = false;
    private double paddleToHomeProgress = 0.0;
    
 // 0.0 = paddle state, 1.0 = home state
    private static final double PADDLE_TO_HOME_SPEED = 0.035; // Animation speed (same as rank)
    
    // Motivational message system
    private double pauseTimer = 0;
    private String currentMotivationalMessage = "";
    private double messageScrollOffset = 0;
    private boolean showMotivationalMessage = false;
    private static final double MESSAGE_SHOW_DELAY = 1; // Show immediately
    private static final double MESSAGE_SCROLL_SPEED = 2.0; // Positive speed
    
    // History system variables
    private java.util.List<GameHistoryEntry> gameHistory = new java.util.ArrayList<>();
    private int selectedHistoryMode = 0; // 0 = Single Player, 1 = Two Players
    private int selectedHistoryCard = 0; // Indice della card selezionata nella cronologia
    private int historyScrollOffset = 0; // Offset per scroll automatico delle card
    private static final String HISTORY_FILE = getHistoryFilePath();
    
    
    // Settings screen variables
    private int selectedSetting = 0; // Current setting being modified
    protected int paddleSpeedSetting = 1; // 0 = Lenta, 1 = Media, 2 = Veloce
    protected int aiDifficultySetting = 2; // 0-4 (0 = Facile, 4 = Impossibile)
    protected int ballSpeedSetting = 25; // Velocità massima numerica (range 5-100)
    protected int player1UpKey = KeyEvent.VK_W; // Tasto su per player 1
    protected int player1DownKey = KeyEvent.VK_S; // Tasto giù per player 1
    protected int player2UpKey = KeyEvent.VK_UP; // Tasto su per player 2
    protected int player2DownKey = KeyEvent.VK_DOWN; // Tasto giù per player 2
    
    // Audio settings
    private int musicVolume = 50; // Volume musica (0-100)
    private int effectsVolume = 75; // Volume effetti (0-100)
    private boolean musicEnabled = true; // Musica attiva
    
    // Category system for settings
    private String[] categoryNames = {"DIFFICOLTA", "IMPOSTAZIONI PADDLE", "COMANDI", "AUDIO", "LINGUA"};
    private int selectedCategory = 0; // Currently selected category
    private int selectedCategorySetting = 0; // Selected setting within category
    private boolean inCategoryColumn = true; // true = left column (categories), false = right column (settings)
    private double[] categoryAnimationProgress = {1.0, 0.0, 0.0, 0.0, 0.0}; // Animation progress for each category (1.0 = fully visible)
    
    // Paddle width animation variables for settings
    private double leftPaddleWidthProgress = 1.0; // 1.0 = expanded, 0.0 = normal
    private double rightPaddleWidthProgress = 0.0; // 1.0 = expanded, 0.0 = normal
    
    // Settings organized by category (use stable identifiers)
    private final String[][] categorySettings = {
        {"SETTING_AI_DIFFICULTY"},
        {"SETTING_PADDLE_SPEED", "SETTING_BALL_SPEED"},
        {"SETTING_P1_UP", "SETTING_P1_DOWN", "SETTING_P2_UP", "SETTING_P2_DOWN"},
        {"SETTING_MUSIC_VOLUME", "SETTING_EFFECTS_VOLUME", "SETTING_MUSIC_ACTIVE"},
        {"SETTING_GAME_LANGUAGE"}
    };
    
    // Stable identifiers list for mapping logic
    private final String[] settingNames = {"SETTING_PADDLE_SPEED", "SETTING_AI_DIFFICULTY", "SETTING_BALL_SPEED", "SETTING_P1_UP", "SETTING_P1_DOWN", "SETTING_P2_UP", "SETTING_P2_DOWN"};
    private String[] paddleSpeedOptions = {"LENTA", "MEDIA", "VELOCE"};
    private String[] aiDifficultyOptions = {"FACILE", "NORMALE", "DIFFICILE", "ESPERTO", "IMPOSSIBILE"};
    // Ball speed is now numeric, no longer uses options array
    
    // Demo mode variables
    private boolean isDemoMode = false;
    private double demoPaddleY = 300.0; // Keep double for precise positioning
    private double demoRedPaddleY = 300.0; // Red paddle position
    private double demoTransitionProgress = 0.0;
    
    // Settings background animation
    private double checkerboardOffset = 0.0;
    private double glowIntensity = 0.6; // Fixed intensity for stable lighting
    // Clean mouse hover system - based on Java Swing best practices
    private boolean mouseOnBackground = false;
    private HoverState currentHoverState = HoverState.NONE;
    private int hoveredCategory = -1;
    private int hoveredSetting = -1;
    private boolean isUsingKeyboardNavigationSettings = false; // Track keyboard navigation in settings
    
    // Hover state enum for clean state management
    private enum HoverState {
        NONE,           // No hover
        BACKGROUND,     // Hovering over background (clickable for themes)
        CATEGORY,       // Hovering over a category
        SETTING         // Hovering over a setting
    }
    
    private boolean isTransitioningToDemo = false;
    private boolean isTransitioningFromDemo = false;
    private boolean isTransitioningDemoToMenu = false;
    private double demoToMenuProgress = 0.0;
    
    // Field selection state
    private boolean isExitingDemo = false;
    private double demoExitProgress = 0.0;
    private boolean isFieldSelection = false;
    private boolean playerOnRight = false; // true if player chooses right field
    private boolean demoPaddleUpPressed = false;
    private boolean demoPaddleDownPressed = false;
    private boolean demoRedPaddleUpPressed = false;
    private boolean demoRedPaddleDownPressed = false;
    
    // Demo ball variables (same as game ball)
    private double demoBallX = 400.0;
    private double demoBallY = 300.0;
    private double demoBallVX = 4.0;
    private double demoBallVY = 2.0;
    // Demo ball size is now always BALL_SIZE (scaled)
    private double demoBallSpeed = 4.0;
    
    // Settings file management
    private static final String SETTINGS_FILE = getSettingsFilePath();
    private boolean isFirstRun = true;
    
    // FirstAccess theme carousel variables
    private double carouselOffset = 0.0;
    private long lastCarouselUpdate = 0;
    
    // Chess pattern animation variables
    private double chessAnimationTime = 0.0;
    private long lastChessUpdate = 0;
    
    // Localization system
    private Map<String, String> currentLanguage = new HashMap<>();
    private String currentLanguageCode = "italiano"; // Default language
    
    
    // AI variables
    private double aiTargetY = 250;
    private double aiCurrentVelocity = 0.0;
    protected double aiPaddleY = 250.0; // Smooth AI paddle position
    private int aiDifficulty = 3; // 1-5, higher = harder
    private double aiMaxSpeed = 4.0;
    private double aiAcceleration = 0.4;
    private double aiDeceleration = 0.85;
    private long lastAIUpdate = 0;
    private long lastBallDirectionChange = 0;
    private double aiReactionDelay = 0.2; // seconds
    private Random random = new Random();
    
    // Visual effects
    private ArrayList<Particle> particles = new ArrayList<>();
    private Color ballTrail = new Color(255, 255, 255, 100);
    
    // Menu animation variables
    private double menuPaddle1Y = 0;
    private double menuPaddle2Y = 0;
    private int menuPaddleHeight = BOARD_HEIGHT;
    private double transitionProgress = 0.0;
    private boolean isTransitioning = false;
    private GameState transitionTarget = GameState.PLAYING;
    
    // Home to themes transition variables
    private boolean isHomeToThemesTransition = false;
    private double homeToThemesProgress = 0.0;
    private double textFadeProgress = 1.0;
    private double paddleExitProgress = 0.0;
    private double themesPanelProgress = 0.0;
    
    // Home to paddle selection transition variables
    private boolean isHomeToPaddleTransition = false;
    private double homeToPaddleProgress = 0.0;
    private double paddleTextFadeProgress = 1.0;
    private double paddlePanelProgress = 0.0;
    private boolean isLeftPaddleTransition = true; // true for left paddle, false for right paddle
    
    // Home to settings transition variables
    private boolean isHomeToSettingsTransition = false;
    private double homeToSettingsProgress = 0.0;
    private double paddleTranslationProgress = 0.0; // Progress of paddles moving to settings position
    private double columnsTranslationProgress = 0.0; // Progress of settings columns sliding in
    private double checkerboardAppearProgress = 0.0; // Progress of checkerboard appearing from bottom to top
    private double checkerboardAnimationProgress = 0.0; // Progress of checkerboard animation after it's fully appeared
    
    // Settings to home transition variables (inverse of home to settings)
    private boolean isSettingsToHomeTransition = false;
    private double settingsToHomeProgress = 0.0;
    private double settingsPaddleTranslationProgress = 0.0; // Progress of paddles moving back to home position
    private double settingsColumnsTranslationProgress = 0.0; // Progress of settings columns sliding out
    private double settingsCheckerboardDisappearProgress = 0.0; // Progress of checkerboard disappearing from top to bottom
    private double settingsCheckerboardAnimationProgress = 0.0; // Progress of checkerboard animation while disappearing
    
    
    // Themes to home transition variables (inverse)
    private boolean isThemesToHomeTransition = false;
    private double themesToHomeProgress = 0.0;
    private double titleExitProgress = 0.0;
    private double panelExitProgress = 0.0;
    private double textAppearProgress = 0.0;
    private double paddleReturnProgress = 0.0; // Progress of paddles returning to menu position (used by themes transition)
    
    // Method to check if any transition is active
    private boolean isAnyTransitionActive() {
        return isTransitioning || 
               isHomeToThemesTransition || 
               isHomeToPaddleTransition || 
               isHomeToSettingsTransition ||
               isSettingsToHomeTransition ||
               isThemesToHomeTransition ||
               isTransitioningToDemo ||
               isTransitioningFromDemo ||
               isTransitioningDemoToMenu ||
               isRankToHomeTransition ||
               isPaddleToHomeTransition;
    }
    
    
    // Background selection variables
    protected int selectedBackground = 0; // Default background
    private ArrayList<String> backgroundNames = new ArrayList<>(); // Dynamic background names
    protected ArrayList<Image> backgroundImages = new ArrayList<>(); // Loaded background images
    private int selectedBackgroundOption = 0; // Currently selected in background menu
    
    // Paddle selection variables
    // Separate paddle themes for blue (left) and red (right) paddles
    private ArrayList<String> bluePaddleThemeNames = new ArrayList<>(); // Blue paddle themes
    protected ArrayList<BufferedImage> bluePaddleThemeImages = new ArrayList<>(); // Blue paddle images
    private ArrayList<String> redPaddleThemeNames = new ArrayList<>(); // Red paddle themes  
    protected ArrayList<BufferedImage> redPaddleThemeImages = new ArrayList<>(); // Red paddle images
    
    // Legacy arrays for compatibility (will use blue themes for now)
    private ArrayList<String> paddleThemeNames = new ArrayList<>(); // Available paddle themes
    private ArrayList<BufferedImage> paddleThemeImages = new ArrayList<>(); // Loaded paddle images
    protected int selectedPaddleTheme = 0; // Currently selected left paddle theme
    protected int selectedRightPaddleTheme = 0; // Currently selected right paddle theme
    private int previewPaddleY = 300; // Y position of preview paddle in selection screen
    
    // Sistema selezione paddle con smooth scrolling avanzato
    private double paddleGridScrollY = 0.0; // Scroll verticale per paddle sinistro (double per precisione)
    private double rightPaddleGridScrollY = 0.0; // Scroll verticale per paddle destro (double per precisione)
    private static final int PADDLE_COLS = 4; // 4 colonne fisse
    
    // Advanced smooth scrolling system - basato su best practices web e game development
    private static final double SCROLL_SENSITIVITY = 2.0; // Sensibilità scroll (pixel per wheel tick)
    private static final double SCROLL_SMOOTHING = 0.88; // Fattore di smoothing/friction (0.85-0.95 ottimale)
    private static final int SCROLL_ANIMATION_FPS = 60; // FPS per animazione scroll
    private static final long SCROLL_ANIMATION_INTERVAL = 1000 / SCROLL_ANIMATION_FPS; // millisecondi
    private static final double MIN_SCROLL_VELOCITY = 0.1; // Velocità minima prima di fermarsi
    
    // Smooth scrolling state variables
    private double targetScrollY = 0.0; // Target scroll per paddle sinistro
    private double targetRightScrollY = 0.0; // Target scroll per paddle destro  
    private double scrollVelocityY = 0.0; // Velocità scroll paddle sinistro
    private double rightScrollVelocityY = 0.0; // Velocità scroll paddle destro
    private javax.swing.Timer scrollAnimationTimer; // Timer per animazione smooth
    private boolean isScrollingLeft = false; // Flag scroll paddle sinistro
    private boolean isScrollingRight = false; // Flag scroll paddle destro
    
    // Dynamic grid size calculation
    private int calculateGridCols() {
        // Base on window width - maintain proper card aspect ratio
        int panelWidth = BOARD_WIDTH / 2;
        int cardMargin = (int)(15 * Math.min(scaleX, scaleY));
        int cardSpacing = (int)(10 * Math.min(scaleX, scaleY));
        
        // Optimal card dimensions for paddle preview (maintain aspect ratio)
        int optimalCardWidth = (int)(100 * Math.min(scaleX, scaleY)); // Optimal width for paddle display
        
        // Calculate maximum columns that fit with optimal size
        int availableWidth = panelWidth - (2 * cardMargin);
        int maxCols = Math.max(2, (availableWidth + cardSpacing) / (optimalCardWidth + cardSpacing));
        
        // Limit to reasonable maximum
        return Math.min(maxCols, 5);
    }
    
    private int calculateGridRows() {
        // Base on window height - maintain proper card aspect ratio  
        int panelHeight = BOARD_HEIGHT;
        int gridStartY = (int)(80 * scaleY);
        int cardMargin = (int)(15 * Math.min(scaleX, scaleY));
        int cardSpacing = (int)(10 * Math.min(scaleX, scaleY));
        int scrollAreaHeight = (int)(60 * scaleY); // Space for scroll indicators
        
        // Optimal card dimensions for paddle preview (maintain aspect ratio)
        int optimalCardHeight = (int)(110 * Math.min(scaleX, scaleY)); // Optimal height for paddle display
        
        // Calculate maximum rows that fit with optimal size
        int availableHeight = panelHeight - gridStartY - cardMargin - scrollAreaHeight;
        int maxRows = Math.max(2, (availableHeight + cardSpacing) / (optimalCardHeight + cardSpacing));
        
        // Limit to reasonable maximum
        return Math.min(maxRows, 4);
    }
    
    private int getGridPageSize() {
        return calculateGridCols() * calculateGridRows();
    }
    
    // Paddle selection input tracking (same as game)
    private boolean paddleSelectionUpPressed = false;
    private boolean paddleSelectionDownPressed = false;
    
    // Text color configuration system
    private java.util.Map<String, Color> currentTextColors = new java.util.HashMap<>();
    private boolean advancedTextMode = false;
    
    // Cross-platform mouse click detection
    private int mouseClickStartX;
    private int mouseClickStartY;
    private boolean mouseClickStarted = false;
    
    // Mouse cursor visibility management
    private boolean isMouseVisible = true;
    private int lastMouseX = -1;
    private int lastMouseY = -1;
    private Cursor invisibleCursor;
    private Cursor defaultCursor;
    
    // Menu background ball
    private double menuBallX = 400;
    private double menuBallY = 300;
    private double menuBallVX = 3;
    private double menuBallVY = 3;
    private int menuBallSize = 45; // Increased size for better visibility
    
    // Second menu ball for Two Players selection
    private double menuBall2X = 400;
    private double menuBall2Y = 300;
    private double menuBall2VX = 3;
    private double menuBall2VY = 3;
    private boolean menuBall2Active = false;
    private boolean menuBall2Falling = false;
    
    // Component listener for resize events
    private boolean needsResize = false;
    
    // Fonts
    protected Font primaryFont;
    private Font secondaryFont;
    private Font rankFont; // Font for rank display
    
    // Audio
    private Clip backgroundMusic;
    
    private Timer gameTimer;
    
    // Particle class for visual effects
    private class Particle {
        double x, y, vx, vy;
        int life, maxLife;
        Color color;
        boolean infinite;
        
        Particle(double x, double y, double vx, double vy, int life, Color color) {
            this(x, y, vx, vy, life, color, false);
        }
        
        Particle(double x, double y, double vx, double vy, int life, Color color, boolean infinite) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = this.maxLife = life;
            this.color = color;
            this.infinite = infinite;
        }
        
        // Reset method for object pooling
        void reset(double x, double y, double vx, double vy, int life, Color color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = this.maxLife = life;
            this.color = color;
            this.infinite = false; // Fire particles are not infinite
        }
        
        void update() {
            x += vx;
            y += vy;
            
            if (infinite) {
                // Rimbalza sui bordi per particelle infinite
                if (x <= 0 || x >= BOARD_WIDTH) {
                    vx = -vx;
                    x = Math.max(0, Math.min(BOARD_WIDTH, x));
                }
                if (y <= 0 || y >= BOARD_HEIGHT) {
                    vy = -vy;
                    y = Math.max(0, Math.min(BOARD_HEIGHT, y));
                }
                // Mantieni velocità costante per particelle infinite
                double speed = Math.sqrt(vx * vx + vy * vy);
                if (speed > 0) {
                    double targetSpeed = 1.5;
                    vx = (vx / speed) * targetSpeed;
                    vy = (vy / speed) * targetSpeed;
                }
            } else {
                // Comportamento normale per particelle temporanee
                vx *= 0.98;
                vy *= 0.98;
                life--;
            }
        }
        
        void draw(Graphics2D g) {
            if (infinite) {
                // Particelle infinite sempre visibili
                g.setColor(color);
                g.fillOval((int)x - 2, (int)y - 2, 4, 4);
                // Aggiunge effetto glow
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));
                g.fillOval((int)x - 4, (int)y - 4, 8, 8);
            } else {
                // Particelle temporanee con fade
                float alpha = (float) life / maxLife;
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 
                    (int)(alpha * color.getAlpha())));
                g.fillOval((int)x - 2, (int)y - 2, 4, 4);
            }
        }
        
        boolean isDead() {
            return !infinite && life <= 0;
        }
    }
    
    public PongGame() {
        this.setPreferredSize(new Dimension(BOARD_WIDTH, BOARD_HEIGHT));
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.addKeyListener(this);
        this.addMouseListener(this);
        this.addMouseWheelListener(this);
        this.addMouseMotionListener(this);
        
        // Disable automatic TAB focus traversal to allow manual TAB handling
        this.setFocusTraversalKeysEnabled(false);
        
        // Add component listener for resize events
        this.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                updateDimensions();
            }
        });
        
        loadFonts();
        loadBackgrounds(); // Load background images from temi/GameBack
        loadPaddleThemes(); // Load paddle themes from temi/Padle
        loadLanguage("italiano"); // Load default language (Italian)
        updateLocalizedArrays(); // Initialize localized strings
        loadSettingsFromFile(); // Load settings before music to apply volume
        loadTextColorsForTheme(); // Load text colors for current theme
        loadGameHistory(); // Load game history
        initializeMouseCursors(); // Initialize cursor visibility system
        loadMusic();
        updateDimensions();
        
        // Determine if this is first run (no settings file exists)
        isFirstRun = !settingsFileExists();
        if (isFirstRun) {
            currentState = GameState.FIRST_ACCESS;
            // First run - show first access setup screen
            isDemoMode = false;
            isTransitioningToDemo = false;
            isTransitioningFromDemo = false;
            demoTransitionProgress = 0.0;
        } else {
            currentState = GameState.MENU;
        }
        
        // Initialize menu ball position and velocity scaled to screen size
        menuBallX = BOARD_WIDTH / 2 - menuBallSize / 2;
        menuBallY = BOARD_HEIGHT / 2 - menuBallSize / 2;
        double initialSpeed = 4.5 * Math.min(scaleX, scaleY); // Velocità aumentata
        menuBallVX = (Math.random() > 0.5) ? initialSpeed : -initialSpeed;
        menuBallVY = initialSpeed;
        
        gameTimer = new Timer(16, this); // Still used for resize animations only
        
        // Inizializza smooth scrolling animation timer
        initializeScrollAnimation();
        
        startGameLoop();
    }
    
    // Animation variables for smooth resizing
    private Timer resizeTimer;
    private double targetScaleX, targetScaleY;
    private double currentAnimatedScaleX, currentAnimatedScaleY;
    private int targetWidth, targetHeight;
    private int currentAnimatedWidth, currentAnimatedHeight;
    private static final int RESIZE_ANIMATION_DURATION = 200; // milliseconds
    private static final double RESIZE_SMOOTHING_FACTOR = 0.15;
    
    // Animation variables for difficulty effects
    private double difficultyAnimationTime = 0.0;
    
    private void updateDimensions() {
        Dimension size = getSize();
        if (size.width < MIN_WIDTH || size.height < MIN_HEIGHT) {
            // Enforce minimum size
            size.width = Math.max(size.width, MIN_WIDTH);
            size.height = Math.max(size.height, MIN_HEIGHT);
        }
        
        // Set target dimensions
        targetWidth = size.width;
        targetHeight = size.height;
        targetScaleX = (double) targetWidth / BASE_WIDTH;
        targetScaleY = (double) targetHeight / BASE_HEIGHT;
        
        // Initialize animated values if not set
        if (currentAnimatedWidth == 0) {
            currentAnimatedWidth = targetWidth;
            currentAnimatedHeight = targetHeight;
            currentAnimatedScaleX = targetScaleX;
            currentAnimatedScaleY = targetScaleY;
            updateDimensionsImmediate();
            return;
        }
        
        // Start or restart animation timer
        if (resizeTimer != null) {
            resizeTimer.stop();
        }
        
        resizeTimer = new Timer(16, new ActionListener() { // 60 FPS animation
            @Override
            public void actionPerformed(ActionEvent e) {
                // Smooth interpolation towards target
                currentAnimatedScaleX += (targetScaleX - currentAnimatedScaleX) * RESIZE_SMOOTHING_FACTOR;
                currentAnimatedScaleY += (targetScaleY - currentAnimatedScaleY) * RESIZE_SMOOTHING_FACTOR;
                currentAnimatedWidth += (targetWidth - currentAnimatedWidth) * RESIZE_SMOOTHING_FACTOR;
                currentAnimatedHeight += (targetHeight - currentAnimatedHeight) * RESIZE_SMOOTHING_FACTOR;
                
                // Check if animation is complete (close enough to target)
                if (Math.abs(currentAnimatedScaleX - targetScaleX) < 0.001 && 
                    Math.abs(currentAnimatedScaleY - targetScaleY) < 0.001) {
                    currentAnimatedScaleX = targetScaleX;
                    currentAnimatedScaleY = targetScaleY;
                    currentAnimatedWidth = targetWidth;
                    currentAnimatedHeight = targetHeight;
                    resizeTimer.stop();
                }
                
                updateDimensionsImmediate();
            }
        });
        resizeTimer.start();
    }
    
    /**
     * Get the correct resource path based on the runtime environment
     * - For jpackage apps (all platforms): Use app/ directory structure
     * - For JAR/development: Use relative paths
     */
    private String getResourcePath(String relativePath) {
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
    
    private void updateDimensionsImmediate() {
        BOARD_WIDTH = (int) currentAnimatedWidth;
        BOARD_HEIGHT = (int) currentAnimatedHeight;
        
        // Calculate scale factors using animated values
        scaleX = currentAnimatedScaleX;
        scaleY = currentAnimatedScaleY;
        
        // Update scaled dimensions
        PADDLE_WIDTH = (int) (20 * Math.min(scaleX, scaleY));
        PADDLE_HEIGHT = (int) (80 * scaleY);
        BALL_SIZE = (int) (20 * Math.min(scaleX, scaleY));
        MENU_PADDLE_WIDTH = PADDLE_WIDTH * 2;
        menuPaddleHeight = BOARD_HEIGHT;
        
        // Scale ball speeds based on window size
        double speedScale = Math.min(scaleX, scaleY);
        double oldMaxBallSpeed = maxBallSpeed;
        double oldMinBallSpeed = minBallSpeed;
        
        maxBallSpeed = BASE_MAX_BALL_SPEED * speedScale;
        minBallSpeed = BASE_MIN_BALL_SPEED * speedScale;
        
        // Scale current ball velocities proportionally if the ball is moving
        if (currentState == GameState.PLAYING || currentState == GameState.SINGLE_PLAYER) {
            if (oldMaxBallSpeed > 0 && (ballVX != 0 || ballVY != 0)) {
                double velocityScale = maxBallSpeed / oldMaxBallSpeed;
                ballVX *= velocityScale;
                ballVY *= velocityScale;
            }
        }
        
        // Scale demo ball velocities in paddle selection screens
        if (currentState == GameState.PADDLE_SELECTION || currentState == GameState.RIGHT_PADDLE_SELECTION) {
            if (oldMaxBallSpeed > 0 && (demoBallVX != 0 || demoBallVY != 0)) {
                double velocityScale = maxBallSpeed / oldMaxBallSpeed;
                demoBallVX *= velocityScale;
                demoBallVY *= velocityScale;
                // Also update the base demo ball speed
                demoBallSpeed *= velocityScale;
            }
        }
        
        // Update paddle positions to stay within bounds with smooth transition
        paddle1Y = Math.max(0, Math.min(paddle1Y, BOARD_HEIGHT - PADDLE_HEIGHT));
        paddle2Y = Math.max(0, Math.min(paddle2Y, BOARD_HEIGHT - PADDLE_HEIGHT));
        aiPaddleY = Math.max(0, Math.min(aiPaddleY, BOARD_HEIGHT - PADDLE_HEIGHT));
        
        // Update ball position to stay within bounds with smooth transition
        ballX = Math.max(0, Math.min(ballX, BOARD_WIDTH - BALL_SIZE));
        ballY = Math.max(0, Math.min(ballY, BOARD_HEIGHT - BALL_SIZE));
        
        // Update demo ball and paddle positions to stay within bounds
        demoBallX = Math.max(0, Math.min(demoBallX, BOARD_WIDTH - BALL_SIZE));
        demoBallY = Math.max(0, Math.min(demoBallY, BOARD_HEIGHT - BALL_SIZE));
        demoPaddleY = Math.max(0, Math.min(demoPaddleY, BOARD_HEIGHT - PADDLE_HEIGHT));
        demoRedPaddleY = Math.max(0, Math.min(demoRedPaddleY, BOARD_HEIGHT - PADDLE_HEIGHT));
        previewPaddleY = Math.max(0, Math.min(previewPaddleY, BOARD_HEIGHT - PADDLE_HEIGHT));
        
        // Update menu ball size and scale velocity for current screen size
        menuBallSize = (int) (45 * Math.min(scaleX, scaleY)); // Increased base size
        menuBallX = Math.max(0, Math.min(menuBallX, BOARD_WIDTH - menuBallSize));
        menuBallY = Math.max(0, Math.min(menuBallY, BOARD_HEIGHT - menuBallSize));
        
        // Scale menu ball velocity to screen size with smooth transition
        double currentSpeed = Math.sqrt(menuBallVX * menuBallVX + menuBallVY * menuBallVY);
        if (currentSpeed > 0) {
            double targetSpeed = 4.5 * Math.min(scaleX, scaleY); // Velocità aumentata
            double speedRatio = targetSpeed / currentSpeed;
            menuBallVX *= speedRatio;
            menuBallVY *= speedRatio;
        }
        
        // ===== AGGIORNA SCROLL TARGET DOPO RIDIMENSIONAMENTO =====
        // Quando la finestra viene ridimensionata, i limiti di scroll cambiano
        // Dobbiamo aggiornare i target del sistema smooth scrolling
        updateScrollTargetsAfterResize();
        
        repaint();
    }
    
    /**
     * Aggiorna i target del sistema smooth scrolling dopo ridimensionamento
     */
    private void updateScrollTargetsAfterResize() {
        // Solo se siamo nelle schermate di paddle selection
        if (currentState == GameState.PADDLE_SELECTION || currentState == GameState.RIGHT_PADDLE_SELECTION) {
            
            // Calcola nuovi limiti con le nuove dimensioni
            int panelWidth = getWidth() / 2;
            int panelHeight = getHeight();
            float titleSize = Math.max(16f, Math.min(24f, panelWidth / 15f));
            int headerHeight = (int)(titleSize * 2f);
            int footerHeight = 30;
            int availableHeight = panelHeight - headerHeight - footerHeight;
            int availableWidth = panelWidth;
            
            int horizontalSpacing = 4;
            int verticalSpacing = 8;
            int maxCardSize = Math.min(200, availableHeight / 2);
            int cardSize = (availableWidth - (PADDLE_COLS - 1) * horizontalSpacing) / PADDLE_COLS;
            cardSize = Math.min(cardSize, maxCardSize);
            int minCardSize = Math.max(40, availableWidth / 12);
            cardSize = Math.max(cardSize, minCardSize);
            
            int actualGridWidth = PADDLE_COLS * cardSize + (PADDLE_COLS - 1) * horizontalSpacing;
            if (actualGridWidth < availableWidth) {
                int extraSpace = availableWidth - actualGridWidth;
                cardSize += extraSpace / PADDLE_COLS;
            }
            
            int rowHeight = cardSize + verticalSpacing;
            
            // Aggiorna per paddle sinistro (se nella schermata paddle selection)
            if (currentState == GameState.PADDLE_SELECTION) {
                int totalThemes = bluePaddleThemeNames.size();
                int totalRows = (int) Math.ceil((double) totalThemes / PADDLE_COLS);
                double totalContentHeight = totalRows * rowHeight;
                double maxScroll = Math.max(0, totalContentHeight - availableHeight);
                
                // Limita scroll corrente ai nuovi limiti
                paddleGridScrollY = Math.max(0, Math.min(paddleGridScrollY, maxScroll));
                targetScrollY = paddleGridScrollY; // Sincronizza target con posizione attuale
                
                System.out.println("RESIZE DEBUG Left: maxScroll=" + maxScroll + 
                                 ", newScrollY=" + paddleGridScrollY + ", targetY=" + targetScrollY);
            }
            
            // Aggiorna per paddle destro (se nella schermata right paddle selection) 
            if (currentState == GameState.RIGHT_PADDLE_SELECTION) {
                int totalThemes = redPaddleThemeNames.size();
                int totalRows = (int) Math.ceil((double) totalThemes / PADDLE_COLS);
                double totalContentHeight = totalRows * rowHeight;
                double maxScroll = Math.max(0, totalContentHeight - availableHeight);
                
                // Limita scroll corrente ai nuovi limiti
                rightPaddleGridScrollY = Math.max(0, Math.min(rightPaddleGridScrollY, maxScroll));
                targetRightScrollY = rightPaddleGridScrollY; // Sincronizza target con posizione attuale
                
                System.out.println("RESIZE DEBUG Right: maxScroll=" + maxScroll + 
                                 ", newScrollY=" + rightPaddleGridScrollY + ", targetY=" + targetRightScrollY);
            }
        }
    }
    
    private void loadFonts() {
        try {
            // Try to load fonts as resources from JAR
            InputStream primaryStream = getClass().getClassLoader().getResourceAsStream("font/Silkscreen/Silkscreen-Regular.ttf");
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
            
            InputStream secondaryStream = getClass().getClassLoader().getResourceAsStream("font/Space_Mono/SpaceMono-Regular.ttf");
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
            InputStream rankStream = getClass().getClassLoader().getResourceAsStream("font/Bitcount_Prop_Double/static/BitcountPropDouble-Bold.ttf");
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
    
    private void loadBackgrounds() {
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
                        InputStream imageStream = getClass().getClassLoader().getResourceAsStream("temi/GameBack/" + filename);
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
    
    private void loadPaddleThemes() {
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
     * Converte il nome del file paddle in chiave di traduzione
     */
    private String fileNameToTranslationKey(String displayName) {
        // Il displayName arriva già formattato come "Diagonal Rain"
        // Devo convertirlo in "PADDLE_DIAGONAL_RAIN"
        
        // Rimuove estensioni se presenti
        String cleanName = displayName.replaceAll("\\.(png|jpg|jpeg)$", "");
        
        // Converte spazi in underscore e tutto in maiuscolo
        String key = cleanName.toUpperCase().replace(" ", "_");
        
        // Gestisce casi speciali con parentesi
        key = key.replace("(", "").replace(")", "");
        
        // Aggiungi prefisso per paddle
        return "PADDLE_" + key;
    }
    
    /**
     * Ottiene il nome tradotto del paddle
     */
    private String getTranslatedPaddleName(String fileName) {
        String key = fileNameToTranslationKey(fileName);
        String translated = currentLanguage.get(key);
        
        // Le traduzioni funzionano correttamente
        
        // Se non trova la traduzione, usa il nome file formattato
        if (translated == null || translated.isEmpty()) {
            // Fallback: converte blue-pond-lily -> Blue Pond Lily
            String fallback = fileName.replaceAll("\\.(png|jpg|jpeg)$", "")
                                    .replace("-", " ");
            // Capitalizza ogni parola
            String[] words = fallback.split(" ");
            StringBuilder result = new StringBuilder();
            for (String word : words) {
                if (word.length() > 0) {
                    result.append(Character.toUpperCase(word.charAt(0)))
                          .append(word.substring(1).toLowerCase())
                          .append(" ");
                }
            }
            return result.toString().trim();
        }
        
        return translated;
    }
    
    private void shufflePaddleThemes() {
        // Mescola i temi blu con algoritmo anti-raggruppamento
        if (bluePaddleThemeNames.size() > 2) {
            shuffleThemesWithAntiClustering(bluePaddleThemeNames, bluePaddleThemeImages, "Blu");
        }
        
        // Mescola i temi rossi con algoritmo anti-raggruppamento
        if (redPaddleThemeNames.size() > 2) {
            shuffleThemesWithAntiClustering(redPaddleThemeNames, redPaddleThemeImages, "Rosso");
        }
        
        System.out.println("Temi paddle mescolati con algoritmo anti-raggruppamento");
    }
    
    private void shuffleThemesWithAntiClustering(ArrayList<String> themeNames, ArrayList<BufferedImage> themeImages, String colorName) {
        // Salva il tema default
        String defaultName = themeNames.get(0);
        BufferedImage defaultImage = themeImages.get(0);
        
        // Estrae tutti gli altri temi
        ArrayList<String> tempNames = new ArrayList<>();
        ArrayList<BufferedImage> tempImages = new ArrayList<>();
        for (int i = 1; i < themeNames.size(); i++) {
            tempNames.add(themeNames.get(i));
            tempImages.add(themeImages.get(i));
        }
        
        // Algoritmo anti-raggruppamento: tenta più volte di trovare una disposizione ottimale
        int maxAttempts = 100;
        ArrayList<String> bestNames = null;
        ArrayList<BufferedImage> bestImages = null;
        int bestScore = Integer.MAX_VALUE;
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Mescola casualmente
            ArrayList<String> shuffledNames = new ArrayList<>(tempNames);
            ArrayList<BufferedImage> shuffledImages = new ArrayList<>(tempImages);
            
            for (int i = shuffledNames.size() - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                // Scambia nomi
                String tempName = shuffledNames.get(i);
                shuffledNames.set(i, shuffledNames.get(j));
                shuffledNames.set(j, tempName);
                // Scambia immagini
                BufferedImage tempImg = shuffledImages.get(i);
                shuffledImages.set(i, shuffledImages.get(j));
                shuffledImages.set(j, tempImg);
            }
            
            // Calcola il punteggio di raggruppamento (più basso = meglio)
            int clusterScore = calculateClusterScore(shuffledNames);
            
            if (clusterScore < bestScore) {
                bestScore = clusterScore;
                bestNames = new ArrayList<>(shuffledNames);
                bestImages = new ArrayList<>(shuffledImages);
                
                // Se abbiamo trovato una distribuzione perfetta, usala
                if (clusterScore == 0) {
                    break;
                }
            }
        }
        
        // Ricostruisce la lista con il default al primo posto e la migliore disposizione
        themeNames.clear();
        themeImages.clear();
        themeNames.add(defaultName);
        themeImages.add(defaultImage);
        if (bestNames != null) {
            themeNames.addAll(bestNames);
            themeImages.addAll(bestImages);
        }
        
        System.out.println("Temi " + colorName + " mescolati - Punteggio raggruppamento: " + bestScore);
    }
    
    private int calculateClusterScore(ArrayList<String> names) {
        int score = 0;
        int gridCols = 5; // Numero massimo di colonne nella griglia
        
        // Controlla raggruppamenti orizzontali e verticali
        for (int i = 0; i < names.size(); i++) {
            String currentTheme = names.get(i);
            
            // Controlla adiacenti orizzontali (stessa riga)
            int row = i / gridCols;
            int col = i % gridCols;
            
            // Controlla a destra
            if (col < gridCols - 1 && i + 1 < names.size()) {
                if (currentTheme.equals(names.get(i + 1))) {
                    score += 10; // Penalità alta per adiacenti orizzontali
                }
            }
            
            // Controlla sotto
            if (i + gridCols < names.size()) {
                if (currentTheme.equals(names.get(i + gridCols))) {
                    score += 10; // Penalità alta per adiacenti verticali
                }
            }
            
            // Controlla diagonali
            if (col < gridCols - 1 && i + gridCols + 1 < names.size()) {
                if (currentTheme.equals(names.get(i + gridCols + 1))) {
                    score += 5; // Penalità media per diagonali
                }
            }
            
            if (col > 0 && i + gridCols - 1 < names.size()) {
                if (currentTheme.equals(names.get(i + gridCols - 1))) {
                    score += 5; // Penalità media per diagonali
                }
            }
        }
        
        return score;
    }
    
    private void loadPaddleThemesFromDirectory(String dirPath, ArrayList<String> themeNames, 
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
                    URL resourceUrl = getClass().getResource("/" + fullJarPath);
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
                        InputStream imageStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
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
    
    private void addDefaultPaddleTheme(ArrayList<String> themeNames, ArrayList<BufferedImage> themeImages) {
        themeNames.add("Default (Gradient)");
        themeImages.add(null);
    }
    
    private void loadTextColorsForTheme() {
        // Set default colors
        currentTextColors.put("menuTitle", Color.WHITE);
        currentTextColors.put("menuItems", Color.WHITE);
        currentTextColors.put("menuSelectedItem", Color.WHITE);
        currentTextColors.put("gameScores", Color.WHITE);
        currentTextColors.put("gameStats", Color.WHITE);
        currentTextColors.put("gameControls", Color.WHITE);
        currentTextColors.put("settingsTitle", Color.WHITE);
        currentTextColors.put("settingsLabels", Color.WHITE);
        currentTextColors.put("settingsValues", Color.WHITE);
        
        // Try to load theme-specific colors
        if (selectedBackground >= 0 && selectedBackground < backgroundNames.size()) {
            String themeName = backgroundNames.get(selectedBackground);
            if (themeName.equals("Default (Black)")) {
                themeName = "Default_Black";
            }
            
            // Try to load from JAR resources first
            InputStream themeStream = getClass().getClassLoader().getResourceAsStream("temi/GameBack/" + themeName + ".txt");
            java.io.BufferedReader reader = null;
            
            try {
                if (themeStream != null) {
                    reader = new java.io.BufferedReader(new java.io.InputStreamReader(themeStream));
                    System.out.println("✓ Theme config loaded from JAR: " + themeName + ".txt");
                } else {
                    // Fallback: try loading from file system (development mode)
                    File themeColorFile = new File(getResourcePath("temi/GameBack/" + themeName + ".txt"));
                    if (themeColorFile.exists()) {
                        reader = new java.io.BufferedReader(new java.io.FileReader(themeColorFile));
                        System.out.println("✓ Theme config loaded from file: " + themeName + ".txt");
                    }
                }
                
                if (reader != null) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("#") || line.isEmpty()) continue;
                        
                        String[] parts = line.split("=");
                        if (parts.length == 2) {
                            String colorType = parts[0].trim();
                            String colorValue = parts[1].trim();
                            
                            Color color = parseColorValue(colorValue);
                            if (color != null) {
                                currentTextColors.put(colorType, color);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error loading text colors for theme " + themeName + ": " + e.getMessage());
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e) {
                        // Ignore close errors
                    }
                }
            }
        }
    }
    
    private Color parseColorValue(String colorValue) {
        colorValue = colorValue.toLowerCase().trim();
        
        switch (colorValue) {
            case "white":
                return Color.WHITE;
            case "black":
                return Color.BLACK;
            default:
                if (colorValue.startsWith("custom,")) {
                    try {
                        String[] rgb = colorValue.substring(7).split(",");
                        if (rgb.length == 3) {
                            int r = Integer.parseInt(rgb[0].trim());
                            int g = Integer.parseInt(rgb[1].trim());
                            int b = Integer.parseInt(rgb[2].trim());
                            return new Color(r, g, b);
                        }
                    } catch (Exception e) {
                        System.out.println("Error parsing custom color: " + colorValue);
                    }
                }
                return null;
        }
    }
    
    private void initializeMouseCursors() {
        try {
            // Create invisible cursor
            BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            invisibleCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, 0), "invisible");
            
            // Store default cursor
            defaultCursor = Cursor.getDefaultCursor();
        } catch (Exception e) {
            System.out.println("Could not create invisible cursor: " + e.getMessage());
            // Fallback to blank cursor if creation fails
            invisibleCursor = new Cursor(Cursor.DEFAULT_CURSOR);
            defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
        }
    }
    
    private void loadMusic() {
        try {
            AudioInputStream audioInputStream = null;
            
            // Try to load from JAR resources first
            InputStream musicStream = getClass().getClassLoader().getResourceAsStream("music/Gava-OfficialSoundtrack.wav");
            if (musicStream != null) {
                audioInputStream = AudioSystem.getAudioInputStream(musicStream);
                System.out.println("✓ Music loaded from JAR: Gava-OfficialSoundtrack.wav");
            } else {
                // Fallback: try loading from file system (development mode)
                File musicFile = new File("music/Gava-OfficialSoundtrack.wav");
                if (musicFile.exists()) {
                    audioInputStream = AudioSystem.getAudioInputStream(musicFile);
                    System.out.println("✓ Music loaded from file: Gava-OfficialSoundtrack.wav");
                }
            }
            
            if (audioInputStream != null) {
                backgroundMusic = AudioSystem.getClip();
                backgroundMusic.open(audioInputStream);
                updateMusicVolume(); // Set initial volume
                if (musicEnabled) {
                    backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
                }
            } else {
                System.out.println("⚠️  Background music file not found");
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.out.println("Could not load background music: " + e.getMessage());
        }
    }
    
    private void updateMusicVolume() {
        if (backgroundMusic != null && backgroundMusic.isOpen()) {
            try {
                FloatControl volumeControl = (FloatControl) backgroundMusic.getControl(FloatControl.Type.MASTER_GAIN);
                // Convert 0-100 range to decibel range
                float volume = musicVolume / 100.0f;
                float dB = (float) (Math.log(volume == 0 ? 0.0001 : volume) / Math.log(10.0) * 20.0);
                volumeControl.setValue(Math.max(volumeControl.getMinimum(), Math.min(dB, volumeControl.getMaximum())));
            } catch (Exception e) {
                System.out.println("Could not set volume: " + e.getMessage());
            }
        }
    }
    
    // Sound effect methods
    private void playPaddleHitSound() {
        if (effectsVolume == 0) return; // Skip if effects muted
        new Thread(() -> {
            try {
                AudioFormat af = new AudioFormat(44100, 8, 1, true, false);
                SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
                sdl.open(af);
                sdl.start();
                
                byte[] buf = new byte[1000];
                double volumeMultiplier = effectsVolume / 100.0;
                for (int i = 0; i < buf.length; i++) {
                    double angle = i / (44100.0 / 800) * 2.0 * Math.PI;
                    buf[i] = (byte) (Math.sin(angle) * 80 * volumeMultiplier);
                }
                
                sdl.write(buf, 0, buf.length);
                sdl.drain();
                sdl.close();
            } catch (Exception e) { /* Silent fail */ }
        }).start();
    }
    
    private void playScoreSound() {
        if (effectsVolume == 0) return; // Skip if effects muted
        new Thread(() -> {
            try {
                AudioFormat af = new AudioFormat(44100, 8, 1, true, false);
                SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
                sdl.open(af);
                sdl.start();
                
                double volumeMultiplier = effectsVolume / 100.0;
                for (int freq : new int[]{523, 659, 784}) {
                    byte[] buf = new byte[2000];
                    for (int i = 0; i < buf.length; i++) {
                        double angle = i / (44100.0 / freq) * 2.0 * Math.PI;
                        buf[i] = (byte) (Math.sin(angle) * 60 * volumeMultiplier);
                    }
                    sdl.write(buf, 0, buf.length);
                }
                
                sdl.drain();
                sdl.close();
            } catch (Exception e) { /* Silent fail */ }
        }).start();
    }
    
    private void playWallHitSound() {
        if (effectsVolume == 0) return; // Skip if effects muted
        new Thread(() -> {
            try {
                AudioFormat af = new AudioFormat(44100, 8, 1, true, false);
                SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
                sdl.open(af);
                sdl.start();
                
                byte[] buf = new byte[800];
                double volumeMultiplier = effectsVolume / 100.0;
                for (int i = 0; i < buf.length; i++) {
                    double angle = i / (44100.0 / 300) * 2.0 * Math.PI;
                    buf[i] = (byte) (Math.sin(angle) * 40 * volumeMultiplier);
                }
                
                sdl.write(buf, 0, buf.length);
                sdl.drain();
                sdl.close();
            } catch (Exception e) { /* Silent fail */ }
        }).start();
    }
    
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Screen shake effect
        if (shakeIntensity > 0) {
            g2d.translate(shakeX, shakeY);
        }
        
        switch (currentState) {
            case SETTINGS:
                drawSettings(g2d);
                break;
            case TRANSITIONING:
                drawTransition(g2d);
                break;
            case PLAYING:
            case SINGLE_PLAYER:
                drawGame(g2d);
                break;
            case PAUSED:
                drawGameForPause(g2d);
                drawPauseOverlay(g2d);
                break;
            case GAME_OVER:
                drawGame(g2d);
                if (showRankScreen && currentState == GameState.GAME_OVER) {
                    drawRankScreen(g2d);
                } else {
                    drawGameOverOverlay(g2d);
                }
                break;
            case GAME_MODE_SELECTION:
                drawGameModeSelection(g2d);
                break;
            case BACKGROUND_SELECTION:
                if (isThemesToHomeTransition) {
                    drawThemesToHomeTransition(g2d);
                } else {
                    drawBackgroundSelection(g2d);
                }
                break;
            case PADDLE_SELECTION:
                if (isPaddleToHomeTransition) {
                    drawPaddleToHomeTransition(g2d);
                } else {
                    drawPaddleSelection(g2d);
                }
                break;
            case RANK:
                drawRankScreen(g2d);
                break;
            case DEBUG:
                drawDebug(g2d);
                break;
            case RIGHT_PADDLE_SELECTION:
                if (isPaddleToHomeTransition) {
                    drawPaddleToHomeTransition(g2d);
                } else {
                    drawRightPaddleSelection(g2d);
                }
                break;
            case FIRST_ACCESS:
                drawFirstAccess(g2d);
                break;
            case MENU:
                if (isHomeToThemesTransition) {
                    drawHomeToThemesTransition(g2d);
                } else if (isHomeToPaddleTransition) {
                    drawHomeToPaddleTransition(g2d);
                } else if (isHomeToSettingsTransition) {
                    drawHomeToSettingsTransition(g2d);
                } else {
                    drawMenu(g2d);
                }
                break;
            case HISTORY:
                drawHistory(g2d);
                break;
        }
    }
    
    private void drawSettingsBackground(Graphics2D g) {
        // Draw the selected background theme behind the checkerboard
        if (selectedBackground >= 0 && selectedBackground < backgroundImages.size()) {
            Image backgroundImg = backgroundImages.get(selectedBackground);
            
            if (backgroundImg != null) {
                // Draw background image scaled to full screen
                g.drawImage(backgroundImg, 0, 0, getWidth(), getHeight(), this);
                
                // Add contrast effect for better text visibility (same as game)
                drawBackgroundContrastEffect(g);
            } else {
                // Draw default black background
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        } else {
            // Draw default black background
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
    
    private void drawAnimatedCheckerboard(Graphics2D g) {
        // First draw the selected background theme
        drawSettingsBackground(g);
        
        // Checkerboard properties - scale tile size based on window dimensions
        int baseTileSize = 40;
        int tileSize = (int)(baseTileSize * Math.min(scaleX, scaleY));
        
        // Calculate diagonal offset (both x and y move together for diagonal effect)
        double diagonalOffset = checkerboardOffset;
        int offsetX = (int)(diagonalOffset);
        int offsetY = (int)(diagonalOffset);
        
        // Calculate how many tiles we need to cover the screen plus the offset
        int tilesX = (getWidth() / tileSize) + 3; // Extra tiles for smooth animation, use actual window width
        int tilesY = (getHeight() / tileSize) + 3; // Use actual window height
        
        // Draw checkerboard pattern with lighting effects
        for (int x = -2; x < tilesX; x++) {
            for (int y = -2; y < tilesY; y++) {
                // Calculate actual position with diagonal offset
                int posX = x * tileSize - offsetX;
                int posY = y * tileSize - offsetY;
                
                // Determine if this tile should be dark or light
                boolean isDark = (x + y) % 2 == 0;
                
                // Skip dark tiles (they remain transparent to show background)
                if (isDark) {
                    continue; // Don't draw anything - let background show through
                }
                
                // Calculate distance from selected element for lighting effect
                double centerX, centerY;
                
                if (inCategoryColumn) {
                    // Light follows selected category on the left
                    centerX = 200 * scaleX; // Left side position
                    centerY = (280 + selectedCategory * 80) * scaleY; // Moved down to match new position
                } else {
                    // Light follows selected setting on the right
                    centerX = 650 * scaleX; // Much closer to right edge
                    centerY = (200 + selectedCategorySetting * 80) * scaleY; // Setting position
                }
                double tileCenterX = posX + tileSize / 2.0;
                double tileCenterY = posY + tileSize / 2.0;
                double distance = Math.sqrt(Math.pow(tileCenterX - centerX, 2) + Math.pow(tileCenterY - centerY, 2));
                double maxDistance = Math.sqrt(Math.pow(getWidth() / 2.0, 2) + Math.pow(getHeight() / 2.0, 2));
                
                // Create lighting falloff (brighter in center)
                double lightingFactor = 1.0 - (distance / maxDistance) * 0.7;
                lightingFactor = Math.max(0.3, lightingFactor); // Minimum brightness
                
                // Apply glow intensity animation
                double currentGlow = glowIntensity * 0.5; // Scale down for subtlety
                lightingFactor += currentGlow;
                lightingFactor = Math.min(1.0, lightingFactor);
                
                // Only draw red tiles (light tiles) - fully opaque
                int redComponent = (int)(180 * lightingFactor);
                int greenComponent = (int)(30 * lightingFactor * currentGlow); // Slight orange tint when glowing
                Color baseColor = new Color(Math.min(255, redComponent), Math.min(255, greenComponent), 0); // Fully opaque
                
                g.setColor(baseColor);
                g.fillRect(posX, posY, tileSize, tileSize);
                
                // Add glow border effect on red tiles
                if (currentGlow > 0.3) {
                    // Create glow border
                    int glowAlpha = (int)(100 * currentGlow); // Normal glow intensity
                    Color glowColor = new Color(255, 100, 100, glowAlpha);
                    g.setColor(glowColor);
                    
                    // Draw glow border
                    int glowSize = (int)(currentGlow * 3);
                    g.fillRect(posX - glowSize, posY - glowSize, 
                              tileSize + glowSize * 2, tileSize + glowSize * 2);
                    
                    // Redraw the tile on top
                    g.setColor(baseColor);
                    g.fillRect(posX, posY, tileSize, tileSize);
                }
            }
        }
        
        // Add overall atmospheric glow effect
        if (glowIntensity > 0.2) {
            // Create radial gradient overlay centered on selected element
            float centerX, centerY;
            
            if (inCategoryColumn) {
                // Glow follows selected category on the left
                centerX = (float)(200 * scaleX); // Left side position
                centerY = (float)((280 + selectedCategory * 80) * scaleY); // Moved down to match new position
            } else {
                // Glow follows selected setting on the right
                centerX = (float)(650 * scaleX); // Much closer to right edge
                centerY = (float)((200 + selectedCategorySetting * 80) * scaleY);
            }
            
            float radius = Math.max(BOARD_WIDTH, BOARD_HEIGHT) / 3f; // Smaller radius for more focused effect
            
            java.awt.RadialGradientPaint radialGradient = new java.awt.RadialGradientPaint(
                centerX, centerY, radius,
                new float[]{0f, 0.6f, 1f},
                new Color[]{
                    new Color(100, 0, 0, (int)(30 * glowIntensity)),  // Center glow
                    new Color(50, 0, 0, (int)(15 * glowIntensity)),   // Mid glow
                    new Color(0, 0, 0, 0)                             // Fade to transparent
                }
            );
            
            g.setPaint(radialGradient);
            g.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
        }
        
        // Add gray dissolve overlay on top of checkerboard
        int dissolveHeight = (int)(350 * scaleY); // Cover top 350px (more area)
        java.awt.GradientPaint grayDissolve = new java.awt.GradientPaint(
            0, 0, new Color(100, 100, 100, 120),        // Darker gray at top with more opacity
            0, dissolveHeight, new Color(100, 100, 100, 0)  // Fade to transparent
        );
        g.setPaint(grayDissolve);
        g.fillRect(0, 0, BOARD_WIDTH, dissolveHeight);
        
        // Add hover effect when mouse is on background
        if (mouseOnBackground) {
            System.out.println("DEBUG: Drawing background hover effect");
            // Create a subtle highlight overlay
            Color hoverColor = new Color(255, 255, 255, 30); // White with low opacity
            g.setColor(hoverColor);
            g.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
            
            // Add a pulsing border effect
            float borderSize = 4.0f;
            float alpha = (float)(0.5 + 0.3 * Math.sin(System.currentTimeMillis() * 0.005)); // Pulsing effect
            Color borderColor = new Color(100, 150, 255, (int)(alpha * 100));
            g.setColor(borderColor);
            
            // Draw border
            BasicStroke borderStroke = new BasicStroke(borderSize);
            g.setStroke(borderStroke);
            g.drawRect((int)borderSize/2, (int)borderSize/2, 
                      BOARD_WIDTH - (int)borderSize, BOARD_HEIGHT - (int)borderSize);
        }
    }
    
    private void drawSettings(Graphics2D g) {
        // Handle settings to home transition first (like home to settings does)
        if (isSettingsToHomeTransition) {
            drawSettingsToHomeTransition(g);
            return;
        }
        
        // Draw animated checkerboard background
        drawAnimatedCheckerboard(g);
        
        if (isTransitioningDemoToMenu) {
            // Draw transition from demo to menu
            drawDemoToMenuTransition(g);
        } else if (isTransitioningToDemo || isTransitioningFromDemo) {
            // Draw transition animation (works for both directions)
            drawTransitioningElements(g, demoTransitionProgress);
        } else {
            // Always show normal settings (no more integrated demo)
            drawNormalSettings(g);
        }
    }
    
    private void drawNormalSettings(Graphics2D g) {
        // Save original transform
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        // Simple title with right tilt (localized)
        g.setColor(Color.WHITE);
        float titleSize = (float)(36 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(titleSize));
        FontMetrics titleFm = g.getFontMetrics();
        String title = getText("MENU_SETTINGS");
        int titleX = (BOARD_WIDTH - titleFm.stringWidth(title)) / 2;
        int titleY = (int)(80 * scaleY);
        
        g.drawString(title, titleX, titleY);
        
        // Draw black paddle behind the settings
        drawSettingsPaddle(g, originalTransform);
        
        // Draw two-column layout (on top of paddle)
        drawCategoryColumn(g, originalTransform);
        drawSettingColumn(g, originalTransform);
        
        // Dynamic instructions based on current column position
        g.setColor(new Color(120, 120, 120));
        float instructionSize = (float)(14 * Math.min(scaleX, scaleY));
        g.setFont(secondaryFont.deriveFont(instructionSize));
        
        String instructions;
        if (inCategoryColumn) {
            instructions = getText("UI_ESC_BACKSPACE_HOME");
        } else {
            instructions = getText("UI_ESC_BACKSPACE_BACK");
        }
        
        FontMetrics fm = g.getFontMetrics();
        int instructionX = (int)(10 * scaleX); // Proprio nell'angolo sinistra
        int instructionY = BOARD_HEIGHT - (int)(10 * scaleY); // Proprio in basso
        
        // Calculate left paddle bounds to check for overlap
        int basePaddleWidth = (int)(250 * Math.min(scaleX, scaleY));
        double leftWidthMultiplier = 1.0 + (0.3 * leftPaddleWidthProgress);
        int leftPaddleWidth = (int)(basePaddleWidth * leftWidthMultiplier);
        
        // Calculate approximate horizontal space taken by left paddle (considering rotation)
        int paddleRightBound = (int)(leftPaddleWidth * 0.7); // Approximate bound considering -25° rotation
        
        // Check if text overlaps with paddle
        int textWidth = fm.stringWidth(instructions);
        if (instructionX + textWidth > paddleRightBound) {
            // Split text into two lines to avoid paddle overlap
            String[] words = instructions.split(" ");
            String line1 = "";
            String line2 = "";
            boolean firstLineFull = false;
            
            for (String word : words) {
                String testLine1 = line1.isEmpty() ? word : line1 + " " + word;
                if (!firstLineFull && fm.stringWidth(testLine1) <= paddleRightBound - instructionX) {
                    line1 = testLine1;
                } else {
                    firstLineFull = true;
                    line2 = line2.isEmpty() ? word : line2 + " " + word;
                }
            }
            
            // Draw two lines
            g.drawString(line1, instructionX, instructionY - (int)(18 * scaleY));
            g.drawString(line2, instructionX, instructionY);
        } else {
            // Single line - no overlap
            g.drawString(instructions, instructionX, instructionY);
        }
        
        // Draw "by Gava" signature
        drawGavaSignature(g);
    }
    
    private void drawSettingsPaddle(Graphics2D g, java.awt.geom.AffineTransform originalTransform) {
        // Draw black paddle with EXACT same position and characteristics as left menu paddle
        int basePaddleWidth = (int)(250 * Math.min(scaleX, scaleY)); // Base width scaled uniformly
        int paddleHeight = (int)(menuPaddleHeight * 1.8); // 80% taller than screen (increased for rotation)
        int paddleYOffset = (int)(-paddleHeight * 0.2); // Start further above screen
        
        // Calculate width expansion with smooth animation
        double leftWidthMultiplier = 1.0 + (0.3 * leftPaddleWidthProgress); // 1.0 to 1.3
        
        // Calculate right paddle width based on average text content width in right column
        double rightWidthMultiplier = calculateRightPaddleWidthFromContent(g);
        rightWidthMultiplier = 1.0 + (rightWidthMultiplier * rightPaddleWidthProgress); // Apply animation progress
        
        int leftPaddleWidth = (int)(basePaddleWidth * leftWidthMultiplier);
        int rightPaddleWidth = (int)(basePaddleWidth * rightWidthMultiplier);
        
        // Left paddle position - EXACTLY same as in drawMenuPaddles
        int leftCenterX = 0; // Completely attached to left edge
        int leftCenterY = paddleYOffset + paddleHeight / 2;
        
        g.translate(leftCenterX, leftCenterY);
        g.rotate(Math.toRadians(-25)); // Same rotation as left menu paddle
        
        // Draw black paddle (solid black instead of blue gradient)
        g.setColor(Color.BLACK);
        g.fillRect(-leftPaddleWidth/2, -paddleHeight/2, leftPaddleWidth, paddleHeight);
        
        // Black paddle border glow (same style as menu paddle but black)
        g.setColor(new Color(50, 50, 50, 100));
        int glowWidth = Math.max(2, (int)(8 * Math.min(scaleX, scaleY)));
        g.fillRect(leftPaddleWidth/2, -paddleHeight/2, glowWidth, paddleHeight);
        
        // Reset transform for right paddle
        g.setTransform(originalTransform);
        
        // Right paddle position - like red paddle from home menu but with black color and -25° rotation
        int rightCenterX = getWidth() + rightPaddleWidth/8; // Move center just slightly so right edge is barely hidden
        int rightCenterY = paddleYOffset + paddleHeight / 2;
        
        g.translate(rightCenterX, rightCenterY);
        g.rotate(Math.toRadians(-25)); // Reversed rotation: red paddle normally has +25°, this has -25°
        
        // Draw black right paddle (same position as red paddle but black color)
        g.setColor(Color.BLACK);
        g.fillRect(-rightPaddleWidth/2, -paddleHeight/2, rightPaddleWidth, paddleHeight);
        
        // Black paddle border glow (same position as red paddle - on left side)
        g.setColor(new Color(50, 50, 50, 100));
        g.fillRect(-rightPaddleWidth/2 - glowWidth, -paddleHeight/2, glowWidth, paddleHeight);
        
        // Reset transform
        g.setTransform(originalTransform);
    }
    
    private void drawDefaultLeftPaddleTransition(Graphics2D g, int leftPaddleWidth, int paddleHeight) {
        // Left paddle transitions from blue to black (original logic)
        Color leftColor1, leftColor2, leftGlowColor;
        if (paddleTranslationProgress < 0.5) {
            // First half: original blue colors
            leftColor1 = new Color(100, 150, 255);
            leftColor2 = new Color(150, 200, 255);
            leftGlowColor = getPaddleGlowColor(true); // Theme-based glow
        } else {
            // Second half: transition to black
            double blackProgress = (paddleTranslationProgress - 0.5) * 2.0;
            leftColor1 = interpolateToBlack(new Color(100, 150, 255), blackProgress);
            leftColor2 = interpolateToBlack(new Color(150, 200, 255), blackProgress);
            leftGlowColor = interpolateToBlack(getPaddleGlowColor(true), blackProgress);
        }
        
        // Draw left paddle glow (right side)
        int glowWidth = Math.max(2, (int)(8 * Math.min(scaleX, scaleY)));
        g.setColor(leftGlowColor);
        g.fillRect(leftPaddleWidth/2, -paddleHeight/2, glowWidth, paddleHeight);
        
        // Draw left paddle
        GradientPaint leftGradient = new GradientPaint(
            -leftPaddleWidth/2, -paddleHeight/2, leftColor1,
            leftPaddleWidth/2, paddleHeight/2, leftColor2);
        g.setPaint(leftGradient);
        g.fillRect(-leftPaddleWidth/2, -paddleHeight/2, leftPaddleWidth, paddleHeight);
        g.setPaint(null);
    }
    
    private void drawDefaultRightPaddleTransition(Graphics2D g, int rightPaddleWidth, int paddleHeight) {
        // Right paddle color: from red to black (original logic)
        Color rightColor1, rightColor2, rightGlowColor;
        if (paddleTranslationProgress < 0.5) {
            // First half: original red colors
            rightColor1 = new Color(255, 100, 100);
            rightColor2 = new Color(255, 150, 150);
            rightGlowColor = getPaddleGlowColor(false); // Theme-based glow
        } else {
            // Second half: transition to black
            double blackProgress = (paddleTranslationProgress - 0.5) * 2.0;
            rightColor1 = interpolateToBlack(new Color(255, 100, 100), blackProgress);
            rightColor2 = interpolateToBlack(new Color(255, 150, 150), blackProgress);
            rightGlowColor = interpolateToBlack(getPaddleGlowColor(false), blackProgress);
        }
        
        // Draw right paddle glow
        int glowWidth = Math.max(2, (int)(8 * Math.min(scaleX, scaleY)));
        if (paddleTranslationProgress < 0.8) { // Only show glow during transition, not in final settings
            g.setColor(rightGlowColor);
            if (paddleTranslationProgress < 0.5) {
                // During first half (still red), glow on left side (menu style)
                g.fillRect(-rightPaddleWidth/2 - glowWidth, -paddleHeight/2, glowWidth, paddleHeight);
            } else {
                // During second half (turning black), glow transitions to settings position
                double glowTransition = (paddleTranslationProgress - 0.5) * 2.0;
                int startGlowX = -rightPaddleWidth/2 - glowWidth; // Menu position (left side)
                int endGlowX = -rightPaddleWidth/2 - glowWidth;   // Settings position (still left side due to rotation)
                int currentGlowX = (int)(startGlowX + (endGlowX - startGlowX) * glowTransition);
                g.fillRect(currentGlowX, -paddleHeight/2, glowWidth, paddleHeight);
            }
        }
        
        // Draw right paddle
        GradientPaint rightGradient = new GradientPaint(
            -rightPaddleWidth/2, -paddleHeight/2, rightColor1,
            rightPaddleWidth/2, paddleHeight/2, rightColor2);
        g.setPaint(rightGradient);
        g.fillRect(-rightPaddleWidth/2, -paddleHeight/2, rightPaddleWidth, paddleHeight);
        g.setPaint(null);
    }
    
    private double calculateRightPaddleWidthFromContent(Graphics2D g) {
        // Only apply dynamic width when in right column (settings column)
        if (inCategoryColumn) {
            return 0.3; // Use default expansion when in left column
        }
        
        String[] currentSettings = categorySettings[selectedCategory];
        double totalWidth = 0;
        int contentCount = 0;
        
        // Calculate average width of ONLY setting names (fixed text, not variable values)
        for (int i = 0; i < currentSettings.length; i++) {
            String settingId = currentSettings[i];
            String displayName = getSettingDisplayName(settingId);
            // Measure only setting display name width (primary text) - NO values
            float nameSize = (float)(26 * Math.min(scaleX, scaleY));
            g.setFont(primaryFont.deriveFont(nameSize));
            FontMetrics nameFm = g.getFontMetrics();
            totalWidth += nameFm.stringWidth(displayName);
            contentCount++;
        }
        
        if (contentCount == 0) {
            return 0.3; // Default expansion
        }
        
        // Calculate average width and convert to multiplier
        double averageWidth = totalWidth / contentCount;
        double basePaddleWidth = 250 * Math.min(scaleX, scaleY);
        
        // Scale the multiplier based on average content width
        // Normalize to a reasonable range (0.1 to 0.8 expansion)
        double contentWidthRatio = averageWidth / (basePaddleWidth * 0.4); // Compare to 40% of base width
        double expansion = Math.max(0.1, Math.min(0.8, contentWidthRatio * 0.3)); // Scale and clamp
        
        return expansion;
    }
    
    private double calculateBackgroundSpeedFromSetting() {
        // Base speed for different difficulty levels
        double speed = 2.0; // Default speed
        
        // Check current selected category and its settings
        if (selectedCategory >= 0 && selectedCategory < categorySettings.length) {
            String[] currentSettings = categorySettings[selectedCategory];
            
            for (int i = 0; i < currentSettings.length; i++) {
                String settingId = currentSettings[i];
                String currentValue = getCurrentCategorySettingValue(selectedCategory, i);
                
                // Apply speed multiplier based on setting values
                if ("SETTING_AI_DIFFICULTY".equals(settingId)) {
                    switch (currentValue) {
                        case "FACILE":
                            speed *= 0.3; // Very slow
                            break;
                        case "NORMALE":
                            speed *= 0.6; // Moderate
                            break;
                        case "DIFFICILE":
                            speed *= 1.0; // Normal
                            break;
                        case "ESPERTO":
                            speed *= 1.8; // Fast
                            break;
                        case "IMPOSSIBILE":
                            speed *= 3.5; // Very fast
                            break;
                    }
                } else if ("SETTING_PADDLE_SPEED".equals(settingId)) {
                    switch (currentValue) {
                        case "FACILE":
                            speed *= 0.4;
                            break;
                        case "NORMALE":
                            speed *= 0.7;
                            break;
                        case "DIFFICILE":
                            speed *= 1.0;
                            break;
                        case "ESPERTO":
                            speed *= 1.6;
                            break;
                        case "IMPOSSIBILE":
                            speed *= 3.0;
                            break;
                    }
                } else if ("SETTING_BALL_SPEED".equals(settingId)) {
                    // Ballspeed is numeric, convert to multiplier
                    try {
                        int ballSpeed = Integer.parseInt(currentValue);
                        if (ballSpeed <= 2) speed *= 0.3;
                        else if (ballSpeed <= 4) speed *= 0.6;
                        else if (ballSpeed <= 6) speed *= 1.0;
                        else if (ballSpeed <= 8) speed *= 1.5;
                        else speed *= 2.5;
                    } catch (NumberFormatException e) {
                        // Use default multiplier
                    }
                }
            }
        }
        
        return Math.max(0.1, Math.min(8.0, speed)); // Clamp between 0.1 and 8.0
    }
    
    private void drawCategoryColumn(Graphics2D g, java.awt.geom.AffineTransform originalTransform) {
        // Left column - Categories
        int startY = (int)(280 * scaleY); // Moved further down
        int categoryHeight = (int)(80 * scaleY); // Increased to match settings column
        
        for (int i = 0; i < categoryNames.length; i++) {
            // Clean hover-based selection: mouse hover overrides keyboard selection only when not using keyboard
            boolean isSelected = (!isUsingKeyboardNavigationSettings && currentHoverState == HoverState.CATEGORY && hoveredCategory == i) ||
                                (isUsingKeyboardNavigationSettings || currentHoverState != HoverState.CATEGORY) && selectedCategory == i && inCategoryColumn;
            
            // Show active category only when not hovering with mouse or when using keyboard
            boolean isCategoryActive = false;
            if (isUsingKeyboardNavigationSettings || currentHoverState != HoverState.CATEGORY) {
                // Show keyboard selection
                isCategoryActive = selectedCategory == i && !inCategoryColumn; // Dim when focus is on settings
            }
            
            // Category name with left tilt - highlight if selected and in category column
            if (isSelected) {
                g.setColor(new Color(100, 150, 255)); // Bright blue when actively selected
            } else if (isCategoryActive) {
                g.setColor(new Color(150, 150, 200)); // Dim blue when category is active but focus is on settings
            } else {
                g.setColor(Color.WHITE); // White when not selected
            }
            
            float nameSize = (float)(32 * Math.min(scaleX, scaleY));
            g.setFont(primaryFont.deriveFont(nameSize));
            
            // Always resolve category label from translations to ensure correctness
            String categoryName;
            switch (i) {
                case 0: categoryName = getText("SETTINGS_DIFFICULTY"); break;
                case 1: categoryName = getText("SETTINGS_PADDLE"); break;
                case 2: categoryName = getText("SETTINGS_CONTROLS"); break;
                case 3: categoryName = getText("SETTINGS_AUDIO"); break;
                case 4: categoryName = getText("SETTINGS_LANGUAGE"); break;
                default: categoryName = categoryNames[i]; // Fallback
            }
            
            // Smooth animated position based on animation progress
            double hiddenX = 10 * scaleX; // Hidden position (far left)
            double visibleX = 50 * scaleX; // Visible position (normal)
            double animProgress = categoryAnimationProgress[i];
            
            // Interpolate between hidden and visible positions
            int nameX = (int)(hiddenX + (visibleX - hiddenX) * animProgress);
            
            int y = startY + i * categoryHeight;
            
            // Apply left tilt to category name (same as paddle: -25°)
            g.rotate(Math.toRadians(-25), nameX, y);
            g.drawString(categoryName, nameX, y);
            g.setTransform(originalTransform);
            
            // Selection indicator - only show when actively selected
            if (isSelected) {
                g.setColor(new Color(100, 150, 255));
                
                // Use same font size as category for proper alignment (reuse existing nameSize)
                g.setFont(primaryFont.deriveFont(nameSize));
                
                // Calculate exact position for perfect alignment with rotated text
                int arrowX = nameX - (int)(30 * scaleX);
                
                // Apply rotation around the SAME point as the category name for perfect alignment
                g.rotate(Math.toRadians(-25), nameX, y);
                g.drawString(">", arrowX, y);
                g.setTransform(originalTransform);
            }
        }
    }
    
    private void drawSettingColumn(Graphics2D g, java.awt.geom.AffineTransform originalTransform) {
        // Right column - Settings for selected category
        int startY = (int)(200 * scaleY); // Moved down to match categories
        int settingHeight = (int)(90 * scaleY); // Increased from 80 to 90 for better spacing
        
        String[] currentSettings = categorySettings[selectedCategory];
        
        for (int i = 0; i < currentSettings.length; i++) {
            // Clean hover-based selection: mouse hover overrides keyboard selection only when not using keyboard
            boolean isSelected = (!isUsingKeyboardNavigationSettings && currentHoverState == HoverState.SETTING && hoveredSetting == i) ||
                                (isUsingKeyboardNavigationSettings || currentHoverState != HoverState.SETTING) && selectedCategorySetting == i && !inCategoryColumn;
            
            String settingId = currentSettings[i];
            String settingName = getSettingDisplayName(settingId);
            String currentValue = getCurrentCategorySettingValue(selectedCategory, i);
            
            int y = startY + i * settingHeight;
            int centerX = (int)(650 * scaleX); // Much closer to right edge
            
            // 1. Setting name at the top (centered) - Primary text level
            if (isSelected) {
                g.setColor(new Color(255, 165, 85)); // Warm orange for selected - primary accent
            } else {
                g.setColor(new Color(240, 246, 252)); // Near white with subtle blue tint - high contrast
            }
            float nameSize = (float)(26 * Math.min(scaleX, scaleY)); // Larger - primary text level
            g.setFont(primaryFont.deriveFont(nameSize));
            FontMetrics nameFm = g.getFontMetrics();
            int nameX = centerX - nameFm.stringWidth(settingName) / 2;
            g.drawString(settingName, nameX, y);
            
            // 2. Value with arrows below (centered) - Secondary text level
            int valueY = y + (int)(40 * scaleY); // Increased spacing - 40px below setting name
            
            if (canChangeSettingWithArrows(settingName)) {
                // For changeable settings: < VALUE >
                float valueSize = (float)(20 * Math.min(scaleX, scaleY)); // Secondary text size
                g.setFont(primaryFont.deriveFont(valueSize));
                FontMetrics valueFm = g.getFontMetrics();
                
                // Calculate positions for < VALUE > (using same style as category arrows)
                String leftArrow = "<";
                String rightArrow = ">";
                int totalWidth = valueFm.stringWidth(leftArrow + " " + currentValue + " " + rightArrow);
                int startX = centerX - totalWidth / 2;
                
                // Draw arrows with modern UI colors
                if (isSelected) {
                    g.setColor(new Color(255, 180, 100)); // Lighter orange for arrows when selected
                } else {
                    g.setColor(new Color(156, 163, 175)); // Gray-400 for inactive UI elements
                }
                g.drawString(leftArrow, startX, valueY);
                
                // Value color with high contrast - special handling for AI difficulty
                int valueX = startX + valueFm.stringWidth(leftArrow + " ");
                
                // Check if this is AI difficulty setting to apply special effects
                if (settingId.equals("SETTING_AI_DIFFICULTY")) {
                    // Use special difficulty drawing with effects
                    drawDifficultyText(g, currentValue, valueX, valueY, valueSize, aiDifficultySetting);
                } else {
                    // Normal value drawing
                    if (isSelected) {
                        g.setColor(new Color(248, 250, 252)); // Nearly white for maximum contrast
                    } else {
                        g.setColor(new Color(203, 213, 225)); // Gray-300 for secondary text
                    }
                    g.drawString(currentValue, valueX, valueY);
                }
                
                // Right arrow matching left arrow
                if (isSelected) {
                    g.setColor(new Color(255, 180, 100)); // Lighter orange for arrows when selected
                } else {
                    g.setColor(new Color(156, 163, 175)); // Gray-400 for inactive UI elements
                }
                int rightArrowX = valueX + valueFm.stringWidth(currentValue + " ");
                g.drawString(rightArrow, rightArrowX, valueY);
                
            } else {
                // For key configuration settings: just VALUE
                if (isSelected) {
                    g.setColor(new Color(248, 250, 252)); // Nearly white for maximum contrast when selected
                } else {
                    g.setColor(new Color(203, 213, 225)); // Gray-300 for secondary text when not selected
                }
                float valueSize = (float)(20 * Math.min(scaleX, scaleY)); // Matching secondary text size
                g.setFont(primaryFont.deriveFont(valueSize));
                FontMetrics valueFm = g.getFontMetrics();
                int valueX = centerX - valueFm.stringWidth(currentValue) / 2;
                g.drawString(currentValue, valueX, valueY);
                
                // Add "ENTER per configurare" hint below for key settings - Tertiary text level
                if (isSelected) {
                    g.setColor(new Color(156, 163, 175)); // Gray-400 for hint text - lower hierarchy
                    float hintSize = (float)(14 * Math.min(scaleX, scaleY)); // Tertiary text size (smaller)
                    g.setFont(secondaryFont.deriveFont(hintSize));
                    String hint = "ENTER per configurare";
                    FontMetrics hintFm = g.getFontMetrics();
                    int hintX = centerX - hintFm.stringWidth(hint) / 2;
                    g.drawString(hint, hintX, valueY + (int)(25 * scaleY)); // Increased spacing
                }
            }
            
            // Selection indicator on the left of the right column (aligned with setting name)
            if (isSelected) {
                g.setColor(new Color(255, 180, 120)); // Improved arrow color
                float arrowSize = (float)(22 * Math.min(scaleX, scaleY)); // Same size as setting name
                g.setFont(primaryFont.deriveFont(arrowSize));
                g.drawString(">", centerX - (int)(140 * scaleX), y); // Aligned with first line (setting name)
            }
        }
    }
    
    private void drawSettingItem(Graphics2D g, int settingIndex, int y) {
        boolean isSelected = selectedSetting == settingIndex;
        
        // Save original transform for this item
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        // Setting name with left tilt
        g.setColor(isSelected ? new Color(100, 150, 255) : Color.WHITE);
        float nameSize = (float)(32 * Math.min(scaleX, scaleY)); // Much larger font size
        g.setFont(primaryFont.deriveFont(nameSize));
        
        String settingName = getSettingDisplayName(settingNames[settingIndex]);
        int nameX = (int)(50 * scaleX);
        
        // Apply moderate left tilt to setting name
        g.rotate(Math.toRadians(-20), nameX, y);
        g.drawString(settingName, nameX, y);
        g.setTransform(originalTransform);
        
        // Setting value with right tilt
        String currentValue = getCurrentSettingValue(settingIndex);
        g.setColor(isSelected ? new Color(150, 200, 255) : new Color(180, 180, 180));
        
        FontMetrics fm = g.getFontMetrics();
        int valueX = BOARD_WIDTH - (int)(50 * scaleX) - fm.stringWidth(currentValue);
        
        // Apply left tilt to setting value
        g.rotate(Math.toRadians(-15), valueX + fm.stringWidth(currentValue)/2, y);
        g.drawString(currentValue, valueX, y);
        g.setTransform(originalTransform);
        
        // Selection indicator
        if (isSelected) {
            g.setColor(new Color(100, 150, 255));
            g.drawString(">", nameX - (int)(25 * scaleX), y);
            
            // Right side navigation arrows for changeable settings
            if (settingIndex <= 2 || (settingIndex >= 3 && settingIndex <= 6 && waitingForKeyInput == -1)) {
                g.setColor(new Color(100, 150, 255));
                float arrowSize = (float)(16 * Math.min(scaleX, scaleY));
                g.setFont(primaryFont.deriveFont(arrowSize));
                
                // Check if we can go left/right based on current values
                boolean canGoLeft = canDecreaseSetting(settingIndex);
                boolean canGoRight = canIncreaseSetting(settingIndex);
                
                // Left arrow (to decrease value) - only if we can decrease
                if (canGoLeft) {
                    g.drawString("<", valueX - (int)(25 * scaleX), y);
                }
                
                // Right arrow (to increase value) - only if we can increase
                if (canGoRight) {
                    g.drawString(">", valueX + fm.stringWidth(currentValue) + (int)(10 * scaleX), y);
                }
            }
        }
    }
    
    private String getCurrentCategorySettingValue(int categoryIndex, int settingIndex) {
        String settingName = categorySettings[categoryIndex][settingIndex];
        
        // Handle audio settings directly
        if (categoryIndex == 3) { // Audio category
            switch (settingIndex) {
                case 0: return String.valueOf(musicVolume); // VOLUME MUSICA
                case 1: return String.valueOf(effectsVolume); // VOLUME EFFETTI
                case 2: return musicEnabled ? getText("UI_ON") : getText("UI_OFF"); // MUSICA ATTIVA (localized)
                default: return "";
            }
        }
        
        // Handle language settings directly
        if (categoryIndex == 4) { // Language category
            switch (settingIndex) {
                case 0: // LINGUA GIOCO
                    if (currentLanguageCode.equals("italiano")) return "ITALIANO";
                    else if (currentLanguageCode.equals("inglese")) return "ENGLISH";
                    else return "ESPAÑOL";
                default: return "";
            }
        }
        
        // Map setting identifier to original index for compatibility
        for (int i = 0; i < settingNames.length; i++) {
            if (settingNames[i].equals(settingName)) {
                return getCurrentSettingValue(i);
            }
        }
        return "";
    }
    
    private String getCurrentSettingValue(int settingIndex) {
        switch (settingIndex) {
            case 0: return paddleSpeedOptions[paddleSpeedSetting];
            case 1: return aiDifficultyOptions[aiDifficultySetting];
            case 2: return String.valueOf(ballSpeedSetting); // Show numeric value
            case 3: return waitingForKeyInput == 3 ? "Premi tasto..." : getKeyDisplayName(player1UpKey);
            case 4: return waitingForKeyInput == 4 ? "Premi tasto..." : getKeyDisplayName(player1DownKey);
            case 5: return waitingForKeyInput == 5 ? "Premi tasto..." : getKeyDisplayName(player2UpKey);
            case 6: return waitingForKeyInput == 6 ? "Premi tasto..." : getKeyDisplayName(player2DownKey);
            default: return "";
        }
    }
    
    private String getKeyDisplayName(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP: return "FRECCIA SU";
            case KeyEvent.VK_DOWN: return "FRECCIA GIU";
            case KeyEvent.VK_LEFT: return "FRECCIA SINISTRA";
            case KeyEvent.VK_RIGHT: return "FRECCIA DESTRA";
            case KeyEvent.VK_SPACE: return "SPAZIO";
            case KeyEvent.VK_ENTER: return "INVIO";
            case KeyEvent.VK_ESCAPE: return "ESC";
            case KeyEvent.VK_BACK_SPACE: return "BACKSPACE";
            case KeyEvent.VK_SHIFT: return "SHIFT";
            case KeyEvent.VK_CONTROL: return "CTRL";
            case KeyEvent.VK_ALT: return "ALT";
            default: return KeyEvent.getKeyText(keyCode);
        }
    }
    
    private int getCurrentKeyForSetting(int settingIndex) {
        switch (settingIndex) {
            case 3: return player1UpKey;
            case 4: return player1DownKey;
            case 5: return player2UpKey;
            case 6: return player2DownKey;
            default: return KeyEvent.VK_SPACE;
        }
    }
    
    private void setKeyForSetting(int settingIndex, int keyCode) {
        switch (settingIndex) {
            case 3: player1UpKey = keyCode; break;
            case 4: player1DownKey = keyCode; break;
            case 5: player2UpKey = keyCode; break;
            case 6: player2DownKey = keyCode; break;
        }
    }
    
    private boolean canDecreaseSetting(int settingIndex) {
        switch (settingIndex) {
            case 0: return paddleSpeedSetting > 0; // Velocità Paddle (0-2)
            case 1: return aiDifficultySetting > 0; // Difficoltà IA (0-4)
            case 2: return ballSpeedSetting > 5; // Velocità Palla (5-100)
            case 3:
            case 4:
            case 5:
            case 6: return true; // I tasti si possono sempre cambiare
            default: return false;
        }
    }
    
    private boolean canIncreaseSetting(int settingIndex) {
        switch (settingIndex) {
            case 0: return paddleSpeedSetting < 2; // Velocità Paddle (0-2)
            case 1: return aiDifficultySetting < 4; // Difficoltà IA (0-4)
            case 2: return ballSpeedSetting < 100; // Velocità Palla (5-100)
            case 3:
            case 4:
            case 5:
            case 6: return true; // I tasti si possono sempre cambiare
            default: return false;
        }
    }
    
    private void drawDemoMode(Graphics2D g) {
        double easeProgress = easeInOutQuad(demoTransitionProgress);
        
        // Draw elements transitioning from normal settings
        drawTransitioningElements(g, easeProgress);
        
        // Draw demo paddle on the left (blue paddle transitioning)
        drawDemoPaddle(g, easeProgress);
        
        
        // Draw demo ball
        drawDemoBall(g);
        
        // Demo instructions
        drawDemoInstructions(g);
    }
    
    private void drawSettingCard(Graphics2D g, int cardType) {
        String[] options = cardType == 0 ? paddleSpeedOptions : aiDifficultyOptions;
        int currentValue = cardType == 0 ? paddleSpeedSetting : aiDifficultySetting;
        String title = cardType == 0 ? "Velocità Paddle" : "Difficoltà IA";
        
        // Card position and size
        int cardWidth = (int)(300 * Math.min(scaleX, scaleY));
        int cardHeight = (int)(120 * Math.min(scaleX, scaleY));
        int cardX = (BOARD_WIDTH - cardWidth) / 2;
        int cardY = (int)(150 * scaleY) + cardType * (int)(150 * scaleY);
        
        // Card background
        boolean isSelected = selectedSetting == cardType;
        Color cardBg = isSelected ? new Color(40, 40, 50) : new Color(25, 25, 30);
        g.setColor(cardBg);
        g.fillRoundRect(cardX, cardY, cardWidth, cardHeight, 12, 12);
        
        // Card border
        if (isSelected) {
            g.setColor(new Color(100, 150, 255));
            g.drawRoundRect(cardX, cardY, cardWidth, cardHeight, 12, 12);
        }
        
        // Title
        g.setColor(isSelected ? new Color(150, 200, 255) : new Color(180, 180, 180));
        float titleSize = (float)(18 * Math.min(scaleX, scaleY));
        g.setFont(secondaryFont.deriveFont(titleSize));
        FontMetrics titleFm = g.getFontMetrics();
        int titleX = cardX + (cardWidth - titleFm.stringWidth(title)) / 2;
        int titleY = cardY + (int)(30 * scaleY);
        g.drawString(title, titleX, titleY);
        
        // Current value
        String valueText = options[currentValue];
        float valueSize = (float)(28 * Math.min(scaleX, scaleY));
        FontMetrics valueFm = g.getFontMetrics(primaryFont.deriveFont(valueSize));
        int valueX = cardX + (cardWidth - valueFm.stringWidth(valueText)) / 2;
        int valueY = cardY + (int)(75 * scaleY);
        
        // Use special drawing for AI difficulty
        if (cardType == 1) {
            drawDifficultyText(g, valueText, valueX, valueY, valueSize, currentValue);
        } else {
            g.setColor(Color.WHITE);
            g.setFont(primaryFont.deriveFont(valueSize));
            g.drawString(valueText, valueX, valueY);
        }
        
        // Navigation arrows (using simple text arrows that work)
        if (isSelected) {
            g.setColor(new Color(100, 150, 255));
            float arrowSize = (float)(24 * Math.min(scaleX, scaleY));
            g.setFont(primaryFont.deriveFont(arrowSize));
            
            // Left arrow
            if (currentValue > 0) {
                g.drawString("<", cardX + (int)(20 * scaleX), valueY);
            }
            // Right arrow  
            if (currentValue < options.length - 1) {
                g.drawString(">", cardX + cardWidth - (int)(30 * scaleX), valueY);
            }
            
            // Add instruction for velocity card (cardType == 0)
            if (cardType == 0) {
                g.setColor(new Color(120, 160, 220));
                float instructSize = (float)(12 * Math.min(scaleX, scaleY));
                g.setFont(secondaryFont.deriveFont(instructSize));
                FontMetrics instructFm = g.getFontMetrics();
                
                String instruction = getText("SETTINGS_PRESS_ENTER_TEST");
                int instructX = cardX + (cardWidth - instructFm.stringWidth(instruction)) / 2;
                int instructY = valueY + (int)(25 * scaleY); // Below the arrows
                
                g.drawString(instruction, instructX, instructY);
            }
        }
    }
    
    private void drawHorizontalPaddles(Graphics2D g) {
        // Paddle dimensions
        int paddleWidth = BOARD_WIDTH / 2 - (int)(30 * scaleX);
        int paddleHeight = (int)(60 * scaleY);
        int paddleY = BOARD_HEIGHT - paddleHeight - (int)(40 * scaleY);
        
        int leftPaddleX = (int)(15 * scaleX);
        int rightPaddleX = BOARD_WIDTH / 2 + (int)(15 * scaleX);
        
        // Left paddle (blue)
        boolean leftSelected = selectedSetting == 0;
        Color leftColor = leftSelected ? new Color(100, 150, 255) : new Color(60, 90, 150);
        g.setColor(leftColor);
        g.fillRoundRect(leftPaddleX, paddleY, paddleWidth, paddleHeight, 8, 8);
        
        // Right paddle (red)
        boolean rightSelected = selectedSetting == 1;
        Color rightColor = rightSelected ? new Color(255, 100, 100) : new Color(150, 60, 60);
        g.setColor(rightColor);
        g.fillRoundRect(rightPaddleX, paddleY, paddleWidth, paddleHeight, 8, 8);
    }
    
    
    private void drawMinimalInstructions(Graphics2D g) {
        g.setColor(new Color(120, 120, 120));
        float instructSize = (float)(14 * Math.min(scaleX, scaleY));
        g.setFont(secondaryFont.deriveFont(instructSize));
        FontMetrics instructFm = g.getFontMetrics();
        
        String instruct = "SU GIU per sezione  |  < > per valore  |  SPAZIO per test  |  ENTER per continuare";
        int instructX = (BOARD_WIDTH - instructFm.stringWidth(instruct)) / 2;
        int instructY = (int)(BOARD_HEIGHT - 15 * scaleY);
        g.drawString(instruct, instructX, instructY);
    }
    
    private void drawDemoToMenuTransition(Graphics2D g) {
        double progress = easeInOutQuad(demoToMenuProgress);
        
        // Background
        g.setColor(new Color(15, 15, 15));
        g.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
        
        // Get demo paddle positions (starting positions)
        int demoPanelWidth = (int)(BOARD_WIDTH * 0.4);
        int demoPanelStartX = BOARD_WIDTH - demoPanelWidth;
        int demoRightPaddleX = demoPanelStartX - PADDLE_WIDTH - (int)(10 * scaleX);
        int demoLeftPaddleX = (int)(20 * scaleX);
        
        // Get menu paddle positions (target positions)
        int menuWidePaddleWidth = (int)(250 * Math.min(scaleX, scaleY));
        int menuPaddleHeight = (int)(this.menuPaddleHeight * 1.8);
        int menuPaddleYOffset = (int)(-menuPaddleHeight * 0.2);
        int menuLeftCenterX = 0;
        int menuRightCenterX = BOARD_WIDTH;
        int menuCenterY = menuPaddleYOffset + menuPaddleHeight / 2;
        
        // Interpolate paddle properties
        // Left paddle
        double leftX = demoLeftPaddleX + (menuLeftCenterX - demoLeftPaddleX) * progress;
        double leftY = demoPaddleY + (menuCenterY - demoPaddleY) * progress;
        double leftWidth = PADDLE_WIDTH + (menuWidePaddleWidth - PADDLE_WIDTH) * progress;
        double leftHeight = PADDLE_HEIGHT + (menuPaddleHeight - PADDLE_HEIGHT) * progress;
        double leftRotation = -25.0 * progress;
        
        // Right paddle  
        double rightX = demoRightPaddleX + (menuRightCenterX - demoRightPaddleX) * progress;
        double rightY = demoRedPaddleY + (menuCenterY - demoRedPaddleY) * progress;
        double rightWidth = PADDLE_WIDTH + (menuWidePaddleWidth - PADDLE_WIDTH) * progress;
        double rightHeight = PADDLE_HEIGHT + (menuPaddleHeight - PADDLE_HEIGHT) * progress;
        double rightRotation = 25.0 * progress;
        
        // Draw transitioning paddles
        drawTransitionPaddle(g, leftX, leftY, leftWidth, leftHeight, leftRotation, 
                           new Color(100, 150, 255), new Color(150, 200, 255));
        drawTransitionPaddle(g, rightX, rightY, rightWidth, rightHeight, rightRotation,
                           new Color(255, 100, 100), new Color(255, 150, 150));
        
        // Fade out demo UI, fade in menu UI
        if (progress < 0.5) {
            // Fade out demo text
            double fadeOut = 1.0 - (progress * 2);
            g.setColor(new Color(255, 255, 255, (int)(255 * fadeOut * 0.5)));
            float textSize = (float)(16 * Math.min(scaleX, scaleY));
            g.setFont(secondaryFont.deriveFont(textSize));
            String demoText = "DEMO - ENTER per continuare";
            FontMetrics fm = g.getFontMetrics();
            int textX = (BOARD_WIDTH - fm.stringWidth(demoText)) / 2;
            int textY = BOARD_HEIGHT - (int)(30 * scaleY);
            g.drawString(demoText, textX, textY);
        }
        
        if (progress > 0.5) {
            // Fade in menu title
            double fadeIn = (progress - 0.5) * 2;
            g.setColor(new Color(255, 255, 255, (int)(255 * fadeIn)));
            float titleSize = (float)(48 * Math.min(scaleX, scaleY) * fadeIn);
            if (titleSize > 0) {
                g.setFont(primaryFont.deriveFont(titleSize));
                String title = "PONG PING";
                FontMetrics titleFm = g.getFontMetrics();
                int titleX = (BOARD_WIDTH - titleFm.stringWidth(title)) / 2;
                int titleY = (int)(150 * scaleY);
                g.drawString(title, titleX, titleY);
            }
        }
        
        // Demo ball fades out
        if (progress < 0.8) {
            double ballAlpha = 1.0 - (progress / 0.8);
            g.setColor(new Color(255, 255, 255, (int)(255 * ballAlpha)));
            g.fillOval((int)demoBallX, (int)demoBallY, BALL_SIZE, BALL_SIZE);
        }
    }
    
    private void drawTransitionPaddle(Graphics2D g, double x, double y, double width, double height,
                                     double rotation, Color color1, Color color2) {
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        g.translate(x, y);
        g.rotate(Math.toRadians(rotation));
        
        int cornerRadius = (int)(8 * Math.min(scaleX, scaleY));
        
        // Check if this is the left paddle (blue colors) and apply selected theme
        boolean isLeftPaddle = (color1.equals(new Color(100, 150, 255)) && color2.equals(new Color(150, 200, 255)));
        
        if (isLeftPaddle && selectedPaddleTheme >= 0 && selectedPaddleTheme < bluePaddleThemeImages.size()) {
            BufferedImage paddleImg = bluePaddleThemeImages.get(selectedPaddleTheme);
            
            if (paddleImg != null) {
                // Custom theme paddle with rounded corners and clipping
                g.setClip(new java.awt.geom.RoundRectangle2D.Float((float)(-width/2), (float)(-height/2), (float)width, (float)height, cornerRadius, cornerRadius));
                g.drawImage(paddleImg, (int)(-width/2), (int)(-height/2), (int)width, (int)height, this);
                g.setClip(null);
            } else {
                // Default gradient for left paddle
                GradientPaint gradient = new GradientPaint(
                    (float)(-width/2), (float)(-height/2), color1,
                    (float)(width/2), (float)(height/2), color2);
                g.setPaint(gradient);
                g.fillRoundRect((int)(-width/2), (int)(-height/2), (int)width, (int)height, cornerRadius, cornerRadius);
            }
        } else {
            // Default gradient for right paddle or fallback
            GradientPaint gradient = new GradientPaint(
                (float)(-width/2), (float)(-height/2), color1,
                (float)(width/2), (float)(height/2), color2);
            g.setPaint(gradient);
            g.fillRoundRect((int)(-width/2), (int)(-height/2), (int)width, (int)height, cornerRadius, cornerRadius);
        }
        
        g.setTransform(originalTransform);
    }
    
    
    private void drawTransitioningElements(Graphics2D g, double progress) {
        // Title transitions from center "IMPOSTAZIONI" to right "TEST VELOCITÀ"
        drawTransitioningTitle(g, progress);
        
        // Setting cards move from center to right panel
        drawTransitioningSettingCards(g, progress);
        
        // Red paddle (right paddle) stays in place and remains visible
        drawTransitioningRedPaddle(g, progress);
        
        // Blue paddle transitions with rotation (shown during both directions)
        drawDemoPaddle(g, progress);
    }
    
    private void drawTransitioningTitle(Graphics2D g, double progress) {
        g.setColor(Color.WHITE);
        
        // Title size transitions from larger to smaller
        float startSize = (float)(36 * Math.min(scaleX, scaleY));
        float endSize = (float)(28 * Math.min(scaleX, scaleY));
        float currentSize = startSize + (endSize - startSize) * (float)progress;
        g.setFont(primaryFont.deriveFont(currentSize));
        FontMetrics fm = g.getFontMetrics();
        
        // Title text changes gradually
        String startTitle = "IMPOSTAZIONI";
        String endTitle = "VELOCITÀ PADDLE";
        String currentTitle = progress < 0.5 ? startTitle : endTitle;
        
        // Calculate right panel position to align horizontally with selectors
        int panelWidth = (int)(BOARD_WIDTH * 0.4);
        int panelStartX = BOARD_WIDTH - panelWidth;
        int currentPanelX = (int)(BOARD_WIDTH + (panelStartX - BOARD_WIDTH) * progress);
        
        // Position transitions from center to aligned with right panel
        int startX = (BOARD_WIDTH - fm.stringWidth(currentTitle)) / 2;
        int endX = currentPanelX + (panelWidth - fm.stringWidth(currentTitle)) / 2; // Centered in right panel
        int currentX = (int)(startX + (endX - startX) * progress);
        
        // Y position remains at original height
        int currentY = (int)(80 * scaleY); // Keep original IMPOSTAZIONI height
        
        g.drawString(currentTitle, currentX, currentY);
    }
    
    private void drawTransitioningSettingCards(Graphics2D g, double progress) {
        // Setting cards move from center to right panel
        int panelWidth = (int)(BOARD_WIDTH * 0.4);
        int panelStartX = BOARD_WIDTH - panelWidth;
        int currentPanelX = (int)(BOARD_WIDTH + (panelStartX - BOARD_WIDTH) * progress);
        
        // Draw both cards in their transitioning positions
        drawTransitioningCard(g, 0, progress, currentPanelX, panelWidth); // Paddle speed
        drawTransitioningCard(g, 1, progress, currentPanelX, panelWidth); // AI difficulty
    }
    
    private void drawTransitioningCard(Graphics2D g, int cardType, double progress, int panelX, int panelWidth) {
        String[] options = cardType == 0 ? paddleSpeedOptions : aiDifficultyOptions;
        int currentValue = cardType == 0 ? paddleSpeedSetting : aiDifficultySetting;
        String title = cardType == 0 ? "Velocità Paddle" : "Difficoltà IA";
        
        // Calculate card positions - transitioning from normal settings position to demo position
        int cardWidth = (int)(300 * Math.min(scaleX, scaleY)); // Keep original size
        int cardHeight = (int)(120 * Math.min(scaleX, scaleY)); // Keep original size
        int normalCardX = (BOARD_WIDTH - cardWidth) / 2;
        int cardY = (int)(150 * scaleY) + cardType * (int)(150 * scaleY); // Original Y positions from drawSettingCard
        
        // Demo position (same size, in right panel, keeping original Y positions)
        int demoCardX = panelX + (panelWidth - cardWidth) / 2;
        
        // Only X position changes, size and Y remain constant
        int currentX = (int)(normalCardX + (demoCardX - normalCardX) * progress);
        int currentY = cardY; // Y position remains constant
        int currentWidth = cardWidth; // Size remains constant
        int currentHeight = cardHeight; // Size remains constant
        
        // Card background (same style as original settings) - always selected in demo mode
        boolean isSelected = isDemoMode ? true : (cardType == 0 && selectedSetting == 0) || (cardType == 1 && selectedSetting == 1);
        Color cardBg = isSelected ? new Color(40, 40, 50) : new Color(25, 25, 30);
        g.setColor(cardBg);
        g.fillRoundRect(currentX, currentY, currentWidth, currentHeight, 12, 12); // Same border radius as original
        
        // Card border (same style as original settings)
        if (isSelected) {
            g.setColor(new Color(100, 150, 255));
            g.drawRoundRect(currentX, currentY, currentWidth, currentHeight, 12, 12);
        }
        
        // Title (exactly same style as original settings)
        g.setColor(isSelected ? new Color(150, 200, 255) : new Color(180, 180, 180));
        float titleSize = (float)(18 * Math.min(scaleX, scaleY)); // Exact original size
        g.setFont(secondaryFont.deriveFont(titleSize));
        FontMetrics titleFm = g.getFontMetrics();
        int titleX = currentX + (currentWidth - titleFm.stringWidth(title)) / 2;
        int titleY = currentY + (int)(30 * scaleY); // Same as original
        g.drawString(title, titleX, titleY);
        
        // Current value (exactly same style as original settings)
        String valueText = options[currentValue];
        float valueSize = (float)(28 * Math.min(scaleX, scaleY)); // Exact original size
        FontMetrics valueFm = g.getFontMetrics(primaryFont.deriveFont(valueSize));
        int valueX = currentX + (currentWidth - valueFm.stringWidth(valueText)) / 2;
        int valueY = currentY + (int)(75 * scaleY); // Same as original
        
        // Use special drawing for AI difficulty
        if (cardType == 1) {
            drawDifficultyText(g, valueText, valueX, valueY, valueSize, currentValue);
        } else {
            g.setColor(Color.WHITE); // Exact original color
            g.setFont(primaryFont.deriveFont(valueSize));
            g.drawString(valueText, valueX, valueY);
        }
        
        // Navigation arrows (only for paddle speed card)
        if (isSelected && cardType == 0) {
            g.setColor(new Color(100, 150, 255)); // No fade, same as original
            float arrowSize = (float)(24 * Math.min(scaleX, scaleY)); // Keep original size
            g.setFont(primaryFont.deriveFont(arrowSize));
            
            if (currentValue > 0) {
                g.drawString("<", currentX + (int)(20 * scaleX), valueY); // Same position as original
            }
            if (currentValue < options.length - 1) {
                g.drawString(">", currentX + currentWidth - (int)(30 * scaleX), valueY); // Same position as original
            }
        }
        
        // Add instruction text for both cards
        if (isSelected) {
            g.setColor(new Color(120, 160, 220));
            float instructSize = (float)(12 * Math.min(scaleX, scaleY));
            g.setFont(secondaryFont.deriveFont(instructSize));
            FontMetrics instructFm = g.getFontMetrics();
            
            String instruction = cardType == 0 ? getText("SETTINGS_PRESS_ARROWS_PADDLE") : getText("SETTINGS_PRESS_SPACE_CHANGE");
            int instructX = currentX + (currentWidth - instructFm.stringWidth(instruction)) / 2;
            int instructY = valueY + (int)(25 * scaleY); // Below the arrows/value
            
            g.drawString(instruction, instructX, instructY);
        }
    }
    
    private void drawTransitioningRedPaddle(Graphics2D g, double progress) {
        // Red paddle transitions from horizontal position to demo position (like blue paddle)
        
        // Start position: red horizontal paddle position (from right side of horizontal paddles)
        int startX = BOARD_WIDTH / 2 + (int)(15 * scaleX);
        int startY = BOARD_HEIGHT - (int)(60 * scaleY) - (int)(40 * scaleY);
        int startWidth = BOARD_WIDTH / 2 - (int)(30 * scaleX);
        int startHeight = (int)(60 * scaleY);
        
        // Calculate end position: before the AI difficulty card in right panel
        int panelWidth = (int)(BOARD_WIDTH * 0.4);
        int panelStartX = BOARD_WIDTH - panelWidth;
        int currentPanelX = (int)(BOARD_WIDTH + (panelStartX - BOARD_WIDTH) * progress);
        
        // End position: before the AI difficulty card
        int endX = currentPanelX - PADDLE_WIDTH - (int)(10 * scaleX);
        int endY = (int)demoRedPaddleY;
        
        // Target dimensions: exact same as game paddle
        int targetWidth = PADDLE_WIDTH;
        int targetHeight = PADDLE_HEIGHT;
        
        // Interpolate position
        int currentX = (int)(startX + (endX - startX) * progress);
        int currentY = (int)(startY + (endY - startY) * progress);
        
        // Interpolate dimensions to reach exact game paddle size
        int currentWidth = (int)(startWidth + (targetWidth - startWidth) * progress);
        int currentHeight = (int)(startHeight + (targetHeight - startHeight) * progress);
        
        // Red paddle color (based on selection state)
        boolean rightSelected = selectedSetting == 1;
        Color baseColor = rightSelected ? new Color(255, 100, 100) : new Color(150, 60, 60);
        Color gradientColor = rightSelected ? new Color(255, 150, 150) : new Color(200, 100, 100);
        
        GradientPaint paddleGradient = new GradientPaint(
            currentX, currentY, baseColor,
            currentX + currentWidth, currentY + currentHeight, gradientColor);
        g.setPaint(paddleGradient);
        
        int cornerRadius = Math.max(4, currentWidth / 4);
        g.fillRoundRect(currentX, currentY, currentWidth, currentHeight, cornerRadius, cornerRadius);
        
        // Paddle glow
        g.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 100));
        g.drawRoundRect(currentX - 2, currentY - 2, currentWidth + 4, currentHeight + 4, cornerRadius + 2, cornerRadius + 2);
    }
    
    
    
    private void drawDemoElements(Graphics2D g, double progress) {
        // Calculate right panel position
        int panelWidth = (int)(BOARD_WIDTH * 0.4); // 40% of screen width
        int panelStartX = BOARD_WIDTH - panelWidth;
        int currentPanelX = (int)(BOARD_WIDTH + (panelStartX - BOARD_WIDTH) * progress);
        
        // Draw both setting cards in right panel
        drawDemoCard(g, 0, currentPanelX, panelWidth, progress); // Paddle speed
        drawDemoCard(g, 1, currentPanelX, panelWidth, progress); // AI difficulty
        
        // Draw horizontal paddles that move to right panel
        drawDemoHorizontalPaddles(g, currentPanelX, panelWidth, progress);
    }
    
    private void drawDemoCard(Graphics2D g, int cardType, int panelX, int panelWidth, double progress) {
        String[] options = cardType == 0 ? paddleSpeedOptions : aiDifficultyOptions;
        int currentValue = cardType == 0 ? paddleSpeedSetting : aiDifficultySetting;
        String title = cardType == 0 ? "Velocità Paddle" : "Difficoltà IA";
        
        // Card position in right panel
        int cardWidth = (int)(250 * Math.min(scaleX, scaleY));
        // Make AI difficulty card taller to accommodate extra text
        int cardHeight = cardType == 1 ? (int)(110 * Math.min(scaleX, scaleY)) : (int)(80 * Math.min(scaleX, scaleY));
        int cardX = panelX + (panelWidth - cardWidth) / 2;
        int cardY = (int)(120 * scaleY) + cardType * (int)(100 * scaleY);
        
        // Only draw if visible
        if (cardX < BOARD_WIDTH) {
            // Card background - always selected in demo mode
            boolean isSelected = true; // Both cards always appear selected in demo
            Color cardBg = new Color(40, 40, 50);
            g.setColor(cardBg);
            g.fillRoundRect(cardX, cardY, cardWidth, cardHeight, 12, 12);
            
            // Card border - always show border in demo
            g.setColor(new Color(100, 150, 255));
            g.drawRoundRect(cardX, cardY, cardWidth, cardHeight, 12, 12);
            
            // Title
            g.setColor(isSelected ? new Color(150, 200, 255) : new Color(180, 180, 180));
            float titleSize = (float)(14 * Math.min(scaleX, scaleY));
            g.setFont(secondaryFont.deriveFont(titleSize));
            FontMetrics titleFm = g.getFontMetrics();
            int titleX = cardX + (cardWidth - titleFm.stringWidth(title)) / 2;
            int titleY = cardY + (int)(20 * scaleY);
            g.drawString(title, titleX, titleY);
            
            // Current value
            String valueText = options[currentValue];
            float valueSize = (float)(20 * Math.min(scaleX, scaleY));
            FontMetrics valueFm = g.getFontMetrics(primaryFont.deriveFont(valueSize));
            int valueX = cardX + (cardWidth - valueFm.stringWidth(valueText)) / 2;
            int valueY = cardY + (int)(50 * scaleY);
            
            // Use special drawing for AI difficulty
            if (cardType == 1) {
                drawDifficultyText(g, valueText, valueX, valueY, valueSize, currentValue);
            } else {
                g.setColor(Color.WHITE);
                g.setFont(primaryFont.deriveFont(valueSize));
                g.drawString(valueText, valueX, valueY);
            }
            
            // Navigation arrows (only for selected and only for paddle speed card)
            if (isSelected && cardType == 0) {
                g.setColor(new Color(100, 150, 255));
                float arrowSize = (float)(16 * Math.min(scaleX, scaleY));
                g.setFont(primaryFont.deriveFont(arrowSize));
                
                if (currentValue > 0) {
                    g.drawString("<", cardX + (int)(10 * scaleX), valueY);
                }
                if (currentValue < options.length - 1) {
                    g.drawString(">", cardX + cardWidth - (int)(20 * scaleX), valueY);
                }
            }
            
            // Add instruction text for both cards
            g.setColor(new Color(150, 180, 255));
            float instructSize = (float)(12 * Math.min(scaleX, scaleY));
            g.setFont(secondaryFont.deriveFont(instructSize));
            FontMetrics instructFm = g.getFontMetrics();
            
            String instruction = cardType == 0 ? getText("SETTINGS_PRESS_ARROWS_PADDLE") : getText("SETTINGS_PRESS_SPACE_CHANGE");
            int instructX = cardX + (cardWidth - instructFm.stringWidth(instruction)) / 2;
            int instructY = cardY + cardHeight - (int)(15 * scaleY); // Position near bottom of card
            
            g.drawString(instruction, instructX, instructY);
        }
    }
    
    private void drawDemoHorizontalPaddles(Graphics2D g, int panelX, int panelWidth, double progress) {
        // Paddle dimensions
        int paddleWidth = panelWidth / 2 - (int)(15 * scaleX);
        int paddleHeight = (int)(40 * scaleY);
        int paddleY = BOARD_HEIGHT - paddleHeight - (int)(40 * scaleY);
        
        int leftPaddleX = panelX + (int)(10 * scaleX);
        int rightPaddleX = panelX + panelWidth / 2 + (int)(5 * scaleX);
        
        // Only draw if visible
        if (leftPaddleX < BOARD_WIDTH) {
            // Left paddle (blue)
            boolean leftSelected = selectedSetting == 0;
            Color leftColor = leftSelected ? new Color(100, 150, 255) : new Color(60, 90, 150);
            g.setColor(leftColor);
            g.fillRoundRect(leftPaddleX, paddleY, paddleWidth, paddleHeight, 8, 8);
            
            // Right paddle (red) - only if there's space
            if (rightPaddleX + paddleWidth <= BOARD_WIDTH) {
                boolean rightSelected = selectedSetting == 1;
                Color rightColor = rightSelected ? new Color(255, 100, 100) : new Color(150, 60, 60);
                g.setColor(rightColor);
                g.fillRoundRect(rightPaddleX, paddleY, paddleWidth, paddleHeight, 8, 8);
            }
        }
    }
    
    private void drawSettingCardOffset(Graphics2D g, int cardType, int xOffset) {
        String[] options = cardType == 0 ? paddleSpeedOptions : aiDifficultyOptions;
        int currentValue = cardType == 0 ? paddleSpeedSetting : aiDifficultySetting;
        String title = cardType == 0 ? "Velocità Paddle" : "Difficoltà IA";
        
        // Card position and size (moved right)
        int cardWidth = (int)(250 * Math.min(scaleX, scaleY)); // Smaller in demo
        int cardHeight = (int)(100 * Math.min(scaleX, scaleY));
        int cardX = BOARD_WIDTH - cardWidth - (int)(20 * scaleX) - xOffset;
        int cardY = (int)(120 * scaleY);
        
        // Card background - always selected in demo mode
        boolean isSelected = isDemoMode ? true : selectedSetting == cardType;
        Color cardBg = isSelected ? new Color(40, 40, 50) : new Color(25, 25, 30);
        g.setColor(cardBg);
        g.fillRoundRect(cardX, cardY, cardWidth, cardHeight, 12, 12);
        
        // Card border
        if (isSelected) {
            g.setColor(new Color(100, 150, 255));
            g.drawRoundRect(cardX, cardY, cardWidth, cardHeight, 12, 12);
        }
        
        // Title
        g.setColor(isSelected ? new Color(150, 200, 255) : new Color(180, 180, 180));
        float titleSize = (float)(16 * Math.min(scaleX, scaleY));
        g.setFont(secondaryFont.deriveFont(titleSize));
        FontMetrics titleFm = g.getFontMetrics();
        int titleX = cardX + (cardWidth - titleFm.stringWidth(title)) / 2;
        int titleY = cardY + (int)(25 * scaleY);
        g.drawString(title, titleX, titleY);
        
        // Current value
        String valueText = options[currentValue];
        float valueSize = (float)(24 * Math.min(scaleX, scaleY));
        FontMetrics valueFm = g.getFontMetrics(primaryFont.deriveFont(valueSize));
        int valueX = cardX + (cardWidth - valueFm.stringWidth(valueText)) / 2;
        int valueY = cardY + (int)(65 * scaleY);
        
        // Use special drawing for AI difficulty
        if (cardType == 1) {
            drawDifficultyText(g, valueText, valueX, valueY, valueSize, currentValue);
        } else {
            g.setColor(Color.WHITE);
            g.setFont(primaryFont.deriveFont(valueSize));
            g.drawString(valueText, valueX, valueY);
        }
        
        // Navigation arrows (only for paddle speed card)
        if (isSelected && cardType == 0) {
            g.setColor(new Color(100, 150, 255));
            float arrowSize = (float)(20 * Math.min(scaleX, scaleY));
            g.setFont(primaryFont.deriveFont(arrowSize));
            
            if (currentValue > 0) {
                g.drawString("<", cardX + (int)(15 * scaleX), valueY);
            }
            if (currentValue < options.length - 1) {
                g.drawString(">", cardX + cardWidth - (int)(25 * scaleX), valueY);
            }
        }
        
        // Add instruction text for both cards
        g.setColor(new Color(150, 180, 255));
        float instructSize = (float)(11 * Math.min(scaleX, scaleY));
        g.setFont(secondaryFont.deriveFont(instructSize));
        FontMetrics instructFm = g.getFontMetrics();
        
        String instruction = cardType == 0 ? getText("SETTINGS_PRESS_ARROWS_PADDLE") : getText("SETTINGS_PRESS_SPACE_CHANGE");
        int instructX = cardX + (cardWidth - instructFm.stringWidth(instruction)) / 2;
        int instructY = cardY + cardHeight - (int)(12 * scaleY); // Position near bottom of card
        
        g.drawString(instruction, instructX, instructY);
    }
    
    private void drawDemoPaddle(Graphics2D g, double progress) {
        // Blue paddle animates between horizontal position and demo position with rotation
        // For reverse transition, we only show the paddle during the transition
        
        if (isTransitioningFromDemo && progress <= 0.0) {
            return; // Don't show paddle when back to normal settings
        }
        
        // Start position: blue horizontal paddle position (from left side of horizontal paddles)
        int startX = (int)(15 * scaleX);
        int startY = BOARD_HEIGHT - (int)(60 * scaleY) - (int)(40 * scaleY);
        int startWidth = BOARD_WIDTH / 2 - (int)(30 * scaleX);
        int startHeight = (int)(60 * scaleY);
        
        // End position: exact same as game paddle blue (left paddle)
        int endX = (int)(20 * scaleX); // Same X position as game left paddle
        int endY = (int)demoPaddleY;
        
        // Interpolate position
        int currentX = (int)(startX + (endX - startX) * progress);
        int currentY = (int)(startY + (endY - startY) * progress);
        
        // Target dimensions: exact same as game paddle (no rotation in dimensions)
        int targetWidth = PADDLE_WIDTH;   // Same width as game paddle
        int targetHeight = PADDLE_HEIGHT; // Same height as game paddle
        
        // Interpolate dimensions to reach exact game paddle size
        int currentWidth = (int)(startWidth + (targetWidth - startWidth) * progress);
        int currentHeight = (int)(startHeight + (targetHeight - startHeight) * progress);
        
        // Draw paddle without rotation (simple transition with position and size changes only)
        Color paddleColor = new Color(100, 150, 255);
        Color gradientColor = new Color(150, 200, 255);
        
        GradientPaint paddleGradient = new GradientPaint(
            currentX, currentY, paddleColor,
            currentX + currentWidth, currentY + currentHeight, gradientColor);
        g.setPaint(paddleGradient);
        
        int cornerRadius = Math.max(4, currentWidth / 4);
        g.fillRoundRect(currentX, currentY, currentWidth, currentHeight, cornerRadius, cornerRadius);
        
        // Paddle glow
        g.setColor(getPaddleGlowColor(true));
        g.drawRoundRect(currentX - 2, currentY - 2, currentWidth + 4, currentHeight + 4, cornerRadius + 2, cornerRadius + 2);
    }
    
    
    private void drawDemoBall(Graphics2D g) {
        // Draw ball with same style as game ball
        int ballX = (int)demoBallX;
        int ballY = (int)demoBallY;
        
        // Use scaled ball size like in the game
        int scaledBallSize = BALL_SIZE; // Same scaling as game ball
        
        // Ball glow
        g.setColor(new Color(255, 255, 255, 30));
        g.fillOval(ballX - 3, ballY - 3, scaledBallSize + 6, scaledBallSize + 6);
        
        // Ball gradient  
        Color ballCenter = new Color(255, 255, 255);
        Color ballEdge = new Color(200, 200, 255);
        
        GradientPaint ballGradient = new GradientPaint(
            ballX, ballY, ballCenter,
            ballX + scaledBallSize, ballY + scaledBallSize, ballEdge);
        g.setPaint(ballGradient);
        
        g.fillOval(ballX, ballY, scaledBallSize, scaledBallSize);
        
        // Ball highlight
        g.setColor(new Color(255, 255, 255, 200));
        int highlightSize = scaledBallSize / 3;
        g.fillOval(ballX + highlightSize/2, ballY + highlightSize/2, 
                   highlightSize, highlightSize);
    }
    
    private void drawDemoInstructions(Graphics2D g) {
        g.setColor(Color.WHITE);
        float instructSize = (float)(24 * Math.min(scaleX, scaleY)); // Large white text
        g.setFont(primaryFont.deriveFont(instructSize));
        FontMetrics instructFm = g.getFontMetrics();
        
        String instruct = getText("SETTINGS_PRESS_ENTER_CONTINUE");
        int instructX = (BOARD_WIDTH - instructFm.stringWidth(instruct)) / 2;
        int instructY = (int)(BOARD_HEIGHT - 40 * scaleY);
        g.drawString(instruct, instructX, instructY);
    }
    
    private void drawDifficultyText(Graphics2D g, String text, int x, int y, float fontSize, int difficulty) {
        switch (difficulty) {
            case 0: // FACILE - Verde
                g.setColor(new Color(0, 200, 0));
                g.setFont(primaryFont.deriveFont(fontSize));
                g.drawString(text, x, y);
                break;
                
            case 1: // NORMALE - Bianco
                g.setColor(Color.WHITE);
                g.setFont(primaryFont.deriveFont(fontSize));
                g.drawString(text, x, y);
                break;
                
            case 2: // DIFFICILE - Rosso
                g.setColor(new Color(255, 50, 50));
                g.setFont(primaryFont.deriveFont(fontSize));
                g.drawString(text, x, y);
                break;
                
            case 3: // ESPERTO - Rosso e arancione con vibrazione
                drawExpertText(g, text, x, y, fontSize);
                break;
                
            case 4: // IMPOSSIBILE - Rosso, arancione, giallo con fiamme e vibrazione
                drawImpossibleText(g, text, x, y, fontSize);
                break;
        }
    }
    
    private void drawExpertText(Graphics2D g, String text, int x, int y, float fontSize) {
        // Piccola vibrazione
        double shake = Math.sin(difficultyAnimationTime * 12) * 1.5;
        int shakeX = (int)(x + shake);
        int shakeY = (int)(y + Math.cos(difficultyAnimationTime * 10) * 1.5);
        
        // Gradiente rosso-arancione
        double phase = Math.sin(difficultyAnimationTime * 3) * 0.5 + 0.5;
        int red = 255;
        int green = (int)(100 + phase * 155); // Da 100 a 255
        Color expertColor = new Color(red, green, 0);
        
        g.setColor(expertColor);
        g.setFont(primaryFont.deriveFont(fontSize));
        g.drawString(text, shakeX, shakeY);
    }
    
    private void drawImpossibleText(Graphics2D g, String text, int x, int y, float fontSize) {
        // Grande vibrazione
        double shake = Math.sin(difficultyAnimationTime * 15) * 3;
        int shakeX = (int)(x + shake);
        int shakeY = (int)(y + Math.cos(difficultyAnimationTime * 12) * 3);
        
        // Effetto fiamme - gradiente rosso-arancione-giallo che cambia
        double phase1 = Math.sin(difficultyAnimationTime * 4) * 0.5 + 0.5;
        double phase2 = Math.cos(difficultyAnimationTime * 5) * 0.5 + 0.5;
        
        int red = 255;
        int green = (int)(100 + phase1 * 155); // Da 100 a 255
        int yellow = (int)(phase2 * 255); // Da 0 a 255
        
        // Disegna più copie con offset per effetto fiamma
        for (int i = 0; i < 3; i++) {
            double flamePhase = difficultyAnimationTime * 6 + i * 0.5;
            int flameOffset = (int)(Math.sin(flamePhase) * 2);
            
            Color flameColor = new Color(red, Math.max(0, green - i * 20), Math.max(0, yellow - i * 50));
            g.setColor(flameColor);
            g.setFont(primaryFont.deriveFont(fontSize));
            g.drawString(text, shakeX + flameOffset, shakeY + flameOffset);
        }
    }
    
    private void drawMenuBackground(Graphics2D g) {
        if (selectedBackground >= 0 && selectedBackground < backgroundImages.size()) {
            Image backgroundImg = backgroundImages.get(selectedBackground);
            
            if (backgroundImg != null) {
                // Draw background image scaled to full screen
                g.drawImage(backgroundImg, 0, 0, getWidth(), getHeight(), this);
                
                // Add contrast effect for menu only if not default black theme
                drawMenuContrastEffect(g);
            } else {
                // Draw default black background (no contrast effect needed)
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        } else {
            // Draw default black background (no contrast effect needed)
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
    
    private void drawMenuContrastEffect(Graphics2D g) {
        // Save original composite
        Composite originalComposite = g.getComposite();
        
        // Create a stronger dark overlay for better contrast on entire background (same as game)
        AlphaComposite contrastComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
        g.setComposite(contrastComposite);
        
        // Medium dark overlay to improve contrast for all menu elements (same as game)
        g.setColor(new Color(0, 0, 0, 140)); // Same strength as game
        g.fillRect(0, 0, getWidth(), getHeight());
        
        // Add subtle gradient to make it less flat (same as game)
        GradientPaint gradientOverlay = new GradientPaint(
            0, 0, new Color(0, 0, 0, 120), // Slightly lighter at top
            0, getHeight(), new Color(0, 0, 0, 160) // Darker at bottom
        );
        
        AlphaComposite gradientComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);
        g.setComposite(gradientComposite);
        g.setPaint(gradientOverlay);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        // Restore original composite and paint
        g.setComposite(originalComposite);
        g.setPaint(null);
    }
    
    private void drawMenu(Graphics2D g) {
        
        // Draw selected background theme
        drawMenuBackground(g);
        
        // Draw full-height decorative paddles
        drawMenuPaddles(g);
        
        // Draw animated background ball
        drawMenuBall(g);
        
        // Add hover effect when mouse is on background (behind text)
        if (mouseOnBackground) {
            // Create a very subtle highlight overlay
            Color hoverColor = new Color(255, 255, 255, 15); // Reduced opacity from 30 to 15
            g.setColor(hoverColor);
            g.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
            
            // Add a subtle pulsing border effect
            float borderSize = 2.0f; // Reduced from 4.0f
            float alpha = (float)(0.3 + 0.2 * Math.sin(System.currentTimeMillis() * 0.005)); // Reduced intensity
            Color borderColor = new Color(100, 150, 255, (int)(alpha * 60)); // Reduced from 100
            g.setColor(borderColor);
            
            // Draw border
            BasicStroke borderStroke = new BasicStroke(borderSize);
            g.setStroke(borderStroke);
            g.drawRect((int)borderSize/2, (int)borderSize/2, 
                      BOARD_WIDTH - (int)borderSize, BOARD_HEIGHT - (int)borderSize);
        }
        
        // Title with glow effect (scaled)
        float titleSize = (float)(48 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(titleSize));
        FontMetrics titleFm = g.getFontMetrics();
        String title = "PONG PING";
        int titleX = (BOARD_WIDTH - titleFm.stringWidth(title)) / 2;
        int titleY = (int)(150 * scaleY);
        
        // Glow effect with transition fade
        int glowOffset = Math.max(1, (int)(3 * Math.min(scaleX, scaleY)));
        int glowAlpha = (int)(50 * textFadeProgress);
        g.setColor(new Color(0, 255, 255, glowAlpha));
        for (int i = 1; i <= glowOffset; i++) {
            g.drawString(title, titleX - i, titleY - i);
            g.drawString(title, titleX + i, titleY + i);
        }
        Color titleColor = currentTextColors.getOrDefault("menuTitle", Color.WHITE);
        int titleAlpha = (int)(titleColor.getAlpha() * textFadeProgress);
        g.setColor(new Color(titleColor.getRed(), titleColor.getGreen(), titleColor.getBlue(), titleAlpha));
        g.drawString(title, titleX, titleY);
        
        // Menu items (scaled)
        float menuSize = (float)(24 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(menuSize));
        FontMetrics menuFm = g.getFontMetrics();
        int menuStartY = (int)(300 * scaleY);
        int menuSpacing = (int)(60 * scaleY);
        
        for (int i = 0; i < menuItems.length; i++) {
            // Show cyan selection for the currently selected item (unified approach)
            boolean showCyanSelection = false;
            if (!leftPaddleSelected && !rightPaddleSelected) {
                // Show cyan for selected item - always show when using keyboard navigation
                if (i == selectedMenuItem && (isUsingKeyboardNavigation || !mouseOnBackground)) {
                    showCyanSelection = true;
                }
            }
            
            if (showCyanSelection) {
                int cyanAlpha = (int)(255 * textFadeProgress);
                g.setColor(new Color(0, 255, 255, cyanAlpha));
                g.drawString("> " + menuItems[i] + " <", 
                    (BOARD_WIDTH - menuFm.stringWidth("> " + menuItems[i] + " <")) / 2, 
                    menuStartY + i * menuSpacing);
            } else {
                Color menuColor = currentTextColors.getOrDefault("menuItems", Color.WHITE);
                int menuAlpha = (int)(menuColor.getAlpha() * textFadeProgress);
                g.setColor(new Color(menuColor.getRed(), menuColor.getGreen(), menuColor.getBlue(), menuAlpha));
                g.drawString(menuItems[i], 
                    (BOARD_WIDTH - menuFm.stringWidth(menuItems[i])) / 2, 
                    menuStartY + i * menuSpacing);
            }
        }
        
        // Draw "by Gava" signature
        drawGavaSignature(g);
    }
    
    private void drawMenuPaddles(Graphics2D g) {
        // Make paddles fixed size for consistent appearance across all screen sizes
        int widePaddleWidth = (int)(250 * Math.min(scaleX, scaleY)); // Fixed width scaled uniformly (slightly wider)
        int paddleHeight = (int)(menuPaddleHeight * 1.8); // 80% taller than screen (increased for rotation)
        int paddleYOffset = (int)(-paddleHeight * 0.2); // Start further above screen
        
        // Save original transform
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        // Left paddle - rotated 25° counter-clockwise, use selected theme
        // Apply transition offset - moves left paddle to the left during transition
        int paddleExitOffset = (int)(paddleExitProgress * widePaddleWidth * 1.5);
        int leftCenterX = 0 - paddleExitOffset; // Moves left during transition
        int leftCenterY = paddleYOffset + paddleHeight / 2;
        
        g.translate(leftCenterX, leftCenterY);
        g.rotate(Math.toRadians(-25));
        
        // Use selected paddle theme in menu - same logic as in game
        if (selectedPaddleTheme >= 0 && selectedPaddleTheme < bluePaddleThemeImages.size()) {
            BufferedImage paddleImg = bluePaddleThemeImages.get(selectedPaddleTheme);
            
            if (paddleImg != null) {
                // Custom theme paddle with rounded corners
                int cornerRadius = Math.max(4, widePaddleWidth / 4);
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, cornerRadius, cornerRadius));
                
                // Apply whitish tint when selected
                if (leftPaddleSelected) {
                    // Create a lighter version of the image by applying a white overlay
                    g.drawImage(paddleImg, -widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, this);
                    g.setColor(new Color(255, 255, 255, 80)); // Semi-transparent white overlay
                    g.fillRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight);
                } else {
                    g.drawImage(paddleImg, -widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, this);
                }
                g.setClip(null);
            } else {
                // Default gradient paddle with selection colors
                Color leftColor, rightColor;
                if (leftPaddleSelected) {
                    // Whiter/lighter colors when selected
                    leftColor = new Color(180, 200, 255);   // Much lighter blue
                    rightColor = new Color(220, 230, 255);  // Very light blue
                } else {
                    // Normal colors
                    leftColor = new Color(100, 150, 255);
                    rightColor = new Color(150, 200, 255);
                }
                
                GradientPaint leftPaddleGradient = new GradientPaint(
                    -widePaddleWidth/2, -paddleHeight/2, leftColor,
                    widePaddleWidth/2, paddleHeight/2, rightColor);
                g.setPaint(leftPaddleGradient);
                g.fillRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight);
            }
        } else {
            // Fallback to default gradient with selection colors
            Color leftColor, rightColor;
            if (leftPaddleSelected) {
                // Whiter/lighter colors when selected
                leftColor = new Color(180, 200, 255);   // Much lighter blue
                rightColor = new Color(220, 230, 255);  // Very light blue
            } else {
                // Normal colors
                leftColor = new Color(100, 150, 255);
                rightColor = new Color(150, 200, 255);
            }
            
            GradientPaint leftPaddleGradient = new GradientPaint(
                -widePaddleWidth/2, -paddleHeight/2, leftColor,
                widePaddleWidth/2, paddleHeight/2, rightColor);
            g.setPaint(leftPaddleGradient);
            g.fillRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight);
        }
        
        // Define glow width for use in both paddles
        int glowWidth = Math.max(2, (int)(8 * Math.min(scaleX, scaleY)));
        
        // Left paddle normal side glow (no special selection glow above)
        g.setColor(getPaddleGlowColor(true));
        g.fillRect(widePaddleWidth/2, -paddleHeight/2, glowWidth, paddleHeight);
        
        // Reset transform
        g.setTransform(originalTransform);
        
        // Right paddle - rotated 25° clockwise, rosso gradiente
        // Apply transition offset - moves right paddle to the right during transition
        int rightCenterX = BOARD_WIDTH + paddleExitOffset; // Moves right during transition
        int rightCenterY = paddleYOffset + paddleHeight / 2;
        
        g.translate(rightCenterX, rightCenterY);
        g.rotate(Math.toRadians(25));
        
        // Draw right paddle with selected theme or default gradient
        if (selectedRightPaddleTheme >= 0 && selectedRightPaddleTheme < redPaddleThemeImages.size()) {
            BufferedImage rightPaddleImg = redPaddleThemeImages.get(selectedRightPaddleTheme);
            if (rightPaddleImg != null) {
                // Custom theme paddle with rounded corners
                int cornerRadius = Math.max(4, widePaddleWidth / 4);
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, cornerRadius, cornerRadius));
                
                // Apply whitish tint when selected (same as left paddle)
                if (rightPaddleSelected) {
                    // Create a lighter version of the image by applying a white overlay
                    g.drawImage(rightPaddleImg, -widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, this);
                    g.setColor(new Color(255, 255, 255, 80)); // Semi-transparent white overlay
                    g.fillRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight);
                } else {
                    g.drawImage(rightPaddleImg, -widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, this);
                }
                g.setClip(null);
            } else {
                // Fallback to gradient with selection colors
                drawRightPaddleGradient(g, widePaddleWidth, paddleHeight);
            }
        } else {
            // Default gradient with selection colors
            drawRightPaddleGradient(g, widePaddleWidth, paddleHeight);
        }
        
        // Right paddle border glow
        g.setColor(getPaddleGlowColor(false));
        g.fillRect(-widePaddleWidth/2 - glowWidth, -paddleHeight/2, glowWidth, paddleHeight);
        
        // Reset transform
        g.setTransform(originalTransform);
        
        // Finish drawing center line and other elements
        finishMenuPaddleDrawing(g);
    }
    
    private void drawRightPaddleGradient(Graphics2D g, int widePaddleWidth, int paddleHeight) {
        // Right paddle with selection colors
        Color rightLeftColor, rightRightColor;
        if (rightPaddleSelected) {
            // Whiter/lighter colors when selected
            rightLeftColor = new Color(255, 180, 180);   // Much lighter red
            rightRightColor = new Color(255, 220, 220);  // Very light red
        } else {
            // Normal colors
            rightLeftColor = new Color(255, 100, 100);
            rightRightColor = new Color(255, 150, 150);
        }
        
        GradientPaint rightPaddleGradient = new GradientPaint(
            -widePaddleWidth/2, -paddleHeight/2, rightLeftColor,
            widePaddleWidth/2, paddleHeight/2, rightRightColor);
        g.setPaint(rightPaddleGradient);
        g.fillRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight);
        g.setPaint(null); // Reset paint
    }
    
    private void finishMenuPaddleDrawing(Graphics2D g) {
        // Draw subtle dotted center line
        g.setColor(new Color(255, 255, 255, 50));
        int dotSpacing = (int)(30 * scaleY);
        int dotWidth = Math.max(1, (int)(2 * scaleX));
        int dotHeight = (int)(15 * scaleY);
        for (int i = 0; i < BOARD_HEIGHT; i += dotSpacing) {
            g.fillRect(BOARD_WIDTH / 2 - dotWidth/2, i, dotWidth, dotHeight);
        }
    }
    
    private void drawTransitioningMenuPaddles(Graphics2D g) {
        // Calculate base paddle dimensions
        int basePaddleWidth = (int)(250 * Math.min(scaleX, scaleY));
        int paddleHeight = (int)(menuPaddleHeight * 1.8);
        int paddleYOffset = (int)(-paddleHeight * 0.2);
        
        // Calculate paddle widths with dynamic expansion
        double leftWidthMultiplier = 1.0 + (0.3 * leftPaddleWidthProgress); // 1.0 to 1.3
        double rightWidthMultiplier = 1.0; // Right paddle doesn't expand during this transition
        
        int leftPaddleWidth = (int)(basePaddleWidth * leftWidthMultiplier);
        int rightPaddleWidth = (int)(basePaddleWidth * rightWidthMultiplier);
        
        // Save original transform
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        // Left paddle: stays in same position (no movement needed)
        int leftCenterX = 0;
        int leftCenterY = paddleYOffset + paddleHeight / 2;
        
        g.translate(leftCenterX, leftCenterY);
        g.rotate(Math.toRadians(-25));
        
        // Use selected paddle theme in transition - same logic as in drawMenuPaddles
        if (selectedPaddleTheme >= 0 && selectedPaddleTheme < bluePaddleThemeImages.size()) {
            BufferedImage paddleImg = bluePaddleThemeImages.get(selectedPaddleTheme);
            
            if (paddleImg != null) {
                // For themed paddle, transition from theme to black
                if (paddleTranslationProgress < 0.5) {
                    // First half: original theme image
                    g.drawImage(paddleImg, -leftPaddleWidth/2, -paddleHeight/2, leftPaddleWidth, paddleHeight, this);
                } else {
                    // Second half: gradually darken the image
                    double blackProgress = (paddleTranslationProgress - 0.5) * 2.0;
                    float alpha = (float)(1.0 - blackProgress);
                    g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));
                    g.drawImage(paddleImg, -leftPaddleWidth/2, -paddleHeight/2, leftPaddleWidth, paddleHeight, this);
                    g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1.0f));
                    
                    // Draw black overlay with increasing opacity
                    g.setColor(new Color(0, 0, 0, (int)(255 * blackProgress)));
                    g.fillRect(-leftPaddleWidth/2, -paddleHeight/2, leftPaddleWidth, paddleHeight);
                }
                
                // Draw glow for themed paddle
                int glowWidth = Math.max(2, (int)(8 * Math.min(scaleX, scaleY)));
                Color glowColor;
                if (paddleTranslationProgress < 0.5) {
                    glowColor = getPaddleGlowColor(true); // Theme-based glow
                } else {
                    double blackProgress = (paddleTranslationProgress - 0.5) * 2.0;
                    glowColor = interpolateToBlack(getPaddleGlowColor(true), blackProgress);
                }
                g.setColor(glowColor);
                g.fillRect(leftPaddleWidth/2, -paddleHeight/2, glowWidth, paddleHeight);
            } else {
                // Fallback to default colors if image fails
                drawDefaultLeftPaddleTransition(g, leftPaddleWidth, paddleHeight);
            }
        } else {
            // Default gradient colors when no theme selected
            drawDefaultLeftPaddleTransition(g, leftPaddleWidth, paddleHeight);
        }
        
        // Restore transform for right paddle
        g.setTransform(originalTransform);
        
        // Right paddle: moves from menu position to settings position
        int menuRightCenterX = BOARD_WIDTH;
        int settingsRightCenterX = getWidth() + rightPaddleWidth/8;
        int currentRightCenterX = (int)(menuRightCenterX + (settingsRightCenterX - menuRightCenterX) * paddleTranslationProgress);
        int rightCenterY = paddleYOffset + paddleHeight / 2;
        
        g.translate(currentRightCenterX, rightCenterY);
        
        // Right paddle rotation: from +25° to -25°
        double menuRotation = 25.0;
        double settingsRotation = -25.0;
        double currentRotation = menuRotation + (settingsRotation - menuRotation) * paddleTranslationProgress;
        g.rotate(Math.toRadians(currentRotation));
        
        // Use selected right paddle theme in transition
        if (selectedRightPaddleTheme >= 0 && selectedRightPaddleTheme < redPaddleThemeImages.size()) {
            BufferedImage rightPaddleImg = redPaddleThemeImages.get(selectedRightPaddleTheme);
            
            if (rightPaddleImg != null) {
                // For themed paddle, transition from theme to black
                if (paddleTranslationProgress < 0.5) {
                    // First half: original theme image
                    g.drawImage(rightPaddleImg, -rightPaddleWidth/2, -paddleHeight/2, rightPaddleWidth, paddleHeight, this);
                } else {
                    // Second half: gradually darken the image
                    double blackProgress = (paddleTranslationProgress - 0.5) * 2.0;
                    float alpha = (float)(1.0 - blackProgress);
                    g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));
                    g.drawImage(rightPaddleImg, -rightPaddleWidth/2, -paddleHeight/2, rightPaddleWidth, paddleHeight, this);
                    g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1.0f));
                    
                    // Draw black overlay with increasing opacity
                    g.setColor(new Color(0, 0, 0, (int)(255 * blackProgress)));
                    g.fillRect(-rightPaddleWidth/2, -paddleHeight/2, rightPaddleWidth, paddleHeight);
                }
                
                // Draw glow for themed paddle
                int glowWidth = Math.max(2, (int)(8 * Math.min(scaleX, scaleY)));
                Color rightGlowColor;
                if (paddleTranslationProgress < 0.5) {
                    rightGlowColor = getPaddleGlowColor(false); // Theme-based glow
                } else {
                    double blackProgress = (paddleTranslationProgress - 0.5) * 2.0;
                    rightGlowColor = interpolateToBlack(getPaddleGlowColor(false), blackProgress);
                }
                
                if (paddleTranslationProgress < 0.8) { // Only show glow during transition
                    g.setColor(rightGlowColor);
                    if (paddleTranslationProgress < 0.5) {
                        // During first half (still themed), glow on left side (menu style)
                        g.fillRect(-rightPaddleWidth/2 - glowWidth, -paddleHeight/2, glowWidth, paddleHeight);
                    } else {
                        // During second half (turning black), glow transitions to settings position
                        double glowTransition = (paddleTranslationProgress - 0.5) * 2.0;
                        int startGlowX = -rightPaddleWidth/2 - glowWidth; // Menu position (left side)
                        int endGlowX = -rightPaddleWidth/2 - glowWidth;   // Settings position (still left side due to rotation)
                        int currentGlowX = (int)(startGlowX + (endGlowX - startGlowX) * glowTransition);
                        g.fillRect(currentGlowX, -paddleHeight/2, glowWidth, paddleHeight);
                    }
                }
            } else {
                // Fallback to default colors if image fails
                drawDefaultRightPaddleTransition(g, rightPaddleWidth, paddleHeight);
            }
        } else {
            // Default gradient colors when no theme selected
            drawDefaultRightPaddleTransition(g, rightPaddleWidth, paddleHeight);
        }
        
        // Restore original transform
        g.setTransform(originalTransform);
        
        // Draw center line (fading out)
        if (paddleTranslationProgress < 0.8) {
            int alpha = (int)(255 * (1.0 - paddleTranslationProgress / 0.8));
            g.setColor(new Color(255, 255, 255, alpha));
            int dotSpacing = (int)(30 * scaleY);
            int dotWidth = Math.max(1, (int)(2 * scaleX));
            int dotHeight = (int)(15 * scaleY);
            for (int i = 0; i < BOARD_HEIGHT; i += dotSpacing) {
                g.fillRect(BOARD_WIDTH / 2 - dotWidth/2, i, dotWidth, dotHeight);
            }
        }
    }
    
    private void drawSettingsTitleAppearing(Graphics2D g, double progress) {
        // Settings title appears with fade in
        int alpha = (int)(255 * progress);
        g.setColor(new Color(255, 255, 255, alpha));
        
        float titleSize = (float)(36 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(titleSize));
        FontMetrics titleFm = g.getFontMetrics();
        String title = getText("MENU_SETTINGS");
        int titleX = (BOARD_WIDTH - titleFm.stringWidth(title)) / 2;
        int titleY = (int)(80 * scaleY);
        
        g.drawString(title, titleX, titleY);
    }
    
    private Color interpolateToBlack(Color original, double factor) {
        // Interpolate each color component towards 0 (black)
        int r = (int)(original.getRed() * (1.0 - factor));
        int g = (int)(original.getGreen() * (1.0 - factor));
        int b = (int)(original.getBlue() * (1.0 - factor));
        int a = original.getAlpha(); // Preserve alpha channel
        return new Color(r, g, b, a);
    }
    
    private void drawSettingsColumnsSlideIn(Graphics2D g, double progress) {
        // Save original transform
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        // Calculate translation distances for right column only
        // Right elements: start from off-screen right, move to final position (right side)
        int rightColumnStartX = (int)(BOARD_WIDTH * 1.2); // Start off-screen right
        int rightColumnEndX = 0; // Final position (no additional offset needed)
        
        // Apply easing for smoother animation
        double easedProgress = easeInOutQuad(progress);
        int rightColumnCurrentX = (int)(rightColumnStartX + (rightColumnEndX - rightColumnStartX) * easedProgress);
        
        // Draw left column (categories) in final position (no translation)
        drawCategoryColumn(g, originalTransform);
        
        // Draw right column (settings) sliding in from right
        g.translate(rightColumnCurrentX, 0);
        drawSettingColumn(g, originalTransform);
        g.setTransform(originalTransform);
    }
    
    
    private void drawMenuBall(Graphics2D g) {
        // Draw menu ball with glow effect
        int glowSize = (int)(10 * Math.min(scaleX, scaleY));
        
        // Outer glow
        g.setColor(new Color(255, 255, 255, 30));
        g.fillOval((int)menuBallX - glowSize, (int)menuBallY - glowSize, 
                   menuBallSize + glowSize*2, menuBallSize + glowSize*2);
        
        // Inner glow
        g.setColor(new Color(255, 255, 255, 60));
        g.fillOval((int)menuBallX - glowSize/2, (int)menuBallY - glowSize/2, 
                   menuBallSize + glowSize, menuBallSize + glowSize);
        
        // Main ball
        g.setColor(Color.WHITE);
        g.fillOval((int)menuBallX, (int)menuBallY, menuBallSize, menuBallSize);
        
        // Add subtle highlight
        g.setColor(new Color(255, 255, 255, 200));
        int highlightSize = menuBallSize / 3;
        g.fillOval((int)menuBallX + highlightSize/2, (int)menuBallY + highlightSize/2, 
                   highlightSize, highlightSize);
        
        // Draw second ball if active - identical to first ball
        if (menuBall2Active) {
            // Outer glow
            g.setColor(new Color(255, 255, 255, 30));
            g.fillOval((int)menuBall2X - glowSize, (int)menuBall2Y - glowSize, 
                       menuBallSize + glowSize*2, menuBallSize + glowSize*2);
            
            // Inner glow
            g.setColor(new Color(255, 255, 255, 60));
            g.fillOval((int)menuBall2X - glowSize/2, (int)menuBall2Y - glowSize/2, 
                       menuBallSize + glowSize, menuBallSize + glowSize);
            
            // Main ball
            g.setColor(Color.WHITE);
            g.fillOval((int)menuBall2X, (int)menuBall2Y, menuBallSize, menuBallSize);
            
            // Add subtle highlight
            g.setColor(new Color(255, 255, 255, 200));
            g.fillOval((int)menuBall2X + highlightSize/2, (int)menuBall2Y + highlightSize/2, 
                       highlightSize, highlightSize);
        }
    }
    
    private void drawTransition(Graphics2D g) {
        // Check if this is a pause transition
        if (isTransitioningToPause || isTransitioningFromPause) {
            drawPauseTransition(g);
            return;
        }
        
        // Check if this is a rank-to-home transition
        if (isRankToHomeTransition) {
            drawRankToHomeTransition(g);
            return;
        }
        
        // Check if this is a paddle-to-home transition
        if (isPaddleToHomeTransition) {
            drawPaddleToHomeTransition(g);
            return;
        }
        
        // Draw selected background first during transition
        drawGameBackground(g);
        
        // Interpolate paddle positions and sizes during transition
        double easeProgress = easeInOutQuad(transitionProgress);
        
        // Calculate scaled positions for game paddles
        int leftPaddleX = (int)(20 * scaleX);
        int rightPaddleX = BOARD_WIDTH - leftPaddleX - PADDLE_WIDTH;
        
        // Menu paddle properties (matching drawMenuPaddles exactly)
        int widePaddleWidth = (int)(250 * Math.min(scaleX, scaleY));
        int tallPaddleHeight = (int)(menuPaddleHeight * 1.8);
        int paddleYOffset = (int)(-tallPaddleHeight * 0.2);
        
        // Starting positions in menu (exact centers from drawMenuPaddles)
        double menuLeftCenterX = 0; // Left paddle center is at left edge
        double menuRightCenterX = BOARD_WIDTH; // Right paddle center is at right edge  
        double menuCenterY = paddleYOffset + tallPaddleHeight / 2;
        
        // Target positions in game (centers)
        double gameLeftCenterX = leftPaddleX + PADDLE_WIDTH / 2;
        double gameRightCenterX = rightPaddleX + PADDLE_WIDTH / 2;
        double gameCenterY = paddle1Y + PADDLE_HEIGHT / 2;
        
        // Interpolate center positions
        double currentLeftCenterX = menuLeftCenterX + (gameLeftCenterX - menuLeftCenterX) * easeProgress;
        double currentRightCenterX = menuRightCenterX + (gameRightCenterX - menuRightCenterX) * easeProgress;
        double currentCenterY = menuCenterY + (gameCenterY - menuCenterY) * easeProgress;
        
        // Convert centers to top-left positions for rendering
        double currentPaddle1X = currentLeftCenterX - (widePaddleWidth + (PADDLE_WIDTH - widePaddleWidth) * easeProgress) / 2;
        double currentPaddle2X = currentRightCenterX - (widePaddleWidth + (PADDLE_WIDTH - widePaddleWidth) * easeProgress) / 2;
        double currentPaddle1Y = currentCenterY - (tallPaddleHeight + (PADDLE_HEIGHT - tallPaddleHeight) * easeProgress) / 2;
        double currentPaddle2Y = currentCenterY - (tallPaddleHeight + (PADDLE_HEIGHT - tallPaddleHeight) * easeProgress) / 2;
        
        // Interpolate dimensions
        double currentPaddleWidth = widePaddleWidth + (PADDLE_WIDTH - widePaddleWidth) * easeProgress;
        double currentPaddleHeight = tallPaddleHeight + (PADDLE_HEIGHT - tallPaddleHeight) * easeProgress;
        
        // Draw particles first (behind everything)
        for (Particle p : particles) {
            p.draw(g);
        }
        
        // Draw transitioning paddles with morphing from tilted wide to straight narrow
        drawTransitioningPaddles(g, easeProgress, currentPaddle1X, currentPaddle1Y, 
                                currentPaddle2X, currentPaddle2Y, currentPaddleWidth, currentPaddleHeight);
        
        // Animate menu ball transitioning to game ball
        drawTransitioningBall(g, easeProgress);
        
        // Shared element: Center line transforms from menu dots to game dashes
        drawCenterLineTransition(g, easeProgress);
        
        // Shared element: Title transforms into scores
        drawTitleToScoreTransition(g, easeProgress);
        
        // Shared element: Instructions text transition
        drawInstructionsTransition(g, easeProgress);
        
        // Shared element: Menu items fade out
        drawMenuItemsFadeOut(g, easeProgress);
    }
    
    private void drawPauseTransition(Graphics2D g) {
        // Draw game background
        drawGameBackground(g);
        
        // Draw particles
        ArrayList<Particle> particlesCopy = new ArrayList<>(particles);
        for (Particle p : particlesCopy) {
            p.draw(g);
        }
        
        // Draw paddles (they stay the same)
        drawTransitionGamePaddles(g);
        
        // Draw ball (stays the same)
        drawTransitionBall(g);
        
        // Draw animated center line (rotating from vertical to diagonal)
        drawTransitionCenterLine(g);
        
        // Draw animated scores (translating to pause positions)
        drawTransitionScores(g);
        
        // Draw pause overlay with appropriate opacity based on direction
        drawTransitionPauseOverlay(g);
    }
    
    private void drawTransitionGamePaddles(Graphics2D g) {
        int leftPaddleX = (int)(20 * scaleX);
        int rightPaddleX = BOARD_WIDTH - leftPaddleX - PADDLE_WIDTH;
        int cornerRadius = Math.max(4, PADDLE_WIDTH / 4);
        
        // Draw left paddle (blue/theme)
        if (selectedPaddleTheme >= 0 && selectedPaddleTheme < bluePaddleThemeImages.size()) {
            BufferedImage paddleImg = bluePaddleThemeImages.get(selectedPaddleTheme);
            if (paddleImg != null) {
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(leftPaddleX, paddle1Y, PADDLE_WIDTH, PADDLE_HEIGHT, cornerRadius, cornerRadius));
                g.drawImage(paddleImg, leftPaddleX, paddle1Y, PADDLE_WIDTH, PADDLE_HEIGHT, this);
                g.setClip(null);
            } else {
                // Default gradient paddle
                GradientPaint paddle1Gradient = new GradientPaint(
                    leftPaddleX, paddle1Y, Color.BLUE,
                    leftPaddleX + PADDLE_WIDTH, paddle1Y + PADDLE_HEIGHT, Color.CYAN
                );
                g.setPaint(paddle1Gradient);
                g.fillRoundRect(leftPaddleX, paddle1Y, PADDLE_WIDTH, PADDLE_HEIGHT, cornerRadius, cornerRadius);
            }
        } else {
            // Default gradient
            GradientPaint paddle1Gradient = new GradientPaint(
                leftPaddleX, paddle1Y, Color.BLUE,
                leftPaddleX + PADDLE_WIDTH, paddle1Y + PADDLE_HEIGHT, Color.CYAN
            );
            g.setPaint(paddle1Gradient);
            g.fillRoundRect(leftPaddleX, paddle1Y, PADDLE_WIDTH, PADDLE_HEIGHT, cornerRadius, cornerRadius);
        }
        
        // Draw right paddle (red/theme)
        if (selectedRightPaddleTheme >= 0 && selectedRightPaddleTheme < redPaddleThemeImages.size()) {
            BufferedImage paddleImg = redPaddleThemeImages.get(selectedRightPaddleTheme);
            if (paddleImg != null) {
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(rightPaddleX, paddle2Y, PADDLE_WIDTH, PADDLE_HEIGHT, cornerRadius, cornerRadius));
                g.drawImage(paddleImg, rightPaddleX, paddle2Y, PADDLE_WIDTH, PADDLE_HEIGHT, this);
                g.setClip(null);
            } else {
                // Default gradient paddle
                GradientPaint paddle2Gradient = new GradientPaint(
                    rightPaddleX, paddle2Y, Color.RED,
                    rightPaddleX + PADDLE_WIDTH, paddle2Y + PADDLE_HEIGHT, Color.ORANGE
                );
                g.setPaint(paddle2Gradient);
                g.fillRoundRect(rightPaddleX, paddle2Y, PADDLE_WIDTH, PADDLE_HEIGHT, cornerRadius, cornerRadius);
            }
        } else {
            // Default gradient
            GradientPaint paddle2Gradient = new GradientPaint(
                rightPaddleX, paddle2Y, Color.RED,
                rightPaddleX + PADDLE_WIDTH, paddle2Y + PADDLE_HEIGHT, Color.ORANGE
            );
            g.setPaint(paddle2Gradient);
            g.fillRoundRect(rightPaddleX, paddle2Y, PADDLE_WIDTH, PADDLE_HEIGHT, cornerRadius, cornerRadius);
        }
        
        g.setPaint(null); // Reset paint
    }
    
    private void drawTransitionBall(Graphics2D g) {
        // Draw the ball with glow effect
        int glowSize = 8;
        RadialGradientPaint ballGradient = new RadialGradientPaint(
            (float)ballX + BALL_SIZE/2f, (float)ballY + BALL_SIZE/2f, 
            BALL_SIZE/2f + glowSize,
            new float[]{0f, 0.7f, 1f},
            new Color[]{Color.WHITE, new Color(255, 255, 255, 200), new Color(255, 255, 255, 0)}
        );
        g.setPaint(ballGradient);
        g.fillOval((int)ballX - glowSize, (int)ballY - glowSize, 
                  BALL_SIZE + 2*glowSize, BALL_SIZE + 2*glowSize);
        
        // Draw ball core
        g.setColor(Color.WHITE);
        g.fillOval((int)ballX, (int)ballY, BALL_SIZE, BALL_SIZE);
        g.setPaint(null);
    }
    
    private void drawTransitionCenterLine(Graphics2D g) {
        g.setColor(new Color(255, 255, 255, 150));
        
        // Calculate line properties - same as pause and game
        int lineSpacing = (int)(20 * scaleY);
        int lineWidth = Math.max(2, (int)(4 * scaleX));
        int lineHeight = (int)(10 * scaleY);
        
        // Simple and smooth transition: rotate around screen center
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        // Rotate around screen center
        g.translate(BOARD_WIDTH / 2, BOARD_HEIGHT / 2);
        g.rotate(centerLineRotation);
        
        // Draw line extending through the entire screen
        double halfLength = Math.max(BOARD_WIDTH, BOARD_HEIGHT);
        for (double distance = -halfLength; distance < halfLength; distance += lineSpacing) {
            g.fillRect(-lineWidth/2, (int)distance, lineWidth, lineHeight);
        }
        
        g.setTransform(originalTransform);
    }
    
    private void drawTransitionScores(Graphics2D g) {
        // Calculate font size - starts at game size, goes to pause size
        float gameSize = (float)(48 * Math.min(scaleX, scaleY));
        float pauseSize = (float)(96 * Math.min(scaleX, scaleY)); // Same as pause overlay
        float currentSize = gameSize + (pauseSize - gameSize) * (float)scoreTranslationProgress;
        
        g.setFont(primaryFont.deriveFont(Font.BOLD, currentSize));
        FontMetrics fm = g.getFontMetrics();
        
        // Original game positions
        int gameLeftScoreX = BOARD_WIDTH / 4 - fm.stringWidth(String.valueOf(score1)) / 2;
        int gameRightScoreX = 3 * BOARD_WIDTH / 4 - fm.stringWidth(String.valueOf(score2)) / 2;
        int gameScoreY = (int)(80 * scaleY);
        
        // EXACT pause positions from drawPauseOverlay
        int pauseLeftScoreX = (int)(40 * scaleX);
        int pauseLeftScoreY = (int)(120 * scaleY);
        
        // For right score, calculate width first
        int rightScoreWidth = fm.stringWidth(String.valueOf(score2));
        int pauseRightScoreX = BOARD_WIDTH - rightScoreWidth - (int)(40 * scaleX);
        int pauseRightScoreY = BOARD_HEIGHT - (int)(80 * scaleY);
        
        // Interpolate positions
        int leftScoreX = (int)(gameLeftScoreX + (pauseLeftScoreX - gameLeftScoreX) * scoreTranslationProgress);
        int leftScoreY = (int)(gameScoreY + (pauseLeftScoreY - gameScoreY) * scoreTranslationProgress);
        int rightScoreX = (int)(gameRightScoreX + (pauseRightScoreX - gameRightScoreX) * scoreTranslationProgress);
        int rightScoreY = (int)(gameScoreY + (pauseRightScoreY - gameScoreY) * scoreTranslationProgress);
        
        // Interpolate colors - game white to pause colors
        int leftRed = (int)(255 + (150 - 255) * scoreTranslationProgress);   // 255->150
        int leftGreen = (int)(255 + (200 - 255) * scoreTranslationProgress); // 255->200  
        int leftBlue = (int)(255 + (255 - 255) * scoreTranslationProgress);  // 255->255
        
        int rightRed = (int)(255 + (255 - 255) * scoreTranslationProgress);   // 255->255
        int rightGreen = (int)(255 + (150 - 255) * scoreTranslationProgress); // 255->150
        int rightBlue = (int)(255 + (150 - 255) * scoreTranslationProgress);  // 255->150
        
        // Draw left score with shadow
        int shadowOffset = Math.max(1, (int)(3 * Math.min(scaleX, scaleY)));
        g.setColor(new Color(0, 0, 0, 120));
        g.drawString(String.valueOf(score1), leftScoreX + shadowOffset, leftScoreY + shadowOffset);
        
        g.setColor(new Color(leftRed, leftGreen, leftBlue));
        g.drawString(String.valueOf(score1), leftScoreX, leftScoreY);
        
        // Draw right score with shadow
        g.setColor(new Color(0, 0, 0, 120));
        g.drawString(String.valueOf(score2), rightScoreX + shadowOffset, rightScoreY + shadowOffset);
        
        g.setColor(new Color(rightRed, rightGreen, rightBlue));
        g.drawString(String.valueOf(score2), rightScoreX, rightScoreY);
    }
    
    private void drawTransitionPauseOverlay(Graphics2D g) {
        // Calculate overlay alpha based on transition direction
        int overlayAlpha;
        if (isTransitioningToPause) {
            // Going to pause: increase opacity
            overlayAlpha = (int)(180 * pauseTransitionProgress);
        } else {
            // Going back to game: decrease opacity
            overlayAlpha = (int)(180 * pauseTransitionProgress);
        }
        
        // Draw overlay background
        g.setColor(new Color(0, 0, 0, overlayAlpha));
        g.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
        
        // Only draw pause elements if we're transitioning TO pause or if we're still mostly in pause
        if (isTransitioningToPause || (isTransitioningFromPause && pauseTransitionProgress > 0.5)) {
            drawPauseElements(g);
        }
    }
    
    private void drawPauseElements(Graphics2D g) {
        // Draw pause title and any other static pause elements that should appear during transition
        // For now, we don't draw anything extra since scores and line are handled separately
        
        // Title "PAUSA" (only show during pause transition, not resume)
        if (isTransitioningToPause && pauseTransitionProgress > 0.7) {
            g.setColor(Color.WHITE);
            float titleSize = (float)(56 * Math.min(scaleX, scaleY));
            g.setFont(primaryFont.deriveFont(Font.BOLD, titleSize));
            FontMetrics titleFm = g.getFontMetrics();
            String pauseTitle = getText("GAME_PAUSE");
            int titleX = (BOARD_WIDTH - titleFm.stringWidth(pauseTitle)) / 2;
            int titleY = (int)(80 * scaleY);
            
            // Calculate alpha for title
            double titleAlpha = (pauseTransitionProgress - 0.7) / 0.3; // Fade in during last 30%
            int alpha = (int)(255 * titleAlpha);
            g.setColor(new Color(255, 255, 255, alpha));
            g.drawString(pauseTitle, titleX, titleY);
        }
    }
    
    private void drawRankToHomeTransition(Graphics2D g) {
        // Calculate eased progress for smooth animation
        double easedProgress = easeInOutQuad(rankToHomeProgress);
        
        // Draw home background (destination)
        drawHomeBackground(g);
        
        // Calculate slide offset (elements slide to the right and exit)
        int slideOffsetX = (int)(BOARD_WIDTH * easedProgress);
        
        // Draw rank elements sliding out to the right
        drawSlidingRankElements(g, slideOffsetX);
        
        // Draw home paddle sliding in from left
        drawTransitioningHomePaddle(g, easedProgress);
    }
    
    private void drawSlidingRankElements(Graphics2D g, int offsetX) {
        // Save original transform
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        // Translate everything to the right by offsetX
        g.translate(offsetX, 0);
        
        // Draw rank screen elements (they will appear to slide right)
        drawRankScreenContent(g);
        
        // Restore transform
        g.setTransform(originalTransform);
    }
    
    private void drawRankScreenContent(Graphics2D g) {
        // Draw rank screen content without background - use the same elements as drawRankScreen
        
        // Use current animation progress to maintain consistency
        float animProgress = Math.min(rankAnimationFrame / 90.0f, 1.0f);
        
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        
        // Draw rank spostato più a destra del centro (same as drawRankScreen)
        int rankX = centerX + (int)(150 * Math.min(scaleX, scaleY)); // Spostato a destra
        drawRankDisplay(g, rankX, centerY, animProgress);
        
        // Draw scrolling game info behind paddle in top-right (same as drawRankScreen)
        drawScrollingGameInfo(g, animProgress);
        
        // Draw winner paddle in same position as left paddle in home screen (same as drawRankScreen)
        drawWinnerPaddle(g, animProgress);
    }
    
    private void drawTransitioningHomePaddle(Graphics2D g, double progress) {
        // Home paddle slides in from the left as rank elements slide out to the right
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        // Calculate paddle dimensions (exact same as menu)
        int widePaddleWidth = (int)(250 * Math.min(scaleX, scaleY));
        int paddleHeight = (int)(menuPaddleHeight * 1.8);
        int paddleYOffset = (int)(-paddleHeight * 0.2);
        
        // Calculate final position (exact same as menu left paddle)
        int finalCenterX = 0; // Completely attached to left edge (same as menu)
        int finalCenterY = paddleYOffset + paddleHeight / 2;
        
        // Start position (off-screen to the left)
        int startCenterX = -widePaddleWidth - 50; // Start completely off-screen
        int startCenterY = finalCenterY; // Same Y as final position
        
        // Interpolate position
        int currentCenterX = (int)(startCenterX + (finalCenterX - startCenterX) * progress);
        int currentCenterY = (int)(startCenterY + (finalCenterY - startCenterY) * progress);
        
        // Position and rotate paddle like in menu
        g.translate(currentCenterX, currentCenterY);
        g.rotate(Math.toRadians(-25)); // Same rotation as menu left paddle
        
        // Draw paddle with selected theme or default gradient
        if (selectedPaddleTheme >= 0 && selectedPaddleTheme < bluePaddleThemeImages.size()) {
            BufferedImage paddleImg = bluePaddleThemeImages.get(selectedPaddleTheme);
            
            if (paddleImg != null) {
                // Custom theme paddle
                g.drawImage(paddleImg, -widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, null);
            } else {
                // Default gradient
                GradientPaint paddleGradient = new GradientPaint(
                    -widePaddleWidth/2, -paddleHeight/2, new Color(100, 150, 255),
                    widePaddleWidth/2, paddleHeight/2, new Color(150, 200, 255)
                );
                g.setPaint(paddleGradient);
                g.fillRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight);
                g.setPaint(null);
            }
        } else {
            // Default gradient
            GradientPaint paddleGradient = new GradientPaint(
                -widePaddleWidth/2, -paddleHeight/2, new Color(100, 150, 255),
                widePaddleWidth/2, paddleHeight/2, new Color(150, 200, 255)
            );
            g.setPaint(paddleGradient);
            g.fillRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight);
            g.setPaint(null);
        }
        
        // Add glow effect (same as menu)
        int glowWidth = Math.max(2, (int)(8 * Math.min(scaleX, scaleY)));
        g.setColor(new Color(100, 150, 255, 100)); // Blue glow
        g.fillRect(widePaddleWidth/2, -paddleHeight/2, glowWidth, paddleHeight);
        
        g.setTransform(originalTransform);
    }
    
    private void drawPaddleToHomeTransition(Graphics2D g) {
        // This is the INVERSE of drawHomeToPaddleTransition
        
        // Draw the background
        drawMenuBackground(g);
        
        // Calculate inverse phases like in updatePaddleToHomeTransition but in reverse order
        double inverseTextFadeProgress;
        double inversePanelProgress;
        
        // Phase 1: Panel slides out (first 70% of transition)
        if (paddleToHomeProgress <= 0.7) {
            inversePanelProgress = paddleToHomeProgress / 0.7; // Va da 0.0 a 1.0 durante i primi 70%
        } else {
            inversePanelProgress = 1.0; // Completamente fuori dopo il 70%
        }
        
        // Phase 2: Text fades in (last 30% of transition - inverse of first 30%)
        if (paddleToHomeProgress > 0.7) {
            inverseTextFadeProgress = (paddleToHomeProgress - 0.7) / 0.3;
        } else {
            inverseTextFadeProgress = 0.0;
        }
        
        // Draw the selected paddle in its final menu position (doesn't move during transition)
        if (isLeftPaddleTransition) {
            drawPaddleTransitionLeft(g);
        } else {
            drawPaddleTransitionRight(g);
        }
        
        // Draw the themes panel sliding out to the side
        if (inversePanelProgress > 0) {
            if (isLeftPaddleTransition) {
                drawLeftPaddleThemesPanelSlideOut(g, inversePanelProgress);
            } else {
                drawRightPaddleThemesPanelSlideOut(g, inversePanelProgress);
            }
        }
        
        // Draw menu text fading in
        if (inverseTextFadeProgress > 0) {
            drawMenuTextWithFade(g, inverseTextFadeProgress);
        }
    }
    
    private void drawSlidingPaddleElements(Graphics2D g, int offsetX) {
        // Save original transform
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        // Translate everything to the right by offsetX
        g.translate(offsetX, 0);
        
        // Draw paddle selection elements (they will appear to slide right)
        drawPaddleSelectionContent(g);
        
        // Restore transform
        g.setTransform(originalTransform);
    }
    
    private void drawPaddleSelectionContent(Graphics2D g) {
        // Draw paddle selection content without background - use same elements as drawPaddleSelection
        
        // Title
        g.setColor(Color.WHITE);
        float titleSize = (float)(48 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(Font.BOLD, titleSize));
        FontMetrics titleFm = g.getFontMetrics();
        String title = "Paddle Selezione";
        int titleX = (getWidth() - titleFm.stringWidth(title)) / 2;
        int titleY = (int)(80 * scaleY);
        
        // Shadow
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(title, titleX + 3, titleY + 3);
        
        // Main text
        g.setColor(Color.WHITE);
        g.drawString(title, titleX, titleY);
        
        // Draw paddle options (simplified version from drawPaddleSelection)
        int paddleY = (int)(200 * scaleY);
        int paddleSpacing = (int)(120 * scaleX);
        int startX = (getWidth() - (paddleSpacing * (bluePaddleThemeImages.size() - 1))) / 2;
        
        for (int i = 0; i < bluePaddleThemeImages.size(); i++) {
            int paddleX = startX + i * paddleSpacing;
            boolean isSelected = (i == selectedPaddleTheme);
            
            // Draw paddle preview
            if (i < bluePaddleThemeImages.size()) {
                BufferedImage paddleImg = bluePaddleThemeImages.get(i);
                if (paddleImg != null) {
                    int previewWidth = (int)(40 * Math.min(scaleX, scaleY));
                    int previewHeight = (int)(80 * Math.min(scaleX, scaleY));
                    
                    // Selection highlight
                    if (isSelected) {
                        g.setColor(new Color(100, 150, 255, 100));
                        g.fillRect(paddleX - previewWidth/2 - 5, paddleY - previewHeight/2 - 5, 
                                 previewWidth + 10, previewHeight + 10);
                    }
                    
                    g.drawImage(paddleImg, paddleX - previewWidth/2, paddleY - previewHeight/2, 
                              previewWidth, previewHeight, null);
                }
            }
        }
        
        // Instructions
        g.setColor(new Color(200, 200, 200));
        float instructSize = (float)(24 * Math.min(scaleX, scaleY));
        g.setFont(secondaryFont.deriveFont(instructSize));
        FontMetrics instructFm = g.getFontMetrics();
        
        String instruct = "Use arrows to select, ENTER to confirm, ESC to return";
        int instructX = (getWidth() - instructFm.stringWidth(instruct)) / 2;
        int instructY = getHeight() - (int)(80 * scaleY);
        
        g.drawString(instruct, instructX, instructY);
    }
    
    private void drawHomeBackground(Graphics2D g) {
        // Draw the same background as menu
        if (selectedBackground >= 0 && selectedBackground < backgroundImages.size()) {
            Image backgroundImg = backgroundImages.get(selectedBackground);
            if (backgroundImg != null) {
                g.drawImage(backgroundImg, 0, 0, getWidth(), getHeight(), this);
            } else {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        } else {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
    
    private double easeInOutQuad(double t) {
        return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    }
    
    private void drawTitleToScoreTransition(Graphics2D g, double progress) {
        // Shared element animation: Title morphs into scores
        double easeProgress = easeInOutQuad(progress);
        
        // Calculate positions and sizes
        int titleY = (int)(150 * scaleY);
        int scoreY = (int)(60 * scaleY);
        float titleSize = (float)(48 * Math.min(scaleX, scaleY));
        float scoreSize = (float)(40 * Math.min(scaleX, scaleY));
        
        // Interpolate position and size
        int currentY = (int)(titleY + (scoreY - titleY) * easeProgress);
        float currentSize = titleSize + (scoreSize - titleSize) * (float)easeProgress;
        
        g.setFont(primaryFont.deriveFont(currentSize));
        FontMetrics fm = g.getFontMetrics();
        
        if (progress < 0.3) {
            // Phase 1: Show full title "PONG PING"
            g.setColor(Color.WHITE);
            String title = "PONG PING";
            int titleX = (BOARD_WIDTH - fm.stringWidth(title)) / 2;
            
            // Glow effect that fades out
            int glowOffset = Math.max(1, (int)(3 * Math.min(scaleX, scaleY) * (1 - progress / 0.3)));
            g.setColor(new Color(0, 255, 255, (int)(50 * (1 - progress / 0.3))));
            for (int i = 1; i <= glowOffset; i++) {
                g.drawString(title, titleX - i, currentY - i);
                g.drawString(title, titleX + i, currentY + i);
            }
            g.setColor(Color.WHITE);
            g.drawString(title, titleX, currentY);
            
        } else if (progress < 0.7) {
            // Phase 2: Title splits and moves apart
            double splitProgress = (progress - 0.3) / 0.4;
            double splitEase = easeInOutQuad(splitProgress);
            
            g.setColor(new Color(255, 255, 255, (int)(255 * (1 - splitProgress * 0.3))));
            
            // "PONG" moves to left score position
            String leftPart = "PONG";
            int leftFinalX = BOARD_WIDTH / 4 - fm.stringWidth("0") / 2;
            int leftStartX = BOARD_WIDTH / 2 - fm.stringWidth("PONG PING") / 4;
            int leftCurrentX = (int)(leftStartX + (leftFinalX - leftStartX) * splitEase);
            g.drawString(leftPart, leftCurrentX - fm.stringWidth(leftPart) / 2, currentY);
            
            // "PING" moves to right score position
            String rightPart = "PING";
            int rightFinalX = 3 * BOARD_WIDTH / 4 - fm.stringWidth("0") / 2;
            int rightStartX = BOARD_WIDTH / 2 + fm.stringWidth("PONG PING") / 4;
            int rightCurrentX = (int)(rightStartX + (rightFinalX - rightStartX) * splitEase);
            g.drawString(rightPart, rightCurrentX - fm.stringWidth(rightPart) / 2, currentY);
            
        } else {
            // Phase 3: Transform into actual scores
            double scoreProgress = (progress - 0.7) / 0.3;
            double scoreEase = easeInOutQuad(scoreProgress);
            
            // Calculate shadow offset
            int shadowOffset = Math.max(1, (int)(2 * Math.min(scaleX, scaleY) * scoreEase));
            
            // Draw shadows
            g.setColor(new Color(0, 0, 0, (int)(100 * scoreEase)));
            g.drawString(String.valueOf(score1), BOARD_WIDTH / 4 - fm.stringWidth(String.valueOf(score1)) / 2 + shadowOffset, currentY + shadowOffset);
            g.drawString(String.valueOf(score2), 3 * BOARD_WIDTH / 4 - fm.stringWidth(String.valueOf(score2)) / 2 + shadowOffset, currentY + shadowOffset);
            
            // Draw scores
            g.setColor(new Color(255, 255, 255, (int)(255 * scoreEase)));
            g.drawString(String.valueOf(score1), BOARD_WIDTH / 4 - fm.stringWidth(String.valueOf(score1)) / 2, currentY);
            g.drawString(String.valueOf(score2), 3 * BOARD_WIDTH / 4 - fm.stringWidth(String.valueOf(score2)) / 2, currentY);
            
            // Fade out the text parts if still visible
            if (scoreProgress < 0.5) {
                g.setColor(new Color(255, 255, 255, (int)(255 * (1 - scoreProgress * 2))));
                g.drawString("0", BOARD_WIDTH / 4 - fm.stringWidth("0") / 2, currentY);
                g.drawString("0", 3 * BOARD_WIDTH / 4 - fm.stringWidth("0") / 2, currentY);
            }
        }
    }
    
    private void drawInstructionsTransition(Graphics2D g, double progress) {
        // Empty method - instructions removed as requested
    }
    
    private void drawMenuItemsFadeOut(Graphics2D g, double progress) {
        // Shared element: Menu items scale down and fade out
        if (progress > 0.8) return; // Complete fade out after 80%
        
        double fadeProgress = Math.min(progress / 0.8, 1.0);
        double fadeEase = easeInOutQuad(fadeProgress);
        
        // Calculate scaled properties
        float menuSize = (float)(24 * Math.min(scaleX, scaleY) * (1 - fadeEase * 0.3));
        int alpha = (int)(255 * (1 - fadeEase));
        int menuStartY = (int)(300 * scaleY);
        int menuSpacing = (int)(60 * scaleY);
        
        g.setFont(primaryFont.deriveFont(menuSize));
        FontMetrics menuFm = g.getFontMetrics();
        
        // Add slight scale and rotation effect
        double scale = 1.0 - fadeEase * 0.2;
        int offsetY = (int)(fadeEase * 20 * scaleY); // Items move slightly down as they fade
        
        for (int i = 0; i < menuItems.length; i++) {
            int itemY = (int)((menuStartY + i * menuSpacing + offsetY) * scale);
            
            if (i == selectedMenuItem) {
                // Selected item has a different fade effect
                g.setColor(new Color(0, 255, 255, alpha / 2)); // Cyan fading faster
                String selectedText = "> " + menuItems[i] + " <";
                g.drawString(selectedText, 
                    (BOARD_WIDTH - menuFm.stringWidth(selectedText)) / 2, 
                    itemY);
            } else {
                g.setColor(new Color(255, 255, 255, alpha));
                g.drawString(menuItems[i], 
                    (BOARD_WIDTH - menuFm.stringWidth(menuItems[i])) / 2, 
                    itemY);
            }
        }
    }
    
    private void drawCenterLineTransition(Graphics2D g, double progress) {
        // Shared element: Center line morphs from menu style to game style
        double easeProgress = easeInOutQuad(progress);
        
        // Menu style: subtle dots
        int menuDotSpacing = (int)(30 * scaleY);
        int menuDotWidth = Math.max(1, (int)(2 * scaleX));
        int menuDotHeight = (int)(15 * scaleY);
        
        // Game style: dashed line
        int gameLineSpacing = (int)(20 * scaleY);
        int gameLineWidth = Math.max(1, (int)(4 * scaleX));
        int gameLineHeight = (int)(10 * scaleY);
        
        // Interpolate properties
        int currentSpacing = (int)(menuDotSpacing + (gameLineSpacing - menuDotSpacing) * easeProgress);
        int currentWidth = (int)(menuDotWidth + (gameLineWidth - menuDotWidth) * easeProgress);
        int currentHeight = (int)(menuDotHeight + (gameLineHeight - menuDotHeight) * easeProgress);
        
        // Interpolate opacity: menu style fades out, game style fades in
        int menuAlpha = (int)(30 * (1 - easeProgress));
        int gameAlpha = (int)(100 * easeProgress);
        int currentAlpha = Math.max(menuAlpha, gameAlpha);
        
        g.setColor(new Color(255, 255, 255, currentAlpha));
        
        for (int i = 0; i < BOARD_HEIGHT; i += currentSpacing) {
            g.fillRect(BOARD_WIDTH / 2 - currentWidth/2, i, currentWidth, currentHeight);
        }
    }
    
    private void drawTransitioningPaddles(Graphics2D g, double progress, 
                                        double leftX, double leftY, double rightX, double rightY, 
                                        double width, double height) {
        // Morph from wide rotated rectangles to narrow straight rectangles
        
        // Interpolate rotation from 25° to 0°
        double currentRotation = 25 * (1 - progress);
        
        // Save original transform
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        // Calculate corner radius that transitions from sharp (menu) to rounded (game)
        int gameCornerRadius = Math.max(4, PADDLE_WIDTH / 4);
        int currentCornerRadius = (int)(0 + gameCornerRadius * progress); // Start from 0 (sharp) to gameCornerRadius
        
        // Left paddle transition - center-based rendering with selected theme
        int leftCenterX = (int)(leftX + width / 2);
        int leftCenterY = (int)(leftY + height / 2);
        
        g.translate(leftCenterX, leftCenterY);
        g.rotate(Math.toRadians(-currentRotation));
        
        // Apply selected paddle theme in transition
        if (selectedPaddleTheme >= 0 && selectedPaddleTheme < bluePaddleThemeImages.size()) {
            BufferedImage paddleImg = bluePaddleThemeImages.get(selectedPaddleTheme);
            
            if (paddleImg != null) {
                // Custom theme paddle with rounded corners and clipping
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(-(int)width/2, -(int)height/2, (int)width, (int)height, currentCornerRadius, currentCornerRadius));
                g.drawImage(paddleImg, -(int)width/2, -(int)height/2, (int)width, (int)height, this);
                g.setClip(null);
            } else {
                // Default gradient for left paddle
                GradientPaint leftGradient = new GradientPaint(
                    -(int)width/2, -(int)height/2, new Color(100, 150, 255),
                    (int)width/2, (int)height/2, new Color(150, 200, 255));
                g.setPaint(leftGradient);
                g.fillRoundRect(-(int)width/2, -(int)height/2, (int)width, (int)height, currentCornerRadius, currentCornerRadius);
            }
        } else {
            // Fallback to default gradient
            GradientPaint leftGradient = new GradientPaint(
                -(int)width/2, -(int)height/2, new Color(100, 150, 255),
                (int)width/2, (int)height/2, new Color(150, 200, 255));
            g.setPaint(leftGradient);
            g.fillRoundRect(-(int)width/2, -(int)height/2, (int)width, (int)height, currentCornerRadius, currentCornerRadius);
        }
        
        // Reset transform
        g.setTransform(originalTransform);
        
        // Right paddle transition - center-based rendering with selected theme
        int rightCenterX = (int)(rightX + width / 2);
        int rightCenterY = (int)(rightY + height / 2);
        
        g.translate(rightCenterX, rightCenterY);
        g.rotate(Math.toRadians(currentRotation)); // Clockwise for right paddle
        
        // Apply selected right paddle theme in transition
        if (selectedRightPaddleTheme >= 0 && selectedRightPaddleTheme < redPaddleThemeImages.size()) {
            BufferedImage rightPaddleImg = redPaddleThemeImages.get(selectedRightPaddleTheme);
            
            if (rightPaddleImg != null) {
                // Custom theme paddle with rounded corners and clipping
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(-(int)width/2, -(int)height/2, (int)width, (int)height, currentCornerRadius, currentCornerRadius));
                g.drawImage(rightPaddleImg, -(int)width/2, -(int)height/2, (int)width, (int)height, this);
                g.setClip(null);
            } else {
                // Default gradient for right paddle
                GradientPaint rightGradient = new GradientPaint(
                    -(int)width/2, -(int)height/2, new Color(255, 100, 100),
                    (int)width/2, (int)height/2, new Color(255, 150, 150));
                g.setPaint(rightGradient);
                g.fillRoundRect(-(int)width/2, -(int)height/2, (int)width, (int)height, currentCornerRadius, currentCornerRadius);
            }
        } else {
            // Fallback to default gradient
            GradientPaint rightGradient = new GradientPaint(
                -(int)width/2, -(int)height/2, new Color(255, 100, 100),
                (int)width/2, (int)height/2, new Color(255, 150, 150));
            g.setPaint(rightGradient);
            g.fillRoundRect(-(int)width/2, -(int)height/2, (int)width, (int)height, currentCornerRadius, currentCornerRadius);
        }
        
        // Reset transform
        g.setTransform(originalTransform);
    }
    
    private void drawTransitioningBall(Graphics2D g, double progress) {
        // Interpolate ball position from current menu ball position to center of screen
        double currentBallX = menuBallX + (ballX - menuBallX) * progress;
        double currentBallY = menuBallY + (ballY - menuBallY) * progress;
        
        // Interpolate ball size from menu size to game size
        double currentBallSize = menuBallSize + (BALL_SIZE - menuBallSize) * progress;
        
        // Interpolate glow size
        int menuGlowSize = (int)(15 * Math.min(scaleX, scaleY)); // Larger glow for menu ball
        int gameGlowSize = (int)(5 * Math.min(scaleX, scaleY));
        int currentGlowSize = (int)(menuGlowSize + (gameGlowSize - menuGlowSize) * progress);
        
        // Draw ball with transitioning glow
        g.setColor(new Color(255, 255, 255, (int)(100 * (0.3 + 0.7 * progress)))); // Fade in glow
        g.fillOval((int)(currentBallX - currentGlowSize), (int)(currentBallY - currentGlowSize), 
                   (int)(currentBallSize + currentGlowSize * 2), (int)(currentBallSize + currentGlowSize * 2));
        
        // Inner glow (only for early transition to maintain menu ball look)
        if (progress < 0.7) {
            g.setColor(new Color(255, 255, 255, (int)(60 * (1 - progress))));
            g.fillOval((int)(currentBallX - currentGlowSize/2), (int)(currentBallY - currentGlowSize/2), 
                       (int)(currentBallSize + currentGlowSize), (int)(currentBallSize + currentGlowSize));
        }
        
        // Main ball
        g.setColor(Color.WHITE);
        g.fillOval((int)currentBallX, (int)currentBallY, (int)currentBallSize, (int)currentBallSize);
        
        // Add highlight (fade out as it transitions to game ball)
        if (progress < 0.8) {
            g.setColor(new Color(255, 255, 255, (int)(200 * (1 - progress))));
            int highlightSize = (int)(currentBallSize / 3);
            g.fillOval((int)(currentBallX + highlightSize/2), (int)(currentBallY + highlightSize/2), 
                       highlightSize, highlightSize);
        }
    }
    
    private void drawGameBackground(Graphics2D g) {
        if (selectedBackground >= 0 && selectedBackground < backgroundImages.size()) {
            Image backgroundImg = backgroundImages.get(selectedBackground);
            
            if (backgroundImg != null) {
                // Draw background image scaled to full screen
                g.drawImage(backgroundImg, 0, 0, getWidth(), getHeight(), this);
                
                // Add contrast effect only to the background image (not for default black)
                drawBackgroundContrastEffect(g);
            } else {
                // Draw default black background (no contrast effect needed)
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        } else {
            // Draw default black background (no contrast effect needed)
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        
        // Always draw center line on top of background (dotted style for better visibility)
        g.setColor(new Color(255, 255, 255, 150)); // Semi-transparent white for visibility on any background
        int lineSpacing = (int)(20 * scaleY);
        int lineWidth = Math.max(2, (int)(4 * scaleX));
        int lineHeight = (int)(10 * scaleY);
        for (int i = 0; i < getHeight(); i += lineSpacing) {
            g.fillRect(getWidth() / 2 - lineWidth/2, i, lineWidth, lineHeight);
        }
    }
    
    private void drawBackgroundContrastEffect(Graphics2D g) {
        // Save original composite
        Composite originalComposite = g.getComposite();
        
        // Create a stronger dark overlay for better contrast on entire background
        AlphaComposite contrastComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
        g.setComposite(contrastComposite);
        
        // Medium dark overlay to improve contrast for all game elements
        g.setColor(new Color(0, 0, 0, 140)); // Stronger dark overlay
        g.fillRect(0, 0, getWidth(), getHeight());
        
        // Add subtle gradient to make it less flat
        GradientPaint gradientOverlay = new GradientPaint(
            0, 0, new Color(0, 0, 0, 120), // Slightly lighter at top
            0, getHeight(), new Color(0, 0, 0, 160) // Darker at bottom
        );
        
        AlphaComposite gradientComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);
        g.setComposite(gradientComposite);
        g.setPaint(gradientOverlay);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        // Restore original composite and paint
        g.setComposite(originalComposite);
        g.setPaint(null);
    }
    
    private void drawGameModeSelection(Graphics2D g) {
        // Draw selected background first
        drawMenuBackground(g);
        
        // Draw contrast overlay
        drawMenuContrastEffect(g);
        
        // Draw vertical paddles on the sides
        drawGameModeSelectionPaddles(g);
        
        // Draw title
        float titleSize = (float)(36 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(titleSize));
        FontMetrics titleFm = g.getFontMetrics();
        String title = "MODALITA' DI GIOCO";
        int titleX = (BOARD_WIDTH - titleFm.stringWidth(title)) / 2;
        int titleY = (int)(120 * scaleY);
        
        // Title glow effect
        g.setColor(new Color(0, 255, 255, 80));
        for (int i = 1; i <= 2; i++) {
            g.drawString(title, titleX - i, titleY - i);
            g.drawString(title, titleX + i, titleY + i);
        }
        Color titleColor = currentTextColors.getOrDefault("menuTitle", Color.WHITE);
        g.setColor(titleColor);
        g.drawString(title, titleX, titleY);
        
        // Draw game modes in vertical list
        int modeWidth = (int)(400 * scaleX);
        int modeHeight = (int)(80 * scaleY);
        int modeSpacing = (int)(20 * scaleY);
        int startX = (BOARD_WIDTH - modeWidth) / 2;
        int startY = (int)(200 * scaleY);
        
        for (int i = 0; i < gameModes.length; i++) {
            int modeY = startY + i * (modeHeight + modeSpacing);
            boolean isSelected = (i == selectedGameMode);
            
            // Mode background
            if (isSelected) {
                g.setColor(new Color(40, 40, 60, 200));
            } else {
                g.setColor(new Color(20, 20, 30, 150));
            }
            g.fillRoundRect(startX, modeY, modeWidth, modeHeight, 15, 15);
            
            // Mode border
            if (isSelected) {
                g.setColor(new Color(100, 150, 255));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(startX, modeY, modeWidth, modeHeight, 15, 15);
            }
            
            // Game mode title
            float modeSize = (float)(28 * Math.min(scaleX, scaleY));
            g.setFont(primaryFont.deriveFont(modeSize));
            FontMetrics modeFm = g.getFontMetrics();
            
            g.setColor(isSelected ? new Color(150, 200, 255) : Color.WHITE);
            String modeTitle = gameModes[i];
            int modeTitleX = startX + (modeWidth - modeFm.stringWidth(modeTitle)) / 2;
            int modeTitleY = modeY + (int)(35 * scaleY);
            g.drawString(modeTitle, modeTitleX, modeTitleY);
            
            // Game mode description
            float descSize = (float)(16 * Math.min(scaleX, scaleY));
            g.setFont(secondaryFont.deriveFont(descSize));
            FontMetrics descFm = g.getFontMetrics();
            
            g.setColor(isSelected ? new Color(180, 180, 180) : new Color(140, 140, 140));
            String description = gameModeDescriptions[i];
            int descX = startX + (modeWidth - descFm.stringWidth(description)) / 2;
            int descY = modeY + (int)(58 * scaleY);
            g.drawString(description, descX, descY);
        }
    }
    
    private void drawGameModeSelectionPaddles(Graphics2D g) {
        // Use same dimensions as menu paddles but rotated 90 degrees
        int widePaddleWidth = (int)(250 * Math.min(scaleX, scaleY)); // Wide dimension from menu
        int paddleHeight = (int)(menuPaddleHeight * 1.8); // Tall dimension from menu
        int paddleYOffset = (int)(-paddleHeight * 0.2);
        
        // Save original transform
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        // Left paddle position - rotated 90 degrees from menu position
        int leftCenterX = (int)(80 * scaleX); // Distance from left edge
        int leftCenterY = BOARD_HEIGHT / 2;
        
        g.translate(leftCenterX, leftCenterY);
        g.rotate(Math.toRadians(0)); // No rotation - straight vertical
        
        // Apply selected paddle theme for left paddle
        if (selectedPaddleTheme >= 0 && selectedPaddleTheme < bluePaddleThemeImages.size()) {
            BufferedImage paddleImg = bluePaddleThemeImages.get(selectedPaddleTheme);
            if (paddleImg != null) {
                // Custom theme paddle with rounded corners and clipping
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, 12, 12));
                g.drawImage(paddleImg, -widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, this);
                g.setClip(null);
            } else {
                // Default gradient for left paddle
                GradientPaint leftGradient = new GradientPaint(
                    -widePaddleWidth/2, -paddleHeight/2, new Color(100, 150, 255),
                    widePaddleWidth/2, paddleHeight/2, new Color(150, 200, 255));
                g.setPaint(leftGradient);
                g.fillRoundRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, 12, 12);
                g.setPaint(null);
            }
        } else {
            // Default gradient for left paddle
            GradientPaint leftGradient = new GradientPaint(
                -widePaddleWidth/2, -paddleHeight/2, new Color(100, 150, 255),
                widePaddleWidth/2, paddleHeight/2, new Color(150, 200, 255));
            g.setPaint(leftGradient);
            g.fillRoundRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, 12, 12);
            g.setPaint(null);
        }
        
        // Left paddle glow
        int glowWidth = Math.max(2, (int)(8 * Math.min(scaleX, scaleY)));
        g.setColor(getPaddleGlowColor(true)); // Use theme-based glow
        g.fillRect(widePaddleWidth/2, -paddleHeight/2, glowWidth, paddleHeight);
        
        // Reset transform for right paddle
        g.setTransform(originalTransform);
        
        // Right paddle position - rotated 90 degrees from menu position
        int rightCenterX = BOARD_WIDTH - (int)(80 * scaleX); // Distance from right edge
        int rightCenterY = BOARD_HEIGHT / 2;
        
        g.translate(rightCenterX, rightCenterY);
        g.rotate(Math.toRadians(0)); // No rotation - straight vertical
        
        // Apply selected right paddle theme
        if (selectedRightPaddleTheme >= 0 && selectedRightPaddleTheme < redPaddleThemeImages.size()) {
            BufferedImage rightPaddleImg = redPaddleThemeImages.get(selectedRightPaddleTheme);
            if (rightPaddleImg != null) {
                // Custom theme paddle with rounded corners and clipping
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, 12, 12));
                g.drawImage(rightPaddleImg, -widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, this);
                g.setClip(null);
            } else {
                // Default red gradient for right paddle
                GradientPaint rightGradient = new GradientPaint(
                    -widePaddleWidth/2, -paddleHeight/2, new Color(255, 100, 100),
                    widePaddleWidth/2, paddleHeight/2, new Color(255, 150, 150));
                g.setPaint(rightGradient);
                g.fillRoundRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, 12, 12);
                g.setPaint(null);
            }
        } else {
            // Default red gradient for right paddle
            GradientPaint rightGradient = new GradientPaint(
                -widePaddleWidth/2, -paddleHeight/2, new Color(255, 100, 100),
                widePaddleWidth/2, paddleHeight/2, new Color(255, 150, 150));
            g.setPaint(rightGradient);
            g.fillRoundRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, 12, 12);
            g.setPaint(null);
        }
        
        // Right paddle glow
        g.setColor(getPaddleGlowColor(false)); // Use theme-based glow
        g.fillRect(-widePaddleWidth/2 - glowWidth, -paddleHeight/2, glowWidth, paddleHeight);
        
        // Reset transform
        g.setTransform(originalTransform);
    }
    
    private void drawGame(Graphics2D g) {
        // Draw selected background first
        drawGameBackground(g);
        
        // Draw particles next (behind game elements but on top of background)
        // Use copy to avoid ConcurrentModificationException
        ArrayList<Particle> particlesCopy = new ArrayList<>(particles);
        for (Particle p : particlesCopy) {
            p.draw(g);
        }
        
        g.setColor(Color.WHITE);
        
        // Calculate scaled paddle positions
        int leftPaddleX = (int)(20 * scaleX);
        int rightPaddleX = BOARD_WIDTH - leftPaddleX - PADDLE_WIDTH;
        
        // Draw paddles with gradient effect and rounded corners - stessi colori del menu
        int cornerRadius = Math.max(4, PADDLE_WIDTH / 4); // Corner radius based on paddle width
        
        // Draw dynamic paddle glow effects
        if (leftPaddleGlow > 0) {
            int glowSize = (int)(leftPaddleGlow * 12 * Math.min(scaleX, scaleY));
            int alpha = (int)(leftPaddleGlow * 150);
            g.setColor(new Color(cachedLeftGlowColor.getRed(), cachedLeftGlowColor.getGreen(), cachedLeftGlowColor.getBlue(), alpha));
            g.fillRoundRect(leftPaddleX - glowSize/2, paddle1Y - glowSize/2, 
                          PADDLE_WIDTH + glowSize, PADDLE_HEIGHT + glowSize, 
                          cornerRadius + glowSize/2, cornerRadius + glowSize/2);
        }
        
        if (rightPaddleGlow > 0) {
            int glowSize = (int)(rightPaddleGlow * 12 * Math.min(scaleX, scaleY));
            int alpha = (int)(rightPaddleGlow * 150);
            g.setColor(new Color(cachedRightGlowColor.getRed(), cachedRightGlowColor.getGreen(), cachedRightGlowColor.getBlue(), alpha));
            g.fillRoundRect(rightPaddleX - glowSize/2, paddle2Y - glowSize/2,
                          PADDLE_WIDTH + glowSize, PADDLE_HEIGHT + glowSize,
                          cornerRadius + glowSize/2, cornerRadius + glowSize/2);
        }
        
        // Left paddle with rounded corners - use selected theme
        if (selectedPaddleTheme >= 0 && selectedPaddleTheme < bluePaddleThemeImages.size()) {
            BufferedImage paddleImg = bluePaddleThemeImages.get(selectedPaddleTheme);
            
            if (paddleImg != null) {
                // Custom theme paddle with rounded corners
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(leftPaddleX, paddle1Y, PADDLE_WIDTH, PADDLE_HEIGHT, cornerRadius, cornerRadius));
                g.drawImage(paddleImg, leftPaddleX, paddle1Y, PADDLE_WIDTH, PADDLE_HEIGHT, this);
                g.setClip(null);
            } else {
                // Default gradient paddle
                GradientPaint paddle1Gradient = new GradientPaint(
                    leftPaddleX, paddle1Y, new Color(100, 150, 255), 
                    leftPaddleX + PADDLE_WIDTH, paddle1Y + PADDLE_HEIGHT, new Color(150, 200, 255));
                g.setPaint(paddle1Gradient);
                g.fillRoundRect(leftPaddleX, paddle1Y, PADDLE_WIDTH, PADDLE_HEIGHT, cornerRadius, cornerRadius);
            }
        } else {
            // Fallback to default gradient
            GradientPaint paddle1Gradient = new GradientPaint(
                leftPaddleX, paddle1Y, new Color(100, 150, 255), 
                leftPaddleX + PADDLE_WIDTH, paddle1Y + PADDLE_HEIGHT, new Color(150, 200, 255));
            g.setPaint(paddle1Gradient);
            g.fillRoundRect(leftPaddleX, paddle1Y, PADDLE_WIDTH, PADDLE_HEIGHT, cornerRadius, cornerRadius);
        }
        
        // Right paddle with rounded corners - use selected theme
        // Use smooth AI position for rendering in single player mode
        int displayPaddle2Y = currentState == GameState.SINGLE_PLAYER ? (int)aiPaddleY : paddle2Y;
        if (selectedRightPaddleTheme >= 0 && selectedRightPaddleTheme < redPaddleThemeImages.size()) {
            BufferedImage rightPaddleImg = redPaddleThemeImages.get(selectedRightPaddleTheme);
            
            if (rightPaddleImg != null) {
                // Custom theme paddle with rounded corners
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(rightPaddleX, displayPaddle2Y, PADDLE_WIDTH, PADDLE_HEIGHT, cornerRadius, cornerRadius));
                g.drawImage(rightPaddleImg, rightPaddleX, displayPaddle2Y, PADDLE_WIDTH, PADDLE_HEIGHT, this);
                g.setClip(null);
            } else {
                // Default gradient paddle
                GradientPaint paddle2Gradient = new GradientPaint(
                    rightPaddleX, displayPaddle2Y, new Color(255, 100, 100),
                    rightPaddleX + PADDLE_WIDTH, displayPaddle2Y + PADDLE_HEIGHT, new Color(255, 150, 150));
                g.setPaint(paddle2Gradient);
                g.fillRoundRect(rightPaddleX, displayPaddle2Y, PADDLE_WIDTH, PADDLE_HEIGHT, cornerRadius, cornerRadius);
            }
        } else {
            // Fallback to default gradient
            GradientPaint paddle2Gradient = new GradientPaint(
                rightPaddleX, displayPaddle2Y, new Color(255, 100, 100),
                rightPaddleX + PADDLE_WIDTH, displayPaddle2Y + PADDLE_HEIGHT, new Color(255, 150, 150));
            g.setPaint(paddle2Gradient);
            g.fillRoundRect(rightPaddleX, displayPaddle2Y, PADDLE_WIDTH, PADDLE_HEIGHT, cornerRadius, cornerRadius);
        }
        
        // Draw ball trail first (behind ball)
        drawBallTrail(g);
        
        // Draw ball with fire effects if active
        drawFireBall(g);
        
        // Draw scores with shadow (scaled)
        float scoreSize = (float)(40 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(scoreSize));
        FontMetrics fm = g.getFontMetrics();
        int scoreY = (int)(60 * scaleY);
        int shadowOffset = Math.max(1, (int)(2 * Math.min(scaleX, scaleY)));
        
        // Shadows
        g.setColor(new Color(0, 0, 0, 100));
        g.drawString(String.valueOf(score1), BOARD_WIDTH / 4 - fm.stringWidth(String.valueOf(score1)) / 2 + shadowOffset, scoreY + shadowOffset);
        g.drawString(String.valueOf(score2), 3 * BOARD_WIDTH / 4 - fm.stringWidth(String.valueOf(score2)) / 2 + shadowOffset, scoreY + shadowOffset);
        
        // Scores
        g.setColor(currentTextColors.getOrDefault("gameScores", Color.WHITE));
        g.drawString(String.valueOf(score1), BOARD_WIDTH / 4 - fm.stringWidth(String.valueOf(score1)) / 2, scoreY);
        g.drawString(String.valueOf(score2), 3 * BOARD_WIDTH / 4 - fm.stringWidth(String.valueOf(score2)) / 2, scoreY);
        
        // Draw advanced combo counter with effects (only when visible)
        if (comboCount > 0 && showCombo) {
            drawAdvancedCombo(g, shadowOffset);
        }
        
        // Draw right paddle combo in multiplayer mode (top-right corner)
        if (currentState == GameState.PLAYING && rightComboCount > 0 && showRightCombo) {
            drawAdvancedRightCombo(g, shadowOffset);
        }
        
        // Draw fire ball system status
        if (consecutivePaddleBounces >= 3) { // Show when getting close to fire mode
            drawFireBallStatus(g, shadowOffset);
        }
        
        // Draw "by Gava" signature
        drawGavaSignature(g);
        
    }
    
    private void drawGameForPause(Graphics2D g) {
        // Draw game background without center line
        drawGameBackgroundForPause(g);
        
        // Draw particles next (behind game elements but on top of background)
        ArrayList<Particle> particlesCopy = new ArrayList<>(particles);
        for (Particle p : particlesCopy) {
            p.draw(g);
        }
        
        g.setColor(Color.WHITE);
        
        // Calculate scaled paddle positions
        int leftPaddleX = (int)(20 * scaleX);
        int rightPaddleX = BOARD_WIDTH - leftPaddleX - PADDLE_WIDTH;
        
        // Draw paddles with gradient effect and rounded corners - stessi colori del menu
        int cornerRadius = Math.max(4, PADDLE_WIDTH / 4);
        
        // Draw dynamic paddle glow effects
        if (leftPaddleGlow > 0) {
            int glowSize = (int)(leftPaddleGlow * 12 * Math.min(scaleX, scaleY));
            int alpha = (int)(leftPaddleGlow * 150);
            g.setColor(new Color(cachedLeftGlowColor.getRed(), cachedLeftGlowColor.getGreen(), cachedLeftGlowColor.getBlue(), alpha));
            g.fillRoundRect(leftPaddleX - glowSize/2, paddle1Y - glowSize/2, 
                          PADDLE_WIDTH + glowSize, PADDLE_HEIGHT + glowSize, 
                          cornerRadius + glowSize/2, cornerRadius + glowSize/2);
        }
        
        if (rightPaddleGlow > 0) {
            int glowSize = (int)(rightPaddleGlow * 12 * Math.min(scaleX, scaleY));
            int alpha = (int)(rightPaddleGlow * 150);
            g.setColor(new Color(cachedRightGlowColor.getRed(), cachedRightGlowColor.getGreen(), cachedRightGlowColor.getBlue(), alpha));
            g.fillRoundRect(rightPaddleX - glowSize/2, paddle2Y - glowSize/2,
                          PADDLE_WIDTH + glowSize, PADDLE_HEIGHT + glowSize,
                          cornerRadius + glowSize/2, cornerRadius + glowSize/2);
        }
        
        // Left paddle with rounded corners - use selected theme
        if (selectedPaddleTheme >= 0 && selectedPaddleTheme < bluePaddleThemeImages.size()) {
            BufferedImage paddleImg = bluePaddleThemeImages.get(selectedPaddleTheme);
            
            if (paddleImg != null) {
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(leftPaddleX, paddle1Y, PADDLE_WIDTH, PADDLE_HEIGHT, cornerRadius, cornerRadius));
                g.drawImage(paddleImg, leftPaddleX, paddle1Y, PADDLE_WIDTH, PADDLE_HEIGHT, this);
                g.setClip(null);
            } else {
                GradientPaint paddle1Gradient = new GradientPaint(
                    leftPaddleX, paddle1Y, new Color(100, 150, 255), 
                    leftPaddleX + PADDLE_WIDTH, paddle1Y + PADDLE_HEIGHT, new Color(150, 200, 255));
                g.setPaint(paddle1Gradient);
                g.fillRoundRect(leftPaddleX, paddle1Y, PADDLE_WIDTH, PADDLE_HEIGHT, cornerRadius, cornerRadius);
            }
        } else {
            GradientPaint paddle1Gradient = new GradientPaint(
                leftPaddleX, paddle1Y, new Color(100, 150, 255), 
                leftPaddleX + PADDLE_WIDTH, paddle1Y + PADDLE_HEIGHT, new Color(150, 200, 255));
            g.setPaint(paddle1Gradient);
            g.fillRoundRect(leftPaddleX, paddle1Y, PADDLE_WIDTH, PADDLE_HEIGHT, cornerRadius, cornerRadius);
        }
        
        // Right paddle with rounded corners - use selected theme
        int displayPaddle2Y = currentState == GameState.SINGLE_PLAYER ? (int)aiPaddleY : paddle2Y;
        if (selectedRightPaddleTheme >= 0 && selectedRightPaddleTheme < redPaddleThemeImages.size()) {
            BufferedImage rightPaddleImg = redPaddleThemeImages.get(selectedRightPaddleTheme);
            
            if (rightPaddleImg != null) {
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(rightPaddleX, displayPaddle2Y, PADDLE_WIDTH, PADDLE_HEIGHT, cornerRadius, cornerRadius));
                g.drawImage(rightPaddleImg, rightPaddleX, displayPaddle2Y, PADDLE_WIDTH, PADDLE_HEIGHT, this);
                g.setClip(null);
            } else {
                GradientPaint paddle2Gradient = new GradientPaint(
                    rightPaddleX, displayPaddle2Y, new Color(255, 100, 100),
                    rightPaddleX + PADDLE_WIDTH, displayPaddle2Y + PADDLE_HEIGHT, new Color(255, 150, 150));
                g.setPaint(paddle2Gradient);
                g.fillRoundRect(rightPaddleX, displayPaddle2Y, PADDLE_WIDTH, PADDLE_HEIGHT, cornerRadius, cornerRadius);
            }
        } else {
            GradientPaint paddle2Gradient = new GradientPaint(
                rightPaddleX, displayPaddle2Y, new Color(255, 100, 100),
                rightPaddleX + PADDLE_WIDTH, displayPaddle2Y + PADDLE_HEIGHT, new Color(255, 150, 150));
            g.setPaint(paddle2Gradient);
            g.fillRoundRect(rightPaddleX, displayPaddle2Y, PADDLE_WIDTH, PADDLE_HEIGHT, cornerRadius, cornerRadius);
        }
        
        // Draw ball with glow effect
        int glowSize = 8;
        RadialGradientPaint ballGradient = new RadialGradientPaint(
            (float)ballX + BALL_SIZE/2f, (float)ballY + BALL_SIZE/2f, 
            BALL_SIZE/2f + glowSize,
            new float[]{0f, 0.7f, 1f},
            new Color[]{Color.WHITE, new Color(255, 255, 255, 200), new Color(255, 255, 255, 0)}
        );
        g.setPaint(ballGradient);
        g.fillOval((int)ballX - glowSize, (int)ballY - glowSize, 
                  BALL_SIZE + 2*glowSize, BALL_SIZE + 2*glowSize);
        
        g.setColor(Color.WHITE);
        g.fillOval((int)ballX, (int)ballY, BALL_SIZE, BALL_SIZE);
        g.setPaint(null);
        
        // DO NOT draw scores - they are handled by pause overlay
        // DO NOT draw center line - it's handled by pause overlay  
        // DO NOT draw combo counters - they are not shown in pause
    }
    
    private void drawGameBackgroundForPause(Graphics2D g) {
        // Draw the selected background theme
        if (selectedBackground >= 0 && selectedBackground < backgroundImages.size()) {
            Image backgroundImg = backgroundImages.get(selectedBackground);
            if (backgroundImg != null) {
                // Draw tiled or scaled background
                g.drawImage(backgroundImg, 0, 0, getWidth(), getHeight(), this);
                
                // Add slight dark overlay for better contrast (stronger for pause)
                drawBackgroundContrastEffect(g);
            } else {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        } else {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        
        // DO NOT draw center line - it will be drawn by pause overlay as diagonal
    }
    
    private void drawAdvancedCombo(Graphics2D g, int shadowOffset) {
        // Calculate dynamic size with scale and pulse effects
        float baseSize = (float)(28 * Math.min(scaleX, scaleY));
        float dynamicSize = baseSize * comboScale * comboPulse;
        
        g.setFont(primaryFont.deriveFont(Font.BOLD, dynamicSize));
        FontMetrics comboFm = g.getFontMetrics();
        
        String comboText = "COMBO";
        String numberText = String.valueOf(comboCount);
        
        int comboX = (int)(25 * scaleX);
        int comboY = (int)(40 * scaleY);
        
        // Calculate text dimensions for centering
        int comboWidth = comboFm.stringWidth(comboText);
        int numberWidth = comboFm.stringWidth(numberText);
        int maxWidth = Math.max(comboWidth, numberWidth);
        
        // Glow effect removed - cleaner look
        
        // Draw shadow with enhanced offset for bigger text
        int enhancedShadowOffset = Math.max(2, (int)(shadowOffset * comboScale));
        g.setColor(new Color(0, 0, 0, 200));
        g.drawString(comboText, comboX + enhancedShadowOffset, comboY + enhancedShadowOffset);
        g.drawString(numberText, comboX + enhancedShadowOffset, comboY + (int)dynamicSize + enhancedShadowOffset);
        
        // Draw main combo text with dynamic color
        g.setColor(comboColor);
        g.drawString(comboText, comboX, comboY);
        
        // Draw combo number with extra emphasis
        if (comboMilestoneHit) {
            // Milestone effect - extra glow and scale
            float milestoneScale = 1.0f + (float)Math.sin(comboMilestoneTimer * 0.3) * 0.3f;
            Font milestoneFont = primaryFont.deriveFont(Font.BOLD, dynamicSize * milestoneScale);
            g.setFont(milestoneFont);
            
            // Rainbow effect for milestones
            float hue = (System.currentTimeMillis() * 0.01f) % 1.0f;
            Color rainbowColor = Color.getHSBColor(hue, 1.0f, 1.0f);
            g.setColor(rainbowColor);
        }
        
        g.drawString(numberText, comboX, comboY + (int)dynamicSize);
        
        // Draw milestone celebration text
        if (comboMilestoneHit && comboCount % 10 == 0) {
            String milestoneText = "";
            if (comboCount >= 50) milestoneText = "LEGENDARY!";
            else if (comboCount >= 20) milestoneText = "AMAZING!";
            else if (comboCount >= 10) milestoneText = "GREAT!";
            
            if (!milestoneText.isEmpty()) {
                float milestoneSize = baseSize * 0.6f;
                g.setFont(primaryFont.deriveFont(Font.BOLD, milestoneSize));
                FontMetrics milestoneFm = g.getFontMetrics();
                
                int milestoneX = comboX;
                int milestoneY = comboY + (int)dynamicSize + milestoneFm.getHeight();
                
                // Pulsing milestone text
                float milestoneAlpha = (float)Math.sin(comboMilestoneTimer * 0.4) * 0.5f + 0.5f;
                g.setColor(new Color(255, 255, 255, (int)(milestoneAlpha * 255)));
                g.drawString(milestoneText, milestoneX, milestoneY);
            }
        }
    }
    
    private void drawAdvancedRightCombo(Graphics2D g, int shadowOffset) {
        // Calculate dynamic size with scale and pulse effects
        float baseSize = (float)(28 * Math.min(scaleX, scaleY));
        float dynamicSize = baseSize * rightComboScale * rightComboPulse;
        
        g.setFont(primaryFont.deriveFont(Font.BOLD, dynamicSize));
        FontMetrics comboFm = g.getFontMetrics();
        
        String comboText = "COMBO";
        String numberText = String.valueOf(rightComboCount);
        
        // Position in top-right corner
        int comboWidth = comboFm.stringWidth(comboText);
        int numberWidth = comboFm.stringWidth(numberText);
        int maxWidth = Math.max(comboWidth, numberWidth);
        
        int comboX = BOARD_WIDTH - maxWidth - (int)(25 * scaleX); // Right aligned
        int comboY = (int)(40 * scaleY);
        
        // Draw shadow with enhanced offset for bigger text
        int enhancedShadowOffset = Math.max(2, (int)(shadowOffset * rightComboScale));
        g.setColor(new Color(0, 0, 0, 200));
        g.drawString(comboText, comboX + enhancedShadowOffset, comboY + enhancedShadowOffset);
        g.drawString(numberText, comboX + enhancedShadowOffset, comboY + (int)dynamicSize + enhancedShadowOffset);
        
        // Draw main combo text with dynamic color
        g.setColor(rightComboColor);
        g.drawString(comboText, comboX, comboY);
        
        // Draw combo number with extra emphasis
        if (rightComboMilestoneHit) {
            // Milestone effect - extra glow and scale
            float milestoneScale = 1.0f + (float)Math.sin(rightComboMilestoneTimer * 0.3) * 0.3f;
            Font milestoneFont = primaryFont.deriveFont(Font.BOLD, dynamicSize * milestoneScale);
            g.setFont(milestoneFont);
            
            // Rainbow effect for milestones
            float hue = (System.currentTimeMillis() * 0.01f) % 1.0f;
            Color rainbowColor = Color.getHSBColor(hue, 1.0f, 1.0f);
            g.setColor(rainbowColor);
        }
        
        g.drawString(numberText, comboX, comboY + (int)dynamicSize);
        
        // Draw milestone celebration text
        if (rightComboMilestoneHit && rightComboCount % 10 == 0) {
            String milestoneText = "";
            if (rightComboCount >= 50) milestoneText = "LEGENDARY!";
            else if (rightComboCount >= 20) milestoneText = "AMAZING!";
            else if (rightComboCount >= 10) milestoneText = "GREAT!";
            
            if (!milestoneText.isEmpty()) {
                float milestoneSize = baseSize * 0.6f;
                g.setFont(primaryFont.deriveFont(Font.BOLD, milestoneSize));
                FontMetrics milestoneFm = g.getFontMetrics();
                
                // Right align milestone text too
                int milestoneWidth = milestoneFm.stringWidth(milestoneText);
                int milestoneX = BOARD_WIDTH - milestoneWidth - (int)(25 * scaleX);
                int milestoneY = comboY + (int)dynamicSize + milestoneFm.getHeight();
                
                // Pulsing milestone text
                float milestoneAlpha = (float)Math.sin(rightComboMilestoneTimer * 0.4) * 0.5f + 0.5f;
                g.setColor(new Color(255, 255, 255, (int)(milestoneAlpha * 255)));
                g.drawString(milestoneText, milestoneX, milestoneY);
            }
        }
    }
    
    private void drawPauseOverlay(Graphics2D g) {
        // Background overlay
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
        
        // Only draw diagonal line if we're NOT transitioning from pause
        // (During exit transition, the line is handled by drawTransitionCenterLine)
        if (!isTransitioningFromPause) {
            // Diagonal line from bottom-left to top-right (same dashed pattern as game center line)
            g.setColor(new Color(255, 255, 255, 150)); // Same transparency as game center line
            
            // Use exact same pattern as game center line
            int lineSpacing = (int)(20 * scaleY);
            int lineWidth = Math.max(2, (int)(4 * scaleX));
            int lineHeight = (int)(10 * scaleY);
            
            // Calculate diagonal line parameters
            double lineLength = Math.sqrt(BOARD_WIDTH * BOARD_WIDTH + BOARD_HEIGHT * BOARD_HEIGHT);
            double angle = Math.atan2(-BOARD_HEIGHT, BOARD_WIDTH); // From bottom-left to top-right
            double cosAngle = Math.cos(angle);
            double sinAngle = Math.sin(angle);
            
            // Draw dashed diagonal segments with same spacing as game center line + animation offset
            for (double distance = -pauseLineOffset; distance < lineLength; distance += lineSpacing) {
                // Calculate start point of this segment
                double startX = distance * cosAngle;
                double startY = BOARD_HEIGHT + distance * sinAngle;
                
                // Calculate end point of this segment (parallel to diagonal)
                double segmentEndDistance = distance + lineHeight;
                double endX = segmentEndDistance * cosAngle;
                double endY = BOARD_HEIGHT + segmentEndDistance * sinAngle;
                
                // Only draw if segment is within screen bounds
                if (startX >= 0 && startX <= BOARD_WIDTH && startY >= 0 && startY <= BOARD_HEIGHT &&
                    endX >= 0 && endX <= BOARD_WIDTH && endY >= 0 && endY <= BOARD_HEIGHT) {
                    g.setStroke(new java.awt.BasicStroke(lineWidth, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_MITER));
                    g.drawLine((int)startX, (int)startY, (int)endX, (int)endY);
                }
            }
        }
        
        // Left paddle score (top-left)
        g.setColor(new Color(100, 150, 255)); // Blue color for left paddle
        float scoreSize = (float)(96 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(Font.BOLD, scoreSize));
        FontMetrics scoreFm = g.getFontMetrics();
        String leftScore = String.valueOf(score1);
        
        int leftScoreX = (int)(40 * scaleX);
        int leftScoreY = (int)(120 * scaleY);
        
        // Left score shadow
        g.setColor(new Color(0, 0, 0, 120));
        g.drawString(leftScore, leftScoreX + 4, leftScoreY + 4);
        
        // Left score text
        g.setColor(new Color(150, 200, 255));
        g.drawString(leftScore, leftScoreX, leftScoreY);
        
        // Right paddle score (bottom-right)
        g.setColor(new Color(255, 100, 100)); // Red color for right paddle
        String rightScore = String.valueOf(score2);
        int rightScoreWidth = scoreFm.stringWidth(rightScore);
        
        int rightScoreX = BOARD_WIDTH - rightScoreWidth - (int)(40 * scaleX);
        int rightScoreY = BOARD_HEIGHT - (int)(80 * scaleY);
        
        // Right score shadow
        g.setColor(new Color(0, 0, 0, 120));
        g.drawString(rightScore, rightScoreX + 4, rightScoreY + 4);
        
        // Right score text
        g.setColor(new Color(255, 150, 150));
        g.drawString(rightScore, rightScoreX, rightScoreY);
        
        // Title "PAUSA" (top center)
        g.setColor(Color.WHITE);
        float titleSize = (float)(56 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(Font.BOLD, titleSize));
        FontMetrics titleFm = g.getFontMetrics();
        String pauseTitle = getText("GAME_PAUSE");
        
        int titleX = (BOARD_WIDTH - titleFm.stringWidth(pauseTitle)) / 2;
        int titleY = (int)(80 * scaleY); // Moved to top
        
        // Title glow effect
        g.setColor(new Color(255, 255, 255, 100));
        g.drawString(pauseTitle, titleX + 3, titleY + 3);
        g.drawString(pauseTitle, titleX - 3, titleY - 3);
        
        // Title text
        g.setColor(Color.WHITE);
        g.drawString(pauseTitle, titleX, titleY);
        
        // Instructions (bottom center)
        float instructSize = (float)(20 * Math.min(scaleX, scaleY));
        g.setFont(secondaryFont.deriveFont(instructSize));
        FontMetrics instructFm = g.getFontMetrics();
        
        g.setColor(new Color(220, 220, 220, 255));
        String[] instructions = {
            getText("GAME_PAUSE_RESUME"),
            getText("GAME_PAUSE_EXIT")
        };
        
        int startY = BOARD_HEIGHT - (int)(120 * scaleY); // Moved to bottom
        int instructLineSpacing = (int)(35 * Math.min(scaleX, scaleY));
        
        for (int i = 0; i < instructions.length; i++) {
            int instructX = (BOARD_WIDTH - instructFm.stringWidth(instructions[i])) / 2;
            int instructY = startY + (i * instructLineSpacing);
            
            // Instruction shadow
            g.setColor(new Color(0, 0, 0, 80));
            g.drawString(instructions[i], instructX + 2, instructY + 2);
            
            // Instruction text
            g.setColor(new Color(220, 220, 220, 255));
            g.drawString(instructions[i], instructX, instructY);
        }
        
        // Draw motivational message scrolling along diagonal
        if (showMotivationalMessage && !currentMotivationalMessage.isEmpty()) {
            drawMotivationalMessage(g);
        }
        
        // Draw "by Gava" signature
        drawGavaSignature(g);
    }
    
    private void drawMotivationalMessage(Graphics2D g) {
        // Calculate diagonal position for message
        double diagonalLength = Math.sqrt(BOARD_WIDTH * BOARD_WIDTH + BOARD_HEIGHT * BOARD_HEIGHT);
        double angle = Math.atan2(-BOARD_HEIGHT, BOARD_WIDTH); // Same angle as diagonal line
        double cosAngle = Math.cos(angle);
        double sinAngle = Math.sin(angle);
        
        // Calculate message position along diagonal (from bottom-left to top-right)
        double messageX = messageScrollOffset * cosAngle;
        double messageY = BOARD_HEIGHT + messageScrollOffset * sinAngle;
        
        // Only draw if message is within screen bounds
        if (messageX >= -200 && messageX <= BOARD_WIDTH + 200 && 
            messageY >= -200 && messageY <= BOARD_HEIGHT + 200) {
            
            // Save original transform
            AffineTransform originalTransform = g.getTransform();
            
            // Rotate graphics context to align with diagonal
            g.translate(messageX, messageY);
            g.rotate(angle);
            
            // Parse message to check for difficulty names and apply special effects
            String message = currentMotivationalMessage;
            float messageSize = (float)(24 * Math.min(scaleX, scaleY));
            
            // Simple white text rendering
            g.setFont(primaryFont.deriveFont(Font.BOLD, messageSize));
            FontMetrics fm = g.getFontMetrics();
            
            // Center the text vertically on the line
            int textHeight = fm.getAscent();
            int yOffset = -textHeight / 2;
            
            // Shadow effect
            g.setColor(new Color(0, 0, 0, 150));
            g.drawString(message, 2, yOffset + 2);
            
            // Main white text
            g.setColor(Color.WHITE);
            g.drawString(message, 0, yOffset);
            
            // Restore original transform
            g.setTransform(originalTransform);
        }
    }
    
    protected void drawGameOverOverlay(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
        
        // Winner announcement (scaled)
        g.setColor(Color.YELLOW);
        float winnerSize = (float)(48 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(winnerSize));
        FontMetrics winnerFm = g.getFontMetrics();
        String winnerText = winner + " " + getText("GAME_WINNER") + "!";
        int winnerY = (int)(BOARD_HEIGHT / 2 - 50 * scaleY);
        g.drawString(winnerText, (BOARD_WIDTH - winnerFm.stringWidth(winnerText)) / 2, winnerY);
        
        // Final score (scaled)
        g.setColor(Color.WHITE);
        float scoreSize = (float)(32 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(scoreSize));
        FontMetrics scoreFm = g.getFontMetrics();
        String finalScore = score1 + " - " + score2;
        g.drawString(finalScore, (BOARD_WIDTH - scoreFm.stringWidth(finalScore)) / 2, BOARD_HEIGHT / 2);
        
        
        // Instructions (scaled)
        g.setColor(Color.GRAY);
        String instruct = getText("UI_PRESS_ESC");
        FontMetrics instructFm = g.getFontMetrics();
        int instructY = (int)(BOARD_HEIGHT / 2 + 80 * scaleY);
        g.drawString(instruct, (BOARD_WIDTH - instructFm.stringWidth(instruct)) / 2, instructY);
        
        // Draw "by Gava" signature
        drawGavaSignature(g);
    }
    
    public void move() {
        // Update difficulty animation time (not during rank screen to avoid flickering)
        if (currentState != GameState.RANK) {
            difficultyAnimationTime += 0.1;
        }
        
        // Handle transition animation
        if (currentState == GameState.TRANSITIONING) {
            // Check if this is a pause transition
            if (isTransitioningToPause) {
                updatePauseTransition();
                return; // Don't process other game logic during pause transition
            }
            
            // Check if this is a resume transition
            if (isTransitioningFromPause) {
                updateResumeTransition();
                return; // Don't process other game logic during resume transition
            }
            
            // Check if this is a rank-to-home transition
            if (isRankToHomeTransition) {
                updateRankToHomeTransition();
                return; // Don't process other game logic during rank-to-home transition
            }
            
            // Handle normal menu/game transitions
            transitionProgress += 0.02; // Adjust speed as needed
            
            if (transitionProgress >= 1.0) {
                transitionProgress = 1.0;
                isTransitioning = false;
                currentState = transitionTarget;
            }
            return;
        }
        
        // Handle home to themes transition
        if (isHomeToThemesTransition) {
            updateHomeToThemesTransition();
            return;
        }
        
        // Handle home to paddle transition
        if (isHomeToPaddleTransition) {
            updateHomeToPaddleTransition();
            return;
        }
        
        // Handle home to settings transition
        if (isHomeToSettingsTransition) {
            updateHomeToSettingsTransition();
            return;
        }
        
        // Handle settings to home transition
        if (isSettingsToHomeTransition) {
            updateSettingsToHomeTransition();
            return;
        }
        
        
        // Handle themes to home transition (inverse)
        if (isThemesToHomeTransition) {
            updateThemesToHomeTransition();
            return;
        }
        
        // Handle paddle to home transition (inverse)
        if (isPaddleToHomeTransition) {
            updatePaddleToHomeTransition();
            return;
        }
        
        
        // Update menu ball animation
        if (currentState == GameState.MENU) {
            updateMenuBall();
            updateParticles(); // Aggiorna particelle di sfondo nel menu
            return;
        }
        
        // Update first access carousel and particles
        if (currentState == GameState.FIRST_ACCESS) {
            updateCarousel();
            updateParticles(); // Aggiorna particelle di sfondo
            return;
        }
        
        // Update demo transitions and paddle physics
        if (currentState == GameState.SETTINGS && isTransitioningToDemo) {
            demoTransitionProgress += 0.03; // Transition speed
            if (demoTransitionProgress >= 1.0) {
                demoTransitionProgress = 1.0;
                isTransitioningToDemo = false;
            }
        }
        
        // Update reverse transition from demo to settings
        if (currentState == GameState.SETTINGS && isTransitioningFromDemo) {
            demoTransitionProgress -= 0.03; // Reverse transition speed
            if (demoTransitionProgress <= 0.0) {
                demoTransitionProgress = 0.0;
                isTransitioningFromDemo = false;
            }
        }
        
        // Update transition from demo to menu
        if (isTransitioningDemoToMenu) {
            demoToMenuProgress += 0.02; // Slower transition for smoother effect
            if (demoToMenuProgress >= 1.0) {
                demoToMenuProgress = 1.0;
                isTransitioningDemoToMenu = false;
                currentState = GameState.MENU;
                isDemoMode = false;
                
                // Initialize menu ball for smooth transition
                menuBallX = BOARD_WIDTH / 2 - menuBallSize / 2;
                menuBallY = BOARD_HEIGHT / 2 - menuBallSize / 2;
                double initialSpeed = 4.5 * Math.min(scaleX, scaleY);
                menuBallVX = (Math.random() > 0.5) ? initialSpeed : -initialSpeed;
                menuBallVY = initialSpeed;
            }
        }
        
        // Update rank screen animation
        if (showRankScreen) {
            // Phase 1: Paddle transition from game position to rank position
            if (!rankPaddleTransitionComplete) {
                rankPaddleProgress += 0.025; // Velocità transizione paddle
                if (rankPaddleProgress >= 1.0) {
                    rankPaddleProgress = 1.0;
                    rankPaddleTransitionComplete = true;
                    rankTextTransitionStarted = true;
                }
            }
            
            // Phase 2: Text enters from right (only after paddle transition is complete)
            if (rankTextTransitionStarted && rankPaddleTransitionComplete) {
                rankTextProgress += 0.035; // Velocità entrata testo
                if (rankTextProgress >= 1.0) {
                    rankTextProgress = 1.0;
                }
            }
            
            // Phase 3: Scrolling text drops from top (after paddle transition is complete)
            if (rankPaddleTransitionComplete && !scrollingTextStarted) {
                scrollingTextStarted = true;
            }
            
            if (scrollingTextStarted) {
                if (!scrollingTextEntryComplete) {
                    // Drop animation from top
                    scrollingTextDropProgress += 0.04; // Velocità caduta dall'alto
                    if (scrollingTextDropProgress >= 1.0) {
                        scrollingTextDropProgress = 1.0;
                        scrollingTextEntryComplete = true;
                    }
                }
                // No more complex animations - just show difficulty text normally
            }
        }
        
        // Update checkerboard animation for settings background
        if (currentState == GameState.SETTINGS) {
            // Calculate dynamic speed based on current settings values
            double baseSpeed = calculateBackgroundSpeedFromSetting();
            checkerboardOffset += baseSpeed; // Remove scale factor for consistent speed
            
            // Scale tile size and reset point based on window dimensions
            double scaledTileSize = 40.0 * Math.min(scaleX, scaleY);
            if (checkerboardOffset >= scaledTileSize) {
                checkerboardOffset = 0.0;
            }
            
            // Update paddle width animations for settings
            double animationSpeed = 0.08; // Animation speed (higher = faster)
            
            // Left paddle animation
            double targetLeftProgress = inCategoryColumn ? 1.0 : 0.0;
            if (leftPaddleWidthProgress < targetLeftProgress) {
                leftPaddleWidthProgress = Math.min(targetLeftProgress, leftPaddleWidthProgress + animationSpeed);
            } else if (leftPaddleWidthProgress > targetLeftProgress) {
                leftPaddleWidthProgress = Math.max(targetLeftProgress, leftPaddleWidthProgress - animationSpeed);
            }
            
            // Right paddle animation
            double targetRightProgress = !inCategoryColumn ? 1.0 : 0.0;
            if (rightPaddleWidthProgress < targetRightProgress) {
                rightPaddleWidthProgress = Math.min(targetRightProgress, rightPaddleWidthProgress + animationSpeed);
            } else if (rightPaddleWidthProgress > targetRightProgress) {
                rightPaddleWidthProgress = Math.max(targetRightProgress, rightPaddleWidthProgress - animationSpeed);
            }
            
            // Set constant glow intensity (no pulsing animation)
            glowIntensity = 0.6; // Fixed intensity level for stable lighting
            
            // Update category animation progress
            updateCategoryAnimations();
        }
        
        // Update demo paddle movement - replicate exact game paddle movement
        if (currentState == GameState.SETTINGS && isDemoMode) {
            // Calculate paddle speed exactly like in the game
            double baseSpeed = 4.0; // Lenta
            if (paddleSpeedSetting == 1) baseSpeed = 6.0; // Media  
            else if (paddleSpeedSetting == 2) baseSpeed = 8.0; // Veloce
            int paddleSpeed = Math.max(3, (int)(baseSpeed * scaleY));
            
            // Move demo paddle exactly like game paddles
            // Use exact same dimensions and limits as game paddle
            if (demoPaddleUpPressed && demoPaddleY > 0) {
                demoPaddleY -= paddleSpeed;
            }
            if (demoPaddleDownPressed && demoPaddleY < BOARD_HEIGHT - PADDLE_HEIGHT) {
                demoPaddleY += paddleSpeed;
            }
            
            // Keep paddle within bounds (exact same as game paddle)
            demoPaddleY = Math.max(0, Math.min(demoPaddleY, BOARD_HEIGHT - PADDLE_HEIGHT));
            
            // Update demo ball (same physics as game)
            updateDemoBall();
            
            // Update red paddle AI with selected difficulty
            updateDemoAI();
        }
        
        // Handle paddle movement in paddle selection screen - EXACT same as game
        if (currentState == GameState.PADDLE_SELECTION || currentState == GameState.RIGHT_PADDLE_SELECTION) {
            // EXACT same paddle speed calculation as in game
            double baseSpeed = 4.0; // Lenta
            if (paddleSpeedSetting == 1) baseSpeed = 6.0; // Media  
            else if (paddleSpeedSetting == 2) baseSpeed = 8.0; // Veloce
            
            int paddleSpeed = Math.max(3, (int)(baseSpeed * scaleY)); // Minimum 3px, scaled with screen height
            
            // Move preview paddle with EXACT same logic as game paddle
            if (paddleSelectionUpPressed && previewPaddleY > 0) {
                previewPaddleY -= paddleSpeed;
            }
            if (paddleSelectionDownPressed && previewPaddleY < BOARD_HEIGHT - PADDLE_HEIGHT) {
                previewPaddleY += paddleSpeed;
            }
            return;
        }
        
        
        // Update pause line animation when paused
        if (currentState == GameState.PAUSED) {
            updatePauseLineAnimation();
            return; // Don't process game logic when paused
        }
        
        if (currentState != GameState.PLAYING && currentState != GameState.SINGLE_PLAYER) return;
        
        // Calculate scaled paddle speed based on screen height and settings
        double baseSpeed = 4.0; // Lenta
        if (paddleSpeedSetting == 1) baseSpeed = 6.0; // Media  
        else if (paddleSpeedSetting == 2) baseSpeed = 8.0; // Veloce
        
        int paddleSpeed = Math.max(3, (int)(baseSpeed * scaleY)); // Minimum 3px, scaled with screen height
        
        // Store previous paddle positions for physics calculations
        prevPaddle1Y = paddle1Y;
        prevPaddle2Y = paddle2Y;
        
        // Move paddles
        if (wPressed && paddle1Y > 0) {
            paddle1Y -= paddleSpeed;
        }
        if (sPressed && paddle1Y < BOARD_HEIGHT - PADDLE_HEIGHT) {
            paddle1Y += paddleSpeed;
        }
        
        // AI or human player 2
        if (currentState == GameState.SINGLE_PLAYER) {
            updateAI();
        } else {
            if (upPressed && paddle2Y > 0) {
                paddle2Y -= paddleSpeed;
            }
            if (downPressed && paddle2Y < BOARD_HEIGHT - PADDLE_HEIGHT) {
                paddle2Y += paddleSpeed;
            }
        }
        
        // Move ball (velocities are already scaled in updateScaling())
        ballX += ballVX;
        ballY += ballVY;
        
        // Ball collision with top and bottom walls
        if (ballY <= 0 || ballY >= BOARD_HEIGHT - BALL_SIZE) {
            ballVY = -ballVY;
            // Ensure ball stays within bounds
            if (ballY <= 0) ballY = 0;
            if (ballY >= BOARD_HEIGHT - BALL_SIZE) ballY = BOARD_HEIGHT - BALL_SIZE;
            lastBallDirectionChange = System.currentTimeMillis(); // Track direction change for AI
            playWallHitSound();
            createParticles((int)ballX + BALL_SIZE/2, (int)ballY + BALL_SIZE/2, Color.WHITE, 8);
            
            // Reset fire ball system on wall hit
            resetFireBallSystem();
        }
        
        // Ball collision with paddles (scaled positions)
        int leftPaddleX = (int)(20 * scaleX);
        int rightPaddleX = BOARD_WIDTH - leftPaddleX - PADDLE_WIDTH;
        
        if (ballX <= leftPaddleX + PADDLE_WIDTH && ballX + BALL_SIZE >= leftPaddleX && ballY + BALL_SIZE >= paddle1Y && ballY <= paddle1Y + PADDLE_HEIGHT && ballVX < 0) {
            // Calculate realistic physics
            applyPaddlePhysics(1, leftPaddleX + PADDLE_WIDTH, paddle1Y);
            
            rallies++;
            
            // Handle combo based on game mode
            if (currentState == GameState.SINGLE_PLAYER) {
                // Single player mode - use original combo system
                comboCount++; // Increment combo on successful paddle hit
                if (comboCount > maxCombo) maxCombo = comboCount; // Track best combo
                triggerComboIncrement(); // Trigger visual effects
            } else if (currentState == GameState.PLAYING) {
                // Two players mode - use player1 combo system
                player1ComboCount++; // Increment Player 1 combo
                if (player1ComboCount > player1MaxCombo) player1MaxCombo = player1ComboCount; // Track best combo
                triggerComboIncrement(); // Trigger visual effects
            }
            
            playPaddleHitSound();
            
            // Fire ball system - increment consecutive paddle bounces
            incrementFireBallSystem();
            
            createParticles((int)ballX, (int)ballY + BALL_SIZE/2, cachedLeftGlowColor, 10);
            addScreenShake(3);
        }
        
        // Use smooth AI position for collision detection in single player mode
        int effectivePaddle2Y = currentState == GameState.SINGLE_PLAYER ? (int)aiPaddleY : paddle2Y;
        if (ballX + BALL_SIZE >= rightPaddleX && ballX <= rightPaddleX + PADDLE_WIDTH && ballY + BALL_SIZE >= effectivePaddle2Y && ballY <= effectivePaddle2Y + PADDLE_HEIGHT && ballVX > 0) {
            // Calculate realistic physics
            applyPaddlePhysics(2, rightPaddleX, effectivePaddle2Y);
            
            rallies++;
            
            // Right paddle combo ONLY in multiplayer mode (TWO_PLAYERS)
            if (currentState == GameState.PLAYING) { // This is multiplayer mode
                player2ComboCount++; // Increment Player 2 combo
                if (player2ComboCount > player2MaxCombo) player2MaxCombo = player2ComboCount; // Track best combo
                triggerRightComboIncrement(); // Trigger visual effects
            }
            // In single player mode, no combo for AI paddle
            
            playPaddleHitSound();
            
            // Fire ball system - increment consecutive paddle bounces
            incrementFireBallSystem();
            
            createParticles((int)ballX + BALL_SIZE, (int)ballY + BALL_SIZE/2, cachedRightGlowColor, 10);
            addScreenShake(3);
        }
        
        // Ball goes off screen - scoring
        if (ballX < 0) {
            // Check fire ball bonus BEFORE resetting system
            int pointsToAdd = doublePointsActive ? 2 : 1;
            score2 += pointsToAdd;
            
            // Show fire ball bonus message if active
            if (doublePointsActive) {
                System.out.println("FIRE BALL BONUS! Punti doppi assegnati: " + pointsToAdd);
            }
            
            // Update AI streak tracking
            if (lastPointWinner == 2) {
                aiWinStreak++;
            } else {
                aiWinStreak = 1;
                playerWinStreak = 0;
            }
            lastPointWinner = 2;
            
            playScoreSound();
            createParticles(BOARD_WIDTH / 2, BOARD_HEIGHT / 2, Color.YELLOW, 20);
            addScreenShake(8);
            // Reset combo only when ball goes out on player side (left)
            if (currentState == GameState.SINGLE_PLAYER) {
                comboCount = 0; // Reset single player combo
            } else if (currentState == GameState.PLAYING) {
                player1ComboCount = 0; // Reset Player 1 combo in two players mode
            }
            // Track player miss for consistency factor
            consecutiveMissedShots++;
            
            checkWinCondition();
            // Reset fire ball system AFTER scoring
            resetFireBallSystem();
            resetBall();
        }
        if (ballX > BOARD_WIDTH) {
            // Check fire ball bonus BEFORE resetting system
            int pointsToAdd = doublePointsActive ? 2 : 1;
            score1 += pointsToAdd;
            
            // Show fire ball bonus message if active
            if (doublePointsActive) {
                System.out.println("FIRE BALL BONUS! Punti doppi assegnati: " + pointsToAdd);
            }
            
            // Update player streak tracking
            if (lastPointWinner == 1) {
                playerWinStreak++;
            } else {
                playerWinStreak = 1;
                aiWinStreak = 0;
            }
            lastPointWinner = 1;
            
            playScoreSound();
            createParticles(BOARD_WIDTH / 2, BOARD_HEIGHT / 2, Color.YELLOW, 20);
            addScreenShake(8);
            // Don't reset left combo when player scores (ball exits right side)
            // But reset right combo when ball exits their side
            if (currentState == GameState.PLAYING) {
                player2ComboCount = 0; // Reset Player 2 combo in two players mode
            }
            // Reset missed shots counter when player scores
            consecutiveMissedShots = 0;
            
            checkWinCondition();
            // Reset fire ball system AFTER scoring
            resetFireBallSystem();
            resetBall();
        }
        
        // Update visual effects
        updateParticles();
        updateScreenShake();
        updateBallTrail();
        updatePaddleGlow();
        updateComboEffects();
        updateRightComboEffects();
    }
    

    private void updateCategoryAnimations() {
        // Animate category positions smoothly
        double animationSpeed = 0.08; // Smooth animation speed
        
        for (int i = 0; i < categoryAnimationProgress.length; i++) {
            if (i == selectedCategory) {
                // Selected category moves towards fully visible (1.0)
                if (categoryAnimationProgress[i] < 1.0) {
                    categoryAnimationProgress[i] += animationSpeed;
                    if (categoryAnimationProgress[i] > 1.0) {
                        categoryAnimationProgress[i] = 1.0;
                    }
                }
            } else {
                // Non-selected categories move towards hidden (0.0)
                if (categoryAnimationProgress[i] > 0.0) {
                    categoryAnimationProgress[i] -= animationSpeed;
                    if (categoryAnimationProgress[i] < 0.0) {
                        categoryAnimationProgress[i] = 0.0;
                    }
                }
            }
        }
    }
    
    private void updateMenuBall() {
        // Move menu ball
        menuBallX += menuBallVX;
        menuBallY += menuBallVY;
        
        // Aggiungi gravità per far cadere la palla gradualmente
        double gravity = 0.02 * scaleY; // Gravità scalata con le dimensioni dello schermo
        menuBallVY += gravity;
        
        // Scale velocity based on screen size - velocità aumentata
        double baseSpeed = 4.5 * Math.min(scaleX, scaleY); // Aumentata da 3 a 4.5
        
        // Get paddle properties (same as in drawMenuPaddles)
        int widePaddleWidth = (int)(250 * Math.min(scaleX, scaleY));
        int paddleHeight = (int)(menuPaddleHeight * 1.8);
        int paddleYOffset = (int)(-paddleHeight * 0.2);
        
        // Paddle centers
        int leftCenterX = 0;
        int leftCenterY = paddleYOffset + paddleHeight / 2;
        int rightCenterX = BOARD_WIDTH;
        int rightCenterY = paddleYOffset + paddleHeight / 2;
        
        // Ball properties
        double ballCenterX = menuBallX + menuBallSize / 2;
        double ballCenterY = menuBallY + menuBallSize / 2;
        double ballRadius = menuBallSize / 2;
        
        // La palla passa attraverso i muri e riappare dall'altro lato
        if (menuBallY + menuBallSize < 0) {
            menuBallY = BOARD_HEIGHT;
        }
        
        if (menuBallY > BOARD_HEIGHT) {
            menuBallY = -menuBallSize;
        }
        
        // Left paddle collision - traiettoria ondulata verso il paddle destro
        if (isCollidingWithRotatedPaddle(ballCenterX, ballCenterY, ballRadius, 
                                        leftCenterX, leftCenterY, widePaddleWidth, paddleHeight, -25) && 
            menuBallVX < 0) {
            menuBallVX = Math.abs(menuBallVX);
            
            // Calcola direzione base verso il paddle destro con bias verso l'alto
            double targetX = rightCenterX;
            double targetY = rightCenterY - BOARD_HEIGHT * 0.3; // Punta più in alto
            double directionX = targetX - ballCenterX;
            double directionY = targetY - ballCenterY;
            double distance = Math.sqrt(directionX * directionX + directionY * directionY);
            
            // Aggiungi variazione casuale per ondulazione
            double randomAngle = (Math.random() - 0.5) * Math.PI * 0.4; // ±36 gradi
            
            // Normalizza e scala la velocità
            if (distance > 0) {
                double normalizedX = directionX / distance;
                double normalizedY = directionY / distance;
                
                // Applica rotazione per ondulazione
                double rotatedX = normalizedX * Math.cos(randomAngle) - normalizedY * Math.sin(randomAngle);
                double rotatedY = normalizedX * Math.sin(randomAngle) + normalizedY * Math.cos(randomAngle);
                
                menuBallVX = rotatedX * baseSpeed;
                menuBallVY = rotatedY * baseSpeed;
            }
            
            menuBallX += 5; // Move right to prevent sticking
        }
        
        // Right paddle collision - traiettoria ondulata verso il paddle sinistro
        if (isCollidingWithRotatedPaddle(ballCenterX, ballCenterY, ballRadius, 
                                        rightCenterX, rightCenterY, widePaddleWidth, paddleHeight, 25) && 
            menuBallVX > 0) {
            menuBallVX = -Math.abs(menuBallVX);
            
            // Calcola direzione base verso il paddle sinistro con bias verso l'alto
            double targetX = leftCenterX;
            double targetY = leftCenterY - BOARD_HEIGHT * 0.3; // Punta più in alto
            double directionX = targetX - ballCenterX;
            double directionY = targetY - ballCenterY;
            double distance = Math.sqrt(directionX * directionX + directionY * directionY);
            
            // Aggiungi variazione casuale per ondulazione
            double randomAngle = (Math.random() - 0.5) * Math.PI * 0.4; // ±36 gradi
            
            // Normalizza e scala la velocità
            if (distance > 0) {
                double normalizedX = directionX / distance;
                double normalizedY = directionY / distance;
                
                // Applica rotazione per ondulazione
                double rotatedX = normalizedX * Math.cos(randomAngle) - normalizedY * Math.sin(randomAngle);
                double rotatedY = normalizedX * Math.sin(randomAngle) + normalizedY * Math.cos(randomAngle);
                
                menuBallVX = rotatedX * baseSpeed;
                menuBallVY = rotatedY * baseSpeed;
            }
            
            menuBallX -= 5; // Move left to prevent sticking
        }
        
        // Keep ball velocity reasonable - limiti aumentati per velocità maggiore
        menuBallVX = Math.max(-baseSpeed * 2.5, Math.min(baseSpeed * 2.5, menuBallVX));
        menuBallVY = Math.max(-baseSpeed * 2.5, Math.min(baseSpeed * 2.5, menuBallVY));
        
        // Update second ball if active
        if (menuBall2Active) {
            // Move second ball
            menuBall2X += menuBall2VX;
            menuBall2Y += menuBall2VY;
            
            // Normal ball physics - identical to first ball
            menuBall2VY += gravity;
            
            // Ball properties
            double ball2CenterX = menuBall2X + menuBallSize / 2;
            double ball2CenterY = menuBall2Y + menuBallSize / 2;
            double ball2Radius = menuBallSize / 2;
            
            // La palla passa attraverso i muri e riappare dall'altro lato
            if (menuBall2Y + menuBallSize < 0) {
                menuBall2Y = BOARD_HEIGHT;
            }
            
            if (menuBall2Y > BOARD_HEIGHT) {
                if (menuBall2Falling) {
                    // If ball is falling (player moved away from Two Players), deactivate it
                    menuBall2Active = false;
                    menuBall2Falling = false;
                } else {
                    // Normal cycling behavior when Two Players is selected
                    menuBall2Y = -menuBallSize;
                }
            }
            
            // Left paddle collision - different behavior based on falling state
            boolean leftCollision = isCollidingWithRotatedPaddle(ball2CenterX, ball2CenterY, ball2Radius, 
                                            leftCenterX, leftCenterY, widePaddleWidth, paddleHeight, -25);
            if (leftCollision) {
                if (menuBall2Falling) {
                    // Falling mode - slide down the paddle (always handle in falling mode)
                    handleFallingBallPaddleCollision(ball2CenterX, ball2CenterY, leftCenterX, leftCenterY, widePaddleWidth, paddleHeight, -25, true);
                } else if (menuBall2VX < 0) {
                    // Normal mode - bounce toward right paddle
                    menuBall2VX = Math.abs(menuBall2VX);
                    
                    // Calcola direzione base verso il paddle destro con bias verso l'alto
                    double targetX = rightCenterX;
                    double targetY = rightCenterY - BOARD_HEIGHT * 0.3; // Punta più in alto
                    double directionX = targetX - ball2CenterX;
                    double directionY = targetY - ball2CenterY;
                    double distance = Math.sqrt(directionX * directionX + directionY * directionY);
                    
                    // Aggiungi variazione casuale per ondulazione
                    double waveAmplitude = 0.4;
                    double randomAngle = (Math.random() - 0.5) * waveAmplitude;
                    
                    // Ruota la direzione per l'ondulazione
                    double cosWave = Math.cos(randomAngle);
                    double sinWave = Math.sin(randomAngle);
                    double rotatedX = (directionX * cosWave - directionY * sinWave) / distance;
                    double rotatedY = (directionX * sinWave + directionY * cosWave) / distance;
                    
                    // Applica la nuova velocità
                    if (distance > 0) {
                        menuBall2VX = rotatedX * baseSpeed;
                        menuBall2VY = rotatedY * baseSpeed;
                    }
                    
                    menuBall2X += 5; // Move right to prevent sticking
                }
            }
            
            // Right paddle collision - different behavior based on falling state
            boolean rightCollision = isCollidingWithRotatedPaddle(ball2CenterX, ball2CenterY, ball2Radius, 
                                            rightCenterX, rightCenterY, widePaddleWidth, paddleHeight, 25);
            if (rightCollision) {
                if (menuBall2Falling) {
                    // Falling mode - slide down the paddle (always handle in falling mode)
                    handleFallingBallPaddleCollision(ball2CenterX, ball2CenterY, rightCenterX, rightCenterY, widePaddleWidth, paddleHeight, 25, false);
                } else if (menuBall2VX > 0) {
                    // Normal mode - bounce toward left paddle
                    menuBall2VX = -Math.abs(menuBall2VX);
                    
                    // Calcola direzione base verso il paddle sinistro con bias verso l'alto
                    double targetX = leftCenterX;
                    double targetY = leftCenterY - BOARD_HEIGHT * 0.3; // Punta più in alto
                    double directionX = targetX - ball2CenterX;
                    double directionY = targetY - ball2CenterY;
                    double distance = Math.sqrt(directionX * directionX + directionY * directionY);
                    
                    // Aggiungi variazione casuale per ondulazione
                    double waveAmplitude = 0.4;
                    double randomAngle = (Math.random() - 0.5) * waveAmplitude;
                    
                    // Ruota la direzione per l'ondulazione
                    double cosWave = Math.cos(randomAngle);
                    double sinWave = Math.sin(randomAngle);
                    double rotatedX = (directionX * cosWave - directionY * sinWave) / distance;
                    double rotatedY = (directionX * sinWave + directionY * cosWave) / distance;
                    
                    // Applica la nuova velocità
                    if (distance > 0) {
                        menuBall2VX = rotatedX * baseSpeed;
                        menuBall2VY = rotatedY * baseSpeed;
                    }
                    
                    menuBall2X -= 5; // Move left to prevent sticking
                }
            }
            
            // Keep ball velocity reasonable - limiti identici alla prima palla
            menuBall2VX = Math.max(-baseSpeed * 2.5, Math.min(baseSpeed * 2.5, menuBall2VX));
            menuBall2VY = Math.max(-baseSpeed * 2.5, Math.min(baseSpeed * 2.5, menuBall2VY));
        }
    }
    
    private void startHomeToThemesTransition() {
        isHomeToThemesTransition = true;
        homeToThemesProgress = 0.0;
        textFadeProgress = 1.0;
        paddleExitProgress = 0.0;
        themesPanelProgress = 0.0;
    }
    
    private void startHomeToPaddleTransition(boolean isLeftPaddle) {
        isHomeToPaddleTransition = true;
        homeToPaddleProgress = 0.0;
        paddleTextFadeProgress = 1.0;
        paddlePanelProgress = 0.0;
        isLeftPaddleTransition = isLeftPaddle;
        
        // Center the paddle vertically on screen
        previewPaddleY = (BOARD_HEIGHT - PADDLE_HEIGHT) / 2;
    }
    
    private void startHomeToSettingsTransition() {
        System.out.println("DEBUG: Starting Home to Settings transition!");
        isHomeToSettingsTransition = true;
        homeToSettingsProgress = 0.0;
        paddleTranslationProgress = 0.0;
        columnsTranslationProgress = 0.0;
        checkerboardAppearProgress = 0.0;
        checkerboardAnimationProgress = 0.0;
        
        // Reset paddle selection state to ensure clean transition
        leftPaddleSelected = false;
        rightPaddleSelected = false;
        
        // Initialize settings state for proper paddle expansion during transition
        inCategoryColumn = true; // Start in category column (left paddle expanded)
        selectedCategory = 0;
        selectedCategorySetting = 0;
        leftPaddleWidthProgress = 0.0; // Will animate to 1.0 during transition
        rightPaddleWidthProgress = 0.0;
    }
    
    private void startSettingsToHomeTransition() {
        System.out.println("DEBUG: Starting Settings to Home transition!");
        isSettingsToHomeTransition = true;
        settingsToHomeProgress = 0.0;
        settingsPaddleTranslationProgress = 0.0;
        settingsColumnsTranslationProgress = 0.0;
        settingsCheckerboardDisappearProgress = 0.0;
        settingsCheckerboardAnimationProgress = 0.0;
        
        // Keep current settings state - we're transitioning FROM settings
        // No need to reset selection states, we're leaving them
        
        // Keep current paddle expansion state and animate it back to 0
        // leftPaddleWidthProgress will animate from current value to 0.0
        // rightPaddleWidthProgress will animate from current value to 0.0
    }
    
    private void updateSettingsToHomeTransition() {
        settingsToHomeProgress += 0.02; // Same speed as home to settings (3 seconds)
        
        // Checkerboard disappear progress (first 30% of transition - background disappears from top to bottom)
        if (settingsToHomeProgress <= 0.3) {
            settingsCheckerboardDisappearProgress = settingsToHomeProgress / 0.3; // 0.0 to 1.0
        } else {
            settingsCheckerboardDisappearProgress = 1.0;
        }
        
        // Checkerboard animation progress (after 30% - background continues moving while disappearing)
        if (settingsToHomeProgress > 0.3) {
            settingsCheckerboardAnimationProgress = (settingsToHomeProgress - 0.3) / 0.7; // 0.0 to 1.0
        }
        
        // Paddle translation progress (paddles move back to home position)
        settingsPaddleTranslationProgress = settingsToHomeProgress;
        
        // Columns translation progress (settings UI slides out)
        if (settingsToHomeProgress > 0.4) {
            // Start sliding columns out after 40% of transition
            settingsColumnsTranslationProgress = (settingsToHomeProgress - 0.4) / 0.6; // 0.0 to 1.0
            settingsColumnsTranslationProgress = Math.min(1.0, settingsColumnsTranslationProgress);
        }
        
        // Animate paddle contraction (from current state back to 0.0)
        if (settingsToHomeProgress > 0.5) {
            // Start contracting paddles after halfway point
            double contractionProgress = (settingsToHomeProgress - 0.5) * 2.0; // 0.0 to 1.0
            leftPaddleWidthProgress = Math.max(0.0, 1.0 - contractionProgress); // Contract from 1.0 to 0.0
            rightPaddleWidthProgress = Math.max(0.0, rightPaddleWidthProgress * (1.0 - contractionProgress)); // Contract right paddle too
        }
        
        System.out.println("DEBUG: Settings to Home - Progress: " + settingsToHomeProgress + 
                          ", Translation: " + settingsPaddleTranslationProgress + 
                          ", Columns: " + settingsColumnsTranslationProgress +
                          ", LeftContraction: " + leftPaddleWidthProgress);
        
        // Complete transition
        if (settingsToHomeProgress >= 1.0) {
            settingsToHomeProgress = 1.0;
            settingsPaddleTranslationProgress = 1.0;
            settingsColumnsTranslationProgress = 1.0;
            settingsCheckerboardDisappearProgress = 1.0;
            settingsCheckerboardAnimationProgress = 1.0;
            leftPaddleWidthProgress = 0.0; // Ensure full contraction
            rightPaddleWidthProgress = 0.0;
            isSettingsToHomeTransition = false;
            setState(GameState.MENU); // Finally go to menu
        }
    }
    
    private void updateHomeToPaddleTransition() {
        homeToPaddleProgress += 0.04; // Slightly faster than themes transition
        
        // Phase 1: Text fade out (first 30% of transition)
        if (homeToPaddleProgress <= 0.3) {
            paddleTextFadeProgress = 1.0 - (homeToPaddleProgress / 0.3);
        } else {
            paddleTextFadeProgress = 0.0;
        }
        
        // Phase 2: Panel slides in from side (30% to 100% of transition)
        if (homeToPaddleProgress > 0.3) {
            paddlePanelProgress = (homeToPaddleProgress - 0.3) / 0.7;
            paddlePanelProgress = Math.min(paddlePanelProgress, 1.0);
        }
        
        // Complete transition
        if (homeToPaddleProgress >= 1.0) {
            homeToPaddleProgress = 1.0;
            isHomeToPaddleTransition = false;
            
            // Set final state based on which paddle was selected
            if (isLeftPaddleTransition) {
                currentState = GameState.PADDLE_SELECTION;
            } else {
                currentState = GameState.RIGHT_PADDLE_SELECTION;
            }
            
            // Reset transition variables
            paddleTextFadeProgress = 1.0;
            paddlePanelProgress = 0.0;
        }
    }
    
    private void updateHomeToSettingsTransition() {
        homeToSettingsProgress += 0.02; // Transition speed (3 seconds)
        
        // Checkerboard appear progress (first 30% of transition - background appears from bottom to top)
        if (homeToSettingsProgress <= 0.3) {
            checkerboardAppearProgress = homeToSettingsProgress / 0.3; // 0.0 to 1.0
        } else {
            checkerboardAppearProgress = 1.0;
        }
        
        // Checkerboard animation progress (after 30% - background starts moving)
        if (homeToSettingsProgress > 0.3) {
            checkerboardAnimationProgress = (homeToSettingsProgress - 0.3) / 0.7; // 0.0 to 1.0
        }
        
        // Paddle translation progress (paddles move to settings position)
        paddleTranslationProgress = homeToSettingsProgress;
        
        // Columns translation progress (settings UI slides in)
        if (homeToSettingsProgress > 0.4) {
            // Start sliding columns after 40% of transition
            columnsTranslationProgress = (homeToSettingsProgress - 0.4) / 0.6; // 0.0 to 1.0
            columnsTranslationProgress = Math.min(1.0, columnsTranslationProgress);
        }
        
        // Animate left paddle expansion (should reach 1.0 since we're in category column)
        if (homeToSettingsProgress > 0.5) {
            // Start expanding left paddle after halfway point
            double expansionProgress = (homeToSettingsProgress - 0.5) * 2.0; // 0.0 to 1.0
            leftPaddleWidthProgress = Math.min(1.0, expansionProgress);
        }
        
        System.out.println("DEBUG: Home to Settings - Progress: " + homeToSettingsProgress + 
                          ", Translation: " + paddleTranslationProgress + 
                          ", Columns: " + columnsTranslationProgress +
                          ", LeftExpansion: " + leftPaddleWidthProgress);
        
        // Complete transition
        if (homeToSettingsProgress >= 1.0) {
            homeToSettingsProgress = 1.0;
            paddleTranslationProgress = 1.0;
            columnsTranslationProgress = 1.0;
            checkerboardAppearProgress = 1.0;
            checkerboardAnimationProgress = 1.0;
            leftPaddleWidthProgress = 1.0; // Ensure full expansion
            isHomeToSettingsTransition = false;
            currentState = GameState.SETTINGS;
            System.out.println("DEBUG: Transition to SETTINGS completed!");
        }
    }
    
    
    private void updateHomeToThemesTransition() {
        homeToThemesProgress += 0.03; // Overall transition speed
        
        // Phase 1: Text fade out (first 30% of transition)
        if (homeToThemesProgress <= 0.3) {
            textFadeProgress = 1.0 - (homeToThemesProgress / 0.3);
        } else {
            textFadeProgress = 0.0;
        }
        
        // Phase 2: Paddles exit (30% to 70% of transition)
        if (homeToThemesProgress > 0.3 && homeToThemesProgress <= 0.7) {
            paddleExitProgress = (homeToThemesProgress - 0.3) / 0.4;
        } else if (homeToThemesProgress > 0.7) {
            paddleExitProgress = 1.0;
        }
        
        // Phase 3: Themes panel appears from bottom (last 40% of transition)
        if (homeToThemesProgress > 0.6) {
            themesPanelProgress = (homeToThemesProgress - 0.6) / 0.4;
            themesPanelProgress = Math.min(themesPanelProgress, 1.0);
        }
        
        // Complete transition
        if (homeToThemesProgress >= 1.0) {
            homeToThemesProgress = 1.0;
            isHomeToThemesTransition = false;
            currentState = GameState.BACKGROUND_SELECTION;
            
            // Reset transition variables
            textFadeProgress = 1.0;
            paddleExitProgress = 0.0;
            themesPanelProgress = 0.0;
        }
    }
    
    private void drawHomeToPaddleTransition(Graphics2D g) {
        // Draw the background
        drawMenuBackground(g);
        
        // Draw the selected paddle in its final position (doesn't move during transition)
        if (isLeftPaddleTransition) {
            drawPaddleTransitionLeft(g);
        } else {
            drawPaddleTransitionRight(g);
        }
        
        // Draw menu text fading out
        if (paddleTextFadeProgress > 0) {
            drawMenuTextWithFade(g, paddleTextFadeProgress);
        }
        
        // Draw the themes panel sliding in from the side
        if (paddlePanelProgress > 0) {
            if (isLeftPaddleTransition) {
                drawLeftPaddleThemesPanelSlideIn(g, paddlePanelProgress);
            } else {
                drawRightPaddleThemesPanelSlideIn(g, paddlePanelProgress);
            }
        }
    }
    
    private void drawPaddleTransitionLeft(Graphics2D g) {
        // Draw the left paddle in its EXACT menu position (inclinato, no movement during transition)
        // Save original transform
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        // Position paddle exactly like in drawMenuPaddles (left paddle inclinato)
        int widePaddleWidth = (int)(250 * Math.min(scaleX, scaleY));
        int paddleHeight = (int)(menuPaddleHeight * 1.8);
        int paddleYOffset = (int)(-paddleHeight * 0.2);
        
        // Left paddle center - exactly like in drawMenuPaddles
        int leftCenterX = 0; // Completely attached to left edge
        int leftCenterY = paddleYOffset + paddleHeight / 2;
        
        g.translate(leftCenterX, leftCenterY);
        g.rotate(Math.toRadians(-25)); // Same rotation as left menu paddle
        
        // Draw paddle with selected theme if available
        if (selectedPaddleTheme >= 0 && selectedPaddleTheme < bluePaddleThemeImages.size()) {
            BufferedImage paddleImg = bluePaddleThemeImages.get(selectedPaddleTheme);
            if (paddleImg != null) {
                // Custom theme paddle with rounded corners
                int cornerRadius = Math.max(4, widePaddleWidth / 4);
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, cornerRadius, cornerRadius));
                g.drawImage(paddleImg, -widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, null);
                g.setClip(null);
            } else {
                // Fallback to default gradient
                GradientPaint gradient = new GradientPaint(-widePaddleWidth/2, -paddleHeight/2, new Color(100, 150, 255),
                                                         widePaddleWidth/2, paddleHeight/2, new Color(150, 200, 255));
                g.setPaint(gradient);
                g.fillRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight);
                g.setPaint(null);
            }
        } else {
            // Default gradient paddle
            GradientPaint gradient = new GradientPaint(-widePaddleWidth/2, -paddleHeight/2, new Color(100, 150, 255),
                                                     widePaddleWidth/2, paddleHeight/2, new Color(150, 200, 255));
            g.setPaint(gradient);
            g.fillRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight);
            g.setPaint(null);
        }
        
        // Left paddle border glow (same as in drawMenuPaddles)
        int glowWidth = Math.max(2, (int)(8 * Math.min(scaleX, scaleY)));
        g.setColor(getPaddleGlowColor(true));
        g.fillRect(widePaddleWidth/2, -paddleHeight/2, glowWidth, paddleHeight);
        
        g.setTransform(originalTransform);
    }
    
    private void drawPaddleTransitionRight(Graphics2D g) {
        // Draw the right paddle in its EXACT menu position (inclinato, no movement during transition)
        // Save original transform
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        // Position paddle exactly like in drawMenuPaddles (right paddle inclinato)
        int widePaddleWidth = (int)(250 * Math.min(scaleX, scaleY));
        int paddleHeight = (int)(menuPaddleHeight * 1.8);
        int paddleYOffset = (int)(-paddleHeight * 0.2);
        
        // Right paddle center - exactly like in drawMenuPaddles
        int rightCenterX = BOARD_WIDTH; // Completely attached to right edge
        int rightCenterY = paddleYOffset + paddleHeight / 2;
        
        g.translate(rightCenterX, rightCenterY);
        g.rotate(Math.toRadians(25)); // Same rotation as right menu paddle
        
        // Draw paddle with selected theme if available
        if (selectedRightPaddleTheme >= 0 && selectedRightPaddleTheme < redPaddleThemeImages.size()) {
            BufferedImage paddleImg = redPaddleThemeImages.get(selectedRightPaddleTheme);
            if (paddleImg != null) {
                // Custom theme paddle with rounded corners
                int cornerRadius = Math.max(4, widePaddleWidth / 4);
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, cornerRadius, cornerRadius));
                g.drawImage(paddleImg, -widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, null);
                g.setClip(null);
            } else {
                // Fallback to default gradient
                GradientPaint gradient = new GradientPaint(-widePaddleWidth/2, -paddleHeight/2, new Color(255, 100, 100),
                                                         widePaddleWidth/2, paddleHeight/2, new Color(255, 150, 150));
                g.setPaint(gradient);
                g.fillRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight);
                g.setPaint(null);
            }
        } else {
            // Default gradient paddle
            GradientPaint gradient = new GradientPaint(-widePaddleWidth/2, -paddleHeight/2, new Color(255, 100, 100),
                                                     widePaddleWidth/2, paddleHeight/2, new Color(255, 150, 150));
            g.setPaint(gradient);
            g.fillRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight);
            g.setPaint(null);
        }
        
        // Right paddle border glow (same as in drawMenuPaddles)
        int glowWidth = Math.max(2, (int)(8 * Math.min(scaleX, scaleY)));
        g.setColor(getPaddleGlowColor(false));
        g.fillRect(-widePaddleWidth/2 - glowWidth, -paddleHeight/2, glowWidth, paddleHeight);
        
        g.setTransform(originalTransform);
    }
    
    private void drawMenuTextWithFade(Graphics2D g, double fadeProgress) {
        if (fadeProgress <= 0) return;
        
        // Draw title with fade
        float titleSize = (float)(48 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(titleSize));
        FontMetrics titleFm = g.getFontMetrics();
        String title = "PONG PING";
        int titleX = (BOARD_WIDTH - titleFm.stringWidth(title)) / 2;
        int titleY = (int)(150 * scaleY);
        
        // Title with fade
        Color titleColor = currentTextColors.getOrDefault("menuTitle", Color.WHITE);
        int titleAlpha = (int)(titleColor.getAlpha() * fadeProgress);
        g.setColor(new Color(titleColor.getRed(), titleColor.getGreen(), titleColor.getBlue(), titleAlpha));
        g.drawString(title, titleX, titleY);
        
        // Menu items with fade  
        float menuSize = (float)(24 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(menuSize));
        FontMetrics menuFm = g.getFontMetrics();
        int menuStartY = (int)(300 * scaleY);
        int menuSpacing = (int)(60 * scaleY);
        
        for (int i = 0; i < menuItems.length; i++) {
            Color menuColor = currentTextColors.getOrDefault("menuItems", Color.WHITE);
            int menuAlpha = (int)(menuColor.getAlpha() * fadeProgress);
            g.setColor(new Color(menuColor.getRed(), menuColor.getGreen(), menuColor.getBlue(), menuAlpha));
            g.drawString(menuItems[i], 
                (BOARD_WIDTH - menuFm.stringWidth(menuItems[i])) / 2, 
                menuStartY + i * menuSpacing);
        }
    }
    
    private void drawLeftPaddleThemesPanelSlideIn(Graphics2D g, double progress) {
        // Panel slides in from the right side (outside screen to final position)
        int panelWidth = BOARD_WIDTH / 2;
        int panelHeight = BOARD_HEIGHT;
        int finalPanelX = BOARD_WIDTH / 2; // Final position (right half of screen)
        
        // Calculate current position (starts from outside right edge)
        int startX = BOARD_WIDTH; // Start completely outside
        int currentPanelX = (int)(startX + (finalPanelX - startX) * progress);
        
        // Draw themes panel (using simple grid)
        drawSimplePaddleGrid(g, currentPanelX, 0, panelWidth, panelHeight, true);
    }
    
    private void drawRightPaddleThemesPanelSlideIn(Graphics2D g, double progress) {
        // Panel slides in from the left side (outside screen to final position)
        int panelWidth = BOARD_WIDTH / 2;
        int panelHeight = BOARD_HEIGHT;
        int finalPanelX = 0; // Final position (left half of screen)
        
        // Calculate current position (starts from outside left edge)
        int startX = -panelWidth; // Start completely outside left edge
        int currentPanelX = (int)(startX + (finalPanelX - startX) * progress);
        
        // Draw themes panel (using simple grid)
        drawSimplePaddleGrid(g, currentPanelX, 0, panelWidth, panelHeight, false);
    }
    
    private void drawLeftPaddleThemesPanelSlideOut(Graphics2D g, double progress) {
        // Panel slides out to the RIGHT side (paddle sinistra esce verso destra)
        int panelWidth = BOARD_WIDTH / 2;
        int panelHeight = BOARD_HEIGHT;
        int startPanelX = BOARD_WIDTH / 2; // Start position (right half of screen)
        
        // Calculate current position (goes to outside RIGHT edge)
        int endX = BOARD_WIDTH; // End completely outside right edge
        int currentPanelX = (int)(startPanelX + (endX - startPanelX) * progress);
        
        // Draw themes panel (using simple grid)
        drawSimplePaddleGrid(g, currentPanelX, 0, panelWidth, panelHeight, true);
    }
    
    private void drawRightPaddleThemesPanelSlideOut(Graphics2D g, double progress) {
        // Panel slides out to the LEFT side (paddle destra esce verso sinistra)
        int panelWidth = BOARD_WIDTH / 2;
        int panelHeight = BOARD_HEIGHT;
        int startPanelX = 0; // Start position (left half of screen)
        
        // Calculate current position (goes to outside LEFT edge)
        int endX = -panelWidth; // End completely outside left edge
        int currentPanelX = (int)(startPanelX + (endX - startPanelX) * progress);
        
        // Draw themes panel (using simple grid)
        drawSimplePaddleGrid(g, currentPanelX, 0, panelWidth, panelHeight, false);
    }
    
    private void drawRightPaddleThemesPanelAt(Graphics2D g, int panelX, int panelY, int panelWidth, int panelHeight) {
        // Modern grid layout panel background with red gradient
        GradientPaint panelGradient = new GradientPaint(
            panelX, panelY, new Color(40, 20, 20, 200),
            panelX + panelWidth, panelY + panelHeight, new Color(20, 0, 0, 180)
        );
        g.setPaint(panelGradient);
        g.fillRect(panelX, panelY, panelWidth, panelHeight);
        g.setPaint(null);
        
        // Panel title with red glow effect
        g.setColor(new Color(255, 255, 255, 200));
        float panelTitleSize = (float)(24 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(Font.BOLD, panelTitleSize));
        FontMetrics panelTitleFm = g.getFontMetrics();
        String panelTitle = "TEMI PADDLE";
        int panelTitleX = panelX + (panelWidth - panelTitleFm.stringWidth(panelTitle)) / 2;
        int panelTitleY = panelY + (int)(50 * scaleY);
        
        // Title glow effect (red theme)
        g.setColor(new Color(255, 100, 100, 60));
        g.drawString(panelTitle, panelTitleX + 2, panelTitleY + 2);
        g.setColor(Color.WHITE);
        g.drawString(panelTitle, panelTitleX, panelTitleY);
        
        // 4-column scrollable grid layout
        int gridStartY = panelY + (int)(80 * scaleY);
        int cardMargin = (int)(15 * Math.min(scaleX, scaleY));
        int cardSpacing = (int)(10 * Math.min(scaleX, scaleY));
        
        // Calculate card size based on 4 columns
        int availableWidth = panelWidth - (2 * cardMargin) - ((PADDLE_COLS - 1) * cardSpacing);
        int cardWidth = availableWidth / PADDLE_COLS;
        int cardHeight = (int)(100 * Math.min(scaleX, scaleY)); // Fixed card height
        
        // Create clipping area for scrolling
        Shape originalClip = g.getClip();
        g.clipRect(panelX, gridStartY, panelWidth, panelHeight - gridStartY - cardMargin);
        
        // Calculate rows needed
        int totalThemes = redPaddleThemeNames.size();
        int totalRows = (int) Math.ceil((double) totalThemes / PADDLE_COLS);
        
        // Draw all themes in scrollable 4-column grid
        for (int i = 0; i < totalThemes; i++) {
            int col = i % PADDLE_COLS;
            int row = i / PADDLE_COLS;
            
            int cardX = panelX + cardMargin + col * (cardWidth + cardSpacing);
            int cardY = gridStartY + row * (cardHeight + cardSpacing) - (int)rightPaddleGridScrollY;
            
            // Only draw if card is visible (performance optimization)
            if (cardY + cardHeight >= gridStartY && cardY <= panelY + panelHeight) {
                drawRightPaddleThemeCardAt(g, i, cardX, cardY, cardWidth, cardHeight);
            }
        }
        
        // Restore original clip
        g.setClip(originalClip);
        
        // Draw scroll indicators if needed
        if (totalRows > 2) { // Show indicators if content extends beyond 2 rows
            drawScrollIndicators(g, panelX, panelY, panelWidth, panelHeight, rightPaddleGridScrollY, totalRows, cardHeight + cardSpacing);
        }
    }
    
    private void drawRightPaddleThemeCardAt(Graphics2D g, int themeIndex, int cardX, int cardY, int cardWidth, int cardHeight) {
        // Use the modern right theme card style (same as drawModernRightThemeCard)
        drawModernRightThemeCard(g, themeIndex, cardX, cardY, cardWidth, cardHeight);
    }

    private void drawSimplePaddleGrid(Graphics2D g, int panelX, int panelY, int panelWidth, int panelHeight, boolean isLeftPaddle) {
        // Sfondo pannello con gradiente
        GradientPaint bgGradient = new GradientPaint(
            panelX, panelY, new Color(0, 0, 0, 180),
            panelX, panelY + panelHeight, new Color(20, 20, 30, 150)
        );
        g.setPaint(bgGradient);
        g.fillRect(panelX, panelY, panelWidth, panelHeight);
        g.setPaint(null);
        
        // Titolo con stile migliorato
        g.setColor(Color.WHITE);
        float titleSize = Math.max(16f, Math.min(24f, panelWidth / 15f)); // Ridimensiona titolo
        g.setFont(primaryFont.deriveFont(Font.BOLD, titleSize));
        String title = "PADDLE " + (isLeftPaddle ? "SINISTRO" : "DESTRO");
        FontMetrics fm = g.getFontMetrics();
        int titleX = panelX + (panelWidth - fm.stringWidth(title)) / 2;
        g.drawString(title, titleX, panelY + (int)(titleSize * 1.5f));
        
        // Calcola dimensioni adattive per la griglia
        int headerHeight = (int)(titleSize * 2f);
        int footerHeight = 30;
        int availableHeight = panelHeight - headerHeight - footerHeight;
        int availableWidth = panelWidth; // Nessun margine laterale - usa tutto lo spazio
        
        // NUOVO CALCOLO: massimizza dimensioni paddle invece di spacing
        int horizontalSpacing = 4; // Spacing orizzontale fisso tra i paddle
        int verticalSpacing = 8; // Spacing verticale più ampio tra le righe
        int maxCardSize = Math.min(200, availableHeight / 2); // Limite più generoso per paddle grandi
        
        // Calcola il cardSize usando tutto lo spazio rimanente dopo spacing orizzontale
        int cardSize = (availableWidth - (PADDLE_COLS - 1) * horizontalSpacing) / PADDLE_COLS;
        
        // Se supera il limite massimo, usa il limite
        if (cardSize > maxCardSize) {
            cardSize = maxCardSize;
        }
        
        // Assicurati che non sia troppo piccolo
        int minCardSize = Math.max(40, availableWidth / 12);
        if (cardSize < minCardSize) {
            cardSize = minCardSize;
        }
        
        
        // Verifica finale: se c'è spazio extra, ingrandisci i paddle invece dello spacing
        int actualGridWidth = PADDLE_COLS * cardSize + (PADDLE_COLS - 1) * horizontalSpacing;
        if (actualGridWidth < availableWidth) {
            // Distribuisci tutto lo spazio extra sui paddle (rendendoli più grandi)
            int extraSpace = availableWidth - actualGridWidth;
            cardSize += extraSpace / PADDLE_COLS;
        }
        int totalGridWidth = PADDLE_COLS * cardSize + (PADDLE_COLS - 1) * horizontalSpacing;
        int gridStartX = panelX; // Nessun margine - inizia dal bordo del pannello
        
        int gridY = panelY + headerHeight;
        // ===== SMOOTH SCROLLING: Usa valori double per precisione =====
        double scrollYDouble = isLeftPaddle ? paddleGridScrollY : rightPaddleGridScrollY;
        int scrollY = (int)Math.round(scrollYDouble); // Converti a int per rendering
        
        // Clipping per scroll
        g.clipRect(panelX, gridY, panelWidth, availableHeight);
        
        // Lista temi e immagini
        java.util.List<String> themes = isLeftPaddle ? bluePaddleThemeNames : redPaddleThemeNames;
        ArrayList<BufferedImage> themeImages = isLeftPaddle ? bluePaddleThemeImages : redPaddleThemeImages;
        int selectedTheme = isLeftPaddle ? selectedPaddleTheme : selectedRightPaddleTheme;
        
        // Disegna i temi in griglia 4xN
        for (int i = 0; i < themes.size(); i++) {
            int col = i % PADDLE_COLS;
            int row = i / PADDLE_COLS;
            
            int cardX = gridStartX + col * (cardSize + horizontalSpacing);
            int cardY = gridY + row * (cardSize + verticalSpacing) - scrollY;
            
            // Solo se visibile
            if (cardY + cardSize > gridY && cardY < gridY + availableHeight) {
                drawPaddleCard(g, i, cardX, cardY, cardSize, themes, themeImages, selectedTheme, isLeftPaddle);
            }
        }
        
        // Ripristina clip
        g.setClip(null);
        
        // Indicatori scroll migliorati
        if (themes.size() > PADDLE_COLS * 2) { // Se più di 2 righe
            drawSimpleScrollIndicators(g, panelX, panelY, panelWidth, panelHeight, scrollY, themes.size());
        }
    }
    
    private void drawPaddleCard(Graphics2D g, int themeIndex, int x, int y, int size, 
                               java.util.List<String> themes, ArrayList<BufferedImage> images, 
                               int selectedTheme, boolean isLeftPaddle) {
        boolean isSelected = (themeIndex == selectedTheme);
        
        // Ottimizza dimensioni paddle per riempire completamente lo spazio senza margini
        // Aspect ratio del paddle: altezza = 4 * larghezza (20x80 dal gioco)
        
        // Usa il 100% dello spazio disponibile per eliminare tutti i margini
        int maxWidth = size;
        int maxHeight = size;
        
        // Calcola dimensioni ottimali mantenendo proporzioni 1:4
        int paddleWidth, paddleHeight;
        
        // Prova prima a dimensionare basandosi sulla larghezza (riempi orizzontalmente)
        paddleWidth = maxWidth;
        paddleHeight = paddleWidth * 4;
        
        // Se troppo alto, ridimensiona basandosi sull'altezza (riempi verticalmente)
        if (paddleHeight > maxHeight) {
            paddleHeight = maxHeight;
            paddleWidth = paddleHeight / 4;
        }
        
        // Assicurati che non sia troppo piccolo (limiti minimi molto bassi)
        paddleWidth = Math.max(paddleWidth, 6); // Minimo 6px larghezza
        paddleHeight = Math.max(paddleHeight, 24); // Minimo 24px altezza
        
        // Posiziona il paddle per massimizzare l'uso dello spazio
        int paddleX, paddleY;
        
        // Se il paddle riempie completamente la larghezza, allinea a sinistra per eliminare margini
        if (paddleWidth == maxWidth) {
            paddleX = x; // Nessun margine laterale
            paddleY = y + (size - paddleHeight) / 2; // Centra solo verticalmente se necessario
        }
        // Se il paddle riempie completamente l'altezza, allinea in alto per eliminare margini
        else if (paddleHeight == maxHeight) {
            paddleX = x + (size - paddleWidth) / 2; // Centra solo orizzontalmente se necessario
            paddleY = y; // Nessun margine verticale
        }
        // Altrimenti centra normalmente (caso raro)
        else {
            paddleX = x + (size - paddleWidth) / 2;
            paddleY = y + (size - paddleHeight) / 2;
        }
        
        // Disegna immagine paddle se disponibile
        if (themeIndex < images.size() && images.get(themeIndex) != null) {
            BufferedImage paddleImg = images.get(themeIndex);
            
            // Disegna immagine con proporzioni corrette del paddle
            g.drawImage(paddleImg, paddleX, paddleY, paddleWidth, paddleHeight, this);
            
            // Bordo bianco più grande se selezionato
            if (isSelected) {
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(4f)); // Bordo più spesso
                g.drawRect(paddleX - 3, paddleY - 3, paddleWidth + 6, paddleHeight + 6);
            }
        } else {
            // Fallback: disegna un paddle con gradiente usando proporzioni corrette
            Color color1, color2;
            if (isLeftPaddle) {
                color1 = new Color(100, 150, 255);
                color2 = new Color(150, 200, 255);
            } else {
                color1 = new Color(255, 100, 100);
                color2 = new Color(255, 150, 150);
            }
            
            GradientPaint paddleGradient = new GradientPaint(paddleX, paddleY, color1, 
                                                           paddleX + paddleWidth, paddleY + paddleHeight, color2);
            g.setPaint(paddleGradient);
            g.fillRect(paddleX, paddleY, paddleWidth, paddleHeight);
            g.setPaint(null);
            
            // Bordo bianco più grande se selezionato
            if (isSelected) {
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(4f)); // Bordo più spesso
                g.drawRect(paddleX - 3, paddleY - 3, paddleWidth + 6, paddleHeight + 6);
            }
        }
    }
    
    private void drawSimpleScrollIndicators(Graphics2D g, int panelX, int panelY, int panelWidth, int panelHeight, int scrollY, int totalThemes) {
        int totalRows = (int) Math.ceil((double) totalThemes / (double) PADDLE_COLS);
        int maxScroll = Math.max(0, totalRows * 90 - (panelHeight - 150));
        
        // Freccia su
        if (scrollY > 0) {
            g.setColor(new Color(255, 255, 255, 200));
            int arrowX = panelX + panelWidth - 25;
            int arrowY = panelY + 60;
            int[] xPoints = {arrowX, arrowX + 10, arrowX + 20};
            int[] yPoints = {arrowY + 10, arrowY, arrowY + 10};
            g.fillPolygon(xPoints, yPoints, 3);
        }
        
        // Freccia giù
        if (scrollY < maxScroll) {
            g.setColor(new Color(255, 255, 255, 200));
            int arrowX = panelX + panelWidth - 25;
            int arrowY = panelY + panelHeight - 40;
            int[] xPoints = {arrowX, arrowX + 10, arrowX + 20};
            int[] yPoints = {arrowY, arrowY + 10, arrowY};
            g.fillPolygon(xPoints, yPoints, 3);
        }
        
        // Barra di scroll laterale
        if (maxScroll > 0) {
            int scrollBarX = panelX + panelWidth - 8;
            int scrollBarY = panelY + 80;
            int scrollBarHeight = panelHeight - 160;
            
            // Track della scrollbar
            g.setColor(new Color(255, 255, 255, 50));
            g.fillRect(scrollBarX, scrollBarY, 4, scrollBarHeight);
            
            // Thumb della scrollbar
            double thumbHeight = Math.max(20, scrollBarHeight * (double)(panelHeight - 150) / (totalRows * 90));
            double thumbY = scrollBarY + (scrollY / (double)maxScroll) * (scrollBarHeight - thumbHeight);
            
            g.setColor(new Color(255, 255, 255, 150));
            g.fillRect(scrollBarX, (int)thumbY, 4, (int)thumbHeight);
        }
    }
    
    private void drawScrollIndicators(Graphics2D g, int panelX, int panelY, int panelWidth, int panelHeight, double scrollY, int totalRows, int rowHeight) {
        // Calculate if we can scroll up or down
        double maxScroll = Math.max(0, totalRows * rowHeight - (panelHeight - 120)); // 120 for title space
        boolean canScrollUp = scrollY > 0;
        boolean canScrollDown = scrollY < maxScroll;
        
        // Draw up arrow if can scroll up
        if (canScrollUp) {
            int arrowX = panelX + panelWidth - 30;
            int arrowY = panelY + 90;
            drawScrollArrow(g, arrowX, arrowY, true, Color.WHITE);
        }
        
        // Draw down arrow if can scroll down  
        if (canScrollDown) {
            int arrowX = panelX + panelWidth - 30;
            int arrowY = panelY + panelHeight - 30;
            drawScrollArrow(g, arrowX, arrowY, false, Color.WHITE);
        }
        
        // Draw scroll bar on the right side
        if (totalRows > 2) {
            int scrollBarX = panelX + panelWidth - 10;
            int scrollBarY = panelY + 100;
            int scrollBarHeight = panelHeight - 140;
            drawScrollBar(g, scrollBarX, scrollBarY, scrollBarHeight, scrollY, maxScroll);
        }
    }
    
    private void drawScrollArrow(Graphics2D g, int x, int y, boolean up, Color color) {
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 150));
        int[] xPoints, yPoints;
        if (up) {
            xPoints = new int[]{x, x + 10, x + 20};
            yPoints = new int[]{y + 10, y, y + 10};
        } else {
            xPoints = new int[]{x, x + 10, x + 20};
            yPoints = new int[]{y, y + 10, y};
        }
        g.fillPolygon(xPoints, yPoints, 3);
    }
    
    private void drawScrollBar(Graphics2D g, int x, int y, int height, double scrollY, double maxScroll) {
        // Draw scroll track
        g.setColor(new Color(255, 255, 255, 50));
        g.fillRect(x, y, 5, height);
        
        // Draw scroll thumb
        if (maxScroll > 0) {
            double thumbHeight = Math.max(20, height * (height / (maxScroll + height)));
            double thumbY = y + (scrollY / maxScroll) * (height - thumbHeight);
            
            g.setColor(new Color(255, 255, 255, 150));
            g.fillRect(x, (int)thumbY, 5, (int)thumbHeight);
        }
    }

    private void drawHomeToThemesTransition(Graphics2D g) {
        // Draw the regular menu first
        drawMenu(g);
        
        // Draw the themes panel sliding up from bottom
        if (themesPanelProgress > 0) {
            drawThemesPanel(g);
        }
    }
    
    private void drawHomeToSettingsTransition(Graphics2D g) {
        // Always draw menu background first
        drawMenuBackground(g);
        
        // Draw checkerboard appearing from bottom to top and then animating
        if (checkerboardAppearProgress > 0) {
            drawTransitioningCheckerboard(g, checkerboardAppearProgress, checkerboardAnimationProgress);
        }
        
        // Draw paddles transitioning to settings position
        drawTransitioningMenuPaddles(g);
        
        // Draw animated background ball (fading out)
        if (homeToSettingsProgress < 0.7) {
            drawMenuBall(g);
        }
        
        // Draw menu text (gradually fading)
        drawMenuTextWithFade(g, 1.0 - homeToSettingsProgress);
        
        // Draw settings columns sliding in
        if (columnsTranslationProgress > 0) {
            drawSettingsColumnsSlideIn(g, columnsTranslationProgress);
        }
    }
    
    private void drawTransitioningCheckerboard(Graphics2D g, double appearProgress, double animationProgress) {
        // First draw the selected background theme
        drawSettingsBackground(g);
        
        // Checkerboard properties - scale tile size based on window dimensions
        int baseTileSize = 40;
        int tileSize = (int)(baseTileSize * Math.min(scaleX, scaleY));
        
        // Calculate diagonal offset for animation (only when animation progress > 0)
        double diagonalOffset = animationProgress * checkerboardOffset;
        int offsetX = (int)(diagonalOffset);
        int offsetY = (int)(diagonalOffset);
        
        // Calculate how many tiles we need to cover the screen plus the offset
        int tilesX = (getWidth() / tileSize) + 3;
        int tilesY = (getHeight() / tileSize) + 3;
        
        // Calculate the maximum Y position based on appear progress (0.0 to 1.0)
        // appearProgress 0.0 = bottom of screen, 1.0 = slightly above screen top
        // Make the checkerboard go higher than the window height
        int extraHeight = (int)(getHeight() * 0.2); // 20% extra height above screen
        int totalHeight = getHeight() + extraHeight;
        int maxVisibleY = (int)(totalHeight * (1.0 - appearProgress)) - extraHeight;
        
        // Draw checkerboard pattern with appear effect from bottom to top
        for (int x = -2; x < tilesX; x++) {
            for (int y = -2; y < tilesY; y++) {
                // Calculate actual position with diagonal offset
                int posX = x * tileSize - offsetX;
                int posY = y * tileSize - offsetY;
                
                // Skip tiles that are above the appear progress line
                if (posY < maxVisibleY) {
                    continue;
                }
                
                // Determine if this tile should be dark or light
                boolean isDark = (x + y) % 2 == 0;
                
                // Skip dark tiles (they remain transparent to show background)
                if (isDark) {
                    continue;
                }
                
                // Calculate distance from selected element for lighting effect
                double centerX, centerY;
                
                if (inCategoryColumn) {
                    // Light follows selected category on the left
                    centerX = 200 * scaleX;
                    centerY = (280 + selectedCategory * 80) * scaleY;
                } else {
                    // Light follows selected setting on the right
                    centerX = 650 * scaleX;
                    centerY = (200 + selectedCategorySetting * 90) * scaleY;
                }
                
                // Calculate distance for lighting
                double distance = Math.sqrt(Math.pow(posX + tileSize/2 - centerX, 2) + 
                                          Math.pow(posY + tileSize/2 - centerY, 2));
                double maxDistance = Math.sqrt(Math.pow(getWidth(), 2) + Math.pow(getHeight(), 2));
                double lightIntensity = 1.0 - (distance / maxDistance);
                lightIntensity = Math.max(0.1, Math.min(1.0, lightIntensity));
                
                // Create light tile color with intensity
                int alpha = (int)(80 * lightIntensity);
                Color tileColor = new Color(255, 255, 255, alpha);
                
                g.setColor(tileColor);
                g.fillRect(posX, posY, tileSize, tileSize);
                
                // Add subtle border
                g.setColor(new Color(255, 255, 255, (int)(20 * lightIntensity)));
                g.drawRect(posX, posY, tileSize, tileSize);
            }
        }
    }
    
    
    private void drawThemesPanel(Graphics2D g) {
        // Calculate panel height and position
        int panelHeight = (int)(120 * scaleY); // Same as in drawBackgroundSelection
        int targetY = BOARD_HEIGHT - panelHeight;
        
        // Apply transition animation - panel slides up from bottom
        int currentY = (int)(BOARD_HEIGHT - (panelHeight * themesPanelProgress));
        
        // Save original transform
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        g.translate(0, currentY - targetY);
        
        // Draw only the themes selection panel (not the full background)
        drawThemesSelectionPanel(g, targetY);
        
        // Restore transform
        g.setTransform(originalTransform);
    }
    
    private void drawThemesSelectionPanel(Graphics2D g, int panelY) {
        int panelHeight = (int)(120 * scaleY);
        
        // Draw title "SELEZIONE TEMI" sliding down from top
        drawThemesTitle(g);
        
        // Semi-transparent dark panel
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, panelY, getWidth(), panelHeight);
        
        // Panel border
        g.setColor(getPaddleGlowColor(true));
        g.setStroke(new BasicStroke(2));
        g.drawLine(0, panelY, getWidth(), panelY);
        
        // Theme thumbnails in horizontal row
        int thumbWidth = (int)(160 * scaleX);
        int thumbHeight = (int)(92 * scaleY);
        int thumbSpacing = (int)(10 * Math.min(scaleX, scaleY));
        
        // Calculate total width and center position
        int totalWidth = backgroundNames.size() * thumbWidth + (backgroundNames.size() - 1) * thumbSpacing;
        int thumbStartX = (getWidth() - totalWidth) / 2;
        int thumbY = panelY + (panelHeight - thumbHeight) / 2; // Center vertically in panel
        
        // Draw theme thumbnails
        for (int i = 0; i < backgroundNames.size(); i++) {
            int thumbX = thumbStartX + i * (thumbWidth + thumbSpacing);
            drawThemeThumbnail(g, i, thumbX, thumbY, thumbWidth, thumbHeight);
        }
        
        // Exit instruction at bottom left
        g.setColor(new Color(200, 200, 200));
        float instructionSize = (float)(16 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(instructionSize));
        g.drawString("ESC per tornare", (int)(20 * scaleX), getHeight() - (int)(10 * scaleY));
    }
    
    private void drawThemesTitle(Graphics2D g) {
        // Title slides down from top during transition
        float titleSize = (float)(36 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(titleSize));
        FontMetrics titleFm = g.getFontMetrics();
        String title = getText("THEME_SELECTION_TITLE");
        int titleX = (BOARD_WIDTH - titleFm.stringWidth(title)) / 2;
        
        // Calculate title Y position - slides down from top
        int targetY = (int)(50 * scaleY);
        int startY = -titleFm.getHeight(); // Start above screen
        int currentTitleY = (int)(startY + (targetY - startY) * themesPanelProgress);
        
        // Draw title background
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(titleX - 20, currentTitleY - titleFm.getHeight() + 5, 
                       titleFm.stringWidth(title) + 40, titleFm.getHeight() + 10, 10, 10);
        
        // Draw title text
        g.setColor(Color.WHITE);
        g.drawString(title, titleX, currentTitleY);
    }
    
    private void startThemesToHomeTransition() {
        isThemesToHomeTransition = true;
        themesToHomeProgress = 0.0;
        titleExitProgress = 0.0;
        panelExitProgress = 0.0;
        textAppearProgress = 0.0;
        paddleReturnProgress = 0.0;
    }
    
    private void updateThemesToHomeTransition() {
        themesToHomeProgress += 0.03; // Overall transition speed
        
        // Phase 1: Title goes up and panel goes down (first 40% of transition)
        if (themesToHomeProgress <= 0.4) {
            titleExitProgress = themesToHomeProgress / 0.4;
            panelExitProgress = themesToHomeProgress / 0.4;
        } else {
            titleExitProgress = 1.0;
            panelExitProgress = 1.0;
        }
        
        // Phase 2: Paddles return from sides (30% to 70% of transition)
        if (themesToHomeProgress > 0.3 && themesToHomeProgress <= 0.7) {
            paddleReturnProgress = (themesToHomeProgress - 0.3) / 0.4;
        } else if (themesToHomeProgress > 0.7) {
            paddleReturnProgress = 1.0;
        }
        
        // Phase 3: Text appears (last 30% of transition)
        if (themesToHomeProgress > 0.7) {
            textAppearProgress = (themesToHomeProgress - 0.7) / 0.3;
        }
        
        // Complete transition
        if (themesToHomeProgress >= 1.0) {
            themesToHomeProgress = 1.0;
            isThemesToHomeTransition = false;
            currentState = GameState.MENU;
            
            // Reset transition variables
            titleExitProgress = 0.0;
            panelExitProgress = 0.0;
            textAppearProgress = 0.0;
            paddleReturnProgress = 0.0;
            
            // Reset menu transition variables to normal state
            textFadeProgress = 1.0;
            paddleExitProgress = 0.0;
            themesPanelProgress = 0.0;
        }
    }
    
    private void drawSettingsToHomeTransition(Graphics2D g) {
        // Use the EXACT inverse of drawHomeToSettingsTransition
        // Draw menu background first (destination)
        drawMenuBackground(g);
        
        // Draw checkerboard disappearing (inverse appear progress)
        double inverseCheckerboardAppearProgress = 1.0 - settingsCheckerboardDisappearProgress;
        double inverseCheckerboardAnimationProgress = settingsCheckerboardAnimationProgress;
        if (inverseCheckerboardAppearProgress > 0) {
            drawTransitioningCheckerboard(g, inverseCheckerboardAppearProgress, inverseCheckerboardAnimationProgress);
        }
        
        // Draw paddles using INVERSE of the home→settings system
        drawTransitioningMenuPaddlesInverse(g);
        
        // Draw menu ball appearing (opposite of fading out)
        if (settingsToHomeProgress > 0.3) {
            drawMenuBall(g);
        }
        
        // Draw menu text appearing (opposite of fading)
        double inverseFadeProgress = settingsToHomeProgress; // Text appears as we progress
        drawMenuTextWithFade(g, inverseFadeProgress);
        
        // Draw settings columns disappearing (inverse of sliding in)
        double inverseColumnsProgress = 1.0 - settingsColumnsTranslationProgress;
        if (inverseColumnsProgress > 0) {
            drawSettingsColumnsSlideIn(g, inverseColumnsProgress);
        }
    }
    
    private void drawTransitioningMenuPaddlesInverse(Graphics2D g) {
        // This is the EXACT inverse of drawTransitioningMenuPaddles
        // Use the existing system but with inverted progress values
        
        // Calculate base paddle dimensions (same as original)
        int basePaddleWidth = (int)(250 * Math.min(scaleX, scaleY));
        int paddleHeight = (int)(menuPaddleHeight * 1.8);
        int paddleYOffset = (int)(-paddleHeight * 0.2);
        
        // Calculate paddle widths with INVERSE expansion (contracting back to menu state)
        double inverseLeftPaddleProgress = leftPaddleWidthProgress; // This contracts as we go to menu
        double leftWidthMultiplier = 1.0 + (0.3 * inverseLeftPaddleProgress); // Contracts from expanded to normal
        double rightWidthMultiplier = 1.0; // Right paddle doesn't expand
        
        int leftPaddleWidth = (int)(basePaddleWidth * leftWidthMultiplier);
        int rightPaddleWidth = (int)(basePaddleWidth * rightWidthMultiplier);
        
        // Save original transform
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        // Left paddle: same position as menu (exact position from drawMenuPaddles)
        int paddleExitOffset = (int)(paddleExitProgress * leftPaddleWidth * 1.5); // Should be 0 when in final menu position
        int leftCenterX = 0 - paddleExitOffset; // Same calculation as in drawMenuPaddles
        int leftCenterY = paddleYOffset + paddleHeight / 2;
        
        g.translate(leftCenterX, leftCenterY);
        g.rotate(Math.toRadians(-25));
        
        // Left paddle color dissolution from black to blue theme
        // Use smooth dissolution throughout entire transition
        double colorProgress = settingsPaddleTranslationProgress; // 0 = black, 1 = blue
        Color black = new Color(0, 0, 0);
        Color blue1 = new Color(100, 150, 255);
        Color blue2 = new Color(150, 200, 255);
        Color blueGlow = new Color(100, 150, 255, 100);
        
        // Smooth dissolution effect - immediate color transition with easing
        double easedProgress = easeInOutQuad(colorProgress);
        Color leftColor1 = blendColors(black, blue1, easedProgress);
        Color leftColor2 = blendColors(black, blue2, easedProgress);
        Color leftGlowColor = blendColors(new Color(0, 0, 0, 0), blueGlow, easedProgress);
        
        // Draw left paddle glow (right side, like in original)
        int glowWidth = Math.max(2, (int)(8 * Math.min(scaleX, scaleY)));
        if (easedProgress > 0.1) { // Show glow early in dissolution process
            g.setColor(leftGlowColor);
            g.fillRect(leftPaddleWidth/2, -paddleHeight/2, glowWidth, paddleHeight);
        }
        
        // Apply selected paddle theme with proper color dissolution
        if (selectedPaddleTheme >= 0 && selectedPaddleTheme < bluePaddleThemeImages.size()) {
            BufferedImage paddleImg = bluePaddleThemeImages.get(selectedPaddleTheme);
            if (paddleImg != null) {
                // Draw theme image with proper color dissolution from black
                if (easedProgress < 0.1) {
                    // Very early in transition: pure black
                    g.setColor(Color.BLACK);
                    g.fillRect(-leftPaddleWidth/2, -paddleHeight/2, leftPaddleWidth, paddleHeight);
                } else {
                    // Draw the theme image with alpha based on dissolution progress
                    float imageAlpha = (float)Math.max(0.0, Math.min(1.0, (easedProgress - 0.1) / 0.9));
                    g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, imageAlpha));
                    g.drawImage(paddleImg, -leftPaddleWidth/2, -paddleHeight/2, leftPaddleWidth, paddleHeight, this);
                    g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1.0f));
                    
                    // Draw black overlay that fades out
                    float blackAlpha = (float)(1.0 - easedProgress);
                    if (blackAlpha > 0) {
                        g.setColor(new Color(0, 0, 0, (int)(255 * blackAlpha)));
                        g.fillRect(-leftPaddleWidth/2, -paddleHeight/2, leftPaddleWidth, paddleHeight);
                    }
                }
            } else {
                // Default gradient (same as original - use fillRect)
                GradientPaint gradient = new GradientPaint(-leftPaddleWidth/2, -paddleHeight/2, leftColor1,
                                                         leftPaddleWidth/2, paddleHeight/2, leftColor2);
                g.setPaint(gradient);
                g.fillRect(-leftPaddleWidth/2, -paddleHeight/2, leftPaddleWidth, paddleHeight);
            }
        } else {
            // Default gradient (same as original - use fillRect)
            GradientPaint gradient = new GradientPaint(-leftPaddleWidth/2, -paddleHeight/2, leftColor1,
                                                     leftPaddleWidth/2, paddleHeight/2, leftColor2);
            g.setPaint(gradient);
            g.fillRect(-leftPaddleWidth/2, -paddleHeight/2, leftPaddleWidth, paddleHeight);
        }
        g.setPaint(null); // Reset paint like in original
        
        g.setTransform(originalTransform);
        
        // Right paddle: MOVES from settings position (off-screen) to menu position (exact position from drawMenuPaddles)
        // This is the INVERSE of the original: menu → settings becomes settings → menu
        int settingsRightCenterX = getWidth() + rightPaddleWidth/8; // Off-screen right (where it goes in settings)
        int rightPaddleExitOffset = (int)(paddleExitProgress * rightPaddleWidth * 1.5); // Should be 0 when in final menu position
        int menuRightCenterX = BOARD_WIDTH + rightPaddleExitOffset; // Same calculation as in drawMenuPaddles
        int currentRightCenterX = (int)(settingsRightCenterX + (menuRightCenterX - settingsRightCenterX) * settingsPaddleTranslationProgress);
        int rightCenterY = paddleYOffset + paddleHeight / 2;
        
        g.translate(currentRightCenterX, rightCenterY);
        
        // Right paddle rotation: INVERSE of original (from -25° back to +25°)
        double settingsRotation = -25.0; // Where it was in settings
        double menuRotation = 25.0; // Where it should end up in menu
        double currentRotation = settingsRotation + (menuRotation - settingsRotation) * settingsPaddleTranslationProgress;
        g.rotate(Math.toRadians(currentRotation));
        
        // Right paddle color dissolution from black to red theme
        // Use smooth dissolution throughout entire transition (matching left paddle)
        Color red1 = new Color(255, 100, 100);
        Color red2 = new Color(255, 150, 150);
        Color redGlow = getPaddleGlowColor(false);
        
        // Reuse the eased progress from left paddle calculation
        Color rightColor1 = blendColors(black, red1, easedProgress);
        Color rightColor2 = blendColors(black, red2, easedProgress);
        Color rightGlowColor = blendColors(new Color(0, 0, 0, 0), redGlow, easedProgress);
        
        // Draw right paddle glow with smooth dissolution
        if (easedProgress > 0.1) { // Show glow early in dissolution process
            g.setColor(rightGlowColor);
            int glowX = -rightPaddleWidth/2 - glowWidth; // Left side glow position
            g.fillRect(glowX, -paddleHeight/2, glowWidth, paddleHeight);
        }
        
        // Apply right paddle theme with proper color dissolution
        if (selectedRightPaddleTheme >= 0 && selectedRightPaddleTheme < redPaddleThemeImages.size()) {
            BufferedImage paddleImg = redPaddleThemeImages.get(selectedRightPaddleTheme);
            if (paddleImg != null) {
                // Draw theme image with proper color dissolution from black
                if (easedProgress < 0.1) {
                    // Very early in transition: pure black
                    g.setColor(Color.BLACK);
                    g.fillRect(-rightPaddleWidth/2, -paddleHeight/2, rightPaddleWidth, paddleHeight);
                } else {
                    // Draw the theme image with alpha based on dissolution progress
                    float imageAlpha = (float)Math.max(0.0, Math.min(1.0, (easedProgress - 0.1) / 0.9));
                    g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, imageAlpha));
                    g.drawImage(paddleImg, -rightPaddleWidth/2, -paddleHeight/2, rightPaddleWidth, paddleHeight, this);
                    g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1.0f));
                    
                    // Draw black overlay that fades out
                    float blackAlpha = (float)(1.0 - easedProgress);
                    if (blackAlpha > 0) {
                        g.setColor(new Color(0, 0, 0, (int)(255 * blackAlpha)));
                        g.fillRect(-rightPaddleWidth/2, -paddleHeight/2, rightPaddleWidth, paddleHeight);
                    }
                }
            } else {
                // Default gradient (same as original - use fillRect)
                GradientPaint gradient = new GradientPaint(-rightPaddleWidth/2, -paddleHeight/2, rightColor1,
                                                         rightPaddleWidth/2, paddleHeight/2, rightColor2);
                g.setPaint(gradient);
                g.fillRect(-rightPaddleWidth/2, -paddleHeight/2, rightPaddleWidth, paddleHeight);
            }
        } else {
            // Default gradient (same as original - use fillRect)
            GradientPaint gradient = new GradientPaint(-rightPaddleWidth/2, -paddleHeight/2, rightColor1,
                                                     rightPaddleWidth/2, paddleHeight/2, rightColor2);
            g.setPaint(gradient);
            g.fillRect(-rightPaddleWidth/2, -paddleHeight/2, rightPaddleWidth, paddleHeight);
        }
        g.setPaint(null); // Reset paint like in original
        
        g.setTransform(originalTransform);
    }
    
    private Color blendColors(Color color1, Color color2, double blend) {
        double invBlend = 1.0 - blend;
        int r = (int)(color1.getRed() * invBlend + color2.getRed() * blend);
        int green = (int)(color1.getGreen() * invBlend + color2.getGreen() * blend);
        int b = (int)(color1.getBlue() * invBlend + color2.getBlue() * blend);
        int alpha = (int)(color1.getAlpha() * invBlend + color2.getAlpha() * blend);
        return new Color(Math.max(0, Math.min(255, r)), Math.max(0, Math.min(255, green)), Math.max(0, Math.min(255, b)), Math.max(0, Math.min(255, alpha)));
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
    
    private Color getPaddleGlowColor(boolean isLeftPaddle) {
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
    
    private void updateCachedGlowColors() {
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
    
    private void drawThemesToHomeTransition(Graphics2D g) {
        // Draw the background that was selected (already visible)
        drawMenuBackground(g);
        
        // Draw paddles returning to their positions
        drawMenuPaddlesWithReturn(g);
        
        // Draw themes panel exiting down if still visible
        if (panelExitProgress < 1.0) {
            drawThemesPanelExit(g);
        }
        
        // Draw title exiting up if still visible
        if (titleExitProgress < 1.0) {
            drawThemesTitleExit(g);
        }
        
        // Draw menu text appearing
        if (textAppearProgress > 0) {
            drawMenuTextAppearing(g);
        }
        
        // Draw menu ball
        drawMenuBall(g);
    }
    
    private void drawMenuPaddlesWithReturn(Graphics2D g) {
        // Use inverse of exit progress for paddle return
        double returnOffset = 1.0 - paddleReturnProgress; // 1.0 -> 0.0
        
        // Make paddles fixed size for consistent appearance across all screen sizes
        int widePaddleWidth = (int)(250 * Math.min(scaleX, scaleY));
        int paddleHeight = (int)(menuPaddleHeight * 1.8);
        int paddleYOffset = (int)(-paddleHeight * 0.2);
        
        // Save original transform
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        // Left paddle returning from left
        int paddleExitOffset = (int)(returnOffset * widePaddleWidth * 1.5);
        int leftCenterX = 0 - paddleExitOffset;
        int leftCenterY = paddleYOffset + paddleHeight / 2;
        
        g.translate(leftCenterX, leftCenterY);
        g.rotate(Math.toRadians(-25));
        
        // Use selected paddle theme in transition - same logic as in drawMenuPaddles
        if (selectedPaddleTheme >= 0 && selectedPaddleTheme < bluePaddleThemeImages.size()) {
            BufferedImage paddleImg = bluePaddleThemeImages.get(selectedPaddleTheme);
            
            if (paddleImg != null) {
                // Custom theme paddle with rounded corners
                int cornerRadius = Math.max(4, widePaddleWidth / 4);
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, cornerRadius, cornerRadius));
                g.drawImage(paddleImg, -widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, null);
                g.setClip(null);
            } else {
                // Default gradient paddle
                GradientPaint leftPaddleGradient = new GradientPaint(
                    -widePaddleWidth/2, -paddleHeight/2, new Color(100, 150, 255),
                    widePaddleWidth/2, paddleHeight/2, new Color(150, 200, 255));
                g.setPaint(leftPaddleGradient);
                g.fillRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight);
            }
        } else {
            // Fallback to default gradient
            GradientPaint leftPaddleGradient = new GradientPaint(
                -widePaddleWidth/2, -paddleHeight/2, new Color(100, 150, 255),
                widePaddleWidth/2, paddleHeight/2, new Color(150, 200, 255));
            g.setPaint(leftPaddleGradient);
            g.fillRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight);
        }
        
        // Left paddle border glow
        g.setColor(getPaddleGlowColor(true));
        int glowWidth = Math.max(2, (int)(8 * Math.min(scaleX, scaleY)));
        g.fillRect(widePaddleWidth/2, -paddleHeight/2, glowWidth, paddleHeight);
        
        g.setTransform(originalTransform);
        
        // Right paddle returning from right
        int rightCenterX = BOARD_WIDTH + paddleExitOffset;
        int rightCenterY = paddleYOffset + paddleHeight / 2;
        
        g.translate(rightCenterX, rightCenterY);
        g.rotate(Math.toRadians(25));
        
        // Draw right paddle with selected theme
        if (selectedRightPaddleTheme >= 0 && selectedRightPaddleTheme < redPaddleThemeImages.size()) {
            BufferedImage rightPaddleImg = redPaddleThemeImages.get(selectedRightPaddleTheme);
            if (rightPaddleImg != null) {
                // Custom theme paddle with rounded corners
                int cornerRadius = Math.max(4, widePaddleWidth / 4);
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, cornerRadius, cornerRadius));
                g.drawImage(rightPaddleImg, -widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, null);
                g.setClip(null);
            } else {
                // Default gradient paddle
                GradientPaint rightPaddleGradient = new GradientPaint(
                    -widePaddleWidth/2, -paddleHeight/2, new Color(255, 100, 100),
                    widePaddleWidth/2, paddleHeight/2, new Color(255, 150, 150));
                g.setPaint(rightPaddleGradient);
                g.fillRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight);
            }
        } else {
            // Default gradient paddle
            GradientPaint rightPaddleGradient = new GradientPaint(
                -widePaddleWidth/2, -paddleHeight/2, new Color(255, 100, 100),
                widePaddleWidth/2, paddleHeight/2, new Color(255, 150, 150));
            g.setPaint(rightPaddleGradient);
            g.fillRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight);
        }
        
        // Right paddle border glow
        g.setColor(getPaddleGlowColor(false));
        g.fillRect(-widePaddleWidth/2 - glowWidth, -paddleHeight/2, glowWidth, paddleHeight);
        
        g.setTransform(originalTransform);
        g.setPaint(null);
    }
    
    private void drawThemesPanelExit(Graphics2D g) {
        int panelHeight = (int)(120 * scaleY);
        int targetY = BOARD_HEIGHT - panelHeight;
        
        // Panel moves down and exits at bottom
        int currentY = (int)(targetY + (panelHeight * panelExitProgress));
        
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        g.translate(0, currentY - targetY);
        
        drawThemesSelectionPanel(g, targetY);
        
        g.setTransform(originalTransform);
    }
    
    private void drawThemesTitleExit(Graphics2D g) {
        float titleSize = (float)(36 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(titleSize));
        FontMetrics titleFm = g.getFontMetrics();
        String title = getText("THEME_SELECTION_TITLE");
        int titleX = (BOARD_WIDTH - titleFm.stringWidth(title)) / 2;
        
        // Title moves up and exits at top
        int targetY = (int)(50 * scaleY);
        int exitY = -titleFm.getHeight();
        int currentTitleY = (int)(targetY + (exitY - targetY) * titleExitProgress);
        
        // Draw title background
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(titleX - 20, currentTitleY - titleFm.getHeight() + 5, 
                       titleFm.stringWidth(title) + 40, titleFm.getHeight() + 10, 10, 10);
        
        // Draw title text
        g.setColor(Color.WHITE);
        g.drawString(title, titleX, currentTitleY);
    }
    
    private void drawMenuTextAppearing(Graphics2D g) {
        // Title appearing
        float titleSize = (float)(48 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(titleSize));
        FontMetrics titleFm = g.getFontMetrics();
        String title = "PONG PING";
        int titleX = (BOARD_WIDTH - titleFm.stringWidth(title)) / 2;
        int titleY = (int)(150 * scaleY);
        
        // Glow effect with appearing animation
        int glowOffset = Math.max(1, (int)(3 * Math.min(scaleX, scaleY)));
        int glowAlpha = (int)(50 * textAppearProgress);
        g.setColor(new Color(0, 255, 255, glowAlpha));
        for (int i = 1; i <= glowOffset; i++) {
            g.drawString(title, titleX - i, titleY - i);
            g.drawString(title, titleX + i, titleY + i);
        }
        Color titleColor = currentTextColors.getOrDefault("menuTitle", Color.WHITE);
        int titleAlpha = (int)(titleColor.getAlpha() * textAppearProgress);
        g.setColor(new Color(titleColor.getRed(), titleColor.getGreen(), titleColor.getBlue(), titleAlpha));
        g.drawString(title, titleX, titleY);
        
        // Menu items appearing
        float menuSize = (float)(24 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(menuSize));
        FontMetrics menuFm = g.getFontMetrics();
        int menuStartY = (int)(300 * scaleY);
        int menuSpacing = (int)(60 * scaleY);
        
        for (int i = 0; i < menuItems.length; i++) {
            if (i == selectedMenuItem) {
                int cyanAlpha = (int)(255 * textAppearProgress);
                g.setColor(new Color(0, 255, 255, cyanAlpha));
                g.drawString("> " + menuItems[i] + " <", 
                    (BOARD_WIDTH - menuFm.stringWidth("> " + menuItems[i] + " <")) / 2, 
                    menuStartY + i * menuSpacing);
            } else {
                Color menuColor = currentTextColors.getOrDefault("menuItems", Color.WHITE);
                int menuAlpha = (int)(menuColor.getAlpha() * textAppearProgress);
                g.setColor(new Color(menuColor.getRed(), menuColor.getGreen(), menuColor.getBlue(), menuAlpha));
                g.drawString(menuItems[i], 
                    (BOARD_WIDTH - menuFm.stringWidth(menuItems[i])) / 2, 
                    menuStartY + i * menuSpacing);
            }
        }
    }
    
    private boolean isCollidingWithRotatedPaddle(double ballCenterX, double ballCenterY, double ballRadius, 
                                                double cx, double cy, double width, double height, double angleDegrees) {
        // Transform ball position to paddle's coordinate system
        double angleRad = Math.toRadians(angleDegrees);
        double cosA = Math.cos(angleRad);
        double sinA = Math.sin(angleRad);
        
        // Relative position
        double dx = ballCenterX - cx;
        double dy = ballCenterY - cy;
        
        // Rotate the ball's position to align with the paddle
        double bxPrime = dx * cosA + dy * sinA;
        double byPrime = -dx * sinA + dy * cosA;
        
        // Paddle bounds in transformed space
        double left = -width / 2;
        double right = width / 2;
        double top = -height / 2;
        double bottom = height / 2;
        
        // Find closest point on the rectangle to the transformed ball center
        double closestX = Math.max(left, Math.min(bxPrime, right));
        double closestY = Math.max(top, Math.min(byPrime, bottom));
        
        // Calculate distance from ball center to closest point
        double distX = bxPrime - closestX;
        double distY = byPrime - closestY;
        double distance = Math.sqrt(distX * distX + distY * distY);
        
        // Collision if distance is less than or equal to ball radius
        return distance <= ballRadius;
    }
    
    // Handle falling ball collision with rotated paddles - makes ball slide down
    private void handleFallingBallPaddleCollision(double ballCenterX, double ballCenterY, 
                                                 double paddleCenterX, double paddleCenterY, 
                                                 double paddleWidth, double paddleHeight, 
                                                 double paddleAngle, boolean isLeftPaddle) {
        // Transform to paddle coordinate system
        double angleRad = Math.toRadians(paddleAngle);
        double cosA = Math.cos(angleRad);
        double sinA = Math.sin(angleRad);
        
        // Relative position
        double dx = ballCenterX - paddleCenterX;
        double dy = ballCenterY - paddleCenterY;
        
        // Rotate to paddle space
        double bxPrime = dx * cosA + dy * sinA;
        double byPrime = -dx * sinA + dy * cosA;
        
        // Paddle bounds in transformed space
        double paddleLeft = -paddleWidth / 2;
        double paddleRight = paddleWidth / 2;
        double paddleTop = -paddleHeight / 2;
        double paddleBottom = paddleHeight / 2;
        
        // Find closest point on paddle to ball center
        double closestX = Math.max(paddleLeft, Math.min(bxPrime, paddleRight));
        double closestY = Math.max(paddleTop, Math.min(byPrime, paddleBottom));
        
        // Vector from closest point to ball center
        double penetrationX = bxPrime - closestX;
        double penetrationY = byPrime - closestY;
        double penetrationDistance = Math.sqrt(penetrationX * penetrationX + penetrationY * penetrationY);
        
        double ballRadius = menuBallSize / 2;
        
        if (penetrationDistance < ballRadius) {
            // We're intersecting - need to push ball out
            double pushDistance = ballRadius - penetrationDistance + 1; // +1 for safety margin
            
            // Normalize penetration vector
            if (penetrationDistance > 0) {
                penetrationX /= penetrationDistance;
                penetrationY /= penetrationDistance;
            } else {
                // Ball center is exactly on closest point - use a default direction
                penetrationX = 0;
                penetrationY = -1; // Push up
            }
            
            // Transform penetration vector back to world space
            double worldPenetrationX = penetrationX * cosA - penetrationY * sinA;
            double worldPenetrationY = penetrationX * sinA + penetrationY * cosA;
            
            // Push ball out of paddle
            menuBall2X += worldPenetrationX * pushDistance;
            menuBall2Y += worldPenetrationY * pushDistance;
            
            // Calculate surface normal (points away from paddle)
            double normalX = worldPenetrationX;
            double normalY = worldPenetrationY;
            
            // Current velocity
            double vx = menuBall2VX;
            double vy = menuBall2VY;
            
            // Remove velocity component that goes into the paddle
            double dotProduct = vx * normalX + vy * normalY;
            if (dotProduct < 0) { // Only if moving into paddle
                menuBall2VX = vx - dotProduct * normalX;
                menuBall2VY = vy - dotProduct * normalY;
            }
            
            // Add sliding effect along the paddle surface
            double baseSpeed = 2.0 * Math.min(scaleX, scaleY);
            
            // Calculate tangent vector (perpendicular to normal, pointing down along paddle)
            double tangentX = -normalY;
            double tangentY = normalX;
            
            // Ensure tangent points generally downward
            if (tangentY < 0) {
                tangentX = -tangentX;
                tangentY = -tangentY;
            }
            
            // Add sliding motion along the surface
            double slideSpeed = baseSpeed * 0.5;
            menuBall2VX += tangentX * slideSpeed;
            menuBall2VY += tangentY * slideSpeed;
            
            // Ensure minimum downward motion (gravity effect)
            if (menuBall2VY < baseSpeed * 0.3) {
                menuBall2VY = baseSpeed * 0.3;
            }
            
            // Limit velocity to prevent extreme speeds
            double maxSlideSpeed = baseSpeed * 1.8;
            menuBall2VX = Math.max(-maxSlideSpeed, Math.min(maxSlideSpeed, menuBall2VX));
            menuBall2VY = Math.max(baseSpeed * 0.2, Math.min(maxSlideSpeed, menuBall2VY));
        }
    }
    
    private void updateAI() {
        long currentTime = System.currentTimeMillis();
        double deltaTime = (currentTime - lastAIUpdate) / 1000.0;
        if (lastAIUpdate == 0) deltaTime = 0.016; // First frame
        lastAIUpdate = currentTime;
        
        // AI Difficulty Levels with target win rates for player (increased difficulty):
        // 0=Facile (85%), 1=Normale (40%), 2=Difficile (18%), 3=Esperto (12%), 4=Impossibile (5%)
        
        switch (aiDifficultySetting) {
            case 0: // FACILE - Player wins 85%
                updateAI_Easy(deltaTime);
                break;
            case 1: // NORMALE - Progressive AI (starts easy, gets harder)  
                updateAI_Normal(deltaTime);
                break;
            case 2: // DIFFICILE - Player wins 18%
                updateAI_Hard(deltaTime);
                break;
            case 3: // ESPERTO - Player wins 12%
                updateAI_Expert(deltaTime);
                break;
            case 4: // IMPOSSIBILE - Player wins 5%
                updateAI_Perfect(deltaTime);
                break;
            default:
                updateAI_Normal(deltaTime);
        }
        
        // Keep paddle within bounds
        aiPaddleY = Math.max(0, Math.min(BOARD_HEIGHT - PADDLE_HEIGHT, aiPaddleY));
        paddle2Y = (int)aiPaddleY;
    }
    
    private double applyMovementErrors(double velocity, int difficulty) {
        // Apply realistic movement errors based on difficulty
        switch (difficulty) {
            case 0: // FACILE - Many movement errors
                // 30% chance of hesitation (reduced speed)
                if (random.nextDouble() < 0.3) {
                    velocity *= 0.6; // Hesitate
                }
                // 15% chance of wrong direction briefly
                if (random.nextDouble() < 0.15) {
                    velocity *= -0.2; // Brief wrong direction
                }
                break;
                
            case 1: // NORMALE - Some movement errors  
                // 20% chance of hesitation
                if (random.nextDouble() < 0.2) {
                    velocity *= 0.75;
                }
                // 8% chance of overcorrection
                if (random.nextDouble() < 0.08) {
                    velocity *= 1.3; // Overcorrect then slow down next frame
                }
                break;
                
            case 2: // DIFFICILE - Very few movement errors
                // 6% chance of slight hesitation (reduced from 10%)
                if (random.nextDouble() < 0.06) {
                    velocity *= 0.88; // Less hesitation (was 0.85)
                }
                break;
                
            case 3: // ESPERTO - Minimal movement errors
                // 3% chance of minor hesitation (reduced from 5%)
                if (random.nextDouble() < 0.03) {
                    velocity *= 0.93; // Minimal hesitation (was 0.9)
                }
                break;
                
            case 4: // IMPOSSIBILE - Almost no movement errors
                // 2% chance of tiny hesitation
                if (random.nextDouble() < 0.02) {
                    velocity *= 0.95;
                }
                break;
        }
        
        return velocity;
    }
    
    private double applyProgressiveMovementErrors(double velocity, double progressFactor) {
        // BALANCED MOVEMENT ERRORS (Research: fewer errors, more strategic)
        double hesitationChance = Math.max(0.1, 0.25 - (progressFactor * 0.15)); // 25% to 10% hesitation
        double hesitationStrength = Math.max(0.7, 0.75 + (progressFactor * 0.2)); // Less severe hesitation
        double overcorrectionChance = Math.max(0.05, 0.12 - (progressFactor * 0.07)); // 12% to 5% overcorrection
        double overcorrectionStrength = Math.min(1.6, 1.2 + (progressFactor * 0.3)); // More aggressive overcorrection
        
        // Apply hesitation (reduced speed) - less frequent but still present
        if (random.nextDouble() < hesitationChance) {
            velocity *= hesitationStrength;
        }
        
        // Apply overcorrection - can make AI overshoot
        if (random.nextDouble() < overcorrectionChance) {
            velocity *= overcorrectionStrength;
        }
        
        return velocity;
    }
    
    private double calculatePlayerPerformanceFactor(int playerScore, int aiScore, int totalPoints) {
        if (totalPoints == 0) return 0.0; // No performance data yet
        
        // Base performance factor on player's win rate
        double winRate = (double) playerScore / totalPoints;
        
        // Score difference factor - if player is ahead, increase difficulty
        int scoreDifference = playerScore - aiScore;
        double scoreFactor = 0.0;
        if (scoreDifference >= 3) scoreFactor = 1.0;      // Player dominating
        else if (scoreDifference >= 2) scoreFactor = 0.7; // Player ahead
        else if (scoreDifference >= 1) scoreFactor = 0.4; // Player slightly ahead
        else if (scoreDifference == 0) scoreFactor = 0.2; // Tied
        else if (scoreDifference >= -1) scoreFactor = 0.1; // Player slightly behind
        else scoreFactor = 0.0; // Player losing badly
        
        // Win rate factor - higher win rate = harder AI
        double winRateFactor = Math.max(0.0, (winRate - 0.3) * 2.0); // 0.0 at 30% win rate, 1.0 at 80%
        
        // Combine factors
        return (scoreFactor * 0.6) + (winRateFactor * 0.4);
    }
    
    private double calculateStreakFactor() {
        // Player on winning streak = harder AI
        if (playerWinStreak >= 4) return 1.0;      // Very hot streak
        else if (playerWinStreak >= 3) return 0.8; // Hot streak
        else if (playerWinStreak >= 2) return 0.5; // Good streak
        else if (playerWinStreak >= 1) return 0.2; // Just won last point
        
        // AI on winning streak = easier AI (give player a chance)
        if (aiWinStreak >= 4) return -0.3;         // Make AI easier
        else if (aiWinStreak >= 3) return -0.2;
        else if (aiWinStreak >= 2) return -0.1;
        
        return 0.0; // No streak
    }
    
    // ADVANCED AI ADAPTATION METHODS (Based on 2024 Research)
    
    private double calculateAdvancedPlayerPerformance(int playerScore, int aiScore, int totalPoints) {
        if (totalPoints == 0) return 0.0;
        
        double winRate = (double) playerScore / totalPoints;
        int scoreDifference = playerScore - aiScore;
        
        // Score dominance factor (exponential scaling for big leads)
        double dominanceBonus = 0.0;
        if (scoreDifference >= 5) dominanceBonus = 1.0;
        else if (scoreDifference >= 4) dominanceBonus = 0.8;
        else if (scoreDifference >= 3) dominanceBonus = 0.6;
        else if (scoreDifference >= 2) dominanceBonus = 0.4;
        else if (scoreDifference >= 1) dominanceBonus = 0.2;
        
        // Win rate bonus (MIT research: >70% win rate indicates mastery)
        double winRateBonus = Math.max(0.0, (winRate - 0.5) * 2.0); // 0.0 at 50%, 1.0 at 100%
        
        // Early game bonus (first few points are crucial indicators)
        double earlyGameMultiplier = totalPoints <= 4 ? 1.5 : 1.0;
        
        return Math.min(1.0, (dominanceBonus * 0.6 + winRateBonus * 0.4) * earlyGameMultiplier);
    }
    
    private double calculateAdvancedStreakFactor() {
        // Enhanced streak calculation with momentum consideration
        double streakPower = 0.0;
        
        if (playerWinStreak >= 5) streakPower = 1.0;      // Unstoppable
        else if (playerWinStreak >= 4) streakPower = 0.85; // Dominating
        else if (playerWinStreak >= 3) streakPower = 0.6;  // Hot streak
        else if (playerWinStreak >= 2) streakPower = 0.35; // Building momentum
        else if (playerWinStreak >= 1) streakPower = 0.15; // Just won
        
        // AI comeback mechanism (DeepMind approach)
        if (aiWinStreak >= 4) streakPower -= 0.4; // Give player chance
        else if (aiWinStreak >= 3) streakPower -= 0.25;
        else if (aiWinStreak >= 2) streakPower -= 0.1;
        
        return Math.max(-0.4, Math.min(1.0, streakPower));
    }
    
    private double calculateRallyIntensityFactor() {
        if (rallyHitCounts.isEmpty()) return 0.0;
        
        // Calculate average rally length (skill indicator)
        double avgHits = rallyHitCounts.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        
        // Current rally intensity
        double currentRallyFactor = Math.min(1.0, currentRallyHits / 20.0); // Max at 20 hits
        
        // Historical performance
        double historicalFactor = Math.min(1.0, avgHits / 15.0); // Max at 15 avg hits
        
        return (currentRallyFactor * 0.6) + (historicalFactor * 0.4);
    }
    
    private double calculateDominanceFactor(int playerScore, int aiScore) {
        if (playerScore + aiScore == 0) return 0.0;
        
        double scoreDominance = (double) playerScore / (playerScore + aiScore);
        
        // Exponential scaling for dominance (research: human frustration curves)
        if (scoreDominance >= 0.8) return 1.0;      // 80%+ dominance
        else if (scoreDominance >= 0.7) return 0.8; // 70%+ strong lead
        else if (scoreDominance >= 0.6) return 0.5; // 60%+ moderate lead
        else if (scoreDominance >= 0.55) return 0.2; // 55%+ slight lead
        else return 0.0; // Even or behind
    }
    
    private double calculateConsistencyFactor() {
        // Track player's shot consistency (fewer misses = higher skill)
        int totalGamePoints = score1 + score2;
        if (totalGamePoints == 0) return 0.0;
        
        double missRate = (double) consecutiveMissedShots / Math.max(1, totalGamePoints);
        return Math.max(0.0, 1.0 - (missRate * 2.0)); // Invert miss rate
    }
    
    private double calculatePlayerReactionTime() {
        // Estimate reaction time based on ball speed handling
        if (ballSpeedHistory.isEmpty()) return 0.0;
        
        double avgHandledSpeed = ballSpeedHistory.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        // MIT research: humans handle 11-25 m/s, we scale to our game units
        double maxGameSpeed = 20.0; // Our max ball speed
        return Math.min(1.0, avgHandledSpeed / maxGameSpeed);
    }
    
    private double calculateEngagementLevel(int scoreDifference, int rallyHits) {
        // Pupil dilation research: engagement peaks at medium difficulty
        double scoreTension = Math.abs(scoreDifference) <= 2 ? 1.0 : Math.max(0.3, 1.0 - Math.abs(scoreDifference) * 0.1);
        double rallyEngagement = Math.min(1.0, rallyHits / 12.0); // Peak engagement at 12-hit rallies
        
        return (scoreTension * 0.6) + (rallyEngagement * 0.4);
    }
    
    private double calculateFrustrationLevel(int aiStreak, int playerScore) {
        // Prevent frustration by making AI easier when player is struggling
        double frustration = 0.0;
        
        if (playerScore == 0 && aiStreak >= 3) frustration = 0.8; // Very frustrated
        else if (playerScore <= 1 && aiStreak >= 4) frustration = 0.6; // Quite frustrated
        else if (aiStreak >= 5) frustration = 0.4; // Getting frustrated
        
        return frustration; // This will reduce AI difficulty
    }
    
    private double calculateBallSpeedAdaptation() {
        // Track how well player handles increasing ball speeds
        double currentSpeed = Math.sqrt(ballVX * ballVX + ballVY * ballVY);
        ballSpeedHistory.add(currentSpeed);
        
        // Keep only recent history (last 20 hits)
        if (ballSpeedHistory.size() > 20) {
            ballSpeedHistory.remove(0);
        }
        
        double avgSpeed = ballSpeedHistory.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return Math.min(1.0, avgSpeed / 15.0); // Normalize to our speed range
    }
    
    // Research-based reaction zone system (inspired by classic arcade games)
    private boolean isInReactionZone(int difficulty) {
        switch (difficulty) {
            case 0: // CAUTIOUS - Only reacts when ball is very close (75% court)
                return ballVX > 0 && ballX > BOARD_WIDTH * 0.75;
            case 1: // BALANCED - Reacts when ball crosses middle (50% court)  
                return ballVX > 0 && ballX > BOARD_WIDTH * 0.5;
            case 2: // AGGRESSIVE - Reacts early (30% court)
                return ballVX > 0 && ballX > BOARD_WIDTH * 0.3;
            case 3: // PREDICTIVE - Always watching (10% court)
                return ballVX > 0 && ballX > BOARD_WIDTH * 0.1;
            case 4: // PERFECT - Always tracking
                return ballVX > 0; 
            default:
                return ballVX > 0 && ballX > BOARD_WIDTH * 0.5;
        }
    }
    
    // Human-based reaction delays (research: 200-400ms typical human reaction)
    private long getReactionDelay(int difficulty) {
        switch (difficulty) {
            case 0: return 350 + random.nextInt(150); // 350-500ms (slow)
            case 1: return 250 + random.nextInt(100); // 250-350ms (normal)
            case 2: return 180 + random.nextInt(70);  // 180-250ms (fast)
            case 3: return 120 + random.nextInt(50);  // 120-170ms (very fast)
            case 4: return 50 + random.nextInt(30);   // 50-80ms (inhuman)
            default: return 250;
        }
    }
    
    // Imperfect prediction system with growing errors based on distance
    private double calculatePredictionError(int difficulty, double distance) {
        double baseError = distance / BOARD_WIDTH; // Error grows with distance
        
        switch (difficulty) {
            case 0: return baseError * 80 + random.nextGaussian() * 40; // ±40-120px
            case 1: return baseError * 50 + random.nextGaussian() * 25; // ±25-75px
            case 2: return baseError * 30 + random.nextGaussian() * 15; // ±15-45px
            case 3: return baseError * 15 + random.nextGaussian() * 8;  // ±8-23px
            case 4: return baseError * 5 + random.nextGaussian() * 3;   // ±3-8px
            default: return baseError * 50;
        }
    }
    
    // Personality-based movement patterns
    private double applyPersonalityTraits(double velocity, int difficulty) {
        switch (difficulty) {
            case 0: // CAUTIOUS - Hesitant, conservative movements
                if (random.nextDouble() < 0.4) velocity *= 0.6; // Frequent hesitation
                if (random.nextDouble() < 0.2) velocity *= -0.3; // Wrong direction
                break;
                
            case 1: // BALANCED - Moderate errors, balanced play
                if (random.nextDouble() < 0.15) velocity *= 0.8; // Some hesitation
                if (random.nextDouble() < 0.1) velocity *= 1.2; // Occasional rush
                break;
                
            case 2: // AGGRESSIVE - Fast but sometimes overcommits
                if (random.nextDouble() < 0.12) velocity *= 1.4; // Aggressive moves
                if (random.nextDouble() < 0.08) velocity *= 0.7; // Overcommit recovery
                break;
                
            case 3: // PREDICTIVE - Smooth, calculated movements
                if (random.nextDouble() < 0.05) velocity *= 0.9; // Rare hesitation
                if (random.nextDouble() < 0.03) velocity *= 1.1; // Precise adjustments
                break;
                
            case 4: // PERFECT - Minimal errors, machine-like precision
                if (random.nextDouble() < 0.02) velocity *= 0.98; // Tiny imperfections
                break;
        }
        
        return velocity;
    }
    
    private void updateAI_Easy(double deltaTime) {
        // FACILE: Player wins 85% - Weak AI with many mistakes (slightly harder)
        double ballCenterY = ballY + BALL_SIZE / 2;
        double paddleCenterY = aiPaddleY + PADDLE_HEIGHT / 2;
        
        // Get player paddle speed setting
        double baseSpeed = 4.0; // Lenta
        if (paddleSpeedSetting == 1) baseSpeed = 6.0; // Media  
        else if (paddleSpeedSetting == 2) baseSpeed = 8.0; // Veloce
        double playerSpeed = Math.max(3, (int)(baseSpeed * scaleY)); // Same as player paddle
        
        // React earlier and more consistently
        if (ballVX > 0 && ballX > BOARD_WIDTH * 0.65) {
            // 40% chance to miss completely (reduced from 50%)
            if (random.nextDouble() < 0.4) {
                return; // Skip this update (mistake)
            }
            
            // Large error in targeting but slightly better
            double error = (random.nextDouble() - 0.5) * 100; // ±50 pixel error (reduced from ±60)
            double targetY = ballCenterY + error;
            
            // Slightly faster than before (25% of player speed)
            double maxSpeed = playerSpeed * 0.25;
            double diff = targetY - paddleCenterY;
            
            if (Math.abs(diff) > 20) {
                aiCurrentVelocity = Math.signum(diff) * Math.min(maxSpeed, Math.abs(diff) * 0.35);
            } else {
                aiCurrentVelocity *= 0.7; // Slow down when close
            }
        } else {
            // Return to center very slowly
            double centerY = BOARD_HEIGHT / 2;
            double diff = centerY - paddleCenterY;
            aiCurrentVelocity = Math.signum(diff) * Math.min(playerSpeed * 0.1, Math.abs(diff) * 0.1);
        }
        
        // Add movement errors to make AI more human-like
        aiCurrentVelocity = applyMovementErrors(aiCurrentVelocity, 0); // 0 = Easy difficulty
        
        aiPaddleY += aiCurrentVelocity;
    }
    
    private void updateAI_Normal(double deltaTime) {
        // NORMALE: Progressive AI that starts easier and gradually gets harder
        double ballCenterY = ballY + BALL_SIZE / 2;
        double paddleCenterY = aiPaddleY + PADDLE_HEIGHT / 2;
        
        // Get player paddle speed setting
        double baseSpeed = 4.0; // Lenta
        if (paddleSpeedSetting == 1) baseSpeed = 6.0; // Media  
        else if (paddleSpeedSetting == 2) baseSpeed = 8.0; // Veloce
        double playerSpeed = Math.max(3, (int)(baseSpeed * scaleY)); // Same as player paddle
        
        // COMPREHENSIVE ADAPTIVE DIFFICULTY SYSTEM (Based on 2024 AI Research)
        int playerScore = score1;  // Left paddle (player) score
        int aiScore = score2;      // Right paddle (AI) score
        int totalPoints = playerScore + aiScore;
        int scoreDifference = playerScore - aiScore;
        long gameTime = System.currentTimeMillis() - gameStartTime;
        
        // CORE PERFORMANCE METRICS (Research-based)
        double gameProgressFactor = Math.min(1.0, totalPoints / 12.0); // Extended to 12 points for more granular progression
        double playerPerformanceFactor = calculateAdvancedPlayerPerformance(playerScore, aiScore, totalPoints);
        double streakFactor = calculateAdvancedStreakFactor();
        double rallyIntensityFactor = calculateRallyIntensityFactor();
        
        // ADVANCED BEHAVIORAL METRICS (MIT/DeepMind inspired)
        double timeBasedStressFactor = Math.min(1.0, gameTime / 300000.0); // 5 minutes max stress buildup
        double dominanceFactor = calculateDominanceFactor(playerScore, aiScore);
        double consistencyFactor = calculateConsistencyFactor();
        double reactionTimeFactor = calculatePlayerReactionTime();
        
        // PHYSIOLOGICAL SIMULATION (Research: pupil dilation studies)
        double engagementFactor = calculateEngagementLevel(scoreDifference, currentRallyHits);
        double frustrationFactor = calculateFrustrationLevel(aiWinStreak, playerScore);
        
        // BALL PHYSICS ADAPTATION (40mm ball physics research)
        double ballSpeedAdaptation = calculateBallSpeedAdaptation();
        double trajectoryComplexityFactor = Math.min(1.0, Math.abs(ballVY) / 8.0); // More complex angles = skilled player
        
        // BALANCED WEIGHTED COMBINATION (Research: start at moderate baseline)
        double baselineDifficulty = 0.3; // Start at 30% baseline (research: avoid too easy start)
        double adaptiveDifficulty = baselineDifficulty + 
                                   (gameProgressFactor * 0.10) +          // Game length (reduced weight)
                                   (Math.max(0.0, playerPerformanceFactor) * 0.20) +      // Core performance (main factor)
                                   (Math.max(-0.3, Math.min(0.8, streakFactor)) * 0.12) + // Win/loss patterns (clamped, less extreme)
                                   (Math.max(0.0, rallyIntensityFactor) * 0.08) +         // Rally skill
                                   (Math.max(0.0, dominanceFactor) * 0.12) +              // Score dominance (important)
                                   (Math.max(0.0, consistencyFactor) * 0.06) +            // Play consistency
                                   (Math.max(0.0, reactionTimeFactor) * 0.04) +           // Reaction speed
                                   (Math.max(0.0, engagementFactor) * 0.03) +             // Player engagement
                                   (Math.max(0.0, Math.min(0.5, frustrationFactor)) * 0.02) + // Frustration (anti-frustration)
                                   (Math.max(0.0, ballSpeedAdaptation) * 0.03);           // Ball physics mastery
        
        adaptiveDifficulty = Math.max(0.0, Math.min(1.0, adaptiveDifficulty)); // Clamp 0-1
        
        // MINIMUM RALLY GUARANTEE - First 10 hits must be successful
        boolean guaranteeHit = currentRallyHits < 10;
        
        // SAFETY CLAMPS - AI must always be functional
        adaptiveDifficulty = Math.max(0.0, Math.min(1.0, adaptiveDifficulty));
        
        // RESEARCH-BASED BALANCED PARAMETERS (Target: 50% player win rate)
        // Key principle: High speed + strategic failures = balanced gameplay
        double reactionDistance = guaranteeHit ? 0.3 : Math.max(0.2, 0.45 - (adaptiveDifficulty * 0.25)); // More aggressive: 45% to 20%
        double errorMultiplier = guaranteeHit ? 0.2 : Math.max(0.3, Math.min(1.0, 0.8 - (adaptiveDifficulty * 0.4))); // Fewer errors: 80% to 30%
        double speedMultiplier = guaranteeHit ? 0.8 : Math.max(0.6, Math.min(1.1, 0.7 + (adaptiveDifficulty * 0.4))); // Higher speeds: 70% to 110%
        double mistakeChance = guaranteeHit ? 0.03 : Math.max(0.08, Math.min(0.25, 0.2 - (adaptiveDifficulty * 0.12))); // Strategic failures: 20% to 8%
        
        // EMERGENCY REACTION ZONE - Always react if ball is very close
        boolean emergencyZone = ballVX > 0 && ballX > BOARD_WIDTH * 0.8; // Last 20% of screen
        boolean normalReaction = ballVX > 0 && ballX > BOARD_WIDTH * reactionDistance;
        
        if (emergencyZone || normalReaction) {
            // Calculate where ball will hit the paddle line
            int rightPaddleX = BOARD_WIDTH - (int)(20 * scaleX) - PADDLE_WIDTH;
            double timeToReach = (rightPaddleX - ballX) / ballVX;
            
            // Predict ball position with progressive error reduction
            double predictedY = ballY + ballVY * timeToReach;
            
            // STRATEGIC FAILURE SYSTEM (Research-based balancing)
            double paddleBallDistance = Math.abs(paddleCenterY - ballCenterY);
            boolean tooFarToReach = paddleBallDistance > PADDLE_HEIGHT * 2.5; // Research: strategic failure when too far
            
            // EMERGENCY MODE - Much more accurate in emergency zone
            double finalErrorMultiplier = emergencyZone ? 0.1 : errorMultiplier;
            double finalMistakeChance = emergencyZone ? 0.01 : mistakeChance;
            
            // STRATEGIC FAILURE - Miss when too far (research principle)
            if (tooFarToReach && !emergencyZone) {
                finalMistakeChance = Math.min(0.6, finalMistakeChance * 3.0); // Higher failure when far
                finalErrorMultiplier = Math.min(1.5, finalErrorMultiplier * 2.0); // More errors when reaching
            }
            
            // Add reaction delay and prediction error (research-tuned)
            double baseError = 25 * finalErrorMultiplier; // Reduced base error for more challenge
            double reactionError = random.nextGaussian() * baseError;
            double targetY = predictedY + reactionError;
            
            // Strategic mistake chance (research: AI should fail predictably when disadvantaged)
            if (random.nextDouble() < finalMistakeChance) {
                double mistakeSize = 80 * finalErrorMultiplier; // Smaller but more strategic errors
                targetY += (random.nextDouble() - 0.5) * mistakeSize;
            }
            
            // Progressive speed increase (faster in emergency)
            double diff = targetY - paddleCenterY;
            double finalSpeedMultiplier = emergencyZone ? Math.max(speedMultiplier, 0.8) : speedMultiplier;
            double maxSpeed = playerSpeed * finalSpeedMultiplier;
            
            // HUMAN-LIKE REACTION DELAY (Research: humans don't react instantly)
            double reactionDelay = emergencyZone ? 0.05 : (0.15 - (adaptiveDifficulty * 0.1)); // 150ms to 50ms reaction
            double accelerationRate = emergencyZone ? 0.25 : (0.08 + (adaptiveDifficulty * 0.12)); // Adaptive acceleration
            
            // Acceleration/deceleration with human-like delays
            double desiredVel = Math.signum(diff) * Math.min(maxSpeed, Math.abs(diff) * (0.5 + adaptiveDifficulty * 0.5));
            aiCurrentVelocity += (desiredVel - aiCurrentVelocity) * accelerationRate;
            
        } else {
            // Return to center with some overshoot
            double centerY = BOARD_HEIGHT / 2;
            double diff = centerY - paddleCenterY;
            aiCurrentVelocity += Math.signum(diff) * 0.6 * scaleY;
            aiCurrentVelocity *= 0.94; // Damping
        }
        
        // Add adaptive movement errors - NORMALE difficulty (with rally guarantee and emergency override)
        double movementErrorFactor = emergencyZone ? 0.05 : (guaranteeHit ? 0.1 : adaptiveDifficulty);
        aiCurrentVelocity = applyProgressiveMovementErrors(aiCurrentVelocity, movementErrorFactor);
        
        aiPaddleY += aiCurrentVelocity;
    }
    
    private void updateAI_Hard(double deltaTime) {
        // DIFFICILE: Player wins 15% - More competitive AI with strong prediction
        double ballCenterY = ballY + BALL_SIZE / 2;
        double paddleCenterY = aiPaddleY + PADDLE_HEIGHT / 2;
        
        // Get player paddle speed setting
        double baseSpeed = 4.0; // Lenta
        if (paddleSpeedSetting == 1) baseSpeed = 6.0; // Media  
        else if (paddleSpeedSetting == 2) baseSpeed = 8.0; // Veloce
        double playerSpeed = Math.max(3, (int)(baseSpeed * scaleY)); // Same as player paddle
        
        if (ballVX > 0 && ballX > BOARD_WIDTH * 0.3) { // Earlier reaction (was 0.35)
            // Advanced prediction with fewer errors
            double predictionY = calculateBallTrajectory();
            
            // Reduced errors - 10% chance of mistake (reduced from 15%)
            if (random.nextDouble() < 0.10) {
                predictionY += (random.nextDouble() - 0.5) * 40; // Smaller error (reduced from 60)
            }
            
            // Reduced reaction error
            double reactionError = random.nextGaussian() * 18; // Reduced from 25
            predictionY += reactionError;
            
            // Faster movement (90% of player speed, increased from 80%)
            double diff = predictionY - paddleCenterY;
            double maxSpeed = playerSpeed * 0.90;
            
            // More responsive movement
            if (Math.abs(diff) > 6) { // More precise threshold (was 8)
                double desiredVel = Math.signum(diff) * Math.min(maxSpeed, Math.abs(diff) * 1.2); // Increased multiplier
                aiCurrentVelocity += (desiredVel - aiCurrentVelocity) * 0.22; // Faster acceleration (was 0.18)
            } else {
                aiCurrentVelocity *= 0.94; // Better fine positioning (was 0.92)
            }
            
        } else {
            // More aggressive strategic positioning
            double strategicY = BOARD_HEIGHT * 0.5;
            double diff = strategicY - paddleCenterY;
            aiCurrentVelocity += Math.signum(diff) * Math.min(playerSpeed * 0.4, Math.abs(diff) * 0.5); // Increased positioning speed
            aiCurrentVelocity *= 0.98; // Better damping (was 0.97)
        }
        
        // Add movement errors - DIFFICILE difficulty
        aiCurrentVelocity = applyMovementErrors(aiCurrentVelocity, 2);
        
        aiPaddleY += aiCurrentVelocity;
    }
    
    private void updateAI_Expert(double deltaTime) {
        // ESPERTO: Player wins 8% - Elite AI with near-professional level play
        double ballCenterY = ballY + BALL_SIZE / 2;
        double paddleCenterY = aiPaddleY + PADDLE_HEIGHT / 2;
        
        // Get player paddle speed setting
        double baseSpeed = 4.0; // Lenta
        if (paddleSpeedSetting == 1) baseSpeed = 6.0; // Media  
        else if (paddleSpeedSetting == 2) baseSpeed = 8.0; // Veloce
        double playerSpeed = Math.max(3, (int)(baseSpeed * scaleY)); // Same as player paddle
        
        if (ballVX > 0 && ballX > BOARD_WIDTH * 0.2) { // Much earlier reaction (was 0.25)
            // Elite prediction with minimal errors
            double predictionY = calculateBallTrajectory();
            
            // Very few errors - 6% chance of mistake (reduced from 10%)
            if (random.nextDouble() < 0.06) {
                predictionY += (random.nextDouble() - 0.5) * 30; // Much smaller error (reduced from 60)
            }
            
            // Minimal reaction delay
            double reactionError = random.nextGaussian() * 12; // Reduced from 20
            predictionY += reactionError;
            
            // Near player speed (105% of player speed for challenge)
            double diff = predictionY - paddleCenterY;
            double maxSpeed = playerSpeed * 1.05;
            
            // Highly responsive movement
            if (Math.abs(diff) > 4) { // Very precise threshold (was 8)
                double desiredVel = Math.signum(diff) * Math.min(maxSpeed, Math.abs(diff) * 1.4); // Higher multiplier
                aiCurrentVelocity += (desiredVel - aiCurrentVelocity) * 0.26; // Much faster acceleration (was 0.18)
            } else {
                aiCurrentVelocity *= 0.96; // Very precise positioning (was 0.92)
            }
            
        } else {
            // Advanced strategic positioning with anticipation
            double strategicY = BOARD_HEIGHT * 0.5;
            // Anticipate player movement patterns
            if (ballVX < 0 && ballX < BOARD_WIDTH * 0.5) {
                // Position slightly towards where ball might return
                strategicY += (ballY - BOARD_HEIGHT * 0.5) * 0.3;
            }
            double diff = strategicY - paddleCenterY;
            aiCurrentVelocity += Math.signum(diff) * Math.min(playerSpeed * 0.5, Math.abs(diff) * 0.6); // Faster positioning
            aiCurrentVelocity *= 0.99; // Superior damping (was 0.95)
        }
        
        // Add movement errors - ESPERTO difficulty
        aiCurrentVelocity = applyMovementErrors(aiCurrentVelocity, 3);
        
        aiPaddleY += aiCurrentVelocity;
    }
    
    private void updateAI_Perfect(double deltaTime) {
        // IMPOSSIBILE: Very challenging but not truly impossible (5% player win rate)
        double ballCenterY = ballY + BALL_SIZE / 2;
        double paddleCenterY = aiPaddleY + PADDLE_HEIGHT / 2;
        
        // Get player paddle speed setting
        double baseSpeed = 4.0; // Lenta
        if (paddleSpeedSetting == 1) baseSpeed = 6.0; // Media  
        else if (paddleSpeedSetting == 2) baseSpeed = 8.0; // Veloce
        double playerSpeed = Math.max(3, (int)(baseSpeed * scaleY)); // Same as player paddle
        
        // Always tracking when ball is moving toward AI
        if (isInReactionZone(4)) {
            // Very fast reaction delay (40-60ms)
            long reactionTime = getReactionDelay(4);
            if (System.currentTimeMillis() - lastBallDirectionChange < reactionTime) {
                return;
            }
            
            // Near-perfect trajectory prediction
            double predictionY = calculateAdvancedBallTrajectory();
            
            // Very small prediction error (5% chance of bigger mistake to allow some wins)
            double distance = BOARD_WIDTH - ballX;
            double predictionError = calculatePredictionError(4, distance);
            
            // Rare chance for significant error to give player hope
            if (random.nextDouble() < 0.05) {
                predictionError += (random.nextDouble() - 0.5) * 60; // Occasional mistake
            }
            
            double targetY = predictionY + predictionError;
            
            // Near player speed with excellent timing
            double maxSpeed = playerSpeed * 0.98; // Almost as fast as player
            double diff = targetY - paddleCenterY;
            
            // Machine-like precision movements
            if (Math.abs(diff) > 5) {
                double desiredVel = Math.signum(diff) * Math.min(maxSpeed, Math.abs(diff) * 0.8);
                aiCurrentVelocity += (desiredVel - aiCurrentVelocity) * 0.3;
            } else {
                aiCurrentVelocity *= 0.95; // Perfect positioning
            }
            
        } else {
            // Strategic counter-positioning based on player patterns
            double strategicY = BOARD_HEIGHT * 0.5;
            
            // Analyze player paddle position for counter-strategy
            double playerPaddleCenter = paddle1Y + PADDLE_HEIGHT / 2.0;
            double screenCenter = BOARD_HEIGHT / 2.0;
            double playerBias = (playerPaddleCenter - screenCenter) / (BOARD_HEIGHT / 2.0);
            
            // Counter-position to player's likely shot
            strategicY += playerBias * PADDLE_HEIGHT * 0.6;
            
            double diff = strategicY - paddleCenterY;
            aiCurrentVelocity = Math.signum(diff) * Math.min(playerSpeed * 0.4, Math.abs(diff) * 0.3);
        }
        
        // Apply perfect personality traits (minimal errors)
        aiCurrentVelocity = applyPersonalityTraits(aiCurrentVelocity, 4);
        
        aiPaddleY += aiCurrentVelocity;
    }
    
    // Advanced trajectory calculation with multiple wall bounces (for predictive AI)
    private double calculateAdvancedBallTrajectory() {
        double simBallX = ballX;
        double simBallY = ballY;
        double simBallVX = ballVX;
        double simBallVY = ballVY;
        
        int rightPaddleX = BOARD_WIDTH - (int)(20 * scaleX) - PADDLE_WIDTH;
        int bounceCount = 0;
        
        // Simulate ball movement with multiple wall bounces
        while (simBallX < rightPaddleX && simBallVX > 0 && bounceCount < 10) {
            simBallX += simBallVX;
            simBallY += simBallVY;
            
            // Check for wall bounces
            if (simBallY <= 0) {
                simBallVY = Math.abs(simBallVY); // Bounce down
                simBallY = 0;
                bounceCount++;
            } else if (simBallY >= BOARD_HEIGHT - BALL_SIZE) {
                simBallVY = -Math.abs(simBallVY); // Bounce up
                simBallY = BOARD_HEIGHT - BALL_SIZE;
                bounceCount++;
            }
            
            // Safety check
            if (simBallX > BOARD_WIDTH * 2) break;
        }
        
        return simBallY + BALL_SIZE / 2;
    }
    
    // Simple trajectory for basic AI levels
    private double calculateBallTrajectory() {
        double simBallX = ballX;
        double simBallY = ballY;
        double simBallVX = ballVX;
        double simBallVY = ballVY;
        
        int rightPaddleX = BOARD_WIDTH - (int)(20 * scaleX) - PADDLE_WIDTH;
        
        // Simple simulation with one bounce
        while (simBallX < rightPaddleX && simBallVX > 0) {
            simBallX += simBallVX;
            simBallY += simBallVY;
            
            // Single wall bounce
            if (simBallY <= 0 || simBallY >= BOARD_HEIGHT - BALL_SIZE) {
                simBallVY = -simBallVY;
                simBallY = Math.max(0, Math.min(BOARD_HEIGHT - BALL_SIZE, simBallY));
                break; // Only one bounce for simpler AI
            }
            
            if (simBallX > BOARD_WIDTH * 2) break;
        }
        
        return simBallY + BALL_SIZE / 2;
    }
    
    private void applyPaddlePhysics(int paddleNumber, int paddleX, int paddleY) {
        // Advanced physics system considering multiple factors
        
        // Count this hit for progressive speed increase
        currentRallyHits++;
        
        // 1. IMPACT POSITION - Where on paddle the ball hits
        double ballCenterY = ballY + BALL_SIZE / 2;
        double paddleCenterY = paddleY + PADDLE_HEIGHT / 2;
        double impactOffset = (ballCenterY - paddleCenterY) / (PADDLE_HEIGHT / 2); // -1 to 1
        
        // 2. PADDLE MOVEMENT - Transfer momentum from moving paddle
        int currentPaddleY = paddleNumber == 1 ? paddle1Y : paddle2Y;
        int previousPaddleY = paddleNumber == 1 ? prevPaddle1Y : prevPaddle2Y;
        double paddleVelocity = (currentPaddleY - previousPaddleY); // Pixels per frame
        
        // 3. BALL SPEED - Current ball speed magnitude
        double currentSpeed = Math.sqrt(ballVX * ballVX + ballVY * ballVY);
        
        // 4. PROGRESSIVE SPEED INCREASE (based on research)
        // Slower progression: 1% increase per hit instead of 3%
        double speedMultiplier = 1.0 + (currentRallyHits * 0.01); // 1% increase per hit
        speedMultiplier = Math.min(speedMultiplier, 1.8); // Cap at 180% of initial speed
        
        // 4. AI DIFFICULTY FACTOR - Affects ball behavior
        double difficultyFactor = 1.0;
        if (currentState == GameState.SINGLE_PLAYER) {
            switch (aiDifficultySetting) {
                case 0: // Facile - Much slower for player advantage (90% win rate)
                    difficultyFactor = 0.8;
                    break;
                case 1: // Normale - Balanced (50% win rate)
                    difficultyFactor = 0.95;
                    break;
                case 2: // Difficile - Faster ball (15% win rate)
                    difficultyFactor = 1.12;
                    break;
                case 3: // Esperto - Much faster ball (8% win rate)
                    difficultyFactor = 1.22;
                    break;
                case 4: // Impossibile - Much faster (8% win rate)
                    difficultyFactor = 1.25;
                    break;
            }
        }
        
        // 5. RALLY COUNT FACTOR - Ball gets slightly faster in long rallies
        double rallyFactor = 1.0 + (rallies * 0.01); // 1% increase per rally, max effect
        rallyFactor = Math.min(rallyFactor, 1.2); // Cap at 20% increase
        
        // CALCULATE NEW VELOCITY
        
        // Reverse X direction
        ballVX = -ballVX;
        lastBallDirectionChange = System.currentTimeMillis(); // Track direction change for AI
        
        // Calculate new speed based on impact and conditions
        double baseSpeed = currentSpeed;
        
        // SPEED REDUCTION CONDITIONS:
        // 1. Paddle moving away from ball (defensive hit) - reduces speed
        boolean defensiveHit = (paddleNumber == 1 && paddleVelocity > 0 && ballVY > 0) || 
                              (paddleNumber == 1 && paddleVelocity < 0 && ballVY < 0) ||
                              (paddleNumber == 2 && paddleVelocity > 0 && ballVY > 0) || 
                              (paddleNumber == 2 && paddleVelocity < 0 && ballVY < 0);
        
        // 2. Edge hits (poor contact) - reduces speed
        boolean edgeHit = Math.abs(impactOffset) > 0.7;
        
        // 3. Very fast ball (over 80% of max speed) - natural decay
        boolean fastBall = currentSpeed > (maxBallSpeed * 0.8);
        
        // Apply speed modifications
        if (defensiveHit) {
            baseSpeed *= 0.85; // Reduce speed by 15% for defensive hits
        } else if (edgeHit) {
            baseSpeed *= 0.90; // Reduce speed by 10% for edge hits
        } else if (fastBall) {
            baseSpeed *= 0.95; // Slight natural decay for very fast balls
        } else {
            baseSpeed *= 0.98; // Very slight decay for normal hits
        }
        
        // Apply difficulty factor
        baseSpeed *= difficultyFactor;
        
        // Apply rally factor for long games
        baseSpeed *= rallyFactor;
        
        // Apply progressive speed increase
        baseSpeed *= speedMultiplier;
        
        // Add paddle movement momentum
        double paddleMomentum = paddleVelocity * PADDLE_SPEED_TRANSFER;
        
        // Calculate new angle based on impact position
        double newAngle = impactOffset * (Math.PI / 6); // Max 30 degrees
        
        // Apply angle to Y velocity
        ballVY = Math.sin(newAngle) * baseSpeed + paddleMomentum;
        
        // Ensure X velocity maintains minimum speed
        double xComponent = Math.cos(newAngle) * baseSpeed;
        ballVX = ballVX > 0 ? Math.max(xComponent, minBallSpeed) : -Math.max(xComponent, minBallSpeed);
        
        // 6. SPECIAL CONDITIONS
        
        // Perfect center hit - extra speed boost
        if (Math.abs(impactOffset) < 0.1) {
            double speedBoost = 1.1;
            ballVX *= speedBoost;
            ballVY *= speedBoost;
        }
        
        // Edge hit - more extreme angle
        if (Math.abs(impactOffset) > 0.8) {
            ballVY *= 1.5; // More dramatic angle
        }
        
        // Fast paddle movement - extra momentum transfer
        if (Math.abs(paddleVelocity) > 3) {
            ballVY += paddleVelocity * 0.5; // Extra momentum
        }
        
        // Apply maximum speed limits using dynamic max speed (unless fire ball unlimited speed is active)
        double finalSpeed = Math.sqrt(ballVX * ballVX + ballVY * ballVY);
        if (finalSpeed > maxBallSpeed && !unlimitedSpeedActive) {
            double scale = maxBallSpeed / finalSpeed;
            ballVX *= scale;
            ballVY *= scale;
        }
        
        // Ensure minimum speed
        if (finalSpeed < minBallSpeed) {
            double scale = minBallSpeed / finalSpeed;
            ballVX *= scale;
            ballVY *= scale;
        }
        
        // Position ball outside paddle to prevent multiple collisions
        if (paddleNumber == 1) {
            ballX = paddleX + 1;
        } else {
            ballX = paddleX - BALL_SIZE - 1;
        }
    }
    
    // New collision detection for game paddles (rectangular, not rotated)
    private boolean isCollidingWithGamePaddle(double ballCenterX, double ballCenterY, double ballRadius, int paddleX, int paddleY) {
        // Find closest point on the paddle rectangle to the ball center
        double closestX = Math.max(paddleX, Math.min(ballCenterX, paddleX + PADDLE_WIDTH));
        double closestY = Math.max(paddleY, Math.min(ballCenterY, paddleY + PADDLE_HEIGHT));
        
        // Calculate distance from ball center to closest point
        double distX = ballCenterX - closestX;
        double distY = ballCenterY - closestY;
        double distance = Math.sqrt(distX * distX + distY * distY);
        
        return distance <= ballRadius;
    }
    
    // New paddle collision handler with menu ball-like physics
    private void handlePaddleCollision(int paddleX, int paddleY, boolean isLeftPaddle) {
        // Base speed from current settings
        double baseSpeed = 4.5 * Math.min(scaleX, scaleY);
        
        // In two player mode, use traditional pong physics
        if (currentState == GameState.PLAYING) {
            // Simple bounce back with angle based on where ball hits paddle
            double ballCenterY = ballY + BALL_SIZE / 2;
            double paddleCenterY = paddleY + PADDLE_HEIGHT / 2;
            double hitOffset = (ballCenterY - paddleCenterY) / (PADDLE_HEIGHT / 2); // -1 to 1
            
            // Reverse horizontal direction
            ballVX = isLeftPaddle ? Math.abs(ballVX) : -Math.abs(ballVX);
            
            // Add angle based on hit position
            ballVY = hitOffset * baseSpeed * 0.7; // Less extreme angles than menu
            
            // Normalize speed
            double currentSpeed = Math.sqrt(ballVX * ballVX + ballVY * ballVY);
            if (currentSpeed > 0) {
                double targetSpeed = baseSpeed * 1.2; // Slightly faster than base
                ballVX = (ballVX / currentSpeed) * targetSpeed;
                ballVY = (ballVY / currentSpeed) * targetSpeed;
            }
        } else {
            // Single player mode - menu ball-like physics with sliding
            if (currentState == GameState.SINGLE_PLAYER) {
                // Calculate if ball is hitting paddle from above (sliding effect)
                double ballCenterY = ballY + BALL_SIZE / 2;
                double paddleTop = paddleY;
                double paddleBottom = paddleY + PADDLE_HEIGHT;
                
                // If ball is falling from above and hits paddle, make it slide
                if (ballVY > 0 && ballCenterY < paddleTop + PADDLE_HEIGHT * 0.3) {
                    // Sliding physics - ball bounces off at an angle
                    ballVX = isLeftPaddle ? Math.abs(ballVX) : -Math.abs(ballVX);
                    ballVY = -Math.abs(ballVY) * 0.7; // Bounce up but slower
                    
                    // Add some horizontal speed for sliding effect
                    ballVX *= 1.3;
                } else {
                    // Normal side collision - bounce back toward other paddle
                    ballVX = isLeftPaddle ? Math.abs(ballVX) : -Math.abs(ballVX);
                    
                    // Add upward bias like menu ball
                    double targetX = isLeftPaddle ? BOARD_WIDTH : 0;
                    double targetY = paddleY - BOARD_HEIGHT * 0.2; // Aim higher
                    double directionX = targetX - (ballX + BALL_SIZE/2);
                    double directionY = targetY - (ballY + BALL_SIZE/2);
                    double distance = Math.sqrt(directionX * directionX + directionY * directionY);
                    
                    if (distance > 0) {
                        ballVX = (directionX / distance) * baseSpeed;
                        ballVY = (directionY / distance) * baseSpeed;
                    }
                }
            }
        }
        
        // Position ball outside paddle to prevent sticking
        if (isLeftPaddle) {
            ballX = paddleX + 1;
        } else {
            ballX = paddleX - BALL_SIZE - 1;
        }
        
        // Limit velocity
        ballVX = Math.max(-baseSpeed * 2.5, Math.min(baseSpeed * 2.5, ballVX));
        ballVY = Math.max(-baseSpeed * 2.5, Math.min(baseSpeed * 2.5, ballVY));
    }
    
    // Object Pool methods for particles (performance optimization)
    private Particle getParticleFromPool() {
        if (particlePool.isEmpty()) {
            return new Particle(0, 0, 0, 0, 0, Color.WHITE); // Create new if pool is empty
        }
        return particlePool.poll();
    }
    
    private void returnParticleToPool(Particle particle) {
        if (particlePool.size() < MAX_POOL_SIZE) {
            particlePool.offer(particle);
        }
    }
    
    private void createParticles(int x, int y, Color color, int count) {
        // Limit total active particles to prevent lag
        if (particles.size() >= MAX_ACTIVE_PARTICLES) {
            return; // Skip creating new particles if we're at the limit
        }
        
        // Only create as many particles as we have room for
        int actualCount = Math.min(count, MAX_ACTIVE_PARTICLES - particles.size());
        
        for (int i = 0; i < actualCount; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double speed = random.nextDouble() * 4 + 1;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            
            // Use object pool instead of creating new particles
            Particle particle = getParticleFromPool();
            particle.reset(x, y, vx, vy, 30 + random.nextInt(20), color);
            particles.add(particle);
        }
    }
    
    private void createBackgroundParticles() {
        particles.clear(); // Pulisce particelle esistenti
        
        // Crea griglia per distribuzione uniforme
        int cols = 10;
        int rows = 8;
        int cellWidth = BOARD_WIDTH / cols;
        int cellHeight = BOARD_HEIGHT / rows;
        
        Color[] colors = {
            new Color(100, 150, 255, 180),  // Blu
            new Color(255, 100, 150, 180),  // Rosa
            new Color(150, 255, 100, 180),  // Verde
            new Color(255, 200, 100, 180),  // Arancione
            new Color(200, 100, 255, 180)   // Viola
        };
        
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                // Posiziona particelle casualmente all'interno di ogni cella
                double x = col * cellWidth + random.nextDouble() * cellWidth;
                double y = row * cellHeight + random.nextDouble() * cellHeight;
                
                // Velocità casuale ma controllata
                double angle = random.nextDouble() * 2 * Math.PI;
                double speed = 0.5 + random.nextDouble() * 1.0;
                double vx = Math.cos(angle) * speed;
                double vy = Math.sin(angle) * speed;
                
                // Colore casuale dalla palette
                Color color = colors[random.nextInt(colors.length)];
                
                particles.add(new Particle(x, y, vx, vy, 0, color, true));
            }
        }
    }
    
    private void updateParticles() {
        Iterator<Particle> iter = particles.iterator();
        while (iter.hasNext()) {
            Particle p = iter.next();
            p.update();
            if (p.isDead()) {
                iter.remove();
                returnParticleToPool(p); // Return dead particles to pool for reuse
            }
        }
    }
    
    // Screen shake system
    private void addScreenShake(double intensity) {
        shakeIntensity = Math.max(shakeIntensity, intensity);
        shakeFrames = Math.max(shakeFrames, (int)(intensity * 15)); // Longer shake for stronger hits
    }
    
    private void updateScreenShake() {
        if (shakeFrames > 0) {
            shakeX = (shakeRandom.nextDouble() - 0.5) * shakeIntensity * 2;
            shakeY = (shakeRandom.nextDouble() - 0.5) * shakeIntensity * 2;
            shakeIntensity *= 0.85; // Decay intensity
            shakeFrames--;
        } else {
            shakeX = 0;
            shakeY = 0;
            shakeIntensity = 0;
        }
    }
    
    // Ball trail system
    private void updateBallTrail() {
        // Add current ball position to trail
        ballTrailPoints.add(new Point2D.Double(ballX + BALL_SIZE/2, ballY + BALL_SIZE/2));
        
        // Remove old trail points
        while (ballTrailPoints.size() > MAX_TRAIL_LENGTH) {
            ballTrailPoints.remove(0);
        }
    }
    
    private void drawBallTrail(Graphics2D g) {
        if (ballTrailPoints.size() < 2) return;
        
        // Draw trail segments with fading alpha
        for (int i = 1; i < ballTrailPoints.size(); i++) {
            Point2D prev = ballTrailPoints.get(i - 1);
            Point2D curr = ballTrailPoints.get(i);
            
            // Skip if any point is null
            if (prev == null || curr == null) continue;
            
            // Calculate alpha based on position in trail (newer = more opaque)
            float alpha = (float)i / ballTrailPoints.size() * 0.6f;
            int trailSize = (int)(BALL_SIZE * 0.7 * ((float)i / ballTrailPoints.size()));
            
            g.setColor(new Color(1.0f, 1.0f, 1.0f, alpha));
            g.fillOval((int)curr.getX() - trailSize/2, (int)curr.getY() - trailSize/2, trailSize, trailSize);
        }
    }
    
    // Dynamic paddle glow system
    private void updatePaddleGlow() {
        double ballCenterX = ballX + BALL_SIZE / 2;
        double ballCenterY = ballY + BALL_SIZE / 2;
        
        // Left paddle glow based on ball distance
        int leftPaddleX = (int)(20 * scaleX);
        int leftPaddleCenterX = leftPaddleX + PADDLE_WIDTH / 2;
        int leftPaddleCenterY = paddle1Y + PADDLE_HEIGHT / 2;
        double leftDistance = Math.sqrt(Math.pow(ballCenterX - leftPaddleCenterX, 2) + 
                                       Math.pow(ballCenterY - leftPaddleCenterY, 2));
        
        if (leftDistance < GLOW_DISTANCE_THRESHOLD) {
            leftPaddleGlow = Math.min(MAX_PADDLE_GLOW, 
                (float)(1.0 - leftDistance / GLOW_DISTANCE_THRESHOLD));
        } else {
            leftPaddleGlow *= 0.9f; // Fade out gradually
        }
        
        // Right paddle glow based on ball distance
        int rightPaddleX = BOARD_WIDTH - (int)(20 * scaleX) - PADDLE_WIDTH;
        int rightPaddleCenterX = rightPaddleX + PADDLE_WIDTH / 2;
        int rightPaddleCenterY = paddle2Y + PADDLE_HEIGHT / 2;
        double rightDistance = Math.sqrt(Math.pow(ballCenterX - rightPaddleCenterX, 2) + 
                                        Math.pow(ballCenterY - rightPaddleCenterY, 2));
        
        if (rightDistance < GLOW_DISTANCE_THRESHOLD) {
            rightPaddleGlow = Math.min(MAX_PADDLE_GLOW, 
                (float)(1.0 - rightDistance / GLOW_DISTANCE_THRESHOLD));
        } else {
            rightPaddleGlow *= 0.9f; // Fade out gradually
        }
    }
    
    // Advanced combo effects system
    private void updateComboEffects() {
        long currentTime = System.currentTimeMillis();
        
        // Update pulse animation (sine wave for smooth pulsing)
        comboPulse = (float)Math.sin(currentTime * 0.01) * 0.3f + 1.0f; // Range: 0.7 to 1.3
        
        // Update scale based on combo level and recent activity
        if (currentTime - lastComboTime < 500) { // Recent combo hit
            comboScale = Math.min(comboScale + 0.1f, 2.0f); // Grow on hit
        } else {
            comboScale = Math.max(comboScale - 0.05f, 1.0f); // Shrink back to normal
        }
        
        // Update glow intensity based on combo count
        if (comboCount > 0) {
            float targetGlow = Math.min(comboCount * 0.1f, 1.0f);
            comboGlow += (targetGlow - comboGlow) * 0.1f;
        } else {
            comboGlow *= 0.9f; // Fade out when no combo
        }
        
        // Update color based on combo level
        updateComboColor();
        
        // Handle combo visibility timer
        if (showCombo) {
            comboShowTimer--;
            if (comboShowTimer <= 0) {
                showCombo = false; // Hide combo after timer expires
            }
        }
        
        // Handle milestone effects
        if (comboMilestoneHit) {
            comboMilestoneTimer--;
            if (comboMilestoneTimer <= 0) {
                comboMilestoneHit = false;
            }
        }
    }
    
    private void updateComboColor() {
        if (comboCount >= 50) {
            comboColor = new Color(255, 0, 255); // Magenta for epic combos
        } else if (comboCount >= 20) {
            comboColor = new Color(255, 100, 0); // Orange for great combos
        } else if (comboCount >= 10) {
            comboColor = new Color(255, 255, 0); // Yellow for good combos
        } else if (comboCount >= 5) {
            comboColor = new Color(0, 255, 0); // Green for decent combos
        } else {
            comboColor = Color.WHITE; // White for small combos
        }
    }
    
    private void triggerComboIncrement() {
        lastComboTime = System.currentTimeMillis();
        comboScale = 1.5f; // Immediate scale boost
        
        // Show combo and reset timer
        showCombo = true;
        comboShowTimer = COMBO_SHOW_DURATION;
        
        // Check for milestones
        if (comboCount % 10 == 0 && comboCount > 0) {
            comboMilestoneHit = true;
            comboMilestoneTimer = 60; // 1 second at 60 FPS
            // Add extra particles for milestone
            createParticles(BOARD_WIDTH / 4, 50, comboColor, 15);
        }
    }
    
    // Right paddle combo effects system
    private void updateRightComboEffects() {
        long currentTime = System.currentTimeMillis();
        
        // Update pulse animation (sine wave for smooth pulsing)
        rightComboPulse = (float)Math.sin(currentTime * 0.01) * 0.3f + 1.0f; // Range: 0.7 to 1.3
        
        // Update scale based on combo level and recent activity
        if (currentTime - lastRightComboTime < 500) { // Recent combo hit
            rightComboScale = Math.min(rightComboScale + 0.1f, 2.0f); // Grow on hit
        } else {
            rightComboScale = Math.max(rightComboScale - 0.05f, 1.0f); // Shrink back to normal
        }
        
        // Update glow intensity based on combo count
        if (rightComboCount > 0) {
            float targetGlow = Math.min(rightComboCount * 0.1f, 1.0f);
            rightComboGlow += (targetGlow - rightComboGlow) * 0.1f;
        } else {
            rightComboGlow *= 0.9f; // Fade out when no combo
        }
        
        // Update color based on combo level
        updateRightComboColor();
        
        // Handle combo visibility timer
        if (showRightCombo) {
            rightComboShowTimer--;
            if (rightComboShowTimer <= 0) {
                showRightCombo = false; // Hide combo after timer expires
            }
        }
        
        // Handle milestone effects
        if (rightComboMilestoneHit) {
            rightComboMilestoneTimer--;
            if (rightComboMilestoneTimer <= 0) {
                rightComboMilestoneHit = false;
            }
        }
    }
    
    private void updateRightComboColor() {
        if (rightComboCount >= 50) {
            rightComboColor = new Color(255, 0, 255); // Magenta for epic combos
        } else if (rightComboCount >= 20) {
            rightComboColor = new Color(255, 100, 0); // Orange for great combos
        } else if (rightComboCount >= 10) {
            rightComboColor = new Color(255, 255, 0); // Yellow for good combos
        } else if (rightComboCount >= 5) {
            rightComboColor = new Color(0, 255, 0); // Green for decent combos
        } else {
            rightComboColor = Color.WHITE; // White for small combos
        }
    }
    
    private void triggerRightComboIncrement() {
        lastRightComboTime = System.currentTimeMillis();
        rightComboScale = 1.5f; // Immediate scale boost
        
        // Show combo and reset timer
        showRightCombo = true;
        rightComboShowTimer = COMBO_SHOW_DURATION;
        
        // Check for milestones
        if (rightComboCount % 10 == 0 && rightComboCount > 0) {
            rightComboMilestoneHit = true;
            rightComboMilestoneTimer = 60; // 1 second at 60 FPS
            // Add extra particles for milestone (right side)
            createParticles(3 * BOARD_WIDTH / 4, 50, rightComboColor, 15);
        }
    }
    
    // Pause line animation system
    private void updatePauseLineAnimation() {
        // Move the line offset continuously
        pauseLineOffset += PAUSE_LINE_SPEED;
        
        // Reset offset when it gets too large to create seamless loop
        double lineSpacing = 20 * scaleY;
        if (PAUSE_LINE_SPEED > 0 && pauseLineOffset >= lineSpacing * 2) {
            pauseLineOffset = 0;
        } else if (PAUSE_LINE_SPEED < 0 && pauseLineOffset <= -lineSpacing * 2) {
            pauseLineOffset = 0;
        }
        
        // Update pause timer and motivational message system
        pauseTimer++;
        
        if (pauseTimer >= MESSAGE_SHOW_DELAY && !showMotivationalMessage) {
            // Generate motivational message based on performance
            generateMotivationalMessage();
            showMotivationalMessage = true;
            // Start from beginning of diagonal
            messageScrollOffset = 0;
        }
        
        if (showMotivationalMessage) {
            // Scroll message across screen
            messageScrollOffset += MESSAGE_SCROLL_SPEED;
            
            // Reset message when it goes off screen
            double diagonalLength = Math.sqrt(BOARD_WIDTH * BOARD_WIDTH + BOARD_HEIGHT * BOARD_HEIGHT);
            if (messageScrollOffset > diagonalLength + 200) {
                showMotivationalMessage = false;
                messageScrollOffset = 0;
                pauseTimer = 0; // Reset timer for next message
            }
        }
    }
    
    private void generateMotivationalMessage() {
        int playerScore = score1;
        int aiScore = score2;
        int scoreDifference = playerScore - aiScore;
        String difficultyName = aiDifficultyOptions[aiDifficultySetting];
        int totalPoints = playerScore + aiScore;
        double winRate = totalPoints > 0 ? (double)playerScore / totalPoints : 0.0;
        
        // COMPREHENSIVE PERFORMANCE-BASED MESSAGE SYSTEM
        
        // DEVASTATION TIER - Player getting absolutely destroyed
        if (playerScore == 0 && aiScore >= 9) {
            String[] devastatingMessages = {
                "Forse dovresti provare un altro gioco...",
                "L'IA ti sta umiliando completamente!",
                "Zero punti? Davvero?",
                "Stai giocando con i piedi?",
                "Neanche un punto... impressionante!",
                "L'IA si sta annoiando a vincere cosi facilmente",
                "Hai mai giocato a Pong prima?",
                "Questo e il peggior punteggio mai visto!",
                "L'IA potrebbe giocare a occhi chiusi!",
                "Fai prima a premere ESC e tornare al menu...",
                "La paddle sembra decorativa a questo punto",
                "L'IA sta pensando ad altro mentre ti batte",
                "Neanche i bambini di 3 anni fanno cosi male!",
                "Stai battendo tutti i record... di sconfitta!"
            };
            currentMotivationalMessage = devastatingMessages[(int)(Math.random() * devastatingMessages.length)];
        }
        // DESTRUCTION TIER - Player getting destroyed (0 points, 6-8 AI)
        else if (playerScore == 0 && aiScore >= 6) {
            String[] harshMessages = {
                "Qui ti serve un miracolo!",
                "L'IA sta dominando totalmente!",
                "Zero punti non e un buon segno...",
                "Forse e troppo difficile per te?",
                "L'IA ti sta distruggendo!",
                "Concentrazione sotto zero!",
                "La situazione e drammatica!",
                "Stai regalando punti all'IA!",
                "Ogni tiro dell'IA e un goal!",
                "La paddle e solo per decorazione?",
                "L'IA non fa neanche fatica!",
                "Prova a diminuire la difficolta..."
            };
            currentMotivationalMessage = harshMessages[(int)(Math.random() * harshMessages.length)];
        }
        // TERRIBLE PERFORMANCE - 1 point vs many
        else if (playerScore == 1 && aiScore >= 8) {
            String[] terribleMessages = {
                "Un punto di consolazione...",
                "Almeno hai evitato il cappotto!",
                "L'IA ha avuto pieta di te per quel punto",
                "Un punto su tanti... qualcosa e qualcosa!",
                "L'IA ti ha regalato quel punto?",
                "Prestazione da dimenticare!",
                "Un punto fortunoso non cambia la sostanza",
                "L'IA stava forse distratta?"
            };
            currentMotivationalMessage = terribleMessages[(int)(Math.random() * terribleMessages.length)];
        }
        // VERY BAD PERFORMANCE - 1-2 points vs 5+ AI
        else if (playerScore <= 2 && aiScore >= 5) {
            String[] veryBadMessages = {
                "Solo " + playerScore + " punti? Puoi fare di meglio!",
                "L'IA sta vincendo troppo facilmente",
                "Stai regalando tutti i punti all'avversario",
                "La situazione e disperata...",
                "Hai bisogno di piu allenamento!",
                "L'IA non fa neanche fatica a batterti",
                "I tuoi riflessi sono addormentati!",
                "Concentrati di piu sulla palla!",
                "Stai perdendo ogni duello!",
                "L'IA ti anticipa sempre!"
            };
            currentMotivationalMessage = veryBadMessages[(int)(Math.random() * veryBadMessages.length)];
        }
        // CATASTROPHIC BEHIND - 6+ points difference
        else if (scoreDifference <= -6) {
            String[] catastrophicMessages = {
                "Rimonta impossibile a questo punto!",
                "L'IA ha un vantaggio schiacciante!",
                "Sei troppo indietro per recuperare!",
                "La partita e praticamente finita!",
                "Differenza abissale nel punteggio!",
                "L'IA ti ha dato una lezione!",
                "Questo e dominio totale!",
                "Sei stato completamente surclassato!",
                "La differenza di classe e evidente!",
                "L'IA sta giocando in un'altra categoria!"
            };
            currentMotivationalMessage = catastrophicMessages[(int)(Math.random() * catastrophicMessages.length)];
        }
        // WAY BEHIND - 4-5 points difference
        else if (scoreDifference <= -4) {
            String[] wayBehindMessages = {
                "Concentrati di piu!",
                "La rimonta e ancora possibile!",
                "Non e finita finche non e finita!",
                "Svegliati! Stai perdendo!",
                "L'IA ha troppo vantaggio!",
                "Devi reagire subito!",
                "Stai collezionando sconfitte...",
                "Il divario si sta allargando!",
                "Ogni punto perso pesa di piu!",
                "Devi cambiare strategia!",
                "L'IA ti sta dominando mentalmente!",
                "Ritrova la concentrazione perduta!"
            };
            currentMotivationalMessage = wayBehindMessages[(int)(Math.random() * wayBehindMessages.length)];
        }
        // BEHIND - 2-3 points difference
        else if (scoreDifference <= -2) {
            currentMotivationalMessage = getRandomMessage("MSG_BEHIND", 10);
        }
        // PERFECT DOMINATION - Player 7+ points ahead
        else if (scoreDifference >= 7) {
            currentMotivationalMessage = getRandomMessage("MSG_PERFECT", 11);
        }
        // EXCELLENT DOMINATION - Player 5-6 points ahead
        else if (scoreDifference >= 5) {
            currentMotivationalMessage = getRandomMessage("MSG_EXCELLENT", 8);
        }
        // GREAT PERFORMANCE - Player 3-4 points ahead
        else if (scoreDifference >= 3) {
            currentMotivationalMessage = getRandomMessage("MSG_GREAT", 10);
        }
        // GOOD PERFORMANCE - Player 2 points ahead
        else if (scoreDifference == 2) {
            currentMotivationalMessage = getRandomMessage("MSG_GOOD", 7);
        }
        // SLIGHT ADVANTAGE - Player 1 point ahead
        else if (scoreDifference == 1) {
            currentMotivationalMessage = getRandomMessage("MSG_SLIGHT", 8);
        }
        // PERFECT TIE - Same score
        else if (scoreDifference == 0) {
            currentMotivationalMessage = getRandomMessage("MSG_TIE", 10);
        }
        
        // SPECIAL CONTEXTUAL MESSAGES
        
        // Early game (0-2 total points)
        else if (totalPoints <= 2) {
            currentMotivationalMessage = getRandomMessage("MSG_EARLY", 7);
        }
        // Late game close (12+ total points, close score)
        else if (totalPoints >= 12 && Math.abs(scoreDifference) <= 1) {
            currentMotivationalMessage = getRandomMessage("MSG_LATE_CLOSE", 10);
        }
        
        // DIFFICULTY-SPECIFIC MESSAGES
        
        // Dominating on EASY/FACILE
        else if (scoreDifference >= 3 && aiDifficultySetting == 0) {
            String[] easyDominationMessages = {
                "Troppo facile! Prova NORMALE!",
                "L'IA FACILE non e una sfida per te!",
                "Aumenta la difficolta per divertirti!",
                "Sei pronto per una sfida maggiore!",
                "Difficolta FACILE superata brillantemente!",
                "Tempo di alzare l'asticella!"
            };
            currentMotivationalMessage = easyDominationMessages[(int)(Math.random() * easyDominationMessages.length)];
        }
        // Dominating on NORMAL/NORMALE  
        else if (scoreDifference >= 3 && aiDifficultySetting == 1) {
            String[] normalDominationMessages = {
                "NORMALE e troppo semplice per te!",
                "Prova la difficolta DIFFICILE!",
                "Hai superato il livello NORMALE!",
                "L'IA NORMALE e sotto il tuo livello!",
                "Sfida te stesso con DIFFICILE!",
                "Tempo di una sfida piu seria!"
            };
            currentMotivationalMessage = normalDominationMessages[(int)(Math.random() * normalDominationMessages.length)];
        }
        // Doing well on DIFFICULT/DIFFICILE
        else if (scoreDifference >= 2 && aiDifficultySetting == 2) {
            String[] difficultGoodMessages = {
                "Bravo! DIFFICILE e tosta ma ce la fai!",
                "L'IA DIFFICILE e sotto controllo!",
                "Prestazione di livello superiore!",
                "DIFFICILE ma non per te!",
                "Stai battendo l'IA avanzata!",
                "Talento e allenamento pagano!",
                "L'IA DIFFICILE ti rispetta!"
            };
            currentMotivationalMessage = difficultGoodMessages[(int)(Math.random() * difficultGoodMessages.length)];
        }
        // Doing well on EXPERT/ESPERTO
        else if (scoreDifference >= 1 && aiDifficultySetting == 3) {
            String[] expertGoodMessages = {
                "Incredibile! Stai battendo l'ESPERTO!",
                "L'IA ESPERTO e impressionata!",
                "Prestazione da professionista!",
                "Hai raggiunto il livello ESPERTO!",
                "L'IA ESPERTO non sa come fermarti!",
                "Sei diventato un maestro!",
                "Livello ESPERTO sotto controllo!",
                "Prestazione leggendaria!"
            };
            currentMotivationalMessage = expertGoodMessages[(int)(Math.random() * expertGoodMessages.length)];
        }
        // ANY point against IMPOSSIBLE/IMPOSSIBILE
        else if (playerScore > 0 && aiDifficultySetting == 4) {
            String[] impossibleMessages = {
                "IMPOSSIBILE? Non per te!",
                "Hai segnato contro l'IMPOSSIBILE!",
                "L'IA IMPOSSIBILE ha un degno avversario!",
                "Stai sfidando l'impossibile!",
                "Anche l'IMPOSSIBILE puo essere battuto!",
                "Prestazione oltre ogni aspettativa!",
                "L'IA IMPOSSIBILE e sotto shock!",
                "Hai reso possibile l'impossibile!",
                "Leggenda in azione!",
                "L'IMPOSSIBILE incontra il suo maestro!"
            };
            currentMotivationalMessage = impossibleMessages[(int)(Math.random() * impossibleMessages.length)];
        }
        // Losing badly on EASY - should be impossible
        else if (scoreDifference <= -3 && aiDifficultySetting == 0) {
            String[] easyFailureMessages = {
                "Perdere contro FACILE? Davvero?",
                "L'IA FACILE ti sta battendo...",
                "Anche FACILE e troppo per te?",
                "Forse e meglio un tutorial?",
                "L'IA FACILE e impietosa!",
                "Incredibile ma vero... perdi in FACILE!"
            };
            currentMotivationalMessage = easyFailureMessages[(int)(Math.random() * easyFailureMessages.length)];
        }
        
        // ITALIAN MOTIVATIONAL SPORTS QUOTES (from internet research)
        else {
            String[] inspirationalQuotes = {
                "Non e mai troppo tardi per diventare cio che avresti potuto essere!",
                "La differenza tra l'ordinario e lo straordinario e quel piccolo extra!",
                "Il talento vince le partite, ma il lavoro di squadra vince i campionati!",
                "Non smettere mai di sognare, perche i sogni sono il carburante del successo!",
                "Ogni campione e stato una volta un principiante che non si e mai arreso!",
                "La vittoria appartiene al piu tenace!",
                "Non conta quanto cadi, ma quanto velocemente ti rialzi!",
                "Il successo e la somma di piccoli sforzi ripetuti giorno dopo giorno!",
                "Chi non risica non rosica!",
                "La pratica rende perfetti!",
                "Non arrenderti mai!",
                "Credi in te stesso!",
                "La concentrazione e tutto!",
                "Migliora ad ogni partita!",
                "Il segreto e nella costanza!",
                "Anche i campioni sbagliano qualche volta!",
                "L'esperienza conta!",
                "Pazienza e determinazione!",
                "Ogni errore e una lezione!",
                "La pressione fa i diamanti!",
                "Solo chi osa davvero puo volare!",
                "I limiti esistono solo nella tua mente!",
                "Trasforma ogni ostacolo in opportunita!",
                "La perseveranza e la chiave del successo!",
                "Ogni giorno e una nuova possibilita di migliorare!",
                "Il fallimento e solo l'inizio del successo!",
                "Chi lotta puo perdere, chi non lotta ha gia perso!",
                "La forza non viene dalle capacita fisiche ma dalla volonta!",
                "Sii il cambiamento che vuoi vedere nel mondo!",
                "L'impossibile e solo un'opinione!",
                "Ieri e storia, domani e un mistero, oggi e un dono!",
                "Non contare i giorni, fai in modo che i giorni contino!",
                "Il coraggio non e l'assenza di paura, ma l'azione nonostante la paura!",
                "Ogni esperto e stato una volta un principiante!",
                "La strada per il successo e sempre in costruzione!",
                "Non e mai troppo tardi per essere cio che avresti voluto essere!"
            };
            currentMotivationalMessage = getRandomMessage("MSG_INSPIRATIONAL", 20);
        }
    }
    
    private void updateDemoBall() {
        // Move ball
        demoBallX += demoBallVX;
        demoBallY += demoBallVY;
        
        // Bounce off top and bottom walls
        if (demoBallY <= 0) {
            demoBallY = 0;
            demoBallVY = -demoBallVY;
        }
        if (demoBallY >= BOARD_HEIGHT - BALL_SIZE) {
            demoBallY = BOARD_HEIGHT - BALL_SIZE;
            demoBallVY = -demoBallVY;
        }
        
        // Calculate paddle positions
        double progress = demoTransitionProgress;
        int startX = (int)(15 * scaleX);
        int endX = (int)(20 * scaleX);
        int bluePaddleX = (int)(startX + (endX - startX) * progress);
        
        int panelWidth = (int)(BOARD_WIDTH * 0.4);
        int panelStartX = BOARD_WIDTH - panelWidth;
        int currentPanelX = (int)(BOARD_WIDTH + (panelStartX - BOARD_WIDTH) * progress);
        int redPaddleX = currentPanelX - PADDLE_WIDTH - (int)(10 * scaleX);
        
        // Check if ball passes through paddles (reset ball)
        boolean ballPassedBlue = (demoBallX < bluePaddleX - BALL_SIZE && demoBallVX < 0);
        boolean ballPassedRed = (demoBallX > redPaddleX + PADDLE_WIDTH && demoBallVX > 0);
        
        if (ballPassedBlue || ballPassedRed) {
            // Reset ball to center between paddles
            initializeDemoBall();
        }
        
        // Paddle collisions
        if (demoBallX <= bluePaddleX + PADDLE_WIDTH && 
            demoBallX + BALL_SIZE >= bluePaddleX &&
            demoBallY + BALL_SIZE >= demoPaddleY && 
            demoBallY <= demoPaddleY + PADDLE_HEIGHT &&
            demoBallVX < 0) {
            
            demoBallVX = -demoBallVX;
            demoBallX = bluePaddleX + PADDLE_WIDTH + 1;
            
            int paddleCenter = (int)demoPaddleY + PADDLE_HEIGHT / 2;
            int ballCenter = (int)demoBallY + BALL_SIZE / 2;
            int diff = ballCenter - paddleCenter;
            demoBallVY += diff / 15.0;
        }
        
        if (demoBallX + BALL_SIZE >= redPaddleX && 
            demoBallX <= redPaddleX + PADDLE_WIDTH &&
            demoBallY + BALL_SIZE >= demoRedPaddleY && 
            demoBallY <= demoRedPaddleY + PADDLE_HEIGHT &&
            demoBallVX > 0) {
            
            demoBallVX = -demoBallVX;
            demoBallX = redPaddleX - BALL_SIZE - 1;
            
            int paddleCenter = (int)demoRedPaddleY + PADDLE_HEIGHT / 2;
            int ballCenter = (int)demoBallY + BALL_SIZE / 2;
            int diff = ballCenter - paddleCenter;
            demoBallVY += diff / 15.0;
        }
    }
    
    private void initializeDemoBall() {
        // Calculate paddle positions
        double progress = demoTransitionProgress;
        int startX = (int)(15 * scaleX);
        int endX = (int)(20 * scaleX);
        int bluePaddleX = (int)(startX + (endX - startX) * progress);
        
        int panelWidth = (int)(BOARD_WIDTH * 0.4);
        int panelStartX = BOARD_WIDTH - panelWidth;
        int currentPanelX = (int)(BOARD_WIDTH + (panelStartX - BOARD_WIDTH) * progress);
        int redPaddleX = currentPanelX - PADDLE_WIDTH - (int)(10 * scaleX);
        
        // Position ball at center between paddles
        int centerX = (bluePaddleX + PADDLE_WIDTH + redPaddleX) / 2;
        demoBallX = centerX - BALL_SIZE / 2;
        demoBallY = BOARD_HEIGHT / 2 - BALL_SIZE / 2;
        
        // Apply ball speed setting to demo ball too (scaled down for demo)
        double baseSpeed = (ballSpeedSetting / 20.0) * 4.0; // Scale numeric value
        
        demoBallVX = (Math.random() > 0.5) ? baseSpeed : -baseSpeed;
        demoBallVY = (Math.random() - 0.5) * baseSpeed;
    }
    
    private void updateDemoAI() {
        // Use selected AI difficulty setting for demo
        switch (aiDifficultySetting) {
            case 0: // FACILE - Simple following with mistakes
                updateDemoAI_Easy();
                break;
            case 1: // NORMALE - Predictive with reaction delay  
                updateDemoAI_Normal();
                break;
            case 2: // DIFFICILE - Advanced prediction with minimal errors
                updateDemoAI_Hard();
                break;
            case 3: // ESPERTO - Very strong AI
                updateDemoAI_Expert();
                break;
            case 4: // IMPOSSIBILE - Nearly perfect AI
                updateDemoAI_Impossible();
                break;
            default:
                updateDemoAI_Normal();
        }
        
        // Keep AI paddle within bounds
        demoRedPaddleY = Math.max(0, Math.min(demoRedPaddleY, BOARD_HEIGHT - PADDLE_HEIGHT));
    }
    
    private void updateDemoAI_Easy() {
        // Easy: Simple ball following with mistakes
        double ballCenterY = demoBallY + BALL_SIZE / 2;
        double paddleCenterY = demoRedPaddleY + PADDLE_HEIGHT / 2;
        
        // Get player paddle speed setting
        double baseSpeed = 4.0; // Lenta
        if (paddleSpeedSetting == 1) baseSpeed = 6.0; // Media  
        else if (paddleSpeedSetting == 2) baseSpeed = 8.0; // Veloce
        double playerSpeed = Math.max(3, (int)(baseSpeed * scaleY)); // Same as player paddle
        
        if (demoBallVX > 0 && demoBallX > BOARD_WIDTH * 0.5) {
            // 25% chance to make a mistake
            if (Math.random() < 0.25) return;
            
            // Simple following with error
            double error = (Math.random() - 0.5) * 50;
            double targetY = ballCenterY + error - PADDLE_HEIGHT / 2;
            
            // Much slower than player (20% of player speed)
            double diff = targetY - demoRedPaddleY;
            double maxSpeed = playerSpeed * 0.2;
            double moveAmount = Math.signum(diff) * Math.min(maxSpeed, Math.abs(diff) * 0.4);
            
            demoRedPaddleY += moveAmount;
        } else {
            // Return to center slowly
            double centerY = BOARD_HEIGHT / 2 - PADDLE_HEIGHT / 2;
            double diff = centerY - demoRedPaddleY;
            demoRedPaddleY += Math.signum(diff) * Math.min(playerSpeed * 0.1, Math.abs(diff) * 0.1);
        }
    }
    
    private void updateDemoAI_Normal() {
        // Medium: Predictive movement with some errors
        double ballCenterY = demoBallY + BALL_SIZE / 2;
        
        if (demoBallVX > 0 && demoBallX > BOARD_WIDTH * 0.3) {
            // Calculate demo paddle positions
            double progress = demoTransitionProgress;
            int panelWidth = (int)(BOARD_WIDTH * 0.4);
            int panelStartX = BOARD_WIDTH - panelWidth;
            int currentPanelX = (int)(BOARD_WIDTH + (panelStartX - BOARD_WIDTH) * progress);
            int redPaddleX = currentPanelX - PADDLE_WIDTH - (int)(10 * scaleX);
            
            // Simple prediction
            double timeToReach = (redPaddleX - demoBallX) / demoBallVX;
            double predictedY = demoBallY + demoBallVY * timeToReach;
            
            // Add some error
            double error = (Math.random() - 0.5) * 40;
            double targetY = predictedY + error + BALL_SIZE / 2 - PADDLE_HEIGHT / 2;
            
            // 12% chance of bigger mistake
            if (Math.random() < 0.12) {
                targetY += (Math.random() - 0.5) * 80;
            }
            
            // Get player paddle speed setting
            double baseSpeed = 4.0; // Lenta
            if (paddleSpeedSetting == 1) baseSpeed = 6.0; // Media  
            else if (paddleSpeedSetting == 2) baseSpeed = 8.0; // Veloce
            double playerSpeed = baseSpeed * scaleY;
            
            // Same speed as player (60% of player speed for balance)
            double diff = targetY - demoRedPaddleY;
            double maxSpeed = playerSpeed * 0.6;
            double moveAmount = Math.signum(diff) * Math.min(maxSpeed, Math.abs(diff) * 0.6);
            
            demoRedPaddleY += moveAmount;
        } else {
            // Return to center
            double centerY = BOARD_HEIGHT / 2 - PADDLE_HEIGHT / 2;
            double diff = centerY - demoRedPaddleY;
            // Get player paddle speed setting
            double baseSpeed = 4.0; // Lenta
            if (paddleSpeedSetting == 1) baseSpeed = 6.0; // Media  
            else if (paddleSpeedSetting == 2) baseSpeed = 8.0; // Veloce
            double playerSpeed = baseSpeed * scaleY;
            demoRedPaddleY += Math.signum(diff) * Math.min(playerSpeed * 0.25, Math.abs(diff) * 0.2);
        }
    }
    
    private void updateDemoAI_Hard() {
        // Hard: Advanced prediction with minimal errors
        // Get player paddle speed setting
        double baseSpeed = 4.0; // Lenta
        if (paddleSpeedSetting == 1) baseSpeed = 6.0; // Media  
        else if (paddleSpeedSetting == 2) baseSpeed = 8.0; // Veloce
        double playerSpeed = Math.max(3, (int)(baseSpeed * scaleY)); // Same as player paddle
        
        if (demoBallVX > 0) {
            // Advanced trajectory calculation for demo
            double predictionY = calculateDemoBallTrajectory();
            
            // Very small error - only 3% chance
            if (Math.random() < 0.03) {
                predictionY += (Math.random() - 0.5) * 30;
            }
            
            double targetY = predictionY - PADDLE_HEIGHT / 2;
            
            // Slightly faster than player (80% of player speed)
            double diff = targetY - demoRedPaddleY;
            double maxSpeed = playerSpeed * 0.8;
            double moveAmount = Math.signum(diff) * Math.min(maxSpeed, Math.abs(diff) * 0.8);
            
            demoRedPaddleY += moveAmount;
        } else {
            // Strategic positioning
            double strategicY = BOARD_HEIGHT * 0.5 - PADDLE_HEIGHT / 2;
            double diff = strategicY - demoRedPaddleY;
            demoRedPaddleY += Math.signum(diff) * Math.min(playerSpeed * 0.3, Math.abs(diff) * 0.3);
        }
    }
    
    private double calculateDemoBallTrajectory() {
        // Simulate ball trajectory for demo
        double simBallX = demoBallX;
        double simBallY = demoBallY;
        double simBallVX = demoBallVX;
        double simBallVY = demoBallVY;
        
        // Calculate demo paddle positions
        double progress = demoTransitionProgress;
        int panelWidth = (int)(BOARD_WIDTH * 0.4);
        int panelStartX = BOARD_WIDTH - panelWidth;
        int currentPanelX = (int)(BOARD_WIDTH + (panelStartX - BOARD_WIDTH) * progress);
        int redPaddleX = currentPanelX - PADDLE_WIDTH - (int)(10 * scaleX);
        
        // Simulate until ball reaches paddle
        while (simBallX < redPaddleX && simBallVX > 0) {
            simBallX += simBallVX;
            simBallY += simBallVY;
            
            // Wall bounces
            if (simBallY <= 0 || simBallY >= BOARD_HEIGHT - BALL_SIZE) {
                simBallVY = -simBallVY;
                simBallY = Math.max(0, Math.min(BOARD_HEIGHT - BALL_SIZE, simBallY));
            }
            
            // Safety check
            if (simBallX > BOARD_WIDTH * 2) break;
        }
        
        return simBallY + BALL_SIZE / 2;
    }
    
    private void updateDemoAI_Expert() {
        // Expert: Very strong AI for demo
        // Get player paddle speed setting
        double baseSpeed = 4.0; // Lenta
        if (paddleSpeedSetting == 1) baseSpeed = 6.0; // Media  
        else if (paddleSpeedSetting == 2) baseSpeed = 8.0; // Veloce
        double playerSpeed = Math.max(3, (int)(baseSpeed * scaleY)); // Same as player paddle
        
        if (demoBallVX > 0) {
            double predictionY = calculateDemoBallTrajectory();
            
            // Very small error - only 2% chance
            if (Math.random() < 0.02) {
                predictionY += (Math.random() - 0.5) * 25;
            }
            
            double targetY = predictionY - PADDLE_HEIGHT / 2;
            
            // Same speed as player
            double diff = targetY - demoRedPaddleY;
            double maxSpeed = playerSpeed;
            double moveAmount = Math.signum(diff) * Math.min(maxSpeed, Math.abs(diff) * 1.2);
            
            demoRedPaddleY += moveAmount;
        } else {
            // Strategic positioning
            double strategicY = BOARD_HEIGHT * 0.5 - PADDLE_HEIGHT / 2;
            double diff = strategicY - demoRedPaddleY;
            demoRedPaddleY += Math.signum(diff) * Math.min(playerSpeed * 0.4, Math.abs(diff) * 0.4);
        }
    }
    
    private void updateDemoAI_Impossible() {
        // Impossible: Nearly perfect AI for demo
        // Get player paddle speed setting
        double baseSpeed = 4.0; // Lenta
        if (paddleSpeedSetting == 1) baseSpeed = 6.0; // Media  
        else if (paddleSpeedSetting == 2) baseSpeed = 8.0; // Veloce
        double playerSpeed = Math.max(3, (int)(baseSpeed * scaleY)); // Same as player paddle
        
        if (demoBallVX > 0) {
            double predictionY = calculateDemoBallTrajectory();
            
            // Almost no error - 0.2% chance
            if (Math.random() < 0.002) {
                predictionY += (Math.random() - 0.5) * 10;
            }
            
            double targetY = predictionY - PADDLE_HEIGHT / 2;
            
            // Same speed as player (100% of player speed)
            double diff = targetY - demoRedPaddleY;
            double maxSpeed = playerSpeed;
            double moveAmount = Math.signum(diff) * Math.min(maxSpeed, Math.abs(diff) * 2.0);
            
            demoRedPaddleY += moveAmount;
        } else {
            // Perfect strategic positioning
            double strategicY = BOARD_HEIGHT * 0.5 - PADDLE_HEIGHT / 2;
            double diff = strategicY - demoRedPaddleY;
            demoRedPaddleY += Math.signum(diff) * Math.min(playerSpeed * 0.5, Math.abs(diff) * 0.6);
        }
    }
    
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
    
    private static String getHistoryFilePath() {
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
        
        return directory.getAbsolutePath() + File.separator + "game_history.txt";
    }
    
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
    
    private void saveSettings() {
        try {
            Properties props = new Properties();
            props.setProperty("paddleSpeed", String.valueOf(paddleSpeedSetting));
            props.setProperty("aiDifficulty", String.valueOf(aiDifficultySetting));
            props.setProperty("ballSpeed", String.valueOf(ballSpeedSetting));
            props.setProperty("player1UpKey", String.valueOf(player1UpKey));
            props.setProperty("player1DownKey", String.valueOf(player1DownKey));
            props.setProperty("player2UpKey", String.valueOf(player2UpKey));
            props.setProperty("player2DownKey", String.valueOf(player2DownKey));
            props.setProperty("firstRun", "false");
            
            props.store(new FileOutputStream(SETTINGS_FILE), "PongPing Game Settings");
        } catch (Exception e) {
            System.err.println("Failed to save settings: " + e.getMessage());
        }
    }
    
    private void checkWinCondition() {
        if (score1 >= WINNING_SCORE) {
            winner = currentState == GameState.SINGLE_PLAYER ? "PLAYER" : "PLAYER 1";
            
            // Show rank screen for single player mode
            if (currentState == GameState.SINGLE_PLAYER) {
                finalRank = calculateRank();
                showRankScreen = true;
                rankAnimationFrame = 0;
                
                // Reset rank screen animation
                rankPaddleTransitionComplete = false;
                rankTextTransitionStarted = false;
                rankPaddleProgress = 0.0;
                rankTextProgress = 0.0;
                
                // Reset scrolling text animation
                scrollingTextStarted = false;
                scrollingTextEntryComplete = false;
                showingDifficultyPhase = true;
                gameInfoTransitionStarted = false;
                difficultyHasBeenCovered = false; // Reset per nuova rank screen
                scrollingTextDropProgress = 0.0;
                gameInfoSlideProgress = 0.0;
                difficultyDisplayFrames = 0;
            }
            
            // Record game end time
            if (gameEndTime == 0) {
                gameEndTime = System.currentTimeMillis();
            }
            
            // Save game history entry
            saveGameResult();
            
            setState(GameState.GAME_OVER);
        } else if (score2 >= WINNING_SCORE) {
            winner = currentState == GameState.SINGLE_PLAYER ? "COMPUTER" : "PLAYER 2";
            
            // Show rank screen for single player mode (even if player loses)
            if (currentState == GameState.SINGLE_PLAYER) {
                finalRank = calculateRank();
                showRankScreen = true;
                rankAnimationFrame = 0;
                
                // Reset rank screen animation
                rankPaddleTransitionComplete = false;
                rankTextTransitionStarted = false;
                rankPaddleProgress = 0.0;
                rankTextProgress = 0.0;
                
                // Reset scrolling text animation
                scrollingTextStarted = false;
                scrollingTextEntryComplete = false;
                showingDifficultyPhase = true;
                gameInfoTransitionStarted = false;
                difficultyHasBeenCovered = false; // Reset per nuova rank screen
                scrollingTextDropProgress = 0.0;
                gameInfoSlideProgress = 0.0;
                difficultyDisplayFrames = 0;
            }
            
            // Record game end time
            if (gameEndTime == 0) {
                gameEndTime = System.currentTimeMillis();
            }
            
            // Save game history entry
            saveGameResult();
            
            setState(GameState.GAME_OVER);
        }
    }
    
    public void resetBall() {
        ballX = BOARD_WIDTH / 2 - BALL_SIZE / 2;
        ballY = BOARD_HEIGHT / 2 - BALL_SIZE / 2;
        
        // Clear ball trail
        ballTrailPoints.clear();
        
        // Note: Fire ball system reset is handled manually in scoring code to ensure proper timing
        
        // Track rally statistics for adaptive AI
        if (currentRallyStartTime > 0) {
            long rallyDuration = System.currentTimeMillis() - currentRallyStartTime;
            rallyDurations.add(rallyDuration);
            rallyHitCounts.add(currentRallyHits);
            
            // Keep only recent rally history (last 15 rallies)
            if (rallyDurations.size() > 15) {
                rallyDurations.remove(0);
                rallyHitCounts.remove(0);
            }
            
            // Update average rally performance
            averageRallyLength = rallyHitCounts.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        }
        
        // Always start with base speed, scaled for window size
        double initialSpeed = BASE_BALL_SPEED * Math.min(scaleX, scaleY);
        
        // First few balls in single player should favor the player (go left)
        if (currentState == GameState.SINGLE_PLAYER && (score1 + score2) < 3) {
            ballVX = -initialSpeed; // Always go toward player first
        } else {
            ballVX = (Math.random() > 0.5) ? initialSpeed : -initialSpeed;
        }
        
        ballVY = (random.nextDouble() * initialSpeed * 2 - initialSpeed);
        if (Math.abs(ballVY) < initialSpeed/4) ballVY = initialSpeed/2;
        
        // Set max ball speed for physics calculations (what the ball can reach)
        this.maxBallSpeed = ballSpeedSetting; // Range 5-100
        
        // Reset rally hit counter and start new rally tracking
        currentRallyHits = 0;
        currentRallyStartTime = System.currentTimeMillis();
    }
    
    public void startNewGame(boolean singlePlayer) {
        score1 = 0;
        score2 = 0;
        rallies = 0;
        // Reset combo systems based on game mode
        comboCount = 0; // Reset single player combo
        maxCombo = 0; // Reset single player max combo
        
        // Reset two players combo systems
        player1ComboCount = 0;
        player1MaxCombo = 0;
        player2ComboCount = 0;
        player2MaxCombo = 0;
        
        // Reset old right combo system (for compatibility)
        rightComboCount = 0;
        
        // Reset ranking system
        finalRank = "";
        showRankScreen = false;
        rankAnimationFrame = 0;
        
        // Reset fire ball system at game start
        resetFireBallSystem();
        
        // Reset adaptive AI tracking
        playerWinStreak = 0;
        aiWinStreak = 0;
        lastPointWinner = 0;
        
        // Reset advanced tracking
        rallyDurations.clear();
        rallyHitCounts.clear();
        ballSpeedHistory.clear();
        currentRallyStartTime = 0;
        consecutiveMissedShots = 0;
        averageRallyLength = 0.0;
        // Center paddles based on current screen size
        paddle1Y = (BOARD_HEIGHT - PADDLE_HEIGHT) / 2;
        paddle2Y = (BOARD_HEIGHT - PADDLE_HEIGHT) / 2;
        particles.clear();
        gameStartTime = System.currentTimeMillis();
        gameEndTime = 0; // Reset end time for new game
        
        // Reset AI variables
        aiPaddleY = (BOARD_HEIGHT - PADDLE_HEIGHT) / 2.0;
        aiCurrentVelocity = 0.0;
        aiTargetY = (BOARD_HEIGHT - PADDLE_HEIGHT) / 2.0;
        lastAIUpdate = 0;
        
        // Initialize transition
        menuPaddle1Y = 0;
        menuPaddle2Y = 0;
        transitionProgress = 0.0;
        isTransitioning = true;
        transitionTarget = singlePlayer ? GameState.SINGLE_PLAYER : GameState.PLAYING;
        
        resetBall();
        currentState = GameState.TRANSITIONING;
    }
    
    // Game loop with fixed timestep
    protected void startGameLoop() {
        gameRunning = true;
        gameLoopThread = new Thread(() -> {
            long lastTime = System.nanoTime();
            long accumulator = 0L;
            
            while (gameRunning) {
                long currentTime = System.nanoTime();
                long deltaTime = currentTime - lastTime;
                lastTime = currentTime;
                
                accumulator += deltaTime;
                
                // Fixed timestep update
                while (accumulator >= LOGIC_TIME_STEP) {
                    updateGameLogic();
                    accumulator -= LOGIC_TIME_STEP;
                }
                
                // Request repaint on EDT
                SwingUtilities.invokeLater(this::repaint);
                
                // Sleep to prevent busy waiting
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        gameLoopThread.setName("GameLoop");
        gameLoopThread.setDaemon(true);
        gameLoopThread.start();
    }
    
    private void stopGameLoop() {
        gameRunning = false;
        if (gameLoopThread != null) {
            try {
                gameLoopThread.join(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    // Renamed move() to updateGameLogic() for clarity
    private void updateGameLogic() {
        move(); // Keep existing logic for now
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // Timer now only used for resize animations, not game logic
        // Game logic runs on separate thread with fixed timestep
        // No repaint() here - handled by game loop
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        // Block all input during any transition
        if (isAnyTransitionActive()) {
            return;
        }
        
        // Debug key removed
        
        switch (currentState) {
            case SETTINGS:
                handleSettingsInput(e);
                break;
            case MENU:
                handleMenuInput(e);
                break;
            case TRANSITIONING:
                // No input during transition
                break;
            case PLAYING:
            case SINGLE_PLAYER:
                handleGameInput(e);
                break;
            case PAUSED:
                handlePauseInput(e);
                break;
            case GAME_OVER:
                handleGameOverInput(e);
                break;
            case GAME_MODE_SELECTION:
                handleGameModeSelectionInput(e);
                break;
            case BACKGROUND_SELECTION:
                handleBackgroundSelectionInput(e);
                break;
            case PADDLE_SELECTION:
                handlePaddleSelectionInput(e);
                break;
            case DEBUG:
                handleDebugInput(e);
                break;
            case RIGHT_PADDLE_SELECTION:
                handleRightPaddleSelectionInput(e);
                break;
            case FIRST_ACCESS:
                handleFirstAccessInput(e);
                break;
            case RANK:
                handleRankInput(e);
                break;
            case HISTORY:
                handleHistoryInput(e);
                break;
        }
    }
    
    private void handleRankInput(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            // Start rank-to-home transition
            startRankToHomeTransition();
        }
    }
    
    private void handleSettingsInput(KeyEvent e) {
        int keyCode = e.getKeyCode();
        System.out.println("DEBUG (Input): handleSettingsInput chiamato con tasto: " + keyCode + " (" + KeyEvent.getKeyText(keyCode) + ")");
        
        // If waiting for key input, handle special navigation
        if (waitingForKeyInput >= 3 && waitingForKeyInput <= 6) {
            
            if (keyCode == KeyEvent.VK_ENTER) {
                // Confirm current key selection
                waitingForKeyInput = -1;
                return;
            } else if (keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_BACK_SPACE) {
                // Cancel key selection
                waitingForKeyInput = -1;
                return;
            } else if (keyCode != KeyEvent.VK_ESCAPE && keyCode != KeyEvent.VK_ENTER && keyCode != KeyEvent.VK_BACK_SPACE) {
                // Direct key assignment - accept ANY key including arrow keys
                switch (waitingForKeyInput) {
                    case 3: player1UpKey = keyCode; break;
                    case 4: player1DownKey = keyCode; break;
                    case 5: player2UpKey = keyCode; break;
                    case 6: player2DownKey = keyCode; break;
                }
                waitingForKeyInput = -1; // Stop waiting
                // Save immediately when key is assigned
                saveSettingsToFile();
                return;
            }
        }
        
        
        // Normal navigation (not waiting for key input)
        switch (keyCode) {
            case KeyEvent.VK_LEFT:
                hideMouseCursor(); // Hide mouse when using keyboard navigation
                isUsingKeyboardNavigationSettings = true; // Enable keyboard navigation mode
                if (inCategoryColumn) {
                    // Already in category column, do nothing
                } else {
                    // In settings column: decrease value if possible, otherwise return to categories
                    String[] currentSettings = categorySettings[selectedCategory];
                    if (currentSettings.length > 0) {
                        String currentSetting = currentSettings[selectedCategorySetting];
                        // Check if this setting can be changed with arrows
                        if (canChangeSettingWithArrows(currentSetting)) {
                            // Try to decrease value, but if at minimum, go to categories
                            if (!tryDecreaseSetting()) {
                                inCategoryColumn = true; // Go to categories if can't decrease further
                            }
                        } else {
                            // Move to category column for non-changeable settings
                            inCategoryColumn = true;
                        }
                    } else {
                        inCategoryColumn = true;
                    }
                }
                break;
                
            case KeyEvent.VK_RIGHT:
                hideMouseCursor(); // Hide mouse when using keyboard navigation
                isUsingKeyboardNavigationSettings = true; // Enable keyboard navigation mode
                if (inCategoryColumn) {
                    // Move to settings column (right)
                    inCategoryColumn = false;
                    // Reset to first setting of current category
                    selectedCategorySetting = 0;
                } else {
                    // In settings column: increase value
                    String[] currentSettings = categorySettings[selectedCategory];
                    if (currentSettings.length > 0) {
                        String currentSetting = currentSettings[selectedCategorySetting];
                        // Check if this setting can be changed with arrows
                        if (canChangeSettingWithArrows(currentSetting)) {
                            changeCategorySetting(1); // Increase value
                        }
                    }
                }
                break;
                
            case KeyEvent.VK_TAB:
                // TAB always toggles between columns (best practice for UI navigation)
                // Only works in settings, not in demo mode
                if (!isDemoMode) {
                    inCategoryColumn = !inCategoryColumn;
                    if (!inCategoryColumn) {
                        // Moving to settings column, reset to first setting
                        selectedCategorySetting = 0;
                    }
                }
                break;
                
            case KeyEvent.VK_Q:
                // Alternative to TAB (more reliable) - toggles between columns
                inCategoryColumn = !inCategoryColumn;
                if (!inCategoryColumn) {
                    // Moving to settings column, reset to first setting
                    selectedCategorySetting = 0;
                }
                break;
                
            case KeyEvent.VK_UP:
                hideMouseCursor(); // Hide mouse when using keyboard navigation
                isUsingKeyboardNavigationSettings = true; // Enable keyboard navigation mode
                if (isDemoMode) {
                    demoPaddleUpPressed = true;
                } else if (inCategoryColumn) {
                    // Navigate up in categories
                    selectedCategory = (selectedCategory - 1 + categoryNames.length) % categoryNames.length;
                    selectedCategorySetting = 0; // Reset setting selection when changing category
                } else {
                    // Navigate up in settings of current category
                    String[] currentSettings = categorySettings[selectedCategory];
                    selectedCategorySetting = (selectedCategorySetting - 1 + currentSettings.length) % currentSettings.length;
                }
                break;
                
            case KeyEvent.VK_DOWN:
                hideMouseCursor(); // Hide mouse when using keyboard navigation
                isUsingKeyboardNavigationSettings = true; // Enable keyboard navigation mode
                if (isDemoMode) {
                    demoPaddleDownPressed = true;
                } else if (inCategoryColumn) {
                    // Navigate down in categories
                    selectedCategory = (selectedCategory + 1) % categoryNames.length;
                    selectedCategorySetting = 0; // Reset setting selection when changing category
                } else {
                    // Navigate down in settings of current category
                    String[] currentSettings = categorySettings[selectedCategory];
                    selectedCategorySetting = (selectedCategorySetting + 1) % currentSettings.length;
                }
                break;
                
            case KeyEvent.VK_ENTER:
                if (isDemoMode) {
                    applySettings();
                    // Start transition from demo to menu
                    isTransitioningDemoToMenu = true;
                    demoToMenuProgress = 0.0;
                } else if (!inCategoryColumn) {
                    // We're in the settings column - handle setting configuration
                    String currentSetting = categorySettings[selectedCategory][selectedCategorySetting];
                    
                    // Map to original setting index for key configuration
                    for (int i = 0; i < settingNames.length; i++) {
                        if (settingNames[i].equals(currentSetting)) {
                            if (i >= 3 && i <= 6) { // Key configuration settings
                                waitingForKeyInput = i;
                            }
                            break;
                        }
                    }
                } else {
                    // We're in category column - move to settings column
                    inCategoryColumn = false;
                    selectedCategorySetting = 0;
                }
                break;
                
            case KeyEvent.VK_W:
                if (isDemoMode) {
                    // Press demo paddle up with W key
                    demoPaddleUpPressed = true;
                }
                break;
            case KeyEvent.VK_S:
                if (isDemoMode) {
                    // Press demo paddle down with S key
                    demoPaddleDownPressed = true;
                }
                break;
            case KeyEvent.VK_SHIFT:
                if (isDemoMode) {
                    // Change AI difficulty when Shift is pressed
                    int oldValue = aiDifficultySetting;
                    aiDifficultySetting = (aiDifficultySetting + 1) % aiDifficultyOptions.length;
                    // Apply immediately
                    aiDifficulty = aiDifficultySetting + 1; // Convert to 1-5 range
                    System.out.println("DEBUG (Demo-Shift): Difficoltà IA cambiata da " + oldValue + " a " + aiDifficultySetting + " (aiDifficulty=" + aiDifficulty + ")");
                }
                break;
            case KeyEvent.VK_SPACE:
                if (isDemoMode) {
                    // Change AI difficulty when Space is pressed (regardless of selection)
                    int oldValue = aiDifficultySetting;
                    aiDifficultySetting = (aiDifficultySetting + 1) % aiDifficultyOptions.length;
                    // Apply immediately
                    aiDifficulty = aiDifficultySetting + 1; // Convert to 1-5 range
                    System.out.println("DEBUG (Demo-Space): Difficoltà IA cambiata da " + oldValue + " a " + aiDifficultySetting + " (aiDifficulty=" + aiDifficulty + ")");
                }
                break;
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_BACK_SPACE:
                // Go back to menu only if not in demo mode
                if (!isDemoMode) {
                    if (inCategoryColumn) {
                        // If in category column (left), check where to go back
                        if (isPaused) {
                            // Return to pause menu if we came from a game
                            setState(GameState.PAUSED);
                        } else {
                            // Exit to main menu with animated transition (inverse of home→settings)
                            startSettingsToHomeTransition();
                            // Reset ball position to prevent it from showing in menu
                            ballX = BOARD_WIDTH / 2 - BALL_SIZE / 2;
                            ballY = BOARD_HEIGHT / 2 - BALL_SIZE / 2;
                        }
                    } else {
                        // If in settings column (right), go back to category column (left)
                        inCategoryColumn = true;
                    }
                }
                break;
        }
    }
    
    private void changeCategorySetting(int direction) {
        String currentSetting = categorySettings[selectedCategory][selectedCategorySetting];
        
        
        // Handle audio settings directly
        if (selectedCategory == 3) { // Audio category
            switch (selectedCategorySetting) {
                case 0: // VOLUME MUSICA
                    musicVolume = Math.max(0, Math.min(100, musicVolume + direction * 5));
                    // Apply volume change to music
                    if (backgroundMusic != null && backgroundMusic.isOpen()) {
                        updateMusicVolume();
                    }
                    // Save immediately
                    saveSettingsToFile();
                    break;
                case 1: // VOLUME EFFETTI
                    effectsVolume = Math.max(0, Math.min(100, effectsVolume + direction * 5));
                    // Save immediately
                    saveSettingsToFile();
                    break;
                case 2: // MUSICA ATTIVA
                    if (direction != 0) {
                        musicEnabled = !musicEnabled;
                        if (musicEnabled && backgroundMusic != null) {
                            backgroundMusic.start();
                            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
                        } else if (backgroundMusic != null) {
                            backgroundMusic.stop();
                        }
                        // Save immediately
                        saveSettingsToFile();
                    }
                    break;
            }
            return;
        }
        
        // Handle language settings directly
        if (selectedCategory == 4) { // Language category
            switch (selectedCategorySetting) {
                case 0: // LINGUA GIOCO
                    if (direction != 0) {
                        switchLanguage();
                        updateLocalizedArrays();
                        // Save immediately
                        saveSettingsToFile();
                    }
                    break;
            }
            return;
        }
        
        // Map to original setting index for compatibility
        for (int i = 0; i < settingNames.length; i++) {
            if (settingNames[i].equals(currentSetting)) {
                changeSetting(direction, i);
                break;
            }
        }
    }
    
    private void changeSetting(int direction, int settingIndex) {
        switch (settingIndex) {
            case 0: // Velocità Paddle
                paddleSpeedSetting = Math.max(0, Math.min(2, paddleSpeedSetting + direction));
                // Save immediately
                saveSettingsToFile();
                break;
            case 1: // Difficoltà IA
                int oldValue = aiDifficultySetting;
                aiDifficultySetting = Math.max(0, Math.min(4, aiDifficultySetting + direction));
                // Apply immediately
                aiDifficulty = aiDifficultySetting + 1; // Convert to 1-5 range
                System.out.println("DEBUG: Difficoltà IA cambiata da " + oldValue + " a " + aiDifficultySetting + " (aiDifficulty=" + aiDifficulty + ")");
                // Save immediately
                saveSettingsToFile();
                break;
            case 2: // Velocità Palla (numeric 5-100)
                ballSpeedSetting = Math.max(5, Math.min(100, ballSpeedSetting + direction));
                // Save immediately
                saveSettingsToFile();
                break;
            case 3: // Tasto P1 Su
                if (direction > 0) waitingForKeyInput = 3;
                break;
            case 4: // Tasto P1 Giù
                if (direction > 0) waitingForKeyInput = 4;
                break;
            case 5: // Tasto P2 Su
                if (direction > 0) waitingForKeyInput = 5;
                break;
            case 6: // Tasto P2 Giù
                if (direction > 0) waitingForKeyInput = 6;
                break;
        }
    }
    
    private boolean tryDecreaseSetting() {
        // Try to decrease setting value, return false if already at minimum
        String currentSetting = categorySettings[selectedCategory][selectedCategorySetting];
        
        // Handle audio settings directly
        if (selectedCategory == 3) { // Audio category
            switch (selectedCategorySetting) {
                case 0: // VOLUME MUSICA
                    if (musicVolume > 0) {
                        changeCategorySetting(-1);
                        return true;
                    }
                    return false;
                case 1: // VOLUME EFFETTI
                    if (effectsVolume > 0) {
                        changeCategorySetting(-1);
                        return true;
                    }
                    return false;
                case 2: // MUSICA ATTIVA
                    // This is a toggle, so always allow change
                    changeCategorySetting(-1);
                    return true;
                default:
                    return false;
            }
        }
        
        // Map to original setting index
        for (int i = 0; i < settingNames.length; i++) {
            if (settingNames[i].equals(currentSetting)) {
                return tryChangeSetting(-1, i);
            }
        }
        return false;
    }
    
    private boolean tryChangeSetting(int direction, int settingIndex) {
        // Try to change setting, return false if at boundary
        System.out.println("DEBUG (tryChangeSetting): Tentativo di cambiare impostazione " + settingIndex + " con direction " + direction);
        switch (settingIndex) {
            case 0: // Velocità Paddle
                int newPaddleSpeed = paddleSpeedSetting + direction;
                if (newPaddleSpeed >= 0 && newPaddleSpeed <= 2) {
                    paddleSpeedSetting = newPaddleSpeed;
                    System.out.println("DEBUG (tryChangeSetting): Velocità Paddle cambiata a " + paddleSpeedSetting + " - SALVO!");
                    saveSettingsToFile();
                    return true;
                }
                return false;
            case 1: // Difficoltà IA
                int newAiDifficulty = aiDifficultySetting + direction;
                if (newAiDifficulty >= 0 && newAiDifficulty <= 4) {
                    int oldValue = aiDifficultySetting;
                    aiDifficultySetting = newAiDifficulty;
                    // Apply immediately
                    aiDifficulty = aiDifficultySetting + 1; // Convert to 1-5 range
                    System.out.println("DEBUG (tryChangeSetting): Difficoltà IA cambiata da " + oldValue + " a " + aiDifficultySetting + " (aiDifficulty=" + aiDifficulty + ") - ORA SALVO!");
                    // Save immediately
                    saveSettingsToFile();
                    return true;
                }
                System.out.println("DEBUG (tryChangeSetting): Difficoltà IA NON cambiata, fuori range: " + newAiDifficulty);
                return false;
            case 2: // Velocità Palla
                int newBallSpeed = ballSpeedSetting + direction;
                if (newBallSpeed >= 5 && newBallSpeed <= 100) {
                    ballSpeedSetting = newBallSpeed;
                    System.out.println("DEBUG (tryChangeSetting): Velocità Palla cambiata a " + ballSpeedSetting + " - SALVO!");
                    saveSettingsToFile();
                    return true;
                }
                return false;
            default:
                return false; // Key settings can't be changed this way
        }
    }
    
    private boolean canChangeSettingWithArrows(String settingId) {
        // Key configuration settings cannot be changed with arrows
        return !("SETTING_P1_UP".equals(settingId) ||
                 "SETTING_P1_DOWN".equals(settingId) ||
                 "SETTING_P2_UP".equals(settingId) ||
                 "SETTING_P2_DOWN".equals(settingId));
    }
    
    private void changeSetting(int direction) {
        changeSetting(direction, selectedSetting);
    }
    
    private void handleTestDemoToggle() {
        if (!isDemoMode) {
            // Start demo mode
            isDemoMode = true;
            isTransitioningToDemo = true;
            demoTransitionProgress = 0.0;
        } else {
            // Exit demo mode
            isDemoMode = false;
            isTransitioningFromDemo = true;
            demoTransitionProgress = 1.0;
        }
    }
    
    private int waitingForKeyInput = -1; // -1 = not waiting, 3-6 = waiting for key for specific setting
    
    private void applySettings() {
        // Apply paddle speed setting
        // This will be used in the move() method
        
        // Apply AI difficulty setting
        aiDifficulty = aiDifficultySetting + 1; // Convert to 1-5 range
        
        // Save settings to file
        saveSettingsToFile();
    }
    
    private void handleMenuInput(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                hideMouseCursor(); // Hide mouse when using keyboard navigation
                mouseOnBackground = false; // Reset background selection when using keyboard
                leftPaddleSelected = false; // Reset paddle selection when navigating menu
                rightPaddleSelected = false; // Reset right paddle selection when navigating menu
                isUsingKeyboardNavigation = true; // Enable keyboard navigation mode
                selectedMenuItem = (selectedMenuItem - 1 + menuItems.length) % menuItems.length;
                updateSecondBallState();
                break;
            case KeyEvent.VK_DOWN:
                hideMouseCursor(); // Hide mouse when using keyboard navigation
                mouseOnBackground = false; // Reset background selection when using keyboard
                leftPaddleSelected = false; // Reset paddle selection when navigating menu
                rightPaddleSelected = false; // Reset right paddle selection when navigating menu
                isUsingKeyboardNavigation = true; // Enable keyboard navigation mode
                selectedMenuItem = (selectedMenuItem + 1) % menuItems.length;
                updateSecondBallState();
                break;
            case KeyEvent.VK_LEFT:
                hideMouseCursor(); // Hide mouse when using keyboard navigation
                mouseOnBackground = false; // Reset background selection
                isUsingKeyboardNavigation = true; // Enable keyboard navigation mode
                
                // LEFT ARROW: only from right paddle → center, or center → left paddle
                if (rightPaddleSelected) {
                    // From right paddle, go to center menu
                    rightPaddleSelected = false;
                    leftPaddleSelected = false;
                    // Ensure we have a valid menu selection when returning to center
                    if (selectedMenuItem < 0 || selectedMenuItem >= menuItems.length) {
                        selectedMenuItem = 0; // Default to first menu item
                    }
                    updateSecondBallState(); // Update ball state when returning to menu
                } else if (!leftPaddleSelected && !rightPaddleSelected) {
                    // From center menu, go to left paddle
                    leftPaddleSelected = true;
                    rightPaddleSelected = false;
                }
                // If on left paddle, do nothing (can't go more left)
                repaint(); // Force repaint to show keyboard selection
                break;
                
            case KeyEvent.VK_RIGHT:
                hideMouseCursor(); // Hide mouse when using keyboard navigation
                mouseOnBackground = false; // Reset background selection
                isUsingKeyboardNavigation = true; // Enable keyboard navigation mode
                
                // RIGHT ARROW: only from left paddle → center, or center → right paddle
                if (leftPaddleSelected) {
                    // From left paddle, go to center menu
                    leftPaddleSelected = false;
                    rightPaddleSelected = false;
                    // Ensure we have a valid menu selection when returning to center
                    if (selectedMenuItem < 0 || selectedMenuItem >= menuItems.length) {
                        selectedMenuItem = 0; // Default to first menu item
                    }
                    updateSecondBallState(); // Update ball state when returning to menu
                } else if (!leftPaddleSelected && !rightPaddleSelected) {
                    // From center menu, go to right paddle
                    rightPaddleSelected = true;
                    leftPaddleSelected = false;
                }
                // If on right paddle, do nothing (can't go more right)
                repaint(); // Force repaint to show keyboard selection
                break;
            case KeyEvent.VK_TAB:
                // TAB key opens game mode selection
                hideMouseCursor();
                mouseOnBackground = false;
                leftPaddleSelected = false;
                rightPaddleSelected = false;
                setState(GameState.GAME_MODE_SELECTION);
                break;
            case KeyEvent.VK_ENTER:
                if (leftPaddleSelected) {
                    // Open paddle selection when left paddle is selected
                    startHomeToPaddleTransition(true); // true for left paddle
                } else if (rightPaddleSelected) {
                    // Open right paddle selection when right paddle is selected
                    startHomeToPaddleTransition(false); // false for right paddle
                } else if (selectedMenuItem == 0) {
                    startNewGame(true); // Single player
                } else if (selectedMenuItem == 1) {
                    startNewGame(false); // Two players
                } else if (selectedMenuItem == 2) {
                    setState(GameState.HISTORY); // History
                } else if (selectedMenuItem == 3) {
                    startHomeToSettingsTransition(); // Start animated transition
                    isDemoMode = false; // Ensure we're not in demo mode
                } else if (selectedMenuItem == 4) {
                    System.exit(0);
                }
                break;
            case KeyEvent.VK_SPACE:
                startHomeToThemesTransition(); // Use transition like background click
                selectedBackgroundOption = selectedBackground; // Start with current background
                break;
        }
    }
    
    private void handleDebugInput(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                debugSelection = (debugSelection - 1 + debugLabels.length) % debugLabels.length;
                break;
            case KeyEvent.VK_DOWN:
                debugSelection = (debugSelection + 1) % debugLabels.length;
                break;
            case KeyEvent.VK_PLUS:
            case KeyEvent.VK_EQUALS: // + key without shift
            case KeyEvent.VK_RIGHT: // Use right arrow as +
                adjustDebugValue(1);
                calculateDebugRank();
                break;
            case KeyEvent.VK_MINUS:
            case KeyEvent.VK_LEFT: // Use left arrow as -
                adjustDebugValue(-1);
                calculateDebugRank();
                break;
            case KeyEvent.VK_ESCAPE:
                setState(GameState.MENU);
                break;
        }
    }
    
    private void adjustDebugValue(int delta) {
        switch (debugSelection) {
            case 0: // Player 1 Score
                debugScore1 = Math.max(0, Math.min(20, debugScore1 + delta));
                break;
            case 1: // Player 2 Score
                debugScore2 = Math.max(0, Math.min(20, debugScore2 + delta));
                break;
            case 2: // Combos
                debugCombos = Math.max(0, Math.min(50, debugCombos + delta));
                break;
        }
    }

    private void handleBackgroundSelectionInput(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                hideMouseCursor(); // Hide mouse when using keyboard navigation
                selectedBackgroundOption = (selectedBackgroundOption - 1 + backgroundNames.size()) % backgroundNames.size();
                break;
            case KeyEvent.VK_RIGHT:
                hideMouseCursor(); // Hide mouse when using keyboard navigation
                selectedBackgroundOption = (selectedBackgroundOption + 1) % backgroundNames.size();
                break;
            case KeyEvent.VK_ENTER:
                selectedBackground = selectedBackgroundOption; // Apply selection
                loadTextColorsForTheme(); // Reload text colors for new theme
                saveSettingsToFile(); // Save the new background selection
                saveBackgroundTheme(); // Save theme to separate file
                startThemesToHomeTransition(); // Use transition instead of direct state change
                break;
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_BACK_SPACE:
                startThemesToHomeTransition(); // Use transition instead of direct state change
                break;
        }
    }
    
    private void drawDebug(Graphics2D g) {
        // Dark background
        g.setColor(new Color(20, 20, 30));
        g.fillRect(0, 0, getWidth(), getHeight());
        
        // Title
        g.setFont(primaryFont.deriveFont(48f * (float)scaleX));
        g.setColor(Color.CYAN);
        FontMetrics titleFm = g.getFontMetrics();
        String title = "DEBUG MODE";
        int titleX = (getWidth() - titleFm.stringWidth(title)) / 2;
        g.drawString(title, titleX, 100);
        
        // Instructions
        g.setFont(secondaryFont.deriveFont(16f * (float)scaleX));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        String[] instructions = {
            "Use Up/Down to select value",
            "Use Left/Right to adjust values",
            "Press 7 to exit debug mode"
        };
        
        int y = 150;
        for (String instruction : instructions) {
            int x = (getWidth() - fm.stringWidth(instruction)) / 2;
            g.drawString(instruction, x, y);
            y += 30;
        }
        
        // Debug info in top-right corner with normal font
        g.setFont(secondaryFont.deriveFont(14f * (float)scaleX));
        g.setColor(Color.LIGHT_GRAY);
        FontMetrics infoFm = g.getFontMetrics();
        
        String[] debugInfo = {
            "Max Combo: " + debugCombos,
            "Victory Margin: " + (debugScore1 - debugScore2),
            "Player Win Rate: " + (debugScore2 == 0 ? "100%" : String.format("%.1f%%", (debugScore1 * 100.0 / (debugScore1 + debugScore2)))),
            "Current Rank: " + currentRank,
            "Rank Points: " + calculateRankPoints(),
            "Perfect Game: " + (debugScore2 == 0 ? "YES" : "NO")
        };
        
        int infoY = 30;
        for (String info : debugInfo) {
            int infoX = getWidth() - infoFm.stringWidth(info) - 20; // 20px padding from right
            g.drawString(info, infoX, infoY);
            infoY += 20;
        }
        
        // Debug values
        g.setFont(primaryFont.deriveFont(24f * (float)scaleX));
        y = 300;
        
        for (int i = 0; i < debugLabels.length; i++) {
            // Highlight selected option
            if (i == debugSelection) {
                g.setColor(new Color(50, 50, 100, 150));
                g.fillRect(50, y - 25, getWidth() - 100, 40);
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.WHITE);
            }
            
            String label = debugLabels[i];
            int value;
            switch (i) {
                case 0: value = debugScore1; break;
                case 1: value = debugScore2; break;
                case 2: value = debugCombos; break;
                default: value = 0;
            }
            
            String text = label + ": " + value;
            g.drawString(text, 100, y);
            y += 60;
        }
        
        // Show rank screen overlay using existing method
        y += 30;
        // Temporarily set debug values and show rank screen
        int originalScore1 = score1;
        int originalScore2 = score2;
        int originalMaxCombo = maxCombo;
        String originalFinalRank = finalRank;
        
        // Set debug values
        score1 = debugScore1;
        score2 = debugScore2;
        maxCombo = debugCombos;
        finalRank = currentRank;
        
        // Draw the actual rank screen
        drawRankScreen(g);
        
        // Restore original values
        score1 = originalScore1;
        score2 = originalScore2;
        maxCombo = originalMaxCombo;
        finalRank = originalFinalRank;
    }


    private void drawBackgroundSelection(Graphics2D g) {
        // Draw game field background (selected theme preview)
        if (selectedBackgroundOption >= 0 && selectedBackgroundOption < backgroundImages.size()) {
            Image selectedImg = backgroundImages.get(selectedBackgroundOption);
            
            if (selectedImg != null) {
                // Draw full background image
                g.drawImage(selectedImg, 0, 0, getWidth(), getHeight(), this);
                // Add contrast effect like in game
                drawBackgroundContrastEffect(g);
            } else {
                // Default black background
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        } else {
            // Fallback
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        
        // Draw center line like in game
        g.setColor(new Color(255, 255, 255, 150));
        int lineSpacing = (int)(20 * scaleY);
        int lineWidth = Math.max(2, (int)(4 * scaleX));
        int lineHeight = (int)(10 * scaleY);
        for (int i = 0; i < getHeight(); i += lineSpacing) {
            g.fillRect(getWidth() / 2 - lineWidth/2, i, lineWidth, lineHeight);
        }
        
        // Title with better visibility
        g.setColor(new Color(0, 0, 0, 120)); // Dark background for title
        float titleSize = (float)(36 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(titleSize));
        FontMetrics titleFm = g.getFontMetrics();
        String title = getText("THEME_SELECTION_TITLE");
        int titleX = (BOARD_WIDTH - titleFm.stringWidth(title)) / 2;
        int titleY = (int)(50 * scaleY);
        
        // Title background
        g.fillRoundRect(titleX - 20, titleY - titleFm.getHeight() + 5, titleFm.stringWidth(title) + 40, titleFm.getHeight() + 10, 10, 10);
        g.setColor(Color.WHITE);
        g.drawString(title, titleX, titleY);
        
        // Bottom panel for theme selection
        int panelHeight = (int)(120 * scaleY);
        int panelY = getHeight() - panelHeight;
        
        // Semi-transparent dark panel
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, panelY, getWidth(), panelHeight);
        
        // Panel border
        g.setColor(new Color(100, 150, 255, 100));
        g.setStroke(new BasicStroke(2));
        g.drawLine(0, panelY, getWidth(), panelY);
        
        // Theme thumbnails in horizontal row (fixed 160x92 dimensions)
        int thumbWidth = (int)(160 * scaleX);
        int thumbHeight = (int)(92 * scaleY);
        int thumbSpacing = (int)(10 * Math.min(scaleX, scaleY));
        
        // Calculate total width and center position
        int totalWidth = backgroundNames.size() * thumbWidth + (backgroundNames.size() - 1) * thumbSpacing;
        int thumbStartX = (getWidth() - totalWidth) / 2;
        int thumbY = panelY + (panelHeight - thumbHeight) / 2; // Center vertically in panel
        
        // Draw theme thumbnails with fixed sizes
        for (int i = 0; i < backgroundNames.size(); i++) {
            int thumbX = thumbStartX + i * (thumbWidth + thumbSpacing);
            drawThemeThumbnail(g, i, thumbX, thumbY, thumbWidth, thumbHeight);
        }
        
        // Exit instruction at bottom left
        g.setColor(new Color(200, 200, 200));
        float instructSize = (float)(12 * Math.min(scaleX, scaleY));
        g.setFont(secondaryFont.deriveFont(instructSize));
        
        String instruction = getText("UI_ESC_BACKSPACE_EXIT");
        int instructX = (int)(10 * scaleX);
        int instructY = getHeight() - (int)(10 * scaleY);
        g.drawString(instruction, instructX, instructY);
        
        // Draw "by Gava" signature
        drawGavaSignature(g);
    }
    
    private void drawThemeThumbnail(Graphics2D g, int themeIndex, int x, int y, int width, int height) {
        boolean isSelected = (themeIndex == selectedBackgroundOption);
        boolean isActive = (themeIndex == selectedBackground);
        
        // Thumbnail shadow
        g.setColor(new Color(0, 0, 0, 100));
        g.fillRoundRect(x + 2, y + 2, width, height, 8, 8);
        
        // Thumbnail background
        if (isSelected) {
            g.setColor(new Color(100, 150, 255, 150)); // Blue selection
        } else {
            g.setColor(new Color(60, 60, 60)); // Dark gray
        }
        g.fillRoundRect(x, y, width, height, 8, 8);
        
        // Theme preview (full size, no margin)
        int previewMargin = 0;
        int previewX = x + previewMargin;
        int previewY = y + previewMargin;
        int previewWidth = width - (previewMargin * 2);
        int previewHeight = height - (previewMargin * 2); // Use full height
        
        if (themeIndex < backgroundImages.size()) {
            Image themeImg = backgroundImages.get(themeIndex);
            
            if (themeImg != null) {
                // Calculate scaled dimensions to fit height
                int imgWidth = themeImg.getWidth(this);
                int imgHeight = themeImg.getHeight(this);
                
                if (imgWidth > 0 && imgHeight > 0) {
                    // Scale based on height to maintain aspect ratio
                    double scale = (double)previewHeight / imgHeight;
                    int scaledWidth = (int)(imgWidth * scale);
                    int scaledHeight = previewHeight;
                    
                    // Center horizontally if scaled width is smaller than preview width
                    int drawX = previewX + (previewWidth - scaledWidth) / 2;
                    int drawWidth = Math.min(scaledWidth, previewWidth);
                    
                    // Clip to rounded corners
                    g.setClip(new java.awt.geom.RoundRectangle2D.Float(previewX, previewY, previewWidth, previewHeight, 5, 5));
                    g.drawImage(themeImg, drawX, previewY, drawWidth, scaledHeight, this);
                    g.setClip(null);
                } else {
                    // Fallback if image dimensions not available
                    g.setClip(new java.awt.geom.RoundRectangle2D.Float(previewX, previewY, previewWidth, previewHeight, 5, 5));
                    g.drawImage(themeImg, previewX, previewY, previewWidth, previewHeight, this);
                    g.setClip(null);
                }
            } else {
                // Default theme
                g.setColor(Color.BLACK);
                g.fillRoundRect(previewX, previewY, previewWidth, previewHeight, 5, 5);
                
                // "Default" text
                g.setColor(new Color(150, 150, 150));
                float defaultSize = (float)(10 * Math.min(scaleX, scaleY));
                g.setFont(secondaryFont.deriveFont(defaultSize));
                FontMetrics defaultFm = g.getFontMetrics();
                String defaultText = getText("THEME_DEFAULT");
                int defaultTextX = previewX + (previewWidth - defaultFm.stringWidth(defaultText)) / 2;
                int defaultTextY = previewY + (previewHeight + defaultFm.getHeight()) / 2;
                g.drawString(defaultText, defaultTextX, defaultTextY);
            }
        }
        
        // Selection border
        if (isSelected) {
            g.setColor(new Color(100, 150, 255));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(x - 1, y - 1, width + 2, height + 2, 10, 10);
        }
        
        // Active indicator
        if (isActive) {
            g.setColor(new Color(50, 200, 50));
            g.fillOval(x + width - 12, y + 2, 10, 10);
            g.setColor(Color.WHITE);
            float checkSize = (float)(8 * Math.min(scaleX, scaleY));
            g.setFont(primaryFont.deriveFont(checkSize));
            g.drawString("✓", x + width - 10, y + 10);
        }
    }
    
    private void drawThemeCard(Graphics2D g, int themeIndex, int x, int y, int width, int height) {
        boolean isSelected = (themeIndex == selectedBackgroundOption);
        boolean isActive = (themeIndex == selectedBackground);
        
        // Card shadow for depth
        g.setColor(new Color(0, 0, 0, 60));
        g.fillRoundRect(x + 3, y + 3, width, height, 12, 12);
        
        // Card background with hover effect
        if (isSelected) {
            g.setColor(new Color(50, 100, 200, 40)); // Blue tint for selection
        } else {
            g.setColor(new Color(40, 40, 55)); // Dark card background
        }
        g.fillRoundRect(x, y, width, height, 12, 12);
        
        // Theme preview area
        int previewX = x + 8;
        int previewY = y + 8;
        int previewWidth = width - 16;
        int previewHeight = height - 40;
        
        // Draw theme preview
        if (themeIndex < backgroundImages.size()) {
            Image themeImg = backgroundImages.get(themeIndex);
            
            if (themeImg != null) {
                // Clip to rounded corners
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(previewX, previewY, previewWidth, previewHeight, 8, 8));
                g.drawImage(themeImg, previewX, previewY, previewWidth, previewHeight, this);
                g.setClip(null);
            } else {
                // Default theme preview
                GradientPaint defaultGradient = new GradientPaint(
                    previewX, previewY, new Color(20, 20, 20),
                    previewX, previewY + previewHeight, new Color(5, 5, 5)
                );
                g.setPaint(defaultGradient);
                g.fillRoundRect(previewX, previewY, previewWidth, previewHeight, 8, 8);
                
                // Add classic "Default" text
                g.setColor(new Color(150, 150, 150));
                float defaultSize = (float)(12 * Math.min(scaleX, scaleY));
                g.setFont(secondaryFont.deriveFont(defaultSize));
                FontMetrics defaultFm = g.getFontMetrics();
                String defaultText = getText("THEME_DEFAULT");
                int defaultTextX = previewX + (previewWidth - defaultFm.stringWidth(defaultText)) / 2;
                int defaultTextY = previewY + (previewHeight + defaultFm.getHeight()) / 2;
                g.drawString(defaultText, defaultTextX, defaultTextY);
            }
        }
        
        // Selection border with glow
        if (isSelected) {
            g.setColor(new Color(100, 150, 255, 200));
            g.setStroke(new BasicStroke(3));
            g.drawRoundRect(x - 1, y - 1, width + 2, height + 2, 14, 14);
            
            // Inner glow
            g.setColor(new Color(100, 150, 255, 50));
            g.setStroke(new BasicStroke(1));
            g.drawRoundRect(x + 1, y + 1, width - 2, height - 2, 10, 10);
        }
        
        // Active theme indicator
        if (isActive) {
            // Green checkmark in corner
            g.setColor(new Color(50, 200, 50));
            g.fillOval(x + width - 20, y + 5, 15, 15);
            g.setColor(Color.WHITE);
            float checkSize = (float)(10 * Math.min(scaleX, scaleY));
            g.setFont(primaryFont.deriveFont(checkSize));
            g.drawString("✓", x + width - 17, y + 15);
        }
        
        // Theme name
        g.setColor(isSelected ? Color.WHITE : new Color(200, 200, 200));
        float nameSize = (float)(11 * Math.min(scaleX, scaleY));
        g.setFont(secondaryFont.deriveFont(nameSize));
        FontMetrics nameFm = g.getFontMetrics();
        String themeName = backgroundNames.get(themeIndex);
        
        // Truncate long names
        if (nameFm.stringWidth(themeName) > width - 16) {
            while (nameFm.stringWidth(themeName + "...") > width - 16 && themeName.length() > 3) {
                themeName = themeName.substring(0, themeName.length() - 1);
            }
            themeName += "...";
        }
        
        int nameX = x + (width - nameFm.stringWidth(themeName)) / 2;
        int nameY = y + height - 12;
        g.drawString(themeName, nameX, nameY);
    }
    
    private void drawDefaultBackgroundPreview(Graphics2D g, int x, int y, int width, int height) {
        // Draw simple gradient for default background
        GradientPaint gradient = new GradientPaint(
            x, y, new Color(30, 30, 30),
            x, y + height, new Color(60, 60, 60)
        );
        g.setPaint(gradient);
        g.fillRect(x, y, width, height);
        
        // Draw center line
        g.setColor(new Color(150, 150, 150));
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f}, 0.0f));
        g.drawLine(x + width/2, y, x + width/2, y + height);
        
        // Reset paint
        g.setPaint(null);
    }
    
    private void updateSecondBallState() {
        if (selectedMenuItem == 1) { // Two Players selected (unified for both keyboard and mouse)
            if (!menuBall2Active) {
                // Activate second ball - spawn from top center
                menuBall2Active = true;
                menuBall2Falling = false;
                menuBall2X = BOARD_WIDTH / 2 - menuBallSize / 2;
                menuBall2Y = -menuBallSize; // Start above screen
                double initialSpeed = 4.5 * Math.min(scaleX, scaleY);
                menuBall2VX = (Math.random() > 0.5) ? initialSpeed : -initialSpeed;
                menuBall2VY = initialSpeed * 0.5; // Slower initial downward speed
            } else if (menuBall2Falling) {
                // Player returned to Two Players - stop the falling and resume normal physics
                menuBall2Falling = false;
                // Reset to normal physics
                double baseSpeed = 4.5 * Math.min(scaleX, scaleY);
                menuBall2VX = (Math.random() > 0.5) ? baseSpeed : -baseSpeed;
                menuBall2VY = baseSpeed * 0.5;
            }
        } else {
            if (menuBall2Active && !menuBall2Falling) {
                // Change physics to make ball fall immediately
                menuBall2Falling = true;
                // Reduce horizontal movement but don't eliminate it completely
                menuBall2VX *= 0.3; // Slow down horizontal movement significantly
                menuBall2VY = Math.abs(menuBall2VY) + 2; // Make it fall downward (less aggressive)
            }
        }
    }
    
    private void handleGameInput(KeyEvent e) {
        int keyCode = e.getKeyCode();
        
        // Use custom key bindings
        if (keyCode == player1UpKey) {
            wPressed = true;
        } else if (keyCode == player1DownKey) {
            sPressed = true;
        } else if (keyCode == player2UpKey) {
            upPressed = true;
        } else if (keyCode == player2DownKey) {
            downPressed = true;
        }
        
        switch (keyCode) {
            case KeyEvent.VK_ESCAPE:
                // Pause the game instead of exiting directly
                pauseGame();
                break;
            case KeyEvent.VK_SPACE:
                pauseGame();
                break;
        }
    }
    
    private void handlePauseInput(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_SPACE:
                // Resume game
                unpauseGame();
                break;
            case KeyEvent.VK_ESCAPE:
                // Return to main menu
                returnToMainMenuFromPause();
                break;
        }
    }
    
    private void pauseGame() {
        if (currentState == GameState.PLAYING || currentState == GameState.SINGLE_PLAYER) {
            wasSinglePlayer = (currentState == GameState.SINGLE_PLAYER);
            isPaused = true;
            
            // STOP the game immediately - freeze ball and paddle movement
            // Save current ball velocity and set it to zero
            pausedBallVX = ballVX;
            pausedBallVY = ballVY;
            ballVX = 0;
            ballVY = 0;
            
            // Start transition animation
            startPauseTransition();
            System.out.println("DEBUG: Gioco fermato e messo in pausa con transizione. Era single player: " + wasSinglePlayer);
        }
    }
    
    private void startPauseTransition() {
        isTransitioningToPause = true;
        pauseTransitionProgress = 0.0;
        centerLineRotation = 0.0;
        scoreTranslationProgress = 0.0;
        setState(GameState.TRANSITIONING);
    }
    
    private void updatePauseTransition() {
        if (!isTransitioningToPause) return;
        
        // Update transition progress
        pauseTransitionProgress += PAUSE_TRANSITION_SPEED;
        
        // Smooth easing function (ease-in-out)
        double easedProgress = smoothStep(pauseTransitionProgress);
        
        // Update center line rotation (0 to π/4 radians)
        centerLineRotation = easedProgress * Math.PI / 4;
        
        // Update score translation progress
        scoreTranslationProgress = easedProgress;
        
        // Check if transition is complete
        if (pauseTransitionProgress >= 1.0) {
            pauseTransitionProgress = 1.0;
            centerLineRotation = Math.PI / 4;
            scoreTranslationProgress = 1.0;
            isTransitioningToPause = false;
            setState(GameState.PAUSED);
        }
    }
    
    private double smoothStep(double t) {
        // Smooth step function for easing
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * (3.0 - 2.0 * t);
    }
    
    private void unpauseGame() {
        if (isPaused) {
            // Start reverse transition instead of immediately resuming
            startResumeTransition();
            System.out.println("DEBUG: Avvio transizione di ripresa gioco. Era single player: " + wasSinglePlayer);
        }
    }
    
    private void startResumeTransition() {
        isTransitioningFromPause = true;
        isTransitioningToPause = false;
        pauseTransitionProgress = 1.0; // Start from pause state (1.0)
        centerLineRotation = Math.PI / 4; // Start from diagonal
        scoreTranslationProgress = 1.0; // Start from pause positions
        setState(GameState.TRANSITIONING);
        
        // Reset pause animation and message system
        pauseTimer = 0;
        showMotivationalMessage = false;
        messageScrollOffset = 0;
    }
    
    private void updateResumeTransition() {
        if (!isTransitioningFromPause) return;
        
        // Update transition progress (going backwards)
        pauseTransitionProgress -= PAUSE_TRANSITION_SPEED;
        
        // Smooth easing function (ease-in-out)
        double easedProgress = smoothStep(1.0 - pauseTransitionProgress);
        
        // Update center line rotation (π/4 to 0)
        centerLineRotation = (Math.PI / 4) * (1.0 - easedProgress);
        
        // Update score translation progress (1.0 to 0.0)
        scoreTranslationProgress = 1.0 - easedProgress;
        
        // Check if transition is complete
        if (pauseTransitionProgress <= 0.0) {
            // Complete the resume
            pauseTransitionProgress = 0.0;
            centerLineRotation = 0.0;
            scoreTranslationProgress = 0.0;
            isTransitioningFromPause = false;
            isPaused = false;
            
            // Restore ball velocity to resume game movement
            ballVX = pausedBallVX;
            ballVY = pausedBallVY;
            
            if (wasSinglePlayer) {
                setState(GameState.SINGLE_PLAYER);
                System.out.println("DEBUG: Ripreso gioco SINGLE_PLAYER dopo transizione");
            } else {
                setState(GameState.PLAYING);
                System.out.println("DEBUG: Ripreso gioco PLAYING dopo transizione");
            }
        }
    }
    
    private void startRankToHomeTransition() {
        isRankToHomeTransition = true;
        rankToHomeProgress = 0.0;
        setState(GameState.TRANSITIONING);
        
        // Reset ball position to prevent it from showing in transition
        ballX = BOARD_WIDTH / 2 - BALL_SIZE / 2;
        ballY = BOARD_HEIGHT / 2 - BALL_SIZE / 2;
        
        // Keep rank screen data during transition for animation
        // Will be reset when transition completes
    }
    
    private void updateRankToHomeTransition() {
        if (!isRankToHomeTransition) return;
        
        // Update transition progress
        rankToHomeProgress += RANK_TO_HOME_SPEED;
        
        // Check if transition is complete
        if (rankToHomeProgress >= 1.0) {
            rankToHomeProgress = 1.0;
            isRankToHomeTransition = false;
            
            // Reset rank screen flags now that transition is complete
            showRankScreen = false;
            rankAnimationFrame = 0;
            finalRank = "";
            
            setState(GameState.MENU);
        }
    }
    
    private void startPaddleToHomeTransition() {
        isPaddleToHomeTransition = true;
        paddleToHomeProgress = 0.0;
        
        // Determine which paddle we're transitioning from based on current state
        isLeftPaddleTransition = (currentState == GameState.PADDLE_SELECTION);
        
        // Reset ball position to prevent it from showing in transition
        ballX = BOARD_WIDTH / 2 - BALL_SIZE / 2;
        ballY = BOARD_HEIGHT / 2 - BALL_SIZE / 2;
    }
    
    private void updatePaddleToHomeTransition() {
        if (!isPaddleToHomeTransition) return;
        
        // Update transition progress (same speed as home-to-paddle transition)
        paddleToHomeProgress += 0.04; // Same speed as updateHomeToPaddleTransition
        
        // Check if transition is complete
        if (paddleToHomeProgress >= 1.0) {
            paddleToHomeProgress = 1.0;
            isPaddleToHomeTransition = false;
            
            setState(GameState.MENU);
        }
    }
    
    private void returnToMainMenuFromPause() {
        isPaused = false;
        wasSinglePlayer = false;
        setState(GameState.MENU);
        // Reset game state
        score1 = 0;
        score2 = 0;
        rallies = 0;
        currentRallyHits = 0;
        // Reset ball position
        ballX = BOARD_WIDTH / 2 - BALL_SIZE / 2;
        ballY = BOARD_HEIGHT / 2 - BALL_SIZE / 2;
        System.out.println("DEBUG: Tornato al menu principale dalla pausa");
    }
    
    private void handleGameOverInput(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (showRankScreen) {
                // Start rank-to-home transition if showing rank screen
                startRankToHomeTransition();
            } else {
                // Direct to menu if not showing rank screen
                showRankScreen = false;
                rankAnimationFrame = 0;
                finalRank = "";
                
                currentState = GameState.MENU;
                // Reset ball position to prevent it from showing in menu
                ballX = BOARD_WIDTH / 2 - BALL_SIZE / 2;
                ballY = BOARD_HEIGHT / 2 - BALL_SIZE / 2;
            }
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        
        // Use custom key bindings
        if (keyCode == player1UpKey) {
            wPressed = false;
            // Also handle demo paddle release
            if (isDemoMode) {
                demoPaddleUpPressed = false;
            }
        } else if (keyCode == player1DownKey) {
            sPressed = false;
            // Also handle demo paddle release
            if (isDemoMode) {
                demoPaddleDownPressed = false;
            }
        } else if (keyCode == player2UpKey) {
            upPressed = false;
            // Also handle demo paddle release
            if (isDemoMode) {
                demoPaddleUpPressed = false;
            }
        } else if (keyCode == player2DownKey) {
            downPressed = false;
            // Also handle demo paddle release
            if (isDemoMode) {
                demoPaddleDownPressed = false;
            }
        }
        
        // Handle paddle selection screen key release - EXACT same system as game
        if (currentState == GameState.PADDLE_SELECTION) {
            if (keyCode == player1UpKey) {
                paddleSelectionUpPressed = false;
            } else if (keyCode == player1DownKey) {
                paddleSelectionDownPressed = false;
            }
        } else if (currentState == GameState.RIGHT_PADDLE_SELECTION) {
            if (keyCode == player2UpKey) {
                paddleSelectionUpPressed = false;
            } else if (keyCode == player2DownKey) {
                paddleSelectionDownPressed = false;
            }
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {}
    
    // Mouse event handlers
    @Override
    public void mouseClicked(MouseEvent e) {
        // mouseClicked is less reliable across platforms, so we use mousePressed/Released instead
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        // Block mouse input during transitions
        if (isAnyTransitionActive()) {
            return;
        }
        
        // Store click position and state for cross-platform compatibility
        mouseClickStartX = e.getX();
        mouseClickStartY = e.getY();
        mouseClickStarted = true;
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        // Block mouse input during transitions
        if (isAnyTransitionActive()) {
            mouseClickStarted = false; // Reset click state
            return;
        }
        
        // Only process if mouse was pressed and released in similar position (cross-platform click detection)
        if (mouseClickStarted) {
            int deltaX = Math.abs(e.getX() - mouseClickStartX);
            int deltaY = Math.abs(e.getY() - mouseClickStartY);
            
            // Allow small movement (5 pixels) to account for natural hand tremor
            if (deltaX <= 5 && deltaY <= 5) {
                // Process mouse clicks on EDT for cross-platform compatibility
                SwingUtilities.invokeLater(() -> {
                    if (currentState == GameState.MENU) {
                        handleMenuMouseClick(e);
                    } else if (currentState == GameState.SETTINGS) {
                        handleSettingsMouseClick(e);
                    } else if (currentState == GameState.BACKGROUND_SELECTION) {
                        handleBackgroundSelectionMouseClick(e);
                    } else if (currentState == GameState.PADDLE_SELECTION) {
                        handleSimplePaddleClick(e, true);
                    } else if (currentState == GameState.RIGHT_PADDLE_SELECTION) {
                        handleSimplePaddleClick(e, false);
                    }
                });
            }
            mouseClickStarted = false;
        }
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
        // Mouse entered the component - no action needed
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
        // Clean reset when mouse leaves component
        if (currentState == GameState.SETTINGS) {
            currentHoverState = HoverState.NONE;
            hoveredCategory = -1;
            hoveredSetting = -1;
            mouseOnBackground = false;
            isUsingKeyboardNavigationSettings = false; // Reset keyboard navigation when mouse leaves
            
            System.out.println("DEBUG: Mouse exited - reset to NONE state");
            repaint();
        }
    }
    
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        // ===== SISTEMA DI SCROLL AVANZATO =====
        // Implementa smooth scrolling, momentum, physics-based motion
        
        if (currentState == GameState.PADDLE_SELECTION) {
            // Scroll fluido per paddle sinistro con momentum physics
            applyMomentumScroll(e.getWheelRotation() * 40, true); // 40 = moltiplicatore sensibilità
            
        } else if (currentState == GameState.RIGHT_PADDLE_SELECTION) {
            // Scroll fluido per paddle destro con momentum physics
            applyMomentumScroll(e.getWheelRotation() * 40, false); // 40 = moltiplicatore sensibilità
        }
        
        // === SCROLL LEGACY PER ALTRE SCHERMATE ===
        // Mantiene compatibilità con resto del gioco
        if (currentState != GameState.PADDLE_SELECTION && currentState != GameState.RIGHT_PADDLE_SELECTION) {
            // Scroll classico per altre schermate che potrebbero averlo
            // (placeholder per future implementazioni)
        }
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {}
    
    @Override
    public void mouseMoved(MouseEvent e) {
        // Show mouse cursor when mouse is moved
        int currentX = e.getX();
        int currentY = e.getY();
        
        // Only show if mouse actually moved (not just entered/exited component)
        if (lastMouseX != currentX || lastMouseY != currentY) {
            showMouseCursor();
            lastMouseX = currentX;
            lastMouseY = currentY;
        }
        
        // Block mouse motion handling during transitions
        if (isAnyTransitionActive()) {
            return;
        }
        
        if (currentState == GameState.MENU) {
            handleMenuMouseMove(e);
        } else if (currentState == GameState.SETTINGS) {
            handleSettingsMouseMove(e);
        } else if (currentState == GameState.BACKGROUND_SELECTION) {
            handleBackgroundSelectionMouseMove(e);
        } else if (currentState == GameState.PADDLE_SELECTION) {
            // No mouse handling in original system
        } else if (currentState == GameState.RIGHT_PADDLE_SELECTION) {
            // No mouse handling in original system
        }
    }
    
    private void handleMenuMouseMove(MouseEvent e) {
        int mouseX = e.getX();
        int mouseY = e.getY();
        int menuStartY = (int)(300 * scaleY);
        int menuSpacing = (int)(60 * scaleY);
        
        // Check which menu item the mouse is over
        boolean overMenuItem = false;
        hoveredMenuItem = -1; // Reset hovered item
        isUsingKeyboardNavigation = false; // Disable keyboard navigation when mouse moves
        
        // Get font for accurate text width calculation
        float menuSize = (float)(24 * Math.min(scaleX, scaleY));
        Font menuFont = primaryFont.deriveFont(menuSize);
        FontMetrics menuFm = getFontMetrics(menuFont);
        
        for (int i = 0; i < menuItems.length; i++) {
            int itemY = menuStartY + i * menuSpacing;
            
            // Calculate precise text bounds for this menu item (no arrows when any paddle is selected)
            String itemText = (i == selectedMenuItem && (isUsingKeyboardNavigation || !mouseOnBackground) && !leftPaddleSelected && !rightPaddleSelected) ? "> " + menuItems[i] + " <" : menuItems[i];
            int textWidth = menuFm.stringWidth(itemText);
            int textHeight = menuFm.getHeight();
            int textX = (BOARD_WIDTH - textWidth) / 2;
            
            // Check if mouse is within precise text bounds only
            if (mouseY >= itemY - textHeight/2 && mouseY <= itemY + textHeight/2 &&
                mouseX >= textX && mouseX <= textX + textWidth) {
                overMenuItem = true;
                hoveredMenuItem = i; // Set which item is being hovered
                if (selectedMenuItem != i) {
                    selectedMenuItem = i;
                    leftPaddleSelected = false; // Reset paddle selection when selecting menu items with mouse
                    rightPaddleSelected = false; // Reset right paddle selection when selecting menu items with mouse
                    updateSecondBallState(); // Update ball state when menu selection changes
                    repaint();
                }
                break;
            }
        }
        
        // Check if over space instruction area
        int spaceInstructY = (int)(BOARD_HEIGHT - 50 * scaleY);
        boolean overSpaceArea = (mouseY >= spaceInstructY - 20 && mouseY <= spaceInstructY + 20);
        
        // Check if mouse is over paddle areas
        boolean overLeftPaddleArea = isMouseOverLeftPaddleArea(mouseX, mouseY);
        boolean overRightPaddleArea = isMouseOverRightPaddleArea(mouseX, mouseY);
        
        // Set paddle selection based on mouse position
        if (overLeftPaddleArea) {
            // Select left paddle when mouse is over it
            if (!leftPaddleSelected || rightPaddleSelected) {
                leftPaddleSelected = true;
                rightPaddleSelected = false;
                repaint();
            }
        } else if (overRightPaddleArea) {
            // Select right paddle when mouse is over it
            if (!rightPaddleSelected || leftPaddleSelected) {
                rightPaddleSelected = true;
                leftPaddleSelected = false;
                repaint();
            }
        } else if (!overMenuItem && !overSpaceArea && !overLeftPaddleArea && !overRightPaddleArea) {
            // Only reset paddle selection if not over any paddle area and not over menu items
            if (leftPaddleSelected || rightPaddleSelected) {
                leftPaddleSelected = false;
                rightPaddleSelected = false;
                repaint();
            }
        }
        
        // Update background hover state (exclude precise paddle areas)
        boolean previousMouseOnBackground = mouseOnBackground;
        mouseOnBackground = !overMenuItem && !overSpaceArea && !overLeftPaddleArea && !overRightPaddleArea;
        
        // Reset selectedMenuItem when mouse is not over any menu item (to sync with hoveredMenuItem)
        if (hoveredMenuItem == -1 && !isUsingKeyboardNavigation) {
            // Mouse is not over any menu item, no specific selection
            selectedMenuItem = -1; // No selection when mouse is not hovering
        }
        
        // Update second ball state when mouse position changes
        updateSecondBallState();
        
        // Repaint if background hover state changed
        if (mouseOnBackground != previousMouseOnBackground) {
            repaint();
        }
    }
    
    
    private boolean isMouseOverLeftPaddleArea(int mouseX, int mouseY) {
        // Calculate left paddle dimensions and position (same as in drawMenuPaddles)
        int widePaddleWidth = (int)(250 * Math.min(scaleX, scaleY));
        int paddleHeight = (int)(menuPaddleHeight * 1.8);
        int paddleYOffset = (int)(-paddleHeight * 0.2);
        
        // Left paddle center and rotation (-25°)
        int paddleExitOffset = (int)(paddleExitProgress * widePaddleWidth * 1.5);
        int leftCenterX = 0 - paddleExitOffset;
        int leftCenterY = paddleYOffset + paddleHeight / 2;
        
        // Transform mouse coordinates relative to paddle center
        int relativeX = mouseX - leftCenterX;
        int relativeY = mouseY - leftCenterY;
        
        // Rotate mouse coordinates by +25° (inverse of paddle rotation)
        double angle = Math.toRadians(25);
        double rotatedX = relativeX * Math.cos(angle) - relativeY * Math.sin(angle);
        double rotatedY = relativeX * Math.sin(angle) + relativeY * Math.cos(angle);
        
        // Check if rotated coordinates are within paddle bounds (exact same as visual paddle area)
        // Paddle main body: from -widePaddleWidth/2 to +widePaddleWidth/2
        // Glow on right side: from +widePaddleWidth/2 to +widePaddleWidth/2 + glowWidth
        int glowWidth = Math.max(2, (int)(8 * Math.min(scaleX, scaleY))); // Same as in drawMenuPaddles
        
        // Total clickable area: paddle body + right glow
        boolean inMainPaddle = (rotatedX >= -widePaddleWidth/2 && rotatedX <= widePaddleWidth/2 &&
                               rotatedY >= -paddleHeight/2 && rotatedY <= paddleHeight/2);
        boolean inRightGlow = (rotatedX >= widePaddleWidth/2 && rotatedX <= widePaddleWidth/2 + glowWidth &&
                              rotatedY >= -paddleHeight/2 && rotatedY <= paddleHeight/2);
        
        return inMainPaddle || inRightGlow;
    }
    
    private boolean isMouseOverRightPaddleArea(int mouseX, int mouseY) {
        // Calculate right paddle dimensions and position (same as in drawMenuPaddles)
        int widePaddleWidth = (int)(250 * Math.min(scaleX, scaleY));
        int paddleHeight = (int)(menuPaddleHeight * 1.8);
        int paddleYOffset = (int)(-paddleHeight * 0.2);
        
        // Right paddle center and rotation (+25°)
        int paddleExitOffset = (int)(paddleExitProgress * widePaddleWidth * 1.5);
        int rightCenterX = BOARD_WIDTH + paddleExitOffset;
        int rightCenterY = paddleYOffset + paddleHeight / 2;
        
        // Transform mouse coordinates relative to paddle center
        int relativeX = mouseX - rightCenterX;
        int relativeY = mouseY - rightCenterY;
        
        // Rotate mouse coordinates by -25° (inverse of paddle rotation)
        double angle = Math.toRadians(-25);
        double rotatedX = relativeX * Math.cos(angle) - relativeY * Math.sin(angle);
        double rotatedY = relativeX * Math.sin(angle) + relativeY * Math.cos(angle);
        
        // Check if rotated coordinates are within paddle bounds (exact same as visual paddle area)
        // Paddle main body: from -widePaddleWidth/2 to +widePaddleWidth/2
        // Glow on left side: from -widePaddleWidth/2 - glowWidth to -widePaddleWidth/2
        int glowWidth = Math.max(2, (int)(8 * Math.min(scaleX, scaleY))); // Same as in drawMenuPaddles
        
        // Total clickable area: left glow + paddle body
        boolean inMainPaddle = (rotatedX >= -widePaddleWidth/2 && rotatedX <= widePaddleWidth/2 &&
                               rotatedY >= -paddleHeight/2 && rotatedY <= paddleHeight/2);
        boolean inLeftGlow = (rotatedX >= -widePaddleWidth/2 - glowWidth && rotatedX <= -widePaddleWidth/2 &&
                             rotatedY >= -paddleHeight/2 && rotatedY <= paddleHeight/2);
        
        return inMainPaddle || inLeftGlow;
    }
    
    
    private void drawRightPaddleSelection(Graphics2D g) {
        // Sfondo - stesso della home
        drawMenuBackground(g);
        
        // Linea centrale
        g.setColor(new Color(255, 255, 255, 100));
        for (int i = 0; i < getHeight(); i += 20) {
            g.fillRect(getWidth() / 2 - 2, i, 4, 10);
        }
        
        // Griglia paddle (lato sinistro)
        drawSimplePaddleGrid(g, 0, 0, getWidth() / 2, getHeight(), false);
        
        // Anteprima paddle destro (posizionato come nel gioco)
        drawPreviewRightPaddle(g, selectedRightPaddleTheme);
        
        // Rimosse le istruzioni dalla schermata paddle themes
    }
    
    private void drawRightPreviewPaddle(Graphics2D g) {
        // Position paddle EXACTLY like right paddle in the game
        int rightPaddleX = BOARD_WIDTH - (int)(20 * scaleX) - PADDLE_WIDTH; // EXACT same position as in drawGame()
        int paddleWidth = PADDLE_WIDTH;
        int paddleHeight = PADDLE_HEIGHT;
        
        // EXACT same corner radius calculation as in drawGame()
        int cornerRadius = Math.max(4, PADDLE_WIDTH / 4);
        
        // Draw the selected right paddle theme (use red paddle themes)
        if (selectedRightPaddleTheme >= 0 && selectedRightPaddleTheme < redPaddleThemeImages.size()) {
            BufferedImage paddleImg = redPaddleThemeImages.get(selectedRightPaddleTheme);
            
            if (paddleImg != null) {
                // Custom theme paddle with rounded corners
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(rightPaddleX, previewPaddleY, paddleWidth, paddleHeight, cornerRadius, cornerRadius));
                g.drawImage(paddleImg, rightPaddleX, previewPaddleY, paddleWidth, paddleHeight, this);
                g.setClip(null);
            } else {
                // Default gradient paddle
                GradientPaint rightPaddleGradient = new GradientPaint(
                    rightPaddleX, previewPaddleY, new Color(255, 100, 100),
                    rightPaddleX + paddleWidth, previewPaddleY + paddleHeight, new Color(255, 150, 150));
                g.setPaint(rightPaddleGradient);
                g.fillRoundRect(rightPaddleX, previewPaddleY, paddleWidth, paddleHeight, cornerRadius, cornerRadius);
            }
        } else {
            // Fallback to default gradient
            GradientPaint rightPaddleGradient = new GradientPaint(
                rightPaddleX, previewPaddleY, new Color(255, 100, 100),
                rightPaddleX + paddleWidth, previewPaddleY + paddleHeight, new Color(255, 150, 150));
            g.setPaint(rightPaddleGradient);
            g.fillRoundRect(rightPaddleX, previewPaddleY, paddleWidth, paddleHeight, cornerRadius, cornerRadius);
        }
        
        // Reset paint
        g.setPaint(null);
    }
    
    private void drawRightPaddleThemePanel(Graphics2D g) {
        // Modern grid layout on the LEFT side (opposite of left paddle selection)
        int panelX = 0;
        int panelY = 0;
        int panelWidth = BOARD_WIDTH / 2;
        int panelHeight = BOARD_HEIGHT;
        
        // Semi-transparent panel background with red-tinted gradient  
        GradientPaint panelGradient = new GradientPaint(
            panelX, panelY, new Color(40, 20, 20, 200),
            panelX + panelWidth, panelY + panelHeight, new Color(20, 0, 0, 180)
        );
        g.setPaint(panelGradient);
        g.fillRect(panelX, panelY, panelWidth, panelHeight);
        g.setPaint(null);
        
        // Panel title with red glow effect
        g.setColor(new Color(255, 255, 255, 200));
        float panelTitleSize = (float)(24 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(Font.BOLD, panelTitleSize));
        FontMetrics panelTitleFm = g.getFontMetrics();
        String panelTitle = "TEMI PADDLE";
        int panelTitleX = panelX + (panelWidth - panelTitleFm.stringWidth(panelTitle)) / 2;
        int panelTitleY = panelY + (int)(50 * scaleY);
        
        // Title glow effect (red theme)
        g.setColor(new Color(255, 100, 100, 60));
        g.drawString(panelTitle, panelTitleX + 2, panelTitleY + 2);
        g.setColor(Color.WHITE);
        g.drawString(panelTitle, panelTitleX, panelTitleY);
        
        // Modern scrollable grid layout for themes - DYNAMIC SIZE
        int gridStartY = panelY + (int)(80 * scaleY);
        int cardMargin = (int)(15 * Math.min(scaleX, scaleY));
        int cardSpacing = (int)(10 * Math.min(scaleX, scaleY));
        
        // Dynamic grid calculations
        int gridCols = calculateGridCols();
        int gridRows = calculateGridRows();
        int pageSize = getGridPageSize();
        
        // Use optimal card dimensions for proper paddle display - NO STRETCHING
        int cardWidth = (int)(100 * Math.min(scaleX, scaleY)); // Fixed optimal width
        int cardHeight = (int)(110 * Math.min(scaleX, scaleY)); // Fixed optimal height
        
        // Calculate total pages needed for right paddle (use red themes)
        int totalThemes = redPaddleThemeNames.size();
        int totalPages = (int) Math.ceil((double) totalThemes / pageSize);
        int currentPage = 0; // Using new scrolling system
        
        // Draw themes in current page
        int startIndex = 0; // Using new scrolling system
        int endIndex = Math.min(startIndex + pageSize, totalThemes);
        
        for (int i = startIndex; i < endIndex; i++) {
            int gridPosition = i - startIndex;
            int col = gridPosition % gridCols;
            int row = gridPosition / gridCols;
            
            int cardX = panelX + cardMargin + col * (cardWidth + cardSpacing);
            int cardY = gridStartY + row * (cardHeight + cardSpacing);
            
            drawModernThemeCard(g, i, cardX, cardY, cardWidth, cardHeight, true); // true = right paddle (red)
        }
        
        // Show scroll indicator and page info if there are multiple pages
        if (totalPages > 1) {
            // Scroll indicators
            g.setColor(new Color(255, 255, 255, 180));
            float indicatorSize = (float)(16 * Math.min(scaleX, scaleY));
            g.setFont(primaryFont.deriveFont(Font.BOLD, indicatorSize));
            FontMetrics indicatorFm = g.getFontMetrics();
            
            // Page info
            String pageInfo = String.format("Pagina %d/%d", currentPage + 1, totalPages);
            int pageInfoX = panelX + (panelWidth - indicatorFm.stringWidth(pageInfo)) / 2;
            int pageInfoY = panelY + panelHeight - (int)(50 * scaleY);
            g.drawString(pageInfo, pageInfoX, pageInfoY);
            
            // Navigation hint
            g.setColor(new Color(255, 255, 255, 150));
            float hintSize = (float)(12 * Math.min(scaleX, scaleY));
            g.setFont(primaryFont.deriveFont(hintSize));
            FontMetrics hintFm = g.getFontMetrics();
            
            String navHint = "Q/E per scorrere pagine";
            int hintX = panelX + (panelWidth - hintFm.stringWidth(navHint)) / 2;
            int hintY = panelY + panelHeight - (int)(20 * scaleY);
            g.drawString(navHint, hintX, hintY);
            
            // Visual scroll arrows
            drawScrollArrows(g, panelX + (int)(30 * scaleX), panelY + panelHeight - (int)(40 * scaleY), 
                           currentPage > 0, currentPage < totalPages - 1);
        }
    }
    
    private void drawModernRightThemeCard(Graphics2D g, int themeIndex, int x, int y, int width, int height) {
        boolean isSelected = (themeIndex == selectedRightPaddleTheme);
        boolean isHovered = false; // TODO: Add hover detection based on mouse position
        
        // Enhanced shadow with multiple layers for depth
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(x + 4, y + 4, width, height, 16, 16);
        g.setColor(new Color(0, 0, 0, 60));
        g.fillRoundRect(x + 2, y + 2, width, height, 16, 16);
        
        // Dynamic card background with red gradient
        Color cardColor1, cardColor2;
        if (isSelected) {
            // Vibrant red gradient for selected
            cardColor1 = new Color(255, 100, 100, 240);
            cardColor2 = new Color(235, 80, 80, 240);
        } else if (isHovered) {
            // Subtle red highlight for hover
            cardColor1 = new Color(80, 40, 40, 200);
            cardColor2 = new Color(60, 30, 30, 200);
        } else {
            // Default subtle gradient with red tint
            cardColor1 = new Color(55, 35, 35, 180);
            cardColor2 = new Color(45, 25, 25, 180);
        }
        
        GradientPaint cardGradient = new GradientPaint(
            x, y, cardColor1,
            x + width, y + height, cardColor2
        );
        g.setPaint(cardGradient);
        g.fillRoundRect(x, y, width, height, 12, 12);
        g.setPaint(null);
        
        // Enhanced border with red glow effect
        if (isSelected) {
            // Outer glow for selected theme (red)
            g.setColor(new Color(255, 150, 150, 100));
            g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawRoundRect(x - 2, y - 2, width + 4, height + 4, 16, 16);
            
            // Inner border (red)
            g.setColor(new Color(255, 200, 200));
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawRoundRect(x, y, width, height, 12, 12);
        } else {
            // Subtle border for non-selected
            g.setColor(new Color(140, 100, 100, isHovered ? 150 : 80));
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(x, y, width, height, 12, 12);
        }
        
        // Full card paddle preview (using all available space without names)
        int previewMargin = (int)(12 * Math.min(scaleX, scaleY));
        int previewWidth = Math.min(width - 2 * previewMargin, (int)(PADDLE_WIDTH * 1.8));
        int previewHeight = Math.min(height - 2 * previewMargin, (int)(PADDLE_HEIGHT * 1.5));
        int previewX = x + (width - previewWidth) / 2;
        int previewY = y + (height - previewHeight) / 2;
        
        // Enhanced corner radius for preview
        int cornerRadius = Math.max(6, previewWidth / 6);
        
        // Draw paddle preview with enhanced rendering
        if (themeIndex < paddleThemeImages.size()) {
            BufferedImage themeImg = paddleThemeImages.get(themeIndex);
            
            if (themeImg != null) {
                // Custom theme with enhanced clipping
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(previewX, previewY, previewWidth, previewHeight, cornerRadius, cornerRadius));
                g.drawImage(themeImg, previewX, previewY, previewWidth, previewHeight, this);
                g.setClip(null);
                
                // Add subtle inner shadow for depth
                g.setColor(new Color(0, 0, 0, 40));
                g.setStroke(new BasicStroke(1));
                g.drawRoundRect(previewX, previewY, previewWidth - 1, previewHeight - 1, cornerRadius, cornerRadius);
            } else {
                // Enhanced default red gradient
                GradientPaint paddleGradient = new GradientPaint(
                    previewX, previewY, new Color(255, 120, 120),
                    previewX + previewWidth, previewY + previewHeight, new Color(255, 170, 170)
                );
                g.setPaint(paddleGradient);
                g.fillRoundRect(previewX, previewY, previewWidth, previewHeight, cornerRadius, cornerRadius);
                g.setPaint(null);
                
                // Paddle glow effect (red)
                g.setColor(new Color(255, 100, 100, 60));
                g.fillRoundRect(previewX - (int)(6 * scaleX), previewY, (int)(6 * scaleX), previewHeight, cornerRadius/2, cornerRadius/2);
            }
        }
        
        // Theme names removed as requested
        
        // Selection indicator (checkmark for selected theme) - red theme
        if (isSelected) {
            int indicatorSize = (int)(16 * Math.min(scaleX, scaleY));
            int indicatorX = x + width - indicatorSize - (int)(6 * scaleX);
            int indicatorY = y + (int)(6 * scaleY);
            
            // Checkmark background circle (red)
            g.setColor(new Color(200, 50, 50, 200));
            g.fillOval(indicatorX, indicatorY, indicatorSize, indicatorSize);
            
            // Checkmark
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int checkSize = indicatorSize / 3;
            g.drawLine(indicatorX + checkSize, indicatorY + indicatorSize/2, 
                      indicatorX + indicatorSize/2, indicatorY + indicatorSize - checkSize);
            g.drawLine(indicatorX + indicatorSize/2, indicatorY + indicatorSize - checkSize,
                      indicatorX + indicatorSize - checkSize/2, indicatorY + checkSize);
        }
    }
    
    private void drawRightPaddleThemeCard(Graphics2D g, int themeIndex, int cardX, int cardY, int cardWidth, int cardHeight) {
        // Same as left paddle theme card but for right paddle selection
        boolean isSelected = (themeIndex == selectedRightPaddleTheme);
        
        // Card background
        Color cardBg = isSelected ? new Color(60, 40, 40) : new Color(40, 25, 25);
        g.setColor(cardBg);
        g.fillRoundRect(cardX, cardY, cardWidth, cardHeight, 8, 8);
        
        // Card border
        if (isSelected) {
            g.setColor(new Color(255, 150, 150, 200));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(cardX, cardY, cardWidth, cardHeight, 8, 8);
            g.setStroke(new BasicStroke(1));
        }
        
        // Paddle preview (smaller version)
        int previewSize = (int)(40 * Math.min(scaleX, scaleY));
        int previewX = cardX + (int)(20 * scaleX);
        int previewY = cardY + (cardHeight - previewSize) / 2;
        
        if (themeIndex < paddleThemeImages.size()) {
            BufferedImage paddleImg = paddleThemeImages.get(themeIndex);
            if (paddleImg != null) {
                // Custom theme paddle preview
                int cornerRadius = Math.max(2, previewSize / 8);
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(previewX, previewY, previewSize/2, previewSize, cornerRadius, cornerRadius));
                g.drawImage(paddleImg, previewX, previewY, previewSize/2, previewSize, this);
                g.setClip(null);
            } else {
                // Default gradient preview
                GradientPaint previewGradient = new GradientPaint(
                    previewX, previewY, new Color(255, 100, 100),
                    previewX + previewSize/2, previewY + previewSize, new Color(255, 150, 150));
                g.setPaint(previewGradient);
                g.fillRoundRect(previewX, previewY, previewSize/2, previewSize, 4, 4);
                g.setPaint(null);
            }
        }
        
        // Theme name
        g.setColor(Color.WHITE);
        float nameSize = (float)(16 * Math.min(scaleX, scaleY));
        g.setFont(secondaryFont.deriveFont(nameSize));
        String themeName = paddleThemeNames.get(themeIndex);
        g.drawString(themeName, previewX + previewSize/2 + (int)(15 * scaleX), cardY + cardHeight/2 + (int)(5 * scaleY));
    }
    
    private void drawPaddleSelection(Graphics2D g) {
        // Sfondo - stesso della home
        drawMenuBackground(g);
        
        // Linea centrale
        g.setColor(new Color(255, 255, 255, 100));
        for (int i = 0; i < getHeight(); i += 20) {
            g.fillRect(getWidth() / 2 - 2, i, 4, 10);
        }
        
        // Anteprima paddle sinistro (posizionato come nel gioco)
        drawPreviewLeftPaddle(g, selectedPaddleTheme);
        
        // Griglia paddle (lato destro)
        drawSimplePaddleGrid(g, getWidth() / 2, 0, getWidth() / 2, getHeight(), true);
        
        // Rimosse le istruzioni dalla schermata paddle themes
    }
    
    private void drawPreviewLeftPaddle(Graphics2D g, int themeIndex) {
        // Save original transform
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        // Position paddle exactly like in drawMenuPaddles (left paddle inclinato)
        int widePaddleWidth = (int)(250 * Math.min(scaleX, scaleY));
        int paddleHeight = (int)(menuPaddleHeight * 1.8);
        int paddleYOffset = (int)(-paddleHeight * 0.2);
        
        // Left paddle center - exactly like in drawMenuPaddles
        int leftCenterX = 0; // Completely attached to left edge
        int leftCenterY = paddleYOffset + paddleHeight / 2;
        
        g.translate(leftCenterX, leftCenterY);
        g.rotate(Math.toRadians(-25)); // Same rotation as left menu paddle
        
        // Draw paddle with selected theme if available
        if (themeIndex >= 0 && themeIndex < bluePaddleThemeImages.size()) {
            BufferedImage paddleImg = bluePaddleThemeImages.get(themeIndex);
            if (paddleImg != null) {
                // Custom theme paddle with rounded corners
                int cornerRadius = Math.max(4, widePaddleWidth / 4);
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, cornerRadius, cornerRadius));
                g.drawImage(paddleImg, -widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, null);
                g.setClip(null);
            } else {
                // Fallback to default gradient
                GradientPaint gradient = new GradientPaint(-widePaddleWidth/2, -paddleHeight/2, new Color(100, 150, 255),
                                                         widePaddleWidth/2, paddleHeight/2, new Color(150, 200, 255));
                g.setPaint(gradient);
                g.fillRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight);
                g.setPaint(null);
            }
        } else {
            // Default gradient paddle
            GradientPaint gradient = new GradientPaint(-widePaddleWidth/2, -paddleHeight/2, new Color(100, 150, 255),
                                                     widePaddleWidth/2, paddleHeight/2, new Color(150, 200, 255));
            g.setPaint(gradient);
            g.fillRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight);
            g.setPaint(null);
        }
        
        // Left paddle border glow (same as in drawMenuPaddles)
        int glowWidth = Math.max(2, (int)(8 * Math.min(scaleX, scaleY)));
        g.setColor(getPaddleGlowColor(true));
        g.fillRect(widePaddleWidth/2, -paddleHeight/2, glowWidth, paddleHeight);
        
        g.setTransform(originalTransform);
        
        // Nome del tema selezionato VERTICALE al centro con dimensione che riempie l'altezza (TRADOTTO)
        java.awt.geom.AffineTransform textTransform = g.getTransform();
        g.setColor(Color.WHITE);
        String themeName = getTranslatedPaddleName(bluePaddleThemeNames.get(themeIndex));
        
        // Calcola dimensione font per riempire l'altezza della finestra
        float fontSize = 20f;
        Font scaledFont = primaryFont.deriveFont(fontSize);
        FontMetrics fm = g.getFontMetrics(scaledFont);
        
        // Aumenta il font fino a riempire quasi tutta l'altezza
        while (fm.stringWidth(themeName) < BOARD_HEIGHT - 40 && fontSize < 200) {
            fontSize += 2f;
            scaledFont = primaryFont.deriveFont(fontSize);
            fm = g.getFontMetrics(scaledFont);
        }
        
        g.setFont(scaledFont);
        fm = g.getFontMetrics();
        
        // Posiziona il testo al centro della finestra verticalmente
        int textX = BOARD_WIDTH / 2;
        int textY = BOARD_HEIGHT / 2;
        
        g.translate(textX, textY);
        g.rotate(Math.toRadians(-90)); // Verticale
        
        // Centra il testo
        int textWidth = fm.stringWidth(themeName);
        
        // Ombra del testo
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(themeName, -textWidth/2 + 1, 1);
        g.setColor(Color.WHITE);
        g.drawString(themeName, -textWidth/2, 0);
        
        // Ripristina transform per il testo
        g.setTransform(textTransform);
    }
    
    private void drawPreviewRightPaddle(Graphics2D g, int themeIndex) {
        // Save original transform
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        // Position paddle exactly like in drawMenuPaddles (right paddle inclinato)
        int widePaddleWidth = (int)(250 * Math.min(scaleX, scaleY));
        int paddleHeight = (int)(menuPaddleHeight * 1.8);
        int paddleYOffset = (int)(-paddleHeight * 0.2);
        
        // Right paddle center - exactly like in drawMenuPaddles
        int rightCenterX = BOARD_WIDTH; // Completely attached to right edge
        int rightCenterY = paddleYOffset + paddleHeight / 2;
        
        g.translate(rightCenterX, rightCenterY);
        g.rotate(Math.toRadians(25)); // Same rotation as right menu paddle
        
        // Draw paddle with selected theme if available
        if (themeIndex >= 0 && themeIndex < redPaddleThemeImages.size()) {
            BufferedImage paddleImg = redPaddleThemeImages.get(themeIndex);
            if (paddleImg != null) {
                // Custom theme paddle with rounded corners
                int cornerRadius = Math.max(4, widePaddleWidth / 4);
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, cornerRadius, cornerRadius));
                g.drawImage(paddleImg, -widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight, null);
                g.setClip(null);
            } else {
                // Fallback to default gradient
                GradientPaint gradient = new GradientPaint(-widePaddleWidth/2, -paddleHeight/2, new Color(255, 100, 100),
                                                         widePaddleWidth/2, paddleHeight/2, new Color(255, 150, 150));
                g.setPaint(gradient);
                g.fillRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight);
                g.setPaint(null);
            }
        } else {
            // Default gradient paddle
            GradientPaint gradient = new GradientPaint(-widePaddleWidth/2, -paddleHeight/2, new Color(255, 100, 100),
                                                     widePaddleWidth/2, paddleHeight/2, new Color(255, 150, 150));
            g.setPaint(gradient);
            g.fillRect(-widePaddleWidth/2, -paddleHeight/2, widePaddleWidth, paddleHeight);
            g.setPaint(null);
        }
        
        // Right paddle border glow (same as in drawMenuPaddles)
        int glowWidth = Math.max(2, (int)(8 * Math.min(scaleX, scaleY)));
        g.setColor(getPaddleGlowColor(false));
        g.fillRect(-widePaddleWidth/2 - glowWidth, -paddleHeight/2, glowWidth, paddleHeight);
        
        g.setTransform(originalTransform);
        
        // Nome del tema selezionato VERTICALE al centro con dimensione che riempie l'altezza (TRADOTTO)
        java.awt.geom.AffineTransform textTransform = g.getTransform();
        g.setColor(Color.WHITE);
        String themeName = getTranslatedPaddleName(redPaddleThemeNames.get(themeIndex));
        
        // Calcola dimensione font per riempire l'altezza della finestra
        float fontSize = 20f;
        Font scaledFont = primaryFont.deriveFont(fontSize);
        FontMetrics fm = g.getFontMetrics(scaledFont);
        
        // Aumenta il font fino a riempire quasi tutta l'altezza
        while (fm.stringWidth(themeName) < BOARD_HEIGHT - 40 && fontSize < 200) {
            fontSize += 2f;
            scaledFont = primaryFont.deriveFont(fontSize);
            fm = g.getFontMetrics(scaledFont);
        }
        
        g.setFont(scaledFont);
        fm = g.getFontMetrics();
        
        // Posiziona il testo al centro della finestra verticalmente
        int textX = BOARD_WIDTH / 2;
        int textY = BOARD_HEIGHT / 2;
        
        g.translate(textX, textY);
        g.rotate(Math.toRadians(90)); // Verticale nell'altro verso per paddle destro
        
        // Centra il testo
        int textWidth = fm.stringWidth(themeName);
        
        // Ombra del testo
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(themeName, -textWidth/2 + 1, 1);
        g.setColor(Color.WHITE);
        g.drawString(themeName, -textWidth/2, 0);
        
        // Ripristina transform per il testo
        g.setTransform(textTransform);
    }

    private void drawPreviewPaddle(Graphics2D g) {
        // Position paddle EXACTLY like in the game - left side
        int paddleX = (int)(20 * scaleX); // EXACT same position as in drawGame()
        int paddleWidth = PADDLE_WIDTH;
        int paddleHeight = PADDLE_HEIGHT;
        
        // EXACT same corner radius calculation as in drawGame()
        int cornerRadius = Math.max(4, PADDLE_WIDTH / 4);
        
        // Draw the selected paddle theme (use blue paddle themes)
        if (selectedPaddleTheme >= 0 && selectedPaddleTheme < bluePaddleThemeImages.size()) {
            BufferedImage paddleImg = bluePaddleThemeImages.get(selectedPaddleTheme);
            
            if (paddleImg != null) {
                // For custom themes, draw with rounded corners like in game
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(paddleX, previewPaddleY, paddleWidth, paddleHeight, cornerRadius, cornerRadius));
                g.drawImage(paddleImg, paddleX, previewPaddleY, paddleWidth, paddleHeight, this);
                g.setClip(null);
            } else {
                // EXACT same gradient and drawing method as in drawGame()
                GradientPaint paddle1Gradient = new GradientPaint(
                    paddleX, previewPaddleY, new Color(100, 150, 255), 
                    paddleX + paddleWidth, previewPaddleY + paddleHeight, new Color(150, 200, 255));
                g.setPaint(paddle1Gradient);
                g.fillRoundRect(paddleX, previewPaddleY, paddleWidth, paddleHeight, cornerRadius, cornerRadius);
                g.setPaint(null);
            }
        }
        
        // Remove the glow effect - it's not present in the actual game paddle rendering
    }
    
    private void drawPaddleThemePanel(Graphics2D g) {
        // Modern grid layout covering the right half - FULL HEIGHT
        int panelX = BOARD_WIDTH / 2; // Start at center line
        int panelY = 0; // Start from top of screen
        int panelWidth = BOARD_WIDTH / 2; // Full right half
        int panelHeight = BOARD_HEIGHT; // FULL screen height
        
        // Semi-transparent panel background with subtle gradient
        GradientPaint panelGradient = new GradientPaint(
            panelX, panelY, new Color(0, 0, 0, 200),
            panelX + panelWidth, panelY + panelHeight, new Color(20, 20, 40, 180)
        );
        g.setPaint(panelGradient);
        g.fillRect(panelX, panelY, panelWidth, panelHeight);
        g.setPaint(null);
        
        // Panel title with glow effect
        g.setColor(new Color(255, 255, 255, 200));
        float panelTitleSize = (float)(24 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(Font.BOLD, panelTitleSize));
        FontMetrics panelTitleFm = g.getFontMetrics();
        String panelTitle = "TEMI PADDLE";
        int panelTitleX = panelX + (panelWidth - panelTitleFm.stringWidth(panelTitle)) / 2;
        int panelTitleY = panelY + (int)(50 * scaleY);
        
        // Title glow effect
        g.setColor(new Color(100, 150, 255, 60));
        g.drawString(panelTitle, panelTitleX + 2, panelTitleY + 2);
        g.setColor(Color.WHITE);
        g.drawString(panelTitle, panelTitleX, panelTitleY);
        
        // Modern scrollable grid layout for themes - DYNAMIC SIZE
        int gridStartY = panelY + (int)(80 * scaleY);
        int cardMargin = (int)(15 * Math.min(scaleX, scaleY));
        int cardSpacing = (int)(10 * Math.min(scaleX, scaleY));
        
        // Dynamic grid calculations
        int gridCols = calculateGridCols();
        int gridRows = calculateGridRows();
        int pageSize = getGridPageSize();
        
        // Use optimal card dimensions for proper paddle display - NO STRETCHING
        int cardWidth = (int)(100 * Math.min(scaleX, scaleY)); // Fixed optimal width
        int cardHeight = (int)(110 * Math.min(scaleX, scaleY)); // Fixed optimal height
        
        // Calculate total pages needed (use blue themes for left paddle)
        int totalThemes = bluePaddleThemeNames.size();
        int totalPages = (int) Math.ceil((double) totalThemes / pageSize);
        int currentPage = 0; // Using new scrolling system
        
        // Draw themes in current page
        int startIndex = 0; // Using new scrolling system
        int endIndex = Math.min(startIndex + pageSize, totalThemes);
        
        for (int i = startIndex; i < endIndex; i++) {
            int gridPosition = i - startIndex;
            int col = gridPosition % gridCols;
            int row = gridPosition / gridCols;
            
            int cardX = panelX + cardMargin + col * (cardWidth + cardSpacing);
            int cardY = gridStartY + row * (cardHeight + cardSpacing);
            
            drawModernThemeCard(g, i, cardX, cardY, cardWidth, cardHeight, false); // false = left paddle (blue)
        }
        
        // Show scroll indicator and page info if there are multiple pages
        if (totalPages > 1) {
            // Scroll indicators
            g.setColor(new Color(255, 255, 255, 180));
            float indicatorSize = (float)(16 * Math.min(scaleX, scaleY));
            g.setFont(primaryFont.deriveFont(Font.BOLD, indicatorSize));
            FontMetrics indicatorFm = g.getFontMetrics();
            
            // Page info
            String pageInfo = String.format("Pagina %d/%d", currentPage + 1, totalPages);
            int pageInfoX = panelX + (panelWidth - indicatorFm.stringWidth(pageInfo)) / 2;
            int pageInfoY = panelY + panelHeight - (int)(50 * scaleY);
            g.drawString(pageInfo, pageInfoX, pageInfoY);
            
            // Navigation hint
            g.setColor(new Color(255, 255, 255, 150));
            float hintSize = (float)(12 * Math.min(scaleX, scaleY));
            g.setFont(primaryFont.deriveFont(hintSize));
            FontMetrics hintFm = g.getFontMetrics();
            
            String navHint = "Q/E per scorrere pagine";
            int hintX = panelX + (panelWidth - hintFm.stringWidth(navHint)) / 2;
            int hintY = panelY + panelHeight - (int)(20 * scaleY);
            g.drawString(navHint, hintX, hintY);
            
            // Visual scroll arrows
            drawScrollArrows(g, panelX + panelWidth - (int)(30 * scaleX), panelY + panelHeight - (int)(40 * scaleY), 
                           currentPage > 0, currentPage < totalPages - 1);
        }
    }
    
    private void drawScrollArrows(Graphics2D g, int x, int y, boolean canScrollPrev, boolean canScrollNext) {
        // Draw previous arrow (left arrow)
        g.setColor(canScrollPrev ? new Color(255, 255, 255, 200) : new Color(100, 100, 100, 100));
        int arrowSize = (int)(8 * Math.min(scaleX, scaleY));
        
        // Left arrow
        int[] leftArrowX = {x - arrowSize, x - arrowSize - arrowSize/2, x - arrowSize};
        int[] leftArrowY = {y - arrowSize/2, y, y + arrowSize/2};
        g.fillPolygon(leftArrowX, leftArrowY, 3);
        
        // Right arrow  
        g.setColor(canScrollNext ? new Color(255, 255, 255, 200) : new Color(100, 100, 100, 100));
        int[] rightArrowX = {x + arrowSize, x + arrowSize + arrowSize/2, x + arrowSize};
        int[] rightArrowY = {y - arrowSize/2, y, y + arrowSize/2};
        g.fillPolygon(rightArrowX, rightArrowY, 3);
    }
    
    private void drawModernThemeCard(Graphics2D g, int themeIndex, int x, int y, int width, int height, boolean isRightPanel) {
        // Use appropriate selection state and theme arrays based on paddle side
        boolean isSelected = isRightPanel ? (themeIndex == selectedRightPaddleTheme) : (themeIndex == selectedPaddleTheme);
        boolean isHovered = false; // TODO: Add hover detection based on mouse position
        
        // Get appropriate theme arrays
        ArrayList<BufferedImage> themeImages = isRightPanel ? redPaddleThemeImages : bluePaddleThemeImages;
        
        // Enhanced shadow with multiple layers for depth
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(x + 4, y + 4, width, height, 16, 16);
        g.setColor(new Color(0, 0, 0, 60));
        g.fillRoundRect(x + 2, y + 2, width, height, 16, 16);
        
        // Dynamic card background with gradient (adapt colors based on paddle side)
        Color cardColor1, cardColor2;
        if (isSelected) {
            if (isRightPanel) {
                // Vibrant red gradient for selected right paddle
                cardColor1 = new Color(255, 100, 100, 240);
                cardColor2 = new Color(235, 80, 80, 240);
            } else {
                // Vibrant blue gradient for selected left paddle
                cardColor1 = new Color(100, 150, 255, 240);
                cardColor2 = new Color(80, 130, 235, 240);
            }
        } else if (isHovered) {
            if (isRightPanel) {
                // Subtle red highlight for hover
                cardColor1 = new Color(80, 60, 60, 200);
                cardColor2 = new Color(60, 40, 40, 200);
            } else {
                // Subtle blue highlight for hover
                cardColor1 = new Color(60, 60, 80, 200);
                cardColor2 = new Color(40, 40, 60, 200);
            }
        } else {
            // Default subtle gradient
            cardColor1 = new Color(45, 45, 55, 180);
            cardColor2 = new Color(35, 35, 45, 180);
        }
        
        GradientPaint cardGradient = new GradientPaint(
            x, y, cardColor1,
            x + width, y + height, cardColor2
        );
        g.setPaint(cardGradient);
        g.fillRoundRect(x, y, width, height, 12, 12);
        g.setPaint(null);
        
        // Enhanced border with glow effect (color-coded by paddle side)
        if (isSelected) {
            if (isRightPanel) {
                // Outer glow for selected right paddle theme (red)
                g.setColor(new Color(255, 150, 150, 100));
                g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawRoundRect(x - 2, y - 2, width + 4, height + 4, 16, 16);
                
                // Inner border (red)
                g.setColor(new Color(255, 200, 200));
                g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawRoundRect(x, y, width, height, 12, 12);
            } else {
                // Outer glow for selected left paddle theme (blue)
                g.setColor(new Color(150, 200, 255, 100));
                g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawRoundRect(x - 2, y - 2, width + 4, height + 4, 16, 16);
                
                // Inner border (blue)
                g.setColor(new Color(200, 230, 255));
                g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawRoundRect(x, y, width, height, 12, 12);
            }
        } else {
            // Subtle border for non-selected
            g.setColor(new Color(120, 120, 140, isHovered ? 150 : 80));
            g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(x, y, width, height, 12, 12);
        }
        
        // Full card paddle preview (using all available space without names)
        int previewMargin = (int)(12 * Math.min(scaleX, scaleY));
        int previewWidth = Math.min(width - 2 * previewMargin, (int)(PADDLE_WIDTH * 1.8));
        int previewHeight = Math.min(height - 2 * previewMargin, (int)(PADDLE_HEIGHT * 1.5));
        int previewX = x + (width - previewWidth) / 2;
        int previewY = y + (height - previewHeight) / 2;
        
        // Enhanced corner radius for preview
        int cornerRadius = Math.max(6, previewWidth / 6);
        
        // Draw paddle preview with enhanced rendering (use correct theme images)
        if (themeIndex < themeImages.size()) {
            BufferedImage themeImg = themeImages.get(themeIndex);
            
            if (themeImg != null) {
                // Custom theme with enhanced clipping
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(previewX, previewY, previewWidth, previewHeight, cornerRadius, cornerRadius));
                g.drawImage(themeImg, previewX, previewY, previewWidth, previewHeight, this);
                g.setClip(null);
                
                // Add subtle inner shadow for depth
                g.setColor(new Color(0, 0, 0, 40));
                g.setStroke(new BasicStroke(1));
                g.drawRoundRect(previewX, previewY, previewWidth - 1, previewHeight - 1, cornerRadius, cornerRadius);
            } else {
                // Enhanced default gradient
                GradientPaint paddleGradient = new GradientPaint(
                    previewX, previewY, new Color(120, 170, 255),
                    previewX + previewWidth, previewY + previewHeight, new Color(170, 220, 255)
                );
                g.setPaint(paddleGradient);
                g.fillRoundRect(previewX, previewY, previewWidth, previewHeight, cornerRadius, cornerRadius);
                g.setPaint(null);
                
                // Paddle glow effect
                g.setColor(new Color(100, 150, 255, 60));
                g.fillRoundRect(previewX + previewWidth, previewY, (int)(6 * scaleX), previewHeight, cornerRadius/2, cornerRadius/2);
            }
        }
        
        // Theme names removed as requested
        
        // Selection indicator (checkmark for selected theme)
        if (isSelected) {
            int indicatorSize = (int)(16 * Math.min(scaleX, scaleY));
            int indicatorX = x + width - indicatorSize - (int)(6 * scaleX);
            int indicatorY = y + (int)(6 * scaleY);
            
            // Checkmark background circle
            g.setColor(new Color(50, 200, 50, 200));
            g.fillOval(indicatorX, indicatorY, indicatorSize, indicatorSize);
            
            // Checkmark
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int checkSize = indicatorSize / 3;
            g.drawLine(indicatorX + checkSize, indicatorY + indicatorSize/2, 
                      indicatorX + indicatorSize/2, indicatorY + indicatorSize - checkSize);
            g.drawLine(indicatorX + indicatorSize/2, indicatorY + indicatorSize - checkSize,
                      indicatorX + indicatorSize - checkSize/2, indicatorY + checkSize);
        }
    }
    
    private void drawPaddleThemeCard(Graphics2D g, int themeIndex, int x, int y, int width, int height) {
        boolean isSelected = (themeIndex == selectedPaddleTheme);
        
        // Card shadow for depth
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRoundRect(x + 3, y + 3, width, height, 12, 12);
        
        // Card background
        if (isSelected) {
            g.setColor(new Color(100, 150, 255, 220));
        } else {
            g.setColor(new Color(40, 40, 40, 180));
        }
        g.fillRoundRect(x, y, width, height, 10, 10);
        
        // Card border
        if (isSelected) {
            g.setColor(new Color(150, 200, 255));
            g.setStroke(new BasicStroke(3));
        } else {
            g.setColor(new Color(100, 100, 100));
            g.setStroke(new BasicStroke(1));
        }
        g.drawRoundRect(x, y, width, height, 10, 10);
        
        // Theme preview - EXACT same rendering as in drawGame(), centered in card
        int previewWidth = PADDLE_WIDTH; // Full paddle size
        int previewHeight = PADDLE_HEIGHT;
        int previewX = x + (width - previewWidth) / 2; // Center horizontally
        int previewY = y + (height - previewHeight) / 2; // Center vertically
        
        // EXACT same corner radius calculation as in drawGame()
        int cornerRadius = Math.max(4, PADDLE_WIDTH / 4);
        
        if (themeIndex < paddleThemeImages.size()) {
            BufferedImage themeImg = paddleThemeImages.get(themeIndex);
            
            if (themeImg != null) {
                // For custom themes, draw with rounded corners like in game
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(previewX, previewY, previewWidth, previewHeight, cornerRadius, cornerRadius));
                g.drawImage(themeImg, previewX, previewY, previewWidth, previewHeight, this);
                g.setClip(null);
            } else {
                // EXACT same gradient and drawing method as in drawGame()
                GradientPaint paddle1Gradient = new GradientPaint(
                    previewX, previewY, new Color(100, 150, 255), 
                    previewX + previewWidth, previewY + previewHeight, new Color(150, 200, 255));
                g.setPaint(paddle1Gradient);
                g.fillRoundRect(previewX, previewY, previewWidth, previewHeight, cornerRadius, cornerRadius);
                g.setPaint(null);
            }
        }
        
        // Theme name removed - only show paddle visual
        
        // Selection indicator
        if (isSelected) {
            g.setColor(new Color(50, 255, 50));
            g.fillOval(x + width - (int)(25 * scaleX), y + (int)(10 * scaleY), (int)(15 * scaleX), (int)(15 * scaleY));
            g.setColor(Color.WHITE);
            float checkSize = (float)(12 * Math.min(scaleX, scaleY));
            g.setFont(primaryFont.deriveFont(checkSize));
            g.drawString("✓", x + width - (int)(22 * scaleX), y + (int)(21 * scaleY));
        }
    }
    
    private void handleMenuMouseClick(MouseEvent e) {
        int mouseX = e.getX();
        int mouseY = e.getY();
        
        // Check for paddle clicks using EXACT same functions as mouse motion for consistency
        boolean clickedOnLeftPaddle = isMouseOverLeftPaddleArea(mouseX, mouseY);
        
        if (clickedOnLeftPaddle) {
            startHomeToPaddleTransition(true); // true for left paddle
            repaint();
            return;
        }
        
        boolean clickedOnRightPaddle = isMouseOverRightPaddleArea(mouseX, mouseY);
        
        if (clickedOnRightPaddle) {
            startHomeToPaddleTransition(false); // false for right paddle
            repaint();
            return;
        }
        
        // Check if click is on menu items (use same precise logic as mouse motion)
        boolean clickedOnMenuItem = false;
        int menuStartY = (int)(300 * scaleY);
        int menuSpacing = (int)(60 * scaleY);
        
        // Get font for accurate text bounds calculation
        float menuSize = (float)(24 * Math.min(scaleX, scaleY));
        Font menuFont = primaryFont.deriveFont(menuSize);
        FontMetrics menuFm = getFontMetrics(menuFont);
        
        for (int i = 0; i < menuItems.length; i++) {
            int itemY = menuStartY + i * menuSpacing;
            
            // Calculate precise text bounds (same as in mouse motion)
            String itemText = menuItems[i]; // Use plain text for click detection
            int textWidth = menuFm.stringWidth(itemText);
            int textHeight = menuFm.getHeight();
            int textX = (BOARD_WIDTH - textWidth) / 2;
            
            // Check if click is within precise text bounds only
            if (mouseY >= itemY - textHeight/2 && mouseY <= itemY + textHeight/2 &&
                mouseX >= textX && mouseX <= textX + textWidth) {
                clickedOnMenuItem = true;
                // Execute action for clicked menu item
                if (i == 0) { // Single Player
                    startNewGame(true);
                } else if (i == 1) { // Two Players
                    startNewGame(false);
                } else if (i == 2) { // History
                    setState(GameState.HISTORY);
                } else if (i == 3) { // Settings
                    startHomeToSettingsTransition(); // Start animated transition
                    isDemoMode = false; // Ensure we're not in demo mode
                } else if (i == 4) { // Exit
                    System.exit(0);
                }
                repaint();
                return;
            }
        }
        
        // Check for space key area (background selection)
        int spaceInstructY = (int)(BOARD_HEIGHT - 50 * scaleY);
        boolean clickedOnSpaceArea = (mouseY >= spaceInstructY - 20 && mouseY <= spaceInstructY + 20);
        
        if (clickedOnSpaceArea) {
            setState(GameState.BACKGROUND_SELECTION);
            selectedBackgroundOption = selectedBackground;
            repaint();
            return;
        }
        
        // If clicked on background (not on menu items, space area, or paddles), open themes
        if (!clickedOnMenuItem && !clickedOnSpaceArea && !clickedOnLeftPaddle && !clickedOnRightPaddle) {
            startHomeToThemesTransition();
            selectedMenuItem = 0; // Reset menu selection when opening theme selection
            selectedBackgroundOption = selectedBackground;
            repaint();
        }
    }
    
    private void handleSettingsMouseMove(MouseEvent e) {
        int mouseX = e.getX();
        int mouseY = e.getY();
        
        // Disable keyboard navigation when mouse moves
        isUsingKeyboardNavigationSettings = false;
        
        // Store previous state for comparison
        HoverState previousState = currentHoverState;
        int previousHoveredCategory = hoveredCategory;
        int previousHoveredSetting = hoveredSetting;
        
        // Reset hover state
        currentHoverState = HoverState.NONE;
        hoveredCategory = -1;
        hoveredSetting = -1;
        
        // Simplified and more reliable mouse detection
        checkCategoriesSimple(mouseX, mouseY);
        if (currentHoverState == HoverState.NONE) {
            checkSettingsSimple(mouseX, mouseY);
        }
        
        // Only repaint if state actually changed
        boolean stateChanged = (currentHoverState != previousState ||
                               hoveredCategory != previousHoveredCategory ||
                               hoveredSetting != previousHoveredSetting);
        
        if (stateChanged) {
            repaint();
        }
    }
    
    private void checkCategoriesSimple(int mouseX, int mouseY) {
        // Precise hit detection for rotated categories (left column)
        if (mouseX >= BOARD_WIDTH / 2) return; // Only check left column
        
        int categoryStartY = (int)(280 * scaleY);
        int categorySpacing = (int)(80 * scaleY);
        
        for (int i = 0; i < categoryNames.length; i++) {
            String categoryName = categoryNames[i];
            int categoryY = categoryStartY + i * categorySpacing;
            
            // Calculate exact position like in rendering (match drawCategoryColumn exactly)
            double animProgress = (i < categoryAnimationProgress.length) ? categoryAnimationProgress[i] : 1.0;
            double hiddenX = 10 * scaleX;
            double visibleX = 50 * scaleX;
            int textX = (int)(hiddenX + (visibleX - hiddenX) * animProgress);
            
            // Calculate precise text dimensions using FontMetrics
            float nameSize = (float)(32 * Math.min(scaleX, scaleY));
            
            // Create a temporary graphics context to get exact text metrics
            Graphics2D tempG = (Graphics2D) getGraphics();
            if (tempG != null) {
                tempG.setFont(primaryFont.deriveFont(nameSize));
                FontMetrics fm = tempG.getFontMetrics();
                
                // Get exact text bounds
                java.awt.geom.Rectangle2D textBounds = fm.getStringBounds(categoryName, tempG);
                int exactTextWidth = (int) textBounds.getWidth();
                int exactTextHeight = (int) textBounds.getHeight();
                int textAscent = fm.getAscent();
                int textDescent = fm.getDescent();
                
                tempG.dispose();
                
                // Calculate the rotated text bounds
                // The text is rotated -25° around point (textX, categoryY)
                double angleRad = Math.toRadians(-25);
                double cosAngle = Math.cos(angleRad);
                double sinAngle = Math.sin(angleRad);
                
                // Define the corners of the text rectangle before rotation
                // Text baseline is at categoryY, so top is categoryY - textAscent
                double[] corners = {
                    textX, categoryY - textAscent,                    // Top-left
                    textX + exactTextWidth, categoryY - textAscent,   // Top-right
                    textX + exactTextWidth, categoryY + textDescent,  // Bottom-right
                    textX, categoryY + textDescent                    // Bottom-left
                };
                
                // Rotate all corners around the rotation point (textX, categoryY)
                double[] rotatedCorners = new double[8];
                for (int j = 0; j < 4; j++) {
                    double dx = corners[j*2] - textX;
                    double dy = corners[j*2 + 1] - categoryY;
                    
                    rotatedCorners[j*2] = textX + dx * cosAngle - dy * sinAngle;
                    rotatedCorners[j*2 + 1] = categoryY + dx * sinAngle + dy * cosAngle;
                }
                
                // Find the bounding box of the rotated text
                double minX = rotatedCorners[0], maxX = rotatedCorners[0];
                double minY = rotatedCorners[1], maxY = rotatedCorners[1];
                
                for (int j = 1; j < 4; j++) {
                    minX = Math.min(minX, rotatedCorners[j*2]);
                    maxX = Math.max(maxX, rotatedCorners[j*2]);
                    minY = Math.min(minY, rotatedCorners[j*2 + 1]);
                    maxY = Math.max(maxY, rotatedCorners[j*2 + 1]);
                }
                
                // Add some margin for easier clicking
                int margin = 10;
                if (mouseX >= minX - margin && mouseX <= maxX + margin &&
                    mouseY >= minY - margin && mouseY <= maxY + margin) {
                    
                    // For more precise detection, check if point is inside the rotated rectangle
                    if (isPointInPolygon(mouseX, mouseY, rotatedCorners)) {
                        currentHoverState = HoverState.CATEGORY;
                        hoveredCategory = i;
                        
                        // Update keyboard selection to match mouse hover only when not using keyboard navigation
                        if (!isUsingKeyboardNavigationSettings && (selectedCategory != i || !inCategoryColumn)) {
                            selectedCategory = i;
                            inCategoryColumn = true;
                            selectedCategorySetting = 0; // Reset setting selection
                        }
                        break;
                    }
                }
            } else {
                // Fallback if graphics context is not available
                int textWidth = (int)(categoryName.length() * nameSize * 0.6);
                int textHeight = (int)(nameSize);
                
                if (mouseX >= textX - 10 && mouseX <= textX + textWidth + 10 &&
                    mouseY >= categoryY - textHeight && mouseY <= categoryY + 10) {
                    currentHoverState = HoverState.CATEGORY;
                    hoveredCategory = i;
                    
                    // Update keyboard selection to match mouse hover only when not using keyboard navigation
                    if (!isUsingKeyboardNavigationSettings && (selectedCategory != i || !inCategoryColumn)) {
                        selectedCategory = i;
                        inCategoryColumn = true;
                        selectedCategorySetting = 0;
                    }
                    break;
                }
            }
        }
    }
    
    // Helper method to check if a point is inside a polygon defined by corner coordinates
    private boolean isPointInPolygon(double x, double y, double[] corners) {
        int n = corners.length / 2;
        boolean inside = false;
        
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = corners[i * 2];
            double yi = corners[i * 2 + 1];
            double xj = corners[j * 2];
            double yj = corners[j * 2 + 1];
            
            if (((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
        }
        
        return inside;
    }
    
    private void checkSettingsSimple(int mouseX, int mouseY) {
        // Precise hit detection for settings (right column)
        if (mouseX < BOARD_WIDTH / 2) return; // Only check right column
        
        String[] currentSettings = categorySettings[selectedCategory];
        int settingsStartY = (int)(200 * scaleY);
        int settingsSpacing = (int)(90 * scaleY);
        int centerX = (int)(650 * scaleX);
        
        for (int i = 0; i < currentSettings.length; i++) {
            int settingY = settingsStartY + i * settingsSpacing;
            String settingName = currentSettings[i];
            
            // Calculate text dimensions for setting name
            float nameSize = (float)(26 * Math.min(scaleX, scaleY));
            int textWidth = (int)(settingName.length() * nameSize * 0.6);
            int textHeight = (int)(nameSize * 1.2);
            
            // Also check the value area below
            int valueY = settingY + (int)(40 * scaleY);
            String currentValue = getCurrentCategorySettingValue(selectedCategory, i);
            float valueSize = (float)(20 * Math.min(scaleX, scaleY));
            int valueWidth = (int)(currentValue.length() * valueSize * 0.6);
            int valueHeight = (int)(valueSize * 1.2);
            
            // Check if mouse is over setting name or value (both are not rotated)
            boolean overName = (mouseX >= centerX - textWidth/2 && mouseX <= centerX + textWidth/2 &&
                               mouseY >= settingY - textHeight/2 && mouseY <= settingY + textHeight/2);
            
            boolean overValue = (mouseX >= centerX - valueWidth/2 && mouseX <= centerX + valueWidth/2 &&
                                mouseY >= valueY - valueHeight/2 && mouseY <= valueY + valueHeight/2);
            
            if (overName || overValue) {
                currentHoverState = HoverState.SETTING;
                hoveredSetting = i;
                
                // Update keyboard selection to match mouse hover only when not using keyboard navigation
                if (!isUsingKeyboardNavigationSettings && (selectedCategorySetting != i || inCategoryColumn)) {
                    selectedCategorySetting = i;
                    inCategoryColumn = false;
                }
                break;
            }
        }
    }
    
    // Helper method to check if a point is inside a rotated rectangle
    private boolean isPointInRotatedRect(int pointX, int pointY, int rectX, int rectY, 
                                        int rectWidth, int rectHeight, double angleDegrees) {
        // Convert angle to radians
        double angleRad = Math.toRadians(angleDegrees);
        double cosAngle = Math.cos(angleRad);
        double sinAngle = Math.sin(angleRad);
        
        // Translate point to rectangle's coordinate system (relative to rectangle center)
        double rectCenterX = rectX + rectWidth / 2.0;
        double rectCenterY = rectY;
        double dx = pointX - rectCenterX;
        double dy = pointY - rectCenterY;
        
        // Rotate point back by negative angle to align with rectangle
        double rotatedX = dx * cosAngle + dy * sinAngle;
        double rotatedY = -dx * sinAngle + dy * cosAngle;
        
        // Check if rotated point is inside the non-rotated rectangle
        return (rotatedX >= -rectWidth/2.0 && rotatedX <= rectWidth/2.0 &&
                rotatedY >= -rectHeight/2.0 && rotatedY <= rectHeight/2.0);
    }
    
    private void checkCategoriesPrecise(int mouseX, int mouseY, Graphics2D g2d) {
        if (mouseX >= BOARD_WIDTH / 2) return; // Only check left column
        
        int categoryStartY = (int)(280 * scaleY); // Match exact rendering position
        int categorySpacing = (int)(80 * scaleY);
        
        for (int i = 0; i < categoryNames.length; i++) {
            int categoryY = categoryStartY + i * categorySpacing;
            String categoryName = categoryNames[i];
            
            // Use exact FontMetrics for precision - same font as rendering
            Font font = primaryFont.deriveFont((float)(32 * Math.min(scaleX, scaleY)));
            g2d.setFont(font);
            FontMetrics fm = g2d.getFontMetrics();
            
            // Get precise text bounds using getStringBounds
            java.awt.geom.Rectangle2D textBounds = fm.getStringBounds(categoryName, g2d);
            
            // Calculate exact position where text is drawn (accounting for animation)
            double animProgress = (i < categoryAnimationProgress.length) ? categoryAnimationProgress[i] : 1.0;
            double hiddenX = 10 * scaleX;
            double visibleX = 50 * scaleX;
            int textX = (int)(hiddenX + (visibleX - hiddenX) * animProgress);
            
            // Create precise text rectangle
            int textWidth = (int) textBounds.getWidth();
            int textHeight = (int) textBounds.getHeight();
            int textYTop = categoryY - (int) textBounds.getHeight() / 2;
            
            // Check if mouse is within precise text bounds
            if (mouseX >= textX && mouseX <= textX + textWidth &&
                mouseY >= textYTop && mouseY <= textYTop + textHeight) {
                currentHoverState = HoverState.CATEGORY;
                hoveredCategory = i;
                break;
            }
        }
    }
    
    private void checkSettingsPrecise(int mouseX, int mouseY, Graphics2D g2d) {
        if (mouseX < BOARD_WIDTH / 2) return; // Only check right column
        
        String[] currentSettings = categorySettings[selectedCategory];
        int settingsStartY = (int)(200 * scaleY);
        int settingsSpacing = (int)(90 * scaleY);
        
        for (int i = 0; i < currentSettings.length; i++) {
            int settingY = settingsStartY + i * settingsSpacing;
            String settingName = currentSettings[i];
            String currentValue = getCurrentCategorySettingValue(selectedCategory, i);
            
            // Use exact FontMetrics for setting name
            Font nameFont = primaryFont.deriveFont((float)(28 * Math.min(scaleX, scaleY)));
            g2d.setFont(nameFont);
            FontMetrics nameFm = g2d.getFontMetrics();
            java.awt.geom.Rectangle2D nameBounds = nameFm.getStringBounds(settingName, g2d);
            
            // Use exact FontMetrics for setting value
            Font valueFont = secondaryFont.deriveFont((float)(22 * Math.min(scaleX, scaleY)));
            g2d.setFont(valueFont);
            FontMetrics valueFm = g2d.getFontMetrics();
            java.awt.geom.Rectangle2D valueBounds = valueFm.getStringBounds(currentValue, g2d);
            
            int centerX = (int)(650 * scaleX);
            
            // Check setting name bounds
            int nameWidth = (int) nameBounds.getWidth();
            int nameHeight = (int) nameBounds.getHeight();
            int nameX = centerX - nameWidth / 2;
            int nameYTop = settingY - nameHeight / 2;
            
            boolean overName = (mouseX >= nameX && mouseX <= nameX + nameWidth &&
                               mouseY >= nameYTop && mouseY <= nameYTop + nameHeight);
            
            // Check setting value bounds
            int valueWidth = (int) valueBounds.getWidth();
            int valueHeight = (int) valueBounds.getHeight();
            int valueX = centerX - valueWidth / 2;
            int valueY = settingY + (int)(25 * scaleY);
            int valueYTop = valueY - valueHeight / 2;
            
            boolean overValue = (mouseX >= valueX && mouseX <= valueX + valueWidth &&
                                mouseY >= valueYTop && mouseY <= valueYTop + valueHeight);
            
            if (overName || overValue) {
                currentHoverState = HoverState.SETTING;
                hoveredSetting = i;
                break;
            }
        }
    }
    
    // Fallback methods for when Graphics context is not available
    private void checkCategoriesApproximate(int mouseX, int mouseY) {
        if (mouseX >= BOARD_WIDTH / 2) return;
        
        int categoryStartY = (int)(280 * scaleY);
        int categorySpacing = (int)(80 * scaleY);
        
        for (int i = 0; i < categoryNames.length; i++) {
            int categoryY = categoryStartY + i * categorySpacing;
            String categoryName = categoryNames[i];
            float fontSize = (float)(32 * Math.min(scaleX, scaleY));
            int textWidth = (int)(categoryName.length() * fontSize * 0.6);
            int textHeight = (int)(fontSize * 1.2);
            int textX = (int)(50 * scaleX);
            
            if (mouseX >= textX && mouseX <= textX + textWidth &&
                mouseY >= categoryY - textHeight/2 && mouseY <= categoryY + textHeight/2) {
                currentHoverState = HoverState.CATEGORY;
                hoveredCategory = i;
                break;
            }
        }
    }
    
    private void checkSettingsApproximate(int mouseX, int mouseY) {
        if (mouseX < BOARD_WIDTH / 2) return;
        
        String[] currentSettings = categorySettings[selectedCategory];
        int settingsStartY = (int)(200 * scaleY);
        int settingsSpacing = (int)(90 * scaleY);
        
        for (int i = 0; i < currentSettings.length; i++) {
            int settingY = settingsStartY + i * settingsSpacing;
            String settingName = currentSettings[i];
            String currentValue = getCurrentCategorySettingValue(selectedCategory, i);
            float fontSize = (float)(28 * Math.min(scaleX, scaleY));
            int nameWidth = (int)(settingName.length() * fontSize * 0.6);
            int valueWidth = (int)(currentValue.length() * fontSize * 0.6);
            int textHeight = (int)(fontSize * 1.2);
            int centerX = (int)(650 * scaleX);
            
            int nameX = centerX - nameWidth / 2;
            boolean overName = (mouseX >= nameX && mouseX <= nameX + nameWidth &&
                               mouseY >= settingY - textHeight/2 && mouseY <= settingY + textHeight/2);
            
            int valueX = centerX - valueWidth / 2;
            int valueY = settingY + (int)(25 * scaleY);
            boolean overValue = (mouseX >= valueX && mouseX <= valueX + valueWidth &&
                                mouseY >= valueY - textHeight/2 && mouseY <= valueY + textHeight/2);
            
            if (overName || overValue) {
                currentHoverState = HoverState.SETTING;
                hoveredSetting = i;
                break;
            }
        }
    }
    
    private void handleSettingsMouseClick(MouseEvent e) {
        System.out.println("DEBUG: Click on " + currentHoverState + " (cat: " + hoveredCategory + ", setting: " + hoveredSetting + ")");
        
        // Handle click based on current hover state
        switch (currentHoverState) {
            case CATEGORY:
                // Click on category selects it
                if (hoveredCategory != -1) {
                    selectedCategory = hoveredCategory;
                    selectedCategorySetting = 0;
                    inCategoryColumn = true;
                    repaint();
                }
                break;
                
            case SETTING:
                // Click on setting modifies its value
                if (hoveredSetting != -1 && isLeftMouseButton(e)) {
                    selectedCategorySetting = hoveredSetting;
                    inCategoryColumn = false;
                    handleSettingsValueChange(true); // Increase value
                }
                break;
                
            case NONE:
                // No action for NONE state
                break;
        }
    }
    
    private void handleSettingsValueChange(boolean increase) {
        String[] currentSettings = categorySettings[selectedCategory];
        String currentSetting = currentSettings[selectedCategorySetting];
        
        if (currentSetting.equals("SETTING_PADDLE_SPEED")) {
            if (increase) {
                paddleSpeedSetting = Math.min(paddleSpeedSetting + 1, paddleSpeedOptions.length - 1);
            } else {
                paddleSpeedSetting = Math.max(paddleSpeedSetting - 1, 0);
            }
        } else if (currentSetting.equals("SETTING_AI_DIFFICULTY")) {
            int oldValue = aiDifficultySetting;
            if (increase) {
                aiDifficultySetting = Math.min(aiDifficultySetting + 1, aiDifficultyOptions.length - 1);
            } else {
                aiDifficultySetting = Math.max(aiDifficultySetting - 1, 0);
            }
            // Apply immediately
            aiDifficulty = aiDifficultySetting + 1; // Convert to 1-5 range
            System.out.println("DEBUG (Mouse): Difficoltà IA cambiata da " + oldValue + " a " + aiDifficultySetting + " (aiDifficulty=" + aiDifficulty + ")");
        } else if (currentSetting.equals("SETTING_BALL_SPEED")) {
            if (increase) {
                ballSpeedSetting = Math.min(ballSpeedSetting + 5, 100);
            } else {
                ballSpeedSetting = Math.max(ballSpeedSetting - 5, 5);
            }
        }
        // Add other setting value changes as needed
        
        repaint();
    }
    
    private void handleBackgroundSelectionMouseMove(MouseEvent e) {
        int mouseX = e.getX();
        int mouseY = e.getY();
        
        // Calculate thumbnail layout parameters (same as in drawBackgroundSelection)
        int panelHeight = (int)(120 * scaleY);
        int panelY = getHeight() - panelHeight;
        int thumbWidth = (int)(160 * scaleX);
        int thumbHeight = (int)(92 * scaleY);
        int thumbSpacing = (int)(10 * Math.min(scaleX, scaleY));
        
        // Calculate total width and center position
        int totalWidth = backgroundNames.size() * thumbWidth + (backgroundNames.size() - 1) * thumbSpacing;
        int thumbStartX = (getWidth() - totalWidth) / 2;
        int thumbY = panelY + (panelHeight - thumbHeight) / 2;
        
        // Check which thumbnail the mouse is over
        for (int i = 0; i < backgroundNames.size(); i++) {
            int thumbX = thumbStartX + i * (thumbWidth + thumbSpacing);
            
            if (mouseX >= thumbX && mouseX <= thumbX + thumbWidth && 
                mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                if (selectedBackgroundOption != i) {
                    selectedBackgroundOption = i;
                    repaint();
                }
                return;
            }
        }
    }
    
    private void handleBackgroundSelectionMouseClick(MouseEvent e) {
        int mouseX = e.getX();
        int mouseY = e.getY();
        
        // Scaled thumbnail dimensions to match rendering
        int thumbWidth = (int)(160 * scaleX);
        int thumbHeight = (int)(92 * scaleY);
        int thumbSpacing = (int)(10 * Math.min(scaleX, scaleY));
        
        // Calculate panel position
        int panelHeight = (int)(120 * scaleY);
        int panelY = getHeight() - panelHeight;
        int thumbY = panelY + (panelHeight - thumbHeight) / 2;
        
        // Calculate starting position to center all thumbnails
        int totalWidth = backgroundNames.size() * thumbWidth + (backgroundNames.size() - 1) * thumbSpacing;
        int thumbStartX = (getWidth() - totalWidth) / 2;
        
        // Check which thumbnail was clicked
        int currentX = thumbStartX;
        for (int i = 0; i < backgroundNames.size(); i++) {
            if (mouseX >= currentX && mouseX <= currentX + thumbWidth && 
                mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                selectedBackground = i;
                loadTextColorsForTheme();
                saveSettingsToFile();
                saveBackgroundTheme();
                startThemesToHomeTransition();
                repaint();
                return;
            }
            currentX += thumbWidth + thumbSpacing;
        }
        
        // Check for ESC area (anywhere outside the panel)
        if (mouseY < panelY) {
            startThemesToHomeTransition();
            repaint();
        }
    }
    
    private void handlePaddleSelectionInput(KeyEvent e) {
        int keyCode = e.getKeyCode();
        
        // Handle paddle movement keys with EXACT same system as game
        if (keyCode == player1UpKey) {
            paddleSelectionUpPressed = true;
        } else if (keyCode == player1DownKey) {
            paddleSelectionDownPressed = true;
        }
        
        switch (keyCode) {
            case KeyEvent.VK_ESCAPE:
                // Start paddle-to-home transition instead of direct state change
                startPaddleToHomeTransition();
                repaint();
                break;
            case KeyEvent.VK_ENTER:
                // Save paddle theme and return to menu with transition
                saveSettingsToFile();
                startPaddleToHomeTransition();
                repaint();
                break;
            case KeyEvent.VK_LEFT:
                simpleNavigateLeft(true);
                break;
            case KeyEvent.VK_RIGHT:
                simpleNavigateRight(true);
                break;
            case KeyEvent.VK_UP:
                simpleNavigateUp(true);
                break;
            case KeyEvent.VK_DOWN:
                simpleNavigateDown(true);
                break;
            case KeyEvent.VK_Q:
                // Scroll to previous page
                scrollPaddlePagePrevious(true);
                break;
            case KeyEvent.VK_E:
                // Scroll to next page
                scrollPaddlePageNext(true);
                break;
        }
    }
    
    private void handleRightPaddleSelectionInput(KeyEvent e) {
        int keyCode = e.getKeyCode();
        
        // Handle paddle movement keys with EXACT same system as game but for right paddle (player 2)
        if (keyCode == player2UpKey) {
            paddleSelectionUpPressed = true;
        } else if (keyCode == player2DownKey) {
            paddleSelectionDownPressed = true;
        }
        
        switch (keyCode) {
            case KeyEvent.VK_ESCAPE:
                // Start paddle-to-home transition instead of direct state change
                startPaddleToHomeTransition();
                repaint();
                break;
            case KeyEvent.VK_ENTER:
                // Save right paddle theme and return to menu with transition
                saveSettingsToFile();
                startPaddleToHomeTransition();
                repaint();
                break;
            case KeyEvent.VK_LEFT:
                simpleNavigateLeft(false);
                break;
            case KeyEvent.VK_RIGHT:
                simpleNavigateRight(false);
                break;
            case KeyEvent.VK_UP:
                simpleNavigateUp(false);
                break;
            case KeyEvent.VK_DOWN:
                simpleNavigateDown(false);
                break;
            case KeyEvent.VK_Q:
                // Scroll to previous page
                scrollPaddlePagePrevious(false);
                break;
            case KeyEvent.VK_E:
                // Scroll to next page
                scrollPaddlePageNext(false);
                break;
        }
    }
    
    private void scrollPaddlePagePrevious(boolean isLeftPaddle) {
        // ===== SCROLL FLUIDO PER PAGINA PRECEDENTE =====
        // Usa gli stessi calcoli esatti del metodo applyMomentumScroll
        int panelWidth = getWidth() / 2;
        int panelHeight = getHeight();
        float titleSize = Math.max(16f, Math.min(24f, panelWidth / 15f));
        int headerHeight = (int)(titleSize * 2f);
        int footerHeight = 30;
        int availableHeight = panelHeight - headerHeight - footerHeight;
        int availableWidth = panelWidth;
        
        int horizontalSpacing = 4;
        int verticalSpacing = 8;
        int maxCardSize = Math.min(200, availableHeight / 2);
        int cardSize = (availableWidth - (PADDLE_COLS - 1) * horizontalSpacing) / PADDLE_COLS;
        cardSize = Math.min(cardSize, maxCardSize);
        int minCardSize = Math.max(40, availableWidth / 12);
        cardSize = Math.max(cardSize, minCardSize);
        
        int actualGridWidth = PADDLE_COLS * cardSize + (PADDLE_COLS - 1) * horizontalSpacing;
        if (actualGridWidth < availableWidth) {
            int extraSpace = availableWidth - actualGridWidth;
            cardSize += extraSpace / PADDLE_COLS;
        }
        
        int rowHeight = cardSize + verticalSpacing;
        double scrollAmount = 3.0 * rowHeight; // 3 righe esatte
        
        if (isLeftPaddle) {
            double newTarget = Math.max(0, targetScrollY - scrollAmount);
            startSmoothScroll(newTarget, -1); // Scroll fluido verso l'alto
        } else {
            double newTarget = Math.max(0, targetRightScrollY - scrollAmount);
            startSmoothScroll(-1, newTarget); // Scroll fluido verso l'alto
        }
    }
    
    private void scrollPaddlePageNext(boolean isLeftPaddle) {
        // ===== SCROLL FLUIDO PER PAGINA SUCCESSIVA =====
        // Usa gli stessi calcoli esatti del metodo applyMomentumScroll
        int panelWidth = getWidth() / 2;
        int panelHeight = getHeight();
        float titleSize = Math.max(16f, Math.min(24f, panelWidth / 15f));
        int headerHeight = (int)(titleSize * 2f);
        int footerHeight = 30;
        int availableHeight = panelHeight - headerHeight - footerHeight;
        int availableWidth = panelWidth;
        
        int horizontalSpacing = 4;
        int verticalSpacing = 8;
        int maxCardSize = Math.min(200, availableHeight / 2);
        int cardSize = (availableWidth - (PADDLE_COLS - 1) * horizontalSpacing) / PADDLE_COLS;
        cardSize = Math.min(cardSize, maxCardSize);
        int minCardSize = Math.max(40, availableWidth / 12);
        cardSize = Math.max(cardSize, minCardSize);
        
        int actualGridWidth = PADDLE_COLS * cardSize + (PADDLE_COLS - 1) * horizontalSpacing;
        if (actualGridWidth < availableWidth) {
            int extraSpace = availableWidth - actualGridWidth;
            cardSize += extraSpace / PADDLE_COLS;
        }
        
        int rowHeight = cardSize + verticalSpacing;
        double scrollAmount = 3.0 * rowHeight; // 3 righe esatte
        
        if (isLeftPaddle) {
            int totalThemes = bluePaddleThemeNames.size();
            int totalRows = (int) Math.ceil((double) totalThemes / PADDLE_COLS);
            double totalContentHeight = totalRows * rowHeight;
            double maxScroll = Math.max(0, totalContentHeight - availableHeight);
            
            double newTarget = Math.min(targetScrollY + scrollAmount, maxScroll);
            startSmoothScroll(newTarget, -1);
        } else {
            int totalThemes = redPaddleThemeNames.size();
            int totalRows = (int) Math.ceil((double) totalThemes / PADDLE_COLS);
            double totalContentHeight = totalRows * rowHeight;
            double maxScroll = Math.max(0, totalContentHeight - availableHeight);
            
            double newTarget = Math.min(targetRightScrollY + scrollAmount, maxScroll);
            startSmoothScroll(-1, newTarget);
        }
    }
    
    /**
     * Inizializza il sistema di scroll fluido più avanzato possibile
     * Basato sulle migliori pratiche web e game development
     */
    private void initializeScrollAnimation() {
        // Ferma timer esistente se presente
        if (scrollAnimationTimer != null && scrollAnimationTimer.isRunning()) {
            scrollAnimationTimer.stop();
        }
        
        // Crea timer per animazione smooth scroll a 60fps
        scrollAnimationTimer = new javax.swing.Timer((int)SCROLL_ANIMATION_INTERVAL, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateSmoothScrollAnimation();
            }
        });
        
        // Inizializza valori scroll
        targetScrollY = paddleGridScrollY;
        targetRightScrollY = rightPaddleGridScrollY;
        scrollVelocityY = 0.0;
        rightScrollVelocityY = 0.0;
        isScrollingLeft = false;
        isScrollingRight = false;
        
        // Avvia il timer
        scrollAnimationTimer.start();
    }
    
    /**
     * Aggiorna l'animazione di scroll fluido con physics-based motion
     * Implementa easing, momentum scrolling, e interpolazione avanzata
     */
    private void updateSmoothScrollAnimation() {
        boolean needsRepaint = false;
        
        // === SCROLL PADDLE SINISTRO ===
        if (isScrollingLeft || Math.abs(scrollVelocityY) > MIN_SCROLL_VELOCITY) {
            // Calcola differenza verso il target
            double deltaY = targetScrollY - paddleGridScrollY;
            
            // Applica easing con interpolazione esponenziale
            if (Math.abs(deltaY) > 0.5) {
                // Movimento verso il target con smooth easing
                paddleGridScrollY += deltaY * (1.0 - SCROLL_SMOOTHING);
                scrollVelocityY = deltaY * (1.0 - SCROLL_SMOOTHING);
            } else {
                // Snap al target quando molto vicini
                paddleGridScrollY = targetScrollY;
                scrollVelocityY = 0.0;
            }
            
            // Applica friction/damping per momentum naturale
            scrollVelocityY *= SCROLL_SMOOTHING;
            
            // Ferma scroll se velocità troppo bassa
            if (Math.abs(scrollVelocityY) < MIN_SCROLL_VELOCITY) {
                scrollVelocityY = 0.0;
                isScrollingLeft = false;
            }
            
            needsRepaint = true;
        }
        
        // === SCROLL PADDLE DESTRO ===
        if (isScrollingRight || Math.abs(rightScrollVelocityY) > MIN_SCROLL_VELOCITY) {
            // Calcola differenza verso il target
            double deltaY = targetRightScrollY - rightPaddleGridScrollY;
            
            // Applica easing con interpolazione esponenziale
            if (Math.abs(deltaY) > 0.5) {
                // Movimento verso il target con smooth easing
                rightPaddleGridScrollY += deltaY * (1.0 - SCROLL_SMOOTHING);
                rightScrollVelocityY = deltaY * (1.0 - SCROLL_SMOOTHING);
            } else {
                // Snap al target quando molto vicini
                rightPaddleGridScrollY = targetRightScrollY;
                rightScrollVelocityY = 0.0;
            }
            
            // Applica friction/damping per momentum naturale  
            rightScrollVelocityY *= SCROLL_SMOOTHING;
            
            // Ferma scroll se velocità troppo bassa
            if (Math.abs(rightScrollVelocityY) < MIN_SCROLL_VELOCITY) {
                rightScrollVelocityY = 0.0;
                isScrollingRight = false;
            }
            
            needsRepaint = true;
        }
        
        // Repaint solo se necessario per ottimizzare prestazioni
        if (needsRepaint) {
            repaint();
        }
        
        // Ferma timer se nessun scroll attivo per risparmiare risorse
        if (!isScrollingLeft && !isScrollingRight && 
            Math.abs(scrollVelocityY) < MIN_SCROLL_VELOCITY && 
            Math.abs(rightScrollVelocityY) < MIN_SCROLL_VELOCITY) {
            // Timer resta attivo per responsività immediata
            // Ma non fa repaint inutili
        }
    }
    
    /**
     * Avvia scroll fluido verso target con momentum e physics
     */
    private void startSmoothScroll(double newTargetY, double newTargetRightY) {
        // Aggiorna target scroll
        if (newTargetY >= 0) {
            targetScrollY = newTargetY;
            isScrollingLeft = true;
        }
        if (newTargetRightY >= 0) {
            targetRightScrollY = newTargetRightY;
            isScrollingRight = true;
        }
        
        // Assicura che timer sia attivo
        if (scrollAnimationTimer != null && !scrollAnimationTimer.isRunning()) {
            scrollAnimationTimer.start();
        }
    }
    
    /**
     * Applica momentum scroll con physics realistico
     */
    private void applyMomentumScroll(double wheelRotation, boolean isLeftPanel) {
        // Calcola impulso basato su rotazione wheel
        double scrollImpulse = wheelRotation * SCROLL_SENSITIVITY;
        
        // Calcola dimensioni reali del pannello come in drawSimplePaddleGrid
        int panelWidth = getWidth() / 2;
        int panelHeight = getHeight();
        float titleSize = Math.max(16f, Math.min(24f, panelWidth / 15f));
        int headerHeight = (int)(titleSize * 2f);
        int footerHeight = 30;
        int availableHeight = panelHeight - headerHeight - footerHeight;
        int availableWidth = panelWidth;
        
        // Calcola cardSize ESATTO come in drawSimplePaddleGrid
        int horizontalSpacing = 4;
        int verticalSpacing = 8;
        int maxCardSize = Math.min(200, availableHeight / 2);
        int cardSize = (availableWidth - (PADDLE_COLS - 1) * horizontalSpacing) / PADDLE_COLS;
        cardSize = Math.min(cardSize, maxCardSize);
        int minCardSize = Math.max(40, availableWidth / 12);
        cardSize = Math.max(cardSize, minCardSize);
        
        // Applica espansione se c'è spazio extra
        int actualGridWidth = PADDLE_COLS * cardSize + (PADDLE_COLS - 1) * horizontalSpacing;
        if (actualGridWidth < availableWidth) {
            int extraSpace = availableWidth - actualGridWidth;
            cardSize += extraSpace / PADDLE_COLS;
        }
        
        // Calcola rowHeight ESATTO
        int rowHeight = cardSize + verticalSpacing;
        
        if (isLeftPanel) {
            // Calcola limiti scroll CORRETTI per paddle sinistro
            int totalThemes = bluePaddleThemeNames.size();
            int totalRows = (int) Math.ceil((double) totalThemes / PADDLE_COLS);
            double totalContentHeight = totalRows * rowHeight;
            double maxScroll = Math.max(0, totalContentHeight - availableHeight);
            
            // DEBUG: aggiungi stampa per debuggare
            System.out.println("DEBUG Left: totalThemes=" + totalThemes + ", totalRows=" + totalRows + 
                             ", rowHeight=" + rowHeight + ", availableHeight=" + availableHeight + 
                             ", maxScroll=" + maxScroll + ", currentTarget=" + targetScrollY);
            
            // Applica impulso con physics momentum
            double newTarget = targetScrollY + scrollImpulse;
            newTarget = Math.max(0, Math.min(newTarget, maxScroll));
            
            // Aggiungi velocità per effetto momentum
            scrollVelocityY += scrollImpulse * 0.3;
            
            startSmoothScroll(newTarget, -1);
        } else {
            // Calcola limiti scroll CORRETTI per paddle destro
            int totalThemes = redPaddleThemeNames.size();
            int totalRows = (int) Math.ceil((double) totalThemes / PADDLE_COLS);
            double totalContentHeight = totalRows * rowHeight;
            double maxScroll = Math.max(0, totalContentHeight - availableHeight);
            
            // DEBUG: aggiungi stampa per debuggare
            System.out.println("DEBUG Right: totalThemes=" + totalThemes + ", totalRows=" + totalRows + 
                             ", rowHeight=" + rowHeight + ", availableHeight=" + availableHeight + 
                             ", maxScroll=" + maxScroll + ", currentTarget=" + targetRightScrollY);
            
            // Applica impulso con physics momentum
            double newTarget = targetRightScrollY + scrollImpulse;
            newTarget = Math.max(0, Math.min(newTarget, maxScroll));
            
            // Aggiungi velocità per effetto momentum
            rightScrollVelocityY += scrollImpulse * 0.3;
            
            startSmoothScroll(-1, newTarget);
        }
    }
    
    /**
     * Calcola dimensione ottimale card basata su spazio disponibile
     */
    private int calculateOptimalCardSize() {
        int panelWidth = getWidth() / 2;
        int maxCardWidth = (panelWidth - 80) / PADDLE_COLS; // 80px margini totali
        int cardSize = Math.min(maxCardWidth, 120);
        return Math.max(cardSize, 60);
    }
    
    private void handleSimplePaddleClick(MouseEvent e, boolean isLeftPaddle) {
        int mouseX = e.getX();
        int mouseY = e.getY();
        
        // Determina area pannello
        int panelX = isLeftPaddle ? getWidth() / 2 : 0;
        int panelY = 0;
        int panelWidth = getWidth() / 2;
        int panelHeight = getHeight();
        
        // Se click fuori pannello, torna al menu
        if ((isLeftPaddle && mouseX < panelX) || (!isLeftPaddle && mouseX >= panelWidth)) {
            setState(GameState.MENU);
            repaint();
            return;
        }
        
        // Calcola dimensioni dinamiche (stesso calcolo di drawSimplePaddleGrid)
        float titleSize = Math.max(16f, Math.min(24f, panelWidth / 15f));
        int headerHeight = (int)(titleSize * 2f);
        int footerHeight = 30;
        int availableHeight = panelHeight - headerHeight - footerHeight;
        int availableWidth = panelWidth; // Nessun margine laterale - usa tutto lo spazio
        
        // STESSO CALCOLO di drawSimplePaddleGrid: massimizza dimensioni paddle
        int horizontalSpacing = 4; // Spacing orizzontale fisso tra i paddle
        int verticalSpacing = 8; // Spacing verticale più ampio tra le righe
        int maxCardSize = Math.min(200, availableHeight / 2); // Limite più generoso per paddle grandi
        
        // Calcola il cardSize usando tutto lo spazio rimanente dopo spacing orizzontale
        int cardSize = (availableWidth - (PADDLE_COLS - 1) * horizontalSpacing) / PADDLE_COLS;
        
        // Se supera il limite massimo, usa il limite
        if (cardSize > maxCardSize) {
            cardSize = maxCardSize;
        }
        
        // Assicurati che non sia troppo piccolo
        int minCardSize = Math.max(40, availableWidth / 12);
        if (cardSize < minCardSize) {
            cardSize = minCardSize;
        }
        
        
        // Verifica finale: se c'è spazio extra, ingrandisci i paddle invece dello spacing
        int actualGridWidth = PADDLE_COLS * cardSize + (PADDLE_COLS - 1) * horizontalSpacing;
        if (actualGridWidth < availableWidth) {
            // Distribuisci tutto lo spazio extra sui paddle (rendendoli più grandi)
            int extraSpace = availableWidth - actualGridWidth;
            cardSize += extraSpace / PADDLE_COLS;
        }
        int totalGridWidth = PADDLE_COLS * cardSize + (PADDLE_COLS - 1) * horizontalSpacing;
        int gridStartX = panelX; // Nessun margine - inizia dal bordo del pannello
        
        int gridY = panelY + headerHeight;
        int scrollY = (int)Math.round(isLeftPaddle ? paddleGridScrollY : rightPaddleGridScrollY);
        java.util.List<String> themes = isLeftPaddle ? bluePaddleThemeNames : redPaddleThemeNames;
        
        // Controlla click su carte
        for (int i = 0; i < themes.size(); i++) {
            int col = i % PADDLE_COLS;
            int row = i / PADDLE_COLS;
            
            int cardX = gridStartX + col * (cardSize + horizontalSpacing);
            int cardY = gridY + row * (cardSize + verticalSpacing) - scrollY;
            
            // Se carta visibile e clickata
            if (cardY >= gridY && cardY + cardSize <= gridY + availableHeight) {
                if (mouseX >= cardX && mouseX <= cardX + cardSize &&
                    mouseY >= cardY && mouseY <= cardY + cardSize) {
                    
                    // Seleziona tema
                    if (isLeftPaddle) {
                        selectedPaddleTheme = i;
                    } else {
                        selectedRightPaddleTheme = i;
                    }
                    saveSettingsToFile();
                    repaint();
                    return;
                }
            }
        }
    }
    
    private void handleRightPaddleSelectionMouseClick(MouseEvent e) {
        int mouseX = e.getX();
        int mouseY = e.getY();
        
        // Check clicks on theme cards in the LEFT panel - 4-column scrollable grid
        int panelX = 0;
        int panelY = 0;
        int panelWidth = BOARD_WIDTH / 2;
        int panelHeight = BOARD_HEIGHT;
        
        // Grid layout parameters (same as in drawRightPaddleThemesPanelAt)
        int gridStartY = panelY + (int)(80 * scaleY);
        int cardMargin = (int)(15 * Math.min(scaleX, scaleY));
        int cardSpacing = (int)(10 * Math.min(scaleX, scaleY));
        
        // Calculate card size based on 4 columns
        int availableWidth = panelWidth - (2 * cardMargin) - ((PADDLE_COLS - 1) * cardSpacing);
        int cardWidth = availableWidth / PADDLE_COLS;
        int cardHeight = (int)(100 * Math.min(scaleX, scaleY));
        
        // Check all visible theme cards in scrollable 4-column grid - use RED themes
        int totalThemes = redPaddleThemeNames.size();
        
        for (int i = 0; i < totalThemes; i++) {
            int col = i % PADDLE_COLS;
            int row = i / PADDLE_COLS;
            
            int cardX = panelX + cardMargin + col * (cardWidth + cardSpacing);
            int cardY = gridStartY + row * (cardHeight + cardSpacing) - (int)rightPaddleGridScrollY;
            
            // Only check cards that are visible in the clipped area
            if (cardY + cardHeight >= gridStartY && cardY <= panelY + panelHeight) {
                // Check if click is within this card
                if (mouseX >= cardX && mouseX <= cardX + cardWidth &&
                    mouseY >= cardY && mouseY <= cardY + cardHeight) {
                    selectedRightPaddleTheme = i;
                    updateCachedGlowColors(); // Update cached colors when theme changes
                    saveSettingsToFile(); // Save immediately when theme is selected
                    repaint();
                    return;
                }
            }
        }
        
        // Click outside panel (right half) returns to menu
        if (mouseX > panelX + panelWidth) {
            setState(GameState.MENU);
            repaint();
        }
    }
    
    private void ensureThemeVisible(boolean isLeftPaddle, int themeIndex) {
        // Calculate the row of the selected theme in 4-column grid
        int themeRow = themeIndex / PADDLE_COLS;
        int cardHeight = (int)(100 * Math.min(scaleX, scaleY));
        int cardSpacing = (int)(10 * Math.min(scaleX, scaleY));
        int rowHeight = cardHeight + cardSpacing;
        
        // Calculate current scroll position and visible area
        int currentScroll = (int)Math.round(isLeftPaddle ? paddleGridScrollY : rightPaddleGridScrollY);
        int visibleAreaHeight = 300; // Approximate visible area height
        
        // Theme position on screen
        int themeY = themeRow * rowHeight - currentScroll;
        
        // Check if theme is visible, if not scroll to make it visible
        if (themeY < 0) {
            // Theme is above visible area, scroll up
            int newScroll = Math.max(0, themeRow * rowHeight - 50); // 50px padding from top
            if (isLeftPaddle) {
                paddleGridScrollY = newScroll;
            } else {
                rightPaddleGridScrollY = newScroll;
            }
        } else if (themeY + cardHeight > visibleAreaHeight) {
            // Theme is below visible area, scroll down
            int newScroll = Math.max(0, themeRow * rowHeight - visibleAreaHeight + cardHeight + 50); // 50px padding from bottom
            if (isLeftPaddle) {
                paddleGridScrollY = newScroll;
            } else {
                rightPaddleGridScrollY = newScroll;
            }
        }
    }
    
    private void simpleNavigateUp(boolean isLeftPaddle) {
        int currentTheme = isLeftPaddle ? selectedPaddleTheme : selectedRightPaddleTheme;
        int totalThemes = isLeftPaddle ? bluePaddleThemeNames.size() : redPaddleThemeNames.size();
        
        // Vai alla riga sopra (stessa colonna)
        int newTheme = currentTheme - PADDLE_COLS;
        if (newTheme >= 0) {
            if (isLeftPaddle) {
                selectedPaddleTheme = newTheme;
            } else {
                selectedRightPaddleTheme = newTheme;
            }
            ensureSimpleThemeVisible(isLeftPaddle, newTheme);
            saveSettingsToFile();
            repaint();
        }
    }
    
    private void simpleNavigateDown(boolean isLeftPaddle) {
        int currentTheme = isLeftPaddle ? selectedPaddleTheme : selectedRightPaddleTheme;
        int totalThemes = isLeftPaddle ? bluePaddleThemeNames.size() : redPaddleThemeNames.size();
        
        // Vai alla riga sotto (stessa colonna)
        int newTheme = currentTheme + PADDLE_COLS;
        if (newTheme < totalThemes) {
            if (isLeftPaddle) {
                selectedPaddleTheme = newTheme;
            } else {
                selectedRightPaddleTheme = newTheme;
            }
            ensureSimpleThemeVisible(isLeftPaddle, newTheme);
            saveSettingsToFile();
            repaint();
        }
    }
    
    private void simpleNavigateLeft(boolean isLeftPaddle) {
        int currentTheme = isLeftPaddle ? selectedPaddleTheme : selectedRightPaddleTheme;
        int currentCol = currentTheme % PADDLE_COLS;
        
        // Vai a sinistra se possibile
        if (currentCol > 0) {
            int newTheme = currentTheme - 1;
            if (isLeftPaddle) {
                selectedPaddleTheme = newTheme;
            } else {
                selectedRightPaddleTheme = newTheme;
            }
            ensureSimpleThemeVisible(isLeftPaddle, newTheme);
            saveSettingsToFile();
            repaint();
        }
    }
    
    private void simpleNavigateRight(boolean isLeftPaddle) {
        int currentTheme = isLeftPaddle ? selectedPaddleTheme : selectedRightPaddleTheme;
        int totalThemes = isLeftPaddle ? bluePaddleThemeNames.size() : redPaddleThemeNames.size();
        int currentCol = currentTheme % PADDLE_COLS;
        
        // Vai a destra se possibile
        if (currentCol < PADDLE_COLS - 1 && currentTheme + 1 < totalThemes) {
            int newTheme = currentTheme + 1;
            if (isLeftPaddle) {
                selectedPaddleTheme = newTheme;
            } else {
                selectedRightPaddleTheme = newTheme;
            }
            ensureSimpleThemeVisible(isLeftPaddle, newTheme);
            saveSettingsToFile();
            repaint();
        }
    }
    
    private void ensureSimpleThemeVisible(boolean isLeftPaddle, int themeIndex) {
        int row = themeIndex / PADDLE_COLS;
        
        // Calcola dimensioni dinamiche
        int panelWidth = getWidth() / 2;
        int panelHeight = getHeight();
        float titleSize = Math.max(16f, Math.min(24f, panelWidth / 15f));
        int headerHeight = (int)(titleSize * 2f);
        int footerHeight = 30;
        int availableHeight = panelHeight - headerHeight - footerHeight;
        int availableWidth = panelWidth; // STESSO del rendering (no -30)
        
        // Usa gli STESSI calcoli esatti del rendering (drawSimplePaddleGrid)
        int horizontalSpacing = 4;
        int verticalSpacing = 8; // FISSO come nel rendering
        int maxCardSize = Math.min(200, availableHeight / 2);
        int cardSize = (availableWidth - (PADDLE_COLS - 1) * horizontalSpacing) / PADDLE_COLS;
        cardSize = Math.min(cardSize, maxCardSize);
        int minCardSize = Math.max(40, availableWidth / 12);
        cardSize = Math.max(cardSize, minCardSize);
        
        // Applica espansione se c'è spazio extra (come nel rendering)
        int actualGridWidth = PADDLE_COLS * cardSize + (PADDLE_COLS - 1) * horizontalSpacing;
        if (actualGridWidth < availableWidth) {
            int extraSpace = availableWidth - actualGridWidth;
            cardSize += extraSpace / PADDLE_COLS;
        }
        
        int rowHeight = cardSize + verticalSpacing; // STESSO calcolo del rendering
        int targetY = row * rowHeight;
        
        // ===== SCROLL FLUIDO PER NAVIGAZIONE TASTIERA =====
        
        if (isLeftPaddle) {
            double currentScroll = paddleGridScrollY;
            double newTargetScroll = -1; // -1 significa nessun cambiamento
            
            // DEBUG: Stampa informazioni di debug
            System.out.println("DEBUG LEFT ensureVisible: themeIndex=" + themeIndex + 
                             ", row=" + row + ", targetY=" + targetY + 
                             ", currentScroll=" + currentScroll + 
                             ", availableHeight=" + availableHeight + 
                             ", cardSize=" + cardSize);
            
            // Controlla se dobbiamo scrollare
            if (targetY < currentScroll) {
                // Tema sopra l'area visibile, scrolla su con smooth animation
                newTargetScroll = Math.max(0, targetY - 20); // 20px di padding
                System.out.println("DEBUG LEFT: Scrolling UP to " + newTargetScroll);
            } else if (targetY + cardSize > currentScroll + availableHeight) {
                // Tema sotto l'area visibile, scrolla giù con smooth animation
                newTargetScroll = targetY + cardSize - availableHeight + 20; // 20px di padding
                System.out.println("DEBUG LEFT: Scrolling DOWN to " + newTargetScroll);
            } else {
                System.out.println("DEBUG LEFT: No scroll needed - theme already visible");
            }
            
            if (newTargetScroll >= 0) {
                startSmoothScroll(newTargetScroll, -1); // Avvia scroll fluido per paddle sinistro
            }
            
        } else {
            double currentScroll = rightPaddleGridScrollY;
            double newTargetScroll = -1; // -1 significa nessun cambiamento
            
            // DEBUG: Stampa informazioni di debug
            System.out.println("DEBUG RIGHT ensureVisible: themeIndex=" + themeIndex + 
                             ", row=" + row + ", targetY=" + targetY + 
                             ", currentScroll=" + currentScroll + 
                             ", availableHeight=" + availableHeight + 
                             ", cardSize=" + cardSize);
            
            if (targetY < currentScroll) {
                newTargetScroll = Math.max(0, targetY - 20);
                System.out.println("DEBUG RIGHT: Scrolling UP to " + newTargetScroll);
            } else if (targetY + cardSize > currentScroll + availableHeight) {
                newTargetScroll = targetY + cardSize - availableHeight + 20;
                System.out.println("DEBUG RIGHT: Scrolling DOWN to " + newTargetScroll);
            } else {
                System.out.println("DEBUG RIGHT: No scroll needed - theme already visible");
            }
            
            if (newTargetScroll >= 0) {
                startSmoothScroll(-1, newTargetScroll); // Avvia scroll fluido per paddle destro
            }
        }
    }


    // Cross-platform mouse button detection
    private boolean isLeftMouseButton(MouseEvent e) {
        // Use SwingUtilities for reliable cross-platform left mouse button detection
        return javax.swing.SwingUtilities.isLeftMouseButton(e);
    }
    
    // Mouse cursor visibility methods
    private void hideMouseCursor() {
        if (isMouseVisible && invisibleCursor != null) {
            this.setCursor(invisibleCursor);
            isMouseVisible = false;
        }
    }
    
    private void showMouseCursor() {
        if (!isMouseVisible && defaultCursor != null) {
            this.setCursor(defaultCursor);
            isMouseVisible = true;
        }
    }
    
    private void saveSettingsToFile() {
        System.out.println("DEBUG (Salvataggio): Inizio salvataggio impostazioni...");
        System.out.println("DEBUG: paddleSpeedSetting=" + paddleSpeedSetting + ", aiDifficultySetting=" + aiDifficultySetting + ", ballSpeedSetting=" + ballSpeedSetting);
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
            
            // Create GavaTech/Pong-Ping directory structure
            java.io.File gavaTechDir = new java.io.File(appDataPath, "GavaTech");
            java.io.File pongPingDir = new java.io.File(gavaTechDir, "Pong-Ping");
            
            // Create directories if they don't exist
            if (!pongPingDir.exists()) {
                pongPingDir.mkdirs();
            }
            
            // Create settings.properties file
            java.io.File settingsFile = new java.io.File(pongPingDir, "settings.properties");
            java.util.Properties properties = new java.util.Properties();
            
            // Set all properties
            
            properties.setProperty("paddle.speed", String.valueOf(paddleSpeedSetting));
            properties.setProperty("ai.difficulty", String.valueOf(aiDifficultySetting));
            properties.setProperty("ball.speed", String.valueOf(ballSpeedSetting));
            properties.setProperty("paddle.theme", String.valueOf(selectedPaddleTheme));
            properties.setProperty("right.paddle.theme", String.valueOf(selectedRightPaddleTheme));
            properties.setProperty("player1.up.key", String.valueOf(player1UpKey));
            properties.setProperty("player1.down.key", String.valueOf(player1DownKey));
            properties.setProperty("player2.up.key", String.valueOf(player2UpKey));
            properties.setProperty("player2.down.key", String.valueOf(player2DownKey));
            properties.setProperty("music.volume", String.valueOf(musicVolume));
            properties.setProperty("effects.volume", String.valueOf(effectsVolume));
            properties.setProperty("music.enabled", String.valueOf(musicEnabled));
            properties.setProperty("selected.background", String.valueOf(selectedBackground));
            properties.setProperty("language.code", currentLanguageCode);
            
            System.out.println("DEBUG (Salvataggio): Valori che sto scrivendo nel file:");
            System.out.println("  paddle.speed=" + paddleSpeedSetting);
            System.out.println("  ai.difficulty=" + aiDifficultySetting);
            System.out.println("  ball.speed=" + ballSpeedSetting);
            System.out.println("  paddle.theme=" + selectedPaddleTheme);
            System.out.println("  right.paddle.theme=" + selectedRightPaddleTheme);
            System.out.println("  language.code=" + currentLanguageCode);
            System.out.println("  music.volume=" + musicVolume + ", effects.volume=" + effectsVolume);
            
            // Save properties to file
            java.io.FileOutputStream fos = new java.io.FileOutputStream(settingsFile);
            properties.store(fos, "Pong-Ping Game Settings");
            fos.close();
            
            System.out.println("DEBUG (Salvataggio): Impostazioni salvate con successo in: " + settingsFile.getAbsolutePath());
            
            // Verify the file was written correctly by reading it back
            java.util.Properties verifyProps = new java.util.Properties();
            java.io.FileInputStream verifyFis = new java.io.FileInputStream(settingsFile);
            verifyProps.load(verifyFis);
            verifyFis.close();
            System.out.println("DEBUG (Verifica): Valori effettivamente scritti nel file:");
            System.out.println("  paddle.speed=" + verifyProps.getProperty("paddle.speed"));
            System.out.println("  ai.difficulty=" + verifyProps.getProperty("ai.difficulty"));
            System.out.println("  ball.speed=" + verifyProps.getProperty("ball.speed"));
            System.out.println("  language.code=" + verifyProps.getProperty("language.code"));
            
        } catch (java.io.IOException e) {
            System.out.println("ERRORE nel salvare le impostazioni: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    private boolean settingsFileExists() {
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
            
            // Check if settings.properties file exists
            java.io.File pongPingDir = new java.io.File(new java.io.File(appDataPath, "GavaTech"), "Pong-Ping");
            java.io.File settingsFile = new java.io.File(pongPingDir, "settings.properties");
            
            return settingsFile.exists();
        } catch (Exception e) {
            return false;
        }
    }
    
    private void loadSettingsFromFile() {
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
                loadLanguage(currentLanguageCode);
                updateLocalizedArrays();
                
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
    
    private void saveBackgroundTheme() {
        try {
            // Get user's app data directory (same path as settings)
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
            
            // Find GavaTech/Pong-Ping directory
            java.io.File pongPingDir = new java.io.File(new java.io.File(appDataPath, "GavaTech"), "Pong-Ping");
            
            // Create directories if they don't exist
            if (!pongPingDir.exists()) {
                pongPingDir.mkdirs();
            }
            
            // Create theme.properties file
            java.io.File themeFile = new java.io.File(pongPingDir, "theme.properties");
            java.util.Properties themeProperties = new java.util.Properties();
            
            // Save theme information
            themeProperties.setProperty("selected.background.index", String.valueOf(selectedBackground));
            
            if (selectedBackground >= 0 && selectedBackground < backgroundNames.size()) {
                themeProperties.setProperty("selected.background.name", backgroundNames.get(selectedBackground));
            } else {
                themeProperties.setProperty("selected.background.name", "Nessuno Sfondo");
            }
            
            themeProperties.setProperty("total.backgrounds.count", String.valueOf(backgroundNames.size()));
            themeProperties.setProperty("theme.version", "1.0");
            themeProperties.setProperty("last.updated", String.valueOf(System.currentTimeMillis()));
            
            // Save theme properties to file
            java.io.FileOutputStream fos = new java.io.FileOutputStream(themeFile);
            themeProperties.store(fos, "Pong-Ping Background Theme Settings");
            fos.close();
            
        } catch (java.io.IOException e) {
            System.out.println("Errore nel salvare il tema: " + e.getMessage());
        }
    }
    
    private int getPaddleSpeed() {
        switch (paddleSpeedSetting) {
            case 0: return 4; // Lenta
            case 1: return 6; // Media
            case 2: return 8; // Veloce
            default: return 6;
        }
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
    
    // Fire ball system methods
    private void incrementFireBallSystem() {
        consecutivePaddleBounces++;
        
        // Calculate gradual fire intensity based on bounces
        updateFireIntensity();
        
        // Activate fire effect at 5+ consecutive bounces
        if (consecutivePaddleBounces >= 5) {
            isFireBallActive = true;
        }
        
        // Activate double points at 15+ consecutive bounces
        if (consecutivePaddleBounces >= 15) {
            doublePointsActive = true;
        }
        
        // Activate unlimited speed at 20+ consecutive bounces
        if (consecutivePaddleBounces >= 20) {
            unlimitedSpeedActive = true;
        }
    }
    
    private void resetFireBallSystem() {
        consecutivePaddleBounces = 0;
        isFireBallActive = false;
        doublePointsActive = false;
        unlimitedSpeedActive = false;
        
        // Start smooth fade-out transition
        targetFireIntensity = 0.0f;
    }
    
    private void updateFireIntensity() {
        // Calculate target intensity based on bounces
        if (consecutivePaddleBounces >= 20) {
            targetFireIntensity = 1.0f; // Maximum fire (unlimited speed)
        } else if (consecutivePaddleBounces >= 15) {
            targetFireIntensity = 0.8f; // Strong fire (double points)
        } else if (consecutivePaddleBounces >= 5) {
            targetFireIntensity = 0.6f; // Medium fire (fire ball active)
        } else if (consecutivePaddleBounces >= 1) {
            // Gradual build-up from first bounce to fire activation
            targetFireIntensity = (float)(consecutivePaddleBounces) / 5.0f * 0.4f; // Build up to 40% intensity
        } else {
            targetFireIntensity = 0.0f;
        }
    }
    
    private void updateFireTransition() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastFireUpdate) / 1000.0f; // Convert to seconds
        lastFireUpdate = currentTime;
        
        // Smooth interpolation towards target intensity
        float transitionSpeed;
        if (targetFireIntensity > fireIntensity) {
            // Fire building up - faster transition
            transitionSpeed = 2.0f; // 2 units per second
        } else {
            // Fire cooling down - slower, more realistic transition
            transitionSpeed = 0.8f; // 0.8 units per second (slower fade)
        }
        
        // Interpolate current intensity towards target
        if (Math.abs(targetFireIntensity - fireIntensity) > 0.01f) {
            float direction = Math.signum(targetFireIntensity - fireIntensity);
            fireIntensity += direction * transitionSpeed * deltaTime;
            
            // Clamp to target if very close
            if ((direction > 0 && fireIntensity >= targetFireIntensity) ||
                (direction < 0 && fireIntensity <= targetFireIntensity)) {
                fireIntensity = targetFireIntensity;
            }
        }
        
        // Ensure intensity stays in valid range
        fireIntensity = Math.max(0.0f, Math.min(1.0f, fireIntensity));
    }
    
    // Ranking system methods
    private int calculateRankPoints() {
        int playerScore = debugScore1;
        int aiScore = debugScore2;
        int combos = debugCombos;
        
        // Base score calculation (same logic as rank calculation)
        int basePoints = 0;
        
        // Perfect victory bonus
        if (aiScore == 0) {
            if (playerScore >= 10) basePoints += 1000;
            else if (playerScore >= 8) basePoints += 800;
            else basePoints += 600;
        }
        
        // Victory margin bonus
        int margin = playerScore - aiScore;
        if (margin >= 5) basePoints += 400;
        else if (margin >= 3) basePoints += 200;
        else if (margin >= 1) basePoints += 100;
        
        // Combo multiplier
        float comboMultiplier = 1.0f;
        if (combos >= 15) comboMultiplier = 2.0f;
        else if (combos >= 10) comboMultiplier = 1.7f;
        else if (combos >= 5) comboMultiplier = 1.4f;
        else if (combos >= 3) comboMultiplier = 1.2f;
        
        return (int)(basePoints * comboMultiplier);
    }

    private void calculateDebugRank() {
        int playerScore = debugScore1;
        int aiScore = debugScore2;
        int combos = debugCombos;
        
        // Base score calculation
        int basePoints = 0;
        
        // Perfect victory bonus
        if (aiScore == 0) {
            if (playerScore >= 10) basePoints += 1000;
            else if (playerScore >= 8) basePoints += 800;
            else basePoints += 600;
        }
        
        // Victory margin bonus
        int margin = playerScore - aiScore;
        if (margin >= 5) basePoints += 400;
        else if (margin >= 3) basePoints += 200;
        else if (margin >= 1) basePoints += 100;
        
        // Combo multiplier
        float comboMultiplier = 1.0f;
        if (combos >= 15) comboMultiplier = 2.0f;
        else if (combos >= 10) comboMultiplier = 1.7f;
        else if (combos >= 5) comboMultiplier = 1.4f;
        else if (combos >= 3) comboMultiplier = 1.2f;
        
        // Calculate final score
        int finalScore = (int)(basePoints * comboMultiplier);
        
        // Determine rank
        if (aiScore == 0 && playerScore == 10 && combos >= 15) {
            currentRank = "S+";
        } else if (finalScore >= 1800) {
            currentRank = "S+";
        } else if (finalScore >= 1400 || (aiScore == 0 && playerScore >= 9)) {
            currentRank = "S";
        } else if (finalScore >= 1000 || (aiScore <= 1 && playerScore >= 8)) {
            currentRank = "S-";
        } else if (finalScore >= 700 || margin >= 4) {
            currentRank = "A+";
        } else if (finalScore >= 500 || margin >= 2) {
            currentRank = "A";
        } else if (finalScore >= 350 || margin >= 1) {
            currentRank = "A-";
        } else if (finalScore >= 200 || playerScore >= aiScore) {
            currentRank = "B+";
        } else if (finalScore >= 100) {
            currentRank = "B";
        } else if (finalScore >= 50) {
            currentRank = "B-";
        } else if (finalScore >= 20) {
            currentRank = "C+";
        } else if (finalScore >= 10) {
            currentRank = "C";
        } else if (finalScore >= 0) {
            currentRank = "C-";
        } else {
            currentRank = "D";
        }
        
        // Set rank color
        switch (currentRank) {
            case "S+": rankColor = new Color(255, 215, 0); break; // Gold
            case "S": rankColor = new Color(192, 192, 192); break; // Silver
            case "S-": rankColor = new Color(205, 127, 50); break; // Bronze
            case "A+": rankColor = new Color(138, 43, 226); break; // BlueViolet
            case "A": rankColor = new Color(72, 61, 139); break; // DarkSlateBlue
            case "A-": rankColor = new Color(106, 90, 205); break; // SlateBlue
            case "B+": rankColor = new Color(34, 139, 34); break; // ForestGreen
            case "B": rankColor = new Color(0, 128, 0); break; // Green
            case "B-": rankColor = new Color(154, 205, 50); break; // YellowGreen
            case "C+": rankColor = new Color(255, 140, 0); break; // DarkOrange
            case "C": rankColor = new Color(255, 165, 0); break; // Orange
            case "C-": rankColor = new Color(255, 69, 0); break; // OrangeRed
            default: rankColor = new Color(139, 69, 19); break; // SaddleBrown
        }
    }

    protected String calculateRank() {
        // Only calculate rank for single player mode
        if (currentState != GameState.SINGLE_PLAYER) {
            return "N/A";
        }
        
        int playerScore = score1; // Player is always player 1 in single player
        int aiScore = score2;
        
        // Base score calculation
        int basePoints = 0;
        
        // Perfect victory bonus (10-0, 9-0, 8-0)
        if (aiScore == 0) {
            if (playerScore >= 10) basePoints += 1000; // S+ territory
            else if (playerScore >= 8) basePoints += 800; // S territory  
            else basePoints += 600; // A territory
        }
        
        // Victory margin bonus
        int margin = playerScore - aiScore;
        if (margin >= 5) basePoints += 400; // Dominant victory
        else if (margin >= 3) basePoints += 200; // Clear victory
        else if (margin >= 1) basePoints += 100; // Close victory
        
        // Combo multiplier (based on max combo achieved)
        float comboMultiplier = 1.0f;
        if (maxCombo >= 15) comboMultiplier = 2.0f;      // Incredible combo
        else if (maxCombo >= 10) comboMultiplier = 1.7f; // Great combo
        else if (maxCombo >= 5) comboMultiplier = 1.4f;  // Good combo
        else if (maxCombo >= 3) comboMultiplier = 1.2f;  // Decent combo
        
        // Fire ball bonus
        int fireBallBonus = 0;
        if (consecutivePaddleBounces >= 20) fireBallBonus = 300; // Mastery
        else if (consecutivePaddleBounces >= 15) fireBallBonus = 200; // Expert
        else if (consecutivePaddleBounces >= 10) fireBallBonus = 100; // Skilled
        
        // Rally performance bonus
        int rallyBonus = Math.min(rallies * 10, 200); // Up to 200 points for rallies
        
        // Calculate final score
        int finalScore = (int)((basePoints + fireBallBonus + rallyBonus) * comboMultiplier);
        
        // Determine rank based on final score and specific achievements
        if (aiScore == 0 && playerScore == 10 && maxCombo >= 15) {
            return "S+"; // Perfect game with incredible combo
        } else if (finalScore >= 1800) {
            return "S+";
        } else if (finalScore >= 1400 || (aiScore == 0 && playerScore >= 9)) {
            return "S";
        } else if (finalScore >= 1000 || (aiScore <= 1 && playerScore >= 8)) {
            return "S-";
        } else if (finalScore >= 700 || margin >= 4) {
            return "A+";
        } else if (finalScore >= 500 || margin >= 2) {
            return "A";
        } else if (finalScore >= 350 || margin >= 1) {
            return "A-";
        } else if (finalScore >= 200 || playerScore >= aiScore) {
            return "B+";
        } else if (finalScore >= 100) {
            return "B";
        } else if (finalScore >= 50) {
            return "B-";
        } else if (finalScore >= 20) {
            return "C+";
        } else if (finalScore >= 10) {
            return "C";
        } else if (finalScore >= 0) {
            return "C-";
        } else {
            return "D";
        }
    }
    
    private Color getRankColor(String rank) {
        switch (rank) {
            case "S+": return new Color(255, 215, 0); // Gold
            case "S": return new Color(192, 192, 192);  // Silver
            case "S-": return new Color(205, 127, 50); // Bronze
            case "A+": return new Color(255, 100, 100); // Red
            case "A": return new Color(255, 165, 0);    // Orange  
            case "A-": return new Color(255, 255, 0);   // Yellow
            case "B+": return new Color(0, 255, 0);     // Green
            case "B": return new Color(0, 255, 255);    // Cyan
            case "B-": return new Color(0, 100, 255);   // Blue
            case "C+": return new Color(128, 0, 255);   // Purple
            case "C": return new Color(255, 0, 255);    // Magenta
            case "C-": return new Color(128, 128, 128); // Gray
            case "D": return new Color(64, 64, 64);     // Dark gray
            default: return Color.WHITE;
        }
    }
    
    protected void drawRankScreen(Graphics2D g) {
        // Se siamo nello stato RANK dedicato, usa lo sfondo del tema, altrimenti sfondo scuro
        if (currentState == GameState.RANK) {
            drawRankScreenWithTheme(g);
        } else {
            // Dark background (per overlay)
            g.setColor(new Color(0, 0, 0, 220));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        
        // Update animation frame
        rankAnimationFrame++;
        float animProgress = Math.min(rankAnimationFrame / 90.0f, 1.0f); // 1.5 seconds animation
        
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        
        // Calculate paddle position for positioning other elements
        int paddleHeight = (int)(menuPaddleHeight * 1.8);
        int paddleYOffset = (int)(-paddleHeight * 0.2);
        int paddleCenterY = paddleYOffset + paddleHeight / 2;
        
        // Draw rank spostato più a destra del centro
        int rankX = centerX + (int)(150 * Math.min(scaleX, scaleY)); // Spostato a destra
        drawRankDisplay(g, rankX, centerY, animProgress);
        
        // Draw scrolling game info behind paddle in top-right
        drawScrollingGameInfo(g, animProgress);
        
        // Draw winner paddle in same position as left paddle in home screen (on top of rank)
        drawWinnerPaddle(g, animProgress);
        
        
        // Difficulty removed - now only shown in scrolling text
        
    }
    
    private void drawWinnerPaddle(Graphics2D g, float animProgress) {
        // Determine which paddle won
        boolean leftWon = score1 > score2;
        int winnerThemeIndex = leftWon ? selectedPaddleTheme : selectedRightPaddleTheme;
        
        // Get winner's theme image
        ArrayList<BufferedImage> winnerImages = leftWon ? bluePaddleThemeImages : redPaddleThemeImages;
        
        if (winnerThemeIndex < winnerImages.size()) {
            BufferedImage winnerImg = winnerImages.get(winnerThemeIndex);
            
            if (winnerImg != null) {
                // Posizione iniziale: paddle di gioco (scalata)
                int gameLeftPaddleX = (int)(20 * scaleX);
                int gameRightPaddleX = BOARD_WIDTH - gameLeftPaddleX - PADDLE_WIDTH;
                int gamePaddleY = leftWon ? paddle1Y : paddle2Y;
                
                // Posizione finale: paddle home screen
                int paddleWidth = (int)(250 * Math.min(scaleX, scaleY));
                int paddleHeight = (int)(menuPaddleHeight * 1.8);
                int paddleYOffset = (int)(-paddleHeight * 0.2);
                int homeCenterX = 0; // Posizione home (attaccato a sinistra)
                int homeCenterY = paddleYOffset + paddleHeight / 2;
                
                // Interpolazione posizione durante la transizione
                double transitionProgress = easeInOutQuad(rankPaddleProgress);
                
                // Calcola posizione corrente del paddle
                int startX = leftWon ? gameLeftPaddleX + PADDLE_WIDTH/2 : gameRightPaddleX + PADDLE_WIDTH/2;
                int startY = gamePaddleY + PADDLE_HEIGHT/2;
                int currentCenterX = (int)(startX + (homeCenterX - startX) * transitionProgress);
                int currentCenterY = (int)(startY + (homeCenterY - startY) * transitionProgress);
                
                // Interpolazione dimensioni (da paddle di gioco a paddle home)
                int currentWidth = (int)(PADDLE_WIDTH + (paddleWidth - PADDLE_WIDTH) * transitionProgress);
                int currentHeight = (int)(PADDLE_HEIGHT + (paddleHeight - PADDLE_HEIGHT) * transitionProgress);
                
                // Save current transform
                AffineTransform oldTransform = g.getTransform();
                
                g.translate(currentCenterX, currentCenterY);
                
                // Rotazione: da 0° (gioco) a -25° (home)
                double currentRotation = -25 * transitionProgress;
                g.rotate(Math.toRadians(currentRotation));
                
                // Draw paddle image con dimensioni attuali dell'animazione
                int cornerRadius = Math.max(4, currentWidth / 4);
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(-currentWidth/2, -currentHeight/2, currentWidth, currentHeight, cornerRadius, cornerRadius));
                
                int imgX = -currentWidth / 2;
                int imgY = -currentHeight / 2;
                g.drawImage(winnerImg, imgX, imgY, currentWidth, currentHeight, null);
                
                g.setClip(null);
                
                // Add glow effect che cresce con l'animazione
                if (transitionProgress > 0.3) {
                    Color glowColor = leftWon ? new Color(100, 150, 255, 40) : new Color(255, 100, 100, 40);
                    g.setColor(glowColor);
                    int glowWidth = Math.max(2, (int)(8 * Math.min(scaleX, scaleY)));
                    g.fillRect(currentWidth/2, imgY, glowWidth, currentHeight);
                }
                
                // Restore transform
                g.setTransform(oldTransform);
            }
        }
    }
    
    private void drawRankScreenWithTheme(Graphics2D g) {
        // Draw the selected background theme
        if (selectedBackground >= 0 && selectedBackground < backgroundImages.size()) {
            Image backgroundImg = backgroundImages.get(selectedBackground);
            
            if (backgroundImg != null) {
                // Draw background image scaled to full screen
                g.drawImage(backgroundImg, 0, 0, getWidth(), getHeight(), this);
                
                // Add subtle contrast effect for better text visibility
                drawBackgroundContrastEffect(g);
            } else {
                // Draw default black background
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        } else {
            // Fallback to black background
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        
        // Add semi-transparent overlay for better rank visibility
        g.setColor(new Color(0, 0, 0, 100));
        g.fillRect(0, 0, getWidth(), getHeight());
    }
    
    private void drawRankDisplay(Graphics2D g, int x, int y, float animProgress) {
        // Nuova animazione: il testo entra da destra solo dopo che il paddle ha finito la transizione
        if (!rankTextTransitionStarted || rankTextProgress <= 0) return;
        
        // Slide progress: da destra verso la posizione finale
        double slideProgress = easeInOutQuad(rankTextProgress);
        double fadeProgress = rankTextProgress;
        
        // Center the rank text horizontally based on font size
        int rankX = x; // Default to passed position
        
        // Draw rank with large font - much bigger than window height
        float fontSize = getHeight() * 1.5f; // 150% of window height
        if (rankFont != null) {
            g.setFont(rankFont.deriveFont(fontSize));
        } else {
            g.setFont(primaryFont.deriveFont(Font.BOLD, fontSize));
        }
        
        // Get rank color with fade animation
        Color rankColor = getRankColor(finalRank);
        int alpha = Math.max(0, Math.min(255, (int)(255 * fadeProgress)));
        Color animatedColor = new Color(rankColor.getRed(), rankColor.getGreen(), rankColor.getBlue(), alpha);
        g.setColor(animatedColor);
        
        // Draw rank with separate positioning for letter and modifier
        FontMetrics fm = g.getFontMetrics();
        int textY = y + fm.getAscent() / 2; // Use passed Y position
        
        // Split rank into letter and modifier
        String rankLetter = "";
        String rankModifier = "";
        
        if (finalRank != null && finalRank.length() > 0) {
            rankLetter = finalRank.substring(0, 1); // First character (S, A, B, C, D)
            if (finalRank.length() > 1) {
                rankModifier = finalRank.substring(1); // Rest (+, -, or empty)
            }
        }
        
        // Calculate total text width for animation
        int letterWidth = fm.stringWidth(rankLetter);
        int modifierWidth = rankModifier.isEmpty() ? 0 : fm.stringWidth(rankModifier);
        int spacing = rankModifier.isEmpty() ? 0 : (int)(20 * Math.min(scaleX, scaleY));
        int totalTextWidth = letterWidth + spacing + modifierWidth;
        
        // Animazione: parte da sinistra (fuori schermo) e arriva alla posizione finale
        int finalX = x - totalTextWidth / 2; // Posizione finale centrata
        int startX = -totalTextWidth; // Inizia fuori schermo a sinistra
        int currentX = (int)(startX + (finalX - startX) * slideProgress);
        
        // Draw main letter con posizione animata
        g.drawString(rankLetter, currentX, textY);
        
        // Draw modifier after the letter con posizione animata
        if (!rankModifier.isEmpty()) {
            int modifierX = currentX + letterWidth + spacing;
            g.drawString(rankModifier, modifierX, textY);
        }
        
    }
    
    private void drawDifficultyDisplay(Graphics2D g, float animProgress) {
        // Only show after some animation progress
        if (animProgress < 0.5f) return;
        
        // Only show in single player mode
        if (currentState != GameState.GAME_OVER) return;
        
        // Get difficulty name only
        String difficultyText = aiDifficultyOptions[aiDifficultySetting];
        
        // Font and positioning - use large primary font like the stats above
        float fontSize = 32f * (float)Math.min(scaleX, scaleY);
        g.setFont(primaryFont.deriveFont(fontSize));
        FontMetrics fm = g.getFontMetrics();
        
        // Position in bottom-right corner with padding
        int textWidth = fm.stringWidth(difficultyText);
        int textX = getWidth() - textWidth - (int)(30 * scaleX); // More padding for larger text
        int textY = getHeight() - (int)(30 * scaleY); // More padding from bottom
        
        // Use the same special difficulty drawing with effects from settings
        drawDifficultyText(g, difficultyText, textX, textY, fontSize, aiDifficultySetting);
    }

    private void drawScrollingGameInfo(Graphics2D g, float animProgress) {
        // Show only after paddle transition is complete
        if (!scrollingTextStarted) return;
        
        // Font and positioning - use primary font scaled to window, very large size
        g.setFont(primaryFont.deriveFont(64f * (float)Math.min(scaleX, scaleY)));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        
        // Position finale: in alto centrata (adattata per font più grande)
        int finalY = (int)(60 * Math.min(scaleX, scaleY));
        
        // During drop animation, interpolate Y position from top of screen
        int currentY;
        if (!scrollingTextEntryComplete) {
            // Drop animation dall'alto
            double dropProgress = easeInOutQuad(scrollingTextDropProgress);
            int startY = -fm.getHeight(); // Inizia fuori schermo in alto
            currentY = (int)(startY + (finalY - startY) * dropProgress);
        } else {
            currentY = finalY;
        }
        
        // Prepare scrolling game info text including colored difficulty
        String difficultyText = "DIFFICULTY: " + aiDifficultyOptions[aiDifficultySetting];
        String[] gameInfo = {
            "MAX COMBO: " + maxCombo,
            "RALLIES: " + rallies,
            "DURATION: " + formatGameTime(),
            difficultyText
        };
        String scrollText = String.join("   ", gameInfo) + "   ";
        int totalTextWidth = fm.stringWidth(scrollText);
        
        // Infinite scrolling from left to right
        float baseScrollSpeed = 2.0f * (float)Math.min(scaleX, scaleY);
        float scrollOffset = (rankAnimationFrame * baseScrollSpeed) % totalTextWidth;
        int gameInfoStartX = (int)(-totalTextWidth + (scrollOffset % totalTextWidth));
        
        // Draw infinite scrolling game info with colored difficulty (multiple copies to fill screen)
        int currentX = gameInfoStartX;
        while (currentX < getWidth() + totalTextWidth) {
            if (currentX > -totalTextWidth) { // Only draw if potentially visible
                drawColoredScrollingText(g, gameInfo, currentX, currentY, fm);
            }
            currentX += totalTextWidth;
        }
        
        // Fill screen from right to left  
        currentX = gameInfoStartX - totalTextWidth;
        while (currentX > -totalTextWidth * 2) {
            if (currentX < getWidth()) { // Only draw if potentially visible
                drawColoredScrollingText(g, gameInfo, currentX, currentY, fm);
            }
            currentX -= totalTextWidth;
        }
    }
    
    private void drawColoredScrollingText(Graphics2D g, String[] gameInfo, int startX, int y, FontMetrics fm) {
        int currentX = startX;
        
        for (int i = 0; i < gameInfo.length; i++) {
            String text = gameInfo[i];
            
            if (i == 3) { // Difficulty text (4th element)
                // Extract just the difficulty value (after "DIFFICULTY: ")
                String difficultyValue = text.substring("DIFFICULTY: ".length());
                
                // Draw "DIFFICULTY: " in white
                g.setColor(Color.WHITE);
                g.drawString("DIFFICULTY: ", currentX, y);
                currentX += fm.stringWidth("DIFFICULTY: ");
                
                // Draw difficulty value with special coloring
                float fontSize = 64f * (float)Math.min(scaleX, scaleY);
                drawDifficultyTextAt(g, difficultyValue, currentX, y, fontSize, aiDifficultySetting);
                currentX += fm.stringWidth(difficultyValue);
            } else {
                // Normal white text for other info
                g.setColor(Color.WHITE);
                g.drawString(text, currentX, y);
                currentX += fm.stringWidth(text);
            }
            
            // Add spacing between elements (except for last element)
            if (i < gameInfo.length - 1) {
                g.setColor(Color.WHITE);
                g.drawString("   ", currentX, y);
                currentX += fm.stringWidth("   ");
            }
        }
        
        // Add final spacing for seamless loop
        g.setColor(Color.WHITE);
        g.drawString("   ", currentX, y);
    }
    
    private void drawDifficultyTextAt(Graphics2D g, String text, int x, int y, float fontSize, int difficulty) {
        // Use same coloring logic as drawDifficultyText but at specific position
        Color baseColor;
        Color glowColor;
        
        switch (difficulty) {
            case 0: // Easy - Green
                baseColor = new Color(50, 200, 50);
                glowColor = new Color(0, 255, 0, 80);
                break;
            case 1: // Normal - Blue
                baseColor = new Color(50, 150, 255);
                glowColor = new Color(0, 200, 255, 80);
                break;
            case 2: // Hard - Orange
                baseColor = new Color(255, 150, 50);
                glowColor = new Color(255, 100, 0, 80);
                break;
            case 3: // Expert - Red
                baseColor = new Color(255, 50, 50);
                glowColor = new Color(255, 0, 0, 80);
                break;
            case 4: // Impossible - Purple
                baseColor = new Color(200, 50, 255);
                glowColor = new Color(150, 0, 255, 80);
                break;
            default:
                baseColor = Color.WHITE;
                glowColor = new Color(255, 255, 255, 80);
        }
        
        // Draw glow effect
        int glowOffset = Math.max(1, (int)(2 * Math.min(scaleX, scaleY)));
        g.setColor(glowColor);
        for (int i = 1; i <= glowOffset; i++) {
            g.drawString(text, x - i, y - i);
            g.drawString(text, x + i, y + i);
        }
        
        // Draw main text
        g.setColor(baseColor);
        g.drawString(text, x, y);
    }
    
    private String formatGameTime() {
        if (gameStartTime == 0) return "0:00";
        
        // Use game end time if game is over, otherwise current time
        long endTime = gameEndTime > 0 ? gameEndTime : System.currentTimeMillis();
        long duration = endTime - gameStartTime;
        
        int seconds = (int)(duration / 1000) % 60;
        int minutes = (int)(duration / 60000);
        return String.format("%d:%02d", minutes, seconds);
    }
    
    private float getCurrentSpeedMultiplier() {
        // Calculate current speed multiplier based on ball speed
        return (float)(Math.sqrt(ballVX * ballVX + ballVY * ballVY) / BASE_BALL_SPEED);
    }

    private void drawStatsDisplay(Graphics2D g, int centerX, int centerY, float animProgress) {
        // Animation: stats fade in and slide down from above
        float slideProgress = (float)easeInOutQuad(Math.min((animProgress - 0.5f) * 2.0f, 1.0f));
        float fadeProgress = (float)easeInOutQuad(Math.min((animProgress - 0.6f) * 2.5f, 1.0f));
        
        if (slideProgress <= 0) return;
        
        // Slide down from above
        int statsY = centerY + (int)((50 - 50 * slideProgress) * scaleY);
        
        // Stats to display
        String[] statLabels = {"Max Combo", "Fire Ball", "Total Rally"};
        String[] statValues = {
            String.valueOf(consecutivePaddleBounces),
            isFireBallActive ? "Attivo" : "Inattivo", 
            String.valueOf(rallies)
        };
        
        // Setup font
        g.setFont(secondaryFont.deriveFont(14.0f * (float)Math.min(scaleX, scaleY)));
        FontMetrics fm = g.getFontMetrics();
        
        // Calculate total width for centering
        int maxWidth = 0;
        for (int i = 0; i < statLabels.length; i++) {
            String line = statLabels[i] + ": " + statValues[i];
            maxWidth = Math.max(maxWidth, fm.stringWidth(line));
        }
        
        // Draw each stat line perpendicular (horizontal) above paddle
        int alpha = Math.max(0, Math.min(255, (int)(255 * fadeProgress)));
        int lineHeight = fm.getHeight() + (int)(2 * scaleY);
        int startY = statsY - (statLabels.length * lineHeight) / 2;
        
        for (int i = 0; i < statLabels.length; i++) {
            String line = statLabels[i] + ": " + statValues[i];
            int lineY = startY + i * lineHeight;
            int lineX = centerX - fm.stringWidth(line) / 2;
            
            // Label in light gray
            g.setColor(new Color(180, 180, 180, alpha));
            String label = statLabels[i] + ": ";
            g.drawString(label, lineX, lineY);
            
            // Value in white (or themed color)
            Color valueColor = Color.WHITE;
            if (i == 1 && isFireBallActive) { // Fire ball active
                valueColor = new Color(255, 140, 0); // Orange
            } else if (i == 0 && consecutivePaddleBounces >= 10) { // High combo
                valueColor = new Color(255, 215, 0); // Gold
            }
            
            g.setColor(new Color(valueColor.getRed(), valueColor.getGreen(), valueColor.getBlue(), alpha));
            int valueX = lineX + fm.stringWidth(label);
            g.drawString(statValues[i], valueX, lineY);
        }
        
        // Draw decorative line under stats
        if (fadeProgress > 0.8f) {
            int lineAlpha = Math.max(0, Math.min(255, (int)(120 * (fadeProgress - 0.8f) / 0.2f)));
            g.setColor(new Color(100, 100, 100, lineAlpha));
            int lineY = startY + statLabels.length * lineHeight + (int)(5 * scaleY);
            int lineWidth = maxWidth / 2;
            g.drawLine(centerX - lineWidth / 2, lineY, centerX + lineWidth / 2, lineY);
        }
    }
    
    private void drawContinuePrompt(Graphics2D g, float animProgress) {
        // Animation: fade in and gentle pulse
        float fadeProgress = (float)easeInOutQuad(Math.min((animProgress - 0.8f) * 5.0f, 1.0f));
        
        if (fadeProgress <= 0) return;
        
        // Gentle pulsing effect
        float pulse = (float)(0.9f + 0.1f * Math.sin(System.currentTimeMillis() / 500.0));
        
        // Position at bottom center
        int promptY = getHeight() - (int)(60 * scaleY);
        int promptX = getWidth() / 2;
        
        // Setup font
        g.setFont(secondaryFont.deriveFont(16.0f * (float)Math.min(scaleX, scaleY) * pulse));
        FontMetrics fm = g.getFontMetrics();
        
        // Text with fade animation
        String promptText = getText("SETTINGS_PRESS_ENTER_CONTINUE");
        int alpha = Math.max(0, Math.min(255, (int)(200 * fadeProgress)));
        g.setColor(new Color(220, 220, 220, alpha));
        
        int textX = promptX - fm.stringWidth(promptText) / 2;
        g.drawString(promptText, textX, promptY);
        
        // Draw subtle glow effect
        if (fadeProgress > 0.7f) {
            int glowAlpha = Math.max(0, Math.min(255, (int)(60 * (fadeProgress - 0.7f) / 0.3f)));
            g.setColor(new Color(100, 150, 255, glowAlpha));
            g.drawString(promptText, textX - 1, promptY - 1);
            g.drawString(promptText, textX + 1, promptY + 1);
        }
    }
    
    private void drawFireBallStatus(Graphics2D g, int shadowOffset) {
        // Complete minimalism - no visual clutter, just fire effects on the ball itself
        // The player will notice fire effects and double points when they score
    }
    
    private void drawMinimalistBounceCounter(Graphics2D g, int shadowOffset) {
        // Only show counter when there are bounces (≥3 for subtlety)
        if (consecutivePaddleBounces < 3) return;
        
        // Position: elegant top-right corner
        int rightMargin = (int)(15 * scaleX);
        int topMargin = (int)(15 * scaleY);
        int counterX = BOARD_WIDTH - rightMargin;
        int counterY = topMargin;
        
        // Determine color based on state (subtle color coding)
        Color accentColor = getFireStateColor();
        
        // Ultra-minimalist design: just number + subtle dots
        drawSimpleCounter(g, counterX, counterY, accentColor, shadowOffset);
        
        // Milestone dots (very subtle)
        drawMilestoneDotsMinimal(g, counterX, counterY + (int)(25 * scaleY), accentColor);
    }
    
    private Color getFireStateColor() {
        if (unlimitedSpeedActive) {
            return new Color(255, 80, 80); // Soft red
        } else if (doublePointsActive) {
            return new Color(255, 180, 60); // Soft orange
        } else if (isFireBallActive) {
            return new Color(255, 220, 80); // Soft yellow
        } else {
            return new Color(200, 200, 200, 180); // Subtle gray
        }
    }
    
    private void drawSimpleCounter(Graphics2D g, int x, int y, Color accentColor, int shadowOffset) {
        // Large, clean number
        float fontSize = (float)(28 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(Font.BOLD, fontSize));
        FontMetrics fm = g.getFontMetrics();
        
        String countText = String.valueOf(consecutivePaddleBounces);
        int textWidth = fm.stringWidth(countText);
        
        // Position text (right-aligned)
        int textX = x - textWidth;
        int textY = y + fm.getAscent();
        
        // Subtle pulsing only when fire is active
        if (isFireBallActive) {
            long time = System.currentTimeMillis();
            float pulse = (float)(0.85 + 0.15 * Math.sin(time * 0.01));
            int alpha = (int)(accentColor.getAlpha() * pulse);
            accentColor = new Color(accentColor.getRed(), accentColor.getGreen(), 
                                  accentColor.getBlue(), alpha);
        }
        
        // Subtle shadow
        g.setColor(new Color(0, 0, 0, 100));
        g.drawString(countText, textX + 1, textY + 1);
        
        // Main number
        g.setColor(accentColor);
        g.drawString(countText, textX, textY);
    }
    
    private void drawMilestoneDotsMinimal(Graphics2D g, int centerX, int centerY, Color baseColor) {
        // Three tiny dots representing 5, 15, 20
        int[] milestones = {5, 15, 20};
        int dotSpacing = (int)(8 * scaleX);
        int startX = centerX - dotSpacing;
        
        for (int i = 0; i < 3; i++) {
            int dotX = startX - (i * dotSpacing);
            int dotY = centerY;
            
            // Micro dots (very subtle)
            int dotSize = 3;
            
            Color dotColor;
            if (consecutivePaddleBounces >= milestones[i]) {
                // Achieved milestone - same color as counter but more vibrant
                dotColor = new Color(baseColor.getRed(), 
                                   Math.min(255, baseColor.getGreen() + 50), 
                                   baseColor.getBlue(), 200);
            } else {
                // Not achieved - very subtle gray
                dotColor = new Color(100, 100, 100, 80);
            }
            
            g.setColor(dotColor);
            g.fillOval(dotX - dotSize/2, dotY - dotSize/2, dotSize, dotSize);
        }
    }
    
    
    private void drawFireBall(Graphics2D g) {
        // Update fire transition animation
        updateFireTransition();
        
        int glowSize = (int)(5 * Math.min(scaleX, scaleY));
        
        // Draw fire effects based on current intensity (gradual, not boolean)
        if (fireIntensity > 0.01f) {
            // Advanced fire animation with multiple time frequencies
            long time = System.currentTimeMillis();
            float baseFlicker = (float)(0.85 + 0.15 * Math.sin(time * 0.012));
            float fastFlicker = (float)(0.9 + 0.1 * Math.sin(time * 0.025));
            float slowFlicker = (float)(0.8 + 0.2 * Math.sin(time * 0.008));
            
            // Fire cooling factor - flames get smaller as they rise (classic fire algorithm)
            float coolingFactor = 0.85f;
            
            // Multiple fire glow layers with realistic heat distribution (scaled by current intensity)
            int maxFireSize = (int)(glowSize * (1 + fireIntensity * 4)); // Scale with intensity
            
            // Draw fewer flame layers for better performance (reduced from 6 to 3)
            for (int layer = 0; layer < 3; layer++) {
                float layerRatio = 1.0f - (layer / 3.0f);
                int layerSize = (int)(maxFireSize * layerRatio * layerRatio * fireIntensity); // Scale with intensity
                
                // Calculate realistic fire colors using HSL-like approach
                Color fireColor = calculateFireColor(layerRatio, fireIntensity, baseFlicker * fastFlicker);
                
                // Add random displacement for flame movement (fractal noise simulation)
                int offsetX = (int)(Math.sin(time * 0.015 + layer) * layerRatio * 3 * fireIntensity);
                int offsetY = (int)(Math.cos(time * 0.018 + layer) * layerRatio * 2 * fireIntensity);
                
                g.setColor(fireColor);
                g.fillOval((int)ballX - layerSize + offsetX, (int)ballY - layerSize + offsetY, 
                          BALL_SIZE + layerSize*2, BALL_SIZE + layerSize*2);
            }
            
            // Core fire ball with advanced gradient (colors based on intensity)
            Color coreColor1, coreColor2, coreColor3, coreColor4;
            
            // Calculate colors based on fire intensity (gradual color progression)
            if (fireIntensity < 0.2f) {
                // Very early fire - subtle warmth but ALWAYS opaque ball
                coreColor1 = new Color(255, 255, 255, 255); // Always fully opaque center
                coreColor2 = new Color(255, 250, 220, 255); // Always fully opaque
                coreColor3 = new Color(255, 240, 200, (int)(200 + 55 * fireIntensity)); // Slight transparency at edge
                coreColor4 = new Color(255, 220, 180, (int)(150 + 105 * fireIntensity)); // Gradual edge
            } else if (fireIntensity < 0.6f) {
                // Building fire - yellow tones but ALWAYS opaque ball
                float progression = (fireIntensity - 0.2f) / 0.4f; // 0 to 1 in this range
                coreColor1 = new Color(255, 255, 255, 255); // Always fully opaque center
                coreColor2 = new Color(255, 245, (int)(150 + 50 * progression), 255); // Always fully opaque
                coreColor3 = new Color(255, (int)(200 - 40 * progression), (int)(100 + 50 * progression), 255); // Always opaque
                coreColor4 = new Color(255, (int)(150 - 50 * progression), (int)(60 + 40 * progression), (int)(200 + 55 * progression)); // Slight edge transparency
            } else {
                // Strong fire - orange to red progression but ALWAYS opaque ball
                float progression = (fireIntensity - 0.6f) / 0.4f; // 0 to 1 in this range
                coreColor1 = new Color(255, 255, 255, 255); // Always fully opaque center
                coreColor2 = new Color(255, 240, 150, 255); // Always fully opaque
                coreColor3 = new Color(255, (int)(120 - 40 * progression), (int)(20 + 40 * progression), 255); // Always opaque
                coreColor4 = new Color(255, (int)(100 - 50 * progression), 0, (int)(200 + 55 * progression)); // Slight edge transparency
            }
            
            RadialGradientPaint fireCore = new RadialGradientPaint(
                (float)(ballX + BALL_SIZE/2.0), (float)(ballY + BALL_SIZE/2.0), (float)(BALL_SIZE/2.0),
                new float[]{0.0f, 0.3f, 0.7f, 1.0f},
                new Color[]{coreColor1, coreColor2, coreColor3, coreColor4}
            );
            g.setPaint(fireCore);
            g.fillOval((int)ballX, (int)ballY, BALL_SIZE, BALL_SIZE);
            
            // Frame-based optimization: only create particles every few frames
            frameCounter++;
            if (frameCounter % 4 == 0 && Math.random() < 0.15 * fireIntensity) { // Only every 4th frame
                // Generate fewer particles (max 1 per frame instead of 3)
                int particleCount = fireIntensity > 0.5f ? 1 : 0; // Maximum 1 particle per frame
                
                if (particleCount > 0) {
                    double angle = Math.random() * 2 * Math.PI;
                    double distance = Math.random() * BALL_SIZE * 0.5; // Reduced spread
                    int particleX = (int)(ballX + BALL_SIZE/2 + Math.cos(angle) * distance);
                    int particleY = (int)(ballY + BALL_SIZE/2 + Math.sin(angle) * distance);
                    
                    // Use simpler color calculation for better performance
                    Color particleColor = fireIntensity > 0.5f ? 
                        new Color(255, (int)(100 + 155 * fireIntensity), 0, 150) : 
                        new Color(255, 200, 0, 100);
                    createParticles(particleX, particleY, particleColor, 1);
                }
            }
            
        } else {
            // Enhanced normal ball appearance
            // Subtle glow
            g.setColor(new Color(255, 255, 255, 80));
            g.fillOval((int)ballX - glowSize, (int)ballY - glowSize, BALL_SIZE + glowSize*2, BALL_SIZE + glowSize*2);
            
            // Ball with subtle gradient
            RadialGradientPaint normalBall = new RadialGradientPaint(
                (float)(ballX + BALL_SIZE/3.0), (float)(ballY + BALL_SIZE/3.0), (float)(BALL_SIZE/2.0),
                new float[]{0.0f, 0.7f, 1.0f},
                new Color[]{Color.WHITE, new Color(240, 240, 255), new Color(200, 200, 240)}
            );
            g.setPaint(normalBall);
            g.fillOval((int)ballX, (int)ballY, BALL_SIZE, BALL_SIZE);
        }
    }
    
    // Helper method to calculate realistic fire colors based on temperature/distance
    private Color calculateFireColor(float temperature, float intensity, float flicker) {
        // Simulate blackbody radiation colors (realistic fire physics)
        float alpha = intensity * flicker * 255f;
        
        if (temperature > 0.8f) {
            // Hottest: White-blue
            return new Color(255, 255, (int)(200 + 55 * temperature), (int)alpha);
        } else if (temperature > 0.6f) {
            // Hot: White-yellow
            return new Color(255, (int)(255 * temperature), (int)(100 * temperature), (int)alpha);
        } else if (temperature > 0.4f) {
            // Medium: Yellow-orange
            return new Color(255, (int)(200 * temperature), (int)(50 * temperature), (int)alpha);
        } else if (temperature > 0.2f) {
            // Cool: Orange-red
            return new Color(255, (int)(150 * temperature), 0, (int)(alpha * 0.8f));
        } else {
            // Coolest: Dark red (cooling embers)
            return new Color((int)(255 * temperature * temperature), (int)(100 * temperature), 0, (int)(alpha * 0.6f));
        }
    }
    
    // First Access page methods
    private void drawFirstAccess(Graphics2D g) {
        // Update chess animation
        updateChessAnimation();
        
        // Draw gradient background - more welcoming than pure black
        GradientPaint backgroundGradient = new GradientPaint(
            0, 0, new Color(5, 5, 15),
            0, getHeight(), new Color(15, 15, 25)
        );
        g.setPaint(backgroundGradient);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setPaint(null);
        
        // Draw animated chess pattern background
        drawChessPaddleBackground(g);
        
        // Overlay with semi-transparent layer to make text readable
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(0, 0, getWidth(), getHeight());
        
        // Main content
        drawFirstAccessContent(g);
    }
    
    private void updateCarousel() {
        long currentTime = System.currentTimeMillis();
        if (lastCarouselUpdate == 0) {
            lastCarouselUpdate = currentTime;
        }
        
        double deltaTime = (currentTime - lastCarouselUpdate) / 1000.0;
        carouselOffset += deltaTime * 50.0; // 50 pixels per second - faster movement
        
        // Continuous loop without visible restart
        int horizontalSpacing = (int)(120 * scaleX);
        int totalThemes = bluePaddleThemeNames.size() + redPaddleThemeNames.size();
        double cycleWidth = totalThemes * horizontalSpacing;
        
        if (carouselOffset > cycleWidth) {
            carouselOffset -= cycleWidth; // Seamless loop
        }
        
        lastCarouselUpdate = currentTime;
    }
    
    private void updateChessAnimation() {
        long currentTime = System.currentTimeMillis();
        if (lastChessUpdate == 0) {
            lastChessUpdate = currentTime;
        }
        
        double deltaTime = (currentTime - lastChessUpdate) / 1000.0;
        chessAnimationTime += deltaTime;
        
        lastChessUpdate = currentTime;
    }
    
    private void drawChessPaddleBackground(Graphics2D g) {
        // Paddle dimensions
        int paddleWidth = (int)(60 * Math.min(scaleX, scaleY));
        int paddleHeight = (int)(paddleWidth * 3.5); // Tall paddles like in menu
        
        // Grid spacing
        int gridSpacingX = (int)(paddleWidth * 1.5);
        int gridSpacingY = (int)(paddleHeight * 0.6);
        
        // Animation offset - slow wave motion
        double waveSpeed = 0.5; // Slow movement
        double waveAmplitude = 20.0;
        
        // Calculate grid dimensions
        int cols = (getWidth() / gridSpacingX) + 3; // Extra for smooth scrolling
        int rows = (getHeight() / gridSpacingY) + 3;
        
        // Set low opacity for background effect
        Composite originalComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
        
        // Draw chess pattern of paddles
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                // Chess pattern: alternate colors
                boolean isBlue = (row + col) % 2 == 0;
                
                // Base position
                int baseX = col * gridSpacingX - gridSpacingX;
                int baseY = row * gridSpacingY - gridSpacingY;
                
                // Add wave animation
                double wavePhase = chessAnimationTime * waveSpeed + (row * 0.3) + (col * 0.2);
                int waveX = (int)(Math.sin(wavePhase) * waveAmplitude);
                int waveY = (int)(Math.cos(wavePhase * 0.7) * waveAmplitude * 0.5);
                
                int finalX = baseX + waveX;
                int finalY = baseY + waveY;
                
                // Draw paddle with rotation based on chess position
                drawChessPaddle(g, finalX, finalY, paddleWidth, paddleHeight, isBlue, row + col);
            }
        }
        
        // Restore original composite
        g.setComposite(originalComposite);
    }
    
    private void drawChessPaddle(Graphics2D g, int x, int y, int width, int height, boolean isBlue, int index) {
        // Save original transform
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        // Move to paddle center and rotate slightly based on index
        g.translate(x + width/2, y + height/2);
        double rotation = Math.sin(chessAnimationTime * 0.3 + index * 0.5) * 15; // Gentle rotation
        g.rotate(Math.toRadians(rotation));
        
        // Use theme images from loaded paddle themes
        ArrayList<BufferedImage> themeImages = isBlue ? bluePaddleThemeImages : redPaddleThemeImages;
        
        if (!themeImages.isEmpty()) {
            // Use pseudo-random selection based on position to avoid repetitive patterns
            // Combine row and column with different primes to create better distribution
            int randomSeed = (x * 73 + y * 137 + index * 197) % 1000;
            int themeIndex = Math.abs(randomSeed) % themeImages.size();
            BufferedImage themeImg = themeImages.get(themeIndex);
            
            if (themeImg != null) {
                // Draw theme image with rounded corners
                int cornerRadius = width / 8;
                
                // Create rounded rectangle clip
                Shape oldClip = g.getClip();
                g.setClip(new RoundRectangle2D.Double(-width/2, -height/2, width, height, cornerRadius, cornerRadius));
                
                // Draw scaled theme image
                g.drawImage(themeImg, -width/2, -height/2, width, height, null);
                
                // Restore clip
                g.setClip(oldClip);
                
                // Add subtle glow effect
                g.setColor(new Color(isBlue ? 100 : 255, isBlue ? 150 : 100, isBlue ? 255 : 100, 40));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(-width/2, -height/2, width, height, cornerRadius, cornerRadius);
            } else {
                // Fallback to gradient if image fails to load
                drawChessPaddleGradient(g, width, height, isBlue);
            }
        } else {
            // Fallback to gradient if no themes loaded
            drawChessPaddleGradient(g, width, height, isBlue);
        }
        
        // Restore transform
        g.setTransform(originalTransform);
        g.setPaint(null);
    }
    
    private void drawChessPaddleGradient(Graphics2D g, int width, int height, boolean isBlue) {
        // Draw paddle with gradient fallback
        Color color1, color2;
        if (isBlue) {
            color1 = new Color(100, 150, 255);
            color2 = new Color(150, 200, 255);
        } else {
            color1 = new Color(255, 100, 100);
            color2 = new Color(255, 150, 150);
        }
        
        GradientPaint gradient = new GradientPaint(
            -width/2, -height/2, color1,
            width/2, height/2, color2
        );
        g.setPaint(gradient);
        
        // Draw rounded rectangle paddle
        int cornerRadius = width / 8;
        g.fillRoundRect(-width/2, -height/2, width, height, cornerRadius, cornerRadius);
        
        // Add subtle border glow
        g.setColor(isBlue ? new Color(100, 150, 255, 60) : new Color(255, 100, 100, 60));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(-width/2, -height/2, width, height, cornerRadius, cornerRadius);
    }
    
    private void drawThemeCarousel(Graphics2D g) {
        // Draw floating paddle themes scrolling across the background
        int paddleSize = (int)(80 * Math.min(scaleX, scaleY));
        int verticalSpacing = (int)(120 * scaleY);
        int horizontalSpacing = (int)(150 * scaleX);
        
        // Multiple rows of themes for richer background
        for (int row = 0; row < 4; row++) {
            int y = (int)(row * verticalSpacing + 50 * scaleY);
            double rowOffset = carouselOffset + (row % 2 == 0 ? 0 : -horizontalSpacing/2); // Offset alternate rows
            
            // Draw blue paddle themes
            for (int i = 0; i < bluePaddleThemeNames.size(); i++) {
                int x = (int)(rowOffset + i * horizontalSpacing - horizontalSpacing);
                if (x > -paddleSize && x < getWidth() + paddleSize) { // Only draw if visible
                    drawFloatingPaddle(g, x, y, i, true, paddleSize, 0.3 + (row * 0.1)); // Varying opacity by row
                }
            }
            
            // Draw red paddle themes offset
            for (int i = 0; i < redPaddleThemeNames.size(); i++) {
                int x = (int)(rowOffset + (i + bluePaddleThemeNames.size()) * horizontalSpacing - horizontalSpacing);
                if (x > -paddleSize && x < getWidth() + paddleSize) { // Only draw if visible
                    drawFloatingPaddle(g, x, y, i, false, paddleSize, 0.3 + (row * 0.1)); // Varying opacity by row
                }
            }
        }
    }
    
    private void drawFloatingPaddle(Graphics2D g, int x, int y, int themeIndex, boolean isBlue, int size, double opacity) {
        // Set transparency
        Composite originalComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)opacity));
        
        // Choose theme arrays
        ArrayList<String> themeNames = isBlue ? bluePaddleThemeNames : redPaddleThemeNames;
        ArrayList<BufferedImage> themeImages = isBlue ? bluePaddleThemeImages : redPaddleThemeImages;
        
        if (themeIndex < themeImages.size()) {
            BufferedImage themeImg = themeImages.get(themeIndex);
            
            if (themeImg != null) {
                // Draw theme image with rounded corners
                int cornerRadius = size / 8;
                
                // Create rounded rectangle clip
                Shape oldClip = g.getClip();
                g.setClip(new RoundRectangle2D.Double(x, y, size, size * 4, cornerRadius, cornerRadius));
                
                // Draw scaled theme image
                g.drawImage(themeImg, x, y, size, size * 4, null);
                
                // Restore clip
                g.setClip(oldClip);
                
                // Add subtle glow effect
                g.setColor(new Color(isBlue ? 100 : 255, isBlue ? 150 : 100, isBlue ? 255 : 100, (int)(30 * opacity)));
                g.setStroke(new BasicStroke(2));
                g.drawRoundRect(x, y, size, size * 4, cornerRadius, cornerRadius);
            } else {
                // Fallback: draw colored rectangle with theme name
                Color paddleColor = isBlue ? new Color(100, 150, 255, (int)(150 * opacity)) : new Color(255, 100, 100, (int)(150 * opacity));
                g.setColor(paddleColor);
                int cornerRadius = size / 8;
                g.fillRoundRect(x, y, size, size * 4, cornerRadius, cornerRadius);
                
                // Add theme name if space allows
                if (themeIndex < themeNames.size() && size > 60) {
                    g.setColor(new Color(255, 255, 255, (int)(200 * opacity)));
                    float fontSize = Math.max(10, size / 8);
                    g.setFont(secondaryFont.deriveFont(fontSize));
                    FontMetrics fm = g.getFontMetrics();
                    String name = themeNames.get(themeIndex);
                    if (name.length() > 8) name = name.substring(0, 8) + "..";
                    int textX = x + (size - fm.stringWidth(name)) / 2;
                    int textY = y + size * 2;
                    g.drawString(name, textX, textY);
                }
            }
        }
        
        // Restore original composite
        g.setComposite(originalComposite);
    }
    
    private void drawFirstAccessContent(Graphics2D g) {
        int centerX = getWidth() / 2;
        int startY = (int)(getHeight() * 0.25);
        
        // Scritta benvenuto rimossa
        // drawWelcomeTitle(g, centerX, startY); // RIMOSSO
        
        // Game logo/subtitle - ora piu grande e centrato meglio
        drawGameSubtitle(g, centerX, startY + (int)(50 * scaleY));
        
        // Language selection
        drawLanguageSelection(g, centerX, startY + (int)(200 * scaleY));
        
        // Configurazione rapida rimossa
        // drawQuickSetup(g, centerX, startY + (int)(220 * scaleY)); // RIMOSSO
        
        // Call to action
        drawStartPrompt(g, centerX, getHeight() - (int)(80 * scaleY));
    }
    
    private void drawWelcomeTitle(Graphics2D g, int centerX, int y) {
        float titleSize = (float)(48 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(Font.BOLD, titleSize));
        FontMetrics titleFm = g.getFontMetrics();
        
        String title = "BENVENUTO IN";
        int titleX = centerX - titleFm.stringWidth(title) / 2;
        
        // Glow effect
        for (int i = 1; i <= 3; i++) {
            g.setColor(new Color(100, 150, 255, 30));
            g.drawString(title, titleX + i, y + i);
        }
        
        // Main text
        g.setColor(Color.WHITE);
        g.drawString(title, titleX, y);
    }
    
    private void drawGameSubtitle(Graphics2D g, int centerX, int y) {
        // Calcola la dimensione del font per riempire quasi tutta la larghezza
        String logo = "PONG PING";
        int targetWidth = (int)(getWidth() * 0.9); // 90% della larghezza della finestra
        float logoSize = calculateFontSizeForWidth(g, logo, targetWidth, primaryFont);
        
        g.setFont(primaryFont.deriveFont(Font.BOLD, logoSize));
        FontMetrics logoFm = g.getFontMetrics();
        int logoX = centerX - logoFm.stringWidth(logo) / 2;
        
        // Logo glow
        for (int i = 1; i <= 4; i++) {
            g.setColor(new Color(0, 255, 255, 40));
            g.drawString(logo, logoX + i, y + i);
        }
        
        // Main logo
        g.setColor(new Color(0, 255, 255));
        g.drawString(logo, logoX, y);
        
        // Tagline
        float taglineSize = (float)(18 * Math.min(scaleX, scaleY));
        g.setFont(secondaryFont.deriveFont(taglineSize));
        FontMetrics taglineFm = g.getFontMetrics();
        
        String tagline = getText("FIRST_ACCESS_SUBTITLE");
        int taglineX = centerX - taglineFm.stringWidth(tagline) / 2;
        int taglineY = y + (int)(50 * scaleY);
        
        g.setColor(new Color(255, 255, 255, 180));
        g.drawString(tagline, taglineX, taglineY);
    }
    
    private float calculateFontSizeForWidth(Graphics2D g, String text, int targetWidth, Font baseFont) {
        // Inizia con una dimensione grande e riduci fino a trovare quella giusta
        float fontSize = 200f; // Inizia grande
        Font testFont = baseFont.deriveFont(Font.BOLD, fontSize);
        FontMetrics fm = g.getFontMetrics(testFont);
        
        // Riduci la dimensione fino a che non rientra nella larghezza target
        while (fm.stringWidth(text) > targetWidth && fontSize > 10f) {
            fontSize -= 5f;
            testFont = baseFont.deriveFont(Font.BOLD, fontSize);
            fm = g.getFontMetrics(testFont);
        }
        
        return fontSize;
    }
    
    // Localization methods
    private void loadLanguage(String languageCode) {
        currentLanguage.clear();
        BufferedReader reader = null;
        
        try {
            // Try to load from app context first (for jpackage apps with --app-content)
            String filename = getResourcePath("lingue/" + languageCode + ".txt");
            File languageFile = new File(filename);
            if (languageFile.exists()) {
                reader = new BufferedReader(new FileReader(languageFile));
                System.out.println("✓ Language loaded from app context: " + languageCode + ".txt");
            } else {
                // Fallback: try loading from JAR resources
                InputStream languageStream = getClass().getClassLoader().getResourceAsStream("lingue/" + languageCode + ".txt");
                if (languageStream != null) {
                    reader = new BufferedReader(new InputStreamReader(languageStream));
                    System.out.println("✓ Language loaded from JAR: " + languageCode + ".txt");
                } else {
                    System.out.println("⚠️  Language file not found in app context or JAR: " + languageCode + ".txt");
                }
            }
            
            if (reader != null) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            currentLanguage.put(parts[0].trim(), parts[1].trim());
                        }
                    }
                }
                currentLanguageCode = languageCode;
                System.out.println("Language successfully loaded: " + languageCode);
            } else {
                System.out.println("⚠️  Language file not found: " + languageCode + ".txt");
                loadDefaultLanguage();
            }
        } catch (IOException e) {
            System.out.println("Could not load language file: " + languageCode + ".txt - " + e.getMessage());
            loadDefaultLanguage();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Ignore close errors
                }
            }
        }
    }
    
    private void loadItalianLanguage() {
        currentLanguage.put("MENU_SINGLE_PLAYER", "SINGLE PLAYER");
        currentLanguage.put("MENU_TWO_PLAYERS", "TWO PLAYERS");
        currentLanguage.put("MENU_SETTINGS", "IMPOSTAZIONI");
        currentLanguage.put("MENU_EXIT", "EXIT");
        currentLanguage.put("FIRST_ACCESS_TITLE", "PONG PING");
        currentLanguage.put("FIRST_ACCESS_SUBTITLE", "Il classico gioco rivisto");
        currentLanguage.put("SETTINGS_DIFFICULTY", "DIFFICOLTA");
        currentLanguage.put("SETTINGS_PADDLE", "IMPOSTAZIONI PADDLE");
        currentLanguage.put("SETTINGS_CONTROLS", "COMANDI");
        currentLanguage.put("SETTINGS_AUDIO", "AUDIO");
        currentLanguage.put("SETTINGS_LANGUAGE", "LINGUA");
        currentLanguage.put("PADDLE_SPEED_SLOW", "LENTA");
        currentLanguage.put("PADDLE_SPEED_MEDIUM", "MEDIA");
        currentLanguage.put("PADDLE_SPEED_FAST", "VELOCE");
        currentLanguage.put("AI_DIFFICULTY_EASY", "FACILE");
        currentLanguage.put("AI_DIFFICULTY_NORMAL", "NORMALE");
        currentLanguage.put("AI_DIFFICULTY_HARD", "DIFFICILE");
        currentLanguage.put("AI_DIFFICULTY_EXPERT", "ESPERTO");
        currentLanguage.put("AI_DIFFICULTY_IMPOSSIBLE", "IMPOSSIBILE");
    }
    
    private void loadEnglishLanguage() {
        currentLanguage.put("MENU_SINGLE_PLAYER", "SINGLE PLAYER");
        currentLanguage.put("MENU_TWO_PLAYERS", "TWO PLAYERS");
        currentLanguage.put("MENU_SETTINGS", "SETTINGS");
        currentLanguage.put("MENU_EXIT", "EXIT");
        currentLanguage.put("FIRST_ACCESS_TITLE", "PONG PING");
        currentLanguage.put("FIRST_ACCESS_SUBTITLE", "The classic game revisited");
        currentLanguage.put("SETTINGS_DIFFICULTY", "DIFFICULTY");
        currentLanguage.put("SETTINGS_PADDLE", "PADDLE SETTINGS");
        currentLanguage.put("SETTINGS_CONTROLS", "CONTROLS");
        currentLanguage.put("SETTINGS_AUDIO", "AUDIO");
        currentLanguage.put("SETTINGS_LANGUAGE", "LANGUAGE");
        currentLanguage.put("PADDLE_SPEED_SLOW", "SLOW");
        currentLanguage.put("PADDLE_SPEED_MEDIUM", "MEDIUM");
        currentLanguage.put("PADDLE_SPEED_FAST", "FAST");
        currentLanguage.put("AI_DIFFICULTY_EASY", "EASY");
        currentLanguage.put("AI_DIFFICULTY_NORMAL", "NORMAL");
        currentLanguage.put("AI_DIFFICULTY_HARD", "HARD");
        currentLanguage.put("AI_DIFFICULTY_EXPERT", "EXPERT");
        currentLanguage.put("AI_DIFFICULTY_IMPOSSIBLE", "IMPOSSIBLE");
    }
    
    private void loadDefaultLanguage() {
        // Fallback to Italian if language loading fails
        loadItalianLanguage();
        currentLanguageCode = "italiano"; // Reset to default language code
    }
    
    private String getText(String key) {
        return currentLanguage.getOrDefault(key, key);
    }
    
    // Map stable setting identifiers to localized display names
    private String getSettingDisplayName(String settingId) {
        switch (settingId) {
            case "SETTING_AI_DIFFICULTY": return getText("SETTING_AI_DIFFICULTY");
            case "SETTING_PADDLE_SPEED": return getText("SETTING_PADDLE_SPEED");
            case "SETTING_BALL_SPEED": return getText("SETTING_BALL_SPEED");
            case "SETTING_P1_UP": return getText("SETTING_P1_UP");
            case "SETTING_P1_DOWN": return getText("SETTING_P1_DOWN");
            case "SETTING_P2_UP": return getText("SETTING_P2_UP");
            case "SETTING_P2_DOWN": return getText("SETTING_P2_DOWN");
            case "SETTING_MUSIC_VOLUME": return getText("SETTING_MUSIC_VOLUME");
            case "SETTING_EFFECTS_VOLUME": return getText("SETTING_EFFECTS_VOLUME");
            case "SETTING_MUSIC_ACTIVE": return getText("SETTING_MUSIC_ACTIVE");
            case "SETTING_GAME_LANGUAGE": return getText("SETTING_GAME_LANGUAGE");
            default: return settingId;
        }
    }
    
    private String getRandomMessage(String messagePrefix, int count) {
        int randomIndex = (int)(Math.random() * count) + 1;
        String key = messagePrefix + "_" + randomIndex;
        String message = getText(key);
        
        // Debug: if message equals key, translation not found
        if (message.equals(key)) {
            System.out.println("WARNING: Translation not found for key: " + key);
            // Fallback to a simple message
            return "Partita in corso...";
        }
        return message;
    }
    
    private void switchLanguage() {
        if (currentLanguageCode.equals("italiano")) {
            loadLanguage("inglese");
            currentLanguageCode = "inglese"; // Update the language code
        } else if (currentLanguageCode.equals("inglese")) {
            loadLanguage("spagnolo");
            currentLanguageCode = "spagnolo"; // Update the language code
        } else {
            loadLanguage("italiano");
            currentLanguageCode = "italiano"; // Update the language code  
        }
        // Update menu items and settings arrays
        updateLocalizedArrays();
    }
    
    private void updateLocalizedArrays() {
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
    
    private void drawQuickSetup(Graphics2D g, int centerX, int startY) {
        float settingSize = (float)(20 * Math.min(scaleX, scaleY));
        g.setFont(secondaryFont.deriveFont(settingSize));
        FontMetrics settingFm = g.getFontMetrics();
        
        // Setup title
        String setupTitle = "CONFIGURAZIONE RAPIDA";
        int setupTitleX = centerX - settingFm.stringWidth(setupTitle) / 2;
        g.setColor(new Color(255, 255, 255, 200));
        g.drawString(setupTitle, setupTitleX, startY);
        
        int settingY = startY + (int)(40 * scaleY);
        int lineSpacing = (int)(50 * scaleY);
        
        // Paddle Speed
        String paddleLabel = "Velocità Paddle: " + getPaddleSpeedText();
        String paddleControls = "← →";
        drawSettingLine(g, centerX, settingY, paddleLabel, paddleControls, settingFm);
        
        // AI Difficulty  
        settingY += lineSpacing;
        String aiLabel = "Difficoltà IA: " + getAIDifficultyText();
        String aiControls = "SPACE";
        drawSettingLine(g, centerX, settingY, aiLabel, aiControls, settingFm);
    }
    
    private void drawSettingLine(Graphics2D g, int centerX, int y, String label, String controls, FontMetrics fm) {
        // Setting label
        int labelX = centerX - fm.stringWidth(label) / 2;
        g.setColor(Color.WHITE);
        g.drawString(label, labelX, y);
        
        // Controls hint
        float controlSize = (float)(14 * Math.min(scaleX, scaleY));
        g.setFont(secondaryFont.deriveFont(controlSize));
        FontMetrics controlFm = g.getFontMetrics();
        
        int controlX = centerX - controlFm.stringWidth(controls) / 2;
        int controlY = y + (int)(20 * scaleY);
        
        g.setColor(new Color(100, 255, 100, 150));
        g.drawString(controls, controlX, controlY);
        
        // Reset font
        float settingSize = (float)(20 * Math.min(scaleX, scaleY));
        g.setFont(secondaryFont.deriveFont(settingSize));
    }
    
    private void drawLanguageSelection(Graphics2D g, int centerX, int y) {
        float titleSize = (float)(28 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(titleSize));
        FontMetrics titleFm = g.getFontMetrics();
        
        // Language selection title
        String title = "Scegli lingua / Choose language";
        int titleX = centerX - titleFm.stringWidth(title) / 2;
        g.setColor(new Color(255, 255, 255, 200));
        g.drawString(title, titleX, y);
        
        // Language options with arrows
        int optionsY = y + (int)(50 * scaleY);
        float optionSize = (float)(24 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(optionSize));
        FontMetrics optionFm = g.getFontMetrics();
        
        String leftArrow = "<";
        String rightArrow = ">";
        String currentLang;
        if (currentLanguageCode.equals("italiano")) currentLang = "ITALIANO";
        else if (currentLanguageCode.equals("inglese")) currentLang = "ENGLISH";
        else currentLang = "ESPAÑOL";
        
        String fullText = leftArrow + " " + currentLang + " " + rightArrow;
        int textX = centerX - optionFm.stringWidth(fullText) / 2;
        
        // Draw with selection color
        g.setColor(new Color(255, 165, 85)); // Orange selection color
        g.drawString(fullText, textX, optionsY);
        
        // Instructions
        int instructY = optionsY + (int)(40 * scaleY);
        float instrSize = (float)(18 * Math.min(scaleX, scaleY));
        g.setFont(secondaryFont.deriveFont(instrSize));
        FontMetrics instrFm = g.getFontMetrics();
        
        String instructions = "← → per cambiare / to change";
        int instrX = centerX - instrFm.stringWidth(instructions) / 2;
        g.setColor(new Color(255, 255, 255, 150));
        g.drawString(instructions, instrX, instructY);
    }
    
    private void drawStartPrompt(Graphics2D g, int centerX, int y) {
        float promptSize = (float)(24 * Math.min(scaleX, scaleY));
        g.setFont(primaryFont.deriveFont(Font.BOLD, promptSize));
        FontMetrics promptFm = g.getFontMetrics();
        
        String prompt;
        if (currentLanguageCode.equals("italiano")) prompt = "Premi ENTER per iniziare!";
        else if (currentLanguageCode.equals("inglese")) prompt = "Press ENTER to start!";
        else prompt = "¡Presiona ENTER para empezar!";
        int promptX = centerX - promptFm.stringWidth(prompt) / 2;
        
        // Pulsing effect
        long currentTime = System.currentTimeMillis();
        float pulse = (float)(0.7 + 0.3 * Math.sin(currentTime * 0.003));
        int alpha = (int)(255 * pulse);
        
        g.setColor(new Color(100, 255, 100, alpha));
        g.drawString(prompt, promptX, y);
    }
    
    private String getPaddleSpeedText() {
        switch (paddleSpeedSetting) {
            case 0: return "LENTA";
            case 1: return "MEDIA";
            case 2: return "VELOCE";
            default: return "MEDIA";
        }
    }
    
    private String getAIDifficultyText() {
        switch (aiDifficultySetting) {
            case 0: return "FACILE";
            case 1: return "NORMALE";
            case 2: return "DIFFICILE";
            case 3: return "ESPERTO";
            case 4: return "IMPOSSIBILE";
            default: return "NORMALE";
        }
    }
    
    private void handleFirstAccessInput(KeyEvent e) {
        int keyCode = e.getKeyCode();
        
        if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT) {
            // Switch language
            switchLanguage();
            updateLocalizedArrays();
            repaint();
        } else if (keyCode == KeyEvent.VK_ENTER) {
            // Save settings and go to menu
            saveSettingsToFile();
            currentState = GameState.MENU;
            repaint();
        }
    }
    
    private void handleGameModeSelectionInput(KeyEvent e) {
        int keyCode = e.getKeyCode();
        
        switch (keyCode) {
            case KeyEvent.VK_UP:
                selectedGameMode = (selectedGameMode - 1 + gameModes.length) % gameModes.length;
                break;
            case KeyEvent.VK_DOWN:
                selectedGameMode = (selectedGameMode + 1) % gameModes.length;
                break;
            case KeyEvent.VK_ENTER:
                // Start game with selected mode
                startGameWithMode(selectedGameMode);
                break;
            case KeyEvent.VK_ESCAPE:
                // Return to main menu
                setState(GameState.MENU);
                break;
        }
        repaint();
    }
    
    private void startGameWithMode(int gameMode) {
        currentGameMode = gameMode;
        
        // Show selection briefly before starting game
        System.out.println("Starting game with mode: " + gameModes[gameMode]);
        
        // Handle different game modes
        switch (gameMode) {
            case 0: // NORMALE
                startNewGame(true); // Single player normal mode
                break;
            default:
                startNewGame(true);
                break;
        }
    }
    
    // Game History Entry class
    private static class GameHistoryEntry {
        public String date;
        public String time;
        public String gameMode;
        public int player1Score;
        public int player2Score;
        public String winner;
        public int rallies;
        public String duration;
        public String difficulty;
        public int maxCombo; // For single player mode compatibility
        public int player1MaxCombo; // For two players mode
        public int player2MaxCombo; // For two players mode
        public String rank;
        
        // Constructor for single player mode (backward compatibility)
        public GameHistoryEntry(String date, String time, String gameMode, int p1Score, int p2Score, 
                              String winner, int rallies, String duration, String difficulty, int maxCombo, String rank) {
            this.date = date;
            this.time = time;
            this.gameMode = gameMode;
            this.player1Score = p1Score;
            this.player2Score = p2Score;
            this.winner = winner;
            this.rallies = rallies;
            this.duration = duration;
            this.difficulty = difficulty;
            this.maxCombo = maxCombo;
            this.rank = rank;
            // Set default values for two players combos
            this.player1MaxCombo = 0;
            this.player2MaxCombo = 0;
        }
        
        // Constructor for two players mode
        public GameHistoryEntry(String date, String time, String gameMode, int p1Score, int p2Score, 
                              String winner, int rallies, String duration, String difficulty, 
                              int p1MaxCombo, int p2MaxCombo, String rank) {
            this.date = date;
            this.time = time;
            this.gameMode = gameMode;
            this.player1Score = p1Score;
            this.player2Score = p2Score;
            this.winner = winner;
            this.rallies = rallies;
            this.duration = duration;
            this.difficulty = difficulty;
            this.player1MaxCombo = p1MaxCombo;
            this.player2MaxCombo = p2MaxCombo;
            this.rank = rank;
            // Set maxCombo to the higher of the two for compatibility
            this.maxCombo = Math.max(p1MaxCombo, p2MaxCombo);
        }
        
        @Override
        public String toString() {
            return date + " " + time + " | " + gameMode + " | " + player1Score + "-" + player2Score + 
                   " | Winner: " + winner + " | Rallies: " + rallies + " | Duration: " + duration + 
                   " | Difficulty: " + difficulty + " | Max Combo: " + maxCombo + " | Rank: " + rank;
        }
    }
    
    // History input handler
    private void handleHistoryInput(KeyEvent e) {
        java.util.List<GameHistoryEntry> filteredHistory = getFilteredHistory();
        
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
                // Change mode selection
                selectedHistoryMode = Math.max(0, selectedHistoryMode - 1);
                selectedHistoryCard = 0; // Reset card selection when changing mode
                historyScrollOffset = 0; // Reset scroll when changing mode
                break;
            case KeyEvent.VK_RIGHT:
                // Change mode selection
                selectedHistoryMode = Math.min(1, selectedHistoryMode + 1);
                selectedHistoryCard = 0; // Reset card selection when changing mode
                historyScrollOffset = 0; // Reset scroll when changing mode
                break;
            case KeyEvent.VK_UP:
                // Navigate up through cards con scroll automatico
                if (!filteredHistory.isEmpty()) {
                    if (selectedHistoryCard > 0) {
                        selectedHistoryCard--;
                        // Scroll up se necessario
                        if (selectedHistoryCard < historyScrollOffset) {
                            historyScrollOffset = selectedHistoryCard;
                        }
                    }
                }
                break;
            case KeyEvent.VK_DOWN:
                // Navigate down through cards con scroll automatico
                if (!filteredHistory.isEmpty()) {
                    if (selectedHistoryCard < filteredHistory.size() - 1) {
                        selectedHistoryCard++;
                        // Auto scroll down se la card selezionata non è più visibile
                        int maxVisible = calculateMaxVisibleCards();
                        if (selectedHistoryCard >= historyScrollOffset + maxVisible) {
                            historyScrollOffset = selectedHistoryCard - maxVisible + 1;
                        }
                    }
                }
                break;
            case KeyEvent.VK_ENTER:
                // Action on selected card (placeholder for future functionality)
                if (!filteredHistory.isEmpty() && selectedHistoryCard < filteredHistory.size()) {
                    GameHistoryEntry selectedEntry = filteredHistory.get(selectedHistoryCard);
                    // TODO: Add card details view or other functionality
                    System.out.println("Selected card: " + selectedEntry.winner + " vs " + selectedEntry.gameMode);
                }
                break;
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_BACK_SPACE:
                setState(GameState.MENU);
                break;
        }
        repaint();
    }
    
    // Helper method per calcolare quante card sono visibili
    private int calculateMaxVisibleCards() {
        int cardHeight = (int)(110 * scaleY);
        int cardSpacing = (int)(8 * scaleY);
        int startY = (int)(170 * scaleY);
        int availableHeight = getHeight() - startY - 50;
        return availableHeight / (cardHeight + cardSpacing);
    }
    
    private java.util.List<GameHistoryEntry> getFilteredHistory() {
        java.util.List<GameHistoryEntry> filtered = new java.util.ArrayList<>();
        for (GameHistoryEntry entry : gameHistory) {
            if (selectedHistoryMode == 0 && entry.gameMode.equals("Single Player")) {
                filtered.add(entry);
            } else if (selectedHistoryMode == 1 && entry.gameMode.equals("Two Players")) {
                filtered.add(entry);
            }
        }
        return filtered;
    }
    
    // Draw history screen
    private void drawHistory(Graphics2D g) {
        // Draw background
        drawMenuBackground(g);
        
        // Title
        if (primaryFont != null) {
            g.setFont(primaryFont.deriveFont(Font.BOLD, (float)(48 * scaleX)));
        }
        g.setColor(Color.WHITE);
        String title = getText("HISTORY_TITLE");
        FontMetrics fm = g.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        int titleX = (getWidth() - titleWidth) / 2;
        int titleY = (int)(70 * scaleY);
        g.drawString(title, titleX, titleY);
        
        // Mode selection
        if (primaryFont != null) {
            g.setFont(primaryFont.deriveFont(Font.PLAIN, (float)(22 * scaleX)));
        }
        String[] modeNames = {getText("MENU_SINGLE_PLAYER"), getText("MENU_TWO_PLAYERS")};
        int modeY = (int)(110 * scaleY);
        int totalModeWidth = 0;
        FontMetrics modeFm = g.getFontMetrics();
        
        // Calculate total width for centering
        for (int i = 0; i < modeNames.length; i++) {
            totalModeWidth += modeFm.stringWidth(modeNames[i]);
            if (i < modeNames.length - 1) totalModeWidth += (int)(60 * scaleX); // spacing
        }
        
        int startModeX = (getWidth() - totalModeWidth) / 2;
        int currentModeX = startModeX;
        
        for (int i = 0; i < modeNames.length; i++) {
            if (i == selectedHistoryMode) {
                // Selected mode - highlighted with glow effect
                g.setColor(new Color(0, 255, 255, 80)); // Cyan glow
                int padding = (int)(8 * scaleX);
                int textWidth = modeFm.stringWidth(modeNames[i]);
                g.fillRoundRect(currentModeX - padding, modeY - modeFm.getAscent() - padding, 
                               textWidth + 2 * padding, modeFm.getHeight() + 2 * padding, 10, 10);
                
                g.setColor(Color.CYAN);
                g.drawString(modeNames[i], currentModeX, modeY);
            } else {
                // Unselected mode
                g.setColor(new Color(150, 150, 150));
                g.drawString(modeNames[i], currentModeX, modeY);
            }
            
            currentModeX += modeFm.stringWidth(modeNames[i]) + (int)(60 * scaleX);
        }
        
        // Instructions removed per user request
        
        // Get filtered history based on selected mode
        java.util.List<GameHistoryEntry> filteredHistory = getFilteredHistory();
        
        // History entries as cards
        if (filteredHistory.isEmpty()) {
            g.setColor(new Color(150, 150, 150));
            String noHistory = selectedHistoryMode == 0 ? "Nessuna partita Single Player" : "Nessuna partita Two Players";
            fm = g.getFontMetrics();
            int noHistoryWidth = fm.stringWidth(noHistory);
            int noHistoryX = (getWidth() - noHistoryWidth) / 2;
            int noHistoryY = (int)(300 * scaleY);
            g.drawString(noHistory, noHistoryX, noHistoryY);
        } else {
            drawHistoryCards(g);
        }
    }
    
    private void drawHistoryCards(Graphics2D g) {
        // Get filtered history based on current mode
        java.util.List<GameHistoryEntry> filteredHistory = getFilteredHistory();
        
        if (filteredHistory.isEmpty()) {
            // Mostra messaggio cronologia vuota
            g.setColor(Color.GRAY);
            g.setFont(primaryFont.deriveFont(Font.PLAIN, (float)(20 * scaleX)));
            String emptyMessage = getText("HISTORY_EMPTY");
            FontMetrics fm = g.getFontMetrics();
            int messageWidth = fm.stringWidth(emptyMessage);
            int messageX = (getWidth() - messageWidth) / 2;
            int messageY = (int)(300 * scaleY);
            g.drawString(emptyMessage, messageX, messageY);
            return;
        }
        
        int cardWidth = (int)(getWidth() * 0.95); // Card più larghe
        int cardHeight = (int)(110 * scaleY); // Card più alte
        int cardSpacing = (int)(8 * scaleY); // Spacing ridotto
        int startY = (int)(170 * scaleY); // Adjusted for mode selection
        int startX = (getWidth() - cardWidth) / 2;
        
        // Calcola quante card possiamo mostrare
        int availableHeight = getHeight() - startY - 50;
        int maxVisible = availableHeight / (cardHeight + cardSpacing);
        
        // Mostra le partite con scroll offset
        int displayedCards = 0;
        for (int i = historyScrollOffset; i < filteredHistory.size() && displayedCards < maxVisible; i++) {
            GameHistoryEntry entry = filteredHistory.get(i);
            
            int cardY = startY + displayedCards * (cardHeight + cardSpacing);
            
            // Evidenzia card selezionata
            boolean isSelected = (i == selectedHistoryCard);
            if (isSelected) {
                drawSelectedCardHighlight(g, startX, cardY, cardWidth, cardHeight);
            }
            
            drawHistoryCard(g, entry, startX, cardY, cardWidth, cardHeight);
            
            displayedCards++;
        }
        
        // Mostra indicatori di scroll intelligenti - più grandi e visibili
        g.setFont(primaryFont.deriveFont(Font.BOLD, (float)(20 * scaleX))); // Font più grande e bold
        FontMetrics scrollFm = g.getFontMetrics();
        
        // Indicatore scroll up - freccia disegnata a mano
        if (historyScrollOffset > 0) {
            int arrowSize = (int)(12 * scaleX);
            int upX = getWidth() / 2;
            int upY = startY - (int)(25 * scaleY);
            
            // Background semi-trasparente
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRoundRect(upX - arrowSize - (int)(5 * scaleX), upY - arrowSize - (int)(5 * scaleY), 
                           arrowSize * 2 + (int)(10 * scaleX), arrowSize + (int)(10 * scaleY), 
                           (int)(8 * scaleX), (int)(8 * scaleY));
            
            // Disegna freccia su
            g.setColor(new Color(100, 200, 255));
            g.setStroke(new BasicStroke(2f));
            int[] xPoints = {upX, upX - arrowSize, upX + arrowSize};
            int[] yPoints = {upY - arrowSize, upY, upY};
            g.drawPolygon(xPoints, yPoints, 3);
        }
        
        // Indicatore scroll down - freccia disegnata a mano
        if (historyScrollOffset + maxVisible < filteredHistory.size()) {
            int arrowSize = (int)(12 * scaleX);
            int downX = getWidth() / 2;
            int downY = startY + (maxVisible * (cardHeight + cardSpacing)) + (int)(35 * scaleY);
            
            // Background semi-trasparente
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRoundRect(downX - arrowSize - (int)(5 * scaleX), downY - (int)(5 * scaleY), 
                           arrowSize * 2 + (int)(10 * scaleX), arrowSize + (int)(10 * scaleY), 
                           (int)(8 * scaleX), (int)(8 * scaleY));
            
            // Disegna freccia giù
            g.setColor(new Color(100, 200, 255));
            g.setStroke(new BasicStroke(2f));
            int[] xPoints = {downX, downX - arrowSize, downX + arrowSize};
            int[] yPoints = {downY + arrowSize, downY, downY};
            g.drawPolygon(xPoints, yPoints, 3);
        }
    }
    
    // Disegna l'evidenziazione per la card selezionata
    private void drawSelectedCardHighlight(Graphics2D g, int x, int y, int width, int height) {
        // Border bianco per card selezionata
        g.setColor(new Color(255, 255, 255, 150)); // Bianco semi-trasparente
        g.setStroke(new BasicStroke(3f)); // Border spesso
        g.drawRect(x - 2, y - 2, width + 4, height + 4);
        
        // Glow effect interno bianco
        g.setColor(new Color(255, 255, 255, 40)); // Bianco molto trasparente
        g.fillRect(x, y, width, height);
        
        // Corner highlights per un effetto più marcato
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2f));
        int cornerSize = 15;
        
        // Top-left corner
        g.drawLine(x - 2, y - 2, x - 2 + cornerSize, y - 2);
        g.drawLine(x - 2, y - 2, x - 2, y - 2 + cornerSize);
        
        // Top-right corner
        g.drawLine(x + width + 2 - cornerSize, y - 2, x + width + 2, y - 2);
        g.drawLine(x + width + 2, y - 2, x + width + 2, y - 2 + cornerSize);
        
        // Bottom-left corner
        g.drawLine(x - 2, y + height + 2 - cornerSize, x - 2, y + height + 2);
        g.drawLine(x - 2, y + height + 2, x - 2 + cornerSize, y + height + 2);
        
        // Bottom-right corner
        g.drawLine(x + width + 2, y + height + 2 - cornerSize, x + width + 2, y + height + 2);
        g.drawLine(x + width + 2 - cornerSize, y + height + 2, x + width + 2, y + height + 2);
        
        // Reset stroke
        g.setStroke(new BasicStroke(1f));
    }
    
    private void drawHistoryCard(Graphics2D g, GameHistoryEntry entry, int x, int y, int width, int height) {
        // Check game mode to use different layouts
        if (entry.gameMode.equals("Two Players")) {
            drawTwoPlayersHistoryCard(g, entry, x, y, width, height);
        } else {
            drawSinglePlayerHistoryCard(g, entry, x, y, width, height);
        }
    }
    
    // Helper method to draw "by Gava" signature
    private void drawGavaSignature(Graphics2D g) {
        g.setFont(secondaryFont != null ? secondaryFont.deriveFont(12f) : new Font("Monospaced", Font.PLAIN, 12));
        g.setColor(new Color(128, 128, 128, 180)); // Gray with transparency
        
        FontMetrics fm = g.getFontMetrics();
        String signature = "by Gava";
        int textWidth = fm.stringWidth(signature);
        
        // Position: bottom-left corner with small margins
        int x = 10;
        int y = getHeight() - 10;
        
        g.drawString(signature, x, y);
    }

    // Helper method per formattare data relativa
    private String getRelativeDate(String gameDate) {
        try {
            // Ottieni data di oggi
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
            java.util.Date today = new java.util.Date();
            String todayStr = sdf.format(today);
            
            // Calcola ieri
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(today);
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1);
            String yesterdayStr = sdf.format(cal.getTime());
            
            // Controlla se la data del gioco è oggi, ieri o più vecchia
            if (gameDate.equals(todayStr)) {
                return getText("DATE_TODAY");
            } else if (gameDate.equals(yesterdayStr)) {
                return getText("DATE_YESTERDAY");
            } else {
                return gameDate; // Mostra data completa per partite più vecchie
            }
        } catch (Exception e) {
            return gameDate; // Fallback alla data originale in caso di errore
        }
    }
    
    private void drawSinglePlayerHistoryCard(Graphics2D g, GameHistoryEntry entry, int x, int y, int width, int height) {
        // Card background with winner paddle glow colors - square corners
        boolean isLeftPaddleWinner = entry.winner.equals("PLAYER") || entry.winner.equals("PLAYER 1");
        Color paddleGlowColor = getPaddleGlowColor(isLeftPaddleWinner);
        
        // Create subtle gradient using paddle glow color
        Color darkGlow = new Color(paddleGlowColor.getRed()/4, paddleGlowColor.getGreen()/4, paddleGlowColor.getBlue()/4, 200);
        Color lighterGlow = new Color(paddleGlowColor.getRed()/3, paddleGlowColor.getGreen()/3, paddleGlowColor.getBlue()/3, 160);
        
        GradientPaint cardGradient = new GradientPaint(
            x, y, darkGlow,
            x, y + height, lighterGlow
        );
        g.setPaint(cardGradient);
        g.fillRect(x, y, width, height); // Square corners
        
        // Draw top, left, right borders BEFORE paddle (paddle will be in front of these)
        drawPixelBordersExceptBottom(g, x, y, width, height);
        
        // Draw winner paddle - positioned with bottom inside card, sides extending outside
        int paddleWidth = (int)(PADDLE_WIDTH * 2.5); // Much bigger paddle
        int paddleHeight = (int)(PADDLE_HEIGHT * 1.8); // Much taller paddle
        int paddleX = x + (int)(paddleWidth * 0.05); // Paddle leggermente più a sinistra
        int paddleY = y + (height - paddleHeight) / 2; // Centrato verticalmente sulla card
        
        drawWinnerPaddleTilted(g, entry, paddleX, paddleY, paddleWidth, paddleHeight);
        
        // Draw ONLY bottom border AFTER paddle - so it appears in front
        drawBottomPixelBorder(g, x, y, width, height);
        
        // Cover any paddle part that extends below the card with background color
        g.setColor(Color.BLACK); // Use black to match menu background
        g.fillRect(0, y + height, getWidth(), getHeight() - (y + height));
        
        // Layout che riempie completamente la card senza padding
        int padding = (int)(5 * scaleX); // Padding minimo solo per non toccare i bordi
        int infoStartX = x + padding + (int)(60 * scaleX); // Sposta info ancora più a destra
        int contentWidth = width - padding * 2;
        
        // Rank più largo per testo più grande (20%)
        int rankWidth = (int)(contentWidth * 0.20);
        int rankStartX = x + width - padding - rankWidth;
        
        // Info occupa lo spazio rimanente
        int infoWidth = rankStartX - infoStartX - (int)(10 * scaleX); // Gap adeguato
        
        // Fill the entire card area with content moved right
        drawAllGameInfo(g, entry, infoStartX, y + (int)(5 * scaleY), 
                       infoWidth, height - (int)(10 * scaleY));
        
        // Rank fills its full area
        drawRankSection(g, entry.rank, rankStartX, y, rankWidth, height);
    }
    
    private void drawTwoPlayersHistoryCard(Graphics2D g, GameHistoryEntry entry, int x, int y, int width, int height) {
        // Neutral card background for two players
        Color darkGlow = new Color(40, 40, 50, 200);
        Color lighterGlow = new Color(60, 60, 70, 160);
        
        GradientPaint cardGradient = new GradientPaint(
            x, y, darkGlow,
            x, y + height, lighterGlow
        );
        g.setPaint(cardGradient);
        g.fillRect(x, y, width, height);
        
        // Draw borders
        drawPixelBordersExceptBottom(g, x, y, width, height);
        
        // Draw combo numbers FIRST so they appear behind paddles
        boolean isPlayer1Winner = entry.winner.equals("PLAYER 1");
        boolean isPlayer2Winner = entry.winner.equals("PLAYER 2");
        drawPaddleCombos(g, entry, x, y, width, height, isPlayer1Winner, isPlayer2Winner);
        
        // Left paddle (Player 1) - positioned like single player paddle
        int paddleWidth = (int)(PADDLE_WIDTH * 2.5); // Much bigger paddle like single player
        int paddleHeight = (int)(PADDLE_HEIGHT * 1.8); // Much taller paddle like single player
        int leftPaddleX = x + (int)(paddleWidth * 0.05); // Same positioning as single player
        int leftPaddleY = y + (height - paddleHeight) / 2; // Centrato verticalmente sulla card
        
        // Right paddle (Player 2) - positioned mirrored on right side
        int rightPaddleX = x + width - paddleWidth - (int)(paddleWidth * 0.05); // Mirrored position
        int rightPaddleY = y + (height - paddleHeight) / 2;
        
        // Draw paddles with tilted style like single player - OVER the combo numbers
        drawLeftPaddleTiltedForTwoPlayers(g, entry, leftPaddleX, leftPaddleY, paddleWidth, paddleHeight);
        drawRightPaddleTiltedForTwoPlayers(g, entry, rightPaddleX, rightPaddleY, paddleWidth, paddleHeight);
        
        // Draw bottom border after paddles
        drawBottomPixelBorder(g, x, y, width, height);
        
        // Cover any paddle part that extends below the card with background color
        g.setColor(Color.BLACK); // Use black to match menu background
        g.fillRect(0, y + height, getWidth(), getHeight() - (y + height));
        
        // Draw date at top right
        drawDateInCard(g, entry, x, y, width, height);
        
        // Draw score in center
        drawCenterScore(g, entry, x, y, width, height);
    }
    
    // Helper method per disegnare la data nella parte alta centrata della card e l'ora in basso
    private void drawDateInCard(Graphics2D g, GameHistoryEntry entry, int x, int y, int width, int height) {
        // Data in alto centrata
        String relativeDate = getRelativeDate(entry.date);
        g.setColor(new Color(200, 200, 210));
        g.setFont(primaryFont.deriveFont(Font.BOLD, (float)(14 * scaleX)));
        FontMetrics dateFm = g.getFontMetrics();
        int dateWidth = dateFm.stringWidth(relativeDate);
        int dateX = x + (width - dateWidth) / 2; // Centrata orizzontalmente
        int dateY = y + (int)(10 * scaleY) + dateFm.getAscent(); // Padding dal bordo alto
        g.drawString(relativeDate, dateX, dateY);
        
        // Ora in basso centrata senza millisecondi
        g.setColor(new Color(180, 180, 190));
        g.setFont(primaryFont.deriveFont(Font.PLAIN, (float)(12 * scaleX)));
        FontMetrics timeFm = g.getFontMetrics();
        
        // Rimuovi millisecondi e secondi dall'ora (formato HH:MM:SS.mmm -> HH:MM)
        String timeWithoutMs = entry.time;
        if (timeWithoutMs.contains(".")) {
            timeWithoutMs = timeWithoutMs.substring(0, timeWithoutMs.indexOf("."));
        }
        // Rimuovi i secondi (formato HH:MM:SS -> HH:MM)
        if (timeWithoutMs.contains(":")) {
            String[] timeParts = timeWithoutMs.split(":");
            if (timeParts.length >= 2) {
                timeWithoutMs = timeParts[0] + ":" + timeParts[1];
            }
        }
        
        int timeWidth = timeFm.stringWidth(timeWithoutMs);
        int timeX = x + (width - timeWidth) / 2; // Centrata orizzontalmente
        int timeY = y + height - (int)(10 * scaleY); // Padding dal bordo basso
        g.drawString(timeWithoutMs, timeX, timeY);
    }
    
    private void drawLeftPaddleTiltedForTwoPlayers(Graphics2D g, GameHistoryEntry entry, int x, int y, int width, int height) {
        // Save original transform
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        try {
            boolean isPlayer1Winner = entry.winner.equals("PLAYER 1");
            
            // Calculate rotation center (center of paddle area)
            int centerX = x + width / 2;
            int centerY = y + height / 2;
            
            // Apply rotation (15 degrees tilt)
            g.rotate(Math.toRadians(-15), centerX, centerY);
            
            // Use left paddle (blue) theme if available
            if (selectedPaddleTheme >= 0 && selectedPaddleTheme < bluePaddleThemeImages.size()) {
                BufferedImage paddleImg = bluePaddleThemeImages.get(selectedPaddleTheme);
                
                if (paddleImg != null) {
                    // Draw themed paddle with rounded corners
                    int cornerRadius = Math.max(4, width / 6);
                    g.setClip(new java.awt.geom.RoundRectangle2D.Float(x, y, width, height, cornerRadius, cornerRadius));
                    g.drawImage(paddleImg, x, y, width, height, null);
                    g.setClip(null);
                    
                    // Add subtle border
                    g.setColor(new Color(255, 255, 255, 100));
                    g.drawRoundRect(x, y, width, height, cornerRadius, cornerRadius);
                } else {
                    // Fallback to default gradient
                    drawDefaultLeftPaddleTilted(g, entry, x, y, width, height);
                }
            } else {
                // Fallback to default gradient
                drawDefaultLeftPaddleTilted(g, entry, x, y, width, height);
            }
        } finally {
            // Restore original transform
            g.setTransform(originalTransform);
        }
    }
    
    private void drawRightPaddleTiltedForTwoPlayers(Graphics2D g, GameHistoryEntry entry, int x, int y, int width, int height) {
        // Save original transform
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        try {
            boolean isPlayer2Winner = entry.winner.equals("PLAYER 2");
            
            // Calculate rotation center (center of paddle area)
            int centerX = x + width / 2;
            int centerY = y + height / 2;
            
            // Apply rotation (15 degrees tilt in opposite direction)
            g.rotate(Math.toRadians(15), centerX, centerY);
            
            // Use right paddle (red) theme if available
            if (selectedRightPaddleTheme >= 0 && selectedRightPaddleTheme < redPaddleThemeImages.size()) {
                BufferedImage paddleImg = redPaddleThemeImages.get(selectedRightPaddleTheme);
                
                if (paddleImg != null) {
                    // Draw themed paddle with rounded corners
                    int cornerRadius = Math.max(4, width / 6);
                    g.setClip(new java.awt.geom.RoundRectangle2D.Float(x, y, width, height, cornerRadius, cornerRadius));
                    g.drawImage(paddleImg, x, y, width, height, null);
                    g.setClip(null);
                    
                    // Add subtle border
                    g.setColor(new Color(255, 255, 255, 100));
                    g.drawRoundRect(x, y, width, height, cornerRadius, cornerRadius);
                } else {
                    // Fallback to default gradient
                    drawDefaultRightPaddleTilted(g, entry, x, y, width, height);
                }
            } else {
                // Fallback to default gradient
                drawDefaultRightPaddleTilted(g, entry, x, y, width, height);
            }
        } finally {
            // Restore original transform
            g.setTransform(originalTransform);
        }
    }
    
    private void drawDefaultLeftPaddleTilted(Graphics2D g, GameHistoryEntry entry, int x, int y, int width, int height) {
        boolean isPlayer1Winner = entry.winner.equals("PLAYER 1");
        
        // Use left paddle colors (blue theme)
        Color paddleColor1 = new Color(0, 150, 255, 220); // Bright blue
        Color paddleColor2 = new Color(0, 100, 200, 180); // Darker blue
        
        // Add glow if winner
        if (isPlayer1Winner) {
            // Winner glow effect
            g.setColor(new Color(0, 255, 255, 60)); // Cyan glow
            g.fillRoundRect(x - 3, y - 3, width + 6, height + 6, 12, 12);
        }
        
        // Draw gradient paddle
        GradientPaint paddleGradient = new GradientPaint(
            x, y, paddleColor1,
            x + width, y, paddleColor2
        );
        g.setPaint(paddleGradient);
        g.fillRoundRect(x, y, width, height, 8, 8);
        
        // Border
        g.setColor(isPlayer1Winner ? Color.CYAN : Color.WHITE);
        g.drawRoundRect(x, y, width, height, 8, 8);
    }
    
    private void drawDefaultRightPaddleTilted(Graphics2D g, GameHistoryEntry entry, int x, int y, int width, int height) {
        boolean isPlayer2Winner = entry.winner.equals("PLAYER 2");
        
        // Use right paddle colors (red theme)
        Color paddleColor1 = new Color(255, 100, 100, 220); // Bright red
        Color paddleColor2 = new Color(200, 50, 50, 180);   // Darker red
        
        // Add glow if winner
        if (isPlayer2Winner) {
            // Winner glow effect
            g.setColor(new Color(255, 100, 100, 60)); // Red glow
            g.fillRoundRect(x - 3, y - 3, width + 6, height + 6, 12, 12);
        }
        
        // Draw gradient paddle
        GradientPaint paddleGradient = new GradientPaint(
            x, y, paddleColor1,
            x + width, y, paddleColor2
        );
        g.setPaint(paddleGradient);
        g.fillRoundRect(x, y, width, height, 8, 8);
        
        // Border
        g.setColor(isPlayer2Winner ? new Color(255, 150, 150) : Color.WHITE);
        g.drawRoundRect(x, y, width, height, 8, 8);
    }
    
    private void drawCenterScore(Graphics2D g, GameHistoryEntry entry, int x, int y, int width, int height) {
        // Determine winner colors
        boolean isPlayer1Winner = entry.winner.equals("PLAYER 1");
        boolean isPlayer2Winner = entry.winner.equals("PLAYER 2");
        
        // Draw large score in center - make it as big as possible
        if (primaryFont != null) {
            g.setFont(primaryFont.deriveFont(Font.BOLD, (float)(90 * scaleX))); // Large score
        }
        
        FontMetrics scoreFm = g.getFontMetrics();
        
        // Get separate elements
        String p1Score = String.valueOf(entry.player1Score);
        String dash = " - ";
        String p2Score = String.valueOf(entry.player2Score);
        
        // Calculate widths of each element
        int p1Width = scoreFm.stringWidth(p1Score);
        int dashWidth = scoreFm.stringWidth(dash);
        int p2Width = scoreFm.stringWidth(p2Score);
        int totalWidth = p1Width + dashWidth + p2Width;
        
        // Perfect vertical centering
        int textHeight = scoreFm.getAscent() - scoreFm.getDescent();
        int scoreY = y + (height / 2) + (textHeight / 2);
        
        // Fix the dash position at the center of the card for alignment across all cards
        int dashX = x + (width - dashWidth) / 2;
        
        // Position player scores around the fixed dash position
        int p1X = dashX - p1Width;
        int p2X = dashX + dashWidth;
        
        // Shadow effect for all elements
        g.setColor(new Color(0, 0, 0, 180));
        g.drawString(p1Score, p1X + 3, scoreY + 3);
        g.drawString(dash, dashX + 3, scoreY + 3);
        g.drawString(p2Score, p2X + 3, scoreY + 3);
        
        // Draw each element with proper colors
        if (isPlayer1Winner) {
            // Player 1 wins - highlight left score in cyan
            g.setColor(Color.CYAN);
            g.drawString(p1Score, p1X, scoreY);
            
            g.setColor(Color.WHITE);
            g.drawString(dash, dashX, scoreY);
            g.drawString(p2Score, p2X, scoreY);
        } else if (isPlayer2Winner) {
            // Player 2 wins - highlight right score in light red
            g.setColor(Color.WHITE);
            g.drawString(p1Score, p1X, scoreY);
            g.drawString(dash, dashX, scoreY);
            
            g.setColor(new Color(255, 150, 150));
            g.drawString(p2Score, p2X, scoreY);
        } else {
            // Draw as normal white text
            g.setColor(Color.WHITE);
            g.drawString(p1Score, p1X, scoreY);
            g.drawString(dash, dashX, scoreY);
            g.drawString(p2Score, p2X, scoreY);
        }
        
        // No additional info at bottom - keep layout clean
        // Note: Combo numbers are drawn separately before paddles
    }
    
    private void drawPaddleCombos(Graphics2D g, GameHistoryEntry entry, int x, int y, int width, int height, 
                                 boolean isPlayer1Winner, boolean isPlayer2Winner) {
        // Huge combo font
        if (primaryFont != null) {
            g.setFont(primaryFont.deriveFont(Font.BOLD, (float)(80 * scaleX))); // Huge combo numbers
        }
        FontMetrics comboFm = g.getFontMetrics();
        
        // Player 1 combo (left side, well below left paddle area)
        String player1ComboText = String.valueOf(entry.player1MaxCombo);
        int leftComboX = x + (int)(30 * scaleX); // Near left paddle
        int leftComboY = y + height / 2 + (int)(50 * scaleY); // Further below center - well under paddle
        
        // Shadow for left combo
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(player1ComboText, leftComboX + 2, leftComboY + 2);
        
        // Left combo with color
        if (isPlayer1Winner) {
            g.setColor(Color.CYAN); // Winner highlight
        } else {
            g.setColor(new Color(120, 180, 255)); // Blue for Player 1
        }
        g.drawString(player1ComboText, leftComboX, leftComboY);
        
        // "COMBO" label above Player 1 number
        if (primaryFont != null) {
            g.setFont(primaryFont.deriveFont(Font.PLAIN, (float)(14 * scaleX)));
        }
        g.setColor(new Color(150, 150, 150));
        String comboLabel = "COMBO";
        FontMetrics labelFm = g.getFontMetrics();
        int labelWidth = labelFm.stringWidth(comboLabel);
        int leftLabelX = leftComboX + (comboFm.stringWidth(player1ComboText) - labelWidth) / 2;
        int leftLabelY = leftComboY - (int)(65 * scaleY); // Above combo number
        g.drawString(comboLabel, leftLabelX, leftLabelY);
        
        // Player 2 combo (right side, well below right paddle area)
        String player2ComboText = String.valueOf(entry.player2MaxCombo);
        if (primaryFont != null) {
            g.setFont(primaryFont.deriveFont(Font.BOLD, (float)(80 * scaleX))); // Huge combo numbers
        }
        comboFm = g.getFontMetrics();
        int rightComboX = x + width - (int)(30 * scaleX) - comboFm.stringWidth(player2ComboText); // Near right paddle
        int rightComboY = y + height / 2 + (int)(50 * scaleY); // Further below center - well under paddle
        
        // Shadow for right combo
        g.setColor(new Color(0, 0, 0, 150));
        g.drawString(player2ComboText, rightComboX + 2, rightComboY + 2);
        
        // Right combo with color
        if (isPlayer2Winner) {
            g.setColor(new Color(255, 150, 150)); // Winner highlight
        } else {
            g.setColor(new Color(255, 120, 120)); // Red for Player 2
        }
        g.drawString(player2ComboText, rightComboX, rightComboY);
        
        // "COMBO" label above Player 2 number
        if (primaryFont != null) {
            g.setFont(primaryFont.deriveFont(Font.PLAIN, (float)(14 * scaleX)));
        }
        g.setColor(new Color(150, 150, 150));
        labelFm = g.getFontMetrics();
        labelWidth = labelFm.stringWidth(comboLabel);
        int rightLabelX = rightComboX + (comboFm.stringWidth(player2ComboText) - labelWidth) / 2;
        int rightLabelY = rightComboY - (int)(65 * scaleY); // Above combo number
        g.drawString(comboLabel, rightLabelX, rightLabelY);
    }
    
    private void drawWinnerPaddle(Graphics2D g, GameHistoryEntry entry, int x, int y, int width, int height) {
        // Draw paddle with left paddle theme
        int paddleWidth = (int)(width * 0.6);
        int paddleHeight = (int)(height * 0.7);
        int paddleX = x + (width - paddleWidth) / 2;
        int paddleY = y + (height - paddleHeight) / 2;
        
        // Use selected left paddle theme if available
        if (selectedPaddleTheme >= 0 && selectedPaddleTheme < bluePaddleThemeImages.size()) {
            BufferedImage paddleImg = bluePaddleThemeImages.get(selectedPaddleTheme);
            
            if (paddleImg != null) {
                // Draw themed paddle with rounded corners
                int cornerRadius = Math.max(4, paddleWidth / 6);
                g.setClip(new java.awt.geom.RoundRectangle2D.Float(paddleX, paddleY, paddleWidth, paddleHeight, cornerRadius, cornerRadius));
                g.drawImage(paddleImg, paddleX, paddleY, paddleWidth, paddleHeight, null);
                g.setClip(null);
                
                // Add subtle border
                g.setColor(new Color(255, 255, 255, 100));
                g.drawRoundRect(paddleX, paddleY, paddleWidth, paddleHeight, cornerRadius, cornerRadius);
            } else {
                // Fallback to default gradient if theme image is null
                drawDefaultWinnerPaddle(g, entry, paddleX, paddleY, paddleWidth, paddleHeight);
            }
        } else {
            // Fallback to default gradient if no theme selected
            drawDefaultWinnerPaddle(g, entry, paddleX, paddleY, paddleWidth, paddleHeight);
        }
        
        // Winner label
        g.setColor(Color.WHITE);
        g.setFont(primaryFont.deriveFont(Font.BOLD, (float)(12 * scaleX)));
        FontMetrics fm = g.getFontMetrics();
        String winner = entry.winner.replace("PLAYER", "P");
        int labelWidth = fm.stringWidth(winner);
        g.drawString(winner, x + (width - labelWidth) / 2, y + height - (int)(5 * scaleY));
    }
    
    private void drawDefaultWinnerPaddle(Graphics2D g, GameHistoryEntry entry, int paddleX, int paddleY, int paddleWidth, int paddleHeight) {
        // Determine paddle color based on winner
        Color paddleColor;
        if (entry.winner.equals("PLAYER") || entry.winner.equals("PLAYER 1")) {
            paddleColor = Color.CYAN;
        } else if (entry.winner.equals("COMPUTER") || entry.winner.equals("AI")) {
            paddleColor = Color.RED;
        } else {
            paddleColor = Color.ORANGE; // PLAYER 2
        }
        
        // Gradient paddle
        GradientPaint gradient = new GradientPaint(
            paddleX, paddleY, paddleColor.darker(),
            paddleX + paddleWidth, paddleY + paddleHeight, paddleColor.brighter()
        );
        g.setPaint(gradient);
        g.fillRoundRect(paddleX, paddleY, paddleWidth, paddleHeight, 8, 8);
        
        // Paddle border
        g.setColor(paddleColor.brighter());
        g.drawRoundRect(paddleX, paddleY, paddleWidth, paddleHeight, 8, 8);
    }
    
    private void drawWinnerPaddleTilted(Graphics2D g, GameHistoryEntry entry, int x, int y, int width, int height) {
        // Save original transform
        java.awt.geom.AffineTransform originalTransform = g.getTransform();
        
        try {
            // Calculate rotation center (center of paddle area)
            int centerX = x + width / 2;
            int centerY = y + height / 2;
            
            // Apply rotation (15 degrees tilt)
            g.rotate(Math.toRadians(-15), centerX, centerY);
            
            // Use winner paddle theme if available
            boolean isLeftPaddleWinner = entry.winner.equals("PLAYER") || entry.winner.equals("PLAYER 1");
            
            if (isLeftPaddleWinner) {
                // Use left paddle (blue) theme
                if (selectedPaddleTheme >= 0 && selectedPaddleTheme < bluePaddleThemeImages.size()) {
                    BufferedImage paddleImg = bluePaddleThemeImages.get(selectedPaddleTheme);
                    
                    if (paddleImg != null) {
                        // Draw themed paddle with rounded corners
                        int cornerRadius = Math.max(4, width / 6);
                        g.setClip(new java.awt.geom.RoundRectangle2D.Float(x, y, width, height, cornerRadius, cornerRadius));
                        g.drawImage(paddleImg, x, y, width, height, null);
                        g.setClip(null);
                        
                        // Add subtle border
                        g.setColor(new Color(255, 255, 255, 100));
                        g.drawRoundRect(x, y, width, height, cornerRadius, cornerRadius);
                    } else {
                        // Fallback to default gradient if theme image is null
                        drawDefaultWinnerPaddleTilted(g, entry, x, y, width, height);
                    }
                } else {
                    // Fallback to default gradient if no theme selected
                    drawDefaultWinnerPaddleTilted(g, entry, x, y, width, height);
                }
            } else {
                // Use right paddle (red) theme
                if (selectedRightPaddleTheme >= 0 && selectedRightPaddleTheme < redPaddleThemeImages.size()) {
                    BufferedImage paddleImg = redPaddleThemeImages.get(selectedRightPaddleTheme);
                
                    if (paddleImg != null) {
                        // Draw themed paddle with rounded corners
                        int cornerRadius = Math.max(4, width / 6);
                        g.setClip(new java.awt.geom.RoundRectangle2D.Float(x, y, width, height, cornerRadius, cornerRadius));
                        g.drawImage(paddleImg, x, y, width, height, null);
                        g.setClip(null);
                        
                        // Add subtle border
                        g.setColor(new Color(255, 255, 255, 100));
                        g.drawRoundRect(x, y, width, height, cornerRadius, cornerRadius);
                    } else {
                        // Fallback to default gradient if theme image is null
                        drawDefaultWinnerPaddleTilted(g, entry, x, y, width, height);
                    }
                } else {
                    // Fallback to default gradient if no theme selected
                    drawDefaultWinnerPaddleTilted(g, entry, x, y, width, height);
                }
            }
        } finally {
            // Always restore original transform
            g.setTransform(originalTransform);
        }
    }
    
    private void drawDefaultWinnerPaddleTilted(Graphics2D g, GameHistoryEntry entry, int x, int y, int width, int height) {
        // Determine paddle color based on winner
        Color paddleColor;
        if (entry.winner.equals("PLAYER") || entry.winner.equals("PLAYER 1")) {
            paddleColor = Color.CYAN;
        } else if (entry.winner.equals("COMPUTER") || entry.winner.equals("AI")) {
            paddleColor = Color.RED;
        } else {
            paddleColor = Color.ORANGE; // PLAYER 2
        }
        
        // Gradient paddle
        GradientPaint gradient = new GradientPaint(
            x, y, paddleColor.darker(),
            x + width, y + height, paddleColor.brighter()
        );
        g.setPaint(gradient);
        g.fillRoundRect(x, y, width, height, 8, 8);
        
        // Paddle border
        g.setColor(paddleColor.brighter());
        g.drawRoundRect(x, y, width, height, 8, 8);
    }
    
    private void drawInfoSection(Graphics2D g, String topLabel, String topValue, String bottomLabel, String bottomValue,
                                int x, int y, int width, int height) {
        g.setColor(Color.LIGHT_GRAY);
        g.setFont(primaryFont.deriveFont(Font.PLAIN, (float)(10 * scaleX)));
        FontMetrics smallFm = g.getFontMetrics();
        
        // Top info (Difficulty/Rallies/Score)
        int topLabelY = y + (int)(18 * scaleY);
        g.drawString(topLabel + ":", x + (int)(5 * scaleX), topLabelY);
        
        g.setColor(Color.WHITE);
        g.setFont(primaryFont.deriveFont(Font.BOLD, (float)(12 * scaleX)));
        FontMetrics boldFm = g.getFontMetrics();
        int topValueY = y + (int)(32 * scaleY);
        g.drawString(topValue, x + (int)(5 * scaleX), topValueY);
        
        // Bottom info (Mode/Max Combo/Duration)
        g.setColor(Color.LIGHT_GRAY);
        g.setFont(primaryFont.deriveFont(Font.PLAIN, (float)(10 * scaleX)));
        int bottomLabelY = y + (int)(50 * scaleY);
        g.drawString(bottomLabel + ":", x + (int)(5 * scaleX), bottomLabelY);
        
        g.setColor(Color.WHITE);
        g.setFont(primaryFont.deriveFont(Font.BOLD, (float)(12 * scaleX)));
        int bottomValueY = y + (int)(64 * scaleY);
        g.drawString(bottomValue, x + (int)(5 * scaleX), bottomValueY);
    }
    
    private void drawRankSection(Graphics2D g, String rank, int x, int y, int width, int height) {
        // Use getRankColor method - same as actual rank screen
        Color rankColor = getRankColor(rank);
        
        // Use rank font like in rank screen - even larger size
        g.setColor(rankColor);
        float fontSize = (float)(height * 1.0); // Font size is 100% of card height - molto più grande
        if (rankFont != null) {
            g.setFont(rankFont.deriveFont(fontSize));
        } else {
            g.setFont(primaryFont.deriveFont(Font.BOLD, fontSize)); // Fallback
        }
        
        FontMetrics fm = g.getFontMetrics();
        String displayRank = rank.equals("N/A") ? "UNRANKED" : rank;
        
        // Better alignment for ranks with modifiers (+/-)
        if (displayRank.length() > 1 && (displayRank.endsWith("+") || displayRank.endsWith("-"))) {
            // Split rank into letter and modifier for better alignment
            String rankLetter = displayRank.substring(0, 1);
            String rankModifier = displayRank.substring(1);
            
            int letterWidth = fm.stringWidth(rankLetter);
            int modifierWidth = fm.stringWidth(rankModifier);
            int totalWidth = letterWidth + modifierWidth;
            
            // Center the total text in the section
            int startX = x + (width - totalWidth) / 2;
            int textY = y + (height - fm.getHeight()) / 2 + fm.getAscent();
            
            // Draw letter and modifier separately for perfect alignment
            g.drawString(rankLetter, startX, textY);
            g.drawString(rankModifier, startX + letterWidth, textY);
        } else {
            // Single character ranks (S, A, B, C, D) - standard centering
            int rankWidth = fm.stringWidth(displayRank);
            int rankX = x + (width - rankWidth) / 2;
            int rankY = y + (height - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(displayRank, rankX, rankY);
        }
    }
    
    private void drawAllGameInfo(Graphics2D g, GameHistoryEntry entry, int x, int y, int width, int height) {
        // Layout che riempie completamente l'area disponibile
        int rowHeight = height / 2; // Due righe che occupano tutta l'altezza
        int colWidth = width / 3;   // Tre colonne che occupano tutta la larghezza
        
        // Prima riga - no margini, parte subito dall'alto
        String score = entry.player1Score + "-" + entry.player2Score;
        drawCompactInfoItem(g, getText("HISTORY_SCORE"), score, x, y);
        
        drawCompactInfoItem(g, getText("HISTORY_MODE"), entry.gameMode.replace(" Player", "P"), 
                           x + colWidth, y);
        
        drawCompactInfoItem(g, getText("HISTORY_DIFFICULTY"), entry.difficulty, 
                           x + colWidth * 2, y);
        
        // Seconda riga - usa esattamente la metà inferiore
        int secondRowY = y + rowHeight;
        drawCompactInfoItem(g, getText("HISTORY_RALLIES"), String.valueOf(entry.rallies), 
                           x, secondRowY);
                           
        drawCompactInfoItem(g, "Max Combo", String.valueOf(entry.maxCombo), 
                           x + colWidth, secondRowY);
                           
        drawCompactInfoItem(g, getText("HISTORY_DURATION"), entry.duration, 
                           x + colWidth * 2, secondRowY);
    }
    
    private void drawCompactInfoItem(Graphics2D g, String label, String value, int x, int y) {
        // Label più grande per riempire meglio
        g.setColor(new Color(170, 170, 180));
        g.setFont(primaryFont.deriveFont(Font.PLAIN, (float)(12 * scaleX)));
        g.drawString(label, x, y + (int)(15 * scaleY));
        
        // Valore molto più grande per dominare lo spazio
        g.setColor(Color.WHITE);
        g.setFont(primaryFont.deriveFont(Font.BOLD, (float)(18 * scaleX)));
        g.drawString(value, x, y + (int)(38 * scaleY)); // Riempie tutto lo spazio disponibile
    }
    
    private void drawInfoItem(Graphics2D g, String label, String value, int x, int y, int maxWidth) {
        // Label piccola sopra
        g.setColor(new Color(180, 180, 190));
        g.setFont(primaryFont.deriveFont(Font.PLAIN, (float)(10 * scaleX)));
        g.drawString(label, x, y - (int)(6 * scaleY));
        
        // Valore più grande sotto
        g.setColor(Color.WHITE);
        g.setFont(primaryFont.deriveFont(Font.BOLD, (float)(13 * scaleX)));
        g.drawString(value, x, y + (int)(8 * scaleY));
    }
    
    private void drawTopGameInfo(Graphics2D g, GameHistoryEntry entry, int x, int y, int width) {
        // Game mode on the left
        g.setColor(new Color(150, 200, 255));
        g.setFont(primaryFont.deriveFont(Font.BOLD, (float)(16 * scaleX)));
        String gameMode = entry.gameMode.replace(" Player", "P");
        g.drawString(gameMode, x, y);
        
        // Score in the center-right - most prominent
        FontMetrics fm = g.getFontMetrics();
        String score = entry.player1Score + " - " + entry.player2Score;
        g.setColor(Color.WHITE);
        g.setFont(primaryFont.deriveFont(Font.BOLD, (float)(20 * scaleX)));
        FontMetrics scoreFm = g.getFontMetrics();
        int scoreWidth = scoreFm.stringWidth(score);
        g.drawString(score, x + width - scoreWidth, y);
        
        // Winner indicator under game mode
        g.setColor(new Color(255, 220, 100));
        g.setFont(primaryFont.deriveFont(Font.PLAIN, (float)(12 * scaleX)));
        String winner = "Winner: " + entry.winner.replace("PLAYER", "P");
        g.drawString(winner, x, y + (int)(18 * scaleY));
    }
    
    private void drawCompactStats(Graphics2D g, GameHistoryEntry entry, int x, int y, int width, int height) {
        // Compact horizontal layout for stats
        int statSpacing = width / 4;
        
        // Difficulty
        drawStatPair(g, getText("HISTORY_DIFFICULTY"), entry.difficulty, x, y, height);
        
        // Rallies  
        drawStatPair(g, getText("HISTORY_RALLIES"), String.valueOf(entry.rallies), 
                    x + statSpacing, y, height);
        
        // Max Combo
        drawStatPair(g, "Max Combo", String.valueOf(entry.maxCombo), 
                    x + statSpacing * 2, y, height);
        
        // Duration
        drawStatPair(g, getText("HISTORY_DURATION"), entry.duration, 
                    x + statSpacing * 3, y, height);
    }
    
    private void drawStatPair(Graphics2D g, String label, String value, int x, int y, int height) {
        // Label
        g.setColor(new Color(170, 170, 180));
        g.setFont(primaryFont.deriveFont(Font.PLAIN, (float)(10 * scaleX)));
        g.drawString(label, x, y + (int)(12 * scaleY));
        
        // Value
        g.setColor(Color.WHITE);
        g.setFont(primaryFont.deriveFont(Font.BOLD, (float)(14 * scaleX)));
        g.drawString(value, x, y + (int)(28 * scaleY));
    }
    
    private void drawPixelBorder(Graphics2D g, int x, int y, int width, int height) {
        // Pixel art border grigio uniforme
        Color gray = new Color(160, 160, 160);
        int pixelSize = (int)(4 * scaleX); // Torna alla dimensione precedente
        
        // Bordo grigio uniforme su tutti i lati
        g.setColor(gray);
        
        // Top border
        g.fillRect(x, y, width, pixelSize);
        
        // Bottom border  
        g.fillRect(x, y + height - pixelSize, width, pixelSize);
        
        // Left border
        g.fillRect(x, y, pixelSize, height);
        
        // Right border
        g.fillRect(x + width - pixelSize, y, pixelSize, height);
    }
    
    private void drawPixelBordersExceptBottom(Graphics2D g, int x, int y, int width, int height) {
        // Pixel art border grigio uniforme - solo top, left e right (NO bottom)
        Color gray = new Color(160, 160, 160);
        int pixelSize = (int)(4 * scaleX);
        
        g.setColor(gray);
        
        // Top border
        g.fillRect(x, y, width, pixelSize);
        
        // Left border
        g.fillRect(x, y, pixelSize, height);
        
        // Right border
        g.fillRect(x + width - pixelSize, y, pixelSize, height);
    }
    
    private void drawBottomPixelBorder(Graphics2D g, int x, int y, int width, int height) {
        // Solo il bordo bottom - disegnato DOPO il paddle
        Color gray = new Color(160, 160, 160);
        int pixelSize = (int)(4 * scaleX);
        
        g.setColor(gray);
        
        // Bottom border only
        g.fillRect(x, y + height - pixelSize, width, pixelSize);
    }
    
    // Load game history from file
    private void loadGameHistory() {
        gameHistory.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(HISTORY_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty() && !line.startsWith("#")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 13) {
                        // New format with player1MaxCombo and player2MaxCombo
                        GameHistoryEntry entry = new GameHistoryEntry(
                            parts[0].trim(), parts[1].trim(), parts[2].trim(),
                            Integer.parseInt(parts[3].trim()), Integer.parseInt(parts[4].trim()),
                            parts[5].trim(), Integer.parseInt(parts[6].trim()),
                            parts[7].trim(), parts[8].trim(), 
                            Integer.parseInt(parts[11].trim()), Integer.parseInt(parts[12].trim()), parts[10].trim()
                        );
                        gameHistory.add(entry);
                    } else if (parts.length >= 11) {
                        // Format with maxCombo and rank (single player)
                        GameHistoryEntry entry = new GameHistoryEntry(
                            parts[0].trim(), parts[1].trim(), parts[2].trim(),
                            Integer.parseInt(parts[3].trim()), Integer.parseInt(parts[4].trim()),
                            parts[5].trim(), Integer.parseInt(parts[6].trim()),
                            parts[7].trim(), parts[8].trim(), Integer.parseInt(parts[9].trim()), parts[10].trim()
                        );
                        gameHistory.add(entry);
                    } else if (parts.length >= 9) {
                        // Old format compatibility - default values for missing fields
                        GameHistoryEntry entry = new GameHistoryEntry(
                            parts[0].trim(), parts[1].trim(), parts[2].trim(),
                            Integer.parseInt(parts[3].trim()), Integer.parseInt(parts[4].trim()),
                            parts[5].trim(), Integer.parseInt(parts[6].trim()),
                            parts[7].trim(), parts[8].trim(), 0, "UNRANKED"
                        );
                        gameHistory.add(entry);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Could not load game history: " + e.getMessage());
        }
    }
    
    // Save game history entry
    // Save game history entry for single player mode
    private void saveGameHistoryEntry(String gameMode, int p1Score, int p2Score, String winner, 
                                     int rallies, String duration, String difficulty, int maxCombo, String rank) {
        try {
            // Create entry
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy");
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm:ss");
            java.util.Date now = new java.util.Date();
            
            GameHistoryEntry entry = new GameHistoryEntry(
                dateFormat.format(now), timeFormat.format(now), gameMode,
                p1Score, p2Score, winner, rallies, duration, difficulty, maxCombo, rank
            );
            
            // Add to list
            gameHistory.add(0, entry); // Add to beginning for newest first
            
            // Save to file
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(HISTORY_FILE, true))) {
                pw.println(entry.date + "|" + entry.time + "|" + entry.gameMode + "|" + 
                          entry.player1Score + "|" + entry.player2Score + "|" + entry.winner + "|" + 
                          entry.rallies + "|" + entry.duration + "|" + entry.difficulty + "|" + 
                          entry.maxCombo + "|" + entry.rank);
            }
            
        } catch (Exception e) {
            System.out.println("DEBUG: Could not save game history: " + e.getMessage());
        }
    }
    
    // Save game history entry for two players mode
    private void saveGameHistoryEntryTwoPlayers(String gameMode, int p1Score, int p2Score, String winner, 
                                               int rallies, String duration, String difficulty, 
                                               int p1MaxCombo, int p2MaxCombo, String rank) {
        try {
            // Create entry
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy");
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm:ss");
            java.util.Date now = new java.util.Date();
            
            GameHistoryEntry entry = new GameHistoryEntry(
                dateFormat.format(now), timeFormat.format(now), gameMode,
                p1Score, p2Score, winner, rallies, duration, difficulty, 
                p1MaxCombo, p2MaxCombo, rank
            );
            
            // Add to list
            gameHistory.add(0, entry); // Add to beginning for newest first
            
            // Save to file with new format including both combos
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(HISTORY_FILE, true))) {
                pw.println(entry.date + "|" + entry.time + "|" + entry.gameMode + "|" + 
                          entry.player1Score + "|" + entry.player2Score + "|" + entry.winner + "|" + 
                          entry.rallies + "|" + entry.duration + "|" + entry.difficulty + "|" + 
                          entry.maxCombo + "|" + entry.rank + "|" + p1MaxCombo + "|" + p2MaxCombo);
            }
            
        } catch (Exception e) {
            System.out.println("DEBUG: Could not save game history: " + e.getMessage());
        }
    }
    
    // Save current game result to history
    private void saveGameResult() {
        try {
            // Determine game mode
            String gameMode = currentState == GameState.SINGLE_PLAYER ? "Single Player" : "Two Players";
            
            // Calculate game duration
            long durationMs = gameEndTime - gameStartTime;
            int minutes = (int) (durationMs / 60000);
            int seconds = (int) ((durationMs % 60000) / 1000);
            String duration = String.format("%d:%02d", minutes, seconds);
            
            // Get difficulty
            String difficulty;
            if (currentState == GameState.SINGLE_PLAYER) {
                String[] difficultyNames = {"Easy", "Normal", "Hard", "Expert", "Impossible"};
                difficulty = difficultyNames[Math.min(aiDifficultySetting, difficultyNames.length - 1)];
            } else {
                difficulty = "N/A";
            }
            
            // Calculate rank for this game
            String gameRank = "N/A";
            if (currentState == GameState.SINGLE_PLAYER) {
                gameRank = calculateRank();
            }
            
            // Save the entry based on game mode
            if (currentState == GameState.SINGLE_PLAYER) {
                // Single player mode - use original max combo system
                int currentMaxCombo = maxCombo;
                saveGameHistoryEntry(gameMode, score1, score2, winner, rallies, duration, difficulty, currentMaxCombo, gameRank);
            } else if (currentState == GameState.PLAYING) {
                // Two players mode - use separate combo systems
                saveGameHistoryEntryTwoPlayers(gameMode, score1, score2, winner, rallies, duration, difficulty, 
                                             player1MaxCombo, player2MaxCombo, gameRank);
            }
            
        } catch (Exception e) {
            System.out.println("DEBUG: Could not save game result: " + e.getMessage());
        }
    }

}
