package com.heat.sound;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * The <b>Music</b> class is designed for larger types of sound files, such as
 * background <br>
 * music and narration etc. <br>
 * The biggest difference is that you can not play multiple instances of the
 * same <b>Music</b> <br>
 * without restarting it. <br>
 * 
 * @author Pontus Wirsching
 * @since 2016-03-15
 */
public class Music {

	/**
	 * Clip instance.
	 */
	public Clip clip;

	/**
	 * Audio file stream.
	 */
	private AudioInputStream inputStream;

	/**
	 * Local/Relative file path to music file.
	 */
	private String path;

	/**
	 * The frame number that the clip was paused at. <br>
	 */
	private int pausedAt = 0;

	/**
	 * FloatControl for changing master gain / volume.
	 */
	public FloatControl masterGainControl;

	/**
	 * FloatControl for changing clip pan.
	 */
	private FloatControl panControl;

	public Music(String path) {
		this.path = path;

		Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();


		try {
			InputStream audioSrc = getClass().getResourceAsStream("/" + path);
			InputStream bufferedIn = new BufferedInputStream(audioSrc);
			inputStream = AudioSystem.getAudioInputStream(bufferedIn);
		} catch (UnsupportedAudioFileException e) {
			System.err.println("[HSOUND] The file '" + path + "' is using an unsupported file extension!");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int i = 0;
		
		System.out.println(mixerInfo[i].getName() + ": " + mixerInfo[i].getDescription());

		
		Mixer mixer = AudioSystem.getMixer(mixerInfo[i]);
		AudioFormat format = inputStream.getFormat();
		DataLine.Info info = new DataLine.Info(Clip.class, format, ((int) inputStream.getFrameLength() * format.getFrameSize()));

		try {
			clip = (Clip) mixer.getLine(info);
		} catch (LineUnavailableException e1) {
			e1.printStackTrace();
		}

		try {
			clip.open(inputStream);
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			panControl = (FloatControl) clip.getControl(FloatControl.Type.PAN);
		} catch (Exception e) {
			System.err.println("[HSOUND] Control type 'PAN' is not supported!");
			e.printStackTrace();
		}

		
		try {
			masterGainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		} catch (Exception e) {
			System.err.println("[HSOUND] Control type 'MASTER_GAIN' is not supported!");
			e.printStackTrace();
		}

	
	}

	private float gain = 0.0f;

	/**
	 * Increase the clips master gain by the selected offset.
	 */
	public void increaseMasterGain(float offset) {
		if (gain + offset <= masterGainControl.getMaximum())
			gain += offset;
		masterGainControl.setValue(gain);
	}

	/**
	 * Sets the clips master gain.
	 */
	public void setMasterGain(float gain) {
		if (gain > masterGainControl.getMaximum())
			return;
		this.gain = gain;
		masterGainControl.setValue(gain);
	}

	/**
	 * 
	 * @return The clips master gain.
	 */
	public float getMasterGain() {
		return gain;
	}

	private float pan = 0.0f;

	/**
	 * Sets the clip pan to a value between -1.0 and 1.0
	 */
	public void setPan(float p) {
		if (panControl == null)
			return;
		pan = p;
		if (pan >= panControl.getMinimum() && pan <= panControl.getMaximum())
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
	 * This will loop the clip until stop or pause is called.
	 */
	public void loop() {
		clip.setFramePosition(0);
		clip.loop(Clip.LOOP_CONTINUOUSLY);
	}

	/**
	 * Plays the clip instance from where it was lastly paused. <br>
	 * If the Music was stopped the play method will start from the beginning.
	 * <br>
	 */
	public void play() {
		clip.setFramePosition(pausedAt);
		clip.start();
	}

	/**
	 * Pauses the clip and saves the frame position.
	 */
	public void pause() {
		pausedAt = clip.getFramePosition();
		clip.stop();
	}

	/**
	 * Stops the clip and resets the position to 0.
	 */
	public void stop() {
		pausedAt = 0;
		clip.stop();
	}

	/**
	 * 
	 * @return The progress, a value between 0.0 to 1.0 of the total play time.
	 */
	public double getProgress() {
		return (double) clip.getFramePosition() / (double) clip.getFrameLength();
	}

	/**
	 * 
	 * @return The clips current frame position in milliseconds.
	 */
	public long getClipCurrentPosition() {
		return clip.getMicrosecondPosition();
	}

	/**
	 * 
	 * @return The clips length in milliseconds.
	 */
	public long getClipLengthInMilliseconds() {
		return clip.getMicrosecondLength();
	}

	/**
	 * 
	 * @return The local/relative file path.
	 */
	public String getFilePath() {
		return path;
	}

}
