package org.justcodecs.dsd;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.justcodecs.dsd.DSTDecoder.DSTException;
import org.justcodecs.dsd.Decoder.DecodeException;

// TODO consider extends DISOFormat
public class DISOFormatMt extends DSDFormat<byte[]> implements Scarletbook, Runnable {
	final static int QUEUE_SIZE = 5;
	final static boolean DEBUG = true;
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
	int dstLen;
	byte[] dstBuff;
	boolean dstStart;
	FrmHeader frmHeader;
	int hdrIdx;
	int lastFrm;
	boolean dstSeek;
	/// flow control
	Throwable runException;
	Thread processor;

	long seekSample;
	boolean sleepRequested;

	ArrayBlockingQueue<DSTDecoderMt> avalDecoders;
	ArrayBlockingQueue<DSTDecoderMt> execDecoders;
	ExecutorService workers;

	// TODO move this class to DST decoder and reuse across of formats
	static class DSTDecoderMt {
		DSTDecoder dst;
		byte dstBuff[];
		int dstLen;
		byte[] dsdBuf;
		int dstFrmNo;
		boolean finished;

		public DSTDecoderMt(int noChan, int fs44) throws DSTException {
			dst = new DSTDecoder();
			dst.init(noChan, fs44);
			//System.out.printf("sixe %d vs %d%n", dst.FrameHdr.NrOfBitsPerCh/8, dst.FrameHdr.MaxFrameLen);
			dsdBuf = new byte[dst.FrameHdr.NrOfChannels * dst.FrameHdr.MaxFrameLen];
			dstBuff = new byte[MAX_DST_SIZE];
		}

		public void frameDSTDecode() throws DSTException {
			//System.out.printf("Dst proc buf %s of %d%n", dstBuff, dstLen);
			dst.FramDSTDecode(dstBuff, dsdBuf, dstLen, dstFrmNo);
			synchronized (this) {
				finished = true;
				notify();
			}
		}

		public byte[] swapDSTBuffs(byte[] currentDstBuff, int len) {
			byte[] result = dstBuff;
			dstBuff = currentDstBuff;
			dstLen = len;
			return result;
		}

		public boolean isFinished() {
			return finished;
		}

