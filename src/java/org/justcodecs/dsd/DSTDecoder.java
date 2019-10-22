package org.justcodecs.dsd;

import java.util.Arrays;

public class DSTDecoder {
	static final int RESOL = 8;
	static final int SIZE_MAXFRAMELEN = 4; /* Number of bits for representing framelength 
											in the frame header */
	static final int SIZE_NROFCHANNELS = 4; /* Number of bits for representing NrOfChannels
											in the frame header */
	static final int SIZE_DSTFRAMELEN = 16; /* Number of bits for representing the DST
											framelength in bytes in the frameheader */

	/* PREDICTION */
	static final int SIZE_CODEDPREDORDER = 7; /* Number of bits in the stream for representing
												the "CodedPredOrder" in each frame */
	static final int SIZE_PREDCOEF = 9; /* Number of bits in the stream for representing
										each filter coefficient in each frame */

	/* ARITHMETIC CODING */
	static final int AC_BITS = 8; /* Number of bits and maximum level for coding
									the probability */
	static final int AC_PROBS = (1 << AC_BITS);
	static final int AC_HISBITS = 6; /* Number of entries in the histogram */
	static final int AC_HISMAX = (1 << AC_HISBITS);
	static final int AC_QSTEP = (SIZE_PREDCOEF - AC_HISBITS); /* Quantization step 
																for histogram */

	/* RICE CODING OF PREDICTION COEFFICIENTS AND PTABLES */
	static final int NROFFRICEMETHODS = 3; /* Number of different Pred. Methods for filters
											used in combination with Rice coding       */
	static final int NROFPRICEMETHODS = 3; /* Number of different Pred. Methods for Ptables
											used in combination with Rice coding       */
	static final int MAXCPREDORDER = 3; /* max pred.order for prediction of
										filter coefs / Ptables entries             */
	static final int SIZE_RICEMETHOD = 2; /* nr of bits in stream for indicating method */
	static final int SIZE_RICEM = 3; /* nr of bits in stream for indicating m      */
	static final int MAX_RICE_M_F = 6; /* Max. value of m for filters                */
	static final int MAX_RICE_M_P = 4; /* Max. value of m for Ptables                */

	/* SEGMENTATION */
	static final int MAXNROF_FSEGS = 4; /* max nr of segments per channel for filters */
	static final int MAXNROF_PSEGS = 8; /* max nr of segments per channel for Ptables */
	static final int MIN_FSEG_LEN = 1024; /* min segment length in bits of filters      */
	static final int MIN_PSEG_LEN = 32; /* min segment length in bits of Ptables      */

	/* DSTXBITS */
	static final int MAX_DSTXBITS_SIZE = 256;

	/*  64FS44 =>  4704 */
	/* 128FS44 =>  9408 */
	/* 256FS44 => 18816 */
	static final int MAX_DSDBYTES_INFRAME = 18816;

	static final int MAX_CHANNELS = 6;
	static final int MAX_DSDBITS_INFRAME = (588 * 64);
	static final int MAXNROF_SEGS = 8; /* max nr of segments per channel for filters or Ptables */

	static final int PBITS = AC_BITS; /* number of bits for Probabilities             */
	static final int NBITS = 4; /* number of overhead bits: must be at least 2! */
	/* maximum "variable shift length" is (NBITS-1) */
	static final int PSUM = (1 << (PBITS));
	static final int ABITS = (PBITS + NBITS); /* must be at least PBITS+2     */
	static final int MB = 0; /* if (MB) print max buffer use */
	static final int ONE = (1 << ABITS);
	static final int HALF = (1 << (ABITS - 1));

	static final int FILTER = 0;
	static final int PTABLE = 1;

	///// error 
	static final int DSTErr_InvalidStuffingPattern = -2;

	static class DSTException extends Exception {
		int error;

		DSTException(int e) {
			error = e;
		}

		public DSTException(String s, int e) {
			super(s);
			error = e;
		}
	}

	static class Segment {
		int Resolution; /* Resolution for segments        */
		int SegmentLen[][] = new int[MAX_CHANNELS][MAXNROF_SEGS]; /* SegmentLen[ChNr][SegmentNr]    */
		int NrOfSegments[] = new int[MAX_CHANNELS]; /* NrOfSegments[ChNr]             */
		int Table4Segment[][] = new int[MAX_CHANNELS][MAXNROF_SEGS]; /* Table4Segment[ChNr][SegmentNr] */
	}

	static class FrameHeader {
		int FrameNr; /* Nr of frame that is currently processed    */
		int NrOfChannels; /* Number of channels in the recording        */
		int NrOfFilters; /* Number of filters used for this frame      */
		int NrOfPtables; /* Number of Ptables used for this frame      */
		int Fsample44; /* Sample frequency 64, 128, 256              */
		int[] PredOrder = new int[2 * MAX_CHANNELS]; /* Prediction order used for this frame       */
		int[] PtableLen = new int[2 * MAX_CHANNELS]; /* Nr of Ptable entries used for this frame   */
		int[][] ICoefA; /* Integer coefs for actual coding            */
		int DSTCoded; /* 1=DST coded is put in DST stream,          */
		/* 0=DSD is put in DST stream                 */
		int CalcNrOfBytes; /* Contains number of bytes of the complete   */
		long CalcNrOfBits; /* Contains number of bits of the complete    */
		/* channel stream after arithmetic encoding   */
		/* (also containing bytestuff-,               */
		/* ICoefA-bits, etc.)                         */
		int[] HalfProb = new int[MAX_CHANNELS]; /* Defines per channel which probability is   */
		/* applied for the first PredOrder[] bits of  */
		/* a frame (0 = use Ptable entry, 1 = 128)    */
		int[] NrOfHalfBits = new int[MAX_CHANNELS]; /* Defines per channel how many bits at the   */
		/* start of each frame are optionally coded   */
		/* with p=0.5                                 */
		Segment FSeg; /* Contains segmentation data for filters     */
		byte[][] Filter4Bit = new byte[MAX_CHANNELS][MAX_DSDBITS_INFRAME]; /* Filter4Bit[ChNr][BitNr]                    */
		Segment PSeg; /* Contains segmentation data for Ptables     */
		byte[][] Ptable4Bit = new byte[MAX_CHANNELS][MAX_DSDBITS_INFRAME]; /* Ptable4Bit[ChNr][BitNr]                    */
		int PSameSegAsF; /* 1 if segmentation is equal for F and P     */
		int PSameMapAsF; /* 1 if mapping is equal for F and P          */
		int FSameSegAllCh; /* 1 if all channels have same Filtersegm.    */
		int FSameMapAllCh; /* 1 if all channels have same Filtermap      */
		int PSameSegAllCh; /* 1 if all channels have same Ptablesegm.    */
		int PSameMapAllCh; /* 1 if all channels have same Ptablemap      */
		int SegAndMapBits; /* Number of bits in the stream for Seg&Map   */
		int MaxNrOfFilters; /* Max. nr. of filters allowed per frame      */
		int MaxNrOfPtables; /* Max. nr. of Ptables allowed per frame      */
		int MaxFrameLen; /* Max frame length of this file              */
		long ByteStreamLen; /* MaxFrameLen * NrOfChannels                 */
		long BitStreamLen; /* ByteStreamLen * RESOL                      */
		long NrOfBitsPerCh; /* MaxFrameLen * RESOL                        */

		FrameHeader() {
			FSeg = new Segment();
			PSeg = new Segment();
		}
	}

	static class CodedTable {
		int[] CPredOrder; /* Code_PredOrder[Method]                     */
		int[][] CPredCoef; /* Code_PredCoef[Method][CoefNr]              */
		int[] Coded; /* DST encode coefs/entries of Fir/PtabNr     */
		int[] BestMethod; /* BestMethod[Fir/PtabNr]                     */
		int[][] m; /* m[Fir/PtabNr][Method]                      */
		int[] DataLen; /* Fir/PtabDataLength[Fir/PtabNr]             */
		int StreamBits; /* nr of bits all filters use in the stream   */
		int TableType; /* FILTER or PTABLE: indicates contents       */

		CodedTable(FrameHeader fh) {
			CPredOrder = new int[NROFFRICEMETHODS];
			CPredCoef = new int[NROFFRICEMETHODS][MAXCPREDORDER];
			Coded = new int[fh.MaxNrOfFilters];
			BestMethod = new int[fh.MaxNrOfFilters];
			m = new int[fh.MaxNrOfPtables][NROFPRICEMETHODS];
			DataLen = new int[fh.MaxNrOfPtables];
		}
	}

	static final class StrData {
		byte[] pDSTdata;
		int TotalBytes;
		int ByteCounter;
		int BitPosition;
		byte DataByte;

		/***********************************************************************
		 * ResetReadingIndex
		 ***********************************************************************/

		void ResetReadingIndex() {
			BitPosition = 0;
			ByteCounter = 0;
			DataByte = 0;
		}

		/***********************************************************************
		 * CreateBuffer
		 * 
		 * @throws DSTException
		 ***********************************************************************/

		void CreateBuffer(int Size) throws DSTException {
			TotalBytes = Size;
		}

		/***********************************************************************
		 * DeleteBuffer
		 ***********************************************************************/

		void DeleteBuffer() {
			TotalBytes = 0;
			pDSTdata = null;
			ResetReadingIndex();
		}

