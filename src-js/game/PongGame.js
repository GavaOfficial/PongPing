/**
 * PongGame - Main Game Class (Simplified Functional Version)
 * This is a simplified but functional version of the full Java PongGame.java (22k lines)
 * 
 * Full features to be implemented:
 * - Complete state machine for all 23 game states
 * - Full Circle Mode implementation
 * - Complete rendering for all UI states
 * - AI opponent with 5 difficulty levels
 * - Achievement tracking and unlocking
 * - Theme system
 * - Full animation system
 */

import { GameState } from './GameState.js';
import { Particle } from './Particle.js';
import { 
    BOARD_WIDTH, BOARD_HEIGHT, 
    PADDLE_WIDTH, PADDLE_HEIGHT, 
    BALL_SIZE 
} from '../context/GameContext.js';
import { currentState, setCurrentState, particles } from '../context/AnimationContext.js';
import { PlayerProgress } from '../advancement/PlayerProgress.js';

/**
 * PongGame class - Simplified functional version
 */
export class PongGame {
    constructor(canvas, ctx) {
        this.canvas = canvas;
        this.ctx = ctx;
        
        // Game state
        this.state = GameState.MENU;
        
        // Ball
        this.ballX = BOARD_WIDTH / 2;
        this.ballY = BOARD_HEIGHT / 2;
        this.ballVX = 5;
        this.ballVY = 5;
        
        // Paddles
        this.leftPaddleY = BOARD_HEIGHT / 2 - PADDLE_HEIGHT / 2;
        this.rightPaddleY = BOARD_HEIGHT / 2 - PADDLE_HEIGHT / 2;
        this.paddleSpeed = 8;
        
        // Scores
        this.leftScore = 0;
        this.rightScore = 0;
        this.winningScore = 10;
        
        // Input
        this.keys = {};
        
        // Menu
        this.selectedMenuItem = 0;
        this.menuItems = ['SINGLE PLAYER', 'TWO PLAYERS', 'SETTINGS', 'EXIT'];
        
        // Player progress
        this.playerProgress = PlayerProgress.load();
        
        console.log('PongGame initialized (simplified version)');
    }

    /**
     * Update game logic
     * @param {number} deltaTime - Time since last update in seconds
     */
    update(deltaTime) {
        switch (this.state) {
            case GameState.MENU:
                this.updateMenu();
                break;
            case GameState.PLAYING:
                this.updateGame(deltaTime);
                break;
            case GameState.GAME_OVER:
                // Wait for input to return to menu
                break;
        }
        
        // Update particles
        for (let i = particles.length - 1; i >= 0; i--) {
            particles[i].update();
            if (particles[i].isDead()) {
                particles.splice(i, 1);
            }
        }
    }

    /**
     * Update menu logic
     */
    updateMenu() {
        // Menu is static, waiting for input
    }

    /**
     * Update game logic
     * @param {number} deltaTime 
     */
    updateGame(deltaTime) {
        // Move ball
        this.ballX += this.ballVX;
        this.ballY += this.ballVY;
        
        // Ball collision with top/bottom
        if (this.ballY <= 0 || this.ballY >= BOARD_HEIGHT - BALL_SIZE) {
            this.ballVY = -this.ballVY;
            this.createParticles(this.ballX, this.ballY, '#FFFFFF');
        }
        
        // Ball collision with left paddle
        if (this.ballX <= PADDLE_WIDTH && 
            this.ballY >= this.leftPaddleY && 
            this.ballY <= this.leftPaddleY + PADDLE_HEIGHT) {
            this.ballVX = Math.abs(this.ballVX);
            this.createParticles(this.ballX, this.ballY, '#00FFFF');
        }
        
        // Ball collision with right paddle
        if (this.ballX >= BOARD_WIDTH - PADDLE_WIDTH - BALL_SIZE &&
            this.ballY >= this.rightPaddleY &&
            this.ballY <= this.rightPaddleY + PADDLE_HEIGHT) {
            this.ballVX = -Math.abs(this.ballVX);
            this.createParticles(this.ballX, this.ballY, '#FF00FF');
        }
        
        // Ball out of bounds - score
        if (this.ballX < 0) {
            this.rightScore++;
            this.resetBall();
            this.checkWin();
        } else if (this.ballX > BOARD_WIDTH) {
            this.leftScore++;
            this.resetBall();
            this.checkWin();
        }
        
        // Move paddles based on input
        if (this.keys['KeyW'] && this.leftPaddleY > 0) {
            this.leftPaddleY -= this.paddleSpeed;
        }
        if (this.keys['KeyS'] && this.leftPaddleY < BOARD_HEIGHT - PADDLE_HEIGHT) {
            this.leftPaddleY += this.paddleSpeed;
        }
        if (this.keys['ArrowUp'] && this.rightPaddleY > 0) {
            this.rightPaddleY -= this.paddleSpeed;
        }
        if (this.keys['ArrowDown'] && this.rightPaddleY < BOARD_HEIGHT - PADDLE_HEIGHT) {
            this.rightPaddleY += this.paddleSpeed;
        }
    }

