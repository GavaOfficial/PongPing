/**
 * PongPing - JavaScript Version
 * Main entry point and game initialization
 */

import { GameState } from './game/GameState.js';
import { PongGame } from './game/PongGame.js';
import { BASE_WIDTH, BASE_HEIGHT } from './context/DimensionalContext.js';
import { 
    BOARD_WIDTH, BOARD_HEIGHT, 
    setBoardDimensions, 
    setScaleFactors 
} from './context/GameContext.js';
import { setCurrentState } from './context/AnimationContext.js';
import { isWebMode, printModeInfo } from './context/WebModeContext.js';
import { setCurrentLanguage } from './context/LanguageContext.js';
import { GeneralSettings } from './settings/GeneralSettings.js';
import { LanguageSettings } from './settings/LanguageSettings.js';
import { MusicSettings } from './settings/MusicSettings.js';

/**
 * Main game class - manages canvas, game loop, and core systems
 */
class Main {
    constructor() {
        this.canvas = document.getElementById('gameCanvas');
        if (!this.canvas) {
            throw new Error('Canvas element #gameCanvas not found');
        }
        
        this.ctx = this.canvas.getContext('2d');
        this.lastTime = 0;
        this.gameRunning = false;
        
        // Settings
        this.generalSettings = new GeneralSettings();
        this.languageSettings = new LanguageSettings();
        this.musicSettings = new MusicSettings();
        
        // Game instance
        this.game = null;
        
        // Set initial canvas size
        this.resizeCanvas();
        
        // Initialize game systems
        this.init();
    }

    /**
     * Initialize all game systems
     */
    async init() {
        console.log('=== PongPing JavaScript Initialization ===');
        
        // Print mode info
        printModeInfo();
        
        // Set initial game state
        // Check if first run (no settings in localStorage)
        let isFirstRun = true;
        try {
            isFirstRun = !localStorage.getItem('pongping_settings');
        } catch (e) {
            console.warn('localStorage not available, assuming first run:', e);
        }
        setCurrentState(isFirstRun ? GameState.FIRST_ACCESS : GameState.MENU);
        
        console.log('Initial game state:', isFirstRun ? 'FIRST_ACCESS' : 'MENU');
        
        // Load resources
        await this.loadResources();
        
        // Initialize game
        this.game = new PongGame(this.canvas, this.ctx);
        
        // Set up event listeners
        this.setupEventListeners();
        
        // Hide loading screen
        const loadingEl = document.getElementById('loading');
        if (loadingEl) {
            loadingEl.classList.add('hidden');
        }
        
        // Start game loop
        this.startGameLoop();
        
        console.log('✓ Initialization complete - Game ready to play!');
    }

    /**
     * Load all game resources (fonts, images, audio, language files)
     */
    async loadResources() {
        console.log('Loading resources...');
        
        // TODO: Load fonts
        // TODO: Load language files
        // TODO: Load images (backgrounds, paddle themes)
        // TODO: Load audio
        
        // For now, just set up defaults
        await this.loadFonts();
        await this.loadLanguage();
        
        console.log('✓ Resources loaded');
    }

    /**
     * Load custom fonts
     */
    async loadFonts() {
        // Load Silkscreen font
        const silkscreenFont = new FontFace(
            'Silkscreen',
            'url(font/Silkscreen/Silkscreen-Regular.ttf)'
        );
        
        // Load Space Mono font
        const spaceMonoFont = new FontFace(
            'Space Mono',
            'url(font/Space_Mono/SpaceMono-Regular.ttf)'
        );
        
        try {
            await silkscreenFont.load();
            await spaceMonoFont.load();
            document.fonts.add(silkscreenFont);
            document.fonts.add(spaceMonoFont);
            console.log('✓ Fonts loaded');
        } catch (e) {
            console.warn('Could not load custom fonts:', e);
            // Fall back to system fonts
        }
    }

    /**
     * Load language file (default: Italian)
     */
    async loadLanguage() {
        try {
            const response = await fetch('lingue/italiano.properties');
            const text = await response.text();
            
            // Parse properties file (simple key=value format)
            const lines = text.split('\n');
            const languageMap = new Map();
            
            for (const line of lines) {
                const trimmed = line.trim();
                if (trimmed && !trimmed.startsWith('#')) {
                    const [key, ...valueParts] = trimmed.split('=');
                    if (key && valueParts.length > 0) {
                        languageMap.set(key.trim(), valueParts.join('=').trim());
                    }
                }
            }
            
            // Store in LanguageContext
            setCurrentLanguage(languageMap);
            console.log('✓ Language loaded: italiano (' + languageMap.size + ' keys)');
        } catch (e) {
            console.warn('Could not load language file:', e);
        }
    }

