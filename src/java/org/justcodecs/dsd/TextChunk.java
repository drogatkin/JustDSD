package org.justcodecs.dsd;

import java.io.IOException;

import org.justcodecs.dsd.Decoder.DecodeException;

public abstract class TextChunk extends BaseChunk {
	
	@Override
	void read(DSDStream ds) throws DecodeException {
		super.read(ds);
		try {
			int l = ds.readInt(true);
			byte tb[] = new byte[l];
			ds.readFully(tb, 0, l);
			setText(new String(tb));
			skip(ds);
		} catch (IOException e) {
			throw new DecodeException("IO", e);
		}
	}
	abstract  void setText(String string);
}