    /**
     * Reset ball to center
     */
    resetBall() {
        this.ballX = BOARD_WIDTH / 2;
        this.ballY = BOARD_HEIGHT / 2;
        this.ballVX = (Math.random() > 0.5 ? 1 : -1) * 5;
        this.ballVY = (Math.random() - 0.5) * 10;
    }

    /**
     * Check if someone won
     */
    checkWin() {
        if (this.leftScore >= this.winningScore || this.rightScore >= this.winningScore) {
            this.state = GameState.GAME_OVER;
        }
    }

    /**
     * Create particle effects
     * @param {number} x 
     * @param {number} y 
     * @param {string} color 
     */
    createParticles(x, y, color) {
        for (let i = 0; i < 10; i++) {
            const angle = Math.random() * Math.PI * 2;
            const speed = Math.random() * 3 + 1;
            const particle = new Particle(
                x, y,
                Math.cos(angle) * speed,
                Math.sin(angle) * speed,
                30,
                color
            );
            particles.push(particle);
        }
    }

    /**
     * Render current frame
     */
    render() {
        const ctx = this.ctx;
        
        // Clear screen
        ctx.fillStyle = '#000000';
        ctx.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
        
        // Render based on state
        switch (this.state) {
            case GameState.MENU:
                this.renderMenu();
                break;
            case GameState.PLAYING:
                this.renderGame();
                break;
            case GameState.GAME_OVER:
                this.renderGameOver();
                break;
        }
        
        // Render particles
        particles.forEach(p => p.draw(ctx));
        
        // Render signature
        ctx.font = '16px "Space Mono", monospace';
        ctx.fillStyle = 'rgba(255, 255, 255, 0.3)';
        ctx.textAlign = 'left';
        ctx.fillText('by Gava', 20, BOARD_HEIGHT - 20);
    }

    /**
     * Render menu
     */
    renderMenu() {
        const ctx = this.ctx;
        
        // Title
        ctx.font = 'bold 64px Arial';
        ctx.fillStyle = '#FFFFFF';
        ctx.textAlign = 'center';
        ctx.fillText('PONG PING', BOARD_WIDTH / 2, 150);
        
        // Menu items
        ctx.font = '32px Arial';
        this.menuItems.forEach((item, index) => {
            const y = 250 + index * 60;
            
            if (index === this.selectedMenuItem) {
                ctx.fillStyle = '#00FF00';
                ctx.fillText('> ' + item + ' <', BOARD_WIDTH / 2, y);
            } else {
                ctx.fillStyle = '#FFFFFF';
                ctx.fillText(item, BOARD_WIDTH / 2, y);
            }
        });
        
        // Instructions
        ctx.font = '16px Arial';
        ctx.fillStyle = 'rgba(255, 255, 255, 0.6)';
        ctx.fillText('Use Arrow Keys to navigate, Enter to select', BOARD_WIDTH / 2, BOARD_HEIGHT - 80);
    }

