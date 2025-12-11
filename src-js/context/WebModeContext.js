/**
 * Web Mode Context
 * Detects if the game is running in web mode (native JavaScript)
 * Use this to disable features that don't work well in the browser
 */

// In native JavaScript, we're always in web mode
let webMode = true;

/**
 * Check if the game is running in web mode
 * @returns {boolean} true if running in browser
 */
export function isWebMode() {
    return webMode;
}

/**
 * Check if the game is running as a desktop application
 * @returns {boolean} true if running as desktop app, false if in browser
 */
export function isDesktopMode() {
    return !isWebMode();
}

/**
 * Set web mode (useful for testing or hybrid setups)
 * @param {boolean} value - true for web mode, false for desktop mode
 */
export function setWebMode(value) {
    webMode = value;
}

/**
 * Print debug information about the current mode
 */
export function printModeInfo() {
    if (isWebMode()) {
        console.log('[WebMode] Running in browser as native JavaScript');
        console.log('[WebMode] Some features may be disabled for web compatibility');
    } else {
        console.log('[DesktopMode] Running as native desktop application');
    }
}
