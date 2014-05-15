package org.justcodecs.dsd;

import java.io.IOException;
import java.util.Arrays;

import org.justcodecs.dsd.Decoder.DecodeException;

public class DATAChunk {
	public int signature = Utils.bytesToInt((byte) 'd', (byte) 'a', (byte) 't', (byte) 'a');
	public long chunkSize;
	long dataStart, dataEnd;
	byte[][] data;
	
	protected DATAChunk(DSDStream ds) throws DecodeException {
		try {
		if (ds.readInt(true) != signature)
			throw new Decoder.DecodeException("Invalid signature for the chunk 'data'", null);
		chunkSize = ds.readLong(false);
		dataStart = ds.getFilePointer();
		dataEnd = dataStart + chunkSize - 12;
		} catch (IOException ioe) {
			throw new Decoder.DecodeException("I/O exception " + ioe, ioe);
		}
	}
	
	@Override
	public String toString() {
		return "DATAChunk [signature=" + signature + ", chunkSize=" + chunkSize + ", data=" + Arrays.toString(data)
				+ "]";
	}

	public static DATAChunk read(DSDStream ds) throws DecodeException {
		return new DATAChunk(ds);
	}

}
