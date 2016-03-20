package com.heat.sound;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineEvent.Type;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * The <b>Sound</b> class is meant to be an object of the sound you want to play. <br>
 * This gives you the ability to track in what state the sound is and so on. <br>
 * <b>Sound</b> should only be used to play sound effects that wont be paused during play time. <br>
 * Would the case be that you want to have more control over the sound, see <b>Music</b>
 * 
 * @author Pontus Wirsching
 * @since 2016-03-14
 */
public class Sound {

	/**
	 * AudioStream is not to be messed with.
	 */
	private AudioInputStream inputStream;

	/**
	 * Relative / local path to the file.
	 */
	private String path;

	/**
	 * FloatControl - Used to control pan.
	 */
	private FloatControl panControl;
	
	/**
	 * FloatControl - Used to control master gain.
	 * @since 1.1
	 */
	private FloatControl masterGainControl;

	public Sound(String path) {
		this.path = path;
	}

	private float pan = 0.0f;

	/**
	 * Sets the clip pan to a value between -1.0 and 1.0
	 */
	public void setPan(float p) {
		if (panControl == null)
			return;
		pan = p;
		if (pan >= -1.0 && pan <= 1.0)
			panControl.setValue(pan);
	}

	/**
	 * 
	 * @return The clips pan value.
	 */
	public float getPan() {
		return pan;
	}
	
	/**
	 * @since 1.1
	 */
	private float gain = 0.0f;

	/**
	 * Increase the clips master gain by the selected offset.
	 * @since 1.1
	 */
	public void increaseMasterGain(float offset) {
		if (gain + offset <= masterGainControl.getMaximum())
			gain += offset;
		masterGainControl.setValue(gain);
	}

	/**
	 * Sets the clips master gain.
	 * @since 1.1
	 */
	public void setMasterGain(float gain) {
		if (gain > masterGainControl.getMaximum())
			return;
		this.gain = gain;
		masterGainControl.setValue(gain);
	}

	/**
	 * @since 1.1
	 * @return The clips master gain.
	 */
	public float getMasterGain() {
		return gain;
	}

	/*
	 * If there is a pan error, display it only once.
	 */
	private boolean displayedPanError = false;
	
	/*
	 * If there's a master gain error, display it once.
	 */
	private boolean displayedGainError = false;

	/**
	 * Plays this sound clip, if the sound is already running it will be stopped. <br>
	 * This means that only one instance of this sound can be played at once. <br>
	 * If the case may be that you need to play more sounds of the same type at the same type, simply load more sound <br>
	 * in. <br>
	 */
	public void start() {
		try {
			// Get InputStream, from a relative path.
			InputStream audioSrc = getClass().getResourceAsStream("/" + path);

			// Add a 2nd InputStream as a buffer.
			InputStream bufferedIn = new BufferedInputStream(audioSrc);

			// Create AudioInput steam from
			inputStream = AudioSystem.getAudioInputStream(bufferedIn);
		} catch (UnsupportedAudioFileException | IOException e1) {
			System.out.println("[HSOUND] Error loading sound: \"" + path + "\"");
			e1.printStackTrace();
		}
		try {

			// Create clip instance.
			Clip c = AudioSystem.getClip();

			// Open clip.
			c.open(inputStream);

			/*
			 * Tries to get the pan control, this can and will fail if the sound file isn't stereo.
			 */
			try {
				panControl = (FloatControl) c.getControl(FloatControl.Type.PAN);
				panControl.setValue(pan);
			} catch (Exception e) {
				if (!displayedPanError) {
					System.err.println("[HSOUND] 'Pan' control is not supported for sound file: '" + path + "'");
					displayedPanError = true;
				}
			}
			
			try {
				masterGainControl = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
				masterGainControl.setValue(gain);
			} catch(Exception e) {
				if (!displayedGainError) {
					System.err.println("[HSOUND] 'Master Gain' control is not supported for sound file: '" + path + "'");
					displayedGainError = true;
				}
			}

			currentlyPlaying.add(c);
			
			// Play the clip.
			c.start();

			// Add line listener to close the clip when it's stopped.
			c.addLineListener(new LineListener() {

				@Override
				public void update(LineEvent event) {
					Type t = event.getType();
					if (t == Type.CLOSE) {
					}
					if (t == Type.START) {
					}
					if (t == Type.STOP) {
						 c.close();
						 currentlyPlaying.remove(c);
					}
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * ArrayList that contains all instances of the clips that are playing.
	 */
	private ArrayList<Clip> currentlyPlaying = new ArrayList<Clip>();
	
	public void loop() {
		try {
			inputStream = AudioSystem.getAudioInputStream(Sound.class.getResourceAsStream("/" + path));
		} catch (UnsupportedAudioFileException | IOException e1) {
			e1.printStackTrace();
		}
		try {
			Clip c = AudioSystem.getClip();
			c.open(inputStream);
			c.loop(Clip.LOOP_CONTINUOUSLY);

			currentlyPlaying.add(c);
			
			c.addLineListener(new LineListener() {

				@Override
				public void update(LineEvent event) {
					Type t = event.getType();
					if (t == Type.CLOSE) {
					}
					if (t == Type.START) {
					}
					if (t == Type.STOP) {
						c.close();
						currentlyPlaying.remove(c);
					}
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Stops all instances of this sound.
	 * @since 1.1
	 */
	public void stop() {
		for (Clip c : currentlyPlaying) {
			c.stop();
			c.close();
		}
	}
	
	private ArrayList<SoundStateListener> listeners = new ArrayList<>();

	/**
	 * Adds a new <b>SoundStateListener</b> to this clip.
	 * 
	 * @param soundStateListener
	 */
	public void addSoundStateListener(SoundStateListener soundStateListener) {
		listeners.add(soundStateListener);
	}

}
