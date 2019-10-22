package org.justcodecs.dsd;

import java.io.IOException;
import java.util.HashMap;

import org.justcodecs.dsd.Decoder.DecodeException;

public class ChunkFRM8 extends BaseChunk {
	ChunkPROP props;
	HashMap<String, Object> metaAttrs;
	String encoding;
	
	@Override
	void read(DSDStream ds) throws DecodeException {
		super.read(ds);
		try {
			ds.readFully(IDBuf, 0, 4);
			if ("DSD ".equals(new String(IDBuf)) == false)
				throw new DecodeException("Frame chunk isn't DSD", null);
			metaAttrs = new HashMap<String, Object>();
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
