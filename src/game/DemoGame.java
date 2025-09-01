package game;

import java.awt.*;

import static context.FontContext.primaryFont;
import static context.FontContext.secondaryFont;
import static context.GameContext.*;
import static context.GameContext.BOARD_HEIGHT;
import static context.SettingsContext.*;

public class DemoGame extends PongGame{


    private void drawDemoMode(Graphics2D g) {
        double easeProgress = easeInOutQuad(demoTransitionProgress);

        // Draw elements transitioning from normal settings
        drawTransitioningElements(g, easeProgress);

        // Draw demo paddle on the left (blue paddle transitioning)
        drawDemoPaddle(g, easeProgress);


        // Draw demo ball
        drawDemoBall(g);

        // Demo instructions
        drawDemoInstructions(g);
    }

    private void drawDemoPaddle(Graphics2D g, double progress) {
        // Blue paddle animates between horizontal position and demo position with rotation
        // For reverse transition, we only show the paddle during the transition

        if (isTransitioningFromDemo && progress <= 0.0) {
            return; // Don't show paddle when back to normal settings
        }

        // Start position: blue horizontal paddle position (from left side of horizontal paddles)
        int startX = (int)(15 * scaleX);
        int startY = BOARD_HEIGHT - (int)(60 * scaleY) - (int)(40 * scaleY);
        int startWidth = BOARD_WIDTH / 2 - (int)(30 * scaleX);
        int startHeight = (int)(60 * scaleY);

        // End position: exact same as game paddle blue (left paddle)
        int endX = (int)(20 * scaleX); // Same X position as game left paddle
        int endY = (int)demoPaddleY;

        // Interpolate position
        int currentX = (int)(startX + (endX - startX) * progress);
        int currentY = (int)(startY + (endY - startY) * progress);

        // Target dimensions: exact same as game paddle (no rotation in dimensions)
        int targetWidth = PADDLE_WIDTH;   // Same width as game paddle
        int targetHeight = PADDLE_HEIGHT; // Same height as game paddle

        // Interpolate dimensions to reach exact game paddle size
        int currentWidth = (int)(startWidth + (targetWidth - startWidth) * progress);
        int currentHeight = (int)(startHeight + (targetHeight - startHeight) * progress);

        // Draw paddle without rotation (simple transition with position and size changes only)
        Color paddleColor = new Color(100, 150, 255);
        Color gradientColor = new Color(150, 200, 255);

        GradientPaint paddleGradient = new GradientPaint(
                currentX, currentY, paddleColor,
                currentX + currentWidth, currentY + currentHeight, gradientColor);
        g.setPaint(paddleGradient);

        int cornerRadius = Math.max(4, currentWidth / 4);
        g.fillRoundRect(currentX, currentY, currentWidth, currentHeight, cornerRadius, cornerRadius);

        // Paddle glow
        g.setColor(generalSettings.getPaddleGlowColor(true));
        g.drawRoundRect(currentX - 2, currentY - 2, currentWidth + 4, currentHeight + 4, cornerRadius + 2, cornerRadius + 2);
    }


    private void drawDemoBall(Graphics2D g) {
        // Draw ball with same style as game ball
        int ballX = (int)demoBallX;
        int ballY = (int)demoBallY;

        // Use scaled ball size like in the game
        int scaledBallSize = BALL_SIZE; // Same scaling as game ball

        // Ball glow
        g.setColor(new Color(255, 255, 255, 30));
        g.fillOval(ballX - 3, ballY - 3, scaledBallSize + 6, scaledBallSize + 6);

        // Ball gradient
        Color ballCenter = new Color(255, 255, 255);
        Color ballEdge = new Color(200, 200, 255);

        GradientPaint ballGradient = new GradientPaint(
                ballX, ballY, ballCenter,
                ballX + scaledBallSize, ballY + scaledBallSize, ballEdge);
        g.setPaint(ballGradient);

        g.fillOval(ballX, ballY, scaledBallSize, scaledBallSize);

        // Ball highlight
        g.setColor(new Color(255, 255, 255, 200));
        int highlightSize = scaledBallSize / 3;
        g.fillOval(ballX + highlightSize/2, ballY + highlightSize/2,
                highlightSize, highlightSize);
    }