		/***********************************************************************
		 * FillBuffer
		 ***********************************************************************/

		void FillBuffer(byte[] pBuf, int Size) throws DSTException {
			CreateBuffer(Size);
			pDSTdata = pBuf;
			ResetReadingIndex();
		}

		/***************************************************************************/
		/*                                                                         */
		/* name     : FIO_BitGetChrUnsigned                                        */
		/*                                                                         */
		/* function : Read a character as an unsigned number from file with a      */
		/*            given number of bits.                                        */
		/*                                                                         */
		/* pre      : Len, x, output file must be open by having used getbits_init */
		/*                                                                         */
		/* post     : The second variable in function call is filled with the      */
		/*            unsigned character read                                      */
		/*                                                                         */
		/* uses     : stdio.h, stdlib.h                                            */
		/*                                                                         */
		/***************************************************************************/

		byte FIO_BitGetChrUnsigned(int Len) throws DSTException {
			if (Len > 0) {
				//System.out.printf("CharU %d - %d%n", tmp[0], (byte) tmp[0]);
				return (byte) getbits(Len);
			} else if (Len == 0) {
				return 0;
			} else
				throw new DSTException("EOD", -1);
		}

		/***************************************************************************/
		/*                                                                         */
		/* name     : FIO_BitGetIntUnsigned                                        */
		/*                                                                         */
		/* function : Read an integer as an unsigned number from file with a       */
		/*            given number of bits.                                        */
		/*                                                                         */
		/* pre      : Len, x, output file must be open by having used getbits_init */
		/*                                                                         */
		/* post     : The second variable in function call is filled with the      */
		/*            unsigned integer read                                        */
		/*                                                                         */
		/* uses     : stdio.h, stdlib.h                                            */
		/*                                                                         */
		/***************************************************************************/

		int FIO_BitGetIntUnsigned(int Len) throws DSTException {
			if (Len > 0) {
				//System.out.printf("IntU %d - %d%n", tmp[0], (int) tmp[0]);
				return (int) getbits(Len);
			} else if (Len == 0) {
				return 0;
			} else
				throw new DSTException("EOD", -1);
		}

		/***************************************************************************/
		/*                                                                         */
		/* name     : FIO_BitGetIntSigned                                          */
		/*                                                                         */
		/* function : Read an integer as a signed number from file with a          */
		/*            given number of bits.                                        */
		/*                                                                         */
		/* pre      : Len, x, output file must be open by having used getbits_init */
		/*                                                                         */
		/* post     : The second variable in function call is filled with the      */
		/*            signed integer read                                          */
		/*                                                                         */
		/* uses     : stdio.h, stdlib.h                                            */
		/*                                                                         */
		/***************************************************************************/

		/*int FIO_BitGetIntSigned(int Len) throws DSTException {
			if (Len > 0) {
				int x = (int) getbits(Len);

				if (x >= (1 << (Len - 1))) {
					x -= (1 << Len);
				}
				//System.out.printf("Int %d = %d%n", tmp[0], x);
				return x;
			} else if (Len == 0) {
				return 0;
			} else
				throw new DSTException("EOD", -1);
		}*/

		/***************************************************************************/
		/*                                                                         */
		/* name     : FIO_BitGetShortSigned                                        */
		/*                                                                         */
		/* function : Read a short integer as a signed number from file with a     */
		/*            given number of bits.                                        */
		/*                                                                         */
		/* pre      : Len, x, output file must be open by having used getbits_init */
		/*                                                                         */
		/* post     : The second variable in function call is filled with the      */
		/*            signed short integer read                                    */
		/*                                                                         */
		/* uses     : stdio.h, stdlib.h                                            */
		/*                                                                         */
		/***************************************************************************/

		short FIO_BitGetShortSigned(int Len) throws DSTException {
			if (Len > 0) {
				short x = (short) getbits(Len);

				if (x >= (1 << (Len - 1))) {
					x -= (1 << Len);
				}
				//System.out.printf("Short %x = %d / %d%n", tmp[0], x, Len);
				return x;
			} else if (Len == 0) {
				return 0;
			} else
				throw new DSTException("EOD", -1);
		}

		/***************************************************************************/
		/*                                                                         */
		/* name     : getbits                                                      */
		/*                                                                         */
		/* function : Read bits from the bitstream and decrement the counter.      */
		/*                                                                         */
		/* pre      : out_bitptr                                                   */
		/*                                                                         */
		/* post     : m_ByteCounter, outword, returns EOF on EOF or 0 otherwise.   */
		/*                                                                         */
		/* uses     : stdio.h                                                      */
		/*                                                                         */
		/***************************************************************************/

		static int masks[] = { 0, 1, 3, 7, 0xf, 0x1f, 0x3f, 0x7f, 0xff };

		long getbits(int out_bitptr) throws DSTException {
			long outword;
			if (out_bitptr == 1) {
				if (BitPosition == 0) {
					DataByte = pDSTdata[ByteCounter++];
					//System.out.printf("0x%x ", DataByte);
					if (ByteCounter > TotalBytes) {
						throw new DSTException("EOF", -1); /* EOF */
					}
					BitPosition = 8;
				}
				BitPosition--;
				//System.out.printf("Byte:0x%x, res 0x%x, for %d%n", DataByte, outword[0], out_bitptr);				
				return (DataByte >> BitPosition) & 1;
			}

			outword = 0;
			while (out_bitptr > 0) {
				int thisbits, mask, shift;

				if (BitPosition == 0) {
					DataByte = pDSTdata[ByteCounter++];
					//System.out.printf("0x%x ", DataByte);
					if (ByteCounter > TotalBytes) {
						throw new DSTException("EOF", -1); /* EOF *//* EOF */
					}
					BitPosition = 8;
				}

				if (BitPosition < out_bitptr)
					thisbits = BitPosition;
				else
					thisbits = out_bitptr;
				//thisbits = Math.min(BitPosition, out_bitptr);
				shift = (BitPosition - thisbits);
				mask = masks[thisbits] << shift;

				shift = (out_bitptr - thisbits) - shift;
				if (shift <= 0)
					outword |= ((DataByte & mask) >> -shift);
				else
					outword |= ((DataByte & mask) << shift);
				out_bitptr -= thisbits;
				BitPosition -= thisbits;
			}
			//System.out.printf("Byte:0x%x, res 0x%x, for %d%n", DataByte, outword[0], out_bitptr);
			return outword;
		}

		/***************************************************************************/
		/*                                                                         */
		/* name     : get_bitcount                                                 */
		/*                                                                         */
		/* function : Reset the bits-written counter.                              */
		/*                                                                         */
		/* pre      : None                                                         */
		/*                                                                         */
		/* post     : Returns the number of bits written after an init_bitcount.   */
		/*                                                                         */
		/* uses     : -                                                            */
		/*                                                                         */
		/***************************************************************************/

		long get_in_bitcount() {
			return (long) ByteCounter * 8 - BitPosition;
		}

	}

	static final  class DSTXBITSData {
		int PBit;
		byte Bit;
	}

	static final class FirPtrData {
		int[] Pnt;
		int[][] Status;
	}

	static final  class ACData {
		int Init = 1;
		int C;
		int A;
		int cbptr;

		/*============================================================================*/
		/*       CONSTANTS                                                            */
		/*============================================================================*/

		static final int PBITS = AC_BITS; /* number of bits for Probabilities             */
		static final int NBITS = 4; /* number of overhead bits: must be at least 2! */
		/* maximum "variable shift length" is (NBITS-1) */
		static final int PSUM = (1 << (PBITS));
		static final int ABITS = (PBITS + NBITS); /* must be at least PBITS+2     */
		static final int MB = 0; /* if (MB) print max buffer use */
		static final int ONE = (1 << ABITS);
		static final int HALF = (1 << (ABITS - 1));

		/***************************************************************************/
		/*                                                                         */
		/* name     : DST_ACDecodeBit                                              */
		/*                                                                         */
		/* function : Arithmetic decode one bit.                                   */
		/*                                                                         */
		/* pre      : p       : probability for next bit being a "one"             */
		/*            cb[]    : filled with arithmetic code bit(s)                 */
		/*            fs      : Current length of the arithm. code                 */
		/*            Flush   : 0 = Normal operation,                              */
		/*                      1 = flush remaider of the decoder                  */
		/*                                                                         */
		/* post     : *b      : output bit of arithmetic decoder                   */
		/*            *MonC   : status of C-register (optionally)                  */
		/*            *MonA   : status of A-register (optionally)                  */
		/*                                                                         */
		/***************************************************************************/

		byte DST_ACDecodeBit(int p, byte[] cb, int fs, int Flush) {
			int ap;
			int h;
			byte b;

			if (Init == 1) {
				Init = 0;
				A = ONE - 1;
				C = 0;
				for (cbptr = 1; cbptr <= ABITS; cbptr++) {
					C <<= 1;
					if (cbptr < fs) {
						C |= cb[cbptr];
					}
				}
			}

			if (Flush == 0) {
				/* approximate (A * p) with "partial rounding". */
				ap = ((A >> PBITS) | ((A >> (PBITS - 1)) & 1)) * p;

				h = A - ap;
				if (C >= h) {
					b = 1;
					C -= h;
					A = ap;
				} else {
					b = 0;
					A = h;
				}
				while (A < HALF) {
					A <<= 1;

					/* Use new flushing technique; insert zero in LSB of C if reading past
					   the end of the arithmetic code */
					C <<= 1;
					if (cbptr < fs) {
						C |= cb[cbptr];
					}
					cbptr++;
				}
			} else {
				Init = 1;
				b = 0;
				if (cbptr < fs - 7) {
					b = 1;
				} else {
					while ((b == 0) && (cbptr < fs)) {
						if (cb[cbptr] != 0) {
							b = 1;
						}
						cbptr++;
					}
				}
			}
			return b;
		}

