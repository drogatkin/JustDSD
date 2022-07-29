package org.justcodecs.dsd;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import org.justcodecs.dsd.DSTDecoder.DSTException;
import org.justcodecs.dsd.Decoder.DecodeException;

public class DISOFormat extends DSDFormat<byte[]> implements Scarletbook, Runnable {
	final static int QUEUE_SIZE = 4;
	byte buff[];
	int sectorSize;
	int sectorStartOffset;
	TOC toc;
	AreaTOC atoc;
	int frmHdrSize;
	int block, tail;
	byte[] header;
	int currentFrame;
	int textDuration;
	// DST related
	DSTDecoder dst;
	byte[] dstBuff;
	byte dstPakBuf[] = new byte[SACD_LSN_SIZE];
	byte dsdBuf[];
	int dstLen;
	boolean dstStart;
	FrmHeader frmHeader;
	int hdrIdx;
	int lastFrm;
	boolean dstSeek;
	/// flow control
	Throwable runException;
	Thread processor;
	Thread readingThread;
	ArrayBlockingQueue<byte[]> decodedBuffs = new ArrayBlockingQueue<byte[]>(QUEUE_SIZE);
	ArrayBlockingQueue<byte[]> usedBuffs = new ArrayBlockingQueue<byte[]>(QUEUE_SIZE);
	long seekSample;
	boolean sleepRequested;

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
				sectorStartOffset = 12;
				//System.out.printf("!!%n");
			}
			//System.out.printf("[SACD] image %s at sector %d start area %x%n", toc, sectorSize, toc.area1Toc1Start * sectorSize);

			ds.seek(toc.area1Toc1Start * sectorSize + sectorStartOffset);
			atoc = new AreaTOC();
			if (toc.area1Toc1Start > 0)
				atoc.read(ds, toc.area1Toc1Start);
			if (!atoc.stereo) {
				ds.seek(toc.area_2_toc_1_start * sectorSize + sectorStartOffset);
				//System.out.printf("reading %x%n", toc.area_2_toc_1_start * sectorSize);
				atoc.read(ds, toc.area_2_toc_1_start);
				if (!atoc.stereo)
					throw new DecodeException("No two channels track found", null);
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
			ds.seek((START_OF_MASTER_TOC + 1) * (sectorSize) + sectorStartOffset);
			//System.out.printf("reading cdtext at0x%x size %d%n", (START_OF_MASTER_TOC + 1) * (sectorSize) + sectorStartOffset, atoc.size);
			CDText ctx = new CDText();
			ctx.read(ds, atoc.locales[0].encoding);
			TrackText ttx = new TrackText(atoc.track_count, sectorStartOffset == 0 ? 0 : 32);
			TrackTime tm = new TrackTime();
			boolean ttxf = false;
			for (int i = 1; i < atoc.size; i++) {
				//System.out.printf("checking at 0x%x%n", (atoc.start + i) * sectorSize + sectorStartOffset);
				if (!ttxf) {
					ds.seek((atoc.start + i) * sectorSize + sectorStartOffset);
					try {
						ttx.read(ds, atoc.locales[0].encoding);
						ttxf = true;
						continue;
					} catch (DecodeException de) {
						//System.out.printf("header %s%n", de);
					}
				}
				ds.seek((atoc.start + i) * sectorSize + sectorStartOffset);
				try {
					tm.read(ds);
				} catch (DecodeException de) {
					//System.out.printf("header time %s%n", de);
				}
			}
			if (!ttxf)
				ttx.infos = new TrackInfo[0];
			for (int i = 0; i < ttx.infos.length; i++) {
				ttx.infos[i].start = tm.getStart(i);
				ttx.infos[i].duration = tm.getDuration(i);
			}
			if (ttx.infos.length > 0)
				textDuration = ttx.infos[ttx.infos.length - 1].start + ttx.infos[ttx.infos.length - 1].duration;
			attrs.put("Artist", ctx.textInfo.get("album_artist"));
			attrs.put("Title", ctx.textInfo.get("disc_title"));
			attrs.put("Album", ctx.textInfo.get("album_title"));
			attrs.put("Tracks", ttx.infos);
			attrs.put("Year", new Integer(toc.disc_date_year));
			attrs.put("Genre", toc.albumGenre[0].genre);
			//fo = new FileOutputStream("test.dst");
			currentFrame = 0;
			processor = null;
		} catch (IOException ioe) {
			throw new DecodeException("IO", ioe);
		}
	}
	
	//int it;
	@Override
	boolean readDataBlock() throws DecodeException {
		if (dst != null)
			return readDSTDataBlockAsync();
		if (currentFrame > (atoc.track_end - atoc.track_start))
			return false;
		//if (it > 5)
			//return false;
		if (bufPos < 0)
			bufPos = 0;
		int delta = bufEnd - bufPos;
		if (delta > 0)
			System.arraycopy(buff, bufPos, buff, 0, delta);
		try {
			long pp, pp1;
			//pp = dsdStream.getFilePointer();
			dsdStream.readFully(header, 0, header.length);
			//pp1 = dsdStream.getFilePointer();
			dsdStream.readFully(buff, delta, block);
			//if (tail > 0)
				//dsdStream.readFully(header, 0, tail);
			//System.out.printf("Start 0x%x data 0x%x  dirst 0x%x, last 0x%x%n", pp, pp1, buff[delta], buff[delta+block-1]);
			currentFrame++;
			bufPos = 0;
			bufEnd = block + delta - tail;
		} catch (IOException e) {
			throw new DecodeException("IO exception at reading samples", e);
		}
		//it++;
		return true;
	}

	//FileOutputStream fo ;
	//int cnt;	
	boolean readDSTDataBlock() throws DecodeException {
		do {
			try {
				if (dstStart) {
					if (currentFrame > (atoc.track_end - atoc.track_start))
						return false;
					dstStart = false;
					if (sectorStartOffset > 0)
						dsdStream.readFully(header, 0, sectorStartOffset);
					//System.out.printf("Reading header at %x %n", dsdStream.getFilePointer());
					//frmHeader = new FrmHeader();
					frmHeader.read(dsdStream);
					currentFrame++;
					//System.out.printf("header: %s%n", frmHeader);
					if (frmHeader.packet_info_count > 0) {
						hdrIdx = 0;
					} else {
						dstStart = true;
						dsdStream.readFully(dstPakBuf, 0, sectorSize - frmHeader.getSize() - sectorStartOffset);
						//System.out.printf("moved to %d%n", dsdStream.getFilePointer());
						continue;
					}
				}
				if (frmHeader.isFrameStart(hdrIdx)) {
					// completing current buffer
					if (dstLen > 0) { // complete previous
						//System.out.printf("Decoding 0x%x %x %x %x%n", dstBuff[0], dstBuff[1], dstBuff[2], dstBuff[3]);
						int delta = bufPos < 0 ? 0 : bufEnd - bufPos;
						dst.FramDSTDecode(dstBuff, dsdBuf, dstLen, lastFrm);
						if (delta > 0)
							System.arraycopy(buff, bufPos, buff, 0, delta);
						//System.out.printf("filling from %d for %d bytes%n", delta, dst.FrameHdr.MaxFrameLen * dst.FrameHdr.NrOfChannels);
						int dsdLen = (int) (dst.FrameHdr.NrOfBitsPerCh * dst.FrameHdr.NrOfChannels / 8);
						//int dsdLen=dst.FrameHdr.MaxFrameLen * dst.FrameHdr.NrOfChannels;						 
						System.arraycopy(dsdBuf, 0, buff, delta, dsdLen);
						/*
						int dsdLen = (int) dst.FrameHdr.NrOfBitsPerCh/8;
						for (int i=0; i<dst.FrameHdr.NrOfChannels; i++) {
							if (delta > 0)
								System.arraycopy(buff2[i], bufPos, buff2[i], 0, delta);
							dst.FramDSTDecode(dstBuff, buff2t, dstLen, lastFrm);
							System.arraycopy(buff2t[i], 0, buff2[i], delta, dsdLen);
						}*/
						bufPos = 0;
						bufEnd = delta + dsdLen;
						dstLen = 0;
						return true;
					}
					dstSeek = false;
				}
				//System.out.printf("adding to dst from %d, len %d, idx %d at %x%n", dstLen, frmHeader.getPackLen(hdrIdx), hdrIdx, dsdStream.getFilePointer());
				//try {
				dsdStream.readFully(dstBuff, dstLen, frmHeader.getPackLen(hdrIdx));
				//} catch(Exception e) {
				//System.out.printf("adding to dst from %d, len %d, idx %d of %d at x%x at frame %d%n", dstLen, frmHeader.getPackLen(hdrIdx), hdrIdx, frmHeader.packet_info_count, dsdStream.getFilePointer(), currentFrame);
				//}
				if (frmHeader.getDataType(hdrIdx) == 2 && !dstSeek)
					dstLen += frmHeader.getPackLen(hdrIdx);
				//dstSeek = false;
				lastFrm = frmHeader.getFrames(hdrIdx);
				hdrIdx++;
				if (hdrIdx >= frmHeader.packet_info_count) {
					// advance to next block
					dstStart = true;
					int skip = frmHeader.getPackLen(0);
					for (int i = 1; i < frmHeader.packet_info_count; i++)
						skip += frmHeader.getPackLen(i);
					skip = sectorSize - frmHeader.getSize() - skip - sectorStartOffset;
					if (skip > 0)
						dsdStream.readFully(dstPakBuf, 0, skip);
					else if (skip < 0)
						throw new DecodeException("Problem in DST decoding in frame " + currentFrame, null);
					/*try {
						if (SACD_LSN_SIZE - frmHeader.getSize() - skip > 0)
							dsdStream.readFully(dstPakBuf, 0, SACD_LSN_SIZE - frmHeader.getSize() - skip);
						else if (SACD_LSN_SIZE - frmHeader.getSize() - skip < 0)
							System.out.printf("negative skipping to %x using %d %d in frame %d%n",
									dsdStream.getFilePointer(), SACD_LSN_SIZE - frmHeader.getSize() - skip, skip,
									currentFrame);
					} catch (Exception e) {
						System.out.printf("excep skipping to %x using %d %d in frame %d%n", dsdStream.getFilePointer(),
								SACD_LSN_SIZE - frmHeader.getSize() - skip, skip, currentFrame);
					}*/
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
		tail = sectorSize - block - frmHdrSize - sectorStartOffset;
		if (tail > 0)
			block += tail;
		//System.out.printf("sec size: %d, hdr: %d, block: %d  ta: %d%n", sectorSize, frmHdrSize, block, tail);
		if (dst == null) {
			buff = new byte[block + (overrun * getNumChannels())];
			header = new byte[frmHdrSize+sectorStartOffset];
		} else {
			dstBuff = new byte[MAX_DST_SIZE]; //MAX_DST_SIZE
			buff = new byte[(dst.FrameHdr.MaxFrameLen + overrun) * dst.FrameHdr.NrOfChannels];
			dsdBuf = new byte[dst.FrameHdr.MaxFrameLen * dst.FrameHdr.NrOfChannels]; //??
			frmHeader = new FrmHeader();
			//for (int i = 0; i < QUEUE_SIZE; i++)
				//usedBuffs.offer(new byte[dst.FrameHdr.MaxFrameLen * dst.FrameHdr.NrOfChannels]);
			header = new byte[12];
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
		return dst != null;
	}

	@Override
	void seek(long sampleNum) throws DecodeException {
		final boolean local_debug = false;
		if (local_debug) {
			System.out.printf("Sample %d%n", sampleNum);
		}

		synchronized (this) {
			if (processor != null && processor.isAlive()) {
				if (seekSample < 0) {
					seekSample = sampleNum;
					return;
				}
			}
		}
		try {
			if (sampleNum == 0) {
				dsdStream.seek(atoc.track_start * sectorSize);
				//System.out.printf("Seek %d %x%n", atoc.track_start * sectorSize, atoc.track_start * sectorSize);
				//new Exception().printStackTrace();
			} else if (sampleNum > 0 && sampleNum < getSampleCount()) {
				//System.out.printf("actual samples %d (%ds) text samples %d (%ds) search %d(%ds)%n",getSampleCount(), getSampleCount()/getSampleRate(),
				//	((long)textDuration)*getSampleRate(), textDuration, sampleNum, sampleNum/getSampleRate());
				if (textDuration > 0) {
					sampleNum = Math.round(getTimeAdjustment() * sampleNum);
					//System.out.printf("adjusted %d (%ds) / %f%n", sampleNum, sampleNum/getSampleRate(), getTimeAdjustment());
				}
				if (sampleNum >= getSampleCount())
					throw new DecodeException("Trying to seek non existing sample " + sampleNum, null);
				currentFrame = (int) (sampleNum * (atoc.track_end - atoc.track_start) / getSampleCount());
				dsdStream.seek((long) (atoc.track_start + currentFrame) * sectorSize);
				//dstSeek = currentFrame > 0;
				/*System.out.printf("Seek %ds,  len %db, tot %ds frame %d 0f %d%n", sampleNum / getSampleRate(),
						atoc.track_end - atoc.track_start, getSampleCount() / getSampleRate(), currentFrame,
						atoc.track_end - atoc.track_start);*/
				if (isDST()) {
					if (local_debug)
						System.out.printf("Skip info tracks%n");
					do {
						if (sectorStartOffset > 0)
							dsdStream.readFully(header, 0, sectorStartOffset);
						frmHeader.read(dsdStream);
						if (frmHeader.frame_info_count == 0) {
							currentFrame++;
							dsdStream.seek((long) (atoc.track_start + currentFrame) * sectorSize);
						}
					} while (frmHeader.frame_info_count == 0);
					
					int seekSec = (int) (sampleNum / getSampleRate());
					int currentSec = frmHeader.getMinutes(0) * 60 + frmHeader.getSeconds(0);
					sampleNum += ((long)(seekSec - currentSec)) * getSampleRate();
					if (local_debug) {
						System.out.printf("Seek sec %d, current %d%n", seekSec, currentSec);
					}
					currentFrame = (int) (sampleNum * (atoc.track_end - atoc.track_start) / getSampleCount());
					if (local_debug) {
						System.out.printf("Current frm %d for sample %d%n", currentFrame, sampleNum);
						System.out.printf("Seeking %d starting %d plus current %d mul %d%n", (atoc.track_start + currentFrame) * sectorSize,
								atoc.track_start, currentFrame, sectorSize);
					}
					dsdStream.seek(((long) (atoc.track_start + currentFrame)) * sectorSize);
					dstSeek = currentFrame > 0;
				}
			} else
				throw new DecodeException("Trying to seek non existing sample " + sampleNum, null);
			//bufPos = -1;
			//bufEnd = 0;
			dstStart = true;
			dstLen = 0;
			seekSample = -1;
			//System.out.printf("Positioned to %x sector %d block %d%n", dsdStream.getFilePointer(), atoc.track_start, currentFrame);
		} catch (IOException e) {
			throw new DecodeException("IO", e);
		}
	}

	@Override
	public double getTimeAdjustment() {
		if (textDuration <= 0)
			return 1.0;
		return ((double) getSampleCount()) / textDuration / getSampleRate();
	}

	boolean readDSTDataBlockAsync() throws DecodeException {
		if (processor == null) {
			synchronized (this) {
				//if (usedBuffs.remainingCapacity() == 0)
				for (int i = usedBuffs.size(); i < QUEUE_SIZE; i++) // reassure empty buffs
					usedBuffs.offer(new byte[dst.FrameHdr.MaxFrameLen * dst.FrameHdr.NrOfChannels]);
				processor = new Thread(this);
				processor.setName("DST decoder");
				processor.setDaemon(true);
				processor.start();
				//System.out.printf("Starting decode DST thread / buff %s%n", decodedBuffs);
			}
		} else {
			if (processor.isAlive() == false)
				return false;
		}
		try {
			readingThread = Thread.currentThread();
			byte[] dsdBuff;
			synchronized (processor) {
				if (decodedBuffs != null)
					dsdBuff = decodedBuffs.take();
				else
					return false;
			}
			//System.out.printf("Got decoded buff%n");
			int delta = bufPos < 0 ? 0 : bufEnd - bufPos;
			if (delta > 0)
				System.arraycopy(buff, bufPos, buff, 0, delta);
			int dsdLen = (int) (dst.FrameHdr.NrOfBitsPerCh * dst.FrameHdr.NrOfChannels / 8);
			System.arraycopy(dsdBuff, 0, buff, delta, dsdLen);
			usedBuffs.put(dsdBuff);
			bufPos = 0;
			bufEnd = delta + dsdLen;
			return true;
		} catch (InterruptedException e) {

		} catch (Throwable t) {
			//t.printStackTrace();
			if (processor != null)
				processor.interrupt();
			if (t instanceof ThreadDeath)
				throw (ThreadDeath) t;
			runException = t;
		}
		if (runException != null) {
			if (runException instanceof DecodeException)
				throw (DecodeException) runException;
			else
				throw new DecodeException("Error at decoding", runException);
		}
		return false;
	}

	public void run() {
		//System.out.printf("Starting processing thread%n");
		for (;;) {
			try {
				synchronized (this) {
					if (seekSample >= 0) {
						seek(seekSample);
					}
				}
				if (dstStart) {
					if (currentFrame > (atoc.track_end - atoc.track_start))
						break;
					dstStart = false;
					if (sectorStartOffset > 0)
						dsdStream.readFully(header, 0, sectorStartOffset);
					frmHeader.read(dsdStream);
					currentFrame++;
					if (frmHeader.packet_info_count > 0) {
						hdrIdx = 0;
					} else {
						dstStart = true;
						dsdStream.readFully(dstPakBuf, 0, sectorSize - frmHeader.getSize() - sectorStartOffset);
						continue;
					}
				}
				if (frmHeader.isFrameStart(hdrIdx)) {
					// completing current buffer
					if (dstLen > 0) { // complete previous
						byte[] dsdBuff;
						//System.out.printf("Unprocessed len %d, in buff %d%n", dstLen, usedBuffs.size());
						dst.FramDSTDecode(dstBuff, dsdBuff = getProcessed(), dstLen, lastFrm);
						//System.out.printf("Put buf %d%n", dsdBuff.length);
						putForProcessing(dsdBuff);
						dstLen = 0;
						continue;
					}
					dstSeek = false;
				}
				dsdStream.readFully(dstBuff, dstLen, frmHeader.getPackLen(hdrIdx));
				if (frmHeader.getDataType(hdrIdx) == 2 && !dstSeek)
					dstLen += frmHeader.getPackLen(hdrIdx);
				//dstSeek = false;
				lastFrm = frmHeader.getFrames(hdrIdx);
				hdrIdx++;
				if (hdrIdx >= frmHeader.packet_info_count) {
					// advance to next block
					dstStart = true;
					int skip = frmHeader.getPackLen(0);
					for (int i = 1; i < frmHeader.packet_info_count; i++)
						skip += frmHeader.getPackLen(i);
					skip = sectorSize - frmHeader.getSize() - skip - sectorStartOffset;
					if (skip > 0)
						dsdStream.readFully(dstPakBuf, 0, skip);
					else if (skip < 0)
						throw new DecodeException("Problem in DST decoding in the frame " + currentFrame, null);
				}
			} catch (InterruptedException e) {
				//e.printStackTrace();
				break;
			} catch (Throwable t) {
				//t.printStackTrace();
				runException = t;
				if (t instanceof ThreadDeath)
					throw (ThreadDeath) t;
				break;
			}
		}
		if (readingThread != null) {
			readingThread.interrupt();
			synchronized (processor) {
				//decodedBuffs = null;
				decodedBuffs.clear();
			}
		}
		if (sleepRequested) {
			processor = null;
			sleepRequested = false;
		}
	}

	byte[] getProcessed() throws InterruptedException {
		synchronized (this) {
			if (sleepRequested)
				throw new InterruptedException();
		}
		return usedBuffs.take();
	}

	void putForProcessing(byte[] dsdBuff) throws InterruptedException {
		//System.out.printf("Added buf%n");
		decodedBuffs.put(dsdBuff);
	}

	@Override
	public void sleep() throws DecodeException {
		super.sleep();
		synchronized (this) {
			if (sleepRequested == false) {
				sleepRequested = true;
				if (processor != null && processor.isAlive())
					processor.interrupt();
			}
		}
	}

	@Override
	public void close() {
		if (processor != null) {
			if (processor.isAlive()) {
				sleepRequested = true;
				processor.interrupt();
			} else
				processor = null;
		}
		super.close();
		//System.out.println("???close");
	}

}
