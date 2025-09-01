package settings;

import game.GameHistoryEntry;

import java.io.BufferedReader;
import java.io.FileReader;

import static context.HistoryContext.HISTORY_FILE;
import static context.HistoryContext.gameHistory;

public class HistorySettings {

    // Load game history from file
    public void loadGameHistory() {
        gameHistory.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(HISTORY_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty() && !line.startsWith("#")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 13) {
                        // New format with player1MaxCombo and player2MaxCombo
                        GameHistoryEntry entry = new GameHistoryEntry(
                                parts[0].trim(), parts[1].trim(), parts[2].trim(),
                                Integer.parseInt(parts[3].trim()), Integer.parseInt(parts[4].trim()),
                                parts[5].trim(), Integer.parseInt(parts[6].trim()),
                                parts[7].trim(), parts[8].trim(),
                                Integer.parseInt(parts[11].trim()), Integer.parseInt(parts[12].trim()), parts[10].trim()
                        );
                        gameHistory.add(entry);
                    } else if (parts.length >= 11) {
                        // Format with maxCombo and rank (single player)
                        GameHistoryEntry entry = new GameHistoryEntry(
                                parts[0].trim(), parts[1].trim(), parts[2].trim(),
                                Integer.parseInt(parts[3].trim()), Integer.parseInt(parts[4].trim()),
                                parts[5].trim(), Integer.parseInt(parts[6].trim()),
                                parts[7].trim(), parts[8].trim(), Integer.parseInt(parts[9].trim()), parts[10].trim()
                        );
                        gameHistory.add(entry);
                    } else if (parts.length >= 9) {
                        // Old format compatibility - default values for missing fields
                        GameHistoryEntry entry = new GameHistoryEntry(
                                parts[0].trim(), parts[1].trim(), parts[2].trim(),
                                Integer.parseInt(parts[3].trim()), Integer.parseInt(parts[4].trim()),
                                parts[5].trim(), Integer.parseInt(parts[6].trim()),
                                parts[7].trim(), parts[8].trim(), 0, "UNRANKED"
                        );
                        gameHistory.add(entry);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("DEBUG: Could not load game history: " + e.getMessage());
        }
    }

    // Save game history entry
    // Save game history entry for single player mode
    public void saveGameHistoryEntry(String gameMode, int p1Score, int p2Score, String winner,
                                     int rallies, String duration, String difficulty, int maxCombo, String rank) {
        try {
            // Create entry
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy");
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm:ss");
            java.util.Date now = new java.util.Date();

            GameHistoryEntry entry = new GameHistoryEntry(
                    dateFormat.format(now), timeFormat.format(now), gameMode,
                    p1Score, p2Score, winner, rallies, duration, difficulty, maxCombo, rank
            );

            // Add to list
            gameHistory.add(0, entry); // Add to beginning for newest first

            // Save to file
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(HISTORY_FILE, true))) {
                pw.println(entry.date + "|" + entry.time + "|" + entry.gameMode + "|" +
                        entry.player1Score + "|" + entry.player2Score + "|" + entry.winner + "|" +
                        entry.rallies + "|" + entry.duration + "|" + entry.difficulty + "|" +
                        entry.maxCombo + "|" + entry.rank);
            }

        } catch (Exception e) {
            System.out.println("DEBUG: Could not save game history: " + e.getMessage());
        }
    }

    // Save game history entry for two players mode
    public void saveGameHistoryEntryTwoPlayers(String gameMode, int p1Score, int p2Score, String winner,
                                               int rallies, String duration, String difficulty,
                                               int p1MaxCombo, int p2MaxCombo, String rank) {
        try {
            // Create entry
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy");
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm:ss");
            java.util.Date now = new java.util.Date();

            GameHistoryEntry entry = new GameHistoryEntry(
                    dateFormat.format(now), timeFormat.format(now), gameMode,
                    p1Score, p2Score, winner, rallies, duration, difficulty,
                    p1MaxCombo, p2MaxCombo, rank
            );

            // Add to list
            gameHistory.add(0, entry); // Add to beginning for newest first

            // Save to file with new format including both combos
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(HISTORY_FILE, true))) {
                pw.println(entry.date + "|" + entry.time + "|" + entry.gameMode + "|" +
                        entry.player1Score + "|" + entry.player2Score + "|" + entry.winner + "|" +
                        entry.rallies + "|" + entry.duration + "|" + entry.difficulty + "|" +
                        entry.maxCombo + "|" + entry.rank + "|" + p1MaxCombo + "|" + p2MaxCombo);
            }

        } catch (Exception e) {
            System.out.println("DEBUG: Could not save game history: " + e.getMessage());
        }
    }



}
