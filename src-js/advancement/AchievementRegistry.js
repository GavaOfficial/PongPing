/**
 * Achievement Registry
 * Central registry of all achievements in the game (77+ achievements)
 * Achievements are balanced per game mode with appropriate terminology
 */

import { Achievement, AchievementCategory, AchievementTier } from './Achievement.js';

/**
 * Registry storing all achievements
 */
const achievements = new Map();

/**
 * Register an achievement
 * @param {Achievement} achievement 
 */
function register(achievement) {
    achievements.set(achievement.getId(), achievement);
}

// ===== FIRST TIME (Prime Volte) - General first achievements =====
register(new Achievement('first_game', 'ACH_FIRST_GAME', 'ACH_FIRST_GAME_DESC',
    AchievementCategory.FIRST_TIME, AchievementTier.BRONZE));

register(new Achievement('first_win_classic', 'ACH_FIRST_WIN_CLASSIC', 'ACH_FIRST_WIN_CLASSIC_DESC',
    AchievementCategory.FIRST_TIME, AchievementTier.BRONZE));

register(new Achievement('first_circle_game', 'ACH_FIRST_CIRCLE', 'ACH_FIRST_CIRCLE_DESC',
    AchievementCategory.FIRST_TIME, AchievementTier.BRONZE));

register(new Achievement('games_10', 'ACH_10_GAMES', 'ACH_10_GAMES_DESC',
    AchievementCategory.FIRST_TIME, AchievementTier.BRONZE));

register(new Achievement('games_50', 'ACH_50_GAMES', 'ACH_50_GAMES_DESC',
    AchievementCategory.FIRST_TIME, AchievementTier.SILVER));

register(new Achievement('games_100', 'ACH_100_GAMES', 'ACH_100_GAMES_DESC',
    AchievementCategory.FIRST_TIME, AchievementTier.GOLD));

register(new Achievement('games_250', 'ACH_250_GAMES', 'ACH_250_GAMES_DESC',
    AchievementCategory.FIRST_TIME, AchievementTier.GOLD));

register(new Achievement('games_500', 'ACH_500_GAMES', 'ACH_500_GAMES_DESC',
    AchievementCategory.FIRST_TIME, AchievementTier.PLATINUM));

register(new Achievement('playtime_30min', 'ACH_PLAYTIME_30MIN', 'ACH_PLAYTIME_30MIN_DESC',
    AchievementCategory.FIRST_TIME, AchievementTier.BRONZE));

register(new Achievement('playtime_1hour', 'ACH_PLAYTIME_1HOUR', 'ACH_PLAYTIME_1HOUR_DESC',
    AchievementCategory.FIRST_TIME, AchievementTier.SILVER));

register(new Achievement('playtime_5hours', 'ACH_PLAYTIME_5HOURS', 'ACH_PLAYTIME_5HOURS_DESC',
    AchievementCategory.FIRST_TIME, AchievementTier.GOLD));

register(new Achievement('playtime_10hours', 'ACH_PLAYTIME_10HOURS', 'ACH_PLAYTIME_10HOURS_DESC',
    AchievementCategory.FIRST_TIME, AchievementTier.PLATINUM));

register(new Achievement('login_streak_3', 'ACH_LOGIN_3DAYS', 'ACH_LOGIN_3DAYS_DESC',
    AchievementCategory.FIRST_TIME, AchievementTier.BRONZE));

register(new Achievement('login_streak_7', 'ACH_LOGIN_7DAYS', 'ACH_LOGIN_7DAYS_DESC',
    AchievementCategory.FIRST_TIME, AchievementTier.SILVER));

register(new Achievement('login_streak_30', 'ACH_LOGIN_30DAYS', 'ACH_LOGIN_30DAYS_DESC',
    AchievementCategory.FIRST_TIME, AchievementTier.PLATINUM));

// ===== CIRCLE MODE - Specific achievements for Circle Mode =====

// Balls Protected (Palle Protette)
register(new Achievement('circle_protect_10', 'ACH_CIRCLE_PROTECT_10', 'ACH_CIRCLE_PROTECT_10_DESC',
    AchievementCategory.CIRCLE_MODE, AchievementTier.BRONZE));

