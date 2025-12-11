/**
 * Demo Game
 * Manages demo animations and background effects
 * Simplified version for JavaScript
 */

import { Particle } from './Particle.js';
import { BOARD_WIDTH, BOARD_HEIGHT } from '../context/GameContext.js';

/**
 * DemoGame class - Background animations for menu
 */
export class DemoGame {
    constructor() {
        // Demo ball for background
        this.demoBallX = BOARD_WIDTH / 2;
        this.demoBallY = BOARD_HEIGHT / 2;
        this.demoBallVX = 3;
        this.demoBallVY = 3;
        this.demoBallSize = 15;
        
        // Demo particles
        this.demoParticles = [];
    }

    /**
     * Update demo animations
     */
    update() {
        // Move demo ball
        this.demoBallX += this.demoBallVX;
        this.demoBallY += this.demoBallVY;
        
        // Bounce off walls
        if (this.demoBallX <= 0 || this.demoBallX >= BOARD_WIDTH - this.demoBallSize) {
            this.demoBallVX = -this.demoBallVX;
            this.createDemoParticles();
        }
        if (this.demoBallY <= 0 || this.demoBallY >= BOARD_HEIGHT - this.demoBallSize) {
            this.demoBallVY = -this.demoBallVY;
            this.createDemoParticles();
        }
        
        // Update particles
        for (let i = this.demoParticles.length - 1; i >= 0; i--) {
            this.demoParticles[i].update();
            if (this.demoParticles[i].isDead()) {
                this.demoParticles.splice(i, 1);
            }
        }
    }

    /**
     * Render demo
     * @param {CanvasRenderingContext2D} ctx 
     */
    render(ctx) {
        // Render demo ball
        ctx.fillStyle = 'rgba(255, 255, 255, 0.3)';
        ctx.fillRect(this.demoBallX, this.demoBallY, this.demoBallSize, this.demoBallSize);
        
        // Render particles
        this.demoParticles.forEach(p => p.draw(ctx));
    }

    /**
     * Create demo particles
     */
    createDemoParticles() {
        for (let i = 0; i < 5; i++) {
            const angle = Math.random() * Math.PI * 2;
            const speed = Math.random() * 2 + 1;
            const particle = new Particle(
                this.demoBallX + this.demoBallSize / 2,
                this.demoBallY + this.demoBallSize / 2,
                Math.cos(angle) * speed,
                Math.sin(angle) * speed,
                20,
                'rgba(255, 255, 255, 0.5)'
            );
            this.demoParticles.push(particle);
        }
    }
}
