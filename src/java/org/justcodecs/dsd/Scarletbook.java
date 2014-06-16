package org.justcodecs.dsd;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;

import org.justcodecs.dsd.Decoder.DecodeException;

public interface Scarletbook {
	static final int SACD_LSN_SIZE = 2048;
	static final int SACD_SAMPLING_FREQUENCY = 2822400;

	static final int START_OF_FILE_SYSTEM_AREA = 0;
	static final int START_OF_MASTER_TOC = 510;
	static final int MASTER_TOC_LEN = 10;
	static final int MAX_AREA_TOC_SIZE_LSN = 96;
	static final int MAX_LANGUAGE_COUNT = 8;
	static final int MAX_CHANNEL_COUNT = 6;
	static final int SAMPLES_PER_FRAME = 588;
	static final int FRAME_SIZE_64 = (SAMPLES_PER_FRAME * 64 / 8);
	static final int SUPPORTED_VERSION_MAJOR = 1;
	static final int SUPPORTED_VERSION_MINOR = 20;

	static final int MAX_GENRE_COUNT = 29;
	static final int MAX_CATEGORY_COUNT = 3;
	static final int SACD_PSN_SIZE = 2064;

	static final int SACD_FRAME_RATE = 75;

	static final int FRAME_FORMAT_DST = 0;
	static final int FRAME_FORMAT_DSD_3_IN_14 = 2;
	static final int FRAME_FORMAT_DSD_3_IN_16 = 3;

	static final short AUDIO_PACKET_INFO_SIZE = 2;
	static final short AUDIO_FRAME_DST_INFO_SIZE = 4; // for DST only
	static final short AUDIO_FRAME_INFO_SIZE = 3; // for two channels
	static final short AUDIO_SECTOR_HEADER_SIZE = 1;

	static final int DATA_TYPE_AUDIO = 2;
	static final int DATA_TYPE_SUPPLEMENTARY = 3;
	static final int DATA_TYPE_PADDING = 7;
	
	static final int MAX_DST_SIZE = (1024 * 64);

	static final String CHARSET1[] = { null //      = 0
			, "ISO646" //        = 1    // ISO 646 (IRV), no escape sequences allowed
			, "ISO8859_1" //     = 2    // ISO 8859-1, no escape sequences allowed
			, "RIS506" //        = 3    // MusicShiftJIS, per RIS-506 (RIAJ), Music Shift-JIS Kanji
			, "KSC5601" //       = 4    // Korean KSC 5601-1987
			, "GB2312" //        = 5    // Chinese GB 2312-80
			, "BIG5" //          = 6    // Big5
			, "ISO8859_1_ESC" // = 7    // ISO 8859-1, single byte set escape sequences allowed
	};

	static final String CHARSET[] = { "US-ASCII", "ISO646-JP", "ISO-8859-1", "SHIFT_JISX0213", "KSC5601.1987-0",
			"GB2312.1980-0", "BIG5", "ISO-8859-1" };

	static final String GENRE[] = { "Not used", "Not defined", "Adult Contemporary", "Alternative Rock",
			"Children's Music", "Classical", "Contemporary Christian", "Country", "Dance", "Easy Listening", "Erotic",
			"Folk", "Gospel", "Hip Hop", "Jazz", "Latin", "Musical", "New Age", "Opera", "Operetta", "Pop Music",
			"RAP", "Reggae", "Rock Music", "Rhythm & Blues", "Sound Effects", "Sound Track", "Spoken Word",
			"World Music", "Blues" };

	public static class Genre {
		byte category;
		String genre;
		static byte tb[] = new byte[4];

		void read(DSDStream ds) throws DecodeException {
			try {
				ds.readFully(tb, 0, 4);
				category = tb[0];
				genre = GENRE[tb[3]];
			} catch (IOException e) {
				throw new DecodeException("IO", e);
			}
		}

		@Override
		public String toString() {
			return "Genre [category=" + category + ", genre=" + genre + "]";
		}

	}

	public static class LocaleTable {
		String code;
		String encoding;
		static byte tb[] = new byte[4];

		void read(DSDStream ds) throws DecodeException {
			try {
				ds.readFully(tb, 0, 4);
				if (tb[0] == 0)
					return;
				code = new String(tb, 0, 2);
				encoding = CHARSET[tb[2] & 7];
			} catch (IOException e) {
				throw new DecodeException("IO", e);
			} // TODO invalid index
		}

