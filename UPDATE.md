# PongPing - Update History

## Version 1.0.0 (Current) - Circle Mode & Achievement System

### 🎮 Circle Mode - Complete New Game Mode
- **Revolutionary circular gameplay** - Defend the center circle from balls coming from all directions
  - 360-degree paddle movement controlled by mouse
  - Dynamic ball spawning from all four edges (top, right, bottom, left)
  - Progressive difficulty scaling over time
  - 3-life system with game over on center hit

- **Progressive Ball Spawning System**
  - Time-based progression: 1 ball (0-3min) → 2 balls (3-5min) → 3 balls (5-10min) → 5 balls (10+min)
  - Intelligent spawn timing to prevent simultaneous opposite arrivals
  - Speed variation system (±50%) to stagger ball arrival times
  - 3-second opposite-edge spawn prevention window
  - Ball proximity detection (250px radius) to avoid clustering

- **Advanced Speed Mechanics**
  - Base speed increases progressively every 30 seconds (+0.5x multiplier, max 4.0x)
  - All balls maintain similar speeds at same time point
  - Small speed variation to prevent perfect synchronization
  - Max speed cap of 15.0 for late-game balance

- **Combo System & Spiral Frenzy**
  - Combo counter increases with consecutive deflections
  - Combo 100+ triggers **Spiral Frenzy Mode** for 10 seconds
  - Continuous spiral ball pattern during frenzy (50 max balls)
  - Spiral balls deal reduced damage (2 deflections = 1 normal ball)
  - Visual effects: pulsing combo display, color transitions, scale animations

- **Power-Up System**
  - **Slow-Mo**: Reduces ball speed by 50% for 8 seconds
  - **Paddle Enlarge**: Increases paddle arc size for 10 seconds
  - **Shield**: Protects from 1 center hit
  - **Health**: Restores 1 life (max 3)
  - Power-ups spawn every 15 seconds with visual indicators
  - Collision detection with center circle for activation

- **Window Expansion Feature**
  - Game window progressively expands from 800x600 to fullscreen over 120 seconds
  - Smooth animation with 60 FPS interpolation
  - Maintains aspect ratio and center positioning
  - Visual scale factor consistency throughout expansion

### 🏆 Achievement System (77+ Achievements)
- **Comprehensive Achievement Registry** organized in 5 categories:
  - **First Time** (13 achievements): First game, play time milestones, login streaks
  - **Circle Mode** (29 achievements): Balls deflected, combos, survival time, score
  - **Classic Mode** (23 achievements): Victories, AI difficulty, streaks, perfect games
  - **Mastery** (7 achievements): Advanced challenges, total deflections, completionist
  - **Special** (5 achievements): Secret and fun achievements

- **Tier System with XP Rewards**
  - Bronze: 50 XP
  - Silver: 100 XP
  - Gold: 200 XP
  - Platinum: 500 XP

- **Player Progression System**
  - XP accumulation from achievements
  - Level system with XP thresholds (Level 1: 0 XP, Level 2: 100 XP, scaling exponentially)
  - Separate statistics tracking for Circle Mode and Classic Mode
  - Mode-specific combo tracking (circleMaxCombo, classicMaxCombo)
  - Persistent progress saved in player data

- **Retro-Arcade Notification System**
  - 220x85px notifications in bottom-right corner
  - Slide-in from right, slide-out to bottom animations
  - 2-second display duration per notification
  - Queue system (one notification at a time)
  - Checkerboard background with CRT scanline effects
  - Pixel art icons (trophy, up arrow)
  - Tier-colored borders (Bronze, Silver, Gold, Platinum)
  - Multilingual support (Italian, English, Spanish)
  - Word-wrap for long descriptions (max 3 lines)

- **Achievement Interface (Advancement Screen)**
  - 4 tabs: Progress, Achievements, Unlocks, Statistics
  - Code editor aesthetic with line numbers and syntax highlighting
  - Real-time achievement tracking with unlock status
  - Detailed view with description panel (55%/45% split)
  - Dynamic scroll support with mouse wheel
  - Click detection with proper coordinate mapping
  - Achievement counter: "X/77 unlocked"

### 🔧 Technical Improvements & Bug Fixes
- **Mouse Click Detection Fix** (Advancement Screen)
  - Fixed offset calculation in achievement list click handler
  - Corrected separatorY calculation from hardcoded 170px to dynamic calculation
  - Aligned click bounds with visual rendering coordinates

- **Mode-Specific Statistics Tracking**
  - Separated combo tracking for Circle Mode and Classic Mode
  - Fixed bug where Circle Mode combos unlocked Classic Mode achievements
  - Added `circleMaxCombo` and `classicMaxCombo` fields to PlayerProgress
  - Updated achievement check methods for mode-specific validation

- **Spawn System Optimizations**
  - Enhanced opposite-edge prevention (5 reroll attempts, 3000ms window)
  - Ball proximity detection to avoid clustered spawns
  - Speed variation increased from ±10% to ±50%
  - Check radius expanded from 150px to 250px

