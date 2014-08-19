package org.justcodecs.dsd;

import java.io.IOException;

import org.justcodecs.dsd.Decoder.DecodeException;

public class ChunkDIIN extends BaseChunk { // TODO introduce composite and simple chunks
	@Override
	void read(DSDStream ds) throws DecodeException {		
		super.read(ds);
		try {			
			for (;;) {
				// read local chinks
				BaseChunk c = BaseChunk.create(ds, this);
				if (c instanceof ChunkDITI)
					getFRM8().metaAttrs.put("Title", ((ChunkDITI)c).title); 
				else if (c instanceof ChunkDIAR)
					getFRM8().metaAttrs.put("Artist", ((ChunkDIAR)c).artist); 
				//System.out.printf("--->%s at %d s+s%d%n", c, ds.getFilePointer(), c.start+c.size);
				if (ds.getFilePointer() >= parent.start + parent.size)
					break;
				
			}
		} catch (IOException e) {
			throw new DecodeException("", e);
		}
	}
}
