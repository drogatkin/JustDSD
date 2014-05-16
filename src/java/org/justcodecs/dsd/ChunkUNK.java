package org.justcodecs.dsd;

import org.justcodecs.dsd.Decoder.DecodeException;

public class ChunkUNK extends BaseChunk {
String IDS;
	public ChunkUNK() {
		ID = Utils.bytesToInt(IDBuf);
		IDS = new String(IDBuf);
	}

	@Override
	void read(DSDStream ds) throws DecodeException {
		super.read(ds);
		skip(ds);
	}

	@Override
	public String toString() {
		return String.format("ChunkUNK 0%x(%s) of %d/0%x", ID, IDS, size, start);
	}

	
}
