package context;

import game.GameState;
import game.Particle;
import game.PongGame;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

import static context.GameContext.BOARD_HEIGHT;

public class AnimationContext {

    // Animation variables for smooth resizing
    public static Timer resizeTimer;
    public static double targetScaleX, targetScaleY;
    public static double currentAnimatedScaleX, currentAnimatedScaleY;
    public static int targetWidth, targetHeight;
    public static int currentAnimatedWidth, currentAnimatedHeight;
    public static final int RESIZE_ANIMATION_DURATION = 200; // milliseconds
    public static final double RESIZE_SMOOTHING_FACTOR = 0.15;

    // Animation variables for difficulty effects
    public static double difficultyAnimationTime = 0.0;

    // Visual effects
    public static ArrayList<Particle> particles = new ArrayList<>();
    public static Color ballTrail = new Color(255, 255, 255, 100);

    // Menu animation variables
    public static double menuPaddle1Y = 0;
    public static double menuPaddle2Y = 0;
    public static int menuPaddleHeight = BOARD_HEIGHT;
    public static double transitionProgress = 0.0;
    public static boolean isTransitioning = false;
    public static GameState transitionTarget = GameState.PLAYING;

    // Home to themes transition variables
    public static boolean isHomeToThemesTransition = false;
    public static double homeToThemesProgress = 0.0;
    public static double textFadeProgress = 1.0;
    public static double paddleExitProgress = 0.0;
    public static double themesPanelProgress = 0.0;

    // Home to paddle selection transition variables
    public static boolean isHomeToPaddleTransition = false;
    public static double homeToPaddleProgress = 0.0;
    public static double paddleTextFadeProgress = 1.0;
    public static double paddlePanelProgress = 0.0;
    public static boolean isLeftPaddleTransition = true; // true for left paddle, false for right paddle

    // Home to settings transition variables
    public static boolean isHomeToSettingsTransition = false;
    public static double homeToSettingsProgress = 0.0;
    public static double paddleTranslationProgress = 0.0; // Progress of paddles moving to settings position
    public static double columnsTranslationProgress = 0.0; // Progress of settings columns sliding in
    public static double checkerboardAppearProgress = 0.0; // Progress of checkerboard appearing from bottom to top
    public static double checkerboardAnimationProgress = 0.0; // Progress of checkerboard animation after it's fully appeared

    // Settings to home transition variables (inverse of home to settings)
    public static boolean isSettingsToHomeTransition = false;
    public static double settingsToHomeProgress = 0.0;
    public static double settingsPaddleTranslationProgress = 0.0; // Progress of paddles moving back to home position
    public static double settingsColumnsTranslationProgress = 0.0; // Progress of settings columns sliding out
    public static double settingsCheckerboardDisappearProgress = 0.0; // Progress of checkerboard disappearing from top to bottom
    public static double settingsCheckerboardAnimationProgress = 0.0; // Progress of checkerboard animation while disappearing


    // Themes to home transition variables (inverse)
    public static boolean isThemesToHomeTransition = false;
    public static double themesToHomeProgress = 0.0;
    public static double titleExitProgress = 0.0;
    public static double panelExitProgress = 0.0;
    public static double textAppearProgress = 0.0;
    public static double paddleReturnProgress = 0.0; // Progress of paddles returning to menu position (used by themes transition)

    // Game states

    public static GameState currentState; // Will be set based on first run check

    // Sistema selezione paddle con smooth scrolling avanzato
    public static double paddleGridScrollY = 0.0; // Scroll verticale per paddle sinistro (double per precisione)
    public static double rightPaddleGridScrollY = 0.0; // Scroll verticale per paddle destro (double per precisione)
    public static final int PADDLE_COLS = 4; // 4 colonne fisse

    // Advanced smooth scrolling system - basato su best practices web e game development
    public static final double SCROLL_SENSITIVITY = 2.0; // Sensibilità scroll (pixel per wheel tick)
    public static final double SCROLL_SMOOTHING = 0.88; // Fattore di smoothing/friction (0.85-0.95 ottimale)
    public static final int SCROLL_ANIMATION_FPS = 60; // FPS per animazione scroll
    public static final long SCROLL_ANIMATION_INTERVAL = 1000 / SCROLL_ANIMATION_FPS; // millisecondi
    public static final double MIN_SCROLL_VELOCITY = 0.1; // Velocità minima prima di fermarsi

    // Smooth scrolling state variables
    public static double targetScrollY = 0.0; // Target scroll per paddle sinistro
    public static double targetRightScrollY = 0.0; // Target scroll per paddle destro
    public static double scrollVelocityY = 0.0; // Velocità scroll paddle sinistro
    public static double rightScrollVelocityY = 0.0; // Velocità scroll paddle destro
    public static javax.swing.Timer scrollAnimationTimer; // Timer per animazione smooth
    public static boolean isScrollingLeft = false; // Flag scroll paddle sinistro
    public static boolean isScrollingRight = false; // Flag scroll paddle destro

    // Scrolling text animation phases
    public static boolean scrollingTextStarted = false;
    public static boolean scrollingTextEntryComplete = false;
    public static boolean showingDifficultyPhase = true;
    public static boolean gameInfoTransitionStarted = false;
    public static boolean difficultyHasBeenCovered = false; // Track if difficulty has been covered by scrolling text
    public static double scrollingTextDropProgress = 0.0; // 0.0 = fuori schermo alto, 1.0 = posizione finale
    public static double gameInfoSlideProgress = 0.0; // 0.0 = fuori schermo sinistra, 1.0 = completamente passato
    public static int difficultyDisplayFrames = 0;

}
