package org.justcodecs.dsd;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.justcodecs.dsd.DSTDecoder.DSTException;
import org.justcodecs.dsd.Decoder.DecodeException;

// TODO consider to extend DFFFormat
public class DFFFormatMt extends DSDFormat<byte[]> implements Runnable {
	ChunkFRM8 frm;
	byte buff[];
	int block = 2048;
	long filePosition;
	int dstFrmNo;
	Exception decodingException = null;

	// DST specific
	final int DSTF = Utils.bytesToInt((byte) 'D', (byte) 'S', (byte) 'T', (byte) 'F');
	final int DSTC = Utils.bytesToInt((byte) 'D', (byte) 'S', (byte) 'T', (byte) 'C');

	int DECODERS = 5;

	final static byte[] EMPTY_BUF = new byte[0];

	ArrayBlockingQueue<DSTDecoderMt> freeDecoders;

	ArrayBlockingQueue<DSTDecoderMt> finishedDecoders;

	ExecutorService decoderThreads;

	Thread processingThread;

	AtomicLong seekSample = new AtomicLong(-1);

	static final boolean DEBUG = false;

	static class DSTDecoderMt {
		DSTDecoder dst;

		int dstFrmNo;

		byte[] dsdBuf;
		ChunkDSTF dstf;
		ChunkDSTC dstc;

		boolean finished = false;

		public DSTDecoderMt(int noChan, int fs44) throws DSTException {
			dst = new DSTDecoder();
			dst.init(noChan, fs44);
			dstf = new ChunkDSTF();
			dstc = new ChunkDSTC();
			dsdBuf = new byte[dst.FrameHdr.MaxFrameLen * dst.FrameHdr.NrOfChannels];
		}

		public void framDSTDecode() throws DSTException {
			dst.FramDSTDecode(dstf.data, dsdBuf, (int) dstf.size, dstFrmNo);
			synchronized (this) {
				finished = true;
				notify();
			}
		}

		public boolean isFinished() {
			return finished;
		}

		public void reset() {
			finished = false;
		}

	}

	public DFFFormatMt() {

	}

	public DFFFormatMt(int threads) {
		if (threads > 0 && threads < 10)
			DECODERS = threads;
	}

	@Override
	public void init(DSDStream ds) throws DecodeException {
		super.init(ds);
		BaseChunk c = BaseChunk.create(dsdStream, metadataCharset);
		if (c instanceof ChunkFRM8 == false)
			throw new DecodeException("Invalid .dff format, no FRAME chunk", null);
		// c.skip(dsdStream);
		frm = (ChunkFRM8) c;
		attrs.putAll(frm.metaAttrs);
		if (isDST()) {
			try {
				freeDecoders = new ArrayBlockingQueue<>(DECODERS, false);
				DSTDecoderMt dstd = null;
				for (int d = 0; d < DECODERS; d++) {
					freeDecoders.offer(dstd = new DSTDecoderMt(getNumChannels(), getSampleRate() / 44100));
				}
				block = dstd.dst.FrameHdr.MaxFrameLen;
				decoderThreads = Executors.newFixedThreadPool(DECODERS);

				finishedDecoders = new ArrayBlockingQueue<>(DECODERS, true);

				processingThread = new Thread(this);
				processingThread.setName("DST decoder");
				processingThread.setDaemon(true);

			} catch (DSTException e) {
				clean();
				throw new DecodeException("Couldn't initialize DST decoder", e);
			}
		}

	}

	protected void clean() {
		freeDecoders = null;
		finishedDecoders = null;
		if (processingThread != null && processingThread.isAlive())
			processingThread.interrupt();
		processingThread = null;
		if (decoderThreads != null)
			decoderThreads.shutdown();
	}

	@Override
	public void close() {
		clean();
		super.close();
	}

