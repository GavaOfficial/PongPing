/**
 * Game History Entry
 * Represents a single game in the history
 */

export class GameHistoryEntry {
    /**
     * Create a game history entry
     * @param {string} mode - Game mode ('Single Player' or 'Two Players')
     * @param {number} player1Score - Player 1 score
     * @param {number} player2Score - Player 2 score
     * @param {string} difficulty - AI difficulty (for single player)
     * @param {number} duration - Game duration in seconds
     * @param {number} timestamp - Timestamp when game was played
     */
    constructor(mode, player1Score, player2Score, difficulty, duration, timestamp) {
        this.mode = mode;
        this.player1Score = player1Score;
        this.player2Score = player2Score;
        this.difficulty = difficulty || 'N/A';
        this.duration = duration;
        this.timestamp = timestamp || Date.now();
    }

    /**
     * Get formatted date string
     * @returns {string} Formatted date
     */
    getFormattedDate() {
        const date = new Date(this.timestamp);
        return date.toLocaleDateString();
    }

    /**
     * Get formatted time string
     * @returns {string} Formatted time
     */
    getFormattedTime() {
        const date = new Date(this.timestamp);
        return date.toLocaleTimeString();
    }

    /**
     * Get formatted duration string
     * @returns {string} Duration in MM:SS format
     */
    getFormattedDuration() {
        const minutes = Math.floor(this.duration / 60);
        const seconds = this.duration % 60;
        return `${minutes}:${seconds.toString().padStart(2, '0')}`;
    }

    /**
     * Get winner name
     * @returns {string} Winner ('Player 1', 'Player 2', or 'Draw')
     */
    getWinner() {
        if (this.player1Score > this.player2Score) {
            return 'Player 1';
        } else if (this.player2Score > this.player1Score) {
            return this.mode === 'Single Player' ? 'AI' : 'Player 2';
        } else {
            return 'Draw';
        }
    }

    /**
     * Convert to JSON for storage
     * @returns {object} JSON representation
     */
    toJSON() {
        return {
            mode: this.mode,
            player1Score: this.player1Score,
            player2Score: this.player2Score,
            difficulty: this.difficulty,
            duration: this.duration,
            timestamp: this.timestamp
        };
    }

    /**
     * Create from JSON
     * @param {object} json - JSON object
     * @returns {GameHistoryEntry} New instance
     */
    static fromJSON(json) {
        return new GameHistoryEntry(
            json.mode,
            json.player1Score,
            json.player2Score,
            json.difficulty,
            json.duration,
            json.timestamp
        );
    }
}
