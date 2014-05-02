package org.justcodecs.dsd;

import java.io.File;
import java.io.FileNotFoundException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.justcodecs.dsd.Decoder.DecodeException;
import org.justcodecs.dsd.Decoder.PCMFormat;

public class Player {

	public static void main(String[] args) {
		System.out.printf("Java DSD player for PCM DAC  (c) 2014%n");
		if (args.length == 0) {
			System.out.printf("Please use with at least one DSF file argument%n");
			System.exit(-1);
			return;
		}

		for (String f : args) {
			try {
				new Player().play(f);
			} catch (DecodeException e) {
				System.out.printf("Couldn't play %s, becasue %s%n", f, e);
				e.printStackTrace();
			}
		}

	}

	public void play(String f) throws Decoder.DecodeException {
		Decoder decoder = new Decoder();
		try {
			decoder.init(new Utils.RandomDSDStream(new File(f)));
			PCMFormat pcmf = decoder.getPCMFormat();
			AudioFormat af = new AudioFormat(pcmf.sampleRate, pcmf.bitsPerSample, pcmf.channels, false, pcmf.lsb);
			SourceDataLine dl = AudioSystem.getSourceDataLine(af);
			dl.open();
			dl.start();
			byte[][] samples = new byte[pcmf.channels][2048];
			int channels = (pcmf.channels > 2 ? 2 : pcmf.channels);
			int bytesChannelSample = pcmf.bitsPerSample / 8;
			int bytesSample = channels * bytesChannelSample;
			byte[] playBuffer = new byte[channels * 2048];
			do {
				int nsampl = decoder.decodePCM(samples);
				if (nsampl <= 0)
					break;
				for (int s = 0; s < nsampl; s++) {
					for (int c = 0; c < channels; c++) {
						for (int b = 0; b < bytesChannelSample; b++)
							playBuffer[s * bytesSample + b] = samples[c][s * bytesChannelSample + b];
					}
				}
				dl.write(playBuffer, 0, nsampl * bytesSample);
			} while (true);
			dl.stop();
			dl.close();
		} catch (FileNotFoundException e) {
			throw new Decoder.DecodeException("Not found " + f, e);
		} catch (LineUnavailableException e) {
			throw new Decoder.DecodeException("Can't play this format", e);
		}

	}

}
