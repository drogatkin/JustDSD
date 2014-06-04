package org.justcodecs.dsd;

import java.io.IOException;

import org.justcodecs.dsd.Decoder.DecodeException;

public class DISOFormat extends DSDFormat<byte[]> implements Scarletbook {

	byte buff[];
	int sectorSize;
	TOC toc;
	AreaTOC atoc;
	int block = SACD_LSN_SIZE - 32; // sectorSize
	byte[] header = new byte[32];
	
	@Override
	public void init(DSDStream ds) throws DecodeException {
		super.init(ds);
		toc = new TOC();
		try {
			try {
				ds.seek(START_OF_MASTER_TOC * SACD_LSN_SIZE);
				toc.read(ds);
				sectorSize = SACD_LSN_SIZE;
			} catch (DecodeException de) {
				ds.seek(START_OF_MASTER_TOC * SACD_PSN_SIZE + 12);
				toc.read(ds);
				sectorSize = SACD_PSN_SIZE;
			}
			System.out.printf("SACD image %s%n", toc);
			ds.seek(toc.area1Toc1Start * sectorSize);
			atoc = new AreaTOC();
			atoc.read(ds);
			if (!atoc.stereo) {
				ds.seek(toc.area_2_toc_1_start * sectorSize);
				if (!atoc.stereo)
					throw new DecodeException("No two channels tracks found", null);
			}
			System.out.printf("Area %s%n", atoc);

		} catch (IOException ioe) {
			throw new DecodeException("IO", ioe);
		}
	}

	@Override
	boolean readDataBlock() throws DecodeException {
		try {
			if (dsdStream.getFilePointer() >= atoc.track_end*sectorSize)
				return false;
			if (bufPos < 0)
				bufPos = 0;
			int delta = bufEnd - bufPos;

			if (delta > 0)
				System.arraycopy(buff, bufPos, buff, 0, delta);
			int toRead = block;
			if (toRead > atoc.track_end*sectorSize - dsdStream.getFilePointer())
				toRead = (int) (atoc.track_end*sectorSize - dsdStream.getFilePointer());
			dsdStream.readFully(header, 0, header.length);
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
		if (atoc == null)
			return 0;
		return atoc.sample_frequency;
	}

	@Override
	public long getSampleCount() {
		if (atoc == null)
			return 0;
		return (atoc.minutes * 60 + atoc.seconds + atoc.frames / SACD_FRAME_RATE) * getSampleRate();
	}

	@Override
	public int getNumChannels() {
		if (atoc == null)
			return 0;
		return atoc.channel_count;
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
			if (sampleNum == 0) {
				dsdStream.seek(atoc.track_start * sectorSize);
			}
		} catch (IOException e) {
			throw new DecodeException("IO", e);
		}
	}

}
