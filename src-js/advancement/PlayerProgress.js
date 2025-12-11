/**
 * Player Progress
 * Tracks player achievements, XP, level, and statistics
 */

/**
 * Player progress class - manages all player progression data
 */
export class PlayerProgress {
    constructor() {
        // XP and Level system
        this.xp = 0;
        this.level = 1;
        
        // Unlocked achievements (Set of achievement IDs)
        this.unlockedAchievements = new Set();
        
        // Statistics
        this.totalGamesPlayed = 0;
        this.totalPlayTimeSeconds = 0;
        this.lastLoginDate = null;
        this.loginStreak = 0;
        
        // Classic Mode stats
        this.classicGamesWon = 0;
        this.classicGamesLost = 0;
        this.classicMaxCombo = 0;
        this.classicTotalDeflections = 0;
        
        // Circle Mode stats
        this.circleGamesPlayed = 0;
        this.circleTotalBallsDeflected = 0;
        this.circleMaxCombo = 0;
        this.circleMaxScore = 0;
        this.circleTotalSurvivalTimeSeconds = 0;
        this.circleMaxSurvivalTimeSeconds = 0;
    }

    /**
     * Add XP and check for level up
     * @param {number} amount - Amount of XP to add
     * @returns {boolean} - True if leveled up
     */
    addXP(amount) {
        this.xp += amount;
        return this.checkLevelUp();
    }

    /**
     * Check if player should level up
     * @returns {boolean} - True if leveled up
     */
    checkLevelUp() {
        const requiredXP = this.getXPForNextLevel();
        if (this.xp >= requiredXP) {
            this.level++;
            return true;
        }
        return false;
    }

    /**
     * Get XP required for next level
     * @returns {number}
     */
    getXPForNextLevel() {
        // Exponential scaling: 100 * (1.5 ^ (level - 1))
        return Math.floor(100 * Math.pow(1.5, this.level - 1));
    }

    /**
     * Get XP required for current level
     * @returns {number}
     */
    getXPForCurrentLevel() {
        if (this.level === 1) return 0;
        return Math.floor(100 * Math.pow(1.5, this.level - 2));
    }

    /**
     * Get progress towards next level (0.0 - 1.0)
     * @returns {number}
     */
    getLevelProgress() {
        const currentLevelXP = this.getXPForCurrentLevel();
        const nextLevelXP = this.getXPForNextLevel();
        const progressXP = this.xp - currentLevelXP;
        const totalXPNeeded = nextLevelXP - currentLevelXP;
        return progressXP / totalXPNeeded;
    }

    /**
     * Unlock an achievement
     * @param {string} achievementId 
     * @returns {boolean} - True if newly unlocked (was not already unlocked)
     */
    unlockAchievement(achievementId) {
        if (this.unlockedAchievements.has(achievementId)) {
            return false;
        }
        this.unlockedAchievements.add(achievementId);
        return true;
    }

    /**
     * Check if achievement is unlocked
     * @param {string} achievementId 
     * @returns {boolean}
     */
    isAchievementUnlocked(achievementId) {
        return this.unlockedAchievements.has(achievementId);
    }

    /**
     * Get number of unlocked achievements
     * @returns {number}
     */
    getUnlockedAchievementCount() {
        return this.unlockedAchievements.size;
    }

    /**
     * Update login streak
     */
    updateLoginStreak() {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        
        if (!this.lastLoginDate) {
            this.loginStreak = 1;
        } else {
            const lastLogin = new Date(this.lastLoginDate);
            lastLogin.setHours(0, 0, 0, 0);
            
            const daysDiff = Math.floor((today - lastLogin) / (1000 * 60 * 60 * 24));
            
            if (daysDiff === 0) {
                // Same day, no change
            } else if (daysDiff === 1) {
                // Consecutive day, increase streak
                this.loginStreak++;
            } else {
                // Streak broken, reset
                this.loginStreak = 1;
            }
        }
        
        this.lastLoginDate = today.getTime();
    }

