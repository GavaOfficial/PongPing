/**
 * Language Context
 * Manages localization and language switching
 */

// Localization system
export let currentLanguage = new Map();
export let currentLanguageCode = 'italiano'; // Default language

// Setters for modifying exported values
export function setCurrentLanguage(lang) {
    currentLanguage = lang;
}

export function setCurrentLanguageCode(code) {
    currentLanguageCode = code;
}

/**
 * Get a localized string by key
 * @param {string} key - The translation key
 * @returns {string} - The translated string or the key if not found
 */
export function getLocalizedString(key) {
    return currentLanguage.get(key) || key;
}
