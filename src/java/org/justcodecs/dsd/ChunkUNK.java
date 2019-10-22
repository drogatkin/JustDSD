package org.justcodecs.dsd;

import java.io.IOException;
import java.math.BigInteger;

import org.justcodecs.dsd.Decoder.DecodeException;

public class ChunkUNK extends BaseChunk {
	String IDS;

	public ChunkUNK() {
		ID = Utils.bytesToInt(IDBuf);
		IDS = new String(IDBuf);
	}

	@Override
	void read(DSDStream ds) throws DecodeException {
		/*byte[] ibuf = new byte[100];
		try {
			ds.readFully(ibuf);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BigInteger bi = new BigInteger(ibuf);
		System.out.printf("Chunk 0x%x:0x%s%n", ID,bi.toString(16)+"\n"+new String(ibuf));*/
		if (ID == 0x637565) {
			try {
				ds.readByte();
			} catch (IOException e) {
				throw new DecodeException("cue", e);
			}
		}
		super.read(ds);
		
		skip(ds);
	}

	@Override
	public String toString() {
		return String.format("ChunkUNK 0%x(%s) of %d/0%x", ID, IDS, size, start);
	}

}
