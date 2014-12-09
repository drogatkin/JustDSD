package org.justcodecs.dsd;

import java.io.FileOutputStream;
import java.io.IOException;

import org.justcodecs.dsd.Decoder.DecodeException;

public class DFFFormat extends DSDFormat<byte[]> {
	ChunkFRM8 frm;
	byte buff[];
	int block = 2048;
	long filePosition;

	@Override
	public void init(DSDStream ds) throws DecodeException {
		super.init(ds);
		BaseChunk c = BaseChunk.create(dsdStream, metadataCharset);
		if (c instanceof ChunkFRM8 == false)
			throw new DecodeException("Invalid diff format, no FRAME chunk", null);
		//c.skip(dsdStream);
		frm = (ChunkFRM8) c;
		attrs.putAll(frm.metaAttrs);
		/*try {
			fo = new FileOutputStream("test.dffdst");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}

	FileOutputStream fo;
	int cnt;

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

			if (fo != null) {
				fo.write(buff, delta, toRead);
				cnt += toRead;
				if (cnt > 200 * 1024) {
					fo.close();
					fo = null;
				}
			}
			//System.out.printf("%s%n", Utils.toHexString(0, 100, buff));
			//if (true)
			//throw new DecodeException("test", null);
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
	boolean isDST() {
		return frm.props.dst != null;
	}

	@Override
	void seek(long sampleNum) throws DecodeException {
		if (isDST()) {
			seekDST(sampleNum);
			return;
		}
		//if (sampleNum < getSampleCount())
		try {
			//System.out.printf("Start play %d for sample %d, frm %s%n", dsdStream.getFilePointer(), sampleNum, frm.props.dsd);
			filePosition = frm.props.dsd.start + (sampleNum / 8) * getNumChannels();
			dsdStream.seek(filePosition);
			bufPos = -1;
			bufEnd = 0;
		} catch (IOException e) {
			// TODO invalidate filePosition
			throw new DecodeException("", e);
		}
	}

	boolean decodeDSTDataBlock() throws DecodeException {
		return false;
	}

	void seekDST(long sampleNum) throws DecodeException {
		try {
			if (sampleNum == 0) {
				dsdStream.seek(frm.props.dst.info.start);
			}
		} catch (IOException e) {
			// TODO invalidate filePosition
			throw new DecodeException("", e);
		}
	}
}