    /**
     * Render game
     */
    renderGame() {
        const ctx = this.ctx;
        
        // Center line
        ctx.strokeStyle = '#FFFFFF';
        ctx.setLineDash([10, 10]);
        ctx.beginPath();
        ctx.moveTo(BOARD_WIDTH / 2, 0);
        ctx.lineTo(BOARD_WIDTH / 2, BOARD_HEIGHT);
        ctx.stroke();
        ctx.setLineDash([]);
        
        // Left paddle
        ctx.fillStyle = '#00FFFF';
        ctx.fillRect(0, this.leftPaddleY, PADDLE_WIDTH, PADDLE_HEIGHT);
        
        // Right paddle
        ctx.fillStyle = '#FF00FF';
        ctx.fillRect(BOARD_WIDTH - PADDLE_WIDTH, this.rightPaddleY, PADDLE_WIDTH, PADDLE_HEIGHT);
        
        // Ball
        ctx.fillStyle = '#FFFFFF';
        ctx.fillRect(this.ballX, this.ballY, BALL_SIZE, BALL_SIZE);
        
        // Scores
        ctx.font = '48px Arial';
        ctx.fillStyle = '#FFFFFF';
        ctx.textAlign = 'center';
        ctx.fillText(this.leftScore, BOARD_WIDTH / 4, 80);
        ctx.fillText(this.rightScore, BOARD_WIDTH * 3 / 4, 80);
        
        // Controls hint
        ctx.font = '14px Arial';
        ctx.fillStyle = 'rgba(255, 255, 255, 0.4)';
        ctx.textAlign = 'left';
        ctx.fillText('W/S', 10, 30);
        ctx.textAlign = 'right';
        ctx.fillText('↑/↓', BOARD_WIDTH - 10, 30);
    }

    /**
     * Render game over screen
     */
    renderGameOver() {
        const ctx = this.ctx;
        
        // Semi-transparent overlay
        ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
        ctx.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
        
        // Game Over text
        ctx.font = 'bold 64px Arial';
        ctx.fillStyle = '#FFFFFF';
        ctx.textAlign = 'center';
        ctx.fillText('GAME OVER', BOARD_WIDTH / 2, BOARD_HEIGHT / 2 - 50);
        
        // Winner
        const winner = this.leftScore > this.rightScore ? 'Left Player' : 'Right Player';
        ctx.font = '32px Arial';
        ctx.fillText(winner + ' Wins!', BOARD_WIDTH / 2, BOARD_HEIGHT / 2 + 20);
        
        // Final score
        ctx.font = '24px Arial';
        ctx.fillText(`${this.leftScore} - ${this.rightScore}`, BOARD_WIDTH / 2, BOARD_HEIGHT / 2 + 70);
        
        // Instructions
        ctx.font = '18px Arial';
        ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
        ctx.fillText('Press ENTER to return to menu', BOARD_WIDTH / 2, BOARD_HEIGHT / 2 + 120);
    }

    /**
     * Handle key down
     * @param {KeyboardEvent} e 
     */
    handleKeyDown(e) {
        this.keys[e.code] = true;
        
        if (this.state === GameState.MENU) {
            if (e.code === 'ArrowUp') {
                this.selectedMenuItem = Math.max(0, this.selectedMenuItem - 1);
            } else if (e.code === 'ArrowDown') {
                this.selectedMenuItem = Math.min(this.menuItems.length - 1, this.selectedMenuItem + 1);
            } else if (e.code === 'Enter') {
                this.selectMenuItem();
            }
        } else if (this.state === GameState.GAME_OVER) {
            if (e.code === 'Enter') {
                this.returnToMenu();
            }
        } else if (this.state === GameState.PLAYING) {
            if (e.code === 'Escape') {
                this.returnToMenu();
            }
        }
    }

    /**
     * Handle key up
     * @param {KeyboardEvent} e 
     */
    handleKeyUp(e) {
        this.keys[e.code] = false;
    }

    /**
     * Select menu item
     */
    selectMenuItem() {
        switch (this.selectedMenuItem) {
            case 0: // Single Player
            case 1: // Two Players
                this.startGame();
                break;
            case 2: // Settings
                alert('Settings not yet implemented in simplified version');
                break;
            case 3: // Exit
                alert('Thank you for playing!');
                break;
        }
    }

    /**
     * Start game
     */
    startGame() {
        this.state = GameState.PLAYING;
        this.leftScore = 0;
        this.rightScore = 0;
        this.resetBall();
        console.log('Game started!');
    }

    /**
     * Return to menu
     */
    returnToMenu() {
        this.state = GameState.MENU;
        this.selectedMenuItem = 0;
        console.log('Returned to menu');
    }
}
