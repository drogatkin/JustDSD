package org.justcodecs.dsd;

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
		try {
			//dsdStream.seek(0x800*1 + dsdStream.getFilePointer());
			System.out.printf("Read from 0%x%n", dsdStream.getFilePointer());
			FrmHeader fh = new FrmHeader();
			fh.read(dsdStream);
			System.out.printf("%s%n", fh);
			//dsdStream.readFully(dstBuff, 0, 1);
			for (int f=0; f<fh.packet_info_count; f++) {
			dsdStream.readFully(dstBuff, 0, fh.getPackLen(f));
			System.out.printf("Pos at end 0%x%n", dsdStream.getFilePointer());
			//dsdStream.readFully(dstBuff, 0, 8);
			//dsdStream.readFully(dstBuff, 0, block);
			if (fh.getDataType(f) != DATA_TYPE_AUDIO)
				continue;
			dst.FramDSTDecode(dstBuff, buff, fh.getPackLen(f), f);
			}
			//System.out.printf("Pos at end 0%x%n", dsdStream.getFilePointer());
			bufPos = 0;
			bufEnd = dst.FrameHdr.MaxFrameLen * dst.FrameHdr.NrOfChannels;
		} catch (DSTException e) {
			throw new DecodeException("Problem in DST decoding", e);
		} catch (IOException e) {
			throw new DecodeException("I/O problem", e);
		}
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
			dstBuff = new byte[block];			
			try {
				dst.init(atoc.channel_count, atoc.sample_frequency/44100);
			} catch (DSTException e) {
				throw new RuntimeException("Can't initialize decoder", e);
			}
			buff = new byte[dst.FrameHdr.MaxFrameLen*dst.FrameHdr.NrOfChannels];
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
		} catch (IOException e) {
			throw new DecodeException("IO", e);
		}
	}

}