    private void drawDemoInstructions(Graphics2D g) {
        g.setColor(Color.WHITE);
        float instructSize = (float)(24 * Math.min(scaleX, scaleY)); // Large white text
        g.setFont(primaryFont.deriveFont(instructSize));
        FontMetrics instructFm = g.getFontMetrics();

        String instruct = getText("SETTINGS_PRESS_ENTER_CONTINUE");
        int instructX = (BOARD_WIDTH - instructFm.stringWidth(instruct)) / 2;
        int instructY = (int)(BOARD_HEIGHT - 40 * scaleY);
        g.drawString(instruct, instructX, instructY);
    }

    void drawTransitioningElements(Graphics2D g, double progress) {
        // Title transitions from center "IMPOSTAZIONI" to right "TEST VELOCITÀ"
        drawTransitioningTitle(g, progress);

        // Setting cards move from center to right panel
        drawTransitioningSettingCards(g, progress);

        // Red paddle (right paddle) stays in place and remains visible
        drawTransitioningRedPaddle(g, progress);

        // Blue paddle transitions with rotation (shown during both directions)
        drawDemoPaddle(g, progress);
    }

    private void drawTransitioningTitle(Graphics2D g, double progress) {
        g.setColor(Color.WHITE);

        // Title size transitions from larger to smaller
        float startSize = (float)(36 * Math.min(scaleX, scaleY));
        float endSize = (float)(28 * Math.min(scaleX, scaleY));
        float currentSize = startSize + (endSize - startSize) * (float)progress;
        g.setFont(primaryFont.deriveFont(currentSize));
        FontMetrics fm = g.getFontMetrics();

        // Title text changes gradually
        String startTitle = "IMPOSTAZIONI";
        String endTitle = "VELOCITÀ PADDLE";
        String currentTitle = progress < 0.5 ? startTitle : endTitle;

        // Calculate right panel position to align horizontally with selectors
        int panelWidth = (int)(BOARD_WIDTH * 0.4);
        int panelStartX = BOARD_WIDTH - panelWidth;
        int currentPanelX = (int)(BOARD_WIDTH + (panelStartX - BOARD_WIDTH) * progress);

        // Position transitions from center to aligned with right panel
        int startX = (BOARD_WIDTH - fm.stringWidth(currentTitle)) / 2;
        int endX = currentPanelX + (panelWidth - fm.stringWidth(currentTitle)) / 2; // Centered in right panel
        int currentX = (int)(startX + (endX - startX) * progress);

        // Y position remains at original height
        int currentY = (int)(80 * scaleY); // Keep original IMPOSTAZIONI height

        g.drawString(currentTitle, currentX, currentY);
    }

    private void drawTransitioningSettingCards(Graphics2D g, double progress) {
        // Setting cards move from center to right panel
        int panelWidth = (int)(BOARD_WIDTH * 0.4);
        int panelStartX = BOARD_WIDTH - panelWidth;
        int currentPanelX = (int)(BOARD_WIDTH + (panelStartX - BOARD_WIDTH) * progress);

        // Draw both cards in their transitioning positions
        drawTransitioningCard(g, 0, progress, currentPanelX, panelWidth); // Paddle speed
        drawTransitioningCard(g, 1, progress, currentPanelX, panelWidth); // AI difficulty
    }

