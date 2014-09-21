package org.justcodecs.dsd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;

import org.justcodecs.dsd.Decoder.DecodeException;

public class DFFExtractor {

	public static interface Progress {
		void init(long total, File resultFile, File cueFile);

		void progress(long current);
	}

	public static void main(String... args) {
		System.out.printf("Java SACD ISO -> DFF extractor/player  (c) 2014 D. Rogatkin%n");
		if (args.length == 0) {
			displayHelp();
			System.exit(1);
		}
		String isoF = null;
		String trgDir = null;
		int track = 0;
		boolean cue = true;
		boolean tde = false;
		boolean tre = false;
		boolean ove = false;
		boolean ply = false;
		for (String arg : args) {
			if ("-d".equals(arg))
				tde = true;
			else {
				if (tde) {
					trgDir = arg;
					tde = false;
				} else if (tre) {
					track = Integer.parseInt(arg);
					tre = false;
				} else if ("-t".equals(arg))
					tre = true;
				else if ("-n".equals(arg))
					cue = false;
				else if ("-f".equals(arg))
					ove = true;
				else if ("-p".equals(arg))
					ply = true;
				else {
					isoF = arg;
					break;
				}
			}
		}
		if (trgDir == null)
			trgDir = ".";
		if (ply)
			try {
				new Player().play(isoF);
			} catch (DecodeException e) {
				System.out.printf("Couldn't play %s, becasue %s%n", isoF, e);
			}
		else
			try {
				System.out.printf("Extracting... it may take awhile... ");
				long st = System.currentTimeMillis();
				extractDff(new File(isoF), new File(trgDir), track, cue, ove, new Progress() {
					long t;
					long s;

					@Override
					public void init(long total, File resultFile, File cueFile) {
						t = total;
						s = System.currentTimeMillis();
						System.out.printf("Writing to %s%n", resultFile);
					}

					@Override
					public void progress(long current) {
						long r = (System.currentTimeMillis()-s)*(t-current)/current / 1000;
						System.out.printf("%3.2f%% to complete in %d:%02d  \r", current * 100.0 / t, r/60, r%60);

					}

				});
				st = System.currentTimeMillis() - st;
				st /= 1000;
				System.out.printf("Done in %d:%02d                %n", st / 60, st % 60);
			} catch (ExtractionProblem e) {
				System.out.printf("Problem %s%n", e);
			}
	}

	private static void displayHelp() {
		System.out
				.printf("Usage: [-d <target_directory>] [-n] [-t <nn>] [-f] [-p] <ISO path>%n where: n - no cue,%n        f - overwrite existing files%n        t - extract specified track only%n        p - play specified file instead of extraction");
	}

	public static class ExtractionProblem extends Exception {
		public ExtractionProblem(String reason) {
			super(reason);
		}
	}

