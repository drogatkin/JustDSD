package org.justcodecs.dsd;

import org.justcodecs.dsd.Decoder.DecodeException;

public class ChunkDST extends BaseChunk {
	ChunkFRTE info;
	@Override
	void read(DSDStream ds) throws DecodeException {
		super.read(ds);
		info = (ChunkFRTE)BaseChunk.create(ds, this);
		skip(ds);
	}
}
