package org.justcodecs.dsd;

import java.io.IOException;

import org.justcodecs.dsd.Decoder.DecodeException;

public class DSFFormat extends DSDFormat<byte[][]> {
	protected FMTChunk fmt;
	protected DATAChunk dc;
	protected MetadataChunk md;

	@Override
	public void init(DSDStream ds) throws DecodeException {
		super.init(ds);
		DSDChunk dsd = DSDChunk.read(dsdStream);
		fmt = FMTChunk.read(dsdStream);
		//System.out.printf("FMT:%s%n", fmt);
		dc = DATAChunk.read(dsdStream);
		if (dsd.metadataOffs > 0) {
			md = new MetadataChunk(dsd.metadataOffs, metadataCharset);
			try {
				md.read(ds);
				if (md.attrs != null)
					attrs.putAll(md.attrs);
			} catch (IOException e) {
			}
		}
	}

	@Override
	public int getSampleRate() {
		if (fmt != null)
			return fmt.sampleFreq;
		return 0;
	}

	@Override
	public long getSampleCount() {
		if (fmt != null)
			return fmt.sampleCount;

		return 0;
	}

	@Override
	boolean readDataBlock() throws DecodeException {
		//System.out.printf("read block%n");
		try {
			if (dsdStream.getFilePointer() >= dc.dataEnd)
				return false;
			if (bufPos < 0)
				bufPos = 0;
			int delta = bufEnd - bufPos;
			for (int c = 0; c < fmt.channelNum; c++) {
				if (delta > 0)
					System.arraycopy(dc.data[c], bufPos, dc.data[c], 0, delta);
				dsdStream.readFully(dc.data[c], delta, fmt.blockSize);
			}
			bufPos = 0;
			bufEnd = fmt.blockSize + delta;
		} catch (IOException e) {
			throw new DecodeException("IO exception at reading samples", e);
		}
		return true;
	}

	@Override
	void initBuffers(int overrun) {
		dc.data = new byte[fmt.channelNum][fmt.blockSize + overrun];
	}

	@Override
	boolean isMSB() {
		if (fmt != null)
			return fmt.bitPerSample == 8;
		return false;
	}

	@Override
	public int getNumChannels() {
		if (fmt != null)
			return fmt.channelNum;
		return 0;
	}

	@Override
	byte[][] getSamples() {
		return dc.data;
	}

	@Override
	void seek(long sampleNum) throws DecodeException {
		try {
			if (sampleNum == 0)
				dsdStream.seek(dc.dataStart);
			else if (sampleNum > 0 && sampleNum < getSampleCount()){
				// no accuracy for position in block
				long block = sampleNum / (fmt.blockSize * 8);
				dsdStream.seek(dc.dataStart + block * fmt.blockSize * fmt.channelNum);
				//throw new DecodeException("Pending", null);
			} else 
				throw new DecodeException("Trying to seek non existing sample "+sampleNum, null);
			bufPos = -1;
			bufEnd = 0;
		} catch (IOException e) {
			throw new DecodeException("", e);
		}
	}

}
