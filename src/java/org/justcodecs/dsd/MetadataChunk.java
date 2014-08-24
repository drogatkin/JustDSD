package org.justcodecs.dsd;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

import de.vdheide.mp3.ID3v2;
import de.vdheide.mp3.IOAdapter;
import de.vdheide.mp3.TagContent;
import de.vdheide.mp3.TextFrameEncoding;

public class MetadataChunk {
	protected String encoding;
	
	public MetadataChunk(long metadataOffs, String enc) {
		position = metadataOffs;
		encoding = enc;
	}

	void read(DSDStream ds) throws IOException {
		ds.seek(position);
		IOAdapter io = null;
		try {
			ID3v2 id3 = new ID3v2(io = new IOAdapter((RandomAccessFile) ds), encoding);
			attrs = new HashMap<String, Object>();
			storeAttr(id3, "Album", ID3v2.ALBUM);
			storeAttr(id3, "Artist", ID3v2.ARTIST);
			storeAttr(id3, "Title", ID3v2.TITLE);
			storeAttr(id3, "Year", ID3v2.YEAR);
			if (attrs.containsKey("Year"))
				try {
					attrs.put("Year", new Integer((String)attrs.get("Year")));
				} catch(Exception e) {
					attrs.remove("Year");
				}
			storeAttr(id3, "Track", ID3v2.TRACK);
			if (attrs.containsKey("Track"))
				try {
					attrs.put("Track", new Integer((String)attrs.get("Track")));
				} catch(Exception e) {
					attrs.remove("Track");
				}
			storeAttr(id3, "Genre", ID3v2.GENRE);
			byte[] pic = id3.getPicture().getBinaryContent();
			//System.out.printf("Pic%s%n", pic);
			if (pic != null)
				attrs.put("Picture", pic);
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			
		}
	}
	
	protected void storeAttr(ID3v2 id3, String name, int id) {
		try {
			TagContent tf = TextFrameEncoding.read(id3, id3.getFrameCode(id));
			//System.err.printf("proc %s %s%n", name, tf);
			if (tf != null) {
				attrs.put(name, tf.getTextContent());
			}
		} catch (Exception e) {
			
		}	
	}

	long position;
	HashMap <String, Object> attrs;
}