		public void reset() {
			finished = false;
		}
	}

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
				// System.out.printf("!!%n");
			}

			ds.seek(toc.area1Toc1Start * sectorSize + sectorStartOffset);
			atoc = new AreaTOC();
			if (toc.area1Toc1Start > 0)
				atoc.read(ds, toc.area1Toc1Start);
			if (!atoc.stereo) {
				ds.seek(toc.area_2_toc_1_start * sectorSize + sectorStartOffset);
				// System.out.printf("reading %x%n", toc.area_2_toc_1_start * sectorSize);
				atoc.read(ds, toc.area_2_toc_1_start);
				if (!atoc.stereo)
					throw new DecodeException("No two channels track found", null);
			}
			if (atoc.frame_format == FRAME_FORMAT_DST) {
				try {
					
					avalDecoders = new ArrayBlockingQueue<>(QUEUE_SIZE, false);
					DSTDecoderMt dstd = null;
					for (int d = 0; d < QUEUE_SIZE; d++) {
						avalDecoders.offer(dstd  = new DSTDecoderMt(getNumChannels(), getSampleRate() / 44100));
					}
					assert dstd != null;
					block = dstd.dst.FrameHdr.MaxFrameLen;
					
					workers = Executors.newFixedThreadPool(QUEUE_SIZE);

					execDecoders = new ArrayBlockingQueue<>(QUEUE_SIZE, true);
					
				} catch (DSTException e) {
					throw new DecodeException("Couldn't initialize DST decoder", e);
				}
			} else if (atoc.frame_format == FRAME_FORMAT_DSD_3_IN_16)
				frmHdrSize = 284;
			else if (atoc.frame_format == FRAME_FORMAT_DSD_3_IN_14)
				frmHdrSize = 32;
			// throw new DecodeException("DSS 3 in 16 isn't supported yet", null);
			// System.out.printf("Area-> %s%n", atoc);
			ds.seek((START_OF_MASTER_TOC + 1) * (sectorSize) + sectorStartOffset);
			// System.out.printf("reading cdtext at0x%x size %d%n", (START_OF_MASTER_TOC +
			// 1) * (sectorSize) + sectorStartOffset, atoc.size);
			CDText ctx = new CDText();
			ctx.read(ds, atoc.locales[0].encoding);
			TrackText ttx = new TrackText(atoc.track_count, sectorStartOffset == 0 ? 0 : 32);
			TrackTime tm = new TrackTime();
			boolean ttxf = false;
			for (int i = 1; i < atoc.size; i++) {
				// System.out.printf("checking at 0x%x%n", (atoc.start + i) * sectorSize +
				// sectorStartOffset);
				if (!ttxf) {
					ds.seek((atoc.start + i) * sectorSize + sectorStartOffset);
					try {
						ttx.read(ds, atoc.locales[0].encoding);
						ttxf = true;
						continue;
					} catch (DecodeException de) {
						// System.out.printf("header %s%n", de);
					}
				}
				ds.seek((atoc.start + i) * sectorSize + sectorStartOffset);
				try {
					tm.read(ds);
				} catch (DecodeException de) {
					// System.out.printf("header time %s%n", de);
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
			// fo = new FileOutputStream("test.dst");
			currentFrame = 0;
			processor = null;
		} catch (IOException ioe) {
			throw new DecodeException("IO", ioe);
		}
	}

	// int it;
	@Override
	boolean readDataBlock() throws DecodeException {
		if (atoc.frame_format == FRAME_FORMAT_DST)
			return readDSTDataBlockAsync();
		if (currentFrame > (atoc.track_end - atoc.track_start))
			return false;
		// if (it > 5)
		// return false;
		if (bufPos < 0)
			bufPos = 0;
		int delta = bufEnd - bufPos;
		if (delta > 0)
			System.arraycopy(buff, bufPos, buff, 0, delta);
		try {
			long pp, pp1;
			// pp = dsdStream.getFilePointer();
			dsdStream.readFully(header, 0, header.length);
			// pp1 = dsdStream.getFilePointer();
			dsdStream.readFully(buff, delta, block);
			// if (tail > 0)
			// dsdStream.readFully(header, 0, tail);
			// System.out.printf("Start 0x%x data 0x%x dirst 0x%x, last 0x%x%n", pp, pp1,
			// buff[delta], buff[delta+block-1]);
			currentFrame++;
			bufPos = 0;
			bufEnd = block + delta - tail;
		} catch (IOException e) {
			throw new DecodeException("IO exception at reading samples", e);
		}
		// it++;
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
		// System.out.printf("sec size: %d, hdr: %d, block: %d ta: %d%n", sectorSize,
		// frmHdrSize, block, tail);
		if (!isDST()) {
			block = SACD_LSN_SIZE - frmHdrSize;
			tail = sectorSize - block - frmHdrSize - sectorStartOffset;
			if (tail > 0)
				block += tail;
			buff = new byte[block + (overrun * getNumChannels())];
			header = new byte[frmHdrSize + sectorStartOffset];
		} else {
			//if (overrun != 0)
				//throw new RuntimeException("overun specifed for DST");
			buff = new byte[(block + overrun) * getNumChannels()];
			dstBuff = new byte[MAX_DST_SIZE]; // MAX_DST_SIZE
			frmHeader = new FrmHeader();
			header = new byte[12];
		}
	}

	@Override
	boolean isMSB() {
		return true;
	}

	@Override
	byte[] getSamples() {
		if (buff == null)
			return DFFFormatMt.EMPTY_BUF;
		return buff;
	}

	@Override
	boolean isDST() {
		return atoc.frame_format == FRAME_FORMAT_DST;
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
				// System.out.printf("Seek %d %x%n", atoc.track_start * sectorSize,
				// atoc.track_start * sectorSize);
				// new Exception().printStackTrace();
			} else if (sampleNum > 0 && sampleNum < getSampleCount()) {
				// System.out.printf("actual samples %d (%ds) text samples %d (%ds) search
				// %d(%ds)%n",getSampleCount(), getSampleCount()/getSampleRate(),
				// ((long)textDuration)*getSampleRate(), textDuration, sampleNum,
				// sampleNum/getSampleRate());
				if (textDuration > 0) {
					sampleNum = Math.round(getTimeAdjustment() * sampleNum);
					// System.out.printf("adjusted %d (%ds) / %f%n", sampleNum,
					// sampleNum/getSampleRate(), getTimeAdjustment());
				}
				if (sampleNum >= getSampleCount())
					throw new DecodeException("Trying to seek non existing sample " + sampleNum, null);
				currentFrame = (int) (sampleNum * (atoc.track_end - atoc.track_start) / getSampleCount());
				dsdStream.seek((long) (atoc.track_start + currentFrame) * sectorSize);
				// dstSeek = currentFrame > 0;
				/*
				 * System.out.printf("Seek %ds,  len %db, tot %ds frame %d 0f %d%n", sampleNum /
				 * getSampleRate(), atoc.track_end - atoc.track_start, getSampleCount() /
				 * getSampleRate(), currentFrame, atoc.track_end - atoc.track_start);
				 */
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
					sampleNum += ((long) (seekSec - currentSec)) * getSampleRate();
					if (local_debug) {
						System.out.printf("Seek sec %d, current %d%n", seekSec, currentSec);
					}
					currentFrame = (int) (sampleNum * (atoc.track_end - atoc.track_start) / getSampleCount());
					if (local_debug) {
						System.out.printf("Current frm %d for sample %d%n", currentFrame, sampleNum);
						System.out.printf("Seeking %d starting %d plus current %d mul %d%n",
								(atoc.track_start + currentFrame) * sectorSize, atoc.track_start, currentFrame,
								sectorSize);
					}
					dsdStream.seek(((long) (atoc.track_start + currentFrame)) * sectorSize);
					dstSeek = currentFrame > 0;
				}
			} else
				throw new DecodeException("Trying to seek non existing sample " + sampleNum, null);
			// bufPos = -1;
			// bufEnd = 0;
			dstStart = true;
			dstLen = 0;
			seekSample = -1;
			// System.out.printf("Positioned to %x sector %d block %d%n",
			// dsdStream.getFilePointer(), atoc.track_start, currentFrame);
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
		//System.out.printf("DST read %s%n", processor);
		if (processor == null) {
			synchronized (this) {
				processor = new Thread(this);
				processor.setName("DST decoder");
				processor.setDaemon(true);
				processor.start();
				// System.out.printf("Starting decode DST thread / buff %s%n", decodedBuffs);
			}
		} else {
			if (processor.isAlive() == false)
				return false;
		}
		try {
			DSTDecoderMt decoder = execDecoders.take();
			synchronized (decoder) {
				if (!decoder.isFinished())
					decoder.wait();
			}
			int delta = bufPos < 0 ? 0 : bufEnd - bufPos;
			if (delta > 0)
				System.arraycopy(buff, bufPos, buff, 0, delta);
			System.arraycopy(decoder.dsdBuf, 0, buff, delta, decoder.dsdBuf.length);
			bufPos = 0;
			bufEnd = delta + decoder.dsdBuf.length;
			avalDecoders.offer(decoder);

			return true;
		} catch (InterruptedException ignore) {

		} catch (Throwable t) {
			// t.printStackTrace();
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
		// System.out.printf("Starting processing thread%n");
		for (;;) {
			if (runException != null)
				break;
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
						dsdStream.skipBytes(sectorSize - frmHeader.getSize() - sectorStartOffset);
						//dsdStream.readFully(dstPakBuf, 0, sectorSize - frmHeader.getSize() -
						 //sectorStartOffset);
						continue;
					}
				}
				if (frmHeader.isFrameStart(hdrIdx)) {
					// completing current buffer
					if (dstLen > 0) { // complete previous
						// get avail decoder
						DSTDecoderMt decoder = avalDecoders.take();
						decoder.reset();
					//	System.out.printf("Dst buf %s of %d%n", dstBuff, dstLen);
						dstBuff = decoder.swapDSTBuffs(dstBuff, dstLen);

						execDecoders.offer(decoder); // no reason to check if accepted
						workers.execute(() -> {
							try {
								decoder.frameDSTDecode();

							} catch (Exception e) {
								if (DEBUG)
									e.printStackTrace();
								runException = e;
							}

						});
						dstLen = 0;
						continue;
					}
					dstSeek = false;
				}
				dsdStream.readFully(dstBuff, dstLen, frmHeader.getPackLen(hdrIdx));
				if (frmHeader.getDataType(hdrIdx) == 2 && !dstSeek)
					dstLen += frmHeader.getPackLen(hdrIdx);
				// dstSeek = false;
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
						dsdStream.skipBytes(skip);
					 //dsdStream.readFully(dstPakBuf, 0, skip);
					else if (skip < 0)
						throw new DecodeException("Problem in DST decoding in the frame " + currentFrame, null);
				}
			} catch (InterruptedException e) {
				// e.printStackTrace();
				break;
			} catch (Throwable t) {
				// t.printStackTrace();
				runException = t;
				if (t instanceof ThreadDeath)
					throw (ThreadDeath) t;
				break;
			}
		}

		if (sleepRequested) {
			processor = null;
			sleepRequested = false;
		}
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
		//new Exception("close").printStackTrace();
		if (processor != null) {
			if (processor.isAlive()) {
				sleepRequested = true;
				processor.interrupt();
			}
			processor = null;
		}
		if (workers != null)
			workers.shutdown();
		workers = null;
		super.close();
		// System.out.println("???close");
	}

}
