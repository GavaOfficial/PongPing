package advancement;

import java.io.*;
import java.util.*;

/**
 * Manages player progression system including XP, levels, achievements, and unlocks
 */
public class PlayerProgress implements Serializable {
    private static final long serialVersionUID = 1L;

    // Level and XP
    private int currentLevel = 1;
    private int currentXP = 0;
    private static final int MAX_LEVEL = 50;

    // Equipped items
    private String equippedTitle = "";
    private String equippedBadge = "";

    // Unlocks and achievements
    private Set<String> unlockedAchievements = new HashSet<>();
    private Set<String> unlockedPaddleThemes = new HashSet<>();
    private Set<String> unlockedBackgrounds = new HashSet<>();
    private Set<String> unlockedTitles = new HashSet<>();
    private Set<String> unlockedBadges = new HashSet<>();

    // Statistics - General
    private int totalGamesPlayed = 0;
    private int totalWins = 0;
    private int totalLosses = 0;
    private long totalPlayTimeMillis = 0;

    // Statistics - Records
    private int maxComboReached = 0;
    private int maxBallsDeflected = 0;
    private int highestScore = 0;

    // Statistics - Per Mode
    private int classicGamesPlayed = 0;
    private int classicWins = 0;
    private int circleGamesPlayed = 0;
    private int circleBestScore = 0;

    // Recent progress tracking
    private List<String> recentAchievements = new ArrayList<>();
    private List<Integer> recentLevels = new ArrayList<>();

    // Daily login
    private long lastLoginDate = 0;
    private int consecutiveDaysLogged = 0;
    private boolean todayLoginBonusClaimed = false;

    /**
     * Calculate XP required for a specific level (moderate growth curve)
     */
    public static int getXPRequiredForLevel(int level) {
        if (level <= 1) return 0;
        if (level > MAX_LEVEL) return Integer.MAX_VALUE;

        // Moderate growth: starts at 500, grows to ~3000 at level 50
        // Formula: baseXP + (level * growthFactor)
        int baseXP = 500;
        int growthFactor = 50;
        return baseXP + (level - 1) * growthFactor;
    }

    /**
     * Get total XP needed to reach a level from level 1
     */
    public int getTotalXPForLevel(int level) {
        int total = 0;
        for (int i = 2; i <= level; i++) {
            total += getXPRequiredForLevel(i);
        }
        return total;
    }

    /**
     * Get XP needed for next level
     */
    public int getXPForNextLevel() {
        if (currentLevel >= MAX_LEVEL) return 0;
        return getXPRequiredForLevel(currentLevel + 1);
    }

    /**
     * Get rank name based on level
     */
    public String getRankName() {
        if (currentLevel >= 41) return "MAESTRO";
        if (currentLevel >= 31) return "ESPERTO";
        if (currentLevel >= 21) return "AVANZATO";
        if (currentLevel >= 11) return "INTERMEDIO";
        return "PRINCIPIANTE";
    }

    /**
     * Add XP and handle level ups
     * @return true if leveled up
     */
    public boolean addXP(int xp) {
        if (currentLevel >= MAX_LEVEL) return false;

        currentXP += xp;
        int xpNeeded = getXPForNextLevel();

        if (currentXP >= xpNeeded) {
            currentLevel++;
            currentXP -= xpNeeded;

            // Track recent level
            recentLevels.add(0, currentLevel);
            if (recentLevels.size() > 5) {
                recentLevels.remove(recentLevels.size() - 1);
            }

            return true; // Leveled up!
        }

        return false;
    }

    /**
     * Unlock an achievement
     * @return true if newly unlocked
     */
    public boolean unlockAchievement(String achievementId) {
        if (unlockedAchievements.contains(achievementId)) {
            return false; // Already unlocked
        }

        unlockedAchievements.add(achievementId);

        // Track recent achievement
        recentAchievements.add(0, achievementId);
        if (recentAchievements.size() > 10) {
            recentAchievements.remove(recentAchievements.size() - 1);
        }

        return true;
    }

