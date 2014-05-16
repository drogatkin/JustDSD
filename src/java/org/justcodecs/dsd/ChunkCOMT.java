package org.justcodecs.dsd;

import java.util.Date;

import org.justcodecs.dsd.Decoder.DecodeException;

public class ChunkCOMT extends BaseChunk {

	public static class Comment {
		Date stamp;
		int timeStampYear;  // creation year
		int timeStampMonth; // creation month
		int timeStampDay;  // creation day 
		int timeStampHour; // creation hour 
		int timeStampMinutes; // creation minutes
		int cmtType; // comment type
		int cmtRef; // comment reference
		//ulong count; // string length
		String commentText; // text } Comment;
		
		void read (DSDStream ds) {
			
		}
	}
	@Override
	void read(DSDStream ds) throws DecodeException {
		super.read(ds);
		skip(ds);
	}

}
