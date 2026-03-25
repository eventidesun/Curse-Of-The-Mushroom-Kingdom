package main;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.net.URL;

public class Sound {
    Clip clip; // to open all the files

    URL soundURl[] = new URL[30];

    public Sound() {
        soundURl[0] = getClass().getResource("/sound/BlueBoyAdventure.wav");
        soundURl[1] = getClass().getResource("/sound/coin.wav");
        soundURl[2] = getClass().getResource("/sound/powerup.wav");
        soundURl[3] = getClass().getResource("/sound/unlock.wav");
        soundURl[4] = getClass().getResource("/sound/fanfare.wav");
    }

    public void setFile(int i) {
        try { // the format to open an audio file in java
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundURl[i]);
            clip = AudioSystem.getClip();
            clip.open(ais);
        } catch (Exception e) {

        }
    }

    public void play() {
        if (clip == null) return;
        clip.start();
    }

    public void loop() {
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public void stop() {
        clip.stop();
    }
}
