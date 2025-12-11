/**
 * History Context
 * Manages game history and history file storage
 */

import { GameHistoryEntry } from '../game/GameHistoryEntry.js';

// History system variables
export let gameHistory = [];
export let selectedHistoryMode = 0; // 0 = Single Player, 1 = Two Players
export let selectedHistoryCard = 0; // Index of selected card in history
export let historyScrollOffset = 0; // Offset for automatic scrolling of cards
export const HISTORY_FILE = getHistoryFilePath();

/**
 * Get the history file path based on the browser's localStorage
 * In browser, we use localStorage instead of file system
 */
function getHistoryFilePath() {
    // For browser, we'll use localStorage key instead of file path
    return 'pongping_game_history';
}

/**
 * Load game history from localStorage
 */
export function loadGameHistory() {
    try {
        const data = localStorage.getItem(HISTORY_FILE);
        if (data) {
            const parsed = JSON.parse(data);
            gameHistory = parsed.map(entry => new GameHistoryEntry(
                entry.mode,
                entry.player1Score,
                entry.player2Score,
                entry.difficulty,
                entry.duration,
                entry.timestamp
            ));
        }
    } catch (e) {
        console.error('Error loading game history:', e);
        gameHistory = [];
    }
}

/**
 * Save game history to localStorage
 */
export function saveGameHistory() {
    try {
        localStorage.setItem(HISTORY_FILE, JSON.stringify(gameHistory));
    } catch (e) {
        console.error('Error saving game history:', e);
    }
}

// Setters for modifying exported values
export function setSelectedHistoryMode(value) {
    selectedHistoryMode = value;
}

export function setSelectedHistoryCard(value) {
    selectedHistoryCard = value;
}

export function setHistoryScrollOffset(value) {
    historyScrollOffset = value;
}

export function addGameHistory(entry) {
    gameHistory.push(entry);
    saveGameHistory();
}

export function clearGameHistory() {
    gameHistory = [];
    saveGameHistory();
}
