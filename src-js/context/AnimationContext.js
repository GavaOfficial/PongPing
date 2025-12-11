/**
 * Animation Context
 * Manages all animation states, transitions, and visual effects
 */

import { GameState } from '../game/GameState.js';
import { BOARD_HEIGHT } from './GameContext.js';

// Animation variables for smooth resizing
export let resizeTimer = null;
export let targetScaleX = 1.0;
export let targetScaleY = 1.0;
export let currentAnimatedScaleX = 1.0;
export let currentAnimatedScaleY = 1.0;
export let targetWidth = 800;
export let targetHeight = 600;
export let currentAnimatedWidth = 800;
export let currentAnimatedHeight = 600;
export const RESIZE_ANIMATION_DURATION = 200; // milliseconds
export const RESIZE_SMOOTHING_FACTOR = 0.15;

// Animation variables for difficulty effects
export let difficultyAnimationTime = 0.0;

// Visual effects (using array for particles)
export let particles = [];
export let ballTrail = 'rgba(255, 255, 255, 0.39)';

// Menu animation variables
export let menuPaddle1Y = 0;
export let menuPaddle2Y = 0;
export let menuPaddleHeight = BOARD_HEIGHT;
export let transitionProgress = 0.0;
export let isTransitioning = false;
export let transitionTarget = GameState.PLAYING;

// Home to themes transition variables
export let isHomeToThemesTransition = false;
export let homeToThemesProgress = 0.0;
export let textFadeProgress = 1.0;
export let paddleExitProgress = 0.0;
export let themesPanelProgress = 0.0;

// Home to paddle selection transition variables
export let isHomeToPaddleTransition = false;
export let homeToPaddleProgress = 0.0;
export let paddleTextFadeProgress = 1.0;
export let paddlePanelProgress = 0.0;
export let isLeftPaddleTransition = true; // true for left paddle, false for right paddle

// Home to settings transition variables
export let isHomeToSettingsTransition = false;
export let homeToSettingsProgress = 0.0;
export let paddleTranslationProgress = 0.0; // Progress of paddles moving to settings position
export let columnsTranslationProgress = 0.0; // Progress of settings columns sliding in
export let checkerboardAppearProgress = 0.0; // Progress of checkerboard appearing from bottom to top
export let checkerboardAnimationProgress = 0.0; // Progress of checkerboard animation after it's fully appeared

// Settings to home transition variables (inverse of home to settings)
export let isSettingsToHomeTransition = false;
export let settingsToHomeProgress = 0.0;
export let settingsPaddleTranslationProgress = 0.0; // Progress of paddles moving back to home position
export let settingsColumnsTranslationProgress = 0.0; // Progress of settings columns sliding out
export let settingsCheckerboardDisappearProgress = 0.0; // Progress of checkerboard disappearing from top to bottom
export let settingsCheckerboardAnimationProgress = 0.0; // Progress of checkerboard animation while disappearing

// Themes to home transition variables (inverse)
export let isThemesToHomeTransition = false;
export let themesToHomeProgress = 0.0;
export let titleExitProgress = 0.0;
export let panelExitProgress = 0.0;
export let textAppearProgress = 0.0;
export let paddleReturnProgress = 0.0; // Progress of paddles returning to menu position (used by themes transition)

// Game states
export let currentState = null; // Will be set based on first run check

// Paddle selection system with advanced smooth scrolling
export let paddleGridScrollY = 0.0; // Vertical scroll for left paddle (double for precision)
export let rightPaddleGridScrollY = 0.0; // Vertical scroll for right paddle (double for precision)
export const PADDLE_COLS = 4; // 4 fixed columns

// Advanced smooth scrolling system - based on web and game development best practices
export const SCROLL_SENSITIVITY = 2.0; // Scroll sensitivity (pixels per wheel tick)
export const SCROLL_SMOOTHING = 0.88; // Smoothing/friction factor (0.85-0.95 optimal)
export const SCROLL_ANIMATION_FPS = 60; // FPS for scroll animation
export const SCROLL_ANIMATION_INTERVAL = 1000 / SCROLL_ANIMATION_FPS; // milliseconds
export const MIN_SCROLL_VELOCITY = 0.1; // Minimum velocity before stopping

// Smooth scrolling state variables
export let targetScrollY = 0.0; // Target scroll for left paddle
export let targetRightScrollY = 0.0; // Target scroll for right paddle
export let scrollVelocityY = 0.0; // Scroll velocity left paddle
export let rightScrollVelocityY = 0.0; // Scroll velocity right paddle
export let scrollAnimationTimer = null; // Timer for smooth animation
export let isScrollingLeft = false; // Flag scroll left paddle
export let isScrollingRight = false; // Flag scroll right paddle

// Scrolling text animation phases
export let scrollingTextStarted = false;
export let scrollingTextEntryComplete = false;
export let showingDifficultyPhase = true;
export let gameInfoTransitionStarted = false;
export let difficultyHasBeenCovered = false; // Track if difficulty has been covered by scrolling text
export let scrollingTextDropProgress = 0.0; // 0.0 = off screen top, 1.0 = final position
export let gameInfoSlideProgress = 0.0; // 0.0 = off screen left, 1.0 = completely passed
export let difficultyDisplayFrames = 0;

// Setters for modifying exported values
export function setResizeTimer(value) {
    resizeTimer = value;
}

