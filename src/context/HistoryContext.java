package context;

import game.GameHistoryEntry;

import java.io.File;

public class HistoryContext {

    // History system variables
    public static java.util.List<GameHistoryEntry> gameHistory = new java.util.ArrayList<>();
    public static int selectedHistoryMode = 0; // 0 = Single Player, 1 = Two Players
    public static int selectedHistoryCard = 0; // Indice della card selezionata nella cronologia
    public static int historyScrollOffset = 0; // Offset per scroll automatico delle card
    public static final String HISTORY_FILE = getHistoryFilePath();

    private static String getHistoryFilePath() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        String appDataPath;

        if (os.contains("win")) {
            // Windows: %APPDATA%\GavaTech\Pong-Ping\
            appDataPath = System.getenv("APPDATA");
            if (appDataPath == null) {
                appDataPath = userHome + File.separator + "AppData" + File.separator + "Roaming";
            }
        } else if (os.contains("mac")) {
            // macOS: ~/Library/Application Support/GavaTech/Pong-Ping/
            appDataPath = userHome + File.separator + "Library" + File.separator + "Application Support";
        } else {
            // Linux/Unix: ~/.config/GavaTech/Pong-Ping/
            appDataPath = userHome + File.separator + ".config";
        }

        // Create directory structure: GavaTech/Pong-Ping
        File directory = new File(appDataPath + File.separator + "GavaTech" + File.separator + "Pong-Ping");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        return directory.getAbsolutePath() + File.separator + "game_history.txt";
    }

}
