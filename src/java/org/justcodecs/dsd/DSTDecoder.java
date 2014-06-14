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
		short[][] ICoefA; /* Integer coefs for actual coding            */
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
		int[][] Data; /* Fir/PtabData[Fir/PtabNr][Index]            */
		int[] DataLen; /* Fir/PtabDataLength[Fir/PtabNr]             */
		int StreamBits; /* nr of bits all filters use in the stream   */
		int TableType; /* FILTER or PTABLE: indicates contents       */
	}

	static class StrData {
		byte[] pDSTdata;
		int TotalBytes;
		int ByteCounter;
		int BitPosition;
		byte DataByte;

		long tmp[] = new long[1];

		/***********************************************************************
		 * GetDSTDataPointer
		 ***********************************************************************/

		int GetDSTDataPointer(byte[][] pBuffer) {
			int hr = 0;

			pBuffer[0] = pDSTdata;

			return (hr);
		}

		/***********************************************************************
		 * ResetReadingIndex
		 ***********************************************************************/

		int ResetReadingIndex() {
			int hr = 0;

			BitPosition = 0;
			ByteCounter = 0;
			DataByte = 0;

			return (hr);
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
				getbits(tmp, Len);
				return (byte) tmp[0];
			} else if (Len == 0) {
				return 0;

			} else
				throw new DSTException(-1);
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
				getbits(tmp, Len);
				return (int) tmp[0];
			} else if (Len == 0) {
				return 0;
			} else
				throw new DSTException(-1);
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

		int FIO_BitGetIntSigned(int Len) throws DSTException {
			if (Len > 0) {
				getbits(tmp, Len);
				int x = (int) tmp[0];

				if (x >= (1 << (Len - 1))) {
					x -= (1 << Len);
				}
				return x;
			} else if (Len == 0) {
				return 0;
			} else
				throw new DSTException(-1);
		}

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
				getbits(tmp, Len);
				short x = (short) tmp[0];

				if (x >= (1 << (Len - 1))) {
					x -= (1 << Len);
				}
				return x;
			} else if (Len == 0) {
				return 0;
			} else
				throw new DSTException(-1);
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

		void getbits(long[] outword, int out_bitptr) throws DSTException {
			if (out_bitptr == 1) {
				if (BitPosition == 0) {
					DataByte = pDSTdata[ByteCounter++];
					if (ByteCounter > TotalBytes) {
						throw new DSTException("EOF", -1); /* EOF */
					}
					BitPosition = 8;
				}
				BitPosition--;
				outword[0] = (DataByte >> BitPosition) & 1;
				System.out.printf("Byte:0x%x, res 0x%x, for %d%n", DataByte, outword[0], out_bitptr);
				return;
			}

			outword[0] = 0;
			while (out_bitptr > 0) {
				int thisbits, mask, shift;

				if (BitPosition == 0) {
					DataByte = pDSTdata[ByteCounter++];
					if (ByteCounter > TotalBytes) {
						throw new DSTException("EOF", -1); /* EOF *//* EOF */
					}
					BitPosition = 8;
				}

				thisbits = Math.min(BitPosition, out_bitptr);
				shift = (BitPosition - thisbits);
				mask = masks[thisbits] << shift;

				shift = (out_bitptr - thisbits) - shift;
				if (shift <= 0)
					outword[0] |= ((DataByte & mask) >> -shift);
				else
					outword[0] |= ((DataByte & mask) << shift);
				System.out.printf("Byte:0x%x, res 0x%x, for %d%n", DataByte, outword[0], out_bitptr);
				out_bitptr -= thisbits;
				BitPosition -= thisbits;
			}
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

		int get_in_bitcount() {
			return ByteCounter * 8 - BitPosition;
		}

	}

	static class ACData {
		int Init;
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

		byte DST_ACDecodeBit(byte b, int p, byte[] cb, int fs, int Flush) {
			/*
			static unsigned int  Init = 1;
			static unsigned int  C;
			static unsigned int  A;
			static int           cbptr;
			*/
			int ap;
			int h;

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
						C |= cb[cbptr];
					}
					cbptr++;
				}
			} else {
				Init = 1;
				if (cbptr < fs - 7) {
					b = 0;
				} else {
					b = 1;
					while ((cbptr < fs) && (b == 1)) {
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

		void LT_ACDecodeBit_Init(byte[] cb, int fs) {
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

		byte LT_ACDecodeBit_Decode(byte b, int p, byte[] cb, int fs) {
			int ap;
			int h;

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
					C |= cb[cbptr];
				}
				cbptr++;
			}
			return b;
		}

		byte LT_ACDecodeBit_Flush(byte b, int p, byte[] cb, int fs) {
			Init = 1;
			if (cbptr < fs - 7) {
				b = 0;
			} else {
				b = 1;
				while ((cbptr < fs) && (b == 1)) {
					if (cb[cbptr] != 0) {
						b = 1;
					}
					cbptr++;
				}
			}
			return b;
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

	int SSE2;

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
					Table4Bit[ChNr][BitNr] = Val;
				}
				Start += S.Resolution * 8 * S.SegmentLen[ChNr][SegNr];
			}

			Val = (byte) S.Table4Segment[ChNr][SegNr];
			Arrays.fill(Table4BitCh, Start, NrOfBitsPerCh - Start, Val);
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
	static short reverse[] = { // 128
	1, 65, 33, 97, 17, 81, 49, 113, 9, 73, 41, 105, 25, 89, 57, 121, 5, 69, 37, 101, 21, 85, 53, 117, 13, 77, 45, 109,
			29, 93, 61, 125, 3, 67, 35, 99, 19, 83, 51, 115, 11, 75, 43, 107, 27, 91, 59, 123, 7, 71, 39, 103, 23, 87,
			55, 119, 15, 79, 47, 111, 31, 95, 63, 127, 2, 66, 34, 98, 18, 82, 50, 114, 10, 74, 42, 106, 26, 90, 58,
			122, 6, 70, 38, 102, 22, 86, 54, 118, 14, 78, 46, 110, 30, 94, 62, 126, 4, 68, 36, 100, 20, 84, 52, 116,
			12, 76, 44, 108, 28, 92, 60, 124, 8, 72, 40, 104, 24, 88, 56, 120, 16, 80, 48, 112, 32, 96, 64, 128 };

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

	void ReadTableSegmentData(StrData SD, int NrOfChannels, int FrameLen, int MaxNrOfSegs, int MinSegLen, FrameHeader FH)
			throws DSTException {
		int ChNr = 0;
		int DefinedBits = 0;
		int ResolRead = 0;
		int SegNr = 0;
		int MaxSegSize;
		int NrOfBits;
		int EndOfChannel;

		MaxSegSize = FrameLen - MinSegLen / 8;

		FH.FSameSegAllCh = SD.FIO_BitGetIntUnsigned(1);

		if (FH.FSameSegAllCh == 1) {
			EndOfChannel = SD.FIO_BitGetIntUnsigned(1);

			while (EndOfChannel == 0) {
				if (SegNr >= MaxNrOfSegs) {
					throw new DSTException("ERROR: Too many segments for this channel!", -1);

				}
				if (ResolRead == 0) {
					NrOfBits = (int) Math.log(FrameLen - MinSegLen / 8); // Log2RoundUp
					FH.FSeg.Resolution = SD.FIO_BitGetIntUnsigned(NrOfBits);
					if ((FH.FSeg.Resolution == 0) || (FH.FSeg.Resolution > FrameLen - MinSegLen / 8)) {
						throw new DSTException("ERROR: Invalid segment resolution!", -1);

					}
					ResolRead = 1;
				}
				NrOfBits = (int) Math.round(Math.log(MaxSegSize / FH.FSeg.Resolution));
				FH.FSeg.SegmentLen[0][SegNr] = SD.FIO_BitGetIntUnsigned(NrOfBits);

				if ((FH.FSeg.Resolution * 8 * FH.FSeg.SegmentLen[0][SegNr] < MinSegLen)
						|| (FH.FSeg.Resolution * 8 * FH.FSeg.SegmentLen[0][SegNr] > FrameLen * 8 - DefinedBits
								- MinSegLen)) {
					throw new DSTException("ERROR: Invalid segment length!", -1);

				}
				DefinedBits += FH.FSeg.Resolution * 8 * FH.FSeg.SegmentLen[0][SegNr];
				MaxSegSize -= FH.FSeg.Resolution * FH.FSeg.SegmentLen[0][SegNr];
				SegNr++;
				EndOfChannel = SD.FIO_BitGetIntUnsigned(1);
			}
			FH.FSeg.NrOfSegments[0] = SegNr + 1;
			FH.FSeg.SegmentLen[0][SegNr] = 0;

			for (ChNr = 1; ChNr < NrOfChannels; ChNr++) {
				FH.FSeg.NrOfSegments[ChNr] = FH.FSeg.NrOfSegments[0];
				for (SegNr = 0; SegNr < FH.FSeg.NrOfSegments[0]; SegNr++) {
					FH.FSeg.SegmentLen[ChNr][SegNr] = FH.FSeg.SegmentLen[0][SegNr];
				}
			}
		} else {
			while (ChNr < NrOfChannels) {
				if (SegNr >= MaxNrOfSegs) {
					throw new DSTException("ERROR: Too many segments for this channel!", -1);

				}
				EndOfChannel = SD.FIO_BitGetIntUnsigned(1);
				if (EndOfChannel == 0) {
					if (ResolRead == 0) {
						NrOfBits = (int) Math.round(Math.log(FrameLen - MinSegLen / 8));
						FH.FSeg.Resolution = SD.FIO_BitGetIntUnsigned(NrOfBits);
						if ((FH.FSeg.Resolution == 0) || (FH.FSeg.Resolution > FrameLen - MinSegLen / 8)) {
							throw new DSTException("ERROR: Invalid segment resolution!", -1);

						}
						ResolRead = 1;
					}
					NrOfBits = (int) Math.round(Math.log(MaxSegSize / FH.FSeg.Resolution));
					FH.FSeg.SegmentLen[ChNr][SegNr] = SD.FIO_BitGetIntUnsigned(NrOfBits);

					if ((FH.FSeg.Resolution * 8 * FH.FSeg.SegmentLen[ChNr][SegNr] < MinSegLen)
							|| (FH.FSeg.Resolution * 8 * FH.FSeg.SegmentLen[ChNr][SegNr] > FrameLen * 8 - DefinedBits
									- MinSegLen)) {
						throw new DSTException("ERROR: Invalid segment length!", -1);

					}
					DefinedBits += FH.FSeg.Resolution * 8 * FH.FSeg.SegmentLen[ChNr][SegNr];
					MaxSegSize -= FH.FSeg.Resolution * FH.FSeg.SegmentLen[ChNr][SegNr];
					SegNr++;
				} else {
					FH.FSeg.NrOfSegments[ChNr] = SegNr + 1;
					FH.FSeg.SegmentLen[ChNr][SegNr] = 0;
					SegNr = 0;
					DefinedBits = 0;
					MaxSegSize = FrameLen - MinSegLen / 8;
					ChNr++;
				}
			}
		}
		if (ResolRead == 0) {
			FH.FSeg.Resolution = 1;
		}

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
				throw new DSTException("ERROR: Too many segments!", -1);

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
		ReadTableSegmentData(SD, FH.NrOfChannels, FH.MaxFrameLen, MAXNROF_FSEGS, MIN_FSEG_LEN, FH); // ->

		if (FH.PSameSegAsF == 1) {
			CopySegmentData(FH);
		} else {
			ReadTableSegmentData(SD, FH.NrOfChannels, FH.MaxFrameLen, MAXNROF_PSEGS, MIN_PSEG_LEN, FH); // ->
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

	void ReadTableMappingData(StrData SD, int NrOfChannels, int MaxNrOfTables, FrameHeader FH) throws DSTException {
		int ChNr;
		int CountTables = 1;
		int NrOfBits = 1;
		int SegNr;

		FH.FSeg.Table4Segment[0][0] = 0;

		FH.FSameMapAllCh = SD.FIO_BitGetIntUnsigned(1);
		if (FH.FSameMapAllCh == 1) {
			for (SegNr = 1; SegNr < FH.FSeg.NrOfSegments[0]; SegNr++) {
				NrOfBits = (int) Math.round(Math.log(CountTables));
				FH.FSeg.Table4Segment[0][SegNr] = SD.FIO_BitGetIntUnsigned(NrOfBits);

				if (FH.FSeg.Table4Segment[0][SegNr] == CountTables) {
					CountTables++;
				} else if (FH.FSeg.Table4Segment[0][SegNr] > CountTables) {
					throw new DSTException("ERROR: Invalid table number for segment!", -1);

				}
			}
			for (ChNr = 1; ChNr < NrOfChannels; ChNr++) {
				if (FH.FSeg.NrOfSegments[ChNr] != FH.FSeg.NrOfSegments[0]) {
					throw new DSTException("ERROR: Mapping can't be the same for all channels!", -1);
				}
				for (SegNr = 0; SegNr < FH.FSeg.NrOfSegments[0]; SegNr++) {
					FH.FSeg.Table4Segment[ChNr][SegNr] = FH.FSeg.Table4Segment[0][SegNr];
				}
			}
		} else {
			for (ChNr = 0; ChNr < NrOfChannels; ChNr++) {
				for (SegNr = 0; SegNr < FH.FSeg.NrOfSegments[ChNr]; SegNr++) {
					if ((ChNr != 0) || (SegNr != 0)) {
						NrOfBits = (int) Math.round(Math.log(CountTables));
						FH.FSeg.Table4Segment[ChNr][SegNr] = SD.FIO_BitGetIntUnsigned(NrOfBits);

						if (FH.FSeg.Table4Segment[ChNr][SegNr] == CountTables) {
							CountTables++;
						} else if (FH.FSeg.Table4Segment[ChNr][SegNr] > CountTables) {
							throw new DSTException("ERROR: Invalid table number for segment!", -1);
						}
					}
				}
			}
		}
		if (CountTables > MaxNrOfTables) {
			throw new DSTException("ERROR: Too many tables for this frame!", -1);

		}
		FH.NrOfPtables = CountTables;
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
				throw new DSTException("ERROR: Not same number of segments for filters and Ptables!", -1);
			}
		}
		FH.NrOfPtables = FH.NrOfFilters;
		if (FH.NrOfPtables > FH.MaxNrOfPtables) {
			throw new DSTException("ERROR: Too many tables for this frame!", -1);

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

		ReadTableMappingData(SD, FH.NrOfChannels, FH.MaxNrOfFilters, FH);

		if (FH.PSameMapAsF == 1) {
			CopyMappingData(FH);
		} else {
			ReadTableMappingData(SD, FH.NrOfChannels, FH.MaxNrOfPtables, FH);
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
		int return_value;

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
				}
			} else {
				int bestmethod;

				CF.BestMethod[FilterNr] = SD.FIO_BitGetIntUnsigned(SIZE_RICEMETHOD);
				bestmethod = CF.BestMethod[FilterNr];
				if (CF.CPredOrder[bestmethod] >= FH.PredOrder[FilterNr]) {
					throw new DSTException("ERROR: Invalid coefficient coding method!", -1);

				}

				for (CoefNr = 0; CoefNr < CF.CPredOrder[bestmethod]; CoefNr++) {
					FH.ICoefA[FilterNr][CoefNr] = SD.FIO_BitGetShortSigned(SIZE_PREDCOEF);
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
						throw new DSTException("ERROR: filter coefficient out of range!", -1);

					} else {
						FH.ICoefA[FilterNr][CoefNr] = (short) c;
					}
				}
			}

			/* Clear out remaining coeffs, as the SSE2 code uses them all. */
			Arrays.fill(FH.ICoefA[FilterNr], CoefNr, ((1 << SIZE_CODEDPREDORDER) - CoefNr) * FH.ICoefA[0].length,
					(short) 0);
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
						P_one[PtableNr][EntryNr] = SD.FIO_BitGetIntUnsigned(AC_BITS);
						P_one[PtableNr][EntryNr]++;
					}
				} else {
					int bestmethod;
					CP.BestMethod[PtableNr] = SD.FIO_BitGetIntUnsigned(SIZE_RICEMETHOD);
					bestmethod = CP.BestMethod[PtableNr];
					if (CP.CPredOrder[bestmethod] >= FH.PtableLen[PtableNr]) {
						throw new DSTException("ERROR: Invalid Ptable coding method!", -1);
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

						if ((c < 1) || (c > (1 << (AC_BITS - 1)))) {
							throw new DSTException("ERROR: Ptable entry out of range!", -1);
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

	static int spread[] = {
			//BIG_ENDIAN
			0x00000000, 0x00000001, 0x00000100, 0x00000101, 0x00010000, 0x00010001, 0x00010100, 0x00010101, 0x01000000,
			0x01000001, 0x01000100, 0x01000101, 0x01010000, 0x01010001, 0x01010100, 0x01010101
	/*
	    0x00000000, 0x01000000, 0x00010000, 0x01010000,
	    0x00000100, 0x01000100, 0x00010100, 0x01010100,
	    0x00000001, 0x01000001, 0x00010001, 0x01010001,
	    0x00000101, 0x01000101, 0x00010101, 0x01010101
	*/
	};

	void expandInt(byte[] buf, int off, int fill) {
		for (int i = 0; i < 4; i++) {
			buf[off++] = (byte) (fill & 0xff);
			fill >>= 8;
		}
	}

	void ReadArithmeticCodedData(StrData SD, int ADataLen, byte[] AData) throws DSTException {
		int j;
		int val;

		for (j = 0; j < ADataLen - 31; j += 32) {
			val = SD.FIO_BitGetIntUnsigned(32);
			/* Write out the expanded bits a nibble worth at a time */
			expandInt(AData, j, spread[(val >> 28) & 0xf]);
			expandInt(AData, j + 4, spread[(val >> 24) & 0xf]);
			expandInt(AData, j + 8, spread[(val >> 20) & 0xf]);
			expandInt(AData, j + 12, spread[(val >> 16) & 0xf]);
			expandInt(AData, j + 16, spread[(val >> 12) & 0xf]);
			expandInt(AData, j + 20, spread[(val >> 8) & 0xf]);
			expandInt(AData, j + 24, spread[(val >> 4) & 0xf]);
			expandInt(AData, j + 28, spread[(val) & 0xf]);
		}
		/* Handle remaining bits */
		for (; j < ADataLen; j++) {
			AData[j] = SD.FIO_BitGetChrUnsigned(1);
		}
	}

	void init(int NrChannels, int Fs44) {
		S = new StrData();
		FrameHdr = new FrameHeader();
		FrameHdr.NrOfChannels = NrChannels;
		FrameHdr.FrameNr = 0;
		StrFilter = new CodedTable();
		StrPtable = new CodedTable();
		StrFilter.TableType = FILTER;
		StrPtable.TableType = PTABLE;
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
		AData = new byte[(int) FrameHdr.BitStreamLen];
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
			System.out.printf("Dummy %x%n", Dummy);
			if (Dummy != 0) {
				throw new DSTException(String.format("Illegal stuffing pattern in frame %d!", FrameHdr.FrameNr), 0);
			}
			System.out.printf("Processing dsd frame%n");
			/* Read DSD data and put in output stream */
			ReadDSDframe(S, FrameHdr.MaxFrameLen, FrameHdr.NrOfChannels, DSDdataframe);
		} else {
			System.out.printf("Processing dst frame%n");
			ReadSegmentData(S, FrameHdr);

			ReadMappingData(S, FrameHdr);

			ReadFilterCoefSets(S, FrameHdr.NrOfChannels, FrameHdr, StrFilter);

			ReadProbabilityTables(S, FrameHdr, StrPtable, P_one);

			ADataLen = (int) (FrameHdr.CalcNrOfBits - S.get_in_bitcount());
			ReadArithmeticCodedData(S, ADataLen, AData);

			if ((ADataLen > 0) && (AData[0] != 0)) {
				throw new DSTException(String.format("Illegal arithmetic code in frame %d!", FrameHdr.FrameNr), -1);
			}
		}
	}

	void LT_InitCoefTablesI(short[][][] ICoefI) {
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
					ICoefI[FilterNr][TableNr][i] = (short) cvalue;
				}
			}
		}
	}

	void LT_InitStatus(byte[][] Status) {
		int ChNr, TableNr;

		for (ChNr = 0; ChNr < FrameHdr.NrOfChannels; ChNr++) {
			for (TableNr = 0; TableNr < 16; TableNr++) {
				Status[ChNr][TableNr] = (byte) 0xaa;
			}
		}
	}

	int LT_RUN_FILTER_I(short[][] FilterTable, byte[] ChannelStatus) {
		int Predict = FilterTable[0][ChannelStatus[0]];
		for (int i = 1; i < 16; i++)
			Predict += FilterTable[i][ChannelStatus[i]];
		return Predict;
	}

	int LT_ACGetPtableIndex(short PredicVal, int PtableLen) {
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
		byte ACError = 0;
		int NrOfBitsPerCh = (int) FrameHdr.NrOfBitsPerCh;
		int NrOfChannels = FrameHdr.NrOfChannels;
		byte[] MuxedDSD = MuxedDSDdata;

		FrameHdr.FrameNr = FrameCnt;
		FrameHdr.CalcNrOfBytes = FrameSizeInBytes;
		FrameHdr.CalcNrOfBits = FrameHdr.CalcNrOfBytes * 8;

		/* unpack DST frame: segmentation, mapping, arithmetic data */
		UnpackDSTframe(DSTdata, MuxedDSDdata);

		if (FrameHdr.DSTCoded == 1) {
			ACData AC = new ACData();
			short[][][] LT_ICoefI = new short[2 * MAX_CHANNELS][16][256];
			//short[][][] LT_ICoefU = new short[2 * MAX_CHANNELS][16][256];
			byte[][] LT_Status = new byte[MAX_CHANNELS][16];
			FillTable4Bit(NrOfChannels, NrOfBitsPerCh, FrameHdr.FSeg, FrameHdr.Filter4Bit);
			FillTable4Bit(NrOfChannels, NrOfBitsPerCh, FrameHdr.PSeg, FrameHdr.Ptable4Bit);

			LT_InitCoefTablesI(LT_ICoefI);
			//LT_InitCoefTablesU(D, LT_ICoefU);
			LT_InitStatus(LT_Status);

			AC.LT_ACDecodeBit_Init(AData, ADataLen);
			ACError = AC.LT_ACDecodeBit_Decode(ACError, Reverse7LSBs(FrameHdr.ICoefA[0][0]), AData, ADataLen);
			Arrays.fill(MuxedDSD, 0, NrOfBitsPerCh * NrOfChannels / 8, (byte) 0);

			for (BitNr = 0; BitNr < NrOfBitsPerCh; BitNr++) {
				int ByteNr = BitNr / 8;

				for (ChNr = 0; ChNr < NrOfChannels; ChNr++) {
					short Predict = 0;
					short Residual = 0;
					short BitVal;
					byte Filter = FrameHdr.Filter4Bit[ChNr][BitNr];

					/* Calculate output value of the FIR filter */
					LT_RUN_FILTER_I(LT_ICoefI[Filter], LT_Status[ChNr]);
					//LT_RUN_FILTER_U(LT_ICoefU[Filter], LT_Status[ChNr]);
					//Predict = LT_RunFilterI(LT_ICoefI[Filter], LT_Status[ChNr]);
					//Predict = LT_RunFilterU(LT_ICoefU[Filter], LT_Status[ChNr]);

					/* Arithmetic decode the incoming bit */
					if ((FrameHdr.HalfProb[ChNr] == 1) && (BitNr < FrameHdr.NrOfHalfBits[ChNr])) {
						Residual = AC.LT_ACDecodeBit_Decode((byte) Residual, AC_PROBS / 2, AData, ADataLen);
					} else {
						int table4bit = FrameHdr.Ptable4Bit[ChNr][BitNr];
						int PtableIndex = LT_ACGetPtableIndex(Predict, FrameHdr.PtableLen[table4bit]);

						AC.LT_ACDecodeBit_Decode((byte) Residual, P_one[table4bit][PtableIndex], AData, ADataLen);
					}

					/* Channel bit depends on the predicted bit and BitResidual[][] */
					BitVal = (short) (((((short) Predict) >> 15) ^ Residual) & 1);

					/* Shift the result into the correct bit position */
					MuxedDSD[ByteNr * NrOfChannels + ChNr] |= (byte) (BitVal << (7 - BitNr % 8));

					/* Update filter */
					for (int i = 15; i > 0; i--) {
						LT_Status[ChNr][i] = (byte) ((LT_Status[ChNr][i] << 1) | ((LT_Status[ChNr][i] >> 7) & 1));
					}
					LT_Status[ChNr][0] = (byte) ((LT_Status[ChNr][0] << 1) | BitVal);
				}
			}

			/* Flush the arithmetic decoder */
			ACError = AC.LT_ACDecodeBit_Flush((byte) ACError, 0, AData, ADataLen);

			if (ACError != 1) {
				throw new DSTException("Arithmetic decoding error!", -1);
			}
		}

	}

}