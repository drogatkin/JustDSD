package org.justcodecs.dsd;

import java.io.IOException;

import org.justcodecs.dsd.Decoder.DecodeException;

public abstract class DSDFormat<B> {
	protected DSDStream dsdStream;

	int bufPos = -1;
	int bufEnd;
	
	public void init(DSDStream ds) throws DecodeException {
		dsdStream = ds;
	}

	abstract boolean readDataBlock() throws DecodeException;	
	
	public abstract int getSampleRate();

	public abstract long getSampleCount();
	public abstract int getNumChannels();
	
	abstract void initBuffers(int overrun);
	abstract boolean isMSB();
	abstract B getSamples();
	abstract void seek(long sampleNum) throws DecodeException;

	public void close() {
		try {
			dsdStream.close();
		} catch (IOException e) {

		}
	}

}
