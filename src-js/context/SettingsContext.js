/**
 * Settings Context
 * Manages all game settings, menu state, and user preferences
 */

// Menu selection
export let selectedMenuItem = 0;
export let menuItems = [];
export let leftPaddleSelected = false; // Track if left paddle is selected in menu
export let rightPaddleSelected = false; // Track if right paddle is selected in menu

// Game mode selection
export let selectedGameMode = 0;
export let gameModes = ['NORMALE', 'CIRCLE'];
export let gameModeDescriptions = [
    'Modalità normale: gioco classico standard',
    'Circle Defense: difendi il cerchio centrale dalle palle!'
];

// Current game mode
export let currentGameMode = 0; // 0=Normal, 1=Circle
export let isUsingKeyboardNavigation = false; // Track if user is navigating with keyboard
export let hoveredMenuItem = -1; // Track which menu item is currently hovered (-1 = none)

// Circle Mode settings
export let circleModeInitialHealth = 100; // Initial health for circle mode (default 100 HP)
export let circleMaxCombo = 0; // Max combo reached in Circle Mode
export let circleMaxScore = 0; // Max balls deflected in Circle Mode
export let wasInCircleMode = false; // Was user in Circle Mode when they closed the game

// Audio settings
export let musicVolume = 50; // Music volume (0-100)
export let effectsVolume = 75; // Effects volume (0-100)
export let musicEnabled = true; // Music active

// Category system for settings
export let categoryNames = ['DIFFICOLTA', 'IMPOSTAZIONI PADDLE', 'COMANDI', 'AUDIO', 'LINGUA'];
export let selectedCategory = 0; // Currently selected category
export let selectedCategorySetting = 0; // Selected setting within category
export let inCategoryColumn = true; // true = left column (categories), false = right column (settings)
export let categoryAnimationProgress = [1.0, 0.0, 0.0, 0.0, 0.0]; // Animation progress for each category (1.0 = fully visible)

// Settings organized by category (use stable identifiers)
export const categorySettings = [
    ['SETTING_AI_DIFFICULTY'],
    ['SETTING_PADDLE_SPEED', 'SETTING_BALL_SPEED'],
    ['SETTING_P1_UP', 'SETTING_P1_DOWN', 'SETTING_P2_UP', 'SETTING_P2_DOWN'],
    ['SETTING_MUSIC_VOLUME', 'SETTING_EFFECTS_VOLUME', 'SETTING_MUSIC_ACTIVE'],
    ['SETTING_GAME_LANGUAGE']
];

// Stable identifiers list for mapping logic
export const settingNames = ['SETTING_PADDLE_SPEED', 'SETTING_AI_DIFFICULTY', 'SETTING_BALL_SPEED', 'SETTING_P1_UP', 'SETTING_P1_DOWN', 'SETTING_P2_UP', 'SETTING_P2_DOWN'];
export let paddleSpeedOptions = ['LENTA', 'MEDIA', 'VELOCE'];
export let aiDifficultyOptions = ['FACILE', 'NORMALE', 'DIFFICILE', 'ESPERTO', 'IMPOSSIBILE'];

// Settings background animation
export let checkerboardOffset = 0.0;
export let glowIntensity = 0.6; // Fixed intensity for stable lighting

// Clean mouse hover system
export let mouseOnBackground = false;
export let currentHoverState = HoverState.NONE;
export let hoveredCategory = -1;
export let hoveredSetting = -1;
export let isUsingKeyboardNavigationSettings = false; // Track keyboard navigation in settings

// Hover state enum for clean state management
export const HoverState = {
    NONE: 'NONE',           // No hover
    BACKGROUND: 'BACKGROUND',     // Hovering over background (clickable for themes)
    CATEGORY: 'CATEGORY',       // Hovering over a category
    SETTING: 'SETTING'         // Hovering over a setting
};

// Settings file management (using localStorage in browser)
export const SETTINGS_FILE = 'pongping_settings';
export let isFirstRun = true;

