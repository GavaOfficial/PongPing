/**
 * Music Settings  
 * Manages background music and sound effects using Web Audio API
 */

import { musicVolume, effectsVolume, musicEnabled } from '../context/SettingsContext.js';

/**
 * MusicSettings class
 */
export class MusicSettings {
    constructor() {
        this.audioContext = null;
        this.backgroundMusic = null;
        this.musicSource = null;
        this.musicGainNode = null;
        this.effectsGainNode = null;
        this.isInitialized = false;
    }

    /**
     * Initialize Web Audio API context
     */
    async initAudioContext() {
        if (this.isInitialized) return;
        
        try {
            this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
            
            // Create gain nodes for volume control
            this.musicGainNode = this.audioContext.createGain();
            this.effectsGainNode = this.audioContext.createGain();
            
            this.musicGainNode.connect(this.audioContext.destination);
            this.effectsGainNode.connect(this.audioContext.destination);
            
            this.updateMusicVolume();
            this.isInitialized = true;
            
            console.log('✓ Web Audio API initialized');
        } catch (e) {
            console.error('Could not initialize Web Audio API:', e);
        }
    }

    /**
     * Load and play background music
     */
    async loadMusic() {
        if (!this.isInitialized) {
            await this.initAudioContext();
        }
        
        try {
            const response = await fetch('music/Gava-OfficialSoundtrack.wav');
            if (!response.ok) {
                console.log('⚠️  Background music file not found');
                return;
            }
            
            const arrayBuffer = await response.arrayBuffer();
            this.backgroundMusic = await this.audioContext.decodeAudioData(arrayBuffer);
            
            if (musicEnabled) {
                this.playBackgroundMusic();
            }
            
            console.log('✓ Background music loaded');
        } catch (e) {
            console.log('Could not load background music:', e.message);
        }
    }

    /**
     * Play background music in loop
     */
    playBackgroundMusic() {
        if (!this.backgroundMusic || !this.audioContext) return;
        
        // Stop current source if playing
        if (this.musicSource) {
            this.musicSource.stop();
        }
        
        // Create new source
        this.musicSource = this.audioContext.createBufferSource();
        this.musicSource.buffer = this.backgroundMusic;
        this.musicSource.loop = true;
        this.musicSource.connect(this.musicGainNode);
        this.musicSource.start(0);
    }

    /**
     * Stop background music
     */
    stopBackgroundMusic() {
        if (this.musicSource) {
            this.musicSource.stop();
            this.musicSource = null;
        }
    }

    /**
     * Update music volume (0-100 scale)
     */
    updateMusicVolume() {
        if (this.musicGainNode) {
            this.musicGainNode.gain.value = musicVolume / 100.0;
        }
    }

    /**
     * Update effects volume (0-100 scale)
     */
    updateEffectsVolume() {
        if (this.effectsGainNode) {
            this.effectsGainNode.gain.value = effectsVolume / 100.0;
        }
    }

    /**
     * Play paddle hit sound effect
     */
    playPaddleHitSound() {
        if (effectsVolume === 0 || !this.audioContext) return;
        
        const oscillator = this.audioContext.createOscillator();
        const gainNode = this.audioContext.createGain();
        
        oscillator.connect(gainNode);
        gainNode.connect(this.effectsGainNode);
        
        oscillator.frequency.value = 800;
        gainNode.gain.setValueAtTime(0.3, this.audioContext.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.1);
        
        oscillator.start(this.audioContext.currentTime);
        oscillator.stop(this.audioContext.currentTime + 0.1);
    }

    /**
     * Play score sound effect
     */
    playScoreSound() {
        if (effectsVolume === 0 || !this.audioContext) return;
        
        const frequencies = [523, 659, 784]; // C, E, G
        let time = this.audioContext.currentTime;
        
        frequencies.forEach((freq, i) => {
            const oscillator = this.audioContext.createOscillator();
            const gainNode = this.audioContext.createGain();
            
            oscillator.connect(gainNode);
            gainNode.connect(this.effectsGainNode);
            
            oscillator.frequency.value = freq;
            gainNode.gain.setValueAtTime(0.2, time);
            gainNode.gain.exponentialRampToValueAtTime(0.01, time + 0.15);
            
            oscillator.start(time);
            oscillator.stop(time + 0.15);
            
            time += 0.1;
        });
    }

    /**
     * Play wall hit sound effect
     */
    playWallHitSound() {
        if (effectsVolume === 0 || !this.audioContext) return;
        
        const oscillator = this.audioContext.createOscillator();
        const gainNode = this.audioContext.createGain();
        
        oscillator.connect(gainNode);
        gainNode.connect(this.effectsGainNode);
        
        oscillator.frequency.value = 300;
        gainNode.gain.setValueAtTime(0.2, this.audioContext.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.08);
        
        oscillator.start(this.audioContext.currentTime);
        oscillator.stop(this.audioContext.currentTime + 0.08);
    }
}
