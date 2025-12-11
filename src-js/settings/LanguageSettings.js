/**
 * Language Settings
 * Manages loading and switching of language files
 */

import { currentLanguage, currentLanguageCode, setCurrentLanguage, setCurrentLanguageCode } from '../context/LanguageContext.js';

/**
 * LanguageSettings class
 */
export class LanguageSettings {
    constructor() {
    }

    /**
     * Load language file
     * @param {string} languageCode - Language code (italiano, inglese, spagnolo)
     */
    async loadLanguage(languageCode) {
        console.log(`Loading language: ${languageCode}`);
        
        try {
            // Try to load from file
            const response = await fetch(`lingue/${languageCode}.txt`);
            if (response.ok) {
                const text = await response.text();
                const langMap = new Map();
                
                // Parse language file
                const lines = text.split('\n');
                for (const line of lines) {
                    const trimmed = line.trim();
                    if (trimmed && !trimmed.startsWith('#')) {
                        const parts = trimmed.split('=');
                        if (parts.length >= 2) {
                            const key = parts[0].trim();
                            const value = parts.slice(1).join('=').trim();
                            langMap.set(key, value);
                        }
                    }
                }
                
                setCurrentLanguage(langMap);
                setCurrentLanguageCode(languageCode);
                console.log(`✓ Language loaded: ${languageCode} (${langMap.size} keys)`);
            } else {
                console.log(`⚠️  Language file not found: ${languageCode}.txt`);
                this.loadDefaultLanguage();
            }
        } catch (e) {
            console.error(`Could not load language file: ${languageCode}.txt`, e);
            this.loadDefaultLanguage();
        }
    }

    /**
     * Load default language (Italian)
     */
    loadDefaultLanguage() {
        this.loadItalianLanguage();
        setCurrentLanguageCode('italiano');
    }

    /**
     * Load Italian language (hardcoded fallback)
     */
    loadItalianLanguage() {
        const langMap = new Map();
        langMap.set('MENU_SINGLE_PLAYER', 'SINGLE PLAYER');
        langMap.set('MENU_TWO_PLAYERS', 'TWO PLAYERS');
        langMap.set('MENU_HISTORY', 'CRONOLOGIA');
        langMap.set('MENU_SETTINGS', 'IMPOSTAZIONI');
        langMap.set('MENU_EXIT', 'EXIT');
        langMap.set('FIRST_ACCESS_TITLE', 'PONG PING');
        langMap.set('FIRST_ACCESS_SUBTITLE', 'Il classico gioco rivisto');
        langMap.set('SETTINGS_DIFFICULTY', 'DIFFICOLTA');
        langMap.set('SETTINGS_PADDLE', 'IMPOSTAZIONI PADDLE');
        langMap.set('SETTINGS_CONTROLS', 'COMANDI');
        langMap.set('SETTINGS_AUDIO', 'AUDIO');
        langMap.set('SETTINGS_LANGUAGE', 'LINGUA');
        langMap.set('PADDLE_SPEED_SLOW', 'LENTA');
        langMap.set('PADDLE_SPEED_MEDIUM', 'MEDIA');
        langMap.set('PADDLE_SPEED_FAST', 'VELOCE');
        langMap.set('AI_DIFFICULTY_EASY', 'FACILE');
        langMap.set('AI_DIFFICULTY_NORMAL', 'NORMALE');
        langMap.set('AI_DIFFICULTY_HARD', 'DIFFICILE');
        langMap.set('AI_DIFFICULTY_EXPERT', 'ESPERTO');
        langMap.set('AI_DIFFICULTY_IMPOSSIBLE', 'IMPOSSIBILE');
        
        setCurrentLanguage(langMap);
    }

    /**
     * Load English language (hardcoded fallback)
     */
    loadEnglishLanguage() {
        const langMap = new Map();
        langMap.set('MENU_SINGLE_PLAYER', 'SINGLE PLAYER');
        langMap.set('MENU_TWO_PLAYERS', 'TWO PLAYERS');
        langMap.set('MENU_HISTORY', 'HISTORY');
        langMap.set('MENU_SETTINGS', 'SETTINGS');
        langMap.set('MENU_EXIT', 'EXIT');
        langMap.set('FIRST_ACCESS_TITLE', 'PONG PING');
        langMap.set('FIRST_ACCESS_SUBTITLE', 'The classic game revisited');
        langMap.set('SETTINGS_DIFFICULTY', 'DIFFICULTY');
        langMap.set('SETTINGS_PADDLE', 'PADDLE SETTINGS');
        langMap.set('SETTINGS_CONTROLS', 'CONTROLS');
        langMap.set('SETTINGS_AUDIO', 'AUDIO');
        langMap.set('SETTINGS_LANGUAGE', 'LANGUAGE');
        langMap.set('PADDLE_SPEED_SLOW', 'SLOW');
        langMap.set('PADDLE_SPEED_MEDIUM', 'MEDIUM');
        langMap.set('PADDLE_SPEED_FAST', 'FAST');
        langMap.set('AI_DIFFICULTY_EASY', 'EASY');
        langMap.set('AI_DIFFICULTY_NORMAL', 'NORMAL');
        langMap.set('AI_DIFFICULTY_HARD', 'HARD');
        langMap.set('AI_DIFFICULTY_EXPERT', 'EXPERT');
        langMap.set('AI_DIFFICULTY_IMPOSSIBLE', 'IMPOSSIBLE');
        
        setCurrentLanguage(langMap);
    }

    /**
     * Get translated text by key
     * @param {string} key 
     * @returns {string}
     */
    getText(key) {
        return currentLanguage.get(key) || key;
    }
}
