package org.justcodecs.dsd;

import java.io.IOException;
import java.util.Arrays;

import org.justcodecs.dsd.Decoder.DecodeException;

public class BaseChunk {
	int ID; // char[4]
	long size;
	long start;
	byte[] data;
	BaseChunk parent;
	static byte[] IDBuf = new byte[4];

	public static BaseChunk create(DSDStream ds, BaseChunk parent) throws DecodeException {
		try {
			ds.readFully(IDBuf, 0, 4);
			BaseChunk result;
			try {
				Class<?> chunkClass = Class.forName(
						BaseChunk.class.getPackage().getName() + ".Chunk" + new String(IDBuf).trim());
				result = (BaseChunk) chunkClass.newInstance();
			} catch (ClassNotFoundException e) {
				result = new ChunkUNK();
			}
			result.parent = parent;
			result.read(ds);
			return result;
		} catch (IOException e) {
			throw new DecodeException("IO", e);
		} catch (Exception e) {
			throw new DecodeException("Unsupported chunk:" + new String(IDBuf), e);
		}
	}

	void read(DSDStream ds) throws DecodeException {
		try {
			size = ds.readLong(true);
			start = ds.getFilePointer();
			//System.out.printf("Current %x,  size %d%n", start, size);
			if (size <= 0)
				throw new DecodeException("Invalid size "+size +" of "+new String(IDBuf), null);			
		} catch (IOException e) {
			throw new DecodeException("", e);
		}
	}

	void skip(DSDStream ds) throws DecodeException {
		try {
			ds.seek(start + size);
		} catch (IOException e) {
			throw new DecodeException("", e);
		}
	}

	@Override
	public String toString() {
		return this.getClass().getCanonicalName() + " [size=" + size + ", data=" + Arrays.toString(data) + "]";
	}

}