		/***************************************************************************/
		/*                                                                         */
		/* name     : DST_ACGetPtableIndex                                         */
		/*                                                                         */
		/* function : Determine the Ptable index belonging to the current value    */
		/*            of PredicVal.                                                */
		/*                                                                         */
		/* pre      : PredicVal and PtableLen                                      */
		/*                                                                         */
		/* post     : Returns the index of the Ptable belonging to the PredicVal.  */
		/*                                                                         */
		/***************************************************************************/

		int DST_ACGetPtableIndex(long PredicVal, int PtableLen) {
			int j;
			if (PredicVal < 0)
				PredicVal = -PredicVal;
			j = (int) (/*labs*/(PredicVal) >> AC_QSTEP);
			if (j >= PtableLen) {
				j = PtableLen - 1;
			}
			return j;
		}
		
		/////////////////// LT methods  //////////////////////////////
		void LT_ACDecodeBit_Init(byte[] cb, int fs) {
			A = ONE - 1;
			C = 0;
			for (cbptr = 1; cbptr <= ABITS; cbptr++) {
				C <<= 1;
				if (cbptr < fs) {
					C |= cb[cbptr]; // cb is only 1 or 0
				}
			}
		}

		int LT_ACDecodeBit_Decode(int p, byte[] cb, int fs) {
			int ap;
			int h;
			int b;

			/* approximate (A * p) with "partial rounding". */
			ap = ((A >> PBITS) | ((A >> (PBITS - 1)) & 1)) * p;

			h = A - ap;
			if (C >= h) {
				b = 0;
				C -= h;
				A = ap;
			} else {
				b = 1;
				A = h;
			}
			while (A < HALF) {
				A <<= 1;
				/* Use new flushing technique; insert zero in LSB of C if reading past
				    the end of the arithmetic code */
				C <<= 1;
				if (cbptr < fs) {
					C |= cb[cbptr]; // cb is only 1 or 0
				}
				//System.out.println(cbptr+" is "+cb[cbptr]);
				cbptr++;
				
			}
			return b;
		}

		int LT_ACDecodeBit_Flush(byte[] cb, int fs) {
			return cbptr < fs - 7?0:1; 
		}
	}

	FrameHeader FrameHdr; /* Contains frame based header information     */
	CodedTable StrFilter; /* Contains FIR-coef. compression data         */
	CodedTable StrPtable; /* Contains Ptable-entry compression data      */
	/* input stream.                               */
	int[][] P_one; /* Probability table for arithmetic coder      */
	byte[] AData; /* Contains the arithmetic coded bit stream    */
	/* of a complete frame                         */
	int ADataLen; /* Number of code bits contained in AData[]    */
	StrData S; /* DST data stream */
	DSTXBITSData DstXbits;
	FirPtrData FirPtrs;
	short[][] BitStream11; /* Contains the bitstream of a complete        */
	//byte[][] BitStream11;
	byte BitMask[] = new byte[RESOL];
	ACData AC;
	// precalculated coeff for optimization
	short[][][] LT_ICoefI = new short[2 * MAX_CHANNELS][16][256];
	int[][] LT_Status = new int[MAX_CHANNELS][16];

	/***************************************************************************/
	/*                                                                         */
	/* name     : FillTable4Bit                                                */
	/*                                                                         */
	/* function : Fill an array that indicates for each bit of each channel    */
	/*            which table number must be used.                             */
	/*                                                                         */
	/* pre      : NrOfChannels, NrOfBitsPerCh, S->NrOfSegments[],              */
	/*            S->SegmentLen[][], S->Resolution, S->Table4Segment[][]       */
	/*                                                                         */
	/* post     : Table4Bit[MAX_CHANNELS][MAX_DSDBITS_INFRAME]                                                */
	/*                                                                         */
	/***************************************************************************/

	static void FillTable4Bit(int NrOfChannels, int NrOfBitsPerCh, Segment S, byte Table4Bit[][]) {
		int BitNr;
		int ChNr;
		int SegNr;
		int Start;
		int End;
		byte Val;

		for (ChNr = 0; ChNr < NrOfChannels; ChNr++) {
			byte[] Table4BitCh = Table4Bit[ChNr];
			for (SegNr = 0, Start = 0; SegNr < S.NrOfSegments[ChNr] - 1; SegNr++) {
				Val = (byte) S.Table4Segment[ChNr][SegNr];
				End = Start + S.Resolution * 8 * S.SegmentLen[ChNr][SegNr];
				for (BitNr = Start; BitNr < End; BitNr++) {
					/*Table4Bit[ChNr]*/Table4BitCh[BitNr] = Val;
				}
				Start += S.Resolution * 8 * S.SegmentLen[ChNr][SegNr];
			}
			Val = (byte) (S.Table4Segment[ChNr][SegNr] &255);
			Arrays.fill(Table4BitCh, Start, Table4BitCh.length, Val); // !! NrOfBitsPerCh
		}
	}

	/***************************************************************************/
	/*                                                                         */
	/* name     : Reverse7LSBs                                                 */
	/*                                                                         */
	/* function : Take the 7 LSBs of a number consisting of SIZE_PREDCOEF bits */
	/*            (2's complement), reverse the bit order and add 1 to it.     */
	/*                                                                         */
	/* pre      : c                                                            */
	/*                                                                         */
	/* post     : Returns the translated number                                */
	/*                                                                         */
	/***************************************************************************/
	static short reverse[] = { // size = 128
	1, 65, 33, 97, 17, 81, 49, 113, 9, 73, 41, 105, 25, 89, 57, 121, 5, 69, 37, 101, 21, 85, 53, 117, 13, 77, 45, 109,
			29, 93, 61, 125, 3, 67, 35, 99, 19, 83, 51, 115, 11, 75, 43, 107, 27, 91, 59, 123, 7, 71, 39, 103, 23, 87,
			55, 119, 15, 79, 47, 111, 31, 95, 63, 127, 2, 66, 34, 98, 18, 82, 50, 114, 10, 74, 42, 106, 26, 90, 58,
			122, 6, 70, 38, 102, 22, 86, 54, 118, 14, 78, 46, 110, 30, 94, 62, 126, 4, 68, 36, 100, 20, 84, 52, 116,
			12, 76, 44, 108, 28, 92, 60, 124, 8, 72, 40, 104, 24, 88, 56, 120, 16, 80, 48, 112, 32, 96, 64, 128 };

	/***************************************************************************/
	/*                                                                         */
	/* name     : Reverse7LSBs                                                 */
	/*                                                                         */
	/* function : Take the 7 LSBs of a number consisting of SIZE_PREDCOEF bits */
	/*            (2's complement), reverse the bit order and add 1 to it.     */
	/*                                                                         */
	/* pre      : c                                                            */
	/*                                                                         */
	/* post     : Returns the translated number                                */
	/*                                                                         */
	/***************************************************************************/
	static short Reverse7LSBs(short c) {
		return reverse[(c + (1 << SIZE_PREDCOEF)) & 127];
	}

	/***************************************************************************/
	/*                                                                         */
	/* name     : ReadDSDframe                                                 */
	/*                                                                         */
	/* function : Read DSD signal of this frame from the DST input file.       */
	/*                                                                         */
	/* pre      : a file must be opened by using getbits_init(),               */
	/*            MaxFrameLen, NrOfChannels                                    */
	/*                                                                         */
	/* post     : BS11[][]                                                     */
	/*                                                                         */
	/* uses     : fio_bit.h                                                    */
	/*                                                                         */
	/***************************************************************************/
	void ReadDSDframe(StrData S, long MaxFrameLen, int NrOfChannels, byte[] DSDFrame) throws DSTException {
		int ByteNr;
		int max = (int) (MaxFrameLen * NrOfChannels);
		for (ByteNr = 0; ByteNr < max; ByteNr++) {
			DSDFrame[ByteNr] = S.FIO_BitGetChrUnsigned(8);
		}
	}

	int Log2RoundUp(long x) {
		int y = 0;

		while (x >= (1 << y)) {
			y++;
		}

		return y;
	}

	/***************************************************************************/
	/*                                                                         */
	/* name     : ReadTableSegmentData                                         */
	/*                                                                         */
	/* function : Read segmentation data for filters or Ptables.               */
	/*                                                                         */
	/* pre      : NrOfChannels, FrameLen, MaxNrOfSegs, MinSegLen               */
	/*                                                                         */
	/* post     : S->Resolution, S->SegmentLen[][], S->NrOfSegments[]          */
	/*                                                                         */
	/* uses     : types.h, fio_bit.h, stdio.h, stdlib.h                        */
	/*                                                                         */
	/* @throws DSTException 
	*************************************************************************/

