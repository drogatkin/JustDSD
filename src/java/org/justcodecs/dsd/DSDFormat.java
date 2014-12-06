package org.justcodecs.dsd;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import org.justcodecs.dsd.Decoder.DecodeException;

public abstract class DSDFormat<B> {
	protected DSDStream dsdStream;
	protected HashMap<String, Object> attrs;
	public String getMetadataCharset() {
		return metadataCharset;
	}

	public void setMetadataCharset(String metadataCharset) {
		this.metadataCharset = metadataCharset;
	}

	protected String metadataCharset;

	int bufPos = -1;
	int bufEnd;
	
	public void init(DSDStream ds) throws DecodeException {
		dsdStream = ds;
		attrs = new HashMap<String,Object>();
	}
	
	/** puts any parallel processing activities in sleep
	 * 
	 * @throws DecodeException if sleep cwasn't initiated successfully
	 */
	public void sleep() throws DecodeException {
		
	}

	abstract boolean readDataBlock() throws DecodeException;	
	
	public abstract int getSampleRate();

	public abstract long getSampleCount();
	public abstract int getNumChannels();
	
	abstract void initBuffers(int overrun);
	abstract boolean isMSB();
	abstract B getSamples();
	abstract void seek(long sampleNum) throws DecodeException;
	
	public double getTimeAdjustment() {
		return 1.0;
	}

	public void close() {
		try {
			if(dsdStream != null)
				dsdStream.close();
		} catch (IOException e) {

		}
	}
	
	boolean isDST() {
		return false;
	}
	
	public Object getMetadata(String key) {
		if (attrs == null)
			return null;
		return attrs.get(key);
	}

	@Override
	public String toString() {
		String tracks = Arrays.toString((Object[])(attrs==null?null:attrs.get("Tracks")));
		return "DSDFormat [attrs=" + attrs + ", rate=" + getSampleRate() +", channels=" + getNumChannels() + ", DST=" + isDST()+ ", tracks:" + tracks + "]";
	}
	
	//protected void setMetadata(String){}

}
