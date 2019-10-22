package org.justcodecs.dsd;

import java.io.IOException;

import org.justcodecs.dsd.Decoder.DecodeException;

public class ChunkFVER extends BaseChunk {
	int version;

	@Override
	void read(DSDStream ds) throws DecodeException {		
		super.read(ds);
		try {
			version = ds.readInt(true);
			//System.out.printf("Ver 0%x%n", version);
		} catch (IOException e) {
			throw new DecodeException("", e);
		}
	}

	
}
