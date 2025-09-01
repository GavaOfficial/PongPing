package settings;

import context.ContextLoader;

import java.io.*;

import static context.ContextLoader.getResourcePath;
import static context.LanguageContext.currentLanguage;
import static context.LanguageContext.currentLanguageCode;

public class LanguageSettings {

    // Localization methods
    public void loadLanguage(String languageCode) {
        currentLanguage.clear();
        BufferedReader reader = null;

        try {
            // Try to load from app context first (for jpackage apps with --app-content)
            String filename = getResourcePath("lingue/" + languageCode + ".txt");
            File languageFile = new File(filename);
            if (languageFile.exists()) {
                reader = new BufferedReader(new FileReader(languageFile));
                System.out.println("✓ Language loaded from app context: " + languageCode + ".txt");
            } else {
                // Fallback: try loading from JAR resources
                InputStream languageStream = ContextLoader.class.getClassLoader().getResourceAsStream("lingue/" + languageCode + ".txt");
                if (languageStream != null) {
                    reader = new BufferedReader(new InputStreamReader(languageStream));
                    System.out.println("✓ Language loaded from JAR: " + languageCode + ".txt");
                } else {
                    System.out.println("⚠️  Language file not found in app context or JAR: " + languageCode + ".txt");
                }
            }

            if (reader != null) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            currentLanguage.put(parts[0].trim(), parts[1].trim());
                        }
                    }
                }
                currentLanguageCode = languageCode;
                System.out.println("Language successfully loaded: " + languageCode);
            } else {
                System.out.println("⚠️  Language file not found: " + languageCode + ".txt");
                loadDefaultLanguage();
            }
        } catch (IOException e) {
            System.out.println("Could not load language file: " + languageCode + ".txt - " + e.getMessage());
            loadDefaultLanguage();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Ignore close errors
                }
            }
        }
    }

    public void loadDefaultLanguage() {
        // Fallback to Italian if language loading fails
        loadItalianLanguage();
        currentLanguageCode = "italiano"; // Reset to default language code
    }

    public void loadItalianLanguage() {
        currentLanguage.put("MENU_SINGLE_PLAYER", "SINGLE PLAYER");
        currentLanguage.put("MENU_TWO_PLAYERS", "TWO PLAYERS");
        currentLanguage.put("MENU_SETTINGS", "IMPOSTAZIONI");
        currentLanguage.put("MENU_EXIT", "EXIT");
        currentLanguage.put("FIRST_ACCESS_TITLE", "PONG PING");
        currentLanguage.put("FIRST_ACCESS_SUBTITLE", "Il classico gioco rivisto");
        currentLanguage.put("SETTINGS_DIFFICULTY", "DIFFICOLTA");
        currentLanguage.put("SETTINGS_PADDLE", "IMPOSTAZIONI PADDLE");
        currentLanguage.put("SETTINGS_CONTROLS", "COMANDI");
        currentLanguage.put("SETTINGS_AUDIO", "AUDIO");
        currentLanguage.put("SETTINGS_LANGUAGE", "LINGUA");
        currentLanguage.put("PADDLE_SPEED_SLOW", "LENTA");
        currentLanguage.put("PADDLE_SPEED_MEDIUM", "MEDIA");
        currentLanguage.put("PADDLE_SPEED_FAST", "VELOCE");
        currentLanguage.put("AI_DIFFICULTY_EASY", "FACILE");
        currentLanguage.put("AI_DIFFICULTY_NORMAL", "NORMALE");
        currentLanguage.put("AI_DIFFICULTY_HARD", "DIFFICILE");
        currentLanguage.put("AI_DIFFICULTY_EXPERT", "ESPERTO");
        currentLanguage.put("AI_DIFFICULTY_IMPOSSIBLE", "IMPOSSIBILE");
    }

    public void loadEnglishLanguage() {
        currentLanguage.put("MENU_SINGLE_PLAYER", "SINGLE PLAYER");
        currentLanguage.put("MENU_TWO_PLAYERS", "TWO PLAYERS");
        currentLanguage.put("MENU_SETTINGS", "SETTINGS");
        currentLanguage.put("MENU_EXIT", "EXIT");
        currentLanguage.put("FIRST_ACCESS_TITLE", "PONG PING");
        currentLanguage.put("FIRST_ACCESS_SUBTITLE", "The classic game revisited");
        currentLanguage.put("SETTINGS_DIFFICULTY", "DIFFICULTY");
        currentLanguage.put("SETTINGS_PADDLE", "PADDLE SETTINGS");
        currentLanguage.put("SETTINGS_CONTROLS", "CONTROLS");
        currentLanguage.put("SETTINGS_AUDIO", "AUDIO");
        currentLanguage.put("SETTINGS_LANGUAGE", "LANGUAGE");
        currentLanguage.put("PADDLE_SPEED_SLOW", "SLOW");
        currentLanguage.put("PADDLE_SPEED_MEDIUM", "MEDIUM");
        currentLanguage.put("PADDLE_SPEED_FAST", "FAST");
        currentLanguage.put("AI_DIFFICULTY_EASY", "EASY");
        currentLanguage.put("AI_DIFFICULTY_NORMAL", "NORMAL");
        currentLanguage.put("AI_DIFFICULTY_HARD", "HARD");
        currentLanguage.put("AI_DIFFICULTY_EXPERT", "EXPERT");
        currentLanguage.put("AI_DIFFICULTY_IMPOSSIBLE", "IMPOSSIBLE");
    }

}
