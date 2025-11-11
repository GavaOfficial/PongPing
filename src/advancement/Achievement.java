package advancement;

/**
 * Represents a single achievement in the game
 */
public class Achievement {
    public enum Category {
        FIRST_TIME,      // Prime Volte - First achievements
        CIRCLE_MODE,     // Circle Mode - Specific to Circle Mode
        CLASSIC_MODE,    // Classic Mode - Specific to Classic/PongPing Mode
        MASTERY,         // Maestria - Advanced mastery achievements
        SPECIAL          // Speciali - Special/Secret achievements
    }

    public enum Tier {
        BRONZE(50, 0xCD7F32),
        SILVER(100, 0xC0C0C0),
        GOLD(200, 0xFFD700),
        PLATINUM(500, 0xE5E4E2);

        public final int xpReward;
        public final int color;

        Tier(int xpReward, int color) {
            this.xpReward = xpReward;
            this.color = color;
        }
    }

    private final String id;
    private final String nameKey;           // Language key for name
    private final String descriptionKey;    // Language key for description
    private final Category category;
    private final Tier tier;

    public Achievement(String id, String nameKey, String descriptionKey, Category category, Tier tier) {
        this.id = id;
        this.nameKey = nameKey;
        this.descriptionKey = descriptionKey;
        this.category = category;
        this.tier = tier;
    }

    public String getId() { return id; }
    public String getNameKey() { return nameKey; }
    public String getDescriptionKey() { return descriptionKey; }
    public Category getCategory() { return category; }
    public Tier getTier() { return tier; }
    public int getXPReward() { return tier.xpReward; }
    public int getColor() { return tier.color; }
}
