/**
 * Context Loader
 * Manages loading of all game resources (fonts, images, audio, languages)
 */

/**
 * ContextLoader class
 */
export class ContextLoader {
    constructor() {
        this.loadedFonts = [];
        this.loadedImages = {};
        this.loadedAudio = {};
    }

    /**
     * Load all game resources
     * @returns {Promise<boolean>}
     */
    async loadAllResources() {
        console.log('Loading game resources...');
        
        try {
            await this.loadFonts();
            await this.loadImages();
            await this.loadLanguages();
            // Audio loading is handled by MusicSettings
            
            console.log('✓ All resources loaded');
            return true;
        } catch (e) {
            console.error('Error loading resources:', e);
            return false;
        }
    }

    /**
     * Load custom fonts
     * @returns {Promise<void>}
     */
    async loadFonts() {
        const fonts = [
            { family: 'Silkscreen', url: 'font/Silkscreen/Silkscreen-Regular.ttf' },
            { family: 'Space Mono', url: 'font/Space_Mono/SpaceMono-Regular.ttf' }
        ];

        const promises = fonts.map(async ({family, url}) => {
            try {
                const font = new FontFace(family, `url(${url})`);
                await font.load();
                document.fonts.add(font);
                this.loadedFonts.push(family);
                console.log(`✓ Font loaded: ${family}`);
            } catch (e) {
                console.warn(`Could not load font ${family}:`, e.message);
            }
        });

        await Promise.all(promises);
    }

    /**
     * Load images (backgrounds, paddle themes)
     * @returns {Promise<void>}
     */
    async loadImages() {
        // Placeholder - would load background and paddle theme images
        // For now, using solid colors
        console.log('✓ Image loading skipped (using defaults)');
    }

    /**
     * Load language files
     * @returns {Promise<void>}
     */
    async loadLanguages() {
        // Language loading is handled by LanguageSettings
        console.log('✓ Language loading delegated to LanguageSettings');
    }

    /**
     * Get resource path (helper method)
     * @param {string} path 
     * @returns {string}
     */
    static getResourcePath(path) {
        return path;
    }
}