	@Override
	boolean readDataBlock() throws DecodeException {
		if (isDST()) {
			if (decodingException != null)
				throw new DecodeException("IO exception at reading samples", decodingException);
			try {
				return decodeDSTDataBlockMt();
			} catch (DecodeException | InterruptedException e) {
				if (DEBUG)
					e.printStackTrace();
				throw new DecodeException("IO exception at reading samples", e);
			}
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
			return (frm.props.dsd.dataEnd - frm.props.dsd.start) * (8 / getNumChannels());
		else {
			long res = frm.props.dst.info.numFrames / frm.props.dst.info.rate * getSampleRate();
			if (res < 0)
				return -res;
			return res;
		}
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
		if (buff == null)
			return EMPTY_BUF;
		return buff;
	}

	@Override
	boolean isDST() {
		return frm.props.dst != null;
	}

	@Override
	void seek(long sampleNum) throws DecodeException {
		if (isDST()) {
			seekSample.getAndSet(sampleNum);
			if (processingThread.isAlive() == false)
				processingThread.start();
			return;
		}
		// if (sampleNum < getSampleCount())
		try {
			// System.out.printf("Start play %d for sample %d, frm %s%n",
			// dsdStream.getFilePointer(), sampleNum,
			// frm.props.dsd);
			filePosition = frm.props.dsd.start + (sampleNum / 8) * getNumChannels();
			dsdStream.seek(filePosition);
			bufPos = -1;
			bufEnd = 0;
		} catch (IOException e) {
			// TODO invalidate filePosition
			throw new DecodeException("", e);
		}
	}

	boolean decodeDSTDataBlockMt() throws DecodeException, InterruptedException {
		if (dstFrmNo >= frm.props.dst.info.numFrames)
			return false;
		DSTDecoderMt decoder = finishedDecoders.take();
		synchronized (decoder) {
			if (!decoder.isFinished())
				decoder.wait();
		}
		int delta = bufPos < 0 ? 0 : bufEnd - bufPos;

		if (delta > 0)
			System.arraycopy(buff, bufPos, buff, 0, delta);
		int dlen = decoder.dsdBuf.length;
		System.arraycopy(decoder.dsdBuf, 0, buff, delta, dlen);
		bufPos = 0;
		bufEnd = delta + dlen;
		freeDecoders.offer(decoder);
		return true;

	}

	void seekDST(long sampleNum) throws DecodeException {
		final boolean local_debug = false;
		if (decodingException != null)
			throw new DecodeException("An exception in decoding", decodingException);

		try {
			dsdStream.seek(frm.props.dst.info.start + frm.props.dst.info.size);
			if (sampleNum > 0) {
				int seekChunk = (int) (sampleNum * frm.props.dst.info.numFrames / getSampleCount());
				dstFrmNo = seekChunk;
				// position in file, read buff, find signature
				// or use ChunkDSTI when avail
				if (local_debug)
					System.out.printf("Start play 0x%x (0%d) for sample %d, frm %d total %d%n",
							dsdStream.getFilePointer(), -1, seekChunk, frm.props.dst.info.numFrames);
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

			}
			seekSample.set(-1);
		} catch (IOException e) {
			// TODO invalidate filePosition
			throw new DecodeException("", e);
		}
	}

	@Override
	public void run() {
		do {
			if (decodingException != null)
				break;
			try {
				DSTDecoderMt decoder = freeDecoders.take();
				decoder.reset();
				long sample = seekSample.get();
				if (sample > -1) {
					seekDST(sample);

				} else
					dstFrmNo++;

				if (dstFrmNo >= frm.props.dst.info.numFrames)
					return;
				int type = dsdStream.readInt(true);
				if (type == DSTF)
					decoder.dstf.read(dsdStream);
				else if (type == DSTC) {
					decoder.dstc.read(dsdStream);
					type = dsdStream.readInt(true);
					if (type == DSTF)
						decoder.dstf.read(dsdStream);
					else
						throw new DSTException("Unexpected DST chunk type " + type, 0);
				} else {
					throw new DSTException("Unknown DST chunk type " + type, 0);
				}
				// decoder.dstf.skip(dsdStream);
				decoder.dstFrmNo = dstFrmNo;
				// System.out.printf("Decoder %s %b %d%n", decoder, decoder.isFinished(),
				// decoder.dstFrmNo);
				finishedDecoders.offer(decoder); // no reason to check if accepted
				decoderThreads.execute(() -> {
					try {
						decoder.framDSTDecode();

					} catch (DSTException e) {
						if (DEBUG)
							e.printStackTrace();
						decodingException = e;
					}

				});

			} catch (InterruptedException e) {
				if (DEBUG)
					e.printStackTrace();
				break;
			} catch (DecodeException e) {
				if (DEBUG)
					e.printStackTrace();
				decodingException = e;
				break;
			} catch (IOException e) {
				if (DEBUG)
					e.printStackTrace();
				decodingException = e;
				break;
			} catch (DSTException e) {
				if (DEBUG)
					e.printStackTrace();
				decodingException = e;
				break;
			}

		} while (true);
	}
}