    /**
     * Check daily login and give bonus XP
     * @return bonus XP amount (0 if already claimed today)
     */
    public int checkDailyLogin() {
        long today = System.currentTimeMillis() / (1000 * 60 * 60 * 24); // Days since epoch
        long lastLogin = lastLoginDate / (1000 * 60 * 60 * 24);

        if (today == lastLogin && todayLoginBonusClaimed) {
            return 0; // Already claimed today
        }

        if (today == lastLogin + 1) {
            // Consecutive day
            consecutiveDaysLogged++;
        } else if (today > lastLogin + 1) {
            // Streak broken
            consecutiveDaysLogged = 1;
        }

        lastLoginDate = System.currentTimeMillis();
        todayLoginBonusClaimed = true;

        // Bonus XP: 50 base + 10 per consecutive day (max 200)
        int bonusXP = Math.min(50 + (consecutiveDaysLogged * 10), 200);
        return bonusXP;
    }

    // Getters and Setters
    public int getCurrentLevel() { return currentLevel; }
    public int getCurrentXP() { return currentXP; }
    public String getEquippedTitle() { return equippedTitle; }
    public void setEquippedTitle(String title) { this.equippedTitle = title; }
    public String getEquippedBadge() { return equippedBadge; }
    public void setEquippedBadge(String badge) { this.equippedBadge = badge; }

    public Set<String> getUnlockedAchievements() { return new HashSet<>(unlockedAchievements); }
    public Set<String> getUnlockedPaddleThemes() { return new HashSet<>(unlockedPaddleThemes); }
    public Set<String> getUnlockedBackgrounds() { return new HashSet<>(unlockedBackgrounds); }
    public Set<String> getUnlockedTitles() { return new HashSet<>(unlockedTitles); }
    public Set<String> getUnlockedBadges() { return new HashSet<>(unlockedBadges); }

    public void unlockPaddleTheme(String theme) { unlockedPaddleThemes.add(theme); }
    public void unlockBackground(String background) { unlockedBackgrounds.add(background); }
    public void unlockTitle(String title) { unlockedTitles.add(title); }
    public void unlockBadge(String badge) { unlockedBadges.add(badge); }

    // Statistics getters
    public int getTotalGamesPlayed() { return totalGamesPlayed; }
    public int getTotalWins() { return totalWins; }
    public int getTotalLosses() { return totalLosses; }
    public long getTotalPlayTimeMillis() { return totalPlayTimeMillis; }
    public int getMaxComboReached() { return maxComboReached; }
    public int getMaxBallsDeflected() { return maxBallsDeflected; }
    public int getHighestScore() { return highestScore; }
    public int getClassicGamesPlayed() { return classicGamesPlayed; }
    public int getClassicWins() { return classicWins; }
    public int getCircleGamesPlayed() { return circleGamesPlayed; }
    public int getCircleBestScore() { return circleBestScore; }
    public List<String> getRecentAchievements() { return new ArrayList<>(recentAchievements); }
    public List<Integer> getRecentLevels() { return new ArrayList<>(recentLevels); }
    public int getConsecutiveDaysLogged() { return consecutiveDaysLogged; }

    // Statistics setters/updaters
    public void recordGamePlayed(boolean isCircleMode, boolean won, int score) {
        totalGamesPlayed++;
        if (won) totalWins++;
        else totalLosses++;

        if (isCircleMode) {
            circleGamesPlayed++;
            if (score > circleBestScore) circleBestScore = score;
        } else {
            classicGamesPlayed++;
            if (won) classicWins++;
        }

        if (score > highestScore) highestScore = score;
    }

    public void updateMaxCombo(int combo) {
        if (combo > maxComboReached) maxComboReached = combo;
    }

    public void updateMaxBallsDeflected(int balls) {
        if (balls > maxBallsDeflected) maxBallsDeflected = balls;
    }

    public void addPlayTime(long millis) {
        totalPlayTimeMillis += millis;
    }

    /**
     * Save progress to file
     */
    public void save(String filepath) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filepath))) {
            oos.writeObject(this);
        }
    }

    /**
     * Load progress from file
     */
    public static PlayerProgress load(String filepath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filepath))) {
            return (PlayerProgress) ois.readObject();
        }
    }
}
