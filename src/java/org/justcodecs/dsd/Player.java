package org.justcodecs.dsd;

import java.io.File;
import java.io.FileNotFoundException;

import org.justcodecs.dsd.Decoder.DecodeException;

public class Player {

	public static void main(String[] args) {
		System.out.printf("Java DSD player for PCM DAC  (c) 2014%n");
		if (args.length == 0) {
			System.out.printf("Please use with at least one DSF file argument%n");
			System.exit(-1);
			return;
		}
		
		for(String f:args) {
			try {
				new Player().play(f);
			} catch (DecodeException e) {
				System.out.printf("Couldn't play %s, becasue %s%n", f, e);
				e.printStackTrace();
			}
		}

	}

	public void play(String f) throws Decoder.DecodeException {
		Decoder decoder = new Decoder();
		try {
			decoder.init(new Utils.RandomDSDStream(new File(f)));
			
		} catch (FileNotFoundException e) {
			throw new Decoder.DecodeException("Not found "+f, e);
		}
		
	}

}
