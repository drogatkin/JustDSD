package org.justcodecs.dsd;

import org.justcodecs.dsd.Decoder.DecodeException;

public class ChunkDSTI extends BaseChunk {

	DSTFrameIndex[] entries;
	
	@Override
	void read(DSDStream ds) throws DecodeException {
		super.read(ds);
	   	
		skip(ds);
	}
	
	static class DSTFrameIndex {
		long offset;
		int len;
	}

}
