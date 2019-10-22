package org.justcodecs.dsd;

import java.io.DataInput;
import java.io.IOException;

public interface DSDStream extends DataInput {

	boolean canSeek();

	public void readFully(byte[] b, int off, int len) throws IOException;
	
	public int read(byte[] b, int off, int len) throws IOException;

	public long readLong(boolean lsb) throws IOException;
	
	public int readInt(boolean lsb) throws IOException;
	
	public long readIntUnsigned(boolean lsb) throws IOException;
	
	public short readShort(boolean lsb) throws IOException;

	long length() throws IOException;
	
	long getFilePointer() throws IOException;
	
	void seek(long pointer) throws IOException;
	
	void close()throws IOException;
}