	void ReadTableSegmentData(StrData SD, int NrOfChannels, int FrameLen, int MaxNrOfSegs, int MinSegLen,
			boolean filters, FrameHeader FH) throws DSTException {
		int ChNr = 0;
		int DefinedBits = 0;
		int ResolRead = 0;
		int SegNr = 0;
		int MaxSegSize;
		int NrOfBits;
		int EndOfChannel;

		MaxSegSize = FrameLen - MinSegLen / 8;

		Segment Seg = filters ? FH.FSeg : FH.PSeg;

		int SameSegAllCh = SD.FIO_BitGetIntUnsigned(1);

		if (SameSegAllCh == 1) {
			EndOfChannel = SD.FIO_BitGetIntUnsigned(1);

			while (EndOfChannel == 0) {
				if (SegNr >= MaxNrOfSegs) {
					throw new DSTException("Too many segments for this channel!", -1);
				}
				if (ResolRead == 0) {
					NrOfBits = Log2RoundUp(FrameLen - MinSegLen / 8); // Log2RoundUp
					Seg.Resolution = SD.FIO_BitGetIntUnsigned(NrOfBits);
					if ((Seg.Resolution == 0) || (Seg.Resolution > FrameLen - MinSegLen / 8)) {
						throw new DSTException("Invalid segment resolution!", -1);
					}
					ResolRead = 1;
				}
				NrOfBits = Log2RoundUp(MaxSegSize / Seg.Resolution);
				Seg.SegmentLen[0][SegNr] = SD.FIO_BitGetIntUnsigned(NrOfBits);

				if ((Seg.Resolution * 8 * Seg.SegmentLen[0][SegNr] < MinSegLen)
						|| (Seg.Resolution * 8 * Seg.SegmentLen[0][SegNr] > FrameLen * 8 - DefinedBits - MinSegLen)) {
					throw new DSTException("Invalid segment length!", -1);

				}
				DefinedBits += Seg.Resolution * 8 * Seg.SegmentLen[0][SegNr];
				MaxSegSize -= Seg.Resolution * Seg.SegmentLen[0][SegNr];
				SegNr++;
				EndOfChannel = SD.FIO_BitGetIntUnsigned(1);
			}
			Seg.NrOfSegments[0] = SegNr + 1;
			Seg.SegmentLen[0][SegNr] = 0;

			for (ChNr = 1; ChNr < NrOfChannels; ChNr++) {
				Seg.NrOfSegments[ChNr] = Seg.NrOfSegments[0];
				for (SegNr = 0; SegNr < Seg.NrOfSegments[0]; SegNr++) {
					Seg.SegmentLen[ChNr][SegNr] = Seg.SegmentLen[0][SegNr];
				}
			}
		} else {
			while (ChNr < NrOfChannels) {
				if (SegNr >= MaxNrOfSegs) {
					throw new DSTException("Too many segments for this channel!", -1);
				}
				EndOfChannel = SD.FIO_BitGetIntUnsigned(1);
				if (EndOfChannel == 0) {
					if (ResolRead == 0) {
						NrOfBits = Log2RoundUp(FrameLen - MinSegLen / 8);
						Seg.Resolution = SD.FIO_BitGetIntUnsigned(NrOfBits);
						if ((Seg.Resolution == 0) || (Seg.Resolution > FrameLen - MinSegLen / 8)) {
							throw new DSTException("Invalid segment resolution!", -1);
						}
						ResolRead = 1;
					}
					NrOfBits = Log2RoundUp(MaxSegSize / Seg.Resolution);
					Seg.SegmentLen[ChNr][SegNr] = SD.FIO_BitGetIntUnsigned(NrOfBits);

					if ((Seg.Resolution * 8 * Seg.SegmentLen[ChNr][SegNr] < MinSegLen)
							|| (Seg.Resolution * 8 * Seg.SegmentLen[ChNr][SegNr] > FrameLen * 8 - DefinedBits
									- MinSegLen)) {
						throw new DSTException("Invalid segment length!", -1);
					}
					DefinedBits += Seg.Resolution * 8 * Seg.SegmentLen[ChNr][SegNr];
					MaxSegSize -= Seg.Resolution * Seg.SegmentLen[ChNr][SegNr];
					SegNr++;
				} else {
					Seg.NrOfSegments[ChNr] = SegNr + 1;
					Seg.SegmentLen[ChNr][SegNr] = 0;
					SegNr = 0;
					DefinedBits = 0;
					MaxSegSize = FrameLen - MinSegLen / 8;
					ChNr++;
				}
			}
		}
		if (ResolRead == 0) {
			Seg.Resolution = 1;
		}
		if (filters)
			FH.FSameSegAllCh = SameSegAllCh;
		else
			FH.PSameSegAllCh = SameSegAllCh;
	}

	/***************************************************************************/
	/*                                                                         */
	/* name     : CopySegmentData                                              */
	/*                                                                         */
	/* function : Read segmentation data for filters and Ptables.              */
	/*                                                                         */
	/* pre      : FH->NrOfChannels, FH->FSeg.Resolution,                       */
	/*            FH->FSeg.NrOfSegments[], FH->FSeg.SegmentLen[][]             */
	/*                                                                         */
	/* post     : FH-> : PSeg : .Resolution, .NrOfSegments[], .SegmentLen[][], */
	/*                   PSameSegAllCh                                         */
	/*                                                                         */
	/* uses     : types.h, conststr.h                                          */
	/*                                                                         */
	/**
	 * @throws DSTException
	 *************************************************************************/

	void CopySegmentData(FrameHeader FH) throws DSTException {
		int ChNr;
		int SegNr;

		int dst[] = FH.PSeg.NrOfSegments, src[] = FH.FSeg.NrOfSegments;

		FH.PSeg.Resolution = FH.FSeg.Resolution;
		FH.PSameSegAllCh = 1;
		for (ChNr = 0; ChNr < FH.NrOfChannels; ChNr++) {
			dst[ChNr] = src[ChNr];
			if (dst[ChNr] > MAXNROF_PSEGS) {
				throw new DSTException("Too many segments!", -1);
			}
			if (dst[ChNr] != dst[0]) {
				FH.PSameSegAllCh = 0;
			}
			for (SegNr = 0; SegNr < dst[ChNr]; SegNr++) {
				int lendst[] = FH.PSeg.SegmentLen[ChNr], lensrc[] = FH.FSeg.SegmentLen[ChNr];

				lendst[SegNr] = lensrc[SegNr];
				if ((lendst[SegNr] != 0) && (FH.PSeg.Resolution * 8 * lendst[SegNr] < MIN_PSEG_LEN)) {
					throw new DSTException("ERROR: Invalid segment length!", -1);
				}
				if (lendst[SegNr] != FH.PSeg.SegmentLen[0][SegNr]) {
					FH.PSameSegAllCh = 0;
				}
			}
		}
	}

	/***************************************************************************/
	/*                                                                         */
	/* name     : ReadSegmentData                                              */
	/*                                                                         */
	/* function : Read segmentation data for filters and Ptables.              */
	/*                                                                         */
	/* pre      : FH->NrOfChannels, CO->MaxFrameLen                            */
	/*                                                                         */
	/* post     : FH-> : FSeg : .Resolution, .SegmentLen[][], .NrOfSegments[], */
	/*                   PSeg : .Resolution, .SegmentLen[][], .NrOfSegments[], */
	/*                   PSameSegAsF, FSameSegAllCh, PSameSegAllCh             */
	/*                                                                         */
	/* uses     : types.h, conststr.h, fio_bit.h                               */
	/*                                                                         */
	/***************************************************************************/

	void ReadSegmentData(StrData SD, FrameHeader FH) throws DSTException {

		FH.PSameSegAsF = SD.FIO_BitGetIntUnsigned(1);
		ReadTableSegmentData(SD, FH.NrOfChannels, FH.MaxFrameLen, MAXNROF_FSEGS, MIN_FSEG_LEN, true, FH); // ->
		if (FH.PSameSegAsF == 1) {
			CopySegmentData(FH);
		} else {
			ReadTableSegmentData(SD, FH.NrOfChannels, FH.MaxFrameLen, MAXNROF_PSEGS, MIN_PSEG_LEN, false, FH); // ->
		}

	}

	/***************************************************************************/
	/*                                                                         */
	/* name     : ReadTableMappingData                                         */
	/*                                                                         */
	/* function : Read mapping data for filters or Ptables.                    */
	/*                                                                         */
	/* pre      : NrOfChannels, MaxNrOfTables, S->NrOfSegments[]               */
	/*                                                                         */
	/* post     : S->Table4Segment[][], NrOfTables, SameMapAllCh               */
	/*                                                                         */
	/* uses     : types.h, fio_bit.h, stdio.h, stdlib.h                        */
	/*                                                                         */
	/**
	 * @throws DSTException
	 *************************************************************************/

