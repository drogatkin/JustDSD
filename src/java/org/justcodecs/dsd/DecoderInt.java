package org.justcodecs.dsd;


public class DecoderInt extends Decoder implements FiltersInt {
	int fir_ctables[][];

	@Override
	protected int initLookupTable() throws DecodeException {
		return 0;
	}

	int getSamples2(int scale, int tpdfDitherPeakAmplitude, int clipAmplitude, int[][] samples) throws DecodeException {
		if (samples.length < dsdf.getNumChannels())
			throw new DecodeException("Allocated sample buffers less than number of channels", null);

		return 0;
	}

}
