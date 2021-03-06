// CodonTableUtils.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)


package pal.datatype;


/**
 * Nucleotide Translating Utilities
 *
 * @author Matthew Goode
 *
 * @version $Id: CodonTableUtils.java,v 1.3 2001/07/13 14:39:13 korbinian Exp $
 */

public class CodonTableUtils {


	/**
	 * Translates a Nucleotide sequence into a Amino Acid sequence
	 */
	public static final String convertNucleotideToAminoAcid(String
	nucleotideSequence, int startingPosition, int length, boolean reverse, 
		CodonTable translator) {
		return (convertNucleotideToAminoAcid(nucleotideSequence.toCharArray(), startingPosition,length, reverse, translator)).toString();
	}


	/**
	 * Translates a Nucleotide sequence into a Amino Acid sequence
	 * @param nucleotideSequence - the base nucleotide sequence as a char array
	 * @param starting position - the starting position to begin reading from
	 * @param length - the length of the reading frame (in nucleotide units - 
	 * should be a multiple of 3, if not remainder is truncated!)
	 * @param reverse - if true works backwards with codon at starting 
	 * position being last in translation (codon read in reverse as well). 
	 * Else reads forwards.
	 * @param translator - the nucleotide translator to use for translation
	 * nucleotides into amino acids.
	 * @note can handle circular reading frames (ie startingPositon+length 
	 * can be greater than seuqnce length)
	 */
	public static final char[] convertNucleotideToAminoAcid(char[]
	nucleotideSequence, int startingPosition, int length, boolean reverse, 
	CodonTable translator) {
		char[] work = new char[3];
		//Normal dircection
		int numberOfAminoAcids = (length)/3;
		char[] aas = new char[numberOfAminoAcids];
		if(reverse) {
			for(int i = 0 ; i < numberOfAminoAcids ; i++) {
				int index = i*3+startingPosition;;
				for(int j = 0 ; j < 3 ; j++) {
					work[j] = nucleotideSequence[(index+2-j)%nucleotideSequence.length];
				}
				aas[numberOfAminoAcids-i-1] = translator.getAminoAcidChar(work);
			}
		} else {
			for(int i = 0 ; i < numberOfAminoAcids ; i++) {
				int index = i*3+startingPosition;
				for(int j = 0 ; j < 3 ; j++) {
					work[j] = nucleotideSequence[(index+j)%nucleotideSequence.length];
				}
				aas[i] = translator.getAminoAcidChar(work);
			}
		}
		return aas;

	}

}