	void ReadTableMappingData(StrData SD, int NrOfChannels, int MaxNrOfTables, boolean filters, FrameHeader FH)
			throws DSTException {
		int ChNr;
		int CountTables = 1;
		int NrOfBits = 1;
		int SegNr;
		Segment Seg = filters ? FH.FSeg : FH.PSeg;

		Seg.Table4Segment[0][0] = 0;

		int SameMapAllCh = SD.FIO_BitGetIntUnsigned(1);

		if (SameMapAllCh == 1) {
			for (SegNr = 1; SegNr < Seg.NrOfSegments[0]; SegNr++) {
				NrOfBits = Log2RoundUp(CountTables);

				Seg.Table4Segment[0][SegNr] = SD.FIO_BitGetIntUnsigned(NrOfBits);

				if (Seg.Table4Segment[0][SegNr] == CountTables) {
					CountTables++;
				} else if (Seg.Table4Segment[0][SegNr] > CountTables) {
					throw new DSTException("Invalid table number for segment!", -1);
				}
			}
			for (ChNr = 1; ChNr < NrOfChannels; ChNr++) {
				if (Seg.NrOfSegments[ChNr] != Seg.NrOfSegments[0]) {
					throw new DSTException("Mapping can't be the same for all channels!", -1);
				}
				for (SegNr = 0; SegNr < Seg.NrOfSegments[0]; SegNr++) {
					Seg.Table4Segment[ChNr][SegNr] = Seg.Table4Segment[0][SegNr];
				}
			}
		} else {
			for (ChNr = 0; ChNr < NrOfChannels; ChNr++) {
				for (SegNr = 0; SegNr < Seg.NrOfSegments[ChNr]; SegNr++) {
					if ((ChNr != 0) || (SegNr != 0)) {
						NrOfBits = Log2RoundUp(CountTables);
						Seg.Table4Segment[ChNr][SegNr] = SD.FIO_BitGetIntUnsigned(NrOfBits);

						if (Seg.Table4Segment[ChNr][SegNr] == CountTables) {
							CountTables++;
						} else if (Seg.Table4Segment[ChNr][SegNr] > CountTables) {
							throw new DSTException("ERROR: Invalid table number for segment!", -1);
						}
					}
				}
			}
		}
		if (CountTables > MaxNrOfTables) {
			throw new DSTException("Too many tables for this frame!", -1);
		}
		if (filters) {
			FH.NrOfFilters = CountTables;
			FH.FSameMapAllCh = SameMapAllCh;
		} else {
			FH.NrOfPtables = CountTables;
			FH.PSameMapAllCh = SameMapAllCh;
		}
	}

	/***************************************************************************/
	/*                                                                         */
	/* name     : CopyMappingData                                              */
	/*                                                                         */
	/* function : Copy mapping data for Ptables from the filter mapping.       */
	/*                                                                         */
	/* pre      : CO-> : NrOfChannels, MaxNrOfPtables                          */
	/*            FH-> : FSeg.NrOfSegments[], FSeg.Table4Segment[][],          */
	/*                   NrOfFilters, PSeg.NrOfSegments[]                      */
	/*                                                                         */
	/* post     : FH-> : PSeg.Table4Segment[][], NrOfPtables, PSameMapAllCh    */
	/*                                                                         */
	/* uses     : types.h, stdio.h, stdlib.h, conststr.h                       */
	/*                                                                         */
	/**
	 * @throws DSTException
	 *************************************************************************/

	void CopyMappingData(FrameHeader FH) throws DSTException {
		int ChNr;
		int SegNr;

		FH.PSameMapAllCh = 1;
		for (ChNr = 0; ChNr < FH.NrOfChannels; ChNr++) {
			if (FH.PSeg.NrOfSegments[ChNr] == FH.FSeg.NrOfSegments[ChNr]) {
				for (SegNr = 0; SegNr < FH.FSeg.NrOfSegments[ChNr]; SegNr++) {
					FH.PSeg.Table4Segment[ChNr][SegNr] = FH.FSeg.Table4Segment[ChNr][SegNr];
					if (FH.PSeg.Table4Segment[ChNr][SegNr] != FH.PSeg.Table4Segment[0][SegNr]) {
						FH.PSameMapAllCh = 0;
					}
				}
			} else {
				throw new DSTException("Not same number of segments for filters and Ptables!", -1);
			}
		}
		FH.NrOfPtables = FH.NrOfFilters;
		if (FH.NrOfPtables > FH.MaxNrOfPtables) {
			throw new DSTException("Too many tables for this frame!", -1);
		}
	}

	/***************************************************************************/
	/*                                                                         */
	/* name     : ReadMappingData                                              */
	/*                                                                         */
	/* function : Read mapping data (which channel uses which filter/Ptable).  */
	/*                                                                         */
	/* pre      : CO-> : NrOfChannels, MaxNrOfFilters, MaxNrOfPtables          */
	/*            FH-> : FSeg.NrOfSegments[], PSeg.NrOfSegments[]              */
	/*                                                                         */
	/* post     : FH-> : FSeg.Table4Segment[][], .NrOfFilters,                 */
	/*                   PSeg.Table4Segment[][], .NrOfPtables,                 */
	/*                   PSameMapAsF, FSameMapAllCh, PSameMapAllCh, HalfProb[] */
	/*                                                                         */
	/* uses     : types.h, conststr.h, fio_bit.h                               */
	/*                                                                         */
	/**
	 * @throws DSTException
	 *************************************************************************/

	void ReadMappingData(StrData SD, FrameHeader FH) throws DSTException {
		int j;

		FH.PSameMapAsF = SD.FIO_BitGetIntUnsigned(1);

		ReadTableMappingData(SD, FH.NrOfChannels, FH.MaxNrOfFilters, true, FH);
		if (FH.PSameMapAsF == 1) {
			CopyMappingData(FH);
		} else {
			ReadTableMappingData(SD, FH.NrOfChannels, FH.MaxNrOfPtables, false, FH);
		}

		for (j = 0; j < FH.NrOfChannels; j++) {
			FH.HalfProb[j] = SD.FIO_BitGetIntUnsigned(1);
		}
	}

	/***************************************************************************/
	/*                                                                         */
	/* name     : RiceDecode                                                   */
	/*                                                                         */
	/* function : Read a Rice code from the DST file                           */
	/*                                                                         */
	/* pre      : a file must be opened by using putbits_init(), m             */
	/*                                                                         */
	/* post     : Returns the Rice decoded number                              */
	/*                                                                         */
	/* uses     : fio_bit.h                                                    */
	/*                                                                         */
	/**
	 * @throws DSTException
	 *************************************************************************/

	int RiceDecode(StrData S, int m) throws DSTException {
		int LSBs;
		int Nr;
		int RLBit;
		int RunLength;
		int Sign;

		/* Retrieve run length code */
		RunLength = 0;
		do {
			RLBit = S.FIO_BitGetIntUnsigned(1);
			RunLength += (1 - RLBit);
		} while (RLBit == 0);

		/* Retrieve least significant bits */
		LSBs = S.FIO_BitGetIntUnsigned(m);

		Nr = (RunLength << m) + LSBs;

		/* Retrieve optional sign bit */
		if (Nr != 0) {
			Sign = S.FIO_BitGetIntUnsigned(1);

			if (Sign == 1) {
				Nr = -Nr;
			}
		}

		return Nr;
	}

	/***************************************************************************/
	/*                                                                         */
	/* name     : ReadFilterCoefSets                                           */
	/*                                                                         */
	/* function : Read all filter data from the DST file, which contains:      */
	/*            - which channel uses which filter                            */
	/*            - for each filter:                                           */
	/*              ~ prediction order                                         */
	/*              ~ all coefficients                                         */
	/*                                                                         */
	/* pre      : a file must be opened by using getbits_init(), NrOfChannels  */
	/*            FH->NrOfFilters, CF->CPredOrder[], CF->CPredCoef[][],        */
	/*            FH->FSeg.Table4Segment[][0]                                  */
	/*                                                                         */
	/* post     : FH->PredOrder[], FH->ICoefA[][], FH->NrOfHalfBits[],         */
	/*            CF->Coded[], CF->BestMethod[], CF->m[][],                    */
	/*                                                                         */
	/* uses     : types.h, fio_bit.h, conststr.h, stdio.h, stdlib.h, dst_ac.h  */
	/*                                                                         */
	/**
	 * @throws DSTException
	 *************************************************************************/

