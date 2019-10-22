package org.justcodecs.dsd;

import java.io.IOException;

import org.justcodecs.dsd.Decoder.DecodeException;

public class DSDChunk {
	protected DSDChunk(DSDStream ds) throws DecodeException {
		try {
			ds.readFully(buf, 0, 4);
			if (signature != Utils.bytesToInt(buf))
				throw new Decoder.DecodeException("Invalid signature for the block " + new String(buf), null);
			chunkSize = ds.readLong(false);
			if (chunkSize != 28)
				throw new Decoder.DecodeException("Size of the chunk " + chunkSize + " mismatches specification 28",
						null);
			size = ds.readLong(false);
			if (size != ds.length())
				throw new Decoder.DecodeException("Size of the DSD file " + ds.length() + " mismatched to claimed "
						+ size, null);
			metadataOffs = ds.readLong(false);
			if (metadataOffs < 0 || metadataOffs > size)
				throw new Decoder.DecodeException("Wrong metadata offset " + metadataOffs, null);
		} catch (IOException ioe) {
			throw new Decoder.DecodeException("I/O exception " + ioe, ioe);
		}
	}

	public static int signature = Utils.bytesToInt((byte) 'D', (byte) 'S', (byte) 'D', (byte) ' ');
	public long size;
	public long metadataOffs;
	public long chunkSize = 28;
	protected byte[] buf = new byte[4];

	public static DSDChunk read(DSDStream ds) throws DecodeException {
		return new DSDChunk(ds);
	}
}
