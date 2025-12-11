/**
 * General Settings
 * Manages loading and saving of general game settings
 */

import {
    paddleSpeedSetting, aiDifficultySetting, ballSpeedSetting,
    player1UpKey, player1DownKey, player2UpKey, player2DownKey,
    selectedPaddleTheme, selectedRightPaddleTheme,
    musicVolume, effectsVolume, musicEnabled,
    circleMaxCombo, circleMaxScore, wasInCircleMode,
    isFirstRun, SETTINGS_FILE,
    setPaddleSpeedSetting, setAiDifficultySetting, setBallSpeedSetting,
    setPlayer1UpKey, setPlayer1DownKey, setPlayer2UpKey, setPlayer2DownKey,
    setSelectedPaddleTheme, setSelectedRightPaddleTheme,
    setMusicVolume, setEffectsVolume, setMusicEnabled,
    setCircleMaxCombo, setCircleMaxScore, setWasInCircleMode,
    setIsFirstRun, updateLocalizedArrays
} from '../context/SettingsContext.js';

import { selectedBackground, setSelectedBackground } from '../context/GameContext.js';
import { currentLanguageCode, setCurrentLanguageCode } from '../context/LanguageContext.js';

/**
 * GeneralSettings class
 */
export class GeneralSettings {
    constructor() {
    }

    /**
     * Load settings from localStorage
     */
    loadSettings() {
        try {
            const data = localStorage.getItem(SETTINGS_FILE);
            if (data) {
                const settings = JSON.parse(data);
                
                setPaddleSpeedSetting(settings.paddleSpeed || 1);
                setAiDifficultySetting(settings.aiDifficulty || 2);
                setBallSpeedSetting(settings.ballSpeed || 25);
                setPlayer1UpKey(settings.player1UpKey || 'KeyW');
                setPlayer1DownKey(settings.player1DownKey || 'KeyS');
                setPlayer2UpKey(settings.player2UpKey || 'ArrowUp');
                setPlayer2DownKey(settings.player2DownKey || 'ArrowDown');
                setSelectedPaddleTheme(settings.paddleTheme || 0);
                setSelectedRightPaddleTheme(settings.rightPaddleTheme || 0);
                setMusicVolume(settings.musicVolume || 50);
                setEffectsVolume(settings.effectsVolume || 75);
                setMusicEnabled(settings.musicEnabled !== undefined ? settings.musicEnabled : true);
                setSelectedBackground(settings.selectedBackground || 0);
                setCurrentLanguageCode(settings.languageCode || 'italiano');
                setCircleMaxCombo(settings.circleMaxCombo || 0);
                setCircleMaxScore(settings.circleMaxScore || 0);
                setWasInCircleMode(settings.wasInCircleMode || false);
                
                setIsFirstRun(false);
                console.log('✓ Settings loaded from localStorage');
            } else {
                setIsFirstRun(true);
                console.log('No saved settings found, using defaults');
            }
        } catch (e) {
            console.error('Error loading settings:', e);
            this.resetToDefaults();
            setIsFirstRun(true);
        }
    }

    /**
     * Reset settings to default values
     */
    resetToDefaults() {
        setPaddleSpeedSetting(1);
        setAiDifficultySetting(2);
        setBallSpeedSetting(25);
        setPlayer1UpKey('KeyW');
        setPlayer1DownKey('KeyS');
        setPlayer2UpKey('ArrowUp');
        setPlayer2DownKey('ArrowDown');
        setSelectedPaddleTheme(0);
        setSelectedRightPaddleTheme(0);
        setMusicVolume(50);
        setEffectsVolume(75);
        setMusicEnabled(true);
        setSelectedBackground(0);
        setCurrentLanguageCode('italiano');
        setCircleMaxCombo(0);
        setCircleMaxScore(0);
        setWasInCircleMode(false);
    }

    /**
     * Save settings to localStorage
     */
    saveSettings() {
        try {
            const settings = {
                paddleSpeed: paddleSpeedSetting,
                aiDifficulty: aiDifficultySetting,
                ballSpeed: ballSpeedSetting,
                player1UpKey: player1UpKey,
                player1DownKey: player1DownKey,
                player2UpKey: player2UpKey,
                player2DownKey: player2DownKey,
                paddleTheme: selectedPaddleTheme,
                rightPaddleTheme: selectedRightPaddleTheme,
                musicVolume: musicVolume,
                effectsVolume: effectsVolume,
                musicEnabled: musicEnabled,
                selectedBackground: selectedBackground,
                languageCode: currentLanguageCode,
                circleMaxCombo: circleMaxCombo,
                circleMaxScore: circleMaxScore,
                wasInCircleMode: wasInCircleMode
            };
            
            localStorage.setItem(SETTINGS_FILE, JSON.stringify(settings));
            console.log('✓ Settings saved to localStorage');
        } catch (e) {
            console.error('Error saving settings:', e);
        }
    }

    /**
     * Load settings from file (calls loadSettings and language loader)
     * @param {LanguageSettings} languageSettings 
     */
    loadSettingsFromFile(languageSettings) {
        console.log('Loading settings...');
        this.loadSettings();
        
        // Load language based on saved setting
        if (languageSettings) {
            languageSettings.loadLanguage(currentLanguageCode);
            updateLocalizedArrays((key) => languageSettings.getText(key));
        }
    }
}