// Settings screen variables
export let selectedSetting = 0; // Current setting being modified
export let paddleSpeedSetting = 1; // 0 = Lenta, 1 = Media, 2 = Veloce
export let aiDifficultySetting = 2; // 0-4 (0 = Facile, 4 = Impossibile)
export let ballSpeedSetting = 25; // Maximum numeric speed (range 5-100)
export let player1UpKey = 'KeyW'; // Up key for player 1
export let player1DownKey = 'KeyS'; // Down key for player 1
export let player2UpKey = 'ArrowUp'; // Up key for player 2
export let player2DownKey = 'ArrowDown'; // Down key for player 2

// Right paddle combo system (player 2 - multiplayer only)
export let rightComboCount = 0;
export let rightMaxCombo = 0;

// Advanced combo visual effects (left paddle)
export let comboScale = 1.0;
export let comboPulse = 0.0;
export let comboGlow = 0.0;
export let comboColor = '#FFFF00';
export let comboMilestoneHit = false;
export let comboMilestoneTimer = 0;
export let lastComboTime = 0;

// Combo visibility control (left paddle)
export let showCombo = false;
export let comboShowTimer = 0;
export const COMBO_SHOW_DURATION = 90; // 1.5 seconds at 60 FPS

// Right paddle combo visual effects
export let rightComboScale = 1.0;
export let rightComboPulse = 0.0;
export let rightComboGlow = 0.0;
export let rightComboColor = '#FFFF00';
export let rightComboMilestoneHit = false;
export let rightComboMilestoneTimer = 0;
export let lastRightComboTime = 0;

// Right combo visibility control
export let showRightCombo = false;
export let rightComboShowTimer = 0;

// Cached glow colors to avoid concurrent access issues
export let cachedLeftGlowColor = 'rgba(100, 150, 255, 0.39)';
export let cachedRightGlowColor = 'rgba(255, 100, 100, 0.39)';

/**
 * Update localized arrays (will be called when language changes)
 * @param {Function} getText - Function to get translated text
 */
export function updateLocalizedArrays(getText) {
    // Initialize and update menu items
    if (menuItems.length === 0) {
        menuItems = new Array(5);
    }
    menuItems[0] = getText('MENU_SINGLE_PLAYER');
    menuItems[1] = getText('MENU_TWO_PLAYERS');
    menuItems[2] = getText('MENU_HISTORY');
    menuItems[3] = getText('MENU_SETTINGS');
    menuItems[4] = getText('MENU_EXIT');

    // Update category names
    categoryNames[0] = getText('SETTINGS_DIFFICULTY');
    categoryNames[1] = getText('SETTINGS_PADDLE');
    categoryNames[2] = getText('SETTINGS_CONTROLS');
    categoryNames[3] = getText('SETTINGS_AUDIO');
    categoryNames[4] = getText('SETTINGS_LANGUAGE');

    // Update paddle speed options
    paddleSpeedOptions[0] = getText('PADDLE_SPEED_SLOW');
    paddleSpeedOptions[1] = getText('PADDLE_SPEED_MEDIUM');
    paddleSpeedOptions[2] = getText('PADDLE_SPEED_FAST');

    // Update AI difficulty options
    aiDifficultyOptions[0] = getText('AI_DIFFICULTY_EASY');
    aiDifficultyOptions[1] = getText('AI_DIFFICULTY_NORMAL');
    aiDifficultyOptions[2] = getText('AI_DIFFICULTY_HARD');
    aiDifficultyOptions[3] = getText('AI_DIFFICULTY_EXPERT');
    aiDifficultyOptions[4] = getText('AI_DIFFICULTY_IMPOSSIBLE');
}

// Setters for modifying exported values
export function setSelectedMenuItem(value) {
    selectedMenuItem = value;
}

export function setMenuItems(value) {
    menuItems = value;
}

export function setLeftPaddleSelected(value) {
    leftPaddleSelected = value;
}

export function setRightPaddleSelected(value) {
    rightPaddleSelected = value;
}

export function setSelectedGameMode(value) {
    selectedGameMode = value;
}

