/**
 * History Settings
 * Manages game history persistence
 */

import { gameHistory, HISTORY_FILE, loadGameHistory, saveGameHistory } from '../context/HistoryContext.js';
import { GameHistoryEntry } from '../game/GameHistoryEntry.js';

/**
 * HistorySettings class
 */
export class HistorySettings {
    constructor() {
    }

    /**
     * Load game history from localStorage
     */
    loadHistory() {
        loadGameHistory();
        console.log(`✓ Game history loaded (${gameHistory.length} entries)`);
    }

    /**
     * Save game history to localStorage
     */
    saveHistory() {
        saveGameHistory();
        console.log('✓ Game history saved');
    }

    /**
     * Add new game to history
     * @param {GameHistoryEntry} entry 
     */
    addGameToHistory(entry) {
        gameHistory.push(entry);
        this.saveHistory();
    }

    /**
     * Clear all game history
     */
    clearHistory() {
        gameHistory.length = 0;
        this.saveHistory();
        console.log('✓ Game history cleared');
    }

    /**
     * Get history filtered by mode
     * @param {string} mode - 'Single Player' or 'Two Players'
     * @returns {GameHistoryEntry[]}
     */
    getHistoryByMode(mode) {
        return gameHistory.filter(entry => entry.mode === mode);
    }

    /**
     * Get most recent games
     * @param {number} count - Number of recent games to get
     * @returns {GameHistoryEntry[]}
     */
    getRecentGames(count = 10) {
        return gameHistory.slice(-count).reverse();
    }
}
