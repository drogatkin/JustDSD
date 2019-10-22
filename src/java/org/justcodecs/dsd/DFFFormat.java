package org.justcodecs.dsd;

import java.io.IOException;

import org.justcodecs.dsd.DSTDecoder.DSTException;
import org.justcodecs.dsd.Decoder.DecodeException;

public class DFFFormat extends DSDFormat<byte[]> {
	ChunkFRM8 frm;
	byte buff[];
	int block = 2048;
	long filePosition;

	// DST specific
	int dstFrmNo;
	DSTDecoder dst;
	byte[] dsdBuf;
	ChunkDSTF dstf;
	ChunkDSTC dstc;
	final int DSTF = Utils.bytesToInt((byte) 'D', (byte) 'S', (byte) 'T',
			(byte) 'F');
	final int DSTC = Utils.bytesToInt((byte) 'D', (byte) 'S', (byte) 'T',
			(byte) 'C');

	@Override
	public void init(DSDStream ds) throws DecodeException {
		super.init(ds);
		BaseChunk c = BaseChunk.create(dsdStream, metadataCharset);
		if (c instanceof ChunkFRM8 == false)
			throw new DecodeException("Invalid .dff format, no FRAME chunk",
					null);
		// c.skip(dsdStream);
		frm = (ChunkFRM8) c;
		attrs.putAll(frm.metaAttrs);
		if (isDST()) {
			try {
				dst = new DSTDecoder();
				dst.init(getNumChannels(), getSampleRate() / 44100);
			} catch (DSTException e) {
				dst = null;
				throw new DecodeException("Coudn't initialize DST decoder", e);
			}
		}
		/*
		 * try { fo = new FileOutputStream("test.dffdst"); } catch
		 * (FileNotFoundException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 */
	}

	@Override
	boolean readDataBlock() throws DecodeException {
		if (isDST()) {
			return decodeDSTDataBlock();
		}
		try {
			if (filePosition >= frm.props.dsd.dataEnd)
				return false;
			if (bufPos < 0)
				bufPos = 0;
			int delta = bufEnd - bufPos;

			if (delta > 0)
				System.arraycopy(buff, bufPos, buff, 0, delta);
			int toRead = block * getNumChannels();
			if (toRead > frm.props.dsd.dataEnd - filePosition)
				toRead = (int) (frm.props.dsd.dataEnd - filePosition);
			dsdStream.readFully(buff, delta, toRead);
			filePosition += toRead;
			// System.out.printf("%s%n", Utils.toHexString(0, 100, buff));
			// if (true)
			// throw new DecodeException("test", null);
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
		if (!isDST())
			return (frm.props.dsd.dataEnd - frm.props.dsd.start)
					* (8 / getNumChannels());
		else
			return frm.props.dst.info.numFrames / frm.props.dst.info.rate
					* getSampleRate();
	}

	@Override
	public int getNumChannels() {
		return frm.props.channels;
	}

	@Override
	void initBuffers(int overrun) {
		if (!isDST())
			buff = new byte[(block + overrun) * getNumChannels()];
		else {
			buff = new byte[(dst.FrameHdr.MaxFrameLen + overrun)
					* dst.FrameHdr.NrOfChannels];
			dsdBuf = new byte[dst.FrameHdr.MaxFrameLen
					* dst.FrameHdr.NrOfChannels];
			dstf = new ChunkDSTF();
			dstc = new ChunkDSTC();
		}
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
	boolean isDST() {
		return frm.props.dst != null;
	}

	@Override
	void seek(long sampleNum) throws DecodeException {
		if (isDST()) {
			seekDST(sampleNum);
			return;
		}
		// if (sampleNum < getSampleCount())
		try {
			 //System.out.printf("Start play %d for sample %d, frm %s%n",
			 //dsdStream.getFilePointer(), sampleNum, frm.props.dsd);
			filePosition = frm.props.dsd.start + (sampleNum / 8)
					* getNumChannels();
			dsdStream.seek(filePosition);
			bufPos = -1;
			bufEnd = 0;
		} catch (IOException e) {
			// TODO invalidate filePosition
			throw new DecodeException("", e);
		}
	}

	boolean decodeDSTDataBlock() throws DecodeException {
		dstFrmNo++;
		if (dstFrmNo >= frm.props.dst.info.numFrames)
			return false;
		try {
			if (dsdStream.readInt(true) == DSTF)
				dstf.read(dsdStream);
			else if (dsdStream.readInt(true) == DSTC) {
				dstc.read(dsdStream);
				if (dsdStream.readInt(true) != DSTF)
					new DecodeException("Unexpected chunk format", null);
				dstf.read(dsdStream);
			}

			if (bufPos < 0)
				bufPos = 0;
			int delta = bufEnd - bufPos;
			if (delta > 0)
				System.arraycopy(buff, bufPos, buff, 0, delta);
			int dlen = (int) (dst.FrameHdr.NrOfBitsPerCh
					* dst.FrameHdr.NrOfChannels / 8);
			if (delta + dlen >= buff.length) {
				bufPos = 0;
				bufEnd = delta;
			} else {
				dst.FramDSTDecode(dstf.data, dsdBuf, (int) dstf.size, dstFrmNo);
				System.arraycopy(dsdBuf, 0, buff, delta, dlen);
				bufPos = 0;
				bufEnd = delta + dlen;// (int) (dst.FrameHdr.NrOfBitsPerCh *
										// dst.FrameHdr.NrOfChannels / 8);
			}
		} catch (DSTException e) {
			throw new DecodeException("", e);
		} catch (IOException e) {
			throw new DecodeException("", e);
		}
		return true;
	}

	void seekDST(long sampleNum) throws DecodeException {
		try {
			if (sampleNum == 0) {
				dsdStream.seek(frm.props.dst.info.start
						+ frm.props.dst.info.size);
				// System.out.printf("Start play 0x%x for sample %d, frm %s total %d%n",
				// dsdStream.getFilePointer(),
				// dstFrmNo, frm.props.dst, frm.props.dst.info.numFrames);
			} else {
				int seekChunk = (int) (sampleNum * frm.props.dst.info.numFrames / getSampleCount());
				dsdStream.seek(frm.props.dst.info.start
						+ frm.props.dst.info.size);
				// position in file, read buff, find signature
				// or use ChunkDSTI when avail
				// System.out.printf("Start play 0x%x for sample %d, frm %d total %d%n",
				// dsdStream.getFilePointer(),
				// dstFrmNo, seekChunk, frm.props.dst.info.numFrames);
				// TODO crc chunk presence and adjust jump
				BaseChunk c = BaseChunk.create(dsdStream, (BaseChunk) null);
				if (c instanceof ChunkDSTC)
					seekChunk *= 2;
				else {
					c = BaseChunk.create(dsdStream, (BaseChunk) null);
					if (c instanceof ChunkDSTC)
						seekChunk = 2 * (seekChunk - 1);
					else
						seekChunk -= 2;
				}
				for (int i = 0; i < seekChunk; i++)
					// BaseChunk.create(dsdStream, (BaseChunk)null);
					BaseChunk.jump(dsdStream);
				dstFrmNo = seekChunk;
			}
		} catch (IOException e) {
			// TODO invalidate filePosition
			throw new DecodeException("", e);
		}
	}
}
