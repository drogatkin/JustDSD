package org.justcodecs.dsd;

import java.io.IOException;

import org.justcodecs.dsd.Decoder.DecodeException;

public class ChunkPROP extends BaseChunk {
	int sampleRate;
	int channels;
	String comp;
	ChunkDSD dsd;
	ChunkDST dst;
	long bound;

	@Override
	void read(DSDStream ds) throws DecodeException {
		super.read(ds);
		try {
			ds.readFully(IDBuf, 0, 4);
			if ("SND ".equals(new String(IDBuf)) == false)
				throw new DecodeException("PROP chunk isn't SND", null);
			for (;;) {
				// read local chunks
				BaseChunk c = BaseChunk.create(ds, this);
				if (c instanceof ChunkFS)
					sampleRate = ((ChunkFS) c).sampleRate;
				else if (c instanceof ChunkCHNL)
					channels = ((ChunkCHNL) c).numChannels;
				else if (c instanceof ChunkCMPR) {
					comp = ((ChunkCMPR) c).compression;
				} else if (c instanceof ChunkDITI)
					getFRM8().metaAttrs .put("Title",((ChunkDITI) c).title);
				else if (c instanceof ChunkDSD) {
					dsd = (ChunkDSD) c;
					try {
						dsd.skip(ds);
					} catch (DecodeException e) {
						break;
					}
				} else if (c instanceof ChunkDST) {
					dst = (ChunkDST) c;
					try {
						dst.skip(ds);
					} catch (DecodeException e) {
						//break;
					}
				} else if (c instanceof ChunkID3) {
					
				}
				//System.out.printf("--->%s at %x s+s%x (%x)%n", c, ds.getFilePointer(), c.start+c.size, c.start);
				if (ds.getFilePointer() >= parent.start + parent.size)
					break;
			}
		} catch (DecodeException e) {
			if (dsd == null)
				throw e;
			// still can be playable
		} catch (IOException e) {
			throw new DecodeException("IO", e);
		}
	}

	@Override
	public String toString() {
		return "ChunkPROP [sampleRate=" + sampleRate + ", channels=" + channels + ", comp=" + comp + "]";
	}

}
