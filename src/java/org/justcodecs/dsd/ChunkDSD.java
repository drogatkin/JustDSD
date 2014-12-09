package org.justcodecs.dsd;

import org.justcodecs.dsd.Decoder.DecodeException;

public class ChunkDSD extends BaseChunk {
	long dataEnd;
	//byte[][] data;
	@Override
	void read(DSDStream ds) throws DecodeException {
		super.read(ds);
		dataEnd = start+size;
		//skip(ds);
	}
}
