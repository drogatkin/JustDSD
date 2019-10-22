package org.justcodecs.dsd;

public class ChunkDIAR extends TextChunk {
	public String artist;

	@Override
	void setText(String string) {
		artist = string;
	}
}
