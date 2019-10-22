package org.justcodecs.dsd;

import java.io.IOException;

import org.justcodecs.dsd.Decoder.DecodeException;

public class ChunkFS extends BaseChunk {
	int sampleRate;

	@Override
	void read(DSDStream ds) throws DecodeException {
		super.read(ds);
		try {
			sampleRate = ds.readInt(true);
			//System.out.printf("Rate %d%n", sampleRate);
		} catch (IOException e) {
			throw new DecodeException("", e);
		}
	}
}