	void ReadFilterCoefSets(StrData SD, int NrOfChannels, FrameHeader FH, CodedTable CF) throws DSTException {
		int c;
		int ChNr;
		int CoefNr;
		int FilterNr;
		int TapNr;
		int x;

		/* Read the filter parameters */
		for (FilterNr = 0; FilterNr < FH.NrOfFilters; FilterNr++) {
			FH.PredOrder[FilterNr] = SD.FIO_BitGetIntUnsigned(SIZE_CODEDPREDORDER);
			FH.PredOrder[FilterNr]++;
			CF.Coded[FilterNr] = SD.FIO_BitGetIntUnsigned(1);
			if (CF.Coded[FilterNr] == 0) {
				CF.BestMethod[FilterNr] = -1;
				for (CoefNr = 0; CoefNr < FH.PredOrder[FilterNr]; CoefNr++) {

					FH.ICoefA[FilterNr][CoefNr] = SD.FIO_BitGetShortSigned(SIZE_PREDCOEF);
					//System.out.printf("ICoefA[%d][%d] = %d%n", FilterNr, CoefNr, FH.ICoefA[FilterNr][CoefNr]);
				}
			} else {
				int bestmethod;

				CF.BestMethod[FilterNr] = SD.FIO_BitGetIntUnsigned(SIZE_RICEMETHOD);
				bestmethod = CF.BestMethod[FilterNr];
				if (CF.CPredOrder[bestmethod] >= FH.PredOrder[FilterNr]) {
					throw new DSTException("Invalid coefficient coding method!", -1);
				}

				for (CoefNr = 0; CoefNr < CF.CPredOrder[bestmethod]; CoefNr++) {
					FH.ICoefA[FilterNr][CoefNr] = SD.FIO_BitGetShortSigned(SIZE_PREDCOEF);
					//System.out.printf("ICoefA[%d][%d] = %d%n", FilterNr, CoefNr, FH.ICoefA[FilterNr][CoefNr]);
				}

				CF.m[FilterNr][bestmethod] = SD.FIO_BitGetIntUnsigned(SIZE_RICEM);

				for (CoefNr = CF.CPredOrder[bestmethod]; CoefNr < FH.PredOrder[FilterNr]; CoefNr++) {
					for (TapNr = 0, x = 0; TapNr < CF.CPredOrder[bestmethod]; TapNr++) {
						x += CF.CPredCoef[bestmethod][TapNr] * FH.ICoefA[FilterNr][CoefNr - TapNr - 1];
					}

					if (x >= 0) {
						c = RiceDecode(SD, CF.m[FilterNr][bestmethod]) - (x + 4) / 8;
					} else {
						c = RiceDecode(SD, CF.m[FilterNr][bestmethod]) + (-x + 3) / 8;
					}

					if ((c < -(1 << (SIZE_PREDCOEF - 1))) || (c >= (1 << (SIZE_PREDCOEF - 1)))) {
						throw new DSTException("filter coefficient out of range!", -1);
					} else {
						FH.ICoefA[FilterNr][CoefNr] = (short) c;
						//System.out.printf("ICoefA[%d][%d] = %d%n", FilterNr, CoefNr, FH.ICoefA[FilterNr][CoefNr]);
					}
				}
			}

			/* Clear out remaining coeffs, as the SSE2 code uses them all. */
			if (CoefNr < FH.ICoefA[FilterNr].length)
				Arrays.fill(FH.ICoefA[FilterNr], CoefNr, FH.ICoefA[FilterNr].length, (short) 0); // should do it for remaining in < FilterNr
		}

		for (ChNr = 0; ChNr < NrOfChannels; ChNr++) {
			FH.NrOfHalfBits[ChNr] = FH.PredOrder[FH.FSeg.Table4Segment[ChNr][0]];
		}
	}

	/***************************************************************************/
	/*                                                                         */
	/* name     : ReadProbabilityTables                                        */
	/*                                                                         */
	/* function : Read all Ptable data from the DST file, which contains:      */
	/*            - which channel uses which Ptable                            */
	/*            - for each Ptable all entries                                */
	/*                                                                         */
	/* pre      : a file must be opened by using getbits_init(),               */
	/*            FH->NrOfPtables, CP->CPredOrder[], CP->CPredCoef[][]         */
	/*                                                                         */
	/* post     : FH->PtableLen[], CP->Coded[], CP->BestMethod[], CP->m[][],   */
	/*            P_one[][]                                                    */
	/*                                                                         */
	/* uses     : types.h, fio_bit.h, conststr.h, stdio.h, stdlib.h            */
	/*                                                                         */
	/**
	 * @throws DSTException
	 *************************************************************************/

	void ReadProbabilityTables(StrData SD, FrameHeader FH, CodedTable CP, int[][] P_one) throws DSTException {
		int c;
		int EntryNr;
		int PtableNr;
		int TapNr;
		int x;
		/* Read the data of all probability tables (table entries) */
		for (PtableNr = 0; PtableNr < FH.NrOfPtables; PtableNr++) {
			FH.PtableLen[PtableNr] = SD.FIO_BitGetIntUnsigned(AC_HISBITS);
			FH.PtableLen[PtableNr]++;
			if (FH.PtableLen[PtableNr] > 1) {
				CP.Coded[PtableNr] = SD.FIO_BitGetIntUnsigned(1);
				if (CP.Coded[PtableNr] == 0) {
					CP.BestMethod[PtableNr] = -1;
					for (EntryNr = 0; EntryNr < FH.PtableLen[PtableNr]; EntryNr++) {
						P_one[PtableNr][EntryNr] = SD.FIO_BitGetIntUnsigned(AC_BITS - 1);
						P_one[PtableNr][EntryNr]++;
					}
				} else {
					int bestmethod;
					CP.BestMethod[PtableNr] = SD.FIO_BitGetIntUnsigned(SIZE_RICEMETHOD);
					bestmethod = CP.BestMethod[PtableNr];
					if (CP.CPredOrder[bestmethod] >= FH.PtableLen[PtableNr]) {
						throw new DSTException("Invalid Ptable coding method!", -1);
					}

					for (EntryNr = 0; EntryNr < CP.CPredOrder[bestmethod]; EntryNr++) {
						P_one[PtableNr][EntryNr] = SD.FIO_BitGetIntUnsigned(AC_BITS - 1);
						P_one[PtableNr][EntryNr]++;
					}

					CP.m[PtableNr][bestmethod] = SD.FIO_BitGetIntUnsigned(SIZE_RICEM);

					for (EntryNr = CP.CPredOrder[bestmethod]; EntryNr < FH.PtableLen[PtableNr]; EntryNr++) {
						for (TapNr = 0, x = 0; TapNr < CP.CPredOrder[bestmethod]; TapNr++) {
							x += CP.CPredCoef[bestmethod][TapNr] * P_one[PtableNr][EntryNr - TapNr - 1];
						}

						if (x >= 0) {
							c = RiceDecode(SD, CP.m[PtableNr][bestmethod]) - (x + 4) / 8;
						} else {
							c = RiceDecode(SD, CP.m[PtableNr][bestmethod]) + (-x + 3) / 8;
						}
						//System.out.printf("best met %d %d %d%n", CP.m[PtableNr][bestmethod], x,
						//	RiceDecode(SD, CP.m[PtableNr][bestmethod]));
						if ((c < 1) || (c > (1 << (AC_BITS - 1)))) {
							throw new DSTException(String.format("Ptable entry (%d) out of range!", c), -1);
						} else {
							P_one[PtableNr][EntryNr] = c;
						}
					}
				}
			} else {
				P_one[PtableNr][0] = 128;
				CP.BestMethod[PtableNr] = -1;
			}
		}
	}

	/***************************************************************************/
	/*                                                                         */
	/* name     : ReadArithmeticCodeData                                       */
	/*                                                                         */
	/* function : Read arithmetic coded data from the DST file, which contains:*/
	/*            - length of the arithmetic code                              */
	/*            - all bits of the arithmetic code                            */
	/*                                                                         */
	/* pre      : a file must be opened by using getbits_init(), ADataLen      */
	/*                                                                         */
	/* post     : AData[]                                                      */
	/*                                                                         */
	/* uses     : fio_bit.h                                                    */
	/*                                                                         */
	/***************************************************************************/

	void ReadArithmeticCodedData(StrData SD, int ADataLen, byte[] AData) throws DSTException {
		if (ADataLen == 0)
			return;
		AData[0] = SD.FIO_BitGetChrUnsigned(1);
		if (AData[0] != 0) {
			throw new DSTException(String.format("Illegal arithmetic code in frame %d!", FrameHdr.FrameNr), -1);
		}
		for (int j = 1; j < ADataLen; j++) {
			AData[j] = SD.FIO_BitGetChrUnsigned(1);
		}
	}

	/* CCP = Coding of Coefficients and Ptables */
	/***************************************************************************/
	/*                                                                         */
	/* name     : CCP_CalcInit                                                 */
	/*                                                                         */
	/* function : Initialise the prediction order and coefficients for         */
	/*            prediction filter used to predict the filter coefficients.   */
	/*                                                                         */
	/* pre      : CT->TableType                                                */
	/*                                                                         */
	/* post     : CT->CPredOrder[], CT->CPredCoef[][]                          */
	/*                                                                         */
	/***************************************************************************/

	void CCP_CalcInit(CodedTable CT) throws DSTException {
		int i;

		switch (CT.TableType) {
		case FILTER:
			CT.CPredOrder[0] = 1;
			CT.CPredCoef[0][0] = -8;
			for (i = CT.CPredOrder[0]; i < MAXCPREDORDER; i++) {
				CT.CPredCoef[0][i] = 0;
			}

			CT.CPredOrder[1] = 2;
			CT.CPredCoef[1][0] = -16;
			CT.CPredCoef[1][1] = 8;
			for (i = CT.CPredOrder[1]; i < MAXCPREDORDER; i++) {
				CT.CPredCoef[1][i] = 0;
			}

			CT.CPredOrder[2] = 3;
			CT.CPredCoef[2][0] = -9;
			CT.CPredCoef[2][1] = -5;
			CT.CPredCoef[2][2] = 6;
			for (i = CT.CPredOrder[2]; i < MAXCPREDORDER; i++) {
				CT.CPredCoef[2][i] = 0;
			}
			break;
		case PTABLE:
			CT.CPredOrder[0] = 1;
			CT.CPredCoef[0][0] = -8;
			for (i = CT.CPredOrder[0]; i < MAXCPREDORDER; i++) {
				CT.CPredCoef[0][i] = 0;
			}

			CT.CPredOrder[1] = 2;
			CT.CPredCoef[1][0] = -16;
			CT.CPredCoef[1][1] = 8;
			for (i = CT.CPredOrder[1]; i < MAXCPREDORDER; i++) {
				CT.CPredCoef[1][i] = 0;
			}

			CT.CPredOrder[2] = 3;
			CT.CPredCoef[2][0] = -24;
			CT.CPredCoef[2][1] = 24;
			CT.CPredCoef[2][2] = -8;
			for (i = CT.CPredOrder[2]; i < MAXCPREDORDER; i++) {
				CT.CPredCoef[2][i] = 0;
			}
			break;
		default:
			throw new DSTException("Illegal table type", -1);
		}
	}

