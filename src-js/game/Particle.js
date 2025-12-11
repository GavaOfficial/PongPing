/**
 * Particle class for visual effects
 */

// Import context (will be needed for BOARD_WIDTH, BOARD_HEIGHT)
import { BOARD_WIDTH, BOARD_HEIGHT } from '../context/GameContext.js';

export class Particle {
    constructor(x, y, vx, vy, life, color, infinite = false) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.maxLife = life;
        this.color = color;
        this.infinite = infinite;
    }

    /**
     * Reset method for object pooling
     */
    reset(x, y, vx, vy, life, color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.maxLife = life;
        this.color = color;
        this.infinite = false; // Fire particles are not infinite
    }

    update() {
        this.x += this.vx;
        this.y += this.vy;

        if (this.infinite) {
            // Bounce off edges for infinite particles
            if (this.x <= 0 || this.x >= BOARD_WIDTH) {
                this.vx = -this.vx;
                this.x = Math.max(0, Math.min(BOARD_WIDTH, this.x));
            }
            if (this.y <= 0 || this.y >= BOARD_HEIGHT) {
                this.vy = -this.vy;
                this.y = Math.max(0, Math.min(BOARD_HEIGHT, this.y));
            }
            // Maintain constant speed for infinite particles
            const speed = Math.sqrt(this.vx * this.vx + this.vy * this.vy);
            if (speed > 0) {
                const targetSpeed = 1.5;
                this.vx = (this.vx / speed) * targetSpeed;
                this.vy = (this.vy / speed) * targetSpeed;
            }
        } else {
            // Normal behavior for temporary particles
            this.vx *= 0.98;
            this.vy *= 0.98;
            this.life--;
        }
    }

    draw(ctx) {
        if (this.infinite) {
            // Infinite particles always visible
            ctx.fillStyle = this.color;
            ctx.fillRect(this.x - 2, this.y - 2, 4, 4);
            // Add glow effect
            const glowColor = this.hexToRgba(this.color, 0.2);
            ctx.fillStyle = glowColor;
            ctx.fillRect(this.x - 4, this.y - 4, 8, 8);
        } else {
            // Temporary particles with fade
            const alpha = this.life / this.maxLife;
            ctx.fillStyle = this.hexToRgba(this.color, alpha);
            ctx.fillRect(this.x - 2, this.y - 2, 4, 4);
        }
    }

    isDead() {
        return !this.infinite && this.life <= 0;
    }

    /**
     * Helper method to convert hex color to rgba
     */
    hexToRgba(hex, alpha) {
        // If already rgba, extract and modify alpha
        if (hex.startsWith('rgba')) {
            return hex.replace(/[\d.]+\)$/, `${alpha})`);
        }
        // If rgb, convert to rgba
        if (hex.startsWith('rgb')) {
            return hex.replace('rgb', 'rgba').replace(')', `, ${alpha})`);
        }
        // If hex, convert to rgba
        const r = parseInt(hex.slice(1, 3), 16);
        const g = parseInt(hex.slice(3, 5), 16);
        const b = parseInt(hex.slice(5, 7), 16);
        return `rgba(${r}, ${g}, ${b}, ${alpha})`;
    }
}