export function setCurrentGameMode(value) {
    currentGameMode = value;
}

export function setIsUsingKeyboardNavigation(value) {
    isUsingKeyboardNavigation = value;
}

export function setHoveredMenuItem(value) {
    hoveredMenuItem = value;
}

export function setCircleModeInitialHealth(value) {
    circleModeInitialHealth = value;
}

export function setCircleMaxCombo(value) {
    circleMaxCombo = value;
}

export function setCircleMaxScore(value) {
    circleMaxScore = value;
}

export function setWasInCircleMode(value) {
    wasInCircleMode = value;
}

export function setMusicVolume(value) {
    musicVolume = value;
}

export function setEffectsVolume(value) {
    effectsVolume = value;
}

export function setMusicEnabled(value) {
    musicEnabled = value;
}

export function setSelectedCategory(value) {
    selectedCategory = value;
}

export function setSelectedCategorySetting(value) {
    selectedCategorySetting = value;
}

export function setInCategoryColumn(value) {
    inCategoryColumn = value;
}

export function setCategoryAnimationProgress(index, value) {
    categoryAnimationProgress[index] = value;
}

export function setCheckerboardOffset(value) {
    checkerboardOffset = value;
}

export function setGlowIntensity(value) {
    glowIntensity = value;
}

export function setMouseOnBackground(value) {
    mouseOnBackground = value;
}

export function setCurrentHoverState(value) {
    currentHoverState = value;
}

export function setHoveredCategory(value) {
    hoveredCategory = value;
}

export function setHoveredSetting(value) {
    hoveredSetting = value;
}

export function setIsUsingKeyboardNavigationSettings(value) {
    isUsingKeyboardNavigationSettings = value;
}

export function setIsFirstRun(value) {
    isFirstRun = value;
}

export function setSelectedSetting(value) {
    selectedSetting = value;
}

export function setPaddleSpeedSetting(value) {
    paddleSpeedSetting = value;
}

export function setAiDifficultySetting(value) {
    aiDifficultySetting = value;
}

export function setBallSpeedSetting(value) {
    ballSpeedSetting = value;
}

export function setPlayer1UpKey(value) {
    player1UpKey = value;
}

export function setPlayer1DownKey(value) {
    player1DownKey = value;
}

export function setPlayer2UpKey(value) {
    player2UpKey = value;
}

export function setPlayer2DownKey(value) {
    player2DownKey = value;
}

export function setRightComboCount(value) {
    rightComboCount = value;
}

export function setRightMaxCombo(value) {
    rightMaxCombo = value;
}

export function setComboScale(value) {
    comboScale = value;
}

export function setComboPulse(value) {
    comboPulse = value;
}

export function setComboGlow(value) {
    comboGlow = value;
}

export function setComboColor(value) {
    comboColor = value;
}

export function setComboMilestoneHit(value) {
    comboMilestoneHit = value;
}

export function setComboMilestoneTimer(value) {
    comboMilestoneTimer = value;
}

export function setLastComboTime(value) {
    lastComboTime = value;
}

export function setShowCombo(value) {
    showCombo = value;
}

export function setComboShowTimer(value) {
    comboShowTimer = value;
}

export function setRightComboScale(value) {
    rightComboScale = value;
}

export function setRightComboPulse(value) {
    rightComboPulse = value;
}

export function setRightComboGlow(value) {
    rightComboGlow = value;
}

export function setRightComboColor(value) {
    rightComboColor = value;
}

export function setRightComboMilestoneHit(value) {
    rightComboMilestoneHit = value;
}

export function setRightComboMilestoneTimer(value) {
    rightComboMilestoneTimer = value;
}

export function setLastRightComboTime(value) {
    lastRightComboTime = value;
}

export function setShowRightCombo(value) {
    showRightCombo = value;
}

export function setRightComboShowTimer(value) {
    rightComboShowTimer = value;
}

export function setCachedLeftGlowColor(value) {
    cachedLeftGlowColor = value;
}

export function setCachedRightGlowColor(value) {
    cachedRightGlowColor = value;
}
