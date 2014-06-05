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
			//System.out.printf("SACD image %s at sector %d%n", toc, sectorSize);
			ds.seek(toc.area1Toc1Start * sectorSize);
			atoc = new AreaTOC();
			atoc.read(ds);
			if (!atoc.stereo) {
				ds.seek(toc.area_2_toc_1_start * sectorSize);
				if (!atoc.stereo)
					throw new DecodeException("No two channels tracks found", null);
			}
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
			for (int i=0; i<ttx.infos.length; i++) {
				ttx.infos[i].start = tm.getStart(i);
				ttx.infos[i].duration = tm.getDuration(i);
			}
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
		long end = (long) atoc.track_end * sectorSize;
		try {
			if (dsdStream.getFilePointer() >= end)
				return false;
			if (bufPos < 0)
				bufPos = 0;
			int delta = bufEnd - bufPos;

			if (delta > 0)
				System.arraycopy(buff, bufPos, buff, 0, delta);
			int toRead = block;
			if (toRead > end - dsdStream.getFilePointer())
				toRead = (int) (end - dsdStream.getFilePointer());
			dsdStream.readFully(header, 0, header.length);
			// header[0] -> audioheader as
			//   packet_info_count will follow of size AUDIO_PACKET_INFO_SIZE each
			//  and then frame_info_count of AUDIO_FRAME_INFO_SIZE - 1
			/*byte  packet_info_count = (byte) ((header[0] >> 5) & 7);
			byte  frame_info_count = (byte) ((header[0] >> 2) & 7);
			boolean dst_encoded   = (header[0] & 0x01) == 1;
			System.out.printf("Frame header packets %d frames %d dst %b, total %d raw %x%n", packet_info_count, frame_info_count, dst_encoded,
					1+packet_info_count*AUDIO_PACKET_INFO_SIZE+ frame_info_count*AUDIO_FRAME_INFO_SIZE, header[0]);
			if (!dst_encoded)
				throw new DecodeException("", null);*/
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
		return (long)(atoc.minutes * 60 + atoc.seconds + atoc.frames / SACD_FRAME_RATE) * getSampleRate();
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
			} else if (sampleNum > 0 && sampleNum < getSampleCount()){
				// no accuracy for position in block
				long bn = sampleNum / block / 8 * getNumChannels() ; // TODO check if bn out of range
				if (atoc.track_end <= bn)
					throw new DecodeException("Trying to after end sector "+atoc.track_end, null);
				dsdStream.seek((long)(atoc.track_start+bn) * sectorSize);
				//throw new DecodeException("Pending block "+bn+" seek "+(long)(atoc.track_start+bn) * sectorSize, null);
			} else 
				throw new DecodeException("Trying to seek non existing sample "+sampleNum, null);
			bufPos = -1;
			bufEnd = 0;
		} catch (IOException e) {
			throw new DecodeException("IO", e);
		}
	}

}
