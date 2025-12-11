/**
 * Font Context
 * Manages font loading and storage
 */

// Fonts
export let primaryFont = null;
export let secondaryFont = null;
export let rankFont = null; // Font for rank display

// Setters for modifying exported values
export function setPrimaryFont(font) {
    primaryFont = font;
}

export function setSecondaryFont(font) {
    secondaryFont = font;
}

export function setRankFont(font) {
    rankFont = font;
}