		@Override
		public String toString() {
			return "LocaleTable [code=" + code + ", encoding=" + encoding + "]";
		}

	}

	public static class TOC {
		byte[] id = new byte[8]; // SACDMTOC
		byte major;
		byte minor;
		short albumSetSize;
		short albumSequenceNumber;
		String albumCatalogNumber; // 0x00 when empty, else padded with spaces for short strings
		Genre albumGenre[] = new Genre[4];
		int area1Toc1Start;
		int area1Toc2Start;
		int area_2_toc_1_start;
		int area_2_toc_2_start;
		short typeHybrid;
		short area1TocSize;
		short area_2_toc_size;
		String discCatalogNumber; // 0x00 when empty, else padded with spaces for short strings
		Genre discGenre[] = new Genre[4];
		short disc_date_year;
		byte disc_date_month;
		byte disc_date_day;
		byte text_area_count;
		LocaleTable locales[] = new LocaleTable[MAX_LANGUAGE_COUNT];
		static byte[] tb = new byte[20];

		void read(DSDStream ds) throws DecodeException {
			try {
				ds.readFully(id, 0, id.length);
				if ("SACDMTOC".equals(new String(id)) == false)
					throw new DecodeException("It doesn't be appear SACD image", null);
				ds.readFully(tb, 0, 2);
				major = (byte) (tb[0] & 255);
				minor = (byte) (tb[1] & 255);
				ds.seek(ds.getFilePointer() + 6);
				albumSetSize = ds.readShort(true);
				albumSequenceNumber = ds.readShort(true);
				ds.seek(ds.getFilePointer() + 4);
				ds.readFully(tb, 0, 16);
				if (tb[0] == 0)
					albumCatalogNumber = "";
				else
					albumCatalogNumber = new String(tb, 0, 16).trim();
				for (int i = 0; i < 4; i++) {
					albumGenre[i] = new Genre();
					albumGenre[i].read(ds);
				}
				ds.seek(ds.getFilePointer() + 8);
				area1Toc1Start = ds.readInt(true);
				area1Toc2Start = ds.readInt(true);
				area_2_toc_1_start = ds.readInt(true);
				area_2_toc_2_start = ds.readInt(true);
				typeHybrid = ds.readShort(false);
				ds.seek(ds.getFilePointer() + 2);
				area1TocSize = ds.readShort(true);
				area_2_toc_size = ds.readShort(true);
				ds.readFully(tb, 0, 16);
				if (tb[0] == 0)
					discCatalogNumber = "";
				else
					discCatalogNumber = new String(tb, 0, 16).trim();
				for (int i = 0; i < 4; i++) {
					discGenre[i] = new Genre();
					discGenre[i].read(ds);
				}
				disc_date_year = ds.readShort(true);
				ds.readFully(tb, 0, 2);
				disc_date_month = tb[0];
				disc_date_day = tb[1];
				ds.seek(ds.getFilePointer() + 4);
				ds.readFully(tb, 0, 1);
				text_area_count = tb[0];
				ds.seek(ds.getFilePointer() + 7);
			} catch (IOException e) {
				throw new DecodeException("IO", e);
			}
		}

		@Override
		public String toString() {
			return "TOC [id=" + Arrays.toString(id) + ", major=" + major + ", minor=" + minor + ", albumSetSize="
					+ albumSetSize + ", albumSequenceNumber=" + albumSequenceNumber + ", albumCatalogNumber="
					+ albumCatalogNumber + ", album_genre=" + Arrays.toString(albumGenre) + ", area_1_toc_1_start="
					+ area1Toc1Start + ", area_1_toc_2_start=" + area1Toc2Start + ", area_2_toc_1_start="
					+ area_2_toc_1_start + ", area_2_toc_2_start=" + area_2_toc_2_start + ", typeHybrid=" + typeHybrid
					+ ", area_1_toc_size=" + area1TocSize + ", area_2_toc_size=" + area_2_toc_size
					+ ", disc_catalog_number=" + discCatalogNumber + ", disc_genre=" + Arrays.toString(discGenre)
					+ ", disc_date_year=" + disc_date_year + ", disc_date_month=" + disc_date_month
					+ ", disc_date_day=" + disc_date_day + ", text_area_count=" + text_area_count + ", locales="
					+ Arrays.toString(locales) + "]";
		}

	}

