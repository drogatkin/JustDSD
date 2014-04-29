package org.justcodecs.dsd;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Utils {
	public static int bytesToInt(byte... bytes) {
		int result = 0;
		for (byte b : bytes)
			result = (result << 8) + (b & 255);
		return result;
	}

	public static class RandomDSDStream extends RandomAccessFile implements DSDStream {
		protected byte[] buf = new byte[8];

		public RandomDSDStream(File f) throws FileNotFoundException {
			super(f, "r");
		}

		@Override
		public boolean canSeek() {
			return true;
		}

		public long readLong(boolean lsb) throws IOException {
			if (lsb)
				return readLong();
			readFully(buf, 0, 8);
			//System.out.printf("Buf 7 %d 0 - %d %n", buf[7], buf[0]);
			return ((long) (buf[7] & 255) << 56) + ((long) (buf[6] & 255) << 48) + ((long) (buf[5] & 255) << 40)
					+ ((long) (buf[4] & 255) << 32) + ((long) (buf[3] & 255) << 24) + ((long) (buf[2] & 255) << 16)
					+ ((long) (buf[1] & 255) << 8) + (buf[0] & 255);
		}

		@Override
		public int readInt(boolean lsb) throws IOException {
			if (lsb)
				return readInt();
			readFully(buf, 0, 4);
			return ((int) (buf[3] & 255) << 24) + ((int) (buf[2] & 255) << 16) + ((int) (buf[1] & 255) << 8)
					+ (buf[0] & 255);
		}

	}
}
