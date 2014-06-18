package org.justcodecs.dsd;

import java.io.FileOutputStream;
import java.io.IOException;

import org.justcodecs.dsd.DSTDecoder.DSTException;
import org.justcodecs.dsd.Decoder.DecodeException;

public class DISOFormat extends DSDFormat<byte[]> implements Scarletbook {

	byte buff[];
	int sectorSize;
	TOC toc;
	AreaTOC atoc;
	int trackDuration;
	int frmHdrSize;
	int block = SACD_LSN_SIZE - frmHdrSize; // sectorSize 
	byte[] header;
	int currentFrame;
	DSTDecoder dst;

	byte[] dstBuff;
	byte dstPakBuf[] = new byte[SACD_LSN_SIZE];
	byte dsdBuf[] = new byte[1024 * 64]; //??
	int dstLen;
	boolean dstStart;
	FrmHeader frmHeader;
	int hdrIdx;
	int lastFrm;

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
			//System.out.printf("SACD image %s at sector %d%n", toc, sectorSize);
			ds.seek(toc.area1Toc1Start * sectorSize);
			atoc = new AreaTOC();
			atoc.read(ds);
			if (!atoc.stereo) {
				ds.seek(toc.area_2_toc_1_start * sectorSize);
				if (!atoc.stereo)
					throw new DecodeException("No two channels tracks found", null);
			}
			if (atoc.frame_format == FRAME_FORMAT_DST) {
				dst = new DSTDecoder();
				try {
					dst.init(atoc.channel_count, atoc.sample_frequency / 44100);
				} catch (DSTException e) {
					throw new DecodeException("Coudn't initialize DST decoder", e);
				}
				//throw new DecodeException("DST compression isn't supported yet", null);
			}
			if (atoc.frame_format == FRAME_FORMAT_DSD_3_IN_16)
				frmHdrSize = 284;
			else if (atoc.frame_format == FRAME_FORMAT_DSD_3_IN_14)
				frmHdrSize = 32;
			//throw new DecodeException("DSS 3 in 16 isn't supported yet", null);
			//System.out.printf("Area-> %s%n", atoc);
			ds.seek((START_OF_MASTER_TOC + 1) * (SACD_LSN_SIZE));
			CDText ctx = new CDText();
			ctx.read(ds, atoc.locales[0].encoding);
			TrackText ttx = new TrackText(atoc.track_count);
			TrackTime tm = new TrackTime();
			for (int i = 1; i < atoc.size; i++) {
				ds.seek((toc.area1Toc1Start + i) * sectorSize);
				try {
					ttx.read(ds, atoc.locales[0].encoding);
					//System.out.printf("tt-> %s%n", ttx);
					continue;
				} catch (DecodeException de) {

				}
				ds.seek((toc.area1Toc1Start + i) * sectorSize);
				try {
					tm.read(ds);
					//System.out.printf("ttim-> %s%n", tm);
					//continue;
				} catch (DecodeException de) {

				}
			}
			for (int i = 0; i < ttx.infos.length; i++) {
				ttx.infos[i].start = tm.getStart(i);
				ttx.infos[i].duration = tm.getDuration(i);
			}
			trackDuration = tm.getStart(ttx.infos.length - 1) + tm.getDuration(ttx.infos.length - 1);
			attrs.put("Artist", ctx.textInfo.get("album_artist"));
			attrs.put("Title", ctx.textInfo.get("disc_title"));
			attrs.put("Album", ctx.textInfo.get("album_title"));
			attrs.put("Tracks", ttx.infos);
			attrs.put("Year", new Integer(toc.disc_date_year));
			attrs.put("Genre", toc.albumGenre[0].genre);
		} catch (IOException ioe) {
			throw new DecodeException("IO", ioe);
		}
	}

	@Override
	boolean readDataBlock() throws DecodeException {
		if (dst != null) {
			readDSTDataBlock();
			return true;
		}
		if (currentFrame > (atoc.track_end - atoc.track_start))
			return false;
		try {
			if (bufPos < 0)
				bufPos = 0;
			int delta = bufEnd - bufPos;
			if (delta > 0)
				System.arraycopy(buff, bufPos, buff, 0, delta);
			dsdStream.readFully(header, 0, header.length);
			dsdStream.readFully(buff, delta, block);
			currentFrame++;
			bufPos = 0;
			bufEnd = block + delta;
		} catch (IOException e) {
			throw new DecodeException("IO exception at reading samples", e);
		}
		return true;
	}

	void readDSTDataBlock() throws DecodeException {
		do {
			try {
				if (dstStart) {
					dstStart = false;
					//System.out.printf("Reading header at %x %n", dsdStream.getFilePointer());
					frmHeader = new FrmHeader();
					frmHeader.read(dsdStream);
					//System.out.printf("header: %s%n", frmHeader);
					if (frmHeader.packet_info_count > 0) {
						hdrIdx = 0;
					} else {
						dstStart = true;
						dsdStream.readFully(dstPakBuf, 0, SACD_LSN_SIZE - frmHeader.getSize());
						//System.out.printf("moved to %d%n", dsdStream.getFilePointer());
						continue;
					}
				}
				if (frmHeader.isFrameStart(hdrIdx)) {
					// completing current buffer
					if (dstLen > 0) { // complete previous
						//System.out.printf("Decoding 0x%x %x %x %x%n", dstBuff[0], dstBuff[1], dstBuff[2], dstBuff[3]);
						int delta = bufPos<0?0:bufEnd - bufPos;
						dst.FramDSTDecode(dstBuff, dsdBuf, dstLen, lastFrm);
						if (delta > 0)
							System.arraycopy(buff, bufPos, buff, 0, delta);
						//System.out.printf("filling from %d for %d bytes%n", delta, dst.FrameHdr.MaxFrameLen * dst.FrameHdr.NrOfChannels);
						//int dsdLen = (int) (dst.FrameHdr.NrOfBitsPerCh * dst.FrameHdr.NrOfChannels / 8);
						int dsdLen=dst.FrameHdr.MaxFrameLen * dst.FrameHdr.NrOfChannels;
						System.arraycopy(dsdBuf, 0, buff, delta, dsdLen);
						bufPos = 0;
						bufEnd = delta + dsdLen;
						dstLen = 0;
						return;
					}
				}
				//System.out.printf("adding to dst from %d, len %d, idx %d at %x%n", dstLen, frmHeader.getPackLen(hdrIdx), hdrIdx, dsdStream.getFilePointer()); 
				dsdStream.readFully(dstBuff, dstLen, frmHeader.getPackLen(hdrIdx));
				if (frmHeader.getDataType(hdrIdx) == 2)
					dstLen += frmHeader.getPackLen(hdrIdx);
				lastFrm = frmHeader.getFrames(hdrIdx);
				hdrIdx++;
				if (hdrIdx >= frmHeader.packet_info_count) {
					// advance to next block
					dstStart = true;
					int skip = frmHeader.getPackLen(0);
					for (int i=1; i< frmHeader.packet_info_count; i++)
						skip += frmHeader.getPackLen(i);
					dsdStream.readFully(dstPakBuf, 0,
							SACD_LSN_SIZE - frmHeader.getSize() - skip);
					//System.out.printf("skipping to %x using %d %d%n", dsdStream.getFilePointer(), SACD_LSN_SIZE - frmHeader.getSize() - skip, skip);
				}
			} catch (DSTException e) {
				throw new DecodeException("Problem in DST decoding", e);
			} catch (IOException e) {
				throw new DecodeException("I/O problem", e);
			}
		} while (true);
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
		if (trackDuration > 0)
			return (long) trackDuration * getSampleRate();
		return (long) (atoc.minutes * 60 + atoc.seconds + atoc.frames / SACD_FRAME_RATE) * getSampleRate();
	}

	@Override
	public int getNumChannels() {
		if (atoc == null)
			return 0;
		return atoc.channel_count;
	}

	@Override
	void initBuffers(int overrun) {
		if (frmHdrSize == 0 && dst == null)
			throw new IllegalStateException("Area TOC wasn't processed yet");
		block = SACD_LSN_SIZE - frmHdrSize;
		if (dst == null)
			buff = new byte[block + (overrun * getNumChannels())];
		else {
			dstBuff = new byte[MAX_DST_SIZE]; //MAX_DST_SIZE
			buff = new byte[1024 * 64/*dst.FrameHdr.MaxFrameLen * dst.FrameHdr.NrOfChannels*/];
		}
		header = new byte[frmHdrSize];
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
			} else if (sampleNum > 0 && sampleNum < getSampleCount()) {
				if (true) {
					currentFrame = (int) (sampleNum * (atoc.track_end - atoc.track_start) / getSampleCount());
					dsdStream.seek((long) (atoc.track_start + currentFrame) * sectorSize);
					//System.out.printf("Seek %ds,  len %db, tot %ds%n", sampleNum / getSampleRate(), atoc.track_end
					//	- atoc.track_start, getSampleCount() / getSampleRate());
				} else {
					// no accuracy for position in block
					//block * 8 / getNumChannels();
					long bn = sampleNum / block / 8 * getNumChannels();
					if (atoc.track_end <= bn)
						throw new DecodeException("Trying to after end sector " + atoc.track_end, null);
					dsdStream.seek((long) (atoc.track_start + bn) * SACD_LSN_SIZE);//sectorSize);
				}
			} else
				throw new DecodeException("Trying to seek non existing sample " + sampleNum, null);
			bufPos = -1;
			bufEnd = 0;
			dstStart = true;
		} catch (IOException e) {
			throw new DecodeException("IO", e);
		}
	}

}