register(new Achievement('circle_protect_30', 'ACH_CIRCLE_PROTECT_30', 'ACH_CIRCLE_PROTECT_30_DESC',
    AchievementCategory.CIRCLE_MODE, AchievementTier.BRONZE));

register(new Achievement('circle_protect_50', 'ACH_CIRCLE_PROTECT_50', 'ACH_CIRCLE_PROTECT_50_DESC',
    AchievementCategory.CIRCLE_MODE, AchievementTier.SILVER));

register(new Achievement('circle_protect_100', 'ACH_CIRCLE_PROTECT_100', 'ACH_CIRCLE_PROTECT_100_DESC',
    AchievementCategory.CIRCLE_MODE, AchievementTier.SILVER));

register(new Achievement('circle_protect_250', 'ACH_CIRCLE_PROTECT_250', 'ACH_CIRCLE_PROTECT_250_DESC',
    AchievementCategory.CIRCLE_MODE, AchievementTier.GOLD));

register(new Achievement('circle_protect_500', 'ACH_CIRCLE_PROTECT_500', 'ACH_CIRCLE_PROTECT_500_DESC',
    AchievementCategory.CIRCLE_MODE, AchievementTier.GOLD));

register(new Achievement('circle_protect_1000', 'ACH_CIRCLE_PROTECT_1000', 'ACH_CIRCLE_PROTECT_1000_DESC',
    AchievementCategory.CIRCLE_MODE, AchievementTier.PLATINUM));

register(new Achievement('circle_protect_2500', 'ACH_CIRCLE_PROTECT_2500', 'ACH_CIRCLE_PROTECT_2500_DESC',
    AchievementCategory.CIRCLE_MODE, AchievementTier.PLATINUM));

// Circle Mode Combos
register(new Achievement('circle_combo_25', 'ACH_CIRCLE_COMBO_25', 'ACH_CIRCLE_COMBO_25_DESC',
    AchievementCategory.CIRCLE_MODE, AchievementTier.BRONZE));

register(new Achievement('circle_combo_50', 'ACH_CIRCLE_COMBO_50', 'ACH_CIRCLE_COMBO_50_DESC',
    AchievementCategory.CIRCLE_MODE, AchievementTier.BRONZE));

register(new Achievement('circle_combo_100', 'ACH_CIRCLE_COMBO_100', 'ACH_CIRCLE_COMBO_100_DESC',
    AchievementCategory.CIRCLE_MODE, AchievementTier.SILVER));

register(new Achievement('circle_combo_150', 'ACH_CIRCLE_COMBO_150', 'ACH_CIRCLE_COMBO_150_DESC',
    AchievementCategory.CIRCLE_MODE, AchievementTier.SILVER));

register(new Achievement('circle_combo_200', 'ACH_CIRCLE_COMBO_200', 'ACH_CIRCLE_COMBO_200_DESC',
    AchievementCategory.CIRCLE_MODE, AchievementTier.GOLD));

// TODO: Add remaining 60+ achievements
// - Circle Mode: survival time, score, combos
// - Classic Mode: wins, combos, AI difficulty, streaks
// - Mastery: advanced challenges
// - Special: secret achievements

/**
 * Get achievement by ID
 * @param {string} id 
 * @returns {Achievement}
 */
export function getAchievement(id) {
    return achievements.get(id);
}

/**
 * Get all achievements
 * @returns {Achievement[]}
 */
export function getAllAchievements() {
    return Array.from(achievements.values());
}

/**
 * Get achievements by category
 * @param {string} category 
 * @returns {Achievement[]}
 */
export function getAchievementsByCategory(category) {
    return getAllAchievements().filter(ach => ach.getCategory() === category);
}

/**
 * Get total number of achievements
 * @returns {number}
 */
export function getTotalAchievements() {
    return achievements.size;
}

/**
 * Check if achievement exists
 * @param {string} id 
 * @returns {boolean}
 */
export function hasAchievement(id) {
    return achievements.has(id);
}
