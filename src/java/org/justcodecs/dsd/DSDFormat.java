package org.justcodecs.dsd;

import java.io.IOException;

import org.justcodecs.dsd.Decoder.DecodeException;

public abstract class DSDFormat {
	protected DSDStream dsdStream;

	int bufPos = -1;
	int bufEnd;
	
	void init(DSDStream ds) throws DecodeException {
		dsdStream = ds;
	}

	abstract boolean readDataBlock() throws DecodeException;	
	
	public abstract int getSampleRate();

	public abstract long getSampleCount();
	public abstract int getNumChannels();
	
	abstract void initBuffers(int overrun);
	abstract boolean isMSB();
	abstract byte[][] getSamples();

	public void close() {
		try {
			dsdStream.close();
		} catch (IOException e) {

		}
	}

}
