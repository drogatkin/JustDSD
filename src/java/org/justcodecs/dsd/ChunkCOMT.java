package org.justcodecs.dsd;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.justcodecs.dsd.Decoder.DecodeException;

public class ChunkCOMT extends BaseChunk {
	public static int General_album = 0;
	public static int Channel = 1;
	public static int Sound_Source = 2;
	public static int File_History = 3;

	Comment[] comments;

	public static class Comment {
		Date stamp;
		short timeStampYear; // creation year
		short timeStampMonth; // creation month
		short timeStampDay; // creation day 
		short timeStampHour; // creation hour 
		short timeStampMinutes; // creation minutes
		short cmtType; // comment type
		short cmtRef; // comment reference
		//ulong count; // string length
		String commentText; // text

		void read(DSDStream ds, Calendar c) throws IOException {
			timeStampYear = ds.readShort(true);
			ds.readFully(IDBuf, 0, 4);
			timeStampMonth = (short) (IDBuf[0] & 255);
			timeStampDay = (short) (IDBuf[0] & 255);
			timeStampHour = (short) (IDBuf[0] & 255);
			timeStampMinutes = (short) (IDBuf[0] & 255);
			c.set(timeStampYear, timeStampMonth, timeStampDay, timeStampHour, timeStampMinutes);
			stamp = c.getTime();
			cmtType = ds.readShort(true);
			cmtRef = ds.readShort(true);
			int l = ds.readInt(true);
			// TODO check length
			//System.out.printf("CMT chu size %d com len %d%n", 0, l);
			byte[] tb = new byte[l];
			ds.readFully(tb, 0, l);
			commentText = new String(tb);
			if ((l & 1) == 1)
				ds.readFully(IDBuf, 0, 1); // read padding
		}

		@Override
		public String toString() {
			return "Comment [stamp=" + stamp + ", cmtType=" + cmtType + ", cmtRef=" + cmtRef + ", commentText="
					+ commentText + "]";
		}
	}

	@Override
	void read(DSDStream ds) throws DecodeException {
		super.read(ds);
		Calendar c = Calendar.getInstance();
		try {
			short num = ds.readShort(true);
			comments = new Comment[num];
			for (short j = 0; j < num; j++) {
				comments[j] = new Comment();
				comments[j].read(ds, c);
				if (comments[j].cmtType == General_album)
					getFRM8().metaAttrs.put("Album", comments[j].commentText); 
			}
			//System.out.printf("Comms:%s%n", Arrays.toString(comments));
		} catch (IOException e) {
			throw new DecodeException("", e);
		}
		skip(ds);
	}

}
