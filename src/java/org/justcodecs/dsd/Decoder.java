package org.justcodecs.dsd;

import java.util.Random;

public class Decoder implements Filters {
	public static class PCMFormat {
		@Override
		public String toString() {
			return "PCMFormat [bitsPerSample=" + bitsPerSample + ", sampleRate=" + sampleRate + ", channels="
					+ channels + ", unsigned=" + unsigned + "]";
		}

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

	protected PCMFormat pcmf;
	protected DSDFormat dsdf;
	//protected long currentSample;
	protected int ratio;
	protected int scale;
	protected int clipping;

	private double[][] lookupTable;
	Random rnd;

	public void setPCMFormat(PCMFormat f) throws DecodeException {
		if (dsdf == null)
			throw new DecodeException("Target PCM format has to be set after calling init", null);
		pcmf = f;
		if (getSampleRate() % pcmf.sampleRate != 0)
			throw new DecodeException("PCM sample rate doesn't multiply evenly 44100", null);
		initParameters();
		dsdf.initBuffers(initLookupTable());
		rnd = new Random();
	}

	public void init(DSDFormat f) throws DecodeException {
		dsdf = f;
	}

	public int getSampleRate() {
		if (dsdf == null)
			return 0;
		return dsdf.getSampleRate();
	}

	public long getSampleCount() {
		if (dsdf == null)
			return 0;
		return dsdf.getSampleCount();
	}

	public boolean isDST() {
		if (dsdf == null)
			return false;
		return dsdf.isDST();
	}

	public void seek(long sampleNum) throws DecodeException {
		if (dsdf == null)
			return;
		dsdf.seek(sampleNum);
	}

	public void suspend() throws DecodeException {
		if (dsdf == null)
			return;
		dsdf.sleep();
	}

	public void dispose() {
		if (dsdf == null)
			return;
		dsdf.close();
	}

	protected void initParameters() {
		ratio = getSampleRate() / pcmf.sampleRate;
		// 16 bits 96 db Math.pow(10.0,96/20)*Math.pow(2.0,16-1)
		scale = clipping = ((1 << pcmf.bitsPerSample) - 1) >> 1;
	}

	protected int initLookupTable() throws DecodeException {
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
		return lookupTable.length;
	}

	protected double[][] getLookupTable() {
		return lookupTable;
	}

	protected void setLookupTable(double[][] lookupTable) {
		this.lookupTable = lookupTable;
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
					if (dsdf.isMSB()) { // msb
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

		if (nsc < dsdf.getNumChannels())
			throw new DecodeException("Allocated sample buffers less than number of channels", null);
		nsc = dsdf.getNumChannels();
		// flag if we need to clip
		boolean clip = clipAmplitude > 0;
		int nStep = ratio / 8;
		// get the sample buffer
		Object dsamples = dsdf.getSamples();
		byte buff[][] = null;
		byte[] buffi = null;
		if (dsamples instanceof byte[][])
			buff = (byte[][]) dsamples;
		else if (dsamples instanceof byte[])
			buffi = (byte[]) dsamples;
		else
			throw new DecodeException("Unsupported buffer type", null);
		boolean ils = buffi != null;
		if (dsdf.bufPos < 0 || dsdf.bufPos + lookupTable.length * (ils ? nsc : 1) > dsdf.bufEnd) {
			if (dsdf.readDataBlock() == false)
				return -1;
		}
		if (ils) {
			for (int i = 0; i < slen; i++) {
				// filter each chan in turn
				for (int c = 0; c < nsc; c++) {
					double sum = 0.0;
					for (int t = 0, b = 0, nLookupTable = lookupTable.length; t < nLookupTable; t++, b += nsc) {
						int byt = buffi[dsdf.bufPos + b + c] & 255;
						sum += lookupTable[t][byt];
					}

					sum *= scale;
					// dither before rounding/truncating
					if (tpdfDitherPeakAmplitude > 0) {
						// TPDF dither
						sum += ((rnd.nextDouble() - rnd.nextDouble())) * tpdfDitherPeakAmplitude;
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
				dsdf.bufPos += nStep * nsc;
				if (dsdf.bufPos + lookupTable.length * nsc > dsdf.bufEnd) {
					if (dsdf.readDataBlock() == false)
						return i; // was zeroing start
				}
			}
		} else {
			for (int i = 0; i < slen; i++) {
				// filter each chan in turn
				for (int c = 0; c < nsc; c++) {
					double sum = 0.0;
					buffi = buff[c];
					for (int t = 0, nLookupTable = lookupTable.length; t < nLookupTable; t++) {
						int byt = buffi[dsdf.bufPos + t] & 0xFF;
						sum += lookupTable[t][byt];
					}
					sum *= scale;
					// dither before rounding/truncating
					if (tpdfDitherPeakAmplitude > 0) {
						// TPDF dither
						sum += ((rnd.nextDouble() - rnd.nextDouble())) * tpdfDitherPeakAmplitude;
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
				dsdf.bufPos += nStep;
				if (dsdf.bufPos + lookupTable.length > dsdf.bufEnd) {
					if (dsdf.readDataBlock() == false)
						return i; // was zeroing start
				}
			}
		}
		return slen;
	}

	public int decodePCM(int[]... channels) throws DecodeException {
		return getSamples1(scale, 0, clipping, channels);
	}

	public int decodeDSD(int channels, byte[] samples) throws DecodeException {
		if (channels < dsdf.getNumChannels())
			throw new DecodeException("Channels to decode to less than in source", null);
		if (samples.length % channels != 0)
			throw new DecodeException("Buffer length isn't multiply of number channels", null);
		if (dsdf.bufPos < 0 || dsdf.bufPos > dsdf.bufEnd) {
			if (dsdf.readDataBlock() == false)
				return -1;
		}
		Object dsamples = dsdf.getSamples();
		if (dsamples instanceof byte[][]) {
			byte[][] buff = (byte[][]) dsamples;
			int si = 0;
			do {
				for (int c = 0; c < channels; c++) {
					samples[si++] = buff[c][dsdf.bufPos];
				}
				dsdf.bufPos++;
				if (si >= samples.length)
					return si;
				if (dsdf.bufPos > dsdf.bufEnd) {
					if (dsdf.readDataBlock() == false)
						return si;
				}
			} while (true);
		} else if (dsamples instanceof byte[]) {
			byte[] buff = (byte[]) dsamples;
			if (channels == dsdf.getNumChannels()) {
				int si = Math.min(buff.length, -dsdf.bufPos + dsdf.bufEnd);
				System.arraycopy(buff, 0, samples, 0, si);
				dsdf.bufPos += si;
				return si;
			} else
				throw new DecodeException("Channels down mixing isn't implemented yet", null);
		} else
			throw new DecodeException("Unsupported buffer type", null);
	}
}
