package org.justcodecs.dsd;

import java.io.RandomAccessFile;
import java.util.HashMap;

import org.justcodecs.dsd.Decoder.DecodeException;

import de.vdheide.mp3.ID3v2;
import de.vdheide.mp3.IOAdapter;
import de.vdheide.mp3.TagContent;
import de.vdheide.mp3.TextFrameEncoding;

public class ChunkID3 extends BaseChunk {
	@Override
	void read(DSDStream ds) throws DecodeException {
		super.read(ds);
		IOAdapter io = null;
		ChunkFRM8 f8 = getFRM8();
		if (f8 == null)
			throw new DecodeException("ID3 chink appeared out of FRM8 scope", null);
		try {
			// TODO read chunk in memory and then process as stream (BytesArrayStream)
			ID3v2 id3 = new ID3v2(io = new IOAdapter((RandomAccessFile) ds), f8.encoding);
			storeAttr(f8.metaAttrs, id3, "Album", ID3v2.ALBUM);
			storeAttr(f8.metaAttrs, id3, "Artist", ID3v2.ARTIST);
			storeAttr(f8.metaAttrs, id3, "Title", ID3v2.TITLE);
			storeAttr(f8.metaAttrs, id3, "Year", ID3v2.YEAR);
			if (f8.metaAttrs.containsKey("Year"))
				try {
					f8.metaAttrs.put("Year", new Integer((String)f8.metaAttrs.get("Year")));
				} catch(Exception e) {
					f8.metaAttrs.remove("Year");
				}
			storeAttr(f8.metaAttrs, id3, "Track", ID3v2.TRACK);
			if (f8.metaAttrs.containsKey("Track"))
				try {
					f8.metaAttrs.put("Track", new Integer((String)f8.metaAttrs.get("Track")));
				} catch(Exception e) {
					f8.metaAttrs.remove("Track");
				}
			storeAttr(f8.metaAttrs, id3, "Genre", ID3v2.GENRE);
			byte[] pic = id3.getPicture().getBinaryContent();
			//System.out.printf("Pic%s%n", pic);
			if (pic != null)
				f8.metaAttrs.put("Picture", pic);
		} catch (Exception e) {
			throw new DecodeException("ID3 parsing error", e);
		} finally {
			
		}
		skip(ds);
	}
	
	protected void storeAttr(HashMap<String, Object> attrs, ID3v2 id3, String name, int id) {
		try {
			TagContent tf = TextFrameEncoding.read(id3, id3.getFrameCode(id));
			//System.err.printf("proc %s %s%n", name, tf);
			if (tf != null) {
				attrs.put(name, tf.getTextContent());
			}
		} catch (Exception e) {
			
		}	
	}
}
