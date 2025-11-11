package advancement;

import java.util.*;

/**
 * Central registry of all achievements in the game
 * Achievements are balanced per game mode with appropriate terminology
 */
public class AchievementRegistry {
    private static final Map<String, Achievement> achievements = new HashMap<>();

    static {
        // ===== FIRST TIME (Prime Volte) - General first achievements =====
        register(new Achievement("first_game", "ACH_FIRST_GAME", "ACH_FIRST_GAME_DESC",
            Achievement.Category.FIRST_TIME, Achievement.Tier.BRONZE));

        register(new Achievement("first_win_classic", "ACH_FIRST_WIN_CLASSIC", "ACH_FIRST_WIN_CLASSIC_DESC",
            Achievement.Category.FIRST_TIME, Achievement.Tier.BRONZE));

        register(new Achievement("first_circle_game", "ACH_FIRST_CIRCLE", "ACH_FIRST_CIRCLE_DESC",
            Achievement.Category.FIRST_TIME, Achievement.Tier.BRONZE));

        register(new Achievement("games_10", "ACH_10_GAMES", "ACH_10_GAMES_DESC",
            Achievement.Category.FIRST_TIME, Achievement.Tier.BRONZE));

        register(new Achievement("games_50", "ACH_50_GAMES", "ACH_50_GAMES_DESC",
            Achievement.Category.FIRST_TIME, Achievement.Tier.SILVER));

        register(new Achievement("games_100", "ACH_100_GAMES", "ACH_100_GAMES_DESC",
            Achievement.Category.FIRST_TIME, Achievement.Tier.GOLD));

        register(new Achievement("games_250", "ACH_250_GAMES", "ACH_250_GAMES_DESC",
            Achievement.Category.FIRST_TIME, Achievement.Tier.GOLD));

        register(new Achievement("games_500", "ACH_500_GAMES", "ACH_500_GAMES_DESC",
            Achievement.Category.FIRST_TIME, Achievement.Tier.PLATINUM));

        register(new Achievement("playtime_30min", "ACH_PLAYTIME_30MIN", "ACH_PLAYTIME_30MIN_DESC",
            Achievement.Category.FIRST_TIME, Achievement.Tier.BRONZE));

        register(new Achievement("playtime_1hour", "ACH_PLAYTIME_1HOUR", "ACH_PLAYTIME_1HOUR_DESC",
            Achievement.Category.FIRST_TIME, Achievement.Tier.SILVER));

        register(new Achievement("playtime_5hours", "ACH_PLAYTIME_5HOURS", "ACH_PLAYTIME_5HOURS_DESC",
            Achievement.Category.FIRST_TIME, Achievement.Tier.GOLD));

        register(new Achievement("playtime_10hours", "ACH_PLAYTIME_10HOURS", "ACH_PLAYTIME_10HOURS_DESC",
            Achievement.Category.FIRST_TIME, Achievement.Tier.PLATINUM));

        register(new Achievement("login_streak_3", "ACH_LOGIN_3DAYS", "ACH_LOGIN_3DAYS_DESC",
            Achievement.Category.FIRST_TIME, Achievement.Tier.BRONZE));

        register(new Achievement("login_streak_7", "ACH_LOGIN_7DAYS", "ACH_LOGIN_7DAYS_DESC",
            Achievement.Category.FIRST_TIME, Achievement.Tier.SILVER));

        register(new Achievement("login_streak_30", "ACH_LOGIN_30DAYS", "ACH_LOGIN_30DAYS_DESC",
            Achievement.Category.FIRST_TIME, Achievement.Tier.PLATINUM));

        // ===== CIRCLE MODE - Specific achievements for Circle Mode =====

        // Balls Protected (Palle Protette)
        register(new Achievement("circle_protect_10", "ACH_CIRCLE_PROTECT_10", "ACH_CIRCLE_PROTECT_10_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.BRONZE));

        register(new Achievement("circle_protect_30", "ACH_CIRCLE_PROTECT_30", "ACH_CIRCLE_PROTECT_30_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.BRONZE));

        register(new Achievement("circle_protect_50", "ACH_CIRCLE_PROTECT_50", "ACH_CIRCLE_PROTECT_50_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.SILVER));

        register(new Achievement("circle_protect_100", "ACH_CIRCLE_PROTECT_100", "ACH_CIRCLE_PROTECT_100_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.SILVER));

        register(new Achievement("circle_protect_250", "ACH_CIRCLE_PROTECT_250", "ACH_CIRCLE_PROTECT_250_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.GOLD));

        register(new Achievement("circle_protect_500", "ACH_CIRCLE_PROTECT_500", "ACH_CIRCLE_PROTECT_500_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.GOLD));

        register(new Achievement("circle_protect_1000", "ACH_CIRCLE_PROTECT_1000", "ACH_CIRCLE_PROTECT_1000_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.PLATINUM));

        register(new Achievement("circle_protect_2500", "ACH_CIRCLE_PROTECT_2500", "ACH_CIRCLE_PROTECT_2500_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.PLATINUM));

        // Circle Mode Combos (easier than Classic)
        register(new Achievement("circle_combo_25", "ACH_CIRCLE_COMBO_25", "ACH_CIRCLE_COMBO_25_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.BRONZE));

        register(new Achievement("circle_combo_50", "ACH_CIRCLE_COMBO_50", "ACH_CIRCLE_COMBO_50_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.BRONZE));

        register(new Achievement("circle_combo_100", "ACH_CIRCLE_COMBO_100", "ACH_CIRCLE_COMBO_100_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.SILVER));

        register(new Achievement("circle_combo_150", "ACH_CIRCLE_COMBO_150", "ACH_CIRCLE_COMBO_150_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.SILVER));

        register(new Achievement("circle_combo_200", "ACH_CIRCLE_COMBO_200", "ACH_CIRCLE_COMBO_200_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.GOLD));

        register(new Achievement("circle_combo_300", "ACH_CIRCLE_COMBO_300", "ACH_CIRCLE_COMBO_300_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.GOLD));

        register(new Achievement("circle_combo_500", "ACH_CIRCLE_COMBO_500", "ACH_CIRCLE_COMBO_500_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.PLATINUM));

        // Circle Mode Survival (Sopravvivenza)
        register(new Achievement("circle_survive_30s", "ACH_CIRCLE_SURVIVE_30S", "ACH_CIRCLE_SURVIVE_30S_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.BRONZE));

        register(new Achievement("circle_survive_1min", "ACH_CIRCLE_SURVIVE_1MIN", "ACH_CIRCLE_SURVIVE_1MIN_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.BRONZE));

        register(new Achievement("circle_survive_2min", "ACH_CIRCLE_SURVIVE_2MIN", "ACH_CIRCLE_SURVIVE_2MIN_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.SILVER));

        register(new Achievement("circle_survive_5min", "ACH_CIRCLE_SURVIVE_5MIN", "ACH_CIRCLE_SURVIVE_5MIN_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.GOLD));

        register(new Achievement("circle_survive_10min", "ACH_CIRCLE_SURVIVE_10MIN", "ACH_CIRCLE_SURVIVE_10MIN_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.PLATINUM));

        // Circle Mode Score (Punteggio)
        register(new Achievement("circle_score_1000", "ACH_CIRCLE_SCORE_1K", "ACH_CIRCLE_SCORE_1K_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.BRONZE));

        register(new Achievement("circle_score_5000", "ACH_CIRCLE_SCORE_5K", "ACH_CIRCLE_SCORE_5K_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.SILVER));

        register(new Achievement("circle_score_10000", "ACH_CIRCLE_SCORE_10K", "ACH_CIRCLE_SCORE_10K_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.SILVER));

        register(new Achievement("circle_score_25000", "ACH_CIRCLE_SCORE_25K", "ACH_CIRCLE_SCORE_25K_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.GOLD));

        register(new Achievement("circle_score_50000", "ACH_CIRCLE_SCORE_50K", "ACH_CIRCLE_SCORE_50K_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.PLATINUM));

        // Circle Mode Special
        register(new Achievement("circle_no_damage", "ACH_CIRCLE_NO_DAMAGE", "ACH_CIRCLE_NO_DAMAGE_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.GOLD));

        register(new Achievement("circle_games_10", "ACH_CIRCLE_10_GAMES", "ACH_CIRCLE_10_GAMES_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.BRONZE));

        register(new Achievement("circle_games_50", "ACH_CIRCLE_50_GAMES", "ACH_CIRCLE_50_GAMES_DESC",
            Achievement.Category.CIRCLE_MODE, Achievement.Tier.GOLD));

        // ===== CLASSIC MODE (PongPing) - Specific achievements =====

        // Classic Mode Victories
        register(new Achievement("classic_wins_5", "ACH_CLASSIC_5_WINS", "ACH_CLASSIC_5_WINS_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.BRONZE));

        register(new Achievement("classic_wins_10", "ACH_CLASSIC_10_WINS", "ACH_CLASSIC_10_WINS_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.BRONZE));

        register(new Achievement("classic_wins_25", "ACH_CLASSIC_25_WINS", "ACH_CLASSIC_25_WINS_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.SILVER));

        register(new Achievement("classic_wins_50", "ACH_CLASSIC_50_WINS", "ACH_CLASSIC_50_WINS_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.GOLD));

        register(new Achievement("classic_wins_100", "ACH_CLASSIC_100_WINS", "ACH_CLASSIC_100_WINS_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.PLATINUM));

        // Classic Mode AI Difficulty
        register(new Achievement("beat_easy", "ACH_BEAT_EASY", "ACH_BEAT_EASY_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.BRONZE));

        register(new Achievement("beat_normal", "ACH_BEAT_NORMAL", "ACH_BEAT_NORMAL_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.BRONZE));

        register(new Achievement("beat_hard", "ACH_BEAT_HARD", "ACH_BEAT_HARD_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.SILVER));

        register(new Achievement("beat_expert", "ACH_BEAT_EXPERT", "ACH_BEAT_EXPERT_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.GOLD));

        register(new Achievement("beat_impossible", "ACH_BEAT_IMPOSSIBLE", "ACH_BEAT_IMPOSSIBLE_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.PLATINUM));

        // Classic Mode Win Streaks
        register(new Achievement("classic_streak_3", "ACH_CLASSIC_STREAK_3", "ACH_CLASSIC_STREAK_3_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.BRONZE));

        register(new Achievement("classic_streak_5", "ACH_CLASSIC_STREAK_5", "ACH_CLASSIC_STREAK_5_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.SILVER));

        register(new Achievement("classic_streak_10", "ACH_CLASSIC_STREAK_10", "ACH_CLASSIC_STREAK_10_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.GOLD));

        register(new Achievement("classic_streak_20", "ACH_CLASSIC_STREAK_20", "ACH_CLASSIC_STREAK_20_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.PLATINUM));

        // Classic Mode Combos (much harder than Circle Mode!)
        register(new Achievement("classic_combo_5", "ACH_CLASSIC_COMBO_5", "ACH_CLASSIC_COMBO_5_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.BRONZE));

        register(new Achievement("classic_combo_10", "ACH_CLASSIC_COMBO_10", "ACH_CLASSIC_COMBO_10_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.SILVER));

        register(new Achievement("classic_combo_25", "ACH_CLASSIC_COMBO_25", "ACH_CLASSIC_COMBO_25_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.GOLD));

        register(new Achievement("classic_combo_50", "ACH_CLASSIC_COMBO_50", "ACH_CLASSIC_COMBO_50_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.PLATINUM));

        // Classic Mode Score
        register(new Achievement("classic_score_5", "ACH_CLASSIC_SCORE_5", "ACH_CLASSIC_SCORE_5_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.BRONZE));

        register(new Achievement("classic_score_10", "ACH_CLASSIC_SCORE_10", "ACH_CLASSIC_SCORE_10_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.SILVER));

        register(new Achievement("classic_score_20", "ACH_CLASSIC_SCORE_20", "ACH_CLASSIC_SCORE_20_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.GOLD));

        // Classic Mode Perfect Games
        register(new Achievement("classic_perfect_win", "ACH_CLASSIC_PERFECT", "ACH_CLASSIC_PERFECT_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.GOLD));

        register(new Achievement("classic_games_10", "ACH_CLASSIC_10_GAMES", "ACH_CLASSIC_10_GAMES_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.BRONZE));

        register(new Achievement("classic_games_50", "ACH_CLASSIC_50_GAMES", "ACH_CLASSIC_50_GAMES_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.SILVER));

        register(new Achievement("classic_games_100", "ACH_CLASSIC_100_GAMES", "ACH_CLASSIC_100_GAMES_DESC",
            Achievement.Category.CLASSIC_MODE, Achievement.Tier.GOLD));

        // ===== MASTERY - Advanced/Expert achievements =====

        register(new Achievement("master_all_ai", "ACH_MASTER_ALL_AI", "ACH_MASTER_ALL_AI_DESC",
            Achievement.Category.MASTERY, Achievement.Tier.PLATINUM));

        register(new Achievement("total_deflections_5000", "ACH_DEFLECT_5000", "ACH_DEFLECT_5000_DESC",
            Achievement.Category.MASTERY, Achievement.Tier.GOLD));

        register(new Achievement("total_deflections_10000", "ACH_DEFLECT_10000", "ACH_DEFLECT_10000_DESC",
            Achievement.Category.MASTERY, Achievement.Tier.PLATINUM));

        register(new Achievement("completionist", "ACH_COMPLETIONIST", "ACH_COMPLETIONIST_DESC",
            Achievement.Category.MASTERY, Achievement.Tier.PLATINUM));

        register(new Achievement("level_25", "ACH_LEVEL_25", "ACH_LEVEL_25_DESC",
            Achievement.Category.MASTERY, Achievement.Tier.GOLD));

        register(new Achievement("level_50", "ACH_LEVEL_50", "ACH_LEVEL_50_DESC",
            Achievement.Category.MASTERY, Achievement.Tier.PLATINUM));

        // ===== SPECIAL - Secret/Fun achievements =====

        register(new Achievement("night_owl", "ACH_NIGHT_OWL", "ACH_NIGHT_OWL_DESC",
            Achievement.Category.SPECIAL, Achievement.Tier.SILVER));

        register(new Achievement("dedicated_player", "ACH_DEDICATED", "ACH_DEDICATED_DESC",
            Achievement.Category.SPECIAL, Achievement.Tier.GOLD));

        register(new Achievement("lucky_number", "ACH_LUCKY_NUMBER", "ACH_LUCKY_NUMBER_DESC",
            Achievement.Category.SPECIAL, Achievement.Tier.BRONZE));
    }

    private static void register(Achievement achievement) {
        achievements.put(achievement.getId(), achievement);
    }

    /**
     * Get achievement by ID
     */
    public static Achievement getAchievement(String id) {
        return achievements.get(id);
    }

    /**
     * Get all achievements
     */
    public static Collection<Achievement> getAllAchievements() {
        return new ArrayList<>(achievements.values());
    }

    /**
     * Get achievements by category
     */
    public static List<Achievement> getAchievementsByCategory(Achievement.Category category) {
        List<Achievement> result = new ArrayList<>();
        for (Achievement ach : achievements.values()) {
            if (ach.getCategory() == category) {
                result.add(ach);
            }
        }
        return result;
    }

    /**
     * Get achievements by tier
     */
    public static List<Achievement> getAchievementsByTier(Achievement.Tier tier) {
        List<Achievement> result = new ArrayList<>();
        for (Achievement ach : achievements.values()) {
            if (ach.getTier() == tier) {
                result.add(ach);
            }
        }
        return result;
    }

    /**
     * Get total count of achievements
     */
    public static int getTotalCount() {
        return achievements.size();
    }
}
