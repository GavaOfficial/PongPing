/**
 * Achievement class
 * Represents a single achievement in the game
 */

/**
 * Achievement categories
 */
export const AchievementCategory = {
    FIRST_TIME: 'FIRST_TIME',      // Prime Volte - First achievements
    CIRCLE_MODE: 'CIRCLE_MODE',     // Circle Mode - Specific to Circle Mode
    CLASSIC_MODE: 'CLASSIC_MODE',    // Classic Mode - Specific to Classic/PongPing Mode
    MASTERY: 'MASTERY',         // Maestria - Advanced mastery achievements
    SPECIAL: 'SPECIAL'          // Speciali - Special/Secret achievements
};

/**
 * Achievement tiers with XP rewards and colors
 */
export const AchievementTier = {
    BRONZE: {
        name: 'BRONZE',
        xpReward: 50,
        color: 0xCD7F32,
        colorHex: '#CD7F32'
    },
    SILVER: {
        name: 'SILVER',
        xpReward: 100,
        color: 0xC0C0C0,
        colorHex: '#C0C0C0'
    },
    GOLD: {
        name: 'GOLD',
        xpReward: 200,
        color: 0xFFD700,
        colorHex: '#FFD700'
    },
    PLATINUM: {
        name: 'PLATINUM',
        xpReward: 500,
        color: 0xE5E4E2,
        colorHex: '#E5E4E2'
    }
};

/**
 * Achievement class
 */
export class Achievement {
    /**
     * Create an achievement
     * @param {string} id - Unique identifier
     * @param {string} nameKey - Language key for name
     * @param {string} descriptionKey - Language key for description
     * @param {string} category - Achievement category
     * @param {object} tier - Achievement tier
     */
    constructor(id, nameKey, descriptionKey, category, tier) {
        this.id = id;
        this.nameKey = nameKey;
        this.descriptionKey = descriptionKey;
        this.category = category;
        this.tier = tier;
    }

    /**
     * Get achievement ID
     * @returns {string}
     */
    getId() {
        return this.id;
    }

    /**
     * Get language key for achievement name
     * @returns {string}
     */
    getNameKey() {
        return this.nameKey;
    }

    /**
     * Get language key for achievement description
     * @returns {string}
     */
    getDescriptionKey() {
        return this.descriptionKey;
    }

    /**
     * Get achievement category
     * @returns {string}
     */
    getCategory() {
        return this.category;
    }

    /**
     * Get achievement tier
     * @returns {object}
     */
    getTier() {
        return this.tier;
    }

    /**
     * Get XP reward for this achievement
     * @returns {number}
     */
    getXPReward() {
        return this.tier.xpReward;
    }

    /**
     * Get color for this achievement
     * @returns {number}
     */
    getColor() {
        return this.tier.color;
    }

    /**
     * Get hex color for this achievement
     * @returns {string}
     */
    getColorHex() {
        return this.tier.colorHex;
    }
}
