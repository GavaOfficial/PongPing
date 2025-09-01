package game;

// Game History Entry class
public class GameHistoryEntry {
    public String date;
    public String time;
    public String gameMode;
    public int player1Score;
    public int player2Score;
    public String winner;
    public int rallies;
    public String duration;
    public String difficulty;
    public int maxCombo; // For single player mode compatibility
    public int player1MaxCombo; // For two players mode
    public int player2MaxCombo; // For two players mode
    public String rank;

    // Constructor for single player mode (backward compatibility)
    public GameHistoryEntry(String date, String time, String gameMode, int p1Score, int p2Score,
                            String winner, int rallies, String duration, String difficulty, int maxCombo, String rank) {
        this.date = date;
        this.time = time;
        this.gameMode = gameMode;
        this.player1Score = p1Score;
        this.player2Score = p2Score;
        this.winner = winner;
        this.rallies = rallies;
        this.duration = duration;
        this.difficulty = difficulty;
        this.maxCombo = maxCombo;
        this.rank = rank;
        // Set default values for two players combos
        this.player1MaxCombo = 0;
        this.player2MaxCombo = 0;
    }

    // Constructor for two players mode
    public GameHistoryEntry(String date, String time, String gameMode, int p1Score, int p2Score,
                            String winner, int rallies, String duration, String difficulty,
                            int p1MaxCombo, int p2MaxCombo, String rank) {
        this.date = date;
        this.time = time;
        this.gameMode = gameMode;
        this.player1Score = p1Score;
        this.player2Score = p2Score;
        this.winner = winner;
        this.rallies = rallies;
        this.duration = duration;
        this.difficulty = difficulty;
        this.player1MaxCombo = p1MaxCombo;
        this.player2MaxCombo = p2MaxCombo;
        this.rank = rank;
        // Set maxCombo to the higher of the two for compatibility
        this.maxCombo = Math.max(p1MaxCombo, p2MaxCombo);
    }

    @Override
    public String toString() {
        return date + " " + time + " | " + gameMode + " | " + player1Score + "-" + player2Score +
                " | Winner: " + winner + " | Rallies: " + rallies + " | Duration: " + duration +
                " | Difficulty: " + difficulty + " | Max Combo: " + maxCombo + " | Rank: " + rank;
    }
}