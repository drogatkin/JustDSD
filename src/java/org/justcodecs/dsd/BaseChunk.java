package org.justcodecs.dsd;

import java.io.IOException;
import java.util.Arrays;

import org.justcodecs.dsd.Decoder.DecodeException;

public class BaseChunk {
	int ID; // char[4]
	long size;
	long start;
	byte[] data;
	static byte[] IDBuf = new byte[4];

	public static BaseChunk create(DSDStream ds) throws DecodeException {
		try {
			ds.readFully(IDBuf, 0, 4);
			BaseChunk result = (BaseChunk) Class.forName(
					BaseChunk.class.getPackage().getName() + ".Chunk" + new String(IDBuf).trim()).newInstance();
			result.read(ds);
			return result;

		} catch (IOException e) {
			throw new DecodeException("", e);
		} catch (Exception e) {
			throw new DecodeException("Unsupported chunk:" + new String(IDBuf), e);
		}

	}

	void read(DSDStream ds) throws DecodeException {
		try {
			size = ds.readLong(true);
			start = ds.getFilePointer();
			System.out.printf("Current %d,  size %d%n", start, size);
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
