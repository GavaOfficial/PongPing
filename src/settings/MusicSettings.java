package settings;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static context.SettingsContext.*;

public class MusicSettings {

    public Clip backgroundMusic;

    public void loadMusic() {
        try {
            AudioInputStream audioInputStream = null;

            // Try to load from JAR resources first
            InputStream musicStream = getClass().getClassLoader().getResourceAsStream("music/Gava-OfficialSoundtrack.wav");
            if (musicStream != null) {
                audioInputStream = AudioSystem.getAudioInputStream(musicStream);
                System.out.println("✓ Music loaded from JAR: Gava-OfficialSoundtrack.wav");
            } else {
                // Fallback: try loading from file system (development mode)
                File musicFile = new File("music/Gava-OfficialSoundtrack.wav");
                if (musicFile.exists()) {
                    audioInputStream = AudioSystem.getAudioInputStream(musicFile);
                    System.out.println("✓ Music loaded from file: Gava-OfficialSoundtrack.wav");
                }
            }

            if (audioInputStream != null) {
                backgroundMusic = AudioSystem.getClip();
                backgroundMusic.open(audioInputStream);
                updateMusicVolume(); // Set initial volume
                if (musicEnabled) {
                    backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
                }
            } else {
                System.out.println("⚠️  Background music file not found");
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.out.println("Could not load background music: " + e.getMessage());
        }
    }

    public void updateMusicVolume() {
        if (backgroundMusic != null && backgroundMusic.isOpen()) {
            try {
                FloatControl volumeControl = (FloatControl) backgroundMusic.getControl(FloatControl.Type.MASTER_GAIN);
                // Convert 0-100 range to decibel range
                float volume = musicVolume / 100.0f;
                float dB = (float) (Math.log(volume == 0 ? 0.0001 : volume) / Math.log(10.0) * 20.0);
                volumeControl.setValue(Math.max(volumeControl.getMinimum(), Math.min(dB, volumeControl.getMaximum())));
            } catch (Exception e) {
                System.out.println("Could not set volume: " + e.getMessage());
            }
        }
    }

    // Sound effect methods
    public void playPaddleHitSound() {
        if (effectsVolume == 0) return; // Skip if effects muted
        new Thread(() -> {
            try {
                AudioFormat af = new AudioFormat(44100, 8, 1, true, false);
                SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
                sdl.open(af);
                sdl.start();

                byte[] buf = new byte[1000];
                double volumeMultiplier = effectsVolume / 100.0;
                for (int i = 0; i < buf.length; i++) {
                    double angle = i / (44100.0 / 800) * 2.0 * Math.PI;
                    buf[i] = (byte) (Math.sin(angle) * 80 * volumeMultiplier);
                }

                sdl.write(buf, 0, buf.length);
                sdl.drain();
                sdl.close();
            } catch (Exception e) { /* Silent fail */ }
        }).start();
    }

    public void playScoreSound() {
        if (effectsVolume == 0) return; // Skip if effects muted
        new Thread(() -> {
            try {
                AudioFormat af = new AudioFormat(44100, 8, 1, true, false);
                SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
                sdl.open(af);
                sdl.start();

                double volumeMultiplier = effectsVolume / 100.0;
                for (int freq : new int[]{523, 659, 784}) {
                    byte[] buf = new byte[2000];
                    for (int i = 0; i < buf.length; i++) {
                        double angle = i / (44100.0 / freq) * 2.0 * Math.PI;
                        buf[i] = (byte) (Math.sin(angle) * 60 * volumeMultiplier);
                    }
                    sdl.write(buf, 0, buf.length);
                }

                sdl.drain();
                sdl.close();
            } catch (Exception e) { /* Silent fail */ }
        }).start();
    }

    public void playWallHitSound() {
        if (effectsVolume == 0) return; // Skip if effects muted
        new Thread(() -> {
            try {
                AudioFormat af = new AudioFormat(44100, 8, 1, true, false);
                SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
                sdl.open(af);
                sdl.start();

                byte[] buf = new byte[800];
                double volumeMultiplier = effectsVolume / 100.0;
                for (int i = 0; i < buf.length; i++) {
                    double angle = i / (44100.0 / 300) * 2.0 * Math.PI;
                    buf[i] = (byte) (Math.sin(angle) * 40 * volumeMultiplier);
                }

                sdl.write(buf, 0, buf.length);
                sdl.drain();
                sdl.close();
            } catch (Exception e) { /* Silent fail */ }
        }).start();
    }

}
