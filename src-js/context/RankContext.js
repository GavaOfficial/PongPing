/**
 * Rank Context
 * Manages player ranking and rank screen animations
 */

// Ranking system for single player
export let currentRank = 'NOVICE';
export let rankColor = '#FFFFFF';

export let finalRank = '';
export let showRankScreen = false;
export let rankAnimationFrame = 0;

// Rank screen animation phases
export let rankPaddleTransitionComplete = false;
export let rankTextTransitionStarted = false;
export let rankPaddleProgress = 0.0; // 0.0 = game position, 1.0 = rank position
export let rankTextProgress = 0.0; // 0.0 = off screen right, 1.0 = final position

// Setters for modifying exported values
export function setCurrentRank(value) {
    currentRank = value;
}

export function setRankColor(value) {
    rankColor = value;
}

export function setFinalRank(value) {
    finalRank = value;
}

export function setShowRankScreen(value) {
    showRankScreen = value;
}

export function setRankAnimationFrame(value) {
    rankAnimationFrame = value;
}

export function setRankPaddleTransitionComplete(value) {
    rankPaddleTransitionComplete = value;
}

export function setRankTextTransitionStarted(value) {
    rankTextTransitionStarted = value;
}

export function setRankPaddleProgress(value) {
    rankPaddleProgress = value;
}

export function setRankTextProgress(value) {
    rankTextProgress = value;
}