    private void drawTransitioningCard(Graphics2D g, int cardType, double progress, int panelX, int panelWidth) {
        String[] options = cardType == 0 ? paddleSpeedOptions : aiDifficultyOptions;
        int currentValue = cardType == 0 ? paddleSpeedSetting : aiDifficultySetting;
        String title = cardType == 0 ? "Velocità Paddle" : "Difficoltà IA";

        // Calculate card positions - transitioning from normal settings position to demo position
        int cardWidth = (int)(300 * Math.min(scaleX, scaleY)); // Keep original size
        int cardHeight = (int)(120 * Math.min(scaleX, scaleY)); // Keep original size
        int normalCardX = (BOARD_WIDTH - cardWidth) / 2;
        int cardY = (int)(150 * scaleY) + cardType * (int)(150 * scaleY); // Original Y positions from drawSettingCard

        // Demo position (same size, in right panel, keeping original Y positions)
        int demoCardX = panelX + (panelWidth - cardWidth) / 2;

        // Only X position changes, size and Y remain constant
        int currentX = (int)(normalCardX + (demoCardX - normalCardX) * progress);
        int currentY = cardY; // Y position remains constant
        int currentWidth = cardWidth; // Size remains constant
        int currentHeight = cardHeight; // Size remains constant

        // Card background (same style as original settings) - always selected in demo mode
        boolean isSelected = isDemoMode ? true : (cardType == 0 && selectedSetting == 0) || (cardType == 1 && selectedSetting == 1);
        Color cardBg = isSelected ? new Color(40, 40, 50) : new Color(25, 25, 30);
        g.setColor(cardBg);
        g.fillRoundRect(currentX, currentY, currentWidth, currentHeight, 12, 12); // Same border radius as original

        // Card border (same style as original settings)
        if (isSelected) {
            g.setColor(new Color(100, 150, 255));
            g.drawRoundRect(currentX, currentY, currentWidth, currentHeight, 12, 12);
        }

        // Title (exactly same style as original settings)
        g.setColor(isSelected ? new Color(150, 200, 255) : new Color(180, 180, 180));
        float titleSize = (float)(18 * Math.min(scaleX, scaleY)); // Exact original size
        g.setFont(secondaryFont.deriveFont(titleSize));
        FontMetrics titleFm = g.getFontMetrics();
        int titleX = currentX + (currentWidth - titleFm.stringWidth(title)) / 2;
        int titleY = currentY + (int)(30 * scaleY); // Same as original
        g.drawString(title, titleX, titleY);

        // Current value (exactly same style as original settings)
        String valueText = options[currentValue];
        float valueSize = (float)(28 * Math.min(scaleX, scaleY)); // Exact original size
        FontMetrics valueFm = g.getFontMetrics(primaryFont.deriveFont(valueSize));
        int valueX = currentX + (currentWidth - valueFm.stringWidth(valueText)) / 2;
        int valueY = currentY + (int)(75 * scaleY); // Same as original

        // Use special drawing for AI difficulty
        if (cardType == 1) {
            drawDifficultyText(g, valueText, valueX, valueY, valueSize, currentValue);
        } else {
            g.setColor(Color.WHITE); // Exact original color
            g.setFont(primaryFont.deriveFont(valueSize));
            g.drawString(valueText, valueX, valueY);
        }

        // Navigation arrows (only for paddle speed card)
        if (isSelected && cardType == 0) {
            g.setColor(new Color(100, 150, 255)); // No fade, same as original
            float arrowSize = (float)(24 * Math.min(scaleX, scaleY)); // Keep original size
            g.setFont(primaryFont.deriveFont(arrowSize));

            if (currentValue > 0) {
                g.drawString("<", currentX + (int)(20 * scaleX), valueY); // Same position as original
            }
            if (currentValue < options.length - 1) {
                g.drawString(">", currentX + currentWidth - (int)(30 * scaleX), valueY); // Same position as original
            }
        }

        // Add instruction text for both cards
        if (isSelected) {
            g.setColor(new Color(120, 160, 220));
            float instructSize = (float)(12 * Math.min(scaleX, scaleY));
            g.setFont(secondaryFont.deriveFont(instructSize));
            FontMetrics instructFm = g.getFontMetrics();

            String instruction = cardType == 0 ? getText("SETTINGS_PRESS_ARROWS_PADDLE") : getText("SETTINGS_PRESS_SPACE_CHANGE");
            int instructX = currentX + (currentWidth - instructFm.stringWidth(instruction)) / 2;
            int instructY = valueY + (int)(25 * scaleY); // Below the arrows/value

            g.drawString(instruction, instructX, instructY);
        }
    }

