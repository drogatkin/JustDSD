package org.justcodecs.dsd;

import java.io.IOException;

import org.justcodecs.dsd.Decoder.DecodeException;

public class ChunkFRTE extends BaseChunk {
	
	int numFrames;
	int rate;
	
	@Override
	void read(DSDStream ds) throws DecodeException {
		super.read(ds);
		try {
			numFrames = ds.readInt(true);
			rate = ds.readShort(true);
			//skip(ds);
		} catch (IOException e) {
			throw new DecodeException("", e);
		}
	}
}