	static class AreaTOC {
		byte id[] = new byte[8]; // TWOCHTOC or MULCHTOC
		byte major;
		byte minor; // 1.20 / 0x0114
		short size; // ex. 40 (total size of TOC)
		boolean stereo;
		int max_byte_rate;
		int sample_frequency; // 0x04 = (64 * 44.1 kHz) (physically there can be no other values, or..? :)
		byte frame_format;

		byte channel_count;
		byte loudspeaker_config;

		byte max_available_channels;
		byte area_mute_flags;
		byte track_attribute;

		byte minutes;
		byte seconds;
		byte frames;

		byte track_offset;
		byte track_count;

		int track_start;
		int track_end;
		byte text_area_count;

		LocaleTable locales[] = new LocaleTable[MAX_LANGUAGE_COUNT + 2];
		short track_text_offset;
		short index_list_offset;
		short access_list_offset;
		short area_description_offset;
		short copyright_offset;
		short area_description_phonetic_offset;
		short copyright_phonetic_offset;
		byte data[] = new byte[1896];
		static byte[] tb = new byte[20];

		void read(DSDStream ds) throws DecodeException {
			// TODO consider read in byte buffer and then extract parts
			try {
				ds.readFully(id, 0, id.length);
				String ID = new String(id);
				stereo = "TWOCHTOC".equals(ID);
				if (!stereo && "MULCHTOC".equals(ID) == false)
					throw new DecodeException("Unsupported area toc:" + ID, null);
				ds.readFully(tb, 0, 2);
				major = (byte) (tb[0] & 255);
				minor = (byte) (tb[1] & 255);
				size = ds.readShort(true);
				ds.seek(ds.getFilePointer() + 4);
				max_byte_rate = ds.readInt(true);
				ds.readFully(tb, 0, 2); //System.out.printf("fr:%d%n", tb[0]);
				sample_frequency = 16 * 44100 * (tb[0] & 255);
				frame_format = tb[1];
				ds.seek(ds.getFilePointer() + 10);
				ds.readFully(tb, 0, 4);
				channel_count = tb[0];
				loudspeaker_config = tb[1];
				max_available_channels = tb[2];
				area_mute_flags = tb[3];
				ds.seek(ds.getFilePointer() + 12);
				ds.readFully(tb, 0, 1);
				track_attribute = tb[0];
				ds.seek(ds.getFilePointer() + 15);
				ds.readFully(tb, 0, 3);
				minutes = tb[0];
				seconds = tb[1];
				frames = tb[3];
				ds.readFully(tb, 0, 5);
				track_offset = tb[1];
				track_count = tb[2];
				track_start = ds.readInt(true);
				track_end = ds.readInt(true);
				ds.readFully(tb, 0, 8);
				text_area_count = tb[0];

				for (int i = 0; i < locales.length; i++) {
					locales[i] = new LocaleTable();
					locales[i].read(ds);
				}
				track_text_offset = ds.readShort(true);
				index_list_offset = ds.readShort(true);
				access_list_offset = ds.readShort(true);
				ds.seek(ds.getFilePointer() + 10);
				area_description_offset = ds.readShort(true);
				copyright_offset = ds.readShort(true);
				area_description_phonetic_offset = ds.readShort(true);
				copyright_phonetic_offset = ds.readShort(true);
				////ds.readFully(data, 0, data.length);
				//System.out.printf("Copyright %s%n", new String(data, 0, data.length));
			} catch (IOException e) {
				throw new DecodeException("IO", e);
			}
		}

		@Override
		public String toString() {
			return "AreaTOC [id=" + Arrays.toString(id) + ", major=" + major + ", minor=" + minor + ", size=" + size
					+ ", stereo=" + stereo + ", max_byte_rate=" + max_byte_rate + ", sample_frequency="
					+ sample_frequency + ", frame_format=" + frame_format + ", channel_count=" + channel_count
					+ ", loudspeaker_config=" + loudspeaker_config + ", max_available_channels="
					+ max_available_channels + ", area_mute_flags=" + area_mute_flags + ", track_attribute="
					+ track_attribute + ", minutes=" + minutes + ", seconds=" + seconds + ", frames=" + frames
					+ ", track_offset=" + track_offset + ", track_count=" + track_count + ", track_start="
					+ track_start + ", track_end=" + track_end + ", text_area_count=" + text_area_count + ", locales="
					+ Arrays.toString(locales) + ", track_text_offset=" + track_text_offset + ", index_list_offset="
					+ index_list_offset + ", access_list_offset=" + access_list_offset + ", area_description_offset="
					+ area_description_offset + ", copyright_offset=" + copyright_offset
					+ ", area_description_phonetic_offset=" + area_description_phonetic_offset
					+ ", copyright_phonetic_offset=" + copyright_phonetic_offset + "]";
		}

	}

