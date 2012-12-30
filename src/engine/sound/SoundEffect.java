package engine.sound;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.log4j.Logger;

/**
 * http://www3.ntu.edu.sg/home/ehchua/programming/java/J8c_PlayingSound.html
 * http
 * ://docs.oracle.com/javase/1.4.2/docs/guide/sound/programmer_guide/chapter4
 * .html
 * 
 * @author kenny
 * 
 */
public class SoundEffect extends AbstractSound {

	private static final int BUFFER_SIZE = 64 * 1024; // 64 KB

	private SoundChannels lines;

	private byte[] data;

	private AudioFormat format;

	private final static Logger LOGGER = Logger
			.getLogger(SoundEffect.class.getName());

	public SoundEffect(String file) {
		super(file);
		// Set up an audio input stream piped from the sound file.
		try {
			ByteArrayOutputStream baout = new ByteArrayOutputStream();
			AudioInputStream audioInputStream = AudioSystem
					.getAudioInputStream(new File(file));
			format = audioInputStream.getFormat();

			lines = SoundChannels.getInstance();

			int nBytesRead = 0;
			byte[] buffer = new byte[BUFFER_SIZE];
			while (nBytesRead != -1) {
				while ((nBytesRead = audioInputStream.read(buffer, 0,
						buffer.length)) != -1) {
					baout.write(buffer, 0, nBytesRead);
				}
				audioInputStream.close();
				baout.close();
				data = baout.toByteArray();
			}
			LOGGER.trace("loaded: " + data.length + " bytes");
			loaded = true;
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
			loaded = false;
		} catch (IOException e) {
			e.printStackTrace();
			loaded = false;
			// } catch (LineUnavailableException e) {
			// e.printStackTrace();
			// loaded = false;
		}
	}

	@Override
	public void play() {
		if (loaded) {
			Runnable runner = new Runnable() {
				public void run() {
					try {
						SourceDataLine line = lines.getFreeLine(format, volume);
						if (line != null) {
							line.open(format);
							volume(volume, line);
							line.start();
							line.write(data, 0, data.length);
							line.drain();
							line.close();
						} else {
							// System.out.println("line is null");
						}
					} catch (LineUnavailableException e) {
						e.printStackTrace();
					}

				}
			};
			Thread playThread = new Thread(runner);
			playThread.start();
		}
	}
	
	public void volume(int volume, SourceDataLine line) {
		this.volume = volume;
		if (volume < 0) {
			volume = 0;
		} else if (volume > 100) {
			volume = 100;
		}
		FloatControl gainControl = (FloatControl) line
				.getControl(FloatControl.Type.MASTER_GAIN);
		float amt = 0;
		float range = gainControl.getMaximum() - gainControl.getMinimum();
		amt = (float) (gainControl.getMinimum() + range * volume / 100.0);
		gainControl.setValue(amt); // Reduce volume by 10 decibels.
	}

}