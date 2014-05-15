package org.justcodecs.dsd;

import java.io.IOException;

import org.justcodecs.dsd.Decoder.DecodeException;

public class DFFFormat extends DSDFormat {
	ChunkFRM8 frm;
	byte buff[][] = new byte[1][];
	static final int block = 2048;

	@Override
	void init(DSDStream ds) throws DecodeException {
		super.init(ds);
		BaseChunk c = BaseChunk.create(dsdStream);
		if (c instanceof ChunkFRM8 == false)
			throw new DecodeException("Invalid diff format, no FRAME chunk", null);
		//c.skip(dsdStream);
		frm = (ChunkFRM8) c;
	}

	@Override
	boolean readDataBlock() throws DecodeException {
		try {
			if (dsdStream.getFilePointer() >= frm.props.dsd.dataEnd)
				return false;
			if (bufPos < 0)
				bufPos = 0;
			int delta = bufEnd - bufPos;

			if (delta > 0)
				System.arraycopy(buff[0], bufPos, buff[0], 0, delta);
			dsdStream.readFully(buff[0], delta, block * getNumChannels());

			bufPos = 0;
			bufEnd = block * getNumChannels() + delta;
		} catch (IOException e) {
			throw new DecodeException("IO exception at reading samples", e);
		}
		return true;
	}

	@Override
	public int getSampleRate() {
		return frm.props.sampleRate;
	}

	@Override
	public long getSampleCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumChannels() {
		return frm.props.channels;
	}

	@Override
	void initBuffers(int overrun) {
		buff[0] = new byte[(block + overrun) * getNumChannels()];

	}

	@Override
	boolean isMSB() {
		return false;
	}

	@Override
	byte[][] getSamples() {
		return buff;
	}

}