    /**
     * Set up event listeners for input
     */
    setupEventListeners() {
        // Window resize
        window.addEventListener('resize', () => this.resizeCanvas());
        
        // Keyboard input
        window.addEventListener('keydown', (e) => this.handleKeyDown(e));
        window.addEventListener('keyup', (e) => this.handleKeyUp(e));
        
        // Mouse input
        this.canvas.addEventListener('mousemove', (e) => this.handleMouseMove(e));
        this.canvas.addEventListener('mousedown', (e) => this.handleMouseDown(e));
        this.canvas.addEventListener('mouseup', (e) => this.handleMouseUp(e));
        this.canvas.addEventListener('click', (e) => this.handleClick(e));
        
        // Mouse wheel (for scrolling in menus)
        this.canvas.addEventListener('wheel', (e) => this.handleWheel(e), { passive: false });
        
        console.log('✓ Event listeners set up');
    }

    /**
     * Resize canvas to fit window while maintaining aspect ratio
     */
    resizeCanvas() {
        const windowWidth = window.innerWidth;
        const windowHeight = window.innerHeight;
        
        // Calculate scale to fit window while maintaining aspect ratio
        const scaleX = windowWidth / BASE_WIDTH;
        const scaleY = windowHeight / BASE_HEIGHT;
        const scale = Math.min(scaleX, scaleY);
        
        // Set canvas size
        this.canvas.width = BASE_WIDTH * scale;
        this.canvas.height = BASE_HEIGHT * scale;
        
        // Update board dimensions
        setBoardDimensions(BASE_WIDTH, BASE_HEIGHT);
        setScaleFactors(scale, scale);
        
        // Enable image smoothing for better quality
        this.ctx.imageSmoothingEnabled = true;
        this.ctx.imageSmoothingQuality = 'high';
        
        console.log(`Canvas resized: ${this.canvas.width}x${this.canvas.height} (scale: ${scale.toFixed(2)})`);
    }

    /**
     * Start the game loop
     */
    startGameLoop() {
        this.gameRunning = true;
        this.lastTime = performance.now();
        this.gameLoop(this.lastTime);
    }

    /**
     * Stop the game loop
     */
    stopGameLoop() {
        this.gameRunning = false;
    }

    /**
     * Main game loop - runs at 60 FPS
     */
    gameLoop(currentTime) {
        if (!this.gameRunning) return;
        
        // Calculate delta time
        const deltaTime = (currentTime - this.lastTime) / 1000; // Convert to seconds
        this.lastTime = currentTime;
        
        // Update game logic
        this.update(deltaTime);
        
        // Render frame
        this.render();
        
        // Request next frame
        requestAnimationFrame((time) => this.gameLoop(time));
    }

    /**
     * Update game logic
     */
    update(deltaTime) {
        if (this.game) {
            this.game.update(deltaTime);
        }
    }

    /**
     * Render current frame
     */
    render() {
        // Save context state
        this.ctx.save();
        
        // Clear canvas
        this.ctx.fillStyle = '#000000';
        this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
        
        // Scale context to maintain aspect ratio
        const scaleX = this.canvas.width / BASE_WIDTH;
        const scaleY = this.canvas.height / BASE_HEIGHT;
        const scale = Math.min(scaleX, scaleY);
        
        this.ctx.scale(scale, scale);
        
        // Render game
        if (this.game) {
            this.game.render();
        }
        
        // Restore context
        this.ctx.restore();
    }

    /**
     * Handle keyboard key down
     */
    handleKeyDown(e) {
        // Prevent default for game keys
        if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', ' ', 'Escape', 'Enter'].includes(e.key)) {
            e.preventDefault();
        }
        
        if (this.game) {
            this.game.handleKeyDown(e);
        }
    }

    /**
     * Handle keyboard key up
     */
    handleKeyUp(e) {
        if (this.game) {
            this.game.handleKeyUp(e);
        }
    }

    /**
     * Handle mouse move
     */
    handleMouseMove(e) {
        const rect = this.canvas.getBoundingClientRect();
        const x = (e.clientX - rect.left) / (rect.width / BASE_WIDTH);
        const y = (e.clientY - rect.top) / (rect.height / BASE_HEIGHT);
        
        // TODO: Handle mouse move based on current state
    }

    /**
     * Handle mouse down
     */
    handleMouseDown(e) {
        // TODO: Handle mouse down based on current state
    }

    /**
     * Handle mouse up
     */
    handleMouseUp(e) {
        // TODO: Handle mouse up based on current state
    }

    /**
     * Handle mouse click
     */
    handleClick(e) {
        const rect = this.canvas.getBoundingClientRect();
        const x = (e.clientX - rect.left) / (rect.width / BASE_WIDTH);
        const y = (e.clientY - rect.top) / (rect.height / BASE_HEIGHT);
        
        // TODO: Handle click based on current state
        console.log('Click at:', x, y);
    }

    /**
     * Handle mouse wheel
     */
    handleWheel(e) {
        e.preventDefault();
        
        // TODO: Handle wheel based on current state (for scrolling in menus)
        console.log('Wheel delta:', e.deltaY);
    }
}

// Initialize game when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        new Main();
    });
} else {
    new Main();
}

export default Main;
