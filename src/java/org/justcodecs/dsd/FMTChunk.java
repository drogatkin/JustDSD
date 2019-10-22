package org.justcodecs.dsd;

import java.io.IOException;

import org.justcodecs.dsd.Decoder.DecodeException;

public class FMTChunk {
	public int signature = Utils.bytesToInt((byte) 'f', (byte) 'm', (byte) 't', (byte) ' ');
	public int version;
	public long chunkSize = 52;
	public int ID;
	public int channelType;
	public int channelNum;
	public int sampleFreq;
	public int bitPerSample;
	public long sampleCount;
	public int blockSize;

	protected FMTChunk(DSDStream ds) throws DecodeException {
		try {
			if (ds.readInt(true) != signature)
				throw new Decoder.DecodeException("Invalid signature for the chunk 'fmt '", null);
			chunkSize = ds.readLong(false);
			if (chunkSize < 52 || chunkSize > 256)
				throw new Decoder.DecodeException("Size of the chunk fmt " + chunkSize + " looks out of spec", null);
			chunkSize -= (4 + 8);
			version = ds.readInt(false);
			chunkSize -= 4;
			ID = ds.readInt(false);
			chunkSize -= 4;
			channelType = ds.readInt(false);
			chunkSize -= 4;
			channelNum = ds.readInt(false);
			chunkSize -= 4;
			sampleFreq = ds.readInt(false);
			chunkSize -= 4;
			bitPerSample = ds.readInt(false);
			chunkSize -= 4;
			sampleCount = ds.readLong(false);
			chunkSize -= 8;
			blockSize = ds.readInt(false);
			chunkSize -= 4;
			if (chunkSize % 4 != 0)
				throw new Decoder.DecodeException("Reserve isn't matching 4 bytes boundary " + chunkSize, null);
			for (int i = 0, n = (int) (chunkSize / 4); i < n; i++)
				ds.readInt(false);
			chunkSize = 0;
		} catch (IOException ioe) {
			throw new Decoder.DecodeException("I/O exception " + ioe, ioe);
		}
	}

	@Override
	public String toString() {
		return "FMTChunk [signature=" + signature + ", version=" + version + ", chunkRemained=" + chunkSize + ", ID=" + ID
				+ ", channelType=" + channelType + ", channelNum=" + channelNum + ", sampleFreq=" + sampleFreq
				+ ", bitPerSample=" + bitPerSample + ", sampleCount=" + sampleCount + ", blockSize=" + blockSize + "]";
	}

	public static FMTChunk read(DSDStream ds) throws DecodeException {
		return new FMTChunk(ds);
	}
}