	static class FrmHeader {
		static byte buf[] = new byte[1 + 7 * (AUDIO_PACKET_INFO_SIZE + AUDIO_FRAME_DST_INFO_SIZE)];

		boolean dst;
		int frame_info_count;
		int packet_info_count;

		void read(DSDStream ds) throws DecodeException {
			try {
				ds.readFully(buf, 0, 1);
				dst = (buf[0] & 1) == 1;
				frame_info_count = (buf[0] >> 2) & 7;
				packet_info_count = (buf[0] >> 5) & 7;
				ds.readFully(buf, 1, AUDIO_PACKET_INFO_SIZE * packet_info_count
						+ (dst ? AUDIO_FRAME_DST_INFO_SIZE : AUDIO_FRAME_INFO_SIZE) * frame_info_count);
			} catch (IOException e) {
				throw new DecodeException("I/O", e);
			}
		}

		int getPackLen(int pacNo) {
			if (pacNo < 0 || pacNo >= packet_info_count)
				return -1;
			return buf[1 + pacNo * 2 + 1] + ((buf[1 + pacNo * 2] & 7) << 8);
		}

		int getDataType(int pacNo) {
			if (pacNo < 0 || pacNo >= packet_info_count)
				return -1;
			return (buf[1 + pacNo * 2] >> 3) & 7;
		}

		boolean isFrameStart(int pacNo) {
			if (pacNo < 0 || pacNo >= packet_info_count)
				return false;
			return ((buf[1 + pacNo * 2] >> 7) & 1) == 1;
		}

		int getMinutes(int frmNo) {
			if (frmNo < 0 || frmNo >= frame_info_count)
				return -1;
			return buf[1 + packet_info_count * AUDIO_PACKET_INFO_SIZE + frmNo * 4];
		}

		int getSeconds(int frmNo) {
			if (frmNo < 0 || frmNo >= frame_info_count)
				return -1;
			return buf[1 + packet_info_count * AUDIO_PACKET_INFO_SIZE + frmNo * 4 + 1];
		}

		int getFrames(int frmNo) {
			if (frmNo < 0 || frmNo >= frame_info_count)
				return -1;
			return buf[1 + packet_info_count * AUDIO_PACKET_INFO_SIZE + frmNo * 4 + 2];
		}

		byte getChannelsByte(int frmNo) {
			return buf[1 + packet_info_count * AUDIO_PACKET_INFO_SIZE + frmNo * 4 + 3];
		}

		int getSectorCount(int frmNo) {
			if (frmNo < 0 || frmNo >= frame_info_count)
				return -1;
			return (getChannelsByte(frmNo) >> 2) & 0x1f;
		}

		int getChannelBit1(int frmNo) {
			if (frmNo < 0 || frmNo >= frame_info_count)
				return -1;
			return (getChannelsByte(frmNo) >> 7) & 0x1;
		}

		int getChannelBit2(int frmNo) {
			if (frmNo < 0 || frmNo >= frame_info_count)
				return -1;
			return (getChannelsByte(frmNo) >> 1) & 0x1;
		}

		int getChannelBit3(int frmNo) {
			if (frmNo < 0 || frmNo >= frame_info_count)
				return -1;
			return getChannelsByte(frmNo) & 0x1;
		}

		@Override
		public String toString() {
			String infos = "";

			for (int i = 0; i < packet_info_count; i++) {
				infos += " Packet " + i + ", lengh " + getPackLen(i) + ", start " + isFrameStart(i) + ", type "
						+ getDataType(i);
			}
			for (int i = 0; i < frame_info_count; i++) {
				infos += " Frame " + i + ",m:" + getMinutes(i) + ",s:" + getSeconds(i) + ",f:" + getFrames(i)
						+ ", sector count " + getSectorCount(i) + ", channel bit1 " + getChannelBit1(i)
						+ ", channel bit2 " + getChannelBit2(i) + ", channel bit3 " + getChannelBit3(i);
			}
			return "FrmHeader [dst=" + dst + ", frame_info_count=" + frame_info_count + ", packet_info_count="
					+ packet_info_count + infos + "]";
		}

	}

