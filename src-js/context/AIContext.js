/**
 * AI Context
 * Manages AI opponent state and behavior
 */

// AI variables
export let aiTargetY = 250;
export let aiCurrentVelocity = 0.0;
export let aiPaddleY = 250.0; // Smooth AI paddle position
export let aiDifficulty = 3; // 1-5, higher = harder
export let aiMaxSpeed = 4.0;
export let aiAcceleration = 0.4;
export let aiDeceleration = 0.85;
export let lastAIUpdate = 0;
export let lastBallDirectionChange = 0;
export let aiReactionDelay = 0.2; // seconds

// Setters for modifying exported values
export function setAiTargetY(value) {
    aiTargetY = value;
}

export function setAiCurrentVelocity(value) {
    aiCurrentVelocity = value;
}

export function setAiPaddleY(value) {
    aiPaddleY = value;
}

export function setAiDifficulty(value) {
    aiDifficulty = value;
}

export function setAiMaxSpeed(value) {
    aiMaxSpeed = value;
}

export function setAiAcceleration(value) {
    aiAcceleration = value;
}

export function setAiDeceleration(value) {
    aiDeceleration = value;
}

export function setLastAIUpdate(value) {
    lastAIUpdate = value;
}

export function setLastBallDirectionChange(value) {
    lastBallDirectionChange = value;
}

export function setAiReactionDelay(value) {
    aiReactionDelay = value;
}
