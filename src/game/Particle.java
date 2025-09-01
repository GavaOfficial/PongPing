package game;

import static context.GameContext.*;

import java.awt.*;

// Particle class for visual effects
public class Particle {
    double x, y, vx, vy;
    int life, maxLife;
    Color color;
    boolean infinite;

    public Particle(double x, double y, double vx, double vy, int life, Color color) {
        this(x, y, vx, vy, life, color, false);
    }

    public Particle(double x, double y, double vx, double vy, int life, Color color, boolean infinite) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = this.maxLife = life;
        this.color = color;
        this.infinite = infinite;
    }

    // Reset method for object pooling
    public void reset(double x, double y, double vx, double vy, int life, Color color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = this.maxLife = life;
        this.color = color;
        this.infinite = false; // Fire particles are not infinite
    }

    public void update() {
        x += vx;
        y += vy;

        if (infinite) {
            // Rimbalza sui bordi per particelle infinite
            if (x <= 0 || x >= BOARD_HEIGHT) {
                vx = -vx;
                x = Math.max(0, Math.min(BOARD_WIDTH, x));
            }
            if (y <= 0 || y >= BOARD_HEIGHT) {
                vy = -vy;
                y = Math.max(0, Math.min(BOARD_HEIGHT, y));
            }
            // Mantieni velocità costante per particelle infinite
            double speed = Math.sqrt(vx * vx + vy * vy);
            if (speed > 0) {
                double targetSpeed = 1.5;
                vx = (vx / speed) * targetSpeed;
                vy = (vy / speed) * targetSpeed;
            }
        } else {
            // Comportamento normale per particelle temporanee
            vx *= 0.98;
            vy *= 0.98;
            life--;
        }
    }

    public void draw(Graphics2D g) {
        if (infinite) {
            // Particelle infinite sempre visibili
            g.setColor(color);
            g.fillOval((int)x - 2, (int)y - 2, 4, 4);
            // Aggiunge effetto glow
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 50));
            g.fillOval((int)x - 4, (int)y - 4, 8, 8);
        } else {
            // Particelle temporanee con fade
            float alpha = (float) life / maxLife;
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    (int)(alpha * color.getAlpha())));
            g.fillOval((int)x - 2, (int)y - 2, 4, 4);
        }
    }

    public boolean isDead() {
        return !infinite && life <= 0;
    }
}