	static class CDText {
		byte id[] = new byte[8]; // SACDText
		short album_title_position;
		short album_artist_position;
		short album_publisher_position;
		short album_copyright_position;
		short album_title_phonetic_position;
		short album_artist_phonetic_position;
		short album_publisher_phonetic_position;
		short album_copyright_phonetic_position;
		short disc_title_position;
		short disc_artist_position;
		short disc_publisher_position;
		short disc_copyright_position;
		short disc_title_phonetic_position;
		short disc_artist_phonetic_position;
		short disc_publisher_phonetic_position;
		short disc_copyright_phonetic_position;
		static byte data[] = new byte[2000];
		HashMap<String, String> textInfo = new HashMap<String, String>();

		void read(DSDStream ds, String encoding) throws DecodeException {
			//if (encoding == null)
			encoding = "UTF-8";
			try {
				ds.readFully(id, 0, id.length);
				String ID = new String(id);
				if ("SACDText".equals(ID) == false)
					throw new DecodeException("Text area not found " + ID, null);
				ds.seek(ds.getFilePointer() + 8);
				album_title_position = ds.readShort(true);
				album_artist_position = ds.readShort(true);
				album_publisher_position = ds.readShort(true);
				album_copyright_position = ds.readShort(true);
				album_title_phonetic_position = ds.readShort(true);
				album_artist_phonetic_position = ds.readShort(true);
				album_publisher_phonetic_position = ds.readShort(true);
				album_copyright_phonetic_position = ds.readShort(true);
				disc_title_position = ds.readShort(true);
				disc_artist_position = ds.readShort(true);
				disc_publisher_position = ds.readShort(true);
				disc_copyright_position = ds.readShort(true);
				disc_title_phonetic_position = ds.readShort(true);
				disc_artist_phonetic_position = ds.readShort(true);
				disc_publisher_phonetic_position = ds.readShort(true);
				disc_copyright_phonetic_position = ds.readShort(true);
				ds.readFully(data, 0, data.length);
				addText("album_title", album_title_position, encoding);
				addText("album_artist", album_artist_position, encoding);
				addText("album_publisher", album_publisher_position, encoding);
				addText("album_copyright", album_copyright_position, encoding);
				addText("album_title_phonetic", album_title_phonetic_position, encoding);
				addText("album_artist_phonetic", album_artist_phonetic_position, encoding);
				addText("album_title_phonetic", album_title_phonetic_position, encoding);
				addText("album_artist_phonetic", album_artist_phonetic_position, encoding);
				addText("album_publisher_phonetic", album_publisher_phonetic_position, encoding);
				addText("album_copyright_phonetic", album_copyright_phonetic_position, encoding);
				addText("disc_title", disc_title_position, encoding);
				addText("disc_artist", disc_artist_position, encoding);
				addText("disc_publisher", disc_publisher_position, encoding);
				addText("disc_copyright", disc_copyright_position, encoding);
				addText("disc_title_phonetic", disc_title_phonetic_position, encoding);
				//System.out.printf("Text %s%n", textInfo);
			} catch (IOException e) {
				throw new DecodeException("IO", e);
			}
		}

		public String getText(String name) {
			return textInfo.get(name);
		}

