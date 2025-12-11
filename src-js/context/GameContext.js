/**
 * Game Context
 * Central state management for game dimensions, assets, and runtime variables
 */

import { BASE_WIDTH, BASE_HEIGHT } from './DimensionalContext.js';

// Current dimensions (updated on resize)
export let BOARD_WIDTH = BASE_WIDTH;
export let BOARD_HEIGHT = BASE_HEIGHT;
export let PADDLE_WIDTH = 20;
export let PADDLE_HEIGHT = 80;
export let BALL_SIZE = 20;
export let MENU_PADDLE_WIDTH = 40;

// Functions to update dimensions
export function setBoardDimensions(width, height) {
    BOARD_WIDTH = width;
    BOARD_HEIGHT = height;
}

export function setPaddleDimensions(width, height) {
    PADDLE_WIDTH = width;
    PADDLE_HEIGHT = height;
}

export function setBallSize(size) {
    BALL_SIZE = size;
}

export function setMenuPaddleWidth(width) {
    MENU_PADDLE_WIDTH = width;
}

// Background selection variables
export let selectedBackground = 0; // Default background
export let backgroundNames = []; // Dynamic background names
export let backgroundImages = []; // Loaded background images
export let backgroundFilenames = []; // Original filenames for lazy loading
export let selectedBackgroundOption = 0; // Currently selected in background menu

// Lazy loading flags (web mode optimization)
export let allBackgroundsLoaded = false; // Track if all backgrounds are loaded
export let allPaddleThemesLoaded = false; // Track if all paddle themes are loaded

// Paddle selection variables
// Separate paddle themes for blue (left) and red (right) paddles
export let bluePaddleThemeNames = []; // Blue paddle themes
export let bluePaddleThemeImages = []; // Blue paddle images
export let bluePaddleThemeFilenames = []; // Original filenames for lazy loading
export let redPaddleThemeNames = []; // Red paddle themes
export let redPaddleThemeImages = []; // Red paddle images
export let redPaddleThemeFilenames = []; // Original filenames for lazy loading

// Legacy arrays for compatibility (will use blue themes for now)
export let paddleThemeNames = []; // Available paddle themes
export let paddleThemeImages = []; // Loaded paddle images
export let selectedPaddleTheme = 0; // Currently selected left paddle theme
export let selectedRightPaddleTheme = 0; // Currently selected right paddle theme
export let previewPaddleY = 300; // Y position of preview paddle in selection screen

// Game loop constants
export const LOGIC_FPS = 60;
export const LOGIC_TIME_STEP = 1000000000 / LOGIC_FPS; // nanoseconds (will be in milliseconds for JS)
export let gameRunning = false;
export let gameLoopThread = null;

// Scale factors
export let scaleX = 1.0;
export let scaleY = 1.0;

// Setters for modifying exported values
export function setSelectedBackground(value) {
    selectedBackground = value;
}

export function setSelectedBackgroundOption(value) {
    selectedBackgroundOption = value;
}

export function setAllBackgroundsLoaded(value) {
    allBackgroundsLoaded = value;
}

export function setAllPaddleThemesLoaded(value) {
    allPaddleThemesLoaded = value;
}

export function setSelectedPaddleTheme(value) {
    selectedPaddleTheme = value;
}

export function setSelectedRightPaddleTheme(value) {
    selectedRightPaddleTheme = value;
}

export function setPreviewPaddleY(value) {
    previewPaddleY = value;
}

export function setGameRunning(value) {
    gameRunning = value;
}

export function setGameLoopThread(value) {
    gameLoopThread = value;
}

export function setScaleFactors(x, y) {
    scaleX = x;
    scaleY = y;
}
