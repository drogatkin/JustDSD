package org.justcodecs.dsd;

import java.io.IOException;

import org.justcodecs.dsd.Decoder.DecodeException;

public class ChunkFRM8 extends BaseChunk {
	ChunkPROP props;
	@Override
	void read(DSDStream ds) throws DecodeException {
		super.read(ds);
		try {
			ds.readFully(IDBuf, 0, 4);
			if ("DSD ".equals(new String(IDBuf)) == false)
				throw new DecodeException("Frame chunk isn't DSD", null);
			for (;;) {
				// read local chunks
				BaseChunk c = BaseChunk.create(ds, this);
				//System.out.printf("--->%s%n", c);
				if (c instanceof ChunkPROP) {
					props = (ChunkPROP)c;
					break;
				}
			}
		} catch (IOException e) {
			throw new DecodeException("", e);
		}
	}

}