		void addText(String name, short offset, String encoding) {
			if (offset == 0)
				return;
			try {
				for (int len = 0; len < 255; len++) {
					if (data[offset - 48 + len] == 0) {
						if (len > 0)
							textInfo.put(name, new String(data, offset - 48, len, encoding));
						return;
					}
				}
				textInfo.put(name, new String(data, offset - 48, 255, encoding));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static class TrackInfo extends HashMap<String, String> {
		int position;
		public int start;
		public int duration;

		static final int TRACK_TYPE_TITLE = 0x01;
		static final int TRACK_TYPE_PERFORMER = 0x02;
		static final int TRACK_TYPE_SONGWRITER = 0x03;
		static final int TRACK_TYPE_COMPOSER = 0x04;
		static final int TRACK_TYPE_ARRANGER = 0x05;
		static final int TRACK_TYPE_MESSAGE = 0x06;
		static final int TRACK_TYPE_EXTRA_MESSAGE = 0x07;

		static final int TRACK_TYPE_TITLE_PHONETIC = 0x81;
		static final int TRACK_TYPE_PERFORMER_PHONETIC = 0x82;
		static final int TRACK_TYPE_SONGWRITER_PHONETIC = 0x83;
		static final int TRACK_TYPE_COMPOSER_PHONETIC = 0x84;
		static final int TRACK_TYPE_ARRANGER_PHONETIC = 0x85;
		static final int TRACK_TYPE_MESSAGE_PHONETIC = 0x86;
		static final int TRACK_TYPE_EXTRA_MESSAGE_PHONETIC = 0x87;

		TrackInfo(int pos) {
			position = pos;
			//System.out.printf("Track for %d%n", pos);
		}

		public void fill(byte[] data, int off) {
			int cp = position - off;
			byte amount = data[cp];
			//System.out.printf("Amount %d,  pos %d%n", amount, cp);
			cp += 4;
			for (int i = 0; i < amount; i++) {
				byte type = data[cp];
				cp += 2;
				if (data[cp] != 0) {
					switch (type & 255) {
					case TRACK_TYPE_TITLE:
						cp += addText(data, "title", cp, "UTF-8");
						//System.out.printf("Title %s%n", get("title"));
						break;
					case TRACK_TYPE_PERFORMER:
						cp += addText(data, "performer", cp, "UTF-8");
						break;
					default:
						while (cp < data.length && data[cp] != 0)
							cp++;
					}
				}

				while (cp < data.length && data[cp] == 0)
					cp++;
			}
		}

		int addText(byte[] data, String name, int cp, String encoding) {
			try {
				for (int len = 0; len < 255; len++) {
					if (data[cp + len] == 0) {
						if (len > 0)
							put(name, new String(data, cp, len, encoding));
						return len;
					}
				}
				put(name, new String(data, cp, 255, encoding));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return 255;
		}

		@Override
		public String toString() {
			return "TrackInfo [start=" + start + ", duration=" + duration + ", toString()=" + super.toString() + "]";
		}

	}

	static class TrackText {
		byte[] id = new byte[8];
		TrackInfo[] infos;
		static byte data[] = new byte[2000];

		public TrackText(byte trackCount) {
			infos = new TrackInfo[trackCount];
		}

		void read(DSDStream ds, String encoding) throws DecodeException {
			//if (encoding == null)
			encoding = "UTF-8";
			try {
				ds.readFully(id, 0, id.length);
				String ID = new String(id);
				if ("SACDTTxt".equals(ID) == false) {
					ds.seek(ds.getFilePointer() - 8);
					throw new DecodeException("Track text not found " + ID, null);
				}
				int off = 0;
				//System.out.printf("Entries:%d%n", infos.length);
				for (int i = 0; i < infos.length; i++) {
					short pos = ds.readShort(true);
					if (pos > 0) {
						infos[i] = new TrackInfo(pos);
						if (off == 0)
							off = pos;
						else if (pos < off)
							off = pos;
					}
				}
				//off -= id.length+infos.length*2;
				//System.out.printf("Entries:%d, red %d%n", infos.length, off);
				ds.seek(ds.getFilePointer() + off - (id.length + infos.length * 2));
				ds.readFully(data, 0, data.length);
				for (int i = 0; i < infos.length; i++) {
					if (infos[i] != null) {
						infos[i].fill(data, off);
					}
				}
			} catch (IOException e) {
				throw new DecodeException("IO", e);
			}
		}
	}

	static class TrackTime {
		byte[] id = new byte[8];
		static byte data[] = new byte[4 * 255 * 2];

		void read(DSDStream ds) throws DecodeException {
			try {
				ds.readFully(id, 0, id.length);
				String ID = new String(id);
				if ("SACDTRL2".equals(ID) == false) {
					ds.seek(ds.getFilePointer() - 8);
					throw new DecodeException("Track time not found " + ID, null);
				}
				ds.readFully(data, 0, data.length);
			} catch (IOException e) {
				throw new DecodeException("IO", e);
			}
		}

		int getStart(int trackN) {
			int off = trackN * 4;
			return data[off] * 60 + data[off + 1] + data[off + 2 / SACD_FRAME_RATE];
		}

		int getDuration(int trackN) {
			int off = 255 * 4 + trackN * 4;
			return data[off] * 60 + data[off + 1] + data[off + 2 / SACD_FRAME_RATE];
		}
	}

}