    private void drawTransitioningRedPaddle(Graphics2D g, double progress) {
        // Red paddle transitions from horizontal position to demo position (like blue paddle)

        // Start position: red horizontal paddle position (from right side of horizontal paddles)
        int startX = BOARD_WIDTH / 2 + (int)(15 * scaleX);
        int startY = BOARD_HEIGHT - (int)(60 * scaleY) - (int)(40 * scaleY);
        int startWidth = BOARD_WIDTH / 2 - (int)(30 * scaleX);
        int startHeight = (int)(60 * scaleY);

        // Calculate end position: before the AI difficulty card in right panel
        int panelWidth = (int)(BOARD_WIDTH * 0.4);
        int panelStartX = BOARD_WIDTH - panelWidth;
        int currentPanelX = (int)(BOARD_WIDTH + (panelStartX - BOARD_WIDTH) * progress);

        // End position: before the AI difficulty card
        int endX = currentPanelX - PADDLE_WIDTH - (int)(10 * scaleX);
        int endY = (int)demoRedPaddleY;

        // Target dimensions: exact same as game paddle
        int targetWidth = PADDLE_WIDTH;
        int targetHeight = PADDLE_HEIGHT;

        // Interpolate position
        int currentX = (int)(startX + (endX - startX) * progress);
        int currentY = (int)(startY + (endY - startY) * progress);

        // Interpolate dimensions to reach exact game paddle size
        int currentWidth = (int)(startWidth + (targetWidth - startWidth) * progress);
        int currentHeight = (int)(startHeight + (targetHeight - startHeight) * progress);

        // Red paddle color (based on selection state)
        boolean rightSelected = selectedSetting == 1;
        Color baseColor = rightSelected ? new Color(255, 100, 100) : new Color(150, 60, 60);
        Color gradientColor = rightSelected ? new Color(255, 150, 150) : new Color(200, 100, 100);

        GradientPaint paddleGradient = new GradientPaint(
                currentX, currentY, baseColor,
                currentX + currentWidth, currentY + currentHeight, gradientColor);
        g.setPaint(paddleGradient);

        int cornerRadius = Math.max(4, currentWidth / 4);
        g.fillRoundRect(currentX, currentY, currentWidth, currentHeight, cornerRadius, cornerRadius);

        // Paddle glow
        g.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 100));
        g.drawRoundRect(currentX - 2, currentY - 2, currentWidth + 4, currentHeight + 4, cornerRadius + 2, cornerRadius + 2);
    }



    private void drawDemoElements(Graphics2D g, double progress) {
        // Calculate right panel position
        int panelWidth = (int)(BOARD_WIDTH * 0.4); // 40% of screen width
        int panelStartX = BOARD_WIDTH - panelWidth;
        int currentPanelX = (int)(BOARD_WIDTH + (panelStartX - BOARD_WIDTH) * progress);

        // Draw both setting cards in right panel
        drawDemoCard(g, 0, currentPanelX, panelWidth, progress); // Paddle speed
        drawDemoCard(g, 1, currentPanelX, panelWidth, progress); // AI difficulty

        // Draw horizontal paddles that move to right panel
        drawDemoHorizontalPaddles(g, currentPanelX, panelWidth, progress);
    }

    private void drawDemoCard(Graphics2D g, int cardType, int panelX, int panelWidth, double progress) {
        String[] options = cardType == 0 ? paddleSpeedOptions : aiDifficultyOptions;
        int currentValue = cardType == 0 ? paddleSpeedSetting : aiDifficultySetting;
        String title = cardType == 0 ? "Velocità Paddle" : "Difficoltà IA";

        // Card position in right panel
        int cardWidth = (int)(250 * Math.min(scaleX, scaleY));
        // Make AI difficulty card taller to accommodate extra text
        int cardHeight = cardType == 1 ? (int)(110 * Math.min(scaleX, scaleY)) : (int)(80 * Math.min(scaleX, scaleY));
        int cardX = panelX + (panelWidth - cardWidth) / 2;
        int cardY = (int)(120 * scaleY) + cardType * (int)(100 * scaleY);

        // Only draw if visible
        if (cardX < BOARD_WIDTH) {
            // Card background - always selected in demo mode
            boolean isSelected = true; // Both cards always appear selected in demo
            Color cardBg = new Color(40, 40, 50);
            g.setColor(cardBg);
            g.fillRoundRect(cardX, cardY, cardWidth, cardHeight, 12, 12);

            // Card border - always show border in demo
            g.setColor(new Color(100, 150, 255));
            g.drawRoundRect(cardX, cardY, cardWidth, cardHeight, 12, 12);

            // Title
            g.setColor(isSelected ? new Color(150, 200, 255) : new Color(180, 180, 180));
            float titleSize = (float)(14 * Math.min(scaleX, scaleY));
            g.setFont(secondaryFont.deriveFont(titleSize));
            FontMetrics titleFm = g.getFontMetrics();
            int titleX = cardX + (cardWidth - titleFm.stringWidth(title)) / 2;
            int titleY = cardY + (int)(20 * scaleY);
            g.drawString(title, titleX, titleY);

            // Current value
            String valueText = options[currentValue];
            float valueSize = (float)(20 * Math.min(scaleX, scaleY));
            FontMetrics valueFm = g.getFontMetrics(primaryFont.deriveFont(valueSize));
            int valueX = cardX + (cardWidth - valueFm.stringWidth(valueText)) / 2;
            int valueY = cardY + (int)(50 * scaleY);

            // Use special drawing for AI difficulty
            if (cardType == 1) {
                drawDifficultyText(g, valueText, valueX, valueY, valueSize, currentValue);
            } else {
                g.setColor(Color.WHITE);
                g.setFont(primaryFont.deriveFont(valueSize));
                g.drawString(valueText, valueX, valueY);
            }

            // Navigation arrows (only for selected and only for paddle speed card)
            if (isSelected && cardType == 0) {
                g.setColor(new Color(100, 150, 255));
                float arrowSize = (float)(16 * Math.min(scaleX, scaleY));
                g.setFont(primaryFont.deriveFont(arrowSize));

                if (currentValue > 0) {
                    g.drawString("<", cardX + (int)(10 * scaleX), valueY);
                }
                if (currentValue < options.length - 1) {
                    g.drawString(">", cardX + cardWidth - (int)(20 * scaleX), valueY);
                }
            }

            // Add instruction text for both cards
            g.setColor(new Color(150, 180, 255));
            float instructSize = (float)(12 * Math.min(scaleX, scaleY));
            g.setFont(secondaryFont.deriveFont(instructSize));
            FontMetrics instructFm = g.getFontMetrics();

            String instruction = cardType == 0 ? getText("SETTINGS_PRESS_ARROWS_PADDLE") : getText("SETTINGS_PRESS_SPACE_CHANGE");
            int instructX = cardX + (cardWidth - instructFm.stringWidth(instruction)) / 2;
            int instructY = cardY + cardHeight - (int)(15 * scaleY); // Position near bottom of card

            g.drawString(instruction, instructX, instructY);
        }
    }

    private void drawDemoHorizontalPaddles(Graphics2D g, int panelX, int panelWidth, double progress) {
        // Paddle dimensions
        int paddleWidth = panelWidth / 2 - (int)(15 * scaleX);
        int paddleHeight = (int)(40 * scaleY);
        int paddleY = BOARD_HEIGHT - paddleHeight - (int)(40 * scaleY);

        int leftPaddleX = panelX + (int)(10 * scaleX);
        int rightPaddleX = panelX + panelWidth / 2 + (int)(5 * scaleX);

        // Only draw if visible
        if (leftPaddleX < BOARD_WIDTH) {
            // Left paddle (blue)
            boolean leftSelected = selectedSetting == 0;
            Color leftColor = leftSelected ? new Color(100, 150, 255) : new Color(60, 90, 150);
            g.setColor(leftColor);
            g.fillRoundRect(leftPaddleX, paddleY, paddleWidth, paddleHeight, 8, 8);

            // Right paddle (red) - only if there's space
            if (rightPaddleX + paddleWidth <= BOARD_WIDTH) {
                boolean rightSelected = selectedSetting == 1;
                Color rightColor = rightSelected ? new Color(255, 100, 100) : new Color(150, 60, 60);
                g.setColor(rightColor);
                g.fillRoundRect(rightPaddleX, paddleY, paddleWidth, paddleHeight, 8, 8);
            }
        }
    }

    void updateDemoBall() {
        // Move ball
        demoBallX += demoBallVX;
        demoBallY += demoBallVY;

        // Bounce off top and bottom walls
        if (demoBallY <= 0) {
            demoBallY = 0;
            demoBallVY = -demoBallVY;
        }
        if (demoBallY >= BOARD_HEIGHT - BALL_SIZE) {
            demoBallY = BOARD_HEIGHT - BALL_SIZE;
            demoBallVY = -demoBallVY;
        }

        // Calculate paddle positions
        double progress = demoTransitionProgress;
        int startX = (int)(15 * scaleX);
        int endX = (int)(20 * scaleX);
        int bluePaddleX = (int)(startX + (endX - startX) * progress);

        int panelWidth = (int)(BOARD_WIDTH * 0.4);
        int panelStartX = BOARD_WIDTH - panelWidth;
        int currentPanelX = (int)(BOARD_WIDTH + (panelStartX - BOARD_WIDTH) * progress);
        int redPaddleX = currentPanelX - PADDLE_WIDTH - (int)(10 * scaleX);

        // Check if ball passes through paddles (reset ball)
        boolean ballPassedBlue = (demoBallX < bluePaddleX - BALL_SIZE && demoBallVX < 0);
        boolean ballPassedRed = (demoBallX > redPaddleX + PADDLE_WIDTH && demoBallVX > 0);

        if (ballPassedBlue || ballPassedRed) {
            // Reset ball to center between paddles
            initializeDemoBall();
        }

        // Paddle collisions
        if (demoBallX <= bluePaddleX + PADDLE_WIDTH &&
                demoBallX + BALL_SIZE >= bluePaddleX &&
                demoBallY + BALL_SIZE >= demoPaddleY &&
                demoBallY <= demoPaddleY + PADDLE_HEIGHT &&
                demoBallVX < 0) {

            demoBallVX = -demoBallVX;
            demoBallX = bluePaddleX + PADDLE_WIDTH + 1;

            int paddleCenter = (int)demoPaddleY + PADDLE_HEIGHT / 2;
            int ballCenter = (int)demoBallY + BALL_SIZE / 2;
            int diff = ballCenter - paddleCenter;
            demoBallVY += diff / 15.0;
        }

        if (demoBallX + BALL_SIZE >= redPaddleX &&
                demoBallX <= redPaddleX + PADDLE_WIDTH &&
                demoBallY + BALL_SIZE >= demoRedPaddleY &&
                demoBallY <= demoRedPaddleY + PADDLE_HEIGHT &&
                demoBallVX > 0) {

            demoBallVX = -demoBallVX;
            demoBallX = redPaddleX - BALL_SIZE - 1;

            int paddleCenter = (int)demoRedPaddleY + PADDLE_HEIGHT / 2;
            int ballCenter = (int)demoBallY + BALL_SIZE / 2;
            int diff = ballCenter - paddleCenter;
            demoBallVY += diff / 15.0;
        }
    }

    private void initializeDemoBall() {
        // Calculate paddle positions
        double progress = demoTransitionProgress;
        int startX = (int)(15 * scaleX);
        int endX = (int)(20 * scaleX);
        int bluePaddleX = (int)(startX + (endX - startX) * progress);

        int panelWidth = (int)(BOARD_WIDTH * 0.4);
        int panelStartX = BOARD_WIDTH - panelWidth;
        int currentPanelX = (int)(BOARD_WIDTH + (panelStartX - BOARD_WIDTH) * progress);
        int redPaddleX = currentPanelX - PADDLE_WIDTH - (int)(10 * scaleX);

        // Position ball at center between paddles
        int centerX = (bluePaddleX + PADDLE_WIDTH + redPaddleX) / 2;
        demoBallX = centerX - BALL_SIZE / 2;
        demoBallY = BOARD_HEIGHT / 2 - BALL_SIZE / 2;

        // Apply ball speed setting to demo ball too (scaled down for demo)
        double baseSpeed = (ballSpeedSetting / 20.0) * 4.0; // Scale numeric value

        demoBallVX = (Math.random() > 0.5) ? baseSpeed : -baseSpeed;
        demoBallVY = (Math.random() - 0.5) * baseSpeed;
    }

    void updateDemoAI() {
        // Use selected AI difficulty setting for demo
        switch (aiDifficultySetting) {
            case 0: // FACILE - Simple following with mistakes
                updateDemoAI_Easy();
                break;
            case 1: // NORMALE - Predictive with reaction delay
                updateDemoAI_Normal();
                break;
            case 2: // DIFFICILE - Advanced prediction with minimal errors
                updateDemoAI_Hard();
                break;
            case 3: // ESPERTO - Very strong AI
                updateDemoAI_Expert();
                break;
            case 4: // IMPOSSIBILE - Nearly perfect AI
                updateDemoAI_Impossible();
                break;
            default:
                updateDemoAI_Normal();
        }

        // Keep AI paddle within bounds
        demoRedPaddleY = Math.max(0, Math.min(demoRedPaddleY, BOARD_HEIGHT - PADDLE_HEIGHT));
    }

    private void updateDemoAI_Easy() {
        // Easy: Simple ball following with mistakes
        double ballCenterY = demoBallY + BALL_SIZE / 2;
        double paddleCenterY = demoRedPaddleY + PADDLE_HEIGHT / 2;

        // Get player paddle speed setting
        double baseSpeed = 4.0; // Lenta
        if (paddleSpeedSetting == 1) baseSpeed = 6.0; // Media
        else if (paddleSpeedSetting == 2) baseSpeed = 8.0; // Veloce
        double playerSpeed = Math.max(3, (int)(baseSpeed * scaleY)); // Same as player paddle

        if (demoBallVX > 0 && demoBallX > BOARD_WIDTH * 0.5) {
            // 25% chance to make a mistake
            if (Math.random() < 0.25) return;

            // Simple following with error
            double error = (Math.random() - 0.5) * 50;
            double targetY = ballCenterY + error - PADDLE_HEIGHT / 2;

            // Much slower than player (20% of player speed)
            double diff = targetY - demoRedPaddleY;
            double maxSpeed = playerSpeed * 0.2;
            double moveAmount = Math.signum(diff) * Math.min(maxSpeed, Math.abs(diff) * 0.4);

            demoRedPaddleY += moveAmount;
        } else {
            // Return to center slowly
            double centerY = BOARD_HEIGHT / 2 - PADDLE_HEIGHT / 2;
            double diff = centerY - demoRedPaddleY;
            demoRedPaddleY += Math.signum(diff) * Math.min(playerSpeed * 0.1, Math.abs(diff) * 0.1);
        }
    }

    private void updateDemoAI_Normal() {
        // Medium: Predictive movement with some errors
        double ballCenterY = demoBallY + BALL_SIZE / 2;

        if (demoBallVX > 0 && demoBallX > BOARD_WIDTH * 0.3) {
            // Calculate demo paddle positions
            double progress = demoTransitionProgress;
            int panelWidth = (int)(BOARD_WIDTH * 0.4);
            int panelStartX = BOARD_WIDTH - panelWidth;
            int currentPanelX = (int)(BOARD_WIDTH + (panelStartX - BOARD_WIDTH) * progress);
            int redPaddleX = currentPanelX - PADDLE_WIDTH - (int)(10 * scaleX);

            // Simple prediction
            double timeToReach = (redPaddleX - demoBallX) / demoBallVX;
            double predictedY = demoBallY + demoBallVY * timeToReach;

            // Add some error
            double error = (Math.random() - 0.5) * 40;
            double targetY = predictedY + error + BALL_SIZE / 2 - PADDLE_HEIGHT / 2;

            // 12% chance of bigger mistake
            if (Math.random() < 0.12) {
                targetY += (Math.random() - 0.5) * 80;
            }

            // Get player paddle speed setting
            double baseSpeed = 4.0; // Lenta
            if (paddleSpeedSetting == 1) baseSpeed = 6.0; // Media
            else if (paddleSpeedSetting == 2) baseSpeed = 8.0; // Veloce
            double playerSpeed = baseSpeed * scaleY;

            // Same speed as player (60% of player speed for balance)
            double diff = targetY - demoRedPaddleY;
            double maxSpeed = playerSpeed * 0.6;
            double moveAmount = Math.signum(diff) * Math.min(maxSpeed, Math.abs(diff) * 0.6);

            demoRedPaddleY += moveAmount;
        } else {
            // Return to center
            double centerY = BOARD_HEIGHT / 2 - PADDLE_HEIGHT / 2;
            double diff = centerY - demoRedPaddleY;
            // Get player paddle speed setting
            double baseSpeed = 4.0; // Lenta
            if (paddleSpeedSetting == 1) baseSpeed = 6.0; // Media
            else if (paddleSpeedSetting == 2) baseSpeed = 8.0; // Veloce
            double playerSpeed = baseSpeed * scaleY;
            demoRedPaddleY += Math.signum(diff) * Math.min(playerSpeed * 0.25, Math.abs(diff) * 0.2);
        }
    }

    private void updateDemoAI_Hard() {
        // Hard: Advanced prediction with minimal errors
        // Get player paddle speed setting
        double baseSpeed = 4.0; // Lenta
        if (paddleSpeedSetting == 1) baseSpeed = 6.0; // Media
        else if (paddleSpeedSetting == 2) baseSpeed = 8.0; // Veloce
        double playerSpeed = Math.max(3, (int)(baseSpeed * scaleY)); // Same as player paddle

        if (demoBallVX > 0) {
            // Advanced trajectory calculation for demo
            double predictionY = calculateDemoBallTrajectory();

            // Very small error - only 3% chance
            if (Math.random() < 0.03) {
                predictionY += (Math.random() - 0.5) * 30;
            }

            double targetY = predictionY - PADDLE_HEIGHT / 2;

            // Slightly faster than player (80% of player speed)
            double diff = targetY - demoRedPaddleY;
            double maxSpeed = playerSpeed * 0.8;
            double moveAmount = Math.signum(diff) * Math.min(maxSpeed, Math.abs(diff) * 0.8);

            demoRedPaddleY += moveAmount;
        } else {
            // Strategic positioning
            double strategicY = BOARD_HEIGHT * 0.5 - PADDLE_HEIGHT / 2;
            double diff = strategicY - demoRedPaddleY;
            demoRedPaddleY += Math.signum(diff) * Math.min(playerSpeed * 0.3, Math.abs(diff) * 0.3);
        }
    }

    private double calculateDemoBallTrajectory() {
        // Simulate ball trajectory for demo
        double simBallX = demoBallX;
        double simBallY = demoBallY;
        double simBallVX = demoBallVX;
        double simBallVY = demoBallVY;

        // Calculate demo paddle positions
        double progress = demoTransitionProgress;
        int panelWidth = (int)(BOARD_WIDTH * 0.4);
        int panelStartX = BOARD_WIDTH - panelWidth;
        int currentPanelX = (int)(BOARD_WIDTH + (panelStartX - BOARD_WIDTH) * progress);
        int redPaddleX = currentPanelX - PADDLE_WIDTH - (int)(10 * scaleX);

        // Simulate until ball reaches paddle
        while (simBallX < redPaddleX && simBallVX > 0) {
            simBallX += simBallVX;
            simBallY += simBallVY;

            // Wall bounces
            if (simBallY <= 0 || simBallY >= BOARD_HEIGHT - BALL_SIZE) {
                simBallVY = -simBallVY;
                simBallY = Math.max(0, Math.min(BOARD_HEIGHT - BALL_SIZE, simBallY));
            }

            // Safety check
            if (simBallX > BOARD_WIDTH * 2) break;
        }

        return simBallY + BALL_SIZE / 2;
    }

    private void updateDemoAI_Expert() {
        // Expert: Very strong AI for demo
        // Get player paddle speed setting
        double baseSpeed = 4.0; // Lenta
        if (paddleSpeedSetting == 1) baseSpeed = 6.0; // Media
        else if (paddleSpeedSetting == 2) baseSpeed = 8.0; // Veloce
        double playerSpeed = Math.max(3, (int)(baseSpeed * scaleY)); // Same as player paddle

        if (demoBallVX > 0) {
            double predictionY = calculateDemoBallTrajectory();

            // Very small error - only 2% chance
            if (Math.random() < 0.02) {
                predictionY += (Math.random() - 0.5) * 25;
            }

            double targetY = predictionY - PADDLE_HEIGHT / 2;

            // Same speed as player
            double diff = targetY - demoRedPaddleY;
            double maxSpeed = playerSpeed;
            double moveAmount = Math.signum(diff) * Math.min(maxSpeed, Math.abs(diff) * 1.2);

            demoRedPaddleY += moveAmount;
        } else {
            // Strategic positioning
            double strategicY = BOARD_HEIGHT * 0.5 - PADDLE_HEIGHT / 2;
            double diff = strategicY - demoRedPaddleY;
            demoRedPaddleY += Math.signum(diff) * Math.min(playerSpeed * 0.4, Math.abs(diff) * 0.4);
        }
    }

    private void updateDemoAI_Impossible() {
        // Impossible: Nearly perfect AI for demo
        // Get player paddle speed setting
        double baseSpeed = 4.0; // Lenta
        if (paddleSpeedSetting == 1) baseSpeed = 6.0; // Media
        else if (paddleSpeedSetting == 2) baseSpeed = 8.0; // Veloce
        double playerSpeed = Math.max(3, (int)(baseSpeed * scaleY)); // Same as player paddle

        if (demoBallVX > 0) {
            double predictionY = calculateDemoBallTrajectory();

            // Almost no error - 0.2% chance
            if (Math.random() < 0.002) {
                predictionY += (Math.random() - 0.5) * 10;
            }

            double targetY = predictionY - PADDLE_HEIGHT / 2;

            // Same speed as player (100% of player speed)
            double diff = targetY - demoRedPaddleY;
            double maxSpeed = playerSpeed;
            double moveAmount = Math.signum(diff) * Math.min(maxSpeed, Math.abs(diff) * 2.0);

            demoRedPaddleY += moveAmount;
        } else {
            // Perfect strategic positioning
            double strategicY = BOARD_HEIGHT * 0.5 - PADDLE_HEIGHT / 2;
            double diff = strategicY - demoRedPaddleY;
            demoRedPaddleY += Math.signum(diff) * Math.min(playerSpeed * 0.5, Math.abs(diff) * 0.6);
        }
    }

}