export function setTargetScale(x, y) {
    targetScaleX = x;
    targetScaleY = y;
}

export function setCurrentAnimatedScale(x, y) {
    currentAnimatedScaleX = x;
    currentAnimatedScaleY = y;
}

export function setTargetDimensions(width, height) {
    targetWidth = width;
    targetHeight = height;
}

export function setCurrentAnimatedDimensions(width, height) {
    currentAnimatedWidth = width;
    currentAnimatedHeight = height;
}

export function setDifficultyAnimationTime(value) {
    difficultyAnimationTime = value;
}

export function setParticles(value) {
    particles = value;
}

export function addParticle(particle) {
    particles.push(particle);
}

export function setBallTrail(value) {
    ballTrail = value;
}

export function setMenuPaddle1Y(value) {
    menuPaddle1Y = value;
}

export function setMenuPaddle2Y(value) {
    menuPaddle2Y = value;
}

export function setMenuPaddleHeight(value) {
    menuPaddleHeight = value;
}

export function setTransitionProgress(value) {
    transitionProgress = value;
}

export function setIsTransitioning(value) {
    isTransitioning = value;
}

export function setTransitionTarget(value) {
    transitionTarget = value;
}

export function setIsHomeToThemesTransition(value) {
    isHomeToThemesTransition = value;
}

export function setHomeToThemesProgress(value) {
    homeToThemesProgress = value;
}

export function setTextFadeProgress(value) {
    textFadeProgress = value;
}

export function setPaddleExitProgress(value) {
    paddleExitProgress = value;
}

export function setThemesPanelProgress(value) {
    themesPanelProgress = value;
}

export function setIsHomeToPaddleTransition(value) {
    isHomeToPaddleTransition = value;
}

export function setHomeToPaddleProgress(value) {
    homeToPaddleProgress = value;
}

export function setPaddleTextFadeProgress(value) {
    paddleTextFadeProgress = value;
}

export function setPaddlePanelProgress(value) {
    paddlePanelProgress = value;
}

export function setIsLeftPaddleTransition(value) {
    isLeftPaddleTransition = value;
}

export function setIsHomeToSettingsTransition(value) {
    isHomeToSettingsTransition = value;
}

export function setHomeToSettingsProgress(value) {
    homeToSettingsProgress = value;
}

export function setPaddleTranslationProgress(value) {
    paddleTranslationProgress = value;
}

export function setColumnsTranslationProgress(value) {
    columnsTranslationProgress = value;
}

export function setCheckerboardAppearProgress(value) {
    checkerboardAppearProgress = value;
}

export function setCheckerboardAnimationProgress(value) {
    checkerboardAnimationProgress = value;
}

export function setIsSettingsToHomeTransition(value) {
    isSettingsToHomeTransition = value;
}

export function setSettingsToHomeProgress(value) {
    settingsToHomeProgress = value;
}

export function setSettingsPaddleTranslationProgress(value) {
    settingsPaddleTranslationProgress = value;
}

export function setSettingsColumnsTranslationProgress(value) {
    settingsColumnsTranslationProgress = value;
}

export function setSettingsCheckerboardDisappearProgress(value) {
    settingsCheckerboardDisappearProgress = value;
}

export function setSettingsCheckerboardAnimationProgress(value) {
    settingsCheckerboardAnimationProgress = value;
}

export function setIsThemesToHomeTransition(value) {
    isThemesToHomeTransition = value;
}

export function setThemesToHomeProgress(value) {
    themesToHomeProgress = value;
}

export function setTitleExitProgress(value) {
    titleExitProgress = value;
}

export function setPanelExitProgress(value) {
    panelExitProgress = value;
}

export function setTextAppearProgress(value) {
    textAppearProgress = value;
}

export function setPaddleReturnProgress(value) {
    paddleReturnProgress = value;
}

export function setCurrentState(value) {
    currentState = value;
}

export function setPaddleGridScrollY(value) {
    paddleGridScrollY = value;
}

export function setRightPaddleGridScrollY(value) {
    rightPaddleGridScrollY = value;
}

export function setTargetScrollY(value) {
    targetScrollY = value;
}

export function setTargetRightScrollY(value) {
    targetRightScrollY = value;
}

export function setScrollVelocityY(value) {
    scrollVelocityY = value;
}

export function setRightScrollVelocityY(value) {
    rightScrollVelocityY = value;
}

export function setScrollAnimationTimer(value) {
    scrollAnimationTimer = value;
}

export function setIsScrollingLeft(value) {
    isScrollingLeft = value;
}

export function setIsScrollingRight(value) {
    isScrollingRight = value;
}

export function setScrollingTextStarted(value) {
    scrollingTextStarted = value;
}

export function setScrollingTextEntryComplete(value) {
    scrollingTextEntryComplete = value;
}

export function setShowingDifficultyPhase(value) {
    showingDifficultyPhase = value;
}

export function setGameInfoTransitionStarted(value) {
    gameInfoTransitionStarted = value;
}

export function setDifficultyHasBeenCovered(value) {
    difficultyHasBeenCovered = value;
}

export function setScrollingTextDropProgress(value) {
    scrollingTextDropProgress = value;
}

export function setGameInfoSlideProgress(value) {
    gameInfoSlideProgress = value;
}

export function setDifficultyDisplayFrames(value) {
    difficultyDisplayFrames = value;
}