	public static void extractDff(File iso, File target, int track, boolean cue, boolean ove, Progress progress)
			throws ExtractionProblem {
		RandomAccessFile dff = null;
		DISOFormat dsf = new DISOFormat();
		OutputStreamWriter cuew = null;
		try {
			dsf.init(new Utils.RandomDSDStream(iso));
			String album = (String) dsf.getMetadata("Album");
			if (album == null)
				album = (String) dsf.getMetadata("Title");
			if (album == null) {
				album = iso.getName();
				album = album.substring(0, album.length() - 4);
			}
			Scarletbook.TrackInfo[] tracks = (Scarletbook.TrackInfo[]) dsf.getMetadata("Tracks");
			File df = new File(target, normalize(album) + ".dff");
			File cuef = null;
			long seek = 0;
			long trackLen = 0;
			if (tracks != null) {
				Scarletbook.TrackInfo tr = null;
				if (track > 0) {
					track--;
					if (track < tracks.length) {
						tr = tracks[track];
						seek = (long) tr.start * dsf.getSampleRate();
						//System.out.printf("Seek to sampl %d at %d%n", seek, tr.start );
						trackLen = (long) (tr.duration + 1) * dsf.getNumChannels() * dsf.getSampleRate() / 8;
						album = Utils.nvl((String) tr.get("title"), String.format("Track %02d", track + 1));
					}
				}
				if (cue) { // TODO can be created without tracks
					cuef = new File(target, normalize(album) + ".cue");
					if (cuef.exists() && !ove)
						throw new ExtractionProblem("CUE " + cuef + " already exists");
					cuew = new OutputStreamWriter(new FileOutputStream(cuef), "UTF-8");
					cuew.write(String.format("REM GENRE \"%s\"%n", Utils.nvl(dsf.getMetadata("Genre"), "NA")));
					cuew.write(String.format("REM DATE %s%n", dsf.getMetadata("Year").toString()));
					cuew.write(String.format("REM DISCID %s%n", quoteIt(dsf.toc.discCatalogNumber)));
					cuew.write(String.format("REM TOTAL %02d:%02d%n", dsf.atoc.minutes, dsf.atoc.seconds));
					cuew.write("REM COMMENT \"JustDSD https://github.com/drogatkin/JustDSD\"\r\n");
					cuew.write(String.format("PERFORMER \"%s\"%n",
							Utils.nvl(normalizeName((String) dsf.getMetadata("Artist")), "NA")));
					cuew.write(String.format(
							"TITLE \"%s\"%n",
							Utils.nvl(normalizeName((String) dsf.getMetadata("Title")),
									normalizeName((String) dsf.getMetadata("Album")), "NA")));
					cuew.write(String.format("FILE \"%s\" WAVE%n", df.getName()));
					if (tr == null) {
						for (int t = 0; t < tracks.length; t++) {
							cuew.write(String.format("  TRACK %02d AUDIO%n", t + 1));
							cuew.write(String.format("    TITLE \"%s\"%n",
									Utils.nvl(normalizeName(tracks[t].get("title")), "NA")));
							if (tracks[t].get("performer") != null)
								cuew.write(String.format("    PERFORMER \"%s\"%n",
										normalizeName(tracks[t].get("performer"))));
							if (dsf.textDuration > 0) {
								double adj = ((double) dsf.getSampleCount()) / dsf.textDuration / dsf.getSampleRate();
								int start = (int) Math.round(adj * tracks[t].start);
								cuew.write(String.format("    INDEX 01 %02d:%02d:%02d%n", start / 60, start % 60, 0));
							} else
								cuew.write(String.format("    INDEX 01 %02d:%02d:%02d%n", tracks[t].start / 60,
										tracks[t].start % 60, tracks[t].startFrame));
						}
					} else {
						cuew.write(String.format("  TRACK 01 AUDIO%n"));
						cuew.write(String.format("    TITLE \"%s\"%n",
								Utils.nvl(normalizeName(tracks[track - 1].get("title")))));
						cuew.write(String.format("    PERFORMER \"%s\"%n",
								Utils.nvl(normalizeName(tracks[track - 1].get("performer")))));
						cuew.write(String.format("    INDEX 01 00:00:00%n"));
					}
					cuew.flush();
				}
			}
			if (df.exists() && !ove)
				throw new ExtractionProblem("File " + df + " already exists");
			dff = new RandomAccessFile(df, "rw");
			long hdrSize = writeDFFHeader(dff, dsf);
			long len = 0;
			dsf.initBuffers(0);
			byte samples[] = dsf.getSamples();
			dsf.seek(seek); // TODO track
			if (progress != null) {
				if (trackLen > 0)
					progress.init(trackLen, df, cuef);
				else
					progress.init(dsf.getSampleCount() * dsf.getNumChannels() / 8, df, cuef);
			}
			while (dsf.readDataBlock()) {
				dff.write(samples, 0, dsf.bufEnd);
				dsf.bufPos = dsf.bufEnd;
				len += dsf.bufEnd;
				if (progress != null)
					progress.progress(len);
				if (trackLen > 0 && len >= trackLen)
					break;
			}
			dff.writeByte(0);
			dff.seek(4);
			dff.writeLong(len + hdrSize - 12);
			dff.seek(hdrSize - 8);
			dff.writeLong(len);
		} catch (Exception e) {
			//e.printStackTrace();
			throw new ExtractionProblem(e.getMessage());
		} finally {
			try {
				dff.close();
			} catch (Exception e) {
			}
			if (cuew != null)
				try {
					cuew.close();
				} catch (IOException e) {
				}
			dsf.close();
		}
	}

