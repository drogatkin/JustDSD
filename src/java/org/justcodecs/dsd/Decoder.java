package org.justcodecs.dsd;

import java.io.IOException;
import java.util.Random;

public class Decoder implements Filters {
	public static class PCMFormat {
		public int bitsPerSample;
		public int sampleRate;
		public int channels;
		public boolean lsb;
		public boolean unsigned;
	}

	public static class DecodeException extends Exception {
		public DecodeException(String msg, Throwable reason) {
			super(msg, reason);
		}
	}

	protected FMTChunk fmt;
	protected PCMFormat pcmf;
	protected DATAChunk dc;
	protected DSDStream dsdStream;

	//protected long currentSample;
	protected int ratio;

	double[][] lookupTable;
	Random rnd;

	public void setPCMFormat(PCMFormat f) throws DecodeException {
		if (dc == null)
			throw new DecodeException("Target PCM format has to be set after calling init", null);
		pcmf = f;
		initLookupTable();
		rnd = new Random();
		dc.data = new byte[fmt.channelNum][fmt.blockSize + lookupTable.length];
	}

	public void init(DSDStream ds) throws DecodeException {
		dsdStream = ds;
		DSDChunk.read(dsdStream);
		fmt = FMTChunk.read(dsdStream);
		System.out.printf("FMT:%s%n", fmt);
		dc = DATAChunk.read(dsdStream);
		//System.out.printf("DATA:%s%n", dc);		
	}
	
	public long getSampleRate() {
		if (fmt != null)
			return fmt.sampleFreq;
		return 0;
	}
	
	public long getSampleCount() {
		if (fmt != null)
			return fmt.sampleCount;
		return 0;
	}

	public void dispose() {
		try {
			dsdStream.close();
		} catch (IOException e) {
			
		}
	}

	protected void initLookupTable() throws DecodeException {
		ratio = fmt.sampleFreq / pcmf.sampleRate;
		switch (ratio) {
		case 8:
			initLookupTable(coefs_352, tzero_352);
			break;
		case 16:
			initLookupTable(coefs_176, tzero_176);
			break;
		case 32:
			initLookupTable(coefs_88, tzero_88);
			break;
		default:
			throw new DecodeException("Incompatible sample rate combination " + ratio, null);
		}

	}

	boolean readDataBlock() throws DecodeException {
		//System.out.printf("read block%n");
		try {
			if (dsdStream.getFilePointer() >= dc.dataEnd)
				return false;
			if (dc.bufPos < 0)
				dc.bufPos = 0;
			int delta = dc.bufEnd - dc.bufPos;
			for (int c = 0; c < fmt.channelNum; c++) {
				if (delta > 0)
					System.arraycopy(dc.data[c], dc.bufPos, dc.data[c], 0, delta);
				dsdStream.readFully(dc.data[c], delta, fmt.blockSize);
			}
			dc.bufPos = 0;
			dc.bufEnd = fmt.blockSize + delta;
		} catch (IOException e) {
			throw new DecodeException("IO exception at reading samples", e);
		}
		return true;
	}

	// TODO why it can't be pre-populated without this exercise?
	void initLookupTable(double[] coefs, int tz) {
		int nLookupTable = (coefs.length + 7) / 8;
		// allocate the table
		lookupTable = new double[nLookupTable][256];
		// loop over each entry in the lookup table
		for (int t = 0; t < lookupTable.length; t++) {
			// how many samples from the filter are spanned in this entry
			int k = coefs.length - t * 8;
			if (k > 8)
				k = 8;
			// loop over all possible 8bit dsd sequences
			for (int dsdSeq = 0; dsdSeq < 256; ++dsdSeq) {
				double acc = 0.0;
				for (int bit = 0; bit < k; bit++) {
					double val;
					if (fmt.bitPerSample == 8) { // msb
						val = (dsdSeq & (1 << (7 - bit))) != 0 ? 1.0 : -1.0;
					} else {
						val = (dsdSeq & (1 << (bit))) != 0 ? 1.0 : -1.0;
					}
					acc += val * coefs[t * 8 + bit];
				}
				lookupTable[t][dsdSeq] = acc;
			}
		}
	}

	<S> int getSamples1(double scale, double tpdfDitherPeakAmplitude, double clipAmplitude, S samples)
			throws DecodeException {
		//if (currentSample >= fmt.sampleCount)
		//return -1;
		int[][] samplesInt = null;
		short[][] samplesShort = null;
		double[][] samplesFloat = null;
		boolean roundToInt = true;
		int nsc = 0;
		int slen = 0;
		if (samples instanceof short[][]) {
			samplesShort = (short[][]) samples;
			nsc = samplesShort.length;
			slen = samplesShort[0].length;
		} else if (samples instanceof double[][]) {
			samplesFloat = (double[][]) samples;
			roundToInt = false;
			nsc = samplesFloat.length;
			slen = samplesFloat[0].length;
		} else if (samples instanceof int[][]) {
			samplesInt = (int[][]) samples;
			nsc = samplesInt.length;
			slen = samplesInt[0].length;
		} else
			throw new DecodeException("Unsupported type of samples buffer", null);

		if (nsc < fmt.channelNum)
			throw new DecodeException("Allocated sample buffers less than number of channels", null);

		// flag if we need to clip
		boolean clip = clipAmplitude > 0;
		int nStep = ratio / 8;
		// get the sample buffer
		byte buff[][] = dc.data;
		if (dc.bufPos < 0 || dc.bufPos + lookupTable.length > dc.bufEnd) {
			if (readDataBlock() == false)
				return -1;
		}
		for (int i = 0; i < slen; i++) {
			// filter each chan in turn
			for (int c = 0; c < fmt.channelNum; c++) {
				double sum = 0.0;
				for (int t = 0, nLookupTable = lookupTable.length; t < nLookupTable; t++) {
					int byt = buff[c][dc.bufPos + t] & 0xFF;
					sum += lookupTable[t][byt];
				}
				sum = sum * scale;
				if (c == 0 && false)
					System.out.printf(" %f%n", sum);
				// dither before rounding/truncating
				if (tpdfDitherPeakAmplitude > 0) {
					// TPDF dither
					sum = sum + ((rnd.nextDouble() - rnd.nextDouble())) * tpdfDitherPeakAmplitude;
				}
				if (clip) {
					if (sum > clipAmplitude)
						sum = clipAmplitude;
					else if (sum < -clipAmplitude)
						sum = -clipAmplitude;
				}
				if (roundToInt)
					if (samplesShort == null)
						samplesInt[c][i] = (int) Math.round(sum);
					else
						samplesShort[c][i] = (short) Math.round(sum);
				else
					samplesFloat[c][i] = (sum);
			}
			// step the buffer
			//System.out.printf("Skipping %d%n", nStep);
			//currentSample++;
			//if (currentSample >= fmt.sampleCount)
			//return i;
			dc.bufPos += nStep;
			if (dc.bufPos + lookupTable.length > dc.bufEnd) {
				if (readDataBlock() == false)
					return i;
				dc.bufPos = 0;
			}
		}
		return slen;
	}

	public int decodePCM(int[]... channels) throws DecodeException {
		// 16 bits 96 db
		return getSamples1(0x7fff/*Math.pow(10.0,96/20)*Math.pow(2.0,16-1)*/, 0, 0x7fff, channels);
	}
}
