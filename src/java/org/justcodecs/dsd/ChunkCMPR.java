package org.justcodecs.dsd;

import java.io.IOException;

import org.justcodecs.dsd.Decoder.DecodeException;

public class ChunkCMPR extends BaseChunk {
	String compression;
	String description;
	@Override
	void read(DSDStream ds) throws DecodeException {
		super.read(ds);
		try {
			ds.readFully(IDBuf, 0, 4);
			compression = new String(IDBuf).trim();
			ds.readFully(IDBuf, 0, 1);
			int blen = IDBuf[0] & 255;
			byte buf[] = new byte[blen];
			ds.readFully(buf, 0, blen);
			description = new String(buf);
			skip(ds);
			//System.out.printf("Comp %s%n", description);
		} catch (IOException e) {
			throw new DecodeException("", e);
		}
	}
}