    /**
     * Save progress to localStorage
     */
    save() {
        const data = {
            xp: this.xp,
            level: this.level,
            unlockedAchievements: Array.from(this.unlockedAchievements),
            totalGamesPlayed: this.totalGamesPlayed,
            totalPlayTimeSeconds: this.totalPlayTimeSeconds,
            lastLoginDate: this.lastLoginDate,
            loginStreak: this.loginStreak,
            classicGamesWon: this.classicGamesWon,
            classicGamesLost: this.classicGamesLost,
            classicMaxCombo: this.classicMaxCombo,
            classicTotalDeflections: this.classicTotalDeflections,
            circleGamesPlayed: this.circleGamesPlayed,
            circleTotalBallsDeflected: this.circleTotalBallsDeflected,
            circleMaxCombo: this.circleMaxCombo,
            circleMaxScore: this.circleMaxScore,
            circleTotalSurvivalTimeSeconds: this.circleTotalSurvivalTimeSeconds,
            circleMaxSurvivalTimeSeconds: this.circleMaxSurvivalTimeSeconds
        };
        
        localStorage.setItem('pongping_player_progress', JSON.stringify(data));
    }

    /**
     * Load progress from localStorage
     * @returns {PlayerProgress}
     */
    static load() {
        const progress = new PlayerProgress();
        
        try {
            const data = localStorage.getItem('pongping_player_progress');
            if (data) {
                const parsed = JSON.parse(data);
                
                progress.xp = parsed.xp || 0;
                progress.level = parsed.level || 1;
                progress.unlockedAchievements = new Set(parsed.unlockedAchievements || []);
                progress.totalGamesPlayed = parsed.totalGamesPlayed || 0;
                progress.totalPlayTimeSeconds = parsed.totalPlayTimeSeconds || 0;
                progress.lastLoginDate = parsed.lastLoginDate;
                progress.loginStreak = parsed.loginStreak || 0;
                progress.classicGamesWon = parsed.classicGamesWon || 0;
                progress.classicGamesLost = parsed.classicGamesLost || 0;
                progress.classicMaxCombo = parsed.classicMaxCombo || 0;
                progress.classicTotalDeflections = parsed.classicTotalDeflections || 0;
                progress.circleGamesPlayed = parsed.circleGamesPlayed || 0;
                progress.circleTotalBallsDeflected = parsed.circleTotalBallsDeflected || 0;
                progress.circleMaxCombo = parsed.circleMaxCombo || 0;
                progress.circleMaxScore = parsed.circleMaxScore || 0;
                progress.circleTotalSurvivalTimeSeconds = parsed.circleTotalSurvivalTimeSeconds || 0;
                progress.circleMaxSurvivalTimeSeconds = parsed.circleMaxSurvivalTimeSeconds || 0;
            }
        } catch (e) {
            console.error('Error loading player progress:', e);
        }
        
        // Update login streak
        progress.updateLoginStreak();
        progress.save();
        
        return progress;
    }

    /**
     * Reset all progress (for testing or new game)
     */
    reset() {
        this.xp = 0;
        this.level = 1;
        this.unlockedAchievements.clear();
        this.totalGamesPlayed = 0;
        this.totalPlayTimeSeconds = 0;
        this.lastLoginDate = null;
        this.loginStreak = 0;
        this.classicGamesWon = 0;
        this.classicGamesLost = 0;
        this.classicMaxCombo = 0;
        this.classicTotalDeflections = 0;
        this.circleGamesPlayed = 0;
        this.circleTotalBallsDeflected = 0;
        this.circleMaxCombo = 0;
        this.circleMaxScore = 0;
        this.circleTotalSurvivalTimeSeconds = 0;
        this.circleMaxSurvivalTimeSeconds = 0;
        this.save();
    }
}
