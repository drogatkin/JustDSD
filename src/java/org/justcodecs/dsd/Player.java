package org.justcodecs.dsd;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLDecoder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.justcodecs.dsd.Decoder.DecodeException;
import org.justcodecs.dsd.Decoder.PCMFormat;

public class Player {

	public static void main(String[] args) {
		System.out.printf("Java DSD player for PCM DAC  (c) 2015 D. Rogatkin%n");
		if (args.length == 0) {
			System.out.printf("Please use with at least one .dsf, .dff, or [SACD].iso file argument%n");
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

	Decoder createDecoder() {
		return new Decoder();
	}

	public void play(String f) throws Decoder.DecodeException {
		Decoder decoder = createDecoder();
		long sampleCount = 0;
		try {
			DSDFormat dsd;
			if (f.endsWith(".dsf")) {
				dsd = new DSFFormat();
			} else if (f.endsWith(".iso")) {
				dsd = new DISOFormat();
			} else
				dsd = new DFFFormat();
			if (f.toUpperCase().startsWith("FILE:/")) {
				try {
					f = new URL(URLDecoder.decode(f, "UTF-8")).getFile();
				} catch (Exception e) {
					// ignore
				}	
			}
			dsd.init(new Utils.RandomDSDStream(new File(f)));
			decoder.init(dsd);
			System.out.printf("Playing ... %s%n", dsd);
			SourceDataLine dl = getDSDLine(dsd);
			if (dl != null) {
				byte[] samples = new byte[2 * 2048];
				dl.open();
				dl.start();
				do {
					int nsampl = decoder.decodeDSD(dsd.getNumChannels(), samples);
					if (nsampl <= 0)
						break;
					dl.write(samples, 0, nsampl);
					sampleCount += nsampl;
				} while (true);
				dl.stop();
				dl.close();
			} else {
				//System.out.printf("Samples %d duration %ds%n",  decoder.getSampleCount(), decoder.getSampleCount()/decoder.getSampleRate());
				PCMFormat pcmf = new PCMFormat();
				pcmf.sampleRate = 44100 * 2 * 2;
				pcmf.bitsPerSample = 16;
				//System.out.printf("clip: %x %x  %x-%x%n",((1 << pcmf.bitsPerSample) - 1) >> 1, 1 << pcmf.bitsPerSample, Short.MAX_VALUE, Short.MIN_VALUE); 
				pcmf.channels = 2;
				AudioFormat af = new AudioFormat(pcmf.sampleRate, pcmf.bitsPerSample, pcmf.channels, true, pcmf.lsb);
				dl = AudioSystem.getSourceDataLine(af);
				pcmf.bitsPerSample = 24;
				decoder.setPCMFormat(pcmf);
				dl.open();
				dl.start();
				int[][] samples = new int[pcmf.channels][2048];
				int channels = (pcmf.channels > 2 ? 2 : pcmf.channels);
				int bytesChannelSample = 2; //pcmf.bitsPerSample / 8;
				int bytesSample = channels * bytesChannelSample;
				byte[] playBuffer = new byte[bytesSample * 2048];
				decoder.seek(0);
				int testSeek = 0;
				do {
					int nsampl = decoder.decodePCM(samples);
					if (nsampl <= 0)
						break;
					int bp = 0;
					for (int s = 0; s < nsampl; s++) {
						for (int c = 0; c < channels; c++) {
							//System.out.printf("%x", samples[c][s]);
							samples[c][s] >>=8;
							for (int b = 0; b < bytesChannelSample; b++)
								playBuffer[bp++] = (byte) ((samples[c][s] >> (b * 8)) & 255);
						}
					}
					//for (int k=0;k<bp; k++)
					//System.out.printf("%x", playBuffer[k]);
					dl.write(playBuffer, 0, bp);
					sampleCount += nsampl;
					if (testSeek > 0 && sampleCount > pcmf.sampleRate * 10) {
						decoder.seek((long) decoder.getSampleRate() * (testSeek));
						testSeek = 0;
					}
				} while (true);
				dl.stop();
				dl.close();
			}
		} catch (FileNotFoundException e) {
			throw new Decoder.DecodeException("Not found " + f, e);
		} catch (LineUnavailableException e) {
			throw new Decoder.DecodeException("Can't play this format", e);
		}
		decoder.dispose();
		System.out.printf("Total samples: %d%n", sampleCount);
	}

	protected SourceDataLine getDSDLine(DSDFormat dsd) {
		try {
			return AudioSystem.getSourceDataLine(new AudioFormat(new AudioFormat.Encoding("DSD_UNSIGNED"),
					dsd.getSampleRate(), 1, dsd.getNumChannels(), 4, dsd.getSampleRate()/32,
					true));
		} catch (IllegalArgumentException e) {
			System.out.printf("No DSD %s%n", e);
		} catch (LineUnavailableException e) {
			System.out.printf("No DSD %s%n", e);
		}
		return null;
	}

}
