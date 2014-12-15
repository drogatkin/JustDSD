package org.justcodecs.dsd;

public class DecoderInt extends Decoder /*implements FiltersInt */{
	int lookupTableI[][];

	@Override
	protected int initLookupTable() throws DecodeException {
		switch (ratio) {
		case 8:
			initLookupTableI(coefs_352, tzero_352);
			break;
		case 16:
			initLookupTableI(coefs_176, tzero_176);
			break;
		case 32:
			initLookupTableI(coefs_88, tzero_88);
			break;
		default:
			throw new DecodeException("Incompatible sample rate combination " + ratio, null);
		}
		return lookupTableI.length;
	}

	@Override
	protected void initParameters() {
		super.initParameters();
		scale = ((1 << pcmf.bitsPerSample) - 1);// >> 1;
		clipping = scale >> 1;
	}

	@Override
	public int decodePCM(int[]... channels) throws DecodeException {
		return getSamples2(clipping, channels);
	}

	int getSamples2(int clipAmplitude, int[][] samples) throws DecodeException {
		if (samples.length < dsdf.getNumChannels())
			throw new DecodeException("Number of allocated sample buffers is less than number of channels", null);
		int tpdfDitherPeakAmplitude = 0;
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
		int nStep = ratio / 8;
		int nsc = samples.length;
		if (dsdf.bufPos < 0 || dsdf.bufPos + lookupTableI.length * (ils ? nsc : 1) > dsdf.bufEnd) {
			if (dsdf.readDataBlock() == false)
				return -1;
		}
		if (ils) {
			for (int i = 0; i < samples[0].length; i++) {
				// filter each channel in turn
				for (int c = 0; c < nsc; c++) {
					int sum = 0;
					for (int t = 0, nLookupTable = lookupTableI.length; t < nLookupTable; t++) {
						int byt = buffi[dsdf.bufPos + t * nsc + c] & 255;
						sum += lookupTableI[t][byt];
					}
					// dither before rounding/truncating
					if (tpdfDitherPeakAmplitude > 0) {
						// TPDF dither
						sum = sum + ((rnd.nextInt() - rnd.nextInt())) * tpdfDitherPeakAmplitude;
					}
					if (clipAmplitude > 0) {
						if (sum > clipAmplitude)
							sum = clipAmplitude;
						else if (sum < -clipAmplitude)
							sum = -clipAmplitude;
					}
					samples[c][i] = sum;
				}
				dsdf.bufPos += nStep * nsc;
				if (dsdf.bufPos + lookupTableI.length * nsc > dsdf.bufEnd) {
					if (dsdf.readDataBlock() == false)
						return i; // was zeroing start
				}
			}
		} else {
			for (int i = 0; i < samples[0].length; i++) {
				// filter each chan in turn
				for (int c = 0; c < nsc; c++) {
					int sum = 0;
					buffi = buff[c];
					for (int t = 0, nLookupTable = lookupTableI.length; t < nLookupTable; t++) {
						int byt = buffi[dsdf.bufPos + t] & 0xFF;
						sum += lookupTableI[t][byt];
					}

					// dither before rounding/truncating
					if (tpdfDitherPeakAmplitude > 0) {
						// TPDF dither
						sum = sum + ((rnd.nextInt() - rnd.nextInt())) * tpdfDitherPeakAmplitude;
					}
					if (clipAmplitude > 0) {
						if (sum > clipAmplitude)
							sum = clipAmplitude;
						else if (sum < -clipAmplitude)
							sum = -clipAmplitude;
					}
					samples[c][i] = sum;
				}
				dsdf.bufPos += nStep;
				if (dsdf.bufPos + lookupTableI.length > dsdf.bufEnd) {
					if (dsdf.readDataBlock() == false)
						return i; // was zeroing start
				}
			}
		}
		return samples[0].length;
	}

	void initLookupTableI(double[] coefs, int tz) {
		int nLookupTable = (coefs.length + 7) / 8;
		// allocate the table
		lookupTableI = new int[nLookupTable][256];
		// loop over each entry in the lookup table
		for (int t = 0; t < lookupTableI.length; t++) {
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
				lookupTableI[t][dsdSeq] = (int) Math.round(acc * scale);
			}
		}
	}

	public static void main(String... args) {
		int ratio = 32;
		if (args.length > 0)
			ratio = Integer.parseInt(args[0]);
		new DecoderInt().analyzeTables(ratio);
	}

	protected void analyzeTables(int ratio) {
		switch (ratio) {
		case 8:
			inspectTable(coefs_352, tzero_352);
			break;
		case 16:
			inspectTable(coefs_176, tzero_176);
			break;
		case 32:
			inspectTable(coefs_88, tzero_88);
			break;
		default:

		}
	}

	protected void inspectTable(double[] coefs, int tzero) {
		double maxP = 0, minP = 0, maxN = 0, minN = 0;
		double sc = Math.pow(2, 24) - 1;
		for (double cv : coefs) {
			if (cv > 0) {
				if (maxP == 0)
					maxP = cv;
				else if (minP == 0)
					minP = cv;
				else if (cv > maxP)
					maxP = cv;
				else if (cv < minP)
					minP = cv;
			} else {
				cv = -cv;
				if (maxN == 0)
					maxN = cv;
				else if (minP == 0)
					minN = cv;
				else if (cv > maxP)
					maxN = cv;
				else if (cv < minP)
					minP = cv;
			}
		}
		System.out.printf(" min %f,  max %f, delts %f, neg min %f, max %f, delta %f, scale %f, norm: %f/%f%n", minP,
				maxP, maxP - minP, minN, maxN, maxN - minN, sc, sc * (maxP - minP), sc * (maxN - minN));
	}

}