- **Localization Enhancements**
  - Added 10+ new translation keys for Circle Mode
  - Notification translations in all 3 languages
  - Achievement names and descriptions fully localized
  - Dynamic text loading with fallback support

### 🎨 Visual & UI Enhancements
- **Circle Mode Graphics**
  - Glowing center circle with pulsing animation
  - Gradient paddle with arc rendering
  - Ball trail effects with fade-out
  - Health indicator with heart icons
  - Power-up visual indicators with countdown timers
  - Combo display with dynamic scaling and glow effects

- **Notification Styling**
  - Retro pixel art aesthetic (80s/90s arcade style)
  - 3px thick pixelated borders
  - Checkerboard tile background (3x3px tiles)
  - CRT scanlines (every 2 pixels)
  - XP display with gold text and shadow
  - Achievement tier color coding

- **Advancement Screen Design**
  - Dark code editor theme (#0A0F1A background)
  - Syntax-highlighted text (cyan functions, green comments)
  - Selection highlighting with glow effects
  - Line number column with separator
  - Tabbed navigation with active tab indicators

### 📊 Statistics & Data Persistence
- **Extended PlayerProgress Tracking**
  - Total play time across all modes
  - Mode-specific deflection counters
  - Login streak tracking with last login date
  - Max combo reached per mode
  - Total games played counter
  - Circle Mode specific: balls deflected, survival time, max score

- **Achievement Persistence**
  - Unlocked achievements list saved to disk
  - XP and level progression saved
  - Statistics automatically saved after each game
  - Cross-platform app data directory support

### 🎵 Audio System Updates
- **Circle Mode Sound Design**
  - Unique paddle hit sounds for deflections
  - Power-up activation sound effects
  - Score increase audio feedback
  - Damage/hit sounds for center impacts
  - Frenzy mode transition audio cues

## Version 0.8.0

### Major Code Refactoring and Architecture Optimization
- **Complete project restructuring** from monolithic single-class design to modular architecture
  - Split single `PongGame.java` file into organized package structure
  - Separated concerns into dedicated packages: `context/`, `settings/`, and `game/`
  - Created dedicated `Main.java` entry point with proper application initialization
  
### Context Management System
- **Implemented comprehensive context management** with specialized context classes
  - `GameContext`: Game state variables, dimensions, and runtime data
  - `FontContext`: Font loading and management system
  - `SettingsContext`: Configuration and user preferences
  - `LanguageContext`: Multi-language support and localization
  - `AIContext`: AI difficulty and behavior settings
  - `DimensionalContext`: Screen scaling and responsive design
  - `AnimationContext`: Animation states and transitions
  - `HistoryContext`: Game history and statistics tracking
  - `RankContext`: Player ranking and achievement system

### Settings Architecture Overhaul
- **Modular settings system** with dedicated classes for different setting categories
  - `GeneralSettings`: Core game configuration and controls
  - `MusicSettings`: Audio management and volume controls
  - `LanguageSettings`: Multi-language content loading
  - `HistorySettings`: Game statistics and history persistence
- **Enhanced settings persistence** with cross-platform app data storage
- **Dynamic theme and paddle selection** with real-time preview

### Resource Management Enhancement
- **Intelligent resource loading system** (`ContextLoader`)
  - Cross-platform resource path resolution for JAR and jpackage deployment
  - Dynamic asset enumeration from JAR files and file system
  - Robust fallback mechanisms for missing assets
  - Optimized font loading with proper registration
  - Advanced background and paddle theme loading with metadata extraction

### Application Architecture Improvements
- **Proper application lifecycle management**
  - Dedicated main class with system property configuration
  - Cross-platform application icon handling with multiple sizes
  - Proper window close handling with cleanup procedures
  - macOS-specific optimizations (dock integration, menu bar, dark mode support)

### Code Quality and Maintainability
- **Separation of concerns** with clear package boundaries
- **Static import optimization** for cleaner code organization
- **Enhanced error handling** with comprehensive fallback systems
- **Improved debugging output** with detailed loading information
- **Thread-safe resource access** and proper synchronization

### Performance Optimizations
- **Reduced memory footprint** through optimized resource loading
- **Lazy initialization** of non-critical components
- **Efficient asset caching** with proper lifecycle management
- **Optimized rendering pipeline** with context-aware drawing

## Version 0.7.5

### Build System Enhancements
- **Split GitHub Actions into separate workflows** (build.yml & release.yml)
- **Enhanced JDK setup and build process optimization**
- **Improved changelog extraction for releases**

## Version 0.7.4

### UI/UX Enhancements
- **Added developer signature** "by Gava" in bottom-left corner
  - Appears on all game screens (menu, settings, gameplay, pause, game over, background selection)
  - Uses Space Mono font with semi-transparent gray styling
  - Positioned consistently across all states for brand recognition

### Build System Fixes
- **Fixed macOS jpackage compatibility**
  - Removed problematic version parameter for macOS DMG generation
  - macOS now uses automatic version assignment to avoid build errors
  - Maintained version control for Windows and Linux builds

## Version 0.7.3

### GitHub Actions & CI/CD
- **Added comprehensive GitHub Actions workflow** (`build-app.yml`)
  - Automated building for Linux (DEB), macOS (DMG), and Windows (EXE)
  - Cross-platform JAR compilation and artifact generation
  - Multi-OS matrix build strategy with Ubuntu, macOS, and Windows runners
  - Automatic asset inclusion (fonts, themes, music, icons, languages)

### Installer Enhancements
- **Integrated MIT License** into all installer packages
  - Added `--license-file LICENSE` to jpackage commands
  - License automatically displayed during installation process
  - Applied to Windows EXE, macOS DMG, and Linux DEB installers

### Build System Improvements
- **Enhanced jpackage configuration**
  - Customized installer metadata (version, description, vendor)
  - Platform-specific icons (ICO for Windows, ICNS for macOS, PNG for Linux)
  - Windows: Added Start Menu integration and desktop shortcuts
  - Linux: Added application menu shortcuts in Games category
  - macOS: DMG packaging with proper app bundle structure

### Asset Management
- **Intelligent asset detection**
  - Dynamic music folder inclusion (only if contains files)
  - Comprehensive asset bundling (fonts, themes, icons, languages)
  - Cross-platform asset path handling

### Distribution Features
- **Multi-platform installer generation**
  - Windows: EXE installer with Start Menu and shortcut creation
  - macOS: DMG with drag-to-install experience
  - Linux: DEB package for Debian/Ubuntu systems
- **GitHub Actions artifact storage**
  - 30-day retention for all builds
  - Separate artifacts for JAR and platform-specific installers

### Project Structure
- **Added comprehensive documentation**
  - Enhanced CLAUDE.md with detailed project architecture
  - Complete build commands and development workflow
  - Detailed game state system and component documentation

## Version 0.7.2

### Theme System Enhancements
- **Background selection screen** with visual preview
- **Custom theme loading** from `temi/` directory
- **Image background support** (PNG/JPG) with fallback colors
- **Theme configuration files** (.txt) for color customization
- **Default themes**: Black, Furry, Natura with custom backgrounds

### Settings System Overhaul
- **Categorized settings menu** (Difficulty, Paddle, Controls, Audio)
- **Dynamic navigation** between categories with arrow keys
- **Real-time setting updates** with immediate visual feedback
- **Key binding customization** for all game controls
- **Audio volume controls** for music and sound effects

### Visual Improvements
- **Enhanced menu rendering** with better spacing and alignment
- **Improved gradient effects** on paddles and UI elements
- **Better text rendering** with shadow effects and custom fonts
- **Responsive UI scaling** that adapts to window size changes

## Version 0.7.1

### Audio System Implementation
- **Background music support** with WAV file loading
- **Real-time sound synthesis** for game events
- **Volume controls** in settings menu
- **Sound effects**: Paddle hits, wall bounces, scoring
- **Audio threading** for non-blocking sound playback

### Game Physics Refinements
- **Improved collision detection** with more accurate ball physics
- **Enhanced ball speed progression** with configurable limits
- **Better paddle responsiveness** with smooth movement
- **Realistic bounce angles** based on paddle hit position

### UI/UX Enhancements
- **Smooth state transitions** between game modes
- **Improved pause functionality** with overlay graphics
- **Better game over screen** with statistics display
- **Enhanced visual feedback** for user interactions

## Version 0.7.0

### Core Architecture Rewrite
- **State machine implementation** with 8 distinct game states
- **Modular rendering system** with state-specific draw methods
- **Enhanced input handling** with context-sensitive controls
- **Improved game loop** with consistent 60 FPS timing

### AI System Development
- **Predictive AI opponent** with configurable difficulty
- **5-level difficulty system** (Easy to Impossible)
- **Adaptive AI movement** with realistic reaction times
- **Smart ball prediction** with position forecasting

### Graphics System
- **Custom font integration** (Silkscreen, Space Mono)
- **Particle effect system** for collisions and visual flair
- **Gradient rendering** for paddles and UI elements
- **Screen shake effects** for impact feedback

### Game Features
- **Single-player mode** with AI opponent
- **Two-player local multiplayer**
- **Comprehensive settings system**
- **Pause/resume functionality**
- **Win condition tracking** (first to 7 points)

## Version 0.6.x Series

### Basic Game Implementation
- **Classic Pong gameplay** with two paddles and ball
- **Basic collision detection** and physics
- **Simple scoring system**
- **Window resizing support**
- **Keyboard input handling**

### Initial Features
- **Basic menu system**
- **Game state management** (Menu, Playing, Game Over)
- **Simple graphics rendering**
- **Fixed-speed ball movement**
- **Basic paddle controls**

## Version 0.5.x and Earlier

### Foundation Development
- **Initial Java/Swing setup**
- **Basic game window creation**
- **Simple object rendering**
- **Basic input system**
- **Core game loop implementation**

---

*PongPing is an enhanced classic Pong game featuring modern graphics, sound, and gameplay mechanics.*