	void init(int NrChannels, int Fs44) throws DSTException {
		//System.out.printf("DST init %d at %d%n", NrChannels, Fs44);
		S = new StrData();
		FrameHdr = new FrameHeader();
		FrameHdr.NrOfChannels = NrChannels;
		FrameHdr.FrameNr = 0;
		FrameHdr.Fsample44 = Fs44;
		/*  64FS =>  4704 */
		/* 128FS =>  9408 */
		/* 256FS => 18816 */
		FrameHdr.MaxFrameLen = (588 * Fs44 / 8);

		FrameHdr.ByteStreamLen = FrameHdr.MaxFrameLen * FrameHdr.NrOfChannels;
		FrameHdr.BitStreamLen = FrameHdr.ByteStreamLen * RESOL;
		FrameHdr.NrOfBitsPerCh = FrameHdr.MaxFrameLen * RESOL;

		FrameHdr.MaxNrOfFilters = 2 * FrameHdr.NrOfChannels;
		FrameHdr.MaxNrOfPtables = 2 * FrameHdr.NrOfChannels;
		FrameHdr.ICoefA = new int[FrameHdr.MaxNrOfFilters][(1 << SIZE_CODEDPREDORDER)];
		StrFilter = new CodedTable(FrameHdr);
		StrPtable = new CodedTable(FrameHdr);
		StrFilter.TableType = FILTER;
		StrPtable.TableType = PTABLE;
		AData = new byte[(int) FrameHdr.BitStreamLen]; // TODO ALLOCATE WITH MAX POSSIB;E
		P_one = new int[FrameHdr.MaxNrOfPtables][AC_HISMAX];
		CCP_CalcInit(StrFilter);
		CCP_CalcInit(StrPtable);
		DstXbits = new DSTXBITSData();
		FirPtrs = new FirPtrData();
		FirPtrs.Pnt = new int[FrameHdr.NrOfChannels];
		FirPtrs.Status = new int[FrameHdr.NrOfChannels][(1 << SIZE_CODEDPREDORDER)];
		BitStream11 = new short[FrameHdr.NrOfChannels][(int) FrameHdr.NrOfBitsPerCh];
		//BitStream11 = new byte[FrameHdr.NrOfChannels][(int) FrameHdr.NrOfBitsPerCh/8];
		/* Fill BitMask array (1, 2, 4, 8, 16, 32, 64, 128) */
		for (int BitNr = 0; BitNr < 8; BitNr++) {
			BitMask[BitNr] = (byte) ((1 << BitNr) & 255);
		}
		AC = new ACData();
	}

	/***************************************************************************/
	/*                                                                         */
	/* name     : UnpackDSTframe                                               */
	/*                                                                         */
	/* function : Read a complete frame from the DST input file                */
	/*                                                                         */
	/* pre      : a file must be opened by using getbits_init()                */
	/*                                                                         */
	/* post     : Complete D-structure                                         */
	/*                                                                         */
	/* uses     : types.h, fio_bit.h, stdio.h, stdlib.h, constopt.h,           */
	/*                                                                         */
	/***************************************************************************/

	void UnpackDSTframe(byte[] DSTdataframe, byte[] DSDdataframe) throws DSTException {
		int Dummy;

		/* fill internal buffer with DSTframe */
		S.FillBuffer(DSTdataframe, FrameHdr.CalcNrOfBytes);

		/* interpret DST header byte */
		FrameHdr.DSTCoded = S.FIO_BitGetIntUnsigned(1);
		if (FrameHdr.DSTCoded == 0) { // DSD data
			Dummy = S.FIO_BitGetIntUnsigned(1); /* Was &D->DstXbits.Bit, but it was never used */
			Dummy = S.FIO_BitGetIntUnsigned(6);
			//System.out.printf("Dummy %x%n", Dummy);
			if (Dummy != 0) {
				throw new DSTException(String.format("Illegal stuffing pattern in frame %d!", FrameHdr.FrameNr), 0);
			}
			System.out.printf("Processing dsd frame%n");
			/* Read DSD data and put in output stream */
			ReadDSDframe(S, FrameHdr.MaxFrameLen, FrameHdr.NrOfChannels, DSDdataframe);
		} else {
			//System.out.printf("Processing dst frame%n");
			ReadSegmentData(S, FrameHdr);

			ReadMappingData(S, FrameHdr);

			ReadFilterCoefSets(S, FrameHdr.NrOfChannels, FrameHdr, StrFilter);

			ReadProbabilityTables(S, FrameHdr, StrPtable, P_one);

			ADataLen = (int) (FrameHdr.CalcNrOfBits - S.get_in_bitcount());

			ReadArithmeticCodedData(S, ADataLen, AData);
		}
	}
	
	final void LT_InitCoefTablesI(short[][][] ICoefI) {
		int FilterNr, FilterLength, TableNr, k, i, j;

		for (FilterNr = 0; FilterNr < FrameHdr.NrOfFilters; FilterNr++) {
			FilterLength = FrameHdr.PredOrder[FilterNr];
			for (TableNr = 0; TableNr < 16; TableNr++) {
				k = FilterLength - TableNr * 8;
				if (k > 8) {
					k = 8;
				} else if (k < 0) {
					k = 0;
				}
				for (i = 0; i < 256; i++) {
					int cvalue = 0;
					for (j = 0; j < k; j++) {
						cvalue += (((i >> j) & 1) * 2 - 1) * FrameHdr.ICoefA[FilterNr][TableNr * 8 + j];
					}
					ICoefI[FilterNr][TableNr][i] = (short) cvalue ;
				}
			}
		}
	}
	
	final void LT_InitStatus(int[][] Status) {
		int ChNr, TableNr;

		for (ChNr = 0; ChNr < FrameHdr.NrOfChannels; ChNr++) {
			for (TableNr = 0; TableNr < 16; TableNr++) {
				Status[ChNr][TableNr] = 0xaa;
			}
		}
	}
	
	final int LT_RUN_FILTER_I(short[][] FilterTable, int[] ChannelStatus) {
		int Predict = FilterTable[0][ChannelStatus[0]];
		for (int i = 1; i < 16; i++)
			Predict += FilterTable[i][ChannelStatus[i]];
		return Predict;
	}

	final int LT_ACGetPtableIndex(short PredicVal, int PtableLen) {
		int j;

		j = (PredicVal > 0 ? PredicVal : -PredicVal) >> AC_QSTEP;
		if (j >= PtableLen) {
			j = PtableLen - 1;
		}

		return j;
	}
	
