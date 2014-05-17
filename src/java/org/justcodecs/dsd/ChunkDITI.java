package org.justcodecs.dsd;

public class ChunkDITI extends TextChunk {
	String title;

	@Override
	void setText(String string) {
		title = string;
		//System.out.printf("%s from %s%n", string, parent.getClass().getName());
	}
}
