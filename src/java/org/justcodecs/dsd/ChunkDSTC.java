package org.justcodecs.dsd;

import org.justcodecs.dsd.Decoder.DecodeException;

public class ChunkDSTC extends BaseChunk {
	@Override
	void read(DSDStream ds) throws DecodeException {
		super.read(ds);
		
		skip(ds);
	}
}