	void FramDSTDecode(byte[] DSTdata, byte[] MuxedDSDdata, int FrameSizeInBytes, int FrameCnt) throws DSTException {
		int BitNr;
		int ChNr;
		int ACError;
		int NrOfBitsPerCh = (int) FrameHdr.NrOfBitsPerCh;
		short PredicBit;
		int PtableIndex;
		int Stop;

		FrameHdr.FrameNr = FrameCnt;
		FrameHdr.CalcNrOfBytes = FrameSizeInBytes;
		FrameHdr.CalcNrOfBits = FrameHdr.CalcNrOfBytes * 8;
		/* unpack DST frame: segmentation, mapping, arithmetic data */
		UnpackDSTframe(DSTdata, MuxedDSDdata);
		
		int NrOfChannels = FrameHdr.NrOfChannels;
		if (FrameHdr.DSTCoded == 1) {
			//System.out.printf("Decoding%n");
			int i;
			FillTable4Bit(FrameHdr.NrOfChannels, NrOfBitsPerCh, FrameHdr.FSeg, FrameHdr.Filter4Bit);
			FillTable4Bit(FrameHdr.NrOfChannels, NrOfBitsPerCh, FrameHdr.PSeg, FrameHdr.Ptable4Bit);

			LT_InitCoefTablesI(LT_ICoefI);			
			LT_InitStatus(LT_Status);

			AC.LT_ACDecodeBit_Init(AData, ADataLen);
			ACError = AC.LT_ACDecodeBit_Decode(Reverse7LSBs((short) FrameHdr.ICoefA[0][0]), AData, ADataLen);
			Arrays.fill(MuxedDSDdata, 0, NrOfBitsPerCh * NrOfChannels / 8, (byte) 0);

			for (BitNr = 0; BitNr < NrOfBitsPerCh; BitNr++) {
				int ByteNr = BitNr / 8;

				for (ChNr = 0; ChNr < NrOfChannels; ChNr++) {
					short Predict = 0;
					short Residual = 0;
					short BitVal;
					byte Filter = FrameHdr.Filter4Bit[ChNr][BitNr];

					/* Calculate output value of the FIR filter */
					//Predict =(short) LT_RUN_FILTER_I(LT_ICoefI[Filter], LT_Status[ChNr]);
					short[][] FilterTable = LT_ICoefI[Filter];
					int[] ChannelStatus = LT_Status[ChNr];
					Predict  = (short) (FilterTable[ 0][ChannelStatus[ 0]]
				     +FilterTable[ 1][ChannelStatus[ 1]]
				     +FilterTable[ 2][ChannelStatus[ 2]]
				     +FilterTable[ 3][ChannelStatus[ 3]]
				     +FilterTable[ 4][ChannelStatus[ 4]]
				     +FilterTable[ 5][ChannelStatus[ 5]]
				     +FilterTable[ 6][ChannelStatus[ 6]]
				     +FilterTable[ 7][ChannelStatus[ 7]]
				     +FilterTable[ 8][ChannelStatus[ 8]]
				     +FilterTable[ 9][ChannelStatus[ 9]]
				     +FilterTable[10][ChannelStatus[10]]
				     +FilterTable[11][ChannelStatus[11]]
				     +FilterTable[12][ChannelStatus[12]]
				     +FilterTable[13][ChannelStatus[13]]
				     +FilterTable[14][ChannelStatus[14]]
				     +FilterTable[15][ChannelStatus[15]]);
					
					/* Arithmetic decode the incoming bit */
					if ((FrameHdr.HalfProb[ChNr] == 1) && (BitNr < FrameHdr.NrOfHalfBits[ChNr])) {
						Residual = (short) AC.LT_ACDecodeBit_Decode( AC_PROBS / 2, AData, ADataLen);
					} else {
						int table4bit = FrameHdr.Ptable4Bit[ChNr][BitNr];
						PtableIndex = LT_ACGetPtableIndex(Predict, FrameHdr.PtableLen[table4bit]);
						Residual =  (short) AC.LT_ACDecodeBit_Decode(P_one[table4bit][PtableIndex], AData, ADataLen);
					}

					/* Channel bit depends on the predicted bit and BitResidual[][] */
					BitVal = (short) (((((short) Predict) >> 15) ^ Residual) & 1);
					//if (ByteNr * NrOfChannels + ChNr < 33)
					//System.out.printf(" %x", MuxedDSDdata[ByteNr * NrOfChannels + ChNr]);
					/* Shift the result into the correct bit position */
					MuxedDSDdata[ByteNr * NrOfChannels + ChNr] |= (byte) (BitVal << (7 - BitNr % 8))&255;
					//if (ByteNr * NrOfChannels + ChNr < 33)
					//System.out.printf(" %x %x %d %d-%d %b%n", MuxedDSDdata[ByteNr * NrOfChannels + ChNr], BitVal, ByteNr * NrOfChannels + ChNr, Residual, Predict, BitNr < FrameHdr.NrOfHalfBits[ChNr]); 

					/* Update filter */
					for (i = 15; i > 0; i--) {
						LT_Status[ChNr][i] =  ((LT_Status[ChNr][i] << 1) | ((LT_Status[ChNr][i-1] >> 7) & 1)) &255;
					}
					LT_Status[ChNr][0] = ((LT_Status[ChNr][0] << 1) | BitVal) & 255;
				}
			}

			/* Flush the arithmetic decoder */
			ACError = AC.LT_ACDecodeBit_Flush(AData, ADataLen);
//System.out.printf("%nbuf %s%n", Utils.toHexString(0, 64, MuxedDSDdata));
			if (ACError != 1 /*|| true*/) {
				throw new DSTException("Arithmetic decoding error!", -1);
			}
		}
	}

	void FramDSTDecode(byte[] DSTdata, byte[][] DSDdata, int FrameSizeInBytes, int FrameCnt) throws DSTException {
		int BitNr;
		int ChNr;
		int ACError = 0;
		int NrOfBitsPerCh = (int) FrameHdr.NrOfBitsPerCh;
		short PredicBit;
		int PtableIndex;
		int Stop;

		FrameHdr.FrameNr = FrameCnt;
		FrameHdr.CalcNrOfBytes = FrameSizeInBytes;
		FrameHdr.CalcNrOfBits = FrameHdr.CalcNrOfBytes * 8;
		/* unpack DST frame: segmentation, mapping, arithmetic data */
		byte[] MuxedDSDdata = new byte[1024 * 64];
		UnpackDSTframe(DSTdata, MuxedDSDdata);

		if (FrameHdr.DSTCoded == 1) {
			//System.out.printf("Decoding%n");
			int i;
			ACData AC = new ACData();
			for (i = 0; i < FrameHdr.NrOfChannels; i++)
				Arrays.fill(DSDdata[i], (byte) 0);
			byte[] BM = { (byte) (0x80 & 255), 0x40, 0x20, 0x10, 0x08, 0x04, 0x02, 0x01 };
			FillTable4Bit(FrameHdr.NrOfChannels, NrOfBitsPerCh, FrameHdr.FSeg, FrameHdr.Filter4Bit);
			FillTable4Bit(FrameHdr.NrOfChannels, NrOfBitsPerCh, FrameHdr.PSeg, FrameHdr.Ptable4Bit);

			DstXbits.PBit = Reverse7LSBs((short) FrameHdr.ICoefA[0][0]);
			//System.out.printf("revers of %x is %x%n", FrameHdr.ICoefA[0][0], DstXbits.PBit);

			DstXbits.Bit = AC.DST_ACDecodeBit(DstXbits.PBit, AData, ADataLen, 0);

			/* Initialise the Pnt and Status pointers for each channel */
			for (ChNr = 0; ChNr < FrameHdr.NrOfChannels; ChNr++) {
				for (i = 0; i < (1 << SIZE_CODEDPREDORDER); i++) {
					FirPtrs.Status[ChNr][i] = (short) (2 * (i & 1) - 1);
				}
				FirPtrs.Pnt[ChNr] = 0;
			}

			/* Deinterleaving of the channels is incorporated in these two loops */
			for (BitNr = 0; BitNr < FrameHdr.NrOfBitsPerCh; BitNr++) {
				for (ChNr = 0; ChNr < FrameHdr.NrOfChannels; ChNr++) {
					/* Calculate output value of the FIR filter */
					long PredicVal = 0;

					Stop = FirPtrs.Pnt[ChNr] + FrameHdr.PredOrder[FrameHdr.Filter4Bit[ChNr][BitNr]/* & 255*/];
					if (Stop > (1 << SIZE_CODEDPREDORDER)) {
						Stop = (1 << SIZE_CODEDPREDORDER);
					}
					int j;
					for (i = FirPtrs.Pnt[ChNr], j = 0; i < Stop; i++, j++) {
						PredicVal += FirPtrs.Status[ChNr][i] * FrameHdr.ICoefA[FrameHdr.Filter4Bit[ChNr][BitNr]][j];
					}
					for (i = 0; i < FirPtrs.Pnt[ChNr] + FrameHdr.PredOrder[FrameHdr.Filter4Bit[ChNr][BitNr]]
							- (1 << SIZE_CODEDPREDORDER); i++, j++) {
						PredicVal += FirPtrs.Status[ChNr][i] * FrameHdr.ICoefA[FrameHdr.Filter4Bit[ChNr][BitNr]][j];
					}
					byte BitResidual;
					/* Arithmetic decode the incoming bit */
					if ((FrameHdr.HalfProb[ChNr] == 1) && (BitNr < FrameHdr.NrOfHalfBits[ChNr])) {
						BitResidual = AC.DST_ACDecodeBit(AC_PROBS / 2, AData, ADataLen, 0);
					} else {
						PtableIndex = AC.DST_ACGetPtableIndex(PredicVal,
								FrameHdr.PtableLen[FrameHdr.Ptable4Bit[ChNr][BitNr]]);

						BitResidual = AC.DST_ACDecodeBit(P_one[FrameHdr.Ptable4Bit[ChNr][BitNr]][PtableIndex], AData,
								ADataLen, 0);
					}

					/* Channel bit depends on the predicted bit and BitResidual[][] */
					PredicBit = (short) (PredicVal >= 0 ? 1 : -1);

					if (BitResidual == 1) {
						//BitStream11[ChNr][BitNr] = (short) (-PredicBit);
						DSDdata[ChNr][BitNr / 8] |= (-PredicBit) > 0 ? BM[BitNr % 8] & 255 : 0;
					} else {
						//BitStream11[ChNr][BitNr] = PredicBit;
						DSDdata[ChNr][BitNr / 8] |= PredicBit > 0 ? BM[BitNr % 8] & 255 : 0;
					}

					/* Update filter */
					FirPtrs.Pnt[ChNr]--;
					if (FirPtrs.Pnt[ChNr] < 0) {
						FirPtrs.Pnt[ChNr] += (1 << SIZE_CODEDPREDORDER);
					}

					//FirPtrs.Status[ChNr][FirPtrs.Pnt[ChNr]] = BitStream11[ChNr][BitNr];
					FirPtrs.Status[ChNr][FirPtrs.Pnt[ChNr]] = ((DSDdata[ChNr][(BitNr) / 8] & (BM[BitNr % 8] & 255)) > 0 ? 1
							: -1);
				}
			}
			/* Flush the arithmetic decoder */
			ACError = AC.DST_ACDecodeBit(0, AData, ADataLen, 1);

			if (ACError != 0) {
				throw new DSTException(String.format("Arithmetic decoding error at frame %d!", FrameHdr.FrameNr), -1);
			}
		}
	}

}
