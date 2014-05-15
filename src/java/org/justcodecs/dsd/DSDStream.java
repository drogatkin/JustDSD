package org.justcodecs.dsd;

import java.io.IOException;

public interface DSDStream {

	boolean canSeek();

	public void readFully(byte[] b, int off, int len) throws IOException;

	public long readLong(boolean lsb) throws IOException;
	
	public int readInt(boolean lsb) throws IOException;
	
	public short readShort(boolean lsb) throws IOException;

	long length() throws IOException;
	
	long getFilePointer() throws IOException;
	
	void seek(long pointer) throws IOException;
	
	void close()throws IOException;
}
