package org.justcodecs.dsd;

import java.io.IOException;

import org.justcodecs.dsd.Decoder.DecodeException;

public class ChunkDSTF extends BaseChunk {

	@Override
	void read(DSDStream ds) throws DecodeException {
		super.read(ds);
		int pad = 0;
		if ((size & 1) == 1)
			pad = 1;
		if (data == null || size + pad > data.length)
			data = new byte[(int) (size + pad)];
		try {
			ds.readFully(data, 0, (int) (size + pad));
		} catch (IOException e) {
			throw new DecodeException("", e);
		}
	}
}