	public static String normalize(String album) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < album.length(); i++) {
			switch (album.charAt(i)) {
			case '"':
			case '/':
			case '\\':
			case '?':
			case '*':
				continue;
			default:
				result.append(album.charAt(i));
			}
		}
		return result.toString().trim();
	}

	private static String normalizeName(String name) {
		if (name == null)
			return name;
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < name.length(); i++) {
			switch (name.charAt(i)) {
			case '"':
				result.append('\'');
				continue;
			case '\r':
			case '\n':
				continue;
			default:
				result.append(name.charAt(i));
			}
		}
		return result.toString().trim();
	}

	private static String quoteIt(String s) {
		if (s.indexOf(' ') < 0)
			return s;
		return String.format("\"%s\"", normalizeName(s));
	}

	private static long writeDFFHeader(RandomAccessFile dff, DISOFormat dsf) throws IOException {
		/*
		 *  write DSDIFF(DSD) header
		 *  ----------------------------
		 *  Create DST Header depending on number of channels
		 *  'FRM8'    4 + 8 + 4              = 16
		 *  'FVER'    4 + 8 + 4              = 16
		 *  'PROP'    4 + 8 + 4              = 16
		 *    'FS  '  4 + 8 + 4              = 16
		 *    'CHNL'  4 + 8 + 2 + 4*#ch      = 14 + 4*#ch
		 *    'CMPR'  4 + 8 + 4 + 1 + 14 + 1 = 32  ('not compressed'=14)
		 *  'DSD '    4 + 8                  = 12
		 */
		dff.writeBytes("FRM8");
		dff.writeLong(0); // to fill later
		dff.writeBytes("DSD ");
		dff.writeBytes("FVER");
		dff.writeLong(4);
		dff.writeInt(0x01040000);
		dff.writeBytes("PROP");
		dff.writeInt(0);
		int size = 66 + 4 * dsf.getNumChannels();
		dff.writeByte(0);
		dff.writeByte(0);
		dff.writeByte((size >> 8) & 255);
		dff.writeByte(size & 255);
		dff.writeBytes("SND ");
		dff.writeBytes("FS  ");
		dff.writeLong(4);
		/* m_Fsample44
		 * sampleRate =  64FS44 =  2822400 = 0x002B1100
		 * sampleRate = 128FS44 =  5644800 = 0x00562200
		 * sampleRate = 256FS44 = 11289600 = 0x00AC4400
		 */
		switch (dsf.getSampleRate()) {
		case 2822400:
			dff.writeInt(0x002B1100);
			break;
		case 5644800:
			dff.writeInt(0x00562200);
			break;
		case 11289600:
			dff.writeInt(0x00AC4400);
			break;
		}
		dff.writeBytes("CHNL");
		size = 2 + 4 * dsf.getNumChannels();
		dff.writeLong(size);

		//dff.writeByte((size >> 8) & 255);
		//dff.writeByte(size & 255);
		dff.writeByte(0);
		dff.writeByte(dsf.getNumChannels());
		switch (dsf.getNumChannels()) {
		case 2:
			dff.writeBytes("SLFTSRGT");
			break;
		case 5:
			dff.writeBytes("MLFTMRGTC   LS  RS  ");
			break;
		case 6:
			dff.writeBytes("MLFTMRGTC   LFE LS  RS  ");
			break;
		}
		dff.writeBytes("CMPR");
		dff.writeLong(20);
		dff.writeBytes("DSD ");
		dff.writeByte(14);
		dff.writeBytes("not compressed");
		dff.writeByte(0);
		dff.writeBytes("DSD ");
		dff.writeLong(0);
		return dff.getFilePointer();
	}
}
