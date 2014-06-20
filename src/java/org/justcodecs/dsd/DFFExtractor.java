package org.justcodecs.dsd;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class DFFExtractor {
	public static void main(String... args) {
		System.out.printf("Java SACD ISO -> DFF extractor  (c) 2014 D. Rogatkin%n");
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
				else {
					isoF = arg;
					break;
				}
			}
		}
		if (trgDir == null)
			trgDir = ".";
		try {
			extractDff(new File(isoF), new File(trgDir), track, cue);
			System.out.printf("Done%n");
		} catch (ExtractionProblem e) {
			System.out.printf("Problem %s%n", e);
		}
	}

	private static void displayHelp() {
		System.out
				.printf("Usage: [-d <target_directory>] [-n] [-t <nn>] <ISO path>%n where: n - no cue,%n        t - extract only specified track");
	}

	static class ExtractionProblem extends Exception {
		public ExtractionProblem(String reason) {
			super(reason);
		}
	}

	static void extractDff(File iso, File target, int track, boolean cue) throws ExtractionProblem {
		RandomAccessFile dff = null;
		DISOFormat dsf = new DISOFormat();
		try {

			dsf.init(new Utils.RandomDSDStream(iso));
			String album = (String) dsf.getMetadata("Album");
			if (album == null)
				album = (String) dsf.getMetadata("Title");
			if (album == null) {
				album = iso.getName();
				album = album.substring(0, album.length() - 4);
			}
			dff = new RandomAccessFile(new File(target, normalize(album) + ".dff"), "rw");
			long hdrSize = writeDFFHeader(dff, dsf);
			long len = 0;
			dsf.initBuffers(0);
			byte samples[] = dsf.getSamples();
			while (dsf.readDataBlock()) {
				dff.write(samples, 0, dsf.bufEnd);
				dsf.bufPos = dsf.bufEnd;
				len += dsf.bufEnd;
			}
			dff.writeByte(0);
			dff.seek(4);
			dff.writeLong(len + hdrSize - 12);
			dff.seek(hdrSize - 8);
			dff.writeLong(len);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ExtractionProblem(e.getMessage());
		} finally {
			try {
				dff.close();
			} catch (Exception e) {
			}
			dsf.close();
		}
	}

	private static String normalize(String album) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < album.length(); i++) {
			switch (album.charAt(i)) {
			case '"':
				continue;
			default:
				result.append(album.charAt(i));
			}
		}
		return result.toString();
	}

	static long writeDFFHeader(RandomAccessFile dff, DISOFormat dsf) throws IOException {
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
		dff.writeLong(0);
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
		dff.writeByte(0);
		dff.writeByte(0);
		dff.writeByte((size >> 8) & 255);
		dff.writeByte(size & 255);
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
		dff.writeLong(14);
		dff.writeBytes("not compressed");
		dff.writeByte(0);
		dff.writeBytes("DSD ");
		dff.writeLong(0);
		return dff.getFilePointer();
	}
}
