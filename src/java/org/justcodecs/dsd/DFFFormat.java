package org.justcodecs.dsd;

import java.io.IOException;

import org.justcodecs.dsd.Decoder.DecodeException;

public class DFFFormat extends DSDFormat<byte[]> {
	ChunkFRM8 frm;
	byte buff[];
	static final int block = 2048;

	@Override
	public
	void init(DSDStream ds) throws DecodeException {
		super.init(ds);
		BaseChunk c = BaseChunk.create(dsdStream, null);
		if (c instanceof ChunkFRM8 == false)
			throw new DecodeException("Invalid diff format, no FRAME chunk", null);
		//c.skip(dsdStream);
		frm = (ChunkFRM8) c;
		attrs.put("Artist", frm.artist);
		attrs.put("Title", frm.title);
		attrs.put("Album", frm.album);
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
				System.arraycopy(buff, bufPos, buff, 0, delta);
			int toRead = block * getNumChannels();
			if (toRead > frm.props.dsd.dataEnd - dsdStream.getFilePointer())
				toRead = (int) (frm.props.dsd.dataEnd - dsdStream.getFilePointer());
			dsdStream.readFully(buff, delta, toRead);
			bufPos = 0;
			bufEnd = toRead + delta;
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
		return (frm.props.dsd.dataEnd - frm.props.dsd.start) * (8 / getNumChannels());
	}

	@Override
	public int getNumChannels() {
		return frm.props.channels;
	}

	@Override
	void initBuffers(int overrun) {
		buff = new byte[(block + overrun) * getNumChannels()];
	}

	@Override
	boolean isMSB() {
		return true;
	}

	@Override
	byte[] getSamples() {
		return buff;
	}

	@Override
	void seek(long sampleNum) throws DecodeException {
		try {
			dsdStream.seek(frm.props.dsd.start + (sampleNum / 8)*getNumChannels());
			//System.out.printf("Satrt play %d for sample %d%n", dsdStream.getFilePointer(), sampleNum);
			bufPos = -1;
		} catch (IOException e) {
			throw new DecodeException("", e);
		}
	}

}
